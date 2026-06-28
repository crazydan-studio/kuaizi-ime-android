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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import org.crazydan.studio.app.ime.kuaizi.engine.domain.InputWord
import org.crazydan.studio.app.ime.kuaizi.engine.input_action.InputActionFingerIndicator
import org.crazydan.studio.app.ime.kuaizi.ui.keyboard.CandidateListLayoutState
import org.crazydan.studio.app.ime.kuaizi.ui.theme.LocalKeyboardColors

/**
 * 候选列表面板。
 *
 * 展示当前输入的候选词列表，支持横向滚动。
 * 内建 [InputActionPlayer] 的指示器覆盖层，通过 [showIndicator] 参数控制显示。
 *
 * @param candidates 候选词列表
 * @param selectedIndex 当前选中的候选词索引
 * @param layoutState 布局状态（用于坐标解析）
 * @param onLayoutStateChanged 布局状态变更回调
 * @param showIndicator 是否显示播放器指示器
 * @param indicatorState 手指指示器状态
 * @param onCandidateTap 候选词点击回调
 * @param modifier 修饰符
 */
@Composable
fun CandidateListPanel(
    candidates: List<InputWord>,
    selectedIndex: Int = -1,
    layoutState: CandidateListLayoutState = CandidateListLayoutState(),
    onLayoutStateChanged: (CandidateListLayoutState) -> Unit = {},
    showIndicator: Boolean = false,
    indicatorState: InputActionFingerIndicator? = null,
    onCandidateTap: (InputWord) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val colors = LocalKeyboardColors.current
    val density = LocalDensity.current

    LazyRow(
        modifier = modifier.fillMaxWidth().height(48.dp),
    ) {
        // 遍历候选词列表，每项渲染为可点击的文本
        items(candidates) { candidate ->
            Text(
                text = candidate.text,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp)
                    .clickable { onCandidateTap(candidate) },
                color = colors.candidateTextColor,
                fontSize = colors.candidateTextSize,
            )
        }
    }
}
