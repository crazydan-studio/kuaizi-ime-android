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

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.crazydan.studio.app.ime.kuaizi.engine.logging.LogEntry
import org.crazydan.studio.app.ime.kuaizi.engine.logging.LogStorage
import org.crazydan.studio.app.ime.kuaizi.engine.logging.LogWriter

class FileLogWriter(private val storage: LogStorage) : LogWriter {
    private val channel = Channel<LogEntry>(capacity = Channel.Factory.BUFFERED)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        scope.launch {
            val buffer = mutableListOf<LogEntry>()
            while (true) {
                // 阻塞等待第一条日志
                val entry = channel.receive()
                buffer.add(entry)

                // 非阻塞收集更多日志，最多 100 条
                while (buffer.size < 100) {
                    val polled = channel.tryReceive().getOrNull() ?: break
                    buffer.add(polled)
                }

                // 批量写入文件
                storage.appendEntries(buffer)
                buffer.clear()
            }
        }
    }

    override fun write(entry: LogEntry) {
        channel.trySend(entry)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override suspend fun flush() {
        // 等待 Channel 中的所有条目被消费完毕
        while (!channel.isEmpty) {
            delay(50)
        }
    }
}