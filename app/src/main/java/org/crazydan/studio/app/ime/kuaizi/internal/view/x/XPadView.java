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

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import androidx.annotation.Nullable;
import org.crazydan.studio.app.ime.kuaizi.R;
import org.crazydan.studio.app.ime.kuaizi.utils.ScreenUtils;
import org.crazydan.studio.app.ime.kuaizi.utils.ThemeUtils;
import org.crazydan.studio.app.ime.kuaizi.utils.ViewUtils;
import org.hexworks.mixite.core.api.HexagonOrientation;

/**
 * X 型全滑屏输入面板视图
 * <p/>
 * 参考设计和代码来源于：<a href="https://github.com/8VIM/8VIM">8VIM/8VIM</a>
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-10-29
 */
public class XPadView extends View {
    private static final short TRAIL_STEPS = 100;
    private static final byte TRAIL_STEP_DISTANCE = 5;
    private static final byte TRAIL_MAX_RADIUS = 8;
    private static final float cos_30 = (float) Math.cos(Math.toRadians(30));
    private static final float cos_30_divided_by_1 = 1f / cos_30;

    private final HexagonOrientation orientation = HexagonOrientation.FLAT_TOP;
    private final XZone[] zones = new XZone[3];
    private final Paint textPaint;
    private final PointF currentPoint = new PointF(-100000, -100000);

    private final Path trailPath = new Path();
    private final Paint trailPaint = new Paint();
    private final float[] trailPathPos = new float[2];
    private final PathMeasure trailPathMeasure = new PathMeasure();

    private int padding = ScreenUtils.dpToPx(8);
    private int hexagonRadius = 152;
    private int hexagonCornerRadius = 12;
    private String dividerStyle;

    public XPadView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        this.dividerStyle = ThemeUtils.getStringByAttrId(context, R.attr.x_keyboard_divider_style);

        this.textPaint = new Paint();
        this.textPaint.setAntiAlias(true);

