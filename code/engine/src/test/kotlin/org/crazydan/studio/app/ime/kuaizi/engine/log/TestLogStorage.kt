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

package org.crazydan.studio.app.ime.kuaizi.engine.log

import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2026-06-30
 */
class TestLogStorage {
    // 使用 TemporaryFolder 确保每次测试后，临时文件都会被自动物理删除
    @Rule
    @JvmField
    val tempDir = TemporaryFolder()

    fun createLogStorage(): LogStorage = LogStorage(tempDir.root)

    @Test
    fun `should create log file after appending new log`() = runTest {
        val logStorage = createLogStorage()

        val log = LogEntry(
            level = LogLevel.INFO,
            tag = "JUnit",
            message = "This is a test log",
            exception = RuntimeException("Some error happens"),
        )
        logStorage.appendEntries(listOf(log))

        // -------------------------------------
        val files = tempDir.root.list()
        assertNotNull(files)
        assertEquals(1, files.size)

        // -------------------------------------
        val file = File(tempDir.root, files[0])
        assertTrue(file.isFile)
        assertTrue(file.name.startsWith(LogStorage.FILE_NAME_PREFIX))
        assertTrue(file.name.endsWith(LogStorage.FILE_NAME_SUFFIX))

        val content = file.readText()
        assertTrue(content.contains(log.format()))
    }

    @Test
    fun `should filter logs with some conditions`() = runTest {
        val logStorage = createLogStorage()

        val log1 = LogEntry(
            level = LogLevel.INFO,
            tag = "JUnit",
            message = "This is a test log 1",
        )
        val log2 = LogEntry(
            level = LogLevel.DEBUG,
            tag = "JUnit",
            message = "This is a test log 2",
        )
        val log3 = LogEntry(
            level = LogLevel.DEBUG,
            tag = "JUnit",
            message = "This is a test log 3",
            exception = RuntimeException("Some error happens"),
        )
        val logs = listOf(log1, log2, log3)
        logStorage.appendEntries(logs)

        // -------------------------------------
        val logs1 = logStorage.readLogs()
        assertEquals(logs.size, logs1.size)
        assertEquals(logs[0], logs1[0])
        assertEquals(logs[1], logs1[1])
        assertEquals(logs[2], logs1[2])

        // -------------------------------------
        val logs2 = logStorage.readLogs(level = LogLevel.INFO)
        assertEquals(1, logs2.size)
        assertEquals(log1, logs2[0])

        // -------------------------------------
        val logs3 = logStorage.readLogs(level = LogLevel.DEBUG)
        assertEquals(logs.size, logs3.size)
        assertEquals(logs[0], logs1[0])
        assertEquals(logs[1], logs1[1])
        assertEquals(logs[2], logs1[2])

        // -------------------------------------
        val logs4 = logStorage.readLogs(keyword = "log 2")
        assertEquals(1, logs4.size)
        assertEquals(log2, logs4[0])

        // -------------------------------------
        val logs5 = logStorage.readLogs(level = LogLevel.INFO, keyword = "log 2")
        assertTrue(logs5.isEmpty())
    }
}