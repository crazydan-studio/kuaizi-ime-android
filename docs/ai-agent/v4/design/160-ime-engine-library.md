# 160 — IME 引擎库与 UI 库设计

## 1. 概述

v4 版本将筷字输入法设计为三层库架构，支持其他程序以库的形式引入，提供完整的输入法能力支持。该架构对应 Java 版本中 `ImeSupportEditText` + `ImeIntegratedActivity` 的同等能力，但远超原有设计：

- **引擎库 `:ime-engine`**：纯 Kotlin，不依赖 Android 框架（字典 I/O 除外），提供核心输入引擎能力
- **UI 库 `:ime-ui`**：基于 Compose 的缺省 UI 实现，对第三方应用开放，可作为即插即用的输入界面使用，也可被自定义 UI 替换
- **应用模块 `:app`**：系统 IME 服务壳、设置页面、配置持久化，是库的官方消费者

库的核心价值在于：任何 Android 应用都可以通过引入 `:ime-engine` + `:ime-ui`，获得筷字输入法的完整输入能力和缺省 UI——拼音滑行、X-Pad 连续输入、候选选择、输入列表管理、撤销重做等——而无需依赖系统 IME 服务。仅需要引擎逻辑的场景（如文本预处理、自动化测试）可以只引入 `:ime-engine` 而不引入 UI 库。需要完全自定义 UI 的场景可以只引入 `:ime-engine` 并自行实现视图层。

---

## 2. Java 版本库模式分析

### 2.1 现有架构

Java 版本没有独立的库模块，整个 IME（引擎 + 视图 + 字典）在 `:app` 模块中。嵌入输入法有两种途径：

**途径 A：系统 IME 模式（IMEService）**

```
IMEService (InputMethodService)
  → 创建 IMEConfig + IMEditor + IMEditorView
  → 充当消息中介：UserMsg → IMEditor, InputMsg → IMEditorView
  → InputMsg 也在 IMEService 中处理：通过 InputConnection.commitText() 提交文本
```

**途径 B：应用内嵌模式（ImeIntegratedActivity）**

```
ImeIntegratedActivity (Activity)
  → 创建 IMEConfig + IMEditor + IMEditorView
  → 同样的消息中介模式，但 InputMsg → ImeSupportEditText
  → 用于应用内的练习/引导场景
```

### 2.2 ImeSupportEditText 能力

`ImeSupportEditText` 是"被动"接收者，实现 `InputMsgListener` 接口：

| 能力 | InputMsgType | 说明 |
|------|-------------|------|
| 提交输入文本 | `InputList_Commit_Doing` | 将输入列表的文本插入到 EditText |
| 撤销提交 | `InputList_Committed_Revoke_Doing` | 撤销上次提交，恢复光标和文本 |
| 配对符号 | `InputList_PairSymbol_Commit_Doing` | 插入左右配对符号并将光标置于中间 |
| 粘贴收藏 | `InputFavorite_Text_Commit_Doing` | 插入收藏的文本 |
| 粘贴剪贴板 | `InputClip_Text_Commit_Doing` | 插入剪贴板文本 |
| 移动光标 | `Editor_Cursor_Move_Doing` | 通过 DPAD 按键事件移动光标 |
| 选择文本 | `Editor_Range_Select_Doing` | 通过 Shift+DPAD 选择文本范围 |
| 编辑操作 | `Editor_Edit_Doing` | 退格、全选、复制、粘贴、剪切、撤销、重做 |

### 2.3 问题分析

| 问题 | 说明 |
|------|------|
| **无独立库模块** | 整个 IME 在 `:app` 中，无法作为依赖被其他项目引入 |
| **ImeIntegratedActivity 耦合 Activity** | 嵌入输入法必须继承特定 Activity，不够灵活；无法在 Fragment、Dialog 或自定义 View 中使用 |
| **引擎与视图不分离** | `IMEditorView` 直接引用 `IMEditor`，无法仅使用引擎而不引入视图层 |
| **视图不可复用** | 视图层与 IMEService 耦合，第三方应用无法直接复用 Compose UI 组件 |
| **配置硬编码 SharedPreferences** | 库的使用者无法通过代码设置配置，必须依赖 SharedPreferences |
| **数据库不可替换** | `IMEditorDict` 是单例，使用固定路径的 SQLite，外部无法替换为其他存储实现 |
| **功能不可裁剪** | 收藏和剪贴板功能与引擎深度绑定，无法按需禁用 |

---

## 3. v4 三层库架构

### 3.1 模块划分

```
kuaizi-ime-android/
├── code/
│   ├── app/                     ← 应用模块（系统 IME 服务 + 设置 + 日志诊断界面）
│   │   ├── build.gradle.kts     ← implementation(":ime-engine"), implementation(":ime-ui")
│   │   └── src/main/
│   │       └── org/crazydan/studio/app/ime/kuaizi/   ← 直接使用顶级包名，不加子模块名
│   │           ├── IMEService.kt
│   │           ├── InputConnectionBridge.kt
│   │           ├── ConfigRepository.kt
│   │           ├── settings/     ← 设置页面（SettingsScreen, LogLevelSetting, LogStoragePathSetting）
│   │           ├── logging/      ← 日志界面（LogViewerScreen, LogExportScreen, LogViewerViewModel）
│   │           └── guide/       ← 引导页面
│   │
│   ├── ime-ui/                  ← UI 库模块（Compose 缺省 UI）
│   │   ├── build.gradle.kts     ← android.library
│   │   └── src/main/
│   │       └── org/crazydan/studio/app/ime/kuaizi/ui/
│   │           ├── theme/       ← 主题系统（KeyboardColors, KeyboardThemes, KeyboardTheme）
│   │           ├── panel/       ← 三层面板（GestureInputPanel, KeyGridPanel, GestureFeedbackPanel）
│   │           ├── keyboard/    ← 键盘组件（StandardKeyGridPanel, EmojiKeyGridPanel, XPadView）
│   │           ├── candidate/   ← 候选栏（CandidateListPanel, CandidateItem）
│   │           ├── input/       ← 输入栏（InputListPanel, CharInputItem, GapInputItem）
│   │           ├── editor/      ← 编辑器桥接（EditTextBridge）
│   │           ├── clipboard/   ← 剪贴板 UI（ClipTipPopup）
│   │           ├── favorite/    ← 收藏 UI（FavoriteListPanel）
│   │           ├── practice/    ← 输入练习 UI（FingerOverlay, ActionPlayerPanel）
│   │           └── integration/ ← 集成组件（KeyboardPanel）
│   │
│   └── ime-engine/              ← 引擎库模块（纯 Kotlin）
│       ├── build.gradle.kts     ← android.library
│       └── src/main/
│           └── org/crazydan/studio/app/ime/kuaizi/engine/
│               ├── api/         ← 公开 API（ImeEngine, ImeOutput, ImeConfig）
│               ├── bridge/      ← 输出桥接（ImeOutputBridge, BaseImeOutputBridge）
│               ├── domain/      ← 领域层（Keyboard, InputList, Inputboard）
│               ├── dict/        ← 字典系统（接口 + 内置实现）
│               ├── input/       ← 输入类型（InputKey, InputWord, InputGesture）
│               ├── logging/     ← 日志基础设施（ImeLog, ImeLogger, LogLevel, LogEntry, LogWriter）
│               └── state/       ← 状态定义（KeyboardState, ImeState）
│
├── docs/
└── ...
```

### 3.2 模块职责与依赖

| 模块 | 职责 | 依赖 |
|------|------|------|
| `:ime-engine` | IME 核心引擎，纯 Kotlin，不依赖 Android 框架（字典 I/O 除外） | Kotlin 标准库 + 协程 |
| `:ime-ui` | Compose 缺省 UI，包含完整的输入法界面组件 | `:ime-engine` + Compose + Material3 |
| `:app` | 系统 IME 服务、配置持久化（DataStore）、设置页面 | `:ime-engine` + `:ime-ui` + DataStore + Lifecycle |

**依赖关系图**：

```
┌──────────┐
│  :app    │ 应用模块（IME 服务 + 设置 + 配置持久化）
└────┬─────┘
     │ depends on
     ├──────────────────────┐
     ▼                      ▼
┌──────────┐          ┌──────────┐
│ :ime-ui  │          │:ime-engine│
│ Compose  │          │ 纯 Kotlin │
│ 缺省 UI  │          │ 核心引擎  │
└────┬─────┘          └───────────┘
     │ depends on
     ▼
┌──────────┐
│:ime-engine│
└───────────┘
```

### 3.3 设计原则

1. **引擎与 UI 完全分离**：`:ime-engine` 不包含任何 Compose 代码或 Android View，只暴露状态流和意图接口
2. **UI 库作为缺省实现对外开放**：`:ime-ui` 提供完整的 Compose UI 组件，第三方应用可直接使用，也可替换为自定义 UI
3. **统一配置**：库不内置配置持久化，所有配置通过 `ImeConfig`（含引擎配置和 UI 配置的明确隔离）在创建时或运行时设置
4. **数据库层可替换**：字典接口与实现分离，内置 SQLite 实现，外部可提供自己的 `ImeDictProvider`
5. **功能可裁剪**：收藏和剪贴板等可选功能通过 `Feature` 标记按需启用/禁用
6. **Fail Fast**：非法操作（如禁用收藏后调用收藏功能）立即抛出异常而非静默忽略

### 3.4 命名规范

v4 三层库架构采用分层命名规范，确保通过类名即可识别其所在模块和职能：

| 模块 | 命名规则 | 示例 |
|------|----------|------|
| `:ime-engine` | 公开 class 以 `Ime` 为前缀 | `ImeEngine`, `ImeConfig`, `ImeOutput`, `ImeState`, `ImeIntent` |
| `:ime-ui` | 不使用 `Ime` 前缀，贴近 UI 业务命名 | `KeyboardPanel`, `EditTextBridge`, `GestureFeedbackPanel`, `CandidateListPanel` |
| `:app` | 不使用 `Ime` 前缀，贴近应用业务命名 | `IMEService`, `ConfigRepository`, `InputConnectionBridge` |

**命名约定**：

1. **engine 模块**：所有对外的 class（包括 sealed class、data class、enum、interface）均以 `Ime` 作为前缀，作为引擎库的命名空间标识。内部实现类（如 `KeyboardStateMachine`、`InputListOperator`、`FeatureRegistry`）不强制使用 `Ime` 前缀，因为它们不对外暴露
2. **ui 模块**：组件命名贴近其 UI 职能，使用业务语义化的名称：面板类以 `Panel` 为后缀（如 `GestureInputPanel`、`KeyGridPanel`、`CandidateListPanel`、`GestureFeedbackPanel`）、视图类以 `View` 为后缀（如 `XPadView`）、桥梁类以 `Bridge` 为后缀（如 `EditTextBridge`）。主题系统使用 `Keyboard` 前缀（如 `KeyboardColors`、`KeyboardTheme`），与 Material3 的 `MaterialTheme` 等系统命名区分
3. **app 模块**：Android 系统服务类沿用平台命名惯例（如 `IMEService`），配置类使用职能名称（如 `ConfigRepository`），桥接类使用目标对象命名（如 `InputConnectionBridge`）。页面以 `Screen` 为后缀（如 `SettingsScreen`、`MainScreen`）

---

## 4. 引擎库公开 API

### 4.1 ImeEngine

