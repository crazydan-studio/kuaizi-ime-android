# 160 — IME 引擎库设计

## 1. 概述

v4 版本将筷字输入法核心引擎设计为可被其他程序以库的形式引入的独立模块 `:ime-engine`，提供完整的输入法能力支持。该库对应 Java 版本中 `ImeSupportEditText` + `ImeIntegratedActivity` 的同等能力，但架构更加清晰：引擎与 UI 完全分离，配置通过代码设置（不含配置层），内置数据库层支持被外部实现替换，收藏和剪贴板等可选功能可按需禁用。

库模块的核心价值在于：任何 Android 应用都可以通过引入 `:ime-engine` 模块，获得筷字输入法的完整输入能力——拼音滑行、X-Pad 连续输入、候选选择、输入列表管理、撤销重做等——而无需依赖系统 IME 服务。这对于应用内嵌输入场景（如练习应用、游戏内聊天、安全输入框等）至关重要。

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
| **配置硬编码 SharedPreferences** | 库的使用者无法通过代码设置配置，必须依赖 SharedPreferences |
| **数据库不可替换** | `IMEditorDict` 是单例，使用固定路径的 SQLite，外部无法替换为其他存储实现 |
| **功能不可裁剪** | 收藏和剪贴板功能与引擎深度绑定，无法按需禁用 |

---

## 3. v4 库模块设计

### 3.1 模块划分

```
kuaizi-ime-android/
├── code/
│   ├── app/                  ← 应用模块（系统 IME 服务 + 设置 + 引导）
│   │   └── build.gradle.kts  ← implementation(project(":ime-engine"))
│   │
│   └── ime-engine/           ← 引擎库模块（新）
│       ├── build.gradle.kts  ← android.library
│       └── src/main/
│           └── org/crazydan/ime/engine/
│               ├── api/          ← 公开 API（ImeEngine, ImeOutput, ImeConfig）
│               ├── domain/       ← 领域层（Keyboard, InputList, Inputboard）
│               ├── dict/         ← 字典系统（接口 + 内置实现）
│               ├── input/        ← 输入类型（InputKey, InputWord, InputGesture）
│               └── state/        ← 状态定义（KeyboardState, IMEState）
│
├── docs/
└── ...
```

**模块职责**：

| 模块 | 职责 | 依赖 |
|------|------|------|
| `:ime-engine` | IME 核心引擎，纯 Kotlin，不依赖 Android 框架（字典 I/O 除外） | Kotlin 标准库 + 协程 |
| `:app` | 系统 IME 服务、Compose UI、设置页面 | `:ime-engine` + Compose + DataStore |

### 3.2 设计原则

1. **引擎与 UI 完全分离**：`:ime-engine` 不包含任何 Compose 代码或 Android View，只暴露状态流和意图接口
2. **配置通过代码设置**：库不内置配置持久化，所有配置通过 `ImeEngineConfig` 在创建时或运行时设置
3. **数据库层可替换**：字典接口与实现分离，内置 SQLite 实现，外部可提供自己的 `DictProvider`
4. **功能可裁剪**：收藏和剪贴板等可选功能通过 `Feature` 标记按需启用/禁用
5. **Fail Fast**：非法操作（如禁用收藏后调用收藏功能）立即抛出异常而非静默忽略

---

## 4. 公开 API 设计

### 4.1 ImeEngine

`ImeEngine` 是库的核心入口点，对应 Java 版本的 `IMEditor` 但接口更清晰：

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
 * 使用方式：
 * ```kotlin
 * val engine = ImeEngine.create(
 *     config = ImeEngineConfig(
 *         keyboardType = KeyboardType.Pinyin,
 *         handMode = HandMode.Right,
 *         features = setOf(Feature.Clipboard, Feature.Favorites),
 *     ),
 *     dictProvider = SqliteDictProvider(context),
 * )
 *
 * // 订阅状态
 * engine.state.collect { state -> updateUI(state) }
 *
 * // 订阅输出
 * engine.output.collect { output -> applyToEditor(output) }
 *
 * // 发送用户操作
 * engine.handleGesture(InputGesture.Tap(timestamp, key))
 * ```
 */
