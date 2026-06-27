package org.crazydan.studio.app.ime.kuaizi.engine

import org.crazydan.studio.app.ime.kuaizi.engine.domain.*
import org.crazydan.studio.app.ime.kuaizi.engine.domain.user_data.ImportStrategy

sealed class ImeIntent {
    data class PressKey(val key: InputKey, val gesture: KeyGesture) : ImeIntent()
    data class LongPressKey(val key: InputKey) : ImeIntent()
    data class SelectCandidate(val candidate: InputWord) : ImeIntent()
    data class PageCandidate(val direction: PageDirection) : ImeIntent()
    data class SwitchKeyboard(val type: KeyboardType) : ImeIntent()
    data object CommitInput : ImeIntent()
    data object DeleteInput : ImeIntent()
    data object CleanInput : ImeIntent()
    data class MoveCursorTo(val index: Int) : ImeIntent()
    data class PerformEdit(val action: EditorEditAction) : ImeIntent()
    data class PasteClip(val text: String) : ImeIntent()
    data class SaveFavorite(val favorite: InputFavorite) : ImeIntent()
    data object ShowClipList : ImeIntent()
    data object ShowFavoriteList : ImeIntent()
    data object CloseClipList : ImeIntent()
    data object CloseFavoriteList : ImeIntent()
    data class LoadCandidates(val pinyin: String) : ImeIntent()
    data class SetCandidates(val candidates: CandidateList) : ImeIntent()
    data class UpdateConfig(val config: ImeConfig) : ImeIntent()
    data class ExportUserData(val filePath: String) : ImeIntent()
    data class ImportUserData(val filePath: String, val strategy: ImportStrategy) : ImeIntent()
}

enum class EditorEditAction {
    BACKSPACE, SELECT_ALL, COPY, CUT, PASTE, UNDO, REDO,
}

enum class PageDirection { Next, Previous }
