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

    /** 当前的 {@link Keyboard} 配置信息 */
    public final KeyboardConfig config;

    /** 与当前上下文直接关联的 {@link Key}，一般为触发 {@link UserKeyMsg} 消息所对应的按键，可能为 null */
    public final Key key;
    /** 当前正在处理的 {@link InputList}，可在 {@link Keyboard} 内直接修改其输入 */
    public final InputList inputList;
    /** 接收 {@link Keyboard} 所发送的 {@link InputMsg} 消息的监听器 */
    public final InputMsgListener listener;

    /** 构建 {@link KeyboardContext} */
    public static KeyboardContext build(Consumer<Builder> c) {
        return Builder.build(builder, c);
    }

    KeyboardContext(Builder builder) {
        super(builder);

        this.config = builder.config;
        this.key = builder.key;
        this.inputList = builder.inputList;
        this.listener = builder.listener;
    }

    /** 创建副本 */
    public KeyboardContext copy(Consumer<Builder> c) {
        return Builder.copy(builder, this, c);
    }

    /** {@link #key} 的泛型转换接口，避免编写显式的类型转换代码 */
    public <T extends Key> T key() {
        return (T) this.key;
    }

    /** {@link KeyboardContext} 的构建器 */
    public static class Builder extends Immutable.CachableBuilder<KeyboardContext> {
        private KeyboardConfig config;
        private Key key;
        private InputList inputList;
        private InputMsgListener listener;

        protected Builder() {
            super(2);
        }

        // ===================== Start: 构建函数 ===================

        @Override
        protected KeyboardContext doBuild() {
            return new KeyboardContext(this);
        }

        @Override
        protected void doCopy(KeyboardContext context) {
            config(context.config).key(context.key).inputList(context.inputList).listener(context.listener);
        }

        @Override
        protected void reset() {
            this.config = null;
            this.key = null;
            this.inputList = null;
            this.listener = null;
        }

        @Override
        public int hashCode() {
            // Note: InputList 与 InputMsgListener 采用其引用值，
            // 因为，在上下文使用过程中，二者的实例不会发生变化，可以更好地复用
            return Objects.hash(this.config,
                                this.key,
                                System.identityHashCode(this.inputList),
                                System.identityHashCode(this.listener));
        }

        // ===================== End: 构建函数 ===================

        // ===================== Start: 构建配置 ===================

        /** @see KeyboardContext#config */
        public Builder config(KeyboardConfig config) {
            this.config = config;
            return this;
        }

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

        /** @see KeyboardContext#listener */
        public Builder listener(InputMsgListener listener) {
            this.listener = listener;
            return this;
        }

        // ===================== End: 构建配置 ===================
    }
}
