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

data class InputActionFingerIndicator(
    val position: OffsetF = OffsetF.Zero,
    val pressed: Boolean = false,
    val visible: Boolean = false,
    val clickAnimation: ClickAnimation = ClickAnimation.None,
) {
    enum class ClickAnimation {
        None,
        Pressing,
        Releasing,
    }
}

data class OffsetF(
    val x: Float = 0f,
    val y: Float = 0f,
) {
    companion object {
        val Zero = OffsetF(0.0f, 0.0f)
    }
}

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

    fun contains(offset: OffsetF): Boolean =
        offset.x in left..right && offset.y in top..bottom

    companion object {
        val Zero = RectF(0.0f, 0.0f, 0.0f, 0.0f)
    }
}
