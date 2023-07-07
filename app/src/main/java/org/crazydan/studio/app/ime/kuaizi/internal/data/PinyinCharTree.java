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
        List<Word> list = new ArrayList<>();

        if (pinyinChars == null || pinyinChars.isEmpty()) {
            return list;
        }

        PinyinCharTree tree = this;
        for (String pinyinChar : pinyinChars) {
            tree = tree.getChildByChar(pinyinChar);
            if (tree == null) {
                break;
            }
        }

        if (tree != null) {
            list.addAll(tree.getWords());
        }
        return list;
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
    }
}
