# 150 — 输入面板与按键面板分离设计

## 1. 概述

v4 版本将键盘 UI 分离为「输入面板」和「按键面板」两个独立层，输入面板负责接收用户手势并识别为输入意图，按键面板负责按键的渲染、布局和状态展示。两者通过 `InputGesture` 和 `IMEState` 解耦，互不直接依赖。

这种分离架构的核心价值在于：输入面板可以独立于按键面板的位置和布局而工作，为后续版本中支持多种输入-按键布局模式奠定基础——例如全屏输入模式下输入面板在屏幕下半区、按键面板在屏幕上半区浮动。当前版本仅实现最基本的布局模式：输入面板以透明层形式叠加在按键面板上方。

---

## 2. Java 版本输入与按键的耦合分析

### 2.1 当前架构

Java 版本中，按键的绘制、手势检测、输入处理高度耦合在 `KeyboardView` 及其相关组件中：

```
KeyboardView (RecyclerView)
  ├── KeyboardViewLayoutManager  ← 六边形网格布局
  ├── KeyboardViewAdapter        ← 按键 ViewHolder 管理
  ├── KeyboardViewGestureListener ← 手势检测 + 按键查找 + 消息生成
  ├── KeyboardViewKeyAnimator    ← 按键动画
  └── RecyclerViewGestureTrailer ← 滑行轨迹绘制（ItemDecoration）
```

`XPadView` 同样将绘制、手势和输入处理合为一体：

```
XPadView (自定义 View)
  ├── onDraw()           ← 六边形绘制
  ├── onTouchEvent()     ← 手势检测 + 区域查找
  └── XPadState          ← 状态管理
```

### 2.2 问题分析

| 问题 | 说明 |
|------|------|
| **手势与按键坐标绑定** | `KeyboardViewGestureListener` 通过 `findVisibleKeyViewHolderUnderLoose()` 查找触摸点下的 ViewHolder，手势检测依赖按键的实际布局位置。这意味着手势检测只能在按键面板内部进行，无法在外部独立的手势层完成 |
| **X-Pad 手势与绘制不可分** | `XPadView.onTouchEvent()` 既检测手势区域又触发绘制更新，手势逻辑无法脱离 XPad 的绘制区域 |
| **滑行轨迹与按键面板绑定** | `RecyclerViewGestureTrailer` 作为 `ItemDecoration` 绑定在 `KeyboardView` 上，轨迹绘制依赖按键面板的 Canvas |
| **无法支持分离布局** | 输入手势和按键渲染在同一组件中，无法将输入区域和按键区域放置在不同位置 |

---

## 3. v4 输入面板与按键面板分离设计

### 3.1 核心思想

```
┌─────────────────────────────────────────────────────────────┐
│                        输入面板                               │
│  InputPanel（透明手势层）                                     │
│  - 接收原始触摸事件                                           │
│  - 识别手势类型（点击/长按/滑行/翻转）                          │
│  - 查询按键面板布局定位目标按键                                │
│  - 输出 InputGesture → ViewModel                             │
│  - 绘制手势视觉反馈（轨迹、高亮）                               │
├─────────────────────────────────────────────────────────────┤
│                        按键面板                               │
│  KeyPanel（按键渲染层）                                       │
│  - 根据 IMEState 渲染按键布局                                 │
│  - 展示按键状态（按下/激活/禁用）                               │
│  - 提供布局信息供输入面板查询按键位置                            │
│  - 不处理任何触摸事件                                          │
└─────────────────────────────────────────────────────────────┘
```

**关键原则**：
- 输入面板是唯一的触摸事件接收者，按键面板不处理触摸
- 输入面板通过查询按键面板的布局信息来定位目标按键，而非自己维护按键位置
- 按键面板只根据 State 渲染，是纯展示层
- 两者之间的唯一通信渠道是 ViewModel（InputGesture → State → 重组）

### 3.2 布局模式

输入面板和按键面板的相对位置关系由 `LayoutMode` 定义，当前版本仅实现最基本的叠加模式：

