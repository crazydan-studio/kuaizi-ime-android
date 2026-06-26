# Compose 组件

`:ui` 模块的 Compose 组件层定义了输入法界面中所有可视元素的可组合函数（`Composable`）。这些组件遵循单一职责原则：展示层仅负责渲染与状态读取，交互逻辑统一由 `GestureInputPanel` 捕获后以 `InputGesture` 事件上发至 `ViewModel`。本章详细描述每个组件的签名、职责边界与内部实现要点。

---

## 1. KeyboardHost 集成组件

`KeyboardHost` 是输入法界面的顶层集成组件，作为 `:ui` 模块对外暴露的唯一入口，被 `ImeService` 在创建输入视图时调用。

```kotlin
@Composable
fun KeyboardHost(
    viewModel: KeyboardViewModel,
    modifier: Modifier = Modifier,
)
```

### 布局模式

`KeyboardHost` 根据 `KeyboardLayoutMode` 选择 Stacked 或 Separated 布局：

**Stacked 布局**：所有组件集中在 Zone B，按 Row 1→Row 2→Row 3 垂直排列。

```
┌──────────────────────────┐
│  Row 1: CandidateListPanel   │
│         + PopupTipPanel      │
├──────────────────────────┤
│  Row 2: ToolListPanel     │  ← 与 InputListPanel 互斥
│         or InputListPanel │
├──────────────────────────┤
│  Row 3: KeyLayoutPanel    │
│         GestureFeedback   │
│         GestureInput      │  ← 三层叠加
└──────────────────────────┘
```

**Separated 布局**：Zone A 和 Zone B 分离。

```
┌──────────────────────────┐
│  Zone A: KeyLayoutPanel      │
│          GestureFeedback      │
├──────────────────────────┤
│  Row 1: CandidateListPanel   │
├──────────────────────────┤
│  Row 2: ToolListPanel     │
│         or InputListPanel │
├──────────────────────────┤
│  Row 3: GestureFeedback   │
│          GestureInput      │
└──────────────────────────┘
```

```kotlin
@Composable
fun KeyboardHost(viewModel: KeyboardViewModel) {
    // 使用 snapshotFlow + derivedStateOf 分别订阅独立子状态
    // 任一子状态变化仅触发依赖该子状态的组件重组
    val keyboard by viewModel.state.let { state ->
        remember { derivedStateOf { state.value.keyboard } }
    }
    val inputList by viewModel.state.let { state ->
        remember { derivedStateOf { state.value.inputList } }
    }
    val candidateList by viewModel.state.let { state ->
        remember { derivedStateOf { state.value.candidateList } }
    }
    val config by viewModel.state.let { state ->
        remember { derivedStateOf { state.value.config } }
    }
    
    val layoutMode by viewModel.layoutMode.collectAsState()
    val popupTipState by viewModel.popupTipState.collectAsState()
    val toolListState by viewModel.toolListState.collectAsState()
    val feedbackState = viewModel.feedbackState
    
    // KeyboardTheme 仅依赖 config.ui 变化
    KeyboardTheme(config.ui) {
        // 各面板组件仅接收其所需的最小状态切片
        // KeyLayoutPanel 仅依赖 keyboard 变化
        // CandidateListPanel 仅依赖 candidateList 变化
        // InputListPanel/ToolListPanel 互斥组件仅依赖 inputList 变化
        when (layoutMode) {
            KeyboardLayoutMode.Stacked -> StackedLayout(
                keyboard = keyboard,
                inputList = inputList,
                candidateList = candidateList,
                // ...
            )
            KeyboardLayoutMode.Separated -> SeparatedLayout(
                keyboard = keyboard,
                inputList = inputList,
                candidateList = candidateList,
                // ...
            )
        }
    }
}
```

通过 derivedStateOf 分别订阅 ImeState 的各个子字段，任一子字段的变化仅触发依赖该字段的面板重组。例如键盘按键面板仅订阅 keyboard 字段，输入列表面板仅订阅 inputList 字段——候选列表更新时键盘按键面板不会重组。Collecting the full ImeState at the top level would cause the entire keyboard tree to recompose on every state change (every keystroke, candidate update, clipboard change, etc.).

