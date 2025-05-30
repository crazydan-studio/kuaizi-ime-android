/*
 * 筷字输入法 - 高效编辑需要又好又快的输入法
 * Copyright (C) 2025 Crazydan Studio <https://studio.crazydan.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.
 * If not, see <https://www.gnu.org/licenses/lgpl-3.0.en.html#license-text>.
 */

package org.crazydan.studio.app.ime.kuaizi.core.keyboard;

import org.crazydan.studio.app.ime.kuaizi.core.Input;
import org.crazydan.studio.app.ime.kuaizi.core.InputList;
import org.crazydan.studio.app.ime.kuaizi.core.Key;
import org.crazydan.studio.app.ime.kuaizi.core.KeyFactory;
import org.crazydan.studio.app.ime.kuaizi.core.KeyboardContext;
import org.crazydan.studio.app.ime.kuaizi.core.input.CharInput;
import org.crazydan.studio.app.ime.kuaizi.core.input.GapInput;
import org.crazydan.studio.app.ime.kuaizi.core.input.MathExprInput;
import org.crazydan.studio.app.ime.kuaizi.core.key.CharKey;
import org.crazydan.studio.app.ime.kuaizi.core.key.CtrlKey;
import org.crazydan.studio.app.ime.kuaizi.core.key.MathOpKey;
import org.crazydan.studio.app.ime.kuaizi.core.keyboard.keytable.MathKeyTable;
import org.crazydan.studio.app.ime.kuaizi.core.msg.InputMsg;
import org.crazydan.studio.app.ime.kuaizi.core.msg.UserKeyMsg;
import org.crazydan.studio.app.ime.kuaizi.core.msg.UserKeyMsgType;

import static org.crazydan.studio.app.ime.kuaizi.core.keyboard.SymbolKeyboard.prepare_for_PairKey_Inputting;
import static org.crazydan.studio.app.ime.kuaizi.core.msg.InputMsgType.Input_Choose_Done;

/**
 * {@link Type#Math 算术键盘}
 * <p/>
 * 含数字、计算符号等
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-07-04
 */
public class MathKeyboard extends EditorEditKeyboard {

    @Override
    public Type getType() {return Type.Math;}

    @Override
    public KeyFactory buildKeyFactory(KeyboardContext context) {
        KeyTableConfig keyTableConf = createKeyTableConfig(context);
        MathKeyTable keyTable = MathKeyTable.create(keyTableConf);

        return keyTable::createGrid;
    }

    @Override
    public void start(KeyboardContext context) {
        start_Math_Inputting(context);
    }

    @Override
    public void stop(KeyboardContext context) {
        stop_Math_Inputting(context);
    }

    /** 获取上层输入列表 */
    private InputList getParentInputList(KeyboardContext context) {
        return context.inputList;
    }

    /** 获取算数输入列表 */
    private InputList getMathInputList(KeyboardContext context) {
        InputList parentInputList = getParentInputList(context);
        MathExprInput pending = parentInputList.getMathExprPending();

        return pending.getInputList();
    }

    // ===================== Start: 消息处理 ======================

