package org.crazydan.studio.app.ime.kuaizi.engine.domain

import org.crazydan.studio.app.ime.kuaizi.engine.ImeIntent

data class KeyboardStateTransition(
    val targetState: KeyboardState,
    val sideEffects: List<ImeIntent> = emptyList(),
)

data class KeyboardStateTransitionResult(
    val newState: KeyboardState,
    val sideEffects: List<ImeIntent> = emptyList(),
)
