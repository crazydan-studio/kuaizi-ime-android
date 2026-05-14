# 150 — 输入面板、按键面板与反馈面板三层分离设计

## 1. 概述

v4 版本将键盘 UI 分离为三个独立层：**输入面板**（Gesture Input Panel）负责接收用户手势并识别为输入意图，**按键面板**（Key Panel）负责按键的渲染、布局和状态展示，**反馈面板**（Feedback Panel）负责手势视觉反馈的绘制。三者通过 `InputGesture`、`GestureFeedbackState` 和 `ImeState` 解耦，互不直接依赖。

反馈面板是独立于输入面板和按键面板的透明层，支持多实例。这种设计使得反馈面板可以灵活地与输入面板、按键面板组合叠加——在叠加布局模式下，三层面板完全重叠；在全屏分离布局模式下，反馈面板可以分别与输入面板和按键面板叠加，使手指轨迹在输入区域可见、按键高亮在按键区域可见。

这种三层分离架构的核心价值在于：每一层都可以独立地改变位置、大小和组合方式，而不影响其他层的功能。输入面板可以独立于按键面板的位置工作，反馈面板可以独立地跟随任意面板布局，为后续版本中支持多种输入-按键-反馈布局模式奠定基础。当前版本仅实现最基本的布局模式：三层面板以透明层形式叠加。

---

## 2. Java 版本输入、按键与反馈的耦合分析

### 2.1 当前架构

Java 版本中，按键的绘制、手势检测、手势反馈和输入处理高度耦合在 `KeyboardView` 及其相关组件中：

```
KeyboardView (RecyclerView)
  ├── KeyboardViewLayoutManager        ← 六边形网格布局
  ├── KeyboardViewAdapter              ← 按键 ViewHolder 管理
  ├── KeyboardViewGestureListener      ← 手势检测 + 按键查找 + 消息生成
  ├── KeyboardViewKeyAnimator          ← 按键动画（状态反馈）
  └── RecyclerViewGestureTrailer       ← 滑行轨迹绘制（ItemDecoration，反馈）
```

`XPadView` 同样将绘制、手势和反馈合为一体：

```
XPadView (自定义 View)
  ├── onDraw()                         ← 六边形绘制 + 区域高亮（状态反馈）
  ├── onTouchEvent()                   ← 手势检测 + 区域查找
  └── XPadState                        ← 状态管理 → 触发重绘（反馈）
```

### 2.2 问题分析

| 问题 | 说明 |
|------|------|
| **手势检测与按键坐标绑定** | `KeyboardViewGestureListener` 通过 `findVisibleKeyViewHolderUnderLoose()` 查找触摸点下的 ViewHolder，手势检测依赖按键的实际布局位置，无法在外部独立的手势层完成 |
| **手势反馈与按键面板绑定** | `RecyclerViewGestureTrailer` 作为 `ItemDecoration` 绑定在 `KeyboardView` 上，轨迹绘制依赖按键面板的 Canvas，无法在按键面板之外绘制反馈 |
| **按键状态反馈与按键渲染混合** | `KeyboardViewKeyAnimator` 既负责按键的常规渲染又负责按下/激活等状态动画，视觉反馈逻辑无法脱离按键面板 |
| **X-Pad 手势、反馈与绘制不可分** | `XPadView.onTouchEvent()` 既检测手势区域又触发绘制更新（包括区域高亮），手势逻辑和反馈逻辑无法脱离 XPad 的绘制区域 |
| **无法支持分离布局** | 输入手势、按键渲染和视觉反馈在同一组件中，无法将输入区域、按键区域和反馈区域放置在不同位置 |
| **无法独立控制反馈** | 反馈的显隐、样式、位置与按键面板的生命周期绑定，无法在不影响按键渲染的情况下控制反馈 |

---

## 3. v4 三层分离设计

### 3.1 核心思想

```
┌─────────────────────────────────────────────────────────────┐
│                     输入面板（最上层）                          │
│  GestureInputPanel（透明手势层）                                     │
│  - 接收原始触摸事件（必须在最上层，确保触摸优先到达）             │
│  - 识别手势类型（点击/长按/滑行/翻转）                          │
│  - 查询按键面板布局定位目标按键                                │
│  - 输出 InputGesture → ViewModel                             │
│  - 驱动 GestureFeedbackState → 反馈面板                       │
│  - 不绘制任何视觉反馈（完全透明，不遮挡下层反馈面板的视觉效果）    │
├─────────────────────────────────────────────────────────────┤
│                     反馈面板（可多实例）                        │
│  GestureFeedbackPanel（透明绘制层）                                  │
│  - 绘制滑行轨迹                                              │
│  - 绘制按键高亮                                              │
│  - 绘制 X-Pad 路径高亮                                       │
│  - 绘制手指指示器（程序化输入动画）                              │
│  - 不处理任何触摸事件（触摸事件穿透到下层按键面板或被上层输入面板拦截）│
│  - 不依赖任何面板的 Canvas                                    │
│  - 可与输入面板叠加，也可与按键面板叠加                         │
├─────────────────────────────────────────────────────────────┤
│                        按键面板（最下层）                       │
│  KeyGridPanel（按键渲染层）                                       │
│  - 根据 ImeState 渲染按键布局                                 │
│  - 展示按键状态（按下/激活/禁用）                               │
│  - 提供布局信息供输入面板和反馈面板查询                          │
│  - 不处理任何触摸事件                                          │
│  - 不绘制手势反馈                                             │
└─────────────────────────────────────────────────────────────┘
```

**三条核心原则**：

1. **输入面板是唯一的触摸事件接收者**，按键面板和反馈面板不处理触摸。输入面板必须在最上层，确保触摸事件优先到达手势检测层，而非被反馈面板或按键面板拦截
2. **反馈面板是唯一的视觉反馈绘制者**，输入面板不绘制反馈（完全透明），按键面板仅绘制按键的常规状态渲染（非手势触发的临时反馈）。输入面板虽在最上层，但因完全透明，不会遮挡反馈面板的视觉效果
3. **三者之间的通信通过共享状态解耦**：输入面板 → `InputGesture` → ViewModel → `ImeState` → 按键面板；输入面板 → `GestureFeedbackState` → 反馈面板

**叠加顺序的设计考量**：

为什么输入面板在反馈面板上层而非下层？因为 Compose 的触摸事件从最上层开始向下传播，如果反馈面板在输入面板之上，即使其 Canvas 不主动消费触摸事件，也存在事件被意外拦截的风险（如未来添加交互功能、Compose 版本行为变更等）。将输入面板置于最上层是最可靠的方案——它确保所有触摸事件首先到达手势检测层，同时输入面板完全透明不绘制任何内容，不会视觉遮挡下层的反馈绘制效果。

**按键面板与反馈面板的视觉职责边界**：

按键面板根据 `KeyboardState` 渲染按键的**持续性视觉状态**（如按下态、激活态、禁用态、候选选中态），这些状态是由 ViewModel 的 reduce 结果驱动的确定性渲染。反馈面板绘制**临时性手势视觉反馈**（如滑行轨迹、按键高亮光圈、X-Pad 路径高亮、手指指示器），这些反馈跟随用户手指实时变化，在手势结束后消失。这种分离确保了按键面板的重组仅由状态变更驱动，而高频的手势反馈更新不会触发按键面板的重组。

### 3.2 布局模式

