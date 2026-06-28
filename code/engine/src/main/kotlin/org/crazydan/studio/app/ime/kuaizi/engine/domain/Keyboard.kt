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

/** 键盘的完整状态描述，包含键盘类型、左右手模式和状态机位置 */
data class Keyboard(
    /** 键盘类型，决定按键集合的语义内容 */
    val type: KeyboardType = KeyboardType.Pinyin,
    /** 左右手模式的临时切换状态，null 表示未切换，使用 [UiConfig.keyboardHandMode] 的值 */
    val handMode: KeyboardHandMode? = null,
    /** 键盘状态机的当前状态 */
    val state: KeyboardState = KeyboardState.Idle,
)

/** 键盘类型，定义了键盘的内容类型和语义 */
enum class KeyboardType {
    /** 拼音键盘（主键盘） */
    Pinyin,
    /** 拉丁字母键盘（主键盘） */
    Latin,
    /** 数字键盘（主键盘） */
    Number,
    /** 数学表达式键盘（临时键盘） */
    Math,
    /** 符号选择键盘（临时键盘） */
    Symbol,
    /** Emoji 选择键盘（临时键盘） */
    Emoji,
    /** 编辑功能键盘（临时键盘） */
    Editor,
    /** 候选词选择键盘（超临时键盘） */
    Candidate,
    /** 提交选项键盘（超临时键盘） */
    CommitOption;

    /** 获取当前键盘类型的初始状态 */
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

/** 键盘输入模式，定义按键的几何排列和交互范式 */
enum class KeyboardInputMode {
    /** 六边形网格排列 */
    HexGrid,
    /** 矩形网格排列（传统 QWERTY） */
    RectGrid,
}

/** 左右手操作模式 */
enum class KeyboardHandMode {
    /** 左手模式 */
    Left,
    /** 右手模式 */
    Right,
}

/** 键盘主题类型 */
enum class KeyboardThemeType {
    /** 浅色主题 */
    Light,
    /** 深色主题 */
    Night,
    /** 跟随系统设置 */
    FollowSystem,
}

/** 输入按键的密封类型，表示键盘上的一个可交互按键 */
sealed class InputKey {
    /** 字符按键 */
    data class Char(val text: String) : InputKey()
    /** 控制按键，如 Shift、Backspace 等 */
    data class Ctrl(val type: String) : InputKey()
    /** 候选词选择按键 */
    data object Candidate : InputKey()
    /** 数学运算按键 */
    data object MathOp : InputKey()
    /** 符号按键 */
    data object Symbol : InputKey()
    /** 空按键，表示无操作 */
    data object Null : InputKey()
}

/** 按键手势类型 */
enum class KeyGesture {
    /** 单击（点击后立即抬起） */
    Tap,
    /** 滑行（手指在按键上滑动） */
    Slip,
    /** 翻动（快速滑出触发翻动） */
    Flip,
    /** 长按（按住不动持续一段时间） */
    Hold,
}

/** 翻动方向 */
enum class FlipDirection {
    /** 向左翻动 */
    Left,
    /** 向右翻动 */
    Right,
    /** 向上翻动 */
    Up,
    /** 向下翻动 */
    Down,
}
