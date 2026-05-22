# 引擎架构总览

`:ime-engine` 是筷字输入法的引擎库，提供核心 IME 引擎能力。引擎库独立设计的目标是使输入法的逻辑层与 UI 和应用之间实现彻底分离与解耦，从而方便第三方定制自己的 UI、修改交互逻辑等。第三方应用只需引入 `:ime-engine` 即可获得完整的输入法能力——拼音输入、滑行输入、候选选择、输入列表管理、撤销重做等——无需依赖系统 IME 服务或任何 UI 框架。

---

## 1. 模块定位与设计目标

| 定位 | 说明 |
|------|------|
| **逻辑与 UI 分离** | 引擎库独立设计的目标是使输入法的逻辑层与 UI 和应用之间实现分离、解耦，从而方便第三方定制自己的 UI、修改交互逻辑等 |
| **MVI 驱动** | 通过 `StateFlow<ImeState>` 暴露状态，通过 `ImeIntent` 接收操作，通过 `ImeOutputBridge` 输出编辑指令 |
| **可嵌入** | 第三方应用只需引入 `:ime-engine` 即可获得完整输入法能力，无需系统 IME 服务 |
| **可扩展** | 字典接口与实现分离（`ImeDictProvider`），输出桥接可自定义（`ImeOutputBridge`），功能可裁剪（`Feature`） |
| **Fail Fast** | 非法操作（如禁用收藏后调用收藏功能）立即抛出异常而非静默忽略 |

引擎库的「逻辑与 UI 分离」定位意味着第三方应用可以完全用自定义 UI 替换 `:ime-ui` 而不影响引擎功能，也可以仅引入 `:ime-engine` 自行实现视图层和交互逻辑。唯一依赖 Android 的部分是字典 I/O（`ImeSqliteDictProvider` 使用 Room），但第三方可以提供自己的 `ImeDictProvider` 实现来消除 Android 依赖。

「MVI 驱动」定位是引擎与 UI 完全分离的技术基础。引擎不依赖任何 UI 框架，所有状态变更通过 `StateFlow` 暴露，所有用户操作通过 `ImeIntent` 接收，所有编辑指令通过 `ImeOutputBridge` 输出。这种单向数据流使得引擎可以被任意 UI 框架（Compose、View、Web、游戏引擎等）消费，而不需要引擎感知 UI 的存在。

「可嵌入」定位使得 `:ime-engine` 可以在多种场景下使用：作为系统输入法引擎、嵌入到应用内的自定义输入组件中、甚至作为纯 JVM 环境下的输入法逻辑核心。引擎的创建和销毁完全由宿主控制，不持有任何全局状态或单例。

「可扩展」定位通过三个扩展点实现：`ImeDictProvider` 允许替换整个字典层（例如使用远程字典服务替代本地 SQLite）；`ImeOutputBridge` 允许替换输出目标（例如接入 WebView 编辑器或游戏引擎文本框）；`Feature` 枚举允许裁剪功能（例如禁用剪贴板功能以减少权限需求）。

「Fail Fast」定位确保引擎在运行时检测到非法操作时立即抛出异常，而非静默忽略或产生不确定行为。典型的非法操作包括：在 `Feature.Clipboard` 禁用时调用 `ImeIntent.PasteClip`、在 `Feature.Favorites` 禁用时调用 `ImeIntent.SaveFavorite`、在无效状态下执行不合法的 `KeyboardStateTransition` 等。Fail Fast 原则帮助开发者在开发阶段尽早发现错误，避免错误在调用链中传播后难以定位。

---

## 2. 核心 API 面

引擎库的核心 API 面由六个核心类型构成，形成 `:ime-engine` 与 `:ime-ui`、`:app` 之间的核心契约。这六个类型是第三方应用使用引擎库的主要接口，也是引擎内部各组件协作的基础协议。

### 2.1 ImeEngine

`ImeEngine` 是引擎库的核心入口点，提供完整的输入法能力。引擎不依赖任何 UI 框架，通过 `StateFlow` 暴露状态，通过 `ImeIntent` 接收用户操作，通过 `ImeOutputBridge` 输出编辑指令。完整类定义见本文档 §5。

### 2.2 ImeConfig

