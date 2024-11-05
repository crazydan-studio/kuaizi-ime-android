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
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2024-10-31
 */
public class Viterbi {
    private static final String LOG_TAG = Viterbi.class.getSimpleName();

    public static class Options {
        /** 代表 {@link Hmm#TOTAL} 的字 */
        public String wordTotal;
        /** 代表 {@link Hmm#EOS} 的字 */
        public String wordEos;
        /** 代表 {@link Hmm#BOS} 的字 */
        public String wordBos;
        /** 读音与其所对应的可选字集合 Map */
        public Map<String, Set<String>> spellAndWordsMap;
    }

    /**
     * 从 Viterbi 矩阵中获取前 <code>top</code> 个最佳短语
     *
     * @param top
     *         最佳匹配结果数
     * @return 列表元素为 短语的拼音字 id 数组，且列表中最靠前的为匹配权重最高的短语
     */
    public static List<String[]> getBestPhraseFromViterbi(Map<String, Object[]>[] viterbi, int phraseSize, int top) {
        // <<<<<<<<<<<<< 对串进行回溯即可得对应拼音的汉字
        int lastIndex = phraseSize - 1;

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

//        Log.i(LOG_TAG, "Viterbi Words: " + new Gson().toJson(viterbiWords));

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
     * 计算 Viterbi 矩阵
     * <p/>
     * https://zh.wikipedia.org/wiki/%E7%BB%B4%E7%89%B9%E6%AF%94%E7%AE%97%E6%B3%95#.E4.BE.8B.E5.AD.90
     *
     * @param spellList
     *         读音列表
     * @param transProb
     *         汉字（状态）间转移概率
     */
    public static Map<String, Object[]>[] calcViterbi(
            List<String> spellList, Map<String, Map<String, Integer>> transProb, Options options
    ) {
        // https://github.com/wmhst7/THU_AI_Course_Pinyin
        int total = spellList.size();
        // 用于 log 平滑时所取的最小值，用于代替 0
        double minProb = -50;
        // pos 是目前节点的位置，word 为当前汉字即当前状态，
        // probability 为从 pre_word 上一汉字即上一状态转移到目前状态的概率
        // viterbi[pos][word] = (probability, pre_word)
        Map<String, Object[]>[] viterbi = new Map[total];

        // 句子总数: word_id_ == -1 且 prev_word_id_ == -2
        int phraseTotal = getTransProbValue(transProb, options.wordEos, options.wordTotal);

        int lastIndex = total - 1;
        for (int i = -1; i < lastIndex; i++) {
            int prevIndex = i;
            int currentIndex = prevIndex + 1;

            String currentSpell = spellList.get(currentIndex);
            Set<String> currentWords = options.spellAndWordsMap.get(currentSpell);
            assert currentWords != null;

            // Note：句首字的前序字设为 -1
            String prevSpell = prevIndex < 0 ? null : spellList.get(prevIndex);
            Set<String> prevWords = prevSpell == null
                                    ? Set.of(options.wordBos)
                                    : options.spellAndWordsMap.get(prevSpell);
            assert prevWords != null;

            if (viterbi[currentIndex] == null) {
                viterbi[currentIndex] = new HashMap<>();
            }
            Map<String, Object[]> currentWordViterbi = viterbi[currentIndex];

            // 遍历 current_words 和 prev_words，找出所有可能与当前拼音相符的汉字 curr_word，
            // 利用动态规划算法从前往后，推出每个拼音汉字状态的概率
            // viterbi[curr_index][curr_word] = {prob, prev}
            currentWords.forEach((currentWord) -> {
                Object[] result = prevWords.stream().map((prevWord) -> {
                    double prob = 0;

                    // 句首字的初始概率 = math.log(句首字出现次数 / 句子总数)
                    if (currentIndex == 0) {
                        prob += calcViterbiProb(
                                // 句首字的出现次数
                                getTransProbValue(transProb, currentWord, options.wordBos), //
                                phraseTotal, minProb //
                        );
                    } else {
                        Object[] pair = viterbi[prevIndex].get(prevWord);
                        assert pair != null;

                        prob += (double) pair[0];
                    }

                    prob += calcViterbiProb(
                            // 前序拼音字的出现次数
                            getTransProbValue(transProb, currentWord, prevWord),
                            // 当前拼音字的转移总数
                            getTransProbValue(transProb, currentWord, options.wordTotal), //
                            minProb //
                    );

                    // 加上末尾字的转移概率
                    if (currentIndex == lastIndex) {
                        prob += calcViterbiProb(
                                //
                                getTransProbValue(transProb, options.wordEos, currentWord), //
                                phraseTotal, minProb //
                        );
                    }

                    return new Object[] { prob, prevWord };
                }).reduce(null, (acc, pair) -> //
                        acc == null || Double.compare(((double) acc[0]), ((double) pair[0])) < 0 //
                        ? pair : acc //
                );

                currentWordViterbi.put(currentWord, result);
            });
        }

        return viterbi;
    }

    private static int getTransProbValue(
            Map<String, Map<String, Integer>> transProb, String currWord, String prevWord
    ) {
        Map<String, Integer> prob = transProb.get(currWord);
        if (prob == null) {
            return 0;
        }

        Integer value = prob.get(prevWord);
        if (value == null) {
            return 0;
        }
        return value;
    }

    private static double calcViterbiProb(int count, int total, double min) {
        return count == 0 || total == 0 ? min : Math.log(count * 1.0 / total);
    }
}
