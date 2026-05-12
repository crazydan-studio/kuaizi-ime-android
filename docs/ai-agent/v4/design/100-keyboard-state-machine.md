# 100 — 键盘状态机设计

## 1. 概述

键盘状态机是筷字输入法的核心逻辑，管理键盘在不同输入模式下的状态转换。Java 版本通过深层继承链和 `State` 链表实现状态机，v4 版本将其重构为基于 Sealed class 的显式状态定义和组合模式的状态处理器。

---

## 2. Java 版本状态机分析

### 2.1 当前状态类型

Java 版本在 `core/keyboard/state/` 中定义了 10 种状态数据类：

| 状态 | 说明 |
|------|------|
| `InputCharsSlipStateData` | 滑行输入状态（level0/1/2 按键，下一字符集） |
| `InputCharsFlipStateData` | 翻转输入状态（起始字符） |
| `PagingStateData` | 候选分页状态 |
| `PinyinCandidateChooseStateData` | 拼音候选选择状态 |
| `PinyinCandidateFilterStateData` | 拼音候选过滤状态 |
| `PinyinCandidateAdvanceFilterStateData` | 拼音高级过滤状态（部首、声调） |
| `EmojiChooseStateData` | Emoji 分组选择状态 |
| `SymbolChooseStateData` | 符号分组选择状态 |
| `EditorEditStateData` | 编辑器状态（光标/选择） |
| `InputListCommitOptionChooseStateData` | 提交选项状态 |

### 2.2 当前状态链

`State` 类使用链表结构（`previous` 指针）维护状态历史：

```java
class State {
    State previous;
    StateData data;
}
```

### 2.3 问题分析

1. **状态类型分散**：10 个独立的状态数据类，缺乏统一的类型约束
2. **状态转换隐式**：状态转换逻辑分散在 `BaseKeyboard` 的各个方法中，没有集中定义转换规则
3. **状态链的 `previous` 指针容易导致内存泄漏**：长链持有所有历史状态
4. **非法状态转换无保护**：任何状态都可以转换到任何其他状态，缺乏编译期检查
5. **状态恢复逻辑复杂**：通过 `previous` 链回退状态，容易出错

---

## 3. v4 状态机设计

### 3.1 Sealed class 状态定义

```kotlin
sealed class KeyboardState {
    // 空闲状态
    data object Idle : KeyboardState()

    // 拼音输入状态
    sealed class PinyinInput : KeyboardState() {
        data class Waiting(val pending: CharInput?) : PinyinInput()
        data class Slipping(
            val startKey: CharKey,
            val level0: List<CharKey>,
            val level1: List<CharKey>,
            val level2: List<CharKey>,
            val nextChars: Set<Char>,
        ) : PinyinInput()
        data class Flipping(
            val startChar: Char,
            val candidates: List<CharKey>,
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
            val spell: String,
            val filtered: List<InputWord>,
        ) : CandidateSelection()
        data class AdvanceFiltering(
            val radical: String?,
            val tone: Int?,
            val filtered: List<InputWord>,
        ) : CandidateSelection()
    }

    // 提交选项状态
    data class CommitOptionChoosing(
        val options: List<CommitOption>,
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

### 3.2 状态转换规则

```kotlin
// 状态转换定义，明确哪些转换是合法的
sealed class KeyboardTransition {
    // 拼音输入转换
    data class StartPinyinInput(val char: Char) : KeyboardTransition()
    data class StartSlipping(val startKey: CharKey) : KeyboardTransition()
    data class StartFlipping(val startChar: Char) : KeyboardTransition()
    data class StartXPadding(val zones: List<XPadZone>) : KeyboardTransition()
    data class SlipCharSelected(val char: Char) : KeyboardTransition()
    data class FlipCharSelected(val char: Char) : KeyboardTransition()
    data class XPadZoneSelected(val zone: XPadZone) : KeyboardTransition()

    // 候选选择转换
    data class ShowCandidates(val candidates: List<InputWord>) : KeyboardTransition()
    data class FilterCandidates(val spell: String) : KeyboardTransition()
    data class AdvanceFilterCandidates(val radical: String?, val tone: Int?) : KeyboardTransition()
    data class PageCandidates(val direction: PageDirection) : KeyboardTransition()

    // 提交选项转换
    data class ShowCommitOptions(val options: List<CommitOption>) : KeyboardTransition()

    // 编辑器转换
    data class StartCursorMoving(val position: Int) : KeyboardTransition()
    data class StartTextSelecting(val start: Int, val end: Int) : KeyboardTransition()

