# 键盘状态机

> PlantUML 状态图：[@file:../diagrams/engine-state-machine.puml](../diagrams/engine-state-machine.puml)

## 1. 概述

键盘状态机是筷字输入法的核心逻辑，管理键盘在不同输入模式下的状态转换。基于 Sealed class 的显式状态定义和组合模式的状态处理器，提供编译期状态合法性检查和集中化的转换规则。

---

## 2. KeyboardState 定义

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

### 状态层次一览

| 顶层状态 | 子状态 | 说明 |
|----------|--------|------|
| `Idle` | — | 空闲 |
| `PinyinInput` | `Waiting` | 等待输入 |
| | `Slipping` | 滑行输入中 |
| | `Flipping` | 翻转输入中 |
| | `XPadding` | X-Pad 输入中 |
| `CandidateSelection` | `Choosing` | 候选选择 |
| | `Filtering` | 候选过滤 |
| | `AdvanceFiltering` | 高级过滤（部首/声调） |
| `CommitOptionChoosing` | — | 提交选项 |
| `EditorEditing` | `CursorMoving` | 光标移动 |
| | `TextSelecting` | 文本选择 |
| `SymbolChoosing` | — | 符号选择 |
| `EmojiChoosing` | — | Emoji 选择 |

---

## 3. 状态转换规则

```kotlin
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

---

## 4. KeyboardStateMachine

使用组合模式实现状态处理器：

```kotlin
class KeyboardStateMachine(
    private val audioPlayer: KeyAudioPlayer,
    private val inputListOp: InputListOperator,
) {
    private var _state: KeyboardState = KeyboardState.Idle
    val state: KeyboardState get() = _state
    private val stateHistory = ArrayDeque<KeyboardState>(maxSize = 10)

    fun transition(transition: KeyboardTransition): List<ImeIntent> {
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

---

## 5. 键盘类型与初始状态

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

## 6. Keyboard 组合模式

### 6.1 键盘接口

```kotlin
sealed class Keyboard {
    abstract val type: KeyboardType
    abstract val state: KeyboardState
    abstract fun handleIntent(intent: ImeIntent): KeyboardResult
}

data class KeyboardResult(
    val newState: KeyboardState,
    val sideEffects: List<ImeIntent> = emptyList(),
    val commitText: String? = null,
)
```

### 6.2 各键盘实现

```kotlin
class PinyinKeyboard(
    private val dict: PinyinDict,
    private val config: ImeConfig,
    private val stateMachine: KeyboardStateMachine,
) : Keyboard() {
    override val type = KeyboardType.Pinyin
    override val state get() = stateMachine.state

    override fun handleIntent(intent: ImeIntent): KeyboardResult {
        return when (intent) {
            is ImeIntent.PressKey -> handleKeyPress(intent)
            is ImeIntent.SelectCandidate -> handleCandidateSelection(intent)
            is ImeIntent.PageCandidate -> handleCandidatePaging(intent)
            else -> KeyboardResult(state)
        }
    }
}
```

### 6.3 共享组件

| 共享行为 | 独立组件 |
|----------|----------|
| 按键音效播放 | `KeyAudioPlayer.play(keyType)` |
| 输入列表更新 | `InputListOperator.apply(intent, list)` |
| 状态变更传播 | StateFlow 自动传播 |
| 候选查询 | `CandidateQuery.query(dict, spell)` |
| 拼音候选评估 | `PinyinCandidateEvaluator.evaluate(dict, input)` |
| 拉丁补全评估 | `LatinCompletionEvaluator.evaluate(dict, input)` |

---

## 7. InputKey 体系

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

### 按键生成器

```kotlin
interface KeyTableGenerator {
    fun generate(context: KeyTableContext): List<List<InputKey>>
}

data class KeyTableContext(
    val config: ImeConfig,
    val inputState: InputListState,
    val keyboardState: KeyboardState,
    val candidates: List<InputWord>,
)
```

---

## 8. StateHistory 有界历史栈

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

### 回退策略

- 键盘切换时清空历史栈（不同键盘类型之间无回退关系）
- 同一键盘内的子状态回退通过 `pop()` 实现
- 撤销（Revoke）不依赖状态历史，而是依赖 InputList 的撤销栈
