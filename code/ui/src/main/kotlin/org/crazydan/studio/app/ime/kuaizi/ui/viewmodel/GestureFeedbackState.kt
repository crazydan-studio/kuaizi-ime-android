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

import kotlinx.coroutines.flow.*
import org.crazydan.studio.app.ime.kuaizi.engine.domain.InputKey
import org.crazydan.studio.app.ime.kuaizi.engine.input_action.InputActionFingerIndicator
import org.crazydan.studio.app.ime.kuaizi.engine.input_action.OffsetF

class GestureFeedbackState {
    private val _touchTrailPoints = MutableStateFlow<List<OffsetF>>(emptyList())
    val touchTrailPoints: StateFlow<List<OffsetF>> = _touchTrailPoints.asStateFlow()

    private val _pressedKeys = MutableStateFlow<Set<InputKey>>(emptySet())
    val pressedKeys: StateFlow<Set<InputKey>> = _pressedKeys.asStateFlow()

    private val _fingerIndicator = MutableStateFlow<InputActionFingerIndicator?>(null)
    val fingerIndicator: StateFlow<InputActionFingerIndicator?> = _fingerIndicator.asStateFlow()

    fun addTouchTrailPoint(normalizedPoint: OffsetF) {
        _touchTrailPoints.update { list ->
            val mutable = list.toMutableList()
            mutable.add(normalizedPoint)
            mutable
        }
    }

    fun setTouchTrailPoints(points: List<OffsetF>) {
        _touchTrailPoints.value = points
    }

    fun clearTouchTrail() {
        _touchTrailPoints.value = emptyList()
    }

    fun setPressedKeys(keys: Set<InputKey>) {
        _pressedKeys.value = keys
    }

    fun clearPressedKeys() {
        _pressedKeys.value = emptySet()
    }

    fun setFingerIndicator(state: InputActionFingerIndicator?) {
        _fingerIndicator.value = state
    }

    fun clearAll() {
        clearTouchTrail()
        clearPressedKeys()
    }

    fun clear() {
        clearAll()
        _fingerIndicator.value = null
    }
}
