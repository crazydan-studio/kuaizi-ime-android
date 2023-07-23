/*
 * 筷字输入法 - 高效编辑需要又好又快的输入法
 * Copyright (C) 2023 Crazydan Studio
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

package org.crazydan.studio.app.ime.kuaizi.internal.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 拼音字典
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-07-22
 */
public class PinyinDict {
    /** 字典内的所有 字与{@link Word 字信息} 的映射集合 */
    private final Map<String, Word> words = new HashMap<>();
    private final PinyinTree tree = new PinyinTree();

    public Map<String, Word> getWords() {
        return this.words;
    }

    public PinyinTree getTree() {
        return this.tree;
    }

    public void add(String word, String pinyin, Word wordInfo, float weight) {
        this.words.putIfAbsent(word, wordInfo);

        weight *= 3000;

        // Note: 确保笔画少的更靠前，且笔画相同的能够更靠近在一起，繁体靠最后，字型相近的能挨在一起
        if (wordInfo.hasSimple()) {
            weight -= 200;
        } else {
            weight += 100;
        }

        if (wordInfo.strokeOrder != null && !wordInfo.strokeOrder.isEmpty()) {
            for (int i = 0; i < wordInfo.strokeOrder.length(); i++) {
                char ch = wordInfo.strokeOrder.charAt(i);
                weight -= ch;
            }
        } else if (wordInfo.strokeCount > 0) {
            weight -= wordInfo.strokeCount * 10;
        } else {
            weight += 50;
        }

        // 保留 3 位小数
        weight = Math.round(weight * 1000) / 1000.0f;

        this.tree.add(pinyin, word, weight);
    }

    /**
     * 查找指定拼音的后继字母
     *
     * @return 参数为<code>null</code>或为空时，返回<code>null</code>
     */
    public List<String> findNextPinyinChar(List<String> chars) {
        if (chars == null || chars.isEmpty()) {
            return null;
        }

        PinyinTree tree = this.tree.getChildByPinyin(chars);

        List<String> list = new ArrayList<>();
        if (tree != null) {
            list.addAll(tree.childPinyinChar());
        }
        return list;
    }

    /** 查找指定拼音的候选字 */
    public List<PinyinWord> findCandidateWords(List<String> chars) {
        if (chars == null || chars.isEmpty()) {
            return new ArrayList<>();
        }

        PinyinTree tree = this.tree.getChildByPinyin(chars);

        if (tree != null) {
            return tree.getPinyins().stream().map((pinyin) -> {
                String p = pinyin.getValue();
                String w = pinyin.getWord();
                Word word = this.words.get(w);

                return new PinyinWord(w, p, word.hasSimple());
            }).collect(Collectors.toList());
        }
        return new ArrayList<>();
    }

    public static class Word {
        /** 繁体字对应的简体，存在该值的均为繁体 */
        private String simple;

        /** 笔画数 */
        private int strokeCount;
        /** 笔顺 */
        private String strokeOrder;

        public boolean hasSimple() {
            return this.simple != null;
        }

        public String getSimple() {
            return this.simple;
        }

        public void setSimple(String simple) {
            this.simple = simple;
        }

        public int getStrokeCount() {
            return this.strokeCount;
        }

        public void setStrokeCount(int strokeCount) {
            this.strokeCount = strokeCount;
        }

        public String getStrokeOrder() {
            return this.strokeOrder;
        }

        public void setStrokeOrder(String strokeOrder) {
            this.strokeOrder = strokeOrder;
        }
    }
}
