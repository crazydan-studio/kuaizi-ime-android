# 键盘视图模型

`KeyboardViewModel` 划归 `:ui` 模块的 `viewmodel/` 包，作为 UI 层的协调中心，桥接 Compose UI 组件与 `:engine` 引擎。ViewModel 将 UI 手势（`InputGesture`）转换为引擎意图（`ImeIntent`），暴露引擎状态（`StateFlow<ImeState>`）供 Compose 订阅，管理手势反馈状态（`GestureFeedbackState`），提供运行时布局模式切换（`KeyboardLayoutMode`），以及集成输入动作播放器（`InputActionPlayer`）。ViewModel 仅依赖引擎核心模型和公开 API，平台级职责由 `:app` 模块承担，确保 `:ui` 作为纯 UI 库可被第三方应用即插即用。

---

## 1 `KeyboardViewModel` 定位

### 1.1 模块归属

`KeyboardViewModel` 属于 `:ui` 模块的 `viewmodel/` 包，是 UI 层（UI Layer + ViewModel Layer）的核心组件。

**归属理由**：

| 理由 | 说明 |
|------|------|
| **职能本质是 UI 协调** | ViewModel 将 UI 手势（`InputGesture`）转换为引擎意图（`ImeIntent`），暴露引擎状态（`StateFlow<ImeState>`）供 Compose 订阅，管理 `KeyboardLayoutMode` 切换和布局状态缓存——这些职能完全属于 UI 层 |
| **仅依赖引擎公开 API** | ViewModel 仅持有 `ImeEngine` 引用，使用其 `handleIntent()`、`state`、`effect`、`updateConfig()` 等公开 API，不依赖引擎内部实现 |
| **第三方应用需要** | 任何引入 `:engine` + `:ui` 的第三方应用都需要 `KeyboardViewModel` 来驱动 UI。如果 ViewModel 在 `:app`，第三方应用必须自行实现等价组件，违背「即插即用」的设计目标 |
| **集成组件的直接搭档** | `KeyboardHost`、`KeyboardInputActionPlayerHost` 等集成组件均以 ViewModel 为交互入口，二者同属 UI 层、同生同灭 |

### 1.2 不属于 ViewModel 的职责

以下职责由 `:app` 模块承担，`KeyboardViewModel` 不参与：

| 职责 | 承担者 | 说明 |
|------|--------|------|
| 创建 `ImeEngine` | `:app`（`IMEService`） | `ImeEngine.create()` 需要 `ImeDictProvider`（Android `Context` 依赖），由应用层创建后传入 ViewModel |
| 管理 `InputConnectionBridge` | `:app`（`IMEService`） | 桥梁直接挂载到 `ImeEngine`，与 ViewModel 无关；`IMEService` 在 `onStartInput`/`onDestroy` 时管理桥梁生命周期 |
| 配置持久化 | `:app`（`ConfigDataStore`） | DataStore 读写是应用层职责；ViewModel 仅通过 `engine.updateConfig()` 修改运行时配置 |
| 系统输入法服务生命周期 | `:app`（`IMEService`） | `InputMethodService` 的 `onCreate`/`onDestroy`/`onStartInput` 回调属于平台层 |

`KeyboardViewModel` 与 `:app` 模块的交互仅通过构造参数注入的 `ImeEngine` 完成。ViewModel 暴露的 API 分为三类：第一类是状态读取（`state`、`config`、`layoutMode`、`feedbackState`、`actionPlayer`），由集成组件通过 `collectAsState()` 订阅；第二类是意图分发（`handleGesture()`、`handleIntent()`），由集成组件的用户交互回调触发；第三类是运行时配置（`updateConfig()`、`setKeyboardLayoutMode()`、`updateKeyLayoutState()` 等），由集成组件的布局回调触发。`IMEService` 在 `onCreateInputView()` 中通过 `KeyboardViewModel.Factory(engine)` 注入预创建的引擎，ViewModel 不感知桥梁、持久化和服务生命周期。

---

## 2 完整类定义

