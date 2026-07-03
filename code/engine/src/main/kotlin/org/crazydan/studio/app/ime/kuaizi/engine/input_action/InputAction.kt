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

package org.crazydan.studio.app.ime.kuaizi.engine.input_action

import org.crazydan.studio.app.ime.kuaizi.engine.keyboard.InputKey
import org.crazydan.studio.app.ime.kuaizi.engine.keyboard.KeyboardType

/**
 * 输入动作的基类型，表达六种坐标无关的逻辑动作。
 *
 * 「坐标无关」设计是核心特性：所有动作只引用逻辑标识（[InputKey]、候选索引、[KeyboardType]），
 * 不包含任何像素坐标或屏幕尺寸信息。坐标解析由 [InputActionPositionResolver] 在回放时完成，
 * 使得同一份动作脚本可在不同面板尺寸和设备密度下正确回放。
 *
 * 所有子类共享 [startTime] 字段，表达该动作在脚本中的起始时间偏移量（毫秒），
 * 为回放调度器提供精确的时间控制依据。
 */
sealed class InputAction {
    abstract val startTime: Long

    /** 按下按键：表达手指按下某个按键的逻辑动作，与 [KeyUp] 配对构成完整的按键生命周期。 */
    data class KeyDown(
        override val startTime: Long,
        val key: InputKey,
    ) : InputAction()

    /**
     * 滑行到目标按键：表达手指从 [fromKey] 滑行到 [toKey] 的逻辑动作。
     * @param duration 滑行持续时间（毫秒），用于路径插值器计算中间帧位置
     */
    data class SwipeTo(
        override val startTime: Long,
        val fromKey: InputKey,
        val toKey: InputKey,
        val duration: Long,
    ) : InputAction()

    /** 抬起按键：表达手指从按键抬起的逻辑动作，与 [KeyDown] 配对。 */
    data class KeyUp(
        override val startTime: Long,
        val key: InputKey,
    ) : InputAction()

    /** 等待：表达两个动作之间的等待间隔，用于模拟用户的思考或反应时间。 */
    data class Wait(
        override val startTime: Long,
        val duration: Long,
    ) : InputAction()

    /**
     * 选择候选词：从候选列表中选择指定索引的候选词。
     * 回放时通过 [InputActionPositionResolver.resolveCandidatePosition] 将索引解析为屏幕坐标。
     */
    data class SelectCandidate(
        override val startTime: Long,
        val candidateIndex: Int,
    ) : InputAction()

    /** 切换键盘：切换到目标键盘类型的动作，回放时触发 [ImeIntent.SwitchKeyboard]。 */
    data class SwitchKeyboard(
        override val startTime: Long,
        val targetType: KeyboardType,
    ) : InputAction()
}
