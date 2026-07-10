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

import org.crazydan.studio.app.ime.kuaizi.engine.domain.EditorCursorMotion
import org.crazydan.studio.app.ime.kuaizi.engine.domain.EditorEditAction

/**
 * 引擎的编辑器操作 sealed class。
 *
 * 由引擎内部的 [ImeEngine.dispatchEditorAction] 统一分发到 [ImeEditorBridge]，
 * 桥梁实现者无需理解 [EditorAction] 类型体系。
 */
sealed class EditorAction {

    /**
     * 提交文本到编辑器。
     *
     * @property text 要提交的文本内容
     * @property replacements 替换列表，支持直输模式下的字符轮换
     */
    data class CommitText(
        val text: String,
        val replacements: List<String>? = null,
    ) : EditorAction()

    /** 撤回提交：撤销上一次 [CommitText] 操作。 */
    data object RevokeCommit : EditorAction()

    /**
     * 插入配对符号：在光标位置插入左右配对符号对。
     *
     * @property left 左符号
     * @property right 右符号
     */
    data class InsertPairedSymbols(
        val left: String,
        val right: String,
    ) : EditorAction()

    /**
     * 移动光标：向指定方向移动编辑器光标。
     *
     * @property motion 光标移动信息
     */
    data class MoveCursor(
        val motion: EditorCursorMotion,
    ) : EditorAction()

    /**
     * 选取选区：从光标位置向指定方向选取内容。
     *
     * @property motion 光标移动信息
     */
    data class SelectSelection(
        val motion: EditorCursorMotion,
    ) : EditorAction()

    /**
     * 执行编辑操作：对编辑器执行系统级编辑动作。
     *
     * @property action 要执行的编辑动作
     */
    data class PerformEdit(
        val action: EditorEditAction,
    ) : EditorAction()
}
