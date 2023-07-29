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
import java.util.Set;

import org.crazydan.studio.app.ime.kuaizi.internal.InputList;
import org.crazydan.studio.app.ime.kuaizi.internal.Key;
import org.crazydan.studio.app.ime.kuaizi.internal.Keyboard;
import org.crazydan.studio.app.ime.kuaizi.internal.input.CharInput;
import org.crazydan.studio.app.ime.kuaizi.internal.key.CtrlKey;
import org.crazydan.studio.app.ime.kuaizi.internal.keyboard.state.LocatingInputCursorStateData;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.InputMsg;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.InputMsgData;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.InputMsgListener;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.Motion;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.UserKeyMsg;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.UserKeyMsgData;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.input.InputAudioPlayingMsgData;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.input.InputCharsInputtingMsgData;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.input.InputCommonMsgData;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.input.InputListCommittingMsgData;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.input.InputTargetCursorLocatingMsgData;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.input.KeyboardSwitchingMsgData;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.user.UserFingerSlippingMsgData;

/**
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-06-28
 */
public abstract class BaseKeyboard implements Keyboard {
    private final Set<InputMsgListener> inputMsgListeners = new HashSet<>();

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
        end_InputChars_Inputting();
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

    protected KeyTable.Config createKeyTableConfigure() {
        return new KeyTable.Config(getConfig(), !getInputList().isEmpty());
    }

    /**
     * 尝试对 {@link UserKeyMsg} 做处理
     * <p/>
     * 若返回 <code>true</code>，则表示已处理，否则，返回 <code>false</code>
     */
    protected boolean try_OnUserKeyMsg(UserKeyMsg msg, UserKeyMsgData data) {
        Key<?> key = data.target;

        switch (this.state.type) {
            case InputTarget_Cursor_Locating: {
                on_LocatingInputTargetCursor_CtrlKeyMsg(msg, (CtrlKey) key, data);
                return true;
            }
            case Input_Waiting: {
                if (key instanceof CtrlKey) {
                    return try_OnCtrlKeyMsg(msg, (CtrlKey) key, data);
                }
                break;
            }
        }

        return false;
    }

    /** 当前字符输入已完成，等待新的输入 */
    protected void end_InputChars_Inputting() {
        this.state = new State(State.Type.Input_Waiting);

        fireInputMsg(InputMsg.InputChars_InputtingEnd, new InputCommonMsgData(getKeyFactory()));
    }

    /**
     * 触发按键输入消息
     * <p/>
     * 注：键盘的{@link #state 状态}不变
     */
    protected void do_InputChars_Inputting(Keyboard.KeyFactory keyFactory, Key<?> key) {
        InputMsgData data = new InputCharsInputtingMsgData(keyFactory, key);
        fireInputMsg(InputMsg.InputChars_Inputting, data);
    }

    /**
     * 追加按键到持续输入，即，在当前输入上持续添加字符
     * <p/>
     * 注：键盘的{@link #state 状态}不变
     */
    protected void append_Key_for_Continuous_InputChars_Inputting(Key<?> key) {
        if (getInputList().hasEmptyPending()) {
            getInputList().newPending();
        }

        CharInput input = getInputList().getPending();
        input.appendKey(key);

        do_InputChars_Inputting(getKeyFactory(), key);
    }

    /**
     * 添加按键并直接提交输入列表
     */
    protected void append_Key_and_Commit_InputList(Key<?> key) {
        getInputList().newPending().appendKey(key);

        commit_InputList();
    }

    /** 确认当前输入 */
    protected void confirm_InputChars() {
        getInputList().confirmPending();

        end_InputChars_Inputting();
    }

    /**
     * 确认回车或空格的控制按键输入
     * <p/>
     * 为回车时，直接提交当前输入或在目标输入组件中输入换行；
     * 为空格时，当输入列表为空时，直接向目标输入组件输入空格，
     * 否则，将空格附加到输入列表中
     */
    protected void confirm_Input_Enter_or_Space(CtrlKey key) {
        boolean isEmptyInputList = getInputList().isEmpty();

        // 仅在输入列表为空时，才附加回车到输入列表，以便于向目标提交换行符
        if (isEmptyInputList || key.getType() != CtrlKey.Type.Enter) {
            getInputList().newPending().appendKey(key);
        }

        // 输入列表不为空且不是 Enter 按键时，将空格添加到输入列表中
        if (!isEmptyInputList && key.getType() != CtrlKey.Type.Enter) {
            confirm_InputChars();
        }
        // 否则，直接提交按键输入
        else {
            commit_InputList();
        }
    }

