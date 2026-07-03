# 引擎架构总览

`:engine` 是筷字输入法的引擎库，提供核心 IME 引擎能力。引擎库独立设计的目标是使输入法的逻辑层与 UI 和应用之间实现彻底分离与解耦，从而方便第三方定制自己的 UI、修改交互逻辑等。第三方应用只需引入 `:engine` 即可获得完整的输入法能力——拼音输入、滑行输入、候选选择、输入列表管理、撤销重做等——无需依赖系统 IME 服务或任何 UI 框架。

---

## 1. 模块定位与设计目标

| 定位 | 说明 |
|------|------|
| **逻辑与 UI 分离** | 引擎库独立设计的目标是使输入法的逻辑层与 UI 和应用之间实现分离、解耦，从而方便第三方定制自己的 UI、修改交互逻辑等 |
| **MVI 驱动** | 通过 `StateFlow<ImeState>` 暴露状态，通过 `SharedFlow<ImeEffect>` 发射副作用，通过 `ImeIntent` 接收操作，通过 `ImeEditorBridge` 输出编辑器操作 |
| **可嵌入** | 第三方应用只需引入 `:engine` 即可获得完整输入法能力，无需系统 IME 服务 |
| **可扩展** | 字典接口与实现分离（`ImeDictProvider`），编辑器桥接可自定义（`ImeEditorBridge`），收藏功能可裁剪（`favoriteInputEnabled` / `favoriteClipEnabled`） |
| **Fail Fast** | 非法操作（如禁用收藏后调用收藏功能）立即抛出异常而非静默忽略 |

引擎库的「逻辑与 UI 分离」定位意味着第三方应用可以完全用自定义 UI 替换 `:ui` 而不影响引擎功能，也可以仅引入 `:engine` 自行实现视图层和交互逻辑。唯一依赖 Android 的部分是字典 I/O（`ImeSqliteDictProvider` 使用 Room），但第三方可以提供自己的 `ImeDictProvider` 实现来消除 Android 依赖。

「MVI 驱动」定位是引擎与 UI 完全分离的技术基础。引擎不依赖任何 UI 框架，所有状态变更通过 `StateFlow` 暴露，所有用户操作通过 `ImeIntent` 接收，所有编辑器操作通过 `ImeEditorBridge` 输出。这种单向数据流使得引擎可以被任意 UI 框架（Compose、View、Web、游戏引擎等）消费，而不需要引擎感知 UI 的存在。

「可嵌入」定位使得 `:engine` 可以在多种场景下使用：作为系统输入法引擎、嵌入到应用内的自定义输入组件中、甚至作为纯 JVM 环境下的输入法逻辑核心。引擎的创建和销毁完全由宿主控制，不持有任何全局状态或单例。

「可扩展」定位通过三个扩展点实现：`ImeDictProvider` 允许替换整个字典层（例如使用远程字典服务替代本地 SQLite）；`ImeEditorBridge` 允许替换输出目标（例如接入 WebView 编辑器或游戏引擎文本框）；`EngineConfig.favoriteInputEnabled` / `EngineConfig.favoriteClipEnabled` 允许裁剪收藏功能（例如禁用剪贴板收藏以减少权限需求）。

「Fail Fast」定位确保引擎在运行时检测到非法操作时立即抛出异常，而非静默忽略或产生不确定行为。典型的非法操作包括：在 `EngineConfig.favoriteInputEnabled` 和 `EngineConfig.favoriteClipEnabled` 均为 false 时调用 `ImeIntent.SaveFavorite`、在无效状态下执行不合法的 `KeyboardStateTransition` 等。Fail Fast 原则帮助开发者在开发阶段尽早发现错误，避免错误在调用链中传播后难以定位。

---

## 2. 核心 API 面

引擎库的核心 API 面由六个核心类型构成，形成 `:engine` 与 `:ui`、`:app` 之间的核心契约。这六个类型是第三方应用使用引擎库的主要接口，也是引擎内部各组件协作的基础协议。

### 2.1 ImeEngine

`ImeEngine` 是引擎库的核心入口点，提供完整的输入法能力。引擎不依赖任何 UI 框架，通过 `StateFlow` 暴露状态，通过 `ImeIntent` 接收用户操作，通过 `ImeEditorBridge` 输出编辑器操作。完整类定义见本文档 §5。

### 2.2 ImeConfig

`ImeConfig` 是统一的运行时配置，包含引擎配置（`EngineConfig`）、UI 配置（`UiConfig`）和运行时配置（`RuntimeConfig`），三者各自在数据结构上明确隔离。引擎配置影响引擎的核心行为，UI 配置影响界面呈现和交互反馈，运行时配置承载不持久化的临时状态。库不内置配置持久化，所有配置通过 `ImeConfig` 在创建时或运行时设置，持久化是应用层的职责。完整定义见本文档 §6。

### 2.3 ImeIntent

用户意图的 sealed class 表达，所有用户操作统一为 `ImeIntent`，由 `ImeEngine.handleIntent()` 接收并处理。`InputGesture` 是输入面板的输出，`ImeIntent` 是引擎的输入，`KeyboardViewModel` 负责将前者转换为后者。

```kotlin
sealed class ImeIntent {
    // 按键意图
    data class PressKey(val key: InputKey, val gesture: KeyGesture) : ImeIntent()
    data class LongPressKey(val key: InputKey) : ImeIntent()

    // 候选意图
    data class SelectCandidate(val candidate: InputWord) : ImeIntent()
    data class PageCandidate(val direction: PageDirection) : ImeIntent()

    // 键盘切换
    data class SwitchKeyboard(val type: KeyboardType) : ImeIntent()

    // 输入列表意图
    data object CommitInput : ImeIntent()
    data object DeleteInput : ImeIntent()
    data object CleanInput : ImeIntent()
    data class MoveCursorTo(val index: Int) : ImeIntent()

    // 编辑操作意图
    data class PerformEdit(val action: EditorEditAction) : ImeIntent()

    // 剪贴板与收藏意图
    data class PasteClip(val text: String) : ImeIntent()
    data class SaveFavorite(val favorite: InputFavorite) : ImeIntent()

    // 剪贴板与收藏列表控制
    data object ShowClipList : ImeIntent()
    data object ShowFavoriteList : ImeIntent()
    data object CloseClipList : ImeIntent()
    data object CloseFavoriteList : ImeIntent()

    // 字典查询（异步 sideEffect）
    data class LoadCandidates(val pinyin: String) : ImeIntent()
    data class SetCandidates(val candidates: CandidateList) : ImeIntent()

    // 配置意图
    data class UpdateConfig(val config: ImeConfig) : ImeIntent()

    // 数据导入导出意图
    data object ExportUserData : ImeIntent()
    data class ImportUserData(val filePath: String) : ImeIntent()
}
```

