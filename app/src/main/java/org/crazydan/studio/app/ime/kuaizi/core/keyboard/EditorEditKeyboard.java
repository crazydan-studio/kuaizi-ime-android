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

package org.crazydan.studio.app.ime.kuaizi.core.keyboard;

import org.crazydan.studio.app.ime.kuaizi.common.utils.ScreenUtils;
import org.crazydan.studio.app.ime.kuaizi.core.KeyboardContext;
import org.crazydan.studio.app.ime.kuaizi.core.key.CtrlKey;
import org.crazydan.studio.app.ime.kuaizi.core.keyboard.state.EditorEditStateData;
import org.crazydan.studio.app.ime.kuaizi.core.msg.Motion;
import org.crazydan.studio.app.ime.kuaizi.core.msg.UserKeyMsg;
import org.crazydan.studio.app.ime.kuaizi.core.msg.user.UserFingerMovingMsgData;
import org.crazydan.studio.app.ime.kuaizi.dict.PinyinDict;

/**
 * 编辑器 编辑键盘
 * <p/>
 * 内置对目标编辑器的光标移动和选区处理逻辑
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2025-02-01
 */
public abstract class EditorEditKeyboard extends BaseKeyboard {
    private static final float MIN_MOTION_DISTANCE_IN_PX = ScreenUtils.dpToPx(10f);

    protected EditorEditKeyboard(PinyinDict dict) {
        super(dict);
    }

    @Override
    protected boolean try_On_Common_UserKey_Msg(KeyboardContext context, UserKeyMsg msg) {
        if (this.state.type == State.Type.Editor_Edit_Doing) {
            do_Editor_Editing(context, msg);
            return true;
        }

        return super.try_On_Common_UserKey_Msg(context, msg);
    }

    @Override
    protected boolean try_On_Common_CtrlKey_Msg(KeyboardContext context, UserKeyMsg msg) {
        CtrlKey key = context.key();
        // 仅处理定位按钮
        if (CtrlKey.Type.Editor_Cursor_Locator.match(key)) {
            return try_On_CtrlKey_Editor_Cursor_Locator_Msg(context, msg);
        }

        return super.try_On_Common_CtrlKey_Msg(context, msg);
    }

    protected boolean try_On_CtrlKey_Editor_Cursor_Locator_Msg(KeyboardContext context, UserKeyMsg msg) {
        switch (msg.type) {
            case SingleTap_Key: {
                // 为双击提前播放音效
                play_SingleTick_InputAudio(context);
                return true;
            }
            case DoubleTap_Key: {
                switch_Keyboard_To(context, Type.Editor);
                return true;
            }
            case FingerMoving_Start: {
                start_Editor_Cursor_Moving(context);
                return true;
            }
        }

        return false;
    }

    // ======================== Start: 编辑器编辑逻辑 ========================

    protected void start_Editor_Cursor_Moving(KeyboardContext context) {
        start_Editor_Editing(context, EditorEditStateData.Target.cursor);
    }

    protected void start_Editor_Range_Selecting(KeyboardContext context) {
        start_Editor_Editing(context, EditorEditStateData.Target.selection);
    }

    protected void do_Editor_Editing(KeyboardContext context, UserKeyMsg msg) {
        switch (msg.type) {
            case Press_Key_Start:
            case FingerMoving_Stop: {
                stop_Editor_Editing(context);
                return;
            }
            case FingerMoving: {
                UserFingerMovingMsgData data = msg.data();
                if (data.motion.distance < MIN_MOTION_DISTANCE_IN_PX) {
                    return;
                }
                this.log.warn("Moving motion: %s", () -> new Object[] { data.motion });

                //play_SingleTick_InputAudio(context);

                EditorEditStateData stateData = this.state.data();
                Motion motion = new Motion(data.motion, 1);

                switch (stateData.target) {
                    case cursor: {
                        do_Editor_Cursor_Moving(context, motion);
                        break;
                    }
                    case selection: {
                        do_Editor_Range_Selecting(context, motion);
                        break;
                    }
                }
            }
        }
    }

    protected void start_Editor_Editing(KeyboardContext context, EditorEditStateData.Target target) {
        EditorEditStateData stateData = new EditorEditStateData(target);
        State state = new State(State.Type.Editor_Edit_Doing, stateData, this.state);

        change_State_To(context, state);
    }

    protected void stop_Editor_Editing(KeyboardContext context) {
        change_State_To(context, this.state.previous);
    }

    // ======================== End: 编辑器编辑逻辑 ========================
}
