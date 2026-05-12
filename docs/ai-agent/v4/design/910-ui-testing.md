# 910 — UI 测试方案设计

## 1. 概述

v4 版本设计应用内置的 UI 测试方案，用于在开发和测试阶段快速定位 UI 渲染、组件布局等问题。该方案的核心约束是：**发布版本构建时自动移除所有 UI 测试支持代码和依赖**，确保发布 APK 不包含任何调试专用代码、资源或依赖，从而避免包体积膨胀、性能损耗和信息泄露。Java 版本没有任何 UI 测试工具，v4 从零建设。

---

## 2. 需求分析

### 2.1 核心需求

| 需求 | 说明 |
|------|------|
| 布局边界可视化 | 显示 Compose 组件的边界线，快速定位布局溢出、重叠、间距异常 |
| 组件信息查看 | 点击任意组件查看其尺寸、位置、Modifier 链、重组次数等调试信息 |
| 颜色拾取 | 在键盘界面拾取任意像素颜色值，用于主题调试 |
| 栅格对齐参考线 | 显示栅格线和安全区域，验证键盘布局的对齐和间距一致性 |
| 重组追踪 | 标记频繁重组的组件，辅助性能优化 |
| Release 自动移除 | 所有 UI 测试代码、依赖和资源在 release 构建中完全不存在 |

### 2.2 使用场景

| 场景 | 使用的测试工具 | 说明 |
|------|---------------|------|
| 键盘按键布局不齐 | 布局边界 + 栅格参考线 | 可视化按键实际边界，对齐栅格 |
| 候选栏文字溢出 | 布局边界 + 组件信息 | 查看文字实际渲染宽度和容器宽度 |
| 主题颜色不正确 | 颜色拾取 | 精确读取屏幕像素颜色值 |
| 输入法窗口尺寸异常 | 组件信息 + 栅格参考线 | 查看 IME Window 实际尺寸和安全区域 |
| Compose 重组导致卡顿 | 重组追踪 | 找出频繁重组的组件并优化 |
| 键盘高度在不同设备不一致 | 组件信息 | 查看各组件实际测量尺寸 |

---

## 3. 构建配置：Release 自动移除

### 3.1 方案：Source Set 隔离

将所有 UI 测试代码放入独立的 `debug` 源集，release 构建不包含该源集，从而在编译阶段彻底移除：

```
code/app/src/
├── main/           ← 正式代码
│   ├── java/       ← 业务代码
│   └── res/        ← 正式资源
├── debug/          ← 仅 debug 构建包含
│   ├── java/       ← UI 测试工具代码
│   └── res/        ← UI 测试专用资源
└── release/        ← 仅 release 构建包含（可选，用于 release 专用配置）
```

**Gradle 配置**：

```groovy
android {
    buildTypes {
        debug {
            // UI 测试依赖仅添加到 debug 构建
        }
        release {
            // 不添加 UI 测试依赖
        }
    }
}

dependencies {
    // UI 测试工具（仅 debug）
    debugImplementation "androidx.compose.ui:ui-tooling:{compose_bom_version}"
    debugImplementation "androidx.compose.ui:ui-test-manifest:{compose_bom_version}"
}
```

### 3.2 运行时入口控制

通过 `main` 源集中的接口定义 UI 测试能力，`debug` 源集中的实现类提供具体功能：

```kotlin
// main 源集：接口定义（空实现，不引入任何依赖）
interface UITestOverlay {
    fun enable()
    fun disable()
    fun toggle(tool: UITestTool)
    fun isActive(): Boolean

    companion object {
        /** 获取 UI 测试覆盖层实例。debug 构建返回真实实现，release 构建返回空实现 */
        fun create(): UITestOverlay = UITestOverlayImpl()
    }
}

// main 源集：工具枚举
enum class UITestTool {
    LayoutBounds,     // 布局边界可视化
    ComponentInfo,    // 组件信息查看
    ColorPicker,      // 颜色拾取
    GridGuides,       // 栅格对齐参考线
    Recomposition,    // 重组追踪
}

// main 源集：空实现（作为 fallback）
private class NoopUITestOverlay : UITestOverlay {
    override fun enable() {}
    override fun disable() {}
    override fun toggle(tool: UITestTool) {}
    override fun isActive() = false
}
```