`ImeIntent` 的设计遵循「意图与手势分离」原则：`ImeIntent` 表达业务语义（如「提交输入」），而非底层手势细节（如「手指抬起」）。手势到意图的映射由 `KeyboardViewModel` 完成，引擎只消费意图，不感知手势。这种分层使得引擎可以在不同的输入模式下复用相同的意图处理逻辑——无论是真实手指操作还是 `InputAction` 的程序化回放，最终都通过 `ImeIntent` 驱动引擎。

### 2.4 EditorAction

引擎的编辑器操作。`EditorAction` 由引擎内部的 `dispatchEditorAction()` 统一分发到 `ImeEditorBridge`，桥梁实现者无需理解 `EditorAction` 类型体系。

```kotlin
sealed class EditorAction {
    abstract val timestamp: Long

    data class CommitText(
        override val timestamp: Long,
        val text: String,
        val replacements: List<String>? = null,
    ) : EditorAction()

    data class RevokeCommit(override val timestamp: Long) : EditorAction()

    data class InsertPairedSymbols(
        override val timestamp: Long,
        val left: String,
        val right: String,
    ) : EditorAction()

    data class MoveCursor(
        override val timestamp: Long,
        val direction: CursorDirection,
    ) : EditorAction()

    data class SelectRange(
        override val timestamp: Long,
        val direction: CursorDirection,
    ) : EditorAction()

    data class PerformEdit(
        override val timestamp: Long,
        val action: EditorEditAction,
    ) : EditorAction()
}
```

`EditorAction` 的 sealed class 层次确保引擎在分发输出时穷举所有类型，编译期保证类型安全。每种输出类型携带时间戳，用于日志记录和调试追踪。`CommitText` 的 `replacements` 参数支持直输模式下的字符轮换——默认采用双击按键方式触发，桥梁实现需检查光标前文本是否匹配替换列表，匹配时执行替换而非插入。

### 2.5 ImeState

`ImeState` 是 MVI 架构中的单一状态树根节点，作为 `ImeEngine` 对外暴露的唯一状态源，所有 UI 组件通过 `StateFlow<ImeState>` 订阅状态驱动重组。不可变 `data class`，所有变更通过 `copy()` 生成新实例。

```kotlin
data class ImeState(
    val keyboard: Keyboard = Keyboard(),
    val inputList: InputList = InputList(),
    val candidateList: CandidateList = CandidateList(),
    val clipboard: Clipboard = Clipboard(),
    val favoriteList: FavoriteList = FavoriteList(),
    val toolListState: ToolListState = ToolListState(),
    val config: ImeConfig = ImeConfig(),
)
```

`ImeState` 的不可变性是线程安全的根本保证：`StateFlow.value` 的读写是原子的，所有状态变更在 `reduce` 中串行执行，不可变 `data class` 无需同步。UI 层通过 `collectAsState()` 订阅 `StateFlow<ImeState>`，状态变更自动驱动 Compose 重组，无需手动通知。副作用信号通过独立的 `SharedFlow<ImeEffect>` 通道发射，与 `ImeState` 完全分离。详细子状态设计见 [020-ImeState](020-ime-state.md)。

### 2.6 ImeEffect

引擎副作用信号，表达一次性效果。与 `ImeState` 的持续性字段不同，`ImeEffect` 通过 `SharedFlow<ImeEffect>` 发射——引擎发射信号后 UI 层在独立的 `collectEffect()` 协程中消费，不触发 `ImeState` 的变化。

```kotlin
sealed class ImeEffect {
    sealed class PopupTip : ImeEffect() {
        data class Message(
            val message: String,
            val timeoutMs: Long = 3000L,
        ) : PopupTip()

        data class Action(
            val message: String,
            val actionLabel: String,
            val action: ImeIntent,
            val persistent: Boolean = false,
            val timeoutMs: Long = 5000L,
        ) : PopupTip()
    }
}
```

`ImeEffect` 现仅承载 PopupTip 领域事件（如键盘切换提示、收藏确认、剪贴板检测），不再包含音效和触觉反馈信号。音效与触觉已完全交由 UI 层在 `gestureToIntent()` 中直接处理。`ImeEffect` 通过 `SharedFlow<ImeEffect>` 发射，配置 `extraBufferCapacity = 64` 以应对快速连续打字时的高频 `PopupTip`。UI 层在独立的协程中收集效果并消费，不触发 `ImeState` 的重组。详见 [065-交互反馈设计](065-audio-haptic-feedback.md)。

> **为什么 ImeEffect 与 ImeState 分离？**  
> ImeEffect 与 ImeState 分离避免了一次性效果触发 ImeState.copy() 和 StateFlow 发射导致的全局 UI 重组。每个 effect 通过独立的 SharedFlow 通道传递，UI 层在独立的协程中消费，不触发 ImeState 的变化。

---

## 3. MVI 数据流

引擎采用 MVI（Model-View-Intent）架构，实现严格的单向数据流。MVI 架构将引擎的输入、状态、副作用和输出四个维度明确分离，每个维度通过独立的类型和通道表达，确保数据流的可追踪性和可预测性。

### 3.1 数据流描述

MVI 数据流由四条通道构成，每条通道有明确的语义和方向：

- **输入通道**：`ImeIntent` → `ImeEngine.handleIntent()` → `reduce(state, intent)` → 新 `ImeState`。用户操作统一编码为 `ImeIntent`，由 `ImeEngine` 的 `handleIntent()` 方法接收。`reduce` 函数是纯函数，接收当前 `ImeState` 和 `ImeIntent`，返回新的 `ImeState`，不产生副作用。
- **状态通道**：`ImeState` 通过 `StateFlow<ImeState>` 暴露。UI 层订阅 `StateFlow`，状态变更自动驱动重组。`StateFlow` 保证值的原子性和一致性——订阅者始终读取到最新的完整状态快照，不存在部分更新的问题。
- **副作用通道**：一次性效果（PopupTip 领域事件）通过 `SharedFlow<ImeEffect>` 发射。`reduce` 函数在产生副作用时通过 `_effect.emit()` 发射到 SharedFlow，UI 层在独立的协程中消费。副作用通道与状态通道分离，确保高频效果不会触发全局 UI 重组。
- **编辑器操作通道**：`EditorAction` 由 `ImeEngine` 的 `dispatchEditorAction()` 统一分发到 `ImeEditorBridge`。桥梁实现者只需实现语义方法，无需理解 `EditorAction` 类型体系。编辑器操作通道承担所有对目标编辑器的操作（提交文本、移动光标、插入配对符号等）。

### 3.2 数据流图

```plantuml
@file:../diagrams/engine-mvi-data-flow.puml
```

上图展示了引擎的 MVI 数据流全景。用户操作（`InputGesture`）经 `KeyboardViewModel` 转换为 `ImeIntent`，由 `ImeEngine.handleIntent()` 接收。引擎内部经过 `KeyboardIntentHandler` → `KeyboardStateMachine` → `reduce` 的处理链，产生新的 `ImeState`（通过 `StateFlow` 暴露）、`ImeEffect`（通过 `SharedFlow` 发射）和 `EditorAction`（通过 `ImeEditorBridge` 分发）。UI 层订阅 `StateFlow<ImeState>` 驱动界面重组，在独立协程中订阅 `SharedFlow<ImeEffect>` 消费一次性效果。

