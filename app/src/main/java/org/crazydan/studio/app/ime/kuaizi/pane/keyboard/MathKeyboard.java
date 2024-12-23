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

package org.crazydan.studio.app.ime.kuaizi.pane.keyboard;

import java.util.function.Supplier;

import org.crazydan.studio.app.ime.kuaizi.pane.Input;
import org.crazydan.studio.app.ime.kuaizi.pane.InputList;
import org.crazydan.studio.app.ime.kuaizi.pane.Key;
import org.crazydan.studio.app.ime.kuaizi.pane.KeyFactory;
import org.crazydan.studio.app.ime.kuaizi.pane.input.CharInput;
import org.crazydan.studio.app.ime.kuaizi.pane.input.MathExprInput;
import org.crazydan.studio.app.ime.kuaizi.pane.key.CharKey;
import org.crazydan.studio.app.ime.kuaizi.pane.key.CtrlKey;
import org.crazydan.studio.app.ime.kuaizi.pane.key.InputWordKey;
import org.crazydan.studio.app.ime.kuaizi.pane.key.MathOpKey;
import org.crazydan.studio.app.ime.kuaizi.pane.key.SymbolKey;
import org.crazydan.studio.app.ime.kuaizi.pane.keyboard.keytable.MathKeyTable;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.InputMsg;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.InputMsgType;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.UserKeyMsg;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.UserKeyMsgType;

/**
 * {@link Type#Math 算术键盘}
 * <p/>
 * 含数字、计算符号等
 * <p/>
 * 算术键盘涉及对 {@link InputList} 的嵌套处理，
 * 故而，必须采用独立的子键盘模式，而不能采用嵌入模式
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-07-04
 */
public class MathKeyboard extends BaseKeyboard {
    private InputList mathInputList;

    @Override
    public Type getType() {
        return Type.Math;
    }

    @Override
    public KeyFactory getKeyFactory(InputList inputList) {
        MathKeyTable keyTable = MathKeyTable.create(createKeyTableConfig(inputList));

        return keyTable::createKeys;
    }

    @Override
    protected KeyTableConfig createKeyTableConfig(InputList inputList) {
        return new KeyTableConfig(getConfig(),
                                  !getParentInputList().isEmpty(),
                                  getParentInputList().canRevokeCommit(),
                                  !getInputList().isGapSelected());
    }

    /** 处理来自本层的算术 InputList 的消息 */
    protected void onMathUserInputMsg(InputList matchInputList, InputMsg msg) {
        // 数字、数学符号不做候选切换：需支持更多符号时，可增加数学计算符的候选键盘
        if (msg.type == InputMsgType.Input_Choose_Doing) {
            Input<?> input = msg.data.input;

            matchInputList.confirmPendingAndSelect(input);
            // 对输入的修改均做替换输入
            matchInputList.newPending();

            change_State_to_Init();
            return;
        }

        super.onMsg(matchInputList, msg);
    }

    /** 处理来自父 InputList 的消息 */
    @Override
    public void onMsg(InputList parentInputList, InputMsg msg) {
        switch (msg.type) {
            case Input_Completion_Apply_Done: {
                withMathExprPending(parentInputList);
                fire_InputChars_Input_Doing_in_TapMode(null, null);
                break;
            }
            case Input_Choose_Doing: {
                Input<?> input = msg.data.input;

                // 需首先确认当前输入，以确保在 Gap 上的待输入能够进入输入列表
                parentInputList.confirmPendingAndSelect(input);

                // 仅处理算术表达式输入
                if (!input.isMathExpr()) {
                    super.switch_Keyboard_to_Previous(null);
                } else {
                    resetMathInputList();
                }
                break;
            }
            case InputList_Clean_Done:
            case InputList_Cleaned_Cancel_Done: {
                resetMathInputList();
                break;
            }
        }
    }

    @Override
    public void onMsg(InputList inputList, UserKeyMsg msg) {
        if (try_On_Common_UserKey_Msg(inputList, msg)) {
            return;
        }

        Key<?> key = msg.data.key;
        if (key instanceof CharKey || key instanceof MathOpKey) {
            onMathKeyMsg(msg, key);
        }
    }

