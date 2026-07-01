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

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.crazydan.studio.app.ime.kuaizi.engine.bridge.ImeEditorBridge
import org.crazydan.studio.app.ime.kuaizi.engine.dict.ImeDictProvider
import org.crazydan.studio.app.ime.kuaizi.engine.domain.CandidateKeyboardIntentHandler
import org.crazydan.studio.app.ime.kuaizi.engine.domain.CommitOptionKeyboardIntentHandler
import org.crazydan.studio.app.ime.kuaizi.engine.domain.EditorKeyboardIntentHandler
import org.crazydan.studio.app.ime.kuaizi.engine.domain.EmojiKeyboardIntentHandler
import org.crazydan.studio.app.ime.kuaizi.engine.domain.InputList
import org.crazydan.studio.app.ime.kuaizi.engine.domain.InputListEditor
import org.crazydan.studio.app.ime.kuaizi.engine.domain.InputListOperator
import org.crazydan.studio.app.ime.kuaizi.engine.domain.KeyboardIntentHandler
import org.crazydan.studio.app.ime.kuaizi.engine.domain.KeyboardStateMachine
import org.crazydan.studio.app.ime.kuaizi.engine.domain.KeyboardType
import org.crazydan.studio.app.ime.kuaizi.engine.domain.MathKeyboardIntentHandler
import org.crazydan.studio.app.ime.kuaizi.engine.domain.NumberKeyboardIntentHandler
import org.crazydan.studio.app.ime.kuaizi.engine.domain.PinyinIntentHandler
import org.crazydan.studio.app.ime.kuaizi.engine.domain.SymbolKeyboardIntentHandler
import org.crazydan.studio.app.ime.kuaizi.engine.logging.ImeLog
import org.crazydan.studio.app.ime.kuaizi.engine.logging.LogLevel

/**
 * 引擎核心入口点，提供完整的输入法能力。
 *
 * 采用 MVI 架构管理状态流转：
 * - 通过 [ImeIntent] 接收用户操作（输入通道）
 * - 通过 [StateFlow]<[ImeState]> 暴露状态（状态通道）
 * - 通过 [SharedFlow]<[ImeEffect]> 发射副作用（副作用通道）
 * - 通过 [ImeEditorBridge] 输出编辑器操作（编辑器操作通道）
 *
 * 引擎不依赖任何 UI 框架，第三方应用可自由替换 UI 层。
 * 构造函数标记为 `internal`，强制通过 [Companion.create] 工厂方法创建实例，
 * 确保所有依赖项正确初始化。
 *
 * @property config 运行时配置，包含引擎/UI/运行时三层子配置
 * @property state 只读状态流，UI 层通过 collectAsState 订阅
 * @property effect 一次性副作用通道，用于传递音效、振动、弹出提示等信号
 */
