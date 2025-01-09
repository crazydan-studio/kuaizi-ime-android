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

package org.crazydan.studio.app.ime.kuaizi.common.widget;

import android.graphics.Bitmap;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import org.crazydan.studio.app.ime.kuaizi.common.utils.ScreenUtils;

/**
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-10-14
 */
public class Shadow {
    public final float dx;
    public final float dy;
    public final float radius;
    public final int color;

    public Shadow(float dx, float dy, float radius, int color) {
        this.dx = dx;
        this.dy = dy;
        this.radius = radius;
        this.color = color;
    }

    public static Shadow parse(String shadow) {
        if (shadow == null || shadow.trim().isEmpty()) {
            return null;
        }

        String[] splits = shadow.trim().split("\\s+");
        float dx = ScreenUtils.dpToPx(Float.parseFloat(splits[0]));
        float dy = ScreenUtils.dpToPx(Float.parseFloat(splits[1]));
        float radius = ScreenUtils.dpToPx(Float.parseFloat(splits[2]));
        int color = Color.parseColor(splits[3]);

        return new Shadow(dx, dy, radius, color);
    }

    public Bitmap attachTo(Drawable drawable, int dstWidth, int dstHeight) {
        // https://gist.github.com/nickbutcher/4179642450db266f0a33837f2622ace3#file-tiledrawable-kt
        Bitmap bitmap = Bitmap.createBitmap(dstWidth, dstHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        drawable.setBounds(0, 0, dstWidth, dstHeight);
        drawable.draw(canvas);

        return attachTo(bitmap, dstWidth, dstHeight);
    }

    public Bitmap attachTo(Bitmap bitmap, int dstWidth, int dstHeight) {
        int srcWidth = bitmap.getWidth();
        int srcHeight = bitmap.getHeight();

        // https://stackoverflow.com/questions/17783467/drawing-an-outer-shadow-when-drawing-an-image#answer-24579764
        Bitmap mask = Bitmap.createBitmap(dstWidth, dstHeight, Bitmap.Config.ALPHA_8);

        Matrix scaleToFit = new Matrix();
        RectF src = new RectF(0, 0, srcWidth, srcHeight);
        RectF dst = new RectF(0, 0, dstWidth - this.dx, dstHeight - this.dy);
        scaleToFit.setRectToRect(src, dst, Matrix.ScaleToFit.CENTER);

        Matrix dropShadow = new Matrix(scaleToFit);
        dropShadow.postTranslate(this.dx, this.dy);

        Canvas maskCanvas = new Canvas(mask);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        maskCanvas.drawBitmap(bitmap, scaleToFit, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_OUT));
        maskCanvas.drawBitmap(bitmap, dropShadow, paint);

        BlurMaskFilter filter = new BlurMaskFilter(this.radius, BlurMaskFilter.Blur.NORMAL);
        paint.reset();
        paint.setAntiAlias(true);
        paint.setColor(this.color);
        paint.setMaskFilter(filter);
        paint.setFilterBitmap(true);

        Bitmap ret = Bitmap.createBitmap(dstWidth, dstHeight, Bitmap.Config.ARGB_8888);
        Canvas retCanvas = new Canvas(ret);
        retCanvas.drawBitmap(mask, 0, 0, paint);
        retCanvas.drawBitmap(bitmap, scaleToFit, null);
        mask.recycle();

        return ret;
    }
}
