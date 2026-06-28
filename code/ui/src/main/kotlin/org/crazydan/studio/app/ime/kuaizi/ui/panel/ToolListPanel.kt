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

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.crazydan.studio.app.ime.kuaizi.engine.ImeIntent
import org.crazydan.studio.app.ime.kuaizi.engine.ToolListState
import org.crazydan.studio.app.ime.kuaizi.engine.input_action.InputActionFingerIndicator
import org.crazydan.studio.app.ime.kuaizi.ui.keyboard.KeyLayoutState
import org.crazydan.studio.app.ime.kuaizi.ui.theme.LocalKeyboardColors

/**
 * 工具列表面板。
 *
 * 展示键盘工具栏中的工具按钮列表（如全选、复制、粘贴等编辑功能键），
 * 内建 [InputActionPlayer] 的指示器覆盖层。
 * 与 [InputListPanel] 互斥共享 Row 2 空间，由 [isInputting] 状态控制切换。
 *
 * @param toolList 工具列表状态
 * @param layoutState 布局状态
 * @param onLayoutStateChanged 布局状态变更回调
 * @param showIndicator 是否显示播放器指示器
 * @param indicatorState 手指指示器状态
 * @param onToolClick 工具按钮点击回调
 * @param modifier 修饰符
 */
@Composable
fun ToolListPanel(
    toolList: ToolListState,
    layoutState: KeyLayoutState = KeyLayoutState(),
    onLayoutStateChanged: (KeyLayoutState) -> Unit = {},
    showIndicator: Boolean = false,
    indicatorState: InputActionFingerIndicator? = null,
    onToolClick: (ImeIntent) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val colors = LocalKeyboardColors.current

    Row(
        modifier = modifier.fillMaxWidth().height(48.dp).padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        // 遍历工具列表，每个有 intent 的项渲染为可点击按钮
        toolList.tools.forEach { tool ->
            if (tool.intent != null) {
                TextButton(
                    onClick = { onToolClick(tool.intent!!) },
                    enabled = !tool.disabled,
                ) {
                    Text(text = tool.label, color = colors.functionKeyForeground)
                }
            }
        }
    }
}
