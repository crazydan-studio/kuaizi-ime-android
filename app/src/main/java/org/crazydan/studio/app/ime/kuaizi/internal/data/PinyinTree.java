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
import java.util.stream.Collectors;

import androidx.annotation.NonNull;

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
                new String[] { "m", "m̄", "ḿ", "m̀" },
                new String[] { "e", "ê̄", "ế", "ê̌", "ề" },
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

    private static String[] parsePinyinChars(String pinyin) {
        if ("m̀".equals(pinyin) || "ḿ".equals(pinyin) || "m̄".equals(pinyin) //
            || "ê̄".equals(pinyin) || "ế".equals(pinyin) //
            || "ê̌".equals(pinyin) || "ề".equals(pinyin)) {
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

    public List<Pinyin> getPinyins() {
        return this.pinyins;
    }

    public Map<String, PinyinTree> getChildren() {
        return this.children;
    }

    private boolean aLeaf() {
        return this.children.isEmpty();
    }

    public void addPinyin(String pinyin, String word, int weight) {
        String[] chars = parsePinyinChars(pinyin);

        PinyinTree next = this;
        for (String ch : chars) {
            next = next.children.computeIfAbsent(ch, (key) -> new PinyinTree());
        }

        Pinyin pending = new Pinyin();
        pending.setValue(pinyin);
        pending.setWord(word);
        pending.setChars(String.join("", chars));
        pending.setWeight(weight);

        if (next.pinyins.contains(pending)) {
            return;
        }

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

    /** 得到全部拼音的字母列表 */
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

    /** 遍历拼音树 */
    public void traverse(Traveler traveler) {
        this.pinyins.forEach(traveler::access);
        this.children.forEach((ch, child) -> child.traverse(traveler));
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

    public interface Traveler {

        void access(Pinyin pinyin);
    }

    public static class Pinyin {
        private int id;
        /** 拼音：含声调 */
        private String value;
        /** 拼音的字母：无声调 */
        private String chars;
        /** 关联的{@link PinyinDict.Word#value 字} */
        private String word;

        /**
         * 候选字权重：使用频率、组合频率等
         * <p/>
         * 仅初始字典的权重决定在输入法中的候选字排序，
         * 而用户使用过程中赋予的权重仅用于判断是否做为最优候选字，
         * 而其在候选字分页中的位置是固定的
         */
        private int weight;

        public int getId() {
            return this.id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getCode() {
            return this.value + ":" + this.word;
        }

        public String getValue() {
            return this.value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public String getChars() {
            return this.chars;
        }

        public void setChars(String chars) {
            this.chars = chars;
        }

        public String getWord() {
            return this.word;
        }

        public void setWord(String word) {
            this.word = word;
        }

        public int getWeight() {
            return this.weight;
        }

        public void setWeight(int weight) {
            this.weight = weight;
        }

        @NonNull
        @Override
        public String toString() {
            return this.value + ":" + this.word;
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

    public static class Phrase {
        private List<Pinyin> pinyins = new ArrayList<>();
        private int weight;

        public List<Pinyin> getPinyins() {
            return this.pinyins;
        }

        public void setPinyins(List<Pinyin> pinyins) {
            this.pinyins = pinyins;
        }

        public void addPinyin(Pinyin pinyin) {
            this.pinyins.add(pinyin);
        }

        public int getWeight() {
            return this.weight;
        }

        public void setWeight(int weight) {
            this.weight = weight;
        }

        @NonNull
        @Override
        public String toString() {
            return this.pinyins.stream().map(p -> p.getWord() + "(" + p.getValue() + ")").collect(Collectors.joining());
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Phrase phrase = (Phrase) o;
            return this.pinyins.equals(phrase.pinyins);
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.pinyins);
        }
    }
}
