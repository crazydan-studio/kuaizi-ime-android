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

package org.crazydan.studio.app.ime.kuaizi.ui.view;

import android.content.Context;
import android.util.AttributeSet;
import androidx.annotation.Nullable;
import org.crazydan.studio.app.ime.kuaizi.conf.ConfigKey;
import org.crazydan.studio.app.ime.kuaizi.core.Inputboard;
import org.crazydan.studio.app.ime.kuaizi.core.msg.InputMsg;
import org.crazydan.studio.app.ime.kuaizi.core.msg.InputMsgListener;
import org.crazydan.studio.app.ime.kuaizi.core.msg.input.ConfigUpdateMsgData;

/**
 * {@link Inputboard} 的视图
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-06-30
 */
public class InputboardView extends InputboardViewBase implements InputMsgListener {

    public InputboardView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    // =============================== Start: 消息处理 ===================================

    @Override
    public void onMsg(InputMsg msg) {
        switch (msg.type) {
            case Config_Update_Done: {
                ConfigUpdateMsgData data = msg.data();
                // Note: 仅关注与视图相关的配置更新
                if (data.key != ConfigKey.theme //
                    && data.key != ConfigKey.enable_candidate_variant_first) {
                    break;
                }
            }
            case Input_Selected_Delete_Done:
            case Input_Pending_Drop_Done:
            case Input_Completion_Apply_Done:
            case InputList_Clean_Done:
            case InputList_Cleaned_Cancel_Done:
            case Keyboard_Switch_Done:
            case Keyboard_Start_Done:
            case Keyboard_State_Change_Done:
            case InputChars_Input_Doing:
            case InputChars_Input_Done:
            case InputCandidate_Choose_Doing:
            case InputCandidate_Choose_Done:
            case InputList_Commit_Doing:
            case InputList_PairSymbol_Commit_Doing:
            case InputList_Committed_Revoke_Doing:
                update(msg.inputFactory);
                break;
        }

        super.onMsg(msg);
    }

    // =============================== End: 消息处理 ===================================
}
