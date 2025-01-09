/*
 * 筷字输入法 - 高效编辑需要又好又快的输入法
 * Copyright (C) 2025 Crazydan Studio <https://studio.crazydan.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.
 * If not, see <https://www.gnu.org/licenses/lgpl-3.0.en.html#license-text>.
 */

package org.crazydan.studio.app.ime.kuaizi.ui.view.xpad;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.drawable.Drawable;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import org.crazydan.studio.app.ime.kuaizi.R;
import org.crazydan.studio.app.ime.kuaizi.common.utils.CollectionUtils;
import org.crazydan.studio.app.ime.kuaizi.common.utils.ScreenUtils;
import org.crazydan.studio.app.ime.kuaizi.common.utils.ThemeUtils;
import org.crazydan.studio.app.ime.kuaizi.common.utils.ViewUtils;
import org.crazydan.studio.app.ime.kuaizi.common.widget.ViewGestureDetector;
import org.crazydan.studio.app.ime.kuaizi.common.widget.ViewGestureTrailer;
import org.crazydan.studio.app.ime.kuaizi.core.Key;
import org.crazydan.studio.app.ime.kuaizi.core.key.CharKey;
import org.crazydan.studio.app.ime.kuaizi.core.key.CtrlKey;
import org.crazydan.studio.app.ime.kuaizi.core.msg.Motion;
import org.crazydan.studio.app.ime.kuaizi.ui.view.KeyboardView;
import org.hexworks.mixite.core.api.HexagonOrientation;

