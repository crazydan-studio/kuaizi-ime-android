# 900 — 应用日志系统设计

## 1. 概述

v4 版本设计完整的应用内置日志系统，替代 Java 版本简陋的 `Logger` 工具类。新系统需满足以下核心需求：日志分级输出、崩溃拦截记录、应用内日志查看与导出、按构建类型区分日志等级、可配置日志存放位置。Java 版本的 `Logger` 仅在 DEBUG 构建下生效、仅支持 Logcat 输出和简单的内存缓存，无法在崩溃后保留日志，也无法让用户在应用内查看或导出日志，v4 将彻底解决这些不足。

---

## 2. Java 版本日志系统分析

### 2.1 现有实现

Java 版本的日志系统由 `Logger` 和内部类 `TreeLog`、`LogCache` 组成：

| 组件 | 职责 | 问题 |
|------|------|------|
| `Logger` | 门面类，提供 debug/info/warn/error 方法 | release 构建返回 noop 实例，完全不记录日志 |
| `TreeLog` | 树形日志结构，支持嵌套的 begin/end 逻辑块 | 仅输出到 Logcat，不持久化 |
| `LogCache` | 内存缓存最近日志（String 列表） | 应用崩溃后缓存丢失；无大小限制可能 OOM |

### 2.2 关键问题

1. **Release 构建完全无日志**：`getLogger()` 在非 DEBUG 构建返回 noop，线上问题无法诊断
2. **无持久化**：日志仅输出到 Logcat 和内存缓存，进程死亡即丢失
3. **无崩溃拦截**：未捕获的异常直接导致进程崩溃，无异常日志记录
4. **无日志查看界面**：用户和开发者均无法在应用内查看日志
5. **无日志导出**：无法将日志文件分享或上传
6. **TreeLog 线程安全风险**：`ThreadLocal<Stack<TreeLog>>` + `synchronized` 混用，存在死锁隐患

---

## 3. v4 日志系统设计

### 3.1 设计目标

| 目标 | 说明 |
|------|------|
| 分级日志 | 支持 VERBOSE / DEBUG / INFO / WARN / ERROR 五级，按构建类型设置不同缺省等级 |
| 持久化存储 | 日志写入文件，应用重启后可读 |
| 崩溃拦截 | 捕获未处理异常，记录完整异常堆栈到日志文件 |
| 应用内查看 | 提供 Compose 界面浏览日志，支持按等级过滤和关键词搜索 |
| 日志导出 | 支持导出日志文件，与用户数据导入导出功能协同 |
| 可配置存储路径 | 日志存放位置可通过配置指定，缺省放在应用私有目录 |
| Release 可调等级 | 发布版本可通过设置修改日志等级（默认 WARN），无需重新构建 |
| 性能安全 | 日志写入使用独立协程，不阻塞主线程；日志文件有大小和数量上限 |

### 3.2 架构总览

```
┌─────────────────────────────────────────────────────────────┐
│                      调用层                                  │
│  Domain / ViewModel / Data / Platform 各层通过 AppLog 记录   │
└──────────────────────┬──────────────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────────────┐
│                    AppLog (门面)                              │
│  - 获取 Logger 实例                                          │
│  - 全局配置（等级、存储路径）                                  │
│  - 崩溃拦截安装                                              │
└──────────────────────┬──────────────────────────────────────┘
                       │
              ┌────────┼────────┐
              ▼        ▼        ▼
┌──────────────┐ ┌────────────┐ ┌───────────────┐
│ LogcatWriter │ │ FileWriter │ │ CrashInterceptor│
│ (Logcat 输出) │ │(文件持久化) │ │ (异常拦截)     │
└──────────────┘ └─────┬──────┘ └───────┬───────┘
                       │                 │
                       ▼                 ▼
              ┌─────────────────────────────────┐
              │      LogStorage                  │
              │  - 日志文件管理（滚动、清理）      │
              │  - 日志文件读取（查看、搜索、导出） │
              └─────────────────────────────────┘
```

### 3.3 日志等级

