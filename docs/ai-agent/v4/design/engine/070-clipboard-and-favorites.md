# 剪贴板与收藏

## 1. InputClip 剪贴内容

`InputClip` 是剪贴板条目的核心数据模型，封装了系统剪贴板中提取的文本内容及其自动检测的语义类型。作为 `:engine` 模块中剪贴板子系统的基础数据单元，`InputClip` 被设计为不可变的 `data class`，所有字段在构造时确定，不存在可变状态。`text` 字段存储从系统剪贴板提取的原始文本字符串，`type` 字段通过 `InputTextType.detect()` 自动检测并标注文本的语义类型，为下游服务（如 `ClipboardService`）和 UI 层提供类型信息，以便选择合适的展示图标和操作按钮。

`InputClip` 的伴生对象提供了 `from()` 工厂方法，这是创建 `InputClip` 实例的推荐入口。`from()` 方法接收原始文本字符串，内部调用 `InputTextType.detect()` 执行类型检测，将结果作为 `type` 字段传入构造函数。这种设计将类型检测逻辑与实例构造绑定在一起，确保每个通过 `from()` 创建的 `InputClip` 都携带正确的语义类型标注，避免调用方遗漏类型检测步骤。

```kotlin
data class InputClip(
    val text: String,
    val type: InputTextType? = null,
) {
    companion object {
        /** 根据文本内容自动检测类型并创建 InputClip 实例 */
        suspend fun from(text: String): InputClip = withContext(Dispatchers.Default) {
            InputClip(text = text, type = InputTextType.detect(text))
        }
    }
}
```

`type` 字段为可空类型 `InputTextType?`，当 `detect()` 无法匹配任何已知类型时返回 `null`，表示该文本为普通文本。UI 层在 `type` 为 `null` 时按普通文本渲染，不显示类型特有的图标或操作按钮。`InputClip` 的 `equals()` 和 `hashCode()` 由 `data class` 自动生成，基于 `text` 和 `type` 两个字段，确保内容相同的剪贴条目在集合操作中被正确判等。

---

## 2. InputTextType 文本类型检测

`InputTextType` 枚举定义了剪贴板文本的语义类型体系，涵盖九种常见的文本模式。`detect()` 方法通过正则表达式优先级链实现自动类型检测，每条正则针对一种文本模式精心设计，在特异性与召回率之间取得平衡。优先级链的排列遵循「特异性优先」原则——模式越窄、误匹配风险越低的类型排在越前面，确保高特异性类型不会被低特异性类型的正则提前捕获。

### 2.1 枚举定义与优先级链

```kotlin
enum class InputTextType {
    Text,       // 普通文本（最低优先级，兜底类型）
    Url,        // URL 链接
    Email,      // 邮箱地址
    Phone,      // 手机号码
    Captcha,    // 验证码
    IdCard,     // 身份证号
    CreditCard, // 银行卡号
    Address,    // 地址信息
    Html;       // HTML 内容

    companion object {
        fun detect(text: String): InputTextType? {
            // 优先级链：特异性高的类型优先匹配
            return when {
                CAPTCHA_REGEX.matches(text) -> Captcha
                CREDIT_CARD_REGEX.matches(text) -> CreditCard
                ID_CARD_REGEX.matches(text) -> IdCard
                PHONE_REGEX.matches(text) -> Phone
                EMAIL_REGEX.matches(text) -> Email
                URL_REGEX.matches(text) -> Url
                ADDRESS_KEYWORD_REGEX.containsMatchIn(text) -> Address
                HTML_TAG_REGEX.containsMatchIn(text) -> Html
                else -> null
            }
        }

        private val URL_REGEX = Regex("""https?://[^\s]+""")
        private val EMAIL_REGEX = Regex("""[\w.+-]+@[\w-]+\.[\w.-]+""")
        private val PHONE_REGEX = Regex("""1[3-9]\d{9}""")
        private val CAPTCHA_REGEX = Regex(
            """(?:验证码|code|码)[^\d]*(\d{4,6})""",
            RegexOption.IGNORE_CASE,
        )
        private val ID_CARD_REGEX = Regex("""\d{17}[\dXx]""")
        private val CREDIT_CARD_REGEX = Regex("""\d{16,19}""")
        private val ADDRESS_KEYWORD_REGEX = Regex(
            """(?:省|市|区|县|镇|乡|村|路|街|道|号|栋|楼|室|小区|花园|广场)""",
        )
        private val HTML_TAG_REGEX = Regex("""<[^>]+>""")
    }
}
```

