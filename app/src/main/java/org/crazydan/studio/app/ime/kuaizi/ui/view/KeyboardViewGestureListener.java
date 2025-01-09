/*
 * 筷字输入法 - 高效编辑需要又好又快的输入法
 * Copyright (C) 2025 Crazydan Studio <https://studio.crazydan.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.
 * If not, see <https://www.gnu.org/licenses/lgpl-3.0.en.html#license-text>.
 */

package org.crazydan.studio.app.ime.kuaizi.ui.view;

import android.graphics.PointF;
import org.crazydan.studio.app.ime.kuaizi.common.widget.ViewGestureDetector;
import org.crazydan.studio.app.ime.kuaizi.core.Key;
import org.crazydan.studio.app.ime.kuaizi.core.key.CtrlKey;
import org.crazydan.studio.app.ime.kuaizi.core.msg.Motion;
import org.crazydan.studio.app.ime.kuaizi.core.msg.UserKeyMsg;
import org.crazydan.studio.app.ime.kuaizi.core.msg.UserKeyMsgData;
import org.crazydan.studio.app.ime.kuaizi.core.msg.UserKeyMsgType;
import org.crazydan.studio.app.ime.kuaizi.core.msg.user.UserFingerFlippingMsgData;
import org.crazydan.studio.app.ime.kuaizi.core.msg.user.UserFingerMovingMsgData;
import org.crazydan.studio.app.ime.kuaizi.core.msg.user.UserLongPressTickMsgData;
import org.crazydan.studio.app.ime.kuaizi.core.msg.user.UserSingleTapMsgData;
import org.crazydan.studio.app.ime.kuaizi.ui.view.key.KeyViewHolder;
import org.crazydan.studio.app.ime.kuaizi.ui.view.key.XPadKeyViewHolder;

/**
 * {@link KeyboardView} 的手势监听器
 * <p/>
 * 根据收到的手势消息转换为 {@link UserKeyMsg} 并{@link #fire_UserKeyMsg 发送}
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-07-13
 */
public class KeyboardViewGestureListener implements ViewGestureDetector.Listener, KeyboardView.GestureListener {
    private final KeyboardView keyboardView;

    public KeyboardViewGestureListener(KeyboardView keyboardView) {
        this.keyboardView = keyboardView;
    }

    // ======================== Start: 响应按键手势消息 =========================

    @Override
    public void onGesture(
            Key key, ViewGestureDetector.GestureType type, ViewGestureDetector.GestureData data
    ) {
        switch (type) {
            case PressStart: {
                fire_UserKeyMsg_with_NonNullKey(UserKeyMsgType.Press_Key_Start, key, data);
                break;
            }
            case PressEnd: {
                fire_UserKeyMsg_with_NonNullKey(UserKeyMsgType.Press_Key_Stop, key, data);
                break;
            }
            case LongPressStart: {
                fire_UserKeyMsg_with_NonNullKey(UserKeyMsgType.LongPress_Key_Start, key, data);
                break;
            }
            case LongPressTick: {
                fire_UserKeyMsg_with_NonNullKey(UserKeyMsgType.LongPress_Key_Tick, key, data);
                break;
            }
            case LongPressEnd: {
                fire_UserKeyMsg(UserKeyMsgType.LongPress_Key_Stop, null, data);
                break;
            }
            case SingleTap: {
                fire_UserKeyMsg_with_NonNullKey(UserKeyMsgType.SingleTap_Key, key, data);
                break;
            }
            case DoubleTap: {
                fire_UserKeyMsg_with_NonNullKey(UserKeyMsgType.DoubleTap_Key, key, data);
                break;
            }
            case MovingStart: {
                fire_UserKeyMsg_with_NonNullKey(UserKeyMsgType.FingerMoving_Start, key, data);
                break;
            }
            case Moving: {
                fire_UserKeyMsg(UserKeyMsgType.FingerMoving, key, data);
                break;
            }
            case MovingEnd: {
                fire_UserKeyMsg(UserKeyMsgType.FingerMoving_Stop, key, data);
                break;
            }
            case Flipping: {
                fire_UserKeyMsg(UserKeyMsgType.FingerFlipping, key, data);
                break;
            }
        }
    }

