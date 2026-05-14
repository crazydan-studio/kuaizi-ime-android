# 设计缺失项分析与补充

本文档记录 Java 工作树与 v4 设计文档的全面核对结果，列出设计文档中缺失或不完整的功能项，并给出补充设计。各缺失项按所属子系统分组，优先级标记为高/中/低。

---

## 1. 引擎核心

### 1.1 收藏板完整 CRUD [高]

Java `Favoriteboard` 提供完整的收藏管理：查询、粘贴、保存、删除、清空。当前 `ImeIntent` 仅有 `SaveFavorite`，缺少其余操作。

**补充设计**：

```kotlin
sealed class ImeIntent {
    // 收藏操作（补充）
    data class QueryFavorites(val keyword: String? = null) : ImeIntent()
    data class PasteFavorite(val favorite: FavoriteItem) : ImeIntent()
    data class DeleteFavorite(val id: String) : ImeIntent()
    data class DeleteSelectedFavorites(val ids: Set<String>) : ImeIntent()
    data object ClearAllFavorites : ImeIntent()
    data class SelectFavorite(val id: String, val selected: Boolean) : ImeIntent()
    data object ToggleFavoritePanel : ImeIntent()
}
```

`FavoriteListState` 补充：

```kotlin
data class FavoriteListState(
    val favorites: List<FavoriteItem>,
    val selectedIds: Set<String> = emptySet(),
    val isPanelOpen: Boolean = false,
    val isLoading: Boolean = false,
)
```

### 1.2 剪贴板深度数据提取 [高]

Java `Favoriteboard.deeplyCollectClips()` 使用 7 组正则表达式（captcha、URL、phone、email、id_card、credit_card、address）从一条剪贴板文本中提取多条不同类型的 `InputClip`，并有优先级规则。当前 `InputTextType.detect()` 仅有单类型检测。

**补充设计**：

```kotlin
/**
 * 剪贴板深度数据提取器。
 *
 * 从一条剪贴板文本中提取多条不同类型的 InputClip，
 * 并按优先级规则决定最终结果（如 captcha 优先级最高）。
 */
object InputClipExtractor {

    data class ExtractRule(
        val type: InputTextType,
        val patterns: List<Regex>,
        /** 优先级，数值越高越优先。captcha 优先级最高，有其他匹配时不重复匹配 */
        val priority: Int,
    )

    val rules: List<ExtractRule> = listOf(
        ExtractRule(InputTextType.Captcha, listOf(captchaRegex), priority = 100),
        ExtractRule(InputTextType.Url, listOf(urlRegex), priority = 50),
        ExtractRule(InputTextType.Phone, listOf(phoneRegex), priority = 40),
        ExtractRule(InputTextType.Email, listOf(emailRegex), priority = 30),
        ExtractRule(InputTextType.IdCard, listOf(idCardRegex), priority = 20),
        ExtractRule(InputTextType.CreditCard, listOf(creditCardRegex), priority = 15),
        ExtractRule(InputTextType.Address, listOf(addressRegex), priority = 10),
    )

    /**
     * 从剪贴板文本中提取多条 InputClip。
     * captcha 类型优先级最高：若匹配 captcha，则不再匹配其他类型。
     */
    fun extract(text: String): List<InputClip>
}
```

`InputClip` 补充 `code` 字段用于去重：

```kotlin
data class InputClip(
    val text: String,
    val type: InputTextType,
    /** 去重标识，由 text + type 的哈希生成，用于判断剪贴板数据是否已使用 */
    val code: String = md5("$type:$text"),
    /** HTML 富文本内容（部分剪贴板数据包含格式化文本） */
    val html: String? = null,
)
```

### 1.3 数学表达式计算引擎 [高]

Java `MathExprInput` 实现了完整的中缀→后缀（RPN）转换、运算符优先级与结合性、括号嵌套、百分号/千分号/万分号一元运算符、结果格式化。当前设计仅有 `InputItem.MathExpr` 的数据定义，无计算逻辑。

**补充设计**：

```kotlin
/**
 * 数学表达式计算引擎。
 *
 * 负责中缀表达式求值：中缀→后缀（RPN）转换、
 * 运算符优先级与结合性处理、括号嵌套（含配对引用）、
 * 百分号/千分号/万分号一元运算符。
 */
object MathExprCalculator {

    data class Operator(
        val symbol: String,
        val precedence: Int,
        val associativity: Associativity,
        val operandCount: Int,
        val evaluate: (List<Double>) -> Double,
    )

    enum class Associativity { Left, Right }

    /** 运算符表 */
    val operators: Map<String, Operator>

    /** 中缀表达式 → 后缀表达式（RPN） */
    fun toRPN(expression: String): List<String>

    /** 计算后缀表达式结果 */
    fun evaluate(rpn: List<String>): Double

    /** 格式化结果：4 位小数精度，去除尾零 */
    fun formatResult(value: Double): String
}
```

`InputItem.MathExpr` 补充：

```kotlin
data class MathExpr(
    val expression: String,
    /** 计算结果，实时更新 */
    val result: String? = MathExprCalculator.formatResult(
        MathExprCalculator.evaluate(MathExprCalculator.toRPN(expression))
    ),
    override val nestedList: InputListState,
) : InputItem()
```

### 1.4 输入补全应用逻辑 [高]

