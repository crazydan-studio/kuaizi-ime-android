# 全局状态模型

## 1. ImeState 概述

`ImeState` 是整个 IME 状态的**唯一事实来源**（Single Source of Truth），所有 UI 组件通过 `StateFlow<ImeState>` 订阅状态驱动重组。它是一个不可变的 `data class`，所有变更通过 `copy()` 生成新实例，`ImeEngine` 内部持有 `MutableStateFlow<ImeState>`，对外暴露只读 `StateFlow<ImeState>`。状态变更的唯一路径为 `ImeIntent → reduce(state, intent) → ImeState`，确保状态转换的可追踪性和可预测性。

```kotlin
data class ImeState(
    val keyboard: Keyboard = Keyboard(),
    val inputList: InputList = InputList(),
    val candidateList: CandidateList = CandidateList(),
    val clipboard: Clipboard = Clipboard(),
    val favoriteList: FavoriteList = FavoriteList(),
    val toolListState: ToolListState = ToolListState(),
    val config: ImeConfig = ImeConfig(),
)
```

`ImeState` 的七个字段覆盖了输入法的全部逻辑状态：`keyboard` 描述当前键盘的类型、输入模式和状态机位置；`inputList` 管理用户输入的字符序列与游标；`candidateList` 承载候选词的分页和过滤数据；`clipboard` 和 `favoriteList` 分别维护剪贴板检测与收藏管理的状态；`toolListState` 管理工具栏按钮的配置与启用状态；`config` 提供运行时配置的快照。一次性副作用信号（弹出提示等领域事件）通过独立的 `SharedFlow<ImeEffect>` 通道发射，与 `ImeState` 完全分离。这种扁平组合的设计使得每个子状态都有清晰的职责边界，任何子状态的变更仅影响对应字段的 `copy()` 操作，不会意外波及其他子状态。

需要特别指出的是，`ImeState` 中**不包含**以下两类数据：`isInputting`（由 `KeyboardViewModel` 从 `inputList.pending` 直接派生）、帧级手势反馈状态（如触摸轨迹、按键高亮，由 `:ui` 的 `KeyboardViewModel` 维护本地 `StateFlow`）。这种分离确保了高频帧级状态不会污染引擎的核心状态树，避免了不必要的 Compose 重组开销。

---

## 2. Keyboard 模型

`Keyboard` 将键盘的类型、左右手模式临时状态和交互状态封装为一个不可变的 `data class`，通过组合模式替代继承。三个字段各自承担独立的职责维度：`type` 决定按键集合的语义内容（拼音字母、数字、符号等），`handMode` 记录左右手模式的临时切换状态（`null` 表示未切换，此时使用 `UiConfig.keyboardHandMode` 的值），`state` 记录当前键盘状态机的精确位置。键盘输入模式只能通过 `UiConfig.keyboardInputMode` 配置变更，不支持临时性修改。

```kotlin
data class Keyboard(
    val type: KeyboardType = KeyboardType.Pinyin,
    val handMode: KeyboardHandMode? = null,
    val state: KeyboardState = KeyboardState.Idle,
)
```

### 2.1 KeyboardType 枚举

`KeyboardType` 定义了键盘的内容类型，每种类型对应一个 `KeyboardIntentHandler` 子类来处理意图转换。键盘类型分为主键盘、临时键盘和超临时键盘三个层级：主键盘（`Pinyin`、`Latin`、`Number`）常驻存在，其他键盘退出后回到主键盘；临时键盘（`Math`、`Symbol`、`Emoji`、`Editor`）从主键盘切换进入，退出时回到切换前的主键盘；超临时键盘（`Candidate`、`CommitOption`）选择操作完成后立即返回，生命周期极短。

```kotlin
enum class KeyboardType {
    Pinyin,       // 拼音键盘（主键盘）
    Latin,        // 拉丁字母键盘（主键盘）
    Number,       // 数字键盘（主键盘）
    Math,         // 数学表达式键盘（临时键盘）
    Symbol,       // 符号选择键盘（临时键盘）
    Emoji,        // 表情选择键盘（临时键盘）
    Editor,       // 编辑功能键盘（临时键盘）
    Candidate,    // 候选词选择键盘（超临时键盘）
    CommitOption, // 提交选项键盘（超临时键盘）
}
```

