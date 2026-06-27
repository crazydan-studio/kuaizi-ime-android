package org.crazydan.studio.app.ime.kuaizi.engine.logging

interface LogWriter {
    fun write(entry: LogEntry)
    fun flush()
}

class LogcatWriter(
    private val bufferSize: Int = 0,
) : LogWriter {
    private val channel: java.util.concurrent.BlockingQueue<LogEntry>? =
        if (bufferSize > 0) java.util.concurrent.LinkedBlockingQueue(bufferSize) else null

    init {
        if (channel != null) {
            Thread {
                while (true) {
                    val entry = channel.take()
                    android.util.Log.println(entry.level.priority, entry.tag, entry.message)
                }
            }.apply { isDaemon = true }.start()
        }
    }

    override fun write(entry: LogEntry) {
        val ch = channel
        if (ch != null) {
            ch.offer(entry)
        } else {
            android.util.Log.println(entry.level.priority, entry.tag, entry.message)
        }
    }

    override fun flush() {
        // no-op for Logcat
    }
}

class CrashInterceptor(
    private val writers: List<LogWriter> = emptyList(),
    private val storage: LogStorage? = null,
) {
    private var defaultHandler: Thread.UncaughtExceptionHandler? = null

    fun install() {
        defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            storage?.appendEntries(
                listOf(
                    LogEntry(
                        level = LogLevel.ERROR,
                        tag = "Crash",
                        message = "Uncaught exception on ${thread.name}",
                        throwable = throwable,
                    )
                )
            )
            writers.forEach { it.flush() }
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }
}