`ImeEngine` 是引擎库的核心入口点，对应 Java 版本的 `IMEditor` 但接口更清晰：

```kotlin
/**
 * IME 引擎，库的核心入口点。
 *
 * 提供完整的输入法能力：拼音输入、滑行输入、X-Pad 连续输入、
 * 候选选择、输入列表管理、撤销重做等。
 *
 * 引擎不依赖任何 UI 框架，通过 StateFlow 暴露状态，
 * 通过 Intent 接收用户操作，通过 Output 输出编辑指令。
 *
 * 输出分发方式：
 * - 主路径：通过 ImeOutputBridge 桥接模式，引擎内部统一 when 分发，
 *   桥梁实现者只需实现语义方法，无需理解 ImeOutput 类型体系
 * - 备用路径：通过 output: ReceiveChannel<ImeOutput> 通道，供高级场景使用
 *
 * 使用方式：
 * ```kotlin
 * val engine = ImeEngine.create(
 *     config = ImeConfig(
 *         engine = ImeConfig.EngineConfig(
 *             keyboardType = KeyboardType.Pinyin,
 *             handMode = HandMode.Right,
 *             features = setOf(Feature.Clipboard, Feature.Favorites),
 *         ),
 *     ),
 *     dictProvider = ImeSqliteDictProvider(context),
 * )
 *
 * // 接入桥接
 * val bridge = InputConnectionBridge { currentInputConnection }
 * engine.attachOutputBridge(bridge)
 *
 * // 订阅状态
 * engine.state.collect { state -> updateUI(state) }
 *
 * // 发送用户操作
 * engine.handleGesture(InputGesture.Tap(timestamp, key))
 * ```
 */
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

    /**
     * 备用输出通道，供高级场景使用。
     * 主路径应通过 ImeOutputBridge 桥接模式。
     */
    private val _output = MutableChannel<ImeOutput>()
    val output: ReceiveChannel<ImeOutput> = _output

    /**
     * 接入输出桥梁。
     * 引擎将通过桥梁将 ImeOutput 语义化分派到目标编辑器。
     */
    fun attachOutputBridge(bridge: ImeOutputBridge) {
        _outputBridge = bridge
    }

    /** 断开输出桥梁 */
    fun detachOutputBridge() {
        _outputBridge = null
    }

    /**
     * 将 ImeOutput 分发到桥梁。
     * 引擎内部唯一的 when 分发点，桥梁实现者无需理解 ImeOutput 类型体系。
     */
    private fun dispatchToTarget(output: ImeOutput) {
        val bridge = _outputBridge ?: return
        when (output) {
            is ImeOutput.CommitText -> bridge.commitText(output.text, output.replacements)
            is ImeOutput.RevokeCommit -> bridge.revokeCommit()
            is ImeOutput.InsertPairedSymbols -> bridge.insertPairedSymbols(output.left, output.right)
            is ImeOutput.MoveCursor -> bridge.moveCursor(output.direction)
            is ImeOutput.SelectRange -> bridge.selectRange(output.direction)
            is ImeOutput.PerformEdit -> bridge.performAction(output.action)
        }
    }

    /**
     * 处理输入手势。
     *
     * 将 InputGesture 转换为 ImeIntent 并执行 reduce，
     * 更新状态和输出。
     */
    fun handleGesture(gesture: InputGesture) {
        val intent = gestureToIntent(gesture)
        handleIntent(intent)
    }

    /**
     * 处理意图。
     *
     * 直接发送 ImeIntent 到引擎，适用于程序化操作
     * （如键盘切换、候选选择等非手势操作）。
     */
    fun handleIntent(intent: ImeIntent) {
        val result = reduce(_state.value, intent)
        _state.update { result.state }
        val output = result.output
        if (output != null) {
            dispatchToTarget(output)
        }
    }

    /**
     * 更新运行时配置。
     *
     * 应用配置变更并触发必要的重组。
     * 运行时修改始终优先于持久化配置，直到应用重启：
     * 已被运行时覆盖的字段（记录在 config.runtimeOverrides 中）
     * 不会被持久化同步覆盖。应用重启时，ImeConfig 根据持久化配置重新初始化。
     */
    fun updateConfig(block: (ImeConfig) -> ImeConfig) {
        config = block(config)
        // 触发状态更新以通知 UI
    }

    companion object {
        /**
         * 创建引擎实例。
         *
         * @param config 引擎配置
         * @param dictProvider 字典提供者（内置 SQLite 或外部实现）
         */
        fun create(
            config: ImeConfig = ImeConfig(),
            dictProvider: ImeDictProvider,
        ): ImeEngine {
            return ImeEngine(
                config = config,
                dictProvider = dictProvider,
                stateMachine = KeyboardStateMachine(),
                inputListOp = InputListOperator(),
                featureRegistry = FeatureRegistry(config.features),
            )
        }
    }
}
```

### 4.2 ImeConfig

`ImeConfig` 是统一的运行时配置，同时包含引擎配置和 UI 配置，二者在数据结构上明确隔离，以方便第三方按需使用。引擎配置（`engine`）影响引擎的核心行为，UI 配置（`ui`）影响界面呈现和交互反馈。库不内置配置持久化，所有配置通过 `ImeConfig` 在创建时或运行时设置，持久化是应用层的职责（如 `:app` 模块使用 DataStore）。

> **设计决策**：`ImeConfig` 合并了原 `ImeEngineConfig`（引擎配置）与 `:app` 模块的 `Config`（应用配置，文档 500）的职责，消除两套配置之间的字段重叠和同步问题。`ImeConfig` 作为引擎的运行时配置状态，可在 `:app` 模块中动态修改以直接影响引擎状态，UI 侧（含 `:ime-ui` 库）通过 `ImeState.config` 自动同步更新。在 `:app` 模块的配置界面上的操作需同时做配置持久化和对 `ImeConfig` 的更新，但需确保运行时的修改优先——`ImeConfig` 在运行时的修改始终优先于应用侧配置，直到应用重启。重启时，`ImeConfig` 根据持久化配置进行初始化。`ImeConfig.runtimeOverrides` 记录被运行时临时修改的字段，持久化同步时跳过这些字段。

```kotlin
/**
 * IME 统一配置。
 *
 * 同时包含引擎配置和 UI 配置，二者明确隔离。
 * 库不内置配置持久化，所有配置通过此 data class 在创建时或运行时设置。
 * 持久化是应用层的职责（如 :app 模块使用 DataStore）。
 *
 * 运行时修改优先规则：通过键盘 UI 进行的临时修改（如临时切换左右手模式）
 * 始终优先于应用侧配置，直到应用重启。重启时，ImeConfig 根据持久化配置进行初始化。
 * ImeConfig.runtimeOverrides 记录被运行时覆盖的字段，持久化同步时跳过这些字段。
 */
data class ImeConfig(
    /** 引擎配置（影响核心输入行为） */
    val engine: EngineConfig = EngineConfig(),
    /** UI 配置（影响界面呈现和交互反馈） */
    val ui: UiConfig = UiConfig(),
    /** 运行时覆盖标记：记录哪些字段已被运行时临时修改，持久化同步时应跳过 */
    val runtimeOverrides: Set<ConfigField> = emptySet(),
) {
    /**
     * 引擎配置。
     *
     * 影响引擎核心行为的配置项，与 UI 呈现无关。
     * 仅使用 :ime-engine 的第三方应用主要关注此部分。
     */
    data class EngineConfig(
        /** 默认键盘类型 */
        val keyboardType: KeyboardType = KeyboardType.Pinyin,
        /** 手模式 */
        val handMode: HandMode = HandMode.Right,
        /** 启用的功能集 */
        val features: Set<Feature> = Feature.DefaultSet,
        /** 候选预测是否启用 */
        val candidatePredictionEnabled: Boolean = true,
        /** 单行输入模式 */
        val singleLineInput: Boolean = false,
    )

    /**
     * UI 配置。
     *
     * 影响界面呈现和交互反馈的配置项。
     * :ime-ui 库和 :app 模块使用此部分。
     */
    data class UiConfig(
        /** 主题模式 */
        val themeType: ThemeType = ThemeType.FollowSystem,
        /** X-Pad 是否启用 */
        val xPadEnabled: Boolean = true,
        /** 音频反馈是否启用 */
        val audioFeedbackEnabled: Boolean = true,
        /** 触觉反馈是否启用 */
        val hapticFeedbackEnabled: Boolean = true,
        /** 按键动画是否启用 */
        val keyAnimationEnabled: Boolean = true,
        /** 按键放大提示是否启用 */
        val keyPopupTipsEnabled: Boolean = true,
        /** 滑行轨迹是否启用 */
        val gestureSlippingTrailEnabled: Boolean = true,
        /** 剪贴板粘贴提示是否启用 */
        val clipPopupTipsEnabled: Boolean = true,
        /** 剪贴板提示超时（秒） */
        val clipPopupTipsTimeout: Int = 15,
        /** 是否适配桌面滑动手势 */
        val adaptDesktopSwipeUpGesture: Boolean = false,
        /** 候选字繁体是否优先 */
        val candidateVariantFirstEnabled: Boolean = false,
        /** 拉丁键盘是否复用拼音 X-Pad 布局 */
        val latinUsePinyinKeysInXPadEnabled: Boolean = false,
        /** 用户输入数据记录是否启用 */
        val userInputDataEnabled: Boolean = true,
        /** 翻页音效是否启用 */
        val candidatesPagingAudioEnabled: Boolean = true,
        /** 输入练习演示的播放速度倍率 */
        val practicePlaybackSpeed: Float = 1.0f,
        /** 输入练习演示中是否显示手指指示器 */
        val practiceShowFingerOverlay: Boolean = true,
        /** 输入练习演示中是否显示滑行轨迹 */
        val practiceShowSwipeTrail: Boolean = true,
        /** 发布版本的日志等级（仅 release 生效，debug 始终 VERBOSE） */
        val logLevel: LogLevel = LogLevel.WARN,
        /** 日志文件存放目录路径（null 表示使用缺省应用私有目录） */
        val logStoragePath: String? = null,
    )
}

/** 运行时可覆盖的配置字段标识 */
enum class ConfigField {
    HandMode,
    // 其他可运行时覆盖的字段按需添加
}

enum class ThemeType { Light, Night, FollowSystem }
enum class HandMode { Left, Right }

/**
 * 可选功能标记。
 *
 * 收藏和剪贴板等可选功能通过此枚举按需启用/禁用。
 * 禁用后，引擎不会创建相关组件，相关 Intent 将抛出异常。
 */
enum class Feature {
    /** 剪贴板监听和粘贴 */
    Clipboard,
    /** 收藏管理 */
    Favorites,
    /** 输入练习演示（程序化输入动画） */
    InputPractice,
    /** 候选预测（HMM + Viterbi） */
    CandidatePrediction,
    ;

    companion object {
        /** 默认启用的功能集（不含 InputPractice） */
        val DefaultSet: Set<Feature> = setOf(
            Clipboard,
            Favorites,
            CandidatePrediction,
        )
    }
}
```

### 4.3 ImeOutput

引擎的编辑输出，对应 Java 版本中 `InputMsg` 的编辑指令部分。`ImeOutput` 由引擎内部的 `dispatchToTarget()` 统一分发到 `ImeOutputBridge`，桥梁实现者无需理解 `ImeOutput` 类型体系：