每种 `KeyboardType` 切换时自动映射到对应的初始 `KeyboardState`：`Pinyin` 和 `Latin` 映射到 `PinyinInput.Waiting(null)`，`Number`、`Math` 和 `Editor` 映射到 `Idle`，`Symbol` 映射到 `SymbolChoosing(null)`，`Emoji` 映射到 `EmojiChoosing(null)`，`Candidate` 映射到 `CandidateSelection.Choosing(emptyList(), 0, 0)`，`CommitOption` 映射到 `CommitOptionChoosing(emptyList(), false, false)`。键盘类型切换时清空 `KeyboardStateHistory` 历史栈，因为不同键盘类型之间不存在回退关系。

### 2.2 KeyboardInputMode 枚举

`KeyboardInputMode` 定义了按键的几何排列和交互范式，通过 `ImeConfig.UiConfig.keyboardInputMode` 配置，不再是 `Keyboard` 的字段。键盘输入模式只能通过配置变更，不支持运行时临时修改。当前支持两种输入模式：

```kotlin
enum class KeyboardInputMode {
    HexGrid,  // 六边形网格（静态六边形按键排列）
    RectGrid, // 矩形网格（传统 QWERTY 排列）
}
```

`HexGrid` 模式下，按键以六边形网格排列，相邻按键共享边，适合滑行输入和翻动输入。六边形网格的几何特性使得手指在按键之间滑行时距离更短、方向选择更多，为拼音输入的滑行交互提供了天然的几何优势。引擎在 `HexGrid` 模式下启用滑行识别（Slip）和翻动识别（Flip）手势，`PinyinKeyboardIntentHandler` 在此模式下处理 `BeginSlip` 和 `BeginFlip` 状态转换。

`RectGrid` 模式下，按键以传统矩形网格排列（QWERTY 布局），适合逐键点击输入。矩形网格的几何排列更符合用户对传统键盘的肌肉记忆，在需要精确逐键输入的场景（如拉丁字母输入、数字输入）下更加直观。引擎在 `RectGrid` 模式下以逐键点击为默认交互方式，但仍然支持滑行和翻动手势——只是手势识别策略和按键间距参数与 `HexGrid` 不同。

键盘输入模式的切换通过 `UiConfig.keyboardInputMode` 配置变更实现，不支持运行时临时修改。UI 层根据 `config.ui.keyboardInputMode` 选择对应的布局渲染器（`HexGridLayout` 或 `RectGridLayout`），引擎层根据 `config.ui.keyboardInputMode` 选择对应的手势识别策略。

---

## 3. CandidateList 模型

`CandidateList` 管理拼音输入的候选词数据，包括候选词列表、分页控制和拼音过滤状态。它是一个不可变的 `data class`，所有变更通过 `copy()` 生成新实例。候选列表的生命周期与 `KeyboardState.CandidateSelection` 状态绑定——进入候选选择时加载候选词，退出候选选择时清空列表。

```kotlin
data class CandidateList(
    val candidates: List<InputWord> = emptyList(),
    val pageIndex: Int = 0,
    val pageSize: Int = 20,
    val hasMore: Boolean = false,
    val totalApprox: Int = 0,
    val filter: PinyinWordFilter? = null,
)
```

候选列表采用轮播分页模式：翻到末页后继续翻页回到首页，翻到首页前继续翻页跳到末页。应用过滤条件后 `pageIndex` 重置为 0。若 `pageIndex * pageSize >= candidates.size`，自动调整到最后一页。`hasMore` 标识字典中是否还有更多候选词可供加载，UI 层据此决定是否显示「加载更多」指示器。

### 3.1 InputWord 层级体系

`InputWord` 是候选词的类型层级，以 `sealed class` 表达四种候选词类型。所有子类共享 `text` 和 `frequency` 字段，各自携带类型特有的语义信息。`InputWord.Pinyin` 是最复杂的子类型，包含拼音拼写、繁体变体、部首和声调等中文输入特有的属性；`PinyinPhrase` 是多字词组，记录各字的拼音拼写序列；`Emoji` 包含名称和分组信息；`Latin` 最为简单，仅包含文本和频次。

