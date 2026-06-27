package org.crazydan.studio.app.ime.kuaizi.engine.logging

import kotlin.reflect.KClass
import org.crazydan.studio.app.ime.kuaizi.engine.LogLevel

object ImeLog {
    private val writers = mutableListOf<LogWriter>()

    var level: LogLevel = LogLevel.WARN
        private set

    fun init(level: LogLevel, writers: List<LogWriter>) {
        this.level = level
        this.writers.clear()
        this.writers.addAll(writers)
    }

    fun updateLevel(newLevel: LogLevel) {
        level = newLevel
    }

    fun logger(tag: String): ImeLogger = ImeLogger(tag, this)

    fun logger(cls: KClass<*>): ImeLogger = logger(cls.simpleName ?: "Unknown")

    @PublishedApi internal fun dispatch(entry: LogEntry) {
        if (entry.level.priority < level.priority) return
        writers.forEach { writer -> writer.write(entry) }
    }

    internal suspend fun flush() {
        writers.forEach { it.flush() }
    }
}
