# ImeState 全局状态设计

ImeState 是 v4 版本 MVI 架构中的**单一状态树根节点**，作为 `ImeEngine` 对外暴露的唯一状态源，所有 UI 组件通过 `StateFlow<ImeState>` 订阅状态驱动重组。本文档定义 ImeState 的完整字段清单、各子状态的详细结构、状态不变式与约束、Feature 门控规则、以及与 Java 版本模型的对照关系。

键盘状态机 `KeyboardState` 的状态转换规则详见 [020-键盘状态机](020-state-machine.md)，输入列表 `InputList` 的数据模型与操作详见 [030-输入列表](030-input-list.md)，剪贴板与收藏详见 [070-剪贴板与收藏](070-clipboard-and-favorites.md)。本文档聚焦于 ImeState 自身的结构定义、子状态间的协作关系，以及前述文档未覆盖的子状态细节。

---

## 1 ImeState 数据类定义

```kotlin
/**
 * IME 全局状态。
 *
 * 不可变 data class，所有变更通过 copy() 生成新实例。
 * ImeEngine 持有 MutableStateFlow<ImeState>，对外暴露只读 StateFlow。
 * 状态变更的唯一路径：ImeIntent → reduce(state, intent) → ImeState。
 */
data class ImeState(
    /** 当前键盘实例，绑定类型、输入模式和状态 */
    val keyboard: Keyboard = Keyboard(),
    /** 是否正在输入（有未确认的拼音/拉丁字符），控制 Row 2 面板切换 */
    val isInputting: Boolean = false,
    /** 输入列表 */
    val inputList: InputList = InputList(),
    /** 候选列表 */
    val candidateList: CandidateList = CandidateList(),
    /** 剪贴板状态 */
    val clipboard: Clipboard = Clipboard(),
    /** 收藏列表状态 */
    val favoriteList: FavoriteList = FavoriteList(),
    /** 弹出提示状态，null 表示无提示 */
    val popupTip: PopupTipState? = null,
    /** 工具栏状态（仅 isInputting=false 时显示） */
    val toolList: ToolListState = ToolListState(emptyList()),
    /** 当前运行时配置 */
    val config: ImeConfig = ImeConfig(),
)
```

### 1.1 字段与 UI 面板映射

| 字段 | 消费面板 | 驱动行为 |
|------|---------|---------|
| `keyboard.type` | `KeyLayoutPanel` | 选择按键集合（拼音/拉丁/数字/符号/表情/数学/编辑） |
| `keyboard.mode` | `KeyLayoutPanel`, `GestureInputPanel` | 选择布局几何（XPad/HexGrid/RectGrid/MultiZone）和手势识别策略 |
| `isInputting` | Row 2 面板切换 | `true` → `InputListPanel`；`false` → `ToolListPanel` |
| `keyboard.state` | `KeyboardViewModel`, `GestureInputPanel` | 决定手势识别逻辑（滑行/翻动/XPad/候选选择/编辑） |
| `inputList` | `InputListPanel` | 渲染输入字符序列和光标 |
| `candidateList` | `CandidateListPanel` | 渲染候选词列表和翻页控制 |
| `clipboard` | `PopupTipPanel`, `ToolListPanel` | 剪贴板提示和粘贴操作 |
| `favoriteList` | `ToolListPanel` | 收藏快捷操作 |
| `popupTip` | `PopupTipPanel` (Row 1) | 短暂提示叠加层 |
| `toolList` | `ToolListPanel` (Row 2) | 工具按钮（编辑功能键、剪贴板、收藏） |
| `config` | 主题系统、配置 UI | 驱动颜色、尺寸、功能开关 |

### 1.2 状态不变式

1. **keyboard.type 与 keyboard.state 一致性**：`keyboard.state` 必须与 `keyboard.type` 的初始状态兼容。例如 `keyboard.type == Symbol` 时 `keyboard.state` 应为 `SymbolChoosing`，`keyboard.type == Pinyin` 时 `keyboard.state` 应为 `PinyinInput.*` 或 `CandidateSelection.*`。
2. **isInputting 与 inputList 一致性**：当 `inputList` 中存在未确认的拼音字符（`pending != null` 且非空）时，`isInputting` 必须为 `true`；当 `inputList` 为空或所有输入均已确认时，`isInputting` 可为 `false`。
3. **candidateList 非空前提**：`candidateList.candidates` 非空当且仅当 `keyboard.state` 处于 `CandidateSelection.*` 状态。
4. **toolList 仅在非输入态有效**：`toolList` 的内容在 `isInputting == true` 时无意义，UI 层应忽略。
5. **popupTip 短暂性**：`popupTip` 不应在连续两个 ImeState 中保持相同 `timestamp`，UI 层应自动dismiss。

