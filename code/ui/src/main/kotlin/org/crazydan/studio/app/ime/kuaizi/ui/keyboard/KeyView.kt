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

package org.crazydan.studio.app.ime.kuaizi.ui.keyboard

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.crazydan.studio.app.ime.kuaizi.engine.domain.InputKey
import org.crazydan.studio.app.ime.kuaizi.ui.theme.LocalKeyboardColors

/**
 * 单个按键的渲染组件。
 *
 * 根据 [InputKey] 的语义类型分发渲染，仅负责展示，不处理触摸事件。
 * 按压状态通过 [isPressed] 参数控制色彩变化。
 *
 * @param key 按键的语义标识
 * @param isPressed 是否处于按下状态
 * @param modifier 修饰符
 */
@Composable
fun KeyView(
    key: InputKey,
    isPressed: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val colors = LocalKeyboardColors.current
    // 根据按下状态切换背景色和前景色
    val bg = if (isPressed) colors.keyPressedBackground else colors.keyBackground
    val fg = if (isPressed) colors.keyPressedForeground else colors.keyForeground

    Text(
        text = key.toString(),
        color = fg,
        modifier = modifier.size(48.dp),
    )
}