Java `InputList.applyCompletion()` 区分 Latin（单输入替换）和 Phrase_Word（多输入字确认+新增），处理 Gap 位偏移、范围外多余补全的新增插入。当前 `InputCompletion` 仅有数据定义，无应用逻辑。

**补充设计**：

```kotlin
sealed class InputCompletion {
    /** 应用范围，指定补全替换输入列表中的哪个区间 */
    abstract val applyRange: IntRange

    /** 拉丁文补全：替换 applyRange 内的全部输入字符 */
    data class LatinWord(
        override val applyRange: IntRange,
        val latin: String,
        val word: String?,
    ) : InputCompletion()

    /** 拼音短语补全：确认范围内的输入字，新增范围外的补全字 */
    data class PhraseWord(
        override val applyRange: IntRange,
        val words: List<ConfirmedWord>,
    ) : InputCompletion() {
        data class ConfirmedWord(
            val value: String,
            val spell: String?,
        )
    }
}

/**
 * 将补全应用到输入列表。
 *
 * - LatinWord：替换 applyRange 内的全部输入字符为补全的拉丁文
 * - PhraseWord：确认 applyRange 内的输入字不被预测替换，
 *   新增 applyRange 外的补全字到输入列表
 *
 * 应用后光标移至补全内容尾部。
 */
fun InputListState.applyCompletion(
    completion: InputCompletion,
    position: Int,
): InputListState
```

### 1.5 InputKey.Ctrl 子类型补充 [中]

Java `CtrlKey.Type` 有 22 个值，当前设计仅覆盖 9 种。补充缺失的控制按键类型：

```kotlin
sealed class InputKey {
    data class Ctrl(
        val type: CtrlType,
        val label: String? = null,
        val icon: String? = null,
    ) : InputKey()

    enum class CtrlType {
        // 已有
        Space, Backspace, Enter, Commit,
        SwitchKeyboard, SwitchIme, XPadToggle,
        Editor, PinyinToggle,

        // 补充：输入控制
        DropInput,           // 丢弃当前待输入
        ConfirmInput,        // 确认当前输入
        RevokeInput,         // 撤回已提交输入
        Exit,                // 退出当前键盘/状态

        // 补充：提交选项
        CommitInputListOption,  // 打开提交选项键盘

        // 补充：候选字过滤
        FilterBySpell,          // 按读音过滤候选字
        FilterByRadical,        // 按部首过滤候选字
        ConfirmFilter,          // 确认过滤条件
        FilterAdvance,          // 高级过滤

        // 补充：编辑器
        EditorCursorLocator,    // 光标定位模式
        EditorRangeSelector,    // 选区模式

        // 补充：分组切换
        ToggleEmojiGroup,       // 切换 Emoji 分组
        ToggleSymbolGroup,      // 切换符号分组

        // 补充：X-Pad
        XPadActiveBlock,        // X-Pad 激活区块音效
        XPadCharKey,            // X-Pad 字符键音效
        XPadSimulationDone,     // X-Pad 模拟终止
    }
}
```

### 1.6 拼音输入转换（PinyinToggleType）[中]

```kotlin
/**
 * 拼音输入转换类型。
 *
 * 在已输入的拼音按键序列中，将特定声母或韵母进行互换。
 */
enum class PinyinToggleType {
    /** 平翘舌转换：z↔zh, c↔ch, s↔sh */
    ZcsStart,
    /** n/l 转换：n↔l */
    NlStart,
    /** 前后鼻韵转换：an↔ang, en↔eng, in↔ing */
    NgEnd,
}

// ImeIntent 补充
data class TogglePinyinInput(val toggle: PinyinToggleType) : ImeIntent()
```

### 1.7 候选字过滤机制详细设计 [中]

Java `PinyinWord.Filter` 包含 spells（读音过滤，单选）和 radicals（部首过滤，多选），高级过滤根据 spell→radical 映射计算部首权重排序。

```kotlin
data class PinyinCandidateFilter(
    /** 读音过滤（单选：同一时间仅激活一个读音） */
    val spells: Set<Spell> = emptySet(),
    /** 部首过滤（多选：可同时选择多个部首） */
    val radicals: Set<Radical> = emptySet(),
) {
    data class Spell(val value: String, val id: Int)
    data class Radical(val value: String, val strokeCount: Int)

    /**
     * 根据当前过滤条件对候选字排序。
     *
     * 高级过滤模式下，根据 spell→radical 映射计算部首权重，
     * 匹配度高的候选字排在前面。
     */
    fun sortCandidates(candidates: List<InputWord.Pinyin>): List<InputWord.Pinyin>
}
```

### 1.8 InputWord 确认机制 [中]

Java `CharInput.ConfirmableWord` 标记字已确认，确认后的字不会被词组预测替换。

```kotlin
data class Char(
    val value: String,
    val spell: String?,
    val isCommitted: Boolean = false,
    /** 字是否已确认（不会被词组预测替换） */
    val isWordConfirmed: Boolean = false,
) : InputItem()
```

### 1.9 InputList 冻结机制 [中]

Java `InputList.freeze()` 在 Editor 和 CommitOption 键盘激活时冻结输入列表，避免打断当前键盘操作。

```kotlin
data class InputListState(
    // 已有字段...
    /** 输入列表是否被冻结。冻结状态下忽略输入相关消息 */
    val frozen: Boolean = false,
)
```

### 1.10 InputList 清空撤销 [中]

