# 面板三层分离与屏幕布局

本文档定义 kuaizi IME 的面板三层分离架构与屏幕布局模型。键盘 UI 分离为三个独立层：**手势输入面板**（`GestureInputPanel`）负责接收用户手势并识别为输入意图，**按键布局面板**（`KeyLayoutPanel`）负责按键的布局渲染和状态展示，**手势反馈面板**（`GestureFeedbackPanel`）负责手势视觉反馈的绘制。三者通过 `InputGesture`、`GestureFeedbackState` 和 `ImeState` 解耦，互不直接依赖。

反馈面板是独立于输入面板和按键面板的透明层，支持多实例。这种设计使得反馈面板可以灵活地与输入面板、按键面板组合叠加——在堆叠（`Stacked`）布局模式下，三层面板完全重叠；在分离（`Separated`）布局模式下，反馈面板可以分别与输入面板和按键面板叠加，使手指轨迹在输入区域可见、按键高亮在按键区域可见。

屏幕纵向划分为 Zone A（上半区）和 Zone B（下半区），Zone B 内部按三行结构组织面板。`KeyboardInputMode` 与 `KeyboardType` 作为两个独立的正交维度，任意 `KeyboardInputMode` 可与任意 Type 组合，通过 `KeyLayoutStrategy` 分发不同的布局策略。所有坐标数据使用归一化形式存储，绘制时根据面板实际尺寸转换为像素坐标，使得同一份反馈数据可以正确地在不同 Zone 和不同尺寸的面板实例上渲染。

---

## 1 三层分离架构

三层分离是键盘 UI 的核心架构原则，将面板职责划分为三个完全独立的层，每层仅负责一项核心职责，不越界处理其他层的事务：

**三条核心原则**：

1. **`GestureInputPanel` 是唯一的触摸事件接收者**——始终位于最上层，完全透明，确保触摸事件优先到达手势检测层，而非被反馈面板或按键面板拦截。按键面板和反馈面板不处理触摸。
2. **`GestureFeedbackPanel` 是唯一的视觉反馈绘制者**——输入面板不绘制反馈（完全透明），按键面板仅绘制按键的常规状态渲染（非手势触发的临时反馈）。输入面板虽在最上层，但因完全透明，不会遮挡反馈面板的视觉效果。
3. **三者之间的通信通过共享状态解耦**——`GestureInputPanel` → `InputGesture` → `KeyboardViewModel` → `ImeState` → `KeyLayoutPanel`；`GestureInputPanel` → `GestureFeedbackState` → `GestureFeedbackPanel`。

**叠加顺序（bottom→top）**：`KeyLayoutPanel` → `GestureFeedbackPanel` → `GestureInputPanel`

**叠加顺序的设计考量**：为什么输入面板在反馈面板上层而非下层？因为 Compose 的触摸事件从最上层开始向下传播，如果反馈面板在输入面板之上，即使其 Canvas 不主动消费触摸事件，也存在事件被意外拦截的风险（如未来添加交互功能、Compose 版本行为变更等）。将输入面板置于最上层是最可靠的方案——它确保所有触摸事件首先到达手势检测层，同时输入面板完全透明不绘制任何内容，不会视觉遮挡下层的反馈绘制效果。

**按键面板与反馈面板的视觉职责边界**：按键面板根据 `KeyboardState` 渲染按键的**持续性视觉状态**（如按下态、激活态、禁用态、候选选中态），这些状态是由 ViewModel 的 reduce 结果驱动的确定性渲染。反馈面板绘制**临时性手势视觉反馈**（如输入轨迹、按键高亮光圈、手指指示器），这些反馈跟随用户手指实时变化，在手势结束后消失。这种分离确保了按键面板的重组仅由状态变更驱动，而高频的手势反馈更新不会触发按键面板的重组。

---

## 2 `InputGesture` 坐标无关逻辑手势

`InputGesture` 是输入面板识别手势后输出的坐标无关逻辑手势描述。它不包含任何绝对坐标，只包含按键的语义标识，使得手势识别逻辑与面板布局解耦。

| 属性 | 说明 |
|------|------|
| 角色 | 输入面板的手势输出模型 |
| 职责 | 描述用户的输入意图，坐标无关 |
| 约束 | 不包含任何绝对坐标，只包含按键的语义标识；包含 `KeyboardInputMode` 参数以支持不同模式的手势识别策略 |
| 关键属性 | `timestamp`, `Tap`, `LongPress`, `Swipe`, `Flip`, `CandidateTap` |
| 所属包 | gesture |

