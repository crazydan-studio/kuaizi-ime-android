# 键盘状态机

```plantuml
@file:../diagrams/engine-state-machine.puml
```

## 1. 概述

键盘状态机是筷字输入法的核心逻辑，管理键盘在不同输入模式下的状态转换。v4 版本采用 **sealed class 层级**表达有限状态集，通过 **KeyboardStateTransition** 描述触发转换的原子事件，由 **KeyboardStateMachine** 集中执行转换规则并产生副作用。各 KeyboardIntentHandler 子类（PinyinKeyboardIntentHandler、NumberKeyboardIntentHandler 等）按不同 KeyboardType 创建，各自处理从 ImeIntent 到 KeyboardStateTransition 的转换，状态由 KeyboardStateMachine 集中管理。

### 1.1 设计目标

| 目标 | 说明 |
|------|------|
| **编译期合法性** | 通过 sealed class 确保所有状态和转换类型在编译期穷举检查，非法状态转换不可构造 |
| **集中化规则** | 所有转换规则集中在 KeyboardStateMachine.transition() 中，而非分散在各键盘子类的方法中 |
| **类型安全** | 状态数据内嵌于 sealed class 子类字段，替代 Java 版本的 Type + Data + previous 组合模式 |
| **有界历史** | 使用 KeyboardStateHistory 有界栈替代无限 previous 链表，防止内存泄漏 |
| **关注点分离** | 状态转换逻辑与按键生成逻辑、输入列表操作逻辑分离为独立组件 |

### 1.2 核心设计决策

1. **Sealed class 替代枚举 + 数据类组合**：Java 版本使用 `State.Type` 枚举 + `State.Data` 接口 + `State.previous` 链表组合表达状态，缺乏编译期约束，任意 Type 可与任意 Data 组合。v4 将每种状态定义为一个 data class，状态数据作为子类字段，编译期保证类型正确。

2. **KeyboardStateMachine 提取为独立组件**：Java 版本的状态管理逻辑分散在 `BaseKeyboard.change_State_To()`、`change_State_to_Init()`、`change_State_to_Previous()` 等方法中。v4 将状态转换逻辑提取为独立的 `KeyboardStateMachine` 组件，集中管理转换规则和副作用。

3. **三层映射模型**：`ImeIntent → KeyboardStateTransition → KeyboardState`。KeyboardIntentHandler 子类负责将 ImeIntent 映射为 KeyboardStateTransition，KeyboardStateMachine 执行转换产生新 KeyboardState 和副作用列表。这种分层使得 Intent 的语义与状态转换规则解耦。

4. **KeyboardStateHistory 有界栈**：Java 版本使用 `State.previous` 链表实现无限深度的状态回退，可能导致内存泄漏。v4 使用 `ArrayDeque<KeyboardState>(maxSize = 10)` 有界栈，并在键盘类型切换时清空历史。

---

## 2. KeyboardState 定义

```kotlin
sealed class KeyboardState {
    // 空闲状态
    data object Idle : KeyboardState()

    // 拼音输入状态
    sealed class PinyinInput : KeyboardState() {
        data class Waiting(val pending: InputItem.Char?) : PinyinInput()
        data class Slipping(
            val startKey: InputKey,
            val level0Key: InputKey,
            val level1Key: InputKey?,
            val level2Key: InputKey?,
            val nextCharsByLength: Map<Int, List<String>>,
        ) : PinyinInput()
        data class Flipping(
            val startChar: String,
            val candidates: List<String>,
        ) : PinyinInput()
        data class XPadding(
            val zones: List<XPadZone>,
            val currentSpell: String,
        ) : PinyinInput()
    }

    // 候选选择状态
    sealed class CandidateSelection : KeyboardState() {
        data class Choosing(
            val candidates: List<InputWord>,
            val pageIndex: Int,
            val pageSize: Int,
        ) : CandidateSelection()
        data class Filtering(
            val filter: PinyinWordFilter,
            val filtered: List<InputWord>,
        ) : CandidateSelection()
        data class AdvanceFiltering(
            val radical: PinyinWord.Radical?,
            val tone: PinyinWord.Tone?,
            val filtered: List<InputWord>,
        ) : CandidateSelection()
    }

    // 提交选项状态
    data class CommitOptionChoosing(
        val options: List<CommitOption>,
        val hasSpell: Boolean,
        val hasVariant: Boolean,
    ) : KeyboardState()

    // 编辑器状态
    sealed class EditorEditing : KeyboardState() {
        data class CursorMoving(val position: Int) : EditorEditing()
        data class TextSelecting(val start: Int, val end: Int) : EditorEditing()
    }

    // 符号/Emoji 选择状态
    data class SymbolChoosing(val groupId: String?) : KeyboardState()
    data class EmojiChoosing(val groupId: String?) : KeyboardState()
}
```

### 2.1 状态层次一览

| 顶层状态 | 子状态 | 说明 | 对应 Java State.Type |
|----------|--------|------|---------------------|
| `Idle` | — | 空闲，数字/数学/编辑键盘的默认状态 | `InputChars_Input_Wait_Doing` |
| `PinyinInput` | `Waiting` | 等待输入，pending 为未确认的拼音字符 | `InputChars_Input_Wait_Doing` |
| | `Slipping` | 滑行输入中，三级按键层次结构 | `InputChars_Slip_Doing` |
| | `Flipping` | 翻动输入中，以起始字符展示全部候选 | `InputChars_Flip_Doing` |
| | `XPadding` | X-Pad 输入中，六边形区域连续输入 | `InputChars_XPad_Input_Doing` |
| `CandidateSelection` | `Choosing` | 候选词选择，含分页数据 | `InputCandidate_Choose_Doing` |
| | `Filtering` | 拼音过滤，应用声调/拼写筛选 | `InputCandidate_Choose_Doing`（同状态，filter 非空） |
| | `AdvanceFiltering` | 高级过滤（部首/声调） | `InputCandidate_Advance_Filter_Doing` |
| `CommitOptionChoosing` | — | 提交选项选择 | `InputList_Commit_Option_Choose_Doing` |
| `EditorEditing` | `CursorMoving` | 光标移动中 | `Editor_Edit_Doing`（target=cursor） |
| | `TextSelecting` | 文本范围选择中 | `Editor_Edit_Doing`（target=selection） |
| `SymbolChoosing` | — | 符号选择，含分组信息 | `InputCandidate_Choose_Doing`（SymbolKeyboard 的状态） |
| `EmojiChoosing` | — | Emoji 选择，含分组信息 | `InputCandidate_Choose_Doing`（EmojiKeyboard 的状态） |

### 2.2 Slipping 状态数据说明

滑行输入（Slip）是拼音键盘的核心交互方式，用户手指从起始按键滑出，经过中间级按键，最终到达目标按键完成输入。滑行输入采用三级按键层次结构：

- **level0Key**：滑行起始按键，手指按下时确定的按键
- **level1Key**：滑行第一级目标按键，手指从起始按键滑出后的第一个中间按键
- **level2Key**：滑行第二级目标按键，手指继续滑行到达的最终按键

`nextCharsByLength` 记录在当前滑行路径下，按字符长度分组的可选后续字符列表。此映射用于 UI 层预显示可能的后续输入选项。

对应 Java 版本中的 `InputCharsSlipStateData`，其字段 `level0Key`、`level1Key`、`level2Key`、`level2NextChars` 在 v4 中直接内嵌为 Slipping 子类的字段。

### 2.3 Flipping 状态数据说明

翻动输入（Flip）是在首字母按键上快速滑出触发的输入方式，`startChar` 为起始字符，`candidates` 为翻动产生的候选字符列表。翻动输入是一种快捷输入方式，允许用户通过快速滑动选择同一起始字符下的不同变体。

对应 Java 版本中的 `InputCharsFlipStateData`，其字段 `startChar` 在 v4 中直接内嵌。

