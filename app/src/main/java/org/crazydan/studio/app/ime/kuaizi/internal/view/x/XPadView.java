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
import org.crazydan.studio.app.ime.kuaizi.internal.Key;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.UserKeyMsgListener;
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
public class XPadView extends View {
    private final HexagonOrientation orientation = HexagonOrientation.FLAT_TOP;
    private final ViewGestureTrailer trailer;
    private final XZone[] zones = new XZone[3];

    private BlockKey zone_0_key;
    private BlockKey[] zone_1_keys;
    private BlockKey[][][] zone_2_keys;

    private boolean reversed;
    /** 中心正六边形半径 */
    private float centerHexagonRadius;
    /** 中心坐标 */
    private PointF centerCoord;

    public XPadView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        ViewUtils.enableHardwareAccelerated(this);

        this.centerHexagonRadius = dimen(R.dimen.key_view_bg_min_radius);

        this.trailer = new ViewGestureTrailer();
        this.trailer.setColor(attrColor(R.attr.input_trail_color));
    }

    public void setReversed(boolean reversed) {
        if (this.reversed != reversed) {
            // 触发重绘
            invalidate();
        }
        this.reversed = reversed;
    }

    public void setCenterHexagonRadius(float centerHexagonRadius) {
        this.centerHexagonRadius = centerHexagonRadius;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent e) {
        // Note：由上层视图处理事件，并将属于本视图的事件通过 #onGesture 转发到本视图内处理
        return true;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // 整体的镜像翻转：不适用，应该调换左右位置即可，文字和图片等不能翻转
        //canvas.scale(-1, 1, getWidth() * 0.5f, getHeight() * 0.5f);
        // 将画布整体向左偏移，按键放置位置左右翻转，从而实现键盘的翻转
        if (this.reversed) {
            float dx = getWidth() - this.centerCoord.x * 2;
            canvas.translate(dx, 0);
        }

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

    public void onGesture(
            ViewGestureDetector.GestureType type, ViewGestureDetector.GestureData data, PointF offset,
            UserKeyMsgListener.Executor userKeyMsgListenerExecutor
    ) {
        invalidate();

        float x = data.x + offset.x;
        float y = data.y + offset.y;
        BlockKey blockKey = findBlockKey(x, y);
        XZone centerZone = this.zones[0];
        boolean isCenterBlockKey = blockKey != null && blockKey.zone == 0;

        this.trailer.onGesture(type, ViewGestureDetector.GestureData.newFrom(data, x, y));

        switch (type) {
            case PressStart: {
                if (isCenterBlockKey) {
                    centerZone.press();
                }
                break;
            }
            // Note：长按中心按钮会发生键盘切换，故而，需显式弹起
            case LongPressStart:
            case PressEnd: {
                centerZone.bounce();
                break;
            }
        }

        // Note：向外部传递的 GestureData 不需要做坐标转换
        userKeyMsgListenerExecutor.onGesture(blockKey != null ? blockKey.key : null, type, data);
    }

    public void updateZoneKeys(Key<?> zone_0_key, Key<?>[] zone_1_keys, Key<?>[][][] zone_2_keys) {
        invalidate();

        this.zone_0_key = new BlockKey(0, 0, 0, 0, zone_0_key);

        this.zone_1_keys = new BlockKey[zone_1_keys.length];
        for (int i = 0; i < zone_1_keys.length; i++) {
            Key<?> key = zone_1_keys[i];
            this.zone_1_keys[i] = new BlockKey(1, i, 0, 0, key);
        }

        this.zone_2_keys = new BlockKey[zone_2_keys.length][][];
        for (int i = 0; i < zone_2_keys.length; i++) {
            Key<?>[][] keys = zone_2_keys[i];

            this.zone_2_keys[i] = new BlockKey[keys.length][];
            for (int j = 0; j < keys.length; j++) {
                Key<?>[] array = keys[j];

                this.zone_2_keys[i][j] = new BlockKey[array.length];
                for (int k = 0; k < array.length; k++) {
                    Key<?> key = array[k];
                    this.zone_2_keys[i][j][k] = new BlockKey(2, i, j, k, key);
                }
            }
        }
    }

    private void drawZones(Canvas canvas) {
        // 从外到内绘制，以层叠方式覆盖相交部分
        for (int i = this.zones.length - 1; i >= 0; i--) {
            XZone zone = this.zones[i];
            zone.draw(canvas, this.centerCoord);
        }
    }

    private BlockKey findBlockKey(float x, float y) {
        int[] blockPos = findActiveBlock(x, y);
        if (blockPos == null) {
            return null;
        }

        int zone = blockPos[0];
        int block = blockPos[1];

        switch (zone) {
            case 0: {
                return this.zone_0_key;
            }
            case 1: {
                return getAt(this.zone_1_keys, block);
            }
        }
        return null;
    }

    private int[] findActiveBlock(float x, float y) {
        for (int i = 0; i < this.zones.length; i++) {
            XZone zone = this.zones[i];

            for (int j = 0; j < zone.blocks.size(); j++) {
                XZone.Block block = zone.blocks.get(j);

                if (block.contains(new PointF(x, y))) {
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

        Drawable level_0_zone_icon = drawable(this.zone_0_key.key.getIconResId());
        XDrawablePainter level_0_zone_icon_painter = level_0_zone.newIconPainter(level_0_zone_icon);
        level_0_zone_icon_painter.setSize(this.centerHexagonRadius);
        level_0_zone_icon_painter.setCenter(this.centerCoord);

        // ==============================================
        XZone level_1_zone = this.zones[1];
        level_1_zone.clearIconPainters();

        float ctrl_icon_size = dimen(R.dimen.x_keyboard_ctrl_icon_size);
        for (int i = 0; i < level_1_zone.blocks.size(); i++) {
            BlockKey blockKey = getAt(this.zone_1_keys, i);
            if (blockKey.isNull()) {
                continue;
            }

            XZone.PolygonBlock block = (XZone.PolygonBlock) level_1_zone.blocks.get(i);
            PointF center = block.links.center.center;
            float rotate = orientation == HexagonOrientation.POINTY_TOP //
                           ? 30 * (2 * i - 1) : 60 * (i - 1);

            Drawable icon = drawable(blockKey.key.getIconResId());
            XDrawablePainter level_1_zone_icon_painter = level_1_zone.newIconPainter(icon);
            level_1_zone_icon_painter.setSize(ctrl_icon_size);
            level_1_zone_icon_painter.setCenter(center);
            level_1_zone_icon_painter.setRotate(rotate);
            level_1_zone_icon_painter.setAlpha(blockKey.key.isDisabled() ? 0.4f : 1f);
        }

        // ==============================================
        XZone level_2_zone = this.zones[2];
        level_2_zone.clearTextPainters();

        float textSize = dimen(R.dimen.x_keyboard_chars_text_size);
        float textPadding = dimen(R.dimen.x_keyboard_chars_text_padding);

        for (int i = 0; i < level_2_zone.blocks.size(); i++) {
            int textColor = attrColor(R.attr.x_keyboard_chars_fg_color);
            XZone.PolygonBlock block = (XZone.PolygonBlock) level_2_zone.blocks.get(i);

            BlockKey[][] blockKeys = getAt(this.zone_2_keys, i);
            for (int j = 0; j < block.links.left.size(); j++) {
                BlockKey blockKey = blockKeys[this.reversed ? 1 : 0][j];
                if (blockKey.isNull()) {
                    continue;
                }

                XZone.Link link = block.links.left.get(j);

                PointF start = link.vertexes.get(0);
                PointF end = link.vertexes.get(1);

                XPathTextPainter textPainter = level_2_zone.newTextPainter(blockKey.key.getLabel());
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
                BlockKey blockKey = blockKeys[this.reversed ? 0 : 1][j];
                if (blockKey.isNull()) {
                    continue;
                }

                XZone.Link link = block.links.right.get(j);

                PointF start = link.vertexes.get(0);
                PointF end = link.vertexes.get(1);

                XPathTextPainter textPainter = level_2_zone.newTextPainter(blockKey.key.getLabel());
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
        float cos_30 = (float) Math.cos(Math.toRadians(30));
        float cos_30_divided_by_1 = 1f / cos_30;

        float padPadding = dimen(R.dimen.x_keyboard_pad_padding);
        float maxHexagonRadius = Math.min(width * 0.5f, height * 0.5f) - padPadding;

        float innerHexagonRadius = this.centerHexagonRadius / 0.45f;
        float innerHexagonCornerRadius = dimen(R.dimen.x_keyboard_ctrl_pad_corner_radius);
        float outerHexagonRadius = orientation == HexagonOrientation.FLAT_TOP
                                   ? maxHexagonRadius * cos_30_divided_by_1
                                   : maxHexagonRadius;

        PointF origin = orientation == HexagonOrientation.FLAT_TOP
                        ? new PointF(width - (outerHexagonRadius
                                              + padPadding),
                                     height - maxHexagonRadius - padPadding)
                        : new PointF(width - (outerHexagonRadius * cos_30 + padPadding),
                                     height - outerHexagonRadius - padPadding);
        this.centerCoord = origin;

        // ==================================================
        // 第 0 级分区：中心圆
        float centerHexagonRadius = this.centerHexagonRadius;
        float centerHexagonCornerRadius = dimen(R.dimen.key_view_corner_radius);
        XZone level_0_zone = this.zones[0] = new XZone();

        int level_0_zone_bg_color = attrColor(R.attr.key_ctrl_locator_bg_color);
        String level_0_zone_shadow = attrString(R.attr.key_shadow_style);

        XPathPainter level_0_zone_fill_painter = level_0_zone.newPathPainter();
        level_0_zone_fill_painter.setFillShadow(level_0_zone_shadow);
        level_0_zone_fill_painter.setFillColor(level_0_zone_bg_color);
        level_0_zone_fill_painter.setCornerRadius(centerHexagonCornerRadius);

        PointF[] centerHexagonVertexes = ViewUtils.drawHexagon(level_0_zone_fill_painter.path,
                                                               orientation,
                                                               origin,
                                                               centerHexagonRadius);
        level_0_zone.blocks.add(new XZone.PolygonBlock(centerHexagonVertexes));

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

        float innerHexagonAxisRadius = centerHexagonRadius + (innerHexagonRadius - centerHexagonRadius) * 0.25f;
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
        float innerHexagonCrossRadius = centerHexagonRadius * 0.9f; // 确保中心按下后依然能够显示分隔线
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

    private float dimen(int resId) {
        return ScreenUtils.pxFromDimension(getContext(), resId);
    }

    private int attrColor(int resId) {
        return ThemeUtils.getColorByAttrId(getContext(), resId);
    }

    private String attrString(int resId) {
        return ThemeUtils.getStringByAttrId(getContext(), resId);
    }

    private <T> T getAt(T[] keys, int index) {
        int size = keys.length;
        int idx = this.reversed ? (size + (size / 2 - 1) - index) % size : index;

        return keys[idx];
    }

    private static class BlockKey {
        public final int zone;
        public final int x;
        public final int y;
        public final int z;
        public final Key<?> key;

        public BlockKey(int zone, int x, int y, int z, Key<?> key) {
            this.zone = zone;
            this.x = x;
            this.y = y;
            this.z = z;
            this.key = key;
        }

        public boolean isNull() {
            return this.key == null;
        }
    }
}
