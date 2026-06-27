package org.crazydan.studio.app.ime.kuaizi.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color

@Composable
fun LayoutBoundsOverlay(
    bounds: List<androidx.compose.ui.geometry.Rect>,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        bounds.forEach { rect ->
            drawRect(
                color = Color.Red.copy(alpha = 0.3f),
                topLeft = Offset(rect.left, rect.top),
                size = Size(rect.width, rect.height),
            )
        }
    }
}

@Composable
fun ComponentInfoOverlay(
    componentName: String,
    info: Map<String, String>,
    modifier: Modifier = Modifier,
) {
    // Handled by DebugUITestOverlay logging
}

@Composable
fun ColorPickerOverlay(
    onColorPicked: (Color) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    // Stub - color picker overlay
}

@Composable
fun GridGuidesOverlay(
    gridCols: Int = 4,
    gridRows: Int = 3,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val cellW = size.width / gridCols
        val cellH = size.height / gridRows
        for (col in 0..gridCols) {
            drawLine(Color.Gray, Offset(col * cellW, 0f), Offset(col * cellW, size.height))
        }
        for (row in 0..gridRows) {
            drawLine(Color.Gray, Offset(0f, row * cellH), Offset(size.width, row * cellH))
        }
    }
}

@Composable
fun RecompositionOverlay(
    recompositionCount: Int,
    modifier: Modifier = Modifier,
) {
    // Handled by DebugUITestOverlay logging
}
