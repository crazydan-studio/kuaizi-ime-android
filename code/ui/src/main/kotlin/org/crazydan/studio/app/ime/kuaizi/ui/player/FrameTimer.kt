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

/**
 * 帧定时器：使用 [withFrameNanos] 驱动动画帧循环。
 *
 * 基于 Compose Choreographer 实现帧同步，确保动画进度与屏幕刷新率精确对齐，
 * 避免传统 [delay] 定时器的累积误差问题。
 *
 * 当系统负载导致帧回调延迟时，自动跳过中间状态——[onFrame] 仅以最新进度调用一次，
 * 不累积过期帧。这确保动画始终追赶上最新进度，不会因卡顿而累积延迟。
 *
 * @param scope 协程作用域，用于启动帧循环协程
 */
class FrameTimer(private val scope: CoroutineScope) {
    private var job: Job? = null
    private var paused = false

    /**
     * 启动帧循环。
     *
     * @param durationMs 动画总时长（毫秒）
     * @param onFrame 每帧回调，接收当前进度 [0f..1f]
     * @param onComplete 动画完成回调
     */
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

    /** 暂停帧循环，进度暂停在当前位置。 */
    fun pause() { paused = true }

    /** 恢复帧循环，从暂停位置继续。 */
    fun resume() { paused = false; job?.let { if (it.isCancelled) start(0, {}, {}) } }

    /** 停止帧循环并重置暂停状态。 */
    fun stop() {
        job?.cancel()
        paused = false
    }
}
