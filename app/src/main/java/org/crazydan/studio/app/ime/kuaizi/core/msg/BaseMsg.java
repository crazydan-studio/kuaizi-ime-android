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

package org.crazydan.studio.app.ime.kuaizi.core.msg;

import java.util.Objects;

import org.crazydan.studio.app.ime.kuaizi.common.Immutable;

/**
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2024-12-11
 */
public abstract class BaseMsg<T, D> extends Immutable {
    /** 消息类型 */
    public final T type;
    /** 消息所携带的数据 */
    private final D data;

    protected BaseMsg(Builder<?, ?, T, D> builder) {
        super(builder);

        this.type = builder.type;
        this.data = builder.data;
    }

    /** 用于自动做 {@link #data} 的类型转换 */
    public <DD extends D> DD data() {
        return (DD) this.data;
    }

    /** {@link BaseMsg} 构建器 */
    protected static abstract class Builder< //
            B extends Builder<B, I, T, D>, //
            I extends BaseMsg<T, D>, //
            T, D //
            > extends Immutable.Builder<I> {
        private T type;
        private D data;

        // ===================== Start: 构建函数 ===================

        @Override
        protected void reset() {
            this.type = null;
            this.data = null;
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.type, this.data);
        }

        // ===================== End: 构建函数 ===================

        // ===================== Start: 按键配置 ===================

        /** @see BaseMsg#type */
        public B type(T type) {
            this.type = type;
            return (B) this;
        }

        /** @see BaseMsg#data */
        public B data(D data) {
            this.data = data;
            return (B) this;
        }

        // ===================== End: 按键配置 ===================
    }
}
