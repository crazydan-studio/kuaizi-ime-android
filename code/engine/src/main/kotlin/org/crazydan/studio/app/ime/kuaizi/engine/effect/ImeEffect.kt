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

package org.crazydan.studio.app.ime.kuaizi.engine.effect

import org.crazydan.studio.app.ime.kuaizi.engine.ImeIntent

/**
 * 引擎副作用通道信号。
 *
 * 承载领域事件型的弹出提示消息，由 engine 在意图处理过程中根据业务逻辑发射。
 * UI 层通过订阅 [ImeEngine.effect] 接收并展示。
 *
 * @see PopupTip.Message 纯文本消息，自动消失
 * @see PopupTip.Action 带操作按钮的消息，可点击触发 ImeIntent
 */
sealed class ImeEffect {
    sealed class PopupTip : ImeEffect() {
        /**
         * 纯文本弹出提示。
         *
         * @param message 提示文本
         * @param timeoutMs 自动消失超时（毫秒），默认 3 秒
         */
        data class Message(
            val message: String,
            val timeoutMs: Long = 3000L,
        ) : PopupTip()

        /**
         * 带操作的弹出提示。
         *
         * @param message 提示文本
         * @param actionLabel 操作按钮标签
         * @param action 点击操作按钮时触发的 ImeIntent
         * @param persistent 是否在输入时保持显示
         * @param timeoutMs 自动消失超时（毫秒），默认 5 秒
         */
        data class Action(
            val message: String,
            val actionLabel: String,
            val action: ImeIntent,
            val persistent: Boolean = false,
            val timeoutMs: Long = 5000L,
        ) : PopupTip()
    }
}
