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

package org.crazydan.studio.app.ime.kuaizi.internal.msg;

import org.crazydan.studio.app.ime.kuaizi.internal.Key;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.user.UserFingerFlippingMsgData;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.user.UserFingerMovingMsgData;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.user.UserLongPressTickMsgData;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.user.UserSingleTapMsgData;
import org.crazydan.studio.app.ime.kuaizi.widget.ViewGestureDetector;

/**
 * {@link UserKeyMsgListener} 的{@link UserKeyMsgListener.Trigger 触发器}
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-11-07
 */
public class UserKeyMsgListenerTrigger implements UserKeyMsgListener.Trigger {
    private final UserKeyMsgListener listener;

    public UserKeyMsgListenerTrigger(UserKeyMsgListener listener) {
        this.listener = listener;
    }

    @Override
    public void onGesture(Key<?> key, ViewGestureDetector.GestureType type, ViewGestureDetector.GestureData data) {
        switch (type) {
            case PressStart: {
                onPressStart(key, data);
                break;
            }
            case PressEnd: {
                onPressEnd(key, data);
                break;
            }
            case LongPressStart: {
                onLongPressStart(key, data);
                break;
            }
            case LongPressTick: {
                onLongPressTick(key, data);
                break;
            }
            case LongPressEnd: {
                onLongPressEnd(key, data);
                break;
            }
            case SingleTap: {
                onSingleTap(key, data);
                break;
            }
            case DoubleTap: {
                onDoubleTap(key, data);
                break;
            }
            case MovingStart: {
                onMovingStart(key, data);
                break;
            }
            case Moving: {
                onMoving(key, data);
                break;
            }
            case MovingEnd: {
                onMovingEnd(key, data);
                break;
            }
            case Flipping: {
                onFlipping(key, data);
                break;
            }
        }
    }

    private void onPressStart(Key<?> key, ViewGestureDetector.GestureData data) {
        onUserKeyMsg(UserKeyMsg.KeyPressStart, key, data);
    }

    private void onPressEnd(Key<?> key, ViewGestureDetector.GestureData data) {
        onUserKeyMsg(UserKeyMsg.KeyPressEnd, key, data);
    }

    private void onLongPressStart(Key<?> key, ViewGestureDetector.GestureData data) {
        onUserKeyMsg(UserKeyMsg.KeyLongPressStart, key, data);
    }

    private void onLongPressTick(Key<?> key, ViewGestureDetector.GestureData data) {
        onUserKeyMsg(UserKeyMsg.KeyLongPressTick, key, data);
    }

    private void onLongPressEnd(Key<?> key, ViewGestureDetector.GestureData data) {
        UserKeyMsgData msgData = new UserKeyMsgData(null);
        this.listener.onUserKeyMsg(UserKeyMsg.KeyLongPressEnd, msgData);
    }

    private void onSingleTap(Key<?> key, ViewGestureDetector.GestureData data) {
        onUserKeyMsg(UserKeyMsg.KeySingleTap, key, data);
    }

    private void onDoubleTap(Key<?> key, ViewGestureDetector.GestureData data) {
        onUserKeyMsg(UserKeyMsg.KeyDoubleTap, key, data);
    }

    private void onMoving(Key<?> key, ViewGestureDetector.GestureData data) {
        Motion motion = ((ViewGestureDetector.MovingGestureData) data).motion;
        UserFingerMovingMsgData msgData = new UserFingerMovingMsgData(key, motion);

        this.listener.onUserKeyMsg(UserKeyMsg.FingerMoving, msgData);
    }

    private void onMovingStart(Key<?> key, ViewGestureDetector.GestureData data) {
        onUserKeyMsg(UserKeyMsg.FingerMovingStart, key, data);
    }

    private void onMovingEnd(Key<?> key, ViewGestureDetector.GestureData data) {
        UserKeyMsgData msgData = new UserKeyMsgData(key);
        this.listener.onUserKeyMsg(UserKeyMsg.FingerMovingEnd, msgData);
    }

    private void onFlipping(Key<?> key, ViewGestureDetector.GestureData data) {
        Motion motion = ((ViewGestureDetector.FlippingGestureData) data).motion;
        UserFingerFlippingMsgData msgData = new UserFingerFlippingMsgData(key, motion);

        this.listener.onUserKeyMsg(UserKeyMsg.FingerFlipping, msgData);
    }

    private void onUserKeyMsg(UserKeyMsg msg, Key<?> key, ViewGestureDetector.GestureData data) {
        if (key == null) {
            return;
        }

        UserKeyMsgData msgData = new UserKeyMsgData(key);
        switch (msg) {
            case KeySingleTap:
                ViewGestureDetector.SingleTapGestureData tapData = (ViewGestureDetector.SingleTapGestureData) data;
                msgData = new UserSingleTapMsgData(key, tapData.tick);
                break;
            case KeyLongPressTick:
                ViewGestureDetector.LongPressTickGestureData tickData
                        = (ViewGestureDetector.LongPressTickGestureData) data;
                msgData = new UserLongPressTickMsgData(key, tickData.tick, tickData.duration);
                break;
        }

        this.listener.onUserKeyMsg(msg, msgData);
    }
}
