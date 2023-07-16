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

package org.crazydan.studio.app.ime.kuaizi.internal.view.key;

import java.util.function.Predicate;

import android.view.View;
import android.view.ViewGroup;
import androidx.recyclerview.widget.RecyclerView;
import org.crazydan.studio.app.ime.kuaizi.internal.Key;
import org.crazydan.studio.app.ime.kuaizi.internal.Keyboard;
import org.crazydan.studio.app.ime.kuaizi.internal.view.RecyclerViewLayoutManager;
import org.crazydan.studio.app.ime.kuaizi.utils.ScreenUtils;
import org.hexworks.mixite.core.api.Hexagon;
import org.hexworks.mixite.core.api.HexagonOrientation;
import org.hexworks.mixite.core.api.HexagonalGrid;
import org.hexworks.mixite.core.api.HexagonalGridBuilder;
import org.hexworks.mixite.core.api.HexagonalGridLayout;
import org.hexworks.mixite.core.api.Point;
import org.hexworks.mixite.core.api.contract.SatelliteData;

/**
 * {@link Keyboard 键盘}{@link Key 按键}的{@link RecyclerView}布局器
 * <p/>
 * 注意：<ul>
 * <li>通过按键布局视图的 margin 设置按键与父视图的间隙，
 * 从而降低通过 padding 设置间隙所造成的计算复杂度；</li>
 * </ul>
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-07-01
 */
public class KeyViewLayoutManager extends RecyclerViewLayoutManager {
    private static final double cos_30 = Math.cos(Math.toRadians(30));

    /** 按键正六边形方向 */
    private final HexagonOrientation gridItemOrientation;

    /** 按键正六边形半径：可见范围内的半径 */
    private double gridItemRadius;
    /** 按键正六边形内部边界半径：与可见边相交的圆 */
    private double gridItemInnerRadius;
    /** 按键正六边形外部边界半径：含按键间隔，并与顶点相交的圆 */
    private double gridItemOuterRadius;
    /** 按键间隔 */
    private double gridItemSpacing;
    /** 按键列数 */
    private int gridColumns;
    /** 按键行数 */
    private int gridRows;
    /** 网格左部空白 */
    private double gridPaddingLeft;
    /** 网格顶部空白 */
    private double gridPaddingTop;

    private HexagonalGrid<SatelliteData> grid;

    public KeyViewLayoutManager(HexagonOrientation gridItemOrientation) {
        this.gridItemOrientation = gridItemOrientation;
    }

    public void configGrid(int columns, int rows, int itemSpacingInDp) {
        this.gridColumns = columns;
        this.gridRows = rows;
        this.gridItemSpacing = ScreenUtils.dpToPx(itemSpacingInDp);
    }

    @Override
    public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
        detachAndScrapAttachedViews(recycler);

        int itemCount = state.getItemCount();
        if (itemCount <= 0 || this.gridColumns <= 0 || this.gridRows <= 0) {
            return;
        }

        this.grid = createGrid();