三层面板的相对位置关系由 `LayoutMode` 定义，叠加模式由 `KeyboardPanel` 实现，全屏模式由 `KeyboardScreen` 实现。`KeyboardPanel` 和 `KeyboardScreen` 均为完整的输入法组件，包含候选栏、输入栏、工具栏和键盘区域，二者只是形式和交互上存在差异：

```kotlin
/**
 * 输入面板、按键面板与反馈面板的布局模式。
 *
 * 定义三者的空间关系和尺寸分配。
 * 叠加模式由 KeyboardPanel 实现，全屏模式由 KeyboardScreen 实现（详见文档 160 §5.4/§5.4b）。
 */
sealed class LayoutMode {
    /**
     * 叠加模式。
     *
     * 三层面板完全重叠，由 KeyboardPanel 直接组合：
     * - 底层：按键面板（按键渲染）
     * - 中层：反馈面板（透明视觉反馈）
     * - 顶层：输入面板（透明手势拦截）
     *
     * 输入面板在最上层，确保触摸事件优先到达手势检测层。
     * 反馈面板在中间层绘制视觉反馈，输入面板完全透明不遮挡反馈。
     * 这是与 Java 版本行为一致的默认模式。
     */
    data object Overlay : LayoutMode()

    /**
     * 全屏输入模式，由 KeyboardScreen 实现。
     *
     * 输入面板占据屏幕下半区用于手势输入，
     * 按键面板在屏幕上半区浮动显示。
     * 反馈面板的多实例分别叠加在各面板之下（输入面板始终在最上层拦截触摸）：
     * - 输入区域：GestureInputPanel（顶层）→ GestureFeedbackPanel（底层，绘制触摸轨迹和手指指示器）
     * - 按键区域：GestureFeedbackPanel（底层，绘制按键高亮和按键路径）→ KeyGridPanel
     *
     * 输入面板的触摸点映射到按键面板的布局空间进行按键定位。
     */
    data class FullScreen(
        val inputPanelRatio: Float = 0.5f,
    ) : LayoutMode()
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
        /** 连续点击同一按键的次数（0=首次，1=双击，2=三击...） */
        val tick: Int = 0,
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

### 4.2 InputGesture 与 ImeIntent 的关系

`InputGesture` 是输入面板的输出，`ImeIntent` 是 ViewModel 的输入。ViewModel 将 `InputGesture` 转换为 `ImeIntent`：

```kotlin
// 在 KeyboardViewModel 中
fun handleGesture(gesture: InputGesture) {
    val intent = when (gesture) {
        is InputGesture.Tap -> ImeIntent.PressKey(gesture.key, KeyGesture.Tap)
        is InputGesture.LongPress -> ImeIntent.PressKey(gesture.key, KeyGesture.LongPress)
        is InputGesture.Swipe -> ImeIntent.PressKey(gesture.endKey, KeyGesture.Swipe)
        is InputGesture.Flip -> ImeIntent.PressKey(gesture.startKey, KeyGesture.Flip(gesture.direction))
        is InputGesture.XPadZonePath -> ImeIntent.SelectXPadPath(gesture.startZone, gesture.path)
        is InputGesture.CandidateTap -> ImeIntent.SelectCandidate(/* from index */)
    }
    handleIntent(intent)
}
```

这种两层转换的意义：
- `InputGesture` 表达「用户做了什么手势」，属于输入面板的领域
- `ImeIntent` 表达「系统应该做什么」，属于 ViewModel 的领域
- 分离使得同一手势可以产生不同的 Intent（取决于当前键盘状态），也使得不同手势可以产生相同的 Intent

### 4.3 GestureFeedbackState

反馈面板的状态由输入面板的手势事件和 ViewModel 的键盘状态共同驱动，独立于任何面板：

```kotlin
/**
 * 手势反馈状态，独立于任何面板。
 *
 * 由输入面板的手势事件驱动更新，供 GestureFeedbackPanel 消费。
 * 反馈状态与 ViewModel 的 ImeState 分离：
 * - ImeState 描述键盘的逻辑状态（哪个键被选中、候选项列表等）
 * - GestureFeedbackState 描述手势的视觉反馈（滑行轨迹、临时高亮等）
 *
 * 这种分离确保了反馈状态的高频更新不会触发 ImeState 的变更，
 * 避免不必要的按键面板重组。
 */
class GestureFeedbackState {
    /**
     * 触摸轨迹点（跟随手指的原始路径）。
     *
     * 在手势过程中实时积累，手势结束后自动清除。
     * 这些点是输入面板坐标系下的绝对坐标，
     * 反馈面板在叠加模式下可直接使用，
     * 在分离模式下需通过 LayoutMode 的坐标映射转换。
     */
    private val _touchTrailPoints = MutableStateFlow<List<Offset>>(emptyList())
    val touchTrailPoints: StateFlow<List<Offset>> = _touchTrailPoints.asStateFlow()

    /**
     * 当前按下的按键集合（临时高亮）。
     *
     * 由输入面板在手势过程中根据触摸位置更新，
     * 手势结束后清除。与 KeyboardState 中的按键激活状态不同，
     * 这是跟随手指实时变化的临时高亮。
     */
    private val _pressedKeys = MutableStateFlow<Set<InputKey>>(emptySet())
    val pressedKeys: StateFlow<Set<InputKey>> = _pressedKeys.asStateFlow()

    /**
     * 按键间路径（滑行时经过的按键序列）。
     *
     * 用于在按键面板侧绘制按键间的逻辑连接线，
     * 区别于触摸轨迹（跟随手指的原始路径）。
     */
    private val _keyPath = MutableStateFlow<List<InputKey>>(emptyList())
    val keyPath: StateFlow<List<InputKey>> = _keyPath.asStateFlow()

    /**
     * X-Pad 路径区域（当前手势经过的区域）。
     *
     * 仅在 X-Pad 模式下使用，用于在按键面板侧
     * 绘制 X-Pad 区域的高亮路径。
     */
    private val _xPadPath = MutableStateFlow<List<XPadZone>>(emptyList())
    val xPadPath: StateFlow<List<XPadZone>> = _xPadPath.asStateFlow()

    /**
     * 手指指示器状态（用于程序化输入动画）。
     *
     * 由 InputActionPlayer（文档 930）驱动，
     * 独立于用户真实手势。当程序化输入播放时，
     * 手指指示器的状态会覆盖用户真实手势的反馈。
     */
    private val _fingerIndicator = MutableStateFlow<FingerIndicatorState?>(null)
    val fingerIndicator: StateFlow<FingerIndicatorState?> = _fingerIndicator.asStateFlow()

    // --- 由输入面板调用的更新方法 ---

    fun addTouchTrailPoint(offset: Offset) {
        _touchTrailPoints.update { it + offset }
    }

    fun clearTouchTrail() {
        _touchTrailPoints.value = emptyList()
    }

    fun setPressedKeys(keys: Set<InputKey>) {
        _pressedKeys.value = keys
    }

    fun clearPressedKeys() {
        _pressedKeys.value = emptySet()
    }

    fun setKeyPath(keys: List<InputKey>) {
        _keyPath.value = keys
    }

    fun clearKeyPath() {
        _keyPath.value = emptyList()
    }

    fun setXPadPath(zones: List<XPadZone>) {
        _xPadPath.value = zones
    }

    fun clearXPadPath() {
        _xPadPath.value = emptyList()
    }

    fun setFingerIndicator(state: FingerIndicatorState?) {
        _fingerIndicator.value = state
    }

