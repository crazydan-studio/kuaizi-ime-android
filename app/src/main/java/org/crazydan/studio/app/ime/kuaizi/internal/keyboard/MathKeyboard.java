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
 * {@link Keyboard.Type#Math 数学键盘}
 * <p/>
 * 含数字、计算符号等
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
        // 数字、数学符号不做候选切换：支持更多符号时，可增加数学计算符的候选键盘
        if (msg == UserInputMsg.Choosing_Input) {
            Input<?> input = data.target;

            getInputList().newPendingOn(input);
            confirm_Pending_and_Waiting_Input();
            return;
        }

        super.onUserInputMsg(msg, data);
    }

    public void onTopUserInputMsg(UserInputMsg msg, UserInputMsgData data) {
        switch (msg) {
            case Choosing_Input: {
                Input<?> input = data.target;

                // newPendingOn(input) 不会确认已选中的输入，
                // 故，需强制确认当前输入，以确保在 Gap 上的待输入能够进入输入列表
                getTopInputList().confirmPending();
                getTopInputList().newPendingOn(input);

                // 仅处理算数表达式输入
                if (!input.isMathExpr()) {
                    super.switchTo_Previous_Keyboard();
                } else {
                    resetMathInputList();
                }
                break;
            }
            case Cleaning_Inputs: {
                resetMathInputList();

                clean_InputList();
                break;
            }
            case Canceling_Cleaning_Inputs: {
                resetMathInputList();

                cancel_Cleaning_InputList();
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

        if (msg == UserKeyMsg.KeySingleTap) {
            switch (key.getType()) {
                case Backspace: {
                    play_InputtingSingleTick_Audio(key);

                    // 若当前算数输入不为空，则对其做删除，否则，对上层输入做删除
                    if (!getInputList().isEmpty()) {
                        backspace_InputList_or_InputTarget(getInputList());
                    } else {
                        backspace_InputList_or_InputTarget(topInputList);
                    }
                    return true;
                }
                case Commit_InputList: {
                    play_InputtingSingleTick_Audio(key);

                    // 提交上层输入
                    commit_InputList(topInputList, true, false);
                    // 并退出
                    do_Exit();
                    return true;
                }
                case Space: {
                    play_InputtingSingleTick_Audio(key);

                    // Note：对于上层 InputList 而言，当前正在输入的一定是算数输入
                    topInputList.confirmPendingAndMoveToNextGapInput();

                    confirm_Input_Enter_or_Space(topInputList, key);

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
            play_InputtingSingleTick_Audio(key);

            do_Single_MathKey_Inputting(key);
        }
    }

    private void do_Single_MathKey_Inputting(Key<?> key) {
        InputList inputList = getInputList();

        if (inputList.hasEmptyPending()) {
            inputList.newPending();
        }

        CharInput pending = inputList.getPending();

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
                    prepare_for_PairKey_Inputting(inputList,
                                                  () -> MathKeyTable.bracketKey("("),
                                                  () -> MathKeyTable.bracketKey(")"));
                    break;
                case equal:
                    // 除开头以外的位置，等号始终添加到输入列表的末尾
                    if (inputList.getSelectedIndex() > 1) {
                        inputList.confirmPending();
                        inputList.moveToLastInput();
                    }
                default:
                    inputList.newPending().appendKey(key);
                    inputList.confirmPendingAndMoveToNextGapInput();
            }
        }

        fire_and_Waiting_Continuous_InputChars_Inputting(key);
    }

    @Override
    protected void switchTo_Previous_Keyboard() {
        // 在切换前，确保当前的算数输入列表已被确认，
        getTopInputList().confirmPending();
        // 并丢弃当前的算数输入 pending 以备用于输入其他字符
        getTopInputList().newPending();

        super.switchTo_Previous_Keyboard();
    }

    @Override
    public InputList getInputList() {
        // 基类的输入列表操作均做用到算数输入列表上
        return this.mathInputList;
    }

    @Override
    public void destroy() {
        dropMathInputList();
        getTopInputList().removeUserInputMsgListener(this.topUserInputMsgListener);

        super.destroy();
    }

    @Override
    public void reset() {
        resetMathInputList();

        super.reset();
    }

    @Override
    protected void do_Exit() {
        // 退出前，先移动光标至相邻 Gap
        getTopInputList().confirmPendingAndMoveToNextGapInput();

        super.do_Exit();
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

    @Override
    public void setInputList(InputList inputList) {
        newMatchExprInput(inputList);
    }

    private void newMatchExprInput(InputList topInputList) {
        dropMathInputList();
        super.setInputList(topInputList);

        // 算数键盘仅响应上层输入列表的特定事件
        topInputList.removeUserInputMsgListener(this);
        topInputList.addUserInputMsgListener(this.topUserInputMsgListener);

        Input<?> selected = topInputList.getSelected();
        Input<?> pending = topInputList.getPending();

        if (pending == null || !pending.isMathExpr()) {
            if (selected.isMathExpr()) {
                topInputList.newPendingOn(selected);
            } else {
                topInputList.withPending(new CharMathExprInput());
            }
        }

        // 在上层输入列表中绑定算数输入列表
        CharMathExprInput input = (CharMathExprInput) topInputList.getPending();

        this.mathInputList = input.getInputList();
        this.mathInputList.addUserInputMsgListener(this);
    }
}