    @Override
    protected boolean try_On_Common_CtrlKey_Msg(InputList inputList, UserKeyMsg msg, CtrlKey key) {
        InputList parentInputList = getParentInputList();
        InputList mathInputList = getMathInputList();

        if (msg.type == UserKeyMsgType.SingleTap_Key) {
            switch (key.getType()) {
                case Backspace: {
                    play_SingleTick_InputAudio(key);
                    show_InputChars_Input_Popup(key);

                    // 若当前算术输入不为空，则对其做删除，否则，对上层输入做删除
                    if (!mathInputList.isEmpty()) {
                        backspace_InputList_or_Editor(mathInputList, key);
                    } else {
                        backspace_InputList_or_Editor(parentInputList, key);
                    }
                    return true;
                }
                case Commit_InputList: {
                    play_SingleTick_InputAudio(key);

                    // 提交父输入
                    commit_InputList(parentInputList, true, false);

                    // Note：在 X 型输入中仅需重置算术输入，而不需要退出当前键盘
                    if (this.config.xInputPadEnabled) {
                        resetMathInputList();
                        return true;
                    }

                    // 若是在 内嵌键盘 内提交，则需新建算术输入，以接收新的输入
                    if (this.state.previous != null) {
                        resetMathInputList();
                    }
                    // 否则，若是在不同类型的键盘内提交，
                    // 则在回到前序键盘前，需先确认待输入
                    else {
                        parentInputList.confirmPendingAndSelectNext();
                    }
                    exit_Keyboard(key);

                    return true;
                }
                case Space: {
                    play_SingleTick_InputAudio(key);
                    show_InputChars_Input_Popup(key);

                    // Note：对于父 InputList 而言，当前正在输入的一定是算术输入
                    parentInputList.confirmPendingAndSelectNext();

                    confirm_InputList_Input_Enter_or_Space(parentInputList, key);

                    // 若不是直输空格，则新建算术输入
                    if (!parentInputList.isEmpty()) {
                        resetMathInputList();
                    }
                    return true;
                }
            }
        }

        return super.try_On_Common_CtrlKey_Msg(mathInputList, msg, key);
    }

    private void onMathKeyMsg(UserKeyMsg msg, Key<?> key) {
        if (msg.type == UserKeyMsgType.SingleTap_Key) {
            play_SingleTick_InputAudio(key);
            show_InputChars_Input_Popup(key);

            do_Single_MathKey_Inputting(key);
        }
    }

