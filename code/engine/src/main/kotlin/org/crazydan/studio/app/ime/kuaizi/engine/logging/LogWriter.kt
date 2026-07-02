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

/**
 * 日志写入器接口：日志输出目标的抽象。
 *
 * 采用策略模式实现输出目标的可扩展性。引擎内置 [FileLogWriter] 用于文件持久化，
 * 提供 [LogcatWriter] 用于 Android Logcat 输出。
 * 第三方应用可以实现此接口，将日志输出到远程服务器、遥测系统或任何自定义目标。
 *
 * 所有 [LogWriter] 实现通过 [ImeLog.setWriters] 注册，由 [ImeLog.dispatch] 统一分发。
 * 实现必须保证 [write] 的线程安全——日志可能从不同协程或线程同时写入。
 */
interface LogWriter {

    /** 写入一条日志条目。实现必须保证线程安全。 */
    fun write(entry: LogEntry)

    /** 刷新缓冲区，确保所有待写入日志落盘。 */
    suspend fun flush()
}
