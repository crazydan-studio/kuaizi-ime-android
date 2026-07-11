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
import org.crazydan.studio.app.ime.kuaizi.engine.keyboard.KeyboardState
import org.crazydan.studio.app.ime.kuaizi.engine.keyboard.KeyboardStateTransition

/** 编辑键盘意图处理器 */
class EditorKeyboardIntentHandler : BaseKeyboardIntentHandler() {

    override fun handleIntent(intent: ImeIntent, currentState: KeyboardState): KeyboardStateTransition =
        when (intent) {
            is ImeIntent.OnKeyboard ->
                when (currentState) {
                    is KeyboardState.Idle ->
                        when (intent.key) {
                            is InputKey.Ctrl ->
                                handleCtrlKeyIntentWhenIdle(
                                    intent = intent,
                                    key = intent.key as InputKey.Ctrl,
                                )

                            else -> null
                        }

                    is KeyboardState.Editor ->
                        handleIntentWhenEditorEditing(intent = intent, state = currentState)

                    else -> null
                }

            else -> null
        }
            ?: KeyboardStateTransition.NothingToDo

    /**
     * 处理 [KeyboardState.Idle] 状态下的与 [InputKey.Ctrl] 相关的 [ImeIntent]：
     * - 若在按键 [InputKey.Ctrl.Editor.MoveCursor] 或 [InputKey.Ctrl.Editor.SelectSelection]
     *   上开始滑行，则分别准备进入光标移动和选区选取状态；
     * - 若点击按键 [InputKey.Ctrl.Editor.PerformEdit]，则准备执行相关的编辑操作；
     * - 其余按键由 [handleCtrlKeyIntent] 处理；
     */
    private fun handleCtrlKeyIntentWhenIdle(
        intent: ImeIntent.OnKeyboard,
        key: InputKey.Ctrl,
    ): KeyboardStateTransition? =
        when (key) {
            is InputKey.Ctrl.Editor.MoveCursor ->
                when (intent) {
                    is ImeIntent.OnKeyboard.Swipe.Begin ->
                        KeyboardStateTransition.Editor.Cursor.StartMove

                    else -> null
                }

            is InputKey.Ctrl.Editor.SelectSelection ->
                when (intent) {
                    is ImeIntent.OnKeyboard.Swipe.Begin ->
                        KeyboardStateTransition.Editor.Selection.StartSelect

                    else -> null
                }

            is InputKey.Ctrl.Editor.PerformEdit ->
                when (intent) {
                    is ImeIntent.OnKeyboard.Tap ->
                        KeyboardStateTransition.Editor.PerformEdit(key.action)

                    else -> null
                }

            else ->
                handleCtrlKeyIntent(intent = intent, key = key)
        }
}
