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

package org.crazydan.studio.app.ime.kuaizi.engine.keyboard

import org.crazydan.studio.app.ime.kuaizi.engine.ImeIntent

/**
 * 键盘意图处理器接口，按不同 [KeyboardType] 创建子类，
 * 各自处理从 [ImeIntent] 到 [KeyboardStateTransition] 的映射。
 * 各子类是无状态的策略对象——它们不持有可变状态，状态由 [KeyboardStateMachine] 集中管理。
 */
interface KeyboardIntentHandler {
    /** 当前处理器所属的键盘类型 */
    val type: KeyboardType

    /**
     * 将 [ImeIntent] 映射为 [KeyboardStateTransition]
     * @param intent 输入法意图
     * @param currentState 当前键盘状态
     * @return 映射得到的状态转换
     */
    fun handleIntent(intent: ImeIntent, currentState: KeyboardState): KeyboardStateTransition
}

/**
 * 基础键盘意图处理器，所有键盘类型的默认实现。
 * 默认行为是返回 [KeyboardStateTransition.ReturnToIdle]。
 *
 * @param type 键盘类型
 */
open class BaseKeyboardIntentHandler(override val type: KeyboardType) : KeyboardIntentHandler {

    override fun handleIntent(intent: ImeIntent, currentState: KeyboardState): KeyboardStateTransition {
        return KeyboardStateTransition.ReturnToIdle
    }
}

/** 数字键盘意图处理器 */
class NumberKeyboardIntentHandler(override val type: KeyboardType) : BaseKeyboardIntentHandler(type)

/** 符号键盘意图处理器 */
class SymbolKeyboardIntentHandler(override val type: KeyboardType) : BaseKeyboardIntentHandler(type)

/** Emoji 键盘意图处理器 */
class EmojiKeyboardIntentHandler(override val type: KeyboardType) : BaseKeyboardIntentHandler(type)

/** 数学键盘意图处理器 */
class MathKeyboardIntentHandler(override val type: KeyboardType) : BaseKeyboardIntentHandler(type)

/** 编辑键盘意图处理器 */
class EditorKeyboardIntentHandler(override val type: KeyboardType) : BaseKeyboardIntentHandler(type)

/** 候选键盘意图处理器 */
class CandidateKeyboardIntentHandler(override val type: KeyboardType) : BaseKeyboardIntentHandler(type)

/** 提交选项键盘意图处理器 */
class CommitOptionKeyboardIntentHandler(override val type: KeyboardType) : BaseKeyboardIntentHandler(type)
