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
