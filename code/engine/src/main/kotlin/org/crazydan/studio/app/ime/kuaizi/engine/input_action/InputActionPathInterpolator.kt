package org.crazydan.studio.app.ime.kuaizi.engine.input_action

import kotlin.math.sqrt

object InputActionPathInterpolator {

    fun interpolate(from: OffsetF, to: OffsetF, t: Float): OffsetF {
        val control = computeControlPoint(from, to)
        val oneMinusT = 1.0f - t
        val x = oneMinusT * oneMinusT * from.x + 2.0f * oneMinusT * t * control.x + t * t * to.x
        val y = oneMinusT * oneMinusT * from.y + 2.0f * oneMinusT * t * control.y + t * t * to.y
        return OffsetF(x, y)
    }

    private fun computeControlPoint(from: OffsetF, to: OffsetF): OffsetF {
        val midX = (from.x + to.x) / 2.0f
        val midY = (from.y + to.y) / 2.0f
        val dx = to.x - from.x
        val dy = to.y - from.y
        val distance = sqrt((dx * dx + dy * dy).toDouble()).toFloat()
        if (distance < 0.001f) return OffsetF(midX, midY)
        val curvature = distance * CURVATURE_FACTOR
        val nx = -dy / distance
        val ny = dx / distance
        return OffsetF(midX + nx * curvature, midY + ny * curvature)
    }

    private const val CURVATURE_FACTOR = 0.15f
}