### 3.3 数据流不变式

MVI 数据流遵循以下不变式，确保数据流的可追踪性和可预测性：

1. **单一状态源**：`ImeState` 是引擎对外的唯一状态源，不存在其他状态通道或旁路。UI 层的所有渲染数据均来自 `StateFlow<ImeState>`，不持有独立的业务状态副本。
2. **单向数据流**：数据从 `ImeIntent` 流向 `ImeState`/`EditorAction`，不存在反向依赖。`ImeState` 的变更不触发新的 `ImeIntent`——状态变更是 reduce 的结果而非原因。
3. **纯函数 reduce**：`reduce(state, intent)` 是纯函数，相同输入始终产生相同输出，不依赖外部状态，不产生副作用。异步操作（如字典查询）通过 `sideEffects` 列表延迟执行。
4. **副作用隔离**：需要异步处理的操作通过 `KeyboardStateTransition.Result.sideEffects` 返回 `List<ImeIntent>`，由 `ImeEngine` 通过显式工作队列循环处理。一次性 UI 效果通过 `SharedFlow<ImeEffect>` 独立通道发射。两种副作用机制互不干扰。

---

## 4. 核心模型概览

引擎库的核心模型按职责划分为九个子系统，各子系统拥有独立的设计文档。本节提供每个子系统的简要概览和文档索引，帮助读者快速定位到感兴趣的领域进行深入阅读。

### 4.1 键盘状态机

`Keyboard` / `KeyboardState` / `KeyboardStateMachine` 构成引擎的核心控制逻辑。`KeyboardState` 以 sealed class 表达键盘交互的有限状态机，涵盖空闲、拼音输入（等待、滑行、翻动）、候选选择、提交选项、编辑器编辑、符号选择、Emoji 选择等状态。`KeyboardStateMachine` 集中管理状态转换规则，`KeyboardIntentHandler` 子类负责将 `ImeIntent` 映射为 `KeyboardStateTransition`。三层映射模型（`ImeIntent` → `KeyboardStateTransition` → `KeyboardState`）确保意图语义与状态转换规则解耦。

详见 [030-键盘状态机](030-keyboard-state-machine.md)。

### 4.2 ImeState 子状态

`ImeState` 是 MVI 架构的单一状态树根节点，包含 `keyboard`、`inputList`、`candidateList`、`clipboard`、`favoriteList`、`toolListState`、`config` 七个字段。各子状态均为不可变 `data class`，通过 `copy()` 模式创建新实例。`ImeEffect` 副作用信号通过独立的 `SharedFlow<ImeEffect>` 通道发射，与 `ImeState` 完全分离。

详见 [020-ImeState](020-ime-state.md)。

### 4.3 输入列表

`InputList` / `InputItem` 构成引擎的核心数据结构，管理用户输入的字符序列、游标位置、待确认输入和输入补全。`InputItem` 包含 `Char`（字符输入）、`Gap`（游标位置）和 `MathExpr`（数学表达式）三种类型。`InputListOperator` 提供线程安全的操作方法，`InputListEditor` 支持撤销/恢复。

详见 [040-输入列表](040-input-list.md)。

### 4.4 候选列表与字典

`CandidateList` 管理候选词的分页、过滤和排序。`ImeDictProvider` 是字典查询的抽象接口，`ImeSqliteDictProvider` 是基于 Room 的默认实现，内部委托 `DictRepository` 完成数据库操作。字典系统包含拼音字词查询、用户输入历史、HMM 短语预测和 Viterbi 解码。

详见 [050-候选与字典](050-candidate-and-dict.md)。

### 4.5 编辑器桥接

`ImeEditorBridge` 是引擎与目标编辑器之间的桥梁接口，采用桥接模式实现输出目标与引擎的解耦。`BaseImeEditorBridge` 提供单快照撤销机制的抽象基类，`InputConnectionBridge` 和 `EditTextBridge` 分别面向系统输入连接和 `EditText` 目标的实现。

详见 [060-编辑器桥接](060-intent-editor-action-bridge.md)。

### 4.6 交互反馈

交互反馈（音效、触觉、按键弹出提示）完全由 UI 层在 `gestureToIntent()` 中直接处理，引擎不再参与。`AudioType` / `HapticType` 枚举和播放器接口（`AudioPlayer` / `HapticPlayer`）均定义在 `:ui` 中，平台实现由 `:app` 提供。引擎仅通过 `ImeEffect` 发射 `PopupTip` 领域事件。

详见 [065-交互反馈设计](065-audio-haptic-feedback.md)。

### 4.7 剪贴板与收藏

`ClipboardService` 监听系统剪贴板变更并提供文本类型检测（URL、验证码、手机号等）。`FavoriteService` 管理用户收藏的文本，通过 Room DAO 实现响应式查询。两者的功能启用由 `EngineConfig.favoriteInputEnabled` 和 `EngineConfig.favoriteClipEnabled` 控制，当两者均为 false 时收藏功能完全禁用。

详见 [070-剪贴板与收藏](070-clipboard-and-favorites.md)。

### 4.8 输入动作程序化

`InputAction` sealed class 定义坐标无关的逻辑动作（按下、滑行到、抬起、选择候选等）。`InputActionScript` 将动作序列组合为可回放的脚本，`InputActionScriptCompiler` 将待输入文本编译为动作脚本。归一化坐标基础类型 `OffsetF` / `RectF` 和路径插值算法确保跨面板、跨尺寸的回放一致性。

详见 [080-输入动作](080-input-action.md)。

### 4.9 日志系统

`ImeLog` 是日志系统的全局门面，`ImeLogger` 提供带标签的日志记录器。`LogWriter` 接口支持可扩展的输出目标，`FileLogWriter` 实现异步批量文件写入，`LogcatWriter` 提供 Android Logcat 输出，`CrashInterceptor` 实现 JVM 崩溃拦截。日志等级由应用层注入，引擎不内置构建类型判断。

详见 [090-日志系统](090-logging.md)。

---

## 5. ImeEngine 完整类定义

`ImeEngine` 是引擎库的核心入口点，提供完整的输入法能力。引擎不依赖任何 UI 框架，通过 `StateFlow` 暴露状态，通过 `ImeIntent` 接收用户操作，通过 `ImeEditorBridge` 输出编辑器操作。`ImeEngine` 的构造函数标记为 `internal`，强制通过 `Companion.create()` 工厂方法创建实例，确保所有依赖项正确初始化。

