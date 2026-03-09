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

import android.view.KeyEvent
import com.osfans.trime.core.KeyModifiers
import com.osfans.trime.core.KeyValue
import com.osfans.trime.core.Rime
import com.osfans.trime.core.RimeDispatcher
import com.osfans.trime.core.RimeKeyMapping
import com.osfans.trime.core.RimeLifecycle
import com.osfans.trime.core.RimeLifecycleOwner
import com.osfans.trime.core.RimeLifecycleRegistry
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.withContext
import org.crazydan.studio.ime.libtrime.Trime
import org.crazydan.studio.ime.libtrime.TrimeMessage
import org.crazydan.studio.ime.libtrime.TrimeSchema
import org.crazydan.studio.ime.libtrime.TrimeWord
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
    override var userDataDir: File? = null
    override var sharedDataDir: File? = null

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

        Timber.Forest.i("Trime stop()")
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

    override suspend fun processKey(keyChar: Char): Boolean = withRimeContext {
        processKeyInner(keyChar, 0)
    }

    override suspend fun processKey(keyEvent: KeyEvent): Boolean = withRimeContext {
        val value = KeyValue.fromKeyEvent(keyEvent)
        val modifiers = KeyModifiers.fromKeyEvent(keyEvent)

        processKeyInner(value.value, modifiers.toInt())
    }

    override suspend fun getRawInput(): String = withRimeContext {
        Rime.getRimeRawInput()
    }

    override suspend fun getCaretPos(): Int = withRimeContext {
        Rime.getRimeCaretPos()
    }

    override suspend fun setCaretPos(caretPos: Int) = withRimeContext {
        Rime.setRimeCaretPos(caretPos)
    }

    override suspend fun commitComposition(): Boolean = withRimeContext {
        Rime.commitRimeComposition()
    }

    override suspend fun clearComposition() = withRimeContext {
        Rime.clearRimeComposition()
    }

    // ------------------------------------------------------------------------

    override suspend fun getCandidates(pageStart: Int, pageSize: Int): Array<TrimeWord> = withRimeContext {
        val items = Rime.getRimeCandidates(pageStart, pageSize)
        TrimeWord.from(items)
    }

    override suspend fun getCurrentCandidates(): Array<TrimeWord> = withRimeContext {
        return@withRimeContext Rime.getRimeContext().menu.candidates.map {
            TrimeWord(
                text = it.text,
                spell = it.comment,
            )
        }.toTypedArray()
    }

    override suspend fun selectCandidate(index: Int, global: Boolean): Boolean = withRimeContext {
        Rime.selectRimeCandidate(index, global)
    }

    override suspend fun deleteCandidate(index: Int, global: Boolean): Boolean = withRimeContext {
        Rime.deleteRimeCandidate(index, global)
    }

    override suspend fun nextCandidatePage(): Boolean = withRimeContext {
        Rime.changeRimeCandidatePage(false)
    }

    override suspend fun prevCandidatePage(): Boolean = withRimeContext {
        Rime.changeRimeCandidatePage(true)
    }

    // ------------------------------------------------------------------------

    override suspend fun getSwitchesOption(option: String): Boolean = withRimeContext {
        Rime.getRimeOption(option)
    }

    override suspend fun setSwitchesOption(option: String, value: Boolean) = withRimeContext {
        Rime.setRimeOption(option, value)
    }

    // ------------------------------------------------------------------------

    override suspend fun activateSchema(schemaId: String): Boolean = withRimeContext {
        Rime.selectRimeSchema(schemaId)
    }

    override suspend fun getActivatedSchema(): String = withRimeContext {
        Rime.getCurrentRimeSchema()
    }

    override suspend fun enableSchemas(schemaIds: Array<String>): Boolean = withRimeContext {
        Rime.selectRimeSchemas(schemaIds)
    }

    override suspend fun getEnabledSchemas(): Array<TrimeSchema> = withRimeContext {
        TrimeSchema.from(Rime.getRimeSchemaList())
    }

    override suspend fun deploySchema(schemaFile: File): Boolean = withRimeContext {
        Rime.deployRimeSchemaFile(schemaFile.absolutePath)
    }

    override suspend fun getDeployedSchemas(): Array<TrimeSchema> = withRimeContext {
        TrimeSchema.from(Rime.getAvailableRimeSchemaList())
    }

    // ------------------------------------------------------------------------

    override suspend fun redeploy(full: Boolean) = withRimeContext {
        Rime.exitRime()
        startRime(full)
    }

    // ------------------------------------------------------------------------

    private fun startRime(
        /**
         * - 为 `true` 时强制执行数据文件的完整维护，适合首次部署或重大更新
         * - 为 `false` 时执行数据文件的智能维护，只在检测到变更时才重建，提高效率
         */
        fullCheck: Boolean,
    ) {
        requireNotNull(userDataDir) {
            "Rime userDataDir isn't specified," +
                    " please initialize it in TrimeDaemon.configTrime first"
        }
        requireNotNull(sharedDataDir) {
            "Rime sharedDataDir isn't specified," +
                    " please initialize it in TrimeDaemon.configTrime first"
        }

        userDataDir!!.mkdirs()
        sharedDataDir!!.mkdirs()

        Timber.Forest.d(
            """
            Starting Rime with:
            userDataDir: ${userDataDir!!.absolutePath}
            sharedDataDir: ${sharedDataDir!!.absolutePath}
            fullCheck: $fullCheck
            """.trimIndent(),
        )

        Rime.startupRime(
            sharedDir = sharedDataDir!!.absolutePath,
            userDir = userDataDir!!.absolutePath,
            fullCheck = fullCheck,
        )
    }

    private fun handleRimeMessage(type: Int, params: Array<Any>) {
        // TODO

//        if (type == 3 && params[0] != "start") {
//            lifecycle.emitState(RimeLifecycle.State.READY)
//        }
    }

    private fun processKeyInner(ch: Char, modifiers: Int): Boolean {
        val keyName = when (ch) {
            ' ' -> "space"
            '\'' -> "apostrophe"
            '#' -> "numbersign"
            '*' -> "asterisk"
            '+' -> "plus"
            ',' -> "comma"
            '-' -> "minus"
            '.' -> "period"
            '/' -> "slash"
            ';' -> "semicolon"
            '=' -> "equal"
            '[' -> "bracketleft"
            '\\' -> "backslash"
            ']' -> "bracketright"
            '`' -> "grave"
            else -> ch.toString()
        }
        val value = RimeKeyMapping.nameToKeyVal(keyName)

        return processKeyInner(value, modifiers)
    }

    private fun processKeyInner(keyVal: Int, modifiers: Int): Boolean {
        val handled = Rime.processRimeKey(keyVal, modifiers)
        if (!handled) {
            //
        }

        return handled
    }
}