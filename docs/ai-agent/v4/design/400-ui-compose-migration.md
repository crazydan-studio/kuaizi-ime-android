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

```kotlin
@Composable
fun IMEScreen(viewModel: IMEViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    KuaiziIMETheme(themeType = state.config.themeType) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // 候选项栏（浮动/固定）
            CandidateBar(
                candidates = state.candidates,
                onCandidateSelected = { viewModel.handleIntent(IMEIntent.CandidateSelected(it)) },
            )

            // 输入栏
            InputBar(
                inputList = state.inputList,
                onGapTapped = { viewModel.handleIntent(IMEIntent.CursorMoveTo(it)) },
            )

            // 键盘区域
            KeyboardArea(
                keyboardType = state.keyboardType,
                keyGrid = state.keyGrid,
                keyboardState = state.keyboardState,
                onKeyPress = { key, gesture ->
                    viewModel.handleIntent(IMEIntent.KeyPressed(key, gesture))
                },
            )

            // 工具栏（键盘上方的控制栏）
            Toolbar(
                keyboardType = state.keyboardType,
                config = state.config,
                onSwitchKeyboard = { viewModel.handleIntent(IMEIntent.SwitchKeyboard(it)) },
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
class KuaiziIMEService : InputMethodService() {
    private var composeView: ComposeView? = null

    override fun onCreateInputView(): View {
        return ComposeView(this).also { composeView = it }.apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
            )
            setContent {
                val viewModel: IMEViewModel = viewModel(
                    factory = IMEViewModelFactory(this@KuaiziIMEService)
                )
                IMEScreen(viewModel = viewModel)
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

```kotlin
@Composable
fun KeyboardArea(
    keyboardType: KeyboardType,
    keyGrid: List<List<InputKey>>,
    keyboardState: KeyboardState,
    onKeyPress: (InputKey, KeyGesture) -> Unit,
) {
    when (keyboardType) {
        KeyboardType.Pinyin, KeyboardType.Latin, KeyboardType.Number,
        KeyboardType.Symbol, KeyboardType.Editor, KeyboardType.Math,
        -> StandardKeyboard(keyGrid, keyboardState, onKeyPress)

        KeyboardType.Emoji -> EmojiKeyboard(keyGrid, keyboardState, onKeyPress)
        KeyboardType.Candidate -> CandidateKeyboard(keyGrid, onKeyPress)
        KeyboardType.CommitOption -> CommitOptionKeyboard(keyGrid, onKeyPress)
    }
}

