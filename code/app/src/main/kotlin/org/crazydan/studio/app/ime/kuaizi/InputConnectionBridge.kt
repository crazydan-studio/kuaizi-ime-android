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

class InputConnectionBridge(
    private val targetSupplier: () -> InputConnection?,
) : BaseImeEditorBridge() {

    override fun doNormalCommitText(text: String) {
        val ic = targetSupplier() ?: return
        val beforeText = getText()
        val beforeSel = getSelection()
        ic.commitText(text, 1)
        val afterSel = getSelection()
        recordRevertion(
            beforeStart = beforeSel.start,
            beforeEnd = beforeSel.end,
            beforeContent = beforeText,
            afterStart = afterSel.start,
            afterEnd = afterSel.end,
        )
    }

    override fun doReplaceableCommitText(text: String, replacements: List<String>) {
        val ic = targetSupplier() ?: return
        val beforeText = getText()
        val beforeSel = getSelection()

        // Check if cursor text matches any replacement
        val cursorBefore = beforeText.substring(0, beforeSel.start)
        for (replacement in replacements) {
            if (cursorBefore.endsWith(replacement)) {
                val deleteStart = beforeSel.start - replacement.length
                ic.beginBatchEdit()
                ic.setSelection(deleteStart, beforeSel.start)
                ic.commitText(text, 1)
                ic.endBatchEdit()
                return
            }
        }

        // No match, normal commit
        ic.commitText(text, 1)
    }

    override fun insertPairedSymbols(left: String, right: String) {
        val ic = targetSupplier() ?: return
        ic.commitText("$left$right", 1)
        ic.setSelection(
            (ic.getTextBeforeCursor(1, 0)?.length ?: 0) - right.length,
            (ic.getTextBeforeCursor(1, 0)?.length ?: 0) - right.length,
        )
    }

    override fun moveCursor(direction: CursorDirection) {
        val ic = targetSupplier() ?: return
        when (direction) {
            CursorDirection.Left -> ic.sendKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_DPAD_LEFT))
            CursorDirection.Right -> ic.sendKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_DPAD_RIGHT))
            CursorDirection.Up -> ic.sendKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_DPAD_UP))
            CursorDirection.Down -> ic.sendKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_DPAD_DOWN))
            CursorDirection.Home -> ic.sendKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_MOVE_HOME))
            CursorDirection.End -> ic.sendKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_MOVE_END))
        }
    }

    override fun selectRange(direction: CursorDirection) {
        val ic = targetSupplier() ?: return
        when (direction) {
            CursorDirection.Left -> ic.sendKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_DPAD_LEFT, android.view.KeyEvent.META_SHIFT_ON))
            CursorDirection.Right -> ic.sendKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_DPAD_RIGHT, android.view.KeyEvent.META_SHIFT_ON))
            else -> {}
        }
    }

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

    override fun onRevokeCommit(snapshot: SelectionSnapshot) {
        val ic = targetSupplier() ?: return
        ic.beginBatchEdit()
        ic.setSelection(snapshot.beforeStart, snapshot.beforeEnd)
        ic.commitText(snapshot.beforeContent, 1)
        ic.endBatchEdit()
    }

    override fun getText(): CharSequence {
        val ic = targetSupplier() ?: return ""
        return ic.getExtractedText(android.view.inputmethod.ExtractedTextRequest(), 0)?.text ?: ""
    }

    override fun getSelection(): TextRange {
        val ic = targetSupplier() ?: return TextRange(0, 0)
        val extracted = ic.getExtractedText(android.view.inputmethod.ExtractedTextRequest(), 0) ?: return TextRange(0, 0)
        return TextRange(extracted.selectionStart, extracted.selectionEnd)
    }
}
