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

import java.util.List;
import java.util.function.Consumer;

import org.crazydan.studio.app.ime.kuaizi.common.log.Logger;
import org.crazydan.studio.app.ime.kuaizi.common.widget.EditorAction;
import org.crazydan.studio.app.ime.kuaizi.dict.PinyinDict;
import org.crazydan.studio.app.ime.kuaizi.dict.UserInputData;
import org.crazydan.studio.app.ime.kuaizi.pane.Input;
import org.crazydan.studio.app.ime.kuaizi.pane.InputList;
import org.crazydan.studio.app.ime.kuaizi.pane.InputWord;
import org.crazydan.studio.app.ime.kuaizi.pane.Key;
import org.crazydan.studio.app.ime.kuaizi.pane.Keyboard;
import org.crazydan.studio.app.ime.kuaizi.pane.KeyboardConfig;
import org.crazydan.studio.app.ime.kuaizi.pane.KeyboardContext;
import org.crazydan.studio.app.ime.kuaizi.pane.input.CharInput;
import org.crazydan.studio.app.ime.kuaizi.pane.input.CompletionInput;
import org.crazydan.studio.app.ime.kuaizi.pane.input.PinyinWord;
import org.crazydan.studio.app.ime.kuaizi.pane.key.CharKey;
import org.crazydan.studio.app.ime.kuaizi.pane.key.CtrlKey;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.InputMsg;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.InputMsgData;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.InputMsgType;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.Motion;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.UserKeyMsg;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.UserKeyMsgType;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.input.EditorCursorMsgData;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.input.EditorEditMsgData;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.input.InputAudioPlayMsgData;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.input.InputCharsInputMsgData;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.input.InputCharsInputPopupShowMsgData;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.input.InputCompletionUpdateMsgData;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.input.InputListCommitMsgData;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.input.InputListPairSymbolCommitMsgData;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.input.KeyboardHandModeSwitchMsgData;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.input.KeyboardStateChangeMsgData;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.input.KeyboardSwitchMsgData;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.user.UserFingerFlippingMsgData;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.user.UserSingleTapMsgData;

import static org.crazydan.studio.app.ime.kuaizi.pane.msg.InputMsgType.Editor_Cursor_Move_Doing;
import static org.crazydan.studio.app.ime.kuaizi.pane.msg.InputMsgType.Editor_Edit_Doing;
import static org.crazydan.studio.app.ime.kuaizi.pane.msg.InputMsgType.IME_Switch_Doing;
import static org.crazydan.studio.app.ime.kuaizi.pane.msg.InputMsgType.InputAudio_Play_Doing;
import static org.crazydan.studio.app.ime.kuaizi.pane.msg.InputMsgType.InputChars_Input_Doing;
import static org.crazydan.studio.app.ime.kuaizi.pane.msg.InputMsgType.InputChars_Input_Done;
import static org.crazydan.studio.app.ime.kuaizi.pane.msg.InputMsgType.InputChars_Input_Popup_Hide_Doing;
import static org.crazydan.studio.app.ime.kuaizi.pane.msg.InputMsgType.InputChars_Input_Popup_Show_Doing;
import static org.crazydan.studio.app.ime.kuaizi.pane.msg.InputMsgType.InputList_Commit_Doing;
import static org.crazydan.studio.app.ime.kuaizi.pane.msg.InputMsgType.InputList_Committed_Revoke_Doing;
import static org.crazydan.studio.app.ime.kuaizi.pane.msg.InputMsgType.InputList_PairSymbol_Commit_Doing;
import static org.crazydan.studio.app.ime.kuaizi.pane.msg.InputMsgType.Input_Choose_Done;
import static org.crazydan.studio.app.ime.kuaizi.pane.msg.InputMsgType.Input_Completion_Update_Done;
import static org.crazydan.studio.app.ime.kuaizi.pane.msg.InputMsgType.Input_Pending_Drop_Done;
import static org.crazydan.studio.app.ime.kuaizi.pane.msg.InputMsgType.Input_Selected_Delete_Done;
import static org.crazydan.studio.app.ime.kuaizi.pane.msg.InputMsgType.Keyboard_HandMode_Switch_Doing;
import static org.crazydan.studio.app.ime.kuaizi.pane.msg.InputMsgType.Keyboard_State_Change_Done;
import static org.crazydan.studio.app.ime.kuaizi.pane.msg.InputMsgType.Keyboard_Switch_Doing;
import static org.crazydan.studio.app.ime.kuaizi.pane.msg.InputMsgType.Keyboard_XPad_Simulation_Terminated;

