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

package org.crazydan.studio.app.ime.kuaizi.log

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import org.crazydan.studio.app.ime.kuaizi.engine.log.LogEntry
import org.crazydan.studio.app.ime.kuaizi.engine.log.LogWriter

/**
 * Android Logcat 日志写入器：将引擎的 [LogEntry] 映射到 [Log] 输出。
 *
 * 默认为同步写入（[bufferSize] = 0），直接调用 [Log.println]。
 * 可选的 Channel 缓冲将 Logcat 输出异步化，避免热路径中的同步 IPC 开销。
 * 缓冲满时通过 [Channel.trySend] 静默丢弃，确保写入者永不阻塞。
 *
 * 通常仅在 Debug 构建中注册，Release 构建不包含此 Writer，
 * 确保发布版本不会向 Logcat 输出敏感信息。
 *
 * @param bufferSize 缓冲区大小，0 表示同步写入（默认值）
 */
class LogcatWriter(
    bufferSize: Int = 0, // 0 = 同步写入（默认）
) : LogWriter {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val channel: Channel<LogEntry>? =
        if (bufferSize > 0) Channel(bufferSize)
        else null

    init {
        channel?.let { ch ->
            scope.launch {
                for (entry in ch) {
                    log(entry)
                }
            }
        }
    }

    override fun write(entry: LogEntry) {
        if (channel != null) {
            channel.trySend(entry) // 非阻塞，满则丢弃
        } else {
            log(entry)
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

    // -------------------------------------------------------

    private fun log(entry: LogEntry) {
        entry.apply {
            val msg = stackTrace?.let { "$message\n$it" } ?: message

            Log.println(level.priority, tag, msg)
        }
    }
}