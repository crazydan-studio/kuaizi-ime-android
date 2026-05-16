# Jetpack Compose 最佳实践

本文档基于 Jetpack Compose BOM 2026.04.01（Compose 1.8），说明 Compose 的最佳实践、推荐特性、避坑指南和性能优化建议。

> **说明**：本文档为通用 Compose 最佳实践，所有示例使用领域无关的类名和场景。项目特定的 IME 架构模式（MVI 状态管理、IMEService 桥接、键盘渲染等）属于架构设计范畴，不在技能文档中描述。

---

## 1. Compose 适用性评估

### ✅ 高度适用的场景

| 场景 | Compose 方案 | 优势 |
|------|-------------|------|
| **设置页面** | 标准 Compose 组件 | 声明式 UI、主题统一、代码简洁 |
| **列表 / 网格** | `LazyRow`/`LazyColumn`/`LazyVerticalGrid` | 替代自定义 RecyclerView |
| **标签 / 分类** | `ScrollableTabRow` + `HorizontalPager` | 标准化分页 |
| **主题切换** | `isSystemInDarkTheme()` | 自动跟随系统主题 |
| **按键反馈** | 扩展的触觉反馈 API | 丰富的按键触觉反馈类型 |

### ⚠️ 需谨慎评估的场景

| 场景 | 风险 | 建议 |
|------|------|------|
| **高频重绘区域** | 重组开销需控制 | 使用 `remember`、`derivedStateOf`、`key` 精确控制重组范围 |
| **自定义 Canvas 绘制** | 与 Compose Canvas 的桥接 | 使用 Compose `Canvas` + `drawBehind`/`drawWithContent` |
| **自定义手势检测** | 需要极低延迟 | `pointerInput` + `awaitPointerEventScope`，注意帧率 |
| **Service 中的 ComposeView** | 生命周期与普通 Activity 不同 | 注意内存管理和 Composition 策略 |

### ❌ 不适用 Compose 的场景

| 场景 | 原因 | 替代方案 |
|------|------|----------|
| **极端性能要求的自定义绘制** | Compose 重组和布局有固有的帧预算开销 | 降级为传统 `View` + `Canvas` |
| **需要像素级控制的自定义布局** | Compose 布局系统有固有约束 | 自定义 `Layout` 或降级为传统 `View` |

---

## 2. Compose 1.8 新特性应用

### 2.1 文本自动缩放（TextAutoSize）— ★★★★★ 关键

在需要适应不同宽度和屏幕尺寸的文本场景中使用：

```kotlin
BasicText(
    text = item.label,
    maxLines = 1,
    autoSize = TextAutoSize.StepBased(
        minFontSize = 10.sp,
        maxFontSize = 18.sp,
        stepSize = 1.sp,
    ),
)

// 带中间省略
BasicText(
    text = item.description,
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

```kotlin
// 中间省略，保留首尾
Text(
    text = longText,
    overflow = TextOverflow.MiddleEllipsis,
    maxLines = 1,
)

// 开头省略，保留尾部
Text(
    text = filePath,
    overflow = TextOverflow.StartEllipsis,
    maxLines = 1,
)
```

### 2.3 扩展触觉反馈 — ★★★★★ 关键

```kotlin
val haptics = LocalHapticFeedback.current

// 点击反馈
haptics.performHapticFeedback(HapticFeedbackType.ContextClick)

// 长按触发
haptics.performHapticFeedback(HapticFeedbackType.LongPress)

// 确认操作
haptics.performHapticFeedback(HapticFeedbackType.Confirm)

