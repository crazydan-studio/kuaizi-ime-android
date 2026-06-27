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
    private var clipboardJob: Job? = null

    fun start(startupConfig: StartupConfig) {
        updateRuntimeConfig(startupConfig)

        val keyboardType = resolveKeyboardType(startupConfig)
        stateMachine.resetTo(keyboardType.initialState())

        val isPassword = startupConfig.editorInputType == EditorInputType.Password

        val newState = _state.value.copy(
            keyboard = _state.value.keyboard.copy(type = keyboardType),
            inputList = if (isPassword) InputList() else _state.value.inputList,
            config = _state.value.config.copy(
                runtime = _state.value.config.runtime.copy(
                    keyPopupTipsEnabled = if (isPassword) false
                    else _state.value.config.runtime.keyPopupTipsEnabled,
                ),
            ),
        )
        applyStateUpdate { newState }

        if (_state.value.config.ui.clipPastePopupTipsEnabled
            && _state.value.clipboard.currentText != null
        ) {
            _effect.tryEmit(
                ImeEffect.PopupTip.Action(
                    message = "可粘贴内容",
                    actionLabel = "粘贴",
                    action = ImeIntent.PasteClip(_state.value.clipboard.currentText!!),
                    persistent = true,
                ),
            )
        }
    }

    fun close() {
        clipboardJob?.cancel()
        clipboardJob = null
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

        val editorAction = result.editorAction
        if (editorAction != null) {
            dispatchEditorAction(editorAction)
        }
    }

    private fun processSideEffects(sideEffects: List<ImeIntent>) {
        scope.launch(Dispatchers.Default) {
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
                        val candidates = dictProvider.query(intent.pinyin)
                        handleIntent(ImeIntent.SetCandidates(candidates))
                    }
                    else -> handleIntent(intent)
                }
            }
        }
    }

    private fun resolveHandler(type: KeyboardType): KeyboardIntentHandler {
        return when (type) {
            KeyboardType.Pinyin, KeyboardType.Latin -> PinyinIntentHandler(type)
            KeyboardType.Number -> NumberKeyboardIntentHandler(type)
            KeyboardType.Symbol -> SymbolKeyboardIntentHandler(type)
            KeyboardType.Emoji -> EmojiKeyboardIntentHandler(type)
            KeyboardType.Math -> MathKeyboardIntentHandler(type)
            KeyboardType.Editor -> EditorKeyboardIntentHandler(type)
            KeyboardType.Candidate -> CandidateKeyboardIntentHandler(type)
            KeyboardType.CommitOption -> CommitOptionKeyboardIntentHandler(type)
        }
    }

    private fun updateRuntimeConfig(startupConfig: StartupConfig) {
        applyStateUpdate { state ->
            state.copy(
                config = state.config.copy(
                    runtime = state.config.runtime.copy(
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
            val inputListOp = InputListOperator(InputListEditor())
            return ImeEngine(
                config = config,
                dictProvider = dictProvider,
                stateMachine = KeyboardStateMachine(inputListOp = inputListOp),
                inputListOp = inputListOp,
            )
        }
    }
}
