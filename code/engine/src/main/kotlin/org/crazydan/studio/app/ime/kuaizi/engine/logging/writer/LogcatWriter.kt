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

package org.crazydan.studio.app.ime.kuaizi.engine.logging.writer

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import org.crazydan.studio.app.ime.kuaizi.engine.logging.LogEntry
import org.crazydan.studio.app.ime.kuaizi.engine.logging.LogWriter

class LogcatWriter(
    bufferSize: Int = 0, // 0 = 同步写入（默认）
) : LogWriter {
    private val channel: Channel<LogEntry>? =
        if (bufferSize > 0) Channel(bufferSize) else null

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    init {
        channel?.let { ch ->
            scope.launch {
                for (entry in ch) {
                    Log.println(entry.level.priority, entry.tag, entry.message)
                }
            }
        }
    }

    override fun write(entry: LogEntry) {
        if (channel != null) {
            channel.trySend(entry) // 非阻塞，满则丢弃
        } else {
            Log.println(entry.level.priority, entry.tag, entry.message)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override suspend fun flush() {
        channel?.let {
            while (!it.isEmpty) {
                Thread.sleep(10)
            }
        }
    }
}