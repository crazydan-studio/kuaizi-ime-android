package org.crazydan.studio.app.ime.kuaizi.ui.panel

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import org.crazydan.studio.app.ime.kuaizi.engine.domain.KeyboardInputMode
import org.crazydan.studio.app.ime.kuaizi.ui.keyboard.KeyLayoutState
import org.crazydan.studio.app.ime.kuaizi.ui.viewmodel.InputGesture

@Composable
fun GestureInputPanel(
    keyLayoutState: KeyLayoutState,
    onGesture: (InputGesture) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .pointerInput(keyLayoutState) {
                detectTapGestures { offset ->
                    val key = keyLayoutState.findKeyAt(offset, size)
                    if (key != null) {
                        onGesture(InputGesture.Tap(
                            timestamp = System.currentTimeMillis(),
                            inputMode = KeyboardInputMode.RectGrid,
                            key = key,
                        ))
                    }
                }
            }
    )
}
