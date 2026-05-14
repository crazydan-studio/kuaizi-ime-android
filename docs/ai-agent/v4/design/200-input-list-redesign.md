# 200 — 输入列表重构设计

## 1. 概述

`InputList` 是筷字输入法的核心数据结构，管理用户输入的字符序列、游标位置、待输入字符、补全候选项和间隔插入。Java 版本的 `InputList` 是一个 1100+ 行的可变类，存在线程安全风险和复杂的状态管理问题。v4 版本将其重构为不可变数据模型，利用 Kotlin data class 的 `copy()` 机制实现状态更新。

---

## 2. Java 版本 InputList 分析

### 2.1 核心数据结构

```java
class InputList {
    List<Input> inputs;          // 输入序列（包含 CharInput、GapInput、MathExprInput）
    int cursorIndex;             // 游标位置（GapInput 的索引）
    CharInput pending;           // 待输入字符（拼音输入中尚未提交的字符）
    List<InputCompletion> completions;  // 补全列表
    boolean gapSpacing;          // 是否在字符间显示间隔
    InputList mathExprNested;    // 嵌套的数学表达式 InputList
}
```

### 2.2 问题分析

1. **可变性导致线程安全风险**：`inputs` 是 `ArrayList`，从主线程和异步字典回调线程同时访问，可能导致 `ConcurrentModificationException`
2. **游标管理复杂**：`cursorIndex` 直接操作列表索引，插入/删除时需要手动维护游标位置，容易出错
3. **pending 状态与 InputList 混在一起**：`pending` 是临时状态，与持久化的输入列表属于不同的关注点
4. **MathExprInput 的嵌套 InputList**：递归数据结构嵌套在同一个类中，增加了复杂度
5. **撤销（Revoke）机制与 InputList 耦合**：撤销操作需要保存完整的状态快照
6. **completions 与 InputList 耦合**：补全候选项应该由查询逻辑管理，而非存储在 InputList 中

---

## 3. v4 输入列表设计

### 3.1 不可变数据模型

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

### 3.2 输入项类型

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
        /** 是否有可替换的候选（replacements 数量 > 1 表示有替换选项） */
        val hasReplacements: Boolean get() = replacements.size > 1

        /** 获取下一个替换文本，循环替换 */
        fun nextReplacement(text: String): String {
            if (replacements.size <= 1) return text
            val index = replacements.indexOf(text)
            return if (index >= 0) replacements[(index + 1) % replacements.size] else replacements[0]
        }

        /** 检查指定按键是否可以被替换 */
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

### 3.3 待输入字符

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

### 3.4 输入字

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

### 3.5 输入补全

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

### 3.6 对偶符号

```kotlin
data class PairSymbol(
    val open: String,    // 开启符号，如 (、[、"、'
    val close: String,   // 关闭符号，如 )、]、"、'
    val content: String? = null,  // 中间内容
)
```

### 3.7 剪贴板输入

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
        /** 使用正则检测文本类型 */
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

---

## 4. 线程安全设计

### 4.1 问题回顾

Java 版本的 InputList 从主线程和异步字典回调线程同时访问：

```
主线程: 用户按键 → InputList.append() → 更新 UI
异步线程: PinyinDict.lookup() → 回调 → InputList.updateCompletions()
```

这可能导致 `ConcurrentModificationException` 或数据不一致。

### 4.2 v4 解决方案

```kotlin
// KeyboardViewModel 的详细设计见文档 160 第 8.4 节
// UI 库版本：轻量桥接层，直接委托给 ImeEngine
// :app 扩展版：增加 DataStore 配置持久化和 InputConnection 输出桥接

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
                candidates = CandidateState(candidates = candidates),
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

## 5. 撤销（Revoke）机制

### 5.1 Java 版本

Java 版本通过 `InputList.Revertion` 保存状态快照：

```java
class InputList {
    static class Revertion {
        List<Input> inputs;
        int cursorIndex;
    }
}
```

### 5.2 v4 版本

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

## 6. 游标管理改进

### 6.1 Java 版本问题

- `cursorIndex` 是 inputs 列表的索引，但 GapInput 和 CharInput 混在一起
- 插入/删除后需要手动调整 cursorIndex
- 游标移出边界时行为不明确

### 6.2 v4 改进

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
        // 找到左边最近的 GapInput
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

## 7. Java 功能完整对照

| Java InputList 功能 | v4 对应 | 改进说明 |
|--------------------|---------|---------|
| `inputs` (ArrayList) | `inputs: List<InputItem>` | 不可变 List，线程安全 |
| `cursorIndex` | `gapIndex: Int` + `init` 断言 | 显式边界检查 |
| `pending` (CharInput) | `PendingInput` 独立数据类 | 关注点分离 |
| `completions` | `PendingInput.completions` | 与 pending 绑定 |
| `gapSpacing` | 始终显示 GapInput | 简化：移除 gapSpacing 开关 |
| `mathExprNested` | `InputListState.mathExprNested` | 递归结构保留 |
| `Revertion`（撤销） | `InputListEditor`（undo/redo 栈） | 标准化的撤销/重做 |
| `append()` | `appendChar()` | 返回新实例 |
| `delete()` | `deleteCharBeforeCursor()` | 返回新实例 |
| `clean()` | `clean()` | 返回新实例 |
| `commit()` | ViewModel 中处理 | 提交逻辑不属于 InputList |
| `evaluateInputViewData()` | `InputViewData` 计算 | 分离视图数据计算 |
| `evaluateGapIndex()` | `moveCursorTo()` | 显式游标操作 |
| 对偶符号处理 | `PairSymbol` 数据类 | 类型安全 |
| 剪贴板输入 | `InputClip` + `InputTextType` | 类型安全的剪贴板检测 |
| 收藏输入 | `InputFavorite` | 独立数据类 |
