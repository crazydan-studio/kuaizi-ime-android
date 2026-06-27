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