### 2.2 优先级链设计原理

优先级链的排列顺序基于以下原则：**模式越窄、误匹配风险越低的类型排在越前面**。

| 优先级 | 类型 | 正则模式 | 排位理由 |
|--------|------|----------|----------|
| 1 | `Captcha` | 验证码关键字 + 4-6 位数字 | 最特异的模式：必须同时包含关键字和短数字序列，极低误匹配率 |
| 2 | `CreditCard` | 16-19 位纯数字 | 较特异：长度约束严格 |
| 3 | `IdCard` | 17 位数字 + 校验位 | 较特异：末位可为 X/x |
| 4 | `Phone` | 1 开头 11 位数字 | 中等特异：中国手机号的格式约束明确 |
| 5 | `Email` | 含 @ 符号的邮箱格式 | 中等特异：@ 符号是强特征 |
| 6 | `Url` | http(s):// 开头的链接 | 较宽泛：URL 格式变化多端，但协议前缀是强特征 |
| 7 | `Address` | 包含地址关键字 | 宽泛：基于关键字匹配，可能产生误匹配 |
| 8 | `Html` | 包含 HTML 标签 | 宽泛：仅检查尖括号标签模式 |
| — | `null` | 不匹配任何模式 | 最宽泛的兜底 |

当 `detect()` 无法将文本匹配到任何特定类型时，返回 `null` 而非 `InputTextType.Text`。这种设计使得调用方可以明确区分「检测到普通文本」和「未检测到特定类型」两种语义——`null` 表示无类型标注，UI 层按普通文本处理；`InputTextType.Text` 作为枚举成员保留，供需要显式标注普通文本的场景使用。

### 2.3 检测结果的 UI 影响

| 类型 | UI 图标 | 点击操作 | 提取行为 |
|------|---------|----------|----------|
| `Captcha` | 验证码图标 | 提取验证码数字并粘贴 | 从文本中提取纯数字部分 |
| `CreditCard` | 银行卡图标 | 粘贴完整卡号 | 直接粘贴全部数字 |
| `IdCard` | 证件图标 | 粘贴完整身份证号 | 直接粘贴全部内容 |
| `Phone` | 电话图标 | 粘贴电话号码 | 直接粘贴全部数字 |
| `Email` | 邮箱图标 | 粘贴邮箱地址 | 直接粘贴全部内容 |
| `Url` | 链接图标 | 粘贴完整链接 | 直接粘贴全部内容 |
| `Address` | 地址图标 | 粘贴地址文本 | 直接粘贴全部内容 |
| `Html` | 代码图标 | 粘贴 HTML 源码 | 直接粘贴全部内容 |
| `null` | 无图标 | 粘贴原始文本 | 直接粘贴全部内容 |

`InputTextType.detect()` 包含 9 个按优先级排序的正则匹配（验证码、信用卡、身份证、手机号等），运行在 `Dispatchers.Default` 上避免阻塞主线程。

---

## 3. ClipboardService 剪贴板服务

`ClipboardService` 是剪贴板子系统的核心服务类，负责监听系统剪贴板变更、维护当前剪贴内容状态、提供类型化提取能力以及控制提示显示。该服务通过 `ClipboardManager.OnPrimaryClipChangedListener` 监听系统剪贴板的变化事件，每当用户在其他应用中复制文本时，监听器被触发，服务从 `ClipboardManager` 读取最新的剪贴内容，通过 `InputClip.from()` 创建带类型标注的 `InputClip` 实例，并更新内部的 `StateFlow` 状态。

