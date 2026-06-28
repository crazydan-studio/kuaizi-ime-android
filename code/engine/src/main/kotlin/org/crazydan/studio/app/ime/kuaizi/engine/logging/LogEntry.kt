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

package org.crazydan.studio.app.ime.kuaizi.engine.logging

import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.crazydan.studio.app.ime.kuaizi.engine.util.DateTimeHelper
import kotlin.time.Instant

/**
 * 日志条目：不可变的日志数据类，每次日志调用创建一个新实例。
 *
 * 所有字段在构造时确定，不存在可变状态，确保线程安全。
 * 包含完整的上下文信息：等级、标签、消息、异常对象、时间戳和线程信息。
 *
 * @property level 日志等级
 * @property tag 日志标签，通常为模块名或类名
 * @property message 日志消息内容
 * @property throwable 可选的异常对象
 * @property timestamp 日志时间戳（毫秒）
 * @property threadName 线程名
 * @property threadId 线程 ID
 */
data class LogEntry(
    val level: LogLevel,
    val tag: String,
    val message: String,
    val throwable: Throwable? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val threadName: String = Thread.currentThread().name,
    val threadId: Long = Thread.currentThread().id,
) {

    /**
     * 格式化为可读字符串。
     * 输出格式：`yyyy-MM-dd HH:mm:ss.SSS [LEVEL] [TAG] [ThreadName] message`
     * 异常对象的完整堆栈信息追加在消息之后。
     */
    fun format(): String {
        val time = Instant.fromEpochMilliseconds(timestamp).toLocalDateTime(TimeZone.currentSystemDefault())
        val timeStr = DateTimeHelper.dateTimeFormat.format(time)

        val throwableStr = throwable?.stackTraceToString()?.let { "\n$it" } ?: ""

        return "$timeStr [${level.name}] [$tag] [$threadName] $message$throwableStr"
    }
}