Java `Inputboard.Stage.Type.cleaned` 支持清空后撤销。

```kotlin
sealed class ImeIntent {
    // 补充
    data object CancelCleanInput : ImeIntent()
}

data class ImeState(
    // 补充
    /** 输入列表清空后是否可撤销 */
    val canCancelClean: Boolean = false,
)
```

### 1.11 InputList Gap 空格逻辑 [中]

Java `InputList.needGapSpace()` 根据左右输入类型决定是否在 Gap 位插入空格。

```kotlin
/**
 * Gap 空格规则。
 *
 * 根据左右输入类型决定在拼接文本时是否插入空格：
 * - Latin 与 Latin 之间：插入空格
 * - Latin 与 Pinyin spell 之间：插入空格
 * - MathExpr 与 MathOp 之间：不插入空格（表达式内部）
 * - Symbol 与其他之间：不插入空格
 * - 其余情况：插入空格
 */
object GapSpaceResolver {
    fun needsSpace(left: InputItem?, right: InputItem?): Boolean
}
```

### 1.12 拼音短语提取与分析 [中]

```kotlin
/**
 * 拼音短语分析器。
 *
 * 提供输入列表中的拼音短语提取和段落分析，
 * 用于候选预测和补全。
 */
object PinyinPhraseAnalyzer {
    /** 获取输入列表中全部拼音短语字 */
    fun extractPhraseWords(list: InputListState): List<InputItem.Char>
    /** 获取从指定输入位向前连续的拼音字 */
    fun extractPhraseWordsFrom(list: InputListState, position: Int): List<InputItem.Char>
    /** 获取指定输入所在的拼音段落 */
    fun findPhraseSegment(list: InputListState, position: Int): IntRange
}
```

### 1.13 CharKey.Level 滑行输入按键级别 [中]

```kotlin
data class Char(
    val value: String,
    /** 按键在滑行输入中的层级（仅拼音滑行输入时有值） */
    val level: KeyLevel? = null,
    // ... 其他字段
) : InputKey()

enum class KeyLevel {
    Level0, Level1, Level2, Final
}
```

### 1.14 键盘生命周期 API [中]

Java `IMEditor` 有 `start()/close()/exit()/destroy()` 生命周期方法。当前 `ImeEngine` 无显式生命周期 API。

```kotlin
class ImeEngine {
    /**
     * 键盘生命周期管理。
     *
     * start: 显示键盘，可指定初始键盘类型和是否重置输入状态
     * close: 隐藏键盘（不重置状态）
     * exit: 退出键盘（重置状态）
     * destroy: 回收资源
     */
    fun start(keyboardType: KeyboardType? = null, resetInputting: Boolean = false)
    fun close()
    fun exit()
    fun destroy()
}
```

### 1.15 编辑器手势追踪 [中]

Java `EditorEditStateData` 有 `from/to` 坐标和 `getMotion()` 方向计算，用于通过滑动移动光标/选择内容。

```kotlin
data class CursorMoving(
    val position: Int,
    /** 手势起始坐标 */
    val startPosition: Offset? = null,
    /** 当前偏移量 */
    val currentOffset: Offset? = null,
) : EditorEditing()

data class TextSelecting(
    val start: Int,
    val end: Int,
    /** 手势起始坐标 */
    val startPosition: Offset? = null,
    /** 当前偏移量 */
    val currentOffset: Offset? = null,
) : EditorEditing()

/**
 * 编辑器手势追踪器。
 *
 * 将手势坐标变化转换为光标移动方向。
 */
object EditorGestureTracker {
    fun resolveMotion(start: Offset, current: Offset): Motion
    enum class Motion { Left, Right, Up, Down }
}
```

### 1.16 自动化/测试 API [中]

Java `IMEditor` 有 `changeLastInputWord()` 和 `prepareInputs()` 方法用于输入练习。

```kotlin
class ImeEngine {
    /**
     * 自动化 API（仅 Feature.InputPractice 启用时可用）。
     */
    fun prepareInputs(tuples: List<Array<String>>)
    fun changeLastInputWord(word: InputWord)
}
```

### 1.17 DoubleTap / LongPressTick 手势 [低]

```kotlin
sealed class InputGesture {
    // 补充
    data object DoubleTap : InputGesture()
    data class LongPressTick(val key: InputKey, val duration: Long) : InputGesture()
}
```

### 1.18 符号键盘的"仅配对符号"模式 [低]

```kotlin
data class SymbolChoosing(
    val groupId: String,
    /** 是否仅显示配对符号（用于提交选项键盘） */
    val onlyPairSymbols: Boolean = false,
) : KeyboardState()
```

---

## 2. 输入模型

### 2.1 InputWord.Pinyin 完整字段 [高]

Java `PinyinWord` 含 spell（id + charsId + value）、radical（value + strokeCount）、glyphId、traditional。当前设计仅有 spell: String。

```kotlin
data class Pinyin(
    /** 拼音（含 DB 关联 ID） */
    val spell: Spell,
    /** 候选字变体（多音字） */
    val variant: String? = null,
    /** 声调 */
    val tone: Int? = null,
    /** 部首 */
    val radical: Radical? = null,
    /** 字形 ID（用于 Emoji 关键字匹配） */
    val glyphId: Int? = null,
    /** 是否繁体字 */
    val traditional: Boolean = false,
) : InputWord() {
    data class Spell(
        val value: String,
        /** 拼音在 meta_pinyin_chars 表中的 ID */
        val id: Int,
        /** 拼音字母组合在 meta_pinyin_chars 表中的 ID */
        val charsId: Int,
    )

    data class Radical(
        val value: String,
        val strokeCount: Int,
    )
}
```

