# 000 — 架构总览

## 1. 概述

v4 版本对筷字输入法进行全面的 Kotlin 重构，沿用 Java 版本确立的单向数据流设计原则，以 Kotlin 原生的 MVI + StateFlow 方案替代手写消息路由，同时利用现代 Android 开发技术进行整体现代化改造。核心改进方向：从命令式 MVP 转向声明式 MVI，从继承链转向组合模式，从手写异步转向协程，从 View 系统转向 Compose。

---

## 2. Java 版本架构分析

### 2.1 现有架构

Java 版本采用自定义消息驱动的 MVP 架构：

```
┌──────────────────────────────────────────────────────┐
│                    IMEService                         │
│  (Android Service, Message Router/Dispatcher)        │
│  - 生命周期管理                                       │
│  - InputConnection 操作                               │
│  - 路由: UserMsg → IMEditor, InputMsg → IMEditorView  │
└───────────┬──────────────────────┬───────────────────┘
            │                      │
    UserKeyMsg/UserInputMsg    InputMsg
            │                      │
            ▼                      ▼
┌───────────────────┐    ┌────────────────────┐
│    IMEditor       │    │   IMEditorView     │
│  (Model/Presenter)│    │   (View Layer)     │
│  ├─ Keyboard      │    │  ├─ MainboardView  │
│  ├─ Inputboard    │    │  │  ├─ KeyboardView │
│  ├─ Favoriteboard │    │  │  └─ InputboardView│
│  ├─ InputList     │    │  │     └─ InputListView│
│  └─ IMEditorDict  │    │  ├─ FavoriteboardView│
│                    │    │  └─ CandidatesView │
└───────────────────┘    └────────────────────┘
```

### 2.2 架构优点（保留）

1. **单向数据流的设计原则**：UserMsg 从 View 层向上发送到 IMEService，再分发到 Model 层；InputMsg 从 Model 层向上发送到 IMEService，再分发到 View 层。这种单向消息流确保了数据流向的可追踪性。v4 将这一原则保留并升级：以 ImeIntent 替代 UserMsg 向上传递，以 StateFlow 替代 InputMsg 向下分发，实现类型安全、自动生命周期管理的响应式数据流
2. **Model-View 严格分离**：模型层和视图层不直接通信，所有交互通过消息传递，降低了耦合
3. **Context 模式**：`KeyboardContext`、`InputboardContext`、`FavoriteboardContext` 携带共享状态进入模型操作，避免了全局状态的滥用
4. **不可变 Key 对象 + Builder 缓存**：Key 对象是不可变的，通过 Builder 模式创建并缓存，避免了重复创建
5. **状态机驱动的键盘逻辑**：键盘的输入、滑行、翻转、候选选择等状态通过有限状态机管理，状态转换有明确规则

### 2.3 架构问题（改进）

1. **深层继承链**：`PinyinKeyboard → EditorEditKeyboard → BaseKeyboard`，行为分散在多层，难以理解完整逻辑
2. **IMEService 职责过重**：既是消息路由器，又负责 InputConnection 操作和生命周期管理，职责不清
3. **手动 RecyclerView 管理**：自定义 LayoutManager、GestureDetector、Adapter，代码量巨大且维护困难
4. **线程安全风险**：`InputList` 是可变对象，从多个线程访问（主线程 UI 更新 + 异步字典查询回调）
5. **缺乏依赖注入**：所有依赖手动通过构造参数传递，随着类关系复杂化导致参数列表膨胀
6. **测试几乎缺失**：核心逻辑（InputList、状态机、消息路由）无单元测试，重构风险高
7. **枚举膨胀**：`InputMsgType` 有 35+ 值、`CtrlKey.Type` 有 25+ 值，不同关注点混在一个枚举中
8. **Favoriteboard 职责混合**：同时处理剪贴板和收藏功能，违反单一职责
9. **日志系统简陋**：`Logger` 在 release 构建返回 noop，无法记录任何日志；仅支持 Logcat 输出和内存缓存，崩溃后日志丢失；无日志查看和导出界面
10. **缺乏 UI 调试工具**：无布局边界可视化、组件信息查看等 UI 调试手段，排障效率低

---

## 3. v4 架构设计

### 3.1 分层架构

