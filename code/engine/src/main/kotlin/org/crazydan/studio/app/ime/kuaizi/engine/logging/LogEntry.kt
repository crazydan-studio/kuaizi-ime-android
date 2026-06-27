package org.crazydan.studio.app.ime.kuaizi.engine.logging

import org.crazydan.studio.app.ime.kuaizi.engine.LogLevel
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

data class LogEntry(
    val level: LogLevel,
    val tag: String,
    val message: String,
    val throwable: Throwable? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val threadName: String = Thread.currentThread().name,
    val threadId: Long = Thread.currentThread().id,
) {
    fun format(): String {
        val time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"))
        val base = "$time [${level.name}] [$tag] [$threadName] $message"
        return if (throwable != null) "$base\n${throwable.stackTraceToString()}" else base
    }
}
