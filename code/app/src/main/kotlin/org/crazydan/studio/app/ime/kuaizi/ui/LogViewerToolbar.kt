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

package org.crazydan.studio.app.ime.kuaizi.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.crazydan.studio.app.ime.kuaizi.engine.log.LogLevel

/**
 * 日志浏览界面的工具栏。
 *
 * 包含等级过滤器（DropdownMenu）、关键词搜索输入框和刷新按钮。
 * 等级过滤器显示当前选中的等级名称，点击展开选择列表。
 */
@Composable
fun LogViewerToolbar(
    /** 当前选中的等级过滤条件 */
    level: LogLevel?,
    /** 当前关键词搜索条件 */
    keyword: String?,
    /** 等级过滤变更回调 */
    onLevelChange: (LogLevel?) -> Unit,
    /** 关键词变更回调 */
    onKeywordChange: (String?) -> Unit,
    /** 刷新按钮点击回调 */
    onRefresh: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.padding(8.dp),
    ) {
        // 等级过滤下拉菜单
        FilterChip(
            selected = level != null,
            onClick = { expanded = true },
            label = { Text(level?.name ?: "All") },
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            // "全部"选项（null 表示不过滤）
            DropdownMenuItem(
                text = { Text("All") },
                onClick = {
                    onLevelChange(null)
                    expanded = false
                },
            )
            // 各日志等级选项
            LogLevel.entries.forEach { level ->
                DropdownMenuItem(
                    text = { Text(level.name) },
                    onClick = {
                        onLevelChange(level)
                        expanded = false
                    },
                )
            }
        }

        // 关键词搜索输入框
        OutlinedTextField(
            value = keyword ?: "",
            onValueChange = { onKeywordChange(it.ifBlank { null }) },
            placeholder = { Text("搜索...") },
            modifier = Modifier.weight(1f),
            singleLine = true,
        )

        // 刷新按钮
        IconButton(onClick = onRefresh) {
//            Icon(Icons.Default.Refresh, contentDescription = "刷新")
        }
    }
}
