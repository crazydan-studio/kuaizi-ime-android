package org.crazydan.studio.app.ime.kuaizi.ui.keyboard

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import org.crazydan.studio.app.ime.kuaizi.engine.domain.InputKey
import org.crazydan.studio.app.ime.kuaizi.engine.input_action.OffsetF
import org.crazydan.studio.app.ime.kuaizi.engine.input_action.RectF

data class KeyLayoutState(
    val keyPositions: Map<InputKey, RectF> = emptyMap(),
    val spatialGrid: SpatialGrid = SpatialGrid(),
    val panelSize: Size = Size.Zero,
) {
    class SpatialGrid(
        val gridCols: Int = 4,
        val gridRows: Int = 3,
    ) {
        private val grid: Array<Array<MutableList<InputKey>>> =
            Array(gridRows) { Array(gridCols) { mutableListOf() } }

        fun buildIndex(keyPositions: Map<InputKey, RectF>) {
            grid.forEach { row -> row.forEach { it.clear() } }
            keyPositions.forEach { (key, rect) ->
                val colStart = (rect.center.x * gridCols).toInt().coerceIn(0, gridCols - 1)
                val rowStart = (rect.center.y * gridRows).toInt().coerceIn(0, gridRows - 1)
                grid[rowStart][colStart].add(key)
            }
        }

        fun findCandidateKeys(normalizedPos: OffsetF): List<InputKey> {
            val col = (normalizedPos.x * gridCols).toInt().coerceIn(0, gridCols - 1)
            val row = (normalizedPos.y * gridRows).toInt().coerceIn(0, gridRows - 1)
            return grid[row][col]
        }
    }

    fun denormalize(normalized: RectF, targetSize: Size): Rect {
        return Rect(
            left = normalized.left * targetSize.width,
            top = normalized.top * targetSize.height,
            right = normalized.right * targetSize.width,
            bottom = normalized.bottom * targetSize.height,
        )
    }

    fun findKeyAt(position: Offset, targetSize: Size): InputKey? {
        val normalized = OffsetF(
            x = (position.x / targetSize.width).coerceIn(0f, 1f),
            y = (position.y / targetSize.height).coerceIn(0f, 1f),
        )
        val candidates = spatialGrid.findCandidateKeys(normalized)
        return candidates.firstOrNull { key ->
            val rect = keyPositions[key] ?: return@firstOrNull false
            denormalize(rect, targetSize).contains(position)
        }
    }
}

data class CandidateListLayoutState(
    val candidatePositions: List<Rect>,
    val panelSize: Size = Size.Zero,
) {
    fun locateItem(index: Int): Offset? {
        return candidatePositions.getOrNull(index)?.center
    }
}

data class InputListLayoutState(
    val itemPositions: List<Rect>,
    val panelSize: Size = Size.Zero,
) {
    fun locateItem(index: Int): Offset? {
        return itemPositions.getOrNull(index)?.center
    }
}
