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

import org.crazydan.studio.app.ime.kuaizi.internal.Input;
import org.crazydan.studio.app.ime.kuaizi.internal.Key;
import org.crazydan.studio.app.ime.kuaizi.internal.Keyboard;
import org.crazydan.studio.app.ime.kuaizi.internal.input.CharInput;
import org.crazydan.studio.app.ime.kuaizi.internal.key.CharKey;
import org.crazydan.studio.app.ime.kuaizi.internal.key.CtrlKey;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.UserInputMsg;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.UserInputMsgData;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.UserKeyMsg;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.UserKeyMsgData;

/**
 * 标点符号{@link Keyboard 键盘}
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-07-27
 */
public class SymbolKeyboard extends BaseKeyboard {
    /** 英文标点符号: https://zh.wikipedia.org/wiki/%E6%A0%87%E7%82%B9%E7%AC%A6%E5%8F%B7 */
    private static final Symbol[] latin_symbols = new Symbol[] {
            Symbol.single(",").withReplacements("，"),
            Symbol.single(".").withReplacements("。"),
            Symbol.single("?").withReplacements("？"),
            Symbol.single("!").withReplacements("！"),
            Symbol.single(":").withReplacements("："),
            Symbol.single(";").withReplacements("；"),
            Symbol.single("@"),
            Symbol.single("`"),
            Symbol.doubled("''").withReplacements("‘’"),
            Symbol.doubled("\"\"").withReplacements("“”"),
            Symbol.doubled("()").withReplacements("（）"),
            Symbol.doubled("[]").withReplacements("［］"),
            Symbol.doubled("{}"),
            Symbol.doubled("<>").withReplacements("〈〉"),
            Symbol.single("/"),
            Symbol.single("\\"),
            Symbol.single("|"),
            Symbol.single("~"),
            Symbol.single("#"),
            Symbol.single("$"),
            Symbol.single("%"),
            Symbol.single("^"),
            Symbol.single("&"),
            Symbol.single("*"),
            Symbol.single("-"),
            Symbol.single("+"),
            Symbol.single("="),
            Symbol.single("–"),
            Symbol.single("—"),
            };
    /** 中文标点符号: https://zh.wikipedia.org/wiki/%E6%A0%87%E7%82%B9%E7%AC%A6%E5%8F%B7 */
    private static final Symbol[] chinese_symbols = new Symbol[] {
            Symbol.single("，").withReplacements(","),
            Symbol.single("。").withReplacements("."),
            Symbol.single("？").withReplacements("?"),
            Symbol.single("！").withReplacements("!"),
            Symbol.single("：").withReplacements(":"),
            Symbol.single("；").withReplacements(";"),
            Symbol.single("、"),
            Symbol.doubled("‘’").withReplacements("''"),
            Symbol.doubled("“”").withReplacements("\"\""),
            Symbol.doubled("「」"),
            Symbol.doubled("『』"),
            Symbol.doubled("（）").withReplacements("()"),
            Symbol.doubled("〔〕"),
            Symbol.doubled("〈〉").withReplacements("<>"),
            Symbol.doubled("《》"),
            Symbol.doubled("［］").withReplacements("[]"),
            Symbol.doubled("【】"),
            Symbol.single("-"),
            Symbol.single("－"),
            Symbol.single("——"),
            Symbol.single("＿＿"),
            Symbol.single("～"),
            Symbol.single("﹏﹏"),
            Symbol.single("·"),
            Symbol.single("……"),
            Symbol.single("﹁"),
            Symbol.single("﹂"),
            Symbol.single("﹃"),
            Symbol.single("﹄"),
            };

    @Override
    public KeyFactory getKeyFactory() {
        return () -> KeyTable.createSymbolKeys(createKeyTableConfigure(), 0, chinese_symbols);
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

    @Override
    protected void on_CtrlKey_CommitInputList(CtrlKey key) {
        commit_InputList();
    }

    @Override
    protected void on_CtrlKey_Backspace(CtrlKey key) {
        backspace_InputList_or_InputTarget();
    }

    @Override
    protected void on_CtrlKey_Space_or_Enter(CtrlKey key) {
        confirm_Input_Enter_or_Space(key);
    }

    @Override
    protected void before_Commit_InputList() {
        // nothing to do
    }

    private void onCharKeyMsg(UserKeyMsg msg, CharKey key, UserKeyMsgData data) {
        switch (msg) {
            case KeySingleTap: {
                // 单字符输入
                play_InputtingSingleTick_Audio(key);
                doSingleKeyInputting(key);
                break;
            }
            case KeyDoubleTap: {
                // 双击字符，则替换前一个相同的输入字符：
                // 因为双击会先触发单击，而单击时已添加了一次该双击的字符
                play_InputtingSingleTick_Audio(key);
                //doReplacementKeyInputting(key);
                break;
            }
        }
    }

    private void onCtrlKeyMsg(UserKeyMsg msg, CtrlKey key, UserKeyMsgData data) {
        switch (msg) {
            case KeyDoubleTap: // 双击继续触发第二次单击操作
            case KeySingleTap: {
                switch (key.getType()) {
                    case Exit: {
                        play_InputtingSingleTick_Audio(key);
                        switch_Keyboard(Type.Pinyin);
                        break;
                    }
                }
                break;
            }
            case FingerSlipping: {
                break;
            }
        }
    }

    private void doSingleKeyInputting(CharKey key) {
        boolean continueInputting = !getInputList().isEmpty();

        CharInput input = getInputList().newPending();
        if (key.isDoubleSymbol()) {
            // 拆分成两个 Key 插入到输入列表
            CharKey leftKey = KeyTable.symbolKey(key.getText().substring(0, 1));
            CharKey rightKey = KeyTable.symbolKey(key.getText().substring(1));

            input.appendKey(leftKey);
            input = getInputList().newPending();
            input.appendKey(rightKey);
            // 确认第二个 Key，并移动光标到其后的 Gap 位置
            getInputList().confirmPending();

            // 并将光标移动到成对标点之间的 Gap 位置
            getInputList().newPendingOn(getInputList().getSelectedIndex() - 2);

            // 再在成对标点之间等待新的输入
            continueInputting = true;
        } else {
            input.appendKey(key);
        }

        if (continueInputting) {
            confirm_InputChars();
        } else {
            commit_InputList();
        }
    }

    private void doReplacementKeyInputting(CharKey key) {
        CharInput input;

        if (getInputList().hasEmptyPending()) {
            Input selected = getInputList().getSelected();
            // Note: 标点是单个输入的，故，需向前替换已输入的标点。
            // 若当前选中的是标点，则也支持双击切换标点
            Input preInput = selected.isSymbol() ? selected : getInputList().getInputBeforeSelected();

            if (preInput != null && key.isSymbol() && preInput.isSymbol()) {
                input = (CharInput) preInput;
            } else {
                return;
            }
        } else {
            input = getInputList().getPending();
        }

        Key<?> latestKey = input.getLatestKey();
        if (!key.canReplaceTheKey(latestKey)) {
            return;
        }

        CharKey latestCharKey = (CharKey) latestKey;
        // Note: 在 Input 中的 key 可能不携带 replacement 信息，只能通过当前按键做判断
        String newKeyText = key.nextReplacement(latestCharKey.getText());

        CharKey newKey = KeyTable.alphabetKey(newKeyText);
        input.replaceLatestKey(latestCharKey, newKey);
    }
}
