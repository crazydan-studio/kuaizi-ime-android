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

package org.crazydan.studio.app.ime.kuaizi.ui.view;

import android.content.Context;
import android.util.AttributeSet;
import androidx.annotation.Nullable;
import org.crazydan.studio.app.ime.kuaizi.common.utils.CollectionUtils;
import org.crazydan.studio.app.ime.kuaizi.conf.ConfigKey;
import org.crazydan.studio.app.ime.kuaizi.core.InputList;
import org.crazydan.studio.app.ime.kuaizi.core.msg.InputMsg;
import org.crazydan.studio.app.ime.kuaizi.core.msg.InputMsgListener;
import org.crazydan.studio.app.ime.kuaizi.core.msg.input.ConfigUpdateMsgData;

/**
 * {@link InputList} 的视图
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-06-30
 */
public class InputListView extends InputListViewBase implements InputMsgListener {

    public InputListView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    // =============================== Start: 消息处理 ===================================

    @Override
    public void onMsg(InputMsg msg) {
        switch (msg.type) {
            case Config_Update_Done: {
                ConfigUpdateMsgData data = msg.data();
                // Note: 仅关注与输入列表布局和显示相关的配置更新
                ConfigKey[] effects = new ConfigKey[] {
                        ConfigKey.theme, ConfigKey.enable_candidate_variant_first
                };
                if (!CollectionUtils.contains(effects, data.key)) {
                    break;
                }
            }
            case Keyboard_Switch_Done:
            case Keyboard_Start_Done:
            case Keyboard_State_Change_Done:
                //
            case InputChars_Input_Doing:
            case InputChars_Input_Done:
            case InputCandidate_Choose_Doing:
            case InputCandidate_Choose_Done:
                //
            case Input_Selected_Delete_Done:
            case Input_Pending_Drop_Done:
            case Input_Completion_Apply_Done:
                //
            case InputList_Clean_Done:
            case InputList_Cleaned_Cancel_Done:
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
