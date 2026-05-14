# AI 技能库 — 索引

本目录存放与筷字输入法项目开发相关的 AI 技能文档，提供最佳代码写法、避免不规范写法的指导，明确哪些能做、哪些不能做，以及 Kotlin 和 Jetpack Compose 等库的功能特性使用建议。

技能库内容跨版本通用，适用于项目所有主线版本的开发。

---

## 技能文档列表

| 文档 | 说明 |
|------|------|
| [Kotlin 最佳实践](kotlin-best-practices.md) | Kotlin 2.3.20 特性使用、惯用写法、避坑指南 |
| [Jetpack Compose 最佳实践](compose-best-practices.md) | Compose BOM 2026.04.01 特性使用、IME 场景下的最佳实践 |
| [代码规范](code-conventions.md) | 项目级代码规范：命名、结构、设计原则等 |

---

## 核心原则

### 显式优于隐式

- 所有公开 API 必须有显式的类型声明、文档注释和访问修饰符
- 避免使用隐式转换、隐式接收者、隐式作用域等不明确的写法
- 优先使用显式的 `return`、显式的 `it` 命名（当 lambda 参数超过一个或含义不明确时）
- 状态变更必须有迹可循，禁止隐式修改共享状态
- 错误处理必须显式，不使用 try-catch 吞掉异常

### 任其崩溃（Fail Fast）

- 遇到不可恢复的错误，立即抛出异常或崩溃，而不是静默降级
- 不使用 `!!` 以外的空值假设——如果逻辑上不可能为 null，用 `!!` 明确声明并让它在假设错误时崩溃
- 不对关键前置条件做防御性容错——使用 `require()`、`check()` 在入口处断言
- 状态不一致时立即失败，而不是尝试自动修复
- 崩溃是发现问题的最快方式，静默失败只会掩盖 bug

### 不可变优先

- 所有数据类默认为 `data class`，属性默认 `val`
- 仅在确实需要可变状态时使用 `var`，并需在文档中说明理由
- 集合类型默认使用只读接口（`List`、`Map`、`Set`），仅在构建时使用 `mutableListOf` 等
- 状态变更通过创建新实例而非修改现有实例（`copy()` 模式）

### 单向数据流

- UI 状态自上而下流动，事件自下而上传递
- 禁止视图层直接修改模型层状态
- 所有状态变更必须经过统一的处理管道

---

## 技能文档编写规范

> **目的**：避免因示例与设计不一致、命名不规范、内容范围混乱等问题而反复纠正，确保技能文档的准确性和一致性。

### 1. 文档范围声明

每篇技能文档必须在开头（标题与第一行 `---` 之间）声明其内容范围：

- **通用内容**：与特定项目无关的语言/框架最佳实践。示例使用领域无关的类名和场景（如 `Item`、`User`、`Config` 等），不使用本项目的类名
- **项目内容**：直接涉及本项目的架构、命名、设计。示例使用本项目的真实类名和设计概念

声明格式示例：

```
> **说明**：本文档前半部分为通用 Kotlin 最佳实践，第 5、8 节涉及本项目的 Java → Kotlin 迁移对比。通用示例不绑定本项目领域对象，项目迁移示例则直接使用本项目的类名和设计。
```

**规则**：

- 一篇文档中可同时包含通用和项目内容，但必须明确分节，且在文档开头声明哪些部分是通用的、哪些是项目特定的
- 通用章节中的示例禁止使用本项目的类名（如 `ImeEngine`、`KeyboardPanel` 等），应使用通用名称（如 `DataProcessor`、`ItemPanel` 等）
- 项目特定章节中的示例必须使用本项目的真实类名，不得虚构或使用过时名称

### 2. 命名一致性

技能文档中出现的所有项目类名必须与最新设计一致。以下是当前已确认的命名规范：

#### 2.1 三层模块命名

