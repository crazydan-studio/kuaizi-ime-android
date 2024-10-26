/*
 * 筷字输入法 - 高效编辑需要又好又快的输入法
 * Copyright (C) 2024 Crazydan Studio
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.crazydan.studio.app.ime.kuaizi.core.dict;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import org.crazydan.studio.app.ime.kuaizi.R;
import org.crazydan.studio.app.ime.kuaizi.core.InputWord;
import org.crazydan.studio.app.ime.kuaizi.core.dict.predict.HMM;
import org.crazydan.studio.app.ime.kuaizi.core.input.PinyinInputWord;
import org.crazydan.studio.app.ime.kuaizi.utils.FileUtils;

import static org.crazydan.studio.app.ime.kuaizi.utils.DBUtils.SQLiteQueryParams;
import static org.crazydan.studio.app.ime.kuaizi.utils.DBUtils.SQLiteRawQueryParams;
import static org.crazydan.studio.app.ime.kuaizi.utils.DBUtils.copySQLite;
import static org.crazydan.studio.app.ime.kuaizi.utils.DBUtils.execSQLite;
import static org.crazydan.studio.app.ime.kuaizi.utils.DBUtils.openSQLite;
import static org.crazydan.studio.app.ime.kuaizi.utils.DBUtils.querySQLite;
import static org.crazydan.studio.app.ime.kuaizi.utils.DBUtils.rawQuerySQLite;

/**
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2024-10-20
 */
public class PinyinUserDictDB {
    /** 代表 {@link HMM#TOTAL} 的字 id */
    public static final String TOTAL_WORD_ID = "-2";
    /** 代表 {@link HMM#EOS} 和 {@link HMM#BOS} 的字 id */
    public static final String EOS_BOS_WORD_ID = "-1";

    /** 最新版本号 */
    public static final String LATEST_VERSION = "v3";

    public static final String db_version_file = "pinyin_user_dict.version";

    private final Context context;

    private String version;
    private SQLiteDatabase db;
    /** 用户词组数据的基础权重，以确保用户输入权重大于应用词组数据 */
    private int userPhraseBaseWeight = 500;

    // <<<<<<<<<<<<< 缓存常量数据
    private PinyinTree pinyinTree;
    // >>>>>>>>>>>>>

    private enum DictDBType {
        /** 用户库 */
        user("pinyin_user_dict.db"),
        /** 应用字典库 */
        app_word("pinyin_word_dict.app.db"),
        /** 应用词典库 */
        app_phrase("pinyin_phrase_dict.app.db");

        private final String fileName;

        DictDBType(String fileName) {
            this.fileName = fileName;
        }
    }

    public PinyinUserDictDB(Context context) {
        this.context = context;
    }

    /** 升级数据库 */
    public void upgrade() {
        String version = getVersion();

        if ("v2".equals(version) && LATEST_VERSION.equals("v3")) {
            upgrade_v2_to_v3();
            updateToLatestVersion();
        }
    }

    /**
     * 保存用户输入的拼音短语
     *
     * @param reverse
     *         是否反向操作，即，撤销对输入短语的保存
     */
    public void savePinyinPhrase(List<InputWord> phrase, boolean reverse) {
        if (phrase.isEmpty()) {
            return;
        }

        SQLiteDatabase db = getDB();
        HMM hmm = calcTransProb(phrase);
        updateHMM(db, hmm, reverse);
    }

