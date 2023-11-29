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

package org.crazydan.studio.app.ime.kuaizi.internal.view.xpad;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.Typeface;

import static org.crazydan.studio.app.ime.kuaizi.internal.view.xpad.XPainter.Align.Center;

/**
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-11-03
 */
public class XTextPainter extends XAlignPainter {
    private final Layer layer = new Layer();

    private final String text;

    public XTextPainter(String text) {
        this.text = text;
    }

    @Override
    public void draw(Canvas canvas) {
        this.layer.setCanvas(canvas);

        inCanvasLayer(canvas, this.layer);
    }

    public void enableBoldText() {
        this.paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
    }

    /** 通过 inner class 降低动态创建 lambda 函数的开销 */
    private class Layer implements Runnable {
        private Canvas canvas;

        public void setCanvas(Canvas canvas) {
            this.canvas = canvas;
        }

        @Override
        public void run() {
            XTextPainter.this.paint.setTextSize(XTextPainter.this.size);

            if (XTextPainter.this.rotate != 0) {
                this.canvas.rotate(XTextPainter.this.rotate, XTextPainter.this.start.x, XTextPainter.this.start.y);
            }

            Path path = new Path();
            float x = XTextPainter.this.start.x;
            float y = XTextPainter.this.start.y;
            // 文字在基线以上的高度
            float ascent = -XTextPainter.this.paint.getFontMetrics().ascent * 0.8f;
            // Note：不需要准确宽度，只需要确保其能够大于文本实际宽度即可
            float width = XTextPainter.this.size * XTextPainter.this.text.length() * 2;

            PointF offset = new PointF(0, 0);
            switch (XTextPainter.this.align) {
                case BottomLeft:
                    offset.offset(0, ascent);
                case TopLeft: {
                    path.moveTo(x - width, y);
                    path.lineTo(x, y);
                    XTextPainter.this.paint.setTextAlign(Paint.Align.RIGHT);
                    break;
                }
                case BottomRight:
                    offset.offset(0, ascent);
                case TopRight: {
                    path.moveTo(x, y);
                    path.lineTo(x + width, y);
                    XTextPainter.this.paint.setTextAlign(Paint.Align.LEFT);
                    break;
                }
                case Center:
                case BottomMiddle:
                    offset.offset(0, XTextPainter.this.align == Center ? ascent * 0.5f : ascent);
                case TopMiddle: {
                    path.moveTo(x - width * 0.5f, y);
                    path.lineTo(x + width * 0.5f, y);
                    XTextPainter.this.paint.setTextAlign(Paint.Align.CENTER);
                    break;
                }
            }

//            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
//            paint.setStyle(Paint.Style.STROKE);
//            paint.setColor(Color.BLUE);
//            this.canvas.drawPath(path, paint);

            this.canvas.drawTextOnPath(XTextPainter.this.text, path, offset.x, offset.y, XTextPainter.this.paint);
        }
    }
}