class ImeEngine internal constructor(
    private val config: ImeEngineConfig,
    private val dictProvider: DictProvider,
    private val stateMachine: KeyboardStateMachine,
    private val inputListOp: InputListOperator,
    private val featureRegistry: FeatureRegistry,
) {
    private val _state = MutableStateFlow(IMEState())
    val state: StateFlow<IMEState> = _state.asStateFlow()

    private val _output = MutableChannel<ImeOutput>()
    val output: ReceiveChannel<ImeOutput> = _output

    /**
     * 处理输入手势。
     *
     * 将 InputGesture 转换为 IMEIntent 并执行 reduce，
     * 更新状态和输出。
     */
    fun handleGesture(gesture: InputGesture) {
        val intent = gestureToIntent(gesture)
        handleIntent(intent)
    }

    /**
     * 处理意图。
     *
     * 直接发送 IMEIntent 到引擎，适用于程序化操作
     * （如键盘切换、候选选择等非手势操作）。
     */
    fun handleIntent(intent: IMEIntent) {
        val newState = reduce(_state.value, intent)
        _state.update { newState }
    }

    /**
     * 更新运行时配置。
     *
     * 不持久化，仅在引擎生命周期内有效。
     */
    fun updateConfig(block: (ImeEngineConfig) -> ImeEngineConfig) {
        // 应用配置变更并触发必要的重组
    }

    companion object {
        /**
         * 创建引擎实例。
         *
         * @param config 引擎配置
         * @param dictProvider 字典提供者（内置 SQLite 或外部实现）
         */
        fun create(
            config: ImeEngineConfig = ImeEngineConfig(),
            dictProvider: DictProvider,
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

### 4.2 ImeEngineConfig

库的配置通过代码设置，不内置持久化：

```kotlin
/**
 * IME 引擎配置。
 *
 * 库不内置配置持久化，所有配置通过此 data class 在创建时或运行时设置。
 * 持久化是应用层的职责（如 :app 模块使用 DataStore）。
 */
data class ImeEngineConfig(
    /** 默认键盘类型 */
    val keyboardType: KeyboardType = KeyboardType.Pinyin,
    /** 手模式 */
    val handMode: HandMode = HandMode.Right,
    /** 启用的功能集 */
    val features: Set<Feature> = Feature.DefaultSet,
    /** 音频反馈是否启用 */
    val audioFeedbackEnabled: Boolean = true,
    /** 触觉反馈是否启用 */
    val hapticFeedbackEnabled: Boolean = true,
    /** X-Pad 是否启用 */
    val xPadEnabled: Boolean = true,
    /** 单行输入模式 */
    val singleLineInput: Boolean = false,
    /** 候选预测是否启用 */
    val candidatePredictionEnabled: Boolean = true,
)

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

引擎的编辑输出，对应 Java 版本中 `InputMsg` 的编辑指令部分。这是库使用者需要应用到目标编辑器的操作：

```kotlin
/**
 * IME 引擎输出。
 *
 * 引擎通过此 sealed class 向外部输出编辑指令。
 * 库使用者负责将这些指令应用到目标编辑器
 * （如 EditText、InputConnection、自定义编辑器等）。
 *
 * 对应 Java 版本中 ImeSupportEditText.onMsg() 处理的 InputMsg 子集，
 * 但更清晰地区分了「编辑输出」和「状态通知」。
 */
sealed class ImeOutput {
    abstract val timestamp: Long

    /**
     * 提交文本。
     *
     * 将指定文本插入到编辑器的当前光标位置，
     * 如果指定了 replacement，则替换选中文本。
     * 对应 Java: InputList_Commit_Doing
     */
    data class CommitText(
        override val timestamp: Long,
        val text: String,
        val replacement: String? = null,
    ) : ImeOutput()

    /**
     * 撤销上次提交。
     *
     * 恢复到上次提交前的文本和光标状态。
     * 对应 Java: InputList_Committed_Revoke_Doing
     */
    data class RevokeCommit(
        override val timestamp: Long,
        val beforeState: EditorState,
    ) : ImeOutput()

    /**
     * 插入配对符号。
     *
     * 在选中文本两侧插入左右符号，如 ()、""、[]。
     * 如果没有选中文本，则插入左右符号并将光标置于中间。
     * 对应 Java: InputList_PairSymbol_Commit_Doing
     */
    data class InsertPairedSymbols(
        override val timestamp: Long,
        val left: String,
        val right: String,
    ) : ImeOutput()

    /**
     * 移动光标。
     *
     * 向指定方向移动光标。
     * 对应 Java: Editor_Cursor_Move_Doing
     */
    data class MoveCursor(
        override val timestamp: Long,
        val direction: CursorDirection,
    ) : ImeOutput()

    /**
     * 选择文本范围。
     *
     * 扩展选区到指定方向。
     * 对应 Java: Editor_Range_Select_Doing
     */
    data class SelectRange(
        override val timestamp: Long,
        val direction: CursorDirection,
    ) : ImeOutput()

    /**
     * 编辑操作。
     *
     * 退格、全选、复制、粘贴、剪切等。
     * 对应 Java: Editor_Edit_Doing
     */
    data class EditAction(
        override val timestamp: Long,
        val action: EditorAction,
    ) : ImeOutput()
}

enum class CursorDirection { Left, Right, Up, Down }

enum class EditorAction {
    Backspace, SelectAll, Copy, Paste, Cut, Undo, Redo
}

/**
 * 编辑器状态快照，用于撤销操作时恢复。
 */
data class EditorState(
    val selectionStart: Int,
    val selectionEnd: Int,
    val text: String,
)
```

### 4.4 ImeEditText

提供与 Java 版本 `ImeSupportEditText` 等价的便利组件，自动消费 `ImeOutput` 并应用到自身：

```kotlin
/**
 * IME 支持的 EditText。
 *
 * 对应 Java 版本的 ImeSupportEditText，但通过 ImeEngine 驱动，
 * 而非 InputMsg 消息系统。
 *
 * 特性：
 * - 禁用系统 IME 弹出（setShowSoftInputOnFocus = false）
 * - 自动消费 ImeEngine 的 ImeOutput 并应用到自身
 * - 提供引擎实例供外部创建输入面板
 *
 * 使用方式：
 * ```kotlin
 * val engine = ImeEngine.create(
 *     config = ImeEngineConfig(),
 *     dictProvider = SqliteDictProvider(context),
 * )
 *
 * // 在布局中使用
 * ImeEditText(
 *     engine = engine,
 *     modifier = Modifier.fillMaxWidth(),
 * )
 *
 * // 或在 Compose 中
 * KuaiziKeyboard(
 *     engine = engine,
 *     modifier = Modifier.fillMaxWidth(),
 * )
 * ```
 */
@Composable
fun ImeEditText(
    engine: ImeEngine,
    modifier: Modifier = Modifier,
) {
    var text by rememberSaveable { mutableStateOf("") }
    var selection by remember { mutableStateOf(TextRange(0)) }

    // 收集引擎输出并应用到文本编辑
    LaunchedEffect(engine) {
        engine.output.collect { output ->
            when (output) {
                is ImeOutput.CommitText -> {
                    // 在光标位置插入文本（或替换选区）
                    text = text.replaceRange(selection.start, selection.end, output.text)
                    selection = TextRange(selection.start + output.text.length)
                }
                is ImeOutput.RevokeCommit -> {
                    // 恢复到提交前的状态
                    text = output.beforeState.text
                    selection = TextRange(
                        output.beforeState.selectionStart,
                        output.beforeState.selectionEnd,
                    )
                }
                is ImeOutput.InsertPairedSymbols -> {
                    val selected = text.substring(selection.start, selection.end)
                    val newText = output.left + selected + output.right
                    text = text.replaceRange(selection.start, selection.end, newText)
                    selection = TextRange(
                        selection.start + output.left.length,
                        selection.start + output.left.length + selected.length,
                    )
                }
                is ImeOutput.MoveCursor -> {
                    // 根据 direction 移动光标
                }
                is ImeOutput.SelectRange -> {
                    // 根据 direction 扩展选区
                }
                is ImeOutput.EditAction -> {
                    // 处理编辑操作
                }
            }
        }
    }

    // 渲染编辑框
    BasicTextField(
        value = TextFieldValue(text, selection),
        onValueChange = { /* 仅接受来自引擎的变更 */ },
        modifier = modifier,
    )
}
```

---

## 5. 字典层可替换设计

### 5.1 DictProvider 接口

字典接口与实现分离，内置 SQLite 实现，外部可提供自己的实现：

```kotlin
/**
 * 字典提供者接口。
 *
 * 引擎通过此接口访问字典数据，不关心底层存储实现。
 * 内置 SqliteDictProvider 使用应用内 SQLite 数据库，
 * 外部可实现此接口替换为 Room、SQLDelight、远程 API 或内存字典等。
 */
interface DictProvider {

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

### 5.2 内置实现

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
class SqliteDictProvider(
    private val context: Context,
) : DictProvider {

    override lateinit var pinyin: PinyinDict
        private set

    override lateinit var userInputData: UserInputDataDict
        private set

    override lateinit var userInputFavorite: UserInputFavoriteDict
        private set

    private var db: SQLiteDatabase? = null

    override suspend fun initialize() {
        // 从 assets/dict/ 复制或打开预构建字典数据库
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

### 5.3 自定义实现示例

```kotlin
/**
 * 基于内存的字典提供者（示例）。
 *
 * 适用于测试环境或不需要持久化的场景。
 */
class InMemoryDictProvider(
    private val pinyinData: Map<String, List<InputWord>> = emptyMap(),
) : DictProvider {

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
class RemoteDictProvider(
    private val apiClient: ApiClient,
) : DictProvider {
    // ... 实现略 ...
}
```

---

## 6. 功能可裁剪设计

### 6.1 FeatureRegistry

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
            "功能 ${feature.name} 未启用，请在 ImeEngineConfig.features 中添加"
        }
    }
}
```

### 6.2 各功能的裁剪影响

| 功能 | 禁用后的影响 | 引擎内部检查点 |
|------|------------|---------------|
| **Clipboard** | 不创建剪贴板监听器；`ImeOutput.CommitText` 不包含剪贴板粘贴；剪贴板提示弹窗不显示 | `Inputboard` 初始化时检查，若禁用则跳过剪贴板组件创建 |
| **Favorites** | 不创建收藏管理器；`ImeOutput.CommitText` 不包含收藏粘贴；收藏面板不显示 | `Favoriteboard` 初始化时检查，若禁用则跳过收藏组件创建 |
| **InputPractice** | 不创建 `InputActionPlayer`；手指指示器不可用；程序化输入相关 Intent 抛出异常 | `InputActionPlayer` 创建时检查，若禁用则返回 null |
| **CandidatePrediction** | 不使用 HMM+Viterbi 预测；候选仅按静态频率排序 | `PinyinCandidateEvaluator` 初始化时检查，若禁用则使用简单排序 |

### 6.3 使用示例

```kotlin
// 最小配置：仅核心输入能力，无收藏、剪贴板、练习、预测
val minimalEngine = ImeEngine.create(
    config = ImeEngineConfig(
        features = emptySet(),
    ),
    dictProvider = SqliteDictProvider(context),
)

// 标准配置：核心输入 + 剪贴板 + 收藏 + 候选预测
val standardEngine = ImeEngine.create(
    config = ImeEngineConfig(
        features = Feature.DefaultSet,
    ),
    dictProvider = SqliteDictProvider(context),
)

// 完整配置：全部功能
val fullEngine = ImeEngine.create(
    config = ImeEngineConfig(
        features = Feature.entries.toSet(),
    ),
    dictProvider = SqliteDictProvider(context),
)

// 安全输入场景：无剪贴板（防止粘贴泄露），无收藏
val secureEngine = ImeEngine.create(
    config = ImeEngineConfig(
        features = setOf(Feature.CandidatePrediction),
        singleLineInput = true,
    ),
    dictProvider = SqliteDictProvider(context),
)
```

---

## 7. 引擎与 UI 的边界

### 7.1 引擎暴露给 UI 的契约

引擎通过 `StateFlow<IMEState>` 暴露状态，UI 层订阅此状态进行渲染。引擎不包含任何 Compose 代码：

```kotlin
/**
 * 引擎暴露给 UI 的完整状态。
 *
 * UI 层（Compose 或 View）订阅此状态进行渲染。
 * 引擎不关心 UI 如何渲染，只保证状态的正确性。
 */
data class IMEState(
    val keyboardType: KeyboardType = KeyboardType.Pinyin,
    val keyboardState: KeyboardState = KeyboardState.Idle,
    val keyGrid: List<List<InputKey>> = emptyList(),
    val inputList: InputListState = InputListState(),
    val candidates: CandidateState = CandidateState(),
    val clipboard: ClipboardState = ClipboardState(),
    val favorites: FavoritesState = FavoritesState(),
    val config: ImeEngineConfig = ImeEngineConfig(),
)
```

### 7.2 UI 层对引擎的使用

`:app` 模块中的 `IMEViewModel` 桥接引擎和 Compose UI：

```kotlin
/**
 * IME ViewModel，桥接 ImeEngine 和 Compose UI。
 *
 * :app 模块中的 ViewModel，持有 ImeEngine 实例，
 * 将引擎状态转发给 Compose UI，将 UI 事件转发给引擎。
 * 此类在 :ime-engine 中不存在，仅属于 :app 模块。
 */
class IMEViewModel(
    private val engine: ImeEngine,
) : ViewModel() {

    /** 直接暴露引擎状态给 UI */
    val state: StateFlow<IMEState> = engine.state

    /** 处理手势（由 InputPanel 调用） */
    fun handleGesture(gesture: InputGesture) {
        engine.handleGesture(gesture)
    }

    /** 处理意图（由 UI 控件调用） */
    fun handleIntent(intent: IMEIntent) {
        engine.handleIntent(intent)
    }

    /** 应用编辑输出（由 IMEService 调用，通过 InputConnection） */
    fun collectOutput(scope: CoroutineScope, block: suspend (ImeOutput) -> Unit) {
        scope.launch {
            engine.output.receiveAsFlow().collect(block)
        }
    }
}
```

### 7.3 引擎与 InputConnection 的桥接

在系统 IME 模式下，`ImeOutput` 需要映射到 `InputConnection` 操作：

```kotlin
/**
 * 将 ImeOutput 映射到 InputConnection 操作。
 *
 * 此桥接在 :app 模块的 IMEService 中，
 * 不属于 :ime-engine 库。
 */
class InputConnectionBridge(
    private val inputConnection: InputConnection,
) {
    fun apply(output: ImeOutput) {
        when (output) {
            is ImeOutput.CommitText -> {
                inputConnection.commitText(output.text, 1)
            }
            is ImeOutput.RevokeCommit -> {
                // 通过 SelectionOp 记录和撤销
                inputConnection.setSelection(
                    output.beforeState.selectionStart,
                    output.beforeState.selectionEnd,
                )
            }
            is ImeOutput.InsertPairedSymbols -> {
                // 提交左符号 + 选中 + 右符号
                val selectedText = inputConnection.getSelectedText(0) ?: ""
                inputConnection.commitText(
                    output.left + selectedText + output.right, 1,
                )
            }
            is ImeOutput.MoveCursor -> {
                val keyCode = when (output.direction) {
                    CursorDirection.Left -> KeyEvent.KEYCODE_DPAD_LEFT
                    CursorDirection.Right -> KeyEvent.KEYCODE_DPAD_RIGHT
                    CursorDirection.Up -> KeyEvent.KEYCODE_DPAD_UP
                    CursorDirection.Down -> KeyEvent.KEYCODE_DPAD_DOWN
                }
                inputConnection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
                inputConnection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
            }
            is ImeOutput.SelectRange -> {
                // Shift + DPAD
                val keyCode = when (output.direction) {
                    CursorDirection.Left -> KeyEvent.KEYCODE_DPAD_LEFT
                    CursorDirection.Right -> KeyEvent.KEYCODE_DPAD_RIGHT
                    CursorDirection.Up -> KeyEvent.KEYCODE_DPAD_UP
                    CursorDirection.Down -> KeyEvent.KEYCODE_DPAD_DOWN
                }
                val shiftPressed = KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_SHIFT_LEFT)
                inputConnection.sendKeyEvent(shiftPressed)
                inputConnection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
                inputConnection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
            }
            is ImeOutput.EditAction -> {
                when (output.action) {
                    EditorAction.Backspace -> {
                        inputConnection.sendKeyEvent(
                            KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL)
                        )
                        inputConnection.sendKeyEvent(
                            KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DEL)
                        )
                    }
                    EditorAction.SelectAll -> {
                        // 全选通过 performContextMenuAction
                        inputConnection.performContextMenuAction(android.R.id.selectAll)
                    }
                    EditorAction.Copy -> {
                        inputConnection.performContextMenuAction(android.R.id.copy)
                    }
                    EditorAction.Paste -> {
                        inputConnection.performContextMenuAction(android.R.id.paste)
                    }
                    EditorAction.Cut -> {
                        inputConnection.performContextMenuAction(android.R.id.cut)
                    }
                    EditorAction.Undo -> {
                        inputConnection.performContextMenuAction(android.R.id.undo)
                    }
                    EditorAction.Redo -> {
                        inputConnection.performContextMenuAction(android.R.id.redo)
                    }
                }
            }
        }
    }
}
```

---

## 8. 引擎的完整能力清单

### 8.1 核心输入能力

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

### 8.2 可选能力

| 能力 | Feature | 说明 | ImeOutput |
|------|---------|------|-----------|
| 剪贴板监听 | `Clipboard` | 监听系统剪贴板，提取结构化数据 | 状态变更 |
| 剪贴板粘贴 | `Clipboard` | 粘贴剪贴板内容 | `CommitText` |
| 剪贴板提示 | `Clipboard` | 检测到验证码等特殊内容时弹窗提示 | 状态变更 |
| 收藏管理 | `Favorites` | 收藏常用文本 | 状态变更 |
| 收藏粘贴 | `Favorites` | 粘贴收藏的文本 | `CommitText` |
| 输入练习 | `InputPractice` | 程序化输入动画演示 | 状态变更 |
| 候选预测 | `CandidatePrediction` | HMM+Viterbi 候选排序 | 状态变更 |

---

## 9. 库模块的依赖关系

### 9.1 内部依赖

```
ImeEngine (公开入口)
  ├── ImeEngineConfig       ← 配置（纯数据）
  ├── FeatureRegistry       ← 功能注册（纯逻辑）
  ├── KeyboardStateMachine  ← 状态机（纯逻辑）
  ├── InputListOperator     ← 输入列表操作（纯逻辑）
  ├── DictProvider          ← 字典接口（纯接口）
  │   └── SqliteDictProvider ← 内置 SQLite 实现（依赖 Android Context）
  ├── Keyboard              ← 键盘组合模式（纯逻辑）
  ├── Inputboard            ← 输入板（纯逻辑）
  └── Favoriteboard         ← 收藏板（纯逻辑，Feature.Favorites 启用时）