```kotlin
enum class LogLevel(val priority: Int) {
    VERBOSE(2),
    DEBUG(3),
    INFO(4),
    WARN(5),
    ERROR(6);

    companion object {
        fun fromPriority(priority: Int): LogLevel =
            entries.firstOrNull { it.priority == priority } ?: WARN
    }
}
```

**各构建类型缺省等级**：

| 构建类型 | 缺省日志等级 | 可修改 | 说明 |
|----------|-------------|--------|------|
| debug | `VERBOSE` | 否 | 开发阶段需要全量日志 |
| release | `WARN` | 是 | 仅记录警告和错误，用户可在设置中调低等级以协助排查问题 |

Release 构建的日志等级作为配置项存储在 DataStore 中，与 `ImeConfig.UiConfig` 集成（完整定义见文档 160 第 4.2 节）：

- `logLevel: LogLevel = LogLevel.WARN` — 发布版本的日志等级。仅对 release 构建生效，debug 构建始终为 VERBOSE
- `logStoragePath: String? = null` — 日志文件存放目录路径。null 表示使用缺省应用私有目录

### 3.4 AppLog 门面

```kotlin
/**
 * 应用日志系统门面。
 *
 * 初始化时根据构建类型设置缺省日志等级，并安装崩溃拦截器。
 * 所有日志操作通过 [logger] 获取的 [AppLogger] 实例执行。
 */
object AppLog {
    private val writers = mutableListOf<LogWriter>()
    private var crashInterceptor: CrashInterceptor? = null

    /** 当前生效的日志等级 */
    var level: LogLevel = if (BuildConfig.DEBUG) LogLevel.VERBOSE else LogLevel.WARN
        private set

    fun init(context: Context, config: ImeConfig) {
        // 设置日志等级
        level = if (BuildConfig.DEBUG) LogLevel.VERBOSE else config.ui.logLevel

        // 添加 Logcat 输出（仅 debug 构建）
        if (BuildConfig.DEBUG) {
            writers += LogcatWriter()
        }

        // 添加文件输出
        val storage = LogStorage(context, config.ui.logStoragePath)
        writers += FileLogWriter(storage)

        // 安装崩溃拦截器
        crashInterceptor = CrashInterceptor(writers, storage)
        crashInterceptor?.install()
    }

    /** 更新日志等级（发布版本通过设置界面调用） */
    fun updateLevel(newLevel: LogLevel) {
        level = newLevel
    }

    /** 更新日志存储路径 */
    fun updateStoragePath(path: String?) {
        val fileWriter = writers.filterIsInstance<FileLogWriter>().firstOrNull()
        fileWriter?.updateStoragePath(path)
    }

    /** 获取带标签的 Logger 实例 */
    fun logger(tag: String): AppLogger = AppLogger(tag, this)

    /** 获取带类名标签的 Logger 实例 */
    fun logger(cls: Class<*>): AppLogger = logger(cls.simpleName)

    /** 内部：分发日志到所有 Writer */
    internal fun dispatch(entry: LogEntry) {
        if (entry.level.priority < level.priority) return

        writers.forEach { writer ->
            writer.write(entry)
        }
    }

    /** 内部：刷新所有 Writer */
    internal suspend fun flush() {
        writers.forEach { it.flush() }
    }
}
```

### 3.5 AppLogger

