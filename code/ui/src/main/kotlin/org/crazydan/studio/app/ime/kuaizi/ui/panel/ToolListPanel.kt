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
        toolList.tools.forEach { tool ->
            if (tool.intent != null) {
                TextButton(
                    onClick = { onToolClick(tool.intent) },
                    enabled = !tool.disabled,
                ) {
                    Text(text = tool.label, color = colors.functionKeyForeground)
                }
            }
        }
    }
}
