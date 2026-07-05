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

/** 输入按键的密封类型，表示键盘上的一个可交互按键 */
sealed class InputKey {

    /** 字符按键 */
    sealed class Char : InputKey() {

        /** 字母按键 */
        data class Alphabet(
            val value: String,
            val replacements: List<String> = emptyList(),
        ) : Char()

        /** 数字按键 */
        data class Number(val value: Int) : Char()

        /** 符号按键 */
        data class Symbol(val value: String) : Char()

        /** 表情按键 */
        data class Emoji(val value: String) : Char()
    }

    // -----------------------------------------------------

    /** 控制按键，如 Shift、Backspace 等 */
    data class Ctrl(val type: String) : InputKey()

    /** 数学运算按键 */
    data object MathOp : InputKey()

    /** 候选词选择按键 */
    data object Candidate : InputKey()

    // -----------------------------------------------------

    /** 空按键，表示无操作 */
    data object Null : InputKey()
}