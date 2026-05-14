# Jetpack Compose 最佳实践

本文档基于 Jetpack Compose BOM 2026.04.01（Compose 1.8），说明在本 IME 项目中使用 Compose 的最佳实践、推荐特性、IME 场景适配和避坑指南。

---

## 1. Compose 在 IME 中的适用性评估

### ✅ 高度适用的场景

| 场景 | Compose 方案 | 优势 |
|------|-------------|------|
| **设置页面** | 标准 Compose 组件 | 声明式 UI、主题统一、代码简洁 |
| **关于页面** | `AnnotatedString` + HTML 支持 | 替代 WebView 展示协议和更新日志 |
| **候选项栏** | `LazyRow` + `autoSize` 文本 | 替代 FlexboxLayout，自动适配文本宽度 |
| **候选字面板** | `LazyVerticalGrid` + 分页 | 替代自定义 RecyclerView |
| **收藏/剪贴板列表** | `LazyColumn` + `SwipeToDismiss` | 标准化列表操作 |
| **输入法引导页** | Compose Navigation + Pager | 声明式引导流程 |
| **Emoji 面板** | `LazyVerticalGrid` + 分类 Tab | 替代自定义分页 RecyclerView |
| **符号面板** | `LazyVerticalGrid` + 分组 | 统一的网格布局 |
| **主题切换** | `isSystemInDarkTheme()` | 自动跟随系统主题 |
| **按键反馈** | 扩展的触觉反馈 API | 丰富的按键触觉反馈类型 |
| **练习系统** | Compose 交互组件 | 替代自定义 View 练习界面 |

### ⚠️ 需谨慎评估的场景

| 场景 | 风险 | 建议 |
|------|------|------|
| **键盘主视图** | IME 的 `InputMethodService.onCreateInputView()` 需返回 `View`，Compose 需通过 `ComposeView` 桥接 | 使用 `ComposeView`，但需关注性能和内存 |
| **按键绘制** | 高频重绘（每次按键），Compose 重组开销需控制 | 使用 `remember`、`derivedStateOf`、`key` 精确控制重组范围 |
| **X-Pad 绘制** | Canvas 自定义绘制（六边形、路径），与 Compose Canvas 的桥接 | 使用 Compose `Canvas` + `drawBehind`/`drawWithContent` |
| **手势轨迹** | 自定义手势检测和轨迹绘制 | 使用 Compose 手势 API + `drawBehind` |
| **滑行输入** | 连续触摸事件处理，需要极低延迟 | `pointerInput` + `awaitPointerEventScope`，注意帧率 |

### ❌ 不适用 Compose 的场景

| 场景 | 原因 | 替代方案 |
|------|------|----------|
| **极端性能要求的自定义绘制** | Compose 重组和布局有固有的帧预算开销 | 降级为传统 `View` + `Canvas` |
| **需要像素级控制的自定义布局** | Compose 布局系统有固有约束 | 自定义 `Layout` 或降级为传统 `View` |

> **核心原则**：IME 的键盘主视图是否使用 Compose 需在原型阶段进行性能验证。如果 Compose 能在目标设备上稳定达到 60fps 的按键响应，则全面采用；否则键盘区域降级为传统 View，其余 UI 使用 Compose。

---

## 2. Compose 1.8 新特性在 IME 中的应用

### 2.1 文本自动缩放（TextAutoSize）— ★★★★★ 关键

键盘按键标签和候选项文本必须适应不同的按键宽度和屏幕尺寸：

```kotlin
// 按键标签自动缩放
BasicText(
    text = key.label,
    maxLines = 1,
    autoSize = TextAutoSize.StepBased(
        minFontSize = 10.sp,
        maxFontSize = 18.sp,
        stepSize = 1.sp,
    ),
)

// 候选项文本自动缩放
BasicText(
    text = candidate.text,
    maxLines = 1,
    autoSize = TextAutoSize.StepBased(
        minFontSize = 12.sp,
        maxFontSize = 16.sp,
        stepSize = 1.sp,
    ),
    overflow = TextOverflow.MiddleEllipsis,
)
```

### 2.2 智能省略号 — ★★★★★ 关键

候选项和剪贴板内容截断：