```kotlin
sealed class InputWord {
    abstract val text: String
    abstract val frequency: Int

    data class Pinyin(
        override val text: String,
        override val frequency: Int,
        val spell: Spell?,
        val variant: Variant?,
        val radical: Radical?,
        val tone: Tone?,
    ) : InputWord()

    data class PinyinPhrase(
        override val text: String,
        override val frequency: Int,
        val spells: List<String>,
    ) : InputWord()

    data class Emoji(
        override val text: String,
        override val frequency: Int,
        val name: String,
        val group: String,
    ) : InputWord()

    data class Latin(
        override val text: String,
        override val frequency: Int,
    ) : InputWord()
}
```

### 3.2 InputWord.Pinyin 辅助类型

`InputWord.Pinyin` 携带的辅助类型为拼音过滤和提交选项提供了结构化的数据基础。`Spell` 记录拼音拼写的标识和值，用于拼写过滤和双拼/注音切换。`Variant` 记录繁体/异体变体文本和类型，用于繁简切换。`Radical` 记录部首文本和笔画数，用于高级过滤中的部首筛选。`Tone` 枚举了五个声调值，用于声调过滤。`SpellUsedMode` 控制拼音字的显示形式（全拼/双拼/注音），`VariantType` 区分繁体和异体两种变体类型。

```kotlin
data class Spell(val id: String, val value: String)
data class Variant(val text: String, val type: VariantType)
data class Radical(val text: String, val strokeCount: Int)

enum class Tone { Tone1, Tone2, Tone3, Tone4, Neutral }
enum class SpellUsedMode { FullPinyin, DoublePinyin, Bopomofo }
enum class VariantType { Traditional, Variant }
```

### 3.3 PinyinWordFilter 拼音过滤器

`PinyinWordFilter` 是候选列表的过滤条件，支持按声调和拼写两个维度筛选拼音候选词。过滤条件为空时 `isEmpty` 返回 `true`，此时候选列表不应用任何过滤。`matched()` 方法判断一个 `InputWord` 是否满足过滤条件——非 `InputWord.Pinyin` 类型直接返回 `false`，`InputWord.Pinyin` 类型需同时满足声调和拼写两个维度（若对应维度条件非空）。

```kotlin
data class PinyinWordFilter(
    val tones: Set<InputWord.Pinyin.Tone> = emptySet(),
    val spells: Set<InputWord.Pinyin.Spell> = emptySet(),
) {
    val isEmpty: Boolean get() = tones.isEmpty() && spells.isEmpty()

    fun matched(word: InputWord): Boolean {
        if (word !is InputWord.Pinyin) return false
        if (tones.isNotEmpty() && word.tone !in tones) return false
        if (spells.isNotEmpty() && word.spell !in spells) return false
        return true
    }
}
```

过滤逻辑在 `CandidateSelection.Filtering` 状态下生效：当过滤条件非空时，仅保留匹配的拼音字（不含 `Emoji`），合并首页高频字和后续页面字，高频字不再单独占用首页。过滤后若无匹配结果，候选列表为空。从 `Filtering` 状态退出时，过滤条件被清除，候选列表恢复为未过滤的完整列表。

### 3.4 CommitOption 提交选项

`CommitOption` 是针对已确认输入的后续操作选项，在 `CommitOptionChoosing` 状态下展示给用户。提交选项用于修改已确认输入的显示形式，如切换拼音的拼写使用模式（全拼/双拼/注音）和切换繁简变体。`CommitOption` 与 `InputWord` 正交——`CommitOption` 操作修改的是 `InputList` 的 `InputOption`，而非候选词本身。

```kotlin
sealed class CommitOption {
    data class ToggleSpellMode(val mode: InputWord.Pinyin.SpellUsedMode) : CommitOption()
    data class ToggleVariant(val variant: InputWord.Pinyin.Variant) : CommitOption()
    data object DeleteInput : CommitOption()
    data object CommitInput : CommitOption()
}
```

---

## 4. Clipboard 模型

`Clipboard` 是剪贴板状态的不可变 `data class`，管理当前剪贴板文本、提示显示状态、历史剪贴条目列表和功能开关。剪贴板状态通过 `ClipboardService` 与系统剪贴板监听集成——系统剪贴板变更时 `ClipboardService` 更新内部 `StateFlow`，`ImeEngine` 的 reduce 函数在处理 `ClipboardIntent.UpdateClip` 时读取服务状态并更新 `ImeState.clipboard`。

```kotlin
data class Clipboard(
    val currentText: String? = null,
    val showTip: Boolean = false,
    val clips: List<InputClip> = emptyList(),
    val disabled: Boolean = false,
)
```

