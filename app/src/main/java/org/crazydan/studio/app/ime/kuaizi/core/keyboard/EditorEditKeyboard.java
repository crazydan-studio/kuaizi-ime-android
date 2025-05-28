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

import org.crazydan.studio.app.ime.kuaizi.common.Motion;
import org.crazydan.studio.app.ime.kuaizi.common.Point;
import org.crazydan.studio.app.ime.kuaizi.core.KeyFactory;
import org.crazydan.studio.app.ime.kuaizi.core.KeyboardContext;
import org.crazydan.studio.app.ime.kuaizi.core.key.CtrlKey;
import org.crazydan.studio.app.ime.kuaizi.core.keyboard.keytable.EditorKeyTable;
import org.crazydan.studio.app.ime.kuaizi.core.keyboard.state.EditorEditStateData;
import org.crazydan.studio.app.ime.kuaizi.core.msg.UserKeyMsg;

/**
 * 编辑器 编辑键盘
 * <p/>
 * 内置对目标编辑器的光标移动和选区处理逻辑
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2025-02-01
 */
public abstract class EditorEditKeyboard extends BaseKeyboard {

    @Override
    public KeyFactory buildKeyFactory(KeyboardContext context) {
        if (this.state.type != State.Type.Editor_Edit_Doing) {
            return doBuildKeyFactory(context);
        }

        KeyTableConfig keyTableConfig = createKeyTableConfig(context);
        EditorKeyTable keyTable = EditorKeyTable.create(keyTableConfig);

        EditorEditStateData stateData = this.state.data();

        return (KeyFactory.NoAnimation) () -> {
            CtrlKey.Type type = CtrlKey.Type.Editor_Cursor_Locator;

            if (stateData.target == EditorEditStateData.Target.selection) {
                type = CtrlKey.Type.Editor_Range_Selector;
            }
            return keyTable.createGrid(type);
        };
    }

    protected KeyFactory doBuildKeyFactory(KeyboardContext context) {
        return null;
    }

    @Override
    protected boolean try_On_Common_UserKey_Msg(KeyboardContext context, UserKeyMsg msg) {
        if (this.state.type == State.Type.Editor_Edit_Doing) {
            on_Editor_Editing_Msg(context, msg);
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
            case LongPress_Key_Tick: {
                // 长按定位按键进入内容选择模式
                do_Start_Editor_Editing(context, EditorEditStateData.Target.selection, msg.data().at);
                return true;
            }
            case FingerMoving_Start: {
                start_Editor_Editing(context, msg.data().at);
                return true;
            }
        }

        return false;
    }

    // ======================== Start: 编辑器编辑逻辑 ========================

    /** 仅处理光标移动 */
    protected void start_Editor_Editing(KeyboardContext context, Point from) {
        CtrlKey key = context.key();

        EditorEditStateData.Target target;
        switch (key.type) {
            case Editor_Cursor_Locator:
                target = EditorEditStateData.Target.cursor;
                break;
            case Editor_Range_Selector:
                target = EditorEditStateData.Target.selection;
                break;
            default: {
                // 忽略非光标移动
                return;
            }
        }

        do_Start_Editor_Editing(context, target, from);
    }

    protected void do_Start_Editor_Editing(KeyboardContext context, EditorEditStateData.Target target, Point from) {
        this.log.debug("Start editor editing: key=%s, at=%s", () -> new Object[] { context.key, from });

        EditorEditStateData stateData = new EditorEditStateData(target, from);
        State state = new State(State.Type.Editor_Edit_Doing, stateData, this.state);

        change_State_To(context, state);
    }

    protected void stop_Editor_Editing(KeyboardContext context) {
        this.log.debug("Stop editor editing: key=%s", () -> new Object[] { context.key });

        change_State_to_Previous(context);
    }

    protected void on_Editor_Editing_Msg(KeyboardContext context, UserKeyMsg msg) {
        switch (msg.type) {
            case Press_Key_Start:
            case LongPress_Key_Stop:
            case FingerMoving_Stop: {
                stop_Editor_Editing(context);
                return;
            }
            case FingerMoving: {
                do_Editor_Cursor_Handling(context, msg);
            }
        }
    }

    protected void do_Editor_Cursor_Handling(KeyboardContext context, UserKeyMsg msg) {
        EditorEditStateData stateData = this.state.data();
        stateData.moveTo(msg.data().at);

        Motion motion = stateData.getMotion();
        // 根据单位移动距离计算得出光标的移动次数
        Motion anchor = new Motion(motion, motion.distance >= context.movingThresholdInPx ? 1 : 0);
        if (anchor.distance < 1) {
            return;
        }
        this.log.debug("Moving editor cursor: key=%s, anchor=%s", () -> new Object[] { context.key, anchor });

        // Note: 始终播放点击音效
        play_SingleTick_InputAudio(context, true);

        // 重新定位
        stateData.startAt(msg.data().at);

        switch (stateData.target) {
            case cursor: {
                do_Editor_Cursor_Moving(context, anchor);
                break;
            }
            case selection: {
                do_Editor_Range_Selecting(context, anchor);
                break;
            }
        }
    }

    // ======================== End: 编辑器编辑逻辑 ========================
}
