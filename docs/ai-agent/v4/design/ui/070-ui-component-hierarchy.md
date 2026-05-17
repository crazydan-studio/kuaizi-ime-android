# UI 组件层次设计

本文档基于 v4 现有设计（三层面板分离、MVI 数据流、KeyboardViewModel 等），结合新的屏幕分区模型和交互/输入模式分离需求，重新整理和完善筷字输入法的 UI 组件层次。

---

## 1 核心概念定义表

| 概念 | 英文标识 | 定义 | 层级 | 与现有设计关系 |
|------|----------|------|------|---------------|
| **交互模式** | `InteractionMode` | 决定按键的空间布局方式和用户与按键的交互范式（点击、滑行、连续路径等），同一交互模式下不同输入模式的交互逻辑统一 | UI 层 | **新引入**。对 `KeyGridPanel` 的布局策略做抽象，将原有的"键盘变体"拆分为交互模式维度 |
| **输入模式** | `InputMode` | 决定输入内容类型（拼音、拉丁、符号、数字等），同一输入模式在不同交互模式下展示不同 | UI 层 | **新引入**。对应引擎层 `KeyboardType`，在 UI 层用独立枚举表达以明确交互/输入的二维正交关系 |
| **布局模式** | `LayoutMode` | 决定屏幕分区方式及组件在 A/B 区的分布策略（分离/堆叠） | UI 层 | **修改**。沿用 `LayoutMode` sealed class，但重命名为 `Separated` / `Stacked`，语义更精确 |
| **分离模式** | `LayoutMode.Separated` | 输入区域在 B 区、按键区域在 A 区，手指不遮挡按键视野 | UI 层 | **修改**。沿用原 `LayoutMode.FullScreen` 概念，但增加了 B 区三行布局结构 |
| **堆叠模式** | `LayoutMode.Stacked` | 所有组件均在 B 区堆叠，A 区不显示 | UI 层 | **修改**。沿用原 `LayoutMode.Overlay` 概念，但增加了 B 区三行布局结构 |
| **A 区** | `ZoneA` | 屏幕上部区域，分离模式下显示 KeyLayoutPanel + GestureFeedbackPanel | UI 层 | **新引入**。原 FullScreen 模式中"按键区域"概念的显式命名 |
| **B 区** | `ZoneB` | 屏幕下部区域，三行布局容纳所有输入交互面板 | UI 层 | **新引入**。原 Overlay/FullScreen 模式中"输入区域"概念的显式命名 |
| **按键布局面板** | `KeyLayoutPanel` | 负责按交互模式绘制不同布局的按键，按输入模式填充按键内容，单实例组件 | UI 层 | **修改**。由 `KeyGridPanel` 重命名并扩展职责，新增交互模式/输入模式的二维调度 |
| **手势反馈面板** | `GestureFeedbackPanel` | 透明绘制层，绘制手势轨迹、按键高亮等临时视觉反馈，双实例（A 区/B 区） | UI 层 | **沿用**。实例化策略由 `GestureFeedbackPanelSet` 配置调整 |
| **手势输入面板** | `GestureInputPanel` | 透明手势拦截层，仅在 B 区 KeyboardRow 中存在 | UI 层 | **沿用**。约束为仅存在于 B 区 |
| **输入候选列表面板** | `CandidateListPanel` | 显示可选的补全输入、可粘贴内容等 | UI 层 | **沿用**。从原 `CandidateListPanel` 直接沿用 |
| **信息提示面板** | `InfoTipPanel` | 短暂显示操作信息、已输入字符等，叠加在 CandidateListPanel 上层 | UI 层 | **新引入**。原 `CandidateListPanel` 中的部分提示功能独立为面板 |
| **常用工具面板** | `ToolsPanel` | 空闲时显示常用工具按钮，有输入时显示切换按钮 | UI 层 | **修改**。由原 `Toolbar` 重构，增加显隐切换逻辑 |
| **输入列表面板** | `InputListPanel` | 显示当前输入内容，支持选中编辑和拼音候选选择 | UI 层 | **沿用**。从原 `InputListPanel` 直接沿用 |
| **指示器遮罩** | `IndicatorOverlay` | 透明遮罩层，显示手指形状的指示器，内置到 B 区各行中，播放时开启 | UI 层 | **新引入**。原 `FingerOverlay` 的功能内嵌到各行组件中 |
| **输入法主组件** | `ImeInputPanel` | 完整输入交互 UI，支持动态切换布局模式，可直接集成使用 | UI 层 | **修改**。替代原 `KeyboardPanel` / `KeyboardScreen`，统一为单一入口组件 |
| **输入动作播放组件** | `InputActionPlayerPanel` | 对 ImeInputPanel 的上层封装，支持动画播放和直接输入两种模式 | UI 层 | **修改**。由原 `ExerciseScreen` 中的播放逻辑重构为独立组件 |

### 1.1 InteractionMode 与 InputMode 的关系

交互模式和输入模式是二维正交的概念：

```
                 InputMode
              Pinyin  Latin  Number  Symbol  Math
            ┌────────┬───────┬───────┬───────┬──────┐
RectGrid    │ 拼音   │ 拉丁  │ 数字  │ 符号  │ 算术 │  ← 矩形按键网格
            │ 矩形键 │ 矩形键│ 矩形键│ 矩形键│矩形键│     交互逻辑：点击/滑行
            ├────────┼───────┼───────┼───────┼──────┤
HexGrid     │ 拼音   │ 拉丁  │ —     │ —     │ —    │  ← 六边形按键网格
            │ 六边形 │ 六边形│       │       │      │     交互逻辑：连续路径滑行
            ├────────┼───────┼───────┼───────┼──────┤
XType       │ 拼音   │ 拉丁  │ —     │ —     │ —    │  ← X 型按键布局
            │ X 型键 │ X 型键│       │       │      │     交互逻辑：X 型分区选择
Inter-      ├────────┼───────┼───────┼───────┼──────┤
actionMode  MultiZone│ 拉丁  │ 数字  │ 符号  │ 算术 │  ← 多分区按键
            │ 多分区 │ 多分区│ 多分区│ 多分区│多分区│     交互逻辑：分区 + 子交互
            └────────┴───────┴───────┴───────┴──────┘
```

**核心规则**：
- 同一交互模式下，各输入模式的交互逻辑统一（如 RectGrid 下拼音和拉丁都是点击/滑行）
- 同一输入模式在不同交互模式下展示不同（如拼音在 RectGrid 下显示矩形按键，在 HexGrid 下显示六边形区域）
- 并非所有组合都有意义（如 Number 在 HexGrid 下无需展示，标记为"-"）

```kotlin
/** 交互模式：决定按键布局方式和交互范式 */
enum class InteractionMode {
    /** 多行多列矩形按键（标准键盘） */
    RectGrid,
    /** 多行多列正六边形按键（X-Pad 风格） */
    HexGrid,
    /** X 型按键布局 */
    XType,
    /** 按键多分区 */
    MultiZone,
}

/** 输入模式：决定输入内容类型 */
enum class InputMode {
    Pinyin, Latin, Number, Symbol, Emoji, Math, Editor,
}

/** 交互模式 × 输入模式的兼容性矩阵 */
val INTERACTION_INPUT_MATRIX: Map<InteractionMode, Set<InputMode>> = mapOf(
    InteractionMode.RectGrid to InputMode.entries.toSet(),
    InteractionMode.HexGrid to setOf(InputMode.Pinyin, InputMode.Latin),
    InteractionMode.XType to setOf(InputMode.Pinyin, InputMode.Latin),
    InteractionMode.MultiZone to InputMode.entries.toSet(),
)
```

> **与引擎层的映射**：`InputMode` 与引擎层 `KeyboardType` 一一对应。UI 层引入 `InputMode` 独立枚举而非直接复用 `KeyboardType`，是为了明确交互/输入的二维正交关系，避免 `KeyboardType` 承担过多的 UI 语义。`KeyboardViewModel` 负责二者的映射和转换。