```kotlin
sealed class ImeOutput {
    abstract val timestamp: Long

    /**
     * 提交文本。
     * 若携带 replacements 列表，桥梁需检查光标前文本是否在列表中，
     * 若匹配则替换光标前字符，否则正常插入。
     * 对应 Java: InputList_Commit_Doing
     */
    data class CommitText(
        override val timestamp: Long,
        val text: String,
        /** 可轮换替换字符列表，用于直接输入模式下替换光标前字符，null 表示不可替换 */
        val replacements: List<String>? = null,
    ) : ImeOutput()

    /**
     * 撤销上次提交。
     * 由目标桥梁根据其当前状态 + 已记录的选区快照执行撤销。
     * 对应 Java: InputList_Committed_Revoke_Doing
     */
    data class RevokeCommit(
        override val timestamp: Long,
    ) : ImeOutput()

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

enum class CursorDirection { Left, Right, Up, Down }

enum class EditorAction {
    SELECT_ALL, FAVORITE, COPY,       // 无编辑副作用 → 不影响撤销状态
    BACKSPACE, PASTE, CUT, UNDO, REDO; // 有编辑副作用 → 清空撤销状态

    /** 是否有编辑副作用（修改编辑器内容） */
    val hasEditorEffect: Boolean
        get() = when (this) {
            SELECT_ALL, FAVORITE, COPY -> false
            else -> true
        }
}
```

### 4.3b ImeOutputBridge 接口

```kotlin
/**
 * ImeEngine 与目标编辑器之间的桥梁。
 * 引擎通过此桥梁将 ImeOutput 语义化分派到具体编辑器，
 * 桥梁实现负责将语义操作翻译为平台特定 API 调用。
 *
 * 设计原则：
 * - 接口方法表达「做什么」（语义），不规定「怎么做」（实现）
 * - 实现者按自身能力决定操作粒度
 * - 所有方法在主线程调用
 */
interface ImeOutputBridge {

    /**
     * 提交文本到当前光标位置。
     * 若 replacements 非空，桥梁需检查光标前文本是否在列表中：
     * - 匹配：替换光标前字符（替换轮换）
     * - 不匹配：正常插入文本
     */
    fun commitText(text: String, replacements: List<String>? = null)

    /** 撤销最近一次可撤回输入 */
    fun revokeCommit()

    /** 插入成对符号 */
    fun insertPairedSymbols(left: String, right: String)

    /** 移动光标 */
    fun moveCursor(direction: CursorDirection)

    /** 扩展选区 */
    fun selectRange(direction: CursorDirection)

    /** 执行编辑动作（全选、复制、剪切、粘贴等） */
    fun performAction(action: EditorAction)

    /** 实时获取目标当前文本 */
    fun getText(): CharSequence

    /** 实时获取目标当前选区 */
    fun getSelection(): TextRange
}
```

### 4.3c BaseImeOutputBridge 抽象类

```kotlin
/**
 * ImeOutputBridge 的基础抽象实现。
 *
 * 撤销机制（参照 Java 版 editorChangeRevertion 逻辑）：
 * - 仅支持撤销最近一次可撤回输入，不使用栈
 * - 记录该次输入前后的选区快照（起止点 + 选区内容）
 * - 新的可撤回输入覆盖旧快照（只保留最近一次）
 * - revokeCommit() 消费快照后清空，不可重复撤销
 *
 * 快照重置时机：
 * - 新的可撤回输入开始前 → 先清空旧快照，提交后记录新快照
 * - 撤销完成后 → 清空已消费的快照
 * - 成对符号提交 → 清空快照（不可撤回）
 * - 有编辑副作用的动作（退格、粘贴、剪切、撤销、重做）→ 清空快照
 * - 无编辑副作用的动作（全选、收藏、复制）→ 不影响快照
 *
 * 子类职责：
 * - 在各接口方法中按上述规则调用 resetRevertion() / recordRevertion()
 * - 实现 onRevokeCommit() 执行实际的撤销恢复
 */
abstract class BaseImeOutputBridge : ImeOutputBridge {

    private data class SelectionSnapshot(
        val beforeStart: Int,
        val beforeEnd: Int,
        val beforeContent: String,
        val afterStart: Int,
        val afterEnd: Int
    )

    /** 当前可撤销的选区快照，null 表示无可撤销输入 */
    private var revertion: SelectionSnapshot? = null

    /** 子类调用：清空撤销快照（撤销状态重置） */
    protected fun resetRevertion() {
        revertion = null
    }

    /** 子类调用：记录可撤回输入的快照 */
    protected fun recordRevertion(
        beforeStart: Int, beforeEnd: Int, beforeContent: String,
        afterStart: Int, afterEnd: Int
    ) {
        revertion = SelectionSnapshot(
            beforeStart = beforeStart,
            beforeEnd = beforeEnd,
            beforeContent = beforeContent,
            afterStart = afterStart,
            afterEnd = afterEnd
        )
    }

    override fun revokeCommit() {
        val snapshot = revertion ?: return
        revertion = null  // 消费后立即清空
        onRevokeCommit(snapshot)
    }

    /** 子类实现：根据快照恢复文本与选区 */
    protected abstract fun onRevokeCommit(snapshot: SelectionSnapshot)

    override fun commitText(text: String, replacements: List<String>?) {
        resetRevertion()
        if (replacements != null && replacements.isNotEmpty()) {
            doReplaceableCommitText(text, replacements)
        } else {
            doNormalCommitText(text)
        }
    }

    /** 子类实现：可替换的文本提交 */
    protected abstract fun doReplaceableCommitText(text: String, replacements: List<String>)

    /** 子类实现：普通文本提交（无替换） */
    protected abstract fun doNormalCommitText(text: String)
}
```

### 4.4 ImeIntent

用户意图的 sealed class 表达，替代 Java 版本的三套消息体系（UserKeyMsg 7 种、UserInputMsg 11 种、InputMsg 35+ 种）。v4 将所有用户操作统一为 `ImeIntent`，由 `ImeEngine.handleIntent()` 或 `ImeEngine.handleGesture()` 接收并处理。

`InputGesture`（文档 150）是输入面板的输出，`ImeIntent` 是 ViewModel/引擎的输入。ViewModel 将 `InputGesture` 转换为 `ImeIntent`（转换逻辑见文档 150 第 4.2 节），这种两层转换使得同一手势可以根据当前键盘状态产生不同的 Intent，也使得不同手势可以产生相同的 Intent。

```kotlin
/**
 * IME 用户意图。
 *
 * 所有用户操作统一表达为 ImeIntent，
 * 由 ImeEngine.handleIntent() 接收并处理。
 *
 * ImeIntent 与 InputGesture（文档 150）的区别：
 * - InputGesture 表达「用户做了什么手势」，属于输入面板的领域
 * - ImeIntent 表达「系统应该做什么」，属于引擎的领域
 *
 * 引擎的 reduce 函数根据当前 ImeState 和 ImeIntent 计算新的 ImeState，
 * 必要时产生 ImeOutput。
 */
sealed class ImeIntent {

    // --- 按键意图（由 InputGesture 转换而来） ---

    /**
     * 按按键（含点击、长按、滑行、翻转）。
     *
     * @param key 目标按键
     * @param gesture 手势类型（Tap/LongPress/Swipe/Flip）
     */
    data class PressKey(
        val key: InputKey,
        val gesture: KeyGesture,
    ) : ImeIntent()

    /**
     * 长按按键。
     *
     * @param key 目标按键
     */
    data class LongPressKey(
        val key: InputKey,
    ) : ImeIntent()

    // --- 候选意图 ---

    /**
     * 选择候选项。
     *
     * @param candidate 被选中的候选字词
     */
    data class SelectCandidate(
        val candidate: InputWord,
    ) : ImeIntent()

    /** 翻页候选 */
    data class PageCandidate(
        val direction: PageDirection,
    ) : ImeIntent()

    // --- 键盘切换 ---

    /**
     * 切换键盘类型。
     *
     * @param type 目标键盘类型
     */
    data class SwitchKeyboard(
        val type: KeyboardType,
    ) : ImeIntent()

    // --- 输入列表意图 ---

    /** 提交当前输入 */
    data object CommitInput : ImeIntent()

    /** 删除输入 */
    data object DeleteInput : ImeIntent()

    /** 清空输入 */
    data object CleanInput : ImeIntent()

    /**
     * 移动光标到指定位置。
     *
     * 由 InputListPanel 中的间隙点击触发。
     *
     * @param index 目标位置索引
     */
    data class MoveCursorTo(
        val index: Int,
    ) : ImeIntent()

    // --- 编辑操作意图 ---

    /**
     * 执行编辑操作（退格、全选、复制、粘贴、剪切、撤销、重做）。
     *
     * Intent 和 Output 对称使用同一 EditorAction 枚举：
     * - ImeIntent.PerformEdit(EditorAction.BACKSPACE) → 引擎处理删除
     * - ImeOutput.PerformEdit(action = EditorAction.BACKSPACE) → 桥接到编辑器执行删除
     *
     * @param action 编辑操作类型
     */
    data class PerformEdit(
        val action: EditorAction,
    ) : ImeIntent()

    // --- X-Pad 意图（扩展，见文档 150/700） ---

    /**
     * 选择 X-Pad 路径。
     *
     * @param startZone 起始区域
     * @param path 途经区域序列
     */
    data class SelectXPadPath(
        val startZone: XPadZone,
        val path: List<XPadZone>,
    ) : ImeIntent()

    // --- 剪贴板与收藏意图（扩展，见文档 600） ---

    /** 粘贴剪贴板 */
    data class PasteClip(
        val text: String,
    ) : ImeIntent()

    /** 保存收藏 */
    data class SaveFavorite(
        val favorite: InputFavorite,
    ) : ImeIntent()

    // --- 配置意图 ---

    /**
     * 更新配置。
     *
     * 运行时配置修改通过此 Intent 驱动，
     * 引擎更新 ImeConfig 后，UI 通过 ImeState.config 自动同步。
     * 运行时修改优先于持久化配置（详见第 4.2 节）。
     *
     * @param config 新的配置
     */
    data class UpdateConfig(
        val config: ImeConfig,
    ) : ImeIntent()

    // --- 数据导入导出意图（扩展，见文档 800） ---

    /** 导出用户数据 */
    data object ExportUserData : ImeIntent()

    /** 导入用户数据 */
    data class ImportUserData(
        val filePath: String,
    ) : ImeIntent()
}

/** 按键手势类型 */
sealed class KeyGesture {
    data object Tap : KeyGesture()
    data object LongPress : KeyGesture()
    data object Swipe : KeyGesture()
    data class Flip(val direction: FlipDirection) : KeyGesture()
}

enum class PageDirection { Next, Previous }
```

### 4.5 ImeState 子状态类型

`ImeState`（第 8.1 节）中引用的子状态类型定义如下。这些类型与 `ImeState` 一样是 `data class`，不可变，通过 `copy()` 模式创建新实例。