```kotlin
// 候选项：中间省略，保留首尾
Text(
    text = longCandidateText,
    overflow = TextOverflow.MiddleEllipsis,
    maxLines = 1,
)

// 文件路径：开头省略，保留文件名
Text(
    text = filePath,
    overflow = TextOverflow.StartEllipsis,
    maxLines = 1,
)
```

### 2.3 扩展触觉反馈 — ★★★★★ 关键

```kotlin
val haptics = LocalHapticFeedback.current

// 按键点击
haptics.performHapticFeedback(HapticFeedbackType.ContextClick)

// 长按触发
haptics.performHapticFeedback(HapticFeedbackType.LongPress)

// 输入确认
haptics.performHapticFeedback(HapticFeedbackType.Confirm)

// 手势结束
haptics.performHapticFeedback(HapticFeedbackType.GestureEnd)
```

### 2.4 预测性动画（Predictive Animations）— ★★★★☆ 重要

键盘模式切换动画：

```kotlin
LookaheadScope {
    Box(
        Modifier
            .width(if (expanded) 180.dp else 110.dp)
            .animateBounds(lookaheadScope = this@LookaheadScope)
    ) {
        // 键盘内容
    }
}
```

### 2.5 稳定的多焦点 API — ★★★★★ 关键

IME 需要在输入区、工具栏、候选项之间管理焦点：

```kotlin
Column(Modifier.focusRestorer()) {
    CandidatePanel(Modifier.onFocusChanged { /* ... */ })
    KeyboardPanel(Modifier.onFocusChanged { /* ... */ })
    Toolbar(Modifier.onFocusChanged { /* ... */ })
}
```

### 2.6 可见性追踪 — ★★★★☆ 重要

优化 Emoji 分类的懒加载：

```kotlin
LazyColumn {
    items(emojiGroups) { group ->
        EmojiGroupView(
            group = group,
            modifier = Modifier.onLayRectChanged { rect ->
                // 仅当可见时加载 Emoji 数据
                group.isVisible = rect.isVisible
            },
        )
    }
}
```

### 2.7 Autofill 语义 — ★★★★☆ 重要

IME 的设置页面登录字段：

```kotlin
TextField(
    state = rememberTextFieldState(),
    modifier = Modifier.semantics { contentType = ContentType.Username },
    label = { Text("用户名") },
)
```

### 2.8 可暂停组合 — ★★★★☆ 重要

防止键盘布局切换时的卡顿：

```kotlin
// Compose 1.8 自动启用，无需额外配置
// 在复杂的键盘切换场景中自动分发重组工作到多帧
```

### 2.9 Lazy Layout 优化 — ★★★★☆ 重要

Emoji 网格和候选字列表滚动优化，Compose 1.8 自动应用更智能的预取策略。

### 2.10 HTML AnnotatedString — ★★★☆☆ 有用

协议和更新日志页面：

```kotlin
val htmlContent = buildAnnotatedString {
    appendHtml(agreementHtml)
}
Text(text = htmlContent)
```

---

## 3. IME + Compose 架构模式

### 3.1 InputMethodService 与 Compose 的桥接

```kotlin
class IMEService : InputMethodService() {
    private var composeView: ComposeView? = null

    override fun onCreateInputView(): View {
        return ComposeView(this).also { composeView = it }.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                KeyboardTheme(themeType = ThemeType.FollowSystem) {
                    InputScreen()
                }
            }
        }
    }
}
```

### 3.2 MVI 状态管理

> **注意**：以下为简化示例，完整定义见文档 000 第 3.3 节和文档 160 第 4 节。

```kotlin
// State: 不可变状态（完整定义见文档 160 第 8 节）
data class ImeState(
    val keyboardType: KeyboardType = KeyboardType.Pinyin,
    val keyboardState: KeyboardState = KeyboardState.Idle,
    val keyGrid: List<List<InputKey>> = emptyList(),
    val inputList: InputListState = InputListState(),
    val candidates: CandidateState = CandidateState(),
    val clipboard: ClipboardState = ClipboardState(),
    val favorites: FavoritesState = FavoritesState(),
    val config: ImeConfig = ImeConfig(),
)

// Intent: 用户意图（完整定义见文档 160 第 4 节）
sealed class ImeIntent {
    data class KeyPressed(val key: InputKey, val gesture: KeyGesture) : ImeIntent()
    data class CandidateSelected(val candidate: InputWord) : ImeIntent()
    data class SwitchKeyboard(val type: KeyboardType) : ImeIntent()
    data class ConfigChanged(val config: ImeConfig) : ImeIntent()
    data object CommitInput : ImeIntent()
    // ... 更多 Intent 见文档 160
}

// ViewModel
```
class KeyboardViewModel : ViewModel() {
    private val _state = MutableStateFlow(ImeState())
    val state: StateFlow<ImeState> = _state.asStateFlow()