| 模块 | 命名规则 | 示例 |
|------|----------|------|
| `:ime-engine` | 公开 class 以 `Ime` 为前缀 | `ImeEngine`, `ImeConfig`, `ImeOutput`, `ImeState`, `ImeIntent` |
| `:ime-ui` | 不使用 `Ime` 前缀，贴近 UI 业务命名 | `KeyboardPanel`, `EditorField`, `GestureFeedbackPanel`, `CandidatePanel` |
| `:app` | 不使用 `Ime` 前缀，贴近应用业务命名 | `IMEService`, `ConfigRepository`, `InputConnectionBridge` |

#### 2.2 UI 组件后缀约定

| 后缀 | 含义 | 示例 |
|------|------|------|
| `Panel` | 容器/组合器（组合多个子组件） | `KeyboardPanel`, `GestureInputPanel`, `CandidatePanel`, `InputListPanel`, `KeyGridPanel`, `GestureFeedbackPanel` |
| `View` | 叶子视图（独立的可渲染单元） | `XPadView`, `InputHostView`, `KeyView` |
| 直接职能名 | 非组合非叶子的功能组件 | `EditorField`, `EditorHost` |
| `Screen` | 应用层页面 | `SettingsScreen`, `MainScreen`, `ExerciseScreen` |

#### 2.3 KeyGridPanel 子类命名

`KeyGridPanel` 的子类以 `KeyGridPanel` 为**后缀**，前缀表达键盘变体类型：

| 类名 | 说明 |
|------|------|
| `StandardKeyGridPanel` | 标准键盘（拼音、拉丁、数字、符号、编辑器、数学） |
| `EmojiKeyGridPanel` | Emoji 面板 |
| `CandidateKeyGridPanel` | 候选键盘 |
| `CommitOptionKeyGridPanel` | 提交选项键盘 |

**禁止**使用 `KeyPanel`、`StandardKeyboard`、`StandardKeyPanel` 等已废弃名称。

#### 2.4 常见已更名对照

| 旧名称 | 新名称 | 备注 |
|--------|--------|------|
| `EditorActionType` | `EditorAction` | 统一为单一枚举 |
| `StandardKeyboard` | `StandardKeyGridPanel` | 去掉 `onKeyPress`，纯渲染 |
| `KeyPanel` | `KeyGridPanel` | 强调 Grid 布局特征 |
| `StandardKeyPanel` | `StandardKeyGridPanel` | 跟随 KeyGridPanel 更名 |
| `InputPanel` | `GestureInputPanel` | 强调手势输入职能 |
| `KeyboardView` | `KeyboardPanel` | 明确容器角色 |
| `CandidateBar` / `InputBar` | `CandidatePanel` / `InputListPanel` | 统一 Panel 后缀 |
| `GuideScreen` | `MainScreen` | 准确反映主界面职能 |
| `InputPracticeScreen` | `ExerciseScreen` | 合并练习与演示 |

#### 2.5 引擎 API 命名

| 类型 | 命名 | 说明 |
|------|------|------|
| Intent | `ImeIntent.EditAction(EditorAction)` | 不是 `ImeIntent.EditorAction(EditorActionType)` |
| Output | `ImeOutput.EditAction(EditorAction)` | Intent 与 Output 对称使用同一 `EditorAction` 枚举 |
| 手势输入 | `ImeEngine.handleGesture(InputGesture)` | 不是 `onKeyPress` / `handleKeyPress` |
| 意图处理 | `ImeEngine.handleIntent(ImeIntent)` | 直接发送意图 |

### 3. 架构一致性

技能文档涉及项目架构时，必须与三层库架构设计保持一致：

#### 3.1 三层分离

- **GestureInputPanel**（输入层）：唯一触摸事件接收者，完全透明不绘制，输出 `InputGesture`
- **KeyGridPanel**（按键层）：纯展示，不处理触摸，不绘制手势反馈，提供布局信息
- **GestureFeedbackPanel**（反馈层）：独立透明绘制层，支持多实例，从 `GestureFeedbackState` 消费数据