```kotlin
/**
 * 输入列表状态。
 *
 * 描述输入栏中的字符序列和光标位置。
 * 完整的输入列表重构设计见文档 200。
 */
data class InputListState(
    /** 输入项列表（字符项和间隙项交替排列） */
    val inputs: List<InputItem> = emptyList(),
    /** 光标所在的间隙索引 */
    val gapIndex: Int = 0,
    /** 待确认的拼音串（如 "nihao"） */
    val pendingSpell: String = "",
)

/**
 * 输入项。
 *
 * 输入栏中的单个项，分为字符项和间隙项。
 */
sealed class InputItem {
    abstract val id: String

    /** 字符输入项 */
    data class Char(
        override val id: String,
        val value: String,
        val isCommitted: Boolean = false,
    ) : InputItem()

    /** 间隙项（光标位置） */
    data class Gap(
        override val id: String,
    ) : InputItem()

    /** 数学表达式输入项 */
    data class MathExpr(
        override val id: String,
        val expression: String,
    ) : InputItem()
}

/**
 * 候选状态。
 *
 * 描述候选栏中的候选字词列表和分页信息。
 */
data class CandidateListState(
    /** 候选字词列表 */
    val candidates: List<InputWord> = emptyList(),
    /** 当前页码（从 0 开始） */
    val pageIndex: Int = 0,
    /** 每页数量 */
    val pageSize: Int = 20,
    /** 是否有更多候选 */
    val hasMore: Boolean = false,
)

/**
 * 剪贴板状态。
 *
 * 描述剪贴板的内容和提示信息。
 * 仅在 Feature.Clipboard 启用时使用。
 * 完整的剪贴板设计见文档 600。
 */
data class ClipboardState(
    /** 当前剪贴板文本（null 表示无内容或未监听） */
    val currentText: String? = null,
    /** 是否显示剪贴板提示弹窗 */
    val showTip: Boolean = false,
)

/**
 * 收藏状态。
 *
 * 描述收藏列表的加载和展示信息。
 * 仅在 Feature.Favorites 启用时使用。
 * 完整的收藏设计见文档 600。
 */
data class FavoriteListState(
    /** 收藏列表 */
    val favorites: List<InputFavorite> = emptyList(),
    /** 是否正在加载 */
    val isLoading: Boolean = false,
)
```

---

## 5. UI 库设计

### 5.1 设计目标

UI 库 `:ime-ui` 的核心设计目标是作为**缺省 UI 实现**对第三方应用开放。第三方应用可以直接使用库中的 Compose 组件构建完整的输入法界面，无需自行实现视图层。同时，UI 库的设计遵循「可替换」原则：所有 UI 组件仅依赖 `:ime-engine` 的公开 API（StateFlow、Intent、ImeOutput），不依赖引擎内部实现，因此第三方应用可以完全用自定义 UI 替换 `:ime-ui` 而不影响引擎功能。

**UI 库的定位**：

| 定位 | 说明 |
|------|------|
| **缺省实现** | 提供完整的、即插即用的输入法 UI，第三方应用引入后开箱即用 |
| **可替换** | 所有 UI 组件仅依赖引擎公开 API，第三方应用可自行替换任意组件 |
| **可组合** | 组件粒度合理，第三方应用可选择性使用部分组件（如只用键盘不用候选栏） |
| **可定制** | 通过主题系统（KeyboardColors）和配置参数控制外观和行为 |

### 5.2 UI 库组件清单

UI 库中的组件按层次组织，从底层的原子组件到顶层的集成组件：

**原子组件（由上层组合使用，也可单独使用）**：

| 组件 | 包路径 | 说明 | 对应设计文档 |
|------|--------|------|------------|
| `KeyView` | `keyboard` | 单个按键渲染（纯展示，无触摸） | 150, 400 |
| `CandidateItem` | `candidate` | 单个候选项 | 400 |
| `CharInputItem` / `GapInputItem` | `input` | 输入栏中的字符/间隙项 | 200, 400 |
| `ClipTipPopup` | `clipboard` | 剪贴板提示弹窗 | 600 |
| `FavoriteItem` | `favorite` | 收藏项（含滑动删除） | 600 |
| `FingerOverlay` | `practice` | 手指指示器（程序化输入动画） | 930 |

**面板组件（由原子组件组合而成，可独立使用）**：

| 组件 | 包路径 | 说明 | 对应设计文档 |
|------|--------|------|------------|
| `GestureInputPanel` | `panel` | 透明手势拦截层 | 150 |
| `KeyGridPanel` / `StandardKeyGridPanel` | `panel` | 按键渲染层 | 150 |
| `GestureFeedbackPanel` | `panel` | 透明反馈绘制层 | 150 |
| `CandidateListPanel` | `candidate` | 候选栏 | 400 |
| `InputListPanel` | `input` | 输入栏 | 200, 400 |
| `FavoriteListPanel` | `favorite` | 收藏面板 | 600 |
| `XPadView` | `keyboard` | X-Pad 六边形面板 | 700 |
| `ActionPlayerPanel` | `practice` | 播放控制面板 | 930 |

**集成组件（一站式解决方案）**：

| 组件 | 包路径 | 说明 |
|------|--------|------|
| `KeyboardPanel` | `integration` | 完整键盘组件（三层面板 + 候选栏 + 输入栏 + 工具栏） |

> **注意**：编辑器桥接组件在 `:ime-engine` 的 `api/` 包中定义（ImeOutputBridge、BaseImeOutputBridge），具体实现分别在 `:app`（InputConnectionBridge）和 `:ime-ui`（EditTextBridge）中

**主题系统**：

| 组件 | 包路径 | 说明 |
|------|--------|------|
| `KeyboardColors` | `theme` | 颜色定义（键盘/候选/输入栏/X-Pad） |
| `KeyboardThemes` | `theme` | 预置主题（Light/Night） |
| `KeyboardTheme` | `theme` | 主题 Composable（支持跟随系统） |
| `LocalKeyboardColors` | `theme` | CompositionLocal 提供颜色 |

### 5.3 输出桥接机制

v4 采用 `ImeOutputBridge` 桥接模式替代 `EditorField` 的 `when(ImeOutput)` 直接消费模式。核心改进：引擎内部统一执行 `when(ImeOutput)` 分发（仅一处），桥梁实现者只需实现语义方法，无需理解 `ImeOutput` 类型体系。

**改造前的核心问题**：`EditorField` 和 `InputConnectionBridge` 各自独立实现了完整的 `when(ImeOutput)` 分发，代码完全重复。第三方接入必须理解 `ImeOutput` sealed class。

**改造后的架构**：

```
ImeOutput → ImeEngine.dispatchToTarget() → when分发（仅此一处）
                                                ↓
                                        ImeOutputBridge 接口 [:ime-engine]
                                         ┌─────────┼──────────┐
                                         ↓         ↓          ↓
                               InputConnection  TextEditor  任意第三方
                               Bridge           Bridge      Bridge
                               [:app]           [:ime-ui]   [:第三方]
```

**模块结构变化**：

```
:ime-engine/api/  → +ImeOutputBridge 接口 +BaseImeOutputBridge 抽象类
:ime-ui/          → EditTextBridge（替代 EditorField）
:app/             → InputConnectionBridge 实现 ImeOutputBridge
                    ImeService 不再手动收集 output
```

#### 5.3.1 InputConnectionBridge（系统输入连接）

```kotlin
/**
 * 面向系统输入连接的桥梁实现。
 * 构造时接受 supplier 获取当前 InputConnection。
 */
class InputConnectionBridge(
    private val targetSupplier: () -> InputConnection?
) : BaseImeOutputBridge() {

    /** 记录上次操作的 InputConnection 引用，用于检测变更 */
    private var lastInputConnection: InputConnection? = null

    private fun checkTargetChange() {
        val current = targetSupplier()
        if (current !== lastInputConnection) {
            resetRevertion()
            lastInputConnection = current
        }
    }

    override fun doNormalCommitText(text: String) {
        resetRevertion()
        val ic = targetSupplier() ?: return
        val beforeSel = getSelection()
        val beforeText = getText()
        val beforeStart = beforeSel.start.coerceAtLeast(0)
        val beforeEnd = beforeSel.end.coerceAtLeast(0).coerceAtMost(beforeText.length)
        val beforeContent = beforeText.substring(beforeStart, beforeEnd)
        ic.commitText(text, 1)
        val afterSel = getSelection()
        recordRevertion(
            beforeStart = beforeStart, beforeEnd = beforeEnd, beforeContent = beforeContent,
            afterStart = afterSel.start.coerceAtLeast(0), afterEnd = afterSel.end.coerceAtLeast(0),
        )
    }

    override fun doReplaceableCommitText(text: String, replacements: List<String>) {
        resetRevertion()
        val ic = targetSupplier() ?: return
        val textBeforeCursor = ic.getTextBeforeCursor(text.length, 0)?.toString()
        if (textBeforeCursor != null && replacements.contains(textBeforeCursor)) {
            val replaceStart = getSelection().start - text.length
            val beforeContent = textBeforeCursor
            ic.deleteSurroundingText(text.length, 0)
            ic.commitText(text, 1)
            val afterSel = getSelection()
            recordRevertion(
                beforeStart = replaceStart, beforeEnd = getSelection().start,
                beforeContent = beforeContent,
                afterStart = afterSel.start.coerceAtLeast(0), afterEnd = afterSel.end.coerceAtLeast(0),
            )
        } else {
            doNormalCommitText(text)
        }
    }

    override fun insertPairedSymbols(left: String, right: String) {
        resetRevertion()  // 成对符号不可撤回
        val ic = targetSupplier() ?: return
        val sel = getSelection()
        val currentText = getText()
        if (sel.start != sel.end) {
            val selected = currentText.substring(sel.start, sel.end)
            ic.commitText(left + selected + right, 1)
        } else {
            ic.commitText(left + right, 1)
            ic.setSelection(sel.start + left.length, sel.start + left.length)
        }
    }

    override fun moveCursor(direction: CursorDirection) { /* via InputConnection */ }
    override fun selectRange(direction: CursorDirection) { /* via InputConnection */ }

    override fun performAction(action: EditorAction) {
        if (action.hasEditorEffect) { resetRevertion() }
        val ic = targetSupplier() ?: return
        when (action) {
            EditorAction.BACKSPACE -> { /* sendKeyEvent DEL */ }
            EditorAction.SELECT_ALL -> { ic.performContextMenuAction(android.R.id.selectAll) }
            EditorAction.COPY -> { ic.performContextMenuAction(android.R.id.copy) }
            EditorAction.PASTE -> { ic.performContextMenuAction(android.R.id.paste) }
            EditorAction.CUT -> { ic.performContextMenuAction(android.R.id.cut) }
            EditorAction.UNDO -> { ic.performContextMenuAction(android.R.id.undo) }
            EditorAction.REDO -> { ic.performContextMenuAction(android.R.id.redo) }
            EditorAction.FAVORITE -> { /* no-op for InputConnection */ }
        }
    }

    override fun getText(): CharSequence {
        val ic = targetSupplier() ?: return ""
        val extracted = ic.getExtractedText(ExtractedTextRequest(), 0)
        return extracted?.text ?: ""
    }

    override fun getSelection(): TextRange {
        val ic = targetSupplier() ?: return TextRange(0, 0)
        return TextRange(ic.cursorSelStart, ic.cursorSelEnd)
    }

    override fun onRevokeCommit(snapshot: SelectionSnapshot) {
        val ic = targetSupplier() ?: return
        ic.setSelection(snapshot.beforeStart, snapshot.afterEnd)
        ic.commitText(snapshot.beforeContent, 1)
        ic.setSelection(snapshot.beforeStart, snapshot.beforeEnd)
    }
}
```

