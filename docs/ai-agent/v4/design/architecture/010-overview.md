# 架构总览

## 1 分层架构

v4 采用三层库架构：引擎库（`:ime-engine`）、UI 库（`:ime-ui`）、应用模块（`:app`），自底向上分为五层：

```
┌─────────────────────────────────────────────────────────────────┐
│                        Platform Layer  ← :app 模块               │
│  IMEService (薄壳) → 创建 ImeEngine → 挂载 InputConnectionBridge  │
│  ComposeView 桥接 → 注入 KeyboardViewModel.Factory(engine)      │
│  配置持久化（DataStore）+ 设置页面 + 引导页面                     │
├─────────────────────────────────────────────────────────────────┤
│                   ViewModel Layer   ← :ime-ui 库                 │
│  KeyboardViewModel：持有 ImeEngine，暴露 StateFlow<ImeState>     │
│  InputGesture → ImeIntent 转换 → engine.handleIntent()          │
│  GestureFeedbackState 管理 + updateConfig() 运行时配置修改       │
├─────────────────────────────────────────────────────────────────┤
│                         UI Layer      ← :ime-ui 库               │
│  Compose 缺省 UI：GestureInputPanel / KeyLayoutPanel / GestureFeedbackPanel    │
│  CandidateListPanel / InputListPanel / EditTextBridge / KeyboardHost      │
│  (KeyboardHost 统一叠加模式与全屏模式，内部包含候选栏+输入栏+工具栏+键盘区域)             │
│  主题系统 / 剪贴板与收藏 UI / 输入练习 UI          │
│  (对第三方应用开放的缺省 UI 实现，可整体替换或部分替换)             │
├─────────────────────────────────────────────────────────────────┤
│                       Domain Layer     ← :ime-engine 库          │
│  ImeEngine / Keyboard / InputList / Inputboard / Favoriteboard  │
│  ImeOutputBridge / BaseImeOutputBridge                          │
│  ImeLog / ImeLogger / LogLevel / LogEntry / LogWriter / LogStorage│
│  (逻辑层与 UI/应用分离，第三方可定制 UI 与交互)       │
├─────────────────────────────────────────────────────────────────┤
│                        Data Layer      ← :ime-engine 库          │
│  ImeDictProvider 接口 + ImeSqliteDictProvider 内置实现                  │
│  PinyinDict / UserInputDataDict / UserInputFavoriteDict          │
│  FileLogWriter (Channel 缓冲 + 异步批量写入)                       │
│  (数据库层可替换；配置通过 ImeConfig 代码设置，引擎/UI 配置明确隔离)    │
└─────────────────────────────────────────────────────────────────┘
```

```plantuml
@file:../diagrams/architecture.puml
```

> **注意**：三层库架构的详细设计见 [030-三层模块划分](./030-module-division.md)。v4 采用三层库架构：引擎库（`:ime-engine`，逻辑层与 UI / 应用分离，Domain Layer + Data Layer）、UI 库（`:ime-ui`，Compose 缺省 UI + ViewModel Layer）、应用模块（`:app`，Platform Layer + 配置持久化 + 设置页面）。第三方应用可以引入 `:ime-engine` + `:ime-ui` 获得完整的输入法能力与缺省 UI（含 KeyboardViewModel），也可以仅引入 `:ime-engine` 自行实现 UI。

---

## 2 数据流

v4 采用 MVI（Model-View-Intent）架构，核心数据流如下：

```plantuml
@file:../diagrams/mvi-data-flow.puml
```

- **KeyboardViewModel**：UI 层协调中心，将 InputGesture 转换为 ImeIntent，暴露 `StateFlow<ImeState>` 供 Compose 订阅。完整设计见 [060-KeyboardViewModel](../ui/060-keyboard-view-model.md)
- **ImeIntent**：用户意图的 sealed class 表达。完整定义见 [010-引擎库设计总览](../engine/010-engine-overview.md)
- **ImeState**：不可变状态 data class，通过 StateFlow 自动传播到 UI。完整定义见 [010-引擎库设计总览](../engine/010-engine-overview.md)
- **三层面板分离**：GestureInputPanel（手势拦截层）→ GestureFeedbackPanel（反馈绘制层）→ KeyLayoutPanel（按键渲染层）。完整设计见 [020-面板三层分离设计](../ui/020-panel-separation.md)
- **ImeOutput**：引擎输出 sealed class，通过 ImeOutputBridge 语义化分派到具体编辑器。引擎内部统一执行 when 分发，桥梁实现者只需实现语义方法。完整定义见 [010-引擎库设计总览](../engine/010-engine-overview.md)，桥接机制见 [090-输出桥接机制](../engine/090-output-bridge.md)

### 2.1 按键输入完整流程

```
1. 用户按键
   ↓
2. GestureInputPanel → InputGesture
   ↓
3. KeyboardViewModel.handleGesture() → gestureToIntent() 转换
   ↓
4. ImeIntent → KeyboardViewModel.handleIntent() → ImeEngine.handleIntent()
   ↓
5. reduce(state, intent)
   ├─ 更新 keyboardState（状态机转换）
   ├─ 查询 PinyinDict（协程）
   ├─ 更新 candidates
   └─ 更新 inputList
   ↓
6. _state.update { newState }
   ↓
7. Compose 订阅 StateFlow → 重组 UI
   ↓
8. 候选项、按键状态、输入栏自动更新
```

### 2.2 输入提交流程

```
1. 用户点击提交
   ↓
2. ImeIntent.CommitInput → KeyboardViewModel.handleIntent() → ImeEngine.handleIntent()
   ↓
3. reduce 提取 inputList 的文本
   ↓
4. 通过 ImeOutputBridge.commitText() 提交到编辑器（桥梁由 :app 的 IMEService 挂载）
   ↓
5. 重置 inputList 和 candidates；引擎通过 dispatchToTarget() 自动分发到已挂载的 ImeOutputBridge
   ↓
6. 状态更新 → UI 自动刷新
```

---

## 3 键盘组合模式

`Keyboard` sealed class + 独立共享组件（`KeyAudioPlayer`、`InputListOperator`、`KeyHandler`、`KeyboardStateMachine`、`CandidateListPager`）。完整设计见 [020-键盘状态机](../engine/020-state-machine.md)。

---

## 4 风险与缓解

| 风险 | 影响 | 缓解措施 |
|------|------|----------|
| Compose 在 IME 中的性能 | 键盘响应延迟 | 原型阶段性能验证，必要时降级为 View |
| 状态机迁移的复杂度 | 功能缺失 | 先写测试验证 Java 行为，再迁移 |
| 字典数据库迁移 | 数据丢失 | 保留升级路径，测试所有迁移场景 |
| X-Pad Canvas 绘制 | 视觉不一致 | 逐步迁移，与 Java 版本对比截图验证 |
| UI 测试工具在 release 中的残留 | 包体积增大、信息泄露 | Source Set 隔离 + Lint 规则 + CI 检查，三重保障 |
| 输入练习动画的包体积 | APK 增大 | Compose Canvas 即时绘制，无额外图片资源；预置脚本控制在 50KB 以内 |
| 日志系统性能影响 | I/O 阻塞主线程 | Channel 缓冲 + 独立协程批量写入，不阻塞调用线程（详见 [080-日志系统](../engine/080-logging.md)） |
