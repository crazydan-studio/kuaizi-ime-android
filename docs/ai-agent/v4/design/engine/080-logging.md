# 日志系统

v4 版本设计完整的日志基础设施，作为 `:ime-engine` 引擎库的核心组件，支持日志分级输出、文件持久化与滚动管理、树形日志块、异步批量写入、Android Logcat 输出和崩溃拦截。日志基础设施独立设计的目标是使其与 UI 和应用层解耦，方便第三方按需使用。引擎库提供日志核心基础设施，同时提供 `LogcatWriter`（Android Logcat 输出）和 `CrashInterceptor`（崩溃拦截）作为直接可用的工具类，避免每个消费者重复实现。日志查看、导出、配置等 UI 功能划归 `:app` 模块，详见 [020-日志系统](../app/020-logging.md)。

**模块归属**：日志的核心基础设施（`ImeLog`、`ImeLogger`、`LogLevel`、`LogEntry`、`LogWriter`、`LogStorage`、`FileLogWriter`）和直接可用的工具类（`LogcatWriter`、`CrashInterceptor`）划归 `:ime-engine` 模块。日志相关的界面（`LogViewerScreen`、`LogExportScreen`、`LogLevelSetting`、`LogStoragePathSetting`、`LogViewerViewModel`）划归 `:app` 模块。

---

## 1. 架构总览

日志系统采用分层设计：核心层定义平台无关的抽象和实现，引擎库同时提供 Android 平台常用的工具类（`LogcatWriter`、`CrashInterceptor`），应用层负责初始化和 UI 集成。引擎库通过 `ImeLog` 门面提供统一的日志 API，所有日志操作通过 `ImeLogger` 实例执行。日志条目经 `LogWriter` 接口输出到不同目标，`FileLogWriter` 是引擎内置的文件持久化实现，`LogcatWriter` 是引擎提供的 Android Logcat 输出实现。

```plantuml
@file:../diagrams/engine-logging.puml
```

**设计原则**：

1. **核心与UI/应用分离**：`:ime-engine` 中的核心日志组件（`ImeLog`、`ImeLogger`、`LogLevel`、`LogEntry`、`LogWriter`、`LogStorage`、`FileLogWriter`、`CrashInterceptor`）与 UI 和应用层解耦，第三方可以按需集成
2. **工具类即用**：引擎库提供 `LogcatWriter`（Android Logcat 输出）和 `CrashInterceptor`（JVM 崩溃拦截）作为直接可用的工具类，消费者无需重复实现，按需使用即可
3. **Writer 可扩展**：通过 `LogWriter` 接口，应用层和第三方可以注册自定义输出目标（Logcat、远程服务器、遥测系统等）
4. **等级可配置**：日志等级由外部注入，引擎不内置构建类型判断，应用层根据 `BuildConfig.DEBUG` 或 `ImeConfig` 设置初始等级
5. **性能优先**：使用 `inline` + `lambda` 延迟求值，`Channel` 缓冲 + 独立协程批量写入，不阻塞调用线程

---

## 2. 日志等级

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

`LogLevel` 是平台无关的枚举，定义五个日志等级，按优先级递增。日志等级用于两个层面：一是全局过滤，`ImeLog` 只分发优先级不低于当前等级的日志条目；二是运行时配置，应用层可以通过 `ImeLog.updateLevel()` 动态调整过滤阈值。

**各构建类型缺省等级**：

| 构建类型 | 缺省日志等级 | 可修改 | 说明 |
|----------|-------------|--------|------|
| debug | `VERBOSE` | 否 | 开发阶段需要全量日志，等级在应用层初始化时锁定 |
| release | `WARN` | 是 | 仅记录警告和错误，用户可在设置中调低等级以协助排查问题 |

Release 构建的日志等级作为配置项存储在 DataStore 中，与 `ImeConfig.UiConfig` 集成：

- `logLevel: LogLevel = LogLevel.WARN` — 发布版本的日志等级。仅对 release 构建生效，debug 构建始终为 VERBOSE
- `logStoragePath: String? = null` — 日志文件存放目录路径。null 表示使用缺省应用私有目录