---

## 2 屏幕布局模型

### 2.1 屏幕分区

```
┌─────────────────────────────────────────────┐
│                   A 区（ZoneA）                │
│         屏幕上部，分离模式下显示按键区域          │
│                                              │
│  ┌──────────────────────────────────────┐    │
│  │  KeyLayoutPanel (底层)                │    │
│  │  GestureFeedbackPanel_A (反馈层)       │    │
│  └──────────────────────────────────────┘    │
│                                              │
├─────────────────────────────────────────────┤
│                   B 区（ZoneB）                │
│         屏幕下部，三行布局                      │
│                                              │
│  ┌──────────────────────────────────────┐    │
│  │ Row 1: CandidateListPanel            │    │
│  │        InfoTipPanel (叠加在上层)       │    │
│  │        IndicatorOverlay (最上层)       │    │
│  ├──────────────────────────────────────┤    │
│  │ Row 2: ToolsPanel / InputListPanel   │    │
│  │        IndicatorOverlay (最上层)       │    │
│  ├──────────────────────────────────────┤    │
│  │ Row 3: GestureInputPanel (最上层)     │    │
│  │        GestureFeedbackPanel_B (反馈层) │    │
│  │        KeyLayoutPanel (底层, 仅堆叠)   │    │
│  │        IndicatorOverlay (反馈层之上)   │    │
│  └──────────────────────────────────────┘    │
└─────────────────────────────────────────────┘
```

### 2.2 两种布局模式下的组件分布表

| 组件 | 分离模式 · A 区 | 分离模式 · B 区 | 堆叠模式 · A 区 | 堆叠模式 · B 区 |
|------|:---:|:---:|:---:|:---:|
| **KeyLayoutPanel** | Row 内（底层） | — | — | Row3（底层） |
| **GestureFeedbackPanel_A** | Row 内（反馈层） | — | — | — |
| **GestureFeedbackPanel_B** | — | Row3（反馈层） | — | Row3（反馈层） |
| **GestureInputPanel** | — | Row3（最上层） | — | Row3（最上层） |
| **CandidateListPanel** | — | Row1（底层） | — | Row1（底层） |
| **InfoTipPanel** | — | Row1（叠加层） | — | Row1（叠加层） |
| **ToolsPanel** | — | Row2（空闲时） | — | Row2（空闲时） |
| **InputListPanel** | — | Row2（有输入时） | — | Row2（有输入时） |
| **IndicatorOverlay** | — | Row1/2/3 各一 | — | Row1/2/3 各一 |

> **KeyLayoutPanel 迁移规则**：单实例组件，在分离模式下渲染于 A 区，在堆叠模式下渲染于 B 区 Row3。布局模式切换时，KeyLayoutPanel 从一个区域迁移到另一个区域（Compose 重组），而非创建新实例。

> **GestureFeedbackPanel 双实例规则**：
> - A 区实例（`GestureFeedbackPanel_A`）：仅在分离模式下存在，绘制按键高亮、按键路径、X-Pad 路径高亮
> - B 区实例（`GestureFeedbackPanel_B`）：始终存在，绘制触摸轨迹；分离模式下额外绘制手指指示器
> - 堆叠模式下仅 B 区实例，绘制全部反馈元素（同原 `OverlaySet` 配置）

---

## 3 完整 UI 组件层次树

```
ImeInputPanel                              ← 输入法主组件（布局模式容器）
├── KeyboardViewModel                      ← UI 协调中心（持有 ImeEngine）
├── GestureFeedbackState                   ← 手势反馈状态（独立于 ImeState）
│
├── [SeparatedMode] 分离模式布局
│   ├── ZoneAPanel                         ← A 区容器
│   │   ├── KeyLayoutPanel                 ← 按键布局面板（底层）
│   │   └── GestureFeedbackPanel           ← A 区反馈面板（反馈层）
│   │
│   └── ZoneBPanel                         ← B 区容器（三行）
│       ├── CandidateRow                   ← 第 1 行：候选行
│       │   ├── CandidateListPanel         ← 输入候选列表面板
│       │   ├── InfoTipPanel               ← 信息提示面板（叠加）
│       │   └── IndicatorOverlay           ← 指示器遮罩（叠加）
│       │
│       ├── InputRow                       ← 第 2 行：输入行
│       │   ├── ToolsPanel                 ← 常用工具面板（空闲时显示）
│       │   │   └── ToolsPanelItem[]       ← 工具按钮项
│       │   ├── InputListPanel             ← 输入列表面板（有输入时显示）
│       │   │   ├── CharInputItem[]        ← 字符输入项
│       │   │   └── GapInputItem[]         ← 间隙/游标项
│       │   └── IndicatorOverlay           ← 指示器遮罩（叠加）
│       │
│       └── KeyboardRow                    ← 第 3 行：键盘行
│           ├── GestureInputPanel          ← 手势输入面板（最上层）
│           ├── GestureFeedbackPanel       ← B 区反馈面板（反馈层）
│           └── IndicatorOverlay           ← 指示器遮罩（反馈层之上）
│
├── [StackedMode] 堆叠模式布局
│   └── ZoneBPanel                         ← B 区容器（三行，同上）
│       ├── CandidateRow                   ← （同分离模式）
│       ├── InputRow                       ← （同分离模式）
│       └── KeyboardRow                    ← 第 3 行：键盘行
│           ├── GestureInputPanel          ← 手势输入面板（最上层）
│           ├── GestureFeedbackPanel       ← B 区反馈面板（反馈层）
│           ├── KeyLayoutPanel             ← 按键布局面板（底层）
│           └── IndicatorOverlay           ← 指示器遮罩（反馈层之上）
│
└── InputActionPlayerPanel                 ← 输入动作播放组件（ImeInputPanel 上层封装）
    ├── ImeInputPanel                      ← 内嵌输入法主组件
    ├── InputActionPlayer                  ← 播放引擎
    │   ├── ActionScript                   ← 动作脚本
    │   └── KeyPositionResolver            ← 坐标解析器
    ├── ActionPlayerControlBar             ← 播放控制栏
    └── TargetTextDisplay                  ← 目标文本展示
```

### 3.1 层次关系说明

```
集成组件层
  ImeInputPanel / InputActionPlayerPanel
       │
面板组件层
  CandidateListPanel / InputListPanel / InfoTipPanel / ToolsPanel
  KeyLayoutPanel / GestureInputPanel / GestureFeedbackPanel
  IndicatorOverlay
       │
原子组件层
  KeyView / CandidateItem / CharInputItem / GapInputItem / ToolsPanelItem
       │
ViewModel 层
  KeyboardViewModel / GestureFeedbackState
       │
引擎层 (:ime-engine)
  ImeEngine / ImeState / ImeIntent / InputAction / ActionScript
```

---

## 4 组件详细规格

### 4.1 ImeInputPanel（输入法主组件）

| 属性 | 说明 |
|------|------|
| **角色** | 输入法 UI 的顶层入口组件，提供完整输入交互 UI |
| **职责** | 根据布局模式切换内部分离/堆叠布局；组合 ZoneA/ZonzB 容器；接入 KeyboardViewModel 驱动所有子面板 |
| **关键属性** | `viewModel: KeyboardViewModel`，`layoutMode: LayoutMode`（动态可切换） |
| **约束** | 必须在 `KeyboardTheme` 内使用；KeyLayoutPanel 为单实例，随布局模式在 A/B 区间迁移 |
| **组合结构** | 分离模式：`ZoneAPanel` + `ZoneBPanel`；堆叠模式：仅 `ZoneBPanel`（含 KeyLayoutPanel） |

```kotlin
@Composable
fun ImeInputPanel(
    viewModel: KeyboardViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val feedbackState = viewModel.feedbackState
    val layoutMode = state.layoutMode // 从 ImeState 读取当前布局模式

    KeyboardTheme(themeType = state.config.ui.themeType) {
        when (layoutMode) {
            is LayoutMode.Separated -> SeparatedModeLayout(viewModel, feedbackState, state)
            is LayoutMode.Stacked -> StackedModeLayout(viewModel, feedbackState, state)
        }
    }
}
```