    @Override
    public void onMsg(KeyboardContext context, InputMsg msg) {
        switch (msg.type) {
            case Input_Choose_Doing: {
                InputList parentInputList = getParentInputList(context);

                Input input = msg.data().input;
                // 若所选中的输入属于上层输入列表，则由基类逻辑处理
                if (parentInputList.hasInput(input)) {
                    super.onMsg(context, msg);
                    return;
                }

                // 否则，视为选中的是算术输入列表中的输入，按算术输入逻辑处理
                InputList mathInputList = getMathInputList(context);

                mathInputList.confirmPendingAndSelect(input);
                // 对非括号输入的修改均做替换
                if (!isBracketInput(input)) {
                    mathInputList.newCharPending();
                }

                resetMathInputList(context);

                fire_Common_InputMsg(context, Input_Choose_Done, input);
                break;
            }
            case InputList_Clean_Done:
            case InputList_Cleaned_Cancel_Done: {
                play_SingleTick_InputAudio(context);

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

        Key key = msg.data().key;
        if (key instanceof CharKey || key instanceof MathOpKey) {
            on_Math_Input_Doing_MathKey_Msg(context, msg);
        }
    }

    // ===================== End: 消息处理 ======================

    // ===================== Start: 处理控制按键 ======================

    @Override
    protected boolean try_On_Common_CtrlKey_Msg(KeyboardContext context, UserKeyMsg msg) {
        if (on_Math_Input_Doing_CtrlKey_Msg(context, msg)) {
            return true;
        }
        return super.try_On_Common_CtrlKey_Msg(context, msg);
    }

    /** 响应算术输入的控制按键消息 */
    private boolean on_Math_Input_Doing_CtrlKey_Msg(KeyboardContext context, UserKeyMsg msg) {
        if (msg.type != UserKeyMsgType.SingleTap_Key) {
            return false;
        }

        CtrlKey key = context.key();
        InputList parentInputList = getParentInputList(context);

        switch (key.type) {
            case Backspace: {
                play_SingleTick_InputAudio(context);

                // Note: 若上层输入列表为空，则会对编辑器做删除，
                // 不需要处理对待输入的向前删除，而向前删除逻辑详见 #do_InputList_Backspacing
                backspace_InputList_or_Editor(context);
                return true;
            }
            case Commit_InputList: {
                play_SingleTick_InputAudio(context);

                // 提交父输入
                commit_InputList(context, true, false);

                // Note：在 X 型输入中仅需重置算术输入，而不需要退出当前键盘
                if (context.xInputPadEnabled) {
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

                resetMathInputList(context);
                return true;
            }
        }

        return false;
    }

    // ===================== End: 处理控制按键 ======================

    // ===================== Start: 处理按键输入 ======================

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
        Key key = context.key();
        InputList parentInputList = getParentInputList(context);

        InputList mathInputList = getMathInputList(context);
        if (key instanceof CharKey) {
            do_Single_MathCharKey_Inputting(mathInputList, (CharKey) key);
        }
        //
        else if (key instanceof MathOpKey) {
            do_Single_MathOpKey_Inputting(mathInputList, (MathOpKey) key);
        }

        // Note: 对于有非空待输入的 Gap，需提前确认其待输入，
        // 以保证该待输入，即，当前的算术输入，在上层视图上有实际的输入视图，而不是 Gap 的待输入视图，
        // 确保在输入过程中点击某个输入时，能够定位到目标输入上
        if (parentInputList.isGapSelected()) {
            // Note: 在待输入上的嵌套输入列表保持未确认，以确保与其映射的视图状态与其状态是一致的
            parentInputList.confirmPending(true);
        }

        // Note: 可能会新建待输入，故而，需重新取最新的
        CharInput pending = mathInputList.getCharPending();
        fire_InputChars_Input_Doing_in_TapMode(context, pending);
    }

    /** 处理字符输入 */
    private void do_Single_MathCharKey_Inputting(InputList mathInputList, CharKey key) {
        CharInput pending = mathInputList.getCharPending();

        // Note: 若待输入为括号，则不能在其上追加数字
        if (isBracketInput(pending)) {
            mathInputList.confirmPendingAndSelectNext();
        }

        // Note: 在选中输入时，非括号的待输入已被清空，故而，可直接添加新输入
        mathInputList.getCharPending().appendKey(key);
    }

    /** 处理运算符号的输入 */
    private void do_Single_MathOpKey_Inputting(InputList mathInputList, MathOpKey key) {
        switch (key.type) {
            // 针对以下输入，对当前的选中的数字输入做追加，而不是替换
            case Percent:
            case Permill:
            case Permyriad:
            case Brackets:
            case Dot: {
                prepare_for_Pending_Append_Inputting(mathInputList);
                break;
            }
        }

        switch (key.type) {
            case Dot: {
                CharInput pending = mathInputList.getCharPending();
                // 小数点只能出现在数字中，且只能出现一次
                if (CharInput.isNumber(pending) && !pending.hasAnyKey(key::equals)) {
                    pending.appendKey(key);
                }
                break;
            }
            case Brackets: {
                Input selected = mathInputList.getSelected();
                CharInput pending = mathInputList.getCharPending();
                // 对于正在输入的数字，先提交其待输入，再输入括号，以确保数字被括号包裹
                if (selected instanceof GapInput && CharInput.isNumber(pending)) {
                    mathInputList.confirmPending();
                }

                prepare_for_PairKey_Inputting(mathInputList,
                                              () -> MathKeyTable.bracketKey("("),
                                              () -> MathKeyTable.bracketKey(")"));
                break;
            }
            case Equal: {
                // 除开头以外的位置，等号始终添加到输入列表的末尾
                if (!mathInputList.isGapSelected() || mathInputList.getSelectedIndex() > 1) {
                    mathInputList.confirmPendingAndSelectLast();
                }
                // Note：等号需按 default 逻辑添加至输入列表末尾
                //break;
            }
            default: {
                mathInputList.confirmPendingAndSelectNext();

                mathInputList.getCharPending().appendKey(key);

                // Note：单算术符号不支持追加输入
                mathInputList.confirmPendingAndSelectNext();
            }
        }
    }

    // ===================== End: 处理按键输入 ======================

    // ===================== Start: 准备算术输入 ======================

    /** 进入算术输入状态 */
    private void start_Math_Inputting(KeyboardContext context) {
        InputList parentInputList = getParentInputList(context);

        Input selected = parentInputList.getSelected();
        Input pending = parentInputList.getPending();

        // 先提交从其他键盘切过来之前的待输入
        if (selected instanceof GapInput //
            && !Input.isEmpty(pending) //
            && !(pending instanceof MathExprInput) //
        ) {
            parentInputList.confirmPendingAndSelectNext();
        } else {
            // Note: 在待输入上的嵌套输入列表保持未确认，以确保与其映射的视图状态与其状态是一致的
            parentInputList.confirmPending(true);
        }

        resetMathInputList(context);
    }

    /** 结束算术输入 */
    private void stop_Math_Inputting(KeyboardContext context) {
        InputList parentInputList = getParentInputList(context);

        Input selected = parentInputList.getSelected();
        Input pending = parentInputList.getPending();

        // 在切换前，确保当前的算术输入列表已被确认
        // Note：新位置的待输入将被设置为普通输入，可接受非算术输入，故，不需要处理
        if (selected instanceof MathExprInput || pending instanceof MathExprInput) {
            parentInputList.confirmPendingAndSelectNext();
        }
    }

    /** 重置算术输入列表（已输入内容将保持不变） */
    private void resetMathInputList(KeyboardContext context) {
        InputList parentInputList = getParentInputList(context);

        Input selected = parentInputList.getSelected();
        Input pending = parentInputList.getPending();

        // 在父输入列表中绑定算术输入列表
        if (!(pending instanceof MathExprInput)) {
            if (selected instanceof MathExprInput) {
                parentInputList.newSelectedPending();
            } else {
                parentInputList.newMathExprPending();
            }
        }

        InputList mathInputList = getMathInputList(context);
        mathInputList.setInputOption(parentInputList.getInputOption());
    }

    // ===================== End: 准备算术输入 ======================

    // ===================== Start: 重载置空无关接口 =====================

    @Override
    protected void do_InputList_Pending_Completion_Creating(KeyboardContext context) {
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
            prepare_for_Pending_Append_Inputting(mathInputList);

            // Note: 该上下文仅用于做前向删除，不能覆盖 context 变量
            KeyboardContext mathContext = context.copy((b) -> b.inputList(mathInputList));
            super.do_InputList_Backspacing(mathContext);

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

        // 重置算术输入列表，以接受新的算术输入
        resetMathInputList(context);
    }

    /** 确保非运算符类型的待输入能够做追加输入（注：默认的被选中输入是做{@link #onMsg(KeyboardContext, InputMsg) 替换输入}） */
    private void prepare_for_Pending_Append_Inputting(InputList mathInputList) {
        Input selected = mathInputList.getSelected();
        CharInput pending = mathInputList.getCharPending();

        // 若待输入为空且当前选中输入是数字，则重新选中输入，以确保可在前向删除后，继续追加输入
        if (Input.isEmpty(pending) //
            && (selected instanceof GapInput //
                || CharInput.isNumber(selected)) //
        ) {
            mathInputList.confirmPendingAndSelect(selected, true);
        }
    }

    /** 指定输入是否为括号 */
    private boolean isBracketInput(Input input) {
        Key key = input instanceof CharInput ? ((CharInput) input).getFirstKey() : null;

        return MathOpKey.Type.Brackets.match(key);
    }
}