`KeyboardHost` 从 `viewModel` 读取 `ImeState`，并通过 `KeyboardTheme` 提供当前主题色彩上下文。所有子组件的 `modifier` 均由 `KeyboardHost` 统一管理，确保布局一致性。`KeyboardHost` 自身不处理任何触摸事件，所有交互由 `GestureInputPanel` 统一捕获后转换为 `InputGesture` 事件。

---

## 2. KeyLayoutPanel 按键布局面板

`KeyLayoutPanel` 负责按键布局的渲染和状态展示，根据当前键盘的 `InputMode` 将渲染委托给具体的子面板实现。该组件仅负责展示，不处理任何触摸事件。

```kotlin
@Composable
fun KeyLayoutPanel(
    keyTable: List<List<InputKey>>,
    generator: KeyTableGenerator,
    context: KeyTableContext,
    keyboardInputMode: KeyboardInputMode,
    keyLayoutState: KeyLayoutState,
    onLayoutStateChanged: (KeyLayoutState) -> Unit,
    modifier: Modifier = Modifier,
)
```

### 布局状态回调

`KeyLayoutPanel` 在每次布局测量完成后，通过 `onLayoutStateChanged` 回调上报 `KeyLayoutState`，包含每个按键的归一化位置映射和面板尺寸。该回调将状态同步至 `KeyboardViewModel` 的布局状态缓存，供 `GestureInputPanel` 触摸定位和 `ComposeInputActionPositionResolver` 坐标解析使用。

### 按键矩阵渲染

`KeyLayoutPanel` 根据 `keyTable` 二维矩阵逐行渲染按键。外层 `List` 对应键盘的行，内层 `List` 对应每行中的按键。布局计算使用 Compose 的 `Layout` 可组合函数进行自定义测量与放置，根据 `KeyboardInputMode` 决定按键间距和整体布局边距——`HexGrid` 模式下按键呈六边形排列，`RectGrid` 模式下按键呈矩形排列。

---

## 3. KeyView 按键视图

`KeyView` 是单个按键的渲染组件，根据按键类型分发至对应的视觉实现。该组件仅负责展示，不处理触摸事件。

```kotlin
@Composable
fun KeyView(
    inputKey: InputKey,
    isPressed: Boolean,
    modifier: Modifier = Modifier,
)
```

### 按键类型分发

`KeyView` 根据 `InputKey` 的语义类型进行分发渲染：

| `InputKey` 类型 | 渲染方式 |
|----------------|----------|
| `InputKey.Char` | 显示字符标签，背景色根据 `isPressed` 切换 |
| `InputKey.Ctrl` | 显示功能图标或文本标签，差异化背景色 |
| `InputKey.Candidate` | 显示候选词内容，带有选中高亮 |
| `InputKey.MathOp` | 显示数学运算符号，等宽字体 |
| `InputKey.Symbol` | 显示符号标签 |
| `InputKey.Null` | 不渲染任何内容，仅占据布局空间 |

### 视觉实现

按压状态通过 `KeyboardColors` 中的按键颜色控制色彩变化，配合 `animateColorAsState` 实现平滑过渡动画。按键的圆角半径由 `KeyboardColors.keyCornerShape` 统一控制，确保视觉一致性。

### 性能说明

KeyView 的 isPressed 参数来自 GestureFeedbackState.pressedKeys——这是一个非 StateFlow 的手势反馈状态。手势过程中 pressedKeys 通过 GestureInputPanel 的直接方法调用更新，不经过 StateFlow 发射链路，避免了 60fps 更新对 Compose 重组系统造成的压力。

---

## 4. GestureInputPanel 手势输入面板

`GestureInputPanel` 是键盘界面中唯一的触摸事件捕获组件，负责检测用户手势、确定手势类型、归一化坐标并发射 `InputGesture` 事件。该组件以透明覆盖层形式叠加在所有其他面板之上。

```kotlin
@Composable
fun GestureInputPanel(
    keyLayoutState: KeyLayoutState,
    onGesture: (InputGesture) -> Unit,
    modifier: Modifier = Modifier,
)
```

### 触摸检测

