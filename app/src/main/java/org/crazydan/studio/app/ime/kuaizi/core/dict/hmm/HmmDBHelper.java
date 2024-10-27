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

package org.crazydan.studio.app.ime.kuaizi.core.dict.hmm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import android.database.sqlite.SQLiteDatabase;
import org.crazydan.studio.app.ime.kuaizi.core.InputWord;
import org.crazydan.studio.app.ime.kuaizi.core.input.PinyinInputWord;
import org.crazydan.studio.app.ime.kuaizi.utils.DBUtils;

import static org.crazydan.studio.app.ime.kuaizi.utils.DBUtils.execSQLite;
import static org.crazydan.studio.app.ime.kuaizi.utils.DBUtils.rawQuerySQLite;

/**
 * {@link Hmm} 数据库，提供对 HMM 数据的持久化处理接口
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2024-10-27
 */
public class HmmDBHelper {
    /** 代表 {@link Hmm#TOTAL} 的字 id */
    public static final String TOTAL_WORD_ID = "-2";
    /** 代表 {@link Hmm#EOS} 和 {@link Hmm#BOS} 的字 id */
    public static final String EOS_BOS_WORD_ID = "-1";

    /**
     * 根据拼音的字母组合得到前 N 个最佳预测结果
     *
     * @param pinyinCharsIdList
     *         拼音的字母组合 id 列表
     * @param userPhraseBaseWeight
     *         用户词组数据的基础权重，以确保用户输入权重大于应用词组数据
     * @param top
     *         最佳预测结果数
     * @return 元素为 短语的字数组 的列表，最靠前的为最佳预测的短语
     */
    public static List<String[]> predictPhrase(
            SQLiteDatabase db, List<String> pinyinCharsIdList, int userPhraseBaseWeight, int top
    ) {
        if (pinyinCharsIdList.isEmpty() || top < 1) {
            return List.of();
        }

        Map<String, Map<String, Integer>> transProb = new HashMap<>();
        Map<String, Set<String>> pinyinCharsIdAndWordIdsMap = new HashMap<>(pinyinCharsIdList.size());

        rawQuerySQLite(db, new DBUtils.SQLiteRawQueryParams<Void>() {{
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
                                                  + (userValue > 0 ? userPhraseBaseWeight : 0));

                if (pinyinCharsId != null) {
                    pinyinCharsIdAndWordIdsMap.computeIfAbsent(pinyinCharsId, (k) -> new HashSet<>()).add(wordId);
                }

                return null;
            };
        }});

        // <<<<<<<<<<<<<<<
        Map<String, Object[]>[] viterbi = calcViterbi(pinyinCharsIdList, transProb, pinyinCharsIdAndWordIdsMap);

        // <<<<<<<<<<<<< 对串进行回溯即可得对应拼音的汉字
        int lastIndex = pinyinCharsIdList.size() - 1;

        // 结构: viterbiWords[n] = [[probability, s], ...]，其每一纵列是一个短语的字数组
        Object[][][] viterbiWords = new Object[lastIndex + 1][][];

        // Note：取概率最大的前 top 个末尾汉字
        viterbiWords[lastIndex] = viterbi[lastIndex].keySet().stream().map((wordId) -> {
            Object[] pair = viterbi[lastIndex].get(wordId);
            assert pair != null;

            double prob = (double) pair[0];
            return new Object[] { prob, wordId };
        }).sorted((a, b) -> //
                          Double.compare(((double) b[0]), ((double) a[0])) //
        ).limit(top).toArray(Object[][]::new);

        for (int i = lastIndex - 1; i > -1; i--) {
            int currIndex = i;
            int nextIndex = currIndex + 1;

            viterbiWords[currIndex] = Arrays.stream(viterbiWords[nextIndex]).map((pair) -> {
                String wordId = (String) pair[1];
                Object[] newPair = viterbi[nextIndex].get(wordId);
                assert newPair != null;

                return newPair;
            }).toArray(Object[][]::new);
        }

        // <<<<<<<<<<<< 获取最佳预测的短语列表
        List<String[]> phrases = new ArrayList<>(top);
        // Note: 需做二维数组的行列翻转才能得到短语的字数组
        for (int i = 0; i < viterbiWords.length; i++) {
            Object[][] viterbiWord = viterbiWords[i];

            for (int j = 0; j < viterbiWord.length; j++) {
                String[] phraseWord;
                if (i == 0) {
                    phraseWord = new String[viterbiWords.length];
                    phrases.add(phraseWord);
                } else {
                    phraseWord = phrases.get(j);
                }

                String wordId = (String) viterbiWord[j][1];
                phraseWord[i] = wordId;
            }
        }

        return phrases;
    }

    /**
     * 保存用户输入的拼音短语
     *
     * @param reverse
     *         是否反向操作，即，撤销对输入短语的保存
     */
    public static void savePhrase(SQLiteDatabase db, List<InputWord> phrase, boolean reverse) {
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
            if (Hmm.EOS.equals(s) || Hmm.BOS.equals(s)) {
                return EOS_BOS_WORD_ID;
            } else if (Hmm.TOTAL.equals(s)) {
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

    /** 计算给定短语的 {@link Hmm#transProb} 数据 */
    private static Hmm calcTransProb(List<InputWord> phrase) {
        return Hmm.calcTransProb(phrase.stream()
                                       // 以 拼音字 id 与 拼音字母组合 id 代表短语中的字
                                       .map(word -> word.getUid() + ":" + ((PinyinInputWord) word).getCharsId())
                                       .collect(Collectors.toList()));
    }

    /** 构造 {@link Hmm#transProb} 数据查询 SQL */
    private static String createTransProbQuerySQL(List<String> pinyinCharsIdList) {
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

        // 句子总数: word_id_ == -1 且 prev_word_id_ == -2
        int phraseTotal = getTransProbValue(transProb, EOS_BOS_WORD_ID, TOTAL_WORD_ID);

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

            // 遍历 current_word_ids 和 prev_word_ids，找出所有可能与当前拼音相符的汉字 curr，
            // 利用动态规划算法从前往后，推出每个拼音汉字状态的概率 viterbi[i+1][curr] = {prob, prev}
            currentWordIds.forEach((currentWordId) -> {
                Object[] result = prevWordIds.stream().map((prevWordId) -> {
                    double prob = 0;

                    // 句首字的初始概率 = math.log(句首字出现次数 / 句子总数)
                    if (currentIndex == 0) {
                        prob += calcViterbiProb(
                                // 句首字的出现次数
                                getTransProbValue(transProb, currentWordId, EOS_BOS_WORD_ID), //
                                phraseTotal, minProb //
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
                                phraseTotal, minProb //
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
}
