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

package org.crazydan.studio.app.ime.kuaizi.engine.input.math

/**
 * 算术表达式输入列表
 *
 * @param inputs [MathInput.Gap]-[MathInput.Item] 交替排列的输入序列。
 * 始终不为空，且列表末尾始终多一个 [MathInput.Gap]
 * @param cursor 当前游标位置（指向 inputs 列表中的被选中输入的索引）
 * @param pending 待输入：正在输入还未更新到 [inputs] 的输入项
 */
data class MathInputList(
    val inputs: List<MathInput> = listOf(MathInput.Gap),
    val cursor: Int = 0,
    val pending: MathInput.Item? = null,
) {
    init {
        require(cursor >= 0 && cursor <= inputs.lastIndex)
    }

    /** 已选中输入项 */
    private val selected: MathInput
        get() = inputs[cursor]

    // ------------------------------------------

    /** 输入列表是否为空 */
    fun isEmpty(): Boolean =
        inputs.size == 1

    // ------------------------------------------

    fun select(index: Int): MathInputList =
        this

    // ------------------------------------------

    /**
     * 确认待输入：
     */
    fun confirmPending(): MathInputList =
        this

    // ------------------------------------------

    /**
     * 添加输入项：
     * - ；
     */
    fun addInput(input: MathInput.Item): MathInputList =
        this

    // ------------------------------------------

    /** 回删输入 */
    fun removeBackward(): MathInputList =
        this
}