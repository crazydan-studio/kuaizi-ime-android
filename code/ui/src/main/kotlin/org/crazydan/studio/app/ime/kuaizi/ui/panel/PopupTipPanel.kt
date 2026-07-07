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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.crazydan.studio.app.ime.kuaizi.engine.ImeIntent
import org.crazydan.studio.app.ime.kuaizi.ui.theme.LocalKeyboardColors
import org.crazydan.studio.app.ime.kuaizi.ui.viewmodel.PopupTipState

/**
 * 弹出提示面板。
 *
 * 展示两种类型的弹出提示：
 * - [PopupTipState.Message]：纯文本信息提示，超时后自动消失
 * - [PopupTipState.Action]：带操作按钮的可交互提示，按钮点击触发 [ImeIntent]
 *
 * 以覆盖层形式悬浮于键盘上方，与 [CandidateListPanel] 叠加共享 Row 1 空间。
 *
 * @param tipState 弹出提示状态，为 null 时不显示
 * @param onAction Action 类型提示的按钮点击回调
 * @param modifier 修饰符
 */
@Composable
fun PopupTipPanel(
    tipState: PopupTipState?,
    onAction: (ImeIntent) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    // tipState 为 null 时不渲染任何内容
    if (tipState == null) return

    val colors = LocalKeyboardColors.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(
                // 根据提示类型选择不同的背景色
                when (tipState) {
                    is PopupTipState.Message -> colors.tipMessageBackground
                    is PopupTipState.Action -> colors.tipActionBackground
                    is PopupTipState.Key -> TODO()
                }
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        when (tipState) {
            is PopupTipState.Message -> {
                // Message 类型：仅显示文本
                Text(text = tipState.message, color = Color.White, fontSize = colors.tipTextSize)
            }

            is PopupTipState.Action -> {
                // Action 类型：文本 + 操作按钮
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = tipState.message, color = Color.White, modifier = Modifier.weight(1f))
                    TextButton(onClick = { onAction(tipState.action) }) {
                        Text(text = tipState.actionLabel, color = colors.tipActionButtonColor)
                    }
                }
            }

            is PopupTipState.Key -> TODO()
        }
    }
}
