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

package org.crazydan.studio.app.ime.kuaizi.engine

/**
 * [EditorAction] 的类型别名，提供更符合 IME 上下文语义的命名。
 */
typealias ImeEditorAction = EditorAction

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
     * @property direction 光标移动方向
     */
    data class MoveCursor(
        override val timestamp: Long,
        val direction: CursorDirection,
    ) : EditorAction()

    /**
     * 选择范围：从光标位置向指定方向选取文本范围。
     *
     * @property timestamp 操作时间戳
     * @property direction 选择方向
     */
    data class SelectRange(
        override val timestamp: Long,
        val direction: CursorDirection,
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
 * 编辑器编辑动作枚举。
 *
 * 定义引擎可对目标编辑器执行的系统级编辑操作。
 */
enum class EditorEditAction {
    BACKSPACE, SELECT_ALL, COPY, CUT, PASTE, UNDO, REDO,
}

/** 光标移动方向枚举。 */
enum class CursorDirection { Left, Right, Up, Down, Home, End }

/**
 * 文本范围：表示编辑器中的文本选区。
 *
 * @property start 起始位置（包含）
 * @property end 结束位置（不包含）
 */
data class TextRange(val start: Int, val end: Int)
