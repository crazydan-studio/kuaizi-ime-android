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

import org.crazydan.studio.app.ime.kuaizi.UITestOverlay.Companion.createImpl


/**
 * UI 测试工具枚举。
 *
 * 定义所有可用的 UI 测试辅助工具，每个工具有显示名称和说明。
 * 这些工具仅在 debug 构建中启用，release 构建中不包含相关代码。
 */
enum class UITestTool(
    /** 工具显示名称 */
    val displayName: String,
    /** 工具功能描述 */
    val description: String,
) {
    LAYOUT_BOUNDS("布局边界", "显示组件布局边界框"),
    COMPONENT_INFO("组件信息", "显示选中组件的详细信息"),
    COLOR_PICKER("颜色拾取", "取色器用于校验颜色值"),
    GRID_GUIDES("网格辅助", "显示布局网格参考线"),
    RECOMPOSITION("重组追踪", "追踪 Composable 重组次数"),
}

/**
 * UI 测试覆盖层接口。
 *
 * 定义 UI 测试工具集的操作契约：启用、禁用、切换工具、检查活跃状态。
 * main 源集中提供 [NoopUITestOverlay] 空实现，debug 源集中由 [DebugUITestOverlay] 覆盖。
 *
 * 这种设计确保 release 构建不包含任何 UI 测试代码。
 */
interface UITestOverlay {
    /** 启用 UI 测试模式（自动降级日志等级）。 */
    fun enable()

    /** 禁用 UI 测试模式并清除所有活跃工具状态。 */
    fun disable()

    /** 切换指定工具的激活状态。 */
    fun toggle(tool: UITestTool)

    /** 检查是否有任何工具处于活跃状态。 */
    fun isActive(): Boolean

    companion object {
        /**
         * 创建 UITestOverlay 实例。
         *
         * 默认返回 [NoopUITestOverlay]。
         * Debug 源集中的 [DebugUITestOverlay] 通过重写 [createImpl] 替换此实现。
         */
        fun create(): UITestOverlay = createImpl()

        /** 内部工厂方法，由 debug 源集通过重写替换实现。 */
        internal fun createImpl(): UITestOverlay = NoopUITestOverlay()
    }
}

/**
 * UITestOverlay 的空实现。
 *
 * 所有方法均为空操作，[isActive] 始终返回 false。
 * Release 构建中使用的默认实现，确保 UI 测试代码完全被移除。
 */
class NoopUITestOverlay : UITestOverlay {
    override fun enable() {}
    override fun disable() {}
    override fun toggle(tool: UITestTool) {}
    override fun isActive() = false
}
