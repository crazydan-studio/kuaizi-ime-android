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

package org.crazydan.studio.app.ime.kuaizi.ui.viewmodel

import org.crazydan.studio.app.ime.kuaizi.engine.ImeIntent

/**
 * 弹出提示状态，由 KeyboardViewModel 管理。
 *
 * 引擎通过 [ImeEffect.PopupTip] 发出一次性效果信号，
 * ViewModel 订阅后更新此状态，驱动 [PopupTipPanel] 显示。
 *
 * 两种类型：
 * - [Message]：纯文本信息提示，超时后自动消失
 * - [Action]：可点击提示，含操作按钮，点击后触发 [ImeIntent]
 */
sealed class PopupTipState {
    /**
     * 纯文本信息提示
     * @param message 提示消息内容
     * @param timeoutMs 自动消失超时时间（毫秒），默认 3000ms
     */
    data class Message(
        val message: String,
        val timeoutMs: Long = 3000L,
    ) : PopupTipState()

    /**
     * 可点击操作提示
     * @param message 提示消息内容
     * @param actionLabel 操作按钮标签
     * @param action 点击后触发的 ImeIntent
     * @param persistent 是否持久显示（直到用户开始输入）
     * @param timeoutMs 自动消失超时时间（毫秒），默认 5000ms
     */
    data class Action(
        val message: String,
        val actionLabel: String,
        val action: ImeIntent,
        val persistent: Boolean = false,
        val timeoutMs: Long = 5000L,
    ) : PopupTipState()
}
