package org.crazydan.studio.app.ime.kuaizi.engine.logging

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format.Padding
import kotlinx.datetime.format.char
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import org.crazydan.studio.app.ime.kuaizi.engine.LogLevel
import java.io.File
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days

class LogStorage(
    logDir: File,
) {
    private var logDir: File = logDir
    private var cachedTodayDate: LocalDate? = null
    private var cachedTodayFile: File? = null
    private var lastCleanupTime: Long = 0L
    private val cleanupIntervalMs = 60_000L

    companion object {
        const val MAX_FILE_SIZE_BYTES = 5L * 1024 * 1024
        const val MAX_RETENTION_DAYS = 7L
        const val FILE_NAME_PREFIX = "kuaizi_ime_"
        const val FILE_NAME_SUFFIX = ".log"

        /** yyyy-MM-dd HH:mm:ss.SSS */
        private val dateTimeFormat = LocalDateTime.Format {
            year()
            char('-')
            monthNumber(Padding.ZERO)
            char('-')
            day(Padding.ZERO)
            char(' ')
            hour(Padding.ZERO)
            char(':')
            minute(Padding.ZERO)
            char(':')
            second(Padding.ZERO)
            char('.')
            secondFraction(3)
        }

        /** yyyy-MM-dd */
        private val dateFormat = LocalDate.Format {
            // 2026-06-27
            year()
            char('-')
            monthNumber(Padding.ZERO)
            char('-')
            day(Padding.ZERO)
        }
    }

    fun updateDir(logDir: File) {
        this.logDir = logDir
        cachedTodayDate = null
        cachedTodayFile = null
    }

    suspend fun appendEntries(entries: List<LogEntry>) {
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

    fun exportLogs(
        destination: File,
        fromDate: LocalDate,
        toDate: LocalDate,
    ) {
        val lines = mutableListOf<String>()
        var date = fromDate
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

    private fun todayFile(): File {
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        if (cachedTodayDate != today) {
            cachedTodayDate = today
            cachedTodayFile = fileForDate(today)
        }
        return cachedTodayFile!!
    }

    private fun fileForDate(date: LocalDate): File =
        File(logDir, "$FILE_NAME_PREFIX${dateFormat.format(date)}$FILE_NAME_SUFFIX")

    private fun rotateFile(file: File) {
        val rotated = File(
            file.parent,
            file.nameWithoutExtension + "_rotated_${System.currentTimeMillis()}.log",
        )
        file.renameTo(rotated)
    }

    private fun cleanupOldFiles() {
        val cutoff = Clock.System.now().minus(MAX_RETENTION_DAYS.days)
            .toLocalDateTime(TimeZone.currentSystemDefault()).date

        logDir.listFiles()
            ?.filter { it.name.startsWith(FILE_NAME_PREFIX) && it.name.endsWith(FILE_NAME_SUFFIX) }
            ?.filter { extractDateFromFileName(it.name)?.let { d -> d < cutoff } == true }
            ?.forEach { it.delete() }
    }

    private fun extractDateFromFileName(name: String): LocalDate? = runCatching {
        val dateStr = name.removePrefix(FILE_NAME_PREFIX).removeSuffix(FILE_NAME_SUFFIX)
        LocalDate.parse(dateStr)
    }.getOrNull()

    private fun parseLine(line: String): LogEntry? {
        return runCatching {
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
                        dateTimeFormat
                    ).toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()
                } catch (_: Exception) {
                    System.currentTimeMillis()
                },
                threadName = match.groupValues[4],
            )
        }.getOrNull()
    }
}
