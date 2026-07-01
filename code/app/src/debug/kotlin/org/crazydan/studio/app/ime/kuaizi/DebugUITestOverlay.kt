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
import org.crazydan.studio.app.ime.kuaizi.engine.logging.LogLevel

/**
 * UI 测试覆盖层的 Debug 实现。
 *
 * 仅在 debug 源集中存在，release 构建不包含此文件。
 * 在激活时自动将日志等级降至 DEBUG，确保测试期间有完整日志输出。
 *
 * 通过重写 [UITestOverlay.Companion.createImpl] 替换默认空实现。
 */
class DebugUITestOverlay() : UITestOverlay {
    private val logger by lazy { ImeLog.logger("UITest") }

    /** 当前已激活的测试工具集合 */
    private val activeTools = mutableSetOf<UITestTool>()

    /**
     * 启用 UI 测试模式。
     *
     * 若当前日志等级高于 DEBUG，自动降至 DEBUG 以获取更完整的调试信息。
     */
    override fun enable() {
        if (!ImeLog.isEnabledLevel(LogLevel.DEBUG)) {
            // 降级日志等级到 DEBUG，确保测试工具产生的日志能被记录
            ImeLog.enableLevel(LogLevel.DEBUG)

            logger.info { "UI 测试工具已激活，日志等级已降至 DEBUG" }
        } else {
            logger.info { "UI 测试工具已激活" }
        }
    }

    /** 禁用 UI 测试模式并清除所有活跃工具。 */
    override fun disable() {
        activeTools.clear()
    }

    /** 切换指定工具的激活状态。工具激活时记录调试日志。 */
    override fun toggle(tool: UITestTool) {
        if (tool in activeTools) {
            activeTools.remove(tool)
        } else {
            activeTools.add(tool)

            logger.debug { "激活工具: ${tool.displayName}" }
        }
    }

    /** 检查是否有任何工具处于活跃状态。 */
    override fun isActive() = activeTools.isNotEmpty()

    /** 替换 [UITestOverlay.createImpl] 的默认实现，返回 Debug 实例。 */
    internal fun UITestOverlay.Companion.createImpl(): UITestOverlay = DebugUITestOverlay()
}
