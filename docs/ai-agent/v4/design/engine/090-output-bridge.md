# 输出桥接机制

v4 采用 `ImeOutputBridge` 桥接模式。引擎内部仅执行一次 `when(ImeOutput)` 分发，桥接实现者只需实现语义方法，无需理解 `ImeOutput` 类型层次。本文档描述桥接架构、接口定义、基础抽象类，以及 `:ime-ui` 和 `:app` 模块中的参考实现。

---

## 1 架构总览

```plantuml
@file:../diagrams/engine-output-bridge.puml
```

模块职责：

```
:ime-engine/api/  → ImeOutputBridge 接口 + BaseImeOutputBridge 抽象类
:ime-ui/          → EditTextBridge
:app/             → InputConnectionBridge 实现 ImeOutputBridge
                    IMEService 不再手动收集 output
```

---

## 2 ImeOutputBridge 接口

```kotlin
/**
 * ImeEngine 与目标编辑器之间的桥梁。
 * 引擎通过此桥梁将 ImeOutput 语义化分派到具体编辑器，
 * 桥梁实现负责将语义操作翻译为平台特定 API 调用。
 *
 * 设计原则：
 * - 接口方法表达「做什么」（语义），不规定「怎么做」（实现）
 * - 实现者按自身能力决定操作粒度
 * - 所有方法在主线程调用
 */
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
    fun performAction(action: EditorAction)

    /** 实时获取目标当前文本 */
    fun getText(): CharSequence

    /** 实时获取目标当前选区 */
    fun getSelection(): TextRange
}
```

接口方法表达「做什么」（语义）而非「怎么做」（实现），这使得不同平台和目标编辑器可以按自身能力提供差异化的实现。所有方法在主线程调用以保证线程安全。`commitText()` 方法通过 `replacements` 参数支持直输模式下的字符轮换功能——桥梁实现需检查光标前文本是否匹配替换列表，匹配时执行替换而非插入。

---

## 3 BaseImeOutputBridge 抽象类

```kotlin
/**
 * ImeOutputBridge 的基础抽象实现。
 *
 * 撤销机制：
 * - 仅支持撤销最近一次可撤回输入，不使用栈
 * - 记录该次输入前后的选区快照（起止点 + 选区内容）
 * - 新的可撤回输入覆盖旧快照（只保留最近一次）
 * - revokeCommit() 消费快照后清空，不可重复撤销
 *
 * 快照重置时机：
 * - 新的可撤回输入开始前 → 先清空旧快照，提交后记录新快照
 * - 撤销完成后 → 清空已消费的快照
 * - 成对符号提交 → 清空快照（不可撤回）
 * - 有编辑副作用的动作（退格、粘贴、剪切、撤销、重做）→ 清空快照
 * - 无编辑副作用的动作（全选、收藏、复制）→ 不影响快照
 *
 * 子类职责：
 * - 在各接口方法中按上述规则调用 resetRevertion() / recordRevertion()
 * - 实现 onRevokeCommit() 执行实际的撤销恢复
 */
abstract class BaseImeOutputBridge : ImeOutputBridge {

    private data class SelectionSnapshot(
        val beforeStart: Int,
        val beforeEnd: Int,
        val beforeContent: String,
        val afterStart: Int,
        val afterEnd: Int
    )

    /** 当前可撤销的选区快照，null 表示无可撤销输入 */
    private var revertion: SelectionSnapshot? = null

    /** 子类调用：清空撤销快照（撤销状态重置） */
    protected fun resetRevertion() {
        revertion = null
    }

    /** 子类调用：记录可撤回输入的快照 */
    protected fun recordRevertion(
        beforeStart: Int, beforeEnd: Int, beforeContent: String,
        afterStart: Int, afterEnd: Int
    ) {
        revertion = SelectionSnapshot(
            beforeStart = beforeStart,
            beforeEnd = beforeEnd,
            beforeContent = beforeContent,
            afterStart = afterStart,
            afterEnd = afterEnd
        )
    }

    override fun revokeCommit() {
        val snapshot = revertion ?: return
        revertion = null  // 消费后立即清空
        onRevokeCommit(snapshot)
    }

    /** 子类实现：根据快照恢复文本与选区 */
    protected abstract fun onRevokeCommit(snapshot: SelectionSnapshot)

    override fun commitText(text: String, replacements: List<String>?) {
        resetRevertion()
        if (replacements != null && replacements.isNotEmpty()) {
            doReplaceableCommitText(text, replacements)
        } else {
            doNormalCommitText(text)
        }
    }

    /** 子类实现：可替换的文本提交 */
    protected abstract fun doReplaceableCommitText(text: String, replacements: List<String>)

    /** 子类实现：普通文本提交（无替换） */
    protected abstract fun doNormalCommitText(text: String)
}
```

撤销机制采用单快照设计（而非栈），反映了 IME 的使用模式——用户通常只需撤销最近一次输入。快照记录提交前后的选区状态，使得撤销时可以精确恢复到操作前的文本和光标位置。明确的快照重置规则确保不可逆操作（成对符号、破坏性编辑）正确地使撤销状态失效：成对符号提交清空快照因为无法简单撤回两个插入点；有编辑副作用的动作（退格、粘贴、剪切、撤销、重做）清空快照因为它们改变了撤销所依赖的文本状态；而无编辑副作用的动作（全选、收藏、复制）不影响快照。子类负责在各接口方法中按规则调用 `resetRevertion()` 和 `recordRevertion()`。

