/*
 * 筷字输入法 - 高效编辑需要又好又快的输入法
 * Copyright (C) 2026 Crazydan Studio <https://studio.crazydan.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.
 * If not, see <https://www.gnu.org/licenses/lgpl-3.0.en.html#license-text>.
 */

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

    val size: Int get() = stack.size
}

class KeyboardStateMachine(
    private val inputListOp: InputListOperator,
) {
    private var _state: KeyboardState = KeyboardState.Idle
    val state: KeyboardState get() = _state

    private val stateHistory = KeyboardStateHistory()

    fun transition(transition: KeyboardStateTransition): KeyboardStateTransition.Result {
        val (newState, sideEffects, editorAction) = when (_state) {
            is KeyboardState.Idle -> handleFromIdle(transition)
            is KeyboardState.PinyinInput.Waiting -> handleFromPinyinWaiting(transition)
            is KeyboardState.PinyinInput.Slipping -> handleFromPinyinSlipping(transition)
            is KeyboardState.PinyinInput.Flipping -> handleFromPinyinFlipping(transition)
            is KeyboardState.CandidateSelection.Choosing -> handleFromCandidateChoosing(transition)
            is KeyboardState.CandidateSelection.Filtering -> handleFromCandidateFiltering(transition)
            is KeyboardState.CandidateSelection.AdvanceFiltering -> handleFromCandidateAdvanceFiltering(transition)
            is KeyboardState.CommitOptionChoosing -> handleFromCommitOptionChoosing(transition)
            is KeyboardState.EditorEditing.CursorMoving -> handleFromEditorCursorMoving(transition)
            is KeyboardState.EditorEditing.TextSelecting -> handleFromEditorTextSelecting(transition)
            is KeyboardState.SymbolChoosing -> handleFromSymbolChoosing(transition)
            is KeyboardState.EmojiChoosing -> handleFromEmojiChoosing(transition)
        }

        if (newState != _state) {
            stateHistory.push(_state)
            _state = newState
        }
        return KeyboardStateTransition.Result(newState, sideEffects, editorAction)
    }

    fun backToPrevious() {
        _state = stateHistory.pop() ?: KeyboardState.Idle
    }

    fun resetToIdle() {
        _state = KeyboardState.Idle
        stateHistory.clear()
    }

    fun resetTo(state: KeyboardState) {
        _state = state
        stateHistory.clear()
    }

    private fun handleFromIdle(transition: KeyboardStateTransition): KeyboardStateTransition.Result {
        return when (transition) {
            is KeyboardStateTransition.InputPinyinChar ->
                KeyboardStateTransition.Result(KeyboardState.PinyinInput.Waiting())
            is KeyboardStateTransition.OpenSymbolGroup ->
                KeyboardStateTransition.Result(KeyboardState.SymbolChoosing(transition.groupId))
            is KeyboardStateTransition.OpenEmojiGroup ->
                KeyboardStateTransition.Result(KeyboardState.EmojiChoosing(transition.groupId))
            is KeyboardStateTransition.MoveCursor ->
                KeyboardStateTransition.Result(KeyboardState.EditorEditing.CursorMoving(transition.position))
            is KeyboardStateTransition.LoadCandidates ->
                KeyboardStateTransition.Result(KeyboardState.CandidateSelection.Choosing(transition.candidates))
            is KeyboardStateTransition.LoadCommitOptions ->
                KeyboardStateTransition.Result(KeyboardState.CommitOptionChoosing(transition.options))
            else -> KeyboardStateTransition.Result(_state)
        }
    }

    private fun handleFromPinyinWaiting(transition: KeyboardStateTransition): KeyboardStateTransition.Result {
        return when (transition) {
            is KeyboardStateTransition.InputPinyinChar ->
                KeyboardStateTransition.Result(KeyboardState.PinyinInput.Waiting())
            is KeyboardStateTransition.BeginSlip ->
                KeyboardStateTransition.Result(KeyboardState.PinyinInput.Slipping(transition.startKey, transition.startKey))
            is KeyboardStateTransition.BeginFlip ->
                KeyboardStateTransition.Result(KeyboardState.PinyinInput.Flipping(transition.startChar.toString()))
            is KeyboardStateTransition.LoadCandidates ->
                KeyboardStateTransition.Result(KeyboardState.CandidateSelection.Choosing(transition.candidates))
            is KeyboardStateTransition.ReturnToIdle ->
                KeyboardStateTransition.Result(KeyboardState.Idle)
            else -> KeyboardStateTransition.Result(_state)
        }
    }

    private fun handleFromPinyinSlipping(transition: KeyboardStateTransition): KeyboardStateTransition.Result {
        return when (transition) {
            is KeyboardStateTransition.SelectSlipChar ->
                KeyboardStateTransition.Result(KeyboardState.PinyinInput.Waiting())
            is KeyboardStateTransition.BeginFlip ->
                KeyboardStateTransition.Result(KeyboardState.PinyinInput.Flipping(transition.startChar.toString()))
            is KeyboardStateTransition.LoadCandidates ->
                KeyboardStateTransition.Result(KeyboardState.CandidateSelection.Choosing(transition.candidates))
            is KeyboardStateTransition.ReturnToIdle ->
                KeyboardStateTransition.Result(KeyboardState.PinyinInput.Waiting())
            else -> KeyboardStateTransition.Result(_state)
        }
    }

    private fun handleFromPinyinFlipping(transition: KeyboardStateTransition): KeyboardStateTransition.Result {
        return when (transition) {
            is KeyboardStateTransition.SelectFlipChar ->
                KeyboardStateTransition.Result(KeyboardState.PinyinInput.Waiting())
            is KeyboardStateTransition.LoadCandidates ->
                KeyboardStateTransition.Result(KeyboardState.CandidateSelection.Choosing(transition.candidates))
            is KeyboardStateTransition.ReturnToIdle ->
                KeyboardStateTransition.Result(KeyboardState.PinyinInput.Waiting())
            else -> KeyboardStateTransition.Result(_state)
        }
    }

    private fun handleFromCandidateChoosing(transition: KeyboardStateTransition): KeyboardStateTransition.Result {
        return when (transition) {
            is KeyboardStateTransition.FilterCandidates ->
                KeyboardStateTransition.Result(
                    KeyboardState.CandidateSelection.Filtering(transition.filter),
                )
            is KeyboardStateTransition.AdvanceFilterCandidates ->
                KeyboardStateTransition.Result(
                    KeyboardState.CandidateSelection.AdvanceFiltering(transition.radical, transition.tone),
                )
            is KeyboardStateTransition.ReturnToIdle ->
                KeyboardStateTransition.Result(KeyboardState.PinyinInput.Waiting())
            is KeyboardStateTransition.LoadCommitOptions ->
                KeyboardStateTransition.Result(KeyboardState.CommitOptionChoosing(transition.options))
            is KeyboardStateTransition.PageCandidates ->
                KeyboardStateTransition.Result(_state)
            is KeyboardStateTransition.LoadCandidates ->
                KeyboardStateTransition.Result(_state)
            else -> KeyboardStateTransition.Result(_state)
        }
    }

    private fun handleFromCandidateFiltering(transition: KeyboardStateTransition): KeyboardStateTransition.Result {
        return when (transition) {
            is KeyboardStateTransition.BackToPrevious ->
                KeyboardStateTransition.Result(KeyboardState.CandidateSelection.Choosing())
            is KeyboardStateTransition.FilterCandidates ->
                KeyboardStateTransition.Result(
                    KeyboardState.CandidateSelection.Filtering(transition.filter),
                )
            is KeyboardStateTransition.PageCandidates ->
                KeyboardStateTransition.Result(_state)
            is KeyboardStateTransition.ReturnToIdle ->
                KeyboardStateTransition.Result(KeyboardState.PinyinInput.Waiting())
            else -> KeyboardStateTransition.Result(_state)
        }
    }

    private fun handleFromCandidateAdvanceFiltering(transition: KeyboardStateTransition): KeyboardStateTransition.Result {
        return when (transition) {
            is KeyboardStateTransition.BackToPrevious ->
                KeyboardStateTransition.Result(KeyboardState.CandidateSelection.Choosing())
            is KeyboardStateTransition.AdvanceFilterCandidates ->
                KeyboardStateTransition.Result(
                    KeyboardState.CandidateSelection.AdvanceFiltering(transition.radical, transition.tone),
                )
            is KeyboardStateTransition.PageCandidates ->
                KeyboardStateTransition.Result(_state)
            else -> KeyboardStateTransition.Result(_state)
        }
    }

    private fun handleFromCommitOptionChoosing(transition: KeyboardStateTransition): KeyboardStateTransition.Result {
        return when (transition) {
            is KeyboardStateTransition.ReturnToIdle ->
                KeyboardStateTransition.Result(KeyboardState.PinyinInput.Waiting())
            is KeyboardStateTransition.LoadCommitOptions ->
                KeyboardStateTransition.Result(KeyboardState.CommitOptionChoosing(transition.options))
            else -> KeyboardStateTransition.Result(_state)
        }
    }

    private fun handleFromEditorCursorMoving(transition: KeyboardStateTransition): KeyboardStateTransition.Result {
        return when (transition) {
            is KeyboardStateTransition.MoveCursor ->
                KeyboardStateTransition.Result(
                    KeyboardState.EditorEditing.CursorMoving(transition.position),
                )
            is KeyboardStateTransition.SelectText ->
                KeyboardStateTransition.Result(
                    KeyboardState.EditorEditing.TextSelecting(transition.start, transition.end),
                )
            is KeyboardStateTransition.ReturnToIdle ->
                KeyboardStateTransition.Result(stateHistory.pop() ?: KeyboardState.Idle)
            is KeyboardStateTransition.BackToPrevious ->
                KeyboardStateTransition.Result(stateHistory.pop() ?: KeyboardState.Idle)
            else -> KeyboardStateTransition.Result(_state)
        }
    }

    private fun handleFromEditorTextSelecting(transition: KeyboardStateTransition): KeyboardStateTransition.Result {
        return when (transition) {
            is KeyboardStateTransition.SelectText ->
                KeyboardStateTransition.Result(
                    KeyboardState.EditorEditing.TextSelecting(transition.start, transition.end),
                )
            is KeyboardStateTransition.ReturnToIdle ->
                KeyboardStateTransition.Result(stateHistory.pop() ?: KeyboardState.Idle)
            is KeyboardStateTransition.BackToPrevious ->
                KeyboardStateTransition.Result(stateHistory.pop() ?: KeyboardState.Idle)
            else -> KeyboardStateTransition.Result(_state)
        }
    }

    private fun handleFromSymbolChoosing(transition: KeyboardStateTransition): KeyboardStateTransition.Result {
        return when (transition) {
            is KeyboardStateTransition.OpenSymbolGroup ->
                KeyboardStateTransition.Result(KeyboardState.SymbolChoosing(transition.groupId))
            is KeyboardStateTransition.PageCandidates ->
                KeyboardStateTransition.Result(_state)
            is KeyboardStateTransition.ReturnToIdle ->
                KeyboardStateTransition.Result(KeyboardState.Idle)
            else -> KeyboardStateTransition.Result(_state)
        }
    }

    private fun handleFromEmojiChoosing(transition: KeyboardStateTransition): KeyboardStateTransition.Result {
        return when (transition) {
            is KeyboardStateTransition.OpenEmojiGroup ->
                KeyboardStateTransition.Result(KeyboardState.EmojiChoosing(transition.groupId))
            is KeyboardStateTransition.PageCandidates ->
                KeyboardStateTransition.Result(_state)
            is KeyboardStateTransition.ReturnToIdle ->
                KeyboardStateTransition.Result(KeyboardState.Idle)
            else -> KeyboardStateTransition.Result(_state)
        }
    }
}
