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

import java.util.Objects;

import androidx.annotation.NonNull;

/**
 * 标点符号
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-08-28
 */
public class Symbol {
    public final String value;

    public Symbol(String value) {
        this.value = value;
    }

    public static Symbol single(String value) {
        return new Symbol(value);
    }

    public static Symbol pair(String left, String right) {
        return new Symbol.Pair(left, right);
    }

    @NonNull
    @Override
    public String toString() {
        return this.value;
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
        return this.value.equals(that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.value);
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
