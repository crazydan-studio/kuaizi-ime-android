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

package org.crazydan.studio.app.ime.kuaizi.internal.view;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.os.Handler;
import android.view.MotionEvent;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

/**
 * {@link RecyclerView} 的手势检测器
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-07-13
 */
public class RecyclerViewGestureDetector implements RecyclerView.OnItemTouchListener {
    /** 确定长按的超时时间 */
    private static final long LONG_PRESS_TIMEOUT_MILLS = 200;
    /** 确定滑动的超时时间 */
    private static final long SLIPPING_TIMEOUT_MILLS = 400;

    private final Set<Listener> listeners = new HashSet<>();

    // https://stackoverflow.com/questions/6519748/how-to-determine-a-long-touch-on-android/24050544#24050544
    private final Handler longPressListenerHandler = new Handler();
    private final List<GestureData> movingTracker = new ArrayList<>();

    private Runnable longPressListener;
    private boolean longPressing;
    private boolean moving;

    /** 绑定到 {@link RecyclerView} 上 */
    public RecyclerViewGestureDetector bind(RecyclerView view) {
        view.addOnItemTouchListener(this);
        return this;
    }

    public RecyclerViewGestureDetector addListener(Listener listener) {
        this.listeners.add(listener);
        return this;
    }

    @Override
    public boolean onInterceptTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {
        // 启用对触屏事件的拦截支持
        return true;
    }

    @Override
    public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {
    }

    @Override
    public void onTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {
        GestureData data = GestureData.from(e);

        switch (e.getAction()) {
            case MotionEvent.ACTION_DOWN: {
                onPressStart(data);
                startLongPress(data);
                break;
            }
            case MotionEvent.ACTION_MOVE: {
                // Note: 移动开始时，可能还未触发长按监听，故，需显式取消长按监听
                if (!this.longPressing) {
                    stopLongPress();
                }

                onMoving(data);
                break;
            }
            case MotionEvent.ACTION_UP: {
                if (!this.longPressing //
                    && !this.moving) {
                    onSingleTap(data);
                } else if (!this.longPressing && isSlipping()) {
                    onSlipping(data);
                }

                onGestureEnd(data);
                break;
            }
            case MotionEvent.ACTION_CANCEL: {
                onGestureEnd(data);
                break;
            }
        }
    }

    private void onGestureEnd(GestureData data) {
        onPressEnd(data);

        if (this.longPressing) {
            onLongPressEnd(data);
        }
        if (this.moving) {
            onMovingEnd(data);
        }

        stopLongPress();
    }

    private void onPressStart(GestureData data) {
        triggerListeners(GestureType.PressStart, data);
    }

    private void onPressEnd(GestureData data) {
        triggerListeners(GestureType.PressEnd, data);
    }

    private void startLongPress(GestureData data) {
        stopLongPress();

        this.longPressListener = () -> onLongPressStart(data);
        this.longPressListenerHandler.postDelayed(this.longPressListener, LONG_PRESS_TIMEOUT_MILLS);
    }

    private void stopLongPress() {
        if (this.longPressListener != null) {
            this.longPressListenerHandler.removeCallbacks(this.longPressListener);
            this.longPressListener = null;
        }

        this.longPressing = false;
    }

    private void onLongPressStart(GestureData data) {
        this.longPressing = true;
        triggerListeners(GestureType.LongPressStart, data);
    }

    private void onLongPressEnd(GestureData data) {
        this.longPressing = false;
        triggerListeners(GestureType.LongPressEnd, data);
    }

    private void onSingleTap(GestureData data) {
        triggerListeners(GestureType.SingleTap, data);
    }

    private void onMoving(GestureData data) {
        this.moving = true;
        this.movingTracker.add(data);

        triggerListeners(GestureType.Moving, data);
    }

    private void onMovingEnd(GestureData data) {
        this.moving = false;
        this.movingTracker.clear();
    }

    private void onSlipping(GestureData data) {
        int size = this.movingTracker.size();
        GestureData g1 = this.movingTracker.get(0);
        GestureData g2 = this.movingTracker.get(size - 1);

        float dx = g2.x - g1.x;
        float dy = g2.y - g1.y;
        // 忽略水平方向滑动
        if (Math.abs(dy) <= Math.abs(dx)) {
            return;
        }

        boolean upward = dy < 0;
        GestureData newData = new SlippingGestureData(data, upward);

        triggerListeners(GestureType.Slipping, newData);
    }

    private boolean isSlipping() {
        int size = this.movingTracker.size();
        if (size < 2) {
            return false;
        }

        GestureData g1 = this.movingTracker.get(0);
        GestureData g2 = this.movingTracker.get(size - 1);

        return g2.timestamp - g1.timestamp < SLIPPING_TIMEOUT_MILLS;
    }

    private void triggerListeners(GestureType type, GestureData data) {
        for (Listener listener : this.listeners) {
            listener.onGesture(type, data);
        }
    }

    public enum GestureType {
        /** 开始按压 */
        PressStart,
        /** 结束按压 */
        PressEnd,
        /** 开始长按 */
        LongPressStart,
        /** 结束长按 */
        LongPressEnd,
        /** 单击 */
        SingleTap,
        /** 双击 */
        DoubleTap,
        /** 移动: 手指在屏幕上移动 */
        Moving,
        /** 滑动: 在一段时间内完成手指按下、移动到抬起的过程，期间没有其他动作 */
        Slipping,
    }

    public interface Listener {

        void onGesture(GestureType type, GestureData data);
    }

    public static class GestureData {
        public final float x;
        public final float y;
        public final long timestamp;

        private GestureData(float x, float y, long timestamp) {
            this.x = x;
            this.y = y;
            this.timestamp = timestamp;
        }

        public static GestureData from(MotionEvent e) {
            return new GestureData(e.getX(), e.getY(), e.getEventTime());
        }
    }

    public static class SlippingGestureData extends GestureData {
        public final boolean upward;

        public SlippingGestureData(GestureData g, boolean upward) {
            super(g.x, g.y, g.timestamp);
            this.upward = upward;
        }
    }
}
