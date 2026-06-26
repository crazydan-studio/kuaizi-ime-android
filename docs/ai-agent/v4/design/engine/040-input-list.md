# 输入列表

## 1. InputList 数据模型

`InputList` 是筷字输入法的核心数据结构，管理用户输入的字符序列、游标位置、待确认输入和嵌套数学表达式。它采用不可变 `data class` 设计，所有状态变更通过 `copy()` 创建新实例，从根本上保证线程安全——`StateFlow<ImeState>` 的读写是原子的，不可变 `data class` 无需同步，引擎的 `reduce` 函数在主线程串行执行状态更新。`InputList` 的四个字段各自承担独立职责：`inputs` 以交替排列的 `InputItem.Char` 与 `InputItem.Gap` 序列表达字符与游标的关系；`gapIndex` 指向当前激活的 `Gap` 位置，即游标所在处；`pending` 承载拼音输入中尚未确认的待选字符序列；`mathExprNested` 支持数学表达式中的嵌套输入列表。

```kotlin
data class InputList(
    val inputs: List<InputItem> = emptyList(),
    val gapIndex: Int = 0,
    val pending: PendingInput? = null,
    val mathExprNested: InputList? = null,
) {
    /** 当前游标位置的 Gap */
    val cursorGap: InputItem.Gap
        get() = inputs.getOrElse(gapIndex) { InputItem.Gap }

    /** 可见的字符输入列表（排除 Gap） */
    val visibleInputs: List<InputItem.Char>
        get() = inputs.filterIsInstance<InputItem.Char>()

    /** 输入文本内容 */
    val text: String
        get() = visibleInputs.joinToString("") { it.text }

    /** 是否为空 */
    val isEmpty: Boolean
        get() = inputs.all { it is InputItem.Gap }

    /** 在游标位置追加字符输入 */
    fun appendChar(char: InputItem.Char): InputList {
        val newInputs = inputs.toMutableList().apply {
            add(gapIndex, char)
            add(gapIndex + 1, InputItem.Gap)
        }
        return copy(inputs = newInputs, gapIndex = gapIndex + 2)
    }

    /** 删除游标前的一个字符 */
    fun deleteCharBeforeCursor(): InputList {
        if (gapIndex < 2) return this
        val newInputs = inputs.toMutableList().apply {
            removeAt(gapIndex - 2)
            removeAt(gapIndex - 2)
        }
        return copy(inputs = newInputs, gapIndex = gapIndex - 2)
    }

    /** 移动游标到指定位置 */
    fun moveCursorTo(newGapIndex: Int): InputList {
        val clampedIndex = newGapIndex.coerceIn(0, inputs.lastIndex)
        return copy(gapIndex = clampedIndex)
    }

    /** 清空所有输入 */
    fun clean(): InputList = InputList()

    /** 设置待确认输入 */
    fun withPending(pending: PendingInput?): InputList =
        copy(pending = pending)
}
```

`InputList` 的 `init` 块在构造时执行两项合法性断言：`gapIndex >= 0` 和 `gapIndex <= inputs.lastIndex + 1`，确保游标索引始终在合法范围内。所有状态变更方法（`appendChar`、`deleteCharBeforeCursor`、`moveCursorTo`、`clean`、`withPending`）均返回新的 `InputList` 实例，原始实例不受影响。`visibleInputs` 属性通过 `filterIsInstance<InputItem.Char>()` 过滤掉所有 `Gap`，返回纯字符序列，用于 UI 层渲染输入文本。`text` 属性是 `visibleInputs` 的便捷拼接，将所有字符的 `text` 字段连接为完整字符串。

`inputs` 列表采用 `Char-Gap-Char-Gap-...` 的交替排列模式——每个 `Char` 之后必定跟随一个 `Gap`，游标通过 `gapIndex` 指向当前激活的 `Gap`。这种设计使得插入和删除操作的语义极为清晰：插入时在 `gapIndex` 位置插入 `Char` 和新的 `Gap`，删除时移除 `gapIndex` 前方的 `Char-Gap` 对。游标移动时只需更新 `gapIndex` 指向目标 `Gap` 的索引，无需移动任何列表元素。`pending` 字段与 `inputs` 正交——`pending` 管理拼音输入过程中尚未确认的临时字符序列，确认后将 `pending` 中的字符追加到 `inputs` 中。