### 2.2 InputItem.Space 类型 [中]

```kotlin
data class Space(
    val value: String = " ",
) : InputItem()
```

### 2.3 InputFavorite 完整数据模型 [中]

```kotlin
data class FavoriteItem(
    val id: String,
    val text: String,
    val type: InputTextType,
    /** 快捷输入标识 */
    val shortcut: String? = null,
    /** HTML 富文本内容 */
    val html: String? = null,
    val createdAt: Long,
    /** 最近使用时间 */
    val usedAt: Long? = null,
    val usageCount: Int = 0,
)
```

### 2.4 符号分组体系 [中]

```kotlin
enum class SymbolGroup {
    Han,      // 中文符号
    Latin,    // 英文符号
    Math,     // 数学符号
    Arrow,    // 箭头符号
    Index,    // 序号符号
    Geometry, // 几何符号
    Misc,     // 杂项符号
    Other,    // 其他符号
}

data class SymbolKey(
    val value: String,
    val group: SymbolGroup,
    /** 配对符号的另一半（如 "(" 配对 ")"） */
    val pairWith: String? = null,
) : InputKey()
```

---

## 3. 字典系统

### 3.1 HMM 短语预测的已确认字机制 [高]

```kotlin
interface DictRepository {
    /**
     * 预测拼音短语。
     *
     * @param context 上下文字列表
     * @param confirmedWords 已确认字的映射（位置→字ID），
     *   Viterbi 解码时对已确认位置强制使用指定字 ID
     */
    suspend fun predictPhrase(
        currentSpell: String,
        context: List<String>,
        confirmedWords: Map<Int, Int> = emptyMap(),
    ): List<InputWord.Pinyin>
}
```

### 3.2 用户数据双权重体系 [高]

```kotlin
@Entity(tableName = "phrase_word")
data class PhraseWordEntity(
    @PrimaryKey val wordId: Int,
    val spellCharsId: Int,
    /** 应用内置权重（不可被用户撤销覆盖） */
    val weightApp: Double,
    /** 用户学习权重（可撤销） */
    val weightUser: Double,
)

@Entity(tableName = "phrase_trans_prob")
data class PhraseTransProbEntity(
    @PrimaryKey val id: Int,
    val wordId: Int,
    val prevWordId: Int,
    val wordSpellCharsId: Int,
    val prevWordSpellCharsId: Int,
    val valueApp: Double,
    val valueUser: Double,
)

interface UserInputDao {
    fun incrementFrequency(wordId: Int)
    /** 撤销频率增量（reverse=true 时递减） */
    fun decrementFrequency(wordId: Int)
}
```

### 3.3 Emoji 关键字匹配与拼音短语推荐 [高]

```kotlin
@Entity(tableName = "meta_emoji")
data class EmojiEntity(
    @PrimaryKey val id: Int,
    val value: String,
    val weightUser: Double,
    val enabled: Boolean,
    /** 关键字 ID 列表（逗号分隔），用于关键字搜索 */
    val keywordIdsList: String,
    val groupId: Int,
)

interface DictRepository {
    /** 根据拼音短语的字形 ID 列表推荐相关 Emoji */
    suspend fun predictEmojisByPhrase(phraseGlyphIds: List<Int>, top: Int): List<InputWord.Emoji>
    /** 根据关键字搜索 Emoji */
    suspend fun searchEmojisByKeyword(keywordIds: List<Int>, top: Int): List<InputWord.Emoji>
}
```

### 3.4 数据库升级完整迁移逻辑 [高]

当前设计仅有 `MIGRATION_3_4` 占位符。需补充 v0→v2→v3→v4 的完整迁移链：

- **v0 初始化**：创建全部表结构 + 合并预置词典数据
- **v2→v3**：从旧版 `used_pinyin_phrase` 视图提取短语出现次数，计算 HMM 转移概率写入新表
- **v3→v4**：新增收藏表（`user_input_favorite`）
- **迁移库模式**：用应用字典库作迁移中间库，成功后原子替换用户库

### 3.5 PinyinCharsTree 三层分割算法 [高]

```kotlin
/**
 * 拼音字符树。
 *
 * children 键类型为 String（非 Char），支持 ch/sh/zh 整体节点。
 */
data class PinyinCharsTree(
    val children: Map<String, PinyinCharsTree> = emptyMap(),
    /** 对应 meta_pinyin_chars 表的 ID */
    val charsId: Int? = null,
) {
    companion object {
        /**
         * 三层分割拼音：声母 / 第一后继 / 剩余。
         * ch/sh/zh 作为整体声母节点。
         */
        fun splitChars(chars: String): Triple<String, String, String>
    }

    /** 获取后继字母组合列表 */
    fun getNextChars(): List<String>
    /** 获取子树所有拼音组合 */
    fun getAllPinyinChars(): List<String>
    /** 从树中查找拼音字母组合的 DB ID */
    fun getCharsId(chars: String): Int?
}
```

### 3.6 DB 表结构完整定义 [高]