```kotlin
package org.crazydan.studio.ime.ui.viewmodel

/**
 * 键盘视图模型，`:ui` 模块的 UI 协调中心。
 *
 * 桥接 Compose UI 组件与 `:engine` 引擎：
 * - 将 UI 手势（InputGesture）转换为引擎意图（ImeIntent）
 * - 暴露引擎状态（StateFlow<ImeState>）供 Compose 订阅
 * - 管理手势反馈状态（GestureFeedbackState）
 * - 管理运行时布局模式（KeyboardLayoutMode）
 * - 维护本地工具列表状态（StateFlow<ToolListState>）
 * - 维护弹出提示状态（StateFlow<PopupTipState?>），订阅引擎 ImeEffect 通道
 * - 派生 isInputting 状态，控制 ToolListPanel/InputListPanel 互斥切换
 * - 提供输入动作播放器（InputActionPlayer）
 * - 缓存面板布局状态供播放器坐标解析
 * - 在 gestureToIntent() 中直接处理音效、触觉和按键弹出提示
 *
 * ViewModel 仅依赖引擎核心模型和播放器接口。
 * 平台级职责（ImeEngine 创建、InputConnectionBridge 管理、配置持久化）
 * 均由 `:app` 模块承担。
 */
class KeyboardViewModel(
    private val engine: ImeEngine,
    private val audioPlayer: AudioPlayer? = null,
    private val hapticPlayer: HapticPlayer? = null,
) : ViewModel() {

    // ─── 状态暴露 ────────────────────────────────────────────────

    /** 引擎状态，供 Compose 订阅 */
    val state: StateFlow<ImeState> = engine.state

    /** 当前 ImeConfig 快照，便于 UI 组件快速访问 */
    val config: ImeConfig get() = state.value.config

    // ─── 工具列表状态 ────────────────────────────────────────────

    /**
     * 工具列表状态，由 ViewModel 本地维护。
     *
     * 根据 keyboard.type、keyboard.state 和收藏功能门控（favoriteInputEnabled / favoriteClipEnabled）动态配置工具项。
     * 不属于 ImeState，避免引擎维护 UI 层的展示状态。
     * 当 keyboard.type 或 keyboard.state 变更时，自动重新计算工具列表。
     */
    private val _toolListState = MutableStateFlow(ToolListState(emptyList()))
    val toolListState: StateFlow<ToolListState> = _toolListState.asStateFlow()

    // ─── 弹出提示状态 ────────────────────────────────────────────

    /**
     * 弹出提示状态，由 ViewModel 本地维护。
     *
     * 订阅引擎的 ImeEffect 通道，收到 ImeEffect.PopupTip 效果后更新此状态，
     * 并启动自动 dismiss 定时器。
     * 弹出提示属于一次性效果而非持续性状态，
     * 因此不应由 ImeState 管理。
     *
     * 支持 Message 和 Action 两种类型：
     * - Message：纯文本信息提示，超时后自动消失
     * - Action：可点击提示，含操作按钮，点击后触发 ImeIntent
     */
    private val _popupTipState = MutableStateFlow<PopupTipState?>(null)
    val popupTipState: StateFlow<PopupTipState?> = _popupTipState.asStateFlow()

    /**
     * 是否正在输入，由 inputList.hasPending 直接派生。
     *
     * 不属于 ImeState，避免引擎维护 UI 层的展示状态。
     * 控制 ToolListPanel 和 InputListPanel 的互斥切换。
     */
    val isInputting: Boolean
        get() = state.value.inputList.hasPending

    // ─── 布局模式 ────────────────────────────────────────────────

    /**
     * 运行时布局模式。
     *
     * 由 ViewModel 管理，KeyboardHost 订阅此状态决定组件部署方式。
     * 支持 Stacked（堆叠）和 Separated（分离）两种模式，
     * 可在运行时通过 setKeyboardLayoutMode() 动态切换。
     * 切换时组件按实例策略表重新部署，所有状态通过
     * GestureFeedbackState 和 ImeState 保持连续。
     */
    private val _layoutMode = MutableStateFlow<KeyboardLayoutMode>(KeyboardLayoutMode.Stacked)
    val layoutMode: StateFlow<KeyboardLayoutMode> = _layoutMode.asStateFlow()

    fun setKeyboardLayoutMode(mode: KeyboardLayoutMode) {
        _layoutMode.value = mode
    }

    // ─── 手势反馈状态 ────────────────────────────────────────────

    /**
     * 手势反馈状态，独立于 ImeState，使用归一化坐标。
     *
     * 由 GestureInputPanel 的手势事件驱动更新，
     * 供 GestureFeedbackPanel 消费渲染。
     */
    val feedbackState = GestureFeedbackState()

    // ─── 输入动作播放器 ──────────────────────────────────────────

    /**
     * 输入动作播放器，坐标无关。
     *
     * 通过 viewModel.handleIntent() 驱动引擎状态转换，
     * 通过 feedbackState 驱动手势反馈动画。
     */
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

    // ─── 布局状态缓存 ────────────────────────────────────────────

    /**
     * 当前面板布局状态缓存，供 ComposeInputActionPositionResolver 使用。
     *
     * 布局状态由各面板组件的 onLayoutStateChanged 回调更新。
     */
    private var _currentKeyLayoutState: KeyLayoutState? = null
    private var _currentCandidateLayoutState: CandidateListLayoutState? = null
    private var _currentInputListLayoutState: InputListLayoutState? = null

    fun updateKeyLayoutState(state: KeyLayoutState) {
        _currentKeyLayoutState = state
    }

    fun updateCandidateLayoutState(state: CandidateListLayoutState) {
        _currentCandidateLayoutState = state
    }

    fun updateInputListLayoutState(state: InputListLayoutState) {
        _currentInputListLayoutState = state
    }

    // ─── 手势与意图处理 ──────────────────────────────────────────

    /**
     * 处理输入手势。
     *
     * 1. 同步处理交互反馈（按键弹出提示、音效、触觉）
     * 2. 将 InputGesture（UI 面板的输出）转换为 ImeIntent（引擎的输入）
     * 3. 委托引擎执行 reduce
     *
     * 交互反馈在 gestureToIntent() 中同步处理，不经过 engine → SharedFlow → collect
     * 的异步路径，确保反馈与手势同步、低延迟。
     */
    fun handleGesture(gesture: InputGesture) {
        val intent = gestureToIntent(gesture)
        engine.handleIntent(intent)
    }

    /**
     * 处理意图。
     *
     * 直接发送 ImeIntent 到引擎，适用于：
     * - UI 组件的直接操作（如候选选择、光标移动、键盘切换）
     * - InputActionPlayer 的程序化输入
     * - PopupTipState.Action 的操作按钮点击
     * - 配置变更（UpdateConfig）
     */
    fun handleIntent(intent: ImeIntent) {
        engine.handleIntent(intent)
    }

    // ─── 配置修改 ────────────────────────────────────────────────

    /**
     * 更新运行时配置。
     *
     * 委托引擎更新 ImeConfig，UI 通过 ImeState.config 自动同步。
     * 注意：此方法仅修改运行时配置，不做持久化。
     * 持久化由 `:app` 模块的 ConfigDataStore 负责。
     */
    fun updateConfig(transform: (ImeConfig) -> ImeConfig) {
        engine.updateConfig(transform)
    }

    // ─── 生命周期 ────────────────────────────────────────────────

    init {
        // 订阅引擎副作用通道（仅处理 PopupTip 领域事件）
        viewModelScope.launch {
            engine.effect.collect { effect ->
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
                }
            }
        }

        // 订阅引擎状态变更，动态更新工具列表
        viewModelScope.launch {
            engine.state
                .map { state -> state.keyboard.type }
                .distinctUntilChanged()
                .collect { keyboardType ->
                    _toolListState.value = computeToolList(keyboardType)
                }
        }
    }

    override fun onCleared() {
        super.onCleared()
        actionPlayer.stop()           // 停止播放器动画
        feedbackState.clear()          // 清理手势反馈状态
    }

    // ─── 内部实现 ────────────────────────────────────────────────

    private var popupTipDismissJob: Job? = null

    /**
     * 启动自动 dismiss 定时器。
     *
     * 新的定时器会取消前一个定时器，确保同时只显示一条提示。
     */
    private fun dismissPopupTipAfter(timeoutMs: Long) {
        popupTipDismissJob?.cancel()
        popupTipDismissJob = viewModelScope.launch {
            delay(timeoutMs)
            _popupTipState.value = null
        }
    }

    /**
     * 将手势转换为引擎意图，同步处理交互反馈。
     *
     * 在转换意图的同时，直接同步处理音效、触觉和按键弹出提示。
     * 这些反馈完全由手势类型决定，无需等待引擎的 reduce 结果。
     * 引擎的 ImeEffect 通道仅承载 PopupTip 领域事件。
     */
    private fun gestureToIntent(gesture: InputGesture): ImeIntent {
        // 1. 处理按键弹出提示
        handleKeyPopupTip(gesture)

        // 2. 处理音效和触觉反馈
        when (gesture) {
            is InputGesture.Tap -> {
                playAudio(AudioType.KeyPress)
                playHaptic(HapticType.LightTap)
            }
            is InputGesture.LongPress -> {
                playHaptic(HapticType.HeavyTap)
            }
            is InputGesture.Swipe -> {
                playAudio(AudioType.Slip)
                playHaptic(HapticType.MediumTap)
            }
            is InputGesture.Flip -> {
                playAudio(AudioType.PageFlip)
                playHaptic(HapticType.MediumTap)
            }
            is InputGesture.CandidateTap -> {
                playAudio(AudioType.CandidateSelect)
                playHaptic(HapticType.LightTap)
            }
        }

        // 3. 转换为引擎意图
        return when (gesture) {
            is InputGesture.Tap -> ImeIntent.PressKey(gesture.key, KeyGesture.Tap)
            is InputGesture.LongPress -> ImeIntent.PressKey(gesture.key, KeyGesture.LongPress)
            is InputGesture.Swipe -> ImeIntent.PressKey(gesture.endKey, KeyGesture.Swipe)
            is InputGesture.Flip -> ImeIntent.PressKey(
                gesture.startKey,
                KeyGesture.Flip(gesture.direction),
            )
            is InputGesture.CandidateTap -> ImeIntent.SelectCandidate(
                /* 根据 gesture.candidateIndex 从当前候选列表中获取 */
            )
        }
    }

    // ─── 交互反馈 ────────────────────────────────────────────────

    /**
     * 处理按键弹出提示。
     *
     * 仅 Tap 和 LongPress 手势触发按键弹出提示，受 UiConfig.keyPopupTipsEnabled
     * 和 RuntimeConfig.keyPopupTipsEnabled 双重控制。
     */
    private fun handleKeyPopupTip(gesture: InputGesture) {
        if (gesture !is InputGesture.Tap && gesture !is InputGesture.LongPress) return
        if (!config.ui.keyPopupTipsEnabled) return
        if (config.runtime.keyPopupTipsEnabled == false) return

        _keyPopupTipState.value = KeyPopupTipState(
            key = gesture.key,
            show = true,
        )
    }

    private fun playAudio(type: AudioType) {
        if (config.ui.audioFeedbackEnabled) {
            audioPlayer?.play(type)
        }
    }

    private fun playHaptic(type: HapticType) {
        if (config.ui.hapticFeedbackEnabled) {
            hapticPlayer?.play(type)
        }
    }

    private fun computeToolList(keyboardType: KeyboardType): ToolListState {
        val tools = when (keyboardType) {
            KeyboardType.Pinyin, KeyboardType.Latin -> listOf(
                // 全选、复制、粘贴、剪贴板、撤销、重做
            )
            KeyboardType.Editor -> listOf(
                // 全选、复制、剪切、粘贴、撤销
            )
            KeyboardType.Symbol, KeyboardType.Emoji -> listOf(
                // 全选、复制、粘贴
            )
            KeyboardType.Number, KeyboardType.Math -> listOf(
                // 全选、复制、粘贴
            )
            else -> emptyList()
        }
        return ToolListState(tools)
    }

    // ─── 工厂 ────────────────────────────────────────────────────

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
```

