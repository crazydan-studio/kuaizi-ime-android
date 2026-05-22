# 意图、输出与桥接

## 1. ImeIntent 用户意图体系

`ImeIntent` 是用户操作的统一编码，以 `sealed class` 表达所有可能的用户意图。所有用户操作——按键、选择候选、切换键盘、提交输入、编辑操作、剪贴板粘贴、收藏管理、配置变更、数据导入导出——均编码为 `ImeIntent` 的子类型，由 `ImeEngine.handleIntent()` 接收并处理。`ImeIntent` 是 MVI 架构中 Intent 层的核心类型，是引擎与 UI 之间的输入契约。

```kotlin
sealed class ImeIntent {
    /** 按键意图：用户按下某个按键，携带按键信息和手势类型 */
    data class PressKey(val key: InputKey, val gesture: KeyGesture) : ImeIntent()

    /** 长按意图：用户长按某个按键 */
    data class LongPressKey(val key: InputKey) : ImeIntent()

    /** 选择候选词意图：用户从候选列表中选择一个候选词 */
    data class SelectCandidate(val candidate: InputWord) : ImeIntent()

    /** 翻页候选意图：用户翻页候选列表 */
    data class PageCandidate(val direction: PageDirection) : ImeIntent()

    /** 切换键盘意图：用户切换到指定类型的键盘 */
    data class SwitchKeyboard(val type: KeyboardType) : ImeIntent()

    /** 提交输入意图：用户确认当前输入 */
    data object CommitInput : ImeIntent()

    /** 删除输入意图：用户删除当前输入 */
    data object DeleteInput : ImeIntent()

    /** 清空输入意图：用户清空所有输入 */
    data object CleanInput : ImeIntent()

    /** 移动游标意图：用户将游标移动到指定位置 */
    data class MoveCursorTo(val index: Int) : ImeIntent()

    /** 执行编辑动作意图：用户触发编辑操作（全选、复制、剪切、粘贴、撤销、重做等） */
    data class PerformEdit(val action: EditorEditAction) : ImeIntent()

    /** 粘贴剪贴板意图：用户粘贴剪贴板内容 */
    data class PasteClip(val text: String) : ImeIntent()

    /** 保存收藏意图：用户保存一条收藏 */
    data class SaveFavorite(val favorite: InputFavorite) : ImeIntent()

    /** 更新配置意图：用户修改运行时配置 */
    data class UpdateConfig(val config: ImeConfig) : ImeIntent()

    /** 导出用户数据意图：用户导出输入历史和收藏数据 */
    data object ExportUserData : ImeIntent()

    /** 导入用户数据意图：用户从指定文件导入数据 */
    data class ImportUserData(val filePath: String) : ImeIntent()
}
```

`ImeIntent` 的设计遵循「意图与手势分离」原则：`ImeIntent` 表达业务语义（如「提交输入」），而非底层手势细节（如「手指抬起」）。手势到意图的映射由 `KeyboardViewModel` 完成，引擎只消费意图，不感知手势。这种分层使得引擎可以在不同的输入模式下复用相同的意图处理逻辑——无论是真实手指操作还是 `InputAction` 的程序化回放，最终都通过 `ImeIntent` 驱动引擎。

### 1.1 按键意图

`PressKey` 是最核心的意图类型，携带 `InputKey`（按键信息）和 `KeyGesture`（手势类型）两个参数。`InputKey` 的子类型包括 `InputKey.Char`（字符按键）、`InputKey.Func`（功能按键）和 `InputKey.Switch`（切换按键），每种按键在引擎中有不同的处理逻辑。`KeyGesture` 的枚举值包括 `Tap`（点击）、`Slip`（滑行）、`Flip`（翻动）和 `Hold`（长按），手势类型影响按键的处理路径——例如 `Slip` 手势触发滑行输入逻辑，`Flip` 手势触发翻动输入逻辑。

`LongPressKey` 是长按意图，独立于 `PressKey` 的 `Hold` 手势存在。长按意图的处理逻辑通常是弹出上下文菜单（如标点符号的长按替换菜单），与 `PressKey` 的处理路径完全不同。分离为独立意图类型使得引擎可以根据意图类型选择不同的状态转换路径，避免在同一个处理分支中通过 `gesture` 参数分支逻辑。

### 1.2 候选意图