/**
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-06-28
 */
public abstract class BaseKeyboard implements Keyboard {
    protected final Logger log = Logger.getLogger(getClass());

    /**
     * 为确保在各键盘内提交输入均能对{@link #handle_UserInput_Data 用户输入做保存}，
     * 因此，需要在基类中统一调用字典的存储接口
     */
    protected final PinyinDict dict;

    protected State state = new State(State.Type.InputChars_Input_Wait_Doing);

    protected BaseKeyboard(PinyinDict dict) {
        this.dict = dict;
    }

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
        return KeyTableConfig.from(context.config, context.inputList);
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

                // 重新选中清空输入列表前的已选中输入
                choose_InputList_Selected_Input(context);
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
            this.log.debug("%s: No key for %s", msg.getClass().getSimpleName(), msg.type);
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

    /** 尝试处理控制按键消息：已禁用的不做处理 */
    protected boolean try_On_Common_CtrlKey_Msg(KeyboardContext context, UserKeyMsg msg) {
        CtrlKey key = context.key();
        if (key.isDisabled()) {
            return false;
        }

        switch (msg.type) {
            case LongPress_Key_Tick: {
                switch (key.getType()) {
                    case Backspace:
                    case Space:
                    case Enter:
                        // 长按 tick 视为连续单击
                        return try_On_Common_CtrlKey_Msg(context,
                                                         new UserKeyMsg(UserKeyMsgType.SingleTap_Key, msg.data()));
                }
                break;
            }
            case SingleTap_Key: {
                switch (key.getType()) {
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
                        show_InputChars_Input_Popup(context);

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
                    case Switch_IME: {
                        play_SingleTick_InputAudio(context);
                        switch_IME(context);
                        return true;
                    }
                    case Switch_HandMode: {
                        play_SingleTick_InputAudio(context);
                        switch_HandMode(context);
                        return true;
                    }
                    case Switch_Keyboard: {
                        play_SingleTick_InputAudio(context);

                        CtrlKey.KeyboardSwitchOption option = (CtrlKey.KeyboardSwitchOption) key.getOption();
                        switch_Keyboard_To(context, option.value());
                        return true;
                    }
                }
                break;
            }
        }

        // 处理定位按钮
        if (CtrlKey.is(key, CtrlKey.Type.Editor_Cursor_Locator)) {
            switch (msg.type) {
                case SingleTap_Key: {
                    // 为双击提前播放音效
                    play_SingleTick_InputAudio(context);
                    return true;
                }
                case LongPress_Key_Start: {
                    play_DoubleTick_InputAudio(context);
                    // 继续 DoubleTap_Key 的逻辑
                }
                case DoubleTap_Key: {
                    switch_Keyboard_To(context, Type.Editor);
                    return true;
                }
                case FingerFlipping: {
                    play_SingleTick_InputAudio(context);

                    // 在定位切换按钮上滑动也可以移动光标，但不修改键盘状态
                    UserFingerFlippingMsgData data = msg.data();
                    Motion motion = data.motion;

                    do_Editor_Cursor_Moving(context, motion);
                    return true;
                }
            }
        }

        return false;
    }