```kotlin
/**
 * 输入面板与按键面板的布局模式。
 *
 * 定义两者的空间关系和尺寸分配。
 * 后续版本可扩展新的布局模式。
 */
sealed class LayoutMode {
    /**
     * 叠加模式（当前唯一实现）。
     *
     * 输入面板以全透明层叠加在按键面板之上，
     * 两者完全重叠，输入面板拦截所有触摸事件。
     * 这是与 Java 版本行为一致的默认模式。
     */
    data object Overlay : LayoutMode()

    // 后续版本可扩展：
    // /**
    //  * 全屏输入模式。
    //  *
    //  * 输入面板占据屏幕下半区用于手势输入，
    //  * 按键面板在屏幕上半区浮动显示。
    //  * 输入面板的触摸点映射到按键面板的布局空间进行按键定位。
    //  */
    // data class FullScreen(
    //     val inputPanelRatio: Float = 0.5f,  // 输入面板占比
    //     val keyPanelFloatingOffset: DpOffset = DpOffset(0.dp, 0.dp),
    // ) : LayoutMode()
}
```

---

## 4. 数据模型

### 4.1 InputGesture

输入面板识别手势后，输出 `InputGesture` 而非直接操作按键。`InputGesture` 是坐标无关的逻辑手势描述：

```kotlin
/**
 * 输入手势，坐标无关。
 *
 * 由输入面板识别后发送到 ViewModel，描述用户的输入意图。
 * 不包含任何绝对坐标，只包含按键的语义标识。
 */
sealed class InputGesture {
    /** 手势开始时刻（毫秒） */
    abstract val timestamp: Long

    /**
     * 点击按键。
     *
     * @param key 目标按键（由输入面板根据触摸位置查询按键布局确定）
     */
    data class Tap(
        override val timestamp: Long,
        val key: InputKey,
    ) : InputGesture()

    /**
     * 长按按键。
     */
    data class LongPress(
        override val timestamp: Long,
        val key: InputKey,
    ) : InputGesture()

    /**
     * 滑行输入。
     *
     * @param startKey 起始按键
     * @param endKey 结束按键
     * @param visitedKeys 途经按键序列（按访问顺序）
     * @param duration 滑行持续时间
     */
    data class Swipe(
        override val timestamp: Long,
        val startKey: InputKey,
        val endKey: InputKey,
        val visitedKeys: List<InputKey>,
        val duration: Long,
    ) : InputGesture()

    /**
     * 翻转手势（快速滑行后松手）。
     */
    data class Flip(
        override val timestamp: Long,
        val startKey: InputKey,
        val direction: FlipDirection,
    ) : InputGesture()

    /**
     * X-Pad 区域选择。
     *
     * @param startZone 起始区域
     * @param path 途经区域序列
     */
    data class XPadZonePath(
        override val timestamp: Long,
        val startZone: XPadZone,
        val path: List<XPadZone>,
    ) : InputGesture()

    /**
     * 候选项选择。
     */
    data class CandidateTap(
        override val timestamp: Long,
        val candidateIndex: Int,
    ) : InputGesture()
}

enum class FlipDirection { Left, Right, Up, Down }
```

### 4.2 InputGesture 与 IMEIntent 的关系

`InputGesture` 是输入面板的输出，`IMEIntent` 是 ViewModel 的输入。ViewModel 将 `InputGesture` 转换为 `IMEIntent`：

```kotlin
// 在 IMEViewModel 中
fun handleGesture(gesture: InputGesture) {
    val intent = when (gesture) {
        is InputGesture.Tap -> IMEIntent.KeyPressed(gesture.key, KeyGesture.Tap)
        is InputGesture.LongPress -> IMEIntent.KeyPressed(gesture.key, KeyGesture.LongPress)
        is InputGesture.Swipe -> IMEIntent.KeyPressed(gesture.endKey, KeyGesture.Swipe)
        is InputGesture.Flip -> IMEIntent.KeyPressed(gesture.startKey, KeyGesture.Flip(gesture.direction))
        is InputGesture.XPadZonePath -> IMEIntent.XPadPathSelected(gesture.startZone, gesture.path)
        is InputGesture.CandidateTap -> IMEIntent.CandidateSelected(/* from index */)
    }
    handleIntent(intent)
}
```

这种两层转换的意义：
- `InputGesture` 表达「用户做了什么手势」，属于输入面板的领域
- `IMEIntent` 表达「系统应该做什么」，属于 ViewModel 的领域
- 分离使得同一手势可以产生不同的 Intent（取决于当前键盘状态），也使得不同手势可以产生相同的 Intent

---

## 5. 输入面板设计

### 5.1 InputPanel

