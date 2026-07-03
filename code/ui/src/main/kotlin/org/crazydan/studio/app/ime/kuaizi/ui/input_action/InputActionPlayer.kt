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

package org.crazydan.studio.app.ime.kuaizi.ui.input_action

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.crazydan.studio.app.ime.kuaizi.engine.ImeIntent
import org.crazydan.studio.app.ime.kuaizi.engine.keyboard.InputKey
import org.crazydan.studio.app.ime.kuaizi.engine.input.InputWord
import org.crazydan.studio.app.ime.kuaizi.engine.keyboard.KeyGesture
import org.crazydan.studio.app.ime.kuaizi.engine.input_action.*
import org.crazydan.studio.app.ime.kuaizi.ui.keyboard.CandidateListLayoutState
import org.crazydan.studio.app.ime.kuaizi.ui.keyboard.InputListLayoutState
import org.crazydan.studio.app.ime.kuaizi.ui.keyboard.KeyLayoutState
import org.crazydan.studio.app.ime.kuaizi.ui.viewmodel.GestureFeedbackState
import org.crazydan.studio.app.ime.kuaizi.ui.viewmodel.KeyboardViewModel

/**
 * 输入动作播放器的状态模型，使用密封类定义五种互斥状态。
 *
 * 完整描述播放器的生命周期：
 * Idle → Ready → Playing ↔ Paused → Finished → Idle
 */
sealed class InputActionPlayerState {
    /** 空闲状态：未加载任何脚本 */
    data object Idle : InputActionPlayerState()

    /** 就绪状态：脚本已加载，等待播放 */
    data class Ready(val script: InputActionScript) : InputActionPlayerState()

    /** 播放中：包含当前执行到第几个动作和总动作数 */
    data class Playing(
        val currentIndex: Int,
        val totalActions: Int,
    ) : InputActionPlayerState()

    /** 暂停中：保持当前进度信息 */
    data class Paused(
        val currentIndex: Int,
        val totalActions: Int,
    ) : InputActionPlayerState()

    /** 播放完成 */
    data object Finished : InputActionPlayerState()
}

/**
 * 输入动作播放器。
 *
 * 负责按照预定义的脚本自动执行一系列输入动作，用于教学演示和辅助输入两种场景。
 * 通过 [positionResolver] 将逻辑动作映射到界面坐标，
 * 通过 [feedbackState] 写入视觉反馈，
 * 通过 [viewModel.handleIntent] 发射引擎意图。
 *
 * @param viewModel 键盘视图模型，用于发射 ImeIntent
 * @param feedbackState 手势反馈状态，用于写入视觉反馈
 * @param positionResolver 位置解析器，将语义标识解析为归一化坐标
 * @param scope 协程作用域，通常来自 viewModelScope
 */
