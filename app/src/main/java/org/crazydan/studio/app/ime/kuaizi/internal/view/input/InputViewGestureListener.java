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
import org.crazydan.studio.app.ime.kuaizi.internal.InputList;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.UserInputMsg;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.UserInputMsgData;
import org.crazydan.studio.app.ime.kuaizi.internal.view.InputListView;
import org.crazydan.studio.app.ime.kuaizi.internal.view.RecyclerViewGestureDetector;

/**
 * {@link InputList 输入列表}的手势监听器
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-07-21
 */
public class InputViewGestureListener implements RecyclerViewGestureDetector.Listener {
    private final InputListView inputListView;

    public InputViewGestureListener(InputListView inputListView) {
        this.inputListView = inputListView;
    }

    @Override
    public void onGesture(RecyclerViewGestureDetector.GestureType type, RecyclerViewGestureDetector.GestureData data) {
        InputView<?> inputView = this.inputListView.findVisibleInputViewUnder(data.x, data.y);

        switch (type) {
            case SingleTap:
                onSingleTap(inputView, data);
                break;
        }
    }

    private void onSingleTap(InputView<?> inputView, RecyclerViewGestureDetector.GestureData data) {
        Input<?> input = getInput(inputView);
        if (input == null) {
            if (data.x < this.inputListView.getPaddingStart()) {
                input = this.inputListView.getFirstInput();
            } else {
                input = this.inputListView.getLastInput();
            }
        }

        if (input == null) {
            return;
        }

        UserInputMsgData msgData = new UserInputMsgData(input);
        this.inputListView.onUserInputMsg(UserInputMsg.Choosing_Input, msgData);
    }

    private Input<?> getInput(InputView<?> inputView) {
        return inputView != null ? inputView.getData() : null;
    }
}
