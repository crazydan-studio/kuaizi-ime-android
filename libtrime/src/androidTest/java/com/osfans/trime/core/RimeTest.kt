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
import org.crazydan.studio.ime.libtrime.utils.copyToDir
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import timber.log.Timber
import java.io.File

/**
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2026-03-01
 */
@RunWith(AndroidJUnit4::class)
class RimeTest {

    @Before
    fun setUp() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val userDir = File(appContext.getExternalFilesDir(null), "rime-user").also { it.mkdirs() }
        val sharedDir = File(appContext.getExternalFilesDir(null), "rime-shared").also { it.mkdirs() }

        appContext.assets.copyToDir("rime_wanxiang_simple", sharedDir)

        Rime.startupRime(
            sharedDir = sharedDir.absolutePath,
            userDir = userDir.absolutePath,
            fullCheck = false,
        )
        Thread.sleep(500)
    }

    @After
    fun tearDown() {
        Rime.exitRime()
    }

    @Test
    fun test_startupRime() {
        withRimeContext {
            // nothing
        }
    }

    @Test
    fun test_inputKeys() {
        withRimeContext {
            val spells = "shi jie ren min da tuan jie wan sui".split("\\s+".toRegex())
            val expectedCandidates = "世(shì)界(jiè)人(rén)民(mín)大(dà)团(tuán)结(jié)万(wàn)岁(suì)"

            val candidates = spells.mapIndexed { index, spell ->
                spell.toCharArray().forEach { ch ->
                    if (!pressKey(ch)) {
                        Timber.e("Cannot process key '$ch'")
                    }
                }

                val context = Rime.getRimeContext()

                val candidates = context.menu.candidates
                var selected = 0;
                if (candidates.size > 1 && spell == "jie" && index < 2) {
                    selected = 1 // 结(jié) and 界(jiè)
                }

                // Note: Rime.selectRimeCandidate 会更新字的权重而导致后续选字与预期不符
                pressKey(' ') // trigger commit
                //Rime.selectRimeCandidate(selected, false)

                candidates[selected]
            }

            assertEquals(expectedCandidates, candidates.joinToString("") { it.text + "(${it.comment})" })
        }
    }

    private fun pressKey(ch: Char): Boolean {
        val keyName = when (ch) {
            ' ' -> "space"
            '\'' -> "apostrophe"
            else -> ch.toString()
        }
        val value = RimeKeyMapping.nameToKeyVal(keyName)

        return Rime.processRimeKey(value, 0)
    }

    private fun withRimeContext(block: () -> Unit) {
        val schemas = Rime.getRimeSchemaList()
        assertEquals("wanxiang", schemas[0].id)
        Rime.selectRimeSchema(schemas[0].id)

        block()
    }
}