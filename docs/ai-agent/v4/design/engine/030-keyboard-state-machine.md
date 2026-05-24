# 键盘状态机

```plantuml
@file:../diagrams/engine-state-machine.puml
```

## 1. KeyboardState 层次结构

`KeyboardState` 是键盘交互的有限状态集，以 sealed class 层级表达。每种状态对应一种键盘交互模式，状态数据作为子类字段内嵌，编译期保证类型正确与穷举检查。整个层次结构分为六大分支：空闲、拼音输入、候选选择、提交选项、编辑器编辑、以及符号/Emoji 选择。拼音输入分支包含三种子状态——等待（`Waiting`）、滑行（`Slipping`）与翻动（`Flipping`），覆盖了拼音键盘的核心交互方式。候选选择分支包含三个递进子状态——选择（`Choosing`）、过滤（`Filtering`）与高级过滤（`AdvanceFiltering`），形成逐层细化的候选筛选链路。编辑器编辑分支将光标移动和文本范围选择拆分为独立子状态，避免使用枚举字段区分编辑目标。所有子类均为 `data class` 或 `data object`，支持结构化相等比较与不可变 `copy()` 语义。

```kotlin
sealed class KeyboardState {
    data object Idle : KeyboardState()

    sealed class PinyinInput : KeyboardState() {
        data class Waiting(val pending: InputItem.Char?) : PinyinInput()
        data class Slipping(
            val startKey: InputKey,
            val level0Key: InputKey,
            val level1Key: InputKey?,
            val level2Key: InputKey?,
            val nextCharsByLength: Map<Int, List<String>>,
        ) : PinyinInput()
        data class Flipping(val startChar: String, val candidates: List<String>) : PinyinInput()
    }

    sealed class CandidateSelection : KeyboardState() {
        data class Choosing(val candidates: List<InputWord>, val pageIndex: Int, val pageSize: Int)
        data class Filtering(val filter: PinyinWordFilter, val filtered: List<InputWord>)
        data class AdvanceFiltering(val radical: PinyinWord.Radical?, val tone: PinyinWord.Tone?, val filtered: List<InputWord>)
    }

    data class CommitOptionChoosing(val options: List<CommitOption>, val hasSpell: Boolean, val hasVariant: Boolean)

    sealed class EditorEditing : KeyboardState() {
        data class CursorMoving(val position: Int) : EditorEditing()
        data class TextSelecting(val start: Int, val end: Int) : EditorEditing()
    }

    data class SymbolChoosing(val groupId: String?)
    data class EmojiChoosing(val groupId: String?)
}
```

### 1.1 状态层次一览

| 顶层状态 | 子状态 | 说明 |
|----------|--------|------|
| `Idle` | — | 空闲状态，数字/数学/编辑键盘的默认状态 |
| `PinyinInput` | `Waiting` | 等待输入，`pending` 为未确认的拼音字符 |
| | `Slipping` | 滑行输入中，三级按键层次结构 |
| | `Flipping` | 翻动输入中，以起始字符展示全部候选 |
| `CandidateSelection` | `Choosing` | 候选词选择，含分页数据 |
| | `Filtering` | 拼音过滤，应用声调/拼写筛选 |
| | `AdvanceFiltering` | 高级过滤（部首/声调） |
| `CommitOptionChoosing` | — | 提交选项选择 |
| `EditorEditing` | `CursorMoving` | 光标移动中 |
| | `TextSelecting` | 文本范围选择中 |
| `SymbolChoosing` | — | 符号选择，含分组信息 |
| `EmojiChoosing` | — | Emoji 选择，含分组信息 |

### 1.2 Slipping 状态数据说明

滑行输入（Slip）是拼音键盘的核心交互方式，用户手指从起始按键滑出，经过中间级按键，最终到达目标按键完成输入。滑行输入采用三级按键层次结构：

- **level0Key**：滑行起始按键，手指按下时确定的按键
- **level1Key**：滑行第一级目标按键，手指从起始按键滑出后的第一个中间按键
- **level2Key**：滑行第二级目标按键，手指继续滑行到达的最终按键

`nextCharsByLength` 记录在当前滑行路径下，按字符长度分组的可选后续字符列表。例如，滑行到 `level1Key` 后，可能产生的后续字符按长度 1、2、3 等分组存储。此映射用于 UI 层预显示可能的后续输入选项，使键盘在滑行过程中能够实时展示预测字符，提升输入效率。

### 1.3 Flipping 状态数据说明

翻动输入（Flip）是在首字母按键上快速滑出触发的输入方式，`startChar` 为起始字符，`candidates` 为翻动产生的候选字符列表。翻动输入是一种快捷输入方式，允许用户通过快速滑动选择同一起始字符下的不同变体。当用户在某个声母按键上快速向外滑动时，引擎收集以该声母开头的所有韵母组合对应的候选字符，以列表形式呈现，用户点击即可完成选择。翻动输入与滑行输入共享手指滑动触发的入口，但翻动以快速短滑为触发条件，滑行以持续移动为触发条件。

### 1.4 CandidateSelection 状态数据说明

候选选择有三个子状态，形成 **选择 → 过滤 → 高级过滤** 的递进关系：

**Choosing**：基础候选选择状态，`candidates` 为当前候选词列表，`pageIndex` 和 `pageSize` 控制分页显示。分页采用轮播模式：翻到末页后继续翻页回到首页，翻到首页前继续翻页跳到末页。

**Filtering**：在基础选择上应用拼音过滤器，`filter` 包含声调和拼写过滤条件，`filtered` 为过滤后的候选词列表。过滤时会合并首页高频词和后续页面词，高频词不再单独占用首页。

**AdvanceFiltering**：在基础选择上应用部首和声调的高级过滤，`radical` 为选中的部首，`tone` 为选中的声调，`filtered` 为过滤后的候选词列表。部首列表从候选词中动态提取，按权重排序（出现频次高的部首优先，笔画数少的优先）。

### 1.5 CommitOptionChoosing 状态数据说明

提交选项选择是输入列表中针对已确认输入的后续操作选择。`options` 为可用的提交选项列表，`hasSpell` 表示当前输入列表中是否有包含拼音拼写的候选字，`hasVariant` 表示是否有包含变体（繁体/异体）的候选字。这两个布尔值控制提交选项面板中「切换拼写模式」和「切换变体」按钮的显示与否。当 `hasSpell` 为 `false` 时，拼写模式切换按钮不可见；当 `hasVariant` 为 `false` 时，变体切换按钮不可见。这种门控机制避免向用户展示无意义的操作选项。

