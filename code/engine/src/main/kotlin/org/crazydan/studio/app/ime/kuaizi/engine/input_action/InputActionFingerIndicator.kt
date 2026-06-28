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

/**
 * 手指指示器：为回放过程中的手指位置提供直观的视觉反馈。
 *
 * 显示一个虚拟手指在键盘面板上的移动轨迹，帮助用户理解动作脚本的执行过程
 * 和输入节奏。所有坐标使用归一化坐标系，与设备屏幕密度无关。
 *
 * @property position 指示器在面板归一化坐标系中的当前位置
 * @property pressed 手指的按压状态，影响视觉大小和透明度
 * @property visible 控制指示器的可见性
 * @property clickAnimation 按键点击的微交互动画
 */
data class InputActionFingerIndicator(
    val position: OffsetF = OffsetF.Zero,
    val pressed: Boolean = false,
    val visible: Boolean = false,
    val clickAnimation: ClickAnimation = ClickAnimation.None,
) {
    /** 点击动画类型。 */
    enum class ClickAnimation {
        /** 无动画。 */
        None,
        /** 按下动画：放大 + 透明度降低。 */
        Pressing,
        /** 抬起动画：缩小 + 透明度恢复。 */
        Releasing,
    }
}

/**
 * 二维归一化点坐标，值域为 [0, 1]。
 *
 * 归一化坐标将面板的宽度和高度映射到 [0, 1] 区间：
 * - x = 0 为面板左边缘，x = 1 为面板右边缘
 * - y = 0 为面板顶边缘，y = 1 为面板底边缘
 *
 * 到屏幕像素坐标的转换由 UI 层在渲染时完成：screenX = x * panelWidth。
 */
data class OffsetF(
    val x: Float = 0f,
    val y: Float = 0f,
) {
    companion object {
        val Zero = OffsetF(0.0f, 0.0f)
    }
}

/**
 * 二维归一化矩形，表达面板上某个区域的边界。
 *
 * @property left 左边界
 * @property top 上边界
 * @property right 右边界
 * @property bottom 下边界
 */
data class RectF(
    val left: Float = 0f,
    val top: Float = 0f,
    val right: Float = 0f,
    val bottom: Float = 0f,
) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top
    val centerX: Float get() = (left + right) / 2.0f
    val centerY: Float get() = (top + bottom) / 2.0f
    val center: OffsetF get() = OffsetF(centerX, centerY)

    /** 判断一个点是否落在矩形内部。 */
    fun contains(offset: OffsetF): Boolean =
        offset.x in left..right && offset.y in top..bottom

    companion object {
        val Zero = RectF(0.0f, 0.0f, 0.0f, 0.0f)
    }
}
