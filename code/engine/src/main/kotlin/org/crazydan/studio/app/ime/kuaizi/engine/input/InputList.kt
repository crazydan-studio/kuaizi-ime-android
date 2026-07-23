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

import org.crazydan.studio.app.ime.kuaizi.engine.input.math.MathInputList

/**
 * 输入列表，管理用户输入的字符序列、游标位置、待确认输入。
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
    private val frozen: Boolean = false,
) {
    init {
        require(cursor >= 0 && cursor <= inputs.lastIndex)
    }

    /**
     * 已选中输入项：
     * - 若 [selected] 为 [InputItem.MathExpr]，则 [pending] 始终为 `null`；
     */
    private val selected: InputItem
        get() = inputs[cursor]

    // ------------------------------------------------------

    /** 是否冻结输入列表？  */
    fun freeze(frozen: Boolean): InputList =
        if (this.frozen == frozen) this
        else copy(frozen = frozen)

    /** 输入列表是否已被冻结 */
    fun isFrozen(): Boolean =
        frozen

    /** 清空输入列表 */
    fun clean(): InputList =
        InputList(inputs = listOf(InputItem.Gap), cursor = 0, pending = null)

    /** 输入列表是否为空 */
    fun isEmpty(): Boolean =
        inputs.size == 1

    /** 待输入是否为空 */
    fun hasEmptyPending(): Boolean =
        pending == null || pending.isEmpty()

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
                it.doSelectAt(
                    if (index < cursor) index
                    // 加上后移偏移量：可能为正，可能为负
                    else index + (it.inputs.size - inputs.size)
                )
            }

    // ------------------------------------------------------

    /** 更新待输入：直接替换当前的待输入 */
    fun updatePending(input: InputItem): InputList =
        copy(pending = input)

    /** 丢弃待输入 */
    fun dropPending(): InputList =
        if (pending == null) this
        else copy(pending = null)

    /**
     * 确认待输入：
     * - 若 [pending] 为空，则 [dropPending]，并在 [selected] 不为 [InputItem.Gap] 时将 [cursor] 后移一位，
     *   若 [selected] 为 [InputItem.MathExpr] 还需要先确认算术输入列表的待输入；
     * - 若 [selected] 为 [InputItem.Gap]，则将 [pending] 的 Gap-Char 对插入到 [selected] 之前；
     * - 否则，直接以 [pending] 覆盖 [selected]；
     *
     * [cursor] 始终指向 Gap 位。
     */
    fun confirmPending(): InputList {
        // Note：在 selected 为算术输入时，pending 始终为 null
        if (hasEmptyPending()) {
            return when (selected) {
                is InputItem.Gap ->
                    dropPending()

                is InputItem.MathExpr ->
                    withMathExprUpdate { confirmPending() }
                        .copy(cursor = cursor + 1)

                else ->
                    copy(
                        pending = null,
                        cursor = cursor + 1,
                    )
            }
        }

        return when (selected) {
            // 插入 Gap-Char 对
            is InputItem.Gap ->
                applyInputsUpdate(cursor + 2) {
                    addAll(cursor, listOf(InputItem.Gap, pending!!))
                }

            // 原地覆盖
            else ->
                applyInputsUpdate(cursor + 1) {
                    set(cursor, pending!!)
                }
        }
    }

    // ---------------------------------------------------------

    /**
     * 添加字符：
     * - 若 [char] 为 [InputItem.Char.Latin]，则在 [pending] 也为 [InputItem.Char.Latin] 时，
     *   向其追加字符或替换尾部字符，否则，直接用 [char] 覆盖 [pending]；
     * - 若 [char] 为配对符号，则执行 [doAddPairSymbolChar]；
     * - 若 [pending] 为 [InputItem.Char.Latin]，则先 [confirmPending] 再继续 [addChar]；
     * - 若 [selected] 为 [InputItem.Gap]，则按 [replacements] 替换前序输入或者插入 Gap-Char 对；
     * - 否则，用 [char] 覆盖 [selected]；
     *
     * 仅当 [char] 为 [InputItem.Char.Latin] 时 [cursor] 指向 Char 位，其余情况均将 [cursor] 指向 Gap 位。
     *
     * 不管 [pending] 或 [selected] 是否为 [InputItem.MathExpr]，均按以上规则处理，
     * 对算术输入自身的更新需通过 [withMathExprUpdate] 处理。
     */
    fun addChar(char: InputItem.Char, replacements: List<String>?): InputList =
        when (char) {
            is InputItem.Char.Latin -> when (pending) {
                // 只有 latin 支持连续输入，其余单字符输入都是边输入边确认的，若其被选中，则必然是对其做覆盖
                is InputItem.Char.Latin ->
                    copy(
                        pending = pending.appendChar(
                            char = char.chars[0],
                            replacements = replacements,
                        )
                    )

                else -> copy(pending = char)
            }

            // 配对符号需做替换和包裹处理
            is InputItem.Char.Symbol if char.right != null ->
                doAddPairSymbolChar(char)

            // 单字符输入
            else -> when (pending) {
                // 确认 latin 输入后，再继续新增单字符
                is InputItem.Char.Latin ->
                    confirmPending().addChar(char, replacements)

                else -> when (selected) {
                    is InputItem.Gap -> {
                        val prev = inputs.getOrNull(cursor - 1)

                        // 替换前序 Char
                        if (prev is InputItem.Char && replacements?.contains(prev.value) == true)
                            applyInputsUpdate {
                                set(cursor - 1, char)
                            }
                        // 插入 Gap-Char 对
                        else
                            applyInputsUpdate(cursor + 2) {
                                addAll(cursor, listOf(InputItem.Gap, char))
                            }
                    }

                    // 原地覆盖
                    else ->
                        applyInputsUpdate(cursor + 1) {
                            set(cursor, char)
                        }
                }
            }
        }

    /**
     * 添加配对符号：
     * - 若 [pending] 为空且 [selected] 为 [InputItem.Gap]，则插入配对符号；
     * - 若 [pending] 为空且 [selected] 为配对符号（两端均存在），则替换该配对符号；
     * - 否则以配对符号包裹 [pending] 或 [selected]；
     *
     * [cursor] 始终指向 Gap 位。
     */
    private fun doAddPairSymbolChar(symbol: InputItem.Char.Symbol): InputList {
        val left = symbol
        val right = symbol.right!!

        val selected = this.selected
        val emptyPending = hasEmptyPending()

        if (emptyPending && selected is InputItem.Gap) {
            return applyInputsUpdate(cursor + 2) {
                addAll(cursor, listOf(InputItem.Gap, left, InputItem.Gap, right))
            }
        } //
        else if (emptyPending && selected is InputItem.Char.Symbol) {
            val pairIndex = indexOfPairSymbol(selected)

            // 替换原配对符号
            if (pairIndex >= 0) {
                return applyInputsUpdate(cursor + 1) {
                    // 选中的是左侧符号
                    if (cursor < pairIndex) {
                        set(cursor, left)
                        set(pairIndex, right)
                    }
                    // 选中的是右侧符号
                    else {
                        set(cursor, right)
                        set(pairIndex, left)
                    }
                }
            }
        }

        // 先确认待输入，再包裹该已确认的输入项（确认后，其 cursor 必然在该输入项之后的 Gap 位置）
        return confirmPending().run {
            // Note: cursor 为确认待输入后的游标
            applyInputsUpdate(cursor + 1) {
                // 先插入右侧符号，再插入左侧符号，以避免其 selected 的位置发生变动
                addAll(cursor, listOf(InputItem.Gap, right))
                addAll(cursor - 2, listOf(InputItem.Gap, left))
            }
        }
    }

    // ---------------------------------------------------------

    /**
     * 删除已选中输入项：
     * - 若 [selected] 为 [InputItem.Gap]，则 [dropPending] 即可；
     * - 否则，执行 [doRemoveBackward] (false)；
     */
    fun removeSelected(): InputList =
        when (selected) {
            is InputItem.Gap -> dropPending()
            else -> doRemoveBackward(false)
        }

    /** 回删输入（项），执行 [doRemoveBackward] (true) */
    fun removeBackward(): InputList =
        doRemoveBackward(true)

    /**
     * 回删输入（项）：
     * - 若 [pending] 或 [selected]（待输入为空时）为 [InputItem.Char.Latin]，则从其尾部逐字符删除；
     * - 若 [selected] 为 [InputItem.Gap]，则 [dropPending] 或删除其前序输入项（包括前序可能的配对符号）；
     * - 否则，直接删除 [selected]（包括其配对符号）；
     *
     * 自动根据 [inputs] 缺口偏移 [cursor] 位置。
     *
     * 不管 [pending] 或 [selected] 是否为 [InputItem.MathExpr]，均按以上规则处理，
     * 对算术输入自身的更新需通过 [withMathExprUpdate] 处理。
     */
    private fun doRemoveBackward(oneByOne: Boolean): InputList {
        val selected = this.selected
        val emptyPending = hasEmptyPending()

        if (oneByOne) {
            val current = if (emptyPending) selected else pending

            if (current is InputItem.Char.Latin && current.chars.size > 1) {
                val char = current.dropLastChar()

                return applyInputsUpdate(
                    pending = if (emptyPending) null else char,
                ) {
                    if (emptyPending) set(cursor, char)
                }
            }
        }

        return when (selected) {
            // 删除 Gap 之前的输入项或该 Gap 的待输入
            is InputItem.Gap ->
                if (cursor == 0 || !emptyPending)
                    dropPending()
                else {
                    val prevIndex = cursor - 1
                    val prev = inputs[prevIndex]

                    // 若前序为包含多个字符的 Latin，则对其做选中，以支持后续逐字符删除
                    if (prev is InputItem.Char.Latin && prev.chars.size > 1)
                        doSelectAt(prevIndex)
                    else
                        doRemovePairSymbol(prev).doRemoveNonGap(prev)
                }
            // 删除当前选中输入项及其 Gap 位
            else ->
                doRemovePairSymbol(selected).doRemoveNonGap(selected)
        }
    }

    // ------------------------------------------

    /**
     * 在 [InputItem.MathExpr] 上进行更新：
     * - 若 [pending] 或 [selected] 为 [InputItem.MathExpr] 类型，
     *   则直接对其 [MathInputList] 进行更新操作；
     * - 否则，新建 [InputItem.MathExpr] 类型的 [pending]，
     *   再在该 [pending] 的 [MathInputList] 上进行更新操作；
     */
    fun withMathExprUpdate(block: MathInputList.() -> MathInputList): InputList =
        when (pending) {
            is InputItem.MathExpr ->
                copy(
                    pending = pending.applyInputListUpdate(block)
                )

            else -> {
                val selected = this.selected
                when (selected) {
                    is InputItem.MathExpr ->
                        applyInputsUpdate {
                            set(cursor, selected.applyInputListUpdate(block))
                        }

                    else ->
                        copy(
                            pending = InputItem.MathExpr().applyInputListUpdate(block),
                        )
                }
            }
        }

    // ------------------------------------------

    /** 移动 [cursor] 并更新 [inputs] */
    private fun applyInputsUpdate(
        cursor: Int = this.cursor,
        pending: InputItem? = null,
        block: MutableList<InputItem>.() -> Unit,
    ): InputList =
        copy(
            inputs = inputs.toMutableList().apply(block),
            pending = pending,
            cursor = cursor,
        )

    /** 选中指定位置的输入项，并置空 [pending] */
    private fun doSelectAt(index: Int): InputList =
        copy(cursor = index, pending = null)

    /**
     * 删除指定的非 Gap 输入项。
     * 自动根据 [inputs] 缺口偏移 [cursor] 位置。
     */
    private fun doRemoveNonGap(input: InputItem): InputList =
        when (input) {
            is InputItem.Gap -> this
            else ->
                inputs.indexOf(input).let { index ->
                    if (index < 0) this
                    else doRemoveNonGapAt(index)
                }
        }

    /**
     * 删除指定位置的非 Gap 输入项。
     * 自动根据 [inputs] 缺口偏移 [cursor] 位置。
     */
    private fun doRemoveNonGapAt(index: Int): InputList =
        when (inputs.getOrNull(index)) {
            null, is InputItem.Gap -> this
            else ->
                applyInputsUpdate(
                    cursor =
                        if (cursor < index - 1) cursor
                        else if (cursor > index) cursor - 2
                        else index - 1,
                    pending =
                        if (cursor >= index - 1 && cursor <= index) null
                        else pending,
                ) {
                    // 目标输入项
                    removeAt(index)
                    // 与之配对的 Gap
                    removeAt(index - 1)
                }
        }

    /**
     * 删除指定输入项（仅针对 [InputItem.Char.Symbol]）的对端配对符号输入项。
     * 自动根据 [inputs] 缺口偏移 [cursor] 位置。
     *
     * 若没有配对的符号输入项，则不做处理。
     */
    private fun doRemovePairSymbol(input: InputItem): InputList =
        when (input) {
            !is InputItem.Char.Symbol -> this
            else ->
                indexOfPairSymbol(input).let { index ->
                    if (index < 0) this
                    else doRemoveNonGapAt(index)
                }
        }

    /** 查找指定符号输入项的配对符号输入项的序号 */
    private fun indexOfPairSymbol(symbol: InputItem.Char.Symbol): Int =
        if (symbol.right != null)
            inputs.indexOf(symbol.right)
        else
            inputs.indexOf(symbol).let { index ->
                // 向左查找
                for (i in index - 1 downTo 0) {
                    val left = inputs[i]

                    if (left is InputItem.Char.Symbol && left.right == symbol) {
                        return@let i
                    }
                }
                return@let -1
            }
}
