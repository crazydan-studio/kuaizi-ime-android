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
            Pinyin, Latin -> KeyboardState.PinyinInput.Waiting
            Number, Math -> KeyboardState.Idle
            Symbol -> KeyboardState.SymbolChoosing
            Emoji -> KeyboardState.EmojiChoosing
            Candidate -> KeyboardState.CandidateSelection.Choosing
            CommitOption -> KeyboardState.CommitOptionChoosing
            Editor -> KeyboardState.EditorEditing.CursorMoving
        }
    }
}

enum class KeyboardInputMode { HexGrid, RectGrid }
enum class KeyboardHandMode { Left, Right }
enum class KeyboardThemeType { Light, Night, FollowSystem }

enum class InputKey {
    Char, Ctrl, Candidate, MathOp, Symbol, Null;
}

enum class KeyGesture { Tap, LongPress, Swipe, Flip }
enum class FlipDirection { Left, Right, Up, Down }
