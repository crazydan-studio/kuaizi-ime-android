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

import androidx.compose.foundation.lazy.LazyListState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.crazydan.studio.app.ime.kuaizi.engine.logging.LogLevel
import org.crazydan.studio.app.ime.kuaizi.engine.logging.LogEntry
import org.crazydan.studio.app.ime.kuaizi.engine.logging.LogStorage

/**
 * 日志浏览界面的 ViewModel。
 *
 * 管理日志列表的加载、过滤和搜索状态。
 * 日志读取操作在 [Dispatchers.IO] 上执行，避免阻塞主线程。
 * 过滤条件变更时自动刷新日志列表。
 */
class LogViewerViewModel(private val storage: LogStorage) : ViewModel() {
    private val _state = MutableStateFlow(LogViewerState())
    val state: StateFlow<LogViewerState> = _state.asStateFlow()

    init {
        // 初始化时加载日志
        refresh()
    }

    /**
     * 刷新日志列表。
     *
     * 使用当前过滤条件从 [LogStorage] 读取日志条目。
     * 在 IO 线程执行，结果通过 StateFlow 更新到 UI。
     */
    fun refresh() {
        viewModelScope.launch(Dispatchers.IO) {
            val entries = storage.readLogs(
                levelFilter = _state.value.levelFilter,
                keyword = _state.value.keyword,
            )
            _state.update { it.copy(entries = entries) }
        }
    }

    /** 设置日志等级过滤条件并自动刷新。null 表示不过滤。 */
    fun setLevelFilter(level: LogLevel?) {
        _state.update { it.copy(levelFilter = level) }
        refresh()
    }

    /** 设置关键词搜索条件并自动刷新。null 或空字符串表示不搜索。 */
    fun setKeyword(keyword: String?) {
        _state.update { it.copy(keyword = keyword) }
        refresh()
    }
}

/**
 * 日志浏览界面的状态。
 *
 * 包含日志条目列表、过滤条件、搜索关键词和列表滚动状态。
 */
data class LogViewerState(
    /** 当前显示的日志条目列表 */
    val entries: List<LogEntry> = emptyList(),
    /** 日志等级过滤条件，null 表示不过滤 */
    val levelFilter: LogLevel? = null,
    /** 关键词搜索条件，null 表示不搜索 */
    val keyword: String? = null,
    /** LazyColumn 的滚动状态 */
    val listState: LazyListState = LazyListState(),
)
