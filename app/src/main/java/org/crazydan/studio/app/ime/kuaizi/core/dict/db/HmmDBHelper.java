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

package org.crazydan.studio.app.ime.kuaizi.core.dict.db;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import android.database.sqlite.SQLiteDatabase;
import org.crazydan.studio.app.ime.kuaizi.core.dict.hmm.Hmm;
import org.crazydan.studio.app.ime.kuaizi.core.dict.hmm.Viterbi;
import org.crazydan.studio.app.ime.kuaizi.core.input.PinyinWord;

import static org.crazydan.studio.app.ime.kuaizi.core.dict.hmm.Viterbi.calcViterbi;
import static org.crazydan.studio.app.ime.kuaizi.core.dict.hmm.Viterbi.getBestPhraseFromViterbi;
import static org.crazydan.studio.app.ime.kuaizi.utils.DBUtils.SQLiteRawQueryParams;
import static org.crazydan.studio.app.ime.kuaizi.utils.DBUtils.SQLiteRawUpsertParams;
import static org.crazydan.studio.app.ime.kuaizi.utils.DBUtils.execSQLite;
import static org.crazydan.studio.app.ime.kuaizi.utils.DBUtils.rawQuerySQLite;
import static org.crazydan.studio.app.ime.kuaizi.utils.DBUtils.upsertSQLite;

/**
 * {@link Hmm} 数据库，提供对 HMM 数据的持久化处理接口
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2024-10-27
 */
public class HmmDBHelper {
    private static final String LOG_TAG = HmmDBHelper.class.getSimpleName();

    /** 代表 {@link Hmm#TOTAL} 的字 */
    private static final String WORD_TOTAL = "-2";
    /** 代表 {@link Hmm#EOS} 和 {@link Hmm#BOS} 的字 */
    private static final String WORD_EOS_BOS = "-1";

    /**
     * 根据拼音的字母组合得到前 N 个最佳预测结果
     *
     * @param pinyinCharsIdList
     *         拼音的字母组合 id 列表
     * @param userPhraseBaseWeight
     *         用户词组数据的基础权重，以确保用户输入权重大于应用词组数据
     * @param top
     *         最佳预测结果数
     * @return 列表元素为 短语的拼音字 id 数组，且列表中最靠前的为预测结果权重最高的短语
     */
    public static List<String[]> predictPinyinPhrase(
            SQLiteDatabase db, List<String> pinyinCharsIdList, int userPhraseBaseWeight, int top
    ) {
        if (pinyinCharsIdList.isEmpty() || top < 1) {
            return List.of();
        }

        // 取出 HMM 字间转移概率
        Map<String, Map<String, Integer>> transProb = new HashMap<>();
        Map<String, Set<String>> pinyinCharsIdAndWordIdsMap = new HashMap<>(pinyinCharsIdList.size());

        rawQuerySQLite(db, new SQLiteRawQueryParams<Void>() {{
            // 查询结果列包括：word_id_, prev_word_id_, word_spell_chars_id_, value_app_, value_user_
            this.sql = createTransProbQuerySQL(pinyinCharsIdList);

            this.reader = (row) -> {
                String wordId = row.getString("word_id_");
                String preWordId = row.getString("prev_word_id_");
                String pinyinCharsId = row.getString("word_spell_chars_id_");
                Integer appValue = row.getInt("value_app_");
                Integer userValue = row.getInt("value_user_");

                Map<String, Integer> prob = transProb.computeIfAbsent(wordId, (k) -> new HashMap<>());
                prob.compute(preWordId, (k, v) -> (v == null ? 0 : v) //
                                                  + appValue + userValue
                                                  // 用户数据需加上基础权重
                                                  + (userValue > 0 ? userPhraseBaseWeight : 0));

                if (pinyinCharsId != null) {
                    pinyinCharsIdAndWordIdsMap.computeIfAbsent(pinyinCharsId, (k) -> new HashSet<>()).add(wordId);
                }

                return null;
            };
        }});

//        Log.i(LOG_TAG, "TransProb: " + new Gson().toJson(transProb));

        // 计算 viterbi 矩阵
        Map<String, Object[]>[] viterbi = calcViterbi(pinyinCharsIdList, transProb, new Viterbi.Options() {{
            this.wordTotal = WORD_TOTAL;
            this.wordBos = WORD_EOS_BOS;
            this.wordEos = WORD_EOS_BOS;
            this.spellAndWordsMap = pinyinCharsIdAndWordIdsMap;
        }});

//        Log.i(LOG_TAG, "Viterbi: " + new Gson().toJson(viterbi));

        // 取出最佳短语
        return getBestPhraseFromViterbi(viterbi, pinyinCharsIdList.size(), top);
    }

    /**
     * 保存用户输入的拼音短语
     *
     * @param reverse
     *         是否反向操作，即，撤销对输入短语的保存
     */
    public static void saveUsedPinyinPhrase(SQLiteDatabase db, List<PinyinWord> phrase, boolean reverse) {
        if (phrase.isEmpty()) {
            return;
        }

        Hmm hmm = calcTransProb(phrase);
        saveHmm(db, hmm, reverse);
    }

