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

import kotlin.math.max
import kotlin.math.min

/**
 * 编辑器输入类型。
 *
 * 决定输入法启动时的键盘类型以及 Enter 按键的图标样式。
 */
enum class EditorInputType {
    /** 搜索框输入 */
    Filter,

    /** 自动填充 */
    AutoComplete,

    /** 数字输入 */
    Number,

    /** 日期输入 */
    Datetime,

    /** 电话输入 */
    Phone,

    /** 密码输入 */
    Password,

    /** 邮件输入 */
    Email,

    /** url 地址输入 */
    URI,

    /** 普通文本输入，在无法精确识别输入类型时，均采用该类型 */
    Text
}

// ----------------------------------------------------

/** 编辑器光标移动信息。*/
typealias EditorCursorMotion = Motion

/**
 * 编辑器编辑动作。
 *
 * 定义引擎可对目标编辑器执行的系统级编辑操作。
 */
enum class EditorEditAction {
    BACKSPACE, SELECT_ALL, COPY, CUT, PASTE, UNDO, REDO,
}

/**
 * 编辑器当前选区。
 *
 * @property start 起始位置（包含），其始终小于 [end]
 * @property end 结束位置（不包含），其始终大于 [start]
 * @property content 从 [start] 至 [end] 的选区范围内的已选中内容。其可能为空，也即，未选中任何内容
 */
data class EditorSelection(
    val start: Int, val end: Int,
    val content: CharSequence
) {

    companion object {

        fun empty() = create(0, 0, "")

        fun create(start: Int, end: Int, content: CharSequence) =
            EditorSelection(
                start = min(start, end),
                end = max(start, end),
                content = content,
            )
    }
}
