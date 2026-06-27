package org.crazydan.studio.app.ime.kuaizi.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import org.crazydan.studio.app.ime.kuaizi.engine.ImeConfig
import org.crazydan.studio.app.ime.kuaizi.engine.domain.KeyboardThemeType

@Composable
fun KeyboardTheme(
    type: KeyboardThemeType,
    content: @Composable () -> Unit,
) {
    val isDark = when (type) {
        KeyboardThemeType.Light -> false
        KeyboardThemeType.Night -> true
        KeyboardThemeType.FollowSystem -> isSystemInDarkTheme()
    }

    val colors = if (isDark) KeyboardThemes.Night else KeyboardThemes.Light

    CompositionLocalProvider(LocalKeyboardColors provides colors) {
        content()
    }
}

@Composable
fun KeyboardTheme(
    config: ImeConfig.UiConfig,
    content: @Composable () -> Unit,
) {
    KeyboardTheme(type = config.keyboardThemeType, content = content)
}
