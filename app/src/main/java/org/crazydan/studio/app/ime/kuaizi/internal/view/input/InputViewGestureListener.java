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

package org.crazydan.studio.app.ime.kuaizi.internal.view.input;

import org.crazydan.studio.app.ime.kuaizi.internal.Input;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.MsgBus;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.UserInputMsg;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.UserInputMsgData;
import org.crazydan.studio.app.ime.kuaizi.internal.view.InputListView;
import org.crazydan.studio.app.ime.kuaizi.widget.ViewGestureDetector;

/**
 * {@link InputListView} 的手势监听器
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-07-21
 */
public class InputViewGestureListener implements ViewGestureDetector.Listener {
    private final InputListView inputListView;

    public InputViewGestureListener(InputListView inputListView) {
        this.inputListView = inputListView;
    }

    @Override
    public void onGesture(ViewGestureDetector.GestureType type, ViewGestureDetector.GestureData data) {
        InputView<?> inputView = this.inputListView.findVisibleInputViewUnder(data.x, data.y);

        if (type == ViewGestureDetector.GestureType.SingleTap) {
            onSingleTap(inputView, data);
        }
    }

    private void onSingleTap(InputView<?> inputView, ViewGestureDetector.GestureData data) {
        Input<?> input = determineInput(inputView, data);
        if (input == null) {
            return;
        }

        UserInputMsgData msgData = new UserInputMsgData(input);
        MsgBus.send(this.inputListView.getInputList(), UserInputMsg.Input_Choose_Doing, msgData);
    }

    private Input<?> determineInput(InputView<?> inputView, ViewGestureDetector.GestureData data) {
        Input<?> input = inputView != null ? inputView.getData() : null;

        if (input == null) {
            if (data.x < this.inputListView.getPaddingStart()) {
                input = this.inputListView.getInputList().getFirstInput();
            } else {
                input = this.inputListView.getInputList().getLastInput();
            }
        }
        return input;
    }
}
