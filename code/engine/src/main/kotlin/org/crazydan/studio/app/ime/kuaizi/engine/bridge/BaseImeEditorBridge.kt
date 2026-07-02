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

package org.crazydan.studio.app.ime.kuaizi.engine.bridge

/**
 * 编辑器桥接的抽象基类，提供单快照撤销机制。
 *
 * 撤销机制采用单快照设计（而非栈），反映 IME 的使用模式——用户通常只需撤销最近一次输入。
 * 快照记录提交前后的选区状态，使得撤销时可以精确恢复到操作前的文本和光标位置。
 *
 * [commitText] 方法在基类中实现了分发逻辑——根据 `replacements` 是否为空
 * 选择 [doReplaceableCommitText]（替换轮换）或 [doNormalCommitText]（普通提交），
 * 子类只需实现这两个具体的提交方法。
 *
 * @see ImeEditorBridge 桥梁接口定义
 * @see InputConnectionBridge 基于 Android InputConnection 的实现
 * @see EditTextBridge 基于 Android EditText 的实现
 */
abstract class BaseImeEditorBridge : ImeEditorBridge {
    /** 当前撤销快照，null 表示无可撤销操作 */
    private var revertion: RevertionSnapshot? = null

    /** 清空撤销快照，标记当前操作为不可撤销。 */
    protected fun resetRevertion() {
        revertion = null
    }

    // ------------------------------------------

    /**
     * 撤销最近一次提交。
     * 读取快照后立即清空已消费的快照，然后委托子类执行实际的撤销操作。
     * 若无可撤销操作（[revertion] 为 null），则静默跳过。
     */
    override fun revokeCommit() {
        revertion?.also {
            resetRevertion()
            doRevokeCommit(it)
        }
    }

    /** 子类实现具体的撤销恢复逻辑，根据快照信息恢复编辑器文本和选区。 */
    protected abstract fun doRevokeCommit(revertion: RevertionSnapshot)

    /**
     * 提交文本到编辑器。
     * 先清空旧快照，再根据是否有替换列表选择不同的提交路径：
     * - [replacements] 非空 → 执行替换轮换提交
     * - 否则 → 执行普通提交
     */
    override fun commitText(
        text: CharSequence, replacements: List<String>?,
        oneByOne: Boolean, revertable: Boolean,
    ) {
        resetRevertion()

        var replaced = false
        if (replacements != null && replacements.isNotEmpty()) {
            replaced = doReplaceableCommitText(text, replacements)
        }
        if (replaced) return

        // -----------------
        val before = if (revertable) getSelection() else null

        doNormalCommitText(text, oneByOne)

        if (revertable) {
            val after = getSelection()
            this.revertion = RevertionSnapshot(before!!, after)
        }
    }

    /** 子类实现普通文本提交。 */
    protected abstract fun doNormalCommitText(text: CharSequence, oneByOne: Boolean)

    /**
     * 子类实现可替换轮换的文本提交。
     *
     * @param replacements 始终不为空
     * @return 若实施了文本轮换，则返回 true，否则，返回false
     */
    protected abstract fun doReplaceableCommitText(text: CharSequence, replacements: List<String>): Boolean

    // ------------------------------------------

    /**
     * 记录撤销操作所需的完整选区状态。
     *
     * @property before 提交前的选区
     * @property after 提交后的选区
     */
    protected data class RevertionSnapshot(
        val before: EditorSelection,
        val after: EditorSelection,
    )
}
