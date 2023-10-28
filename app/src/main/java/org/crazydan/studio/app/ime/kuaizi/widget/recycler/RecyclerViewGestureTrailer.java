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

package org.crazydan.studio.app.ime.kuaizi.widget.recycler;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

/**
 * 在 {@link RecyclerView} 之上绘制滑屏轨迹
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-10-27
 */
public class RecyclerViewGestureTrailer extends RecyclerView.ItemDecoration
        implements RecyclerViewGestureDetector.Listener {
    private static final short TRAIL_STEPS = 100;
    private static final byte TRAIL_STEP_DISTANCE = 5;
    private static final byte TRAIL_MAX_RADIUS = 8;

    private final RecyclerView recyclerView;
    private final Path trailPath = new Path();
    private final Paint trailPaint = new Paint();
    private final float[] trailPathPos = new float[2];
    private final PathMeasure trailPathMeasure = new PathMeasure();

    private boolean disabled;
    private int trailColor;

    public RecyclerViewGestureTrailer(RecyclerView recyclerView, boolean disabled) {
        this.recyclerView = recyclerView;
        this.disabled = disabled;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    public void setTrailColor(int trailColor) {
        this.trailColor = trailColor;
    }

    // 绘制于 RecyclerView 之上
    @Override
    public void onDrawOver(@NonNull Canvas canvas, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        if (this.disabled) {
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
    public void onGesture(RecyclerViewGestureDetector.GestureType type, RecyclerViewGestureDetector.GestureData data) {
        switch (type) {
            case MovingStart: {
                moveTo(data);
                break;
            }
            case Moving: {
                lineTo(data);
                break;
            }
            case MovingEnd: {
                reset();
                break;
            }
        }

        if (!this.disabled) {
            this.recyclerView.invalidate();
        }
    }

    private void moveTo(RecyclerViewGestureDetector.GestureData gesture) {
        this.trailPath.reset();

        this.trailPath.moveTo(gesture.x, gesture.y);
        this.trailPaint.setColor(this.trailColor);
    }

    private void lineTo(RecyclerViewGestureDetector.GestureData gesture) {
        this.trailPath.lineTo(gesture.x, gesture.y);
    }

    private void reset() {
        this.trailPath.reset();
    }
}