    /** 根据拼音的字母组合得到前 N 个最佳预测结果 */
    public List<String[]> predict(List<String> pinyinCharsIdList, int top) {
        if (pinyinCharsIdList.isEmpty()) {
            return List.of();
        }

        Map<String, Map<String, Integer>> transProb = new HashMap<>();
        Map<String, Set<String>> pinyinCharsIdAndWordIdsMap = new HashMap<>(pinyinCharsIdList.size());

        rawQuerySQLite(getDB(), new SQLiteRawQueryParams<Void>() {{
            // 查询结果列包括（按顺序）：word_id_, prev_word_id_, word_spell_chars_id_, value_app_, value_user_
            this.sql = createTransProbQuerySQL(pinyinCharsIdList);

            this.reader = (cursor) -> {
                // Note: Android SQLite 从 0 开始取，与 jdbc 的规范不一样
                String wordId = cursor.getString(0);
                String preWordId = cursor.getString(1);
                String pinyinCharsId = cursor.getString(2);
                Integer appValue = cursor.getInt(3);
                Integer userValue = cursor.getInt(4);

                Map<String, Integer> prob = transProb.computeIfAbsent(wordId, (k) -> new HashMap<>());
                prob.compute(preWordId, (k, v) -> (v == null ? 0 : v) //
                                                  + appValue + userValue
                                                  // 用户数据需加上基础权重
                                                  + (userValue > 0 ? PinyinUserDictDB.this.userPhraseBaseWeight : 0));

                if (pinyinCharsId != null) {
                    pinyinCharsIdAndWordIdsMap.computeIfAbsent(pinyinCharsId, (k) -> new HashSet<>()).add(wordId);
                }

                return null;
            };
        }});

        // <<<<<<<<<<<<<<<
        Map<String, Object[]>[] viterbi = calcViterbi(pinyinCharsIdList, transProb, pinyinCharsIdAndWordIdsMap);

        List<String[]> phrases = new ArrayList<>(top);

        return phrases;
    }

    /** 获取应用本地的用户数据库的版本 */
    public String getVersion() {
        if (this.version == null) {
            File userDBFile = getDBFile(this.context, DictDBType.user);
            File versionFile = getVersionFile();

            if (!versionFile.exists()) {
                if (userDBFile.exists()) { // 应用 HMM 算法之前的版本
                    this.version = "v2";
                } else { // 首次安装
                    this.version = LATEST_VERSION;
                }
            } else { // 实际记录的版本号
                this.version = FileUtils.read(versionFile, true);
            }
        }

        return this.version;
    }

    private void updateToLatestVersion() {
        File file = getVersionFile();

        try {
            FileUtils.write(file, LATEST_VERSION);
            this.version = LATEST_VERSION;
        } catch (IOException ignore) {
        }
    }

    private File getVersionFile() {
        return new File(this.context.getFilesDir(), db_version_file);
    }

    private File getDBFile(Context context, DictDBType dbType) {
        return new File(context.getFilesDir(), dbType.fileName);
    }

    private SQLiteDatabase getDB() {
        if (this.db == null) {
            File userDBFile = getDBFile(this.context, DictDBType.user);

            this.db = openSQLite(userDBFile, false);
        }
        return this.db;
    }

    private PinyinTree getPinyinTree() {
        if (this.pinyinTree == null) {
            Map<String, String> pinyinCharsAndIdMap = new HashMap<>(600);

            querySQLite(getDB(), new SQLiteQueryParams<Void>() {{
                this.table = "meta_pinyin_chars";
                this.columns = new String[] { "id_", "value_" };
                this.reader = (cursor) -> {
                    // Note: Android SQLite 从 0 开始取，与 jdbc 的规范不一样
                    pinyinCharsAndIdMap.put(cursor.getString(1), cursor.getString(0));
                    return null;
                };
            }});

            this.pinyinTree = PinyinTree.create(pinyinCharsAndIdMap);
        }
        return this.pinyinTree;
    }

    /** 计算给定短语的 {@link HMM#transProb} 数据 */
    private HMM calcTransProb(List<InputWord> phrase) {
        return HMM.calcTransProb(phrase.stream()
                                       // 以 拼音字 id 与 拼音字母组合 id 代表短语中的字
                                       .map(word -> word.getUid() + ":" + ((PinyinInputWord) word).getCharsId())
                                       .collect(Collectors.toList()));
    }

