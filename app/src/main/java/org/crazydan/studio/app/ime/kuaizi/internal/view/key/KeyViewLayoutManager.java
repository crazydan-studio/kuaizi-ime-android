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

import android.graphics.RectF;
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
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-07-01
 */
public class KeyViewLayoutManager extends RecyclerView.LayoutManager {
    private final RectF mPadding = new RectF();

    private double radius;
    private HexagonOrientation mOrientation;
    private HexagonalGrid<SatelliteData> grid;

    public KeyViewLayoutManager(HexagonOrientation orientation) {
        this.mOrientation = orientation;

        this.grid = createHexagonalGrid();
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

        float paddingLeft = ScreenUtils.dpToPx((int) this.mPadding.left);
        float paddingTop = ScreenUtils.dpToPx((int) this.mPadding.top);
        int i = 0;
        for (Hexagon<SatelliteData> hexagon : this.grid.getHexagons()) {
            if (i >= itemCount) {
                break;
            }

            View view = recycler.getViewForPosition(i++);
            addView(view);

            Point center = hexagon.getCenter();
            double x = center.getCoordinateX() + paddingLeft;
            double y = center.getCoordinateY() + paddingTop;
            int left = (int) Math.round(x - this.radius);
            int top = (int) Math.round(y - this.radius);
            int right = (int) Math.round(x + this.radius);
            int bottom = (int) Math.round(y + this.radius);

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

    public void setPadding(float left, float top, float right, float bottom) {
        this.mPadding.set(left, top, right, bottom);
    }

    public View findChildViewUnder(float x, float y) {
        int itemCount = getItemCount();
        float paddingLeft = ScreenUtils.dpToPx((int) this.mPadding.left);
        float paddingTop = ScreenUtils.dpToPx((int) this.mPadding.top);

        int i = 0;
        Point point = Point.fromPosition(x - paddingLeft, y - paddingTop);
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

    private HexagonalGrid<SatelliteData> createHexagonalGrid() {
        int width = 56;
        int spacing = 8;

        this.radius = ScreenUtils.dpToPx(width) / 2f;
        // 相邻六边形中心间距的计算公式见: https://www.redblobgames.com/grids/hexagons/#spacing
        double distanceHalf = this.radius * (Math.sqrt(3) / 2) + ScreenUtils.dpToPx(spacing);
        HexagonalGridBuilder<SatelliteData> builder = new HexagonalGridBuilder<>().setGridWidth(7)
                                                                                  .setGridHeight(6)
                                                                                  .setGridLayout(HexagonalGridLayout.RECTANGULAR)
                                                                                  .setOrientation(this.mOrientation)
                                                                                  // 该半径为相邻六边形的中心间距的一半
                                                                                  .setRadius(distanceHalf);
        return builder.build();
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
