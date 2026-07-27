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
 * 输入列表
 * - 在列表 [inputs] 中为成对的 [CommonInput.Gap]-[CommonInput.Item]（`Gap-Item`），
 *   且在该列表末尾始终放置一个 Gap，从而确保列表头、尾和 Item 之间的间隙均有一个 Gap，
 *   以方便自由定位列表中具体的输入项以及输入项之间的空隙；
 * - 通过游标 [cursor] 实现对 Gap 或 Item 的定位，从而在指向的 Gap 位置插入新的 Gap-Item 对，
 *   或者对指向的 Item 做替换、修改和删除；
 * - 待输入 [pending] 用于承载对 [cursor] 指向的 Item 的持续性修改（主要针对
 *   [CommonInput.Item.Char.Latin] 和 [CommonInput.Item.MathExpr]），
 *   从而避免频繁更新 [inputs] 列表。而由于 Gap 位置只能做 Gap-Item 对的插入，
 *   因此，当 [cursor] 指向 Gap 时，[pending] 将始终为 `null`；
 *
 * @param inputs Gap-Item 交替排列的输入序列。始终不为空
 * @param cursor 当前游标位置（指向 inputs 列表中的 Gap 或 Item 的索引）
 * @param pending 待输入
 * @param frozen 是否已被冻结：被冻结后，将不能对输入列表做修改
 */
