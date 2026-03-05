/*
 * SPDX-FileCopyrightText: 2015 - 2025 Rime community
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

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

package org.crazydan.studio.ime.libtrime.impl

import com.osfans.trime.core.Rime
import com.osfans.trime.core.RimeDispatcher
import com.osfans.trime.core.RimeLifecycle
import com.osfans.trime.core.RimeLifecycleOwner
import com.osfans.trime.core.RimeLifecycleRegistry
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.withContext
import org.crazydan.studio.ime.libtrime.Trime
import org.crazydan.studio.ime.libtrime.TrimeMessage
import timber.log.Timber
import java.io.File

/**
 * source from [trime/Rime.kt](https://github.com/osfans/trime/blob/develop/app/src/main/java/com/osfans/trime/core/Rime.kt)
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2026-03-05
 */
class TrimeImpl : Trime, Trime.Config, RimeLifecycleOwner {
    companion object {
        private val messageFlow_ =
            MutableSharedFlow<TrimeMessage<*>>(
                extraBufferCapacity = 15,
                onBufferOverflow = BufferOverflow.DROP_OLDEST,
            )
    }

    // ---------------------------------- Configuration --------------------------------------
    override lateinit var userDataDir: File
    override lateinit var sharedDataDir: File

    // ----------------------------------- Message -------------------------------------
    override val messageFlow = messageFlow_.asSharedFlow()

    // ------------------------------------- Lifecycle -----------------------------------
    override val lifecycle by lazy { RimeLifecycleRegistry() }

    override val isReady: Boolean
        get() = lifecycle.currentState == RimeLifecycle.State.READY

    init {
        if (lifecycle.currentState != RimeLifecycle.State.STOPPED) {
            throw IllegalStateException("Rime has already been created!")
        }
    }

    // ------------------------------------- Dispatcher -----------------------------------
    private val dispatcher = RimeDispatcher(
        object : RimeDispatcher.RimeController {
            override fun nativeStartup() {
                startRime(false)

                lifecycle.emitState(RimeLifecycle.State.READY)
            }

            override fun nativeFinalize() {
                Rime.exitRime()
            }
        }
    )

    /** 确保所有 Rime 操作**均在同一线程中**按**顺序执行**，避免并发问题 */
    private suspend inline fun <T> withRimeContext(crossinline block: suspend () -> T): T = withContext(dispatcher) {
        block()
    }

    // ------------------------------------------------------------------------

    fun getUserDataDir() = if (this::userDataDir.isInitialized) userDataDir else null
    fun getSharedDataDir() = if (this::sharedDataDir.isInitialized) sharedDataDir else null

    // ------------------------------------------------------------------------

    override suspend fun rebuild() = withRimeContext {
        Rime.exitRime()
        startRime(true)
    }

    // ------------------------------------------------------------------------

    fun start() {
        if (lifecycle.currentState != RimeLifecycle.State.STOPPED) {
            Timber.Forest.w("Skip starting Trime: not at stopped state!")
            return
        }

        Rime.registerRimeMessageHandler(this::handleRimeMessage)

        lifecycle.emitState(RimeLifecycle.State.STARTING)
        dispatcher.start()
    }

    fun stop() {
        if (lifecycle.currentState != RimeLifecycle.State.READY) {
            Timber.Forest.w("Skip stopping Trime: not at ready state!")
            return
        }

        Timber.Forest.i("Trime finalize()")
        lifecycle.emitState(RimeLifecycle.State.STOPPING)
        dispatcher.stop().let {
            if (it.isNotEmpty()) {
                Timber.Forest.w("${it.size} job(s) didn't get a chance to run!")
            }
        }
        lifecycle.emitState(RimeLifecycle.State.STOPPED)

        Rime.unregisterRimeMessageHandler(this::handleRimeMessage)
    }

    // ------------------------------------------------------------------------

    private fun startRime(
        /**
         * - 为 `true` 时强制执行数据文件的完整维护，适合首次部署或重大更新
         * - 为 `false` 时执行数据文件的智能维护，只在检测到变更时才重建，提高效率
         */
        fullCheck: Boolean,
    ) {
        requireNotNull(getUserDataDir()) {
            "Rime userDataDir isn't specified," +
                    " please initialize it in TrimeSession.config first"
        }
        requireNotNull(getSharedDataDir()) {
            "Rime sharedDataDir isn't specified," +
                    " please initialize it in TrimeSession.config first"
        }

        Timber.Forest.d(
            """
            Starting Rime with:
            userDataDir: ${userDataDir.absolutePath}
            sharedDataDir: ${sharedDataDir.absolutePath}
            fullCheck: $fullCheck
            """.trimIndent(),
        )

        Rime.startupRime(
            sharedDir = sharedDataDir.absolutePath,
            userDir = userDataDir.absolutePath,
            fullCheck = fullCheck,
        )
    }

    private fun handleRimeMessage(type: Int, params: Array<Any>) {
        // TODO
    }
}