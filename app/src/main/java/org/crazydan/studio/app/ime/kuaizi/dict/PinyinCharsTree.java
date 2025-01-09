/*
 * 筷字输入法 - 高效编辑需要又好又快的输入法
 * Copyright (C) 2025 Crazydan Studio <https://studio.crazydan.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.
 * If not, see <https://www.gnu.org/licenses/lgpl-3.0.en.html#license-text>.
 */

package org.crazydan.studio.app.ime.kuaizi.dict;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.crazydan.studio.app.ime.kuaizi.core.input.CharInput;

/**
 * 拼音字母组合树
 * <p/>
 * 按拼音的字母组合逐层分解，最多只有三层，
 * 即，第一层为声母，第二层为除去声母后的第一个字母，
 * 第三层为除去第一二层之后的部分
 */
public class PinyinCharsTree {
    /** 当前节点所对应的拼音字母组合的 id，若不是有效拼音，则其值为 null */
    public final Integer id;
    /** 当前节点所对应的拼音字母组合中的字母 */
    public final String value;

    /** 后继字母及其子树：按字母顺序升序排序 */
    private final Map<String, PinyinCharsTree> children = new LinkedHashMap<>();

    PinyinCharsTree(Integer id, String value) {
        this.id = id;
        this.value = value;
    }

    /** 根据拼音字母组合及其 id 构造 {@link PinyinCharsTree} */
    public static PinyinCharsTree create(Map<String, Integer> pinyinCharsAndIdMap) {
        PinyinCharsTree root = new PinyinCharsTree(null, "");

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

    // <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<

    /** 获取子树的数量 */
    public int countChild() {
        return this.children.size();
    }

    /** 是否有子树 */
    public boolean hasChild() {
        return countChild() != 0;
    }

    /** 获取以指定字符开头的子树 */
    public PinyinCharsTree getChild(String start) {
        return this.children.get(start);
    }

    /**
     * 获取后继字母组合列表，元素包含当前节点的 {@link #value}
     * <p/>
     * 结果先按字符长度升序排列，再按字符顺序排列
     */
    public List<String> getNextChars() {
        return this.children.keySet()
                            .stream()
                            .map((ch) -> this.value + ch)
                            .sorted(Comparator.comparing(String::length))
                            .sorted(String::compareTo)
                            .collect(Collectors.toList());
    }

    /** 获取指定{@link CharInput 输入}的拼音字母组合的 id */
    public Integer getCharsId(CharInput input) {
        String chars = input.getJoinedChars();

        return getCharsId(chars);
    }

    /** 获取指定拼音字母组合的 id */
    public Integer getCharsId(String chars) {
        String[] segments = splitChars(chars);
        if (segments.length == 0) {
            return null;
        }

        PinyinCharsTree child = this;
        for (String segment : segments) {
            if (segment == null || child == null) {
                break;
            }
            child = child.getChild(segment);
        }

        return child != null ? child.id : null;
    }
    // >>>>>>>>>>>>>>>>>>>>>>>>>>>>>

    // <<<<<<<<<<<<<<<<<<<<<<<<<<<<

    /** 当前节点是否为拼音 */
    public boolean isPinyin() {
        return this.id != null;
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

    /** 判断指定的{@link CharInput 输入}是否为拼音字母组合 */
    public boolean isPinyinCharsInput(CharInput input) {
        return getCharsId(input) != null;
    }
    // >>>>>>>>>>>>>>>>>>>>>>>>

    private void add(String topChar, String[] charsSegments, Map<String, Integer> pinyinCharsAndIdMap) {
        String start = charsSegments[0];
        if (start == null) {
            return;
        }

        String subChar = topChar + start;

        PinyinCharsTree child = this.children.computeIfAbsent(start,
                                                              (k) -> new PinyinCharsTree(pinyinCharsAndIdMap.get(subChar),
                                                                                         start));
        if (charsSegments.length > 1) {
            child.add(subChar, Arrays.copyOfRange(charsSegments, 1, charsSegments.length), pinyinCharsAndIdMap);
        }
    }

    private static String[] splitChars(String chars) {
        if (chars == null || chars.isEmpty()) {
            return new String[0];
        }

        int nextCharIndex = 1;
        if (chars.startsWith("ch") || chars.startsWith("sh") || chars.startsWith("zh")) {
            nextCharIndex = 2;
        }

        // 将拼音拆分为三层，再依次添加到树中
        String[] charsSegments = new String[3];
        charsSegments[0] = chars.substring(0, nextCharIndex);
        charsSegments[1] = chars.length() > nextCharIndex ? chars.substring(nextCharIndex, nextCharIndex + 1) : null;
        charsSegments[2] = chars.length() > nextCharIndex + 1 ? chars.substring(nextCharIndex + 1) : null;

        return charsSegments;
    }
}