`onCleared()` 确保 ViewModel 被清除时释放所有资源：停止 InputActionPlayer 的动画帧循环（通过取消其内部协程）、清理 GestureFeedbackState 的临时状态。由于 InputActionPlayer 的 scope 基于 viewModelScope，其协程在 ViewModel 清除时自动取消——onCleared() 中的 stop() 调用确保播放器内部状态标记已清理，而非仅依赖协程取消的隐式行为。

ViewModel 清除 → onCleared() → actionPlayer.stop() → FrameTimer 协程取消 → feedbackState.clear() → 手指指示器/轨迹清除。每一步都是显式调用，不依赖最终化或 GC。

---

## 3 `PopupTipState` 弹出提示状态

`PopupTipState` 是弹出提示的状态模型，由 `KeyboardViewModel` 本地维护，对应引擎 `ImeEffect.PopupTip` 副作用通道中的 `Message` 和 `Action` 两种类型。

```kotlin
/**
 * 弹出提示状态，由 KeyboardViewModel 管理。
 *
 * 引擎通过 ImeEffect.PopupTip 发出一次性效果信号，
 * ViewModel 订阅后更新此状态，驱动 PopupTipPanel 显示。
 *
 * 两种类型：
 * - Message：纯文本信息提示，超时后自动消失
 * - Action：可点击提示，含操作按钮，点击后触发 ImeIntent
 */
sealed class PopupTipState {

    /**
     * 纯文本信息提示。
     *
     * 用于展示短暂的反馈信息，如：
     * - 输入字符反馈（如输入了某个拼音字母）
     * - 键盘切换提示（如"已切换到拉丁键盘"）
     * - 编辑操作反馈（如"已复制"）
     *
     * 超时后自动消失，无需用户操作。
     */
    data class Message(
        /** 提示消息内容 */
        val message: String,
        /** 自动消失超时时间（毫秒），默认 3000ms */
        val timeoutMs: Long = 3000L,
    ) : PopupTipState()

    /**
     * 可点击操作提示。
     *
     * 用于展示带操作按钮的提示信息，如：
     * - 粘贴内容提示（检测到剪贴板内容，点击"粘贴"触发粘贴操作）
     * - 保存收藏提示（输入内容可收藏，点击"收藏"触发保存操作）
     *
     * persistent=false 时超时后自动消失，persistent=true 时
     * 保持显示直到用户开始输入（由 isInputting 变更触发清除）。
     * 点击操作按钮后触发关联的 ImeIntent。
     */
    data class Action(
        /** 提示消息内容 */
        val message: String,
        /** 操作按钮标签 */
        val actionLabel: String,
        /** 点击操作按钮后触发的 ImeIntent */
        val action: ImeIntent,
        /** 是否持久显示（直到用户开始输入） */
        val persistent: Boolean = false,
        /** 自动消失超时时间（毫秒），默认 5000ms；persistent=true 时此参数不生效 */
        val timeoutMs: Long = 5000L,
    ) : PopupTipState()
}
```

