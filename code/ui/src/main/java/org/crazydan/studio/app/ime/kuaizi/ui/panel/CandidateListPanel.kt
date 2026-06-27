package org.crazydan.studio.app.ime.kuaizi.ui.panel

import androidx.compose.runtime.Composable
import org.crazydan.studio.app.ime.kuaizi.engine.domain.*

@Composable
fun CandidateListPanel(
    candidates: List<InputWord>,
    pageIndex: Int,
    hasMore: Boolean,
    showIndicator: Boolean = false,
    onCandidateSelected: (InputWord) -> Unit = {},
    onPageChanged: (PageDirection) -> Unit = {},
) {
    // TODO: Implement candidate list panel
    //   - Renders scrollable candidate list
    //   - Supports carousel paging
    //   - Built-in IndicatorOverlay when showIndicator is true
}
