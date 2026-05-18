# 引擎库设计总览

`ime-engine` 是筷字输入法的引擎库，提供核心 IME 引擎能力。引擎库独立设计的目标是使输入法的逻辑层与 UI 和应用之间实现分离、解耦，从而方便第三方定制自己的 UI、修改交互逻辑等。第三方应用只需引入 `:ime-engine` 即可获得完整的输入法能力——拼音输入、滑行输入、X-Pad 连续输入、候选选择、输入列表管理、撤销重做等——无需依赖系统 IME 服务或任何 UI 框架。

---

## 1 设计目标

| 定位 | 说明 |
|------|------|
| **逻辑与 UI 分离** | 引擎库独立设计的目标是使输入法的逻辑层与 UI 和应用之间实现分离、解耦，从而方便第三方定制自己的 UI、修改交互逻辑等 |
| **MVI 驱动** | 通过 `StateFlow<ImeState>` 暴露状态，通过 `ImeIntent` 接收操作，通过 `ImeOutput` 输出编辑指令 |
| **可嵌入** | 第三方应用只需引入 `:ime-engine` 即可获得完整输入法能力，无需系统 IME 服务 |
| **可扩展** | 字典接口与实现分离（`ImeDictProvider`），输出桥接可自定义（`ImeOutputBridge`），功能可裁剪（`Feature`） |
| **Fail Fast** | 非法操作（如禁用收藏后调用收藏功能）立即抛出异常而非静默忽略 |

引擎库的「逻辑与 UI 分离」定位意味着第三方应用可以完全用自定义 UI 替换 `:ime-ui` 而不影响引擎功能，也可以仅引入 `:ime-engine` 自行实现视图层和交互逻辑。唯一依赖 Android 的部分是字典 I/O（`ImeSqliteDictProvider` 使用 Room），但第三方可以提供自己的 `ImeDictProvider` 实现来消除 Android 依赖。

「MVI 驱动」定位是引擎与 UI 完全分离的技术基础。引擎不依赖任何 UI 框架，所有状态变更通过 `StateFlow` 暴露，所有用户操作通过 `ImeIntent` 接收，所有编辑指令通过 `ImeOutputBridge` 输出。这种单向数据流使得引擎可以被任意 UI 框架（Compose、View、Web、游戏引擎等）消费，而不需要引擎感知 UI 的存在。

---

## 2 核心 class 关系图

```plantuml
@file:../diagrams/engine-overview.puml
```

上图展示了引擎库的核心类关系，按职责分为三层：

- **核心模型**（橙色）：`ImeEngine`、`ImeConfig`、`ImeIntent`、`ImeOutput`、`ImeOutputBridge` 构成引擎库的核心模型，是输入法业务逻辑的基石。第三方应用和 `:ime-ui` 库仅依赖这些核心类型，不依赖引擎内部实现
- **状态类型**（绿色）：`ImeState` 及其子状态类型（`InputList`、`CandidateList`、`Clipboard`、`FavoriteList`），均为不可变 `data class`，通过 `StateFlow` 自动传播到 UI
- **内部组件**（蓝色）：`KeyboardStateMachine`、`InputListOperator`、`FeatureRegistry`、`ImeDictProvider` 等，由 `ImeEngine` 内部组合使用，不对外暴露

---

## 3 核心模型概览

引擎库的核心模型构成了 `:ime-engine` 与 `:ime-ui`、`:app` 之间的核心契约，也是第三方应用使用引擎库的主要接口。引擎的主要职能是输入法的业务逻辑——状态机驱动、字典查询、候选排序、输入管理等——而非单纯暴露 API。

### 3.1 ImeEngine

`ImeEngine` 是引擎库的核心入口点，提供完整的输入法能力。引擎不依赖任何 UI 框架，通过 `StateFlow` 暴露状态，通过 `Intent` 接收用户操作，通过 `ImeOutputBridge` 输出编辑指令。

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

    private var _outputBridge: ImeOutputBridge? = null

    fun attachOutputBridge(bridge: ImeOutputBridge)
    fun detachOutputBridge()
    fun handleIntent(intent: ImeIntent)
    fun updateConfig(block: (ImeConfig) -> ImeConfig)

    companion object {
        fun create(config: ImeConfig = ImeConfig(), dictProvider: ImeDictProvider): ImeEngine
    }
}
```

使用方式：

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

输出分发方式：

- **主路径**：通过 `ImeOutputBridge` 桥接模式，引擎内部统一 `when` 分发，桥梁实现者只需实现语义方法，无需理解 `ImeOutput` 类型体系
- **备用路径**：通过 `output: ReceiveChannel<ImeOutput>` 通道，供高级场景使用

### 3.2 ImeConfig

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
        val xPadEnabled: Boolean = true,
        val audioFeedbackEnabled: Boolean = true,
        val hapticFeedbackEnabled: Boolean = true,
        val keyAnimationEnabled: Boolean = true,
        val keyPopupTipsEnabled: Boolean = true,
        val gestureSlippingTrailEnabled: Boolean = true,
        val clipPopupTipsEnabled: Boolean = true,
        val clipPopupTipsTimeout: Int = 15,
        val adaptDesktopSwipeUpGesture: Boolean = false,
        val candidateVariantFirstEnabled: Boolean = false,
        val latinUsePinyinKeysInXPadEnabled: Boolean = false,
        val userInputDataEnabled: Boolean = true,
        val candidatesPagingAudioEnabled: Boolean = true,
        val practicePlaybackSpeed: Float = 1.0f,
        val practiceShowFingerOverlay: Boolean = true,
        val practiceShowSwipeTrail: Boolean = true,
        val logLevel: LogLevel = LogLevel.WARN,
        val logStoragePath: String? = null,
    )
}

enum class Feature {
    Clipboard,       // 剪贴板监听和粘贴
    Favorites,       // 收藏管理
    InputPractice,   // 输入练习演示
    CandidatePrediction; // 候选预测（HMM + Viterbi）

    companion object {
        val DefaultSet: Set<Feature> = setOf(Clipboard, Favorites, CandidatePrediction)
    }
}
```

