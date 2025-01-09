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
    /** 用于重新布局 {@link Key} */
    public final KeyFactory keyFactory;
    /** 用于重新布局 {@link Input} */
    public final InputFactory inputFactory;

    /** 输入列表状态 */
    public final InputListState inputList;

    public InputMsg(InputMsgType type, InputMsgData data) {
        this(type, data, null, null, false, false);
    }

    public InputMsg(
            InputMsgType type, InputMsgData data, //
            KeyFactory keyFactory, InputFactory inputFactory, //
            boolean isEmptyInputList, boolean canCancelCleanInputList
    ) {
        super(type, data);

        this.keyFactory = keyFactory;
        this.inputFactory = inputFactory;

        this.inputList = new InputListState(isEmptyInputList, canCancelCleanInputList);
    }

    public static class InputListState {
        /** 输入列表是否为空 */
        public final boolean empty;
        /** 是否可取消对输入列表的清空 */
        public final boolean canCancelClean;

        public InputListState(boolean empty, boolean canCancelClean) {
            this.empty = empty;
            this.canCancelClean = canCancelClean;
        }
    }
}
