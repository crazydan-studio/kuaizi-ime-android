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

import org.crazydan.studio.app.ime.kuaizi.engine.ImeIntent
import org.crazydan.studio.app.ime.kuaizi.engine.bridge.EditorAction
import org.crazydan.studio.app.ime.kuaizi.engine.domain.PinyinTree
import org.crazydan.studio.app.ime.kuaizi.engine.input.CommonInput

/**
 * 键盘状态机，集中处理状态转换的核心组件。
 * 接收 [KeyboardStateTransition]（由 [KeyboardIntentHandler] 生成），
 * 并根据当前状态执行转换规则，再返回转换结果。
 *
 * 遵循纯函数式转换原则：给定相同输入，始终产生相同输出。
 */
class KeyboardStateMachine(
    private val pinyinTree: PinyinTree,
) {

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
        when (transition) {
            is KeyboardStateTransition.ReturnToIdle ->
                KeyboardStateTransition.Result(KeyboardState.Idle)

            is KeyboardStateTransition.NothingToDo -> null

            is KeyboardStateTransition.Keyboard ->
                handleWithKeyboard(transition, currentState)

            is KeyboardStateTransition.InputList ->
                handleWithInputList(transition, currentState)

            is KeyboardStateTransition.Editor.PerformEdit ->
                KeyboardStateTransition.Result(
                    newState = currentState,
                    editorAction = EditorAction.PerformEdit(transition.action),
                )

            else ->
                when (currentState) {
                    is KeyboardState.Idle -> handleFromIdle(transition)
                    //
                    is KeyboardState.Pinyin.Inputting -> handleFromPinyinInputting(transition, currentState)
                    //
                    is KeyboardState.Editor -> handleFromEditor(transition, currentState)
                    //
                    is KeyboardState.CandidateSelection.Choosing -> handleFromCandidateChoosing(transition)
                    is KeyboardState.CandidateSelection.Filtering -> handleFromCandidateFiltering(transition)
                    is KeyboardState.CandidateSelection.AdvanceFiltering ->
                        handleFromCandidateAdvanceFiltering(transition)
                    //
                    is KeyboardState.CommitOptionChoosing -> handleFromCommitOptionChoosing(transition)
                    //
                    is KeyboardState.SymbolChoosing -> handleFromSymbolChoosing(transition)
                    is KeyboardState.EmojiChoosing -> handleFromEmojiChoosing(transition)
                    //
                    else -> null
                }
        }
            ?: KeyboardStateTransition.Result(currentState)

    // -----------------------------------------------------------------

    /** 处理 [KeyboardStateTransition.Keyboard] 类型的状态转换 */
    private fun handleWithKeyboard(
        transition: KeyboardStateTransition.Keyboard,
        state: KeyboardState,
    ): KeyboardStateTransition.Result =
        KeyboardStateTransition.Result(
            newState = state,
            sideEffects = listOf(
                when (transition) {
                    is KeyboardStateTransition.Keyboard.SwitchTo ->
                        ImeIntent.Keyboard.SwitchTo(transition.type)

                    is KeyboardStateTransition.Keyboard.ToggleHandMode ->
                        ImeIntent.Keyboard.ToggleHandMode
                }
            ),
        )

    /** 处理 [KeyboardStateTransition.InputList] 类型的状态转换 */
    private fun handleWithInputList(
        transition: KeyboardStateTransition.InputList,
        state: KeyboardState,
    ): KeyboardStateTransition.Result =
        KeyboardStateTransition.Result(
            newState = state,
            sideEffects = listOf(
                when (transition) {
                    is KeyboardStateTransition.InputList.Commit ->
                        ImeIntent.InputList.Commit

                    is KeyboardStateTransition.InputList.Revoke ->
                        ImeIntent.InputList.Revoke

                    is KeyboardStateTransition.InputList.ConfirmPending ->
                        ImeIntent.InputList.ConfirmPending

                    is KeyboardStateTransition.InputList.DeleteSelected ->
                        ImeIntent.InputList.RemoveSelected
                }
            ),
        )

    // -----------------------------------------------------------------

    /** 从 [KeyboardState.Idle] 状态处理转换 */
    private fun handleFromIdle(transition: KeyboardStateTransition): KeyboardStateTransition.Result? =
        when (transition) {
            // --------------------------------------
            is KeyboardStateTransition.Pinyin.StartInput ->
                transition.key.let { key ->
                    KeyboardState.Pinyin.Inputting(
                        level0Key = key,
                        vowelTree = createVowelTree(pinyinTree, key.value),
                    ).let { newState ->
                        KeyboardStateTransition.Result(
                            newState = newState,
                            sideEffects = listOf(
                                ImeIntent.InputList.UpdatePending(
                                    pending = createPinyinInputPending(pinyinTree, newState)
                                ),
                            ),
                        )
                    }
                }

            // --------------------------------------
            is KeyboardStateTransition.Char.Input ->
                KeyboardStateTransition.Result(
                    newState = KeyboardState.Idle,
                    sideEffects = listOf(
                        when (transition.key) {
                            // TODO 向编辑器提交换行符
                            is InputKey.Char.Enter -> TODO()
                            else -> {
                                val ch = transition.key.getReplacement(transition.replacement)

                                val char = when (transition.key) {
                                    is InputKey.Char.Emoji -> CommonInput.Item.Char.Emoji(value = ch)
                                    is InputKey.Char.Symbol -> CommonInput.Item.Char.Symbol(value = ch)
                                    //
                                    is InputKey.Char.Alphabet,
                                    is InputKey.Char.Number ->
                                        CommonInput.Item.Char.Latin(chars = listOf(ch))

                                    else -> CommonInput.Item.Char.Space
                                }

                                ImeIntent.InputList.AddChar(
                                    char = char,
                                    replacements =
                                        if (transition.replacement > 0)
                                            transition.key.replacements
                                        else null,
                                )
                            }
                        }
                    ),
                )

            is KeyboardStateTransition.Char.Backspace ->
                KeyboardStateTransition.Result(
                    newState = KeyboardState.Idle,
                    sideEffects = listOf(
                        ImeIntent.InputList.DeleteBackward
                    ),
                )

            // --------------------------------------
            is KeyboardStateTransition.Editor.Cursor.StartMove ->
                KeyboardStateTransition.Result(
                    newState = KeyboardState.Editor.CursorMoving,
                )

            is KeyboardStateTransition.Editor.Selection.StartSelect ->
                KeyboardStateTransition.Result(
                    newState = KeyboardState.Editor.SelectionSelecting,
                )

            // --------------------------------------
            else -> null
        }

    // -----------------------------------------------------------------

    /** 从 [KeyboardState.Pinyin.Inputting] 状态处理转换 */
    private fun handleFromPinyinInputting(
        transition: KeyboardStateTransition,
        state: KeyboardState.Pinyin.Inputting,
    ): KeyboardStateTransition.Result? =
        when (transition) {
            is KeyboardStateTransition.Pinyin.Inputting ->
                transition.key.let { key ->
                    when (key.level) {
                        // Note：需考虑回退的情况，因此，必须显式置空 level2Key
                        1 -> state.copy(level1Key = key, level2Key = null)
                        2 -> state.copy(level2Key = key)
                        else -> state
                    }.let { newState ->
                        KeyboardStateTransition.Result(
                            newState = newState,
                            sideEffects = listOf(
                                ImeIntent.InputList.UpdatePending(
                                    pending = createPinyinInputPending(pinyinTree, newState)
                                )
                            ),
                        )
                    }
                }

            is KeyboardStateTransition.Pinyin.StopInput ->
                KeyboardStateTransition.Result(
                    newState = KeyboardState.Idle,
                    sideEffects = listOf(
                        ImeIntent.InputList.ConfirmPending
                    ),
                )

            else -> null
        }

    // -----------------------------------------------------------------

    /** 从 [KeyboardState.Editor] 状态处理转换 */
    private fun handleFromEditor(
        transition: KeyboardStateTransition,
        state: KeyboardState.Editor,
    ): KeyboardStateTransition.Result =
        when (transition) {
            is KeyboardStateTransition.Editor.Cursor.Moving ->
                KeyboardStateTransition.Result(
                    newState = state,
                    editorAction =
                        EditorAction.MoveCursor(
                            motion = transition.motion,
                        ),
                )

            is KeyboardStateTransition.Editor.Selection.Selecting ->
                KeyboardStateTransition.Result(
                    newState = state,
                    editorAction =
                        EditorAction.SelectSelection(
                            motion = transition.motion,
                        ),
                )

            is KeyboardStateTransition.Editor.Cursor.StopMove,
            is KeyboardStateTransition.Editor.Selection.StopSelect ->
                null

            else -> null
        }
            ?: KeyboardStateTransition.Result(KeyboardState.Idle)

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
            is KeyboardStateTransition.LoadCommitOptions ->
                KeyboardStateTransition.Result(KeyboardState.CommitOptionChoosing(transition.options))

            else -> null
        }
    }

    // -----------------------------------------------------------------

    /** 从 SymbolChoosing 状态处理转换 */
    private fun handleFromSymbolChoosing(transition: KeyboardStateTransition): KeyboardStateTransition.Result? {
        return when (transition) {
            is KeyboardStateTransition.OpenSymbolGroup ->
                KeyboardStateTransition.Result(KeyboardState.SymbolChoosing(transition.groupId))

            else -> null
        }
    }

    /** 从 EmojiChoosing 状态处理转换 */
    private fun handleFromEmojiChoosing(transition: KeyboardStateTransition): KeyboardStateTransition.Result? {
        return when (transition) {
            is KeyboardStateTransition.OpenEmojiGroup ->
                KeyboardStateTransition.Result(KeyboardState.EmojiChoosing(transition.groupId))

            else -> null
        }
    }
}
