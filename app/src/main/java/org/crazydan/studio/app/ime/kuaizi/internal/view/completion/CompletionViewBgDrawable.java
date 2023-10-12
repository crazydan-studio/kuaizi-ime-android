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

package org.crazydan.studio.app.ime.kuaizi.internal.view.completion;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import androidx.annotation.NonNull;
import org.crazydan.studio.app.ime.kuaizi.utils.ThemeUtils;

/**
 * 绘制云朵样式的 {@link CompletionView} 的背景图
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-10-12
 */
public class CompletionViewBgDrawable extends Drawable {
    private final Paint paint;
    private final Path path;

    private Integer fillColor;
    private String shadow;

    public CompletionViewBgDrawable() {
        this.paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        this.path = new Path();
    }

    @Override
    protected void onBoundsChange(@NonNull Rect bounds) {
        resetPath(bounds);
    }

    @Override
    public void draw(Canvas canvas) {
        if (this.fillColor != null) {
            ThemeUtils.applyShadow(this.paint, this.shadow);

            this.paint.setStyle(Paint.Style.FILL);
            this.paint.setColor(this.fillColor);
            this.paint.setAntiAlias(true);
        }

        canvas.drawPath(this.path, this.paint);
    }

    public void setFillColor(int color) {
        this.fillColor = color;
    }

