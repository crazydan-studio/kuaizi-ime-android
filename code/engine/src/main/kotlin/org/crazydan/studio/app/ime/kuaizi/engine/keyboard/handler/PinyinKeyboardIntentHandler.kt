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
        when (currentState) {
            is KeyboardState.PinyinInput.Waiting -> {
                when (intent) {
                    is ImeIntent.PressOnKey -> {
                        when (intent.key) {
                            is InputKey.Char -> KeyboardStateTransition.InputPinyinChar(intent.key.text.first())
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
                handleIntentWhenSlipping(intent, currentState)
            }

            is KeyboardState.PinyinInput.Flipping -> {
                when (intent) {
                    is ImeIntent.PressOnKey -> KeyboardStateTransition.SelectFlipChar((intent.key as InputKey.Char).text.first())
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

    // ----------------------------------------------------------------------

    /** 处理滑屏输入状态下的 [ImeIntent] */
    private fun handleIntentWhenSlipping(
        intent: ImeIntent,
        state: KeyboardState.PinyinInput.Slipping,
    ): KeyboardStateTransition =
        when (intent) {
            is ImeIntent.PressOnKey -> KeyboardStateTransition.BeginSlip(intent.key)
            else -> KeyboardStateTransition.ReturnToIdle
        }
}