### 4.2 LayoutMode（布局模式）

| 属性 | 说明 |
|------|------|
| **角色** | 定义屏幕分区方式和组件分布策略 |
| **修改说明** | 原有 `LayoutMode.Overlay` → `LayoutMode.Stacked`，`LayoutMode.FullScreen` → `LayoutMode.Separated`，语义更精确 |

```kotlin
sealed class LayoutMode {
    /**
     * 堆叠模式：所有组件均在 B 区。
     * KeyLayoutPanel 在 B 区 Row3 底层，GestureFeedbackPanel 仅 B 区单实例。
     */
    data object Stacked : LayoutMode()

    /**
     * 分离模式：输入区域在 B 区，按键区域在 A 区。
     * KeyLayoutPanel 在 A 区，GestureFeedbackPanel 双实例（A 区 + B 区）。
     *
     * @param zoneARatio A 区占屏幕高度比例
     */
    data class Separated(
        val zoneARatio: Float = 0.5f,
    ) : LayoutMode()
}
```

### 4.3 ZoneAPanel（A 区容器）

| 属性 | 说明 |
|------|------|
| **角色** | 分离模式下的 A 区容器，承载 KeyLayoutPanel 和 GestureFeedbackPanel_A |
| **职责** | 堆叠 KeyLayoutPanel（底层）与 GestureFeedbackPanel_A（反馈层）；提供 KeyLayoutPanel 的布局信息供反馈面板和 B 区 GestureInputPanel 查询 |
| **关键属性** | `keyLayoutPanelLayoutInfo: KeyLayoutPanelLayoutInfo` |
| **约束** | 仅在分离模式下渲染；KeyLayoutPanel 在此区域渲染 |

```kotlin
@Composable
fun ZoneAPanel(
    viewModel: KeyboardViewModel,
    feedbackState: GestureFeedbackState,
    state: ImeState,
    keyLayoutPanelLayoutInfo: KeyLayoutPanelLayoutInfo,
    onLayoutInfoChanged: (KeyLayoutPanelLayoutInfo) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        // 底层：按键布局面板（单实例，分离模式下在 A 区）
        KeyLayoutPanel(
            interactionMode = state.interactionMode,
            inputMode = state.inputMode,
            keyGrid = state.keyGrid,
            keyboardState = state.keyboardState,
            onLayoutInfoChanged = onLayoutInfoChanged,
        )
        // 反馈层：A 区手势反馈面板
        GestureFeedbackPanel(
            elements = GestureFeedbackPanelSet.SeparatedSet.zoneAElements,
            feedbackState = feedbackState,
            keyLayoutPanelLayout = keyLayoutPanelLayoutInfo,
        )
    }
}
```

### 4.4 ZoneBPanel（B 区容器）

| 属性 | 说明 |
|------|------|
| **角色** | B 区容器，三行布局，两种布局模式共用 |
| **职责** | 按 Row1/Row2/Row3 垂直排列三行；在堆叠模式下，Row3 底层包含 KeyLayoutPanel |
| **关键属性** | `layoutMode: LayoutMode`（决定 Row3 是否包含 KeyLayoutPanel） |
| **约束** | 两种布局模式共用同一 B 区结构，仅 Row3 的组件构成不同 |

```kotlin
@Composable
fun ZoneBPanel(
    viewModel: KeyboardViewModel,
    feedbackState: GestureFeedbackState,
    state: ImeState,
    layoutMode: LayoutMode,
    modifier: Modifier = Modifier,
) {
    val indicatorOverlayVisible = state.indicatorOverlayEnabled

    Column(modifier = modifier) {
        // Row 1：候选行
        CandidateRow(
            candidateList = state.candidateList,
            infoTipState = state.infoTipState,
            onCandidateSelected = { viewModel.handleIntent(ImeIntent.SelectCandidate(it)) },
            indicatorOverlayVisible = indicatorOverlayVisible,
            modifier = Modifier.weight(1f),
        )

        // Row 2：输入行
        InputRow(
            inputList = state.inputList,
            keyboardState = state.keyboardState,
            toolsPanelConfig = state.toolsPanelConfig,
            onGapTapped = { viewModel.handleIntent(ImeIntent.MoveCursorTo(it)) },
            onToolClicked = { viewModel.handleIntent(it) },
            indicatorOverlayVisible = indicatorOverlayVisible,
            modifier = Modifier.weight(1f),
        )

        // Row 3：键盘行
        KeyboardRow(
            viewModel = viewModel,
            feedbackState = feedbackState,
            state = state,
            layoutMode = layoutMode,
            indicatorOverlayVisible = indicatorOverlayVisible,
            modifier = Modifier.weight(/* 弹性权重，占主要空间 */),
        )
    }
}
```

### 4.5 CandidateRow（候选行 · Row1）

| 属性 | 说明 |
|------|------|
| **角色** | B 区第 1 行容器 |
| **职责** | 堆叠 CandidateListPanel（底层）+ InfoTipPanel（叠加层）+ IndicatorOverlay（最上层） |
| **共存规则** | InfoTipPanel 叠加在 CandidateListPanel 上层，短暂显示后自动消失；IndicatorOverlay 默认隐藏 |

```kotlin
@Composable
fun CandidateRow(
    candidateList: CandidateList,
    infoTipState: InfoTipState?,
    onCandidateSelected: (InputWord) -> Unit,
    indicatorOverlayVisible: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        CandidateListPanel(
            state = candidateList,
            onCandidateSelected = onCandidateSelected,
        )
        InfoTipPanel(
            state = infoTipState,
        )
        IndicatorOverlay(
            visible = indicatorOverlayVisible,
            targetResolver = { /* CandidateListPanel 的布局解析 */ },
        )
    }
}
```

### 4.6 InputRow（输入行 · Row2）

| 属性 | 说明 |
|------|------|
| **角色** | B 区第 2 行容器 |
| **职责** | 根据输入状态切换显示 ToolsPanel 或 InputListPanel；叠加 IndicatorOverlay |
| **共存规则** | ToolsPanel 与 InputListPanel 互斥共用同一空间；空闲时显示 ToolsPanel，有输入时显示 InputListPanel 并在 ToolsPanel 原位置显示切换按钮 |

```kotlin
@Composable
fun InputRow(
    inputList: InputList,
    keyboardState: KeyboardState,
    toolsPanelConfig: ToolsPanelConfig,
    onGapTapped: (Int) -> Unit,
    onToolClicked: (ImeIntent) -> Unit,
    indicatorOverlayVisible: Boolean,
    modifier: Modifier = Modifier,
) {
    val hasInput = inputList.inputs.any { it is InputItem.Char }

    Box(modifier = modifier) {
        if (hasInput) {
            // 有输入：显示 InputListPanel + 工具切换按钮
            InputListPanel(
                state = inputList,
                onGapTapped = onGapTapped,
            )
            // 工具切换按钮（悬浮或嵌入 InputListPanel）
            ToolsPanelToggleButton(
                onClick = { /* 切换到工具面板 */ },
            )
        } else {
            // 空闲：显示 ToolsPanel
            ToolsPanel(
                config = toolsPanelConfig,
                onToolClicked = onToolClicked,
            )
        }
        IndicatorOverlay(
            visible = indicatorOverlayVisible,
            targetResolver = { /* 当前活动面板的布局解析 */ },
        )
    }
}
```

### 4.7 KeyboardRow（键盘行 · Row3）

| 属性 | 说明 |
|------|------|
| **角色** | B 区第 3 行容器 |
| **职责** | 堆叠键盘相关面板；在堆叠模式下包含 KeyLayoutPanel（底层），分离模式下不含 KeyLayoutPanel |
| **关键属性** | `layoutMode: LayoutMode`（决定是否包含 KeyLayoutPanel） |
| **叠加顺序** | 堆叠模式：底层 KeyLayoutPanel → 反馈层 GestureFeedbackPanel_B → IndicatorOverlay → 最上层 GestureInputPanel |