```kotlin
/**
 * 输入手势，坐标无关。
 *
 * 由输入面板识别后发送到 ViewModel，描述用户的输入意图。
 * 不包含任何绝对坐标，只包含按键的语义标识。
 * 包含 inputMode 参数，使得手势识别逻辑可以根据不同输入模式采用不同策略。
 */
sealed class InputGesture {
    /** 手势开始时刻（毫秒） */
    abstract val timestamp: Long

    /** 产生此手势的输入模式 */
    abstract val inputMode: KeyboardInputMode

    /**
     * 点击按键。
     *
     * @param key 目标按键（由输入面板根据触摸位置查询 KeyLayoutState 确定）
     * @param tick 连续点击同一按键的次数（0=首次，1=双击，2=三击...）
     */
    data class Tap(
        override val timestamp: Long,
        override val inputMode: KeyboardInputMode,
        val key: InputKey,
        val tick: Int = 0,
    ) : InputGesture()

    /**
     * 长按按键。
     */
    data class LongPress(
        override val timestamp: Long,
        override val inputMode: KeyboardInputMode,
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
        override val inputMode: KeyboardInputMode,
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
        override val inputMode: KeyboardInputMode,
        val startKey: InputKey,
        val direction: FlipDirection,
    ) : InputGesture()

    /**
     * 候选项选择。
     */
    data class CandidateTap(
        override val timestamp: Long,
        override val inputMode: KeyboardInputMode,
        val candidateIndex: Int,
    ) : InputGesture()
}

enum class FlipDirection { Left, Right, Up, Down }
```

`InputGesture` 的五种手势类型覆盖了输入法的所有交互场景。`Tap` 是最基本的点击手势，由 `GestureInputPanel` 根据触摸位置查询 `KeyLayoutState` 确定目标按键后生成。`LongPress` 是长按手势，用于触发按键的替代功能（如长按元音键选择声调）。`Swipe` 是滑行输入手势，记录起始按键、结束按键和途经按键序列，适用于拼音滑行输入等场景。`Flip` 是翻转手势，用于快速切换键盘类型或翻页。`CandidateTap` 是候选项选择手势，直接携带候选索引，不经过按键映射。

`InputGesture` 包含 `inputMode: KeyboardInputMode` 参数，使得 `KeyboardViewModel` 可以根据不同输入模式采用不同的手势转换策略。例如，`HexGrid` 模式下的 `Swipe` 手势可能产生与 `RectGrid` 模式不同的 `ImeIntent`，因为六边形网格的滑行路径和矩形网格的滑行路径在语义上有所差异。

---

## 3 `InputGesture` → `ImeIntent` 转换

`InputGesture` 是输入面板的输出，`ImeIntent` 是 ViewModel 的输入。`KeyboardViewModel` 通过 `gestureToIntent()` 方法将 `InputGesture` 转换为 `ImeIntent`，然后委托引擎执行 reduce。

```kotlin
// 在 KeyboardViewModel 中
fun handleGesture(gesture: InputGesture) {
    val intent = gestureToIntent(gesture)
    engine.handleIntent(intent)
}

private fun gestureToIntent(gesture: InputGesture): ImeIntent {
    return when (gesture) {
        is InputGesture.Tap -> ImeIntent.PressKey(gesture.key, KeyGesture.Tap)
        is InputGesture.LongPress -> ImeIntent.PressKey(gesture.key, KeyGesture.LongPress)
        is InputGesture.Swipe -> ImeIntent.PressKey(gesture.endKey, KeyGesture.Swipe)
        is InputGesture.Flip -> ImeIntent.PressKey(
            gesture.startKey,
            KeyGesture.Flip(gesture.direction),
        )
        is InputGesture.CandidateTap -> ImeIntent.SelectCandidate(
            /* 根据 gesture.candidateIndex 从当前候选列表中获取 */
        )
    }
}
```

**完整转换对照表**：

