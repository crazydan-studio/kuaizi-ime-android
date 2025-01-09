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

import org.crazydan.studio.app.ime.kuaizi.conf.Config;
import org.crazydan.studio.app.ime.kuaizi.conf.ConfigKey;

/**
 * {@link Inputboard} 的上下文
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2024-12-15
 */
public class InputboardContext extends BaseInputContext {
    private final static Builder builder = new Builder();

    // <<<<<<<<<<<<<<<<<<<<<<<<< 配置信息
    /** 是否优先使用候选字的变体：主要针对拼音输入的候选字 */
    public final boolean useCandidateVariantFirst;
    // >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>

    /** 构建 {@link InputboardContext} */
    public static InputboardContext build(Consumer<Builder> c) {
        return Builder.build(builder, c);
    }

    InputboardContext(Builder builder) {
        super(builder);

        this.useCandidateVariantFirst = builder.useCandidateVariantFirst;
    }

    /** 创建默认的 {@link Input.Option} */
    public Input.Option createInputOption() {
        return new Input.Option(null, this.useCandidateVariantFirst);
    }

    /** {@link InputboardContext} 的构建器 */
    public static class Builder extends BaseInputContext.Builder<Builder, InputboardContext> {
        private boolean useCandidateVariantFirst;

        protected Builder() {
            super(1);
        }

        // ===================== Start: 构建函数 ===================

        @Override
        protected InputboardContext doBuild() {
            return new InputboardContext(this);
        }

        @Override
        protected void reset() {
            super.reset();

            this.useCandidateVariantFirst = false;
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), this.useCandidateVariantFirst);
        }

        // ===================== End: 构建函数 ===================

        // ===================== Start: 构建配置 ===================

        /** 设置配置信息 */
        public Builder config(Config config) {
            this.useCandidateVariantFirst = config.bool(ConfigKey.enable_candidate_variant_first);

            return this;
        }

        // ===================== End: 构建配置 ===================
    }
}
