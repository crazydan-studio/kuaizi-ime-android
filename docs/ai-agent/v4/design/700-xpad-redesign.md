# 700 — X-Pad 重构设计

## 1. 概述

X-Pad 是筷字输入法的特色输入模式，采用 8pen/8VIM 风格的连续环形输入，通过六边形网格区域和路径选择实现拼音和拉丁字符的快速输入。Java 版本使用 Mixite 库处理六边形网格，配合自定义 Canvas 绘制和手势检测。v4 版本保留核心交互逻辑，简化绘制和手势实现。

---

## 2. Java 版本 X-Pad 分析

### 2.1 核心组件

| 组件 | 说明 |
|------|------|
| `XPadKey` | X-Pad 按键，包含区域定义 |
| `XPadView` | 自定义 View，处理六边形网格绘制和手势 |
| `XPainter` 系列 | 绘制接口：XTextPainter、XDrawablePainter、XPathPainter、XAlignPainter |
| `XZone` | X-Pad 区域定义 |
| `XPadState` | X-Pad 状态 |
| `PinyinKeyTable` | 六边形键表生成（使用 Mixite） |
| `HexagonDrawable` | 六边形形状 Drawable |

### 2.2 工作原理

1. 用户在中心区域按下
2. 滑动到某个六边形区域选择首字母
3. 继续滑动到下一个区域选择后续字母
4. 松开手指完成输入

### 2.3 问题分析

1. **Mixite 库依赖**：仅用于六边形网格计算，功能过重
2. **自定义绘制代码量大**：XPainter 体系有 5 个接口/类，职责分散
3. **手势检测与绘制耦合**：XPadView 同时处理触摸事件和绘制逻辑
4. **状态管理分散**：XPadState 与 KeyboardState 的交互不清晰

---

## 3. v4 X-Pad 设计

### 3.1 六边形网格计算（替代 Mixite）

自行实现六边形网格计算，移除 Mixite 依赖：

```kotlin
/**
 * 六边形网格坐标系统。
 * 使用轴向坐标（axial coordinates）表示六边形位置。
 */
data class HexGrid(
    val hexSize: Float,
    val centerX: Float,
    val centerY: Float,
) {
    /** 轴向坐标 */
    data class Axial(val q: Int, val r: Int)

    /** 将轴向坐标转换为像素坐标 */
    fun axialToPixel(axial: Axial): Offset {
        val x = hexSize * (sqrt(3f) * axial.q + sqrt(3f) / 2f * axial.r) + centerX
        val y = hexSize * (3f / 2f * axial.r) + centerY
        return Offset(x, y)
    }

    /** 将像素坐标转换为轴向坐标 */
    fun pixelToAxial(pixel: Offset): Axial {
        val x = pixel.x - centerX
        val y = pixel.y - centerY
        val q = (sqrt(3f) / 3f * x - 1f / 3f * y) / hexSize
        val r = (2f / 3f * y) / hexSize
        return axialRound(q, r)
    }

    /** 六边形四舍五入 */
    private fun axialRound(q: Float, r: Float): Axial {
        val s = -q - r
        var rq = q.roundToInt()
        var rr = r.roundToInt()
        val rs = s.roundToInt()
        val dq = abs(rq - q)
        val dr = abs(rr - r)
        val ds = abs(rs - s)
        if (dq > dr && dq > ds) rq = -rr - rs
        else if (dr > ds) rr = -rq - rs
        return Axial(rq, rr)
    }

    /** 计算六边形的顶点坐标 */
    fun hexVertices(center: Offset, size: Float = hexSize): List<Offset> {
        return (0..5).map { i ->
            val angle = PI / 3f * i - PI / 6f
            Offset(
                center.x + size * cos(angle).toFloat(),
                center.y + size * sin(angle).toFloat(),
            )
        }
    }
}
```

### 3.2 X-Pad 区域

```kotlin
data class XPadZone(
    val axial: HexGrid.Axial,
    val label: String,
    val chars: List<Char>,
    val color: Color? = null,
)

data class XPadLayout(
    val zones: List<XPadZone>,
    val centerLabel: String = "",
) {
    companion object {
        /** 从拼音键表生成 X-Pad 布局 */
        fun fromPinyinKeys(keys: List<InputKey.Char>): XPadLayout {
            val zones = keys.mapIndexed { index, key ->
                val axial = indexToAxial(index)
                XPadZone(
                    axial = axial,
                    label = key.label,
                    chars = key.levels.flatMap { it.toList() },
                )
            }
            return XPadLayout(zones = zones, centerLabel = "Ⓧ")
        }

        /** 将索引转换为轴向坐标（环形布局） */
        private fun indexToAxial(index: Int): HexGrid.Axial {
            // 第一圈 6 个六边形
            val directions = listOf(
                HexGrid.Axial(1, 0), HexGrid.Axial(0, 1), HexGrid.Axial(-1, 1),
                HexGrid.Axial(-1, 0), HexGrid.Axial(0, -1), HexGrid.Axial(1, -1),
            )
            return directions[index % 6]
        }
    }
}
```

