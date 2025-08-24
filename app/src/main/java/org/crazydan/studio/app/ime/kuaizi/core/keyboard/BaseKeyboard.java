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

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import org.crazydan.studio.app.ime.kuaizi.common.Motion;
import org.crazydan.studio.app.ime.kuaizi.common.log.Logger;
import org.crazydan.studio.app.ime.kuaizi.common.widget.EditorAction;
import org.crazydan.studio.app.ime.kuaizi.core.Input;
import org.crazydan.studio.app.ime.kuaizi.core.InputList;
import org.crazydan.studio.app.ime.kuaizi.core.Key;
import org.crazydan.studio.app.ime.kuaizi.core.Keyboard;
import org.crazydan.studio.app.ime.kuaizi.core.KeyboardContext;
import org.crazydan.studio.app.ime.kuaizi.core.input.CharInput;
import org.crazydan.studio.app.ime.kuaizi.core.input.GapInput;
import org.crazydan.studio.app.ime.kuaizi.core.input.InputWord;
import org.crazydan.studio.app.ime.kuaizi.core.input.MathExprInput;
import org.crazydan.studio.app.ime.kuaizi.core.input.completion.InputCompletion;
import org.crazydan.studio.app.ime.kuaizi.core.input.completion.InputCompletions;
import org.crazydan.studio.app.ime.kuaizi.core.input.word.PinyinWord;
import org.crazydan.studio.app.ime.kuaizi.core.key.CharKey;
import org.crazydan.studio.app.ime.kuaizi.core.key.CtrlKey;
import org.crazydan.studio.app.ime.kuaizi.core.msg.InputMsg;
import org.crazydan.studio.app.ime.kuaizi.core.msg.InputMsgData;
import org.crazydan.studio.app.ime.kuaizi.core.msg.InputMsgType;
import org.crazydan.studio.app.ime.kuaizi.core.msg.UserKeyMsg;
import org.crazydan.studio.app.ime.kuaizi.core.msg.UserKeyMsgType;
import org.crazydan.studio.app.ime.kuaizi.core.msg.input.EditorCursorMsgData;
import org.crazydan.studio.app.ime.kuaizi.core.msg.input.EditorEditMsgData;
import org.crazydan.studio.app.ime.kuaizi.core.msg.input.InputAudioPlayMsgData;
import org.crazydan.studio.app.ime.kuaizi.core.msg.input.InputCharsInputMsgData;
import org.crazydan.studio.app.ime.kuaizi.core.msg.input.InputCharsInputPopupShowMsgData;
import org.crazydan.studio.app.ime.kuaizi.core.msg.input.InputListCommitMsgData;
import org.crazydan.studio.app.ime.kuaizi.core.msg.input.InputListPairSymbolCommitMsgData;
import org.crazydan.studio.app.ime.kuaizi.core.msg.input.KeyboardHandModeSwitchMsgData;
import org.crazydan.studio.app.ime.kuaizi.core.msg.input.KeyboardStateChangeMsgData;
import org.crazydan.studio.app.ime.kuaizi.core.msg.input.KeyboardSwitchMsgData;
import org.crazydan.studio.app.ime.kuaizi.core.msg.user.UserLongPressTickMsgData;
import org.crazydan.studio.app.ime.kuaizi.core.msg.user.UserSingleTapMsgData;
import org.crazydan.studio.app.ime.kuaizi.dict.UserInputData;
import org.crazydan.studio.app.ime.kuaizi.dict.UserInputDataDict;

import static org.crazydan.studio.app.ime.kuaizi.core.msg.InputMsgType.Editor_Cursor_Move_Doing;
import static org.crazydan.studio.app.ime.kuaizi.core.msg.InputMsgType.Editor_Edit_Doing;
import static org.crazydan.studio.app.ime.kuaizi.core.msg.InputMsgType.Editor_Range_Select_Doing;
import static org.crazydan.studio.app.ime.kuaizi.core.msg.InputMsgType.InputAudio_Play_Doing;
import static org.crazydan.studio.app.ime.kuaizi.core.msg.InputMsgType.InputChars_Input_Doing;
import static org.crazydan.studio.app.ime.kuaizi.core.msg.InputMsgType.InputChars_Input_Done;
import static org.crazydan.studio.app.ime.kuaizi.core.msg.InputMsgType.InputChars_Input_Popup_Hide_Doing;
import static org.crazydan.studio.app.ime.kuaizi.core.msg.InputMsgType.InputChars_Input_Popup_Show_Doing;
import static org.crazydan.studio.app.ime.kuaizi.core.msg.InputMsgType.InputCompletion_Create_Done;
import static org.crazydan.studio.app.ime.kuaizi.core.msg.InputMsgType.InputList_Commit_Doing;
import static org.crazydan.studio.app.ime.kuaizi.core.msg.InputMsgType.InputList_Committed_Revoke_Doing;
import static org.crazydan.studio.app.ime.kuaizi.core.msg.InputMsgType.InputList_PairSymbol_Commit_Doing;
import static org.crazydan.studio.app.ime.kuaizi.core.msg.InputMsgType.Input_Choose_Done;
import static org.crazydan.studio.app.ime.kuaizi.core.msg.InputMsgType.Input_Pending_Drop_Done;
import static org.crazydan.studio.app.ime.kuaizi.core.msg.InputMsgType.Input_Selected_Delete_Done;
import static org.crazydan.studio.app.ime.kuaizi.core.msg.InputMsgType.Keyboard_HandMode_Switch_Doing;
import static org.crazydan.studio.app.ime.kuaizi.core.msg.InputMsgType.Keyboard_State_Change_Done;
import static org.crazydan.studio.app.ime.kuaizi.core.msg.InputMsgType.Keyboard_Switch_Doing;
import static org.crazydan.studio.app.ime.kuaizi.core.msg.InputMsgType.Keyboard_XPad_Simulation_Terminated;

