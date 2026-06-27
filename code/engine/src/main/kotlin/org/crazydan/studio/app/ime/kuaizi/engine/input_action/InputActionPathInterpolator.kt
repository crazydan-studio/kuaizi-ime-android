package org.crazydan.studio.app.ime.kuaizi.engine.input_action

import kotlin.math.cos
import kotlin.math.sin

class InputActionPathInterpolator {
    fun interpolate(
        from: OffsetF,
        to: OffsetF,
        arcFactor: Float = 0f,
        steps: Int = 10,
    ): List<OffsetF> {
        val points = mutableListOf<OffsetF>()

        for (i in 0..steps) {
            val t = i.toFloat() / steps
            val x = from.x + (to.x - from.x) * t
            val y = from.y + (to.y - from.y) * t + arcFactor * sin(t * Math.PI.toFloat())

            points.add(OffsetF(x, y))
        }

        return points
    }
}
