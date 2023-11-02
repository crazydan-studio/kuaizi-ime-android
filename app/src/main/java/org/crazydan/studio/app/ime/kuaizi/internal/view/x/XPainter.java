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

package org.crazydan.studio.app.ime.kuaizi.internal.view.x;

import android.graphics.Canvas;
import android.graphics.CornerPathEffect;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import org.crazydan.studio.app.ime.kuaizi.utils.ThemeUtils;

/**
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-11-01
 */
public class XPainter {
    public final Path path = new Path();

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private float scale;

    public void draw(Canvas canvas, PointF origin) {
        Path p = this.path;
        if (this.scale < 1f) {
            Matrix matrix = new Matrix();
            matrix.setScale(this.scale, this.scale, origin.x, origin.y);

            p = new Path();
            this.path.transform(matrix, p);
        }

        canvas.drawPath(p, this.paint);
    }

    public void setFillColor(int color) {
        withFill(() -> this.paint.setColor(color));
    }

    public void setFillShadow(String shadow) {
        withFill(() -> ThemeUtils.applyShadow(this.paint, shadow));
    }

    public void setStrokeShadow(String shadow) {
        withStroke(() -> ThemeUtils.applyShadow(this.paint, shadow));
    }

    public void setStrokeStyle(String style) {
        withStroke(() -> ThemeUtils.applyBorder(this.paint, style));
    }

    public void setCornerRadius(float radius) {
        CornerPathEffect effect = new CornerPathEffect(radius);

        // Note：若画笔设置了圆角，则 Path#op 将不起作用，原因未知
        this.paint.setPathEffect(effect);
    }

    public void setAlpha(float alpha) {
        this.paint.setAlpha((int) (255 * alpha));
    }

    public void setScale(float scale) {
        this.scale = scale;
    }

    private void withStroke(Runnable caller) {
        updatePaint(Paint.Style.STROKE, caller);
    }

    private void withFill(Runnable caller) {
        updatePaint(Paint.Style.FILL, caller);
    }

    private void updatePaint(Paint.Style style, Runnable caller) {
        caller.run();
        this.paint.setStyle(style);
    }
}