```kotlin
@Composable
fun KeyboardRow(
    viewModel: KeyboardViewModel,
    feedbackState: GestureFeedbackState,
    state: ImeState,
    layoutMode: LayoutMode,
    indicatorOverlayVisible: Boolean,
    modifier: Modifier = Modifier,
) {
    var keyLayoutPanelLayoutInfo by remember { mutableStateOf(KeyLayoutPanelLayoutInfo()) }

    Box(modifier = modifier) {
        // 底层：按键布局面板（仅堆叠模式在 B 区）
        if (layoutMode is LayoutMode.Stacked) {
            KeyLayoutPanel(
                interactionMode = state.interactionMode,
                inputMode = state.inputMode,
                keyGrid = state.keyGrid,
                keyboardState = state.keyboardState,
                onLayoutInfoChanged = { keyLayoutPanelLayoutInfo = it },
            )
        }

        // 反馈层：B 区手势反馈面板
        val feedbackElements = when (layoutMode) {
            is LayoutMode.Stacked -> GestureFeedbackPanelSet.StackedSet.allElements
            is LayoutMode.Separated -> GestureFeedbackPanelSet.SeparatedSet.zoneBElements
        }
        GestureFeedbackPanel(
            elements = feedbackElements,
            feedbackState = feedbackState,
            keyLayoutPanelLayout = keyLayoutPanelLayoutInfo,
        )

        // 指示器遮罩
        IndicatorOverlay(
            visible = indicatorOverlayVisible,
            targetResolver = { keyLayoutPanelLayoutInfo },
        )

        // 最上层：手势输入面板
        GestureInputPanel(
            keyPanelLayout = keyLayoutPanelLayoutInfo,
            interactionMode = state.interactionMode,
            feedbackState = feedbackState,
            onGesture = { viewModel.handleGesture(it) },
        )
    }
}
```

### 4.8 KeyLayoutPanel（按键布局面板）

| 属性 | 说明 |
|------|------|
| **角色** | 负责按交互模式绘制不同布局的按键，按输入模式填充按键内容 |
| **职责** | ① 根据 InteractionMode 选择按键布局策略（矩形网格/六边形/X型/多分区）② 根据 InputMode 填充按键内容 ③ 渲染按键持续性状态 ④ 提供布局信息供 GestureInputPanel 和 GestureFeedbackPanel 查询 ⑤ 支持动态切换交互模式和输入模式 |
| **关键属性** | `interactionMode: InteractionMode`，`inputMode: InputMode`，`keyGrid: List<List<InputKey>>`，`keyboardState: KeyboardState` |
| **约束** | **单实例**：分离模式在 A 区渲染，堆叠模式在 B 区 Row3 渲染，不创建第二个实例；纯展示层，不处理触摸事件；不绘制手势反馈 |
| **实例策略** | 单实例迁移：布局模式切换时，KeyLayoutPanel 从 A 区/ B 区的 Compose 树中移除并在另一区域重新插入，Compose 重组时保留 remember 状态（通过 key 参数控制） |

```kotlin
@Composable
fun KeyLayoutPanel(
    interactionMode: InteractionMode,
    inputMode: InputMode,
    keyGrid: List<List<InputKey>>,
    keyboardState: KeyboardState,
    onLayoutInfoChanged: (KeyLayoutPanelLayoutInfo) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (interactionMode) {
        InteractionMode.RectGrid -> RectGridKeyLayoutPanel(
            inputMode = inputMode,
            keyGrid = keyGrid,
            keyboardState = keyboardState,
            onLayoutInfoChanged = onLayoutInfoChanged,
            modifier = modifier,
        )
        InteractionMode.HexGrid -> HexGridKeyLayoutPanel(
            inputMode = inputMode,
            keyGrid = keyGrid,
            keyboardState = keyboardState,
            onLayoutInfoChanged = onLayoutInfoChanged,
            modifier = modifier,
        )
        InteractionMode.XType -> XTypeKeyLayoutPanel(
            inputMode = inputMode,
            keyGrid = keyGrid,
            keyboardState = keyboardState,
            onLayoutInfoChanged = onLayoutInfoChanged,
            modifier = modifier,
        )
        InteractionMode.MultiZone -> MultiZoneKeyLayoutPanel(
            inputMode = inputMode,
            keyGrid = keyGrid,
            keyboardState = keyboardState,
            onLayoutInfoChanged = onLayoutInfoChanged,
            modifier = modifier,
        )
    }
}
```

#### KeyLayoutPanel 子类规格

| 子类 | 布局方式 | 交互逻辑 | 支持的 InputMode |
|------|----------|----------|------------------|
| `RectGridKeyLayoutPanel` | 多行多列矩形按键网格 | 点击/长按/滑行/翻转 | 全部 |
| `HexGridKeyLayoutPanel` | 多行多列正六边形按键网格 | 连续路径滑行 | Pinyin, Latin |
| `XTypeKeyLayoutPanel` | X 型按键分区布局 | 分区选择 + 子交互 | Pinyin, Latin |
| `MultiZoneKeyLayoutPanel` | 按键多分区布局 | 分区内点击/滑行 | 全部 |

> **与原 KeyGridPanel 子类的关系**：原 `StandardKeyGridPanel` → `RectGridKeyLayoutPanel`；原 `EmojiKeyGridPanel` 在 `RectGridKeyLayoutPanel` 的 `InputMode.Emoji` 分支中处理；原 `CandidateKeyGridPanel` / `CommitOptionKeyGridPanel` 为候选/提交专用键盘，在 `RectGridKeyLayoutPanel` 中作为特殊 InputMode 处理。

### 4.9 GestureFeedbackPanel（手势反馈面板）

| 属性 | 说明 |
|------|------|
| **角色** | 透明绘制层，渲染手势相关的临时视觉反馈 |
| **职责** | 沿用原三层面板分离设计中的反馈面板职责：绘制滑行轨迹、按键高亮、X-Pad 路径、手指指示器 |
| **实例策略** | **双实例**：A 区实例绘制按键高亮/按键路径/X-Pad 路径；B 区实例绘制触摸轨迹，堆叠模式下绘制全部 |
| **约束** | 不处理触摸事件；不依赖任何面板的 Canvas；坐标转换需根据 GestureInputPanel 和 KeyLayoutPanel 的实际尺寸进行归一化映射 |

#### 双实例配置集

```kotlin
sealed class GestureFeedbackPanelSet {
    /** 堆叠模式：单实例（B 区），绘制全部反馈 */
    data object StackedSet : GestureFeedbackPanelSet() {
        val allElements: Set<FeedbackElementType> = setOf(
            FeedbackElementType.TouchTrail,
            FeedbackElementType.KeyHighlight,
            FeedbackElementType.KeyPath,
            FeedbackElementType.XPadPathHighlight,
            FeedbackElementType.FingerIndicator,
        )
    }

    /** 分离模式：双实例 */
    data class SeparatedSet(
        /** A 区实例：按键高亮、按键路径、X-Pad 路径 */
        val zoneAElements: Set<FeedbackElementType> = setOf(
            FeedbackElementType.KeyHighlight,
            FeedbackElementType.KeyPath,
            FeedbackElementType.XPadPathHighlight,
        ),
        /** B 区实例：触摸轨迹、手指指示器 */
        val zoneBElements: Set<FeedbackElementType> = setOf(
            FeedbackElementType.TouchTrail,
            FeedbackElementType.FingerIndicator,
        ),
    ) : GestureFeedbackPanelSet()
}
```

#### 坐标归一化与转换

手势反馈面板上的输入轨迹为针对手势输入面板（GestureInputPanel）归一化后的坐标点。在不同区域的面板上回放时，需根据面板实际尺寸转换坐标：

