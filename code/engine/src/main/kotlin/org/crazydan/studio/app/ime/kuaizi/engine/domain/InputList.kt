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

/** 输入列表元素的密封类型基类，所有子类共享唯一的 id 字段 */
sealed class InputItem {
    /** 元素的唯一标识，用于 Compose key 和动画标识 */
    abstract val id: String

    /**
     * 字符输入项，承载用户输入的字符数据
     * @param id 唯一标识
     * @param text 字符的显示文本
     * @param keys 触发该字符的按键序列
     * @param replacements 可替换文本列表，用于标点符号的长按替换
     * @param word 关联的候选词信息
     * @param pairSymbol 配对符号信息，null 表示非配对符号
     */
    data class Char(
        override val id: String,
        val text: String,
        val keys: List<InputKey>,
        val replacements: List<String> = emptyList(),
        val word: InputWord? = null,
        val pairSymbol: PairSymbol? = null,
    ) : InputItem() {
        /** 是否为配对符号 */
        val hasPair: Boolean get() = pairSymbol != null

        /** 是否存在多个替换选项（至少两个才构成可轮换的替换列表） */
        val hasReplacements: Boolean get() = replacements.size > 1

        /**
         * 获取替换轮换中的下一个文本
         * @param text 当前文本
         * @return 替换列表中的下一个文本，若无替换则返回当前文本
         */
        fun nextReplacement(text: String): String {
            if (replacements.size <= 1) return text
            val index = replacements.indexOf(text)
            return if (index >= 0) replacements[(index + 1) % replacements.size] else replacements[0]
        }

        /**
         * 判断指定按键是否可以触发替换操作
         * @param key 字符按键
         * @return 是否可以替换
         */
        fun canReplace(key: InputKey.Char): Boolean =
            replacements.size > 1 && key.text in replacements
    }

    /** 游标间隔标记，所有实例共享同一身份 */
    data object Gap : InputItem() {
        override val id = "gap"
    }

    /**
     * 数学表达式输入项，内部持有一个完整的嵌套输入列表
     * @param id 唯一标识
     * @param nestedList 嵌套的数学输入列表
     */
    data class MathExpr(
        override val id: String,
        val nestedList: InputList,
    ) : InputItem()
}

/**
 * 输入列表，管理用户输入的字符序列、游标位置、待确认输入和嵌套数学表达式。
 * 采用不可变 data class 设计，所有状态变更通过 copy() 创建新实例。
 *
 * @param inputs Char-Gap 交替排列的输入序列
 * @param gapIndex 当前游标位置（指向 inputs 列表中的 Gap 索引）
 * @param pending 待确认的拼音输入
 * @param mathExprNested 嵌套的数学输入列表
 */
data class InputList(
    val inputs: List<InputItem> = emptyList(),
    val gapIndex: Int = 0,
    val pending: PendingInput? = null,
    val mathExprNested: InputList? = null,
) {
    init {
        require(gapIndex >= 0)
        require(gapIndex <= inputs.lastIndex + 1)
    }

    /** 当前游标位置的 Gap 元素 */
    val cursorGap: InputItem.Gap
        get() = inputs.getOrElse(gapIndex) { InputItem.Gap }

    /** 可见的字符输入列表（排除 Gap） */
    val visibleInputs: List<InputItem.Char>
        get() = inputs.filterIsInstance<InputItem.Char>()

    /** 输入文本内容，将 visibleInputs 的 text 字段拼接为完整字符串 */
    val text: String
        get() = visibleInputs.joinToString("") { it.text }

    /** 输入列表是否为空（仅包含 Gap） */
    val isEmpty: Boolean
        get() = inputs.all { it is InputItem.Gap }

    /**
     * 在游标位置追加字符输入
     * @param char 要追加的字符输入项
     * @return 新的 InputList 实例
     */
    fun appendChar(char: InputItem.Char): InputList {
        val newInputs = inputs.toMutableList().apply {
            add(gapIndex, char)
            add(gapIndex + 1, InputItem.Gap)
        }
        return copy(inputs = newInputs, gapIndex = gapIndex + 2)
    }

    /**
     * 删除游标前的一个字符
     * @return 新的 InputList 实例
     */
    fun deleteCharBeforeCursor(): InputList {
        if (gapIndex < 2) return this
        val newInputs = inputs.toMutableList().apply {
            removeAt(gapIndex - 2)
            removeAt(gapIndex - 2)
        }
        return copy(inputs = newInputs, gapIndex = gapIndex - 2)
    }

    /**
     * 移动游标到指定位置
     * @param newGapIndex 目标游标索引
     * @return 新的 InputList 实例
     */
    fun moveCursorTo(newGapIndex: Int): InputList {
        val clampedIndex = newGapIndex.coerceIn(0, inputs.lastIndex)
        return copy(gapIndex = clampedIndex)
    }

    /** 清空所有输入，返回空输入列表 */
    fun clean(): InputList = InputList()

    /**
     * 设置待确认输入
     * @param pending 待确认输入信息
     * @return 新的 InputList 实例
     */
    fun withPending(pending: PendingInput?): InputList =
        copy(pending = pending)
}

/**
 * 待确认的拼音输入数据
 * @param chars 待确认的拼音字符列表
 * @param completions 输入补全列表
 * @param pinyinToggles 拼音切换类型集合，如全拼/双拼/注音
 */
data class PendingInput(
    val chars: List<InputItem.Char>,
    val completions: List<InputCompletion> = emptyList(),
    val pinyinToggles: Set<PinyinToggleType> = emptySet(),
)

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

/**
 * 配对符号结构信息
 * @param open 左半部分符号
 * @param close 右半部分符号
 * @param content 左右符号之间的可选内容，null 表示内部为空
 */
data class PairSymbol(
    val open: String,
    val close: String,
    val content: String? = null,
)

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
    fun needsGap(left: InputItem.Char, right: InputItem.Char): Boolean {
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

    private fun isLatinChar(char: InputItem.Char): Boolean =
        char.word is InputWord.Latin || char.text.all { it.isLetter() && it.code < 128 }

    private fun isDigitChar(char: InputItem.Char): Boolean =
        char.text.all { it.isDigit() }
}
