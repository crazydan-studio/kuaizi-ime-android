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
import org.crazydan.studio.app.ime.kuaizi.internal.input.CharMathExprInput;
import org.crazydan.studio.app.ime.kuaizi.internal.key.CharKey;
import org.crazydan.studio.app.ime.kuaizi.internal.key.CtrlKey;
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
                                   getTopInputList().canBeRevoked(),
                                   !getInputList().isGapSelected());
    }

    @Override
    public void onUserInputMsg(UserInputMsg msg, UserInputMsgData data) {
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
        }
    }

    @Override
    public void onUserKeyMsg(UserKeyMsg msg, UserKeyMsgData data) {
        if (try_OnUserKeyMsg(msg, data)) {
            return;
        }

        Key<?> key = data.target;
        if (key instanceof CharKey //
            || (key instanceof CtrlKey //
                && ((CtrlKey) key).getType() == CtrlKey.Type.Math_Operator)) {
            onMathKeyMsg(msg, key, data);
        }
    }

    @Override
    protected boolean try_Common_OnCtrlKeyMsg(UserKeyMsg msg, CtrlKey key, UserKeyMsgData data) {
        if (msg == UserKeyMsg.KeySingleTap) {
            switch (key.getType()) {
                case Backspace: {
                    play_InputtingSingleTick_Audio(key);

                    // 若当前算数输入不为空，则对其做删除，否则，对上层输入做删除
                    if (!getInputList().isEmpty()) {
                        backspace_InputList_or_InputTarget(getInputList());
                    } else {
                        backspace_InputList_or_InputTarget(getTopInputList());
                    }
                    return true;
                }
                case Commit_InputList: {
                    play_InputtingSingleTick_Audio(key);

                    // 提交上层输入
                    commit_InputList(getTopInputList(), true, false);
                    // 并退出
                    do_Exit();
                    return true;
                }
                case Space: {
                    play_InputtingSingleTick_Audio(key);

                    // TODO 上层添加空格，并准备新的算数输入
                    confirm_Input_Enter_or_Space(getTopInputList(), key);
                    return true;
                }
            }
        }

        return super.try_Common_OnCtrlKeyMsg(msg, key, data);
    }

    private void onMathKeyMsg(UserKeyMsg msg, Key<?> key, UserKeyMsgData data) {
        if (msg == UserKeyMsg.KeySingleTap) {
            play_InputtingSingleTick_Audio(key);

            do_Single_Key_Inputting(key, false);
        }
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
        dropMathInputList();
        super.setInputList(inputList);

        // 算数键盘仅响应上层输入列表的特定事件
        inputList.removeUserInputMsgListener(this);
        inputList.addUserInputMsgListener(this.topUserInputMsgListener);

        Input<?> selected = inputList.getSelected();
        Input<?> pending = inputList.getPending();

        if (pending == null || !pending.isMathExpr()) {
            if (selected.isMathExpr()) {
                inputList.newPendingOn(selected);
            } else {
                inputList.withPending(new CharMathExprInput());
            }
        }

        // 在上层输入列表中绑定算数输入列表
        CharMathExprInput input = (CharMathExprInput) inputList.getPending();

        this.mathInputList = input.getInputList();
        this.mathInputList.addUserInputMsgListener(this);
    }
}