```kotlin
class ClipboardService(
    private val clipboardManager: ClipboardManager,
    private val scope: CoroutineScope,
) {
    private val _clip = MutableStateFlow<InputClip?>(null)
    val clip: StateFlow<InputClip?> = _clip.asStateFlow()

    private val _showTip = MutableStateFlow(false)
    val showTip: StateFlow<Boolean> = _showTip.asStateFlow()

    init {
        scope.launch {
            monitorClipboard()
        }
    }

    /**
     * 监听系统剪贴板变更。
     *
     * 通过 callbackFlow 将 OnPrimaryClipChangedListener 回调
     * 转换为 Flow<InputClip>，在协程中持续收集。
     */
    private suspend fun monitorClipboard() {
        callbackFlow {
            val listener = ClipboardManager.OnPrimaryClipChangedListener {
                val clip = clipboardManager.primaryClip
                    ?.getItemAt(0)?.text?.toString()
                if (clip != null) {
                    scope.launch(Dispatchers.Default) {
                        val inputClip = InputClip.from(clip)
                        trySend(inputClip)
                    }
                }
            }
            clipboardManager.addPrimaryClipChangedListener(listener)
            awaitClose {
                clipboardManager.removePrimaryClipChangedListener(listener)
            }
        }.collect { clip ->
            _clip.value = clip
            _showTip.value = clip != null
        }
    }

    /** 粘贴剪贴板内容，返回当前剪贴文本或 null */
    fun pasteClip(): String? = _clip.value?.text

    /**
     * 提取剪贴板中的特定类型数据。
     *
     * 当剪贴内容的类型与请求类型匹配时，从文本中提取
     * 对应的数据片段（如验证码中的数字部分）。
     * 类型不匹配或无剪贴内容时返回 null。
     */
    fun extractType(type: InputTextType): String? {
        val clip = _clip.value ?: return null
        return when (type) {
            InputTextType.Captcha -> CAPTCHA_REGEX.find(clip.text)?.groupValues?.get(1)
            InputTextType.Url -> URL_REGEX.find(clip.text)?.value
            InputTextType.Email -> EMAIL_REGEX.find(clip.text)?.value
            InputTextType.Phone -> PHONE_REGEX.find(clip.text)?.value
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
        private val CAPTCHA_REGEX = Regex(
            """(?:验证码|code|码)[^\d]*(\d{4,6})""",
            RegexOption.IGNORE_CASE,
        )
        private val ID_CARD_REGEX = Regex("""\d{17}[\dXx]""")
        private val CREDIT_CARD_REGEX = Regex("""\d{16,19}""")
    }
}
```

### 3.1 监听机制与生命周期

`ClipboardService` 通过 `callbackFlow` 将 Android 的 `OnPrimaryClipChangedListener` 回调转换为 Kotlin 协程 `Flow<InputClip>`，在注入的 `CoroutineScope` 中持续收集。`callbackFlow` 的 `awaitClose` 块确保协程取消时移除监听器，避免内存泄漏。监听器的注册和移除操作在主线程执行，符合 `ClipboardManager` 的线程要求。

服务的生命周期由外部注入的 `CoroutineScope` 控制。在 `:app` 模块中，通常传入 `ProcessLifecycleOwner` 的 `scope`，确保服务在整个应用生命周期内持续监听。当 `scope` 被取消时（如应用进程退出），`callbackFlow` 自动关闭，监听器被移除。

### 3.2 状态暴露与线程安全

`ClipboardService` 暴露两个只读 `StateFlow`：`clip` 表示当前剪贴内容（可空），`showTip` 表示是否应显示剪贴板提示。`StateFlow` 的值语义保证了线程安全——`value` 的读写是原子的，订阅者始终读取到最新的完整状态快照。`pasteClip()` 方法返回当前剪贴文本，供 `ImeEngine` 在处理 `ImeIntent.PasteClip` 时获取粘贴内容。`extractType()` 方法根据请求的类型从当前剪贴内容中提取特定格式的数据片段。

---

## 4. InputFavorite 收藏项

`InputFavorite` 是收藏条目的核心数据模型，描述用户保存的常用文本片段及其元数据。与 `InputClip` 的瞬时性不同，`InputFavorite` 是持久化的数据实体，存储在 Room 数据库中，跨应用会话存活。`text` 字段为收藏的文本内容，作为收藏条目的唯一标识——相同 `text` 的收藏不会重复创建，而是递增 `usageCount` 并更新时间戳。`type` 字段复用 `InputTextType` 的类型体系，与 `InputClip` 保持一致的类型语义，使 UI 层可以统一处理剪贴条目和收藏条目的类型展示逻辑。

