# 日志系统

## 1. 日志架构概述

`:engine` 模块的日志系统采用三层架构设计：**门面层**（`ImeLog`）提供全局统一的日志 API 入口；**记录器层**（`ImeLogger`）提供带标签的日志记录能力和树形日志块支持；**写入器层**（`LogWriter` 实现）负责将日志条目输出到不同的目标（文件、Logcat、远程服务器等）。三层之间通过清晰的接口契约协作，每一层只依赖下一层的抽象接口，不依赖具体实现，实现了日志系统的可扩展性和可替换性。

门面层的核心职责是管理日志等级和 `LogWriter` 注册表，所有日志操作通过 `ImeLog` 门面分发到已注册的 `LogWriter` 实例。记录器层的核心职责是为每个模块或类提供带标签的日志记录器，支持 `inline` + `lambda` 延迟求值模式和树形日志块。写入器层的核心职责是将 `LogEntry` 输出到具体目标，引擎内置 `FileLogWriter`（异步文件写入），同时提供 `LogcatWriter`（Android Logcat 输出）和 `CrashInterceptor`（JVM 崩溃拦截）作为直接可用的工具类。

```plantuml
@file:../diagrams/engine-logging.puml
```

三层架构的设计原则：**核心与平台分离**，`ImeLog`、`ImeLogger`、`LogLevel`、`LogEntry`、`LogWriter`、`LogStorage`、`FileLogWriter` 位于纯 Kotlin 公共源集，不依赖 Android 框架；`LogcatWriter` 和 `CrashInterceptor` 虽涉及平台 API，但作为可选工具类提供，消费者按需使用。**Writer 可扩展**，通过 `LogWriter` 接口，应用层和第三方可以注册自定义输出目标。**性能优先**，使用 `inline` + `lambda` 延迟求值，`Channel` 缓冲 + 独立协程批量写入，不阻塞调用线程。

---

## 2. LogLevel 日志等级

`LogLevel` 是平台无关的枚举类型，定义五个日志等级，按优先级递增排列。日志等级用于两个层面：一是全局过滤，`ImeLog` 只分发优先级不低于当前等级的日志条目；二是运行时配置，应用层可以通过 `ImeLog.updateLevel()` 动态调整过滤阈值。`priority` 字段为整数映射，支持通过 `fromPriority()` 方法从整数优先级反向查找对应的 `LogLevel` 实例，该能力在集成 Android Logcat 等使用整数优先级的日志系统时尤为有用。

```kotlin
enum class LogLevel(val priority: Int) {
    VERBOSE(2),
    DEBUG(3),
    INFO(4),
    WARN(5),
    ERROR(6);

    companion object {
        /** 根据优先级整数值查找对应的 LogLevel，未匹配时返回 WARN */
        fun fromPriority(priority: Int): LogLevel =
            entries.firstOrNull { it.priority == priority } ?: WARN
    }
}
```

五个等级的语义划分遵循业界通用的日志分级约定：`VERBOSE` 用于细粒度流程追踪（如键盘状态机每次转换、输入列表每次变更），`DEBUG` 用于开发期调试信息（如候选词查询结果、按键事件参数），`INFO` 用于关键业务节点（如输入提交、键盘切换、字典加载完成），`WARN` 用于可恢复的异常情况（如字典查询超时降级、配置项缺失使用默认值），`ERROR` 用于不可恢复错误（如数据库损坏、`InputConnection` 丢失、崩溃异常）。

---

## 3. LogEntry 日志条目

`LogEntry` 是不可变的日志条目数据类，每次日志调用创建一个新实例，所有字段在构造时确定，不存在可变状态。日志条目包含完整的上下文信息：等级、标签、消息、异常对象、时间戳、线程名和线程 ID。

