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

package org.crazydan.studio.app.ime.kuaizi.engine.input_action

/**
 * 输入动作脚本，将有序的 [InputAction] 序列组合为可命名、可描述、可回放的脚本。
 *
 * 脚本是输入动作程序化的核心数据载体——编译器产出脚本，回放器消费脚本，UI 展示脚本列表供用户选择。
 * 脚本的不可变性确保线程安全和可共享性——同一脚本实例可以被多个回放器同时消费，无需拷贝或同步。
 * 脚本的唯一创建路径是通过 [InputActionScriptCompiler.compile] 编译产出。
 *
 * @property name 脚本的人类可读名称，建议采用「内容 + 模式」格式，如 "你好-Tap"
 * @property description 脚本详细描述，可包含输入文本、模式类型、动作数量等元信息
 * @property inputActionMode 输入交互模式（点击 / 滑行）
 * @property actions 按 [startTime] 升序排列的动作列表
 * @property totalDuration 脚本完整时长（毫秒），等于最后一个动作的 startTime + 持续时长
 */
data class InputActionScript(
    val name: String,
    val description: String,
    val inputActionMode: InputActionMode = InputActionMode.Tap,
    val actions: List<InputAction>,
    val totalDuration: Long,
)

/**
 * 输入交互模式，决定编译器如何将文本转换为动作序列。
 *
 * - [Tap]：逐键点击模式，每个按键独立完成「按下→抬起」，相邻按键通过 [InputAction.Wait] 间隔
 * - [Swipe]：滑行输入模式，同一音节内按键通过 [InputAction.SwipeTo] 连续连接形成滑行轨迹
 */
enum class InputActionMode { Tap, Swipe }
