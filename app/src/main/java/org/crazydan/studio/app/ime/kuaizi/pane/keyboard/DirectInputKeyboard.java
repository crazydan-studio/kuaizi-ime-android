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
import org.crazydan.studio.app.ime.kuaizi.pane.key.CharKey;
import org.crazydan.studio.app.ime.kuaizi.pane.key.CtrlKey;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.InputMsg;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.UserKeyMsg;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.UserKeyMsgType;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.user.UserSingleTapMsgData;

/**
 * 按键直接输入目标组件的键盘，
 * 即，点击按键时即可在目标组件输入按键对应的字符，
 * 不会在输入列表中停留和做预处理
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-07-30
 */
public abstract class DirectInputKeyboard extends BaseKeyboard {

    @Override
    public void onMsg(InputList inputList, InputMsg msg) {
        // Note: 在输入列表为空时，直输键盘无预处理过程，故不对输入列表事件做响应
        if (!inputList.isEmpty()) {
            super.onMsg(inputList, msg);
        }
    }

    @Override
    public void onMsg(InputList inputList, UserKeyMsg msg) {
        if (try_On_Common_UserKey_Msg(inputList, msg)) {
            return;
        }

        Key<?> key = msg.data.key;
        if (key instanceof CharKey) {
            on_CharKey_Msg(inputList, msg, (CharKey) key);
        } else if (key instanceof CtrlKey && !key.isDisabled()) {
            on_CtrlKey_Msg(inputList, msg, (CtrlKey) key);
        }
    }

    protected void on_CharKey_Msg(InputList inputList, UserKeyMsg msg, CharKey key) {
        // 单字符直接输入
        if (msg.type != UserKeyMsgType.SingleTap_Key) {
            return;
        }

        boolean directInputting = inputList.isEmpty();
        start_Single_Key_Inputting(inputList, key, (UserSingleTapMsgData) msg.data, directInputting);
    }

    protected void on_CtrlKey_Msg(InputList inputList, UserKeyMsg msg, CtrlKey key) {
    }
}