### 3.3 X-Pad 视图

```kotlin
@Composable
fun XPadView(
    layout: XPadLayout,
    grid: HexGrid,
    currentPath: List<XPadZone>,
    currentSpell: String,
    onZoneSelected: (XPadZone) -> Unit,
    onPathComplete: (List<XPadZone>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current

    Box(modifier = modifier) {
        // 六边形网格绘制
        Canvas(modifier = Modifier.fillMaxSize()) {
            layout.zones.forEach { zone ->
                val center = grid.axialToPixel(zone.axial)
                val vertices = grid.hexVertices(center)

                // 绘制六边形
                val hexPath = Path().apply {
                    moveTo(vertices[0])
                    for (i in 1..5) lineTo(vertices[i])
                    close()
                }

                val isActive = zone in currentPath
                drawPath(
                    path = hexPath,
                    color = if (isActive) activeZoneColor else zoneColor,
                    style = Stroke(width = 2.dp.toPx()),
                )

                // 绘制区域标签
                drawText(
                    textMeasurer = rememberTextMeasurer(),
                    text = zone.label,
                    topLeft = Offset(center.x - 10.dp.toPx(), center.y - 8.dp.toPx()),
                )
            }
        }

        // 中心显示当前输入
        Text(
            text = currentSpell.ifBlank { layout.centerLabel },
            modifier = Modifier.align(Alignment.Center),
            style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold),
        )

        // 手势处理
        XPadGestureHandler(
            grid = grid,
            zones = layout.zones,
            onZoneSelected = { zone ->
                haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                onZoneSelected(zone)
            },
            onPathComplete = onPathComplete,
        )
    }
}
```

### 3.4 手势处理

```kotlin
@Composable
fun BoxScope.XPadGestureHandler(
    grid: HexGrid,
    zones: List<XPadZone>,
    onZoneSelected: (XPadZone) -> Unit,
    onPathComplete: (List<XPadZone>) -> Unit,
) {
    var currentPath by remember { mutableStateOf<List<XPadZone>>(emptyList()) }
    var currentZone by remember { mutableStateOf<XPadZone?>(null) }

    Modifier.pointerInput(zones) {
        awaitEachGesture {
            val down = awaitFirstDown()
            currentPath = emptyList()

            do {
                val event = awaitPointerEvent()
                val position = event.changes.first().position
                val axial = grid.pixelToAxial(position)
                val zone = zones.find { it.axial == axial }

                if (zone != null && zone != currentZone) {
                    currentZone = zone
                    currentPath = currentPath + zone
                    onZoneSelected(zone)
                }
            } while (event.changes.any { it.pressed })

            // 手势完成
            onPathComplete(currentPath)
            currentPath = emptyList()
            currentZone = null
        }
    }
}
```

---

## 4. X-Pad 状态集成

X-Pad 作为键盘的一种输入模式，其状态集成到键盘状态机中：

```kotlin
// 在 KeyboardState.PinyinInput.XPadding 中
data class XPadding(
    val zones: List<XPadZone>,
    val currentSpell: String,
    val currentPath: List<XPadZone>,
) : PinyinInput()
```

当用户切换到 X-Pad 模式时，键盘状态转换为 `XPadding`；用户完成路径选择后，状态回到 `Waiting` 并触发候选项查询。

---

## 5. Java 功能完整对照

| Java X-Pad 功能 | v4 对应 | 改进说明 |
|----------------|---------|---------|
| `XPadView`（自定义 View） | `XPadView`（Compose） | 声明式 UI |
| `XPainter` 系列（5 个类） | Compose `Canvas` 直接绘制 | 移除 Painter 体系 |
| `XZone` | `XPadZone` data class | 不可变 |
| `XPadState` | `KeyboardState.PinyinInput.XPadding` | 集成到状态机 |
| `XPadKey` | `InputKey.XPad` sealed class 子类 | 类型安全 |
| Mixite 六边形网格 | 自实现 `HexGrid` | 移除 Mixite 依赖 |
| `HexagonDrawable` | Compose `drawPath` | Compose 原生绘制 |
| 触摸事件处理 | `Modifier.pointerInput` | Compose 手势 API |
| 手势轨迹绘制 | Compose `Canvas` | 简化 |
| 拉丁 X-Pad 模式 | `XPadLayout.fromLatinKeys()` | 保留复用逻辑 |
