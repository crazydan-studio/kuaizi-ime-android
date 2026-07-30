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
 * 算术表达式输入列表：
 * - 在列表 [inputs] 中为成对的 [MathInput.Gap]-[MathInput.Item]（`Gap-Item`），
 *   且在该列表末尾始终放置一个 Gap，从而确保列表头、尾和 Item 之间的间隙均有一个 Gap，
 *   以方便自由定位列表中具体的输入项以及输入项之间的空隙；
 * - 通过游标 [cursor] 实现对 Gap 或 Item 的定位，从而在指向的 Gap 位置插入新的 Gap-Item 对，
 *   或者对指向的 Item 做替换、修改和删除；
 * - 待输入 [pending] 用于承载对 [cursor] 指向的 Item 的持续性修改（主要针对 [MathInput.Item.Const.Number]），
 *   从而避免频繁更新 [inputs] 列表。而由于 Gap 位置只能做 Gap-Item 对的插入，
 *   因此，当 [cursor] 指向 Gap 时，[pending] 将始终为 `null`；
 *
 * @param inputs Gap-Item 交替排列的输入序列。始终不为空
 * @param cursor 当前游标位置（指向 inputs 列表中的 Gap 或 Item 的索引）
 * @param pending 待输入。若当前并未更新支持连续输入的输入项，其始终为 `null`
 */
