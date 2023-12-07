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

package org.crazydan.studio.app.ime.kuaizi.core.dict;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.crazydan.studio.app.ime.kuaizi.core.input.CharInput;

/**
 * 拼音树
 * <p/>
 * 按拼音的字母组合逐层分解，最多只有三层，
 * 即，第一层为声母，第二层为除去声母后的第一个字母，
 * 第三层为除去第一二层之后的部分
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-12-06
 */
public class PinyinTree {
    /** 当前节点所对应的拼音字母组合的 id，若不是有效拼音，则其值为 null */
    public final String id;
    /** 当前节点所对应的拼音字母组合中的字母：后继字母 */
    public final String value;

    /** 后继字母及其子树：按字母顺序升序排序 */
    public final Map<String, PinyinTree> children = new LinkedHashMap<>();

    public PinyinTree(String id, String value) {
        this.id = id;
        this.value = value;
    }

    /** 根据拼音字母组合及其 id 构造拼音树 */
    public static PinyinTree create(Map<String, String> pinyinCharsAndIdMap) {
        PinyinTree root = new PinyinTree(null, "");

        pinyinCharsAndIdMap.keySet().stream()
                           // 必须先按长度升序排序，以确保没有后继字母的拼音最先被加入树结构中
                           .sorted(Comparator.comparing(String::length)) //
                           .sorted(String::compareTo) //
                           .forEach(chars -> {
                               // 将拼音拆分为三层，再依次添加到树中
                               String[] charsSegments = splitChars(chars);

                               root.add(root.value, charsSegments, pinyinCharsAndIdMap);
                           });

        return root;
    }

    /** 当前节点是否为拼音 */
    public boolean isPinyin() {
        return this.id != null;
    }

    /** 判断指定的输入是否为有效拼音 */
    public boolean hasValidPinyin(CharInput input) {
        return getPinyinCharsId(input) != null;
    }

    /** 判断指定的输入的拼音字母组合的 id */
    public String getPinyinCharsId(CharInput input) {
        String pinyinChars = String.join("", input.getChars());

        return getPinyinCharsId(pinyinChars);
    }

    /** 获取指定拼音字母组合的 id */
    public String getPinyinCharsId(String chars) {
        PinyinTree child = getChildByChars(chars);

        return child != null ? child.id : null;
    }

    /** 通过 id 获取拼音字母组合 */
    public String getPinyinCharsById(String charsId) {
        if (charsId == null) {
            return null;
        }

        for (Map.Entry<String, PinyinTree> entry : this.children.entrySet()) {
            PinyinTree child = entry.getValue();

            if (Objects.equals(child.id, charsId)) {
                return child.value;
            } else {
                String childChars = child.getPinyinCharsById(charsId);
                if (childChars != null) {
                    return child.value + childChars;
                }
            }
        }
        return null;
    }

    /** 获取以指定字符开头的子树 */
    public PinyinTree getChild(String start) {
        return this.children.get(start);
    }

    /** 获取指定拼音字母组合的子树 */
    public PinyinTree getChildByChars(String chars) {
        String[] segments = splitChars(chars);
        if (segments.length == 0) {
            return null;
        }

        PinyinTree child = this;
        for (String segment : segments) {
            if (segment == null || child == null) {
                break;
            }

            child = child.getChild(segment);
        }
        return child;
    }

    /**
     * 获取后继字母组合列表，元素包含当前节点的 {@link #value}
     * <p/>
     * 结果先按字符长度升序排列，再按字符顺序排列
     */
    public List<String> getNextCharsList() {
        return this.children.keySet()
                            .stream()
                            .map((ch) -> this.value + ch)
                            .sorted(Comparator.comparing(String::length))
                            .sorted(String::compareTo)
                            .collect(Collectors.toList());
    }

    /**
     * 获取当前节点的全部拼音字母组合
     * <p/>
     * 组合的首字母为当前节点的 {@link #value}
     */
    public List<String> getAllPinyinChars() {
        List<String> list = new ArrayList<>();

        // 添加自身：因返回结果会附加当前节点的字母，故而，这里添加空字符即可
        if (isPinyin()) {
            list.add("");
        }

        this.children.forEach((k, p) -> list.addAll(p.getAllPinyinChars()));

        return list.stream().map((ch) -> this.value + ch).collect(Collectors.toList());
    }

    private void add(String topChar, String[] charsSegments, Map<String, String> pinyinCharsAndIdMap) {
        String start = charsSegments[0];
        if (start == null) {
            return;
        }

        String pinyin = topChar + start;

        PinyinTree child = this.children.computeIfAbsent(start,
                                                         (k) -> new PinyinTree(pinyinCharsAndIdMap.get(pinyin), start));
        if (charsSegments.length > 1) {
            child.add(pinyin, Arrays.copyOfRange(charsSegments, 1, charsSegments.length), pinyinCharsAndIdMap);
        }
    }

    private static String[] splitChars(String chars) {
        if (chars == null || chars.isEmpty()) {
            return new String[0];
        }

        int startLength = 1;
        if (chars.startsWith("ch") || chars.startsWith("sh") || chars.startsWith("zh")) {
            startLength = 2;
        }

        // 将拼音拆分为三层，再依次添加到树中
        String[] charsSegments = new String[3];
        charsSegments[0] = chars.substring(0, startLength);
        charsSegments[1] = chars.length() > startLength ? chars.substring(startLength, startLength + 1) : null;
        charsSegments[2] = chars.length() > startLength + 1 ? chars.substring(startLength + 1) : null;

        return charsSegments;
    }
}
