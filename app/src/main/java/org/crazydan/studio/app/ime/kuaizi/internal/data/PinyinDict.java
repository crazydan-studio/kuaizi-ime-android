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
 * 拼音字典（内存版）
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

    public void add(String pinyin, Word word, float weight) {
        this.words.putIfAbsent(word.value, word);

        weight *= 3000;

        // Note: 确保笔画少的更靠前，且笔画相同的能够更靠近在一起，繁体靠最后，字型相近的能挨在一起
        if (word.isTraditional()) {
            weight -= 200;
        } else {
            weight += 100;
        }

        if (word.strokeOrder != null && !word.strokeOrder.isEmpty()) {
            for (int i = 0; i < word.strokeOrder.length(); i++) {
                char ch = word.strokeOrder.charAt(i);
                weight -= ch;
            }
        } else if (word.strokeCount > 0) {
            weight -= word.strokeCount * 10;
        } else {
            weight += 50;
        }

        this.tree.add(pinyin, word.value, weight);
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

                return new PinyinWord(w, p, word.isTraditional());
            }).collect(Collectors.toList());
        }
        return new ArrayList<>();
    }

    public static class Word {
        private int id;
        /** 字 */
        private String value;
        /** 笔画数 */
        private int strokeCount;
        /** 笔画顺序 */
        private String strokeOrder;
        /** 简体字：仅针对当前字为繁体字的情况 */
        private String simpleWord;

        public boolean isTraditional() {
            return this.simpleWord != null;
        }

        public int getId() {
            return this.id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getValue() {
            return this.value;
        }

        public void setValue(String value) {
            this.value = value;
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

        public String getSimpleWord() {
            return this.simpleWord;
        }

        public void setSimpleWord(String simpleWord) {
            this.simpleWord = simpleWord;
        }
    }
}
