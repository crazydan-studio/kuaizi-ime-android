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