`ImeConfig` 是统一的运行时配置，同时包含引擎配置和 UI 配置，二者在数据结构上明确隔离。引擎配置（`EngineConfig`）影响引擎的核心行为，UI 配置（`UiConfig`）影响界面呈现和交互反馈。库不内置配置持久化，所有配置通过 `ImeConfig` 在创建时或运行时设置，持久化是应用层的职责。完整定义见本文档 §6。

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
    data class PerformEdit(val action: EditorAction) : ImeIntent()

    // 剪贴板与收藏意图
    data class PasteClip(val text: String) : ImeIntent()
    data class SaveFavorite(val favorite: InputFavorite) : ImeIntent()

    // 配置意图
    data class UpdateConfig(val config: ImeConfig) : ImeIntent()

    // 数据导入导出意图
    data object ExportUserData : ImeIntent()
    data class ImportUserData(val filePath: String) : ImeIntent()
}
```

`ImeIntent` 的设计遵循「意图与手势分离」原则：`ImeIntent` 表达业务语义（如「提交输入」），而非底层手势细节（如「手指抬起」）。手势到意图的映射由 `KeyboardViewModel` 完成，引擎只消费意图，不感知手势。这种分层使得引擎可以在不同的输入模式下复用相同的意图处理逻辑——无论是真实手指操作还是 `InputAction` 的程序化回放，最终都通过 `ImeIntent` 驱动引擎。

### 2.4 ImeOutput

引擎的编辑输出。`ImeOutput` 由引擎内部的 `dispatchToTarget()` 统一分发到 `ImeOutputBridge`，桥梁实现者无需理解 `ImeOutput` 类型体系。

```kotlin
sealed class ImeOutput {
    abstract val timestamp: Long

    data class CommitText(
        override val timestamp: Long,
        val text: String,
        val replacements: List<String>? = null,
    ) : ImeOutput()

    data class RevokeCommit(override val timestamp: Long) : ImeOutput()

    data class InsertPairedSymbols(
        override val timestamp: Long,
        val left: String,
        val right: String,
    ) : ImeOutput()

    data class MoveCursor(
        override val timestamp: Long,
        val direction: CursorDirection,
    ) : ImeOutput()

    data class SelectRange(
        override val timestamp: Long,
        val direction: CursorDirection,
    ) : ImeOutput()

    data class PerformEdit(
        override val timestamp: Long,
        val action: EditorAction,
    ) : ImeOutput()
}
```

`ImeOutput` 的 sealed class 层次确保引擎在分发输出时穷举所有类型，编译期保证类型安全。每种输出类型携带时间戳，用于日志记录和调试追踪。`CommitText` 的 `replacements` 参数支持直输模式下的字符轮换——桥梁实现需检查光标前文本是否匹配替换列表，匹配时执行替换而非插入。

### 2.5 ImeState

`ImeState` 是 MVI 架构中的单一状态树根节点，作为 `ImeEngine` 对外暴露的唯一状态源，所有 UI 组件通过 `StateFlow<ImeState>` 订阅状态驱动重组。不可变 `data class`，所有变更通过 `copy()` 生成新实例。

```kotlin
data class ImeState(
    val keyboard: Keyboard = Keyboard(),
    val inputList: InputList = InputList(),
    val candidateList: CandidateList = CandidateList(),
    val clipboard: Clipboard = Clipboard(),
    val favoriteList: FavoriteList = FavoriteList(),
    val config: ImeConfig = ImeConfig(),
)
```

`ImeState` 的不可变性是线程安全的根本保证：`StateFlow.value` 的读写是原子的，所有状态变更在 `reduce` 中串行执行，不可变 `data class` 无需同步。UI 层通过 `collectAsState()` 订阅 `StateFlow<ImeState>`，状态变更自动驱动 Compose 重组，无需手动通知。详细子状态设计见 [020-ImeState](020-ime-state.md)。

### 2.6 ImeEffect

引擎副作用通道，表达一次性效果信号。与 `ImeState` 的持续状态不同，`ImeEffect` 表达的是「发生了某件事」的事件语义——引擎发出信号后不维护其状态，UI 层消费后即丢弃。

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

    data class PlayAudio(val type: AudioType) : ImeEffect()
    data class PlayHaptic(val type: HapticType) : ImeEffect()
    data class ConfirmFavorite(val content: String) : ImeEffect()
}

enum class AudioType {
    KeyPress, CandidateSelect, Slip, PageFlip,
}

enum class HapticType {
    LightTap, MediumTap, HeavyTap,
}
```

`ImeEffect` 通过 `SharedFlow<ImeEffect>` 发射，UI 层收集后立即消费，不存在重复消费和状态清理问题。`ImeState` 专注于持续性状态，`ImeEffect` 专注于一次性效果，两者共同构成引擎的完整输出。将弹出提示、音效播放、触觉振动等一次性效果从 `ImeState` 中分离，避免了状态清理负担、语义不匹配和重复消费风险。感官反馈（`PlayAudio` / `PlayHaptic`）的播放器接口定义在 `:ime-ui` 中，平台实现由 `:app` 提供——引擎仅负责决定「何时」触发反馈，UI 层负责「是否和如何」播放反馈。详见 [065-音效与触觉反馈](065-audio-haptic-feedback.md)。

