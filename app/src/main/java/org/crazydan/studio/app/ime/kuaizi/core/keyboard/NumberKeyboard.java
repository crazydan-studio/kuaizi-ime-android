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

import org.crazydan.studio.app.ime.kuaizi.core.KeyFactory;
import org.crazydan.studio.app.ime.kuaizi.core.KeyboardContext;
import org.crazydan.studio.app.ime.kuaizi.core.keyboard.keytable.NumberKeyTable;
import org.crazydan.studio.app.ime.kuaizi.core.msg.UserKeyMsg;
import org.crazydan.studio.app.ime.kuaizi.core.msg.UserKeyMsgType;

/**
 * {@link Type#Number 纯数字键盘}
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-07-28
 */
public class NumberKeyboard extends DirectInputKeyboard {
    private boolean needToShowExit;

    public NumberKeyboard() {
        super(null);
    }

    @Override
    public Type getType() {return Type.Number;}

    @Override
    public boolean isMaster() {return true;}

    @Override
    public void start(KeyboardContext context) {
        // 若是在 X 型输入中切换过来的，则需要在禁用 X 型输入后，提供退出按钮以回到原键盘
        this.needToShowExit = context.xInputPadEnabled;

        super.start(context);
    }

    @Override
    public KeyFactory buildKeyFactory(KeyboardContext context) {
        KeyTableConfig keyTableConf = createKeyTableConfig(context);
        NumberKeyTable keyTable = NumberKeyTable.create(keyTableConf);

        return () -> keyTable.createKeys(this.needToShowExit);
    }

    @Override
    protected void exit_Keyboard(KeyboardContext context) {
        this.needToShowExit = false;

        super.exit_Keyboard(context);
    }

    @Override
    protected boolean disable_Msg_On_CtrlKey_Commit_InputList(UserKeyMsg msg) {
        // Note: 在当前键盘内仅处理 Commit_InputList 按键的单击消息，
        // 忽略其余消息，从而避免切换到输入提交选项键盘
        return msg.type != UserKeyMsgType.SingleTap_Key;
    }
}
