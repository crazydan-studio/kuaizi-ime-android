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

package org.crazydan.studio.app.ime.kuaizi.ui.panel

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp
import org.crazydan.studio.app.ime.kuaizi.engine.keyboard.InputKey
import org.crazydan.studio.app.ime.kuaizi.engine.keyboard.KeyboardInputMode
import org.crazydan.studio.app.ime.kuaizi.ui.keyboard.KeyLayoutState
import org.crazydan.studio.app.ime.kuaizi.ui.theme.LocalKeyboardColors

/**
 * 按键布局面板。
 *
 * 负责按键布局的渲染和状态展示，仅负责展示，不处理任何触摸事件。
 * 根据 [keyTable] 二维矩阵和 [keyLayoutState] 的位置信息渲染按键。
 * 布局测量完成后通过 [onLayoutStateChanged] 回调上报布局状态。
 *
 * @param keyTable 按键布局矩阵
 * @param keyLayoutState 按键布局状态（含归一化位置映射）
 * @param keyboardInputMode 键盘输入模式（影响布局策略）
 * @param onLayoutStateChanged 布局状态变更回调
 * @param modifier 修饰符
 */
@Composable
fun KeyLayoutPanel(
    keyTable: List<List<InputKey>> = emptyList(),
    keyLayoutState: KeyLayoutState = KeyLayoutState(),
    keyboardInputMode: KeyboardInputMode = KeyboardInputMode.RectGrid,
    onLayoutStateChanged: (KeyLayoutState) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val colors = LocalKeyboardColors.current

    // 布局状态变化时通过回调通知 ViewModel 缓存
    LaunchedEffect(keyLayoutState) {
        onLayoutStateChanged(keyLayoutState)
    }

    Canvas(modifier = modifier.fillMaxWidth().height(200.dp)) {
        // 根据 keyPositions 渲染按键矩形
        keyLayoutState.keyPositions.forEach { (key, rectF) ->
            val pixelRect = keyLayoutState.denormalize(rectF, size)
            drawRoundRect(
                color = colors.keyBackground,
                topLeft = Offset(pixelRect.left, pixelRect.top),
                size = Size(pixelRect.width, pixelRect.height),
                cornerRadius = CornerRadius.Zero,
            )
        }
    }
}
