package org.crazydan.studio.app.ime.kuaizi.ui.panel

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import org.crazydan.studio.app.ime.kuaizi.engine.domain.InputWord
import org.crazydan.studio.app.ime.kuaizi.engine.input_action.InputActionFingerIndicator
import org.crazydan.studio.app.ime.kuaizi.ui.keyboard.CandidateListLayoutState
import org.crazydan.studio.app.ime.kuaizi.ui.theme.LocalKeyboardColors

@Composable
fun CandidateListPanel(
    candidates: List<InputWord>,
    selectedIndex: Int = -1,
    layoutState: CandidateListLayoutState = CandidateListLayoutState(),
    onLayoutStateChanged: (CandidateListLayoutState) -> Unit = {},
    showIndicator: Boolean = false,
    indicatorState: InputActionFingerIndicator? = null,
    onCandidateTap: (InputWord) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val colors = LocalKeyboardColors.current
    val density = LocalDensity.current

    LazyRow(
        modifier = modifier.fillMaxWidth().height(48.dp),
    ) {
        items(candidates) { candidate ->
            Text(
                text = candidate.text,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp)
                    .clickable { onCandidateTap(candidate) },
                color = colors.candidateTextColor,
                fontSize = colors.candidateTextSize,
            )
        }
    }
}