---

## 2. InputItem 层次结构

`InputItem` 是输入列表中元素的类型基类，以 `sealed class` 表达三种输入元素：`Char`（字符输入）、`Gap`（游标间隔）和 `MathExpr`（数学表达式）。`sealed class` 的穷举性保证引擎在模式匹配时编译期覆盖所有子类型，不会遗漏新增的输入元素类型。每个 `InputItem` 都携带唯一的 `id` 字段，用于 UI 层的 Compose key 和动画标识。

```kotlin
sealed class InputItem {
    abstract val id: String

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

        fun canReplace(key: InputKey.Char): Boolean =
            replacements.size > 1 && key.text in replacements
    }

    data object Gap : InputItem() {
        override val id = "gap"
    }

    data class MathExpr(
        override val id: String,
        val nestedList: InputList,
    ) : InputItem()
}
```

`InputItem.Char` 是最核心的子类型，承载用户输入的字符数据。`text` 字段为字符的显示文本，`keys` 记录触发该字符的按键序列（支持滑行输入中多按键组合产生一个字符的场景），`replacements` 列表存储字符的可替换文本（如标点符号的长按替换），`word` 关联该字符对应的候选词信息，`pairSymbol` 标识该字符是否为配对符号的一部分。`hasPair` 属性快速判断是否存在配对符号，`hasReplacements` 判断是否存在多个替换选项（至少两个才构成可轮换的替换列表），`nextReplacement()` 方法实现替换轮换逻辑——从当前文本循环到替换列表中的下一个选项，`canReplace()` 方法判断指定按键是否可以触发替换操作。

`InputItem.Gap` 使用 `data object` 定义，表示输入列表中的游标间隔位置。所有 `Gap` 实例共享同一身份，`id` 固定为 `"gap"`。`Gap` 不是用户可见的输入内容，而是 `InputList` 内部的游标标记机制——通过 `gapIndex` 指向特定 `Gap` 即可确定游标位置。这种设计避免了传统的「索引偏移」游标模型在插入删除时需要维护偏移量的复杂性，每个 `Gap` 都是列表中的一个一等公民元素。

`InputItem.MathExpr` 支持数学表达式的嵌套输入，内部持有一个完整的 `InputList` 实例（`nestedList`），实现递归的输入列表结构。数学表达式的输入操作（追加字符、删除字符、移动游标）在嵌套列表上独立执行，与外层输入列表互不干扰。嵌套列表的 `pending` 和 `mathExprNested` 字段在数学表达式场景下通常为 `null`，避免无限递归。

---

## 3. PendingInput 待确认输入

`PendingInput` 表示拼音输入过程中尚未确认的临时字符序列。它与 `InputList.inputs` 的核心区别在于语义：`inputs` 中的 `Char` 是已确认的输入，而 `pending` 中的 `Char` 是用户正在输入但尚未选择具体汉字的拼音字符。拼音输入的核心交互模式是：用户逐键输入拼音字母 → 引擎根据拼音序列查询候选词 → 用户从候选列表中选择 → 选中结果追加到 `inputs` 中，同时清空 `pending`。`PendingInput` 承载的是这个「输入拼音 → 选择汉字」中间态的数据。

```kotlin
data class PendingInput(
    val chars: List<InputItem.Char>,
    val completions: List<InputCompletion> = emptyList(),
    val pinyinToggles: Set<PinyinToggleType> = emptySet(),
)
```

`chars` 字段为待确认的拼音字符列表，每个 `InputItem.Char` 的 `text` 为一个拼音字母，`keys` 记录触发该字母的按键。`chars` 列表的 `text` 拼接后即为当前的完整拼音串，引擎以此查询 `ImeDictProvider` 获取候选词。`completions` 字段为输入补全列表，由字典查询结果填充——当用户输入部分拼音时，字典可能返回词组补全建议，如用户输入 `"zh"` 时补全 `"zhong"` 对应的词组。`pinyinToggles` 字段为拼音切换类型集合，支持用户在输入过程中切换拼音的显示模式（如全拼↔双拼、显示声调等）。

