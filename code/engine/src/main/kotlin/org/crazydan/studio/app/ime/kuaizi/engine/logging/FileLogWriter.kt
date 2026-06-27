package org.crazydan.studio.app.ime.kuaizi.engine.logging

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel

class FileLogWriter(private val storage: LogStorage) : LogWriter {
    private val channel = Channel<LogEntry>(capacity = Channel.BUFFERED)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        scope.launch {
            val buffer = mutableListOf<LogEntry>()
            while (true) {
                val entry = channel.receive()
                buffer.add(entry)

                while (buffer.size < 100) {
                    val polled = channel.tryReceive().getOrNull() ?: break
                    buffer.add(polled)
                }

                storage.appendEntries(buffer)
                buffer.clear()
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
