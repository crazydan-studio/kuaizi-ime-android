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

import org.crazydan.studio.app.ime.kuaizi.dict.PinyinDict;
import org.crazydan.studio.app.ime.kuaizi.pane.Input;
import org.crazydan.studio.app.ime.kuaizi.pane.InputList;
import org.crazydan.studio.app.ime.kuaizi.pane.Key;
import org.crazydan.studio.app.ime.kuaizi.pane.KeyFactory;
import org.crazydan.studio.app.ime.kuaizi.pane.KeyboardContext;
import org.crazydan.studio.app.ime.kuaizi.pane.input.CharInput;
import org.crazydan.studio.app.ime.kuaizi.pane.input.MathExprInput;
import org.crazydan.studio.app.ime.kuaizi.pane.key.CharKey;
import org.crazydan.studio.app.ime.kuaizi.pane.key.CtrlKey;
import org.crazydan.studio.app.ime.kuaizi.pane.key.MathOpKey;
import org.crazydan.studio.app.ime.kuaizi.pane.keyboard.keytable.MathKeyTable;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.InputMsg;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.UserKeyMsg;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.UserKeyMsgType;

import static org.crazydan.studio.app.ime.kuaizi.pane.keyboard.SymbolKeyboard.prepare_for_PairKey_Inputting;

/**
 * {@link Type#Math 算术键盘}
 * <p/>
 * 含数字、计算符号等
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-07-04
 */
public class MathKeyboard extends BaseKeyboard {

    public MathKeyboard(PinyinDict dict) {
        super(dict);
    }

    @Override
    public Type getType() {return Type.Math;}

    @Override
    public KeyFactory getKeyFactory(KeyboardContext context) {
        KeyTableConfig keyTableConf = createKeyTableConfig(context);
        MathKeyTable keyTable = MathKeyTable.create(keyTableConf);

        return keyTable::createKeys;
    }

    @Override
    public void start(KeyboardContext context) {
        start_Math_Inputting(context);
    }

    @Override
    public void stop(KeyboardContext context) {
        stop_Math_Inputting(context);
    }

    @Override
    public void onMsg(KeyboardContext context, InputMsg msg) {
        switch (msg.type) {
            case Input_Choose_Doing: {
                InputList parentInputList = getParentInputList(context);

                Input<?> input = msg.data().input;
                // 需首先确认当前输入，以确保在 Gap 上的待输入能够进入输入列表
                parentInputList.confirmPendingAndSelect(input);

                // 仅处理算术表达式输入
                if (!input.isMathExpr()) {
                    super.switch_Keyboard_to_Previous(null);
                } else {
                    resetMathInputList(context);
                }
                break;
            }
            case InputList_Clean_Done:
            case InputList_Cleaned_Cancel_Done: {
                resetMathInputList(context);
                break;
            }
        }
    }

    @Override
    public void onMsg(KeyboardContext context, UserKeyMsg msg) {
        if (try_On_Common_UserKey_Msg(context, msg)) {
            return;
        }

        Key<?> key = msg.data().key;
        if (key instanceof CharKey || key instanceof MathOpKey) {
            on_Math_Input_Doing_MathKey_Msg(context, msg);
        }
    }

    @Override
    protected boolean try_On_Common_CtrlKey_Msg(KeyboardContext context, UserKeyMsg msg) {
        if (on_Math_Input_Doing_CtrlKey_Msg(context, msg)) {
            return true;
        }
        return super.try_On_Common_CtrlKey_Msg(context, msg);
    }

//    /** 处理来自本层的算术 InputList 的消息 */
//    protected void onMathUserInputMsg(InputMsg msg) {
//        // 数字、数学符号不做候选切换：需支持更多符号时，可增加数学计算符的候选键盘
//        if (msg.type == InputMsgType.Input_Choose_Doing) {
//            Input<?> input = msg.data().input;
//
//            matchInputList.confirmPendingAndSelect(input);
//            // 对输入的修改均做替换输入
//            matchInputList.newPending();
//
//            change_State_to_Init();
//        }
//    }

    /** 响应算术输入的控制按键消息 */
    private boolean on_Math_Input_Doing_CtrlKey_Msg(KeyboardContext context, UserKeyMsg msg) {
        if (msg.type != UserKeyMsgType.SingleTap_Key) {
            return false;
        }

        CtrlKey key = context.key();
        InputList parentInputList = getParentInputList(context);

        switch (key.getType()) {
            case Backspace: {
                play_SingleTick_InputAudio(context);
                show_InputChars_Input_Popup(context);

                // Note: 若上层输入列表为空，则会对编辑器做删除，
                // 不需要处理对待输入的向前删除 #do_InputList_Backspacing
                backspace_InputList_or_Editor(context);
                return true;
            }
            case Commit_InputList: {
                play_SingleTick_InputAudio(context);

                // 提交父输入
                commit_InputList(context, true, false);

                // Note：在 X 型输入中仅需重置算术输入，而不需要退出当前键盘
                if (context.config.xInputPadEnabled) {
                    resetMathInputList(context);
                } else {
                    // 在回到前序键盘前，需先确认待输入
                    parentInputList.confirmPendingAndSelectNext();

                    exit_Keyboard(context);
                }

                return true;
            }
            case Space: {
                play_SingleTick_InputAudio(context);
                show_InputChars_Input_Popup(context);

                // Note：对于上层输入而言，当前正在输入的一定是算术输入
                parentInputList.confirmPendingAndSelectNext();

                // 在上层输入上确认空格输入，以便于继续添加新的算术输入
                confirm_InputList_Input_Enter_or_Space(context);

                // 若不是直输空格，则新建算术输入
                if (!parentInputList.isEmpty()) {
                    resetMathInputList(context);
                }
                return true;
            }
        }

        return false;
    }