    /**
     * 更新 {@link HMM} 数据
     *
     * @param reverse
     *         是否反向更新，即，减掉 HMM 数据
     */
    private void updateHMM(SQLiteDatabase db, HMM hmm, boolean reverse) {
        // 采用 SQLite 的 UPSET 机制插入或更新数据：https://www.sqlite.org/lang_upsert.html

        // =============================================================================
        Function<String, String[]> extractWordIds = (s) -> s.split(":");

        List<String[]> phraseWordData = hmm.wordWeight.keySet().stream().map((key) -> {
            String[] wordIds = extractWordIds.apply(key);
            String val = hmm.wordWeight.get(key) + "";

            return !reverse
                   ? new String[] { wordIds[0], wordIds[1], val, val }
                   : new String[] { val, wordIds[0], wordIds[1] };
        }).collect(Collectors.toList());

        if (!reverse) {
            execSQLite(db,
                       "insert into"
                       + "  phrase_word(word_id_, spell_chars_id_, weight_app_, weight_user_)"
                       + "    values(?, ?, 0, ?)"
                       + "  on conflict(word_id_, spell_chars_id_)"
                       + "  do update set "
                       + "    weight_user_ = weight_user_ + ?",
                       phraseWordData);
        } else {
            execSQLite(db,
                       "update phrase_word"
                       + "  set weight_user_ = max(weight_user_ - ?, 0)"
                       + "  where word_id_ = ? and spell_chars_id_ = ?",
                       phraseWordData);
        }

        // ==============================================================================
        Function<String, String> getWordId = (s) -> {
            // EOS 用 -1 代替（句尾字）
            // BOS 用 -1 代替（句首字）
            // TOTAL 用 -2 代替（句子总数）
            if (HMM.EOS.equals(s) || HMM.BOS.equals(s)) {
                return EOS_BOS_WORD_ID;
            } else if (HMM.TOTAL.equals(s)) {
                return TOTAL_WORD_ID;
            }

            String[] wordIds = extractWordIds.apply(s);
            return wordIds[0];
        };

        List<String[]> phraseTransProbData = new ArrayList<>();
        hmm.transProb.forEach((curr, prob) -> {
            String currId = getWordId.apply(curr);

            prob.forEach((prev, value) -> {
                String prevId = getWordId.apply(prev);
                String val = value + "";

                phraseTransProbData.add(!reverse
                                        ? new String[] { currId, prevId, val, val }
                                        : new String[] { val, currId, prevId });
            });
        });

        if (!reverse) {
            execSQLite(db,
                       "insert into"
                       + "  phrase_trans_prob(word_id_, prev_word_id_, value_app_, value_user_)"
                       + "    values(?, ?, 0, ?)"
                       + "  on conflict(word_id_, prev_word_id_)"
                       + "  do update set "
                       + "    value_user_ = value_user_ + ?",
                       phraseTransProbData);
        } else {
            execSQLite(db,
                       "update phrase_trans_prob"
                       + "  set value_user_ = max(value_user_ - ?, 0)"
                       + "  where word_id_ = ? and prev_word_id_ = ?",
                       phraseTransProbData);
        }

        if (reverse) {
            // 清理无用数据
            execSQLite(db, new String[] {
                    "delete from phrase_word where weight_app_ = 0 and weight_user_ = 0",
                    "delete from phrase_trans_prob where value_app_ = 0 and value_user_ = 0",
                    });
        }
    }

    /** 构造 {@link HMM#transProb} 数据查询 SQL */
    private String createTransProbQuerySQL(List<String> pinyinCharsIdList) {
        List<String> charsIdList = new ArrayList<>(pinyinCharsIdList);

        // <<<<<<<<<<<<<< 构造通过拼音字母组合做字查询的递归 SQL
        // https://www.sqlite.org/lang_with.html
        Set<String> charsIdSet = new HashSet<>(charsIdList);

        List<String> wordTableSQLList = charsIdSet.stream()
                                                  .map((charsId) -> String.format(
                                                          "word_ids_%s(word_id_, spell_chars_id_) as ("
                                                          + " select word_id_, spell_chars_id_"
                                                          + " from phrase_word"
                                                          + " where spell_chars_id_ = %s"
                                                          + ")",
                                                          charsId,
                                                          charsId))
                                                  .collect(Collectors.toList());

        String noneWordCharsId = "none";
        wordTableSQLList.add(String.format("word_ids_%s(word_id_, spell_chars_id_) as ( values(-1, null) )",
                                           noneWordCharsId));

        // 补充短语首字和尾字
        charsIdList.add(0, noneWordCharsId);
        charsIdList.add(noneWordCharsId);

        Map<String, String> wordUnionSQLMap = new HashMap<>(charsIdList.size());
        for (int i = 1; i < charsIdList.size(); i++) {
            String prevCharsId = charsIdList.get(i - 1);
            String currCharsId = charsIdList.get(i);

            String unionCode = prevCharsId + "_" + currCharsId;
            if (wordUnionSQLMap.containsKey(unionCode)) {
                continue;
            }

            wordUnionSQLMap.put(unionCode,
                                String.format("    select"
                                              + " prev_.word_id_ as prev_word_id_,"
                                              + " curr_.word_id_ as curr_word_id_,"
                                              + " curr_.spell_chars_id_ as curr_word_spell_chars_id_"
                                              + " from word_ids_%s prev_ , word_ids_%s curr_",
                                              prevCharsId,
                                              currCharsId));
        }

        wordTableSQLList.add("word_ids(prev_word_id_, curr_word_id_, curr_word_spell_chars_id_) as (\n" //
                             + String.join("\nunion\n", wordUnionSQLMap.values()) //
                             + "\n  )");

        return "with recursive\n  "
               + String.join(",\n  ", wordTableSQLList)
               + "\nselect distinct "
               + " s_.word_id_, s_.prev_word_id_,"
               + " t_.curr_word_spell_chars_id_ as word_spell_chars_id_,"
               + " s_.value_app_, s_.value_user_"
               + " from phrase_trans_prob s_, word_ids t_"
               + " where"
               + " s_.word_id_ = t_.curr_word_id_"
               + " and ("
               + "  s_.prev_word_id_ = t_.prev_word_id_"
               // 当前拼音字都包含 TOTAL 列
               + ("  or s_.prev_word_id_ = " + TOTAL_WORD_ID)
               + ")";
    }