`currentText` 为当前系统剪贴板的文本内容，`null` 表示无内容。`showTip` 控制是否在 UI 上显示剪贴板提示，当 `ClipboardService` 检测到新剪贴内容时设为 `true`，用户粘贴或 dismiss 后设为 `false`。`clips` 为最近的剪贴条目列表，支持用户浏览和选择历史剪贴内容。`disabled` 为功能开关，由 `UiConfig.clipPastePopupTipsEnabled` 与 `EngineConfig.favoriteClipEnabled` 共同确定，见第 8 节门控规则。

### 4.1 InputClip 数据模型

`InputClip` 是剪贴板条目的数据模型，包含文本内容、自动检测的文本类型和唯一标识码。`code` 字段用于条目去重——相同内容的剪贴条目不重复添加到 `clips` 列表中。`type` 字段通过 `InputTextType.detect()` 自动检测，为 UI 层提供类型信息以选择合适的图标和操作按钮。

```kotlin
data class InputClip(
    val text: String,
    val type: InputTextType? = null,
    val code: String = "",
)
```

### 4.2 InputTextType 文本类型检测

`InputTextType` 枚举定义了剪贴板文本的语义类型，`detect()` 方法通过正则表达式匹配自动检测文本类型。检测按严格的优先级顺序依次执行：`Captcha` → `CreditCard` → `IdCard` → `Phone` → `Email` → `Url` → `Address` → `Html` → `Text`。优先级设计遵循「特异性优先」原则——验证码（4-6 位数字+关键字）比银行卡号（16-19 位纯数字）更特异，银行卡号比身份证号（17 位数字+校验位）更特异，以此类推。

```kotlin
enum class InputTextType {
    Text,       // 普通文本
    Html,       // HTML 富文本
    Url,        // 链接文本
    Captcha,    // 验证码
    Phone,      // 手机/电话号码
    Email,      // 邮箱
    IdCard,     // 身份证号
    CreditCard, // 银行卡号
    Address;    // 地址

    companion object {
        fun detect(text: String): InputTextType?
    }
}
```

检测到的类型影响 UI 层的展示行为：`Captcha` 类型在剪贴板提示中显示验证码图标，点击后自动提取验证码数字粘贴；`Url` 类型显示链接图标，点击后粘贴完整链接；`Phone` 类型显示电话图标，点击后粘贴电话号码。对于无法匹配任何特定类型的文本，`detect()` 返回 `null`，UI 层按普通文本处理。

---

## 5. FavoriteList 模型

`FavoriteList` 是收藏列表状态的不可变 `data class`，管理用户收藏的文本条目、加载状态和功能开关。收藏列表通过 `FavoriteService` 与 Room 数据库集成——数据库变更自动触发 `Flow<List<InputFavorite>>` 发射，`ImeEngine` 订阅后更新 `ImeState.favoriteList.favorites`。这种响应式集成确保了收藏列表的实时性：用户在设置页面添加或删除收藏后，IME 键盘上的收藏列表自动同步更新。

```kotlin
data class FavoriteList(
    val favorites: List<InputFavorite> = emptyList(),
    val isLoading: Boolean = false,
    val disabled: Boolean = false,
)
```

`favorites` 为当前用户的收藏条目列表，按使用频次和时间排序——常用条目排在前面，便于快速访问。`isLoading` 标识收藏列表是否正在从数据库加载，初始启动或数据库查询期间为 `true`，加载完成后设为 `false`。UI 层根据 `isLoading` 显示加载指示器或收藏列表内容。`disabled` 为功能开关，见第 8 节收藏功能门控规则。

### 5.1 InputFavorite 数据模型

`InputFavorite` 是收藏条目的数据模型，包含文本内容、文本类型、使用次数、创建时间戳和最后使用时间戳。`type` 字段复用 `InputTextType` 的类型体系，与 `InputClip` 保持一致的类型语义。`usageCount` 记录用户粘贴该收藏条的次数，用于排序——高频使用的收藏条目排在列表前面。`createdAt` 和 `usedAt` 为时间戳，支持按时间和使用频率的复合排序策略。

```kotlin
data class InputFavorite(
    val text: String,
    val type: InputTextType?,
    val usageCount: Int = 0,
    val createdAt: Long = 0L,
    val usedAt: Long = 0L,
)
```

