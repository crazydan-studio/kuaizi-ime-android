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
import org.crazydan.studio.app.ime.kuaizi.engine.domain.EditorSelection

/**
 * 编辑器桥接接口：引擎与目标编辑器之间的桥梁。
 *
 * 采用桥接模式将输出目标与引擎解耦，引擎内部仅执行一次 [EditorAction] 的穷举分发，
 * 桥梁实现者只需实现本接口的语义方法，无需理解 [EditorAction] 类型体系。
 * 接口方法表达「做什么」（语义）而非「怎么做」（实现），不同平台和目标编辑器可以
 * 按自身能力提供差异化的实现。
 *
 * 所有方法在主线程调用以保证线程安全。
 *
 * @see BaseImeEditorBridge 提供单快照撤销机制的抽象基类
 * @see EditorAction 引擎向编辑器输出的操作类型
 */
interface ImeEditorBridge {

    /** 实时获取目标编辑器当前文本内容。 */
    fun getText(): CharSequence

    /** 实时获取目标编辑器当前选区。 */
    fun getSelection(): EditorSelection

    // -------------------------------------------------

    /**
     * 提交文本到当前光标位置。
     *
     * @param text 要提交的文本内容
     * @param replacements 可选的替换轮换列表。
     *   - 非空时，桥梁需检查光标前文本是否在列表中：
     *     - 匹配：替换光标前字符（替换轮换）
     *     - 不匹配：正常插入文本
     *   - 替换轮换用于直输模式下的标点符号双击轮换，如 `.` → `。` → `…`
     * @param oneByOne 是否逐字输入。提交收藏的验证码等逐个填充的数据时，需要逐字输入
     * @param revertable 提交内容是否可撤销
     */
    fun commitText(
        text: CharSequence, replacements: List<String>? = null,
        oneByOne: Boolean, revertable: Boolean,
    )

    /** 撤销最近一次可撤回的输入，恢复编辑器文本到提交前的状态。 */
    fun revokeCommit()

    /**
     * 插入成对符号。
     * 若编辑器中存在选中文本，将选中文本包裹在左右符号之间；
     * 若无选中文本，插入左右符号并将光标置于两者之间。
     */
    fun insertPairedSymbols(left: CharSequence, right: CharSequence)

    // -------------------------------------------------

    /** 按指定方向移动光标。 */
    fun moveCursor(motion: EditorCursorMotion)

    /** 按指定方向扩展选区。 */
    fun selectSelection(motion: EditorCursorMotion)

    /** 执行编辑器编辑操作（全选、复制、剪切、粘贴、撤销、重做等）。 */
    fun performEdit(action: EditorEditAction)
}