`PopupTipState` 与引擎 `ImeEffect.PopupTip` 的类型一一对应。`Message` 类型用于短暂的反馈信息，展示后按 `timeoutMs` 自动消失（默认 3000ms），适用于输入字符反馈、键盘切换提示、编辑操作反馈等场景。`Action` 类型用于带操作按钮的提示信息，用户可以点击操作按钮触发关联的 `ImeIntent`，适用于粘贴内容提示、保存收藏提示等需要用户主动操作的场景。

`Action` 类型的 `persistent` 参数控制提示的显示策略：`persistent=false`（默认）时，提示在 `timeoutMs`（默认 5000ms）后自动消失，与 `Message` 类型的行为一致但超时时间更长；`persistent=true` 时，提示持续显示直到用户开始输入（`isInputting` 变为 `true`），适用于重要的操作提示（如检测到剪贴板内容）不应被忽略的场景。`PopupTipPanel` 在渲染 `Action` 类型提示时，显示消息文本和操作按钮，按钮点击回调调用 `viewModel.handleIntent(action)` 将关联的 `ImeIntent` 发送到引擎。

---

## 4 `ToolListState` 工具列表

`ToolListState` 是工具列表的状态模型，由 `KeyboardViewModel` 本地维护，不属于 `ImeState`。

