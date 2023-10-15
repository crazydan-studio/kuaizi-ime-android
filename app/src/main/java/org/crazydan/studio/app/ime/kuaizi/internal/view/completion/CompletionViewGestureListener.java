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
import org.crazydan.studio.app.ime.kuaizi.widget.recycler.RecyclerViewGestureDetector;

/**
 * {@link InputCompletionsView} 的手势监听器
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-10-12
 */
public class CompletionViewGestureListener implements RecyclerViewGestureDetector.Listener {
    private final InputCompletionsView inputCompletionsView;

    public CompletionViewGestureListener(InputCompletionsView inputCompletionsView) {
        this.inputCompletionsView = inputCompletionsView;
    }

    @Override
    public void onGesture(RecyclerViewGestureDetector.GestureType type, RecyclerViewGestureDetector.GestureData data) {
        CompletionView completionView = this.inputCompletionsView.findCompletionViewUnder(data.x, data.y);
        if (completionView == null) {
            return;
        }

        switch (type) {
            case SingleTap:
                onSingleTap(completionView, data);
                break;
        }
    }

    private void onSingleTap(CompletionView completionView, RecyclerViewGestureDetector.GestureData data) {
        CompletionInput completion = completionView.getData();

        UserInputMsgData msgData = new UserInputMsgData(completion);
        this.inputCompletionsView.onUserInputMsg(UserInputMsg.Input_Completion_Choose_Doing, msgData);
    }
}
