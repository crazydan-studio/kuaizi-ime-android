# UI 库架构总览

UI 库 `:ime-ui` 的核心设计目标是作为**缺省 UI 实现**对第三方应用开放。第三方应用可以直接使用库中的 Compose 组件和 `KeyboardViewModel` 构建完整的输入法界面，无需自行实现视图层或 ViewModel。同时，UI 库的设计遵循「可替换」原则：所有 UI 组件仅依赖 `:ime-engine` 的公开 API（`StateFlow`、`ImeIntent`、`ImeOutput`），不依赖引擎内部实现，因此第三方应用可以完全用自定义 UI 替换 `:ime-ui` 而不影响引擎功能。

---

## 1 设计目标

| 定位 | 说明 |
|------|------|
| **缺省实现** | 提供完整的、即插即用的输入法 UI，第三方应用引入后开箱即用 |
| **可替换** | 所有 UI 组件仅依赖引擎公开 API，第三方应用可自行替换任意组件 |
| **可组合** | 组件粒度合理，第三方应用可选择性使用部分组件（如只用键盘不用候选栏） |
| **可定制** | 通过主题系统（`KeyboardColors`）和配置参数控制外观和行为 |

UI 库的「缺省实现」定位意味着它必须提供功能完备的组件，覆盖输入法界面的所有交互场景。第三方应用无需实现任何 UI 逻辑，只需引入 `:ime-engine` + `:ime-ui` 两个库，即可获得完整的输入法能力与界面。「可替换」定位则要求组件边界清晰、接口稳定，所有组件仅依赖 `:ime-engine` 公开 API，确保第三方应用可替换任意组件而不影响引擎运行。

「可组合」定位要求组件粒度合理，既不过度拆分导致组合复杂度飙升，也不过度聚合导致无法复用。`KeyboardHost` 作为一站式集成组件提供了最便捷的使用方式，而 `KeyLayoutPanel`、`CandidateListPanel` 等面板组件则允许第三方应用按需组合。「可定制」定位通过 `KeyboardColors` 主题系统和 `ImeConfig` 配置参数实现，外观和行为均可在不修改组件代码的前提下调整。四个设计目标之间没有优先级之分，它们共同构成了 UI 库的架构约束，任何设计决策都需要在这四个维度上取得平衡。

---

## 2 组件清单

### 2.1 原子组件

最小粒度 UI 组件，由上层组合使用，也可单独使用。

| 组件 | 包路径 | 说明 |
|------|--------|------|
| `KeyView` | `keyboard` | 单个按键渲染（纯展示，无触摸） |
| `CandidateItem` | `candidate` | 单个候选项 |
| `CharInputItem` / `GapInputItem` | `input` | 输入栏中的字符 / 间隙项 |
| `PopupTipItem` | `panel` | 弹出提示内容项（支持 `Message` 和 `Action` 两种类型） |
| `ToolItem` | `panel` | 工具按钮项（含编辑功能键） |

原子组件是 UI 库中最小的可复用单元，每个原子组件仅负责单一视觉元素的渲染。`KeyView` 渲染单个按键的视觉状态（标签、背景、激活/禁用等持续性状态），不处理触摸事件。`CandidateItem` 渲染单个候选项的文本和选中态。`CharInputItem` 和 `GapInputItem` 分别渲染输入栏中的已输入字符和光标间隙。`PopupTipItem` 渲染弹出提示的内容，根据 `PopupTipState.Message` 或 `PopupTipState.Action` 类型呈现不同的交互方式：`Message` 类型仅展示文本信息并自动消失，`Action` 类型展示文本和可点击的操作按钮。`ToolItem` 渲染工具栏中的功能按钮（如全选、复制、粘贴等），点击后发送关联的 `ImeIntent`。

### 2.2 面板组件

由原子组件组合而成，可独立使用。

| 组件 | 包路径 | 说明 |
|------|--------|------|
| `GestureInputPanel` | `panel` | 透明手势拦截层，唯一触摸事件接收者 |
| `KeyLayoutPanel` | `panel` | 按键布局渲染层，纯展示 |
| `GestureFeedbackPanel` | `panel` | 透明反馈绘制层（归一化坐标绘制），唯一视觉反馈绘制者 |
| `CandidateListPanel` | `candidate` | 候选栏（含内建 `IndicatorOverlay`） |
| `PopupTipPanel` | `panel` | 弹出提示面板（订阅 `PopupTipState`，支持 `Message` 和 `Action` 两种类型） |
| `ToolListPanel` | `panel` | 工具列表面板（含编辑功能键，含内建 `IndicatorOverlay`） |
| `InputListPanel` | `input` | 输入栏（含内建 `IndicatorOverlay`） |