### 2.4 CandidateSelection 状态数据说明

候选选择有三个子状态，形成 **选择 → 过滤 → 高级过滤** 的递进关系：

**Choosing**：基础候选选择状态，`candidates` 为当前候选词列表，`pageIndex` 和 `pageSize` 控制分页显示。分页采用轮播模式：翻到末页后继续翻页回到首页，翻到首页前继续翻页跳到末页。

**Filtering**：在基础选择上应用拼音过滤器，`filter` 包含声调和拼写过滤条件，`filtered` 为过滤后的候选词列表。

**AdvanceFiltering**：在基础选择上应用部首和声调的高级过滤，`radical` 为选中的部首，`tone` 为选中的声调，`filtered` 为过滤后的候选词列表。

### 2.5 CommitOptionChoosing 状态数据说明

提交选项选择是输入列表中针对已确认输入的后续操作选择。`options` 为可用的提交选项列表，`hasSpell` 表示当前输入列表中是否有包含拼音拼写的候选字，`hasVariant` 表示是否有包含变体（繁体/异体）的候选字。这两个布尔值控制提交选项面板中「切换拼写模式」和「切换变体」按钮的显示与否。

对应 Java 版本中的 `InputListCommitOptionChooseStateData`，其字段 `option`、`hasSpell`、`hasVariant` 在 v4 中直接内嵌。

### 2.6 EditorEditing 状态数据说明

编辑器编辑包含两种子状态：`CursorMoving` 控制光标在输入列表中的位置移动，`TextSelecting` 控制文本范围选择。两种编辑模式通过编辑手势触发切换：短按进入光标移动，长按后滑动进入范围选择。

对应 Java 版本中的 `EditorEditStateData`，其 `target` 字段（cursor/selection）在 v4 中拆分为两个独立子状态。

---

## 3. KeyboardStateTransition 定义

```kotlin
sealed class KeyboardStateTransition {
    // 拼音输入转换
    data class InputPinyinChar(val char: Char) : KeyboardStateTransition()
    data class BeginSlip(val startKey: InputKey) : KeyboardStateTransition()
    data class BeginFlip(val startChar: Char) : KeyboardStateTransition()
    data class BeginXPad(val zones: List<XPadZone>) : KeyboardStateTransition()
    data class SelectSlipChar(val char: Char) : KeyboardStateTransition()
    data class SelectFlipChar(val char: Char) : KeyboardStateTransition()
    data class SelectXPadZone(val zone: XPadZone) : KeyboardStateTransition()

    // 候选选择转换
    data class LoadCandidates(val candidates: List<InputWord>) : KeyboardStateTransition()
    data class FilterCandidates(val filter: PinyinWordFilter) : KeyboardStateTransition()
    data class AdvanceFilterCandidates(val radical: PinyinWord.Radical?, val tone: PinyinWord.Tone?) : KeyboardStateTransition()
    data class PageCandidates(val direction: PageDirection) : KeyboardStateTransition()

    // 提交选项转换
    data class LoadCommitOptions(val options: List<CommitOption>) : KeyboardStateTransition()

    // 编辑器转换
    data class MoveCursor(val position: Int) : KeyboardStateTransition()
    data class SelectText(val start: Int, val end: Int) : KeyboardStateTransition()

    // 符号/Emoji 转换
    data class OpenSymbolGroup(val groupId: String?) : KeyboardStateTransition()
    data class OpenEmojiGroup(val groupId: String?) : KeyboardStateTransition()

    // 通用转换
    data object ReturnToIdle : KeyboardStateTransition()
    data object BackToPrevious : KeyboardStateTransition()
}
```

### 3.1 转换类型一览

| 类别 | 转换类型 | 说明 | 触发场景 |
|------|---------|------|---------|
| 拼音输入 | `InputPinyinChar` | 输入拼音字符 | 单击字母按键 |
| | `BeginSlip` | 开始滑行输入 | 手指在字母按键上开始移动 |
| | `BeginFlip` | 开始翻动输入 | 在首字母按键上快速滑出 |
| | `BeginXPad` | 开始 X-Pad 输入 | 在 X-Pad 模式下按下字母按键 |
| | `SelectSlipChar` | 选择滑行字符 | 滑行输入中手指到达目标按键 |
| | `SelectFlipChar` | 选择翻动字符 | 翻动输入中单击目标按键 |
| | `SelectXPadZone` | 选择 X-Pad 区域 | X-Pad 输入中单击目标区域 |
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

---

## 4. KeyboardStateMachine

`KeyboardStateMachine` 是状态转换的集中处理器，接收 `KeyboardStateTransition`，根据当前状态执行转换规则，返回新的 `KeyboardState` 和副作用列表。

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
            is KeyboardState.PinyinInput.XPadding -> handleFromPinyinXPadding(transition)
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

### 4.1 KeyboardStateTransition.Result

```kotlin
sealed class KeyboardStateTransition {
    // ... 转换类型定义见 §3 ...

    /**
     * 状态转换结果。
     *
     * 包含新的键盘状态和需要异步处理的副作用意图列表。
     */
    data class Result(
        val newState: KeyboardState,
        val sideEffects: List<ImeIntent> = emptyList(),
    )
}
```

`sideEffects` 是状态转换产生的副作用意图列表，包含需要异步处理的操作（如字典查询、音频播放、输出桥接等）。KeyboardStateMachine 本身是纯函数，不直接执行副作用，而是通过返回副作用列表交由 ImeEngine 异步处理。`Result` 作为 `KeyboardStateTransition` 的嵌套类型，表达转换与结果之间的归属关系。

---

## 5. KeyboardType 与初始状态

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

### 5.1 键盘类型分类

| 分类 | KeyboardType | 说明 |
|------|-------------|------|
| 主键盘 | `Pinyin`、`Latin`、`Number` | 常驻性键盘，其他键盘退出后回到主键盘 |
| 临时键盘 | `Math`、`Symbol`、`Emoji`、`Editor` | 从主键盘切换进入，退出时回到切换前的主键盘 |
| 超临时键盘 | `Candidate`、`CommitOption` | 选择操作完成后立即返回，生命周期极短 |

### 5.2 KeyboardType → 初始 KeyboardState 映射

| KeyboardType | 初始 KeyboardState | 说明 |
|-------------|-------------------|------|
| Pinyin | `PinyinInput.Waiting(null)` | 拼音键盘等待输入 |
| Latin | `PinyinInput.Waiting(null)` | 拉丁键盘复用拼音的输入模式 |
| Number | `Idle` | 数字键盘无子状态 |
| Math | `Idle` | 数学键盘无子状态（但使用嵌套 InputList） |
| Symbol | `SymbolChoosing(null)` | 符号键盘默认进入符号选择 |
| Emoji | `EmojiChoosing(null)` | Emoji 键盘默认进入 Emoji 选择 |
| Editor | `Idle` | 编辑键盘无子状态 |
| Candidate | `CandidateSelection.Choosing(emptyList(), 0, 0)` | 候选键盘直接进入候选选择 |
| CommitOption | `CommitOptionChoosing(emptyList(), false, false)` | 提交选项键盘直接进入选项选择 |

---

## 6. KeyboardIntentHandler + Keyboard data class

### 6.1 KeyboardIntentHandler 接口

```kotlin
/**
 * 键盘意图处理器，按不同 KeyboardType 创建子类，
 * 各自处理从 ImeIntent 到 KeyboardStateTransition 的转换。
 *
 * 替代原 sealed class Keyboard 的意图处理职能。
 * 各子类是无状态的策略对象，状态由 KeyboardStateMachine 集中管理。
 */
interface KeyboardIntentHandler {
    /** 该处理器对应的键盘类型 */
    val type: KeyboardType

    /**
     * 处理用户意图，将其转换为 KeyboardStateTransition 并委托状态机执行。
     *
     * @param intent 用户意图
     * @param state 当前键盘状态
     * @return 处理结果，包含新状态和副作用意图列表
     */
    fun handleIntent(intent: ImeIntent, state: KeyboardState): KeyboardStateTransition.Result
}
```