`LogLevel` 本身不包含构建类型判断逻辑，该逻辑由应用层在初始化 `ImeLog` 时负责，确保引擎库的平台无关性。

---

## 3. 日志条目（LogEntry）

```kotlin
/**
 * 单条日志记录，不可变。
 *
 * 包含日志的所有上下文信息：等级、标签、消息、异常、
 * 时间戳、线程名和线程 ID。
 * 通过 format() 方法格式化为可读字符串用于文件输出。
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

`LogEntry` 是不可变的日志条目数据类，每次日志调用创建一个新实例。所有字段在构造时确定，不存在可变状态。`format()` 方法将日志条目格式化为标准的可读字符串，包含时间戳、等级、标签、线程名和消息体，异常对象追加在消息之后。此格式用于文件输出和日志导出，确保日志文件的可读性和可解析性。线程信息的记录对于异步场景下的日志排查尤为重要，能够快速定位日志产生的协程或线程上下文。

---

## 4. LogWriter 接口

```kotlin
/**
 * 日志输出目标接口。
 *
 * 引擎库内置 FileLogWriter 实现，同时提供
 * LogcatWriter 等 Android 平台工具类。
 * 所有 Writer 通过 ImeLog 注册，由 ImeLog.dispatch() 统一分发。
 */
interface LogWriter {
    /** 写入一条日志。实现必须保证线程安全 */
    fun write(entry: LogEntry)

    /** 刷新缓冲区，确保所有待写入日志落盘 */
    suspend fun flush()
}
```

`LogWriter` 是日志输出目标的抽象接口，采用策略模式实现输出目标的可扩展性。引擎库内置 `FileLogWriter` 用于文件持久化，引擎库提供 `LogcatWriter` 用于 Android Logcat 输出。第三方应用可以实现此接口，将日志输出到远程服务器、遥测系统或任何自定义目标。

所有 `LogWriter` 实现必须在 `write()` 方法中保证线程安全，因为日志可能从不同协程或线程同时写入。`flush()` 方法用于确保所有缓冲日志落盘，在应用崩溃或主动导出时调用。实现可以选择同步写入（如 `LogcatWriter`）或异步缓冲写入（如 `FileLogWriter`），接口不做限制。

---

## 5. ImeLogger

```kotlin
/**
 * 带标签的日志记录器。
 *
 * 支持 Kotlin 惯用语法和树形日志块。
 * 使用 inline + lambda 避免在不满足日志等级时评估消息参数。
 */
class ImeLogger(private val tag: String, private val log: ImeLog) {

    inline fun verbose(msg: () -> String) {
        if (log.level <= LogLevel.VERBOSE) {
            log.dispatch(LogEntry(LogLevel.VERBOSE, tag, msg()))
        }
    }

    inline fun debug(msg: () -> String) {
        if (log.level <= LogLevel.DEBUG) {
            log.dispatch(LogEntry(LogLevel.DEBUG, tag, msg()))
        }
    }

    inline fun info(msg: () -> String) {
        if (log.level <= LogLevel.INFO) {
            log.dispatch(LogEntry(LogLevel.INFO, tag, msg()))
        }
    }

    inline fun warn(msg: () -> String) {
        if (log.level <= LogLevel.WARN) {
            log.dispatch(LogEntry(LogLevel.WARN, tag, msg()))
        }
    }

    inline fun error(msg: () -> String) {
        if (log.level <= LogLevel.ERROR) {
            log.dispatch(LogEntry(LogLevel.ERROR, tag, msg()))
        }
    }

    inline fun error(throwable: Throwable, msg: () -> String) {
        if (log.level <= LogLevel.ERROR) {
            log.dispatch(LogEntry(LogLevel.ERROR, tag, msg(), throwable))
        }
    }

