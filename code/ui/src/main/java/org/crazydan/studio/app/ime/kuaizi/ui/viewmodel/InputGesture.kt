package org.crazydan.studio.app.ime.kuaizi.ui.viewmodel

import org.crazydan.studio.app.ime.kuaizi.engine.domain.*

sealed class InputGesture {
    abstract val timestamp: Long
    abstract val inputMode: KeyboardInputMode

    data class Tap(
        override val timestamp: Long,
        override val inputMode: KeyboardInputMode,
        val key: InputKey,
        val tick: Int = 0,
    ) : InputGesture()

    data class LongPress(
        override val timestamp: Long,
        override val inputMode: KeyboardInputMode,
        val key: InputKey,
    ) : InputGesture()

    data class Swipe(
        override val timestamp: Long,
        override val inputMode: KeyboardInputMode,
        val startKey: InputKey,
        val endKey: InputKey,
        val visitedKeys: List<InputKey>,
        val duration: Long,
    ) : InputGesture()

    data class Flip(
        override val timestamp: Long,
        override val inputMode: KeyboardInputMode,
        val startKey: InputKey,
        val direction: FlipDirection,
    ) : InputGesture()

    data class CandidateTap(
        override val timestamp: Long,
        override val inputMode: KeyboardInputMode,
        val candidateIndex: Int,
    ) : InputGesture()
}
