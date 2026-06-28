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

import org.crazydan.studio.app.ime.kuaizi.engine.CursorDirection
import org.crazydan.studio.app.ime.kuaizi.engine.EditorEditAction
import org.crazydan.studio.app.ime.kuaizi.engine.TextRange

/**
 * 编辑器桥接的抽象基类，提供单快照撤销机制。
 *
 * 撤销机制采用单快照设计（而非栈），反映 IME 的使用模式——用户通常只需撤销最近一次输入。
 * 快照记录提交前后的选区状态，使得撤销时可以精确恢复到操作前的文本和光标位置。
 *
 * [commitText] 方法在基类中实现了分发逻辑——根据 [replacements] 是否为空
 * 选择 [doReplaceableCommitText]（替换轮换）或 [doNormalCommitText]（普通提交），
 * 子类只需实现这两个具体的提交方法。
 *
 * @see ImeEditorBridge 桥梁接口定义
 * @see com.example.InputConnectionBridge 基于 Android InputConnection 的实现
 * @see com.example.EditTextBridge 基于 Android EditText 的实现
 */
abstract class BaseImeEditorBridge : ImeEditorBridge {

    /**
     * 选区快照：记录撤销操作所需的完整选区状态。
     *
     * @property beforeStart 提交前选区起始位置
     * @property beforeEnd 提交前选区结束位置
     * @property beforeContent 提交前选区内的文本内容
     * @property afterStart 提交后选区起始位置
     * @property afterEnd 提交后选区结束位置
     */
    protected data class SelectionSnapshot(
        val beforeStart: Int,
        val beforeEnd: Int,
        val beforeContent: String,
        val afterStart: Int,
        val afterEnd: Int,
    )

    // 当前撤销快照，null 表示无可撤销操作
    private var revertion: SelectionSnapshot? = null

    /** 清空撤销快照，标记当前操作为不可撤销。 */
    protected fun resetRevertion() {
        revertion = null
    }

    /**
     * 记录撤销快照，保存提交前后的选区状态。
     * 新的可撤回输入开始前应先清空旧快照以避免状态不一致。
     */
    protected fun recordRevertion(
        beforeStart: Int, beforeEnd: Int, beforeContent: String,
        afterStart: Int, afterEnd: Int,
    ) {
        revertion = SelectionSnapshot(
            beforeStart = beforeStart,
            beforeEnd = beforeEnd,
            beforeContent = beforeContent,
            afterStart = afterStart,
            afterEnd = afterEnd,
        )
    }

    /**
     * 撤销最近一次提交。
     * 读取快照后立即清空已消费的快照，然后委托子类执行实际的撤销操作。
     * 若无可撤销操作（[revertion] 为 null），静默跳过。
     */
    override fun revokeCommit() {
        val snapshot = revertion ?: return
        revertion = null
        onRevokeCommit(snapshot)
    }

    /** 子类实现具体的撤销恢复逻辑，根据快照信息恢复编辑器文本和选区。 */
    protected abstract fun onRevokeCommit(snapshot: SelectionSnapshot)

    /**
     * 提交文本到编辑器。
     * 先清空旧快照，再根据是否有替换列表选择不同的提交路径：
     * - [replacements] 非空 → 执行替换轮换提交
     * - 否则 → 执行普通提交
     */
    override fun commitText(text: String, replacements: List<String>?) {
        resetRevertion()
        if (replacements != null && replacements.isNotEmpty()) {
            doReplaceableCommitText(text, replacements)
        } else {
            doNormalCommitText(text)
        }
    }

    /** 子类实现普通文本提交，提交后应调用 [recordRevertion] 记录快照。 */
    protected abstract fun doNormalCommitText(text: String)

    /** 子类实现可替换轮换的文本提交，提交后应调用 [recordRevertion] 记录快照。 */
    protected abstract fun doReplaceableCommitText(text: String, replacements: List<String>)
}
