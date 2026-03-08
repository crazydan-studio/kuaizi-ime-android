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
    val session by lazy { TrimeDaemon.openSession(javaClass.name) }

    @Before
    fun setUp() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val userDir = File(appContext.filesDir, "rime-user")
        val sharedDir = File(appContext.getExternalFilesDir(null), "rime-shared")

        appContext.assets.copyToDir("rime_wanxiang", sharedDir)

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

            val activated = activateSchema("wanxiang")
            assertTrue(activated)
            assertEquals("wanxiang", getActivatedSchema())
        }
    }

    private fun withTrimeContext(block: suspend Trime.() -> Unit) = runBlocking {
        session.runOnReady(block)
    }
}