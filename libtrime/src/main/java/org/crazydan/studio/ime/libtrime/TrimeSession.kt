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

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Trime 会话，用于在不同的场景（配置、练习、正式使用等）下共享 [Trime] 实例
 *
 * source from [trime/RimeSession.kt](https://github.com/osfans/trime/blob/develop/app/src/main/java/com/osfans/trime/daemon/RimeSession.kt)
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2026-03-04
 */
interface TrimeSession {
    val lifecycleScope: CoroutineScope

    /**
     * Set or update [Trime] configurations.
     *
     * Note: only when [Trime.Config.userDataDir] and [Trime.Config.sharedDataDir] are specified
     * can the Trime be started. If the Rime was already started, it will be restarted automatically.
     */
    fun config(block: Trime.Config.() -> Unit)

    /**
     * Run an operation immediately
     * The suspended [block] will be executed in caller's thread.
     * Use this function only for non-blocking operations like
     * accessing [Trime.messageFlow]:
     * ```kotlin
     * val rime = RimeDaemon.createSession(javaClass.name)
     * rime.run { messageFlow }.collect {
     *     handleRimeMessage(it)
     * }
     * ```
     */
    fun <T> run(block: suspend Trime.() -> T): T

    /**
     * Run an operation if Trime is at ready state ([Trime.isReady] == `true`).
     * Otherwise, do nothing.
     * The [block] will be executed in executed in thread pool.
     * This function does not block or suspend the caller.
     */
    fun runIfReady(block: suspend Trime.() -> Unit)

    /**
     * Run an operation immediately if Trime is at ready state ([Trime.isReady] == `true`).
     * Otherwise, caller will be suspended until Trime is ready and operation is done.
     * The [block] will be executed in caller's thread.
     * Client should use this function in most cases.
     */
    suspend fun <T> runOnReady(block: suspend Trime.() -> T): T

    fun launchOnReady(block: suspend CoroutineScope.(Trime) -> Unit) {
        lifecycleScope.launch {
            runOnReady { block(this) }
        }
    }
}