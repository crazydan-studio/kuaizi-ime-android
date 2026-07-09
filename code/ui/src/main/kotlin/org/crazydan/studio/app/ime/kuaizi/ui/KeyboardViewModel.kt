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

package org.crazydan.studio.app.ime.kuaizi.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.crazydan.studio.app.ime.kuaizi.engine.ImeConfig
import org.crazydan.studio.app.ime.kuaizi.engine.ImeEffect
import org.crazydan.studio.app.ime.kuaizi.engine.ImeEngine
import org.crazydan.studio.app.ime.kuaizi.engine.ImeIntent
import org.crazydan.studio.app.ime.kuaizi.engine.ImeState
import org.crazydan.studio.app.ime.kuaizi.engine.ToolItem
import org.crazydan.studio.app.ime.kuaizi.engine.ToolListState
import org.crazydan.studio.app.ime.kuaizi.engine.input.InputWord
import org.crazydan.studio.app.ime.kuaizi.engine.keyboard.KeyboardType
import org.crazydan.studio.app.ime.kuaizi.ui.domain.AudioType
import org.crazydan.studio.app.ime.kuaizi.ui.domain.HapticType
import org.crazydan.studio.app.ime.kuaizi.ui.input_action.ComposeInputActionPositionResolver
import org.crazydan.studio.app.ime.kuaizi.ui.input_action.InputActionPlayer
import org.crazydan.studio.app.ime.kuaizi.ui.keyboard.CandidateListLayoutState
import org.crazydan.studio.app.ime.kuaizi.ui.keyboard.InputListLayoutState
import org.crazydan.studio.app.ime.kuaizi.ui.keyboard.KeyLayoutState
import org.crazydan.studio.app.ime.kuaizi.ui.keyboard.KeyboardLayoutMode
import org.crazydan.studio.app.ime.kuaizi.ui.viewmodel.GestureFeedbackState
import org.crazydan.studio.app.ime.kuaizi.ui.viewmodel.InputGesture
import org.crazydan.studio.app.ime.kuaizi.ui.viewmodel.PopupTipState

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
 * - 管理感官反馈播放
 */
class KeyboardViewModel(private val option: Option) : ViewModel() {
    private val engine = option.engine

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

    // TODO 需支持 Message 与 Action 共存的情况
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

    /** 输入动作播放器，通过 [handleIntent] 驱动引擎状态转换 */
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

    // -----------------------------------------------------------------------

    init {
        launchEffectCollection()
        launchToolListCollection()
    }

    override fun onCleared() {
        super.onCleared()

        actionPlayer.stop()
        feedbackState.clear()
    }

    // -----------------------------------------------------------------------

    /** 处理输入手势：播放反馈、转换为 [ImeIntent] 后委托引擎处理 */
    fun handleGesture(gesture: InputGesture) {
        // 处理按键弹出提示
        processKeyPopupTip(gesture)

        // 处理音效和触觉反馈
        processFeedback(gesture)

        // 转换手势为意图并发送给引擎
        val intent = gestureToIntent(gesture)
        launchHandleEngineIntent(intent)
    }

    /** 处理 [ImeIntent] */
    fun handleIntent(intent: ImeIntent) =
        when (intent) {
            // <<<<<<<<<<<<<<<<<<<<<<
            // 直接由工具栏按钮触发，不需要在 engine 中中转
            is ImeIntent.SwitchIme ->
                option.switchIme?.invoke()

            is ImeIntent.Keyboard.Close ->
                option.closeKeyboard?.invoke()
            // >>>>>>>>>>>>>>>>>>>

            else ->
                launchHandleEngineIntent(intent)
        }

    // -----------------------------------------------------------------------

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

    // -----------------------------------------------------------------------

