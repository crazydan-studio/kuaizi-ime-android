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
 * along with this program.  See the
 * GNU Lesser General Public License for more details.
 *
 * If not, see <https://www.gnu.org/licenses/lgpl-3.0.en.html#license-text>.
 */

package org.crazydan.studio.app.ime.kuaizi.ui.panel

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import org.crazydan.studio.app.ime.kuaizi.ui.keyboard.KeyLayoutState
import org.crazydan.studio.app.ime.kuaizi.ui.theme.LocalKeyboardColors
import org.crazydan.studio.app.ime.kuaizi.ui.viewmodel.GestureFeedbackState

/**
 * 手势反馈面板。
 *
 * 以透明覆盖层形式叠加在按键区域之上，读取 [GestureFeedbackState] 中的归一化坐标，
 * 反归一化为像素坐标后绘制触摸轨迹、按键高亮和手指指示器。
 *
 * @param feedbackState 手势反馈状态（归一化坐标）
 * @param keyLayoutState 按键布局状态，用于按键位置映射
 * @param modifier 修饰符
 */
@Composable
fun GestureFeedbackPanel(
    feedbackState: GestureFeedbackState,
    keyLayoutState: KeyLayoutState = KeyLayoutState(),
    modifier: Modifier = Modifier,
) {
    val trailPoints by feedbackState.touchTrailPoints.collectAsState()
    val pressedKeys by feedbackState.pressedKeys.collectAsState()
    val colors = LocalKeyboardColors.current

    Canvas(modifier = modifier.fillMaxWidth().height(200.dp)) {
        // 绘制触摸轨迹：将归一化坐标反归一化后连接成路径
        if (trailPoints.size >= 2) {
            val path = Path()
            trailPoints.forEachIndexed { i, point ->
                val px = point.x * size.width
                val py = point.y * size.height
                if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
            }
            drawPath(path, color = colors.gestureTrailColor, style = Stroke(width = 4f))
        }
    }
}