各 KeyboardIntentHandler 子类是无状态的策略对象，根据不同 KeyboardType 实现意图转换逻辑。它们通过组合模式持有 `KeyboardStateMachine`、字典引用、配置引用等独立组件，而非通过继承共享行为。各子类的 `handleIntent()` 方法负责将 `ImeIntent` 映射为 `KeyboardStateTransition`，再由 `KeyboardStateMachine` 执行转换。

### 6.2 Keyboard data class

```kotlin
/**
 * 键盘实例，绑定键盘类型、输入模式和键盘状态。
 *
 * 替代原 sealed class Keyboard 的数据绑定职能。
 * 不可变 data class，通过 copy() 生成新实例。
 */
data class Keyboard(
    /** 键盘类型，决定按键集合的语义内容 */
    val type: KeyboardType = KeyboardType.Pinyin,
    /** 输入模式，决定按键的几何排列和交互方式 */
    val mode: KeyboardInputMode = KeyboardInputMode.RectGrid,
    /** 键盘状态机当前状态 */
    val state: KeyboardState = KeyboardState.Idle,
)
```

### 6.3 KeyboardIntentHandler 子类层次

Java 版本采用深层继承（`BaseKeyboard → EditorEditKeyboard → PinyinKeyboard`），v4 采用组合模式替代继承，但保留键盘类型的层次结构用于组织逻辑。KeyboardIntentHandler 子类层次如下：

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

注意：`PinyinKeyboardIntentHandler` 同时处理 `KeyboardType.Pinyin` 和 `KeyboardType.Latin`，因为拉丁键盘复用拼音的滑行/X-Pad 模式。

### 6.4 各 KeyboardIntentHandler 子类的职责和 intent 处理逻辑

#### 6.4.1 PinyinKeyboardIntentHandler（含 LatinKeyboard）

**职责**：拼音输入的核心意图处理器，支持四种输入模式（点击、滑行、翻动、X-Pad），管理拼音字符输入、候选字查询和输入补全。同时处理 `KeyboardType.Latin` 的意图转换，因为拉丁键盘复用拼音的滑行/X-Pad 模式。

**处理的 ImeIntent**：

| ImeIntent | 处理流程 | 产生的 KeyboardStateTransition |
|-----------|---------|------------------------------|
| `PressKey(CharKey.Alphabet)` | 单击字母按键，追加到 pending 或新建 pending，查询候选字 | `InputPinyinChar` |
| `PressKey(CharKey.Alphabet)` + `FingerMoving` | 开始滑行输入，进入 Slipping 状态 | `BeginSlip` |
| `FingerFlipping`（Slipping 中） | 在滑行中触发翻动，进入 Flipping 状态 | `BeginFlip` |
| `FingerMovingStop`（Slipping 中） | 结束滑行输入，确认 pending，回到 Waiting | `SelectSlipChar` + `ReturnToIdle` |
| `PressKey(CharKey.Alphabet)`（XPad 模式） | 开始 X-Pad 输入，进入 XPadding 状态 | `BeginXPad` |
| `PressKey(CharKey.Symbol/Emoji)` | 符号/表情直输或替换输入 | `InputPinyinChar` |
| `PressKey(CtrlKey.Backspace)` | 回删输入列表或编辑器 | 不产生 Transition，直接操作 InputList |
| `PressKey(CtrlKey.Space/Enter)` | 确认 pending + 录入空格/换行 | `ReturnToIdle` |
| `PressKey(CtrlKey.Commit)` | 提交输入列表并回到初始状态 | `ReturnToIdle` |
| `LongPressKey(CtrlKey.Commit)` | 进入提交选项键盘 | 切换到 CommitOption 键盘 |

**内部状态转换流程**：

1. **点击输入**：`Waiting → InputPinyinChar → Waiting`（单字符追加到 pending）
2. **滑行输入**：`Waiting → BeginSlip → Slipping → SelectSlipChar → Waiting`（多字符连续输入）
3. **翻动输入**：`Slipping → BeginFlip → Flipping → SelectFlipChar → Waiting`（快速展示首字母全部候选）
4. **X-Pad 输入**：`Waiting → BeginXPad → XPadding → SelectXPadZone → Waiting`（X-Pad 面板连续输入）
5. **候选选择**：`Waiting → LoadCandidates → CandidateSelection.Choosing`（选中拼音输入后自动进入）

**滑行输入的详细流程**（对应 Java 版本 `PinyinKeyboard.start_InputChars_Slipping()`）：

1. 手指在字母按键上开始移动（`FingerMoving_Start`）
2. 创建新的 `InputCharsSlipStateData`，设置 `level0Key` 为起始按键
3. 将按键追加到 pending 的 CharInput 中
4. 查询拼音字典确定候选字（`determine_NotConfirmed_InputWord`）
5. 触发 `InputChars_Input_Doing` 消息（slip 模式）
6. 手指继续移动时（`FingerMoving`），根据按键级别替换 pending 中的按键
7. 手指停止移动时（`FingerMoving_Stop`），结束滑行输入
8. 若拼音有效则确认 pending，否则丢弃

#### 6.4.2 Latin 键盘的意图处理（由 PinyinKeyboardIntentHandler 统一处理）

**职责**：拉丁字母键盘的意图处理，支持直接输入模式（点击即提交到编辑器）和输入列表模式（输入列表非空时在列表中操作）。复用拼音的输入补全机制。

**处理的 ImeIntent**：

| ImeIntent | 处理流程 | 产生的 KeyboardStateTransition |
|-----------|---------|------------------------------|
| `PressKey(CharKey.Alphabet/Number)` | 单字符输入，输入列表为空时直输，否则追加到 pending | `InputPinyinChar` |
| `LongPressKey(CharKey.Alphabet/Number)` | 连续输入（长按 tick 视为连续单击） | 同 `PressKey` |

**与 Pinyin 键盘意图处理的差异**：Latin 模式不支持滑行输入、翻动输入和 X-Pad 输入，仅支持逐键点击。输入列表为空时，按键直接提交到编辑器（直输模式），不在输入列表中停留和预处理。

#### 6.4.3 NumberKeyboardIntentHandler

**职责**：纯数字键盘的意图处理，支持数字和部分符号（+、-、#、*）的直接输入。与 Latin 键盘类似，支持直输和输入列表两种模式。

**处理的 ImeIntent**：

| ImeIntent | 处理流程 | 产生的 KeyboardStateTransition |
|-----------|---------|------------------------------|
| `PressKey(CharKey.Number)` | 单字符输入 | `InputPinyinChar` |
| `PressKey(CtrlKey.Commit)` | 提交输入列表 | `ReturnToIdle` |
| `LongPressKey(CtrlKey.Commit)` | 禁用（避免切换到提交选项键盘） | 不产生 Transition |
| `PressKey(CtrlKey.Exit)` | 退出键盘（X-Pad 模式下提供退出按钮） | 切换到前一键盘 |

**特殊行为**：
- 在 X-Pad 输入中切换过来时，显示退出按钮以回到原键盘
- 长按提交按键被禁用（`disable_Msg_On_CtrlKey_Commit_InputList`），避免意外进入提交选项键盘

#### 6.4.4 MathKeyboardIntentHandler

**职责**：数学表达式键盘，管理嵌套的数学 InputList，支持数字、运算符和括号的输入，自动计算表达式结果。

**处理的 ImeIntent**：

