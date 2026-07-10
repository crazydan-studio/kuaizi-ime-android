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
import org.crazydan.studio.app.ime.kuaizi.engine.keyboard.KeyboardInputMode
import org.crazydan.studio.app.ime.kuaizi.engine.keyboard.KeyboardState
import org.crazydan.studio.app.ime.kuaizi.engine.keyboard.KeyboardStateTransition

/**
 * 拼音键盘意图处理器，管理拼音字符输入、候选字查询和输入补全。
 *
 * @param inputMode 键盘输入模式
 */
class PinyinKeyboardIntentHandler(
    val inputMode: KeyboardInputMode,
) : BaseKeyboardIntentHandler() {

    override fun handleIntent(intent: ImeIntent, currentState: KeyboardState): KeyboardStateTransition =
        when (intent) {
            is ImeIntent.OnKeyboard ->
                when (currentState) {
                    is KeyboardState.Idle -> {
                        when (intent.key) {
                            is InputKey.Char ->
                                handleCharKeyIntentWhenIdle(intent)

                            is InputKey.Ctrl ->
                                handleCtrlKeyIntentWhenIdle(intent = intent, state = currentState)

                            else -> null
                        }
                            ?: KeyboardStateTransition.ReturnToIdle
                    }

                    is KeyboardState.Pinyin.Inputting ->
                        handleIntentWhenInputting(intent)

                    is KeyboardState.Editor ->
                        handleIntentWhenEditorEditing(intent = intent, state = currentState)

                    else -> null
                }

            else -> null
        }
            ?: KeyboardStateTransition.NothingToDo

    // ----------------------------------------------------------------------

    /** 处理 [KeyboardState.Idle] 状态下的与 [InputKey.Char] 相关的 [ImeIntent] */
    private fun handleCharKeyIntentWhenIdle(
        intent: ImeIntent.OnKeyboard,
        //
        key: InputKey.Char = intent.key as InputKey.Char,
    ): KeyboardStateTransition? =
        // 仅关心对按键的操作
        when (key) {
            is InputKey.Char.Alphabet ->
                when (intent) {
                    is ImeIntent.OnKeyboard.Swipe.Begin ->
                        KeyboardStateTransition.Pinyin.StartInput(key)

                    is ImeIntent.OnKeyboard.Tap ->
                        KeyboardStateTransition.Char.Input(
                            key = key,
                            replacement = intent.tick,
                        )

                    else -> null
                }

            is InputKey.Char.Space,
            is InputKey.Char.Enter,
            is InputKey.Char.Emoji,
            is InputKey.Char.Symbol ->
                // TODO 滑行进入可替换字符选择状态？
                when (intent) {
                    is ImeIntent.OnKeyboard.LongPress.Hold ->
                        // 直接转由单击意图处理
                        handleCharKeyIntentWhenIdle(
                            intent = ImeIntent.OnKeyboard.Tap.Single(key)
                        )

                    is ImeIntent.OnKeyboard.Tap ->
                        KeyboardStateTransition.Char.Input(
                            key = key,
                            replacement = intent.tick,
                        )

                    else -> null
                }

            else -> null
        }

    /** 处理 [KeyboardState.Pinyin.Inputting] 状态下的 [ImeIntent] */
    private fun handleIntentWhenInputting(
        intent: ImeIntent.OnKeyboard,
        //
        key: InputKey.Char = intent.key as InputKey.Char,
    ): KeyboardStateTransition? =
    // Note：滑行结束时可能并未绑定按键，
    // 因此，必须从 intent 角度做分支处理，
        // 确保未绑定按键的 intent 能够正常生成对应的状态转换
        when (intent) {
            is ImeIntent.OnKeyboard.Swipe.Moving ->
                when (key) {
                    is InputKey.Char.Alphabet ->
                        KeyboardStateTransition.Pinyin.Inputting(key)

                    else -> null
                }

            is ImeIntent.OnKeyboard.Swipe.End ->
                KeyboardStateTransition.Pinyin.StopInput

            else -> null
        }

    /** 处理 [KeyboardState.Idle] 状态下的与 [InputKey.Ctrl] 相关的 [ImeIntent] */
    private fun handleCtrlKeyIntentWhenIdle(
        intent: ImeIntent.OnKeyboard, state: KeyboardState,
        //
        key: InputKey.Ctrl = intent.key as InputKey.Ctrl,
    ): KeyboardStateTransition? =
        when (key) {
            is InputKey.Ctrl.Backspace,
            is InputKey.Ctrl.Keyboard ->
                handleCtrlKeyIntent(intent = intent, state = state)

            is InputKey.Ctrl.Editor.MoveCursor ->
                handleEditorMoveCursorKeyIntentWhenIdle(intent = intent)

            else -> null
        }
}
