package org.crazydan.studio.app.ime.kuaizi.engine.domain

import org.crazydan.studio.app.ime.kuaizi.engine.ImeIntent

interface KeyboardIntentHandler {
    fun handleIntent(intent: ImeIntent, currentState: KeyboardState): KeyboardStateTransition
}

class PinyinIntentHandler : KeyboardIntentHandler {
    override fun handleIntent(intent: ImeIntent, currentState: KeyboardState): KeyboardStateTransition {
        return when (intent) {
            is ImeIntent.PressKey -> {
                when (currentState) {
                    is KeyboardState.PinyinInput.Waiting -> {
                        KeyboardStateTransition(
                            targetState = KeyboardState.PinyinInput.Waiting,
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

class NumberKeyboardIntentHandler : KeyboardIntentHandler {
    override fun handleIntent(intent: ImeIntent, currentState: KeyboardState): KeyboardStateTransition {
        return KeyboardStateTransition(currentState)
    }
}

class SymbolKeyboardIntentHandler : KeyboardIntentHandler {
    override fun handleIntent(intent: ImeIntent, currentState: KeyboardState): KeyboardStateTransition {
        return KeyboardStateTransition(currentState)
    }
}

class EmojiKeyboardIntentHandler : KeyboardIntentHandler {
    override fun handleIntent(intent: ImeIntent, currentState: KeyboardState): KeyboardStateTransition {
        return KeyboardStateTransition(currentState)
    }
}

class MathKeyboardIntentHandler : KeyboardIntentHandler {
    override fun handleIntent(intent: ImeIntent, currentState: KeyboardState): KeyboardStateTransition {
        return KeyboardStateTransition(currentState)
    }
}

class EditorKeyboardIntentHandler : KeyboardIntentHandler {
    override fun handleIntent(intent: ImeIntent, currentState: KeyboardState): KeyboardStateTransition {
        return KeyboardStateTransition(currentState)
    }
}

class CandidateKeyboardIntentHandler : KeyboardIntentHandler {
    override fun handleIntent(intent: ImeIntent, currentState: KeyboardState): KeyboardStateTransition {
        return KeyboardStateTransition(currentState)
    }
}

class CommitOptionKeyboardIntentHandler : KeyboardIntentHandler {
    override fun handleIntent(intent: ImeIntent, currentState: KeyboardState): KeyboardStateTransition {
        return KeyboardStateTransition(currentState)
    }
}
