# 800 — 用户数据导入导出设计

## 1. 概述

v4 版本新增用户数据的导入与导出功能，允许用户将输入历史、收藏列表等个人数据导出为文件备份，以及从文件中导入恢复。这是 Java 版本不具备的新增功能，目的是保护用户数据安全，方便换机迁移和灾难恢复。

---

## 2. 功能需求

### 2.1 导出功能

- 将用户数据导出为单个文件
- 导出内容包括：
  - 用户输入频率数据（`user_input_data` 表）
  - 收藏列表（`user_input_favorite` 表）
  - 应用配置（DataStore 中的配置项）
- 导出文件格式为 JSON，便于人工检视和跨版本兼容
- 导出文件存储到用户指定的位置（通过系统文件选择器）
- 导出文件名格式：`kuaizi_ime_backup_{YYYYMMDD_HHmmss}.json`

### 2.2 导入功能

- 从用户选择的文件中导入数据
- 导入前校验文件格式和版本兼容性
- 导入策略选择：
  - **替换**：清除现有数据后导入（默认）
  - **合并**：将导入数据与现有数据合并，相同条目取较高频率/较新时间
- 导入完成后提示导入结果摘要（导入条数、跳过条数、冲突条数）
- 导入失败时自动回滚，不破坏现有数据

### 2.3 导出文件格式

```json
{
  "version": 1,
  "app_version": "4.0.0",
  "exported_at": "2026-05-12T10:30:00+08:00",
  "data": {
    "user_input": [
      { "text": "你好", "type": "pinyin", "freq": 42, "last_used": 1715472000000 },
      { "text": "hello", "type": "latin", "freq": 15, "last_used": 1715471000000 }
    ],
    "favorites": [
      { "text": "example@email.com", "type": "Email", "usage_count": 3, "created_at": 1715470000000 }
    ],
    "config": {
      "theme_type": "FollowSystem",
      "hand_mode": "Right",
      "enable_x_pad": true,
      "enable_candidate_variant_first": false,
      "disable_user_input_data": false,
      "disable_key_clicked_audio": false,
      "disable_key_animation": false,
      "disable_candidates_paging_audio": false,
      "disable_key_popup_tips": false,
      "disable_gesture_slipping_trail": false,
      "disable_clip_popup_tips": false,
      "clip_popup_tips_timeout": 15
    }
  }
}
```

> **设计说明**：
> - `version` 为数据格式版本号，便于后续格式变更时做兼容处理
> - `app_version` 记录导出时的应用版本，用于排查兼容性问题
> - 拼音字典数据（`pinyin_word`、`pinyin_phrase`、`hmm_data`）为内置数据，不纳入导出范围
> - `config` 中的字段均为可选，仅导出与默认值不同的配置项；日志与诊断、输入练习演示相关字段为 v4 新增
> - 导入时，备份文件中不存在的可选字段使用 `null` 默认值，不影响现有配置

---

## 3. 架构设计

### 3.1 UserDataService

```kotlin
/**
 * 用户数据导入导出服务。
 *
 * 负责将用户数据序列化为 JSON 文件以及从 JSON 文件反序列化恢复数据。
 * 所有操作均为协程化的异步操作，避免阻塞主线程。
 */
class UserDataService(
    private val userInputDao: UserInputDao,
    private val favoriteDao: FavoriteDao,
    private val configRepository: ConfigRepository,
    private val json: Json,
    private val scope: CoroutineScope,
) {
    /**
     * 导出用户数据到指定 URI。
     *
     * @param context Android Context，用于访问 ContentResolver
     * @param uri 目标文件 URI（由系统文件选择器返回）
     * @return 导出结果
     */
    suspend fun exportTo(context: Context, uri: Uri): ExportResult {
        val userInputData = userInputDao.getAll()
        val favoriteData = favoriteDao.getAll()
        val configData = configRepository.config.first()

        val backup = UserBackup(
            version = BACKUP_FORMAT_VERSION,
            appVersion = BuildConfig.VERSION_NAME,
            exportedAt = Clock.System.now().toString(),
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

    /**
     * 从指定 URI 导入用户数据。
     *
     * @param context Android Context
     * @param uri 源文件 URI
     * @param strategy 导入策略
     * @return 导入结果
     */
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

            // 版本兼容性检查
            if (backup.version > BACKUP_FORMAT_VERSION) {
                return@withContext ImportResult.Failure(
                    "Backup format version ${backup.version} is not supported (max: $BACKUP_FORMAT_VERSION)"
                )
            }

            // 按策略执行导入
            when (strategy) {
                ImportStrategy.Replace -> importReplace(backup)
                ImportStrategy.Merge -> importMerge(backup)
            }
        }
    }

    private suspend fun importReplace(backup: UserBackup): ImportResult {
        // 先保存当前数据用于回滚
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
                configRepository.updateConfig { config.restoreFromBackup(it) }
            }

            ImportResult.Success(
                importedCount = backup.data.userInput.size + backup.data.favorites.size,
                skippedCount = 0,
                conflictCount = 0,
            )
        }.getOrElse { error ->
            // 回滚
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
                // 取较高频率和较新时间
                val merged = existing.copy(
                    freq = maxOf(existing.freq, entry.freq),
                    lastUsed = maxOf(existing.lastUsed, entry.lastUsed),
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
                    usageCount = maxOf(existing.usageCount, entry.usageCount),
                )
                favoriteDao.upsert(merged)
            } else {
                favoriteDao.upsert(entry.toEntity())
                importedCount++
            }
        }

        backup.data.config?.let { config ->
            configRepository.updateConfig { config.restoreFromBackup(it) }
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
```

### 3.2 数据模型

