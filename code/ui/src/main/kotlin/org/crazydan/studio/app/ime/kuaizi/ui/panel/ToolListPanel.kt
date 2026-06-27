package org.crazydan.studio.app.ime.kuaizi.ui.panel

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.crazydan.studio.app.ime.kuaizi.engine.ImeIntent
import org.crazydan.studio.app.ime.kuaizi.engine.ToolListState
import org.crazydan.studio.app.ime.kuaizi.engine.input_action.InputActionFingerIndicator
import org.crazydan.studio.app.ime.kuaizi.ui.keyboard.KeyLayoutState
import org.crazydan.studio.app.ime.kuaizi.ui.theme.LocalKeyboardColors

@Composable
fun ToolListPanel(
    toolList: ToolListState,
    layoutState: KeyLayoutState = KeyLayoutState(),
    onLayoutStateChanged: (KeyLayoutState) -> Unit = {},
    showIndicator: Boolean = false,
    indicatorState: InputActionFingerIndicator? = null,
    onToolClick: (ImeIntent) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val colors = LocalKeyboardColors.current

    Row(
        modifier = modifier.fillMaxWidth().height(48.dp).padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        toolList.tools.forEach { tool ->
            if (tool.intent != null) {
                TextButton(
                    onClick = { onToolClick(tool.intent) },
                    enabled = !tool.disabled,
                ) {
                    Text(text = tool.label, color = colors.functionKeyForeground)
                }
            }
        }
    }
}