```kotlin
data class InputFavorite(
    val text: String,
    val type: InputTextType?,
    val usageCount: Int,
    val createdAt: Long,
)
```

`usageCount` 记录用户使用（粘贴）该收藏条的次数，用于排序策略——高频使用的收藏条目排在列表前面，降低用户的查找成本。`createdAt` 记录收藏创建的时间戳（毫秒），用于在 `usageCount` 相同时按时间排序，确保新收藏不会因使用频次低而被完全淹没。排序策略为 `usageCount` 降序优先，`createdAt` 降序次之，形成「常用优先、新收藏次之」的排列效果。

`InputFavorite` 与数据库实体 `FavoriteEntity` 之间通过 `toDomain()` 扩展函数进行映射，`FavoriteEntity` 包含数据库特有的字段（如主键 `id`），而 `InputFavorite` 是纯领域模型，不包含任何持久化细节。这种映射层确保了领域模型与数据层的解耦——`FavoriteService` 只操作 `InputFavorite`，数据库实现细节封装在 DAO 层。

---

## 5. FavoriteService 收藏服务

`FavoriteService` 是收藏子系统的核心服务类，负责收藏条目的增删查操作，并通过响应式 `StateFlow` 向外暴露收藏列表。服务通过 `FavoriteDao` 与 Room 数据库交互，`FavoriteDao.getAllFlow()` 返回 `Flow<List<FavoriteEntity>>`，服务将其映射为 `Flow<List<InputFavorite>>` 后转换为 `StateFlow`，确保订阅者始终能读取到最新的收藏列表快照。

```kotlin
class FavoriteService(
    private val favoriteDao: FavoriteDao,
    private val scope: CoroutineScope,
) {
    /** 所有收藏项（响应式），按 usageCount 降序 + createdAt 降序排列 */
    val favorites: StateFlow<List<InputFavorite>> = favoriteDao.getAllFlow()
        .map { entities -> entities.map { it.toDomain() } }
        .stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * 保存文本到收藏。
     *
     * 幂等操作：若文本已存在于收藏中，仅递增 usageCount 并更新 usedAt 时间戳；
     * 若文本不存在，创建新的收藏条目。
     */
    suspend fun save(text: String, type: InputTextType? = null) {
        val existing = favoriteDao.getByText(text)
        if (existing != null) {
            favoriteDao.upsert(
                existing.copy(usageCount = existing.usageCount + 1),
            )
        } else {
            favoriteDao.upsert(
                FavoriteEntity(
                    text = text,
                    type = type?.name,
                    usageCount = 1,
                    createdAt = System.currentTimeMillis(),
                ),
            )
        }
    }

    /** 删除指定文本的收藏 */
    suspend fun delete(text: String) {
        favoriteDao.delete(text)
    }

    /** 清空所有收藏 */
    suspend fun clearAll() {
        favoriteDao.clearAll()
    }

    /** 粘贴收藏项，返回收藏文本 */
    fun paste(favorite: InputFavorite): String = favorite.text
}
```

### 5.1 幂等保存机制

`save()` 方法实现了幂等的保存逻辑：通过 `favoriteDao.getByText(text)` 查询是否已存在相同文本的收藏条目。若存在，仅递增 `usageCount`（通过 `existing.copy(usageCount = existing.usageCount + 1)`）并更新记录；若不存在，创建新的 `FavoriteEntity` 并插入数据库。幂等性设计确保同一文本不会在收藏列表中出现重复条目，同时正确累计使用频次，为排序策略提供准确的数据基础。

### 5.2 响应式列表与 SharingStarted 策略

`favorites` 使用 `stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())` 将 `Flow` 转换为 `StateFlow`。`WhileSubscribed(5000)` 策略的含义是：当最后一个订阅者消失后，等待 5 秒再停止上游 `Flow` 的收集。这 5 秒的缓冲窗口避免了因 UI 层短暂取消订阅（如配置变更导致 Activity 重建）而触发数据库查询的重新启动，减少不必要的 I/O 开销。初始值为 `emptyList()`，确保在数据库查询完成前 UI 层有合法的空列表可用。