Java 版完整表结构需在设计文档中逐表对照补充，特别是以下缺失的字段：

| 表 | 缺失字段 |
|---|---|
| `pinyin_word` | `spell_id_`, `spell_chars_id_`, `traditional_`, `radical_`, `radical_stroke_count_`, `variant_`, `glyph_weight_` |
| `phrase_word` | `spell_chars_id_`, `weight_app_`, `weight_user_`（替换单一 `weight_`） |
| `phrase_trans_prob` | `word_spell_chars_id_`, `prev_word_spell_chars_id_`, `value_app_`, `value_user_`（替换单一 `value_`） |
| `meta_emoji` | `keyword_ids_list_`, `enabled_`, `group_id_` |
| `meta_emoji_group` | 整表缺失 |

### 3.7 Emoji 分组与「常用」动态分组 [中]

```kotlin
interface EmojiDao {
    /**
     * 获取分组 Emoji 列表。
     *
     * 「常用」分组（GROUP_GENERAL）由使用权重降序的前 N 个组成，
     * 其余按 DB 中的 group_ 字段分组。
     */
    suspend fun getAllGroupedEmojis(groupGeneralCount: Int = 24): Map<String, List<EmojiEntity>>
}
```

### 3.8 Emoji 运行时系统兼容性检测 [中]

```kotlin
object EmojiCompatChecker {
    /**
     * 检测系统是否可渲染每个 Emoji，动态更新 enabled_ 标记。
     * 应在每次打开数据库时执行。
     */
    suspend fun checkAndMarkEmojis(db: SupportSQLiteDatabase, paint: Paint)
}
```

### 3.9 候选字声调排序 [中]

```kotlin
/**
 * 拼音声调排序算法。
 *
 * 排序规则：9 组元音 × 4 声调 + 轻声
 * 顺序：ā á ǎ à → ō ó ǒ ò → ē é ě è → ī í ǐ ì → ū ú ǔ ù → ǖ ǘ ǚ ǜ → ... → 轻声
 */
object ToneSorter {
    fun compareByTone(spell1: String, spell2: String): Int
}
```

### 3.10 拉丁文补全长度过滤 [低]

```kotlin
object LatinCompletionRules {
    /** 补全触发条件：输入长度 ≥ 2 */
    const val MIN_TRIGGER_LENGTH = 2
    /** 存储条件：仅记录长度 > 3 的拉丁文 */
    const val MIN_STORE_LENGTH = 4
}
```

---

## 4. UI 组件

### 4.1 InputWordKey 按键类型 [高]

Java `InputWordKeyViewHolder` 在键盘中渲染候选字词（word + spell + traditional mark），与 `CandidateKeyContent` 不同。

```kotlin
data class InputWord(
    val word: InputWord.Pinyin,
) : InputKey()

// KeyView 分支补充
@Composable
fun InputWordKeyContent(key: InputKey.InputWord) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        BasicText(text = key.word.spell.value)   // 拼音
        BasicText(text = key.word.value)          // 字
        if (key.word.traditional) { /* 繁体标记 */ }
    }
}
```

### 4.2 候选栏弹窗系统 [高]

Java `CandidatesView.Popup` 有三种弹窗类型和完整的状态机。

```kotlin
/**
 * 弹窗状态机。
 *
 * showing → shown → closing → closed
 * 支持进出场动画、延迟关闭、动画中断恢复。
 */
enum class PopupState { Showing, Shown, Closing, Closed }

/**
 * 三种弹窗类型：
 * - QuickList: 候选字快捷列表（点击候选栏更多按钮弹出）
 * - Tooltip: 按键输入时显示当前按键字符，800ms 自动隐藏
 * - Snackbar: 编辑操作结果提示 / 剪贴板可收藏确认，3.5s 自动关闭 + 操作按钮
 */
sealed class CandidatePopup {
    abstract val state: PopupState

    data class QuickList(override val state: PopupState) : CandidatePopup()
    data class Tooltip(override val state: PopupState, val char: String) : CandidatePopup()
    data class Snackbar(
        override val state: PopupState,
        val message: String,
        val actionLabel: String? = null,
        val onAction: (() -> Unit)? = null,
    ) : CandidatePopup()
}
```

### 4.3 Toolbar 完整状态机和按钮组 [高]

Java `InputboardView` 有 5 种状态和 12+ 个功能按钮，分 inputbar 组和 toolbar 组。

```kotlin
/**
 * 工具栏状态机。
 */
enum class ToolbarState {
    Init,
    InputFreezeDoing,   // 输入冻结中
    InputDoing,         // 输入中
    InputCleanedWaiting, // 输入已清空，等待撤销
    ToolbarShowDoing,   // 工具栏显示中
}

data class ToolbarStateData(
    val state: ToolbarState = ToolbarState.Init,
    /** 当前显示的按钮组：inputbar 或 toolbar */
    val activeButtonGroup: ButtonGroup = ButtonGroup.InputBar,
    /** 各按钮的可用状态 */
    val buttonAvailability: Map<ToolbarButton, Boolean> = emptyMap(),
)

enum class ButtonGroup { InputBar, ToolBar }

enum class ToolbarButton {
    // InputBar 组
    SwitchIme, ShowToolbar, CloseKeyboard,
    CleanInputList, CancelCleanInputList,
    // ToolBar 组
    HideToolbar, Settings,
    EditorCopy, EditorPaste, EditorCut,
    EditorSelectAll, EditorUndo, EditorRedo,
    Favoriteboard,
}
```

