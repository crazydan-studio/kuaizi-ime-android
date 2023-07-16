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
import org.crazydan.studio.app.ime.kuaizi.internal.msg.UserMsg;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.UserMsgData;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.data.UserFingerMovingMsgData;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.data.UserFingerSlippingMsgData;
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
            onPressEnd(this.prevKeyView);
        }
        this.prevKeyView = keyView;

        switch (type) {
            case PressStart: {
                onPressStart(keyView);
                break;
            }
            case PressEnd: {
                onPressEnd(keyView);
                break;
            }
            case LongPressStart: {
                onLongPressStart(keyView, data);
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
            case Moving: {
                // Note: 靠近检查使用更靠近按键内部的探测方法，以降低拼音滑行输入的后继字母接触的灵敏度
                keyView = this.keyboardView.findVisibleKeyViewUnder(data.x, data.y);
                onMoving(keyView, data);
                break;
            }
            case Slipping: {
                onSlipping(keyView, data);
                break;
            }
        }
    }

    private void onPressStart(KeyView<?, ?> keyView) {
        if (isAvailableKeyView(keyView)) {
            keyView.touchDown();
            this.keyboardView.playTick();
        }
    }

    private void onPressEnd(KeyView<?, ?> keyView) {
        if (isAvailableKeyView(keyView)) {
            keyView.touchUp();
        }
    }

    private Key<?> getKey(KeyView<?, ?> keyView) {
        return keyView != null ? keyView.getKey() : null;
    }

    private void onLongPressStart(KeyView<?, ?> keyView, RecyclerViewGestureDetector.GestureData data) {
        Key<?> targetKey = getKey(keyView);
        if (targetKey != null) {
            UserMsgData msg = new UserMsgData(targetKey);
            this.keyboardView.onUserMsg(UserMsg.KeyLongPressStart, msg);
        }
    }

    private void onLongPressEnd(KeyView<?, ?> keyView, RecyclerViewGestureDetector.GestureData data) {
        UserMsgData msg = new UserMsgData(null);
        this.keyboardView.onUserMsg(UserMsg.KeyLongPressEnd, msg);
    }

    private void onSingleTap(KeyView<?, ?> keyView, RecyclerViewGestureDetector.GestureData data) {
        Key<?> targetKey = getKey(keyView);
        if (targetKey != null) {
            UserMsgData msg = new UserMsgData(targetKey);
            this.keyboardView.onUserMsg(UserMsg.KeySingleTap, msg);
        }
    }

    private void onMoving(KeyView<?, ?> keyView, RecyclerViewGestureDetector.GestureData data) {
        KeyView<?, ?> closedKeyView = this.keyboardView.findVisibleKeyViewNear(data.x, data.y);
        Key<?> targetKey = getKey(keyView);
        Key<?> closedKey = getKey(closedKeyView);

        UserFingerMovingMsgData msg = new UserFingerMovingMsgData(targetKey, closedKey);
        this.keyboardView.onUserMsg(UserMsg.FingerMoving, msg);
    }

    private void onSlipping(KeyView<?, ?> keyView, RecyclerViewGestureDetector.GestureData data) {
        boolean upward = ((RecyclerViewGestureDetector.SlippingGestureData) data).upward;
        UserFingerSlippingMsgData msg = new UserFingerSlippingMsgData(upward);

        this.keyboardView.onUserMsg(UserMsg.FingerSlipping, msg);
    }

    private boolean isAvailableKeyView(KeyView<?, ?> keyView) {
        return keyView != null //
               && (!(keyView instanceof CtrlKeyView) //
                   || !((CtrlKeyView) keyView).getKey().isNoOp()) //
               && !keyView.getKey().isDisabled();
    }
}
