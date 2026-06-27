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

import org.crazydan.studio.app.ime.kuaizi.engine.domain.CandidateList
import org.crazydan.studio.app.ime.kuaizi.engine.domain.ImportStrategy
import org.crazydan.studio.app.ime.kuaizi.engine.domain.InputFavorite
import org.crazydan.studio.app.ime.kuaizi.engine.domain.InputKey
import org.crazydan.studio.app.ime.kuaizi.engine.domain.InputWord
import org.crazydan.studio.app.ime.kuaizi.engine.domain.KeyGesture
import org.crazydan.studio.app.ime.kuaizi.engine.domain.KeyboardType


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