```kotlin
/**
 * 工具列表状态，由 KeyboardViewModel 维护。
 *
 * 工具栏内容根据 keyboard.type 和收藏功能门控（favoriteInputEnabled / favoriteClipEnabled）动态配置。
 * 不属于 ImeState，避免引擎维护 UI 层的展示状态。
 */
data class ToolListState(
    /** 工具项列表 */
    val tools: List<ToolItem>,
)

/**
 * 工具项。
 *
 * 每个工具项对应一个可点击的功能按钮，
 * 点击后发送关联的 ImeIntent。
 */
data class ToolItem(
    /** 按钮标签 */
    val label: String,
    /** 按钮图标 */
    val icon: ImageVector?,
    /** 点击时发送的意图 */
    val intent: ImeIntent,
    /** 是否禁用 */
    val disabled: Boolean = false,
)
```

`KeyboardViewModel` 通过 `computeToolList(keyboardType: KeyboardType)` 方法根据当前键盘类型动态计算工具列表。工具列表的内容随 `keyboard.type` 和收藏功能门控（`EngineConfig.favoriteInputEnabled` / `EngineConfig.favoriteClipEnabled`）变化：拼音和拉丁键盘提供完整的编辑功能键（全选、复制、粘贴、剪贴板、撤销、重做）和键盘切换键；编辑键盘专注于编辑操作（全选、复制、剪切、粘贴、撤销）；符号、表情、数字和数学键盘提供基本编辑功能（全选、复制、粘贴）和返回主键盘的切换键。`ToolItem.disabled` 字段用于在某些状态下禁用特定工具（如输入列表为空时禁用复制），`ToolListPanel` 渲染禁用工具时降低视觉权重并阻止点击。

`toolListState` 仅依赖 keyboard.type 而非完整 ImeState。使用 distinctUntilChanged 过滤后，仅在键盘类型切换时才重算工具列表，避免每次按键输入都触发不必要的重算。

---

## 5 `ImeEffect` 订阅与处理

ViewModel 直接通过 `engine.effect.collect` 订阅 SharedFlow，仅处理 `PopupTip` 领域事件。音效和触觉反馈已完全移至 `gestureToIntent()` 中同步处理，不再经由 `ImeEffect` 通道。

```kotlin
// 在 KeyboardViewModel init 中
viewModelScope.launch {
    engine.effect.collect { effect ->
        when (effect) {
            is ImeEffect.PopupTip.Message -> {
                _popupTipState.value = PopupTipState.Message(...)
                dismissPopupTipAfter(effect.timeoutMs)
            }
            is ImeEffect.PopupTip.Action -> {
                _popupTipState.value = PopupTipState.Action(...)
                if (!effect.persistent) {
                    dismissPopupTipAfter(effect.timeoutMs)
                }
            }
        }
    }
}
```

由于不再需要处理高频的 `PlayAudio`/`PlayHaptic` 信号，`channelFlow` conflate 模式已去除，订阅逻辑简化为直接的 `collect` 调用。ViewModel 在独立的协程中消费 `ImeEffect`，不参与 `ImeState` 的 `StateFlow` 订阅链——只有 `PopupTipState`（`MutableStateFlow`）被更新后，弹出提示面板才会局部重组。

### 5.1 `PopupTip.Message` 处理

