# 剪贴板与收藏

## 1. 概述

剪贴板检测和收藏管理是筷字输入法的辅助功能。v4 将两者拆分为独立的剪贴板服务和收藏服务，各自拥有清晰的数据模型和业务逻辑。

> 本文档仅涵盖 `:ime-engine` 模块中的服务层和数据模型。UI 组件（`ClipTipPopup`、`FavoriteListPanel`）属于 `:ime-ui` 模块，不在本文档范围内。

---

## 2. InputClip 数据模型

```kotlin
data class InputClip(
    val text: String,
    val type: InputTextType? = null,
) {
    /** 根据文本内容自动检测类型 */
    companion object {
        fun from(text: String): InputClip {
            val type = InputTextType.detect(text)
            return InputClip(text, type)
        }
    }
}

enum class InputTextType {
    Text, Url, Email, Phone, Captcha, IdCard, CreditCard, Address, Html;

    companion object {
        fun detect(text: String): InputTextType? = when {
            URL_REGEX.matches(text) -> Url
            EMAIL_REGEX.matches(text) -> Email
            PHONE_REGEX.matches(text) -> Phone
            CAPTCHA_REGEX.matches(text) -> Captcha
            ID_CARD_REGEX.matches(text) -> IdCard
            CREDIT_CARD_REGEX.matches(text) -> CreditCard
            text.contains("<") && text.contains(">") -> Html
            else -> null
        }
    }
}
```

### 支持的文本类型检测

| 类型 | 说明 |
|------|------|
| `Url` | URL 链接 |
| `Email` | 邮箱地址 |
| `Phone` | 手机号 |
| `Captcha` | 4-6 位数字验证码 |
| `IdCard` | 身份证号 |
| `CreditCard` | 银行卡号 |
| `Address` | 地址（关键字匹配） |
| `Html` | HTML 内容 |

---

## 3. ClipboardService 剪贴板服务

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
            InputTextType.Address -> clip.text
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

---

## 4. InputFavorite 数据模型

```kotlin
data class InputFavorite(
    val text: String,
    val type: InputTextType?,
    val usageCount: Int,
    val createdAt: Long,
)
```

---

## 5. FavoriteService 收藏服务

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
```
