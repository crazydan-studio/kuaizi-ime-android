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

package org.crazydan.studio.app.ime.kuaizi.ui.viewmodel

import org.crazydan.studio.app.ime.kuaizi.engine.domain.*

/**
 * 输入手势，坐标无关。
 *
 * 由输入面板识别后发送到 ViewModel，描述用户的输入意图。
 * 不包含任何绝对坐标，只包含按键的语义标识。
 * 包含 [inputMode] 参数，使手势识别逻辑可根据不同输入模式采用不同策略。
 */
sealed class InputGesture {
    /** 手势发生的时间戳（毫秒） */
    abstract val timestamp: Long

    /** 产生此手势的输入模式 */
    abstract val inputMode: KeyboardInputMode

    /**
     * 点击按键
     * @param key 目标按键
     * @param tick 连续点击同一按键的次数（0=首次，1=双击...）
     */
    data class Tap(
        override val timestamp: Long,
        override val inputMode: KeyboardInputMode,
        val key: InputKey,
        val tick: Int = 0,
    ) : InputGesture()

    /** 长按按键 */
    data class LongPress(
        override val timestamp: Long,
        override val inputMode: KeyboardInputMode,
        val key: InputKey,
    ) : InputGesture()

    /**
     * 滑行输入
     * @param startKey 起始按键
     * @param endKey 结束按键
     * @param visitedKeys 途经按键序列
     * @param duration 滑行持续时间
     */
    data class Swipe(
        override val timestamp: Long,
        override val inputMode: KeyboardInputMode,
        val startKey: InputKey,
        val endKey: InputKey,
        val visitedKeys: List<InputKey>,
        val duration: Long,
    ) : InputGesture()

    /** 翻转手势（快速滑行后松手） */
    data class Flip(
        override val timestamp: Long,
        override val inputMode: KeyboardInputMode,
        val startKey: InputKey,
        val direction: FlipDirection,
    ) : InputGesture()

    /** 候选项选择 */
    data class CandidateTap(
        override val timestamp: Long,
        override val inputMode: KeyboardInputMode,
        val candidateIndex: Int,
    ) : InputGesture()
}
