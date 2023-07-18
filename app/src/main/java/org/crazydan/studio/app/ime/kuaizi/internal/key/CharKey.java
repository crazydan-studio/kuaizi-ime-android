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

package org.crazydan.studio.app.ime.kuaizi.internal.key;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import androidx.annotation.NonNull;
import org.crazydan.studio.app.ime.kuaizi.internal.Key;

/**
 * 可输入字符{@link Key 按键}
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-06-28
 */
public class CharKey extends BaseKey<CharKey> {
    private final Type type;
    private final String text;
    private final List<String> replacements;
    private int fgColorAttrId;

    private CharKey(Type type, String text) {
        this.type = type;
        this.text = text;
        this.replacements = new ArrayList<>();

        // Note: 当前字符也需加入替换列表，
        // 以保证在按键被替换后（按键字符被修改），也能进行可替换性检查
        withReplacements(text);
    }

    public static CharKey create(Type type, String text) {
        return new CharKey(type, text);
    }

    /** 按键{@link Type 类型} */
    public Type getType() {
        return this.type;
    }

    /** 按键文本内容 */
    public String getText() {
        return this.text;
    }

    /** 添加可替换内容，用于在同一按键上切换不同的字符，比如，英文字母的大小写切换等 */
    public CharKey withReplacements(String... replacements) {
        for (String s : replacements) {
            if (s != null && !s.isEmpty()) {
                this.replacements.add(s);
            }
        }
        return this;
    }

    /** 获取指定内容之后的可替换内容，循环获取，包括按键字符本身 */
    public String nextReplacement(String s) {
        int index = this.replacements.indexOf(s);
        return index < 0 || index >= this.replacements.size() - 1
               ? this.replacements.get(0)
               : this.replacements.get(index + 1);
    }

    /** 判断是否可替换指定的按键 */
    public boolean canReplaceTheKey(Key<?> key) {
        if (!(key instanceof CharKey)
            // 只有自身，则不做替换
            || this.replacements.size() <= 1) {
            return false;
        } else if (this.equals(key)) {
            return true;
        }

        return this.replacements.contains(((CharKey) key).getText());
    }

    /** 是否为标点 */
    public boolean isPunctuation() {
        return this.type == Type.Punctuation;
    }

    /** 获取前景色属性 id */
    public int getFgColorAttrId() {
        return this.fgColorAttrId;
    }

    /** 设置前景色属性 id */
    public CharKey setFgColorAttrId(int fgColorAttrId) {
        this.fgColorAttrId = fgColorAttrId;
        return this;
    }

    @Override
    public boolean isSameWith(Key<?> key) {
        if (!(key instanceof CharKey)) {
            return false;
        }

        CharKey that = (CharKey) key;
        return this.type == that.type && this.text.equals(that.text);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        CharKey that = (CharKey) o;
        return this.fgColorAttrId == that.fgColorAttrId && this.type == that.type && this.text.equals(that.text);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), this.type, this.text, this.fgColorAttrId);
    }

    @NonNull
    @Override
    public String toString() {
        return "CharKey(" + getType() + ": " + getText() + ')';
    }

    public enum Type {
        /** 字母按键 */
        Alphabet,
        /** 数字按键 */
        Number,
        /** 标点符号按键 */
        Punctuation,
        /** 颜文字按键 */
        Emotion,
    }
}