### 1.6 EditorEditing 状态数据说明

编辑器编辑包含两种子状态：`CursorMoving` 控制光标在输入列表中的位置移动，`TextSelecting` 控制文本范围选择。两种编辑模式通过编辑手势触发切换：短按进入光标移动，长按后滑动进入范围选择。`position` 为光标在输入列表中的索引位置，`start` 和 `end` 为文本选择的起止索引。所有索引均为 `InputList` 中的输入项位置，而非字符偏移量，因为输入列表的粒度是输入项（`InputItem.Char`），而非单个字符。将光标移动和文本选择拆分为独立子状态，使得状态机可以针对两种编辑模式定义不同的合法转换，避免运行时通过枚举字段区分编辑目标。

### 1.7 SymbolChoosing 与 EmojiChoosing 状态数据说明

符号选择和表情选择均支持分组浏览。`groupId` 标识当前选中的分组，`null` 表示默认分组或未选择分组。切换分组时重置分页到首页。符号选择的分组由符号数据定义（中文符号、拉丁符号、数学符号、配对符号），表情选择的分组由表情数据文件的分组键名定义。符号选择还支持 `onlyPair` 模式，仅显示配对符号（如括号、引号），该模式在输入列表中插入配对符号时使用。

---

## 2. KeyboardStateTransition 转换体系

`KeyboardStateTransition` 是触发状态机转换的原子事件 sealed class。每个转换类型携带转换所需的上下文数据，由 `KeyboardIntentHandler` 子类在处理 `ImeIntent` 时构造并提交给 `KeyboardStateMachine`。转换体系分为五大类别：拼音输入转换、候选选择转换、提交选项转换、编辑器转换、以及符号/Emoji 转换。此外，`ReturnToIdle` 和 `BackToPrevious` 作为通用转换，在任何状态下均可能被触发。`Result` 作为嵌套类型，表达转换执行后的输出——新状态与副作用意图列表。这种设计使得状态转换的输入（`KeyboardStateTransition`）和输出（`Result`）在类型归属上形成闭环，编译期即可校验转换结果的合法性。

```kotlin
sealed class KeyboardStateTransition {
    data class InputPinyinChar(val char: Char)
    data class BeginSlip(val startKey: InputKey)
    data class BeginFlip(val startChar: Char)
    data class SelectSlipChar(val char: Char)
    data class SelectFlipChar(val char: Char)
    data class LoadCandidates(val candidates: List<InputWord>)
    data class FilterCandidates(val filter: PinyinWordFilter)
    data class AdvanceFilterCandidates(val radical: PinyinWord.Radical?, val tone: PinyinWord.Tone?)
    data class PageCandidates(val direction: PageDirection)
    data class LoadCommitOptions(val options: List<CommitOption>)
    data class MoveCursor(val position: Int)
    data class SelectText(val start: Int, val end: Int)
    data class OpenSymbolGroup(val groupId: String?)
    data class OpenEmojiGroup(val groupId: String?)
    data object ReturnToIdle
    data object BackToPrevious
    data class Result(val newState: KeyboardState, val sideEffects: List<ImeIntent> = emptyList())
}
```

### 2.1 转换类型一览

| 类别 | 转换类型 | 说明 | 触发场景 |
|------|---------|------|---------|
| 拼音输入 | `InputPinyinChar` | 输入拼音字符 | 单击字母按键 |
| | `BeginSlip` | 开始滑行输入 | 手指在字母按键上开始移动 |
| | `BeginFlip` | 开始翻动输入 | 在首字母按键上快速滑出 |
| | `SelectSlipChar` | 选择滑行字符 | 滑行输入中手指到达目标按键 |
| | `SelectFlipChar` | 选择翻动字符 | 翻动输入中单击目标按键 |
| 候选选择 | `LoadCandidates` | 加载候选词 | 输入拼音后自动加载 / 选中拼音输入 |
| | `FilterCandidates` | 过滤候选词 | 点击拼音过滤按键 |
| | `AdvanceFilterCandidates` | 高级过滤 | 点击高级过滤按键 |
| | `PageCandidates` | 翻页候选词 | 手指翻动翻页 |
| 提交选项 | `LoadCommitOptions` | 加载提交选项 | 长按提交按键 |
| 编辑器 | `MoveCursor` | 移动光标 | 在光标定位按键上滑动 |
| | `SelectText` | 选择文本 | 在光标定位按键上长按后滑动 |
| 符号/Emoji | `OpenSymbolGroup` | 打开符号分组 | 点击符号分组切换按键 |
| | `OpenEmojiGroup` | 打开 Emoji 分组 | 点击 Emoji 分组切换按键 |
| 通用 | `ReturnToIdle` | 回到空闲/初始状态 | 提交输入 / 清空输入 / 退出键盘 |
| | `BackToPrevious` | 回退到前一状态 | 编辑器编辑结束 / 高级过滤确认 |

### 2.2 Result 副作用机制

`Result` 的 `sideEffects` 字段是状态转换产生的副作用意图列表，包含需要异步处理的操作（如字典查询、音频播放、编辑器桥接等）。`KeyboardStateMachine` 本身是纯函数式的——`transition()` 方法根据当前状态和转换类型计算新状态与副作用列表，不直接执行任何副作用。副作用列表由 `ImeEngine` 在获得 `Result` 后异步处理，确保状态计算与副作用执行的解耦。`Result` 作为 `KeyboardStateTransition` 的嵌套类型，表达转换与结果之间的归属关系，使得调用方可以清晰地从类型签名中识别返回值的来源。

---

## 3. KeyboardStateMachine 状态机

`KeyboardStateMachine` 是状态转换的集中处理器，接收 `KeyboardStateTransition`，根据当前状态执行转换规则，返回 `KeyboardStateTransition.Result`。状态机内部维护当前状态 `_state` 和有界历史栈 `stateHistory`，对外仅暴露只读的 `state` 属性。`transition()` 方法通过 `when` 对当前状态的 sealed class 层级进行穷举匹配，每个分支委托给对应的 `handleFrom*` 私有方法处理。新状态与当前状态不同时，自动将当前状态压入历史栈并更新 `_state`。状态机依赖 `KeyAudioPlayer` 播放按键音效，依赖 `InputListOperator` 执行输入列表操作——这两个依赖通过构造函数注入，使得状态机本身不持有字典或 UI 引用，保持职责单一。