| ImeIntent | 处理流程 | 产生的 KeyboardStateTransition |
|-----------|---------|------------------------------|
| `PressKey(CharKey.Number)` | 数字追加到数学输入列表的 pending | `InputPinyinChar` |
| `PressKey(MathOpKey)` | 运算符输入（点号、括号、等号等） | `InputPinyinChar` |
| `PressKey(CtrlKey.Backspace)` | 回删数学输入列表或上层输入列表 | 不产生 Transition，直接操作 InputList |
| `PressKey(CtrlKey.Commit)` | 提交父输入列表，退出键盘 | `ReturnToIdle` |
| `PressKey(CtrlKey.Space)` | 确认当前算术输入 + 录入空格 | `ReturnToIdle` |
| `Input_Choose_Doing` | 选中数学输入列表中的输入 | 不产生 Transition，直接操作数学 InputList |

**嵌套 InputList 机制**：MathKeyboard 维护两层 InputList——父输入列表（上层）和数学输入列表（嵌套在 `MathExprInput` 中）。所有数字和运算符输入在数学输入列表中操作，提交时将整个数学表达式作为 `InputItem.MathExpr` 提交到父输入列表。

**启动与停止流程**（对应 Java 版本 `MathKeyboard.start_Math_Inputting()` / `stop_Math_Inputting()`）：

1. 启动时，先提交从其他键盘切过来之前的 pending
2. 在父输入列表中创建或选中 `MathExprInput`，获取其嵌套的数学 InputList
3. 停止时，确保当前的算术输入列表已被确认

#### 6.4.5 SymbolKeyboardIntentHandler

**职责**：符号选择键盘，提供分组浏览和翻页选择符号的能力。支持配对符号输入（如括号、引号）。

**处理的 ImeIntent**：

| ImeIntent | 处理流程 | 产生的 KeyboardStateTransition |
|-----------|---------|------------------------------|
| `PressKey(SymbolKey)` | 选择符号，配对符号做包裹，非配对符号做替换/直输 | `ReturnToIdle`（非连续输入后退出） |
| `LongPressKey(SymbolKey)` | 连续输入符号 | 不退出键盘 |
| `PressKey(CtrlKey.Toggle_Symbol_Group)` | 切换符号分组 | `OpenSymbolGroup` |
| `FingerFlipping` | 翻页符号 | `PageCandidates` |

**配对符号处理流程**（对应 Java 版本 `SymbolKeyboard.prepare_for_PairKey_Inputting()`）：

1. 若选中的输入已有配对符号，替换左右符号
2. 若 pending 非空且非符号输入，用配对符号包裹 pending（左符号 + Gap + 被包裹输入 + 右符号）
3. 否则，左右符号依次输入，光标放在中间位置

**分组逻辑**：默认根据前一键盘类型选择分组——从拼音键盘切换过来默认显示中文符号（han 分组），否则显示拉丁符号（latin 分组）。

#### 6.4.6 EmojiKeyboardIntentHandler

**职责**：Emoji 选择键盘，提供分组浏览和翻页选择 Emoji 的能力。

**处理的 ImeIntent**：

| ImeIntent | 处理流程 | 产生的 KeyboardStateTransition |
|-----------|---------|------------------------------|
| `PressKey(InputWordKey)` | 选择 Emoji，输入列表非空时做替换，空时直输 | `ReturnToIdle` |
| `LongPressKey(InputWordKey)` | 连续输入 Emoji | 不退出键盘 |
| `PressKey(CtrlKey.Toggle_Emoji_Group)` | 切换 Emoji 分组 | `OpenEmojiGroup` |
| `FingerFlipping` | 翻页 Emoji | `PageCandidates` |

**常用分组**：默认显示常用分组，若常用分组为空则自动切换到第二个分组。常用分组的数据从用户输入数据字典中查询。

#### 6.4.7 EditorKeyboardIntentHandler

**职责**：文本编辑功能键盘，提供光标移动、文本选择、复制、粘贴等编辑操作。

**处理的 ImeIntent**：

| ImeIntent | 处理流程 | 产生的 KeyboardStateTransition |
|-----------|---------|------------------------------|
| `PressKey(CtrlKey.Edit_Editor)` | 执行编辑操作（复制、粘贴等） | 不产生 Transition，通过 ImeOutput 输出 |
| `LongPressKey(CtrlKey.Editor_Cursor_Locator)` | 进入范围选择编辑 | `SelectText` |
| `FingerMoving_Start`（光标定位按键） | 进入光标移动编辑 | `MoveCursor` |
| `DoubleTapKey`（光标定位按键） | 切换到 Editor 键盘 | 切换到 Editor 键盘 |

**特殊行为**：
- 长按提交按键被禁用，避免意外进入提交选项键盘
- 光标定位按键的处理由 EditorEditKeyboard 的基类逻辑接管，EditorKeyboard 屏蔽基类的处理

#### 6.4.8 CandidateKeyboardIntentHandler

**职责**：拼音候选字选择键盘，提供候选词浏览、拼音过滤、高级过滤（部首/声调）和候选字确认。是使用频率最高的超临时键盘。

**处理的 ImeIntent**：

| ImeIntent | 处理流程 | 产生的 KeyboardStateTransition |
|-----------|---------|------------------------------|
| `PressKey(InputWordKey)` | 选择候选词，确认 pending 中的候选字 | `ReturnToIdle`（无下一个拼音）/ 继续选择 |
| `PressKey(CtrlKey.ConfirmInput)` | 确认当前候选字 | `ReturnToIdle` |
| `PressKey(CtrlKey.Toggle_Pinyin_Spell)` | 切换拼音修正模式（ng 结尾 / nl 开头 / zcs 开头） | `FilterCandidates` |
| `PressKey(CtrlKey.Filter_PinyinCandidate_by_Spell)` | 按拼写过滤候选词 | `FilterCandidates` |
| `PressKey(CtrlKey.Filter_PinyinCandidate_advance)` | 进入高级过滤 | `AdvanceFilterCandidates` |
| `PressKey(CtrlKey.Confirm_PinyinCandidate_Filter)` | 确认高级过滤条件，回到候选选择 | `BackToPrevious` |
| `FingerFlipping` | 翻页候选词 | `PageCandidates` |

**候选字确认流程**（对应 Java 版本 `PinyinCandidateKeyboard.confirm_InputList_Pending_InputCandidate()`）：

1. 确认 pending 中的候选字（`pending.confirmWord()`）
2. 确认 pending 并选中下一个输入
3. 查找下一个拼音输入，若有则继续选择候选字，若无则退出键盘
4. 自动进行短语预测和输入补全

**高级过滤流程**（对应 Java 版本 `PinyinCandidateKeyboard.start_InputCandidate_Advance_Filtering()`）：

1. 从候选选择状态进入高级过滤状态，继承当前的过滤条件
2. 按部首或拼写进行过滤
3. 确认过滤条件后，将过滤结果回传给候选选择状态，退出高级过滤

#### 6.4.9 CommitOptionKeyboardIntentHandler

**职责**：输入列表提交选项键盘，控制提交到目标编辑器的内容形式（拼音拼写模式、繁简变体）。

**处理的 ImeIntent**：

| ImeIntent | 处理流程 | 产生的 KeyboardStateTransition |
|-----------|---------|------------------------------|
| `PressKey(CtrlKey.Commit_InputList_Option)` | 切换提交选项（拼写模式/繁简变体） | 不产生 Transition，更新 InputList.inputOption |
| `PressKey(CtrlKey.Commit)` | 提交输入列表并退出 | `ReturnToIdle` |
| `PressKey(CtrlKey.Exit)` | 退出回到切换前的键盘 | 切换到前一键盘 |

**提交选项更新流程**（对应 Java 版本 `InputListCommitOptionKeyboard.update_InputList_Commit_Option()`）：

1. 根据按键的 `InputWordCommitMode` 确定要修改的选项
2. 若有变更，更新 InputList 的 `inputOption`（`spellUsedMode` 或 `variantUsed`）
3. 重新计算 `hasSpell` 和 `hasVariant`