// 手势结束
haptics.performHapticFeedback(HapticFeedbackType.GestureEnd)
```

### 2.4 预测性动画（Predictive Animations）— ★★★★☆ 重要

尺寸变化动画（如展开 / 折叠）：

```kotlin
LookaheadScope {
    Box(
        Modifier
            .width(if (expanded) 180.dp else 110.dp)
            .animateBounds(lookaheadScope = this@LookaheadScope)
    ) {
        // 内容
    }
}
```

### 2.5 稳定的多焦点 API — ★★★★★ 关键

在需要管理多个焦点区域的界面中使用：

```kotlin
Column(Modifier.focusRestorer()) {
    SectionA(Modifier.onFocusChanged { /* ... */ })
    SectionB(Modifier.onFocusChanged { /* ... */ })
    Toolbar(Modifier.onFocusChanged { /* ... */ })
}
```

### 2.6 可见性追踪 — ★★★★☆ 重要

优化分组列表的懒加载：

```kotlin
LazyColumn {
    items(groups) { group ->
        GroupView(
            group = group,
            modifier = Modifier.onLayRectChanged { rect ->
                group.isVisible = rect.isVisible
            },
        )
    }
}
```

### 2.7 Autofill 语义 — ★★★★☆ 重要

```kotlin
TextField(
    state = rememberTextFieldState(),
    modifier = Modifier.semantics { contentType = ContentType.Username },
    label = { Text("用户名") },
)
```

### 2.8 可暂停组合 — ★★★★☆ 重要

Compose 1.8 自动启用，在复杂布局切换场景中自动分发重组工作到多帧，防止卡顿。

### 2.9 Lazy Layout 优化 — ★★★★☆ 重要

Compose 1.8 自动应用更智能的预取策略，优化大列表和网格的滚动性能。

### 2.10 HTML AnnotatedString — ★★★☆☆ 有用

```kotlin
val htmlContent = buildAnnotatedString {
    appendHtml(htmlSource)
}
Text(text = htmlContent)
```

---

## 3. 性能优化规范

### 3.1 重组控制

```kotlin
// ✅ 推荐：使用 remember 和 derivedStateOf 减少重组
@Composable
fun ItemList(items: List<Item>) {
    val visibleItems = remember(items) {
        items.take(MAX_VISIBLE)
    }

    LazyRow {
        items(visibleItems, key = { it.id }) { item ->
            ItemView(item)
        }
    }
}

// ❌ 避免：每次重组都做计算
@Composable
fun ItemList(items: List<Item>) {
    LazyRow {
        items(items.take(MAX_VISIBLE)) { item -> // 每次重组都创建新列表
            ItemView(item)
        }
    }
}
```

### 3.2 列表项的稳定性

```kotlin
// ✅ 推荐：使用 key 标识稳定的列表项
LazyVerticalGrid(
    columns = GridCells.Fixed(columnCount),
) {
    items(items = items, key = { it.stableId }) { item ->
        ItemView(item)
    }
}
```

### 3.3 避免不必要的重组

```kotlin
// ✅ 推荐：Lambda 不捕获频繁变化的 state
GesturePanel(
    onGesture = { gesture -> viewModel.handleGesture(gesture) },
)

// ❌ 避免：Lambda 捕获频繁变化的 state
GesturePanel(
    onGesture = { gesture ->
        val current = state.value // 每次调用都读取 state
        process(gesture, current)
    },
)
```

---

## 4. 手势处理

### 4.1 点击 / 长按手势

```kotlin
// ✅ 推荐：使用 pointerInput 处理自定义手势
Modifier.pointerInput(key) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        // 按下反馈
        onPressed()

        val up = waitForUpOrCancellation()
        if (up == null) {
            // 取消/移出
            onCancelled()
        } else {
            // 抬起
            onReleased()
        }
    }
}
```

### 4.2 滑行 / 拖拽手势

```kotlin
Modifier.pointerInput(Unit) {
    awaitEachGesture {
        val down = awaitFirstDown()
        val startPosition = down.position

        val path = mutableListOf(startPosition)
        do {
            val event = awaitPointerEvent()
            path.addAll(event.changes.map { it.position })
        } while (event.changes.any { it.pressed })

        onSwipePath(startPosition, path)
    }
}
```

---

## 5. ComposeView 内存管理

在 Service 等非标准生命周期组件中使用 ComposeView 需要特别注意内存管理：

```kotlin
class MyService : Service() {
    private var composeView: ComposeView? = null

    override fun onCreate(): View {
        return ComposeView(this).also { composeView = it }.apply {
            // ✅ 关键：设置正确的 Composition 策略
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
            )
            setContent {
                AppTheme {
                    Content()
                }
            }
        }
    }

    override fun onDestroy() {
        // ✅ 关键：清理 ComposeView 引用，避免内存泄漏
        composeView = null
        super.onDestroy()
    }
}
```

> **注意**：如果 Service 在后台持有 ComposeView 引用，可能导致内存泄漏。确保在 `onDestroy()` 中清理引用。
