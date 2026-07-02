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

package org.crazydan.studio.app.ime.kuaizi

import android.view.KeyEvent
import android.view.inputmethod.ExtractedText
import android.view.inputmethod.ExtractedTextRequest
import android.view.inputmethod.InputConnection
import org.crazydan.studio.app.ime.kuaizi.engine.bridge.BaseImeEditorBridge
import org.crazydan.studio.app.ime.kuaizi.engine.bridge.CursorDirection
import org.crazydan.studio.app.ime.kuaizi.engine.bridge.EditorCursorMotion
import org.crazydan.studio.app.ime.kuaizi.engine.bridge.EditorEditAction
import org.crazydan.studio.app.ime.kuaizi.engine.bridge.EditorSelection

/**
 * 基于 Android [InputConnection] 的编辑器桥接实现。
 *
 * 使用 supplier 模式获取当前 [InputConnection] 实例，
 * 因为 InputConnection 由系统管理，编辑器切换时会变更。
 * 每次操作都通过 targetSupplier 获取最新的实例，确保操作目标正确。
 *
 * 继承 [BaseImeEditorBridge] 以获得单快照撤销能力。
 */
class InputConnectionBridge(
    /** 获取当前 [InputConnection] 实例的供给函数 */
    private val targetSupplier: () -> InputConnection?,
    /** 发送按键字符的操作函数。在单字符输入和逐字输入时需要调用 */
    private val sendKeyChar: (charCode: Char) -> Unit,
) : BaseImeEditorBridge() {

    /** 获取编辑器当前全文。 */
    override fun getText(): CharSequence =
        getExtractedText(targetSupplier())?.text ?: ""

    /** 获取编辑器当前选区。 */
    override fun getSelection(): EditorSelection =
        getCurrentSelection(targetSupplier())

    // -----------------------------------------------------------------

    /** 撤销提交：将编辑器文本恢复到快照记录的状态。 */
    override fun doRevokeCommit(revertion: RevertionSnapshot) {
        val ic = targetSupplier() ?: return

        val (before, after) = revertion
        // Note: 撤销（undo）是由编辑器控制的，其可能会撤销间隔时间较短的多个输入，
        // 故而，只能以 记录输入前的范围再还原 的方式实现输入的撤回
        batchEdit(ic) {
            // 将 从编辑前的开始位置 到 编辑后的终点位置 之间的内容恢复为编辑前的内容
            replaceText(ic, before.content, before.start, after.end)

            // 还原编辑前的选区
            ic.setSelection(before.start, before.end)
        }
    }

    /** 正常提交文本：记录撤销快照后调用系统 commitText。 */
    override fun doNormalCommitText(text: CharSequence, oneByOne: Boolean) {
        val ic = targetSupplier() ?: return

        if (oneByOne
            || text.length == 1 // 单个字符需以事件形式发送，才能被所有组件所识别
        ) {
            text.forEach { ch ->
                sendKeyChar(ch)
            }
        } else {
            addText(ic, text)
        }
    }

    /** 可替换提交文本：检查光标前文本是否匹配替换列表，匹配则替换而非插入。 */
    override fun doReplaceableCommitText(text: CharSequence, replacements: List<String>): Boolean {
        val ic = targetSupplier() ?: return true

        // Note: 假设替换字符的长度均相同
        val raw = ic.getTextBeforeCursor(text.length, 0)

        if (raw != null && replacements.contains(raw.toString())) {
            replaceTextBeforeCursor(ic, text, raw.length)
            return true
        }

        return false
    }

    /** 插入配对符号（如括号、引号），并将光标置于左右符号之间。 */
    override fun insertPairedSymbols(left: CharSequence, right: CharSequence) {
        val ic = targetSupplier() ?: return

        val selection = getCurrentSelection(ic, false)
        val (start, end) = selection

        batchEdit(ic) {
            // Note: 先向选区尾部添加符号，以避免选区发生移动
            addText(ic, right, end)
            addText(ic, left, start)

            // 重新选中初始文本
            val offset = left.length
            ic.setSelection(start + offset, end + offset)
        }
    }

    // -----------------------------------------------------------------

    /** 按方向移动光标，通过模拟键盘按键实现。 */
    override fun moveCursor(motion: EditorCursorMotion) {
        val ic = targetSupplier() ?: return

        doMoveCursor(ic, motion)
    }

    /** 按方向扩展选区，模拟 Shift + 方向键。 */
    override fun selectRange(motion: EditorCursorMotion) {
        val ic = targetSupplier() ?: return

        // Note: 通过 shift + 方向键 的方式进行文本选择
        sendKey(ic, KeyEvent.KEYCODE_SHIFT_LEFT) {
            doMoveCursor(ic, motion)
        }
    }

    /** 执行编辑器编辑操作，通过系统上下文菜单动作实现。 */
    override fun performEdit(action: EditorEditAction) {
        val ic = targetSupplier() ?: return

        when (action) {
            EditorEditAction.SELECT_ALL -> ic.performContextMenuAction(android.R.id.selectAll)
            //
            EditorEditAction.COPY -> ic.performContextMenuAction(android.R.id.copy)
            EditorEditAction.PASTE -> ic.performContextMenuAction(android.R.id.paste)
            EditorEditAction.CUT -> ic.performContextMenuAction(android.R.id.cut)
            //
            EditorEditAction.UNDO -> ic.performContextMenuAction(android.R.id.undo)
            EditorEditAction.REDO -> ic.performContextMenuAction(android.R.id.redo)
            //
            EditorEditAction.BACKSPACE ->
                // Note: 发送按键事件的兼容性更好，可由组件处理删除操作
                sendKey(ic, KeyEvent.KEYCODE_DEL)
        }
    }

    /** 按方向移动光标，通过模拟键盘按键实现。 */
    private fun doMoveCursor(ic: InputConnection, motion: EditorCursorMotion) {
        val (direction, distance) = motion
        if (distance <= 0) return

        // Note: 发送按键事件方式可支持上下移动光标，以便于快速定位到目标位置
        for (i in 0..<distance.toInt()) {
            val extracted = getExtractedText(ic)
            // 已失去焦点，停止移动
            if (extracted == null) {
                break
            }

            val head = 0
            val tail = extracted.text.length
            // 光标移动不能超出当前的编辑区域
            // Note: 通过方向键做文本选择不会超出编辑区域，故而，仅需检查光标移动，确保其不超过文本的首尾位置
            val needToStop = when (direction) {
                CursorDirection.Up, CursorDirection.Left ->
                    extracted.selectionStart == head
                            && extracted.selectionEnd == head

                CursorDirection.Down, CursorDirection.Right ->
                    extracted.selectionStart == tail
                            && extracted.selectionEnd == tail
            }
            if (needToStop) {
                break
            }

            when (direction) {
                CursorDirection.Left -> sendKey(ic, KeyEvent.KEYCODE_DPAD_LEFT)
                CursorDirection.Right -> sendKey(ic, KeyEvent.KEYCODE_DPAD_RIGHT)
                CursorDirection.Up -> sendKey(ic, KeyEvent.KEYCODE_DPAD_UP)
                CursorDirection.Down -> sendKey(ic, KeyEvent.KEYCODE_DPAD_DOWN)
            }
        }
    }

    // ---------------------------------------

    /** 仅当包含多个编辑动作时，才需要启用编辑批处理 */
    private fun batchEdit(ic: InputConnection, block: () -> Unit) {
        ic.beginBatchEdit()

        block()

        ic.endBatchEdit()
    }

    /** 发起从按键按下到弹起的一次完整的按键事件 */
    private fun sendKey(ic: InputConnection, code: Int, block: (() -> Unit)? = null) {
        ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, code))

        block?.invoke()

        ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, code))
    }

    // --------------------------------------------

    /** 在光标位置添加文本  */
    private fun addText(ic: InputConnection, text: CharSequence?) =
    // Note: 第二个参数必须为 1，
    // 若设置为 0，则浏览器页面的输入框的光标位置不会移动到插入文本之后，
        // 而若设置为文本长度，则某些 app 会将光标移动两倍文本长度
        ic.commitText(text ?: "", 1)

    /** 向指定位置添加文本  */
    private fun addText(ic: InputConnection, text: CharSequence?, pos: Int) =
        replaceText(ic, text, pos, pos)

    /** 替换光标之前指定长度的文本  */
    private fun replaceTextBeforeCursor(ic: InputConnection, text: CharSequence?, length: Int) {
        val selection = getCurrentSelection(ic, false)
        val (start) = selection

        replaceText(ic, text, start - length, start)
    }

    /** 替换指定范围内（posStart ~ posEnd）的文本  */
    private fun replaceText(ic: InputConnection, text: CharSequence?, posStart: Int, posEnd: Int) {
        // 移动光标到指定位置
        ic.setSelection(posStart, posEnd)

        addText(ic, text)
    }

    // --------------------------------------------

    private fun getCurrentSelection(ic: InputConnection?, withContent: Boolean = true): EditorSelection =
        getExtractedText(ic)?.let {
            EditorSelection.create(
                start = it.selectionStart,
                end = it.selectionEnd,
                content =
                    if (withContent) ic?.getSelectedText(InputConnection.GET_TEXT_WITH_STYLES) ?: ""
                    else "",
            )
        } ?: EditorSelection.empty()

    /**
     * Note: [ExtractedText.text] 为当前编辑器的全部内容，而不是已选中内容
     *
     * https://stackoverflow.com/questions/40521324/selection-using-android-ime#answer-58778722
     */
    private fun getExtractedText(ic: InputConnection?): ExtractedText? =
        ic?.getExtractedText(ExtractedTextRequest(), 0)
}
