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
