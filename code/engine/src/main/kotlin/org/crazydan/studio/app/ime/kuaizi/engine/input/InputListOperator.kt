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

package org.crazydan.studio.app.ime.kuaizi.engine.input

/**
 * 输入列表的集中操作器，封装所有 [InputList] 变更逻辑，
 * 内部持有 [InputListEditor] 管理撤销/重做，
 * 确保每次变更操作自动维护撤销栈的一致性。
 *
 * @param editor 输入列表编辑器，管理撤销/重做
 */
class InputListOperator(
    private val editor: InputListEditor = InputListEditor(),
) {
    /** 是否可以撤销 */
    val canUndo: Boolean get() = editor.canUndo
    /** 是否可以重做 */
    val canRedo: Boolean get() = editor.canRedo

    /**
     * 追加字符到输入列表
     * @param list 当前输入列表
     * @param char 要追加的字符
     * @return 更新后的输入列表
     */
    fun appendChar(list: InputList, char: InputItem.Char): InputList {
        editor.pushUndo(list)
        return list.appendChar(char)
    }

    /**
     * 删除游标前的字符
     * @param list 当前输入列表
     * @return 更新后的输入列表
     */
    fun deleteChar(list: InputList): InputList {
        editor.pushUndo(list)
        return list.deleteCharBeforeCursor()
    }

    /**
     * 确认待输入字符（将 pending 追加到 inputs）
     * @param list 当前输入列表
     * @return 更新后的输入列表
     */
    fun commitPending(list: InputList): InputList {
        val pending = list.pending ?: return list
        editor.pushUndo(list)
        var result = list.copy(pending = null)
        for (char in pending.chars) {
            result = result.appendChar(char)
        }
        return result
    }

    /**
     * 移动游标
     * @param list 当前输入列表
     * @param newGapIndex 目标游标索引
     * @return 更新后的输入列表
     */
    fun moveCursor(list: InputList, newGapIndex: Int): InputList {
        return list.moveCursorTo(newGapIndex)
    }

    /**
     * 清空输入列表
     * @param list 当前输入列表
     * @return 更新后的输入列表
     */
    fun clean(list: InputList): InputList {
        if (list.isEmpty) return list
        editor.pushUndo(list)
        return list.clean()
    }

    /**
     * 设置待确认输入
     * @param list 当前输入列表
     * @param pending 待确认输入
     * @return 更新后的输入列表
     */
    fun withPending(list: InputList, pending: PendingInput?): InputList {
        return list.withPending(pending)
    }

    /**
     * 撤销操作
     * @param current 当前输入列表
     * @return 恢复后的输入列表
     */
    fun undo(current: InputList): InputList = editor.undo(current)

    /**
     * 重做操作
     * @param current 当前输入列表
     * @return 重做后的输入列表
     */
    fun redo(current: InputList): InputList = editor.redo(current)
}
