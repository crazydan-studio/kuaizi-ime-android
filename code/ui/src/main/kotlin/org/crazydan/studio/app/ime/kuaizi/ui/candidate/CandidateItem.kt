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

/**
 * 单个候选项组件。
 *
 * 渲染一个可点击的候选项，支持选中态高亮。
 * 选中项使用 [KeyboardColors.candidateSelectedBackground] 和 [candidateSelectedTextColor]，
 * 未选中项使用 [candidateTextColor]。
 *
 * @param text 候选项文本
 * @param isSelected 是否被选中
 * @param onClick 点击回调
 */
@Composable
fun CandidateItem(
    text: String,
    isSelected: Boolean = false,
    onClick: () -> Unit = {},
) {
    val colors = LocalKeyboardColors.current
    // 根据选中状态切换背景色和前景色
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
