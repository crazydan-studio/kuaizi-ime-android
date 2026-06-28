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

package org.crazydan.studio.app.ime.kuaizi.ui.config

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.crazydan.studio.app.ime.kuaizi.engine.domain.KeyboardThemeType

/**
 * 主题选择器组件。
 *
 * 提供横向滚动的主题卡片选择界面，支持浅色、深色和跟随系统三种主题模式。
 * 每个主题卡片包含一个缩略键盘预览和主题名称标签。
 *
 * @param currentTheme 当前选中的主题类型
 * @param onThemeSelected 主题选择回调
 * @param modifier 修饰符
 */
@Composable
fun ThemeSelector(
    currentTheme: KeyboardThemeType,
    onThemeSelected: (KeyboardThemeType) -> Unit,
    modifier: Modifier = Modifier,
) {
}
