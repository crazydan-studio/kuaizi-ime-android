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

package org.crazydan.studio.app.ime.kuaizi.common.widget;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.view.MotionEvent;
import androidx.annotation.NonNull;
import org.crazydan.studio.app.ime.kuaizi.common.log.Logger;
import org.crazydan.studio.app.ime.kuaizi.core.msg.Motion;

/**
 * 手势检测器
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-07-13
 */
public class ViewGestureDetector {
    /** 确定长按的超时时间 */
    private static final long LONG_PRESS_TIMEOUT_MILLS = 200;
    /** 确定长按 tick 的超时时间 */
    private static final long LONG_PRESS_TICK_TIMEOUT_MILLS = 100;
    /** 确定双击的超时时间 */
    private static final long DOUBLE_TAP_TIMEOUT_MILLS = 300;
    /** 确定滑动的超时时间 */
    private static final long FLIPPING_TIMEOUT_MILLS = 400;

    protected final Logger log = Logger.getLogger(getClass());

    private final Set<Listener> listeners = new LinkedHashSet<>();

    private final GestureHandler gestureHandler = new GestureHandler();
    private final List<GestureData> movingTracker = new ArrayList<>();

    private final AtomicBoolean longPressing = new AtomicBoolean(false);
    private boolean moving;
    private GestureData latestPressStart;
    private SingleTapGestureData latestSingleTap;

    public interface Listener {
        void onGesture(GestureType type, GestureData data);
    }

    public ViewGestureDetector addListener(Listener listener) {
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

    public void onTouchEvent(@NonNull MotionEvent e) {
        GestureData data = GestureData.from(e);

        switch (e.getAction()) {
            case MotionEvent.ACTION_DOWN: {
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
                // Note: ACTION_UP 会触发多次，需确保仅与最近的 ACTION_DOWN 相邻的才有效
                if (this.latestPressStart != null) {
                    if (!this.longPressing.get() //
                        && !this.moving) {
                        onSingleTap(data);
                    } else if (!this.longPressing.get() && isFlipping()) {
                        onFlipping(data);
                    }
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
        this.latestPressStart = data;

        triggerListeners(GestureType.PressStart, data);
    }

    private void onPressEnd(GestureData data) {
        this.latestPressStart = null;

        triggerListeners(GestureType.PressEnd, data);
    }

    private void startLongPress(GestureData data) {
        stopLongPress();

        Message msg = this.gestureHandler.obtainMessage(GestureHandler.MSG_LONG_PRESS, data);
        this.gestureHandler.sendMessageDelayed(msg, LONG_PRESS_TIMEOUT_MILLS);
    }

    private void startLongPressTick(LongPressTickGestureData data) {
        if (!this.longPressing.get()) {
            return;
        }

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

        // 若发生了移动，则需要更新 tick 事件发生位置
        int size = this.movingTracker.size();
        if (size > 0) {
            GestureData g = this.movingTracker.get(size - 1);
            data = new LongPressTickGestureData(g, data.tick, data.duration);
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
        SingleTapGestureData tapData = new SingleTapGestureData(data, 0);

        SingleTapGestureData latestTapData = this.latestSingleTap;
        boolean isContinuousTap = latestTapData != null //
                                  && data.timestamp - latestTapData.timestamp < DOUBLE_TAP_TIMEOUT_MILLS;

        if (isContinuousTap) {
            tapData = new SingleTapGestureData(data, latestTapData.tick + 1);
        }
        this.latestSingleTap = tapData;

        // Note：双击也会触发两次单击事件，且均先于双击事件触发
        triggerListeners(GestureType.SingleTap, tapData);

        // 仅连续单击中的第二次才触发双击事件
        if (tapData.tick == 1) {
            triggerListeners(GestureType.DoubleTap, data);
        }
    }

    private void onMoving(GestureData data) {
        // Note: PressStart、MovingStart、Flipping 均须发生在相同的位置上
        if (!this.moving) {
            data = this.latestPressStart;
        }

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

    private void onFlipping(GestureData data) {
        int size = this.movingTracker.size();
        GestureData g1 = this.movingTracker.get(0);
        // Note: g2 应该始终与 data 相同
        GestureData g2 = this.movingTracker.get(size - 1);

        Motion motion = createMotion(g2, g1);
        if (motion.distance <= 0) {
            return;
        }

        // Note: 坐标位置设置为事件初始发生位置
        GestureData newData = new FlippingGestureData(g1, motion);

        triggerListeners(GestureType.Flipping, newData);
    }

    private boolean isFlipping() {
        int size = this.movingTracker.size();
        if (size < 2) {
            return false;
        }

        GestureData g1 = this.movingTracker.get(0);
        GestureData g2 = this.movingTracker.get(size - 1);

        return g2.timestamp - g1.timestamp < FLIPPING_TIMEOUT_MILLS;
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

    protected String getActionName(MotionEvent e) {
        switch (e.getAction()) {
            case MotionEvent.ACTION_DOWN: {
                return "ACTION_DOWN";
            }
            case MotionEvent.ACTION_MOVE: {
                return "ACTION_MOVE";
            }
            case MotionEvent.ACTION_UP: {
                return "ACTION_UP";
            }
            case MotionEvent.ACTION_CANCEL: {
                return "ACTION_CANCEL";
            }
        }
        return null;
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
        /** 翻动: 在一段时间内完成手指按下、移动到抬起的过程，期间没有其他动作 */
        Flipping,
    }

    public static class GestureData {
        public final float x;
        public final float y;
        public final long timestamp;

        public GestureData(float x, float y, long timestamp) {
            this.x = x;
            this.y = y;
            this.timestamp = timestamp;
        }

        public static GestureData newFrom(GestureData g, float x, float y) {
            return new GestureData(x, y, g.timestamp);
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

    public static class FlippingGestureData extends GestureData {
        public final Motion motion;

        public FlippingGestureData(GestureData g, Motion motion) {
            super(g.x, g.y, g.timestamp);
            this.motion = motion;
        }
    }

    public static class TickGestureData extends GestureData {
        public final int tick;

        public TickGestureData(GestureData g, int tick) {
            super(g.x, g.y, g.timestamp);
            this.tick = tick;
        }
    }

    public static class LongPressTickGestureData extends TickGestureData {
        public final long duration;

        public LongPressTickGestureData(GestureData g, int tick, long duration) {
            super(g, tick);
            this.duration = duration;
        }

        /** 事件位置、tick、duration 不变，仅修改时间戳为当前时间 */
        public static LongPressTickGestureData newFrom(LongPressTickGestureData g) {
            return new LongPressTickGestureData(GestureData.newFrom(g), g.tick, g.duration);
        }
    }

    public static class SingleTapGestureData extends TickGestureData {

        public SingleTapGestureData(GestureData g, int tick) {
            super(g, tick);
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
