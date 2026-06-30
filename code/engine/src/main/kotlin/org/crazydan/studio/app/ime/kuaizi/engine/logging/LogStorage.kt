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

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import org.crazydan.studio.app.ime.kuaizi.engine.util.DateTimeHelper
import java.io.File
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days

/**
 * 日志文件存储管理器，不依赖 Android Context。
 *
 * 路径解析由应用层负责，引擎库只关心文件的读写操作。
 * 日志文件按日期组织，每天一个文件，单文件超过 5MB 时自动滚动，
 * 保留最近 7 天的日志文件，超期自动清理。
 *
 * 性能优化：
 * - [todayFile] 结果缓存，仅在跨日时重新计算
 * - [cleanupOldFiles] 降频至每分钟执行一次，避免每次写入都扫描目录
 *
 * @param logDir 日志文件存储目录
 */
class LogStorage(private var logDir: File) {
    private var cachedTodayDate: LocalDate? = null
    private var cachedTodayFile: File? = null
    private var lastCleanupTime: Long = 0L
    private val cleanupIntervalMs = 60_000L

    companion object {
        const val MAX_FILE_SIZE_BYTES = 5L * 1024 * 1024
        const val MAX_RETENTION_DAYS = 7L
        const val FILE_NAME_PREFIX = "Kuaizi_IME_"
        const val FILE_NAME_SUFFIX = ".log"
    }

    /** 更新日志目录（应用层切换存储路径时调用），同时清空缓存。 */
    fun changeDir(logDir: File) {
        if (this.logDir == logDir) return

        this.logDir = logDir

        cachedTodayDate = null
        cachedTodayFile = null
    }

    /**
     * 追加日志条目到当天文件。
     * 超过大小上限自动滚动，超期文件自动清理。
     */
    fun appendEntries(entries: List<LogEntry>) {
        val file = todayFile()
        if (file.exists() && file.length() > MAX_FILE_SIZE_BYTES) {
            rotateFile(file)
        }
        file.appendText(entries.joinToString("\n") { it.format() } + "\n")

        val now = System.currentTimeMillis()
        if (now - lastCleanupTime > cleanupIntervalMs) {
            lastCleanupTime = now
            cleanupOldFiles()
        }
    }

    /**
     * 读取日志。
     * 支持按日期、等级和关键词过滤。
     */
    fun readLogs(
        date: LocalDate? = null,
        levelFilter: LogLevel? = null,
        keyword: String? = null,
    ): List<LogEntry> {
        val file = if (date != null) fileForDate(date) else todayFile()

        if (!file.exists()) return emptyList()

        return file.readLines()
            .mapNotNull { parseLine(it) }
            .filter { levelFilter == null || it.level.priority >= levelFilter.priority }
            .filter { keyword == null || it.message.contains(keyword, ignoreCase = true) }
    }

    /** 导出指定日期范围的日志为单个文件。 */
    fun exportLogs(
        destination: File,
        fromDate: LocalDate,
        toDate: LocalDate,
    ) {
        var date = fromDate

        val lines = mutableListOf<String>()
        while (date <= toDate) {
            val file = fileForDate(date)

            if (file.exists()) {
                lines += "= ${file.name} ="
                lines += file.readLines()
                lines += ""
            }
            date = date.plus(1, DateTimeUnit.DAY)
        }

        destination.writeText(lines.joinToString("\n"))
    }

    /** 获取今日日志文件（结果缓存，按日期失效），避免每次写入都读取系统时钟。 */
    private fun todayFile(): File {
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

        if (cachedTodayDate != today) {
            cachedTodayDate = today
            cachedTodayFile = fileForDate(today)
        }
        return cachedTodayFile!!
    }

    private fun fileForDate(date: LocalDate): File =
        File(logDir, "$FILE_NAME_PREFIX${DateTimeHelper.dateFormat.format(date)}$FILE_NAME_SUFFIX")

    /** 日志文件滚动：超过大小上限时重命名添加时间戳后缀。 */
    private fun rotateFile(file: File) {
        val rotated = File(
            file.parent,
            file.nameWithoutExtension + "_rotated_${System.currentTimeMillis()}.log",
        )

        file.renameTo(rotated)
    }

    /** 清理超过保留期限的旧日志文件。 */
    private fun cleanupOldFiles() {
        val cutoff =
            Clock.System.now().minus(MAX_RETENTION_DAYS.days)
                .toLocalDateTime(TimeZone.currentSystemDefault()).date

        logDir.listFiles()
            ?.filter { it.name.startsWith(FILE_NAME_PREFIX) && it.name.endsWith(FILE_NAME_SUFFIX) }
            ?.filter { extractDateFromFileName(it.name)?.let { d -> d < cutoff } == true }
            ?.forEach { it.delete() }
    }

    /** 从文件名中提取日期。 */
    private fun extractDateFromFileName(name: String): LocalDate? =
        runCatching {
            val dateStr = name.removePrefix(FILE_NAME_PREFIX).removeSuffix(FILE_NAME_SUFFIX)

            LocalDate.parse(dateStr)
        }.getOrNull()

    /** 将日志行文本解析为 [LogEntry] 对象。 */
    private fun parseLine(line: String): LogEntry? =
        runCatching {
            val regex = Regex(
                """(\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}\.\d{3}) \[(\w+)] \[(\w+)] \[(\w+)] (.+)"""
            )
            val match = regex.matchEntire(line) ?: return null

            LogEntry(
                level = LogLevel.valueOf(match.groupValues[2]),
                tag = match.groupValues[3],
                message = match.groupValues[5],
                timestamp = try {
                    LocalDateTime.parse(
                        match.groupValues[1],
                        DateTimeHelper.dateTimeFormat
                    ).toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()
                } catch (_: Exception) {
                    System.currentTimeMillis()
                },
                threadName = match.groupValues[4],
            )
        }.getOrNull()
}
