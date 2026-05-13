# 400 — UI Compose 迁移设计

## 1. 概述

Java 版本使用传统 View 系统（自定义 View、RecyclerView、FlexboxLayout 等）构建 UI。v4 版本迁移到 Jetpack Compose，利用其声明式范式简化 UI 代码，同时利用 Compose 1.8 的新特性（AutoSize、智能省略号、触觉反馈等）提升 IME 的用户体验。

---

## 2. Java 版本 UI 层分析

### 2.1 View 体系概览

| 组件 | Java 实现 | 代码量 | 复杂度 |
|------|----------|--------|--------|
| **KeyboardView** | 自定义 RecyclerView + LayoutManager | ~800 行 | 高 |
| **InputListView** | 自定义 RecyclerView + LayoutManager | ~600 行 | 高 |
| **CandidatesView** | FlexboxLayout + 自定义分页 | ~400 行 | 中 |
| **FavoriteboardView** | 自定义 RecyclerView | ~300 行 | 中 |
| **XPadView** | 自定义 Canvas 绘制 | ~500 行 | 高 |
| **MainboardView** | 组合容器 | ~200 行 | 低 |
| **InputboardView** | 组合容器 | ~150 行 | 低 |
| **ViewGestureDetector** | 自定义手势检测 | ~300 行 | 高 |
| **ViewGestureTrailer** | 手势轨迹绘制 | ~200 行 | 中 |
| **13 种 ViewHolder** | 各类按键和输入的视图 | ~1500 行 | 中 |

### 2.2 View 层与模型层的交互

```
View → UserKeyMsg → IMEditorView → IMEService → IMEditor
IMEditor → InputMsg → IMEService → IMEditorView → View
```

每次 UI 更新都需要手动分发 InputMsg 并更新对应的 View，命令式操作大量 `setVisibility()`、`setText()`、`setAdapter()` 等。

---

## 3. v4 Compose UI 设计

### 3.1 整体架构

> **注意**：键盘区域的三层面板架构（GestureInputPanel / GestureFeedbackPanel / KeyGridPanel）详见文档 150。本节仅描述 Compose 迁移的组件对照，不重复三层面板的设计细节。

```kotlin
@Composable
fun InputScreen(viewModel: KeyboardViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    KeyboardTheme(themeType = state.config.ui.themeType) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // 候选项栏（浮动/固定）
            CandidatePanel(
                candidates = state.candidates,
                onCandidateSelected = { viewModel.handleIntent(ImeIntent.CandidateSelected(it)) },
            )

            // 输入栏
            InputListPanel(
                inputList = state.inputList,
                onGapTapped = { viewModel.handleIntent(ImeIntent.CursorMoveTo(it)) },
            )

            // 键盘区域（三层面板组合，详见文档 150）
            KeyboardPanel(
                engine = viewModel.engine,
            )

            // 工具栏（键盘上方的控制栏）
            Toolbar(
                keyboardType = state.keyboardType,
                config = state.config,
                onSwitchKeyboard = { viewModel.handleIntent(ImeIntent.SwitchKeyboard(it)) },
            )
        }

        // 浮动 UI
        ClipTipPopup(state.clipboard, onPaste = { ... })
        CommitOptionPopup(state.commitOptions, onSelect = { ... })
    }
}
```

### 3.2 ComposeView 桥接

```kotlin
class IMEService : InputMethodService() {
    private var composeView: ComposeView? = null

    override fun onCreateInputView(): View {
        return ComposeView(this).also { composeView = it }.apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
            )
            setContent {
                val viewModel: KeyboardViewModel = viewModel(
                    factory = KeyboardViewModelFactory(this@IMEService)
                )
                InputScreen(viewModel = viewModel)
            }
        }
    }

    override fun onDestroy() {
        composeView?.disposeComposition()
        composeView = null
        super.onDestroy()
    }
}
```

### 3.3 键盘视图

> **注意**：键盘视图在 v4 中拆分为三层面板（详见文档 150）：KeyGridPanel（纯展示，不处理触摸）、GestureInputPanel（透明手势层）、GestureFeedbackPanel（透明反馈绘制层）。KeyGridPanel 中的 KeyView 不处理触摸事件，触摸由 GestureInputPanel 统一拦截。以下仅展示 KeyGridPanel 中的按键渲染逻辑。

