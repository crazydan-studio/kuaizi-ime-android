package org.crazydan.studio.app.ime.kuaizi.engine.input_action

sealed class InputAction {
    abstract val startTime: Long

    data class KeyDown(
        override val startTime: Long,
        val key: String,
    ) : InputAction()

    data class SwipeTo(
        override val startTime: Long,
        val fromKey: String,
        val toKey: String,
        val duration: Long,
    ) : InputAction()

    data class KeyUp(
        override val startTime: Long,
        val key: String,
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
        val targetType: String,
    ) : InputAction()
}