/**
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-06-28
 */
public abstract class BaseKeyboard implements Keyboard {
    protected final Logger log = Logger.getLogger(getClass());

    protected State state = new State(State.Type.InputChars_Input_Wait_Doing);

    /** 获取键盘初始状态，即，{@link State.Type#InputChars_Input_Wait_Doing 待输入}状态 */
    protected State getInitState() {
        if (this.state.type == State.Type.InputChars_Input_Wait_Doing) {
            return createInitState();
        }
        return new State(State.Type.InputChars_Input_Wait_Doing, this.state);
    }

    protected State createInitState() {
        return new State(State.Type.InputChars_Input_Wait_Doing);
    }

    @Override
    public void start(KeyboardContext context) {
    }

    @Override
    public void reset(KeyboardContext context) {
        if (isMaster()) {
            change_State_to_Init(context);
        }
        // 对于非主键盘，复位的结果便是重启
        else {
            start(context);
        }
    }

    @Override
    public void stop(KeyboardContext context) {
    }

    protected KeyTableConfig createKeyTableConfig(KeyboardContext context) {
        return new KeyTableConfig(context);
    }

    // ====================== Start: 对 InputMsg 的处理 ======================

    @Override
    public void onMsg(KeyboardContext context, InputMsg msg) {
        switch (msg.type) {
            case Input_Choose_Doing: {
                choose_InputList_Input(context, msg.data().input);
                break;
            }
            case InputList_Clean_Done: {
                play_SingleTick_InputAudio(context);

                change_State_to_Init(context);
                break;
            }
            case InputList_Cleaned_Cancel_Done: {
                play_SingleTick_InputAudio(context);

                InputList inputList = context.inputList;
                // 重新选中清空输入列表前的已选中输入，且对于 Gap 待输入不做确认
                Input input = inputList.getSelected();
                choose_InputList_Input(context, input, false);
                break;
            }
        }
    }

    // ====================== End: 对 InputMsg 的处理 ======================

    // ======================== Start: 对 UserKeyMsg 的通用处理 ========================

    /**
     * 尝试对 {@link UserKeyMsg} 做处理
     * <p/>
     * 注意，对于已禁用的按键一般不在该方法内处理，直接返回 false
     *
     * @return 若返回 true，则表示消息已处理，否则，返回 false
     */
    protected boolean try_On_Common_UserKey_Msg(KeyboardContext context, UserKeyMsg msg) {
        Key key = context.key();
        if (key == null) {
            return true;
        }

        // Note: NoOp 控制按键上的消息不能忽略，滑屏输入和翻页等状态下会涉及该类控制按键的消息处理
        if (key instanceof CtrlKey //
            && try_On_Common_CtrlKey_Msg(context, msg) //
        ) {
            return true;
        }

        return try_On_UserKey_Msg_Over_XPad(context, msg);
    }

    /**
     * 是否禁用在 {@link CtrlKey.Type#Commit_InputList} 按键上的消息
     * <p/>
     * 一般用于禁止切换到 {@link Type#InputList_Commit_Option} 键盘上，
     * 忽略 {@link UserKeyMsgType#LongPress_Key_Start} 消息即可
     */
    protected boolean disable_Msg_On_CtrlKey_Commit_InputList(UserKeyMsg msg) {return false;}

    /** 尝试处理控制按键消息：已禁用的不做处理 */
    protected boolean try_On_Common_CtrlKey_Msg(KeyboardContext context, UserKeyMsg msg) {
        CtrlKey key = context.key();
        if (key.disabled) {
            return false;
        }

        if (key.type == CtrlKey.Type.Commit_InputList //
            && disable_Msg_On_CtrlKey_Commit_InputList(msg)) {
            return true;
        }

        switch (msg.type) {
            case LongPress_Key_Tick: {
                switch (key.type) {
                    case Backspace:
                    case Space:
                    case Enter:
                        return try_On_LongPress_Tick_as_SingleTap_Msg(context, msg, this::try_On_Common_CtrlKey_Msg);
                }
                break;
            }
            case LongPress_Key_Start: {
                switch (key.type) {
                    // 长按提交按键，均进入提交选项键盘
                    case Commit_InputList: {
                        play_DoubleTick_InputAudio(context);

                        switch_Keyboard_To(context, Type.InputList_Commit_Option);
                        return true;
                    }
                }
                break;
            }
            case SingleTap_Key: {
                if (try_On_Common_CtrlKey_for_SingleTap_Msg(context, msg)) {
                    return true;
                }
                break;
            }
        }

        return false;
    }