面板组件是原子组件的上层组合，每个面板组件负责一个完整功能区域的渲染和交互协调。`GestureInputPanel`、`KeyLayoutPanel` 和 `GestureFeedbackPanel` 构成三层分离架构的核心，分别负责触摸事件接收、按键状态渲染和手势视觉反馈绘制（详见 [020-面板三层分离与屏幕布局](020-panel-separation.md)）。`CandidateListPanel` 展示候选词列表，`PopupTipPanel` 展示弹出提示，`ToolListPanel` 展示工具按钮列表，`InputListPanel` 展示当前输入内容。`CandidateListPanel`、`ToolListPanel` 和 `InputListPanel` 均内建了指示器绘制能力，通过 `showIndicator` 参数控制是否在面板内部绘制输入动作播放的指示器动画。

### 2.3 集成组件

一站式解决方案，将多个面板组合为完整输入法界面。

| 组件 | 包路径 | 说明 |
|------|--------|------|
| `KeyboardHost` | `integration` | 统一集成组件，通过 `KeyboardLayoutMode` 参数支持 `Stacked`/`Separated` 两种布局 |
| `KeyboardInputActionPlayerHost` | `integration` | 演示集成组件，支持 `Animation`/`DirectInput` 两种 `UseMode` |
| `InputActionPlayerPanel` | `integration` | 输入练习播放控制面板（注：`ExerciseScreen` 属于 `:app` 模块） |

集成组件是 UI 库提供的最高层组合，将多个面板组件组装为功能完整的输入法界面。`KeyboardHost` 是最核心的集成组件，它根据 `KeyboardLayoutMode` 决定 `Stacked` 或 `Separated` 布局，将 `CandidateListPanel`、`PopupTipPanel`、`ToolListPanel`/`InputListPanel`、`KeyLayoutPanel`、`GestureFeedbackPanel` 和 `GestureInputPanel` 按照三层分离架构和三行结构组装。`KeyboardInputActionPlayerHost` 在 `KeyboardHost` 基础上叠加输入动作播放引擎，支持演示和输入辅助两种模式。`InputActionPlayerPanel` 提供播放控制 UI（播放、暂停、进度条等），与 `KeyboardInputActionPlayerHost` 配合使用。

### 2.4 ViewModel 组件

| 组件 | 包路径 | 说明 |
|------|--------|------|
| `KeyboardViewModel` | `viewmodel` | UI 协调中心，持有 `ImeEngine`，暴露 `StateFlow<ImeState>`，将 `InputGesture` 转换为 `ImeIntent`，管理 `GestureFeedbackState`、`PopupTipState`、`ToolListState`、`layoutMode` 及 `InputActionPlayer` |

`KeyboardViewModel` 是 UI 层的协调中心，桥接 Compose UI 组件与 `:ime-engine` 引擎。它不属于 `:app` 模块，而是归属于 `:ime-ui` 模块，确保任何引入 `:ime-engine` + `:ime-ui` 的第三方应用都能获得即插即用的 ViewModel。ViewModel 的核心职责包括：手势/意图分发（`handleGesture()`、`handleIntent()`）、状态暴露（`state`、`config`、`layoutMode`、`feedbackState`）、弹出提示管理（订阅引擎 `ImeEffect` 通道，驱动 `PopupTipPanel`）、工具列表管理（根据 `keyboard.type` 动态配置）、布局模式管理（`KeyboardLayoutMode` 运行时切换）、输入动作播放器集成（`InputActionPlayer`）以及布局状态缓存（供播放器坐标解析）。ViewModel 仅依赖引擎公开 API，不持有 `InputConnectionBridge`，不执行配置持久化，不创建或销毁引擎（详见 [030-键盘视图模型](030-keyboard-view-model.md)）。

### 2.5 主题系统