#### 5.3.2 EditTextBridge（EditText 类型默认实现）

```kotlin
/**
 * 面向 EditText 类型目标的默认桥梁实现。
 * 构造时接受 supplier 获取当前 EditText 实例。
 */
class EditTextBridge(
    private val targetSupplier: () -> EditText?
) : BaseImeOutputBridge() {

    private var lastEditor: EditText? = null

    private fun checkTargetChange() {
        val current = targetSupplier()
        if (current !== lastEditor) {
            resetRevertion()
            lastEditor = current
        }
    }

    override fun doNormalCommitText(text: String) {
        resetRevertion()
        checkTargetChange()
        val e = targetSupplier() ?: return
        val beforeStart = e.selectionStart
        val beforeEnd = e.selectionEnd
        val beforeContent = e.text.substring(beforeStart, beforeEnd)
        e.text.replace(beforeStart, beforeEnd, text)
        recordRevertion(
            beforeStart = beforeStart, beforeEnd = beforeEnd, beforeContent = beforeContent,
            afterStart = e.selectionStart, afterEnd = e.selectionEnd,
        )
    }

    override fun doReplaceableCommitText(text: String, replacements: List<String>) {
        resetRevertion()
        checkTargetChange()
        val e = targetSupplier() ?: return
        val cursorPos = e.selectionStart
        val textBeforeCursor = if (cursorPos >= text.length) {
            e.text.substring(cursorPos - text.length, cursorPos).toString()
        } else null

        if (textBeforeCursor != null && replacements.contains(textBeforeCursor)) {
            val replaceStart = cursorPos - text.length
            val beforeContent = e.text.substring(replaceStart, cursorPos).toString()
            e.text.replace(replaceStart, cursorPos, text)
            recordRevertion(
                beforeStart = replaceStart, beforeEnd = cursorPos, beforeContent = beforeContent,
                afterStart = e.selectionStart, afterEnd = e.selectionEnd,
            )
        } else {
            doNormalCommitText(text)
        }
    }

    override fun insertPairedSymbols(left: String, right: String) {
        resetRevertion()
        checkTargetChange()
        val e = targetSupplier() ?: return
        val start = e.selectionStart
        val end = e.selectionEnd
        if (start != end) {
            val selected = e.text.substring(start, end)
            e.text.replace(start, end, left + selected + right)
            e.setSelection(start + left.length, start + left.length + selected.length)
        } else {
            e.text.replace(start, end, left + right)
            e.setSelection(start + left.length, start + left.length)
        }
    }

    override fun moveCursor(direction: CursorDirection) { /* 操作 EditText 选区 */ }
    override fun selectRange(direction: CursorDirection) { /* 操作 EditText 选区 */ }

    override fun performAction(action: EditorAction) {
        if (action.hasEditorEffect) { resetRevertion() }
        val e = targetSupplier() ?: return
        // ... 执行动作
    }

    override fun getText(): CharSequence = targetSupplier()?.text ?: ""
    override fun getSelection(): TextRange {
        val e = targetSupplier() ?: return TextRange(0, 0)
        return TextRange(e.selectionStart, e.selectionEnd)
    }

    override fun onRevokeCommit(snapshot: SelectionSnapshot) {
        val e = targetSupplier() ?: return
        e.text.replace(snapshot.beforeStart, snapshot.afterEnd, snapshot.beforeContent)
        e.setSelection(snapshot.beforeStart, snapshot.beforeEnd)
    }
}
```

#### 5.3.3 类结构总览

```
ImeOutputBridge (接口, :ime-engine)
  │  commitText / revokeCommit / insertPairedSymbols
  │  moveCursor / selectRange / performAction
  │  getText / getSelection
  │
  └── BaseImeOutputBridge (抽象类, :ime-engine)
        │  - revertion: SelectionSnapshot?    （单值，非栈）
        │  - resetRevertion()                 (protected，清空快照)
        │  - recordRevertion(before, after)   (protected，记录快照)
        │  - revokeCommit()                   (消费快照 → onRevokeCommit)
        │  - onRevokeCommit(snapshot)         (protected abstract)
        │  - commitText()                     (默认实现，分发到 doNormal/doReplaceable)
        │  - doNormalCommitText()             (protected abstract)
        │  - doReplaceableCommitText()        (protected abstract)
        │
        ├── InputConnectionBridge (:app)
        │     targetSupplier: () -> InputConnection?
        │     commitText → resetRevertion + recordRevertion
        │     insertPairedSymbols → resetRevertion
        │     performAction(hasEditorEffect) → resetRevertion
        │
        ├── EditTextBridge (:ime-ui)
        │     targetSupplier: () -> EditText?
        │     同上重置规则
        │
        └── [第三方] MarkdownEditorBridge / ...
              自行决定撤销状态重置策略
```

#### 5.3.4 撤销状态重置规则总结

| 触发点 | 方法 | 操作 |
|--------|------|------|
| 新文本提交开始 | `commitText()` | 先 `resetRevertion()`，提交后 `recordRevertion(before, after)` |
| 撤销完成 | `revokeCommit()` | 消费快照后自动置 null |
| 成对符号提交 | `insertPairedSymbols()` | `resetRevertion()` |
| 有副作用的编辑动作 | `performAction(BACKSPACE/PASTE/CUT/UNDO/REDO)` | `resetRevertion()` |
| 无副作用的动作 | `performAction(SELECT_ALL/FAVORITE/COPY)` | 不影响撤销状态 |
| 光标移动 | `moveCursor()` | 不影响撤销状态 |
| 选区扩展 | `selectRange()` | 不影响撤销状态 |

### 5.4 KeyboardPanel

一站式键盘组件，将三层面板、候选栏、输入栏组合为一个完整的输入键盘：

```kotlin
/**
 * 筷字输入法完整键盘组件。
 *
 * 将三层面板（GestureInputPanel / GestureFeedbackPanel / KeyGridPanel）、
 * 候选栏、输入栏、工具栏组合为一个完整的输入法键盘界面。
 *
 * 第三方应用可直接使用此组件获得完整的输入法 UI，
 * 无需手动组合底层组件。
 *
 * @param engine IME 引擎实例
 * @param modifier 修饰符
 * @param layoutMode 布局模式（默认叠加模式）
 * @param showToolbar 是否显示工具栏
 * @param showCandidateListPanel 是否显示候选栏
 * @param showInputListPanel 是否显示输入栏
 */
@Composable
fun KeyboardPanel(
    engine: ImeEngine,
    modifier: Modifier = Modifier,
    layoutMode: LayoutMode = LayoutMode.Overlay,
    showToolbar: Boolean = true,
    showCandidateListPanel: Boolean = true,
    showInputListPanel: Boolean = true,
) {
    val state by engine.state.collectAsStateWithLifecycle()
    val feedbackState = remember { GestureFeedbackState() }
    var keyPanelLayout by remember { mutableStateOf(KeyGridPanelLayoutInfo()) }

    KeyboardTheme(themeType = state.config.ui.themeType) {
        Box(modifier = modifier) {
            when (layoutMode) {
                is LayoutMode.Overlay -> {
                    // 底层：按键面板
                    KeyGridPanel(
                        keyboardType = state.keyboardType,
                        keyGrid = state.keyGrid,
                        keyboardState = state.keyboardState,
                        onLayoutInfoChanged = { keyPanelLayout = it },
                    )

                    // 中层：反馈面板
                    GestureFeedbackPanel(
                        elements = GestureFeedbackPanelSet.OverlaySet.allElements,
                        feedbackState = feedbackState,
                        keyPanelLayout = keyPanelLayout,
                    )

                    // 顶层：输入面板
                    GestureInputPanel(
                        keyPanelLayout = keyPanelLayout,
                        keyboardType = state.keyboardType,
                        feedbackState = feedbackState,
                        onGesture = { engine.handleGesture(it) },
                    )
                }
            }

            // 候选栏
            if (showCandidateListPanel) {
                CandidateListPanel(
                    state = state.candidates,
                    onCandidateSelected = { candidate ->
                        engine.handleIntent(ImeIntent.SelectCandidate(candidate))
                    },
                )
            }

            // 输入栏
            if (showInputListPanel) {
                InputListPanel(
                    state = state.inputList,
                    onGapTapped = { index ->
                        engine.handleIntent(ImeIntent.MoveCursorTo(index))
                    },
                )
            }
        }
    }
}
```

### 5.5 库的接入方式

引擎通过 `ImeOutputBridge` 与目标编辑器对接。接入方式统一：创建 `ImeOutputBridge` 实现 → `engine.attachOutputBridge(bridge)`。两种接入方式：

1. **构造时传入**：适用于目标在引擎生命周期内固定的场景
2. **运行时接入**：`engine.attachOutputBridge(bridge)` / `engine.detachOutputBridge()`，适用于目标动态切换

#### 系统 IME 场景（:app 模块）

```kotlin
class IMEService : InputMethodService() {
    private var currentInputConnection: InputConnection? = null
    private lateinit var engine: ImeEngine
    private lateinit var bridge: InputConnectionBridge

    override fun onCreate() {
        engine = ImeEngine.create(
            config = ImeConfig(),
            dictProvider = ImeSqliteDictProvider(this),
        )
        bridge = InputConnectionBridge { currentInputConnection }
        engine.attachOutputBridge(bridge)
    }

    override fun onStartInput(attribute: EditorInfo, restarting: Boolean) {
        currentInputConnection = currentInputConnection
        // supplier 闭包自动返回新实例，桥梁自动检测变更并清空快照
    }

    override fun onFinishInput() {
        currentInputConnection = null
    }
}
```

#### 应用内嵌场景（使用 EditTextBridge）

```kotlin
@Composable
fun ChatScreen() {
    var currentEditText by remember { mutableStateOf<EditText?>(null) }
    val engine = remember {
        ImeEngine.create(
            config = ImeConfig(),
            dictProvider = ImeSqliteDictProvider(LocalContext.current),
        )
    }
    val bridge = remember { EditTextBridge { currentEditText } }

    LaunchedEffect(engine) {
        engine.attachOutputBridge(bridge)
    }

    Column {
        AndroidView(
            factory = { ctx -> EditText(ctx).also { currentEditText = it } },
            modifier = Modifier.weight(1f),
        )
        KeyboardPanel(
            engine = engine,
            modifier = Modifier.fillMaxWidth().height(280.dp),
        )
    }
}
```

#### 第三方自定义桥梁

```kotlin
class MarkdownEditorBridge(
    private val targetSupplier: () -> MarkdownEditor?
) : BaseImeOutputBridge() {
    override fun doNormalCommitText(text: String) { /* Markdown 特殊处理 */ }
    override fun doReplaceableCommitText(text: String, replacements: List<String>) { /* Markdown 替换 */ }
    override fun onRevokeCommit(snapshot: SelectionSnapshot) { /* Markdown 撤销 */ }
    // ... getText(), getSelection() 等
}

val mdBridge = MarkdownEditorBridge { markdownEditor }
engine.attachOutputBridge(mdBridge)
```

---

## 6. 字典层可替换设计

### 6.1 ImeDictProvider 接口

字典接口与实现分离，内置 SQLite 实现，外部可提供自己的实现：

