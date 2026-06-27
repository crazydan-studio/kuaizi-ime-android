package org.crazydan.studio.app.ime.kuaizi.engine.logging

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel

class FileLogWriter(
    private val storage: LogStorage,
    bufferSize: Int = Channel.BUFFERED,
) : LogWriter {
    private val channel = Channel<LogEntry>(bufferSize)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        scope.launch {
            while (true) {
                val batch = mutableListOf<LogEntry>()
                batch.add(channel.receive())
                while (true) {
                    val entry = channel.tryReceive().getOrNull() ?: break
                    batch.add(entry)
                    if (batch.size >= 100) break
                }
                storage.appendEntries(batch)
            }
        }
    }

    override fun write(entry: LogEntry) {
        channel.trySend(entry)
    }

    override fun flush() {
        runBlocking {
            while (!channel.isEmpty) {
                delay(50)
            }
        }
    }
}
