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

package com.osfans.trime.core

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.CoroutineContext

// source from https://github.com/osfans/trime/blob/develop/app/src/main/java/com/osfans/trime/core/RimeDispatcher.kt

/**
 * RimeDispatcher is a wrapper of a single-threaded executor that runs RimeController.
 * It provides a coroutine-based interface for dispatching jobs to the executor.
 * It also provides a stop() method to gracefully stop the executor and return the remaining jobs.
 *
 * Adapted from [fcitx5-android/FcitxDispatcher.kt](https://github.com/fcitx5-android/fcitx5-android/blob/364afb44dcf0d9e3db3d43a21a32601b2190cbdf/app/src/main/java/org/fcitx/fcitx5/android/core/FcitxDispatcher.kt).
 */
class RimeDispatcher(
    private val controller: RimeController,
) : CoroutineDispatcher() {

    interface RimeController {
        fun nativeStartup()

        fun nativeFinalize()
    }

    class WrappedRunnable(
        private val runnable: Runnable,
        private val name: String? = null,
    ) : Runnable by runnable {
        private val time = System.currentTimeMillis()
        var started = false
            private set

        private val delta
            get() = System.currentTimeMillis() - time

        override fun run() {
            if (delta > JOB_WAITING_LIMIT) {
                Timber.Forest.w("${toString()} has waited $delta ms to get run since created!")
            }

            started = true
            runnable.run()
        }

        override fun toString(): String = "WrappedRunnable[${name ?: hashCode()}]"

        companion object {
            val Empty = WrappedRunnable({}, "Empty")
        }
    }

    companion object {
        private const val JOB_WAITING_LIMIT = 2000L // ms
    }

    private val internalDispatcher =
        Executors
            .newSingleThreadExecutor {
                Thread(it, "rime-main")
            }.asCoroutineDispatcher()

    private val internalScope = CoroutineScope(internalDispatcher)

    private val mutex = Mutex()

    private val queue = LinkedBlockingQueue<WrappedRunnable>()

    private val isRunning = AtomicBoolean(false)

    /**
     * Start the dispatcher
     * This function returns immediately
     */
    fun start() {
        Timber.Forest.d("RimeDispatcher start()")
        internalScope.launch {
            mutex.withLock {
                if (isRunning.compareAndSet(false, true)) {
                    Timber.Forest.d("nativeStartup()")
                    controller.nativeStartup()

                    while (isActive && isRunning.get()) {
                        val block = queue.take()
                        block.run()
                    }

                    Timber.Forest.i("nativeFinalize()")
                    controller.nativeFinalize()
                }
            }
        }
    }

    /**
     * Stop the dispatcher
     * This function blocks until fully stopped
     */
    fun stop(): List<Runnable> {
        Timber.Forest.i("RimeDispatcher stop()")

        return if (isRunning.compareAndSet(true, false)) {
            runBlocking {
                queue.offer(WrappedRunnable.Empty)

                mutex.withLock {
                    val rest = mutableListOf<WrappedRunnable>()
                    queue.drainTo(rest)
                    rest
                }
            }
        } else {
            emptyList()
        }
    }

    override fun dispatch(
        context: CoroutineContext,
        block: Runnable,
    ) {
        if (!isRunning.get()) {
            throw IllegalStateException("Dispatcher is not in running state!")
        }

        queue.offer(WrappedRunnable(block))
    }
}