`SelectCandidate` 和 `PageCandidate` 是候选相关的两种意图。`SelectCandidate` 携带用户选中的 `InputWord` 实例，引擎收到后执行候选词确认逻辑：将候选词追加到 `InputList.inputs` 中，清空 `pending`，通过 `ImeOutputBridge.commitText()` 输出文本到目标编辑器，通过 `ImeDictProvider.recordInput()` 更新用户词频。`PageCandidate` 携带翻页方向（`PageDirection.Forward` 或 `PageDirection.Backward`），引擎收到后更新 `CandidateList.pageIndex`，不触发字典查询。

### 1.3 输入列表意图

`CommitInput`、`DeleteInput`、`CleanInput` 和 `MoveCursorTo` 是输入列表的四种操作意图。`CommitInput` 确认当前 `pending` 中的待选字符，将选中内容追加到 `inputs` 中并输出到目标编辑器。`DeleteInput` 撤销最近一次确认输入，通过 `ImeOutputBridge.revokeCommit()` 恢复编辑器文本。`CleanInput` 清空整个输入列表，重置 `InputList` 为初始状态。`MoveCursorTo` 移动游标到指定位置，仅更新 `gapIndex`，不触发输出操作。

### 1.4 编辑动作意图

`PerformEdit` 携带 `EditorEditAction` 枚举值，触发目标编辑器的编辑操作。`EditorEditAction` 包含 `BACKSPACE`、`SELECT_ALL`、`COPY`、`CUT`、`PASTE`、`UNDO`、`REDO` 七种动作。引擎收到 `PerformEdit` 后，根据 `EditorEditAction` 类型执行对应的操作：破坏性编辑动作（`BACKSPACE`、`CUT`、`PASTE`、`UNDO`、`REDO`）清空 `BaseImeOutputBridge` 的撤销快照，非破坏性动作（`SELECT_ALL`、`COPY`）不影响撤销快照。收藏功能独立于编辑操作，通过 `ImeIntent.SaveFavorite` 触发，不属于 `EditorEditAction`。

### 1.5 剪贴板、收藏与配置意图

`PasteClip` 携带剪贴板文本内容，引擎收到后将文本追加到 `InputList.inputs` 中并输出到目标编辑器。`PasteClip` 受 `Feature.Clipboard` 门控——禁用时调用立即抛出 `IllegalStateException`。`SaveFavorite` 携带收藏条目，引擎收到后通过 `FavoriteService` 保存收藏，受 `Feature.Favorites` 门控。`UpdateConfig` 携带新的 `ImeConfig` 实例，引擎收到后更新运行时配置，同步更新 `FeatureRegistry` 的门控状态。`ExportUserData` 和 `ImportUserData` 是数据导入导出意图，引擎收到后委托 `DictRepository` 执行数据的序列化和反序列化。

---

## 2. ImeOutput 编辑输出体系

`ImeOutput` 是引擎向目标编辑器输出的编辑指令，以 `sealed class` 表达所有可能的输出类型。与 `ImeIntent` 表达用户意图不同，`ImeOutput` 表达引擎对编辑器的操作指令——提交文本、撤销提交、插入配对符号、移动光标、扩展选区、执行编辑动作。`ImeOutput` 由引擎的 `reduce` 函数在处理 `ImeIntent` 时产生，通过 `dispatchToTarget()` 统一分发到 `ImeOutputBridge`。`sealed class` 的穷举性确保引擎在分发输出时编译期覆盖所有类型。

```kotlin
sealed class ImeOutput {
    abstract val timestamp: Long

    /** 提交文本：将文本插入到当前光标位置，支持替换轮换 */
    data class CommitText(
        override val timestamp: Long,
        val text: String,
        val replacements: List<String>? = null,
    ) : ImeOutput()

    /** 撤销提交：撤销最近一次可撤回的输入 */
    data class RevokeCommit(
        override val timestamp: Long,
    ) : ImeOutput()

    /** 插入配对符号：在当前光标位置插入左右配对符号 */
    data class InsertPairedSymbols(
        override val timestamp: Long,
        val left: String,
        val right: String,
    ) : ImeOutput()

    /** 移动光标：按指定方向移动光标 */
    data class MoveCursor(
        override val timestamp: Long,
        val direction: CursorDirection,
    ) : ImeOutput()

    /** 扩展选区：按指定方向扩展选区 */
    data class SelectRange(
        override val timestamp: Long,
        val direction: CursorDirection,
    ) : ImeOutput()

    /** 执行编辑动作：触发目标编辑器的编辑操作 */
    data class PerformEdit(
        override val timestamp: Long,
        val action: EditorEditAction,
    ) : ImeOutput()
}
```

