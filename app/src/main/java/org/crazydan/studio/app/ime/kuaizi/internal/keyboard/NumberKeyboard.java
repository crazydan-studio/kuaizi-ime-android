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

package org.crazydan.studio.app.ime.kuaizi.internal.keyboard;

import org.crazydan.studio.app.ime.kuaizi.internal.Key;
import org.crazydan.studio.app.ime.kuaizi.internal.Keyboard;
import org.crazydan.studio.app.ime.kuaizi.internal.key.CtrlKey;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.UserKeyMsg;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.UserKeyMsgData;

/**
 * {@link Keyboard.Type#Number 纯数字键盘}
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-07-28
 */
public class NumberKeyboard extends DirectInputKeyboard {
    private static final Key<?>[] number_keys = new Key[] {
            //
            KeyTable.symbolKey("+"),
            KeyTable.symbolKey("-"),
            KeyTable.symbolKey("*"),
            KeyTable.symbolKey("#"),
            KeyTable.symbolKey(","),
            KeyTable.symbolKey(";"),
            KeyTable.symbolKey("."),
            KeyTable.symbolKey(":"),
            KeyTable.symbolKey("/"),
            KeyTable.symbolKey("%"),
            //
            KeyTable.numberKey("0"),
            KeyTable.numberKey("1"),
            KeyTable.numberKey("2"),
            KeyTable.numberKey("3"),
            KeyTable.numberKey("4"),
            KeyTable.numberKey("5"),
            KeyTable.numberKey("6"),
            KeyTable.numberKey("7"),
            KeyTable.numberKey("8"),
            KeyTable.numberKey("9"),
            };

    @Override
    public KeyFactory getKeyFactory() {
        return () -> KeyTable.createNumberKeys(createKeyTableConfigure(), number_keys);
    }

    @Override
    protected void onCtrlKeyMsg(UserKeyMsg msg, CtrlKey key, UserKeyMsgData data) {
        switch (msg) {
            case KeyDoubleTap: // 双击继续触发第二次单击操作
            case KeySingleTap: {
                switch (key.getType()) {
                    case Exit: {
                        // 单字符直接输入
                        play_InputtingSingleTick_Audio(key);

                        switch_Keyboard(Type.Pinyin);
                        break;
                    }
                }
                break;
            }
        }
    }
}