```kotlin
class ImeEngine internal constructor(
    private var config: ImeConfig,
    private val dictProvider: ImeDictProvider,
    private val stateMachine: KeyboardStateMachine,
    private val inputListOp: InputListOperator,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) {
    private val _state = MutableStateFlow(ImeState())
    val state: StateFlow<ImeState> = _state.asStateFlow()

    private val _effect = MutableSharedFlow<ImeEffect>(extraBufferCapacity = 64)
    val effect: SharedFlow<ImeEffect> = _effect.asSharedFlow()

    private val _editorBridges = mutableListOf<ImeEditorBridge>()

    // ─── 生命周期方法 ──────────────────────────────────────────

    /** 启动输入法，初始化 RuntimeConfig 并确定键盘类型 */
    fun start(startupConfig: StartupConfig) { ... }

    /** 关闭输入法，仅隐藏面板，但输入状态保持不变 */
    fun close() { ... }

    /** 销毁引擎，回收所有资源，不可再启动 */
    fun destroy() {
        scope.cancel()  // 取消所有异步操作（字典查询、用户数据记录等）
        // 原有清理代码...
        _editorBridges.clear()
        // ...
    }

    // ─── 意图与配置 ──────────────────────────────────────────

    fun attachEditorBridge(bridge: ImeEditorBridge) { ... }
    fun detachEditorBridge(bridge: ImeEditorBridge) { ... }
    fun handleIntent(intent: ImeIntent) { ... }
    fun updateConfig(block: (ImeConfig) -> ImeConfig) { ... }

    companion object {
        fun create(config: ImeConfig = ImeConfig(), dictProvider: ImeDictProvider): ImeEngine
    }
}
```

### 5.1 构造参数

| 参数 | 类型 | 说明 |
|------|------|------|
| `config` | `ImeConfig` | 运行时配置，包含引擎配置、UI 配置和运行时配置。`var` 声明允许运行时通过 `updateConfig()` 修改 |
| `dictProvider` | `ImeDictProvider` | 字典查询接口，由外部注入。默认实现 `ImeSqliteDictProvider` 基于 Room，第三方可替换 |
| `stateMachine` | `KeyboardStateMachine` | 键盘状态机，集中管理 `KeyboardState` 的转换规则和状态历史 |
| `inputListOp` | `InputListOperator` | 输入列表操作器，提供线程安全的 `InputList` 变更方法 |
| `scope` | `CoroutineScope` | 引擎内部的结构化协程作用域，所有异步操作（字典查询、用户数据记录、剪贴板监听等）均在该作用域内启动。`destroy()` 时通过 `scope.cancel()` 统一取消，确保无泄漏。第三方可以通过构造函数注入自定义 scope，与外部生命周期绑定。默认值使用 `SupervisorJob()` + `Dispatchers.Default` |

### 5.2 状态暴露

`_state` 是内部的 `MutableStateFlow<ImeState>`，对外暴露只读的 `StateFlow<ImeState>`。`StateFlow` 保证值的原子性——订阅者始终读取到最新的完整状态快照。`_effect` 是内部的 `MutableSharedFlow<ImeEffect>`，配置 `extraBufferCapacity = 64` 以应对快速连续打字时的高频效果发射，对外暴露只读的 `SharedFlow<ImeEffect>`。引擎在 `reduce` 过程中通过 `_effect.emit()` 发射副作用信号，UI 层在独立的 `collectEffect()` 协程中收集并消费。

### 5.3 编辑器桥接

`_editorBridges` 是 `MutableList<ImeEditorBridge>`，通过 `attachEditorBridge()` 注册桥接，`detachEditorBridge()` 注销指定桥接。引擎在分发 `EditorAction` 时遍历所有已注册桥接，逐个调用对应的语义方法；若桥接列表为空则静默跳过。这种设计允许引擎在没有桥接的情况下正常运行（例如纯逻辑测试场景），也支持同时向多个编辑器分发编辑器操作。

**桥梁所有权与生命周期**：`ImeEditorBridge` 的创建、注册、注销和销毁均由宿主模块负责，引擎仅持有桥梁引用用于分发 `EditorAction`。`attachEditorBridge()` 与 `detachEditorBridge()` 必须由同一宿主模块成对调用——`attach` 注册桥梁到引擎的桥接列表，`detach` 从列表中移除指定桥梁。这种设计遵循观察者模式的所有权原则：引擎是 Subject，桥梁是 Observer，Subject 不负责 Observer 的注册/注销的完整生命周期管理。`destroy()` 内部对 `_editorBridges` 执行 `clear()` 是防御性安全网，确保即使调用方忘记 `detach`，桥梁引用也不会泄漏到已销毁的引擎中，但 `destroy()` 不会替外部对象执行 `detachEditorBridge()` 的注销逻辑。调用方应在 `destroy()` 前显式调用 `detachEditorBridge(bridge)`，确保 `attach`/`detach` 对称。

### 5.4 工厂方法

`Companion.create()` 是 `ImeEngine` 的唯一创建入口，内部完成以下初始化工作：创建 `KeyboardStateMachine`、创建 `InputListOperator`。工厂方法确保所有依赖项正确初始化，避免外部构造时遗漏关键组件。

### 5.5 生命周期方法

`ImeEngine` 提供三个生命周期方法，分别对应输入法的启动、隐藏和销毁。所有状态变更通过 `applyStateUpdate()` 统一出口，确保日志、断言和状态不变式检查的一致性。

#### `start(startupConfig: StartupConfig)`

启动输入法，建立后续所有 Intent 处理的前置条件。`start()` 是独立的生命周期方法，**不经过 `handleIntent()` → reduce 六步处理链**，而是直接执行引擎级初始化操作。其处理步骤为：

1. **更新 RuntimeConfig**：将 `StartupConfig` 中的值覆盖到当前 `RuntimeConfig`。`screenOrientation` 始终被覆盖；`editorInputType` 在 `StartupConfig.editorInputType` 非 null 时覆盖，否则保持原值不变。
2. **确定 KeyboardType**：通过两级级联规则确定启动时的键盘类型。首先根据 `StartupConfig.imeSubtype` 确定基础键盘（`Latin` → 拉丁键盘，其余 → 拼音键盘），然后根据 `RuntimeConfig.editorInputType` 修正（`Number/Datetime/Phone` → 数字键盘，`Password` → 拉丁键盘，其余保持）。
3. **更新 keyPopupTipsEnabled**：若 `editorInputType` 为 `Password`，强制设置 `RuntimeConfig.keyPopupTipsEnabled = false` 并清空输入列表。
4. **重置 KeyboardStateMachine**：根据确定的 `KeyboardType` 调用 `stateMachine.resetTo(initialState)` 并清空历史栈。
5. **检查剪贴板可粘贴内容**：若 `UiConfig.clipPastePopupTipsEnabled` 为 `true`，检查系统剪贴板是否有新的可粘贴内容，若有，通过 `_effect.emit()` 发射 `ImeEffect.PopupTip.Action(message="可粘贴内容", actionLabel="粘贴", action=ImeIntent.PasteClip(text), persistent=true)` 提示。
6. **通过 `applyStateUpdate()` 原子更新 ImeState**：一次 `copy()` 操作完成所有子状态的协调变更。

调用时机：`InputMethodService#onStartInputView` 和 `InputMethodService#onCurrentInputMethodSubtypeChanged`。

#### `close()`

