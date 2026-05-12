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

package org.crazydan.studio.app.ime.kuaizi.core.key;

import java.util.function.Consumer;

import org.crazydan.studio.app.ime.kuaizi.core.Key;

/**
 * 数学计算运算符按键
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-09-18
 */
public class MathOpKey extends TypedKey<MathOpKey.Type> {
    private final static Builder builder = new Builder();

    /** 构建 {@link MathOpKey} */
    public static MathOpKey build(Consumer<Builder> c) {
        return Builder.build(builder, c);
    }

    protected MathOpKey(Builder builder) {
        super(builder);
    }

    @Override
    public String toString() {
        return this.type + "(" + this.value + ")";
    }

    /** 算术运算按键类型，包括运算符和百分号等 */
    public enum Type {
        /** 数学 = */
        Equal("="),
        /** 数学 + */
        Plus("+"),
        /** 数学 - */
        Minus("-"),
        /** 数学 × */
        Multiply("×"),
        /** 数学 ÷ */
        Divide("÷"),

        /** 数学 % */
        Percent("%"),
        /** 数学 ‰ */
        Permill("‰"),
        /** 数学 ‱ */
        Permyriad("‱"),

        /** 数学 () */
        Brackets("( )"),
        /** 数学 . */
        Dot("."),
        ;

        public final String text;

        Type(String text) {
            this.text = text;
        }

        /** 判断指定的 {@link Key} 是否为当前类型的 {@link MathOpKey} */
        public boolean match(Key key) {
            return key instanceof MathOpKey && ((MathOpKey) key).type == this;
        }

        /** 判断指定的 {@link Key} 是否为 {@link MathOpKey} 的运算符 */
        public static boolean isOperator(Key key) {
            if (!(key instanceof MathOpKey)) {
                return false;
            }

            // 确保数字和非运算符之间无空格
            switch (((MathOpKey) key).type) {
                case Percent:
                case Permill:
                case Permyriad:
                    return false;
            }
            return true;
        }
    }

    /** {@link MathOpKey} 的构建器 */
    public static class Builder extends TypedKey.Builder<Builder, MathOpKey, Type> {
        public static final Consumer<Builder> noop = (b) -> {};

        Builder() {
            super(20);
        }

        // ===================== Start: 构建函数 ===================

        @Override
        protected MathOpKey doBuild() {
            // Note: 对于括号，会在输入时拆分并为 value/label 赋值
            if (value() == null) {
                value(type().text);
            }
            if (label() == null) {
                label(value());
            }

            return new MathOpKey(this);
        }

        // ===================== End: 构建函数 ===================

        // ===================== Start: 构建配置 ===================

        // ===================== End: 构建配置 ===================
    }
}