| `InputGesture` 类型 | 转换逻辑 | 生成的 `ImeIntent` | 说明 |
|---------------------|----------|-------------------|------|
| `Tap(key, tick)` | 直接映射 | `PressKey(key, KeyGesture.Tap)` | 点击按键，tick 用于判断多击 |
| `LongPress(key)` | 直接映射 | `PressKey(key, KeyGesture.LongPress)` | 长按按键，触发替代功能 |
| `Swipe(startKey, endKey, visitedKeys, duration)` | 取结束按键 | `PressKey(endKey, KeyGesture.Swipe)` | 滑行输入，途经按键由引擎在 reduce 中处理 |
| `Flip(startKey, direction)` | 起始按键 + 方向 | `PressKey(startKey, KeyGesture.Flip(direction))` | 翻转手势，方向决定具体功能 |
| `CandidateTap(candidateIndex)` | 索引映射 | `SelectCandidate(...)` | 候选项点击选择 |

**两层转换的意义**：

- `InputGesture` 表达「用户做了什么手势」，属于输入面板的领域
- `ImeIntent` 表达「系统应该做什么」，属于 ViewModel/引擎的领域
- 分离使得同一手势可以根据当前键盘状态产生不同的 Intent，例如同一 `Tap` 手势在 `PinyinInput` 状态下产生 `PressKey`，在 `CandidateSelection` 状态下可能产生 `SelectCandidate`
- 分离也使得不同手势可以产生相同的 Intent，例如 `Tap` 和 `Swipe` 最终都可能产生 `PressKey` 意图
- `KeyboardInputMode` 作为 `InputGesture` 的参数参与转换策略的选择，但转换结果只与语义意图有关，不保留输入模式的几何信息

---

## 4 `GestureFeedbackState` 手势反馈状态

`GestureFeedbackState` 负责纯粹的视觉反馈，独立于 `ImeState`，使用归一化坐标存储。所有坐标数据以归一化形式 `[0,1]x[0,1]` 存储，绘制时由 `GestureFeedbackPanel` 根据面板实际尺寸转换为像素坐标。这使得同一份反馈数据可以正确地在不同 Zone 和不同尺寸的面板实例上渲染。

| 属性 | 说明 |
|------|------|
| 角色 | 手势视觉反馈状态，独立于任何面板 |
| 职责 | 管理触摸轨迹、按键高亮、手指指示器的视觉反馈数据 |
| 约束 | 使用归一化坐标 `[0,1]x[0,1]`；仅含触摸轨迹、按键高亮、手指指示器 |
| 关键属性 | `touchTrailPoints`, `pressedKeys`, `fingerIndicator` |
| 所属包 | feedback |

```kotlin
/**
 * 手势反馈状态，使用归一化坐标，纯视觉反馈。
 *
 * 1. 所有坐标数据以归一化形式 [0,1]x[0,1] 存储，
 *    绘制时由 GestureFeedbackPanel 根据面板实际尺寸转换为像素坐标。
 * 2. 弹出提示通过 ImeEffect 副作用通道驱动，由 KeyboardViewModel 管理 PopupTipState，不属于视觉反馈。
 * 3. 按键间路径统一作为 touchTrailPoints 中的轨迹数据，
 *    由 KeyLayoutPanel 根据 KeyboardInputMode 计算起止按键间的平滑曲线后，
 *    作为插值路径点写入。
 */
class GestureFeedbackState {

    /**
     * 触摸轨迹点（归一化坐标）。
     *
     * 由 GestureInputPanel 在手势过程中实时积累，
     * 手势结束后自动清除。归一化坐标基于 GestureInputPanel
     * 自身尺寸计算：normalizedX = eventX / panelWidth。
     *
     * touchTrailPoints 不仅包含手指的实际触摸点，
     * 还包含按键间路径的平滑曲线插值点。
     * KeyLayoutPanel 根据 KeyboardInputMode 动态计算起始按键到目标按键间的
     * 轨迹形状（如 RectGrid 的直线路径、HexGrid 的弧形路径等），
     * 生成归一化坐标插值路径后写入 touchTrailPoints。
     */
    private val _touchTrailPoints = MutableStateFlow<List<OffsetF>>(emptyList())
    val touchTrailPoints: StateFlow<List<OffsetF>> = _touchTrailPoints.asStateFlow()

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
     * 手指指示器状态（归一化坐标）。
     *
     * 由 InputActionPlayer 驱动，独立于用户真实手势。
     * 归一化坐标基于 KeyLayoutPanel 的尺寸计算，
     * 在动画播放期间由 KeyLayoutPanel 动态提供。
     */
    private val _fingerIndicator = MutableStateFlow<InputActionFingerIndicator?>(null)
    val fingerIndicator: StateFlow<InputActionFingerIndicator?> = _fingerIndicator.asStateFlow()

    // --- 由输入面板/播放器调用的更新方法 ---

    /** 添加单个归一化轨迹点 */
    fun addTouchTrailPoint(normalizedPoint: OffsetF) {
        _touchTrailPoints.update { list ->
            val mutable = list.toMutableList()  // 单次 O(n) 拷贝
            mutable.add(normalizedPoint)         // O(1) 追加
            mutable
        }
    }

    /** 设置完整轨迹（含按键间路径的插值点） */
    fun setTouchTrailPoints(points: List<OffsetF>) {
        _touchTrailPoints.value = points
    }

    fun clearTouchTrail() { _touchTrailPoints.value = emptyList() }

    fun setPressedKeys(keys: Set<InputKey>) { _pressedKeys.value = keys }

    fun clearPressedKeys() { _pressedKeys.value = emptySet() }

    fun setFingerIndicator(state: InputActionFingerIndicator?) { _fingerIndicator.value = state }

    /** 手势结束，清除所有临时反馈 */
    fun clearAll() {
        clearTouchTrail()
        clearPressedKeys()
    }

    /** 清理所有状态（包括手指指示器） */
    fun clear() {
        clearAll()
        _fingerIndicator.value = null
    }
}
```

