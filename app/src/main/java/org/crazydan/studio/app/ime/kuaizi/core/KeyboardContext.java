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
import org.crazydan.studio.app.ime.kuaizi.core.input.InputList;
import org.crazydan.studio.app.ime.kuaizi.core.msg.InputMsg;
import org.crazydan.studio.app.ime.kuaizi.core.msg.InputMsgListener;
import org.crazydan.studio.app.ime.kuaizi.core.msg.UserKeyMsg;

/**
 * {@link Keyboard} 的上下文，以参数形式向键盘传递上下文信息
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2024-12-23
 */
public class KeyboardContext extends Immutable {
    private final static Builder builder = new Builder();

    public final KeyboardConfig config;
    /** 与当前上下文直接关联的 {@link Key}，一般为触发 {@link UserKeyMsg} 消息所对应的按键，可能为 null */
    public final Key key;
    /** 当前正在处理的 {@link InputList}，可在 {@link Keyboard} 内直接修改其输入 */
    public final InputList inputList;
    /** 接收 {@link Keyboard} 所发送的 {@link InputMsg} 消息的监听器 */
    public final InputMsgListener listener;

    /** 是否可撤回对输入的提交 */
    public final boolean canRevokeInputsCommit;
    /** 是否可取消对输入的清空 */
    public final boolean canCancelInputsClean;

    /** 构建 {@link KeyboardContext} */
    public static KeyboardContext build(Consumer<Builder> c) {
        return KeyboardContext.Builder.build(builder, c);
    }

    KeyboardContext(Builder builder) {
        super(builder);

        this.config = config;
        this.inputList = inputList;
        this.listener = listener;
        this.key = key;
    }

    /** 根据 {@link Key} 新建实例，以使其携带该 {@link #key()} */
    public KeyboardContext newWithKey(Key key) {
        KeyboardContext context = new KeyboardContext(this.config, this.inputList, this.listener, key);
        return context.canRevokeInputsCommit(this.canRevokeInputsCommit)
                      .canCancelInputsClean(this.canCancelInputsClean);
    }

    /** 根据 {@link InputList} 新建实例，以使其携带新的 {@link InputList} */
    public KeyboardContext newWithInputList(InputList inputList) {
        KeyboardContext context = new KeyboardContext(this.config, inputList, this.listener, this.key);
        return context.canRevokeInputsCommit(this.canRevokeInputsCommit)
                      .canCancelInputsClean(this.canCancelInputsClean);
    }

    /** {@link #key} 的泛型转换接口，避免编写显式的类型转换代码 */
    public <T extends Key> T key() {
        return (T) this.key;
    }

    /** {@link KeyboardContext} 的构建器 */
    public static class Builder extends Immutable.Builder<KeyboardContext> {
        private Key key;
        private InputList inputList;
        private InputMsgListener listener;

        private boolean canRevokeInputsCommit;
        private boolean canCancelInputsClean;

        // ===================== Start: 构建函数 ===================

        @Override
        protected KeyboardContext build() {
            return new KeyboardContext(this);
        }

        @Override
        protected void reset() {
        }

        @Override
        public int hashCode() {
            return Objects.hash();
        }

        // ===================== End: 构建函数 ===================

        // ===================== Start: 构建配置 ===================

        /** @see KeyboardContext#key */
        public Builder key(Key key) {
            this.key = key;
            return this;
        }

        /** @see KeyboardContext#inputList */
        public Builder inputList(InputList inputList) {
            this.inputList = inputList;
            return this;
        }

        /** @see KeyboardContext#canRevokeInputsCommit */
        public Builder canRevokeInputsCommit(boolean canRevokeInputsCommit) {
            this.canRevokeInputsCommit = canRevokeInputsCommit;
            return this;
        }

        /** @see KeyboardContext#canCancelInputsClean */
        public Builder canCancelInputsClean(boolean canCancelInputsClean) {
            this.canCancelInputsClean = canCancelInputsClean;
            return this;
        }

        // ===================== End: 构建配置 ===================
    }
}
