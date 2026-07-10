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

package org.crazydan.studio.app.ime.kuaizi.engine.keyboard

/** 键盘的完整状态描述，包含键盘类型、左右手模式和当前状态 */
data class Keyboard(
    /** 键盘类型，决定按键集合的语义内容 */
    val type: KeyboardType = KeyboardType.Pinyin,
    /** 键盘当前状态 */
    val state: KeyboardState = KeyboardState.Idle,

    /** 左右手模式的临时切换状态，null 表示未切换，使用 [ImeConfig.Ui.keyboardHandMode] 的值 */
    val handMode: KeyboardHandMode? = null,

    /** 主键盘类型：临时性键盘需要退回到在其切换前所在的主键盘上 */
    val masterType: KeyboardType? = null,
)

/** 键盘类型，定义了键盘的内容类型和语义 */
enum class KeyboardType {
    /** 拼音键盘（主键盘） */
    Pinyin,

    /** 拉丁字母键盘（主键盘） */
    Latin,

    /** 数字键盘（主键盘） */
    Number,

    /** 数学表达式键盘 */
    Math,

    /** 符号选择键盘 */
    Symbol,

    /** Emoji 选择键盘 */
    Emoji,

    /** 编辑器操作键盘 */
    Editor,

    /** 候选词选择键盘 */
    Candidate,

    /** 提交选项键盘 */
    CommitOption;

    /** 获取当前键盘类型的初始状态 */
    fun initialState(): KeyboardState =
        when (this) {
            Pinyin, Latin, Number, Math -> KeyboardState.Idle
            Symbol -> KeyboardState.SymbolChoosing()
            Emoji -> KeyboardState.EmojiChoosing()
            Candidate -> KeyboardState.CandidateSelection.Choosing()
            CommitOption -> KeyboardState.CommitOptionChoosing()
            Editor -> KeyboardState.Editor.CursorMoving()
        }

    /**
     * 是否为主键盘。
     * 主键盘为常驻性键盘，其余键盘均属于临时性键盘，用于临时切换以进行一些操作，
     * 其在退出后均需要回到切换前所在的主键盘上。
     */
    fun isMaster(): Boolean =
        when (this) {
            Pinyin, Latin, Number -> true
            else -> false
        }
}

/** 键盘输入模式，定义按键的交互范式 */
enum class KeyboardInputMode {
    /** 滑行输入 */
    Swip,

    /** 点击输入：不支持全屏键盘模式 */
    Tap,
}

/** 键盘按键布局模式，定义按键的排列方式，并在一定程度上限制了所支持的 [KeyboardInputMode] */
enum class KeyboardKeyLayoutMode {
    /** 网格布局，支持滑行和点击输入 */
    Grid, // TODO 细分为正六边形网格、矩形网格？

    /** X 型布局，仅支持滑行输入 */
    XPad,
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

