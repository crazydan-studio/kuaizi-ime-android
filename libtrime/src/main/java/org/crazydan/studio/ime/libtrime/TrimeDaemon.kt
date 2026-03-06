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

package org.crazydan.studio.ime.libtrime

import com.osfans.trime.core.RimeLifecycle
import com.osfans.trime.core.lifecycleScope
import com.osfans.trime.core.whenReady
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.crazydan.studio.ime.libtrime.TrimeDaemon.trimeImpl
import org.crazydan.studio.ime.libtrime.impl.TrimeImpl
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Trime 守护进程，用于管理单例 [Trime] 和已创建的 [TrimeSession]。
 * 当所有 [TrimeSession] 全部被销毁后，[TrimeDaemon] 将自动关闭 [Trime]。
 *
 * 在调用 [TrimeSession] 的 `run`/`runXxx`/`launchXxx` 函数之前必须先通过
 * [TrimeSession.config] 配置 Trime:
 * ```kotlin
 * val rime = RimeDaemon.createSession(javaClass.name)
 * rime.config {
 *     userDataDir = File(...)
 *     sharedDataDir = File(...)
 * }
 * ```
 *
 * source from [trime/RimeDaemon.kt](https://github.com/osfans/trime/blob/develop/app/src/main/java/com/osfans/trime/daemon/RimeDaemon.kt)
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2026-03-04
 */
object TrimeDaemon {
    /** 通过委托模式将 [trimeImpl] 封装为 [Trime] 接口，避免上层直接访问 [org.crazydan.studio.ime.libtrime.impl.TrimeImpl] 上的函数 */
    private val trimeInf by lazy { object : Trime by trimeImpl {} }
    private val trimeCnf by lazy { object : Trime.Config by trimeImpl {} }

    private val trimeImpl by lazy { TrimeImpl() }

    private val lock = ReentrantLock()
    private val sessions = mutableMapOf<String, TrimeSession>()

    /** 按指定名字创建 [TrimeSession] 会话。若绑定的会话已创建，则直接返回 */
    fun createSession(name: String): TrimeSession = lock.withLock {
        if (name in sessions) {
            return@withLock sessions.getValue(name)
        }

        if (trimeImpl.lifecycle.currentState == RimeLifecycle.State.STOPPED) {
            trimeImpl.start()
        }

        val session = establish(name)
        sessions[name] = session

        return@withLock session
    }

    /** 销毁指定名字的会话 */
    fun destroySession(name: String): Unit = lock.withLock {
        if (name !in sessions) {
            return
        }

        sessions -= name
        if (sessions.isEmpty()) {
            trimeImpl.stop()
        }
    }

    /**  Restart Trime instance to deploy while keep the session */
    fun restartTrime() = lock.withLock {
        trimeImpl.stop()
        trimeImpl.start()
    }

    // ------------------------------------------------------------------

    /** 构造 [TrimeSession] 会话 */
    private fun establish(name: String) = object : TrimeSession {

        override val lifecycleScope: CoroutineScope
            get() = trimeImpl.lifecycle.lifecycleScope

        private inline fun <T> ensureEstablished(block: () -> T) = if (name in sessions) {
            block()
        } else {
            throw IllegalStateException("Trime session $name should be established via ${javaClass.simpleName}#createSession() first")
        }

        override fun config(block: Trime.Config.() -> Unit) {
            val oldUserDataDir = trimeImpl.userDataDir
            val oldSharedDataDir = trimeImpl.sharedDataDir

            block(trimeCnf)
            if (!trimeImpl.isReady) {
                return
            }

            val newUserDataDir = trimeImpl.userDataDir
            val newSharedDataDir = trimeImpl.sharedDataDir
            if ((oldUserDataDir != null && newUserDataDir != null && oldUserDataDir != newUserDataDir)
                || (oldSharedDataDir != null && newSharedDataDir != null && oldSharedDataDir != newSharedDataDir)
            ) {
                restartTrime()
            }
        }

        override fun <T> run(block: suspend Trime.() -> T): T = ensureEstablished {
            runBlocking { block(trimeInf) }
        }

        override suspend fun <T> runOnReady(block: suspend Trime.() -> T): T = ensureEstablished {
            trimeImpl.lifecycle.whenReady { block(trimeInf) }
        }

        override fun runIfReady(block: suspend Trime.() -> Unit) = ensureEstablished {
            if (trimeImpl.isReady) {
                trimeImpl.lifecycleScope.launch {
                    block(trimeInf)
                }
            }
        }
    }
}