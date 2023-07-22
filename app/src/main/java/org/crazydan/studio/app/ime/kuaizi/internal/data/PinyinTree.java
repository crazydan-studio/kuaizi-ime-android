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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 按拼音字母组合构成的数
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-07-06
 */
public class PinyinTree {
    public static final Map<String, String> pinyinCharReplacements = new LinkedHashMap<>();

    static {
        String[][] pairs = new String[][] {
                new String[] { "a", "ā", "á", "ǎ", "à" },
                new String[] { "o", "ō", "ó", "ǒ", "ò" },
                new String[] { "e", "ē", "é", "ě", "è", "ê" },
                new String[] { "i", "ī", "í", "ǐ", "ì" },
                new String[] { "u", "ū", "ú", "ǔ", "ù" },
                new String[] { "ü", "ǖ", "ǘ", "ǚ", "ǜ" },
                new String[] { "n", "ń", "ň", "ǹ" },
                new String[] { "m", "m̄", "m̀" },
                new String[] { "e", "ê̄", "ê̌" },
                };
        for (String[] pair : pairs) {
            for (int i = 1; i < pair.length; i++) {
                pinyinCharReplacements.put(pair[i], pair[0]);
            }
        }
    }

    /** 当前拼音树下的{@link Pinyin 拼音}集合 */
    private final List<Pinyin> pinyins = new ArrayList<>();
    /** 当前拼音的后继字母组成的子树 */
    private final Map<String, PinyinTree> children = new HashMap<>();

    public List<Pinyin> getPinyins() {
        return this.pinyins;
    }

    public Map<String, PinyinTree> getChildren() {
        return this.children;
    }

    private boolean aLeaf() {
        return this.children.isEmpty();
    }

    public void add(String pinyin, String word, float weight) {
        String[] chars = parsePinyinChars(pinyin);

        PinyinTree next = this;
        for (String ch : chars) {
            next = next.children.computeIfAbsent(ch, (key) -> new PinyinTree());
        }

        Pinyin pending = new Pinyin(pinyin, word);
        pending.setWeight(weight);

        if (next.pinyins.isEmpty()) {
            next.pinyins.add(pending);
        } else {
            for (int i = 0; i < next.pinyins.size(); i++) {
                Pinyin exist = next.pinyins.get(i);

                if (exist.weight < pending.weight) {
                    next.pinyins.add(i, pending);
                    return;
                }
            }
            next.pinyins.add(pending);
        }
    }

    /** 得到全部拼音的字母组合列表 */
    public List<String> createPinyinCharsList() {
        Set<String> set = combinePinyinChars("");

        List<String> list = new ArrayList<>(set);
        list.sort(String::compareTo);

        return list;
    }

    /** 拼音字母的连接图（按前继到后继的方向做连接） */
    public List<PinyinCharLink> createPinyinCharLinks() {
        Set<PinyinCharLink> links = new HashSet<>();

        this.children.forEach((source, child) -> {
            if (child.aLeaf()) {
                return;
            }

            child.children.keySet().forEach((target) -> {
                PinyinCharLink link = new PinyinCharLink(source, target);
                links.add(link);
            });

            links.addAll(child.createPinyinCharLinks());
        });

        return new ArrayList<>(links);
    }

    public PinyinTree getChildByPinyin(List<String> chars) {
        PinyinTree tree = this;
        for (String ch : chars) {
            tree = tree.getChildByPinyinChar(ch);
            if (tree == null) {
                break;
            }
        }

        return tree;
    }

    public List<String> childPinyinChar() {
        return new ArrayList<>(this.children.keySet());
    }

    private PinyinTree getChildByPinyinChar(String ch) {
        for (Map.Entry<String, PinyinTree> entry : this.children.entrySet()) {
            String key = entry.getKey();

            if (ch.equals(key)) {
                return entry.getValue();
            }
        }
        return null;
    }

    private Set<String> combinePinyinChars(String top) {
        Set<String> pinyinSet = new HashSet<>();

        if (!this.pinyins.isEmpty() || aLeaf()) {
            pinyinSet.add(top);
        }

        this.children.forEach((ch, child) -> {
            Set<String> childPinyinSet = child.combinePinyinChars(top + ch);

            pinyinSet.addAll(childPinyinSet);
        });

        return pinyinSet;
    }

    private static String[] parsePinyinChars(String pinyin) {
        if ("m̀".equals(pinyin) || "m̄".equals(pinyin) //
            || "ê̄".equals(pinyin) || "ê̌".equals(pinyin)) {
            return new String[] { pinyinCharReplacements.get(pinyin) };
        } else {
            String[] chars = new String[pinyin.length()];

            for (int i = 0; i < pinyin.length(); i++) {
                String ch = pinyin.charAt(i) + "";
                chars[i] = pinyinCharReplacements.getOrDefault(ch, ch);
            }
            return chars;
        }
    }

    public static class Pinyin {
        /** 拼音（含音调） */
        private final String value;
        /** 字 */
        private final String word;

        /**
         * 优先权重：使用频率、组合频率等
         * <p/>
         * 仅初始字典的权重决定在输入法中的候选字排序，
         * 而用户使用过程中赋予的权重仅用于判断是否做为最优候选字，
         * 而其在候选字分页中的位置是固定的
         */
        private float weight;

        public Pinyin(String value, String word) {
            this.value = value;
            this.word = word;
        }

        public String getValue() {
            return this.value;
        }

        public String getWord() {
            return this.word;
        }

        public float getWeight() {
            return this.weight;
        }

        public void setWeight(float weight) {
            this.weight = weight;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Pinyin pinyin = (Pinyin) o;
            return this.value.equals(pinyin.value) && this.word.equals(pinyin.word);
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.value, this.word);
        }
    }
}
