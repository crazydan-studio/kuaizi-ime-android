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

/**
 * 输入列表的撤销/重做管理器。
 * 采用 ArrayDeque 双端队列实现两个有界栈：undoStack 和 redoStack，
 * 利用 InputList 的不可变性保存历史状态引用。
 * 栈最大容量为 50，超出上限时淘汰最旧条目。
 */
class InputListEditor {
    private val undoStack = ArrayDeque<InputList>(50)
    private val redoStack = ArrayDeque<InputList>(50)
    private val maxStackSize = 50

    /** 是否可以撤销 */
    val canUndo: Boolean get() = undoStack.isNotEmpty()
    /** 是否可以重做 */
    val canRedo: Boolean get() = redoStack.isNotEmpty()

    /**
     * 保存快照到撤销栈，超出上限时淘汰最旧条目。
     * 新操作会清空重做栈，使重做历史失效。
     * @param list 要保存的输入列表快照
     */
    fun pushUndo(list: InputList) {
        if (undoStack.size >= maxStackSize) {
            undoStack.removeFirst()
        }
        undoStack.addLast(list)
        redoStack.clear()
    }

    /**
     * 保存快照到重做栈，超出上限时淘汰最旧条目
     * @param list 要保存的输入列表快照
     */
    private fun pushRedo(list: InputList) {
        if (redoStack.size >= maxStackSize) {
            redoStack.removeFirst()
        }
        redoStack.addLast(list)
    }

    /**
     * 撤销：恢复到上一个状态
     * @param current 当前输入列表
     * @return 恢复后的输入列表，若无可撤销则返回当前状态
     */
    fun undo(current: InputList): InputList {
        val previous = undoStack.removeLastOrNull() ?: return current
        pushRedo(current)
        return previous
    }

    /**
     * 重做：前进到下一个状态
     * @param current 当前输入列表
     * @return 重做后的输入列表，若无可重做则返回当前状态
     */
    fun redo(current: InputList): InputList {
        val next = redoStack.removeLastOrNull() ?: return current
        pushUndo(current)
        return next
    }
}