收藏条目的生命周期由 `FavoriteService` 管理：`save()` 方法保存新收藏或递增已有收藏的使用次数，`delete()` 方法删除指定文本的收藏条目，`clearAll()` 清空所有收藏。保存操作的幂等性设计确保同一文本不会重复添加——若文本已存在于收藏中，仅递增 `usageCount` 并更新 `usedAt` 时间戳。

---

## 6. ToolListState 模型

`ToolListState` 是工具栏状态的不可变 `data class`，管理键盘上方工具栏中各按钮的可用性和配置。工具栏内容根据 `keyboard.type`、`keyboard.state` 和收藏功能门控动态配置。将 `ToolListState` 纳入 `ImeState` 而非由 ViewModel 本地维护，使得引擎可以统一管理和持久化工具栏配置，UI 层无需额外订阅本地状态。

```kotlin
data class ToolListState(
    val tools: List<ToolItem> = emptyList(),
)

data class ToolItem(
    val label: String,
    val icon: String? = null,
    val intent: ImeIntent,
    val disabled: Boolean = false,
)
```

`ToolItem` 的 `intent` 字段存储点击该工具后发送的 `ImeIntent`，`disabled` 字段根据运行时状态动态控制——例如撤销/重做工具根据 `InputListEditor` 的 undoStack/redoStack 状态启用或禁用。工具栏的内容按 `keyboard.type` 动态配置：`Pinyin`/`Latin` 键盘显示全选、复制、粘贴、剪贴板、撤销、重做；`Editor` 键盘显示全选、复制、剪切、粘贴、撤销；`Symbol`/`Emoji`/`Number`/`Math` 键盘显示全选、复制、粘贴。剪贴板工具项始终显示（剪贴板粘贴功能不再受门控限制），收藏工具项仅在 `favoriteInputEnabled` 或 `favoriteClipEnabled` 至少一个为 `true` 时显示。

---

## 7. 副作用通道 (ImeEffect)

`ImeEffect` 是引擎的副作用信号，通过独立的 `SharedFlow<ImeEffect>` 通道发射，与 `ImeState` 完全分离。`ImeEngine` 内部持有 `MutableSharedFlow<ImeEffect>(extraBufferCapacity = 64)`，对外暴露只读 `SharedFlow<ImeEffect>`。与持续性状态不同，`ImeEffect` 表达的是「发生了某件事」的事件语义——引擎发射信号后 UI 层在独立协程中消费，不触发 `ImeState` 的变化。

> **为什么 ImeEffect 与 ImeState 分离？**  
> 避免了一次性效果触发 ImeState.copy() 和 StateFlow 发射导致的全局 UI 重组。每个 effect 通过独立的 SharedFlow 通道传递，UI 层在独立的协程中消费，不触发 ImeState 的变化。64 的缓冲容量确保快速连击时高频 `PopupTip` 不会因背压而静默丢弃。

### 7.1 PopupTip 体系

`PopupTip` 被设计为两种语义明确的子类型：`Message` 和 `Action`。`Message` 是纯信息性提示，短暂停留后自动消失，不提供交互操作。`Action` 是可交互的操作提示，附带一个可点击的按钮，点击后触发一个 `ImeIntent`，引擎接收该 Intent 后执行对应操作。

```kotlin
sealed class ImeEffect {
    sealed class PopupTip : ImeEffect() {
        /** 消息提醒：短暂停留的提示信息，如输入字符、键盘切换、编辑器操作提示 */
        data class Message(
            val message: String,
            val timeoutMs: Long = 3000L,
        ) : PopupTip()

        /** 操作提示：可点击触发动作，如可粘贴内容、可收藏内容 */
        data class Action(
            val message: String,
            val actionLabel: String,
            val action: ImeIntent,
            val persistent: Boolean = false,
            val timeoutMs: Long = 5000L,
        ) : PopupTip()
    }
}
```

`ImeEffect` 现仅承载 `PopupTip` 领域事件。音效和触觉反馈已完全交由 UI 层在 `gestureToIntent()` 中直接处理，不再经由 `ImeEffect` 通道。`AudioType` / `HapticType` 枚举和播放器接口均定义在 `:ui` 模块中。详见 [065-交互反馈设计](065-audio-haptic-feedback.md)。

