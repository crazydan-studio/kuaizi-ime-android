# 000 — 架构总览

## 1. 概述

v4 版本对筷字输入法进行全面的 Kotlin 重构，保留 Java 版本的消息驱动架构核心思想，同时利用 Kotlin 和现代 Android 开发技术进行现代化改造。核心改进方向：从命令式 MVP 转向声明式 MVI，从继承链转向组合模式，从手写异步转向协程，从 View 系统转向 Compose。

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

1. **消息驱动的单向数据流**：UserMsg 从 View 层向上发送到 IMEService，再分发到 Model 层；InputMsg 从 Model 层向上发送到 IMEService，再分发到 View 层。这种单向消息流确保了数据流向的可追踪性
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
│  KeyboardViewModel（来自 :ime-ui）+ 配置持久化（独立）+ Output 桥接    │
├─────────────────────────────────────────────────────────────────┤
│                         UI Layer      ← :ime-ui 库               │
│  Compose 缺省 UI：GestureInputPanel / KeyGridPanel / GestureFeedbackPanel    │
│  CandidatePanel / InputListPanel / EditorField / KeyboardPanel          │
│  InputHostView / 主题系统 / 剪贴板与收藏 UI / 输入练习 UI          │
│  (对第三方应用开放的缺省 UI 实现，可整体替换或部分替换)             │
├─────────────────────────────────────────────────────────────────┤
│                       Domain Layer     ← :ime-engine 库          │
│  ImeEngine / Keyboard / InputList / Inputboard / Favoriteboard  │
│  (纯 Kotlin，不依赖 Android 框架，可独立作为库被外部程序引入)       │
├─────────────────────────────────────────────────────────────────┤
│                        Data Layer      ← :ime-engine 库          │
│  DictProvider 接口 + SqliteDictProvider 内置实现                  │
│  PinyinDict / UserInputDataDict / UserInputFavoriteDict          │
│  (数据库层可替换；配置通过 ImeConfig 代码设置，引擎/UI 配置明确隔离)    │
└─────────────────────────────────────────────────────────────────┘
```

> **注意**：`:ime-engine` 库模块和 `:ime-ui` 库模块的设计详见文档 160。v4 采用三层库架构：引擎库（`:ime-engine`，纯 Kotlin，Domain Layer + Data Layer）、UI 库（`:ime-ui`，Compose 缺省 UI，UI Layer）、应用模块（`:app`，Platform Layer + ViewModel Layer + 配置持久化 + 设置页面）。第三方应用可以引入 `:ime-engine` + `:ime-ui` 获得完整的输入法能力与缺省 UI，也可以仅引入 `:ime-engine` 自行实现 UI。库的配置通过 `ImeConfig`（含引擎配置 `EngineConfig` 和 UI 配置 `UiConfig` 的明确隔离）在代码中设置，不含配置持久化层；数据库层通过 `DictProvider` 接口支持外部替换；收藏、剪贴板等可选功能通过 `Feature` 标记按需禁用。`:app` 模块直接使用 UI 库的 `KeyboardViewModel`，不继承也不扩展，配置通过 `ImeConfig` 与引擎交互。

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

### 3.3 MVI 架构

```
┌──────────┐  Gesture   ┌──────────┐  Intent    ┌────────────┐  reduce   ┌──────────┐
│   输入面板   │ ──────────→│ImeEngine │ ──────────→ │  ImeEngine │ ────────→ │  State   │
│GestureInputPanel│       │.handleG..│             │  .reduce() │           │ (不可变)  │
└──────────┘            └──────────┘             └────────────┘           └──────────┘
        │                                                  │                      │
        │ FeedbackState                                    │     StateFlow<ImeState>
        │                                                  │                      │
        ▼                                                  │                      │
┌──────────────┐                     按键面板               │                      │
│  反馈面板     │                   KeyGridPanel               │                      │
│GestureFeedbackPanel │                   (纯展示)                 │                      │
│(透明绘制)    │                      ┌─────────────────────┘                      │
└──────────────┘                      ←────────────────────────────────────────────┘
```

> **注意**：输入面板、按键面板与反馈面板的三层分离设计详见文档 150。叠加顺序为：按键面板（底层）→ 反馈面板（中层）→ 输入面板（顶层）。GestureInputPanel 在顶层确保触摸事件优先到达手势检测层；GestureInputPanel 完全透明不绘制任何内容，不会遮挡中层反馈面板的视觉效果。反馈面板支持多实例，可灵活叠加在输入面板或按键面板所在区域。

**Intent 定义**（完整定义见文档 160 第 4 节）：

```kotlin
sealed class ImeIntent {
    // 键盘按键
    data class KeyPressed(val key: InputKey, val gesture: KeyGesture) : ImeIntent()
    data class KeyLongPressed(val key: InputKey) : ImeIntent()

