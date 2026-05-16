# 日志系统

v4 版本的日志系统在应用层提供引擎日志基础设施的初始化集成和日志管理界面。核心日志基础设施（`ImeLog`、`ImeLogger`、`LogLevel`、`LogEntry`、`LogWriter` 接口、`LogStorage`、`FileLogWriter`）和直接可用的工具类（`LogcatWriter`、`CrashInterceptor`）定义在 `:ime-engine` 引擎库中，详见 [080-日志系统](../engine/080-logging.md)。本模块（`:app`）负责引擎日志基础设施的初始化集成，以及日志查看、导出、等级配置、存储路径配置等用户界面。

**模块归属**：应用层负责日志系统的初始化集成（使用引擎提供的 `LogcatWriter` 和 `CrashInterceptor` 工具类），以及日志相关的界面（`LogViewerScreen`、`LogExportScreen`、`LogLevelSetting`、`LogStoragePathSetting`、`LogViewerViewModel`）。日志核心基础设施和工具类划归 `:ime-engine` 模块。

---

## 1. 架构总览

应用层在引擎日志基础设施之上，提供两个层面的扩展：初始化集成层（使用引擎提供的工具类初始化日志系统）和用户界面层（查看、导出、配置）。这两层共同构成完整的 Android 日志体验。

```
┌───────────────────────────────────────────────────────────────┐
│                    用户界面层 [:app]                             │
│  LogViewerScreen / LogExportScreen / LogLevelSetting           │
│  LogStoragePathSetting / LogViewerViewModel                    │
├───────────────────────────────────────────────────────────────┤
│                    初始化集成层 [:app]                           │
│  ImeLog.init(level, writers) → 注册 LogcatWriter + FileLogWriter│
│  CrashInterceptor(writers, storage).install() → 安装崩溃拦截   │
│  路径解析 → Context.filesDir / SAF URI → File → LogStorage     │
├───────────────────────────────────────────────────────────────┤
│               引擎日志基础设施 [:ime-engine]                     │
│  ImeLog / ImeLogger / LogLevel / LogEntry / LogWriter          │
│  LogStorage / FileLogWriter / LogcatWriter / CrashInterceptor  │
└───────────────────────────────────────────────────────────────┘
```

应用层不修改引擎的日志核心逻辑，仅通过 `ImeLog.init()` 注入平台特有的配置和使用引擎提供的工具类（`LogcatWriter`、`CrashInterceptor`），通过 `ImeLog.updateLevel()` 和 `LogStorage.updateDir()` 响应用户的运行时配置变更。这种分层确保了引擎库的平台无关性，同时为 Android 应用提供完整的日志能力。

---

## 2. 日志系统初始化

应用层负责在应用启动时初始化引擎日志基础设施，包括：根据构建类型确定初始日志等级、创建并注册平台特有的 `LogWriter`、解析日志存储路径。

```kotlin
/**
 * 日志系统初始化。
 *
 * 在 Application.onCreate() 或 IMEService.onCreate() 中调用。
 * 根据 BuildConfig.DEBUG 确定初始日志等级，
 * 创建并注册 LogcatWriter（仅 debug）和 FileLogWriter，
 * 解析日志存储路径并创建 LogStorage。
 */
fun initLogging(context: Context, config: ImeConfig) {
    val level = if (BuildConfig.DEBUG) LogLevel.VERBOSE
                else config.ui.logLevel

    val logDir = resolveLogDir(context, config.ui.logStoragePath)
    val storage = LogStorage(logDir)

    val writers = buildList {
        // 引擎提供的 LogcatWriter，仅 debug 构建使用
        if (BuildConfig.DEBUG) {
            add(LogcatWriter())
        }
        // 文件持久化始终启用
        add(FileLogWriter(storage))
    }

    ImeLog.init(level, writers)

    // 安装引擎提供的崩溃拦截器
    CrashInterceptor(writers, storage).install()
}

/**
 * 解析日志存储目录。
 *
 * 优先级：
 * 1. 用户配置路径（通过 SAF 选择的 URI）→ 优先使用
 * 2. 应用私有目录 {filesDir}/logs/ → 缺省路径
 * 3. 配置路径无效 → 降级到缺省路径并记录警告日志
 */
private fun resolveLogDir(context: Context, customPath: String?): File {
    if (customPath != null) {
        val dir = File(customPath)
        if (dir.isDirectory || dir.mkdirs()) {
            return dir
        }
        // 降级到缺省路径
        ImeLog.logger("LogInit").warn { "配置的日志路径无效: $customPath，降级到缺省路径" }
    }
    return File(context.filesDir, "logs")
}
```

