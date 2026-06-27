package org.crazydan.studio.app.ime.kuaizi.engine.domain

class InputListEditor {
    private val undoStack = ArrayDeque<InputList>(50)
    private val redoStack = ArrayDeque<InputList>(50)
    private val maxStackSize = 50

    val canUndo: Boolean get() = undoStack.isNotEmpty()
    val canRedo: Boolean get() = redoStack.isNotEmpty()

    fun pushUndo(list: InputList) {
        if (undoStack.size >= maxStackSize) {
            undoStack.removeFirst()
        }
        undoStack.addLast(list)
        redoStack.clear()
    }

    private fun pushRedo(list: InputList) {
        if (redoStack.size >= maxStackSize) {
            redoStack.removeFirst()
        }
        redoStack.addLast(list)
    }

    fun undo(current: InputList): InputList {
        val previous = undoStack.removeLastOrNull() ?: return current
        pushRedo(current)
        return previous
    }

    fun redo(current: InputList): InputList {
        val next = redoStack.removeLastOrNull() ?: return current
        pushUndo(current)
        return next
    }
}
