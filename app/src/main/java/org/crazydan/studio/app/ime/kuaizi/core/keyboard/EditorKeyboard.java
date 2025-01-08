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

package org.crazydan.studio.app.ime.kuaizi.core.keyboard;

import org.crazydan.studio.app.ime.kuaizi.common.widget.EditorAction;
import org.crazydan.studio.app.ime.kuaizi.core.Key;
import org.crazydan.studio.app.ime.kuaizi.core.KeyFactory;
import org.crazydan.studio.app.ime.kuaizi.core.KeyboardContext;
import org.crazydan.studio.app.ime.kuaizi.core.key.CtrlKey;
import org.crazydan.studio.app.ime.kuaizi.core.keyboard.keytable.EditorKeyTable;
import org.crazydan.studio.app.ime.kuaizi.core.msg.InputMsgData;
import org.crazydan.studio.app.ime.kuaizi.core.msg.Motion;
import org.crazydan.studio.app.ime.kuaizi.core.msg.UserKeyMsg;
import org.crazydan.studio.app.ime.kuaizi.core.msg.UserKeyMsgType;
import org.crazydan.studio.app.ime.kuaizi.core.msg.input.EditorCursorMsgData;
import org.crazydan.studio.app.ime.kuaizi.core.msg.user.UserFingerFlippingMsgData;

import static org.crazydan.studio.app.ime.kuaizi.core.msg.InputMsgType.Editor_Range_Select_Doing;

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
    public KeyFactory buildKeyFactory(KeyboardContext context) {
        KeyTableConfig keyTableConfig = createKeyTableConfig(context);
        EditorKeyTable keyTable = EditorKeyTable.create(keyTableConfig);

        return keyTable::createKeys;
    }

    @Override
    protected void switch_Keyboard_to_Previous(KeyboardContext context) {
        // Note: 直接回到其切换前的键盘，而不管切换前的是否为主键盘
        switch_Keyboard_To(context, context.keyboardPrevType);
    }

    @Override
    protected boolean disable_Msg_On_CtrlKey_Commit_InputList(UserKeyMsg msg) {
        // Note: 在当前键盘内仅处理 Commit_InputList 按键的单击消息，
        // 忽略其余消息，从而避免切换到输入提交选项键盘
        return msg.type != UserKeyMsgType.SingleTap_Key;
    }

    @Override
    protected boolean try_On_Common_CtrlKey_Msg(KeyboardContext context, UserKeyMsg msg) {
        CtrlKey key = context.key();

        // 在当前键盘内需单独处理光标移动按键，不采用公共处理逻辑
        if (CtrlKey.Type.Editor_Cursor_Locator.match(key)) {
            return false;
        }

        return super.try_On_Common_CtrlKey_Msg(context, msg);
    }

    @Override
    protected void on_CtrlKey_Msg(KeyboardContext context, UserKeyMsg msg) {
        CtrlKey key = context.key();
        if (key.disabled) {
            return;
        }

        switch (msg.type) {
            case SingleTap_Key: {
                if (CtrlKey.Type.Edit_Editor.match(key)) {
                    play_SingleTick_InputAudio(context);

                    CtrlKey.Option<EditorAction> option = key.option();
                    do_Editor_Editing(context, option.value);
                }
                break;
            }
            case FingerFlipping: {
                UserFingerFlippingMsgData data = msg.data();
                Motion motion = data.motion;
                switch (key.type) {
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

    private void do_Editor_Range_Selecting(KeyboardContext context, Motion motion) {
        Key key = context.key();
        InputMsgData data = new EditorCursorMsgData(key, motion);

        fire_InputMsg(context, Editor_Range_Select_Doing, data);
    }
}