---

## 3. MVI 数据流

引擎采用 MVI（Model-View-Intent）架构，实现严格的单向数据流。MVI 架构将引擎的输入、状态、输出和副作用四个维度明确分离，每个维度通过独立的类型和通道表达，确保数据流的可追踪性和可预测性。

### 3.1 数据流描述

MVI 数据流由四条通道构成，每条通道有明确的语义和方向：

- **输入通道**：`ImeIntent` → `ImeEngine.handleIntent()` → `reduce(state, intent)` → 新 `ImeState`。用户操作统一编码为 `ImeIntent`，由 `ImeEngine` 的 `handleIntent()` 方法接收。`reduce` 函数是纯函数，接收当前 `ImeState` 和 `ImeIntent`，返回新的 `ImeState`，不产生副作用。
- **状态通道**：`ImeState` 通过 `StateFlow<ImeState>` 暴露。UI 层订阅 `StateFlow`，状态变更自动驱动重组。`StateFlow` 保证值的原子性和一致性——订阅者始终读取到最新的完整状态快照，不存在部分更新的问题。
- **输出通道**：`ImeOutput` 由 `ImeEngine` 的 `dispatchToTarget()` 统一分发到 `ImeOutputBridge`。桥梁实现者只需实现语义方法，无需理解 `ImeOutput` 类型体系。输出通道承担所有对目标编辑器的操作（提交文本、移动光标、插入配对符号等）。
- **副作用通道**：`ImeEffect` 通过 `SharedFlow<ImeEffect>` 发射。一次性效果（弹出提示、音效、确认对话框）通过此通道传递，UI 层消费后即丢弃。副作用通道与状态通道的分离确保了一次性效果不会在配置变更或进程重建时被重复消费。

### 3.2 数据流图

```plantuml
@file:../diagrams/engine-mvi-data-flow.puml
```

上图展示了引擎的 MVI 数据流全景。用户操作（`InputGesture`）经 `KeyboardViewModel` 转换为 `ImeIntent`，由 `ImeEngine.handleIntent()` 接收。引擎内部经过 `KeyboardIntentHandler` → `KeyboardStateMachine` → `reduce` 的处理链，产生新的 `ImeState`（通过 `StateFlow` 暴露）、`ImeOutput`（通过 `ImeOutputBridge` 分发）和 `ImeEffect`（通过 `SharedFlow` 发射）。UI 层同时订阅 `StateFlow<ImeState>` 和 `SharedFlow<ImeEffect>`，分别驱动界面重组和一次性效果展示。

### 3.3 数据流不变式

MVI 数据流遵循以下不变式，确保数据流的可追踪性和可预测性：

1. **单一状态源**：`ImeState` 是引擎对外的唯一状态源，不存在其他状态通道或旁路。UI 层的所有渲染数据均来自 `StateFlow<ImeState>`，不持有独立的业务状态副本。
2. **单向数据流**：数据从 `ImeIntent` 流向 `ImeState`/`ImeOutput`/`ImeEffect`，不存在反向依赖。`ImeState` 的变更不触发新的 `ImeIntent`——状态变更是 reduce 的结果而非原因。
3. **纯函数 reduce**：`reduce(state, intent)` 是纯函数，相同输入始终产生相同输出，不依赖外部状态，不产生副作用。异步操作（如字典查询）通过 `sideEffects` 列表延迟执行。
4. **副作用隔离**：需要异步处理的操作通过 `KeyboardStateTransition.Result.sideEffects` 返回 `List<ImeIntent>`，由 `ImeEngine` 异步处理。一次性 UI 效果通过 `ImeEffect` 通道发射。两种副作用机制互不干扰。

---

## 4. 核心模型概览

引擎库的核心模型按职责划分为九个子系统，各子系统拥有独立的设计文档。本节提供每个子系统的简要概览和文档索引，帮助读者快速定位到感兴趣的领域进行深入阅读。

### 4.1 键盘状态机

`Keyboard` / `KeyboardState` / `KeyboardStateMachine` 构成引擎的核心控制逻辑。`KeyboardState` 以 sealed class 表达键盘交互的有限状态机，涵盖空闲、拼音输入（等待、滑行、翻动）、候选选择、提交选项、编辑器编辑、符号选择、Emoji 选择等状态。`KeyboardStateMachine` 集中管理状态转换规则，`KeyboardIntentHandler` 子类负责将 `ImeIntent` 映射为 `KeyboardStateTransition`。三层映射模型（`ImeIntent` → `KeyboardStateTransition` → `KeyboardState`）确保意图语义与状态转换规则解耦。

