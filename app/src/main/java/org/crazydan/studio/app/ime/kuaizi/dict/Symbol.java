/*
 * 筷字输入法 - 高效编辑需要又好又快的输入法
 * Copyright (C) 2024 Crazydan Studio
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

package org.crazydan.studio.app.ime.kuaizi.dict;

import java.util.Objects;

import androidx.annotation.NonNull;

/**
 * 标点符号
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-08-28
 */
public class Symbol {
    public final String text;

    public Symbol(String text) {
        this.text = text;
    }

    public static Symbol single(String text) {
        return new Symbol(text);
    }

    public static Symbol pair(String left, String right) {
        return new Symbol.Pair(left, right);
    }

    @NonNull
    @Override
    public String toString() {
        return this.text;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Symbol that = (Symbol) o;
        return this.text.equals(that.text);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.text);
    }

    /** 配对的标点符号，含左右两个符号 */
    public static class Pair extends Symbol {
        public final String left;
        public final String right;

        public Pair(String left, String right) {
            super(left + " " + right);
            this.left = left;
            this.right = right;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            Pair that = (Pair) o;
            return this.left.equals(that.left) && this.right.equals(that.right);
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), this.left, this.right);
        }
    }
}