    /** 手势结束，清除所有临时反馈 */
    fun clearAll() {
        clearTouchTrail()
        clearPressedKeys()
        clearKeyPath()
        clearXPadPath()
    }
}

/**
 * 手指指示器状态。
 */
data class FingerIndicatorState(
    val position: Offset,
    val pressed: Boolean,
    val visible: Boolean = true,
)
```

---

## 5. 输入面板设计

### 5.1 GestureInputPanel

输入面板仅负责手势捕获，不绘制任何视觉反馈：

```kotlin
/**
 * 输入面板，接收用户手势并识别为 InputGesture。
 *
 * 输入面板是透明的手势拦截层，叠加在按键面板之上。
 * 它不渲染任何可见内容，只负责：
 * 1. 拦截触摸事件
 * 2. 根据触摸位置和按键面板布局信息识别目标按键
 * 3. 识别手势类型（点击/长按/滑行/翻转）
 * 4. 输出 InputGesture → ViewModel
 * 5. 驱动 GestureFeedbackState → 反馈面板
 *
 * 注意：手势视觉反馈由独立的 GestureFeedbackPanel 绘制，
 * 输入面板不包含任何绘制逻辑。
 */
@Composable
fun GestureInputPanel(
    keyPanelLayout: KeyGridPanelLayoutInfo,
    keyboardType: KeyboardType,
    feedbackState: GestureFeedbackState,
    onGesture: (InputGesture) -> Unit,
    modifier: Modifier = Modifier,
) {
    // 纯手势检测（透明层，无绘制）
    GestureDetectorLayer(
        keyPanelLayout = keyPanelLayout,
        keyboardType = keyboardType,
        feedbackState = feedbackState,
        onGesture = onGesture,
        modifier = modifier.fillMaxSize(),
    )
}
```

### 5.2 手势检测层

```kotlin
/**
 * 手势检测层。
 *
 * 透明的全尺寸层，拦截所有触摸事件。
 * 根据触摸位置查询按键面板布局，识别目标按键和手势类型。
 * 手势过程中同步更新 GestureFeedbackState 供反馈面板消费。
 */
