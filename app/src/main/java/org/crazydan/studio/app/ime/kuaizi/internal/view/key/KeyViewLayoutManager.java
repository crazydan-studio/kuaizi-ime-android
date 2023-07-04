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

import java.util.List;

import android.view.View;
import android.view.ViewGroup;
import androidx.recyclerview.widget.RecyclerView;
import org.crazydan.studio.app.ime.kuaizi.utils.ScreenUtils;
import org.hexworks.mixite.core.api.Hexagon;
import org.hexworks.mixite.core.api.HexagonOrientation;
import org.hexworks.mixite.core.api.HexagonalGrid;
import org.hexworks.mixite.core.api.HexagonalGridBuilder;
import org.hexworks.mixite.core.api.HexagonalGridLayout;
import org.hexworks.mixite.core.api.Point;
import org.hexworks.mixite.core.api.contract.SatelliteData;

/**
 * 注意：<ul>
 * <li>通过按键布局视图的 margin 设置按键与父视图的间隙，
 * 从而降低通过 padding 设置间隙所造成的计算复杂度；</li>
 * </ul>
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-07-01
 */
public class KeyViewLayoutManager extends RecyclerView.LayoutManager {
    /** 按键正六边形方向 */
    private final HexagonOrientation orientation;

    /** 按键正六边形半径 */
    private double itemRadius;
    /** 按键间隔 */
    private double itemSpacing;
    /** 按键列数 */
    private int gridColumns;
    /** 按键行数 */
    private int gridRows;
    /** 网格左部空白 */
    private double gridPaddingLeft;
    /** 网格顶部空白 */
    private double gridPaddingTop;

    private HexagonalGrid<SatelliteData> grid;

    public KeyViewLayoutManager(HexagonOrientation orientation) {
        this.orientation = orientation;
    }

    @Override
    public RecyclerView.LayoutParams generateDefaultLayoutParams() {
        return new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
    }

    @Override
    public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
        detachAndScrapAttachedViews(recycler);

        int itemCount = state.getItemCount();
        if (itemCount <= 0) {
            return;
        }

        this.grid = createGrid();

