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
import org.crazydan.studio.app.ime.kuaizi.utils.ScreenUtils;

/**
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-10-14
 */
public class Shadow {
    public final float dx;
    public final float dy;
    public final float radius;
    public final int color;

    public Shadow(int dx, int dy, int radius, int color) {
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
        int dx = ScreenUtils.dpToPx(Integer.parseInt(splits[0]));
        int dy = ScreenUtils.dpToPx(Integer.parseInt(splits[1]));
        int radius = ScreenUtils.dpToPx(Integer.parseInt(splits[2]));
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