    fun handleIntent(intent: ImeIntent) {
        _state.update { current ->
            reduce(current, intent)
        }
    }
}
```

### 3.3 Compose 中的键盘渲染

> **注意**：v4 采用三层分离设计（详见文档 150），键盘 UI 分离为 GestureInputPanel（手势拦截层）、GestureFeedbackPanel（反馈绘制层）和 KeyGridPanel（按键渲染层）。以下展示的是 KeyGridPanel 的纯渲染部分。

```kotlin
@Composable
fun StandardKeyGridPanel(
    keyGrid: List<List<InputKey>>,
    keyboardState: KeyboardState,
    onLayoutInfoChanged: (KeyGridPanelLayoutInfo) -> Unit,
    modifier: Modifier = Modifier,
) {
    val keyPositions = remember { mutableMapOf<InputKey, Rect>() }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        keyGrid.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                row.forEach { key ->
                    KeyView(
                        key = key,
                        isActive = isKeyActive(key, keyboardState),
                        modifier = Modifier
                            .weight(key.weight)
                            .onGloballyPositioned { coordinates ->
                                keyPositions[key] = coordinates.boundsInRoot()
                                onLayoutInfoChanged(
                                    KeyGridPanelLayoutInfo(
                                        keyPositions = keyPositions.toMap(),
                                        xPadLayoutInfo = null,
                                        candidateLayoutInfo = null,
                                    )
                                )
                            },
                    )
                }
            }
        }
    }
}
```

---

## 4. 性能优化规范

### 4.1 重组控制

```kotlin
// ✅ 推荐：使用 remember 和 derivedStateOf 减少重组
@Composable
fun CandidatePanel(candidates: List<Candidate>) {
    val displayCandidates = remember(candidates) {
        candidates.take(MAX_VISIBLE)
    }

    LazyRow {
        items(displayCandidates, key = { it.id }) { candidate ->
            CandidateItem(candidate)
        }
    }
}

// ❌ 避免：每次重组都做计算
@Composable
fun CandidatePanel(candidates: List<Candidate>) {
    LazyRow {
        items(candidates.take(MAX_VISIBLE)) { candidate -> // 每次重组都创建新列表
            CandidateItem(candidate)
        }
    }
}
```

### 4.2 键盘按键的稳定性

```kotlin
// ✅ 推荐：使用 key 标识稳定的列表项
LazyVerticalGrid(
    columns = GridCells.Fixed(columnCount),
) {
    items(items = keys, key = { it.stableId }) { key ->
        KeyView(key = key)
    }
}
```

### 4.3 避免不必要的重组

```kotlin
// ✅ 推荐：Lambda 不捕获可变状态
GestureInputPanel(
    onGesture = { gesture -> viewModel.handleGesture(gesture) },
)

// ❌ 避免：Lambda 捕获频繁变化的 state
GestureInputPanel(
    onGesture = { gesture ->
        val current = state.value // 每次调用都读取 state
        process(gesture, current)
    },
)
```

---

## 5. 主题系统

### 5.1 主题定义

> **注意**：以下为简化示例，完整主题系统设计见文档 500 第 4 节。v4 主题系统采用 `KeyboardColors` data class + `KeyboardThemes` 预置主题 + `KeyboardTheme` Composable 的三层结构，与 Material3 的 `MaterialTheme` 等系统命名区分。

```kotlin
data class KeyboardColors(
    val keyBackground: Color,
    val keyForeground: Color,
    val keyPressedBackground: Color,
    val keyActiveBackground: Color,
    val keyDisabledBackground: Color,
    val keyBorder: Color,
    val candidatePanelBackground: Color,
    val candidateChipBackground: Color,
    val candidateChipForeground: Color,
    val candidateChipActiveBackground: Color,
    val inputListPanelBackground: Color,
    val inputListPanelForeground: Color,
    val inputListPanelCursorColor: Color,
    val xPadBackground: Color,
    val xPadZoneBorder: Color,
    val xPadZoneForeground: Color,
    val xPadActiveZoneBackground: Color,
    val background: Color,
    val foreground: Color,
    val divider: Color,
)

