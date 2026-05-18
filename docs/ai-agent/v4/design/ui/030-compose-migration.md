# Compose UI 迁移设计

v4 版本将键盘 UI 迁移到 Jetpack Compose，利用其声明式范式简化 UI 代码，同时利用 Compose 1.8 的新特性（AutoSize、智能省略号、触觉反馈等）提升 IME 的用户体验。

> 键盘区域的三层面板架构（GestureInputPanel / GestureFeedbackPanel / KeyLayoutPanel）详见[020-三层面板分离](020-panel-separation.md)。本节仅描述 Compose 组件架构和迁移设计，不重复三层面板的设计细节。

---

## 1. Compose 组件架构

### 1.1 KeyboardHost（统一集成组件）

> `KeyboardViewModel` 的完整设计见 [060-KeyboardViewModel](060-keyboard-view-model.md)。以下仅展示集成组件与 ViewModel 的交互方式。

```kotlin
@Composable
fun KeyboardHost(viewModel: KeyboardViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val feedbackState = viewModel.feedbackState
    var keyLayoutState by remember { mutableStateOf(KeyLayoutState()) }

    KeyboardTheme(themeType = state.config.ui.themeType) {
        Column(modifier = Modifier) {
            // 候选栏
            CandidateListPanel(
                state = state.candidateList,
                onCandidateSelected = { candidate ->
                    viewModel.handleIntent(ImeIntent.SelectCandidate(candidate))
                },
            )

            // 输入栏
            InputListPanel(
                state = state.inputList,
                onGapTapped = { index ->
                    viewModel.handleIntent(ImeIntent.MoveCursorTo(index))
                },
            )

            // 三层面板叠加区域
            Box {
                // 底层：按键面板
                KeyLayoutPanel(
                    keyboardType = state.keyboardType,
                    keyGrid = state.keyGrid,
                    keyboardState = state.keyboardState,
                    onLayoutStateChanged = { keyLayoutState = it },
                )

                // 中层：反馈面板
                GestureFeedbackPanel(
                    elements = GestureFeedbackPanelSet.OverlaySet.allElements,
                    feedbackState = feedbackState,
                    keyLayoutState = keyLayoutState,
                )

                // 顶层：输入面板
                GestureInputPanel(
                    keyLayoutState = keyLayoutState,
                    keyboardType = state.keyboardType,
                    feedbackState = feedbackState,
                    onGesture = { viewModel.handleGesture(it) },
                )
            }

            // 工具列表
            ToolListPanel(
                keyboardType = state.keyboardType,
                config = state.config,
                onSwitchKeyboard = { viewModel.handleIntent(ImeIntent.SwitchKeyboard(it)) },
            )
        }
    }
}
```

> **注意**：`KeyboardHost` 是 UI 库的统一集成组件，包含候选栏、输入栏、工具列表和三层面板叠加区域（GestureInputPanel / GestureFeedbackPanel / KeyLayoutPanel 直接叠加，无中间包装层），通过 `LayoutMode` 参数支持 Stacked/Separated 两种布局模式。

### 1.2 ComposeView 桥接

`:app` 模块的 `IMEService` 负责创建引擎、挂载桥梁、注入 ViewModel。完整设计见 [060-KeyboardViewModel](060-keyboard-view-model.md) §4。

```kotlin
class IMEService : InputMethodService() {
    private var engine: ImeEngine? = null
    private var bridge: InputConnectionBridge? = null
    private var composeView: ComposeView? = null

    override fun onCreate() {
        super.onCreate()
        // 创建引擎
        engine = ImeEngine.create(
            config = ImeConfig(),
            dictProvider = ImeSqliteDictProvider(this),
        )
        // 创建并挂载输出桥梁（与 ViewModel 无关）
        bridge = InputConnectionBridge { currentInputConnection }
        engine?.attachOutputBridge(bridge!!)
    }

    override fun onCreateInputView(): View {
        val engine = this.engine ?: error("Engine not initialized")
        return ComposeView(this).also { composeView = it }.apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
            )
            setContent {
                val viewModel: KeyboardViewModel = viewModel(
                    factory = KeyboardViewModel.Factory(engine)
                )
                // KeyboardHost 已包含候选栏 + 输入栏 + 工具列表 + 三层面板叠加
                KeyboardHost(viewModel = viewModel)
            }
        }
    }

    override fun onDestroy() {
        // 断开桥梁并销毁引擎
        engine?.detachOutputBridge()
        engine = null
        bridge = null
        composeView?.disposeComposition()
        composeView = null
        super.onDestroy()
    }
}
```

### 1.3 键盘视图

