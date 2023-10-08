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

package org.crazydan.studio.app.ime.kuaizi.internal.view.key;

import org.crazydan.studio.app.ime.kuaizi.internal.Key;
import org.crazydan.studio.app.ime.kuaizi.internal.Keyboard;
import org.crazydan.studio.app.ime.kuaizi.internal.key.CtrlKey;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.Motion;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.UserKeyMsg;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.UserKeyMsgData;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.user.UserFingerFlippingMsgData;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.user.UserFingerMovingMsgData;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.user.UserLongPressTickMsgData;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.user.UserSingleTapMsgData;
import org.crazydan.studio.app.ime.kuaizi.internal.view.KeyboardView;
import org.crazydan.studio.app.ime.kuaizi.internal.view.RecyclerViewGestureDetector;

/**
 * {@link Keyboard 键盘}{@link Key 按键}的手势监听器
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-07-13
 */
public class KeyViewGestureListener implements RecyclerViewGestureDetector.Listener {
    private final KeyboardView keyboardView;
    private KeyView<?, ?> prevKeyView;

    public KeyViewGestureListener(KeyboardView keyboardView) {
        this.keyboardView = keyboardView;
    }

    @Override
    public void onGesture(RecyclerViewGestureDetector.GestureType type, RecyclerViewGestureDetector.GestureData data) {
        KeyView<?, ?> keyView = this.keyboardView.findVisibleKeyViewUnderLoose(data.x, data.y);

        if (this.prevKeyView != null && this.prevKeyView != keyView) {
            onPressEnd(this.prevKeyView, data);
        }
        this.prevKeyView = keyView;

        switch (type) {
            case PressStart: {
                onPressStart(keyView, data);
                break;
            }
            case PressEnd: {
                onPressEnd(keyView, data);
                break;
            }
            case LongPressStart: {
                onLongPressStart(keyView, data);
                break;
            }
            case LongPressTick: {
                onLongPressTick(keyView, data);
                break;
            }
            case LongPressEnd: {
                onLongPressEnd(keyView, data);
                break;
            }
            case SingleTap: {
                onSingleTap(keyView, data);
                break;
            }
            case DoubleTap: {
                onDoubleTap(keyView, data);
                break;
            }
            case MovingStart: {
                onMovingStart(keyView, data);
                break;
            }
            case Moving: {
                keyView = this.keyboardView.findVisibleKeyViewUnderLoose(data.x, data.y);
                onMoving(keyView, data);
                break;
            }
            case MovingEnd: {
                onMovingEnd(keyView, data);
                break;
            }
            case Flipping: {
                onFlipping(keyView, data);
                break;
            }
        }
    }

    private void onPressStart(KeyView<?, ?> keyView, RecyclerViewGestureDetector.GestureData data) {
        if (isAvailableKeyView(keyView)) {
            if (keyView.getData().isClickable()) {
                keyView.touchDown();
            }
        }

        onUserKeyMsg(UserKeyMsg.KeyPressStart, keyView, data);
    }

    private void onPressEnd(KeyView<?, ?> keyView, RecyclerViewGestureDetector.GestureData data) {
        if (isAvailableKeyView(keyView)) {
            if (keyView.getData().isClickable()) {
                keyView.touchUp();
            }
        }

        onUserKeyMsg(UserKeyMsg.KeyPressEnd, keyView, data);
    }

    private Key<?> getKey(KeyView<?, ?> keyView) {
        return keyView != null ? keyView.getData() : null;
    }

    private void onLongPressStart(KeyView<?, ?> keyView, RecyclerViewGestureDetector.GestureData data) {
        onUserKeyMsg(UserKeyMsg.KeyLongPressStart, keyView, data);
    }

    private void onLongPressTick(KeyView<?, ?> keyView, RecyclerViewGestureDetector.GestureData data) {
        Key<?> targetKey = getKey(keyView);
        if (targetKey == null) {
            return;
        }

        RecyclerViewGestureDetector.LongPressTickGestureData tickData
                = (RecyclerViewGestureDetector.LongPressTickGestureData) data;
        UserLongPressTickMsgData msgData = new UserLongPressTickMsgData(targetKey, tickData.tick, tickData.duration);

        this.keyboardView.onUserKeyMsg(UserKeyMsg.KeyLongPressTick, msgData);
    }

    private void onLongPressEnd(KeyView<?, ?> keyView, RecyclerViewGestureDetector.GestureData data) {
        UserKeyMsgData msgData = new UserKeyMsgData(null);
        this.keyboardView.onUserKeyMsg(UserKeyMsg.KeyLongPressEnd, msgData);
    }

    private void onSingleTap(KeyView<?, ?> keyView, RecyclerViewGestureDetector.GestureData data) {
        onUserKeyMsg(UserKeyMsg.KeySingleTap, keyView, data);
    }

    private void onDoubleTap(KeyView<?, ?> keyView, RecyclerViewGestureDetector.GestureData data) {
        onUserKeyMsg(UserKeyMsg.KeyDoubleTap, keyView, data);
    }

    private void onMoving(KeyView<?, ?> keyView, RecyclerViewGestureDetector.GestureData data) {
        KeyView<?, ?> closedKeyView = this.keyboardView.findVisibleKeyViewNear(data.x, data.y);
        Key<?> targetKey = getKey(keyView);
        Key<?> closedKey = getKey(closedKeyView);

        Motion motion = ((RecyclerViewGestureDetector.MovingGestureData) data).motion;
        UserFingerMovingMsgData msgData = new UserFingerMovingMsgData(targetKey, closedKey, motion);

        this.keyboardView.onUserKeyMsg(UserKeyMsg.FingerMoving, msgData);
    }

    private void onMovingStart(KeyView<?, ?> keyView, RecyclerViewGestureDetector.GestureData data) {
        onUserKeyMsg(UserKeyMsg.FingerMovingStart, keyView, data);
    }

    private void onMovingEnd(KeyView<?, ?> keyView, RecyclerViewGestureDetector.GestureData data) {
        Key<?> targetKey = getKey(keyView);

        UserKeyMsgData msgData = new UserKeyMsgData(targetKey);
        this.keyboardView.onUserKeyMsg(UserKeyMsg.FingerMovingEnd, msgData);
    }

    private void onFlipping(KeyView<?, ?> keyView, RecyclerViewGestureDetector.GestureData data) {
        Key<?> targetKey = getKey(keyView);

        Motion motion = ((RecyclerViewGestureDetector.FlippingGestureData) data).motion;
        UserFingerFlippingMsgData msgData = new UserFingerFlippingMsgData(targetKey, motion);

        this.keyboardView.onUserKeyMsg(UserKeyMsg.FingerFlipping, msgData);
    }

    private void onUserKeyMsg(UserKeyMsg msg, KeyView<?, ?> keyView, RecyclerViewGestureDetector.GestureData data) {
        Key<?> targetKey = getKey(keyView);
        if (targetKey == null) {
            return;
        }

        UserKeyMsgData msgData = new UserKeyMsgData(targetKey);
        if (data instanceof RecyclerViewGestureDetector.SingleTapGestureData) {
            msgData = new UserSingleTapMsgData(targetKey,
                                               ((RecyclerViewGestureDetector.SingleTapGestureData) data).tick);
        }

        this.keyboardView.onUserKeyMsg(msg, msgData);
    }

    private boolean isAvailableKeyView(KeyView<?, ?> keyView) {
        return keyView != null //
               && (!(keyView instanceof CtrlKeyView) //
                   || !(CtrlKey.isNoOp(keyView.getData()))) //
               && !keyView.getData().isDisabled();
    }
}
