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

package org.crazydan.studio.app.ime.kuaizi.keyboard.view.xpad;

import android.graphics.Canvas;
import android.graphics.PointF;
import android.graphics.drawable.Drawable;

/**
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-11-03
 */
public class XDrawablePainter extends XAlignPainter {
    private final Layer layer = new Layer();

    private final Drawable drawable;

    public XDrawablePainter(Drawable drawable) {
        // Note：从资源获取的 Drawable 实例是共享的，
        // 修改对其他地方的引用也是同样生效的，
        // 故而，需单独复制一份以做独立修改
        // Note: mutate 后的 Drawable 不能被缓存，
        // 否则，其资源将不会被回收，从而导致内存占用暴增
        this.drawable = drawable.mutate();
    }

    @Override
    public void draw(Canvas canvas) {
        this.layer.setCanvas(canvas);

        inCanvasLayer(canvas, this.layer);
    }

    /** 通过 inner class 降低动态创建 lambda 函数的开销 */
    private class Layer implements Runnable {
        private Canvas canvas;

        public void setCanvas(Canvas canvas) {
            this.canvas = canvas;
        }

        @Override
        public void run() {
            int w = XDrawablePainter.this.drawable.getIntrinsicWidth();
            int h = XDrawablePainter.this.drawable.getIntrinsicHeight();
            // 按最小比例做长宽的等比例缩放
            float scale = XDrawablePainter.this.size / Math.max(w, h);

            float width = (int) (w * scale);
            float height = (int) (h * scale);

            XDrawablePainter.this.drawable.setAlpha((int) (XDrawablePainter.this.alpha * 255));
            XDrawablePainter.this.drawable.setBounds(0, 0, (int) width, (int) height);

            if (XDrawablePainter.this.rotate != 0) {
                this.canvas.rotate(XDrawablePainter.this.rotate,
                                   XDrawablePainter.this.start.x,
                                   XDrawablePainter.this.start.y);
            }

            PointF offset = new PointF(0, 0);
            switch (XDrawablePainter.this.align) {
                case TopLeft: {
                    offset.offset(-width, -height);
                    break;
                }
                case BottomLeft: {
                    offset.offset(-width, 0);
                    break;
                }
                case TopRight: {
                    offset.offset(0, -height);
                    break;
                }
                case BottomRight: {
                    offset.offset(0, 0);
                    break;
                }
                case TopMiddle: {
                    offset.offset(-width * 0.5f, -height);
                    break;
                }
                case BottomMiddle: {
                    offset.offset(-width * 0.5f, 0);
                    break;
                }
                case Center: {
                    offset.offset(-width * 0.5f, -height * 0.5f);
                    break;
                }
            }

            float x = XDrawablePainter.this.start.x + offset.x;
            float y = XDrawablePainter.this.start.y + offset.y;
            this.canvas.translate(x, y);

            XDrawablePainter.this.drawable.draw(this.canvas);
        }
    }
}
