# UI 库设计总览

UI 库 `:ime-ui` 的核心设计目标是作为**缺省 UI 实现**对第三方应用开放。第三方应用可以直接使用库中的 Compose 组件和 `KeyboardViewModel` 构建完整的输入法界面，无需自行实现视图层或 ViewModel。同时，UI 库的设计遵循「可替换」原则：所有 UI 组件仅依赖 `:ime-engine` 的公开 API（StateFlow、Intent、ImeOutput），不依赖引擎内部实现，因此第三方应用可以完全用自定义 UI 替换 `:ime-ui` 而不影响引擎功能。

---

## 1 设计目标

**UI 库的定位**：

| 定位 | 说明 |
|------|------|
| **缺省实现** | 提供完整的、即插即用的输入法 UI，第三方应用引入后开箱即用 |
| **可替换** | 所有 UI 组件仅依赖引擎公开 API，第三方应用可自行替换任意组件 |
| **可组合** | 组件粒度合理，第三方应用可选择性使用部分组件（如只用键盘不用候选栏） |
| **可定制** | 通过主题系统（KeyboardColors）和配置参数控制外观和行为 |

UI 库的「缺省实现」定位意味着它必须提供功能完备的组件，覆盖输入法界面的所有交互场景——从按键输入、候选选择到剪贴板粘贴、收藏管理、输入练习等。第三方应用无需实现任何 UI 逻辑，只需引入 `:ime-engine` + `:ime-ui` 两个库，即可获得完整的输入法能力与界面。

「可替换」定位则要求 UI 库的组件边界清晰、接口稳定。所有组件仅依赖 `:ime-engine` 的公开 API（`ImeEngine.state: StateFlow<ImeState>`、`ImeIntent`、`ImeOutput`、`ImeOutputBridge`），不依赖引擎内部的 `reduce()` 函数、状态机实现或字典查询逻辑。这种依赖隔离确保了第三方应用可以用完全自定义的 UI 替换 `:ime-ui` 的任意组件，而不影响引擎的正常运行。

---

## 2 UI 库组件清单

UI 库中的组件按层次组织，从底层的原子组件到顶层的集成组件：

### 2.1 原子组件

原子组件是最小粒度的 UI 组件，由上层组合使用，也可单独使用：

| 组件 | 包路径 | 说明 | 对应设计文档 |
|------|--------|------|------------|
| `KeyView` | `keyboard` | 单个按键渲染（纯展示，无触摸） | 150, 400 |
| `CandidateItem` | `candidate` | 单个候选项 | 400 |
| `CharInputItem` / `GapInputItem` | `input` | 输入栏中的字符/间隙项 | 200, 400 |
| `ClipTipPopup` | `clipboard` | 剪贴板提示弹窗 | 600 |
| `FavoriteItem` | `favorite` | 收藏项（含滑动删除） | 600 |
| `FingerOverlay` | `practice` | 手指指示器（程序化输入动画） | 930 |

### 2.2 面板组件

面板组件由原子组件组合而成，可独立使用：

| 组件 | 包路径 | 说明 | 对应设计文档 |
|------|--------|------|------------|
| `GestureInputPanel` | `panel` | 透明手势拦截层 | 150 |
| `KeyGridPanel` / `StandardKeyGridPanel` | `panel` | 按键渲染层 | 150 |
| `GestureFeedbackPanel` | `panel` | 透明反馈绘制层 | 150 |
| `CandidateListPanel` | `candidate` | 候选栏 | 400 |
| `InputListPanel` | `input` | 输入栏 | 200, 400 |
| `FavoriteListPanel` | `favorite` | 收藏面板 | 600 |
| `XPadView` | `keyboard` | X-Pad 六边形面板 | 700 |
| `ActionPlayerPanel` | `practice` | 播放控制面板 | 930 |

### 2.3 集成组件

集成组件是一站式解决方案，将多个面板和功能组件组合为完整的输入法界面：

| 组件 | 包路径 | 说明 |
|------|--------|------|
| `KeyboardPanel` | `integration` | 叠加模式完整输入法组件（候选栏 + 输入栏 + 工具栏 + 三层面板叠加：GestureInputPanel + GestureFeedbackPanel + KeyGridPanel） |
| `KeyboardScreen` | `integration` | 全屏模式完整输入法组件（候选栏 + 输入栏 + 工具栏 + 手势输入面板与按键面板分离布局） |

### 2.4 ViewModel 组件

| 组件 | 包路径 | 说明 | 对应设计文档 |
|------|--------|------|------------|
| `KeyboardViewModel` | `viewmodel` | UI 层协调中心，持有 ImeEngine，暴露 `StateFlow<ImeState>`，将 InputGesture 转换为 ImeIntent，管理 GestureFeedbackState | [060-KeyboardViewModel](060-keyboard-view-model.md) |

> **注意**：编辑器桥接组件在 `:ime-engine` 中定义（ImeOutputBridge、BaseImeOutputBridge），具体实现分别在 `:app`（InputConnectionBridge）和 `:ime-ui`（EditTextBridge）中。完整设计见 [090-输出桥接机制](../engine/090-output-bridge.md)。

### 2.5 主题系统

| 组件 | 包路径 | 说明 |
|------|--------|------|
| `KeyboardColors` | `theme` | 颜色定义（键盘/候选/输入栏/X-Pad） |
| `KeyboardThemes` | `theme` | 预置主题（Light/Night） |
| `KeyboardTheme` | `theme` | 主题 Composable（支持跟随系统） |
| `LocalKeyboardColors` | `theme` | CompositionLocal 提供颜色 |

---

## 3 组件层次关系

```plantuml
@file:../diagrams/ui-component-hierarchy.puml
```

集成组件 `KeyboardPanel` 和 `KeyboardScreen` 是第三方应用最常使用的入口。它们内部组合了所有面板组件，面板组件又组合了原子组件。`KeyboardViewModel` 作为 UI 协调中心，连接引擎与视图层。第三方应用可以选择使用集成组件（最简集成）、面板组件（部分替换）或原子组件（完全自定义布局），根据需求灵活选择集成深度。

---

## 4 与引擎库的依赖关系

UI 库的所有组件仅依赖 `:ime-engine` 的公开 API，不依赖引擎内部实现：

| 依赖的引擎 API | UI 库中的使用场景 |
|---------------|-----------------|
| `ImeEngine.state: StateFlow<ImeState>` | 所有 Compose 组件通过 `collectAsState()` 订阅状态驱动重组 |
| `ImeIntent` | `KeyboardViewModel` 将 `InputGesture` 转换为 `ImeIntent` 后发送给引擎 |
| `ImeOutput` | 不直接使用（通过 `ImeOutputBridge` 分发） |
| `ImeOutputBridge` / `BaseImeOutputBridge` | `EditTextBridge` 实现用于非系统 IME 场景 |
| `ImeConfig` / `ImeConfig.UiConfig` | 主题系统、配置 UI 组件读取配置驱动界面呈现 |

这种依赖隔离是 UI 库「可替换」定位的技术保障。第三方应用可以用自己的 ViewModel 替换 `KeyboardViewModel`，只要该 ViewModel 正确地将用户操作转换为 `ImeIntent` 并发送给 `ImeEngine`，引擎就能正常工作。同理，第三方应用可以用自己的 Compose 组件替换任何 `:ime-ui` 组件，只要订阅 `StateFlow<ImeState>` 并正确渲染即可。
