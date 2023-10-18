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

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import org.crazydan.studio.app.ime.kuaizi.internal.Input;
import org.crazydan.studio.app.ime.kuaizi.internal.InputList;
import org.crazydan.studio.app.ime.kuaizi.internal.InputWord;
import org.crazydan.studio.app.ime.kuaizi.internal.Key;
import org.crazydan.studio.app.ime.kuaizi.internal.Keyboard;
import org.crazydan.studio.app.ime.kuaizi.internal.Symbol;
import org.crazydan.studio.app.ime.kuaizi.internal.data.Emojis;
import org.crazydan.studio.app.ime.kuaizi.internal.data.PinyinDictDB;
import org.crazydan.studio.app.ime.kuaizi.internal.data.SymbolGroup;
import org.crazydan.studio.app.ime.kuaizi.internal.input.CharInput;
import org.crazydan.studio.app.ime.kuaizi.internal.input.CompletionInput;
import org.crazydan.studio.app.ime.kuaizi.internal.key.CharKey;
import org.crazydan.studio.app.ime.kuaizi.internal.key.CtrlKey;
import org.crazydan.studio.app.ime.kuaizi.internal.key.InputWordKey;
import org.crazydan.studio.app.ime.kuaizi.internal.key.SymbolKey;
import org.crazydan.studio.app.ime.kuaizi.internal.keyboard.keytable.EditorEditKeyTable;
import org.crazydan.studio.app.ime.kuaizi.internal.keyboard.keytable.SymbolEmojiKeyTable;
import org.crazydan.studio.app.ime.kuaizi.internal.keyboard.state.EditorEditDoingStateData;
import org.crazydan.studio.app.ime.kuaizi.internal.keyboard.state.EmojiChooseDoingStateData;
import org.crazydan.studio.app.ime.kuaizi.internal.keyboard.state.PagingStateData;
import org.crazydan.studio.app.ime.kuaizi.internal.keyboard.state.SymbolChooseDoingStateData;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.EditorEditAction;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.InputMsg;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.InputMsgData;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.InputMsgListener;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.Motion;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.UserInputMsg;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.UserInputMsgData;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.UserKeyMsg;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.UserKeyMsgData;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.input.EditorCursorMovingMsgData;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.input.EditorEditDoingMsgData;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.input.InputAudioPlayDoingMsgData;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.input.InputCharsInputtingMsgData;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.input.InputChooseDoneMsgData;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.input.InputCommonMsgData;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.input.InputListCommitDoingMsgData;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.input.InputListInputCompletionApplyDoneMsgData;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.input.InputListInputDeletedMsgData;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.input.InputListPairSymbolCommitDoingMsgData;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.input.KeyboardConfigUpdateDoneMsgData;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.input.KeyboardHandModeSwitchDoneMsgData;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.input.KeyboardStateChangeDoneMsgData;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.input.KeyboardSwitchDoingMsgData;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.user.UserFingerFlippingMsgData;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.user.UserSingleTapMsgData;

/**
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-06-28
 */
public abstract class BaseKeyboard implements Keyboard {
    protected final PinyinDictDB pinyinDict = PinyinDictDB.getInstance();
    private final Set<InputMsgListener> inputMsgListeners = new HashSet<>();
    protected State state = new State(State.Type.InputChars_Input_Waiting);

    private Config config;

    /** 输入列表 */
    private InputList inputList;

    /** 获取键盘初始状态，即，{@link State.Type#InputChars_Input_Waiting 待输入}状态 */
    protected State getInitState() {
        if (this.state.type == State.Type.InputChars_Input_Waiting) {
            return createInitState();
        }
        return new State(State.Type.InputChars_Input_Waiting, this.state);
    }

    protected State createInitState() {
        return new State(State.Type.InputChars_Input_Waiting);
    }

    @Override
    public Config getConfig() {
        return this.config;
    }

    @Override
    public void setConfig(Config newConfig) {
        Config oldConfig = this.config;
        this.config = newConfig;

        if (newConfig.equals(oldConfig)) {
            return;
        }

        InputMsgData data = new KeyboardConfigUpdateDoneMsgData(oldConfig, newConfig);
        fireInputMsg(InputMsg.Keyboard_Config_Update_Done, data);
    }

    @Override
    public void start() {
        // 将算数键盘视为内嵌键盘，故而，在选中其他类型输入时，需做选择处理。
        // 而对于其他键盘，选中的输入将视为将被替换的输入，故不做选择处理
        if (getConfig().getSwitchFromType() == Type.Math) {
            start_Selected_Input_ReChoosing(getInputList());
        }
    }

    @Override
    public void reset() {
        change_State_to_Init();
    }

    @Override
    public void destroy() {
        if (this.inputList != null) {
            this.inputList.removeUserInputMsgListener(this);
        }

        this.inputMsgListeners.clear();
        this.config = null;
        this.inputList = null;
    }

    public InputList getInputList() {
        return this.inputList;
    }

    @Override
    public void setInputList(InputList inputList) {
        this.inputList = inputList;
        this.inputList.addUserInputMsgListener(this);
    }

    @Override
    public void addInputMsgListener(InputMsgListener listener) {
        this.inputMsgListeners.add(listener);
    }

    @Override
    public void removeInputMsgListener(InputMsgListener listener) {
        this.inputMsgListeners.remove(listener);
    }

    @Override
    public void onThemeUpdated() {
        fireInputMsg(InputMsg.Keyboard_Theme_Update_Done, new InputCommonMsgData());
    }

    /** 触发 {@link InputMsg} 消息 */
    public void fireInputMsg(InputMsg msg, InputMsgData data) {
        // Note: 存在在监听未执行完毕便移除监听的情况，故，在监听列表的副本中执行监听
        Set<InputMsgListener> listeners = new HashSet<>(this.inputMsgListeners);
        listeners.forEach(listener -> listener.onInputMsg(msg, data));
    }

