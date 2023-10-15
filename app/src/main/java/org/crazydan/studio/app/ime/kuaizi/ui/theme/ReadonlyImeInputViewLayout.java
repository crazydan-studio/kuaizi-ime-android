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

package org.crazydan.studio.app.ime.kuaizi.ui.theme;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import org.crazydan.studio.app.ime.kuaizi.utils.ScreenUtils;

/**
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-10-15
 */
public class ReadonlyImeInputViewLayout extends FrameLayout {

    public ReadonlyImeInputViewLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        // 禁止传递事件，以确保输入键盘不能接收到事件，
        // 也就不能响应用户操作，变成为只读键盘了
        return true;
    }

    @Override
    protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
        float dstWidth = getMeasuredWidth();
        float dstHeight = getMeasuredHeight();
        float srcWidth = dstWidth;//child.getMeasuredWidth();
        float srcHeight = child.getMeasuredHeight();//ScreenUtils.dpToPx(308);//child.getMeasuredHeight();

        Matrix matrix = new Matrix();
        RectF src = new RectF(0, 0, srcWidth, srcHeight);
        RectF dst = new RectF(0, 0, dstWidth, dstHeight);
        matrix.setRectToRect(src, dst, Matrix.ScaleToFit.CENTER);

        int save = canvas.save();
        canvas.concat(matrix);

        boolean result = super.drawChild(canvas, child, drawingTime);
        canvas.restoreToCount(save);

        return result;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

//        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
//        int heightSize = MeasureSpec.getSize(heightMeasureSpec);
//        setMeasuredDimension(widthSize, heightSize);
//
//        // Measure our child as unspecified.
//        int spec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.AT_MOST);
//        measureChildren(spec, spec);

//        if (getChildCount() == 0) {
//            return;
//        }
//        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
//
//        View child = getChildAt(0);
//        child.measure(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
//
//        int width = getMeasuredWidth();
//        int height = getMeasuredHeight();
//
//        int childWidth = child.getMeasuredWidth();
//        int childHeight = child.getMeasuredHeight();
//
//        float wRatio = childWidth > width ? width / (childWidth * 1.0f) : 1f;
//        float hRatio = childHeight > height ? height / (childHeight * 1.0f) : 1f;
//        float scaleRatio = Math.min(wRatio, hRatio);
//
//        child.setScaleX(scaleRatio);
//        child.setScaleY(scaleRatio);
    }
}
