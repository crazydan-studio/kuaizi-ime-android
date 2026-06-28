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

import kotlinx.coroutines.runBlocking
import kotlin.reflect.KClass

object ImeLog {
    internal var level: LogLevel = LogLevel.WARN
        private set

    private val writers = mutableListOf<LogWriter>()

    fun init(level: LogLevel, writers: List<LogWriter>) {
        this.level = level

        this.writers.clear()
        this.writers.addAll(writers)
    }

    fun enableLevel(newLevel: LogLevel) {
        level = newLevel
    }

    fun logger(tag: String): ImeLogger = ImeLogger(tag, this)

    fun logger(cls: KClass<*>): ImeLogger = logger(cls.simpleName ?: "Unknown")

    internal fun dispatch(entry: LogEntry) {
        if (entry.level.priority < level.priority) return

        writers.forEach { writer -> writer.write(entry) }
    }

    internal suspend fun flush() {
        writers.forEach { it.flush() }
    }

    class CrashInterceptor(private val storage: LogStorage) {
        private var defaultHandler: Thread.UncaughtExceptionHandler? = null

        fun install() {
            defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

            Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
                handleCrash(thread, throwable)
            }
        }

        private fun handleCrash(thread: Thread, throwable: Throwable) {
            val entry = LogEntry(
                level = LogLevel.ERROR,
                tag = "Crash",
                message = "未捕获异常 [${thread.name}]",
                throwable = throwable,
            )

            // 崩溃发生时，绕过 FileLogWriter 的 Channel 缓冲机制，
            // 直接通过 LogStorage.appendEntries() 同步写入崩溃信息。
            // 因为在崩溃场景下，进程即将终止，FileLogWriter 的独立协程可能来不及消费
            // Channel 中的缓冲日志 —— 直接同步写入确保崩溃信息不会丢失
            storage.appendEntries(listOf(entry))

            // 同步刷新所有 Writer
            runBlocking {
                flush()
            }

            // 委托系统默认处理器正常处理崩溃
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }
}
