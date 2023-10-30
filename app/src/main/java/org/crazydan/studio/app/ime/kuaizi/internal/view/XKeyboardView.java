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

package org.crazydan.studio.app.ime.kuaizi.internal.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.CornerPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import androidx.annotation.Nullable;
import org.crazydan.studio.app.ime.kuaizi.R;
import org.crazydan.studio.app.ime.kuaizi.utils.ThemeUtils;
import org.crazydan.studio.app.ime.kuaizi.utils.ViewUtils;
import org.hexworks.mixite.core.api.HexagonOrientation;

/**
 * X 型全滑屏输入键盘视图
 * <p/>
 * 参考设计来源于：<a href="https://github.com/8VIM/8VIM">8VIM/8VIM</a>
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-10-29
 */
public class XKeyboardView extends View {
    private static final short TRAIL_STEPS = 100;
    private static final byte TRAIL_STEP_DISTANCE = 5;
    private static final byte TRAIL_MAX_RADIUS = 8;

    private final HexagonOrientation orientation = HexagonOrientation.FLAT_TOP;
    private final Paint paint;
    private final Path path;
    private final PointF[][] zoneVertexes = new PointF[6][];
    private final PointF currentPoint = new PointF();

    private final Path trailPath = new Path();
    private final Paint trailPaint = new Paint();
    private final float[] trailPathPos = new float[2];
    private final PathMeasure trailPathMeasure = new PathMeasure();

    private int hexagonRadius = 128;
    private int hexagonCornerRadius = 12;
    private String dividerStyle;

    public XKeyboardView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        this.dividerStyle = ThemeUtils.getStringByAttrId(context, R.attr.key_disabled_border_style);

        this.paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        this.path = new Path();