import static org.crazydan.studio.app.ime.kuaizi.common.Constants.cos_30;
import static org.crazydan.studio.app.ime.kuaizi.common.Constants.cos_30_divided_by_1;
import static org.crazydan.studio.app.ime.kuaizi.common.Constants.sin_30;

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
    private final ValueAnimator.AnimatorUpdateListener zone_1_animator_update_listener
            = new Zone1AnimatorUpdateListener();
    private final float pad_padding;
    private final float ctrl_icon_size;
    private final float zone_0_CornerRadius;
    private final float zone_1_CornerRadius;
    private final float zone_1_HexagonRadius;
    private final float zone_1_HexagonRadius_input_waiting;
    private final float zone_1_HexagonRadius_input_doing;
    private ValueAnimator zone_1_animator;
    private BlockKey zone_0_key;
    private BlockKey[] zone_1_keys;
    private BlockKey[][][] zone_2_keys;
    private XPadState state = new XPadState(XPadState.Type.Init);
    private BlockIndex active_block = null;
    private XZone active_label_zone = null;
    private BlockKey active_ctrl_block_key = null;
    private boolean reversed;
    private boolean simulating;
    /** 中心正六边形半径 */
    private float zone_0_HexagonRadius;
    /** 中心坐标 */
    private PointF center_coordinate;

    public XPadView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        ViewUtils.enableHardwareAccelerated(this);

        this.pad_padding = dimen(R.dimen.x_keyboard_pad_padding);
        this.ctrl_icon_size = dimen(R.dimen.x_keyboard_ctrl_icon_size);
        this.zone_0_HexagonRadius = dimen(R.dimen.key_view_bg_min_radius);
        this.zone_0_CornerRadius = dimen(R.dimen.key_view_corner_radius);
        this.zone_1_CornerRadius = dimen(R.dimen.x_keyboard_ctrl_pad_corner_radius);

        this.zone_1_HexagonRadius = this.zone_0_HexagonRadius / 0.35f;
        this.zone_1_HexagonRadius_input_waiting = this.zone_0_HexagonRadius / 0.45f;
        this.zone_1_HexagonRadius_input_doing = this.zone_0_HexagonRadius / 0.60f;

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
        this.zone_0_HexagonRadius = level_0_zone_HexagonRadius;
    }

    private void reset() {
        stop_zone_1_animator();

        this.state = new XPadState(XPadState.Type.Init);
        this.active_block = null;

        XZone[] zones = determineZones();
        zones[0].bounce();
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

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent e) {
        // Note：由上层视图处理事件，并将属于本视图的事件通过 #onGesture 转发到本视图内处理
        return true;
    }

    @Override
    protected void onDraw(Canvas canvas) {
//        Log.d("onDraw", "Doing ...");

        // 整体的镜像翻转：不适用，应该调换左右位置即可，文字和图片等不能翻转
        //canvas.scale(-1, 1, getWidth() * 0.5f, getHeight() * 0.5f);
        // 将画布整体向左偏移，按键放置位置左右翻转，从而实现键盘的翻转
        if (this.reversed) {
            float dx = getWidth() - this.center_coordinate.x * 2;
            canvas.translate(dx, 0);
        }

        // 准备分区的待显示内容
        prepareContentOnZone(this.orientation);
        // 绘制分区
        drawZones(canvas);

        // 绘制滑屏轨迹
        this.trailer.draw(canvas);

//        Log.d("onDraw",
//              String.format("Done: %s",
//                            this.active_ctrl_block_key != null
//                            ? ((CtrlKey) this.active_ctrl_block_key.key).getOption()
//                                                                        .value()
//                            : null));
    }

    public void onGesture(
            KeyboardView.GestureListener listener, ViewGestureDetector.GestureType type,
            ViewGestureDetector.GestureData data, PointF offset, boolean disableTrailer
    ) {
        // 忽略长按的 tick 事件，目前没有对该事件的处理需求
        if (type == ViewGestureDetector.GestureType.LongPressTick) {
            return;
        }

        boolean simulationTerminated = false;
        if (this.simulating) {
            if (listener != null) {
                if (type == ViewGestureDetector.GestureType.PressEnd) {
                    // 连续输入的模拟过程中，手指已离开屏幕，
                    // 则终止模拟，并继续后续逻辑以复原键盘
                    this.simulating = false;
                    simulationTerminated = true;
                } else {
                    // 正在模拟演示中，则不响应外部事件
                    return;
                }
            }
        } else if (listener == null) {
            // 演示模拟已提前终止
            return;
        }

        invalidate();

        float x = data.x + offset.x;
        float y = data.y + offset.y;

        this.trailer.setDisabled(disableTrailer);
        this.trailer.onGesture(type, ViewGestureDetector.GestureData.newFrom(data, x, y));

        BlockIndex old_active_block = this.active_block;
        BlockIndex new_active_block = this.active_block = findBlockAt(x, y);

//        if (old_active_block != null && new_active_block != null //
//            && (old_active_block.zone != new_active_block.zone //
//                || old_active_block.block != new_active_block.block)) {
//            Log.d("GestureOnXPadView",
//                  String.format("%s: simulation - %s:%s, x/y - %f/%f, old - %d:%d, new - %d:%d",
//                                type,
//                                this.simulating,
//                                trigger,
//                                x,
//                                y,
//                                old_active_block.zone,
//                                old_active_block.block,
//                                new_active_block.zone,
//                                new_active_block.block));
//        }

        if (new_active_block == null || type == ViewGestureDetector.GestureType.PressEnd) {
            if (this.state.type != XPadState.Type.Init && this.zone_1_animator != null) {
                boolean finalSimulationTerminated = simulationTerminated;

                // 输入状态结束，动画还原到初始状态
                start_zone_1_animator(0, this.zone_1_HexagonRadius, new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        onGesture_End(listener, data, finalSimulationTerminated);
                        invalidate();
                    }
                });
                return;
            }

            onGesture_End(listener, data, simulationTerminated);
            return;
        }

        if (old_active_block != null) {
            // 开始输入：减少手指画圈的半径和滑行距离
            if (old_active_block.zone == 1 && new_active_block.zone == 2) {
                start_zone_1_animator(this.zone_1_HexagonRadius_input_waiting, this.zone_1_HexagonRadius_input_doing);
            }
        }

        switch (new_active_block.zone) {
            case 0: {
                onGesture_Over_Zone_0(listener, type, data);
                break;
            }
            case 1: {
                onGesture_Over_Zone_1(listener, type, data);
                break;
            }
            case 2: {
                onGesture_Over_Zone_2(listener, type, data);
                break;
            }
        }

        // 触发滑行过程中的切换消息
        if (old_active_block != null //
            && type == ViewGestureDetector.GestureType.Moving) {
            boolean isInputting = this.state.type == XPadState.Type.InputChars_Input_Doing;
            boolean isBlockChanged = old_active_block.zone == 1 && new_active_block.zone == 2;
            boolean isCharChanged = old_active_block.zone == 2
                                    && new_active_block.zone == 2
                                    && old_active_block.block != new_active_block.block;

            // 告知处于输入字符待确认状态
            if (isInputting //
                && (isBlockChanged //
                    // 回到输入字符待确认区域
                    || (((XPadState.BlockData) this.state.data).getBlockDiff() == 0 //
                        && isCharChanged) //
                ) //
            ) {
                BlockKey blockKey = getBlockKey(new_active_block, 1);
                if (BlockKey.isNull(blockKey)) {
                    blockKey = getBlockKey(new_active_block, -1);
                }

                // 在左右任意轴线上有字符，均可触发提示音效
                if (!BlockKey.isNull(blockKey)) {
                    trigger_Moving_Gesture(listener, CtrlKey.build(CtrlKey.Type.XPad_Active_Block), data);
                }
            }
            // 告知待输入字符发生了切换
            else if (isCharChanged) {
                BlockKey blockKey = getActiveBlockKey_In_Zone_2();
                if (!BlockKey.isNull(blockKey)) {
                    trigger_Moving_Gesture(listener, CtrlKey.build(CtrlKey.Type.XPad_Char_Key), data);
                }
            }
        }
    }

    public void updateZoneKeys(Key zone_0_key, Key[] zone_1_keys, Key[][][] zone_2_keys) {
//        Log.d("updateZoneKeys", "Doing ...");
        invalidate();

        this.zone_0_key = new BlockKey(0, 0, 0, 0, zone_0_key);

        this.zone_1_keys = new BlockKey[zone_1_keys.length];
        for (int i = 0; i < zone_1_keys.length; i++) {
            Key key = zone_1_keys[i];
            this.zone_1_keys[i] = new BlockKey(1, i, 0, 0, key);

            if (key != null && key.disabled) {
                this.active_ctrl_block_key = this.zone_1_keys[i];
            }
        }

        this.zone_2_keys = new BlockKey[zone_2_keys.length][][];
        for (int i = 0; i < zone_2_keys.length; i++) {
            Key[][] keys = zone_2_keys[i];

            this.zone_2_keys[i] = new BlockKey[keys.length][];
            for (int j = 0; j < keys.length; j++) {
                Key[] array = keys[j];

                this.zone_2_keys[i][j] = new BlockKey[array.length];
                for (int k = 0; k < array.length; k++) {
                    Key key = array[k];
                    this.zone_2_keys[i][j][k] = new BlockKey(2, i, j, k, key);
                }
            }
        }

//        Log.d("updateZoneKeys",
//              String.format("Got active key: %s",
//                            this.active_ctrl_block_key != null
//                            ? ((CtrlKey) this.active_ctrl_block_key.key).getOption()
//                                                                        .value()
//                            : null));
//        try {
//            throw new RuntimeException();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        Log.d("updateZoneKeys", "Done ...");
    }

    private void drawZones(Canvas canvas) {
        XZone[] zones = determineZones();

        // 从外到内绘制，以层叠方式覆盖相交部分
        for (int i = zones.length - 1; i >= 0; i--) {
            XZone zone = zones[i];
            zone.draw(canvas, this.center_coordinate);
        }

        this.active_label_zone.draw(canvas, this.center_coordinate);
    }

    private void prepareContentOnZone(HexagonOrientation orientation) {
        XZone[] zones = determineZones();
        boolean isInputting = this.state.type != XPadState.Type.Init;

        for (XZone zone : zones) {
            zone.clearTextPainters();
            zone.clearIconPainters();
        }
        this.active_label_zone.clearTextPainters();
        this.active_label_zone.clearIconPainters();

        // ==============================================
        XZone zone_0 = zones[0];

        if (!isInputting) {
            Drawable icon = drawable(this.zone_0_key.key.icon);
            XDrawablePainter icon_painter = zone_0.newIconPainter(icon);
            icon_painter.setStart(this.center_coordinate);
            icon_painter.setAlign(XPainter.Align.Center);
            icon_painter.setSize(this.zone_0_HexagonRadius);
        }

        // ==============================================
        XZone zone_1 = zones[1];

        if (!isInputting) {
            for (int i = 0; i < zone_1.blocks.size(); i++) {
                BlockKey blockKey = getAt(this.zone_1_keys, i);
                if (BlockKey.isNull(blockKey)) {
                    continue;
                }

                XZone.PolygonBlock block = (XZone.PolygonBlock) zone_1.blocks.get(i);
                PointF center = block.links.center.center;
                float rotate = orientation == HexagonOrientation.POINTY_TOP //
                               ? 30 * (2 * i - 1) : 60 * (i - 1);

                Drawable icon = drawable(blockKey.key.icon);
                XDrawablePainter icon_painter = zone_1.newIconPainter(icon);
                icon_painter.setStart(center);
                icon_painter.setAlign(XPainter.Align.Center);
                icon_painter.setRotate(rotate);
                icon_painter.setSize(this.ctrl_icon_size);
                icon_painter.setAlpha(blockKey.key.disabled ? 0.4f : 1f);
            }
        } else if (!BlockKey.isNull(this.active_ctrl_block_key)) {
            BlockKey blockKey = this.active_ctrl_block_key;
            Drawable icon = drawable(blockKey.key.icon);
            XDrawablePainter icon_painter = zone_1.newIconPainter(icon);
            icon_painter.setStart(this.center_coordinate);
            icon_painter.setAlign(XPainter.Align.Center);
            icon_painter.setSize(this.ctrl_icon_size * 1.8f);
        }

        // ==============================================
        BlockKey zone_2_active_key = getActiveBlockKey_In_Zone_2();
        String zone_2_active_key_label = zone_2_active_key != null ? zone_2_active_key.key.label : null;
        if (zone_2_active_key_label == null) {
            this.active_label_zone.hide();
        } else {
            this.active_label_zone.show();

            XZone.PolygonBlock block = (XZone.PolygonBlock) this.active_label_zone.blocks.get(0);

            PointF start = middle(block.vertexes[0], block.vertexes[1]);

            float textSize = dimen(R.dimen.input_popup_key_text_size);
            float textSizeScale = zone_2_active_key.key instanceof CtrlKey ? 0.8f : 1f;
            int textColor = attrColor(R.attr.x_keyboard_chars_highlight_fg_color);
            start.offset(0, textSize);

            XTextPainter painter = this.active_label_zone.newTextPainter(zone_2_active_key_label);
            painter.setStart(start);
            painter.setAlign(XPainter.Align.TopMiddle);
            painter.setSize(textSize * textSizeScale);
            painter.setFillColor(textColor);
            painter.enableBoldText();
        }

        // ==============================================
        XZone zone_2 = zones[2];

        float textSize = dimen(R.dimen.x_keyboard_chars_text_size);
        float textPadding = dimen(R.dimen.x_keyboard_chars_text_padding);

        for (int i = 0; i < zone_2.blocks.size(); i++) {
            boolean isActiveBlock = isActiveBlock_In_Zone_2(i);
            int defaultTextColor = isActiveBlock && zone_2_active_key == null
                                   ? attrColor(R.attr.x_keyboard_chars_highlight_fg_color)
                                   : attrColor(R.attr.x_keyboard_chars_fg_color);
            XZone.PolygonBlock block = (XZone.PolygonBlock) zone_2.blocks.get(i);

            BlockKey[][] blockKeys = getBlockKeys_In_Zone_2(i, isActiveBlock);
            for (int j = 0; j < block.links.left.size(); j++) {
                BlockKey blockKey = getAt(blockKeys, 0)[j];
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
                    Drawable icon = drawable(blockKey.key.icon);
                    painter = zone_2.newIconPainter(icon);
                    size = this.ctrl_icon_size;
                } else {
                    painter = zone_2.newTextPainter(blockKey.key.label);

                    float textSizeScale = 1f;
                    int textColor = defaultTextColor;
                    if (Objects.equals(blockKey, zone_2_active_key)) {
                        textColor = attrColor(R.attr.x_keyboard_chars_highlight_fg_color);
                        textSizeScale = 1.25f;
                    } else if (zone_2_active_key != null) {
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
                BlockKey blockKey = getAt(blockKeys, 1)[j];
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
                    Drawable icon = drawable(blockKey.key.icon);
                    painter = zone_2.newIconPainter(icon);
                    size = this.ctrl_icon_size;
                } else {
                    painter = zone_2.newTextPainter(blockKey.key.label);

                    float textSizeScale = 1f;
                    int textColor = defaultTextColor;
                    if (Objects.equals(blockKey, zone_2_active_key)) {
                        textColor = attrColor(R.attr.x_keyboard_chars_highlight_fg_color);
                        textSizeScale = 1.25f;
                    } else if (zone_2_active_key != null) {
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
        float padPadding = this.pad_padding;

        float maxHexagonRadius = Math.min(width * 0.5f, height * 0.5f) - padPadding;
        float zone_2_HexagonRadius = orientation == HexagonOrientation.FLAT_TOP
                                     ? maxHexagonRadius * cos_30_divided_by_1
                                     : maxHexagonRadius;

        PointF origin = orientation == HexagonOrientation.FLAT_TOP
                        ? new PointF(width - (zone_2_HexagonRadius
                                              + padPadding),
                                     height - maxHexagonRadius - padPadding)
                        : new PointF(width - (zone_2_HexagonRadius * cos_30 + padPadding),
                                     height - zone_2_HexagonRadius - padPadding);
        this.center_coordinate = origin;

        // ==================================================
        // 绘制激活标签分区
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

        // ====================================================
        // 绘制分区
        float zone_0_HexagonRadius = this.zone_0_HexagonRadius;
        float zone_1_HexagonRadius = this.zone_1_HexagonRadius;
        float zone_2_bound_HexagonRadius = Math.max(width, height);

        this.zones[0] = create_zone_0(origin, orientation, zone_0_HexagonRadius);
        this.zones[1] = create_zone_1(origin, orientation, zone_0_HexagonRadius, zone_1_HexagonRadius, false);
        this.zones[2] = create_zone_2(origin,
                                      orientation,
                                      zone_2_HexagonRadius,
                                      zone_2_bound_HexagonRadius,
                                      zone_1_HexagonRadius + dimen(R.dimen.x_keyboard_pad_padding));

        this.inputting_zones[0] = new XZone();
        this.inputting_zones[1] = create_zone_1(origin, orientation, zone_0_HexagonRadius, zone_1_HexagonRadius, true);
        this.inputting_zones[2] = this.zones[2];
    }

    /** 创建第 0 级分区：中心正六边形 */
    private XZone create_zone_0(
            PointF origin, HexagonOrientation orientation, float zone_0_HexagonRadius
    ) {
        XZone zone = new XZone();

        int bg_color = attrColor(R.attr.key_ctrl_locator_bg_color);
        String shadow_style = attrStr(R.attr.key_shadow_style);

        XPathPainter fill_painter = zone.newPathPainter();
        fill_painter.setFillShadow(shadow_style);
        fill_painter.setFillColor(bg_color);
        fill_painter.setCornerRadius(this.zone_0_CornerRadius);

        PointF[] centerHexagonVertexes = ViewUtils.drawHexagon(fill_painter.path,
                                                               orientation,
                                                               origin,
                                                               zone_0_HexagonRadius);
        zone.blocks.add(new XZone.PolygonBlock(centerHexagonVertexes));

        return zone;
    }

    /** 创建第 1 级分区：内六边形 */
    private XZone create_zone_1(
            PointF origin, HexagonOrientation orientation, //
            float zone_0_HexagonRadius, float zone_1_HexagonRadius, //
            boolean noDivider
    ) {
        XZone zone = new XZone();

        int bg_color = attrColor(R.attr.x_keyboard_ctrl_bg_style);
        String divider_style = attrStr(R.attr.x_keyboard_ctrl_divider_style);
        String shadow_style = attrStr(R.attr.x_keyboard_shadow_style);

        XPathPainter fill_painter = zone.newPathPainter();
        fill_painter.setFillColor(bg_color);
        fill_painter.setFillShadow(shadow_style);
        fill_painter.setCornerRadius(this.zone_1_CornerRadius);

        float innerHexagonAxisRadius = zone_0_HexagonRadius + (zone_1_HexagonRadius - zone_0_HexagonRadius) * 0.25f;
        PointF[] innerHexagonVertexes = ViewUtils.drawHexagon(fill_painter.path,
                                                              orientation,
                                                              origin,
                                                              zone_1_HexagonRadius);
        PointF[] innerHexagonAxisVertexes = ViewUtils.createHexagon(orientation, origin, innerHexagonAxisRadius);

        for (int i = 0; i < innerHexagonVertexes.length; i++) {
            int currentIndex = i;
            int nextIndex = (i + 1) % innerHexagonVertexes.length;

            PointF current = innerHexagonVertexes[i];
            PointF next = innerHexagonVertexes[nextIndex];

            PointF axisCurrent = innerHexagonAxisVertexes[currentIndex];
            PointF axisNext = innerHexagonAxisVertexes[nextIndex];

            XZone.PolygonBlock block = new XZone.PolygonBlock(origin, current, next);
            zone.blocks.add(block);

            // 中心 Link 为其可视的梯形区域
            block.links.center.addVertexes(axisCurrent, current, next, axisNext);
        }

        if (!noDivider) {
            // - 绘制分隔线
            XPathPainter stroke_painter = zone.newPathPainter();
            stroke_painter.setStrokeCap(Paint.Cap.ROUND);
            stroke_painter.setStrokeStyle(divider_style);

            // Note：圆角是通过指定半径的圆与矩形的角相切再去掉角的外部后得到的；切点过圆心的线一定与切线垂直
            // - 不清楚为何实际绘制的圆角半径是定义半径的 1.6 倍？
            float innerHexagonCornerActualRadius = this.zone_1_CornerRadius * 1.6f;
            float innerHexagonCropRadius = zone_1_HexagonRadius - innerHexagonCornerActualRadius * (cos_30_divided_by_1
                                                                                                    - 1);
            float innerHexagonCrossRadius = zone_0_HexagonRadius * 0.9f; // 确保中心按下后依然能够显示分隔线
            PointF[] innerHexagonCropCornerVertexes = ViewUtils.createHexagon(orientation, origin, //
                                                                              innerHexagonCropRadius);
            PointF[] innerHexagonCrossCircleVertexes = ViewUtils.createHexagon(orientation,
                                                                               origin,
                                                                               innerHexagonCrossRadius);
            for (int i = 0; i < innerHexagonVertexes.length; i++) {
                PointF start = innerHexagonCrossCircleVertexes[i];
                PointF end = innerHexagonCropCornerVertexes[i];

                stroke_painter.path.moveTo(start.x, start.y);
                stroke_painter.path.lineTo(end.x, end.y);
            }
        }

//        PointF[] vertexes = ViewUtils.createHexagon(orientation,
//                                                    origin,
//                                                    zone_1_HexagonRadius
//                                                    - innerHexagonCornerActualRadius * cos_30_divided_by_1);
//        for (PointF start : vertexes) {
//            stroke_painter.path.addCircle(start.x, start.y, innerHexagonCornerActualRadius, Path.Direction.CW);
//        }

        return zone;
    }

    /** 绘制第 2 级分区：外六边形，不封边，且辐射轴线范围内均为其分区空间 */
    private XZone create_zone_2(
            PointF origin, HexagonOrientation orientation, //
            float zone_2_HexagonRadius, float zone_2_bound_HexagonRadius, //
            float spacing_from_origin
    ) {
        XZone zone = new XZone();

        String divider_style = attrStr(R.attr.x_keyboard_chars_divider_style);
        String shadow_style = attrStr(R.attr.x_keyboard_chars_divider_shadow_style);

        XPathPainter stroke_painter = zone.newPathPainter();
        stroke_painter.setStrokeCap(Paint.Cap.ROUND); // 设置端点为圆形
        stroke_painter.setStrokeStyle(divider_style);
        stroke_painter.setStrokeShadow(shadow_style);

        PointF[] outerHexagonVertexes = ViewUtils.createHexagon(orientation, origin, zone_2_HexagonRadius);
        for (PointF outerHexagonVertex : outerHexagonVertexes) {
            PointF start = origin;
            PointF end = outerHexagonVertex;

            stroke_painter.path.moveTo(start.x, start.y);
            stroke_painter.path.lineTo(end.x, end.y);
        }

        // - 确定一个最大外边界
        PointF[] maxHexagonBoundVertexes = ViewUtils.createHexagon(orientation, origin, zone_2_bound_HexagonRadius);
        for (int i = 0; i < outerHexagonVertexes.length; i++) {
            int currentIndex = i;
            int nextIndex = (i + 1) % maxHexagonBoundVertexes.length;

            PointF current = maxHexagonBoundVertexes[currentIndex];
            PointF next = maxHexagonBoundVertexes[nextIndex];

            XZone.PolygonBlock block = new XZone.PolygonBlock(origin, current, next);
            zone.blocks.add(block);
        }

        // - 添加垂直于左右轴线的 Link
        int axis_link_count = 4;
        float outerHexagonAxisSpacing = (zone_2_HexagonRadius - spacing_from_origin) / (axis_link_count - 1);

        PointF[][] axisHexagonVertexesArray = new PointF[axis_link_count][];
        for (int i = 0; i < axis_link_count; i++) {
            float axisHexagonRadius = (spacing_from_origin + outerHexagonAxisSpacing * i) //
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

                XZone.PolygonBlock block = (XZone.PolygonBlock) zone.blocks.get(j);

                if (i == 1) {
                    block.links.center.addVertexes(innerCurrent, outerCurrent);
                }

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

        return zone;
    }

    private void start_zone_1_animator(float from, float to) {
        start_zone_1_animator(from, to, null);
    }

    private void start_zone_1_animator(float from, float to, Animator.AnimatorListener listener) {
        float current = from;
        if (this.zone_1_animator != null) {
            current = (float) this.zone_1_animator.getAnimatedValue();

            stop_zone_1_animator();
        }

        this.zone_1_animator = ValueAnimator.ofFloat(current, to);
        this.zone_1_animator.setDuration(80);

        if (listener != null) {
            this.zone_1_animator.addListener(listener);
        }
        this.zone_1_animator.addUpdateListener(this.zone_1_animator_update_listener);

        this.zone_1_animator.start();
    }

    private void stop_zone_1_animator() {
        if (this.zone_1_animator == null) {
            return;
        }

        this.zone_1_animator.cancel();
        this.zone_1_animator = null;
    }

    private void on_zone_1_update_animation(@NonNull ValueAnimator animation) {
        invalidate();

        PointF origin = this.center_coordinate;
        HexagonOrientation orientation = this.orientation;
        float zone_0_HexagonRadius = this.zone_0_HexagonRadius;

        float zone_1_HexagonRadius = (float) animation.getAnimatedValue();
        this.inputting_zones[1] = create_zone_1(origin, orientation, zone_0_HexagonRadius, zone_1_HexagonRadius, true);
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

    private <T> T getAt(T[] array, int index) {
        int size = array.length;
        int idx = getIndex(size, index);

        return array[idx];
    }

    private int getIndex(int size, int index) {
        int idx = (index < 0 ? size + index : index) % size;

        if (this.reversed) {
            idx = size == 2 ? (size - 1 - index) % size : (size + (size / 2 - 1) - index) % size;
        }
        return idx;
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

                BlockKey[][] blockKeysArray = getBlockKeys_In_Zone_2(blockIndex.block, true);
                BlockKey[] blockKeys = getAt(blockKeysArray, blockIndexDiff > 0 ? 1 : 0);

                BlockKey[] nonNullBlockKeys = Arrays.stream(blockKeys)
                                                    .filter((bk) -> !BlockKey.isNull(bk))
                                                    .toArray(BlockKey[]::new);
                int totalBlockKeys = nonNullBlockKeys.length;
                if (totalBlockKeys == 0) {
                    return null;
                }

                int blockKeyAbsIndex = Math.abs(blockIndexDiff) - 1;
                int blockKeyIndex = blockKeyAbsIndex % totalBlockKeys;

                return nonNullBlockKeys[blockKeyIndex];
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

        return BlockKey.isNull(blockKey) || blockKey.key.disabled ? null : blockKey;
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

    private BlockKey[][] getBlockKeys_In_Zone_2(int blockIndex, boolean isActiveBlock) {
        BlockKey[][] blockKeysArray = getAt(this.zone_2_keys, blockIndex);
        if (this.state.type != XPadState.Type.InputChars_Input_Doing || !isActiveBlock) {
            return blockKeysArray;
        }

        XPadState.BlockData stateData = (XPadState.BlockData) this.state.data;
        int blockIndexDiff = stateData.getBlockDiff();
        if (blockIndexDiff == 0) {
            return blockKeysArray;
        }

        int blockKeysIndex = blockIndexDiff > 0 ? 1 : 0;
        BlockKey[] blockKeys = getAt(blockKeysArray, blockKeysIndex);
        BlockKey[] nonNullBlockKeys = Arrays.stream(blockKeys)
                                            .filter((bk) -> !BlockKey.isNull(bk))
                                            .toArray(BlockKey[]::new);
        int totalBlockKeys = nonNullBlockKeys.length;
        if (totalBlockKeys == 0) {
            return blockKeysArray;
        }

        int blockKeyAbsIndex = Math.abs(blockIndexDiff) - 1;
        int replacementBlockKeyIndex = blockKeyAbsIndex / totalBlockKeys;
        if (replacementBlockKeyIndex == 0) {
            return blockKeysArray;
        }

        BlockKey[][] newBlockKeysArray = Arrays.copyOf(blockKeysArray, blockKeysArray.length);
        for (int i = 0; i < blockKeysArray.length; i++) {
            newBlockKeysArray[i] = Arrays.copyOf(blockKeysArray[i], blockKeysArray[i].length);
        }

        blockKeys = getAt(newBlockKeysArray, blockKeysIndex);

        boolean changed = false;
        for (int i = 0; i < blockKeys.length; i++) {
            BlockKey blockKey = blockKeys[i];

            if (!BlockKey.isNull(blockKey) //
                && blockKey.key instanceof CharKey //
                && ((CharKey) blockKey.key).hasReplacement()) {
                Key key = ((CharKey) blockKey.key).createReplacementKey(replacementBlockKeyIndex);

                blockKeys[i] = new BlockKey(blockKey, key);
                changed = true;
            }
        }

        return changed ? newBlockKeysArray : blockKeysArray;
    }

    private void onGesture_End(
            KeyboardView.GestureListener listener, ViewGestureDetector.GestureData data, //
            boolean simulationTerminated
    ) {
        reset();

        Key key = CtrlKey.build(CtrlKey.Type.NoOp);
        trigger_Gesture(listener, key, ViewGestureDetector.GestureType.PressEnd, data);

        if (simulationTerminated) {
            key = CtrlKey.build(CtrlKey.Type.XPad_Simulation_Terminated);
            trigger_Gesture(listener, key, ViewGestureDetector.GestureType.PressEnd, data);
        }
    }

    private void onGesture_Over_Zone_0(
            KeyboardView.GestureListener listener, ViewGestureDetector.GestureType type,
            ViewGestureDetector.GestureData data
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
            case SingleTap: // 用于播放双击提示音：两次单击音效
            case Flipping: {
                trigger_Gesture(listener, blockKey, type, data);
                break;
            }
            case DoubleTap:
            case LongPressStart: {
                trigger_Gesture(listener, blockKey, type, data);

                // Note：进入编辑器编辑状态会发生键盘切换，故而，需显式重置状态
                reset();
                break;
            }
        }
    }

    private void onGesture_Over_Zone_1(
            KeyboardView.GestureListener listener, ViewGestureDetector.GestureType type,
            ViewGestureDetector.GestureData data
    ) {
        switch (type) {
            case SingleTap: {
                BlockKey blockKey = getBlockKey(this.active_block);
                if (BlockKey.isNull(blockKey)) {
                    return;
                }

                trigger_Gesture(listener, blockKey, type, data);
                break;
            }
            case MovingStart: {
                this.state = new XPadState(XPadState.Type.InputChars_Input_Waiting);

                // 预备输入：减少定位输入分区的滑行距离
                start_zone_1_animator(this.zone_1_HexagonRadius, this.zone_1_HexagonRadius_input_waiting);

                BlockKey blockKey = getBlockKey(this.active_block);
                if (BlockKey.isNull(blockKey)) {
                    return;
                }

                // 发送点击事件以触发通用的子键盘切换
                trigger_SingleTap_Gesture(listener, blockKey, data);
                break;
            }
            case Moving: {
                if (this.state.type != XPadState.Type.InputChars_Input_Doing) {
                    return;
                }

                // 再次进入第 2 分区，则表示当前输入已确定
                BlockKey blockKey = getActiveBlockKey_In_Zone_2();

                // 重置当前的状态数据：在获取 BlockKey 后再执行
                XPadState.BlockData stateData = (XPadState.BlockData) this.state.data;
                stateData.reset();

                if (BlockKey.isNull(blockKey)) {
                    return;
                }

                trigger_SingleTap_Gesture(listener, blockKey, data);
                break;
            }
        }
    }

    private void onGesture_Over_Zone_2(
            KeyboardView.GestureListener listener, ViewGestureDetector.GestureType type,
            ViewGestureDetector.GestureData data
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

    private void trigger_Moving_Gesture(
            KeyboardView.GestureListener listener, Key key, ViewGestureDetector.GestureData data
    ) {
        ViewGestureDetector.MovingGestureData movingData = new ViewGestureDetector.MovingGestureData(data,
                                                                                                     new Motion());
        trigger_Gesture(listener, key, ViewGestureDetector.GestureType.Moving, movingData);
    }

    private void trigger_SingleTap_Gesture(
            KeyboardView.GestureListener listener, BlockKey blockKey, ViewGestureDetector.GestureData data
    ) {
        ViewGestureDetector.SingleTapGestureData tapData = new ViewGestureDetector.SingleTapGestureData(data, 0);

        trigger_Gesture(listener, blockKey, ViewGestureDetector.GestureType.SingleTap, tapData);
    }

    private void trigger_Gesture(
            KeyboardView.GestureListener listener, BlockKey blockKey, ViewGestureDetector.GestureType type,
            ViewGestureDetector.GestureData data
    ) {
        Key key = !BlockKey.isNull(blockKey) ? blockKey.key : null;

        trigger_Gesture(listener, key, type, data);
    }

    private void trigger_Gesture(
            KeyboardView.GestureListener listener, Key key, ViewGestureDetector.GestureType type,
            ViewGestureDetector.GestureData data
    ) {
        if (listener != null) {
            // Note：向外部传递的 GestureData 不需要做坐标转换
            listener.onGesture(key, type, data);
        }
    }

    // ====================================================================
    public GestureSimulator createSimulator() {
        return new GestureSimulator();
    }

    private PointF get_center_coordinate() {
        return this.center_coordinate;
    }

    private BlockKey[] get_zone_1_keys() {
        return this.zone_1_keys;
    }

    private BlockKey[][][] get_zone_2_keys() {
        return this.zone_2_keys;
    }
    // ====================================================================

    private static class BlockIndex {
        public final int zone;
        public final int block;

        private BlockIndex(int zone, int block) {
            this.zone = zone;
            this.block = block;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            BlockIndex that = (BlockIndex) o;
            return this.zone == that.zone && this.block == that.block;
        }
    }

    private static class BlockKey extends BlockIndex {
        public final int x;
        public final int y;
        public final Key key;

        public BlockKey(int zone, int block, int x, int y, Key key) {
            super(zone, block);

            this.x = x;
            this.y = y;
            this.key = key;
        }

        public BlockKey(BlockKey other, Key key) {
            this(other.zone, other.block, other.x, other.y, key);
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
                   && this.block == that.block //
                   && this.x == that.x //
                   && this.y == that.y //
                   && Objects.equals(this.key, that.key);
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.zone, this.block, this.x, this.y, this.key);
        }
    }

    private class Zone1AnimatorUpdateListener implements ValueAnimator.AnimatorUpdateListener {
        @Override
        public void onAnimationUpdate(@NonNull ValueAnimator animation) {
            on_zone_1_update_animation(animation);
        }
    }

    public class GestureSimulator {
        private final Handler handler = new Handler();
        private boolean stopped = false;

        public void stop() {
            Log.d(getClass().getSimpleName(), "stop");
            this.handler.stop();

            this.stopped = true;
            XPadView.this.simulating = false;
        }

        public boolean isStopped() {
            return this.stopped;
        }

        public void input(Key start, Key charKey, Runnable after) {
            this.stopped = false;
            XPadView.this.simulating = true;

            doInput(start, charKey, () -> {
                after.run();
                XPadView.this.simulating = false;
            });
        }

        public void input(Key charKey, Runnable after) {
            input(null, charKey, after);
        }

        private void doInput(Key start, Key charKey, Runnable after) {
            PointF endPoint = get_center_coordinate();

            // ==================================================================
            List<Runnable> gestures = new ArrayList<>();

            if (start == null) {
                gestures.add(() -> {
                    ViewGestureDetector.GestureType type = ViewGestureDetector.GestureType.PressStart;
                    XPadView.this.trailer.onGesture(type, createGestureData(type, endPoint));
                });
            } else {
                gestures.add(() -> {
                    BlockKey startBlockKey = getMatchedBlockKey(start);
                    PointF startPoint = getBlockCenter(startBlockKey);

                    executeGesture(ViewGestureDetector.GestureType.PressEnd, startPoint);
                    executeGesture(ViewGestureDetector.GestureType.PressStart, startPoint);

                    executeGesture(ViewGestureDetector.GestureType.MovingStart, startPoint);
                    executeGesture(ViewGestureDetector.GestureType.Moving, startPoint);

                    executeGesture(ViewGestureDetector.GestureType.Moving, endPoint);
                });
            }

            gestures.addAll(createCharKeyGesture(charKey));

            gestures.add(() -> {
                executeGesture(ViewGestureDetector.GestureType.Moving, endPoint);
            });

            if (start == null) {
                gestures.add(() -> {
                    ViewGestureDetector.GestureType type = ViewGestureDetector.GestureType.PressEnd;
                    XPadView.this.trailer.onGesture(type, createGestureData(type, endPoint));

                    type = ViewGestureDetector.GestureType.PressStart;
                    XPadView.this.trailer.onGesture(type, createGestureData(type, endPoint));
                });
            } else {
                gestures.add(() -> {
                    executeGesture(ViewGestureDetector.GestureType.PressEnd, endPoint);
                });
            }

            // ============================================================
            gestures.add(after);

            executeGestures(gestures);
        }

        private void executeGestures(List<Runnable> cbs) {
            if (cbs.isEmpty()) {
                return;
            }

            Message msg = this.handler.obtainMessage(Handler.MSG_TICK, cbs);
            this.handler.sendMessageDelayed(msg, 500);
        }

        private void executeGesture(ViewGestureDetector.GestureType type, PointF point) {
            PointF offset = new PointF(0, 0);
            ViewGestureDetector.GestureData data = createGestureData(type, point);

            onGesture(null, type, data, offset, false);
        }

        private List<Runnable> createCharKeyGesture(Key charKey) {
            BlockKey charBlockKey = getMatchedBlockKey(charKey);
            int charBlockKeyAxis = charBlockKey.x;
            int charBlockKeyCross = charBlockKey.y + 2;

            List<Runnable> gestures = new ArrayList<>();
            for (int i = 0; i < charBlockKeyCross; i++) {
                int targetBlock = charBlockKey.block + (charBlockKeyAxis == 0 ? -i : i);
                BlockIndex targetBlockIndex = new BlockIndex(charBlockKey.zone, targetBlock);

                gestures.add(() -> {
                    PointF point = getBlockCenter(targetBlockIndex);
                    executeGesture(ViewGestureDetector.GestureType.Moving, point);
                });
            }
            return gestures;
        }

        private ViewGestureDetector.GestureData createGestureData(ViewGestureDetector.GestureType type, PointF point) {
            ViewGestureDetector.GestureData data = new ViewGestureDetector.GestureData(point.x, point.y, 0);

            switch (type) {
                case Moving:
                case MovingStart: {
                    return new ViewGestureDetector.MovingGestureData(data, null);
                }
            }
            return data;
        }

        private PointF getBlockCenter(BlockIndex blockIndex) {
            XZone[] zones = determineZones();
            List<XZone.Block> blocks = zones[blockIndex.zone].blocks;
            int index = getIndex(blocks.size(), blockIndex.block);
            XZone.BaseBlock block = (XZone.BaseBlock) blocks.get(index);

            return block.links.center.center;
        }

        private BlockKey getMatchedBlockKey(Key key) {
            for (BlockKey blockKey : get_zone_1_keys()) {
                if (BlockKey.isNull(blockKey)) {
                    continue;
                }

                if (key instanceof CtrlKey //
                    && ((CtrlKey) key).type.match(blockKey.key) //
                    && Objects.equals(((CtrlKey) key).option, ((CtrlKey) blockKey.key).option) //
                ) {
                    return blockKey;
                }
            }

            for (BlockKey[][] zone_2_key : get_zone_2_keys()) {
                for (BlockKey[] blockKeys : zone_2_key) {
                    BlockKey[] nonNullBlockKeys = Arrays.stream(blockKeys)
                                                        .filter((bk) -> !BlockKey.isNull(bk))
                                                        .toArray(BlockKey[]::new);
                    for (int i = 0; i < nonNullBlockKeys.length; i++) {
                        BlockKey blockKey = nonNullBlockKeys[i];

                        if (key.value.equals(blockKey.key.value)) {
                            return new BlockKey(blockKey.zone, blockKey.block, blockKey.x, i, key);
                        } else if (blockKey.key instanceof CharKey //
                                   && ((CharKey) blockKey.key).canReplaceTheKey(key) //
                        ) {
                            return new BlockKey(blockKey.zone, blockKey.block, blockKey.x,
                                                // Note：替换字符的输入位置需要环绕轴上字符一周
                                                i + nonNullBlockKeys.length, key);
                        }
                    }
                }
            }
            return null;
        }

        private class Handler extends android.os.Handler {
            private static final int MSG_TICK = 1;

            public void stop() {
                removeMessages(MSG_TICK);
            }

            @Override
            public void handleMessage(Message msg) {
                if (msg.what == MSG_TICK) {
                    List<Runnable> cbs = (List<Runnable>) msg.obj;
                    Runnable first = CollectionUtils.first(cbs);

                    first.run();
                    executeGestures(cbs.isEmpty() ? cbs : cbs.subList(1, cbs.size()));
                }
            }
        }
    }
}