@Composable
fun StandardKeyboard(
    keyGrid: List<List<InputKey>>,
    keyboardState: KeyboardState,
    onKeyPress: (InputKey, KeyGesture) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        keyGrid.forEachIndexed { rowIndex, row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                row.forEach { key ->
                    KeyView(
                        key = key,
                        isActive = isKeyActive(key, keyboardState),
                        onPress = { onKeyPress(key, KeyGesture.Tap) },
                        onLongPress = { onKeyPress(key, KeyGesture.LongPress) },
                        modifier = Modifier.weight(key.weight),
                    )
                }
            }
        }
    }
}
```

### 3.4 按键视图

```kotlin
@Composable
fun KeyView(
    key: InputKey,
    isActive: Boolean,
    onPress: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(if (isActive) activeKeyColor else keyBackgroundColor)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                    onPress()
                },
                onLongClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongPress()
                },
            ),
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
fun CandidateBar(
    candidates: CandidateState,
    onCandidateSelected: (InputWord) -> Unit,
) {
    if (candidates.candidates.isEmpty()) return

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .background(candidateBarBackgroundColor),
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
            overflow = TextOverflow.MiddleEllipsis,
            style = TextStyle(color = candidateForegroundColor),
        )
    }
}
```

### 3.6 输入栏

```kotlin
@Composable
fun InputBar(
    inputList: InputListState,
    onGapTapped: (Int) -> Unit,
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .height(36.dp)
            .background(inputBarBackgroundColor),
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

### 5.1 手势检测

```kotlin
@Composable
fun rememberSwipeGestureState(
    onSwipePath: (startKey: InputKey, path: List<Offset>) -> Unit,
): SwipeGestureState {
    return remember { SwipeGestureState(onSwipePath) }
}

Modifier.pointerInput(Unit) {
    awaitEachGesture {
        val down = awaitFirstDown()
        val startKey = findKeyAt(down.position)
        val path = mutableListOf(down.position)

        do {
            val event = awaitPointerEvent(PointerEventPass.Main)
            path.addAll(event.changes.map { it.position })
        } while (event.changes.any { it.pressed })

        if (startKey != null && path.size > 2) {
            onSwipePath(startKey, path)
        }
    }
}
```

### 5.2 手势轨迹绘制

```kotlin
@Composable
fun GestureTrailOverlay(
    path: List<Offset>,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        if (path.size < 2) return@Canvas

        val trailPath = Path().apply {
            moveTo(path[0])
            for (i in 1 until path.size) {
                quadraticBezierTo(
                    path[i - 1],
                    midpoint(path[i - 1], path[i]),
                )
            }
        }

        drawPath(
            path = trailPath,
            color = gestureTrailColor,
            style = Stroke(
                width = 3f,
                cap = StrokeCap.Round,
                pathEffect = null,
            ),
        )
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
| `Guide` (Activity) | `GuideScreen` (Compose + Navigation) | 移除 Alpha 用户协议确认逻辑 |
| `ExerciseGuide` | `ExerciseScreen` (Compose) | |
| 12 个 About 页面 | `AboutScreen` (Compose + Navigation) | 移除 `AlphaUserAgreement` 页面 |

### 6.2 设置页面示例

```kotlin
@Composable
fun SettingsScreen(
    config: Config,
    onConfigChanged: (Config) -> Unit,
) {
    LazyColumn {
        item { SettingsSectionHeader("外观") }
        item {
            ThemeSelector(
                currentTheme = config.themeType,
                onThemeSelected = { onConfigChanged(config.copy(themeType = it)) },
            )
        }
        item {
            HandModeSelector(
                currentHandMode = config.handMode,
                onHandModeSelected = { onConfigChanged(config.copy(handMode = it)) },
            )
        }

        item { SettingsSectionHeader("输入") }
        item {
            SwitchPreference(
                title = "X-Pad 输入",
                description = "启用 X-Pad 连续输入模式",
                checked = config.enableXPad,
                onCheckedChange = { onConfigChanged(config.copy(enableXPad = it)) },
            )
        }
        item {
            SwitchPreference(
                title = "繁体优先",
                description = "候选字优先显示繁体异体字",
                checked = config.enableVariantFirst,
                onCheckedChange = { onConfigChanged(config.copy(enableVariantFirst = it)) },
            )
        }

        // ... 更多设置项
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
| `MainboardView` | `IMEScreen` 顶层组合 | 声明式布局 |
| `KeyboardView` + `KeyboardViewAdapter` | `StandardKeyboard` + `KeyView` | 移除 Adapter/ViewHolder 模式 |
| `KeyboardViewLayoutManager` | Compose `Row`/`Column` + `Modifier.weight` | 移除自定义 LayoutManager |
| `KeyboardViewGestureListener` | `Modifier.pointerInput` | Compose 手势 API |
| `KeyboardViewKeyAnimator` | Compose 动画 API | 声明式动画 |
| 12 种 `KeyViewHolder` | `KeyContent()` 分支 | 按类型分发 Composable |
| `InputListView` + `InputListViewAdapter` | `InputBar` + `LazyRow` | 简化 |
| 5 种 `InputViewHolder` | `InputItem()` 分支 | 按类型分发 Composable |
| `InputQuickListView` | `QuickListPopup` | 浮层 |
| `InputFavoriteListView` | `FavoritesList` + `LazyColumn` | 简化 |
| `CandidatesView` | `CandidateBar` + `LazyRow` | FlexboxLayout → Compose |
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
| `Guide` | `GuideScreen` | Compose + Navigation |
| 12 个 About Activity | `AboutScreen` + Navigation | 单 Activity，移除 `AlphaUserAgreement` |
