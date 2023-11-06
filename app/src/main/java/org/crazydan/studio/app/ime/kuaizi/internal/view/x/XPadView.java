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
import org.crazydan.studio.app.ime.kuaizi.widget.ViewGestureDetector;
import org.crazydan.studio.app.ime.kuaizi.widget.ViewGestureTrailer;
import org.hexworks.mixite.core.api.HexagonOrientation;

/**
 * X 型全滑屏输入面板视图
 * <p/>
 * 参考设计和代码来源于：<a href="https://github.com/8VIM/8VIM">8VIM/8VIM</a>
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-10-29
 */
public class XPadView extends View implements ViewGestureDetector.Listener {
    private static final float cos_30 = (float) Math.cos(Math.toRadians(30));
    private static final float cos_30_divided_by_1 = 1f / cos_30;

    private final HexagonOrientation orientation = HexagonOrientation.FLAT_TOP;
    private final XZone[] zones = new XZone[3];
    private final ViewGestureDetector gesture;
    private final ViewGestureTrailer trailer;

    /** 中心正六边形半径 */
    private float centerHexagonRadius;
    /** 中心坐标 */
    private PointF centerCoord;
    private int[] activeBlock;

    public XPadView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        ViewUtils.enableHardwareAccelerated(this);

        this.centerHexagonRadius = dimens(R.dimen.key_view_bg_min_radius);

        this.trailer = new ViewGestureTrailer();
        this.trailer.setColor(attrColor(R.attr.input_trail_color));