**退出逻辑**：退出时恢复输入列表的 InputOption 至切换前的状态（若为 Exit 操作）。

### 6.5 KeyboardIntentHandler 注册表机制

ImeEngine 维护一个 `Map<KeyboardType, KeyboardIntentHandler>` 注册表，根据当前 `Keyboard.type` 选择对应的 KeyboardIntentHandler 实现：

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
                // 异步处理副作用
                result.sideEffects.forEach { sideEffect ->
                    handleIntent(sideEffect)
                }
            }
        }
    }
}
```

### 6.6 键盘类型之间的切换逻辑

键盘切换遵循**主键盘 → 临时键盘 → 超临时键盘**的层级关系，回退方向为反向。

#### 6.6.1 切换场景

| 触发场景 | 切换方向 | 说明 |
|---------|---------|------|
| 点击键盘切换按键 | 主键盘 → 主键盘 | Pinyin ↔ Latin ↔ Number |
| 输入拼音后选中 | 主键盘 → 超临时键盘 | Pinyin → Candidate |
| 长按提交按键 | 主/临时键盘 → 超临时键盘 | → CommitOption |
| 选中数学输入 | 主键盘 → 临时键盘 | → Math |
| 选中符号/表情输入 | 主键盘 → 临时键盘 | → Symbol/Emoji |
| 双击光标定位按键 | 主键盘 → 临时键盘 | → Editor |

#### 6.6.2 回退逻辑

| 当前键盘 | 退出行为 | 说明 |
|---------|---------|------|
| 超临时键盘（Candidate/CommitOption） | 选择完成后自动回到切换前键盘 | 生命周期极短 |
| 临时键盘（Math/Symbol/Emoji/Editor） | 退出时回到切换前的主键盘 | `switch_Keyboard_to_Previous` |
| 主键盘 | 无回退 | 常驻性键盘 |

#### 6.6.3 状态历史与键盘切换的交互

- **键盘类型切换时清空历史栈**：不同键盘类型之间无回退关系，切换时清空 KeyboardStateHistory
- **同一键盘内的子状态回退**：通过 `stateHistory.pop()` 实现，如 EditorEditing → Waiting
- **撤销（Revoke）不依赖状态历史**：撤销通过 InputListEditor 的 undoStack/redoStack 实现

---

## 7. KeyboardStateTransition 处理流程

### 7.1 三层映射模型

键盘状态转换遵循三层映射模型：

```
ImeIntent → KeyboardStateTransition → KeyboardState
```

- **第一层（ImeIntent → KeyboardStateTransition）**：由 KeyboardIntentHandler 子类的 `handleIntent()` 方法负责。各处理器根据自身业务逻辑，将 ImeIntent 映射为零或多个 KeyboardStateTransition。例如 PinyinKeyboardIntentHandler 将 `PressKey(CharKey.Alphabet)` + `FingerMoving` 映射为 `BeginSlip`，而 Latin 模式下对同一 Intent 不产生任何 Transition。

- **第二层（KeyboardStateTransition → KeyboardState）**：由 `KeyboardStateMachine.transition()` 集中处理。根据当前状态和转换类型，确定新状态和副作用列表。

- **第三层（副作用执行）**：由 ImeEngine 异步处理副作用列表中的 ImeIntent，如字典查询、音频播放、输出桥接等。

### 7.2 状态转换规则表

以下表格列出每种 KeyboardStateTransition 在各当前状态下的合法转换规则。

#### 7.2.1 从 Idle 状态转换

| 当前状态 | 转换类型 | 新状态 | 说明 |
|---------|---------|-------|------|
| Idle | InputPinyinChar | PinyinInput.Waiting | 键盘切换到拼音/拉丁时进入 |
| Idle | OpenSymbolGroup | SymbolChoosing | 键盘切换到符号时进入 |
| Idle | OpenEmojiGroup | EmojiChoosing | 键盘切换到 Emoji 时进入 |
| Idle | MoveCursor | EditorEditing.CursorMoving | 编辑器操作触发 |
| Idle | LoadCandidates | CandidateSelection.Choosing | 键盘切换到候选时进入 |
| Idle | LoadCommitOptions | CommitOptionChoosing | 键盘切换到提交选项时进入 |
| Idle | 其他 | **保持 Idle**（不合法） | Fail-safe：保持原状态 |

#### 7.2.2 从 PinyinInput.Waiting 状态转换

| 当前状态 | 转换类型 | 新状态 | 说明 |
|---------|---------|-------|------|
| Waiting | BeginSlip | PinyinInput.Slipping | 开始滑行输入 |
| Waiting | BeginFlip | PinyinInput.Flipping | 开始翻动输入 |
| Waiting | BeginXPad | PinyinInput.XPadding | 开始 X-Pad 输入 |
| Waiting | LoadCandidates | CandidateSelection.Choosing | 加载候选词进入候选选择 |
| Waiting | ReturnToIdle | Idle | 提交/清空输入后回到空闲 |
| Waiting | 其他 | **保持 Waiting** | Fail-safe |

#### 7.2.3 从 PinyinInput.Slipping 状态转换

| 当前状态 | 转换类型 | 新状态 | 说明 |
|---------|---------|-------|------|
| Slipping | SelectSlipChar | PinyinInput.Waiting | 滑行字符选中后回到等待 |
| Slipping | BeginFlip | PinyinInput.Flipping | 滑行中触发翻动 |
| Slipping | LoadCandidates | CandidateSelection.Choosing | 滑行输入结束加载候选词 |
| Slipping | ReturnToIdle | PinyinInput.Waiting | 异常终止回到等待状态 |
| Slipping | 其他 | **保持 Slipping** | Fail-safe |

#### 7.2.4 从 PinyinInput.Flipping 状态转换

| 当前状态 | 转换类型 | 新状态 | 说明 |
|---------|---------|-------|------|
| Flipping | SelectFlipChar | PinyinInput.Waiting | 翻动字符选中后回到等待 |
| Flipping | LoadCandidates | CandidateSelection.Choosing | 翻动输入结束加载候选词 |
| Flipping | ReturnToIdle | PinyinInput.Waiting | 异常终止回到等待状态 |
| Flipping | 其他 | **保持 Flipping** | Fail-safe |

#### 7.2.5 从 PinyinInput.XPadding 状态转换

| 当前状态 | 转换类型 | 新状态 | 说明 |
|---------|---------|-------|------|
| XPadding | SelectXPadZone | PinyinInput.Waiting | X-Pad 区域选中后回到等待 |
| XPadding | LoadCandidates | CandidateSelection.Choosing | X-Pad 输入结束加载候选词 |
| XPadding | ReturnToIdle | PinyinInput.Waiting | 异常终止回到等待状态 |
| XPadding | 其他 | **保持 XPadding** | Fail-safe |

#### 7.2.6 从 CandidateSelection.Choosing 状态转换

| 当前状态 | 转换类型 | 新状态 | 说明 |
|---------|---------|-------|------|
| Choosing | FilterCandidates | CandidateSelection.Filtering | 应用拼音过滤 |
| Choosing | AdvanceFilterCandidates | CandidateSelection.AdvanceFiltering | 进入高级过滤 |
| Choosing | ReturnToIdle | PinyinInput.Waiting / Idle | 选择候选后回到等待/空闲 |
| Choosing | LoadCommitOptions | CommitOptionChoosing | 进入提交选项 |
| Choosing | PageCandidates | CandidateSelection.Choosing | 翻页（pageIndex 更新） |
| Choosing | LoadCandidates | CandidateSelection.Choosing | 重新加载候选词 |
| Choosing | 其他 | **保持 Choosing** | Fail-safe |

#### 7.2.7 从 CandidateSelection.Filtering 状态转换

| 当前状态 | 转换类型 | 新状态 | 说明 |
|---------|---------|-------|------|
| Filtering | BackToPrevious | CandidateSelection.Choosing | 清除过滤条件回到候选选择 |
| Filtering | FilterCandidates | CandidateSelection.Filtering | 更新过滤条件 |
| Filtering | PageCandidates | CandidateSelection.Filtering | 翻页（pageIndex 更新） |
| Filtering | ReturnToIdle | PinyinInput.Waiting / Idle | 选择候选后回到等待/空闲 |
| Filtering | 其他 | **保持 Filtering** | Fail-safe |

#### 7.2.8 从 CandidateSelection.AdvanceFiltering 状态转换

| 当前状态 | 转换类型 | 新状态 | 说明 |
|---------|---------|-------|------|
| AdvanceFiltering | BackToPrevious | CandidateSelection.Choosing | 确认过滤条件回到候选选择 |
| AdvanceFiltering | AdvanceFilterCandidates | CandidateSelection.AdvanceFiltering | 更新高级过滤条件 |
| AdvanceFiltering | PageCandidates | CandidateSelection.AdvanceFiltering | 翻页 |
| AdvanceFiltering | 其他 | **保持 AdvanceFiltering** | Fail-safe |

#### 7.2.9 从 CommitOptionChoosing 状态转换

| 当前状态 | 转换类型 | 新状态 | 说明 |
|---------|---------|-------|------|
| CommitOptionChoosing | ReturnToIdle | PinyinInput.Waiting / Idle | 选择完成后回到等待/空闲 |
| CommitOptionChoosing | LoadCommitOptions | CommitOptionChoosing | 更新提交选项 |
| CommitOptionChoosing | 其他 | **保持 CommitOptionChoosing** | Fail-safe |

#### 7.2.10 从 EditorEditing 状态转换

| 当前状态 | 转换类型 | 新状态 | 说明 |
|---------|---------|-------|------|
| CursorMoving | MoveCursor | EditorEditing.CursorMoving | 更新光标位置 |
| CursorMoving | SelectText | EditorEditing.TextSelecting | 从光标移动切换到文本选择 |
| CursorMoving | ReturnToIdle | 前一状态（通过 pop） | 编辑结束回到前一状态 |
| CursorMoving | BackToPrevious | 前一状态（通过 pop） | 同上 |
| TextSelecting | SelectText | EditorEditing.TextSelecting | 更新选择范围 |
| TextSelecting | ReturnToIdle | 前一状态（通过 pop） | 编辑结束回到前一状态 |
| TextSelecting | BackToPrevious | 前一状态（通过 pop） | 同上 |
| EditorEditing.* | 其他 | **保持当前状态** | Fail-safe |

#### 7.2.11 从 SymbolChoosing 状态转换

| 当前状态 | 转换类型 | 新状态 | 说明 |
|---------|---------|-------|------|
| SymbolChoosing | OpenSymbolGroup | SymbolChoosing | 切换分组（groupId 更新） |
| SymbolChoosing | PageCandidates | SymbolChoosing | 翻页 |
| SymbolChoosing | ReturnToIdle | Idle | 退出符号选择 |
| SymbolChoosing | 其他 | **保持 SymbolChoosing** | Fail-safe |

#### 7.2.12 从 EmojiChoosing 状态转换

| 当前状态 | 转换类型 | 新状态 | 说明 |
|---------|---------|-------|------|
| EmojiChoosing | OpenEmojiGroup | EmojiChoosing | 切换分组（groupId 更新） |
| EmojiChoosing | PageCandidates | EmojiChoosing | 翻页 |
| EmojiChoosing | ReturnToIdle | Idle | 退出 Emoji 选择 |
| EmojiChoosing | 其他 | **保持 EmojiChoosing** | Fail-safe |

### 7.3 不合法转换的处理策略

采用 **fail-safe** 策略：不合法的转换（即当前状态 × 转换类型的组合不在上述规则表中）不会抛出异常，而是保持原状态不变。这确保了在意外的用户操作或程序错误下，键盘状态不会崩溃或进入未定义状态。

### 7.4 sideEffects 的含义和处理方式

`KeyboardStateTransition.Result.sideEffects` 是状态转换产生的副作用意图列表，类型为 `List<ImeIntent>`。副作用不直接由 KeyboardStateMachine 执行，而是返回给 ImeEngine 异步处理。

典型的副作用类型：

| 副作用 ImeIntent | 触发时机 | 说明 |
|-----------------|---------|------|
| `ImeIntent.CommitInput` | 提交输入列表 | 通过 ImeOutputBridge 输出文本到编辑器 |
| `ImeIntent.DeleteInput` | 删除输入 | 通过 InputListEditor 操作输入列表 |
| `ImeIntent.PerformEdit(EditorAction)` | 编辑器操作 | 通过 ImeOutputBridge 输出编辑指令 |
| `ImeIntent.UpdateConfig(...)` | 切换手模式/主题 | 更新 ImeConfig |
| `ImeIntent.SwitchKeyboard(...)` | 切换键盘类型 | 改变 keyboard.type 并选择对应的 KeyboardIntentHandler |

---

## 8. 状态历史与回退

### 8.1 KeyboardStateHistory 有界历史栈

```kotlin
class KeyboardStateHistory(maxSize: Int = 10) {
    private val stack = ArrayDeque<KeyboardState>(maxSize)