```kotlin
/**
 * 带标签的日志记录器。
 *
 * 支持 Kotlin 惯用语法和树形日志块。
 * 使用 inline + lambda 避免在不满足日志等级时评估消息参数。
 */
class AppLogger(private val tag: String, private val appLog: AppLog) {

    inline fun verbose(msg: () -> String) {
        if (appLog.level <= LogLevel.VERBOSE) {
            appLog.dispatch(LogEntry(LogLevel.VERBOSE, tag, msg()))
        }
    }

    inline fun debug(msg: () -> String) {
        if (appLog.level <= LogLevel.DEBUG) {
            appLog.dispatch(LogEntry(LogLevel.DEBUG, tag, msg()))
        }
    }

    inline fun info(msg: () -> String) {
        if (appLog.level <= LogLevel.INFO) {
            appLog.dispatch(LogEntry(LogLevel.INFO, tag, msg()))
        }
    }

    inline fun warn(msg: () -> String) {
        if (appLog.level <= LogLevel.WARN) {
            appLog.dispatch(LogEntry(LogLevel.WARN, tag, msg()))
        }
    }

    inline fun error(msg: () -> String) {
        if (appLog.level <= LogLevel.ERROR) {
            appLog.dispatch(LogEntry(LogLevel.ERROR, tag, msg()))
        }
    }

    inline fun error(throwable: Throwable, msg: () -> String) {
        if (appLog.level <= LogLevel.ERROR) {
            appLog.dispatch(LogEntry(LogLevel.ERROR, tag, msg(), throwable))
        }
    }

    /**
     * 开始树形日志块。
     *
     * 在 [block] 执行期间，所有通过当前 logger 记录的日志
     * 将作为此树形块的子节点输出，形成嵌套结构。
     */
    inline fun tree(title: String, block: () -> Unit) {
        val treeWriter = TreeLogWriter(appLog, tag, title)
        treeWriter.begin()
        try {
            block()
        } finally {
            treeWriter.end()
        }
    }
}
```

**使用示例**：

```kotlin
class PinyinKeyboard(private val dict: PinyinDict) {
    private val log = AppLog.logger(PinyinKeyboard::class)

    fun handleInput(char: Char) {
        log.tree("处理拼音输入: $char") {
            log.debug { "查询拼音候选: $char" }
            val candidates = dict.lookup(char.toString())
            log.info { "找到 ${candidates.size} 个候选" }
        }
    }
}
```

### 3.6 日志条目

```kotlin
/**
 * 单条日志记录，不可变。
 */
data class LogEntry(
    val level: LogLevel,
    val tag: String,
    val message: String,
    val throwable: Throwable? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val threadName: String = Thread.currentThread().name,
    val threadId: Long = Thread.currentThread().id,
) {
    /** 格式化为可读字符串 */
    fun format(): String {
        val time = Instant.fromEpochMilliseconds(timestamp)
            .toLocalDateTime(TimeZone.currentSystemDefault())
            .format(DateTimeFormatter("yyyy-MM-dd HH:mm:ss.SSS"))
        val throwableStr = throwable?.stackTraceToString()?.let { "\n$it" } ?: ""
        return "$time [${level.name}] [$tag] [$threadName] $message$throwableStr"
    }
}
```

### 3.7 LogWriter 接口

```kotlin
interface LogWriter {
    fun write(entry: LogEntry)
    suspend fun flush()
}

/**
 * Logcat 输出。仅 debug 构建使用。
 */
class LogcatWriter : LogWriter {
    override fun write(entry: LogEntry) {
        when (entry.level) {
            LogLevel.VERBOSE -> Log.v(entry.tag, entry.message, entry.throwable)
            LogLevel.DEBUG -> Log.d(entry.tag, entry.message, entry.throwable)
            LogLevel.INFO -> Log.i(entry.tag, entry.message, entry.throwable)
            LogLevel.WARN -> Log.w(entry.tag, entry.message, entry.throwable)
            LogLevel.ERROR -> Log.e(entry.tag, entry.message, entry.throwable)
        }
    }

    override suspend fun flush() {
        // Logcat 无需刷新
    }
}
```

### 3.8 FileLogWriter

```kotlin
/**
 * 文件日志输出。
 *
 * 使用协程 Channel 缓冲日志条目，独立协程批量写入文件，
 * 避免阻塞调用线程。文件按日期滚动，超过上限自动清理。
 */
class FileLogWriter(private val storage: LogStorage) : LogWriter {
    private val channel = Channel<LogEntry>(capacity = Channel.BUFFERED)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val flushLatch = CompletableDeferred<Unit>()

    init {
        scope.launch {
            val buffer = mutableListOf<LogEntry>()
            while (true) {
                // 收集一批日志（最多 100 条或等待 2 秒）
                val entry = channel.receive()
                buffer.add(entry)

                while (buffer.size < 100) {
                    val polled = channel.tryReceive().getOrNull() ?: break
                    buffer.add(polled)
                }

                // 批量写入
                storage.appendEntries(buffer)
                buffer.clear()

                // 检查是否需要刷新信号
                if (channel.isEmpty) {
                    flushLatch.complete(Unit)
                }
            }
        }
    }

    override fun write(entry: LogEntry) {
        channel.trySend(entry)
    }

    override suspend fun flush() {
        while (!channel.isEmpty) {
            delay(50)
        }
    }

    fun updateStoragePath(path: String?) {
        storage.updatePath(path)
    }
}
```

