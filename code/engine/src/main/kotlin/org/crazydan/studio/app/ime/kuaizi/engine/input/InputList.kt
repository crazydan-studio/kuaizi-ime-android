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
 *   [CommonInput.Item.Char.Latin]、[CommonInput.Item.Pinyin] 和 [CommonInput.Item.MathExpr]），
 *   从而避免频繁更新 [inputs] 列表。而由于 Gap 位置只能做 Gap-Item 对的插入，
 *   因此，当 [cursor] 指向 Gap 时，[pending] 将始终为 `null`；
 *
 * @param inputs Gap-Item 交替排列的输入序列。始终不为空
 * @param cursor 当前游标位置（指向 inputs 列表中的 Gap 或 Item 的索引）
 * @param pending 待输入。若当前并未更新支持连续输入的输入项，其始终为 `null`
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
     * - 若 [item] 为 [CommonInput.Item.MathExpr]，则不做处理，对算术输入本身的更新需通过 [withMathExprUpdate] 处理；
     * - 否则，若 [item] 为 [CommonInput.Item.Pinyin]，则执行 [doAddPinyinItem]；
     * - 否则，若 [item] 为 [CommonInput.Item.Char.Latin]，则执行 [doAddLatinCharItem]；
     * - 否则，执行 [doAddOtherItem]；
     */
    fun addItem(item: CommonInput.Item, replacements: List<String>?): InputList =
        when (item) {
            is CommonInput.Item.MathExpr ->
                this

            is CommonInput.Item.Pinyin ->
                doAddPinyinItem(item)

            is CommonInput.Item.Char.Latin ->
                doAddLatinCharItem(item, replacements)

            else ->
                doAddOtherItem(item, replacements)
        }

    /**
     * 添加拼音输入项：
     * - 若 [selected] 为 [CommonInput.Gap]，则插入 [item] 的 Gap-Item 对，并将 [cursor] 指向 [item]，
     *   同时将 [pending] 也设置为 [item]，以支持对拼音的持续性输入；
     * - 否则，将 [pending] 设置为 [item]，以支持对拼音的持续性输入并最终用其替代 [selected]；
     *
     * [cursor] 始终指向 [CommonInput.Item]，且 [pending] 为正在处理且等待更新到 [inputs]
     * 的 [CommonInput.Item.Pinyin]。
     */
    private fun doAddPinyinItem(item: CommonInput.Item.Pinyin): InputList =
        // 对拼音输入项是做整体替换，而不是追加
        when (selected) {
            is CommonInput.Gap ->
                applyInputsUpdate(cursor = cursor + 1, pending = item) {
                    addAll(cursor, listOf(CommonInput.Gap, item))
                }

            else ->
                copy(pending = item)
        }

    /**
     * 添加拉丁文：
     * - 若 [pending] 为 [CommonInput.Item.Char.Latin]，则向 [pending] 追加拉丁文（根据 [replacements] 判断是否替代前序字符）；
     * - 否则，若 [selected] 为 [CommonInput.Gap]，则插入 [item] 的 Gap-Item 对，并将 [cursor] 指向 [item]，
     *   同时将 [pending] 也设置为 [item]，以支持对拉丁文的持续性输入；
     * - 否则，将 [pending] 设置为 [item]，以支持对拉丁文的持续性输入并最终用其替代 [selected]；
     *
     * [cursor] 始终指向 [CommonInput.Item]，且 [pending] 为正在处理且等待更新到 [inputs]
     * 的 [CommonInput.Item.Char.Latin]。
     */
    private fun doAddLatinCharItem(item: CommonInput.Item.Char.Latin, replacements: List<String>?): InputList =
        when (pending) {
            is CommonInput.Item.Char.Latin ->
                copy(
                    pending = pending.appendChar(
                        char = item.chars[0],
                        replacements = replacements,
                    )
                )

            else -> when (selected) {
                is CommonInput.Gap ->
                    applyInputsUpdate(cursor = cursor + 1, pending = item) {
                        addAll(cursor, listOf(CommonInput.Gap, item))
                    }

                else ->
                    copy(pending = item)
            }
        }


    /**
     * 添加其他输入项：
     * - 若 [item] 不是配对输入项，则执行 [doAddNonPairItem]；
     * - 否则，执行 [doAddPairItem]；
     *
     * [cursor] 始终指向 Gap 位，且 [pending] 为 `null`。
     */
    private fun doAddOtherItem(item: CommonInput.Item, replacements: List<String>?): InputList =
        item.getClose().let { close ->
            if (close == null) {
                doAddNonPairItem(item, replacements)
            } else {
                doAddPairItem(item, close)
            }
        }

    /**
     * 添加非配对输入项：
     * - 若 [pending] 不为 `null`，则先 [confirmPending] 再插入 [item] 的 Gap-Item 对，并将 [cursor] 指向其后的 Gap，
     *   从而保留当前正在更新的连续性输入项并在其后插入 [item]；
     * - 否则，若 [selected] 为 [CommonInput.Gap]，则：
     *   - 若前序输入为 [CommonInput.Item.Char]，且该输入项的值包含在 [replacements] 中，则将该输入项替换为 [item]，从而支持替换输入；
     *   - 否则，在 [cursor] 处插入 [item] 的 Gap-Item 对，并将 [cursor] 指向其后的 Gap；
     * - 否则：
     *   - - 若 [selected] 不是配对输入项，则将 [selected] 替换为 [item]，并将 [cursor] 指向其后的 Gap；
     *   - - 否则，先 [confirmPending] 再插入 Gap-Item 对，即，保留 [cursor] 指向的配对输入项，并在该位置之后插入 [item] 的 Gap-Item 对，再将 [cursor] 指向其后的 Gap；
     *
     * [cursor] 始终指向 Gap 位，且 [pending] 为 `null`。
     */
    private fun doAddNonPairItem(item: CommonInput.Item, replacements: List<String>?): InputList =
        if (pending != null)
            confirmPending().doAddNonPairItem(item, replacements)
        else
            when (selected) {
                is CommonInput.Gap -> {
                    val prevIndex = cursor - 1
                    val prev = inputs.getOrNull(prevIndex)

                    if (prev is CommonInput.Item.Char && replacements?.contains(prev.value) == true)
                        applyInputsUpdate {
                            set(prevIndex, item)
                        }
                    else
                        applyInputsUpdate(cursor + 2) {
                            addAll(cursor, listOf(CommonInput.Gap, item))
                        }
                }

                else -> indexOfPairItemAt(cursor).let { selectedCloseIndex ->
                    if (selectedCloseIndex < 0)
                        applyInputsUpdate(cursor + 1) {
                            set(cursor, item)
                        }
                    else
                        confirmPending().doAddNonPairItem(item, replacements)
                }
            }

    /**
     * 添加配对输入项：
     * - 若 [pending] 不为 `null`，则先 [confirmPending] 再以 [open] 和 [close] 包裹该已确认的输入项，并将 [cursor] 指向其后的 Gap；
     * - 否则，若 [selected] 为 [CommonInput.Gap]，则插入 [open] 和 [close] 的 Gap-Item 对，并将 [cursor] 指向二者之间的 Gap；
     * - 否则：
     *   - - 若 [selected] 不是配对输入项，则使用 [open] 和 [close] 包裹该 [selected]，并将 [cursor] 指向其后的 Gap；
     *   - - 否则，使用 [open] 和 [close] 替换其开闭输入项，并将 [cursor] 指向 [selected] 之后的 Gap；
     *
     * [cursor] 始终指向 Gap 位，且 [pending] 为 `null`。
     */
    private fun doAddPairItem(open: CommonInput.Item, close: CommonInput.Item): InputList =
        if (pending != null) // 先确认再包裹
            confirmPending().let {
                // 未发生 inputs 列表元素移动
                if (it.inputs.size == inputs.size) it.selectAt(cursor - 1)
                // cursor 已指向 Gap 位
                else it
            }.doAddPairItem(open, close)
        else
            when (selected) {
                is CommonInput.Gap ->
                    applyInputsUpdate(cursor + 2) {
                        addAll(cursor, listOf(CommonInput.Gap, open, CommonInput.Gap, close))
                    }

                else -> indexOfPairItemAt(cursor).let { selectedCloseIndex ->
                    // 包裹非配对输入项
                    if (selectedCloseIndex < 0)
                        applyInputsUpdate(cursor + 1 + 2) {
                            // 先插入右侧符号，再插入左侧符号，以避免其 selected 的位置发生变动
                            addAll(cursor + 1, listOf(CommonInput.Gap, close))
                            addAll(cursor - 1, listOf(CommonInput.Gap, open))
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

    // ---------------------------------------------------------

    /**
     * 删除已选中输入项：
     * - 若 [selected] 为 [CommonInput.Gap]，则 [dropPending] 即可；
     * - 否则，执行 [doDeleteBackward]（`oneByOne=false`）；
     */
    fun removeSelected(): InputList =
        doDeleteBackward(false)

    /** 回删输入（项），执行 [doDeleteBackward]（`oneByOne=true`） */
    fun deleteBackward(): InputList =
        doDeleteBackward(true)

    /**
     * 回删输入：
     * - 若 [pending] 或 [selected] 为包含多个字符的 [CommonInput.Item.Char.Latin]
     *   且 [oneByOne]=`true` 时，则删除其尾部字符；
     * - 否则，若 [selected] 为 [CommonInput.Gap]，则：
     *   - 若 [cursor] 为 `0`，则不做处理；
     *   - 否则，若 [cursor] 前序输入项为包含多个字符的 [CommonInput.Item.Char.Latin]，则执行 [selectAt] 以将其选中，从而等待后续处理；
     *   - 否则，执行 [tryRemovePairItemAt] 先尝试删除前序输入项的配对输入项，再执行 [doRemoveNonGap] 以删除前序输入项；
     * - 否则，执行 [tryRemovePairItemAt] 先尝试删除 [selected] 的配对输入项，再执行 [doRemoveNonGap] 以删除 [selected]；
     *
     * 对于 Latin 输入项会尝试逐字符删除，其余类型的输入项则将被直接删除，[cursor] 也将指向删除后的空隙（Gap）。
     * 对算术输入自身的更新需通过 [withMathExprUpdate] 处理。
     */
    private fun doDeleteBackward(oneByOne: Boolean): InputList {
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
                        tryRemovePairItemAt(prevIndex).doRemoveNonGap(prev)
                }

            else ->
                tryRemovePairItemAt(cursor).doRemoveNonGap(selected)
        }
    }

    /**
     * 尝试删除指定位置的输入项的配对输入项：
     * - 若 [sourceIndex] 位置的输入项没有配对输入项，则不做处理；
     * - 否则，执行 [doRemoveNonGapAt] 以按位置删除对应的配对输入项；
     */
    private fun tryRemovePairItemAt(sourceIndex: Int): InputList =
        indexOfPairItemAt(sourceIndex).let { targetIndex ->
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

    /** 查找指定位置输入项的配对符号输入项的序号 */
    private fun indexOfPairItemAt(sourceIndex: Int): Int {
        val source = inputs[sourceIndex]
        when (source) {
            is CommonInput.Gap ->
                return -1

            is CommonInput.Item -> {
                val close = source.getClose()
                if (close != null) {
                    // 向右查找
                    for (i in sourceIndex + 1..inputs.lastIndex) {
                        val input = inputs[i]
                        if (close == input) {
                            return i
                        }
                    }
                } else if (source is CommonInput.Item.Char.Symbol) {
                    // 向左查找
                    for (i in sourceIndex - 1 downTo 0) {
                        val input = inputs[i]
                        if (input is CommonInput.Item && input.getClose() == source) {
                            return i
                        }
                    }
                }

                return -1
            }
        }
    }
}