```kotlin
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

`format()` 方法输出的字符串格式为 `yyyy-MM-dd HH:mm:ss.SSS [LEVEL] [TAG] [ThreadName] message`，异常对象的完整堆栈信息追加在消息之后。此格式兼顾了可读性和可解析性——人工阅读时可以快速定位时间、等级、标签和消息，程序解析时可以通过正则表达式提取结构化字段。

---

## 4. LogWriter 写入接口

`LogWriter` 是日志输出目标的抽象接口，采用策略模式实现输出目标的可扩展性。引擎内置 `FileLogWriter` 用于文件持久化，引擎提供 `LogcatWriter` 用于 Android Logcat 输出。第三方应用可以实现此接口，将日志输出到远程服务器、遥测系统或任何自定义目标。所有 `LogWriter` 实现通过 `ImeLog.init()` 注册，由 `ImeLog.dispatch()` 统一分发。

```kotlin
interface LogWriter {
    /** 写入一条日志。实现必须保证线程安全 */
    fun write(entry: LogEntry)

    /** 刷新缓冲区，确保所有待写入日志落盘 */
    suspend fun flush()
}
```

`write()` 方法接收一个 `LogEntry` 实例，实现必须保证线程安全——日志可能从不同协程或线程同时写入，实现需要使用适当的同步机制（如 `Channel` 缓冲、锁或原子操作）避免数据竞争。`flush()` 方法用于确保所有缓冲日志落盘，在应用崩溃或主动导出时调用。实现可以选择同步写入（如 `LogcatWriter` 直接调用 `android.util.Log` 方法）或异步缓冲写入（如 `FileLogWriter` 通过 `Channel` 缓冲后批量写入），接口不做限制。

---

## 5. ImeLog 门面

`ImeLog` 是日志系统的全局门面，采用 Kotlin `object` 单例模式，确保整个应用中只有一个日志系统实例。`ImeLog` 的核心职责包括：管理当前生效的日志等级、维护 `LogWriter` 注册表、分发日志条目到所有已注册的 `LogWriter`、提供 `ImeLogger` 实例的工厂方法。所有日志操作通过 `ImeLog.logger()` 获取的 `ImeLogger` 实例执行，`ImeLog` 本身不提供直接的日志记录方法。

```kotlin
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
    fun logger(cls: KClass<*>): ImeLogger = logger(cls.simpleName ?: "Unknown")

    /** 内部：分发日志到所有 Writer */
    internal fun dispatch(entry: LogEntry) {
        if (entry.level.priority < level.priority) return
        writers.forEach { writer -> writer.write(entry) }
    }

    /** 内部：刷新所有 Writer */
    internal suspend fun flush() {
        writers.forEach { it.flush() }
    }
}
```

`init()` 方法接受日志等级和 `LogWriter` 列表，由应用层负责创建和注入平台特有的组件。这种设计使日志基础设施与 UI 和应用层解耦，方便第三方按需集成。`init()` 方法在应用启动时调用一次，后续通过 `updateLevel()` 动态调整日志等级。`dispatch()` 方法在分发前检查日志等级，低于当前等级的日志直接丢弃。

`logger()` 方法支持两种标签创建方式：字符串标签和类引用标签。推荐使用类引用形式 `ImeLog.logger(ClassName::class)`，确保标签与类名一致，便于日志搜索和过滤。`dispatch()` 和 `flush()` 方法标记为 `internal`，仅对 `ImeLogger` 和 `CrashInterceptor` 可见，外部代码不应直接调用。

---

## 6. ImeLogger 带标签记录器

`ImeLogger` 是面向调用者的日志记录器，每个模块或类通过 `ImeLog.logger(tag)` 获取带标签的实例。所有日志方法使用 `inline` + `lambda` 延迟求值模式，确保在日志等级不满足时不会执行消息参数的字符串拼接和对象格式化，避免性能浪费。`tree()` 方法支持树形日志块，在调试复杂流程时提供结构化的日志输出。

```kotlin
class ImeLogger(private val tag: String, private val log: ImeLog) {

    inline fun verbose(msg: () -> String) {
        if (log.level.priority <= LogLevel.VERBOSE.priority) {
            log.dispatch(LogEntry(LogLevel.VERBOSE, tag, msg()))
        }
    }