```kotlin
/**
 * 输入面板，接收用户手势并识别为 InputGesture。
 *
 * 输入面板是透明的手势拦截层，叠加在按键面板之上。
 * 它不渲染任何可见内容（手势反馈除外），只负责：
 * 1. 拦截触摸事件
 * 2. 根据触摸位置和按键面板布局信息识别目标按键
 * 3. 识别手势类型（点击/长按/滑行/翻转）
 * 4. 输出 InputGesture
 * 5. 绘制手势视觉反馈（滑行轨迹、按键高亮等）
 */
@Composable
fun InputPanel(
    keyPanelLayout: KeyPanelLayoutInfo,
    keyboardType: KeyboardType,
    keyboardState: KeyboardState,
    onGesture: (InputGesture) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        // 手势检测（透明层）
        GestureDetectorLayer(
            keyPanelLayout = keyPanelLayout,
            keyboardType = keyboardType,
            onGesture = onGesture,
        )

        // 手势视觉反馈
        GestureFeedbackLayer(
            keyboardState = keyboardState,
            keyPanelLayout = keyPanelLayout,
        )
    }
}
```

### 5.2 手势检测层

```kotlin
/**
 * 手势检测层。
 *
 * 透明的全尺寸层，拦截所有触摸事件。
 * 根据触摸位置查询按键面板布局，识别目标按键和手势类型。
 */
@Composable
fun BoxScope.GestureDetectorLayer(
    keyPanelLayout: KeyPanelLayoutInfo,
    keyboardType: KeyboardType,
    onGesture: (InputGesture) -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    val gestureState = rememberGestureState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(keyPanelLayout, keyboardType) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val downTime = System.currentTimeMillis()

                    // 查找按下位置的按键
                    val downKey = keyPanelLayout.findKeyAt(down.position)

                    when {
                        // X-Pad 模式：区域路径检测
                        keyboardType == KeyboardType.Pinyin
                            && /* isXPadMode */ false -> {
                            handleXPadGesture(
                                downPosition = down.position,
                                keyPanelLayout = keyPanelLayout,
                                onGesture = onGesture,
                                haptics = haptics,
                            )
                        }
                        // 标准按键：点击/长按/滑行
                        downKey != null -> {
                            handleStandardGesture(
                                downPosition = down.position,
                                downKey = downKey,
                                downTime = downTime,
                                keyPanelLayout = keyPanelLayout,
                                onGesture = onGesture,
                                haptics = haptics,
                            )
                        }
                    }
                }
            },
    )
}
```

### 5.3 标准按键手势处理

```kotlin
/**
 * 标准按键手势处理。
 *
 * 处理按键面板上的点击、长按和滑行手势。
 * 通过 keyPanelLayout 查询触摸位置对应的按键，
 * 而非依赖按键自身的触摸回调。
 */
private suspend fun PointerInputScope.handleStandardGesture(
    downPosition: Offset,
    downKey: InputKey,
    downTime: Long,
    keyPanelLayout: KeyPanelLayoutInfo,
    onGesture: (InputGesture) -> Unit,
    haptics: HapticFeedback,
) {
    var currentKey = downKey
    val visitedKeys = mutableListOf(downKey)
    var isSwiping = false
    var isLongPress = false

    // 长按检测协程
    val longPressJob = coroutineScope {
        launch {
            delay(ViewConfiguration.getLongPressTimeout().toLong())
            isLongPress = true
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            onGesture(InputGesture.LongPress(
                timestamp = System.currentTimeMillis(),
                key = downKey,
            ))
        }
    }

    do {
        val event = awaitPointerEvent()
        val position = event.changes.first().position

        // 查找当前触摸位置的按键
        val keyAtPosition = keyPanelLayout.findKeyAt(position)
        if (keyAtPosition != null && keyAtPosition != currentKey) {
            // 进入新按键 → 滑行
            longPressJob.cancel()
            isSwiping = true
            currentKey = keyAtPosition
            visitedKeys.add(keyAtPosition)
        }
    } while (event.changes.any { it.pressed })

    longPressJob.cancel()

    if (isLongPress) {
        // 长按已处理
    } else if (isSwiping) {
        // 滑行完成
        onGesture(InputGesture.Swipe(
            timestamp = System.currentTimeMillis(),
            startKey = downKey,
            endKey = currentKey,
            visitedKeys = visitedKeys.toList(),
            duration = System.currentTimeMillis() - downTime,
        ))
    } else {
        // 点击
        haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
        onGesture(InputGesture.Tap(
            timestamp = System.currentTimeMillis(),
            key = downKey,
        ))
    }
}
```

