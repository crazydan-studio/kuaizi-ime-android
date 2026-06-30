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

/**
 * 日志系统全局门面，采用单例模式。
 *
 * 核心职责：
 * - 管理当前生效的日志等级
 * - 维护 [LogWriter] 注册表
 * - 分发日志条目到所有已注册的 [LogWriter]
 * - 提供 [ImeLogger] 实例的工厂方法
 *
 * 所有日志操作通过 [logger] 获取的 [ImeLogger] 实例执行，[ImeLog] 本身不提供直接的日志记录方法。
 * [dispatch] 和 [flush] 方法标记为 internal，仅对 [ImeLogger] 和 [CrashInterceptor] 可见。
 */
object ImeLog {
    /** 当前生效的日志等级，默认只输出 WARN 及以上级别。 */
    internal var level: LogLevel = LogLevel.WARN
        private set

    private val writers = mutableListOf<LogWriter>()

    /**
     * 初始化日志系统。
     *
     * @param level 初始日志等级（由应用层根据构建类型决定）
     * @param writers 日志输出目标列表（应用层注册平台特有 Writer）
     */
    fun init(level: LogLevel, writers: List<LogWriter>) {
        this.level = level

        this.writers.clear()
        this.writers.addAll(writers)
    }

    /** 动态更新日志等级。 */
    fun changeLevel(newLevel: LogLevel) {
        level = newLevel
    }

    /** 获取带字符串标签的 [ImeLogger] 实例。 */
    fun logger(tag: String): ImeLogger = ImeLogger(tag, this)

    /** 获取带类名标签的 [ImeLogger] 实例，标签自动取 [KClass.simpleName]。 */
    fun logger(cls: KClass<*>): ImeLogger = logger(cls.simpleName ?: "Unknown")

    /** 分发日志条目到所有已注册的 Writer，低于当前等级的日志直接丢弃。 */
    internal fun dispatch(entry: LogEntry) {
        if (entry.level.priority < level.priority) return

        writers.forEach { writer -> writer.write(entry) }
    }

    /** 刷新所有 Writer 的缓冲区。 */
    internal suspend fun flush() {
        writers.forEach { it.flush() }
    }

    /**
     * 崩溃拦截器：安装为 JVM 的 [Thread.UncaughtExceptionHandler]，
     * 在应用崩溃时确保日志不会丢失。
     *
     * 在崩溃场景下，绕过 [FileLogWriter] 的 Channel 缓冲机制，
     * 直接通过 [LogStorage.appendEntries] 同步写入崩溃信息。
     * 这是因为进程即将终止，FileLogWriter 的独立协程可能来不及消费
     * Channel 中的缓冲日志——直接同步写入确保崩溃信息不会丢失。
     */
    class CrashInterceptor(private val storage: LogStorage) {
        private var defaultHandler: Thread.UncaughtExceptionHandler? = null

        /** 安装崩溃拦截器，替换系统默认的未捕获异常处理器。 */
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

            // 绕过 FileLogWriter 的 Channel 缓冲，直接同步写入文件
            storage.appendEntries(listOf(entry))

            // 同步刷新所有 Writer 的缓冲区
            runBlocking {
                flush()
            }

            // 委托系统默认处理器正常处理崩溃
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }
}
