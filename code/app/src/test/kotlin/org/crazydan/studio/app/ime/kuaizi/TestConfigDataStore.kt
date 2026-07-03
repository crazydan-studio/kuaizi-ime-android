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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import okio.Path.Companion.toOkioPath
import org.crazydan.studio.app.ime.kuaizi.engine.ImeEngine
import org.crazydan.studio.app.ime.kuaizi.engine.dict.provider.InMemoryDictProvider
import org.crazydan.studio.app.ime.kuaizi.engine.keyboard.KeyboardHandMode
import org.crazydan.studio.app.ime.kuaizi.engine.log.LogLevel
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import kotlin.coroutines.CoroutineContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2026-06-30
 */
class TestConfigDataStore {
    // 使用 TemporaryFolder 确保每次测试后，临时文件都会被自动物理删除
    @Rule
    @JvmField
    val tempDir = TemporaryFolder()

    fun createConfigDataStore(context: CoroutineContext): ConfigDataStore {
        val dataStore = PreferenceDataStoreFactory.createWithPath(
            // 保证 DataStore 内部写入文件和并发读取时使用的线程调度器与测试代码完全一致，
            // 避免 assertEquals 在数据还没写入完成时就执行了
            scope = TestScope(context),
            produceFile = { tempDir.newFile("test_prefs.preferences_pb").toOkioPath() }
        )

        return ConfigDataStore(dataStore)
    }

    // --------------------------------------------------------------------------

    @Test
    fun `should persist and restore config`() = runTest {
        val configDataStore = createConfigDataStore(testScheduler)

        val old = configDataStore.getConfig()
        assertNotEquals(LogLevel.INFO, old.engine.logLevel)
        assertNotEquals(KeyboardHandMode.Left, old.ui.keyboardHandMode)

        configDataStore.updateConfig {
            it.copy(
                engine = it.engine.copy(
                    logLevel = LogLevel.INFO
                ),
                ui = it.ui.copy(
                    keyboardHandMode = KeyboardHandMode.Left
                )
            )
        }

        val new = configDataStore.getConfig()
        assertNotEquals(old, new)
        assertEquals(LogLevel.INFO, new.engine.logLevel)
        assertEquals(KeyboardHandMode.Left, new.ui.keyboardHandMode)
    }

    @Test
    fun `should not persist when config isn't changed`() = runTest {
        val configDataStore = createConfigDataStore(testScheduler)

        val old = configDataStore.getConfig()
        configDataStore.updateConfig {
            it.copy(
                engine = it.engine.copy(),
                ui = it.ui.copy()
            )
        }

        val new = configDataStore.getConfig()
        assertEquals(old, new)
        // 对象引用未变
        assertEquals(old.hashCode(), new.hashCode())
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `should be detected when config was updated`() = runTest {
        val configDataStore = createConfigDataStore(testScheduler)

        val changed = mutableListOf<Any?>()
        // 采用 backgroundScope 以确保 runTest 结束后能够自动清理协程
        backgroundScope.launch {
            configDataStore.whenConfigUpdated { config ->
                changed.add(config.engine.logLevel)
                changed.add(config.ui.keyboardHandMode)
            }
        }
        // 强迫当前线程上所有待执行的协程任务立刻执行完毕，从而能够立刻断言结果
        runCurrent()

        configDataStore.updateConfig {
            it.copy(
                engine = it.engine.copy(
                    logLevel = LogLevel.INFO
                ),
                ui = it.ui.copy(
                    keyboardHandMode = KeyboardHandMode.Left
                )
            )
        }
        runCurrent()

        assertEquals(2, changed.size)
        assertTrue(changed.contains(LogLevel.INFO))
        assertTrue(changed.contains(KeyboardHandMode.Left))
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `should persist when engine config was updated`() = runTest {
        val configDataStore = createConfigDataStore(testScheduler)

        val old = configDataStore.getConfig()
        val engine = ImeEngine.create(
            config = old,
            dictProvider = InMemoryDictProvider()
        )

        // 采用 backgroundScope 以确保 runTest 结束后能够自动清理协程
        backgroundScope.launch {
            updateConfigDataStoreWhenEngineConfigUpdated(configDataStore, engine)
        }
        // 强迫当前线程上所有待执行的协程任务立刻执行完毕，从而能够立刻断言结果
        runCurrent()

        engine.updateConfig {
            it.copy(
                engine = it.engine.copy(
                    logLevel = LogLevel.INFO
                ),
                ui = it.ui.copy(
                    keyboardHandMode = KeyboardHandMode.Left
                )
            )
        }
        runCurrent()

        val new = configDataStore.getConfig()
        assertNotEquals(old, new)
        assertEquals(LogLevel.INFO, new.engine.logLevel)
        assertEquals(KeyboardHandMode.Left, new.ui.keyboardHandMode)
    }
}