# 三层模块划分

## 1 概述

v4 版本将筷字输入法设计为三层库架构，支持其他程序以库的形式引入，提供完整的输入法能力支持：

- **引擎库 `:ime-engine`**：纯 Kotlin，不依赖 Android 框架（字典 I/O 除外），提供核心输入引擎能力
- **UI 库 `:ime-ui`**：基于 Compose 的缺省 UI 实现，对第三方应用开放，可作为即插即用的输入界面使用，也可被自定义 UI 替换
- **应用模块 `:app`**：系统 IME 服务壳、设置页面、配置持久化，是库的官方消费者

库的核心价值在于：任何 Android 应用都可以通过引入 `:ime-engine` + `:ime-ui`，获得筷字输入法的完整输入能力和缺省 UI——拼音滑行、X-Pad 连续输入、候选选择、输入列表管理、撤销重做等——而无需依赖系统 IME 服务。仅需要引擎逻辑的场景（如文本预处理、自动化测试）可以只引入 `:ime-engine` 而不引入 UI 库。需要完全自定义 UI 的场景可以只引入 `:ime-engine` 并自行实现视图层。

> **PlantUML 图**：[class-organization.puml](../diagrams/class-organization.puml)

---

## 2 模块职责与依赖

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

> **PlantUML 图**：[architecture.puml](../diagrams/architecture.puml)

---

## 3 设计原则

1. **引擎与 UI 完全分离**：`:ime-engine` 不包含任何 Compose 代码或 Android View，只暴露状态流和意图接口
2. **UI 库作为缺省实现对外开放**：`:ime-ui` 提供完整的 Compose UI 组件，第三方应用可直接使用，也可替换为自定义 UI
3. **统一配置**：库不内置配置持久化，所有配置通过 `ImeConfig`（含引擎配置和 UI 配置的明确隔离）在创建时或运行时设置
4. **数据库层可替换**：字典接口与实现分离，内置 SQLite 实现，外部可提供自己的 `ImeDictProvider`
5. **功能可裁剪**：收藏和剪贴板等可选功能通过 `Feature` 标记按需启用/禁用
6. **Fail Fast**：非法操作（如禁用收藏后调用收藏功能）立即抛出异常而非静默忽略

---

## 4 引擎库公开 API

### 4.1 ImeEngine

`ImeEngine` 是引擎库的核心入口点：

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

> **设计决策**：`ImeConfig` 合并了引擎配置与应用配置的职责，消除两套配置之间的字段重叠和同步问题。`ImeConfig` 作为引擎的运行时配置状态，可在 `:app` 模块中动态修改以直接影响引擎状态，UI 侧（含 `:ime-ui` 库）通过 `ImeState.config` 自动同步更新。在 `:app` 模块的配置界面上的操作需同时做配置持久化和对 `ImeConfig` 的更新，但需确保运行时的修改优先——`ImeConfig` 在运行时的修改始终优先于应用侧配置，直到应用重启。重启时，`ImeConfig` 根据持久化配置进行初始化。`ImeConfig.runtimeOverrides` 记录被运行时临时修改的字段，持久化同步时跳过这些字段。

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

引擎的编辑输出。`ImeOutput` 由引擎内部的 `dispatchToTarget()` 统一分发到 `ImeOutputBridge`，桥梁实现者无需理解 `ImeOutput` 类型体系：

```kotlin
sealed class ImeOutput {
    abstract val timestamp: Long

    /**
     * 提交文本。
     * 若携带 replacements 列表，桥梁需检查光标前文本是否在列表中，
     * 若匹配则替换光标前字符，否则正常插入。
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
 * 撤销机制：
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

用户意图的 sealed class 表达，所有用户操作统一为 `ImeIntent`，由 `ImeEngine.handleIntent()` 或 `ImeEngine.handleGesture()` 接收并处理。

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

`ImeState` 中引用的子状态类型定义如下。这些类型与 `ImeState` 一样是 `data class`，不可变，通过 `copy()` 模式创建新实例。

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

## 5 UI 库设计

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
| `KeyboardPanel` | `integration` | 叠加模式完整输入法组件（候选栏 + 输入栏 + 工具栏 + 三层面板叠加：GestureInputPanel + GestureFeedbackPanel + KeyGridPanel） |
| `KeyboardScreen` | `integration` | 全屏模式完整输入法组件（候选栏 + 输入栏 + 工具栏 + 手势输入面板与按键面板分离布局） |

> **注意**：编辑器桥接组件在 `:ime-engine` 的 `api/` 包中定义（ImeOutputBridge、BaseImeOutputBridge），具体实现分别在 `:app`（InputConnectionBridge）和 `:ime-ui`（EditTextBridge）中

**主题系统**：

| 组件 | 包路径 | 说明 |
|------|--------|------|
| `KeyboardColors` | `theme` | 颜色定义（键盘/候选/输入栏/X-Pad） |
| `KeyboardThemes` | `theme` | 预置主题（Light/Night） |
| `KeyboardTheme` | `theme` | 主题 Composable（支持跟随系统） |
| `LocalKeyboardColors` | `theme` | CompositionLocal 提供颜色 |

### 5.3 输出桥接机制

v4 采用 `ImeOutputBridge` 桥接模式。引擎内部统一执行 `when(ImeOutput)` 分发（仅一处），桥梁实现者只需实现语义方法，无需理解 `ImeOutput` 类型体系。

**架构**：

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

**模块职责**：

```
:ime-engine/api/  → ImeOutputBridge 接口 + BaseImeOutputBridge 抽象类
:ime-ui/          → EditTextBridge
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
