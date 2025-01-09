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

import org.crazydan.studio.app.ime.kuaizi.core.InputList;
import org.crazydan.studio.app.ime.kuaizi.core.Key;
import org.crazydan.studio.app.ime.kuaizi.core.KeyboardContext;
import org.crazydan.studio.app.ime.kuaizi.core.key.CharKey;
import org.crazydan.studio.app.ime.kuaizi.core.key.CtrlKey;
import org.crazydan.studio.app.ime.kuaizi.core.msg.InputMsg;
import org.crazydan.studio.app.ime.kuaizi.core.msg.UserKeyMsg;
import org.crazydan.studio.app.ime.kuaizi.core.msg.UserKeyMsgType;
import org.crazydan.studio.app.ime.kuaizi.dict.PinyinDict;

/**
 * 按键直接输入目标组件的键盘，
 * 即，点击按键时即可在目标组件输入按键对应的字符，
 * 不会在输入列表中停留和做预处理
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-07-30
 */
public abstract class DirectInputKeyboard extends BaseKeyboard {

    protected DirectInputKeyboard(PinyinDict dict) {
        super(dict);
    }

    @Override
    public void onMsg(KeyboardContext context, InputMsg msg) {
        InputList inputList = context.inputList;

        // Note: 在输入列表为空时，直输键盘无预处理过程，故不对输入列表事件做响应
        if (inputList.isEmpty()) {
            return;
        }

        super.onMsg(context, msg);
    }

    @Override
    public void onMsg(KeyboardContext context, UserKeyMsg msg) {
        if (try_On_Common_UserKey_Msg(context, msg)) {
            return;
        }

        Key key = context.key();
        // Note: 被禁用的按键也可能需要处理
        if (key instanceof CharKey) {
            on_CharKey_Msg(context, msg);
        } else if (key instanceof CtrlKey) {
            on_CtrlKey_Msg(context, msg);
        }
    }

    protected void on_CharKey_Msg(KeyboardContext context, UserKeyMsg msg) {
        // 单字符直接输入
        if (msg.type != UserKeyMsgType.SingleTap_Key) {
            return;
        }

        InputList inputList = context.inputList;
        boolean directInputting = inputList.isEmpty();

        start_Single_CharKey_Inputting(context, msg.data(), directInputting);
    }

    protected void on_CtrlKey_Msg(KeyboardContext context, UserKeyMsg msg) {
    }
}
