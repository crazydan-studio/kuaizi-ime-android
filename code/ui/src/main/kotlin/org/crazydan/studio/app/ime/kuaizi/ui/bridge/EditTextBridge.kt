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

package org.crazydan.studio.app.ime.kuaizi.ui.bridge

import android.view.KeyEvent
import android.widget.EditText
import org.crazydan.studio.app.ime.kuaizi.engine.bridge.BaseImeEditorBridge
import org.crazydan.studio.app.ime.kuaizi.engine.domain.EditorCursorMotion
import org.crazydan.studio.app.ime.kuaizi.engine.domain.EditorEditAction
import org.crazydan.studio.app.ime.kuaizi.engine.domain.EditorSelection
import org.crazydan.studio.app.ime.kuaizi.engine.domain.Motion
import kotlin.math.max

/**
 * 基于 [EditText] 的编辑器桥接器。
 *
 * 用于非系统 IME 场景（如应用内嵌入输入法），将引擎的编辑操作
 * 直接应用到 Android [EditText] 控件上。
 * 支持普通提交、替换提交、配对符号插入、光标移动、选区操作、撤销等完整编辑功能。
 *
 * @param targetSupplier 返回目标 EditText 实例的提供者
 */
class EditTextBridge(
    private val targetSupplier: () -> EditText?,
) : BaseImeEditorBridge() {

    /** 获取编辑器当前全文。 */
    override fun getText(): CharSequence =
        targetSupplier()?.text ?: ""

    /** 获取编辑器当前选区。 */
    override fun getSelection(): EditorSelection =
        getCurrentSelection(targetSupplier())

    // -------------------------------------------------------

    /** 撤销提交：恢复到操作前的内容 */
    override fun doRevokeCommit(revertion: RevertionSnapshot) {
        val et = targetSupplier() ?: return

        // Note: 撤销（undo）是由编辑器控制的，其可能会撤销间隔时间较短的多个输入，
        // 故而，只能以 记录输入前的范围再还原 的方式实现输入的撤回
        val (before, after) = revertion

        // 将 从编辑前的开始位置 到 编辑后的终点位置 之间的内容恢复为编辑前的内容
        replaceText(et, before.content, before.start, after.end)
        // 还原编辑前的选区
        et.setSelection(before.start, before.end)
    }

    /** 普通提交文本：在光标位置插入文本 */
    override fun doNormalCommitText(text: CharSequence, oneByOne: Boolean) {
        val et = targetSupplier() ?: return

        val selection = getCurrentSelection(et, false)
        val (start, end) = selection

        // ------------------
        replaceText(et, text, start, end)

        // 移动到替换后的文本内容之后
        val offset = text.length
        et.setSelection(start + offset)
    }

    /** 可替换提交文本：检查光标前文本是否匹配替换列表，匹配则替换而非插入。 */
    override fun doReplaceableCommitText(text: CharSequence, replacements: List<String>): Boolean {
        val et = targetSupplier() ?: return true
        val editable = et.text ?: return true

        val selection = getCurrentSelection(et, false)
        val (start) = selection

        // Note：假设替换字符的长度均相同
        val replacementStartIndex = max(0, start - text.length)
        val raw = editable.subSequence(replacementStartIndex, start)

        if (replacements.contains(raw.toString())) {
            replaceText(et, text, replacementStartIndex, start)
            return true
        }

        return false
    }

    /** 插入配对符号（如括号、引号），并将光标置于左右符号之间。 */
    override fun insertPairedSymbols(left: CharSequence, right: CharSequence) {
        val et = targetSupplier() ?: return

        val selection = getCurrentSelection(et, false)
        val (start, end) = selection

        // Note：先向选区尾部添加符号，以避免选区发生移动
        replaceText(et, right, end, end)
        replaceText(et, left, start, start)

        // 重新选中初始文本
        val offset = left.length
        et.setSelection(start + offset, end + offset)
    }

    // -------------------------------------------------------

    /** 移动光标到指定方向 */
    override fun moveCursor(motion: EditorCursorMotion) {
        val et = targetSupplier() ?: return

        doMoveCursor(et, motion)
    }

    /** 向指定方向扩展选区 */
    override fun selectSelection(motion: EditorCursorMotion) {
        val et = targetSupplier() ?: return

        // Note: 通过 shift + 方向键 的方式进行文本选择
        sendKey(et, KeyEvent.KEYCODE_SHIFT_LEFT) {
            doMoveCursor(et, motion)
        }
    }

    /** 执行编辑操作 */
    override fun performEdit(action: EditorEditAction) {
        val et = targetSupplier() ?: return

        when (action) {
            EditorEditAction.SelectAll -> et.onTextContextMenuItem(android.R.id.selectAll)
            //
            EditorEditAction.Copy -> et.onTextContextMenuItem(android.R.id.copy)
            EditorEditAction.Paste -> et.onTextContextMenuItem(android.R.id.paste)
            EditorEditAction.Cut -> et.onTextContextMenuItem(android.R.id.cut)
            //
            EditorEditAction.Undo -> et.onTextContextMenuItem(android.R.id.undo)
            EditorEditAction.Redo -> et.onTextContextMenuItem(android.R.id.redo)
            //
            EditorEditAction.Backspace ->
                // Note: 发送按键事件的兼容性更好，可由组件处理删除操作
                sendKey(et, KeyEvent.KEYCODE_DEL)
        }
    }

    /** 按方向移动光标，通过模拟键盘按键实现。 */
    private fun doMoveCursor(et: EditText, motion: EditorCursorMotion) {
        val (direction, distance) = motion
        if (distance <= 0) return

        // Note: 发送按键事件方式可支持上下移动光标，以便于快速定位到目标位置
        for (i in 0..<distance.toInt()) {
            when (direction) {
                Motion.Direction.Left -> sendKey(et, KeyEvent.KEYCODE_DPAD_LEFT)
                Motion.Direction.Right -> sendKey(et, KeyEvent.KEYCODE_DPAD_RIGHT)
                Motion.Direction.Up -> sendKey(et, KeyEvent.KEYCODE_DPAD_UP)
                Motion.Direction.Down -> sendKey(et, KeyEvent.KEYCODE_DPAD_DOWN)
            }
        }
    }

    // --------------------------------------------

    /** 发起从按键按下到弹起的一次完整的按键事件 */
    private fun sendKey(et: EditText, code: Int, block: (() -> Unit)? = null) {
        et.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, code))

        block?.invoke()

        et.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_UP, code))
    }

    // --------------------------------------------

    /** 替换指定范围内（posStart ~ posEnd）的文本  */
    private fun replaceText(et: EditText, text: CharSequence?, posStart: Int, posEnd: Int) =
        et.text?.replace(posStart, posEnd, text ?: "")

    // --------------------------------------------

    private fun getCurrentSelection(et: EditText?, withContent: Boolean = true): EditorSelection =
        et?.let {
            val start = it.selectionStart
            val end = it.selectionEnd

            EditorSelection.create(
                start = start,
                end = end,
                content =
                    if (withContent) it.text?.substring(start, end) ?: ""
                    else "",
            )
        } ?: EditorSelection.empty()
}
