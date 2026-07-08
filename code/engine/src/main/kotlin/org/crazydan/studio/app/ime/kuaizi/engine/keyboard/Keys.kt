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

/**
 * 输入按键的密封类型，表示键盘上的一个可交互按键。
 *
 * 已被禁用的按键通过 [Disabled] 进行封装：
 * ```kotlin
 * val disabled = Disabled(key = key)
 * ```
 * 只有类型为 [Disabled] 的按键才是已被禁用的，其余类型均始终为已启用。
 */
sealed class InputKey {

    /** 字符按键 */
    sealed class Char : InputKey() {
        /** 字符按键的实际输入字符，其与 [label] 可能并不相等 */
        abstract val value: String

        /** 按键上显示的文字内容 */
        abstract val label: String?

        /** 字符按键的可替换字符列表，用于在同一按键上切换不同的字符，比如，英文字母的大小写切换等 */
        abstract val replacements: List<String>?

        /**
         * 字母按键
         * @property level 按键层级。
         * 在输入过程中需要按**输入组合的层级**（比如拼音输入需要拆分为**声母+韵母**两个层级）动态布局按键时设置属性。
         * 其中，`0` 表示初始层级，`-1` 表示最末层级。
         */
        data class Alphabet(
            val level: Int = 0,
            override val value: String,
            override val label: String? = null,
            override val replacements: List<String>? = null,
        ) : Char()

        /** 数字按键 */
        data class Number(
            val actual: Int,
            override val value: String = actual.toString(),
            override val label: String? = null,
            override val replacements: List<String>? = null,
        ) : Char()

        /** 空格按键 */
        data object Space : Char() {
            override val value: String = " "
            override val label: String? = null
            override val replacements: List<String>? = null
        }

        /** 符号按键 */
        data class Symbol(
            override val value: String,
            override val label: String? = null,
            override val replacements: List<String>? = null,
        ) : Char()

        /** 表情按键 */
        data class Emoji(
            override val value: String,
            override val label: String? = null,
            override val replacements: List<String>? = null,
        ) : Char()

        /**
         * 获取指定位置的可替换字符，循环获取，且按键 [value] 本身作为首字符参与替换列表的循环
         * @param index 若 `<= 0`，则直接返回按键字符 [value]
         * @return 若无可替换字符列表（[replacements] 为 `null` 或空），则返回按键字符 [value]
         */
        fun getReplacement(index: Int): String {
            if (index <= 0 || replacements == null || replacements!!.isEmpty())
                return value

            val i = index % (replacements!!.size + 1)
            return if (i == 0) value else replacements!![i - 1]
        }
    }

    // -----------------------------------------------------

    /** 控制按键 */
    data class Ctrl(
        val type: String,
    ) : InputKey()

    /** 数学运算按键 */
    data object MathOp : InputKey()

    /** 候选词选择按键 */
    data object Candidate : InputKey()

    // -----------------------------------------------------

    /** 空按键，表示无操作 */
    data object NoOp : InputKey()

    /**
     * 被禁用按键
     * @property key 原始按键
     */
    data class Disabled(val key: InputKey) : InputKey()
}
