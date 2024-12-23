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

import org.crazydan.studio.app.ime.kuaizi.pane.InputList;
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

    @Override
    public Type getType() {
        return Type.Editor;
    }

    @Override
    public KeyFactory getKeyFactory(InputList inputList) {
        EditorKeyTable keyTable = EditorKeyTable.create(createKeyTableConfig(inputList));

        return keyTable::createKeys;
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
            case FingerFlipping:
                Motion motion = ((UserFingerFlippingMsgData) msg.data).motion;
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

    private void do_Editor_Range_Selecting(KeyboardContext context, Motion motion) {
        Key<?> key = context.key();
        InputMsgData data = new EditorCursorMsgData(key, motion);

        fire_InputMsg(context, InputMsgType.Editor_Range_Select_Doing, data);
    }
}