```
┌─────────────────────────────────────────────────────────────────┐
│                        Platform Layer  ← :app 模块               │
│  IMEService (薄壳) → InputConnection 桥接 → ComposeView 桥接     │
│  配置持久化（DataStore）+ 设置页面 + 引导页面                     │
├─────────────────────────────────────────────────────────────────┤
│                      ViewModel Layer   ← :app 模块               │
│  KeyboardViewModel（来自 :ime-ui）+ 配置持久化（独立）+ ImeOutputBridge 桥接    │
├─────────────────────────────────────────────────────────────────┤
│                         UI Layer      ← :ime-ui 库               │
│  Compose 缺省 UI：GestureInputPanel / KeyGridPanel / GestureFeedbackPanel    │
│  CandidateListPanel / InputListPanel / EditTextBridge / KeyboardPanel / KeyboardScreen      │
│  主题系统 / 剪贴板与收藏 UI / 输入练习 UI          │
│  (对第三方应用开放的缺省 UI 实现，可整体替换或部分替换)             │
├─────────────────────────────────────────────────────────────────┤
│                       Domain Layer     ← :ime-engine 库          │
│  ImeEngine / Keyboard / InputList / Inputboard / Favoriteboard  │
│  ImeOutputBridge / BaseImeOutputBridge                          │
│  (纯 Kotlin，不依赖 Android 框架，可独立作为库被外部程序引入)       │
├─────────────────────────────────────────────────────────────────┤
│                        Data Layer      ← :ime-engine 库          │
│  ImeDictProvider 接口 + ImeSqliteDictProvider 内置实现                  │
│  PinyinDict / UserInputDataDict / UserInputFavoriteDict          │
│  (数据库层可替换；配置通过 ImeConfig 代码设置，引擎/UI 配置明确隔离)    │
└─────────────────────────────────────────────────────────────────┘
```

> **注意**：三层库架构的详细设计见文档 160。v4 采用三层库架构：引擎库（`:ime-engine`，纯 Kotlin，Domain Layer + Data Layer）、UI 库（`:ime-ui`，Compose 缺省 UI，UI Layer）、应用模块（`:app`，Platform Layer + ViewModel Layer + 配置持久化 + 设置页面）。第三方应用可以引入 `:ime-engine` + `:ime-ui` 获得完整的输入法能力与缺省 UI，也可以仅引入 `:ime-engine` 自行实现 UI。

### 3.2 核心设计决策

| 决策 | Java 版本 | Kotlin 版本 | 理由 |
|------|-----------|-------------|------|
| 架构模式 | 自定义 MVP | MVI | 单向数据流更清晰，状态可追踪 |
| 消息系统 | 手动消息路由 | Intent + StateFlow | 类型安全、响应式、自动生命周期管理 |
| 异步 | CompletableFuture + Handler | Coroutine + Flow | 结构化并发、简化异步代码 |
| 状态管理 | 可变对象 + 手动同步 | 不可变 data class + StateFlow | 线程安全、可追踪 |
| 键盘逻辑 | 深层继承 | 组合 + Sealed class | 逻辑集中、类型安全 |
| UI | 自定义 View + RecyclerView | Jetpack Compose | 声明式、代码量减少 |
| 配置 | SharedPreferences | DataStore + Flow | 类型安全、异步、响应式 |
| 数据库 | 手写 SQLite | Room | 类型安全、编译期检查、官方推荐 |
| 依赖管理 | 手动构造 | 构造参数注入（不用 Hilt） | 项目规模适中，手动注入足够 |

**移除的依赖**：`appcompat`、`preference`、`material`（Compose 不需要）、`flexbox`（Compose 内置 FlowRow）、`gson`/`org.json`（Kotlin Serialization 替代）、`mixite`（自实现 HexGrid）。**构建变更**：移除 `alpha` 构建类型，发布包命名为 `Kuaizi_IME-{version}.apk`，字典资源移至 `assets/dict/`。

### 3.3 MVI 数据流

v4 采用 MVI（Model-View-Intent）架构，核心数据流如下：

```
GestureInputPanel → InputGesture → ImeEngine.handleGesture()
                                      ↓
                                 ImeEngine.reduce()
                                      ↓
                              StateFlow<ImeState> → KeyGridPanel（纯渲染）
                              StateFlow<ImeState> → CandidateListPanel
                              StateFlow<ImeState> → InputListPanel
                                      ↓
                              GestureFeedbackState → GestureFeedbackPanel（视觉反馈）
```

- **ImeIntent**：用户意图的 sealed class 表达，替代 Java 版本的三套消息体系（UserKeyMsg、UserInputMsg、InputMsg）。完整定义见文档 160 第 4.4 节
- **ImeState**：不可变状态 data class，通过 StateFlow 自动传播到 UI。完整定义见文档 160 第 8.1 节
- **三层面板分离**：GestureInputPanel（手势拦截层）→ GestureFeedbackPanel（反馈绘制层）→ KeyGridPanel（按键渲染层）。完整设计见文档 150
- **ImeOutput**：引擎输出 sealed class，通过 ImeOutputBridge 语义化分派到具体编辑器。引擎内部统一执行 when 分发，桥梁实现者只需实现语义方法。完整定义见文档 160 第 4.3 节