### 3.9 LogStorage

```kotlin
/**
 * 日志文件存储管理。
 *
 * 文件组织：
 *   {logDir}/
 *   ├── kuaizi_ime_2026-05-12.log   ← 当天日志
 *   ├── kuaizi_ime_2026-05-11.log   ← 前一天日志
 *   └── ...
 *
 * 文件滚动策略：每天一个文件，保留最近 7 天，单文件最大 5MB。
 * 存储路径可通过配置指定，缺省为应用私有目录下的 logs/ 子目录。
 */
class LogStorage(
    context: Context,
    customPath: String? = null,
) {
    private var logDir: File = resolveLogDir(context, customPath)

    companion object {
        const val MAX_FILE_SIZE_BYTES = 5L * 1024 * 1024 // 5MB
        const val MAX_RETENTION_DAYS = 7
        const val FILE_NAME_PREFIX = "kuaizi_ime_"
        const val FILE_NAME_SUFFIX = ".log"

        private val dateFormat = DateTimeFormatter("yyyy-MM-dd")

        private fun resolveLogDir(context: Context, customPath: String?): File {
            if (customPath != null) {
                val dir = File(customPath)
                if (dir.isDirectory || dir.mkdirs()) {
                    return dir
                }
            }
            return File(context.filesDir, "logs")
        }
    }

    fun appendEntries(entries: List<LogEntry>) {
        val todayFile = todayFile()
        // 检查文件大小，超过上限则滚动
        if (todayFile.exists() && todayFile.length() > MAX_FILE_SIZE_BYTES) {
            rotateFile(todayFile)
        }

        todayFile.appendText(entries.joinToString("\n") { it.format() } + "\n")
        cleanupOldFiles()
    }

    fun readLogs(
        date: LocalDate? = null,
        levelFilter: LogLevel? = null,
        keyword: String? = null,
    ): List<LogEntry> {
        val file = if (date != null) fileForDate(date) else todayFile()
        if (!file.exists()) return emptyList()

        return file.readLines()
            .mapNotNull { parseLine(it) }
            .filter { levelFilter == null || it.level.priority >= levelFilter.priority }
            .filter { keyword == null || it.message.contains(keyword, ignoreCase = true) }
    }

    /** 导出指定日期范围的日志为单个文件 */
    fun exportLogs(
        destination: File,
        fromDate: LocalDate,
        toDate: LocalDate,
    ) {
        val lines = mutableListOf<String>()
        var date = fromDate
        while (date <= toDate) {
            val file = fileForDate(date)
            if (file.exists()) {
                lines += "= ${file.name} ="
                lines += file.readLines()
                lines += ""
            }
            date = date.plusDays(1)
        }
        destination.writeText(lines.joinToString("\n"))
    }

    fun updatePath(customPath: String?) {
        logDir = resolveLogDir(/* 需要 context */, customPath)
    }

    private fun todayFile(): File = fileForDate(Clock.System.todayIn(TimeZone.currentSystemDefault()))

    private fun fileForDate(date: LocalDate): File =
        File(logDir, "$FILE_NAME_PREFIX${dateFormat.format(date)}$FILE_NAME_SUFFIX")

    private fun rotateFile(file: File) {
        val rotated = File(file.parent, file.nameWithoutExtension + "_rotated_${System.currentTimeMillis()}.log")
        file.renameTo(rotated)
    }

    private fun cleanupOldFiles() {
        val cutoff = Clock.System.todayIn(TimeZone.currentSystemDefault())
            .minusDays(MAX_RETENTION_DAYS.toLong())
        logDir.listFiles()
            ?.filter { it.name.startsWith(FILE_NAME_PREFIX) && it.name.endsWith(FILE_NAME_SUFFIX) }
            ?.filter { extractDateFromFileName(it.name) < cutoff }
            ?.forEach { it.delete() }
    }

    private fun extractDateFromFileName(name: String): LocalDate? = runCatching {
        val dateStr = name.removePrefix(FILE_NAME_PREFIX).removeSuffix(FILE_NAME_SUFFIX)
        LocalDate.parse(dateStr)
    }.getOrNull()
}
```