`GestureInputPanel` 使用 Compose 的 `pointerInput` 修饰符监听触摸事件，在 `detectTapGestures` 和 `detectDragGestures` 的基础上实现自定义手势检测器，能够识别以下手势类型：

| 手势类型 | 触发条件 |
|---------|----------|
| `Tap` | 手指按下后在 150ms 内抬起，且移动距离小于 8dp |
| `LongPress` | 手指按下后保持 400ms 未抬起且未移动 |
| `Swipe` | 手指按下后移动距离超过 20dp，记录途经按键 |
| `Flip` | 快速滑行后松手，根据方向判断 |
| `CandidateTap` | 触摸位置位于候选区时直接选择 |

### 坐标归一化

触摸坐标在发射前进行归一化处理：将像素坐标转换为 `[0f, 1f]` 范围的相对坐标，x 轴相对于面板宽度，y 轴相对于面板高度。归一化坐标通过 `Offset.normalize()` 扩展方法实现，使得下游消费者无需关心屏幕分辨率差异。

### InputGesture 发射

手势识别完成后，`GestureInputPanel` 构造 `InputGesture` 对象并通过 `onGesture` 回调发射。`InputGesture` 包含手势类型、时间戳和按键标识，不包含任何绝对坐标。

---

## 5. GestureFeedbackPanel 手势反馈面板

`GestureFeedbackPanel` 读取 `GestureFeedbackState` 状态，将归一化坐标反归一化为像素坐标，并绘制触摸轨迹、按键高亮和手指指示器。该组件以透明覆盖层形式叠加在按键区域之上，不影响下层组件的布局。

```kotlin
@Composable
fun GestureFeedbackPanel(
    feedbackState: GestureFeedbackState,
    keyLayoutState: KeyLayoutState,
    modifier: Modifier = Modifier,
)
```

### 坐标反归一化

`GestureFeedbackPanel` 在绘制前将 `GestureFeedbackState` 中存储的归一化坐标 `[0f, 1f]` 转换回像素坐标。转换公式为 `pixelX = normalizedX * panelWidth`、`pixelY = normalizedY * panelHeight`，其中面板尺寸通过 `BoxWithConstraints` 获取。反归一化确保反馈视觉效果与用户实际触摸位置精确对齐。

### 触摸轨迹绘制

`GestureFeedbackState.touchTrailPoints` 包含一系列归一化坐标点。`GestureFeedbackPanel` 使用 `Canvas` 绘制贝塞尔曲线连接这些点，线宽从起点到终点逐渐变细（从 4dp 到 1dp），颜色使用 `KeyboardColors.gestureTrailColor` 并带有透明度渐变。

### 按键高亮

`GestureFeedbackState.pressedKeys` 包含当前按下的按键标识集合。`GestureFeedbackPanel` 通过 `keyLayoutState` 找到对应按键的归一化矩形，反归一化后使用 `KeyboardColors.keyPressedHighlightColor` 绘制半透明高亮覆盖层。

### 手指指示器

当 `GestureFeedbackState.fingerIndicator` 不为 `null` 时，`GestureFeedbackPanel` 在指示器位置绘制一个圆形手指图标，圆心对齐归一化坐标反算后的像素位置。指示器半径为 20dp，填充色为 `KeyboardColors.fingerIndicatorColor`，带有脉冲动画效果。

---

## 6. CandidateListPanel 候选列表面板

`CandidateListPanel` 展示当前输入的候选词列表，支持横向滚动和翻页。该组件内建 `InputActionPlayer` 的指示器覆盖层。

```kotlin
@Composable
fun CandidateListPanel(
    candidates: List<CandidateItem>,
    selectedIndex: Int,
    layoutState: CandidateListLayoutState,
    onLayoutStateChanged: (CandidateListLayoutState) -> Unit,
    showIndicator: Boolean = false,
    indicatorState: InputActionFingerIndicator? = null,
    onCandidateTap: (Int) -> Unit,
    modifier: Modifier = Modifier,
)
```

### 候选词渲染

