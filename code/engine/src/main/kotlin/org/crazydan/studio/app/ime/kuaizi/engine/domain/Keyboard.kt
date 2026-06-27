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
