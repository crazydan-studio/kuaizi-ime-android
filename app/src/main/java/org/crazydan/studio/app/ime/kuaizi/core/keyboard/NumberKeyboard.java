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

import org.crazydan.studio.app.ime.kuaizi.core.Key;
import org.crazydan.studio.app.ime.kuaizi.core.Keyboard;
import org.crazydan.studio.app.ime.kuaizi.core.keyboard.keytable.NumberKeyTable;
import org.crazydan.studio.app.ime.kuaizi.core.msg.InputMsgListener;

/**
 * {@link Keyboard.Type#Number 纯数字键盘}
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-07-28
 */
public class NumberKeyboard extends DirectInputKeyboard {
    private boolean needToShowExit;

    public NumberKeyboard(InputMsgListener listener, Type prevType) {super(listener, prevType);}

    @Override
    public Type getType() {
        return Type.Number;
    }

    @Override
    public void start() {
        // 若是在 X 型输入中切换过来的，则需要在禁用 X 型输入后，提供退出按钮以回到前一键盘
        this.needToShowExit = isXInputPadEnabled();

        super.start();
    }

    @Override
    protected KeyFactory doGetKeyFactory() {
        NumberKeyTable keyTable = NumberKeyTable.create(createKeyTableConfig());

        return () -> keyTable.createKeys(this.needToShowExit);
    }

    @Override
    protected void switchTo_Previous_Keyboard(Key<?> key) {
        this.needToShowExit = false;

        super.switchTo_Previous_Keyboard(key);
    }
}
