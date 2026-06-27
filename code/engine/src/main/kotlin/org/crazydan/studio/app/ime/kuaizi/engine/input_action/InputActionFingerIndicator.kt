package org.crazydan.studio.app.ime.kuaizi.engine.input_action

data class InputActionFingerIndicator(
    val position: OffsetF = OffsetF(),
    val pressed: Boolean = false,
    val visible: Boolean = true,
    val clickAnimation: ClickAnimation? = null,
)

data class ClickAnimation(
    val progress: Float = 0f,
    val maxRadius: Float = 20f,
    val color: Long = 0xFFFFFFFF,
)

data class OffsetF(
    val x: Float = 0f,
    val y: Float = 0f,
)

data class RectF(
    val left: Float = 0f,
    val top: Float = 0f,
    val right: Float = 0f,
    val bottom: Float = 0f,
) {
    val center: OffsetF get() = OffsetF((left + right) / 2, (top + bottom) / 2)
}