    fun push(state: KeyboardState) {
        if (stack.size >= stack.maxSize) stack.removeFirst()
        stack.addLast(state)
    }

    fun pop(): KeyboardState? = stack.removeLastOrNull()

    fun peek(): KeyboardState? = stack.lastOrNull()

    fun clear() = stack.clear()
}
```

### 8.2 回退策略

| 场景 | 回退行为 | 说明 |
|------|---------|------|
| 同一键盘内的子状态回退 | `stateHistory.pop()` | 如 EditorEditing → Waiting |
| 键盘类型切换 | `stateHistory.clear()` | 不同键盘类型之间无回退关系 |
| 撤销（Revoke） | 不依赖状态历史 | 通过 InputListEditor 的 undoStack 实现 |
| 无前序状态 | 回到 Idle / 初始状态 | `pop()` 返回 null 时使用默认状态 |

### 8.3 与 Java 版本状态回退的对比

Java 版本使用 `State.previous` 链表实现无限深度的状态回退。`change_State_to_Previous()` 方法会跳过与当前状态类型相同的前序状态，直到找到不同类型的前序状态或到达链表末尾。这种方式可能导致无限增长的链表，且状态类型相同的前序状态被跳过的逻辑不够直观。

v4 版本通过 KeyboardStateHistory 有界栈替代：

| 维度 | Java 版本 | v4 版本 |
|------|----------|---------|
| 数据结构 | `State.previous` 链表（无限增长） | `ArrayDeque<KeyboardState>(maxSize = 10)` |
| 回退方式 | 遍历 previous 链，跳过相同类型 | 直接 `pop()` |
| 内存安全 | 可能内存泄漏 | 有界，自动丢弃最旧状态 |
| 键盘切换 | 不清空历史 | 清空历史栈 |

---

## 9. 共享组件

Keyboard 子类通过组合模式使用以下独立共享组件，替代 Java 版本中 BaseKeyboard 的继承共享行为：

| 共享行为 | 独立组件 | Java BaseKeyboard 对应方法 |
|----------|----------|--------------------------|
| 按键音效播放 | `KeyAudioPlayer.play(keyType)` | `play_SingleTick_InputAudio()` / `play_DoubleTick_InputAudio()` / `play_PageFlip_InputAudio()` |
| 输入列表更新 | `InputListOperator.apply(intent, list)` | `commit_InputList()` / `confirm_InputList_Pending()` / `delete_InputList_Selected()` 等 |
| 状态变更传播 | StateFlow 自动传播 | `fire_InputMsg(Keyboard_State_Change_Done)` |
| 候选查询 | `CandidateQuery.query(dict, spell)` | `PinyinDict.getCandidates()` |
| 拼音候选评估 | `PinyinCandidateEvaluator.evaluate(dict, input)` | `determine_NotConfirmed_InputWord()` / `predict_NotConfirmed_Phrase_InputWords()` |
| 拉丁补全评估 | `LatinCompletionEvaluator.evaluate(dict, input)` | `getTopBestMatchedLatins()` / `do_InputList_Pending_Completion_Creating()` |

### 9.1 KeyAudioPlayer

按键音效播放器，封装不同类型的音效播放逻辑：

| 音效类型 | 对应 Java 版本 | 触发场景 |
|---------|--------------|---------|
| SingleTick | `play_SingleTick_InputAudio` | 单击按键、确认输入 |
| DoubleTick | `play_DoubleTick_InputAudio` | 滑行输入中追加字符、长按提交按键 |
| PingTick | `play_PingTick_InputAudio` | X-Pad 区域激活 |
| ClockTick | `play_ClockTick_InputAudio` | X-Pad 字符按键切换 |
| PageFlip | `play_PageFlip_InputAudio` | 候选词翻页 |

### 9.2 InputListOperator

输入列表操作器，封装 InputList 的各种操作逻辑：

| 操作 | 对应 Java 版本方法 | 说明 |
|------|-------------------|------|
| 提交输入列表 | `commit_InputList()` / `commit_InputList_and_Goto_Init_State()` | 确认 pending 并输出文本 |
| 确认 pending | `confirm_InputList_Pending()` | 确认待输入字符 |
| 删除选中输入 | `delete_InputList_Selected()` | 删除当前选中的输入 |
| 回删输入 | `backspace_InputList_or_Editor()` / `do_InputList_Backspacing()` | 向前删除输入或编辑器内容 |
| 撤销提交 | `revoke_Committed_InputList()` | 撤销上一次提交 |
| 丢弃 pending | `drop_InputList_Pending()` | 丢弃无效的待输入 |

### 9.3 CandidateQuery 与候选评估

| 组件 | 职责 | 对应 Java 版本 |
|------|------|---------------|
| `CandidateQuery` | 根据拼音拼写查询候选词 | `PinyinDict.getCandidates()` / `PinyinDict.getFirstBestCandidate()` / `PinyinDict.findTopBestEmojisMatchedPhrase()` |
| `PinyinCandidateEvaluator` | 评估未确认输入的候选字，进行短语预测 | `determine_NotConfirmed_InputWord()` / `predict_NotConfirmed_Phrase_InputWords()` |
| `LatinCompletionEvaluator` | 评估拉丁输入补全 | `UserInputDataDict.findTopBestMatchedLatins()` |

---

## 10. InputKey 体系

```kotlin
sealed class InputKey {
    abstract val id: String
    abstract val label: String
    abstract val weight: Float  // 按键在行中的宽度权重