`CandidateListPanel` 使用 `LazyRow` 横向排列候选词。每个 `CandidateItem` 渲染为一个可点击的文本项，选中项通过 `KeyboardColors.candidateSelectedBackground` 高亮。候选词文本字号使用 `KeyboardColors.candidateTextSize`，未选中项使用 `KeyboardColors.candidateTextColor`，选中项使用 `KeyboardColors.candidateSelectedTextColor`。

### 内建指示器

`showIndicator` 参数控制指示器覆盖层的显示。当 `showIndicator` 为 `true` 且 `indicatorState` 不为 `null` 时，`CandidateListPanel` 在候选词列表上方绘制一条水平高亮线，宽度与面板宽度对齐，颜色为 `KeyboardColors.playerIndicatorColor`。指示器的显示由 `InputActionPlayer` 的 `UseMode` 决定：`Animation` 模式下显示，`DirectInput` 模式下不显示。

### 布局状态

`onLayoutStateChanged` 回调在布局测量后上报 `CandidateListLayoutState`，包含各候选项的位置矩形和面板尺寸，供 `InputActionPlayer` 坐标解析使用。

---

## 7. InputListPanel 输入列表面板

`InputListPanel` 渲染已输入的内容项列表，支持三种项目类型：字符输入项、间隔输入项和数学表达式输入项。

```kotlin
@Composable
fun InputListPanel(
    items: List<CharInputItem>,
    cursorIndex: Int,
    layoutState: InputListLayoutState,
    onLayoutStateChanged: (InputListLayoutState) -> Unit,
    showIndicator: Boolean = false,
    indicatorState: InputActionFingerIndicator? = null,
    modifier: Modifier = Modifier,
)
```

### 项目类型渲染

`InputListPanel` 使用 `LazyRow` 横向排列输入项，根据 `CharInputItem` 的类型分发渲染：

| 项目类型 | 渲染方式 |
|---------|----------|
| `CharInputItem` | 显示单个字符，光标位于该项时绘制竖线闪烁动画 |
| `GapInputItem` | 显示为窄间隔条，宽度为 4dp，使用 `KeyboardColors.gapColor` 填充 |

### 光标渲染

`cursorIndex` 指定当前光标位置。`InputListPanel` 在对应项目右侧绘制一条 2dp 宽的竖线，颜色为 `KeyboardColors.cursorColor`，通过 `InfiniteTransition` 实现 500ms 周期的闪烁动画。光标仅在键盘获得焦点时显示。

### 滚动与对齐

当输入项超出面板可见区域时，`InputListPanel` 自动滚动至光标位置，确保当前编辑位置始终可见。滚动使用 `animateScrollToItem` 实现平滑过渡。

---

## 8. PopupTipPanel 弹出提示面板

`PopupTipPanel` 展示两种类型的弹出提示：`Message` 类型用于短暂的信息展示，`Action` 类型用于带操作按钮的可交互提示。该组件以覆盖层形式悬浮于键盘上方。

```kotlin
@Composable
fun PopupTipPanel(
    tipState: PopupTipState?,
    onAction: (ImeIntent) -> Unit,
    modifier: Modifier = Modifier,
)
```

### Message 类型

`PopupTipState.Message` 用于展示短暂的提示信息，显示文本内容后按 `timeoutMs` 自动消失。视觉样式为：圆角矩形背景，文本居中，无操作按钮。提示出现和消失时带有淡入淡出动画，过渡时长 200ms。

### Action 类型

`PopupTipState.Action` 在文本右侧显示一个操作按钮，按钮标签为 `actionLabel`。用户点击按钮时，`PopupTipPanel` 调用 `onAction(action)` 将 `ImeIntent` 上发至 `ViewModel` 处理。`persistent` 属性控制提示的消失策略：`persistent=true` 时提示保持显示直到用户开始新的输入操作；`persistent=false` 时提示在 `timeoutMs` 后自动消失。

---

## 9. ToolListPanel 工具列表面板

`ToolListPanel` 展示键盘工具栏中的工具按钮列表，支持横向滚动。该组件内建 `InputActionPlayer` 的指示器覆盖层。