data class InputList(
    val inputs: List<CommonInput> = listOf(CommonInput.Gap),
    val cursor: Int = 0,
    val pending: CommonInput.Item? = null,
    private val frozen: Boolean = false,
) {
    init {
        require(cursor >= 0 && cursor <= inputs.lastIndex)
    }

    /** 已选中输入项：[cursor] 指向的 [CommonInput.Gap] 或 [CommonInput.Item] */
    private val selected: CommonInput
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
        InputList(inputs = listOf(CommonInput.Gap), cursor = 0, pending = null)

    /** 输入列表是否为空：只包含唯一的 Gap 时，该输入列表即为空 */
    fun isEmpty(): Boolean =
        inputs.size == 1

    // ------------------------------------------------------

    /**
     * 选中指定位置的输入项：
     * - 若指定位置已选中或者 [index] 不在有效范围，则不做处理；
     * - 否则，先 [confirmPending]，再做 [selectAt]，
     *   并对 [cursor] 加上偏移量（[inputs] 列表长度可能变短）。
     *   注意，[pending] 将被重置为 `null`；
     */
    fun select(index: Int): InputList =
        if (cursor == index || index < 0 || index > inputs.lastIndex)
            this
        else
            confirmPending().let {
                it.selectAt(
                    if (index < cursor) index
                    // 加上偏移量
                    else index + (it.inputs.size - inputs.size)
                )
            }

    // ------------------------------------------------------

    /** 更新待输入：直接替换 [pending] */
    fun updatePending(input: CommonInput.Item): InputList =
        copy(pending = input)

    /** 丢弃待输入：重置 [pending] 为 `null` */
    fun dropPending(): InputList =
        if (pending == null) this
        else copy(pending = null)

    /**
     * 确认 [pending]，将其更新到 [inputs]：
     * - 若 [selected] 为 [CommonInput.Gap]，则不做处理，因为，Gap 位置不对应任何输入项；
     * - 否则，若 [pending] 为 `null`，则将 [cursor] 后移一位，从而指向 Gap 位，
     *   以等待在该位置插入新的输入项；
     * - 否则，若 [pending] 为 [CommonInput.Item.MathExpr]，则先确认算术表达式的待输入，再：
     *   - 若算术表达式确认后，该 [pending] 为空，则执行 [doRemoveNonGapAt] 将 [cursor] 指向的算术表达式移除；
     *   - 否则，将 [selected] 替换为 [pending]，并将 [cursor] 后移一位，等待插入新的输入项；
     * - 否则，将 [selected] 替换为 [pending]，并将 [cursor] 后移一位，等待插入新的输入项；
     *
     * 注意：
     * - [cursor] 始终指向 Gap 位，且 [pending] 为 `null`；
     * - [inputs] 长度仅在删除空的算术表达式输入项时才变化，其余情况均不会发生变化；
     */
    fun confirmPending(): InputList {
        val selected = this.selected
        if (selected is CommonInput.Gap) {
            return this
        }

        return when (pending) {
            null ->
                selectAt(cursor + 1)

            is CommonInput.Item.MathExpr ->
                withMathExprUpdate {
                    confirmPending()
                }.run {
                    if (pending.isEmpty())
                        doRemoveNonGapAt(cursor)
                    else
                        applyInputsUpdate(cursor + 1) {
                            set(cursor, pending!!)
                        }
                }

            else ->
                applyInputsUpdate(cursor + 1) {
                    set(cursor, pending)
                }
        }
    }

    // ---------------------------------------------------------

    /**
     * 添加输入项：
     * - 若 [input] 为 [CommonInput.Item.Char.Latin]，则在 [pending] 也为 [CommonInput.Item.Char.Latin] 时，
     *   向其追加字符或替换尾部字符，否则，直接用 [input] 覆盖 [pending]；
     * - 若 [input] 为配对符号，则执行 [doAddPairSymbolChar]；
     * - 若 [pending] 为 [CommonInput.Item.Char.Latin]，则先 [confirmPending] 再继续 [addInput]；
     * - 若 [selected] 为 [CommonInput.Gap]，则按 [replacements] 替换前序输入或者插入 Gap-Item 对；
     * - 否则，用 [input] 替换 [selected]；
     *
     * 仅当 [input] 为 [CommonInput.Item.Char.Latin] 时 [cursor] 指向 Item 位，
     * 其余情况均将 [cursor] 指向 Gap 位，且 [pending] 为 `null`。
     *
     * 不管 [pending] 或 [selected] 是否为 [CommonInput.Item.MathExpr]，均按以上规则处理，
     * 对算术输入自身的更新需通过 [withMathExprUpdate] 处理。
     */
    fun addInput(input: CommonInput.Item.Char, replacements: List<String>?): InputList =
        when (input) {
            is CommonInput.Item.Char.Latin -> when (pending) {
                // 只有 latin 支持连续输入，其余单字符输入都是边输入边确认的，若其被选中，则必然是对其做覆盖
                is CommonInput.Item.Char.Latin ->
                    copy(
                        pending = pending.appendChar(
                            char = input.chars[0],
                            replacements = replacements,
                        )
                    )

                else -> copy(pending = input)
            }

            // 配对符号需做替换和包裹处理
            is CommonInput.Item.Char.Symbol if input.right != null ->
                doAddPairSymbolChar(input)

            // 单字符输入
            else -> when (pending) {
                // 确认 latin 输入后，再继续新增单字符
                is CommonInput.Item.Char.Latin ->
                    confirmPending().addInput(input, replacements)

                else -> when (selected) {
                    is CommonInput.Gap -> {
                        val prev = inputs.getOrNull(cursor - 1)

                        // 替换前序 Char
                        if (prev is CommonInput.Item.Char && replacements?.contains(prev.value) == true)
                            applyInputsUpdate {
                                set(cursor - 1, input)
                            }
                        // 插入 Gap-Item 对
                        else
                            applyInputsUpdate(cursor + 2) {
                                addAll(cursor, listOf(CommonInput.Gap, input))
                            }
                    }

                    // TODO 处理配对符号：只有配对符号可相互替换，否则，只能新增
                    // 原地替换
                    else ->
                        applyInputsUpdate(cursor + 1) {
                            set(cursor, input)
                        }
                }
            }
        }

    /**
     * 添加配对符号：
     * - 若 [pending] 为空且 [selected] 为 [CommonInput.Gap]，则插入配对符号；
     * - 若 [pending] 为空且 [selected] 为配对符号（两端均存在），则替换该配对符号；
     * - 否则以配对符号包裹 [pending] 或 [selected]；
     *
     * [cursor] 始终指向 Gap 位。
     */
    private fun doAddPairSymbolChar(symbol: CommonInput.Item.Char.Symbol): InputList {
        val left = symbol
        val right = symbol.right!!

        val selected = this.selected
        val emptyPending = pending.isEmpty()

        if (emptyPending && selected is CommonInput.Gap) {
            return applyInputsUpdate(cursor + 2) {
                addAll(cursor, listOf(CommonInput.Gap, left, CommonInput.Gap, right))
            }
        } //
        else if (emptyPending && selected is CommonInput.Item.Char.Symbol) {
            val pairIndex = indexOfPairInputAt(selected)

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
            // 包裹后，需将游标放在包裹项之后
            applyInputsUpdate(cursor + 2) {
                // 先插入右侧符号，再插入左侧符号，以避免其 selected 的位置发生变动
                addAll(cursor, listOf(CommonInput.Gap, right))
                addAll(cursor - 2, listOf(CommonInput.Gap, left))
            }
        }
    }

    // ---------------------------------------------------------

    /**
     * 删除已选中输入项：
     * - 若 [selected] 为 [CommonInput.Gap]，则 [dropPending] 即可；
     * - 否则，执行 [doRemoveBackward]（`oneByOne=false`）；
     */
    fun removeSelected(): InputList =
        doRemoveBackward(false)

    /** 回删输入（项），执行 [doRemoveBackward]（`oneByOne=true`） */
    fun removeBackward(): InputList =
        doRemoveBackward(true)

    /**
     * 回删输入：
     * - 若 [pending] 或 [selected] 为包含多个字符的 [CommonInput.Item.Char.Latin]
     *   且 [oneByOne]=`true` 时，则删除其尾部字符；
     * - 否则，若 [selected] 为 [CommonInput.Gap]，则：
     *   - 若 [cursor] 为 `0`，则不做处理；
     *   - 否则，若 [cursor] 前序输入项为包含多个字符的 [CommonInput.Item.Char.Latin]，则执行 [selectAt] 以将其选中，从而等待后续处理；
     *   - 否则，执行 [tryRemovePairInputAt] 先尝试删除前序输入项的配对输入项，再执行 [doRemoveNonGap] 以删除前序输入项；
     * - 否则，执行 [tryRemovePairInputAt] 先尝试删除 [selected] 的配对输入项，再执行 [doRemoveNonGap] 以删除 [selected]；
     *
     * 对于 Latin 输入项会尝试逐字符删除，其余类型的输入项则将被直接删除，[cursor] 也将指向删除后的空隙（Gap）。
     * 对算术输入自身的更新需通过 [withMathExprUpdate] 处理。
     */
    private fun doRemoveBackward(oneByOne: Boolean): InputList {
        val selected = this.selected
        val emptyPending = pending.isEmpty()

        if (oneByOne) {
            val current = if (emptyPending) selected else pending

            if (current is CommonInput.Item.Char.Latin && current.chars.size > 1) {
                return copy(pending = current.dropLastChar())
            }
        }

        return when (selected) {
            is CommonInput.Gap ->
                if (cursor == 0) this
                else {
                    val prevIndex = cursor - 1
                    val prev = inputs[prevIndex]

                    if (prev is CommonInput.Item.Char.Latin && prev.chars.size > 1)
                        selectAt(prevIndex)
                    else
                        tryRemovePairInputAt(prevIndex).doRemoveNonGap(prev)
                }

            else ->
                tryRemovePairInputAt(cursor).doRemoveNonGap(selected)
        }
    }

    /**
     * 尝试删除指定位置的输入项的配对输入项：
     * - 若 [sourceIndex] 位置的输入项没有配对输入项，则不做处理；
     * - 否则，执行 [doRemoveNonGapAt] 以按位置删除对应的配对输入项；
     */
    private fun tryRemovePairInputAt(sourceIndex: Int): InputList =
        indexOfPairInputAt(sourceIndex).let { targetIndex ->
            if (targetIndex < 0) this
            else doRemoveNonGapAt(targetIndex)
        }

    /**
     * 删除指定的非 Gap 输入：
     * - 若 [input] 为 [CommonInput.Gap]，则不做处理；
     * - 否则，若 [input] 不在 [inputs] 内，则不做处理；
     * - 否则，执行 [doRemoveNonGapAt] 以按位置删除 [input] 的 Gap-Item 对；
     */
    private fun doRemoveNonGap(input: CommonInput): InputList =
        when (input) {
            is CommonInput.Gap -> this
            else ->
                inputs.indexOf(input).let { index ->
                    if (index < 0) this
                    else doRemoveNonGapAt(index)
                }
        }

    /**
     * 删除指定位置的非 Gap 输入（即，Item 输入项）：
     * - 若 [index] 位置的输入为 `null` 或 [CommonInput.Gap]，则不做处理；
     * - 否则，删除 [index] 位置的 Gap-Item 对，并根据 [inputs] 缺口偏移 [cursor] 的位置；
     */
    private fun doRemoveNonGapAt(index: Int): InputList =
        when (inputs.getOrNull(index)) {
            null, is CommonInput.Gap -> this
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
                    // 与之成对的 Gap
                    removeAt(index - 1)
                }
        }

    // ------------------------------------------

    /**
     * 在当前的 [CommonInput.Item.MathExpr] 待输入上进行更新：
     * - 若 [pending] 或 [selected] 为 [CommonInput.Item.MathExpr] 类型，
     *   则直接对其 [MathInputList] 进行更新操作；
     * - 否则，新建 [CommonInput.Item.MathExpr]，再对其 [MathInputList] 上进行更新操作；
     *
     * 最终的更新结果将挂载到 [pending] 上，以便于对该算术表达式输入项进行后续操作。
     */
    fun withMathExprUpdate(block: MathInputList.() -> MathInputList): InputList =
        when (pending) {
            is CommonInput.Item.MathExpr -> pending
            else -> selected.let {
                when (it) {
                    is CommonInput.Item.MathExpr -> it
                    else -> CommonInput.Item.MathExpr()
                }
            }
        }.let {
            copy(
                pending = it.applyInputListUpdate(block)
            )
        }

    // ------------------------------------------

    /** 移动 [cursor] 并更新 [inputs] */
    private fun applyInputsUpdate(
        cursor: Int = this.cursor,
        pending: CommonInput.Item? = null,
        block: MutableList<CommonInput>.() -> Unit,
    ): InputList =
        copy(
            inputs = inputs.toMutableList().apply(block),
            pending = pending,
            cursor = cursor,
        )

    /** 选中指定位置的输入项，并置空 [pending] */
    private fun selectAt(index: Int): InputList =
        copy(cursor = index, pending = null)

    /** 查找指定符号输入项的配对符号输入项的序号 */
    private fun indexOfPairInputAt(symbol: CommonInput.Item.Char.Symbol): Int =
        if (symbol.right != null)
            inputs.indexOf(symbol.right)
        else
            inputs.indexOf(symbol).let { index ->
                // 向左查找
                for (i in index - 1 downTo 0) {
                    val left = inputs[i]

                    if (left is CommonInput.Item.Char.Symbol && left.right == symbol) {
                        return@let i
                    }
                }
                return@let -1
            }

    /** 查找指定位置输入项的配对符号输入项的序号 */
    private fun indexOfPairInputAt(sourceIndex: Int): Int {
        val source = inputs[sourceIndex]
        when (source) {
            is CommonInput.Gap ->
                return -1

            is CommonInput.Item -> {
                val pair = source.getPair()
                if (pair != null) {
                    // 向右查找
                    for (i in sourceIndex + 1..inputs.lastIndex) {
                        val input = inputs[i]
                        if (pair == input) {
                            return i
                        }
                    }
                } else if (source is CommonInput.Item.Char.Symbol) {
                    // 向左查找
                    for (i in sourceIndex - 1 downTo 0) {
                        val input = inputs[i]
                        if (input is CommonInput.Item && input.getPair() == source) {
                            return i
                        }
                    }
                }

                return -1
            }
        }
    }
}
