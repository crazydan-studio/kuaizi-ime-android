package org.crazydan.studio.app.ime.kuaizi.engine.input_action

data class InputActionFingerIndicator(
    val position: OffsetF = OffsetF.Zero,
    val pressed: Boolean = false,
    val visible: Boolean = false,
    val clickAnimation: ClickAnimation = ClickAnimation.None,
) {
    enum class ClickAnimation {
        None,
        Pressing,
        Releasing,
    }
}

data class OffsetF(
    val x: Float = 0f,
    val y: Float = 0f,
) {
    companion object {
        val Zero = OffsetF(0.0f, 0.0f)
    }
}

data class RectF(
    val left: Float = 0f,
    val top: Float = 0f,
    val right: Float = 0f,
    val bottom: Float = 0f,
) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top
    val centerX: Float get() = (left + right) / 2.0f
    val centerY: Float get() = (top + bottom) / 2.0f
    val center: OffsetF get() = OffsetF(centerX, centerY)

    fun contains(offset: OffsetF): Boolean =
        offset.x in left..right && offset.y in top..bottom

    companion object {
        val Zero = RectF(0.0f, 0.0f, 0.0f, 0.0f)
    }
}
