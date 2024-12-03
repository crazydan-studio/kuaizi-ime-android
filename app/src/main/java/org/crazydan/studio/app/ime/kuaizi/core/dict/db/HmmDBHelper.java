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
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import android.database.sqlite.SQLiteDatabase;
import org.crazydan.studio.app.ime.kuaizi.core.dict.hmm.Hmm;
import org.crazydan.studio.app.ime.kuaizi.core.dict.hmm.Viterbi;
import org.crazydan.studio.app.ime.kuaizi.core.input.PinyinWord;
import org.crazydan.studio.app.ime.kuaizi.utils.DBUtils;

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
    private static final Integer WORD_TOTAL = -2;
    /** 代表 {@link Hmm#EOS} 和 {@link Hmm#BOS} 的字 */
    private static final Integer WORD_EOS_BOS = -1;
    /** 代表 未收录 的字，其没有对应的拼音字 */
    private static final Integer WORD_IGNORED = -10;

    /** @see #predictPinyinPhrase(SQLiteDatabase, List, Map, int, int) */
    public static List<Integer[]> predictPinyinPhrase(
            SQLiteDatabase db, List<Integer> pinyinCharsIdList, int userPhraseBaseWeight, int top
    ) {
        return predictPinyinPhrase(db, pinyinCharsIdList, null, userPhraseBaseWeight, top);
    }

    /**
     * 根据拼音的字母组合得到前 N 个最佳预测结果
     *
     * @param pinyinCharsIdList
     *         拼音的字母组合 id 列表
     * @param confirmedPhraseWords
     *         已经确认位置的拼音字 id，已确认的字将会影响其前后相邻字的预测结果。
     *         为 null 或空时，表示无已确认的拼音字
     * @param userPhraseBaseWeight
     *         用户词组数据的基础权重，以确保用户输入权重大于应用词组数据
     * @param top
     *         最佳预测结果数
     * @return 列表元素为 短语的拼音字 id 数组，且列表中最靠前的为预测结果权重最高的短语
     */
    public static List<Integer[]> predictPinyinPhrase(
            SQLiteDatabase db, //
            List<Integer> pinyinCharsIdList, Map<Integer, Integer> confirmedPhraseWords, //
            int userPhraseBaseWeight, int top
    ) {
        if (pinyinCharsIdList.isEmpty() || top < 1) {
            return List.of();
        }

        // 取出 HMM 字间转移概率
        Map<Integer, Map<Integer, Integer>> transProb = new HashMap<>();
        Map<Integer, Set<Integer>> pinyinCharsIdAndWordIdsMap = new HashMap<>(pinyinCharsIdList.size());

        queryTransProb(db, pinyinCharsIdList, (row) -> {
            Integer wordId = row.getInt("word_id_");
            Integer preWordId = row.getInt("prev_word_id_");
            int pinyinCharsId = row.getInt("word_spell_chars_id_");
            int appValue = row.getInt("value_app_");
            int userValue = row.getInt("value_user_");

            Map<Integer, Integer> prob = transProb.computeIfAbsent(wordId, (k) -> new HashMap<>());
            prob.compute(preWordId, (k, v) -> (v == null ? 0 : v) //
                                              + appValue + userValue
                                              // 用户数据需加上基础权重
                                              + (userValue > 0 ? userPhraseBaseWeight : 0));

            if (pinyinCharsId >= 0) {
                pinyinCharsIdAndWordIdsMap.computeIfAbsent(pinyinCharsId, (k) -> new HashSet<>()).add(wordId);
            }
        });

//        Log.i(LOG_TAG, "TransProb: " + new Gson().toJson(transProb));

        // 计算 viterbi 矩阵
        Map<Integer, Object[]>[] viterbi = calcViterbi(pinyinCharsIdList, transProb, new Viterbi.Options() {{
            this.wordTotal = WORD_TOTAL;
            this.wordBos = WORD_EOS_BOS;
            this.wordEos = WORD_EOS_BOS;

            this.wordsGetter = (spell, index) -> {
                Integer confirmed = confirmedPhraseWords != null ? confirmedPhraseWords.get(index) : null;
                if (confirmed != null) {
                    return Set.of(confirmed);
                }
                // Note: 在词典表中未收录的拼音，直接返回 WORD_IGNORED，以表示待忽略字
                return pinyinCharsIdAndWordIdsMap.getOrDefault(spell, Set.of(WORD_IGNORED));
            };
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
     * <p/>
     * 注意，{@link Hmm} 中的字数据应该为<code>'拼音字 id' + ':' + '拼音字母组合 id'</code>
     *
     * @param reverse
     *         是否反向更新，即，减掉 HMM 数据
     */
    public static void saveHmm(SQLiteDatabase db, Hmm hmm, boolean reverse) {
        // =============================================================================
        // @return ['word_id_', 'spell_chars_id_']
        Function<String, String[]> extractWordIds = (s) -> s.split(":");

        Function<Boolean, List<String[]>> phraseWordDataGetter = //
                (updated) -> hmm.wordWeight.keySet().stream().map((key) -> {
                    String[] wordIds = extractWordIds.apply(key);
                    String val = hmm.wordWeight.get(key) + "";

                    return updated ? new String[] { val, wordIds[0] } //
                                   : new String[] { val, wordIds[0], wordIds[1] };
                }).collect(Collectors.toList());

        if (!reverse) {
            // Note: SQLite 3.24.0 版本才支持 upsert
            // https://www.sqlite.org/lang_upsert.html#history
//            execSQLite(db,
//                       "insert into phrase_word ("
//                       + "   word_id_, spell_chars_id_,"
//                       + "   weight_app_, weight_user_"
//                       + " ) values(?, ?, 0, ?)"
//                       + " on conflict(word_id_)"
//                       + " do update set "
//                       + "   weight_user_ = weight_user_ + ?",
//                       phraseWordData);
            upsertSQLite(db, new SQLiteRawUpsertParams() {{
                // Note: 确保更新和新增的参数位置相同
                this.updateSQL = "update phrase_word" //
                                 + " set weight_user_ = weight_user_ + ?" //
                                 + " where word_id_ = ?";
                this.insertSql = "insert into phrase_word ("
                                 + "   weight_app_, weight_user_,"
                                 + "   word_id_, spell_chars_id_"
                                 + " ) values (0, ?, ?, ?)";

                this.insertParamsList = phraseWordDataGetter.apply(false);
                this.updateParamsGetter = (i) -> Arrays.copyOf(this.insertParamsList.get(i), 2);
            }});
        } else {
            execSQLite(db, "update phrase_word" //
                           + " set weight_user_ = max(weight_user_ - ?, 0)" //
                           + " where word_id_ = ?", phraseWordDataGetter.apply(true));
        }

        // ==============================================================================
        Function<String, String[]> getWordId = (s) -> {
            // EOS 用 -1 代替（句尾字）
            // BOS 用 -1 代替（句首字）
            // TOTAL 用 -2 代替（句子总数）
            if (Hmm.EOS.equals(s) || Hmm.BOS.equals(s)) {
                return new String[] { WORD_EOS_BOS + "", WORD_EOS_BOS + "" };
            } else if (Hmm.TOTAL.equals(s)) {
                return new String[] { WORD_TOTAL + "", WORD_TOTAL + "" };
            }

            return extractWordIds.apply(s);
        };

        Function<Boolean, List<String[]>> phraseTransProbDataGetter = //
                (updated) -> {
                    List<String[]> phraseTransProbData = new ArrayList<>();
                    hmm.transProb.forEach((curr, prob) -> {
                        String[] currIds = getWordId.apply(curr);

                        prob.forEach((prev, value) -> {
                            String[] prevIds = getWordId.apply(prev);
                            String val = value + "";

                            phraseTransProbData.add(updated
                                                    ? new String[] { val, currIds[0], prevIds[0] }
                                                    : new String[] {
                                                            val, currIds[0], prevIds[0], //
                                                            currIds[1], prevIds[1]
                                                    });
                        });
                    });
                    return phraseTransProbData;
                };

        if (!reverse) {
            // Note: SQLite 3.24.0 版本才支持 upsert
            // https://www.sqlite.org/lang_upsert.html#history
//            execSQLite(db,
//                       "insert into phrase_trans_prob ("
//                       + "   word_id_, prev_word_id_,"
//                       + "   word_spell_chars_id_, prev_word_spell_chars_id_,"
//                       + "   value_app_, value_user_"
//                       + " ) values(?, ?, ?, ?, 0, ?)"
//                       + " on conflict(word_id_, prev_word_id_)"
//                       + " do update set "
//                       + "   value_user_ = value_user_ + ?",
//                       phraseTransProbData);
            upsertSQLite(db, new SQLiteRawUpsertParams() {{
                // Note: 确保更新和新增的参数位置相同
                this.updateSQL = "update phrase_trans_prob"
                                 + " set value_user_ = value_user_ + ?"
                                 + " where word_id_ = ? and prev_word_id_ = ?";
                this.insertSql = "insert into phrase_trans_prob ("
                                 + "   value_app_, value_user_,"
                                 + "   word_id_, prev_word_id_,"
                                 + "   word_spell_chars_id_, prev_word_spell_chars_id_"
                                 + " ) values (0, ?, ?, ?, ?, ?)";

                this.insertParamsList = phraseTransProbDataGetter.apply(false);
                this.updateParamsGetter = (i) -> Arrays.copyOf(this.insertParamsList.get(i), 3);
            }});
        } else {
            execSQLite(db,
                       "update phrase_trans_prob"
                       + " set value_user_ = max(value_user_ - ?, 0)"
                       + " where word_id_ = ? and prev_word_id_ = ?",
                       phraseTransProbDataGetter.apply(true));
        }

        if (reverse) {
            // 清理无用数据
            execSQLite(db,
                       "delete from phrase_word where weight_app_ = 0 and weight_user_ = 0",
                       "delete from phrase_trans_prob where value_app_ = 0 and value_user_ = 0");
        }
    }

    /** 计算给定短语的 {@link Hmm#transProb} 数据 */
    private static Hmm calcTransProb(List<PinyinWord> phrase) {
        return Hmm.calcTransProb(phrase.stream()
                                       // 以 拼音字 id 与 拼音字母组合 id 代表短语中的字
                                       .map(word -> word.getId() + ":" + word.getCharsId())
                                       .collect(Collectors.toList()));
    }

    private static void queryTransProb(
            SQLiteDatabase db, List<Integer> spellCharsIdList, Consumer<DBUtils.SQLiteRow> consumer
    ) {
        // 短语前后序拼音组合
        List<Integer[]> charsIdPairList = new ArrayList<>(spellCharsIdList.size() + 1);
        for (int i = 0; i <= spellCharsIdList.size(); i++) {
            Integer prevCharsId = i == 0 ? WORD_EOS_BOS : spellCharsIdList.get(i - 1);
            Integer currCharsId = i == spellCharsIdList.size() ? WORD_EOS_BOS : spellCharsIdList.get(i);

            charsIdPairList.add(new Integer[] { prevCharsId, currCharsId });
            // 当前拼音字都需包含 TOTAL 列，以得到其转移总数
            charsIdPairList.add(new Integer[] { WORD_TOTAL, currCharsId });
        }

        rawQuerySQLite(db, new SQLiteRawQueryParams<Void>() {{
            // Note: 直接拼接参数，以避免参数解析
            this.sql = "select distinct *" //
                       + " from phrase_trans_prob" //
                       + " where "
                       // Note: 低版本不支持 where (a, b) in ((1, 2), (3, 4), ...) 形式，只能采用 or 实现
                       + charsIdPairList.stream()
                                        .map(pair -> "(prev_word_spell_chars_id_, word_spell_chars_id_)" //
                                                     + (" = (" + pair[0] + ", " + pair[1] + ")"))
                                        .collect(Collectors.joining(" or "));

            this.voidReader = consumer;
        }});
    }
}
