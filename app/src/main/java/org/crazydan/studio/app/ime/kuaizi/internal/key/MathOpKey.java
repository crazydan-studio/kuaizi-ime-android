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

import java.util.Objects;

import org.crazydan.studio.app.ime.kuaizi.internal.Key;

/**
 * 数学计算运算符按键
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-09-18
 */
public class MathOpKey extends BaseCharKey<MathOpKey> {
    private final Type type;

    private MathOpKey(Type type, String text) {
        super(text);

        this.type = type;
    }

    public static MathOpKey create(Type type, String text) {
        return new MathOpKey(type, text);
    }

    public static boolean isType(Key<?> key, Type type) {
        return key instanceof MathOpKey && ((MathOpKey) key).getType() == type;
    }

    public Type getType() {
        return this.type;
    }

    @Override
    public boolean isMathOperator() {
        switch (this.type) {
            case percent:
            case permill:
            case permyriad:
                return false;
        }
        return true;
    }

    @Override
    public boolean isSymbol() {
        // 确保数字和非运算符之间无空格
        return !isMathOperator();
    }

    @Override
    public boolean isLatin() {
        switch (this.type) {
            case brackets:
                // 确保括号与数字和运算符之间都有空格
            case dot:
                return true;
        }
        return false;
    }

    @Override
    public boolean isSameWith(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        } else if (this == o) {
            return true;
        }

        MathOpKey that = (MathOpKey) o;
        return this.type == that.type && Objects.equals(this.getText(), that.getText());
    }

    @Override
    public String toString() {
        return this.type + "(" + getText() + ")";
    }

    public enum Type {
        /** 数学 = */
        equal,
        /** 数学 + */
        plus,
        /** 数学 - */
        minus,
        /** 数学 × */
        multiply,
        /** 数学 ÷ */
        divide,

        /** 数学 % */
        percent,
        /** 数学 ‰ */
        permill,
        /** 数学 ‱ */
        permyriad,

        /** 数学 () */
        brackets,
        /** 数学 . */
        dot,
    }
}