当 ViewModel 收到 `ImeEffect.PopupTip.Message` 时，将 `_popupTipState` 更新为 `PopupTipState.Message`，并启动自动 dismiss 定时器（`timeoutMs` 默认 3000ms）。定时器超时后将 `_popupTipState` 置为 `null`，`PopupTipPanel` 检测到 `null` 后隐藏提示。新的 `PopupTip` 效果到来时，取消前一个定时器并启动新的定时器，确保同时只显示一条提示。

`Message` 类型用于短暂的反馈信息，如输入字符反馈（引擎在 reduce 中检测到按键输入后发出）、键盘切换提示（引擎在 `SwitchKeyboard` reduce 中发出"已切换到拉丁键盘"）、编辑操作反馈（引擎在 `ClipboardPaste` reduce 中发出"已粘贴"）。这些提示不需要用户操作，仅作为输入反馈短暂显示后自动消失。

### 5.2 `PopupTip.Action` 处理

当 ViewModel 收到 `ImeEffect.PopupTip.Action` 时，将 `_popupTipState` 更新为 `PopupTipState.Action`，保存 `message`、`actionLabel`、`action: ImeIntent` 和 `persistent` 参数。`persistent=false` 时启动自动 dismiss 定时器（`timeoutMs` 默认 5000ms），`persistent=true` 时不启动定时器，提示保持显示直到用户开始输入。

`PopupTipPanel` 在渲染 `Action` 类型提示时，显示消息文本和操作按钮。操作按钮的点击回调调用 `viewModel.handleIntent(action)`，将关联的 `ImeIntent` 发送到引擎。例如，引擎检测到剪贴板有新内容时，发出 `ImeEffect.PopupTip.Action(message="检测到剪贴板内容", actionLabel="粘贴", action=ImeIntent.PasteClipboard, persistent=true)`，`PopupTipPanel` 显示提示和"粘贴"按钮，用户点击按钮后触发 `ImeIntent.PasteClipboard`，引擎在 reduce 中执行粘贴操作并输出结果到编辑器。

### 5.3 收藏确认处理

收藏确认通过 `PopupTip.Action` 实现，而非独立的 `ImeEffect` 类型。当 `ImeIntent.CommitInput` 处理完成后，引擎检查已提交文本是否已在收藏列表中。若未收藏，引擎发射 `ImeEffect.PopupTip.Action(message="可收藏内容", actionLabel="收藏", action=ImeIntent.SaveFavorite(InputFavorite(text=committedText)), persistent=false)`，ViewModel 收到后显示带「收藏」按钮的提示条，用户点击即可保存。已收藏的内容不触发任何提示。这种设计将收藏确认统一到 `PopupTip.Action` 体系中，无需额外的 `ImeEffect` 子类型。

---

## 6 `GestureFeedbackState` 生产者-消费者

`GestureFeedbackState` 采用生产者-消费者模式，多个组件写入（生产）反馈数据，`GestureFeedbackPanel` 读取（消费）并渲染。所有坐标数据使用归一化形式存储，确保同一份数据可以在不同尺寸的面板实例上正确渲染。

| 状态字段 | 生产者 | 消费者 | 坐标类型 |
|----------|--------|--------|----------|
| `touchTrailPoints` | `GestureInputPanel`（用户手势，实时积累触摸点）、`InputActionPlayer`（程序化播放，含 `InputActionPathInterpolator` 插值路径） | `GestureFeedbackPanel`（反归一化后绘制轨迹） | 归一化 `[0,1]x[0,1]` |
| `pressedKeys` | `GestureInputPanel`（手势过程中更新）、`InputActionPlayer`（播放时更新） | `GestureFeedbackPanel`（通过 `KeyLayoutState` 映射按键位置后绘制高亮） | 语义标识，非坐标 |
| `fingerIndicator` | `InputActionPlayer`（播放时驱动） | `GestureFeedbackPanel`（反归一化后绘制手指指示器）、`CandidateListPanel`/`InputListPanel`/`ToolListPanel`（内建指示器） | 归一化 `[0,1]x[0,1]` |

`touchTrailPoints` 有两种写入来源：`GestureInputPanel` 在用户手势过程中将触摸事件坐标归一化后逐点追加（`addTouchTrailPoint()`），`InputActionPlayer` 在播放滑行动作时通过 `InputActionPathInterpolator` 生成完整插值路径后一次性写入（`setTouchTrailPoints()`）。`pressedKeys` 由 `GestureInputPanel` 和 `InputActionPlayer` 共同写入，在手势或动作开始时设置按下的按键集合，结束时清除。`fingerIndicator` 仅由 `InputActionPlayer` 写入，用于在程序化播放期间显示虚拟手指的位置和状态，`GestureFeedbackPanel` 订阅后在 Row 3 区域绘制，`CandidateListPanel` 等面板通过各自的 `indicatorState` 参数在内部绘制。