键盘视图在 v4 中拆分为三层面板（详见[020-三层面板分离](020-panel-separation.md)）：KeyLayoutPanel（纯展示，不处理触摸）、GestureInputPanel（透明手势层）、GestureFeedbackPanel（透明反馈绘制层）。KeyLayoutPanel 中的 KeyView 不处理触摸事件，触摸由 GestureInputPanel 统一拦截。以下仅展示 KeyLayoutPanel 中的按键渲染逻辑。

```kotlin
// KeyLayoutPanel：纯展示层，不处理触摸事件
@Composable
fun KeyLayoutPanel(
    keyboardType: KeyboardType,
    keyGrid: List<List<InputKey>>,
    keyboardState: KeyboardState,
    onLayoutStateChanged: (KeyLayoutState) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (keyboardType) {
        KeyboardType.Pinyin, KeyboardType.Latin, KeyboardType.Number,
        KeyboardType.Symbol, KeyboardType.Math,
        -> StandardKeyLayoutPanel(keyGrid, keyboardState, onLayoutStateChanged, modifier)

        KeyboardType.Emoji -> EmojiKeyLayoutPanel(keyGrid, keyboardState, onLayoutStateChanged, modifier)
        KeyboardType.Candidate -> CandidateKeyLayoutPanel(keyGrid, onLayoutStateChanged, modifier)
        KeyboardType.CommitOption -> CommitOptionKeyLayoutPanel(keyGrid, onLayoutStateChanged, modifier)
    }
}
```

三层面板直接在 `KeyboardHost` 中叠加（无中间包装层），`KeyboardHost` 同时包含候选栏、输入栏和工具列表，构成完整的输入法组件。

### 1.4 按键视图

KeyView 在 v4 中是纯展示组件，不处理触摸事件，也不绘制手势反馈。触摸由 GestureInputPanel 统一拦截，手势反馈由 GestureFeedbackPanel 绘制。KeyView 仅渲染按键的常规状态（标签、背景、激活 / 禁用等持续性状态）。

```kotlin
/**
 * 按键视图（纯展示，无触摸处理，无手势反馈）。
 *
 * 按键的"按下"视觉状态由 keyboardState 驱动（持续性状态），
 * 手势触发的临时高亮由 GestureFeedbackPanel 绘制。
 */
@Composable
fun KeyView(
    key: InputKey,
    isActive: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(if (isActive) activeKeyColor else keyBackgroundColor),
        contentAlignment = Alignment.Center,
    ) {
        when (key) {
            is InputKey.Char -> CharKeyContent(key)
            is InputKey.Ctrl -> CtrlKeyContent(key)
            is InputKey.Candidate -> CandidateKeyContent(key)
            is InputKey.MathOp -> MathOpKeyContent(key)
            is InputKey.Symbol -> SymbolKeyContent(key)
            is InputKey.XPad -> XPadKeyContent(key)
            is InputKey.Null -> { /* 空占位 */ }
        }
    }
}

@Composable
fun CharKeyContent(key: InputKey.Char) {
    BasicText(
        text = key.label,
        maxLines = 1,
        autoSize = TextAutoSize.StepBased(
            minFontSize = 10.sp,
            maxFontSize = 18.sp,
            stepSize = 1.sp,
        ),
        style = TextStyle(color = keyForegroundColor),
    )
}
```

### 1.5 候选项栏

```kotlin
@Composable
fun CandidateListPanel(
    state: CandidateList,
    onCandidateSelected: (InputWord) -> Unit,
) {
    if (state.candidateList.isEmpty()) return

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .background(candidatePanelBackgroundColor),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        contentPadding = PaddingValues(horizontal = 8.dp),
    ) {
        items(items = state.candidateList.candidates, key = { it.id }) { candidate ->
            CandidateItem(
                candidate = candidate,
                onClick = { onCandidateSelected(candidate) },
            )
        }
    }
}

@Composable
fun CandidateItem(candidate: InputWord, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .wrapContentWidth()
            .fillMaxHeight()
            .clip(RoundedCornerShape(4.dp))
            .background(candidateChipBackgroundColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            text = candidate.text,
            maxLines = 1,
            autoSize = TextAutoSize.StepBased(
                minFontSize = 12.sp,
                maxFontSize = 16.sp,
                stepSize = 1.sp,
            ),
            overflow = TextOverflow.Ellipsis,
            style = TextStyle(color = candidateForegroundColor),
        )
    }
}
```

### 1.6 输入栏

