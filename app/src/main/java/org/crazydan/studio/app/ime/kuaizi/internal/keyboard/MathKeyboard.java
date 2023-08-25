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
import org.crazydan.studio.app.ime.kuaizi.internal.key.CharKey;
import org.crazydan.studio.app.ime.kuaizi.internal.key.CtrlKey;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.UserInputMsg;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.UserInputMsgData;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.UserKeyMsg;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.UserKeyMsgData;

/**
 * {@link Keyboard.Type#Math 数学键盘}
 * <p/>
 * 含数字、计算符号等
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-07-04
 */
public class MathKeyboard extends BaseKeyboard {
    private static final Key<?>[] math_keys = new Key[] {
            CtrlKey.create(CtrlKey.Type.Math_Plus).setLabel("+"),
            CtrlKey.create(CtrlKey.Type.Math_Minus).setLabel("-"),
            CtrlKey.create(CtrlKey.Type.Math_Multiply).setLabel("×"),
            CtrlKey.create(CtrlKey.Type.Math_Divide).setLabel("÷"),
            CtrlKey.create(CtrlKey.Type.Math_Percent).setLabel("%"),
            CtrlKey.create(CtrlKey.Type.Math_Dot).setLabel("."),
            CtrlKey.create(CtrlKey.Type.Math_Brackets).setLabel("( )"),
            CharKey.create(CharKey.Type.Number, "0").setLabel("0"),
            CharKey.create(CharKey.Type.Number, "1").setLabel("1"),
            CharKey.create(CharKey.Type.Number, "2").setLabel("2"),
            CharKey.create(CharKey.Type.Number, "3").setLabel("3"),
            CharKey.create(CharKey.Type.Number, "4").setLabel("4"),
            CharKey.create(CharKey.Type.Number, "5").setLabel("5"),
            CharKey.create(CharKey.Type.Number, "6").setLabel("6"),
            CharKey.create(CharKey.Type.Number, "7").setLabel("7"),
            CharKey.create(CharKey.Type.Number, "8").setLabel("8"),
            CharKey.create(CharKey.Type.Number, "9").setLabel("9"),
            };

    @Override
    protected KeyFactory doGetKeyFactory() {
        return () -> KeyTable.createMathKeys(createKeyTableConfigure(), math_keys);
    }

    @Override
    public void onUserInputMsg(UserInputMsg msg, UserInputMsgData data) {

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

    private void onCharKeyMsg(UserKeyMsg msg, CharKey key, UserKeyMsgData data) {
        switch (msg) {
            case KeyDoubleTap: // 双击继续触发第二次单击操作
            case KeySingleTap: {
                // 单字符输入
                play_InputtingSingleTick_Audio(key);

                append_Key_for_Continuous_InputChars_Inputting(key);
                break;
            }
        }
    }

    private void onCtrlKeyMsg(UserKeyMsg msg, CtrlKey key, UserKeyMsgData data) {
        switch (msg) {
            case KeyDoubleTap: // 双击继续触发第二次单击操作
            case KeySingleTap: {
                play_InputtingSingleTick_Audio(key);

                switch (key.getType()) {
                    case Exit: {
                        switchTo_Previous_Keyboard();
                        break;
                    }
                    case Math_Plus:
                    case Math_Minus:
                    case Math_Multiply:
                    case Math_Divide:
                    case Math_Equal:
                    case Math_Dot:
                    case Math_Percent: {
                        append_Key_for_Continuous_InputChars_Inputting(key);
                        break;
                    }
                }
                break;
            }
        }
    }
}