    /**
     * 开始树形日志块。
     *
     * 在 [block] 执行期间，所有通过当前 logger 记录的日志
     * 将作为此树形块的子节点输出，形成嵌套结构。
     */
    inline fun tree(title: String, block: () -> Unit) {
        val treeWriter = TreeLogWriter(log, tag, title)
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
    private val logger = ImeLog.logger(PinyinKeyboard::class)

    fun handleInput(char: Char) {
        logger.tree("处理拼音输入: $char") {
            logger.debug { "查询拼音候选: $char" }
            val candidates = dict.lookup(char.toString())
            logger.info { "找到 ${candidates.size} 个候选" }
        }
    }
}
```

`ImeLogger` 是面向调用者的日志记录器，每个模块或类通过 `ImeLog.logger(tag)` 获取带标签的实例。所有日志方法使用 `inline` + `lambda` 延迟求值模式，确保在日志等级不满足时不会执行消息参数的字符串拼接和对象格式化，避免性能浪费。`tree()` 方法支持树形日志块，在调试复杂流程时提供结构化的日志输出，块内所有日志作为子节点嵌套显示，帮助理解执行流程的层次关系。`error()` 方法提供两种重载：一种仅记录消息，另一种同时记录异常对象，异常的完整堆栈信息将被包含在日志条目中。

---

## 6. ImeLog 门面

```kotlin
/**
 * 日志系统门面。
 *
 * 平台无关的日志基础设施核心，不依赖 Android 框架。
 * 所有日志操作通过 [logger] 获取的 [ImeLogger] 实例执行。
 *
 * 初始化时由应用层注入日志等级和 Writer 列表，
 * 引擎不内置构建类型判断或平台特有逻辑。
 */
object ImeLog {
    private val writers = mutableListOf<LogWriter>()

    /** 当前生效的日志等级 */
    var level: LogLevel = LogLevel.WARN
        private set

    /**
     * 初始化日志系统。
     *
     * @param level 初始日志等级（由应用层根据构建类型决定）
     * @param writers 日志输出目标列表（应用层注册平台特有 Writer）
     */
    fun init(level: LogLevel, writers: List<LogWriter>) {
        this.level = level
        this.writers.clear()
        this.writers.addAll(writers)
    }

    /** 更新日志等级（发布版本通过设置界面调用） */
    fun updateLevel(newLevel: LogLevel) {
        level = newLevel
    }

    /** 获取带标签的 Logger 实例 */
    fun logger(tag: String): ImeLogger = ImeLogger(tag, this)