初始化的关键设计决策是将所有 Android 平台依赖（`Context`、`BuildConfig.DEBUG`）集中在应用层处理，引擎库的 `ImeLog.init()` 只接收纯 Kotlin 参数（`LogLevel` 和 `List<LogWriter>`）。`resolveLogDir()` 方法将 Android 的 `Context.filesDir` 或 SAF URI 转换为纯 `File` 对象，传给引擎的 `LogStorage`，确保引擎不持有任何 Android 引用。

日志等级与 `ImeConfig.UiConfig` 集成，配置字段 `logLevel` 和 `logStoragePath` 由 `ConfigDataStore` 管理。等级变更通过 `ImeLog.updateLevel()` 立即生效，无需重启应用。路径变更通过 `LogStorage.updateDir()` 切换写入目录。

---

## 3. 日志查看界面

### 3.1 LogViewerScreen

```kotlin
/**
 * 日志浏览界面。
 *
 * 支持按等级过滤、关键词搜索、实时滚动。
 * 日志条目使用 LazyColumn 渲染，不同等级用不同颜色标识。
 */
@Composable
fun LogViewerScreen(
    viewModel: LogViewerViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        // 工具栏
        LogViewerToolbar(
            levelFilter = state.levelFilter,
            keyword = state.keyword,
            onLevelFilterChange = viewModel::setLevelFilter,
            onKeywordChange = viewModel::setKeyword,
            onRefresh = viewModel::refresh,
        )

        // 日志列表
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = state.listState,
        ) {
            items(state.entries, key = { it.timestamp }) { entry ->
                LogEntryItem(entry)
            }
        }
    }
}

@Composable
private fun LogEntryItem(entry: LogEntry) {
    val textColor = when (entry.level) {
        LogLevel.VERBOSE -> Color.Gray
        LogLevel.DEBUG -> Color(0xFF2196F3)  // 蓝色
        LogLevel.INFO -> Color(0xFF4CAF50)   // 绿色
        LogLevel.WARN -> Color(0xFFFF9800)   // 橙色
        LogLevel.ERROR -> Color(0xFFF44336)  // 红色
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp),
    ) {
        Text(
            text = entry.format(),
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            color = textColor,
        )
    }
}
```

`LogViewerScreen` 是应用内置的日志浏览界面，使用 `LazyColumn` 渲染日志条目列表，支持按等级过滤和关键词搜索。不同日志等级使用不同颜色标识，使得在查看大量日志时可以快速区分信息类型：灰色表示 VERBOSE（最详细的追踪信息）、蓝色表示 DEBUG（开发期调试信息）、绿色表示 INFO（关键业务节点）、橙色表示 WARN（可恢复的异常情况）、红色表示 ERROR（不可恢复错误）。日志条目使用等宽字体显示，确保时间戳和标签列对齐，便于快速浏览和定位问题。

### 3.2 LogViewerViewModel

```kotlin
class LogViewerViewModel(private val storage: LogStorage) : ViewModel() {
    private val _state = MutableStateFlow(LogViewerState())
    val state: StateFlow<LogViewerState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch(Dispatchers.IO) {
            val entries = storage.readLogs(
                levelFilter = _state.value.levelFilter,
                keyword = _state.value.keyword,
            )
            _state.update { it.copy(entries = entries) }
        }
    }

    fun setLevelFilter(level: LogLevel?) {
        _state.update { it.copy(levelFilter = level) }
        refresh()
    }

    fun setKeyword(keyword: String?) {
        _state.update { it.copy(keyword = keyword) }
        refresh()
    }
}

data class LogViewerState(
    val entries: List<LogEntry> = emptyList(),
    val levelFilter: LogLevel? = null,
    val keyword: String? = null,
    val listState: LazyListState = LazyListState(),
)
```

