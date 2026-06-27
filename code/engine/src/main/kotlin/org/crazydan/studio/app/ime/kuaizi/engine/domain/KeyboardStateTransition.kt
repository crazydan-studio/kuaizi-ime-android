package org.crazydan.studio.app.ime.kuaizi.engine.domain

import org.crazydan.studio.app.ime.kuaizi.engine.EditorAction
import org.crazydan.studio.app.ime.kuaizi.engine.ImeIntent

sealed class KeyboardStateTransition {
    data class InputPinyinChar(val char: Char) : KeyboardStateTransition()
    data class BeginSlip(val startKey: InputKey) : KeyboardStateTransition()
    data class BeginFlip(val startChar: Char) : KeyboardStateTransition()
    data class SelectSlipChar(val char: Char) : KeyboardStateTransition()
    data class SelectFlipChar(val char: Char) : KeyboardStateTransition()
    data class LoadCandidates(val candidates: List<InputWord>) : KeyboardStateTransition()
    data class FilterCandidates(val filter: PinyinWordFilter) : KeyboardStateTransition()
    data class AdvanceFilterCandidates(
        val radical: Radical?,
        val tone: Tone?,
    ) : KeyboardStateTransition()
    data class PageCandidates(val direction: org.crazydan.studio.app.ime.kuaizi.engine.PageDirection) : KeyboardStateTransition()
    data class LoadCommitOptions(val options: List<InputWord.CommitOption>) : KeyboardStateTransition()
    data class MoveCursor(val position: Int) : KeyboardStateTransition()
    data class SelectText(val start: Int, val end: Int) : KeyboardStateTransition()
    data class OpenSymbolGroup(val groupId: String?) : KeyboardStateTransition()
    data class OpenEmojiGroup(val groupId: String?) : KeyboardStateTransition()
    data object ReturnToIdle : KeyboardStateTransition()
    data object BackToPrevious : KeyboardStateTransition()
    data object LoadMoreCandidates : KeyboardStateTransition()
    data object SelectCandidate : KeyboardStateTransition()
    data object CommitInput : KeyboardStateTransition()
    data object DeleteInput : KeyboardStateTransition()
    data object BackToInput : KeyboardStateTransition()
    data object BackToChoosing : KeyboardStateTransition()

    data class Result(
        val newState: KeyboardState,
        val sideEffects: List<ImeIntent> = emptyList(),
        val editorAction: EditorAction? = null,
    )
}