class InputActionPlayer(
    private val viewModel: KeyboardViewModel,
    private val feedbackState: GestureFeedbackState,
    private val positionResolver: InputActionPositionResolver,
    private val scope: CoroutineScope,
) {
    private val _playbackState = MutableStateFlow<InputActionPlayerState>(InputActionPlayerState.Idle)
    val playbackState: StateFlow<InputActionPlayerState> = _playbackState.asStateFlow()

    /** Row 1（CandidateListPanel）的指示器状态 */
    private val _row1IndicatorState = MutableStateFlow<InputActionFingerIndicator?>(null)
    val row1IndicatorState: StateFlow<InputActionFingerIndicator?> = _row1IndicatorState.asStateFlow()

    /** Row 2（InputListPanel）的指示器状态 */
    private val _row2IndicatorState = MutableStateFlow<InputActionFingerIndicator?>(null)
    val row2IndicatorState: StateFlow<InputActionFingerIndicator?> = _row2IndicatorState.asStateFlow()

    private var job: Job? = null

    /** 当前加载的脚本 */
    var script: InputActionScript? = null
        private set

    private val frameTimer = FrameTimer(scope)

    /** 加载脚本 */
    fun load(script: InputActionScript) {
        this.script = script
        _playbackState.value = InputActionPlayerState.Ready(script)
    }

    /** 开始播放 */
    fun play() {
        val s = script ?: return
        job?.cancel()
        _playbackState.value = InputActionPlayerState.Playing(0, s.actions.size)

        if (s.actions.isEmpty()) {
            _playbackState.value = InputActionPlayerState.Finished
            return
        }

        val totalDuration = s.totalDuration.coerceAtLeast(1L)
        // 使用 FrameTimer 驱动每帧回调
        frameTimer.start(
            durationMs = totalDuration,
            onFrame = { progress ->
                val currentIndex = (progress * s.actions.size).toInt().coerceAtMost(s.actions.size - 1)
                _playbackState.value = InputActionPlayerState.Playing(currentIndex, s.actions.size)

                // 执行从 0 到当前索引的所有未处理动作
                var i = 0
                while (i <= currentIndex && i < s.actions.size) {
                    processAction(s.actions[i], s.actions.getOrNull(i + 1))
                    i++
                }
            },
            onComplete = {
                _playbackState.value = InputActionPlayerState.Finished
                feedbackState.clear()
            },
        )
    }

    /** 暂停播放 */
    fun pause() {
        val current = _playbackState.value
        if (current is InputActionPlayerState.Playing) {
            _playbackState.value = InputActionPlayerState.Paused(
                currentIndex = current.currentIndex,
                totalActions = current.totalActions,
            )
            job?.cancel()
        }
    }

    /** 恢复播放 */
    fun resume() {
        play()
    }

    /** 停止播放并清理所有反馈状态 */
    fun stop() {
        job?.cancel()
        _playbackState.value = InputActionPlayerState.Idle
        feedbackState.clear()
        _row1IndicatorState.value = null
        _row2IndicatorState.value = null
    }

    /** 设置播放速度 */
    fun setSpeed(speed: Float) {
        val currentState = _playbackState.value
        if (currentState is InputActionPlayerState.Playing) {
            stop()
            play()
        }
    }

    /** 处理单个输入动作 */
    private fun processAction(action: InputAction, nextAction: InputAction?) {
        when (action) {
            is InputAction.KeyDown -> {
                // 解析按键位置，设置手指指示器为按下状态
                val pos = positionResolver.resolve(action.key)
                if (pos != null) {
                    feedbackState.setFingerIndicator(
                        InputActionFingerIndicator(
                            position = pos,
                            pressed = true,
                            visible = true,
                        )
                    )
                }
                viewModel.handleIntent(ImeIntent.PressKey(action.key, KeyGesture.Tap))
            }

            is InputAction.SwipeTo -> {
                // 解析起止按键位置，生成平滑路径
                val fromPos = positionResolver.resolve(action.fromKey)
                val toPos = positionResolver.resolve(action.toKey)
                if (fromPos != null && toPos != null) {
                    val path = generateSmoothPath(fromPos, toPos, 10)
                    feedbackState.setTouchTrailPoints(path)
                    feedbackState.setFingerIndicator(
                        InputActionFingerIndicator(
                            position = toPos,
                            pressed = true,
                            visible = true,
                        )
                    )
                }
                viewModel.handleIntent(ImeIntent.PressKey(action.toKey, KeyGesture.Swipe))
            }

            is InputAction.KeyUp -> {
                // 设置手指指示器为抬起状态
                feedbackState.setFingerIndicator(
                    InputActionFingerIndicator(
                        position = positionResolver.resolve(action.key) ?: OffsetF.Zero,
                        pressed = false,
                        visible = true,
                    )
                )
            }

            is InputAction.SelectCandidate -> {
                // 解析候选词位置，更新 Row 1 指示器和手指指示器
                val pos = positionResolver.resolveCandidatePosition(action.candidateIndex)
                if (pos != null) {
                    feedbackState.setFingerIndicator(
                        InputActionFingerIndicator(
                            position = pos,
                            pressed = true,
                            visible = true,
                            clickAnimation = InputActionFingerIndicator.ClickAnimation.Pressing,
                        )
                    )
                    _row1IndicatorState.value = InputActionFingerIndicator(
                        position = pos,
                        pressed = true,
                        visible = true,
                    )
                }
                val word = InputWord.Pinyin(text = "", frequency = 1)
                viewModel.handleIntent(ImeIntent.SelectCandidate(word))
            }

            is InputAction.Wait -> { /* 等待动作，无操作 */ }

            is InputAction.SwitchKeyboard -> {
                viewModel.handleIntent(ImeIntent.SwitchKeyboard(action.targetType))
            }
        }
    }

    /** 生成起始点到目标点的平滑路径插值点 */
    private fun generateSmoothPath(from: OffsetF, to: OffsetF, steps: Int): List<OffsetF> {
        return (0..steps).map { i ->
            val t = i.toFloat() / steps
            InputActionPathInterpolator.interpolate(from, to, t)
        }
    }
}

/**
 * Compose 环境下的输入动作位置解析器。
 *
 * 通过读取 ViewModel 中缓存的布局状态，将按键标识和索引映射为归一化坐标。
 *
 * @param keyboardLayoutStateProvider 按键布局状态提供者
 * @param candidateLayoutStateProvider 候选栏布局状态提供者
 * @param inputListLayoutStateProvider 输入栏布局状态提供者
 */
class ComposeInputActionPositionResolver(
    private val keyboardLayoutStateProvider: () -> KeyLayoutState?,
    private val candidateLayoutStateProvider: () -> CandidateListLayoutState?,
    private val inputListLayoutStateProvider: () -> InputListLayoutState?,
) : InputActionPositionResolver {
    /** 解析按键位置：从 keyPositions 映射中查找按键中心坐标 */
    override fun resolve(key: InputKey): OffsetF? {
        val layout = keyboardLayoutStateProvider() ?: return null
        return layout.keyPositions[key]?.center
    }

    /** 解析候选词位置：从候选栏布局中查找指定索引项的归一化坐标 */
    override fun resolveCandidatePosition(index: Int): OffsetF? {
        val layout = candidateLayoutStateProvider() ?: return null
        val pos = layout.locateItem(index) ?: return null
        val size = layout.panelSize
        return OffsetF(
            x = (pos.x / size.width).coerceIn(0f, 1f),
            y = (pos.y / size.height).coerceIn(0f, 1f),
        )
    }

    /** 解析输入项位置：从输入栏布局中查找指定索引项的归一化坐标 */
    override fun resolveInputItemPosition(index: Int): OffsetF? {
        val layout = inputListLayoutStateProvider() ?: return null
        val pos = layout.locateItem(index) ?: return null
        val size = layout.panelSize
        return OffsetF(
            x = (pos.x / size.width).coerceIn(0f, 1f),
            y = (pos.y / size.height).coerceIn(0f, 1f),
        )
    }
}
