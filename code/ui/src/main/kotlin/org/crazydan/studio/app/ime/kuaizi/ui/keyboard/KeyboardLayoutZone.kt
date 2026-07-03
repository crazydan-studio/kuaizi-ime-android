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

package org.crazydan.studio.app.ime.kuaizi.ui.keyboard

/**
 * 屏幕布局分区。
 *
 * 定义 IME 在屏幕上的空间划分。
 * Zone A 占据屏幕上半区，Zone B 占据屏幕下半区。
 * 在 [KeyboardLayoutMode.Stacked] 模式下仅使用 Zone B；
 * 在 [KeyboardLayoutMode.Separated] 模式下 Zone A 和 Zone B 均被使用。
 */
sealed class KeyboardLayoutZone {

    /** 屏幕上半区，Separated 模式下展示按键布局和手势反馈 */
    data object A : KeyboardLayoutZone()

    /** 屏幕下半区，所有交互的核心区域，包含三行结构 */
    data object B : KeyboardLayoutZone()
}