```kotlin
// debug 源集：真实实现
class DebugUITestOverlay : UITestOverlay {
    private val activeTools = mutableSetOf<UITestTool>()

    override fun enable() {
        // 激活 UI 测试覆盖层
    }

    override fun disable() {
        activeTools.clear()
    }

    override fun toggle(tool: UITestTool) {
        if (tool in activeTools) activeTools.remove(tool) else activeTools.add(tool)
    }

    override fun isActive() = activeTools.isNotEmpty()
}

// 通过反射或编译期常量提供真实实现
internal fun UITestOverlay.Companion.createImpl(): UITestOverlay = DebugUITestOverlay()
```

### 3.3 编译期完全移除验证

通过 ProGuard/R8 规则确保 release 构建中不残留任何 UI 测试类：

```proguard
# release 构建移除 UI 测试相关类
-assumenosideeffects class androidx.compose.ui.tooling.** { *; }
```

同时，在 CI 流水线中增加验证步骤，确保 release APK 不包含 UI 测试代码：

```bash
# 检查 release APK 中是否包含 UI 测试类
if aapt dump classes release.apk | grep -i "uitest\|debugoverlay"; then
    echo "ERROR: Release APK contains UI test classes!"
    exit 1
fi
```

---

## 4. UI 测试工具设计

### 4.1 布局边界可视化

在 Compose 组件周围绘制边界线和间距标注，类似 Android View 系统的「显示布局边界」开发者选项，但更精细——支持按组件类型选择显示范围，并标注具体尺寸数值。

```kotlin
// debug 源集
@Composable
fun LayoutBoundsOverlay(
    content: @Composable () -> Unit,
) {
    Box {
        content()

        if (UITestState.current.isToolActive(UITestTool.LayoutBounds)) {
            // 通过 Modifier.layout 附加测量信息
            LayoutBoundsCanvas(
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun LayoutBoundsCanvas(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        // 从 Compose 的 LayoutInfo 树收集所有组件边界
        val rootInfo = (view as? View)?.let { findRootLayoutInfo(it) }
        rootInfo?.let { drawBounds(it) }
    }
}

private fun DrawScope.drawBounds(info: LayoutInfo) {
    // 绘制组件边界矩形
    drawRect(
        color = Color.Red.copy(alpha = 0.5f),
        topLeft = Offset(info.offsetX.toFloat(), info.offsetY.toFloat()),
        size = Size(info.width.toFloat(), info.height.toFloat()),
        style = Stroke(width = 1.dp.toPx()),
    )

    // 绘制尺寸标注
    drawContext.canvas.nativeCanvas.drawText(
        "${info.width.toInt()}x${info.height.toInt()}",
        info.offsetX.toFloat(),
        info.offsetY.toFloat() - 4.dp.toPx(),
        textPaint,
    )

    // 递归绘制子组件
    info.children.forEach { drawBounds(it) }
}
```

**Compose LayoutInfo 方案**：利用 Compose 的 `LayoutInfo` 树获取所有组件的测量信息。通过 `View.getRootView()` 拿到根 View，遍历其 ComposeView 子节点，从 `LayoutInfo` 获取组件树结构。相比自定义 Modifier 侵入方案，这种方式不需要修改任何业务 Composable。

### 4.2 组件信息查看

点击任意 UI 组件，显示该组件的详细信息面板：

```kotlin
// debug 源集
@Composable
fun ComponentInfoOverlay(
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit,
) {
    var selectedInfo by remember { mutableStateOf<ComponentDebugInfo?>(null) }

    Box(modifier = modifier.pointerInput(Unit) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent()
                if (event.changes.any { it.pressed }) {
                    val position = event.changes.first().position
                    selectedInfo = findComponentAt(position)
                }
            }
        }
    }) {
        // 信息面板
        selectedInfo?.let { info ->
            ComponentInfoPanel(
                info = info,
                onDismiss = { selectedInfo = null },
            )
        }
    }
}

data class ComponentDebugInfo(
    val name: String,                    // Composable 函数名
    val size: IntSize,                   // 实际尺寸
    val position: Offset,               // 在父容器中的位置
    val modifiers: List<String>,         // Modifier 链描述
    val recompositionCount: Int,         // 重组次数
    val parentInfo: ComponentDebugInfo?, // 父组件信息
)
```

**信息面板展示**：

