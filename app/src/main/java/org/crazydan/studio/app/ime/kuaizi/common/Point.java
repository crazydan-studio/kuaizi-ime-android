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

package org.crazydan.studio.app.ime.kuaizi.common;

/**
 * 坐标点（只读）
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2025-02-02
 */
public class Point {
    public final float x;
    public final float y;

    public Point(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public Motion motion(Point to) {
        double dx = to.x - this.x;
        double dy = to.y - this.y;
        double distance = Math.hypot(dx, dy);
        double angle = Math.toDegrees(Math.acos(dx / distance));

        Motion.Direction direction;
        // Note: 屏幕绘图坐标与空间坐标存在上下翻转关系
        //  ----- x
        //  |
        //  |
        //  y
        if (angle >= 45 && angle < 45 + 90) {
            direction = dy > 0 ? Motion.Direction.down : Motion.Direction.up;
        } else if (angle >= 45 + 90 && angle <= 180) {
            direction = Motion.Direction.left;
        } else {
            direction = Motion.Direction.right;
        }

        return new Motion(direction, (int) distance);
    }

    @Override
    public String toString() {
        return "{x=" + this.x + ", y=" + this.y + '}';
    }
}
