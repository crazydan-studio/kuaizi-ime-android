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

package org.crazydan.studio.app.ime.kuaizi

import android.view.inputmethod.InputConnection
import org.crazydan.studio.app.ime.kuaizi.engine.CursorDirection
import org.crazydan.studio.app.ime.kuaizi.engine.EditorEditAction
import org.crazydan.studio.app.ime.kuaizi.engine.TextRange
import org.crazydan.studio.app.ime.kuaizi.engine.bridge.BaseImeEditorBridge
import org.crazydan.studio.app.ime.kuaizi.engine.bridge.SelectionSnapshot

/**
 * 基于 Android [InputConnection] 的编辑器桥接实现。
 *
 * 使用 supplier 模式获取当前 [InputConnection] 实例，
 * 因为 InputConnection 由系统管理，编辑器切换时会变更。
 * 每次操作都通过 targetSupplier 获取最新的实例，确保操作目标正确。
 *
 * 继承 [BaseImeEditorBridge] 以获得单快照撤销能力。
 */
class InputConnectionBridge(
    /** 获取当前 [InputConnection] 实例的供给函数 */
    private val targetSupplier: () -> InputConnection?,
) : BaseImeEditorBridge() {

    /** 正常提交文本：记录撤销快照后调用系统 commitText。 */
    override fun doNormalCommitText(text: String) {
        val ic = targetSupplier() ?: return
        // 记录提交前的文本和选区，用于后续撤销
        val beforeText = getText()
        val beforeSel = getSelection()
        ic.commitText(text, 1)
        val afterSel = getSelection()
        // 保存撤销快照
        recordRevertion(
            beforeStart = beforeSel.start,
            beforeEnd = beforeSel.end,
            beforeContent = beforeText,
            afterStart = afterSel.start,
            afterEnd = afterSel.end,
        )
    }

    /** 可替换提交文本：检查光标前文本是否匹配替换列表，匹配则替换而非插入。 */
    override fun doReplaceableCommitText(text: String, replacements: List<String>) {
        val ic = targetSupplier() ?: return
        val beforeText = getText()
        val beforeSel = getSelection()

        // 检查光标前文本是否匹配任一替换项
        val cursorBefore = beforeText.substring(0, beforeSel.start)
        for (replacement in replacements) {
            if (cursorBefore.endsWith(replacement)) {
                // 匹配：删除匹配文本后插入新文本（替换轮换）
                val deleteStart = beforeSel.start - replacement.length
                ic.beginBatchEdit()
                ic.setSelection(deleteStart, beforeSel.start)
                ic.commitText(text, 1)
                ic.endBatchEdit()
                return
            }
        }

        // 不匹配：退化为普通提交
        ic.commitText(text, 1)
    }

    /** 插入配对符号（如括号、引号），光标置于左右符号之间。 */
    override fun insertPairedSymbols(left: String, right: String) {
        val ic = targetSupplier() ?: return
        ic.commitText("$left$right", 1)
        // 将光标移动到左右符号之间
        ic.setSelection(
            (ic.getTextBeforeCursor(1, 0)?.length ?: 0) - right.length,
            (ic.getTextBeforeCursor(1, 0)?.length ?: 0) - right.length,
        )
    }

    /** 按方向移动光标，通过模拟键盘按键实现。 */
    override fun moveCursor(direction: CursorDirection) {
        val ic = targetSupplier() ?: return
        when (direction) {
            CursorDirection.Left -> ic.sendKeyEvent(
                android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_DPAD_LEFT),
            )
            CursorDirection.Right -> ic.sendKeyEvent(
                android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_DPAD_RIGHT),
            )
            CursorDirection.Up -> ic.sendKeyEvent(
                android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_DPAD_UP),
            )
            CursorDirection.Down -> ic.sendKeyEvent(
                android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_DPAD_DOWN),
            )
            CursorDirection.Home -> ic.sendKeyEvent(
                android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_MOVE_HOME),
            )
            CursorDirection.End -> ic.sendKeyEvent(
                android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_MOVE_END),
            )
        }
    }

    /** 按方向扩展选区，模拟 Shift + 方向键。 */
    override fun selectRange(direction: CursorDirection) {
        val ic = targetSupplier() ?: return
        when (direction) {
            CursorDirection.Left -> ic.sendKeyEvent(
                android.view.KeyEvent(
                    android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_DPAD_LEFT,
                    android.view.KeyEvent.META_SHIFT_ON,
                ),
            )
            CursorDirection.Right -> ic.sendKeyEvent(
                android.view.KeyEvent(
                    android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_DPAD_RIGHT,
                    android.view.KeyEvent.META_SHIFT_ON,
                ),
            )
            // Up/Down 方向不支持选区扩展
            else -> {}
        }
    }

    /** 执行编辑器编辑操作，通过系统上下文菜单动作实现。 */
    override fun performEdit(action: EditorEditAction) {
        val ic = targetSupplier() ?: return
        when (action) {
            EditorEditAction.SELECT_ALL -> ic.performContextMenuAction(android.R.id.selectAll)
            EditorEditAction.COPY -> ic.performContextMenuAction(android.R.id.copy)
            EditorEditAction.PASTE -> ic.performContextMenuAction(android.R.id.paste)
            EditorEditAction.CUT -> ic.performContextMenuAction(android.R.id.cut)
            EditorEditAction.UNDO -> ic.performContextMenuAction(android.R.id.undo)
            EditorEditAction.REDO -> ic.performContextMenuAction(android.R.id.redo)
        }
    }

    /** 撤销提交：将编辑器文本恢复到快照记录的状态。 */
    override fun onRevokeCommit(snapshot: SelectionSnapshot) {
        val ic = targetSupplier() ?: return
        ic.beginBatchEdit()
        // 选中被修改的范围，然后提交原始内容覆盖
        ic.setSelection(snapshot.beforeStart, snapshot.beforeEnd)
        ic.commitText(snapshot.beforeContent, 1)
        ic.endBatchEdit()
    }

    /** 获取编辑器当前全文。 */
    override fun getText(): CharSequence {
        val ic = targetSupplier() ?: return ""
        return ic.getExtractedText(android.view.inputmethod.ExtractedTextRequest(), 0)?.text ?: ""
    }

    /** 获取编辑器当前选区的起始和结束位置。 */
    override fun getSelection(): TextRange {
        val ic = targetSupplier() ?: return TextRange(0, 0)
        val extracted = ic.getExtractedText(android.view.inputmethod.ExtractedTextRequest(), 0)
            ?: return TextRange(0, 0)
        return TextRange(extracted.selectionStart, extracted.selectionEnd)
    }
}
