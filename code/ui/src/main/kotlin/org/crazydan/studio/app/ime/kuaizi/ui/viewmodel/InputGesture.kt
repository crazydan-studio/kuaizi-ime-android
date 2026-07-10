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

package org.crazydan.studio.app.ime.kuaizi.ui.viewmodel

import org.crazydan.studio.app.ime.kuaizi.engine.domain.Motion
import org.crazydan.studio.app.ime.kuaizi.engine.keyboard.InputKey

/**
 * 输入手势，坐标无关。
 *
 * 由输入面板识别后发送到 ViewModel，描述用户的输入意图。
 * 不包含任何绝对坐标，只包含按键的语义标识。
 */
sealed class InputGesture {
    /** 手势发生时所在的按键，若其为 `null`，则表示无相关按键 */
    abstract val key: InputKey?

    // ----------------------------------------------

    /**
     * 按压
     * @param released 是否已释放
     */
    data class Press(
        override val key: InputKey? = null,

        val released: Boolean = false,
    ) : InputGesture()

    /**
     * 长按
     * @param tick 停留的滴答次数
     * @param released 是否已释放
     */
    data class LongPress(
        override val key: InputKey? = null,

        val tick: Int = 0,
        val released: Boolean = false,
    ) : InputGesture()

    /**
     * 点击
     * @param tick 连击次数（0=首次，1=双击...）
     */
    data class Tap(
        override val key: InputKey? = null,

        val tick: Int = 0,
    ) : InputGesture()

    // ----------------------------------------------

    /**
     * 滑行
     * @param motion 运动数据。[Motion.distance] 为实际滑行像素数与 [ImeConfig.Ui.scaledTouchSlop] 的比值
     * @param tick 停留的滴答次数
     * @param released 是否已释放
     */
    data class Swipe(
        override val key: InputKey? = null,

        val motion: Motion? = null,
        val tick: Int = 0,
        val released: Boolean = false,
    ) : InputGesture()

    /**
     * 翻动（快速滑行后松手）
     * @param motion 运动数据。[Motion.distance] 为实际滑行像素数与 [ImeConfig.Ui.scaledTouchSlop] 的比值
     */
    data class Flip(
        override val key: InputKey? = null,

        val motion: Motion? = null,
    ) : InputGesture()
}
