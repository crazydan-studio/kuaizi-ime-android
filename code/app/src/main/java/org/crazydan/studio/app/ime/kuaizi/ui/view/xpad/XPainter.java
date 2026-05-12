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

package org.crazydan.studio.app.ime.kuaizi.ui.view.xpad;

import android.graphics.Canvas;
import android.graphics.CornerPathEffect;
import android.graphics.Paint;
import org.crazydan.studio.app.ime.kuaizi.common.utils.ThemeUtils;

/**
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-11-01
 */
public abstract class XPainter {
    public final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    protected float alpha = 1f;

    public static void inCanvasLayer(Canvas canvas, Runnable caller) {
        canvas.save();
        caller.run();
        canvas.restore();
    }

    public abstract void draw(Canvas canvas);

    protected Paint getPaint() {
        Paint p = new Paint(this.paint);

        if (this.alpha < 1f) {
            p.setAlpha((int) (this.alpha * 255));
        }
        return p;
    }

    public void setAlpha(float alpha) {
        this.alpha = alpha;
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

    /** 圆角化线的端点 */
    public void setStrokeCap(Paint.Cap cap) {
        this.paint.setStrokeCap(cap);
    }

    public void setCornerRadius(float radius) {
        CornerPathEffect effect = new CornerPathEffect(radius);

        // Note：若画笔设置了圆角，则 Path#op 将不起作用，原因未知
        this.paint.setPathEffect(effect);
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

    public enum Align {
        None,
        TopLeft,
        BottomLeft,
        TopRight,
        BottomRight,
        TopMiddle,
        BottomMiddle,
        Center,
    }
}
