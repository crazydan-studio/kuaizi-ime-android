# 输入列表

## 1. 概述

`InputList` 是筷字输入法的核心数据结构，管理用户输入的字符序列、游标位置、待输入字符、补全候选项和间隔插入。v4 采用不可变数据模型，利用 Kotlin data class 的 `copy()` 机制实现状态更新，从根本上保证线程安全。

---

## 2. InputListState 不可变数据模型

```kotlin
/**
 * 输入列表状态，不可变。
 *
 * 所有状态变更通过 copy() 创建新实例，原始实例不受影响。
 * 游标位置通过 gapIndex 表示，指向 inputs 中的 GapInput 位置。
 */
data class InputListState(
    val inputs: List<InputItem> = emptyList(),
    val gapIndex: Int = 0,
    val pending: PendingInput? = null,
    val mathExprNested: InputListState? = null,
) {
    /** 当前游标位置的 GapInput */
    val cursorGap: InputItem.Gap
        get() = inputs.getOrElse(gapIndex) { InputItem.Gap }

    /** 可见的字符输入列表（排除 GapInput） */
    val visibleInputs: List<InputItem.Char>
        get() = inputs.filterIsInstance<InputItem.Char>()

    /** 输入文本内容 */
    val text: String
        get() = visibleInputs.joinToString("") { it.text }

    /** 是否为空 */
    val isEmpty: Boolean
        get() = inputs.all { it is InputItem.Gap }

    // === 状态变更操作（返回新实例） ===

    /** 在游标位置追加字符输入 */
    fun appendChar(char: InputItem.Char): InputListState {
        val newInputs = inputs.toMutableList().apply {
            add(gapIndex, char)
            // 在新字符后插入 GapInput
            add(gapIndex + 1, InputItem.Gap)
        }
        return copy(inputs = newInputs, gapIndex = gapIndex + 2)
    }

    /** 删除游标前的一个字符 */
    fun deleteCharBeforeCursor(): InputListState {
        if (gapIndex < 2) return this
        val newInputs = inputs.toMutableList().apply {
            removeAt(gapIndex - 2) // 删除字符
            removeAt(gapIndex - 2) // 删除字符后的 GapInput
        }
        return copy(inputs = newInputs, gapIndex = gapIndex - 2)
    }

    /** 移动游标到指定位置 */
    fun moveCursorTo(newGapIndex: Int): InputListState {
        val clampedIndex = newGapIndex.coerceIn(0, inputs.lastIndex)
        return copy(gapIndex = clampedIndex)
    }

    /** 清空所有输入 */
    fun clean(): InputListState = InputListState()

    /** 设置待输入字符 */
    fun withPending(pending: PendingInput?): InputListState =
        copy(pending = pending)
}
```

---

## 3. 输入项类型

```kotlin
sealed class InputItem {
    abstract val id: String

    /** 字符输入 */
    data class Char(
        override val id: String,
        val text: String,
        val keys: List<InputKey>,
        val replacements: List<String> = emptyList(),
        val word: InputWord? = null,
        val pairSymbol: PairSymbol? = null,
    ) : InputItem() {
        val hasPair: Boolean get() = pairSymbol != null
        val hasReplacements: Boolean get() = replacements.size > 1

        fun nextReplacement(text: String): String {
            if (replacements.size <= 1) return text
            val index = replacements.indexOf(text)
            return if (index >= 0) replacements[(index + 1) % replacements.size] else replacements[0]
        }

        fun canReplace(key: InputKey.Char): Boolean = replacements.size > 1 && key.text in replacements
    }

    /** 间隔/游标位置 */
    data object Gap : InputItem() {
        override val id = "gap"
    }

    /** 数学表达式 */
    data class MathExpr(
        override val id: String,
        val nestedList: InputListState,
    ) : InputItem()
}
```

---

## 4. 待输入字符

```kotlin
/**
 * 待输入字符，表示拼音输入中尚未提交的字符序列。
 * 与 InputListState 分离，因为 pending 是临时状态。
 */
data class PendingInput(
    val chars: List<InputItem.Char>,
    val completions: List<InputCompletion> = emptyList(),
    val pinyinToggles: Set<PinyinToggleType> = emptySet(),
)
```

---

## 5. InputWord 输入字

