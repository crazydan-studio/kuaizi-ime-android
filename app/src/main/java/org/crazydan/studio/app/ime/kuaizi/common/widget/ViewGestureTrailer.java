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

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import androidx.annotation.NonNull;

/**
 * 绘制滑屏轨迹
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-10-27
 */
public class ViewGestureTrailer implements ViewGestureDetector.Listener {
    private static final short TRAIL_STEPS = 100;
    private static final byte TRAIL_STEP_DISTANCE = 5;
    private static final byte TRAIL_MAX_RADIUS = 8;

    private final Path trailPath = new Path();
    private final Paint trailPaint = new Paint();
    private final float[] trailPathPos = new float[2];
    private final PathMeasure trailPathMeasure = new PathMeasure();

    private boolean disabled;
    private boolean started;

    public boolean isDisabled() {
        return this.disabled;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    public void setColor(int color) {
        this.trailPaint.setColor(color);
    }

    public void draw(@NonNull Canvas canvas) {
        if (this.disabled || !this.started) {
            return;
        }

        // https://github.com/8VIM/8VIM/blob/master/8vim/src/main/java/inc/flide/vim8/views/mainkeyboard/XpadView.java#L407
        this.trailPathMeasure.setPath(this.trailPath, false);

        float pathLength = this.trailPathMeasure.getLength();
        for (short i = 1; i <= TRAIL_STEPS; i++) {
            float distance = pathLength - i * TRAIL_STEP_DISTANCE;
            if (distance < 0) {
                continue;
            }

            float trailRadius = TRAIL_MAX_RADIUS * (1 - (float) i / TRAIL_STEPS);
            this.trailPathMeasure.getPosTan(distance, this.trailPathPos, null);

            float x = this.trailPathPos[0];
            float y = this.trailPathPos[1];

            canvas.drawCircle(x, y, trailRadius, this.trailPaint);
        }
    }

    @Override
    public void onGesture(ViewGestureDetector.GestureType type, ViewGestureDetector.GestureData data) {
        switch (type) {
            case PressStart: {
                start(data);
                break;
            }
            case MovingStart:
            case Moving: {
                moving(data);
                break;
            }
            case MovingEnd:
            case PressEnd: {
                stop();
                break;
            }
        }
    }

    private void start(ViewGestureDetector.GestureData gesture) {
        this.started = true;
        this.trailPath.reset();

        this.trailPath.moveTo(gesture.x, gesture.y);
    }

    private void moving(ViewGestureDetector.GestureData gesture) {
        if (!this.started) {
            return;
        }

        this.trailPath.lineTo(gesture.x, gesture.y);
    }

    private void stop() {
        this.started = false;
        this.trailPath.reset();
    }
}
