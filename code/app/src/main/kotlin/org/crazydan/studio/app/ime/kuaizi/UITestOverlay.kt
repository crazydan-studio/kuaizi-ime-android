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

package org.crazydan.studio.app.ime.kuaizi

import org.crazydan.studio.app.ime.kuaizi.engine.logging.ImeLog
import org.crazydan.studio.app.ime.kuaizi.engine.LogLevel

enum class UITestTool(
    val displayName: String,
    val description: String,
) {
    LAYOUT_BOUNDS("布局边界", "显示组件布局边界框"),
    COMPONENT_INFO("组件信息", "显示选中组件的详细信息"),
    COLOR_PICKER("颜色拾取", "取色器用于校验颜色值"),
    GRID_GUIDES("网格辅助", "显示布局网格参考线"),
    RECOMPOSITION("重组追踪", "追踪 Composable 重组次数"),
}

interface UITestOverlay {
    fun enable()
    fun disable()
    fun toggle(tool: UITestTool)
    fun isActive(): Boolean

    companion object {
        fun create(): UITestOverlay = createImpl()

        internal fun createImpl(): UITestOverlay = NoopUITestOverlay()
    }
}

class NoopUITestOverlay : UITestOverlay {
    override fun enable() {}
    override fun disable() {}
    override fun toggle(tool: UITestTool) {}
    override fun isActive() = false
}