```
┌──────────────────────────────────┐
│  CandidateBar                    │
│  ─────────────────────────────   │
│  Size:    1080 x 48 dp           │
│  Position: (0, 1200)             │
│  Modifiers:                      │
│    fillMaxWidth()                 │
│    height(48.dp)                  │
│    background(CandidateBarBg)     │
│    padding(horizontal=8.dp)       │
│  Recompositions: 12              │
│  ─────────────────────────────   │
│  [Copy] [Close]                  │
└──────────────────────────────────┘
```

### 4.3 颜色拾取

在键盘界面上拾取任意像素的颜色值：

```kotlin
// debug 源集
@Composable
fun ColorPickerOverlay(
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit,
) {
    var pickedColor by remember { mutableStateOf<Color?>(null) }
    var cursorPosition by remember { mutableStateOf(Offset.Zero) }

    Box(modifier = modifier.pointerInput(Unit) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent()
                val position = event.changes.firstOrNull()?.position ?: continue
                cursorPosition = position

                if (event.changes.any { it.pressed }) {
                    // 从 Bitmap 获取像素颜色
                    pickedColor = capturePixelColorAt(position)
                }
            }
        }
    }) {
        // 放大镜 + 十字准心
        ColorPickerCursor(position = cursorPosition)

        // 颜色信息弹窗
        pickedColor?.let { color ->
            ColorInfoPopup(
                color = color,
                onDismiss = { pickedColor = null },
            )
        }
    }
}

@Composable
private fun ColorInfoPopup(color: Color, onDismiss: () -> Unit) {
    Card(
        modifier = Modifier.padding(16.dp),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(color)
                        .border(1.dp, Color.Black),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "HEX: ${colorToHex(color)}",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                    )
                    Text(
                        text = "RGB: ${colorToRgb(color)}",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                    )
                }
            }
            Row(modifier = Modifier.padding(top = 8.dp)) {
                TextButton(onClick = { copyToClipboard(colorToHex(color)) }) {
                    Text("复制 HEX")
                }
                TextButton(onClick = onDismiss) {
                    Text("关闭")
                }
            }
        }
    }
}
```

### 4.4 栅格对齐参考线

显示栅格线、间距参考和安全区域，验证键盘布局的对齐精度：

```kotlin
// debug 源集
@Composable
fun GridGuidesOverlay(
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val gridSpacing = 8.dp.toPx() // 8dp 栅格间距

        // 绘制垂直栅格线
        var x = 0f
        while (x < size.width) {
            drawLine(
                color = Color.Cyan.copy(alpha = 0.3f),
                start = Offset(x, 0f),
                end = Offset(x, size.height),
                strokeWidth = 0.5.dp.toPx(),
            )
            x += gridSpacing
        }

        // 绘制水平栅格线
        var y = 0f
        while (y < size.height) {
            drawLine(
                color = Color.Cyan.copy(alpha = 0.3f),
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 0.5.dp.toPx(),
            )
            y += gridSpacing
        }

        // 绘制安全区域
        val safeArea = getSafeAreaInsets()
        drawRect(
            color = Color.Red.copy(alpha = 0.1f),
            topLeft = Offset(0f, 0f),
            size = Size(size.width, safeArea.top.toFloat()),
        )
        drawRect(
            color = Color.Red.copy(alpha = 0.1f),
            topLeft = Offset(0f, size.height - safeArea.bottom.toFloat()),
            size = Size(size.width, safeArea.bottom.toFloat()),
        )
    }
}
```

### 4.5 重组追踪

利用 Compose Compiler 的重组追踪能力，标记频繁重组的组件：

```kotlin
// debug 源集
/**
 * 重组追踪覆盖层。
 *
 * 利用 Compose 的 LayoutInfo 树中 ReusableComposeNode 的重组计数，
 * 在每个组件上叠加颜色标记：
 * - 绿色：0-2 次重组（正常）
 * - 黄色：3-5 次重组（需关注）
 * - 红色：6+ 次重组（需优化）
 */
@Composable
fun RecompositionOverlay(
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val recompositionData = collectRecompositionData()

        recompositionData.forEach { (bounds, count) ->
            val color = when {
                count <= 2 -> Color.Green.copy(alpha = 0.2f)
                count <= 5 -> Color.Yellow.copy(alpha = 0.3f)
                else -> Color.Red.copy(alpha = 0.4f)
            }
            drawRect(
                color = color,
                topLeft = Offset(bounds.left.toFloat(), bounds.top.toFloat()),
                size = Size(bounds.width().toFloat(), bounds.height().toFloat()),
            )
        }
    }
}
```