```kotlin
/**
 * 坐标归一化工具。
 *
 * GestureInputPanel 采集的触摸点为面板坐标系下的绝对坐标，
 * 归一化后存储到 GestureFeedbackState。
 * GestureFeedbackPanel 渲染时根据自身尺寸反归一化。
 */
object CoordinateNormalizer {
    /** 将绝对坐标归一化到 [0,1] 范围 */
    fun normalize(point: Offset, sourceSize: Size): NormalizedPoint {
        return NormalizedPoint(
            x = if (sourceSize.width > 0) point.x / sourceSize.width else 0f,
            y = if (sourceSize.height > 0) point.y / sourceSize.height else 0f,
        )
    }

    /** 将归一化坐标转换为面板绝对坐标 */
    fun denormalize(point: NormalizedPoint, targetSize: Size): Offset {
        return Offset(
            x = point.x * targetSize.width,
            y = point.y * targetSize.height,
        )
    }
}

data class NormalizedPoint(val x: Float, val y: Float)
```

### 4.10 GestureInputPanel（手势输入面板）

| 属性 | 说明 |
|------|------|
| **角色** | 透明手势拦截层，接收用户手势并识别为 InputGesture |
| **职责** | 沿用原三层面板分离设计中的输入面板职责 |
| **实例策略** | **单实例**：仅在 B 区 KeyboardRow 中存在，始终在最上层 |
| **约束** | 完全透明不绘制任何内容；必须在最上层确保触摸事件优先到达；触摸坐标需根据 InteractionMode 进行不同的按键定位逻辑 |

> **分离模式下的坐标映射**：在分离模式下，GestureInputPanel 在 B 区接收触摸，但按键布局在 A 区的 KeyLayoutPanel 中。GestureInputPanel 通过 `keyLayoutPanelLayoutInfo` 查询按键布局，将 B 区触摸坐标映射到 A 区按键位置进行定位。映射关系由 `KeyLayoutPanelLayoutInfo` 中的按键 Rect 坐标提供——这些坐标已是屏幕全局坐标（通过 `onGloballyPositioned` 的 `boundsInRoot()` 获取），因此 B 区触摸事件的屏幕坐标可直接与 A 区按键的屏幕坐标匹配。

### 4.11 InfoTipPanel（信息提示面板）

| 属性 | 说明 |
|------|------|
| **角色** | 短暂显示操作信息、已输入字符等 |
| **职责** | 在 CandidateListPanel 上层短暂叠加显示信息（如"已切换到拉丁键盘"、"已粘贴"等），自动消失 |
| **关键属性** | `state: InfoTipState?`（null 时不显示） |
| **约束** | 仅在 Row1 中叠加存在；显示时长短暂（1-2 秒）；不处理触摸事件（触摸穿透到下层 CandidateListPanel） |
| **共存规则** | 与 CandidateListPanel 堆叠占用同一空间，InfoTipPanel 在上层 |

```kotlin
data class InfoTipState(
    val message: String,
    val timestamp: Long,
    val durationMs: Long = 1500L,
)

@Composable
fun InfoTipPanel(
    state: InfoTipState?,
    modifier: Modifier = Modifier,
) {
    if (state == null) return

    var visible by remember { mutableStateOf(true) }
    LaunchedEffect(state.timestamp) {
        visible = true
        delay(state.durationMs)
        visible = false
    }

    AnimatedVisibility(visible = visible, exit = fadeOut()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = state.message, color = Color.White)
        }
    }
}
```

### 4.12 ToolsPanel（常用工具面板）

| 属性 | 说明 |
|------|------|
| **角色** | 空闲时显示常用工具按钮，有输入时显示切换按钮 |
| **职责** | ① 输入空闲时：显示剪贴板、收藏、设置、键盘切换等常用工具按钮 ② 有输入时：收缩为切换按钮，点击可展开工具面板或切换回工具视图 |
| **关键属性** | `config: ToolsPanelConfig`，`onToolClicked: (ImeIntent) -> Unit` |
| **约束** | 与 InputListPanel 互斥共用 Row2 空间 |
| **与原设计关系** | 由原 `Toolbar` 重构。原 Toolbar 始终显示，新 ToolsPanel 增加了与 InputListPanel 的互斥切换逻辑 |

```kotlin
data class ToolsPanelConfig(
    val tools: List<ToolItem>,
    val isExpanded: Boolean = true,
)

data class ToolItem(
    val id: String,
    val icon: ImageVector,
    val label: String,
    val intent: ImeIntent,
)
```

### 4.13 IndicatorOverlay（指示器遮罩）

| 属性 | 说明 |
|------|------|
| **角色** | 透明遮罩层，播放输入动画时显示手指形状的指示器 |
| **职责** | 在各行上层覆盖透明遮罩，根据 InputActionPlayer 提供的归一化坐标绘制手指指示器动画 |
| **关键属性** | `visible: Boolean`（由 InputActionPlayer 控制开关），`targetResolver: () -> LayoutInfo?`（提供当前行的布局信息用于坐标转换） |
| **约束** | 内置于 B 区各行中，默认隐藏；仅在 InputActionPlayer 播放时显示；不拦截触摸事件 |
| **坐标转换** | InputActionPlayer 产生的指示器坐标为针对 KeyLayoutPanel 归一化的坐标点，IndicatorOverlay 需根据所在行的实际面板尺寸和位置进行坐标转换 |

```kotlin
@Composable
fun IndicatorOverlay(
    visible: Boolean,
    targetResolver: () -> LayoutInfo?,
    modifier: Modifier = Modifier,
) {
    if (!visible) return

    val fingerIndicator by LocalFingerIndicatorState.current.collectAsState()

    // 透明遮罩，不拦截触摸
    Canvas(
        modifier = modifier
            .fillMaxSize()
            .semantics { invisibleToUser() }
            .pointerInput(Unit) { /* 不消费触摸事件，全部穿透 */ },
    ) {
        fingerIndicator?.let { indicator ->
            if (!indicator.visible) return@let
            val layoutInfo = targetResolver() ?: return@let

            // 将归一化坐标转换为本面板的绝对坐标
            val position = CoordinateNormalizer.denormalize(
                indicator.normalizedPosition,
                size,
            )
            // 绘制手指指示器
            drawFingerIndicator(position, indicator.pressed)
        }
    }
}
```

> **设计决策**：IndicatorOverlay 内置于各行而非作为独立的全局遮罩，理由是：① 各行面板的坐标空间不同（CandidateListPanel、InputListPanel、KeyLayoutPanel 的归一化坐标系各自独立），分行遮罩可以精确控制坐标转换；② 行级遮罩不影响其他行的触摸交互；③ 开关控制简单，只需在播放时设 `visible = true`。

---

## 5 面板共存规则汇总表

| 行 | 面板组合 | 共存规则 | 叠加顺序（底→顶） |
|----|----------|----------|-------------------|
| **Row1** | CandidateListPanel + InfoTipPanel + IndicatorOverlay | InfoTipPanel 叠加在 CandidateListPanel 上层，短暂显示；IndicatorOverlay 默认隐藏 | CandidateListPanel → InfoTipPanel → IndicatorOverlay |
| **Row2** | ToolsPanel / InputListPanel + IndicatorOverlay | ToolsPanel 与 InputListPanel 互斥共用空间；IndicatorOverlay 默认隐藏 | (ToolsPanel \| InputListPanel) → IndicatorOverlay |
| **Row3** | KeyLayoutPanel（仅堆叠）+ GestureFeedbackPanel_B + GestureInputPanel + IndicatorOverlay | 三层核心面板始终共存堆叠；KeyLayoutPanel 仅堆叠模式存在；IndicatorOverlay 默认隐藏 | KeyLayoutPanel → GestureFeedbackPanel_B → IndicatorOverlay → GestureInputPanel |
| **A 区** | KeyLayoutPanel + GestureFeedbackPanel_A | 仅分离模式存在；KeyLayoutPanel 在底层 | KeyLayoutPanel → GestureFeedbackPanel_A |

### 5.1 面板实例化策略表

