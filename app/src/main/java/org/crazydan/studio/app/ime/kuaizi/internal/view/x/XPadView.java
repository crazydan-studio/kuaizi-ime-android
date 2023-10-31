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
import android.graphics.CornerPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
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

    private final HexagonOrientation orientation = HexagonOrientation.FLAT_TOP;
    private final Paint paint;
    private final Paint textPaint;
    private final Path path;
    private final PointF[][] zoneVertexes = new PointF[6][];
    private final PointF currentPoint = new PointF(-100000, -100000);

    private final Path trailPath = new Path();
    private final Paint trailPaint = new Paint();
    private final float[] trailPathPos = new float[2];
    private final PathMeasure trailPathMeasure = new PathMeasure();

    private int padding = ScreenUtils.dpToPx(4);
    private int hexagonRadius = 152;
    private int hexagonCornerRadius = 12;
    private String dividerStyle;

    public XPadView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        this.dividerStyle = ThemeUtils.getStringByAttrId(context, R.attr.x_keyboard_divider_style);

        this.path = new Path();

        // 画图和文字的 Paint 必须单独定义，画图的画笔设置会影响文字的样式，
        // 若二者共用，会导致绘制的文字样式混乱。
        // 比如，设置了 CornerPathEffect 的画笔会使得文字的拐角都被圆角化
        this.paint = new Paint();
        this.paint.setAntiAlias(true);

        this.textPaint = new Paint();
        this.textPaint.setAntiAlias(true);

        ViewUtils.prepareForShadow(this);
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
        drawPad(canvas);

        // 绘制滑屏轨迹
        drawTrailPath(canvas);
    }

    private void drawPad(Canvas canvas) {
        int width = getWidth();
        int height = getHeight();
        PointF center = new PointF(width / 2f, height / 2f);
        float innerCircleMaxRadius = Math.min(center.x, center.y) - this.padding;

        float innerHexagonRadius = this.hexagonRadius;
        float outerHexagonRadius = this.orientation == HexagonOrientation.FLAT_TOP
                                   ? innerCircleMaxRadius / cos_30
                                   : innerCircleMaxRadius;

        PointF origin = this.orientation == HexagonOrientation.FLAT_TOP //
                        ? new PointF(width - outerHexagonRadius - this.padding,
                                     height - innerCircleMaxRadius - this.padding) //
                        : new PointF(width - outerHexagonRadius * cos_30 - this.padding,
                                     height - outerHexagonRadius - this.padding);

        float circleRadius = this.hexagonRadius * 0.4f;
        int keyNormalColor = ThemeUtils.getColorByAttrId(getContext(), R.attr.key_fg_color);
        int keyHighlightColor = ThemeUtils.getColorByAttrId(getContext(), R.attr.key_highlight_fg_color);

        // 绘制正六边形
        this.path.reset();

        ThemeUtils.applyBorder(this.paint, this.dividerStyle);
        CornerPathEffect effect = new CornerPathEffect(this.hexagonCornerRadius);
        this.paint.setPathEffect(effect);

        PointF[] innerHexagonVertexes = ViewUtils.drawHexagon(this.path, this.orientation, origin, innerHexagonRadius);
        canvas.drawPath(this.path, this.paint);

        // 绘制辐射轴线：让后续的中心圆遮盖相交的部分（注：做 path 运算似乎不起作用）
        this.path.reset();

        PointF[] outerHexagonVertexes = ViewUtils.createHexagon(this.orientation, origin, outerHexagonRadius);
        for (PointF end : outerHexagonVertexes) {
            PointF start = origin;

            this.path.moveTo(start.x, start.y);
            this.path.lineTo(end.x, end.y);
        }
        canvas.drawPath(this.path, this.paint);

//        // 绘制外边界
//        this.path.reset();
//        this.path.addRect(0, 0, width, height, Path.Direction.CW);
//        canvas.drawPath(this.path, this.paint);

        // 查找激活的分区
        int activeZoneIndex = -1;
        PointF[] maxBoundHexagonVertexes = ViewUtils.createHexagon(this.orientation, origin, outerHexagonRadius * 2);
        for (int i = 0; i < innerHexagonVertexes.length; i++) {
            int startIndex = i;
            int endIndex = (i + 1) % innerHexagonVertexes.length;
            PointF innerStart = innerHexagonVertexes[startIndex];
            PointF innerEnd = innerHexagonVertexes[endIndex];
            PointF outerStart = maxBoundHexagonVertexes[startIndex];
            PointF outerEnd = maxBoundHexagonVertexes[endIndex];

            this.zoneVertexes[i] = new PointF[] { innerEnd, innerStart, outerStart, outerEnd };

            if (ViewUtils.isPointInPolygon(this.currentPoint, this.zoneVertexes[i])) {
                activeZoneIndex = i;
            }
        }

        // 绘制中心圆
        this.path.reset();

        int circleBgColor = ThemeUtils.getColorByAttrId(getContext(), R.attr.key_bg_color);
        String circleBorder = this.dividerStyle;

        this.paint.setStyle(Paint.Style.FILL);
        this.paint.setColor(circleBgColor);

        this.path.addCircle(origin.x, origin.y, circleRadius, Path.Direction.CW);
        canvas.drawPath(this.path, this.paint);

        ThemeUtils.applyBorder(this.paint, circleBorder);
        canvas.drawPath(this.path, this.paint);

        // 绘制输入文本
        float keyTextSize = 45f;
        float keySpacing = (outerHexagonRadius - innerHexagonRadius - keyTextSize * 0.5f) //
                           / 3f;
//        // - 绘制输入文本布局线
//        this.path.reset();
//
//        for (int i = 0; i < 4; i++) {
//            float radius = keySpacing * (i + 1) + innerHexagonRadius;
//
//            this.path.reset();
//            ViewUtils.drawHexagon(this.path,
//                                  this.orientation == HexagonOrientation.FLAT_TOP
//                                  ? HexagonOrientation.POINTY_TOP
//                                  : HexagonOrientation.FLAT_TOP,
//                                  origin,
//                                  radius);
//            canvas.drawPath(this.path, this.paint);
//        }

        // - 绘制输入文本
        this.path.reset();

        //this.textPaint.setStrokeWidth(0.75f * ScreenUtils.dpToPx(1));
        this.textPaint.setStyle(Paint.Style.FILL);
        this.textPaint.setTextSize(keyTextSize);

        // 采用三维坐标定位文本位置：
        // - 第一维：辐射轴线划分的分区。从最右边轴线开始，顺时针旋转一周所遇到的分区，其编号从 0 开始依次加 1
        // - 第二维：每个分区的两条辐射轴线，沿顺时针方向，依次定为 x 轴和 y 轴
        // - 底三维：分区的 x/y 轴上的位置
        // 例如，(0, 0, 1) 表示第 0 区的 x 轴上序号为 1 的点；(1, 2, 0) 表示第 1 区 y 轴上序号为 2 的点
        String[][][] keys = getKeys();

        float keyTextSpacing = 20;
        float keyTextHeight = -this.textPaint.getFontMetrics().ascent;
        for (int i = 0; i < 3; i++) {
            float radius = keySpacing * (i + 1) + innerHexagonRadius;
            PointF[] vertexes = ViewUtils.createHexagon(this.orientation == HexagonOrientation.FLAT_TOP
                                                        ? HexagonOrientation.POINTY_TOP
                                                        : HexagonOrientation.FLAT_TOP, origin, radius);

            for (int j = 0; j < vertexes.length; j++) {
                PointF before;
                PointF current;
                PointF after;

                if (this.orientation == HexagonOrientation.FLAT_TOP) {
                    before = vertexes[j];
                    current = vertexes[(j + 1) % vertexes.length];
                    after = vertexes[(j + 2) % vertexes.length];
                } else {
                    before = vertexes[(j > 0 ? j : vertexes.length) - 1];
                    current = vertexes[j];
                    after = vertexes[(j + 1) % vertexes.length];
                }

                PointF xPoint = new PointF((current.x + before.x) / 2f, (current.y + before.y) / 2f);
                PointF yPoint = new PointF((current.x + after.x) / 2f, (current.y + after.y) / 2f);

                String xKey = keys[j][0][i];
                String yKey = keys[j][1][i];

                float xKeyWidth = getTextWidth(this.textPaint, xKey);
                float yKeyWidth = getTextWidth(this.textPaint, yKey);

                this.textPaint.setColor(j == activeZoneIndex ? keyHighlightColor : keyNormalColor);

                float hOffset;
                float vOffset;

                // 靠近分区 x 轴绘制文本
                float xHOffset = xKeyWidth;
                this.path.reset();
                if (j == 0) {
                    this.path.moveTo(origin.x, origin.y);
                    this.path.lineTo(xPoint.x + xHOffset, xPoint.y);
                    this.textPaint.setTextAlign(Paint.Align.RIGHT);
                    hOffset = 0; // 偏移无效
                    vOffset = keyTextHeight + keyTextSpacing * 0.5f;
                } else if (j == 3) {
                    this.path.moveTo(xPoint.x - xHOffset, xPoint.y);
                    this.path.lineTo(origin.x, origin.y);
                    this.textPaint.setTextAlign(Paint.Align.LEFT);
                    hOffset = 0; // 偏移无效
                    vOffset = -keyTextSpacing;
                } else if (j <= 2) {
                    this.path.moveTo(current.x, current.y);
                    this.path.lineTo(xPoint.x, xPoint.y);
                    this.textPaint.setTextAlign(Paint.Align.RIGHT);
                    hOffset = -keyTextSpacing;
                    vOffset = keyTextHeight;
                } else {
                    this.path.moveTo(xPoint.x, xPoint.y);
                    this.path.lineTo(current.x, current.y);
                    this.textPaint.setTextAlign(Paint.Align.LEFT);
                    hOffset = keyTextSpacing;
                    vOffset = 0;
                }
                canvas.drawTextOnPath(xKey, this.path, hOffset, vOffset, this.textPaint);

                // 靠近分区 y 轴绘制文本
                float yHOffset = yKeyWidth;
                this.path.reset();
                if (j == 2) {
                    this.path.moveTo(yPoint.x - yHOffset, yPoint.y);
                    this.path.lineTo(origin.x, origin.y);
                    this.textPaint.setTextAlign(Paint.Align.LEFT);
                    hOffset = 0; // 偏移无效
                    vOffset = keyTextHeight + keyTextSpacing * 0.5f;
                } else if (j == 5) {
                    this.path.moveTo(origin.x, origin.y);
                    this.path.lineTo(yPoint.x + yHOffset, yPoint.y);
                    this.textPaint.setTextAlign(Paint.Align.RIGHT);
                    hOffset = 0; // 偏移无效
                    vOffset = -keyTextSpacing;
                } else if (j <= 2) {
                    this.path.moveTo(yPoint.x, yPoint.y);
                    this.path.lineTo(current.x, current.y);
                    this.textPaint.setTextAlign(Paint.Align.LEFT);
                    hOffset = keyTextSpacing;
                    vOffset = keyTextHeight;
                } else {
                    this.path.moveTo(current.x, current.y);
                    this.path.lineTo(yPoint.x, yPoint.y);
                    this.textPaint.setTextAlign(Paint.Align.RIGHT);
                    hOffset = -keyTextSpacing;
                    vOffset = 0;
                }
                canvas.drawTextOnPath(yKey, this.path, hOffset, vOffset, this.textPaint);
            }
        }

        // 绘制按钮文本
        float labelTextSize = 35f;
        this.textPaint.setTextSize(labelTextSize);

        String[] switcherTexts = new String[] { "英文", "拼音", "数字", "算数", "", "大写" };
        for (int i = 0; i < innerHexagonVertexes.length; i++) {
            PointF start = innerHexagonVertexes[i]; // 分区 x 轴上的点
            PointF end = innerHexagonVertexes[(i + 1) % innerHexagonVertexes.length]; // 分区 y 轴上的点

            this.path.reset();
            // 在水平线上部，需在外圈绘制文本（绘制线向量沿顺时针方向），以确保文字直立显示
            if (i > 2) {
                this.path.moveTo(start.x, start.y);
                this.path.lineTo(end.x, end.y);
            }
            // 在水平线下部，需在内圈绘制文本（绘制线向量沿逆时针方向），以确保文字直立显示
            else {
                this.path.moveTo(end.x, end.y);
                this.path.lineTo(start.x, start.y);
            }

            this.textPaint.setColor(keyNormalColor);
            this.textPaint.setTextAlign(Paint.Align.CENTER);

            canvas.drawTextOnPath(switcherTexts[i],
                                  this.path,
                                  0,
                                  i > 2 ? labelTextSize * 1.25f : -labelTextSize * 0.5f,
                                  this.textPaint);
        }
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

    private float getTextWidth(Paint paint, String text) {
        float[] widths = new float[text.length()];
        paint.getTextWidths(text, widths);

        float total = 0;
        for (float width : widths) {
            total += width;
        }
        return total;
    }

    private RectF getTextBounds(Paint paint, String text) {
        Rect rect = new Rect();
        paint.getTextBounds(text, 0, text.length(), rect);

        return new RectF(rect.left, rect.top, rect.right, rect.bottom);
    }

    private String[][][] getKeys() {
        return new String[][][] {
                new String[][] {
                        new String[] { "i", "u", "ü" }, new String[] { "", "空格", "" },
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