```kotlin
@Composable
fun ToolListPanel(
    toolListState: ToolListState,
    layoutState: KeyLayoutState,
    onLayoutStateChanged: (KeyLayoutState) -> Unit,
    showIndicator: Boolean = false,
    indicatorState: InputActionFingerIndicator? = null,
    onToolClick: (ImeIntent) -> Unit,
    modifier: Modifier = Modifier,
)
```

### 工具项渲染

`ToolListPanel` 使用 `LazyRow` 横向排列工具项。每个 `ToolItem` 渲染为一个图标按钮，图标使用 `ToolItem.icon` 指定的 `ImageVector`。选中工具通过 `KeyboardColors.toolSelectedBackground` 高亮，未选中工具使用 `KeyboardColors.toolBackground`。

### 内建指示器

`showIndicator` 参数控制指示器覆盖层的显示，逻辑与 `CandidateListPanel` 一致。当激活时，在工具列表下方绘制一条水平高亮线，颜色为 `KeyboardColors.playerIndicatorColor`。

---

## 10. 主题系统

主题系统为所有键盘组件提供统一的色彩方案，支持浅色（`Light`）、夜间（`Night`）和跟随系统三种模式。主题系统基于 Compose 的 `CompositionLocal` 机制实现，确保主题切换时所有组件自动重组。

### KeyboardColors

`KeyboardColors` 定义了键盘界面使用的完整色彩方案，包含所有组件的背景色、前景色、高亮色和辅助色：

```kotlin
data class KeyboardColors(
    val background: Color,
    val keyBackground: Color,
    val keyForeground: Color,
    val keyPressedBackground: Color,
    val keyPressedForeground: Color,
    val keyPressedHighlightColor: Color,
    val functionKeyBackground: Color,
    val functionKeyForeground: Color,
    val keyCornerShape: CornerSize,
    val candidateTextColor: Color,
    val candidateSelectedTextColor: Color,
    val candidateSelectedBackground: Color,
    val candidateTextSize: TextUnit,
    val cursorColor: Color,
    val charInputTextSize: TextUnit,
    val gapColor: Color,
    val gestureTrailColor: Color,
    val fingerIndicatorColor: Color,
    val tipMessageBackground: Color,
    val tipActionBackground: Color,
    val tipTextSize: TextUnit,
    val tipActionButtonColor: Color,
    val toolBackground: Color,
    val toolSelectedBackground: Color,
    val toolDividerColor: Color,
    val toolSpacing: Dp,
    val playerIndicatorColor: Color,
)
```

### 颜色字段说明

| 字段 | 用途 |
|------|------|
| `background` | 键盘整体背景色 |
| `keyBackground` | 普通按键背景色 |
| `keyForeground` | 普通按键前景色（文本/图标） |
| `keyPressedBackground` | 按键按下态背景色 |
| `keyPressedForeground` | 按键按下态前景色 |
| `keyPressedHighlightColor` | 按键按下高亮覆盖层颜色 |
| `functionKeyBackground` | 功能键背景色 |
| `functionKeyForeground` | 功能键前景色 |
| `keyCornerShape` | 按键圆角 |
| `candidateTextColor` | 候选词文本色 |
| `candidateSelectedTextColor` | 选中候选词文本色 |
| `candidateSelectedBackground` | 选中候选词背景色 |
| `candidateTextSize` | 候选词字号 |
| `cursorColor` | 光标颜色 |
| `charInputTextSize` | 输入字符字号 |
| `gapColor` | 输入栏间隔条颜色 |
| `gestureTrailColor` | 手势触摸轨迹颜色 |
| `fingerIndicatorColor` | 手指指示器颜色 |
| `tipMessageBackground` | Message 类型弹出提示背景色 |
| `tipActionBackground` | Action 类型弹出提示背景色 |
| `tipTextSize` | 弹出提示字号 |
| `tipActionButtonColor` | 弹出提示操作按钮颜色 |
| `toolBackground` | 工具按钮背景色 |
| `toolSelectedBackground` | 选中工具按钮背景色 |
| `toolDividerColor` | 工具分组分隔线颜色 |
| `toolSpacing` | 工具按钮间距 |
| `playerIndicatorColor` | 输入动作播放行指示器颜色 |

### KeyboardThemes

`KeyboardThemes` 提供 `Light` 和 `Night` 两套预定义色彩方案：

