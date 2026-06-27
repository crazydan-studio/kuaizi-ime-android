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
