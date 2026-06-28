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

package org.crazydan.studio.app.ime.kuaizi.engine.input_action

import org.crazydan.studio.app.ime.kuaizi.engine.domain.InputKey

/**
 * 位置解析接口：将逻辑标识解析为归一化坐标 [OffsetF]。
 *
 * 接口的抽象设计将坐标解析与动作逻辑彻底分离——编译器产出只包含逻辑标识的动作脚本，
 * 回放器通过注入的 [InputActionPositionResolver] 实现在运行时解析坐标。
 * 这种分离使得同一份动作脚本可以在不同面板布局、不同按键尺寸的环境下正确回放，
 * 只需替换解析器实现即可适配新的布局参数。
 */
interface InputActionPositionResolver {
    /**
     * 将按键解析为归一化坐标。
     * 返回按键中心点在面板归一化坐标系中的位置，若按键不存在则返回 null。
     */
    fun resolve(key: InputKey): OffsetF?

    /**
     * 将候选索引解析为归一化坐标。
     * 用于 [InputAction.SelectCandidate] 动作的手指指示器定位。
     */
    fun resolveCandidatePosition(index: Int): OffsetF?

    /**
     * 将输入项索引解析为归一化坐标。
     * 用于辅助回放场景中输入项的高亮定位。
     */
    fun resolveInputItemPosition(index: Int): OffsetF?
}
