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
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-07-06
 */
public class PinyinTree {
    public static final Map<String, String> pinyinReplacements = new LinkedHashMap<>();

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
                pinyinReplacements.put(pair[i], pair[0]);
            }
        }
    }

    private final Set<PinyinWord> words = new LinkedHashSet<>();
    private final Map<String, PinyinTree> tree = new HashMap<>();

    public boolean isLeaf() {
        return this.tree.isEmpty();
    }

    public void add(PinyinWord word) {
        if (!word.isValid()) {
            return;
        }

        for (String pinyin : word.getPinyins()) {
            PinyinTree next = this;
            for (String ch : parsePinyinChars(pinyin)) {
                next = next.tree.computeIfAbsent(ch, (key) -> new PinyinTree());
            }
            next.words.add(word);
        }
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

            List<PinyinCharTree.Word> words = child.getCharTreeWords();
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

    private List<PinyinCharTree.Word> getCharTreeWords() {
        Map<Integer, List<PinyinCharTree.Word>> wordByStrokesMap = new TreeMap<>();
        Map<String, List<PinyinCharTree.Word>> wordByValueMap = new HashMap<>();

        List<PinyinCharTree.Word> traditionalWords = new ArrayList<>();
        List<PinyinCharTree.Word> noStrokesWords = new ArrayList<>();
        this.words.forEach(pinyinWord -> {
            List<PinyinCharTree.Word> charTreeWords = PinyinCharTree.Word.from(pinyinWord);

            for (PinyinCharTree.Word charTreeWord : charTreeWords) {
                if (charTreeWord.isTraditional()) {
                    traditionalWords.add(charTreeWord);
                } else if (charTreeWord.getStrokes() == 0) {
                    noStrokesWords.add(charTreeWord);
                } else {
                    wordByValueMap.computeIfAbsent(charTreeWord.getValue(), (k) -> new ArrayList<>()).add(charTreeWord);
                    wordByStrokesMap.computeIfAbsent(charTreeWord.getStrokes(), (k) -> new ArrayList<>())
                                    .add(charTreeWord);
                }
            }
        });

        Set<PinyinCharTree.Word> set = new LinkedHashSet<>();
        // 按笔画由少到多排序
        wordByStrokesMap.forEach((s, list) -> {
            list.forEach(charTreeWord -> {
                set.add(charTreeWord);

                // 将多音字放在一起
                List<PinyinCharTree.Word> words = wordByValueMap.get(charTreeWord.getValue());
                words.sort(Comparator.comparing(PinyinCharTree.Word::getNotation).reversed());
                set.addAll(words);
            });
        });

        set.addAll(noStrokesWords);
        set.addAll(traditionalWords);

        List<PinyinCharTree.Word> list = new ArrayList<>(set);
        list.sort(Comparator.comparing(PinyinCharTree.Word::getWeight).reversed());

        return list;
    }

    private static String[] parsePinyinChars(String pinyin) {
        if ("m̀".equals(pinyin) || "m̄".equals(pinyin) //
            || "ê̄".equals(pinyin) || "ê̌".equals(pinyin)) {
            return new String[] { pinyinReplacements.get(pinyin) };
        } else {
            String[] chars = new String[pinyin.length()];

            for (int i = 0; i < pinyin.length(); i++) {
                String ch = pinyin.charAt(i) + "";
                chars[i] = pinyinReplacements.getOrDefault(ch, ch);
            }
            return chars;
        }
    }
}