    // 候选选择
    data class CandidateSelected(val candidate: InputWord) : ImeIntent()
    data class CandidatePaged(val direction: PageDirection) : ImeIntent()

    // 键盘切换
    data class SwitchKeyboard(val type: KeyboardType) : ImeIntent()

    // 编辑操作
    data object CommitInput : ImeIntent()
    data object DeleteInput : ImeIntent()
    data object CleanInput : ImeIntent()
    data class EditAction(val action: EditorAction) : ImeIntent()

    // 剪贴板与收藏
    data class ClipPasted(val clip: InputClip) : ImeIntent()
    data class FavoriteSaved(val text: String) : ImeIntent()

    // 配置变更
    data class ConfigChanged(val config: ImeConfig) : ImeIntent()
}
```

**State 定义**（完整定义见文档 160 第 8 节）：

```kotlin
data class ImeState(
    val keyboardType: KeyboardType = KeyboardType.Pinyin,
    val keyboardState: KeyboardState = KeyboardState.Idle,
    val keyGrid: List<List<InputKey>> = emptyList(),
    val inputList: InputListState = InputListState(),
    val candidates: CandidateState = CandidateState(),
    val clipboard: ClipboardState = ClipboardState(),
    val favorites: FavoritesState = FavoritesState(),
    val config: ImeConfig = ImeConfig(), // 含 engine（引擎配置）和 ui（UI 配置）
)
```

### 3.4 键盘逻辑的组合模式

Java 版本的键盘继承链：

```
PinyinKeyboard → EditorEditKeyboard → BaseKeyboard
LatinKeyboard → DirectInputKeyboard → BaseKeyboard
NumberKeyboard → DirectInputKeyboard → BaseKeyboard
MathKeyboard → BaseKeyboard
SymbolKeyboard → BaseKeyboard
EmojiKeyboard → BaseKeyboard
EditorKeyboard → BaseKeyboard
InputCandidateKeyboard → BaseKeyboard
PinyinCandidateKeyboard → BaseKeyboard
InputListCommitOptionKeyboard → BaseKeyboard
```

v4 版本改为组合模式（完整设计见文档 100 第 4 节）：

```kotlin
sealed class Keyboard {
    abstract val type: KeyboardType
    abstract val state: KeyboardState
    abstract fun handleIntent(intent: ImeIntent): KeyboardResult
}
```

**共享行为提取为独立组件**：

| Java BaseKeyboard 行为 | Kotlin 独立组件 |
|----------------------|-----------------|
| 音频播放 | `KeyAudioPlayer` |
| InputList 操作 | `InputListOperator` |
| 按键消息处理 | `KeyHandler`（各键盘独立实现） |
| 状态机管理 | `KeyboardStateMachine` |
| 候选分页 | `CandidatePager` |

### 3.5 消息系统简化

Java 版本有三套消息体系：
- `UserKeyMsg` / `UserKeyMsgType`：按键消息（7 种类型）
- `UserInputMsg` / `UserInputMsgType`：输入消息（11 种类型）
- `InputMsg` / `InputMsgType`：模型→视图消息（35+ 种类型）

v4 统一为 Intent 体系：

```kotlin
sealed class ImeIntent {
    // 原 UserKeyMsg + UserInputMsg → 统一为 Intent
    // 原 InputMsg → StateFlow 的状态变更自动传播到 UI
}
```

> **关键改变**：Java 版本需要手动分发 InputMsg 到 View 层，Kotlin 版本通过 StateFlow 自动传播状态变更，不再需要 InputMsg 体系。ViewModel 的 `reduce` 函数处理 Intent 后更新 State，UI 层自动响应。

---

## 4. 数据流

### 4.1 按键输入完整流程

```
1. 用户按键
   ↓
2. Compose 手势检测 → 生成 ImeIntent.KeyPressed
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
4. 通过 InputConnection.commitText() 提交到编辑器
   ↓
5. 重置 inputList 和 candidates
   ↓