### 3.3 ImeOutput

引擎的编辑输出。`ImeOutput` 由引擎内部的 `dispatchToTarget()` 统一分发到 `ImeOutputBridge`，桥梁实现者无需理解 `ImeOutput` 类型体系。桥接接口和基础抽象类的完整设计见 [090-输出桥接机制](090-output-bridge.md)。

```kotlin
sealed class ImeOutput {
    abstract val timestamp: Long

    data class CommitText(override val timestamp: Long, val text: String, val replacements: List<String>? = null) : ImeOutput()
    data class RevokeCommit(override val timestamp: Long) : ImeOutput()
    data class InsertPairedSymbols(override val timestamp: Long, val left: String, val right: String) : ImeOutput()
    data class MoveCursor(override val timestamp: Long, val direction: CursorDirection) : ImeOutput()
    data class SelectRange(override val timestamp: Long, val direction: CursorDirection) : ImeOutput()
    data class PerformEdit(override val timestamp: Long, val action: EditorAction) : ImeOutput()
}
```

### 3.4 ImeIntent

用户意图的 sealed class 表达，所有用户操作统一为 `ImeIntent`，由 `ImeEngine.handleIntent()` 接收并处理。`InputGesture` 是输入面板的输出，`ImeIntent` 是引擎的输入，`KeyboardViewModel` 负责将前者转换为后者（转换逻辑见 [060-KeyboardViewModel](../ui/060-keyboard-view-model.md)）。

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

    // X-Pad 意图
    data class SelectXPadPath(val startZone: XPadZone, val path: List<XPadZone>) : ImeIntent()

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

### 3.5 ImeState 子状态类型

`ImeState` 中引用的子状态类型均为 `data class`，不可变，通过 `copy()` 模式创建新实例。

```kotlin
data class InputList(
    val inputs: List<InputItem> = emptyList(),
    val gapIndex: Int = 0,
    val pendingSpell: String = "",
)

sealed class InputItem {
    abstract val id: String
    data class Char(override val id: String, val value: String, val isCommitted: Boolean = false) : InputItem()
    data class Gap(override val id: String) : InputItem()
    data class MathExpr(override val id: String, val expression: String) : InputItem()
}

data class CandidateList(
    val candidates: List<InputWord> = emptyList(),
    val pageIndex: Int = 0,
    val pageSize: Int = 20,
    val hasMore: Boolean = false,
)

data class Clipboard(
    val currentText: String? = null,
    val showTip: Boolean = false,
)

data class FavoriteList(
    val favorites: List<InputFavorite> = emptyList(),
    val isLoading: Boolean = false,
)
```

---

## 4 子系统索引

| 文档 | 说明 |
|------|------|
| [020-键盘状态机](020-state-machine.md) | KeyboardState sealed class 层次结构、状态转换规则、Keyboard 组合模式、InputKey 体系、StateHistory 有界历史栈 |
| [030-输入列表](030-input-list.md) | InputList 不可变数据模型、InputItem/InputWord/InputCompletion 类型、线程安全设计、撤销机制、游标管理、InputListEditor |
| [040-字典系统](040-dict-system.md) | DictRepository + DAO 接口、Room 数据库与 Entity、ImeDictProvider/ImeSqliteDictProvider、PinyinCharsTree 前缀树、HmmModel + ViterbiDecoder |
| [050-X-Pad 核心](050-xpad-core.md) | HexGrid 六边形网格计算、XPadZone/XPadLayout 区域定义、KeyboardState.PinyinInput.XPadding 状态集成 |
| [060-输入动作程序化](060-input-action.md) | InputAction sealed class、InputActionScript、InputMethod 枚举、PinyinSegment、InputActionScriptCompiler 脚本编译器 |
| [070-剪贴板与收藏](070-clipboard-and-favorites.md) | ClipboardService 剪贴板监听与类型检测、FavoriteService 收藏管理、InputClip/InputFavorite 数据模型 |
| [080-日志系统](080-logging.md) | ImeLog 门面、ImeLogger 带标签记录器、LogLevel 枚举、LogEntry 不可变条目、LogWriter 接口、LogStorage 文件存储管理、FileLogWriter 异步文件写入、LogcatWriter Android Logcat 输出、CrashInterceptor 崩溃拦截 |
| [090-输出桥接机制](090-output-bridge.md) | ImeOutputBridge 桥接模式、BaseImeOutputBridge 抽象类、InputConnectionBridge 系统输入连接、EditTextBridge EditText 桥接 |