`PendingInput` 的生命周期由 `KeyboardStateMachine` 的状态转换控制：进入拼音输入状态（`PinyinInput.Waiting`）时创建 `PendingInput`，用户逐键输入时追加 `Char` 到 `chars` 中，用户选择候选词时将选中结果追加到 `InputList.inputs` 并清空 `pending`，用户取消输入时直接丢弃 `PendingInput`。`PendingInput` 的不可变性确保了状态转换的可追踪性——每次按键产生新的 `PendingInput` 实例，旧实例保留在 `InputListEditor` 的撤销栈中，支持撤销操作。

`PendingInput` 与 `InputList` 的关联通过 `InputList.withPending()` 方法建立，返回包含新 `pending` 的 `InputList` 实例。引擎在 `reduce` 函数中根据 `ImeIntent.PressKey` 更新 `pending`：若当前 `pending` 为 `null`，创建新的 `PendingInput` 并追加首个字符；若当前 `pending` 非空，向 `chars` 追加新字符并更新 `completions`。

---

## 4. InputCompletion 补全

`InputCompletion` 是输入补全的类型基类，以 `sealed class` 表达两种补全来源：`LatinWord`（拉丁词补全）和 `PhraseWord`（拼音词组补全）。补全机制在用户输入过程中提供前瞻性的文本建议，减少完整输入所需的按键次数。所有 `InputCompletion` 子类共享 `text` 字段作为补全的完整文本，各自携带类型特有的元数据。

```kotlin
sealed class InputCompletion {
    abstract val text: String

    data class LatinWord(
        override val text: String,
        val remaining: String,
    ) : InputCompletion()

    data class PhraseWord(
        override val text: String,
        val remaining: String,
        val spells: List<String>,
    ) : InputCompletion()
}
```

`LatinWord` 是拉丁字母词汇的补全建议，适用于英文输入和拉丁字符场景。`text` 为补全的完整词汇，`remaining` 为用户尚未输入的剩余部分。例如用户输入 `"hel"` 时，`LatinWord` 的 `text` 为 `"hello"`，`remaining` 为 `"lo"`。`remaining` 的用途是为 UI 层提供差异化的显示——已输入部分和待补全部分可以用不同的视觉样式区分，帮助用户快速识别补全建议的内容。`LatinWord` 的来源是 `UserInputDao` 的 `getCompletions()` 查询，基于用户历史输入频率排序。

`PhraseWord` 是拼音词组的补全建议，适用于中文拼音输入场景。`text` 为补全的中文词组文本（如 `"中国"`），`remaining` 为用户尚未输入的拼音部分（如 `"ong"`），`spells` 为词组各字的拼音拼写列表（如 `["zhong", "guo"]`）。`spells` 字段使得 UI 层可以展示词组的拼音分解，帮助用户理解补全建议的来源。`PhraseWord` 的来源是 `DictRepository` 的 `lookupPinyinPhrases()` 查询，基于字典频率和 HMM 预测排序。

补全的选中逻辑由 `KeyboardIntentHandler` 处理：用户在候选列表中选择补全建议时，引擎将补全的 `text` 作为 `InputItem.Char` 追加到 `InputList.inputs` 中，同时清空 `pending`。若选中 `PhraseWord` 补全，引擎还会将词组的拼音拼写记录到用户输入历史中，通过 `ImeDictProvider.recordInput()` 更新用户词频，提高后续查询的准确性。

---

## 5. PairSymbol 配对符号

`PairSymbol` 描述配对符号的结构信息，用于自动补全括号、引号等成对符号。配对符号是 IME 提升输入效率的重要功能——用户输入左符号时自动插入右符号，并将游标置于两者之间，减少手动补全的操作。`PairSymbol` 作为 `InputItem.Char` 的可选字段存在，`pairSymbol` 为 `null` 表示该字符不是配对符号。