### 2.1 CommitText 提交文本

`CommitText` 是最常见的输出类型，在用户确认拼音输入、选择候选词、粘贴剪贴板内容等场景下产生。`text` 为提交的文本内容，`replacements` 为可选的替换列表——当 `replacements` 非空时，`ImeOutputBridge` 实现需检查光标前文本是否匹配替换列表中的某一项，匹配时执行替换而非插入。替换轮换机制用于直输模式下的标点符号双击轮换——用户双击同一个按键时，引擎输出 `CommitText` 并携带替换列表，桥梁实现自动轮换光标前的文本。

`replacements` 的典型场景：用户输入英文句号 `.` 后双击同一按键，引擎输出 `CommitText(text = "。", replacements = [".", "。", "…"])`，桥梁实现检查光标前文本是否为 `"."` 或 `"。"`，若是则替换为下一个轮换项；若不匹配任何替换项，则正常插入 `text`。默认的替换轮换采用双击按键方式触发——用户在短时间内连续按两次同一个按键时，引擎输出携带 `replacements` 的 `CommitText`，桥梁实现自动轮换光标前的文本。这种设计将替换逻辑从引擎中解耦——引擎只负责提供替换列表，桥梁实现负责检测和执行替换操作，使得不同平台可以按自身能力提供差异化的替换实现。

### 2.2 RevokeCommit 撤销提交

`RevokeCommit` 在用户执行 `ImeIntent.DeleteInput` 时产生，触发 `ImeOutputBridge.revokeCommit()` 恢复编辑器文本到最近一次提交前的状态。撤销机制采用单快照设计（而非栈），反映 IME 的使用模式——用户通常只需撤销最近一次输入。`RevokeCommit` 的处理由 `BaseImeOutputBridge` 的 `revertion` 快照驱动，桥梁实现从快照中恢复选区位置和文本内容。

### 2.3 InsertPairedSymbols 插入配对符号

`InsertPairedSymbols` 在用户输入配对符号的左半部分时产生，触发 `ImeOutputBridge.insertPairedSymbols(left, right)` 在目标编辑器中插入成对符号。若编辑器中存在选中文本，桥梁实现将选中文本包裹在左右符号之间；若无选中文本，插入左右符号并将光标置于两者之间。配对符号的插入清空撤销快照，因为涉及两个插入点，无法通过单次撤销恢复。

### 2.4 MoveCursor 与 SelectRange

`MoveCursor` 和 `SelectRange` 分别触发光标移动和选区扩展操作，携带 `CursorDirection` 参数指定方向。`CursorDirection` 枚举包含 `Left`、`Right`、`Up`、`Down` 四个方向。这两种输出类型在用户通过键盘工具栏或编辑操作意图触发光标操作时产生，桥梁实现将方向参数映射为平台特定的光标操作 API 调用。

### 2.5 PerformEdit 执行编辑动作

`PerformEdit` 携带 `EditorEditAction` 枚举值，触发目标编辑器的编辑操作。桥梁实现根据 `EditorEditAction` 类型调用平台特定的编辑 API——例如在 `InputConnectionBridge` 中，`SELECT_ALL` 调用 `InputConnection.performContextMenuAction(android.R.id.selectAll)`，`COPY` 调用 `InputConnection.performContextMenuAction(android.R.id.copy)`。破坏性编辑动作（`BACKSPACE`、`CUT`、`PASTE`、`UNDO`、`REDO`）清空撤销快照，非破坏性动作（`SELECT_ALL`、`COPY`）不影响快照。

---

## 3. ImeOutputBridge 输出桥接接口

`ImeOutputBridge` 是引擎与目标编辑器之间的桥梁接口，采用桥接模式实现输出目标与引擎的解耦。引擎内部仅执行一次 `when(ImeOutput)` 穷举分发，桥梁实现者只需实现语义方法，无需理解 `ImeOutput` 类型体系。接口方法表达「做什么」（语义）而非「怎么做」（实现），不同平台和目标编辑器可以按自身能力提供差异化的实现。

