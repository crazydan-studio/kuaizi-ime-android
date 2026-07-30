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

import org.crazydan.studio.app.ime.kuaizi.engine.input.BaseInputList

/**
 * 算术表达式 [BaseInputList]：
 * - Gap 为 [MathInput.Gap]，Item 为 [MathInput.Item]；
 * - 持续性输入项为 [MathInput.Item.Const.Number]；
 * - 配对输入项为 [MathInput.Item.Bracket]、[MathInput.Item.Func]；
 */
data class MathInputList(
    override val inputs: List<MathInput> = listOf(MathInput.Gap),
    override val cursor: Int = 0,
    override val pending: MathInput.Item? = null,
) : BaseInputList<MathInputList, MathInput, MathInput.Item, MathInput.Gap>(
    inputs = inputs,
    cursor = cursor,
    pending = pending,
) {

    override fun isGap(input: MathInput): Boolean =
        input is MathInput.Gap

    override fun getPairCloseItem(input: MathInput.Item): MathInput.Item? =
        input.getClose()

    override fun isPairCloseItem(input: MathInput.Item): Boolean =
        input is MathInput.Item.Symbol

    override fun isContinuousInputItem(input: MathInput): Boolean =
        input is MathInput.Item.Const.Number && input.chars.size > 1

    override fun doCopy(
        inputs: List<MathInput>,
        cursor: Int,
        pending: MathInput.Item?,
    ): MathInputList =
        copy(inputs = inputs, cursor = cursor, pending = pending)

    // ------------------------------------------

    /**
     * 确认 [pending]，将其更新到 [inputs]：
     * - 若 [selected] 为 [MathInput.Gap]，则不做处理，因为，Gap 位置不对应任何输入项；
     * - 否则，若 [pending] 为 `null`，则将 [cursor] 后移一位，从而指向 Gap 位，
     *   以等待在该位置插入新的输入项；
     * - 否则，执行 [doReplaceSelected] 使用 [pending] 替换 [selected] 并后移 [cursor]；
     *
     * 注意：
     * - [cursor] 始终指向 Gap 位，且 [pending] 为 `null`；
     * - [inputs] 长度不会发生变化；
     */
    override fun confirmPending(): MathInputList =
        when (selected) {
            is MathInput.Gap -> this
            is MathInput.Item -> when (pending) {
                null ->
                    selectAt(cursor + 1)

                else ->
                    doReplaceSelected(pending)
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

            // TODO 等号只能加到开头或结尾，且不可重复

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
     *   - - 否则，若 [selected] 为 [MathInput.Item.Const]，则向该常数添加正负号，并执行 [doReplaceSelected] 使用带符号的该常数替换 [selected] 并将 [cursor] 指向其后的 Gap，从而禁止对常数做连续修改；
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
                            doReplaceSelected(s.addSignByOp(item))

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
                doAddPairItem(item, close, MathInput.Gap)
            }
        }

    /**
     * 添加非配对输入项：
     * - 若 [pending] 不为 `null`，则先 [confirmPending] 再插入 [item] 的 Gap-Item 对，并将 [cursor] 指向其后的 Gap，
     *   从而保留当前正在更新的持续性输入项并在其后插入 [item]；
     * - 否则，若 [selected] 为 [MathInput.Gap]，则插入 [item] 的 Gap-Item 对，并将 [cursor] 指向其后的 Gap；
     * - 否则：
     *   - - 若 [selected] 不是配对输入项（括号、单参函数等），则执行 [doReplaceSelected] 使用 [item] 替换 [selected] 并后移 [cursor]；
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
                        doReplaceSelected(item)
                    else
                        confirmPending().doAddNonPairItem(item)
                }
            }

    // ------------------------------------------

    /**
     * 回删输入：
     * - 若 [pending] 或 [selected] 为包含多个字符的 [MathInput.Item.Const.Number]
     *   且 [oneByOne]=`true` 时，则删除其尾部字符；
     * - 否则，执行 [doDeleteBackwardOnSelected] 对 [selected] 做回删处理；
     *
     * 对于数字输入项会尝试逐字符删除，其余类型的输入项则将被直接删除，[cursor] 也将指向删除后的空隙（Gap）。
     */
    override fun doDeleteBackward(oneByOne: Boolean): MathInputList {
        val selected = this.selected

        if (oneByOne) {
            val current = pending ?: selected

            if (current is MathInput.Item.Const.Number && isContinuousInputItem(current)) {
                return copy(pending = current.dropLastChar())
            }
        }

        return doDeleteBackwardOnSelected()
    }
}
