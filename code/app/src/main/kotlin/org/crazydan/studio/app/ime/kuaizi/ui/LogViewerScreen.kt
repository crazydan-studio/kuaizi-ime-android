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

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.lifecycle.viewmodel.compose.viewModel
import org.crazydan.studio.app.ime.kuaizi.engine.logging.LogLevel
import org.crazydan.studio.app.ime.kuaizi.engine.logging.LogEntry

/**
 * 日志浏览界面。
 *
 * 支持按等级过滤、关键词搜索、实时滚动查看日志。
 * 日志条目使用 [LazyColumn] 渲染，不同等级用不同颜色标识：
 * - VERBOSE：灰色（最详细的追踪信息）
 * - DEBUG：蓝色（开发期调试信息）
 * - INFO：绿色（关键业务节点）
 * - WARN：橙色（可恢复的异常情况）
 * - ERROR：红色（不可恢复错误）
 */
@Composable
fun LogViewerScreen(
    viewModel: LogViewerViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        // 顶部工具栏：等级过滤、搜索、刷新
        LogViewerToolbar(
            levelFilter = state.levelFilter,
            keyword = state.keyword,
            onLevelFilterChange = viewModel::setLevelFilter,
            onKeywordChange = viewModel::setKeyword,
            onRefresh = viewModel::refresh,
        )

        // 日志条目列表，使用时间戳作为稳定 key
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = state.listState,
        ) {
            items(state.entries, key = { it.timestamp }) { entry ->
                LogEntryItem(entry)
            }
        }
    }
}

/**
 * 单条日志条目的渲染组件。
 *
 * 根据日志等级设置不同的文字颜色，
 * 使用等宽字体确保时间戳和标签列对齐。
 */
@Composable
private fun LogEntryItem(entry: LogEntry) {
    // 按等级分配颜色
    val textColor = when (entry.level) {
        LogLevel.VERBOSE -> Color.Gray
        LogLevel.DEBUG -> Color(0xFF2196F3)
        LogLevel.INFO -> Color(0xFF4CAF50)
        LogLevel.WARN -> Color(0xFFFF9800)
        LogLevel.ERROR -> Color(0xFFF44336)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp),
    ) {
        Text(
            text = entry.format(),
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            color = textColor,
        )
    }
}