```kotlin
interface ImeOutputBridge {
    /**
     * 提交文本到当前光标位置。
     * 若 replacements 非空，桥梁需检查光标前文本是否在列表中：
     * - 匹配：替换光标前字符（替换轮换）
     * - 不匹配：正常插入文本
     */
    fun commitText(text: String, replacements: List<String>? = null)

    /** 撤销最近一次可撤回输入 */
    fun revokeCommit()

    /** 插入成对符号 */
    fun insertPairedSymbols(left: String, right: String)

    /** 移动光标 */
    fun moveCursor(direction: CursorDirection)

    /** 扩展选区 */
    fun selectRange(direction: CursorDirection)

    /** 执行编辑动作（全选、复制、剪切、粘贴等） */
    fun performEdit(action: EditorEditAction)

    /** 实时获取目标当前文本 */
    fun getText(): CharSequence

    /** 实时获取目标当前选区 */
    fun getSelection(): TextRange
}
```

接口的七个方法覆盖了 IME 对目标编辑器的全部操作：`commitText()` 提交文本（支持替换轮换），`revokeCommit()` 撤销最近输入，`insertPairedSymbols()` 插入配对符号，`moveCursor()` 移动光标，`selectRange()` 扩展选区，`performEdit()` 执行编辑动作，`getText()` 和 `getSelection()` 读取编辑器状态。所有方法在主线程调用以保证线程安全。`getText()` 和 `getSelection()` 是桥梁实现从目标编辑器读取状态的唯二方法，引擎在需要获取编辑器当前文本和选区信息时调用这两个方法。

### 3.1 BaseImeOutputBridge 抽象基类

`BaseImeOutputBridge` 提供单快照撤销机制的抽象基类，减少子类的实现负担。撤销机制采用单快照设计（而非栈），反映了 IME 的使用模式——用户通常只需撤销最近一次输入。快照记录提交前后的选区状态，使得撤销时可以精确恢复到操作前的文本和光标位置。

```kotlin
abstract class BaseImeOutputBridge : ImeOutputBridge {

    private data class SelectionSnapshot(
        val beforeStart: Int,
        val beforeEnd: Int,
        val beforeContent: String,
        val afterStart: Int,
        val afterEnd: Int,
    )

    private var revertion: SelectionSnapshot? = null

    protected fun resetRevertion() {
        revertion = null
    }

    protected fun recordRevertion(
        beforeStart: Int, beforeEnd: Int, beforeContent: String,
        afterStart: Int, afterEnd: Int,
    ) {
        revertion = SelectionSnapshot(
            beforeStart = beforeStart,
            beforeEnd = beforeEnd,
            beforeContent = beforeContent,
            afterStart = afterStart,
            afterEnd = afterEnd,
        )
    }

    override fun revokeCommit() {
        val snapshot = revertion ?: return
        revertion = null
        onRevokeCommit(snapshot)
    }

    protected abstract fun onRevokeCommit(snapshot: SelectionSnapshot)

    override fun commitText(text: String, replacements: List<String>?) {
        resetRevertion()
        if (replacements != null && replacements.isNotEmpty()) {
            doReplaceableCommitText(text, replacements)
        } else {
            doNormalCommitText(text)
        }
    }

    protected abstract fun doReplaceableCommitText(text: String, replacements: List<String>)
    protected abstract fun doNormalCommitText(text: String)
}
```

快照重置规则确保撤销状态与编辑器状态保持一致：新的可撤回输入开始前先清空旧快照，提交后记录新快照；撤销完成后清空已消费的快照；配对符号提交清空快照（不可撤回）；有编辑副作用的动作（退格、粘贴、剪切、撤销、重做）清空快照；无编辑副作用的动作（全选、复制）不影响快照。子类在各接口方法中按上述规则调用 `resetRevertion()` 和 `recordRevertion()`，确保撤销状态的正确性。`commitText()` 方法在基类中实现了分发逻辑——根据 `replacements` 是否为空选择 `doReplaceableCommitText()` 或 `doNormalCommitText()`，子类只需实现这两个具体的提交方法。

---

## 4. InputConnectionBridge 系统输入连接

`InputConnectionBridge` 是面向 Android 系统 `InputConnection` 的桥梁实现，由 `:app` 模块中的 `IMEService` 创建和管理。它使用 supplier 模式在每次操作时获取当前 `InputConnection`——这是因为 `InputConnection` 的生命周期由系统管理，IME 服务无法持有稳定的引用，目标编辑器切换时 `InputConnection` 实例会变更。supplier 模式确保每次操作都使用最新的 `InputConnection` 实例。