---

## 2 KeyboardType 与 KeyboardInputMode

### 2.1 KeyboardType 枚举（:ime-engine 模块）

> **注意**：`KeyboardType` 和 `KeyboardInputMode` 现在通过 `Keyboard` data class 组合，而非作为 `ImeState` 的直接字段。访问方式为 `state.keyboard.type` 和 `state.keyboard.mode`。

```kotlin
/**
 * 键盘内容类型，决定按键集合的语义内容和标签。
 *
 * 与 KeyboardInputMode 正交：任意 KeyboardType 可与任意 KeyboardInputMode 组合。
 * 定义在 :ime-engine 模块，作为引擎公开 API 的一部分。
 * 通过 Keyboard data class 组合到 ImeState 中。
 */
enum class KeyboardType {
    /** 拼音输入键盘（主键盘） */
    Pinyin,
    /** 拉丁字母键盘（主键盘） */
    Latin,
    /** 数字键盘 */
    Number,
    /** 数学表达式键盘 */
    Math,
    /** 符号选择键盘 */
    Symbol,
    /** 表情选择键盘 */
    Emoji,
    /** 编辑功能键盘（光标移动、文本选择、复制粘贴等） */
    Editor,
    /** 候选词选择键盘（临时键盘，选择后返回主键盘） */
    Candidate,
    /** 提交选项键盘（临时键盘，选择后返回主键盘） */
    CommitOption,
}
```

**主键盘与临时键盘**：`Pinyin` 和 `Latin` 为主键盘（`isMaster == true`），其余为临时键盘。临时键盘退出时返回上一个主键盘。`Candidate` 和 `CommitOption` 为超临时键盘，选择操作完成后立即返回。

**KeyboardType → 初始 KeyboardState 映射**：

| KeyboardType | 初始 KeyboardState |
|-------------|-------------------|
| Pinyin | `PinyinInput.Waiting(null)` |
| Latin | `PinyinInput.Waiting(null)` |
| Number | `Idle` |
| Math | `Idle` |
| Symbol | `SymbolChoosing(null)` |
| Emoji | `EmojiChoosing(null)` |
| Editor | `Idle` |
| Candidate | `CandidateSelection.Choosing(emptyList(), 0, 0)` |
| CommitOption | `CommitOptionChoosing(emptyList())` |

### 2.2 KeyboardInputMode 枚举（:ime-engine 模块）

```kotlin
/**
 * 输入交互范式，决定按键的几何排列和交互方式。
 *
 * 与 KeyboardType 正交：任意 KeyboardInputMode 可与任意 KeyboardType 组合。
 * 定义在 :ime-engine 模块，作为引擎公开 API 的一部分。
 */
enum class KeyboardInputMode {
    /** X-Pad 六边形环状滑行输入 */
    XPad,
    /** 六边形网格（静态六边形按键排列） */
    HexGrid,
    /** 矩形网格（传统 QWERTY 排列） */
    RectGrid,
    /** 多区域分离布局（Zone A/B 分离模式） */
    MultiZone,
}
```

---

## 3 KeyboardState 状态机

`KeyboardState` 是 ImeState 的核心子状态，以 sealed class 表达键盘交互的有限状态机。完整的状态转换规则、转换触发器、历史栈机制详见 [020-键盘状态机](020-state-machine.md)。本节补充前述文档未覆盖的状态数据细节。

### 3.1 KeyboardState 完整层级

