package org.crazydan.studio.app.ime.kuaizi.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import org.crazydan.studio.app.ime.kuaizi.ui.theme.LocalKeyboardColors

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
    val context = androidx.compose.ui.platform.LocalContext.current
    val infoText = info.entries.joinToString("\n") { "${it.key}: ${it.value}" }
    LaunchedEffect(componentName) {
        android.util.Log.d("UITest", "Component: $componentName\n$infoText")
    }
}

@Composable
fun ColorPickerOverlay(
    onColorPicked: (Color) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    // Stub for debug color picker
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
    val context = androidx.compose.ui.platform.LocalContext.current
    LaunchedEffect(recompositionCount) {
        if (recompositionCount > 0 && recompositionCount % 10 == 0) {
            android.util.Log.d("UITest", "Recompositions: $recompositionCount")
        }
    }
}
