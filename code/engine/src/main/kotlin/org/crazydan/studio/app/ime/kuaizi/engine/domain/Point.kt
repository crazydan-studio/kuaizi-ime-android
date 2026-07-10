/*
 * 筷字输入法 - 高效编辑需要又好又快的输入法
 * Copyright (C) 2026 Crazydan Studio <https://studio.crazydan.org>
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

package org.crazydan.studio.app.ime.kuaizi.engine.domain

import kotlin.math.acos
import kotlin.math.hypot

/** 坐标点。 */
data class Point(
    val x: Float,
    val y: Float,
) {

    fun motion(to: Point): Motion? {
        val dx = (to.x - this.x).toDouble()
        val dy = (to.y - this.y).toDouble()
        val distance = hypot(dx, dy)
        val angle = Math.toDegrees(acos(dx / distance))

        // Note: 屏幕绘图坐标与空间坐标存在上下翻转关系
        //  ----- x
        //  |
        //  |
        //  y
        val direction =
            if (angle >= 45 && angle < 45 + 90) {
                if (dy > 0)
                    Motion.Direction.Down
                else
                    Motion.Direction.Up
            } else if (angle >= 45 + 90 && angle <= 180) {
                Motion.Direction.Left
            } else {
                Motion.Direction.Right
            }

        return Motion(direction, distance.toFloat())
    }
}