```kotlin
object KeyboardThemes {
    val Light: KeyboardColors = KeyboardColors(
        background = Color(0xFFF5F5F5),
        keyBackground = Color(0xFFFFFFFF),
        keyForeground = Color(0xFF1A1A1A),
        keyPressedBackground = Color(0xFFE0E0E0),
        keyPressedForeground = Color(0xFF1A1A1A),
        keyPressedHighlightColor = Color(0x33FFFFFF),
        functionKeyBackground = Color(0xFFE8E8E8),
        functionKeyForeground = Color(0xFF333333),
        keyCornerShape = CornerSize(8.dp),
        candidateTextColor = Color(0xFF333333),
        candidateSelectedTextColor = Color(0xFFFFFFFF),
        candidateSelectedBackground = Color(0xFF2196F3),
        candidateTextSize = 16.sp,
        cursorColor = Color(0xFF2196F3),
        charInputTextSize = 18.sp,
        gapColor = Color(0xFFCCCCCC),
        gestureTrailColor = Color(0x664CAF50),
        fingerIndicatorColor = Color(0x80FF9800),
        tipMessageBackground = Color(0xFF333333),
        tipActionBackground = Color(0xFF2196F3),
        tipTextSize = 14.sp,
        tipActionButtonColor = Color(0xFFFFFFFF),
        toolBackground = Color(0xFFE8E8E8),
        toolSelectedBackground = Color(0xFF2196F3),
        toolDividerColor = Color(0xFFCCCCCC),
        toolSpacing = 4.dp,
        playerIndicatorColor = Color(0xFFFF9800),
    )

    val Night: KeyboardColors = KeyboardColors(
        background = Color(0xFF1A1A1A),
        keyBackground = Color(0xFF2D2D2D),
        keyForeground = Color(0xFFE0E0E0),
        keyPressedBackground = Color(0xFF404040),
        keyPressedForeground = Color(0xFFE0E0E0),
        keyPressedHighlightColor = Color(0x33FFFFFF),
        functionKeyBackground = Color(0xFF333333),
        functionKeyForeground = Color(0xFFCCCCCC),
        keyCornerShape = CornerSize(8.dp),
        candidateTextColor = Color(0xFFCCCCCC),
        candidateSelectedTextColor = Color(0xFFFFFFFF),
        candidateSelectedBackground = Color(0xFF1976D2),
        candidateTextSize = 16.sp,
        cursorColor = Color(0xFF64B5F6),
        charInputTextSize = 18.sp,
        gapColor = Color(0xFF555555),
        gestureTrailColor = Color(0x6681C784),
        fingerIndicatorColor = Color(0x80FFB74D),
        tipMessageBackground = Color(0xFF555555),
        tipActionBackground = Color(0xFF1976D2),
        tipTextSize = 14.sp,
        tipActionButtonColor = Color(0xFFFFFFFF),
        toolBackground = Color(0xFF333333),
        toolSelectedBackground = Color(0xFF1976D2),
        toolDividerColor = Color(0xFF555555),
        toolSpacing = 4.dp,
        playerIndicatorColor = Color(0xFFFFB74D),
    )
}
```

### KeyboardTheme 与 LocalKeyboardColors

`KeyboardTheme` 是一个可组合函数，负责根据当前主题模式选择 `KeyboardColors` 实例并通过 `CompositionLocal` 向下传递：

```kotlin
@Composable
fun KeyboardTheme(
    type: KeyboardThemeType,
    content: @Composable () -> Unit,
)

val LocalKeyboardColors = compositionLocalOf { KeyboardThemes.Light }
```

`KeyboardTheme` 读取 `type` 参数（`KeyboardThemeType.Light`、`KeyboardThemeType.Night` 或 `KeyboardThemeType.FollowSystem`），当 `FollowSystem` 时通过 `isSystemInDarkTheme()` 判断系统当前模式。选定 `KeyboardColors` 后，通过 `CompositionLocalProvider` 将其注入 `LocalKeyboardColors`。所有子组件通过 `LocalKeyboardColors.current` 读取色彩值，确保主题切换时界面风格的一致性。
