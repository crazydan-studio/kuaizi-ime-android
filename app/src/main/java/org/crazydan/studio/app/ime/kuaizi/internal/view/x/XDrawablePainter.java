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
import android.graphics.drawable.Drawable;

/**
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-11-03
 */
public class XDrawablePainter extends XPainter {
    private final Drawable drawable;

    private PointF center;
    private float size;
    private float rotate;

    public XDrawablePainter(Drawable drawable) {
        // Note：从资源获取的 Drawable 实例是共享的，
        // 修改对其他地方的引用也是同样生效的，
        // 故而，需单独复制一份以做独立修改
        this.drawable = drawable.mutate();
    }

    @Override
    public void draw(Canvas canvas) {
        this.drawable.setAlpha((int) (this.alpha * 255));
        this.drawable.setBounds(0, 0, (int) this.size, (int) this.size);

        inCanvasLayer(canvas, () -> {
            if (this.rotate != 0) {
                canvas.rotate(this.rotate, this.center.x, this.center.y);
            }

            float offset = this.size * 0.5f;
            canvas.translate(this.center.x - offset, this.center.y - offset);

            this.drawable.draw(canvas);
        });
    }

    public void setCenter(PointF center) {
        this.center = center;
    }

    public void setSize(float size) {
        this.size = size;
    }

    public void setRotate(float rotate) {
        this.rotate = rotate;
    }
}