```
KeyboardState (sealed)
├── Idle (data object)
├── PinyinInput (sealed)
│   ├── Waiting(pending: InputItem.Char?)     — 待输入，pending 为未确认的拼音字符
│   ├── Slipping(                              — 滑行输入中
│   │     startKey: InputKey,
│   │     level0Key: InputKey,
│   │     level1Key: InputKey?,
│   │     level2Key: InputKey?,
│   │     nextCharsByLength: Map<Int, List<String>>,
│   │   )
│   ├── Flipping(                              — 翻动输入中
│   │     startChar: String,
│   │     candidates: List<String>,
│   │   )
│   └── XPadding(                              — X-Pad 输入中
│         zones: List<XPadZone>,
│         currentSpell: String,
│       )
├── CandidateSelection (sealed)
│   ├── Choosing(                              — 候选词选择中
│   │     candidates: List<InputWord>,
│   │     pageIndex: Int,
│   │     pageSize: Int,
│   │   )
│   ├── Filtering(                             — 拼音过滤中
│   │     filter: PinyinWordFilter,
│   │     filtered: List<InputWord>,
│   │   )
│   └── AdvanceFiltering(                      — 高级过滤中（部首/声调）
│         radical: PinyinWord.Radical?,
│         tone: PinyinWord.Tone?,
│         filtered: List<InputWord>,
│       )
├── CommitOptionChoosing(                      — 提交选项选择中
│     options: List<CommitOption>,
│     hasSpell: Boolean,
│     hasVariant: Boolean,
│   )
├── EditorEditing (sealed)
│   ├── CursorMoving(position: Int)            — 光标移动中
│   └── TextSelecting(start: Int, end: Int)    — 文本范围选择中
├── SymbolChoosing(groupId: String?)           — 符号选择中
└── EmojiChoosing(groupId: String?)            — 表情选择中
```

### 3.2 Slipping 状态数据

滑行输入（Slip）是拼音键盘的核心交互方式，用户手指从起始按键滑出，经过中间级按键，最终到达目标按键完成输入。滑行输入采用三级按键层次结构：

- **level0Key**：滑行起始按键，手指按下时确定的按键
- **level1Key**：滑行第一级目标按键，手指从起始按键滑出后的第一个中间按键
- **level2Key**：滑行第二级目标按键，手指继续滑行到达的最终按键

`nextCharsByLength` 记录在当前滑行路径下，按字符长度分组的可选后续字符列表。例如，滑行到 `level1Key` 后，可能产生的后续字符按长度 1、2、3 等分组存储。此映射用于 UI 层预显示可能的后续输入选项。

### 3.3 Flipping 状态数据

翻动输入（Flip）是在首字母按键上快速滑出触发的输入方式，`startChar` 为起始字符，`candidates` 为翻动产生的候选字符列表。翻动输入是一种快捷输入方式，允许用户通过快速滑动选择同一起始字符下的不同变体。

### 3.4 CandidateSelection 状态数据

候选选择有三个子状态，形成选择 → 过滤 → 高级过滤的递进关系：

**Choosing**：基础候选选择状态，`candidates` 为当前候选词列表，`pageIndex` 和 `pageSize` 控制分页显示。分页采用轮播模式：翻到末页后继续翻页回到首页，翻到首页前继续翻页跳到末页。`hasMore` 标识是否还有更多候选词可供加载。

**Filtering**：在基础选择上应用拼音过滤器，`filter` 包含声调和拼写过滤条件，`filtered` 为过滤后的候选词列表。过滤时会合并首页高频词和后续页面词，高频词不再单独占用首页。

**AdvanceFiltering**：在基础选择上应用部首和声调的高级过滤，`radical` 为选中的部首，`tone` 为选中的声调，`filtered` 为过滤后的候选词列表。部首列表从候选词中动态提取，按权重排序（出现频次高的部首优先，笔画数少的优先）。

### 3.5 CommitOptionChoosing 状态数据

提交选项选择是输入列表中针对已确认输入的后续操作选择，`options` 为可用的提交选项列表，`hasSpell` 表示当前输入列表中是否有包含拼音拼写的候选字，`hasVariant` 表示是否有包含变体（繁体/异体）的候选字。这两个布尔值控制提交选项面板中「切换拼写模式」和「切换变体」按钮的显示与否。

`CommitOption` 定义如下：

```kotlin
/**
 * 输入提交选项。
 *
 * 提交选项用于修改已确认输入的显示形式，
 * 如切换拼音的拼写使用模式（全拼/双拼/注音）、切换繁简变体等。
 */
sealed class CommitOption {
    /** 切换拼写使用模式 */
    data class ToggleSpellMode(val mode: PinyinWord.SpellUsedMode) : CommitOption()
    /** 切换变体（繁体/简体） */
    data class ToggleVariant(val variant: PinyinWord.Variant) : CommitOption()
    /** 删除输入 */
    data object DeleteInput : CommitOption()
    /** 提交输入 */
    data object CommitInput : CommitOption()
}
```

### 3.6 EditorEditing 状态数据

编辑器编辑包含两种子状态：`CursorMoving` 控制光标在输入列表中的位置移动，`TextSelecting` 控制文本范围选择。两种编辑模式通过编辑手势触发切换：短按进入光标移动，长按后滑动进入范围选择。

