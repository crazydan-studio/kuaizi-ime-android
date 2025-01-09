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

import java.util.Objects;

import org.crazydan.studio.app.ime.kuaizi.core.Key;

/**
 * 可细分类型的按键
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2024-12-30
 */
public abstract class TypedKey<T extends Enum<?>> extends Key {
    /** 按键的类型 */
    public final T type;

    protected TypedKey(Builder<?, ?, T> builder) {
        super(builder);

        this.type = builder.type;
    }

    /** {@link TypedKey} 的构建器 */
    protected static abstract class Builder< //
            B extends Builder<B, K, T>, //
            K extends TypedKey<T>, //
            T extends Enum<?> //
            > //
            extends Key.Builder<B, K> {
        private T type;

        protected Builder(int cacheSize) {
            super(cacheSize);
        }

        // ===================== Start: 构建函数 ===================

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), this.type);
        }

        @Override
        protected void reset() {
            super.reset();

            this.type = null;
        }

        // ===================== End: 构建函数 ===================

        // ===================== Start: 构建配置 ===================

        @Override
        public B from(K key) {
            return super.from(key).type(key.type);
        }

        /** @see TypedKey#type */
        public B type(T type) {
            this.type = type;
            return (B) this;
        }

        public T type() {return this.type;}

        // ===================== End: 构建配置 ===================
    }
}
