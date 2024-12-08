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

package org.crazydan.studio.app.ime.kuaizi.pane.view;

import android.content.Context;
import android.util.AttributeSet;
import androidx.annotation.Nullable;
import org.crazydan.studio.app.ime.kuaizi.pane.InputList;
import org.crazydan.studio.app.ime.kuaizi.pane.Keyboard;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.KeyboardMsg;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.KeyboardMsgData;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.KeyboardMsgListener;

/**
 * {@link InputList 输入列表}的视图
 * <p/>
 * 由上层视图向下分发 {@link KeyboardMsg} 消息
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-06-30
 */
public class InputListView extends InputListViewBase implements KeyboardMsgListener {

    public InputListView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void onMsg(Keyboard keyboard, KeyboardMsg msg, KeyboardMsgData data) {
        switch (msg) {
            case Keyboard_Config_Update_Done:
            case Keyboard_Switch_Done:
            case Keyboard_Start_Done:
            case Keyboard_State_Change_Done:
            case InputChars_Input_Doing:
            case InputChars_Input_Done:
            case InputCandidate_Choose_Doing:
            case InputCandidate_Choose_Done:
            case Emoji_Choose_Doing:
            case Symbol_Choose_Doing:
            case InputList_Input_Completion_Apply_Done:
            case InputList_Pending_Drop_Done:
            case InputList_Selected_Delete_Done:
            case InputList_Commit_Doing:
            case InputList_PairSymbol_Commit_Doing:
            case InputList_Committed_Revoke_Doing:
//                update(inputList, true);
                break;
        }
    }
}
