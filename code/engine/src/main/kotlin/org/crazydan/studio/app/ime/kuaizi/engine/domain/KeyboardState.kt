package org.crazydan.studio.app.ime.kuaizi.engine.domain

sealed class KeyboardState {
    data object Idle : KeyboardState()

    sealed class PinyinInput : KeyboardState() {
        data class Waiting(val pending: InputItem.Char? = null) : PinyinInput()
        data class Slipping(
            val startKey: InputKey,
            val level0Key: InputKey,
            val level1Key: InputKey? = null,
            val level2Key: InputKey? = null,
            val nextCharsByLength: Map<Int, List<String>> = emptyMap(),
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
            val filter: PinyinWordFilter,
            val filtered: List<InputWord> = emptyList(),
        ) : CandidateSelection()

        data class AdvanceFiltering(
            val radical: Radical? = null,
            val tone: Tone? = null,
            val filtered: List<InputWord> = emptyList(),
        ) : CandidateSelection()
    }

    data class CommitOptionChoosing(
        val options: List<CommitOption> = emptyList(),
        val hasSpell: Boolean = false,
        val hasVariant: Boolean = false,
    ) : KeyboardState()

    sealed class EditorEditing : KeyboardState() {
        data class CursorMoving(val position: Int = 0) : EditorEditing()
        data class TextSelecting(val start: Int = 0, val end: Int = 0) : EditorEditing()
    }

    data class SymbolChoosing(val groupId: String? = null) : KeyboardState()
    data class EmojiChoosing(val groupId: String? = null) : KeyboardState()
}
