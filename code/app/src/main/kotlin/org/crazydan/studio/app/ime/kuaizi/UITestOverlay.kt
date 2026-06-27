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