```kotlin
class KeyboardStateMachine(
    private val audioPlayer: KeyAudioPlayer,
    private val inputListOp: InputListOperator,
) {
    private var _state: KeyboardState = KeyboardState.Idle
    val state: KeyboardState get() = _state
    private val stateHistory = KeyboardStateHistory(maxSize = 10)

    fun transition(transition: KeyboardStateTransition): KeyboardStateTransition.Result {
        val (newState, sideEffects) = when (_state) {
            is KeyboardState.Idle -> handleFromIdle(transition)
            is KeyboardState.PinyinInput.Waiting -> handleFromPinyinWaiting(transition)
            is KeyboardState.PinyinInput.Slipping -> handleFromPinyinSlipping(transition)
            is KeyboardState.PinyinInput.Flipping -> handleFromPinyinFlipping(transition)
            is KeyboardState.CandidateSelection.Choosing -> handleFromCandidateChoosing(transition)
            is KeyboardState.CandidateSelection.Filtering -> handleFromCandidateFiltering(transition)
            is KeyboardState.CandidateSelection.AdvanceFiltering -> handleFromCandidateAdvanceFiltering(transition)
            is KeyboardState.CommitOptionChoosing -> handleFromCommitOptionChoosing(transition)
            is KeyboardState.EditorEditing.CursorMoving -> handleFromEditorCursorMoving(transition)
            is KeyboardState.EditorEditing.TextSelecting -> handleFromEditorTextSelecting(transition)
            is KeyboardState.SymbolChoosing -> handleFromSymbolChoosing(transition)
            is KeyboardState.EmojiChoosing -> handleFromEmojiChoosing(transition)
        }

        if (newState != _state) {
            stateHistory.push(_state)
            _state = newState
        }
        return KeyboardStateTransition.Result(newState, sideEffects)
    }

    fun backToPrevious() {
        _state = stateHistory.pop() ?: KeyboardState.Idle
    }

    fun resetToIdle() {
        _state = KeyboardState.Idle
        stateHistory.clear()
    }

    fun resetTo(state: KeyboardState) {
        _state = state
        stateHistory.clear()
    }
}
```

### 3.1 纯函数式转换原则

`transition()` 方法遵循纯函数式转换原则：给定相同的当前状态和转换类型，始终产生相同的新状态和副作用列表。状态机不直接修改 `InputList` 或调用字典查询——这些操作通过副作用意图列表交由 `ImeEngine` 异步处理。音频播放也作为副作用的一部分，由 `ImeEngine` 在处理副作用时调用 `KeyAudioPlayer`。这种设计使得状态转换逻辑可以脱离运行时环境进行单元测试，只需验证 `transition()` 返回的 `Result` 是否符合预期。

### 3.2 Fail-safe 策略

当某个转换类型在当前状态下不合法时（例如在 `Flipping` 状态下收到 `BeginSlip`），对应的 `handleFrom*` 方法返回保持当前状态不变的 `Result`，即 **Fail-safe** 策略：不抛异常，不崩溃，保持原状态。这种策略确保状态机在面对意外输入时始终处于安全状态，而非进入未定义行为。每个 `handleFrom*` 方法的 `else` 分支均返回 `(currentState, emptyList())`，即维持原状、无副作用。

---

## 4. KeyboardIntentHandler 接口与实现

### 4.1 KeyboardIntentHandler 接口

`KeyboardIntentHandler` 是键盘意图处理器的接口，按不同 `KeyboardType` 创建子类，各自处理从 `ImeIntent` 到 `KeyboardStateTransition` 的映射。各子类是无状态的策略对象——它们不持有可变状态，状态由 `KeyboardStateMachine` 集中管理。各子类的 `handleIntent()` 方法负责将 `ImeIntent` 映射为 `KeyboardStateTransition`，再由 `KeyboardStateMachine` 执行转换。这种设计使得意图的语义与状态转换规则解耦：同一个 `ImeIntent` 在不同键盘类型下可以产生不同的 `KeyboardStateTransition`，而状态机不需要感知意图的来源。

```kotlin
interface KeyboardIntentHandler {
    val type: KeyboardType

    fun handleIntent(intent: ImeIntent, state: KeyboardState): KeyboardStateTransition.Result
}
```

### 4.2 KeyboardIntentHandler 子类层次

```
KeyboardIntentHandler (interface)
├── PinyinKeyboardIntentHandler          — 拼音键盘意图处理（主键盘），同时处理 Latin
├── NumberKeyboardIntentHandler          — 数字键盘意图处理（主键盘）
├── MathKeyboardIntentHandler            — 数学键盘意图处理（临时键盘）
├── SymbolKeyboardIntentHandler          — 符号键盘意图处理（临时键盘）
├── EmojiKeyboardIntentHandler           — Emoji 键盘意图处理（临时键盘）
├── EditorKeyboardIntentHandler          — 编辑键盘意图处理（临时键盘）
├── CandidateKeyboardIntentHandler       — 候选键盘意图处理（超临时键盘）
├── CommitOptionKeyboardIntentHandler    — 提交选项键盘意图处理（超临时键盘）
```

`PinyinKeyboardIntentHandler` 同时处理 `KeyboardType.Pinyin` 和 `KeyboardType.Latin`，因为拉丁键盘复用拼音的滑行交互模式。两者的差异在于：拉丁模式不支持翻动输入，且输入列表为空时按键直接提交到编辑器（直输模式），不在输入列表中停留和预处理。

### 4.3 各 Handler 职责

#### PinyinKeyboardIntentHandler

拼音输入的核心意图处理器，支持三种输入模式（点击、滑行、翻动），管理拼音字符输入、候选字查询和输入补全。同时处理 `KeyboardType.Latin` 的意图转换。

| `ImeIntent` | 处理流程 | 产生的 `KeyboardStateTransition` |
|-------------|---------|-------------------------------|
| `PressKey(CharKey.Alphabet)` | 单击字母按键，追加到 pending 或新建 pending，查询候选字 | `InputPinyinChar` |
| `PressKey(CharKey.Alphabet)` + `FingerMoving` | 开始滑行输入，进入 `Slipping` 状态 | `BeginSlip` |
| `FingerFlipping`（`Slipping` 中） | 在滑行中触发翻动，进入 `Flipping` 状态 | `BeginFlip` |
| `FingerMovingStop`（`Slipping` 中） | 结束滑行输入，确认 pending，回到 `Waiting` | `SelectSlipChar` + `ReturnToIdle` |
| `PressKey(CharKey.Symbol/Emoji)` | 符号/表情直输或替换输入 | `InputPinyinChar` |
| `PressKey(CtrlKey.Backspace)` | 回删输入列表或编辑器 | 不产生 Transition，直接操作 `InputList` |
| `PressKey(CtrlKey.Space/Enter)` | 确认 pending + 录入空格/换行 | `ReturnToIdle` |
| `PressKey(CtrlKey.Commit)` | 提交输入列表并回到初始状态 | `ReturnToIdle` |
| `LongPressKey(CtrlKey.Commit)` | 进入提交选项键盘 | 切换到 `CommitOption` 键盘 |

