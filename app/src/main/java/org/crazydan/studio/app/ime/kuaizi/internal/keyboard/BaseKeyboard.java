/*
 * 筷字输入法 - 高效编辑需要又好又快的输入法
 * Copyright (C) 2023 Crazydan Studio
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

package org.crazydan.studio.app.ime.kuaizi.internal.keyboard;

import java.util.ArrayList;
import java.util.List;

import org.crazydan.studio.app.ime.kuaizi.internal.InputList;
import org.crazydan.studio.app.ime.kuaizi.internal.Keyboard;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.InputMsg;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.InputMsgData;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.InputMsgListener;

/**
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-06-28
 */
public abstract class BaseKeyboard implements Keyboard {
    private final List<InputMsgListener> inputMsgListeners = new ArrayList<>();

    /** 左右手模式 */
    private HandMode handMode = HandMode.Right;

    /** 输入列表 */
    private InputList inputList;

    @Override
    public void reset() {

    }

    public HandMode handMode() {
        return this.handMode;
    }

    public void handMode(HandMode handMode) {
        this.handMode = handMode;
    }

    public InputList inputList() {
        return this.inputList;
    }

    @Override
    public void inputList(InputList inputList) {
        this.inputList = inputList;
    }

    @Override
    public void addInputMsgListener(InputMsgListener listener) {
        if (!this.inputMsgListeners.contains(listener)) {
            this.inputMsgListeners.add(listener);
        }
    }

    public void onInputMsg(InputMsg msg, InputMsgData data) {
        this.inputMsgListeners.forEach(listener -> listener.onInputMsg(msg, data));
    }
}
