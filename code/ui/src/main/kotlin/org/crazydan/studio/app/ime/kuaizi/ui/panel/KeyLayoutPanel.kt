package org.crazydan.studio.app.ime.kuaizi.ui.panel

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.crazydan.studio.app.ime.kuaizi.engine.domain.InputKey
import org.crazydan.studio.app.ime.kuaizi.engine.domain.KeyboardInputMode
import org.crazydan.studio.app.ime.kuaizi.ui.keyboard.KeyLayoutState
import org.crazydan.studio.app.ime.kuaizi.ui.keyboard.KeyTableContext
import org.crazydan.studio.app.ime.kuaizi.ui.keyboard.KeyTableGenerator

@Composable
fun KeyLayoutPanel(
    keyTable: List<List<InputKey>>,
    generator: KeyTableGenerator,
    context: KeyTableContext,
    keyboardInputMode: KeyboardInputMode,
    keyLayoutState: KeyLayoutState,
    onLayoutStateChanged: (KeyLayoutState) -> Unit = {},
    modifier: Modifier = Modifier,
) {
}
