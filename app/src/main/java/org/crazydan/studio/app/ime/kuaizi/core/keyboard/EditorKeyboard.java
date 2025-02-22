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

import org.crazydan.studio.app.ime.kuaizi.common.widget.EditorAction;
import org.crazydan.studio.app.ime.kuaizi.core.Key;
import org.crazydan.studio.app.ime.kuaizi.core.KeyFactory;
import org.crazydan.studio.app.ime.kuaizi.core.KeyboardContext;
import org.crazydan.studio.app.ime.kuaizi.core.key.CtrlKey;
import org.crazydan.studio.app.ime.kuaizi.core.keyboard.keytable.EditorKeyTable;
import org.crazydan.studio.app.ime.kuaizi.core.msg.UserKeyMsg;
import org.crazydan.studio.app.ime.kuaizi.core.msg.UserKeyMsgType;

/**
 * {@link Type#Editor 文本编辑键盘}
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2024-12-10
 */
public class EditorKeyboard extends EditorEditKeyboard {

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
    protected boolean try_On_CtrlKey_Editor_Cursor_Locator_Msg(KeyboardContext context, UserKeyMsg msg) {
        // 屏蔽基类中对光标定位按键的处理逻辑，由当前键盘自行处理
        return false;
    }

    @Override
    public void onMsg(KeyboardContext context, UserKeyMsg msg) {
        if (try_On_Common_UserKey_Msg(context, msg)) {
            return;
        }

        Key key = context.key();
        if (key instanceof CtrlKey && !key.disabled) {
            on_CtrlKey_Msg(context, msg);
        }
    }

    protected void on_CtrlKey_Msg(KeyboardContext context, UserKeyMsg msg) {
        CtrlKey key = context.key();

        switch (msg.type) {
            case SingleTap_Key: {
                if (CtrlKey.Type.Edit_Editor.match(key)) {
                    play_SingleTick_InputAudio(context);

                    CtrlKey.Option<EditorAction> option = key.option();
                    do_Editor_Editing(context, option.value);
                }
                break;
            }
            case FingerMoving_Start: {
                start_Editor_Editing(context, msg.data().at);
                break;
            }
        }
    }
}
