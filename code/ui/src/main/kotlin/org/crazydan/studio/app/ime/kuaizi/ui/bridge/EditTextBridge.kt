package org.crazydan.studio.app.ime.kuaizi.ui.bridge

import android.widget.EditText
import android.text.Editable
import org.crazydan.studio.app.ime.kuaizi.engine.CursorDirection
import org.crazydan.studio.app.ime.kuaizi.engine.EditorEditAction
import org.crazydan.studio.app.ime.kuaizi.engine.TextRange
import org.crazydan.studio.app.ime.kuaizi.engine.bridge.BaseImeEditorBridge
import org.crazydan.studio.app.ime.kuaizi.engine.bridge.BaseImeEditorBridge.SelectionSnapshot

class EditTextBridge(
    private val targetSupplier: () -> EditText?,
) : BaseImeEditorBridge() {

    override fun doNormalCommitText(text: String) {
        val editText = targetSupplier() ?: return
        val editable = editText.text as? Editable ?: return
        val start = editText.selectionStart
        val end = editText.selectionEnd

        recordRevertion(
            beforeStart = start,
            beforeEnd = end,
            beforeContent = editable.toString(),
            afterStart = start + text.length,
            afterEnd = start + text.length,
        )

        editable.replace(start.coerceAtLeast(0), end.coerceAtLeast(0), text)
    }

    override fun doReplaceableCommitText(text: String, replacements: List<String>) {
        val editText = targetSupplier() ?: return
        val editable = editText.text as? Editable ?: return
        val cursor = editText.selectionStart

        for (replacement in replacements) {
            val start = cursor - replacement.length
            if (start >= 0 && editable.substring(start, cursor) == replacement) {
                editable.replace(start, cursor, text)
                return
            }
        }
        doNormalCommitText(text)
    }

    override fun insertPairedSymbols(left: String, right: String) {
        val editText = targetSupplier() ?: return
        val editable = editText.text as? Editable ?: return
        val start = editText.selectionStart
        editable.insert(start.coerceAtLeast(0), "$left$right")
        editText.setSelection(start + left.length)
    }

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

    override fun getText(): CharSequence = targetSupplier()?.text ?: ""

    override fun getSelection(): TextRange {
        val et = targetSupplier() ?: return TextRange(0, 0)
        return TextRange(et.selectionStart, et.selectionEnd)
    }

    override fun onRevokeCommit(snapshot: SelectionSnapshot) {
        val editText = targetSupplier() ?: return
        val editable = editText.text as? Editable ?: return
        editable.replace(0, editable.length, snapshot.beforeContent)
        editText.setSelection(snapshot.beforeStart)
    }
}