| 组件 | 包路径 | 说明 |
|------|--------|------|
| `KeyboardColors` | `theme` | 颜色定义（键盘 / 候选 / 输入栏 / 弹出提示 / 工具栏） |
| `KeyboardThemes` | `theme` | 预置主题（`Light`/`Night`） |
| `KeyboardTheme` | `theme` | 主题 `Composable`（支持跟随系统） |
| `LocalKeyboardColors` | `theme` | `CompositionLocal` 提供颜色 |

主题系统为所有 UI 组件提供统一的颜色体系。`KeyboardColors` 定义了键盘界面各区域的颜色变量，包括按键背景色、按键前景色、按键激活色、候选栏背景色、候选栏前景色、输入栏背景色、输入栏前景色、弹出提示背景色、弹出提示前景色、工具栏背景色等。`KeyboardThemes` 提供两套预置主题——亮色（`Light`）和暗色（`Night`），分别对应 `KeyboardColors` 的不同配色方案。`KeyboardTheme` 是顶层 `Composable`，根据 `ImeConfig.ui.themeType` 选择主题，通过 `CompositionLocal` 将 `KeyboardColors` 向下传递。第三方应用可以通过实现自定义的 `KeyboardColors` 完全替换配色方案，也可以通过 `ImeConfig.ui` 调整部分颜色参数。

### 2.6 按键生成组件

| 组件 | 包路径 | 说明 |
|------|--------|------|
| `KeyTableGenerator` | `keyboard` | 按键布局生成器接口，根据键盘类型、输入模式、键盘状态和相关数据生成按键布局矩阵 |
| `KeyTableContext` | `keyboard` | 按键生成上下文，包含 `config`、`keyboard`、`inputList`、`candidateList` |

`KeyTableGenerator` 是按键布局生成器接口，归属于 `:ime-ui` 模块，因为按键布局是 UI 关注点，由 UI 层根据键盘状态决定按键的排列和显示。`KeyTableGenerator` 根据 `KeyTableContext` 中的信息生成 `List<List<InputKey>>` 二维矩阵，每一行对应键盘的一行按键，每个 `InputKey` 描述按键的语义标识和标签内容。`KeyTableContext` 包含 `config: ImeConfig`（运行时配置）、`keyboard: Keyboard`（当前键盘实例，含 `type`、`mode`、`state` 三个维度）、`inputList: InputList`（当前输入列表，用于确定功能键状态）、`candidateList: CandidateList`（当前候选列表，用于确定候选键内容）。不同 `KeyboardInputMode`（`HexGrid` 和 `RectGrid`）下，`KeyTableGenerator` 的实现可能不同——`HexGrid` 生成六边形排列的按键矩阵，`RectGrid` 生成矩形排列的按键矩阵。

```kotlin
interface KeyTableGenerator {
    fun generate(context: KeyTableContext): List<List<InputKey>>
}

data class KeyTableContext(
    val config: ImeConfig,
    val keyboard: Keyboard,
    val inputList: InputList,
    val candidateList: CandidateList,
)
```

### 2.7 工具组件

| 组件 | 包路径 | 说明 |
|------|--------|------|
| `CoordinateNormalizer` | `util` | 归一化坐标转换工具，在逻辑坐标与像素坐标之间双向转换 |

`CoordinateNormalizer` 是坐标归一化工具，在绝对像素坐标与归一化坐标 `[0,1]x[0,1]` 之间进行双向转换。归一化坐标使得手势反馈数据可以跨面板、跨 Zone 使用，无需关心面板的实际像素尺寸。`GestureInputPanel` 将触摸事件坐标归一化后写入 `GestureFeedbackState`，`GestureFeedbackPanel` 读取归一化坐标后根据面板实际尺寸反归一化绘制。`ComposeInputActionPositionResolver` 在解析候选项和输入项位置时，将像素坐标归一化后返回。归一化坐标的约定：`(0, 0)` 对应面板左上角，`(1, 1)` 对应面板右下角，X 轴向右为正，Y 轴向下为正。

---

## 3 组件层次关系

以下为组件层次树，展示 `:ime-ui` 模块各组件之间的组合关系（详细设计见 [020-面板三层分离与屏幕布局](020-panel-separation.md)）。

```plantuml
@file:../diagrams/ui-component-hierarchy.puml
```

