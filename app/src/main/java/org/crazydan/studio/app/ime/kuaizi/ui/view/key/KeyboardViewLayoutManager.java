/*
 * 筷字输入法 - 高效编辑需要又好又快的输入法
 * Copyright (C) 2024 Crazydan Studio
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

package org.crazydan.studio.app.ime.kuaizi.ui.view.key;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

import android.graphics.RectF;
import android.view.View;
import android.view.ViewGroup;
import androidx.recyclerview.widget.RecyclerView;
import org.crazydan.studio.app.ime.kuaizi.R;
import org.crazydan.studio.app.ime.kuaizi.common.utils.ScreenUtils;
import org.crazydan.studio.app.ime.kuaizi.common.widget.recycler.RecyclerViewLayoutManager;
import org.crazydan.studio.app.ime.kuaizi.core.key.XPadKey;
import org.crazydan.studio.app.ime.kuaizi.ui.view.KeyboardView;
import org.crazydan.studio.app.ime.kuaizi.ui.view.xpad.XPadView;
import org.hexworks.mixite.core.api.Hexagon;
import org.hexworks.mixite.core.api.HexagonOrientation;
import org.hexworks.mixite.core.api.HexagonalGridBuilder;
import org.hexworks.mixite.core.api.HexagonalGridLayout;
import org.hexworks.mixite.core.api.Point;
import org.hexworks.mixite.core.api.contract.SatelliteData;

import static org.crazydan.studio.app.ime.kuaizi.common.Constants.cos_30;

/**
 * {@link KeyboardView} 的 {@link RecyclerView} 布局器
 * <p/>
 * 注意：<ul>
 * <li>通过按键布局视图的 margin 设置按键与父视图的间隙，
 * 从而降低通过 padding 设置间隙所造成的计算复杂度；</li>
 * </ul>
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-07-01
 */
public class KeyboardViewLayoutManager extends RecyclerViewLayoutManager {
    /** 按键正六边形方向 */
    private HexagonOrientation gridItemOrientation;

    /** 按键正六边形半径：可见范围内的半径 */
    private float gridItemRadius;
    /** 按键正六边形最小半径 */
    private float gridItemMinRadius;
    /** 按键正六边形内部边界半径：与可见边相交的圆 */
    private float gridItemInnerRadius;
    /** 按键正六边形外部边界半径：含按键间隔，并与顶点相交的圆 */
    private float gridItemOuterRadius;
    /** 按键间隔 */
    private float gridItemSpacing;
    /** 按键列数 */
    private int gridColumns;
    /** 按键行数 */
    private int gridRows;
    /** 网格右部最大空白：配置数据，用于确保按键最靠近右手 */
    private float gridMaxPaddingRight;
    /** 网格底部空白：实际剩余空间 */
    private float gridPaddingBottom;

    private boolean reversed;
    private boolean xPadEnabled;
    private RectHexagon xPadKeyRectHexagon = null;
    private List<RectHexagon> rectHexagons;

    public void setGridItemOrientation(HexagonOrientation gridItemOrientation) {
        this.gridItemOrientation = gridItemOrientation;
    }

    public void setReversed(boolean reversed) {
        this.reversed = reversed;
    }

    public void enableXPad(boolean enabled) {
        this.xPadEnabled = enabled;
    }

    public void configGrid(
            int columns, int rows, float gridItemMinRadius, float gridItemSpacing, float gridMaxPaddingRight
    ) {
        this.gridColumns = columns;
        this.gridRows = rows;
        this.gridItemMinRadius = gridItemMinRadius;
        this.gridItemSpacing = gridItemSpacing;
        this.gridMaxPaddingRight = gridMaxPaddingRight;
    }

    @Override
    public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
        detachAndScrapAttachedViews(recycler);

        int itemCount = state.getItemCount();
        if (itemCount <= 0 || this.gridColumns <= 0 || this.gridRows <= 0) {
            return;
        }

        this.rectHexagons = createGrid(this.gridItemOrientation);
        this.xPadKeyRectHexagon = null;

