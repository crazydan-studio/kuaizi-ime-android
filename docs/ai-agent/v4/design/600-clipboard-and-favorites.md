# 600 — 剪贴板与收藏系统设计

## 1. 概述

剪贴板检测和收藏管理是筷字输入法的辅助功能。Java 版本将两者混合在 `Favoriteboard` 类中，职责不清。v4 版本将其拆分为独立的剪贴板服务和收藏服务，各自拥有清晰的数据模型和业务逻辑。

---

## 2. Java 版本分析

### 2.1 Favoriteboard 的混合职责

`Favoriteboard` 同时处理：
1. **剪贴板检测**：监听系统剪贴板变化，识别文本类型（URL、邮箱、电话、验证码等）
2. **剪贴板提示**：弹出浮动提示，允许快速粘贴或提取特定数据
3. **收藏管理**：保存、粘贴、删除、清空收藏文本
4. **智能提取**：从剪贴板文本中提取 URL、邮箱、电话号码、验证码等

### 2.2 剪贴板数据类型

| 类型 | 正则模式 | 用途 |
|------|----------|------|
| URL | 标准 URL 正则 | 提取链接 |
| Email | 标准 Email 正则 | 提取邮箱地址 |
| Phone | 手机号正则 | 提取电话号码 |
| Captcha | 4-6 位数字验证码 | 提取验证码 |
| IdCard | 身份证号正则 | 提取身份证号 |
| CreditCard | 银行卡号正则 | 提取银行卡号 |
| Address | 地址关键字匹配 | 提取地址 |
| Html | 包含 HTML 标签 | HTML 内容 |

### 2.3 问题分析

1. **职责混合**：剪贴板和收藏是两个不同的功能，不应合并在一个类中
2. **剪贴板监听使用 `OnPrimaryClipChangedListener`**：需要手动注册/注销，容易遗漏
3. **收藏数据与字典耦合**：`UserInputFavoriteDict` 直接操作数据库
4. **类型检测正则分散**：类型检测逻辑分散在 `InputClip` 类和工具方法中

---

## 3. v4 设计

### 3.1 剪贴板服务

```kotlin
/**
 * 剪贴板服务，监听系统剪贴板变化并提供类型检测。
 */
class ClipboardService(
    private val clipboardManager: ClipboardManager,
    private val scope: CoroutineScope,
) {
    private val _clip = MutableStateFlow<InputClip?>(null)
    val clip: StateFlow<InputClip?> = _clip.asStateFlow()

    private val _showTip = MutableStateFlow(false)
    val showTip: StateFlow<Boolean> = _showTip

    init {
        scope.launch {
            monitorClipboard()
        }
    }

    private suspend fun monitorClipboard() {
        callbackFlow {
            val listener = ClipboardManager.OnPrimaryClipChangedListener {
                val clip = clipboardManager.primaryClip?.getItemAt(0)?.text?.toString()
                if (clip != null) {
                    trySend(InputClip.from(clip))
                }
            }
            clipboardManager.addPrimaryClipChangedListener(listener)
            awaitClose { clipboardManager.removePrimaryClipChangedListener(listener) }
        }.collect { clip ->
            _clip.value = clip
            _showTip.value = clip != null
        }
    }

    /** 粘贴剪贴板内容 */
    fun pasteClip(): String? = _clip.value?.text

    /** 提取剪贴板中的特定类型数据 */
    fun extractType(type: InputTextType): String? {
        val clip = _clip.value ?: return null
        return when (type) {
            InputTextType.Url -> URL_REGEX.find(clip.text)?.value
            InputTextType.Email -> EMAIL_REGEX.find(clip.text)?.value
            InputTextType.Phone -> PHONE_REGEX.find(clip.text)?.value
            InputTextType.Captcha -> CAPTCHA_REGEX.find(clip.text)?.value
            InputTextType.IdCard -> ID_CARD_REGEX.find(clip.text)?.value
            InputTextType.CreditCard -> CREDIT_CARD_REGEX.find(clip.text)?.value
            InputTextType.Address -> clip.text  // 地址返回全文
            InputTextType.Html -> clip.text
            InputTextType.Text -> clip.text
        }
    }

    /** 关闭剪贴板提示 */
    fun dismissTip() {
        _showTip.value = false
    }

    companion object {
        private val URL_REGEX = Regex("""https?://[^\s]+""")
        private val EMAIL_REGEX = Regex("""[\w.+-]+@[\w-]+\.[\w.-]+""")
        private val PHONE_REGEX = Regex("""1[3-9]\d{9}""")
        private val CAPTCHA_REGEX = Regex("""(?:验证码|code)[^\d]*(\d{4,6})""", RegexOption.IGNORE_CASE)
        private val ID_CARD_REGEX = Regex("""\d{17}[\dXx]""")
        private val CREDIT_CARD_REGEX = Regex("""\d{16,19}""")
    }
}
```

### 3.2 收藏服务