| 面板 | 实例数 | 实例位置 | 说明 |
|------|--------|----------|------|
| KeyLayoutPanel | 1 | 分离模式→A 区；堆叠模式→B 区 Row3 | 单实例迁移，不在两区同时存在 |
| GestureFeedbackPanel | 2（分离模式）或 1（堆叠模式） | A 区 + B 区 Row3 | 分离模式双实例，堆叠模式仅 B 区实例 |
| GestureInputPanel | 1 | B 区 Row3 | 始终在 B 区 |
| CandidateListPanel | 1 | B 区 Row1 | — |
| InfoTipPanel | 1 | B 区 Row1 | 叠加在 CandidateListPanel 上层 |
| ToolsPanel | 1 | B 区 Row2 | 与 InputListPanel 互斥 |
| InputListPanel | 1 | B 区 Row2 | 与 ToolsPanel 互斥 |
| IndicatorOverlay | 3 | B 区 Row1/Row2/Row3 各一 | 内置于各行 |

---

## 6 逻辑/状态层

### 6.1 状态模型

#### ImeState 扩展

在原有 `ImeState` 基础上新增以下字段以支持新的 UI 组件层次：

```kotlin
data class ImeState(
    // ─── 原有字段 ───
    val config: ImeConfig,
    val inputList: InputList,
    val candidateList: CandidateList,
    val keyboardState: KeyboardState,
    val keyGrid: List<List<InputKey>>,

    // ─── 新增字段 ───
    /** 当前交互模式 */
    val interactionMode: InteractionMode = InteractionMode.RectGrid,
    /** 当前输入模式 */
    val inputMode: InputMode = InputMode.Pinyin,
    /** 当前布局模式 */
    val layoutMode: LayoutMode = LayoutMode.Stacked,
    /** 信息提示状态 */
    val infoTipState: InfoTipState? = null,
    /** 常用工具面板配置 */
    val toolsPanelConfig: ToolsPanelConfig = ToolsPanelConfig(),
    /** 指示器遮罩是否可见（由 InputActionPlayer 控制） */
    val indicatorOverlayEnabled: Boolean = false,
)
```

#### GestureFeedbackState 扩展

原有 `GestureFeedbackState` 中的 `touchTrailPoints` 改为归一化坐标存储，以支持多区域面板的坐标转换：

```kotlin
class GestureFeedbackState {
    // ─── 修改：归一化触摸轨迹点 ───
    private val _normalizedTouchTrailPoints = MutableStateFlow<List<NormalizedPoint>>(emptyList())
    val normalizedTouchTrailPoints: StateFlow<List<NormalizedPoint>> =
        _normalizedTouchTrailPoints.asStateFlow()

    // ─── 沿用 ───
    private val _pressedKeys = MutableStateFlow<Set<InputKey>>(emptySet())
    val pressedKeys: StateFlow<Set<InputKey>> = _pressedKeys.asStateFlow()

    private val _keyPath = MutableStateFlow<List<InputKey>>(emptyList())
    val keyPath: StateFlow<List<InputKey>> = _keyPath.asStateFlow()

    private val _xPadPath = MutableStateFlow<List<XPadZone>>(emptyList())
    val xPadPath: StateFlow<List<XPadZone>> = _xPadPath.asStateFlow()

    // ─── 修改：手指指示器使用归一化坐标 ───
    private val _fingerIndicator = MutableStateFlow<FingerIndicatorState?>(null)
    val fingerIndicator: StateFlow<FingerIndicatorState?> = _fingerIndicator.asStateFlow()

    // ... 更新方法同步修改为归一化存储 ...
}

data class FingerIndicatorState(
    /** 归一化坐标（相对于目标面板） */
    val normalizedPosition: NormalizedPoint,
    val pressed: Boolean,
    val visible: Boolean = true,
    /** 指示器目标行（Row1/Row2/Row3），用于定位到正确的 IndicatorOverlay */
    val targetRow: IndicatorTargetRow,
)

enum class IndicatorTargetRow { Row1, Row2, Row3 }
```

### 6.2 KeyboardViewModel 扩展

```kotlin
class KeyboardViewModel(
    private val engine: ImeEngine,
) : ViewModel() {

    val state: StateFlow<ImeState> = engine.state
    val feedbackState = GestureFeedbackState()

    // ─── 新增：交互模式切换 ───
    fun switchInteractionMode(mode: InteractionMode) {
        // 验证交互模式与当前输入模式的兼容性
        val compatible = INTERACTION_INPUT_MATRIX[mode]?.contains(state.value.inputMode) ?: false
        if (!compatible) return
        engine.updateConfig { config ->
            config.copy(ui = config.ui.copy(interactionMode = mode))
        }
    }

    // ─── 新增：输入模式切换 ───
    fun switchInputMode(mode: InputMode) {
        // 验证输入模式与当前交互模式的兼容性
        val compatible = INTERACTION_INPUT_MATRIX[state.value.interactionMode]?.contains(mode) ?: false
        if (!compatible) return
        // 映射到引擎的 KeyboardType
        val keyboardType = mode.toKeyboardType()
        engine.handleIntent(ImeIntent.SwitchKeyboard(keyboardType))
    }

    // ─── 新增：布局模式切换 ───
    fun switchLayoutMode(mode: LayoutMode) {
        engine.updateConfig { config ->
            config.copy(ui = config.ui.copy(layoutMode = mode))
        }
    }

    // ─── 沿用 ───
    fun handleGesture(gesture: InputGesture) { /* ... */ }
    fun handleIntent(intent: ImeIntent) { engine.handleIntent(intent) }
    fun updateConfig(transform: (ImeConfig) -> ImeConfig) { engine.updateConfig(transform) }
}
```

### 6.3 数据流图

```
用户触摸 GestureInputPanel
       │
       ▼
 InputGesture（坐标无关）
       │
       ├──────────────────────┐
       ▼                      ▼
 KeyboardViewModel        GestureFeedbackState
 gestureToIntent()        归一化轨迹/按键高亮
       │                      │
       ▼                      ▼
 ImeIntent ──────→ ImeEngine.handleIntent()
                       │
                       ▼
                  ImeState（StateFlow）
                       │
       ┌───────────────┼───────────────┐
       ▼               ▼               ▼
 CandidateListPanel  InputListPanel  KeyLayoutPanel
 (Row1)             (Row2)          (A区/Row3)
                       │
                       ▼
              GestureFeedbackPanel_A/B
              （从 GestureFeedbackState 读取）

═════════════════════════════════════════════════

InputActionPlayer 播放路径：
 ActionScript → InputAction →
    ├─ KeyDown/SwipeTo/KeyUp → viewModel.handleIntent(ImeIntent)
    │                          + feedbackState.setFingerIndicator(归一化坐标)
    ├─ SelectCandidate → viewModel.handleIntent(ImeIntent.SelectCandidate)
    │                    + feedbackState.setFingerIndicator(归一化坐标, targetRow=Row1)
    └─ SwitchKeyboard → viewModel.handleIntent(ImeIntent.SwitchKeyboard)

 IndicatorOverlay（Row1/2/3 各一）从 feedbackState.fingerIndicator 读取
    → 根据 targetRow 过滤归属本行的指示器
    → 归一化坐标 denormalize 到本行面板尺寸
    → 绘制手指形状指示器
```

---

## 7 InputActionPlayer 组件结构

### 7.1 组件结构

```
InputActionPlayerPanel                     ← 输入动作播放组件
├── ImeInputPanel                          ← 内嵌输入法主组件
├── InputActionPlayer                      ← 播放引擎（逻辑层）
│   ├── ActionScript                       ← 动作脚本
│   ├── KeyPositionResolver                ← 按键位置解析器
│   │   ├── resolve(key) → Offset?         ← 按键坐标（针对 KeyLayoutPanel 归一化）
│   │   ├── resolveCandidatePosition(index) → Offset?
│   │   └── resolveInputItemPosition(index) → Offset?
│   └── PlaybackState                      ← 播放状态机
│
├── ActionPlayerControlBar                 ← 播放控制栏
│   ├── Play/Pause/Stop 按钮
│   ├── StepForward 按钮
│   └── Speed 滑动条
│
└── TargetTextDisplay                      ← 目标文本展示
```

