package org.crazydan.studio.app.ime.kuaizi.ui.input

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.crazydan.studio.app.ime.kuaizi.ui.theme.LocalKeyboardColors

@Composable
fun CharInputItem(char: String) {
    val colors = LocalKeyboardColors.current
    Text(
        text = char,
        modifier = Modifier.padding(horizontal = 2.dp),
        color = colors.keyForeground,
        fontSize = colors.charInputTextSize,
    )
}