```

### 9.2 外部依赖

| 依赖 | 必需 | 说明 |
|------|------|------|
| Kotlin 标准库 | ✅ | 开发语言 |
| Kotlin 协程 | ✅ | 异步字典查询 |
| Android Context | ⚠️ | 仅 SqliteDictProvider 需要；自定义实现可替换 |
| Android InputConnection | ❌ | 属于 :app 模块，不属于引擎 |
| Compose | ❌ | 属于 :app 模块，不属于引擎 |

### 9.3 库的 build.gradle.kts

```kotlin
plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "org.crazydan.ime.engine"
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

---

## 10. 库的使用场景

### 10.1 系统 IME 服务

`:app` 模块的 `KuaiziIMEService` 使用引擎提供系统级输入法服务：

```kotlin
class KuaiziIMEService : InputMethodService() {
    private var engine: ImeEngine? = null
    private var bridge: InputConnectionBridge? = null

    override fun onCreate() {
        super.onCreate()
        engine = ImeEngine.create(
            config = ImeEngineConfig(features = Feature.DefaultSet),
            dictProvider = SqliteDictProvider(this),
        )
        bridge = InputConnectionBridge(currentInputConnection)

        // 应用编辑输出到 InputConnection
        lifecycleScope.launch {
            engine!!.output.receiveAsFlow().collect { output ->
                bridge?.apply(output)
            }
        }
    }

    override fun onCreateInputView(): View {
        return ComposeView(this).also {
            it.setContent {
                val viewModel = remember { IMEViewModel(engine!!) }
                IMEScreen(viewModel)
            }
        }
    }

    override fun onDestroy() {
        engine = null
        bridge = null
        super.onDestroy()
    }
}
```

