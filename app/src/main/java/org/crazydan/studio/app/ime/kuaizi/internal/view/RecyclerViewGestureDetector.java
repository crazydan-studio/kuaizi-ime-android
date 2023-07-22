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
import java.util.concurrent.atomic.AtomicBoolean;

import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.Motion;

/**
 * {@link RecyclerView} 的手势检测器
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-07-13
 */
public class RecyclerViewGestureDetector implements RecyclerView.OnItemTouchListener {
    /** 确定长按的超时时间 */
    private static final long LONG_PRESS_TIMEOUT_MILLS = 200;
    /** 确定长按 tick 的超时时间 */
    private static final long LONG_PRESS_TICK_TIMEOUT_MILLS = 150;
    /** 确定双击的超时时间 */
    private static final long DOUBLE_TAP_TIMEOUT_MILLS = 400;
    /** 确定滑动的超时时间 */
    private static final long SLIPPING_TIMEOUT_MILLS = 400;

    private final Set<Listener> listeners = new HashSet<>();

    private final GestureHandler gestureHandler = new GestureHandler();
    private final List<GestureData> movingTracker = new ArrayList<>();

    private final AtomicBoolean longPressing = new AtomicBoolean(false);
    private boolean moving;
    private GestureData latestSingleTap;
    private View prevView;

    /** 绑定到 {@link RecyclerView} 上 */
    public RecyclerViewGestureDetector bind(RecyclerView view) {
        view.addOnItemTouchListener(this);
        return this;
    }

    public RecyclerViewGestureDetector addListener(Listener listener) {
        this.listeners.add(listener);
        return this;
    }

    public void reset() {
        this.longPressing.set(false);
        this.moving = false;
        this.latestSingleTap = null;

        this.gestureHandler.clear();
        this.movingTracker.clear();
    }

    @Override
    public boolean onInterceptTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {
        // Note: onTouchEvent 默认只在该函数返回 true 时才执行，
        // 故，在该函数始终返回 false 时，只能在该函数中执行手势检测处理
        onTouchEvent(rv, e);

        // 始终返回 false 以避免禁用 RecyclerView 的滚动功能
        return false;
    }

    @Override
    public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {
    }