        ViewUtils.enableHardwareAccelerated(this);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent e) {
        invalidate();

        this.currentPoint.set(-100000, -100000);

        // Note：需要返回 true 才能拦截到 move 等事件
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN: {
                this.trailPath.reset();
                this.trailPath.moveTo(e.getX(), e.getY());

                int trailColor = ThemeUtils.getColorByAttrId(getContext(), R.attr.key_highlight_fg_color);
                this.trailPaint.setColor(trailColor);
                return true;
            }
            case MotionEvent.ACTION_MOVE: {
                this.currentPoint.set(e.getX(), e.getY());
                this.trailPath.lineTo(e.getX(), e.getY());
                return true;
            }
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL: {
                this.trailPath.reset();
                return true;
            }
        }

        return false;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // 绘制分区
        drawZones(canvas);

        // 绘制分区的 Link 内容
        drawContentOnZoneLinks(canvas);

        // 绘制滑屏轨迹
        drawTrailPath(canvas);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int specWidth = MeasureSpec.getSize(widthMeasureSpec);
        int specHeight = MeasureSpec.getSize(heightMeasureSpec);

        setMeasuredDimension(specWidth, specHeight);

        // Note：在该接口内调用 getWidth/getHeight 将返回 0，
        // 但 specWidth/specHeight 是视图的真实宽高
        prepareZones(this.orientation, specWidth, specHeight);
    }

    private void drawZones(Canvas canvas) {
        // 从外到内绘制，以层叠方式覆盖相交部分
        for (int i = this.zones.length - 1; i >= 0; i--) {
            XZone zone = this.zones[i];
            zone.draw(canvas);
        }
    }

    private void drawContentOnZoneLinks(Canvas canvas) {
        Path path = new Path();
        XZone level_1_zone = this.zones[1];

        int textColor = ThemeUtils.getColorByAttrId(getContext(), R.attr.key_fg_color);
        this.textPaint.setColor(textColor);
        this.textPaint.setStyle(Paint.Style.FILL);

        this.textPaint.setTextSize(45f);

        float ctrlFontAscent = -this.textPaint.getFontMetrics().ascent;
        float ctrlFontDescent = this.textPaint.getFontMetrics().descent;

        String[] ctrlLabels = new String[] { "abc", "拼音", "123", "算数", "", "ABC" };
        for (int i = 0; i < level_1_zone.blocks.size(); i++) {
            String text = ctrlLabels[i];

            XZone.PolygonBlock block = (XZone.PolygonBlock) level_1_zone.blocks.get(i);
            PointF start = block.links.center.vertexes.get(1);
            PointF end = block.links.center.vertexes.get(2);

            float vOffset = -ctrlFontDescent;

            this.textPaint.setTextAlign(Paint.Align.CENTER);
            if (i < 3) {
                start = block.links.center.vertexes.get(2);
                end = block.links.center.vertexes.get(1);
            } else {
                vOffset = ctrlFontAscent;
            }

            path.reset();
            path.moveTo(start.x, start.y);
            path.lineTo(end.x, end.y);

            canvas.drawTextOnPath(text, path, 0, vOffset, this.textPaint);
        }

        XZone level_2_zone = this.zones[2];
        this.textPaint.setTextSize(45f);

        float axisPadding = 15f;
        float axisFontAscent = -this.textPaint.getFontMetrics().ascent;
        float axisFontDescent = this.textPaint.getFontMetrics().descent;

        String[][][] axisTextArray = getKeys();
        for (int i = 0; i < level_2_zone.blocks.size(); i++) {
            XZone.PolygonBlock block = (XZone.PolygonBlock) level_2_zone.blocks.get(i);

            for (int j = 0; j < block.links.left.size(); j++) {
                XZone.Link link = block.links.left.get(j);

                String text = axisTextArray[i][0][j];
                PointF start = link.vertexes.get(0);
                PointF end = link.vertexes.get(1);

                float vOffset = -axisFontDescent;
                float hOffset = axisPadding;

                this.textPaint.setTextAlign(Paint.Align.LEFT);
                if (i == 0) {
                    start = link.vertexes.get(2);
                    end = link.vertexes.get(1);
                    hOffset = axisFontDescent;
                    vOffset = axisFontAscent + axisFontDescent;
                } else if (i == 3) {
                    start = link.vertexes.get(1);
                    end = link.vertexes.get(2);
                    hOffset = -axisFontDescent;
                    vOffset = -axisPadding;

                    this.textPaint.setTextAlign(Paint.Align.RIGHT);
                } else if (i < 3) {
                    start = link.vertexes.get(1);
                    end = link.vertexes.get(0);
                    hOffset = -axisPadding;
                    vOffset = axisFontAscent;

                    this.textPaint.setTextAlign(Paint.Align.RIGHT);
                }

                path.reset();
                path.moveTo(end.x, end.y);
                path.lineTo(start.x, start.y);

                canvas.drawTextOnPath(text, path, hOffset, vOffset, this.textPaint);
            }

            for (int j = 0; j < block.links.right.size(); j++) {
                XZone.Link link = block.links.right.get(j);

                String text = axisTextArray[i][1][j];
                PointF start = link.vertexes.get(0);
                PointF end = link.vertexes.get(1);

                float vOffset = -axisFontDescent;
                float hOffset = axisPadding;

                this.textPaint.setTextAlign(Paint.Align.LEFT);
                if (i == 2) {
                    start = link.vertexes.get(1);
                    end = link.vertexes.get(2);
                    hOffset = -axisFontDescent;
                    vOffset = axisFontAscent + axisFontDescent;

                    this.textPaint.setTextAlign(Paint.Align.RIGHT);
                } else if (i == 5) {
                    start = link.vertexes.get(2);
                    end = link.vertexes.get(1);
                    hOffset = axisFontDescent;
                    vOffset = -axisPadding;
                } else if (i < 3) {
                    vOffset = axisFontAscent;
                } else {
                    start = link.vertexes.get(1);
                    end = link.vertexes.get(0);
                    hOffset = -axisPadding;

                    this.textPaint.setTextAlign(Paint.Align.RIGHT);
                }

                path.reset();
                path.moveTo(end.x, end.y);
                path.lineTo(start.x, start.y);

                canvas.drawTextOnPath(text, path, hOffset, vOffset, this.textPaint);
            }
        }
    }

    private void prepareZones(HexagonOrientation orientation, int width, int height) {
        float maxHexagonRadius = Math.min(width * 0.5f, height * 0.5f) - this.padding;

        float innerHexagonRadius = this.hexagonRadius;
        float outerHexagonRadius = orientation == HexagonOrientation.FLAT_TOP
                                   ? maxHexagonRadius * cos_30_divided_by_1
                                   : maxHexagonRadius;

        PointF origin = orientation == HexagonOrientation.FLAT_TOP //
                        ? new PointF(width - outerHexagonRadius - this.padding,
                                     height - maxHexagonRadius - this.padding) //
                        : new PointF(width - outerHexagonRadius * cos_30 - this.padding,
                                     height - outerHexagonRadius - this.padding);

        // 第 0 级分区：中心圆
        String centerCircleBorder = this.dividerStyle;
        int centerCircleBgColor = ThemeUtils.getColorByAttrId(getContext(), R.attr.key_bg_color);

        XZone level_0_zone = this.zones[0] = new XZone();
        level_0_zone.painter.setFillColor(centerCircleBgColor);
        level_0_zone.painter.setStrokeStyle(centerCircleBorder);

        float centerCircleRadius = innerHexagonRadius * 0.4f;
        level_0_zone.path.addCircle(origin.x, origin.y, centerCircleRadius, Path.Direction.CW);

        level_0_zone.blocks.add(new XZone.CircleBlock(origin, centerCircleRadius));

        // 第 1 级分区：内六边形
        XZone level_1_zone = this.zones[1] = new XZone();
        level_1_zone.painter.setStrokeStyle(this.dividerStyle);
        level_1_zone.painter.setCornerRadius(this.hexagonCornerRadius);

        float innerHexagonAxisRadius = centerCircleRadius + (innerHexagonRadius - centerCircleRadius) * 0.25f;
        PointF[] innerHexagonVertexes = ViewUtils.drawHexagon(level_1_zone.path,
                                                              orientation,
                                                              origin,
                                                              innerHexagonRadius);
        PointF[] innerHexagonAxisVertexes = ViewUtils.createHexagon(orientation, origin, innerHexagonAxisRadius);

        for (int i = 0; i < innerHexagonVertexes.length; i++) {
            int currentIndex = i;
            int nextIndex = (i + 1) % innerHexagonVertexes.length;

            PointF current = innerHexagonVertexes[i];
            PointF next = innerHexagonVertexes[nextIndex];

            PointF axisCurrent = innerHexagonAxisVertexes[currentIndex];
            PointF axisNext = innerHexagonAxisVertexes[nextIndex];

            XZone.PolygonBlock block = new XZone.PolygonBlock(origin, current, next);
            level_1_zone.blocks.add(block);

            // 中心 Link 为其可视的梯形区域
            block.links.center.addVertexes(axisCurrent, current, next, axisNext);
        }

        // 第 2 级分区：外六边形，不封边，且射线范围内均为其分区空间
        XZone level_2_zone = this.zones[2] = new XZone();
        level_2_zone.painter.setStrokeStyle(this.dividerStyle);

        PointF[] outerHexagonVertexes = ViewUtils.createHexagon(orientation, origin, outerHexagonRadius);
        for (PointF current : outerHexagonVertexes) {
            level_2_zone.path.moveTo(origin.x, origin.y);
            level_2_zone.path.lineTo(current.x, current.y);
        }

        // - 确定一个最大外边界
        PointF[] maxHexagonBoundVertexes = ViewUtils.createHexagon(orientation, origin, Math.max(width, height));
        for (int i = 0; i < outerHexagonVertexes.length; i++) {
            int currentIndex = i;
            int nextIndex = (i + 1) % maxHexagonBoundVertexes.length;

            PointF current = maxHexagonBoundVertexes[currentIndex];
            PointF next = maxHexagonBoundVertexes[nextIndex];

            XZone.PolygonBlock block = new XZone.PolygonBlock(origin, current, next);
            level_2_zone.blocks.add(block);
        }

        // - 添加垂直于左右轴线的 Link
        int level_2_zone_axis_link_count = 4;
        float outerHexagonAxisSpacing = (outerHexagonRadius - innerHexagonRadius) / level_2_zone_axis_link_count;

        PointF[][] axisHexagonVertexesArray = new PointF[level_2_zone_axis_link_count][];
        for (int i = 0; i < level_2_zone_axis_link_count; i++) {
            float axisHexagonRadius = (innerHexagonRadius + outerHexagonAxisSpacing * (i + 1)) //
                                      * cos_30_divided_by_1;
            PointF[] axisHexagonVertexes = ViewUtils.createHexagon(orientation == HexagonOrientation.FLAT_TOP
                                                                   ? HexagonOrientation.POINTY_TOP
                                                                   : HexagonOrientation.FLAT_TOP,
                                                                   origin,
                                                                   axisHexagonRadius);

            axisHexagonVertexesArray[i] = axisHexagonVertexes;
        }

        for (int i = 0; i < axisHexagonVertexesArray.length - 1; i++) {
            PointF[] innerVertexes = axisHexagonVertexesArray[i];
            PointF[] outerVertexes = axisHexagonVertexesArray[i + 1];

            for (int j = 0; j < innerVertexes.length; j++) {
                int beforeIndex = orientation == HexagonOrientation.FLAT_TOP //
                                  ? (j > 0 ? j : innerVertexes.length) - 1 : j;
                int currentIndex = orientation == HexagonOrientation.FLAT_TOP //
                                   ? j : (j + 1) % innerVertexes.length;
                int afterIndex = orientation == HexagonOrientation.FLAT_TOP //
                                 ? (j + 1) % innerVertexes.length : (j + 2) % innerVertexes.length;

                PointF innerBefore = innerVertexes[beforeIndex];
                PointF innerCurrent = innerVertexes[currentIndex];
                PointF innerAfter = innerVertexes[afterIndex];

                PointF outerBefore = outerVertexes[beforeIndex];
                PointF outerCurrent = outerVertexes[currentIndex];
                PointF outerAfter = outerVertexes[afterIndex];

                XZone.PolygonBlock block = (XZone.PolygonBlock) level_2_zone.blocks.get(j);
                XZone.Link leftLink = new XZone.Link();
                block.links.left.add(leftLink);

                leftLink.addVertexes(innerCurrent,
                                     middle(innerCurrent, innerBefore),
                                     middle(outerCurrent, outerBefore),
                                     outerCurrent);

                XZone.Link rightLink = new XZone.Link();
                block.links.right.add(rightLink);

                rightLink.addVertexes(innerCurrent,
                                      middle(innerCurrent, innerAfter),
                                      middle(outerCurrent, outerAfter),
                                      outerCurrent);
            }
        }
    }

    private PointF middle(PointF p1, PointF p2) {
        return new PointF((p1.x + p2.x) / 2f, (p1.y + p2.y) / 2f);
    }

    private void drawTrailPath(Canvas canvas) {
        // https://github.com/8VIM/8VIM/blob/master/8vim/src/main/java/inc/flide/vim8/views/mainkeyboard/XpadView.java#L407
        this.trailPathMeasure.setPath(this.trailPath, false);

        float pathLength = this.trailPathMeasure.getLength();
        for (short i = 1; i <= TRAIL_STEPS; i++) {
            float distance = pathLength - i * TRAIL_STEP_DISTANCE;
            if (distance < 0) {
                continue;
            }

            float trailRadius = TRAIL_MAX_RADIUS * (1 - (float) i / TRAIL_STEPS);
            this.trailPathMeasure.getPosTan(distance, this.trailPathPos, null);

            float x = this.trailPathPos[0];
            float y = this.trailPathPos[1];

            canvas.drawCircle(x, y, trailRadius, this.trailPaint);
        }
    }

    private String[][][] getKeys() {
        return new String[][][] {
                new String[][] {
                        new String[] { "i", "u", "ü" }, new String[] { "", "Space", "" },
                        }, //
                new String[][] {
                        new String[] { "", "", "" }, new String[] { "p", "w", "y" },
                        }, //
                new String[][] {
                        new String[] { "a", "e", "o" }, new String[] { "h", "k", "t" },
                        }, //
                new String[][] {
                        new String[] { "n", "l", "m" }, new String[] { "d", "b", "f" },
                        }, //
                new String[][] {
                        new String[] { "j", "q", "x" }, new String[] { "z", "c", "s" },
                        }, //
                new String[][] {
                        new String[] { "zh", "ch", "sh" }, new String[] { "", "r", "g" },
                        },
                };
    }
}
