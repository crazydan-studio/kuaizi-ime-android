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

import org.crazydan.studio.app.ime.kuaizi.common.Immutable;
import org.crazydan.studio.app.ime.kuaizi.core.msg.InputMsg;
import org.crazydan.studio.app.ime.kuaizi.core.msg.InputMsgData;
import org.crazydan.studio.app.ime.kuaizi.core.msg.InputMsgListener;
import org.crazydan.studio.app.ime.kuaizi.core.msg.InputMsgType;

/**
 * {@link Inputboard} 与 {@link Keyboard} 的上下文基类
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2024-12-15
 */
public abstract class BaseInputContext extends Immutable {
    /** 当前正在处理的 {@link InputList}，可在 {@link Inputboard} 或 {@link Keyboard} 内直接修改其输入 */
    public final InputList inputList;

    /** 接收 {@link Inputboard} 或 {@link Keyboard} 所发送的 {@link InputMsg} 消息的监听器 */
    protected final InputMsgListener listener;

    protected BaseInputContext(Builder<?, ?> builder) {
        super(builder);

        this.inputList = builder.inputList;
        this.listener = builder.listener;
    }

    /** 发送 {@link InputMsg} 消息 */
    public void fireInputMsg(InputMsgType type, Input input) {
        fireInputMsg(type, null, input);
    }

    /** 发送 {@link InputMsg} 消息 */
    public void fireInputMsg(InputMsgType type, Key key, Input input) {
        InputMsgData data = new InputMsgData(key, input);

        fireInputMsg(type, data);
    }

    /** 触发 {@link InputMsg} 消息 */
    public void fireInputMsg(InputMsgType msgType, InputMsgData msgData) {
        InputMsg msg = new InputMsg(msgType, msgData);

        this.listener.onMsg(msg);
    }

    /** {@link BaseInputContext} 的构建器 */
    protected static abstract class Builder< //
            B extends Builder<B, I>, //
            I extends BaseInputContext //
            > extends CachableBuilder<I> {
        private InputList inputList;
        private InputMsgListener listener;

        protected Builder(int cacheSize) {
            super(cacheSize);
        }

        // ===================== Start: 构建函数 ===================

        @Override
        protected void doCopy(I source) {
            super.doCopy(source);

            inputList(source.inputList).listener(source.listener);
        }

        @Override
        protected void reset() {
            this.inputList = null;
            this.listener = null;
        }

        @Override
        public int hashCode() {
            // Note: InputList 与 InputMsgListener 采用其引用值，
            // 因为，在上下文使用过程中，二者的实例不会发生变化，可以更好地复用
            return Objects.hash(System.identityHashCode(this.inputList), System.identityHashCode(this.listener));
        }

        // ===================== End: 构建函数 ===================

        // ===================== Start: 构建配置 ===================

        /** @see BaseInputContext#inputList */
        public B inputList(InputList inputList) {
            this.inputList = inputList;
            return (B) this;
        }

        /** @see BaseInputContext#listener */
        public B listener(InputMsgListener listener) {
            this.listener = listener;
            return (B) this;
        }

        // ===================== End: 构建配置 ===================
    }
}