```kotlin
data class PairSymbol(
    val open: String,
    val close: String,
    val content: String? = null,
)
```

`open` 字段为配对符号的左半部分（如 `(`、`[`、`"`、`'`），`close` 字段为右半部分（如 `)`、`]`、`"`、`'`），`content` 字段为左右符号之间的可选内容。`content` 为 `null` 表示配对符号内部为空——用户输入左符号后，引擎通过 `EditorAction.InsertPairedSymbols` 输出左右符号并将游标置于两者之间。`content` 非空表示配对符号包裹了已有文本——用户选中文本后输入左符号，引擎将选中内容包裹在配对符号内。

配对符号的插入流程由 `ImeEngine` 的 `reduce` 函数处理：当 `InputItem.Char` 的 `hasPair` 为 `true` 时，引擎在追加 `Char` 到 `InputList` 后，额外产生一个 `EditorAction.InsertPairedSymbols` 输出，通过 `ImeEditorBridge.insertPairedSymbols(left, right)` 在目标编辑器中插入成对符号。若目标编辑器中存在选中文本，`ImeEditorBridge` 实现将选中文本包裹在左右符号之间；若无选中文本，插入左右符号并将游标置于两者之间。

`PairSymbol` 的常见配置包括：圆括号 `(` `)`、方括号 `[` `]`、花括号 `{` `}`、双引号 `"` `"`、单引号 `'` `'`、书名号 `《` `》`、双书名号 `〈` `〉` 等。中英文标点符号的配对规则不同——中文标点使用全角字符，英文标点使用半角字符，引擎根据当前键盘类型和 `ImeConfig` 的标点配置选择合适的配对字符集。配对符号的插入不受 `InputListEditor` 撤销机制的单快照约束——`BaseImeEditorBridge` 在插入配对符号时调用 `resetRevertion()` 清空撤销快照，因为配对符号涉及两个插入点，无法简单地通过单次撤销恢复。

---

## 6. InputListEditor 撤销/重做

`InputListEditor` 提供输入列表的撤销和重做能力，采用 `ArrayDeque` 双端队列实现两个有界栈：`undoStack` 记录可撤销的历史状态，`redoStack` 记录可重做的未来状态。栈的最大容量为 50，采用 FIFO 淘汰策略——超出上限时淘汰最旧条目（`removeFirst()`），确保栈大小始终不超过上限。撤销/重做机制利用 `InputList` 的不可变性——历史状态无需深拷贝，直接保存引用即可，内存开销极低。

```kotlin
class InputListEditor {
    private val undoStack = ArrayDeque<InputList>(50)  // initialCapacity
    private val redoStack = ArrayDeque<InputList>(50)  // initialCapacity
    private val maxStackSize = 50

    val canUndo: Boolean get() = undoStack.isNotEmpty()
    val canRedo: Boolean get() = redoStack.isNotEmpty()

    /** 保存快照到撤销栈，超出上限时淘汰最旧条目 */
    fun pushUndo(list: InputList) {
        if (undoStack.size >= maxStackSize) {
            undoStack.removeFirst()
        }
        undoStack.addLast(list)
        redoStack.clear()  // 新操作清空重做栈
    }

    /** 保存快照到重做栈，超出上限时淘汰最旧条目 */
    private fun pushRedo(list: InputList) {
        if (redoStack.size >= maxStackSize) {
            redoStack.removeFirst()
        }
        redoStack.addLast(list)
    }

    /** 撤销：恢复到上一个状态 */
    fun undo(current: InputList): InputList {
        val previous = undoStack.removeLastOrNull() ?: return current
        pushRedo(current)
        return previous
    }

    /** 重做：前进到下一个状态 */
    fun redo(current: InputList): InputList {
        val next = redoStack.removeLastOrNull() ?: return current
        pushUndo(current)
        return next
    }
}
```