详见 [030-键盘状态机](030-keyboard-state-machine.md)。

### 4.2 ImeState 子状态

`ImeState` 是 MVI 架构的单一状态树根节点，包含 `keyboard`、`inputList`、`candidateList`、`clipboard`、`favoriteList`、`config` 六个字段。各子状态均为不可变 `data class`，通过 `copy()` 模式创建新实例。`ImeEffect` 副作用通道与 `ImeState` 分离，表达一次性效果信号。

详见 [020-ImeState](020-ime-state.md)。

### 4.3 输入列表

`InputList` / `InputItem` 构成引擎的核心数据结构，管理用户输入的字符序列、游标位置、待确认输入和输入补全。`InputItem` 包含 `Char`（字符输入）、`Gap`（游标位置）和 `MathExpr`（数学表达式）三种类型。`InputListOperator` 提供线程安全的操作方法，`InputListEditor` 支持撤销/恢复。

详见 [040-输入列表](040-input-list.md)。

### 4.4 候选列表与字典

`CandidateList` 管理候选词的分页、过滤和排序。`ImeDictProvider` 是字典查询的抽象接口，`ImeSqliteDictProvider` 是基于 Room 的默认实现，内部委托 `DictRepository` 完成数据库操作。字典系统包含拼音字词查询、用户输入历史、HMM 短语预测和 Viterbi 解码。

详见 [050-候选与字典](050-candidate-and-dict.md)。

### 4.5 输出桥接

`ImeOutputBridge` 是引擎与目标编辑器之间的桥梁接口，采用桥接模式实现输出目标与引擎的解耦。`BaseImeOutputBridge` 提供单快照撤销机制的抽象基类，`InputConnectionBridge` 和 `EditTextBridge` 分别面向系统输入连接和 `EditText` 目标的实现。

详见 [060-输出桥接](060-intent-output-bridge.md)。

### 4.6 音效与触觉反馈

`ImeEffect.PlayAudio` 和 `ImeEffect.PlayHaptic` 是感官反馈信号，遵循 fire-and-forget 语义。感官反馈的播放器接口（`AudioPlayer` / `HapticPlayer`）定义在 `:ime-ui` 中，平台实现（`AndroidAudioPlayer` / `AndroidHapticPlayer`）由 `:app` 提供。引擎仅负责决定「何时」触发反馈，UI 层负责「是否和如何」播放反馈。

详见 [065-音效与触觉反馈](065-audio-haptic-feedback.md)。

### 4.7 剪贴板与收藏

`ClipboardService` 监听系统剪贴板变更并提供文本类型检测（URL、验证码、手机号等）。`FavoriteService` 管理用户收藏的文本，通过 Room DAO 实现响应式查询。两者均通过 `Feature` 枚举进行门控，禁用时调用相关 Intent 立即抛出异常。

详见 [070-剪贴板与收藏](070-clipboard-and-favorites.md)。

### 4.8 输入动作程序化

`InputAction` sealed class 定义坐标无关的逻辑动作（按下、滑行到、抬起、选择候选等）。`InputActionScript` 将动作序列组合为可回放的脚本，`InputActionScriptCompiler` 将待输入文本编译为动作脚本。归一化坐标基础类型 `OffsetF` / `RectF` 和路径插值算法确保跨面板、跨尺寸的回放一致性。

详见 [080-输入动作](080-input-action.md)。

### 4.9 日志系统

`ImeLog` 是日志系统的全局门面，`ImeLogger` 提供带标签的日志记录器。`LogWriter` 接口支持可扩展的输出目标，`FileLogWriter` 实现异步批量文件写入，`LogcatWriter` 提供 Android Logcat 输出，`CrashInterceptor` 实现 JVM 崩溃拦截。日志等级由应用层注入，引擎不内置构建类型判断。

详见 [090-日志系统](090-logging.md)。

---

## 5. ImeEngine 完整类定义

`ImeEngine` 是引擎库的核心入口点，提供完整的输入法能力。引擎不依赖任何 UI 框架，通过 `StateFlow` 暴露状态，通过 `ImeIntent` 接收用户操作，通过 `ImeOutputBridge` 输出编辑指令。`ImeEngine` 的构造函数标记为 `internal`，强制通过 `Companion.create()` 工厂方法创建实例，确保所有依赖项正确初始化。

