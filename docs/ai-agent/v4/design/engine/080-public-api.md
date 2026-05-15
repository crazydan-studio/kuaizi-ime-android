# 引擎库公开 API

本文档描述 `:ime-engine` 引擎库对外暴露的公开 API，包括核心入口点、配置模型、意图体系、输出类型和状态子类型。这些 API 构成了引擎库与 UI 库（`:ime-ui`）及应用模块（`:app`）之间的契约，也是第三方应用使用引擎库的唯一接口。

输出桥接机制（`ImeOutputBridge`、`BaseImeOutputBridge` 及其实现）的完整设计见 [090-输出桥接机制](090-output-bridge.md)。

---

## 1 ImeEngine

`ImeEngine` 是引擎库的核心入口点，提供完整的输入法能力：拼音输入、滑行输入、X-Pad 连续输入、候选选择、输入列表管理、撤销重做等。引擎不依赖任何 UI 框架，通过 `StateFlow` 暴露状态，通过 `Intent` 接收用户操作，通过 `Output` 输出编辑指令。

输出分发方式：

- **主路径**：通过 `ImeOutputBridge` 桥接模式，引擎内部统一 `when` 分发，桥梁实现者只需实现语义方法，无需理解 `ImeOutput` 类型体系
- **备用路径**：通过 `output: ReceiveChannel<ImeOutput>` 通道，供高级场景使用

```kotlin
/**
 * IME 引擎，库的核心入口点。
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
 * // 创建 ViewModel（:ime-ui）
 * val viewModel = KeyboardViewModel(engine)
 *
 * // 订阅状态
 * viewModel.state.collect { state -> updateUI(state) }
 *
 * // 发送用户操作（由 ViewModel 做手势转换）
 * viewModel.handleGesture(InputGesture.Tap(timestamp, key))
 *
 * // 或直接发送意图
 * viewModel.handleIntent(ImeIntent.SwitchKeyboard(KeyboardType.Latin))
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
     * 处理意图。
     *
     * 直接发送 ImeIntent 到引擎，适用于程序化操作
     * （如键盘切换、候选选择等非手势操作）。
     *
     * InputGesture → ImeIntent 的转换由 KeyboardViewModel（:ime-ui）负责，
     * 引擎只接收 ImeIntent，不感知 InputGesture。
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

---

## 2 ImeConfig

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
 * ImeConfig.runtimeOverrides 记录被运行时覆盖的字段，持久化同步时应跳过这些字段。
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

---

## 3 ImeOutput

引擎的编辑输出。`ImeOutput` 由引擎内部的 `dispatchToTarget()` 统一分发到 `ImeOutputBridge`，桥梁实现者无需理解 `ImeOutput` 类型体系。桥接接口和基础抽象类的完整设计见 [090-输出桥接机制](090-output-bridge.md)。

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

---

## 4 ImeIntent

用户意图的 sealed class 表达，所有用户操作统一为 `ImeIntent`，由 `KeyboardViewModel.handleIntent()` 接收并委托给 `ImeEngine.handleIntent()` 处理。

`InputGesture`（文档 150）是输入面板的输出，`ImeIntent` 是 ViewModel/引擎的输入。`KeyboardViewModel` 将 `InputGesture` 转换为 `ImeIntent`（转换逻辑见 [050-KeyboardViewModel](../ui/050-keyboard-view-model.md) §2.1），这种两层转换使得同一手势可以根据当前键盘状态产生不同的 Intent，也使得不同手势可以产生相同的 Intent。

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
     * 运行时修改优先于持久化配置（详见第 2 节）。
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

---

## 5 ImeState 子状态类型

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