    /**
     * 更新 {@link Hmm} 数据
     *
     * @param reverse
     *         是否反向更新，即，减掉 HMM 数据
     */
    public static void saveHmm(SQLiteDatabase db, Hmm hmm, boolean reverse) {
        // 采用 SQLite 的 UPSET 机制插入或更新数据：https://www.sqlite.org/lang_upsert.html

        // =============================================================================
        Function<String, String[]> extractWordIds = (s) -> s.split(":");

        List<String[]> phraseWordData = hmm.wordWeight.keySet().stream().map((key) -> {
            String[] wordIds = extractWordIds.apply(key);
            String val = hmm.wordWeight.get(key) + "";

            return new String[] { val, wordIds[0], wordIds[1] };
        }).collect(Collectors.toList());

        if (!reverse) {
            // Note: SQLite 3.24.0 版本才支持 upsert
            // https://www.sqlite.org/lang_upsert.html#history
//            execSQLite(db,
//                       "insert into"
//                       + " phrase_word(word_id_, spell_chars_id_, weight_app_, weight_user_)"
//                       + "   values(?, ?, 0, ?)"
//                       + " on conflict(word_id_, spell_chars_id_)"
//                       + " do update set "
//                       + "   weight_user_ = weight_user_ + ?",
//                       phraseWordData);
            upsertSQLite(db, new SQLiteRawUpsertParams() {{
                // Note: 确保更新和新增的参数位置相同
                this.updateSQL = "update phrase_word"
                                 + " set weight_user_ = weight_user_ + ?"
                                 + " where word_id_ = ? and spell_chars_id_ = ?";
                this.insertSql = "insert into"
                                 + " phrase_word(weight_app_, weight_user_, word_id_, spell_chars_id_)"
                                 + "   values(0, ?, ?, ?)";
                this.updateParamsList = this.insertParamsList = phraseWordData;
            }});
        } else {
            execSQLite(db,
                       "update phrase_word"
                       + " set weight_user_ = max(weight_user_ - ?, 0)"
                       + " where word_id_ = ? and spell_chars_id_ = ?",
                       phraseWordData);
        }

        // ==============================================================================
        Function<String, String> getWordId = (s) -> {
            // EOS 用 -1 代替（句尾字）
            // BOS 用 -1 代替（句首字）
            // TOTAL 用 -2 代替（句子总数）
            if (Hmm.EOS.equals(s) || Hmm.BOS.equals(s)) {
                return WORD_EOS_BOS;
            } else if (Hmm.TOTAL.equals(s)) {
                return WORD_TOTAL;
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

                phraseTransProbData.add(new String[] { val, currId, prevId });
            });
        });

        if (!reverse) {
            // Note: SQLite 3.24.0 版本才支持 upsert
            // https://www.sqlite.org/lang_upsert.html#history
//            execSQLite(db,
//                       "insert into"
//                       + " phrase_trans_prob(word_id_, prev_word_id_, value_app_, value_user_)"
//                       + "   values(?, ?, 0, ?)"
//                       + " on conflict(word_id_, prev_word_id_)"
//                       + " do update set "
//                       + "   value_user_ = value_user_ + ?",
//                       phraseTransProbData);
            upsertSQLite(db, new SQLiteRawUpsertParams() {{
                // Note: 确保更新和新增的参数位置相同
                this.updateSQL = "update phrase_trans_prob"
                                 + " set value_user_ = value_user_ + ?"
                                 + " where word_id_ = ? and prev_word_id_ = ?";
                this.insertSql = "insert into"
                                 + " phrase_trans_prob(value_app_, value_user_, word_id_, prev_word_id_)"
                                 + "   values(0, ?, ?, ?)";
                this.updateParamsList = this.insertParamsList = phraseTransProbData;
            }});
        } else {
            execSQLite(db,
                       "update phrase_trans_prob"
                       + " set value_user_ = max(value_user_ - ?, 0)"
                       + " where word_id_ = ? and prev_word_id_ = ?",
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

    /** 计算给定短语的 {@link Hmm#transProb} 数据 */
    private static Hmm calcTransProb(List<PinyinWord> phrase) {
        return Hmm.calcTransProb(phrase.stream()
                                       // 以 拼音字 id 与 拼音字母组合 id 代表短语中的字
                                       .map(word -> word.getId() + ":" + word.getCharsId())
                                       .collect(Collectors.toList()));
    }

    /** 构造 {@link Hmm#transProb} 数据查询 SQL */
    private static String createTransProbQuerySQL(List<String> spellCharsIdList) {
        List<String> charsIdList = new ArrayList<>(spellCharsIdList);

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
               + "   s_.word_id_, s_.prev_word_id_,"
               + "   t_.curr_word_spell_chars_id_ as word_spell_chars_id_,"
               + "   s_.value_app_, s_.value_user_"
               + " from phrase_trans_prob s_, word_ids t_"
               + " where"
               + "   s_.word_id_ = t_.curr_word_id_"
               + "   and ("
               + "     s_.prev_word_id_ = t_.prev_word_id_"
               // 当前拼音字都包含 TOTAL 列
               + ("    or s_.prev_word_id_ = " + WORD_TOTAL)
               + "   )";
    }
}