`position`、`start`、`end` 均为 `InputList` 中的输入索引位置，而非字符偏移量，因为输入列表的粒度是输入项（CharInput/GapInput），而非单个字符。

### 3.7 SymbolChoosing 与 EmojiChoosing 状态数据

符号选择和表情选择均支持分组浏览。`groupId` 标识当前选中的分组，`null` 表示默认分组或未选择分组。切换分组时重置分页到首页。

符号选择的分组由 `SymbolGroup` 枚举定义（han/latin/math/pair），表情选择的分组由表情数据文件的分组键名定义。符号选择还支持 `onlyPair` 模式，仅显示配对符号（如括号、引号），该模式在输入列表中插入配对符号时使用。

---

## 4 InputList 输入列表

`InputList` 是 ImeState 中最复杂的子状态，管理用户输入的字符序列、光标位置、待确认输入、输入补全等。完整的数据模型和操作详见 [030-输入列表](030-input-list.md)。本节补充 ImeState 上下文中 InputList 的关键协作细节。

### 4.1 InputList 与 KeyboardState 的协作

InputList 和 KeyboardState 通过 ImeEngine 的 reduce 函数协调变更：

1. **PinyinInput.Waiting** → 用户按下拼音键 → 更新 `InputList.pending` + 设置 `isInputting = true`
2. **CandidateSelection.Choosing** → 用户选择候选词 → `InputList` 确认 pending 字符 + 设置候选词 → `isInputting` 可能变为 `false`
3. **CommitOptionChoosing** → 用户选择选项 → 更新 `InputList.inputOption` 或替换输入词 → 返回 `Waiting`
4. **EditorEditing** → 光标移动/范围选择 → 更新 `InputList.gapIndex`（光标位置）

### 4.2 InputList 冻结机制

InputList 支持 `frozen` 标志，当键盘进入编辑模式（EditorEditing）或提交选项模式（CommitOptionChoosing）时，InputList 被冻结，禁止修改输入内容。冻结确保在这些特殊模式下不会意外修改输入列表。

### 4.3 InputList 输入选项

```kotlin
/**
 * 输入选项，控制已确认输入的显示形式。
 */
data class InputOption(
    /** 拼音拼写使用模式 */
    val spellUsedMode: PinyinWord.SpellUsedMode = PinyinWord.SpellUsedMode.FullPinyin,
    /** 是否使用候选字变体（繁体/异体） */
    val variantUsed: Boolean = false,
)
```

`InputOption` 影响输入列表的文本输出：`spellUsedMode` 决定拼音字的显示方式（全拼/双拼/注音），`variantUsed` 决定是否使用繁体变体。该选项通过 `CommitOptionChoosing` 状态的提交选项进行修改。

### 4.4 输入补全

输入补全是基于当前输入内容自动提供的替换建议，分为两种类型：

| 类型 | 触发条件 | 补全方式 |
|------|---------|---------|
| `Latin` | 拉丁字符输入中 | 替换单个拉丁输入的全部按键字符 |
| `PhraseWord` | 拼音短语输入中（光标在 Gap 上） | 替换或补充指定范围内的拼音输入字 |

```kotlin
data class InputCompletion(
    val type: InputCompletion.Type,
    val applyRange: IntRange,
    val inputs: List<InputItem.Char>,
) {
    enum class Type { Latin, PhraseWord }
}
```

补全的应用范围 `applyRange` 为 InputList 中的索引区间 [start, end)。应用补全时，范围内的输入被替换为补全内容中的输入，范围外的多余补全按新增插入。

### 4.5 配对符号

字符输入支持配对符号引用：左括号引用右括号，左引号引用右引号。删除一侧配对符号时，另一侧同步删除。配对符号通过 `InputItem.Char.pairSymbol` 字段建立引用关系。

```kotlin
data class PairSymbol(
    /** 配对的另一侧符号的输入 ID */
    val pairedId: String,
    /** 是否为左侧符号（开符号） */
    val isOpen: Boolean,
)
```

### 4.6 数学表达式输入

数学表达式输入 `InputItem.MathExpr` 包含嵌套的 InputList，用户可以在数学模式下的嵌套输入列表中输入数字和运算符，引擎自动计算表达式结果。数学表达式使用逆波兰表示法（RPN）求值，支持四则运算、幂运算和括号。

### 4.7 输入列表撤销与恢复

