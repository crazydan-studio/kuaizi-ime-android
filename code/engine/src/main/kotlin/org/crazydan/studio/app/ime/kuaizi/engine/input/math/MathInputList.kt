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
 * @param cursor 当前游标位置（指向 inputs 列表中的被选中 [MathInput] 的索引）
 * @param pending 待输入：用于承载对非 Gap 的 [selected] 的持续性修改，从而避免频繁更新 [inputs] 列表
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

    /**
     * 选中指定位置输入项：
     * - 若指定位置已选中或者 [index] 不在有效范围，则不做处理；
     * - 否则，先 [confirmPending]，再做 [selectAt]。[pending] 将被重置为 `null`；
     */
    fun select(index: Int): MathInputList =
        if (cursor == index || index < 0 || index > inputs.lastIndex)
            this
        else
            confirmPending().selectAt(index)

    // ------------------------------------------

    /** 丢弃待输入 */
    fun dropPending(): MathInputList =
        if (pending == null) this
        else copy(pending = null)

    /**
     * 确认待输入：
     * - 若 [selected] 为 [MathInput.Gap]，则不做处理；
     * - 否则，若 [pending] 为 `null`，则将 [cursor] 后移一位；
     * - 否则，使用 [pending] 替换 [selected]，并将 [cursor] 后移一位；
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
                    selectByOffset(1)
                else
                    applyInputsUpdate(cursor + 1) { set(cursor, pending) }
        }

    // ------------------------------------------

    /**
     * 添加输入项：
     * - 若 [input] 为 [MathInput.Item.Const.Number]，则执行 [doAddNumberInput]；
     * - 否则，若 [input] 为 [MathInput.Item.Dot]，则执行 [doAddDotInput]；
     * - 否则，若 [input] 为 [MathInput.Item.Op]，则执行 [doAddOpInput]；
     * - 否则，执行 [doAddOtherInput]；
     */
    fun addInput(input: MathInput.Item): MathInputList =
        when (input) {
            is MathInput.Item.Const.Number ->
                doAddNumberInput(input)

            is MathInput.Item.Dot ->
                doAddDotInput(input)

            is MathInput.Item.Op ->
                doAddOpInput(input)

            else ->
                doAddOtherInput(input)
        }

    /**
     * 添加数字：
     * - 若 [pending] 为 [MathInput.Item.Const.Number]，则向 [pending] 追加数字；
     * - 否则，若 [selected] 为 [MathInput.Gap]，则插入 Gap-Item 对，并将 [cursor] 指向 [input]，
     *   同时将 [input] 也挂到 [pending] 上，以继续数字输入；
     * - 否则，将 [pending] 设置为 [input]，以继续数字输入并准备替代 [selected]；
     *
     * [cursor] 始终指向 [MathInput.Item]，且 [pending] 为正在处理的数字输入。
     */
    private fun doAddNumberInput(input: MathInput.Item.Const.Number): MathInputList =
        when (pending) {
            is MathInput.Item.Const.Number ->
                copy(pending = pending.appendChar(input.chars[0]))

            else -> when (selected) {
                is MathInput.Gap ->
                    applyInputsUpdate(cursor = cursor + 1, pending = input) {
                        addAll(cursor, listOf(MathInput.Gap, input))
                    }

                else ->
                    copy(pending = input)
            }
        }

    /**
     * 添加小数点：
     * - 若 [pending] 为 [MathInput.Item.Const.Number]，则向 [pending] 追加小数点；
     * - 否则，若 [selected] 为 [MathInput.Item.Const.Number]，则向 [selected] 追加小数点，
     *   再挂到 [pending] 上，以继续数字输入；
     * - 否则，不做处理，因为，小数点仅对数字输入有效；
     *
     * [cursor] 位置始终不变。
     */
    private fun doAddDotInput(input: MathInput.Item.Dot): MathInputList =
        input.value[0].let { dot ->
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
     * - 若 [input] 为加号和减号，则：
     *   - 若 [pending] 为 [MathInput.Item.Const.Number]，则为该数字输入添加正负号；
     *   - 否则，若 [selected] 为 [MathInput.Item.Const]，则为该常数输入添加正负号，并将 [cursor] 后移一位，且将 [pending] 重置为 `null`；
     *   - 否则，执行 [doAddOtherInput]；
     * - 否则，执行 [doAddOtherInput]；
     */
    private fun doAddOpInput(input: MathInput.Item.Op): MathInputList =
        when (input) {
            is MathInput.Item.Op.Plus,
            is MathInput.Item.Op.Minus
                -> when (pending) {
                is MathInput.Item.Const.Number ->
                    copy(pending = pending.addSignByOp(input))

                else -> selected.let { s ->
                    when (s) {
                        is MathInput.Item.Const ->
                            applyInputsUpdate(cursor + 1) {
                                set(cursor, s.addSignByOp(input))
                            }

                        else -> doAddOtherInput(input)
                    }
                }
            }

            else -> doAddOtherInput(input)
        }

    /**
     * 添加其他类型输入项：
     * - 若 [input] 为配对输入项，则执行 [doAddPairInput]；
     * - 否则，执行 [doAddNonPairInput]；
     *
     * [cursor] 始终指向 Gap 位，且 [pending] 为 `null`。
     */
    private fun doAddOtherInput(input: MathInput.Item): MathInputList =
        input.getClose().let { close ->
            if (close == null) {
                doAddNonPairInput(input)
            } else {
                doAddPairInput(input, close)
            }
        }

    /**
     * 添加非配对输入项：
     * - 若 [pending] 不为 `null`，则先 [confirmPending] 再插入 Gap-Item 对；
     * - 否则，若 [selected] 为 [MathInput.Gap]，则插入 Gap-Item 对；
     * - 否则：
     *   - 若 [selected] 为配对输入，则先 [confirmPending] 再插入 Gap-Item 对；
     *   - 否则，使用 [input] 替换 [selected]；
     *
     * [cursor] 始终指向 Gap 位，且 [pending] 为 `null`。
     */
    private fun doAddNonPairInput(input: MathInput.Item): MathInputList =
        if (pending != null)
            confirmPending().doAddNonPairInput(input)
        else
            when (selected) {
                is MathInput.Gap ->
                    applyInputsUpdate(cursor + 2) {
                        addAll(cursor, listOf(MathInput.Gap, input))
                    }

                else -> indexOfPairInputAt(cursor).let { selectedCloseIndex ->
                    if (selectedCloseIndex < 0)
                        applyInputsUpdate(cursor + 1) {
                            set(cursor, input)
                        }
                    else
                        confirmPending().doAddNonPairInput(input)
                }
            }

    /**
     * 添加配对输入项：
     * - 若 [pending] 不为 `null`，则先 [confirmPending] 再以 [open] 和 [close] 包裹该已确认的输入项；
     * - 否则，若 [selected] 为 [MathInput.Gap]，则插入 [open] 和 [close] 的 Gap-Item 对；
     * - 否则：
     *   - 若 [selected] 为配对输入，则使用 [open] 和 [close] 替换其开闭输入项；
     *   - 否则，使用 [open] 和 [close] 包裹 [selected]；
     *
     * [cursor] 始终指向 Gap 位，且 [pending] 为 `null`。
     */
    private fun doAddPairInput(open: MathInput.Item, close: MathInput.Item): MathInputList =
        if (pending != null) // 先确认再包裹
            confirmPending().selectByOffset(-1).doAddPairInput(open, close)
        else
            when (selected) {
                is MathInput.Gap ->
                    applyInputsUpdate(cursor + 2) {
                        addAll(cursor, listOf(MathInput.Gap, open, MathInput.Gap, close))
                    }

                else -> indexOfPairInputAt(cursor).let { selectedCloseIndex ->
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

    /** 回删输入 */
    fun removeBackward(): MathInputList =
        this

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

    /** 选中指定位置的输入项，并置空 [pending] */
    private fun selectAt(index: Int): MathInputList =
        copy(cursor = index, pending = null)

    /** 选中指定位置偏移的输入项，并置空 [pending] */
    private fun selectByOffset(offset: Int): MathInputList =
        selectAt(cursor + offset)

    /** 查找指定位置输入项的配对（开启/关闭）输入项的序号 */
    private fun indexOfPairInputAt(sourceIndex: Int): Int {
        val source = inputs[sourceIndex]
        when (source) {
            is MathInput.Gap ->
                return -1

            is MathInput.Item -> {
                val sourceClose = source.getClose()
                if (sourceClose != null) {
                    // 向右查找
                    for (i in sourceIndex + 1..inputs.lastIndex) {
                        val close = inputs[i]
                        if (sourceClose == close) {
                            return i
                        }
                    }
                } else if (source is MathInput.Item.Symbol) {
                    // 向左查找
                    for (i in sourceIndex - 1 downTo 0) {
                        val open = inputs[i]
                        if (open is MathInput.Item && open.getClose() == source) {
                            return i
                        }
                    }
                }

                return -1
            }
        }
    }
}
