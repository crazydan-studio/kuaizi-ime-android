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

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2026-03-01
 */
@RunWith(AndroidJUnit4::class)
class RimeTest {

    @Test
    fun test_startupRime() {
        withRimeContext {
            // nothing
        }
    }

    @Test
    fun test_inputKeys() {
        withRimeContext {
            val spells = "shi jie ren min da tuan jie wan sui".split("\\s+")
            val candidates = "世 界 人 民 大 团 结 万 岁".split("\\s+")

            spells.forEachIndexed { i, spell ->
                spell.toCharArray().forEach { ch ->
                    pressKey(ch)
                }

                if (i < spells.size - 1) {
                    pressKey('\'')
                }
            }

            val results = Rime.getRimeCandidates(0, 1)
            results.forEach {
                it.text + ' ' + it.comment
            }
        }
    }

    private fun pressKey(ch: Char) {
        val value = RimeKeyMapping.nameToKeyVal(ch.toString())

        Rime.processRimeKey(value, 0)
    }

    private fun withRimeContext(block: () -> Unit) {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val sharedDataDir = File(appContext.getExternalFilesDir(null), "rime").also { it.mkdirs() }
        val userDir = File(appContext.filesDir, "user").also { it.mkdirs() }

        Rime.startupRime(
            sharedDir = sharedDataDir.absolutePath,
            userDir = userDir.absolutePath,
            fullCheck = false,
        )

        block.invoke()

        Rime.exitRime()
    }
}