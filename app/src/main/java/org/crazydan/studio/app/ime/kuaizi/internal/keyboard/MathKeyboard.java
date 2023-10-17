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
import org.crazydan.studio.app.ime.kuaizi.internal.InputList;
import org.crazydan.studio.app.ime.kuaizi.internal.Key;
import org.crazydan.studio.app.ime.kuaizi.internal.Keyboard;
import org.crazydan.studio.app.ime.kuaizi.internal.input.CharInput;
import org.crazydan.studio.app.ime.kuaizi.internal.input.CharMathExprInput;
import org.crazydan.studio.app.ime.kuaizi.internal.input.CompletionInput;
import org.crazydan.studio.app.ime.kuaizi.internal.key.CharKey;
import org.crazydan.studio.app.ime.kuaizi.internal.key.CtrlKey;
import org.crazydan.studio.app.ime.kuaizi.internal.key.MathOpKey;
import org.crazydan.studio.app.ime.kuaizi.internal.keyboard.keytable.MathKeyTable;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.UserInputMsg;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.UserInputMsgData;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.UserInputMsgListener;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.UserKeyMsg;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.UserKeyMsgData;

/**
 * {@link Keyboard.Type#Math 算数键盘}
 * <p/>
 * 含数字、计算符号等
 * <p/>
 * 算数键盘涉及对 {@link InputList} 的嵌套处理，
 * 故而，必须采用独立的子键盘模式，而不能采用嵌入模式
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-07-04
 */
public class MathKeyboard extends BaseKeyboard {
    private final UserInputMsgListener topUserInputMsgListener = this::onTopUserInputMsg;

    private InputList mathInputList;

    @Override
    protected KeyFactory doGetKeyFactory() {
        MathKeyTable keyTable = MathKeyTable.create(createKeyTableConfigure());

        return keyTable::createKeys;
    }

    @Override
    protected KeyTable.Config createKeyTableConfigure() {
        return new KeyTable.Config(getConfig(),
                                   !getTopInputList().isEmpty(),
                                   getTopInputList().canRevokeCommit(),
                                   !getInputList().isGapSelected());
    }

    @Override
    public void onUserInputMsg(UserInputMsg msg, UserInputMsgData data) {
        // 数字、数学符号不做候选切换：需支持更多符号时，可增加数学计算符的候选键盘
        if (msg == UserInputMsg.Input_Choose_Doing) {
            InputList inputList = getInputList();
            Input<?> input = data.target;

            inputList.confirmPendingAndSelect(input);
            // 对输入的修改均做替换输入
            inputList.newPending();

            change_State_to_Init();
            return;
        }

        super.onUserInputMsg(msg, data);
    }

    public void onTopUserInputMsg(UserInputMsg msg, UserInputMsgData data) {
        switch (msg) {
            case Input_Completion_Choose_Doing: {
                InputList topInputList = getTopInputList();
                CompletionInput completion = (CompletionInput) data.target;

                start_InputList_Completion_Applying(topInputList, completion);
                break;
            }
            case Input_Choose_Doing: {
                InputList topInputList = getTopInputList();
                Input<?> input = data.target;

                // 需首先确认当前输入，以确保在 Gap 上的待输入能够进入输入列表
                topInputList.confirmPendingAndSelect(input);

                // 仅处理算数表达式输入
                if (!input.isMathExpr()) {
                    super.switchTo_Previous_Keyboard(null);
                } else {
                    resetMathInputList();
                }
                break;
            }
            case Inputs_Clean_Done: {
                resetMathInputList();

                fire_InputList_Clean_Done();
                break;
            }
            case Inputs_Cleaned_Cancel_Done: {
                resetMathInputList();

                fire_InputList_Cleaned_Cancel_Done();
                break;
            }
        }
    }

    @Override
    public void onUserKeyMsg(UserKeyMsg msg, UserKeyMsgData data) {
        if (try_OnUserKeyMsg(msg, data)) {
            return;
        }

        Key<?> key = data.target;
        if (key instanceof CharKey || key instanceof MathOpKey) {
            onMathKeyMsg(msg, key, data);
        }
    }

