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
import org.crazydan.studio.app.ime.kuaizi.pane.key.CtrlKey;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.UserKeyMsgListenerTrigger;
import org.crazydan.studio.app.ime.kuaizi.ui.view.KeyboardView;

/**
 * {@link KeyboardView} 的手势监听器
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-07-13
 */
public class KeyboardViewGestureListener extends UserKeyMsgListenerTrigger implements ViewGestureDetector.Listener {
    private final KeyboardView keyboardView;

    private KeyViewHolder<?, ?> prevKeyViewHolder;
    private KeyViewHolder<?, ?> movingOverXPadKeyViewHolder;

    public KeyboardViewGestureListener(KeyboardView keyboardView) {
        super(keyboardView);
        this.keyboardView = keyboardView;
    }

    @Override
    public void onGesture(ViewGestureDetector.GestureType type, ViewGestureDetector.GestureData data) {
        KeyViewHolder<?, ?> keyViewHolder = this.keyboardView.findVisibleKeyViewHolderUnderLoose(data.x, data.y);

        KeyViewHolder<?, ?> oldKeyViewHolder = this.prevKeyViewHolder;
        this.prevKeyViewHolder = keyViewHolder;

        // 处理普通键盘的按键发生切换的情况
        if (oldKeyViewHolder != null && oldKeyViewHolder != keyViewHolder) {
            onPressEnd(oldKeyViewHolder, ViewGestureDetector.GestureType.PressEnd, data);
        }

        // Note：需要处理 MovingEnd 事件发生在 X 面板以外的情况
        if (try_OnXPadGesture(keyViewHolder, type, data)) {
            switch (type) {
                case MovingStart:
                    this.movingOverXPadKeyViewHolder = keyViewHolder;
                    break;
                case PressEnd:
                case MovingEnd:
                    this.movingOverXPadKeyViewHolder = null;
                    break;
            }
            return;
        } else {
            KeyViewHolder<?, ?> xPadKeyViewHolder = this.movingOverXPadKeyViewHolder;
            switch (type) {
                case PressEnd:
                    this.movingOverXPadKeyViewHolder = null;
                case MovingEnd:
                    try_OnXPadGesture(xPadKeyViewHolder, type, data);
                    break;
            }
        }

        // 处理普通键盘的按键按压和弹起
        switch (type) {
            case PressStart: {
                onPressStart(keyViewHolder, type, data);
                return;
            }
            case PressEnd: {
                onPressEnd(keyViewHolder, type, data);
                return;
            }
        }

        // 触发 UserKeyMsgListener
        Key<?> key = getKey(keyViewHolder);
        super.onGesture(key, type, data);
    }

    private boolean try_OnXPadGesture(
            KeyViewHolder<?, ?> holder, ViewGestureDetector.GestureType type, ViewGestureDetector.GestureData data
    ) {
        if (!(holder instanceof XPadKeyViewHolder)) {
            return false;
        }

        float x = holder.itemView.getX();
        float y = holder.itemView.getY();
        PointF offset = new PointF(-x, -y);

        boolean disableTrailer = this.keyboardView.isGestureTrailerDisabled();
        ((XPadKeyViewHolder) holder).getXPad().onGesture(this, type, data, offset, disableTrailer);

        return true;
    }

    private void onPressStart(
            KeyViewHolder<?, ?> holder, ViewGestureDetector.GestureType type, ViewGestureDetector.GestureData data
    ) {
        if (isAvailableKeyView(holder)) {
            holder.touchDown();
        }

        super.onGesture(getKey(holder), type, data);
    }

    private void onPressEnd(
            KeyViewHolder<?, ?> holder, ViewGestureDetector.GestureType type, ViewGestureDetector.GestureData data
    ) {
        if (isAvailableKeyView(holder)) {
            holder.touchUp();
        }

        super.onGesture(getKey(holder), type, data);
    }

    private Key<?> getKey(KeyViewHolder<?, ?> holder) {
        return holder != null ? holder.getData() : null;
    }

    private boolean isAvailableKeyView(KeyViewHolder<?, ?> holder) {
        return holder != null //
               && (!(holder instanceof CtrlKeyViewHolder) //
                   || !(CtrlKey.isNoOp(holder.getData()))) //
               && !holder.getData().isDisabled();
    }
}
