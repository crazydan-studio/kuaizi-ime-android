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

import org.crazydan.studio.app.ime.kuaizi.engine.domain.InputKey
import org.crazydan.studio.app.ime.kuaizi.engine.domain.KeyboardType

sealed class InputAction {
    abstract val startTime: Long

    data class KeyDown(
        override val startTime: Long,
        val key: InputKey,
    ) : InputAction()

    data class SwipeTo(
        override val startTime: Long,
        val fromKey: InputKey,
        val toKey: InputKey,
        val duration: Long,
    ) : InputAction()

    data class KeyUp(
        override val startTime: Long,
        val key: InputKey,
    ) : InputAction()

    data class Wait(
        override val startTime: Long,
        val duration: Long,
    ) : InputAction()

    data class SelectCandidate(
        override val startTime: Long,
        val candidateIndex: Int,
    ) : InputAction()

    data class SwitchKeyboard(
        override val startTime: Long,
        val targetType: KeyboardType,
    ) : InputAction()
}