    /** 在 X 型输入中的非控制按键或已禁用的按键不做处理 */
    private boolean try_On_UserKey_Msg_Over_XPad(KeyboardContext context, UserKeyMsg msg) {
        Key key = context.key();
        if (!context.config.xInputPadEnabled || !(key instanceof CtrlKey) || key.isDisabled()) {
            return false;
        }

        switch (msg.type) {
            case Press_Key_Stop: {
                if (CtrlKey.is(key, CtrlKey.Type.XPad_Simulation_Terminated)) {
                    fire_InputMsg(context, Keyboard_XPad_Simulation_Terminated, new InputMsgData());
                    return true;
                } else {
                    break;
                }
            }
            case FingerMoving: {
                // 播放输入分区激活和待输入按键切换的提示音
                switch (((CtrlKey) key).getType()) {
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

    // ======================== Start: 文本编辑 ========================

    /** 回删输入列表中的输入内容 */
    protected void do_InputList_Backspacing(KeyboardContext context) {
        InputList inputList = context.inputList;
        inputList.deleteBackward();

        fire_InputChars_Input_Doing_in_TapMode(context, null);

        do_InputList_Pending_Completion_Updating(context);

        do_InputList_Current_Phrase_Completion_Updating(context);
    }

    /** 回删 目标编辑器 的内容 */
    protected void do_Editor_Backspacing(KeyboardContext context) {
        do_Editor_Editing(context, EditorAction.backspace);
    }

    protected void do_Editor_Editing(KeyboardContext context, EditorAction action) {
        InputList inputList = context.inputList;

        // 对编辑内容会造成修改的操作，需要清空待撤回输入数据
        if (EditorAction.hasEffect(action)) {
            inputList.clearCommitRevokes();
        }

        InputMsgData data = new EditorEditMsgData(action);

        fire_InputMsg(context, Editor_Edit_Doing, data);
    }

    protected void do_Editor_Cursor_Moving(KeyboardContext context, Motion motion) {
        Key key = context.key();
        InputMsgData data = new EditorCursorMsgData(key, motion);

        fire_InputMsg(context, Editor_Cursor_Move_Doing, data);
    }

    // ======================== End: 文本编辑逻辑 ========================

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

        InputList inputList = context.inputList;
        inputList.clearPhraseCompletions();

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

        switch (key.getType()) {
            // 若为标点、表情符号，则直接确认输入，不支持连续输入其他字符
            case Emoji:
            case Symbol: {
                do_Single_CharKey_Replace_or_NewPending_Inputting(context);
                break;
            }
            // 字母、数字可连续输入
            case Number:
            case Alphabet: {
                CharInput pending = inputList.getPending();
                // Note：非拉丁字符输入不可连续输入，直接对其做替换
                if (!pending.isLatin()) {
                    pending = inputList.newPending();
                }

                pending.appendKey(key);
                fire_InputChars_Input_Doing_in_TapMode(context, pending);

                do_InputList_Pending_Completion_Updating(context);
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

        Input input = inputList.getPending();
        if (key.isSymbol()) {
            // Note：标点符号是独立输入，故，需替换当前位置的前一个标点符号输入（当前输入必然为 Gap）
            input = inputList.getInputBeforeSelected();

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
        String newKeyText = key.nextReplacement(lastCharKey.getText());
        CharKey newKey = key.createReplacementKey(newKeyText);

        input.replaceLatestKey(lastCharKey, newKey);

        context = context.newWithKey(newKey);

        show_InputChars_Input_Popup(context);
        fire_InputChars_Input_Doing_in_TapMode(context, input);
    }

    /** 提交单字符按键的替换输入：对编辑框内的输入字符做输入替换 */
    protected void do_Single_CharKey_Replacement_Committing(KeyboardContext context, int replacementIndex) {
        CharKey key = context.key();
        CharKey newKey = key.createReplacementKey(replacementIndex);

        context = context.newWithKey(newKey);

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
    protected List<String> getTopBestMatchedLatins(String text) {
        return List.of();
    }

    /** 更新待输入的输入补全 */
    protected void do_InputList_Pending_Completion_Updating(KeyboardContext context) {
        InputList inputList = context.inputList;

        CharInput pending = inputList.getPending();
        if (Input.isEmpty(pending) || !pending.isLatin()) {
            return;
        }
        pending.clearCompletions();

        String text = pending.getText().toString();
        getTopBestMatchedLatins(text).forEach((latin) -> {
            List<Key> keys = CharKey.from(latin);

            if (!keys.isEmpty()) {
                CharInput input = CharInput.from(keys);
                CompletionInput completion = new CompletionInput(text.length());
                completion.add(input);

                pending.addCompletion(completion);
            }
        });

        fire_Input_Completion_Update_Done(context, pending);
    }

    /** 更新当前输入位置的短语输入补全 */
    protected void do_InputList_Current_Phrase_Completion_Updating(KeyboardContext context) {
        InputList inputList = context.inputList;
        inputList.clearPhraseCompletions();

        Input input = inputList.getSelected();
        do_InputList_Phrase_Completion_Updating(context, input);

        fire_Input_Completion_Update_Done(context, input);
    }

    protected void do_InputList_Phrase_Completion_Updating(KeyboardContext context, Input input) {}

    // ======================== End: 输入补全 ========================

    // ======================== Start: 操作输入列表 ========================

    /** 选中输入列表中的已选择输入，用于再次触发对输入的 {@link #choose_InputList_Input 选中处理} */
    protected void choose_InputList_Selected_Input(KeyboardContext context) {
        InputList inputList = context.inputList;
        Input selected = inputList.getSelected();

        choose_InputList_Input(context, selected);
    }

    /** 选中输入列表中的指定输入，一般切换到该输入所对应的 {@link Keyboard} 上 */
    protected void choose_InputList_Input(KeyboardContext context, Input input) {
        InputList inputList = context.inputList;
        inputList.select(input);

        do_InputList_Current_Phrase_Completion_Updating(context);

        // Note：输入过程中操作和处理的都是 pending
        CharInput pending = inputList.getPending();

        // 处理选中的输入需要切换到原键盘的情况
        if (context.config.xInputPadEnabled) {
            // Note：在 X 型输入中，各类键盘是可直接相互切换的，不需要退出再进入，
            // 故而，在选中其输入时，也需要能够直接进入其输入选择状态
            if (pending.isPinyin() && getType() != Type.Pinyin) {
                switch_Keyboard_To(context, Type.Pinyin);
                return;
            }
        }

        if (pending.isMathExpr()) {
            switch_Keyboard_To(context, Type.Math);
        } else if (pending.isEmoji()) {
            switch_Keyboard_To(context, Type.Emoji);
        } else if (pending.isSymbol()) {
            switch_Keyboard_To(context, Type.Symbol);
        } else if (pending.isPinyin()) {
            switch_Keyboard_To(context, Type.Pinyin_Candidate);
        } else {
            // 在选择输入时，对于新输入，需先确认其 pending
            if (input.isGap() && !pending.isEmpty()) {
                confirm_InputList_Pending(context);
            }
            change_State_to_Init(context);

            fire_Common_InputMsg(context, Input_Choose_Done, input);
        }
    }

    /** 确认待输入，并触发 {@link InputMsgType#InputChars_Input_Done} 消息 */
    protected void confirm_InputList_Pending(KeyboardContext context) {
        InputList inputList = context.inputList;
        CharInput pending = inputList.getPending();

        inputList.confirmPendingAndSelectNext();

        fire_InputChars_Input_Done(context, pending);

        do_InputList_Current_Phrase_Completion_Updating(context);
    }

    /**
     * 对于 Gap 输入先提交其待输入，并选中后继 Gap；
     * 而对于已存在的输入，则直接新建待输入以做替换输入
     */
    protected void confirm_or_New_InputList_Pending(KeyboardContext context) {
        InputList inputList = context.inputList;

        // 对于新增输入，先做提交，再录入
        if (inputList.isGapSelected()) {
            inputList.confirmPendingAndSelectNext();
        }
        // 对于修改输入，则直接对其做替换
        else {
            inputList.newPending();
        }
    }

    /** 仅{@link #confirm_InputList_Pending 确认}只有唯一按键的输入 */
    protected void confirm_InputList_Input_with_SingleKey_Only(KeyboardContext context) {
        Key key = context.key();
        InputList inputList = context.inputList;

        inputList.newPending().appendKey(key);

        confirm_InputList_Pending(context);
    }

    /**
     * 确认回车或空格的控制按键输入
     * <p/>
     * 为回车时，直接提交当前输入或在 目标编辑器 中输入换行；
     * 为空格时，当输入列表为空时，直接向 目标编辑器 输入空格，
     * 否则，将空格附加到输入列表中
     */
    protected void confirm_InputList_Input_Enter_or_Space(KeyboardContext context) {
        CtrlKey key = context.key();
        InputList inputList = context.inputList;
        boolean isDirectInputting = inputList.isEmpty();

        if (isDirectInputting) {
            switch (key.getType()) {
                case Enter:
                case Space:
                    // Note：直输回车和空格后，不再支持输入撤回
                    inputList.clearCommitRevokes();

                    fire_InputList_Commit_Doing(context, key.getText(), null);
                    break;
            }
        }
        // 输入列表不为空且按键为空格按键时，将其添加到输入列表中
        else if (CtrlKey.is(key, CtrlKey.Type.Space)) {
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
        fire_Common_InputMsg(context, Input_Selected_Delete_Done, input);

        do_InputList_Current_Phrase_Completion_Updating(context);
    }

    /** 删除待输入 */
    protected void drop_InputList_Pending(KeyboardContext context) {
        InputList inputList = context.inputList;
        CharInput pending = inputList.getPending();

        inputList.dropPending();
        fire_Common_InputMsg(context, Input_Pending_Drop_Done, pending);

        do_InputList_Current_Phrase_Completion_Updating(context);
    }

    /**
     * {@link #commit_InputList 提交输入列表（可撤销）}，
     * 并{@link #change_State_to_Init 进入初始状态}
     */
    protected void commit_InputList_and_Goto_Init_State(KeyboardContext context) {
        commit_InputList(context, true, false);

        change_State_to_Init(context);
    }

    /** 提交单一按键输入，且该提交不可被撤销 */
    protected void commit_InputList_with_SingleKey_Only(KeyboardContext context, boolean needToBeReplaced) {
        Key key = context.key();
        InputList inputList = context.inputList;

        inputList.newPending().appendKey(key);

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

            inputList.commit(false);

            fire_InputList_PairSymbol_Commit_Doing(context, left.getText(), right.getText());
        } else {
            List<String> replacements = null;
            if (needToBeReplaced) {
                CharInput input = inputList.getLastCharInput();
                CharKey key = (CharKey) input.getLastKey();

                replacements = key.getReplacements();
            }

            StringBuilder text = inputList.commit(canBeRevoked);

            fire_InputList_Commit_Doing(context, text, replacements);
        }
    }

    /** 撤回输入列表，且状态保持不变 */
    protected void revoke_Committed_InputList(KeyboardContext context) {
        InputList inputList = context.inputList;
        if (!inputList.canRevokeCommit()) {
            return;
        }

        inputList.revokeCommit();
        after_Revoke_Committed_InputList(context);

        fire_Common_InputMsg(context, InputList_Committed_Revoke_Doing);

        choose_InputList_Selected_Input(context);
    }

    /**
     * 回删输入列表中的输入或 目标编辑器 的内容
     * <p/>
     * 输入列表不为空时，在输入列表中做删除，否则，在 目标编辑器 做删除
     */
    protected void backspace_InputList_or_Editor(KeyboardContext context) {
        InputList inputList = context.inputList;
        inputList.clearCommitRevokes();

        if (!inputList.isEmpty()) {
            do_InputList_Backspacing(context);
        } else {
            do_Editor_Backspacing(context);
        }
    }

    /** 在 {@link #commit_InputList} 之前需要做的事情 */
    protected void before_Commit_InputList(KeyboardContext context) {
        if (this.dict != null) {
            handle_UserInput_Data(context, this.dict::saveUserInputData);
        }
    }

    /** 在 {@link #revoke_Committed_InputList} 之后需要做的事情 */
    protected void after_Revoke_Committed_InputList(KeyboardContext context) {
        if (this.dict != null) {
            handle_UserInput_Data(context, this.dict::revokeSavedUserInputData);
        }
    }

    /**
     * 在未{@link KeyboardConfig#userInputDataDisabled 禁用}对用户输入数据的保存的情况下，
     * 调用 <code>consumer</code> 对{@link UserInputData 用户输入数据}做处理
     */
    protected void handle_UserInput_Data(KeyboardContext context, Consumer<UserInputData> consumer) {
        if (context.config.userInputDataDisabled) {
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
        InputMsg msg = new InputMsg(msgType, msgData);
        context.listener.onMsg(msg);
    }

    protected void fire_Common_InputMsg(KeyboardContext context, InputMsgType msgType) {
        fire_Common_InputMsg(context, msgType, null);
    }

    protected void fire_Common_InputMsg(KeyboardContext context, InputMsgType msgType, Input input) {
        Key key = context.key();
        InputMsgData data = new InputMsgData(key, input);
        fire_InputMsg(context, msgType, data);
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

    /** 触发 {@link InputMsgType#Input_Completion_Update_Done} 消息 */
    protected void fire_Input_Completion_Update_Done(KeyboardContext context, Input input) {
        InputList inputList = context.inputList;
        InputCompletionUpdateMsgData data = new InputCompletionUpdateMsgData(input, inputList.getCompletions());

        fire_InputMsg(context, Input_Completion_Update_Done, data);
    }

    /** 触发 {@link InputMsgType#InputList_Commit_Doing} 消息 */
    protected void fire_InputList_Commit_Doing(KeyboardContext context, CharSequence text, List<String> replacements) {
        InputMsgData data = new InputListCommitMsgData(text, replacements);

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
        Key key = context.key();
        if (CtrlKey.isNoOp(key) && audioType != InputAudioPlayMsgData.AudioType.PageFlip) {
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
        HandMode mode = context.config.handMode;
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

    /** 切换系统输入法 */
    protected void switch_IME(KeyboardContext context) {
        // Note：有可能切换，也有可能不切换，
        // 若发生切换，则再回来时键盘状态会主动被重置，
        // 故不需要提前重置键盘状态
        fire_Common_InputMsg(context, IME_Switch_Doing);
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
        String text = key != null ? key.getLabel() : null;

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
        fire_InputAudio_Play_Doing(context, InputAudioPlayMsgData.AudioType.SingleTick);
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