    // 符号/Emoji 转换
    data class ShowSymbolGroup(val groupId: String?) : KeyboardTransition()
    data class ShowEmojiGroup(val groupId: String?) : KeyboardTransition()

    // 通用转换
    data object ReturnToIdle : KeyboardTransition()
    data object BackToPrevious : KeyboardTransition()
}
```

### 3.3 状态处理器

替代 Java 版本的深层继承链，使用组合模式：

```kotlin
class KeyboardStateMachine(
    private val audioPlayer: KeyAudioPlayer,
    private val inputListOp: InputListOperator,
) {
    private var _state: KeyboardState = KeyboardState.Idle
    val state: KeyboardState get() = _state
    private val stateHistory = ArrayDeque<KeyboardState>(maxSize = 10)

    fun transition(transition: KeyboardTransition): List<IMEIntent> {
        val (newState, sideEffects) = when (_state) {
            is KeyboardState.Idle -> handleFromIdle(transition)
            is KeyboardState.PinyinInput.Waiting -> handleFromPinyinWaiting(transition)
            is KeyboardState.PinyinInput.Slipping -> handleFromPinyinSlipping(transition)
            is KeyboardState.PinyinInput.Flipping -> handleFromPinyinFlipping(transition)
            is KeyboardState.PinyinInput.XPadding -> handleFromPinyinXPadding(transition)
            is KeyboardState.CandidateSelection -> handleFromCandidateSelection(transition)
            is KeyboardState.EditorEditing -> handleFromEditorEditing(transition)
            is KeyboardState.SymbolChoosing -> handleFromSymbolChoosing(transition)
            is KeyboardState.EmojiChoosing -> handleFromEmojiChoosing(transition)
            is KeyboardState.CommitOptionChoosing -> handleFromCommitOptionChoosing(transition)
        }

        if (newState != _state) {
            stateHistory.addLast(_state)
            _state = newState
        }
        return sideEffects
    }

    fun backToPrevious() {
        _state = stateHistory.removeLastOrNull() ?: KeyboardState.Idle
    }
}
```

### 3.4 键盘类型与状态的关系

```kotlin
enum class KeyboardType {
    Pinyin,       // 拼音键盘：Idle → PinyinInput → CandidateSelection → CommitOptionChoosing
    Latin,        // 拉丁键盘：Idle → PinyinInput（滑行/X-Pad 模式）
    Number,       // 数字键盘：Idle（无子状态）
    Math,         // 数学键盘：Idle（嵌套 InputList）
    Symbol,       // 符号键盘：Idle → SymbolChoosing
    Emoji,        // Emoji 键盘：Idle → EmojiChoosing
    Editor,       // 编辑键盘：Idle → EditorEditing
    Candidate,    // 候选键盘：CandidateSelection
    CommitOption, // 提交选项键盘：CommitOptionChoosing
}

// 每种键盘类型对应其合法的初始状态和状态范围
val KeyboardType.initialState: KeyboardState
    get() = when (this) {
        KeyboardType.Pinyin -> KeyboardState.PinyinInput.Waiting(null)
        KeyboardType.Latin -> KeyboardState.PinyinInput.Waiting(null)
        KeyboardType.Number -> KeyboardState.Idle
        KeyboardType.Math -> KeyboardState.Idle
        KeyboardType.Symbol -> KeyboardState.SymbolChoosing(null)
        KeyboardType.Emoji -> KeyboardState.EmojiChoosing(null)
        KeyboardType.Editor -> KeyboardState.Idle
        KeyboardType.Candidate -> KeyboardState.CandidateSelection.Choosing(emptyList(), 0, 0)
        KeyboardType.CommitOption -> KeyboardState.CommitOptionChoosing(emptyList())
    }
```

---

## 4. 键盘组合模式设计

### 4.1 键盘接口

```kotlin
sealed class Keyboard {
    abstract val type: KeyboardType
    abstract val state: KeyboardState
    abstract fun handleIntent(intent: IMEIntent): KeyboardResult
}