内部状态转换流程：

1. **点击输入**：`Waiting` → `InputPinyinChar` → `Waiting`（单字符追加到 pending）
2. **滑行输入**：`Waiting` → `BeginSlip` → `Slipping` → `SelectSlipChar` → `Waiting`（多字符连续输入）
3. **翻动输入**：`Slipping` → `BeginFlip` → `Flipping` → `SelectFlipChar` → `Waiting`（快速展示首字母全部候选）
4. **候选选择**：`Waiting` → `LoadCandidates` → `CandidateSelection.Choosing`（选中拼音输入后自动进入）

#### NumberKeyboardIntentHandler

纯数字键盘的意图处理，支持数字和部分符号（+、-、#、*）的直接输入。与 Latin 键盘类似，支持直输和输入列表两种模式。

| `ImeIntent` | 处理流程 | 产生的 `KeyboardStateTransition` |
|-------------|---------|-------------------------------|
| `PressKey(CharKey.Number)` | 单字符输入 | `InputPinyinChar` |
| `PressKey(CtrlKey.Commit)` | 提交输入列表 | `ReturnToIdle` |
| `LongPressKey(CtrlKey.Commit)` | 禁用（避免切换到提交选项键盘） | 不产生 Transition |

#### MathKeyboardIntentHandler

数学表达式键盘，管理嵌套的数学 `InputList`，支持数字、运算符和括号的输入，自动计算表达式结果。

| `ImeIntent` | 处理流程 | 产生的 `KeyboardStateTransition` |
|-------------|---------|-------------------------------|
| `PressKey(CharKey.Number)` | 数字追加到数学输入列表的 pending | `InputPinyinChar` |
| `PressKey(MathOpKey)` | 运算符输入（点号、括号、等号等） | `InputPinyinChar` |
| `PressKey(CtrlKey.Backspace)` | 回删数学输入列表或上层输入列表 | 不产生 Transition，直接操作数学 `InputList` |
| `PressKey(CtrlKey.Commit)` | 提交父输入列表，退出键盘 | `ReturnToIdle` |

**嵌套 InputList 机制**：`MathKeyboardIntentHandler` 维护两层 `InputList`——父输入列表（上层）和数学输入列表（嵌套在 `InputItem.MathExpr` 中）。所有数字和运算符输入在数学输入列表中操作，提交时将整个数学表达式作为 `InputItem.MathExpr` 提交到父输入列表。

#### SymbolKeyboardIntentHandler

符号选择键盘，提供分组浏览和翻页选择符号的能力。支持配对符号输入（如括号、引号）。

| `ImeIntent` | 处理流程 | 产生的 `KeyboardStateTransition` |
|-------------|---------|-------------------------------|
| `PressKey(SymbolKey)` | 选择符号，配对符号做包裹，非配对符号做替换/直输 | `ReturnToIdle`（非连续输入后退出） |
| `LongPressKey(SymbolKey)` | 连续输入符号 | 不退出键盘 |
| `PressKey(CtrlKey.Toggle_Symbol_Group)` | 切换符号分组 | `OpenSymbolGroup` |
| `FingerFlipping` | 翻页符号 | `PageCandidates` |

配对符号处理流程：若选中的输入已有配对符号，替换左右符号；若 pending 非空且非符号输入，用配对符号包裹 pending（左符号 + Gap + 被包裹输入 + 右符号）；否则，左右符号依次输入，光标放在中间位置。

#### EmojiKeyboardIntentHandler

Emoji 选择键盘，提供分组浏览和翻页选择 Emoji 的能力。

| `ImeIntent` | 处理流程 | 产生的 `KeyboardStateTransition` |
|-------------|---------|-------------------------------|
| `PressKey(InputWordKey)` | 选择 Emoji，输入列表非空时做替换，空时直输 | `ReturnToIdle` |
| `LongPressKey(InputWordKey)` | 连续输入 Emoji | 不退出键盘 |
| `PressKey(CtrlKey.Toggle_Emoji_Group)` | 切换 Emoji 分组 | `OpenEmojiGroup` |
| `FingerFlipping` | 翻页 Emoji | `PageCandidates` |

#### EditorKeyboardIntentHandler

文本编辑功能键盘，提供光标移动、文本选择、复制、粘贴等编辑操作。

| `ImeIntent` | 处理流程 | 产生的 `KeyboardStateTransition` |
|-------------|---------|-------------------------------|
| `PressKey(CtrlKey.Edit_Editor)` | 执行编辑操作（复制、粘贴等） | 不产生 Transition，通过 `EditorAction` 输出 |
| `LongPressKey(CtrlKey.Editor_Cursor_Locator)` | 进入范围选择编辑 | `SelectText` |
| `FingerMoving_Start`（光标定位按键） | 进入光标移动编辑 | `MoveCursor` |

#### CandidateKeyboardIntentHandler

拼音候选字选择键盘，提供候选词浏览、拼音过滤、高级过滤（部首/声调）和候选字确认。是使用频率最高的超临时键盘。

| `ImeIntent` | 处理流程 | 产生的 `KeyboardStateTransition` |
|-------------|---------|-------------------------------|
| `PressKey(InputWordKey)` | 选择候选词，确认 pending 中的候选字 | `ReturnToIdle`（无下一个拼音）/ 继续选择 |
| `PressKey(CtrlKey.ConfirmInput)` | 确认当前候选字 | `ReturnToIdle` |
| `PressKey(CtrlKey.Toggle_Pinyin_Spell)` | 切换拼音修正模式 | `FilterCandidates` |
| `PressKey(CtrlKey.Filter_PinyinCandidate_by_Spell)` | 按拼写过滤候选词 | `FilterCandidates` |
| `PressKey(CtrlKey.Filter_PinyinCandidate_advance)` | 进入高级过滤 | `AdvanceFilterCandidates` |
| `PressKey(CtrlKey.Confirm_PinyinCandidate_Filter)` | 确认高级过滤条件，回到候选选择 | `BackToPrevious` |
| `FingerFlipping` | 翻页候选词 | `PageCandidates` |

候选字确认流程：确认 pending 中的候选字 → 确认 pending 并选中下一个输入 → 查找下一个拼音输入 → 若有则继续选择候选字，若无则退出键盘 → 自动进行短语预测和输入补全。

