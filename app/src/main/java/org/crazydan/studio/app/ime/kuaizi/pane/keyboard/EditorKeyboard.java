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

package org.crazydan.studio.app.ime.kuaizi.pane.keyboard;

import org.crazydan.studio.app.ime.kuaizi.pane.Key;
import org.crazydan.studio.app.ime.kuaizi.pane.KeyFactory;
import org.crazydan.studio.app.ime.kuaizi.pane.KeyboardContext;
import org.crazydan.studio.app.ime.kuaizi.pane.key.CtrlKey;
import org.crazydan.studio.app.ime.kuaizi.pane.keyboard.keytable.EditorKeyTable;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.InputMsgData;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.InputMsgType;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.Motion;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.UserKeyMsg;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.input.EditorCursorMsgData;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.user.UserFingerFlippingMsgData;

/**
 * {@link Type#Editor 文本编辑键盘}
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2024-12-10
 */
public class EditorKeyboard extends DirectInputKeyboard {

    public EditorKeyboard() {
        super(null);
    }

    @Override
    public Type getType() {return Type.Editor;}

    @Override
    public KeyFactory getKeyFactory(KeyboardContext context) {
        KeyTableConfig keyTableConfig = createKeyTableConfig(context);
        EditorKeyTable keyTable = EditorKeyTable.create(keyTableConfig);

        return keyTable::createKeys;
    }

    @Override
    protected boolean try_On_Common_CtrlKey_Msg(KeyboardContext context, UserKeyMsg msg) {
        CtrlKey key = context.key();

        // 在当前键盘内单独处理光标移动按键
        if (CtrlKey.is(key, CtrlKey.Type.Editor_Cursor_Locator)) {
            return false;
        }
        return super.try_On_Common_CtrlKey_Msg(context, msg);
    }

    @Override
    protected void on_CtrlKey_Msg(KeyboardContext context, UserKeyMsg msg) {
        CtrlKey key = context.key();

        switch (msg.type) {
            case SingleTap_Key: {
                if (CtrlKey.is(key, CtrlKey.Type.Edit_Editor)) {
                    play_SingleTick_InputAudio(context);

                    CtrlKey.EditorEditOption option = (CtrlKey.EditorEditOption) key.getOption();
                    do_Editor_Editing(context, option.value());
                }
                break;
            }
            case FingerFlipping: {
                UserFingerFlippingMsgData data = msg.data();
                Motion motion = data.motion;
                switch (key.getType()) {
                    case Editor_Cursor_Locator:
                        play_SingleTick_InputAudio(context);

                        do_Editor_Cursor_Moving(context, motion);
                        break;
                    case Editor_Range_Selector:
                        play_SingleTick_InputAudio(context);

                        do_Editor_Range_Selecting(context, motion);
                        break;
                }
                break;
            }
        }
    }

    @Override
    protected void switch_Keyboard_to_Previous(KeyboardContext context) {
        // Note: 编辑键盘均直接回到其切换前的键盘，而不管切换前的是否为主键盘
        switch_Keyboard_To(context, context.config.prevType);
    }

    private void do_Editor_Range_Selecting(KeyboardContext context, Motion motion) {
        Key key = context.key();
        InputMsgData data = new EditorCursorMsgData(key, motion);

        fire_InputMsg(context, InputMsgType.Editor_Range_Select_Doing, data);
    }
}
