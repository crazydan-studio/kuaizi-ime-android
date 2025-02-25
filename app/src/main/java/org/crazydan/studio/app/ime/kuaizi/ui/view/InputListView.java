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
        this.log.beginTreeLog("Handle %s", () -> new Object[] { msg.getClass() }) //
                .debug("Message Type: %s", () -> new Object[] { msg.type }) //
                .debug("Message Data: %s", () -> new Object[] { msg.data() });

        switch (msg.type) {
            case Input_Choose_Done: {
                super.onMsg(msg);

                this.log.endTreeLog();
                return;
            }
            case Config_Update_Done: {
                ConfigUpdateMsgData data = msg.data();
                // Note: 仅关注与输入列表布局和显示相关的配置更新
                ConfigKey[] effects = new ConfigKey[] {
                        ConfigKey.theme, ConfigKey.enable_candidate_variant_first
                };
                if (!CollectionUtils.contains(effects, data.configKey)) {
                    this.log.warn("Ignore configuration %s", () -> new Object[] { data.configKey }) //
                            .endTreeLog();
                    return;
                }
                break;
            }
            // Note: 不影响输入列表视图的消息，直接返回
            case IME_Switch_Doing:
            case InputAudio_Play_Doing:
                //
            case Keyboard_Switch_Doing:
            case Keyboard_Start_Doing:
            case Keyboard_Hide_Done:
            case Keyboard_Exit_Done:
            case Keyboard_HandMode_Switch_Doing:
            case Keyboard_HandMode_Switch_Done:
            case Keyboard_XPad_Simulation_Terminated:
                //
            case Editor_Edit_Doing:
            case Editor_Cursor_Move_Doing:
            case Editor_Range_Select_Doing:
                //
            case InputCompletion_Create_Done:
                //
            case Input_Choose_Doing:
            case InputChars_Input_Popup_Show_Doing:
            case InputChars_Input_Popup_Hide_Doing: {
                this.log.warn("Do not handle message %s", () -> new Object[] { msg.type }) //
                        .endTreeLog();
                return;
            }
        }

        this.log.debug("Update view for message %s", () -> new Object[] { msg.type });

        update(msg.inputFactory);

        this.log.endTreeLog();
    }

    // =============================== End: 消息处理 ===================================
}
