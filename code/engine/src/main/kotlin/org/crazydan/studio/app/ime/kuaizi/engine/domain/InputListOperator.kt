package org.crazydan.studio.app.ime.kuaizi.engine.domain

class InputListOperator(private val editor: InputListEditor) {
    fun appendChar(list: InputList, char: InputItem.Char): InputList {
        editor.pushUndo(list)
        val mutableInputs = list.inputs.toMutableList()
        val gapIndex = list.gapIndex
        mutableInputs.add(gapIndex, char)
        mutableInputs.add(gapIndex + 1, InputItem.Gap(id = "gap_${System.nanoTime()}"))
        return list.copy(inputs = mutableInputs, gapIndex = gapIndex + 1)
    }

    fun deleteCharBeforeCursor(list: InputList): InputList {
        editor.pushUndo(list)
        val gapIdx = list.gapIndex
        if (gapIdx <= 0) return list

        val mutableInputs = list.inputs.toMutableList()
        val charIdx = gapIdx - 1
        if (charIdx >= 0 && mutableInputs[charIdx] is InputItem.Char) {
            mutableInputs.removeAt(charIdx)
            mutableInputs.removeAt(charIdx)
            return list.copy(
                inputs = mutableInputs,
                gapIndex = gapIdx - 1,
            )
        }
        return list
    }

    fun moveCursorTo(list: InputList, index: Int): InputList {
        val newGapIndex = index.coerceIn(0, list.inputs.size)
        return list.copy(gapIndex = newGapIndex)
    }

    fun clear(list: InputList): InputList {
        return InputList()
    }

    fun undo(list: InputList): InputList {
        return editor.undo(list)
    }

    fun redo(list: InputList): InputList {
        return editor.redo(list) ?: list
    }

    val canUndo: Boolean get() = editor.canUndo
    val canRedo: Boolean get() = editor.canRedo
}
