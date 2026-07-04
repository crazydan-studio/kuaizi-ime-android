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

package org.crazydan.studio.app.ime.kuaizi.engine.log

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private val format = Json {
    encodeDefaults = true      // 支持 encode 时序列化缺省值
    prettyPrint = false        // 单行
    ignoreUnknownKeys = true   // 在 decode 时忽略未知属性
    explicitNulls = false      // 忽略 null 值
}

/**
 * 日志条目：不可变的日志数据类，每次日志调用创建一个新实例。
 *
 * 所有字段在构造时确定，不存在可变状态，确保线程安全。
 * 包含完整的上下文信息：等级、标签、消息、异常对象、时间戳和线程信息。
 *
 * @property level 日志等级
 * @property tag 日志标签，通常为模块名或类名
 * @property timestamp 日志时间戳（毫秒）
 * @property threadId 线程 ID
 * @property message 日志消息内容
 * @property stackTrace 异常堆栈信息
 */
@Serializable
data class LogEntry(
    // Note：映射的 json 属性采用单字符，以尽可能减少日志文件大小
    @SerialName("t")
    val timestamp: Long,
    @SerialName("l")
    val level: LogLevel,
    @SerialName("g")
    val tag: String,
    @SerialName("r")
    val threadId: Long,
    @SerialName("m")
    val message: String,
    @SerialName("e")
    val stackTrace: String?,
) {
    constructor(
        timestamp: Long = System.currentTimeMillis(),
        level: LogLevel,
        tag: String,
        threadId: Long = Thread.currentThread().id,
        message: String,
        exception: Throwable? = null
    ) : this(
        timestamp = timestamp,
        level = level,
        tag = tag,
        threadId = threadId,
        message = message,
        stackTrace = exception?.stackTraceToString()
    )

    /** 格式化为 JSON 字符串。 */
    fun format(): String =
        format.encodeToString(this)

    companion object {

        /** 将 [format] 格式化后的结果解析为 [LogEntry] 对象 */
        fun parse(log: String): LogEntry? =
            runCatching {
                format.decodeFromString<LogEntry>(log)
            }.getOrNull()
    }
}
