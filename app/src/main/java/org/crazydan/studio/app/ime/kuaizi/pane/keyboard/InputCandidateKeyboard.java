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

import org.crazydan.studio.app.ime.kuaizi.dict.PinyinDict;
import org.crazydan.studio.app.ime.kuaizi.pane.Key;
import org.crazydan.studio.app.ime.kuaizi.pane.KeyboardContext;
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
 * 输入候选字选择键盘，
 * 负责统一处理候选字的分页和翻页逻辑
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2024-12-11
 */
public abstract class InputCandidateKeyboard extends BaseKeyboard {

    protected InputCandidateKeyboard(PinyinDict dict) {
        super(dict);
    }

    @Override
    public void onMsg(KeyboardContext context, UserKeyMsg msg) {
        show_InputCandidate_Popup_when_LongPress_Msg(context, msg);

        if (try_On_Common_UserKey_Msg(context, msg)) {
            return;
        }
        if (this.state.type != State.Type.InputCandidate_Choose_Doing) {
            return;
        }

        Key<?> key = context.key();
        if (msg.type == UserKeyMsgType.FingerFlipping) {
            on_InputCandidate_Choose_Doing_FingerFlipping_Msg(context, msg);
        } else {
            if (key instanceof CtrlKey) {
                on_InputCandidate_Choose_Doing_CtrlKey_Msg(context, msg);
            } else if (!key.isDisabled()) {
                on_InputCandidate_Choose_Doing_PagingKey_Msg(context, msg);
            }
        }
    }

    /** 响应翻页消息，以根据 {@link UserKeyMsg} 更新 {@link PagingStateData 分页数据} */
    protected void on_InputCandidate_Choose_Doing_FingerFlipping_Msg(KeyboardContext context, UserKeyMsg msg) {
        PagingStateData<?> stateData = (PagingStateData<?>) this.state.data;
        UserFingerFlippingMsgData msgData = (UserFingerFlippingMsgData) msg.data;

        Motion motion = msgData.motion;
        boolean pageUp = motion.direction == Motion.Direction.up || motion.direction == Motion.Direction.left;
        boolean needPaging = pageUp ? stateData.nextPage() : stateData.prevPage();
        if (needPaging) {
            play_PageFlip_InputAudio(context);
        }

        fire_InputCandidate_Choose_Doing(context);
    }

    /** 响应在翻页数据按键上的消息 */
    protected void on_InputCandidate_Choose_Doing_PagingKey_Msg(KeyboardContext context, UserKeyMsg msg) {}

    /** 响应在控制按键上的消息 */
    protected void on_InputCandidate_Choose_Doing_CtrlKey_Msg(KeyboardContext context, UserKeyMsg msg) {}

    /** 长按期间手指经过的数据按键，将会为其显示气泡提示 */
    protected void show_InputCandidate_Popup_when_LongPress_Msg(KeyboardContext context, UserKeyMsg msg) {
        switch (msg.type) {
            case LongPress_Key_Start:
            case LongPress_Key_Tick: {
                Key<?> key = context.key();

                if (!(key instanceof CtrlKey)) {
                    String text = key.toString();

                    show_InputChars_Input_Popup(context, text, false);
                }
                break;
            }
            case LongPress_Key_Stop: {
                hide_InputChars_Input_Popup(context);
                break;
            }
        }
    }

    /** 触发 {@link InputMsgType#InputCandidate_Choose_Doing} 消息 */
    protected void fire_InputCandidate_Choose_Doing(KeyboardContext context) {
        PagingStateData<?> stateData = (PagingStateData<?>) this.state.data;

        fire_Common_InputMsg(context, InputCandidate_Choose_Doing, stateData.input);
    }

    /** 触发 {@link InputMsgType#InputCandidate_Choose_Done} 消息 */
    protected void fire_InputCandidate_Choose_Done(KeyboardContext context, CharInput input) {
        fire_Common_InputMsg(context, InputCandidate_Choose_Done, input);
    }
}
