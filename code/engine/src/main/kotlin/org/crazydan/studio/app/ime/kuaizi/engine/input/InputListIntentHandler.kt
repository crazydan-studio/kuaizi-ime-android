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

import org.crazydan.studio.app.ime.kuaizi.engine.ImeIntent
import org.crazydan.studio.app.ime.kuaizi.engine.dict.ImeDictProvider

// 采用无状态的全局静态对象的接口以避免频繁创建对象示例

/** 仅处理直接变更 [InputList] 的意图 */
object InputListIntentHandler {

    /** 对 [ImeIntent.InputList] 的处理结果 */
    data class Result(
        val inputList: InputList,
        val next: List<ImeIntent>? = null,
    )

    suspend fun handle(
        intent: ImeIntent.InputList,
        inputList: InputList,
        dict: ImeDictProvider,
    ) {
        when (intent) {
            is ImeIntent.InputList.AddItem -> {
                // TODO 根据 InputList 当前状态决定字符添加和替换
                // TODO 对拉丁文输入做数据库补全查询
                // TODO 若为拼音输入且拼音有效，则查询候选字
            }

            is ImeIntent.InputList.DeleteBackward -> {
                // TODO 回删 InputList 中字符
            }

            is ImeIntent.InputList.RemoveSelected -> {
            }

            is ImeIntent.InputList.ConfirmPending -> {
                // TODO InputList 确认待输入
                // TODO 若为拼音输入，则更新拼音输入短语
            }

            is ImeIntent.InputList.DropPending -> {
                // TODO InputList 丢弃待输入
            }

            is ImeIntent.InputList.SelectAt -> TODO()
            is ImeIntent.InputList.UpdateTextOption -> TODO()

            is ImeIntent.InputList.Commit -> {
                // TODO 更新数据库
                // TODO 主键盘切换到 Idle 状态，临时性键盘切换回主键盘
            }

            is ImeIntent.InputList.Revoke -> {
                // TODO 恢复 InputList
                // TODO 还原数据库记录
                // TODO 选中 InputList 中的已选中项
            }
        }
    }
}