### 4.4 FavoriteboardView 完整交互 [高]

```kotlin
data class FavoriteListState(
    val favorites: List<FavoriteItem>,
    val selectedIds: Set<String> = emptySet(),
    val isPanelOpen: Boolean = false,
    val isLoading: Boolean = false,
    /** 是否显示空列表提示 */
    val showEmptyHint: Boolean = false,
    /** 是否处于多选模式 */
    val isMultiSelectMode: Boolean = false,
)

/**
 * 收藏面板交互补充：
 * - 多选 checkbox + 删除选中按钮（带确认对话框）
 * - 清空全部按钮（带确认对话框）
 * - 空列表提示视图
 * - 标题带收藏数量
 * - 增量列表更新（单项增删改，避免全量重载）
 */
```

### 4.5 六边形按键背景 [高]

Java `HexagonDrawable` 支持六边形形状、圆角、阴影、边框。当前设计简化为矩形。

```kotlin
/**
 * 六边形按键形状。
 *
 * 使用 Compose GenericShape 或自定义 Shape 实现，
 * 支持圆角、阴影（shadow_style）、边框（border_style/disabled_border_style）。
 * 用于 X-Pad 按键和部分控制键的六边形外观。
 */
class HexagonShape(
    private val cornerRadius: Dp = 4.dp,
) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline
}
```

### 4.6 面板管理系统 [中]

Java `IMEditorView` 有 BoardType 枚举和面板切换管理。

```kotlin
/**
 * 面板管理器。
 *
 * 管理键盘面板和收藏面板的激活/失活、
 * 面板显隐策略和进场动画。
 */
enum class BoardType { Main, Favorite }

class PanelManager {
    val activeBoard: StateFlow<BoardType>

    fun switchTo(board: BoardType)
    fun closeActiveBoard()

    /**
     * 面板显隐策略：
     * - 主面板隐藏时用 INVISIBLE（保留空间）
     * - 其他面板用 GONE + 延迟隐藏
     * - 进场视图播放 fade_in 动画，退场直接隐藏避免重影
     */
}
```

### 4.7 剪贴板可收藏 Snackbar 确认交互 [中]

```kotlin
/**
 * 剪贴板可收藏确认弹窗。
 *
 * 当检测到剪贴板内容可收藏时，显示 Snackbar：
 * - 提示文本："确认文本"
 * - 操作按钮："保存为收藏"
 * - 3.5s 后自动关闭
 */
data class ClipFavoriteSnackbar(
    val clipText: String,
    val onConfirm: () -> Unit,
)
```

### 4.8 AudioPlayer 音频反馈系统 [中]

```kotlin
/**
 * 音频反馈播放器。
 *
 * 音效类型与资源映射：
 * - tick_single: 单击按键音
 * - tick_double: 双击按键音
 * - page_flip: 翻页音
 * - tick_clock: 长按持续触发音
 * - tick_ping: X-Pad 音效
 *
 * 按键音受 audioFeedbackEnabled 控制，
 * 翻页音受 candidatesPagingAudioEnabled 控制。
 */
class AudioPlayer(private val context: Context) {
    fun play(type: SoundType)
    enum class SoundType { TickSingle, TickDouble, PageFlip, TickClock, TickPing }
}
```

### 4.9 DialogConfirm 内嵌确认对话框 [中]

```kotlin
/**
 * 键盘内嵌确认弹窗。
 *
 * 嵌入在 KeyboardPanel 内部的轻量级确认机制，
 * 模态（阻止事件传播），带进出动画。
 * 用于确认删除收藏、确认清空数据等操作。
 */
@Composable
fun ConfirmPopup(
    message: String,
    positiveLabel: String,
    negativeLabel: String,
    onPositive: () -> Unit,
    onNegative: () -> Unit,
)
```

### 4.10 XPad 三级分区架构 [中]

Java `XPadView` 有 3 级分区（zone_0 中心 / zone_1 控制键扇区 / zone_2 字符键扇区）。

```kotlin
/**
 * X-Pad 三级分区数据模型。
 *
 * - zone_0: 中心正六边形（控制区入口）
 * - zone_1: 6 个控制键扇区（左右轴标签）
 * - zone_2: 6 个字符键扇区（左右轴标签）
 */
data class XPadLayout(
    val zone0: HexZone,
    val zone1: List<HexZone>,
    val zone2: List<HexZone>,
    /** 六边形方向：FLAT_TOP（X-Pad 模式）或 POINTY_TOP（普通键盘模式） */
    val orientation: HexagonOrientation,
)

enum class HexagonOrientation { FlatTop, PointyTop }

/**
 * X-Pad 输入态视觉反馈：
 * - 手指从 zone_1 滑到 zone_2 时内六边形缩小（80ms 动画）
 * - 输入完成后还原
 * - 模拟模式（simulating=true）下由 InputActionPlayer 驱动
 */
```

### 4.11 输入列表只读嵌套 [中]

```kotlin
/**
 * MathExprInputItem 的嵌套只读渲染。
 *
 * 内部使用只读的 InputListPanel（无独立滚动、无手势处理），
 * 由外层 InputListPanel 控制滚动位置。
 */
@Composable
fun MathExprInputItem(item: InputItem.MathExpr) {
    Column {
        BasicText(text = item.expression)
        item.result?.let { BasicText(text = "= $it") }
        InputListPanel(
            state = item.nestedList,
            readOnly = true,  // 只读模式：无滚动、无手势
        )
    }
}
```

