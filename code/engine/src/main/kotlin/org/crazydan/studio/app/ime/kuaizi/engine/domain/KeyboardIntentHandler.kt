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
                    currentState is KeyboardState.PinyinInput.Waiting && intent.key is InputKey.Char -> {
                        KeyboardStateTransition.InputPinyinChar(' ')
                    }
                    currentState is KeyboardState.PinyinInput.Waiting -> {
                        KeyboardStateTransition.LoadCandidates(emptyList())
                    }
                    else -> KeyboardStateTransition.ReturnToIdle
                }
            }
            is ImeIntent.DeleteInput -> {
                KeyboardStateTransition.ReturnToIdle
            }
            else -> KeyboardStateTransition.ReturnToIdle
        }
    }
}

class NumberKeyboardIntentHandler(override val type: KeyboardType) : KeyboardIntentHandler {
    override fun handleIntent(intent: ImeIntent, currentState: KeyboardState): KeyboardStateTransition {
        return KeyboardStateTransition.ReturnToIdle
    }
}

class SymbolKeyboardIntentHandler(override val type: KeyboardType) : KeyboardIntentHandler {
    override fun handleIntent(intent: ImeIntent, currentState: KeyboardState): KeyboardStateTransition {
        return KeyboardStateTransition.ReturnToIdle
    }
}

class EmojiKeyboardIntentHandler(override val type: KeyboardType) : KeyboardIntentHandler {
    override fun handleIntent(intent: ImeIntent, currentState: KeyboardState): KeyboardStateTransition {
        return KeyboardStateTransition.ReturnToIdle
    }
}

class MathKeyboardIntentHandler(override val type: KeyboardType) : KeyboardIntentHandler {
    override fun handleIntent(intent: ImeIntent, currentState: KeyboardState): KeyboardStateTransition {
        return KeyboardStateTransition.ReturnToIdle
    }
}

class EditorKeyboardIntentHandler(override val type: KeyboardType) : KeyboardIntentHandler {
    override fun handleIntent(intent: ImeIntent, currentState: KeyboardState): KeyboardStateTransition {
        return KeyboardStateTransition.ReturnToIdle
    }
}

class CandidateKeyboardIntentHandler(override val type: KeyboardType) : KeyboardIntentHandler {
    override fun handleIntent(intent: ImeIntent, currentState: KeyboardState): KeyboardStateTransition {
        return KeyboardStateTransition.ReturnToIdle
    }
}

class CommitOptionKeyboardIntentHandler(override val type: KeyboardType) : KeyboardIntentHandler {
    override fun handleIntent(intent: ImeIntent, currentState: KeyboardState): KeyboardStateTransition {
        return KeyboardStateTransition.ReturnToIdle
    }
}
