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

package org.crazydan.studio.app.ime.kuaizi.service

/**
 * 用户数据导入导出服务。
 *
 * 负责将用户数据（输入频率、收藏、配置）序列化为 JSON 文件，
 * 以及从 JSON 文件反序列化恢复数据。
 *
 * 导出使用 SAF [CreateDocument] 合约让用户选择保存位置，
 * 导入使用 [OpenDocument] 合约选择备份文件。
 * 支持替换（Replace）和合并（Merge）两种导入策略。
 */
class UserDataService(
//    /** 用户输入数据 DAO */
//    private val userInputDao: UserInputDao,
//    /** 收藏数据 DAO */
//    private val favoriteDao: FavoriteDao,
//    /** 配置存储，用于导入/导出配置数据 */
//    private val configStore: ConfigDataStore,
//    /** JSON 序列化器 */
//    private val json: Json,
//    /** 协程作用域 */
//    private val scope: CoroutineScope,
) {
//    /**
//     * 导出用户数据到指定 URI。
//     *
//     * 将用户输入历史、收藏列表和配置序列化为 JSON，
//     * 通过 ContentResolver 写入目标文件。
//     *
//     * @return 导出结果，包含成功数量或失败信息
//     */
//    suspend fun exportTo(context: Context, uri: Uri): ExportResult {
//        // 收集所有需要导出的数据
//        val userInputData = userInputDao.getAll()
//        val favoriteData = favoriteDao.getAll()
//        val configData = configStore.config.first()
//
//        // 构建备份数据结构
//        val backup = UserBackup(
//            version = BACKUP_FORMAT_VERSION,
//            appVersion = BuildConfig.VERSION_NAME,
//            exportedAt = java.time.Clock.System.now().toString(),
//            data = BackupData(
//                userInput = userInputData.map { it.toBackupEntry() },
//                favorites = favoriteData.map { it.toBackupEntry() },
//                config = configData.toBackupEntry(),
//            ),
//        )
//
//        // 序列化为 JSON 字符串
//        val jsonString = json.encodeToString(backup)
//
//        // 在 IO 线程写入文件
//        return withContext(Dispatchers.IO) {
//            runCatching {
//                context.contentResolver.openOutputStream(uri)?.use { output ->
//                    output.write(jsonString.toByteArray(Charsets.UTF_8))
//                } ?: error("Failed to open output stream for URI: $uri")
//            }.fold(
//                onSuccess = { ExportResult.Success(itemCount = userInputData.size + favoriteData.size) },
//                onFailure = { ExportResult.Failure(it.message ?: "Unknown error") },
//            )
//        }
//    }
//
//    /**
//     * 从指定 URI 导入用户数据。
//     *
//     * 从文件读取 JSON，解析为 [UserBackup]，验证版本兼容性后，
//     * 根据 [ImportStrategy] 执行替换或合并导入。
//     *
//     * @param strategy 导入策略：Replace 替换现有数据，Merge 与现有数据合并
//     * @return 导入结果
//     */
//    suspend fun importFrom(
//        context: Context,
//        uri: Uri,
//        strategy: ImportStrategy = ImportStrategy.Replace,
//    ): ImportResult {
//        return withContext(Dispatchers.IO) {
//            // 从 URI 读取 JSON 字符串
//            val jsonString = runCatching {
//                context.contentResolver.openInputStream(uri)?.use { input ->
//                    input.readBytes().toString(Charsets.UTF_8)
//                } ?: error("Failed to open input stream for URI: $uri")
//            }.getOrElse {
//                return@withContext ImportResult.Failure(it.message ?: "Failed to read file")
//            }
//
//            // 解析 JSON 为 UserBackup
//            val backup = runCatching {
//                json.decodeFromString<UserBackup>(jsonString)
//            }.getOrElse {
//                return@withContext ImportResult.Failure("Invalid backup file format: ${it.message}")
//            }
//
//            // 验证版本兼容性：只支持不高于当前版本的格式
//            if (backup.version > BACKUP_FORMAT_VERSION) {
//                return@withContext ImportResult.Failure(
//                    "Backup format version ${backup.version} is not supported (max: $BACKUP_FORMAT_VERSION)",
//                )
//            }
//
//            // 按策略执行导入
//            when (strategy) {
//                ImportStrategy.Replace -> importReplace(backup)
//                ImportStrategy.Merge -> importMerge(backup)
//            }
//        }
//    }
//
//    /**
//     * 替换策略导入。
//     *
//     * 先备份当前数据，清空后写入备份数据，
//     * 若写入失败则自动回滚到备份状态。
//     */
//    private suspend fun importReplace(backup: UserBackup): ImportResult {
//        // 备份当前数据，用于失败回滚
//        val currentInputData = userInputDao.getAll()
//        val currentFavorites = favoriteDao.getAll()
//
//        return runCatching {
//            // 清空现有数据
//            userInputDao.clearAll()
//            favoriteDao.clearAll()
//
//            // 写入备份数据
//            backup.data.userInput.forEach { entry ->
//                userInputDao.upsert(entry.toEntity())
//            }
//            backup.data.favorites.forEach { entry ->
//                favoriteDao.upsert(entry.toEntity())
//            }
//            // 恢复配置
//            backup.data.config?.let { config ->
//                configStore.updateConfig { config.restoreFromBackup(it) }
//            }
//
//            ImportResult.Success(
//                importedCount = backup.data.userInput.size + backup.data.favorites.size,
//                skippedCount = 0,
//                conflictCount = 0,
//            )
//        }.getOrElse { error ->
//            // 导入失败：回滚到备份数据
//            runCatching {
//                userInputDao.clearAll()
//                favoriteDao.clearAll()
//                currentInputData.forEach { userInputDao.upsert(it) }
//                currentFavorites.forEach { favoriteDao.upsert(it) }
//            }
//            ImportResult.Failure("Import failed, rolled back: ${error.message}")
//        }
//    }
//
//    /**
//     * 合并策略导入。
//     *
//     * 保留现有数据，对已存在的条目取较高使用频率和时间，
//     * 不存在的条目作为新条目导入。
//     */
//    private suspend fun importMerge(backup: UserBackup): ImportResult {
//        var importedCount = 0
//        var conflictCount = 0
//
//        // 合并用户输入数据
//        backup.data.userInput.forEach { entry ->
//            val existing = userInputDao.getByTextAndType(entry.text, entry.type)
//            if (existing != null) {
//                // 已存在：取较高频率和较新的时间戳
//                conflictCount++
//                val merged = existing.copy(
//                    freq = maxOf(existing.freq, entry.freq),
//                    lastUsed = maxOf(existing.lastUsed, entry.last_used),
//                )
//                userInputDao.upsert(merged)
//            } else {
//                // 不存在：新建条目
//                userInputDao.upsert(entry.toEntity())
//                importedCount++
//            }
//        }
//
//        // 合并收藏数据
//        backup.data.favorites.forEach { entry ->
//            val existing = favoriteDao.getByText(entry.text)
//            if (existing != null) {
//                // 已存在：取较高使用次数
//                conflictCount++
//                val merged = existing.copy(
//                    usageCount = maxOf(existing.usageCount, entry.usage_count),
//                )
//                favoriteDao.upsert(merged)
//            } else {
//                // 不存在：新建条目
//                favoriteDao.upsert(entry.toEntity())
//                importedCount++
//            }
//        }
//
//        // 恢复配置
//        backup.data.config?.let { config ->
//            configStore.updateConfig { config.restoreFromBackup(it) }
//        }
//
//        return ImportResult.Success(
//            importedCount = importedCount,
//            skippedCount = 0,
//            conflictCount = conflictCount,
//        )
//    }
//
//    companion object {
//        /** 当前备份格式版本号，用于向后兼容性检测 */
//        private const val BACKUP_FORMAT_VERSION = 1
//    }
}
