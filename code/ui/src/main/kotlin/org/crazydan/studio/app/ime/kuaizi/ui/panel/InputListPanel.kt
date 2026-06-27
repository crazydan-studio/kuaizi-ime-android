package org.crazydan.studio.app.ime.kuaizi.ui.panel

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.crazydan.studio.app.ime.kuaizi.engine.domain.InputList
import org.crazydan.studio.app.ime.kuaizi.engine.input_action.InputActionFingerIndicator
import org.crazydan.studio.app.ime.kuaizi.ui.keyboard.InputListLayoutState
import org.crazydan.studio.app.ime.kuaizi.ui.theme.LocalKeyboardColors

@Composable
fun InputListPanel(
    inputList: InputList,
    layoutState: InputListLayoutState = InputListLayoutState(),
    onLayoutStateChanged: (InputListLayoutState) -> Unit = {},
    showIndicator: Boolean = false,
    indicatorState: InputActionFingerIndicator? = null,
    modifier: Modifier = Modifier,
) {
    val colors = LocalKeyboardColors.current

    Row(
        modifier = modifier.fillMaxWidth().height(48.dp).padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        inputList.chars.forEach { char ->
            Text(
                text = char.text,
                modifier = Modifier.padding(horizontal = 2.dp),
                color = colors.keyForeground,
                fontSize = colors.charInputTextSize,
            )
        }
    }
}
