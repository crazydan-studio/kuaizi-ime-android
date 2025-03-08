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

package org.crazydan.studio.app.ime.kuaizi.core.input.clip;

import java.util.Objects;
import java.util.function.Consumer;

import org.crazydan.studio.app.ime.kuaizi.common.Immutable;

/** 剪贴板数据 */
public class ClipInputData extends Immutable {
    private final static Builder builder = new Builder();

    /** 类型（文本、图像。。。） */
    public final Type type;
    /** 内容 */
    public final String content;

    /** 创建时间 */
    public final long createdAt;
    /** 最近粘贴时间 */
    public final long usedAt;
    /** 粘贴次数 */
    public final long usedCount;
    /**
     * 是否敏感内容：密码、银行卡号等，由系统确定得出。
     * 检测得到的验证码等也将标记为敏感
     */
    public final boolean sensitive;

    /** 构建 {@link ClipInputData} */
    public static ClipInputData build(Consumer<Builder> c) {
        return Builder.build(builder, c);
    }

    /** 创建副本 */
    public ClipInputData copy(Consumer<Builder> c) {
        return Builder.copy(builder, this, c);
    }

    protected ClipInputData(Builder builder) {
        super(builder);

        this.type = builder.type;
        this.content = builder.content;

        this.createdAt = builder.createdAt;
        this.usedAt = builder.usedAt;
        this.usedCount = builder.usedCount;
        this.sensitive = builder.sensitive;
    }

    public boolean isSameWith(ClipInputData data) {
        return Objects.equals(this.type, data.type) && Objects.equals(this.content, data.content);
    }

    /** {@link ClipInputData} 的类型 */
    public enum Type {
        /** 文本 */
        text,
        /** HTML */
        html,
        /** 链接文本 */
        url,
        /** 验证码 */
        captcha,
    }

    /** {@link ClipInputData} 的构建器 */
    public static class Builder extends Immutable.Builder<ClipInputData> {
        private Type type;
        private String content;

        private long createdAt;
        private long usedAt;
        private long usedCount;
        private boolean sensitive;

        // ===================== Start: 构建函数 ===================

        @Override
        protected ClipInputData build() {
            return new ClipInputData(this);
        }

        @Override
        protected void doCopy(ClipInputData source) {
            super.doCopy(source);

            this.type = source.type;
            this.content = source.content;
            this.createdAt = source.createdAt;
            this.usedAt = source.usedAt;
            this.usedCount = source.usedCount;
            this.sensitive = source.sensitive;
        }

        @Override
        protected void reset() {
            this.type = null;
            this.content = null;

            this.createdAt = 0;
            this.usedAt = 0;
            this.usedCount = 0;
            this.sensitive = false;
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.type, this.content, this.createdAt, this.usedAt, this.usedCount, this.sensitive);
        }

        // ===================== End: 构建函数 ===================

        // ===================== Start: 构建配置 ===================

        /** @see ClipInputData#type */
        public Builder type(Type type) {
            this.type = type;
            return this;
        }

        /** @see ClipInputData#content */
        public Builder content(String content) {
            this.content = content;
            return this;
        }

        /** @see ClipInputData#createdAt */
        public Builder createdAt(long createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        /** @see ClipInputData#usedAt */
        public Builder usedAt(long usedAt) {
            this.usedAt = usedAt;
            return this;
        }

        /** @see ClipInputData#usedCount */
        public Builder usedCount(long usedCount) {
            this.usedCount = usedCount;
            return this;
        }

        /** @see ClipInputData#sensitive */
        public Builder sensitive(boolean sensitive) {
            this.sensitive = sensitive;
            return this;
        }

        // ===================== End: 构建配置 ===================
    }
}
