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

import org.crazydan.studio.app.ime.kuaizi.engine.input.math.MathInput

/**
 * 输入列表：
 * - 在列表 [inputs] 中为成对的 [Gap]-[Item]，且在该列表末尾始终放置一个 Gap，
 *   从而确保列表头、尾和 Item 之间的间隙均有一个 Gap，
 *   以方便自由定位列表中具体的输入项以及输入项之间的空隙；
 * - 通过游标 [cursor] 实现对 [Gap] 或 [Item] 的定位，从而在指向的 Gap 位置插入新的 Gap-Item 对，
 *   或者对指向的 [Item] 做替换、修改和删除；
 * - 待输入 [pending] 用于承载对 [cursor] 指向的持续性 [Item] 的更新，
 *   从而避免频繁更新 [inputs] 列表。此外，由于 [Gap] 位置只能做 Gap-Item 对的插入，
 *   因此，当 [cursor] 指向 [Gap] 时，[pending] 将始终为 `null`；
 *
 * @property inputs Gap-Item 交替排列的输入序列。始终不为空
 * @property cursor 当前游标位置（指向 inputs 列表中的 [Gap] 或 [Item] 的索引）
 * @property pending 待输入。若当前并未更新持续性输入项，则其始终为 `null`
 */
abstract class BaseInputList<This : BaseInputList<This, Input, Item, Gap>, Input, Item : Input, Gap : Input>(
    open val inputs: List<Input>,
    open val cursor: Int = 0,
    open val pending: Item? = null,
) {
    init {
        require(cursor >= 0 && cursor <= inputs.lastIndex)
    }

    /** 已选中输入项：[cursor] 指向的 [Gap] 或 [Item] */
    protected val selected: Input
        get() = inputs[cursor]

    // ------------------------------------------

    /** 输入列表是否为空：只包含唯一的 Gap 时，该输入列表即为空 */
    fun isEmpty(): Boolean =
        inputs.size == 1

    /** 获取 [Gap] 类型实体 */
    protected abstract fun getGap(): Gap

    /** 判断指定的输入是否为 Gap */
    private fun isGap(input: Input): Boolean =
        input == getGap()

    /** 判断指定的 [item] 是否为空 */
    protected open fun isEmptyItem(item: Item?): Boolean =
        item != null

    // ------------------------------------------

    /**
     * 选中指定位置的输入项：
     * - 若指定位置已选中或者 [index] 不在有效范围，则不做处理；
     * - 否则，先 [confirmPending]，再做 [selectAt]，
     *   并对 [cursor] 加上偏移量（[inputs] 列表长度可能变短）。
     *   注意，[pending] 将被重置为 `null`；
     */
    fun select(index: Int): This =
        if (cursor == index || index < 0 || index > inputs.lastIndex)
            this as This
        else
            confirmPending().let {
                it.selectAt(
                    if (index < cursor) index
                    // 加上偏移量
                    else index + (it.inputs.size - inputs.size)
                )
            }

    /** 选中指定位置的输入项，并重置 [pending] 为 `null` */
    protected fun selectAt(index: Int): This =
        doCopy(cursor = index, pending = null)

    // ------------------------------------------

    /** 确认 [pending]，将其更新到 [inputs] */
    abstract fun confirmPending(): This

    /** 将 [selected]（非 Gap）替换为 [item]，并将 [cursor] 后移一位指向 Gap 位，以等待插入新的输入项 */
    protected fun doReplaceSelected(item: Item): This =
        if (isGap(selected)) this as This
        else
            applyInputsUpdate(cursor + 1) {
                set(cursor, item)
            }

    // ------------------------------------------

    /** 复制本输入列表 */
    protected abstract fun doCopy(
        inputs: List<Input> = this.inputs,
        cursor: Int = this.cursor,
        pending: Item? = this.pending,
    ): This

    /** 在 [index] 位置插入 [items] 的 Gap-Item 对 */
    protected fun insertItemAt(index: Int, vararg items: Item): This =
        applyInputsUpdate {
            addAll(index, items.flatMap { listOf(getGap(), it) })
        }

    /**
     * 添加配对输入项：
     * - 若 [pending] 不为 `null`，则先 [confirmPending] 再以 [open] 和 [close] 包裹该已确认的输入项，并将 [cursor] 指向其后的 Gap；
     * - 否则，若 [selected] 为 Gap（[isGap]==`true`），则插入 [open] 和 [close] 的 Gap-Item 对，并将 [cursor] 指向二者之间的 Gap；
     * - 否则：
     *   - - 若 [selected] 不是配对输入项，则使用 [open] 和 [close] 包裹该 [selected]，并将 [cursor] 指向其后的 Gap；
     *   - - 否则，使用 [open] 和 [close] 替换其开闭输入项，并将 [cursor] 指向 [selected] 之后的 Gap；
     *
     * [cursor] 始终指向 Gap 位，且 [pending] 为 `null`。
     */
    protected fun doAddPairItem(open: Item, close: Item): This =
        if (pending != null) // 先确认再包裹
            confirmPending().let {
                // 未发生 inputs 列表元素移动
                if (it.inputs.size == inputs.size) it.selectAt(cursor - 1)
                // cursor 已指向 Gap 位
                else it
            }.doAddPairItem(open, close)
        else if (isGap(selected))
            insertItemAt(cursor, open, close).selectAt(cursor + 2)
        else
            indexOfPairItemAt(cursor).let { selectedCloseIndex ->
                // 包裹非配对输入项
                if (selectedCloseIndex < 0)
                // 先插入闭符号，再插入开符号，以避免其 selected 的位置发生变动
                    insertItemAt(cursor + 1, close)
                        .insertItemAt(cursor - 1, open)
                        .selectAt(cursor + 1 + 2)
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

    // ------------------------------------------

    /**
     * 删除已选中输入：
     * - 若 [selected] 为 [MathInput.Gap]，则不做处理；
     * - 否则，执行 [doDeleteBackward] (false)；
     */
    fun removeSelected(): This =
        if (isGap(selected)) this as This
        else doDeleteBackward(false)

    /** 回删输入项，执行 [doDeleteBackward] (true) */
    fun deleteBackward(): This =
        doDeleteBackward(true)

    /**
     * 回删输入
     * @param oneByOne 是否逐字符删除
     */
    protected abstract fun doDeleteBackward(oneByOne: Boolean): This

    /** 判断指定的输入是否为持续性输入项（即，可接受多个字符输入） */
    protected abstract fun isContinuousInputItem(input: Input): Boolean

    /**
     * 对 [selected] 做回删处理：
     * - 若 [selected] 为 Gap（[isGap]==`true`），则：
     *   - 若 [cursor] 为 `0`，则不做处理；
     *   - 否则，若 [cursor] 前序输入项为可持续性输入项（[isContinuousInputItem]==`true`），则执行 [selectAt] 以将其选中，从而支持对其做持续输入；
     *   - 否则，执行 [tryRemovePairItemAt] 先尝试删除前序输入项的配对输入项，再执行 [doRemoveNonGap] 以删除前序输入项；
     * - 否则，执行 [tryRemovePairItemAt] 先尝试删除 [selected] 的配对输入项，再执行 [doRemoveNonGap] 以删除 [selected]；
     */
    protected fun doDeleteBackwardOnSelected(): This {
        val selected = this.selected

        if (isGap(selected)) {
            return if (cursor == 0)
                this as This
            else {
                val prevIndex = cursor - 1
                val prev = inputs[prevIndex]

                if (isContinuousInputItem(prev))
                    selectAt(prevIndex)
                else
                    tryRemovePairItemAt(prevIndex).doRemoveNonGap(prev)
            }
        }

        return tryRemovePairItemAt(cursor).doRemoveNonGap(selected)
    }

    /**
     * 尝试删除指定位置的输入项的配对输入项：
     * - 若 [sourceIndex] 位置的输入项没有配对输入项，则不做处理；
     * - 否则，执行 [doRemoveNonGapAt] 以按位置删除对应的配对输入项；
     */
    protected fun tryRemovePairItemAt(sourceIndex: Int): This =
        indexOfPairItemAt(sourceIndex).let { targetIndex ->
            if (targetIndex < 0) this as This
            else doRemoveNonGapAt(targetIndex)
        }

    /**
     * 删除指定的非 Gap 输入：
     * - 若 [input] 为 [MathInput.Gap]，则不做处理；
     * - 否则，若 [input] 不在 [inputs] 内，则不做处理；
     * - 否则，执行 [doRemoveNonGapAt] 以按位置删除 [input] 的 Gap-Item 对；
     */
    protected fun doRemoveNonGap(input: Input): This =
        if (isGap(input)) this as This
        else
            inputs.indexOf(input).let { index ->
                if (index < 0) this as This
                else doRemoveNonGapAt(index)
            }

    /**
     * 删除指定位置的非 Gap 输入（即，Item 输入项）：
     * - 若 [index] 位置的输入为 `null` 或 [MathInput.Gap]，则不做处理；
     * - 否则，删除 [index] 位置的 Gap-Item 对，并根据 [inputs] 缺口偏移 [cursor] 的位置；
     */
    protected fun doRemoveNonGapAt(index: Int): This =
        inputs.getOrNull(index).let { input ->
            if (input == null || isGap(input)) this as This
            else
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

    /** 获取输入列表的可提交文本：将根据 [needGapSpace] 决定是否在 Gap 位插入空格 */
    fun getText(option: InputTextOption): CharSequence =
        inputs.mapIndexed { i, input ->
            if (isGap(input))
                if (
                    i > 0 && i < inputs.lastIndex
                    && needGapSpace(i, option)
                ) " "
                else ""
            else
                getText(item = getValidItemAt(i), option)

        }.joinTo(StringBuilder(), "")

    /** 获取输入项的可提交文本 */
    protected abstract fun getText(item: Item, option: InputTextOption): CharSequence

    /**
     * 通过 [needGapSpaceBetween] 判断指定位置（[gapIndex] 不包含 [inputs] 首尾序号）的
     * [Gap] 位的左右两侧的输入项之间是否需要插入空格间隔
     */
    fun needGapSpace(gapIndex: Int, option: InputTextOption): Boolean {
        val left = getValidItemAt(gapIndex - 1)
        val right = getValidItemAt(gapIndex + 1)

        return needGapSpaceBetween(left, right, option)
    }

    /** 判断指定的左右两个输入项之间是否需要插入空格间隔 */
    abstract fun needGapSpaceBetween(left: Item, right: Item, option: InputTextOption): Boolean

    /**
     * 获取指定位置的有效输入项：
     * - 若 [index] 为 [cursor] 且 [pending] 不为 `null` 或空，则返回 [pending]；
     * - 需确保 [index] 指向的是 [Item] 而不是 [Gap]；
     */
    fun getValidItemAt(index: Int): Item =
        if (cursor == index && !isEmptyItem(pending)) pending!!
        else inputs[index] as Item

    // ------------------------------------------

    /** 移动 [cursor] 重置 [pending] 并更新 [inputs] */
    protected fun applyInputsUpdate(
        cursor: Int = this.cursor,
        pending: Item? = null,
        block: MutableList<Input>.() -> Unit,
    ): This =
        doCopy(
            inputs = inputs.toMutableList().apply(block),
            pending = pending,
            cursor = cursor,
        )

    /** 获取与指定输入项配对的闭合输入项 */
    protected abstract fun getPairCloseItem(item: Item): Item?

    /** 判断指定输入项是否为配对的闭合输入项 */
    protected abstract fun isPairCloseItem(item: Item): Boolean

    /** 查找指定位置输入项的配对输入项（开或闭）的序号 */
    protected fun indexOfPairItemAt(sourceIndex: Int): Int {
        val source = inputs[sourceIndex]
        if (isGap(source)) {
            return -1
        }

        // 查找 source 的闭合输入项：向右查找
        val sourceClose = getPairCloseItem(source as Item)
        if (sourceClose != null) {
            for (i in sourceIndex + 1..inputs.lastIndex) {
                val input = inputs[i]

                if (sourceClose == input) {
                    return i
                }
            }
        }
        // 查找以 source 为闭合输入项的开输入项：向左查找
        else if (isPairCloseItem(source)) {
            for (i in sourceIndex - 1 downTo 0) {
                val input = inputs[i]
                val inputClose = getPairCloseItem(input as Item)

                if (inputClose == source) {
                    return i
                }
            }
        }

        return -1
    }
}
