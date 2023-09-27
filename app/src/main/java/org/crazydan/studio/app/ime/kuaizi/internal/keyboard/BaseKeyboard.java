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
import org.crazydan.studio.app.ime.kuaizi.internal.key.CharKey;
import org.crazydan.studio.app.ime.kuaizi.internal.key.CtrlKey;
import org.crazydan.studio.app.ime.kuaizi.internal.key.InputWordKey;
import org.crazydan.studio.app.ime.kuaizi.internal.key.SymbolKey;
import org.crazydan.studio.app.ime.kuaizi.internal.keyboard.keytable.LocatorKeyTable;
import org.crazydan.studio.app.ime.kuaizi.internal.keyboard.keytable.SymbolEmojiKeyTable;
import org.crazydan.studio.app.ime.kuaizi.internal.keyboard.state.ChoosingEmojiStateData;
import org.crazydan.studio.app.ime.kuaizi.internal.keyboard.state.ChoosingSymbolStateData;
import org.crazydan.studio.app.ime.kuaizi.internal.keyboard.state.LocatingInputCursorStateData;
import org.crazydan.studio.app.ime.kuaizi.internal.keyboard.state.PagingStateData;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.InputEditAction;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.InputMsg;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.InputMsgData;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.InputMsgListener;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.Motion;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.UserInputMsg;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.UserInputMsgData;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.UserKeyMsg;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.UserKeyMsgData;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.input.InputAudioPlayingMsgData;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.input.InputCharsInputtingMsgData;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.input.InputCommonMsgData;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.input.InputListCommittingMsgData;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.input.InputListPairSymbolCommittingMsgData;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.input.InputTargetCursorLocatingMsgData;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.input.InputTargetEditingMsgData;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.input.KeyboardHandModeSwitchingMsgData;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.input.KeyboardSwitchingMsgData;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.user.UserFingerFlippingMsgData;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.user.UserSingleTapMsgData;

/**
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-06-28
 */
public abstract class BaseKeyboard implements Keyboard {
    private final Set<InputMsgListener> inputMsgListeners = new HashSet<>();
    protected final PinyinDictDB pinyinDict = PinyinDictDB.getInstance();

    protected State state = new State(State.Type.Input_Waiting);

    private Config config;

    /** 输入列表 */
    private InputList inputList;

    @Override
    public void setConfig(Config config) {
        this.config = config;
    }

    @Override
    public Config getConfig() {
        return this.config;
    }

    @Override
    public void reset() {
        end_InputChars_Inputting_and_Waiting_Input(null);
    }

