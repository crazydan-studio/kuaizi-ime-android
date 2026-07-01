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

package org.crazydan.studio.app.ime.kuaizi.engine

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.crazydan.studio.app.ime.kuaizi.engine.dict.provider.InMemoryDictProvider
import org.crazydan.studio.app.ime.kuaizi.engine.domain.KeyboardThemeType
import org.crazydan.studio.app.ime.kuaizi.engine.logging.LogLevel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2026-07-01
 */
class TestImeEngine {

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `should be detected when config was updated`() = runTest {
        val engine = ImeEngine.create(
            dictProvider = InMemoryDictProvider()
        )

        val changed = mutableListOf<Any?>()
        // 采用 backgroundScope 以确保 runTest 结束后能够自动清理协程
        backgroundScope.launch {
            engine.whenConfigUpdated { config ->
                changed.add(config.engine.logLevel)
                changed.add(config.ui.keyboardThemeType)
                changed.add(config.runtime.editorInputType)
            }
        }
        // 强迫当前线程上所有待执行的协程任务立刻执行完毕，从而能够立刻断言结果
        runCurrent()

        // --------------------------------
        val logLevel = LogLevel.VERBOSE
        engine.updateConfig {
            it.copy(
                engine = it.engine.copy(
                    logLevel = logLevel
                )
            )
        }
        runCurrent()

        val keyboardThemeType = KeyboardThemeType.Night
        engine.updateConfig {
            it.copy(
                ui = it.ui.copy(
                    keyboardThemeType = keyboardThemeType
                )
            )
        }
        runCurrent()

        val editorInputType = EditorInputType.URI
        engine.updateConfig {
            it.copy(
                runtime = it.runtime.copy(
                    editorInputType = editorInputType
                )
            )
        }
        runCurrent()

        val config = engine.state.value.config
        assertEquals(logLevel, config.engine.logLevel)
        assertEquals(keyboardThemeType, config.ui.keyboardThemeType)
        assertEquals(editorInputType, config.runtime.editorInputType)

        // ------------------
        assertEquals(3 * 3, changed.size)
        assertTrue(changed.contains(logLevel))
        assertTrue(changed.contains(keyboardThemeType))
        assertTrue(changed.contains(editorInputType))
    }
}