关闭输入法，仅隐藏面板，但输入状态保持不变。`close()` 是轻量级操作——关闭 `ClipboardService` 对系统剪贴板的监听（停止占用系统资源），但不重置键盘状态、输入列表或候选列表。调用 `start()` 后即可恢复到关闭前的完整工作状态。

调用时机：`InputMethodService#onFinishInputView`。

#### `destroy()`

销毁引擎，回收所有资源。`destroy()` 是终态操作——停止异步任务、关闭字典连接、注销剪贴板监听、清空编辑器桥接列表，并将所有内部引用置为 `null`。调用 `destroy()` 后引擎不可再启动，任何对引擎方法的调用将抛出 `IllegalStateException`。

调用时机：`InputMethodService#onDestroy`。

### 5.6 applyStateUpdate() 统一状态更新出口

`applyStateUpdate()` 是 `ImeEngine` 内部的私有方法，所有 `ImeState` 变更——无论是 `handleIntent()` 中的 reduce 逻辑、`SwitchKeyboard` 的直接切换，还是 `start()`/`close()`/`destroy()` 中的生命周期操作——都必须经过此方法更新 `_state`。该方法是引擎状态更新的唯一出口，提供以下统一能力：

- **日志**：每次状态变更记录旧状态与新状态的 diff，便于调试和追踪（仅 DEBUG 等级以上执行）
- **断言**：验证状态不变式（如 `keyboard.type` 与 `keyboard.state` 一致性、`candidateList` 非空前提、`favoriteList` 禁用一致性等），assertStateInvariants 包装在 DEBUG 等级守卫中，release 构建中零开销。使用 check() 而非 assert() 的设计变更为等级守卫，确保不变式在开发阶段充分验证
- **状态不变式检查**：确保 §3.3 中定义的所有不变式在每次状态变更后仍然成立

```kotlin
/** 统一的状态更新出口：所有 ImeState 变更必须经过此方法 */
private fun applyStateUpdate(transform: (ImeState) -> ImeState) {
    val oldState = _state.value
    val newState = transform(oldState)
    if (ImeLog.level <= LogLevel.DEBUG) {
        ImeLogger.d("ImeEngine", "State updated: ${oldState.diff(newState)}")
        assertStateInvariants(newState)  // 仅在 debug 下执行不变式检查
    }
    _state.value = newState
}
```

`applyStateUpdate()` 的引入确保了即使 `start()` 和 `handleIntent()` 的处理路径不同，状态更新的质量保证是统一的——不存在绕过日志和断言的旁路。assertStateInvariants 包装在 DEBUG 等级守卫中，release 构建中零开销。使用 check() 而非 assert() 的设计变更为等级守卫，确保不变式在开发阶段充分验证。

> **性能优化记录**：不可变集合的 `addTouchTrailPoint()` 之前使用 `ArrayList` 逐次添加导致 O(n²) 复杂度，现已改用 `toMutableList()` 模式将逐个追加转为批量重建，O(n) 复杂度。详情见 UI 层手势反馈文档。

### 5.7 使用示例

```kotlin
val engine = ImeEngine.create(
    config = ImeConfig(
        engine = ImeConfig.EngineConfig(
            inputPredictionEnabled = true,
            favoriteInputEnabled = true,
            favoriteClipEnabled = true,
        ),
        ui = ImeConfig.UiConfig(
            keyboardHandMode = KeyboardHandMode.Right,
            keyboardThemeType = KeyboardThemeType.FollowSystem,
        ),
    ),
    dictProvider = ImeSqliteDictProvider(context),
)

// 启动输入法（在 InputMethodService.onStartInputView 中调用）
engine.start(StartupConfig(
    imeSubtype = IMESubtype.Hans,
    screenOrientation = ScreenOrientation.Portrait,
    editorInputType = EditorInputType.Text,
))

// 接入桥接
val bridge = InputConnectionBridge { currentInputConnection }
engine.attachEditorBridge(bridge)

// 订阅状态
engine.state.collect { state -> updateUI(state) }

// 发送意图
engine.handleIntent(ImeIntent.SwitchKeyboard(KeyboardType.Latin))
```

---

## 6. ImeConfig 配置模型

`ImeConfig` 是统一的运行时配置，包含引擎配置、UI 配置和运行时配置三个子配置，各自在数据结构上明确隔离。引擎配置（`EngineConfig`）和 UI 配置（`UiConfig`）均为持久化配置项，运行时配置（`RuntimeConfig`）不做持久化。`StartupConfig` 仅作为 `ImeEngine.start()` 的参数，用于初始化 `RuntimeConfig`。对配置项的修改在 UI 和引擎层面都是即时生效的。库不内置配置持久化，所有配置通过 `ImeConfig` 在创建时或运行时设置，持久化是应用层的职责（如 `:app` 模块使用 DataStore）。

```kotlin
data class ImeConfig(
    val engine: EngineConfig = EngineConfig(),
    val ui: UiConfig = UiConfig(),
    val runtime: RuntimeConfig = RuntimeConfig(),
) {
    /** 引擎配置：影响引擎的核心行为，均为持久化配置项 */
    data class EngineConfig(
        val logLevel: LogLevel = LogLevel.WARN,
        val logStoragePath: String? = null,
        val inputPredictionEnabled: Boolean = true,
        val userDataPersistEnabled: Boolean = true,
        val favoriteInputEnabled: Boolean = true,
        val favoriteClipEnabled: Boolean = true,
        val favoriteSyncToUserDictEnabled: Boolean = false,
        val candidateVariantFirstEnabled: Boolean = false,
    )

    /** UI 配置：影响界面呈现和交互反馈，均为持久化配置项 */
    data class UiConfig(
        val keyboardInputMode: KeyboardInputMode = KeyboardInputMode.RectGrid,
        val keyboardHandMode: KeyboardHandMode = KeyboardHandMode.Right,
        val keyboardThemeType: KeyboardThemeType = KeyboardThemeType.FollowSystem,
        val keyPopupTipsEnabled: Boolean = true,
        val audioFeedbackEnabled: Boolean = true,
        val hapticFeedbackEnabled: Boolean = true,
        val keyAnimationEnabled: Boolean = true,
        val gestureSlippingTrailEnabled: Boolean = true,
        val clipPopupTipsEnabled: Boolean = true,
        val clipPastePopupTipsEnabled: Boolean = true,
        val clipPopupTipsTimeout: Int = 15,
        val adaptDesktopSwipeUpGesture: Boolean = false,
        val candidatesPagingAudioEnabled: Boolean = true,
        val practicePlaybackSpeed: Float = 1.0f,
        val practiceShowFingerOverlay: Boolean = true,
        val practiceShowSwipeTrail: Boolean = true,
    )

    /** 运行时配置：不做持久化的临时状态 */
    data class RuntimeConfig(
        val screenOrientation: ScreenOrientation = ScreenOrientation.Landscape,
        val editorInputType: EditorInputType = EditorInputType.Text,
        val keyPopupTipsEnabled: Boolean? = null,
        val toolSettingsEnabled: Boolean = true,
        val toolSwitchIMEEnabled: Boolean = true,
        val toolCloseKeyboardEnabled: Boolean = true,
    )
}

/** 启动配置：仅作为 ImeEngine.start() 的参数 */
data class StartupConfig(
    val imeSubtype: IMESubtype,
    val screenOrientation: ScreenOrientation,
    val editorInputType: EditorInputType?,
)

enum class ScreenOrientation { Landscape, Portrait }
enum class EditorInputType { Filter, Number, Datetime, Phone, Password, Email, URI, Text }
enum class IMESubtype { Latin, Hans }
```

