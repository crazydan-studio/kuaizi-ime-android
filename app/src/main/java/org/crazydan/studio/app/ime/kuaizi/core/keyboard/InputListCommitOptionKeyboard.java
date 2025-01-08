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

import org.crazydan.studio.app.ime.kuaizi.core.Input;
import org.crazydan.studio.app.ime.kuaizi.core.InputList;
import org.crazydan.studio.app.ime.kuaizi.core.KeyFactory;
import org.crazydan.studio.app.ime.kuaizi.core.KeyboardContext;
import org.crazydan.studio.app.ime.kuaizi.core.input.word.PinyinWord;
import org.crazydan.studio.app.ime.kuaizi.core.key.CtrlKey;
import org.crazydan.studio.app.ime.kuaizi.core.keyboard.keytable.InputListCommitOptionKeyTable;
import org.crazydan.studio.app.ime.kuaizi.core.keyboard.state.InputListCommitOptionChooseStateData;
import org.crazydan.studio.app.ime.kuaizi.core.msg.UserKeyMsg;
import org.crazydan.studio.app.ime.kuaizi.core.msg.UserKeyMsgType;

/**
 * {@link Type#InputList_Commit_Option 输入列表提交选项键盘}
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2025-01-08
 */
public class InputListCommitOptionKeyboard extends DirectInputKeyboard {

    public InputListCommitOptionKeyboard() {
        super(null);
    }

    @Override
    public Type getType() {return Type.InputList_Commit_Option;}

    @Override
    public void start(KeyboardContext context) {
        start_InputList_Commit_Option_Choosing(context);
    }

    @Override
    public KeyFactory buildKeyFactory(KeyboardContext context) {
        if (this.state.type != State.Type.InputList_Commit_Option_Choose_Doing) {
            return null;
        }

        KeyTableConfig keyTableConf = createKeyTableConfig(context);
        InputListCommitOptionKeyTable keyTable = InputListCommitOptionKeyTable.create(keyTableConf);

        InputListCommitOptionChooseStateData stateData = this.state.data();

        return () -> keyTable.createKeys(stateData.getOption(), stateData.hasSpell(), stateData.hasVariant());
    }

    @Override
    protected void switch_Keyboard_to_Previous(KeyboardContext context) {
        InputList inputList = context.inputList;
        InputListCommitOptionChooseStateData stateData = this.state.data();
        // 恢复输入列表的 Input.Option 至切换前的状态
        inputList.setInputOption(stateData.oldOption);

        // Note: 直接回到其切换前的键盘，而不管切换前的是否为主键盘
        switch_Keyboard_To(context, context.keyboardPrevType);
    }

    @Override
    protected boolean try_On_Common_CtrlKey_Msg(KeyboardContext context, UserKeyMsg msg) {
        CtrlKey key = context.key();

        // Note: 仅处理 Commit_InputList 按键的单击消息，忽略其余消息
        if (key.type == CtrlKey.Type.Commit_InputList //
            && msg.type != UserKeyMsgType.SingleTap_Key) {
            return true;
        }

        return super.try_On_Common_CtrlKey_Msg(context, msg);
    }

    @Override
    protected void on_CtrlKey_Msg(KeyboardContext context, UserKeyMsg msg) {
        if (msg.type != UserKeyMsgType.SingleTap_Key) {
            return;
        }

        CtrlKey key = context.key();
        if (!CtrlKey.Type.Commit_InputList_Option.match(key)) {
            return;
        }

        if (update_InputList_Commit_Option(context)) {
            play_SingleTick_InputAudio(context);
        }
        fire_InputChars_Input_Done(context, null);
    }

    private void start_InputList_Commit_Option_Choosing(KeyboardContext context) {
        InputList inputList = context.inputList;
        Input.Option inputOption = inputList.getInputOption();

        InputListCommitOptionChooseStateData stateData = new InputListCommitOptionChooseStateData(inputOption);
        stateData.update(inputList);

        this.state = new State(State.Type.InputList_Commit_Option_Choose_Doing, stateData, createInitState());
    }

    /**
     * 更新输入列表提交选项
     *
     * @return 若存在变更，则返回 true，否则，返回 false
     */
    private boolean update_InputList_Commit_Option(KeyboardContext context) {
        CtrlKey key = context.key();
        InputList inputList = context.inputList;
        InputListCommitOptionChooseStateData stateData = this.state.data();

        Input.Option oldInputOption = inputList.getInputOption();
        Input.Option newInputOption = null;

        CtrlKey.Option<CtrlKey.InputWordCommitMode> option = key.option();
        CtrlKey.InputWordCommitMode mode = option.value;
        if (stateData.hasSpell()) {
            switch (mode) {
                case only_pinyin:
                case with_pinyin: {
                    PinyinWord.SpellUsedMode expected = //
                            mode == CtrlKey.InputWordCommitMode.only_pinyin
                            ? PinyinWord.SpellUsedMode.replacing
                            : PinyinWord.SpellUsedMode.following;

                    PinyinWord.SpellUsedMode spellUsedMode = //
                            oldInputOption.wordSpellUsedMode == expected ? null : expected;

                    newInputOption = new Input.Option(spellUsedMode, oldInputOption.wordVariantUsed);
                    break;
                }
            }
        }
        if (stateData.hasVariant()) {
            switch (mode) {
                case trad_to_simple:
                case simple_to_trad: {
                    // 被禁用的繁简转换按钮不做响应
                    if (!key.disabled) {
                        newInputOption = //
                                new Input.Option(oldInputOption.wordSpellUsedMode, !oldInputOption.wordVariantUsed);
                    }
                    break;
                }
            }
        }

        if (newInputOption != null) {
            inputList.setInputOption(newInputOption);

            stateData.update(inputList);

            return true;
        }
        return false;
    }
}
