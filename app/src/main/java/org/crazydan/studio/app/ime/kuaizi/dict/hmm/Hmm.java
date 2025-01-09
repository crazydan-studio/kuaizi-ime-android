/*
 * 筷字输入法 - 高效编辑需要又好又快的输入法
 * Copyright (C) 2025 Crazydan Studio <https://studio.crazydan.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.
 * If not, see <https://www.gnu.org/licenses/lgpl-3.0.en.html#license-text>.
 */

package org.crazydan.studio.app.ime.kuaizi.dict.hmm;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * - 《基于 HMM 的拼音输入法》：https://zhuanlan.zhihu.com/p/508599305<br>
 * - 《自制输入法：拼音输入法与 HMM》: https://elliot00.com/posts/input-method-hmm
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2024-10-20
 */
public class Hmm {
    /** 短语句尾符号 */
    public static final String EOS = "EOS";
    /** 短语句首符号 */
    public static final String BOS = "BOS";
    /** 短语总数 */
    public static final String TOTAL = "TOTAL";

    /** 汉字权重，其结构为 <code>{'字': 出现次数, ...}</code> */
    public final Map<String, Integer> wordWeight = new HashMap<>();
    /** 汉字（状态）间转移概率，其结构为 <code>{'当前字': {'前序字': 出现次数}, ...}</code> */
    public final Map<String, Map<String, Integer>> transProb = new HashMap<>();

    /**
     * 计算含出现次数的短语中的汉字（状态）间转移概率
     *
     * @param phraseCountMap
     *         结构为 <code>{'字1,字2,...': 出现次数}</code>
     */
    public static Hmm calcTransProb(Map<String, Integer> phraseCountMap) {
        Hmm hmm = new Hmm();

        phraseCountMap.forEach((phrase, count) -> {
            calcTransProb(hmm, List.of(phrase.split(",")), count);
        });

        return hmm;
    }

    /**
     * 计算单个短语中的汉字（状态）间转移概率
     *
     * @param phraseWordList
     *         短语中的字列表
     */
    public static Hmm calcTransProb(List<String> phraseWordList) {
        return calcTransProb(new Hmm(), phraseWordList, 1);
    }

    /**
     * 累积计算指定短语中的汉字（状态）间转移概率
     * <p/>
     * <code>转移概率 = math.log(前序字出现次数 / total)</code>
     * <p/>
     * 当前字为 EOS 且其前序为 BOS 的转移次数即为 训练的句子总数，
     * 而各个包含 BOS 前序的字即为句首字，且其出现次数即为 BOS 的值
     *
     * @param phraseWordList
     *         短语中的字列表
     * @param count
     *         短语出现的次数
     */
    private static Hmm calcTransProb(
            Hmm hmm, List<String> phraseWordList, Integer count
    ) {
        Map<String, Integer> phraseWordWeight = new HashMap<>();
        Map<String, Map<String, Integer>> phraseTransProb = new HashMap<>();

        int wordTotal = phraseWordList.size();
        for (int i = 0; i <= wordTotal; i++) {
            String curr = i == wordTotal ? EOS : phraseWordList.get(i);

            // 仅短语内字数大于 1 时，才计算转移概率
            if (wordTotal > 1) {
                String prev = i == 0 ? BOS : phraseWordList.get(i - 1);

                Map<String, Integer> prob = phraseTransProb.computeIfAbsent(curr, (k) -> new HashMap<>());
                // 前序字和总量需累加
                for (String key : new String[] { prev, TOTAL }) {
                    prob.compute(key, (k, v) -> (v == null ? 0 : v) + 1);
                }
            }

            if (i < wordTotal) {
                phraseWordWeight.compute(curr, (k, v) -> (v == null ? 0 : v) + 1);
            }
        }

        // 累积短语出现次数
        phraseTransProb.forEach((curr, prob) -> {
            Map<String, Integer> hmmProb = hmm.transProb.computeIfAbsent(curr, (k) -> new HashMap<>());

            prob.forEach((prev, val) -> {
                hmmProb.compute(prev, (k, v) -> (v == null ? 0 : v) + (val * count));
            });
        });

        phraseWordWeight.forEach((word, val) -> {
            hmm.wordWeight.compute(word, (k, v) -> (v == null ? 0 : v) + (val * count));
        });

        return hmm;
    }
}
