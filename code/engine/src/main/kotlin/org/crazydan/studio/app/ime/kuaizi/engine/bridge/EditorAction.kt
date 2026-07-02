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

package org.crazydan.studio.app.ime.kuaizi.engine.bridge

import kotlin.math.max
import kotlin.math.min

/**
 * 引擎的编辑器操作 sealed class。
 *
 * 由引擎内部的 [ImeEngine.dispatchEditorAction] 统一分发到 [ImeEditorBridge]，
 * 桥梁实现者无需理解 [EditorAction] 类型体系。
 * 每种输出类型携带时间戳，用于日志记录和调试追踪。
 *
 * @property timestamp 操作时间戳
 */
sealed class EditorAction {
    abstract val timestamp: Long

    /**
     * 提交文本到编辑器。
     *
     * @property timestamp 操作时间戳
     * @property text 要提交的文本内容
     * @property replacements 替换列表，支持直输模式下的字符轮换
     */
    data class CommitText(
        override val timestamp: Long,
        val text: String,
        val replacements: List<String>? = null,
    ) : EditorAction()

    /** 撤回提交：撤销上一次 [CommitText] 操作。 */
    data class RevokeCommit(override val timestamp: Long) : EditorAction()

    /**
     * 插入配对符号：在光标位置插入左右配对符号对。
     *
     * @property timestamp 操作时间戳
     * @property left 左符号
     * @property right 右符号
     */
    data class InsertPairedSymbols(
        override val timestamp: Long,
        val left: String,
        val right: String,
    ) : EditorAction()

    /**
     * 移动光标：向指定方向移动编辑器光标。
     *
     * @property timestamp 操作时间戳
     * @property motion 光标移动信息
     */
    data class MoveCursor(
        override val timestamp: Long,
        val motion: EditorCursorMotion,
    ) : EditorAction()

    /**
     * 选择范围：从光标位置向指定方向选取文本范围。
     *
     * @property timestamp 操作时间戳
     * @property motion 光标移动信息
     */
    data class SelectRange(
        override val timestamp: Long,
        val motion: EditorCursorMotion,
    ) : EditorAction()

    /**
     * 执行编辑操作：对编辑器执行系统级编辑动作。
     *
     * @property timestamp 操作时间戳
     * @property action 要执行的编辑动作
     */
    data class PerformEdit(
        override val timestamp: Long,
        val action: EditorEditAction,
    ) : EditorAction()
}

/**
 * 编辑器编辑动作。
 *
 * 定义引擎可对目标编辑器执行的系统级编辑操作。
 */
enum class EditorEditAction {
    BACKSPACE, SELECT_ALL, COPY, CUT, PASTE, UNDO, REDO,
}

/** 光标移动方向。 */
enum class CursorDirection { Left, Right, Up, Down }

/**
 * 编辑器光标移动信息。
 *
 * @property direction 移动方向
 * @property distance 移动距离
 */
data class EditorCursorMotion(val direction: CursorDirection, val distance: Float)

/**
 * 编辑器当前选区。
 *
 * @property start 起始位置（包含），其始终小于 [end]
 * @property end 结束位置（不包含），其始终大于 [start]
 * @property reversed 是否为反向选择，即，实际的起点位置大于终点位置
 * @property content 从 [start] 至 [end] 的选区范围内的已选中内容。其可能为空，也即，未选中任何内容
 */
data class EditorSelection(
    val start: Int, val end: Int,
    val reversed: Boolean,
    val content: CharSequence
) {

    companion object {

        fun empty() = create(0, 0, "")

        fun create(start: Int, end: Int, content: CharSequence) =
            EditorSelection(
                start = min(start, end),
                end = max(start, end),
                reversed = start > end,
                content = content,
            )
    }
}