`GestureFeedbackState` 与 `ImeState` 的分离确保了反馈状态的高频更新不会触发 `ImeState` 的变更，避免不必要的按键面板重组。反馈状态的更新频率可达每帧（16ms@60fps），而 `ImeState` 的更新频率仅在用户操作或引擎 reduce 时触发，两者解耦后互不影响。

---

## 7 `KeyboardHost` 集成组件

`KeyboardHost` 是顶层集成组件，通过 `viewModel.layoutMode` 统一两种布局模式的入口。`KeyboardHost` 订阅 `viewModel.state`、`viewModel.layoutMode`、`viewModel.feedbackState`、`viewModel.popupTipState`、`viewModel.toolListState`，根据布局模式选择 `StackedLayout` 或 `SeparatedLayout`。

```kotlin
/**
 * 键盘宿主组件，顶层集成组件。
 *
 * 通过 KeyboardLayoutMode 参数统一两种布局模式的入口。
 * 支持运行时动态切换 KeyboardLayoutMode，切换时按实例策略表重新部署组件。
 */
@Composable
fun KeyboardHost(
    viewModel: KeyboardViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val layoutMode by viewModel.layoutMode.collectAsState()
    val feedbackState = viewModel.feedbackState
    val popupTipState by viewModel.popupTipState.collectAsState()
    val toolListState by viewModel.toolListState.collectAsState()
    var keyLayoutState by remember { mutableStateOf(KeyLayoutState()) }

    KeyboardTheme(type = state.config.ui.keyboardThemeType) {
        when (layoutMode) {
            is KeyboardLayoutMode.Stacked -> StackedLayout(
                viewModel, state, feedbackState,
                keyLayoutState, popupTipState, toolListState,
            )
            is KeyboardLayoutMode.Separated -> SeparatedLayout(
                viewModel, state, feedbackState,
                keyLayoutState, popupTipState, toolListState,
                layoutMode as KeyboardLayoutMode.Separated,
            )
        }
    }
}
```

`StackedLayout` 中，所有组件集中在 Zone B，三层面板叠加共享 Row 3 空间。`KeyLayoutPanel` 部署在 Zone B Row 3，`GestureFeedbackPanel` 和 `GestureInputPanel` 叠加在其上方。Row 1 承载 `CandidateListPanel` 和 `PopupTipPanel` 的叠加，`PopupTipPanel` 根据 `popupTipState` 显示 `Message` 或 `Action` 类型的提示。Row 2 根据 ViewModel 的 `isInputting` 派生状态互斥切换 `ToolListPanel`（`toolListState`）和 `InputListPanel`。

`SeparatedLayout` 中，Zone A 承载 `KeyLayoutPanel` 和 `GestureFeedbackPanel` 的叠加，Zone B 包含三行结构。Row 3 被进一步划分为三列：左列和右列放置功能按钮，中列承载 `GestureFeedbackPanel` 和 `GestureInputPanel` 的叠加。

`PopupTipPanel` 的渲染逻辑：当 `popupTipState` 为 `PopupTipState.Message` 时，显示文本信息并在超时后自动隐藏；当 `popupTipState` 为 `PopupTipState.Action` 时，显示文本信息和操作按钮（`actionLabel`），按钮点击回调调用 `viewModel.handleIntent(action)`。`persistent=true` 的 `Action` 提示保持显示直到 `isInputting` 变为 `true`。`PopupTipPanel` 叠加在 `CandidateListPanel` 之上，提示可见时候选列表不响应交互。

各面板的用户交互均通过 `viewModel.handleGesture()` 或 `viewModel.handleIntent()` 分发意图。`KeyLayoutPanel` 的 `onLayoutStateChanged` 回调更新 `keyLayoutState` 局部变量，同时调用 `viewModel.updateKeyLayoutState()` 更新 ViewModel 的布局状态缓存，供 `InputActionPlayer` 的坐标解析器使用。

---

## 8 `IMEService` 装配流程

`:app` 模块的 `IMEService` 负责创建引擎、挂载桥梁、注入 ViewModel，是 ViewModel 与平台层交互的唯一入口。

