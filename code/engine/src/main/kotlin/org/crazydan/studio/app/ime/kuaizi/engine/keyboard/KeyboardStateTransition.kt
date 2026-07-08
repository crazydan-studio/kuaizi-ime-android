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

import org.crazydan.studio.app.ime.kuaizi.engine.ImeIntent
import org.crazydan.studio.app.ime.kuaizi.engine.PageDirection
import org.crazydan.studio.app.ime.kuaizi.engine.bridge.EditorAction
import org.crazydan.studio.app.ime.kuaizi.engine.input.InputWord
import org.crazydan.studio.app.ime.kuaizi.engine.input.PinyinWordFilter
import org.crazydan.studio.app.ime.kuaizi.engine.input.Radical
import org.crazydan.studio.app.ime.kuaizi.engine.input.Tone

/**
 * 触发状态机转换的原子事件 sealed class。
 * 每个转换类型携带转换所需的上下文数据，由 [KeyboardIntentHandler] 子类
 * 在处理 [ImeIntent] 时构造并提交给 [KeyboardStateMachine]。
 *
 * 从推动状态转换的角度来命名子类，而不是以 UI 交互动作命名，
 * 状态转换是与具体交互动作无关的，不同形式的交互均可以触发相同的状态转换。
 * 比如，在拼音键盘中输入拼音时，不管交互是滑行还是点击形式，
 * 拼音输入仅根据输入开始 [StartInputPinyin]、输入中 [DoInputPinyin]
 * 和输入结束 [StopInputPinyin] 做状态转换，而至于是哪个交互动作触发了何种转换，
 * 则由具体的 [KeyboardIntentHandler] 做决定。
 */
sealed class KeyboardStateTransition {

    /**
     * 状态转换结果，表达转换执行后的输出
     * @param newState 转换后的新状态
     * @param sideEffects 需要异步处理的副作用意图列表
     * @param editorAction 编辑器操作，如提交输入、插入配对符号、选择文本、移动光标等
     */
    data class Result(
        val newState: KeyboardState,
        val sideEffects: List<ImeIntent>? = null,
        val editorAction: EditorAction? = null,
    )

    // ------------------------------------------------------------------------

    /** 回到空闲/初始状态 */
    data object ReturnToIdle : KeyboardStateTransition()

    /** 回退到前一状态 */
    data object BackToPrevious : KeyboardStateTransition()

    /** 无事可做 */
    data object NothingToDo : KeyboardStateTransition()

    // ------------------------------------------------------------------------

    /**
     * 输入字符：可用于单字符输入、可替换字符输入等
     * @property replacement 可替换字符的序号。大于 0 时有效
     */
    data class InputChar(val key: InputKey.Char, val replacement: Int = 0) : KeyboardStateTransition()

    // ------------------------------------------------------------------------

    /** 开始拼音输入 */
    data class StartInputPinyin(val key: InputKey.Char.Alphabet) : KeyboardStateTransition()

    /** 正在输入拼音 */
    data class DoInputPinyin(val key: InputKey.Char.Alphabet) : KeyboardStateTransition()

    /** 结束拼音输入 */
    data object StopInputPinyin : KeyboardStateTransition()

    // ------------------------------------------------------------------------

    /** 加载候选词 */
    data class LoadCandidates(val candidates: List<InputWord>) : KeyboardStateTransition()

    /** 按拼音过滤器过滤候选词 */
    data class FilterCandidates(val filter: PinyinWordFilter) : KeyboardStateTransition()

    /** 高级过滤（按部首和声调） */
    data class AdvanceFilterCandidates(
        val radical: Radical?,
        val tone: Tone?,
    ) : KeyboardStateTransition()

    /** 翻页候选词 */
    data class PageCandidates(val direction: PageDirection) : KeyboardStateTransition()

    /** 加载提交选项 */
    data class LoadCommitOptions(val options: List<InputWord.CommitOption>) : KeyboardStateTransition()

    /** 移动光标 */
    data class MoveCursor(val position: Int) : KeyboardStateTransition()

    /** 选择文本范围 */
    data class SelectText(val start: Int, val end: Int) : KeyboardStateTransition()

    /** 打开符号分组 */
    data class OpenSymbolGroup(val groupId: String?) : KeyboardStateTransition()

    /** 打开 Emoji 分组 */
    data class OpenEmojiGroup(val groupId: String?) : KeyboardStateTransition()

    /** 加载更多候选词 */
    data object LoadMoreCandidates : KeyboardStateTransition()

    /** 选择候选词 */
    data object SelectCandidate : KeyboardStateTransition()

    /** 提交输入 */
    data object CommitInput : KeyboardStateTransition()

    /** 删除输入 */
    data object DeleteInput : KeyboardStateTransition()

    /** 回到输入状态 */
    data object BackToInput : KeyboardStateTransition()

    /** 回到候选选择状态 */
    data object BackToChoosing : KeyboardStateTransition()
}