    // 字符按键
    data class Char(
        override val id: String,
        override val label: String,
        val levels: List<String>,
        val replacements: List<String> = emptyList(),
        override val weight: Float = 1f,
    ) : InputKey() {
        val hasReplacements: Boolean get() = replacements.size > 1

        fun nextReplacement(current: String): String {
            if (replacements.size <= 1) return current
            val index = replacements.indexOf(current)
            return if (index >= 0) replacements[(index + 1) % replacements.size] else replacements[0]
        }

        fun canReplace(current: String): Boolean = replacements.size > 1 && current in replacements
    }

    // 控制按键
    sealed class Ctrl : InputKey() {
        data class Space(override val weight: Float = 2f) : Ctrl() {
            override val id = "ctrl_space"
            override val label = "空格"
        }
        data class Backspace(override val weight: Float = 1.5f) : Ctrl() {
            override val id = "ctrl_backspace"
            override val label = "⌫"
        }
        data class Enter(override val weight: Float = 1.5f) : Ctrl() {
            override val id = "ctrl_enter"
            override val label = "↵"
        }
        data class Commit(override val weight: Float = 1.5f) : Ctrl() {
            override val id = "ctrl_commit"
            override val label = "确认"
        }
        data class SwitchKeyboard(val target: KeyboardType) : Ctrl() {
            override val id = "ctrl_switch_${target.name.lowercase()}"
            override val label = target.switchLabel
            override val weight = 1.5f
        }
        data class SwitchIme(override val weight: Float = 1.5f) : Ctrl() {
            override val id = "ctrl_switch_ime"
            override val label = "🌐"
        }
        data class XPadToggle(override val weight: Float = 1f) : Ctrl() {
            override val id = "ctrl_xpad_toggle"
            override val label = "✦"
        }
        data class Editor(val action: EditorAction) : Ctrl() {
            override val id = "ctrl_editor_${action.name.lowercase()}"
            override val label = action.label
            override val weight = 1f
        }
        data class PinyinToggle(val toggle: PinyinToggleType) : Ctrl() {
            override val id = "ctrl_pinyin_toggle_${toggle.name.lowercase()}"
            override val label = toggle.label
            override val weight = 1f
        }
    }

    // 候选字按键
    data class Candidate(
        override val id: String,
        override val label: String,
        val word: InputWord,
        override val weight: Float = 1f,
    ) : InputKey()

    // 数学运算按键
    data class MathOp(
        override val id: String,
        override val label: String,
        val op: MathOperator,
        override val weight: Float = 1f,
    ) : InputKey()

    // 符号按键
    data class Symbol(
        override val id: String,
        override val label: String,
        val group: String,
        val pairWith: String? = null,
        override val weight: Float = 1f,
    ) : InputKey()

    // X-Pad 按键
    data class XPad(
        override val id: String,
        override val label: String,
        val zones: List<XPadZone>,
        override val weight: Float = 1f,
    ) : InputKey()

    // 空占位按键
    data object Null : InputKey() {
        override val id = "null"
        override val label = ""
        override val weight = 1f
    }
}
```

### 10.1 InputKey 与 Java 版本 Key 体系的对照

| Java Key 子类 | v4 InputKey 子类 | 变更说明 |
|--------------|-----------------|---------|
| `CharKey` | `InputKey.Char` | sealed class 子类替代继承，replacements 和 level 内嵌 |
| `CtrlKey` | `InputKey.Ctrl` sealed class | 各 CtrlKey.Type 拆分为 Ctrl 的独立子类 |
| `InputWordKey` | `InputKey.Candidate` | 更语义化的命名 |
| `MathOpKey` | `InputKey.MathOp` | sealed class 子类替代继承 |
| `SymbolKey` | `InputKey.Symbol` | sealed class 子类替代继承 |
| `XPadKey` | `InputKey.XPad` | sealed class 子类替代继承 |
| `Key`（空占位） | `InputKey.Null` | data object 单例替代 null |

---

## 11. 按键生成器（已移至 :ime-ui 模块）

> **注意**：`KeyTableGenerator` 接口和 `KeyTableContext` 数据类已从 `:ime-engine` 模块移至 `:ime-ui` 模块。按键布局是 UI 关注点，由 UI 层根据键盘类型、输入模式、键盘状态和相关数据决定按键布局。详细设计见 [010-UI 库设计总览](../ui/010-ui-library-overview.md)。

以下是历史定义（已废弃）：

```kotlin
// 已移至 :ime-ui 模块
interface KeyTableGenerator {
    fun generate(context: KeyTableContext): List<List<InputKey>>
}

