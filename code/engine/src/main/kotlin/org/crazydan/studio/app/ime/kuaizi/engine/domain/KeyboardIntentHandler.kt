package org.crazydan.studio.app.ime.kuaizi.engine.domain

import org.crazydan.studio.app.ime.kuaizi.engine.ImeIntent

interface KeyboardIntentHandler {
    val type: KeyboardType
    fun handleIntent(intent: ImeIntent, currentState: KeyboardState): KeyboardStateTransition
}

class PinyinIntentHandler(override val type: KeyboardType) : KeyboardIntentHandler {
    override fun handleIntent(intent: ImeIntent, currentState: KeyboardState): KeyboardStateTransition {
        return when (intent) {
            is ImeIntent.PressKey -> {
                when {
                    currentState is KeyboardState.PinyinInput.Waiting && intent.key == InputKey.Char -> {
                        KeyboardStateTransition(
                            targetState = KeyboardState.PinyinInput.Waiting,
                        )
                    }
                    currentState is KeyboardState.PinyinInput.Waiting -> {
                        KeyboardStateTransition(
                            targetState = KeyboardState.CandidateSelection.Choosing(),
                            sideEffects = listOf(ImeIntent.LoadCandidates("")),
                        )
                    }
                    else -> KeyboardStateTransition(currentState)
                }
            }
            is ImeIntent.DeleteInput -> {
                KeyboardStateTransition(
                    targetState = KeyboardState.PinyinInput.Waiting,
                )
            }
            else -> KeyboardStateTransition(currentState)
        }
    }
}

class NumberKeyboardIntentHandler(override val type: KeyboardType) : KeyboardIntentHandler {
    override fun handleIntent(intent: ImeIntent, currentState: KeyboardState): KeyboardStateTransition {
        return KeyboardStateTransition(currentState)
    }
}

class SymbolKeyboardIntentHandler(override val type: KeyboardType) : KeyboardIntentHandler {
    override fun handleIntent(intent: ImeIntent, currentState: KeyboardState): KeyboardStateTransition {
        return KeyboardStateTransition(currentState)
    }
}

class EmojiKeyboardIntentHandler(override val type: KeyboardType) : KeyboardIntentHandler {
    override fun handleIntent(intent: ImeIntent, currentState: KeyboardState): KeyboardStateTransition {
        return KeyboardStateTransition(currentState)
    }
}

class MathKeyboardIntentHandler(override val type: KeyboardType) : KeyboardIntentHandler {
    override fun handleIntent(intent: ImeIntent, currentState: KeyboardState): KeyboardStateTransition {
        return KeyboardStateTransition(currentState)
    }
}

class EditorKeyboardIntentHandler(override val type: KeyboardType) : KeyboardIntentHandler {
    override fun handleIntent(intent: ImeIntent, currentState: KeyboardState): KeyboardStateTransition {
        return KeyboardStateTransition(currentState)
    }
}

class CandidateKeyboardIntentHandler(override val type: KeyboardType) : KeyboardIntentHandler {
    override fun handleIntent(intent: ImeIntent, currentState: KeyboardState): KeyboardStateTransition {
        return KeyboardStateTransition(currentState)
    }
}

class CommitOptionKeyboardIntentHandler(override val type: KeyboardType) : KeyboardIntentHandler {
    override fun handleIntent(intent: ImeIntent, currentState: KeyboardState): KeyboardStateTransition {
        return KeyboardStateTransition(currentState)
    }
}
