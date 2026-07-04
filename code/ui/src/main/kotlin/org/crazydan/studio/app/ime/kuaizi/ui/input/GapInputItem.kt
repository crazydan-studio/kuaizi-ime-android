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

package org.crazydan.studio.app.ime.kuaizi.ui.input

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.crazydan.studio.app.ime.kuaizi.ui.theme.LocalKeyboardColors

/**
 * 输入栏中的光标间隙组件。
 *
 * 渲染为一条窄竖线，代表光标在当前输入串中的位置。
 * 使用 [KeyboardColors.cursorColor] 作为颜色。
 * 点击间隙可移动光标位置。
 *
 * @param onGapClicked 间隙点击回调
 */
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
