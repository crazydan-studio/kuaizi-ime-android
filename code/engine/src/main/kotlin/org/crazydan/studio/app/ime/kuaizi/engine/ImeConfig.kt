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

import org.crazydan.studio.app.ime.kuaizi.annotation.DataStoreConfig
import org.crazydan.studio.app.ime.kuaizi.engine.domain.KeyboardHandMode
import org.crazydan.studio.app.ime.kuaizi.engine.domain.KeyboardInputMode
import org.crazydan.studio.app.ime.kuaizi.engine.domain.KeyboardThemeType
import org.crazydan.studio.app.ime.kuaizi.engine.logging.LogLevel

/**
 * 统一的运行时配置，包含引擎配置、UI 配置和运行时配置三层子配置。
 *
 * 引擎配置（[Engine]）和 UI 配置（[Ui]）均为持久化配置项，
 * 运行时配置（[Runtime]）不做持久化。
 * 对配置项的修改在 UI 和引擎层面都是即时生效的。
 * 库不内置配置持久化，所有配置通过 [ImeConfig] 在创建时或运行时设置，持久化是应用层的职责。
 *
 * @property engine 引擎配置，影响引擎的核心行为
 * @property ui UI 配置，影响界面呈现和交互反馈
 * @property runtime 运行时配置，承载不持久化的临时状态
 */
data class ImeConfig(
    val engine: Engine = Engine(),
    val ui: Ui = Ui(),
    val runtime: Runtime = Runtime(),
) {
    /**
     * 引擎配置：影响引擎的核心行为，均为持久化配置项。
     *
     * @property logLevel 日志等级，由应用层注入
     * @property logStoragePath 日志文件存放目录路径，null 使用默认应用私有目录
     * @property inputPredictionEnabled 是否启用输入预测，用于输入补全
     * @property userDataPersistEnabled 是否持久化用户数据
     * @property favoriteInputEnabled 是否启用输入收藏
     * @property favoriteClipEnabled 是否启用剪贴板收藏
     * @property favoriteSyncToUserDictEnabled 是否启用收藏与用户字典的同步
     * @property candidateVariantFirstEnabled 是否启用繁体优先
     */
    @DataStoreConfig(prefix = "engine")
    data class Engine(
        val logLevel: LogLevel = LogLevel.WARN,
        val logStoragePath: String? = null,

        val inputPredictionEnabled: Boolean = true,
        val userDataPersistEnabled: Boolean = true,

        val favoriteInputEnabled: Boolean = true,
        val favoriteClipEnabled: Boolean = true,
        val favoriteSyncToUserDictEnabled: Boolean = false,

        val candidateVariantFirstEnabled: Boolean = false,
    )

    /**
     * UI 配置：影响界面呈现和交互反馈，均为持久化配置项。
     *
     * @property keyboardInputMode 键盘输入模式，影响按键布局和交互逻辑
     * @property keyboardHandMode 键盘左右手模式，影响按键布局调整
     * @property keyboardThemeType 键盘主题样式类型
     * @property keyPopupTipsEnabled 是否显示按键输入提示
     * @property audioFeedbackEnabled 是否启用按键音效反馈
     * @property hapticFeedbackEnabled 是否启用触觉反馈
     * @property keyAnimationEnabled 是否启用按键动画
     * @property gestureSlippingTrailEnabled 是否启用滑行轨迹显示
     * @property clipPopupTipsEnabled 是否启用剪贴板收藏弹出提示
     * @property clipPastePopupTipsEnabled 是否启用可粘贴内容的弹出提示
     * @property clipPopupTipsTimeout 剪贴板弹出提示超时（秒）
     * @property adaptDesktopSwipeUpGesture 是否适配桌面下滑手势
     * @property candidatesPagingAudioEnabled 候选词翻页是否播放音效
     */
    @DataStoreConfig(prefix = "ui")
    data class Ui(
        val keyboardInputMode: KeyboardInputMode = KeyboardInputMode.RectGrid,
        val keyboardHandMode: KeyboardHandMode = KeyboardHandMode.Right,
        val keyboardThemeType: KeyboardThemeType = KeyboardThemeType.FollowSystem,

        val keyPopupTipsEnabled: Boolean = true,
        val audioFeedbackEnabled: Boolean = true,
        val hapticFeedbackEnabled: Boolean = true,
        val keyAnimationEnabled: Boolean = true,
        val gestureSlippingTrailEnabled: Boolean = true,

        val clipPopupTipsEnabled: Boolean = true,
        val clipPastePopupTipsEnabled: Boolean = true,
        val clipPopupTipsTimeout: Int = 15,

        val adaptDesktopSwipeUpGesture: Boolean = false,
        val candidatesPagingAudioEnabled: Boolean = true,
    )

    /**
     * 运行时配置：不做持久化的临时状态。
     *
     * @property screenOrientation 屏幕方向，仅影响 UI 层键盘布局形式
     * @property editorInputType 编辑器输入类型，决定启动时的键盘类型
     * @property keyPopupTipsEnabled 是否启用按键输入提示，可覆盖 UiConfig 的同名配置
     * @property toolSettingsEnabled 是否启用工具栏中的配置按钮
     * @property toolSwitchInputMethodEnabled 是否启用工具栏中的输入法切换按钮
     * @property toolCloseKeyboardEnabled 是否启用工具栏中的关闭键盘按钮
     */
    data class Runtime(
        val screenOrientation: ScreenOrientation = ScreenOrientation.Landscape,
        val editorInputType: EditorInputType = EditorInputType.Text,

        val keyPopupTipsEnabled: Boolean? = null,

        val toolSettingsEnabled: Boolean = true,
        val toolSwitchInputMethodEnabled: Boolean = true,
        val toolCloseKeyboardEnabled: Boolean = true,
    )

    /**
     * 启动配置：仅作为 [ImeEngine.start] 的参数，用于初始化 [Runtime]。
     *
     * @property inputMethodSubtype 系统输入法子类型，决定基础键盘类型
     * @property screenOrientation 屏幕方向
     * @property editorInputType 目标编辑器的输入类型，null 表示不覆盖当前值
     */
    data class Startup(
        val inputMethodSubtype: InputMethodSubtype,
        val screenOrientation: ScreenOrientation,
        val editorInputType: EditorInputType?,
    )
}
