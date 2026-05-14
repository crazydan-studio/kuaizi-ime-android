# 代码规范

本文档定义项目的代码级规范，包括命名、格式、注释、错误处理和 Git 规范。

> **说明**：本文档为通用代码规范，适用于所有 Kotlin 项目。项目特定的命名规范（模块前缀、UI 组件后缀、已更名对照等）详见 [v4 命名规范](../v4/design/010-naming-conventions.md)。

---

## 1. 命名规范

### 1.1 类命名

| 类型 | 规范 | 示例 |
|------|------|------|
| 类/接口 | PascalCase | `DataProcessor`, `InputHandler` |
| Sealed class | PascalCase，子类也 PascalCase | `Result.Success`, `Result.Error` |
| Data class | PascalCase | `UserState`, `Config` |
| Value class | PascalCase | `EntityId`, `PreferenceKey` |
| Enum class | PascalCase | `Status`, `Direction` |
| Object（单例） | PascalCase | `Emojis`, `Defaults` |
| Companion object | `Companion` | 不自定义名称 |

### 1.2 函数命名

| 类型 | 规范 | 示例 |
|------|------|------|
| 普通函数 | camelCase | `lookupCandidate()`, `commitInput()` |
| 返回 Boolean | `is`/`has`/`should`/`can` 前缀 | `isReady()`, `hasPendingInput()` |
| Composable 函数 | PascalCase | `KeyboardPanel()`, `ItemList()` |
| 工厂函数 | `of`/`from`/`create` | `fromSpell()`, `createContext()` |
| DSL builder 函数 | 与目标类同名（小写） | `config { }`, `keyTable { }` |

### 1.3 变量命名

| 类型 | 规范 | 示例 |
|------|------|------|
| 局部变量 | camelCase | `candidate`, `currentInput` |
| 常量（compile-time） | `SCREAMING_SNAKE_CASE` | `MAX_CANDIDATES`, `DEFAULT_TIMEOUT` |
| 顶层/对象常量 | `SCREAMING_SNAKE_CASE` | `Defaults.MAX_COUNT` |
| 私有属性 | camelCase，不加前缀 | `state`, `dict`（不用 `mState`） |
| Backing property | 下划线前缀 | `_state` → `state`（`StateFlow` 模式） |
| Lambda 参数 | 显式命名，避免 `it` 超过一层嵌套 | `items.map { item -> item.name }` |

### 1.4 文件命名

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
 * 数据处理器，负责将原始输入转换为结构化结果。
 *
 * 处理流程包含验证、转换和缓存三个阶段：
 * - 验证：检查输入数据的完整性和合法性
 * - 转换：将输入映射为目标数据结构
 * - 缓存：缓存转换结果以避免重复计算
 *
 * @param config 处理器配置
 * @param repository 数据仓库
 */
class DataProcessor(
    private val config: ProcessorConfig,
    private val repository: Repository,
) {
    /**
     * 处理单条输入数据。
     *
     * @param input 输入数据
     * @return 处理结果，可能为空
     */
    fun process(input: Input): Result? { ... }
}
```

### 3.2 实现注释

复杂的业务逻辑需要行内注释说明"为什么"而非"做什么"：

```kotlin
// 使用 Viterbi 算法计算最可能的路径组合，
// 因为逐段匹配无法处理多义性消歧
val path = viterbi.decode(segments)
```

---

## 4. 错误处理规范

### 4.1 前置条件断言

```kotlin
// ✅ 推荐：require 检查公开 API 参数
fun lookup(spell: String): List<Candidate> {
    require(spell.isNotBlank()) { "Spell must not be blank" }
    // ...
}

// ✅ 推荐：check 检查内部状态
fun commitInput() {
    check(state.pending != null) { "No pending input to commit" }
    // ...
}
```

### 4.2 异常处理

```kotlin
// ✅ 推荐：对可恢复错误使用 Result
fun tryLookup(spell: String): Result<List<Candidate>> = runCatching {
    dict.lookup(spell)
}

// ✅ 推荐：对不可恢复错误直接抛出
fun openDict(): Dict {
    return Dict.open(path) ?: error("Failed to open dictionary at $path")
}

// ❌ 禁止：空 catch 块
try { ... } catch (_: Exception) { } // 吞掉所有异常
```

---

## 5. 测试规范

### 5.1 命名

```kotlin
@Test
fun `lookup returns candidates for valid spell`() { ... }

@Test
fun `lookup throws when spell is blank`() { ... }
```

### 5.2 结构

```kotlin
@Test
fun `commit input updates state correctly`() {
    // Given
    val state = InputState(items = listOf(item))

    // When
    val newState = state.commit()

    // Then
    assertEquals(emptyList<Item>(), newState.items)
    assertEquals(0, newState.cursorIndex)
}
```

---

## 6. Git 规范

### 6.1 提交信息格式

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

### 6.2 分支策略

- `refactor-kotlin`: Kotlin 重构主分支
- `java-frozen`: Java 版本冻结分支（只读）
- 功能开发：从 `refactor-kotlin` 检出，完成后合并回去
