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

package org.crazydan.studio.app.ime.kuaizi.engine.input

/**
 * 输入列表，管理用户输入的字符序列、游标位置、待确认输入和嵌套数学表达式。
 *
 * @param inputs [InputItem.Gap]-[InputItem.Char] 交替排列的输入序列。
 * 始终不为空，且列表末尾始终多一个 [InputItem.Gap]
 * @param cursor 当前游标位置（指向 inputs 列表中的被选中输入的索引）
 * @param pending 待输入
 * @param frozen 是否已被冻结：被冻结后，将不能对输入列表做修改
 */
data class InputList(
    val inputs: List<InputItem> = listOf(InputItem.Gap),
    val cursor: Int = 0,
    val pending: InputItem? = null,
    val frozen: Boolean = false,
) {
    init {
        require(cursor >= 0 && cursor <= inputs.lastIndex)
    }

    /** 输入列表是否为空 */
    val empty: Boolean
        get() = inputs.size == 1

    /** 已选中输入项 */
    private val selected: InputItem
        get() = inputs[cursor]

    val hasPending: Boolean
        get() = !inputs.isEmpty()

    /** 当前游标位置的 Gap 元素 */
    val cursorGap: InputItem.Gap
        get() = inputs.getOrElse(cursor) { InputItem.Gap } as InputItem.Gap

    /** 可见的字符输入列表（排除 Gap） */
    val visibleInputs: List<InputItem.Char>
        get() = inputs.filterIsInstance<InputItem.Char>()

    /** 输入文本内容，将 visibleInputs 的 text 字段拼接为完整字符串 */
    val text: String
        get() = visibleInputs.joinToString("") { it.value }

    val chars: List<InputItem.Char>
        get() = visibleInputs

    // ------------------------------------------------------

    /** 是否冻结输入列表？  */
    fun freeze(frozen: Boolean): InputList =
        copy(frozen = frozen)

    /** 清空输入列表 */
    fun clean(): InputList =
        InputList(inputs = listOf(InputItem.Gap), cursor = 0, pending = null)

    // ------------------------------------------------------

    /**
     * 选中指定位置输入项：
     * - 若指定位置已选中或者 [index] 不在有效范围，则不做处理；
     * - 若在其他位置，则先 [confirmPending] 确认当前待输入，再做选中；
     */
    fun select(index: Int): InputList =
        if (cursor == index || index < 0 || index > inputs.lastIndex) this
        else
            confirmPending().let {
                it.copy(
                    cursor =
                        if (index < cursor) index
                        // 加上后移偏移量：可能为正，可能为负
                        else index + (it.inputs.size - inputs.size),
                    pending = null,
                )
            }

    // ------------------------------------------------------

    /** 更新待输入：直接替换当前的待输入 */
    fun updatePending(input: InputItem): InputList =
        copy(pending = input)

    /** 丢弃待输入 */
    fun dropPending(): InputList =
        copy(pending = null)

    /** 确认待输入 */
    fun confirmPending(): InputList {
        if (pending == null) return this
        if (InputItem.isEmpty(pending)) return dropPending()

        return when (selected) {
            is InputItem.Gap -> {
                // 在当前游标位置新增 Gap-Char 对
                copy(
                    inputs = updateInputs {
                        add(cursor, InputItem.Gap)
                        add(cursor + 1, pending)
                    },
                    pending = null,
                    cursor = cursor + 2,
                )
            }

            is InputItem.Char -> {
                // 直接原地替换
                copy(
                    inputs = updateInputs { set(cursor, pending) },
                    pending = null,
                    cursor = cursor + 1,
                )
            }

            else -> this
        }
    }

    // ---------------------------------------------------------

    /** 添加字符：替换 [pending]，或向 [pending] 追加字符，[cursor] 位置不变 */
    fun addChar(char: InputItem.Char, replacements: List<String>?): InputList =
        when (char) {
            // 非拉丁输入直接替换
            is InputItem.Char.Space,
            is InputItem.Char.Emoji,
            is InputItem.Char.Symbol
                ->
                // TODO 处理配对符号
                when (selected) {
                    // 替换输入
                    is InputItem.Gap
                        if (cursor > 1 && InputItem.isEmpty(pending) && replacements != null)
                        -> {
                        inputs[cursor - 1].let { prev ->
                            when (prev) {
                                is InputItem.Char if (replacements.contains(prev.value))
                                    ->
                                    copy(
                                        inputs = updateInputs { set(cursor - 1, char) },
                                        pending = null,
                                    )

                                else -> null
                            }
                        } ?: copy(pending = char)
                    }

                    else -> copy(pending = char)
                }

            is InputItem.Char.Latin ->
                when (pending) {
                    is InputItem.Char.Latin if (!InputItem.isEmpty(pending))
                        ->
                        copy(
                            pending = pending.appendChar(
                                char = char.chars[0],
                                replacements = replacements,
                            )
                        )

                    else -> copy(pending = char)
                }
        }

    // ---------------------------------------------------------

    /**
     * 删除已选中输入项：
     * - 若 [selected] 为 [InputItem.Gap]，则 [dropPending] 即可；
     * - 否则，执行 [deleteBackward]；
     */
    fun deleteSelected(): InputList =
        when (selected) {
            is InputItem.Gap -> dropPending()
            else -> doDeleteBackward(false)
        }

    /**
     * 回删输入（项）：
     * - 若 [pending] 或 [selected]（待输入为空时）为 [InputItem.Char.Latin]，则从其尾部逐字符删除；
     * - 若 [selected] 为 [InputItem.Gap]，则删除其前面的输入项；
     * - 若 [selected] 为配对符号输入，则同时删除其另一侧的符号输入项；
     * - 否则，直接删除 [selected]；
     */
    fun deleteBackward(): InputList =
        doDeleteBackward(true)

    private fun doDeleteBackward(oneByOne: Boolean): InputList {
        if (oneByOne) {
            val emptyPending = InputItem.isEmpty(pending)
            val current = if (emptyPending) selected else pending

            if (current is InputItem.Char.Latin && current.chars.size > 1) {
                return copy(
                    inputs =
                        if (emptyPending) updateInputs { set(cursor, current.dropLastChar()) }
                        else inputs,
                    pending =
                        if (emptyPending) null
                        else current.dropLastChar(),
                )
            }
        }

        return this
    }

    // ------------------------------------------

    private fun updateInputs(block: MutableList<InputItem>.() -> Unit): List<InputItem> =
        inputs.toMutableList().apply(block)

}
