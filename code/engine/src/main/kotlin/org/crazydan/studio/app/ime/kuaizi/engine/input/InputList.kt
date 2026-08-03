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
 * 通用输入列表 [BaseInputList]：
 * - Gap 为 [CommonInput.Gap]，Item 为 [CommonInput.Item]；
 * - 持续性输入项为 [CommonInput.Item.Char.Latin]、[CommonInput.Item.Pinyin] 和 [CommonInput.Item.MathExpr]；
 * - 配对输入项为 [CommonInput.Item.Char.Symbol]；
 * - 只能通过 [withMathExprUpdate] 对 [CommonInput.Item.MathExpr] 类型的输入项进行更新；
 * - 只能通过 [withTextOptionUpdate] 对 [textOption] 进行更新；
 *
 * @property textOption 可提交文本的转换配置
 * @property frozen 是否已被冻结：被冻结后，将不能对输入列表做修改
 */
data class InputList(
    override val inputs: List<CommonInput> = listOf(CommonInput.Gap),
    override val cursor: Int = 0,
    override val pending: CommonInput.Item? = null,
    val textOption: InputTextOption = InputTextOption(),
    private val frozen: Boolean = false,
) : BaseInputList<InputList, CommonInput, CommonInput.Item, CommonInput.Gap>(
    inputs = inputs,
    cursor = cursor,
) {

    override fun getGap(): CommonInput.Gap =
        CommonInput.Gap

    override fun getPairCloseItem(item: CommonInput.Item): CommonInput.Item? =
        item.getClose()

    override fun isPairCloseItem(item: CommonInput.Item): Boolean =
        item is CommonInput.Item.Char.Symbol && item.close == null

    override fun isContinuousInputItem(input: CommonInput): Boolean =
        input is CommonInput.Item.Char.Latin && input.chars.size > 1

    override fun isEmptyItem(item: CommonInput.Item?): Boolean =
        when (item) {
            null -> true
            is CommonInput.Item.MathExpr -> item.inputList.isEmpty()
            else -> false
        }

    override fun doCopy(
        inputs: List<CommonInput>,
        cursor: Int,
        pending: CommonInput.Item?,
    ): InputList =
        copy(inputs = inputs, cursor = cursor, pending = pending)

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
        InputList(
            inputs = listOf(CommonInput.Gap),
            cursor = 0, pending = null,
            textOption = textOption,
        )

    // ------------------------------------------------------

    /** 丢弃待输入：重置 [pending] 为 `null` */
    fun dropPending(): InputList =
        if (pending == null) this
        else copy(pending = null)

    /**
     * 确认 [pending]，将其更新到 [inputs]：
     * - 若 [selected] 为 [CommonInput.Gap]，则不做处理，因为，Gap 位置不对应任何输入项；
     * - 否则，若 [pending] 为 [CommonInput.Item.MathExpr]，则执行 [doConfirmMathExprPending]；
     * - 否则，若 [pending] 为 [CommonInput.Item.Pinyin]，则执行 [doConfirmPinyinPending]；
     * - 否则，若 [pending] 为 `null`，则将 [cursor] 后移一位，从而指向 Gap 位，
     *   以等待在该位置插入新的输入项；
     * - 否则，执行 [doReplaceSelected] 使用 [pending] 替换 [selected] 并后移 [cursor]；
     *
     * 注意：
     * - [cursor] 始终指向 Gap 位，且 [pending] 为 `null`；
     * - [inputs] 长度仅在删除空的算术表达式输入项或无效的拼音输入项时才变化，其余情况均不会发生变化；
     */
    override fun confirmPending(): InputList =
        when (selected) {
            is CommonInput.Gap -> this
            is CommonInput.Item -> when (pending) {
                is CommonInput.Item.MathExpr ->
                    doConfirmMathExprPending()

                is CommonInput.Item.Pinyin ->
                    doConfirmPinyinPending()

                null ->
                    selectAt(cursor + 1)

                else ->
                    doReplaceSelected(pending)
            }
        }

    /**
     * 确认 [CommonInput.Item.MathExpr] 类型的 [pending]：
     * - 先调用 [MathInputList.confirmPending] 确认算术表达式的待输入；
     * - 再继续：
     *   - 若 [pending] 为空，则执行 [doRemoveNonGapAt] 将 [cursor] 指向的算术表达式移除；
     *   - 否则，执行 [doReplaceSelected] 使用 [pending] 替换 [selected] 并后移 [cursor]；
     */
    private fun doConfirmMathExprPending(): InputList =
        withMathExprUpdate {
            confirmPending()
        }.run {
            if (isEmptyItem(pending))
                doRemoveNonGapAt(cursor)
            else
                doReplaceSelected(pending!!)
        }

    /**
     * 确认 [CommonInput.Item.Pinyin] 类型的 [pending]：
     * - 若 [pending] 为有效拼音输入项，则执行 [doReplaceSelected] 使用 [pending] 替换 [selected] 并后移 [cursor]；
     * - 否则：
     *   - 若 [selected] 为无效的拼音输入项，则执行 [doRemoveNonGapAt] 将 [cursor] 指向的拼音输入项移除；
     *   - 否则，丢弃 [pending]，保持 [selected] 不变，并将 [cursor] 后移一位，等待插入新的输入项；
     */
    private fun doConfirmPinyinPending(
        pending: CommonInput.Item.Pinyin = this.pending as CommonInput.Item.Pinyin
    ): InputList =
        if (pending.valid)
            doReplaceSelected(pending)
        else selected.let { s ->
            when (s) {
                is CommonInput.Item.Pinyin if !s.valid ->
                    doRemoveNonGapAt(cursor)

                else ->
                    selectAt(cursor + 1)
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
                insertItemAt(cursor, item).copy(
                    cursor = cursor + 1, pending = item,
                )

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
                    insertItemAt(cursor, item).copy(
                        cursor = cursor + 1, pending = item,
                    )

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
     *   从而保留当前正在更新的持续性输入项并在其后插入 [item]；
     * - 否则，若 [selected] 为 [CommonInput.Gap]，则：
     *   - 若前序输入为 [CommonInput.Item.Char]，且该输入项的值包含在 [replacements] 中，则将该输入项替换为 [item]，从而支持替换输入；
     *   - 否则，在 [cursor] 处插入 [item] 的 Gap-Item 对，并将 [cursor] 指向其后的 Gap；
     * - 否则：
     *   - - 若 [selected] 不是配对输入项，则执行 [doReplaceSelected] 使用 [item] 替换 [selected] 并后移 [cursor]；
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
                        insertItemAt(cursor, item).selectAt(cursor + 2)
                }

                else -> indexOfPairItemAt(cursor).let { selectedCloseIndex ->
                    if (selectedCloseIndex < 0)
                        doReplaceSelected(item)
                    else
                        confirmPending().doAddNonPairItem(item, replacements)
                }
            }

    // ---------------------------------------------------------

    /**
     * 回删输入：
     * - 若 [pending] 或 [selected] 为包含多个字符的 [CommonInput.Item.Char.Latin]
     *   且 [oneByOne]=`true` 时，则删除其尾部字符；
     * - 否则，执行 [doDeleteBackwardOnSelected] 对 [selected] 做回删处理；
     *
     * 对于 Latin 输入项会尝试逐字符删除，其余类型的输入项则将被直接删除，[cursor] 也将指向删除后的空隙（Gap）。
     * 对算术输入自身的更新需通过 [withMathExprUpdate] 处理。
     */
    override fun doDeleteBackward(oneByOne: Boolean): InputList {
        val selected = this.selected
        val emptyPending = isEmptyItem(pending)

        if (oneByOne) {
            val current = if (emptyPending) selected else pending

            if (current is CommonInput.Item.Char.Latin && isContinuousInputItem(current)) {
                return copy(pending = current.dropLastChar())
            }
        }

        return doDeleteBackwardOnSelected()
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
            copy(pending = it.applyInputListUpdate(block))
        }

    /** 对 [textOption] 进行更新 */
    fun withTextOptionUpdate(block: InputTextOption.() -> InputTextOption): InputList =
        copy(textOption = textOption.block())

    // -------------------------------------------------

    /** 以 [textOption] 作为选项获取可提交文本 */
    override fun getText(): CharSequence =
        getText(textOption)

    override fun getText(item: CommonInput.Item, option: InputTextOption): CharSequence =
        item.getText(option)

    override fun needGapSpaceBetween(
        left: CommonInput.Item, right: CommonInput.Item,
        option: InputTextOption,
    ): Boolean =
        // 已经有显式的空格，则不需要空格间隔
        if (left is CommonInput.Item.Char.Space || right is CommonInput.Item.Char.Space) false
        // 算术表达式输入项与其他输入项之间需要空格间隔
        else if (left is CommonInput.Item.MathExpr || right is CommonInput.Item.MathExpr) true
        // 拉丁文输入项与符号输入项之间不需要空格间隔
        else if (left is CommonInput.Item.Char.Latin)
            right !is CommonInput.Item.Char.Symbol
        // 仅全角符号输入项与拉丁文输入项之间不需要空格间隔
        else if (right is CommonInput.Item.Char.Latin)
            !(left is CommonInput.Item.Char.Symbol && left.fullWidth)
        else {
            val onlyUseSpell =
                fun(item: CommonInput.Item): Boolean =
                    item is CommonInput.Item.Pinyin
                            && (
                            item.word == null
                                    || option.spellUseMode == InputWord.SpellUseMode.Replace
                            )

            // 汉字与其他输入之间均不需要空格间隔，而其纯读音则按照拉丁文处理
            if (onlyUseSpell(left))
                needGapSpaceBetween(
                    CommonInput.Item.Char.Latin(chars = listOf("L")),
                    right, option
                )
            else if (onlyUseSpell(right))
                needGapSpaceBetween(
                    left,
                    CommonInput.Item.Char.Latin(chars = listOf("R")),
                    option
                )
            else
                false
        }
}
