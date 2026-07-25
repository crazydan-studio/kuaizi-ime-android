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
 * @param pending 待输入：正在输入还未更新到 [inputs] 的输入项。
 * 只有 [MathInput.Item.Const.Number] 才支持连续输入，因此，
 * 只有此类输入才存在待输入，其余类型均在添加时完成确认
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

    /** 丢弃待输入 */
    fun dropPending(): MathInputList =
        if (pending == null) this
        else copy(pending = null)

    /**
     * 确认待输入：
     * - 若 [pending] 为空，则 [dropPending]，并在 [selected] 不为 [MathInput.Gap] 时将 [cursor] 后移一位，
     * - 若 [selected] 为 [MathInput.Gap]，则将 [pending] 的 Gap-Item 对插入到 [selected] 之前；
     * - 否则，执行 [doReplaceOrInsertInput]；
     *
     * [cursor] 始终指向 Gap 位，且 [pending] 为 `null`。
     */
    fun confirmPending(): MathInputList =
        if (pending == null)
            when (selected) {
                is MathInput.Gap -> dropPending()
                else -> copy(cursor = cursor + 1)
            }
        else
            doReplaceOrInsertInput(pending)

    // ------------------------------------------

    /**
     * 添加输入项：
     * - 若 [input] 为 [MathInput.Item.Const.Number]，则在 [pending] 也为 [MathInput.Item.Const.Number] 时，
     *   向其追加数字，否则，直接用 [input] 覆盖 [pending]；
     * - 否则，执行 [doReplaceOrInsertInput]；
     *
     * 仅当 [input] 为 [MathInput.Item.Const.Number] 时 [cursor] 指向 Item 位，
     * 其余情况均将 [cursor] 指向 Gap 位，且 [pending] 为 `null`。
     */
    fun addInput(input: MathInput.Item): MathInputList {
        val selected = this.selected

        return when (input) {
            is MathInput.Item.Const -> when (input) {
                is MathInput.Item.Const.Number -> when (pending) {
                    // 追加数字输入
                    is MathInput.Item.Const.Number ->
                        copy(pending = pending.appendChar(input.chars[0]))

                    // 新的数字待输入
                    else -> copy(pending = input)
                }

                // e、π 等直接替换或插入
                else -> doReplaceOrInsertInput(input)
            }

            // 仅向数字输入添加小数点，其余直接忽略
            is MathInput.Item.Dot -> when (pending) {
                is MathInput.Item.Const.Number ->
                    copy(pending = pending.appendChar(input.value[0]))

                else -> when (selected) {
                    // 将追加后的数字输入挂到 pending 上，以支持对其做数字追加
                    is MathInput.Item.Const.Number ->
                        copy(pending = selected.appendChar(input.value[0]))

                    else -> this
                }
            }

            is MathInput.Item.Op -> when (input) {
                is MathInput.Item.Op.Plus,
                is MathInput.Item.Op.Minus
                    -> when (pending) {
                    // Note：只有数字输入才会有 pending
                    is MathInput.Item.Const.Number ->
                        copy(pending = pending.addSignByOp(input))

                    else -> when (selected) {
                        // 仅支持对常数补充正负号
                        is MathInput.Item.Const ->
                            applyInputsUpdate(cursor + 1) {
                                set(cursor, selected.addSignByOp(input))
                            }

                        // 正常添加 加号、减号
                        else -> doReplaceOrInsertInput(input)
                    }
                }

                else -> doReplaceOrInsertInput(input)
            }

            is MathInput.Item.Bracket ->
                this

            else -> this
        }
    }

    /**
     * 替换或插入输入项：
     * - 若 [selected] 为 [MathInput.Gap]，则在 [cursor] 前插入 [input] 的 Gap-Item 对（包括配对输入）；
     * - 若 [selected] 为配对输入项，则：
     *   - 若 [input] 为配对输入项，则由 [input] 的开闭符号替换 [selected] 的开闭符号；
     *   - 否则，在选中位置之后插入 [input] 的 Gap-Item 对；
     * - 否则：
     *   - 若 [input] 为配对输入项，则由 [input] 的开闭符号包裹 [selected]；
     *   - 否则，用 [input] 替换 [selected]；
     *
     * [cursor] 始终指向 Gap 位，且 [pending] 为 `null`。
     */
    private fun doReplaceOrInsertInput(input: MathInput.Item): MathInputList =
        input.getClose().let { inputClose ->
            when (selected) {
                is MathInput.Gap ->
                    // 插入 Gap-Item 对
                    applyInputsUpdate(cursor + 2) {
                        if (inputClose != null) {
                            addAll(cursor, listOf(MathInput.Gap, input, MathInput.Gap, inputClose))
                        } else {
                            addAll(cursor, listOf(MathInput.Gap, input))
                        }
                    }

                else -> indexOfPairInputAt(cursor).let { selectedCloseIndex ->
                    if (selectedCloseIndex < 0)
                    // 包裹已选中输入项
                        if (inputClose != null)
                            applyInputsUpdate(cursor + 1 + 2) {
                                // 先插入右侧符号，再插入左侧符号，以避免其 selected 的位置发生变动
                                addAll(cursor + 1, listOf(MathInput.Gap, inputClose))
                                addAll(cursor - 1, listOf(MathInput.Gap, input))
                            }
                        // 替换已选中输入项
                        else
                            applyInputsUpdate(cursor + 1) {
                                set(cursor, input)
                            }
                    // 在 input 也是配对输入项时，替换已选中的配对输入项
                    else if (inputClose != null)
                        applyInputsUpdate(cursor + 1) {
                            // 选中的是左侧符号
                            if (cursor < selectedCloseIndex) {
                                set(cursor, input)
                                set(selectedCloseIndex, inputClose)
                            }
                            // 选中的是右侧符号
                            else {
                                set(cursor, inputClose)
                                set(selectedCloseIndex, input)
                            }
                        }
                    // 插入 Gap-Item 对
                    else
                        applyInputsUpdate(cursor + 1 + 2) {
                            addAll(cursor + 1, listOf(MathInput.Gap, input))
                        }
                }
            }
        }

    // ------------------------------------------

    /** 回删输入 */
    fun removeBackward(): MathInputList =
        this

    // ------------------------------------------

    /** 移动 [cursor] 并更新 [inputs] */
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
    private fun doSelectAt(index: Int): MathInputList =
        copy(cursor = index, pending = null)

    /** 选中指定位置偏移的输入项，并置空 [pending] */
    private fun doSelectOffset(offset: Int): MathInputList =
        doSelectAt(cursor + offset)

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