### 7.2 Message 提示

`Message` 提示是最简单的 PopupTip 类型，用于展示短暂的纯信息性消息。默认超时 3000ms（3 秒），超时后自动消失。典型场景包括：键盘类型切换提示（如「已切换到拉丁键盘」）、输入字符反馈（如输入特殊符号时的字符名称提示）、编辑器操作反馈（如「已全选」）。`Message` 提示不携带任何交互操作，用户无法点击它触发动作，仅作为视觉反馈存在。UI 层收到 `Message` 提示后显示 Toast 风格的浮动文字条，超时后自动 dismiss。

### 7.3 Action 提示

`Action` 提示是可交互的 PopupTip 类型，携带一个可点击按钮，点击后触发一个 `ImeIntent`。`persistent` 字段控制提示的停留策略：

- **`persistent = false`**（默认）：提示在超时后自动消失，默认超时 5000ms（5 秒）。适合时效性较强的操作提示，如剪贴板检测到新内容后的「可粘贴内容」提示。
- **`persistent = true`**：提示持续显示直到用户开始输入，不会因超时自动消失。适合需要用户注意但不紧急的操作提示，如「剪贴板内容可用」提示——用户可能在阅读输入内容后再决定是否粘贴，此时提示应持续可见。

典型 Action 提示场景：

| 场景 | `message` | `actionLabel` | `action` | `persistent` | 说明 |
|------|-----------|---------------|----------|-------------|------|
| 剪贴板检测到新内容 | "可粘贴内容" | "粘贴" | `ImeIntent.PasteClip(text)` | `false` | 5 秒后自动消失 |
| 检测到可收藏内容 | "可收藏内容" | "收藏" | `ImeIntent.SaveFavorite(favorite)` | `false` | 5 秒后自动消失 |
| 剪贴板持续可用 | "剪贴板内容可用" | "粘贴" | `ImeIntent.PasteClip(text)` | `true` | 持续显示直到用户输入 |

### 7.4 通道集成

`ImeEngine` 在 `handleIntent()` 处理过程中，将 `ImeEffect` 通过 `_effect.emit()` 发射到 `SharedFlow<ImeEffect>`。UI 层在独立的协程中收集并消费：`PopupTip.Message` 显示浮动提示条并启动自动 dismiss 定时器；`PopupTip.Action` 显示带按钮的提示条，按钮点击触发对应的 `ImeIntent`。收藏确认通过 `PopupTip.Action` 实现——输入提交后若内容未收藏，引擎发射 `PopupTip.Action(message="可收藏内容", actionLabel="收藏", action=ImeIntent.SaveFavorite(...))` 提示，用户点击「收藏」按钮即可保存。

`PopupTip.Action` 的 dismiss 策略在 UI 层实现：`persistent = false` 时启动 `delay(timeoutMs)` 协程，超时后自动 dismiss；`persistent = true` 时不启动超时定时器，改为监听 `keyboard.state` 变更——当用户开始输入（状态从 `Idle` 转换到 `PinyinInput.Waiting`）时自动 dismiss。

---

## 8. 收藏功能门控规则

收藏功能门控由 `ImeConfig.EngineConfig` 中的 `favoriteInputEnabled` 和 `favoriteClipEnabled` 两个布尔字段联合控制。门控规则遵循 **Fail Fast** 原则——禁用功能后调用相关操作立即抛出异常。

### 8.1 收藏功能总门控

当 `EngineConfig.favoriteInputEnabled` 和 `EngineConfig.favoriteClipEnabled` 均为 `false` 时：

- `ImeState.favoriteList.disabled = true`
- `favoriteList.favorites` 始终为空列表
- `favoriteList.isLoading` 始终为 `false`
- 调用 `ImeIntent.SaveFavorite(favorite)` 立即抛出 `IllegalStateException`
- UI 层不显示收藏面板切换按钮

门控的写入路径由 `ImeEngine` 的 reduce 函数保证：当两个开关均为 false 时，任何试图更新 `favoriteList` 字段的 reduce 逻辑都会被短路，状态保持为禁用默认值。读取路径由 UI 层保证：`KeyboardViewModel` 根据 `favoriteList.disabled` 隐藏收藏相关的 UI 元素和工具项。

### 8.2 输入收藏门控

当 `EngineConfig.favoriteInputEnabled` 为 `false` 时：

