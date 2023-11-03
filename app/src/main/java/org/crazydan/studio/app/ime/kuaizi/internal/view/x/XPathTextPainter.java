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
import android.graphics.Paint;

/**
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-11-03
 */
public class XPathTextPainter extends XPathPainter {
    private final String text;
    private final float[] offsets = new float[2];

    public XPathTextPainter(String text) {
        this.text = text;
    }

    @Override
    public void draw(Canvas canvas) {
        canvas.drawTextOnPath(this.text, this.path, this.offsets[0], this.offsets[1], this.paint);
    }

    public void setOffset(float h, float v) {
        this.offsets[0] = h;
        this.offsets[1] = v;
    }

    public void setTextSize(float size) {
        this.paint.setTextSize(size);
    }

    public void setTextAlign(Paint.Align align) {
        this.paint.setTextAlign(align);
    }

    public float getFontAscent() {
        return this.paint.getFontMetrics().ascent;
    }

    public float getFontDescent() {
        return this.paint.getFontMetrics().descent;
    }
}
