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

package org.crazydan.studio.app.ime.kuaizi.engine

import org.crazydan.studio.app.ime.kuaizi.engine.input.CandidateList
import org.crazydan.studio.app.ime.kuaizi.engine.input.Clipboard
import org.crazydan.studio.app.ime.kuaizi.engine.input.FavoriteList
import org.crazydan.studio.app.ime.kuaizi.engine.input.InputList
import org.crazydan.studio.app.ime.kuaizi.engine.keyboard.Keyboard

/**
 * MVI 架构中的单一状态树根节点，作为 [ImeEngine] 对外暴露的唯一状态源。
 *
 * 所有 UI 组件通过 [StateFlow]<[ImeState]> 订阅状态驱动重组。
 * 不可变 [data class]，所有变更通过 [copy] 生成新实例。
 *
 * 包含七个字段覆盖输入法的全部逻辑状态：
 * - 键盘状态 ([keyboard])：描述当前键盘的类型、输入模式和状态机位置
 * - 输入列表 ([inputList])：管理用户输入的字符序列与游标
 * - 候选列表 ([candidateList])：承载候选词的分页和过滤数据
 * - 剪贴板 ([clipboard])：维护剪贴板检测状态
 * - 收藏列表 ([favoriteList])：维护收藏管理的状态
 * - 工具栏状态 ([toolListState])：管理工具栏按钮的配置与启用状态
 * - 运行时配置 ([config])：提供运行时配置的快照
 *
 * 一次性副作用信号（弹出提示、音效、触觉振动）通过独立的 [SharedFlow]<[ImeEffect]> 通道发射，
 * 与 [ImeState] 完全分离。
 *
 * @property keyboard 当前键盘的类型、左右手模式临时状态和交互状态
 * @property inputList 用户输入的字符序列与游标位置
 * @property candidateList 候选词列表及分页控制
 * @property clipboard 剪贴板状态
 * @property favoriteList 收藏列表状态
 * @property toolListState 工具栏按钮状态
 * @property config 运行时配置快照
 */
data class ImeState(
    val keyboard: Keyboard = Keyboard(),
    val inputList: InputList = InputList(),
    val candidateList: CandidateList = CandidateList(),
    val clipboard: Clipboard = Clipboard(),
    val favoriteList: FavoriteList = FavoriteList(),
    val toolListState: ToolListState = ToolListState(),
    val config: ImeConfig = ImeConfig(),
)

/**
 * 工具栏状态：管理键盘上方工具栏中各按钮的可用性和配置。
 *
 * 工具栏内容根据 [Keyboard.type]、[Keyboard.state] 和收藏功能门控动态配置。
 * 纳入 [ImeState] 而非由 ViewModel 本地维护，使得引擎可以统一管理和持久化工具栏配置。
 *
 * @property tools 工具栏按钮列表
 */
data class ToolListState(
    val tools: List<ToolItem> = emptyList(),
)

/**
 * 工具栏按钮项。
 *
 * @property label 按钮显示文本
 * @property icon 按钮图标名称
 * @property intent 点击按钮后发送的 [ImeIntent]
 * @property disabled 按钮是否禁用
 */
data class ToolItem(
    val label: String,
    val icon: String? = null,
    val intent: ImeIntent? = null,
    val disabled: Boolean = false,
)