#### CommitOptionKeyboardIntentHandler

输入列表提交选项键盘，控制提交到目标编辑器的内容形式（拼音拼写模式、繁简变体）。

| `ImeIntent` | 处理流程 | 产生的 `KeyboardStateTransition` |
|-------------|---------|-------------------------------|
| `PressKey(CtrlKey.Commit_InputList_Option)` | 切换提交选项（拼写模式/繁简变体） | 不产生 Transition，更新 `InputList.inputOption` |
| `PressKey(CtrlKey.Commit)` | 提交输入列表并退出 | `ReturnToIdle` |
| `PressKey(CtrlKey.Exit)` | 退出回到切换前的键盘 | 切换到前一键盘 |

### 4.4 Handler 注册表机制

`ImeEngine` 维护一个 `Map<KeyboardType, KeyboardIntentHandler>` 注册表，根据当前 `Keyboard.type` 选择对应的 `KeyboardIntentHandler` 实现。当 `ImeIntent.SwitchKeyboard` 到达时，引擎根据目标键盘类型查找对应 Handler 并重置状态；其他 Intent 则分派给当前键盘类型对应的 Handler 处理。

```kotlin
class ImeEngine internal constructor(
    private val handlers: Map<KeyboardType, KeyboardIntentHandler>,
    // ...
) {
    fun handleIntent(intent: ImeIntent) {
        when (intent) {
            is ImeIntent.SwitchKeyboard -> {
                val handler = handlers[intent.type]
                if (handler != null) {
                    val newKeyboard = _state.value.keyboard.copy(
                        type = intent.type,
                        state = intent.type.initialState,
                    )
                    _state.update { it.copy(keyboard = newKeyboard) }
                }
            }
            else -> {
                val handler = handlers[_state.value.keyboard.type] ?: return
                val result = handler.handleIntent(intent, _state.value.keyboard.state)
                val newKeyboard = _state.value.keyboard.copy(state = result.newState)
                _state.update { it.copy(keyboard = newKeyboard) }
                result.sideEffects.forEach { sideEffect ->
                    handleIntent(sideEffect)
                }
            }
        }
    }
}
```

---

## 5. Keyboard 组合模式

### 5.1 Keyboard data class

`Keyboard` 将键盘的类型、左右手模式临时状态和交互状态封装为一个不可变的 `data class`，通过组合模式替代继承。三个字段各自承担独立的职责维度：`type` 决定按键集合的语义内容，`handMode` 记录左右手模式的临时切换（`null` 表示未切换，使用 `UiConfig.keyboardHandMode` 的值），`state` 记录当前键盘状态机的精确位置。键盘输入模式（`KeyboardInputMode`）不再是 `Keyboard` 的字段，仅通过 `ImeConfig.UiConfig.keyboardInputMode` 配置变更。状态变更通过 `copy()` 生成新实例，原始实例不受影响。

```kotlin
data class Keyboard(
    val type: KeyboardType = KeyboardType.Pinyin,
    val handMode: KeyboardHandMode? = null,
    val state: KeyboardState = KeyboardState.Idle,
)
```

### 5.2 KeyboardType 枚举

```kotlin
enum class KeyboardType {
    Pinyin,       // 拼音键盘：主键盘
    Latin,        // 拉丁键盘：主键盘
    Number,       // 数字键盘：主键盘
    Math,         // 数学键盘：临时键盘
    Symbol,       // 符号键盘：临时键盘
    Emoji,        // Emoji 键盘：临时键盘
    Editor,       // 编辑键盘：临时键盘
    Candidate,    // 候选键盘：超临时键盘
    CommitOption, // 提交选项键盘：超临时键盘
}
```

键盘类型分类：

| 分类 | `KeyboardType` | 说明 |
|------|---------------|------|
| 主键盘 | `Pinyin`、`Latin`、`Number` | 常驻性键盘，其他键盘退出后回到主键盘 |
| 临时键盘 | `Math`、`Symbol`、`Emoji`、`Editor` | 从主键盘切换进入，退出时回到切换前的主键盘 |
| 超临时键盘 | `Candidate`、`CommitOption` | 选择操作完成后立即返回，生命周期极短 |

### 5.3 KeyboardInputMode 枚举

```kotlin
enum class KeyboardInputMode {
    HexGrid,   // 六边形网格（六边形按键排列）
    RectGrid,  // 矩形网格（传统 QWERTY 排列）
}
```

`KeyboardInputMode` 定义了按键的几何排列和交互范式，通过 `ImeConfig.UiConfig.keyboardInputMode` 配置，不再是 `Keyboard` 的字段。键盘输入模式只能通过配置变更，不支持运行时临时修改。不同输入模式决定按键的几何排列和手势识别策略——`RectGrid` 采用传统矩形按键布局，手势识别基于方向向量；`HexGrid` 采用六边形按键布局，手势识别基于轴向坐标。

### 5.4 KeyboardType → 初始 KeyboardState 映射

| `KeyboardType` | 初始 `KeyboardState` | 说明 |
|----------------|---------------------|------|
| `Pinyin` | `PinyinInput.Waiting(null)` | 拼音键盘等待输入 |
| `Latin` | `PinyinInput.Waiting(null)` | 拉丁键盘复用拼音的输入模式 |
| `Number` | `Idle` | 数字键盘无子状态 |
| `Math` | `Idle` | 数学键盘无子状态（但使用嵌套 `InputList`） |
| `Symbol` | `SymbolChoosing(null)` | 符号键盘默认进入符号选择 |
| `Emoji` | `EmojiChoosing(null)` | Emoji 键盘默认进入 Emoji 选择 |
| `Editor` | `Idle` | 编辑键盘无子状态 |
| `Candidate` | `CandidateSelection.Choosing(emptyList(), 0, 0)` | 候选键盘直接进入候选选择 |
| `CommitOption` | `CommitOptionChoosing(emptyList(), false, false)` | 提交选项键盘直接进入选项选择 |

### 5.5 键盘类型之间的切换逻辑

键盘切换遵循 **主键盘 → 临时键盘 → 超临时键盘** 的层级关系，回退方向为反向。

切换场景：