    /** 处理控制按键上的 {@link UserKeyMsgType#SingleTap_Key} 消息 */
    protected boolean try_On_Common_CtrlKey_for_SingleTap_Msg(KeyboardContext context, UserKeyMsg msg) {
        CtrlKey key = context.key();

        switch (key.type) {
            // Note：在任意子键盘中提交输入，都需直接回到初始键盘
            case Commit_InputList: {
                play_SingleTick_InputAudio(context);
                commit_InputList_and_Goto_Init_State(context);
                return true;
            }
            case DropInput: {
                play_SingleTick_InputAudio(context);
                delete_InputList_Selected(context);

                change_State_to_Init(context);
                return true;
            }
            case RevokeInput: {
                play_SingleTick_InputAudio(context);
                revoke_Committed_InputList(context);
                return true;
            }
            case Backspace: {
                play_SingleTick_InputAudio(context);

                backspace_InputList_or_Editor(context);
                return true;
            }
            case Space:
            case Enter: {
                play_SingleTick_InputAudio(context);
                show_InputChars_Input_Popup(context);

                confirm_InputList_Input_Enter_or_Space(context);
                return true;
            }
            case Exit: {
                play_SingleTick_InputAudio(context);
                exit_Keyboard(context);
                return true;
            }
            case Switch_HandMode: {
                play_SingleTick_InputAudio(context);
                switch_HandMode(context);
                return true;
            }
            case Switch_Keyboard: {
                play_SingleTick_InputAudio(context);

                CtrlKey.Option<Keyboard.Type> option = key.option();
                switch_Keyboard_To(context, option.value);
                return true;
            }
        }

        return false;
    }

    /**
     * 将 {@link UserKeyMsgType#LongPress_Key_Tick} 转换为 {@link UserKeyMsgType#SingleTap_Key} 消息后再做处理
     * <p/>
     * 也就是将长按 tick 视为连续单击
     */
    protected <T> T try_On_LongPress_Tick_as_SingleTap_Msg(
            KeyboardContext context, UserKeyMsg msg, BiFunction<KeyboardContext, UserKeyMsg, T> c
    ) {
        UserLongPressTickMsgData msgData = msg.data();
        UserSingleTapMsgData newMsgData = new UserSingleTapMsgData(msgData.key, msgData.at, 0);
        UserKeyMsg newMsg = UserKeyMsg.build((b) -> b.type(UserKeyMsgType.SingleTap_Key).data(newMsgData));

        return c.apply(context, newMsg);
    }

    /** @see #try_On_LongPress_Tick_as_SingleTap_Msg(KeyboardContext, UserKeyMsg, BiFunction) */
    protected void try_On_LongPress_Tick_as_SingleTap_Msg(
            KeyboardContext context, UserKeyMsg msg, BiConsumer<KeyboardContext, UserKeyMsg> c
    ) {
        try_On_LongPress_Tick_as_SingleTap_Msg(context, msg, (ctx, m) -> {
            c.accept(ctx, m);
            return null;
        });
    }

    /** 在 X 型输入中的非控制按键或已禁用的按键不做处理 */
    private boolean try_On_UserKey_Msg_Over_XPad(KeyboardContext context, UserKeyMsg msg) {
        Key key = context.key();
        if (!context.xInputPadEnabled || !(key instanceof CtrlKey) || key.disabled) {
            return false;
        }

        switch (msg.type) {
            case Press_Key_Stop: {
                if (CtrlKey.Type.XPad_Simulation_Terminated.match(key)) {
                    fire_InputMsg(context, Keyboard_XPad_Simulation_Terminated, new InputMsgData());
                    return true;
                } else {
                    break;
                }
            }
            case FingerMoving: {
                // 播放输入分区激活和待输入按键切换的提示音
                switch (((CtrlKey) key).type) {
                    case XPad_Active_Block: {
                        play_PingTick_InputAudio(context);
                        return true;
                    }
                    case XPad_Char_Key: {
                        play_ClockTick_InputAudio(context);
                        return true;
                    }
                }
                break;
            }
        }

        return false;
    }

    // ======================== End: 对 UserKeyMsg 的通用处理 ========================

    // ======================== Start: 编辑器处理逻辑 ========================

    /** 对目标编辑器做粘贴、复制等操作 */
    protected void do_Editor_Editing(KeyboardContext context, EditorAction action) {
        InputMsgData data = new EditorEditMsgData(action);

        fire_InputMsg(context, Editor_Edit_Doing, data);
    }

    /** 对目标编辑器做光标移动操作 */
    protected void do_Editor_Cursor_Moving(KeyboardContext context, Motion anchor) {
        Key key = context.key();
        InputMsgData data = new EditorCursorMsgData(key, anchor);

        fire_InputMsg(context, Editor_Cursor_Move_Doing, data);
    }

