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

class DebugUITestOverlay(
    private val log: ImeLog = ImeLog,
) : UITestOverlay {
    private val activeTools = mutableSetOf<UITestTool>()

    override fun enable() {
        if (log.level > LogLevel.DEBUG) {
            log.updateLevel(LogLevel.DEBUG)
            log.logger("UITest").info { "UI 测试工具已激活，日志等级已降至 DEBUG" }
        }
    }

    override fun disable() {
        activeTools.clear()
    }

    override fun toggle(tool: UITestTool) {
        if (tool in activeTools) {
            activeTools.remove(tool)
        } else {
            activeTools.add(tool)
            log.logger("UITest").debug { "激活工具: ${tool.displayName}" }
        }
    }

    override fun isActive() = activeTools.isNotEmpty()

    internal fun UITestOverlay.Companion.createImpl(): UITestOverlay = DebugUITestOverlay()
}