| 触发场景 | 切换方向 | 说明 |
|---------|---------|------|
| 点击键盘切换按键 | 主键盘 → 主键盘 | `Pinyin` ↔ `Latin` ↔ `Number` |
| 输入拼音后选中 | 主键盘 → 超临时键盘 | `Pinyin` → `Candidate` |
| 长按提交按键 | 主/临时键盘 → 超临时键盘 | → `CommitOption` |
| 选中数学输入 | 主键盘 → 临时键盘 | → `Math` |
| 选中符号/表情输入 | 主键盘 → 临时键盘 | → `Symbol` / `Emoji` |
| 双击光标定位按键 | 主键盘 → 临时键盘 | → `Editor` |

回退逻辑：

| 当前键盘 | 退出行为 | 说明 |
|---------|---------|------|
| 超临时键盘（`Candidate` / `CommitOption`） | 选择完成后自动回到切换前键盘 | 生命周期极短 |
| 临时键盘（`Math` / `Symbol` / `Emoji` / `Editor`） | 退出时回到切换前的主键盘 | `switch_Keyboard_to_Previous` |
| 主键盘 | 无回退 | 常驻性键盘 |

---

## 6. 完整状态转换规则

以下表格列出每种当前状态下所有合法转换的完整规则。未列出的转换类型在对应状态下均为 Fail-safe（保持原状态、无副作用）。每个表格中的「副作用」列标识 `Result.sideEffects` 中包含的典型 `ImeIntent`。

### 6.1 从 Idle 状态转换

| 当前状态 | 转换类型 | 新状态 | 副作用 | 说明 |
|---------|---------|-------|--------|------|
| `Idle` | `InputPinyinChar` | `PinyinInput.Waiting` | 查询候选字 | 键盘切换到拼音/拉丁时进入 |
| `Idle` | `OpenSymbolGroup` | `SymbolChoosing` | — | 键盘切换到符号时进入 |
| `Idle` | `OpenEmojiGroup` | `EmojiChoosing` | — | 键盘切换到 Emoji 时进入 |
| `Idle` | `MoveCursor` | `EditorEditing.CursorMoving` | — | 编辑器操作触发 |
| `Idle` | `LoadCandidates` | `CandidateSelection.Choosing` | — | 键盘切换到候选时进入 |
| `Idle` | `LoadCommitOptions` | `CommitOptionChoosing` | — | 键盘切换到提交选项时进入 |
| `Idle` | 其他 | **保持 `Idle`** | — | Fail-safe |

### 6.2 从 PinyinInput.Waiting 状态转换

| 当前状态 | 转换类型 | 新状态 | 副作用 | 说明 |
|---------|---------|-------|--------|------|
| `Waiting` | `InputPinyinChar` | `PinyinInput.Waiting` | 查询候选字 | 字符追加到 pending |
| `Waiting` | `BeginSlip` | `PinyinInput.Slipping` | 播放滑行音效 | 开始滑行输入 |
| `Waiting` | `BeginFlip` | `PinyinInput.Flipping` | 播放翻动音效 | 开始翻动输入 |
| `Waiting` | `LoadCandidates` | `CandidateSelection.Choosing` | — | 加载候选词进入候选选择 |
| `Waiting` | `ReturnToIdle` | `Idle` | 提交/清空输入 | 提交/清空输入后回到空闲 |
| `Waiting` | 其他 | **保持 `Waiting`** | — | Fail-safe |

### 6.3 从 PinyinInput.Slipping 状态转换

| 当前状态 | 转换类型 | 新状态 | 副作用 | 说明 |
|---------|---------|-------|--------|------|
| `Slipping` | `SelectSlipChar` | `PinyinInput.Waiting` | 确认 pending 字符 | 滑行字符选中后回到等待 |
| `Slipping` | `BeginFlip` | `PinyinInput.Flipping` | 播放翻动音效 | 滑行中触发翻动 |
| `Slipping` | `LoadCandidates` | `CandidateSelection.Choosing` | — | 滑行输入结束加载候选词 |
| `Slipping` | `ReturnToIdle` | `PinyinInput.Waiting` | 丢弃 pending | 异常终止回到等待状态 |
| `Slipping` | 其他 | **保持 `Slipping`** | — | Fail-safe |

滑行输入的详细转换过程：手指在字母按键上开始移动时触发 `BeginSlip`，进入 `Slipping` 状态并记录 `startKey` 和 `level0Key`；手指继续移动时，状态保持在 `Slipping`，内部更新 `level1Key` 和 `level2Key` 以及 `nextCharsByLength`，但不产生状态转换；手指停止移动或到达目标按键时触发 `SelectSlipChar`，确认 pending 中的字符并回到 `Waiting`。

### 6.4 从 PinyinInput.Flipping 状态转换

| 当前状态 | 转换类型 | 新状态 | 副作用 | 说明 |
|---------|---------|-------|--------|------|
| `Flipping` | `SelectFlipChar` | `PinyinInput.Waiting` | 确认 pending 字符 | 翻动字符选中后回到等待 |
| `Flipping` | `LoadCandidates` | `CandidateSelection.Choosing` | — | 翻动输入结束加载候选词 |
| `Flipping` | `ReturnToIdle` | `PinyinInput.Waiting` | 丢弃 pending | 异常终止回到等待状态 |
| `Flipping` | 其他 | **保持 `Flipping`** | — | Fail-safe |

翻动输入与滑行输入的区别在于：翻动输入展示的是同一起始字符下的所有候选字符列表，用户通过点击选择目标字符；而滑行输入是通过手指持续移动经过不同级别的按键完成多字符输入。翻动输入的触发条件是在首字母按键上的快速短滑，引擎识别为翻动手势后自动收集候选并进入 `Flipping` 状态。

### 6.5 从 CandidateSelection.Choosing 状态转换

| 当前状态 | 转换类型 | 新状态 | 副作用 | 说明 |
|---------|---------|-------|--------|------|
| `Choosing` | `FilterCandidates` | `CandidateSelection.Filtering` | 查询过滤结果 | 应用拼音过滤 |
| `Choosing` | `AdvanceFilterCandidates` | `CandidateSelection.AdvanceFiltering` | 查询高级过滤结果 | 进入高级过滤 |
| `Choosing` | `ReturnToIdle` | `PinyinInput.Waiting` / `Idle` | 确认候选字 | 选择候选后回到等待/空闲 |
| `Choosing` | `LoadCommitOptions` | `CommitOptionChoosing` | — | 进入提交选项 |
| `Choosing` | `PageCandidates` | `CandidateSelection.Choosing` | 播放翻页音效 | 翻页（`pageIndex` 更新） |
| `Choosing` | `LoadCandidates` | `CandidateSelection.Choosing` | — | 重新加载候选词 |
| `Choosing` | 其他 | **保持 `Choosing`** | — | Fail-safe |