```kotlin
class ImeEngine internal constructor(
    private var config: ImeConfig,
    private val dictProvider: ImeDictProvider,
    private val stateMachine: KeyboardStateMachine,
    private val inputListOp: InputListOperator,
    private val featureRegistry: FeatureRegistry,
) {
    private val _state = MutableStateFlow(ImeState())
    val state: StateFlow<ImeState> = _state.asStateFlow()

    private val _effect = MutableSharedFlow<ImeEffect>(extraBufferCapacity = 16)
    val effect: SharedFlow<ImeEffect> = _effect.asSharedFlow()

    private var _outputBridge: ImeOutputBridge? = null

    fun attachOutputBridge(bridge: ImeOutputBridge) { ... }
    fun detachOutputBridge() { ... }
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
| `config` | `ImeConfig` | 运行时配置，包含引擎配置和 UI 配置。`var` 声明允许运行时通过 `updateConfig()` 修改 |
| `dictProvider` | `ImeDictProvider` | 字典查询接口，由外部注入。默认实现 `ImeSqliteDictProvider` 基于 Room，第三方可替换 |
| `stateMachine` | `KeyboardStateMachine` | 键盘状态机，集中管理 `KeyboardState` 的转换规则和状态历史 |
| `inputListOp` | `InputListOperator` | 输入列表操作器，提供线程安全的 `InputList` 变更方法 |
| `featureRegistry` | `FeatureRegistry` | 功能注册表，管理 `Feature` 的启用/禁用状态和门控检查 |

### 5.2 状态暴露

`_state` 是内部的 `MutableStateFlow<ImeState>`，对外暴露只读的 `StateFlow<ImeState>`。`StateFlow` 保证值的原子性——订阅者始终读取到最新的完整状态快照。`_effect` 是内部的 `MutableSharedFlow<ImeEffect>`，`extraBufferCapacity = 16` 确保高频率发射时不会因订阅者处理慢而丢弃事件。对外暴露只读的 `SharedFlow<ImeEffect>`。

### 5.3 输出桥接

`_outputBridge` 是可空的 `ImeOutputBridge?`，通过 `attachOutputBridge()` 注册，`detachOutputBridge()` 注销。引擎在分发 `ImeOutput` 时检查桥接是否存在：若存在则调用对应的语义方法，若不存在则静默忽略。这种设计允许引擎在没有桥接的情况下正常运行（例如纯逻辑测试场景），输出操作被自动跳过。

### 5.4 工厂方法

`Companion.create()` 是 `ImeEngine` 的唯一创建入口，内部完成以下初始化工作：创建 `KeyboardStateMachine`、创建 `InputListOperator`、根据 `ImeConfig.engine.features` 创建 `FeatureRegistry`、根据 `ImeConfig.engine.keyboardType` 设置初始键盘状态。工厂方法确保所有依赖项正确初始化，避免外部构造时遗漏关键组件。

### 5.5 使用示例

```kotlin
val engine = ImeEngine.create(
    config = ImeConfig(
        engine = ImeConfig.EngineConfig(
            keyboardType = KeyboardType.Pinyin,
            handMode = HandMode.Right,
            features = setOf(Feature.Clipboard, Feature.Favorites),
        ),
    ),
    dictProvider = ImeSqliteDictProvider(context),
)

// 接入桥接
val bridge = InputConnectionBridge { currentInputConnection }
engine.attachOutputBridge(bridge)

// 订阅状态
engine.state.collect { state -> updateUI(state) }

