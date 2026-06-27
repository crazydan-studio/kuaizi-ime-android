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

package org.crazydan.studio.app.ime.kuaizi.engine

typealias ImeEditorAction = EditorAction

sealed class EditorAction {
    abstract val timestamp: Long

    data class CommitText(
        override val timestamp: Long,
        val text: String,
        val replacements: List<String>? = null,
    ) : EditorAction()

    data class RevokeCommit(override val timestamp: Long) : EditorAction()
    data class InsertPairedSymbols(
        override val timestamp: Long,
        val left: String,
        val right: String,
    ) : EditorAction()

    data class MoveCursor(
        override val timestamp: Long,
        val direction: CursorDirection,
    ) : EditorAction()

    data class SelectRange(
        override val timestamp: Long,
        val direction: CursorDirection,
    ) : EditorAction()

    data class PerformEdit(
        override val timestamp: Long,
        val action: EditorEditAction,
    ) : EditorAction()
}

enum class CursorDirection { Left, Right, Up, Down, Home, End }

data class TextRange(val start: Int, val end: Int)