### 10.2 应用内嵌输入法

第三方应用通过引擎和 Compose 组件在应用内嵌入输入法：

```kotlin
@Composable
fun ChatScreen() {
    val engine = remember {
        ImeEngine.create(
            config = ImeEngineConfig(
                features = setOf(Feature.CandidatePrediction),
                // 安全场景：禁用剪贴板和收藏
            ),
            dictProvider = SqliteDictProvider(LocalContext.current),
        )
    }

    Column {
        // 聊天消息列表
        MessageList(...)

        // 输入框
        ImeEditText(engine = engine)

        // 内嵌键盘
        KuaiziKeyboard(
            engine = engine,
            modifier = Modifier.fillMaxWidth().height(280.dp),
        )
    }
}
```

### 10.3 测试环境

使用内存字典提供者在测试中验证引擎逻辑：

```kotlin
class ImeEngineTest {
    @Test
    fun `swipe input should commit correct text`() = runTest {
        val engine = ImeEngine.create(
            config = ImeEngineConfig(features = emptySet()),
            dictProvider = InMemoryDictProvider(
                pinyinData = mapOf(
                    "ni" to listOf(InputWord("你", "ni"), InputWord("尼", "ni")),
                    "hao" to listOf(InputWord("好", "hao")),
                ),
            ),
        )

        // 模拟滑行输入 "ni"
        engine.handleGesture(InputGesture.Swipe(
            timestamp = 0L,
            startKey = InputKey.Char(id = "char_n", label = "n", levels = emptyList(), replacements = emptyMap()),
            endKey = InputKey.Char(id = "char_i", label = "i", levels = emptyList(), replacements = emptyMap()),
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

### 10.4 无 UI 的纯引擎模式

某些场景仅需要引擎的逻辑能力，不需要任何 UI：

```kotlin
// 文本预处理：将拼音文本转换为汉字
class PinyinTextProcessor {
    private val engine = ImeEngine.create(
        config = ImeEngineConfig(features = setOf(Feature.CandidatePrediction)),
        dictProvider = InMemoryDictProvider(pinyinData),
    )