`LogViewerViewModel` 管理日志浏览界面的状态，使用 `StateFlow` 驱动 UI 更新。日志读取操作在 `Dispatchers.IO` 上执行，避免阻塞主线程。过滤条件变更时自动刷新日志列表。`LogStorage.readLogs()` 方法支持按等级和关键词过滤，`LogViewerViewModel` 将用户的选择传递给 `LogStorage`，获取过滤后的日志条目列表。

---

## 4. 日志导出

日志导出与用户数据导入导出功能中的导出机制协同：

```kotlin
/**
 * 导出日志到用户指定位置。
 *
 * 通过 Activity Result API 的 CreateDocument 合约，
 * 让用户选择保存位置，默认文件名包含日期范围。
 */
class LogExportScreen : ComponentActivity() {
    private val createDocumentLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        uri?.let { viewModel.exportTo(it) }
    }

    fun startExport(fromDate: LocalDate, toDate: LocalDate) {
        val fileName = "kuaizi_ime_log_${dateFormat.format(fromDate)}_${dateFormat.format(toDate)}.txt"
        createDocumentLauncher.launch(fileName)
    }
}
```

日志导出也集成到统一导出界面中，作为独立导出项：

| 导出项 | 格式 | 包含内容 |
|--------|------|----------|
| 用户数据 | JSON | 输入频率、收藏、配置 |
| 诊断日志 | TXT | 指定日期范围的日志文件 |

日志导出使用 Android 的 Activity Result API `CreateDocument` 合约，让用户通过系统文件选择器指定保存位置。导出文件为纯文本格式，包含指定日期范围内所有日志文件的内容，每个文件以文件名作为分隔标识。默认文件名包含日期范围，便于识别导出的日志涵盖的时间段。导出操作委托给引擎的 `LogStorage.exportLogs()` 方法执行，应用层只负责文件选择和 URI 处理。

---

## 5. 日志等级管理

### 5.1 等级切换界面

```kotlin
@Composable
fun LogLevelSetting(
    currentLevel: LogLevel,
    isDebugBuild: Boolean,
    onLevelChange: (LogLevel) -> Unit,
) {
    if (isDebugBuild) {
        // Debug 构建显示当前等级但不可修改
        ListItem(
            headlineContent = { Text("日志等级") },
            supportingContent = { Text("调试构建固定为 VERBOSE") },
        )
    } else {
        // Release 构建提供等级选择
        var showDialog by remember { mutableStateOf(false) }

        ListItem(
            headlineContent = { Text("日志等级") },
            supportingContent = { Text(currentLevel.name) },
            modifier = Modifier.clickable { showDialog = true },
        )

        if (showDialog) {
            AlertDialog(
                title = { Text("选择日志等级") },
                text = {
                    Column {
                        LogLevel.entries.forEach { level ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onLevelChange(level)
                                        showDialog = false
                                    }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                RadioButton(
                                    selected = level == currentLevel,
                                    onClick = {
                                        onLevelChange(level)
                                        showDialog = false
                                    },
                                )
                                Text(level.displayName)
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showDialog = false }) {
                        Text("取消")
                    }
                },
            )
        }
    }
}

val LogLevel.displayName: String
    get() = when (this) {
        LogLevel.VERBOSE -> "VERBOSE（详细）- 记录所有日志"
        LogLevel.DEBUG -> "DEBUG（调试）- 记录调试及以上日志"
        LogLevel.INFO -> "INFO（信息）- 记录一般及以上日志"
        LogLevel.WARN -> "WARN（警告）- 仅记录警告和错误"
        LogLevel.ERROR -> "ERROR（错误）- 仅记录错误"
    }
```

### 5.2 等级变更流程

```
用户在设置中选择日志等级
    ↓
ConfigDataStore.updateConfig { it.copy(ui = it.ui.copy(logLevel = newLevel)) }
    ↓
ImeLog.updateLevel(newLevel)
    ↓
后续日志按新等级过滤
```

等级变更立即生效，不需要重启应用。DataStore 异步持久化确保配置在应用重启后恢复。

