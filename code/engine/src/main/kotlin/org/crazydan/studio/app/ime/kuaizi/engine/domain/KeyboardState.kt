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
        val options: List<InputWord.CommitOption> = emptyList(),
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