### 6.1 EngineConfig 字段说明

| 字段 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `logLevel` | `LogLevel` | `WARN` | 日志等级，由应用层注入 |
| `logStoragePath` | `String?` | `null` | 日志文件存放目录路径，`null` 使用默认应用私有目录 |
| `inputPredictionEnabled` | `Boolean` | `true` | 是否启用输入预测，用于输入补全，提升输入效率 |
| `userDataPersistEnabled` | `Boolean` | `true` | 是否持久化用户数据。启用后，已提交且未撤回的输入将被保存，用于提升输入预测的准确性 |
| `favoriteInputEnabled` | `Boolean` | `true` | 是否启用「输入收藏」。启用后，已提交输入将提示可收藏，并在用户确认后将该输入内容收藏起来（保存到收藏表），被收藏的输入内容可在「收藏面板」中单击输入，避免反复输入相同内容 |
| `favoriteClipEnabled` | `Boolean` | `true` | 是否启用「剪贴板收藏」。启用后，将监听剪贴板，当剪贴板中有未收藏的可粘贴内容时，将提示该可粘贴内容可收藏，并在用户确认后将该可粘贴内容收藏起来（保存到收藏表），方便后续直接输入。由于剪贴板监听只有在输入法处于前台时才会起作用，因此，复制/剪切将均是由输入法触发的 |
| `favoriteSyncToUserDictEnabled` | `Boolean` | `false` | 是否启用「收藏与用户字典的同步」。启用后，被收藏的输入内容或可粘贴内容，将自动保存到系统用户字典中。对于系统用户字典中的已有数据，可以在收藏面板中增加「同步」按钮，将用户字典数据同步到收藏中（需要用户自行定期手动同步）。在删除收藏时，可选择是否一并删除用户字典（单独按钮，与仅删除收藏按钮同级）。注意，同步和同步删除仅在 `favoriteSyncToUserDictEnabled` 为 `true` 时启用 |
| `candidateVariantFirstEnabled` | `Boolean` | `false` | 是否启用「繁体优先」，主要用于自动将拼音候选字转换为繁体 |

### 6.2 UiConfig 字段说明

| 字段 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `keyboardInputMode` | `KeyboardInputMode` | `RectGrid` | 键盘采用的输入模式。影响 UI 层面的按键布局，也影响引擎层面的交互逻辑处理。不支持临时性修改，只能通过配置变更 |
| `keyboardHandMode` | `KeyboardHandMode` | `Right` | 键盘的左右手模式。只影响 UI 层面键盘为适应左右手而做的按键布局调整 |
| `keyboardThemeType` | `KeyboardThemeType` | `FollowSystem` | 键盘主题样式类型。不支持临时性修改 |
| `keyPopupTipsEnabled` | `Boolean` | `true` | 是否显示按键输入提示。当其为 `false` 时，按键输入提示将始终被禁用，而若其为 `true`，则按键输入提示可被 `RuntimeConfig.keyPopupTipsEnabled` 临时禁用 |
| `audioFeedbackEnabled` | `Boolean` | `true` | 是否启用按键音效反馈 |
| `hapticFeedbackEnabled` | `Boolean` | `true` | 是否启用触觉反馈 |
| `keyAnimationEnabled` | `Boolean` | `true` | 是否启用按键动画 |
| `gestureSlippingTrailEnabled` | `Boolean` | `true` | 是否启用滑行轨迹显示 |
| `clipPopupTipsEnabled` | `Boolean` | `true` | 是否启用剪贴板收藏弹出提示 |
| `clipPastePopupTipsEnabled` | `Boolean` | `true` | 是否启用可粘贴内容的弹出提示。若启用，则在 `ImeEngine.start()` 时检查剪贴板是否有新的可粘贴内容，若有，则弹出粘贴确认提示，在用户点击后向目标编辑器粘贴对应的内容 |
| `clipPopupTipsTimeout` | `Int` | `15` | 剪贴板弹出提示超时（秒） |
| `adaptDesktopSwipeUpGesture` | `Boolean` | `false` | 是否适配桌面下滑手势 |
| `candidatesPagingAudioEnabled` | `Boolean` | `true` | 候选词翻页是否播放音效 |
| `practicePlaybackSpeed` | `Float` | `1.0f` | 输入练习回放速度倍率 |
| `practiceShowFingerOverlay` | `Boolean` | `true` | 输入练习是否显示手指覆盖层 |
| `practiceShowSwipeTrail` | `Boolean` | `true` | 输入练习是否显示滑行轨迹 |

### 6.3 RuntimeConfig 字段说明

| 字段 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `screenOrientation` | `ScreenOrientation` | `Landscape` | 在调用 `ImeEngine.start` 时被 `StartupConfig.screenOrientation` 覆盖。其仅影响 UI 层的键盘在横屏或竖屏下的按键布局形式。对于某些 `KeyboardInputMode` 可以不支持横屏切换，这取决于该输入模式在横屏中的交互是否友好、便捷、顺畅 |
| `editorInputType` | `EditorInputType` | `Text` | 在调用 `ImeEngine.start` 时被 `StartupConfig.editorInputType` 覆盖，但若是 `StartupConfig.editorInputType` 为 `null`，则不做覆盖。编辑器的输入类型可以决定输入法启动时的键盘类型，也决定了键盘中 enter 按键的图标样式：当输入类型为 `Filter` 时，采用搜索图标，表示开始查询；当输入类型为 `Number`、`Phone`、`Password`、`URI` 等单行输入时，采用提交图标，表示提交输入；对于 `Text` 等其他无明确动作区分的类型，则采用换行符图标，表示输入换行符。注意，虽然 enter 按键的视觉样式不同，但其行为没有变化，其始终向编辑器输入 `\n` 字符 |
| `keyPopupTipsEnabled` | `Boolean?` | `null` | 是否启用按键输入提示，在调用 `ImeEngine.start` 时根据 `RuntimeConfig.editorInputType` 更新该值，若编辑器输入类型为 `Password` 等敏感信息类型时，始终强制禁用按键输入提示，以保护敏感数据，其余情况则可由应用侧自行决定。该值为 `false` 时，忽略 `UiConfig.keyPopupTipsEnabled` 的设置，该值不为 `false` 时，由 `UiConfig.keyPopupTipsEnabled` 的值决定是否显示提示 |
| `toolSettingsEnabled` | `Boolean` | `true` | 是否启用「配置」按钮。当其为 `false` 时，键盘上方的工具栏中的「配置」按钮将被禁用但仍然显示 |
| `toolSwitchIMEEnabled` | `Boolean` | `true` | 是否启用「输入法切换」按钮。当其为 `false` 时，键盘上方的工具栏中的「输入法切换」按钮将被禁用但仍然显示 |
| `toolCloseKeyboardEnabled` | `Boolean` | `true` | 是否启用「关闭键盘」按钮。当其为 `false` 时，键盘上方的工具栏中的「关闭键盘」按钮将被禁用但仍然显示 |

