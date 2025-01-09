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

package org.crazydan.studio.app.ime.kuaizi.core.input;

import java.util.Objects;

import org.crazydan.studio.app.ime.kuaizi.common.Immutable;
import org.crazydan.studio.app.ime.kuaizi.core.Input;

/**
 * {@link Input 输入}对应的{@link Input#getWord() 字}
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-07-08
 */
public abstract class InputWord extends Immutable {
    /** 对象 id，一般对应持久化的主键值 */
    public final Integer id;
    /** 值 */
    public final String value;
    /** 字的权重 */
    public final int weight;

    protected InputWord(Builder<?, ?> builder) {
        super(builder);

        this.id = builder.id;
        this.value = builder.value;
        this.weight = builder.weight;
    }

    @Override
    public String toString() {
        return this.value + '(' + this.id + ')';
    }

    /** {@link InputWord} 的构建器 */
    protected static abstract class Builder< //
            B extends Builder<B, W>, //
            W extends InputWord //
            > extends Immutable.Builder<W> {
        private Integer id;
        private String value;
        private int weight;

        // ===================== Start: 构建函数 ===================

        @Override
        protected void reset() {
            this.id = null;
            this.value = null;
            this.weight = 0;
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.id, this.value, this.weight);
        }

        // ===================== End: 构建函数 ===================

        // ===================== Start: 构建配置 ===================

        /** @see InputWord#id */
        public B id(Integer id) {
            this.id = id;
            return (B) this;
        }

        /** @see InputWord#value */
        public B value(String value) {
            this.value = value;
            return (B) this;
        }

        /** @see InputWord#weight */
        public B weight(int weight) {
            this.weight = weight;
            return (B) this;
        }

        // ===================== End: 构建配置 ===================
    }
}
