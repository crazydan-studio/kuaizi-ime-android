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

package org.crazydan.studio.app.ime.kuaizi.core;

import java.util.Objects;
import java.util.function.Consumer;

import org.crazydan.studio.app.ime.kuaizi.common.Immutable;
import org.crazydan.studio.app.ime.kuaizi.conf.Config;
import org.crazydan.studio.app.ime.kuaizi.conf.ConfigKey;
import org.crazydan.studio.app.ime.kuaizi.core.input.InputList;
import org.crazydan.studio.app.ime.kuaizi.core.msg.InputMsg;
import org.crazydan.studio.app.ime.kuaizi.core.msg.InputMsgListener;

/**
 * {@link Inputboard} 的上下文
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2024-12-15
 */
public class InputboardContext extends Immutable {
    private final static Builder builder = new Builder();

    /** 当前正在处理的 {@link InputList}，可在 {@link Inputboard} 内直接修改其输入 */
    public final InputList inputList;
    /** 接收 {@link Inputboard} 所发送的 {@link InputMsg} 消息的监听器 */
    public final InputMsgListener listener;

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

        this.inputList = builder.inputList;
        this.listener = builder.listener;

        this.useCandidateVariantFirst = builder.useCandidateVariantFirst;
    }

    /** 创建默认的 {@link Input.Option} */
    public Input.Option createInputOption() {
        return new Input.Option(null, this.useCandidateVariantFirst);
    }

    /** {@link InputboardContext} 的构建器 */
    public static class Builder extends Immutable.CachableBuilder<InputboardContext> {
        private InputList inputList;
        private InputMsgListener listener;

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
            this.inputList = null;
            this.listener = null;

            this.useCandidateVariantFirst = false;
        }

        @Override
        public int hashCode() {
            // Note: InputList 与 InputMsgListener 采用其引用值，
            // 因为，在上下文使用过程中，二者的实例不会发生变化，可以更好地复用
            return Objects.hash(System.identityHashCode(this.inputList),
                                System.identityHashCode(this.listener),
                                this.useCandidateVariantFirst);
        }

        // ===================== End: 构建函数 ===================

        // ===================== Start: 构建配置 ===================

        /** 设置配置信息 */
        public Builder config(Config config) {
            this.useCandidateVariantFirst = config.bool(ConfigKey.enable_candidate_variant_first);

            return this;
        }

        /** @see InputboardContext#inputList */
        public Builder inputList(InputList inputList) {
            this.inputList = inputList;
            return this;
        }

        /** @see InputboardContext#listener */
        public Builder listener(InputMsgListener listener) {
            this.listener = listener;
            return this;
        }

        // ===================== End: 构建配置 ===================
    }
}
