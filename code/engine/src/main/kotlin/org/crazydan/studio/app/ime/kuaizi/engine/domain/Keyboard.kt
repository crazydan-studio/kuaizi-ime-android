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

import org.crazydan.studio.app.ime.kuaizi.engine.ImeConfig

data class Keyboard(
    val type: KeyboardType = KeyboardType.Pinyin,
    val handMode: KeyboardHandMode? = null,
    val state: KeyboardState = KeyboardState.Idle,
)

enum class KeyboardType {
    Pinyin, Latin, Number, Symbol, Emoji, Math, Editor, Candidate, CommitOption;

    fun initialState(): KeyboardState {
        return when (this) {
            Pinyin, Latin -> KeyboardState.PinyinInput.Waiting()
            Number, Math -> KeyboardState.Idle
            Symbol -> KeyboardState.SymbolChoosing()
            Emoji -> KeyboardState.EmojiChoosing()
            Candidate -> KeyboardState.CandidateSelection.Choosing()
            CommitOption -> KeyboardState.CommitOptionChoosing()
            Editor -> KeyboardState.EditorEditing.CursorMoving()
        }
    }
}

enum class KeyboardInputMode { HexGrid, RectGrid }
enum class KeyboardHandMode { Left, Right }
enum class KeyboardThemeType { Light, Night, FollowSystem }

sealed class InputKey {
    data class Char(val text: String) : InputKey()
    data class Ctrl(val type: String) : InputKey()
    data object Candidate : InputKey()
    data object MathOp : InputKey()
    data object Symbol : InputKey()
    data object Null : InputKey()
}

enum class KeyGesture { Tap, Slip, Flip, Hold }
enum class FlipDirection { Left, Right, Up, Down }