// 发送意图
engine.handleIntent(ImeIntent.SwitchKeyboard(KeyboardType.Latin))
```

---

## 6. ImeConfig 配置模型

`ImeConfig` 是统一的运行时配置，同时包含引擎配置和 UI 配置，二者在数据结构上明确隔离。引擎配置（`engine`）影响引擎的核心行为，UI 配置（`ui`）影响界面呈现和交互反馈。库不内置配置持久化，所有配置通过 `ImeConfig` 在创建时或运行时设置，持久化是应用层的职责（如 `:app` 模块使用 DataStore）。

> **设计决策**：`ImeConfig` 合并了引擎配置与应用配置的职责，消除两套配置之间的字段重叠和同步问题。运行时修改始终优先于持久化配置——`ImeConfig.runtimeOverrides` 记录被运行时临时修改的字段，持久化同步时跳过这些字段。应用重启时，`ImeConfig` 根据持久化配置重新初始化。

```kotlin
data class ImeConfig(
    val engine: EngineConfig = EngineConfig(),
    val ui: UiConfig = UiConfig(),
    val runtimeOverrides: Set<ConfigField> = emptySet(),
) {
    data class EngineConfig(
        val keyboardType: KeyboardType = KeyboardType.Pinyin,
        val handMode: HandMode = HandMode.Right,
        val features: Set<Feature> = Feature.DefaultSet,
        val candidatePredictionEnabled: Boolean = true,
        val singleLineInput: Boolean = false,
    )

    data class UiConfig(
        val themeType: ThemeType = ThemeType.FollowSystem,
        val audioFeedbackEnabled: Boolean = true,
        val hapticFeedbackEnabled: Boolean = true,
        val keyAnimationEnabled: Boolean = true,
        val keyPopupTipsEnabled: Boolean = true,
        val gestureSlippingTrailEnabled: Boolean = true,
        val clipPopupTipsEnabled: Boolean = true,
        val clipPopupTipsTimeout: Int = 15,
        val adaptDesktopSwipeUpGesture: Boolean = false,
        val candidateVariantFirstEnabled: Boolean = false,
        val userInputDataEnabled: Boolean = true,
        val candidatesPagingAudioEnabled: Boolean = true,
        val practicePlaybackSpeed: Float = 1.0f,
        val practiceShowFingerOverlay: Boolean = true,
        val practiceShowSwipeTrail: Boolean = true,
        val logLevel: LogLevel = LogLevel.WARN,
        val logStoragePath: String? = null,
    )
}
```

### 6.1 EngineConfig 字段说明

| 字段 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `keyboardType` | `KeyboardType` | `Pinyin` | 初始键盘类型，运行时通过 `ImeState.keyboard.type` 访问当前类型 |
| `handMode` | `HandMode` | `Right` | 手模式，影响键盘布局的左右手偏移 |
| `features` | `Set<Feature>` | `DefaultSet` | 启用的功能集合，门控剪贴板、收藏、输入练习、候选预测等 |
| `candidatePredictionEnabled` | `Boolean` | `true` | 是否启用候选预测（HMM + Viterbi），影响短语预测功能 |
| `singleLineInput` | `Boolean` | `false` | 是否启用单行输入模式，影响输入列表的显示方式 |

### 6.2 UiConfig 字段说明

| 字段 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `themeType` | `ThemeType` | `FollowSystem` | 主题模式（跟随系统/亮色/暗色） |
| `audioFeedbackEnabled` | `Boolean` | `true` | 是否启用按键音效反馈 |
| `hapticFeedbackEnabled` | `Boolean` | `true` | 是否启用触觉反馈 |
| `keyAnimationEnabled` | `Boolean` | `true` | 是否启用按键动画 |
| `keyPopupTipsEnabled` | `Boolean` | `true` | 是否启用按键弹出提示 |
| `gestureSlippingTrailEnabled` | `Boolean` | `true` | 是否启用滑行轨迹显示 |
| `clipPopupTipsEnabled` | `Boolean` | `true` | 是否启用剪贴板弹出提示 |
| `clipPopupTipsTimeout` | `Int` | `15` | 剪贴板弹出提示超时（秒） |
| `adaptDesktopSwipeUpGesture` | `Boolean` | `false` | 是否适配桌面下滑手势 |
| `candidateVariantFirstEnabled` | `Boolean` | `false` | 候选词变体是否优先显示 |
| `userInputDataEnabled` | `Boolean` | `true` | 是否启用用户输入数据记录 |
| `candidatesPagingAudioEnabled` | `Boolean` | `true` | 候选词翻页是否播放音效 |
| `practicePlaybackSpeed` | `Float` | `1.0f` | 输入练习回放速度倍率 |
| `practiceShowFingerOverlay` | `Boolean` | `true` | 输入练习是否显示手指覆盖层 |
| `practiceShowSwipeTrail` | `Boolean` | `true` | 输入练习是否显示滑行轨迹 |
| `logLevel` | `LogLevel` | `WARN` | 发布版本的日志等级 |
| `logStoragePath` | `String?` | `null` | 日志文件存放目录路径，`null` 使用默认应用私有目录 |

### 6.3 runtimeOverrides 机制

`runtimeOverrides` 记录被运行时临时修改的配置字段名称集合。当应用层将 `ImeConfig` 持久化到 DataStore 时，需要跳过 `runtimeOverrides` 中记录的字段——这些字段的值来自运行时修改（如用户在输入过程中临时切换手模式），而非用户的持久化偏好。应用重启时，被跳过的字段使用 DataStore 中的持久化值重新初始化。

---

## 7. Feature 门控机制

Feature 门控机制允许引擎在运行时根据配置启用或禁用特定功能。每个 `Feature` 枚举值对应一个功能域，禁用后该功能域的所有操作将被拦截并抛出异常。Feature 门控是引擎 Fail Fast 原则的核心实现之一，确保开发者在开发阶段尽早发现功能使用与配置不匹配的问题。

### 7.1 Feature 枚举

```kotlin
enum class Feature {
    Clipboard,            // 剪贴板监听和粘贴
    Favorites,            // 收藏管理
    InputPractice,        // 输入练习演示
    CandidatePrediction;  // 候选预测（HMM + Viterbi）

