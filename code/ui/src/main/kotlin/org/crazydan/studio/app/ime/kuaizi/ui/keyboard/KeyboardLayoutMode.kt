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

/** 键盘布局模式，定义 Zone A 与 Zone B 的使用方式 */
sealed class KeyboardLayoutMode {

    /** 堆叠模式：所有组件集中在 Zone B，三层面板叠加共享同一空间 */
    data object Stacked : KeyboardLayoutMode()

    /**
     * 分离模式：输入区域占据 Zone B，按键展示区域占据 Zone A
     * @param zoneARatio Zone A 占屏幕高度的比例，默认 0.4
     */
    data class Separated(val zoneARatio: Float = 0.4f) : KeyboardLayoutMode()
}