    public void setShadow(String shadow) {
        this.shadow = shadow;
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

    private void resetPath(Rect bounds) {
        float ow = bounds.width();
        float oh = bounds.height();
        float x = ow / 2;
        float y = oh / 2;
        float w = ow;
        float h = oh;

        float od = Math.min(w / ow, h / oh);

        Matrix matrix = new Matrix();
        matrix.reset();
        matrix.setScale(od, od);

        this.path.reset();

        this.path.moveTo(230.4f, 389.57f);
        this.path.cubicTo(194.87f, 389.57f, 160.53f, 375.34f, 135.23f, 350.32f);
        this.path.cubicTo(126.79f, 353.95f, 117.63f, 355.87f, 108.39f, 355.87f);
        this.path.cubicTo(71.04f, 355.87f, 40.66f, 325.48f, 40.66f, 288.12f);
        this.path.cubicTo(40.66f, 284.13f, 41.04f, 280.08f, 41.79f, 276.03f);
        this.path.cubicTo(15.52f, 256.6f, 0.0f, 226.05f, 0.0f, 193.21f);
        this.path.cubicTo(0.0f, 141.56f, 38.98f, 97.72f, 89.71f, 91.12f);
        this.path.cubicTo(102.41f, 57.89f, 134.06f, 35.92f, 170.07f, 35.92f);
        this.path.cubicTo(201.51f, 35.92f, 230.25f, 53.09f, 245.33f, 80.28f);
        this.path.cubicTo(256.19f, 76.53f, 267.51f, 74.62f, 279.11f, 74.62f);
        this.path.cubicTo(336.25f, 74.62f, 382.74f, 121.1f, 382.74f, 178.24f);
        this.path.cubicTo(382.74f, 180.64f, 382.63f, 183.08f, 382.42f, 185.66f);
        this.path.cubicTo(408.09f, 195.71f, 425.49f, 220.71f, 425.49f, 248.69f);
        this.path.cubicTo(425.49f, 288.36f, 390.96f, 320.14f, 350.76f, 316.07f);
        this.path.cubicTo(327.63f, 360.91f, 281.04f, 389.57f, 230.4f, 389.57f);
        this.path.moveTo(138.64f, 330.26f);
        this.path.lineTo(142.95f, 334.92f);
        this.path.cubicTo(165.84f, 359.68f, 196.9f, 373.32f, 230.4f, 373.32f);
        this.path.cubicTo(276.76f, 373.32f, 319.27f, 346.01f, 338.69f, 303.75f);
        this.path.lineTo(341.35f, 297.92f);
        this.path.lineTo(347.63f, 299.15f);
        this.path.cubicTo(351.01f, 299.82f, 354.41f, 300.16f, 357.74f, 300.16f);
        this.path.cubicTo(386.12f, 300.16f, 409.22f, 277.06f, 409.22f, 248.68f);
        this.path.cubicTo(409.22f, 225.63f, 393.69f, 205.24f, 371.46f, 199.11f);
        this.path.lineTo(364.6f, 197.22f);
        this.path.lineTo(365.56f, 190.17f);
        this.path.cubicTo(366.18f, 185.61f, 366.48f, 181.83f, 366.48f, 178.23f);
        this.path.cubicTo(366.48f, 130.06f, 327.28f, 90.87f, 279.1f, 90.87f);
        this.path.cubicTo(267.14f, 90.87f, 255.53f, 93.27f, 244.56f, 97.99f);
        this.path.lineTo(237.16f, 101.17f);
        this.path.lineTo(233.91f, 93.81f);
        this.path.cubicTo(222.73f, 68.51f, 197.67f, 52.16f, 170.06f, 52.16f);
        this.path.cubicTo(139.37f, 52.16f, 112.6f, 71.82f, 103.44f, 101.09f);
        this.path.lineTo(101.79f, 106.34f);
        this.path.lineTo(96.31f, 106.77f);
        this.path.cubicTo(51.42f, 110.21f, 16.26f, 148.18f, 16.26f, 193.2f);
        this.path.cubicTo(16.26f, 222.46f, 30.89f, 249.56f, 55.4f, 265.7f);
        this.path.lineTo(60.31f, 268.94f);
        this.path.lineTo(58.76f, 274.61f);
        this.path.cubicTo(57.53f, 279.16f, 56.91f, 283.69f, 56.91f, 288.11f);
        this.path.cubicTo(56.91f, 316.49f, 80.0f, 339.59f, 108.39f, 339.59f);
        this.path.cubicTo(117.0f, 339.59f, 125.53f, 337.41f, 133.06f, 333.3f);
        this.path.lineTo(138.64f, 330.26f);

        this.path.transform(matrix);
    }

    public void draw(Canvas c, int x, int y, int w, int h, int color) {
        // original size of path
        float ow = 200f;
        float oh = 200f;

        float od = Math.min(w / ow, h / oh);

        c.save();
        c.translate((w - od * ow) / 2f + x, (h - od * oh) / 2f + y);

        Matrix matrix = new Matrix();
        matrix.reset();
        matrix.setScale(od, od);

        c.save();
        c.scale(2.5f, 2.5f);
        c.save();
        path.reset();

        path.moveTo(230.4f, 389.57f);
        path.cubicTo(194.87f, 389.57f, 160.53f, 375.34f, 135.23f, 350.32f);
        path.cubicTo(126.79f, 353.95f, 117.63f, 355.87f, 108.39f, 355.87f);
        path.cubicTo(71.04f, 355.87f, 40.66f, 325.48f, 40.66f, 288.12f);
        path.cubicTo(40.66f, 284.13f, 41.04f, 280.08f, 41.79f, 276.03f);
        path.cubicTo(15.52f, 256.6f, 0.0f, 226.05f, 0.0f, 193.21f);
        path.cubicTo(0.0f, 141.56f, 38.98f, 97.72f, 89.71f, 91.12f);
        path.cubicTo(102.41f, 57.89f, 134.06f, 35.92f, 170.07f, 35.92f);
        path.cubicTo(201.51f, 35.92f, 230.25f, 53.09f, 245.33f, 80.28f);
        path.cubicTo(256.19f, 76.53f, 267.51f, 74.62f, 279.11f, 74.62f);
        path.cubicTo(336.25f, 74.62f, 382.74f, 121.1f, 382.74f, 178.24f);
        path.cubicTo(382.74f, 180.64f, 382.63f, 183.08f, 382.42f, 185.66f);
        path.cubicTo(408.09f, 195.71f, 425.49f, 220.71f, 425.49f, 248.69f);
        path.cubicTo(425.49f, 288.36f, 390.96f, 320.14f, 350.76f, 316.07f);
        path.cubicTo(327.63f, 360.91f, 281.04f, 389.57f, 230.4f, 389.57f);
        path.moveTo(138.64f, 330.26f);
        path.lineTo(142.95f, 334.92f);
        path.cubicTo(165.84f, 359.68f, 196.9f, 373.32f, 230.4f, 373.32f);
        path.cubicTo(276.76f, 373.32f, 319.27f, 346.01f, 338.69f, 303.75f);
        path.lineTo(341.35f, 297.92f);
        path.lineTo(347.63f, 299.15f);
        path.cubicTo(351.01f, 299.82f, 354.41f, 300.16f, 357.74f, 300.16f);
        path.cubicTo(386.12f, 300.16f, 409.22f, 277.06f, 409.22f, 248.68f);
        path.cubicTo(409.22f, 225.63f, 393.69f, 205.24f, 371.46f, 199.11f);
        path.lineTo(364.6f, 197.22f);
        path.lineTo(365.56f, 190.17f);
        path.cubicTo(366.18f, 185.61f, 366.48f, 181.83f, 366.48f, 178.23f);
        path.cubicTo(366.48f, 130.06f, 327.28f, 90.87f, 279.1f, 90.87f);
        path.cubicTo(267.14f, 90.87f, 255.53f, 93.27f, 244.56f, 97.99f);
        path.lineTo(237.16f, 101.17f);
        path.lineTo(233.91f, 93.81f);
        path.cubicTo(222.73f, 68.51f, 197.67f, 52.16f, 170.06f, 52.16f);
        path.cubicTo(139.37f, 52.16f, 112.6f, 71.82f, 103.44f, 101.09f);
        path.lineTo(101.79f, 106.34f);
        path.lineTo(96.31f, 106.77f);
        path.cubicTo(51.42f, 110.21f, 16.26f, 148.18f, 16.26f, 193.2f);
        path.cubicTo(16.26f, 222.46f, 30.89f, 249.56f, 55.4f, 265.7f);
        path.lineTo(60.31f, 268.94f);
        path.lineTo(58.76f, 274.61f);
        path.cubicTo(57.53f, 279.16f, 56.91f, 283.69f, 56.91f, 288.11f);
        path.cubicTo(56.91f, 316.49f, 80.0f, 339.59f, 108.39f, 339.59f);
        path.cubicTo(117.0f, 339.59f, 125.53f, 337.41f, 133.06f, 333.3f);
        path.lineTo(138.64f, 330.26f);

        path.transform(matrix);

        c.drawPath(path, paint);
        c.restore();
    }
}
