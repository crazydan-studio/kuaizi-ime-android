package org.crazydan.studio.app.ime.kuaizi.ui.player

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.crazydan.studio.app.ime.kuaizi.engine.ImeIntent
import org.crazydan.studio.app.ime.kuaizi.engine.InputWord
import org.crazydan.studio.app.ime.kuaizi.engine.domain.InputKey
import org.crazydan.studio.app.ime.kuaizi.engine.domain.KeyGesture
import org.crazydan.studio.app.ime.kuaizi.engine.domain.KeyboardType
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
    private var script: InputActionScript? = null

    fun load(script: InputActionScript) {
        this.script = script
        _playbackState.value = InputActionPlayerState.Ready(script)
    }

    fun play() {
        val s = script ?: return
        job?.cancel()
        job = scope.launch {
            _playbackState.value = InputActionPlayerState.Playing(0, s.actions.size)
            
            val frameNanos = 16_666_667L // ~60fps
            var currentIndex = 0
            val startTime = System.nanoTime()

            while (currentIndex < s.actions.size) {
                val frameTime = System.nanoTime()
                val elapsed = frameTime - startTime

                // Process due actions
                while (currentIndex < s.actions.size) {
                    val action = s.actions[currentIndex]
                    val actionTimeMs = action.startTime
                    val actionTimeNanos = actionTimeMs * 1_000_000L

                    if (elapsed < actionTimeNanos) break

                    processAction(action, s.actions.getOrNull(currentIndex + 1))
                    currentIndex++
                }

                _playbackState.value = InputActionPlayerState.Playing(currentIndex, s.actions.size)

                if (currentIndex >= s.actions.size) {
                    _playbackState.value = InputActionPlayerState.Finished
                    feedbackState.clear()
                    break
                }

                // Wait for next frame
                val nextFrameTime = ((elapsed / frameNanos) + 1) * frameNanos
                val delayNanos = nextFrameTime - (System.nanoTime() - startTime)
                if (delayNanos > 0) {
                    delay(delayNanos / 1_000_000)
                }
            }
        }
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
        // restart from paused position
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
        // adjust timing, re-run if playing
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
                val key = try { InputKey.valueOf(action.key) } catch (_: Exception) { InputKey.Char }
                viewModel.handleIntent(ImeIntent.PressKey(key, KeyGesture.Tap))
            }
            is InputAction.SwipeTo -> {
                val fromPos = positionResolver.resolve(action.fromKey)
                val toPos = positionResolver.resolve(action.toKey)
                if (fromPos != null && toPos != null) {
                    val interpolator = InputActionPathInterpolator()
                    val path = interpolator.interpolate(fromPos, toPos, steps = 10)
                    feedbackState.setTouchTrailPoints(path)
                    feedbackState.setFingerIndicator(
                        InputActionFingerIndicator(
                            position = toPos,
                            pressed = true,
                            visible = true,
                        )
                    )
                }
                val key = try { InputKey.valueOf(action.toKey) } catch (_: Exception) { InputKey.Char }
                viewModel.handleIntent(ImeIntent.PressKey(key, KeyGesture.Swipe))
            }
            is InputAction.KeyUp -> {
                feedbackState.setFingerIndicator(
                    InputActionFingerIndicator(
                        position = positionResolver.resolve(action.key) ?: OffsetF(),
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
                            clickAnimation = ClickAnimation(
                                progress = 1f,
                                maxRadius = 15f,
                                color = 0xFF1976D2,
                            ),
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
                val type = try { KeyboardType.valueOf(action.targetType) } catch (_: Exception) { KeyboardType.Pinyin }
                viewModel.handleIntent(ImeIntent.SwitchKeyboard(type))
            }
        }
    }
}

class ComposeInputActionPositionResolver : InputActionPositionResolver {
    private var _currentKeyLayoutState: KeyLayoutState? = null
    private var _currentCandidateLayout: CandidateListLayoutState? = null
    private var _currentInputListLayout: InputListLayoutState? = null

    fun updateKeyLayout(state: KeyLayoutState) {
        _currentKeyLayoutState = state
    }

    fun updateCandidateLayout(state: CandidateListLayoutState?) {
        _currentCandidateLayout = state
    }

    fun updateInputListLayout(state: InputListLayoutState?) {
        _currentInputListLayout = state
    }

    override fun resolve(key: String): OffsetF? {
        // Simple heuristic: position in center of panel
        return OffsetF(0.5f, 0.5f)
    }

    override fun resolveCandidatePosition(index: Int): OffsetF? {
        val layout = _currentCandidateLayout ?: return null
        val pos = layout.locateItem(index) ?: return null
        val size = layout.panelSize
        return OffsetF(
            x = (pos.x / size.width).coerceIn(0f, 1f),
            y = (pos.y / size.height).coerceIn(0f, 1f),
        )
    }

    override fun resolveInputItemPosition(index: Int): OffsetF? {
        val layout = _currentInputListLayout ?: return null
        val pos = layout.locateItem(index) ?: return null
        val size = layout.panelSize
        return OffsetF(
            x = (pos.x / size.width).coerceIn(0f, 1f),
            y = (pos.y / size.height).coerceIn(0f, 1f),
        )
    }
}
