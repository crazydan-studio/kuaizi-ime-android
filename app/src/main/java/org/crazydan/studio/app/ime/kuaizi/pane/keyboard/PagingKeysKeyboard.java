/*
 * 筷字输入法 - 高效编辑需要又好又快的输入法
 * Copyright (C) 2024 Crazydan Studio
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

import org.crazydan.studio.app.ime.kuaizi.pane.InputList;
import org.crazydan.studio.app.ime.kuaizi.pane.Key;
import org.crazydan.studio.app.ime.kuaizi.pane.input.CharInput;
import org.crazydan.studio.app.ime.kuaizi.pane.key.CtrlKey;
import org.crazydan.studio.app.ime.kuaizi.pane.keyboard.state.PagingStateData;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.InputMsgType;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.Motion;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.UserKeyMsg;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.UserKeyMsgType;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.user.UserFingerFlippingMsgData;

import static org.crazydan.studio.app.ime.kuaizi.pane.msg.InputMsgType.InputCandidate_Choose_Doing;
import static org.crazydan.studio.app.ime.kuaizi.pane.msg.InputMsgType.InputCandidate_Choose_Done;

/**
 * 支持按键分页的键盘，
 * 负责统一处理按键的分页和翻页逻辑
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2024-12-11
 */
public abstract class PagingKeysKeyboard extends BaseKeyboard {

    @Override
    public void reset() {
        // Note: 分页键盘无复位状态，直接等待新的 start 调用即可
    }

    @Override
    protected void change_State_to_Init(Key<?> key) {
        // Note: 分页键盘没有初始状态，直接退出即可
        exit_Keyboard(key);
    }

    @Override
    public void onMsg(InputList inputList, UserKeyMsg msg) {
        if (try_On_Common_UserKey_Msg(inputList, msg)) {
            return;
        }
        if (this.state.type != State.Type.InputCandidate_Choose_Doing) {
            return;
        }

        Key<?> key = msg.data.key;
        if (msg.type == UserKeyMsgType.FingerFlipping) {
            on_InputCandidate_Choose_Doing_PageFlipping_Msg(inputList, msg, key);
        } else {
            if (key instanceof CtrlKey) {
                on_InputCandidate_Choose_Doing_CtrlKey_Msg(inputList, msg, (CtrlKey) key);
            } else if (!key.isDisabled()) {
                on_InputCandidate_Choose_Doing_PagingKey_Msg(inputList, msg);
            }
        }
    }

    /** 响应翻页消息，以根据 {@link UserKeyMsg} 更新 {@link PagingStateData 分页数据} */
    protected void on_InputCandidate_Choose_Doing_PageFlipping_Msg(InputList inputList, UserKeyMsg msg, Key<?> key) {
        PagingStateData<?> stateData = (PagingStateData<?>) this.state.data;
        UserFingerFlippingMsgData msgData = (UserFingerFlippingMsgData) msg.data;

        Motion motion = msgData.motion;
        boolean pageUp = motion.direction == Motion.Direction.up || motion.direction == Motion.Direction.left;
        boolean needPaging = pageUp ? stateData.nextPage() : stateData.prevPage();
        if (needPaging) {
            play_PageFlip_InputAudio();
        }

        fire_InputCandidate_Choose_Doing(key);
    }

    /** 响应在翻页数据按键上的消息 */
    protected void on_InputCandidate_Choose_Doing_PagingKey_Msg(InputList inputList, UserKeyMsg msg) {}

    /** 响应在控制按键上的消息 */
    protected void on_InputCandidate_Choose_Doing_CtrlKey_Msg(InputList inputList, UserKeyMsg msg, CtrlKey key) {}

    /** 触发 {@link InputMsgType#InputCandidate_Choose_Doing} 消息 */
    protected void fire_InputCandidate_Choose_Doing(Key<?> key) {
        PagingStateData<?> stateData = (PagingStateData<?>) this.state.data;

        fire_Common_InputMsg(InputCandidate_Choose_Doing, key, stateData.input);
    }

    /** 触发 {@link InputMsgType#InputCandidate_Choose_Done} 消息 */
    protected void fire_InputCandidate_Choose_Done(CharInput input, Key<?> key) {
        fire_Common_InputMsg(InputCandidate_Choose_Done, key, input);
    }
}