data class KeyboardResult(
    val newState: KeyboardState,
    val sideEffects: List<IMEIntent> = emptyList(),
    val commitText: String? = null,
)
```

### 4.2 各键盘实现

```kotlin
class PinyinKeyboard(
    private val dict: PinyinDict,
    private val config: Config,
    private val stateMachine: KeyboardStateMachine,
) : Keyboard() {
    override val type = KeyboardType.Pinyin
    override val state get() = stateMachine.state

    override fun handleIntent(intent: IMEIntent): KeyboardResult {
        return when (intent) {
            is IMEIntent.KeyPressed -> handleKeyPress(intent)
            is IMEIntent.CandidateSelected -> handleCandidateSelection(intent)
            is IMEIntent.CandidatePaged -> handleCandidatePaging(intent)
            else -> KeyboardResult(state)
        }
    }
}
```

### 4.3 共享组件提取

| Java BaseKeyboard 中的共享行为 | Kotlin 独立组件 |
|-------------------------------|-----------------|
| `playKeyAudio()` | `KeyAudioPlayer.play(keyType)` |
| `updateInputList()` | `InputListOperator.apply(intent, list)` |
| `fireInputMsg()` | StateFlow 自动传播，不需要手动触发 |
| `evaluateInputWordKeys()` | `CandidateQuery.query(dict, spell)` |
| `evaluatePinyinCandidates()` | `PinyinCandidateEvaluator.evaluate(dict, input)` |
| `evaluateLatinCompletion()` | `LatinCompletionEvaluator.evaluate(dict, input)` |

---

## 5. 按键类型体系

### 5.1 Sealed class 替代枚举

Java 版本的按键类型继承体系：

```java
abstract class Key { ... }
abstract class TypedKey<T> extends Key { ... }
class CharKey extends TypedKey<String> { ... }
class CtrlKey extends TypedKey<CtrlKey.Type> { ... }  // CtrlKey.Type 是 25+ 值的枚举
class InputWordKey extends TypedKey<InputWord> { ... }
class MathOpKey extends TypedKey<MathOpKey.Op> { ... }
class SymbolKey extends Key { ... }
class XPadKey extends Key { ... }
```

v4 版本：

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
        val replacements: Map<String, String>,
        override val weight: Float = 1f,
    ) : InputKey()

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
        data class SwitchIME(override val weight: Float = 1.5f) : Ctrl() {
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

### 5.2 按键生成器

```kotlin
interface KeyTableGenerator {
    fun generate(context: KeyTableContext): List<List<InputKey>>
}

data class KeyTableContext(
    val config: Config,
    val inputState: InputListState,
    val keyboardState: KeyboardState,
    val candidates: List<InputWord>,
)
```

---

## 6. 状态历史与回退

### 6.1 有界历史栈

Java 版本使用链表维护状态历史，可能无限增长。v4 使用有界栈：

```kotlin
class StateHistory(maxSize: Int = 10) {
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

### 6.2 回退策略

- 键盘切换时清空历史栈（不同键盘类型之间无回退关系）
- 同一键盘内的子状态回退通过 `pop()` 实现
- 撤销（Revoke）不依赖状态历史，而是依赖 InputList 的撤销栈

---

## 7. Java 版本功能完整对照

| Java 键盘类型 | Java 状态 | v4 对应 | 备注 |
|-------------|----------|---------|------|
| PinyinKeyboard | InputChars_Input_Wait | PinyinInput.Waiting | 直接对应 |
| PinyinKeyboard | InputChars_Slip_Doing | PinyinInput.Slipping | 直接对应 |
| PinyinKeyboard | InputChars_Flip_Doing | PinyinInput.Flipping | 直接对应 |
| PinyinKeyboard | InputChars_XPad_Doing | PinyinInput.XPadding | 直接对应 |
| PinyinCandidateKeyboard | Candidate_Choose | CandidateSelection.Choosing | 直接对应 |
| PinyinCandidateKeyboard | Candidate_Filter_Basic | CandidateSelection.Filtering | 直接对应 |
| PinyinCandidateKeyboard | Candidate_Filter_Advance | CandidateSelection.AdvanceFiltering | 直接对应 |
| InputListCommitOptionKeyboard | Commit_Option_Choose | CommitOptionChoosing | 直接对应 |
| EditorEditKeyboard | Editor_Edit_Cursor | EditorEditing.CursorMoving | 直接对应 |
| EditorEditKeyboard | Editor_Edit_Selection | EditorEditing.TextSelecting | 直接对应 |
| SymbolKeyboard | Symbol_Choose | SymbolChoosing | 直接对应 |
| EmojiKeyboard | Emoji_Choose | EmojiChoosing | 直接对应 |
| LatinKeyboard | InputChars_Slip/XPad | PinyinInput.Slipping/XPadding | 拉丁键盘可复用拼音的滑行/X-Pad 模式 |
| NumberKeyboard | 无子状态 | Idle | 直接对应 |
| MathKeyboard | 无子状态 | Idle | 直接对应 |
| EditorKeyboard | 无子状态 | Idle | 直接对应 |