// 已移至 :ime-ui 模块
data class KeyTableContext(
    val config: ImeConfig,
    val keyboard: Keyboard,
    val inputList: InputList,
    val candidateList: CandidateList,
)
```

### 11.1 各键盘的按键生成逻辑

| 键盘类型 | KeyTableGenerator 实现 | 根据 KeyboardState 变化 |
|---------|----------------------|----------------------|
| PinyinKeyboard | `PinyinKeyTable` | Waiting → 标准网格；Slipping → 后继字母网格；Flipping → 全部候选字母网格；XPadding → X-Pad 布局 |
| LatinKeyboard | `LatinKeyTable` | 始终标准网格 |
| NumberKeyboard | `NumberKeyTable` | 标准网格，X-Pad 模式下显示退出按钮 |
| MathKeyboard | `MathKeyTable` | 始终标准网格 |
| SymbolKeyboard | `SymbolEmojiKeyTable` | 符号网格，含分组切换和翻页 |
| EmojiKeyboard | `SymbolEmojiKeyTable` | Emoji 网格，含分组切换和翻页 |
| EditorKeyboard | `EditorKeyTable` | 标准网格 / 光标定位网格（EditorEditing 状态） |
| PinyinCandidateKeyboard | `PinyinCandidateKeyTable` | 候选词网格 / 高级过滤网格 |
| InputListCommitOptionKeyboard | `InputListCommitOptionKeyTable` | 提交选项网格 |

---

## 12. 与 Java 版本的对照

### 12.1 状态模型对照

| Java 版本 | v4 版本 | 变化说明 |
|-----------|---------|---------|
| `State` (Type + Data + previous) | `KeyboardState` sealed class | 从 Type+Data 组合改为 sealed class 层级，状态数据内嵌于各子类；previous 链表改为 `KeyboardStateHistory` 有界栈 |
| `State.Type.InputChars_Input_Wait_Doing` | `KeyboardState.PinyinInput.Waiting` | 从平面枚举改为嵌套 sealed class，语义更清晰 |
| `State.Type.InputChars_Slip_Doing` | `KeyboardState.PinyinInput.Slipping` | 数据从 `InputCharsSlipStateData` 内嵌到 Slipping 子类字段 |
| `State.Type.InputChars_Flip_Doing` | `KeyboardState.PinyinInput.Flipping` | 数据从 `InputCharsFlipStateData` 内嵌到 Flipping 子类字段 |
| `State.Type.InputChars_XPad_Input_Doing` | `KeyboardState.PinyinInput.XPadding` | 数据内嵌，XPad 视图状态不再独立为 `XPadState` |
| `State.Type.InputCandidate_Choose_Doing` | `KeyboardState.CandidateSelection.Choosing` | 分页数据从 `PagingStateData` 抽象类改为子类字段 |
| `State.Type.InputCandidate_Advance_Filter_Doing` | `KeyboardState.CandidateSelection.AdvanceFiltering` | 部首/声调过滤从 `PinyinCandidateAdvanceFilterStateData` 改为子类字段 |
| `State.Type.InputList_Commit_Option_Choose_Doing` | `KeyboardState.CommitOptionChoosing` | 数据从 `InputListCommitOptionChooseStateData` 内嵌 |
| `State.Type.Editor_Edit_Doing` | `KeyboardState.EditorEditing.CursorMoving/TextSelecting` | 从单一状态拆分为两个子状态 |
| `State.Data`（接口） | sealed class 子类字段 | 从接口+实现类改为内嵌字段 |

### 12.2 状态管理机制对照

| 维度 | Java 版本 | v4 版本 |
|------|----------|---------|
| 状态定义 | 10 个独立 `StateData` 类 + `State` 链表 | `KeyboardState` sealed class 层级 |
| 状态转换 | 分散在 `BaseKeyboard` 各方法中 | `KeyboardStateMachine.transition()` 集中处理 |
| 状态历史 | `State.previous` 链表（可能无限增长） | `ArrayDeque<KeyboardState>(maxSize = 10)` |
| 状态回退 | 通过 `previous` 链回退，跳过相同类型 | `stateHistory.pop()` |
| 键盘切换 | 无清空逻辑 | 切换时清空历史栈 |
| 转换触发 | 直接在键盘方法中调用 `change_State_To()` | 通过 `KeyboardStateTransition` 间接触发 |

### 12.3 键盘继承层次对照

| Java 继承链 | v4 对应 | 变更说明 |
|-----------|---------|---------|
| `BaseKeyboard` | `Keyboard` sealed class + 共享组件 | 继承共享行为提取为独立组件组合 |
| `BaseKeyboard → EditorEditKeyboard` | `Keyboard` + `EditorEditing` 状态 | 编辑器编辑逻辑从中间层提取到状态机 |
| `EditorEditKeyboard → PinyinKeyboard` | `PinyinKeyboard` | 组合模式：注入 `PinyinDict`、`ImeConfig`、`KeyboardStateMachine` |
| `EditorEditKeyboard → LatinKeyboard` (via `DirectInputKeyboard`) | `LatinKeyboard` | 组合模式 |
| `DirectInputKeyboard → NumberKeyboard` | `NumberKeyboard` | 组合模式 |
| `EditorEditKeyboard → MathKeyboard` | `MathKeyboard` | 组合模式 |
| `InputCandidateKeyboard → SymbolKeyboard` | `SymbolKeyboard` | 组合模式 |
| `InputCandidateKeyboard → EmojiKeyboard` | `EmojiKeyboard` | 组合模式 |
| `InputCandidateKeyboard → PinyinCandidateKeyboard` | `PinyinCandidateKeyboard` | 组合模式 |
| `BaseKeyboard → InputListCommitOptionKeyboard` | `InputListCommitOptionKeyboard` | 组合模式 |
| `EditorEditKeyboard → EditorKeyboard` | `EditorKeyboard` | 组合模式 |

### 12.4 消息体系对照

| Java 版本 | v4 版本 | 变更说明 |
|-----------|---------|---------|
| `InputMsgType.Keyboard_State_Change_Done` | `StateFlow<ImeState>` 自动传播 | 状态变更不再需要手动触发消息 |
| `InputMsgType.Keyboard_Switch_Doing` | `ImeIntent.SwitchKeyboard` | 键盘切换通过 Intent 驱动 |
| `InputMsgType.InputChars_Input_Doing` | `KeyboardState` 变更 + StateFlow | 输入状态通过状态机变更传播 |
| `InputMsgType.InputCandidate_Choose_Doing` | `KeyboardState.CandidateSelection` 变更 | 候选选择通过状态变更传播 |
| `InputMsgType.Editor_Edit_Doing` | `ImeOutput.PerformEdit` | 编辑操作通过输出桥接传播 |
| `UserKeyMsgType` (13 个值) | `InputGesture` + `KeyGesture` | 从消息枚举改为 sealed class |
| `UserInputMsgType` (16 个值) | `ImeIntent` sealed class | 从消息枚举改为 Intent |

### 12.5 关键设计差异

1. **状态不可变性**：Java 版本中 `InputList`、`State.Data` 等均为可变对象，v4 全部改为不可变 data class，通过 `copy()` 生成新实例。

2. **状态历史**：Java 版本使用 `State.previous` 链表实现无限深度的状态回退，v4 改为 `KeyboardStateHistory` 有界栈（最大 10 层），并在键盘类型切换时清空历史。

3. **分页逻辑**：Java 版本的 `PagingStateData` 是可变抽象类，内部维护 `pageStart` 状态；v4 将分页状态（`pageIndex`、`pageSize`）作为 CandidateSelection.Choosing 的不可变字段。

4. **过滤逻辑**：Java 版本的 `PinyinCandidateFilterStateData` 在内部维护过滤结果缓存；v4 将过滤条件和过滤结果作为 CandidateSelection.Filtering/AdvanceFiltering 的不可变字段。

5. **XPad 状态归属**：Java 版本中 `XPadState` 是视图层独立状态；v4 将 XPad 状态整合到引擎状态机的 `XPadding` 子状态中，遵循「引擎拥有所有逻辑状态」的原则。

6. **撤销机制**：Java 版本使用 `Inputboard.Stage`（committed/cleaned 两种类型，各保存一份 InputList 副本）；v4 改为 `InputListEditor` 的 undoStack/redoStack 双栈（最大 50 层），支持多次撤销和重做。
