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

import java.util.Date;
import java.util.Objects;
import java.util.function.Consumer;

import org.crazydan.studio.app.ime.kuaizi.common.Immutable;

/**
 * 输入收藏
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-07-06
 */
public class InputFavorite extends Immutable {
    private final static Builder builder = new Builder();

    /** 对象 id，一般对应持久化的主键值 */
    public final Integer id;

    /** 文本内容 */
    public final String text;

    /** 创建时间 */
    public final Date createdAt;
    /** 最近使用时间 */
    public final Date usedAt;
    /** 使用次数 */
    public final long usedCount;

    /** 是否敏感内容：密码、银行卡号、验证码等。在使用时，需逐字输入 */
    public final boolean sensitive;

    /** 构建 {@link InputFavorite} */
    public static InputFavorite build(Consumer<Builder> c) {
        return Builder.build(builder, c);
    }

    /** 创建副本 */
    public InputFavorite copy(Consumer<Builder> c) {
        return Builder.copy(builder, this, c);
    }

    protected InputFavorite(Builder builder) {
        super(builder);

        this.id = builder.id;
        this.text = builder.text;

        this.createdAt = builder.createdAt;
        this.usedAt = builder.usedAt;
        this.usedCount = builder.usedCount;
        this.sensitive = builder.sensitive;
    }

    /** {@link InputFavorite} 的构建器 */
    public static class Builder extends Immutable.Builder<InputFavorite> {
        private Integer id;
        private String text;

        private Date createdAt;
        private Date usedAt;
        private long usedCount;
        private boolean sensitive;

        // ===================== Start: 构建函数 ===================

        @Override
        protected InputFavorite build() {
            return new InputFavorite(this);
        }

        @Override
        protected void doCopy(InputFavorite source) {
            super.doCopy(source);

            this.id = source.id;
            this.text = source.text;

            this.createdAt = source.createdAt;
            this.usedAt = source.usedAt;
            this.usedCount = source.usedCount;
            this.sensitive = source.sensitive;
        }

        @Override
        protected void reset() {
            this.id = null;
            this.text = null;

            this.createdAt = null;
            this.usedAt = null;
            this.usedCount = 0;
            this.sensitive = false;
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.id, this.text, this.createdAt, this.usedAt, this.usedCount, this.sensitive);
        }

        // ===================== End: 构建函数 ===================

        // ===================== Start: 构建配置 ===================

        /** @see InputFavorite#id */
        public Builder id(Integer id) {
            this.id = id;
            return this;
        }

        /** @see InputFavorite#text */
        public Builder text(String text) {
            this.text = text;
            return this;
        }

        /** @see InputFavorite#createdAt */
        public Builder createdAt(Date createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        /** @see InputFavorite#usedAt */
        public Builder usedAt(Date usedAt) {
            this.usedAt = usedAt;
            return this;
        }

        /** @see InputFavorite#usedCount */
        public Builder usedCount(long usedCount) {
            this.usedCount = usedCount;
            return this;
        }

        /** @see InputFavorite#sensitive */
        public Builder sensitive(boolean sensitive) {
            this.sensitive = sensitive;
            return this;
        }

        // ===================== End: 构建配置 ===================
    }
}