    @Override
    public void destroy() {
        if (this.inputList != null) {
            this.inputList.removeUserInputMsgListener(this);
        }
        this.inputMsgListeners.clear();
        this.config = null;
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

    /** 触发 {@link InputMsg} 消息 */
    public void fireInputMsg(InputMsg msg, InputMsgData data) {
        // Note: 存在在监听未执行完毕便移除监听的情况，故，在监听列表的副本中执行监听
        Set<InputMsgListener> listeners = new HashSet<>(this.inputMsgListeners);
        listeners.forEach(listener -> listener.onInputMsg(msg, data));
    }

    @Override
    public KeyFactory getKeyFactory() {
        switch (this.state.type) {
            case InputTarget_Cursor_Locating: {
                LocatorKeyTable keyTable = LocatorKeyTable.create(createKeyTableConfigure());

                return keyTable::createKeys;
            }
            case Emoji_Choosing: {
                SymbolEmojiKeyTable keyTable = SymbolEmojiKeyTable.create(createKeyTableConfigure());

                ChoosingEmojiStateData stateData = (ChoosingEmojiStateData) this.state.data;

                return () -> keyTable.createEmojiKeys(stateData.getGroups(),
                                                      stateData.getPagingData(),
                                                      stateData.getGroup(),
                                                      stateData.getPageStart());
            }
            case Symbol_Choosing: {
                SymbolEmojiKeyTable keyTable = SymbolEmojiKeyTable.create(createKeyTableConfigure());

                ChoosingSymbolStateData stateData = (ChoosingSymbolStateData) this.state.data;

                return () -> keyTable.createSymbolKeys(stateData.getGroup(),
                                                       stateData.isOnlyPair(),
                                                       stateData.getPageStart());
            }
        }
        return doGetKeyFactory();
    }

    protected abstract KeyFactory doGetKeyFactory();

    protected KeyTable.Config createKeyTableConfigure() {
        return new KeyTable.Config(getConfig(),
                                   !getInputList().isEmpty(),
                                   getInputList().canRevokeCommit(),
                                   !getInputList().isGapSelected());
    }

    @Override
    public void onUserInputMsg(UserInputMsg msg, UserInputMsgData data) {
        switch (this.state.type) {
            case InputCandidate_Choosing:
                switch (msg) {
                    case Cleaning_Inputs:
                    case Canceling_Cleaning_Inputs:
                        goto_InitState();
                        break;
                }
            case Input_Waiting:
            case Emoji_Choosing:
            case Symbol_Choosing:
                switch (msg) {
                    case Choosing_Input: {
                        onChoosingInputMsg(data.target);
                        break;
                    }
                    case Cleaning_Inputs: {
                        clean_InputList();
                        break;
                    }
                    case Canceling_Cleaning_Inputs: {
                        cancel_Cleaning_InputList();
                        break;
                    }
                }
                break;
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
            case Input_Waiting: {
                break;
            }
            case InputTarget_Cursor_Locating: {
                if (key instanceof CtrlKey) {
                    on_LocatingInputTargetCursor_CtrlKeyMsg(msg, (CtrlKey) key, data);
                }
                return true;
            }
            case Emoji_Choosing: {
                if (msg == UserKeyMsg.FingerFlipping) {
                    on_ChoosingEmoji_PageFlippingMsg(msg, key, data);
                } else if (key instanceof InputWordKey) {
                    on_ChoosingEmoji_InputWordKeyMsg(msg, (InputWordKey) key, data);
                } else if (key instanceof CtrlKey) {
                    on_ChoosingEmoji_CtrlKeyMsg(msg, (CtrlKey) key, data);
                }
                return true;
            }
            case Symbol_Choosing: {
                if (msg == UserKeyMsg.FingerFlipping) {
                    on_ChoosingSymbol_PageFlippingMsg(msg, key, data);
                } else if (key instanceof SymbolKey) {
                    on_ChoosingSymbol_SymbolKeyMsg(msg, (SymbolKey) key, data);
                } else if (key instanceof CtrlKey) {
                    on_ChoosingSymbol_CtrlKeyMsg(msg, (CtrlKey) key, data);
                }
                return true;
            }
        }

        return false;
    }

    protected State getInitState() {
        return new State(State.Type.Input_Waiting);
    }

    protected void goto_InitState() {
        this.state = getInitState();
    }

    /** 当前字符输入已完成，并进入 {@link State.Type#Input_Waiting} 状态 */
    protected void end_InputChars_Inputting_and_Waiting_Input(Key<?> key) {
        goto_InitState();

        end_InputChars_Inputting(key);
    }

    /**
     * 当前输入设置唯一按键并{@link #confirm_Pending_and_Waiting_Input 确认当前输入}
     */
    protected void input_Only_Key_and_Confirm_Pending(Key<?> key) {
        getInputList().newPending().appendKey(key);

        confirm_Pending_and_Waiting_Input(key);
    }

    /** 确认当前输入，并进入 {@link State.Type#Input_Waiting} 状态 */
    protected void confirm_Pending_and_Waiting_Input(Key<?> key) {
        goto_InitState();

        confirm_Pending(key);
    }

    /**
     * 确认当前字符输入，并移至下一个 Gap 输入，再进入 {@link State.Type#Input_Waiting} 状态
     * <p/>
     *
     * @param key
     *         选中 Gap 输入需添加的按键
     */
    protected void confirm_CharInput_Pending_and_MoveTo_NextGapInput(Key<?> key) {
        if (!getInputList().isGapSelected()) {
            getInputList().selectNext();

            if (key != null) {
                getInputList().newPending().appendKey(key);
            }
        }

        confirm_Pending(key);
    }

    /**
     * 确认当前字符输入，并移至下一个 Gap 输入，再进入 {@link State.Type#Input_Waiting} 状态
     * <p/>
     *
     * @param key
     *         选中 Gap 输入需添加的按键
     */
    protected void confirm_CharInput_Pending_and_MoveTo_NextGapInput_then_Waiting_Input(Key<?> key) {
        goto_InitState();

        confirm_CharInput_Pending_and_MoveTo_NextGapInput(key);
    }

    /** 确认当前输入，并移至下一个字符输入，再进入 {@link State.Type#Input_Waiting} 状态 */
    protected void confirm_Pending_and_MoveTo_NextCharInput_then_Waiting_Input(Key<?> key) {
        goto_InitState();

        confirm_Pending_and_MoveTo_NextCharInput(key);
    }

    /**
     * 当前输入设置唯一按键并直接{@link #commit_InputList 提交输入列表（不可撤销）}，
     * 并进入 {@link State.Type#Input_Waiting} 状态
     */
    protected void input_Only_Key_and_Commit_InputList(Key<?> key, boolean needToBeReplaced) {
        getInputList().newPending().appendKey(key);

        goto_InitState();

        commit_InputList(getInputList(), false, needToBeReplaced);
    }

    /**
     * {@link #commit_InputList 提交输入列表（可撤销）}，
     * 并进入 {@link State.Type#Input_Waiting} 状态
     */
    protected void commit_InputList_and_Waiting_Input() {
        goto_InitState();

        commit_InputList(getInputList(), true, false);
    }

    /**
     * 在连续输入中添加单个按键后触发 {@link InputMsg#InputChars_Inputting} 消息
     * <p/>
     * 注：键盘的{@link #state 状态}始终为 {@link State.Type#Input_Waiting}
     */
    protected void fire_and_Waiting_Continuous_InputChars_Inputting(Key<?> key) {
        goto_InitState();

        fire_InputChars_Inputting(getKeyFactory(), key);
    }

    /** 回到前序状态 */
    protected void back_To_Previous_State(Key<?> key) {
        if (this.state.previous == null) {
            end_InputChars_Inputting_and_Waiting_Input(key);
            return;
        }

        this.state = this.state.previous;
        end_InputChars_Inputting(key);
    }

    /**
     * 触发 {@link InputMsg#InputChars_Inputting} 消息
     * <p/>
     * 注：键盘的{@link #state 状态}不变
     */
    protected void fire_InputChars_Inputting(Keyboard.KeyFactory keyFactory, Key<?> key) {
        InputMsgData data = new InputCharsInputtingMsgData(keyFactory, key);

        fireInputMsg(InputMsg.InputChars_Inputting, data);
    }

    /** 当前字符输入已完成，且保持状态不变 */
    protected void end_InputChars_Inputting(Key<?> key) {
        fireInputMsg(InputMsg.InputChars_InputtingEnd, new InputCharsInputtingMsgData(getKeyFactory(), key));
    }

    /** 确认当前输入，且状态保持不变 */
    protected void confirm_Pending(Key<?> key) {
        getInputList().confirmPending();

        end_InputChars_Inputting(key);
    }

    /** 确认当前输入并移至下一个输入，且状态保持不变 */
    protected void confirm_Pending_and_MoveTo_NextCharInput(Key<?> key) {
        getInputList().confirmPending();
        getInputList().moveToNextCharInput();

        end_InputChars_Inputting(key);
    }

    /**
     * 确认回车或空格的控制按键输入，且状态保持不变
     * <p/>
     * 为回车时，直接提交当前输入或在目标输入组件中输入换行；
     * 为空格时，当输入列表为空时，直接向目标输入组件输入空格，
     * 否则，将空格附加到输入列表中
     */
    protected void confirm_Input_Enter_or_Space(InputList inputList, CtrlKey key) {
        boolean isEmpty = inputList.isEmpty();

        if (isEmpty) {
            switch (key.getType()) {
                case Enter:
                case Space:
                    // Note：直输换行和回车后，不再支持输入撤回
                    inputList.cleanCommitRevokes();

                    InputMsgData data = new InputListCommittingMsgData(getKeyFactory(), key.getText());
                    fireInputMsg(InputMsg.InputList_Committing, data);
                    break;
            }
        }
        // 输入列表不为空且不是 Enter 按键时，将其添加到输入列表中
        else if (key.getType() != CtrlKey.Type.Enter) {
            inputList.newPending().appendKey(key);
            getInputList().confirmPending();

            fire_InputChars_Inputting(getKeyFactory(), key);
        }
    }

    /** 清空输入列表，且状态保持不变 */
    protected void clean_InputList() {
        fireInputMsg(InputMsg.InputList_Cleaning, new InputCommonMsgData(getKeyFactory()));
    }

    /** 撤销清空输入列表，且状态保持不变 */
    protected void cancel_Cleaning_InputList() {
        fireInputMsg(InputMsg.InputList_Cleaned_Canceling, new InputCommonMsgData(getKeyFactory()));
    }

    /** 提交输入列表，且状态保持不变 */
    protected void commit_InputList(InputList inputList, boolean canBeRevoked, boolean needToBeReplaced) {
        commit_InputList(inputList, canBeRevoked, needToBeReplaced, false);
    }

    /** 提交输入列表，且状态保持不变 */
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

            // Note：输入提交按钮会根据输入内容确定按钮状态，故，需要回传 KeyFactory 以重新渲染按键
            InputMsgData data = new InputListPairSymbolCommittingMsgData(getKeyFactory(),
                                                                         left.getText(),
                                                                         right.getText());
            fireInputMsg(InputMsg.InputList_PairSymbol_Committing, data);
        } else {
            List<String> replacements = null;
            if (needToBeReplaced) {
                CharInput input = inputList.getLastCharInput();
                CharKey key = (CharKey) input.getLastKey();

                replacements = key.getReplacements();
            }

            StringBuilder text = inputList.commit(canBeRevoked);

            // Note：输入提交按钮会根据输入内容确定按钮状态，故，需要回传 KeyFactory 以重新渲染按键
            InputMsgData data = new InputListCommittingMsgData(getKeyFactory(), text, replacements);
            fireInputMsg(InputMsg.InputList_Committing, data);
        }
    }

    /** 在 {@link #commit_InputList 输入列表提交} 之前需要做的事情 */
    protected void before_Commit_InputList(InputList inputList) {}

    /** 撤回输入列表，且状态保持不变 */
    protected void revoke_Committed_InputList(InputList inputList) {
        if (!inputList.canRevokeCommit()) {
            return;
        }

        before_Revoke_Committed_InputList(inputList);

        inputList.revokeCommit();

        // Note：输入撤回按钮会根据输入内容确定按钮状态，故，需要回传 KeyFactory 以重新渲染按键
        InputMsgData data = new InputCommonMsgData(getKeyFactory());
        fireInputMsg(InputMsg.InputList_Committed_Revoking, data);
    }

    /** 在 {@link #revoke_Committed_InputList 输入列表撤回提交} 之前需要做的事情 */
    protected void before_Revoke_Committed_InputList(InputList inputList) {}

    /**
     * 回删输入列表中的输入或输入目标中的内容，且状态保持不变
     * <p/>
     * 输入列表不为空时，在输入列表中做删除，否则，在输入目标中做删除
     */
    protected void backspace_InputList_or_InputTarget(InputList inputList, Key<?> key) {
        inputList.cleanCommitRevokes();

        if (!inputList.isEmpty()) {
            inputList.deleteBackward();

            end_InputChars_Inputting(key);
        } else {
            backspace_InputTarget();
        }
    }

    /**
     * 回删输入目标中的内容，且状态保持不变
     */
    protected void backspace_InputTarget() {
        do_InputTarget_Editing(InputEditAction.backspace);
    }

    /** 为状态数据做翻页处理 */
    protected void flip_Page_for_PagingState(PagingStateData<?> stateData, UserFingerFlippingMsgData data) {
        Motion motion = data.motion;
        boolean pageUp = motion.direction == Motion.Direction.up || motion.direction == Motion.Direction.left;

        boolean needPaging = pageUp ? stateData.nextPage() : stateData.prevPage();
        if (needPaging) {
            play_InputPageFlipping_Audio();
        }
    }

    /** 切换到指定类型的键盘 */
    protected void switch_Keyboard(Keyboard.Type target) {
        goto_InitState();

        fireInputMsg(InputMsg.Keyboard_Switching, new KeyboardSwitchingMsgData(getConfig().getType(), target));
    }

    /** 切换到先前的键盘，也就是从哪个键盘切过来的，就切回到哪个键盘 */
    protected void switchTo_Previous_Keyboard() {
        switch_Keyboard(getConfig().getSwitchFromType());
    }

    /** 切换键盘的左右手模式 */
    protected void switch_HandMode() {
        goto_InitState();

        switch (getConfig().getHandMode()) {
            case Left:
                getConfig().setHandMode(HandMode.Right);
                break;
            case Right:
                getConfig().setHandMode(HandMode.Left);
                break;
        }

        fireInputMsg(InputMsg.HandMode_Switching,
                     new KeyboardHandModeSwitchingMsgData(getKeyFactory(), getConfig().getHandMode()));
    }

    /** 切换系统输入法 */
    protected void switch_IME() {
        // 单次操作，直接重置为待输入状态
        reset();

        fireInputMsg(InputMsg.IME_Switching, new InputCommonMsgData());
    }

    /** 返回到原状态或前序键盘 */
    protected void do_Exit(Key<?> key) {
        if (this.state.previous == null) {
            switchTo_Previous_Keyboard();
        } else {
            back_To_Previous_State(key);
        }
    }

    /** 播放输入单击音效 */
    protected void play_InputtingSingleTick_Audio(Key<?> key) {
        fire_InputAudio_Playing(key, InputAudioPlayingMsgData.AudioType.SingleTick);
    }

    /** 播放输入双击音效 */
    protected void play_InputtingDoubleTick_Audio(Key<?> key) {
        fire_InputAudio_Playing(key, InputAudioPlayingMsgData.AudioType.DoubleTick);
    }

    /** 播放输入翻页音效 */
    protected void play_InputPageFlipping_Audio() {
        InputMsgData data = new InputAudioPlayingMsgData(InputAudioPlayingMsgData.AudioType.PageFlip);
        fireInputMsg(InputMsg.InputAudio_Playing, data);
    }

    private void fire_InputAudio_Playing(Key<?> key, InputAudioPlayingMsgData.AudioType audioType) {
        if (key == null //
            || (key instanceof CtrlKey && ((CtrlKey) key).isNoOp())) {
            return;
        }

        InputMsgData data = new InputAudioPlayingMsgData(audioType);
        fireInputMsg(InputMsg.InputAudio_Playing, data);
    }

    /** 尝试处理控制按键消息 */
    protected boolean try_Common_OnCtrlKeyMsg(UserKeyMsg msg, CtrlKey key, UserKeyMsgData data) {
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
                    case LocateInputCursor:
                        return true;
                    // 在任意子键盘中提交输入，都直接回到初始键盘
                    case Commit_InputList: {
                        play_InputtingSingleTick_Audio(key);
                        commit_InputList_and_Waiting_Input();
                        return true;
                    }
                    case DropInput: {
                        switch (this.state.type) {
                            case Emoji_Choosing:
                            case Symbol_Choosing:
                                play_InputtingSingleTick_Audio(key);

                                getInputList().deleteSelected();
                                end_InputChars_Inputting(key);
                                return true;
                        }
                        break;
                    }
                    case RevokeInput: {
                        play_InputtingSingleTick_Audio(key);
                        revoke_Committed_InputList(getInputList());
                        return true;
                    }
                    case Backspace: {
                        play_InputtingSingleTick_Audio(key);
                        backspace_InputList_or_InputTarget(getInputList(), key);
                        return true;
                    }
                    case Space:
                    case Enter: {
                        play_InputtingSingleTick_Audio(key);
                        confirm_Input_Enter_or_Space(getInputList(), key);
                        return true;
                    }
                    // 点击 退出 按钮，则退回到前序状态或原键盘
                    case Exit: {
                        play_InputtingSingleTick_Audio(key);
                        do_Exit(key);
                        return true;
                    }
                    case SwitchIME: {
                        play_InputtingSingleTick_Audio(key);
                        switch_IME();
                        return true;
                    }
                    case SwitchHandMode: {
                        play_InputtingSingleTick_Audio(key);
                        switch_HandMode();
                        return true;
                    }
                    case SwitchToLatinKeyboard: {
                        play_InputtingSingleTick_Audio(key);
                        switch_Keyboard(Type.Latin);
                        return true;
                    }
                    case SwitchToPinyinKeyboard: {
                        play_InputtingSingleTick_Audio(key);
                        switch_Keyboard(Type.Pinyin);
                        return true;
                    }
                    case SwitchToNumberKeyboard: {
                        play_InputtingSingleTick_Audio(key);
                        switch_Keyboard(Type.Number);
                        return true;
                    }
                    case SwitchToMathKeyboard: {
                        play_InputtingSingleTick_Audio(key);
                        switch_Keyboard(Type.Math);
                        return true;
                    }
                    case SwitchToEmojiKeyboard: {
                        play_InputtingSingleTick_Audio(key);
                        start_Emoji_Choosing();
                        return true;
                    }
                    case SwitchToSymbolKeyboard: {
                        play_InputtingSingleTick_Audio(key);
                        start_Symbol_Choosing(false);
                        return true;
                    }
                }
                break;
            }
        }

        // 处理定位按钮
        if (key.getType() == CtrlKey.Type.LocateInputCursor) {
            switch (msg) {
                case KeyLongPressStart: {
                    play_InputtingDoubleTick_Audio(key);

                    do_InputTarget_Cursor_Locating(key, null);

                    return true;
                }
                case FingerFlipping: {
                    play_InputtingSingleTick_Audio(key);

                    // 在定位切换按钮上滑动也可以移动光标，但不修改键盘状态
                    Motion motion = ((UserFingerFlippingMsgData) data).motion;
                    Motion anchor = LocatingInputCursorStateData.createAnchor(motion);

                    InputMsgData idata = new InputTargetCursorLocatingMsgData(key, anchor);
                    fireInputMsg(InputMsg.InputTarget_Cursor_Locating, idata);

                    return true;
                }
            }
        }

        return false;
    }

    // <<<<<< 单字符输入处理逻辑

    /**
     * 单字符输入处理
     * <p/>
     * 对于有替代字符的按键，根据连续点击次数确定替代字符并替换前序按键字符
     */
    protected void start_Single_Key_Inputting(Key<?> key, UserSingleTapMsgData data, boolean directInputting) {
        if (key instanceof CharKey && ((CharKey) key).hasReplacement() && data.tick > 0) {
            do_Single_CharKey_Replacement_Inputting((CharKey) key, data.tick, directInputting);
        } else {
            do_Single_Key_Inputting(key, directInputting);
        }
    }

    protected void do_Single_Key_Inputting(Key<?> key, boolean directInputting) {
        if (directInputting) {
            input_Only_Key_and_Commit_InputList(key, false);
            return;
        }

        if (getInputList().hasEmptyPending()) {
            getInputList().newPending();
        }

        // Note：该类键盘不涉及配对符号的输入，故始终清空配对符号的绑定
        getInputList().clearPairOnSelected();

        if (try_Single_Key_Inputting(key)) {
            return;
        }

        if (key instanceof CharKey) {
            do_Single_CharKey_Inputting((CharKey) key);
        }
    }

    protected void do_Single_CharKey_Inputting(CharKey key) {
        CharInput pending = getInputList().getPending();

        switch (key.getType()) {
            // 若为标点、表情符号，则直接确认输入，不支持连续输入其他字符
            case Emoji:
            case Symbol: {
                boolean isEmpty = getInputList().isEmpty();

                if (!isEmpty) {
                    // 已选中字符输入，则直接替换
                    if (!getInputList().isGapSelected()) {
                        getInputList().newPending().appendKey(key);

                        confirm_Pending_and_MoveTo_NextCharInput_then_Waiting_Input(key);
                    }
                    // 否则，做追加
                    else {
                        input_Only_Key_and_Confirm_Pending(key);
                    }
                } else {
                    // 单个标点、表情则直接提交输入
                    input_Only_Key_and_Commit_InputList(key, false);
                }
                break;
            }
            // 字母、数字可连续输入
            case Number:
            case Alphabet: {
                // Note：非拉丁字符输入不可连续输入，直接对其做替换
                if (!pending.isLatin()) {
                    pending = getInputList().newPending();
                }
                pending.appendKey(key);

                fire_and_Waiting_Continuous_InputChars_Inputting(key);
                break;
            }
        }
    }

    protected void do_Single_CharKey_Replacement_Inputting(CharKey key, int replacementIndex, boolean directInputting) {
        if (directInputting) {
            do_ReplacementKey_for_Committing_InputList(key, replacementIndex);
            return;
        }

        Input<?> input;
        if (key.isSymbol()) {
            // Note：标点符号是独立输入，故，需替换当前位置的前一个标点符号输入
            input = getInputList().getInputBeforeSelected();

            // 对输入列表为空时的标点符号直输输入进行替换
            if (input == null) {
                do_ReplacementKey_for_Committing_InputList(key, replacementIndex);
                return;
            }
        } else {
            input = getInputList().getPending();
        }

        Key<?> lastKey = input.getLastKey();
        if (!key.canReplaceTheKey(lastKey)) {
            // 转为单字符输入
            do_Single_Key_Inputting(key, false);
            return;
        }

        CharKey lastCharKey = (CharKey) lastKey;
        // Note: 在 Input 中的 key 可能不携带 replacement 信息，只能通过当前按键做判断
        String newKeyText = key.nextReplacement(lastCharKey.getText());

        // Note: 按键类型需保留，字符间是否需空格将通过字符类型进行判断
        CharKey newKey = lastCharKey.isSymbol() ? KeyTable.symbolKey(newKeyText) : KeyTable.alphabetKey(newKeyText);
        input.replaceLatestKey(lastCharKey, newKey);

        fire_and_Waiting_Continuous_InputChars_Inputting(newKey);
    }

    protected boolean try_Single_Key_Inputting(Key<?> key) {
        if (!key.isEmoji() && !key.isSymbol()) {
            return false;
        }

        boolean isEmpty = getInputList().isEmpty();
        CharInput pending = getInputList().getPending();

        if (!isEmpty //
            && !getInputList().hasEmptyPending() //
            && pending.isLatin()) {
            // 对于新增的拉丁字符输入，需先提交，再录入新按键
            if (getInputList().isGapSelected()) {
                getInputList().confirmPending();
            }
            // 对于修改为拉丁字符的输入，将光标移到其后继输入（实际为 Gap）
            else {
                getInputList().selectNext();
            }

            if (key instanceof SymbolKey && ((SymbolKey) key).isPair()) {
                prepare_for_PairSymbol_Inputting((Symbol.Pair) ((SymbolKey) key).getSymbol());

                // Note：配对符号输入后不再做连续输入，键盘状态重置为初始状态
                confirm_Pending_and_Waiting_Input(key);
            } else {
                getInputList().newPending().appendKey(key);

                confirm_Pending(key);
            }

            return true;
        }
        return false;
    }

    protected void do_ReplacementKey_for_Committing_InputList(CharKey key, int replacementIndex) {
        String newKeyText = key.getReplacement(replacementIndex);
        CharKey newKey = KeyTable.alphabetKey(newKeyText);
        key.getReplacements().forEach(newKey::withReplacements);

        input_Only_Key_and_Commit_InputList(newKey, true);
    }
    // >>>>>>

    // <<<<<< 输入定位逻辑
    private void do_InputTarget_Cursor_Locating(CtrlKey key, Motion motion) {
        LocatingInputCursorStateData stateData;

        if (this.state.type != State.Type.InputTarget_Cursor_Locating) {
            stateData = new LocatingInputCursorStateData();

            this.state = new State(State.Type.InputTarget_Cursor_Locating, stateData, this.state);
        } else {
            stateData = (LocatingInputCursorStateData) this.state.data;
            stateData.updateLocator(motion);
        }

        InputMsgData data = new InputTargetCursorLocatingMsgData(getKeyFactory(), key, stateData.getLocator());
        fireInputMsg(InputMsg.InputTarget_Cursor_Locating, data);
    }

    private void on_LocatingInputTargetCursor_CtrlKeyMsg(UserKeyMsg msg, CtrlKey key, UserKeyMsgData data) {
        switch (msg) {
            case KeySingleTap: {
                if (key.getType() == CtrlKey.Type.Edit_InputTarget) {
                    play_InputtingSingleTick_Audio(key);

                    CtrlKey.EditInputTargetOption option = (CtrlKey.EditInputTargetOption) key.getOption();
                    do_InputTarget_Editing(option.value());
                }
                break;
            }
            case FingerFlipping:
                Motion motion = ((UserFingerFlippingMsgData) data).motion;
                switch (key.getType()) {
                    case LocateInputCursor_Locator:
                        play_InputtingSingleTick_Audio(key);

                        do_InputTarget_Cursor_Locating(key, motion);
                        break;
                    case LocateInputCursor_Selector:
                        play_InputtingSingleTick_Audio(key);

                        do_InputTarget_Selecting(key, motion);
                        break;
                }
                break;
        }
    }

    private void do_InputTarget_Editing(InputEditAction action) {
        switch (action) {
            case noop:
            case copy:
                // 无需清空待撤回输入数据
                break;
            default:
                getInputList().cleanCommitRevokes();
        }

        InputMsgData data = new InputTargetEditingMsgData(getKeyFactory(), action);
        fireInputMsg(InputMsg.InputTarget_Editing, data);
    }

    private void do_InputTarget_Selecting(CtrlKey key, Motion motion) {
        LocatingInputCursorStateData stateData = (LocatingInputCursorStateData) this.state.data;
        stateData.updateSelector(motion);

        InputMsgData data = new InputTargetCursorLocatingMsgData(key, stateData.getSelector());
        fireInputMsg(InputMsg.InputTarget_Selecting, data);
    }
    // >>>>>>>>

    // <<<<<<<< 表情符号选择逻辑
    protected void start_Emoji_Choosing() {
        SymbolEmojiKeyTable keyTable = SymbolEmojiKeyTable.create(createKeyTableConfigure());
        int pageSize = keyTable.getEmojiKeysPageSize();

        Emojis emojis = this.pinyinDict.getEmojis(pageSize / 2);

        ChoosingEmojiStateData stateData = new ChoosingEmojiStateData(emojis, pageSize);
        this.state = new State(State.Type.Emoji_Choosing, stateData, getInitState());

        String group = null;
        // 若默认分组（常用）的数据为空，则切换到第二个分组
        if (stateData.getPagingData().isEmpty()) {
            group = stateData.getGroups().get(1);
        }
        do_Emoji_Choosing(group);
    }

    private void on_ChoosingEmoji_InputWordKeyMsg(UserKeyMsg msg, InputWordKey key, UserKeyMsgData data) {
        switch (msg) {
            case KeyLongPressTick:
            case KeySingleTap: {
                play_InputtingSingleTick_Audio(key);

                do_Single_Emoji_Inputting(key);
            }
            break;
        }
    }

    private void on_ChoosingEmoji_CtrlKeyMsg(UserKeyMsg msg, CtrlKey key, UserKeyMsgData data) {
        if (msg == UserKeyMsg.KeySingleTap) {
            if (key.getType() == CtrlKey.Type.Toggle_Emoji_Group) {
                play_InputtingSingleTick_Audio(key);

                CtrlKey.TextOption option = (CtrlKey.TextOption) key.getOption();
                do_Emoji_Choosing(option.value());
            }
        }
    }

    private void on_ChoosingEmoji_PageFlippingMsg(UserKeyMsg msg, Key<?> key, UserKeyMsgData data) {
        flip_Page_for_PagingState((PagingStateData<?>) this.state.data, (UserFingerFlippingMsgData) data);

        do_Emoji_Choosing();
    }

    private void do_Emoji_Choosing(String group) {
        ChoosingEmojiStateData stateData = (ChoosingEmojiStateData) this.state.data;
        stateData.setGroup(group);

        do_Emoji_Choosing();
    }

    private void do_Emoji_Choosing() {
        InputMsgData data = new InputCommonMsgData(getKeyFactory());
        fireInputMsg(InputMsg.Emoji_Choosing, data);
    }

    private void do_Single_Emoji_Inputting(InputWordKey key) {
        if (try_Single_Key_Inputting(key)) {
            return;
        }

        InputList inputList = getInputList();
        boolean isEmpty = inputList.isEmpty();
        CharInput pending = inputList.newPending();

        InputWord word = key.getWord();
        pending.appendKey(key);
        pending.setWord(word);

        if (isEmpty) {
            // 直接提交输入
            commit_InputList(inputList, false, false);
        } else {
            // 连续输入
            if (inputList.isGapSelected()) {
                confirm_Pending(key);
            }
            // 替换输入
            else {
                confirm_Pending_and_MoveTo_NextCharInput(key);
            }
        }
    }
    // >>>>>>>>

    // <<<<<<<<<<< 对标点符号的操作
    private void start_Symbol_Choosing(boolean onlyPair) {
        SymbolEmojiKeyTable keyTable = SymbolEmojiKeyTable.create(createKeyTableConfigure());
        int pageSize = keyTable.getSymbolKeysPageSize();

        ChoosingSymbolStateData stateData = new ChoosingSymbolStateData(pageSize, onlyPair);
        this.state = new State(State.Type.Symbol_Choosing, stateData, getInitState());

        SymbolGroup group = SymbolGroup.latin;
        if (getConfig().getType() == Type.Pinyin) {
            group = SymbolGroup.han;
        }
        do_Symbol_Choosing(group);
    }

    private void on_ChoosingSymbol_SymbolKeyMsg(UserKeyMsg msg, SymbolKey key, UserKeyMsgData data) {
        boolean continuous = false;

        switch (msg) {
            case KeyLongPressTick:
                continuous = true;
            case KeySingleTap: {
                play_InputtingSingleTick_Audio(key);

                do_Single_Symbol_Inputting(key, continuous);
                break;
            }
        }
    }

    private void on_ChoosingSymbol_CtrlKeyMsg(UserKeyMsg msg, CtrlKey key, UserKeyMsgData data) {
        if (msg == UserKeyMsg.KeySingleTap) {
            if (key.getType() == CtrlKey.Type.Toggle_Symbol_Group) {
                play_InputtingSingleTick_Audio(key);

                CtrlKey.SymbolGroupOption option = (CtrlKey.SymbolGroupOption) key.getOption();
                do_Symbol_Choosing(option.value());
            }
        }
    }

    private void on_ChoosingSymbol_PageFlippingMsg(UserKeyMsg msg, Key<?> key, UserKeyMsgData data) {
        flip_Page_for_PagingState((PagingStateData<?>) this.state.data, (UserFingerFlippingMsgData) data);

        do_Symbol_Choosing();
    }

    private void do_Symbol_Choosing(SymbolGroup group) {
        ChoosingSymbolStateData stateData = (ChoosingSymbolStateData) this.state.data;
        stateData.setGroup(group);

        do_Symbol_Choosing();
    }

    private void do_Symbol_Choosing() {
        InputMsgData data = new InputCommonMsgData(getKeyFactory());
        fireInputMsg(InputMsg.Symbol_Choosing, data);
    }

    protected void do_Single_Symbol_Inputting(SymbolKey key, boolean continuousInput) {
        if (try_Single_Key_Inputting(key)) {
            return;
        }

        InputList inputList = getInputList();
        boolean isEmpty = inputList.isEmpty();
        boolean isPairSymbolKey = key.isPair();
        CharInput pending = inputList.newPending();

        if (isPairSymbolKey) {
            Symbol.Pair symbol = (Symbol.Pair) key.getSymbol();

            prepare_for_PairSymbol_Inputting(symbol);
        } else {
            pending.appendKey(key);
            pending.clearPair();
        }

        // Note：非连续输入的情况下，配对符号输入后不再做连续输入，键盘状态重置为初始状态
        if (isPairSymbolKey && !continuousInput) {
            goto_InitState();
        }

        if (isEmpty) {
            // 直接提交输入
            commit_InputList(inputList, false, false, isPairSymbolKey);
        } else {
            // 连续输入
            if (inputList.isGapSelected()) {
                confirm_Pending(key);
            }
            // 替换输入
            else {
                confirm_Pending_and_MoveTo_NextCharInput(key);
            }
        }
    }

    private void prepare_for_PairSymbol_Inputting(Symbol.Pair symbol) {
        String left = symbol.left;
        String right = symbol.right;

        prepare_for_PairKey_Inputting(getInputList(),
                                      () -> SymbolKey.create(Symbol.single(left)),
                                      () -> SymbolKey.create(Symbol.single(right)));
    }

    protected void prepare_for_PairKey_Inputting(InputList inputList, Supplier<Key<?>> left, Supplier<Key<?>> right) {
        Input<?> selected = inputList.getSelected();
        CharInput pending = inputList.newPending();

        Key<?> leftKey = left.get();
        Key<?> rightKey = right.get();

        if (selected instanceof CharInput //
            && ((CharInput) selected).hasPair()) {
            CharInput leftInput = pending;
            CharInput rightInput = ((CharInput) selected).getPair();
            int rightInputIndex = inputList.indexOf(rightInput);

            if (inputList.getSelectedIndex() > rightInputIndex) {
                leftInput = rightInput;
                rightInput = pending;
            }

            leftInput.replaceLastKey(leftKey);
            rightInput.replaceLastKey(rightKey);
        } else {
            CharInput leftInput = pending;
            leftInput.appendKey(leftKey);

            // 若当前输入不是 Gap，则其右侧的配对符号需在其右侧的 Gap 中录入
            if (selected instanceof CharInput) {
                int selectedIndex = inputList.getSelectedIndex();
                inputList.newPendingOn(selectedIndex + 1);
            } else {
                inputList.newPending();
            }

            CharInput rightInput = inputList.getPending();
            rightInput.appendKey(rightKey);

            // 绑定配对符号的关联：由任意一方发起绑定即可
            rightInput.setPair(leftInput);

            // 确定右侧配对输入，并将光标移动到该输入左侧的 Gap 位置以确保光标在配对符号的中间位置
            inputList.confirmPending();
            inputList.newPendingOn(inputList.getSelectedIndex() - 2);
        }
    }
    // >>>>>>>>>>>

    // <<<<<<<<< 对输入列表的操作
    protected void onChoosingInputMsg(Input<?> input) {
        getInputList().newPendingOn(input);

        // Note：输入过程中操作和处理的都是 pending
        CharInput pending = getInputList().getPending();

        if (pending.isEmoji()) {
            start_Emoji_Choosing();
        } else if (pending.isSymbol()) {
            boolean hasPair = input instanceof CharInput && ((CharInput) input).hasPair();

            start_Symbol_Choosing(hasPair);
        } else if (pending.isMathExpr()) {
            switch_Keyboard(Type.Math);
        } else if (!do_Choosing_Input_in_InputList(pending)) {
            confirm_Pending_and_Waiting_Input(null);
        }
    }

    /** 已处理时返回 <code>true</code>，否则返回 <code>false</code> 以按默认方式处理 */
    protected boolean do_Choosing_Input_in_InputList(CharInput input) {return false;}
    // >>>>>>>>>
}