### 5.4 X-Pad 手势处理

```kotlin
/**
 * X-Pad 手势处理。
 *
 * X-Pad 模式下，手指在按键面板的 XPad 区域滑行。
 * 输入面板通过 keyPanelLayout 查询触摸位置对应的 X-Pad 区域，
 * 而非依赖 XPadView 自身的触摸处理。
 */
private suspend fun PointerInputScope.handleXPadGesture(
    downPosition: Offset,
    keyPanelLayout: KeyPanelLayoutInfo,
    onGesture: (InputGesture) -> Unit,
    haptics: HapticFeedback,
) {
    var currentZone = keyPanelLayout.findXPadZoneAt(downPosition)
    val path = mutableListOf<XPadZone>()

    if (currentZone != null) {
        path.add(currentZone)
    }

    do {
        val event = awaitPointerEvent()
        val position = event.changes.first().position
        val zone = keyPanelLayout.findXPadZoneAt(position)

        if (zone != null && zone != currentZone) {
            currentZone = zone
            path.add(zone)
            haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
        }
    } while (event.changes.any { it.pressed })

    if (path.isNotEmpty()) {
        onGesture(InputGesture.XPadZonePath(
            timestamp = System.currentTimeMillis(),
            startZone = path.first(),
            path = path.toList(),
        ))
    }
}
```

### 5.5 手势反馈层

```kotlin
/**
 * 手势视觉反馈层。
 *
 * 在输入面板上绘制手势相关的视觉效果：
 * - 滑行轨迹
 * - 当前按下按键的高亮
 * - X-Pad 路径高亮
 *
 * 注意：按键的常规视觉状态（如按下、激活）由按键面板自身根据
 * KeyboardState 渲染，此处仅绘制与手势动作相关的临时视觉反馈。
 */
@Composable
fun BoxScope.GestureFeedbackLayer(
    keyboardState: KeyboardState,
    keyPanelLayout: KeyPanelLayoutInfo,
) {
    // 滑行轨迹
    val swipeState by rememberSwipeFeedbackState()

    if (swipeState.trailPoints.isNotEmpty()) {
        GestureTrailOverlay(path = swipeState.trailPoints)
    }

    // 按键高亮（当前手指按下的按键）
    swipeState.pressedKey?.let { key ->
        KeyHighlightFeedback(
            key = key,
            keyPanelLayout = keyPanelLayout,
        )
    }
}

/**
 * 手势反馈状态，由手势检测层驱动更新。
 */
class SwipeFeedbackState {
    private val _trailPoints = MutableStateFlow<List<Offset>>(emptyList())
    val trailPoints: StateFlow<List<Offset>> = _trailPoints.asStateFlow()

    private val _pressedKey = MutableStateFlow<InputKey?>(null)
    val pressedKey: StateFlow<InputKey?> = _pressedKey.asStateFlow()

    fun addTrailPoint(offset: Offset) {
        _trailPoints.update { it + offset }
    }

    fun clearTrail() {
        _trailPoints.value = emptyList()
    }

    fun setPressedKey(key: InputKey?) {
        _pressedKey.value = key
    }
}
```

---

## 6. 按键面板设计

### 6.1 KeyPanel

```kotlin
/**
 * 按键面板，纯展示层。
 *
 * 根据 IMEState 渲染按键布局和状态，不处理任何触摸事件。
 * 通过 onLayoutInfoChanged 回调向输入面板提供布局信息。
 *
 * 按键面板是"被动"的——它的所有视觉变化都由 State 驱动，
 * 不响应任何触摸事件。
 */
@Composable
fun KeyPanel(
    keyboardType: KeyboardType,
    keyGrid: List<List<InputKey>>,
    keyboardState: KeyboardState,
    onLayoutInfoChanged: (KeyPanelLayoutInfo) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (keyboardType) {
        KeyboardType.Pinyin, KeyboardType.Latin, KeyboardType.Number,
        KeyboardType.Symbol, KeyboardType.Editor, KeyboardType.Math,
        -> StandardKeyPanel(keyGrid, keyboardState, onLayoutInfoChanged, modifier)

        KeyboardType.Emoji -> EmojiKeyPanel(keyGrid, keyboardState, onLayoutInfoChanged, modifier)
        KeyboardType.Candidate -> CandidateKeyPanel(keyGrid, onLayoutInfoChanged, modifier)
        KeyboardType.CommitOption -> CommitOptionKeyPanel(keyGrid, onLayoutInfoChanged, modifier)
    }
}
```

