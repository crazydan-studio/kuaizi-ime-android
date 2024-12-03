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

import android.graphics.PointF;

/**
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-11-01
 */
public abstract class XAlignPainter extends XPainter {
    protected PointF start;
    protected Align align = Align.None;

    protected float size;
    protected float rotate;

    public void setStart(PointF start) {
        this.start = start;
    }

    public void setAlign(Align align) {
        this.align = align != null ? align : Align.None;
    }

    public void setRotate(float rotate) {
        this.rotate = rotate;
    }

    public void setSize(float size) {
        this.size = size;
    }
}