```kotlin
@Composable
fun InputListPanel(
    state: InputList,
    onGapTapped: (Int) -> Unit,
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .height(36.dp)
            .background(inputListPanelBackgroundColor),
        contentPadding = PaddingValues(horizontal = 4.dp),
    ) {
        itemsIndexed(state.inputs, key = { _, it -> it.id }) { index, item ->
            when (item) {
                is InputItem.Char -> CharInputItem(item)
                is InputItem.Gap -> GapInputItem(
                    isCursor = index == state.gapIndex,
                    onTap = { onGapTapped(index) },
                )
                is InputItem.MathExpr -> MathExprInputItem(item)
            }
        }
    }
}
```

---

## 2. X-Pad Compose 迁移

### 2.1 Canvas 绘制

```kotlin
@Composable
fun XPadView(
    zones: List<XPadZone>,
    currentSpell: String,
    onZoneSelected: (XPadZone) -> Unit,
    modifier: Modifier = Modifier,
) {
    var center by remember { mutableOffsetOf(Offset.Zero) }

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            // 绘制六边形网格
            zones.forEach { zone ->
                drawHexagon(zone, center)
                drawZoneLabel(zone, center)
            }
        }

        // 当前输入的拼音显示在中心
        Text(
            text = currentSpell,
            modifier = Modifier.align(Alignment.Center),
        )
    }
}

private fun DrawScope.drawHexagon(zone: XPadZone, center: Offset) {
    val path = Path().apply {
        val hexCenter = calculateHexCenter(zone.index, center)
        val vertices = calculateHexVertices(hexCenter, zone.radius)
        moveTo(vertices[0])
        for (i in 1..5) lineTo(vertices[i])
        close()
    }
    drawPath(path, color = zoneColor, style = Stroke(width = 2f))
}
```

### 2.2 手势交互

```kotlin
Modifier.pointerInput(zones) {
    awaitEachGesture {
        val down = awaitFirstDown()
        val startZone = findZoneAt(down.position, zones)

        val path = mutableListOf<Offset>(down.position)
        var currentZone = startZone

        do {
            val event = awaitPointerEvent()
            val position = event.changes.first().position
            val zone = findZoneAt(position, zones)

            if (zone != currentZone) {
                // 进入新区域
                currentZone = zone
                onZoneSelected(zone)
            }
            path.add(position)
        } while (event.changes.any { it.pressed })

        // 手势结束
        haptics.performHapticFeedback(HapticFeedbackType.GestureEnd)
    }
}
```

---

## 3. 滑行输入手势处理

> v4 的滑行手势检测统一由 GestureInputPanel 处理（详见[020-三层面板分离](020-panel-separation.md) §3），手势轨迹绘制由 GestureFeedbackPanel 处理（详见[020-三层面板分离](020-panel-separation.md) §4）。以下仅列出 Compose 手势 API 的基本用法参考。

### 3.1 Compose 手势 API 参考

```kotlin
// GestureInputPanel 中的手势检测核心逻辑
Modifier.pointerInput(keyLayoutState, keyboardType) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        // 根据 keyLayoutState 查找触摸位置对应的按键
        // 识别手势类型（点击/长按/滑行/翻转）
        // 输出 InputGesture → ViewModel
        // 同步更新 GestureFeedbackState → GestureFeedbackPanel
    }
}
```

### 3.2 Compose Canvas 绘制参考

```kotlin
// GestureFeedbackPanel 中的轨迹绘制
Canvas(modifier = modifier.fillMaxSize()) {
    if (touchTrailPoints.size >= 2) {
        val path = Path().apply {
            moveTo(touchTrailPoints.first())
            for (i in 1 until touchTrailPoints.size) {
                quadraticBezierTo(
                    touchTrailPoints[i - 1],
                    midpoint(touchTrailPoints[i - 1], touchTrailPoints[i]),
                )
            }
        }
        drawPath(path, color = gestureTrailColor, style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round))
    }
}
```

---

## 4. 性能验证计划

### 4.1 关键指标

| 指标 | 目标 | 测量方式 |
|------|------|----------|
| 按键响应延迟 | < 16ms（60fps） | Systrace / Perfetto |
| 键盘切换帧率 | 稳定 60fps | FrameMetrics |
| 内存占用 | 不超过 Java 版本 1.2 倍 | Android Profiler |
| 滑行输入延迟 | < 8ms | 手势事件时间戳 |
| 候选列表滚动 | 无掉帧 | FrameMetrics |

### 4.2 降级方案

如果 Compose 在 IME 环境中无法满足性能要求：

1. **方案 A**：键盘区域使用传统 View，其余 UI 使用 Compose
2. **方案 B**：使用 Compose 但关闭动画和部分特效
3. **方案 C**：完全回退到传统 View（最后手段）
