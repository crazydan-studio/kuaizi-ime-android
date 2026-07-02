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

/**
 * 带标签的日志记录器。
 *
 * 每个模块或类通过 [ImeLog.logger] 获取带标签的实例。
 * 所有日志方法使用 `inline` + `lambda` 延迟求值模式，
 * 确保在日志等级不满足时不会执行消息参数的字符串拼接和对象格式化，
 * 避免性能浪费。
 *
 * [tree] 方法支持树形日志块，在调试复杂流程时提供结构化的日志输出。
 *
 * @param tag 日志标签，通常为类名或模块名
 * @param log 日志门面引用，用于分发日志条目
 */
class ImeLogger(private val tag: String, private val log: ImeLog) {

    /** 输出 VERBOSE 级别日志。 */
    fun verbose(msg: () -> String) = dispatch(LogLevel.VERBOSE, msg)

    /** 输出 DEBUG 级别日志。 */
    fun debug(msg: () -> String) = dispatch(LogLevel.DEBUG, msg)

    /** 输出 INFO 级别日志。 */
    fun info(msg: () -> String) = dispatch(LogLevel.INFO, msg)

    /** 输出 WARN 级别日志。 */
    fun warn(msg: () -> String) = dispatch(LogLevel.WARN, msg)

    /** 输出 ERROR 级别日志（仅消息）。 */
    fun error(msg: () -> String) = dispatch(LogLevel.ERROR, msg)

    /** 输出 ERROR 级别日志（包含异常信息）。 */
    fun error(throwable: Throwable, msg: () -> String) =
        dispatch(LogLevel.ERROR, msg, throwable)

    // ----------------------------------------------

    /**
     * 开始树形日志块。
     *
     * 在 [block] 执行期间，所有通过当前 logger 记录的日志将作为此树形块的子节点输出，
     * 形成嵌套结构，帮助理解执行流程的层次关系。
     * 块内日志通过 [TreeLogWriter] 临时收集，块结束时按树形结构格式化后统一输出。
     */
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

    // ----------------------------------------------

    /** 内部分发：等级不足时直接跳过 lambda 求值。 */
    private fun dispatch(level: LogLevel, msg: () -> String, throwable: Throwable? = null) {
        if (log.isEnabledLevel(level)) {
            log.dispatch(LogEntry(level, tag, msg(), throwable))
        }
    }
}