### 6.2 StandardKeyPanel

```kotlin
/**
 * 标准按键面板。
 *
 * 渲染网格布局的按键，通过 onGloballyPositioned 收集每个按键的位置信息，
 * 通过 onLayoutInfoChanged 回调通知输入面板。
 */
@Composable
fun StandardKeyPanel(
    keyGrid: List<List<InputKey>>,
    keyboardState: KeyboardState,
    onLayoutInfoChanged: (KeyPanelLayoutInfo) -> Unit,
    modifier: Modifier = Modifier,
) {
    val keyPositions = remember { mutableMapOf<InputKey, Rect>() }

    Column(
        modifier = modifier.fillMaxWidth(),
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
                        modifier = Modifier
                            .weight(key.weight)
                            .onGloballyPositioned { coordinates ->
                                // 收集按键位置信息
                                keyPositions[key] = coordinates.boundsInRoot()
                                // 通知布局更新
                                onLayoutInfoChanged(
                                    KeyPanelLayoutInfo(
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

### 6.3 KeyView（无触摸处理）

按键面板中的 `KeyView` 不处理触摸事件，只负责渲染：

```kotlin
/**
 * 按键视图（纯展示，无触摸处理）。
 *
 * 与文档 400 中的 KeyView 不同，此版本移除了 combinedClickable，
 * 因为触摸事件由输入面板统一处理。
 * 按键的"按下"视觉状态由 keyboardState 驱动，而非触摸回调。
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
```

### 6.4 KeyPanelLayoutInfo

```kotlin
/**
 * 按键面板布局信息。
 *
 * 由按键面板通过 Compose 布局系统收集，提供给输入面板用于按键定位。
 * 每次 Compose 重组后自动更新，确保与实际渲染位置一致。
 */
data class KeyPanelLayoutInfo(
    /** 各按键在屏幕上的矩形区域 */
    val keyPositions: Map<InputKey, Rect>,
    /** X-Pad 布局信息（仅 X-Pad 模式时非空） */
    val xPadLayoutInfo: XPadLayoutInfo?,
    /** 候选栏布局信息 */
    val candidateLayoutInfo: CandidateLayoutInfo?,
) {
    /**
     * 查找指定坐标处的按键。
     *
     * @param position 屏幕坐标
     * @return 该位置上的按键，若无可点击按键则返回 null
     */
    fun findKeyAt(position: Offset): InputKey? {
        return keyPositions.entries.find { (_, rect) ->
            rect.contains(position)
        }?.key
    }

    /**
     * 查找指定坐标处的 X-Pad 区域。
     */
    fun findXPadZoneAt(position: Offset): XPadZone? {
        return xPadLayoutInfo?.findZoneAt(position)
    }
}

/**
 * X-Pad 布局信息。
 */
data class XPadLayoutInfo(
    val grid: HexGrid,
    val zones: List<XPadZone>,
    val bounds: Rect,
) {
    fun findZoneAt(position: Offset): XPadZone? {
        if (!bounds.contains(position)) return null
        val axial = grid.pixelToAxial(position)
        return zones.find { it.axial == axial }
    }
}

/**
 * 候选栏布局信息。
 */
data class CandidateLayoutInfo(
    val candidatePositions: List<Rect>,
) {
    fun findCandidateAt(position: Offset): Int? {
        return candidatePositions.indexOfFirst { it.contains(position) }
            .takeIf { it >= 0 }
    }
}
```

---

## 7. 组合布局

### 7.1 叠加模式（当前唯一实现）

输入面板透明叠加在按键面板上方，两者完全重叠：

```kotlin
/**
 * 键盘区域，组合输入面板和按键面板。
 *
 * 在叠加模式下，按键面板先渲染，输入面板以透明层叠加在其上方，
 * 输入面板拦截所有触摸事件，按键面板不接收触摸。
 */