```kotlin
class InputConnectionBridge(
    private val targetSupplier: () -> InputConnection?
) : BaseImeOutputBridge() {

    override fun doNormalCommitText(text: String) {
        resetRevertion()
        val ic = targetSupplier() ?: return
        val beforeSel = getSelection()
        val beforeText = getText()
        val beforeStart = beforeSel.start.coerceAtLeast(0)
        val beforeEnd = beforeSel.end.coerceAtLeast(0).coerceAtMost(beforeText.length)
        val beforeContent = beforeText.substring(beforeStart, beforeEnd)
        ic.commitText(text, 1)
        val afterSel = getSelection()
        recordRevertion(
            beforeStart = beforeStart,
            beforeEnd = beforeEnd,
            beforeContent = beforeContent,
            afterStart = afterSel.start.coerceAtLeast(0),
            afterEnd = afterSel.end.coerceAtLeast(0),
        )
    }

    override fun doReplaceableCommitText(text: String, replacements: List<String>) {
        resetRevertion()
        val ic = targetSupplier() ?: return
        val textBeforeCursor = ic.getTextBeforeCursor(text.length, 0)?.toString()
        if (textBeforeCursor != null && replacements.contains(textBeforeCursor)) {
            val replaceStart = getSelection().start - text.length
            val beforeContent = textBeforeCursor
            ic.deleteSurroundingText(text.length, 0)
            ic.commitText(text, 1)
            val afterSel = getSelection()
            recordRevertion(
                beforeStart = replaceStart,
                beforeEnd = getSelection().start,
                beforeContent = beforeContent,
                afterStart = afterSel.start.coerceAtLeast(0),
                afterEnd = afterSel.end.coerceAtLeast(0),
            )
        } else {
            doNormalCommitText(text)
        }
    }

    override fun insertPairedSymbols(left: String, right: String) {
        resetRevertion()
        val ic = targetSupplier() ?: return
        val sel = getSelection()
        val currentText = getText()
        if (sel.start != sel.end) {
            val selected = currentText.substring(sel.start, sel.end)
            ic.commitText(left + selected + right, 1)
        } else {
            ic.commitText(left + right, 1)
            ic.setSelection(sel.start + left.length, sel.start + left.length)
        }
    }

    override fun moveCursor(direction: CursorDirection) {
        val ic = targetSupplier() ?: return
        when (direction) {
            CursorDirection.Left -> ic.sendKeyEvent(
                KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_LEFT)
            )
            CursorDirection.Right -> ic.sendKeyEvent(
                KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_RIGHT)
            )
            CursorDirection.Up, CursorDirection.Down -> { /* 多行编辑器支持 */ }
        }
    }

    override fun selectRange(direction: CursorDirection) {
        val ic = targetSupplier() ?: return
        when (direction) {
            CursorDirection.Left -> ic.sendKeyEvent(
                KeyEvent(0, KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.META_SHIFT_ON)
            )
            CursorDirection.Right -> ic.sendKeyEvent(
                KeyEvent(0, KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.META_SHIFT_ON)
            )
            CursorDirection.Up, CursorDirection.Down -> { /* 多行编辑器支持 */ }
        }
    }

    override fun performEdit(action: EditorEditAction) {
        if (action.hasEditorEffect) resetRevertion()
        val ic = targetSupplier() ?: return
        when (action) {
            EditorEditAction.BACKSPACE -> {
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL))
            }
            EditorEditAction.SELECT_ALL -> ic.performContextMenuAction(android.R.id.selectAll)
            EditorEditAction.COPY -> ic.performContextMenuAction(android.R.id.copy)
            EditorEditAction.PASTE -> ic.performContextMenuAction(android.R.id.paste)
            EditorEditAction.CUT -> ic.performContextMenuAction(android.R.id.cut)
            EditorEditAction.UNDO -> ic.performContextMenuAction(android.R.id.undo)
            EditorEditAction.REDO -> ic.performContextMenuAction(android.R.id.redo)
        }
    }

    override fun getText(): CharSequence {
        val ic = targetSupplier() ?: return ""
        val extracted = ic.getExtractedText(ExtractedTextRequest(), 0)
        return extracted?.text ?: ""
    }

    override fun getSelection(): TextRange {
        val ic = targetSupplier() ?: return TextRange(0, 0)
        return TextRange(ic.cursorSelStart, ic.cursorSelEnd)
    }

    override fun onRevokeCommit(snapshot: SelectionSnapshot) {
        val ic = targetSupplier() ?: return
        ic.setSelection(snapshot.beforeStart, snapshot.afterEnd)
        ic.commitText(snapshot.beforeContent, 1)
        ic.setSelection(snapshot.beforeStart, snapshot.beforeEnd)
    }
}
```