输入列表支持通过 Stage 机制进行撤销和恢复操作：

| 操作 | Stage 类型 | 说明 |
|------|-----------|------|
| 提交输入 | `Committed` | 提交后保存 InputList 副本，可撤销恢复 |
| 清空输入 | `Cleaned` | 清空前保存 InputList 副本，可撤销恢复 |

Stage 机制在 `InputListEditor` 内部实现，与 KeyboardState 的历史栈（KeyboardStateHistory）独立运作。撤销提交（RevokeCommit）不使用状态历史栈，而是通过 Stage 恢复。

### 4.8 Gap 空格规则

InputList 中两个相邻可见输入之间可能需要插入空格，规则如下：

- 数学运算符左右都需要空格
- 数学表达式输入两侧需要空格
- 拉丁字符与中文之间需要空格
- 已有显式空格的不重复添加
- 符号与拉丁字符之间不需要空格
- 拼音拼写显示模式下的空格规则与拉丁字符一致

---

## 5 CandidateList 候选列表

### 5.1 数据模型

```kotlin
data class CandidateList(
    /** 候选词列表 */
    val candidates: List<InputWord> = emptyList(),
    /** 当前页索引（从 0 开始） */
    val pageIndex: Int = 0,
    /** 每页候选词数量 */
    val pageSize: Int = 20,
    /** 是否还有更多候选词可加载 */
    val hasMore: Boolean = false,
    /** 拼音过滤器（仅 CandidateSelection.Filtering 时有效） */
    val filter: PinyinWordFilter = PinyinWordFilter(),
)
```

### 5.2 分页规则

候选列表采用轮播分页模式：

- **下一页**：`pageIndex` 前移一页，若已到末页则回到首页
- **上一页**：`pageIndex` 回退一页，若已在首页则跳到末页
- **过滤后重置**：应用过滤条件后 `pageIndex` 重置为 0
- **数据变更后纠正**：若 `pageIndex * pageSize >= candidates.size`，自动调整到最后一页

### 5.3 PinyinWordFilter 拼音过滤器

```kotlin
/**
 * 拼音候选词过滤器。
 *
 * 用于在候选选择界面按声调和拼写进行筛选。
 */
data class PinyinWordFilter(
    /** 按声调过滤：声调值列表 */
    val tones: Set<PinyinWord.Tone> = emptySet(),
    /** 按拼写过滤：拼写对象列表 */
    val spells: Set<PinyinWord.Spell> = emptySet(),
) {
    val isEmpty: Boolean get() = tones.isEmpty() && spells.isEmpty()

    fun matched(word: InputWord): Boolean {
        if (word !is PinyinWord) return false
        if (tones.isNotEmpty() && word.tone !in tones) return false
        if (spells.isNotEmpty() && word.spell !in spells) return false
        return true
    }
}
```

过滤逻辑：当过滤条件非空时，仅过滤匹配的拼音字（不含 Emoji），合并首页高频字和后续页面字，高频字不再单独占用首页。过滤后若无匹配结果，候选列表为空。

### 5.4 InputWord 体系

```
InputWord (sealed)
├── PinyinWord(
│     text: String,         — 汉字文本
│     frequency: Int,       — 使用频次
│     spell: Spell?,        — 拼音拼写
│     variant: Variant?,    — 繁体/异体变体
│     radical: Radical?,    — 部首
│     tone: Tone?,          — 声调
│   )
├── PinyinPhrase(
│     text: String,         — 短语文本
│     frequency: Int,       — 使用频次
│     spells: List<String>, — 各字的拼音拼写
│   )
├── Emoji(
│     text: String,         — Emoji 字符
│     frequency: Int,       — 使用频次
│     name: String,         — Emoji 名称
│     group: String,        — 所属分组
│   )
└── Latin(
      text: String,         — 拉丁字符文本
      frequency: Int,       — 使用频次
    )
```

### 5.5 PinyinWord 辅助类型

```kotlin
data class Spell(
    val id: String,         — 拼写标识（带声调的拼音）
    val value: String,      — 拼写值
)

data class Variant(
    val text: String,       — 变体文本（繁体/异体字）
    val type: VariantType,  — 变体类型
)

data class Radical(
    val text: String,       — 部首文本
    val strokeCount: Int,   — 笔画数
)

enum class Tone { Tone1, Tone2, Tone3, Tone4, Neutral }

enum class SpellUsedMode { FullPinyin, DoublePinyin, Bopomofo }

enum class VariantType { Traditional, Variant }
```