`pushUndo()` 在每次 `InputList` 变更前调用，将当前状态推入 `undoStack`，同时清空 `redoStack`——新变更使得重做历史失效。`undo()` 从 `undoStack` 弹出最近的状态替换当前状态，并将当前状态推入 `redoStack`，实现双向导航。`redo()` 从 `redoStack` 弹出最近的状态替换当前状态，并将当前状态推入 `undoStack`。`canUndo` 和 `canRedo` 属性供 UI 层的撤销/重做工具按钮判断是否启用。

`InputListEditor` 由 `InputListOperator` 内部持有和管理，外部不直接操作编辑器。`InputListOperator` 在每次执行输入列表变更操作时自动调用 `pushUndo()`，确保撤销栈与实际状态变更同步。撤销和重做操作通过 `ImeIntent.PerformEdit(EditorEditAction.UNDO)` 和 `ImeIntent.PerformEdit(EditorEditAction.REDO)` 触发，引擎在 `reduce` 函数中委托 `InputListOperator` 执行对应的撤销/重做逻辑。

撤销栈和重做栈的最大容量均为 50，采用手动 FIFO 淘汰策略：`pushUndo()` / `pushRedo()` 在栈大小达到上限时调用 `removeFirst()` 移除最旧条目，再添加新条目。这一设计确保了栈大小始终不超过上限，同时淘汰行为显式可控。容量选择在内存占用和撤销深度之间取得平衡：50 个不可变 `InputList` 实例的内存占用通常在 KB 级别，而 50 层撤销深度覆盖了绝大多数用户的使用场景。

---

## 7. InputListOperator 操作器

`InputListOperator` 是输入列表的集中操作器，封装所有 `InputList` 变更逻辑，提供线程安全的操作方法。它内部持有 `InputListEditor` 管理撤销/重做，确保每次变更操作自动维护撤销栈的一致性。`InputListOperator` 是 `ImeEngine` 处理输入列表相关 `ImeIntent` 的唯一委托对象，所有 `InputList` 的变更都通过操作器执行，避免了散落在各处的直接状态修改。

```kotlin
class InputListOperator(
    private val editor: InputListEditor = InputListEditor(),
) {
    val canUndo: Boolean get() = editor.canUndo
    val canRedo: Boolean get() = editor.canRedo

    /** 追加字符到输入列表 */
    fun appendChar(list: InputList, char: InputItem.Char): InputList {
        editor.pushUndo(list)
        return list.appendChar(char)
    }

    /** 删除游标前的字符 */
    fun deleteChar(list: InputList): InputList {
        editor.pushUndo(list)
        return list.deleteCharBeforeCursor()
    }

    /** 确认待输入字符（将 pending 追加到 inputs） */
    fun commitPending(list: InputList): InputList {
        val pending = list.pending ?: return list
        editor.pushUndo(list)
        var result = list.copy(pending = null)
        for (char in pending.chars) {
            result = result.appendChar(char)
        }
        return result
    }

    /** 移动游标 */
    fun moveCursor(list: InputList, newGapIndex: Int): InputList {
        return list.moveCursorTo(newGapIndex)
    }

    /** 清空输入列表 */
    fun clean(list: InputList): InputList {
        if (list.isEmpty) return list
        editor.pushUndo(list)
        return list.clean()
    }

    /** 设置待确认输入 */
    fun withPending(list: InputList, pending: PendingInput?): InputList {
        return list.withPending(pending)
    }

    /** 撤销 */
    fun undo(current: InputList): InputList = editor.undo(current)

    /** 重做 */
    fun redo(current: InputList): InputList = editor.redo(current)
}
```

`InputListOperator` 的每个变更方法在执行实际操作前先调用 `editor.pushUndo(list)` 保存当前状态，确保撤销栈与状态变更同步。游标移动操作（`moveCursor`）不推入撤销栈，因为它不改变输入内容，仅改变视觉位置——用户通常不期望撤销一次游标移动。`commitPending` 方法将 `pending` 中的字符逐一追加到 `inputs` 中，然后清空 `pending`，完成拼音输入到确认输入的转换。