    companion object {
        val DefaultSet: Set<Feature> = setOf(Clipboard, Favorites, CandidatePrediction)
    }
}
```

### 7.2 Feature 枚举值说明

| Feature | 功能域 | 禁用影响 |
|---------|--------|---------|
| `Clipboard` | 剪贴板监听和粘贴 | `ImeState.clipboard.disabled = true`，`clipboard.clips` 始终为空，调用 `ImeIntent.PasteClip` 抛出 `IllegalStateException` |
| `Favorites` | 收藏管理 | `ImeState.favoriteList.disabled = true`，`favoriteList.favorites` 始终为空，调用 `ImeIntent.SaveFavorite` 抛出 `IllegalStateException` |
| `InputPractice` | 输入练习演示 | 禁用输入动作脚本的编译和回放，调用 `InputActionScriptCompiler.compile()` 抛出 `IllegalStateException` |
| `CandidatePrediction` | 候选预测（HMM + Viterbi） | `EngineConfig.candidatePredictionEnabled = false`，字典查询跳过 HMM 预测和 Viterbi 解码步骤，仅返回基础候选词 |

### 7.3 DefaultSet 说明

`Feature.DefaultSet` 包含 `Clipboard`、`Favorites` 和 `CandidatePrediction` 三个功能，覆盖大多数用户的使用需求。`InputPractice` 默认不启用，因为输入练习演示是开发者/教学场景下的辅助功能，普通用户不需要。

### 7.4 FeatureRegistry 门控检查

`FeatureRegistry` 是引擎内部的功能注册表，管理 `Feature` 的启用/禁用状态和门控检查。`FeatureRegistry` 提供以下核心方法：

```kotlin
class FeatureRegistry(private val features: Set<Feature>) {

    /** 检查功能是否启用，未启用时抛出 IllegalStateException */
    fun require(feature: Feature) {
        check(feature in features) {
            "Feature $feature is not enabled. Add it to ImeConfig.engine.features to enable."
        }
    }

    /** 检查功能是否启用，返回布尔值 */
    fun isEnabled(feature: Feature): Boolean = feature in features
}
```

`require()` 方法在引擎处理 `ImeIntent` 时被调用，作为功能门控的守卫检查。例如，`ImeEngine.handleIntent(ImeIntent.PasteClip(...))` 的处理逻辑中首先调用 `featureRegistry.require(Feature.Clipboard)`，若 `Clipboard` 功能未启用则立即抛出异常。这种 Fail Fast 的门控方式确保非法操作在调用点即被发现，而非在后续处理中产生不确定行为。

### 7.5 Feature 与 ImeConfig 的同步

`Feature` 的启用/禁用状态存储在 `ImeConfig.engine.features` 中，`FeatureRegistry` 在 `ImeEngine.create()` 时根据配置初始化。运行时通过 `ImeEngine.updateConfig()` 修改 `features` 集合后，`FeatureRegistry` 同步更新，后续的门控检查使用新的功能集合。`ImeState.config` 始终反映当前生效的配置，UI 层可根据 `Feature` 的启用状态显示或隐藏对应的功能入口。

---

## 8. reduce 函数的核心逻辑

`handleIntent()` 是 `ImeEngine` 处理用户意图的核心方法，内部通过六步处理链将 `ImeIntent` 转化为状态变更、编辑输出和副作用信号。整个处理链在主线程上同步执行，异步操作通过 `sideEffects` 列表延迟到独立协程中处理，确保 `reduce` 函数的纯函数特性。

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
Step 3: 处理 sideEffects（异步意图如字典查询）
  │
  ▼
Step 4: 通过 copy() 模式更新 ImeState
  │
  ▼
Step 5: 分发 ImeOutput 到 ImeOutputBridge
  │
  ▼
Step 6: 发射 ImeEffect 到 SharedFlow
```

### 8.2 Step 1：ImeIntent → KeyboardStateTransition

`ImeEngine` 维护一个 `Map<KeyboardType, KeyboardIntentHandler>` 注册表，根据当前 `ImeState.keyboard.type` 选择对应的 `KeyboardIntentHandler` 实现。各 `KeyboardIntentHandler` 子类是无状态的策略对象，负责将 `ImeIntent` 映射为 `KeyboardStateTransition`，再由 `KeyboardStateMachine` 执行转换。

当 `ImeIntent` 为 `SwitchKeyboard` 时，直接切换键盘类型并设置初始状态，不经过 `KeyboardIntentHandler`。其他 `ImeIntent` 均委托给当前键盘类型的 `KeyboardIntentHandler` 处理。`KeyboardIntentHandler.handleIntent()` 接收 `ImeIntent` 和当前 `KeyboardState`，返回 `KeyboardStateTransition.Result`（包含新状态和副作用列表）。

