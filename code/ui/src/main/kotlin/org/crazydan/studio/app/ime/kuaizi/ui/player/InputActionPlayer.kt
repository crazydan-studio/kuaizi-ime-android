package org.crazydan.studio.app.ime.kuaizi.ui.player

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.crazydan.studio.app.ime.kuaizi.engine.ImeIntent
import org.crazydan.studio.app.ime.kuaizi.engine.domain.InputKey
import org.crazydan.studio.app.ime.kuaizi.engine.domain.KeyGesture
import org.crazydan.studio.app.ime.kuaizi.engine.input_action.*
import org.crazydan.studio.app.ime.kuaizi.ui.viewmodel.GestureFeedbackState
import org.crazydan.studio.app.ime.kuaizi.ui.viewmodel.KeyboardViewModel

sealed class InputActionPlayerState {
    data object Idle : InputActionPlayerState()
    data class Ready(val script: InputActionScript) : InputActionPlayerState()
    data class Playing(
        val currentIndex: Int,
        val totalActions: Int,
    ) : InputActionPlayerState()

    data class Paused(
        val currentIndex: Int,
        val totalActions: Int,
    ) : InputActionPlayerState()

    data object Finished : InputActionPlayerState()
}

class InputActionPlayer(
    private val viewModel: KeyboardViewModel,
    private val feedbackState: GestureFeedbackState,
    private val positionResolver: InputActionPositionResolver,
    private val scope: CoroutineScope,
) {
    private val _playbackState = MutableStateFlow<InputActionPlayerState>(InputActionPlayerState.Idle)
    val playbackState: StateFlow<InputActionPlayerState> = _playbackState.asStateFlow()

    private val _row1IndicatorState = MutableStateFlow<InputActionFingerIndicator?>(null)
    val row1IndicatorState: StateFlow<InputActionFingerIndicator?> = _row1IndicatorState.asStateFlow()

    private val _row2IndicatorState = MutableStateFlow<InputActionFingerIndicator?>(null)
    val row2IndicatorState: StateFlow<InputActionFingerIndicator?> = _row2IndicatorState.asStateFlow()

    private var job: Job? = null
    var script: InputActionScript? = null
        private set
    private val frameTimer = FrameTimer(scope)

    fun load(script: InputActionScript) {
        this.script = script
        _playbackState.value = InputActionPlayerState.Ready(script)
    }

    fun play() {
        val s = script ?: return
        job?.cancel()
        _playbackState.value = InputActionPlayerState.Playing(0, s.actions.size)

        if (s.actions.isEmpty()) {
            _playbackState.value = InputActionPlayerState.Finished
            return
        }

        val totalDuration = s.totalDuration.coerceAtLeast(1L)
        frameTimer.start(
            durationMs = totalDuration,
            onFrame = { progress ->
                val currentIndex = (progress * s.actions.size).toInt().coerceAtMost(s.actions.size - 1)
                _playbackState.value = InputActionPlayerState.Playing(currentIndex, s.actions.size)

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

    fun resume() {
        play()
    }

    fun stop() {
        job?.cancel()
        _playbackState.value = InputActionPlayerState.Idle
        feedbackState.clear()
        _row1IndicatorState.value = null
        _row2IndicatorState.value = null
    }

    fun setSpeed(speed: Float) {
        // Adjust timing, re-run if playing
        val currentState = _playbackState.value
        if (currentState is InputActionPlayerState.Playing) {
            stop()
            play()
        }
    }

    private fun processAction(action: InputAction, nextAction: InputAction?) {
        when (action) {
            is InputAction.KeyDown -> {
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
                feedbackState.setFingerIndicator(
                    InputActionFingerIndicator(
                        position = positionResolver.resolve(action.key) ?: OffsetF.Zero,
                        pressed = false,
                        visible = true,
                    )
                )
            }

            is InputAction.SelectCandidate -> {
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
                val word = InputWord.Pinyin(text = "", spell = "")
                viewModel.handleIntent(ImeIntent.SelectCandidate(word))
            }

            is InputAction.Wait -> { /* no-op */ }

            is InputAction.SwitchKeyboard -> {
                viewModel.handleIntent(ImeIntent.SwitchKeyboard(action.targetType))
            }
        }
    }

    private fun generateSmoothPath(from: OffsetF, to: OffsetF, steps: Int): List<OffsetF> {
        return (0..steps).map { i ->
            val t = i.toFloat() / steps
            InputActionPathInterpolator.interpolate(from, to, t)
        }
    }
}

class ComposeInputActionPositionResolver(
    private val keyboardLayoutStateProvider: () -> KeyLayoutState?,
    private val candidateLayoutStateProvider: () -> CandidateListLayoutState?,
    private val inputListLayoutStateProvider: () -> InputListLayoutState?,
) : InputActionPositionResolver {
    override fun resolve(key: InputKey): OffsetF? {
        val layout = keyboardLayoutStateProvider() ?: return null
        return layout.keyPositions[key]?.center
    }

    override fun resolveCandidatePosition(index: Int): OffsetF? {
        val layout = candidateLayoutStateProvider() ?: return null
        val pos = layout.locateItem(index) ?: return null
        val size = layout.panelSize
        return OffsetF(
            x = (pos.x / size.width).coerceIn(0f, 1f),
            y = (pos.y / size.height).coerceIn(0f, 1f),
        )
    }

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
