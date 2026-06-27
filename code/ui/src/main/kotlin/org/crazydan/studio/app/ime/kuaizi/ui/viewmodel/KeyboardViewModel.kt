package org.crazydan.studio.app.ime.kuaizi.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.crazydan.studio.app.ime.kuaizi.engine.*
import org.crazydan.studio.app.ime.kuaizi.engine.domain.*
import org.crazydan.studio.app.ime.kuaizi.engine.input_action.*
import org.crazydan.studio.app.ime.kuaizi.ui.AudioPlayer
import org.crazydan.studio.app.ime.kuaizi.ui.HapticPlayer
import org.crazydan.studio.app.ime.kuaizi.ui.keyboard.CandidateListLayoutState
import org.crazydan.studio.app.ime.kuaizi.ui.keyboard.InputListLayoutState
import org.crazydan.studio.app.ime.kuaizi.ui.keyboard.KeyLayoutState
import org.crazydan.studio.app.ime.kuaizi.ui.player.ComposeInputActionPositionResolver
import org.crazydan.studio.app.ime.kuaizi.ui.player.InputActionPlayer

sealed class KeyboardLayoutMode {
    data object Stacked : KeyboardLayoutMode()
    data class Separated(val zoneARatio: Float = 0.4f) : KeyboardLayoutMode()
}

class KeyboardViewModel(
    private val engine: ImeEngine,
    private val audioPlayer: AudioPlayer? = null,
    private val hapticPlayer: HapticPlayer? = null,
) : ViewModel() {

    val state: StateFlow<ImeState> = engine.state
    val config: ImeConfig get() = state.value.config

    val feedbackState = GestureFeedbackState()

    private val _layoutMode = MutableStateFlow<KeyboardLayoutMode>(KeyboardLayoutMode.Stacked)
    val layoutMode: StateFlow<KeyboardLayoutMode> = _layoutMode.asStateFlow()

    private val _popupTipState = MutableStateFlow<PopupTipState?>(null)
    val popupTipState: StateFlow<PopupTipState?> = _popupTipState.asStateFlow()

    private val _toolListState = MutableStateFlow(ToolListState(emptyList()))
    val toolListState: StateFlow<ToolListState> = _toolListState.asStateFlow()

    private var popupTipDismissJob: Job? = null

    private var _currentKeyLayoutState: KeyLayoutState? = null
    private var _currentCandidateLayoutState: CandidateListLayoutState? = null
    private var _currentInputListLayoutState: InputListLayoutState? = null

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

    val isInputting: Boolean get() = state.value.inputList.hasPending

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

    private fun dismissPopupTipAfter(timeoutMs: Long) {
        popupTipDismissJob?.cancel()
        popupTipDismissJob = viewModelScope.launch {
            delay(timeoutMs)
            _popupTipState.value = null
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
        _currentKeyLayoutState = state
    }

    fun updateCandidateLayoutState(state: CandidateListLayoutState) {
        _currentCandidateLayoutState = state
    }

    fun updateInputListLayoutState(state: InputListLayoutState) {
        _currentInputListLayoutState = state
    }

    private fun computeToolList(keyboardType: KeyboardType): ToolListState {
        val tools = mutableListOf<ToolItem>()
        tools.add(ToolItem(label = "全选", disabled = false))
        tools.add(ToolItem(label = "复制", disabled = false))
        tools.add(ToolItem(label = "粘贴", disabled = false))
        tools.add(ToolItem(label = "剪切", disabled = false))
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

    override fun onCleared() {
        super.onCleared()
        actionPlayer.stop()
        feedbackState.clear()
    }

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
