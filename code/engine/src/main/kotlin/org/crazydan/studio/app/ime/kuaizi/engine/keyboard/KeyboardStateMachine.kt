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

package org.crazydan.studio.app.ime.kuaizi.engine.keyboard

import org.crazydan.studio.app.ime.kuaizi.engine.domain.PinyinTree
import org.crazydan.studio.app.ime.kuaizi.engine.input.InputListOperator

/**
 * 键盘状态机，集中处理状态转换的核心组件。
 * 接收 [KeyboardStateTransition]（由 [KeyboardIntentHandler] 生成），
 * 并根据当前状态执行转换规则，再返回转换结果。
 *
 * 遵循纯函数式转换原则：给定相同输入，始终产生相同输出。
 *
 * @param inputListOp 输入列表操作器，用于执行输入列表变更
 */
class KeyboardStateMachine(
    private val pinyinTree: PinyinTree,
    private val inputListOp: InputListOperator,
) {
    private val stateHistory = KeyboardStateHistory()

    // -----------------------------------------------------------------

    /**
     * 处理状态转换
     * @param handleTransition 要执行的状态转换
     * @param currentState 当前键盘状态
     * @return 转换结果，包含新状态、副作用列表和编辑器动作
     */
    fun handleTransition(
        transition: KeyboardStateTransition,
        currentState: KeyboardState,
    ): KeyboardStateTransition.Result =
        when (currentState) {
            is KeyboardState.Idle -> handleFromIdle(transition)
            //
            is KeyboardState.Pinyin.Waiting -> handleFromPinyinWaiting(transition)
            is KeyboardState.Pinyin.Inputting -> handleFromPinyinInputting(transition)
            //
            is KeyboardState.CandidateSelection.Choosing -> handleFromCandidateChoosing(transition)
            is KeyboardState.CandidateSelection.Filtering -> handleFromCandidateFiltering(transition)
            is KeyboardState.CandidateSelection.AdvanceFiltering -> handleFromCandidateAdvanceFiltering(transition)
            //
            is KeyboardState.CommitOptionChoosing -> handleFromCommitOptionChoosing(transition)
            is KeyboardState.EditorEditing.CursorMoving -> handleFromEditorCursorMoving(transition)
            is KeyboardState.EditorEditing.TextSelecting -> handleFromEditorTextSelecting(transition)
            //
            is KeyboardState.SymbolChoosing -> handleFromSymbolChoosing(transition)
            is KeyboardState.EmojiChoosing -> handleFromEmojiChoosing(transition)
        }?.apply {
            if (newState != currentState) {
                stateHistory.push(currentState)
            }
        }
            ?: KeyboardStateTransition.Result(currentState)

    // -----------------------------------------------------------------

    /** 回退到前一状态，历史栈空时回退到 [KeyboardState.Idle] */
    fun backToPrevious(): KeyboardState =
        stateHistory.pop() ?: KeyboardState.Idle

    /** 重置：清空历史栈 */
    fun reset() {
        stateHistory.clear()
    }

    // -----------------------------------------------------------------

    /** 从 Idle 状态处理转换 */
    private fun handleFromIdle(transition: KeyboardStateTransition): KeyboardStateTransition.Result? {
        return when (transition) {
            is KeyboardStateTransition.InputChar ->
                KeyboardStateTransition.Result(KeyboardState.Pinyin.Waiting())

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

            else -> null
        }
    }

    // -----------------------------------------------------------------

    /** 从 [KeyboardState.Pinyin.Waiting] 状态处理转换 */
    private fun handleFromPinyinWaiting(transition: KeyboardStateTransition): KeyboardStateTransition.Result? {
        return when (transition) {
            is KeyboardStateTransition.StartInputPinyin ->
                KeyboardStateTransition.Result(
                    newState = KeyboardState.Pinyin.Inputting(
                        lastKey = transition.key,
                        level0Key = transition.key,
                    )
                )

            is KeyboardStateTransition.InputChar ->
                KeyboardStateTransition.Result(KeyboardState.Pinyin.Waiting())

            is KeyboardStateTransition.LoadCandidates ->
                KeyboardStateTransition.Result(KeyboardState.CandidateSelection.Choosing(transition.candidates))

            is KeyboardStateTransition.ReturnToIdle ->
                KeyboardStateTransition.Result(KeyboardState.Idle)

            else -> null
        }
    }

    /** 从 [KeyboardState.Pinyin.Inputting] 状态处理转换 */
    private fun handleFromPinyinInputting(transition: KeyboardStateTransition): KeyboardStateTransition.Result? {
        return when (transition) {
            is KeyboardStateTransition.InputChar ->
                KeyboardStateTransition.Result(
                    newState = KeyboardState.Pinyin.Waiting()
                )

            is KeyboardStateTransition.LoadCandidates ->
                KeyboardStateTransition.Result(
                    newState = KeyboardState.CandidateSelection.Choosing(transition.candidates)
                )

            is KeyboardStateTransition.ReturnToIdle ->
                KeyboardStateTransition.Result(
                    newState = KeyboardState.Pinyin.Waiting()
                )

            else -> null
        }
    }

    // -----------------------------------------------------------------

    /** 从 CandidateSelection.Choosing 状态处理转换 */
    private fun handleFromCandidateChoosing(transition: KeyboardStateTransition): KeyboardStateTransition.Result? {
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
                KeyboardStateTransition.Result(KeyboardState.Pinyin.Waiting())

            is KeyboardStateTransition.LoadCommitOptions ->
                KeyboardStateTransition.Result(KeyboardState.CommitOptionChoosing(transition.options))

            else -> null
        }
    }

    /** 从 CandidateSelection.Filtering 状态处理转换 */
    private fun handleFromCandidateFiltering(transition: KeyboardStateTransition): KeyboardStateTransition.Result? {
        return when (transition) {
            is KeyboardStateTransition.BackToPrevious ->
                KeyboardStateTransition.Result(KeyboardState.CandidateSelection.Choosing())

            is KeyboardStateTransition.FilterCandidates ->
                KeyboardStateTransition.Result(
                    KeyboardState.CandidateSelection.Filtering(transition.filter),
                )

            is KeyboardStateTransition.ReturnToIdle ->
                KeyboardStateTransition.Result(KeyboardState.Pinyin.Waiting())

            else -> null
        }
    }

    /** 从 CandidateSelection.AdvanceFiltering 状态处理转换 */
    private fun handleFromCandidateAdvanceFiltering(transition: KeyboardStateTransition): KeyboardStateTransition.Result? {
        return when (transition) {
            is KeyboardStateTransition.BackToPrevious ->
                KeyboardStateTransition.Result(KeyboardState.CandidateSelection.Choosing())

            is KeyboardStateTransition.AdvanceFilterCandidates ->
                KeyboardStateTransition.Result(
                    KeyboardState.CandidateSelection.AdvanceFiltering(transition.radical, transition.tone),
                )

            else -> null
        }
    }

    // -----------------------------------------------------------------

    /** 从 CommitOptionChoosing 状态处理转换 */
    private fun handleFromCommitOptionChoosing(transition: KeyboardStateTransition): KeyboardStateTransition.Result? {
        return when (transition) {
            is KeyboardStateTransition.ReturnToIdle ->
                KeyboardStateTransition.Result(KeyboardState.Pinyin.Waiting())

            is KeyboardStateTransition.LoadCommitOptions ->
                KeyboardStateTransition.Result(KeyboardState.CommitOptionChoosing(transition.options))

            else -> null
        }
    }

    // -----------------------------------------------------------------

    /** 从 EditorEditing.CursorMoving 状态处理转换 */
    private fun handleFromEditorCursorMoving(transition: KeyboardStateTransition): KeyboardStateTransition.Result? {
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

            else -> null
        }
    }

    /** 从 EditorEditing.TextSelecting 状态处理转换 */
    private fun handleFromEditorTextSelecting(transition: KeyboardStateTransition): KeyboardStateTransition.Result? {
        return when (transition) {
            is KeyboardStateTransition.SelectText ->
                KeyboardStateTransition.Result(
                    KeyboardState.EditorEditing.TextSelecting(transition.start, transition.end),
                )

            is KeyboardStateTransition.ReturnToIdle ->
                KeyboardStateTransition.Result(stateHistory.pop() ?: KeyboardState.Idle)

            is KeyboardStateTransition.BackToPrevious ->
                KeyboardStateTransition.Result(stateHistory.pop() ?: KeyboardState.Idle)

            else -> null
        }
    }

    // -----------------------------------------------------------------

    /** 从 SymbolChoosing 状态处理转换 */
    private fun handleFromSymbolChoosing(transition: KeyboardStateTransition): KeyboardStateTransition.Result? {
        return when (transition) {
            is KeyboardStateTransition.OpenSymbolGroup ->
                KeyboardStateTransition.Result(KeyboardState.SymbolChoosing(transition.groupId))

            is KeyboardStateTransition.ReturnToIdle ->
                KeyboardStateTransition.Result(KeyboardState.Idle)

            else -> null
        }
    }

    /** 从 EmojiChoosing 状态处理转换 */
    private fun handleFromEmojiChoosing(transition: KeyboardStateTransition): KeyboardStateTransition.Result? {
        return when (transition) {
            is KeyboardStateTransition.OpenEmojiGroup ->
                KeyboardStateTransition.Result(KeyboardState.EmojiChoosing(transition.groupId))

            is KeyboardStateTransition.ReturnToIdle ->
                KeyboardStateTransition.Result(KeyboardState.Idle)

            else -> null
        }
    }
}

/**
 * 键盘状态历史的有界栈，用于实现同一键盘类型内的子状态回退。
 * 采用 ArrayDeque 实现，最大容量为 10，FIFO 淘汰策略。
 *
 * @param maxSize 历史栈最大容量
 */
class KeyboardStateHistory(private val maxSize: Int = 10) {
    private val stack = ArrayDeque<KeyboardState>(maxSize)

    /** 当前历史栈大小 */
    val size: Int get() = stack.size

    /** 将当前状态压入历史栈，超出上限时淘汰最旧条目 */
    fun push(state: KeyboardState) {
        if (stack.size >= maxSize) {
            stack.removeFirst()
        }
        stack.addLast(state)
    }

    /** 弹出最近的历史状态，栈空时返回 null */
    fun pop(): KeyboardState? = stack.removeLastOrNull()

    /** 清空历史栈 */
    fun clear() {
        stack.clear()
    }
}