### 7.2 InputActionPlayerPanel 规格

| 属性 | 说明 |
|------|------|
| **角色** | 对 ImeInputPanel 的上层封装，提供输入动作动画播放和直接输入两种模式 |
| **职责** | ① 封装 ImeInputPanel 提供完整输入能力 ② 管理 InputActionPlayer 的生命周期 ③ 控制 IndicatorOverlay 的显示/隐藏 ④ 提供播放控制 UI |
| **关键属性** | `viewModel: KeyboardViewModel`，`player: InputActionPlayer`，`mode: PlayerMode` |
| **约束** | 动画播放模式下不支持打断（一次播完）；直接输入模式下提供完整输入能力；IndicatorOverlay 在播放时开启，播放结束后关闭 |

```kotlin
enum class PlayerMode {
    /** 动画播放模式：播放预设脚本，不支持打断 */
    Playback,
    /** 直接输入模式：提供完整输入能力，可手动触发单步播放 */
    DirectInput,
}

@Composable
fun InputActionPlayerPanel(
    viewModel: KeyboardViewModel,
    player: InputActionPlayer,
    mode: PlayerMode,
    modifier: Modifier = Modifier,
) {
    // 播放时开启指示器遮罩
    val isPlaying = player.playbackState.collectAsState().value is PlaybackState.Playing
    LaunchedEffect(isPlaying) {
        viewModel.handleIntent(ImeIntent.UpdateConfig(
            config = viewModel.state.value.config.copy(
                ui = viewModel.state.value.config.ui.copy(indicatorOverlayEnabled = isPlaying)
            )
        ))
    }

    Column(modifier = modifier) {
        // 目标文本展示
        TargetTextDisplay(...)

        // 输入法主组件
        ImeInputPanel(viewModel = viewModel, modifier = Modifier.weight(1f))

        // 播放控制栏
        ActionPlayerControlBar(player = player, mode = mode)
    }
}
```

### 7.3 InputActionPlayer 执行流

```
1. player.load(script)         ← 加载脚本
       │
2. player.play()               ← 开始播放
       │
3. 遍历 script.actions:
   │
   ├── InputAction.KeyDown(key)
   │   ├── positionResolver.resolve(key) → 归一化坐标（针对 KeyLayoutPanel）
   │   ├── feedbackState.setFingerIndicator(
   │   │     FingerIndicatorState(normalizedPosition, pressed=true,
   │   │                           targetRow=Row3))
   │   └── viewModel.handleIntent(ImeIntent.PressKey(key, Tap))
   │
   ├── InputAction.SwipeTo(from, to, duration)
   │   ├── fromPos = positionResolver.resolve(from) → 归一化坐标
   │   ├── toPos = positionResolver.resolve(to) → 归一化坐标
   │   ├── path = SwipePathInterpolator.interpolate(fromPos, toPos)
   │   │   （路径点均为归一化坐标）
   │   ├── animateFingerAlongPath(feedbackState, path, duration)
   │   └── viewModel.handleIntent(ImeIntent.PressKey(to, Swipe))
   │
   ├── InputAction.KeyUp(key)
   │   └── feedbackState.setFingerIndicator(
   │         FingerIndicatorState(normalizedPosition, pressed=false,
   │                               targetRow=Row3))
   │
   ├── InputAction.SelectCandidate(index)
   │   ├── positionResolver.resolveCandidatePosition(index) → 归一化坐标
   │   ├── feedbackState.setFingerIndicator(
   │   │     FingerIndicatorState(normalizedPosition, pressed=true,
   │   │                           targetRow=Row1))
   │   ├── delay(100ms)
   │   ├── feedbackState.setFingerIndicator(
   │   │     FingerIndicatorState(normalizedPosition, pressed=false,
   │   │                           targetRow=Row1))
   │   └── viewModel.handleIntent(ImeIntent.SelectCandidate(...))
   │
   ├── InputAction.SelectInputItem(index)        ← 新增：输入列表交互
   │   ├── positionResolver.resolveInputItemPosition(index) → 归一化坐标
   │   ├── feedbackState.setFingerIndicator(
   │   │     FingerIndicatorState(normalizedPosition, pressed=true,
   │   │                           targetRow=Row2))
   │   └── viewModel.handleIntent(ImeIntent.MoveCursorTo(index))
   │
   └── InputAction.SwitchKeyboard(targetType)
       └── viewModel.handleIntent(ImeIntent.SwitchKeyboard(targetType))
       │
4. 播放完成
   ├── feedbackState.setFingerIndicator(null)    ← 清除指示器
   └── playbackState = PlaybackState.Finished
```

### 7.4 KeyPositionResolver 扩展

```kotlin
interface KeyPositionResolver {
    /**
     * 查找指定按键在当前 KeyLayoutPanel 中的归一化中心坐标。
     * 坐标针对 KeyLayoutPanel 归一化，渲染时需根据面板实际尺寸转换。
     */
    fun resolve(key: InputKey): NormalizedPoint?

    /** 查找候选项在 CandidateListPanel 中的归一化中心坐标 */
    fun resolveCandidatePosition(index: Int): NormalizedPoint?

    /** 查找输入项在 InputListPanel 中的归一化中心坐标 */
    fun resolveInputItemPosition(index: Int): NormalizedPoint?
}
```

### 7.5 IndicatorOverlay 机制

**设计原则**：指示器遮罩内置于 B 区各行中，通过 `indicatorOverlayEnabled` 开关控制可见性。

**工作流程**：

```
1. InputActionPlayer 执行动作
2. 计算目标位置的归一化坐标（针对目标面板归一化）
3. 设置 FingerIndicatorState(normalizedPosition, targetRow)
4. IndicatorOverlay 在对应行中读取 FingerIndicatorState
5. 过滤 targetRow 匹配本行的指示器
6. 将归一化坐标 denormalize 为本行面板的绝对坐标
7. 在面板上层绘制手指形状指示器
```

**分行指示器定位**：

| 目标行 | 目标面板 | 坐标归一化基准 | 指示器内容 |
|--------|----------|---------------|-----------|
| Row1 | CandidateListPanel | CandidateListPanel 尺寸 | 手指点击候选 |
| Row2 | InputListPanel | InputListPanel 尺寸 | 手指点击输入项 |
| Row3 | KeyLayoutPanel | KeyLayoutPanel 尺寸 | 手指按键/滑行轨迹 |

> **KeyLayoutPanel 归一化坐标的双重使用**：KeyLayoutPanel 归一化坐标同时用于两个场景：① GestureFeedbackPanel_A/B 绘制按键高亮和路径（通过 `keyLayoutPanelLayoutInfo` 中的按键 Rect 映射）；② IndicatorOverlay 绘制手指指示器（通过 `KeyPositionResolver` 获取归一化坐标后 denormalize）。两者共享同一归一化基准（KeyLayoutPanel 尺寸），确保视觉对齐。

---

## 8 与现有 v4 设计的对照说明

### 8.1 沿用的设计概念

| 概念 | 说明 | 来源文档 |
|------|------|----------|
| **三层面板分离** | GestureInputPanel / GestureFeedbackPanel / KeyGridPanel 三层解耦架构 | 020-面板三层分离设计 |
| **MVI 数据流** | ImeIntent → ImeEngine → ImeState 单向数据流 | 010-架构总览 |
| **KeyboardViewModel** | UI 协调中心，持有 ImeEngine，暴露 StateFlow | 060-KeyboardViewModel |
| **GestureFeedbackState** | 手势反馈状态独立于 ImeState | 020-面板三层分离设计 |
| **InputGesture / ImeIntent 双层转换** | InputGesture 表达手势，ImeIntent 表达意图 | 020-面板三层分离设计 |
| **坐标无关设计** | ActionScript 不含绝对坐标，回放时动态解析 | 060-输入动作程序化 |
| **三层库架构** | :ime-engine → :ime-ui → :app | 030-三层模块划分 |
| **ImeOutputBridge** | 引擎输出桥接模式 | 090-输出桥接机制 |
| **KeyboardState 状态机** | 引擎层键盘状态管理 | 020-键盘状态机 |
| **InputList 不可变模型** | 输入列表数据结构 | 030-输入列表 |
| **主题系统** | KeyboardTheme / KeyboardColors | 010-UI 库设计总览 |

