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
