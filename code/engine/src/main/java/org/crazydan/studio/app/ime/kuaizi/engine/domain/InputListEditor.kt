package org.crazydan.studio.app.ime.kuaizi.engine.domain

class InputListEditor {
    private val undoStack = ArrayDeque<InputList>(50)
    private val redoStack = ArrayDeque<InputList>(50)
    private val maxStackSize = 50

    fun pushUndo(list: InputList) {
        if (undoStack.size >= maxStackSize) {
            undoStack.removeFirst()
        }
        undoStack.addLast(list)
        redoStack.clear()
    }

    fun pushRedo(list: InputList) {
        if (redoStack.size >= maxStackSize) {
            redoStack.removeFirst()
        }
        redoStack.addLast(list)
    }

    fun undo(current: InputList): InputList? {
        if (undoStack.isEmpty()) return null
        pushRedo(current)
        return undoStack.removeLast()
    }

    fun redo(current: InputList): InputList? {
        if (redoStack.isEmpty()) return null
        pushUndo(current)
        return redoStack.removeLast()
    }

    fun clear() {
        undoStack.clear()
        redoStack.clear()
    }
}
