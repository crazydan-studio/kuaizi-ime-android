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

package org.crazydan.studio.app.ime.kuaizi

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.test.runTest
import okio.Path.Companion.toOkioPath
import org.crazydan.studio.app.ime.kuaizi.engine.domain.KeyboardHandMode
import org.crazydan.studio.app.ime.kuaizi.engine.logging.LogLevel
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

/**
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2026-06-30
 */
class TestConfigDataStore {
    private val tempDir by lazy { createTempDirectory() }

    private val dataStore = PreferenceDataStoreFactory.createWithPath(
        corruptionHandler = null,
        produceFile = { tempDir.resolve("test_prefs.preferences_pb").toOkioPath() }
    )
    private val configStore = ConfigDataStore(dataStore)

    @Test
    fun `should persist and restore config`() = runTest {
        val old = configStore.getConfig()
        assertNotEquals(LogLevel.INFO, old.engine.logLevel)
        assertNotEquals(KeyboardHandMode.Left, old.ui.keyboardHandMode)

        configStore.updateConfig {
            it.copy(
                engine =
                    it.engine.copy(
                        logLevel = LogLevel.INFO
                    ),
                ui =
                    it.ui.copy(
                        keyboardHandMode = KeyboardHandMode.Left
                    )
            )
        }

        val new = configStore.getConfig()
        assertNotEquals(old, new)
        assertEquals(LogLevel.INFO, new.engine.logLevel)
        assertEquals(KeyboardHandMode.Left, new.ui.keyboardHandMode)
    }

    @Test
    fun `should not persist when config isn't changed`() = runTest {
        val old = configStore.getConfig()
        configStore.updateConfig {
            it.copy(
                engine = it.engine.copy(),
                ui = it.ui.copy()
            )
        }

        val new = configStore.getConfig()
        assertEquals(old, new)
        // 对象引用未变
        assertEquals(old.hashCode(), new.hashCode())
    }
}