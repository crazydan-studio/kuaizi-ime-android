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

package org.crazydan.studio.app.ime.kuaizi.engine.input_action

import kotlin.math.sqrt

/**
 * 输入动作路径插值器：为 [InputAction.SwipeTo] 动作提供平滑的中间路径点计算。
 *
 * 采用二次 Bézier 曲线算法，以起点和终点为曲线的两端控制点，以起点和终点连线的
 * 中垂线偏移点为中间控制点，生成自然弯曲的滑行轨迹。
 *
 * 纯算法组件，不持有任何可变状态——输入起点、终点和进度参数，输出插值位置，
 * 完全确定性的纯函数行为。
 */
object InputActionPathInterpolator {

    /**
     * 二次 Bézier 曲线插值。
     *
     * 参数方程：B(t) = (1-t)²·P0 + 2(1-t)t·P1 + t²·P2
     *
     * @param from 起点坐标
     * @param to 终点坐标
     * @param t 插值进度 [0.0, 1.0]，0 为起点，1 为终点
     * @return 插值位置
     */
    fun interpolate(from: OffsetF, to: OffsetF, t: Float): OffsetF {
        val control = computeControlPoint(from, to)
        val oneMinusT = 1.0f - t
        val x = oneMinusT * oneMinusT * from.x + 2.0f * oneMinusT * t * control.x + t * t * to.x
        val y = oneMinusT * oneMinusT * from.y + 2.0f * oneMinusT * t * control.y + t * t * to.y
        return OffsetF(x, y)
    }

    /**
     * 计算二次 Bézier 曲线的中间控制点。
     * 控制点位于起点-终点连线的中垂线上，向连线法线方向偏移。
     * 偏移量与连线长度成正比，模拟自然滑行时的弧线轨迹。
     */
    private fun computeControlPoint(from: OffsetF, to: OffsetF): OffsetF {
        val midX = (from.x + to.x) / 2.0f
        val midY = (from.y + to.y) / 2.0f
        val dx = to.x - from.x
        val dy = to.y - from.y
        val distance = sqrt((dx * dx + dy * dy).toDouble()).toFloat()
        // 当距离极小时，控制点直接取中点，避免除以零
        if (distance < 0.001f) return OffsetF(midX, midY)
        val curvature = distance * CURVATURE_FACTOR
        // 法线方向（逆时针旋转 90 度）
        val nx = -dy / distance
        val ny = dx / distance
        return OffsetF(midX + nx * curvature, midY + ny * curvature)
    }

    // 曲率因子：控制点偏移量为连线长度的 15%，产生轻微但可感知的弧形轨迹
    private const val CURVATURE_FACTOR = 0.15f
}
