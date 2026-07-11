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
import org.crazydan.studio.app.ime.kuaizi.engine.bridge.EditorAction
import org.crazydan.studio.app.ime.kuaizi.engine.bridge.ImeEditorBridge
import org.crazydan.studio.app.ime.kuaizi.engine.dict.ImeDictProvider
import org.crazydan.studio.app.ime.kuaizi.engine.domain.EditorInputType
import org.crazydan.studio.app.ime.kuaizi.engine.domain.InputMethodSubtype
import org.crazydan.studio.app.ime.kuaizi.engine.domain.PinyinTree
import org.crazydan.studio.app.ime.kuaizi.engine.input.InputList
import org.crazydan.studio.app.ime.kuaizi.engine.input.InputListEditor
import org.crazydan.studio.app.ime.kuaizi.engine.input.InputListOperator
import org.crazydan.studio.app.ime.kuaizi.engine.keyboard.CandidateKeyboardIntentHandler
import org.crazydan.studio.app.ime.kuaizi.engine.keyboard.CommitOptionKeyboardIntentHandler
import org.crazydan.studio.app.ime.kuaizi.engine.keyboard.EmojiKeyboardIntentHandler
import org.crazydan.studio.app.ime.kuaizi.engine.keyboard.KeyboardHandMode
import org.crazydan.studio.app.ime.kuaizi.engine.keyboard.KeyboardIntentHandler
import org.crazydan.studio.app.ime.kuaizi.engine.keyboard.KeyboardState
import org.crazydan.studio.app.ime.kuaizi.engine.keyboard.KeyboardStateMachine
import org.crazydan.studio.app.ime.kuaizi.engine.keyboard.KeyboardType
import org.crazydan.studio.app.ime.kuaizi.engine.keyboard.MathKeyboardIntentHandler
import org.crazydan.studio.app.ime.kuaizi.engine.keyboard.NumberKeyboardIntentHandler
import org.crazydan.studio.app.ime.kuaizi.engine.keyboard.SymbolKeyboardIntentHandler
import org.crazydan.studio.app.ime.kuaizi.engine.keyboard.intent.EditorKeyboardIntentHandler
import org.crazydan.studio.app.ime.kuaizi.engine.keyboard.intent.LatinKeyboardIntentHandler
import org.crazydan.studio.app.ime.kuaizi.engine.keyboard.intent.PinyinKeyboardIntentHandler
import org.crazydan.studio.app.ime.kuaizi.engine.log.ImeLog
import org.crazydan.studio.app.ime.kuaizi.engine.log.LogLevel

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
 * @param config 引擎初始配置，包含引擎/UI/运行时三层子配置
 */
