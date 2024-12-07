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

package org.crazydan.studio.app.ime.kuaizi.pane.view.xpad;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.drawable.Drawable;
import org.crazydan.studio.app.ime.kuaizi.common.utils.ViewUtils;

/**
 * 区
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-11-01
 */
public class XZone {
    public final List<Block> blocks = new ArrayList<>();
    private final Layer layer = new Layer();
    private final List<XPathPainter> pathPainters = new ArrayList<>();
    private final List<XDrawablePainter> iconPainters = new ArrayList<>();
    private final List<XTextPainter> textPainters = new ArrayList<>();
    private boolean hidden;
    private Float alpha = null;
    private Float scale = null;

    public void draw(Canvas canvas, PointF origin) {
        if (this.hidden) {
            return;
        }

        this.layer.setCanvas(canvas);
        this.layer.setOrigin(origin);

        XPainter.inCanvasLayer(canvas, this.layer);

        // 绘制 Link 边界线
        //drawLinkBoundaries(canvas);
    }

    public void hide() {
        this.hidden = true;
    }

    public void show() {
        this.hidden = false;
    }

    public void reset() {
        bounce();
    }

    /** 按压 */
    public void press() {
        this.alpha = 0.4f;
        this.scale = 0.9f;
    }

    /** 弹起 */
    public void bounce() {
        this.alpha = 1f;
        this.scale = 1f;
    }

    public XPathPainter newPathPainter() {
        XPathPainter painter = new XPathPainter();
        this.pathPainters.add(painter);

        return painter;
    }

    public void clearIconPainters() {
        this.iconPainters.clear();
    }

    public XDrawablePainter newIconPainter(Drawable drawable) {
        XDrawablePainter painter = new XDrawablePainter(drawable);
        this.iconPainters.add(painter);

        return painter;
    }

    public void clearTextPainters() {
        this.textPainters.clear();
    }

    public XTextPainter newTextPainter(String text) {
        XTextPainter painter = new XTextPainter(text);
        this.textPainters.add(painter);

        return painter;
    }

    private <T extends XPainter> void doPaint(Canvas canvas, List<T> painters) {
        for (XPainter painter : painters) {
            if (painter == null) {
                continue;
            }

            if (this.alpha != null) {
                painter.setAlpha(this.alpha);
            }
            painter.draw(canvas);
        }
    }

    private void drawLinkBoundaries(Canvas canvas) {
        Path path = new Path();
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(Color.RED);

        for (Block block : this.blocks) {
            Links links = ((BaseBlock) block).links;

            drawLinkBoundary(links.center, canvas, path, paint);

            for (Link link : links.left) {
                drawLinkBoundary(link, canvas, path, paint);
            }
            for (Link link : links.right) {
                drawLinkBoundary(link, canvas, path, paint);
            }
        }
    }

    private void drawLinkBoundary(Link link, Canvas canvas, Path path, Paint paint) {
        if (link.isEmpty()) {
            return;
        }

        path.reset();
        for (int i = 0; i < link.vertexes.size(); i++) {
            PointF point = link.vertexes.get(i);

            if (i == 0) {
                path.moveTo(point.x, point.y);
            } else {
                path.lineTo(point.x, point.y);
            }
        }
        path.close();

        canvas.drawPath(path, paint);
    }

    /** 块 */
    interface Block {
        /** 判断指定的点是否被包含在边界内 */
        boolean contains(PointF point);
    }

    /** 链 */
    public static class Link {
        public final PointF center = new PointF();
        public final List<PointF> vertexes = new ArrayList<>();

        public boolean isEmpty() {
            return this.vertexes.isEmpty();
        }

        public void addVertexes(PointF... vertexes) {
            this.vertexes.addAll(Arrays.asList(vertexes));

            PointF p = ViewUtils.getPolygonCenter(this.vertexes);
            this.center.set(p.x, p.y);
        }
    }

    public static class Links {
        public final Link center = new Link();
        public final List<Link> left = new ArrayList<>();
        public final List<Link> right = new ArrayList<>();
    }

    public static abstract class BaseBlock implements Block {
        public final Links links = new Links();
    }

    /** 圆形块 */
    public static class CircleBlock extends BaseBlock {
        public final PointF center;
        public final float radius;
        private final float radius_pow_2;

        public CircleBlock(PointF center, float radius) {
            this.center = center;
            this.radius = radius;
            this.radius_pow_2 = radius * radius;
        }

        @Override
        public boolean contains(PointF point) {
            double distance_pow_2 = (this.center.x - point.x) * (this.center.x - point.x) //
                                    + (this.center.y - point.y) * (this.center.y - point.y);

            return distance_pow_2 < this.radius_pow_2;
        }
    }

    /** 多边形块 */
    public static class PolygonBlock extends BaseBlock {
        public final PointF[] vertexes;

        public PolygonBlock(PointF... vertexes) {
            this.vertexes = vertexes;
        }

        @Override
        public boolean contains(PointF point) {
            return ViewUtils.isPointInPolygon(point, this.vertexes);
        }
    }

    /** 通过 inner class 降低动态创建 lambda 函数的开销 */
    private class Layer implements Runnable {
        private Canvas canvas;
        private PointF origin;

        public void setCanvas(Canvas canvas) {
            this.canvas = canvas;
        }

        public void setOrigin(PointF origin) {
            this.origin = origin;
        }

        @Override
        public void run() {
            if (XZone.this.scale != null) {
                this.canvas.scale(XZone.this.scale, XZone.this.scale, this.origin.x, this.origin.y);
            }

            XZone.this.doPaint(this.canvas, XZone.this.pathPainters);
            XZone.this.doPaint(this.canvas, XZone.this.iconPainters);
            XZone.this.doPaint(this.canvas, XZone.this.textPainters);
        }
    }
}