```kotlin
// KeyGridPanel：纯展示层，不处理触摸事件
@Composable
fun KeyGridPanel(
    keyboardType: KeyboardType,
    keyGrid: List<List<InputKey>>,
    keyboardState: KeyboardState,
    onLayoutInfoChanged: (KeyGridPanelLayoutInfo) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (keyboardType) {
        KeyboardType.Pinyin, KeyboardType.Latin, KeyboardType.Number,
        KeyboardType.Symbol, KeyboardType.Editor, KeyboardType.Math,
        -> StandardKeyGridPanel(keyGrid, keyboardState, onLayoutInfoChanged, modifier)

        KeyboardType.Emoji -> EmojiKeyGridPanel(keyGrid, keyboardState, onLayoutInfoChanged, modifier)
        KeyboardType.Candidate -> CandidateKeyGridPanel(keyGrid, onLayoutInfoChanged, modifier)
        KeyboardType.CommitOption -> CommitOptionKeyGridPanel(keyGrid, onLayoutInfoChanged, modifier)
    }
}
```

完整的三层面板组合（GestureInputPanel + GestureFeedbackPanel + KeyGridPanel）由 `KeyboardPanel` 集成组件封装，详见文档 160 第 5.4 节。

### 3.4 按键视图

> **注意**：KeyView 在 v4 中是纯展示组件，不处理触摸事件，也不绘制手势反馈。触摸由 GestureInputPanel 统一拦截（详见文档 150），手势反馈由 GestureFeedbackPanel 绘制。KeyView 仅渲染按键的常规状态（标签、背景、激活/禁用等持续性状态）。

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

### 3.5 候选项栏

```kotlin
@Composable
fun CandidatePanel(
    candidates: CandidateState,
    onCandidateSelected: (InputWord) -> Unit,
) {
    if (candidates.candidates.isEmpty()) return

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .background(candidatePanelBackgroundColor),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        contentPadding = PaddingValues(horizontal = 8.dp),
    ) {
        items(items = candidates.candidates, key = { it.id }) { candidate ->
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

### 3.6 输入栏

```kotlin
@Composable
fun InputListPanel(
    inputList: InputListState,
    onGapTapped: (Int) -> Unit,
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .height(36.dp)
            .background(inputListPanelBackgroundColor),
        contentPadding = PaddingValues(horizontal = 4.dp),
    ) {
        items indexed items = inputList.inputs, key = { it.id }  { index, item ->
            when (item) {
                is InputItem.Char -> CharInputItem(item)
                is InputItem.Gap -> GapInputItem(
                    isCursor = index == inputList.gapIndex,
                    onTap = { onGapTapped(index) },
                )
                is InputItem.MathExpr -> MathExprInputItem(item)
            }
        }
    }
}
```

---

## 4. X-Pad Compose 迁移

### 4.1 Canvas 绘制

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

### 4.2 手势交互

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

## 5. 滑行输入手势

> **注意**：v4 的滑行手势检测统一由 GestureInputPanel 处理（详见文档 150 第 5 节），手势轨迹绘制由 GestureFeedbackPanel 处理（详见文档 150 第 6 节）。GestureInputPanel 是透明的手势拦截层，识别手势后输出 InputGesture；GestureFeedbackPanel 是独立的透明绘制层，根据 GestureFeedbackState 绘制滑行轨迹、按键高亮等视觉反馈。以下仅列出 Compose 手势 API 的基本用法参考。

### 5.1 Compose 手势 API 参考

```kotlin
// GestureInputPanel 中的手势检测核心逻辑（详见文档 150 第 5.2 节）
Modifier.pointerInput(keyPanelLayout, keyboardType) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        // 根据 keyPanelLayout 查找触摸位置对应的按键
        // 识别手势类型（点击/长按/滑行/翻转）
        // 输出 InputGesture → ViewModel
        // 同步更新 GestureFeedbackState → GestureFeedbackPanel
    }
}
```

### 5.2 Compose Canvas 绘制参考

```kotlin
// GestureFeedbackPanel 中的轨迹绘制（详见文档 150 第 6.4 节）
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

## 6. 设置页面 Compose 迁移

### 6.1 迁移对照

| Java 页面 | Compose 迁移 |
|-----------|-------------|
| `Preferences` (PreferenceFragmentCompat) | `SettingsScreen` (Compose) |
| `PreferencesTheme` | `ThemeSettingsScreen` (Compose) |
| `Guide` (Activity) | `MainScreen` (Compose + Navigation) | 移除 Alpha 用户协议确认逻辑 |
| `ExerciseGuide` | `ExerciseScreen` (Compose) | |
| 12 个 About 页面 | `AboutScreen` (Compose + Navigation) | 移除 `AlphaUserAgreement` 页面 |

### 6.2 设置页面示例

> **注意**：v4 设置页面的详细设计已独立为文档 [920 — 配置界面改进设计](920-config-ui-improvement.md)，包含场景化分组、肯定式命名、即时预览、条件显示、搜索和快捷切换等完整方案。以下仅展示基本框架。