data class MathInputList(
    val inputs: List<MathInput> = listOf(MathInput.Gap),
    val cursor: Int = 0,
    val pending: MathInput.Item? = null,
) {
    init {
        require(cursor >= 0 && cursor <= inputs.lastIndex)
    }

    /** 已选中输入项：[cursor] 指向的 [MathInput.Gap] 或 [MathInput.Item] */
    private val selected: MathInput
        get() = inputs[cursor]

    // ------------------------------------------

    /** 输入列表是否为空：只包含唯一的 Gap 时，该输入列表即为空 */
    fun isEmpty(): Boolean =
        inputs.size == 1

    // ------------------------------------------

    /**
     * 选中指定位置的输入项：
     * - 若指定位置已选中或者 [index] 不在有效范围，则不做处理；
     * - 否则，先 [confirmPending]，再做 [selectAt]。
     *   注意，[pending] 将被重置为 `null`；
     */
    fun select(index: Int): MathInputList =
        if (cursor == index || index < 0 || index > inputs.lastIndex)
            this
        else
            confirmPending().selectAt(index)

    // ------------------------------------------

    /**
     * 确认 [pending]，将其更新到 [inputs]：
     * - 若 [selected] 为 [MathInput.Gap]，则不做处理，因为，Gap 位置不对应任何输入项；
     * - 否则，若 [pending] 为 `null`，则将 [cursor] 后移一位，从而指向 Gap 位，
     *   以等待在该位置插入新的输入项；
     * - 否则，将 [selected] 替换为 [pending]，并将 [cursor] 后移一位，等待插入新的输入项；
     *
     * 注意：
     * - [cursor] 始终指向 Gap 位，且 [pending] 为 `null`；
     * - [inputs] 长度不会发生变化；
     */
    fun confirmPending(): MathInputList =
        when (selected) {
            is MathInput.Gap -> this
            is MathInput.Item ->
                if (pending == null)
                    selectAt(cursor + 1)
                else
                    applyInputsUpdate(cursor + 1) {
                        set(cursor, pending)
                    }
        }

    // ------------------------------------------

    /**
     * 添加输入项：
     * - 若 [item] 为 [MathInput.Item.Const.Number]，则执行 [doAddNumberItem]；
     * - 否则，若 [item] 为 [MathInput.Item.Dot]，则执行 [doAddDotItem]；
     * - 否则，若 [item] 为 [MathInput.Item.Op]，则执行 [doAddOpItem]；
     * - 否则，执行 [doAddOtherItem]；
     */
    fun addItem(item: MathInput.Item): MathInputList =
        when (item) {
            is MathInput.Item.Const.Number ->
                doAddNumberItem(item)

            is MathInput.Item.Dot ->
                doAddDotItem(item)

            is MathInput.Item.Op ->
                doAddOpItem(item)

            else ->
                doAddOtherItem(item)
        }

    /**
     * 添加数字：
     * - 若 [pending] 为 [MathInput.Item.Const.Number]，则向 [pending] 追加数字；
     * - 否则，若 [selected] 为 [MathInput.Gap]，则插入 [item] 的 Gap-Item 对，并将 [cursor] 指向 [item]，
     *   同时将 [pending] 也设置为 [item]，以支持对数字的持续性输入；
     * - 否则，将 [pending] 设置为 [item]，以支持对数字的持续性输入并最终用其替代 [selected]；
     *
     * [cursor] 始终指向 [MathInput.Item]，且 [pending] 为正在处理且等待更新到 [inputs]
     * 的 [MathInput.Item.Const.Number]。
     */
    private fun doAddNumberItem(item: MathInput.Item.Const.Number): MathInputList =
        when (pending) {
            is MathInput.Item.Const.Number ->
                copy(pending = pending.appendChar(item.chars[0]))

            else -> when (selected) {
                is MathInput.Gap ->
                    applyInputsUpdate(cursor = cursor + 1, pending = item) {
                        addAll(cursor, listOf(MathInput.Gap, item))
                    }

                else ->
                    copy(pending = item)
            }
        }

    /**
     * 添加小数点：
     * - 若 [pending] 为 [MathInput.Item.Const.Number]，则向 [pending] 追加小数点；
     * - 否则，若 [selected] 为 [MathInput.Item.Const.Number]，则向 [selected] 的副本追加小数点，
     *   并将 [pending] 设置为该副本，以支持继续追加数字；
     * - 否则，不做处理，因为，小数点仅对数字输入有效；
     *
     * [cursor] 位置始终不变。
     */
    private fun doAddDotItem(item: MathInput.Item.Dot): MathInputList =
        item.value[0].let { dot ->
            when (pending) {
                is MathInput.Item.Const.Number ->
                    copy(pending = pending.appendChar(dot))

                else -> selected.let { s ->
                    when (s) {
                        is MathInput.Item.Const.Number ->
                            copy(pending = s.appendChar(dot))

                        else -> this
                    }
                }
            }
        }

    /**
     * 添加运算符：
     * - 若 [item] 为加号（MathInput.Item.Op.Plus）和减号（MathInput.Item.Op.Minus），则：
     *   - - 若 [pending] 为 [MathInput.Item.Const.Number]，则向其添加正负号；
     *   - - 否则，若 [selected] 为 [MathInput.Item.Const]，则向该常数添加正负号，并将 [cursor] 指向其后的 Gap，从而禁止对常数做连续修改；
     *   - - 否则，执行 [doAddOtherItem]；
     * - 否则，执行 [doAddOtherItem]；
     */
    private fun doAddOpItem(item: MathInput.Item.Op): MathInputList =
        when (item) {
            is MathInput.Item.Op.Plus,
            is MathInput.Item.Op.Minus
                -> when (pending) {
                is MathInput.Item.Const.Number ->
                    copy(pending = pending.addSignByOp(item))

                else -> selected.let { s ->
                    when (s) {
                        is MathInput.Item.Const ->
                            applyInputsUpdate(cursor + 1) {
                                set(cursor, s.addSignByOp(item))
                            }

                        else -> doAddOtherItem(item)
                    }
                }
            }

            else -> doAddOtherItem(item)
        }

    /**
     * 添加其他输入项：
     * - 若 [item] 不是配对输入项（括号、单参函数等），则执行 [doAddNonPairItem]；
     * - 否则，执行 [doAddPairItem]；
     *
     * [cursor] 始终指向 Gap 位，且 [pending] 为 `null`。
     */
    private fun doAddOtherItem(item: MathInput.Item): MathInputList =
        item.getClose().let { close ->
            if (close == null) {
                doAddNonPairItem(item)
            } else {
                doAddPairItem(item, close)
            }
        }

    /**
     * 添加非配对输入项：
     * - 若 [pending] 不为 `null`，则先 [confirmPending] 再插入 [item] 的 Gap-Item 对，并将 [cursor] 指向其后的 Gap，
     *   从而保留当前正在更新的连续性输入项并在其后插入 [item]；
     * - 否则，若 [selected] 为 [MathInput.Gap]，则插入 [item] 的 Gap-Item 对，并将 [cursor] 指向其后的 Gap；
     * - 否则：
     *   - - 若 [selected] 不是配对输入项（括号、单参函数等），则将 [selected] 替换为 [item]，并将 [cursor] 指向其后的 Gap；
     *   - - 否则，先 [confirmPending] 再插入 Gap-Item 对，即，保留 [cursor] 指向的配对输入项，并在该位置之后插入 [item] 的 Gap-Item 对，再将 [cursor] 指向其后的 Gap；
     *
     * [cursor] 始终指向 Gap 位，且 [pending] 为 `null`。
     */
    private fun doAddNonPairItem(item: MathInput.Item): MathInputList =
        if (pending != null)
            confirmPending().doAddNonPairItem(item)
        else
            when (selected) {
                is MathInput.Gap ->
                    applyInputsUpdate(cursor + 2) {
                        addAll(cursor, listOf(MathInput.Gap, item))
                    }

                else -> indexOfPairItemAt(cursor).let { selectedCloseIndex ->
                    if (selectedCloseIndex < 0)
                        applyInputsUpdate(cursor + 1) {
                            set(cursor, item)
                        }
                    else
                        confirmPending().doAddNonPairItem(item)
                }
            }

    /**
     * 添加配对输入项：
     * - 若 [pending] 不为 `null`，则先 [confirmPending] 再以 [open] 和 [close] 包裹该已确认的输入项，并将 [cursor] 指向其后的 Gap；
     * - 否则，若 [selected] 为 [MathInput.Gap]，则插入 [open] 和 [close] 的 Gap-Item 对，并将 [cursor] 指向二者之间的 Gap；
     * - 否则：
     *   - - 若 [selected] 不是配对输入项（括号、单参函数等），则使用 [open] 和 [close] 包裹该 [selected]，并将 [cursor] 指向其后的 Gap；
     *   - - 否则，使用 [open] 和 [close] 替换其开闭输入项，并将 [cursor] 指向 [selected] 之后的 Gap；
     *
     * [cursor] 始终指向 Gap 位，且 [pending] 为 `null`。
     */
    private fun doAddPairItem(open: MathInput.Item, close: MathInput.Item): MathInputList =
        if (pending != null) // 先确认再包裹
            confirmPending().selectAt(cursor - 1).doAddPairItem(open, close)
        else
            when (selected) {
                is MathInput.Gap ->
                    applyInputsUpdate(cursor + 2) {
                        addAll(cursor, listOf(MathInput.Gap, open, MathInput.Gap, close))
                    }

                else -> indexOfPairItemAt(cursor).let { selectedCloseIndex ->
                    // 包裹非配对输入项
                    if (selectedCloseIndex < 0)
                        applyInputsUpdate(cursor + 1 + 2) {
                            // 先插入右侧符号，再插入左侧符号，以避免其 selected 的位置发生变动
                            addAll(cursor + 1, listOf(MathInput.Gap, close))
                            addAll(cursor - 1, listOf(MathInput.Gap, open))
                        }
                    // 替换配对输入项
                    else
                        applyInputsUpdate(cursor + 1) {
                            // 选中的是左侧符号
                            if (cursor < selectedCloseIndex) {
                                set(cursor, open)
                                set(selectedCloseIndex, close)
                            }
                            // 选中的是右侧符号
                            else {
                                set(cursor, close)
                                set(selectedCloseIndex, open)
                            }
                        }
                }
            }

    // ------------------------------------------

    /**
     * 删除已选中输入：
     * - 若 [selected] 为 [MathInput.Gap]，则不做处理；
     * - 否则，执行 [doDeleteBackward] (false)；
     */
    fun removeSelected(): MathInputList =
        when (selected) {
            is MathInput.Gap -> this
            else -> doDeleteBackward(false)
        }

    /** 回删输入项，执行 [doDeleteBackward] (true) */
    fun deleteBackward(): MathInputList =
        doDeleteBackward(true)

    /**
     * 回删输入：
     * - 若 [pending] 或 [selected] 为包含多个字符的 [MathInput.Item.Const.Number]
     *   且 [oneByOne]=`true` 时，则删除其尾部字符；
     * - 否则，若 [selected] 为 [MathInput.Gap]，则：
     *   - 若 [cursor] 为 `0`，则不做处理；
     *   - 否则，若 [cursor] 前序输入项为包含多个字符的 [MathInput.Item.Const.Number]，则执行 [selectAt] 以将其选中，从而等待后续处理；
     *   - 否则，执行 [tryRemovePairItemAt] 先尝试删除前序输入项的配对输入项，再执行 [doRemoveNonGap] 以删除前序输入项；
     * - 否则，执行 [tryRemovePairItemAt] 先尝试删除 [selected] 的配对输入项，再执行 [doRemoveNonGap] 以删除 [selected]；
     *
     * 对于数字输入项会尝试逐字符删除，其余类型的输入项则将被直接删除，[cursor] 也将指向删除后的空隙（Gap）。
     */
    private fun doDeleteBackward(oneByOne: Boolean): MathInputList {
        val selected = this.selected

        if (oneByOne) {
            val current = pending ?: selected

            if (current is MathInput.Item.Const.Number && current.chars.size > 1) {
                return copy(pending = current.dropLastChar())
            }
        }

        return when (selected) {
            is MathInput.Gap ->
                if (cursor == 0) this
                else {
                    val prevIndex = cursor - 1
                    val prev = inputs[prevIndex]

                    if (prev is MathInput.Item.Const.Number && prev.chars.size > 1)
                        selectAt(prevIndex)
                    else
                        tryRemovePairItemAt(prevIndex).doRemoveNonGap(prev)
                }

            else ->
                tryRemovePairItemAt(cursor).doRemoveNonGap(selected)
        }
    }

    /**
     * 尝试删除指定位置的输入项的配对（开启/关闭）输入项：
     * - 若 [sourceIndex] 位置的输入项没有配对输入项，则不做处理；
     * - 否则，执行 [doRemoveNonGapAt] 以按位置删除对应的配对输入项；
     */
    private fun tryRemovePairItemAt(sourceIndex: Int): MathInputList =
        indexOfPairItemAt(sourceIndex).let { targetIndex ->
            if (targetIndex < 0) this
            else doRemoveNonGapAt(targetIndex)
        }

    /**
     * 删除指定的非 Gap 输入：
     * - 若 [input] 为 [MathInput.Gap]，则不做处理；
     * - 否则，若 [input] 不在 [inputs] 内，则不做处理；
     * - 否则，执行 [doRemoveNonGapAt] 以按位置删除 [input] 的 Gap-Item 对；
     */
    private fun doRemoveNonGap(input: MathInput): MathInputList =
        when (input) {
            is MathInput.Gap -> this
            else ->
                inputs.indexOf(input).let { index ->
                    if (index < 0) this
                    else doRemoveNonGapAt(index)
                }
        }

    /**
     * 删除指定位置的非 Gap 输入（即，Item 输入项）：
     * - 若 [index] 位置的输入为 `null` 或 [MathInput.Gap]，则不做处理；
     * - 否则，删除 [index] 位置的 Gap-Item 对，并根据 [inputs] 缺口偏移 [cursor] 的位置；
     */
    private fun doRemoveNonGapAt(index: Int): MathInputList =
        when (inputs.getOrNull(index)) {
            null, is MathInput.Gap -> this
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

    /** 移动 [cursor] 重置 [pending] 并更新 [inputs] */
    private fun applyInputsUpdate(
        cursor: Int = this.cursor,
        pending: MathInput.Item? = null,
        block: MutableList<MathInput>.() -> Unit,
    ): MathInputList =
        copy(
            inputs = inputs.toMutableList().apply(block),
            pending = pending,
            cursor = cursor,
        )

    /** 选中指定位置的输入项，并重置 [pending] 为 `null` */
    private fun selectAt(index: Int): MathInputList =
        copy(cursor = index, pending = null)

    /** 查找指定位置输入项的配对（开启/关闭）输入项的序号 */
    private fun indexOfPairItemAt(sourceIndex: Int): Int {
        val source = inputs[sourceIndex]
        when (source) {
            is MathInput.Gap ->
                return -1

            is MathInput.Item -> {
                val sourceClose = source.getClose()
                if (sourceClose != null) {
                    // 向右查找
                    for (i in sourceIndex + 1..inputs.lastIndex) {
                        val input = inputs[i]
                        if (sourceClose == input) {
                            return i
                        }
                    }
                } else if (source is MathInput.Item.Symbol) {
                    // 向左查找
                    for (i in sourceIndex - 1 downTo 0) {
                        val input = inputs[i]
                        if (input is MathInput.Item && input.getClose() == source) {
                            return i
                        }
                    }
                }

                return -1
            }
        }
    }
}