```kotlin
@Serializable
data class UserBackup(
    val version: Int,
    val appVersion: String,
    val exportedAt: String,
    val data: BackupData,
)

@Serializable
data class BackupData(
    val userInput: List<UserInputBackupEntry>,
    val favorites: List<FavoriteBackupEntry>,
    val config: ConfigBackupEntry? = null,
)

@Serializable
data class UserInputBackupEntry(
    val text: String,
    val type: String,
    val freq: Int,
    val last_used: Long,
)

@Serializable
data class FavoriteBackupEntry(
    val text: String,
    val type: String? = null,
    val usage_count: Int,
    val created_at: Long,
)

@Serializable
data class ConfigBackupEntry(
    // 外观
    val theme_type: String? = null,
    val hand_mode: String? = null,
    // 输入体验
    val enable_x_pad: Boolean? = null,
    val enable_latin_use_pinyin_keys_in_x_pad: Boolean? = null,
    val adapt_desktop_swipe_up_gesture: Boolean? = null,
    val enable_candidate_variant_first: Boolean? = null,
    // 隐私
    val disable_user_input_data: Boolean? = null,
    // 反馈控制
    val disable_key_clicked_audio: Boolean? = null,
    val disable_key_animation: Boolean? = null,
    val disable_candidates_paging_audio: Boolean? = null,
    val disable_key_popup_tips: Boolean? = null,
    val disable_gesture_slipping_trail: Boolean? = null,
    val disable_clip_popup_tips: Boolean? = null,
    val clip_popup_tips_timeout: Int? = null,
    // 日志与诊断
    val log_level: String? = null,
    val log_storage_path: String? = null,
    // 输入练习演示
    val practice_playback_speed: Float? = null,
    val practice_show_finger_overlay: Boolean? = null,
    val practice_show_swipe_trail: Boolean? = null,
)
```

### 3.3 结果类型

```kotlin
sealed class ExportResult {
    data class Success(val itemCount: Int) : ExportResult()
    data class Failure(val message: String) : ExportResult()
}

sealed class ImportResult {
    data class Success(
        val importedCount: Int,
        val skippedCount: Int,
        val conflictCount: Int,
    ) : ImportResult()

    data class Failure(val message: String) : ImportResult()
}

enum class ImportStrategy {
    Replace,  // 替换：清除现有数据后导入
    Merge,    // 合并：与现有数据合并
}
```

### 3.4 Intent 扩展

```kotlin
sealed class ImeIntent {
    // ... 已有 Intent ...

    // 用户数据导入导出
    data class ExportUserData(val uri: Uri) : ImeIntent()
    data class ImportUserData(val uri: Uri, val strategy: ImportStrategy) : ImeIntent()
}
```

---

## 4. UI 设计

### 4.1 设置页面中的入口

```kotlin
@Composable
fun DataManagementSection(
    onExport: () -> Unit,
    onImport: () -> Unit,
) {
    SettingsSectionHeader("数据管理")

    ClickablePreference(
        title = "导出用户数据",
        description = "将输入历史、收藏和设置导出为备份文件",
        onClick = onExport,
    )

    ClickablePreference(
        title = "导入用户数据",
        description = "从备份文件恢复输入历史、收藏和设置",
        onClick = onImport,
    )
}
```

### 4.2 文件选择

```kotlin
// 导出：使用 Activity Result API 创建文件
val createFileLauncher = rememberLauncherForActivityResult(
    ActivityResultContracts.CreateDocument("application/json")
) { uri ->
    uri?.let { viewModel.handleIntent(ImeIntent.ExportUserData(it)) }
}

// 导入：使用 Activity Result API 打开文件
val openFileLauncher = rememberLauncherForActivityResult(
    ActivityResultContracts.OpenDocument(arrayOf("application/json"))
) { uri ->
    uri?.let {
        // 显示导入策略选择对话框
        showImportStrategyDialog = true
        selectedImportUri = it
    }
}
```

### 4.3 导入策略选择对话框

```kotlin
@Composable
fun ImportStrategyDialog(
    onStrategySelected: (ImportStrategy) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("导入策略") },
        text = {
            Column {
                Text("请选择数据导入方式：")
                Spacer(modifier = Modifier.height(16.dp))
                // 替换选项
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = true, onClick = { /* ... */ })
                    Column {
                        Text("替换现有数据", fontWeight = FontWeight.Bold)
                        Text("清除当前所有数据后导入备份数据", style = bodySmall)
                    }
                }
                // 合并选项
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = false, onClick = { /* ... */ })
                    Column {
                        Text("与现有数据合并", fontWeight = FontWeight.Bold)
                        Text("保留现有数据，相同条目取较高频率", style = bodySmall)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onStrategySelected(ImportStrategy.Replace) }) {
                Text("确认导入")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}
```

---

## 5. 权限与安全

### 5.1 权限

- **不需要** `WRITE_EXTERNAL_STORAGE` 或 `READ_EXTERNAL_STORAGE` 权限
- 使用 SAF（Storage Access Framework）的 `CreateDocument` 和 `OpenDocument`，系统自动处理权限

### 5.2 数据安全

- 导出文件为明文 JSON，不包含加密
- 导出文件中不包含拼音字典数据（为应用内置数据）
- 导入时验证文件格式版本，不兼容的版本拒绝导入
- 导入失败自动回滚，确保数据完整性

---

## 6. 与 Java 版本的差异

| 维度 | Java v3 | Kotlin v4 |
|------|---------|-----------|
| 数据导出 | ❌ 不支持 | ✅ 支持 JSON 格式导出 |
| 数据导入 | ❌ 不支持 | ✅ 支持替换/合并两种导入策略 |
| 换机迁移 | 手动复制数据库文件 | 系统文件选择器导出/导入 |
| 数据备份 | 无 | 用户可随时导出备份 |
| 导入回滚 | 无 | 导入失败自动回滚 |