`KeyboardHost` 是最顶层的集成组件，通过 `KeyboardLayoutMode` 统一 `Stacked`/`Separated` 入口。`KeyboardHost` 订阅 `KeyboardViewModel` 的 `state`、`layoutMode`、`feedbackState`、`popupTipState`、`toolListState` 等状态，根据布局模式选择不同的组件部署方式。在 `Stacked` 布局下，所有面板集中在 Zone B 的三行结构中；在 `Separated` 布局下，`KeyLayoutPanel` 迁移到 Zone A，其余面板仍在 Zone B。`KeyboardInputActionPlayerHost` 在 `KeyboardHost` 基础上叠加播放引擎，专用于输入动作演示。

组件层次的核心设计原则是「三层分离」——`GestureInputPanel`（触摸）、`KeyLayoutPanel`（渲染）、`GestureFeedbackPanel`（反馈）各自独立，通过 `InputGesture`、`GestureFeedbackState` 和 `ImeState` 解耦通信。指示器已内建到 `CandidateListPanel`、`InputListPanel`、`ToolListPanel` 中，通过 `showIndicator` 参数控制，消除了独立覆盖层组件。`PopupTipPanel` 订阅 `PopupTipState`，支持 `Message`（纯文本，自动消失）和 `Action`（含操作按钮，可点击触发 `ImeIntent`）两种类型的弹出提示。

```
KeyboardHost
├── KeyboardTheme
│   └── KeyboardLayout (Stacked / Separated)
│       ├── Zone A (Separated 模式)
│       │   ├── KeyLayoutPanel
│       │   └── GestureFeedbackPanel
│       └── Zone B
│           ├── Row 1: CandidateListPanel + PopupTipPanel (叠加)
│           ├── Row 2: ToolListPanel ↔ InputListPanel (互斥)
│           └── Row 3: KeyLayoutPanel + GestureFeedbackPanel + GestureInputPanel (三层叠加)

KeyboardInputActionPlayerHost
├── KeyboardHost (含指示器参数)
└── InputActionPlayerPanel

KeyboardViewModel
├── state: StateFlow<ImeState>
├── config: ImeConfig
├── layoutMode: StateFlow<KeyboardLayoutMode>
├── feedbackState: GestureFeedbackState
├── popupTipState: StateFlow<PopupTipState?>
├── toolListState: StateFlow<ToolListState>
├── isInputting: Boolean
├── actionPlayer: InputActionPlayer
└── 布局状态缓存
```

---

## 4 引擎依赖

UI 库的所有组件仅依赖 `:ime-engine` 的公开 API：

| 依赖的引擎 API | UI 库中的使用场景 |
|---------------|-----------------|
| `ImeEngine.state: StateFlow<ImeState>` | 所有 Compose 组件通过 `collectAsState()` 订阅状态驱动重组 |
| `ImeEngine.effect: SharedFlow<ImeEffect>` | `KeyboardViewModel` 订阅副作用通道，驱动 `PopupTipState`、音效播放、收藏确认 |
| `ImeIntent` | `KeyboardViewModel` 将 `InputGesture` 转换为 `ImeIntent` 后发送给引擎，`ToolItem` 点击直接发送 `ImeIntent`，`PopupTipState.Action` 点击触发 `ImeIntent` |
| `ImeOutput` | 不直接使用（通过 `ImeOutputBridge` 分发） |
| `ImeOutputBridge` / `BaseImeOutputBridge` | `EditTextBridge` 实现用于非系统 IME 场景 |
| `ImeEffect` | `KeyboardViewModel` 订阅引擎副作用通道，处理 `PopupTip.Message`、`PopupTip.Action`、`PlayAudio`、`ConfirmFavorite` |
| `ImeConfig` / `ImeConfig.UiConfig` | 主题系统、配置 UI 组件读取配置驱动界面呈现 |
| `KeyboardInputMode` 枚举 | `KeyLayoutPanel` 布局策略选择、`GestureInputPanel` 手势识别逻辑。`KeyboardInputMode` 与 `KeyboardType` 是 `:ime-engine` 中两个不同的概念：`KeyboardType` 是引擎的键盘分类（`Pinyin`/`Latin`/`Symbol`/`Emoji`/`Number`/`Math`），决定按键集合的语义内容；`KeyboardInputMode` 是输入交互范式分类（`HexGrid`/`RectGrid`），决定按键的几何排列和手势交互方式。二者正交组合 |
| `InputActionPlayerState` | `InputActionPlayer` 播放状态管理 |
| `InputActionFingerIndicator` | 手指指示器渲染，绘制代表手指的图形并跟随滑行轨迹移动，以及手指的点击动画（供 `CandidateListPanel`/`InputListPanel`/`ToolListPanel` 内建绘制及 `GestureFeedbackPanel` 绘制） |
| `InputActionPathInterpolator` | `InputActionPlayer` 轨迹插值计算 |
| `InputActionPositionResolver` | `ComposeInputActionPositionResolver` 实现的接口，将语义标识解析为归一化坐标 |
| `OffsetF` / `RectF` | 归一化坐标类型，`CoordinateNormalizer` 与 `GestureFeedbackPanel` 使用 |

