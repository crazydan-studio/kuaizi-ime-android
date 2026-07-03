/*
 * 筷字输入法 - 高效编辑需要又好又快的输入法
 * Copyright (C) 2026 Crazydan Studio <https://studio.crazydan.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.
 * If not, see <https://www.gnu.org/licenses/lgpl-3.0.en.html#license-text>.
 */

package org.crazydan.studio.app.ime.kuaizi.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import org.crazydan.studio.app.ime.kuaizi.engine.ImeConfig
import org.crazydan.studio.app.ime.kuaizi.engine.keyboard.KeyboardThemeType

/**
 * 键盘主题提供者。
 *
 * 根据 [type] 选择 [KeyboardColors] 并通过 [CompositionLocalProvider]
 * 注入到 [LocalKeyboardColors] 中，所有子组件通过 `LocalKeyboardColors.current`
 * 读取配色方案。主题切换时所有子组件自动重组。
 *
 * @param type 主题类型（浅色/深色/跟随系统）
 * @param content 子组件内容
 */
@Composable
fun KeyboardTheme(
    type: KeyboardThemeType,
    content: @Composable () -> Unit,
) {
    // 根据主题类型判断是否为深色模式
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

/**
 * 通过 [ImeConfig.Ui] 提供键盘主题的便捷重载。
 *
 * @param config UI 配置，从中读取 [keyboardThemeType]
 * @param content 子组件内容
 */
@Composable
fun KeyboardTheme(
    config: ImeConfig.Ui,
    content: @Composable () -> Unit,
) {
    KeyboardTheme(type = config.keyboardThemeType, content = content)
}