---

## 6 Clipboard 剪贴板状态

```kotlin
data class Clipboard(
    /** 当前剪贴板文本，null 表示无内容 */
    val currentText: String? = null,
    /** 是否显示剪贴板提示 */
    val showTip: Boolean = false,
    /** 剪贴板条目列表（最近的剪贴内容） */
    val clips: List<InputClip> = emptyList(),
    /** 剪贴板功能是否已禁用 */
    val disabled: Boolean = false,
)
```

### 6.1 与 ClipboardService 的集成

`ClipboardService` 是 `:ime-engine` 的内部组件，通过 `ClipboardManager` 监听系统剪贴板变更。服务通过 `StateFlow<InputClip?>` 暴露当前剪贴内容。ImeEngine 的 reduce 函数在处理剪贴板相关 Intent 时，读取 `ClipboardService` 的状态并更新 `ImeState.clipboard`。

剪贴板状态变更传播路径：

1. 系统剪贴板变更 → `ClipboardService` 更新内部 `StateFlow`
2. `ImeEngine` 接收到 `ClipboardIntent.UpdateClip` → reduce 函数读取 `ClipboardService` 状态
3. reduce 函数生成新的 `ImeState`，更新 `clipboard` 字段

### 6.2 InputClip 数据模型

```kotlin
data class InputClip(
    /** 剪贴文本内容 */
    val text: String,
    /** 检测到的文本类型 */
    val type: InputTextType? = null,
    /** 唯一标识码（用于去重） */
    val code: String = "",
)
```

### 6.3 InputTextType 文本类型检测

```kotlin
enum class InputTextType {
    Text,       — 普通文本
    Html,       — HTML 富文本
    Url,        — 链接文本
    Captcha,    — 验证码
    Phone,      — 手机/电话号码
    Email,      — 邮箱
    IdCard,     — 身份证号
    CreditCard, — 银行卡号
    Address;    — 地址

    companion object {
        /** 通过正则匹配检测文本类型 */
        fun detect(text: String): InputTextType?
    }
}
```

文本类型检测使用正则表达式匹配，按以下优先级依次检测：Captcha → CreditCard → IdCard → Phone → Email → Url → Address → Html → Text。检测到的类型用于剪贴板提示的图标和操作按钮选择。

### 6.4 Feature 门控

剪贴板功能通过 `Feature.Clipboard` 标记门控。当该功能被禁用时：

- `ImeState.clipboard.disabled = true`
- `clipboard.clips` 始终为空列表
- `clipboard.showTip` 始终为 `false`
- 调用剪贴板相关 Intent（如 `PasteClip`）立即抛出 `IllegalStateException`

---

## 7 FavoriteList 收藏列表状态

```kotlin
data class FavoriteList(
    /** 收藏条目列表 */
    val favorites: List<InputFavorite> = emptyList(),
    /** 是否正在加载 */
    val isLoading: Boolean = false,
    /** 收藏功能是否已禁用 */
    val disabled: Boolean = false,
)
```

### 7.1 与 FavoriteService 的集成

`FavoriteService` 是 `:ime-engine` 的内部组件，通过 Room DAO 响应式查询收藏列表。ImeEngine 的 reduce 函数在处理收藏相关 Intent 时，通过 `FavoriteService` 执行增删改查操作，操作结果通过新的 `ImeState` 反映。

收藏列表通过 Room 的 `Flow<List<InputFavorite>>` 实现响应式更新：数据库变更自动触发 Flow 发射，ImeEngine 订阅后更新 `ImeState.favoriteList.favorites`。

### 7.2 InputFavorite 数据模型

```kotlin
data class InputFavorite(
    /** 文本内容 */
    val text: String,
    /** 文本类型 */
    val type: InputTextType?,
    /** 使用次数 */
    val usageCount: Int = 0,
    /** 创建时间戳 */
    val createdAt: Long = 0L,
    /** 最后使用时间戳 */
    val usedAt: Long = 0L,
)
```

### 7.3 Feature 门控

收藏功能通过 `Feature.Favorites` 标记门控。当该功能被禁用时：

- `ImeState.favoriteList.disabled = true`
- `favoriteList.favorites` 始终为空列表
- 调用收藏相关 Intent（如 `SaveFavorite`、`DeleteFavorite`）立即抛出 `IllegalStateException`

---

## 8 PopupTipState 弹出提示状态