### 8.2 新引入的概念

| 概念 | 引入原因 | 与现有概念的关系 |
|------|----------|-----------------|
| **InteractionMode** | 明确区分"按键如何布局和交互"与"输入什么内容"两个维度 | 对原 `KeyGridPanel` 子类（Standard/Emoji/Candidate/CommitOption）按布局方式重新分类 |
| **InputMode** | 与 InteractionMode 正交，明确表达输入内容类型 | 对应引擎层 `KeyboardType`，在 UI 层用独立枚举表达 |
| **ZoneA / ZoneB** | 显式化屏幕分区模型，替代原隐式的"按键区域/输入区域"概念 | 对原 `LayoutMode.FullScreen` 的 `inputPanelRatio` 做结构化扩展 |
| **InfoTipPanel** | 将原散落在各处的临时提示信息统一为独立面板 | 从 CandidateListPanel 和其他面板的提示功能中独立出来 |
| **ToolsPanel** | 明确工具面板与输入列表的互斥切换逻辑 | 由原 `Toolbar` 重构，增加显隐状态切换 |
| **IndicatorOverlay** | 将原全局 FingerOverlay 内置到各行，支持分行指示器定位 | 替代原 `FingerOverlay` 独立组件方案 |
| **ImeInputPanel** | 统一原 `KeyboardPanel` / `KeyboardScreen` 为单一入口组件 | 合并原两个集成组件，通过 LayoutMode 切换内部布局 |
| **InputActionPlayerPanel** | 将输入动作播放能力封装为独立组件 | 替代原 `ExerciseScreen` 中的内嵌播放逻辑 |
| **坐标归一化** | 统一 GestureInputPanel/GestureFeedbackPanel/IndicatorOverlay 间的坐标映射 | 对原 GestureFeedbackState 中绝对坐标存储的改进 |

### 8.3 修改的概念

| 原概念 | 修改后 | 修改原因 |
|--------|--------|----------|
| `KeyGridPanel` | `KeyLayoutPanel` | 职责扩展：不仅渲染按键网格，还负责按交互模式调度不同布局策略 |
| `StandardKeyGridPanel` 等 | `RectGridKeyLayoutPanel` 等 | 命名体现交互模式维度，而非键盘变体 |
| `LayoutMode.Overlay` | `LayoutMode.Stacked` | 语义更精确：组件在 B 区内堆叠 |
| `LayoutMode.FullScreen` | `LayoutMode.Separated` | 语义更精确：输入与键盘分离到不同区域 |
| `KeyboardPanel` / `KeyboardScreen` | `ImeInputPanel`（统一入口） | 两种模式共享大部分组件，统一入口减少重复代码 |
| `Toolbar` | `ToolsPanel` | 职责重构：增加与 InputListPanel 的互斥切换逻辑 |
| `GestureFeedbackState.touchTrailPoints` | `GestureFeedbackState.normalizedTouchTrailPoints` | 改为归一化坐标存储，支持多区域面板的坐标转换 |
| `FingerIndicatorState.position: Offset` | `FingerIndicatorState.normalizedPosition: NormalizedPoint` | 使用归一化坐标，支持分行 IndicatorOverlay 定位 |
| `FingerOverlay`（独立组件） | `IndicatorOverlay`（内置到各行） | 分行遮罩可精确控制坐标转换和触摸穿透 |
| `ExerciseScreen` | `InputActionPlayerPanel` | 播放能力独立为组件，可与 ImeInputPanel 灵活组合 |
| `GestureFeedbackPanelSet.OverlaySet` | `GestureFeedbackPanelSet.StackedSet` | 命名与 LayoutMode.Stacked 对齐 |
| `GestureFeedbackPanelSet.FullScreenSet` | `GestureFeedbackPanelSet.SeparatedSet` | 命名与 LayoutMode.Separated 对齐 |

### 8.4 废弃的概念

| 废弃概念 | 替代方案 | 废弃原因 |
|----------|----------|----------|
| `KeyboardPanel`（叠加模式集成组件） | `ImeInputPanel` + `LayoutMode.Stacked` | 统一为单一入口组件 |
| `KeyboardScreen`（全屏模式集成组件） | `ImeInputPanel` + `LayoutMode.Separated` | 统一为单一入口组件 |
| `FingerOverlay`（全局手指指示器） | `IndicatorOverlay`（分行内置） | 分行遮罩更精确 |
| `KeyGridPanel`（含子类命名） | `KeyLayoutPanel`（含交互模式子类） | 职责扩展，命名更新 |
| `ThreeLayerKeyboardArea` | B 区 `KeyboardRow` 内的三层叠加 | 结构更清晰 |
| `InputScreen` | `ImeInputPanel` | 命名统一 |

### 8.5 改进说明

#### 改进 1：交互模式与输入模式的二维分离

**原有问题**：原 `KeyGridPanel` 的子类（Standard/Emoji/Candidate/CommitOption）混合了"布局方式"和"内容类型"两个维度。`StandardKeyGridPanel` 处理拼音、拉丁、数字等多种输入，但无法表达同一输入在不同布局方式下的差异（如拼音的矩形布局 vs 六边形布局）。

**改进方案**：引入 `InteractionMode` × `InputMode` 二维矩阵。`KeyLayoutPanel` 先按 `InteractionMode` 选择布局策略，再按 `InputMode` 填充按键内容。这确保了同一交互模式下不同输入模式的交互逻辑统一（如 RectGrid 下拼音和拉丁都是点击/滑行），同一输入模式在不同交互模式下展示不同（如拼音在 RectGrid 和 HexGrid 下视觉差异）。

#### 改进 2：屏幕分区模型的结构化

**原有问题**：原 `LayoutMode.FullScreen` 仅定义了 `inputPanelRatio`，没有结构化 A/B 区的内部布局。B 区的候选栏、输入栏、工具栏在 `KeyboardPanel` 中以 Column 直接排列，缺乏明确的行级组织。

**改进方案**：显式定义 ZoneA/ZoneB 和 Row1/Row2/Row3 结构，使面板的空间分配和共存规则一目了然。B 区三行结构在两种布局模式下共用，仅 Row3 的组件构成因布局模式不同而变化。

#### 改进 3：坐标归一化统一

**原有问题**：原 `GestureFeedbackState.touchTrailPoints` 存储绝对坐标，在分离模式下需要手动映射。原 `FingerIndicatorState.position` 也是绝对坐标，无法在分行 IndicatorOverlay 中精确定位。

**改进方案**：统一使用归一化坐标存储。`GestureFeedbackState` 和 `FingerIndicatorState` 中的坐标均针对源面板归一化，渲染时根据目标面板尺寸反归一化。这简化了多实例 GestureFeedbackPanel 和分行 IndicatorOverlay 的坐标映射逻辑。

#### 改进 4：IndicatorOverlay 内置化

**原有问题**：原 `FingerOverlay` 作为全局覆盖层叠加在整个键盘上，无法区分候选栏、输入栏和键盘行上的指示器位置。对候选栏和输入栏的点击动画需要在整个键盘区域定位，坐标计算复杂。

**改进方案**：IndicatorOverlay 内置于 B 区各行中，每行有独立的遮罩和坐标空间。InputActionPlayer 通过 `targetRow` 属性指定指示器应在哪一行显示，各行 IndicatorOverlay 只处理归属自己的指示器。这简化了坐标映射，也避免了一行上的指示器遮挡其他行的交互。
