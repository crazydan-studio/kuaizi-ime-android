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

import androidx.compose.foundation.shape.CornerSize
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 键盘配色方案，定义键盘界面各区域的颜色变量。
 *
 * 包含按键、候选栏、输入栏、弹出提示、工具栏等全部视觉元素的色彩配置。
 * 通过 [CompositionLocal] 机制向下传递，切换主题时所有组件自动重组。
 */
data class KeyboardColors(
    /** 键盘整体背景色 */
    val background: Color,
    /** 普通按键背景色 */
    val keyBackground: Color,
    /** 普通按键前景色（文本/图标） */
    val keyForeground: Color,
    /** 按键按下态背景色 */
    val keyPressedBackground: Color,
    /** 按键按下态前景色 */
    val keyPressedForeground: Color,
    /** 按键按下高亮覆盖层颜色 */
    val keyPressedHighlightColor: Color,
    /** 功能键背景色 */
    val functionKeyBackground: Color,
    /** 功能键前景色 */
    val functionKeyForeground: Color,
    /** 按键圆角半径 */
    val keyCornerShape: CornerSize,
    /** 候选词文本色 */
    val candidateTextColor: Color,
    /** 选中候选词文本色 */
    val candidateSelectedTextColor: Color,
    /** 选中候选词背景色 */
    val candidateSelectedBackground: Color,
    /** 候选词字号 */
    val candidateTextSize: TextUnit,
    /** 光标颜色 */
    val cursorColor: Color,
    /** 输入字符字号 */
    val charInputTextSize: TextUnit,
    /** 输入栏间隔条颜色 */
    val gapColor: Color,
    /** 手势触摸轨迹颜色 */
    val gestureTrailColor: Color,
    /** 手指指示器颜色 */
    val fingerIndicatorColor: Color,
    /** Message 类型弹出提示背景色 */
    val tipMessageBackground: Color,
    /** Action 类型弹出提示背景色 */
    val tipActionBackground: Color,
    /** 弹出提示字号 */
    val tipTextSize: TextUnit,
    /** 弹出提示操作按钮颜色 */
    val tipActionButtonColor: Color,
    /** 工具按钮背景色 */
    val toolBackground: Color,
    /** 选中工具按钮背景色 */
    val toolSelectedBackground: Color,
    /** 工具分组分隔线颜色 */
    val toolDividerColor: Color,
    /** 工具按钮间距 */
    val toolSpacing: Dp,
    /** 输入动作播放行指示器颜色 */
    val playerIndicatorColor: Color,
)

/** 预置主题集合，提供 Light 和 Night 两套配色方案 */
object KeyboardThemes {
    /** 亮色主题 */
    val Light = KeyboardColors(
        background = Color(0xFFF5F5F5),
        keyBackground = Color(0xFFFFFFFF),
        keyForeground = Color(0xFF1A1A1A),
        keyPressedBackground = Color(0xFFE0E0E0),
        keyPressedForeground = Color(0xFF1A1A1A),
        keyPressedHighlightColor = Color(0x33FFFFFF),
        functionKeyBackground = Color(0xFFE8E8E8),
        functionKeyForeground = Color(0xFF333333),
        keyCornerShape = CornerSize(8.dp),
        candidateTextColor = Color(0xFF333333),
        candidateSelectedTextColor = Color(0xFFFFFFFF),
        candidateSelectedBackground = Color(0xFF2196F3),
        candidateTextSize = 16.sp,
        cursorColor = Color(0xFF2196F3),
        charInputTextSize = 18.sp,
        gapColor = Color(0xFFCCCCCC),
        gestureTrailColor = Color(0x664CAF50),
        fingerIndicatorColor = Color(0x80FF9800),
        tipMessageBackground = Color(0xFF333333),
        tipActionBackground = Color(0xFF2196F3),
        tipTextSize = 14.sp,
        tipActionButtonColor = Color(0xFFFFFFFF),
        toolBackground = Color(0xFFE8E8E8),
        toolSelectedBackground = Color(0xFF2196F3),
        toolDividerColor = Color(0xFFCCCCCC),
        toolSpacing = 4.dp,
        playerIndicatorColor = Color(0xFFFF9800),
    )

    /** 暗色主题 */
    val Night = KeyboardColors(
        background = Color(0xFF1A1A1A),
        keyBackground = Color(0xFF2D2D2D),
        keyForeground = Color(0xFFE0E0E0),
        keyPressedBackground = Color(0xFF404040),
        keyPressedForeground = Color(0xFFE0E0E0),
        keyPressedHighlightColor = Color(0x33FFFFFF),
        functionKeyBackground = Color(0xFF333333),
        functionKeyForeground = Color(0xFFCCCCCC),
        keyCornerShape = CornerSize(8.dp),
        candidateTextColor = Color(0xFFCCCCCC),
        candidateSelectedTextColor = Color(0xFFFFFFFF),
        candidateSelectedBackground = Color(0xFF1976D2),
        candidateTextSize = 16.sp,
        cursorColor = Color(0xFF64B5F6),
        charInputTextSize = 18.sp,
        gapColor = Color(0xFF555555),
        gestureTrailColor = Color(0x6681C784),
        fingerIndicatorColor = Color(0x80FFB74D),
        tipMessageBackground = Color(0xFF555555),
        tipActionBackground = Color(0xFF1976D2),
        tipTextSize = 14.sp,
        tipActionButtonColor = Color(0xFFFFFFFF),
        toolBackground = Color(0xFF333333),
        toolSelectedBackground = Color(0xFF1976D2),
        toolDividerColor = Color(0xFF555555),
        toolSpacing = 4.dp,
        playerIndicatorColor = Color(0xFFFFB74D),
    )
}

/** CompositionLocal 提供当前主题的 KeyboardColors，默认使用亮色主题 */
val LocalKeyboardColors = compositionLocalOf { KeyboardThemes.Light }
