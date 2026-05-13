# 代码规范

本文档定义筷字输入法 Kotlin 项目的代码规范，所有开发者（包括 AI Agent）必须遵循。

---

## 1. 命名规范

### 1.1 包命名

- 格式：`org.crazydan.studio.app.ime.kuaizi.<module>.<submodule>`（:app 模块无子模块名，直接使用顶级包名）
- 全小写，不使用下划线
- 模块划分与功能对应，不按技术层划分

```
org.crazydan.studio.app.ime.kuaizi.engine.api
org.crazydan.studio.app.ime.kuaizi.engine.domain
org.crazydan.studio.app.ime.kuaizi.engine.dict
org.crazydan.studio.app.ime.kuaizi.ui.theme
org.crazydan.studio.app.ime.kuaizi.ui.keyboard
org.crazydan.studio.app.ime.kuaizi.ui.integration
org.crazydan.studio.app.ime.kuaizi       ← :app 模块（无子模块名）
```

### 1.2 类命名

| 类型 | 规范 | 示例 |
|------|------|------|
| 类/接口 | PascalCase | `PinyinKeyboard`, `InputKey` |
| Sealed class | PascalCase，子类也 PascalCase | `InputKey.Char`, `InputKey.Ctrl` |
| Data class | PascalCase | `InputState`, `Candidate` |
| Value class | PascalCase | `Spell`, `ConfigKey` |
| Enum class | PascalCase | `KeyboardType` |
| Object（单例） | PascalCase | `Emojis`, `SymbolGroups` |
| Companion object | `Companion` | 不自定义名称 |

### 1.3 函数命名

| 类型 | 规范 | 示例 |
|------|------|------|
| 普通函数 | camelCase | `lookupCandidate()`, `commitInput()` |
| 返回 Boolean | `is`/`has`/`should`/`can` 前缀 | `isPinyinMode()`, `hasPendingInput()` |
| Composable 函数 | PascalCase | `KeyboardPanel()`, `CandidatePanel()` |
| 工厂函数 | `of`/`from`/`create` | `fromSpell()`, `createContext()` |
| DSL builder 函数 | 与目标类同名（小写） | `inputMsg { }`, `keyTable { }` |

### 1.4 变量命名

| 类型 | 规范 | 示例 |
|------|------|------|
| 局部变量 | camelCase | `candidate`, `currentInput` |
| 常量（compile-time） | `SCREAMING_SNAKE_CASE` | `MAX_CANDIDATES`, `DEFAULT_TIMEOUT` |
| 顶层/对象常量 | `SCREAMING_SNAKE_CASE` | `Emojis.GROUP_COUNT` |
| 私有属性 | camelCase，不加前缀 | `state`, `dict`（不用 `mState`） |
| Backing property | 下划线前缀 | `_state` → `state`（`StateFlow` 模式） |
| Lambda 参数 | 显式命名，避免 `it` 超过一层嵌套 | `chars.map { char -> char.text }` |

### 1.5 文件命名

- 单类文件：与类名一致，PascalCase
- 多类/函数文件：使用 camelCase 或 PascalCase，反映内容
- 扩展函数文件：`<ReceiverName>+<Feature>.kt`，如 `String+Pinyin.kt`

---

## 2. 格式规范

### 2.1 缩进与换行

- 缩进：4 空格（不用 Tab）
- 最大行长：120 字符
- 函数签名过长时，每个参数独占一行：

```kotlin
fun processInput(
    input: CharInput,
    keyboard: Keyboard,
    config: Config,
    dict: PinyinDict,
): InputState {
    // ...
}
```

### 2.2 空行规则

- 类成员之间：1 个空行
- 方法之间：1 个空行
- 逻辑段落之间：1 个空行
- 包声明后：1 个空行
- import 块后：1 个空行

### 2.3 Import 规范

- 禁止通配符 import（`import x.y.*`）
- 按字母序排列
- 删除未使用的 import

---

## 3. 注释规范

### 3.1 文档注释（KDoc）

所有公开的类、接口、函数和属性必须有 KDoc：

