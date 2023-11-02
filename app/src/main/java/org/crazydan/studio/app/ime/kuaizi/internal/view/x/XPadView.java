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
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
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
    private int hexagonRadius = 136;
    private int hexagonCornerRadius = 16;

    public XPadView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

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
        drawContentOnZoneLinks(canvas, this.orientation);

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

    private void drawContentOnZoneLinks(Canvas canvas, HexagonOrientation orientation) {
        // TODO 按下后呈按压状态
        XZone level_0_zone = this.zones[0];
        XZone.CircleBlock centerCircleBlock = (XZone.CircleBlock) level_0_zone.blocks.get(0);
        draw(canvas, R.drawable.ic_input_cursor, centerCircleBlock.center, centerCircleBlock.radius);

        Path textPath = new Path();
        XZone level_1_zone = this.zones[1];

        // TODO 按下控制建后，其余按键图标不显示，被按下按键置灰，整个分区呈按压状态
        float ctrl_icon_size = 48f;
        Integer[] ctrlIcons = new Integer[] {
                R.drawable.ic_switch_to_latin,
                R.drawable.ic_switch_to_pinyin,
                R.drawable.ic_emoji,
                R.drawable.ic_calculator,
                null,
                R.drawable.ic_symbol
        };
        for (int i = 0; i < level_1_zone.blocks.size(); i++) {
            Integer icon = ctrlIcons[i];
            if (icon == null) {
                continue;
            }

            XZone.PolygonBlock block = (XZone.PolygonBlock) level_1_zone.blocks.get(i);
            PointF center = block.links.center.center;

            canvas.save();
            draw(canvas, icon, center, ctrl_icon_size, //
                 orientation == HexagonOrientation.POINTY_TOP //
                 ? 30 * (2 * i - 1) : 60 * (i - 1));
            canvas.restore();
        }

        XZone level_2_zone = this.zones[2];
        int textColor = ThemeUtils.getColorByAttrId(getContext(), R.attr.key_fg_color);
        this.textPaint.setColor(textColor);
        this.textPaint.setStyle(Paint.Style.FILL);
        this.textPaint.setTextSize(40f);

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
                if (i == 0 //
                    || (orientation == HexagonOrientation.POINTY_TOP //
                        && i == 5)) {
                    start = link.vertexes.get(2);
                    end = link.vertexes.get(1);
                    hOffset = axisFontDescent;
                    vOffset = axisFontAscent + axisFontDescent;
                } else if (i == 3 //
                           || (orientation == HexagonOrientation.POINTY_TOP //
                               && i == 2)) {
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

                textPath.reset();
                textPath.moveTo(end.x, end.y);
                textPath.lineTo(start.x, start.y);

                canvas.drawTextOnPath(text, textPath, hOffset, vOffset, this.textPaint);
            }

            for (int j = 0; j < block.links.right.size(); j++) {
                XZone.Link link = block.links.right.get(j);

                String text = axisTextArray[i][1][j];
                PointF start = link.vertexes.get(0);
                PointF end = link.vertexes.get(1);

                float vOffset = -axisFontDescent;
                float hOffset = axisPadding;

                this.textPaint.setTextAlign(Paint.Align.LEFT);
                if (i == 2 //
                    || (orientation == HexagonOrientation.POINTY_TOP //
                        && i == 1)) {
                    start = link.vertexes.get(1);
                    end = link.vertexes.get(2);
                    hOffset = -axisFontDescent;
                    vOffset = axisFontAscent + axisFontDescent;

                    this.textPaint.setTextAlign(Paint.Align.RIGHT);
                } else if (i == 5 //
                           || (orientation == HexagonOrientation.POINTY_TOP //
                               && i == 4)) {
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

                textPath.reset();
                textPath.moveTo(end.x, end.y);
                textPath.lineTo(start.x, start.y);

                canvas.drawTextOnPath(text, textPath, hOffset, vOffset, this.textPaint);
            }
        }
    }

    private void prepareZones(HexagonOrientation orientation, int width, int height) {
        float maxHexagonRadius = Math.min(width * 0.5f, height * 0.5f) - this.padding;

        float innerHexagonRadius = this.hexagonRadius;
        float innerHexagonCornerRadius = this.hexagonCornerRadius;
        float outerHexagonRadius = orientation == HexagonOrientation.FLAT_TOP
                                   ? maxHexagonRadius * cos_30_divided_by_1
                                   : maxHexagonRadius;

        PointF origin = orientation == HexagonOrientation.FLAT_TOP //
                        ? new PointF(width - outerHexagonRadius - this.padding,
                                     height - maxHexagonRadius - this.padding) //
                        : new PointF(width - outerHexagonRadius * cos_30 - this.padding,
                                     height - outerHexagonRadius - this.padding);

        // ==================================================
        // 第 0 级分区：中心圆
        XZone level_0_zone = this.zones[0] = new XZone();

        int level_0_zone_bg_color = ThemeUtils.getColorByAttrId(getContext(), R.attr.key_ctrl_locator_bg_color);
        String level_0_zone_shadow = ThemeUtils.getStringByAttrId(getContext(), R.attr.key_shadow_style);

        XPainter level_0_zone_fill_painter = level_0_zone.newPainter();
        level_0_zone_fill_painter.setFillShadow(level_0_zone_shadow);
        level_0_zone_fill_painter.setFillColor(level_0_zone_bg_color);

        float centerCircleRadius = innerHexagonRadius * 0.4f;
        level_0_zone_fill_painter.path.addCircle(origin.x, origin.y, centerCircleRadius, Path.Direction.CW);

        level_0_zone.blocks.add(new XZone.CircleBlock(origin, centerCircleRadius));

        // ==================================================
        // 第 1 级分区：内六边形
        XZone level_1_zone = this.zones[1] = new XZone();

        int level_1_zone_bg_color = ThemeUtils.getColorByAttrId(getContext(), R.attr.key_bg_color);
        String level_1_zone_divider_style = ThemeUtils.getStringByAttrId(getContext(),
                                                                         R.attr.x_keyboard_ctrl_divider_style);
        String level_1_zone_shadow_style = ThemeUtils.getStringByAttrId(getContext(), R.attr.x_keyboard_shadow_style);

        XPainter level_1_zone_fill_painter = level_1_zone.newPainter();
        level_1_zone_fill_painter.setFillColor(level_1_zone_bg_color);
        level_1_zone_fill_painter.setFillShadow(level_1_zone_shadow_style);
        level_1_zone_fill_painter.setCornerRadius(innerHexagonCornerRadius);

        float innerHexagonAxisRadius = centerCircleRadius + (innerHexagonRadius - centerCircleRadius) * 0.25f;
        PointF[] innerHexagonVertexes = ViewUtils.drawHexagon(level_1_zone_fill_painter.path,
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
        // 去掉与重新圆重复的部分，以避免中心圆的背景色被覆盖
        level_1_zone_fill_painter.path.op(level_0_zone_fill_painter.path, Path.Op.XOR);

        // - 绘制分隔线
        XPainter level_1_zone_stroke_painter = level_1_zone.newPainter();
        level_1_zone_stroke_painter.setStrokeStyle(level_1_zone_divider_style);

        PointF[] innerHexagonCropCornerVertexes = ViewUtils.createHexagon(orientation,
                                                                          origin,
                                                                          innerHexagonRadius
                                                                          - innerHexagonCornerRadius * 0.2f);
        PointF[] innerHexagonCrossCircleVertexes = ViewUtils.createHexagon(orientation, origin, centerCircleRadius);
        for (int i = 0; i < innerHexagonVertexes.length; i++) {
            PointF start = innerHexagonCrossCircleVertexes[i];
            PointF end = innerHexagonCropCornerVertexes[i];

            level_1_zone_stroke_painter.path.moveTo(start.x, start.y);
            level_1_zone_stroke_painter.path.lineTo(end.x, end.y);
        }

        // ==================================================
        // 第 2 级分区：外六边形，不封边，且射线范围内均为其分区空间
        XZone level_2_zone = this.zones[2] = new XZone();

        String level_2_zone_divider_style = ThemeUtils.getStringByAttrId(getContext(),
                                                                         R.attr.x_keyboard_chars_divider_style);
        String level_2_zone_shadow_style = ThemeUtils.getStringByAttrId(getContext(), R.attr.x_keyboard_shadow_style);

        XPainter level_2_zone_stroke_painter = level_2_zone.newPainter();
        level_2_zone_stroke_painter.setStrokeStyle(level_2_zone_divider_style);
        level_2_zone_stroke_painter.setStrokeShadow(level_2_zone_shadow_style);

        PointF[] outerHexagonVertexes = ViewUtils.createHexagon(orientation, origin, outerHexagonRadius);
        for (int i = 0; i < outerHexagonVertexes.length; i++) {
            PointF start = innerHexagonCrossCircleVertexes[i];
            PointF end = outerHexagonVertexes[i];

            level_2_zone_stroke_painter.path.moveTo(start.x, start.y);
            level_2_zone_stroke_painter.path.lineTo(end.x, end.y);
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

    private void draw(Canvas canvas, int resId, PointF center, float size) {
        draw(canvas, resId, center, size, 0);
    }

    private void draw(Canvas canvas, int resId, PointF center, float size, float rotate) {
        Drawable drawable = AppCompatResources.getDrawable(getContext(), resId);
        drawable.setBounds(0, 0, (int) size, (int) size);

        canvas.save();

        if (rotate != 0) {
            canvas.rotate(rotate, center.x, center.y);
        }
        float offset = size * 0.5f;
        canvas.translate(center.x - offset, center.y - offset);
        drawable.draw(canvas);

        canvas.restore();
    }

    private String[][][] getKeys() {
        return new String[][][] {
                new String[][] {
                        new String[] { "i", "u", "ü" }, new String[] { "", "Space", "Newline" },
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
