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

package org.crazydan.studio.app.ime.kuaizi.engine.keyboard.intent

import org.crazydan.studio.app.ime.kuaizi.engine.ImeIntent
import org.crazydan.studio.app.ime.kuaizi.engine.keyboard.InputKey
import org.crazydan.studio.app.ime.kuaizi.engine.keyboard.KeyboardIntentHandler
import org.crazydan.studio.app.ime.kuaizi.engine.keyboard.KeyboardState
import org.crazydan.studio.app.ime.kuaizi.engine.keyboard.KeyboardStateTransition
import org.crazydan.studio.app.ime.kuaizi.engine.keyboard.KeyboardType

/**
 * 基础键盘意图处理器，所有键盘类型的默认实现。
 */
open class BaseKeyboardIntentHandler() : KeyboardIntentHandler {

    override fun handleIntent(intent: ImeIntent, currentState: KeyboardState): KeyboardStateTransition {
        return KeyboardStateTransition.ReturnToIdle
    }

    /**
     * 处理与 [InputKey.Ctrl] 相关的 [ImeIntent.OnKeyboard]：
     * - 若长按或点击按键 [InputKey.Ctrl.Backspace]，均准备回删字符；
     * - 若操作按键 [InputKey.Ctrl.Keyboard]，则由 [handleKeyboardCtrlKeyIntent] 处理；
     * - 若操作按键 [InputKey.Ctrl.Editor.MoveCursor]，则由 [handleEditorMoveCursorKeyIntent] 处理；
     */
    protected fun handleCtrlKeyIntent(
        intent: ImeIntent.OnKeyboard,
        key: InputKey.Ctrl,
    ): KeyboardStateTransition? =
        when (key) {
            is InputKey.Ctrl.Backspace ->
                when (intent) {
                    is ImeIntent.OnKeyboard.Tap,
                    is ImeIntent.OnKeyboard.LongPress.Hold,
                        ->
                        KeyboardStateTransition.Char.Backspace

                    else -> null
                }

            is InputKey.Ctrl.Keyboard ->
                handleKeyboardCtrlKeyIntent(intent = intent, key = key)

            is InputKey.Ctrl.Editor.MoveCursor ->
                handleEditorMoveCursorKeyIntent(intent)

            is InputKey.Ctrl.InputList ->
                handleInputListCtrlKeyIntent(intent = intent, key = key)

            else -> null
        }

    /**
     * 处理与 [InputKey.Ctrl.Editor.MoveCursor] 相关的 [ImeIntent.OnKeyboard]：
     * - 若双击，则准备切换到编辑器键盘；
     * - 若滑行，则准备进入光标移动状态；
     * - 若长按，则准备进入选区选区状态；
     */
    protected fun handleEditorMoveCursorKeyIntent(
        intent: ImeIntent.OnKeyboard,
    ): KeyboardStateTransition? =
        when (intent) {
            is ImeIntent.OnKeyboard.Tap.Double ->
                KeyboardStateTransition.Keyboard.SwitchTo(KeyboardType.Editor)

            is ImeIntent.OnKeyboard.Swipe.Begin ->
                KeyboardStateTransition.Editor.Cursor.StartMove

            is ImeIntent.OnKeyboard.LongPress.Begin ->
                KeyboardStateTransition.Editor.Selection.StartSelect

            else -> null
        }

    /**
     * 处理与 [InputKey.Ctrl.Keyboard] 相关的 [ImeIntent.OnKeyboard]：
     * - 若点击切换按键，则准备切换到目标类型键盘；
     * - 若点击退回按键，则准备切换到所要退回到的目标类型键盘；
     * - 若点击左右手模式按键，则准备反转键盘的左右手模式；
     */
    protected fun handleKeyboardCtrlKeyIntent(
        intent: ImeIntent.OnKeyboard,
        key: InputKey.Ctrl.Keyboard,
    ): KeyboardStateTransition? =
        when (intent) {
            is ImeIntent.OnKeyboard.Tap ->
                when (key) {
                    is InputKey.Ctrl.Keyboard.SwitchTo ->
                        KeyboardStateTransition.Keyboard.SwitchTo(key.type)

                    is InputKey.Ctrl.Keyboard.BackTo ->
                        KeyboardStateTransition.Keyboard.SwitchTo(key.type)

                    is InputKey.Ctrl.Keyboard.ToggleHandMode ->
                        KeyboardStateTransition.Keyboard.ToggleHandMode
                }

            else -> null
        }

    /**
     * 处理与 [InputKey.Ctrl.InputList] 相关的 [ImeIntent.OnKeyboard]：
     */
    protected fun handleInputListCtrlKeyIntent(
        intent: ImeIntent.OnKeyboard,
        key: InputKey.Ctrl.InputList,
    ): KeyboardStateTransition? =
        when (intent) {
            is ImeIntent.OnKeyboard.Tap ->
                when (key) {
                    is InputKey.Ctrl.InputList.Commit ->
                        KeyboardStateTransition.InputList.Commit

                    is InputKey.Ctrl.InputList.Revoke ->
                        KeyboardStateTransition.InputList.Revoke

                    is InputKey.Ctrl.InputList.DeleteSelected ->
                        KeyboardStateTransition.InputList.DeleteSelected

                    is InputKey.Ctrl.InputList.ConfirmPending ->
                        KeyboardStateTransition.InputList.ConfirmPending
                }

            else -> null
        }

    // ------------------------------------------------------------------------

    /**
     * 处理 [KeyboardState.Editor] 状态下的 [ImeIntent.OnKeyboard]：
     * - 若为滑行中，则准备更新光标位置或更新选区范围；
     * - 若为滑行结束，则准备结束光标移动或结束选区选取状态；
     */
    protected fun handleIntentWhenEditorEditing(
        intent: ImeIntent.OnKeyboard, state: KeyboardState.Editor,
    ): KeyboardStateTransition =
        when (intent) {
            is ImeIntent.OnKeyboard.Swipe.Moving ->
                // 仅作一次有效移动，避免光标移动过于灵活
                if (intent.motion.distance < 1) null
                else
                    when (state) {
                        is KeyboardState.Editor.CursorMoving ->
                            KeyboardStateTransition.Editor.Cursor.Moving(
                                motion = intent.motion.copy(distance = 1f)
                            )

                        is KeyboardState.Editor.SelectionSelecting ->
                            KeyboardStateTransition.Editor.Selection.Selecting(
                                motion = intent.motion.copy(distance = 1f)
                            )
                    }

            is ImeIntent.OnKeyboard.Swipe.End ->
                when (state) {
                    is KeyboardState.Editor.CursorMoving ->
                        KeyboardStateTransition.Editor.Cursor.StopMove

                    is KeyboardState.Editor.SelectionSelecting ->
                        KeyboardStateTransition.Editor.Selection.StopSelect
                }

            else -> null
        }
            ?: KeyboardStateTransition.NothingToDo
}