    inline fun debug(msg: () -> String) {
        if (log.level.priority <= LogLevel.DEBUG.priority) {
            log.dispatch(LogEntry(LogLevel.DEBUG, tag, msg()))
        }
    }

    inline fun info(msg: () -> String) {
        if (log.level.priority <= LogLevel.INFO.priority) {
            log.dispatch(LogEntry(LogLevel.INFO, tag, msg()))
        }
    }

    inline fun warn(msg: () -> String) {
        if (log.level.priority <= LogLevel.WARN.priority) {
            log.dispatch(LogEntry(LogLevel.WARN, tag, msg()))
        }
    }

    inline fun error(msg: () -> String) {
        if (log.level.priority <= LogLevel.ERROR.priority) {
            log.dispatch(LogEntry(LogLevel.ERROR, tag, msg()))
        }
    }

    inline fun error(throwable: Throwable, msg: () -> String) {
        if (log.level.priority <= LogLevel.ERROR.priority) {
            log.dispatch(LogEntry(LogLevel.ERROR, tag, msg(), throwable))
        }
    }

    /**
     * 开始树形日志块。
     *
     * 在 block 执行期间，所有通过当前 logger 记录的日志
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

### 6.1 inline + lambda 延迟求值

所有日志方法使用 `inline` + `lambda` 延迟求值模式，这是高性能日志系统的核心优化。当日志等级不满足时，`lambda` 不会执行，避免了字符串拼接、对象格式化等不必要的计算开销。在频繁调用的路径（如按键处理、状态机转换）中，这一优化尤为关键——即使日志等级为 `WARN`，每帧数十次的 `debug()` 调用也不会产生任何性能损耗，因为 `lambda` 体内的字符串拼接代码根本不会执行。

### 6.2 树形日志块

`tree()` 方法支持树形日志块，在调试复杂流程时提供结构化的日志输出。块内所有日志作为子节点嵌套显示，帮助理解执行流程的层次关系。`tree()` 方法内部创建 `TreeLogWriter` 临时拦截日志输出，在块结束时将收集的日志按树形结构格式化后统一输出。

---

## 7. LogStorage 文件存储

`LogStorage` 是纯 Kotlin 实现的日志文件管理器，不依赖 Android `Context`，构造时直接接收已解析的 `File` 日志目录。路径解析由应用层负责，引擎库只关心文件的读写操作。日志文件按日期组织，每天一个文件，命名格式为 `kuaizi_ime_YYYY-MM-DD.log`。单文件超过 5MB 时自动滚动（重命名添加时间戳后缀），保留最近 7 天的日志文件，超期自动清理。

```kotlin
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

    /**
     * 追加日志条目到当天文件。
     * 超过大小上限自动滚动，超期文件自动清理。
     */
    fun appendEntries(entries: List<LogEntry>) {
        val todayFile = todayFile()
        if (todayFile.exists() && todayFile.length() > MAX_FILE_SIZE_BYTES) {
            rotateFile(todayFile)
        }
        todayFile.appendText(entries.joinToString("\n") { it.format() } + "\n")
        cleanupOldFiles()
    }

    /**
     * 读取日志。
     * 支持按日期、等级和关键词过滤。
     */
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

    private fun todayFile(): File =
        fileForDate(Clock.System.todayIn(TimeZone.currentSystemDefault()))

    private fun fileForDate(date: LocalDate): File =
        File(logDir, "$FILE_NAME_PREFIX${dateFormat.format(date)}$FILE_NAME_SUFFIX")

    private fun rotateFile(file: File) {
        val rotated = File(
            file.parent,
            file.nameWithoutExtension + "_rotated_${System.currentTimeMillis()}.log",
        )
        file.renameTo(rotated)
    }

    private fun cleanupOldFiles() {
        val cutoff = Clock.System.todayIn(TimeZone.currentSystemDefault())
            .minusDays(MAX_RETENTION_DAYS.toLong())
        logDir.listFiles()
            ?.filter { it.name.startsWith(FILE_NAME_PREFIX) && it.name.endsWith(FILE_NAME_SUFFIX) }
            ?.filter { extractDateFromFileName(it.name)?.let { d -> d < cutoff } == true }
            ?.forEach { it.delete() }
    }

