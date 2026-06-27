package org.crazydan.studio.app.ime.kuaizi.engine

enum class LogLevel(val priority: Int) {
    VERBOSE(2),
    DEBUG(3),
    INFO(4),
    WARN(5),
    ERROR(6);

    companion object {
        fun fromPriority(priority: Int): LogLevel {
            return entries.firstOrNull { it.priority == priority } ?: WARN
        }
    }
}