    @Override
    public void onTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {
        GestureData data = GestureData.from(e);

        switch (e.getAction()) {
            case MotionEvent.ACTION_DOWN: {
                // 事件发生的视图做了切换，则需要重置事件监测
                View view = rv.findChildViewUnder(e.getX(), e.getY());
                if (this.prevView != view) {
                    reset();
                }
                this.prevView = view;

                // Note: 优先触发长按监听，以确保其在指定的延时后能够及时执行，
                // 而不会因为后续监听的执行导致其执行被延后
                startLongPress(data);
                onPressStart(data);
                break;
            }
            case MotionEvent.ACTION_MOVE: {
                // Note: 移动开始时，可能还未触发长按监听，故，需显式取消长按监听
                if (!this.longPressing.get()) {
                    stopLongPress();
                }

                onMoving(data);
                break;
            }
            case MotionEvent.ACTION_UP: {
                if (!this.longPressing.get() //
                    && !this.moving) {
                    onSingleTap(data);
                } else if (!this.longPressing.get() && isSlipping()) {
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
        // Note: 先结束带定时任务的事件
        onLongPressEnd(data);
        onMovingEnd(data);

        onPressEnd(data);
    }

    private void onPressStart(GestureData data) {
        triggerListeners(GestureType.PressStart, data);
    }

    private void onPressEnd(GestureData data) {
        triggerListeners(GestureType.PressEnd, data);
    }

    private void startLongPress(GestureData data) {
        stopLongPress();

        Message msg = this.gestureHandler.obtainMessage(GestureHandler.MSG_LONG_PRESS, data);
        this.gestureHandler.sendMessageDelayed(msg, LONG_PRESS_TIMEOUT_MILLS);
    }

    private void startLongPressTick(LongPressTickGestureData data) {
        long timeout = LONG_PRESS_TICK_TIMEOUT_MILLS;
        LongPressTickGestureData newData = new LongPressTickGestureData(data, data.tick + 1, data.duration + timeout);

        Message msg = this.gestureHandler.obtainMessage(GestureHandler.MSG_LONG_PRESS_TICK, newData);
        this.gestureHandler.sendMessageDelayed(msg, timeout);
    }

    private void stopLongPress() {
        this.longPressing.set(false);
        this.gestureHandler.removeMessages(GestureHandler.MSG_LONG_PRESS_TICK);
        this.gestureHandler.removeMessages(GestureHandler.MSG_LONG_PRESS);
    }

    private void onLongPressStart(GestureData data) {
        this.longPressing.set(true);

        GestureData newData = GestureData.newFrom(data);
        triggerListeners(GestureType.LongPressStart, newData);

        // 事件处理完后，再准备首次触发 tick
        LongPressTickGestureData tickData = new LongPressTickGestureData(data, 0, 0);
        startLongPressTick(tickData);
    }

    private void onLongPressTick(LongPressTickGestureData data) {
        if (!this.longPressing.get()) {
            return;
        }

        // 分发当前 tick 事件
        LongPressTickGestureData newData = LongPressTickGestureData.newFrom(data);
        triggerListeners(GestureType.LongPressTick, newData);

        // 事件处理完后，再准备触发下一个 tick
        startLongPressTick(data);
    }

    private void onLongPressEnd(GestureData data) {
        boolean hasLongPressing = this.longPressing.get();
        stopLongPress();

        if (hasLongPressing) {
            triggerListeners(GestureType.LongPressEnd, data);
        }
    }

    private void onSingleTap(GestureData data) {
        GestureData latestData = this.latestSingleTap;
        this.latestSingleTap = data;

        if (latestData != null //
            && data.timestamp - latestData.timestamp < DOUBLE_TAP_TIMEOUT_MILLS) {
            triggerListeners(GestureType.DoubleTap, data);
        } else {
            triggerListeners(GestureType.SingleTap, data);
        }
    }

    private void onMoving(GestureData data) {
        this.moving = true;
        this.movingTracker.add(data);

        int size = this.movingTracker.size();
        GestureData g1 = size > 1 ? this.movingTracker.get(0) : null;
        GestureData g2 = this.movingTracker.get(size - 1);

        Motion motion = createMotion(g2, g1);
        GestureData newData = new MovingGestureData(data, motion);

        if (size == 1) {
            triggerListeners(GestureType.MovingStart, data);
        } else {
            triggerListeners(GestureType.Moving, newData);
        }
    }

    private void onMovingEnd(GestureData data) {
        boolean hasMoving = this.moving;
        this.moving = false;
        this.movingTracker.clear();

        if (hasMoving) {
            triggerListeners(GestureType.MovingEnd, data);
        }
    }

    private void onSlipping(GestureData data) {
        int size = this.movingTracker.size();
        GestureData g1 = this.movingTracker.get(0);
        // Note: g2 应该始终与 data 相同
        GestureData g2 = this.movingTracker.get(size - 1);

        Motion motion = createMotion(g2, g1);
        if (motion.distance <= 0) {
            return;
        }

        // Note: 坐标位置设置为事件初始发生位置
        GestureData newData = new SlippingGestureData(g1, motion);

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

    private Motion createMotion(GestureData newData, GestureData oldData) {
        if (oldData == null) {
            return new Motion(Motion.Direction.none, 0, newData.timestamp);
        }

        long timestamp = newData.timestamp;

        double dx = newData.x - oldData.x;
        double dy = newData.y - oldData.y;
        double distance = Math.sqrt(dx * dx + dy * dy);
        double angle = Math.toDegrees(Math.acos(dx / distance));

        Motion.Direction direction;
        // Note: 屏幕绘图坐标与空间坐标存在上下翻转关系
        //  ----- x
        //  |
        //  |
        //  y
        if (angle >= 45 && angle < 45 + 90) {
            direction = dy > 0 ? Motion.Direction.down : Motion.Direction.up;
        } else if (angle >= 45 + 90 && angle <= 180) {
            direction = Motion.Direction.left;
        } else {
            direction = Motion.Direction.right;
        }

        return new Motion(direction, (int) distance, timestamp);
    }

    public enum GestureType {
        /** 开始按压 */
        PressStart,
        /** 结束按压 */
        PressEnd,
        /** 开始长按 */
        LongPressStart,
        /** 长按 tick */
        LongPressTick,
        /** 结束长按 */
        LongPressEnd,
        /** 单击 */
        SingleTap,
        /** 双击 */
        DoubleTap,
        /** 开始移动 */
        MovingStart,
        /** 移动: 手指在屏幕上移动 */
        Moving,
        /** 结束移动 */
        MovingEnd,
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

        /** 事件位置不变，仅修改时间戳为当前时间 */
        public static GestureData newFrom(GestureData g) {
            return new GestureData(g.x, g.y, SystemClock.uptimeMillis());
        }

        public static GestureData from(MotionEvent e) {
            return new GestureData(e.getX(), e.getY(), e.getEventTime());
        }
    }

    public static class MovingGestureData extends GestureData {
        public final Motion motion;

        public MovingGestureData(GestureData g, Motion motion) {
            super(g.x, g.y, g.timestamp);
            this.motion = motion;
        }
    }

    public static class SlippingGestureData extends GestureData {
        public final Motion motion;

        public SlippingGestureData(GestureData g, Motion motion) {
            super(g.x, g.y, g.timestamp);
            this.motion = motion;
        }
    }

    public static class LongPressTickGestureData extends GestureData {
        public final int tick;
        public final long duration;

        public LongPressTickGestureData(GestureData g, int tick, long duration) {
            super(g.x, g.y, g.timestamp);
            this.tick = tick;
            this.duration = duration;
        }

        /** 事件位置、tick、duration 不变，仅修改时间戳为当前时间 */
        public static LongPressTickGestureData newFrom(LongPressTickGestureData g) {
            return new LongPressTickGestureData(GestureData.newFrom(g), g.tick, g.duration);
        }
    }

    private class GestureHandler extends Handler {
        private static final int MSG_LONG_PRESS = 1;
        private static final int MSG_LONG_PRESS_TICK = 2;

        public void clear() {
            removeMessages(MSG_LONG_PRESS_TICK);
            removeMessages(MSG_LONG_PRESS);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_LONG_PRESS:
                    onLongPressStart((GestureData) msg.obj);
                    break;
                case MSG_LONG_PRESS_TICK:
                    onLongPressTick((LongPressTickGestureData) msg.obj);
                    break;
            }
        }
    }
}