Compose 自 1.3.0 起提供 `ComposeCompilerReport`，可在编译期生成重组报告。运行时可通过 `Modifier.recomposeHighlighter()` 实验性 API 追踪重组：

```kotlin
// debug 源集：自定义重组追踪 Modifier
private fun Modifier.recomposeTracker(): Modifier = this.then(
    Modifier.layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        layout(placeable.width, placeable.height) {
            placeable.placeRelative(0, 0)
            // 增加重组计数
            RecompositionTracker.record(this@layout)
        }
    }
)

object RecompositionTracker {
    private val counts = mutableMapOf<String, Int>()

    fun record(composable: Any) {
        val key = composable.javaClass.simpleName
        counts[key] = (counts[key] ?: 0) + 1
    }

    fun getSnapshot(): Map<String, Int> = counts.toMap()

    fun reset() = counts.clear()
}
```

---

## 5. UI 测试工具栏

### 5.1 浮动工具栏

所有 UI 测试工具通过一个可拖动的浮动工具栏切换：

```kotlin
// debug 源集
@Composable
fun UITestToolbar(
    overlay: UITestOverlay,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    var toolbarOffset by remember { mutableStateOf(Offset(16f, 100f)) }

    Box(
        modifier = modifier
            .offset { IntOffset(toolbarOffset.x.toInt(), toolbarOffset.y.toInt()) }
            .draggable(
                orientation = Orientation.Vertical,
                state = rememberDraggableState { delta ->
                    toolbarOffset = toolbarOffset.copy(y = (toolbarOffset.y + delta).coerceIn(0f, 800f))
                },
            ),
    ) {
        if (expanded) {
            // 工具面板
            Card(
                modifier = Modifier.padding(bottom = 48.dp),
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    UITestTool.entries.forEach { tool ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { overlay.toggle(tool) }
                                .padding(vertical = 8.dp, horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(
                                checked = overlay.isToolActive(tool),
                                onCheckedChange = { overlay.toggle(tool) },
                            )
                            Text(tool.displayName, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }

        // 触发按钮
        FloatingActionButton(
            onClick = { expanded = !expanded },
            modifier = Modifier.size(40.dp),
        ) {
            Icon(
                imageVector = Icons.Default.BugReport,
                contentDescription = "UI 测试工具",
            )
        }
    }
}

val UITestTool.displayName: String
    get() = when (this) {
        UITestTool.LayoutBounds -> "布局边界"
        UITestTool.ComponentInfo -> "组件信息"
        UITestTool.ColorPicker -> "颜色拾取"
        UITestTool.GridGuides -> "栅格参考线"
        UITestTool.Recomposition -> "重组追踪"
    }
```

### 5.2 集成入口

在 `ImeService` 的 ComposeView 层次中，debug 构建额外包裹 UI 测试覆盖层：

```kotlin
// main 源集
@Composable
fun ImeRoot(state: ImeState, intentHandler: (ImeIntent) -> Unit) {
    ImeTheme(themeType = state.config.themeType) {
        ImeMainScreen(state, intentHandler)
    }
}

// debug 源集
@Composable
fun ImeRoot(state: ImeState, intentHandler: (ImeIntent) -> Unit) {
    ImeTheme(themeType = state.config.themeType) {
        Box {
            ImeMainScreen(state, intentHandler)

            // UI 测试覆盖层（仅 debug 构建存在）
            val overlay = remember { UITestOverlay.create() }
            if (overlay.isActive()) {
                UITestOverlays(overlay)
            }
            UITestToolbar(overlay)
        }
    }
}
```

通过同一函数签名 `ImeRoot` 在不同源集中的不同实现，debug 构建自动包含 UI 测试覆盖层，release 构建不含任何覆盖层代码。这种方式无需在 `main` 源集中使用 `if (BuildConfig.DEBUG)` 判断，确保 release 代码路径完全干净。

---

## 6. Compose 编译器报告集成

### 6.1 编译期重组报告

