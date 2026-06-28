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

/**
 * 键盘交互的有限状态集，以 sealed class 层级表达。
 * 每种状态对应一种键盘交互模式，状态数据作为子类字段内嵌。
 */
sealed class KeyboardState {
    /** 空闲状态，无任何交互正在进行 */
    data object Idle : KeyboardState()

    /**
     * 拼音输入状态分支，包含等待、滑行和翻动三种子状态，
     * 覆盖了拼音键盘的核心交互方式
     */
    sealed class PinyinInput : KeyboardState() {
        /**
         * 等待输入状态
         * @param pending 未确认的拼音字符，null 表示无待确认输入
         */
        data class Waiting(val pending: InputItem.Char? = null) : PinyinInput()

        /**
         * 滑行输入状态
         * @param startKey 滑行起始按键
         * @param level0Key 滑行第一级按键
         * @param level1Key 滑行第二级按键
         * @param level2Key 滑行第三级按键
         * @param nextCharsByLength 按字符长度分组的可输入字符
         */
        data class Slipping(
            val startKey: InputKey,
            val level0Key: InputKey,
            val level1Key: InputKey? = null,
            val level2Key: InputKey? = null,
            val nextCharsByLength: Map<Int, List<String>> = emptyMap(),
        ) : PinyinInput()

        /**
         * 翻动输入状态
         * @param startChar 翻动起始字符
         * @param candidates 翻动产生的候选字符列表
         */
        data class Flipping(
            val startChar: String = "",
            val candidates: List<String> = emptyList(),
        ) : PinyinInput()
    }

    /**
     * 候选选择状态分支，包含选择、过滤和高级过滤三个递进子状态
     */
    sealed class CandidateSelection : KeyboardState() {
        /**
         * 基础候选选择状态
         * @param candidates 候选词列表
         * @param pageIndex 当前页码
         * @param pageSize 每页大小
         */
        data class Choosing(
            val candidates: List<InputWord> = emptyList(),
            val pageIndex: Int = 0,
            val pageSize: Int = 20,
        ) : CandidateSelection()

        /**
         * 拼音过滤状态
         * @param filter 拼音过滤条件
         * @param filtered 过滤后的候选词列表
         */
        data class Filtering(
            val filter: PinyinWordFilter,
            val filtered: List<InputWord> = emptyList(),
        ) : CandidateSelection()

        /**
         * 高级过滤状态（按部首和声调）
         * @param radical 选中的部首
         * @param tone 选中的声调
         * @param filtered 过滤后的候选词列表
         */
        data class AdvanceFiltering(
            val radical: Radical? = null,
            val tone: Tone? = null,
            val filtered: List<InputWord> = emptyList(),
        ) : CandidateSelection()
    }

    /**
     * 提交选项选择状态
     * @param options 可用的提交选项列表
     * @param hasSpell 是否包含拼音拼写切换选项
     * @param hasVariant 是否包含变体切换选项
     */
    data class CommitOptionChoosing(
        val options: List<InputWord.CommitOption> = emptyList(),
        val hasSpell: Boolean = false,
        val hasVariant: Boolean = false,
    ) : KeyboardState()

    /**
     * 编辑器编辑状态分支，包含光标移动和文本范围选择两种子状态
     */
    sealed class EditorEditing : KeyboardState() {
        /**
         * 光标移动状态
         * @param position 光标在输入列表中的索引
         */
        data class CursorMoving(val position: Int = 0) : EditorEditing()

        /**
         * 文本范围选择状态
         * @param start 选择起始索引
         * @param end 选择结束索引
         */
        data class TextSelecting(val start: Int = 0, val end: Int = 0) : EditorEditing()
    }

    /**
     * 符号选择状态
     * @param groupId 当前选中的符号分组，null 表示默认分组
     */
    data class SymbolChoosing(val groupId: String? = null) : KeyboardState()

    /**
     * Emoji 选择状态
     * @param groupId 当前选中的 Emoji 分组，null 表示默认分组
     */
    data class EmojiChoosing(val groupId: String? = null) : KeyboardState()
}
