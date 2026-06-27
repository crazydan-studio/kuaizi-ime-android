package org.crazydan.studio.app.ime.kuaizi.engine.domain

import org.crazydan.studio.app.ime.kuaizi.engine.ImeIntent
import org.crazydan.studio.app.ime.kuaizi.engine.domain.InputKey.Char as CharKey

interface KeyboardIntentHandler {
    val type: KeyboardType
    fun handleIntent(intent: ImeIntent, currentState: KeyboardState): KeyboardStateTransition
}

open class BaseKeyboardIntentHandler(override val type: KeyboardType) : KeyboardIntentHandler {
    override fun handleIntent(intent: ImeIntent, currentState: KeyboardState): KeyboardStateTransition {
        return KeyboardStateTransition.ReturnToIdle
    }
}

class PinyinIntentHandler(override val type: KeyboardType) : KeyboardIntentHandler {
    override fun handleIntent(intent: ImeIntent, currentState: KeyboardState): KeyboardStateTransition {
        return when (currentState) {
            is KeyboardState.PinyinInput.Waiting -> {
                when (intent) {
                    is ImeIntent.PressKey -> {
                        when (intent.key) {
                            is CharKey -> KeyboardStateTransition.InputPinyinChar(intent.key.text.first())
                            else -> KeyboardStateTransition.LoadMoreCandidates
                        }
                    }

                    is ImeIntent.DeleteInput -> KeyboardStateTransition.DeleteInput
                    is ImeIntent.SelectCandidate -> KeyboardStateTransition.SelectCandidate
                    is ImeIntent.CommitInput -> KeyboardStateTransition.CommitInput
                    else -> KeyboardStateTransition.ReturnToIdle
                }
            }

            is KeyboardState.PinyinInput.Slipping -> {
                when (intent) {
                    is ImeIntent.PressKey -> KeyboardStateTransition.BeginSlip(intent.key)
                    else -> KeyboardStateTransition.ReturnToIdle
                }
            }

            is KeyboardState.PinyinInput.Flipping -> {
                when (intent) {
                    is ImeIntent.PressKey -> KeyboardStateTransition.SelectFlipChar((intent.key as InputKey.Char).text.first())
                    else -> KeyboardStateTransition.ReturnToIdle
                }
            }

            is KeyboardState.CandidateSelection.Choosing -> {
                when (intent) {
                    is ImeIntent.SelectCandidate -> KeyboardStateTransition.SelectCandidate
                    is ImeIntent.PageCandidate -> KeyboardStateTransition.PageCandidates(intent.direction)
                    is ImeIntent.DeleteInput -> KeyboardStateTransition.BackToInput
                    else -> KeyboardStateTransition.ReturnToIdle
                }
            }

            is KeyboardState.CandidateSelection.Filtering -> {
                when (intent) {
                    is ImeIntent.DeleteInput -> KeyboardStateTransition.BackToChoosing
                    else -> KeyboardStateTransition.ReturnToIdle
                }
            }

            is KeyboardState.CommitOptionChoosing -> {
                when (intent) {
                    is ImeIntent.SelectCandidate -> KeyboardStateTransition.CommitInput
                    else -> KeyboardStateTransition.ReturnToIdle
                }
            }

            else -> KeyboardStateTransition.ReturnToIdle
        }
    }
}

class NumberKeyboardIntentHandler(override val type: KeyboardType) : BaseKeyboardIntentHandler(type)
class SymbolKeyboardIntentHandler(override val type: KeyboardType) : BaseKeyboardIntentHandler(type)
class EmojiKeyboardIntentHandler(override val type: KeyboardType) : BaseKeyboardIntentHandler(type)
class MathKeyboardIntentHandler(override val type: KeyboardType) : BaseKeyboardIntentHandler(type)
class EditorKeyboardIntentHandler(override val type: KeyboardType) : BaseKeyboardIntentHandler(type)
class CandidateKeyboardIntentHandler(override val type: KeyboardType) : BaseKeyboardIntentHandler(type)
class CommitOptionKeyboardIntentHandler(override val type: KeyboardType) : BaseKeyboardIntentHandler(type)