### 4.12 输入列表滚动定位 [中]

```kotlin
/**
 * InputListPanel 智能滚动定位。
 *
 * - 选中项宽度超过可见区域时滚动到其尾部
 * - 选中项已在可见区域右侧时不滚动
 * - 支持滚动偏移而非仅 scrollToPosition
 */
object InputListScrollHelper {
    fun calculateScrollOffset(
        listState: LazyListState,
        selectedIndex: Int,
        itemWidth: Int,
        viewportWidth: Int,
    ): Int
}
```

### 4.13 收藏项元数据展示 [中]

```kotlin
@Composable
fun FavoriteItemView(item: FavoriteItem) {
    Column {
        // InputTextType 标签
        TypeTag(type = item.type)
        // 文本内容（HTML 类型使用富文本渲染）
        if (item.html != null) {
            HtmlText(text = item.html)
        } else {
            BasicText(text = item.text)
        }
        // 元数据
        Row {
            Text(text = formatDate(item.createdAt))
            Text(text = "使用 ${item.usageCount} 次")
            item.usedAt?.let { Text(text = "最近 ${formatDate(it)}") }
        }
    }
}
```

### 4.14 键盘警告视图和底部间距适配 [中]

```kotlin
/**
 * 键盘加载提示。
 *
 * 在键盘启动过程中显示加载动画，启动完成后隐藏。
 */
@Composable
fun KeyboardWarningOverlay(isLoading: Boolean)

/**
 * 底部间距适配。
 *
 * 竖屏模式下适配系统导航手势高度，
 * 由 adaptDesktopSwipeUpGesture 配置驱动。
 */
fun Modifier.adaptBottomSpacing(config: ImeConfig): Modifier
```

### 4.15 GapInput 光标闪烁动画 [低]

```kotlin
@Composable
fun GapInputItem(isCursor: Boolean, onTap: () -> Unit) {
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse,
        ),
    )
    // 闪烁效果
}
```

---

## 5. 应用模块

### 5.1 IMEService InputType 自动适配 [高]

```kotlin
/**
 * 输入类型自动适配规则。
 *
 * 根据 EditorInfo.inputType 决定键盘类型和行为标志。
 */
object InputTypeAdapter {
    data class AdaptedConfig(
        val keyboardType: KeyboardType,
        val forceCleanInput: Boolean = false,
        val singleLine: Boolean = false,
        val passwordMode: Boolean = false,
        val disableKeyPopup: Boolean = false,
    )

    fun adapt(inputType: Int, variation: Int): AdaptedConfig

    // 映射规则：
    // TYPE_CLASS_NUMBER / DATETIME / PHONE → Number, forceCleanInput=true
    // TYPE_CLASS_TEXT + VARIATION_PASSWORD / VISIBLE_PASSWORD / WEB_PASSWORD → Latin, singleLine=true, passwordMode=true, disableKeyPopup=true
    // TYPE_CLASS_TEXT + VARIATION_EMAIL_ADDRESS / URI / FILTER / AUTO_COMPLETE → singleLine=true
}
```

### 5.2 运行时临时配置项 [高]

Java `ConfigKey` 中有 6+ 个临时性配置项不在 `ImeConfig` 中。这些本质是运行时状态而非用户配置：

```kotlin
data class ImeState(
    // 补充运行时临时状态
    /** 当前 IME 子类型（latin/hans），由系统 InputMethodSubtype 决定 */
    val imeSubtype: ImeSubtype = ImeSubtype.Hans,
    /** 键盘布局方向 */
    val orientation: KeyboardOrientation = KeyboardOrientation.Portrait,
    /** 是否单行输入模式（由编辑器 InputType 决定） */
    val singleLineInput: Boolean = false,
    /** 原键盘类型（用于切换后恢复） */
    val prevKeyboardType: KeyboardType? = null,
    /** 已使用的剪贴数据标识（防止重复提示） */
    val usedInputClipCode: String? = null,
)

enum class ImeSubtype { Latin, Hans }
enum class KeyboardOrientation { Portrait, Landscape }

/**
 * UI 控制标志（作为 KeyboardPanel 参数，而非 ImeConfig）。
 */
data class KeyboardPanelConfig(
    val disableSettingsBtn: Boolean = false,
    val disableSwitchImeBtn: Boolean = false,
    val disableCloseKeyboardBtn: Boolean = false,
)
```

### 5.3 InputConnectionBridge 实现细节 [高]