    /** 对目标编辑器做内容选择操作 */
    protected void do_Editor_Range_Selecting(KeyboardContext context, Motion anchor) {
        Key key = context.key();
        InputMsgData data = new EditorCursorMsgData(key, anchor);

        fire_InputMsg(context, Editor_Range_Select_Doing, data);
    }

    // ======================== End: 编辑器处理逻辑 ========================

    // ======================== Start: 单字符输入 ========================

    /**
     * 单字符输入处理
     * <p/>
     * 对于有替代字符的按键，根据连续点击次数确定替代字符并替换前序按键字符
     */
    protected void start_Single_CharKey_Inputting(
            KeyboardContext context, UserSingleTapMsgData data, boolean directInputting
    ) {
        play_SingleTick_InputAudio(context);
        show_InputChars_Input_Popup(context);

        CharKey key = context.key();
        if (key.hasReplacement() && data.tick > 0) {
            do_Single_CharKey_Replacement_Inputting(context, data.tick, directInputting);
        } else {
            do_Single_CharKey_Inputting(context, directInputting);
        }
    }

    /** 单一字符按键输入 */
    protected void do_Single_CharKey_Inputting(KeyboardContext context, boolean directInputting) {
        if (directInputting) {
            commit_InputList_with_SingleKey_Only(context, false);
            return;
        }

        CharKey key = context.key();
        InputList inputList = context.inputList;

        // Note: 此处不涉及配对符号的输入，故始终清空配对符号的绑定
        inputList.clearPairOnSelected();

        switch (key.type) {
            // 若为标点、表情符号，则直接确认输入，不支持连续输入其他字符
            case Emoji:
            case Symbol: {
                do_Single_CharKey_Replace_or_NewPending_Inputting(context);
                break;
            }
            // 字母、数字可连续输入
            case Number:
            case Alphabet: {
                Input pending = inputList.getPending();
                // Note：非拉丁字符输入不可连续输入，直接对其做替换
                if (!CharInput.isLatin(pending)) {
                    pending = inputList.newCharPending();
                }
                ((CharInput) pending).appendKey(key);

                fire_InputChars_Input_Doing_in_TapMode(context, pending);
                do_InputList_Pending_Completion_Creating(context);
                break;
            }
        }
    }

    /** 处理单字符按键的 替换输入 */
    protected void do_Single_CharKey_Replacement_Inputting(
            KeyboardContext context, int replacementIndex, boolean directInputting
    ) {
        if (directInputting) {
            do_Single_CharKey_Replacement_Committing(context, replacementIndex);
            return;
        }

        CharKey key = context.key();
        InputList inputList = context.inputList;

        CharInput input = inputList.getCharPending();
        if (CharKey.Type.Symbol.match(key)) {
            // Note：标点符号是独立输入，故，需替换当前位置的前一个标点符号输入（当前输入必然为 Gap）
            input = (CharInput) inputList.getInputBeforeSelected();

            // 对输入列表为空时的标点符号直输输入进行替换
            if (input == null) {
                do_Single_CharKey_Replacement_Committing(context, replacementIndex);
                return;
            }
        }

        Key lastKey = input.getLastKey();
        if (!key.canReplaceTheKey(lastKey)) {
            // 转为单字符输入
            do_Single_CharKey_Inputting(context, false);
            return;
        }

        CharKey lastCharKey = (CharKey) lastKey;
        // Note: 在 Input 中的按键可能不携带 replacement 信息，只能通过当前按键做判断
        String newKeyText = key.nextReplacement(lastCharKey.value);
        CharKey newKey = key.createReplacementKey(newKeyText);

        input.replaceLatestKey(lastCharKey, newKey);

        context = context.copy((b) -> b.key(newKey));

        show_InputChars_Input_Popup(context);

        fire_InputChars_Input_Doing_in_TapMode(context, input);
        do_InputList_Pending_Completion_Creating(context);
    }

    /** 提交单字符按键的替换输入：对编辑框内的输入字符做输入替换 */
    protected void do_Single_CharKey_Replacement_Committing(KeyboardContext context, int replacementIndex) {
        CharKey key = context.key();
        CharKey newKey = key.createReplacementKey(replacementIndex);

        context = context.copy((b) -> b.key(newKey));

        show_InputChars_Input_Popup(context);
        commit_InputList_with_SingleKey_Only(context, true);
    }

    /**
     * 当前输入做单一字符替换，或者在新的待输入中录入该单一字符
     * <p/>
     * 主要针对符号和表情输入，在当前输入为 Gap 时，先提交其待输入，
     * 再在新的待输入中录入当前按键，而对于已存在的输入，则直接将其替换为当前按键
     */
    protected void do_Single_CharKey_Replace_or_NewPending_Inputting(KeyboardContext context) {
        confirm_or_New_InputList_Pending(context);

        confirm_InputList_Input_with_SingleKey_Only(context);
    }

    // ======================== End: 单字符输入 ========================

    // ======================== Start: 输入补全 ========================

    /** 查找以指定 text 开头的最靠前的 top 个拉丁文 */
    protected List<String> getTopBestMatchedLatins(KeyboardContext context, String text) {
        return List.of();
    }

