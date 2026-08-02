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
             * @property fullWidth 是否为全角符号：其视觉宽度占两个拉丁字符空间（https://zh.wikipedia.org/zh-cn/%E5%85%A8%E5%BD%A2%E5%92%8C%E5%8D%8A%E5%BD%A2）
             * @property close 闭合符号：当前输入项作为与其配对的开符号
             */
            data class Symbol(
                override val value: String,
                val fullWidth: Boolean = false,
                val close: Symbol? = null,
            ) : Char()

            /** 拉丁文（字母 + 数字）输入项（多字符） */
            data class Latin(
                val chars: List<String>,
                val spell: Spell? = null,
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
         * @property word 该拼音的候选字：仅针对有效拼音
         */
        data class Pinyin(
            override val value: String,
            val valid: Boolean,
            val word: InputWord.Hanzi? = null,
        ) : Item()

        /**
         * 算术表达式输入项
         * @property inputList 表达式输入列表
         */
        data class MathExpr(
            override val value: String = "",
            val inputList: MathInputList = MathInputList(),
        ) : Item()
    }
}

// -----------------------------------------------------------------

/** 获取与当前输入项配对的闭合输入项 */
fun CommonInput.Item.getClose(): CommonInput.Item? =
    when (this) {
        is CommonInput.Item.Char.Symbol -> close
        else -> null
    }

/** 按配置将当前输入项转换为提交至目标编辑器的文本 */
fun CommonInput.Item.getText(option: InputTextOption): CharSequence =
    when (this) {
        is CommonInput.Item.MathExpr ->
            inputList.getText(option)

        is CommonInput.Item.Pinyin if word != null -> {
            val spell = word.spell.value
            val char = when (option.hanziType) {
                InputWord.Hanzi.Type.Traditional -> word.variant
                else -> null
            } ?: word.text

            when (option.spellUseMode) {
                InputWord.SpellUseMode.Replace -> spell
                InputWord.SpellUseMode.Follow -> "$char($spell)"
                else -> char
            }
        }

        is CommonInput.Item.Char.Latin
            if spell != null && option.spellUseMode != null
            ->
            spell.value.let {
                when (option.spellUseMode) {
                    InputWord.SpellUseMode.Replace -> "/$it/"
                    InputWord.SpellUseMode.Follow -> "$value /$it/"
                }
            }

        else -> value
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

/**
 * 输入的可提交文本的转换选项：用于控制将输入转换为何种形式的文本
 * @property hanziType 汉字类型：向目标编辑器提交汉字的简体或繁体形式。
 * 为 `null` 时，采用简体字
 * @property spellUseMode 汉字/英文读音使用模式：
 * 向目标编辑器提交携带读音的汉字/英文（如 `汉(hàn)字(zì)`、`better /ˈbetər/ world /wɜːld/`）
 * 或将汉字/英文替换为其读音（如 `hàn zì`、`/ˈbetər/ /wɜːld/`）。为 `null` 时，不携带读音，仅为汉字/英文
 * @property mathResultPrecision 算术表达式计算结果的精度（小数点位数）。缺省为 `4`，从而支持万分数 `‱` 的精度
 */
data class InputTextOption(
    val hanziType: InputWord.Hanzi.Type? = null,
    val spellUseMode: InputWord.SpellUseMode? = null,
    val mathResultPrecision: Int = 4,
)

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