    @Override
    public KeyFactory getKeyFactory() {
        switch (this.state.type) {
            case Editor_Edit_Doing: {
                EditorEditKeyTable keyTable = EditorEditKeyTable.create(createKeyTableConfigure());

                return keyTable::createKeys;
            }
            case Emoji_Choose_Doing: {
                SymbolEmojiKeyTable keyTable = SymbolEmojiKeyTable.create(createKeyTableConfigure());

                EmojiChooseDoingStateData stateData = (EmojiChooseDoingStateData) this.state.data;

                return () -> keyTable.createEmojiKeys(stateData.getGroups(),
                                                      stateData.getPagingData(),
                                                      stateData.getGroup(),
                                                      stateData.getPageStart());
            }
            case Symbol_Choose_Doing: {
                SymbolEmojiKeyTable keyTable = SymbolEmojiKeyTable.create(createKeyTableConfigure());

                SymbolChooseDoingStateData stateData = (SymbolChooseDoingStateData) this.state.data;

                return () -> keyTable.createSymbolKeys(stateData.getGroup(),
                                                       stateData.isOnlyPair(),
                                                       stateData.getPageStart());
            }
        }
        return doGetKeyFactory();
    }

    protected abstract KeyFactory doGetKeyFactory();

    protected KeyTable.Config createKeyTableConfigure() {
        return createKeyTableConfigure(getInputList());
    }

    protected KeyTable.Config createKeyTableConfigure(InputList inputList) {
        return new KeyTable.Config(getConfig(),
                                   !inputList.isEmpty(),
                                   inputList.canRevokeCommit(),
                                   !inputList.isGapSelected());
    }

    @Override
    public void onUserInputMsg(UserInputMsg msg, UserInputMsgData data) {
        switch (msg) {
            case Inputs_Clean_Done:
            case Input_Choose_Doing:
            case Inputs_Cleaned_Cancel_Done: {
                getInputList().clearPhraseCompletions();
                break;
            }
            case Input_Completion_Choose_Doing: {
                CompletionInput completion = (CompletionInput) data.target;
                start_InputList_Completion_Applying(completion);
                break;
            }
        }

        switch (this.state.type) {
            case InputChars_Input_Waiting:
            case Emoji_Choose_Doing:
            case Symbol_Choose_Doing: {
                switch (msg) {
                    case Input_Choose_Doing: {
                        start_Input_Choosing(data.target);
                        break;
                    }
                    case Inputs_Clean_Done: {
                        fire_InputList_Clean_Done();
                        break;
                    }
                    case Inputs_Cleaned_Cancel_Done: {
                        fire_InputList_Cleaned_Cancel_Done();
                        break;
                    }
                }
                break;
            }
        }
    }

    /**
     * 尝试对 {@link UserKeyMsg} 做处理
     * <p/>
     * 若返回 <code>true</code>，则表示已处理，否则，返回 <code>false</code>
     * <p/>
     * {@link CtrlKey#isDisabled() 被禁用}的 {@link CtrlKey} 将始终返回 <code>true</code>
     */
    protected boolean try_OnUserKeyMsg(UserKeyMsg msg, UserKeyMsgData data) {
        Key<?> key = data.target;

        // Note: NoOp 控制按键上的消息不能忽略，滑屏输入和翻页等状态下会涉及该类控制按键的消息处理
        if (key instanceof CtrlKey //
            && (key.isDisabled() //
                || try_Common_OnCtrlKeyMsg(msg, (CtrlKey) key, data))) {
            return true;
        }

        switch (this.state.type) {
            case InputChars_Input_Waiting: {
                break;
            }
            case Editor_Edit_Doing: {
                if (key instanceof CtrlKey) {
                    on_Editor_Edit_Doing_CtrlKey_Msg(msg, (CtrlKey) key, data);
                }
                return true;
            }
            case Emoji_Choose_Doing: {
                if (msg == UserKeyMsg.FingerFlipping) {
                    on_Emoji_Choose_Doing_PageFlipping_Msg(msg, key, data);
                } else if (key instanceof InputWordKey) {
                    on_Emoji_Choose_Doing_InputWordKey_Msg(msg, (InputWordKey) key, data);
                } else if (key instanceof CtrlKey) {
                    on_Emoji_Choose_Doing_CtrlKey_Msg(msg, (CtrlKey) key, data);
                }
                return true;
            }
            case Symbol_Choose_Doing: {
                if (msg == UserKeyMsg.FingerFlipping) {
                    on_Symbol_Choose_Doing_PageFlipping_Msg(msg, key, data);
                } else if (key instanceof SymbolKey) {
                    on_Symbol_Choose_Doing_SymbolKey_Msg(msg, (SymbolKey) key, data);
                } else if (key instanceof CtrlKey) {
                    on_Symbol_Choose_Doing_CtrlKey_Msg(msg, (CtrlKey) key, data);
                }
                return true;
            }
        }

        return false;
    }

    protected void fire_Common_InputMsg(InputMsg msg) {
        fire_Common_InputMsg(msg, null);
    }

    protected void fire_Common_InputMsg(InputMsg msg, Key<?> key) {
        InputMsgData data = new InputCommonMsgData(getKeyFactory(), key);

        fireInputMsg(msg, data);
    }

    /** 触发 {@link InputMsg#InputChars_Input_Doing} 消息 */
    protected void fire_InputChars_Input_Doing(Key<?> key) {
        InputMsgData data = new InputCharsInputtingMsgData(getKeyFactory(), key);

        fireInputMsg(InputMsg.InputChars_Input_Doing, data);
    }

    /** 触发 {@link InputMsg#InputChars_Input_Done} 消息 */
    protected void fire_InputChars_Input_Done(Key<?> key) {
        InputMsgData data = new InputCharsInputtingMsgData(getKeyFactory(), key);

        fireInputMsg(InputMsg.InputChars_Input_Done, data);
    }

    /** 触发 {@link InputMsg#InputList_Input_Choose_Done} 消息 */
    protected void fire_InputList_Input_Choose_Done(Input<?> input) {
        InputMsgData data = new InputChooseDoneMsgData(input);

        fireInputMsg(InputMsg.InputList_Input_Choose_Done, data);
    }

    /** 触发 {@link InputMsg#InputList_Input_Completion_Update_Done} 消息 */
    protected void fire_InputList_Input_Completion_Update_Done() {
        InputMsgData data = new InputCommonMsgData();

        fireInputMsg(InputMsg.InputList_Input_Completion_Update_Done, data);
    }