        int i = 0;
        double radius = this.gridItemRadius;
        for (Hexagon<SatelliteData> hexagon : this.grid.getHexagons()) {
            if (i >= itemCount) {
                break;
            }

            View view = recycler.getViewForPosition(i++);
            Point center = hexagon.getCenter();

            double x = center.getCoordinateX() + this.gridPaddingLeft;
            double y = center.getCoordinateY() + this.gridPaddingTop;
            int left = (int) Math.round(x - radius);
            int top = (int) Math.round(y - radius);
            int right = (int) Math.round(x + radius);
            int bottom = (int) Math.round(y + radius);

            // 按按键半径调整按键视图的宽高
            ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
            layoutParams.height = layoutParams.width = (int) Math.round(radius * 2);

            addView(view);
            measureChildWithMargins(view, 0, 0);
            layoutDecoratedWithMargins(view, left, top, right, bottom);
        }
    }

    private HexagonalGrid<SatelliteData> createGrid() {
        // Note: 只有布局完成后，才能得到视图的宽高
        // 按键按照行列数自动适配屏幕尺寸（以 POINTY_TOP 方向为例，FLAT_TOP 方向为 POINTY_TOP 方向的 xy 轴交换）
        // - 这里按照按键紧挨的情况计算正六边形的半径，
        //   再在绘制时按照按键间隔计算正六边形的实际绘制半径
        // - 相邻六边形中心间距的计算公式见: https://www.redblobgames.com/grids/hexagons/#spacing
        // - 横向半径（r1）与宽度（w）的关系: w = (2 * n + 1) * r1 * cos30
        // - 纵向半径（r2）与高度（h）的关系: h = 2 * r2 + (m - 1) * cos30 * (2 * r2 * cos30)
        // - 最终正六边形的半径: radius = Math.min(r1, r2)
        // - 按键实际绘制半径: R = radius - spacing / (2 * cos30)
        int w = this.gridItemOrientation == HexagonOrientation.POINTY_TOP ? getWidth() : getHeight();
        int h = this.gridItemOrientation == HexagonOrientation.POINTY_TOP ? getHeight() : getWidth();
        int n = this.gridItemOrientation == HexagonOrientation.POINTY_TOP ? this.gridColumns : this.gridRows;
        int m = this.gridItemOrientation == HexagonOrientation.POINTY_TOP ? this.gridRows : this.gridColumns;
        double r1 = w / ((2 * n + 1) * cos_30);
        double r2 = h / (2 * ((m - 1) * cos_30 * cos_30 + 1));
        double radius;
        int compare = Double.compare(r1, r2);

        // 计算左上角的空白偏移量
        this.gridPaddingLeft = this.gridPaddingTop = 0;
        if (compare < 0) {
            radius = r1;

            double h_used = 2 * radius + (m - 1) * cos_30 * (2 * radius * cos_30);
            double paddingY = (h - h_used) / 2;
            if (this.gridItemOrientation == HexagonOrientation.POINTY_TOP) {
                this.gridPaddingTop = paddingY;
            } else {
                this.gridPaddingLeft = paddingY;
            }
        } else if (compare > 0) {
            radius = r2;

            double w_used = (2 * n + 1) * radius * cos_30;
            double paddingX = (w - w_used) / 2;
            if (this.gridItemOrientation == HexagonOrientation.POINTY_TOP) {
                this.gridPaddingLeft = paddingX;
            } else {
                this.gridPaddingTop = paddingX;
            }
        } else {
            radius = r1;
        }

        this.gridItemRadius = radius - this.gridItemSpacing / (2 * cos_30);
        this.gridItemOuterRadius = radius;
        this.gridItemInnerRadius = this.gridItemRadius * cos_30;

        HexagonalGridBuilder<SatelliteData> builder = new HexagonalGridBuilder<>();
        builder.setGridWidth(this.gridColumns)
               .setGridHeight(this.gridRows)
               .setGridLayout(HexagonalGridLayout.RECTANGULAR)
               .setOrientation(this.gridItemOrientation)
               .setRadius(radius);

        return builder.build();
    }

    /**
     * 找出指定坐标下的子视图
     * <p/>
     * 探测范围比 {@link #findChildViewUnder} 更大，
     * 但比 {@link #findChildViewNear} 更小
     */
    public View findChildViewUnderLoose(double x, double y) {
        return filterChildViewByHexagonCenterDistance(x, y, distance -> distance < this.gridItemRadius);
    }

    /**
     * 找出指定坐标下的子视图
     * <p/>
     * 仅当坐标在正六边形的内圈中时才视为符合条件
     */
    public View findChildViewUnder(double x, double y) {
        return filterChildViewByHexagonCenterDistance(x, y, distance -> distance < this.gridItemInnerRadius);
    }

    /**
     * 找出靠近指定坐标的子视图
     * <p/>
     * 仅当坐标在正六边形的外圈但不在内圈中时才视为符合条件
     */
    public View findChildViewNear(double x, double y, int deltaInDp) {
        int delta = ScreenUtils.dpToPx(deltaInDp);
        double outer = this.gridItemOuterRadius + delta;

        return filterChildViewByHexagonCenterDistance(x,
                                                      y,
                                                      distance -> distance > this.gridItemInnerRadius
                                                                  && distance < outer);
    }

    private View filterChildViewByHexagonCenterDistance(double x, double y, Predicate<Double> predicate) {
        int itemCount = getItemCount();

        int i = 0;
        Point point = Point.fromPosition(x - this.gridPaddingLeft, y - this.gridPaddingTop);
        for (Hexagon<SatelliteData> hexagon : this.grid.getHexagons()) {
            if (i >= itemCount) {
                break;
            }

            Point center = hexagon.getCenter();
            double distance = center.distanceFrom(point);

            if (predicate.test(distance)) {
                return getChildAt(i);
            }
            i++;
        }

        return null;
    }
}
