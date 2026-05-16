# X-Pad 核心

## 1. 概述

X-Pad 是筷字输入法的特色输入模式，采用 8pen/8VIM 风格的连续环形输入，通过六边形网格区域和路径选择实现拼音和拉丁字符的快速输入。`ime-engine` 模块提供 X-Pad 的核心计算逻辑（六边形网格、区域定义、布局生成）和状态集成，UI 渲染和手势处理由 `:ime-ui` 模块负责。

---

## 2. HexGrid 六边形网格计算

自行实现六边形网格计算，使用轴向坐标（axial coordinates）表示六边形位置：

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

---

## 3. XPadZone 区域定义

```kotlin
data class XPadZone(
    val axial: HexGrid.Axial,
    val label: String,
    val chars: List<Char>,
    val color: Color? = null,
)
```

---

## 4. XPadLayout 布局

```kotlin
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

---

## 5. X-Pad 状态集成

X-Pad 作为键盘的一种输入模式，其状态集成到键盘状态机中：

```kotlin
// 在 KeyboardState.PinyinInput 中
data class XPadding(
    val zones: List<XPadZone>,
    val currentSpell: String,
) : PinyinInput()
```

当用户切换到 X-Pad 模式时，键盘状态转换为 `XPadding`；用户完成路径选择后，状态回到 `Waiting` 并触发候选项查询。

### 状态转换

| 转换触发 | 源状态 | 目标状态 |
|----------|--------|----------|
| `StartXPadding(zones)` | `PinyinInput.Waiting` | `PinyinInput.XPadding` |
| `XPadZoneSelected(zone)` | `PinyinInput.XPadding` | `PinyinInput.XPadding`（更新 currentSpell） |
| 路径完成 | `PinyinInput.XPadding` | `PinyinInput.Waiting`（触发候选查询） |
| `BackToPrevious` | `PinyinInput.XPadding` | `PinyinInput.Waiting` |