---

## 6. ImeState 剪贴板与收藏状态

### 6.1 Clipboard 状态

```kotlin
data class Clipboard(
    val clipperText: String? = null,
    val inputTextType: InputTextType? = null,
    val disabled: Boolean = false,
)
```

`clipperText` 为当前系统剪贴板的文本内容，`null` 表示无内容。`inputTextType` 为自动检测的文本类型。`disabled` 由 `UiConfig.clipPastePopupTipsEnabled` 与 `EngineConfig.favoriteClipEnabled` 共同确定——当两者均为 `false` 时 `ClipboardService` 不工作。

### 6.2 FavoriteList 状态

```kotlin
data class FavoriteList(
    val favorites: List<InputFavorite> = emptyList(),
    val disabled: Boolean = false,
    val isLoading: Boolean = false,
)
```

`favorites` 为当前用户的收藏条目列表，按使用频次和时间排序。`disabled` 由 `EngineConfig.favoriteInputEnabled` 和 `EngineConfig.favoriteClipEnabled` 联合控制。`isLoading` 标识收藏列表是否正在从数据库加载。

### 6.3 收藏功能门控

剪贴板和收藏的 `ImeEffect` 发射受 `EngineConfig` 布尔门控约束。当 `EngineConfig.favoriteClipEnabled` 为 `false` 时，引擎不监听系统剪贴板变更，不发射剪贴板相关的 `PopupTip.Action`，`ImeState.clipboard` 始终保持禁用默认值。当 `EngineConfig.favoriteInputEnabled` 和 `EngineConfig.favoriteClipEnabled` 均为 `false` 时，调用 `ImeIntent.SaveFavorite` 立即抛出 `IllegalStateException`，不会产生任何副作用信号。门控逻辑在 `ImeEngine` 的 reduce 函数中执行，确保禁用功能的副作用信号不会泄漏到 UI 层。

---

## 7. 与 ImeEffect 的协作

剪贴板检测和收藏操作与引擎的副作用通道 `ImeEffect` 紧密协作，通过 `ImeEffect.PopupTip` 向用户发送一次性提示信号。`PopupTip` 分为两种子类型：`Message` 是纯信息性提示，短暂停留后自动消失；`Action` 是可交互的操作提示，附带一个可点击按钮，点击后触发一个 `ImeIntent`。

### 7.1 剪贴板检测触发 Action 提示

当 `ClipboardService` 检测到系统剪贴板发生变更时，引擎在 reduce 过程中产生 `ImeEffect.PopupTip.Action` 副作用信号：

```kotlin
ImeEffect.PopupTip.Action(
    message = "可粘贴内容",
    actionLabel = "粘贴",
    action = ImeIntent.PasteClip(text),
    persistent = true,
)
```

### 7.2 输入提交触发收藏 Action 提示

当 `ImeIntent.CommitInput` 处理完成后，引擎检查已提交的文本是否已在收藏列表中。若文本未被收藏，引擎产生 `ImeEffect.PopupTip.Action` 副作用信号：

```kotlin
ImeEffect.PopupTip.Action(
    message = "可收藏内容",
    actionLabel = "收藏",
    action = ImeIntent.SaveFavorite(InputFavorite(text = committedText)),
    persistent = false,
)
```

### 7.3 完整协作流程

```plantuml
@file:../diagrams/engine-clipboard-favorites.puml
```

系统剪贴板变更时，`ClipboardService` 更新内部 `StateFlow`，`ImeEngine` 在 reduce 过程中读取服务状态并更新 `ImeState.clipboard`，同时发射 `ImeEffect.PopupTip.Action` 提示。用户点击提示中的「粘贴」按钮后，`ImeIntent.PasteClip` 被 `ImeEngine` 处理，文本通过 `ImeEditorBridge` 提交到编辑器。收藏流程类似：输入提交后若文本未收藏，发射收藏提示；用户点击「收藏」后，`FavoriteService` 执行保存，引擎发射确认提示。