    /** 触发 {@link InputMsg#InputList_Input_Completion_Apply_Done} 消息 */
    protected void fire_InputList_Input_Completion_Apply_Done(CompletionInput completion) {
        InputMsgData data = new InputListInputCompletionApplyDoneMsgData(completion);

        fireInputMsg(InputMsg.InputList_Input_Completion_Apply_Done, data);
    }

    /** 触发 {@link InputMsg#InputList_Selected_Delete_Done} 消息 */
    protected void fire_InputList_Selected_Delete_Done(Key<?> key, CharInput input) {
        InputMsgData data = new InputListInputDeletedMsgData(getKeyFactory(), key, input);

        fireInputMsg(InputMsg.InputList_Selected_Delete_Done, data);
    }

    /** 触发 {@link InputMsg#InputList_Pending_Drop_Done} 消息 */
    protected void fire_InputList_Pending_Drop_Done(Key<?> key, CharInput input) {
        InputMsgData data = new InputListInputDeletedMsgData(getKeyFactory(), key, input);

        fireInputMsg(InputMsg.InputList_Pending_Drop_Done, data);
    }

    /** 触发 {@link InputMsg#InputList_Clean_Done} 消息 */
    protected void fire_InputList_Clean_Done() {
        fire_Common_InputMsg(InputMsg.InputList_Clean_Done);
    }

    /** 触发 {@link InputMsg#InputList_Cleaned_Cancel_Done} 消息 */
    protected void fire_InputList_Cleaned_Cancel_Done() {
        fire_Common_InputMsg(InputMsg.InputList_Cleaned_Cancel_Done);
    }

    /** 触发 {@link InputMsg#InputList_Commit_Doing} 消息 */
    protected void fire_InputList_Commit_Doing(CharSequence text, List<String> replacements) {
        // Note：输入提交按钮会根据输入内容确定按钮状态，故，需要回传 KeyFactory 以重新渲染按键
        InputMsgData data = new InputListCommitDoingMsgData(getKeyFactory(), text, replacements);

        fireInputMsg(InputMsg.InputList_Commit_Doing, data);
    }

    /** 触发 {@link InputMsg#InputList_PairSymbol_Commit_Doing} 消息 */
    protected void fire_InputList_PairSymbol_Commit_Doing(CharSequence left, CharSequence right) {
        // Note：输入提交按钮会根据输入内容确定按钮状态，故，需要回传 KeyFactory 以重新渲染按键
        InputMsgData data = new InputListPairSymbolCommitDoingMsgData(getKeyFactory(), left, right);

        fireInputMsg(InputMsg.InputList_PairSymbol_Commit_Doing, data);
    }

    /** 状态回到{@link State.Type#InputChars_Input_Waiting 待输入} */
    protected void change_State_to_Init() {
        change_State_to_Init(null);
    }

    /**
     * 状态回到{@link State.Type#InputChars_Input_Waiting 待输入}
     *
     * @param key
     *         触发状态变化的按键
     */
    protected void change_State_to_Init(Key<?> key) {
        change_State_To(key, getInitState());
    }

    protected void change_State_To(Key<?> key, State state) {
        this.state = state;

        InputMsgData data = new KeyboardStateChangeDoneMsgData(getKeyFactory(), key, state);
        fireInputMsg(InputMsg.Keyboard_State_Change_Done, data);
    }

    /**
     * 回到前序状态
     * <p/>
     * 若无前序状态，则回到初始状态
     */
    protected void change_State_to_Previous(Key<?> key) {
        State state = this.state;
        State previous = state.previous;

        // 跳过与当前状态的类型相同的前序状态
        while (previous != null && previous.type == state.type) {
            state = previous;
            previous = state.previous;
        }

        if (previous == null) {
            change_State_to_Init(key);
        } else {
            change_State_To(key, previous);
        }
    }

    /** {@link #confirm_InputList_Pending 确认当前输入}，并{@link #change_State_to_Init 进入初始状态} */
    protected void confirm_InputList_Pending_and_Goto_Init_State(InputList inputList, Key<?> key) {
        confirm_InputList_Pending(inputList, key);

        change_State_to_Init();
    }

    /** 确认待输入，并触发 {@link InputMsg#InputChars_Input_Done} 消息 */
    protected void confirm_InputList_Pending(InputList inputList, Key<?> key) {
        inputList.confirmPendingAndSelectNext();
        fire_InputChars_Input_Done(key);

        start_InputList_Current_Phrase_Completion_Updating(inputList);
    }