    /** 提交输入列表 */
    protected void commit_InputList() {
        this.state = new State(State.Type.Input_Waiting);

        getInputList().confirmPending();
        before_Commit_InputList();

        StringBuilder text = getInputList().getText();
        getInputList().empty();

        InputMsgData data = new InputListCommittingMsgData(getKeyFactory(), text);
        fireInputMsg(InputMsg.InputList_Committing, data);
    }

    /** 在 {@link #commit_InputList() 输入列表提交} 之前需要做的事情 */
    protected void before_Commit_InputList() {}

    /**
     * 回删输入列表中的输入或输入目标中的内容
     * <p/>
     * 输入列表不为空时，在输入列表中做删除，否则，在输入目标中做删除
     */
    protected void backspace_InputList_or_InputTarget() {
        if (!getInputList().isEmpty()) {
            if (!getInputList().hasEmptyPending()) {
                getInputList().dropPending();
            } else {
                getInputList().deleteBackward();
            }
            end_InputChars_Inputting();
        } else {
            backspace_InputTarget();
        }
    }

    /** 回删输入目标中的内容 */
    protected void backspace_InputTarget() {
        // 单次操作，直接重置为待输入状态
        this.state = new State(State.Type.Input_Waiting);

        fireInputMsg(InputMsg.InputTarget_Backspacing, new InputCommonMsgData(getKeyFactory()));
    }

    /** 切换到指定类型的键盘 */
    protected void switch_Keyboard(Keyboard.Type type) {
        this.state = new State(State.Type.Input_Waiting);

        fireInputMsg(InputMsg.Keyboard_Switching, new KeyboardSwitchingMsgData(type));
    }

