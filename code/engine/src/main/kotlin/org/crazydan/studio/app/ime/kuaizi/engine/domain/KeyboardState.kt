package org.crazydan.studio.app.ime.kuaizi.engine.domain

sealed class KeyboardState {
    data object Idle : KeyboardState()

    sealed class PinyinInput : KeyboardState() {
        data object Waiting : PinyinInput()
        data class Slipping(
            val startKey: String = "",
            val levels: List<String> = emptyList(),
            val nextChars: List<String> = emptyList(),
        ) : PinyinInput()

        data class Flipping(
            val startChar: String = "",
            val candidates: List<String> = emptyList(),
        ) : PinyinInput()
    }

    sealed class CandidateSelection : KeyboardState() {
        data class Choosing(
            val pageIndex: Int = 0,
        ) : CandidateSelection()

        data class Filtering(
            val spell: String = "",
        ) : CandidateSelection()

        data class AdvanceFiltering(
            val radical: String? = null,
            val tone: Int? = null,
        ) : CandidateSelection()
    }

    data object CommitOptionChoosing : KeyboardState()

    sealed class EditorEditing : KeyboardState() {
        data class CursorMoving(val position: Int = 0) : EditorEditing()
        data class TextSelecting(val start: Int = 0, val end: Int = 0) : EditorEditing()
    }

    data class SymbolChoosing(val groupId: String? = null) : KeyboardState()
    data class EmojiChoosing(val groupId: String? = null) : KeyboardState()
}