Compose Compiler 支持在编译时生成重组分析报告，帮助开发者识别不稳定的参数和不必要的重组。在 debug 构建中启用：

```groovy
// code/app/build.gradle
android {
    buildTypes {
        debug {
            // 启用 Compose 编译器报告
            composeCompiler {
                reportsDestination = layout.buildDirectory.dir("compose_compiler_reports")
                metricsDestination = layout.buildDirectory.dir("compose_compiler_metrics")
            }
        }
    }
}
```

### 6.2 报告分析

编译完成后，通过脚本解析报告中的问题项：

| 报告项 | 含义 | 修复建议 |
|--------|------|----------|
| `restartable` 但非 `skippable` | Composable 每次都重组 | 确保参数类型稳定 |
| 不稳定参数 | 参数类型非 Stable/Immutable | 使用 `@Immutable` 或 `@Stable` 注解 |
| 高重组频率 | 在短时间内重组次数过多 | 提升状态粒度，使用 `derivedStateOf`/`remember` |

---

## 7. 截图对比测试

### 7.1 设计

截图对比测试（Screenshot Testing）用于验证 UI 在代码变更后不会出现意外视觉变化。使用 Paparazzi 或 Roborazzi 框架，在 JVM/设备上渲染 Compose 组件并截图，与基准截图进行像素级对比。

**框架选型**：

| 框架 | 优势 | 劣势 |
|------|------|------|
| **Paparazzi** | JVM 上渲染（无需设备），速度快 | 依赖 Android SDK layoutlib，部分 Compose 效果渲染不精确 |
| **Roborazzi** | 在 Robolectric 环境渲染，支持 Compose | 需要 Robolectric，环境配置复杂 |

推荐 **Paparazzi**：速度更快，且对 Compose 支持日趋完善。

### 7.2 测试组织

```
code/app/src/test/
└── screenshot/
    └── org/crazydan/studio/app/ime/kuaizi/ui/
        ├── keyboard/
        │   ├── PinyinKeyboardScreenshotTest.kt
        │   ├── LatinKeyboardScreenshotTest.kt
        │   └── NumberKeyboardScreenshotTest.kt
        ├── candidate/
        │   └── CandidateBarScreenshotTest.kt
        ├── input/
        │   └── InputBarScreenshotTest.kt
        └── theme/
            ├── LightThemeScreenshotTest.kt
            └── NightThemeScreenshotTest.kt
```

### 7.3 示例测试

```kotlin
class PinyinKeyboardScreenshotTest {
    @get:Rule
    val paparazzi = Paparazzi()

    @Test
    fun pinyinKeyboardIdle() {
        paparazzi.snapshot {
            ImeTheme(themeType = ThemeType.Light) {
                KeyboardScreen(
                    state = ImeState(
                        keyboardType = KeyboardType.Pinyin,
                        keyboardState = KeyboardState.Idle,
                    ),
                    intentHandler = {},
                )
            }
        }
    }

    @Test
    fun pinyinKeyboardWithCandidates() {
        paparazzi.snapshot {
            ImeTheme(themeType = ThemeType.Light) {
                KeyboardScreen(
                    state = ImeState(
                        keyboardType = KeyboardType.Pinyin,
                        keyboardState = KeyboardState.InputWaiting(
                            inputChars = listOf(charInput('n'), charInput('i')),
                            candidates = testCandidates,
                        ),
                    ),
                    intentHandler = {},
                )
            }
        }
    }
}
```

### 7.4 CI 集成

截图对比测试在 CI 流水线中作为独立阶段执行，检测到差异时上传对比图作为 Artifacts：

```bash
# 运行截图测试
./gradlew verifyPaparazziDebug

# 如果失败，生成差异报告
./gradlew recordPaparazziDebug  # 更新基准截图
```

---

## 8. 构建保证机制

### 8.1 依赖隔离

| 依赖 | 作用 | 构建类型 |
|------|------|----------|
| `androidx.compose.ui:ui-tooling` | Layout Inspector、Compose 信息 | `debugImplementation` |
| `app.cash.paparazzi:paparazzi` | 截图对比测试 | `testImplementation` |
| UI 测试工具代码 | 布局边界、组件信息、颜色拾取等 | `debug` 源集 |

### 8.2 代码隔离检查清单

