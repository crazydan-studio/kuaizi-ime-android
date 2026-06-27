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
