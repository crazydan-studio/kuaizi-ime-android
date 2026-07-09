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

package org.crazydan.studio.app.ime.kuaizi.engine.keyboard.intent

import org.crazydan.studio.app.ime.kuaizi.engine.ImeIntent
import org.crazydan.studio.app.ime.kuaizi.engine.keyboard.InputKey
import org.crazydan.studio.app.ime.kuaizi.engine.keyboard.KeyboardIntentHandler
import org.crazydan.studio.app.ime.kuaizi.engine.keyboard.KeyboardState
import org.crazydan.studio.app.ime.kuaizi.engine.keyboard.KeyboardStateTransition

/**
 * 基础键盘意图处理器，所有键盘类型的默认实现。
 */
open class BaseKeyboardIntentHandler() : KeyboardIntentHandler {

    override fun handleIntent(intent: ImeIntent, currentState: KeyboardState): KeyboardStateTransition {
        return KeyboardStateTransition.ReturnToIdle
    }

    /** 处理与 [InputKey.Ctrl] 相关的 [ImeIntent] */
    protected fun handleCtrlKeyIntent(
        intent: ImeIntent.OnKey, state: KeyboardState,
        //
        key: InputKey.Ctrl = intent.key as InputKey.Ctrl,
    ): KeyboardStateTransition? =
        when (key) {
            is InputKey.Ctrl.Backspace ->
                when (intent) {
                    is ImeIntent.OnKey.LongPress.Hold ->
                        // 直接转由单击意图处理
                        handleCtrlKeyIntent(intent = ImeIntent.OnKey.Tap(key), state = state)

                    is ImeIntent.OnKey.Tap ->
                        KeyboardStateTransition.BackspaceChar

                    else -> null
                }

            else -> null
        }
}
