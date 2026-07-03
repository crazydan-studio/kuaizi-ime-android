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

package org.crazydan.studio.app.ime.kuaizi.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.crazydan.studio.app.ime.kuaizi.engine.*
import org.crazydan.studio.app.ime.kuaizi.engine.effect.ImeEffect
import org.crazydan.studio.app.ime.kuaizi.engine.input.InputWord
import org.crazydan.studio.app.ime.kuaizi.engine.keyboard.KeyGesture
import org.crazydan.studio.app.ime.kuaizi.engine.keyboard.KeyboardType
import org.crazydan.studio.app.ime.kuaizi.ui.AudioPlayer
import org.crazydan.studio.app.ime.kuaizi.ui.HapticPlayer
import org.crazydan.studio.app.ime.kuaizi.ui.keyboard.CandidateListLayoutState
import org.crazydan.studio.app.ime.kuaizi.ui.keyboard.InputListLayoutState
import org.crazydan.studio.app.ime.kuaizi.ui.keyboard.KeyLayoutState
import org.crazydan.studio.app.ime.kuaizi.ui.player.ComposeInputActionPositionResolver
import org.crazydan.studio.app.ime.kuaizi.ui.player.InputActionPlayer

/** 键盘布局模式，定义 Zone A 与 Zone B 的使用方式 */
sealed class KeyboardLayoutMode {
    /** 堆叠模式：所有组件集中在 Zone B，三层面板叠加共享同一空间 */
    data object Stacked : KeyboardLayoutMode()

    /**
     * 分离模式：输入区域占据 Zone B，按键展示区域占据 Zone A
     * @param zoneARatio Zone A 占屏幕高度的比例，默认 0.4
     */
    data class Separated(val zoneARatio: Float = 0.4f) : KeyboardLayoutMode()
}

/**
 * 键盘视图模型，UI 层的协调中心。
 *
 * 桥接 Compose UI 组件与 `:engine` 引擎：
 * - 将 UI 手势（[InputGesture]）转换为引擎意图（[ImeIntent]）
 * - 暴露引擎状态（[StateFlow]<[ImeState]>) 供 Compose 订阅
 * - 管理手势反馈状态（[GestureFeedbackState]）
 * - 管理运行时布局模式（[KeyboardLayoutMode]）
 * - 维护本地工具列表状态和弹出提示状态
 * - 提供输入动作播放器（[InputActionPlayer]）
 * - 缓存面板布局状态供播放器坐标解析
 * - 管理感官反馈播放（[AudioPlayer] / [HapticPlayer]）
 */
