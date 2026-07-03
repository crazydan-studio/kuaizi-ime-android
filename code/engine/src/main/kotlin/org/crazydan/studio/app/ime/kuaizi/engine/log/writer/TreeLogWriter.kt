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

package org.crazydan.studio.app.ime.kuaizi.engine.log.writer

import org.crazydan.studio.app.ime.kuaizi.engine.log.ImeLog
import org.crazydan.studio.app.ime.kuaizi.engine.log.LogEntry
import org.crazydan.studio.app.ime.kuaizi.engine.log.LogLevel
import org.crazydan.studio.app.ime.kuaizi.engine.log.LogWriter

/**
 * 树形日志写入器：将日志条目按树形结构组织后输出。
 *
 * 在 [ImeLogger.tree] 块中使用，临时拦截日志输出，在块结束时将收集的日志
 * 按树形结构格式化后统一输出。输出格式：
 * ```
 * ┌─ title
 * │ entry 1
 * │ entry 2
 * └─ (title end)
 * ```
 *
 * 树形日志在调试复杂流程时提供结构化的日志输出，帮助理解执行流程的层次关系。
 *
 * @param log 日志门面引用
 * @param tag 日志标签
 * @param title 树形块标题
 */
class TreeLogWriter(
    private val log: ImeLog,
    private val tag: String,
    private val title: String,
) : LogWriter {
    private val entries = mutableListOf<LogEntry>()

    /** 开始树形块：清空之前收集的日志条目。 */
    fun begin() {
        entries.clear()
    }

    /** 结束树形块：将收集的日志按树形结构输出。 */
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

    /** 收集日志条目，添加树形缩进前缀。 */
    override fun write(entry: LogEntry) {
        entries.add(
            entry.copy(
                message = "│ ${entry.message}",
            )
        )
    }

    /** 树形写入器无需刷新。 */
    override suspend fun flush() {
        // no-op
    }
}
