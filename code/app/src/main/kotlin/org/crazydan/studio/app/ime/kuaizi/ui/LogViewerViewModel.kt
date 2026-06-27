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
import org.crazydan.studio.app.ime.kuaizi.engine.LogLevel
import org.crazydan.studio.app.ime.kuaizi.engine.logging.LogEntry
import org.crazydan.studio.app.ime.kuaizi.engine.logging.LogStorage

class LogViewerViewModel(private val storage: LogStorage) : ViewModel() {
    private val _state = MutableStateFlow(LogViewerState())
    val state: StateFlow<LogViewerState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch(Dispatchers.IO) {
            val entries = storage.readLogs(
                levelFilter = _state.value.levelFilter,
                keyword = _state.value.keyword,
            )
            _state.update { it.copy(entries = entries) }
        }
    }

    fun setLevelFilter(level: LogLevel?) {
        _state.update { it.copy(levelFilter = level) }
        refresh()
    }

    fun setKeyword(keyword: String?) {
        _state.update { it.copy(keyword = keyword) }
        refresh()
    }
}

data class LogViewerState(
    val entries: List<LogEntry> = emptyList(),
    val levelFilter: LogLevel? = null,
    val keyword: String? = null,
    val listState: LazyListState = LazyListState(),
)
