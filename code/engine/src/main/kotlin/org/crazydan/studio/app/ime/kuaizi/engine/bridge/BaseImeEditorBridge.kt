package org.crazydan.studio.app.ime.kuaizi.engine.bridge

import org.crazydan.studio.app.ime.kuaizi.engine.CursorDirection
import org.crazydan.studio.app.ime.kuaizi.engine.EditorEditAction
import org.crazydan.studio.app.ime.kuaizi.engine.TextRange

abstract class BaseImeEditorBridge : ImeEditorBridge {

    private data class SelectionSnapshot(
        val beforeStart: Int,
        val beforeEnd: Int,
        val beforeContent: String,
        val afterStart: Int,
        val afterEnd: Int,
    )

    private var revertion: SelectionSnapshot? = null

    protected fun resetRevertion() {
        revertion = null
    }

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

    override fun revokeCommit() {
        val snapshot = revertion ?: return
        revertion = null
        onRevokeCommit(snapshot)
    }

    protected abstract fun onRevokeCommit(snapshot: SelectionSnapshot)

    override fun commitText(text: String, replacements: List<String>?) {
        resetRevertion()
        if (replacements != null && replacements.isNotEmpty()) {
            doReplaceableCommitText(text, replacements)
        } else {
            doNormalCommitText(text)
        }
    }

    protected abstract fun doNormalCommitText(text: String)
    protected abstract fun doReplaceableCommitText(text: String, replacements: List<String>)
}
