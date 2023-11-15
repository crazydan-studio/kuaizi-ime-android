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

import android.graphics.Canvas;
import android.graphics.PointF;

/**
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-11-03
 */
public class XTextPainter extends XAlignPainter {
    private final String text;

    public XTextPainter(String text) {
        this.text = text;
    }

    @Override
    public void draw(Canvas canvas) {
        this.paint.setTextSize(this.size);

        inCanvasLayer(canvas, () -> {
            if (this.rotate != 0) {
                canvas.rotate(this.rotate, this.start.x, this.start.y);
            }

            // 文字在基线以上的高度
            float ascent = -this.paint.getFontMetrics().ascent;
            // 文本实际占用宽度
            float width = this.paint.measureText(this.text);

            PointF offset = new PointF(0, 0);
            switch (this.align) {
                case TopLeft: {
                    offset.offset(-width, 0);
                    break;
                }
                case BottomLeft: {
                    offset.offset(-width, ascent);
                    break;
                }
                case TopRight: {
                    offset.offset(0, 0);
                    break;
                }
                case BottomRight: {
                    offset.offset(0, ascent);
                    break;
                }
                case TopMiddle: {
                    offset.offset(-width * 0.5f, 0);
                    break;
                }
                case BottomMiddle: {
                    offset.offset(-width * 0.5f, ascent);
                    break;
                }
                case Center: {
                    offset.offset(-width * 0.5f, ascent * 0.5f);
                    break;
                }
            }

            float x = this.start.x + offset.x;
            float y = this.start.y + offset.y;
            canvas.drawText(this.text, x, y, this.paint);
        });
    }
}
