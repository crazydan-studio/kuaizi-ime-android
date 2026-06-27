package org.crazydan.studio.app.ime.kuaizi.ui.theme

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

data class KeyboardColors(
    val background: Color = Color(0xFFF5F5F5),
    val keyBackground: Color = Color(0xFFE8E8E8),
    val keyForeground: Color = Color(0xFF333333),
    val keyPressedBackground: Color = Color(0xFFD0D0D0),
    val keyPressedForeground: Color = Color(0xFF000000),
    val keyPressedHighlightColor: Color = Color(0x33FFFFFF),
    val functionKeyBackground: Color = Color(0xFFD5D5D5),
    val functionKeyForeground: Color = Color(0xFF666666),
    val keyCornerShape: Float = 4f,
    val candidateTextColor: Color = Color(0xFF333333),
    val candidateSelectedTextColor: Color = Color(0xFF1976D2),
    val candidateSelectedBackground: Color = Color(0xFFE3F2FD),
    val candidateTextSize: Float = 16f,
    val cursorColor: Color = Color(0xFF1976D2),
    val charInputTextSize: Float = 18f,
    val gapColor: Color = Color(0xFFCCCCCC),
    val gestureTrailColor: Color = Color(0x401976D2),
    val fingerIndicatorColor: Color = Color(0x801976D2),
    val tipMessageBackground: Color = Color(0xFF333333),
    val tipActionBackground: Color = Color(0xFF1976D2),
    val tipTextSize: Float = 14f,
    val tipActionButtonColor: Color = Color(0xFFFFFFFF),
    val toolBackground: Color = Color(0xFFEEEEEE),
    val toolSelectedBackground: Color = Color(0xFFD0D0D0),
    val toolDividerColor: Color = Color(0xFFCCCCCC),
    val toolSpacing: Float = 0f,
    val playerIndicatorColor: Color = Color(0xFFE53935),
)

object KeyboardThemes {
    val Light = KeyboardColors()

    val Night = KeyboardColors(
        background = Color(0xFF1E1E1E),
        keyBackground = Color(0xFF333333),
        keyForeground = Color(0xFFE8E8E8),
        keyPressedBackground = Color(0xFF555555),
        keyPressedForeground = Color(0xFFFFFFFF),
        functionKeyBackground = Color(0xFF444444),
        functionKeyForeground = Color(0xFFAAAAAA),
        candidateTextColor = Color(0xFFE8E8E8),
        candidateSelectedTextColor = Color(0xFF64B5F6),
        candidateSelectedBackground = Color(0xFF1A3A5C),
        cursorColor = Color(0xFF64B5F6),
        gapColor = Color(0xFF666666),
        gestureTrailColor = Color(0x4064B5F6),
        fingerIndicatorColor = Color(0x8064B5F6),
        tipMessageBackground = Color(0xFFCCCCCC),
        tipActionBackground = Color(0xFF64B5F6),
        toolBackground = Color(0xFF2A2A2A),
        toolSelectedBackground = Color(0xFF444444),
        toolDividerColor = Color(0xFF444444),
        playerIndicatorColor = Color(0xFFEF5350),
    )
}

val LocalKeyboardColors = compositionLocalOf { KeyboardThemes.Light }
