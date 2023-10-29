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

package org.crazydan.studio.app.ime.kuaizi.widget;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.CornerPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import androidx.annotation.NonNull;
import org.crazydan.studio.app.ime.kuaizi.utils.ThemeUtils;
import org.crazydan.studio.app.ime.kuaizi.utils.ViewUtils;
import org.hexworks.mixite.core.api.HexagonOrientation;

/**
 * 正六边形绘图
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-07-01
 */
public class HexagonDrawable extends Drawable {
    private final Paint paint;
    private final Path path;
    private final HexagonOrientation orientation;

    private Integer fillColor;
    private String border;
    private String shadow;

    public HexagonDrawable(HexagonOrientation orientation) {
        this.paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        this.path = new Path();
        this.orientation = orientation;
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

            canvas.drawPath(this.path, this.paint);
        }

        if (ThemeUtils.applyBorder(this.paint, this.border)) {
            this.paint.clearShadowLayer();

            canvas.drawPath(this.path, this.paint);
        }
    }

    public void setFillColor(int color) {
        this.fillColor = color;
    }

    public void setBorder(String border) {
        this.border = border;
    }

    public void setShadow(String shadow) {
        this.shadow = shadow;
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

    private void resetPath(Rect bounds) {
        this.path.reset();

        PointF center = new PointF(bounds.width() / 2f, bounds.height() / 2f);
        // Note：bounds 的坐标从 (0, 0) 开始，且其为正方形，故而，半径与中心位置相同
        float radius = this.orientation == HexagonOrientation.FLAT_TOP ? center.x : center.y;

        ViewUtils.drawHexagon(this.path, this.orientation, center, radius);
    }
}
