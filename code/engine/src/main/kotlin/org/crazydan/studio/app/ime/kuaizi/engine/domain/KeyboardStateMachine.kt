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
        val currentState = _state
        val targetState = transition.targetState

        val newState = when (currentState) {
            is KeyboardState.Idle -> {
                when (targetState) {
                    is KeyboardState.PinyinInput,
                    is KeyboardState.SymbolChoosing,
                    is KeyboardState.EmojiChoosing,
                    is KeyboardState.EditorEditing -> targetState
                    else -> currentState
                }
            }
            is KeyboardState.PinyinInput.Waiting -> {
                when (targetState) {
                    is KeyboardState.PinyinInput.Waiting,
                    is KeyboardState.PinyinInput.Slipping,
                    is KeyboardState.PinyinInput.Flipping,
                    is KeyboardState.CandidateSelection -> targetState
                    else -> currentState
                }
            }
            is KeyboardState.PinyinInput.Slipping -> {
                when (targetState) {
                    is KeyboardState.PinyinInput.Waiting,
                    is KeyboardState.CandidateSelection -> targetState
                    else -> currentState
                }
            }
            is KeyboardState.PinyinInput.Flipping -> {
                when (targetState) {
                    is KeyboardState.PinyinInput.Waiting,
                    is KeyboardState.CandidateSelection -> targetState
                    else -> currentState
                }
            }
            is KeyboardState.CandidateSelection -> {
                when (targetState) {
                    is KeyboardState.PinyinInput,
                    is KeyboardState.CommitOptionChoosing -> targetState
                    else -> currentState
                }
            }
            is KeyboardState.CommitOptionChoosing -> {
                when (targetState) {
                    is KeyboardState.Idle,
                    is KeyboardState.PinyinInput.Waiting -> targetState
                    else -> currentState
                }
            }
            is KeyboardState.EditorEditing -> {
                when (targetState) {
                    is KeyboardState.Idle -> targetState
                    else -> currentState
                }
            }
            is KeyboardState.SymbolChoosing -> {
                when (targetState) {
                    is KeyboardState.Idle -> targetState
                    else -> currentState
                }
            }
            is KeyboardState.EmojiChoosing -> {
                when (targetState) {
                    is KeyboardState.Idle -> targetState
                    else -> currentState
                }
            }
        }

        if (newState != currentState) {
            history.push(currentState)
        }

        _state = newState

        return KeyboardStateTransitionResult(
            newState = newState,
            sideEffects = transition.sideEffects,
            editorAction = transition.editorAction,
        )
    }

    fun resetTo(state: KeyboardState) {
        history.clear()
        _state = state
    }

    fun resetToIdle() {
        resetTo(KeyboardState.Idle)
    }

    fun backToPrevious() {
        val previous = history.pop() ?: return
        _state = previous
    }
}
