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

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.CornerPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import androidx.annotation.NonNull;
import org.hexworks.mixite.core.api.HexagonOrientation;

/**
 * 绘制正六边形样式的{@link KeyView 按键视图}
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-07-01
 */
public class KeyViewDrawable extends Drawable {
    private final Paint paint;
    private final Path path;
    private final HexagonOrientation orientation;

    public KeyViewDrawable(HexagonOrientation orientation) {
        this.paint = new Paint();
        this.path = new Path();
        this.orientation = orientation;

        initPaint();
    }

    @Override
    public void draw(Canvas canvas) {
        canvas.drawPath(this.path, this.paint);
    }

    public void setColor(int color) {
        this.paint.setColor(color);
    }

    /** 绘制圆角 */
    public void setCornerRadius(int radius) {
        CornerPathEffect effect = new CornerPathEffect(radius);

        this.paint.setPathEffect(effect);
    }

    @Override
    public void setAlpha(int alpha) {
        this.paint.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
        this.paint.setColorFilter(colorFilter);
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSPARENT;
    }

    @Override
    protected void onBoundsChange(@NonNull Rect bounds) {
        resetPath(bounds);
    }

    private void initPaint() {
        this.paint.setAntiAlias(true);
        this.paint.setStyle(Paint.Style.FILL);

        // 绘制阴影
        //this.paint.setShadowLayer(5, 3, 3, Color.GRAY);
    }

    private void resetPath(Rect bounds) {
        double centerX = bounds.width() / 2f;
        double centerY = bounds.height() / 2f;
        double radius = this.orientation == HexagonOrientation.FLAT_TOP ? centerX : centerY;

        double[] vertexX = new double[6];
        double[] vertexY = new double[6];
        for (int i = 0; i < 6; i++) {
            int times = this.orientation == HexagonOrientation.FLAT_TOP ? 2 * i : 2 * i + 1;
            double radians = Math.toRadians(30 * times);

            vertexX[i] = centerX + radius * Math.cos(radians);
            vertexY[i] = centerY + radius * Math.sin(radians);
        }

        this.path.reset();
        for (int i = 0; i < 6; i++) {
            float x = (float) vertexX[i];
            float y = (float) vertexY[i];

            if (i == 0) {
                this.path.moveTo(x, y);
            } else {
                this.path.lineTo(x, y);
            }
        }
        this.path.close();
    }
}
