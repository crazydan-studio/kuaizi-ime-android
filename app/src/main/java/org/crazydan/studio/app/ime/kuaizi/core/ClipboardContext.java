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

package org.crazydan.studio.app.ime.kuaizi.core;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * {@link Clipboard} 的上下文
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2025-02-21
 */
public class ClipboardContext extends BaseInputContext {
    private final static Builder builder = new Builder();

    // <<<<<<<<<<<<<<<<<<<<<<<<< 配置信息
    /** 已使用的剪贴数据标识 */
    public final String usedClipCode;
    /** 是否已禁用剪贴数据 */
    public final boolean clipsDisabled;
    // >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>

    /** 构建 {@link ClipboardContext} */
    public static ClipboardContext build(Consumer<Builder> c) {
        return Builder.build(builder, c);
    }

    ClipboardContext(Builder builder) {
        super(builder);

        this.usedClipCode = builder.usedClipCode;
        this.clipsDisabled = builder.clipsDisabled;
    }

    /** {@link ClipboardContext} 的构建器 */
    public static class Builder extends BaseInputContext.Builder<Builder, ClipboardContext> {
        private String usedClipCode;
        private boolean clipsDisabled;

        protected Builder() {
            super(1);
        }

        // ===================== Start: 构建函数 ===================

        @Override
        protected ClipboardContext doBuild() {
            return new ClipboardContext(this);
        }

        @Override
        protected void reset() {
            super.reset();

            this.usedClipCode = null;
            this.clipsDisabled = false;
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), this.usedClipCode, this.clipsDisabled);
        }

        // ===================== End: 构建函数 ===================

        // ===================== Start: 构建配置 ===================

        /** @see ClipboardContext#usedClipCode */
        public Builder usedClipCode(String usedClipCode) {
            this.usedClipCode = usedClipCode;
            return this;
        }

        /** @see ClipboardContext#clipsDisabled */
        public Builder clipsDisabled(boolean clipsDisabled) {
            this.clipsDisabled = clipsDisabled;
            return this;
        }

        // ===================== End: 构建配置 ===================
    }
}