    /** 订阅引擎副作用通道 */
    private fun launchEffectCollection() {
        viewModelScope.launch {
            engine.effect.collect { effect ->
                processEffect(effect)
            }
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

    private fun launchHandleEngineIntent(intent: ImeIntent) {
        viewModelScope.launch {
            engine.handleIntent(intent)
        }
    }

    // ----------------------------------------------------------------

    /** 处理 [ImeEffect] */
    private fun processEffect(effect: ImeEffect) {
        when (effect) {
            is ImeEffect.PopupTip.Message -> {
                _popupTipState.value = PopupTipState.Message(
                    message = effect.message,
                    timeout = effect.timeout,
                ).apply {
                    dismissPopupTipAfter(timeout)
                }
            }

            is ImeEffect.PopupTip.Action -> {
                _popupTipState.value = PopupTipState.Action(
                    message = effect.message,
                    actionLabel = effect.actionLabel,
                    action = effect.action,
                    persistent = effect.persistent,
                    timeout = effect.timeout,
                ).apply {
                    if (!persistent) {
                        dismissPopupTipAfter(timeout)
                    }
                }
            }
        }
    }

    private fun processFeedback(gesture: InputGesture) =
        when (gesture) {
            is InputGesture.Tap -> {
                playAudio(AudioType.SingleTick)
                playHaptic(HapticType.LightTap)
            }

            is InputGesture.Flip -> {
                playAudio(AudioType.PageFlip)
            }

            else -> {}
        }

    private fun processKeyPopupTip(gesture: InputGesture) {
        if (!config.ui.keyPopupTipsEnabled) return

        when (gesture) {
            is InputGesture.Tap -> {
                _popupTipState.value = PopupTipState.Key(
                    value = gesture.key.toString(),
                ).apply {
                    dismissPopupTipAfter(timeout)
                }
            }

            is InputGesture.LongPress -> {
                // TODO 长按按键，一直显示按键字符，直到释放
                _popupTipState.value = PopupTipState.Key(
                    value = gesture.key.toString(),
                ).apply {
                    dismissPopupTipAfter(timeout)
                }
            }

            else -> {}
        }
    }

    /** 启动自动消失定时器，新的定时器会取消前一个 */
    private fun dismissPopupTipAfter(timeout: Long) {
        popupTipDismissJob?.cancel()

        popupTipDismissJob = viewModelScope.launch {
            delay(timeout)
            _popupTipState.value = null
        }
    }

    // ---------------------------------------------------------------------------

    /** 将 [InputGesture] 转换为 [ImeIntent] */
    private fun gestureToIntent(gesture: InputGesture): ImeIntent =
        when (gesture) {
            is InputGesture.Press ->
                if (gesture.released)
                    ImeIntent.OnKeyboard.Press.End(gesture.key)
                else ImeIntent.OnKeyboard.Press.Begin(gesture.key)

            is InputGesture.LongPress ->
                if (gesture.released)
                    ImeIntent.OnKeyboard.LongPress.End(gesture.key)
                else if (gesture.tick > 0)
                    ImeIntent.OnKeyboard.LongPress.Hold(
                        key = gesture.key,
                        tick = gesture.tick,
                    )
                else ImeIntent.OnKeyboard.LongPress.Begin(gesture.key)

            is InputGesture.Tap ->
                ImeIntent.OnKeyboard.Tap(key = gesture.key, tick = gesture.tick)

            is InputGesture.Swipe ->
                if (gesture.released)
                    ImeIntent.OnKeyboard.Swipe.End(gesture.key)
                else if (gesture.motion != null)
                    ImeIntent.OnKeyboard.Swipe.Moving(
                        key = gesture.key,
                        motion = gesture.motion,
                    )
                else if (gesture.tick > 0)
                    ImeIntent.OnKeyboard.Swipe.Hold(
                        key = gesture.key,
                        tick = gesture.tick,
                    )
                else ImeIntent.OnKeyboard.Swipe.Begin(gesture.key)

            is InputGesture.Flip ->
                ImeIntent.OnKeyboard.Flip(key = gesture.key, motion = gesture.motion)

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

    // ----------------------------------------------------------------

    private fun playAudio(type: AudioType) {
        if (config.ui.audioFeedbackEnabled) {
            option.playAudio?.invoke(type)
        }
    }

    private fun playHaptic(type: HapticType) {
        if (config.ui.hapticFeedbackEnabled) {
            option.playHaptic?.invoke(type)
        }
    }

    // -----------------------------------------------------------------------

    /**
     * ViewModel 工厂：
     * ```kotlin
     * private val viewModel: KeyboardViewModel by viewModels {
     *     KeyboardViewModel.Factory(
     *         KeyboardViewModel.Option(engine)
     *     )
     * }
     * ```
     */
    class Factory(private val option: Option) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            KeyboardViewModel(option) as T
    }

    data class Option(
        val engine: ImeEngine,
        //
        val playAudio: ((type: AudioType) -> Unit)? = null,
        val playHaptic: ((type: HapticType) -> Unit)? = null,
        //
        val switchIme: (() -> Unit)? = null,
        val closeKeyboard: (() -> Unit)? = null,
    )
}