---

## 4. 崩溃拦截

### 4.1 CrashInterceptor

```kotlin
/**
 * 崩溃拦截器。
 *
 * 安装为 UncaughtExceptionHandler，在应用崩溃时：
 * 1. 记录完整异常信息到日志文件（通过 FileLogWriter 直接写入）
 * 2. 刷新日志缓冲区，确保所有待写入日志落盘
 * 3. 调用原始 Handler 让系统正常处理崩溃（显示对话框、退出进程）
 */
class CrashInterceptor(
    private val writers: List<LogWriter>,
    private val storage: LogStorage,
) {
    private var defaultHandler: Thread.UncaughtExceptionHandler? = null

    fun install() {
        defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            handleCrash(thread, throwable)
        }
    }

    private fun handleCrash(thread: Thread, throwable: Throwable) {
        val entry = LogEntry(
            level = LogLevel.ERROR,
            tag = "Crash",
            message = "未捕获异常 [${thread.name}]",
            throwable = throwable,
        )

        // 直接写入文件，绕过 Channel 缓冲
        storage.appendEntries(listOf(entry))

        // 同步刷新所有 Writer
        runBlocking {
            writers.forEach { it.flush() }
        }

        // 交由系统默认处理器处理
        defaultHandler?.uncaughtException(thread, throwable)
    }
}
```

### 4.2 崩溃日志内容

崩溃时记录的日志条目包含：

| 字段 | 内容 |
|------|------|
| level | `ERROR` |
| tag | `Crash` |
| message | `未捕获异常 [线程名]` |
| throwable | 完整异常对象（含堆栈） |
| timestamp | 崩溃时刻 |
| threadName | 崩溃线程名 |
| threadId | 崩溃线程 ID |

格式化后的崩溃日志示例：

```
2026-05-12 14:32:15.891 [ERROR] [Crash] [main] 未捕获异常 [main]
java.lang.NullPointerException: InputConnection is null
    at org.crazydan.studio.app.ime.kuaizi.IMEService.commitText(IMEService.kt:156)
    at org.crazydan.studio.app.ime.kuaizi.ui.KeyboardViewModel.reduce(KeyboardViewModel.kt:89)
    at org.crazydan.studio.app.ime.kuaizi.ui.KeyboardViewModel.handleIntent(KeyboardViewModel.kt:45)
    ...
```

---

## 5. 日志查看界面

### 5.1 入口

日志查看界面通过设置页入口进入。在设置页增加「日志与诊断」分组：

```
设置
├── 外观
│   ├── 主题模式
│   └── 手模式
├── 输入
│   ├── 启用 X-Pad
│   └── 繁体优先
├── 日志与诊断          ← 新增分组
│   ├── 日志等级        ← 仅 release 构建可修改
│   ├── 查看日志        ← 进入日志浏览界面
│   ├── 导出日志        ← 触发系统文件选择器
│   └── 日志存储路径    ← 配置存储位置
├── 隐私
│   └── 禁用输入数据记录
└── 反馈
    └── ...
```

### 5.2 日志浏览界面

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

### 5.3 LogViewerViewModel

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

### 5.4 日志导出

日志导出与文档 800（用户数据导入导出）中的导出机制协同：

```kotlin
/**
 * 导出日志到用户指定位置。
 *
 * 通过 Activity Result API 的 CreateDocument 合约，
 * 让用户选择保存位置，默认文件名包含日期范围。
 */
class LogExportActivity : ComponentActivity() {
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

日志导出也集成到文档 800 的统一导出界面中，作为独立导出项：

| 导出项 | 格式 | 包含内容 |
|--------|------|----------|
| 用户数据 | JSON | 输入频率、收藏、配置 |
| 诊断日志 | TXT | 指定日期范围的日志文件 |

---

## 6. 日志等级管理

### 6.1 等级切换界面

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

### 6.2 等级变更流程

```
用户在设置中选择日志等级
    ↓
