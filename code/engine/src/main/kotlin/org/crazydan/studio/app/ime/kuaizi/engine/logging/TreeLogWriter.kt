package org.crazydan.studio.app.ime.kuaizi.engine.logging

import org.crazydan.studio.app.ime.kuaizi.engine.LogLevel

class TreeLogWriter(
    private val log: ImeLog,
    private val tag: String,
    private val title: String,
) : LogWriter {
    private val entries = mutableListOf<LogEntry>()
    private var originalWriters: List<LogWriter>? = null

    fun begin() {
        originalWriters = mutableListOf<LogWriter>()
        entries.clear()
    }

    fun end() {
        if (entries.isEmpty()) return
        val header = LogEntry(
            level = LogLevel.DEBUG,
            tag = tag,
            message = "┌─ $title",
        )
        val footer = LogEntry(
            level = LogLevel.DEBUG,
            tag = tag,
            message = "└─ ($title end)",
        )
        log.dispatch(header)
        entries.forEach { log.dispatch(it) }
        log.dispatch(footer)
    }

    override fun write(entry: LogEntry) {
        entries.add(
            entry.copy(
                message = "│ ${entry.message}",
            )
        )
    }

    override suspend fun flush() {
        // no-op for tree writer
    }
}