@Composable
fun KeyboardArea(
    keyboardType: KeyboardType,
    keyGrid: List<List<InputKey>>,
    keyboardState: KeyboardState,
    onGesture: (InputGesture) -> Unit,
    modifier: Modifier = Modifier,
) {
    // 按键面板布局信息
    var keyPanelLayout by remember { mutableStateOf(KeyPanelLayoutInfo(emptyMap(), null, null)) }

    Box(modifier = modifier) {
        // 底层：按键面板（纯展示）
        KeyPanel(
            keyboardType = keyboardType,
            keyGrid = keyGrid,
            keyboardState = keyboardState,
            onLayoutInfoChanged = { keyPanelLayout = it },
        )

        // 上层：输入面板（透明手势层）
        InputPanel(
            keyPanelLayout = keyPanelLayout,
            keyboardType = keyboardType,
            keyboardState = keyboardState,
            onGesture = onGesture,
        )
    }
}
```

### 7.2 IMEScreen 集成

```kotlin
@Composable
fun IMEScreen(viewModel: IMEViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    KuaiziIMETheme(themeType = state.config.themeType) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // 候选项栏
            CandidateBar(
                candidates = state.candidates,
                onCandidateSelected = { viewModel.handleGesture(InputGesture.CandidateTap(/* ... */)) },
            )

            // 输入栏
            InputBar(
                inputList = state.inputList,
                onGapTapped = { viewModel.handleIntent(IMEIntent.CursorMoveTo(it)) },
            )

            // 键盘区域（输入面板 + 按键面板）
            KeyboardArea(
                keyboardType = state.keyboardType,
                keyGrid = state.keyGrid,
                keyboardState = state.keyboardState,
                onGesture = { viewModel.handleGesture(it) },
            )

            // 工具栏
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

---

## 8. 与坐标无关输入动作程序化的协作

文档 930 的 `KeyPositionResolver` 与本文档的 `KeyPanelLayoutInfo` 协作：

```kotlin
/**
 * 基于 KeyPanelLayoutInfo 实现的坐标解析器。
 *
 * 复用按键面板的布局信息，供输入动作程序化系统在回放时
 * 动态查询按键位置。
 */
class KeyPanelPositionResolver(
    private val layoutProvider: () -> KeyPanelLayoutInfo,
) : KeyPositionResolver {

    override fun resolve(key: InputKey): Offset? {
        val layout = layoutProvider()
        return layout.keyPositions[key]?.center
    }

    override fun resolveCandidatePosition(index: Int): Offset? {
        val layout = layoutProvider()
        return layout.candidateLayoutInfo?.candidatePositions?.getOrNull(index)?.center
    }
}
```

这样，输入动作程序化系统与输入-按键分离架构共享同一套布局信息，无需维护两套位置查询逻辑。

---

## 9. X-Pad 的特殊处理

### 9.1 输入面板接管 X-Pad 手势

Java 版本中 `XPadView` 自行处理触摸事件和手势检测。分离后，X-Pad 的手势检测由输入面板统一处理：

| 职责 | Java 版本（XPadView 内） | v4（分离后） |
|------|--------------------------|-------------|
| 六边形绘制 | XPadView.onDraw() | KeyPanel XPadKeyContent |
| 区域计算 | XPadView.findBlockAt() | KeyPanelLayoutInfo.findXPadZoneAt() |
| 触摸检测 | XPadView.onTouchEvent() | InputPanel GestureDetectorLayer |
| 路径追踪 | XPadState.BlockData | InputGesture.XPadZonePath |
| 状态更新 | XPadView → IMEditor | InputGesture → ViewModel → State → KeyPanel |

### 9.2 X-Pad 按键面板渲染

X-Pad 作为按键面板的一种渲染模式，仍使用 Compose Canvas 绘制六边形网格，但不处理触摸：

```kotlin
@Composable
fun XPadKeyContent(
    key: InputKey.XPad,
    modifier: Modifier = Modifier,
) {
    // X-Pad 在按键面板中渲染六边形网格
    // 手势处理由输入面板的 handleXPadGesture 负责
    Box(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            // 绘制六边形网格和区域标签
            // 状态高亮由 KeyboardState.PinyinInput.XPadding 驱动
        }
    }
}
```

---

## 10. 后续扩展

### 10.1 全屏输入模式（后续版本）

叠加模式是基础，全屏输入模式是重要扩展方向。在全屏输入模式下，输入面板和按键面板不再重叠：

```
┌─────────────────────────────┐
│     按键面板（浮动上半区）      │  ← 仅展示按键，不接收触摸
│     ┌───────────────────┐    │
│     │  Q  W  E  R  T    │    │
│     │  A  S  D  F  G    │    │
│     │  Z  X  C  V  B    │    │
│     └───────────────────┘    │
│                              │
├──────────────────────────────┤
│     输入面板（下半区）         │  ← 接收所有触摸手势
│                              │
│     用户在此区域触摸/滑行      │
│     系统将触摸位置映射到        │
│     按键面板的布局空间          │
│                              │
└──────────────────────────────┘
```

由于输入面板和按键面板已经分离，全屏模式只需要：
1. 新增 `LayoutMode.FullScreen` 定义
2. 修改 `KeyboardArea` 的组合逻辑（不再叠加，改为上下布局）
3. 输入面板的按键查找增加坐标映射（输入面板坐标 → 按键面板坐标）

当前版本不实现全屏模式，但架构预留了扩展点。

### 10.2 坐标映射

全屏输入模式需要将输入面板的触摸坐标映射到按键面板的布局空间。这种映射由 `LayoutMode` 定义：

```kotlin
sealed class LayoutMode {
    data object Overlay : LayoutMode() {
        // 叠加模式：无需映射，触摸坐标直接对应按键面板坐标
    }

    // 后续版本：
    // data class FullScreen(...) : LayoutMode() {
    //     // 全屏模式：需要将输入面板坐标映射到按键面板坐标
    //     fun mapToKeyPanel(inputPosition: Offset, inputPanelBounds: Rect, keyPanelBounds: Rect): Offset {
    //         return Offset(
    //             x = (inputPosition.x / inputPanelBounds.width) * keyPanelBounds.width,
    //             y = (inputPosition.y / inputPanelBounds.height) * keyPanelBounds.height,
    //         )
    //     }
    // }
}
```

---

## 11. Java 功能完整对照

| Java 组件 | v4 分离后对应 | 说明 |
|-----------|-------------|------|
| `KeyboardViewGestureListener` | `InputPanel.GestureDetectorLayer` | 手势检测从按键面板移至输入面板 |
| `KeyboardView`（触摸事件） | `InputPanel`（透明手势层） | 触摸事件接收者分离 |
| `KeyboardView`（按键渲染） | `KeyPanel`（纯展示层） | 按键渲染独立，无触摸处理 |
| `ViewGestureTrailer` | `InputPanel.GestureFeedbackLayer` | 轨迹绘制从按键面板 ItemDecoration 移至输入面板 |
| `XPadView.onTouchEvent()` | `InputPanel.handleXPadGesture()` | X-Pad 手势由输入面板统一处理 |
| `XPadView.onDraw()` | `KeyPanel.XPadKeyContent` | X-Pad 绘制保留在按键面板 |
| `findVisibleKeyViewHolderUnderLoose()` | `KeyPanelLayoutInfo.findKeyAt()` | 按键查找改为查询布局信息 |
| `XPadView.findBlockAt()` | `KeyPanelLayoutInfo.findXPadZoneAt()` | X-Pad 区域查找改为查询布局信息 |
| `KeyViewHolder.touchDown/Up()` | `KeyboardState` 驱动的按键视觉状态 | 按键视觉反馈由 State 驱动 |
| N/A | `InputGesture` | 新增：逻辑手势抽象层 |
| N/A | `KeyPanelLayoutInfo` | 新增：按键面板布局信息暴露 |

---

## 12. 与其他系统的协作

| 协作系统 | 协作方式 |
|----------|----------|
| 键盘状态机（100） | `InputGesture` 经 ViewModel 转换为 `IMEIntent`，驱动状态机；状态变更更新 `KeyboardState`，按键面板据此重组 |
| UI Compose 迁移（400） | `IMEScreen` 中的 `KeyboardArea` 替代原 `KeyboardArea`，内含输入面板+按键面板双层结构 |
| X-Pad 重构（700） | X-Pad 的手势检测从 `XPadView` 移至输入面板，绘制保留在按键面板的 `XPadKeyContent` |
| 输入动作程序化（930） | `KeyPanelPositionResolver` 复用 `KeyPanelLayoutInfo`，共享按键位置查询逻辑 |
| 配置系统（500） | `LayoutMode` 可作为配置项持久化（当前仅有 Overlay） |
| 配置界面改进（920） | 后续版本可在设置中提供布局模式切换入口 |
