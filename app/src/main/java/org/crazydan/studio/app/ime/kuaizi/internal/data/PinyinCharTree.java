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
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-07-06
 */
public class PinyinCharTree {
    /** 字母 */
    private final String value;
    /** 该子树下的{@link Word 字} */
    private final List<Word> words = new ArrayList<>();

    private final List<PinyinCharTree> children = new ArrayList<>();

    public PinyinCharTree(String value) {
        this.value = value;
    }

    public String getValue() {
        return this.value;
    }

    public List<Word> getWords() {
        return this.words;
    }

    public List<PinyinCharTree> getChildren() {
        return this.children;
    }

    /**
     * 查找指定拼音的后继字母
     *
     * @return 参数为<code>null</code>或为空时，返回<code>null</code>
     */
    public List<String> findNextChars(List<String> pinyinChars) {
        if (pinyinChars == null || pinyinChars.isEmpty()) {
            return null;
        }

        PinyinCharTree tree = this;
        for (String pinyinChar : pinyinChars) {
            tree = tree.getChildByChar(pinyinChar);
            if (tree == null) {
                break;
            }
        }

        List<String> list = new ArrayList<>();
        if (tree != null) {
            list.addAll(tree.childChars());
        }
        return list;
    }

    /** 查找指定拼音的候选字 */
    public List<Word> findCandidateWords(List<String> pinyinChars) {
        if (pinyinChars == null || pinyinChars.isEmpty()) {
            return new ArrayList<>();
        }

        PinyinCharTree tree = this;
        for (String pinyinChar : pinyinChars) {
            tree = tree.getChildByChar(pinyinChar);
            if (tree == null) {
                break;
            }
        }

        if (tree != null) {
            return new ArrayList<>(tree.words);
        }
        return new ArrayList<>();
    }

    public List<String> childChars() {
        return this.children.stream().map(PinyinCharTree::getValue).collect(Collectors.toList());
    }

    public PinyinCharTree getChildByChar(String ch) {
        for (PinyinCharTree child : this.children) {
            if (ch.equals(child.getValue())) {
                return child;
            }
        }
        return null;
    }

    public static class Word {
        private String value;
        private String notation;

        private boolean traditional;
        private int level;
        private float weight;
        /** 笔画数 */
        private int strokes;

        public static Word from(PinyinWord pw) {
            PinyinCharTree.Word cw = new PinyinCharTree.Word(pw.getWord(), pw.getPinyin());
            cw.setTraditional(pw.isTraditional());
            cw.setLevel(pw.getLevel());
            cw.setWeight(pw.getWeight());
            cw.setStrokes(pw.getStrokes());

            return cw;
        }

        public Word(String value, String notation) {
            this.value = value;
            this.notation = notation;
        }

        public String getValue() {
            return this.value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public String getNotation() {
            return this.notation;
        }

        public void setNotation(String notation) {
            this.notation = notation;
        }

        public boolean isTraditional() {
            return this.traditional;
        }

        public void setTraditional(boolean traditional) {
            this.traditional = traditional;
        }

        public int getLevel() {
            return this.level;
        }

        public void setLevel(int level) {
            this.level = level;
        }

        public float getWeight() {
            return this.weight;
        }

        public void setWeight(float weight) {
            this.weight = weight;
        }

        public int getStrokes() {
            return this.strokes;
        }

        public void setStrokes(int strokes) {
            this.strokes = strokes;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Word that = (Word) o;
            return this.value.equals(that.value) && this.notation.equals(that.notation);
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.value, this.notation);
        }
    }
}
