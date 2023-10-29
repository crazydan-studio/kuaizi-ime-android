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

package org.crazydan.studio.app.ime.kuaizi.internal.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.CornerPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.view.View;
import androidx.annotation.Nullable;
import org.crazydan.studio.app.ime.kuaizi.utils.ViewUtils;
import org.hexworks.mixite.core.api.HexagonOrientation;

/**
 * X 型全滑屏输入键盘视图
 * <p/>
 * 参考设计来源于：<a href="https://github.com/8VIM/8VIM">8VIM/8VIM</a>
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-10-29
 */
public class XKeyboardView extends View {
    private final Paint paint;
    private final Path path;
    private final HexagonOrientation orientation = HexagonOrientation.FLAT_TOP;

    private int hexagonRadius = 128;
    private int hexagonCornerRadius = 12;

    public XKeyboardView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        this.paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        this.path = new Path();

        CornerPathEffect effect = new CornerPathEffect(this.hexagonCornerRadius);
        this.paint.setPathEffect(effect);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int width = getWidth();
        int height = getHeight();

        PointF center = new PointF(width / 2f, height / 2f);

        this.paint.setColor(Color.RED);
        ViewUtils.drawHexagon(this.path, this.orientation, center, this.hexagonRadius);

        canvas.drawPath(this.path, this.paint);
    }
}
