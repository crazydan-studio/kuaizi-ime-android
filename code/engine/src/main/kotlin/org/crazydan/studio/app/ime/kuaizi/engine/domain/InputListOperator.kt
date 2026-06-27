package org.crazydan.studio.app.ime.kuaizi.engine.domain

class InputListOperator(
    private val editor: InputListEditor = InputListEditor(),
) {
    val canUndo: Boolean get() = editor.canUndo
    val canRedo: Boolean get() = editor.canRedo

    fun appendChar(list: InputList, char: InputItem.Char): InputList {
        editor.pushUndo(list)
        return list.appendChar(char)
    }

    fun deleteChar(list: InputList): InputList {
        editor.pushUndo(list)
        return list.deleteCharBeforeCursor()
    }

    fun commitPending(list: InputList): InputList {
        val pending = list.pending ?: return list
        editor.pushUndo(list)
        var result = list.copy(pending = null)
        for (char in pending.chars) {
            result = result.appendChar(char)
        }
        return result
    }

    fun moveCursor(list: InputList, newGapIndex: Int): InputList {
        return list.moveCursorTo(newGapIndex)
    }

    fun clean(list: InputList): InputList {
        if (list.isEmpty) return list
        editor.pushUndo(list)
        return list.clean()
    }

    fun withPending(list: InputList, pending: PendingInput?): InputList {
        return list.withPending(pending)
    }

    fun undo(current: InputList): InputList = editor.undo(current)

    fun redo(current: InputList): InputList = editor.redo(current)
}