`GestureFeedbackState` 包含三类核心视觉反馈：触摸轨迹点（`touchTrailPoints`，含按键间路径的插值点）、按键高亮集合（`pressedKeys`）、手指指示器状态（`fingerIndicator`）。弹出提示通过引擎 `ImeEffect` 副作用通道驱动，由 `KeyboardViewModel` 管理 `PopupTipState`，不属于视觉反馈。按键间路径统一合并到 `touchTrailPoints` 中，由 `KeyLayoutPanel` 根据 `KeyboardInputMode` 计算起止按键间的平滑曲线后，作为插值路径点统一写入。触摸轨迹、按键间路径统一为一种输入轨迹，简化了状态管理和绘制逻辑。

`InputActionFingerIndicator` 的类型定义在 engine 模块中，此处直接引用。

---

## 5 屏幕分区模型

### 5.1 `LayoutZone` —— 屏幕分区

IME 将屏幕纵向划分为两个 Zone，每个 Zone 承担不同的交互职责。Zone A（`LayoutZone.A`）占据屏幕上半区，仅在 `Separated` 模式下承载 `KeyLayoutPanel` 和 `GestureFeedbackPanel`，用于展示按键布局和手势反馈。Zone B（`LayoutZone.B`）占据屏幕下半区，是所有交互的核心区域，无论 `Stacked` 还是 `Separated` 模式，Zone B 始终包含完整的三行结构。Zone 的划分使得输入区域和按键展示区域在物理上分离成为可能，从而解决手指遮挡按键视野的问题，同时保持两种模式在组件层面的统一性。

```kotlin
/**
 * 屏幕布局分区。
 *
 * 定义 IME 在屏幕上的空间划分。
 * Zone A 占据屏幕上半区，Zone B 占据屏幕下半区。
 * 在 Stacked 模式下，仅使用 Zone B；在 Separated 模式下，Zone A 和 Zone B 均被使用。
 */
sealed class LayoutZone {

    /** 屏幕上半区，Separated 模式下展示按键布局和手势反馈 */
    data object A : LayoutZone()

    /** 屏幕下半区，所有交互的核心区域，包含三行结构 */
    data object B : LayoutZone()
}
```

### 5.2 `KeyboardLayoutMode` —— 布局模式

`KeyboardLayoutMode` 定义 Zone A 与 Zone B 的使用方式。`Stacked` 模式下，所有组件集中在 Zone B 内，三层面板叠加共享同一空间，适合紧凑布局和单手操作场景。`Separated` 模式下，输入区域占据 Zone B，按键展示区域占据 Zone A，手指在 Zone B 输入时不会被自身遮挡，按键在 Zone A 的更宽空间中展示，缩短了视觉搜索路径。两种模式可动态切换，切换时组件实例按实例策略表进行重新部署。

