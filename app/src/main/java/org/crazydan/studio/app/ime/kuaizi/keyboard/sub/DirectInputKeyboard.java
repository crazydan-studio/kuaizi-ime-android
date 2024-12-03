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

package org.crazydan.studio.app.ime.kuaizi.keyboard.sub;

import org.crazydan.studio.app.ime.kuaizi.keyboard.InputList;
import org.crazydan.studio.app.ime.kuaizi.keyboard.Key;
import org.crazydan.studio.app.ime.kuaizi.keyboard.Keyboard;
import org.crazydan.studio.app.ime.kuaizi.keyboard.key.CharKey;
import org.crazydan.studio.app.ime.kuaizi.keyboard.key.CtrlKey;
import org.crazydan.studio.app.ime.kuaizi.keyboard.msg.InputMsgListener;
import org.crazydan.studio.app.ime.kuaizi.keyboard.msg.UserInputMsg;
import org.crazydan.studio.app.ime.kuaizi.keyboard.msg.UserInputMsgData;
import org.crazydan.studio.app.ime.kuaizi.keyboard.msg.UserKeyMsg;
import org.crazydan.studio.app.ime.kuaizi.keyboard.msg.UserKeyMsgData;
import org.crazydan.studio.app.ime.kuaizi.keyboard.msg.user.UserSingleTapMsgData;

/**
 * 按键直接输入目标组件的键盘，
 * 即，点击按键时即可在目标组件输入按键对应的字符，
 * 不会在输入列表中停留和做预处理
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-07-30
 */
public abstract class DirectInputKeyboard extends BaseKeyboard {

    public DirectInputKeyboard(InputMsgListener listener, Keyboard.Subtype prevType) {super(listener, prevType);}

    @Override
    public void onMsg(InputList inputList, UserInputMsg msg, UserInputMsgData msgData) {
        // Note: 在输入列表为空且消息为非输入列表清空消息时，直输键盘无预处理过程，故不对输入列表事件做响应
        if (!inputList.isEmpty() || msg == UserInputMsg.Inputs_Clean_Done) {
            super.onMsg(inputList, msg, msgData);
        }
    }

    @Override
    public void onUserKeyMsg(UserKeyMsg msg, UserKeyMsgData data) {
        if (try_OnUserKeyMsg(msg, data)) {
            return;
        }

        Key<?> key = data.target;
        if (key instanceof CharKey) {
            onCharKeyMsg(msg, (CharKey) key, data);
        } else if (key instanceof CtrlKey) {
            onCtrlKeyMsg(msg, (CtrlKey) key, data);
        }
    }

    protected void onCharKeyMsg(UserKeyMsg msg, CharKey key, UserKeyMsgData data) {
        InputList inputList = getInputList();

        // 单字符直接输入
        if (msg == UserKeyMsg.KeySingleTap) {
            play_SingleTick_InputAudio(key);
            show_InputChars_Input_Popup(key);

            boolean isDirectInputting = inputList.isEmpty();
            start_Single_Key_Inputting(inputList, key, (UserSingleTapMsgData) data, isDirectInputting);
        }
    }

    protected void onCtrlKeyMsg(UserKeyMsg msg, CtrlKey key, UserKeyMsgData data) {
    }
}
