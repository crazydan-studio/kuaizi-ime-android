package org.crazydan.studio.app.ime.kuaizi.ui.panel

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.crazydan.studio.app.ime.kuaizi.engine.InputWord
import org.crazydan.studio.app.ime.kuaizi.engine.input_action.InputActionFingerIndicator
import org.crazydan.studio.app.ime.kuaizi.ui.keyboard.CandidateListLayoutState

@Composable
fun CandidateListPanel(
    candidates: List<InputWord>,
    selectedIndex: Int,
    layoutState: CandidateListLayoutState,
    onLayoutStateChanged: (CandidateListLayoutState) -> Unit = {},
    showIndicator: Boolean = false,
    indicatorState: InputActionFingerIndicator? = null,
    onCandidateTap: (Int) -> Unit = {},
    modifier: Modifier = Modifier,
) {
}
