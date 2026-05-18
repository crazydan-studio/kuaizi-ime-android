# UI 库设计总览

UI 库 `:ime-ui` 的核心设计目标是作为**缺省 UI 实现**对第三方应用开放。第三方应用可以直接使用库中的 Compose 组件和 `KeyboardViewModel` 构建完整的输入法界面，无需自行实现视图层或 ViewModel。同时，UI 库的设计遵循「可替换」原则：所有 UI 组件仅依赖 `:ime-engine` 的公开 API（StateFlow、Intent、ImeOutput），不依赖引擎内部实现，因此第三方应用可以完全用自定义 UI 替换 `:ime-ui` 而不影响引擎功能。

---

## 1 设计目标

| 定位 | 说明 |
|------|------|
| **缺省实现** | 提供完整的、即插即用的输入法 UI，第三方应用引入后开箱即用 |
| **可替换** | 所有 UI 组件仅依赖引擎公开 API，第三方应用可自行替换任意组件 |
| **可组合** | 组件粒度合理，第三方应用可选择性使用部分组件（如只用键盘不用候选栏） |
| **可定制** | 通过主题系统（KeyboardColors）和配置参数控制外观和行为 |

UI 库的「缺省实现」定位意味着它必须提供功能完备的组件，覆盖输入法界面的所有交互场景。第三方应用无需实现任何 UI 逻辑，只需引入 `:ime-engine` + `:ime-ui` 两个库，即可获得完整的输入法能力与界面。「可替换」定位则要求组件边界清晰、接口稳定，所有组件仅依赖 `:ime-engine` 公开 API，确保第三方应用可替换任意组件而不影响引擎运行。

---

## 2 UI 库组件清单

### 2.1 原子组件

最小粒度 UI 组件，由上层组合使用，也可单独使用。

| 组件 | 包路径 | 说明 |
|------|--------|------|
| `KeyView` | `keyboard` | 单个按键渲染（纯展示，无触摸） |
| `CandidateItem` | `candidate` | 单个候选项 |
| `CharInputItem` / `GapInputItem` | `input` | 输入栏中的字符 / 间隙项 |
| `PopupTipItem` | `panel` | 弹出提示内容项 |
| `ToolItem` | `panel` | 工具按钮项（含原 Editor 编辑功能键） |

### 2.2 面板组件

由原子组件组合而成，可独立使用。

| 组件 | 包路径 | 说明 |
|------|--------|------|
| `GestureInputPanel` | `panel` | 透明手势拦截层 |
| `KeyLayoutPanel` | `panel` | 按键布局渲染层（原 KeyGridPanel，强调布局渲染职责） |
| `GestureFeedbackPanel` | `panel` | 透明反馈绘制层（归一化坐标绘制） |
| `CandidateListPanel` | `candidate` | 候选栏（含内建 IndicatorOverlay） |
| `PopupTipPanel` | `panel` | 弹出提示面板（新，替代 InfoTipPanel/ClipTipPopup，从 ImeState 读取状态） |
| `ToolListPanel` | `panel` | 工具列表面板（新，替代 Toolbar，含原 Editor 编辑功能键，含内建 IndicatorOverlay） |
| `InputListPanel` | `input` | 输入栏（含内建 IndicatorOverlay） |

### 2.3 集成组件

一站式解决方案，将多个面板组合为完整输入法界面。

| 组件 | 包路径 | 说明 |
|------|--------|------|
| `KeyboardHost` | `integration` | 统一集成组件（替代 KeyboardPanel + KeyboardScreen），通过 LayoutMode 参数支持 Stacked/Separated 两种布局 |
| `KeyboardInputActionPlayerHost` | `integration` | 演示集成组件（新，替代 ExerciseScreen），支持 Animation/DirectInput 两种 UseMode |

### 2.4 ViewModel 组件

| 组件 | 包路径 | 说明 |
|------|--------|------|
| `KeyboardViewModel` | `viewmodel` | UI 协调中心，持有 ImeEngine，暴露 `StateFlow<ImeState>`，将 InputGesture 转换为 ImeIntent，管理 GestureFeedbackState、layoutMode 及 InputActionPlayer |

> 编辑器桥接组件在 `:ime-engine` 中定义（ImeOutputBridge、BaseImeOutputBridge），实现分别在 `:app`（InputConnectionBridge）和 `:ime-ui`（EditTextBridge）。详见 [090-输出桥接机制](../engine/090-output-bridge.md)。

### 2.5 主题系统

