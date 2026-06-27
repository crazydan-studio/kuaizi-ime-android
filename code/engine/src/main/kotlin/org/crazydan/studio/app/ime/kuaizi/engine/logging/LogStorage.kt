package org.crazydan.studio.app.ime.kuaizi.engine.logging

import java.io.File
import java.time.LocalDate

class LogStorage(private var logDir: File) {
    private var cachedTodayDate: LocalDate? = null
    private var cachedTodayFile: File? = null
    private var lastCleanupTime: Long = 0L
    private val cleanupIntervalMs = 60_000L

    private fun todayFile(): File {
        val today = LocalDate.now()
        if (cachedTodayDate != today) {
            cachedTodayDate = today
            cachedTodayFile = fileForDate(today)
        }
        return cachedTodayFile!!
    }

    private fun fileForDate(date: LocalDate): File {
        return File(logDir, "kuaizi_ime_${date}.log")
    }

    fun appendEntries(entries: List<LogEntry>) {
        val file = todayFile()
        file.parentFile?.mkdirs()

        val text = entries.joinToString("\n") { it.format() } + "\n"
        file.appendText(text)

        val now = System.currentTimeMillis()
        if (now - lastCleanupTime > cleanupIntervalMs) {
            lastCleanupTime = now
            cleanupOldFiles()
        }
    }

    fun readLogs(
        date: LocalDate? = null,
        levelFilter: String? = null,
        keyword: String? = null,
    ): List<LogEntry> {
        val targetDate = date ?: LocalDate.now()
        val file = fileForDate(targetDate)
        if (!file.exists()) return emptyList()

        return file.readLines().mapNotNull { line ->
            parseLine(line)
        }.filter { entry ->
            (levelFilter == null || entry.level.name == levelFilter) &&
                (keyword == null || entry.message.contains(keyword, ignoreCase = true))
        }
    }

    fun exportLogs(destination: File, fromDate: LocalDate, toDate: LocalDate) {
        destination.parentFile?.mkdirs()
        destination.bufferedWriter().use { writer ->
            var date = fromDate
            while (!date.isAfter(toDate)) {
                val file = fileForDate(date)
                if (file.exists()) {
                    file.forEachLine { line ->
                        writer.write(line)
                        writer.newLine()
                    }
                }
                date = date.plusDays(1)
            }
        }
    }

    fun updateDir(newDir: File) {
        logDir = newDir
        cachedTodayDate = null
        cachedTodayFile = null
    }

    private fun cleanupOldFiles() {
        val files = logDir.listFiles { f -> f.name.startsWith("kuaizi_ime_") } ?: return
        val cutoff = LocalDate.now().minusDays(7)
        files.forEach { file ->
            val dateStr = file.name.removePrefix("kuaizi_ime_").removeSuffix(".log")
            try {
                val date = LocalDate.parse(dateStr)
                if (date.isBefore(cutoff)) {
                    file.delete()
                }
                if (file.length() > 5 * 1024 * 1024) {
                    file.renameTo(File(file.absolutePath + ".old"))
                }
            } catch (_: Exception) {
                // skip unparseable files
            }
        }
    }

    private fun parseLine(line: String): LogEntry? {
        // Simple parser - in production use a more robust format
        return try {
            val parts = line.split("] [", limit = 4)
            if (parts.size < 3) return null
            val levelPart = parts[0].substringAfter("[").substringBefore("]")
            val tagPart = parts[1]
            val msgPart = parts.last().removeSuffix("]")
            LogEntry(
                level = LogLevel.fromPriority(LogLevel.valueOf(levelPart).priority),
                tag = tagPart,
                message = msgPart,
            )
        } catch (_: Exception) {
            null
        }
    }
}