### 6.4 StartupConfig 字段说明

| 字段 | 类型 | 说明 |
|------|------|------|
| `imeSubtype` | `IMESubtype` | 系统输入法子类型（Input Method Subtype）。通过 context 实时获取。枚举量为 `Latin`、`Hans` |
| `screenOrientation` | `ScreenOrientation` | 屏幕方向（纵向 or 横向）。通过 context 实时获取 |
| `editorInputType` | `EditorInputType?` | 目标编辑器的输入类型。`Filter` 代表搜索框输入；`Number` 代表数字输入；`Datetime` 代表日期输入；`Phone` 代表电话输入；`Password` 代表密码输入；`Email` 代表邮件输入；`URI` 代表 url 地址输入；`Text` 代表普通文本输入，在无法精确识别输入类型时，均采用该类型。在 `InputMethodService#onCurrentInputMethodSubtypeChanged` 中，该配置项值始终为 `null`，以确保不覆盖已识别到的目标类型 |

### 6.5 KeyboardType 切换规则

`KeyboardType` 不是配置项，而是运行时状态。键盘类型的切换遵循以下规则：

1. **在 `ImeEngine.start()` 中**：首先检查 `StartupConfig.imeSubtype`，若为 `Latin` 则切换到拉丁键盘，否则切换到拼音键盘；然后检查 `RuntimeConfig.editorInputType`，若为 `Number`、`Datetime` 或 `Phone` 则切换到数字键盘，若为 `Password` 则切换到拉丁键盘，其余类型保持前一步的键盘类型。
2. **通过功能键点击**：用户点击键盘上的功能键时，发送 `ImeIntent.SwitchKeyboard` 意图，引擎处理该意图切换键盘类型。
3. **调用时机**：`ImeEngine.start()` 在 `InputMethodService#onStartInputView` 和 `InputMethodService#onCurrentInputMethodSubtypeChanged` 中调用。

### 6.6 keyPopupTipsEnabled 交互逻辑

`UiConfig.keyPopupTipsEnabled` 与 `RuntimeConfig.keyPopupTipsEnabled` 之间存在优先级关系：

- 当 `RuntimeConfig.keyPopupTipsEnabled` 为 `false` 时，强制禁用按键输入提示，无论 `UiConfig.keyPopupTipsEnabled` 的值为何。
- 当 `RuntimeConfig.keyPopupTipsEnabled` 不为 `false`（即 `null` 或 `true`）时，由 `UiConfig.keyPopupTipsEnabled` 的值决定是否显示按键输入提示。
- 在 `ImeEngine.start()` 中，若 `editorInputType` 为 `Password`，则强制设置 `RuntimeConfig.keyPopupTipsEnabled = false`，并同时清空输入列表（`InputList`）。

### 6.7 收藏功能门控

收藏功能的启用由 `EngineConfig.favoriteInputEnabled` 和 `EngineConfig.favoriteClipEnabled` 两个配置项控制：

- 当 `favoriteInputEnabled` 和 `favoriteClipEnabled` 均为 `false` 时，「收藏」功能完全禁用，UI 层隐藏收藏面板切换按钮。
- 当 `favoriteInputEnabled` 为 `true` 时：已提交输入将提示可收藏，用户确认后将该输入内容收藏。
- 当 `favoriteClipEnabled` 为 `true` 时：监听剪贴板，当剪贴板中有未收藏的可粘贴内容时，提示该可粘贴内容可收藏，用户确认后收藏。
- 当 `favoriteSyncToUserDictEnabled` 为 `true` 时：收藏面板中出现「同步」和「同步删除」按钮，支持收藏与用户字典的双向同步。注意，`favoriteSyncToUserDictEnabled` 仅在 `favoriteInputEnabled` 或 `favoriteClipEnabled` 至少一个为 `true` 时生效。

---

## 7. 收藏功能门控

收藏功能的门控机制采用直接的布尔配置字段来控制功能的启用与禁用。

### 7.1 门控规则

- 当 `favoriteInputEnabled` 和 `favoriteClipEnabled` 均为 `false` 时，收藏功能完全禁用：`ImeState.favoriteList.disabled = true`，`favoriteList.favorites` 始终为空，调用 `ImeIntent.SaveFavorite` 抛出 `IllegalStateException`。
- 当 `favoriteClipEnabled` 为 `false` 且 `clipPastePopupTipsEnabled` 为 `false` 时，`Clipboard.disabled = true`，`ClipboardService` 完全不工作。
- 当 `favoriteClipEnabled` 为 `false` 但 `clipPastePopupTipsEnabled` 为 `true` 时，`Clipboard.disabled = false`，`ClipboardService` 正常工作（支持粘贴功能和可粘贴内容提示），但不会产生剪贴板收藏提示。
- 当 `favoriteInputEnabled` 为 `false` 时，已提交输入不会产生输入收藏提示。
- `favoriteSyncToUserDictEnabled` 是正交配置，仅在 `favoriteInputEnabled` 或 `favoriteClipEnabled` 至少一个为 `true` 时生效。当其为 `true` 时，收藏面板中出现「同步」和「同步删除」按钮；当其均为 `false` 时，该配置项无意义。

### 7.2 与 ImeConfig 的同步

`favoriteInputEnabled`、`favoriteClipEnabled` 和 `favoriteSyncToUserDictEnabled` 的值存储在 `ImeConfig.engine` 中，`ImeEngine` 在 `create()` 时根据配置初始化。运行时通过 `ImeEngine.updateConfig()` 修改这些字段后，引擎同步更新门控状态，后续的检查使用新的配置值。`ImeState.config` 始终反映当前生效的配置，UI 层可根据配置的启用状态显示或隐藏对应的功能入口。

---

## 8. reduce 函数的核心逻辑

`handleIntent()` 是 `ImeEngine` 处理用户意图的核心方法，内部通过六步处理链将 `ImeIntent` 转化为状态变更、编辑器操作和副作用信号。整个处理链在主线程上同步执行，异步操作通过 `sideEffects` 列表延迟到独立协程中处理，确保 `reduce` 函数的纯函数特性。

### 8.1 处理链六步

```
ImeIntent
  │
  ▼
Step 1: KeyboardIntentHandler 将 ImeIntent 映射为 KeyboardStateTransition
  │
  ▼
Step 2: KeyboardStateMachine 执行 transition → 新 KeyboardState + sideEffects
  │
  ▼
Step 3: 处理 sideEffects（通过 ArrayDeque 工作队列循环处理，最大深度 5）
  │
  ▼
Step 4: 通过 copy() 模式更新 ImeState
  │
  ▼
Step 5: 分发 EditorAction 到 ImeEditorBridge
  │
  ▼
Step 6: 发射 ImeEffect 到 SharedFlow
```