```kotlin
@Composable
fun SettingsScreen(
    config: ImeConfig,
    onConfigChanged: (ImeConfig) -> Unit,
) {
    var searchQuery by rememberSaveable { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        // 搜索栏
        SearchBar(
            query = searchQuery,
            onQueryChange = { searchQuery = it },
            onSearch = {},
            active = false,
            onActiveChange = {},
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            placeholder = { Text("搜索设置") },
        ) {}

        LazyColumn {
            // 外观（高频，默认展开）
            sectionHeader("外观")
            item { ThemeSelector(config.ui.themeType) { onConfigChanged(config.copy(ui = config.ui.copy(themeType = it))) } }
            item { HandModeToggle(config.engine.handMode) { onConfigChanged(config.copy(engine = config.engine.copy(handMode = it))) } }
            item { KeyboardPreview(config) }

            // 输入体验（高频）
            sectionHeader("输入体验")
            item { InputSettings(config, onConfigChanged) }

            // 反馈控制（中频）
            sectionHeader("反馈控制")
            item { FeedbackSettings(config, onConfigChanged) }

            // 低频分组默认折叠
            expandableSection("数据与隐私", defaultExpanded = false) { /* ... */ }
            expandableSection("日志与诊断", defaultExpanded = false) { /* ... */ }
            expandableSection("关于", defaultExpanded = false) { /* ... */ }
        }
    }
}
```

---

## 7. 性能验证计划

### 7.1 关键指标

| 指标 | 目标 | 测量方式 |
|------|------|----------|
| 按键响应延迟 | < 16ms（60fps） | Systrace / Perfetto |
| 键盘切换帧率 | 稳定 60fps | FrameMetrics |
| 内存占用 | 不超过 Java 版本 1.2 倍 | Android Profiler |
| 滑行输入延迟 | < 8ms | 手势事件时间戳 |
| 候选列表滚动 | 无掉帧 | FrameMetrics |

### 7.2 降级方案

如果 Compose 在 IME 环境中无法满足性能要求：

1. **方案 A**：键盘区域使用传统 View，其余 UI 使用 Compose
2. **方案 B**：使用 Compose 但关闭动画和部分特效
3. **方案 C**：完全回退到传统 View（最后手段）

---

## 8. Java 功能完整对照

| Java UI 组件 | Compose 对应 | 改进说明 |
|-------------|-------------|---------|
| `MainboardView` | `InputScreen` 顶层组合 | 声明式布局 |
| `KeyboardView` + `KeyboardViewAdapter` | `StandardKeyboard` + `KeyView` | 移除 Adapter/ViewHolder 模式 |
| `KeyboardViewLayoutManager` | Compose `Row`/`Column` + `Modifier.weight` | 移除自定义 LayoutManager |
| `KeyboardViewGestureListener` | `Modifier.pointerInput` | Compose 手势 API |
| `KeyboardViewKeyAnimator` | Compose 动画 API | 声明式动画 |
| 12 种 `KeyViewHolder` | `KeyContent()` 分支 | 按类型分发 Composable |
| `InputListView` + `InputListViewAdapter` | `InputListPanel` + `LazyRow` | 简化 |
| 5 种 `InputViewHolder` | `InputItem()` 分支 | 按类型分发 Composable |
| `InputQuickListView` | `QuickListPopup` | 浮层 |
| `InputFavoriteListView` | `FavoritesList` + `LazyColumn` | 简化 |
| `CandidatesView` | `CandidatePanel` + `LazyRow` | FlexboxLayout → Compose |
| `FavoriteboardView` | `FavoritesScreen` | Compose |
| `XPadView` + `XPainter` 系列 | `XPadView` + Compose `Canvas` | 统一绘制 API |
| `ViewGestureDetector` | `Modifier.pointerInput` | 标准手势 API |
| `ViewGestureTrailer` | `GestureTrailOverlay` + `Canvas` | Compose Canvas |
| `ShadowDrawable` / `HexagonDrawable` | Compose `drawBehind` | 声明式绘制 |
| `AudioPlayer` | Compose `LocalHapticFeedback` + 音频 | 扩展触觉反馈 |
| `DialogAlert` / `DialogConfirm` | Compose `AlertDialog` | 标准 Dialog |
| `Toast` | Compose `Snackbar` | 标准反馈 |
| `HtmlTextView` | `AnnotatedString.appendHtml()` | Compose 原生 HTML |
| `Preferences` | `SettingsScreen` | Compose 设置页 |
| `Guide` | `MainScreen` | Compose + Navigation |
| 12 个 About Activity | `AboutScreen` + Navigation | 单 Activity，移除 `AlphaUserAgreement` |