`ReturnToIdle` 的目标状态取决于输入列表中是否还有待选的拼音输入：若有，回到 `PinyinInput.Waiting` 继续下一个拼音的选择；若无，回到 `Idle` 表示输入流程结束。`PageCandidates` 在 `Choosing` 状态下更新 `pageIndex`，轮播模式确保翻页操作永不越界。

### 6.6 从 CandidateSelection.Filtering 状态转换

| 当前状态 | 转换类型 | 新状态 | 副作用 | 说明 |
|---------|---------|-------|--------|------|
| `Filtering` | `BackToPrevious` | `CandidateSelection.Choosing` | 清除过滤条件 | 清除过滤条件回到候选选择 |
| `Filtering` | `FilterCandidates` | `CandidateSelection.Filtering` | 查询过滤结果 | 更新过滤条件 |
| `Filtering` | `PageCandidates` | `CandidateSelection.Filtering` | 播放翻页音效 | 翻页（`pageIndex` 更新） |
| `Filtering` | `ReturnToIdle` | `PinyinInput.Waiting` / `Idle` | 确认候选字 | 选择候选后回到等待/空闲 |
| `Filtering` | 其他 | **保持 `Filtering`** | — | Fail-safe |

`Filtering` 状态下的 `BackToPrevious` 会清除当前过滤条件，将完整的候选词列表恢复到 `Choosing` 状态。过滤条件下仍可选择候选词（通过 `ReturnToIdle`），此时确认的是过滤后的候选字。

### 6.7 从 CandidateSelection.AdvanceFiltering 状态转换

| 当前状态 | 转换类型 | 新状态 | 副作用 | 说明 |
|---------|---------|-------|--------|------|
| `AdvanceFiltering` | `BackToPrevious` | `CandidateSelection.Choosing` | 应用过滤结果 | 确认过滤条件回到候选选择 |
| `AdvanceFiltering` | `AdvanceFilterCandidates` | `CandidateSelection.AdvanceFiltering` | 查询高级过滤结果 | 更新高级过滤条件 |
| `AdvanceFiltering` | `PageCandidates` | `CandidateSelection.AdvanceFiltering` | 播放翻页音效 | 翻页 |
| `AdvanceFiltering` | 其他 | **保持 `AdvanceFiltering`** | — | Fail-safe |

`AdvanceFiltering` 是候选选择的最终细化步骤。用户可以同时按部首和声调进行过滤，每次修改过滤条件都会触发 `AdvanceFilterCandidates` 转换并重新计算 `filtered` 列表。确认过滤条件后通过 `BackToPrevious` 回到 `Choosing` 状态，此时 `Choosing` 的候选词列表已替换为高级过滤的结果。

### 6.8 从 CommitOptionChoosing 状态转换

| 当前状态 | 转换类型 | 新状态 | 副作用 | 说明 |
|---------|---------|-------|--------|------|
| `CommitOptionChoosing` | `ReturnToIdle` | `PinyinInput.Waiting` / `Idle` | 提交输入列表 | 选择完成后回到等待/空闲 |
| `CommitOptionChoosing` | `LoadCommitOptions` | `CommitOptionChoosing` | — | 更新提交选项 |
| `CommitOptionChoosing` | 其他 | **保持 `CommitOptionChoosing`** | — | Fail-safe |

`CommitOptionChoosing` 状态下，用户可以多次切换提交选项（拼写模式、繁简变体），每次切换通过更新 `InputList.inputOption` 实时反映到输入列表显示中。用户最终通过提交按键（`ReturnToIdle`）确认所有选项并提交输入，或通过退出按键恢复原始选项并回到前一键盘。

### 6.9 从 EditorEditing.CursorMoving 状态转换

| 当前状态 | 转换类型 | 新状态 | 副作用 | 说明 |
|---------|---------|-------|--------|------|
| `CursorMoving` | `MoveCursor` | `EditorEditing.CursorMoving` | 更新光标位置 | 更新光标位置 |
| `CursorMoving` | `SelectText` | `EditorEditing.TextSelecting` | — | 从光标移动切换到文本选择 |
| `CursorMoving` | `ReturnToIdle` | 前一状态（通过 `pop`） | — | 编辑结束回到前一状态 |
| `CursorMoving` | `BackToPrevious` | 前一状态（通过 `pop`） | — | 同上 |
| `CursorMoving` | 其他 | **保持 `CursorMoving`** | — | Fail-safe |

`CursorMoving` 到 `TextSelecting` 的切换由长按手势触发：用户在光标定位按键上长按后滑动，从光标移动模式切换到文本范围选择模式。`ReturnToIdle` 和 `BackToPrevious` 均通过 `stateHistory.pop()` 回到编辑前状态，通常是 `PinyinInput.Waiting` 或 `Idle`。

### 6.10 从 EditorEditing.TextSelecting 状态转换

| 当前状态 | 转换类型 | 新状态 | 副作用 | 说明 |
|---------|---------|-------|--------|------|
| `TextSelecting` | `SelectText` | `EditorEditing.TextSelecting` | 更新选择范围 | 更新选择范围 |
| `TextSelecting` | `ReturnToIdle` | 前一状态（通过 `pop`） | 执行选择操作 | 编辑结束回到前一状态 |
| `TextSelecting` | `BackToPrevious` | 前一状态（通过 `pop`） | 取消选择 | 同上 |
| `TextSelecting` | 其他 | **保持 `TextSelecting`** | — | Fail-safe |

`TextSelecting` 状态下，用户手指滑动时持续触发 `SelectText` 更新选择范围（`start` 和 `end`）。手指抬起后，选择范围确定，用户可以执行复制、剪切等编辑操作，或通过 `BackToPrevious` 取消选择回到编辑前状态。

### 6.11 从 SymbolChoosing 状态转换

| 当前状态 | 转换类型 | 新状态 | 副作用 | 说明 |
|---------|---------|-------|--------|------|
| `SymbolChoosing` | `OpenSymbolGroup` | `SymbolChoosing` | — | 切换分组（`groupId` 更新） |
| `SymbolChoosing` | `PageCandidates` | `SymbolChoosing` | 播放翻页音效 | 翻页 |
| `SymbolChoosing` | `ReturnToIdle` | `Idle` | 插入符号 | 退出符号选择 |
| `SymbolChoosing` | 其他 | **保持 `SymbolChoosing`** | — | Fail-safe |

`SymbolChoosing` 状态下，切换分组和翻页均在同一状态内更新数据，不产生状态层级变化。选择符号后，根据输入列表状态决定行为：输入列表非空时做替换或包裹，空时直接输出到编辑器。非连续输入（非长按）后自动退出符号选择键盘。