object KeyboardThemes {
    val Light = KeyboardColors(
        keyBackground = Color(0xFFE8E8E8),
        keyForeground = Color(0xFF333333),
        // ...
    )

    val Night = KeyboardColors(
        keyBackground = Color(0xFF333333),
        keyForeground = Color(0xFFE8E8E8),
        // ...
    )
}

val LocalKeyboardColors = compositionLocalOf { KeyboardThemes.Light }
```

### 5.2 跟随系统主题

```kotlin
@Composable
fun KeyboardTheme(
    themeType: ThemeType = ThemeType.FollowSystem,
    content: @Composable () -> Unit,
) {
    val isDark = when (themeType) {
        ThemeType.Light -> false
        ThemeType.Night -> true
        ThemeType.FollowSystem -> isSystemInDarkTheme()
    }

    val colors = if (isDark) KeyboardThemes.Night else KeyboardThemes.Light

    CompositionLocalProvider(LocalKeyboardColors provides colors) {
        content()
    }
}
```

---

## 6. 手势处理

### 6.1 按键手势

```kotlin
// ✅ 推荐：使用 pointerInput 处理按键手势
Modifier.pointerInput(key) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        // 触发按键按下反馈
        onKeyPress()

        val up = waitForUpOrCancellation()
        if (up == null) {
            // 取消/移出
        } else {
            // 触发按键抬起
            onKeyRelease()
        }
    }
}
```

### 6.2 滑行输入手势

```kotlin
Modifier.pointerInput(Unit) {
    awaitEachGesture {
        val down = awaitFirstDown()
        val startKey = findKeyAt(down.position)

        val path = mutableListOf<Offset>(down.position)
        do {
            val event = awaitPointerEvent()
            path.addAll(event.changes.map { it.position })
        } while (event.changes.any { it.pressed })

        onSwipePath(startKey, path)
    }
}
```

---

## 7. ComposeView 内存管理

IME 中的 ComposeView 需要特别注意内存管理，因为 InputMethodService 的生命周期与普通 Activity 不同：

```kotlin
class IMEService : InputMethodService() {
    override fun onCreateInputView(): View {
        return ComposeView(this).apply {
            // ✅ 关键：设置正确的 Composition 策略
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
            )
            setContent {
                KeyboardTheme(themeType = ThemeType.FollowSystem) {
                    InputScreen()
                }
            }
        }
    }
}
```

> **注意**：如果 IME 在后台持有 ComposeView 引用，可能导致内存泄漏。确保在 `onDestroy()` 中清理引用。

---

## 8. 替代方案对照表

| Java 版本组件 | Compose 替代方案 |
|--------------|-----------------|
| `FlexboxLayout`（候选项） | `LazyRow` + `FlowRow` |
| `RecyclerView` + 自定义 `LayoutManager` | `LazyRow` / `LazyVerticalGrid` |
| `RecyclerView.Adapter` + `ViewHolder` | `items()` + `@Composable` 函数 |
| `ItemDecoration` | `Arrangement.spacedBy()` / `Modifier.padding()` |
| `ItemAnimator` | `animateItemPlacement()` / `animateBounds()` |
| `ViewPager` + `PagerAdapter` | `HorizontalPager` |
| `TabLayout` + `ViewPager` | `ScrollableTabRow` + `HorizontalPager` |
| `SharedPreferences` + `OnChangeListener` | `DataStore` + `Flow` |
| `AlertDialog` | `AlertDialog` (Compose) |
| `Toast` | `Snackbar` (Compose) |
| `WebView`（协议页） | `AnnotatedString.appendHtml()` |
| `Canvas`（X-Pad） | Compose `Canvas` / `drawBehind` |
| `GestureDetector` | `Modifier.pointerInput` + `detectTapGestures` 等 |
| `ValueAnimator` | `animate*AsState()` / `Animatable` |
| `OnClickListener` | `Modifier.clickable` |
| `OnLongClickListener` | `Modifier.combinedClickable` |
| `OnTouchListener` | `Modifier.pointerInput` |
| `View.setVisibility()` | 条件组合（`if (visible) { ... }`） |
| `View.setEnabled()` | `Modifier.enabled()` |
| `View.animate()` | `Modifier.animateContentSize()` / `animateBounds()` |