这种依赖隔离是 UI 库「可替换」定位的技术保障。第三方应用可以用自己的 ViewModel 替换 `KeyboardViewModel`，只要正确将用户操作转换为 `ImeIntent` 发送给 `ImeEngine`、正确订阅 `ImeEffect` 副作用通道；同理，可以替换任何 `:ime-ui` 组件，只要订阅 `StateFlow<ImeState>` 并正确渲染。UI 库不依赖引擎内部实现细节（如状态机的 `reduce` 逻辑、字典查询机制、剪贴板访问方式等），仅依赖引擎公开的类型和 API，确保引擎内部重构不会影响 UI 层。

---

## 5 `KeyTableGenerator` 接口

`KeyTableGenerator` 是按键布局生成器接口，根据 `KeyTableContext` 中的上下文信息生成按键布局矩阵。该接口的设计使得按键布局逻辑可以按 `KeyboardInputMode` 和 `KeyboardType` 的组合进行策略分发——不同组合可以注册不同的 `KeyTableGenerator` 实现，由 `KeyLayoutPanel` 根据当前 `keyboard.mode` 和 `keyboard.type` 选择合适的生成器。

```kotlin
/**
 * 按键布局生成器接口。
 *
 * 根据 KeyTableContext 中的键盘类型、输入模式、键盘状态和相关数据
 * 生成按键布局矩阵 List<List<InputKey>>。
 *
 * 不同 KeyboardInputMode 和 KeyboardType 的组合可以注册不同的实现，
 * 由 KeyLayoutPanel 根据当前键盘状态选择合适的生成器。
 */
interface KeyTableGenerator {
    fun generate(context: KeyTableContext): List<List<InputKey>>
}

/**
 * 按键生成上下文。
 *
 * 包含按键布局生成所需的全部信息，
 * 生成器根据这些信息决定按键的数量、排列、标签和状态。
 */
data class KeyTableContext(
    /** 运行时配置 */
    val config: ImeConfig,
    /** 当前键盘实例（含 type、mode、state） */
    val keyboard: Keyboard,
    /** 当前输入列表 */
    val inputList: InputList,
    /** 当前候选列表 */
    val candidateList: CandidateList,
)
```

`KeyTableGenerator` 的 `generate()` 方法接收 `KeyTableContext`，返回 `List<List<InputKey>>` 二维矩阵。矩阵的外层 `List` 对应键盘的行，内层 `List` 对应每行中的按键。`InputKey` 是按键的语义标识，可以是 `InputKey.Char`（字符键）、`InputKey.Ctrl`（控制键）、`InputKey.Candidate`（候选键）、`InputKey.MathOp`（数学运算键）、`InputKey.Symbol`（符号键）或 `InputKey.Null`（空占位键）。`KeyTableContext` 提供了生成器所需的全部上下文：`config` 提供手模式、功能开关等配置信息；`keyboard` 提供当前键盘的类型、输入模式和状态机状态；`inputList` 提供当前输入内容，用于确定删除键、光标键等功能键的状态；`candidateList` 提供当前候选词列表，用于确定候选键的内容。

`HexGrid` 模式下，`KeyTableGenerator` 生成六边形排列的按键矩阵，相邻按键之间呈 60 度夹角，使得手指在六边形区域间滑行时路径更短。`RectGrid` 模式下，`KeyTableGenerator` 生成传统的矩形排列按键矩阵，每行按键等高对齐，适合 QWERTY 式布局。两种模式共享相同的 `KeyTableContext` 输入，但生成的按键矩阵在行列数和按键排列上有所不同，由 `KeyLayoutPanel` 负责将矩阵渲染为对应的视觉布局。
