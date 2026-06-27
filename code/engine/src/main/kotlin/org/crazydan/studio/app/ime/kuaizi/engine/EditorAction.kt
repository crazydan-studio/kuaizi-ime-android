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