```kotlin
/**
 * 字典提供者接口。
 *
 * 引擎通过此接口访问字典数据，不关心底层存储实现。
 * 内置 ImeSqliteDictProvider 使用应用内 SQLite 数据库，
 * 外部可实现此接口替换为 Room、SQLDelight、远程 API 或内存字典等。
 */
interface ImeDictProvider {

    /** 拼音字典 */
    val pinyin: PinyinDict

    /** 用户输入数据字典（词频学习） */
    val userInputData: UserInputDataDict

    /** 用户收藏字典（仅 Feature.Favorites 启用时访问） */
    val userInputFavorite: UserInputFavoriteDict

    /**
     * 初始化字典资源。
     *
     * 在引擎创建后调用，用于加载字典数据。
     * 内置实现从 assets/dict/ 加载预构建字典。
     */
    suspend fun initialize()

    /** 释放字典资源 */
    fun close()
}

/**
 * 拼音字典接口。
 */
interface PinyinDict {
    /**
     * 根据拼音查询候选字词。
     *
     * @param spell 拼音字符串，如 "ni"、"hao"
     * @return 匹配的候选字词列表，按优先级排序
     */
    suspend fun queryCandidates(spell: String): List<InputWord>

    /**
     * 查询候选字的拼音路径（用于 X-Pad 模式）。
     *
     * @param text 目标文字
     * @return 对应的拼音序列
     */
    suspend fun querySpellPath(text: String): List<PinyinSegment>
}

/**
 * 用户输入数据字典接口。
 */
interface UserInputDataDict {
    /** 记录用户选择（用于词频学习） */
    suspend fun recordSelection(word: InputWord, context: List<InputWord>)

    /** 查询用户输入历史中的候选 */
    suspend fun queryUserCandidates(spell: String): List<InputWord>
}

/**
 * 用户收藏字典接口。
 */
interface UserInputFavoriteDict {
    /** 查询收藏列表 */
    suspend fun queryFavorites(): List<InputFavorite>

    /** 添加收藏 */
    suspend fun addFavorite(favorite: InputFavorite)

    /** 删除收藏 */
    suspend fun removeFavorite(id: String)

    /** 清空收藏 */
    suspend fun clearFavorites()
}
```

### 6.2 内置实现

```kotlin
/**
 * 基于 SQLite 的字典提供者（内置实现）。
 *
 * 对应 Java 版本的 IMEditorDict，但不再使用单例模式，
 * 而是由引擎使用者显式创建和传入。
 *
 * 字典文件从 assets/dict/ 加载预构建数据库，
 * 用户数据存储在应用内部存储中。
 */
class ImeSqliteDictProvider(
    private val context: Context,
) : ImeDictProvider {

    override lateinit var pinyin: PinyinDict
        private set

    override lateinit var userInputData: UserInputDataDict
        private set

    override lateinit var userInputFavorite: UserInputFavoriteDict
        private set

    private var db: SQLiteDatabase? = null

    override suspend fun initialize() {
        val dbPath = context.getDatabasePath("kuaizi_dict.db")
        if (!dbPath.exists()) {
            context.assets.open("dict/pinyin.db").use { input ->
                dbPath.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        }

        db = SQLiteDatabase.openDatabase(
            dbPath.absolutePath, null,
            SQLiteDatabase.OPEN_READONLY,
        )

        pinyin = SqlitePinyinDict(db!!)
        userInputData = SqliteUserInputDataDict(db!!)
        userInputFavorite = SqliteUserInputFavoriteDict(db!!)
    }

    override fun close() {
        db?.close()
        db = null
    }
}
```

### 6.3 自定义实现示例

```kotlin
/**
 * 基于内存的字典提供者（示例）。
 *
 * 适用于测试环境或不需要持久化的场景。
 */
class ImeInMemoryDictProvider(
    private val pinyinData: Map<String, List<InputWord>> = emptyMap(),
) : ImeDictProvider {

    override val pinyin: PinyinDict = InMemoryPinyinDict(pinyinData)
    override val userInputData: UserInputDataDict = InMemoryUserInputDataDict()
    override val userInputFavorite: UserInputFavoriteDict = InMemoryUserInputFavoriteDict()

    override suspend fun initialize() {
        // 内存实现无需初始化
    }

    override fun close() {
        // 无需释放
    }
}

/**
 * 基于远程 API 的字典提供者（示例）。
 *
 * 适用于需要从服务端获取字典数据的场景。
 */
class ImeRemoteDictProvider(
    private val apiClient: ApiClient,
) : ImeDictProvider {
    // ... 实现略 ...
}
```

---

## 7. 功能可裁剪设计

### 7.1 FeatureRegistry

引擎内部通过 `FeatureRegistry` 查询功能启用状态，禁用的功能不创建相关组件，相关调用抛出异常：

```kotlin
/**
 * 功能注册表。
 *
 * 管理引擎的启用/禁用功能。
 * 禁用的功能不会创建相关组件，相关调用将抛出 IllegalStateException。
 */
class FeatureRegistry(
    private val enabledFeatures: Set<Feature>,
) {
    /** 检查指定功能是否启用 */
    fun isEnabled(feature: Feature): Boolean = feature in enabledFeatures

    /**
     * 断言指定功能已启用。
     *
     * 如果功能未启用，抛出 IllegalStateException。
     * 用于在禁用功能的代码路径上 Fail Fast。
     */
    fun require(feature: Feature) {
        check(isEnabled(feature)) {
            "功能 ${feature.name} 未启用，请在 ImeConfig.engine.features 中添加"
        }
    }
}
```

### 7.2 各功能的裁剪影响

| 功能 | 禁用后的影响 | 引擎内部检查点 | UI 库影响 |
|------|------------|---------------|----------|
| **Clipboard** | 不创建剪贴板监听器；`ImeOutput.CommitText` 不包含剪贴板粘贴；剪贴板提示弹窗不显示 | `Inputboard` 初始化时检查，若禁用则跳过剪贴板组件创建 | `ClipTipPopup` 不渲染；`KeyboardPanel` 隐藏剪贴板相关 UI |
| **Favorites** | 不创建收藏管理器；`ImeOutput.CommitText` 不包含收藏粘贴；收藏面板不显示 | `Favoriteboard` 初始化时检查，若禁用则跳过收藏组件创建 | `FavoriteListPanel` 不渲染；`ClipTipPopup` 隐藏收藏按钮 |
| **InputPractice** | 不创建 `InputActionPlayer`；手指指示器不可用；程序化输入相关 Intent 抛出异常 | `InputActionPlayer` 创建时检查，若禁用则返回 null | `FingerOverlay` 不渲染；`ActionPlayerPanel` 不渲染 |
| **CandidatePrediction** | 不使用 HMM+Viterbi 预测；候选仅按静态频率排序 | `PinyinCandidateEvaluator` 初始化时检查，若禁用则使用简单排序 | 无直接影响，候选排序方式变更 |

### 7.3 使用示例

```kotlin
// 最小配置：仅核心输入能力，无收藏、剪贴板、练习、预测
val minimalEngine = ImeEngine.create(
    config = ImeConfig(
        engine = ImeConfig.EngineConfig(features = emptySet()),
    ),
    dictProvider = ImeSqliteDictProvider(context),
)

// 标准配置：核心输入 + 剪贴板 + 收藏 + 候选预测
val standardEngine = ImeEngine.create(
    config = ImeConfig(
        engine = ImeConfig.EngineConfig(features = Feature.DefaultSet),
    ),
    dictProvider = ImeSqliteDictProvider(context),
)

// 完整配置：全部功能
val fullEngine = ImeEngine.create(
    config = ImeConfig(
        engine = ImeConfig.EngineConfig(features = Feature.entries.toSet()),
    ),
    dictProvider = ImeSqliteDictProvider(context),
)

// 安全输入场景：无剪贴板（防止粘贴泄露），无收藏
val secureEngine = ImeEngine.create(
    config = ImeConfig(
        engine = ImeConfig.EngineConfig(
            features = setOf(Feature.CandidatePrediction),
            singleLineInput = true,
        ),
    ),
    dictProvider = ImeSqliteDictProvider(context),
)
```

---

## 8. 引擎与 UI 的边界

### 8.1 引擎暴露给 UI 的契约

引擎通过 `StateFlow<ImeState>` 暴露状态，UI 层订阅此状态进行渲染。引擎不包含任何 Compose 代码：

```kotlin
/**
 * 引擎暴露给 UI 的完整状态。
 *
 * UI 层（Compose 或 View）订阅此状态进行渲染。
 * 引擎不关心 UI 如何渲染，只保证状态的正确性。
 */
data class ImeState(
    val keyboardType: KeyboardType = KeyboardType.Pinyin,
    val keyboardState: KeyboardState = KeyboardState.Idle,
    val keyGrid: List<List<InputKey>> = emptyList(),
    val inputList: InputListState = InputListState(),
    val candidates: CandidateListState = CandidateListState(),
    val clipboard: ClipboardState = ClipboardState(),
    val favorites: FavoriteListState = FavoriteListState(),
    val config: ImeConfig = ImeConfig(),
)
```

### 8.2 UI 库对引擎的依赖方式

UI 库中的所有组件仅通过以下三个通道与引擎交互，不直接访问引擎内部实现：

| 通道 | 方向 | 说明 |
|------|------|------|
| `engine.state: StateFlow<ImeState>` | 引擎 → UI | 订阅状态进行渲染 |
| `engine.handleGesture(InputGesture)` | UI → 引擎 | 发送用户手势 |
| `engine.handleIntent(ImeIntent)` | UI → 引擎 | 发送用户意图 |
| `engine.attachOutputBridge(ImeOutputBridge)` | UI → 引擎 | 接入输出桥梁 |
| `engine.detachOutputBridge()` | UI → 引擎 | 断开输出桥梁 |
| `engine.output: ReceiveChannel<ImeOutput>` | 引擎 → UI | 保留，供高级场景 |

这种设计确保了 UI 库和引擎库的完全解耦——第三方应用可以用自定义 UI 替换 `:ime-ui` 中的任何组件，只要遵循相同的交互契约即可。

### 8.3 UI 库与 :app 模块的职责划分

| 职责 | `:ime-ui` 库 | `:app` 模块 |
|------|-------------|------------|
| 三层面板（GestureInputPanel / KeyGridPanel / GestureFeedbackPanel） | ✅ | ❌ |
| 候选栏、输入栏 | ✅ | ❌ |
| EditTextBridge | ✅ | ❌ |
| KeyboardPanel | ✅ | ❌ |
| 主题系统（KeyboardColors / KeyboardTheme） | ✅ | ❌ |
| 剪贴板/收藏 UI 组件 | ✅ | ❌ |
| 输入练习 UI（FingerOverlay / ActionPlayerPanel） | ✅ | ❌ |
| IMEService | ❌ | ✅ |
| InputConnectionBridge（实现 ImeOutputBridge） | ❌ | ✅ |
| 配置持久化（DataStore） | ❌ | ✅ |
| 设置页面（SettingsScreen） | ❌ | ✅ |
| 引导页面 | ❌ | ✅ |

### 8.4 KeyboardViewModel 的归属

> **设计决策**：`:app` 模块应视为对 `:ime-engine`、`:ime-ui` 库的使用特例，其地位与第三方应用相同。因此，`:app` 应直接使用 UI 库中的 `KeyboardViewModel`，不继承也不扩展。`:app` 的应用配置应与 `ImeConfig` 耦合，不应与 UI 库的 `KeyboardViewModel` 耦合。配置持久化（DataStore）和 InputConnection 桥接等平台特定职责由 `:app` 中的独立组件承担，而非通过 ViewModel 继承实现。

