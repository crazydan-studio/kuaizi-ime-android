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

import org.crazydan.studio.app.ime.kuaizi.engine.logging.writer.TreeLogWriter

class ImeLogger(private val tag: String, private val log: ImeLog) {

    fun isEnabled(level: LogLevel) = log.level.priority <= level.priority

    fun verbose(msg: () -> String) = dispatch(LogLevel.VERBOSE, msg)

    fun debug(msg: () -> String) = dispatch(LogLevel.DEBUG, msg)

    fun info(msg: () -> String) = dispatch(LogLevel.INFO, msg)

    fun warn(msg: () -> String) = dispatch(LogLevel.WARN, msg)

    fun error(msg: () -> String) = dispatch(LogLevel.ERROR, msg)

    fun error(throwable: Throwable, msg: () -> String) =
        dispatch(LogLevel.ERROR, msg, throwable)

    fun tree(title: String, block: () -> Unit) {
        // TODO 树形日志输入待改进
        val writer = TreeLogWriter(log, tag, title)

        writer.begin()
        try {
            block()
        } finally {
            writer.end()
        }
    }

    private inline fun dispatch(level: LogLevel, msg: () -> String, throwable: Throwable? = null) {
        if (!isEnabled(level)) return

        log.dispatch(LogEntry(level, tag, msg(), throwable))
    }
}