    /** 更新待输入的输入补全 */
    protected void do_InputList_Pending_Completion_Creating(KeyboardContext context) {
        InputList inputList = context.inputList;

        CharInput pending = inputList.getCharPending();
        if (Input.isEmpty(pending) || !CharInput.isLatin(pending)) {
            return;
        }

        Input selected = inputList.getSelected();
        InputCompletions completions = inputList.newLatinCompletions(selected);

        String text = pending.getText().toString();
        getTopBestMatchedLatins(context, text).forEach((latin) -> {
            // Note: 对于拉丁文输入的补全，采用逐个字符构建，
            // 以支持在应用补全后，仍然可以从输入中逐个删除
            List<Key> keys = CharKey.from(latin);
            if (keys.isEmpty()) {
                return;
            }

            CharInput input = CharInput.from(keys);
            InputCompletion completion = new InputCompletion();
            completion.inputs.add(input);

            completions.add(completion);
        });

        fire_Input_Completion_Create_Done(context);
    }

    // ======================== End: 输入补全 ========================

    // ======================== Start: 操作输入列表 ========================

    /** 选中输入列表中的已选择输入，用于再次触发对输入的 {@link #choose_InputList_Input 选中处理} */
    protected void choose_InputList_Selected_Input(KeyboardContext context) {
        InputList inputList = context.inputList;
        Input selected = inputList.getSelected();

        choose_InputList_Input(context, selected);
    }

    /** @see #choose_InputList_Input(KeyboardContext, Input, boolean) */
    protected void choose_InputList_Input(KeyboardContext context, Input input) {
        choose_InputList_Input(context, input, true);
    }

    /** 选中输入列表中的指定输入，一般切换到该输入所对应的 {@link Keyboard} 上 */
    protected void choose_InputList_Input(KeyboardContext context, Input input, boolean confirmGapPending) {
        InputList inputList = context.inputList;
        inputList.select(input);

        // Note：输入过程中操作和处理的都是 pending
        Input pending = inputList.getPending();

        if (pending instanceof MathExprInput) {
            switch_Keyboard_To(context, Type.Math);
        } //
        else if (CharInput.isEmoji(pending)) {
            switch_Keyboard_To(context, Type.Emoji);
        } //
        else if (CharInput.isSymbol(pending)) {
            switch_Keyboard_To(context, Type.Symbol);
        } //
        else if (CharInput.isPinyin(pending)) {
            switch_Keyboard_To(context, Type.Pinyin_Candidate);
        } //
        else {
            // 在选择输入时，对于新输入，需先确认其待输入
            if (confirmGapPending // Note: 在输入列表清空操作被取消后，需保持 Gap 待输入不变
                && input instanceof GapInput //
                && !Input.isEmpty(pending) //
            ) {
                confirm_InputList_Pending(context);
            }
            change_State_to_Init(context);

            fire_Common_InputMsg(context, Input_Choose_Done, input);
        }
    }

    /** 确认待输入，并触发 {@link InputMsgType#InputChars_Input_Done} 消息 */
    protected void confirm_InputList_Pending(KeyboardContext context) {
        InputList inputList = context.inputList;
        Input pending = inputList.getPending();

        inputList.confirmPendingAndSelectNext();

        fire_InputChars_Input_Done(context, pending);
    }

    /**
     * 若当前为 Gap 输入或是已修改的输入，则先确认其待输入，再做新增，
     * 否则，则直接新建待输入以做替换输入
     */
    protected void confirm_or_New_InputList_Pending(KeyboardContext context) {
        InputList inputList = context.inputList;

        // 先确认新增的或已修改的输入，再新增
        if (inputList.isGapSelected() //
            || inputList.hasChangedPending() //
        ) {
            inputList.confirmPendingAndSelectNext();
        }
        // 否则，直接对其做替换
        else {
            inputList.newCharPending();
        }
    }

    /** 仅{@link #confirm_InputList_Pending 确认}只有唯一按键的输入 */
    protected void confirm_InputList_Input_with_SingleKey_Only(KeyboardContext context) {
        Key key = context.key();
        InputList inputList = context.inputList;

        inputList.newCharPending().appendKey(key);

        confirm_InputList_Pending(context);
    }

    /**
     * 确认回车或空格的控制按键输入
     * <p/>
     * 若输入列表为空或{@link InputList#isFrozen() 已被冻结}，
     * 则换行和空格均直接输入，否则，仅将空格附加到输入列表中
     */
    protected void confirm_InputList_Input_Enter_or_Space(KeyboardContext context) {
        CtrlKey key = context.key();
        InputList inputList = context.inputList;
        boolean isDirectInputting = inputList.isFrozen() || inputList.isEmpty();

        if (isDirectInputting) {
            switch (key.type) {
                case Enter:
                case Space:
                    // Note：对于直输的回车和空格，不支持输入撤回
                    fire_InputList_Commit_Doing(context, key.value, null, false);
                    break;
            }
        }
        // 输入列表不为空且按键为空格按键时，将其添加到输入列表中
        else if (CtrlKey.Type.Space.match(key)) {
            // Note：空格不替换当前输入
            inputList.confirmPendingAndSelectNext();

            confirm_InputList_Input_with_SingleKey_Only(context);
        }
    }

