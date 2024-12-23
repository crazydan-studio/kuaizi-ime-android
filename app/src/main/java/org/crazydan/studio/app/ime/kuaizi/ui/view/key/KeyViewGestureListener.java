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

package org.crazydan.studio.app.ime.kuaizi.ui.view.key;

import android.graphics.PointF;
import org.crazydan.studio.app.ime.kuaizi.common.widget.ViewGestureDetector;
import org.crazydan.studio.app.ime.kuaizi.pane.Key;
import org.crazydan.studio.app.ime.kuaizi.pane.Keyboard;
import org.crazydan.studio.app.ime.kuaizi.pane.key.CtrlKey;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.UserKeyMsgListenerTrigger;
import org.crazydan.studio.app.ime.kuaizi.ui.view.KeyboardView;

/**
 * {@link Keyboard 键盘}{@link KeyViewHolder 按键}的手势监听器
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-07-13
 */
public class KeyViewGestureListener extends UserKeyMsgListenerTrigger implements ViewGestureDetector.Listener {
    private final KeyboardView keyboardView;
    private KeyViewHolder<?, ?> prevKeyView;
    private KeyViewHolder<?, ?> movingOverXPadKeyView;

    public KeyViewGestureListener(KeyboardView keyboardView) {
        super(keyboardView);
        this.keyboardView = keyboardView;
    }

    @Override
    public void onGesture(ViewGestureDetector.GestureType type, ViewGestureDetector.GestureData data) {
        KeyViewHolder<?, ?> keyView = this.keyboardView.findVisibleKeyViewUnderLoose(data.x, data.y);

        KeyViewHolder<?, ?> oldKeyView = this.prevKeyView;
        this.prevKeyView = keyView;

        // 处理普通键盘的按键发生切换的情况
        if (oldKeyView != null && oldKeyView != keyView) {
            onPressEnd(oldKeyView, ViewGestureDetector.GestureType.PressEnd, data);
        }

        // Note：需要处理 MovingEnd 事件发生在 X 面板以外的情况
        if (try_OnXPadGesture(keyView, type, data)) {
            switch (type) {
                case MovingStart:
                    this.movingOverXPadKeyView = keyView;
                    break;
                case PressEnd:
                case MovingEnd:
                    this.movingOverXPadKeyView = null;
                    break;
            }
            return;
        } else {
            KeyViewHolder<?, ?> xPadKeyView = this.movingOverXPadKeyView;
            switch (type) {
                case PressEnd:
                    this.movingOverXPadKeyView = null;
                case MovingEnd:
                    try_OnXPadGesture(xPadKeyView, type, data);
                    break;
            }
        }

        // 处理普通键盘的按键按压和弹起
        switch (type) {
            case PressStart: {
                onPressStart(keyView, type, data);
                return;
            }
            case PressEnd: {
                onPressEnd(keyView, type, data);
                return;
            }
        }

        // 触发 UserKeyMsgListener
        Key<?> key = getKey(keyView);
        super.onGesture(key, type, data);
    }

    private boolean try_OnXPadGesture(
            KeyViewHolder<?, ?> keyView, ViewGestureDetector.GestureType type, ViewGestureDetector.GestureData data
    ) {
        if (!(keyView instanceof XPadKeyViewHolder)) {
            return false;
        }

        float x = keyView.itemView.getX();
        float y = keyView.itemView.getY();
        PointF offset = new PointF(-x, -y);

        boolean disableTrailer = this.keyboardView.isGestureTrailerDisabled();
        ((XPadKeyViewHolder) keyView).getXPad().onGesture(this, type, data, offset, disableTrailer);

        return true;
    }

    private void onPressStart(
            KeyViewHolder<?, ?> keyView, ViewGestureDetector.GestureType type, ViewGestureDetector.GestureData data
    ) {
        if (isAvailableKeyView(keyView)) {
            keyView.touchDown();
        }

        super.onGesture(getKey(keyView), type, data);
    }

    private void onPressEnd(
            KeyViewHolder<?, ?> keyView, ViewGestureDetector.GestureType type, ViewGestureDetector.GestureData data
    ) {
        if (isAvailableKeyView(keyView)) {
            keyView.touchUp();
        }

        super.onGesture(getKey(keyView), type, data);
    }

    private Key<?> getKey(KeyViewHolder<?, ?> keyView) {
        return keyView != null ? keyView.getData() : null;
    }

    private boolean isAvailableKeyView(KeyViewHolder<?, ?> keyView) {
        return keyView != null //
               && (!(keyView instanceof CtrlKeyViewHolder) //
                   || !(CtrlKey.isNoOp(keyView.getData()))) //
               && !keyView.getData().isDisabled();
    }
}
