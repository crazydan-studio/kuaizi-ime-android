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