    /** 切换系统输入法 */
    protected void switch_IME() {
        // 单次操作，直接重置为待输入状态
        reset();

        fireInputMsg(InputMsg.IME_Switching, new InputCommonMsgData(null));
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

    /** 处理 {@link CtrlKey.Type#CommitInputList} 控制按键点击事件 */
    protected void on_CtrlKey_CommitInputList(CtrlKey key) {commit_InputList();}

    /** 处理 {@link CtrlKey.Type#Backspace} 控制按键点击事件 */
    protected void on_CtrlKey_Backspace(CtrlKey key) {backspace_InputList_or_InputTarget();}

    /** 处理 {@link CtrlKey.Type#Space} 或 {@link CtrlKey.Type#Enter} 控制按键点击事件 */
    protected void on_CtrlKey_Space_or_Enter(CtrlKey key) {confirm_Input_Enter_or_Space(key);}

    /** 尝试处理控制按键消息 */
    private boolean try_OnCtrlKeyMsg(UserKeyMsg msg, CtrlKey key, UserKeyMsgData data) {
        switch (msg) {
            case KeyDoubleTap: // 双击继续触发第二次单击操作
            case KeySingleTap: {
                switch (key.getType()) {
                    // 定位按钮不响应单击和双击操作
                    case LocateInputCursor:
                        return true;
                    case CommitInputList: {
                        play_InputtingSingleTick_Audio(key);
                        on_CtrlKey_CommitInputList(key);
                        return true;
                    }
                    case Backspace: {
                        play_InputtingSingleTick_Audio(key);
                        on_CtrlKey_Backspace(key);
                        return true;
                    }
                    case Space:
                    case Enter: {
                        play_InputtingSingleTick_Audio(key);
                        on_CtrlKey_Space_or_Enter(key);
                        return true;
                    }
                    case SwitchIME: {
                        play_InputtingSingleTick_Audio(key);
                        switch_IME();
                        return true;
                    }
                    case SwitchHandMode: {
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
                }
                break;
            }
            case KeyLongPressStart: {
                if (key.getType() == CtrlKey.Type.LocateInputCursor) {
                    play_InputtingDoubleTick_Audio(key);
                    do_InputTarget_Cursor_Locating(key, null);
                    return true;
                }
                break;
            }
            case KeyLongPressTick: {
                switch (key.getType()) {
                    case Backspace:
                    case Space:
                    case Enter:
                        // 长按 tick 视为连续单击
                        return try_OnCtrlKeyMsg(UserKeyMsg.KeySingleTap, key, data);
                }
                break;
            }
            case FingerSlipping: {
                // 在定位切换按钮上滑动也可以移动光标，但不修改键盘状态
                if (key.getType() == CtrlKey.Type.LocateInputCursor) {
                    Motion motion = ((UserFingerSlippingMsgData) data).motion;
                    play_InputtingSingleTick_Audio(key);

                    Motion anchor = LocatingInputCursorStateData.createAnchor(motion);
                    InputMsgData idata = new InputTargetCursorLocatingMsgData(null, key, anchor);
                    fireInputMsg(InputMsg.InputTarget_Cursor_Locating, idata);

                    return true;
                }
                break;
            }
        }

        return false;
    }

    // <<<<<< 输入定位逻辑
    private void on_LocatingInputTargetCursor_CtrlKeyMsg(UserKeyMsg msg, CtrlKey key, UserKeyMsgData data) {
        switch (msg) {
            case KeyDoubleTap: // 双击继续触发第二次单击操作
            case KeySingleTap: {
                switch (key.getType()) {
                    // 点击 退出 按钮，则退回到输入状态
                    case Exit:
                        play_InputtingSingleTick_Audio(key);

                        end_InputChars_Inputting();
                        break;
                    case Backspace:
                        play_InputtingSingleTick_Audio(key);

                        fireInputMsg(InputMsg.InputTarget_Backspacing, new InputCommonMsgData(null));
                        break;
                    case Enter:
                        play_InputtingSingleTick_Audio(key);

                        InputMsgData msgData = new InputListCommittingMsgData(null, "\n");
                        fireInputMsg(InputMsg.InputList_Committing, msgData);
                        break;
                    case Redo:
                        play_InputtingSingleTick_Audio(key);

                        do_InputTarget_Redoing();
                        break;
                    case Undo:
                        play_InputtingSingleTick_Audio(key);

                        do_InputTarget_Undoing();
                        break;
                    case Cut:
                        play_InputtingSingleTick_Audio(key);

                        do_InputTarget_Cutting();
                        break;
                    case Paste:
                        play_InputtingSingleTick_Audio(key);

                        do_InputTarget_Pasting();
                        break;
                    case Copy:
                        play_InputtingSingleTick_Audio(key);

                        do_InputTarget_Copying();
                        break;
                }
                break;
            }
            case KeyLongPressTick: {
                switch (key.getType()) {
                    case Backspace:
                    case Enter:
                        // 长按 tick 视为连续单击
                        on_LocatingInputTargetCursor_CtrlKeyMsg(UserKeyMsg.KeySingleTap, key, data);
                        break;
                }
                break;
            }
            case FingerSlipping:
                // Note: 仅在 按键 上的滑动才有效
                if (key != null) {
                    Motion motion = ((UserFingerSlippingMsgData) data).motion;
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
                }
                break;
        }
    }

    private void do_InputTarget_Cursor_Locating(CtrlKey key, Motion motion) {
        KeyFactory keyFactory = null;
        LocatingInputCursorStateData stateData;

        if (this.state.type != State.Type.InputTarget_Cursor_Locating) {
            stateData = new LocatingInputCursorStateData();
            keyFactory = () -> KeyTable.createLocatorKeys(createKeyTableConfigure());

            this.state = new State(State.Type.InputTarget_Cursor_Locating, stateData);
        } else {
            stateData = (LocatingInputCursorStateData) this.state.data;
            stateData.updateLocator(motion);
        }

        InputMsgData data = new InputTargetCursorLocatingMsgData(keyFactory, key, stateData.getLocator());
        fireInputMsg(InputMsg.InputTarget_Cursor_Locating, data);
    }

    private void do_InputTarget_Selecting(CtrlKey key, Motion motion) {
        LocatingInputCursorStateData stateData = (LocatingInputCursorStateData) this.state.data;
        stateData.updateSelector(motion);

        InputMsgData data = new InputTargetCursorLocatingMsgData(null, key, stateData.getSelector());
        fireInputMsg(InputMsg.InputTarget_Selecting, data);
    }

    private void do_InputTarget_Copying() {
        InputMsgData data = new InputCommonMsgData(null);
        fireInputMsg(InputMsg.InputTarget_Copying, data);
    }

    private void do_InputTarget_Pasting() {
        InputMsgData data = new InputCommonMsgData(null);
        fireInputMsg(InputMsg.InputTarget_Pasting, data);
    }

    private void do_InputTarget_Cutting() {
        InputMsgData data = new InputCommonMsgData(null);
        fireInputMsg(InputMsg.InputTarget_Cutting, data);
    }

    private void do_InputTarget_Undoing() {
        InputMsgData data = new InputCommonMsgData(null);
        fireInputMsg(InputMsg.InputTarget_Undoing, data);
    }

    private void do_InputTarget_Redoing() {
        InputMsgData data = new InputCommonMsgData(null);
        fireInputMsg(InputMsg.InputTarget_Redoing, data);
    }
    // >>>>>>>>
}
