package org.crazydan.studio.app.ime.kuaizi.engine.logging

import org.crazydan.studio.app.ime.kuaizi.engine.LogLevel

class ImeLogger(private val tag: String, @PublishedApi internal val log: ImeLog) {

    inline fun verbose(msg: () -> String) {
        if (log.level.priority <= LogLevel.VERBOSE.priority) {
            log.dispatch(LogEntry(LogLevel.VERBOSE, tag, msg()))
        }
    }

    inline fun debug(msg: () -> String) {
        if (log.level.priority <= LogLevel.DEBUG.priority) {
            log.dispatch(LogEntry(LogLevel.DEBUG, tag, msg()))
        }
    }

    inline fun info(msg: () -> String) {
        if (log.level.priority <= LogLevel.INFO.priority) {
            log.dispatch(LogEntry(LogLevel.INFO, tag, msg()))
        }
    }

    inline fun warn(msg: () -> String) {
        if (log.level.priority <= LogLevel.WARN.priority) {
            log.dispatch(LogEntry(LogLevel.WARN, tag, msg()))
        }
    }

    inline fun error(msg: () -> String) {
        if (log.level.priority <= LogLevel.ERROR.priority) {
            log.dispatch(LogEntry(LogLevel.ERROR, tag, msg()))
        }
    }

    inline fun error(throwable: Throwable, msg: () -> String) {
        if (log.level.priority <= LogLevel.ERROR.priority) {
            log.dispatch(LogEntry(LogLevel.ERROR, tag, msg(), throwable))
        }
    }

    inline fun tree(title: String, block: () -> Unit) {
        val treeWriter = TreeLogWriter(log, tag, title)
        treeWriter.begin()
        try {
            block()
        } finally {
            treeWriter.end()
        }
    }

    companion object {
        fun d(tag: String, message: String) {
            if (ImeLog.level.priority <= LogLevel.DEBUG.priority) {
                ImeLog.dispatch(LogEntry(LogLevel.DEBUG, tag, message))
            }
        }
    }
}
