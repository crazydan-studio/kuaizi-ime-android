package org.crazydan.studio.app.ime.kuaizi.ui.panel

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp
import org.crazydan.studio.app.ime.kuaizi.engine.domain.InputKey
import org.crazydan.studio.app.ime.kuaizi.engine.domain.KeyboardInputMode
import org.crazydan.studio.app.ime.kuaizi.ui.keyboard.KeyLayoutState
import org.crazydan.studio.app.ime.kuaizi.ui.theme.LocalKeyboardColors

@Composable
fun KeyLayoutPanel(
    keyTable: List<List<InputKey>> = emptyList(),
    keyLayoutState: KeyLayoutState = KeyLayoutState(),
    keyboardInputMode: KeyboardInputMode = KeyboardInputMode.RectGrid,
    onLayoutStateChanged: (KeyLayoutState) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val colors = LocalKeyboardColors.current

    LaunchedEffect(keyLayoutState) {
        onLayoutStateChanged(keyLayoutState)
    }

    Canvas(modifier = modifier.fillMaxWidth().height(200.dp)) {
        keyLayoutState.keyPositions.forEach { (key, rectF) ->
            val pixelRect = keyLayoutState.denormalize(rectF, size)
            drawRoundRect(
                color = colors.keyBackground,
                topLeft = Offset(pixelRect.left, pixelRect.top),
                size = Size(pixelRect.width, pixelRect.height),
                cornerRadius = CornerRadius(colors.keyCornerShape),
            )
        }
    }
}
