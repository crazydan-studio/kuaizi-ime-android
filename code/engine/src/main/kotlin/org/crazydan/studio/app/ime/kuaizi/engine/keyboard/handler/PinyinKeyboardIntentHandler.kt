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

package org.crazydan.studio.app.ime.kuaizi.engine.keyboard.handler

import org.crazydan.studio.app.ime.kuaizi.engine.ImeIntent
import org.crazydan.studio.app.ime.kuaizi.engine.keyboard.InputKey
import org.crazydan.studio.app.ime.kuaizi.engine.keyboard.KeyboardIntentHandler
import org.crazydan.studio.app.ime.kuaizi.engine.keyboard.KeyboardState
import org.crazydan.studio.app.ime.kuaizi.engine.keyboard.KeyboardStateTransition
import org.crazydan.studio.app.ime.kuaizi.engine.keyboard.KeyboardType

/**
 * 拼音键盘意图处理器，支持三种输入模式（点击、滑行、翻动），
 * 管理拼音字符输入、候选字查询和输入补全。
 *
 * @param type 键盘类型
 */
class PinyinKeyboardIntentHandler(override val type: KeyboardType) : KeyboardIntentHandler {

    override fun handleIntent(intent: ImeIntent, currentState: KeyboardState): KeyboardStateTransition =
        when (intent) {
            is ImeIntent.OnKey ->
                when (currentState) {
                    is KeyboardState.PinyinInput.Waiting -> {
                        when (intent.key) {
                            is InputKey.Char ->
                                handleCharKeyIntentWhenWaiting(intent)

                            is InputKey.Ctrl ->
                                TODO("对控制按键的处理")

                            else -> null
                        }
                            ?: KeyboardStateTransition.ReturnToIdle
                    }

                    is KeyboardState.PinyinInput.Swiping -> {
                        handleIntentWhenSwiping(intent, currentState)
                            ?: KeyboardStateTransition.NothingToDo
                    }

                    else -> KeyboardStateTransition.ReturnToIdle
                }

            else -> KeyboardStateTransition.ReturnToIdle
        }

    // ----------------------------------------------------------------------

    /** 处理 [KeyboardState.PinyinInput.Waiting] 状态下的与 [InputKey.Char] 相关的 [ImeIntent] */
    private fun handleCharKeyIntentWhenWaiting(
        intent: ImeIntent.OnKey,
        //
        key: InputKey.Char = intent.key as InputKey.Char,
    ): KeyboardStateTransition? =
        when (key) {
            is InputKey.Char.Alphabet ->
                when (intent) {
                    is ImeIntent.OnKey.Swipe
                        if intent.stage == ImeIntent.OnKey.Swipe.Stage.Begin
                        ->
                        KeyboardStateTransition.StartSwipe(key)

                    is ImeIntent.OnKey.Tap ->
                        KeyboardStateTransition.InputChar(
                            key = key,
                            replacement = intent.tick,
                        )

                    else -> null
                }

            is InputKey.Char.Emoji,
            is InputKey.Char.Symbol ->
                when (intent) {
                    // 仅支持对表情、符号做连续输入。
                    // 若对字符按键做连续输入，其会与滑行拼音输入发生冲突
                    is ImeIntent.OnKey.LongPress
                        if intent.stage == ImeIntent.OnKey.LongPress.Stage.Hold
                        ->
                        // 直接转由单击意图处理
                        handleCharKeyIntentWhenWaiting(ImeIntent.OnKey.Tap(key))

                    is ImeIntent.OnKey.Tap ->
                        KeyboardStateTransition.InputChar(
                            key = key,
                            replacement = intent.tick,
                        )

                    else -> null
                }

            is InputKey.Char.Number -> TODO("拼音键盘还未支持数字输入")
        }

    /** 处理 [KeyboardState.PinyinInput.Swiping] 状态下的 [ImeIntent] */
    private fun handleIntentWhenSwiping(
        intent: ImeIntent.OnKey,
        state: KeyboardState.PinyinInput.Swiping,
        //
        key: InputKey.Char = intent.key as InputKey.Char,
    ): KeyboardStateTransition? =
        when (key) {
            is InputKey.Char.Alphabet ->
                when (intent) {
                    is ImeIntent.OnKey.Swipe ->
                        when (intent.stage) {
                            ImeIntent.OnKey.Swipe.Stage.Moving
                                // 拼音的后继不会是相同字母
                                if key.value != state.lastKey.value
                                ->
                                KeyboardStateTransition.InputChar(key = key)

                            ImeIntent.OnKey.Swipe.Stage.End ->
                                KeyboardStateTransition.StopSwipe

                            else -> null
                        }

                    else -> null
                }

            else -> null
        }
}