```kotlin
class IMEService : InputMethodService() {
    private var engine: ImeEngine? = null
    private var bridge: InputConnectionBridge? = null
    private var audioPlayer: AndroidAudioPlayer? = null
    private var hapticPlayer: AndroidHapticPlayer? = null
    private var composeView: ComposeView? = null

    override fun onCreate() {
        super.onCreate()
        // 创建引擎（需要 Android Context 和字典提供者）
        engine = ImeEngine.create(
            config = ImeConfig(),
            dictProvider = ImeSqliteDictProvider(this),
        )
        // 创建并挂载编辑器桥梁（与 ViewModel 无关）
        bridge = InputConnectionBridge { currentInputConnection }
        engine?.attachEditorBridge(bridge!!)
        // 创建感官反馈播放器
        audioPlayer = AndroidAudioPlayer(this)
        hapticPlayer = AndroidHapticPlayer(this)
    }

    override fun onCreateInputView(): View {
        val engine = this.engine ?: error("Engine not initialized")
        val audio = this.audioPlayer
        val haptic = this.hapticPlayer
        return ComposeView(this).also { composeView = it }.apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
            )
            setContent {
                val viewModel: KeyboardViewModel = viewModel(
                    factory = KeyboardViewModel.Factory(engine, audio, haptic)
                )
                // KeyboardHost 已包含候选栏 + 输入栏 + 工具列表 + 三层面板叠加 + 弹出提示
                KeyboardHost(viewModel = viewModel)
            }
        }
    }

    override fun onStartInputView(editorInfo: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(editorInfo, restarting)
        engine?.start(StartupConfig(
            imeSubtype = IMESubtype.from(this),
            screenOrientation = ScreenOrientation.from(this),
            editorInputType = EditorInputType.from(editorInfo),
        ))
    }

    override fun onCurrentInputMethodSubtypeChanged(subtype: InputMethodSubtype?) {
        super.onCurrentInputMethodSubtypeChanged(subtype)
        engine?.start(StartupConfig(
            imeSubtype = IMESubtype.from(this),
            screenOrientation = ScreenOrientation.from(this),
            editorInputType = null,
        ))
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        super.onFinishInputView(finishingInput)
        engine?.close()
    }

    override fun onDestroy() {
        audioPlayer?.release()
        audioPlayer = null
        hapticPlayer = null
        engine?.detachEditorBridge(bridge!!)
        engine?.destroy()
        engine = null
        bridge = null
        composeView?.disposeComposition()
        composeView = null
        super.onDestroy()
    }
}
```

装配流程的关键步骤：

1. **引擎创建**：`IMEService.onCreate()` 中调用 `ImeEngine.create()` 创建引擎实例，传入 `ImeConfig` 和 `ImeSqliteDictProvider`。引擎创建是应用层职责，因为 `ImeDictProvider` 需要 Android `Context` 访问 SQLite 数据库。

2. **桥梁挂载**：`IMEService.onCreate()` 中创建 `InputConnectionBridge`，传入 `currentInputConnection` 的提供者，然后通过 `engine.attachEditorBridge()` 挂载。桥梁的挂载与 ViewModel 无关——桥梁直接与引擎交互，将 `EditorAction` 分发到 `InputConnection`。

3. **播放器创建**：`IMEService.onCreate()` 中创建 `AndroidAudioPlayer` 和 `AndroidHapticPlayer`，传入 `Context` 用于加载音频资源和获取 `Vibrator` 服务。播放器的生命周期与 `IMEService` 相同，不随 `InputConnection` 变更而重建。

4. **ViewModel 注入**：`IMEService.onCreateInputView()` 中通过 `KeyboardViewModel.Factory(engine, audio, haptic)` 创建 ViewModel 工厂，将预创建的引擎和播放器注入 ViewModel。ViewModel 通过引擎的公开 API（`handleIntent()`、`state`、`effect`、`updateConfig()`）与引擎交互，通过播放器接口与平台层交互，不感知桥梁和平台生命周期。

5. **Compose 渲染**：`setContent {}` 中使用 `KeyboardHost(viewModel = viewModel)` 作为根组件，`KeyboardHost` 订阅 ViewModel 的所有状态，驱动 Compose 渲染完整的输入法界面。

6. **启动输入**：`IMEService.onStartInputView()` 中调用 `engine.start(startupConfig)` 启动输入法，传入从 `EditorInfo` 解析的 `StartupConfig`。`onCurrentInputMethodSubtypeChanged()` 中同样调用 `engine.start()`，但 `editorInputType` 为 `null` 以确保不覆盖已识别的编辑器类型。

7. **关闭与销毁**：`IMEService.onFinishInputView()` 中调用 `engine.close()` 关闭输入法（仅隐藏面板，状态保持不变）。`IMEService.onDestroy()` 中的清理遵循严格的顺序：先释放播放器资源，再断开桥梁，最后销毁引擎。`detachEditorBridge(bridge)` 必须在 `destroy()` 前显式调用，而非依赖 `destroy()` 自动注销，这是基于桥梁所有权原则——桥梁由 `IMEService` 创建和拥有，`attach`/`detach` 是宿主模块的对称操作，引擎仅是桥梁的消费者。`destroy()` 内部对 `_editorBridges` 仅执行防御性 `clear()`，不调用 `detachEditorBridge()` 的注销逻辑。ViewModel 的 `onCleared()` 仅清理自身资源（`actionPlayer.stop()` + `feedbackState.clear()`），不负责销毁引擎和播放器——引擎和播放器的生命周期由 `:app` 管理，比 ViewModel 更长（引擎和播放器在 `onCreate()` 中创建，ViewModel 在 `onCreateInputView()` 中创建）。
