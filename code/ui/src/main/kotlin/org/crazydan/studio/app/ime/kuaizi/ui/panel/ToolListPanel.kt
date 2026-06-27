package org.crazydan.studio.app.ime.kuaizi.ui.panel

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.crazydan.studio.app.ime.kuaizi.engine.ImeIntent
import org.crazydan.studio.app.ime.kuaizi.engine.ToolListState
import org.crazydan.studio.app.ime.kuaizi.engine.input_action.InputActionFingerIndicator
import org.crazydan.studio.app.ime.kuaizi.ui.keyboard.KeyLayoutState

@Composable
fun ToolListPanel(
    toolListState: ToolListState,
    layoutState: KeyLayoutState,
    onLayoutStateChanged: (KeyLayoutState) -> Unit = {},
    showIndicator: Boolean = false,
    indicatorState: InputActionFingerIndicator? = null,
    onToolClick: (ImeIntent) -> Unit = {},
    modifier: Modifier = Modifier,
) {
}
