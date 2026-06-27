package org.crazydan.studio.app.ime.kuaizi.engine.logging

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import org.crazydan.studio.app.ime.kuaizi.engine.LogLevel

interface LogWriter {
    fun write(entry: LogEntry)
    suspend fun flush()
}

class LogcatWriter(
    private val bufferSize: Int = 0,
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
            ch.trySend(entry)
        } else {
            android.util.Log.println(entry.level.priority, entry.tag, entry.message)
        }
    }

    override suspend fun flush() {
        channel?.let {
            while (!it.isEmpty) {
                delay(10)
            }
        }
    }
}

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

        runBlocking {
            storage.appendEntries(listOf(entry))
            writers.forEach { it.flush() }
        }

        defaultHandler?.uncaughtException(thread, throwable)
    }
}
