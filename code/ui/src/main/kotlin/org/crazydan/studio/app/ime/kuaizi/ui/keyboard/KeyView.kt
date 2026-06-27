package org.crazydan.studio.app.ime.kuaizi.ui.keyboard

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.crazydan.studio.app.ime.kuaizi.engine.domain.InputKey
import org.crazydan.studio.app.ime.kuaizi.ui.theme.LocalKeyboardColors

@Composable
fun KeyView(
    key: InputKey,
    isPressed: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val colors = LocalKeyboardColors.current
    val bg = if (isPressed) colors.keyPressedBackground else colors.keyBackground
    val fg = if (isPressed) colors.keyPressedForeground else colors.keyForeground

    Text(
        text = key.toString(),
        color = fg,
        modifier = modifier.size(48.dp),
    )
}