Debug 构建的日志等级固定为 `VERBOSE`，界面上显示当前等级但不允许修改，这确保开发阶段始终有完整的日志输出。Release 构建允许用户通过设置页面调整日志等级，典型的使用场景是用户遇到问题后，在设置中将等级从 `WARN` 调低到 `DEBUG`，复现问题后导出日志发送给开发者排查。等级变更通过 `ConfigDataStore` 持久化到 DataStore，同时通过 `ImeLog.updateLevel()` 立即更新运行时等级，两条路径并行确保即时生效和持久恢复。

---

## 6. 日志存储路径配置

### 6.1 路径选择

```kotlin
@Composable
fun LogStoragePathSetting(
    currentPath: String?,
    onPathChange: (String?) -> Unit,
) {
    var showPicker by remember { mutableStateOf(false) }

    ListItem(
        headlineContent = { Text("日志存储路径") },
        supportingContent = {
            Text(currentPath ?: "应用私有目录（缺省）")
        },
        modifier = Modifier.clickable { showPicker = true },
    )

    if (showPicker) {
        // 使用系统目录选择器
        val launcher = rememberLauncherForActivityResult(
            ActivityResultContracts.OpenDocumentTree()
        ) { uri ->
            uri?.let {
                // 获取持久化权限
                context.contentResolver.takePersistableUriPermission(
                    it, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
                onPathChange(it.toString())
            }
            showPicker = false
        }
        // 触发选择器...
    }
}
```

### 6.2 路径解析优先级

1. 用户配置路径（通过 SAF 选择的 URI）→ 优先使用
2. 应用私有目录 `{filesDir}/logs/` → 缺省路径
3. 配置路径无效（目录不存在且无法创建）→ 降级到缺省路径并记录警告日志

日志存储路径通过 Android SAF（Storage Access Framework）的 `OpenDocumentTree` 合约让用户选择目录，应用获取持久化 URI 权限后，将 URI 路径存入 `ImeConfig.UiConfig.logStoragePath`。路径变更时，应用层同时更新 `ConfigDataStore` 的持久化配置和 `LogStorage.updateDir()` 的运行时目录，确保后续日志写入新路径。缺省路径为应用私有目录下的 `logs/` 子目录，无需用户配置即可使用，但日志文件在应用卸载后会被系统清除。用户选择外部存储路径后，日志文件在应用卸载后仍可保留，便于问题排查和数据迁移。

---

## 7. 与 UI 测试的协作

UI 测试方案与应用日志系统协同工作，详见 [030-UI 测试方案](030-ui-testing.md)：

| 协作点 | 说明 |
|--------|------|
| 组件信息 → 日志记录 | 「组件信息查看」工具可将选中组件的调试信息以 INFO 等级写入日志 |
| 重组追踪 → 日志记录 | 重组超过阈值的组件自动记录 WARN 日志，便于后续排查 |
| 布局异常 → 日志警告 | 检测到布局溢出（组件尺寸超出父容器）时自动记录 WARN 日志 |
| 日志等级联动 | UI 测试工具激活时，自动将日志等级降至 DEBUG 以获取更完整信息 |

```kotlin
// debug 源集：UI 测试与日志联动
class DebugUITestOverlay(
    private val logFacade: ImeLog,
) : UITestOverlay {

    override fun enable() {
        // UI 测试激活时降级日志等级
        if (logFacade.level > LogLevel.DEBUG) {
            logFacade.updateLevel(LogLevel.DEBUG)
            logFacade.logger("UITest").info { "UI 测试工具已激活，日志等级已降至 DEBUG" }
        }
    }

    override fun toggle(tool: UITestTool) {
        if (tool in activeTools) {
            activeTools.remove(tool)
        } else {
            activeTools.add(tool)
            logFacade.logger("UITest").debug { "激活工具: ${tool.displayName}" }
        }
    }
}
```

UI 测试覆盖层在 debug 构建中激活时，自动将日志等级降至 `DEBUG`，确保开发者在调试 UI 问题时能够获取更完整的日志信息。组件信息查看、重组追踪等工具产生的日志使用 `ImeLogger` 的标准 API 写入，遵循统一的标签和等级约定，便于在日志查看界面中筛选和检索。