```kotlin
/**
 * 收藏服务，管理用户收藏的文本。
 */
class FavoriteService(
    private val favoriteDao: FavoriteDao,
    private val scope: CoroutineScope,
) {
    /** 所有收藏项（响应式） */
    val favorites: StateFlow<List<InputFavorite>> = favoriteDao.getAllFlow()
        .map { entities -> entities.map { it.toDomain() } }
        .stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** 保存文本到收藏 */
    suspend fun save(text: String, type: InputTextType? = null) {
        val existing = favoriteDao.getByText(text)
        if (existing != null) {
            favoriteDao.upsert(existing.copy(usageCount = existing.usageCount + 1))
        } else {
            favoriteDao.upsert(
                FavoriteEntity(
                    text = text,
                    type = type?.name,
                    usageCount = 1,
                    createdAt = System.currentTimeMillis(),
                )
            )
        }
    }

    /** 删除收藏 */
    suspend fun delete(text: String) {
        favoriteDao.delete(text)
    }

    /** 清空所有收藏 */
    suspend fun clearAll() {
        favoriteDao.clearAll()
    }

    /** 粘贴收藏项 */
    fun paste(favorite: InputFavorite): String = favorite.text
}

data class InputFavorite(
    val text: String,
    val type: InputTextType?,
    val usageCount: Int,
    val createdAt: Long,
)
```

### 3.3 剪贴板提示 UI

```kotlin
@Composable
fun ClipTipPopup(
    clip: InputClip?,
    show: Boolean,
    onPaste: (String) -> Unit,
    onExtractType: (InputTextType, String) -> Unit,
    onDismiss: () -> Unit,
    onFavorite: (String) -> Unit,
) {
    if (!show || clip == null) return

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // 剪贴板内容预览
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = clip.text,
                    maxLines = 2,
                    overflow = TextOverflow.MiddleEllipsis,
                    style = MaterialTheme.typography.bodySmall,
                )

                // 类型标签
                clip.type?.let { type ->
                    SuggestionChip(
                        onClick = { onExtractType(type, extractContent(clip, type)) },
                        label = { Text(typeLabel(type)) },
                    )
                }
            }

            // 操作按钮
            IconButton(onClick = { onPaste(clip.text) }) {
                Icon(Icons.Default.ContentPaste, "粘贴")
            }
            IconButton(onClick = { onFavorite(clip.text) }) {
                Icon(Icons.Default.FavoriteBorder, "收藏")
            }
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, "关闭")
            }
        }
    }
}
```

### 3.4 收藏列表面板

```kotlin
@Composable
fun FavoritesPanel(
    favorites: List<InputFavorite>,
    onPaste: (InputFavorite) -> Unit,
    onDelete: (String) -> Unit,
    onClearAll: () -> Unit,
) {
    Column {
        // 顶部工具栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("收藏", style = MaterialTheme.typography.titleSmall)
            TextButton(onClick = onClearAll) {
                Text("清空")
            }
        }

        Divider()

        // 收藏列表
        LazyColumn {
            items(items = favorites, key = { it.text }) { favorite ->
                FavoriteItem(
                    favorite = favorite,
                    onPaste = { onPaste(favorite) },
                    onDelete = { onDelete(favorite.text) },
                )
            }
        }
    }
}

@Composable
fun FavoriteItem(
    favorite: InputFavorite,
    onPaste: () -> Unit,
    onDelete: () -> Unit,
) {
    SwipeToDismiss(
        state = rememberSwipeToDismissBoxState(),
        background = { /* 删除背景 */ },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
        ) {
            Text(
                text = favorite.text,
                modifier = Modifier.weight(1f),
                maxLines = 2,
                overflow = TextOverflow.MiddleEllipsis,
            )
            IconButton(onClick = onPaste) {
                Icon(Icons.Default.ContentPaste, "粘贴")
            }
        }
    }
}
```

---

## 4. Java 功能完整对照

| Java 剪贴板/收藏功能 | v4 对应 | 改进说明 |
|--------------------|---------|---------|
| `Favoriteboard`（混合职责） | `ClipboardService` + `FavoriteService` | 职责分离 |
| `ClipboardManager.OnPrimaryClipChangedListener` | `callbackFlow` 封装 | 协程化，自动注销 |
| `InputClip` + `InputTextType` 枚举 | `InputClip.from()` + `InputTextType.detect()` | 类型安全的自动检测 |
| 正则提取（分散在多处） | `ClipboardService.extractType()` | 集中管理 |
| `UserInputFavoriteDict` | `FavoriteService` + `FavoriteDao` | Room DAO + Flow |
| 剪贴板浮动提示（自定义 View） | `ClipTipPopup` (Compose) | 声明式 UI |
| 收藏面板（自定义 RecyclerView） | `FavoritesPanel` + `LazyColumn` | Compose 简化 |
| 收藏删除（手动按钮） | `SwipeToDismiss` | 滑动删除 |
| 收藏使用计数 | `FavoriteEntity.usageCount` | 保留 |
| 收藏保存（从输入/剪贴板） | `FavoriteService.save()` | 统一入口 |
| 用户数据备份 | ❌ 不支持 | `UserDataService`（见 [800-用户数据导入导出设计](800-user-data-import-export.md)） |
