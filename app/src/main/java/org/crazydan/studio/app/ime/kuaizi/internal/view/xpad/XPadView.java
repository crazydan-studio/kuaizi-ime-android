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

package org.crazydan.studio.app.ime.kuaizi.internal.view.xpad;

import java.util.Arrays;
import java.util.Objects;

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
import org.crazydan.studio.app.ime.kuaizi.internal.key.CharKey;
import org.crazydan.studio.app.ime.kuaizi.internal.key.CtrlKey;
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
    private final XZone[] inputting_zones = new XZone[3];

    private BlockKey zone_0_key;
    private BlockKey[] zone_1_keys;
    private BlockKey[][][] zone_2_keys;

    private XPadState state = new XPadState(XPadState.Type.Init);
    private BlockIndex active_block = null;
    private XZone active_label_zone = null;
    private BlockKey active_ctrl_block_key = null;

    private boolean reversed;
    private final float pad_padding;
    private final float ctrl_icon_size;
    private final float level_0_zone_CornerRadius;
    private final float level_1_zone_CornerRadius;
    /** 中心正六边形半径 */
    private float level_0_zone_HexagonRadius;
    private final float level_1_zone_HexagonRadius;
    /** 中心坐标 */
    private PointF centerCoord;

    public XPadView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        ViewUtils.enableHardwareAccelerated(this);

        this.pad_padding = dimen(R.dimen.x_keyboard_pad_padding);
        this.ctrl_icon_size = dimen(R.dimen.x_keyboard_ctrl_icon_size);
        this.level_0_zone_HexagonRadius = dimen(R.dimen.key_view_bg_min_radius);
        this.level_0_zone_CornerRadius = dimen(R.dimen.key_view_corner_radius);
        this.level_1_zone_HexagonRadius = this.level_0_zone_HexagonRadius / 0.45f;
        this.level_1_zone_CornerRadius = dimen(R.dimen.x_keyboard_ctrl_pad_corner_radius);

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

    public void setCenterHexagonRadius(float level_0_zone_HexagonRadius) {
        this.level_0_zone_HexagonRadius = level_0_zone_HexagonRadius;
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

        this.trailer.onGesture(type, ViewGestureDetector.GestureData.newFrom(data, x, y));

        this.active_block = findBlockAt(x, y);
        if (this.active_block == null || type == ViewGestureDetector.GestureType.PressEnd) {
            reset();
            return;
        }

        switch (this.active_block.zone) {
            case 0: {
                onGesture_Over_Zone_0(type, data, userKeyMsgListenerExecutor);
                break;
            }
            case 1: {
                onGesture_Over_Zone_1(type, data, userKeyMsgListenerExecutor);
                break;
            }
            case 2: {
                onGesture_Over_Zone_2(type, data, userKeyMsgListenerExecutor);
                break;
            }
        }
    }

    public void updateZoneKeys(Key<?> zone_0_key, Key<?>[] zone_1_keys, Key<?>[][][] zone_2_keys) {
        invalidate();

        this.zone_0_key = new BlockKey(0, 0, 0, 0, zone_0_key);

        this.zone_1_keys = new BlockKey[zone_1_keys.length];
        for (int i = 0; i < zone_1_keys.length; i++) {
            Key<?> key = zone_1_keys[i];
            this.zone_1_keys[i] = new BlockKey(1, i, 0, 0, key);

            if (key != null && key.isDisabled()) {
                this.active_ctrl_block_key = this.zone_1_keys[i];
            }
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
        XZone[] zones = determineZones();

        // 从外到内绘制，以层叠方式覆盖相交部分
        for (int i = zones.length - 1; i >= 0; i--) {
            XZone zone = zones[i];
            zone.draw(canvas, this.centerCoord);
        }

        this.active_label_zone.draw(canvas, this.centerCoord);
    }

    private void prepareContentOnZone(HexagonOrientation orientation) {
        float cos_30 = (float) Math.cos(Math.toRadians(30));
        float sin_30 = (float) Math.sin(Math.toRadians(30));

        XZone[] zones = determineZones();
        boolean isInputting = this.state.type != XPadState.Type.Init;

        for (XZone zone : zones) {
            zone.clearTextPainters();
            zone.clearIconPainters();
        }
        this.active_label_zone.clearTextPainters();
        this.active_label_zone.clearIconPainters();

        // ==============================================
        XZone level_0_zone = zones[0];

        if (!isInputting) {
            Drawable icon = drawable(this.zone_0_key.key.getIconResId());
            XDrawablePainter icon_painter = level_0_zone.newIconPainter(icon);
            icon_painter.setStart(this.centerCoord);
            icon_painter.setAlign(XPainter.Align.Center);
            icon_painter.setSize(this.level_0_zone_HexagonRadius);
        }

        // ==============================================
        XZone level_1_zone = zones[1];

        if (!isInputting) {
            for (int i = 0; i < level_1_zone.blocks.size(); i++) {
                BlockKey blockKey = getAt(this.zone_1_keys, i);
                if (BlockKey.isNull(blockKey)) {
                    continue;
                }

                XZone.PolygonBlock block = (XZone.PolygonBlock) level_1_zone.blocks.get(i);
                PointF center = block.links.center.center;
                float rotate = orientation == HexagonOrientation.POINTY_TOP //
                               ? 30 * (2 * i - 1) : 60 * (i - 1);

                Drawable icon = drawable(blockKey.key.getIconResId());
                XDrawablePainter icon_painter = level_1_zone.newIconPainter(icon);
                icon_painter.setStart(center);
                icon_painter.setAlign(XPainter.Align.Center);
                icon_painter.setRotate(rotate);
                icon_painter.setSize(this.ctrl_icon_size);
                icon_painter.setAlpha(blockKey.key.isDisabled() ? 0.4f : 1f);
            }
        } else if (!BlockKey.isNull(this.active_ctrl_block_key)) {
            BlockKey blockKey = this.active_ctrl_block_key;
            Drawable icon = drawable(blockKey.key.getIconResId());
            XDrawablePainter icon_painter = level_1_zone.newIconPainter(icon);
            icon_painter.setStart(this.centerCoord);
            icon_painter.setAlign(XPainter.Align.Center);
            icon_painter.setSize(this.ctrl_icon_size * 1.8f);
        }

        // ==============================================
        BlockKey level_2_zone_active_key = getActiveBlockKey_In_Zone_2();
        String level_2_zone_active_key_label = level_2_zone_active_key != null
                                               ? level_2_zone_active_key.key.getLabel()
                                               : null;
        if (level_2_zone_active_key_label == null) {
            this.active_label_zone.hide();
        } else {
            this.active_label_zone.show();

            XZone.PolygonBlock block = (XZone.PolygonBlock) this.active_label_zone.blocks.get(0);

            PointF start = middle(block.vertexes[0], block.vertexes[1]);

            float textSize = dimen(R.dimen.input_popup_key_text_size);
            int textColor = attrColor(R.attr.x_keyboard_chars_highlight_fg_color);
            start.offset(0, textSize);

            XTextPainter painter = this.active_label_zone.newTextPainter(level_2_zone_active_key_label);
            painter.setStart(start);
            painter.setAlign(XPainter.Align.TopMiddle);
            painter.setSize(textSize);
            painter.setFillColor(textColor);
            painter.enableBoldText();
        }

        // ==============================================
        XZone level_2_zone = zones[2];

        float textSize = dimen(R.dimen.x_keyboard_chars_text_size);
        float textPadding = dimen(R.dimen.x_keyboard_chars_text_padding);

        for (int i = 0; i < level_2_zone.blocks.size(); i++) {
            boolean isActiveBlock = isActiveBlock_In_Zone_2(i);
            int textColor = isActiveBlock && level_2_zone_active_key == null
                            ? attrColor(R.attr.x_keyboard_chars_highlight_fg_color)
                            : attrColor(R.attr.x_keyboard_chars_fg_color);
            XZone.PolygonBlock block = (XZone.PolygonBlock) level_2_zone.blocks.get(i);

            BlockKey[][] blockKeys = getAt(this.zone_2_keys, i);
            for (int j = 0; j < block.links.left.size(); j++) {
                BlockKey blockKey = blockKeys[this.reversed ? 1 : 0][j];
                if (BlockKey.isNull(blockKey)) {
                    continue;
                }

                XZone.Link link = block.links.left.get(j);
                PointF start = new PointF(link.vertexes.get(0).x, link.vertexes.get(0).y);

                float rotate = 0;
                XPainter.Align align;
                if (orientation == HexagonOrientation.POINTY_TOP) {
                    if (i == 1) {
                        start.offset(-textPadding, 0);
                        align = XPainter.Align.BottomLeft;
                    } else if (i == 4) {
                        start.offset(textPadding, 0);
                        align = XPainter.Align.TopRight;
                    } else if (i < 3) {
                        float dir = i == 0 ? -1 : 1;
                        rotate = dir * 60;
                        start.offset(-textPadding * sin_30, -dir * textPadding * cos_30);

                        align = XPainter.Align.BottomLeft;
                    } else {
                        float dir = i == 3 ? -1 : 1;
                        rotate = dir * 60;
                        start.offset(textPadding * sin_30, dir * textPadding * cos_30);

                        align = XPainter.Align.TopRight;
                    }
                } else {
                    if (i == 0) {
                        start.offset(0, textPadding);
                        align = XPainter.Align.BottomRight;
                    } else if (i == 3) {
                        start.offset(0, -textPadding);
                        align = XPainter.Align.TopLeft;
                    } else if (i < 3) {
                        float dir = i == 1 ? -1 : 1;
                        rotate = dir * 30;
                        start.offset(-textPadding * cos_30, -dir * textPadding * sin_30);

                        align = XPainter.Align.BottomLeft;
                    } else {
                        float dir = i == 4 ? -1 : 1;
                        rotate = dir * 30;
                        start.offset(textPadding * cos_30, dir * textPadding * sin_30);

                        align = XPainter.Align.TopRight;
                    }
                }

                float size;
                XAlignPainter painter;
                if (blockKey.key instanceof CtrlKey) {
                    Drawable icon = drawable(blockKey.key.getIconResId());
                    painter = level_2_zone.newIconPainter(icon);
                    size = this.ctrl_icon_size;
                } else {
                    painter = level_2_zone.newTextPainter(blockKey.key.getLabel());

                    float textSizeScale = 1f;
                    if (Objects.equals(blockKey, level_2_zone_active_key)) {
                        textColor = attrColor(R.attr.x_keyboard_chars_highlight_fg_color);
                        textSizeScale = 1.25f;
                    } else if (level_2_zone_active_key != null) {
                        textColor = attrColor(R.attr.x_keyboard_chars_fg_color);
                    }
                    size = textSize * textSizeScale;

                    painter.setFillColor(textColor);
                }

                painter.setStart(start);
                painter.setAlign(align);
                painter.setRotate(rotate);
                painter.setSize(size);
            }

            for (int j = 0; j < block.links.right.size(); j++) {
                BlockKey blockKey = blockKeys[this.reversed ? 0 : 1][j];
                if (BlockKey.isNull(blockKey)) {
                    continue;
                }

                XZone.Link link = block.links.right.get(j);
                PointF start = new PointF(link.vertexes.get(0).x, link.vertexes.get(0).y);

                float rotate = 0;
                XPainter.Align align;
                if (orientation == HexagonOrientation.POINTY_TOP) {
                    if (i == 0) {
                        start.offset(textPadding, 0);
                        align = XPainter.Align.BottomRight;
                    } else if (i == 3) {
                        start.offset(-textPadding, 0);
                        align = XPainter.Align.TopLeft;
                    } else if (i < 3) {
                        float dir = i == 1 ? 1 : -1;
                        rotate = dir * 60;
                        start.offset(dir * textPadding * sin_30, textPadding * cos_30);

                        align = dir > 0 ? XPainter.Align.BottomRight : XPainter.Align.TopLeft;
                    } else {
                        float dir = i == 4 ? 1 : -1;
                        rotate = dir * 60;
                        start.offset(-dir * textPadding * sin_30, -textPadding * cos_30);

                        align = dir > 0 ? XPainter.Align.TopLeft : XPainter.Align.BottomRight;
                    }
                } else {
                    if (i == 2) {
                        start.offset(0, textPadding);
                        align = XPainter.Align.BottomLeft;
                    } else if (i == 5) {
                        start.offset(0, -textPadding);
                        align = XPainter.Align.TopRight;
                    } else if (i < 2) {
                        float dir = i == 0 ? -1 : 1;
                        rotate = dir * 30;
                        start.offset(textPadding * cos_30, dir * textPadding * sin_30);

                        align = XPainter.Align.BottomRight;
                    } else {
                        float dir = i == 3 ? -1 : 1;
                        rotate = dir * 30;
                        start.offset(-textPadding * cos_30, -dir * textPadding * sin_30);

                        align = XPainter.Align.TopLeft;
                    }
                }

                float size;
                XAlignPainter painter;
                if (blockKey.key instanceof CtrlKey) {
                    Drawable icon = drawable(blockKey.key.getIconResId());
                    painter = level_2_zone.newIconPainter(icon);
                    size = this.ctrl_icon_size;
                } else {
                    painter = level_2_zone.newTextPainter(blockKey.key.getLabel());

                    float textSizeScale = 1f;
                    if (Objects.equals(blockKey, level_2_zone_active_key)) {
                        textColor = attrColor(R.attr.x_keyboard_chars_highlight_fg_color);
                        textSizeScale = 1.25f;
                    } else if (level_2_zone_active_key != null) {
                        textColor = attrColor(R.attr.x_keyboard_chars_fg_color);
                    }
                    size = textSize * textSizeScale;

                    painter.setFillColor(textColor);
                }

                painter.setStart(start);
                painter.setAlign(align);
                painter.setRotate(rotate);
                painter.setSize(size);
            }
        }
    }

    private void prepareZones(HexagonOrientation orientation, int width, int height) {
        float cos_30 = (float) Math.cos(Math.toRadians(30));
        float cos_30_divided_by_1 = 1f / cos_30;

        float padPadding = this.pad_padding;
        float level_0_zone_HexagonRadius = this.level_0_zone_HexagonRadius;
        float level_1_zone_HexagonRadius = this.level_1_zone_HexagonRadius;

        float maxHexagonRadius = Math.min(width * 0.5f, height * 0.5f) - padPadding;
        float level_2_zone_HexagonRadius = orientation == HexagonOrientation.FLAT_TOP
                                           ? maxHexagonRadius
                                             * cos_30_divided_by_1
                                           : maxHexagonRadius;

        PointF origin = orientation == HexagonOrientation.FLAT_TOP
                        ? new PointF(width - (level_2_zone_HexagonRadius
                                              + padPadding),
                                     height - maxHexagonRadius - padPadding)
                        : new PointF(width - (level_2_zone_HexagonRadius * cos_30 + padPadding),
                                     height - level_2_zone_HexagonRadius - padPadding);
        this.centerCoord = origin;

        // ==================================================
        // 激活标签分区
        this.active_label_zone = new XZone();
        {
            float size = padPadding * 8;
            PointF start = new PointF(origin.x - size, 0);
            PointF end = new PointF(origin.x + size, 0);
            if (orientation == HexagonOrientation.POINTY_TOP) {
                start = new PointF(padPadding, 0);
                end = new PointF(origin.x, 0);
            }

            XZone.PolygonBlock block = new XZone.PolygonBlock(start, end);
            this.active_label_zone.blocks.add(block);
        }

        float level_1_zone_scale = 1.25f;
        XZone[] zones = createZones(origin,
                                    orientation,
                                    width,
                                    height,
                                    level_0_zone_HexagonRadius,
                                    level_1_zone_HexagonRadius * level_1_zone_scale,
                                    level_2_zone_HexagonRadius,
                                    level_1_zone_HexagonRadius * (1 - level_1_zone_scale),
                                    cos_30_divided_by_1);
        System.arraycopy(zones, 0, this.zones, 0, zones.length);

        level_1_zone_scale = 0.75f;
        zones = createZones(origin,
                            orientation,
                            width,
                            height,
                            level_0_zone_HexagonRadius,
                            level_1_zone_HexagonRadius * level_1_zone_scale,
                            level_2_zone_HexagonRadius,
                            level_1_zone_HexagonRadius * (1 - level_1_zone_scale),
                            cos_30_divided_by_1);
        System.arraycopy(zones, 0, this.inputting_zones, 0, zones.length);

        this.inputting_zones[0] = new XZone();
        // 去掉分隔线
        this.inputting_zones[1].dropPathPainter(1);
        // 共用相同部分
        this.inputting_zones[2].blocks.clear();
        this.inputting_zones[2].blocks.addAll(this.zones[2].blocks);
    }

    private XZone[] createZones(
            PointF origin, HexagonOrientation orientation, int width, int height, //
            float level_0_zone_HexagonRadius, float level_1_zone_HexagonRadius, float level_2_zone_HexagonRadius, //
            float level_1_and_2_zone_spacing, float cos_30_divided_by_1
    ) {
        XZone[] zones = new XZone[3];
        // ==================================================
        // 第 0 级分区：中心圆
        XZone level_0_zone = zones[0] = new XZone();

        int level_0_zone_bg_color = attrColor(R.attr.key_ctrl_locator_bg_color);
        String level_0_zone_shadow = attrStr(R.attr.key_shadow_style);

        XPathPainter level_0_zone_fill_painter = level_0_zone.newPathPainter();
        level_0_zone_fill_painter.setFillShadow(level_0_zone_shadow);
        level_0_zone_fill_painter.setFillColor(level_0_zone_bg_color);
        level_0_zone_fill_painter.setCornerRadius(this.level_0_zone_CornerRadius);

        PointF[] centerHexagonVertexes = ViewUtils.drawHexagon(level_0_zone_fill_painter.path,
                                                               orientation,
                                                               origin,
                                                               level_0_zone_HexagonRadius);
        level_0_zone.blocks.add(new XZone.PolygonBlock(centerHexagonVertexes));

        // ==================================================
        // 第 1 级分区：内六边形
        XZone level_1_zone = zones[1] = new XZone();

        int level_1_zone_bg_color = attrColor(R.attr.x_keyboard_ctrl_bg_style);
        String level_1_zone_divider_style = attrStr(R.attr.x_keyboard_ctrl_divider_style);
        String level_1_zone_shadow_style = attrStr(R.attr.x_keyboard_shadow_style);

        XPathPainter level_1_zone_fill_painter = level_1_zone.newPathPainter();
        level_1_zone_fill_painter.setFillColor(level_1_zone_bg_color);
        level_1_zone_fill_painter.setFillShadow(level_1_zone_shadow_style);
        level_1_zone_fill_painter.setCornerRadius(this.level_1_zone_CornerRadius);

        float innerHexagonAxisRadius = level_0_zone_HexagonRadius
                                       + (level_1_zone_HexagonRadius - level_0_zone_HexagonRadius) * 0.25f;
        PointF[] innerHexagonVertexes = ViewUtils.drawHexagon(level_1_zone_fill_painter.path,
                                                              orientation,
                                                              origin,
                                                              level_1_zone_HexagonRadius);
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
        float innerHexagonCornerActualRadius = this.level_1_zone_CornerRadius * 1.6f;
        float innerHexagonCropRadius = level_1_zone_HexagonRadius - innerHexagonCornerActualRadius * (
                cos_30_divided_by_1
                - 1);
        float innerHexagonCrossRadius = level_0_zone_HexagonRadius * 0.9f; // 确保中心按下后依然能够显示分隔线
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
//                                                    level_1_zone_HexagonRadius
//                                                    - innerHexagonCornerActualRadius * cos_30_divided_by_1);
//        for (PointF start : vertexes) {
//            level_1_zone_stroke_painter.path.addCircle(start.x,
//                                                       start.y,
//                                                       innerHexagonCornerActualRadius,
//                                                       Path.Direction.CW);
//        }

        // ==================================================
        // 第 2 级分区：外六边形，不封边，且射线范围内均为其分区空间
        XZone level_2_zone = zones[2] = new XZone();

        String level_2_zone_divider_style = attrStr(R.attr.x_keyboard_chars_divider_style);
        String level_2_zone_shadow_style = attrStr(R.attr.x_keyboard_chars_divider_shadow_style);

        XPathPainter level_2_zone_stroke_painter = level_2_zone.newPathPainter();
        level_2_zone_stroke_painter.setStrokeCap(Paint.Cap.ROUND); // 设置端点为圆形
        level_2_zone_stroke_painter.setStrokeStyle(level_2_zone_divider_style);
        level_2_zone_stroke_painter.setStrokeShadow(level_2_zone_shadow_style);

        PointF[] outerHexagonVertexes = ViewUtils.createHexagon(orientation, origin, level_2_zone_HexagonRadius);
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
        float spacing_between_level_1_and_2_zone = level_1_zone_HexagonRadius + level_1_and_2_zone_spacing;
        float outerHexagonAxisSpacing = (level_2_zone_HexagonRadius - spacing_between_level_1_and_2_zone)
                                        / level_2_zone_axis_link_count;

        PointF[][] axisHexagonVertexesArray = new PointF[level_2_zone_axis_link_count][];
        for (int i = 0; i < level_2_zone_axis_link_count; i++) {
            float axisHexagonRadius = (spacing_between_level_1_and_2_zone + outerHexagonAxisSpacing * (i + 1)) //
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

                // 左侧从轴线相交点逆时针旋转
                leftLink.addVertexes(middle(innerCurrent, innerBefore),
                                     innerCurrent,
                                     outerCurrent,
                                     middle(outerCurrent, outerBefore));

                XZone.Link rightLink = new XZone.Link();
                block.links.right.add(rightLink);

                // 右侧从轴线相交点顺时针旋转
                rightLink.addVertexes(middle(innerCurrent, innerAfter),
                                      innerCurrent,
                                      outerCurrent,
                                      middle(outerCurrent, outerAfter));
            }
        }

        return zones;
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

    private String attrStr(int resId) {
        return ThemeUtils.getStringByAttrId(getContext(), resId);
    }

    private <T> T getAt(T[] keys, int index) {
        int size = keys.length;
        int idx = this.reversed ? (size + (size / 2 - 1) - index) % size : index;

        return keys[idx];
    }

    private void reset() {
        this.state = new XPadState(XPadState.Type.Init);

        XZone[] zones = determineZones();
        zones[0].bounce();
    }

    private BlockKey getBlockKey(BlockIndex blockIndex) {
        return getBlockKey(blockIndex, 0);
    }

    private BlockKey getBlockKey(BlockIndex blockIndex, int blockIndexDiff) {
        if (blockIndex == null) {
            return null;
        }

        switch (blockIndex.zone) {
            case 0: {
                return this.zone_0_key;
            }
            case 1: {
                return getAt(this.zone_1_keys, blockIndex.block);
            }
            case 2: {
                if (blockIndexDiff == 0) {
                    return null;
                }

                BlockKey[][] blockKeysArray = getAt(this.zone_2_keys, blockIndex.block);
                int blockKeysIndex = blockIndexDiff > 0 //
                                     ? (this.reversed ? 0 : 1) //
                                     : (this.reversed ? 1 : 0);
                BlockKey[] blockKeys = Arrays.stream(blockKeysArray[blockKeysIndex])
                                             .filter((bk) -> !BlockKey.isNull(bk))
                                             .toArray(BlockKey[]::new);
                int totalBlockKeys = blockKeys.length;
                if (totalBlockKeys == 0) {
                    return null;
                }

                int blockKeyAbsIndex = Math.abs(blockIndexDiff) - 1;
                int blockKeyIndex = blockKeyAbsIndex % totalBlockKeys;
                BlockKey blockKey = blockKeys[blockKeyIndex];

                if (blockKey.key instanceof CharKey && ((CharKey) blockKey.key).hasReplacement()) {
                    int newKeyIndex = blockKeyAbsIndex / totalBlockKeys;
                    Key<?> newKey = ((CharKey) blockKey.key).createReplacementKey(newKeyIndex);

                    blockKey = new BlockKey(blockKey, newKey);
                }

                return blockKey;
            }
        }
        return null;
    }

    private BlockKey getActiveBlockKey_In_Zone_2() {
        if (this.state.type != XPadState.Type.InputChars_Input_Doing) {
            return null;
        }

        XPadState.BlockData stateData = (XPadState.BlockData) this.state.data;
        BlockKey blockKey = getBlockKey(new BlockIndex(2, stateData.getStartBlock()), stateData.getBlockDiff());

        return BlockKey.isNull(blockKey) ? null : blockKey;
    }

    private boolean isActiveBlock_In_Zone_2(int index) {
        return this.state.type == XPadState.Type.InputChars_Input_Doing
               && ((XPadState.BlockData) this.state.data).getStartBlock() == index;
    }

    private BlockIndex findBlockAt(float x, float y) {
        XZone[] zones = determineZones();

        for (int i = 0; i < zones.length; i++) {
            XZone zone = zones[i];

            for (int j = 0; j < zone.blocks.size(); j++) {
                XZone.Block block = zone.blocks.get(j);

                if (block.contains(new PointF(x, y))) {
                    return new BlockIndex(i, j);
                }
            }
        }
        return null;
    }

    private XZone[] determineZones() {
        if (this.state.type == XPadState.Type.Init) {
            return this.zones;
        }
        return this.inputting_zones;
    }

    private void onGesture_Over_Zone_0(
            ViewGestureDetector.GestureType type, ViewGestureDetector.GestureData data,
            UserKeyMsgListener.Executor userKeyMsgListenerExecutor
    ) {
        if (this.state.type != XPadState.Type.Init) {
            return;
        }

        XZone[] zones = determineZones();
        BlockKey blockKey = getBlockKey(this.active_block);
        XZone centerZone = zones[0];

        switch (type) {
            case PressStart: {
                centerZone.press();
                break;
            }
            case Flipping: {
                execute_UserKeyMsgListener(userKeyMsgListenerExecutor, blockKey, type, data);
                break;
            }
            case LongPressStart: {
                execute_UserKeyMsgListener(userKeyMsgListenerExecutor, blockKey, type, data);

                // Note：进入编辑器编辑状态会发生键盘切换，故而，需显式重置状态
                reset();
                break;
            }
        }
    }

    private void onGesture_Over_Zone_1(
            ViewGestureDetector.GestureType type, ViewGestureDetector.GestureData data,
            UserKeyMsgListener.Executor userKeyMsgListenerExecutor
    ) {
        switch (type) {
            case MovingStart: {
                this.state = new XPadState(XPadState.Type.InputChars_Input_Waiting);

                BlockKey blockKey = getBlockKey(this.active_block);
                if (BlockKey.isNull(blockKey)) {
                    return;
                }

                // 发送点击事件以触发通用的子键盘切换
                ViewGestureDetector.SingleTapGestureData tapData = new ViewGestureDetector.SingleTapGestureData(data,
                                                                                                                0);
                execute_UserKeyMsgListener(userKeyMsgListenerExecutor,
                                           blockKey,
                                           ViewGestureDetector.GestureType.SingleTap,
                                           tapData);
                break;
            }
            case Moving: {
                if (this.state.type != XPadState.Type.InputChars_Input_Doing) {
                    return;
                }

                // 再次进入第 2 分区，则表示当前输入已确定
                BlockKey blockKey = getActiveBlockKey_In_Zone_2();

                XPadState.BlockData stateData = (XPadState.BlockData) this.state.data;
                stateData.reset();

                if (blockKey == null) {
                    return;
                }

                ViewGestureDetector.SingleTapGestureData tapData = new ViewGestureDetector.SingleTapGestureData(data,
                                                                                                                0);
                execute_UserKeyMsgListener(userKeyMsgListenerExecutor,
                                           blockKey,
                                           ViewGestureDetector.GestureType.SingleTap,
                                           tapData);
                break;
            }
        }
    }

    private void onGesture_Over_Zone_2(
            ViewGestureDetector.GestureType type, ViewGestureDetector.GestureData data,
            UserKeyMsgListener.Executor userKeyMsgListenerExecutor
    ) {
        if (type != ViewGestureDetector.GestureType.Moving) {
            return;
        }

        XZone[] zones = determineZones();
        int blockIndex = this.active_block.block;

        switch (this.state.type) {
            case InputChars_Input_Waiting: {
                XZone level_2_zone = zones[2];
                XPadState.BlockData stateData = new XPadState.BlockData(level_2_zone.blocks.size());
                this.state = new XPadState(XPadState.Type.InputChars_Input_Doing, stateData);

                stateData.updateCurrentBlock(blockIndex);
                break;
            }
            case InputChars_Input_Doing: {
                ((XPadState.BlockData) this.state.data).updateCurrentBlock(blockIndex);
                break;
            }
        }
    }

    private void execute_UserKeyMsgListener(
            UserKeyMsgListener.Executor userKeyMsgListenerExecutor, BlockKey blockKey,
            ViewGestureDetector.GestureType type, ViewGestureDetector.GestureData data
    ) {
        // Note：向外部传递的 GestureData 不需要做坐标转换
        userKeyMsgListenerExecutor.onGesture(blockKey != null ? blockKey.key : null, type, data);
    }

    private static class BlockIndex {
        public final int zone;
        public final int block;

        private BlockIndex(int zone, int block) {
            this.zone = zone;
            this.block = block;
        }
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

        public BlockKey(BlockKey other, Key<?> key) {
            this.zone = other.zone;
            this.x = other.x;
            this.y = other.y;
            this.z = other.z;
            this.key = key;
        }

        public static boolean isNull(BlockKey blockKey) {
            return blockKey == null || blockKey.key == null;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            BlockKey that = (BlockKey) o;
            return this.zone == that.zone //
                   && this.x == that.x //
                   && this.y == that.y //
                   && this.z == that.z //
                   && Objects.equals(this.key, that.key);
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.zone, this.x, this.y, this.z, this.key);
        }
    }
}