**UI 库中的 `KeyboardViewModel`**（唯一实现，`:app` 和第三方应用均直接使用）：

```kotlin
/**
 * IME ViewModel，桥接 ImeEngine 和 Compose UI。
 *
 * 在 UI 库中提供，作为引擎与 UI 之间的薄桥接层。
 * :app 模块和第三方应用均直接使用此 ViewModel，
 * 不继承也不扩展。
 *
 * 配置变更通过 ImeConfig 驱动，而非通过 ViewModel 子类化。
 * :app 模块通过 engine.updateConfig() 更新运行时配置，
 * UI 侧通过 ImeState.config 自动同步。
 */
class KeyboardViewModel(
    val engine: ImeEngine,
) : ViewModel() {

    /** 直接暴露引擎状态给 UI */
    val state: StateFlow<ImeState> = engine.state

    /** 处理手势（由 GestureInputPanel 调用） */
    fun handleGesture(gesture: InputGesture) {
        engine.handleGesture(gesture)
    }

    /** 处理意图（由 UI 控件调用） */
    fun handleIntent(intent: ImeIntent) {
        engine.handleIntent(intent)
    }
}
```

**:app 模块如何使用 KeyboardViewModel**（直接使用，不继承）：

```kotlin
/**
 * :app 模块的 IMEService 示例。
 *
 * 直接使用 UI 库的 KeyboardViewModel，不继承。
 * 配置持久化和 InputConnection 桥接由独立组件承担。
 */
class IMEService : InputMethodService() {
    private lateinit var engine: ImeEngine
    private lateinit var viewModel: KeyboardViewModel // 直接使用，不继承
    private lateinit var configRepository: ConfigRepository
    private var currentInputConnection: InputConnection? = null
    private lateinit var bridge: InputConnectionBridge

    override fun onCreate() {
        super.onCreate()
        // 从 DataStore 加载持久化配置创建引擎
        val persistedConfig = runBlocking { configRepository.config.first() }
        engine = ImeEngine.create(
            config = persistedConfig,
            dictProvider = ImeSqliteDictProvider(this),
        )
        viewModel = KeyboardViewModel(engine) // 直接使用 UI 库的 ViewModel
        bridge = InputConnectionBridge { currentInputConnection }
        engine.attachOutputBridge(bridge)

        // 同步持久化配置变更到引擎（尊重运行时覆盖）
        lifecycleScope.launch {
            configRepository.config.collect { persistedConfig ->
                engine.updateConfig { current ->
                    // 仅更新未被运行时覆盖的字段
                    persistedConfig.overrideRespectingRuntime(current)
                }
            }
        }
    }

    override fun onCreateInputView(): View {
        return ComposeView(this).apply {
            setContent { InputScreen(viewModel = viewModel) }
        }
    }

    override fun onStartInput(attribute: EditorInfo, restarting: Boolean) {
        currentInputConnection = currentInputConnection
    }

    override fun onFinishInput() {
        currentInputConnection = null
    }
}
```

这种设计确保了 `:app` 模块与第三方应用在库的使用方式上完全一致，降低了 `:app` 与 `:ime-ui` 之间的耦合，也避免了 Kotlin 类继承带来的构造参数兼容性问题。

### 8.5 引擎与 InputConnection 的桥接

在系统 IME 模式下，`ImeOutput` 通过 `ImeOutputBridge` 桥接机制映射到 `InputConnection` 操作。`InputConnectionBridge` 实现 `ImeOutputBridge` 接口，接受 `targetSupplier: () -> InputConnection?` 延迟获取当前输入连接，桥梁内部自动检测 InputConnection 实例变更并清空撤销快照。完整设计见第 5.3.1 节。

**:app 模块的使用方式**：

```kotlin
class IMEService : InputMethodService() {
    private var currentInputConnection: InputConnection? = null
    private lateinit var engine: ImeEngine
    private lateinit var bridge: InputConnectionBridge

    override fun onCreate() {
        engine = ImeEngine.create(...)
        bridge = InputConnectionBridge { currentInputConnection }
        engine.attachOutputBridge(bridge)
        // 不再需要 onStartInput 中手动收集 output 并桥接！
    }

    override fun onStartInput(attribute: EditorInfo, restarting: Boolean) {
        currentInputConnection = currentInputConnection
    }

    override fun onFinishInput() {
        currentInputConnection = null
    }
}
```

**对比改造前后**：

| 维度 | 改造前 | 改造后 |
|------|--------|--------|
| ImeService.onStartInput | 手动收集 `engine.output` + 手动桥接 | 仅更新 currentInputConnection 引用 |
| InputConnection 生命周期 | 需在 onStartInput 中重建 bridge 并重启收集 | supplier 自然返回新实例 |
| 空安全 | `inputConnectionBridge` 可空，需手动管理 | supplier 返回 null 时自动跳过分发 |
| 引擎创建时机 | 必须等 InputConnection 就绪 | 任意时机创建，supplier 延迟解析 |

---

## 9. 库模块的依赖关系

### 9.1 内部依赖

```
ImeEngine (公开入口, :ime-engine)
  ├── ImeConfig       ← 配置（纯数据）
  ├── ImeOutputBridge ← 输出桥梁接口（纯接口）
  │   └── BaseImeOutputBridge ← 输出桥梁抽象类（撤销快照逻辑）
  ├── FeatureRegistry       ← 功能注册（纯逻辑）
  ├── KeyboardStateMachine  ← 状态机（纯逻辑）
  ├── InputListOperator     ← 输入列表操作（纯逻辑）
  ├── ImeDictProvider          ← 字典接口（纯接口）
  │   └── ImeSqliteDictProvider ← 内置 SQLite 实现（依赖 Android Context）
  ├── Keyboard              ← 键盘组合模式（纯逻辑）
  ├── Inputboard            ← 输入板（纯逻辑）
  └── Favoriteboard         ← 收藏板（纯逻辑，Feature.Favorites 启用时）

KeyboardPanel (公开入口, :ime-ui)
  ├── GestureInputPanel            ← 手势拦截层（依赖 Compose + engine.state）
  ├── KeyGridPanel              ← 按键渲染层（依赖 Compose + engine.state）
  ├── GestureFeedbackPanel         ← 反馈绘制层（依赖 Compose + GestureFeedbackState）
  ├── CandidateListPanel          ← 候选栏（依赖 Compose + engine.state）
  ├── InputListPanel              ← 输入栏（依赖 Compose + engine.state）
  ├── EditTextBridge          ← 编辑器桥梁（依赖 BaseImeOutputBridge + EditText）
  ├── KeyboardColors / KeyboardTheme ← 主题系统（依赖 Compose）
  └── KeyboardViewModel          ← 轻量桥接（依赖 ViewModel + engine）
```

### 9.2 外部依赖

| 依赖 | :ime-engine | :ime-ui | :app |
|------|:-----------:|:-------:|:----:|
| Kotlin 标准库 | ✅ | ✅ | ✅ |
| Kotlin 协程 | ✅ | ✅ | ✅ |
| Compose UI | ❌ | ✅ | ✅ |
| Compose Material3 | ❌ | ✅ | ✅ |
| Lifecycle ViewModel | ❌ | ✅ | ✅ |
| Android Context | ⚠️ 仅 ImeSqliteDictProvider | ❌ | ✅ |
| DataStore | ❌ | ❌ | ✅ |
| Android InputConnection | ❌ | ❌ | ✅ |

### 9.3 库的 build.gradle.kts

**`:ime-engine`**：

```kotlin
plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "org.crazydan.studio.app.ime.kuaizi.engine"
    compileSdk = 35

    defaultConfig {
        minSdk = 24
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    // Kotlin 协程（唯一外部依赖）
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    // 测试依赖
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    testImplementation("junit:junit:4.13.2")
}
```

**`:ime-ui`**：

```kotlin
plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "org.crazydan.studio.app.ime.kuaizi.ui"
    compileSdk = 35

    defaultConfig {
        minSdk = 24
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    // 引擎库
    api(project(":ime-engine"))

    // Compose
    implementation(platform("androidx.compose:compose-bom:2026.04.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // Lifecycle ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.0")

    // 测试依赖
    testImplementation("junit:junit:4.13.2")
}
```

> **注意**：`:ime-ui` 使用 `api(project(":ime-engine"))` 而非 `implementation`，使得引入 `:ime-ui` 的模块可以同时访问 `:ime-engine` 的公开 API，无需重复声明依赖。

---

## 10. 库的使用场景

### 10.1 系统 IME 服务

`:app` 模块的 `IMEService` 使用引擎和 UI 库提供系统级输入法服务：

```kotlin
class IMEService : InputMethodService() {
    private var currentInputConnection: InputConnection? = null
    private lateinit var engine: ImeEngine
    private lateinit var bridge: InputConnectionBridge

    override fun onCreate() {
        super.onCreate()
        engine = ImeEngine.create(
            config = ImeConfig(features = Feature.DefaultSet),
            dictProvider = ImeSqliteDictProvider(this),
        )
        bridge = InputConnectionBridge { currentInputConnection }
        engine.attachOutputBridge(bridge)
    }

    override fun onCreateInputView(): View {
        return ComposeView(this).also {
            it.setContent {
                KeyboardPanel(engine = engine)
            }
        }
    }

    override fun onStartInput(attribute: EditorInfo, restarting: Boolean) {
        currentInputConnection = currentInputConnection
    }

    override fun onFinishInput() {
        currentInputConnection = null
    }

    override fun onDestroy() {
        engine.detachOutputBridge()
        engine = null
        super.onDestroy()
    }
}
```

### 10.2 应用内嵌输入法（使用 UI 库）

第三方应用通过引擎库和 UI 库在应用内嵌入完整输入法，这是最常见的使用场景：

```kotlin
@Composable
fun ChatScreen() {
    var currentEditText by remember { mutableStateOf<EditText?>(null) }
    val engine = remember {
        ImeEngine.create(
            config = ImeConfig(
                features = setOf(Feature.CandidatePrediction),
                // 安全场景：禁用剪贴板和收藏
            ),
            dictProvider = ImeSqliteDictProvider(LocalContext.current),
        )
    }
    val bridge = remember { EditTextBridge { currentEditText } }

    LaunchedEffect(engine) {
        engine.attachOutputBridge(bridge)
    }

    Column {
        AndroidView(
            factory = { ctx -> EditText(ctx).also { currentEditText = it } },
            modifier = Modifier.weight(1f),
        )
        KeyboardPanel(
            engine = engine,
            modifier = Modifier.fillMaxWidth().height(280.dp),
        )
    }
}
```

或者更灵活地组合组件：

```kotlin
@Composable
fun CustomInputScreen() {
    val engine = remember {
        ImeEngine.create(
            config = ImeConfig(features = Feature.DefaultSet),
            dictProvider = ImeSqliteDictProvider(LocalContext.current),
        )
    }

    Column {
        // 自定义编辑区域 + 自定义桥梁
        MyCustomEditor(
            bridge = remember { MyCustomBridge { myEditor } }.also {
                LaunchedEffect(engine) { engine.attachOutputBridge(it) }
            },
        )

        // 使用 UI 库的键盘组件
        KeyboardPanel(
            engine = engine,
            modifier = Modifier.fillMaxWidth().height(280.dp),
            showToolbar = false, // 隐藏工具栏
        )
    }
}
```