    /**
     * 计算 Viterbi
     *
     * @param pinyinCharsIdList
     *         输入拼音的字母组合 id 列表
     * @param transProb
     *         汉字（状态）间转移概率
     * @param pinyinCharsIdAndWordIdsMap
     *         输入拼音的字母组合 id 所对应的可选字 id 集合
     */
    private static Map<String, Object[]>[] calcViterbi(
            List<String> pinyinCharsIdList, Map<String, Map<String, Integer>> transProb,
            Map<String, Set<String>> pinyinCharsIdAndWordIdsMap
    ) {
        int total = pinyinCharsIdList.size();
        // 用于 log 平滑时所取的最小值，用于代替 0
        double minProb = -3.14e100;
        // pos 是目前节点的位置，word 为当前汉字即当前状态，
        // probability 为从 pre_word 上一汉字即上一状态转移到目前状态的概率
        // viterbi[pos][word] = (probability, pre_word)
        Map<String, Object[]>[] viterbi = new Map[total];

        // 训练数据的句子总数: word_id_ == -1 且 prev_word_id_ == -2
        int basePhraseSize = getTransProbValue(transProb, EOS_BOS_WORD_ID, TOTAL_WORD_ID);

        int lastIndex = total - 1;
        for (int i = -1; i < lastIndex; i++) {
            int prevIndex = i;
            int currentIndex = prevIndex + 1;

            String currentPinyinCharsId = pinyinCharsIdList.get(currentIndex);
            Set<String> currentWordIds = pinyinCharsIdAndWordIdsMap.get(currentPinyinCharsId);
            assert currentWordIds != null;

            // Note：句首字的前序字设为 -1
            String prevPinyinCharsId = prevIndex < 0 ? null : pinyinCharsIdList.get(prevIndex);
            Set<String> prevWordIds = prevPinyinCharsId == null
                                      ? Set.of(EOS_BOS_WORD_ID)
                                      : pinyinCharsIdAndWordIdsMap.get(prevPinyinCharsId);
            assert prevWordIds != null;

            if (viterbi[currentIndex] == null) {
                viterbi[currentIndex] = new HashMap<>();
            }
            Map<String, Object[]> currentWordViterbi = viterbi[currentIndex];

            // 遍历 current_word_ids 和 prev_word_ids，找出所有可能与当前拼音相符的汉字 s，
            // 利用动态规划算法从前往后，推出每个拼音汉字状态的概率 viterbi[i+1][s]
            currentWordIds.forEach((currentWordId) -> {
                Object[] result = prevWordIds.stream().map((prevWordId) -> {
                    double prob = 0;

                    // 句首字的初始概率 = math.log(句首字出现次数 / 训练数据的句子总数)
                    if (currentIndex == 0) {
                        prob += calcViterbiProb(
                                // 句首字的出现次数
                                getTransProbValue(transProb, currentWordId, EOS_BOS_WORD_ID), //
                                basePhraseSize, minProb //
                        );
                    } else {
                        Object[] pair = viterbi[prevIndex].get(prevWordId);
                        assert pair != null;

                        prob += (double) pair[0];
                    }

                    prob += calcViterbiProb(
                            // 前序拼音字的出现次数
                            getTransProbValue(transProb, currentWordId, prevWordId),
                            // 当前拼音字的转移总数
                            getTransProbValue(transProb, currentWordId, TOTAL_WORD_ID), //
                            minProb //
                    );

                    // 加上末尾字的转移概率
                    if (currentIndex == lastIndex) {
                        prob += calcViterbiProb(
                                //
                                getTransProbValue(transProb, EOS_BOS_WORD_ID, currentWordId), //
                                basePhraseSize, minProb //
                        );
                    }

                    return new Object[] { prob, prevWordId };
                }).reduce(null, (acc, pair) -> //
                        acc == null || ((double) acc[0]) < ((double) pair[0]) ? pair : acc //
                );

                currentWordViterbi.put(currentWordId, result);
            });
        }

        return viterbi;
    }