`InputConnectionBridge` 的替换轮换逻辑（`doReplaceableCommitText()`）首先通过 `getTextBeforeCursor()` 获取光标前文本，检查是否匹配 `replacements` 列表中的某一项。匹配时，先通过 `deleteSurroundingText()` 删除光标前文本，再通过 `commitText()` 插入新文本，实现替换操作。不匹配时退化为普通提交。撤销恢复逻辑（`onRevokeCommit()`）先设置选区覆盖被修改的文本范围，然后提交原始内容覆盖当前文本，最后恢复到操作前的选区位置。

目标变更（用户切换输入目标编辑器）不需要特殊检测：`resetRevertion()` 的调用时机已经规避了目标变更可能造成的影响——每次新的可撤回输入开始前都会清空旧快照，目标变更后的首次输入自然会在新的 `InputConnection` 上建立新快照。

---

## 5. EditTextBridge 编辑框桥接

`EditTextBridge` 是面向 Android `EditText` 目标的桥梁实现，由 `:ime-ui` 模块提供，适用于 IME 引擎在应用自身的 `EditText` 中使用（而非作为系统输入法）的场景。与 `InputConnectionBridge` 一样，它使用 supplier 模式获取当前 `EditText` 实例，且不检测目标变更——`resetRevertion()` 的调用时机已经规避了目标变更的影响。关键区别在于它直接操作 `EditText` 的 `Editable` 文本，相比 `InputConnection` 提供了更精细的文本操控能力。

```kotlin
class EditTextBridge(
    private val targetSupplier: () -> EditText?
) : BaseImeOutputBridge() {

    override fun doNormalCommitText(text: String) {
        resetRevertion()
        val e = targetSupplier() ?: return
        val beforeStart = e.selectionStart
        val beforeEnd = e.selectionEnd
        val beforeContent = e.text.substring(beforeStart, beforeEnd)
        e.text.replace(beforeStart, beforeEnd, text)
        recordRevertion(
            beforeStart = beforeStart,
            beforeEnd = beforeEnd,
            beforeContent = beforeContent,
            afterStart = e.selectionStart,
            afterEnd = e.selectionEnd,
        )
    }

    override fun doReplaceableCommitText(text: String, replacements: List<String>) {
        resetRevertion()
        val e = targetSupplier() ?: return
        val cursorPos = e.selectionStart
        val textBeforeCursor = if (cursorPos >= text.length) {
            e.text.substring(cursorPos - text.length, cursorPos).toString()
        } else null

        if (textBeforeCursor != null && replacements.contains(textBeforeCursor)) {
            val replaceStart = cursorPos - text.length
            val beforeContent = e.text.substring(replaceStart, cursorPos).toString()
            e.text.replace(replaceStart, cursorPos, text)
            recordRevertion(
                beforeStart = replaceStart,
                beforeEnd = cursorPos,
                beforeContent = beforeContent,
                afterStart = e.selectionStart,
                afterEnd = e.selectionEnd,
            )
        } else {
            doNormalCommitText(text)
        }
    }

    override fun insertPairedSymbols(left: String, right: String) {
        resetRevertion()
        val e = targetSupplier() ?: return
        val start = e.selectionStart
        val end = e.selectionEnd
        if (start != end) {
            val selected = e.text.substring(start, end)
            e.text.replace(start, end, left + selected + right)
            e.setSelection(start + left.length, start + left.length + selected.length)
        } else {
            e.text.replace(start, end, left + right)
            e.setSelection(start + left.length, start + left.length)
        }
    }

    override fun moveCursor(direction: CursorDirection) {
        val e = targetSupplier() ?: return
        val pos = when (direction) {
            CursorDirection.Left -> (e.selectionStart - 1).coerceAtLeast(0)
            CursorDirection.Right -> (e.selectionStart + 1).coerceAtMost(e.text.length)
            CursorDirection.Up, CursorDirection.Down -> e.selectionStart
        }
        e.setSelection(pos)
    }

    override fun selectRange(direction: CursorDirection) {
        val e = targetSupplier() ?: return
        val start = e.selectionStart
        val end = when (direction) {
            CursorDirection.Left -> (e.selectionEnd - 1).coerceAtLeast(start)
            CursorDirection.Right -> (e.selectionEnd + 1).coerceAtMost(e.text.length)
            CursorDirection.Up, CursorDirection.Down -> e.selectionEnd
        }
        e.setSelection(start, end)
    }

    override fun performEdit(action: EditorEditAction) {
        if (action.hasEditorEffect) resetRevertion()
        val e = targetSupplier() ?: return
        when (action) {
            EditorEditAction.BACKSPACE -> {
                val start = e.selectionStart
                if (start > 0) e.text.delete(start - 1, start)
            }
            EditorEditAction.SELECT_ALL -> e.selectAll()
            EditorEditAction.COPY -> {
                val clipboard = e.context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("text", e.text.substring(e.selectionStart, e.selectionEnd)))
            }
            EditorEditAction.PASTE -> {
                val clipboard = e.context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = clipboard.primaryClip?.getItemAt(0)?.text?.toString() ?: return
                e.text.replace(e.selectionStart, e.selectionEnd, clip)
            }
            EditorEditAction.CUT -> {
                val clipboard = e.context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("text", e.text.substring(e.selectionStart, e.selectionEnd)))
                e.text.delete(e.selectionStart, e.selectionEnd)
            }
            EditorEditAction.UNDO -> { /* EditText 无内置撤销 */ }
            EditorEditAction.REDO -> { /* EditText 无内置撤销 */ }
        }
    }

    override fun getText(): CharSequence = targetSupplier()?.text ?: ""

    override fun getSelection(): TextRange {
        val e = targetSupplier() ?: return TextRange(0, 0)
        return TextRange(e.selectionStart, e.selectionEnd)
    }

    override fun onRevokeCommit(snapshot: SelectionSnapshot) {
        val e = targetSupplier() ?: return
        e.text.replace(snapshot.beforeStart, snapshot.afterEnd, snapshot.beforeContent)
        e.setSelection(snapshot.beforeStart, snapshot.beforeEnd)
    }
}
```