6. 状态更新 → UI 自动刷新
```

---

## 5. 技术选型

### 5.1 核心依赖

| 依赖 | 版本 | 用途 |
|------|------|------|
| Kotlin | 2.3.20 | 开发语言 |
| Compose BOM | 2026.04.01 | UI 框架 |
| Compose Material3 | BOM 管理 | 主题和组件 |
| Lifecycle ViewModel | 最新稳定版 | ViewModel 生命周期 |
| Kotlin Coroutines | Kotlin 自带 | 异步编程 |
| DataStore | 最新稳定版 | 配置存储 |
| Room | 最新稳定版 | 字典数据库 |
| Flexbox | 3.0.0 → Compose FlowRow | 候选栏布局 |
| Compose UI Tooling | BOM 管理（debugImplementation） | UI 测试工具（仅 debug） |
| Paparazzi | 最新稳定版（testImplementation） | 截图对比测试（仅 test） |

### 5.2 移除的依赖

| Java 版本依赖 | 移除原因 | 替代方案 |
|--------------|----------|----------|
| `androidx.appcompat:appcompat` | Compose 不需要 | Compose Material3 |
| `androidx.preference:preference` | Compose 自建设置页 | Compose 组件 |
| `com.google.android.material:material` | Compose 不需要 | Compose Material3 |
| `com.google.android.flexbox:flexbox` | Compose 内置 | `FlowRow` / `LazyRow` |
| `com.google.code.gson:gson` | 测试专用 | Kotlin Serialization |
| `org.json:json` | 测试专用 | Kotlin Serialization |
| `com.github.Hexworks.Mixite:mixite.core-jvm` | X-Pad 自实现六边形网格 | 自实现 `HexGrid` |

### 5.3 移除的构建类型

Java 版本有三个构建类型：`debug`、`release`、`alpha`。v4 版本移除 `alpha` 构建类型，仅保留 `debug` 和 `release`：

| 构建类型 | Java v3 | Kotlin v4 | 说明 |
|----------|---------|-----------|------|
| debug | ✅ | ✅ | 调试构建，applicationIdSuffix ".debug" |
| release | ✅ | ✅ | 发布构建，混淆+压缩 |
| alpha | ✅ | ❌ 移除 | 不再需要独立测试渠道 |

**移除 alpha 的理由**：
- alpha 变体的唯一作用是提供独立的测试安装渠道，可通过 debug 变体或 flavor 替代
- 减少 `alpha/` 源集、`pack-alpha.sh` 脚本、alpha 专用资源等维护成本
- 简化构建配置，降低 CI/CD 复杂度

### 5.4 构建配置变更

| 配置项 | Java v3 | Kotlin v4 |
|--------|---------|-----------|
| 发布包命名 | 默认 Gradle 命名 | `Kuaizi_IME-{version}.apk` |
| 签名证书路径 | `keystore/release.properties`（相对于 code 目录） | `keystore/release.properties`（相对于项目根目录） |
| 字典资源位置 | `res/raw/` | `assets/dict/` |
| alpha 构建脚本 | `tools/pack-alpha.sh` | 移除 |

### 5.5 字典数据库方案

**选择 Room**，理由：
1. 项目仅 Android 平台，Room 是 Android 官方推荐 ORM
2. 已有手写 SQL 逻辑，Room 的迁移路径更直接
3. Room 提供 LiveData/Flow 集成，适合响应式架构
4. 编译期 SQL 检查，避免运行时 SQL 错误

> **决策说明**：现阶段直接采用 Room 框架，后续视情况决定是否更换方案。详细的字典系统设计见文档 300。

---

## 6. 与 Java 版本的架构对比

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
| 日志 | Logger（仅 DEBUG 生效） | AppLog（分级+持久化+崩溃拦截+查看导出） |
| UI 调试 | 无 | 内置 UI 测试工具（debug 构建，release 自动移除） |
| 输入练习 | ExerciseGuide（手动 Exercise 步骤） | InputActionPlayer + ExerciseScreen（release 可用的动作动画演示） |

---

## 7. 风险与缓解

| 风险 | 影响 | 缓解措施 |
|------|------|----------|
| Compose 在 IME 中的性能 | 键盘响应延迟 | 原型阶段性能验证，必要时降级为 View |
| 状态机迁移的复杂度 | 功能缺失 | 先写测试验证 Java 行为，再迁移 |
| 字典数据库迁移 | 数据丢失 | 保留升级路径，测试所有迁移场景 |
| X-Pad Canvas 绘制 | 视觉不一致 | 逐步迁移，与 Java 版本对比截图验证 |
| UI 测试工具在 release 中的残留 | 包体积增大、信息泄露 | Source Set 隔离 + Lint 规则 + CI 检查，三重保障 |
| 输入练习动画的包体积 | APK 增大 | Compose Canvas 即时绘制，无额外图片资源；预置脚本控制在 50KB 以内 |
| 日志系统性能影响 | I/O 阻塞主线程 | Channel 缓冲 + 独立协程批量写入，不阻塞调用线程 |
