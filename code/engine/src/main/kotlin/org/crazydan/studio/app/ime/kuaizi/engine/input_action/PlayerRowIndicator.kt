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

/**
 * 双行手指指示器：支持上下两行键盘区域的手指位置指示。
 *
 * 用于双行键盘布局的回放场景，上下行各有一个独立的手指指示器，
 * 可以分别追踪不同行的手指移动轨迹。
 *
 * @property row1 第一行（上行）手指指示器
 * @property row2 第二行（下行）手指指示器
 */
data class PlayerRowIndicator(
    val row1: InputActionFingerIndicator? = null,
    val row2: InputActionFingerIndicator? = null,
)