```kotlin
/**
 * 布局模式，定义 Zone A 与 Zone B 的使用方式。
 */
sealed class KeyboardLayoutMode {

    /**
     * 堆叠模式。
     *
     * 所有交互组件集中在 Zone B，三层面板叠加共享同一空间。
     * KeyLayoutPanel 部署在 Zone B 的 Row 3。
     */
    data object Stacked : KeyboardLayoutMode()

    /**
     * 分离模式。
     *
     * 输入区域占据 Zone B，按键展示区域占据 Zone A。
     * KeyLayoutPanel 部署在 Zone A，
     * GestureInputPanel 部署在 Zone B 的 Row 3 中列。
     */
    data class Separated(
        /** Zone A 占屏幕高度的比例，默认 0.4 */
        val zoneARatio: Float = 0.4f,
    ) : KeyboardLayoutMode()
}
```

---

## 6 Zone B 三行结构

Zone B 纵向划分为三个行，每行承载不同类型的面板组件，行与行之间不存在重叠。

**Row 1：`CandidateListPanel` + `PopupTipPanel`（叠加共存）**

Row 1 位于 Zone B 顶部，承载 `CandidateListPanel` 和 `PopupTipPanel`，二者叠加共享同一空间。`CandidateListPanel` 在底层渲染候选词列表，`PopupTipPanel` 仅在需要时短暂浮现并覆盖在 `CandidateListPanel` 之上。弹出提示支持两种类型：`PopupTipState.Message`（纯文本信息提示，如输入字符、键盘切换、编辑操作反馈，自动消失）和 `PopupTipState.Action`（可点击提示，含操作按钮，点击后触发 `ImeIntent`，如粘贴内容、保存收藏）。`PopupTipState.Action` 的 `persistent` 参数控制是否在输入时保持显示——`persistent=true` 的提示在用户开始输入前不会自动消失，`persistent=false` 的提示在超时后自动消失。当 `PopupTipPanel` 可见时，`CandidateListPanel` 仍在底层渲染但不响应用户交互，提示消失后候选列表恢复交互能力。

**Row 2：`ToolListPanel` ↔ `InputListPanel`（互斥共存）**

Row 2 位于中间，承载 `ToolListPanel` 和 `InputListPanel`，二者互斥共享同一空间。由 `KeyboardViewModel` 从 `state.inputList.hasPending` 派生的 `isInputting` 状态控制切换——空闲时显示 `ToolListPanel`，输入时切换为 `InputListPanel`。切换时存在短暂的进入/退出动画，确保视觉流畅性。`ToolListPanel` 展示工具按钮（全选、复制、粘贴、撤销等编辑功能键，以及键盘类型切换键），按钮内容由 `KeyboardViewModel` 根据 `keyboard.type` 和 Feature 门控动态配置。`InputListPanel` 展示当前输入的拼音串、光标位置和编辑状态，支持点击间隙移动光标。

**Row 3：`KeyLayoutPanel` + `GestureFeedbackPanel` + `GestureInputPanel`（三层叠加）**

Row 3 位于底部，承载三层分离架构的核心面板，三者始终共存并叠加共享同一空间。叠加顺序从底到顶为：`KeyLayoutPanel`（按键渲染）→ `GestureFeedbackPanel`（手势反馈绘制）→ `GestureInputPanel`（触摸事件接收）。`KeyLayoutPanel` 根据 `KeyboardInputMode` 和 `KeyboardType` 渲染按键布局，`GestureFeedbackPanel` 绘制触摸轨迹、按键高亮和手指指示器，`GestureInputPanel` 接收触摸事件并识别为 `InputGesture`。在输入动作播放期间，Row 1 和 Row 2 的面板可通过内建的 `showIndicator` 参数在自身内部绘制指示器动画，无需额外的覆盖层组件。

---

## 7 归一化坐标体系

归一化坐标体系是三层分离架构的重要基础设施，使得手势反馈数据可以跨面板、跨 Zone 使用，无需关心面板的实际像素尺寸。

### 7.1 `CoordinateNormalizer`

