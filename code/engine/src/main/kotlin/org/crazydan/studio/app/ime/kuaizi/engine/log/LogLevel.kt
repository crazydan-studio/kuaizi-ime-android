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

package org.crazydan.studio.app.ime.kuaizi.engine.log

/**
 * 日志等级枚举，按优先级递增排列。
 *
 * 用于两个层面：
 * 1. 全局过滤：[ImeLog] 只分发优先级不低于当前等级的日志条目
 * 1. 运行时配置：应用层可通过 [ImeLog.enableLevel] 动态调整过滤阈值
 *
 * @property priority 整数优先级映射，用于与 Android Logcat 等系统集成，确保与 [android.util.Log] 的等级相对应
 */
enum class LogLevel(val priority: Int) {
    /** 细粒度流程追踪：状态机转换、输入列表变更等。 */
    VERBOSE(2),

    /** 开发期调试信息：候选词查询结果、按键事件参数等。 */
    DEBUG(3),

    /** 关键业务节点：输入提交、键盘切换、字典加载完成等。 */
    INFO(4),

    /** 可恢复的异常情况：字典查询超时降级、配置项缺失使用默认值等。 */
    WARN(5),

    /** 不可恢复错误：数据库损坏、InputConnection 丢失、崩溃异常等。 */
    ERROR(6);

    companion object {
        /**
         * 根据整数优先级反向查找对应的 [LogLevel]。
         * 用于集成 Android Logcat 等使用整数优先级的日志系统。
         * 未匹配时返回 [WARN] 作为安全默认值。
         */
        fun fromPriority(priority: Int): LogLevel {
            return entries.firstOrNull { it.priority == priority } ?: WARN
        }
    }
}
