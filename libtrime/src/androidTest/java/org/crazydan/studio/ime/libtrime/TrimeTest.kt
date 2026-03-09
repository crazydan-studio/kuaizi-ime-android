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

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.crazydan.studio.ime.libtrime.utils.copyToDir
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2026-03-06
 */
@RunWith(AndroidJUnit4::class)
class TrimeTest {
    val schema = "wanxiang"
    val session by lazy { TrimeDaemon.openSession(javaClass.name) }

    @Before
    fun setUp() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val userDir = File(appContext.filesDir, "rime-user")
        val sharedDir = File(appContext.getExternalFilesDir(null), "rime-shared")

        prepareData(appContext, userDir)

        TrimeDaemon.configTrime(appContext) {
            userDataDir = userDir
            sharedDataDir = sharedDir
        }
    }

    @After
    fun tearDown() {
        session.close()
    }

    @Test
    fun test_activateSchema() {
        withTrimeContext {
            val deployedSchemas = getDeployedSchemas()
            val enabledSchemas = getEnabledSchemas()
            assertTrue(enabledSchemas.isNotEmpty())
            assertTrue(deployedSchemas.size > enabledSchemas.size)

            val activated = activateSchema(schema)
            assertTrue(activated)
            assertEquals(schema, getActivatedSchema())
        }
    }

    @Test
    fun test_inputPinyin() {
        withTrimeContext {
            activateSchema(schema)

            val spells = "shi jie ren min da tuan jie wan sui".split("\\s+".toRegex())
            val expectedCandidates = "世(shì)界(jiè)人(rén)民(mín)大(dà)团(tuán)结(jié)万(wàn)岁(suì)"

            val candidates = inputSpells(spells)
            assertEquals(expectedCandidates, candidates.joinToString("") { it.text + "(${it.spell})" })
        }
    }

    private fun withTrimeContext(block: suspend Trime.() -> Unit) = runBlocking {
        session.runOnReady(block)
    }

    private fun prepareData(context: Context, userDir: File) {
        context.assets.copyToDir("rime_wanxiang/rime-wanxiang-base.zip", userDir, unzip = true)
        context.assets.copyToDir("rime_wanxiang/wanxiang-lts-zh-hans.gram", userDir)

        context.assets.copyToDir("rime_wanxiang/installed", userDir)

        val userConfigFile = userDir.resolve("user.yaml")
        // 修改 user.yaml 中的构建时间 var/last_build_time 以避免重新部署
        // Note: 仅针对已准备了部署文件的情况 (see src/androidTest/assets/rime_wanxiang/README.md)
        if (userConfigFile.exists()) {
            val currentTime = 10 + System.currentTimeMillis() / 1000
            userConfigFile.writeText(
                """
                    var:
                      last_build_time: $currentTime
                      previously_selected_schema: $schema
                      schema_access_time:
                        $schema: $currentTime
                """.trimIndent()
            )
        }
    }
}