        this.gesture = new ViewGestureDetector();
        this.gesture.addListener(this) //
                    .addListener(this.trailer);
    }

    public void setCenterHexagonRadius(float centerHexagonRadius) {
        this.centerHexagonRadius = centerHexagonRadius;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent e) {
        invalidate();

        this.gesture.onTouchEvent(e);

        // Note：需要返回 true 才能拦截到 move 等事件
        return true;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // 准备分区的待显示内容
        prepareContentOnZone(this.orientation);

        // 绘制分区
        drawZones(canvas);

        // 绘制滑屏轨迹
        this.trailer.draw(canvas);
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

    @Override
    public void onGesture(ViewGestureDetector.GestureType type, ViewGestureDetector.GestureData data) {
        switch (type) {
            case PressStart:
                this.zones[0].press();
                break;
            case PressEnd:
                this.zones[0].bounce();
                break;
            case MovingStart:
            case Moving:
                this.activeBlock = findActiveBlock(data);
                break;
            case MovingEnd:
                this.activeBlock = null;
                break;
        }
    }

    private void drawZones(Canvas canvas) {
        // 从外到内绘制，以层叠方式覆盖相交部分
        for (int i = this.zones.length - 1; i >= 0; i--) {
            XZone zone = this.zones[i];
            zone.draw(canvas, this.centerCoord);
        }
    }

    private int[] findActiveBlock(ViewGestureDetector.GestureData data) {
        for (int i = 0; i < this.zones.length; i++) {
            XZone zone = this.zones[i];

            for (int j = 0; j < zone.blocks.size(); j++) {
                XZone.Block block = zone.blocks.get(j);

                if (block.contains(new PointF(data.x, data.y))) {
                    return new int[] { i, j };
                }
            }
        }
        return null;
    }

    private void prepareContentOnZone(HexagonOrientation orientation) {
        // ==============================================
        XZone level_0_zone = this.zones[0];
        level_0_zone.clearIconPainters();

        XZone.CircleBlock centerCircleBlock = (XZone.CircleBlock) level_0_zone.blocks.get(0);

        Drawable level_0_zone_icon = drawable(R.drawable.ic_input_cursor);
        XDrawablePainter level_0_zone_icon_painter = level_0_zone.newIconPainter(level_0_zone_icon);
        level_0_zone_icon_painter.setSize(centerCircleBlock.radius);
        level_0_zone_icon_painter.setCenter(centerCircleBlock.center);

        // ==============================================
        XZone level_1_zone = this.zones[1];
        level_1_zone.clearIconPainters();

        float ctrl_icon_size = dimens(R.dimen.x_keyboard_ctrl_icon_size);
        Integer[] ctrlIcons = new Integer[] {
                R.drawable.ic_switch_to_latin,
                R.drawable.ic_switch_to_pinyin,
                R.drawable.ic_emoji,
                R.drawable.ic_calculator,
                null,
                R.drawable.ic_symbol
        };
        for (int i = 0; i < level_1_zone.blocks.size(); i++) {
            Integer iconResId = ctrlIcons[i];
            if (iconResId == null) {
                continue;
            }

            XZone.PolygonBlock block = (XZone.PolygonBlock) level_1_zone.blocks.get(i);
            PointF center = block.links.center.center;
            float rotate = orientation == HexagonOrientation.POINTY_TOP //
                           ? 30 * (2 * i - 1) : 60 * (i - 1);

            Drawable icon = drawable(iconResId);
            XDrawablePainter level_1_zone_icon_painter = level_1_zone.newIconPainter(icon);
            level_1_zone_icon_painter.setSize(ctrl_icon_size);
            level_1_zone_icon_painter.setCenter(center);
            level_1_zone_icon_painter.setRotate(rotate);
        }

        // ==============================================
        XZone level_2_zone = this.zones[2];
        level_2_zone.clearTextPainters();

        float textSize = dimens(R.dimen.x_keyboard_chars_text_size);
        float textPadding = dimens(R.dimen.x_keyboard_chars_text_padding);

        String[][][] axisTextArray = getKeys();
        for (int i = 0; i < level_2_zone.blocks.size(); i++) {
            int textColor = this.activeBlock != null //
                            && this.activeBlock[0] == 2 //
                            && this.activeBlock[1] == i
                            ? attrColor(R.attr.x_keyboard_chars_highlight_fg_color)
                            : attrColor(R.attr.x_keyboard_chars_fg_color);
            XZone.PolygonBlock block = (XZone.PolygonBlock) level_2_zone.blocks.get(i);

            for (int j = 0; j < block.links.left.size(); j++) {
                String text = axisTextArray[i][0][j];
                XZone.Link link = block.links.left.get(j);

                PointF start = link.vertexes.get(0);
                PointF end = link.vertexes.get(1);

                XPathTextPainter textPainter = level_2_zone.newTextPainter(text);
                textPainter.setTextSize(textSize);
                textPainter.setFillColor(textColor);

                float fontAscent = -textPainter.getFontAscent();
                float fontDescent = textPainter.getFontDescent();
                float vOffset = -fontDescent;
                float hOffset = textPadding;
                Paint.Align align = Paint.Align.LEFT;

                if (i == 0 //
                    || (orientation == HexagonOrientation.POINTY_TOP //
                        && i == 5)) {
                    start = link.vertexes.get(2);
                    end = link.vertexes.get(1);

                    hOffset = fontDescent;
                    vOffset = fontAscent + fontDescent;
                } else if (i == 3 //
                           || (orientation == HexagonOrientation.POINTY_TOP //
                               && i == 2)) {
                    start = link.vertexes.get(1);
                    end = link.vertexes.get(2);

                    hOffset = -fontDescent;
                    vOffset = -textPadding;
                    align = Paint.Align.RIGHT;
                } else if (i < 3) {
                    start = link.vertexes.get(1);
                    end = link.vertexes.get(0);

                    hOffset = -textPadding;
                    vOffset = fontAscent;
                    align = Paint.Align.RIGHT;
                }

                textPainter.path.reset();
                textPainter.path.moveTo(end.x, end.y);
                textPainter.path.lineTo(start.x, start.y);

                textPainter.setTextAlign(align);
                textPainter.setOffset(hOffset, vOffset);
            }

            for (int j = 0; j < block.links.right.size(); j++) {
                String text = axisTextArray[i][1][j];
                XZone.Link link = block.links.right.get(j);

                PointF start = link.vertexes.get(0);
                PointF end = link.vertexes.get(1);

                XPathTextPainter textPainter = level_2_zone.newTextPainter(text);
                textPainter.setTextSize(textSize);
                textPainter.setFillColor(textColor);

                float fontAscent = -textPainter.getFontAscent();
                float fontDescent = textPainter.getFontDescent();
                float vOffset = -fontDescent;
                float hOffset = textPadding;
                Paint.Align align = Paint.Align.LEFT;

                if (i == 2 //
                    || (orientation == HexagonOrientation.POINTY_TOP //
                        && i == 1)) {
                    start = link.vertexes.get(1);
                    end = link.vertexes.get(2);

                    hOffset = -fontDescent;
                    vOffset = fontAscent + fontDescent;
                    align = Paint.Align.RIGHT;
                } else if (i == 5 //
                           || (orientation == HexagonOrientation.POINTY_TOP //
                               && i == 4)) {
                    start = link.vertexes.get(2);
                    end = link.vertexes.get(1);

                    hOffset = fontDescent;
                    vOffset = -textPadding;
                } else if (i < 3) {
                    vOffset = fontAscent;
                } else {
                    start = link.vertexes.get(1);
                    end = link.vertexes.get(0);

                    hOffset = -textPadding;
                    align = Paint.Align.RIGHT;
                }

                textPainter.path.reset();
                textPainter.path.moveTo(end.x, end.y);
                textPainter.path.lineTo(start.x, start.y);

                textPainter.setTextAlign(align);
                textPainter.setOffset(hOffset, vOffset);
            }
        }
    }

    private void prepareZones(HexagonOrientation orientation, int width, int height) {
        float padPadding = dimens(R.dimen.x_keyboard_pad_padding);
        float maxHexagonRadius = Math.min(width * 0.5f, height * 0.5f) - padPadding;

        float innerHexagonRadius = this.centerHexagonRadius / 0.45f;
        float innerHexagonCornerRadius = dimens(R.dimen.x_keyboard_ctrl_pad_corner_radius);
        float outerHexagonRadius = orientation == HexagonOrientation.FLAT_TOP
                                   ? maxHexagonRadius * cos_30_divided_by_1
                                   : maxHexagonRadius;

        PointF origin = orientation == HexagonOrientation.FLAT_TOP //
                        ? new PointF(width - outerHexagonRadius - padPadding, height - maxHexagonRadius - padPadding) //
                        : new PointF(width - outerHexagonRadius * cos_30 - padPadding,
                                     height - outerHexagonRadius - padPadding);
        this.centerCoord = origin;

        // ==================================================
        // 第 0 级分区：中心圆
        float centerCircleRadius = this.centerHexagonRadius;
        float centerHexagonCornerRadius = dimens(R.dimen.key_view_corner_radius);
        XZone level_0_zone = this.zones[0] = new XZone();

        int level_0_zone_bg_color = attrColor(R.attr.key_ctrl_locator_bg_color);
        String level_0_zone_shadow = attrString(R.attr.key_shadow_style);

        XPathPainter level_0_zone_fill_painter = level_0_zone.newPathPainter();
        level_0_zone_fill_painter.setFillShadow(level_0_zone_shadow);
        level_0_zone_fill_painter.setFillColor(level_0_zone_bg_color);
        level_0_zone_fill_painter.setCornerRadius(centerHexagonCornerRadius);

//        level_0_zone_fill_painter.path.addCircle(origin.x, origin.y, centerCircleRadius, Path.Direction.CW);
        ViewUtils.drawHexagon(level_0_zone_fill_painter.path,
//                              orientation == HexagonOrientation.FLAT_TOP
//                              ? HexagonOrientation.POINTY_TOP
//                              : HexagonOrientation.FLAT_TOP,
                              orientation, origin, centerCircleRadius);

        level_0_zone.blocks.add(new XZone.CircleBlock(origin, centerCircleRadius));

        // ==================================================
        // 第 1 级分区：内六边形
        XZone level_1_zone = this.zones[1] = new XZone();

        int level_1_zone_bg_color = attrColor(R.attr.x_keyboard_ctrl_bg_style);
        String level_1_zone_divider_style = attrString(R.attr.x_keyboard_ctrl_divider_style);
        String level_1_zone_shadow_style = attrString(R.attr.x_keyboard_shadow_style);

        XPathPainter level_1_zone_fill_painter = level_1_zone.newPathPainter();
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
        XPathPainter level_1_zone_stroke_painter = level_1_zone.newPathPainter();
        level_1_zone_stroke_painter.setStrokeCap(Paint.Cap.ROUND);
        level_1_zone_stroke_painter.setStrokeStyle(level_1_zone_divider_style);

        // Note：圆角是通过指定半径的圆与矩形的角相切再去掉角的外部后得到的；切点过圆心的线一定与切线垂直
        // - 不清楚为何实际绘制的圆角半径是定义半径的 1.6 倍？
        float innerHexagonCornerActualRadius = innerHexagonCornerRadius * 1.6f;
        float innerHexagonCropRadius = innerHexagonRadius - innerHexagonCornerActualRadius * (cos_30_divided_by_1 - 1);
        float innerHexagonCrossRadius = centerCircleRadius * 0.9f; // 确保中心按下后依然能够显示分隔线
        PointF[] innerHexagonCropCornerVertexes = ViewUtils.createHexagon(orientation, origin, //
                                                                          innerHexagonCropRadius);
        PointF[] innerHexagonCrossCircleVertexes = ViewUtils.createHexagon(orientation,
                                                                           origin,
                                                                           innerHexagonCrossRadius);
        for (int i = 0; i < innerHexagonVertexes.length; i++) {
            PointF start = innerHexagonCrossCircleVertexes[i];
            PointF end = innerHexagonCropCornerVertexes[i];

            level_1_zone_stroke_painter.path.moveTo(start.x, start.y);
            level_1_zone_stroke_painter.path.lineTo(end.x, end.y);
        }

//        PointF[] vertexes = ViewUtils.createHexagon(orientation,
//                                                    origin,
//                                                    innerHexagonRadius
//                                                    - innerHexagonCornerActualRadius * cos_30_divided_by_1);
//        for (PointF start : vertexes) {
//            level_1_zone_stroke_painter.path.addCircle(start.x,
//                                                       start.y,
//                                                       innerHexagonCornerActualRadius,
//                                                       Path.Direction.CW);
//        }

        // ==================================================
        // 第 2 级分区：外六边形，不封边，且射线范围内均为其分区空间
        XZone level_2_zone = this.zones[2] = new XZone();

        String level_2_zone_divider_style = attrString(R.attr.x_keyboard_chars_divider_style);
        String level_2_zone_shadow_style = attrString(R.attr.x_keyboard_chars_divider_shadow_style);

        XPathPainter level_2_zone_stroke_painter = level_2_zone.newPathPainter();
        level_2_zone_stroke_painter.setStrokeCap(Paint.Cap.ROUND);
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

    private Drawable drawable(int resId) {
        return AppCompatResources.getDrawable(getContext(), resId);
    }

    private float dimens(int resId) {
        return ScreenUtils.pxFromDimension(getContext(), resId);
    }

    private int attrColor(int resId) {
        return ThemeUtils.getColorByAttrId(getContext(), resId);
    }

    private String attrString(int resId) {
        return ThemeUtils.getStringByAttrId(getContext(), resId);
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
