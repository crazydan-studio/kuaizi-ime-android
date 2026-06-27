package org.crazydan.studio.app.ime.kuaizi.engine.logging

import org.crazydan.studio.app.ime.kuaizi.engine.LogLevel

object ImeLog {
    var level: LogLevel = LogLevel.WARN
        private set

    private val writers: MutableList<LogWriter> = mutableListOf()

    fun init(level: LogLevel, writers: List<LogWriter>) {
        this.level = level
        this.writers.clear()
        this.writers.addAll(writers)
    }

    fun updateLevel(newLevel: LogLevel) {
        level = newLevel
    }

    fun logger(tag: String): ImeLogger = ImeLogger(tag, this)
    fun logger(cls: Class<*>): ImeLogger = logger(cls.simpleName)

    internal fun dispatch(entry: LogEntry) {
        if (entry.level.priority < level.priority) return
        writers.forEach { writer -> writer.write(entry) }
    }

    internal fun flush() {
        writers.forEach { it.flush() }
    }
}