class KeyboardViewModel(
    private val engine: ImeEngine,
    private val audioPlayer: AudioPlayer? = null,
    private val hapticPlayer: HapticPlayer? = null,
) : ViewModel() {

    // ==================== 状态暴露 ====================

    /** 引擎状态，供 Compose 订阅 */
    val state: StateFlow<ImeState> = engine.state

    /** 当前 ImeConfig 快照，便于 UI 组件快速访问 */
    val config: ImeConfig get() = state.value.config

    /** 手势反馈状态，独立于 ImeState，使用归一化坐标 */
    val feedbackState = GestureFeedbackState()

    // ==================== 布局模式 ====================

    private val _layoutMode = MutableStateFlow<KeyboardLayoutMode>(KeyboardLayoutMode.Stacked)
    val layoutMode: StateFlow<KeyboardLayoutMode> = _layoutMode.asStateFlow()

    // ==================== 弹出提示 ====================

    private val _popupTipState = MutableStateFlow<PopupTipState?>(null)
    val popupTipState: StateFlow<PopupTipState?> = _popupTipState.asStateFlow()

    // ==================== 工具列表 ====================

    private val _toolListState = MutableStateFlow(ToolListState(emptyList()))
    val toolListState: StateFlow<ToolListState> = _toolListState.asStateFlow()

    /** 弹出提示自动消失定时器 Job，确保同时只显示一条提示 */
    private var popupTipDismissJob: Job? = null

    // ==================== 布局状态缓存 ====================
    // 供 ComposeInputActionPositionResolver 坐标解析使用

    private var _currentKeyLayoutState: KeyLayoutState? = null
    private var _currentCandidateLayoutState: CandidateListLayoutState? = null
    private var _currentInputListLayoutState: InputListLayoutState? = null

    // ==================== 输入动作播放器 ====================

    /** 输入动作播放器，通过 [viewModel.handleIntent] 驱动引擎状态转换 */
    val actionPlayer = InputActionPlayer(
        viewModel = this,
        feedbackState = feedbackState,
        positionResolver = ComposeInputActionPositionResolver(
            keyboardLayoutStateProvider = { _currentKeyLayoutState },
            candidateLayoutStateProvider = { _currentCandidateLayoutState },
            inputListLayoutStateProvider = { _currentInputListLayoutState },
        ),
        scope = viewModelScope,
    )

    /** 是否正在输入，由 inputList.hasPending 直接派生 */
    val isInputting: Boolean get() = state.value.inputList.hasPending

    init {
        launchEffectCollection()
        launchToolListCollection()
    }

    /** 订阅引擎副作用通道，处理弹出提示和感官反馈 */
    private fun launchEffectCollection() {
        viewModelScope.launch {
//            engine.effect
//                .channelFlow {
//                    // 将 SharedFlow 转为 conflated Channel
//                    // 感官反馈使用 trySend 非阻塞发送，丢弃旧事件
//                    // 弹出提示使用 send 确保不丢失
//                    engine.effect.collect { effect ->
//                        when (effect) {
//                            is ImeEffect.PlayAudio, is ImeEffect.PlayHaptic -> {
//                                trySend(effect)
//                            }
//                            is ImeEffect.PopupTip -> {
//                                send(effect)
//                            }
//                        }
//                    }
//                }
//                .collect { effect ->
//                    processEffect(effect)
//                }
        }
    }

    /** 订阅引擎状态变更，动态更新工具列表 */
    private fun launchToolListCollection() {
        viewModelScope.launch {
            engine.state
                .map { state -> state.keyboard.type }
                .distinctUntilChanged()
                .collect { keyboardType ->
                    _toolListState.value = computeToolList(keyboardType)
                }
        }
    }

    /** 处理引擎副作用 */
    private fun processEffect(effect: ImeEffect) {
        when (effect) {
            is ImeEffect.PopupTip.Message -> {
                _popupTipState.value = PopupTipState.Message(
                    message = effect.message,
                    timeoutMs = effect.timeoutMs,
                )
                dismissPopupTipAfter(effect.timeoutMs)
            }
            is ImeEffect.PopupTip.Action -> {
                _popupTipState.value = PopupTipState.Action(
                    message = effect.message,
                    actionLabel = effect.actionLabel,
                    action = effect.action,
                    persistent = effect.persistent,
                    timeoutMs = effect.timeoutMs,
                )
                if (!effect.persistent) {
                    dismissPopupTipAfter(effect.timeoutMs)
                }
            }
            is ImeEffect.PlayAudio -> {
                // 配置启用且播放器可用时才播放音效
                if (state.value.config.ui.audioFeedbackEnabled && audioPlayer != null) {
                    audioPlayer.play(effect.type)
                }
            }
            is ImeEffect.PlayHaptic -> {
                if (state.value.config.ui.hapticFeedbackEnabled && hapticPlayer != null) {
                    hapticPlayer.play(effect.type)
                }
            }
        }
    }

    /** 启动自动消失定时器，新的定时器会取消前一个 */
    private fun dismissPopupTipAfter(timeoutMs: Long) {
        popupTipDismissJob?.cancel()
        popupTipDismissJob = viewModelScope.launch {
            delay(timeoutMs)
            _popupTipState.value = null
        }
    }

    // ==================== 手势与意图处理 ====================

    /** 处理输入手势：转换为 [ImeIntent] 后委托引擎处理 */
    fun handleGesture(gesture: InputGesture) {
        val intent = gestureToIntent(gesture)
        engine.handleIntent(intent)
    }

    /** 直接发送 [ImeIntent] 到引擎 */
    fun handleIntent(intent: ImeIntent) {
        engine.handleIntent(intent)
    }

    /** 将 [InputGesture] 转换为 [ImeIntent] */
    private fun gestureToIntent(gesture: InputGesture): ImeIntent {
        return when (gesture) {
            is InputGesture.Tap -> ImeIntent.PressKey(gesture.key, KeyGesture.Tap)
            is InputGesture.LongPress -> ImeIntent.PressKey(gesture.key, KeyGesture.LongPress)
            is InputGesture.Swipe -> ImeIntent.PressKey(gesture.endKey, KeyGesture.Swipe)
            is InputGesture.Flip -> ImeIntent.PressKey(
                gesture.startKey,
                KeyGesture.Flip,
            )
            is InputGesture.CandidateTap -> {
                val candidates = state.value.candidateList.candidates
                val idx = gesture.candidateIndex
                if (idx in candidates.indices) {
                    ImeIntent.SelectCandidate(candidates[idx])
                } else {
                    // 索引越界时用空词兜底
                    ImeIntent.SelectCandidate(
                        InputWord.Pinyin(text = "", frequency = 1)
                    )
                }
            }
        }
    }

    // ==================== 配置与布局 ====================

    /** 更新运行时配置，委托引擎执行 */
    fun updateConfig(transform: (ImeConfig) -> ImeConfig) {
        engine.updateConfig(transform)
    }

    /** 设置键盘布局模式 */
    fun setKeyboardLayoutMode(mode: KeyboardLayoutMode) {
        _layoutMode.value = mode
    }

    /** 更新按键布局状态缓存 */
    fun updateKeyLayoutState(state: KeyLayoutState) {
        _currentKeyLayoutState = state
    }

    /** 更新候选栏布局状态缓存 */
    fun updateCandidateLayoutState(state: CandidateListLayoutState) {
        _currentCandidateLayoutState = state
    }

    /** 更新输入栏布局状态缓存 */
    fun updateInputListLayoutState(state: InputListLayoutState) {
        _currentInputListLayoutState = state
    }

    /** 根据键盘类型动态计算工具列表 */
    private fun computeToolList(keyboardType: KeyboardType): ToolListState {
        val tools = mutableListOf<ToolItem>()
        // 基本编辑功能键
        tools.add(ToolItem(label = "全选", disabled = false))
        tools.add(ToolItem(label = "复制", disabled = false))
        tools.add(ToolItem(label = "粘贴", disabled = false))
        tools.add(ToolItem(label = "剪切", disabled = false))
        // 拼音/拉丁键盘额外提供设置、切换和关闭功能
        when (keyboardType) {
            KeyboardType.Pinyin, KeyboardType.Latin -> {
                tools.add(ToolItem(label = "设置"))
                tools.add(ToolItem(label = "切换输入法"))
                tools.add(ToolItem(label = "关闭键盘"))
            }
            else -> {}
        }
        return ToolListState(tools = tools)
    }

    // ==================== 生命周期 ====================

    override fun onCleared() {
        super.onCleared()
        actionPlayer.stop()
        feedbackState.clear()
    }

    /**
     * ViewModel 工厂，用于注入预创建的 [ImeEngine] 和播放器。
     *
     * 引擎和播放器由 `:app` 模块在 `IMEService.onCreate()` 中创建，
     * 通过此工厂注入 ViewModel。
     */
    class Factory(
        private val engine: ImeEngine,
        private val audioPlayer: AudioPlayer? = null,
        private val hapticPlayer: HapticPlayer? = null,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return KeyboardViewModel(engine, audioPlayer, hapticPlayer) as T
        }
    }
}