        int i = 0;
        for (Hexagon<SatelliteData> hexagon : this.grid.getHexagons()) {
            if (i >= itemCount) {
                break;
            }

            View view = recycler.getViewForPosition(i++);
            Point center = hexagon.getCenter();

            double x = center.getCoordinateX() + this.gridPaddingLeft;
            double y = center.getCoordinateY() + this.gridPaddingTop;
            int left = (int) Math.round(x - this.itemRadius);
            int top = (int) Math.round(y - this.itemRadius);
            int right = (int) Math.round(x + this.itemRadius);
            int bottom = (int) Math.round(y + this.itemRadius);

            // 按按键半径调整按键视图的宽高
            ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
            layoutParams.height = layoutParams.width = (int) Math.round(this.itemRadius * 2);

            addView(view);
            measureChildWithMargins(view, 0, 0);
            layoutDecoratedWithMargins(view, left, top, right, bottom);
        }
    }

    @Override
    public boolean canScrollHorizontally() {
        return false;
    }

    @Override
    public boolean canScrollVertically() {
        return false;
    }

    public void configGrid(int gridColumns, int gridRows, int itemSpacing) {
        this.gridColumns = gridColumns;
        this.gridRows = gridRows;
        this.itemSpacing = ScreenUtils.dpToPx(itemSpacing);
    }

    private HexagonalGrid<SatelliteData> createGrid() {
        double cos_30 = Math.cos(Math.toRadians(30));
        double cos_30_pow2 = cos_30 * cos_30;

        // Note: 只有布局完成后，才能得到视图的宽高
        // 按键按照行列数自动适配屏幕尺寸（以 POINTY_TOP 方向为例，FLAT_TOP 方向为 POINTY_TOP 方向的 xy 轴交换）
        // - 横向半径（r1）与宽度（w）的关系: w = n * spacing + (2 * n + 1) * r1 * cos30
        // - 纵向半径（r2）与高度（h）的关系: h = 2 * r2 + (m - 1) * cos30 * (spacing + 2 * r2 * cos30)
        // - 最终按键半径: radius = Math.min(r1, r2)
        int w = this.orientation == HexagonOrientation.POINTY_TOP ? getWidth() : getHeight();
        int h = this.orientation == HexagonOrientation.POINTY_TOP ? getHeight() : getWidth();
        int n = this.orientation == HexagonOrientation.POINTY_TOP ? this.gridColumns : this.gridRows;
        int m = this.orientation == HexagonOrientation.POINTY_TOP ? this.gridRows : this.gridColumns;
        double spacing = this.itemSpacing;
        double r1 = (w - n * spacing) / ((2 * n + 1) * cos_30);
        double r2 = (h - (m - 1) * spacing * cos_30) / (2 * ((m - 1) * cos_30_pow2 + 1));
        double radius;
        int compare = Double.compare(r1, r2);

        // 计算左上角的空白偏移量
        if (compare < 0) {
            this.itemRadius = radius = r1;

            double h_used = 2 * radius + (m - 1) * cos_30 * (spacing + 2 * radius * cos_30);
            double paddingY = (h - h_used) / 2;
            if (this.orientation == HexagonOrientation.POINTY_TOP) {
                this.gridPaddingLeft = 0;
                this.gridPaddingTop = paddingY;
            } else {
                this.gridPaddingLeft = paddingY;
                this.gridPaddingTop = 0;
            }
        } else if (compare > 0) {
            this.itemRadius = radius = r2;

            double w_used = n * spacing + (2 * n + 1) * radius * cos_30;
            double paddingX = (w - w_used) / 2;
            if (this.orientation == HexagonOrientation.POINTY_TOP) {
                this.gridPaddingLeft = paddingX;
                this.gridPaddingTop = 0;
            } else {
                this.gridPaddingLeft = 0;
                this.gridPaddingTop = paddingX;
            }
        } else {
            this.itemRadius = radius = r1;
            this.gridPaddingLeft = this.gridPaddingTop = 0;
        }

        // TODO 1. 高度为 200dp 时，纵向按键显示不全；2. 纵向按键存在被截取的情况；
        // 相邻六边形中心间距的计算公式见: https://www.redblobgames.com/grids/hexagons/#spacing
        double distanceHalf = radius * cos_30 + spacing;
        HexagonalGridBuilder<SatelliteData> builder = new HexagonalGridBuilder<>().setGridWidth(this.gridColumns)
                                                                                  .setGridHeight(this.gridRows)
                                                                                  .setGridLayout(HexagonalGridLayout.RECTANGULAR)
                                                                                  .setOrientation(this.orientation)
                                                                                  // 该半径为相邻六边形的中心间距的一半
                                                                                  .setRadius(distanceHalf);
        return builder.build();
    }

    public View findChildViewUnder(float x, float y) {
        int itemCount = getItemCount();

        int i = 0;
        Point point = Point.fromPosition(x - this.gridPaddingLeft, y - this.gridPaddingTop);
        for (Hexagon<SatelliteData> hexagon : this.grid.getHexagons()) {
            if (i >= itemCount) {
                break;
            }

            List<Point> points = hexagon.getPoints();
            if (isInside(points, point)) {
                return getChildAt(i);
            }

            i++;
        }

        return null;
    }

    private boolean isInside(List<Point> points, Point point) {
        // 通过射线法判断: https://xoyozo.net/Blog/Details/is-the-point-inside-the-polygon
        int intersections = 0;
        for (int i = 0; i < points.size(); i++) {
            Point p1 = points.get(i);
            Point p2 = points.get((i + 1) % points.size());
            double px = point.getCoordinateX();
            double py = point.getCoordinateY();
            double p1x = p1.getCoordinateX();
            double p1y = p1.getCoordinateY();
            double p2x = p2.getCoordinateX();
            double p2y = p2.getCoordinateY();

            if (py > Math.min(p1y, p2y)
                //
                && py <= Math.max(p1y, p2y)
                //
                && px <= Math.max(p1x, p2x)
                //
                && p1y != p2y) {
                double xIntersection = (py - p1y) * (p2x - p1x) / (p2y - p1y) + p1x;

                if (p1x == p2x || px <= xIntersection) {
                    intersections++;
                }
            }
        }
        return intersections % 2 != 0;
    }
}