### 10.3 应用内嵌输入法（自定义 UI，不使用 UI 库）

仅引入 `:ime-engine`，自行实现所有 UI：

```kotlin
// build.gradle.kts
dependencies {
    implementation(project(":ime-engine"))
    // 不引入 :ime-ui
}

@Composable
fun CustomInputScreen() {
    val engine = remember {
        ImeEngine.create(
            config = ImeConfig(features = emptySet()),
            dictProvider = ImeSqliteDictProvider(LocalContext.current),
        )
    }
    val state by engine.state.collectAsStateWithLifecycle()

    // 完全自定义的 UI
    MyCustomKeyboard(
        keyGrid = state.keyGrid,
        keyboardState = state.keyboardState,
        onGesture = { gesture ->
            engine.handleGesture(gesture)
        },
    )

    // 自定义桥梁或通过 output 通道处理输出
    val bridge = remember { MyCustomBridge { myCustomEditor } }
    LaunchedEffect(engine) {
        engine.attachOutputBridge(bridge)
    }
}
```

### 10.4 测试环境

使用内存字典提供者在测试中验证引擎逻辑：

```kotlin
class ImeEngineTest {
    @Test
    fun `swipe input should commit correct text`() = runTest {
        val engine = ImeEngine.create(
            config = ImeConfig(features = emptySet()),
            dictProvider = ImeInMemoryDictProvider(
                pinyinData = mapOf(
                    "ni" to listOf(InputWord("你", "ni"), InputWord("尼", "ni")),
                    "hao" to listOf(InputWord("好", "hao")),
                ),
            ),
        )

        // 模拟滑行输入 "ni"
        engine.handleGesture(InputGesture.Swipe(
            timestamp = 0L,
            startKey = InputKey.Char(id = "char_n", label = "n", levels = emptyList(), replacements = emptyList()),
            endKey = InputKey.Char(id = "char_i", label = "i", levels = emptyList(), replacements = emptyList()),
            visitedKeys = emptyList(),
            duration = 150L,
        ))

        // 验证候选列表
        assertEquals(listOf(InputWord("你", "ni"), InputWord("尼", "ni")),
            engine.state.value.candidates.candidates)

        // 选择第一个候选
        engine.handleGesture(InputGesture.CandidateTap(timestamp = 100L, candidateIndex = 0))

        // 验证输出
        val output = engine.output.receive()
        assertEquals("你", (output as ImeOutput.CommitText).text)
    }
}
```

### 10.5 无 UI 的纯引擎模式

某些场景仅需要引擎的逻辑能力，不需要任何 UI：

```kotlin
// 文本预处理：将拼音文本转换为汉字
class PinyinTextProcessor {
    private val engine = ImeEngine.create(
        config = ImeConfig(features = setOf(Feature.CandidatePrediction)),
        dictProvider = ImeInMemoryDictProvider(pinyinData),
    )

    suspend fun convertPinyinToText(pinyin: String): String {
        val result = StringBuilder()
        val segments = pinyin.split(" ")

        for (segment in segments) {
            engine.handleIntent(ImeIntent.SelectCandidate(
                candidate = engine.state.value.candidates.candidates.first()
            ))
            val output = engine.output.receive()
            if (output is ImeOutput.CommitText) {
                result.append(output.text)
            }
        }

        return result.toString()
    }
}
```

### 10.6 仅使用部分 UI 组件

第三方应用可以只使用 UI 库中的部分组件，而非完整的 `KeyboardPanel`：

```kotlin
@Composable
fun MinimalInputScreen() {
    val engine = remember { ImeEngine.create(...) }
    val state by engine.state.collectAsStateWithLifecycle()
    val feedbackState = remember { GestureFeedbackState() }

    Column {
        // 只用候选栏
        CandidateListPanel(
            state = state.candidates,
            onCandidateSelected = { candidate ->
                engine.handleIntent(ImeIntent.SelectCandidate(candidate))
            },
        )

        // 只用输入栏
        InputListPanel(
            state = state.inputList,
            onGapTapped = { index ->
                engine.handleIntent(ImeIntent.MoveCursorTo(index))
            },
        )

        // 自定义键盘布局，但使用 UI 库的反馈面板
        MyCustomKeyLayout(
            keyGrid = state.keyGrid,
            keyboardState = state.keyboardState,
        )

        GestureFeedbackPanel(
            elements = setOf(FeedbackElementType.TouchTrail, FeedbackElementType.KeyHighlight),
            feedbackState = feedbackState,
            keyPanelLayout = myCustomLayoutInfo,
        )
    }
}
```

---

## 11. 引擎的完整能力清单

### 11.1 核心输入能力

| 能力 | 说明 | ImeOutput |
|------|------|-----------|
| 拼音输入 | 通过按键组合输入拼音，查询候选字词 | `CommitText` |
| 滑行输入 | 在按键间滑行，自动识别声母韵母 | `CommitText` |
| X-Pad 输入 | 通过六边形面板连续滑行输入 | `CommitText` |
| 候选选择 | 从候选列表中选择目标字词 | `CommitText` |
| 候选翻页 | 翻页查看更多候选 | 状态变更，无输出 |
| 候选过滤 | 通过拼音/部首/声调过滤候选 | 状态变更，无输出 |
| 配对符号 | 输入 ()、""、[] 等配对符号 | `InsertPairedSymbols` |
| 输入列表管理 | 输入列表的显示、编辑、游标移动 | 状态变更 + `MoveCursor` |
| 撤销/重做 | 撤销和重做输入操作 | `RevokeCommit` |
| 键盘切换 | 切换拼音/拉丁/数字/符号/Emoji 等 | 状态变更，无输出 |
| 长按输入 | 长按按键触发二级功能 | `CommitText` 或状态变更 |

### 11.2 可选能力

| 能力 | Feature | 说明 | ImeOutput |
|------|---------|------|-----------|
| 剪贴板监听 | `Clipboard` | 监听系统剪贴板，提取结构化数据 | 状态变更 |
| 剪贴板粘贴 | `Clipboard` | 粘贴剪贴板内容 | `CommitText` |
| 剪贴板提示 | `Clipboard` | 检测到验证码等特殊内容时弹窗提示 | 状态变更 |
| 收藏管理 | `Favorites` | 收藏常用文本 | 状态变更 |
| 收藏粘贴 | `Favorites` | 粘贴收藏的文本 | `CommitText` |
| 收藏操作 | `Favorites` | 收藏当前选中文本 | `PerformEdit(FAVORITE)` |
| 输入练习 | `InputPractice` | 程序化输入动画演示 | 状态变更 |
| 候选预测 | `CandidatePrediction` | HMM+Viterbi 候选排序 | 状态变更 |

---

## 12. 与其他设计文档的协作

| 协作系统 | 协作方式 |
|----------|----------|
| 架构总览（000） | `:ime-engine` 模块对应 Domain Layer + Data Layer 的接口层；`:ime-ui` 模块对应 UI Layer 的缺省实现；`:app` 模块对应 Platform Layer + ViewModel Layer + 配置持久化 + 设置页面 |
| 键盘状态机（100） | `KeyboardStateMachine` 在引擎内部运行，通过 `ImeState.keyboardState` 暴露状态 |
| 面板分离（150） | 三层面板（GestureInputPanel / KeyGridPanel / GestureFeedbackPanel）在 `:ime-ui` 库中实现，引擎不包含面板代码 |
| 输入列表（200） | `InputListOperator` 在引擎内部运行，通过 `ImeState.inputList` 暴露状态 |
| 字典系统（300） | `ImeDictProvider` 接口在引擎中定义，`ImeSqliteDictProvider` 在引擎中实现；外部可替换 |
| UI 迁移（400） | Compose 组件在 `:ime-ui` 库中实现，订阅引擎状态进行渲染 |
| 配置系统（500） | `ImeConfig` 是引擎的配置接口，不含持久化；`:app` 的 DataStore 持久化是应用层职责 |
| 剪贴板与收藏（600） | 通过 `Feature.Clipboard` 和 `Feature.Favorites` 按需启用/禁用；UI 组件在 `:ime-ui` 中 |
| X-Pad（700） | X-Pad 逻辑在引擎的 `Keyboard.Pinyin` 中，X-Pad 渲染在 `:ime-ui` 的 KeyGridPanel 中 |
| 输入动作程序化（930） | `InputActionPlayer` 通过 `Feature.InputPractice` 按需启用，通过引擎的 `GestureFeedbackState` 驱动反馈；UI 组件在 `:ime-ui` 中 |

---

## 13. Java 功能完整对照

| Java 组件 | v4 库模块对应 | 所属模块 | 说明 |
|-----------|-------------|---------|------|
| `IMEditor` | `ImeEngine` | `:ime-engine` | 核心引擎入口，接口更清晰 |
| `ImeSupportEditText` | `ImeOutputBridge` + `EditTextBridge` | `:ime-engine` + `:ime-ui` | 自动消费 ImeOutput |
| `ImeIntegratedActivity` | `ImeOutputBridge` + `KeyboardPanel`（接入示例） | `:ime-engine` + `:ime-ui` | 不绑定 Activity，可在任意 Composable 中使用 |
| `InputMsgListener` | `ImeOutput` 收集 | `:ime-engine` | 从消息回调改为 Flow/Channel |
| `InputMsg` / `InputMsgType` | `ImeOutput` sealed class | `:ime-engine` | 从枚举消息改为类型安全的 sealed class |
| `UserKeyMsg` / `UserInputMsg` | `InputGesture` / `ImeIntent` | `:ime-engine` | 从消息系统改为 Intent 体系 |
| `IMEConfig` | `ImeConfig` | `:ime-engine` | 无 SharedPreferences，纯代码配置 |
| `IMEditorDict` | `ImeDictProvider` 接口 + `ImeSqliteDictProvider` 实现 | `:ime-engine` | 从单例改为可替换的接口 |
| `ConfigChangeListener` | `ImeEngine.updateConfig()` | `:ime-engine` | 从回调改为主动更新 |
| `KeyboardPanel` + 手势检测 | `GestureInputPanel` + `KeyGridPanel` + `GestureFeedbackPanel` | `:ime-ui` | 三层分离，职责清晰 |
| `CandidatesView` | `CandidateListPanel` | `:ime-ui` | Compose LazyRow |
| `InputListView` | `InputListPanel` | `:ime-ui` | Compose LazyRow |
| `XPadView` | `XPadView`（Compose Canvas） | `:ime-ui` | Compose Canvas |
| `FavoriteboardView` | `FavoriteListPanel` | `:ime-ui` | Compose LazyColumn + SwipeToDismiss |
| `MainboardView` | `KeyboardPanel` | `:ime-ui` | 一站式键盘组件 |
| N/A | `Feature` / `FeatureRegistry` | `:ime-engine` | 新增：功能可裁剪 |
| N/A | `InputConnectionBridge` | `:app` | 新增：引擎输出到 InputConnection 的桥接 |
| N/A | `ImeOutputBridge` / `BaseImeOutputBridge` | `:ime-engine` | 新增：引擎与编辑器的桥梁接口 |
| N/A | `ImeInMemoryDictProvider` | `:ime-engine` | 新增：测试用内存字典 |
| N/A | `KeyboardColors` / `KeyboardTheme` | `:ime-ui` | 新增：可定制主题系统 |
