package org.crazydan.studio.app.ime.kuaizi.engine.domain

import org.crazydan.studio.app.ime.kuaizi.engine.ImeIntent

class KeyboardStateHistory(private val maxSize: Int = 10) {
    private val stack = ArrayDeque<KeyboardState>(maxSize)

    fun push(state: KeyboardState) {
        if (stack.size >= maxSize) {
            stack.removeFirst()
        }
        stack.addLast(state)
    }

    fun pop(): KeyboardState? = stack.removeLastOrNull()

    fun clear() {
        stack.clear()
    }
}

class KeyboardStateMachine {
    private var _state: KeyboardState = KeyboardState.Idle
    val state: KeyboardState get() = _state

    private val history = KeyboardStateHistory()

    fun transition(transition: KeyboardStateTransition): KeyboardStateTransitionResult {
        val oldState = _state
        val newState = transition.targetState

        if (newState != oldState) {
            history.push(oldState)
        }

        _state = newState

        return KeyboardStateTransitionResult(
            newState = newState,
            sideEffects = transition.sideEffects,
        )
    }

    fun resetTo(state: KeyboardState) {
        history.clear()
        _state = state
    }

    fun backToPrevious(): Boolean {
        val previous = history.pop() ?: return false
        _state = previous
        return true
    }
}