ConfigRepository.updateConfig { it.copy(ui = it.ui.copy(logLevel = newLevel)) }
    ↓
AppLog.updateLevel(newLevel)
    ↓
后续日志按新等级过滤
```

等级变更立即生效，不需要重启应用。DataStore 异步持久化确保配置在应用重启后恢复。

---

## 7. 日志存储路径配置

### 7.1 路径选择

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

### 7.2 路径解析优先级

1. 用户配置路径（通过 SAF 选择的 URI）→ 优先使用
2. 应用私有目录 `{filesDir}/logs/` → 缺省路径
3. 配置路径无效（目录不存在且无法创建）→ 降级到缺省路径并记录警告日志

---

## 8. 各层日志使用规范

### 8.1 层级与标签约定

| 层 | 标签前缀 | 典型内容 |
|----|----------|----------|
| Platform | `IME#` | InputConnection 操作、生命周期、窗口状态 |
| ViewModel | `VM#` | Intent 处理、状态转换、协程异常 |
| Domain | `KB#` / `IL#` / `Dict#` | 键盘状态机、输入列表操作、字典查询 |
| Data | `DB#` / `DS#` | 数据库操作、DataStore 读写 |
| 日志系统 | `Crash` | 未捕获异常 |

### 8.2 日志等级使用指南

| 等级 | 使用场景 | 示例 |
|------|----------|------|
| VERBOSE | 细粒度流程追踪 | 键盘状态机每次转换、RecyclerView 绑定 |
| DEBUG | 开发期调试信息 | 候选词查询结果、按键事件参数 |
| INFO | 关键业务节点 | 输入提交、键盘切换、字典加载完成 |
| WARN | 可恢复的异常情况 | 字典查询超时降级、配置项缺失使用默认值 |
| ERROR | 不可恢复错误 | 数据库损坏、InputConnection 丢失、崩溃异常 |

### 8.3 性能注意事项

```kotlin
// ✅ 推荐：使用 inline + lambda 延迟求值
log.debug { "查询结果: ${candidates.map { it.text }.joinToString()}" }

// ❌ 禁止：直接拼接字符串（即使不满足等级也会执行拼接）
log.debug("查询结果: ${candidates.map { it.text }.joinToString()}")
```

---

## 9. Java 功能完整对照

| Java 日志功能 | v4 对应 | 改进说明 |
|-------------|---------|---------|
| `Logger.getLogger()` | `AppLog.logger()` | 支持所有构建类型，不再 release 返回 noop |
| `Logger.debug/info/warn/error` | `AppLogger.verbose/debug/info/warn/error` | 新增 VERBOSE 级别，inline lambda 延迟求值 |
| `Logger.beginTreeLog/endTreeLog` | `AppLogger.tree { }` | 作用域函数替代手动 begin/end，避免遗漏 end |
| `Logger.noop` | 日志等级过滤 | 不再创建 noop 实例，统一由等级控制 |
| `LogCache` 内存缓存 | `LogStorage` 文件持久化 | 崩溃后日志不丢失，可按日期、等级、关键词检索 |
| 无崩溃拦截 | `CrashInterceptor` | 捕获未处理异常，记录完整堆栈 |
| 无日志查看 | `LogViewerScreen` | Compose 界面浏览日志，按等级着色 |
| 无日志导出 | `LogExportActivity` + 统一导出界面 | 系统文件选择器导出，支持日期范围选择 |
| 无等级配置 | `ImeConfig.UiConfig.logLevel` | release 构建可通过设置修改 |
| 无路径配置 | `ImeConfig.UiConfig.logStoragePath` | 可指定存储路径，缺省应用私有目录 |
| `TreeLog` ThreadLocal + synchronized | Kotlin 协程 Channel | 无线程安全风险，无死锁隐患 |