### 3.4 键盘组合模式

Java 版本通过深层继承链（如 `PinyinKeyboard → EditorEditKeyboard → BaseKeyboard`）实现键盘逻辑，v4 改为组合模式：`Keyboard` sealed class + 独立共享组件（`KeyAudioPlayer`、`InputListOperator`、`KeyHandler`、`KeyboardStateMachine`、`CandidateListPager`）。完整设计见文档 100。

### 3.5 消息系统简化

Java 版本有三套消息体系（UserKeyMsg 7 种、UserInputMsg 11 种、InputMsg 35+ 种），v4 统一为 Intent 体系：原 UserKeyMsg + UserInputMsg → `ImeIntent`；原 InputMsg → StateFlow 状态变更自动传播。ViewModel 的 `reduce` 函数处理 Intent 后更新 State，UI 层自动响应，不再需要手动分发 InputMsg。详见文档 160。

---

## 4. 数据流

### 4.1 按键输入完整流程

```
1. 用户按键
   ↓
2. Compose 手势检测 → 生成 ImeIntent.PressKey
   ↓
3. KeyboardViewModel.handleIntent(intent)
   ↓
4. reduce(state, intent)
   ├─ 更新 keyboardState（状态机转换）
   ├─ 查询 PinyinDict（协程）
   ├─ 更新 candidates
   └─ 更新 inputList
   ↓
5. _state.update { newState }
   ↓
6. Compose 订阅 StateFlow → 重组 UI
   ↓
7. 候选项、按键状态、输入栏自动更新
```

### 4.2 输入提交流程

```
1. 用户点击提交
   ↓
2. ImeIntent.CommitInput
   ↓
3. reduce 提取 inputList 的文本
   ↓
4. 通过 ImeOutputBridge.commitText() 提交到编辑器
   ↓
5. 重置 inputList 和 candidates；引擎通过 dispatchToTarget() 自动分发到已挂载的 ImeOutputBridge
   ↓
6. 状态更新 → UI 自动刷新
```

---

## 5. 与 Java 版本的架构对比

| 维度 | Java v3 | Kotlin v4 |
|------|---------|-----------|
| 架构模式 | 自定义 MVP + 手动消息路由 | MVI + StateFlow |
| 消息系统 | 3 套消息（UserKeyMsg, UserInputMsg, InputMsg） | 统一 Intent + StateFlow |
| 状态管理 | 可变对象 + 手动同步 | 不可变 data class + StateFlow |
| 异步 | CompletableFuture + Handler | Coroutine + Flow |
| UI | View + RecyclerView | Compose |
| 键盘逻辑 | 深层继承（3 层） | 组合模式 + Sealed class |
| 配置 | SharedPreferences | DataStore |
| 数据库 | 手写 SQLiteOpenHelper | Room |
| 依赖管理 | 手动构造 | 手动构造注入（同 Java，但更简洁） |
| 测试 | 几乎无 | 全面单元测试 + 截图对比 + UI 测试工具 |
| 日志 | Logger（仅 DEBUG 生效） | ImeLog（分级+持久化+崩溃拦截+查看导出） |
| UI 调试 | 无 | 内置 UI 测试工具（debug 构建，release 自动移除） |
| 输出桥接 | 手动 when 分发（2处重复） | ImeOutputBridge 语义化桥接（1处分发） |
| 输入练习 | ExerciseGuide（手动 Exercise 步骤） | InputActionPlayer + ExerciseScreen（release 可用的动作动画演示） |

---

## 6. 风险与缓解

| 风险 | 影响 | 缓解措施 |
|------|------|----------|
| Compose 在 IME 中的性能 | 键盘响应延迟 | 原型阶段性能验证，必要时降级为 View |
| 状态机迁移的复杂度 | 功能缺失 | 先写测试验证 Java 行为，再迁移 |
| 字典数据库迁移 | 数据丢失 | 保留升级路径，测试所有迁移场景 |
| X-Pad Canvas 绘制 | 视觉不一致 | 逐步迁移，与 Java 版本对比截图验证 |
| UI 测试工具在 release 中的残留 | 包体积增大、信息泄露 | Source Set 隔离 + Lint 规则 + CI 检查，三重保障 |
| 输入练习动画的包体积 | APK 增大 | Compose Canvas 即时绘制，无额外图片资源；预置脚本控制在 50KB 以内 |
| 日志系统性能影响 | I/O 阻塞主线程 | Channel 缓冲 + 独立协程批量写入，不阻塞调用线程 |