        layoutRectHexagons(recycler, this.rectHexagons, itemCount);
    }

    private List<RectHexagon> createGrid(HexagonOrientation orientation) {
        // Note: 只有布局完成后，才能得到视图的宽高
        // 按键按照行列数自动适配屏幕尺寸（以 POINTY_TOP 方向为例，FLAT_TOP 方向为 POINTY_TOP 方向的 xy 轴交换）
        // - 这里按照按键紧挨的情况计算正六边形的半径，
        //   再在绘制时按照按键间隔计算正六边形的实际绘制半径
        // - 相邻六边形中心间距的计算公式见: https://www.redblobgames.com/grids/hexagons/#spacing
        // - 横向半径（r1）与宽度（w）的关系: w = (2 * n + 1) * r1 * cos30
        // - 纵向半径（r2）与高度（h）的关系: h = 2 * r2 + (m - 1) * cos30 * (2 * r2 * cos30)
        // - 最终正六边形的半径: radius = Math.min(r1, r2)
        // - 按键实际绘制半径（减去一半间距）: R = radius - spacing / (2 * cos30)
        int w = orientation == HexagonOrientation.POINTY_TOP ? getWidth() : getHeight();
        int h = orientation == HexagonOrientation.POINTY_TOP ? getHeight() : getWidth();
        int n = orientation == HexagonOrientation.POINTY_TOP ? this.gridColumns : this.gridRows;
        int m = orientation == HexagonOrientation.POINTY_TOP ? this.gridRows : this.gridColumns;
        float r1 = w / ((2 * n + 1) * cos_30);
        float r2 = h / (2 * ((m - 1) * cos_30 * cos_30 + 1));

        // 计算左上角的空白偏移量
        float gridPaddingTop = 0;
        float gridPaddingLeft = 0;
        this.gridPaddingBottom = 0;

        float radius;
        int compare = Double.compare(r1, r2);
        if (compare < 0) {
            radius = r1;

            float h_used = 2 * radius + (m - 1) * cos_30 * (2 * radius * cos_30);
            float h_left = h - h_used;
            float paddingY = h_left / 2;
            if (orientation == HexagonOrientation.POINTY_TOP) {
                gridPaddingTop = paddingY;
                this.gridPaddingBottom = paddingY;
            } else {
                gridPaddingLeft = Math.max(0, h_left - this.gridMaxPaddingRight);
            }
        } else if (compare > 0) {
            radius = r2;

            float w_used = (2 * n + 1) * radius * cos_30;
            float w_left = w - w_used;
            float paddingX = w_left / 2;
            if (orientation == HexagonOrientation.POINTY_TOP) {
                gridPaddingLeft = Math.max(0, w_left - this.gridMaxPaddingRight);
            } else {
                gridPaddingTop = paddingX;
                this.gridPaddingBottom = paddingX;
            }
        } else {
            radius = r1;
        }

        this.gridItemOuterRadius = radius;
        this.gridItemRadius = this.gridItemOuterRadius - this.gridItemSpacing / (2 * cos_30);
        this.gridItemInnerRadius = this.gridItemRadius * cos_30;

        float hexagonRectWidth = (orientation == HexagonOrientation.POINTY_TOP
                                  ? this.gridItemInnerRadius
                                  : this.gridItemRadius) * 2 + this.gridItemSpacing;
        float rectTop = gridPaddingTop;
        float rectLeft = this.xPadEnabled //
                         ? 0 //
                         : Math.min(gridPaddingLeft, this.gridMaxPaddingRight);
        float rectHeight = getHeight() - this.gridPaddingBottom;
        float leftRectRight = rectLeft + hexagonRectWidth;
        float rightRectRight = getWidth() - rectLeft;
        float rightRectLeft = rightRectRight //
                              - (orientation == HexagonOrientation.POINTY_TOP
                                 ? hexagonRectWidth * 1.5f
                                 : hexagonRectWidth);
        int middleGridColumns = this.gridColumns - 2;
        float middleRectLeft = this.xPadEnabled //
                               ? leftRectRight //
                               // 靠右对齐
                               : rightRectLeft //
                                 - (orientation == HexagonOrientation.POINTY_TOP
                                    ? hexagonRectWidth
                                    : (this.gridItemRadius * cos_30 * 2 + this.gridItemSpacing) * cos_30)
                                   * middleGridColumns;

        // ======================================================================
        // 将键盘拆分为左、中、右三个部分，左部分的往最左侧靠齐，中和右部分往最右侧靠齐
        List<RectHexagon> rectHexagonList = new ArrayList<>(this.gridColumns * this.gridRows);

        RectF leftRect = new RectF(rectLeft, rectTop, leftRectRight, rectHeight);
        Function<Integer, Integer> leftRectHexagonIndexer = (idx) -> idx * this.gridColumns;
        List<RectHexagon> leftRectHexagonList = RectHexagon.create(leftRect,
                                                                   1,
                                                                   this.gridRows,
                                                                   orientation,
                                                                   this.gridItemOuterRadius,
                                                                   this.gridItemRadius,
                                                                   leftRectHexagonIndexer);
        rectHexagonList.addAll(leftRectHexagonList);

        RectF rightRect = new RectF(rightRectLeft, rectTop, rightRectRight, rectHeight);
        Function<Integer, Integer> rightRectHexagonIndexer = (idx) -> (idx + 1) * this.gridColumns - 1;
        List<RectHexagon> rightRectHexagonList = RectHexagon.create(rightRect,
                                                                    1,
                                                                    this.gridRows,
                                                                    orientation,
                                                                    this.gridItemOuterRadius,
                                                                    this.gridItemRadius,
                                                                    rightRectHexagonIndexer);
        rectHexagonList.addAll(rightRectHexagonList);

        RectF middleRect = new RectF(middleRectLeft, rectTop, rightRect.left, rectHeight);
        Function<Integer, Integer> middleRectHexagonIndexer = (idx) -> idx + (2 * (idx / middleGridColumns) + 1);
        List<RectHexagon> middleRectHexagonList = RectHexagon.create(middleRect,
                                                                     middleGridColumns,
                                                                     this.gridRows,
                                                                     orientation,
                                                                     this.gridItemOuterRadius,
                                                                     this.gridItemRadius,
                                                                     middleRectHexagonIndexer);
        rectHexagonList.addAll(middleRectHexagonList);

        // 必须确保从 0 到 n 的递增顺序排列，以避免视图与绑定的数据映射错误
        rectHexagonList.sort(Comparator.comparingInt(a -> a.index));

        return rectHexagonList;
    }

    public float getGridPaddingBottom() {
        return this.gridPaddingBottom;
    }

    /**
     * 找出指定坐标下的子视图
     * <p/>
     * 探测范围比 {@link #findChildViewUnder} 更大，
     * 但比 {@link #findChildViewNear} 更小
     */
    public View findChildViewUnderLoose(float x, float y) {
        return filterChildViewByHexagonCenterDistance(x, y, distance -> distance < this.gridItemRadius);
    }

    /**
     * 找出指定坐标下的子视图
     * <p/>
     * 仅当坐标在正六边形的内圈中时才视为符合条件
     */
    public View findChildViewUnder(float x, float y) {
        return filterChildViewByHexagonCenterDistance(x, y, distance -> distance < this.gridItemInnerRadius);
    }

    /**
     * 找出靠近指定坐标的子视图
     * <p/>
     * 仅当坐标在正六边形的外圈但不在内圈中时才视为符合条件
     */
    public View findChildViewNear(float x, float y, float deltaInDp) {
        float delta = ScreenUtils.dpToPx(deltaInDp);
        double outer = this.gridItemOuterRadius + delta;

        return filterChildViewByHexagonCenterDistance(x,
                                                      y,
                                                      distance -> distance > this.gridItemInnerRadius
                                                                  && distance < outer);
    }

    private View filterChildViewByHexagonCenterDistance(float x, float y, Predicate<Double> predicate) {
        // X 面板的区域为矩形，只需要坐标点在矩形内即可，不需要判断距离
        if (this.xPadEnabled && this.xPadKeyRectHexagon != null && this.xPadKeyRectHexagon.rect.contains(x, y)) {
            return getChildAt(this.xPadKeyRectHexagon.index);
        }

        Point point = Point.fromPosition(x, y);
        for (RectHexagon rectHexagon : this.rectHexagons) {
            Point center = coord(rectHexagon.hexagon.getCenter(), rectHexagon.rect);
            double distance = center.distanceFrom(point);

            if (predicate.test(distance)) {
                return getChildAt(rectHexagon.index);
            }
        }

        return null;
    }

    private Point coord(Point point, RectF rect) {
        double x = point.getCoordinateX() + rect.left;
        double y = point.getCoordinateY() + rect.top;

        if (this.reversed) {
            return Point.fromPosition(getWidth() - x, y);
        }
        return Point.fromPosition(x, y);
    }

    private void layoutRectHexagons(RecyclerView.Recycler recycler, List<RectHexagon> rectHexagons, int itemCount) {
        View xPadKeyView = null;
        XPadKey xPadKey = XPadKey.build(XPadKey.Builder.noop);
        int xPadKeyViewType = KeyboardViewAdapter.getKeyViewType(xPadKey);

        for (RectHexagon rectHexagon : rectHexagons) {
            int index = rectHexagon.index;
            // Note：实际绑定的按键数可能小于满屏布局的按键数
            if (index >= itemCount) {
                break;
            }

            View view = recycler.getViewForPosition(index);
            addView(view, index);

            if (getItemViewType(view) == xPadKeyViewType) {
                xPadKeyView = view;
                this.xPadKeyRectHexagon = rectHexagon;
                continue;
            }

            layoutRectHexagonView(view, rectHexagon);
        }

        if (xPadKeyView != null) {
            RectF rect = this.xPadKeyRectHexagon.rect;
            int left = (int) rect.left;
            int right = (int) rect.right;
            float radius = this.xPadKeyRectHexagon.radius;
            float minRadius = this.gridItemMinRadius;

            if (radius < minRadius) {
                float scale = radius / minRadius;
                xPadKeyView.setScaleX(scale);
                xPadKeyView.setScaleY(scale);
            }

            XPadView xPad = (new XPadKeyViewHolder(xPadKeyView)).getXPad();
            xPad.setReversed(this.reversed);
            xPad.setCenterHexagonRadius(radius);

            layoutItemView(xPadKeyView,
                           Math.min(left, right),
                           (int) rect.top,
                           Math.max(left, right),
                           (int) rect.bottom);
        }
    }

    private void layoutItemView(View view, int left, int top, int right, int bottom) {
        ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
        layoutParams.width = Math.abs(left - right);
        layoutParams.height = Math.abs(top - bottom);

        measureChildWithMargins(view, 0, 0);
        layoutDecoratedWithMargins(view, left, top, right, bottom);
    }

    private void layoutRectHexagonView(View view, RectHexagon rectHexagon) {
        // 按按键半径调整按键视图的宽高
        RectF rect = rectHexagon.rect;
        float radius = rectHexagon.radius;
        Point center = coord(rectHexagon.hexagon.getCenter(), rect);
        float x = (float) center.getCoordinateX();
        float y = (float) center.getCoordinateY();

        int left = Math.round(x - radius);
        int top = Math.round(y - radius);
        int right = Math.round(x + radius);
        int bottom = Math.round(y + radius);

        float minRadius = this.gridItemMinRadius;
        if (radius < minRadius) {
            float scale = radius / minRadius;

            for (int j = 0; j < ((ViewGroup) view).getChildCount(); j++) {
                View child = ((ViewGroup) view).getChildAt(j);
                if (child.getId() == R.id.bg_view) {
                    continue;
                }

                child.setScaleX(scale);
                child.setScaleY(scale);
            }
        }

        layoutItemView(view, left, top, right, bottom);
    }

    private static class RectHexagon {
        public final int index;
        public final RectF rect;
        public final float radius;
        public final Hexagon<SatelliteData> hexagon;

        private RectHexagon(int index, RectF rect, float radius, Hexagon<SatelliteData> hexagon) {
            this.index = index;
            this.rect = rect;
            this.radius = radius;
            this.hexagon = hexagon;
        }

        public static List<RectHexagon> create(
                RectF rect, int columns, int rows, //
                HexagonOrientation orientation, float outerRadius, float innerRadius, //
                Function<Integer, Integer> indexer
        ) {
            HexagonalGridBuilder<SatelliteData> builder = new HexagonalGridBuilder<>();
            builder.setGridWidth(columns)
                   .setGridHeight(rows)
                   .setGridLayout(HexagonalGridLayout.RECTANGULAR)
                   .setOrientation(orientation)
                   // 包含间隔的半径
                   .setRadius(outerRadius);

            int i = 0;
            List<RectHexagon> list = new ArrayList<>();
            Iterable<Hexagon<SatelliteData>> it = builder.build().getHexagons();
            for (Hexagon<SatelliteData> hexagon : it) {
                int index = indexer.apply(i++);

                list.add(new RectHexagon(index, rect, innerRadius, hexagon));
            }

            return list;
        }
    }
}