    /** 删除已选中的输入 */
    protected void delete_InputList_Selected(KeyboardContext context) {
        InputList inputList = context.inputList;
        Input input = inputList.getSelected();

        inputList.deleteSelected();

        after_InputList_Selected_Deleted(context);

        fire_Common_InputMsg(context, Input_Selected_Delete_Done, input);
    }

    /**
     * {@link #delete_InputList_Selected 选中输入已删除}后的处理
     * <p/>
     * 可在该接口内做输入预测等，且不需要触发 {@link InputMsg} 消息，
     * 在该接口之后会触发 {@link InputMsgType#Input_Selected_Delete_Done} 消息
     */
    protected void after_InputList_Selected_Deleted(KeyboardContext context) {}

    /** 删除待输入 */
    protected void drop_InputList_Pending(KeyboardContext context) {
        InputList inputList = context.inputList;
        Input pending = inputList.getPending();

        inputList.dropPending();

        fire_Common_InputMsg(context, Input_Pending_Drop_Done, pending);
    }

    /**
     * {@link #commit_InputList 提交输入列表（可撤销）}，
     * 并{@link #change_State_to_Init 进入初始状态}
     */
    protected void commit_InputList_and_Goto_Init_State(KeyboardContext context) {
        InputList inputList = context.inputList;
        // Note: 输入列表提交前，强制解除冻结，以确保输入列表可被重置并记录撤回状态数据
        inputList.freeze(false);

        // 在仅有唯一的配对输入时，采用配对输入方式提交，且不可被撤回
        if (inputList.hasOnlyOnePairInputs()) {
            commit_InputList(context, false, false, true);
        } else {
            commit_InputList(context, true, false);
        }

        change_State_to_Init(context);
    }

    /** 提交单一按键输入，且该提交不可被撤销 */
    protected void commit_InputList_with_SingleKey_Only(KeyboardContext context, boolean needToBeReplaced) {
        Key key = context.key();
        InputList inputList = context.inputList;

        inputList.newCharPending().appendKey(key);

        commit_InputList(context, false, needToBeReplaced);
    }

    /** 提交输入列表 */
    protected void commit_InputList(KeyboardContext context, boolean canBeRevoked, boolean needToBeReplaced) {
        commit_InputList(context, canBeRevoked, needToBeReplaced, false);
    }

    /** 提交输入列表 */
    protected void commit_InputList(
            KeyboardContext context, boolean canBeRevoked, boolean needToBeReplaced, boolean isPairSymbol
    ) {
        InputList inputList = context.inputList;
        inputList.confirmPending();

        if (inputList.isEmpty()) {
            return;
        }

        before_Commit_InputList(context);

        if (isPairSymbol) {
            CharInput left = inputList.getFirstCharInput();
            CharInput right = inputList.getLastCharInput();

            fire_InputList_PairSymbol_Commit_Doing(context, left.getText(), right.getText());
        } else {
            List<String> replacements = null;
            if (needToBeReplaced) {
                CharInput input = inputList.getLastCharInput();
                CharKey key = (CharKey) input.getLastKey();

                replacements = key.replacements;
            }

            StringBuilder text = inputList.getText();

            fire_InputList_Commit_Doing(context, text, replacements, canBeRevoked);
        }
    }

    /** 撤回输入列表，且状态保持不变 */
    protected void revoke_Committed_InputList(KeyboardContext context) {
        if (!context.hasRevokableInputsCommit) {
            return;
        }

        fire_Common_InputMsg(context, InputList_Committed_Revoke_Doing);

        after_Revoke_Committed_InputList(context);

        choose_InputList_Selected_Input(context);
    }

    /**
     * 回删输入列表中的输入或 目标编辑器 的内容
     * <p/>
     * 输入列表不为空且{@link InputList#isFrozen() 未被冻结}时，
     * 在输入列表中做删除，否则，在 目标编辑器 做删除
     */
    protected void backspace_InputList_or_Editor(KeyboardContext context) {
        InputList inputList = context.inputList;

        if (!inputList.isFrozen() && !inputList.isEmpty()) {
            show_InputChars_Input_Popup(context);

            do_InputList_Backspacing(context);
        } else {
            do_Editor_Backspacing(context);
        }
    }

    /** 回删 目标编辑器 的内容 */
    protected void do_Editor_Backspacing(KeyboardContext context) {
        do_Editor_Editing(context, EditorAction.backspace);
    }

    /** 在 {@link #commit_InputList} 之前需要做的事情 */
    protected void before_Commit_InputList(KeyboardContext context) {
        handle_UserInput_Data(context, (data) -> {
            UserInputDataDict dict = context.dict.useUserInputDataDict();
            dict.save(data);
        });
    }

    /** 在 {@link #revoke_Committed_InputList} 之后需要做的事情 */
    protected void after_Revoke_Committed_InputList(KeyboardContext context) {
        handle_UserInput_Data(context, (data) -> {
            UserInputDataDict dict = context.dict.useUserInputDataDict();
            dict.revokeSave(data);
        });
    }

