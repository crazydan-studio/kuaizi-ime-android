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

package org.crazydan.studio.app.ime.kuaizi.core.dict.predict;

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
public class HMM {
    /** 短语句尾符号 */
    public static final String EOS = "EOS";
    /** 短语句首符号 */
    public static final String BOS = "BOS";
    /** 短语总数 */
    public static final String TOTAL = "TOTAL";

    /**
     * 计算指定短语中的汉字（状态）间转移概率
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
    public static Map<String, Map<String, Integer>> calcTransProb(List<String> phraseWordList, Integer count) {
        Map<String, Map<String, Integer>> transProb = new HashMap<>();
        for (int i = 0; i <= phraseWordList.size(); i++) {
            String curr = i == phraseWordList.size() ? EOS : phraseWordList.get(i);
            String prev = i == 0 ? BOS : phraseWordList.get(i - 1);

            Map<String, Integer> prob = transProb.computeIfAbsent(curr, (k) -> new HashMap<>());

            for (String key : new String[] { prev, TOTAL }) {
                prob.put(key, prob.getOrDefault(key, 0) + 1);
            }
        }

        // 累积短语出现次数
        transProb.forEach((curr, prob) -> {
            prob.forEach((prev, val) -> {
                prob.put(prev, val * count);
            });
        });

        return transProb;
    }
}
