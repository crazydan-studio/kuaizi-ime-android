package org.crazydan.studio.app.ime.kuaizi.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.crazydan.studio.app.ime.kuaizi.engine.*
import org.crazydan.studio.app.ime.kuaizi.engine.domain.*
import org.crazydan.studio.app.ime.kuaizi.engine.input_action.*
import org.crazydan.studio.app.ime.kuaizi.ui.keyboard.KeyLayoutState

sealed class PopupTipState {
    data class Message(
        val message: String,
        val timeoutMs: Long = 3000L,
    ) : PopupTipState()

    data class Action(
        val message: String,
        val actionLabel: String,
        val action: ImeIntent,
        val timeoutMs: Long = 5000L,
    ) : PopupTipState()
}

sealed class KeyboardLayoutMode {
    data object Stacked : KeyboardLayoutMode()
    data class Separated(val zoneARatio: Float = 0.4f) : KeyboardLayoutMode()
}

class KeyboardViewModel(
    private val engine: ImeEngine,
    private val feedbackState: GestureFeedbackState = GestureFeedbackState(),
) : ViewModel() {

    val state: StateFlow<ImeState> = engine.state
    val config: ImeConfig get() = state.value.config

    private val _layoutMode = MutableStateFlow<KeyboardLayoutMode>(KeyboardLayoutMode.Stacked)
    val layoutMode: StateFlow<KeyboardLayoutMode> = _layoutMode.asStateFlow()

    val feedbackStateRef: GestureFeedbackState get() = feedbackState

    private val _popupTipState = MutableStateFlow<PopupTipState?>(null)
    val popupTipState: StateFlow<PopupTipState?> = _popupTipState.asStateFlow()

    private val _toolListState = MutableStateFlow<ToolListState>(ToolListState())
    val toolListState: StateFlow<ToolListState> = _toolListState.asStateFlow()

    private val _actionPlayer = InputActionPlayer(
        feedbackState = feedbackState,
        positionResolver = ComposeInputActionPositionResolver(),
        scope = viewModelScope,
    )
    val actionPlayer: InputActionPlayer get() = _actionPlayer

    val isInputting: Boolean get() = state.value.inputList.hasPending

    private var currentKeyLayoutState = MutableStateFlow(KeyLayoutState())
    private var currentCandidateLayoutState: Any? = null
    private var currentInputListLayoutState: Any? = null

    init {
        launchEffectCollection()
        launchToolListCollection()
    }

    private fun launchEffectCollection() {
        viewModelScope.launch {
            engine.effect
                .channelFlow {
                    engine.effect.collect { effect ->
                        when (effect) {
                            is ImeEffect.PlayAudio, is ImeEffect.PlayHaptic -> {
                                trySend(effect)
                            }
                            is ImeEffect.PopupTip -> {
                                send(effect)
                            }
                        }
                    }
                }
                .collect { effect ->
                    processEffect(effect)
                }
        }
    }

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

    private fun processEffect(effect: ImeEffect) {
        when (effect) {
            is ImeEffect.PopupTip.Message -> {
                _popupTipState.value = PopupTipState.Message(
                    message = effect.message,
                    timeoutMs = effect.timeoutMs,
                )
            }
            is ImeEffect.PopupTip.Action -> {
                _popupTipState.value = PopupTipState.Action(
                    message = effect.message,
                    actionLabel = effect.actionLabel,
                    action = effect.action,
                    timeoutMs = effect.timeoutMs,
                )
            }
            is ImeEffect.PlayAudio -> {
                if (config.ui.audioFeedbackEnabled) {
                    // delegate to audio player
                }
            }
            is ImeEffect.PlayHaptic -> {
                if (config.ui.hapticFeedbackEnabled) {
                    // delegate to haptic player
                }
            }
        }
    }

    fun handleGesture(gesture: InputGesture) {
        val intent = gestureToIntent(gesture)
        engine.handleIntent(intent)
    }

    fun handleIntent(intent: ImeIntent) {
        engine.handleIntent(intent)
    }

    private fun gestureToIntent(gesture: InputGesture): ImeIntent {
        return when (gesture) {
            is InputGesture.Tap -> ImeIntent.PressKey(gesture.key, KeyGesture.Tap)
            is InputGesture.LongPress -> ImeIntent.PressKey(gesture.key, KeyGesture.LongPress)
            is InputGesture.Swipe -> ImeIntent.PressKey(gesture.endKey, KeyGesture.Swipe)
            is InputGesture.Flip -> ImeIntent.PressKey(
                gesture.startKey,
                KeyGesture.Flip(gesture.direction),
            )
            is InputGesture.CandidateTap -> {
                val candidates = state.value.candidateList.candidates
                val idx = gesture.candidateIndex
                if (idx in candidates.indices) {
                    ImeIntent.SelectCandidate(candidates[idx])
                } else {
                    ImeIntent.SelectCandidate(
                        InputWord.Pinyin(text = "", spell = "")
                    )
                }
            }
        }
    }

    fun updateConfig(transform: (ImeConfig) -> ImeConfig) {
        engine.updateConfig(transform)
    }

    fun setKeyboardLayoutMode(mode: KeyboardLayoutMode) {
        _layoutMode.value = mode
    }

    fun updateKeyLayoutState(state: KeyLayoutState) {
        currentKeyLayoutState.value = state
    }

    fun updateCandidateLayoutState(state: Any?) {
        currentCandidateLayoutState = state
    }

    fun updateInputListLayoutState(state: Any?) {
        currentInputListLayoutState = state
    }

    private fun computeToolList(keyboardType: KeyboardType): ToolListState {
        return ToolListState(
            settings = true,
            switchIME = true,
            closeKeyboard = true,
        )
    }

    override fun onCleared() {
        super.onCleared()
        actionPlayer.stop()
        feedbackState.clear()
    }
}