    /** 获取带类名标签的 Logger 实例 */
    fun logger(cls: Class<*>): ImeLogger = logger(cls.simpleName)

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

`ImeLog` 是日志系统的全局门面，采用 Kotlin `object` 单例模式，确保整个应用中只有一个日志系统实例。`init()` 方法接受日志等级和 `LogWriter` 列表，由应用层负责创建和注入平台特有的组件。这种设计使日志基础设施与 UI 和应用层解耦，方便第三方按需集成。

**应用层初始化示例**（详见 [020-日志系统](../app/020-logging.md)）：

```kotlin
// :app 模块中的初始化
class ImeApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        val level = if (BuildConfig.DEBUG) LogLevel.VERBOSE
                    else config.ui.logLevel

        val writers = buildList {
            if (BuildConfig.DEBUG) {
                add(LogcatWriter())
            }
            add(FileLogWriter(LogStorage(resolveLogDir())))
        }

        ImeLog.init(level, writers)
    }
}
```

`updateLevel()` 方法允许运行时动态调整日志等级，典型的使用场景是用户在设置页面修改日志等级后立即生效，无需重启应用。`dispatch()` 方法在分发前检查日志等级，低于当前等级的日志直接丢弃，避免不必要的 Writer 调用和 `LogEntry` 对象创建（`ImeLogger` 的 `inline` + `lambda` 模式在调用侧即已过滤，`dispatch()` 中的检查作为二次保障）。`flush()` 方法在应用崩溃或导出日志时调用，确保所有缓冲日志落盘。

---

## 7. LogStorage

```kotlin
/**
 * 日志文件存储管理。
 *
 * 纯 Kotlin 实现，不依赖 Android Context。
 * 通过构造参数接收已解析的日志目录，由应用层负责解析路径。
 *
 * 文件组织：
 *   {logDir}/
 *   ├── kuaizi_ime_2026-05-12.log   ← 当天日志
 *   ├── kuaizi_ime_2026-05-11.log   ← 前一天日志
 *   └── ...
 *
 * 文件滚动策略：每天一个文件，保留最近 7 天，单文件最大 5MB。
 */
class LogStorage(
    logDir: File,
) {
    private var logDir: File = logDir

    companion object {
        const val MAX_FILE_SIZE_BYTES = 5L * 1024 * 1024 // 5MB
        const val MAX_RETENTION_DAYS = 7
        const val FILE_NAME_PREFIX = "kuaizi_ime_"
        const val FILE_NAME_SUFFIX = ".log"

        private val dateFormat = DateTimeFormatter("yyyy-MM-dd")
    }

    /** 更新日志目录（应用层切换存储路径时调用） */
    fun updateDir(logDir: File) {
        this.logDir = logDir
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
            ?.filter { extractDateFromFileName(it.name) != null && extractDateFromFileName(it.name)!! < cutoff }
            ?.forEach { it.delete() }
    }

    private fun extractDateFromFileName(name: String): LocalDate? = runCatching {
        val dateStr = name.removePrefix(FILE_NAME_PREFIX).removeSuffix(FILE_NAME_SUFFIX)
        LocalDate.parse(dateStr)
    }.getOrNull()

    private fun parseLine(line: String): LogEntry? {
        // 解析 format() 输出的日志行
        // 格式：yyyy-MM-dd HH:mm:ss.SSS [LEVEL] [TAG] [ThreadName] message
        return runCatching {
            val regex = Regex("""(\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}\.\d{3}) \[(\w+)] \[(\w+)] \[(\w+)] (.+)""")
            val match = regex.matchEntire(line) ?: return null
            LogEntry(
                level = LogLevel.valueOf(match.groupValues[2]),
                tag = match.groupValues[3],
                message = match.groupValues[5],
                timestamp = LocalDateTime.parse(match.groupValues[1])
                    .toInstant(TimeZone.currentSystemDefault())
                    .toEpochMilliseconds(),
                threadName = match.groupValues[4],
            )
        }.getOrNull()
    }
}
```

`LogStorage` 是纯 Kotlin 实现的日志文件管理器，不依赖 Android `Context`，构造时直接接收已解析的 `File` 日志目录。路径解析（从 `Context.filesDir` 或 SAF URI 到 `File`）由应用层负责，引擎库只关心文件的读写操作。这种设计确保了引擎的平台无关性：在 Android 应用中，应用层将 `context.filesDir.resolve("logs")` 传入；在纯 JVM 测试环境中，可以传入任意临时目录。

日志文件按日期组织，每天一个文件，命名格式为 `kuaizi_ime_YYYY-MM-DD.log`。单文件超过 5MB 时自动滚动（重命名添加时间戳后缀），保留最近 7 天的日志文件，超期自动清理。`readLogs()` 方法支持按等级和关键词过滤，供应用层的日志查看界面使用。`exportLogs()` 方法将指定日期范围的日志合并输出到单个文件，供应用层的日志导出功能使用。

---

## 8. FileLogWriter

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
}
```

`FileLogWriter` 是引擎内置的文件日志输出实现，采用协程 `Channel` 缓冲 + 独立协程批量写入的策略，确保日志写入不阻塞调用线程。日志条目通过 `trySend()` 非阻塞地发送到 `Channel`，独立协程从 `Channel` 中批量收集（最多 100 条）后一次性写入文件，减少 I/O 操作次数。`flush()` 方法等待 `Channel` 清空，在应用崩溃或日志导出时调用，确保所有缓冲日志落盘。

`FileLogWriter` 持有 `LogStorage` 实例的引用，但自身不管理文件路径。当应用层切换日志存储路径时，通过 `LogStorage.updateDir()` 更新目录，`FileLogWriter` 无需感知路径变更。这种职责分离确保了写入逻辑和存储管理的独立性。

---

## 9. 日志标签与等级约定

### 9.1 层级与标签约定

| 层 | 标签前缀 | 典型内容 |
|----|----------|----------|
| Platform | `IME#` | InputConnection 操作、生命周期、窗口状态 |
| ViewModel | `VM#` | Intent 处理、状态转换、协程异常 |
| Domain | `KB#` / `IL#` / `Dict#` | 键盘状态机、输入列表操作、字典查询 |
| Data | `DB#` / `DS#` | 数据库操作、DataStore 读写 |
| 日志系统 | `Crash` | 未捕获异常 |

标签前缀使得日志在查看和搜索时可以快速定位来源层级。`ImeLog.logger()` 方法支持通过字符串标签或 `Class` 对象创建 `ImeLogger`，推荐在类中使用 `ImeLog.logger(ClassName::class)` 的形式，确保标签与类名一致。

### 9.2 日志等级使用指南

| 等级 | 使用场景 | 示例 |
|------|----------|------|
| VERBOSE | 细粒度流程追踪 | 键盘状态机每次转换、RecyclerView 绑定 |
| DEBUG | 开发期调试信息 | 候选词查询结果、按键事件参数 |
| INFO | 关键业务节点 | 输入提交、键盘切换、字典加载完成 |
| WARN | 可恢复的异常情况 | 字典查询超时降级、配置项缺失使用默认值 |
| ERROR | 不可恢复错误 | 数据库损坏、InputConnection 丢失、崩溃异常 |

### 9.3 性能注意事项

```kotlin
// ✅ 推荐：使用 inline + lambda 延迟求值
logger.debug { "查询结果: ${candidates.map { it.text }.joinToString()}" }

// ❌ 禁止：直接拼接字符串（即使不满足等级也会执行拼接）
logger.debug("查询结果: ${candidates.map { it.text }.joinToString()}")
```

所有 `ImeLogger` 方法使用 `inline` + `lambda` 延迟求值模式。当日志等级不满足时，`lambda` 不会执行，避免了字符串拼接、对象格式化等不必要的计算开销。在频繁调用的路径（如按键处理、状态机转换）中，应特别注意使用 `lambda` 形式，避免在日志等级为 WARN 的 release 构建中产生隐藏的性能损耗。

---

## 10. 与应用层的集成接口

引擎库的日志系统通过依赖注入与应用层集成，引擎不持有任何平台引用。应用层的集成职责包括：

| 集成点 | 应用层职责 | 引擎提供 |
|--------|-----------|---------|
| 初始化 | 创建 `LogWriter` 列表，调用 `ImeLog.init()` | `ImeLog` 门面 |
| 日志等级 | 根据 `BuildConfig.DEBUG` 设置初始等级 | `LogLevel` 枚举 |
| 存储路径 | 解析 `Context.filesDir` 或 SAF URI 为 `File`，创建 `LogStorage` | `LogStorage(File)` 构造 |
| Logcat 输出 | 注册引擎提供的 `LogcatWriter` | `LogcatWriter` 工具类 |
| 崩溃拦截 | 安装引擎提供的 `CrashInterceptor` | `CrashInterceptor` 工具类 |
| 等级变更 | 从 `ConfigDataStore` 同步到 `ImeLog.updateLevel()` | `ImeLog.updateLevel()` |
| 路径变更 | 从 `ConfigDataStore` 同步到 `LogStorage.updateDir()` | `LogStorage.updateDir()` |

这种分层设计确保了日志核心与 UI/应用层的分离：在 Android 应用中，应用层使用引擎提供的 `LogcatWriter` 和 `CrashInterceptor` 工具类，只需一行代码即可完成平台集成；仅需要文件日志的场景可以只使用 `FileLogWriter` + `LogStorage`。第三方应用可以完全跳过 `LogcatWriter` 和 `CrashInterceptor`，仅使用引擎内置的文件日志能力。

---

## 11. LogcatWriter

```kotlin
/**
 * Logcat 输出。
 *
 * 将 ImeLog 的日志条目映射到 Android Logcat 的对应方法，
 * 在 Android Studio 的 Logcat 面板中实时查看。
 *
 * 由引擎库提供，位于 :ime-engine 的 Android 特定源集
 * （src/androidMain/kotlin），供 Android 消费者直接使用。
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
        // Logcat 无需刷新，日志即时输出
    }
}
```

`LogcatWriter` 将引擎的 `LogEntry` 映射到 `android.util.Log` 的对应方法，使日志在 Android Studio 的 Logcat 面板中实时可见。由于 `LogcatWriter` 依赖 Android 框架的 `android.util.Log` 类，它位于引擎库的 Android 特定源集（`src/androidMain/kotlin`）中，而非纯 Kotlin 公共源集。这种源集分离确保了引擎核心的平台无关性：仅引入纯 Kotlin 公共源集的消费者不会受到 Android 依赖的影响，而 Android 消费者可以直接使用 `LogcatWriter`，无需自行实现。

`LogcatWriter` 的 `flush()` 方法为空实现，因为 Android Logcat 是即时输出通道，不存在缓冲区。这与 `FileLogWriter` 的异步缓冲写入形成对比：`FileLogWriter` 通过 `Channel` 缓冲日志条目，需要 `flush()` 确保落盘；`LogcatWriter` 直接调用 `Log.v/d/i/w/e`，日志立即出现在 Logcat 面板中。通常仅在 debug 构建中注册 `LogcatWriter`，release 构建不包含此 Writer，确保发布版本不会向 Logcat 输出敏感信息。

---

## 12. CrashInterceptor

```kotlin
/**
 * 崩溃拦截器。
 *
 * 安装为 UncaughtExceptionHandler，在应用崩溃时：
 * 1. 记录完整异常信息到日志文件（通过 LogStorage 直接写入）
 * 2. 刷新日志缓冲区，确保所有待写入日志落盘
 * 3. 调用原始 Handler 让系统正常处理崩溃（显示对话框、退出进程）
 *
 * 由引擎库提供，位于 :ime-engine 的纯 Kotlin 公共源集
 * （src/commonMain/kotlin），因为其使用的
 * Thread.UncaughtExceptionHandler 是 JVM 标准 API。
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

**崩溃时记录的日志条目**：

| 字段 | 内容 |
|------|------|
| level | `ERROR` |
| tag | `Crash` |
| message | `未捕获异常 [线程名]` |
| throwable | 完整异常对象（含堆栈） |
| timestamp | 崩溃时刻 |
| threadName | 崩溃线程名 |
| threadId | 崩溃线程 ID |

`CrashInterceptor` 是引擎库提供的崩溃防护工具类，安装为 JVM 的 `UncaughtExceptionHandler`。由于 `Thread.UncaughtExceptionHandler` 是 JVM 标准 API（并非 Android 特有），`CrashInterceptor` 位于引擎库的纯 Kotlin 公共源集中，可以在任何 Kotlin/JVM 环境中使用，不依赖 Android 框架。

崩溃发生时，`CrashInterceptor` 绕过 `FileLogWriter` 的 `Channel` 缓冲，直接通过 `LogStorage.appendEntries()` 同步写入文件，确保崩溃信息不会丢失在缓冲区中。随后同步刷新所有 Writer 的缓冲区，将崩溃前尚未落盘的日志全部写入文件。最后调用系统默认的异常处理器，让平台正常处理崩溃（Android 系统会显示崩溃对话框并退出进程）。

将 `CrashInterceptor` 放在引擎库中而非应用层的原因在于：崩溃拦截是日志系统的核心能力之一，每个使用引擎库的 Android 应用都需要此功能。如果放在应用层，每个消费者都需要重复实现相同的逻辑——记录异常、刷新缓冲、委托系统处理。将其作为引擎库提供的工具类，消费者只需调用 `CrashInterceptor(writers, storage).install()` 即可完成安装，无需关心实现细节。
