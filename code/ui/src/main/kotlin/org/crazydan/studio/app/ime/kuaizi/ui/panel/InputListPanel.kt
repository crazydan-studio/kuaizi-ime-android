package org.crazydan.studio.app.ime.kuaizi.ui.panel

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.crazydan.studio.app.ime.kuaizi.engine.domain.InputItem
import org.crazydan.studio.app.ime.kuaizi.engine.input_action.InputActionFingerIndicator
import org.crazydan.studio.app.ime.kuaizi.ui.keyboard.InputListLayoutState

@Composable
fun InputListPanel(
    items: List<InputItem>,
    cursorIndex: Int,
    layoutState: InputListLayoutState,
    onLayoutStateChanged: (InputListLayoutState) -> Unit = {},
    showIndicator: Boolean = false,
    indicatorState: InputActionFingerIndicator? = null,
    modifier: Modifier = Modifier,
) {
}
