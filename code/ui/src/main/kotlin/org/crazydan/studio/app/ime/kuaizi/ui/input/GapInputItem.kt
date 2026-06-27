package org.crazydan.studio.app.ime.kuaizi.ui.input

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.crazydan.studio.app.ime.kuaizi.ui.theme.LocalKeyboardColors

@Composable
fun GapInputItem(onGapClicked: () -> Unit = {}) {
    val colors = LocalKeyboardColors.current
    Box(
        modifier = Modifier
            .width(2.dp)
            .height(24.dp)
            .background(colors.cursorColor)
            .clickable(onClick = onGapClicked),
    )
}
