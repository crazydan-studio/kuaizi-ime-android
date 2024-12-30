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

import java.util.Objects;

import org.crazydan.studio.app.ime.kuaizi.pane.Key;

/**
 * 数学计算运算符按键
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-09-18
 */
public class MathOpKey extends TypedKey<MathOpKey.Type> {

    private MathOpKey(Type type, String text) {
        super(type, text);
    }

    public static MathOpKey create(Type type, String text) {
        return new MathOpKey(type, text);
    }

    public Type getType() {
        return this.type;
    }

    @Override
    public boolean isSameWith(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        } else if (this == o) {
            return true;
        }

        MathOpKey that = (MathOpKey) o;
        return this.type == that.type && Objects.equals(this.value, that.value);
    }

    @Override
    public String toString() {
        return this.type + "(" + this.value + ")";
    }

    public enum Type {
        /** 数学 = */
        Equal,
        /** 数学 + */
        Plus,
        /** 数学 - */
        Minus,
        /** 数学 × */
        Multiply,
        /** 数学 ÷ */
        Divide,

        /** 数学 % */
        Percent,
        /** 数学 ‰ */
        Permill,
        /** 数学 ‱ */
        Permyriad,

        /** 数学 () */
        Brackets,
        /** 数学 . */
        Dot,
        ;

        /** 判断指定的 {@link Key} 是否为当前类型的 {@link MathOpKey} */
        public boolean match(Key key) {
            return key instanceof MathOpKey && ((MathOpKey) key).type == this;
        }

        public static boolean isSymbol(Key key) {
            if (!(key instanceof MathOpKey)) {
                return false;
            }

            // 确保数字和非运算符之间无空格
            switch (((MathOpKey) key).type) {
                case Percent:
                case Permill:
                case Permyriad:
                    return true;
            }
            return false;
        }
    }
}
