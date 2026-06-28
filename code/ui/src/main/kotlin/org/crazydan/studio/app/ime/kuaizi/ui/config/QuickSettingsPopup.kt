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

package org.crazydan.studio.app.ime.kuaizi.ui.config

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.crazydan.studio.app.ime.kuaizi.engine.ImeConfig

/**
 * 快捷设置弹窗组件。
 *
 * 从键盘工具栏弹出的快捷设置面板，提供高频配置项的快速切换入口。
 * 以 [Popup] 形式展示，点击弹窗外部区域关闭。
 *
 * 设置项按优先级排列：主题模式 → 手模式 → 「更多设置」按钮。
 * 配置变更通过 [onConfigChanged] 回调立即生效。
 *
 * @param config 当前配置
 * @param onConfigChanged 配置变更回调
 * @param onDismiss 弹窗关闭回调
 * @param modifier 修饰符
 */
@Composable
fun QuickSettingsPopup(
    config: ImeConfig,
    onConfigChanged: (ImeConfig) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
}
