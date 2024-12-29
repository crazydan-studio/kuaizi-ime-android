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

package org.crazydan.studio.app.ime.kuaizi.pane.key;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.crazydan.studio.app.ime.kuaizi.pane.Key;

/**
 * 可输入字符{@link Key 按键}
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-06-28
 */
public class CharKey extends Key {
    private final Type type;
    private List<String> replacements;

    private CharKey(Type type, String text) {
        super(text);

        this.type = type;
        this.replacements = new ArrayList<>();

        // Note: 当前的文本字符也需加入替换列表，
        // 以保证在按键被替换后（按键字符被修改），也能进行可替换性检查
        withReplacements(getText());
    }

    public static CharKey create(Type type, String text) {
        return new CharKey(type, text);
    }

    public static boolean isAlphabet(Key key) {
        return key instanceof CharKey && key.isAlphabet();
    }

    public static List<Key> from(String text) {
        if (text == null) {
            return new ArrayList<>();
        }

        List<Key> keys = new ArrayList<>(text.length());

        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);

            CharKey key;
            if (Character.isDigit(ch)) {
                key = new CharKey(Type.Number, String.valueOf(ch));
            } else if (Character.isLetter(ch)) {
                key = new CharKey(Type.Alphabet, String.valueOf(ch));
            } else {
                continue;
            }

            keys.add(key);
        }
        return keys;
    }

    /** 按键{@link Type 类型} */
    public Type getType() {
        return this.type;
    }

    /** 添加可替换内容，用于在同一按键上切换不同的字符，比如，英文字母的大小写切换等 */
    public CharKey withReplacements(String... replacements) {
        for (String s : replacements) {
            if (s != null && !s.isEmpty() && !this.replacements.contains(s)) {
                this.replacements.add(s);
            }
        }
        return this;
    }

    /** 是否有替代字符 */
    public boolean hasReplacement() {
        return this.replacements.size() > 1;
    }

    /** 获取指定内容之后的可替换内容，循环获取，包括按键字符本身 */
    public String nextReplacement(String s) {
        int index = this.replacements.indexOf(s);
        return getReplacement(index + 1);
    }

    /** 获取指定位置的可替换内容，循环获取，包括按键字符本身 */
    public String getReplacement(int index) {
        int total = this.replacements.size();
        return index < 0 ? this.replacements.get(0) : this.replacements.get(index % total);
    }

    public List<String> getReplacements() {
        return new ArrayList<>(this.replacements);
    }

    /** 判断是否可替换指定的按键 */
    public boolean canReplaceTheKey(Key key) {
        if (!(key instanceof CharKey)
            // 只有自身，则不做替换
            || !hasReplacement()) {
            return false;
        } else if (this.equals(key)) {
            return true;
        }

        return this.replacements.contains(key.getText());
    }

    /** 根据指定位置的可替换内容，创建替换按键 */
    public CharKey createReplacementKey(int index) {
        String replacement = getReplacement(index);

        return createReplacementKey(replacement);
    }

    /** 根据指定的可替换内容，创建替换按键 */
    public CharKey createReplacementKey(String replacement) {
        // Note: 按键类型需保留，因为，字符间是否需要空格是通过字符类型进行判断的
        CharKey key = new CharKey(getType(), replacement);

        key.setLabel(replacement);

        // Note：新 key 需附带替换列表（以共享方式减少内存消耗），
        // 以便于在替换 目标编辑器 内容时进行可替换性判断
        key.replacements = this.replacements;

        return key;
    }

    @Override
    public boolean isNumber() {
        return this.type == Type.Number;
    }

    @Override
    public boolean isEmoji() {
        return this.type == Type.Emoji;
    }

    /** 是否为标点 */
    @Override
    public boolean isSymbol() {
        return this.type == Type.Symbol;
    }

    @Override
    public boolean isAlphabet() {
        return this.type == Type.Alphabet;
    }

    @Override
    public boolean isSameWith(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        } else if (this == o) {
            return true;
        }

        CharKey that = (CharKey) o;
        return this.type == that.type && Objects.equals(this.getText(), that.getText());
    }

    public enum Type {
        /** 字母按键 */
        Alphabet,
        /** 数字按键 */
        Number,
        /** 标点符号按键 */
        Symbol,
        /** 表情符号按键 */
        Emoji,
    }
}
