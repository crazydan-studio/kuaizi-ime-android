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
import org.crazydan.studio.app.ime.kuaizi.engine.keyboard.KeyboardHandMode

/**
 * 单手模式切换组件。
 *
 * 提供左右手模式的分段选择控件，以 [FilterChip] 组的形式呈现。
 * 用于设置页面和快捷设置弹窗中切换左右手模式。
 *
 * @param currentHandMode 当前选中的手模式
 * @param onHandModeSelected 手模式选择回调
 * @param modifier 修饰符
 */
@Composable
fun HandModeToggle(
    currentHandMode: KeyboardHandMode,
    onHandModeSelected: (KeyboardHandMode) -> Unit,
    modifier: Modifier = Modifier,
) {
}