```kotlin
/**
 * 坐标归一化工具。
 *
 * 将绝对像素坐标归一化到 [0,1] 范围，以及反向转换。
 * 归一化坐标使得手势反馈数据可以跨面板、跨 Zone 使用，
 * 无需关心面板的实际像素尺寸。
 *
 * 归一化坐标的约定：
 * - (0, 0) 对应面板左上角
 * - (1, 1) 对应面板右下角
 * - X 轴向右为正，Y 轴向下为正
 */
object CoordinateNormalizer {

    /**
     * 将绝对坐标归一化。
     *
     * @param offset 绝对像素坐标
     * @param sourceSize 坐标来源面板的尺寸
     * @return 归一化坐标 [0,1] x [0,1]
     */
    fun normalize(offset: Offset, sourceSize: Size): OffsetF {
        require(sourceSize.width > 0f && sourceSize.height > 0f) {
            "Source size must be positive: $sourceSize"
        }
        return OffsetF(
            x = (offset.x / sourceSize.width).coerceIn(0f, 1f),
            y = (offset.y / sourceSize.height).coerceIn(0f, 1f),
        )
    }

    /**
     * 将归一化坐标反归一化为绝对坐标。
     *
     * @param normalized 归一化坐标
     * @param targetSize 目标面板的尺寸
     * @return 绝对像素坐标
     */
    fun denormalize(normalized: OffsetF, targetSize: Size): Offset {
        return Offset(
            x = normalized.x * targetSize.width,
            y = normalized.y * targetSize.height,
        )
    }

    /**
     * 将归一化矩形反归一化为绝对矩形。
     */
    fun denormalize(normalized: RectF, targetSize: Size): Rect {
        return Rect(
            left = normalized.left * targetSize.width,
            top = normalized.top * targetSize.height,
            right = normalized.right * targetSize.width,
            bottom = normalized.bottom * targetSize.height,
        )
    }
}
```

### 7.2 `OffsetF` 与 `RectF`

```kotlin
/**
 * 归一化坐标点。
 */
data class OffsetF(val x: Float, val y: Float)

/**
 * 归一化矩形（坐标范围 [0,1]）。
 */
data class RectF(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
) {
    val center: OffsetF get() = OffsetF(
        (left + right) / 2,
        (top + bottom) / 2,
    )
}

/** Offset 扩展：归一化到 [0,1] 范围 */
fun Offset.normalize(sourceSize: Size): OffsetF =
    CoordinateNormalizer.normalize(this, sourceSize)

/** OffsetF 扩展：反归一化为绝对坐标 */
fun OffsetF.denormalize(targetSize: Size): Offset =
    CoordinateNormalizer.denormalize(this, targetSize)
```

归一化坐标体系的核心价值在于：`GestureInputPanel` 在 Zone B 的 Row 3 中将触摸事件坐标归一化后写入 `GestureFeedbackState`，`GestureFeedbackPanel` 在 Zone A 和 Zone B 各自根据自身尺寸反归一化后绘制。由于 Zone A 和 Zone B 的面板尺寸不同，同一份归一化坐标数据在两个 Zone 中渲染出的像素坐标也不同，但视觉位置关系保持一致。这使得 `Separated` 模式下的双实例 `GestureFeedbackPanel` 可以正确渲染同一套反馈数据。

---

## 8 布局状态模型

### 8.1 `KeyLayoutState`

`KeyLayoutState` 是面板布局的核心状态对象，被多个组件（`GestureInputPanel`、`GestureFeedbackPanel`、`ComposeInputActionPositionResolver`）共同引用。位置信息使用归一化坐标 `[0,1]x[0,1]` 存储，绘制时根据面板实际尺寸转换为像素坐标。

