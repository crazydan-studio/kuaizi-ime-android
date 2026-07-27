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

package org.crazydan.studio.app.ime.kuaizi.engine.input

import org.crazydan.studio.app.ime.kuaizi.engine.input.math.MathInputList

/** 通用输入 */
sealed class CommonInput {

    /** [Item] 之间的空隙，便于在相邻输入项之间添加其他输入项 */
    data object Gap : CommonInput()

    /** 输入项：承载实际可见的输入内容 */
    sealed class Item : CommonInput() {
        /** 输入项字符值 */
        abstract val value: String

        /** 字符输入项，承载用户输入的字符数据 */
        sealed class Char : Item() {

            /** 空格输入项 */
            data object Space : Char() {
                override val value: String = " "
            }

            /** 表情输入项（单字符） */
            data class Emoji(
                override val value: String,
            ) : Char()

            /**
             * 符号输入项（单字符）
             * @property right 左配对符号的右配对符号
             */
            data class Symbol(
                override val value: String,
                val right: Symbol? = null,
            ) : Char()

            /** 拉丁文（字母 + 数字）输入项（多字符） */
            data class Latin(
                val chars: List<String>,
            ) : Char() {
                override val value: String
                    get() = chars.joinToString("")

                init {
                    require(chars.isNotEmpty())
                }
            }
        }

        /**
         * 拼音输入项
         * @property valid 是否为有效拼音
         * @property word 该（有效）拼音的候选字
         */
        data class Pinyin(
            override val value: String,
            val valid: Boolean,
            val word: InputWord.Pinyin? = null,
        ) : Item()

        /**
         * 算术表达式输入项
         * @property inputList 表达式输入列表
         */
        data class MathExpr(
            val inputList: MathInputList = MathInputList(),
            override val value: String = "",
        ) : Item()
    }
}

// -----------------------------------------------------------------

/** 输入项是否为空：主要针对 [CommonInput.Item.MathExpr] 和 `null`，其余均不应该为空 */
fun CommonInput.Item?.isEmpty(): Boolean =
    when (this) {
        null -> true
        is CommonInput.Item.MathExpr -> inputList.isEmpty()
        else -> false
    }

/** 获取输入项的配对输入项 */
fun CommonInput.Item.getPair(): CommonInput.Item? =
    when (this) {
        is CommonInput.Item.Char.Symbol -> right
        else -> null
    }

/** 丢弃最后一个字符 */
fun CommonInput.Item.Char.Latin.dropLastChar(): CommonInput.Item.Char.Latin =
    copy(chars = chars.dropLast(1))

/** 向尾部追加字符，或替换尾部字符 */
fun CommonInput.Item.Char.Latin.appendChar(
    char: String,
    replacements: List<String>?,
): CommonInput.Item.Char.Latin =
    copy(
        chars = (
                replacements?.first { chars.last() == it }
                    // 替换最后一个
                    ?.let { chars.dropLast(1) }
                // 追加
                    ?: chars
                ) + char
    )

/** 应用算术表达式输入列表更新 */
fun CommonInput.Item.MathExpr.applyInputListUpdate(
    block: MathInputList.() -> MathInputList,
): CommonInput.Item.MathExpr =
    copy(inputList = inputList.block())

// -----------------------------------------------------------------

/** 输入补全的密封类型基类 */
sealed class InputCompletion {
    /** 补全的完整文本 */
    abstract val text: String

    /**
     * 拉丁词汇补全建议
     * @param text 补全的完整词汇
     * @param remaining 用户尚未输入的剩余部分
     */
    data class LatinWord(
        override val text: String,
        val remaining: String,
    ) : InputCompletion()

    /**
     * 拼音词组补全建议
     * @param text 补全的中文词组文本
     * @param remaining 用户尚未输入的拼音部分
     * @param spells 词组各字的拼音拼写列表
     */
    data class PhraseWord(
        override val text: String,
        val remaining: String,
        val spells: List<String>,
    ) : InputCompletion()
}

/** 拼音切换类型 */
enum class PinyinToggleType {
    /** 全拼模式 */
    FullPinyin,

    /** 双拼模式 */
    DoublePinyin,

    /** 注音模式 */
    Bopomofo,

    /** 显示声调 */
    ShowTone,
}

/**
 * 输入间距规则，定义不同类型字符之间的 Gap 插入策略。
 * 核心原则是：游标仅在语义边界处停留。
 */
object InputGapSpacing {
    /**
     * 判断两个相邻 Char 之间是否需要插入 Gap
     * @param left 左侧字符
     * @param right 右侧字符
     * @return true 表示需要间隔（游标可停留），false 表示紧密连接
     */
    fun needsGap(left: CommonInput.Char, right: CommonInput.Char): Boolean {
        // 配对符号内部不需要间隔
        if (left.pairSymbol != null && left.pairSymbol.content == null) return false
        // 同一拼音词组的字符不需要间隔
        if (left.word is InputWord.PinyinPhrase && left.word == right.word) return false
        // 英文单词内部不需要间隔
        if (isLatinChar(left) && isLatinChar(right)) return false
        // 数字序列内部不需要间隔
        if (isDigitChar(left) && isDigitChar(right)) return false
        // 其他情况均需间隔
        return true
    }

    private fun isLatinChar(char: CommonInput.Char): Boolean =
        char.word is InputWord.Latin || char.value.all { it.isLetter() && it.code < 128 }

    private fun isDigitChar(char: CommonInput.Char): Boolean =
        char.value.all { it.isDigit() }
}
