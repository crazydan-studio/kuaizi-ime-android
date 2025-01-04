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

package org.crazydan.studio.app.ime.kuaizi.core.msg;

import org.crazydan.studio.app.ime.kuaizi.core.Input;
import org.crazydan.studio.app.ime.kuaizi.core.InputFactory;
import org.crazydan.studio.app.ime.kuaizi.core.Key;
import org.crazydan.studio.app.ime.kuaizi.core.KeyFactory;
import org.crazydan.studio.app.ime.kuaizi.core.Keyboard;
import org.crazydan.studio.app.ime.kuaizi.core.input.InputList;

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
        this(type, data, null, null);
    }

    public InputMsg(
            InputMsgType type, InputMsgData data, //
            InputList inputList, KeyFactory keyFactory
    ) {
        super(type, data);

        this.keyFactory = keyFactory;
        this.inputFactory = inputList != null ? inputList.getInputFactory() : null;
        this.inputList = new InputListState(inputList);
    }

    public static class InputListState {
        /** 输入列表是否为空 */
        public final boolean empty;
        /** 输入列表的 已删除 是否可取消 */
        public final boolean deletedCancelable;

        private InputListState(InputList inputList) {
            this.empty = inputList == null || inputList.isEmpty();
            this.deletedCancelable = inputList != null && inputList.canCancelDelete();
        }
    }
}