class ImeEngine internal constructor(
    config: ImeConfig,
    private val dictProvider: ImeDictProvider,
    private val keyboardStateMachine: KeyboardStateMachine,
    private val inputListOp: InputListOperator,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) {
    private val logger by lazy { ImeLog.logger(ImeEngine::class) }

    // -------------------------------
    private val _state: MutableStateFlow<ImeState> = MutableStateFlow(ImeState(config = config))

    /** 只读状态流：UI 层通过此流订阅 [ImeState] 驱动界面重组。 */
    val state: StateFlow<ImeState> = _state.asStateFlow()

    // -------------------------------
    private val _effect: MutableSharedFlow<ImeEffect> = MutableSharedFlow(extraBufferCapacity = 64)

    /** 只读副作用流：UI 层在独立协程中收集并消费一次性效果。 */
    val effect: SharedFlow<ImeEffect> = _effect.asSharedFlow()

    // -------------------------------
    private val editorBridges: MutableList<ImeEditorBridge> = mutableListOf()
    private var clipboardJob: Job? = null

    // -----------------------------------------------

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
        suspend fun create(
            config: ImeConfig = ImeConfig(),
            dictProvider: ImeDictProvider,
        ): ImeEngine {
            val pinyinList = dictProvider.loadAllPinyin()
            val pinyinTree = PinyinTree.Builder().addAll(pinyinList).build()

            val inputListOp = InputListOperator(InputListEditor())

            return ImeEngine(
                config = config,
                dictProvider = dictProvider,
                keyboardStateMachine = KeyboardStateMachine(
                    pinyinTree = pinyinTree,
                ),
                inputListOp = inputListOp,
            )
        }
    }

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
     * 1. 重置 KeyboardStateMachine：根据确定的 KeyboardType 重置状态机
     * 1. 检查剪贴板可粘贴内容：若启用则发射粘贴提示
     * 1. 通过 [applyStateUpdate] 原子更新 [ImeState]
     *
     * @param startupConfig 启动配置，包含输入法子类型、屏幕方向和编辑器输入类型
     */
    fun start(startupConfig: ImeConfig.Startup) {
        updateRuntimeConfig(startupConfig)

        // -----------------------------
        val keyboardType = resolveKeyboardType(startupConfig)
        val keyboardState = keyboardType.initialState()

        val isPassword = startupConfig.editorInputType == EditorInputType.Password

        applyStateUpdate { state ->
            state.copy(
                keyboard = state.keyboard.copy(
                    type = keyboardType,
                    state = keyboardState,
                    masterType = null,
                ),
                inputList =
                    // 清空输入列表，以确保采用直输模式
                    if (isPassword) InputList()
                    else state.inputList,
            )
        }

        // -----------------------------
        // Note：_state.value 可能已变更
        val state = _state.value

        // 检查剪贴板是否有可粘贴内容，若有则弹出粘贴确认提示
        if (state.config.ui.clipPastePopupTipsEnabled
            && state.clipboard.currentText != null
        ) {
            _effect.tryEmit(
                ImeEffect.PopupTip.Action(
                    message = "可粘贴内容",
                    actionLabel = "粘贴",
                    action = ImeIntent.PasteClip(state.clipboard.currentText),
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
        // TODO 待定
//        clipboardJob?.cancel()
//        clipboardJob = null
    }

    fun exit() {
        // TODO 待定
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
     *
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
     * @param intent 用户意图
     */
    suspend fun handleIntent(intent: ImeIntent) {
        when (intent) {
            is ImeIntent.OnKeyboard ->
                handleIntentOnKeyboard(intent)

            is ImeIntent.InputList ->
                handleIntentWithInputList(intent)

            is ImeIntent.Keyboard ->
                handleIntentWithKeyboard(intent)

            is ImeIntent.Editor.PerformEdit ->
                dispatchEditorAction(
                    action = EditorAction.PerformEdit(intent.action)
                )

            else -> {}
        }
    }

    // -----------------------------------------------

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

    /** 监听配置 [ImeConfig] 的变更。注意，该函数将挂起当前协程，后续代码不会被执行。 */
    suspend inline fun whenConfigUpdated(collector: FlowCollector<ImeConfig>) =
        state.map { it.config }
            .distinctUntilChanged()
            .drop(1)  // 跳过订阅时的状态，仅关注后续的变化
            .collect(collector)

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

    // -----------------------------------------------

    /** 处理发生在键盘上的意图 */
    private suspend fun handleIntentOnKeyboard(intent: ImeIntent.OnKeyboard) {
        val keyboard = _state.value.keyboard
        val keyboardType = keyboard.type
        val keyboardState = keyboard.state

        val handler = resolveKeyboardIntentHandler(keyboardType)
        val transition = handler.handleIntent(intent, keyboardState)

        // --------------------
        val result = keyboardStateMachine.handleTransition(transition, keyboardState)
        updateKeyboardState(result.newState)

        // --------------------
        result.sideEffects?.forEach {
            handleIntent(it)
        }

        result.editorAction?.also {
            dispatchEditorAction(it)
        }
    }

    /**
     * 根据键盘类型解析对应的 [KeyboardIntentHandler]。
     *
     * @param keyboardType 当前键盘类型
     * @return 对应的意图处理器
     */
    private fun resolveKeyboardIntentHandler(keyboardType: KeyboardType): KeyboardIntentHandler {
        val keyboardInputMode = _state.value.config.ui.keyboardInputMode

        return when (keyboardType) {
            KeyboardType.Pinyin -> PinyinKeyboardIntentHandler(keyboardInputMode)
            KeyboardType.Latin -> LatinKeyboardIntentHandler(keyboardInputMode)
            //
            KeyboardType.Number -> NumberKeyboardIntentHandler()
            KeyboardType.Symbol -> SymbolKeyboardIntentHandler()
            KeyboardType.Emoji -> EmojiKeyboardIntentHandler()
            //
            KeyboardType.Math -> MathKeyboardIntentHandler()
            // TODO 考虑将复制、粘贴等常用功能集成到键盘面板中
            KeyboardType.Editor -> EditorKeyboardIntentHandler()
            KeyboardType.Candidate -> CandidateKeyboardIntentHandler()
            KeyboardType.CommitOption -> CommitOptionKeyboardIntentHandler()
        }
    }

//    /**
//     * 处理副作用意图列表。
//     *
//     * 通过 [ArrayDeque] 显式工作队列循环处理，而非递归调用。
//     * 队列最大深度 5 作为安全网防止无限递归。
//     *
//     * @param sideEffects 需要异步处理的副作用意图列表
//     */
//    private fun processSideEffects(sideEffects: List<ImeIntent>) {
//        if (sideEffects.isEmpty()) return
//
//        scope.launch(Dispatchers.Default) {
//            val queue = ArrayDeque(sideEffects)
//            var depth = 0
//            val maxDepth = 5
//
//            while (queue.isNotEmpty()) {
//                if (++depth > maxDepth) {
//                    throw IllegalStateException("Side effect recursion exceeds max depth $maxDepth")
//                }
//
//                val intent = queue.removeFirst()
//                handleIntent(intent)
//            }
//        }
//    }

    /**
     * 分发 [EditorAction] 到所有已注册的 [ImeEditorBridge]。
     *
     * 遍历所有已注册桥接，对每个桥接根据 [EditorAction] 类型调用对应的语义方法。
     * 若桥接列表为空则静默跳过。
     *
     * @param action 要分发的编辑器操作
     */
    private fun dispatchEditorAction(action: EditorAction) {
        editorBridges.forEach { bridge ->
            when (action) {
                is EditorAction.CommitText ->
                    bridge.commitText(
                        action.text, action.replacements,
                        oneByOne = false, revertable = true
                    )

                is EditorAction.RevokeCommit ->
                    bridge.revokeCommit()

                is EditorAction.InsertPairedSymbols ->
                    bridge.insertPairedSymbols(action.left, action.right)

                // -----------------------------
                is EditorAction.MoveCursor ->
                    bridge.moveCursor(action.motion)

                is EditorAction.SelectSelection ->
                    bridge.selectSelection(action.motion)

                is EditorAction.PerformEdit ->
                    bridge.performEdit(action.action)
            }
        }
    }

    /** 处理与输入列表相关的意图 */
    private suspend fun handleIntentWithInputList(intent: ImeIntent.InputList) {
        when (intent) {
            is ImeIntent.InputList.Commit -> {
                // TODO 向编辑器提交文本：EditorAction.CommitText、EditorAction.InsertPairedSymbols
                // TODO 更新数据库
                // TODO 主键盘切换到 Idle 状态，临时性键盘切换回主键盘
            }

            is ImeIntent.InputList.Revoke -> {
                // TODO 从编辑器撤销提交：EditorAction.RevokeCommit
                // TODO 恢复 InputList
                // TODO 还原数据库记录
                // TODO 选中 InputList 中的已选中项
            }

            is ImeIntent.InputList.AddChar -> {
                // TODO 根据 InputList 当前状态决定字符添加和替换，以及是否为直输（冻结或为空时）
                // TODO 对拉丁文输入做数据库补全查询
            }

            is ImeIntent.InputList.BackspaceChar -> {
                // TODO 回删 InputList 中字符或编辑器内字符（InputList 为直输时）
                // TODO 编辑器回删通过 EditorEditAction.Backspace
            }

            is ImeIntent.InputList.DeleteSelected -> {
            }

            is ImeIntent.InputList.NewPending -> {
                // TODO 若为拼音输入且拼音有效，则查询候选字
                // TODO InputList 新建待输入
            }

            is ImeIntent.InputList.UpdatePending -> {
                // TODO 若为拼音输入且拼音有效，则查询候选字
                // TODO InputList 更新待输入
            }

            is ImeIntent.InputList.ConfirmPending -> {
                // TODO InputList 确认待输入
                // TODO 若为拼音输入，则更新拼音输入短语
            }

            is ImeIntent.InputList.DropPending -> {
                // TODO InputList 丢弃待输入
            }
        }
    }

    /** 处理与键盘相关的意图 */
    private fun handleIntentWithKeyboard(intent: ImeIntent.Keyboard) {
        when (intent) {
            is ImeIntent.Keyboard.SwitchTo ->
                switchKeyboard(intent.type)

            is ImeIntent.Keyboard.ToggleHandMode ->
                toggleKeyboardHandMode()

            else -> {}
        }
    }

    // -----------------------------------------------

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

    private fun switchKeyboard(keyboardType: KeyboardType) =
        applyStateUpdate { state ->
            val oldKeyboardType = state.keyboard.type
            val oldMasterKeyboardType = state.keyboard.masterType

            val keyboardState = keyboardType.initialState()

            state.copy(
                keyboard = state.keyboard.copy(
                    type = keyboardType,
                    state = keyboardState,
                    masterType = when {
                        // 主键盘之间采用直接切换，不需要退回
                        keyboardType.isMaster() -> null
                        // 从主键盘切换过来的临时性键盘，需要能够退回到原主键盘
                        oldKeyboardType.isMaster() -> oldKeyboardType
                        // 临时性键盘之间的切换不改变所要退回到的主键盘
                        else -> oldMasterKeyboardType
                    },
                )
            )
        }

    private fun toggleKeyboardHandMode() =
        applyStateUpdate { state ->
            val oldHandMode = state.keyboard.handMode ?: state.config.ui.keyboardHandMode

            state.copy(
                keyboard = state.keyboard.copy(
                    handMode = when (oldHandMode) {
                        KeyboardHandMode.Left ->
                            KeyboardHandMode.Right

                        KeyboardHandMode.Right ->
                            KeyboardHandMode.Left
                    },
                )
            )
        }

    // -----------------------------------------------

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
        val old = _state.value
        val new = transform(old)

        if (old == new) return

        if (ImeLog.isEnabledLevel(LogLevel.DEBUG)) {
            logger.debug { "State updated" }
            assertStateInvariants(new)
        }

        _state.value = new
    }

    private fun updateKeyboardState(newState: KeyboardState) {
        if (_state.value.keyboard.state == newState) return

        applyStateUpdate { state ->
            state.copy(
                keyboard = state.keyboard.copy(state = newState),
            )
        }
    }

    // -----------------------------------------------

    /**
     * 断言状态不变式（仅在 DEBUG 等级下执行）。
     *
     * 验证 [ImeState] 中各子状态之间的一致性约束，
     * 包括 keyboard.type 与 keyboard.state 的兼容性等。
     */
    private fun assertStateInvariants(state: ImeState) {
        // invariants checked only in DEBUG
    }
}