    private fun extractDateFromFileName(name: String): LocalDate? = runCatching {
        val dateStr = name.removePrefix(FILE_NAME_PREFIX).removeSuffix(FILE_NAME_SUFFIX)
        LocalDate.parse(dateStr)
    }.getOrNull()

    private fun parseLine(line: String): LogEntry? {
        return runCatching {
            val regex = Regex(
                """(\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}\.\d{3}) \[(\w+)] \[(\w+)] \[(\w+)] (.+)"""
            )
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

`LogStorage` 的平台无关设计确保了引擎的可测试性和可移植性。在 Android 应用中，应用层将 `context.filesDir.resolve("logs")` 传入；在纯 JVM 测试环境中，可以传入任意临时目录。

---

## 8. FileLogWriter 异步文件写入

`FileLogWriter` 是引擎内置的文件日志输出实现，采用协程 `Channel` 缓冲 + 独立协程批量写入的策略，确保日志写入不阻塞调用线程。日志条目通过 `trySend()` 非阻塞地发送到 `Channel`，独立协程从 `Channel` 中批量收集（最多 100 条）后一次性写入文件，减少 I/O 操作次数。

```kotlin
class FileLogWriter(private val storage: LogStorage) : LogWriter {
    private val channel = Channel<LogEntry>(capacity = Channel.BUFFERED)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        scope.launch {
            val buffer = mutableListOf<LogEntry>()
            while (true) {
                // 阻塞等待第一条日志
                val entry = channel.receive()
                buffer.add(entry)

                // 非阻塞收集更多日志，最多 100 条
                while (buffer.size < 100) {
                    val polled = channel.tryReceive().getOrNull() ?: break
                    buffer.add(polled)
                }

                // 批量写入文件
                storage.appendEntries(buffer)
                buffer.clear()
            }
        }
    }

    override fun write(entry: LogEntry) {
        channel.trySend(entry)
    }

    override suspend fun flush() {
        // 等待 Channel 中的所有条目被消费完毕
        while (!channel.isEmpty) {
            delay(50)
        }
    }
}
```

### 8.1 Channel 缓冲与批量写入

`Channel<LogEntry>(capacity = Channel.BUFFERED)` 创建一个具有默认缓冲容量的 `Channel`，`trySend()` 方法在缓冲区未满时立即返回，不会阻塞调用线程。独立协程使用 `receive()` 阻塞等待第一条日志，然后通过 `tryReceive()` 非阻塞地收集更多日志，直到收集满 100 条或 `Channel` 为空。批量写入通过 `storage.appendEntries(buffer)` 一次性完成，将 100 条日志格式化为字符串后追加到文件末尾，仅需一次文件 I/O 操作。

这种「阻塞等待首条 + 非阻塞收集余量」的策略在低流量和高流量场景下都表现出色：低流量时，`receive()` 确保日志及时写入（不会因等待凑批而延迟）；高流量时，批量收集减少 I/O 次数（100 条日志合并为一次写入），提升吞吐量。

### 8.2 flush 语义与崩溃安全

`flush()` 方法等待 `Channel` 清空，确保所有缓冲日志落盘。实现方式为轮询 `channel.isEmpty` 并延迟 50ms，直到 `Channel` 中所有条目被独立协程消费完毕。`flush()` 在应用正常关闭或日志导出时调用。对于崩溃场景，`CrashInterceptor` 绕过 `FileLogWriter` 的 `Channel` 缓冲，直接通过 `LogStorage.appendEntries()` 同步写入崩溃信息，确保崩溃日志不会丢失在缓冲区中。

---

## 9. LogcatWriter Android Logcat 输出

`LogcatWriter` 将引擎的 `LogEntry` 映射到 `android.util.Log` 的对应方法，使日志在 Android Studio 的 Logcat 面板中实时可见。LogcatWriter 默认为同步写入（bufferSize=0），直接调用 `android.util.Log.println()`。可选的小型 Channel 缓冲（如 bufferSize=16）将 Logcat 输出异步化，避免热路径中的同步 IPC 开销。缓冲满时通过 `trySend` 静默丢弃，确保写入者永不阻塞。

```kotlin
class LogcatWriter(
    private val bufferSize: Int = 0, // 0 = 同步写入（默认）
) : LogWriter {
    private val channel: Channel<LogEntry>? =
        if (bufferSize > 0) Channel(bufferSize) else null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    init {
        channel?.let { ch ->
            scope.launch {
                for (entry in ch) {
                    android.util.Log.println(entry.level.priority, entry.tag, entry.message)
                }
            }
        }
    }

    override fun write(entry: LogEntry) {
        val ch = channel
        if (ch != null) {
            ch.trySend(entry) // 非阻塞，满则丢弃
        } else {
            android.util.Log.println(entry.level.priority, entry.tag, entry.message)
        }
    }

    override fun flush() {
        channel?.let {
            while (!it.isEmpty) {
                Thread.sleep(10)
            }
        }
    }
}
```

通常仅在 Debug 构建中注册 `LogcatWriter`，Release 构建不包含此 Writer，确保发布版本不会向 Logcat 输出敏感信息。

---

## 10. CrashInterceptor 崩溃拦截

`CrashInterceptor` 是引擎库提供的崩溃防护工具类，安装为 JVM 的 `Thread.UncaughtExceptionHandler`，在应用崩溃时执行三个关键操作：记录完整异常信息到日志文件、刷新所有 `LogWriter` 的缓冲区确保待写入日志落盘、委托系统默认处理器正常处理崩溃。

```kotlin
class CrashInterceptor(
    private val writers: List<LogWriter>,
    private val storage: LogStorage,
) {
    private var defaultHandler: Thread.UncaughtExceptionHandler? = null

    /** 安装崩溃拦截器 */
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

### 10.1 崩溃时的日志写入策略

崩溃发生时，`CrashInterceptor` 绕过 `FileLogWriter` 的 `Channel` 缓冲机制，直接通过 `LogStorage.appendEntries()` 同步写入崩溃信息。这是因为在崩溃场景下，进程即将终止，`FileLogWriter` 的独立协程可能来不及消费 `Channel` 中的缓冲日志——直接同步写入确保崩溃信息不会丢失。写入完成后，`CrashInterceptor` 通过 `runBlocking` 同步刷新所有 `LogWriter` 的缓冲区，将崩溃前尚未落盘的日志全部写入文件。

### 10.2 崩溃日志的标签约定

崩溃日志使用固定的标签 `"Crash"`，消息格式为 `"未捕获异常 [线程名]"`，异常对象包含完整的堆栈信息。这种一致的标签和消息格式使得崩溃日志在日志查看和搜索时可以快速定位——搜索标签 `"Crash"` 即可找到所有崩溃记录，消息中的线程名帮助区分主线程崩溃和工作线程崩溃。

### 10.3 初始化流程

日志系统的完整初始化流程如下：

```kotlin
// 1. 应用层创建日志存储和 Writer
val logDir = context.filesDir.resolve("logs")
val storage = LogStorage(logDir)
val fileWriter = FileLogWriter(storage)
val writers = mutableListOf<LogWriter>(fileWriter)

// Debug 构建时添加 LogcatWriter
if (BuildConfig.DEBUG) {
    writers.add(LogcatWriter())
}

// 2. 初始化 ImeLog 门面
ImeLog.init(
    level = if (BuildConfig.DEBUG) LogLevel.VERBOSE else LogLevel.WARN,
    writers = writers,
)

// 3. 安装崩溃拦截器
CrashInterceptor(writers, storage).install()

// 4. 获取 Logger 并使用
val logger = ImeLog.logger("MyApp")
logger.info { "应用启动完成" }
```

崩溃拦截器不自动安装，由应用层在合适的时机显式调用 `install()`，保持调用方对崩溃处理流程的完全控制。
