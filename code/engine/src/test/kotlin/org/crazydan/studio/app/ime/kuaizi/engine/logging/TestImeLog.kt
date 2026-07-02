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

package org.crazydan.studio.app.ime.kuaizi.engine.logging

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.crazydan.studio.app.ime.kuaizi.engine.logging.writer.FileLogWriter
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import kotlin.coroutines.CoroutineContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2026-06-30
 */
class TestImeLog {
    // 使用 TemporaryFolder 确保每次测试后，临时文件都会被自动物理删除
    @Rule
    @JvmField
    val tempDir = TemporaryFolder()

    fun createLogStorage(): LogStorage = LogStorage(tempDir.root)

    fun getImeLogger(context: CoroutineContext, storage: LogStorage): ImeLogger {
        ImeLog.addWriters(
            FileLogWriter(
                storage = storage,
                scope = TestScope(context),
            )
        )
        ImeLog.enableLevel(LogLevel.INFO)

        return ImeLog.logger(TestImeLog::class)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `should only log enabled level logs`() = runTest {
        val logStorage = createLogStorage()
        val logger = getImeLogger(testScheduler, logStorage)

        // ----------------
        ImeLog.enableLevel(LogLevel.WARN)

        logger.debug { "This is a debug log" }
        runCurrent() // 等待 FileLogWriter 写入完毕

        assertFalse(ImeLog.isEnabledLevel(LogLevel.DEBUG))
        assertTrue(logStorage.readLogs().isEmpty())

        // -----
        logger.error { "This is a error log" }
        runCurrent() // 等待 FileLogWriter 写入完毕

        assertTrue(ImeLog.isEnabledLevel(LogLevel.ERROR))

        val logs1 = logStorage.readLogs()
        assertEquals(1, logs1.size)
        assertTrue(logs1[0].message.contains("error log"))

        // ----------------
        ImeLog.enableLevel(LogLevel.DEBUG)

        logger.debug { "This is a debug log" }
        runCurrent() // 等待 FileLogWriter 写入完毕

        assertTrue(ImeLog.isEnabledLevel(LogLevel.DEBUG))

        val logs2 = logStorage.readLogs(keyword = "debug")
        assertEquals(1, logs2.size)
        assertTrue(logs2[0].message.contains("debug log"))
    }
}