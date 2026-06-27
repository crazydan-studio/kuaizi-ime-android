/*
 * 筷字输入法 - 高效编辑需要又好又快的输入法
 * Copyright (C) 2026 Crazydan Studio <https://studio.crazydan.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.
 * If not, see <https://www.gnu.org/licenses/lgpl-3.0.en.html#license-text>.
 */

package org.crazydan.studio.app.ime.kuaizi.ui.player

import androidx.compose.runtime.withFrameNanos
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

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