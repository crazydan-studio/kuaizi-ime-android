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