    private void do_Single_MathKey_Inputting(Key<?> key) {
        InputList parentInputList = getParentInputList();
        parentInputList.clearPhraseCompletions();

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

                    // Note：单算术符号不支持追加输入
                    mathInputList.confirmPendingAndSelectNext();
            }
        }

        fire_InputChars_Input_Doing_in_TapMode(key, pending);
    }

    @Override
    public InputList getInputList() {
        // 基类的输入列表操作均做用到算术输入列表上
        return getMathInputList();
    }

    @Override
    public void setInputList(Supplier<InputList> getter) {
        setParentInputList(getter);
    }

    @Override
    public void start(InputList inputList) {
        InputList parentInputList = getParentInputList();

        // 先提交从其他键盘切过来之前的待输入
        if (parentInputList.isGapSelected() //
            && !parentInputList.hasEmptyPending() //
            && !parentInputList.getPending().isMathExpr()) {
            parentInputList.confirmPendingAndSelectNext();
        } else {
            parentInputList.confirmPending();
        }

        resetMathInputList();

        super.start(inputList);
    }

    @Override
    public void destroy() {
        InputList parentInputList = getParentInputList();
        before_ChangeState_Or_SwitchKeyboard(parentInputList);

        dropMathInputList();

        super.destroy();
    }

    @Override
    public void reset() {
        resetMathInputList();

        super.reset();
    }

    @Override
    protected void do_InputList_Backspacing(InputList inputList, Key<?> key) {
        InputList parentInputList = getParentInputList();

        // 处理父输入
        if (parentInputList == inputList) {
            if (!inputList.isGapSelected()) {
                inputList.deleteSelected();
                fire_InputChars_Input_Doing_in_TapMode(key, null);
            } else {
                super.do_InputList_Backspacing(inputList, key);
            }
        }
        // 处理算术输入
        else {
            super.do_InputList_Backspacing(inputList, key);

            // 当前算术输入已清空，且该输入在上层中不是 Gap 的待输入，则从父输入中移除该输入
            if (!parentInputList.isGapSelected() && inputList.isEmpty()) {
                parentInputList.deleteBackward();
                fire_InputChars_Input_Doing_in_TapMode(key, null);
            }
        }

        // 不管哪个层级的输入列表为空，均重置算术输入列表，以接受新的算术输入
        resetMathInputList();
    }

    @Override
    protected void switch_Keyboard_to_Previous(Key<?> key) {
        InputList parentInputList = getParentInputList();
        before_ChangeState_Or_SwitchKeyboard(parentInputList);

        super.switch_Keyboard_to_Previous(key);
    }

    // <<<<<<<<<<<<<<<< Start - 支持切换表情和标点键盘

    /** 从表情和标点符号切换回来时调用 */
    @Override
    protected void change_State_to_Previous(Key<?> key) {
        super.change_State_to_Previous(key);

        resetMathInputList();
    }

    @Override
    protected void do_Single_Emoji_Inputting(InputList inputList, InputWordKey key) {
        InputList parentInputList = getParentInputList();
        before_ChangeState_Or_SwitchKeyboard(parentInputList);

        super.do_Single_Emoji_Inputting(parentInputList, key);
    }

    @Override
    protected void do_Single_Symbol_Inputting(InputList inputList, SymbolKey key, boolean continuousInput) {
        InputList parentInputList = getParentInputList();
        before_ChangeState_Or_SwitchKeyboard(parentInputList);

        super.do_Single_Symbol_Inputting(parentInputList, key, continuousInput);
    }

    @Override
    protected void revoke_Committed_InputList(InputList inputList, Key<?> key) {
        InputList parentInputList = getParentInputList();

        super.revoke_Committed_InputList(parentInputList, key);
    }
    // >>>>>>>>>>>>>>>> End

    private void before_ChangeState_Or_SwitchKeyboard(InputList parentInputList) {
        parentInputList.getPending().clearCompletions();

        // 在切换前，确保当前的算术输入列表已被确认
        // Note：新位置的待输入将被设置为普通输入，可接受非算术输入，故，不需要处理
        if (parentInputList.getSelected().isMathExpr() //
            || parentInputList.getPending().isMathExpr() //
        ) {
            parentInputList.confirmPendingAndSelectNext();
        }
    }

    private InputList getMathInputList() {
        return this.mathInputList;
    }

    private InputList getParentInputList() {
        return super.getInputList();
    }

    private void setParentInputList(Supplier<InputList> getter) {
        super.setInputList(getter);
    }

    private void dropMathInputList() {
        if (this.mathInputList != null) {
            this.mathInputList.setListener(null);
        }
        this.mathInputList = null;
    }

    /** 重置当前键盘的算术输入列表（已输入内容将保持不变） */
    private void resetMathInputList() {
        InputList parentInputList = getParentInputList();

        dropMathInputList();
        withMathExprPending(parentInputList);
    }

    /** 确保当前的待输入为算术输入 */
    private void withMathExprPending(InputList parentInputList) {
        Input<?> selected = parentInputList.getSelected();
        CharInput pending = parentInputList.getPending();

        if (pending == null || !pending.isMathExpr()) {
            if (selected.isMathExpr()) {
                pending = ((MathExprInput) selected).copy();
            } else {
                pending = new MathExprInput();
            }

            parentInputList.withPending(pending);
        }

        // 在父输入列表中绑定算术输入列表
        MathExprInput input = (MathExprInput) parentInputList.getPending();
        this.mathInputList = input.getInputList();
        this.mathInputList.updateOption(parentInputList.getOption());
        this.mathInputList.setListener(this::onMathUserInputMsg);
    }
}