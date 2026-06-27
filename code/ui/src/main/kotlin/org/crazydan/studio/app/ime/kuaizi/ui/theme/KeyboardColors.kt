package org.crazydan.studio.app.ime.kuaizi.ui.theme

import androidx.compose.foundation.shape.CornerSize
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class KeyboardColors(
    val background: Color,
    val keyBackground: Color,
    val keyForeground: Color,
    val keyPressedBackground: Color,
    val keyPressedForeground: Color,
    val keyPressedHighlightColor: Color,
    val functionKeyBackground: Color,
    val functionKeyForeground: Color,
    val keyCornerShape: CornerSize,
    val candidateTextColor: Color,
    val candidateSelectedTextColor: Color,
    val candidateSelectedBackground: Color,
    val candidateTextSize: TextUnit,
    val cursorColor: Color,
    val charInputTextSize: TextUnit,
    val gapColor: Color,
    val gestureTrailColor: Color,
    val fingerIndicatorColor: Color,
    val tipMessageBackground: Color,
    val tipActionBackground: Color,
    val tipTextSize: TextUnit,
    val tipActionButtonColor: Color,
    val toolBackground: Color,
    val toolSelectedBackground: Color,
    val toolDividerColor: Color,
    val toolSpacing: Dp,
    val playerIndicatorColor: Color,
)

object KeyboardThemes {
    val Light = KeyboardColors(
        background = Color(0xFFF5F5F5),
        keyBackground = Color(0xFFFFFFFF),
        keyForeground = Color(0xFF1A1A1A),
        keyPressedBackground = Color(0xFFE0E0E0),
        keyPressedForeground = Color(0xFF1A1A1A),
        keyPressedHighlightColor = Color(0x33FFFFFF),
        functionKeyBackground = Color(0xFFE8E8E8),
        functionKeyForeground = Color(0xFF333333),
        keyCornerShape = CornerSize(8.dp),
        candidateTextColor = Color(0xFF333333),
        candidateSelectedTextColor = Color(0xFFFFFFFF),
        candidateSelectedBackground = Color(0xFF2196F3),
        candidateTextSize = 16.sp,
        cursorColor = Color(0xFF2196F3),
        charInputTextSize = 18.sp,
        gapColor = Color(0xFFCCCCCC),
        gestureTrailColor = Color(0x664CAF50),
        fingerIndicatorColor = Color(0x80FF9800),
        tipMessageBackground = Color(0xFF333333),
        tipActionBackground = Color(0xFF2196F3),
        tipTextSize = 14.sp,
        tipActionButtonColor = Color(0xFFFFFFFF),
        toolBackground = Color(0xFFE8E8E8),
        toolSelectedBackground = Color(0xFF2196F3),
        toolDividerColor = Color(0xFFCCCCCC),
        toolSpacing = 4.dp,
        playerIndicatorColor = Color(0xFFFF9800),
    )

    val Night = KeyboardColors(
        background = Color(0xFF1A1A1A),
        keyBackground = Color(0xFF2D2D2D),
        keyForeground = Color(0xFFE0E0E0),
        keyPressedBackground = Color(0xFF404040),
        keyPressedForeground = Color(0xFFE0E0E0),
        keyPressedHighlightColor = Color(0x33FFFFFF),
        functionKeyBackground = Color(0xFF333333),
        functionKeyForeground = Color(0xFFCCCCCC),
        keyCornerShape = CornerSize(8.dp),
        candidateTextColor = Color(0xFFCCCCCC),
        candidateSelectedTextColor = Color(0xFFFFFFFF),
        candidateSelectedBackground = Color(0xFF1976D2),
        candidateTextSize = 16.sp,
        cursorColor = Color(0xFF64B5F6),
        charInputTextSize = 18.sp,
        gapColor = Color(0xFF555555),
        gestureTrailColor = Color(0x6681C784),
        fingerIndicatorColor = Color(0x80FFB74D),
        tipMessageBackground = Color(0xFF555555),
        tipActionBackground = Color(0xFF1976D2),
        tipTextSize = 14.sp,
        tipActionButtonColor = Color(0xFFFFFFFF),
        toolBackground = Color(0xFF333333),
        toolSelectedBackground = Color(0xFF1976D2),
        toolDividerColor = Color(0xFF555555),
        toolSpacing = 4.dp,
        playerIndicatorColor = Color(0xFFFFB74D),
    )
}

val LocalKeyboardColors = compositionLocalOf { KeyboardThemes.Light }