    /** 响应算术输入的按键消息 */
    private void on_Math_Input_Doing_MathKey_Msg(KeyboardContext context, UserKeyMsg msg) {
        if (msg.type != UserKeyMsgType.SingleTap_Key) {
            return;
        }

        play_SingleTick_InputAudio(context);
        show_InputChars_Input_Popup(context);

        do_Single_MathKey_Inputting(context);
    }

    /** 处理数字和运算符号的输入 */
    private void do_Single_MathKey_Inputting(KeyboardContext context) {
        Key<?> key = context.key();

        InputList parentInputList = getParentInputList(context);
        parentInputList.clearPhraseCompletions();

        InputList mathInputList = getMathInputList(context);
        if (key instanceof CharKey) {
            CharInput pending = mathInputList.getPending();
            pending.appendKey(key);
        }
        //
        else if (key instanceof MathOpKey) {
            do_Single_MathOpKey_Inputting(mathInputList, (MathOpKey) key);
        }

        // Note: 可能会新建待输入，故而，需重新取最新的
        CharInput pending = mathInputList.getPending();
        fire_InputChars_Input_Doing_in_TapMode(context, pending);
    }

    /** 处理运算符号的输入 */
    private void do_Single_MathOpKey_Inputting(InputList inputList, MathOpKey key) {
        CharInput pending = inputList.getPending();

        switch (key.getType()) {
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
                if (!inputList.isGapSelected() || inputList.getSelectedIndex() > 1) {
                    inputList.confirmPendingAndSelectLast();
                }
                // Note：等号需按 default 逻辑添加至输入列表末尾
                //break;
            default:
                inputList.confirmPendingAndSelectNext();

                inputList.getPending().appendKey(key);

                // Note：单算术符号不支持追加输入
                inputList.confirmPendingAndSelectNext();
        }
    }

    /** 进入算术输入状态 */
    private void start_Math_Inputting(KeyboardContext context) {
        InputList parentInputList = getParentInputList(context);

        // 先提交从其他键盘切过来之前的待输入
        if (parentInputList.isGapSelected() //
            && !parentInputList.hasEmptyPending() //
            && !parentInputList.getPending().isMathExpr()) {
            parentInputList.confirmPendingAndSelectNext();
        } else {
            parentInputList.confirmPending();
        }

        resetMathInputList(context);
    }

    /** 结束算术输入 */
    private void stop_Math_Inputting(KeyboardContext context) {
        InputList parentInputList = getParentInputList(context);
        parentInputList.getPending().clearCompletions();

        // 在切换前，确保当前的算术输入列表已被确认
        // Note：新位置的待输入将被设置为普通输入，可接受非算术输入，故，不需要处理
        if (parentInputList.getSelected().isMathExpr() //
            || parentInputList.getPending().isMathExpr() //
        ) {
            parentInputList.confirmPendingAndSelectNext();
        }
    }

    /** 获取上层输入列表 */
    private InputList getParentInputList(KeyboardContext context) {
        return context.inputList;
    }

    /** 获取算数输入列表 */
    private InputList getMathInputList(KeyboardContext context) {
        InputList parentInputList = getParentInputList(context);
        MathExprInput pending = (MathExprInput) parentInputList.getPending();

        return pending.getInputList();
    }

    /** 重置算术输入列表（已输入内容将保持不变） */
    private void resetMathInputList(KeyboardContext context) {
        InputList parentInputList = getParentInputList(context);

        Input<?> selected = parentInputList.getSelected();
        CharInput pending = parentInputList.getPending();

        // 在父输入列表中绑定算术输入列表
        if (pending == null || !pending.isMathExpr()) {
            if (selected.isMathExpr()) {
                pending = ((MathExprInput) selected).copy();
            } else {
                pending = new MathExprInput();
            }

            parentInputList.withPending(pending);
        }

        MathExprInput input = (MathExprInput) parentInputList.getPending();
        input.getInputList().updateOption(parentInputList.getOption());
    }

    // ===================== Start: 重载置空无关接口 =====================

    @Override
    protected void do_InputList_Pending_Completion_Updating(KeyboardContext context) {
        // Note: 无需输入补全处理
    }

    @Override
    protected void do_InputList_Current_Phrase_Completion_Updating(KeyboardContext context) {
        // Note: 无需输入补全处理
    }

    // ===================== End: 重载置空无关接口 =====================

    /** 同时处理父输入列表和算术输入列表的向前删除操作 */
    @Override
    protected void do_InputList_Backspacing(KeyboardContext context) {
        InputList parentInputList = getParentInputList(context);
        InputList mathInputList = getMathInputList(context);

        // 若当前算术输入不为空，则对其做删除，
        if (!mathInputList.isEmpty()) {
            context = context.newWithInputList(mathInputList);

            super.do_InputList_Backspacing(context);

            // 当前算术输入已清空，且该输入在上层中不是 Gap 的待输入，则从父输入中移除该输入
            if (!parentInputList.isGapSelected() && mathInputList.isEmpty()) {
                parentInputList.deleteBackward();

                fire_InputChars_Input_Doing_in_TapMode(context, null);
            }
        }
        // 否则，对上层输入做删除
        else {
            if (!parentInputList.isGapSelected()) {
                parentInputList.deleteSelected();

                fire_InputChars_Input_Doing_in_TapMode(context, null);
            } else {
                super.do_InputList_Backspacing(context);
            }
        }

        // 不管哪个层级的输入列表为空，均重置算术输入列表，以接受新的算术输入
        resetMathInputList(context);
    }
}