| 检查项 | 方法 |
|--------|------|
| release APK 无 UI 测试类 | CI 脚本检查 APK class 列表 |
| release APK 无 UI 测试资源 | CI 脚本检查 APK 资源列表 |
| release APK 无 ui-tooling 依赖 | 分析 release 构建依赖树 |
| main 源集无 UI 测试引用 | 代码审查 + Lint 规则 |
| release R8 移除所有调试代码 | ProGuard 规则 + APK 反编译验证 |

### 8.3 Lint 规则

自定义 Lint 规则，防止在 `main` 源集中意外引用 `debug` 源集的类：

```kotlin
// 自定义 Lint 规则（放在 tools/lint 模块）
class UITestReferenceDetector : Detector(), Detector.UastScanner {
    override fun getApplicableUastTypes() = listOf(UCallExpression::class.java)

    override fun createUastHandler(context: JavaContext) =
        object : UElementHandler() {
            override fun visitCallExpression(node: UCallExpression) {
                val className = node.classReference?.qualifiedName ?: return
                if (className.startsWith("org.crazydan.ime.app.uitest.")) {
                    // 检查是否在 main 源集中
                    val sourceSet = context.file.path.substringAfter("/src/").substringBefore("/")
                    if (sourceSet == "main") {
                        context.report(
                            issue = ISSUE_UI_TEST_IN_MAIN,
                            location = context.getLocation(node),
                            message = "UI 测试代码不应在 main 源集中引用",
                        )
                    }
                }
            }
        }

    companion object {
        val ISSUE_UI_TEST_IN_MAIN = Issue.create(
            id = "UITestReferenceInMain",
            briefDescription = "UI 测试引用不应出现在 main 源集",
            explanation = "main 源集中的代码会包含在 release 构建中，引用 UI 测试类会导致 release 构建包含调试代码",
            category = Category.CORRECTNESS,
            severity = Severity.ERROR,
            implementation = Implementation(
                UITestReferenceDetector::class.java,
                Scope.JAVA_FILE_SCOPE,
            ),
        )
    }
}
```

---

## 9. 与日志系统的协作

UI 测试方案与应用日志系统（文档 900）协同工作：

| 协作点 | 说明 |
|--------|------|
| 组件信息 → 日志记录 | 「组件信息查看」工具可将选中组件的调试信息以 INFO 等级写入日志 |
| 重组追踪 → 日志记录 | 重组超过阈值的组件自动记录 WARN 日志，便于后续排查 |
| 布局异常 → 日志警告 | 检测到布局溢出（组件尺寸超出父容器）时自动记录 WARN 日志 |
| 日志等级联动 | UI 测试工具激活时，自动将日志等级降至 DEBUG 以获取更完整信息 |

```kotlin
// debug 源集：UI 测试与日志联动
class DebugUITestOverlay(
    private val appLog: AppLog,
) : UITestOverlay {

    override fun enable() {
        // UI 测试激活时降级日志等级
        if (appLog.level > LogLevel.DEBUG) {
            appLog.updateLevel(LogLevel.DEBUG)
            appLog.logger("UITest").info { "UI 测试工具已激活，日志等级已降至 DEBUG" }
        }
    }

    override fun toggle(tool: UITestTool) {
        if (tool in activeTools) {
            activeTools.remove(tool)
        } else {
            activeTools.add(tool)
            appLog.logger("UITest").debug { "激活工具: ${tool.displayName}" }
        }
    }
}
```

---

## 10. 与 Java 版本对照

| Java 版本 | v4 版本 | 改进说明 |
|-----------|---------|---------|
| 无 UI 测试工具 | 5 种内置 UI 测试工具 | 全新建设 |
| 无布局可视化 | 布局边界 + 栅格参考线 | Compose LayoutInfo 树驱动 |
| 无组件调试 | 组件信息查看 | 点击查看尺寸、Modifier、重组次数 |
| 无颜色调试 | 颜色拾取 | 放大镜 + HEX/RGB 值显示 |
| 无重组追踪 | 重组追踪 + 编译期报告 | Compose Compiler 原生支持 |
| 无截图测试 | Paparazzi 截图对比 | CI 自动化视觉回归检测 |
| 无 Release 移除机制 | Source Set 隔离 + Lint 规则 + CI 检查 | 多重保障确保发布包干净 |
| 无构建隔离 | debug/release 源集分离 | 编译期移除，非运行时判断 |