- 输入提交后不向 SharedFlow 发射 `ImeEffect.PopupTip.Action("可收藏内容", ...)` 提示
- 不影响剪贴板收藏功能（若 `favoriteClipEnabled` 为 `true`）
- 不影响已收藏内容的使用（若 `favoriteClipEnabled` 为 `true`，收藏面板仍然可用）

### 8.3 剪贴板门控

`Clipboard.disabled` 由 `UiConfig.clipPastePopupTipsEnabled` 与 `EngineConfig.favoriteClipEnabled` 共同确定：

| `clipPastePopupTipsEnabled` | `favoriteClipEnabled` | `Clipboard.disabled` | 说明 |
|---------------------------|----------------------|---------------------|------|
| `true` | `true` | `false` | 剪贴板完全启用：支持粘贴、可粘贴内容提示、剪贴板收藏提示 |
| `true` | `false` | `false` | 剪贴板部分启用：支持粘贴和可粘贴内容提示，但不产生剪贴板收藏提示 |
| `false` | `true` | `false` | 剪贴板部分启用：支持粘贴和剪贴板收藏提示，但不产生可粘贴内容提示 |
| `false` | `false` | `true` | 剪贴板完全禁用：`ClipboardService` 不工作 |

即 `Clipboard.disabled = !clipPastePopupTipsEnabled && !favoriteClipEnabled`。

当 `Clipboard.disabled` 为 `true` 时：

- `ImeState.clipboard.disabled = true`
- `ClipboardService` 停止监听系统剪贴板
- 不向 SharedFlow 发射任何剪贴板相关的 `ImeEffect` 信号

当 `EngineConfig.favoriteClipEnabled` 为 `false` 但 `UiConfig.clipPastePopupTipsEnabled` 为 `true` 时：

- `ClipboardService` 正常运行，`ImeEngine.start()` 时检查剪贴板可粘贴内容并弹出粘贴确认提示
- 不向 SharedFlow 发射剪贴板收藏相关的 `ImeEffect.PopupTip.Action` 信号
- 不影响输入收藏功能（若 `favoriteInputEnabled` 为 `true`）

当 `EngineConfig.favoriteClipEnabled` 为 `true` 但 `UiConfig.clipPastePopupTipsEnabled` 为 `false` 时：

- `ClipboardService` 正常运行，但 `ImeEngine.start()` 时不检查剪贴板可粘贴内容
- 剪贴板收藏提示正常设置
- 用户仍可通过工具栏的剪贴板按钮手动粘贴

### 8.4 收藏同步门控

当 `EngineConfig.favoriteSyncToUserDictEnabled` 为 `true` 时：

- 被收藏的输入内容或可粘贴内容，自动保存到系统用户字典中
- 收藏面板中显示「同步」按钮（将用户字典数据同步到收藏）和删除时的「同步删除」按钮
- 此门控仅在 `favoriteInputEnabled` 或 `favoriteClipEnabled` 至少一个为 `true` 时生效

### 8.5 剪贴板粘贴功能说明

剪贴板粘贴功能（`ImeIntent.PasteClip`）始终可用，不受任何门控限制。即使在 `Clipboard.disabled = true` 的情况下，用户仍可通过工具栏的剪贴板按钮手动粘贴——只是不会有自动的可粘贴内容提示。

---

## 9. 状态不变式

以下不变式在任何时刻对 `ImeState` 都必须成立，`ImeEngine` 的 reduce 函数在每次状态转换后必须维护这些约束。所有状态不变式在 DEBUG 日志等级下通过 `assertStateInvariants()` 检查，使用 `assert()` 断言。Release 构建中跳过的断言以 0 额外开销运行。

1. **`keyboard.type` 与 `keyboard.state` 一致性**：`keyboard.state` 必须与 `keyboard.type` 的初始状态兼容。例如 `keyboard.type == Symbol` 时 `keyboard.state` 应为 `SymbolChoosing`，`keyboard.type == Pinyin` 时 `keyboard.state` 应为 `PinyinInput.*` 或 `CandidateSelection.*`，`keyboard.type == Candidate` 时 `keyboard.state` 应为 `CandidateSelection.*`。此不变式由 `KeyboardStateMachine.transition()` 保证——键盘类型切换时自动设置对应的初始状态。

