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
 * If not, see <https://www.gnu.org/licenses/gpl-3.0.en.html#license-text>.
 */

package org.crazydan.studio.app.ime.kuaizi.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import org.crazydan.studio.app.ime.kuaizi.ui.theme.LocalKeyboardColors

/**
 * 布局边界覆盖层。
 *
 * 在指定组件周围绘制半透明红色矩形边界，用于可视化布局范围。
 * 利用 [Canvas] 直接绘制，不修改业务 Composable 的布局。
 */
@Composable
fun LayoutBoundsOverlay(
    /** 需要绘制的边界矩形列表 */
    bounds: List<androidx.compose.ui.geometry.Rect>,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        bounds.forEach { rect ->
            drawRect(
                color = Color.Red.copy(alpha = 0.3f),
                topLeft = Offset(rect.left, rect.top),
                size = Size(rect.width, rect.height),
            )
        }
    }
}

/**
 * 组件信息覆盖层。
 *
 * 记录选中组件的调试信息（名称、位置、尺寸等），
 * 通过 Android Logcat 输出（标签：UITest）。
 */
@Composable
fun ComponentInfoOverlay(
    /** 组件名称 */
    componentName: String,
    /** 组件属性信息键值对 */
    info: Map<String, String>,
    modifier: Modifier = Modifier,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val infoText = info.entries.joinToString("\n") { "${it.key}: ${it.value}" }
    LaunchedEffect(componentName) {
        android.util.Log.d("UITest", "Component: $componentName\n$infoText")
    }
}

/**
 * 颜色拾取覆盖层（存根）。
 *
 * 预留颜色拾取功能接口，当前为存根实现。
 * 完整的实现将允许用户点击键盘界面上任意像素以获取颜色值。
 */
@Composable
fun ColorPickerOverlay(
    onColorPicked: (Color) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    // Stub for debug color picker
}

/**
 * 网格辅助覆盖层。
 *
 * 在键盘界面上绘制水平和垂直参考线，用于验证布局对齐精度。
 * 网格线为灰色半透明线条，不干扰正常操作。
 *
 * @param gridCols 水平网格列数
 * @param gridRows 垂直网格行数
 */
@Composable
fun GridGuidesOverlay(
    gridCols: Int = 4,
    gridRows: Int = 3,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val cellW = size.width / gridCols
        val cellH = size.height / gridRows
        // 绘制垂直线
        for (col in 0..gridCols) {
            drawLine(Color.Gray, Offset(col * cellW, 0f), Offset(col * cellW, size.height))
        }
        // 绘制水平线
        for (row in 0..gridRows) {
            drawLine(Color.Gray, Offset(0f, row * cellH), Offset(size.width, row * cellH))
        }
    }
}

/**
 * 重组追踪覆盖层。
 *
 * 监控 Composable 的重组次数，每 10 次通过 Logcat 输出一次警告。
 * 用于识别频繁重组的组件，帮助优化性能。
 */
@Composable
fun RecompositionOverlay(
    /** 当前重组计数 */
    recompositionCount: Int,
    modifier: Modifier = Modifier,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    LaunchedEffect(recompositionCount) {
        if (recompositionCount > 0 && recompositionCount % 10 == 0) {
            android.util.Log.d("UITest", "Recompositions: $recompositionCount")
        }
    }
}