### 8.3 Step 2：KeyboardStateMachine 执行 transition

`KeyboardStateMachine.transition()` 是状态转换的集中处理器，接收 `KeyboardStateTransition`，根据当前状态执行转换规则，返回 `KeyboardStateTransition.Result`。`Result` 包含两个部分：

- `newState: KeyboardState`：转换后的新状态。若转换在当前状态下不合法，`newState` 等于原状态（Fail-safe 行为）。
- `sideEffects: List<ImeIntent>`：转换产生的副作用意图列表，包含需要异步处理的操作（如字典查询、音频播放、输出桥接等）。`KeyboardStateMachine` 本身是纯函数，不直接执行副作用，而是通过返回副作用列表交由 `ImeEngine` 异步处理。

若 `newState` 与原状态不同，`KeyboardStateMachine` 将原状态推入 `KeyboardStateHistory` 有界栈（最大 10 层），供后续回退使用。

### 8.4 Step 3：处理 sideEffects

`sideEffects` 是 `KeyboardStateTransition.Result` 中返回的 `List<ImeIntent>`，包含状态转换产生的异步操作意图。`ImeEngine` 逐个处理 `sideEffects` 中的 `ImeIntent`，通过递归调用 `handleIntent()` 执行。典型的副作用意图包括：

- `ImeIntent.SelectCandidate(...)`：候选词选中后触发字典查询和输入列表确认
- `ImeIntent.CommitInput`：输入提交后触发 `ImeOutput.CommitText` 输出
- `ImeIntent.DeleteInput`：输入删除后触发 `ImeOutput.RevokeCommit` 输出

副作用处理的递归深度受引擎内部保护，超过阈值时抛出异常，防止无限递归。

### 8.5 Step 4：更新 ImeState

`ImeState` 通过 `copy()` 模式创建新实例，确保不可变性。每次 `handleIntent()` 调用产生一个新的 `ImeState`，通过 `MutableStateFlow.value` 原子更新。`ImeState` 的更新涉及多个子状态的协调变更：

- `keyboard`：更新 `keyboard.state` 为新的 `KeyboardState`
- `inputList`：根据意图类型更新输入列表（追加字符、确认候选、删除输入等）
- `candidateList`：根据字典查询结果更新候选列表
- `clipboard` / `favoriteList`：根据剪贴板和收藏操作更新对应子状态
- `config`：根据配置变更意图更新运行时配置

各子状态的变更通过一次 `copy()` 操作原子完成，不存在中间状态被外部观察到的风险。

### 8.6 Step 5：分发 ImeOutput 到 ImeOutputBridge

`ImeEngine` 在 `reduce` 过程中收集需要输出的 `ImeOutput` 实例，在状态更新完成后统一分发到 `ImeOutputBridge`。分发通过 `dispatchToTarget()` 方法实现，内部使用 `when(ImeOutput)` 穷举所有输出类型，调用桥接的对应语义方法：

- `ImeOutput.CommitText` → `bridge.commitText(text, replacements)`
- `ImeOutput.RevokeCommit` → `bridge.revokeCommit()`
- `ImeOutput.InsertPairedSymbols` → `bridge.insertPairedSymbols(left, right)`
- `ImeOutput.MoveCursor` → `bridge.moveCursor(direction)`
- `ImeOutput.SelectRange` → `bridge.selectRange(direction)`
- `ImeOutput.PerformEdit` → `bridge.performAction(action)`

若 `_outputBridge` 为 `null`，分发操作被跳过。所有桥接方法在主线程调用，确保线程安全。

### 8.7 Step 6：发射 ImeEffect 到 SharedFlow

`ImeEngine` 在 `reduce` 过程中收集需要发射的 `ImeEffect` 实例，在状态更新和输出分发完成后统一发射。发射通过 `_effect.tryEmit()` 实现，`extraBufferCapacity = 16` 确保高频率发射时不会因订阅者处理慢而丢弃事件。

典型的 `ImeEffect` 发射场景：
- 键盘类型切换时发射 `ImeEffect.PopupTip.Message("已切换到拉丁键盘")`
- 按键处理时发射 `ImeEffect.PlayAudio(AudioType.KeyPress)` 和 `ImeEffect.PlayHaptic(HapticType.LightTap)`
- 候选词选择时发射 `ImeEffect.PlayAudio(AudioType.CandidateSelect)`
- 收藏保存时发射 `ImeEffect.ConfirmFavorite(content)`

`ImeEffect` 的发射时机在 `ImeState` 更新之后，确保 UI 层先观察到状态变更，再处理副作用信号。这种时序保证了一次性效果（如弹出提示）所依赖的渲染状态已经就绪。