**禁止**在 KeyGridPanel 示例中出现 `onKeyPress`、`onClick`、`pointerInput` 等触摸处理逻辑——这些属于 GestureInputPanel 的职责。

#### 3.2 MVI 数据流

```
GestureInputPanel → InputGesture → ImeEngine.handleGesture()
                                      ↓
                                 ImeEngine.reduce()
                                      ↓
                              StateFlow<ImeState> → KeyGridPanel（纯渲染）
                              StateFlow<ImeState> → CandidatePanel
                              StateFlow<ImeState> → InputListPanel
                                      ↓
                              GestureFeedbackState → GestureFeedbackPanel（视觉反馈）
```

**禁止**出现 UI 组件直接调用 `ImeEngine.reduce()` 的示例——Intent 处理必须通过 `handleGesture` 或 `handleIntent` 入口。

#### 3.3 依赖方向

```
:app → :ime-ui → :ime-engine
```

- `:ime-engine` 不依赖任何 UI 框架和 Android 框架（字典 I/O 除外）
- `:ime-ui` 仅依赖 `:ime-engine` 的公开 API，不依赖引擎内部实现
- `:app` 依赖 `:ime-engine` + `:ime-ui`，直接使用 `KeyboardViewModel` 而不继承或扩展

### 4. 示例编写规范

#### 4.1 通用示例

通用章节的示例应使用领域无关的命名：

```kotlin
// ✅ 正确：通用示例用领域无关名称
data class ListState(
    val items: List<Item> = emptyList(),
    val selectedIndex: Int = 0,
)

// ❌ 错误：通用示例使用本项目类名
data class InputListState(
    val chars: List<CharInput> = emptyList(),
    val cursorIndex: Int = 0,
)
```

#### 4.2 项目示例

项目特定章节的示例必须使用本项目的真实类名，并反映最新设计：

```kotlin
// ✅ 正确：项目示例用真实类名
@Composable
fun StandardKeyGridPanel(
    keyGrid: List<List<InputKey>>,
    keyboardState: KeyboardState,
    onLayoutInfoChanged: (KeyGridPanelLayoutInfo) -> Unit,
    modifier: Modifier = Modifier,
) { ... }

// ❌ 错误：使用已废弃的类名或参数
@Composable
fun StandardKeyboard(
    keyGrid: List<List<InputKey>>,
    onKeyPress: (InputKey) -> Unit,  // 触摸处理不属于 KeyGridPanel
    modifier: Modifier = Modifier,
) { ... }
```

#### 4.3 交叉引用

当技能文档需要引用设计文档的详细内容时，使用文档编号交叉引用，不重复内容：

```
完整定义见文档 160 第 4 节
三层分离设计详见文档 150
主题系统设计见文档 500 第 4 节
```

### 5. 同步更新规则

当项目的命名、架构或设计发生变更时，技能文档必须**同步更新**：

1. **类名变更**：所有技能文档中出现的旧类名必须替换为新类名，包括示例代码、表格、注释中的引用
2. **架构变更**：涉及组件职责变更时，相关示例必须反映新的职责边界（如 KeyGridPanel 去掉触摸处理）
3. **新增约定**：新确认的命名约定或设计规则必须同步补充到本文档的命名一致性章节
4. **废弃对照**：重要的更名应添加到「常见已更名对照」表，保留历史参照

### 6. 内容边界

技能文档聚焦于**怎么做**（how-to）和**为什么**（why），不包含以下内容：

- ❌ 设计决策记录——属于 `docs/ai-agent/v4/discussions/` 目录
- ❌ 执行过程和变更日志——属于 `docs/ai-agent/v4/logs/` 目录
- ❌ 设计方案的完整定义——属于 `docs/ai-agent/v4/design/` 目录，技能文档仅引用
- ❌ 测试编写规范——属于 `docs/ai-agent/v4/tests/` 目录
