package org.crazydan.studio.app.ime.kuaizi.engine.logging

import org.crazydan.studio.app.ime.kuaizi.engine.LogLevel

class ImeLogger(private val tag: String, private val log: ImeLog) {

    inline fun verbose(msg: () -> String) {
        if (log.level <= LogLevel.VERBOSE) {
            log.dispatch(LogEntry(LogLevel.VERBOSE, tag, msg()))
        }
    }

    inline fun debug(msg: () -> String) {
        if (log.level <= LogLevel.DEBUG) {
            log.dispatch(LogEntry(LogLevel.DEBUG, tag, msg()))
        }
    }

    inline fun info(msg: () -> String) {
        if (log.level <= LogLevel.INFO) {
            log.dispatch(LogEntry(LogLevel.INFO, tag, msg()))
        }
    }

    inline fun warn(msg: () -> String) {
        if (log.level <= LogLevel.WARN) {
            log.dispatch(LogEntry(LogLevel.WARN, tag, msg()))
        }
    }

    inline fun error(msg: () -> String) {
        if (log.level <= LogLevel.ERROR) {
            log.dispatch(LogEntry(LogLevel.ERROR, tag, msg()))
        }
    }

    fun error(throwable: Throwable, msg: () -> String) {
        if (log.level <= LogLevel.ERROR) {
            log.dispatch(LogEntry(LogLevel.ERROR, tag, msg(), throwable))
        }
    }

    companion object {
        fun d(tag: String, message: String) {
            if (ImeLog.level <= LogLevel.DEBUG) {
                ImeLog.dispatch(LogEntry(LogLevel.DEBUG, tag, message))
            }
        }
    }
}