```kotlin
class InputConnectionBridge(
    private val inputConnectionSupplier: () -> InputConnection?
) : BaseImeOutputBridge() {

    // 补充关键实现逻辑：

    /** 字符替换提交：检查光标前文本是否在 replacements 列表中 */
    override fun doReplaceableCommitText(text: String, replacements: List<String>) {
        val ic = inputConnectionSupplier() ?: return
        val before = ic.getTextBeforeCursor(1, 0)?.toString()
        if (before in replacements) {
            ic.deleteSurroundingText(1, 0)
        }
        ic.commitText(text, 1)
    }

    /** 成对符号提交：先插入右符号再插入左符号，重新选中初始文本 */
    override fun doInsertPairedSymbols(left: String, right: String) {
        val ic = inputConnectionSupplier() ?: return
        ic.beginBatchEdit()
        ic.commitText(right, 1)
        ic.commitText(left, -1)
        ic.endBatchEdit()
    }

    /** 编辑动作映射 */
    override fun doPerformEdit(action: EditorAction) {
        val ic = inputConnectionSupplier() ?: return
        when (action) {
            EditorAction.Backspace -> ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL))
            EditorAction.SelectAll, EditorAction.Copy, EditorAction.Paste,
            EditorAction.Cut, EditorAction.Undo, EditorAction.Redo,
            -> ic.performContextMenuAction(actionToAndroidId(action))
            else -> {}
        }
    }

    /** 光标移动边界检查：通过 ExtractedText 检查是否已到编辑区域边界 */
    override fun doMoveCursor(direction: Int) {
        val ic = inputConnectionSupplier() ?: return
        val extracted = ic.getExtractedText(ExtractedTextRequest(), 0)
        // 边界检查后再发送方向键
    }
}
```

### 5.4 IMESubtype 子类型切换 [中]

```kotlin
/**
 * 系统 IME 子类型处理。
 *
 * 从 InputMethodSubtype 的 locale/languageTag 判断当前子类型：
 * - en_US → Latin 键盘
 * - 其他 → Pinyin 键盘（Hans）
 *
 * onCurrentInputMethodSubtypeChanged 时触发键盘切换。
 */
class IMEService : InputMethodService() {
    override fun onCurrentInputMethodSubtypeChanged(subtype: InputMethodSubtype) {
        val newSubtype = when {
            subtype.locale.startsWith("en") -> ImeSubtype.Latin
            else -> ImeSubtype.Hans
        }
        engine.handleIntent(ImeIntent.UpdateImeSubtype(newSubtype))
    }
}
```

### 5.5 更新日志数据格式 [中]

```kotlin
/**
 * 更新日志数据格式。
 *
 * JSON 结构：
 * {
 *   "versions": [
 *     {
 *       "version": "1.0.0",
 *       "date": "2024-01-01",
 *       "logs": [
 *         { "text": "新增功能", "details": ["细节1", "细节2"] }
 *       ]
 *     }
 *   ]
 * }
 *
 * 渲染规则：
 * - 版本号清洗（去除 -alpha 等后缀）
 * - 当前版本高亮标记
 * - 条目列表 + 子条目缩进
 */
```

### 5.6 交互式练习引导系统 [高]

Java 的练习系统是**交互式**引导（用户操作验证 + 提示），而非文档 930 的自动播放式演示。

```kotlin
/**
 * 交互式练习引导系统。
 *
 * 与文档 930 的 InputActionPlayer（自动播放式演示）不同，
 * 本系统支持步骤验证和用户交互引导。
 */

/** 练习数据模型 */
data class Exercise(
    val id: String,
    val title: String,
    val description: String,
    val mode: ExerciseMode,
    val steps: List<ExerciseStep>,
)

enum class ExerciseMode {
    Free,       // 自由练习（无验证）
    Normal,     // 互动验证（监听 ImeOutput/ImeState 验证操作）
    Introduce,  // 介绍模式（自动演示，无互动）
}

data class ExerciseStep(
    val instruction: String,
    /** 验证规则：监听 ImeOutput/ImeState 判断用户操作是否正确 */
    val validator: ((ImeOutput?) -> Boolean)? = null,
    /** 自动动作（如自动切换键盘） */
    val autoAction: ImeIntent? = null,
    /** 操作错误时的提示信息 */
    val hintOnWrong: String? = null,
)

/**
 * 按键图像渲染。
 *
 * 在练习步骤说明中嵌入真实按键图形（Compose 环境），
 * 使用 KeyView 渲染单个按键并截取为 ImageBitmap。
 */
@Composable
fun KeyImageRender(key: InputKey, isActive: Boolean = false): ImageBitmap
```

### 5.7 捐赠页面交互 [中]

```kotlin
/**
 * 捐赠页面交互补充：
 * - 点击提示文本自动复制备注名到剪贴板
 * - 复制成功后显示 Toast 反馈
 * - 长按二维码图片触发系统分享
 * - 二维码图片从 raw 资源加载
 */
```

### 5.8 用户反馈 URL 构建 [中]

```kotlin
/**
 * 用户反馈流程：
 * 1. 构建 GitHub Issue URL（labels + title + body）
 * 2. 收集设备信息（制造商、型号、Android 版本、屏幕尺寸、DPI）
 * 3. 收集应用信息（包名 + 版本号）
 * 4. 弹窗让用户选择是否携带系统信息
 * 5. 确认后打开浏览器
 */
object FeedbackUrlBuilder {
    fun buildUrl(includeDeviceInfo: Boolean): String
}
```

---

## 6. 音效/触觉反馈触发机制 [低]

```kotlin
/**
 * 引擎侧音效触发。
 *
 * 方案：音效/弹窗由 UI 层根据按键类型和配置自行决定，
 * 无需引擎参与。GestureInputPanel 在检测到手势时，
 * 根据当前按键类型和 ImeConfig.UiConfig 中的配置决定
 * 是否播放音效或显示按键提示。
 *
 * 这样引擎保持纯逻辑，音效/触觉等平台特定副作用由 UI 层处理。
 */
```
