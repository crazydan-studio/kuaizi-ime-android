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

package org.crazydan.studio.app.ime.kuaizi.ui.bridge

import android.widget.EditText
import android.text.Editable
import org.crazydan.studio.app.ime.kuaizi.engine.CursorDirection
import org.crazydan.studio.app.ime.kuaizi.engine.EditorEditAction
import org.crazydan.studio.app.ime.kuaizi.engine.TextRange
import org.crazydan.studio.app.ime.kuaizi.engine.bridge.BaseImeEditorBridge
import org.crazydan.studio.app.ime.kuaizi.engine.bridge.BaseImeEditorBridge.SelectionSnapshot

/**
 * 基于 [EditText] 的编辑器桥接器。
 *
 * 用于非系统 IME 场景（如应用内嵌入输入法），将引擎的编辑操作
 * 直接应用到 Android [EditText] 控件上。
 * 支持普通提交、替换提交、配对符号插入、光标移动、选区操作、撤销等完整编辑功能。
 *
 * @param targetSupplier 返回目标 EditText 实例的提供者
 */
class EditTextBridge(
    private val targetSupplier: () -> EditText?,
) : BaseImeEditorBridge() {

    /** 普通提交文本：在光标位置插入文本 */
    override fun doNormalCommitText(text: String) {
        val editText = targetSupplier() ?: return
        val editable = editText.text as? Editable ?: return
        val start = editText.selectionStart
        val end = editText.selectionEnd

        // 记录操作前的状态，用于撤销
        recordRevertion(
            beforeStart = start,
            beforeEnd = end,
            beforeContent = editable.toString(),
            afterStart = start + text.length,
            afterEnd = start + text.length,
        )

        editable.replace(start.coerceAtLeast(0), end.coerceAtLeast(0), text)
    }

    /** 可替换提交：先尝试替换已有文本，失败则普通提交 */
    override fun doReplaceableCommitText(text: String, replacements: List<String>) {
        val editText = targetSupplier() ?: return
        val editable = editText.text as? Editable ?: return
        val cursor = editText.selectionStart

        // 遍历替换模式列表，找到匹配的文本进行替换
        for (replacement in replacements) {
            val start = cursor - replacement.length
            if (start >= 0 && editable.substring(start, cursor) == replacement) {
                editable.replace(start, cursor, text)
                return
            }
        }
        // 无匹配时执行普通提交
        doNormalCommitText(text)
    }

    /** 插入配对符号：在光标位置插入左右成对符号，并将光标置于中间 */
    override fun insertPairedSymbols(left: String, right: String) {
        val editText = targetSupplier() ?: return
        val editable = editText.text as? Editable ?: return
        val start = editText.selectionStart
        editable.insert(start.coerceAtLeast(0), "$left$right")
        editText.setSelection(start + left.length)
    }

    /** 移动光标到指定方向 */
    override fun moveCursor(direction: CursorDirection) {
        val editText = targetSupplier() ?: return
        val pos = editText.selectionStart
        editText.setSelection(
            when (direction) {
                CursorDirection.Left -> (pos - 1).coerceAtLeast(0)
                CursorDirection.Right -> (pos + 1).coerceAtMost(editText.text?.length ?: 0)
                CursorDirection.Home -> 0
                CursorDirection.End -> editText.text?.length ?: 0
                else -> pos
            }
        )
    }

    /** 向指定方向扩展选区 */
    override fun selectRange(direction: CursorDirection) {
        val editText = targetSupplier() ?: return
        val start = editText.selectionStart
        val end = editText.selectionEnd
        when (direction) {
            CursorDirection.Left -> editText.setSelection(start, (end - 1).coerceAtLeast(start))
            CursorDirection.Right -> editText.setSelection(start, (end + 1).coerceAtMost(editText.text?.length ?: 0))
            else -> {}
        }
    }

    /** 执行编辑操作 */
    override fun performEdit(action: EditorEditAction) {
        val editText = targetSupplier() ?: return
        when (action) {
            EditorEditAction.SELECT_ALL -> editText.selectAll()
            EditorEditAction.COPY -> editText.onEditorAction(android.view.inputmethod.EditorInfo.IME_ACTION_UNSPECIFIED)
            EditorEditAction.PASTE -> editText.onTextContextMenuItem(android.R.id.paste)
            EditorEditAction.CUT -> editText.onTextContextMenuItem(android.R.id.cut)
            EditorEditAction.UNDO -> editText.onTextContextMenuItem(android.R.id.undo)
            EditorEditAction.REDO -> editText.onTextContextMenuItem(android.R.id.redo)
            EditorEditAction.BACKSPACE -> {
                val editable = editText.text as? Editable ?: return
                val start = editText.selectionStart
                if (start > 0) editable.delete(start - 1, start)
            }
        }
    }

    /** 获取编辑器当前文本 */
    override fun getText(): CharSequence = targetSupplier()?.text ?: ""

    /** 获取当前选区范围 */
    override fun getSelection(): TextRange {
        val et = targetSupplier() ?: return TextRange(0, 0)
        return TextRange(et.selectionStart, et.selectionEnd)
    }

    /** 撤销提交：恢复到操作前的内容 */
    override fun onRevokeCommit(snapshot: SelectionSnapshot) {
        val editText = targetSupplier() ?: return
        val editable = editText.text as? Editable ?: return
        editable.replace(0, editable.length, snapshot.beforeContent)
        editText.setSelection(snapshot.beforeStart)
    }
}
