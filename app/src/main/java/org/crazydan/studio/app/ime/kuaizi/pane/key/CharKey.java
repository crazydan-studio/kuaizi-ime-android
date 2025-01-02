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
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import org.crazydan.studio.app.ime.kuaizi.common.utils.CharUtils;
import org.crazydan.studio.app.ime.kuaizi.pane.Key;

/**
 * 可输入字符按键
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-06-28
 */
public class CharKey extends TypedKey<CharKey.Type> {
    private final static Builder builder = new Builder();

    /** 字符按键的{@link Level 等级} */
    public final Level level;
    /** 字符按键的可替换字符列表，用于在同一按键上切换不同的字符，比如，英文字母的大小写切换等：只读 */
    public final List<String> replacements;

    /** 构建 {@link CharKey} */
    public static CharKey build(Consumer<Builder> c) {
        return Builder.build(builder, c);
    }

    /** 将指定的字符串中的字符挨个构造为 {@link CharKey} */
    public static List<Key> from(String value) {
        if (value == null) {
            return new ArrayList<>();
        }

        List<Key> keys = new ArrayList<>(value.length());

        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);

            Type type;
            if (Character.isDigit(c)) {
                type = Type.Number;
            } else if (Character.isLetter(c)) {
                type = Type.Alphabet;
            } else {
                continue;
            }

            String val = String.valueOf(c);
            CharKey key = CharKey.build((b) -> b.type(type).value(val));

            keys.add(key);
        }
        return keys;
    }

    protected CharKey(Builder builder) {
        super(builder);

        this.level = builder.level;
        // Note: 若列表已经是只读的，则将会直接返回，不会被嵌套封装
        this.replacements = Collections.unmodifiableList(builder.replacements);
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

    /** 判断是否可替换指定的按键 */
    public boolean canReplaceTheKey(Key key) {
        if (!(key instanceof CharKey)
            // 只有自身，则不做替换
            || !hasReplacement()) {
            return false;
        } else if (this.equals(key)) {
            return true;
        }

        return this.replacements.contains(key.value);
    }

    /** 根据指定位置的可替换内容，创建替换按键 */
    public CharKey createReplacementKey(int index) {
        String replacement = getReplacement(index);

        return createReplacementKey(replacement);
    }

    /** 根据指定的可替换内容，创建替换按键 */
    public CharKey createReplacementKey(String replacement) {
        // Note：新 key 需附带替换列表（以共享方式减少内存消耗），
        // 以便于在替换 目标编辑器 内容时进行可替换性判断
        return CharKey.build((b) -> b.from(this) //
                                     .value(replacement).label(replacement) //
                                     .replacements(this.replacements));
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
        ;

        /** 判断指定的 {@link Key} 是否为当前类型的 {@link CharKey} */
        public boolean match(Key key) {
            return key instanceof CharKey && ((CharKey) key).type == this;
        }
    }

    /** 按键级别 */
    public enum Level {
        /**
         * 第 0 级：初始布局的按键。
         * 一个拼音的首字母均处于该级别（ch、sh、zh 独立成为第 0 级），
         * 如，huang 中的 h 为第 0 级
         */
        level_0,
        /**
         * 第 1 级：拼音滑屏的第一级后继字母按键。
         * 拼音第 0 级字母之后的第一个字母均处于该级别，
         * 如，huang 中的 u 为第 1 级
         */
        level_1,
        /**
         * 第 2 级：拼音滑屏的第二级后继字母按键。
         * 拼音第 0 级字母之后的剩余字母均处于该级别，
         * 如，huang 中的 uang 为第 2 级
         */
        level_2,
        /**
         * 末级：完整且无后继字母的拼音。
         * 如，ai, er, an, ang, m, n 等
         */
        level_final,
    }

    /** {@link CharKey} 的构建器 */
    public static class Builder extends TypedKey.Builder<Builder, CharKey, Type> {
        public static final Consumer<Builder> noop = (b) -> {};

        private Level level = Level.level_0;
        private List<String> replacements = new ArrayList<>();

        Builder() {
            super(60);
        }

        // ===================== Start: 构建函数 ===================

        @Override
        protected CharKey doBuild() {
            // Note: 当前的输入值也需加入替换列表，
            // 以保证在按键被替换后（按键字符被修改），也能进行可替换性检查
            replacements(value());

            return new CharKey(this);
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), this.level, this.replacements);
        }

        @Override
        protected void reset() {
            super.reset();

            this.level = Level.level_0;
            this.replacements = new ArrayList<>();
        }

        // ===================== End: 构建函数 ===================

        // ===================== Start: 构建配置 ===================

        /** Note: {@link CharKey#replacements} 未复制，如有需要，可自行调用 {@link #replacements(String...)} 补充 */
        @Override
        public Builder from(CharKey key) {
            return super.from(key).level(key.level);
        }

        /** @see CharKey#level */
        public Builder level(Level level) {
            this.level = level != null ? level : Level.level_0;
            return this;
        }

        /** @see CharKey#replacements */
        public Builder replacements(String... replacements) {
            for (String s : replacements) {
                if (!CharUtils.isBlank(s) && !this.replacements.contains(s)) {
                    this.replacements.add(s);
                }
            }
            return this;
        }

        /** 直接设置 {@link #replacements}，以便于复用集合列表实例 */
        protected Builder replacements(List<String> replacements) {
            this.replacements = replacements != null ? replacements : new ArrayList<>();
            return this;
        }

        // ===================== End: 构建配置 ===================
    }
}