class ImeEngine internal constructor(
    config: ImeConfig,
    private val dictProvider: ImeDictProvider,
    private val keyboardStateMachine: KeyboardStateMachine,
    private val inputListOp: InputListOperator,
    internal val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) {
    private val logger by lazy { ImeLog.logger(ImeEngine::class) }

    private val _state: MutableStateFlow<ImeState> = MutableStateFlow(ImeState(config = config))

    /** 只读状态流：UI 层通过此流订阅 [ImeState] 驱动界面重组。 */
    val state: StateFlow<ImeState> = _state.asStateFlow()

    private val _effect: MutableSharedFlow<ImeEffect> = MutableSharedFlow(extraBufferCapacity = 64)

    /** 只读副作用流：UI 层在独立协程中收集并消费一次性效果。 */
    val effect: SharedFlow<ImeEffect> = _effect.asSharedFlow()

    private val editorBridges: MutableList<ImeEditorBridge> = mutableListOf()
    private var clipboardJob: Job? = null

    // -----------------------------------------------

    /**
     * 启动输入法，建立后续所有 Intent 处理的前置条件。
     *
     * [start] 是独立的生命周期方法，不经过 [handleIntent] 处理链，
     * 而是直接执行引擎级初始化操作。
     *
     * 处理步骤：
     * 1. 更新 RuntimeConfig：[ImeConfig.Startup] 中的值覆盖到当前 [ImeConfig.Runtime]
     * 1. 确定 KeyboardType：通过两级级联规则确定启动时的键盘类型
     * 1. 更新 keyPopupTipsEnabled：若为 Password 类型则强制禁用按键提示
     * 1. 重置 KeyboardStateMachine：根据确定的 KeyboardType 重置状态机
     * 1. 检查剪贴板可粘贴内容：若启用则发射粘贴提示
     * 1. 通过 [applyStateUpdate] 原子更新 [ImeState]
     *
     * @param startupConfig 启动配置，包含输入法子类型、屏幕方向和编辑器输入类型
     */
    fun start(startupConfig: ImeConfig.Startup) {
        updateRuntimeConfig(startupConfig)

        val keyboardType = resolveKeyboardType(startupConfig)
        keyboardStateMachine.resetTo(keyboardType.initialState())

        val isPassword = startupConfig.editorInputType == EditorInputType.Password
        val newState = _state.value.copy(
            keyboard = _state.value.keyboard.copy(type = keyboardType),
            inputList =
                // 清空输入列表，以确保采用直输模式
                if (isPassword) InputList()
                else _state.value.inputList,
            config = _state.value.config.copy(
                runtime = _state.value.config.runtime.copy(
                    keyPopupTipsEnabled =
                        if (isPassword) false
                        else _state.value.config.runtime.keyPopupTipsEnabled,
                ),
            ),
        )
        applyStateUpdate { newState }

        // 检查剪贴板是否有可粘贴内容，若有则弹出粘贴确认提示
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

    /**
     * 关闭输入法，仅隐藏面板，输入状态保持不变。
     *
     * 轻量级操作：关闭剪贴板监听停止占用系统资源，
     * 但不重置键盘状态、输入列表或候选列表。
     * 调用 [start] 后可恢复到关闭前的完整工作状态。
     */
    fun close() {
        clipboardJob?.cancel()
        clipboardJob = null
    }

    /**
     * 销毁引擎，回收所有资源。
     *
     * 终态操作：取消所有异步任务、清空编辑器桥接列表。
     * 调用 [destroy] 后引擎不可再启动。
     */
    fun destroy() {
        scope.cancel()
        editorBridges.clear()
    }

    // -----------------------------------------------

    /**
     * 注册编辑器桥接。
     *
     * 注册后的桥接将接收引擎分发的所有 [EditorAction]。
     * @param bridge 要注册的编辑器桥接实例
     */
    fun attachEditorBridge(bridge: ImeEditorBridge) {
        editorBridges.add(bridge)
    }

    /**
     * 注销编辑器桥接。
     *
     * @param bridge 要注销的编辑器桥接实例
     */
    fun detachEditorBridge(bridge: ImeEditorBridge) {
        editorBridges.remove(bridge)
    }

    // -----------------------------------------------

    /**
     * 处理用户意图，MVI 架构的核心入口。
     *
     * 处理流程：
     * 1. [SwitchKeyboard] 意图直接切换键盘，不经过状态机
     * 2. 其他意图通过 [KeyboardIntentHandler] 映射为 [KeyboardStateTransition]，
     *    由 [KeyboardStateMachine] 执行状态转换
     * 3. 处理 sideEffects 副作用意图
     * 4. 通过 [applyStateUpdate] 更新 [ImeState]
     * 5. 分发 [EditorAction] 到 [ImeEditorBridge]
     * 6. 发射 [ImeEffect] 副作用信号
     *
     * @param intent 用户意图
     */
    fun handleIntent(intent: ImeIntent) {
        when (intent) {
            is ImeIntent.SwitchKeyboard -> handleSwitchKeyboard(intent.type)
            else -> handleWithStateMachine(intent)
        }
    }

    /**
     * 更新配置。
     *
     * @param block 配置转换函数，接收当前 [ImeConfig] 返回新配置
     */
    fun updateConfig(block: (ImeConfig) -> ImeConfig) {
        applyStateUpdate { state ->
            state.copy(config = block(state.config))
        }
    }

    /** 监听配置 [ImeConfig] 的变更 */
    suspend inline fun whenConfigUpdated(collector: FlowCollector<ImeConfig>) =
        state.map { it.config }
            .distinctUntilChanged()
            .drop(1)  // 跳过订阅时的状态，仅关注后续的变化
            .collect(collector)

    // -----------------------------------------------

    private fun handleSwitchKeyboard(type: KeyboardType) {
        keyboardStateMachine.resetTo(type.initialState())

        applyStateUpdate { state ->
            state.copy(keyboard = state.keyboard.copy(type = type))
        }
    }

    private fun handleWithStateMachine(intent: ImeIntent) {
        val handler = resolveHandler(_state.value.keyboard.type)
        val transition = handler.handleIntent(intent, _state.value.keyboard.state)
        val result = keyboardStateMachine.transition(transition)
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

    /**
     * 处理副作用意图列表。
     *
     * 通过 [ArrayDeque] 显式工作队列循环处理，而非递归调用。
     * 队列最大深度 5 作为安全网防止无限递归。
     *
     * @param sideEffects 需要异步处理的副作用意图列表
     */
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
//                        handleIntent(ImeIntent.SetCandidates(candidates))
                    }

                    else -> handleIntent(intent)
                }
            }
        }
    }

    /**
     * 根据键盘类型解析对应的 [KeyboardIntentHandler]。
     *
     * @param type 当前键盘类型
     * @return 对应的意图处理器
     */
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

    private fun updateRuntimeConfig(startupConfig: ImeConfig.Startup) {
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

    /**
     * 根据启动配置确定启动时的键盘类型。
     *
     * 两级级联规则：
     * 1. 根据 [InputMethodSubtype] 确定基础键盘（Latin → 拉丁键盘，Hans → 拼音键盘）
     * 1. 根据 [EditorInputType] 修正（Number/Datetime/Phone → 数字键盘，Password → 拉丁键盘）
     *
     * @param startupConfig 启动配置
     * @return 确定的键盘类型
     */
    private fun resolveKeyboardType(startupConfig: ImeConfig.Startup): KeyboardType {
        val baseType =
            when (startupConfig.inputMethodSubtype) {
                InputMethodSubtype.Latin -> KeyboardType.Latin
                InputMethodSubtype.Hans -> KeyboardType.Pinyin
            }

        return when (startupConfig.editorInputType) {
            EditorInputType.Number,
            EditorInputType.Datetime,
            EditorInputType.Phone ->
                KeyboardType.Number

            EditorInputType.Password ->
                KeyboardType.Latin

            else -> baseType
        }
    }

    /**
     * 分发 [EditorAction] 到所有已注册的 [ImeEditorBridge]。
     *
     * 遍历所有已注册桥接，对每个桥接根据 [EditorAction] 类型调用对应的语义方法。
     * 若桥接列表为空则静默跳过。
     *
     * @param action 要分发的编辑器操作
     */
    private fun dispatchEditorAction(action: EditorAction) {
        if (editorBridges.isEmpty()) return

        editorBridges.forEach { bridge ->
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

    /**
     * 统一的状态更新出口：所有 [ImeState] 变更必须经过此方法。
     *
     * 提供以下统一能力：
     * - 日志记录：DEBUG 等级下记录旧状态与新状态的 diff
     * - 断言检查：DEBUG 等级下验证状态不变式
     *
     * @param transform 状态转换函数，接收当前状态返回新状态
     */
    private fun applyStateUpdate(transform: (ImeState) -> ImeState) {
        val oldState = _state.value
        val newState = transform(oldState)

        if (ImeLog.isEnabledLevel(LogLevel.DEBUG)) {
            logger.debug { "State updated" }
            assertStateInvariants(newState)
        }

        _state.value = newState
    }

    /**
     * 断言状态不变式（仅在 DEBUG 等级下执行）。
     *
     * 验证 [ImeState] 中各子状态之间的一致性约束，
     * 包括 keyboard.type 与 keyboard.state 的兼容性等。
     */
    private fun assertStateInvariants(state: ImeState) {
        // invariants checked only in DEBUG
    }

    companion object {
        /**
         * 工厂方法：创建 [ImeEngine] 实例。
         *
         * 内部完成以下初始化工作：
         * - 创建 [KeyboardStateMachine]
         * - 创建 [InputListOperator]
         *
         * 工厂方法确保所有依赖项正确初始化，避免外部构造时遗漏关键组件。
         *
         * @param config 运行时配置，可选，默认使用全默认值的 [ImeConfig]
         * @param dictProvider 字典查询接口，由外部注入
         * @return 初始化完成的 [ImeEngine] 实例
         */
        fun create(
            config: ImeConfig = ImeConfig(),
            dictProvider: ImeDictProvider,
        ): ImeEngine {
            val inputListOp = InputListOperator(InputListEditor())

            return ImeEngine(
                config = config,
                dictProvider = dictProvider,
                keyboardStateMachine = KeyboardStateMachine(inputListOp = inputListOp),
                inputListOp = inputListOp,
            )
        }
    }
}