```kotlin
data class PopupTipState(
    /** 提示消息文本 */
    val message: String,
    /** 创建时间戳，用于自动 dismiss */
    val timestamp: Long = System.currentTimeMillis(),
    /** 提示类型 */
    val type: PopupTipType = PopupTipType.Info,
)
```

### 8.1 PopupTipType 提示类型

```kotlin
enum class PopupTipType {
    /** 信息提示（如切换键盘类型） */
    Info,
    /** 剪贴板提示（检测到新剪贴内容） */
    Clipboard,
    /** 编辑器操作提示（光标移动/范围选择） */
    Editor,
}
```

### 8.2 自动 Dismiss 机制

PopupTipState 通过以下机制自动消失：

1. **时间驱动**：UI 层在 `PopupTipPanel` 中启动定时器，默认超时 3000ms 后自动将 `popupTip` 设为 `null`
2. **Intent 驱动**：下一个 ImeIntent 到来时，reduce 函数检查 `popupTip.timestamp`，若超过超时阈值则自动清除
3. **状态驱动**：当 `keyboard.state` 发生状态转换时，已有的 `popupTip` 被清除（避免提示与当前状态不一致）

超时阈值通过 `ImeConfig.ui.popupTipTimeout` 配置，默认 3000ms。

---

## 9 ToolListState 工具栏状态

```kotlin
data class ToolListState(
    /** 工具项列表 */
    val tools: List<ToolItem>,
)

data class ToolItem(
    /** 工具显示标签 */
    val label: String,
    /** 工具图标 */
    val icon: ImageVector?,
    /** 点击工具后发送的 ImeIntent */
    val intent: ImeIntent,
    /** 是否禁用 */
    val disabled: Boolean = false,
)
```

### 9.1 工具项动态配置

工具栏的内容由 `KeyboardViewModel` 根据 `keyboard.type` 和 `keyboard.state` 动态配置：

| 键盘类型 | 工具项 |
|---------|--------|
| Pinyin/Latin | 全选、复制、粘贴、剪贴板、撤销、重做 |
| Editor | 全选、复制、剪切、粘贴、撤销 |
| Symbol/Emoji | 全选、复制、粘贴 |
| Number/Math | 全选、复制、粘贴 |

### 9.2 工具项与 Feature 门控

- 剪贴板工具项仅在 `Feature.Clipboard` 启用时显示
- 收藏工具项仅在 `Feature.Favorites` 启用时显示
- 撤销/重做工具项根据 `InputListEditor` 的 undoStack/redoStack 状态动态启用/禁用

---

## 10 状态变更流程

### 10.1 MVI 单向数据流

```
User Gesture → InputGesture → KeyboardViewModel → ImeIntent → ImeEngine.reduce()
                                                                          ↓
                                                                  new ImeState (via StateFlow)
                                                                          ↓
                                                              Compose UI (collectAsState)
                                                                          ↓
                                                              Panels re-compose
```

`ImeEngine.reduce(state: ImeState, intent: ImeIntent): ImeState` 是状态变更的唯一入口，纯函数，无副作用。副作用（如字典查询、剪贴板读取）通过返回 `List<ImeIntent>` 的副作用列表交由 ImeEngine 异步处理。

### 10.2 高频状态与低频状态分离

`GestureFeedbackState`（触摸轨迹、按键高亮、手指指示器） deliberately 不属于 ImeState，而是在 `:ime-ui` 模块中由 `KeyboardViewModel` 独立管理。这种分离确保高频手势更新（每帧 60fps）不会触发 ImeState 变更和不必要的 Compose 重组。

| 状态 | 更新频率 | 所属 |
|------|---------|------|
| ImeState | 按键级（ms 级） | `:ime-engine` |
| GestureFeedbackState | 帧级（16ms 级） | `:ime-ui` |
| InputActionPlayerState | 播放控制级（秒级） | `:ime-ui` |
| KeyLayoutState | 布局变更级（秒级） | `:ime-ui` |

---

## 11 与 Java 版本模型的对照

### 11.1 状态模型对照

