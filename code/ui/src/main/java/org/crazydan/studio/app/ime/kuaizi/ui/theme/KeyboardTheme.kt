package org.crazydan.studio.app.ime.kuaizi.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import org.crazydan.studio.app.ime.kuaizi.engine.domain.KeyboardThemeType
import org.crazydan.studio.app.ime.kuaizi.engine.ImeConfig

@Composable
fun KeyboardTheme(
    config: ImeConfig.UiConfig = ImeConfig.UiConfig(),
    content: @Composable () -> Unit,
) {
    val isDark = when (config.keyboardThemeType) {
        KeyboardThemeType.Light -> false
        KeyboardThemeType.Night -> true
        KeyboardThemeType.FollowSystem -> isSystemInDarkTheme()
    }

    val colors = if (isDark) KeyboardThemes.Night else KeyboardThemes.Light

    CompositionLocalProvider(LocalKeyboardColors provides colors) {
        content()
    }
}
