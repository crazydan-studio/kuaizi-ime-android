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

package org.crazydan.studio.app.ime.kuaizi.internal.view.completion;

import org.crazydan.studio.app.ime.kuaizi.internal.input.CompletionInput;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.UserInputMsg;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.UserInputMsgData;
import org.crazydan.studio.app.ime.kuaizi.internal.view.InputCompletionsView;
import org.crazydan.studio.app.ime.kuaizi.widget.ViewGestureDetector;

/**
 * {@link InputCompletionsView} 的手势监听器
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-10-12
 */
public class CompletionViewGestureListener implements ViewGestureDetector.Listener {
    private final InputCompletionsView inputCompletionsView;

    public CompletionViewGestureListener(InputCompletionsView inputCompletionsView) {
        this.inputCompletionsView = inputCompletionsView;
    }

    @Override
    public void onGesture(ViewGestureDetector.GestureType type, ViewGestureDetector.GestureData data) {
        CompletionView completionView = this.inputCompletionsView.findCompletionViewUnder(data.x, data.y);
        if (completionView == null) {
            return;
        }

        if (type == ViewGestureDetector.GestureType.SingleTap) {
            onSingleTap(completionView, data);
        }
    }

    private void onSingleTap(CompletionView completionView, ViewGestureDetector.GestureData data) {
        CompletionInput completion = completionView.getData();

        UserInputMsg msg = UserInputMsg.Input_Completion_Choose_Doing;
        UserInputMsgData msgData = new UserInputMsgData(completion);
        msg.send(this.inputCompletionsView.getInputList(), msgData);
    }
}