**未知意图处理**：`handleIntent()` 的 `when(intent)` 表达式由 Kotlin 编译器验证穷尽性——所有 ImeIntent 子类型必须被覆盖。若未来新增 ImeIntent 子类型但未在 `when` 中添加对应分支，编译器报错而非运行时静默忽略。因此不存在"未知意图"的运行时分支。

### 8.2 Step 1：ImeIntent → KeyboardStateTransition

`ImeEngine` 维护一个 `Map<KeyboardType, KeyboardIntentHandler>` 注册表，根据当前 `ImeState.keyboard.type` 选择对应的 `KeyboardIntentHandler` 实现。各 `KeyboardIntentHandler` 子类是无状态的策略对象，负责将 `ImeIntent` 映射为 `KeyboardStateTransition`，再由 `KeyboardStateMachine` 执行转换。

当 `ImeIntent` 为 `SwitchKeyboard` 时，直接切换键盘类型并设置初始状态，不经过 `KeyboardIntentHandler`。其他 `ImeIntent` 均委托给当前键盘类型的 `KeyboardIntentHandler` 处理。`KeyboardIntentHandler.handleIntent()` 接收 `ImeIntent` 和当前 `KeyboardState`，返回 `KeyboardStateTransition.Result`（包含新状态和副作用列表）。

### 8.3 Step 2：KeyboardStateMachine 执行 transition

`KeyboardStateMachine.transition()` 是状态转换的集中处理器，接收 `KeyboardStateTransition`，根据当前状态执行转换规则，返回 `KeyboardStateTransition.Result`。`Result` 包含两个部分：

- `newState: KeyboardState`：转换后的新状态。若转换在当前状态下不合法，`newState` 等于原状态（Fail-safe 行为）。
- `sideEffects: List<ImeIntent>`：转换产生的副作用意图列表，包含需要异步处理的操作（如字典查询、音频播放、编辑器桥接等）。`KeyboardStateMachine` 本身是纯函数，不直接执行副作用，而是通过返回副作用列表交由 `ImeEngine` 异步处理。

若 `newState` 与原状态不同，`KeyboardStateMachine` 将原状态推入 `KeyboardStateHistory` 有界栈（最大 10 层），供后续回退使用。

### 8.4 Step 3：处理 sideEffects

`sideEffects` 是 `KeyboardStateTransition.Result` 中返回的 `List<ImeIntent>`，包含状态转换产生的异步操作意图。sideEffects 通过 `ArrayDeque<ImeIntent>` 显式工作队列循环处理，而非递归调用。队列最大深度 5 作为安全网防止无限递归，主要清理机制由 `destroy()` 中的 `scope.cancel()` 保证——引擎销毁时所有异步操作统一取消，确保无泄漏。整个处理在 `scope.launch` 中异步执行，不阻塞主线程：

```kotlin
private fun processSideEffects(sideEffects: List<ImeIntent>) {
    scope.launch {
        val queue = ArrayDeque(sideEffects)
        var depth = 0
        val maxDepth = 5

        while (queue.isNotEmpty()) {
            if (++depth > maxDepth) {
                throw IllegalStateException("Side effect recursion exceeds max depth $maxDepth")
            }
            val intent = queue.removeFirst()
            val result = doHandleIntent(intent)
            queue.addAll(result.sideEffects)
        }
    }
}
```

典型的副作用意图包括：

- `ImeIntent.SelectCandidate(...)`：候选词选中后触发字典查询和输入列表确认
- `ImeIntent.CommitInput`：输入提交后触发 `EditorAction.CommitText` 输出
- `ImeIntent.DeleteInput`：输入删除后触发 `EditorAction.RevokeCommit` 输出

副作用处理的递归深度受引擎内部保护，超过阈值时抛出异常，防止无限递归。

### 8.5 Step 4：更新 ImeState

`ImeState` 通过 `copy()` 模式创建新实例，确保不可变性。每次 `handleIntent()` 调用产生一个新的 `ImeState`，通过 `MutableStateFlow.value` 原子更新。`ImeState` 的更新涉及多个子状态的协调变更：

- `keyboard`：更新 `keyboard.state` 为新的 `KeyboardState`
- `inputList`：根据意图类型更新输入列表（追加字符、确认候选、删除输入等）
- `candidateList`：根据字典查询结果更新候选列表
- `clipboard` / `favoriteList`：根据剪贴板和收藏操作更新对应子状态

各子状态的变更通过 `applyStateUpdate()` 统一出口完成，不存在绕过日志和断言的旁路。`start()` 中的状态初始化也经过 `applyStateUpdate()`，确保所有状态变更——无论来源是生命周期方法还是用户意图——都经过统一的质量保证流程。

### 8.6 Step 5：分发 EditorAction 到 ImeEditorBridge

`ImeEngine` 在 `reduce` 过程中收集需要输出的 `EditorAction` 实例，在状态更新完成后统一分发到所有已注册的 `ImeEditorBridge`。分发通过 `dispatchEditorAction()` 方法实现，遍历 `_editorBridges` 中的所有桥接，对每个桥接使用 `when(EditorAction)` 穷举所有动作类型，调用桥接的对应语义方法：

- `EditorAction.CommitText` → `bridge.commitText(text, replacements)`
- `EditorAction.RevokeCommit` → `bridge.revokeCommit()`
- `EditorAction.InsertPairedSymbols` → `bridge.insertPairedSymbols(left, right)`
- `EditorAction.MoveCursor` → `bridge.moveCursor(direction)`
- `EditorAction.SelectRange` → `bridge.selectRange(direction)`
- `EditorAction.PerformEdit` → `bridge.performEdit(action)`

若 `_editorBridges` 为空，分发操作被静默跳过。所有桥接方法在主线程调用，确保线程安全。

### 8.7 Step 6：发射 ImeEffect 到 SharedFlow

`ImeEngine` 在 `reduce` 过程中收集需要发射的 `ImeEffect` 实例，在状态更新完成后通过 `_effect.emit()` 发射到 `SharedFlow<ImeEffect>`。UI 层在独立的 `collectEffect()` 协程中收集效果并消费，不触发 `ImeState` 的重组。

典型的 `ImeEffect` 场景：
- 键盘类型切换时发射 `ImeEffect.PopupTip.Message("已切换到拉丁键盘")`
- 输入提交后若内容未收藏且 `EngineConfig.favoriteInputEnabled` 为 `true`，发射 `ImeEffect.PopupTip.Action(message="可收藏内容", actionLabel="收藏", action=ImeIntent.SaveFavorite(...), persistent=false)`
- 引擎启动时若检测到可粘贴内容，发射 `ImeEffect.PopupTip.Action(message="可粘贴内容", actionLabel="粘贴", action=ImeIntent.PasteClip(text), persistent=true)`

音效和触觉反馈已完全交由 UI 层在 `gestureToIntent()` 中直接处理，不再经由 `ImeEffect` 通道。
