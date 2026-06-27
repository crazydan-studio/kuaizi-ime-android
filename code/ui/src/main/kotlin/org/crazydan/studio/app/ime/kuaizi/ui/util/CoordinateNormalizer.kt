package org.crazydan.studio.app.ime.kuaizi.ui.util

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import org.crazydan.studio.app.ime.kuaizi.engine.input_action.OffsetF
import org.crazydan.studio.app.ime.kuaizi.engine.input_action.RectF

fun Offset.normalize(sourceSize: Size): OffsetF {
    require(sourceSize.width > 0f && sourceSize.height > 0f) {
        "Source size must be positive: $sourceSize"
    }
    return OffsetF(
        x = (x / sourceSize.width).coerceIn(0f, 1f),
        y = (y / sourceSize.height).coerceIn(0f, 1f),
    )
}

fun OffsetF.denormalize(targetSize: Size): Offset {
    return Offset(
        x = x * targetSize.width,
        y = y * targetSize.height,
    )
}

fun RectF.denormalize(targetSize: Size): Rect {
    return Rect(
        left = left * targetSize.width,
        top = top * targetSize.height,
        right = right * targetSize.width,
        bottom = bottom * targetSize.height,
    )
}

object CoordinateNormalizer {
    fun normalize(offset: Offset, sourceSize: Size): OffsetF {
        require(sourceSize.width > 0f && sourceSize.height > 0f) {
            "Source size must be positive: $sourceSize"
        }
        return OffsetF(
            x = (offset.x / sourceSize.width).coerceIn(0f, 1f),
            y = (offset.y / sourceSize.height).coerceIn(0f, 1f),
        )
    }

    fun denormalize(normalized: OffsetF, targetSize: Size): Offset {
        return Offset(
            x = normalized.x * targetSize.width,
            y = normalized.y * targetSize.height,
        )
    }

    fun denormalize(normalized: RectF, targetSize: Size): Rect {
        return Rect(
            left = normalized.left * targetSize.width,
            top = normalized.top * targetSize.height,
            right = normalized.right * targetSize.width,
            bottom = normalized.bottom * targetSize.height,
        )
    }
}