@Composable
fun GestureDetectorLayer(
    keyPanelLayout: KeyGridPanelLayoutInfo,
    keyboardType: KeyboardType,
    feedbackState: GestureFeedbackState,
    onGesture: (InputGesture) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current

    Box(
        modifier = modifier
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
                                feedbackState = feedbackState,
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
                                feedbackState = feedbackState,
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
 * 同时更新 feedbackState 供反馈面板绘制视觉反馈。
 */
private suspend fun PointerInputScope.handleStandardGesture(
    downPosition: Offset,
    downKey: InputKey,
    downTime: Long,
    keyPanelLayout: KeyGridPanelLayoutInfo,
    feedbackState: GestureFeedbackState,
    onGesture: (InputGesture) -> Unit,
    haptics: HapticFeedback,
) {
    var currentKey = downKey
    val visitedKeys = mutableListOf(downKey)
    var isSwiping = false
    var isLongPress = false

    // 初始反馈：按下按键高亮 + 触摸轨迹起点
    feedbackState.setPressedKeys(setOf(downKey))
    feedbackState.addTouchTrailPoint(downPosition)

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

        // 更新触摸轨迹
        feedbackState.addTouchTrailPoint(position)

        // 查找当前触摸位置的按键
        val keyAtPosition = keyPanelLayout.findKeyAt(position)
        if (keyAtPosition != null && keyAtPosition != currentKey) {
            // 进入新按键 → 滑行
            longPressJob.cancel()
            isSwiping = true
            currentKey = keyAtPosition
            visitedKeys.add(keyAtPosition)

            // 更新反馈：当前按下的按键 + 按键路径
            feedbackState.setPressedKeys(setOf(keyAtPosition))
            feedbackState.setKeyPath(visitedKeys.toList())
        }
    } while (event.changes.any { it.pressed })

    longPressJob.cancel()

    // 手势结束，清除临时反馈
    feedbackState.clearAll()

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
 * 同时更新 feedbackState 供反馈面板绘制 X-Pad 路径高亮。
 */
private suspend fun PointerInputScope.handleXPadGesture(
    downPosition: Offset,
    keyPanelLayout: KeyGridPanelLayoutInfo,
    feedbackState: GestureFeedbackState,
    onGesture: (InputGesture) -> Unit,
    haptics: HapticFeedback,
) {
    var currentZone = keyPanelLayout.findXPadZoneAt(downPosition)
    val path = mutableListOf<XPadZone>()

    if (currentZone != null) {
        path.add(currentZone)
        feedbackState.setXPadPath(path)
    }

    feedbackState.addTouchTrailPoint(downPosition)

    do {
        val event = awaitPointerEvent()
        val position = event.changes.first().position
        val zone = keyPanelLayout.findXPadZoneAt(position)

        feedbackState.addTouchTrailPoint(position)

        if (zone != null && zone != currentZone) {
            currentZone = zone
            path.add(zone)
            feedbackState.setXPadPath(path)
            haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
        }
    } while (event.changes.any { it.pressed })

    // 手势结束，清除临时反馈
    feedbackState.clearAll()

    if (path.isNotEmpty()) {
        onGesture(InputGesture.XPadZonePath(
            timestamp = System.currentTimeMillis(),
            startZone = path.first(),
            path = path.toList(),
        ))
    }
}
```

---

## 6. 反馈面板设计

### 6.1 核心概念

反馈面板是独立于输入面板和按键面板的透明绘制层，职责是渲染所有手势相关的临时视觉反馈。关键设计要点：

1. **独立性**：反馈面板不嵌入输入面板或按键面板，而是独立的 Composable，可以自由放置在布局的任意位置
2. **多实例**：可以创建多个反馈面板实例，每个实例绘制不同类型的反馈元素，放置在不同的位置
3. **配置性**：每个反馈面板实例通过 `FeedbackElements` 配置其绘制的反馈类型，从而在不同布局模式下灵活分配反馈到对应区域
4. **状态驱动**：反馈面板从 `GestureFeedbackState` 读取数据，不直接与输入面板或按键面板交互

### 6.2 反馈元素类型

```kotlin
/**
 * 反馈元素类型。
 *
 * 每种类型对应反馈面板可绘制的一种视觉反馈。
 * 反馈面板实例通过配置要绘制的元素类型集合，
 * 决定其职责范围。
 */
enum class FeedbackElementType {
    /**
     * 触摸轨迹。
     *
     * 跟随手指移动的原始路径，由 GestureFeedbackState.touchTrailPoints 驱动。
     * 在叠加模式下，轨迹覆盖整个键盘区域；
     * 在分离模式下，轨迹在输入面板侧绘制。
     */
    TouchTrail,

    /**
     * 按键高亮。
     *
     * 当前手指按下的按键的临时高亮效果（如光圈），
     * 由 GestureFeedbackState.pressedKeys 驱动。
     * 始终在按键面板侧绘制，因为高亮需要与按键位置对齐。
     */
    KeyHighlight,

    /**
     * 按键间路径。
     *
     * 滑行时按键间的逻辑连接线（从起始键到当前键的贝塞尔曲线），
     * 由 GestureFeedbackState.keyPath 驱动。
     * 在按键面板侧绘制，路径连接的是按键的中心位置。
     */
    KeyPath,

    /**
     * X-Pad 路径高亮。
     *
     * X-Pad 模式下手势经过的区域高亮，
     * 由 GestureFeedbackState.xPadPath 驱动。
     * 在按键面板侧绘制，与 X-Pad 的六边形区域对齐。
     */
    XPadPathHighlight,

    /**
     * 手指指示器。
     *
     * 程序化输入动画的虚拟手指，由 GestureFeedbackState.fingerIndicator 驱动。
     * 在叠加模式下覆盖整个区域；
     * 在分离模式下可在输入面板侧或按键面板侧绘制。
     */
    FingerIndicator,
}
```

### 6.3 GestureFeedbackPanel

```kotlin
/**
 * 反馈面板，独立的透明绘制层。
 *
 * 根据 GestureFeedbackState 绘制指定类型的手势视觉反馈。
 * 反馈面板不处理任何触摸事件，不依赖输入面板或按键面板的 Canvas。
 *
 * 支持多实例：不同的 GestureFeedbackPanel 实例可以绘制不同的反馈元素，
 * 放置在不同的位置（如输入面板侧或按键面板侧）。
 *
 * @param elements 该实例绘制的反馈元素类型集合
 * @param feedbackState 手势反馈状态
 * @param keyPanelLayout 按键面板布局信息（用于将按键语义标识映射到屏幕坐标）
 */
@Composable
fun GestureFeedbackPanel(
    elements: Set<FeedbackElementType>,
    feedbackState: GestureFeedbackState,
    keyPanelLayout: KeyGridPanelLayoutInfo,
    modifier: Modifier = Modifier,
) {
    if (elements.isEmpty()) return

    val touchTrailPoints by feedbackState.touchTrailPoints.collectAsState()
    val pressedKeys by feedbackState.pressedKeys.collectAsState()
    val keyPath by feedbackState.keyPath.collectAsState()
    val xPadPath by feedbackState.xPadPath.collectAsState()
    val fingerIndicator by feedbackState.fingerIndicator.collectAsState()

    Canvas(modifier = modifier.fillMaxSize()) {
        // 触摸轨迹
        if (FeedbackElementType.TouchTrail in elements && touchTrailPoints.size >= 2) {
            drawTouchTrail(touchTrailPoints)
        }

        // 按键高亮
        if (FeedbackElementType.KeyHighlight in elements && pressedKeys.isNotEmpty()) {
            drawKeyHighlights(pressedKeys, keyPanelLayout)
        }

        // 按键间路径
        if (FeedbackElementType.KeyPath in elements && keyPath.size >= 2) {
            drawKeyPath(keyPath, keyPanelLayout)
        }

        // X-Pad 路径高亮
        if (FeedbackElementType.XPadPathHighlight in elements && xPadPath.isNotEmpty()) {
            drawXPadPathHighlight(xPadPath, keyPanelLayout)
        }

        // 手指指示器
        if (FeedbackElementType.FingerIndicator in elements && fingerIndicator != null) {
            drawFingerIndicator(fingerIndicator!!)
        }
    }
}
```

### 6.4 反馈绘制实现

```kotlin
// --- GestureFeedbackPanel 内的绘制辅助方法 ---

/**
 * 绘制触摸轨迹。
 *
 * 在手指移动路径上绘制平滑的贝塞尔曲线，
 * 半透明蓝色，手势结束后由 feedbackState 自动清除。
 */
private fun DrawScope.drawTouchTrail(points: List<Offset>) {
    val path = Path().apply {
        moveTo(points.first())
        for (i in 1 until points.size) {
            quadraticBezierTo(
                points[i - 1],
                midpoint(points[i - 1], points[i]),
            )
        }
    }

    drawPath(
        path = path,
        color = Color(0xFF2196F3).copy(alpha = 0.6f),
        style = Stroke(
            width = 4.dp.toPx(),
            cap = StrokeCap.Round,
        ),
    )
}

/**
 * 绘制按键高亮。
 *
 * 在当前按下的按键位置绘制半透明光圈，
 * 提供视觉上的按下反馈。按键位置通过
 * keyPanelLayout 从语义标识映射到屏幕坐标。
 */
private fun DrawScope.drawKeyHighlights(
    keys: Set<InputKey>,
    keyPanelLayout: KeyGridPanelLayoutInfo,
) {
    keys.forEach { key ->
        val rect = keyPanelLayout.keyPositions[key] ?: return@forEach
        drawRoundRect(
            color = Color(0xFF2196F3).copy(alpha = 0.3f),
            topLeft = rect.topLeft,
            size = rect.size,
            cornerRadius = CornerRadius(4.dp.toPx()),
        )
    }
}

/**
 * 绘制按键间的逻辑路径。
 *
 * 在滑行输入时，绘制从起始按键到当前按键的
 * 贝塞尔曲线连接线。曲线通过每个途经按键的中心。
 */
private fun DrawScope.drawKeyPath(
    keys: List<InputKey>,
    keyPanelLayout: KeyGridPanelLayoutInfo,
) {
    val centers = keys.mapNotNull { keyPanelLayout.keyPositions[it]?.center }
    if (centers.size < 2) return

    val path = Path().apply {
        moveTo(centers.first())
        for (i in 1 until centers.size) {
            val from = centers[i - 1]
            val to = centers[i]
            // 使用二次贝塞尔曲线连接相邻按键
            val controlPoint = Offset(
                (from.x + to.x) / 2,
                (from.y + to.y) / 2 - 20f, // 轻微上弯
            )
            quadraticBezierTo(controlPoint, to)
        }
    }

    drawPath(
        path = path,
        color = Color(0xFF2196F3).copy(alpha = 0.5f),
        style = Stroke(
            width = 3.dp.toPx(),
            cap = StrokeCap.Round,
        ),
    )
}

/**
 * 绘制 X-Pad 路径高亮。
 *
 * 高亮手势经过的六边形区域，颜色更鲜明，
 * 表明这些区域已被选中。
 */
private fun DrawScope.drawXPadPathHighlight(
    zones: List<XPadZone>,
    keyPanelLayout: KeyGridPanelLayoutInfo,
) {
    val xPadInfo = keyPanelLayout.xPadLayoutInfo ?: return

    zones.forEach { zone ->
        val center = xPadInfo.grid.axialToPixel(zone.axial)
        val vertices = xPadInfo.grid.hexVertices(center)

        val hexPath = Path().apply {
            moveTo(vertices[0])
            for (i in 1..5) lineTo(vertices[i])
            close()
        }

        drawPath(
            path = hexPath,
            color = Color(0xFF2196F3).copy(alpha = 0.3f),
        )
    }
}

/**
 * 绘制手指指示器（程序化输入动画用）。
 *
 * 半透明圆形，跟随指定位置移动，
 * 按下时缩小并加深颜色。
 */
private fun DrawScope.drawFingerIndicator(state: FingerIndicatorState) {
    if (!state.visible) return

    val radius = 24.dp.toPx()
    val scale = if (state.pressed) 0.8f else 1.0f

    // 阴影
    drawCircle(
        color = Color.Black.copy(alpha = 0.2f),
        radius = radius * scale + 4.dp.toPx(),
        center = Offset(state.position.x, state.position.y + 2.dp.toPx()),
    )

    // 手指圆圈
    drawCircle(
        color = if (state.pressed) {
            Color(0xFF2196F3).copy(alpha = 0.6f)
        } else {
            Color(0xFF2196F3).copy(alpha = 0.3f)
        },
        radius = radius * scale,
        center = state.position,
    )

    // 中心点
    drawCircle(
        color = Color.White.copy(alpha = 0.8f),
        radius = 4.dp.toPx(),
        center = state.position,
    )
}

private fun midpoint(a: Offset, b: Offset): Offset =
    Offset((a.x + b.x) / 2, (a.y + b.y) / 2)
```

### 6.5 反馈面板的多实例配置

不同的布局模式下，反馈面板实例的配置不同。通过 `GestureFeedbackPanelSet` 封装当前布局模式下的反馈面板组合：

```kotlin
/**
 * 反馈面板配置集。
 *
 * 定义当前布局模式下所需的反馈面板实例及其元素类型分配。
 * 不同布局模式有不同的配置集。
 */
sealed class GestureFeedbackPanelSet {

    /**
     * 叠加模式配置：单个反馈面板实例绘制所有反馈。
     *
     * 在叠加模式下，三层面板完全重叠，
     * 一个反馈面板即可覆盖所有反馈绘制。
     */
    data object OverlaySet : GestureFeedbackPanelSet() {
        val allElements: Set<FeedbackElementType> = setOf(
            FeedbackElementType.TouchTrail,
            FeedbackElementType.KeyHighlight,
            FeedbackElementType.KeyPath,
            FeedbackElementType.XPadPathHighlight,
            FeedbackElementType.FingerIndicator,
        )
    }

    /**
     * 全屏分离模式配置：两个反馈面板实例。
     *
     * 输入区域侧：绘制触摸轨迹和手指指示器
     * 按键区域侧：绘制按键高亮、按键路径和 X-Pad 路径高亮
     */
    data class FullScreenSet(
        val inputSideElements: Set<FeedbackElementType> = setOf(
            FeedbackElementType.TouchTrail,
            FeedbackElementType.FingerIndicator,
        ),
        val keySideElements: Set<FeedbackElementType> = setOf(
            FeedbackElementType.KeyHighlight,
            FeedbackElementType.KeyPath,
            FeedbackElementType.XPadPathHighlight,
        ),
    ) : GestureFeedbackPanelSet()
}
```

---

## 7. 按键面板设计

### 7.1 KeyGridPanel

按键面板与之前设计相同，纯展示层，不处理触摸事件，也不绘制手势反馈：

```kotlin
/**
 * 按键面板，纯展示层。
 *
 * 根据 ImeState 渲染按键布局和状态，不处理任何触摸事件，
 * 也不绘制手势视觉反馈（反馈由独立的 GestureFeedbackPanel 负责）。
 * 通过 onLayoutInfoChanged 回调向输入面板和反馈面板提供布局信息。
 *
 * 按键面板的视觉职责：
 * - 渲染按键的常规外观（标签、背景、边框）
 * - 渲染按键的持续性状态（按下态、激活态、禁用态）
 *   这些状态由 KeyboardState 驱动，非手势触发的临时反馈
 * - 不渲染任何临时性手势反馈（滑行轨迹、临时高亮等）
 */
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

### 7.2 StandardKeyGridPanel

```kotlin
/**
 * 标准按键面板。
 *
 * 渲染网格布局的按键，通过 onGloballyPositioned 收集每个按键的位置信息，
 * 通过 onLayoutInfoChanged 回调通知输入面板和反馈面板。
 */
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

### 7.3 KeyView（无触摸处理，无手势反馈）

按键面板中的 `KeyView` 不处理触摸事件，也不绘制手势反馈，只负责按键的常规状态渲染：

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
```

### 7.4 KeyGridPanelLayoutInfo

```kotlin
/**
 * 按键面板布局信息。
 *
 * 由按键面板通过 Compose 布局系统收集，提供给输入面板和反馈面板。
 * 每次 Compose 重组后自动更新，确保与实际渲染位置一致。
 * 反馈面板需要此信息将按键的语义标识映射到屏幕坐标，
 * 以便在正确的位置绘制按键高亮和路径。
 */
data class KeyGridPanelLayoutInfo(
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

## 8. 组合布局

### 8.1 叠加模式（KeyboardPanel）

叠加模式下，`KeyboardPanel` 是完整的输入法组件，包含候选栏、输入栏、工具栏和三层面板叠加区域。三层面板直接在 `KeyboardPanel` 中叠加（无中间包装层），由 `KeyboardPanel` 直接组合。底层按键渲染、中层反馈绘制、顶层手势拦截，三层面板完全重叠：

```kotlin
// KeyboardPanel 内部的完整结构（详见文档 160 §5.4）
@Composable
fun KeyboardPanel(
    engine: ImeEngine,
    modifier: Modifier = Modifier,
) {
    val state by engine.state.collectAsStateWithLifecycle()
    val feedbackState = remember { GestureFeedbackState() }
    var keyPanelLayout by remember { mutableStateOf(KeyGridPanelLayoutInfo()) }

    KeyboardTheme(themeType = state.config.ui.themeType) {
        Column(modifier = modifier) {
            // 候选栏
            CandidateListPanel(
                state = state.candidates,
                onCandidateSelected = { candidate ->
                    engine.handleIntent(ImeIntent.SelectCandidate(candidate))
                },
            )

            // 输入栏
            InputListPanel(
                state = state.inputList,
                onGapTapped = { index ->
                    engine.handleIntent(ImeIntent.MoveCursorTo(index))
                },
            )

            // 三层面板叠加区域
            Box {
                // 底层：按键面板（纯展示）
                KeyGridPanel(
                    keyboardType = state.keyboardType,
                    keyGrid = state.keyGrid,
                    keyboardState = state.keyboardState,
                    onLayoutInfoChanged = { keyPanelLayout = it },
                )

                // 中层：反馈面板（透明视觉反馈层）
                GestureFeedbackPanel(
                    elements = GestureFeedbackPanelSet.OverlaySet.allElements,
                    feedbackState = feedbackState,
                    keyPanelLayout = keyPanelLayout,
                )

                // 顶层：输入面板（透明手势层，最上层确保触摸优先到达）
                GestureInputPanel(
                    keyPanelLayout = keyPanelLayout,
                    keyboardType = state.keyboardType,
                    feedbackState = feedbackState,
                    onGesture = { engine.handleGesture(it) },
                )
            }

            // 工具栏
            Toolbar(
                keyboardType = state.keyboardType,
                config = state.config,
                onSwitchKeyboard = { engine.handleIntent(ImeIntent.SwitchKeyboard(it)) },
            )
        }
    }
}
```

### 8.2 全屏分离模式（KeyboardScreen）

全屏分离模式由 `KeyboardScreen` 实现，它是完整的输入法组件，包含候选栏、输入栏、工具栏和分离布局的键盘区域。输入面板和按键面板不再重叠，反馈面板的多实例分别叠加在各自区域（输入面板始终在最上层拦截触摸）：

```kotlin
// KeyboardScreen 的三层面板分离结构（详见文档 160 §5.4b）
@Composable
fun KeyboardScreen(
    engine: ImeEngine,
    modifier: Modifier = Modifier,
    inputPanelRatio: Float = 0.5f,
) {
    val state by engine.state.collectAsStateWithLifecycle()
    val feedbackState = remember { GestureFeedbackState() }
    var keyPanelLayout by remember { mutableStateOf(KeyGridPanelLayoutInfo()) }

    KeyboardTheme(themeType = state.config.ui.themeType) {
        Column(modifier = modifier.fillMaxSize()) {
            // 上半区：按键区域
            Box(modifier = Modifier.fillMaxWidth().weight(1f - inputPanelRatio)) {
                // 底层：按键侧反馈面板（按键高亮、按键路径、X-Pad 高亮）
                GestureFeedbackPanel(
                    elements = GestureFeedbackPanelSet.FullScreenSet.keySideElements,
                    feedbackState = feedbackState,
                    keyPanelLayout = keyPanelLayout,
                )
                // 顶层：按键面板
                KeyGridPanel(
                    keyboardType = state.keyboardType,
                    keyGrid = state.keyGrid,
                    keyboardState = state.keyboardState,
                    onLayoutInfoChanged = { keyPanelLayout = it },
                )
                // 注意：按键区域不需要输入面板，触摸由下半区的输入面板统一处理
            }

            // 下半区：输入区域
            Box(modifier = Modifier.fillMaxWidth().weight(inputPanelRatio)) {
                // 底层：输入侧反馈面板（触摸轨迹、手指指示器）
                GestureFeedbackPanel(
                    elements = GestureFeedbackPanelSet.FullScreenSet.inputSideElements,
                    feedbackState = feedbackState,
                    keyPanelLayout = keyPanelLayout,
                )

                // 顶层：输入面板（最上层拦截触摸）
                GestureInputPanel(
                    keyPanelLayout = keyPanelLayout,
                    keyboardType = state.keyboardType,
                    feedbackState = feedbackState,
                    onGesture = { engine.handleGesture(it) },
                )
            }

            // 候选栏 + 输入栏 + 工具栏
            CandidateListPanel(...)
            InputListPanel(...)
            Toolbar(...)
        }
    }
}
```

### 8.3 分离模式下的坐标映射

全屏分离模式需要处理反馈面板中触摸轨迹的坐标映射。在叠加模式下，触摸轨迹的坐标与按键面板坐标一致。在分离模式下，输入面板的触摸坐标需要映射到按键面板的坐标空间：

```kotlin
sealed class LayoutMode {
    data object Overlay : LayoutMode() {
        // 叠加模式：无需映射
        // 反馈面板直接使用 GestureFeedbackState 中的坐标
    }

    data class FullScreen(
        val inputPanelRatio: Float = 0.5f,
    ) : LayoutMode() {
        /**
         * 将输入面板坐标系下的触摸轨迹映射到按键面板坐标系。
         *
         * 在分离模式下，反馈面板的按键侧实例需要将触摸轨迹
         * 从输入面板的坐标空间映射到按键面板的坐标空间，
         * 以便在按键面板上正确绘制轨迹。
         */
        fun mapTrailToKeyGridPanel(
            trail: List<Offset>,
            inputPanelBounds: Rect,
            keyPanelBounds: Rect,
        ): List<Offset> {
            return trail.map { point ->
                Offset(
                    x = (point.x / inputPanelBounds.width) * keyPanelBounds.width,
                    y = (point.y / inputPanelBounds.height) * keyPanelBounds.height,
                )
            }
        }
    }
}
```

### 8.4 KeyboardPanel / KeyboardScreen 集成

`KeyboardPanel` 和 `KeyboardScreen` 均为完整的输入法组件，包含候选栏、输入栏、工具栏和键盘区域，二者只是形式和交互上存在差异。叠加模式由 `KeyboardPanel` 实现，三层面板直接叠加，无中间包装层；全屏模式由 `KeyboardScreen` 实现，手势输入面板与按键面板分离。详见文档 160 第 5.4/5.4b 节。

**叠加模式（KeyboardPanel）**：

```kotlin
@Composable
fun KeyboardPanel(
    engine: ImeEngine,
    modifier: Modifier = Modifier,
) {
    val state by engine.state.collectAsStateWithLifecycle()
    val feedbackState = remember { GestureFeedbackState() }
    var keyPanelLayout by remember { mutableStateOf(KeyGridPanelLayoutInfo()) }

    KeyboardTheme(themeType = state.config.ui.themeType) {
        Column(modifier = modifier) {
            // 候选栏
            CandidateListPanel(
                state = state.candidates,
                onCandidateSelected = { candidate ->
                    engine.handleIntent(ImeIntent.SelectCandidate(candidate))
                },
            )

            // 输入栏
            InputListPanel(
                state = state.inputList,
                onGapTapped = { index ->
                    engine.handleIntent(ImeIntent.MoveCursorTo(index))
                },
            )

            // 三层面板叠加区域
            Box {
                // 底层：按键面板
                KeyGridPanel(
                    keyboardType = state.keyboardType,
                    keyGrid = state.keyGrid,
                    keyboardState = state.keyboardState,
                    onLayoutInfoChanged = { keyPanelLayout = it },
                )

                // 中层：反馈面板（叠加模式：单个实例绘制所有反馈）
                GestureFeedbackPanel(
                    elements = GestureFeedbackPanelSet.OverlaySet.allElements,
                    feedbackState = feedbackState,
                    keyPanelLayout = keyPanelLayout,
                )

                // 顶层：输入面板
                GestureInputPanel(
                    keyPanelLayout = keyPanelLayout,
                    keyboardType = state.keyboardType,
                    feedbackState = feedbackState,
                    onGesture = { engine.handleGesture(it) },
                )
            }

            // 工具栏
            Toolbar(
                keyboardType = state.keyboardType,
                config = state.config,
                onSwitchKeyboard = { engine.handleIntent(ImeIntent.SwitchKeyboard(it)) },
            )
        }
    }
}
```

**全屏模式（KeyboardScreen）**：

```kotlin
@Composable
fun KeyboardScreen(
    engine: ImeEngine,
    modifier: Modifier = Modifier,
    inputPanelRatio: Float = 0.5f,
) {
    val state by engine.state.collectAsStateWithLifecycle()
    val feedbackState = remember { GestureFeedbackState() }
    var keyPanelLayout by remember { mutableStateOf(KeyGridPanelLayoutInfo()) }

    KeyboardTheme(themeType = state.config.ui.themeType) {
        Column(modifier = modifier.fillMaxSize()) {
            // 上半区：按键区域
            Box(modifier = Modifier.fillMaxWidth().weight(1f - inputPanelRatio)) {
                // 底层：反馈面板（按键侧）
                GestureFeedbackPanel(
                    elements = GestureFeedbackPanelSet.FullScreenSet.keySideElements,
                    feedbackState = feedbackState,
                    keyPanelLayout = keyPanelLayout,
                )
                // 顶层：按键面板
                KeyGridPanel(
                    keyboardType = state.keyboardType,
                    keyGrid = state.keyGrid,
                    keyboardState = state.keyboardState,
                    onLayoutInfoChanged = { keyPanelLayout = it },
                )
            }

            // 下半区：输入区域
            Box(modifier = Modifier.fillMaxWidth().weight(inputPanelRatio)) {
                // 底层：反馈面板（输入侧）
                GestureFeedbackPanel(
                    elements = GestureFeedbackPanelSet.FullScreenSet.inputSideElements,
                    feedbackState = feedbackState,
                    keyPanelLayout = keyPanelLayout,
                )
                // 顶层：输入面板
                GestureInputPanel(
                    keyPanelLayout = keyPanelLayout,
                    keyboardType = state.keyboardType,
                    feedbackState = feedbackState,
                    onGesture = { engine.handleGesture(it) },
                )
            }

            // 候选栏 + 输入栏 + 工具栏
            CandidateListPanel(...)
            InputListPanel(...)
            Toolbar(...)
        }
    }
}
```

---

## 9. 与坐标无关输入动作程序化的协作

文档 930 的 `KeyPositionResolver` 与本文档的 `KeyGridPanelLayoutInfo` 和 `GestureFeedbackState` 协作：

### 9.1 KeyGridPanelPositionResolver

复用按键面板的布局信息，供输入动作程序化系统在回放时动态查询按键位置：

```kotlin
/**
 * 基于 KeyGridPanelLayoutInfo 实现的坐标解析器。
 *
 * 复用按键面板的布局信息，供输入动作程序化系统在回放时
 * 动态查询按键位置。
 */
class KeyGridPanelPositionResolver(
    private val layoutProvider: () -> KeyGridPanelLayoutInfo,
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

### 9.2 InputActionPlayer 与 GestureFeedbackState 的协作

程序化输入动画的 `InputActionPlayer` 通过 `GestureFeedbackState` 驱动反馈面板，与真实用户手势共享同一套反馈状态和反馈面板：

```kotlin
/**
 * InputActionPlayer 通过 GestureFeedbackState 驱动反馈面板。
 *
 * 当程序化输入播放时，播放器更新 GestureFeedbackState 的
 * fingerIndicator、keyPath 等状态，反馈面板自动响应。
 * 这意味着程序化输入动画的视觉反馈与用户真实手势的视觉反馈
 * 使用相同的反馈面板实例，无需额外配置。
 */
class InputActionPlayer(
    private val viewModel: KeyboardViewModel,
    private val feedbackState: GestureFeedbackState,
    private val positionResolver: KeyPositionResolver,
    private val scope: CoroutineScope,
) {
    // ... 播放逻辑 ...

    private fun executeAction(action: InputAction) {
        when (action) {
            is InputAction.KeyDown -> {
                val position = positionResolver.resolve(action.key) ?: return
                // 通过 feedbackState 驱动反馈面板
                feedbackState.setFingerIndicator(
                    FingerIndicatorState(position = position, pressed = true)
                )
                feedbackState.setPressedKeys(setOf(action.key))
                viewModel.handleIntent(ImeIntent.PressKey(action.key, KeyGesture.Tap))
            }
            is InputAction.SwipeTo -> {
                val fromPosition = positionResolver.resolve(action.fromKey) ?: return
                val toPosition = positionResolver.resolve(action.toKey) ?: return

                // 更新手指指示器位置和按键路径
                feedbackState.setKeyPath(listOf(action.fromKey, action.toKey))
                feedbackState.setPressedKeys(setOf(action.toKey))

                // 沿贝塞尔曲线动画移动手指指示器
                val path = SwipePathInterpolator.interpolate(fromPosition, toPosition)
                scope.launch {
                    animateFingerAlongPath(path, action.duration)
                }
                viewModel.handleIntent(ImeIntent.PressKey(action.toKey, KeyGesture.Swipe))
            }
            is InputAction.KeyUp -> {
                val position = positionResolver.resolve(action.key)
                if (position != null) {
                    feedbackState.setFingerIndicator(
                        FingerIndicatorState(position = position, pressed = false)
                    )
                }
                feedbackState.clearPressedKeys()
            }
            // ... 其他动作 ...
        }
    }

    private suspend fun animateFingerAlongPath(path: List<Offset>, durationMs: Long) {
        if (path.size < 2) return
        val stepDuration = durationMs / (path.size - 1)
        for (i in 1 until path.size) {
            val animatable = Animatable(0f)
            animatable.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = stepDuration.toInt()),
            ) {
                val from = path[i - 1]
                val to = path[i]
                feedbackState.setFingerIndicator(
                    FingerIndicatorState(
                        position = Offset(
                            from.x + (to.x - from.x) * value,
                            from.y + (to.y - from.y) * value,
                        ),
                        pressed = true,
                    )
                )
            }
        }
    }
}
```

这样，输入动作程序化系统与三层面板分离架构共享同一套 `GestureFeedbackState` 和 `GestureFeedbackPanel`，无需维护独立的视觉反馈逻辑。

---

## 10. X-Pad 的特殊处理

### 10.1 三层面板在 X-Pad 模式下的分工

Java 版本中 `XPadView` 自行处理触摸事件和手势检测。三层分离后，X-Pad 的各职责明确分配到三个面板：

| 职责 | Java 版本（XPadView 内） | v4（三层分离后） |
|------|--------------------------|-----------------|
| 六边形绘制 | XPadView.onDraw() | KeyGridPanel XPadKeyContent |
| 区域计算 | XPadView.findBlockAt() | KeyGridPanelLayoutInfo.findXPadZoneAt() |
| 触摸检测 | XPadView.onTouchEvent() | GestureInputPanel GestureDetectorLayer |
| 路径追踪 | XPadState.BlockData | InputGesture.XPadZonePath |
| 区域高亮 | XPadView.onDraw()（状态驱动） | GestureFeedbackPanel.drawXPadPathHighlight() |
| 触摸轨迹 | 无（X-Pad 不绘制轨迹） | GestureFeedbackPanel.drawTouchTrail() |
| 状态更新 | XPadView → IMEditor | InputGesture → ViewModel → State → KeyGridPanel |

### 10.2 X-Pad 按键面板渲染

X-Pad 作为按键面板的一种渲染模式，仍使用 Compose Canvas 绘制六边形网格，但不处理触摸，也不绘制手势高亮：

```kotlin
/**
 * X-Pad 按键内容（按键面板内）。
 *
 * 仅绘制六边形网格的静态布局和标签，
 * 不绘制手势反馈（如区域高亮、路径），
 * 不处理触摸事件。
 * 手势高亮由 GestureFeedbackPanel 的 XPadPathHighlight 元素绘制，
 * 触摸检测由 GestureInputPanel 的 handleXPadGesture 负责。
 */
@Composable
fun XPadKeyContent(
    key: InputKey.XPad,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            // 绘制六边形网格和区域标签（静态布局）
            // 不绘制任何手势反馈
        }
    }
}
```

### 10.3 X-Pad 反馈面板绘制

X-Pad 模式的手势高亮由反馈面板负责，与标准按键模式的反馈绘制复用同一个 `GestureFeedbackPanel` 实例（叠加模式下）：

- `FeedbackElementType.XPadPathHighlight`：高亮手势经过的六边形区域
- `FeedbackElementType.TouchTrail`：绘制手指在 X-Pad 上的移动轨迹
- `FeedbackElementType.KeyHighlight`：当前按下的 X-Pad 区域的额外高亮（如果需要）

---

## 11. 三层面板的数据流总结

```
用户手指
   │
   ▼
┌──────────────────────────────────┐
│         GestureInputPanel               │
│  拦截触摸 → 识别手势              │
│  输出: InputGesture → ViewModel  │
│  更新: GestureFeedbackState      │
└──────┬───────────────┬───────────┘
       │               │
       ▼               ▼
┌──────────────┐  ┌──────────────────────┐
│   ViewModel  │  │  GestureFeedbackState │
│  reduce()    │  │  (独立反馈状态)        │
│  → ImeState  │  │  - touchTrailPoints  │
└──────┬───────┘  │  - pressedKeys       │
       │          │  - keyPath           │
       │          │  - xPadPath          │
       │          │  - fingerIndicator   │
       │          └──────────┬───────────┘
       │                     │
       ▼                     ▼
┌──────────────┐    ┌──────────────────┐
│   KeyGridPanel   │    │  GestureFeedbackPanel   │
│  按键渲染    │    │  反馈绘制         │
│  ImeState → │    │  FeedbackState → │
│  重组        │    │  Canvas 绘制      │
└──────────────┘    └──────────────────┘
```

**关键数据流路径**：

1. **手势 → 逻辑**：`GestureInputPanel → InputGesture → ViewModel → ImeIntent → reduce() → ImeState`
2. **手势 → 反馈**：`GestureInputPanel → GestureFeedbackState → GestureFeedbackPanel → Canvas 绘制`
3. **状态 → 渲染**：`ImeState → KeyGridPanel → Compose 重组`
4. **程序化输入 → 反馈**：`InputActionPlayer → GestureFeedbackState → GestureFeedbackPanel → Canvas 绘制`

路径 2 和路径 3 的分离是三层架构的核心优势：高频的手势反馈更新通过路径 2 直接驱动反馈面板，不触发按键面板的重组；按键面板只在 ImeState 变更时重组，重组频率远低于手势反馈。

---

## 12. 后续扩展

### 12.1 全屏输入模式（后续版本）

叠加模式是基础，全屏输入模式是重要扩展方向。在全屏输入模式下，输入面板和按键面板不再重叠，反馈面板的多实例分别叠加在各区域（输入面板始终在最上层拦截触摸）：

```
┌─────────────────────────────┐
│     按键区域（上半区）          │
│     ┌───────────────────┐    │
│     │  GestureFeedbackPanel     │    │  ← 按键侧反馈（高亮、路径）
│     │  (keySideElements) │    │  ← 上层
│     ├───────────────────┤    │
│     │  KeyGridPanel          │    │  ← 按键渲染，下层
│     │  Q  W  E  R  T     │    │
│     │  A  S  D  F  G     │    │
│     │  Z  X  C  V  B     │    │
│     └───────────────────┘    │
│                              │
├──────────────────────────────┤
│     输入区域（下半区）         │
│     ┌───────────────────┐    │
│     │  GestureInputPanel        │    │  ← 手势拦截，最上层
│     ├───────────────────┤    │
│     │  GestureFeedbackPanel     │    │  ← 输入侧反馈（轨迹、指示器）
│     │  (inputSideElements)│   │  ← 下层
│     └───────────────────┘    │
└──────────────────────────────┘
```

由于三层面板已经分离，全屏模式只需要：
1. 新增 `LayoutMode.FullScreen` 定义
2. 新增 `GestureFeedbackPanelSet.FullScreenSet` 配置
3. 修改 `KeyboardPanel` 的组合逻辑（不再叠加，改为上下布局），或使用 `KeyboardScreen`（全屏模式，同样是完整输入法组件）
4. 输入面板的按键查找增加坐标映射（输入面板坐标 → 按键面板坐标）
5. 反馈面板的触摸轨迹增加坐标映射（输入面板坐标 → 按键面板坐标，供按键侧反馈面板使用）

当前版本不实现全屏模式，但架构预留了所有扩展点。

### 12.2 反馈样式自定义

后续版本可在配置系统中添加反馈样式选项，如轨迹颜色、轨迹宽度、高亮透明度、手指指示器大小等。这些配置项通过 `GestureFeedbackState` 传递给 `GestureFeedbackPanel`，不影响输入面板和按键面板。

### 12.3 多点触控反馈

当前设计假设单点触控。如果后续支持多点触控（如双手输入），`GestureFeedbackState` 需要扩展为支持多指轨迹和多个手指指示器。反馈面板的多实例能力天然支持多点触控——不同手指的反馈可以分配到不同的反馈面板实例。

---

## 13. Java 功能完整对照

| Java 组件 | v4 三层分离后对应 | 说明 |
|-----------|-----------------|------|
| `KeyboardViewGestureListener` | `GestureInputPanel.GestureDetectorLayer` | 手势检测从按键面板移至输入面板 |
| `KeyboardView`（触摸事件） | `GestureInputPanel`（透明手势层） | 触摸事件接收者分离 |
| `KeyboardView`（按键渲染） | `KeyGridPanel`（纯展示层） | 按键渲染独立，无触摸处理 |
| `KeyboardView`（按键状态动画） | `KeyGridPanel`（KeyboardState 驱动） | 持续性状态渲染保留在按键面板 |
| `RecyclerViewGestureTrailer` | `GestureFeedbackPanel.drawTouchTrail()` | 轨迹绘制从 ItemDecoration 移至独立反馈面板 |
| `KeyboardViewKeyAnimator`（临时高亮） | `GestureFeedbackPanel.drawKeyHighlights()` | 临时性手势高亮移至反馈面板 |
| `XPadView.onTouchEvent()` | `GestureInputPanel.handleXPadGesture()` | X-Pad 手势由输入面板统一处理 |
| `XPadView.onDraw()`（静态布局） | `KeyGridPanel.XPadKeyContent` | X-Pad 静态绘制保留在按键面板 |
| `XPadView.onDraw()`（区域高亮） | `GestureFeedbackPanel.drawXPadPathHighlight()` | X-Pad 手势高亮移至反馈面板 |
| `findVisibleKeyViewHolderUnderLoose()` | `KeyGridPanelLayoutInfo.findKeyAt()` | 按键查找改为查询布局信息 |
| `XPadView.findBlockAt()` | `KeyGridPanelLayoutInfo.findXPadZoneAt()` | X-Pad 区域查找改为查询布局信息 |
| `KeyViewHolder.touchDown/Up()` | `KeyboardState` 驱动 + `GestureFeedbackPanel` | 状态驱动 + 独立反馈 |
| N/A | `InputGesture` | 新增：逻辑手势抽象层 |
| N/A | `KeyGridPanelLayoutInfo` | 新增：按键面板布局信息暴露 |
| N/A | `GestureFeedbackState` | 新增：独立反馈状态，解耦反馈与面板 |
| N/A | `GestureFeedbackPanel` | 新增：独立反馈面板，支持多实例 |
| N/A | `FeedbackElementType` | 新增：反馈元素类型，配置反馈面板实例职责 |
| N/A | `GestureFeedbackPanelSet` | 新增：反馈面板配置集，按布局模式分配反馈 |

---

## 14. 与其他系统的协作

| 协作系统 | 协作方式 |
|----------|----------|
| 键盘状态机（100） | `InputGesture` 经 ViewModel 转换为 `ImeIntent`，驱动状态机；状态变更更新 `KeyboardState`，按键面板据此重组；反馈面板不消费 `KeyboardState` |
| UI Compose 迁移（400） | `KeyboardPanel` 为完整输入法组件（合并原 KeyboardArea + 候选栏 + 输入栏 + 工具栏）；`KeyView` 移除 `combinedClickable` 和手势反馈逻辑 |
| X-Pad 重构（700） | X-Pad 的手势检测从 `XPadView` 移至输入面板，静态绘制保留在按键面板，区域高亮移至反馈面板 |
| 输入动作程序化（930） | `KeyGridPanelPositionResolver` 复用 `KeyGridPanelLayoutInfo`，共享按键位置查询；`InputActionPlayer` 通过 `GestureFeedbackState` 驱动反馈面板，与真实手势共享反馈基础设施 |
| 配置系统（500） | `LayoutMode` 和 `GestureFeedbackPanelSet` 可作为配置项持久化（当前仅有 Overlay）；反馈样式配置（颜色、宽度等）可扩展 |
| 配置界面改进（920） | 后续版本可在设置中提供布局模式切换入口和反馈样式自定义入口 |
