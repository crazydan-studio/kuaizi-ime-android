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
 * 引擎副作用信号 sealed class，表达一次性效果。
 *
 * 与 [ImeState] 的持续性字段不同，
 * [ImeEffect] 通过 [SharedFlow]<[ImeEffect]> 发射——
 * 引擎发射信号后 UI 层在独立的 collectEffect 协程中消费，不触发 [ImeState] 的变化。
 * 这种分离避免了一次性效果触发全局 UI 重组。
 *
 * @see PopupTip
 * @see PlayAudio
 * @see PlayHaptic
 */
sealed class ImeEffect {

    /** 音效反馈信号：指示 UI 层播放指定类型的音效。 */
    data class PlayAudio(val type: AudioType) : ImeEffect()

    /** 触觉反馈信号：指示 UI 层触发指定类型的振动。 */
    data class PlayHaptic(val type: HapticType) : ImeEffect()

    /**
     * 弹出提示信号：展示短暂的提示信息或可交互的操作提示。
     *
     * @see Message 纯信息性提示，短暂停留后自动消失
     * @see Action 可交互的操作提示，附带可点击按钮
     */
    sealed class PopupTip : ImeEffect() {

        /**
         * 消息提醒：短暂停留的提示信息。
         *
         * 不提供交互操作，超时后自动消失。
         * 典型场景：键盘类型切换提示、编辑器操作反馈。
         *
         * @property message 提示文本内容
         * @property timeout 超时时间（毫秒），默认 3000ms
         */
        data class Message(
            val message: String,
            val timeout: Long = 3000L,
        ) : PopupTip()

        /**
         * 操作提示：可点击触发动作的提示。
         *
         * 附带一个可点击按钮，点击后触发一个 [ImeIntent]。
         * 典型场景：可粘贴内容提示、可收藏内容提示。
         *
         * @property message 提示文本内容
         * @property actionLabel 按钮标签文本
         * @property action 点击按钮后发送的 [ImeIntent]
         * @property persistent 是否持续显示直到用户输入
         * @property timeout 超时时间（毫秒），默认 5000ms
         */
        data class Action(
            val message: String,
            val actionLabel: String,
            val action: ImeIntent,
            val persistent: Boolean = false,
            val timeout: Long = 5000L,
        ) : PopupTip()
    }
}