    /** 仅在 {@link Key} 不为 null 时才发送 {@link UserKeyMsg} */
    private void fire_UserKeyMsg_with_NonNullKey(
            UserKeyMsgType msgType, Key key, ViewGestureDetector.GestureData data
    ) {
        if (key != null) {
            fire_UserKeyMsg(msgType, key, data);
        }
    }

    /** 发送 {@link UserKeyMsg} 消息，且不限定 {@link Key} 是否为 null */
    private void fire_UserKeyMsg(UserKeyMsgType msgType, Key key, ViewGestureDetector.GestureData data) {
        UserKeyMsgData msgData = new UserKeyMsgData(key);

        switch (msgType) {
            case SingleTap_Key: {
                ViewGestureDetector.SingleTapGestureData tapData = (ViewGestureDetector.SingleTapGestureData) data;
                msgData = new UserSingleTapMsgData(key, tapData.tick);
                break;
            }
            case LongPress_Key_Tick: {
                ViewGestureDetector.LongPressTickGestureData tickData
                        = (ViewGestureDetector.LongPressTickGestureData) data;
                msgData = new UserLongPressTickMsgData(key, tickData.tick, tickData.duration);
                break;
            }
            case FingerMoving: {
                Motion motion = ((ViewGestureDetector.MovingGestureData) data).motion;
                msgData = new UserFingerMovingMsgData(key, motion);
                break;
            }
            case FingerFlipping: {
                Motion motion = ((ViewGestureDetector.FlippingGestureData) data).motion;
                msgData = new UserFingerFlippingMsgData(key, motion);
                break;
            }
        }

        UserKeyMsg msg = new UserKeyMsg(msgType, msgData);
        this.keyboardView.onMsg(msg);
    }

    // ======================== End: 响应按键手势消息 =========================

    // ======================== Start: 响应视图的手势消息 =========================

    private KeyViewHolder<?> prevKeyViewHolder;
    private KeyViewHolder<?> movingOverXPadKeyViewHolder;

    @Override
    public void onGesture(ViewGestureDetector.GestureType type, ViewGestureDetector.GestureData data) {
        KeyViewHolder<?> keyViewHolder = this.keyboardView.findVisibleKeyViewHolderUnderLoose(data.x, data.y);

        KeyViewHolder<?> oldKeyViewHolder = this.prevKeyViewHolder;
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
            KeyViewHolder<?> xPadKeyViewHolder = this.movingOverXPadKeyViewHolder;
            switch (type) {
                case PressEnd:
                    this.movingOverXPadKeyViewHolder = null;
                case MovingEnd:
                    try_OnXPadGesture(xPadKeyViewHolder, type, data);
                    break;
            }
        }

        switch (type) {
            // 处理普通键盘的按键按压和弹起
            case PressStart: {
                onPressStart(keyViewHolder, type, data);
                return;
            }
            case PressEnd: {
                onPressEnd(keyViewHolder, type, data);
                return;
            }
            default: {
                Key key = getKey(keyViewHolder);
                onGesture(key, type, data);
            }
        }
    }

    private boolean try_OnXPadGesture(
            KeyViewHolder<?> holder, ViewGestureDetector.GestureType type, ViewGestureDetector.GestureData data
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
            KeyViewHolder<?> holder, ViewGestureDetector.GestureType type, ViewGestureDetector.GestureData data
    ) {
        Key key = getKey(holder);

        if (isAvailableKey(key)) {
            holder.touchDown();
        }
        onGesture(key, type, data);
    }

    private void onPressEnd(
            KeyViewHolder<?> holder, ViewGestureDetector.GestureType type, ViewGestureDetector.GestureData data
    ) {
        Key key = getKey(holder);

        if (isAvailableKey(key)) {
            holder.touchUp();
        }
        onGesture(key, type, data);
    }

    private boolean isAvailableKey(Key key) {
        return key != null && !CtrlKey.Type.NoOp.match(key) && !key.disabled;
    }

    private Key getKey(KeyViewHolder<?> holder) {
        return this.keyboardView.getAdapterItem(holder);
    }

    // ======================== End: 响应视图的手势消息 =========================
}
