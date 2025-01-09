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
import java.util.function.Consumer;

import org.crazydan.studio.app.ime.kuaizi.core.Input;
import org.crazydan.studio.app.ime.kuaizi.core.InputFactory;
import org.crazydan.studio.app.ime.kuaizi.core.InputList;
import org.crazydan.studio.app.ime.kuaizi.core.Key;
import org.crazydan.studio.app.ime.kuaizi.core.KeyFactory;
import org.crazydan.studio.app.ime.kuaizi.core.Keyboard;

/**
 * {@link Keyboard} 和 {@link InputList} 所发送的消息
 * <p/>
 * 输入状态变更相关的消息
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2024-12-11
 */
public class InputMsg extends BaseMsg<InputMsgType, InputMsgData> {
    private final static Builder builder = new Builder();

    /** 用于重新布局 {@link Key} */
    public final KeyFactory keyFactory;
    /** 用于重新布局 {@link Input} */
    public final InputFactory inputFactory;

    /** 输入列表状态 */
    public final InputListState inputList;

    /** 构建 {@link InputMsg} */
    public static InputMsg build(Consumer<Builder> c) {
        return Builder.build(builder, c);
    }

    InputMsg(Builder builder) {
        super(builder);

        this.keyFactory = builder.keyFactory;
        this.inputFactory = builder.inputFactory;

        this.inputList = builder.inputList;
    }

    public static class InputListState {
        /** 输入列表是否已冻结 */
        public final boolean frozen;
        /** 输入列表是否为空 */
        public final boolean empty;
        /** 是否可取消对输入列表的清空 */
        public final boolean canCancelClean;

        InputListState(boolean frozen, boolean empty, boolean canCancelClean) {
            this.frozen = frozen;
            this.empty = empty;
            this.canCancelClean = canCancelClean;
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.frozen, this.empty, this.canCancelClean);
        }
    }

    /** {@link InputMsg} 的构建器 */
    public static class Builder extends BaseMsg.Builder<Builder, InputMsg, InputMsgType, InputMsgData> {
        private KeyFactory keyFactory;
        private InputFactory inputFactory;

        private InputListState inputList;

        // ===================== Start: 构建函数 ===================

        @Override
        protected InputMsg build() {
            return new InputMsg(this);
        }

        @Override
        protected void reset() {
            super.reset();

            this.keyFactory = null;
            this.inputFactory = null;
            this.inputList = null;
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), this.keyFactory, this.inputFactory, this.inputList);
        }

        // ===================== End: 构建函数 ===================

        // ===================== Start: 构建配置 ===================

        /** @see InputMsg#keyFactory */
        public Builder keyFactory(KeyFactory keyFactory) {
            this.keyFactory = keyFactory;
            return this;
        }

        /** @see InputMsg#inputFactory */
        public Builder inputFactory(InputFactory inputFactory) {
            this.inputFactory = inputFactory;
            return this;
        }

        /** @see InputMsg#inputList */
        public Builder inputList(InputList inputList, boolean canCancelCleanInputList) {
            this.inputList = new InputListState(inputList.isFrozen(), inputList.isEmpty(), canCancelCleanInputList);
            return this;
        }

        // ===================== End: 构建配置 ===================
    }
}
