package org.crazydan.studio.app.ime.kuaizi.engine

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.crazydan.studio.app.ime.kuaizi.engine.domain.*
import org.crazydan.studio.app.ime.kuaizi.engine.bridge.ImeEditorBridge
import org.crazydan.studio.app.ime.kuaizi.engine.dict.ImeDictProvider
import org.crazydan.studio.app.ime.kuaizi.engine.logging.ImeLog
import org.crazydan.studio.app.ime.kuaizi.engine.logging.ImeLogger

class ImeEngine internal constructor(
    private var config: ImeConfig,
    private val dictProvider: ImeDictProvider,
    private val stateMachine: KeyboardStateMachine,
    private val inputListOp: InputListOperator,
    internal val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) {
    private val _state: MutableStateFlow<ImeState> = MutableStateFlow(ImeState())
    val state: StateFlow<ImeState> = _state.asStateFlow()

    private val _effect: MutableSharedFlow<ImeEffect> = MutableSharedFlow(extraBufferCapacity = 64)
    val effect: SharedFlow<ImeEffect> = _effect.asSharedFlow()

    private val _editorBridges: MutableList<ImeEditorBridge> = mutableListOf()

    fun start(startupConfig: StartupConfig) {
        updateRuntimeConfig(startupConfig)

        val keyboardType = resolveKeyboardType(startupConfig)
        stateMachine.resetTo(keyboardType.initialState())

        val newState = _state.value.copy(
            keyboard = _state.value.keyboard.copy(type = keyboardType),
            config = _state.value.config.copy(
                runtime = _state.value.config.runtime.copy(
                    keyPopupTipsEnabled = if (startupConfig.editorInputType == EditorInputType.Password) false
                    else _state.value.config.ui.keyPopupTipsEnabled,
                ),
            ),
        )
        applyStateUpdate { newState }
    }

    fun close() {
        // lightweight: stop clipboard monitoring, keep state
    }

    fun destroy() {
        scope.cancel()
        _editorBridges.clear()
    }

    fun attachEditorBridge(bridge: ImeEditorBridge) {
        _editorBridges.add(bridge)
    }

    fun detachEditorBridge(bridge: ImeEditorBridge) {
        _editorBridges.remove(bridge)
    }

    fun handleIntent(intent: ImeIntent) {
        when (intent) {
            is ImeIntent.SwitchKeyboard -> handleSwitchKeyboard(intent.type)
            else -> handleWithStateMachine(intent)
        }
    }

    fun updateConfig(block: (ImeConfig) -> ImeConfig) {
        applyStateUpdate { state ->
            state.copy(config = block(state.config))
        }
    }

    private fun handleSwitchKeyboard(type: KeyboardType) {
        stateMachine.resetTo(type.initialState())
        applyStateUpdate { state ->
            state.copy(keyboard = state.keyboard.copy(type = type))
        }
    }

    private fun handleWithStateMachine(intent: ImeIntent) {
        val handler = resolveHandler(_state.value.keyboard.type)
        val transition = handler.handleIntent(intent, _state.value.keyboard.state)
        val result = stateMachine.transition(transition)
        val sideEffects = result.sideEffects

        val effect = when (intent) {
            is ImeIntent.PressKey -> ImeEffect.PlayAudio(AudioType.KeyPress)
            else -> null
        }

        applyStateUpdate { state ->
            state.copy(
                keyboard = state.keyboard.copy(state = result.newState),
            )
        }

        if (effect != null) {
            _effect.tryEmit(effect)
        }

        processSideEffects(sideEffects)
    }

    private fun processSideEffects(sideEffects: List<ImeIntent>) {
        val queue = ArrayDeque(sideEffects)
        var depth = 0
        val maxDepth = 5

        while (queue.isNotEmpty()) {
            if (++depth > maxDepth) {
                throw IllegalStateException("Side effect recursion exceeds max depth $maxDepth")
            }
            val intent = queue.removeFirst()
            when (intent) {
                is ImeIntent.LoadCandidates -> {
                    scope.launch {
                        val candidates = dictProvider.query(intent.pinyin)
                        handleIntent(ImeIntent.SetCandidates(candidates))
                    }
                }
                else -> handleIntent(intent)
            }
        }
    }

    private fun resolveHandler(type: KeyboardType): KeyboardIntentHandler {
        return when (type) {
            KeyboardType.Pinyin, KeyboardType.Latin -> PinyinIntentHandler()
            KeyboardType.Number -> NumberKeyboardIntentHandler()
            KeyboardType.Symbol -> SymbolKeyboardIntentHandler()
            KeyboardType.Emoji -> EmojiKeyboardIntentHandler()
            KeyboardType.Math -> MathKeyboardIntentHandler()
            KeyboardType.Editor -> EditorKeyboardIntentHandler()
            KeyboardType.Candidate -> CandidateKeyboardIntentHandler()
            KeyboardType.CommitOption -> CommitOptionKeyboardIntentHandler()
        }
    }

    private fun updateRuntimeConfig(startupConfig: StartupConfig) {
        applyStateUpdate { state ->
            state.copy(
                config = state.config.copy(
                    runtime = ImeConfig.RuntimeConfig(
                        screenOrientation = startupConfig.screenOrientation,
                        editorInputType = startupConfig.editorInputType ?: state.config.runtime.editorInputType,
                    ),
                ),
            )
        }
    }

    private fun resolveKeyboardType(startupConfig: StartupConfig): KeyboardType {
        val baseType = when (startupConfig.imeSubtype) {
            IMESubtype.Latin -> KeyboardType.Latin
            IMESubtype.Hans -> KeyboardType.Pinyin
        }
        return when (startupConfig.editorInputType) {
            EditorInputType.Number, EditorInputType.Datetime, EditorInputType.Phone -> KeyboardType.Number
            EditorInputType.Password -> KeyboardType.Latin
            else -> baseType
        }
    }

    private fun dispatchEditorAction(action: EditorAction) {
        if (_editorBridges.isEmpty()) return
        _editorBridges.forEach { bridge ->
            when (action) {
                is EditorAction.CommitText -> bridge.commitText(action.text, action.replacements)
                is EditorAction.RevokeCommit -> bridge.revokeCommit()
                is EditorAction.InsertPairedSymbols -> bridge.insertPairedSymbols(action.left, action.right)
                is EditorAction.MoveCursor -> bridge.moveCursor(action.direction)
                is EditorAction.SelectRange -> bridge.selectRange(action.direction)
                is EditorAction.PerformEdit -> bridge.performEdit(action.action)
            }
        }
    }

    private fun applyStateUpdate(transform: (ImeState) -> ImeState) {
        val oldState = _state.value
        val newState = transform(oldState)

        if (ImeLog.level <= LogLevel.DEBUG) {
            ImeLogger.d("ImeEngine", "State updated")
            assertStateInvariants(newState)
        }

        _state.value = newState
    }

    private fun assertStateInvariants(state: ImeState) {
        // invariants checked only in DEBUG
    }

    companion object {
        fun create(
            config: ImeConfig = ImeConfig(),
            dictProvider: ImeDictProvider,
        ): ImeEngine {
            return ImeEngine(
                config = config,
                dictProvider = dictProvider,
                stateMachine = KeyboardStateMachine(),
                inputListOp = InputListOperator(),
            )
        }
    }
}