    private static int getTransProbValue(
            Map<String, Map<String, Integer>> transProb, String currWordId, String prevWordId
    ) {
        Map<String, Integer> prob = transProb.get(currWordId);
        if (prob == null) {
            return 0;
        }

        Integer value = prob.get(prevWordId);
        if (value == null) {
            return 0;
        }
        return value;
    }

    private static double calcViterbiProb(int count, int total, double min) {
        return count == 0 || total == 0 ? min : Math.log(count * 1.0 / total);
    }

    // ============================= Start: 数据库升级 ==============================

    /** 数据库从 v2 版本升级到 v3 版本 */
    private void upgrade_v2_to_v3() {
        File appPhraseDBFile = getDBFile(this.context, DictDBType.app_phrase);

        File userDBFile = getDBFile(this.context, DictDBType.user);
        // 使用应用的字典库作为迁移库
        File transferDBFile = getDBFile(this.context, DictDBType.app_word);

        // Note: SQLite 数据库只有复制到本地才能进行 SQL 操作
        copySQLite(this.context, appPhraseDBFile, R.raw.pinyin_phrase_dict);
        copySQLite(this.context, transferDBFile, R.raw.pinyin_word_dict);

        try (
                SQLiteDatabase transferDB = openSQLite(transferDBFile, false);
        ) {
            // <<<<<<<<<<<<<<<< 初始化
            String[] clauses = new String[] {
                    // 连接应用和用户库
                    "attach database '" + appPhraseDBFile.getAbsolutePath() + "' as app;",
                    "attach database '" + userDBFile.getAbsolutePath() + "' as user;",
                    // 创建包含用户和应用权重数据的词典表
                    "create table" //
                    + "  if not exists phrase_word ("
                    //  -- 拼音字 id: 其为 link_word_with_pinyin 中的 id_
                    + "    word_id_ integer not null,"
                    //  -- 拼音字母组合 id: 其为 link_word_with_pinyin 中的 spell_chars_id_
                    + "    spell_chars_id_ integer not null,"
                    // -- 应用字典中短语内的字权重：出现次数
                    + "    weight_app_ integer not null,"
                    // -- 用户字典中短语内的字权重：出现次数
                    + "    weight_user_ integer not null," //
                    + "    primary key (word_id_, spell_chars_id_)" + "  );",
                    //
                    "create table" //
                    + "  if not exists phrase_trans_prob ("
                    //  -- 当前拼音字 id: EOS 用 -1 代替（句尾字）
                    //  -- Note：其为字典库中 link_word_with_pinyin 中的 id_
                    + "    word_id_ integer not null,"
                    //  -- 前序拼音字 id: BOS 用 -1 代替（句首字），TOTAL 用 -2 代替
                    //  -- Note：其为字典库中 link_word_with_pinyin 中的 id_
                    + "    prev_word_id_ integer not null,"
                    //  -- 当 word_id_ == -1 且 prev_word_id_ == -2 时，其代表训练数据的句子总数，用于计算句首字出现频率；
                    //  -- 当 word_id_ == -1 且 prev_word_id_ != -1 时，其代表末尾字出现次数；
                    //  -- 当 word_id_ != -1 且 prev_word_id_ == -1 时，其代表句首字出现次数；
                    //  -- 当 word_id_ != -1 且 prev_word_id_ == -2 时，其代表当前拼音字的转移总数；
                    //  -- 当 word_id_ != -1 且 prev_word_id_ != -1 时，其代表前序拼音字的出现次数；
                    // -- 应用字典中字出现的次数
                    + "    value_app_ integer not null,"
                    // -- 用户字典中字出现的次数
                    + "    value_user_ integer not null," //
                    + "    primary key (word_id_, prev_word_id_)" + "  );",
                    // 添加用户库表所需字段
                    "alter table meta_emoji"
                    // -- 按使用频率等排序的权重
                    + "  add column weight_user_ integer default 0",
                    //
                    "create table" //
                    + "  if not exists meta_latin (" //
                    + "    id_ integer not null primary key,"
                    // -- 拉丁文内容
                    + "    value_ text not null,"
                    // -- 按使用频率等排序的权重
                    + "    weight_user_ integer not null," //
                    + "    unique (value_)" //
                    + "  );",
                    // 通过 SQL 迁移数据
                    "insert into phrase_word"
                    + "  (word_id_, spell_chars_id_, weight_app_, weight_user_)"
                    + "select"
                    + "  word_id_, spell_chars_id_,"
                    + "  app_.weight_ as weight_app_,"
                    + "  0 as weight_user_"
                    + "from app.phrase_word as app_;",
                    //
                    "insert into phrase_trans_prob"
                    + "  (word_id_, prev_word_id_, value_app_, value_user_)"
                    + "select"
                    + "  word_id_, prev_word_id_,"
                    + "  app_.value_ as value_app_,"
                    + "  0 as value_user_"
                    + "from app.phrase_trans_prob as app_;",
                    //
                    "update meta_emoji as emoji_"
                    + "  set emoji_.weight_user_ = user_.weight_"
                    + "  from user.used_emoji as user_"
                    + "    where user_.id_ = emoji_.id_",
                    //
                    "insert into meta_latin"
                    + "  (id_, value_, weight_user_)"
                    + "select"
                    + "  user_.id_, user_.value_, user_.weight_"
                    + "from user.used_latin as user_;",
                    };
            execSQLite(transferDB, clauses);
            // >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>

            // <<<<<<<<<<<<< 获取用户输入短语出现次数
            // {'<字 id>:<字读音字母组合 id>,...': 10, ...}
            Map<String, Integer> usedPhraseCountMap = new HashMap<>();
            // Note：视图 used_pinyin_phrase 已默认按 target_index_ 升序排序，
            // 组合后的 target_id_ 与短语字的顺序是一致的
            querySQLite(transferDB, new SQLiteQueryParams<Void>() {{
                this.table = "user.used_pinyin_phrase";
                this.columns = new String[] {
                        "group_concat("  //
                        + "  concat(target_id_, ':', target_spell_chars_id_)"  //
                        + ", ',') as target_ids_", //
                        "weight_"
                };
                this.where = "weight_ > 0 ";
                this.groupBy = "source_id_";
                this.reader = (cursor) -> {
                    String phraseWords = cursor.getString(0);
                    int phraseCount = cursor.getInt(1);

                    usedPhraseCountMap.put(phraseWords, phraseCount);
                    return null;
                };
            }});
            // >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>

            // <<<<<<<<<<<<<<<<<<<<<<<<< 更新用户输入短语权重
            HMM hmm = HMM.calcTransProb(usedPhraseCountMap);
            updateHMM(transferDB, hmm, false);
            // >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>

            execSQLite(transferDB, new String[] {
                    // 回收无用空间
                    "vacuum;",
                    });
        }

        // TODO 待删除 - 测试备份
        File userDBBakFile = new File(userDBFile.getParentFile(), userDBFile.getName() + ".bak");
        if (!userDBBakFile.exists()) {
            FileUtils.moveFile(userDBFile, userDBBakFile);
        }

        // 迁移库就地转换为用户库
        FileUtils.moveFile(transferDBFile, userDBFile);
    }
    // ============================= End: 数据库升级 ==============================
}
