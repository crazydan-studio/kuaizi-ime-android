package org.crazydan.studio.app.ime.kuaizi.engine.domain

sealed class KeyboardState {
    data object Idle : KeyboardState()

    sealed class PinyinInput : KeyboardState() {
        data class Waiting(val pending: InputItem.Char? = null) : PinyinInput()
        data class Slipping(
            val startKey: String = "",
            val level0Key: String = "",
            val level1Key: String? = null,
            val level2Key: String? = null,
        ) : PinyinInput()

        data class Flipping(
            val startChar: String = "",
            val candidates: List<String> = emptyList(),
        ) : PinyinInput()
    }

    sealed class CandidateSelection : KeyboardState() {
        data class Choosing(
            val candidates: List<InputWord> = emptyList(),
            val pageIndex: Int = 0,
            val pageSize: Int = 20,
        ) : CandidateSelection()

        data class Filtering(
            val spell: String = "",
        ) : CandidateSelection()

        data class AdvanceFiltering(
            val radical: String? = null,
            val tone: Int? = null,
        ) : CandidateSelection()
    }

    data class CommitOptionChoosing(
        val options: List<InputWord.CommitOption> = emptyList(),
    ) : KeyboardState()

    sealed class EditorEditing : KeyboardState() {
        data class CursorMoving(val position: Int = 0) : EditorEditing()
        data class TextSelecting(val start: Int = 0, val end: Int = 0) : EditorEditing()
    }

    data class SymbolChoosing(val groupId: String? = null) : KeyboardState()
    data class EmojiChoosing(val groupId: String? = null) : KeyboardState()
}