| Java 版本 | v4 版本 | 变化说明 |
|-----------|---------|---------|
| `State` (Type + Data + previous) | `KeyboardState` sealed class | 从 Type+Data 组合改为 sealed class 层级，状态数据内嵌于各子类；previous 链表改为 `KeyboardStateHistory` 有界栈 |
| `State.Type.InputChars_Input_Wait_Doing` | `KeyboardState.PinyinInput.Waiting` | 从平面枚举改为嵌套 sealed class，语义更清晰 |
| `State.Type.InputChars_Slip_Doing` | `KeyboardState.PinyinInput.Slipping` | 数据从 `InputCharsSlipStateData` 内嵌到 Slipping 子类字段 |
| `State.Type.InputChars_Flip_Doing` | `KeyboardState.PinyinInput.Flipping` | 数据从 `InputCharsFlipStateData` 内嵌到 Flipping 子类字段 |
| `State.Type.InputChars_XPad_Input_Doing` | `KeyboardState.PinyinInput.XPadding` | 数据内嵌，XPad 视图状态不再独立为 `XPadState` |
| `State.Type.InputCandidate_Choose_Doing` | `KeyboardState.CandidateSelection.Choosing` | 分页数据从 `PagingStateData` 抽象类改为 CandidateList 子状态字段 |
| `State.Type.InputCandidate_Advance_Filter_Doing` | `KeyboardState.CandidateSelection.AdvanceFiltering` | 部首/声调过滤从 `PinyinCandidateAdvanceFilterStateData` 改为子类字段 |
| `State.Type.InputList_Commit_Option_Choose_Doing` | `KeyboardState.CommitOptionChoosing` | 数据从 `InputListCommitOptionChooseStateData` 内嵌 |
| `State.Type.Editor_Edit_Doing` | `KeyboardState.EditorEditing.CursorMoving/TextSelecting` | 从单一状态拆分为两个子状态 |
| `IMEditor` (中央编排器) | `ImeEngine` | 从持有可变状态改为暴露不可变 StateFlow |
| `InputList` (可变) | `InputList` (不可变 data class) | 从可变对象改为不可变 data class + copy() 模式 |
| `Inputboard.Stage` | `InputListEditor` undo/redo | 从 Stage（committed/cleaned）改为 undoStack/redoStack 双栈 |
| `XPadState` (视图层) | `KeyboardState.PinyinInput.XPadding` | 从独立视图状态改为引擎状态机子状态 |
| `PinyinWord.Filter` | `PinyinWordFilter` | 从内部类改为独立 data class，作为 CandidateList 字段 |
| `InputCompletions` | `InputList.pending + completions` | 从独立对象改为 InputList 内嵌字段 |

### 11.2 消息体系对照

| Java 版本 | v4 版本 | 变化说明 |
|-----------|---------|---------|
| `InputMsgType` (37 个值) | `ImeState` 字段 + `ImeOutput` | 输出消息从枚举驱动改为状态驱动，UI 订阅 StateFlow 自动响应 |
| `UserKeyMsgType` (13 个值) | `InputGesture` + `KeyGesture` | 从消息枚举改为 sealed class 层级 |
| `UserInputMsgType` (16 个值) | `ImeIntent` sealed class | 从消息枚举改为 Intent sealed class |
| `BaseInputContext` + 子类 | `ImeEngine.reduce()` 参数 | 从上下文传递改为 reduce 函数的闭包捕获 |

### 11.3 关键设计差异

1. **状态不可变性**：Java 版本中 `InputList`、`State.Data` 等均为可变对象，v4 全部改为不可变 data class，通过 `copy()` 生成新实例。
2. **状态历史**：Java 版本使用 `State.previous` 链表实现无限深度的状态回退，v4 改为 `KeyboardStateHistory` 有界栈（最大 10 层），并在键盘类型切换时清空历史。
3. **分页逻辑**：Java 版本的 `PagingStateData` 是可变抽象类，内部维护 `pageStart` 状态；v4 将分页状态（`pageIndex`、`pageSize`）作为 CandidateList 的不可变字段，分页操作通过 ImeIntent 触发 reduce 函数生成新的 ImeState。
4. **过滤逻辑**：Java 版本的 `PinyinCandidateFilterStateData` 在内部维护过滤结果缓存；v4 将过滤条件和过滤结果作为 CandidateList 的不可变字段，过滤操作通过 ImeIntent 触发重新计算。
5. **XPad 状态归属**：Java 版本中 `XPadState` 是视图层独立状态；v4 将 XPad 状态整合到引擎状态机的 `XPadding` 子状态中，遵循「引擎拥有所有逻辑状态」的原则。
6. **撤销机制**：Java 版本使用 `Inputboard.Stage`（committed/cleaned 两种类型，各保存一份 InputList 副本）；v4 改为 `InputListEditor` 的 undoStack/redoStack 双栈（最大 50 层），支持多次撤销和重做。