---

## 4 InputConnectionBridge（系统输入连接）

> 本实现位于 `:app` 模块。

```kotlin
/**
 * 面向系统输入连接的桥梁实现。
 * 构造时接受 supplier 获取当前 InputConnection。
 */
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
            beforeStart = beforeStart, beforeEnd = beforeEnd, beforeContent = beforeContent,
            afterStart = afterSel.start.coerceAtLeast(0), afterEnd = afterSel.end.coerceAtLeast(0),
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
                beforeStart = replaceStart, beforeEnd = getSelection().start,
                beforeContent = beforeContent,
                afterStart = afterSel.start.coerceAtLeast(0), afterEnd = afterSel.end.coerceAtLeast(0),
            )
        } else {
            doNormalCommitText(text)
        }
    }

    override fun insertPairedSymbols(left: String, right: String) {
        resetRevertion()  // 成对符号不可撤回
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

    override fun moveCursor(direction: CursorDirection) { /* via InputConnection */ }
    override fun selectRange(direction: CursorDirection) { /* via InputConnection */ }

    override fun performAction(action: EditorAction) {
        if (action.hasEditorEffect) { resetRevertion() }
        val ic = targetSupplier() ?: return
        when (action) {
            EditorAction.BACKSPACE -> { /* sendKeyEvent DEL */ }
            EditorAction.SELECT_ALL -> { ic.performContextMenuAction(android.R.id.selectAll) }
            EditorAction.COPY -> { ic.performContextMenuAction(android.R.id.copy) }
            EditorAction.PASTE -> { ic.performContextMenuAction(android.R.id.paste) }
            EditorAction.CUT -> { ic.performContextMenuAction(android.R.id.cut) }
            EditorAction.UNDO -> { ic.performContextMenuAction(android.R.id.undo) }
            EditorAction.REDO -> { ic.performContextMenuAction(android.R.id.redo) }
            EditorAction.FAVORITE -> { /* no-op for InputConnection */ }
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

`InputConnectionBridge` 是面向 Android 系统 `InputConnection` 的桥梁实现，由 `:app` 模块中的 `IMEService` 创建和管理。它使用 supplier 模式在每次操作时获取当前 `InputConnection`。目标变更（当用户切换输入目标时发生）不需要检测：`resetRevertion()` 的调用时机已经规避了目标变更可能造成的影响——每次新的可撤回输入开始前都会清空旧快照，目标变更后的首次输入自然会在新的 `InputConnection` 上建立新快照，不会错误恢复到旧目标的文本状态。`FAVORITE` 动作在此实现中为空操作，因为 `InputConnection` 没有收藏的概念。

---

## 5 EditTextBridge（EditText 类型默认实现）

> 本实现位于 `:ime-ui` 模块。

```kotlin
/**
 * 面向 EditText 类型目标的默认桥梁实现。
 * 构造时接受 supplier 获取当前 EditText 实例。
 */
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
            beforeStart = beforeStart, beforeEnd = beforeEnd, beforeContent = beforeContent,
            afterStart = e.selectionStart, afterEnd = e.selectionEnd,
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
                beforeStart = replaceStart, beforeEnd = cursorPos, beforeContent = beforeContent,
                afterStart = e.selectionStart, afterEnd = e.selectionEnd,
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

    override fun moveCursor(direction: CursorDirection) { /* 操作 EditText 选区 */ }
    override fun selectRange(direction: CursorDirection) { /* 操作 EditText 选区 */ }

    override fun performAction(action: EditorAction) {
        if (action.hasEditorEffect) { resetRevertion() }
        val e = targetSupplier() ?: return
        // ... 执行动作
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

`EditTextBridge` 是面向 Android `EditText` 目标的桥梁实现，由 `:ime-ui` 模块提供，适用于 IME 引擎在应用自身的 `EditText` 中使用（而非作为系统输入法）的场景。与 `InputConnectionBridge` 一样，它使用 supplier 模式且不检测目标变更——`resetRevertion()` 的调用时机已经规避了目标变更可能造成的影响。关键区别在于它直接操作 `EditText` 的 `Editable` 文本，相比 `InputConnection` 提供了更精细的文本操控能力。

---

## 6 扩展桥梁

第三方应用可以创建自定义桥梁实现，将 IME 引擎接入任意编辑器目标：

- **简单场景**：直接实现 `ImeOutputBridge` 接口，按需提供各语义方法的实现。
- **需要撤销支持**：继承 `BaseImeOutputBridge`，自动获得单快照撤销机制，只需实现 `onRevokeCommit()` 和两个文本提交方法。
- **接口设计原则**：`ImeOutputBridge` 保持最小化和平台无关，新增桥梁无需理解引擎内部类型。

典型扩展示例：

- **Web 编辑器桥梁**：将语义操作翻译为 JavaScript 调用，通过 WebView 的 `evaluateJavascript()` 执行 DOM 操作。
- **游戏引擎文本输入桥梁**：将文本提交和光标操作映射到游戏引擎的 UI 框架 API。
- **终端模拟器桥梁**：将文本提交转换为按键事件序列，适配终端的字符输入模型。
