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

import org.crazydan.studio.app.ime.kuaizi.engine.input.InputWord
import org.crazydan.studio.app.ime.kuaizi.engine.input.PinyinWordFilter
import org.crazydan.studio.app.ime.kuaizi.engine.input.Radical
import org.crazydan.studio.app.ime.kuaizi.engine.input.Tone

/**
 * 键盘交互的有限状态集，以 sealed class 层级表达。
 * 每种状态对应一种根据转换动作 [KeyboardStateTransition] 做状态驱动后的结果，
 * 而状态数据则以类属性形式记录在相应的状态中。
 *
 * 状态转换是与具体键盘类型和具体 UI 交互动作无关的，
 * 其由唯一的状态机 [KeyboardStateMachine] 做全局性转换控制，
 * 再由 [KeyboardIntentHandler] 根据 [ImeIntent] 生成相应的转换动作
 * [KeyboardStateTransition]，从而实现对键盘状态的驱动。
 */
sealed class KeyboardState {

    /** 空闲状态，无任何交互正在进行 */
    data object Idle : KeyboardState()

    // ------------------------------------------------------------------

    /** 拼音输入的状态分支 */
    sealed class Pinyin : KeyboardState() {

        /** 拼音待输入状态 */
        data object Waiting : Pinyin()

        /**
         * 拼音输入中状态
         * @param level0Key 滑行第 0 级按键
         * @param level1Key 滑行第 1 级按键
         * @param level2Key 滑行第 2 级按键
         * @param vowelTree 以 [level0Key] 为声母的拼音的韵母树。
         * 其中，key 为第一级韵母，value 为第二级韵母列表。
         * 如果 value 为空，则表示该拼音只有该单字符韵母，
         * 而若 value 中包含空字符，则表示第一级韵母本身就是一个有效拼音
         */
        data class Inputting(
            val level0Key: InputKey.Char.Alphabet,
            val level1Key: InputKey.Char.Alphabet? = null,
            val level2Key: InputKey.Char.Alphabet? = null,

            val vowelTree: Map<String, List<String>>,
        ) : Pinyin() {

            /** 已输入字符的有效组合 */
            fun getChars(): String =
                level0Key.value + (level1Key?.value ?: "") + (level2Key?.value ?: "")
        }
    }

    // ------------------------------------------------------------------

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

    // ------------------------------------------------------------------

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

    // ------------------------------------------------------------------

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

    // ------------------------------------------------------------------

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
}