    /** 回删输入列表中的输入内容 */
    protected void do_InputList_Backspacing(KeyboardContext context) {
        InputList inputList = context.inputList;
        inputList.deleteBackward();

        after_InputList_Backspacing(context);

        fire_InputChars_Input_Doing_in_TapMode(context, null);

        do_InputList_Pending_Completion_Creating(context);
    }

    /**
     * 回删输入列表的输入内容之后的操作
     * <p/>
     * 可在该接口内做输入预测等，且不需要触发 {@link InputMsg} 消息，
     * 在该接口之后会触发 {@link #fire_InputChars_Input_Doing_in_TapMode 输入消息}
     */
    protected void after_InputList_Backspacing(KeyboardContext context) {}

    /**
     * 在未{@link KeyboardContext#userInputDataDisabled 禁用}对用户输入数据的保存的情况下，
     * 调用 <code>consumer</code> 对{@link UserInputData 用户输入数据}做处理
     */
    protected void handle_UserInput_Data(KeyboardContext context, Consumer<UserInputData> consumer) {
        if (context.userInputDataDisabled) {
            return;
        }

        InputList inputList = context.inputList;

        List<List<PinyinWord>> phrases = inputList.getPinyinPhraseWords();
        List<InputWord> emojis = inputList.getEmojis();
        List<String> latins = inputList.getLatins();

        UserInputData data = new UserInputData(phrases, emojis, latins);
        consumer.accept(data);
    }

    // ======================== End: 操作输入列表 ========================

    // ====================== Start: 触发 InputMsg 消息 ======================

    /** 触发 {@link InputMsg} 消息 */
    protected void fire_InputMsg(KeyboardContext context, InputMsgType msgType, InputMsgData msgData) {
        context.fireInputMsg(msgType, msgData);
    }

    protected void fire_Common_InputMsg(KeyboardContext context, InputMsgType msgType) {
        fire_Common_InputMsg(context, msgType, null);
    }

    protected void fire_Common_InputMsg(KeyboardContext context, InputMsgType msgType, Input input) {
        Key key = context.key();
        context.fireInputMsg(msgType, key, input);
    }

    /** 触发 {@link InputMsgType#InputChars_Input_Doing} 消息 */
    protected void fire_InputChars_Input_Doing_in_TapMode(KeyboardContext context, Input input) {
        fire_InputChars_Input_Doing(context, input, InputCharsInputMsgData.InputMode.tap);
    }

    /** 触发 {@link InputMsgType#InputChars_Input_Doing} 消息 */
    protected void fire_InputChars_Input_Doing(
            KeyboardContext context, Input input, InputCharsInputMsgData.InputMode inputMode
    ) {
        Key key = context.key();
        InputMsgData data = new InputCharsInputMsgData(key, input, inputMode);

        fire_InputMsg(context, InputChars_Input_Doing, data);
    }

    /** 触发 {@link InputMsgType#InputChars_Input_Done} 消息 */
    protected void fire_InputChars_Input_Done(KeyboardContext context, Input input) {
        Key key = context.key();
        InputMsgData data = new InputCharsInputMsgData(key, input);

        fire_InputMsg(context, InputChars_Input_Done, data);
    }

    /** 触发 {@link InputMsgType#InputCompletion_Create_Done} 消息 */
    protected void fire_Input_Completion_Create_Done(KeyboardContext context) {
        fire_Common_InputMsg(context, InputCompletion_Create_Done);
    }

    /** 触发 {@link InputMsgType#InputList_Commit_Doing} 消息 */
    protected void fire_InputList_Commit_Doing(
            KeyboardContext context, CharSequence text, List<String> replacements, boolean canBeRevoked
    ) {
        Key key = context.key();
        InputMsgData data = new InputListCommitMsgData(key, text, replacements, canBeRevoked);

        fire_InputMsg(context, InputList_Commit_Doing, data);
    }

    /** 触发 {@link InputMsgType#InputList_PairSymbol_Commit_Doing} 消息 */
    protected void fire_InputList_PairSymbol_Commit_Doing(
            KeyboardContext context, CharSequence left, CharSequence right
    ) {
        InputMsgData data = new InputListPairSymbolCommitMsgData(left, right);

        fire_InputMsg(context, InputList_PairSymbol_Commit_Doing, data);
    }

    /** 触发 {@link InputMsgType#InputAudio_Play_Doing} 消息 */
    private void fire_InputAudio_Play_Doing(KeyboardContext context, InputAudioPlayMsgData.AudioType audioType) {
        fire_InputAudio_Play_Doing(context, audioType, false);
    }

    private void fire_InputAudio_Play_Doing(
            KeyboardContext context, InputAudioPlayMsgData.AudioType audioType, boolean force
    ) {
        Key key = context.key();
        if (!force //
            && CtrlKey.Type.NoOp.match(key) //
            && audioType != InputAudioPlayMsgData.AudioType.PageFlip //
        ) {
            return;
        }

        InputMsgData data = new InputAudioPlayMsgData(key, audioType);
        fire_InputMsg(context, InputAudio_Play_Doing, data);
    }

    // ====================== End: 触发 InputMsg 消息 ======================

