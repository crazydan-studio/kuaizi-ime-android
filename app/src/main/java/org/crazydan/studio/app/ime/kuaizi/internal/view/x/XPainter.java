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

import java.util.function.Consumer;

import android.graphics.Canvas;
import android.graphics.CornerPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import org.crazydan.studio.app.ime.kuaizi.utils.ThemeUtils;

/**
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-11-01
 */
public class XPainter {
    public final Path path = new Path();

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public void draw(Canvas canvas) {
        canvas.drawPath(this.path, this.paint);
    }

    public void setFillColor(int color) {
        updateFill((p) -> p.setColor(color));
    }

    public void setFillShadow(String shadow) {
        updateFill((p) -> ThemeUtils.applyShadow(p, shadow));
    }

    public void setStrokeShadow(String shadow) {
        updateStroke((p) -> ThemeUtils.applyShadow(p, shadow));
    }

    public void setStrokeStyle(String style) {
        updateStroke((p) -> ThemeUtils.applyBorder(p, style));
    }

    public void setCornerRadius(float radius) {
        CornerPathEffect effect = new CornerPathEffect(radius);

        // Note：若画笔设置了圆角，则 Path#op 将不起作用，原因未知
        this.paint.setPathEffect(effect);
    }

    private void updateStroke(Consumer<Paint> updater) {
        updatePaint(Paint.Style.STROKE, updater);
    }

    private void updateFill(Consumer<Paint> updater) {
        updatePaint(Paint.Style.FILL, updater);
    }

    private void updatePaint(Paint.Style style, Consumer<Paint> updater) {
        updater.accept(this.paint);

        this.paint.setStyle(style);
    }
}