```kotlin
sealed class InputWord {
    abstract val text: String
    abstract val frequency: Int

    /** 拼音字 */
    data class Pinyin(
        override val text: String,
        override val frequency: Int,
        val spell: String,
        val variant: String? = null,     // 繁体异体
        val tone: Int? = null,
    ) : InputWord()

    /** 拼音词组 */
    data class PinyinPhrase(
        override val text: String,
        override val frequency: Int,
        val spells: List<String>,
    ) : InputWord()

    /** Emoji */
    data class Emoji(
        override val text: String,
        override val frequency: Int = 0,
        val name: String,
        val group: String,
    ) : InputWord()

    /** 拉丁词 */
    data class Latin(
        override val text: String,
        override val frequency: Int,
    ) : InputWord()
}
```

---

## 6. InputCompletion 输入补全

```kotlin
sealed class InputCompletion {
    abstract val text: String

    /** 拉丁词补全 */
    data class LatinWord(
        override val text: String,
        val remaining: String,
    ) : InputCompletion()

    /** 拼音词组补全 */
    data class PhraseWord(
        override val text: String,
        val remaining: String,
        val spells: List<String>,
    ) : InputCompletion()
}
```

---

## 7. PairSymbol 对偶符号

```kotlin
data class PairSymbol(
    val open: String,    // 开启符号，如 (、[、"、'
    val close: String,   // 关闭符号，如 )、]、"、'
    val content: String? = null,  // 中间内容
)
```

---

## 8. 线程安全设计

不可变 data class + StateFlow 保证原子性：

```kotlin
// 引擎内部的 reduce 逻辑（ImeEngine.reduce）
// 所有状态变更串行执行，StateFlow 保证原子性
private suspend fun reduce(state: ImeState, intent: ImeIntent): ImeState {
    return when (intent) {
        is ImeIntent.PressKey -> {
            // 1. 更新 InputList（StateFlow 保证原子性）
            val newInputList = state.inputList.appendChar(
                InputItem.Char(id = uuid(), text = intent.key.label, keys = listOf(intent.key))
            )
            // 2. 异步查询候选（协程，不阻塞主线程）
            val candidates = dictProvider.pinyin.queryCandidates(newInputList.pending?.text ?: "")
            // 3. 返回新状态
            state.copy(
                inputList = newInputList,
                candidates = CandidateListState(candidates = candidates),
            )
        }
        // ...
    }
}
```

**关键保证**：
- `StateFlow.value` 的读写是原子的
- 所有状态变更在 `reduce` 中串行执行
- 异步操作（字典查询）通过协程挂起，不阻塞主线程
- 不可变 data class 无需同步

---

## 9. 撤销（Revoke）机制

由于 InputListState 是不可变的，撤销只需保存之前的状态引用：

```kotlin
class InputListEditor {
    private val undoStack = ArrayDeque<InputListState>(maxSize = 50)
    private val redoStack = ArrayDeque<InputListState>(maxSize = 50)

    fun pushUndo(state: InputListState) {
        undoStack.addLast(state)
        redoStack.clear()
    }

    fun undo(current: InputListState): InputListState {
        val previous = undoStack.removeLastOrNull() ?: return current
        redoStack.addLast(current)
        return previous
    }

    fun redo(current: InputListState): InputListState {
        val next = redoStack.removeLastOrNull() ?: return current
        undoStack.addLast(current)
        return next
    }
}
```

---

## 10. 游标管理

```kotlin
data class InputListState(
    val inputs: List<InputItem> = emptyList(),
    val gapIndex: Int = 0,
) {
    init {
        // 编译期断言：gapIndex 在合法范围内
        require(gapIndex >= 0) { "gapIndex must be >= 0, was $gapIndex" }
        require(gapIndex <= inputs.lastIndex + 1) {
            "gapIndex must be <= ${inputs.lastIndex + 1}, was $gapIndex"
        }
    }

    /** 移动游标到左侧的 GapInput */
    fun moveCursorLeft(): InputListState {
        val leftGapIndex = findPreviousGap(gapIndex)
        return if (leftGapIndex >= 0) copy(gapIndex = leftGapIndex) else this
    }

    /** 移动游标到右侧的 GapInput */
    fun moveCursorRight(): InputListState {
        val rightGapIndex = findNextGap(gapIndex)
        return if (rightGapIndex >= 0) copy(gapIndex = rightGapIndex) else this
    }

    private fun findPreviousGap(fromIndex: Int): Int {
        for (i in (fromIndex - 1) downTo 0) {
            if (inputs[i] is InputItem.Gap) return i
        }
        return -1
    }

    private fun findNextGap(fromIndex: Int): Int {
        for (i in (fromIndex + 1)..inputs.lastIndex) {
            if (inputs[i] is InputItem.Gap) return i
        }
        return -1
    }
}
```

---

## 11. InputClip 剪贴板输入

```kotlin
data class InputClip(
    val text: String,
    val type: InputTextType? = null,
) {
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
