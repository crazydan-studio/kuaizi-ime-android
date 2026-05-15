# 三层模块划分

## 1 概述

v4 版本将筷字输入法设计为三层库架构，支持其他程序以库的形式引入，提供完整的输入法能力支持：

- **引擎库 `:ime-engine`**：纯 Kotlin，不依赖 Android 框架（字典 I/O 除外），提供核心输入引擎能力
- **UI 库 `:ime-ui`**：基于 Compose 的缺省 UI 实现，对第三方应用开放，可作为即插即用的输入界面使用，也可被自定义 UI 替换
- **应用模块 `:app`**：系统 IME 服务壳、设置页面、配置持久化，是库的官方消费者

库的核心价值在于：任何 Android 应用都可以通过引入 `:ime-engine` + `:ime-ui`，获得筷字输入法的完整输入能力和缺省 UI——拼音滑行、X-Pad 连续输入、候选选择、输入列表管理、撤销重做等——而无需依赖系统 IME 服务。仅需要引擎逻辑的场景（如文本预处理、自动化测试）可以只引入 `:ime-engine` 而不引入 UI 库。需要完全自定义 UI 的场景可以只引入 `:ime-engine` 并自行实现视图层。

```plantuml
@file:../diagrams/class-organization.puml
```

---

## 2 模块职责与依赖

| 模块 | 职责 | 依赖 |
|------|------|------|
| `:ime-engine` | IME 核心引擎，纯 Kotlin，不依赖 Android 框架（字典 I/O 除外） | Kotlin 标准库 + 协程 |
| `:ime-ui` | Compose 缺省 UI + KeyboardViewModel，包含完整的输入法界面组件和 UI 协调逻辑 | `:ime-engine` + Compose + Material3 + Lifecycle ViewModel |
| `:app` | 系统 IME 服务壳、ImeEngine 创建与 InputConnectionBridge 管理、配置持久化（DataStore）、设置页面 | `:ime-engine` + `:ime-ui` + DataStore + Lifecycle |

**依赖关系图**：

```
┌──────────┐
│  :app    │ 应用模块（IME 服务 + 设置 + 配置持久化）
└────┬─────┘
     │ depends on
     ├──────────────────────┐
     ▼                      ▼
┌──────────┐          ┌──────────┐
│ :ime-ui  │          │:ime-engine│
│ Compose  │          │ 纯 Kotlin │
│ 缺省 UI  │          │ 核心引擎  │
└────┬─────┘          └───────────┘
     │ depends on
     ▼
┌──────────┐
│:ime-engine│
└───────────┘
```

```plantuml
@file:../diagrams/architecture.puml
```

---

## 3 设计原则

1. **引擎与 UI 完全分离**：`:ime-engine` 不包含任何 Compose 代码或 Android View，只暴露状态流和意图接口
2. **UI 库作为缺省实现对外开放**：`:ime-ui` 提供完整的 Compose UI 组件，第三方应用可直接使用，也可替换为自定义 UI
3. **统一配置**：库不内置配置持久化，所有配置通过 `ImeConfig`（含引擎配置和 UI 配置的明确隔离）在创建时或运行时设置
4. **数据库层可替换**：字典接口与实现分离，内置 SQLite 实现，外部可提供自己的 `ImeDictProvider`
5. **功能可裁剪**：收藏和剪贴板等可选功能通过 `Feature` 标记按需启用/禁用
6. **Fail Fast**：非法操作（如禁用收藏后调用收藏功能）立即抛出异常而非静默忽略

---

## 4 引擎库公开 API

引擎库的公开 API 定义了 `:ime-engine` 与 `:ime-ui`、`:app` 之间的核心契约，包括：

- **ImeEngine**：引擎核心入口点，通过 `StateFlow` 暴露状态，通过 `ImeIntent` 接收操作，通过 `ImeOutputBridge` 输出编辑指令
- **ImeConfig**：统一运行时配置，含引擎配置（`EngineConfig`）和 UI 配置（`UiConfig`）的明确隔离
- **ImeOutput**：引擎编辑输出的 sealed class 表达，由引擎统一分发到桥梁
- **ImeIntent**：用户意图的 sealed class 表达，所有用户操作统一为 Intent
- **ImeState 子状态类型**：`InputListState`、`CandidateListState`、`ClipboardState`、`FavoriteListState` 等

完整的 API 定义和代码详见 [010-引擎库设计总览](../engine/010-engine-overview.md)。

---

## 5 UI 库设计

UI 库 `:ime-ui` 的核心设计目标是作为**缺省 UI 实现**对第三方应用开放，遵循「缺省实现、可替换、可组合、可定制」四大设计原则。UI 库提供从原子组件（KeyView、CandidateItem）到面板组件（GestureInputPanel、KeyGridPanel、CandidateListPanel）再到集成组件（KeyboardPanel、KeyboardScreen）的完整组件层次，以及 `KeyboardViewModel` 作为 UI 协调中心和主题系统。

**输出桥接机制**（ImeOutputBridge 桥接模式）是引擎与目标编辑器之间的核心架构模式：引擎内部统一执行 `when(ImeOutput)` 分发，桥梁实现者只需实现语义方法。`ImeOutputBridge` 接口和 `BaseImeOutputBridge` 抽象类定义在 `:ime-engine`，`InputConnectionBridge`（系统输入连接）实现在 `:app`，`EditTextBridge`（EditText 类型）实现在 `:ime-ui`。

详细设计分别见：
- UI 库设计目标与组件清单：[010-UI 库设计总览](../ui/010-ui-library-overview.md)
- 输出桥接机制：[090-输出桥接机制](../engine/090-output-bridge.md)
