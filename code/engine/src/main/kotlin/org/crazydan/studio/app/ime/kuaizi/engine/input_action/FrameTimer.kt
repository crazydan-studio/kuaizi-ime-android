package org.crazydan.studio.app.ime.kuaizi.engine.input_action

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose

class FrameTimer(private val scope: CoroutineScope) {
    private var job: Job? = null
    private var paused = false

    fun start(
        durationMs: Long,
        onFrame: (progress: Float) -> Unit,
        onComplete: () -> Unit,
    ) {
        job?.cancel()
        paused = false
        job = scope.launch {
            val startNanos = System.nanoTime()
            val durationNanos = durationMs * 1_000_000L
            val frameNanos = 16_666_667L

            while (true) {
                withFrameNanos { frameTimeNanos ->
                    if (paused) return@withFrameNanos
                    val elapsed = frameTimeNanos - startNanos
                    val progress = (elapsed.toFloat() / durationNanos).coerceIn(0f, 1f)
                    onFrame(progress)
                    if (progress >= 1f) {
                        onComplete()
                        return@withFrameNanos
                    }
                }
            }
        }
    }

    fun pause() { paused = true }
    fun resume() { paused = false; job?.let { if (it.isCancelled) start(0, {}, {}) } }

    fun stop() {
        job?.cancel()
        paused = false
    }
}