    /**
     * 对于 Gap 输入先提交其待输入，并选中后继 Gap；
     * 而对于已存在的输入，则直接新建待输入以做替换输入
     */
    protected void confirm_or_New_InputList_Pending(InputList inputList) {
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
    protected void confirm_InputList_Input_with_SingleKey_Only(InputList inputList, Key<?> key) {
        inputList.newPending().appendKey(key);

        confirm_InputList_Pending(inputList, key);
    }

    /**
     * 确认回车或空格的控制按键输入
     * <p/>
     * 为回车时，直接提交当前输入或在 目标编辑器 中输入换行；
     * 为空格时，当输入列表为空时，直接向 目标编辑器 输入空格，
     * 否则，将空格附加到输入列表中
     */
    protected void confirm_InputList_Input_Enter_or_Space(InputList inputList, CtrlKey key) {
        boolean isDirectInputting = inputList.isEmpty();

        if (isDirectInputting) {
            switch (key.getType()) {
                case Enter:
                case Space:
                    // Note：直输回车和空格后，不再支持输入撤回
                    inputList.clearCommitRevokes();

                    fire_InputList_Commit_Doing(key.getText(), null);
                    break;
            }
        }
        // 输入列表不为空且按键为空格按键时，将其添加到输入列表中
        else if (key.getType() == CtrlKey.Type.Space) {
            // Note：空格不替换当前输入
            inputList.confirmPendingAndSelectNext();

            confirm_InputList_Input_with_SingleKey_Only(inputList, key);
        }
    }

    /** 删除已选中的输入，并触发 {@link InputMsg#InputList_Selected_Delete_Done} 消息 */
    protected void delete_InputList_Selected(InputList inputList, Key<?> key) {
        CharInput input = inputList.getPending();

        inputList.deleteSelected();
        fire_InputList_Selected_Delete_Done(key, input);

        start_InputList_Current_Phrase_Completion_Updating(inputList);
    }

    /** 删除待输入，并触发 {@link InputMsg#InputList_Pending_Drop_Done} 消息 */
    protected void drop_InputList_Pending(InputList inputList, Key<?> key) {
        CharInput input = inputList.getPending();

        inputList.dropPending();
        fire_InputList_Pending_Drop_Done(key, input);

        start_InputList_Current_Phrase_Completion_Updating(inputList);
    }

    /**
     * {@link #commit_InputList 提交输入列表（可撤销）}，
     * 并并{@link #change_State_to_Init 进入初始状态}
     */
    protected void commit_InputList_and_Goto_Init_State(InputList inputList) {
        commit_InputList(inputList, true, false);

        change_State_to_Init();
    }

    /** 仅{@link #commit_InputList 提交}唯一按键输入（提交不可撤销） */
    protected void commit_InputList_with_SingleKey_Only(InputList inputList, Key<?> key, boolean needToBeReplaced) {
        inputList.newPending().appendKey(key);

        commit_InputList(inputList, false, needToBeReplaced);
    }

    /** 提交输入列表 */
    protected void commit_InputList(InputList inputList, boolean canBeRevoked, boolean needToBeReplaced) {
        commit_InputList(inputList, canBeRevoked, needToBeReplaced, false);
    }

    /** 提交输入列表 */
    protected void commit_InputList(
            InputList inputList, boolean canBeRevoked, boolean needToBeReplaced, boolean isPairSymbol
    ) {
        inputList.confirmPending();
        if (inputList.isEmpty()) {
            return;
        }

        before_Commit_InputList(inputList);

        if (isPairSymbol) {
            CharInput left = inputList.getFirstCharInput();
            CharInput right = inputList.getLastCharInput();

            inputList.commit(canBeRevoked);

            fire_InputList_PairSymbol_Commit_Doing(left.getText(), right.getText());
        } else {
            List<String> replacements = null;
            if (needToBeReplaced) {
                CharInput input = inputList.getLastCharInput();
                CharKey key = (CharKey) input.getLastKey();

                replacements = key.getReplacements();
            }

            StringBuilder text = inputList.commit(canBeRevoked);

            fire_InputList_Commit_Doing(text, replacements);
        }
    }

    /** 在 {@link #commit_InputList} 之前需要做的事情 */
    protected void before_Commit_InputList(InputList inputList) {}

    /** 撤回输入列表，且状态保持不变 */
    protected void revoke_Committed_InputList(InputList inputList, Key<?> key) {
        if (!inputList.canRevokeCommit()) {
            return;
        }

        inputList.revokeCommit();
        after_Revoke_Committed_InputList(inputList);
        fire_Common_InputMsg(InputMsg.InputList_Committed_Revoke_Doing, key);

        start_Selected_Input_ReChoosing(inputList);
    }

    /** 在 {@link #revoke_Committed_InputList} 之后需要做的事情 */
    protected void after_Revoke_Committed_InputList(InputList inputList) {}

    /**
     * 回删输入列表中的输入或 目标编辑器 的内容
     * <p/>
     * 输入列表不为空时，在输入列表中做删除，否则，在 目标编辑器 做删除
     */
    protected void backspace_InputList_or_Editor(InputList inputList, Key<?> key) {
        inputList.clearCommitRevokes();

        if (!inputList.isEmpty()) {
            do_InputList_Backspacing(inputList, key);
        } else {
            do_Editor_Backspacing(inputList, key);
        }
    }

    /** 根据{@link UserKeyMsg 按键消息}更新{@link PagingStateData 分页状态数据} */
    protected void update_PagingStateData_by_UserKeyMsg(PagingStateData<?> stateData, UserFingerFlippingMsgData data) {
        Motion motion = data.motion;
        boolean pageUp = motion.direction == Motion.Direction.up || motion.direction == Motion.Direction.left;

        boolean needPaging = pageUp ? stateData.nextPage() : stateData.prevPage();
        if (needPaging) {
            play_PageFlip_InputAudio();
        }
    }

    /** 切换到指定类型的键盘 */
    protected void switch_Keyboard(Keyboard.Type target, Key<?> key) {
        Keyboard.Type source = getConfig().getType();
        InputMsgData data = new KeyboardSwitchDoingMsgData(key, source, target);

        fireInputMsg(InputMsg.Keyboard_Switch_Doing, data);
    }

    /** 切换到先前的键盘，也就是从哪个键盘切过来的，就切回到哪个键盘 */
    protected void switchTo_Previous_Keyboard(Key<?> key) {
        Keyboard.Type target = getConfig().getSwitchFromType();

        switch_Keyboard(target, key);
    }

    /** 切换键盘的左右手模式 */
    protected void switch_HandMode(Key<?> key) {
        Keyboard.Config config = getConfig();

        switch (config.getHandMode()) {
            case Left:
                config.setHandMode(HandMode.Right);
                break;
            case Right:
                config.setHandMode(HandMode.Left);
                break;
        }

        InputMsgData data = new KeyboardHandModeSwitchDoneMsgData(getKeyFactory(), key, config.getHandMode());
        fireInputMsg(InputMsg.Keyboard_HandMode_Switch_Done, data);
    }

    /** 切换系统输入法 */
    protected void switch_IME(Key<?> key) {
        // Note：有可能切换，也有可能不切换，
        // 若发生切换，则再回来时键盘状态会主动被重置，
        // 故不需要提前重置键盘状态
        fire_Common_InputMsg(InputMsg.IME_Switch_Doing, key);
    }

    /** 退出并返回到原状态或前序键盘 */
    protected void exit(Key<?> key) {
        if (this.state.previous == null) {
            switchTo_Previous_Keyboard(key);
        } else {
            change_State_to_Previous(key);
        }
    }

    /** 播放输入单击音效 */
    protected void play_SingleTick_InputAudio(Key<?> key) {
        fire_InputAudio_Play_Doing(key, InputAudioPlayDoingMsgData.AudioType.SingleTick);
    }

    /** 播放输入双击音效 */
    protected void play_DoubleTick_InputAudio(Key<?> key) {
        fire_InputAudio_Play_Doing(key, InputAudioPlayDoingMsgData.AudioType.DoubleTick);
    }

    /** 播放输入翻页音效 */
    protected void play_PageFlip_InputAudio() {
        fire_InputAudio_Play_Doing(null, InputAudioPlayDoingMsgData.AudioType.PageFlip);
    }

    private void fire_InputAudio_Play_Doing(Key<?> key, InputAudioPlayDoingMsgData.AudioType audioType) {
        if (CtrlKey.isNoOp(key)) {
            return;
        }

        InputMsgData data = new InputAudioPlayDoingMsgData(key, audioType);
        fireInputMsg(InputMsg.InputAudio_Play_Doing, data);
    }

    /** 尝试处理控制按键消息 */
    protected boolean try_Common_OnCtrlKeyMsg(UserKeyMsg msg, CtrlKey key, UserKeyMsgData data) {
        InputList inputList = getInputList();

        switch (msg) {
            case KeyLongPressTick: {
                switch (key.getType()) {
                    case Backspace:
                    case Space:
                    case Enter:
                        // 长按 tick 视为连续单击
                        return try_Common_OnCtrlKeyMsg(UserKeyMsg.KeySingleTap, key, data);
                }
                break;
            }
            case KeySingleTap: {
                switch (key.getType()) {
                    // 定位按钮不响应单击和双击操作
                    case Editor_Cursor_Locator:
                        return true;
                    // Note：在任意子键盘中提交输入，都需直接回到初始键盘
                    case Commit_InputList: {
                        play_SingleTick_InputAudio(key);
                        commit_InputList_and_Goto_Init_State(inputList);
                        return true;
                    }
                    case DropInput: {
                        switch (this.state.type) {
                            case Emoji_Choose_Doing:
                            case Symbol_Choose_Doing:
                                play_SingleTick_InputAudio(key);
                                delete_InputList_Selected(inputList, key);
                                return true;
                        }
                        break;
                    }
                    case RevokeInput: {
                        play_SingleTick_InputAudio(key);
                        revoke_Committed_InputList(inputList, key);
                        return true;
                    }
                    case Backspace: {
                        play_SingleTick_InputAudio(key);
                        backspace_InputList_or_Editor(inputList, key);
                        return true;
                    }
                    case Space:
                    case Enter: {
                        play_SingleTick_InputAudio(key);
                        confirm_InputList_Input_Enter_or_Space(inputList, key);
                        return true;
                    }
                    // 点击 退出 按钮，则退回到前序状态或原键盘
                    case Exit: {
                        play_SingleTick_InputAudio(key);
                        exit(key);
                        return true;
                    }
                    case Switch_IME: {
                        play_SingleTick_InputAudio(key);
                        switch_IME(key);
                        return true;
                    }
                    case Switch_HandMode: {
                        play_SingleTick_InputAudio(key);
                        switch_HandMode(key);
                        return true;
                    }
                    case Switch_Keyboard: {
                        play_SingleTick_InputAudio(key);

                        CtrlKey.KeyboardSwitchOption option = (CtrlKey.KeyboardSwitchOption) key.getOption();
                        switch_Keyboard(option.value(), key);
                        return true;
                    }
                    case Toggle_Emoji_Keyboard: {
                        play_SingleTick_InputAudio(key);
                        start_Emoji_Choosing(key);
                        return true;
                    }
                    case Toggle_Symbol_Keyboard: {
                        play_SingleTick_InputAudio(key);
                        start_Symbol_Choosing(key, false);
                        return true;
                    }
                }
                break;
            }
        }

        // 处理定位按钮
        if (this.state.type != State.Type.Editor_Edit_Doing //
            && key.getType() == CtrlKey.Type.Editor_Cursor_Locator) {
            switch (msg) {
                case KeyLongPressStart: {
                    play_DoubleTick_InputAudio(key);

                    start_Editor_Editing(key);
                    return true;
                }
                case FingerFlipping: {
                    play_SingleTick_InputAudio(key);

                    // 在定位切换按钮上滑动也可以移动光标，但不修改键盘状态
                    Motion motion = ((UserFingerFlippingMsgData) data).motion;

                    do_Editor_Cursor_Moving(key, motion);
                    return true;
                }
            }
        }

        return false;
    }

    // <<<<<<< 回删逻辑

    /** 回删输入列表中的输入内容 */
    protected void do_InputList_Backspacing(InputList inputList, Key<?> key) {
        inputList.deleteBackward();
        fire_InputChars_Input_Doing(key);

        do_InputList_Pending_Completion_Updating(inputList);

        start_InputList_Current_Phrase_Completion_Updating(inputList);
    }

    /** 回删 目标编辑器 的内容 */
    protected void do_Editor_Backspacing(InputList inputList, Key<?> key) {
        do_Editor_Editing(inputList, EditorEditAction.backspace);
    }
    // >>>>>>>>

    // <<<<<< 输入定位逻辑
    private void start_Editor_Editing(CtrlKey key) {
        EditorEditDoingStateData stateData = new EditorEditDoingStateData();

        State state = new State(State.Type.Editor_Edit_Doing, stateData, this.state);
        change_State_To(key, state);
    }

    private void on_Editor_Edit_Doing_CtrlKey_Msg(UserKeyMsg msg, CtrlKey key, UserKeyMsgData data) {
        InputList inputList = getInputList();

        switch (msg) {
            case KeySingleTap: {
                if (key.getType() == CtrlKey.Type.Edit_Editor) {
                    play_SingleTick_InputAudio(key);

                    CtrlKey.EditorEditOption option = (CtrlKey.EditorEditOption) key.getOption();
                    do_Editor_Editing(inputList, option.value());
                }
                break;
            }
            case FingerFlipping:
                Motion motion = ((UserFingerFlippingMsgData) data).motion;
                switch (key.getType()) {
                    case Editor_Cursor_Locator:
                        play_SingleTick_InputAudio(key);
                        do_Editor_Cursor_Moving(key, motion);
                        break;
                    case Editor_Range_Selector:
                        play_SingleTick_InputAudio(key);
                        do_Editor_Range_Selecting(key, motion);
                        break;
                }
                break;
        }
    }

    private void do_Editor_Editing(InputList inputList, EditorEditAction action) {
        switch (action) {
            case noop:
            case copy:
                // 无需清空待撤回输入数据
                break;
            default:
                inputList.clearCommitRevokes();
        }

        InputMsgData data = new EditorEditDoingMsgData(getKeyFactory(), action);
        fireInputMsg(InputMsg.Editor_Edit_Doing, data);
    }

    private void do_Editor_Cursor_Moving(CtrlKey key, Motion motion) {
        Motion anchor = EditorEditDoingStateData.createAnchor(motion);

        InputMsgData data = new EditorCursorMovingMsgData(getKeyFactory(), key, anchor);
        fireInputMsg(InputMsg.Editor_Cursor_Move_Doing, data);
    }

    private void do_Editor_Range_Selecting(CtrlKey key, Motion motion) {
        Motion anchor = EditorEditDoingStateData.createAnchor(motion);

        InputMsgData data = new EditorCursorMovingMsgData(getKeyFactory(), key, anchor);
        fireInputMsg(InputMsg.Editor_Range_Select_Doing, data);
    }
    // >>>>>>>>

    // <<<<<< 单字符输入处理逻辑

    /**
     * 单字符输入处理
     * <p/>
     * 对于有替代字符的按键，根据连续点击次数确定替代字符并替换前序按键字符
     */
    protected void start_Single_Key_Inputting(
            InputList inputList, Key<?> key, UserSingleTapMsgData data, boolean directInputting
    ) {
        inputList.clearPhraseCompletions();

        if (key instanceof CharKey && ((CharKey) key).hasReplacement() && data.tick > 0) {
            do_Single_CharKey_Replacement_Inputting(inputList, (CharKey) key, data.tick, directInputting);
        } else {
            do_Single_Key_Inputting(inputList, key, directInputting);
        }
    }

    protected void do_Single_Key_Inputting(InputList inputList, Key<?> key, boolean directInputting) {
        if (directInputting) {
            commit_InputList_with_SingleKey_Only(inputList, key, false);
            return;
        }

        // Note：该类键盘不涉及配对符号的输入，故始终清空配对符号的绑定
        inputList.clearPairOnSelected();

        if (try_Single_Key_Inputting(inputList, key)) {
            return;
        }

        if (key instanceof CharKey) {
            do_Single_CharKey_Inputting(inputList, (CharKey) key);
        }
    }

    protected void do_Single_CharKey_Inputting(InputList inputList, CharKey key) {
        switch (key.getType()) {
            // 若为标点、表情符号，则直接确认输入，不支持连续输入其他字符
            case Emoji:
            case Symbol: {
                boolean isDirectInputting = inputList.isEmpty();

                if (!isDirectInputting) {
                    // Note：被选中的输入直接对其做替换
                    inputList.newPending();

                    inputList.getPending().appendKey(key);

                    confirm_InputList_Pending(inputList, key);
                } else {
                    // 单个标点、表情，直接提交输入
                    commit_InputList_with_SingleKey_Only(inputList, key, false);
                }
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
                fire_InputChars_Input_Doing(key);

                do_InputList_Pending_Completion_Updating(inputList);
                break;
            }
        }
    }

    protected void do_Single_CharKey_Replacement_Inputting(
            InputList inputList, CharKey key, int replacementIndex, boolean directInputting
    ) {
        if (directInputting) {
            do_Single_CharKey_Replacement_Committing(inputList, key, replacementIndex);
            return;
        }

        Input<?> input;
        if (key.isSymbol()) {
            // Note：标点符号是独立输入，故，需替换当前位置的前一个标点符号输入（当前输入必然为 Gap）
            input = inputList.getInputBeforeSelected();

            // 对输入列表为空时的标点符号直输输入进行替换
            if (input == null) {
                do_Single_CharKey_Replacement_Committing(inputList, key, replacementIndex);
                return;
            }
        } else {
            input = inputList.getPending();
        }

        Key<?> lastKey = input.getLastKey();
        if (!key.canReplaceTheKey(lastKey)) {
            // 转为单字符输入
            do_Single_Key_Inputting(inputList, key, false);
            return;
        }

        CharKey lastCharKey = (CharKey) lastKey;
        // Note: 在 Input 中的按键可能不携带 replacement 信息，只能通过当前按键做判断
        String newKeyText = key.nextReplacement(lastCharKey.getText());
        CharKey newKey = key.createReplacementKey(newKeyText);

        input.replaceLatestKey(lastCharKey, newKey);

        fire_InputChars_Input_Doing(newKey);
    }

    protected boolean try_Single_Key_Inputting(InputList inputList, Key<?> key) {
        if (!key.isEmoji() && !key.isSymbol()) {
            return false;
        }

        boolean isDirectInputting = inputList.isEmpty();
        if (isDirectInputting) {
            return false;
        }

        if (key instanceof SymbolKey && ((SymbolKey) key).isPair()) {
            prepare_for_PairSymbol_Inputting(inputList, (Symbol.Pair) ((SymbolKey) key).getSymbol());

            // Note：配对符号输入后不再做连续输入，键盘状态重置为初始状态
            confirm_InputList_Pending_and_Goto_Init_State(inputList, key);
        } else {
            confirm_or_New_InputList_Pending(inputList);

            confirm_InputList_Input_with_SingleKey_Only(inputList, key);
        }

        return true;
    }

    protected void do_Single_CharKey_Replacement_Committing(InputList inputList, CharKey key, int replacementIndex) {
        CharKey newKey = key.createReplacementKey(replacementIndex);

        commit_InputList_with_SingleKey_Only(inputList, newKey, true);
    }
    // >>>>>>

    // <<<<<<< 对输入补全的处理
    protected void start_InputList_Completion_Applying(CompletionInput completion) {
        InputList inputList = getInputList();

        start_InputList_Completion_Applying(inputList, completion);
    }

    protected void start_InputList_Completion_Applying(InputList inputList, CompletionInput completion) {
        inputList.applyCompletion(completion);
        // Note：待输入的补全数据将在 confirm 时清除
        inputList.confirmPendingAndSelectNext();

        fire_InputList_Input_Completion_Apply_Done(completion);
    }

    /** 更新待输入的输入补全 */
    protected void do_InputList_Pending_Completion_Updating(InputList inputList) {
        CharInput pending = inputList.getPending();
        if (Input.isEmpty(pending) || !pending.isLatin()) {
            return;
        }

        String text = pending.getText().toString();
        List<String> bestLatins = this.pinyinDict.findTopBestMatchedLatin(text, 5);

        pending.clearCompletions();
        bestLatins.forEach((latin) -> {
            List<Key<?>> keys = CharKey.from(latin);

            if (!keys.isEmpty()) {
                CharInput input = CharInput.from(keys);
                CompletionInput completion = new CompletionInput(text.length());
                completion.add(input);

                pending.addCompletion(completion);
            }
        });

        fire_InputList_Input_Completion_Update_Done();
    }

    /** 更新当前输入位置的短语输入补全 */
    protected void start_InputList_Current_Phrase_Completion_Updating(InputList inputList) {
        inputList.clearPhraseCompletions();

        Input<?> input = inputList.getSelected();
        do_InputList_Phrase_Completion_Updating(inputList, input);

        fire_InputList_Input_Completion_Update_Done();
    }

    protected void do_InputList_Phrase_Completion_Updating(InputList inputList, Input<?> input) {}
    // >>>>>>

    // <<<<<<<< 表情符号选择逻辑
    protected void start_Emoji_Choosing(Key<?> key) {
        SymbolEmojiKeyTable keyTable = SymbolEmojiKeyTable.create(createKeyTableConfigure());
        int pageSize = keyTable.getEmojiKeysPageSize();

        Emojis emojis = this.pinyinDict.findTopBestEmojis(pageSize / 2);

        EmojiChooseDoingStateData stateData = new EmojiChooseDoingStateData(emojis, pageSize);
        State state = new State(State.Type.Emoji_Choose_Doing, stateData, createInitState());
        change_State_To(key, state);

        String group = null;
        // 若默认分组（常用）的数据为空，则切换到第二个分组
        if (stateData.getPagingData().isEmpty()) {
            group = stateData.getGroups().get(1);
        }

        do_Emoji_Choosing(key, group);
    }

    private void on_Emoji_Choose_Doing_InputWordKey_Msg(UserKeyMsg msg, InputWordKey key, UserKeyMsgData data) {
        InputList inputList = getInputList();

        switch (msg) {
            case KeyLongPressTick:
            case KeySingleTap: {
                play_SingleTick_InputAudio(key);
                do_Single_Emoji_Inputting(inputList, key);
                break;
            }
        }
    }

    private void on_Emoji_Choose_Doing_CtrlKey_Msg(UserKeyMsg msg, CtrlKey key, UserKeyMsgData data) {
        if (msg == UserKeyMsg.KeySingleTap) {
            if (key.getType() == CtrlKey.Type.Toggle_Emoji_Group) {
                play_SingleTick_InputAudio(key);

                CtrlKey.TextOption option = (CtrlKey.TextOption) key.getOption();
                do_Emoji_Choosing(key, option.value());
            }
        }
    }

    private void on_Emoji_Choose_Doing_PageFlipping_Msg(UserKeyMsg msg, Key<?> key, UserKeyMsgData data) {
        update_PagingStateData_by_UserKeyMsg((PagingStateData<?>) this.state.data, (UserFingerFlippingMsgData) data);

        fire_Emoji_Choose_Doing(key);
    }

    private void do_Emoji_Choosing(Key<?> key, String group) {
        EmojiChooseDoingStateData stateData = (EmojiChooseDoingStateData) this.state.data;
        stateData.setGroup(group);

        fire_Emoji_Choose_Doing(key);
    }

    private void fire_Emoji_Choose_Doing(Key<?> key) {
        fire_Common_InputMsg(InputMsg.Emoji_Choose_Doing, key);
    }

    private void do_Single_Emoji_Inputting(InputList inputList, InputWordKey key) {
        if (try_Single_Key_Inputting(inputList, key)) {
            return;
        }

        boolean isDirectInputting = inputList.isEmpty();
        CharInput pending = inputList.newPending();

        InputWord word = key.getWord();
        pending.appendKey(key);
        pending.setWord(word);

        if (isDirectInputting) {
            // 直接提交输入
            commit_InputList(inputList, false, false);
        } else {
            confirm_InputList_Pending(inputList, key);
        }
    }
    // >>>>>>>>

    // <<<<<<<<<<< 对标点符号的操作
    private void start_Symbol_Choosing(Key<?> key, boolean onlyPair) {
        SymbolEmojiKeyTable keyTable = SymbolEmojiKeyTable.create(createKeyTableConfigure());
        int pageSize = keyTable.getSymbolKeysPageSize();

        SymbolChooseDoingStateData stateData = new SymbolChooseDoingStateData(pageSize, onlyPair);
        State state = new State(State.Type.Symbol_Choose_Doing, stateData, createInitState());
        change_State_To(key, state);

        SymbolGroup group = SymbolGroup.latin;
        if (getConfig().getType() == Type.Pinyin) {
            group = SymbolGroup.han;
        }

        do_Symbol_Choosing(key, group);
    }

    private void on_Symbol_Choose_Doing_SymbolKey_Msg(UserKeyMsg msg, SymbolKey key, UserKeyMsgData data) {
        InputList inputList = getInputList();
        boolean continuous = false;

        switch (msg) {
            case KeyLongPressTick:
                continuous = true;
            case KeySingleTap: {
                play_SingleTick_InputAudio(key);

                do_Single_Symbol_Inputting(inputList, key, continuous);
                break;
            }
        }
    }

    private void on_Symbol_Choose_Doing_CtrlKey_Msg(UserKeyMsg msg, CtrlKey key, UserKeyMsgData data) {
        if (msg == UserKeyMsg.KeySingleTap) {
            if (key.getType() == CtrlKey.Type.Toggle_Symbol_Group) {
                play_SingleTick_InputAudio(key);

                CtrlKey.SymbolGroupToggleOption option = (CtrlKey.SymbolGroupToggleOption) key.getOption();
                do_Symbol_Choosing(key, option.value());
            }
        }
    }

    private void on_Symbol_Choose_Doing_PageFlipping_Msg(UserKeyMsg msg, Key<?> key, UserKeyMsgData data) {
        update_PagingStateData_by_UserKeyMsg((PagingStateData<?>) this.state.data, (UserFingerFlippingMsgData) data);

        fire_Symbol_Choose_Doing(key);
    }

    private void do_Symbol_Choosing(Key<?> key, SymbolGroup group) {
        SymbolChooseDoingStateData stateData = (SymbolChooseDoingStateData) this.state.data;
        stateData.setGroup(group);

        fire_Symbol_Choose_Doing(key);
    }

    private void fire_Symbol_Choose_Doing(Key<?> key) {
        fire_Common_InputMsg(InputMsg.Symbol_Choose_Doing, key);
    }

    protected void do_Single_Symbol_Inputting(InputList inputList, SymbolKey key, boolean continuousInput) {
        if (try_Single_Key_Inputting(inputList, key)) {
            return;
        }

        boolean isDirectInputting = inputList.isEmpty();
        boolean isPairSymbolKey = key.isPair();
        CharInput pending = inputList.newPending();

        if (isPairSymbolKey) {
            Symbol.Pair symbol = (Symbol.Pair) key.getSymbol();

            prepare_for_PairSymbol_Inputting(inputList, symbol);
        } else {
            pending.appendKey(key);
            pending.clearPair();
        }

        if (isDirectInputting) {
            // 直接提交输入
            commit_InputList(inputList, false, false, isPairSymbolKey);
        } else {
            confirm_InputList_Pending(inputList, key);
        }

        // Note：非连续输入的情况下，配对符号输入后不再做连续输入，键盘状态重置为初始状态
        if (isPairSymbolKey && !continuousInput) {
            change_State_to_Init();
        }
    }

    private void prepare_for_PairSymbol_Inputting(InputList inputList, Symbol.Pair symbol) {
        String left = symbol.left;
        String right = symbol.right;

        prepare_for_PairKey_Inputting(inputList,
                                      () -> SymbolKey.create(Symbol.single(left)),
                                      () -> SymbolKey.create(Symbol.single(right)));
    }

    protected void prepare_for_PairKey_Inputting(InputList inputList, Supplier<Key<?>> left, Supplier<Key<?>> right) {
        Input<?> selected = inputList.getSelected();

        Key<?> leftKey = left.get();
        Key<?> rightKey = right.get();

        // 用新的配对符号替换原配对符号
        if (!selected.isGap() && ((CharInput) selected).hasPair()) {
            CharInput pending = inputList.newPending();

            CharInput leftInput = pending;
            CharInput rightInput = ((CharInput) selected).getPair();

            int rightInputIndex = inputList.indexOf(rightInput);
            // 交换左右顺序
            if (inputList.getSelectedIndex() > rightInputIndex) {
                leftInput = rightInput;
                rightInput = pending;
            }

            leftInput.replaceLastKey(leftKey);
            rightInput.replaceLastKey(rightKey);
        } else {
            // 对于非空的待输入，对其做配对符号包裹
            boolean wrapSelected = !inputList.hasEmptyPending() && !selected.isSymbol();
            if (wrapSelected) {
                // 选中被包裹输入的左侧 Gap
                inputList.confirmPendingAndSelectPrevious();
            }

            CharInput leftInput = inputList.getPending();
            leftInput.appendKey(leftKey);

            if (wrapSelected) {
                // 选中被包裹输入的右侧 Gap：左符号+Gap+被包裹输入+右符号
                inputList.confirmPendingAndSelectByOffset(3);
            } else {
                inputList.confirmPendingAndSelectNext();
            }

            CharInput rightInput = inputList.getPending();
            rightInput.appendKey(rightKey);

            // 绑定配对符号的关联：由任意一方发起绑定即可
            rightInput.setPair(leftInput);

            // 确认右侧的配对输入，并将光标移动到 右配对输入 的左侧 Gap 位置以确保光标在配对符号的中间位置
            inputList.confirmPendingAndSelectPrevious();
        }
    }
    // >>>>>>>>>>>

    // <<<<<<<<< 对输入列表的操作
    protected void start_Selected_Input_ReChoosing(InputList inputList) {
        Input<?> selected = inputList.getSelected();

        start_Input_Choosing(inputList, selected);
    }

    protected void start_Input_Choosing(Input<?> input) {
        start_Input_Choosing(getInputList(), input);
    }

    protected void start_Input_Choosing(InputList inputList, Input<?> input) {
        inputList.select(input);
        start_InputList_Current_Phrase_Completion_Updating(inputList);

        // Note：输入过程中操作和处理的都是 pending
        CharInput pending = inputList.getPending();

        if (pending.isEmoji()) {
            start_Emoji_Choosing(null);
        } else if (pending.isSymbol()) {
            boolean hasPair = !input.isGap() && ((CharInput) input).hasPair();

            start_Symbol_Choosing(null, hasPair);
        } else if (pending.isMathExpr()) {
            switch_Keyboard(Type.Math, null);
        } else if (!do_Input_Choosing(inputList, pending)) {
            // 在选择输入时，对于新输入，需先确认其 pending
            if (input.isGap() && !pending.isEmpty()) {
                confirm_InputList_Pending_and_Goto_Init_State(inputList, null);
            } else {
                change_State_to_Init();
            }
        }

        fire_InputList_Input_Choose_Done(input);
    }

    /** 已处理时返回 <code>true</code>，否则返回 <code>false</code> 以按默认方式处理 */
    protected boolean do_Input_Choosing(InputList inputList, CharInput input) {return false;}
    // >>>>>>>>>
}