        ViewUtils.prepareForShadow(this);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent e) {
        invalidate();

        this.currentPoint.set(0, 0);

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
        float innerHexagonRadius = this.hexagonRadius;
        float outerHexagonRadius = Math.min(width, height) / 2f;
        float circleRadius = this.hexagonRadius * 0.4f;
        int keyNormalColor = ThemeUtils.getColorByAttrId(getContext(), R.attr.key_fg_color);
        int keyHighlightColor = ThemeUtils.getColorByAttrId(getContext(), R.attr.key_highlight_fg_color);

        // 绘制正六边形
        this.path.reset();

        ThemeUtils.applyBorder(this.paint, this.dividerStyle);
        CornerPathEffect effect = new CornerPathEffect(this.hexagonCornerRadius);
        this.paint.setPathEffect(effect);

        PointF[] innerHexagonVertexes = ViewUtils.drawHexagon(this.path, this.orientation, center, innerHexagonRadius);
        canvas.drawPath(this.path, this.paint);

        // 绘制辐射轴线：让后续的中心圆遮盖相交的部分（注：做 path 运算似乎不起作用）
        this.path.reset();

        PointF[] outerHexagonVertexes = ViewUtils.createHexagon(this.orientation, center, outerHexagonRadius);
        for (PointF end : outerHexagonVertexes) {
            PointF start = center;

            this.path.moveTo(start.x, start.y);
            this.path.lineTo(end.x, end.y);
        }
        canvas.drawPath(this.path, this.paint);

        // 查找激活的分区
        int activeZoneIndex = -1;
        for (int i = 0; i < innerHexagonVertexes.length; i++) {
            int startIndex = i;
            int endIndex = (i + 1) % innerHexagonVertexes.length;
            PointF innerStart = innerHexagonVertexes[startIndex];
            PointF innerEnd = innerHexagonVertexes[endIndex];
            PointF outerStart = outerHexagonVertexes[startIndex];
            PointF outerEnd = outerHexagonVertexes[endIndex];

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

        this.path.addCircle(center.x, center.y, circleRadius, Path.Direction.CW);
        canvas.drawPath(this.path, this.paint);

        ThemeUtils.applyBorder(this.paint, circleBorder);
        canvas.drawPath(this.path, this.paint);

        // 绘制输入文本
        this.path.reset();

        float keyTextSize = 35f;
        this.paint.setStrokeWidth(0f);
        this.paint.setStyle(Paint.Style.FILL_AND_STROKE);
        this.paint.setTextSize(keyTextSize);

        // 采用三维坐标定位文本位置：
        // - 第一维：辐射轴线划分的分区。从最右边轴线开始，顺时针旋转一周所遇到的分区，其编号从 0 开始依次加 1
        // - 第二维：每个分区的两条辐射轴线，沿顺时针方向，依次定为 x 轴和 y 轴
        // - 底三维：分区的 x/y 轴上的位置
        // 例如，(0, 0, 1) 表示第 0 区的 x 轴上序号为 1 的点；(1, 2, 0) 表示第 1 区 y 轴上序号为 2 的点
        float keyHalfTextSize = keyTextSize / 2f;
        float keySpacing = (outerHexagonRadius - innerHexagonRadius - keyHalfTextSize) / 3f;
        String[][][] keys = getKeys();

        for (int i = 0; i < 3; i++) {
            float radius = keySpacing * (i + 1) + innerHexagonRadius;
            PointF[] vertexes = ViewUtils.createHexagon(this.orientation, center, radius);

            for (int j = 0; j < vertexes.length; j++) {
                PointF start = vertexes[j]; // 分区 x 轴上的点
                PointF end = vertexes[(j + 1) % vertexes.length]; // 分区 y 轴上的点
                String xKey = keys[j][j > 2 ? 1 : 0][i];
                String yKey = keys[j][j > 2 ? 0 : 1][i];

                this.path.reset();
                // 在水平线上部，需在外圈绘制文本（绘制线向量沿顺时针方向），以确保文字直立显示
                if (j > 2) {
                    this.path.moveTo(start.x, start.y);
                    this.path.lineTo(end.x, end.y);
                }
                // 在水平线下部，需在内圈绘制文本（绘制线向量沿逆时针方向），以确保文字直立显示
                else {
                    this.path.moveTo(end.x, end.y);
                    this.path.lineTo(start.x, start.y);
                }
//                // 文本对齐的辅助线
//                canvas.drawPath(this.path, this.paint);

                this.paint.setColor(j == activeZoneIndex ? keyHighlightColor : keyNormalColor);

                // 靠近分区 x 轴绘制文本：文本沿向量方向右对齐
                this.paint.setTextAlign(Paint.Align.RIGHT);
                canvas.drawTextOnPath(xKey, this.path, -keyHalfTextSize, j > 2 ? keyHalfTextSize : 0, this.paint);

                // 靠近分区 y 轴绘制文本：文本沿向量方向左对齐
                this.paint.setTextAlign(Paint.Align.LEFT);
                canvas.drawTextOnPath(yKey, this.path, keyHalfTextSize, j > 2 ? keyHalfTextSize : 0, this.paint);
            }
        }

        // 绘制按钮文本
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

            this.paint.setColor(keyNormalColor);
            this.paint.setTextAlign(Paint.Align.CENTER);

            canvas.drawTextOnPath(switcherTexts[i], this.path, 0, i > 2 ? keyTextSize : -10, this.paint);
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

    private String[][][] getKeys() {
        return new String[][][] {
                new String[][] {
                        new String[] { "r", "g", "" }, new String[] { "Space", "", "" },
                        }, //
                new String[][] {
                        new String[] { "", "", "" }, new String[] { "p", "w", "y" },
                        }, //
                new String[][] {
                        new String[] { "a", "e", "o" }, new String[] { "h", "k", "t" },
                        }, //
                new String[][] {
                        new String[] { "i", "u", "ü" }, new String[] { "d", "b", "f" },
                        }, //
                new String[][] {
                        new String[] { "j", "q", "x" }, new String[] { "z", "c", "s" },
                        }, //
                new String[][] {
                        new String[] { "zh", "ch", "sh" }, new String[] { "n", "l", "m" },
                        },
                };
    }
}
