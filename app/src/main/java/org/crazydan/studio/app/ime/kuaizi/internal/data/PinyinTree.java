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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-07-06
 */
public class PinyinTree {
    private final List<PinyinWord> words = new ArrayList<>();
    private final Map<String, PinyinTree> tree = new HashMap<>();

    private Integer allWordSize;

    public int allWordSize() {
        if (this.allWordSize == null) {
            this.allWordSize = this.words.size();
            this.tree.forEach((c, t) -> this.allWordSize += t.allWordSize());
        }
        return this.allWordSize;
    }

    public boolean isLeaf() {
        return this.tree.isEmpty();
    }

    public void add(PinyinWord word) {
        if (!word.isValid()) {
            return;
        }

        PinyinTree next = this;
        for (String ch : word.getPinyinChars()) {
            next = next.tree.computeIfAbsent(ch, (key) -> new PinyinTree());
        }

        next.words.add(word);
    }

    public List<PinyinCharLink> charLinks(boolean undirected) {
        Set<PinyinCharLink> links = new HashSet<>();

        this.tree.forEach((sourceChar, child) -> {
            if (child.isLeaf()) {
                return;
            }

            child.tree.keySet().forEach((targetChar) -> {
                PinyinCharLink link = new PinyinCharLink(undirected, sourceChar, targetChar);
                links.add(link);
            });

            links.addAll(child.charLinks(undirected));
        });

        return new ArrayList<>(links);
    }

    public PinyinCharTree charTree() {
        PinyinCharTree root = new PinyinCharTree("");
        mountCharTree(root);

        return root;
    }

    private void mountCharTree(PinyinCharTree root) {
        this.tree.forEach((ch, child) -> {
            PinyinCharTree childCharTree = new PinyinCharTree(ch);
            List<PinyinCharTree.Word> words = child.words.stream()
                                                         .map(PinyinCharTree.Word::from)
                                                         .collect(Collectors.toList());
            childCharTree.getWords().addAll(words);

            root.getChildren().add(childCharTree);

            child.mountCharTree(childCharTree);
        });
    }

    public List<String> getPinyinList() {
        Set<String> set = combinePinyin("");
        List<String> list = new ArrayList<>(set);
        list.sort(String::compareTo);

        return list;
    }

    private Set<String> combinePinyin(String top) {
        Set<String> pinyinSet = new HashSet<>();

        if (!this.words.isEmpty() || isLeaf()) {
            pinyinSet.add(top);
        }

        this.tree.forEach((ch, child) -> {
            Set<String> childPinyinSet = child.combinePinyin(top + ch);
            pinyinSet.addAll(childPinyinSet);
        });

        return pinyinSet;
    }
}