```kotlin
/** 按键布局状态，含空间格网索引 */
data class KeyLayoutState(
    /** 按键归一化位置映射（归一化坐标） */
    val keyPositions: Map<InputKey, RectF> = emptyMap(),
    /** 空间格网索引：将键盘划分为 gridCols×gridRows 个格子，每个格子关联其覆盖的按键列表 */
    val spatialGrid: SpatialGrid = SpatialGrid(),
    /** 面板实际尺寸（像素），用于归一化坐标转换 */
    val panelSize: Size = Size.Zero,
) {
    /**
     * 空间格网索引。
     *
     * 将归一化键盘区域划分为 gridCols × gridRows 个格子，
     * 每个格子预计算其覆盖的按键列表。
     * 触摸事件时，先通过坐标定位到格子，再检测该格子内的少量按键。
     */
    class SpatialGrid(
        val gridCols: Int = 4,
        val gridRows: Int = 3,
    ) {
        private val grid: Array<Array<MutableList<InputKey>>> = 
            Array(gridRows) { Array(gridCols) { mutableListOf() } }
        
        /** 构建索引：将按键按归一化位置分配到对应格子 */
        fun buildIndex(keyPositions: Map<InputKey, RectF>) {
            grid.forEach { row -> row.forEach { it.clear() } }
            keyPositions.forEach { (key, rect) ->
                val colStart = (rect.center.x * gridCols).toInt().coerceIn(0, gridCols - 1)
                val rowStart = (rect.center.y * gridRows).toInt().coerceIn(0, gridRows - 1)
                grid[rowStart][colStart].add(key)
            }
        }
        
        /** 查找指定归一化坐标所在的格子中的按键列表 */
        fun findCandidateKeys(normalizedPos: OffsetF): List<InputKey> {
            val col = (normalizedPos.x * gridCols).toInt().coerceIn(0, gridCols - 1)
            val row = (normalizedPos.y * gridRows).toInt().coerceIn(0, gridRows - 1)
            return grid[row][col]
        }
    }
    
    /** 将归一化矩形转换为指定面板尺寸下的像素矩形 */
    fun denormalize(normalized: RectF, targetSize: Size): Rect {
        return Rect(
            left = normalized.left * targetSize.width,
            top = normalized.top * targetSize.height,
            right = normalized.right * targetSize.width,
            bottom = normalized.bottom * targetSize.height,
        )
    }
    
    /** 使用空间格网索引查找按键 */
    fun findKeyAt(position: Offset, targetSize: Size): InputKey? {
        val normalized = OffsetF(
            x = (position.x / targetSize.width).coerceIn(0f, 1f),
            y = (position.y / targetSize.height).coerceIn(0f, 1f),
        )
        // 先通过格网定位候选按键（通常 3-5 个）
        val candidates = spatialGrid.findCandidateKeys(normalized)
        // 仅检测候选按键中的精确命中
        return candidates.firstOrNull { key ->
            val rect = keyPositions[key] ?: return@firstOrNull false
            denormalize(rect, targetSize).contains(position)
        }
    }
}
```

性能对比：使用 4×3 空间格网索引后，每次触摸事件的按键检测从 30 次 Rect 创建降至 3-5 次，对象分配从 1800 对象/秒（60fps）降至 180-300 对象/秒。gridCols/gridRows 可通过 KeyLayoutState 构建时由布局策略动态配置。

### 8.2 `CandidateListLayoutState`

`CandidateListLayoutState` 用于坐标解析和输入动作播放期间指示器定位。候选项位置使用屏幕坐标系存储，定位时通过 `locateItem()` 方法获取候选项中心坐标。

```kotlin
/**
 * 候选列表面板布局状态，用于坐标解析。
 */
data class CandidateListLayoutState(
    /** 各候选项的位置矩形（屏幕坐标系） */
    val candidatePositions: List<Rect>,
    /** 面板实际尺寸，用于归一化坐标转换 */
    val panelSize: Size = Size.Zero,
) {
    /**
     * 查找指定候选项的中心坐标。
     *
     * 用于 InputActionPlayer 绘制指示器动画时定位候选项。
     * 若目标候选项不在可视范围内，需先滚动到目标位置。
     */
    fun locateItem(index: Int): Offset? {
        return candidatePositions.getOrNull(index)?.center
    }
}
```

### 8.3 `InputListLayoutState`

`InputListLayoutState` 用于坐标解析和输入动作播放期间指示器定位。输入项位置使用屏幕坐标系存储，定位时通过 `locateItem()` 方法获取输入项中心坐标。

```kotlin
/**
 * 输入列表面板布局状态。
 */
data class InputListLayoutState(
    /** 各输入项的位置矩形（屏幕坐标系） */
    val itemPositions: List<Rect>,
    /** 面板实际尺寸，用于归一化坐标转换 */
    val panelSize: Size = Size.Zero,
) {
    /**
     * 查找指定输入项的中心坐标。
     *
     * 用于 InputActionPlayer 绘制指示器动画时定位输入项。
     * 若目标项不在可视范围内，需先滚动到目标位置。
     */
    fun locateItem(index: Int): Offset? {
        return itemPositions.getOrNull(index)?.center
    }
}
```
