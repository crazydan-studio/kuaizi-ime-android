package org.crazydan.studio.app.ime.kuaizi.engine.bridge

import org.crazydan.studio.app.ime.kuaizi.engine.CursorDirection
import org.crazydan.studio.app.ime.kuaizi.engine.EditorEditAction
import org.crazydan.studio.app.ime.kuaizi.engine.TextRange

data class SelectionSnapshot(
    val beforeContent: CharSequence,
    val beforeStart: Int,
    val beforeEnd: Int,
    val afterContent: CharSequence,
    val afterStart: Int,
    val afterEnd: Int,
)

abstract class BaseImeEditorBridge : ImeEditorBridge {
    private var revertion: SelectionSnapshot? = null

    protected fun resetRevertion() {
        revertion = null
    }

    protected fun recordRevertion(
        beforeStart: Int,
        beforeEnd: Int,
        beforeContent: CharSequence,
        afterStart: Int,
        afterEnd: Int,
    ) {
        revertion = SelectionSnapshot(
            beforeContent = beforeContent,
            beforeStart = beforeStart,
            beforeEnd = beforeEnd,
            afterContent = getText(),
            afterStart = afterStart,
            afterEnd = afterEnd,
        )
    }

    override fun revokeCommit() {
        val snapshot = revertion ?: return
        onRevokeCommit(snapshot)
        revertion = null
    }

    protected open fun onRevokeCommit(snapshot: SelectionSnapshot) {
        // subclasses override to restore editor state
    }

    abstract fun doNormalCommitText(text: String)
    abstract fun doReplaceableCommitText(text: String, replacements: List<String>)
}