    /** 状态回到{@link State.Type#InputChars_Input_Wait_Doing 待输入} */
    protected void change_State_to_Init(KeyboardContext context) {
        // Note: 仅主键盘才具备初始状态，而其余键盘的初始态就是回到切换前的主键盘
        if (isMaster()) {
            change_State_To(context, getInitState());
        } else {
            exit_Keyboard(context);
        }
    }

    protected void change_State_To(KeyboardContext context, State state) {
        assert state != null;

        this.state = state;

        Key key = context.key();
        InputMsgData data = new KeyboardStateChangeMsgData(key, state);
        fire_InputMsg(context, Keyboard_State_Change_Done, data);
    }

    /**
     * 回到前序状态
     * <p/>
     * 若无前序状态，则回到初始状态
     */
    protected void change_State_to_Previous(KeyboardContext context) {
        State state = this.state;
        State previous = state.previous;

        // 跳过与当前状态的类型相同的前序状态
        while (previous != null && previous.type == state.type) {
            state = previous;
            previous = state.previous;
        }

        if (previous == null) {
            change_State_to_Init(context);
        } else {
            change_State_To(context, previous);
        }
    }

    /** 切换到先前的键盘，也就是从哪个键盘切过来的，就切回到哪个键盘 */
    protected void switch_Keyboard_to_Previous(KeyboardContext context) {
        switch_Keyboard_To(context, null);
    }

    /** 切换到指定类型的键盘 */
    protected void switch_Keyboard_To(KeyboardContext context, Type type) {
        Key key = context.key();
        InputMsgData data = new KeyboardSwitchMsgData(key, type);

        fire_InputMsg(context, Keyboard_Switch_Doing, data);
    }

    /** 退出并返回到原状态或前序键盘 */
    protected void exit_Keyboard(KeyboardContext context) {
        if (this.state.previous == null) {
            switch_Keyboard_to_Previous(context);
        } else {
            change_State_to_Previous(context);
        }
    }

    /** 切换键盘的左右手模式 */
    protected void switch_HandMode(KeyboardContext context) {
        HandMode mode = context.keyboardHandMode;
        switch (mode) {
            case left:
                mode = Keyboard.HandMode.right;
                break;
            case right:
                mode = Keyboard.HandMode.left;
                break;
        }

        Key key = context.key();
        InputMsgData data = new KeyboardHandModeSwitchMsgData(key, mode);
        fire_InputMsg(context, Keyboard_HandMode_Switch_Doing, data);
    }

    /** 显示输入提示气泡 */
    protected void show_InputChars_Input_Popup(KeyboardContext context) {
        show_InputChars_Input_Popup(context, true);
    }

    /**
     * 显示输入提示气泡
     *
     * @param hideDelayed
     *         是否延迟隐藏，若为 false，则不自动隐藏气泡
     */
    protected void show_InputChars_Input_Popup(KeyboardContext context, boolean hideDelayed) {
        Key key = context.key();
        String text = key != null ? key.label : null;

        show_InputChars_Input_Popup(context, text, hideDelayed);
    }

    /**
     * 显示输入提示气泡
     *
     * @param text
     *         气泡显示内容
     * @param hideDelayed
     *         是否延迟隐藏，若为 false，则不自动隐藏气泡
     */
    protected void show_InputChars_Input_Popup(KeyboardContext context, String text, boolean hideDelayed) {
        InputMsgData data = new InputCharsInputPopupShowMsgData(text, hideDelayed);

        fire_InputMsg(context, InputChars_Input_Popup_Show_Doing, data);
    }

    /** 隐藏输入提示气泡 */
    protected void hide_InputChars_Input_Popup(KeyboardContext context) {
        InputMsgData data = new InputMsgData();

        fire_InputMsg(context, InputChars_Input_Popup_Hide_Doing, data);
    }

    /** 播放输入单击音效 */
    protected void play_SingleTick_InputAudio(KeyboardContext context) {
        play_SingleTick_InputAudio(context, false);
    }

    /** 播放输入单击音效 */
    protected void play_SingleTick_InputAudio(KeyboardContext context, boolean force) {
        fire_InputAudio_Play_Doing(context, InputAudioPlayMsgData.AudioType.SingleTick, force);
    }

    /** 播放输入双击音效 */
    protected void play_DoubleTick_InputAudio(KeyboardContext context) {
        fire_InputAudio_Play_Doing(context, InputAudioPlayMsgData.AudioType.DoubleTick);
    }

    /** 播放时钟走时音效 */
    protected void play_ClockTick_InputAudio(KeyboardContext context) {
        fire_InputAudio_Play_Doing(context, InputAudioPlayMsgData.AudioType.ClockTick);
    }

    /** 播放敲击音效 */
    protected void play_PingTick_InputAudio(KeyboardContext context) {
        fire_InputAudio_Play_Doing(context, InputAudioPlayMsgData.AudioType.PingTick);
    }

    /** 播放输入翻页音效 */
    protected void play_PageFlip_InputAudio(KeyboardContext context) {
        fire_InputAudio_Play_Doing(context, InputAudioPlayMsgData.AudioType.PageFlip);
    }
}