`InputListOperator` 作为 `ImeEngine` 的构造参数注入，引擎在 `reduce` 函数中根据 `ImeIntent` 类型调用对应的操作方法。操作器的集中化设计带来三个好处：一是撤销/重做逻辑与状态变更逻辑内聚在一起，避免遗漏 `pushUndo` 调用；二是操作方法提供统一的变更入口，便于添加横切关注点（如变更日志、性能监控）；三是操作器可替换——测试时注入空操作器跳过撤销逻辑，或注入自定义操作器扩展变更行为。

---

## 8. 间距规则

`InputList` 的 `inputs` 列表采用 `Char-Gap` 交替排列模式，但并非所有 `Char` 之间都需要插入 `Gap`。间距规则定义了不同字符类型之间的 `Gap` 插入策略，影响游标可以停留的位置。合理的间距规则使得游标仅在语义上有意义的位置停留，避免在无意义的字符间隙中放置游标（如英文单词内部、数字序列中间等），提升用户的游标操作效率。

### 8.1 间距判断逻辑

```kotlin
object InputGapSpacing {
    /**
     * 判断两个相邻 Char 之间是否需要插入 Gap。
     * 返回 true 表示需要间隔（游标可停留），false 表示紧密连接。
     */
    fun needsGap(left: InputItem.Char, right: InputItem.Char): Boolean {
        // 配对符号内部不需要间隔
        if (left.pairSymbol != null && left.pairSymbol.content == null) return false
        // 同一拼音词组的字符不需要间隔
        if (left.word is InputWord.PinyinPhrase && left.word == right.word) return false
        // 英文单词内部不需要间隔
        if (isLatinChar(left) && isLatinChar(right)) return false
        // 数字序列内部不需要间隔
        if (isDigitChar(left) && isDigitChar(right)) return false
        // 其他情况均需间隔
        return true
    }

    private fun isLatinChar(char: InputItem.Char): Boolean =
        char.word is InputWord.Latin || char.text.all { it.isLetter() && it.code < 128 }

    private fun isDigitChar(char: InputItem.Char): Boolean =
        char.text.all { it.isDigit() }
}
```

### 8.2 间距规则详述

间距规则的核心原则是：**游标仅在语义边界处停留**。具体规则如下：

1. **拼音字之间**：需要间隔。每个拼音字是独立的语义单元，用户可能需要在两个字之间插入新字符或删除某个字。例如输入 `"你好"` 时，`"你"` 和 `"好"` 之间有 `Gap`，游标可以停留在两者之间。

2. **拼音词组内部**：不需要间隔。词组是一个不可分割的语义单元，用户不会在词组内部插入或删除单个字。例如输入 `"中国"` 作为词组候选时，`"中"` 和 `"国"` 之间没有 `Gap`，游标无法停留在两者之间——删除操作删除整个词组，而非单个字。

3. **英文单词内部**：不需要间隔。英文字母构成单词，单词内部不应出现游标停留点。例如输入 `"hello"` 时，字母之间没有 `Gap`，游标只能在单词首尾停留。

4. **数字序列内部**：不需要间隔。连续的数字构成一个数值，数值内部不应出现游标停留点。例如输入 `"123"` 时，数字之间没有 `Gap`。

5. **配对符号内部（无内容）**：不需要间隔。当用户输入左符号后，左右符号之间自动插入的空位由 `ImeEditorBridge.insertPairedSymbols()` 管理，不需要 `InputList` 层面的 `Gap`。

6. **异种字符之间**：需要间隔。不同类型的字符之间（如中文字与英文字母、数字与标点符号等）始终插入 `Gap`，因为异种字符之间存在语义边界，用户可能需要在此位置进行编辑操作。

间距规则在 `InputList.appendChar()` 方法中应用：追加新字符时，调用 `InputGapSpacing.needsGap()` 判断新字符与前一个字符之间是否需要插入 `Gap`。若需要间隔，插入 `Char-Gap` 对；若不需要间隔，仅插入 `Char`，不插入 `Gap`。这种条件插入策略使得 `inputs` 列表的长度和结构随字符类型动态调整，游标位置始终与语义边界对齐。