    @Override
    protected boolean try_Common_OnCtrlKeyMsg(UserKeyMsg msg, CtrlKey key, UserKeyMsgData data) {
        InputList topInputList = getTopInputList();
        InputList mathInputList = getMathInputList();

        if (msg == UserKeyMsg.KeySingleTap) {
            switch (key.getType()) {
                case Backspace: {
                    play_SingleTick_InputAudio(key);

                    // 若当前算数输入不为空，则对其做删除，否则，对上层输入做删除
                    if (!mathInputList.isEmpty()) {
                        backspace_InputList_or_Editor(mathInputList, key);
                    } else {
                        backspace_InputList_or_Editor(topInputList, key);
                    }
                    return true;
                }
                case Commit_InputList: {
                    play_SingleTick_InputAudio(key);

                    // 提交上层输入
                    commit_InputList(topInputList, true, false);

                    // 若是在 内嵌键盘 内提交，则需新建算数输入，以接收新的输入
                    if (this.state.previous != null) {
                        newMatchExprInput(topInputList);
                    }
                    // 否则，若是在不同类型的键盘内提交，
                    // 则在回到前序键盘前，需先确认待输入
                    else {
                        topInputList.confirmPendingAndSelectNext();
                    }
                    exit(key);

                    return true;
                }
                case Space: {
                    play_SingleTick_InputAudio(key);

                    // Note：对于上层 InputList 而言，当前正在输入的一定是算数输入
                    topInputList.confirmPendingAndSelectNext();

                    confirm_InputList_Input_Enter_or_Space(topInputList, key);

                    // 若不是直输空格，则新建算数输入
                    if (!topInputList.isEmpty()) {
                        newMatchExprInput(topInputList);
                    }
                    return true;
                }
            }
        }

        return super.try_Common_OnCtrlKeyMsg(msg, key, data);
    }

    private void onMathKeyMsg(UserKeyMsg msg, Key<?> key, UserKeyMsgData data) {
        if (msg == UserKeyMsg.KeySingleTap) {
            play_SingleTick_InputAudio(key);

            do_Single_MathKey_Inputting(key);
        }
    }

    private void do_Single_MathKey_Inputting(Key<?> key) {
        InputList topInputList = getTopInputList();
        topInputList.clearPhraseCompletions();

        InputList mathInputList = getMathInputList();
        CharInput pending = mathInputList.getPending();

        if (key instanceof CharKey) {
            pending.appendKey(key);
        } else if (key instanceof MathOpKey) {
            switch (((MathOpKey) key).getType()) {
                case dot:
                    // 一个输入中只能有一个符号
                    if (!pending.hasSameKey(key)) {
                        pending.appendKey(key);
                    }
                    break;
                case brackets:
                    prepare_for_PairKey_Inputting(mathInputList,
                                                  () -> MathKeyTable.bracketKey("("),
                                                  () -> MathKeyTable.bracketKey(")"));
                    break;
                case equal:
                    // 除开头以外的位置，等号始终添加到输入列表的末尾
                    if (!mathInputList.isGapSelected() || mathInputList.getSelectedIndex() > 1) {
                        mathInputList.confirmPendingAndSelectLast();
                    }
                    // Note：等号需按 default 逻辑添加至输入列表末尾
                    //break;
                default:
                    mathInputList.confirmPendingAndSelectNext();

                    mathInputList.getPending().appendKey(key);

                    // Note：单算数符号不支持追加输入
                    mathInputList.confirmPendingAndSelectNext();
            }
        }

        fire_InputChars_Input_Doing(key);
    }

    @Override
    protected void switchTo_Previous_Keyboard(Key<?> key) {
        InputList topInputList = getTopInputList();

        // 在切换前，确保当前的算数输入列表已被确认
        // Note：新位置的待输入将被设置为普通输入，可接受非算数输入，故，不需要处理
        topInputList.confirmPendingAndSelectNext();

        super.switchTo_Previous_Keyboard(key);
    }

    @Override
    public InputList getInputList() {
        // 基类的输入列表操作均做用到算数输入列表上
        return getMathInputList();
    }

    @Override
    public void setInputList(InputList inputList) {
        newMatchExprInput(inputList);
    }

    @Override
    public void destroy() {
        InputList topInputList = getTopInputList();

        dropMathInputList();
        topInputList.removeUserInputMsgListener(this.topUserInputMsgListener);

        super.destroy();
    }

    @Override
    public void reset() {
        resetMathInputList();

        super.reset();
    }

    private InputList getMathInputList() {
        return this.mathInputList;
    }

    /** 重置当前键盘的算数输入列表（已输入内容将保持不变） */
    private void resetMathInputList() {
        setInputList(getTopInputList());
    }

    private void dropMathInputList() {
        if (this.mathInputList == null) {
            return;
        }

        // 移除算数输入列表的监听
        this.mathInputList.removeUserInputMsgListener(this);
        this.mathInputList = null;
    }

    private InputList getTopInputList() {
        return super.getInputList();
    }

    private void newMatchExprInput(InputList topInputList) {
        dropMathInputList();
        super.setInputList(topInputList);

        // 算数键盘仅响应上层输入列表的特定事件
        topInputList.removeUserInputMsgListener(this);
        topInputList.addUserInputMsgListener(this.topUserInputMsgListener);

        Input<?> selected = topInputList.getSelected();
        CharInput pending = topInputList.getPending();

        if (pending == null || !pending.isMathExpr()) {
            if (selected.isMathExpr()) {
                pending = ((CharMathExprInput) selected).copy();
            } else {
                pending = new CharMathExprInput();
            }

            topInputList.withPending(pending);
        }

        // 在上层输入列表中绑定算数输入列表
        CharMathExprInput input = (CharMathExprInput) topInputList.getPending();

        this.mathInputList = input.getInputList();
        this.mathInputList.addUserInputMsgListener(this);
    }
}
