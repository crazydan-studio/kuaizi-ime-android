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