`EditTextBridge` 直接操作 `EditText` 的 `Editable` 文本接口，提供了比 `InputConnection` 更精细的文本操控能力。`doNormalCommitText()` 使用 `Editable.replace()` 替换当前选区文本，自动处理选区重叠和文本长度变化。替换轮换逻辑（`doReplaceableCommitText()`）通过 `substring()` 获取光标前文本进行检查，匹配时使用 `replace()` 执行替换。配对符号插入时，若无选中文本，插入左右符号后通过 `setSelection()` 将光标置于两者之间；若有选中文本，将选中文本包裹在左右符号之间。编辑动作的实现使用 `EditText` 的原生 API（`selectAll()`、`ClipboardManager` 等），`UNDO` 和 `REDO` 动作为空操作（`EditText` 无内置撤销/重做支持）。收藏功能独立于编辑操作，通过 `ImeIntent.SaveFavorite` 触发，不属于 `EditorEditAction`，无需在桥梁中处理。

`EditTextBridge` 适用于应用内嵌入输入法的场景——例如应用内有一个自定义的文本编辑器，需要使用筷字输入法的引擎能力，但不通过系统 IME 服务。在这种场景下，`EditTextBridge` 将引擎的输出指令直接映射到 `EditText` 的文本操作，无需经过 `InputConnection` 的中转层。两个桥梁实现的选择由宿主模块决定：`:app` 模块中的 `IMEService` 使用 `InputConnectionBridge`，`:ime-ui` 模块中的嵌入式输入组件使用 `EditTextBridge`。

---

## 6. 数据流转全景图

引擎的数据流由三条核心通道构成：Intent 通道（用户操作 → 引擎处理）、State 通道（引擎状态 → UI 渲染）、Output 通道（引擎输出 → 目标编辑器）。三条通道通过 `ImeEngine` 的 `handleIntent()` 方法串联，形成严格的单向数据流。此外还有 Effect 通道（一次性效果 → UI 消费），与 State 通道并行但语义不同——State 表达持续性状态，Effect 表达一次性事件。

```plantuml
@file:../diagrams/engine-output-bridge.puml
```

### 6.1 完整数据流路径

从用户手势到最终输出的完整数据流路径如下：

1. **手势层**（`:ime-ui`）：用户在输入面板上的触摸手势由 `KeyboardComposable` 捕获，转换为 `InputGesture`（按下、滑行到、抬起、长按等）。

2. **ViewModel 层**（`:ime-ui`）：`KeyboardViewModel` 将 `InputGesture` 映射为 `ImeIntent`，调用 `ImeEngine.handleIntent()` 发送意图。手势到意图的映射是 ViewModel 的核心职责——`Tap` 手势映射为 `PressKey(key, KeyGesture.Tap)`，`Slip` 手势映射为 `PressKey(key, KeyGesture.Slip)`，`Flip` 手势映射为 `PressKey(key, KeyGesture.Flip)`。

