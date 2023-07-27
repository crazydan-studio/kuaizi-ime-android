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
import org.crazydan.studio.app.ime.kuaizi.internal.key.CtrlKey;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.InputMsg;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.InputMsgData;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.InputMsgListener;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.input.InputCommonMsgData;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.input.InputListCommittingMsgData;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.input.InputAudioPlayingMsgData;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.input.KeyboardSwitchingMsgData;

/**
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-06-28
 */
public abstract class BaseKeyboard implements Keyboard {
    private final Set<InputMsgListener> inputMsgListeners = new HashSet<>();

    protected State state = new State(State.Type.Input_Waiting);

    /** 左右手模式 */
    private HandMode handMode = HandMode.Right;

    /** 输入列表 */
    private InputList inputList;

    @Override
    public void reset() {
        end_InputChars_Inputting();
    }

    public HandMode getHandMode() {
        return this.handMode;
    }

    public void setHandMode(HandMode handMode) {
        this.handMode = handMode;
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

    protected KeyTable.Configure createKeyTableConfigure() {
        return new KeyTable.Configure(getHandMode(), !getInputList().isEmpty());
    }

    /** 当前字符输入已完成，等待新的输入 */
    protected void end_InputChars_Inputting() {
        this.state = new State(State.Type.Input_Waiting);

        fireInputMsg(InputMsg.InputChars_InputtingEnd, new InputCommonMsgData(getKeyFactory()));
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
    protected abstract void before_Commit_InputList();

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

    /** 切换到指定类型的输入法 */
    protected void switch_Keyboard(Keyboard.Type type) {
        this.state = new State(State.Type.Input_Waiting);

        fireInputMsg(InputMsg.Keyboard_Switching, new KeyboardSwitchingMsgData(type));
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
}
