package org.crazydan.studio.app.ime.kuaizi.engine.bridge

import org.crazydan.studio.app.ime.kuaizi.engine.CursorDirection
import org.crazydan.studio.app.ime.kuaizi.engine.EditorEditAction
import org.crazydan.studio.app.ime.kuaizi.engine.TextRange

interface ImeEditorBridge {
    fun commitText(text: String, replacements: List<String>? = null)
    fun revokeCommit()
    fun insertPairedSymbols(left: String, right: String)
    fun moveCursor(direction: CursorDirection)
    fun selectRange(direction: CursorDirection)
    fun performEdit(action: EditorEditAction)
    fun getText(): CharSequence
    fun getSelection(): TextRange
}