3. **Intent 处理层**（`:ime-engine`）：`ImeEngine.handleIntent()` 接收 `ImeIntent`，执行六步处理链：
   - Step 1：`KeyboardIntentHandler` 将 `ImeIntent` 映射为 `KeyboardStateTransition`
   - Step 2：`KeyboardStateMachine` 执行状态转换 → 新 `KeyboardState` + `sideEffects`
   - Step 3：处理 `sideEffects`（异步意图如字典查询）
   - Step 4：通过 `copy()` 模式更新 `ImeState`
   - Step 5：分发 `ImeOutput` 到 `ImeOutputBridge`
   - Step 6：发射 `ImeEffect` 到 `SharedFlow`

4. **State 通道**：新 `ImeState` 通过 `StateFlow<ImeState>` 原子更新，UI 层通过 `collectAsState()` 订阅状态驱动 Compose 重组。状态更新是即时且持续的——订阅者始终读取到最新的完整状态快照。

5. **Output 通道**：`ImeOutput` 由 `dispatchToTarget()` 统一分发到 `ImeOutputBridge`，桥梁实现将语义操作翻译为平台特定 API 调用。输出通道承担所有对目标编辑器的操作——提交文本、移动光标、插入配对符号、执行编辑动作等。

6. **Effect 通道**：`ImeEffect` 通过 `SharedFlow<ImeEffect>` 发射，UI 层收集后立即消费。一次性效果（弹出提示、音效播放、确认对话框）通过此通道传递，消费后即丢弃，不存在状态清理和重复消费问题。

### 6.2 Intent/Output 映射关系

每种 `ImeIntent` 在引擎处理过程中可能产生零到多个 `ImeOutput`，映射关系如下：

| `ImeIntent` | 产生的 `ImeOutput` | 说明 |
|---|---|---|
| `PressKey` (拼音字符) | `CommitText` | 确认候选词后提交文本 |
| `SelectCandidate` | `CommitText` | 选中候选词后提交文本 |
| `CommitInput` | `CommitText` | 确认待选输入后提交文本 |
| `DeleteInput` | `RevokeCommit` | 撤销最近一次提交 |
| `PressKey` (配对符号) | `InsertPairedSymbols` | 输入配对符号时插入左右符号 |
| `MoveCursorTo` | `MoveCursor` | 移动光标到指定位置 |
| `PerformEdit` | `PerformEdit` | 执行编辑动作 |
| `PasteClip` | `CommitText` | 粘贴剪贴板内容 |
| `SwitchKeyboard` | 无 `ImeOutput` | 仅状态变更，无编辑器操作 |
| `PageCandidate` | 无 `ImeOutput` | 仅分页变更，无编辑器操作 |
| `UpdateConfig` | 无 `ImeOutput` | 仅配置变更，无编辑器操作 |

映射关系体现了引擎的设计原则：**仅编辑器操作产生 `ImeOutput`，纯状态变更不产生输出**。键盘切换、候选翻页、配置变更等操作仅影响引擎内部状态，不向目标编辑器发送任何指令，避免了不必要的编辑器干扰。

### 6.3 桥梁选择与注册

`ImeOutputBridge` 的注册由宿主模块在创建 `ImeEngine` 后执行：`:app` 模块通过 `engine.attachOutputBridge(InputConnectionBridge { currentInputConnection })` 注册系统输入连接桥梁，`:ime-ui` 模块通过 `engine.attachOutputBridge(EditTextBridge { currentEditText })` 注册编辑框桥梁。桥梁在引擎运行期间可以动态切换——`detachOutputBridge()` 注销当前桥梁，`attachOutputBridge()` 注册新桥梁。引擎在分发 `ImeOutput` 时检查桥梁是否存在：若存在则调用对应的语义方法，若不存在则静默忽略。这种设计允许引擎在没有桥梁的情况下正常运行（例如纯逻辑测试场景），输出操作被自动跳过。

第三方应用可以创建自定义桥梁实现，将 IME 引擎接入任意编辑器目标：简单场景直接实现 `ImeOutputBridge` 接口，需要撤销支持则继承 `BaseImeOutputBridge`。接口保持最小化和平台无关，新增桥梁无需理解引擎内部类型——引擎内部的 `ImeOutput` 类型体系通过 `dispatchToTarget()` 翻译为桥梁的语义方法调用，桥梁实现者只需关注「做什么」，无需理解引擎内部的输出类型分发逻辑。
