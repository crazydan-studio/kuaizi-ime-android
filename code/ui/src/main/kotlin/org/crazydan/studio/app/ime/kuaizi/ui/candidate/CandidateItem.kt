package org.crazydan.studio.app.ime.kuaizi.ui.candidate

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.crazydan.studio.app.ime.kuaizi.ui.theme.LocalKeyboardColors

@Composable
fun CandidateItem(
    text: String,
    isSelected: Boolean = false,
    onClick: () -> Unit = {},
) {
    val colors = LocalKeyboardColors.current
    val bg = if (isSelected) colors.candidateSelectedBackground else Color.Transparent
    val fg = if (isSelected) colors.candidateSelectedTextColor else colors.candidateTextColor

    Text(
        text = text,
        modifier = Modifier
            .padding(horizontal = 8.dp, vertical = 12.dp)
            .background(bg, shape = RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 4.dp),
        color = fg,
        fontSize = colors.candidateTextSize,
    )
}
