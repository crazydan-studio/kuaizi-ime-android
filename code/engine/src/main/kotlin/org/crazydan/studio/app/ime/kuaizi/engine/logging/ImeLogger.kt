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

import org.crazydan.studio.app.ime.kuaizi.engine.LogLevel

class ImeLogger(@PublishedApi internal val tag: String, @PublishedApi internal val log: ImeLog) {

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