### 6.12 从 EmojiChoosing 状态转换

| 当前状态 | 转换类型 | 新状态 | 副作用 | 说明 |
|---------|---------|-------|--------|------|
| `EmojiChoosing` | `OpenEmojiGroup` | `EmojiChoosing` | — | 切换分组（`groupId` 更新） |
| `EmojiChoosing` | `PageCandidates` | `EmojiChoosing` | 播放翻页音效 | 翻页 |
| `EmojiChoosing` | `ReturnToIdle` | `Idle` | 插入 Emoji | 退出 Emoji 选择 |
| `EmojiChoosing` | 其他 | **保持 `EmojiChoosing`** | — | Fail-safe |

`EmojiChoosing` 的转换规则与 `SymbolChoosing` 对称。选择 Emoji 后，输入列表非空时做替换，空时直接输出到编辑器。长按选择时不退出键盘，允许连续输入多个 Emoji。默认分组为常用分组，若常用分组为空则自动切换到第二个分组。

---

## 7. KeyboardStateHistory 有界历史栈

`KeyboardStateHistory` 是 `KeyboardStateMachine` 内部的有界历史栈，用于实现同一键盘类型内的子状态回退。栈采用 `ArrayDeque<KeyboardState>` 实现，最大容量为 10 层。当栈满时，新压入的状态会自动淘汰栈底最旧的状态（FIFO 淘汰策略），确保历史栈的内存占用始终有上界。

```kotlin
class KeyboardStateHistory(private val maxSize: Int = 10) {
    private val stack = ArrayDeque<KeyboardState>(maxSize)

    fun push(state: KeyboardState) {
        if (stack.size >= maxSize) {
            stack.removeFirst()
        }
        stack.addLast(state)
    }

    fun pop(): KeyboardState? = stack.removeLastOrNull()

    fun clear() = stack.clear()

    val size: Int get() = stack.size
}
```

### 7.1 历史栈的生命周期管理

- **压入时机**：`KeyboardStateMachine.transition()` 中，当新状态与当前状态不同时，将当前状态压入历史栈
- **弹出时机**：`backToPrevious()` 方法弹出栈顶状态并恢复；若栈为空则回退到 `Idle`
- **清空时机**：`resetToIdle()` 和 `resetTo(state)` 方法清空历史栈；键盘类型切换时也清空历史栈

### 7.2 历史栈与撤销的职责划分

历史栈的职责是**同一键盘类型内的子状态回退**，例如 `EditorEditing.CursorMoving` → `PinyinInput.Waiting` 的回退。历史栈 **不负责** 输入列表的撤销操作——撤销通过 `InputListEditor` 的 `undoStack` / `redoStack` 双栈实现，保存的是 `InputList` 的完整快照，而非 `KeyboardState` 的快照。

| 操作 | 机制 | 说明 |
|------|------|------|
| 状态回退（`BackToPrevious`） | `KeyboardStateHistory.pop()` | 回到前一个键盘交互状态 |
| 撤销提交（`RevokeCommit`） | `InputListEditor.undoStack` | 恢复提交前的 `InputList` 快照 |
| 撤销清空（`RevokeClean`） | `InputListEditor.undoStack` | 恢复清空前的 `InputList` 快照 |

### 7.3 键盘切换时的历史栈行为

键盘类型切换时清空历史栈，因为不同键盘类型之间无回退关系。例如，从 `Pinyin` 键盘切换到 `Symbol` 键盘后，不应通过 `BackToPrevious` 回到拼音键盘的某个子状态——键盘切换通过 `ImeIntent.SwitchKeyboard` 显式控制，而非通过状态历史回退。只有同一键盘内的子状态转换（如 `Waiting` → `Slipping` → `Flipping`）才需要历史栈支持回退。

---

## 8. 三层映射模型

键盘状态转换遵循三层映射模型：

```
ImeIntent → KeyboardStateTransition → KeyboardState
```

这三层间接映射是键盘状态机架构的核心设计，每一层承担不同的职责，层与层之间通过明确的接口契约解耦。

### 8.1 第一层：ImeIntent → KeyboardStateTransition

由 `KeyboardIntentHandler` 子类的 `handleIntent()` 方法负责。各处理器根据自身业务逻辑，将 `ImeIntent` 映射为零或多个 `KeyboardStateTransition`。同一个 `ImeIntent` 在不同键盘类型下可以产生不同的 `KeyboardStateTransition`——例如 `PressKey(CharKey.Alphabet)` 在拼音键盘下可能映射为 `InputPinyinChar`，而在数字键盘下则不产生任何转换。这种差异封装在各自的 `KeyboardIntentHandler` 实现中，`KeyboardStateMachine` 不需要感知意图的来源和语义。

### 8.2 第二层：KeyboardStateTransition → KeyboardState

由 `KeyboardStateMachine.transition()` 集中处理。根据当前状态和转换类型，确定新状态和副作用列表。所有合法转换规则集中在状态机内部，形成一张完整的有限状态转换表。任何不在转换表中的组合均为 Fail-safe（保持原状态），确保状态机行为可预测。这一层是纯函数式的——输入为当前状态和转换类型，输出为新状态和副作用列表，不产生任何可观察的副作用。

### 8.3 第三层：副作用执行

由 `ImeEngine` 异步处理副作用列表中的 `ImeIntent`，如字典查询、音频播放、编辑器桥接等。`KeyboardStateMachine.transition()` 返回的 `Result.sideEffects` 由 `ImeEngine` 逐一处理——每个副作用 `ImeIntent` 再次进入 `handleIntent()` 流程，可能触发新的状态转换或异步操作。这种递归处理机制使得状态机可以在一次转换中表达需要链式执行的副作用序列，同时保持状态转换本身的纯函数性质。

### 8.4 三层映射的优势

| 优势 | 说明 |
|------|------|
| **关注点分离** | 意图语义、转换规则、副作用执行三者独立演化，修改任一层不影响其他层 |
| **可测试性** | 第二层可脱离运行时环境进行纯函数测试，只需验证 `(state, transition) → result` |
| **可扩展性** | 新增键盘类型只需实现新的 `KeyboardIntentHandler`，无需修改状态机或引擎 |
| **Fail-safe 保障** | 非法转换在第二层被安全拦截，不会传播到副作用执行层 |
| **副作用延迟执行** | 副作用在状态计算完成后统一处理，避免状态计算过程中的时序竞争 |

```plantuml
@file:../diagrams/engine-state-machine.puml
```