2. **`candidateList` 非空前提**：`candidateList.candidates` 非空当且仅当 `keyboard.state` 处于 `CandidateSelection.*` 状态。进入候选选择时加载候选词，退出候选选择时清空列表。此不变式确保 UI 层不会在非候选状态下渲染空白的候选面板。

3. **`favoriteList` 禁用一致性**：当 `EngineConfig.favoriteInputEnabled` 和 `EngineConfig.favoriteClipEnabled` 均为 `false` 时，`favoriteList.disabled` 必须为 `true`，`favoriteList.favorites` 必须为空列表，`favoriteList.isLoading` 必须为 `false`。此不变式由 reduce 函数的门控短路逻辑保证。

4. **`clipboard` 禁用一致性**：当 `UiConfig.clipPastePopupTipsEnabled` 和 `EngineConfig.favoriteClipEnabled` 均为 `false` 时，`clipboard.disabled` 必须为 `true`。此不变式由 reduce 函数的门控逻辑保证。

5. **`inputList.gapIndex` 范围合法性**：`gapIndex` 必须满足 `0 <= gapIndex <= inputs.lastIndex + 1`，且 `inputs[gapIndex]` 必须是 `InputItem.Gap`（当 `gapIndex <= inputs.lastIndex` 时）。此不变式由 `InputList` 的 `init` 块和所有游标移动方法的边界检查保证。

6. **`candidateList.pageIndex` 范围合法性**：当 `candidateList.candidates` 非空时，`pageIndex` 必须满足 `0 <= pageIndex < ceil(candidates.size / pageSize)`。若数据变更导致 `pageIndex * pageSize >= candidates.size`，自动调整到最后一页。此不变式由 `CandidateList` 的分页逻辑保证。

7. **`keyboard` 切换清空历史**：`KeyboardType` 切换时 `KeyboardStateHistory` 必须被清空。不同键盘类型之间不存在状态回退关系，残留的历史栈会导致回退到语义不兼容的状态。此不变式由 `KeyboardStateMachine.resetTo()` 保证。

---

## 10. 状态频率分层

IME 系统中的状态变更频率差异极大——从每帧 60fps 的手势反馈到分钟级的配置变更。将不同频率的状态分配到不同的所有者和通道，是避免性能问题的关键设计决策。高频状态（帧级、按键级）如果混入 `ImeState`，会导致每次高频更新都触发 `StateFlow` 发射和 Compose 重组，产生不必要的性能开销。

| 状态 | 变更频率 | 所有者 | 通道 |
|------|---------|--------|------|
| `ImeState` | 按键级（ms 级） | `:engine` | `StateFlow<ImeState>` |
| `ImeEffect` | 按键级（ms 级） | `:engine` | `SharedFlow<ImeEffect>` |
| `GestureFeedbackState` | 帧级（16ms 级） | `:ui` | ViewModel 本地 `StateFlow` |
| `ToolListState` | 键盘切换级（s 级） | `:engine` | `StateFlow<ImeState>.toolListState` |
| `InputActionPlayerState` | 播放控制级（s 级） | `:ui` | ViewModel 本地 `StateFlow` |
| `KeyLayoutState` | 布局变更级（s 级） | `:ui` | ViewModel 本地 `StateFlow` |

`ImeState` 由 `:engine` 拥有，通过 `StateFlow` 向外暴露，是引擎与 UI 之间的核心契约。`GestureFeedbackState`（触摸轨迹、按键高亮、手指指示器）属于帧级高频状态，由 `:ui` 的 `KeyboardViewModel` 独立管理，不经过 `ImeState`——这确保了每帧的手势反馈更新不会触发引擎状态的变更和 Compose 重组的级联传播。`ToolListState` 纳入 `ImeState`，由引擎统一管理工具栏配置和门控状态。`InputActionPlayerState` 和 `KeyLayoutState` 均为秒级低频状态，由 `:ui` 的 ViewModel 本地管理。

这种频率分层架构的核心原则是：**引擎只拥有逻辑状态，UI 拥有展示状态和交互反馈状态**。逻辑状态的变更频率与用户的按键操作同步（ms 级），展示状态和交互反馈的变更频率与 UI 渲染帧率同步（16ms 级）或更低（s 级）。通过将两者分配到不同的所有者和通道，避免了高频 UI 更新对引擎状态树的污染，也避免了引擎状态变更对低频 UI 组件的不必要触发。