```kotlin
/**
 * 拼音输入键盘，处理拼音字符的输入、滑行、翻转和 X-Pad 模式。
 *
 * 状态机包含以下阶段：
 * - Input_Wait: 等待用户输入
 * - Slip: 滑行输入中
 * - Flip: 翻转输入中
 * - XPad: X-Pad 输入中
 *
 * @param dict 拼音字典，用于候选项查询
 * @param config 输入法配置
 */
class PinyinKeyboard(
    private val dict: PinyinDict,
    private val config: ImeConfig,
) {
    /**
     * 处理用户按键消息，更新内部状态并返回输入消息。
     *
     * @param msg 用户按键消息
     * @return 输入消息列表，可能为空
     */
    fun handleKeyMsg(msg: UserKeyMsg): List<InputMsg> { ... }
}
```

### 3.2 实现注释

复杂的业务逻辑需要行内注释说明"为什么"而非"做什么"：

```kotlin
// 使用 Viterbi 算法计算最可能的词组组合，
// 因为逐字匹配无法处理多音字消歧
val phrase = viterbi.decode(spellChars)
```

---

## 4. 架构规范

### 4.1 依赖方向

```
UI 层 → ViewModel → Domain 层 → Data 层
                                        ↓
                                    Platform 层
```

- 上层可依赖下层，下层不可依赖上层
- 同层之间通过接口或消息通信，不直接引用实现

### 4.2 模块职责

| 层 | 职责 | 对应 Java 版本 |
|----|------|---------------|
| **UI 层** | Compose 渲染、手势处理、动画 | `ui/view/`, `ui/about/`, `ui/guide/` |
| **ViewModel 层** | 状态管理、Intent 处理 | `IMEditor`（部分）, `IMEditorView`（部分） |
| **Domain 层** | 键盘逻辑、输入列表、状态机 | `core/keyboard/`, `core/`, `InputList` |
| **Data 层** | 字典查询、用户数据、配置 | `dict/`, `conf/`, `ImeConfig` |
| **Platform 层** | Android IME Service 桥接 | `IMEService` |

### 4.3 状态管理

- 所有 UI 状态通过 `StateFlow` 暴露
- 状态变更通过 `Intent` 触发
- 禁止 UI 层直接修改 Domain 层状态

---

## 5. 错误处理规范

### 5.1 前置条件断言

```kotlin
// ✅ 推荐：require 检查公开 API 参数
fun lookup(spell: String): List<InputWord> {
    require(spell.isNotBlank()) { "Spell must not be blank" }
    // ...
}

// ✅ 推荐：check 检查内部状态
fun commitInput() {
    check(state.pending != null) { "No pending input to commit" }
    // ...
}
```

### 5.2 异常处理

```kotlin
// ✅ 推荐：对可恢复错误使用 Result
fun tryLookup(spell: String): Result<List<InputWord>> = runCatching {
    dict.lookup(spell)
}

// ✅ 推荐：对不可恢复错误直接抛出
fun openDict(): PinyinDict {
    return PinyinDict.open(context) ?: error("Failed to open pinyin dictionary")
}

// ❌ 禁止：空 catch 块
try { ... } catch (_: Exception) { } // 吞掉所有异常
```

---

## 6. 测试规范

### 6.1 命名

```kotlin
@Test
fun `lookup returns candidates for valid pinyin spell`() { ... }

@Test
fun `lookup throws when spell is blank`() { ... }
```

### 6.2 结构

```kotlin
@Test
fun `commit input updates state correctly`() {
    // Given
    val state = InputState(chars = listOf(charInput))

    // When
    val newState = state.commit()

    // Then
    assertEquals(emptyList<CharInput>(), newState.chars)
    assertEquals(0, newState.cursorIndex)
}
```

---

## 7. Git 规范

### 7.1 提交信息格式

```
<type>(<scope>): <subject>

<body>
```

类型：
- `feat`: 新功能
- `fix`: 修复缺陷
- `refactor`: 重构（不改变功能）
- `docs`: 文档
- `test`: 测试
- `chore`: 构建/工具

### 7.2 分支策略

- `refactor-kotlin`: Kotlin 重构主分支
- `java-frozen`: Java 版本冻结分支（只读）
- 功能开发：从 `refactor-kotlin` 检出，完成后合并回去