    suspend fun convertPinyinToText(pinyin: String): String {
        val result = StringBuilder()
        val segments = pinyin.split(" ")

        for (segment in segments) {
            engine.handleIntent(IMEIntent.CandidateSelected(
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

---

## 11. 与其他设计文档的协作

| 协作系统 | 协作方式 |
|----------|----------|
| 架构总览（000） | `:ime-engine` 模块对应 Domain Layer + Data Layer 的接口层；`:app` 模块对应 Platform Layer + UI Layer + ViewModel Layer |
| 键盘状态机（100） | `KeyboardStateMachine` 在引擎内部运行，通过 `IMEState.keyboardState` 暴露状态 |
| 面板分离（150） | 三层面板（InputPanel / KeyPanel / FeedbackPanel）在 `:app` 模块中实现，引擎不包含面板代码 |
| 输入列表（200） | `InputListOperator` 在引擎内部运行，通过 `IMEState.inputList` 暴露状态 |
| 字典系统（300） | `DictProvider` 接口在引擎中定义，`SqliteDictProvider` 在引擎中实现；外部可替换 |
| UI 迁移（400） | Compose 组件在 `:app` 中，订阅引擎状态进行渲染 |
| 配置系统（500） | `ImeEngineConfig` 是引擎的配置接口，不含持久化；`:app` 的 DataStore 持久化是应用层职责 |
| 剪贴板与收藏（600） | 通过 `Feature.Clipboard` 和 `Feature.Favorites` 按需启用/禁用 |
| X-Pad（700） | X-Pad 逻辑在引擎的 `Keyboard.Pinyin` 中，X-Pad 渲染在 `:app` 的 KeyPanel 中 |
| 输入动作程序化（930） | `InputActionPlayer` 通过 `Feature.InputPractice` 按需启用，通过引擎的 `GestureFeedbackState` 驱动反馈 |

---

## 12. Java 功能完整对照

| Java 组件 | v4 库模块对应 | 说明 |
|-----------|-------------|------|
| `IMEditor` | `ImeEngine` | 核心引擎入口，接口更清晰 |
| `ImeSupportEditText` | `ImeEditText`（Compose） | 自动消费 ImeOutput |
| `ImeIntegratedActivity` | 无直接对应，由库使用者自行组合 | 更灵活，不绑定 Activity |
| `InputMsgListener` | `ImeOutput` 收集 | 从消息回调改为 Flow/Channel |
| `InputMsg` / `InputMsgType` | `ImeOutput` sealed class | 从枚举消息改为类型安全的 sealed class |
| `UserKeyMsg` / `UserInputMsg` | `InputGesture` / `IMEIntent` | 从消息系统改为 Intent 体系 |
| `IMEConfig` | `ImeEngineConfig` | 无 SharedPreferences，纯代码配置 |
| `IMEditorDict` | `DictProvider` 接口 + `SqliteDictProvider` 实现 | 从单例改为可替换的接口 |
| `ConfigChangeListener` | `ImeEngine.updateConfig()` | 从回调改为主动更新 |
| N/A | `Feature` / `FeatureRegistry` | 新增：功能可裁剪 |
| N/A | `InputConnectionBridge` | 新增：引擎输出到 InputConnection 的桥接 |
| N/A | `InMemoryDictProvider` | 新增：测试用内存字典 |
