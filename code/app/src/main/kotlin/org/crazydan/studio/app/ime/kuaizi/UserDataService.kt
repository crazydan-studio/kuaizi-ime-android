package org.crazydan.studio.app.ime.kuaizi

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.crazydan.studio.app.ime.kuaizi.config.ConfigDataStore
import org.crazydan.studio.app.ime.kuaizi.engine.domain.*

class UserDataService(
    private val userInputDao: UserInputDao,
    private val favoriteDao: FavoriteDao,
    private val configStore: ConfigDataStore,
    private val json: Json,
    private val scope: CoroutineScope,
) {
    suspend fun exportTo(context: Context, uri: Uri): ExportResult {
        val userInputData = userInputDao.getAll()
        val favoriteData = favoriteDao.getAll()
        val configData = configStore.config.first()

        val backup = UserBackup(
            version = BACKUP_FORMAT_VERSION,
            appVersion = BuildConfig.VERSION_NAME,
            exportedAt = java.time.Clock.System.now().toString(),
            data = BackupData(
                userInput = userInputData.map { it.toBackupEntry() },
                favorites = favoriteData.map { it.toBackupEntry() },
                config = configData.toBackupEntry(),
            ),
        )

        val jsonString = json.encodeToString(backup)

        return withContext(Dispatchers.IO) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use { output ->
                    output.write(jsonString.toByteArray(Charsets.UTF_8))
                } ?: error("Failed to open output stream for URI: $uri")
            }.fold(
                onSuccess = { ExportResult.Success(itemCount = userInputData.size + favoriteData.size) },
                onFailure = { ExportResult.Failure(it.message ?: "Unknown error") },
            )
        }
    }

    suspend fun importFrom(
        context: Context,
        uri: Uri,
        strategy: ImportStrategy = ImportStrategy.Replace,
    ): ImportResult {
        return withContext(Dispatchers.IO) {
            val jsonString = runCatching {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    input.readBytes().toString(Charsets.UTF_8)
                } ?: error("Failed to open input stream for URI: $uri")
            }.getOrElse {
                return@withContext ImportResult.Failure(it.message ?: "Failed to read file")
            }

            val backup = runCatching {
                json.decodeFromString<UserBackup>(jsonString)
            }.getOrElse {
                return@withContext ImportResult.Failure("Invalid backup file format: ${it.message}")
            }

            if (backup.version > BACKUP_FORMAT_VERSION) {
                return@withContext ImportResult.Failure(
                    "Backup format version ${backup.version} is not supported (max: $BACKUP_FORMAT_VERSION)",
                )
            }

            when (strategy) {
                ImportStrategy.Replace -> importReplace(backup)
                ImportStrategy.Merge -> importMerge(backup)
            }
        }
    }

    private suspend fun importReplace(backup: UserBackup): ImportResult {
        val currentInputData = userInputDao.getAll()
        val currentFavorites = favoriteDao.getAll()

        return runCatching {
            userInputDao.clearAll()
            favoriteDao.clearAll()

            backup.data.userInput.forEach { entry ->
                userInputDao.upsert(entry.toEntity())
            }
            backup.data.favorites.forEach { entry ->
                favoriteDao.upsert(entry.toEntity())
            }
            backup.data.config?.let { config ->
                configStore.updateConfig { config.restoreFromBackup(it) }
            }

            ImportResult.Success(
                importedCount = backup.data.userInput.size + backup.data.favorites.size,
                skippedCount = 0,
                conflictCount = 0,
            )
        }.getOrElse { error ->
            runCatching {
                userInputDao.clearAll()
                favoriteDao.clearAll()
                currentInputData.forEach { userInputDao.upsert(it) }
                currentFavorites.forEach { favoriteDao.upsert(it) }
            }
            ImportResult.Failure("Import failed, rolled back: ${error.message}")
        }
    }

    private suspend fun importMerge(backup: UserBackup): ImportResult {
        var importedCount = 0
        var conflictCount = 0

        backup.data.userInput.forEach { entry ->
            val existing = userInputDao.getByTextAndType(entry.text, entry.type)
            if (existing != null) {
                conflictCount++
                val merged = existing.copy(
                    freq = maxOf(existing.freq, entry.freq),
                    lastUsed = maxOf(existing.lastUsed, entry.last_used),
                )
                userInputDao.upsert(merged)
            } else {
                userInputDao.upsert(entry.toEntity())
                importedCount++
            }
        }

        backup.data.favorites.forEach { entry ->
            val existing = favoriteDao.getByText(entry.text)
            if (existing != null) {
                conflictCount++
                val merged = existing.copy(
                    usageCount = maxOf(existing.usageCount, entry.usage_count),
                )
                favoriteDao.upsert(merged)
            } else {
                favoriteDao.upsert(entry.toEntity())
                importedCount++
            }
        }

        backup.data.config?.let { config ->
            configStore.updateConfig { config.restoreFromBackup(it) }
        }

        return ImportResult.Success(
            importedCount = importedCount,
            skippedCount = 0,
            conflictCount = conflictCount,
        )
    }

    companion object {
        private const val BACKUP_FORMAT_VERSION = 1
    }
}
