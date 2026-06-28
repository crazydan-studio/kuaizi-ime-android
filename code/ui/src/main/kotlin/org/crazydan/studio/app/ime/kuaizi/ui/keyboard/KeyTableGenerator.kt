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

package org.crazydan.studio.app.ime.kuaizi.ui.keyboard

import org.crazydan.studio.app.ime.kuaizi.engine.ImeConfig
import org.crazydan.studio.app.ime.kuaizi.engine.domain.*

/**
 * 按键布局生成器接口。
 *
 * 根据 [KeyTableContext] 中的上下文信息（键盘类型、输入模式、候选列表等）
 * 生成按键布局矩阵 `List<List<InputKey>>`。
 *
 * 不同 [KeyboardInputMode] 和 [KeyboardType] 的组合可以注册不同的实现，
 * 由 [KeyLayoutPanel] 根据当前键盘状态选择合适的生成器。
 */
interface KeyTableGenerator {
    /**
     * 生成按键布局矩阵
     * @param context 按键生成上下文
     * @return 二维矩阵，外层 List 对应键盘的行，内层 List 对应每行中的按键
     */
    fun generate(context: KeyTableContext): List<List<InputKey>>
}

/**
 * 按键生成上下文，包含按键布局生成所需的全部信息。
 *
 * @param config 运行时配置（手模式、功能开关等）
 * @param keyboard 当前键盘实例（含 type、mode、state）
 * @param inputList 当前输入列表
 * @param candidateList 当前候选列表
 */
data class KeyTableContext(
    val config: ImeConfig,
    val keyboard: Keyboard,
    val inputList: InputList,
    val candidateList: CandidateList,
)
