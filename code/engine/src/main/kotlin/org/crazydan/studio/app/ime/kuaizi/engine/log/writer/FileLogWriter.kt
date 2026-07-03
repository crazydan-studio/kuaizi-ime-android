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

package org.crazydan.studio.app.ime.kuaizi.engine.log.writer

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.crazydan.studio.app.ime.kuaizi.engine.log.LogEntry
import org.crazydan.studio.app.ime.kuaizi.engine.log.LogStorage
import org.crazydan.studio.app.ime.kuaizi.engine.log.LogWriter

/**
 * 文件日志写入器：异步将日志写入文件。
 *
 * 采用协程 [Channel] 缓冲 + 独立协程批量写入的策略，确保日志写入不阻塞调用线程。
 * [Channel.trySend] 在缓冲区未满时立即返回，不会阻塞调用线程。
 * 独立协程使用「阻塞等待首条 + 非阻塞收集余量」的策略：
 * - 低流量时，[Channel.receive] 确保日志及时写入
 * - 高流量时，批量收集（最多 100 条）减少 I/O 次数
 *
 * @param storage 日志文件存储管理器
 */
class FileLogWriter(
    scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
    private val storage: LogStorage,
) : LogWriter {
    private val channel = Channel<LogEntry>(capacity = Channel.Factory.BUFFERED)

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

    /** 非阻塞发送日志条目到 Channel。 */
    override fun write(entry: LogEntry) {
        channel.trySend(entry)
    }

    /** 等待 Channel 清空，确保所有缓冲日志落盘。 */
    @OptIn(ExperimentalCoroutinesApi::class)
    override suspend fun flush() {
        while (!channel.isEmpty) {
            delay(50)
        }
    }
}