| 组件 | 包路径 | 说明 |
|------|--------|------|
| `KeyboardColors` | `theme` | 颜色定义（键盘 / 候选 / 输入栏 / X-Pad） |
| `KeyboardThemes` | `theme` | 预置主题（Light/Night） |
| `KeyboardTheme` | `theme` | 主题 Composable（支持跟随系统） |
| `LocalKeyboardColors` | `theme` | CompositionLocal 提供颜色 |

### 2.6 工具组件

| 组件 | 包路径 | 说明 |
|------|--------|------|
| `CoordinateNormalizer` | `util` | 归一化坐标转换工具，在逻辑坐标与像素坐标之间双向转换 |

---

## 3 组件层次关系

以下为组件层次树（详细设计见 [020-面板三层分离与屏幕布局设计](020-panel-separation.md)）。

```
KeyboardHost (集成组件)
├── Zone A Container (仅 Separated 模式)
│   ├── KeyLayoutPanel (单实例)
│   │   └── KeyView (原子组件) x N
│   └── GestureFeedbackPanel (Zone A 实例)
│       └── Canvas (归一化坐标绘制)
└── Zone B Container
    ├── Row 1
    │   ├── CandidateListPanel (含内建 IndicatorOverlay)
    │   │   └── CandidateItem x N
    │   └── PopupTipPanel (叠加)
    │       └── PopupTipItem
    ├── Row 2
    │   ├── ToolListPanel (空闲时，含内建 IndicatorOverlay)
    │   │   └── ToolItem x N
    │   └── InputListPanel (输入时互斥，含内建 IndicatorOverlay)
    │       ├── CharInputItem x N
    │       └── GapInputItem x N
    └── Row 3
        ├── Stacked: KeyLayoutPanel + GestureFeedbackPanel + GestureInputPanel (叠加)
        └── Separated: 左右列功能按钮 + 中列 GestureFeedbackPanel + GestureInputPanel

KeyboardInputActionPlayerHost (演示集成组件)
├── Animation 模式: KeyboardHost (showIndicator=true)
└── DirectInput 模式: KeyboardHost (showIndicator=false)
```

KeyboardHost 替代 KeyboardPanel 和 KeyboardScreen，通过 LayoutMode 统一 Stacked/Separated 入口。KeyboardInputActionPlayerHost 在 KeyboardHost 基础上叠加播放引擎，专用于输入动作演示。行指示器已内建到 CandidateListPanel、InputListPanel、ToolListPanel 中，通过 showIndicator 参数控制，消除独立覆盖层。

---

## 4 与引擎库的依赖关系

UI 库的所有组件仅依赖 `:ime-engine` 的公开 API：

| 依赖的引擎 API | UI 库中的使用场景 |
|---------------|-----------------|
| `ImeEngine.state: StateFlow<ImeState>` | 所有 Compose 组件通过 `collectAsState()` 订阅状态驱动重组 |
| `ImeIntent` | `KeyboardViewModel` 将 `InputGesture` 转换为 `ImeIntent` 后发送给引擎 |
| `ImeOutput` | 不直接使用（通过 `ImeOutputBridge` 分发） |
| `ImeOutputBridge` / `BaseImeOutputBridge` | `EditTextBridge` 实现用于非系统 IME 场景 |
| `ImeConfig` / `ImeConfig.UiConfig` | 主题系统、配置 UI 组件读取配置驱动界面呈现 |
| `InputMode` 枚举 | KeyLayoutPanel 布局策略选择、GestureInputPanel 手势识别逻辑 |
| `InputActionPlaybackState` | InputActionPlayer 播放状态管理 |
| `InputActionFingerIndicator` | GestureFeedbackPanel 手指指示器渲染 |
| `InputActionRowIndicator` | CandidateListPanel/InputListPanel/ToolListPanel 内建行指示器 |
| `InputActionPathInterpolator` | InputActionPlayer 轨迹插值计算 |
| `InputActionPositionResolver` | InputActionPlayer 坐标解析 |
| `OffsetF` / `RectF` | 归一化坐标类型，CoordinateNormalizer 与 GestureFeedbackPanel 使用 |

这种依赖隔离是 UI 库「可替换」定位的技术保障。第三方应用可以用自己的 ViewModel 替换 `KeyboardViewModel`，只要正确将用户操作转换为 `ImeIntent` 发送给 `ImeEngine`；同理，可以替换任何 `:ime-ui` 组件，只要订阅 `StateFlow<ImeState>` 并正确渲染。
