# 面板三层分离与屏幕布局设计

本文档定义 kuaizi IME 的面板三层分离架构与屏幕布局模型。键盘 UI 分离为三个独立层：**手势输入面板**（GestureInputPanel）负责接收用户手势并识别为输入意图，**按键布局面板**（KeyLayoutPanel）负责按键的布局渲染和状态展示，**手势反馈面板**（GestureFeedbackPanel）负责手势视觉反馈的绘制。三者通过 `InputGesture`、`GestureFeedbackState` 和 `ImeState` 解耦，互不直接依赖。

反馈面板是独立于输入面板和按键面板的透明层，支持多实例。这种设计使得反馈面板可以灵活地与输入面板、按键面板组合叠加——在堆叠（Stacked）布局模式下，三层面板完全重叠；在分离（Separated）布局模式下，反馈面板可以分别与输入面板和按键面板叠加，使手指轨迹在输入区域可见、按键高亮在按键区域可见。

屏幕纵向划分为 Zone A（上半区）和 Zone B（下半区），Zone B 内部按三行结构组织面板。InputMode 与 Keyboard.Type 作为两个独立的正交维度，任意 InputMode 可与任意 Type 组合，通过 LayoutStrategy 分发不同的布局策略。所有坐标数据使用归一化形式存储，绘制时根据面板实际尺寸转换为像素坐标，使得同一份反馈数据可以正确地在不同 Zone 和不同尺寸的面板实例上渲染。

```plantuml
@file:../diagrams/ui-panel-separation.puml
```

```plantuml
@file:../diagrams/ui-keyboard-panel.puml
```

---

## 1. 核心概念

### 1.1 三层分离核心思想

三层分离是键盘 UI 的核心架构原则，将面板职责划分为三个完全独立的层，每层仅负责一项核心职责，不越界处理其他层的事务：

```
┌─────────────────────────────────────────────────────────────┐
│                     输入面板（最上层）                        │
│  GestureInputPanel（透明手势层）                             │
│  - 接收原始触摸事件（必须在最上层，确保触摸优先到达）          │
│  - 识别手势类型（点击/长按/滑行/翻转）                       │
│  - 查询 KeyLayoutState 定位目标按键                         │
│  - 输出 InputGesture → ViewModel                           │
│  - 驱动 GestureFeedbackState → 反馈面板                     │
│  - 不绘制任何视觉反馈（完全透明，不遮挡下层反馈面板）          │
├─────────────────────────────────────────────────────────────┤
│                     反馈面板（可多实例）                      │
│  GestureFeedbackPanel（透明绘制层）                          │
│  - 绘制输入轨迹（含按键间路径和 X-Pad 路径的平滑曲线）        │
│  - 绘制按键高亮                                             │
│  - 绘制手指指示器（程序化输入动画）                           │
│  - 不处理任何触摸事件（触摸事件穿透到下层或被上层拦截）        │
│  - 不依赖任何面板的 Canvas                                  │
│  - 可与输入面板叠加，也可与按键面板叠加                       │
├─────────────────────────────────────────────────────────────┤
│                        按键面板（最下层）                     │
│  KeyLayoutPanel（按键布局层）                               │
│  - 根据 InputMode 和 Keyboard.Type 渲染按键布局             │
│  - 展示按键状态（按下/激活/禁用）                            │
│  - 提供布局状态供输入面板和反馈面板查询                       │
│  - 不处理任何触摸事件                                       │
│  - 不绘制手势反馈                                           │
└─────────────────────────────────────────────────────────────┘
```

**三条核心原则**：

1. **输入面板是唯一的触摸事件接收者**，按键面板和反馈面板不处理触摸。输入面板必须在最上层，确保触摸事件优先到达手势检测层，而非被反馈面板或按键面板拦截
2. **反馈面板是唯一的视觉反馈绘制者**，输入面板不绘制反馈（完全透明），按键面板仅绘制按键的常规状态渲染（非手势触发的临时反馈）。输入面板虽在最上层，但因完全透明，不会遮挡反馈面板的视觉效果
3. **三者之间的通信通过共享状态解耦**：输入面板 → `InputGesture` → ViewModel → `ImeState` → 按键面板；输入面板 → `GestureFeedbackState` → 反馈面板

**叠加顺序的设计考量**：

为什么输入面板在反馈面板上层而非下层？因为 Compose 的触摸事件从最上层开始向下传播，如果反馈面板在输入面板之上，即使其 Canvas 不主动消费触摸事件，也存在事件被意外拦截的风险（如未来添加交互功能、Compose 版本行为变更等）。将输入面板置于最上层是最可靠的方案——它确保所有触摸事件首先到达手势检测层，同时输入面板完全透明不绘制任何内容，不会视觉遮挡下层的反馈绘制效果。

**按键面板与反馈面板的视觉职责边界**：

按键面板根据 `KeyboardState` 渲染按键的**持续性视觉状态**（如按下态、激活态、禁用态、候选选中态），这些状态是由 ViewModel 的 reduce 结果驱动的确定性渲染。反馈面板绘制**临时性手势视觉反馈**（如输入轨迹、按键高亮光圈、手指指示器），这些反馈跟随用户手指实时变化，在手势结束后消失。这种分离确保了按键面板的重组仅由状态变更驱动，而高频的手势反馈更新不会触发按键面板的重组。

### 1.2 LayoutZone -- 屏幕分区

| 属性 | 说明 |
|------|------|
| 角色 | 屏幕空间分区定义 |
| 职责 | 将 IME 屏幕纵向划分为两个功能区，分配交互职责 |
| 约束 | Zone A 仅在 Separated 模式下使用；Zone B 在两种模式下始终使用 |
| 关键属性 | LayoutZoneA（上半区），LayoutZoneB（下半区） |
| 所属包 | keyboard |

IME 将屏幕纵向划分为两个 Zone，每个 Zone 承担不同的交互职责。Zone A（Keyboard.LayoutZoneA）占据屏幕上半区，仅在 Separated 模式下承载 KeyLayoutPanel 和 GestureFeedbackPanel，用于展示按键布局和手势反馈。Zone B（Keyboard.LayoutZoneB）占据屏幕下半区，是所有交互的核心区域，无论 Stacked 还是 Separated 模式，Zone B 始终包含完整的三行结构。Zone 的划分使得输入区域和按键展示区域在物理上分离成为可能，从而解决手指遮挡按键视野的问题，同时保持两种模式在组件层面的统一性。

```kotlin
/**
 * 屏幕布局分区。
 *
 * 定义 IME 在屏幕上的空间划分。
 * Zone A 占据屏幕上半区，Zone B 占据屏幕下半区。
 * 在 Stacked 模式下，仅使用 Zone B；在 Separated 模式下，Zone A 和 Zone B 均被使用。
 */
sealed class Keyboard {

    /** 屏幕上半区，Separated 模式下展示按键布局和手势反馈 */
    data object LayoutZoneA : Keyboard()

    /** 屏幕下半区，所有交互的核心区域，包含三行结构 */
    data object LayoutZoneB : Keyboard()
}
```

### 1.3 LayoutMode -- 布局模式

| 属性 | 说明 |
|------|------|
| 角色 | 定义 Zone A 与 Zone B 的使用方式 |
| 职责 | 控制面板组件的空间分配和部署位置 |
| 约束 | Stacked 模式下 Zone A 不使用；Separated 模式下两区均使用 |
| 关键属性 | Stacked（堆叠），Separated(zoneARatio)（分离） |
| 所属包 | keyboard |

LayoutMode 定义 Zone A 与 Zone B 的使用方式。Stacked 模式下，所有组件集中在 Zone B 内，三层面板叠加共享同一空间，适合紧凑布局和单手操作场景。Separated 模式下，输入区域占据 Zone B，按键展示区域占据 Zone A，手指在 Zone B 输入时不会被自身遮挡，按键在 Zone A 的更宽空间中展示，缩短了视觉搜索路径。两种模式可动态切换，切换时组件实例按实例策略表（见 §4.2）进行重新部署。

```kotlin
/**
 * 布局模式，定义 Zone A 与 Zone B 的使用方式。
 *
 * Stacked 模式：所有组件集中在 Zone B 内。
 * Separated 模式：输入区域在 Zone B，按键展示在 Zone A。
 */
sealed class Keyboard {

    sealed class LayoutMode {

        /**
         * 堆叠模式。
         *
         * 所有交互组件集中在 Zone B，三层面板叠加共享同一空间。
         * 适合紧凑布局和单手操作场景。
         * KeyLayoutPanel 部署在 Zone B 的 Row 3。
         */
        data object Stacked : LayoutMode()

        /**
         * 分离模式。
         *
         * 输入区域占据 Zone B，按键展示区域占据 Zone A。
         * 提供更宽的按键视野，避免手指遮挡，缩短输入路径。
         * KeyLayoutPanel 部署在 Zone A，GestureInputPanel 部署在 Zone B 的 Row 3 中列。
         */
        data class Separated(
            /** Zone A 占屏幕高度的比例，默认 0.4 */
            val zoneARatio: Float = 0.4f,
        ) : LayoutMode()
    }
}
```

### 1.4 InputMode -- 输入模式

| 属性 | 说明 |
|------|------|
| 角色 | 输入交互范式定义 |
| 职责 | 决定按键的几何排列方式和手势交互方式 |
| 约束 | 与 Keyboard.Type 正交，任意 InputMode 可与任意 Type 组合 |
| 关键属性 | XPad, HexGrid, RectGrid, MultiZone |
| 所属包 | keyboard |

InputMode 决定交互范式和布局几何，是独立于键盘内容类型的正交维度。X-Pad 模式采用六边形网格布局，手指在六边形区域间滑行选择声母韵母组合；HexGrid 模式采用六边形网格但交互方式不同于 X-Pad，适用于需要六边形紧密排列的场景；RectGrid 模式采用传统矩形网格布局，是最常见的 QWERTY 式按键排列；MultiZone 模式将键盘划分为多个独立区域，每个区域可独立交互，适用于需要分区操作的场景。InputMode 的选择直接影响 KeyLayoutPanel 的布局算法和 GestureInputPanel 的手势识别逻辑，同时也决定了输入轨迹的几何形状——不同 InputMode 下从起始按键到目标按键的轨迹曲线由 KeyLayoutPanel 动态计算，作为输入轨迹的一部分写入 GestureFeedbackState 的 touchTrailPoints。

```kotlin
/**
 * 输入模式，决定交互范式和布局几何。
 *
 * InputMode 与 Keyboard.Type 正交：任意 InputMode 可与任意 Type 组合。
 * InputMode 影响按键的几何排列方式和手势交互方式。
 */
sealed class Keyboard {

    enum class InputMode {
        /** X-Pad 六边形面板，手指在六边形区域间滑行 */
        XPad,
        /** 六边形网格布局，紧密排列 */
        HexGrid,
        /** 矩形网格布局，传统 QWERTY 式排列 */
        RectGrid,
        /** 多区域布局，键盘划分为多个独立交互区域 */
        MultiZone,
    }
}
```

### 1.5 Keyboard.Type -- 键盘类型

| 属性 | 说明 |
|------|------|
| 角色 | 键盘内容类型定义 |
| 职责 | 决定按键集合的语义内容和标签 |
| 约束 | 与 InputMode 正交；编辑功能由 ToolListPanel 统一管理 |
| 关键属性 | Pinyin, Latin, Symbol, Emoji, Number, Math |
| 所属包 | keyboard |

Keyboard.Type 决定键盘的内容类型，即按键集合的语义分类。Pinyin 类型提供拼音输入的声母韵母按键，Latin 类型提供拉丁字母按键，Symbol/Emoji 类型提供符号和表情，Number 类型提供数字和基本运算符，Math 类型提供数学公式相关按键。Type 的选择决定了 KeyLayoutPanel 渲染哪些按键以及按键的标签内容，但不影响按键的几何排列方式——几何排列由 InputMode 决定。Editor 类型的编辑功能键（如全选、复制、粘贴、撤销等）由 ToolListPanel 统一管理，作为工具项展示，编辑功能在任何键盘类型下均可通过工具栏快速访问。

```kotlin
/**
 * 键盘类型，决定键盘的内容类型。
 *
 * Type 与 InputMode 正交：任意 Type 可与任意 InputMode 组合。
 * Type 决定按键集合的语义内容和标签，InputMode 决定按键的几何排列方式。
 * 编辑功能由 ToolListPanel 统一管理。
 */
sealed class Keyboard {

    enum class Type {
        Pinyin,
        Latin,
        Symbol,
        Emoji,
        Number,
        Math,
    }
}
```

### 1.6 InputMode x Keyboard.Type 正交矩阵

InputMode 与 Keyboard.Type 是两个完全独立的正交维度，不存在互斥关系。任意 InputMode 可以与任意 Type 组合，产生不同的按键布局和交互体验。例如 Pinyin + XPad 组合产生六边形拼音滑行面板，Pinyin + RectGrid 组合产生传统 QWERTY 拼音键盘，Number + HexGrid 组合产生六边形数字面板。这种正交设计使得新增 InputMode 或 Type 时不需要修改已有组合的代码，只需在 KeyLayoutPanel 中为新的组合提供布局策略即可。

|  | Pinyin | Latin | Symbol/Emoji | Number | Math |
|---|---|---|---|---|---|
| **XPad** | 六边形拼音滑行 | 六边形拉丁滑行 | 六边形符号选择 | 六边形数字输入 | 六边形公式输入 |
| **HexGrid** | 六边形拼音网格 | 六边形拉丁网格 | 六边形符号网格 | 六边形数字网格 | 六边形公式网格 |
| **RectGrid** | QWERTY 拼音键盘 | QWERTY 拉丁键盘 | 矩形符号键盘 | 矩形数字键盘 | 矩形公式键盘 |
| **MultiZone** | 分区拼音输入 | 分区拉丁输入 | 分区符号输入 | 分区数字输入 | 分区公式输入 |

---

## 2. 屏幕布局模型

### 2.1 Zone A 与 Zone B 空间关系

屏幕纵向划分为 Zone A（上半区）和 Zone B（下半区），两区比例由 LayoutMode 决定。在 Stacked 模式下，Zone A 不使用（高度为 0），Zone B 占据全部 IME 屏幕空间；在 Separated 模式下，Zone A 和 Zone B 按 zoneARatio 比例分配屏幕高度，默认 Zone A 占 40%、Zone B 占 60%。Zone A 的内容仅包含 KeyLayoutPanel 和 GestureFeedbackPanel 的叠加，用于展示按键布局和手势反馈轨迹。Zone B 包含完整的三行结构，承载所有交互面板。

```
Separated 模式下的屏幕布局：

+-------------------------------------------+
|              Zone A (40%)                  |
|  +---------------------------------------+|
|  | KeyLayoutPanel + GestureFeedbackPanel  ||
|  |     (叠加，共享同一空间)                ||
|  +---------------------------------------+|
+-------------------------------------------+
|              Zone B (60%)                  |
|  +---------------------------------------+|
|  | Row 1: CandidateListPanel             ||
|  |        + PopupTipPanel (叠加)          ||
|  +---------------------------------------+|
|  | Row 2: ToolListPanel / InputListPanel  ||
|  |        (互斥，共享同一空间)             ||
|  +---------------------------------------+|
|  | Row 3: KeyLayoutPanel +                ||
|  |        GestureFeedbackPanel +           ||
|  |        GestureInputPanel (叠加)         ||
|  +---------------------------------------+|
+-------------------------------------------+

Stacked 模式下的屏幕布局：

+-------------------------------------------+
|              Zone B (100%)                 |
|  +---------------------------------------+|
|  | Row 1: CandidateListPanel             ||
|  |        + PopupTipPanel (叠加)          ||
|  +---------------------------------------+|
|  | Row 2: ToolListPanel / InputListPanel  ||
|  |        (互斥，共享同一空间)             ||
|  +---------------------------------------+|
|  | Row 3: KeyLayoutPanel +                ||
|  |        GestureFeedbackPanel +           ||
|  |        GestureInputPanel (叠加)         ||
|  +---------------------------------------+|
+-------------------------------------------+
```

### 2.2 Zone B 三行结构

Zone B 纵向划分为三个行，每行承载不同类型的面板组件，行与行之间不存在重叠。Row 1 位于 Zone B 顶部，承载 CandidateListPanel 和 PopupTipPanel，二者叠加共享同一空间，PopupTipPanel 仅在需要时短暂浮现并覆盖在 CandidateListPanel 之上。Row 2 位于中间，承载 ToolListPanel 和 InputListPanel，二者互斥共享同一空间，空闲时显示 ToolListPanel，输入时切换为 InputListPanel。Row 3 位于底部，承载 KeyLayoutPanel、GestureFeedbackPanel 和 GestureInputPanel，三者始终共存并叠加共享同一空间，分别负责按键渲染、手势反馈绘制和触摸事件接收。

三行结构的设计考量在于：将不同交互职责的面板在空间上分离，避免面板之间的视觉干扰和事件冲突。Row 1 的候选提示和操作提示在功能上相近（都是展示信息），共享空间但通过叠加方式共存；Row 2 的工具栏和输入栏在时间上互斥（空闲时用工具、输入时用输入栏），共享空间通过切换方式共存；Row 3 的三层面板在交互上互补（触摸/反馈/渲染），必须始终共存通过叠加方式共享空间。在输入动作播放期间，Row 1 和 Row 2 的面板可通过内建的 showIndicator 参数在自身内部绘制指示器动画，无需额外的覆盖层组件。

### 2.3 Separated 模式 Row 3 三列布局

在 Separated 模式下，Row 3 被进一步划分为三列，以充分利用分离布局带来的空间优势。左列和右列各放置常用输入功能按钮，如切换键盘、删除、空格、回车等功能键，这些按钮在分离模式下从 KeyLayoutPanel 的主键区中抽出，放置在两侧便于拇指快速触达。中列承载 GestureFeedbackPanel 和 GestureInputPanel 的叠加，是手势交互的核心区域。KeyLayoutPanel 在 Separated 模式下部署在 Zone A 而非 Zone B 的 Row 3，因此 Row 3 的三列布局中不包含 KeyLayoutPanel。

三列布局的设计目的是利用分离模式下手指在 Zone B 操作、按键在 Zone A 展示的空间优势，将常用功能键分布在手指两侧，缩短拇指移动距离，提升输入效率。中列的 GestureInputPanel 接收触摸事件，GestureFeedbackPanel 绘制手势轨迹，二者叠加共享中列空间。左列和右列的功能按钮由 KeyboardViewModel 根据 Keyboard.Type 动态配置，不同类型键盘可能显示不同的功能按钮集合。

```
Separated 模式下 Row 3 的三列布局：

+----------+--------------------+----------+
| 左列     |      中列           | 右列     |
| 功能按钮  | GestureFeedbackPanel| 功能按钮  |
| (切换等)  | +                  | (删除等)  |
|          | GestureInputPanel   |          |
|          | (叠加，共享空间)     |          |
+----------+--------------------+----------+
```

### 2.4 Zone A 内容

Zone A 仅在 Separated 模式下被使用，其内容由 KeyLayoutPanel 和 GestureFeedbackPanel 叠加构成。KeyLayoutPanel 负责根据当前 InputMode 和 Keyboard.Type 渲染按键布局，GestureFeedbackPanel 负责绘制触摸轨迹、按键高亮等视觉反馈。两面板叠加共享 Zone A 的全部空间，叠加顺序从底到顶为：KeyLayoutPanel（底层渲染）-> GestureFeedbackPanel（透明反馈层）。Zone A 的容器支持指定尺寸和布局方向（居中、顶部对齐、底部对齐），面板组件自动填充容器提供的可用空间。

Zone A 中的 GestureFeedbackPanel 使用归一化坐标绘制反馈。由于 Zone A 和 Zone B 各有独立的 GestureFeedbackPanel 实例，它们的尺寸不同，但共享同一套归一化坐标数据。Zone A 实例的尺寸由 Zone A 面板容器决定，Zone B 实例的尺寸由 Zone B 面板容器决定。绘制时，各实例根据自身尺寸将归一化坐标转换为实际像素坐标。KeyLayoutPanel 只能出现在 Zone A（Separated 模式）或 Zone B（Stacked 模式）中的一个位置，不存在同时出现在两个 Zone 的情况，即 KeyLayoutPanel 是单实例组件。

---

## 3. 数据模型

### 3.1 InputGesture

输入面板识别手势后，输出 `InputGesture` 而非直接操作按键。`InputGesture` 是坐标无关的逻辑手势描述：

| 属性 | 说明 |
|------|------|
| 角色 | 输入面板的手势输出模型 |
| 职责 | 描述用户的输入意图，坐标无关 |
| 约束 | 不包含任何绝对坐标，只包含按键的语义标识；包含 InputMode 参数以支持不同模式的手势识别策略 |
| 关键属性 | timestamp, Tap, LongPress, Swipe, Flip, XPadZonePath, CandidateTap |
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
    abstract val inputMode: InputMode

    /**
     * 点击按键。
     *
     * @param key 目标按键（由输入面板根据触摸位置查询 KeyLayoutState 确定）
     */
    data class Tap(
        override val timestamp: Long,
        override val inputMode: InputMode,
        val key: InputKey,
        /** 连续点击同一按键的次数（0=首次，1=双击，2=三击...） */
        val tick: Int = 0,
    ) : InputGesture()

    /**
     * 长按按键。
     */
    data class LongPress(
        override val timestamp: Long,
        override val inputMode: InputMode,
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
        override val inputMode: InputMode,
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
        override val inputMode: InputMode,
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
        override val inputMode: InputMode,
        val startZone: XPadZone,
        val path: List<XPadZone>,
    ) : InputGesture()

    /**
     * 候选项选择。
     */
    data class CandidateTap(
        override val timestamp: Long,
        override val inputMode: InputMode,
        val candidateIndex: Int,
    ) : InputGesture()
}

enum class FlipDirection { Left, Right, Up, Down }
```

InputGesture 与 ImeIntent 的关系：`InputGesture` 是输入面板的输出，`ImeIntent` 是 ViewModel 的输入。ViewModel 将 `InputGesture` 转换为 `ImeIntent`：

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

### 3.2 ImeState 扩展

| 属性 | 说明 |
|------|------|
| 角色 | 输入法全局状态 |
| 职责 | 承载键盘逻辑状态，驱动所有面板的渲染和切换 |
| 约束 | 由引擎 reduce 逻辑计算，UI 层只读；弹出提示归属 ImeState |
| 关键属性 | inputMode, isInputting, popupTip, toolList, keyboardType, keyGrid, keyboardState, candidateList, inputList |
| 所属包 | state |

ImeState 需要扩展以支持屏幕布局模型相关概念。新增 inputMode 字段表示当前输入模式，isInputting 字段表示是否正在输入（控制 ToolListPanel 和 InputListPanel 的互斥切换），popupTip 字段由 ImeState 管理弹出提示（弹出提示是引擎处理意图后更新 ImeState 触发的展示，属于输入状态变化而非视觉反馈），toolList 字段管理工具列表状态（含 Editor 类型的编辑功能键）。这些扩展仅涉及 UI 层状态的暴露，不改变引擎的核心 reduce 逻辑——引擎仍然通过 ImeIntent 驱动状态转换，UI 层从 ImeState 中读取新增字段来决定面板的部署和切换。

```kotlin
/**
 * ImeState 的 UI 层扩展字段。
 *
 * 这些字段由引擎的 reduce 逻辑计算并写入 ImeState，
 * UI 层通过 collectAsState() 订阅后驱动面板的部署和切换。
 */
data class ImeState(
    // ... 现有字段 ...

    /** 当前输入模式，决定按键布局几何和交互范式 */
    val inputMode: InputMode = InputMode.RectGrid,

    /** 是否正在输入，控制 ToolListPanel/InputListPanel 的互斥切换 */
    val isInputting: Boolean = false,

    /** 工具列表状态（含 Editor 类型的编辑功能键） */
    val toolList: ToolListState = ToolListState(emptyList()),

    /**
     * 弹出提示状态。
     *
     * 由引擎处理意图后更新 ImeState 触发显示。
     * PopupTipPanel 从 ImeState.popupTip 读取提示内容。
     * 弹出提示属于输入状态变化触发的展示，不属于视觉反馈。
     */
    val popupTip: PopupTipState? = null,
)
```

### 3.3 GestureFeedbackState

| 属性 | 说明 |
|------|------|
| 角色 | 手势视觉反馈状态，独立于任何面板 |
| 职责 | 管理触摸轨迹、按键高亮、手指指示器的视觉反馈数据 |
| 约束 | 使用归一化坐标 [0,1]x[0,1]；不含 popupTip/keyPath/xPadPath（已简化合并） |
| 关键属性 | touchTrailPoints, pressedKeys, fingerIndicator |
| 所属包 | feedback |

GestureFeedbackState 经简化后仅保留纯粹的视觉反馈职责。弹出提示由 ImeState 管理（见 §3.2），按键间路径和 X-Pad 路径统一合并到 touchTrailPoints 中。合并的理由：按键间路径（keyPath）和 X-Pad 路径（xPadPath）本质上都是输入轨迹的组成部分，由 KeyLayoutPanel 根据 InputMode 计算起止按键间的平滑曲线后，作为 touchTrailPoints 中的插值路径点统一写入。这三类路径本质上都是手指移动的轨迹，不应作为独立反馈类型。简化后的 GestureFeedbackState 包含三类核心视觉反馈：触摸轨迹点（含计算后的平滑曲线）、按键高亮集合、手指指示器状态。

```kotlin
/**
 * 手势反馈状态，使用归一化坐标，职责简化为纯视觉反馈。
 *
 * 1. 所有坐标数据以归一化形式 [0,1]x[0,1] 存储，
 *    绘制时由 GestureFeedbackPanel 根据面板实际尺寸转换为像素坐标。
 * 2. 移除 popupTip：弹出提示由 ImeState 管理，不属于视觉反馈。
 * 3. 移除 keyPath 和 xPadPath：按键间路径和 X-Pad 路径统一合并
 *    为输入轨迹的一部分，由 KeyLayoutPanel 根据 InputMode 计算
 *    起止按键间的平滑曲线后，作为 touchTrailPoints 写入。
 *    这三类路径本质上都是手指移动的轨迹，不应作为独立反馈类型。
 */
class GestureFeedbackState {

    /**
     * 触摸轨迹点（归一化坐标）。
     *
     * 由 GestureInputPanel 在手势过程中实时积累，
     * 手势结束后自动清除。归一化坐标基于 GestureInputPanel
     * 自身尺寸计算：normalizedX = eventX / panelWidth。
     *
     * **轨迹计算说明**：touchTrailPoints 不仅包含手指的实际触摸点，
     * 还包含按键间路径和 X-Pad 路径的平滑曲线插值点。
     * KeyLayoutPanel 根据 InputMode 动态计算起始按键到目标按键间的
     * 轨迹形状（如 RectGrid 的直线路径、XPad 的弧形路径等），
     * 生成归一化坐标插值路径后写入 touchTrailPoints。
     * 这样触摸轨迹、按键间路径、X-Pad 路径统一为一种输入轨迹，
     * 简化了状态管理和绘制逻辑。
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
        _touchTrailPoints.update { it + normalizedPoint }
    }

    /** 设置完整轨迹（含按键间路径和 X-Pad 路径的插值点） */
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

/**
 * 手指指示器状态（归一化坐标）。
 *
 * 属于输入动作播放体系，与普通 UI 状态命名区分。
 */
data class InputActionFingerIndicator(
    /** 归一化坐标位置 [0,1]x[0,1] */
    val position: OffsetF,
    val pressed: Boolean,
    val visible: Boolean = true,
)
```

### 3.4 PopupTipState

| 属性 | 说明 |
|------|------|
| 角色 | 弹出提示状态模型 |
| 职责 | 承载短暂弹出提示的展示内容 |
| 约束 | 归属 ImeState；短暂显示后自动消失 |
| 关键属性 | message, timestamp |
| 所属包 | state |

弹出提示状态，由引擎处理意图后更新 ImeState 触发显示。PopupTipPanel 从 ImeState.popupTip 读取提示内容。弹出提示属于输入状态变化触发的展示，不属于手势视觉反馈。

```kotlin
/**
 * 弹出提示状态。
 *
 * 由引擎处理意图后更新 ImeState.popupTip 触发显示。
 * 弹出提示是输入状态变化的结果展示，不属于手势视觉反馈，
 * 因此不应由 GestureFeedbackState 管理。
 */
data class PopupTipState(
    /** 提示消息内容 */
    val message: String,
    /** 提示生成时间戳（毫秒） */
    val timestamp: Long = System.currentTimeMillis(),
)
```

### 3.5 KeyLayoutState

| 属性 | 说明 |
|------|------|
| 角色 | 按键布局核心状态 |
| 职责 | 承载按键位置映射和 X-Pad 布局信息，提供坐标转换和按键查询方法 |
| 约束 | 使用归一化坐标存储；被多个组件共同引用 |
| 关键属性 | keyPositions, xPadLayoutInfo, panelSize |
| 所属包 | panel |

KeyLayoutState 是面板布局的核心状态对象，被多个组件（GestureInputPanel、GestureFeedbackPanel、InputActionPositionResolver）共同引用。位置信息使用归一化坐标 [0,1]x[0,1] 存储，绘制时根据面板实际尺寸转换为像素坐标。

```kotlin
/**
 * 按键布局状态。
 *
 * 包含按键位置映射和 X-Pad 布局信息，
 * 供 GestureInputPanel 和 GestureFeedbackPanel 查询。
 * 位置信息使用归一化坐标 [0,1] x [0,1] 存储，
 * 绘制时根据面板实际尺寸转换为像素坐标。
 */
data class KeyLayoutState(
    /** 按键归一化位置映射（归一化坐标） */
    val keyPositions: Map<InputKey, RectF> = emptyMap(),
    /** X-Pad 布局信息（仅 X-Pad 模式） */
    val xPadLayoutInfo: XPadLayoutInfo? = null,
    /** 面板实际尺寸（像素），用于归一化坐标转换 */
    val panelSize: Size = Size.Zero,
) {
    /**
     * 将归一化矩形转换为指定面板尺寸下的像素矩形。
     */
    fun denormalize(normalized: RectF, targetSize: Size): Rect {
        return Rect(
            left = normalized.left * targetSize.width,
            top = normalized.top * targetSize.height,
            right = normalized.right * targetSize.width,
            bottom = normalized.bottom * targetSize.height,
        )
    }

    /**
     * 查找指定位置（像素坐标）对应的按键。
     *
     * 将像素坐标与归一化按键矩形比较，
     * 先将归一化矩形反归一化为目标尺寸下的像素矩形，
     * 再判断像素坐标是否落在矩形内。
     */
    fun findKeyAt(position: Offset, targetSize: Size): InputKey? {
        return keyPositions.entries.firstOrNull { (_, rect) ->
            denormalize(rect, targetSize).contains(position)
        }?.key
    }
}
```

### 3.6 CandidateListLayoutState

| 属性 | 说明 |
|------|------|
| 角色 | 候选列表布局状态 |
| 职责 | 承载候选项位置信息，提供 locateItem() 定位方法 |
| 约束 | 候选项位置使用屏幕坐标系；面板尺寸用于归一化坐标转换 |
| 关键属性 | candidatePositions, panelSize |
| 所属包 | candidate |

CandidateListLayoutState 用于坐标解析和输入动作播放期间指示器定位。

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
     *
     * @param index 候选项索引
     * @return 候选项中心坐标，若索引越界则返回 null
     */
    fun locateItem(index: Int): Offset? {
        return candidatePositions.getOrNull(index)?.center
    }
}
```

### 3.7 InputListLayoutState

| 属性 | 说明 |
|------|------|
| 角色 | 输入列表布局状态 |
| 职责 | 承载输入项位置信息，提供 locateItem() 定位方法 |
| 约束 | 输入项位置使用屏幕坐标系；面板尺寸用于归一化坐标转换 |
| 关键属性 | itemPositions, panelSize |
| 所属包 | input |

InputListLayoutState 用于坐标解析和输入动作播放期间指示器定位。

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
     *
     * @param index 输入项索引
     * @return 输入项中心坐标，若索引越界则返回 null
     */
    fun locateItem(index: Int): Offset? {
        return itemPositions.getOrNull(index)?.center
    }
}
```

### 3.8 CoordinateNormalizer

| 属性 | 说明 |
|------|------|
| 角色 | 坐标归一化工具 |
| 职责 | 在绝对坐标与归一化坐标之间进行双向转换 |
| 约束 | 归一化坐标范围 [0,1] x [0,1]，(0,0) 为左上角 |
| 关键方法 | normalize(), denormalize() |
| 所属包 | panel |

坐标归一化工具，在绝对像素坐标与归一化坐标之间进行双向转换。归一化坐标使得手势反馈数据可以跨面板、跨 Zone 使用，无需关心面板的实际像素尺寸。

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

/**
 * 归一化坐标点。
 */
data class OffsetF(val x: Float, val y: Float)

/** Offset 扩展：归一化到 [0,1] 范围 */
fun Offset.normalize(sourceSize: Size): OffsetF =
    CoordinateNormalizer.normalize(this, sourceSize)

/** OffsetF 扩展：反归一化为绝对坐标 */
fun OffsetF.denormalize(targetSize: Size): Offset =
    CoordinateNormalizer.denormalize(this, targetSize)
```

---

## 4. 面板共存规则

### 4.1 叠加与互斥共存

Zone B 三行结构中，不同行的面板组件遵循不同的共存规则。Row 1 的 CandidateListPanel 和 PopupTipPanel 采用叠加共存，二者共享同一空间，PopupTipPanel 短暂浮现覆盖在 CandidateListPanel 之上。Row 2 的 ToolListPanel 和 InputListPanel 采用互斥共存，同一时刻仅显示其中一个，由 ImeState.isInputting 状态控制切换。Row 3 的 KeyLayoutPanel、GestureFeedbackPanel 和 GestureInputPanel 采用叠加共存，三者始终同时存在，分别负责渲染、反馈和触摸，叠加顺序从底到顶为 KeyLayoutPanel -> GestureFeedbackPanel -> GestureInputPanel。在输入动作播放期间，Row 1 和 Row 2 的面板通过内建的 showIndicator 参数在自身绘制区域内叠加指示器动画，不再需要独立的覆盖层组件。

叠加共存与互斥共存的核心区别在于：叠加共存的面板同时渲染在同一空间，通过透明度和 Z 轴顺序实现视觉分层；互斥共存的面板同一时刻仅渲染其中一个，切换时存在短暂的进入/退出动画。Row 1 的叠加方式允许 PopupTipPanel 在不影响 CandidateListPanel 布局的情况下短暂浮现，提示消失后候选列表自然可见。Row 2 的互斥方式确保工具栏和输入栏不会视觉冲突，用户在输入时看到输入内容，空闲时看到工具选项。Row 3 的叠加方式延续了三层分离设计，确保触摸/反馈/渲染三层的独立性。指示器动画通过面板内建的 showIndicator 参数控制在面板内部绘制，与面板的常规渲染内容叠加共存，不影响其他行的布局。

### 4.2 实例策略表

不同 LayoutMode 下，组件实例的部署位置和数量不同。KeyLayoutPanel 作为单实例组件，在 Stacked 模式下部署在 Zone B Row 3，在 Separated 模式下部署在 Zone A。GestureFeedbackPanel 作为双实例组件，在 Stacked 模式下仅 Zone B 有一个实例，在 Separated 模式下 Zone A 和 Zone B 各有一个实例，分别绘制不同类型的反馈元素。GestureInputPanel 在两种模式下均仅存在于 Zone B Row 3。

| 组件 | Stacked 模式实例数 | Stacked 部署位置 | Separated 模式实例数 | Separated 部署位置 |
|---|---|---|---|---|
| KeyLayoutPanel | 1 | Zone B Row 3 | 1 | Zone A |
| GestureFeedbackPanel | 1 | Zone B Row 3 | 2 | Zone A (按键侧), Zone B Row 3 (输入侧) |
| GestureInputPanel | 1 | Zone B Row 3 | 1 | Zone B Row 3 中列 |
| CandidateListPanel | 1 | Zone B Row 1 | 1 | Zone B Row 1 |
| PopupTipPanel | 1 | Zone B Row 1 | 1 | Zone B Row 1 |
| ToolListPanel | 1 | Zone B Row 2 | 1 | Zone B Row 2 |
| InputListPanel | 1 | Zone B Row 2 | 1 | Zone B Row 2 |

LayoutMode 动态切换时的实例迁移策略如下。从 Stacked 切换到 Separated 时：KeyLayoutPanel 从 Zone B Row 3 迁移到 Zone A；GestureFeedbackPanel 从 Zone B 的单实例拆分为 Zone A 和 Zone B 的双实例，反馈元素按 SeparatedSet 配置重新分配；GestureInputPanel 保持在 Zone B 但从整行宽度收缩到中列宽度；Zone A 的容器被创建并填充内容。从 Separated 切换到 Stacked 时执行反向操作：KeyLayoutPanel 从 Zone A 迁移回 Zone B Row 3；GestureFeedbackPanel 的双实例合并为 Zone B 单实例，反馈元素按 StackedSet 配置合并；GestureInputPanel 从中列宽度扩展到整行宽度；Zone A 的容器被移除。切换过程中，所有状态通过 GestureFeedbackState 和 ImeState 保持连续，不丢失任何手势或输入进度信息。

---

## 5. GestureInputPanel（透明触摸层）

输入面板仅负责手势捕获，不绘制任何视觉反馈。它是透明的手势拦截层，叠加在按键面板之上。始终部署在 Row 3 的最上层，确保触摸事件优先到达。手势识别过程中，将触摸坐标归一化后写入 GestureFeedbackState，供 GestureFeedbackPanel 消费绘制。

| 属性 | 说明 |
|------|------|
| 角色 | Row 3 面板（顶层），触摸事件接收 |
| 职责 | 仅接收输入事件（透明触摸层），识别手势类型，输出 InputGesture，驱动 GestureFeedbackState |
| 约束 | 始终为最上层面板，不绘制任何视觉内容；仅在 Zone B 中存在实例 |
| 关键属性 | keyLayoutState, inputMode, keyboardType, feedbackState, onGesture |
| 所属包 | panel |

```kotlin
/**
 * 手势输入面板，透明触摸层。
 *
 * 仅负责接收用户手势并识别为 InputGesture，不绘制任何视觉反馈。
 * 始终部署在 Row 3 的最上层，确保触摸事件优先到达。
 *
 * 手势识别过程中，将触摸坐标归一化后写入 GestureFeedbackState，
 * 供 GestureFeedbackPanel 消费绘制。归一化坐标基于 GestureInputPanel
 * 自身的尺寸计算：normalizedX = eventX / panelWidth,
 * normalizedY = eventY / panelHeight。
 *
 * 在 Separated 模式下，GestureInputPanel 仅存在于 Zone B Row 3 中列，
 * 其触摸点通过 keyLayoutState 映射到 Zone A 的 KeyLayoutPanel 进行按键定位。
 */
@Composable
fun GestureInputPanel(
    keyLayoutState: KeyLayoutState,
    inputMode: InputMode,
    keyboardType: Keyboard.Type,
    feedbackState: GestureFeedbackState,
    onGesture: (InputGesture) -> Unit,
    modifier: Modifier = Modifier,
) {
    // 纯手势检测（透明层，无绘制）
    GestureDetectorLayer(
        keyLayoutState = keyLayoutState,
        inputMode = inputMode,
        keyboardType = keyboardType,
        feedbackState = feedbackState,
        onGesture = onGesture,
        modifier = modifier.fillMaxSize(),
    )
}
```

### 5.1 手势检测层

```kotlin
/**
 * 手势检测层。
 *
 * 透明的全尺寸层，拦截所有触摸事件。
 * 根据触摸位置查询 KeyLayoutState 定位目标按键和手势类型。
 * 手势过程中同步更新 GestureFeedbackState 供反馈面板消费。
 * 触摸坐标在写入 feedbackState 时归一化为 OffsetF。
 */
@Composable
fun GestureDetectorLayer(
    keyLayoutState: KeyLayoutState,
    inputMode: InputMode,
    keyboardType: Keyboard.Type,
    feedbackState: GestureFeedbackState,
    onGesture: (InputGesture) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current

    // 获取面板尺寸用于坐标归一化
    var panelSize by remember { mutableStateOf(Size.Zero) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { coordinates ->
                panelSize = coordinates.size.toSize()
            }
            .pointerInput(keyLayoutState, inputMode, keyboardType) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val downTime = System.currentTimeMillis()

                    // 查找按下位置的按键（使用 keyLayoutState 的归一化坐标）
                    val downKey = keyLayoutState.findKeyAt(down.position, panelSize)

                    when {
                        // X-Pad 模式：区域路径检测
                        inputMode == InputMode.XPad -> {
                            handleXPadGesture(
                                downPosition = down.position,
                                panelSize = panelSize,
                                keyLayoutState = keyLayoutState,
                                inputMode = inputMode,
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
                                panelSize = panelSize,
                                keyLayoutState = keyLayoutState,
                                inputMode = inputMode,
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

### 5.2 标准按键手势处理

```kotlin
/**
 * 标准按键手势处理。
 *
 * 处理按键面板上的点击、长按和滑行手势。
 * 通过 keyLayoutState 查询触摸位置对应的按键，
 * 同时更新 feedbackState 供反馈面板绘制视觉反馈。
 * 触摸坐标归一化后写入 feedbackState。
 */
private suspend fun PointerInputScope.handleStandardGesture(
    downPosition: Offset,
    downKey: InputKey,
    downTime: Long,
    panelSize: Size,
    keyLayoutState: KeyLayoutState,
    inputMode: InputMode,
    feedbackState: GestureFeedbackState,
    onGesture: (InputGesture) -> Unit,
    haptics: HapticFeedback,
) {
    var currentKey = downKey
    val visitedKeys = mutableListOf(downKey)
    var isSwiping = false
    var isLongPress = false

    // 初始反馈：按下按键高亮 + 触摸轨迹起点（归一化坐标）
    feedbackState.setPressedKeys(setOf(downKey))
    feedbackState.addTouchTrailPoint(downPosition.normalize(panelSize))

    // 长按检测协程
    val longPressJob = coroutineScope {
        launch {
            delay(ViewConfiguration.getLongPressTimeout().toLong())
            isLongPress = true
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            onGesture(InputGesture.LongPress(
                timestamp = System.currentTimeMillis(),
                inputMode = inputMode,
                key = downKey,
            ))
        }
    }

    do {
        val event = awaitPointerEvent()
        val position = event.changes.first().position

        // 更新触摸轨迹（归一化坐标）
        feedbackState.addTouchTrailPoint(position.normalize(panelSize))

        // 查找当前触摸位置的按键
        val keyAtPosition = keyLayoutState.findKeyAt(position, panelSize)
        if (keyAtPosition != null && keyAtPosition != currentKey) {
            // 进入新按键 -> 滑行
            longPressJob.cancel()
            isSwiping = true
            currentKey = keyAtPosition
            visitedKeys.add(keyAtPosition)

            // 更新反馈：当前按下的按键
            feedbackState.setPressedKeys(setOf(keyAtPosition))

            // 由 KeyLayoutPanel 根据 InputMode 计算按键间插值轨迹，
            // 将归一化坐标插值路径写入 touchTrailPoints
            // （此处简化，实际由 LayoutStrategy 计算）
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
            inputMode = inputMode,
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
            inputMode = inputMode,
            key = downKey,
        ))
    }
}
```

### 5.3 X-Pad 手势处理

```kotlin
/**
 * X-Pad 手势处理。
 *
 * X-Pad 模式下，手指在按键面板的 XPad 区域滑行。
 * 输入面板通过 keyLayoutState 查询触摸位置对应的 X-Pad 区域，
 * 同时更新 feedbackState 供反馈面板绘制输入轨迹。
 * 触摸坐标归一化后写入 feedbackState。
 */
private suspend fun PointerInputScope.handleXPadGesture(
    downPosition: Offset,
    panelSize: Size,
    keyLayoutState: KeyLayoutState,
    inputMode: InputMode,
    feedbackState: GestureFeedbackState,
    onGesture: (InputGesture) -> Unit,
    haptics: HapticFeedback,
) {
    var currentZone = keyLayoutState.xPadLayoutInfo?.findZoneAt(downPosition, panelSize)
    val path = mutableListOf<XPadZone>()

    if (currentZone != null) {
        path.add(currentZone)
    }

    // 触摸轨迹起点（归一化坐标）
    feedbackState.addTouchTrailPoint(downPosition.normalize(panelSize))

    do {
        val event = awaitPointerEvent()
        val position = event.changes.first().position
        val zone = keyLayoutState.xPadLayoutInfo?.findZoneAt(position, panelSize)

        // 更新触摸轨迹（归一化坐标）
        feedbackState.addTouchTrailPoint(position.normalize(panelSize))

        if (zone != null && zone != currentZone) {
            currentZone = zone
            path.add(zone)
            haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
        }
    } while (event.changes.any { it.pressed })

    // 手势结束，清除临时反馈
    feedbackState.clearAll()

    if (path.isNotEmpty()) {
        onGesture(InputGesture.XPadZonePath(
            timestamp = System.currentTimeMillis(),
            inputMode = inputMode,
            startZone = path.first(),
            path = path.toList(),
        ))
    }
}
```

---

## 6. GestureFeedbackPanel（透明反馈层）

### 6.1 核心概念

反馈面板是独立于输入面板和按键面板的透明绘制层，职责是渲染所有手势相关的临时视觉反馈。关键设计要点：

1. **独立性**：反馈面板不嵌入输入面板或按键面板，而是独立的 Composable，可以自由放置在布局的任意位置
2. **双实例**：Stacked 模式下仅 Zone B 有一个实例绘制所有反馈；Separated 模式下 Zone A 和 Zone B 各有一个实例，分别绘制按键侧和输入侧的反馈
3. **配置性**：每个反馈面板实例通过 `FeedbackElementType` 集合配置其绘制的反馈类型，从而在不同布局模式下灵活分配反馈到对应区域
4. **归一化坐标**：GestureFeedbackState 中的坐标数据以归一化形式 [0,1]x[0,1] 存储，GestureFeedbackPanel 在绘制时根据自身实际尺寸转换为像素坐标。这使得同一份反馈数据可以正确地在不同尺寸的面板实例上渲染
5. **状态驱动**：反馈面板从 `GestureFeedbackState` 读取数据，不直接与输入面板或按键面板交互

### 6.2 FeedbackElementType

简化后仅保留三种核心类型。KeyPath 和 XPadPathHighlight 已合并到 TouchTrail 中，因为按键间路径和 X-Pad 路径本质上都是输入轨迹的组成部分，由 KeyLayoutPanel 根据 InputMode 计算起止按键间的平滑曲线，统一作为 touchTrailPoints 写入 GestureFeedbackState。合并的理由：这三类路径本质上都是手指移动的轨迹，其视觉表现均为从起点到终点的曲线，仅曲线形状由 InputMode 决定（如 RectGrid 的直线、XPad 的弧线），没有必要作为独立反馈类型分别管理。

| 属性 | 说明 |
|------|------|
| 角色 | 反馈元素类型枚举 |
| 职责 | 定义反馈面板可绘制的视觉反馈种类 |
| 约束 | 仅三种类型（简化合并后） |
| 关键属性 | TouchTrail, KeyHighlight, FingerIndicator |
| 所属包 | feedback |

```kotlin
/**
 * 反馈元素类型。
 *
 * 简化后仅保留三种核心类型：
 * - TouchTrail：输入轨迹（包含按键间路径和 X-Pad 路径的平滑曲线）
 * - KeyHighlight：按键高亮（手势过程中按下的按键临时高亮）
 * - FingerIndicator：手指指示器（播放动画时显示手指位置）
 *
 * KeyPath 和 XPadPathHighlight 已合并到 TouchTrail 中，
 * 因为按键间路径和 X-Pad 路径本质上都是输入轨迹的组成部分，
 * 由 KeyLayoutPanel 根据 InputMode 计算起止按键间的平滑曲线，
 * 统一作为 touchTrailPoints 写入 GestureFeedbackState。
 * 这三类路径本质上都是手指移动的轨迹，其视觉表现均为
 * 从起点到终点的曲线，仅曲线形状由 InputMode 决定
 * （如 RectGrid 的直线、XPad 的弧线），
 * 没有必要作为独立反馈类型分别管理。
 */
enum class FeedbackElementType {
    /** 输入轨迹，包含按键间路径和 X-Pad 路径的平滑曲线 */
    TouchTrail,
    /** 按键高亮，手势过程中按下的按键临时高亮 */
    KeyHighlight,
    /** 手指指示器，播放动画时显示手指位置 */
    FingerIndicator,
}
```

### 6.3 GestureFeedbackPanel 实现

```kotlin
/**
 * 手势反馈面板，透明绘制层，支持多实例。
 *
 * 根据 GestureFeedbackState 绘制指定类型的手势视觉反馈。
 * 使用归一化坐标，绘制时根据面板实际尺寸转换为像素坐标。
 * 不处理任何触摸事件，不依赖输入面板或按键面板的 Canvas。
 *
 * **双实例部署**：
 * - Stacked 模式：仅 Zone B 一个实例，绘制所有反馈元素
 * - Separated 模式：Zone A 实例绘制按键侧反馈（KeyHighlight, TouchTrail），
 *   Zone B 实例绘制输入侧反馈（TouchTrail, FingerIndicator）
 *
 * @param elements 该实例绘制的反馈元素类型集合
 * @param feedbackState 手势反馈状态（归一化坐标）
 * @param keyLayoutState 按键布局状态（归一化坐标），用于按键定位
 */
@Composable
fun GestureFeedbackPanel(
    elements: Set<FeedbackElementType>,
    feedbackState: GestureFeedbackState,
    keyLayoutState: KeyLayoutState,
    modifier: Modifier = Modifier,
) {
    if (elements.isEmpty()) return

    // 从 feedbackState 收集归一化坐标数据
    val touchTrailPoints by feedbackState.touchTrailPoints.collectAsState()
    val pressedKeys by feedbackState.pressedKeys.collectAsState()
    val fingerIndicator by feedbackState.fingerIndicator.collectAsState()

    Canvas(modifier = modifier.fillMaxSize()) {
        val panelSize = size

        // 输入轨迹（包含按键间路径和 X-Pad 路径）：归一化坐标 -> 像素坐标
        // 按键间路径和 X-Pad 路径已合并为输入轨迹的一部分，
        // 由 KeyLayoutPanel 根据 InputMode 计算轨迹形状后
        // 作为 touchTrailPoints 中的平滑曲线点写入
        if (FeedbackElementType.TouchTrail in elements && touchTrailPoints.size >= 2) {
            val pixelPoints = touchTrailPoints.map { it.denormalize(panelSize) }
            drawTouchTrail(pixelPoints)
        }

        // 按键高亮：归一化坐标 -> 像素坐标
        if (FeedbackElementType.KeyHighlight in elements && pressedKeys.isNotEmpty()) {
            drawKeyHighlights(pressedKeys, keyLayoutState, panelSize)
        }

        // 手指指示器：归一化坐标 -> 像素坐标
        if (FeedbackElementType.FingerIndicator in elements && fingerIndicator != null) {
            val indicator = fingerIndicator!!
            val pixelPosition = indicator.position.denormalize(panelSize)
            drawFingerIndicator(indicator.copy(position = pixelPosition))
        }
    }
}
```

### 6.4 GestureFeedbackPanelSet

反馈面板配置集，定义当前布局模式下所需的反馈面板实例及其元素类型分配。Stacked 模式使用 StackedSet（单实例绘制所有反馈），Separated 模式使用 SeparatedSet（双实例分别绘制按键侧和输入侧反馈）。

| 属性 | 说明 |
|------|------|
| 角色 | 反馈面板配置集 |
| 职责 | 定义不同布局模式下的反馈面板实例和元素类型分配 |
| 约束 | Stacked 模式单实例，Separated 模式双实例 |
| 关键属性 | StackedSet.allElements, SeparatedSet.keySideElements/inputSideElements |
| 所属包 | feedback |

```kotlin
/**
 * 反馈面板配置集。
 *
 * 定义当前布局模式下所需的反馈面板实例及其元素类型分配。
 * 不同布局模式有不同的配置集。
 */
sealed class GestureFeedbackPanelSet {

    /**
     * 堆叠模式配置：Zone B 单实例绘制所有反馈。
     *
     * 在 Stacked 模式下，三层面板完全重叠，
     * 一个反馈面板即可覆盖所有反馈绘制。
     */
    data object StackedSet : GestureFeedbackPanelSet() {
        val allElements: Set<FeedbackElementType> = setOf(
            FeedbackElementType.TouchTrail,
            FeedbackElementType.KeyHighlight,
            FeedbackElementType.FingerIndicator,
        )
    }

    /**
     * 分离模式配置：Zone A 和 Zone B 各一个实例。
     *
     * Zone A（按键侧）反馈元素：按键高亮 + 输入轨迹
     * Zone B（输入侧）反馈元素：输入轨迹 + 手指指示器
     */
    data class SeparatedSet(
        /** Zone A（按键侧）反馈元素 */
        val keySideElements: Set<FeedbackElementType> = setOf(
            FeedbackElementType.KeyHighlight,
            FeedbackElementType.TouchTrail,
        ),
        /** Zone B（输入侧）反馈元素 */
        val inputSideElements: Set<FeedbackElementType> = setOf(
            FeedbackElementType.TouchTrail,
            FeedbackElementType.FingerIndicator,
        ),
    ) : GestureFeedbackPanelSet()
}
```

### 6.5 绘制实现

```kotlin
// --- GestureFeedbackPanel 内的绘制辅助方法 ---

/**
 * 绘制输入轨迹（含按键间路径和 X-Pad 路径的平滑曲线）。
 *
 * 在手指移动路径上绘制平滑的贝塞尔曲线，
 * 包含实际触摸点和按键间插值路径点。
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
 * keyLayoutState 从归一化坐标转换为像素坐标。
 */
private fun DrawScope.drawKeyHighlights(
    keys: Set<InputKey>,
    keyLayoutState: KeyLayoutState,
    panelSize: Size,
) {
    keys.forEach { key ->
        val normalizedRect = keyLayoutState.keyPositions[key] ?: return@forEach
        val rect = keyLayoutState.denormalize(normalizedRect, panelSize)
        drawRoundRect(
            color = Color(0xFF2196F3).copy(alpha = 0.3f),
            topLeft = rect.topLeft,
            size = rect.size,
            cornerRadius = CornerRadius(4.dp.toPx()),
        )
    }
}

/**
 * 绘制手指指示器（程序化输入动画用）。
 *
 * 半透明圆形，跟随指定位置移动，
 * 按下时缩小并加深颜色。
 */
private fun DrawScope.drawFingerIndicator(state: InputActionFingerIndicator) {
    if (!state.visible) return

    val position = state.position
    // position 已在外层反归一化为像素坐标
    val radius = 24.dp.toPx()
    val scale = if (state.pressed) 0.8f else 1.0f

    // 阴影
    drawCircle(
        color = Color.Black.copy(alpha = 0.2f),
        radius = radius * scale + 4.dp.toPx(),
        center = Offset(position.x, position.y + 2.dp.toPx()),
    )

    // 手指圆圈
    drawCircle(
        color = if (state.pressed) {
            Color(0xFF2196F3).copy(alpha = 0.6f)
        } else {
            Color(0xFF2196F3).copy(alpha = 0.3f)
        },
        radius = radius * scale,
        center = position,
    )

    // 中心点
    drawCircle(
        color = Color.White.copy(alpha = 0.8f),
        radius = 4.dp.toPx(),
        center = position,
    )
}

private fun midpoint(a: Offset, b: Offset): Offset =
    Offset((a.x + b.x) / 2, (a.y + b.y) / 2)
```

---

## 7. KeyLayoutPanel（按键布局层）

### 7.1 核心概念

KeyLayoutPanel 是按键布局渲染层，强调其布局渲染职责而非仅网格渲染。根据 InputMode 和 Keyboard.Type 动态选择布局策略，渲染按键的常规外观和持续性状态。不处理触摸事件，不绘制手势反馈（由 GestureFeedbackPanel 负责）。

KeyLayoutPanel 是单实例组件——在任意时刻仅存在一个实例，根据 LayoutMode 决定部署位置：Stacked 模式下部署在 Zone B Row 3，Separated 模式下部署在 Zone A。

InputMode 与 Keyboard.Type 正交组合：任意 InputMode 可与任意 Type 组合，通过 LayoutStrategy 分发不同的布局策略实现。

| 属性 | 说明 |
|------|------|
| 角色 | Row 3 面板，按键布局渲染 |
| 职责 | 根据 InputMode 和 Keyboard.Type 动态切换按键布局并渲染按键，提供布局状态供其他面板查询 |
| 约束 | 单实例：同一时刻仅存在于 Zone A 或 Zone B 之一；InputMode x Type 正交组合 |
| 关键属性 | inputMode, keyboardType, onLayoutStateChanged, 计算归一化坐标 |
| 所属包 | panel |

### 7.2 KeyLayoutPanel 实现

```kotlin
/**
 * 按键布局面板。
 *
 * 根据 InputMode 和 Keyboard.Type 动态选择布局策略，
 * 渲染按键的常规外观和持续性状态。
 * 不处理触摸事件，不绘制手势反馈（由 GestureFeedbackPanel 负责）。
 *
 * **单实例约束**：KeyLayoutPanel 在任意时刻仅存在一个实例，
 * 根据 LayoutMode 决定部署位置：
 * - Stacked 模式：部署在 Zone B Row 3
 * - Separated 模式：部署在 Zone A
 *
 * **InputMode x Type 正交组合**：
 * 任意 InputMode 可与任意 Type 组合，通过 LayoutStrategy 分发：
 * - XPad + Pinyin -> XPadPinyinLayout
 * - RectGrid + Pinyin -> RectGridPinyinLayout
 * - HexGrid + Number -> HexGridNumberLayout
 * - ... 其他组合类似
 */
@Composable
fun KeyLayoutPanel(
    inputMode: InputMode,
    keyboardType: Keyboard.Type,
    keyGrid: List<List<InputKey>>,
    keyboardState: KeyboardState,
    onLayoutStateChanged: (KeyLayoutState) -> Unit,
    modifier: Modifier = Modifier,
) {
    val layoutStrategy = remember(inputMode, keyboardType) {
        LayoutStrategy.resolve(inputMode, keyboardType)
    }

    layoutStrategy.Layout(
        keyGrid = keyGrid,
        keyboardState = keyboardState,
        onLayoutStateChanged = onLayoutStateChanged,
        modifier = modifier,
    )
}
```

### 7.3 LayoutStrategy

```kotlin
/**
 * 布局策略，根据 InputMode x Type 组合分发。
 *
 * 每种 InputMode x Type 组合对应一个 LayoutStrategy 实现，
 * 负责按键的几何排列和 KeyLayoutState 的计算。
 * 策略内部将按键位置归一化后写入 KeyLayoutState，
 * 供 GestureInputPanel 和 GestureFeedbackPanel 查询。
 */
interface LayoutStrategy {
    @Composable
    fun Layout(
        keyGrid: List<List<InputKey>>,
        keyboardState: KeyboardState,
        onLayoutStateChanged: (KeyLayoutState) -> Unit,
        modifier: Modifier,
    )

    companion object {
        /**
         * 根据 InputMode 和 Type 解析对应的布局策略。
         *
         * 任意 InputMode 可与任意 Type 组合，不存在互斥关系。
         */
        fun resolve(inputMode: InputMode, type: Keyboard.Type): LayoutStrategy {
            return when (inputMode) {
                InputMode.XPad -> XPadLayoutStrategy(type)
                InputMode.HexGrid -> HexGridLayoutStrategy(type)
                InputMode.RectGrid -> RectGridLayoutStrategy(type)
                InputMode.MultiZone -> MultiZoneLayoutStrategy(type)
            }
        }
    }
}
```

### 7.4 KeyLayoutState 详细说明

KeyLayoutState 是按键布局的核心状态对象，包含按键位置映射和 X-Pad 布局信息，供 GestureInputPanel 和 GestureFeedbackPanel 查询。位置信息使用归一化坐标 [0,1]x[0,1] 存储，绘制时根据面板实际尺寸转换为像素坐标。

```kotlin
/**
 * 按键布局状态。
 *
 * 包含按键位置映射和 X-Pad 布局信息，
 * 供 GestureInputPanel 和 GestureFeedbackPanel 查询。
 * 位置信息使用归一化坐标 [0,1] x [0,1] 存储，
 * 绘制时根据面板实际尺寸转换为像素坐标。
 *
 * 由 KeyLayoutPanel 通过 onLayoutStateChanged 回调更新，
 * 由 KeyboardViewModel 缓存供 InputActionPositionResolver 使用。
 */
data class KeyLayoutState(
    /** 按键归一化位置映射（归一化坐标） */
    val keyPositions: Map<InputKey, RectF> = emptyMap(),
    /** X-Pad 布局信息（仅 X-Pad 模式） */
    val xPadLayoutInfo: XPadLayoutInfo? = null,
    /** 面板实际尺寸（像素），用于归一化坐标转换 */
    val panelSize: Size = Size.Zero,
) {
    /**
     * 将归一化矩形转换为指定面板尺寸下的像素矩形。
     *
     * 用于 GestureFeedbackPanel 将归一化按键位置转换为像素坐标，
     * 以便在 Canvas 上绘制按键高亮。
     */
    fun denormalize(normalized: RectF, targetSize: Size): Rect {
        return Rect(
            left = normalized.left * targetSize.width,
            top = normalized.top * targetSize.height,
            right = normalized.right * targetSize.width,
            bottom = normalized.bottom * targetSize.height,
        )
    }

    /**
     * 查找指定位置（像素坐标）对应的按键。
     *
     * 用于 GestureInputPanel 根据触摸位置查找目标按键。
     * 将像素坐标与归一化按键矩形比较，
     * 先将归一化矩形反归一化为目标尺寸下的像素矩形，
     * 再判断像素坐标是否落在矩形内。
     *
     * @param position 触摸位置（像素坐标）
     * @param targetSize 面板尺寸（像素）
     * @return 目标按键，若位置不在任何按键区域内则返回 null
     */
    fun findKeyAt(position: Offset, targetSize: Size): InputKey? {
        return keyPositions.entries.firstOrNull { (_, rect) ->
            denormalize(rect, targetSize).contains(position)
        }?.key
    }
}

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

data class OffsetF(val x: Float, val y: Float)
```

### 7.5 KeyView

按键面板中的 `KeyView` 不处理触摸事件，也不绘制手势反馈，只负责按键的常规状态渲染。按键的"按下"视觉状态由 keyboardState 驱动（持续性状态），手势触发的临时高亮由 GestureFeedbackPanel 绘制。

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

---

## 8. CandidateListPanel（候选列表面板）

### 8.1 实现

CandidateListPanel 展示多个可补全输入、可粘贴内容等候选项，支持水平滚动浏览和点击选择。部署在 Zone B Row 1，与 PopupTipPanel 叠加共享空间。在输入动作播放期间，可通过 locateItem() 方法获取指定候选项的中心坐标，用于绘制指示器动画。指示器通过 showIndicator 参数内建绘制，无需外部覆盖层。

| 属性 | 说明 |
|------|------|
| 角色 | Row 1 面板，展示候选列表 |
| 职责 | 展示候选项，支持滚动和选择；内建行指示器动画 |
| 约束 | 始终部署在 Zone B Row 1，与 PopupTipPanel 叠加共享空间 |
| 关键属性 | state, onCandidateSelected, showIndicator, indicatorState |
| 所属包 | candidate |

```kotlin
/**
 * 候选列表面板。
 *
 * 展示多个可补全输入、可粘贴内容等候选项。
 * 支持水平滚动浏览候选项，点击选择候选项提交输入。
 * 部署在 Zone B Row 1，与 PopupTipPanel 叠加共享空间。
 *
 * 在输入动作播放期间，可通过 locateItem() 方法
 * 获取指定候选项的中心坐标，用于绘制指示器动画。
 * 指示器通过 showIndicator 参数内建绘制，无需外部覆盖层。
 */
@Composable
fun CandidateListPanel(
    state: CandidateListState,
    onCandidateSelected: (Candidate) -> Unit,
    modifier: Modifier = Modifier,
    showIndicator: Boolean = false,
    indicatorState: InputActionFingerIndicator? = null,
) {
    Box(modifier = modifier.fillMaxSize()) {
        LazyRow(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            contentPadding = PaddingValues(horizontal = 8.dp),
        ) {
            itemsIndexed(state.candidates) { index, candidate ->
                CandidateItem(
                    candidate = candidate,
                    isSelected = index == state.selectedIndex,
                    onClick = { onCandidateSelected(candidate) },
                )
            }
        }

        // 内建行指示器
        if (showIndicator && indicatorState != null) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val pixelPosition = indicatorState.position.denormalize(size)
                drawCircle(
                    color = Color(0xFF2196F3).copy(alpha = 0.5f),
                    radius = 20.dp.toPx(),
                    center = pixelPosition,
                )
            }
        }
    }
}
```

### 8.2 CandidateListLayoutState

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
     *
     * @param index 候选项索引
     * @return 候选项中心坐标，若索引越界则返回 null
     */
    fun locateItem(index: Int): Offset? {
        return candidatePositions.getOrNull(index)?.center
    }
}
```

---

## 9. PopupTipPanel（弹出提示面板）

PopupTipPanel 短暂显示操作信息（如按键操作结果、已输入字符、功能切换提示等），覆盖在 CandidateListPanel 上方。显示后经短暂延时自动消失，不遮挡候选列表的持续使用。

PopupTipPanel 从 ImeState 读取弹出提示状态。弹出提示是引擎处理意图后更新 ImeState 触发的，属于输入状态变化而非视觉反馈，因此由 ImeState 管理。

| 属性 | 说明 |
|------|------|
| 角色 | Row 1 面板，短暂展示操作提示 |
| 职责 | 短暂显示操作信息、已输入字符等提示，覆盖在 CandidateListPanel 上方 |
| 约束 | 与 CandidateListPanel 叠加共享 Row 1 空间，仅短暂显示后自动消失 |
| 关键属性 | state: ImeState |
| 所属包 | panel |

```kotlin
/**
 * 弹出提示面板。
 *
 * 短暂显示操作信息（如按键操作结果、已输入字符、功能切换提示等），
 * 覆盖在 CandidateListPanel 上方。
 * 显示后经短暂延时自动消失，不遮挡候选列表的持续使用。
 *
 * 从 ImeState 读取弹出提示状态。
 * 弹出提示是引擎处理意图后更新 ImeState 触发的，
 * 属于输入状态变化而非视觉反馈，
 * 因此由 ImeState 管理。
 */
@Composable
fun PopupTipPanel(
    state: ImeState,
    modifier: Modifier = Modifier,
) {
    val tipState = state.popupTip

    AnimatedVisibility(
        visible = tipState != null,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
        modifier = modifier.fillMaxSize(),
    ) {
        tipState?.let { tip ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = tip.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}
```

---

## 10. ToolListPanel（工具列表面板）

ToolListPanel 空闲时展示固定 ToolItem 按钮（剪贴板粘贴、收藏管理、设置、键盘切换等），输入时收缩为仅显示切换按钮。与 InputListPanel 互斥共享 Row 2 空间，由 ImeState.isInputting 控制切换。

Editor 类型的编辑功能键（如全选、复制、粘贴、剪切、撤销、重做等）由 ToolListPanel 统一管理，作为 ToolItem 展示。编辑功能在任何键盘类型下均可通过工具栏快速访问。

| 属性 | 说明 |
|------|------|
| 角色 | Row 2 面板（空闲时），展示工具按钮（含 Editor 类型的编辑功能键） |
| 职责 | 空闲时展示固定 ToolItem 按钮，输入时仅显示切换按钮；内建行指示器动画 |
| 约束 | 与 InputListPanel 互斥共享 Row 2 空间，由 isInputting 状态控制切换 |
| 关键属性 | state, onToolSelected, showIndicator, indicatorState |
| 所属包 | panel |

```kotlin
/**
 * 工具列表面板。
 *
 * 空闲时展示固定 ToolItem 按钮（剪贴板粘贴、收藏管理、设置、键盘切换等），
 * 输入时收缩为仅显示切换按钮（从 InputListPanel 切回 ToolListPanel）。
 * 与 InputListPanel 互斥共享 Row 2 空间，由 ImeState.isInputting 控制切换。
 *
 * **Editor 功能说明**：编辑功能键（如全选、复制、粘贴、剪切、撤销、重做等）
 * 由 ToolListPanel 统一管理，作为 ToolItem 展示。
 * 编辑功能在任何键盘类型下均可通过工具栏快速访问。
 */
@Composable
fun ToolListPanel(
    state: ToolListState,
    onToolSelected: (ImeIntent) -> Unit,
    modifier: Modifier = Modifier,
    showIndicator: Boolean = false,
    indicatorState: InputActionFingerIndicator? = null,
) {
    Box(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            state.tools.forEach { tool ->
                ToolItem(
                    tool = tool,
                    onClick = { onToolSelected(tool.intent) },
                )
            }
        }

        // 内建行指示器
        if (showIndicator && indicatorState != null) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val pixelPosition = indicatorState.position.denormalize(size)
                drawCircle(
                    color = Color(0xFF2196F3).copy(alpha = 0.5f),
                    radius = 20.dp.toPx(),
                    center = pixelPosition,
                )
            }
        }
    }
}

/**
 * 工具项状态。
 */
data class ToolItem(
    val label: String,
    val icon: ImageVector?,
    val intent: ImeIntent,
)

data class ToolListState(
    val tools: List<ToolItem>,
)
```

---

## 11. InputListPanel（输入列表面板）

### 11.1 实现

InputListPanel 展示当前输入的字符序列，支持点击间隙移动光标、选择已输入文本进行修改、从候选列表中选择替换。与 ToolListPanel 互斥共享 Row 2 空间。提供 locateItem() 方法，用于 InputActionPlayer 在播放期间定位输入项的中心坐标，绘制指示器动画。指示器通过 showIndicator 参数内建绘制，无需外部覆盖层。

| 属性 | 说明 |
|------|------|
| 角色 | Row 2 面板（输入时），展示当前输入 |
| 职责 | 展示当前输入内容，支持选择已输入文本进行修改或选择候选；内建行指示器动画 |
| 约束 | 与 ToolListPanel 互斥共享 Row 2 空间；提供 locateItem() 方法供播放器定位 |
| 关键属性 | state, onGapTapped, onItemSelected, showIndicator, indicatorState |
| 所属包 | input |

```kotlin
/**
 * 输入列表面板。
 *
 * 展示当前输入的字符序列，支持点击间隙移动光标、
 * 选择已输入文本进行修改、从候选列表中选择替换。
 * 与 ToolListPanel 互斥共享 Row 2 空间。
 *
 * 提供 locateItem() 方法，用于 InputActionPlayer 在播放期间
 * 定位输入项的中心坐标，绘制指示器动画。
 * 指示器通过 showIndicator 参数内建绘制，无需外部覆盖层。
 */
@Composable
fun InputListPanel(
    state: InputListState,
    onGapTapped: (Int) -> Unit,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    showIndicator: Boolean = false,
    indicatorState: InputActionFingerIndicator? = null,
) {
    Box(modifier = modifier.fillMaxSize()) {
        LazyRow(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(0.dp),
            contentPadding = PaddingValues(horizontal = 8.dp),
        ) {
            itemsIndexed(state.items) { index, item ->
                when (item) {
                    is InputListItem.CharItem -> CharInputItem(
                        item = item,
                        onClick = { onItemSelected(index) },
                    )
                    is InputListItem.GapItem -> GapInputItem(
                        item = item,
                        onClick = { onGapTapped(index) },
                    )
                }
            }
        }

        // 内建行指示器
        if (showIndicator && indicatorState != null) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val pixelPosition = indicatorState.position.denormalize(size)
                drawCircle(
                    color = Color(0xFF2196F3).copy(alpha = 0.5f),
                    radius = 20.dp.toPx(),
                    center = pixelPosition,
                )
            }
        }
    }
}
```

### 11.2 InputListLayoutState

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
     *
     * @param index 输入项索引
     * @return 输入项中心坐标，若索引越界则返回 null
     */
    fun locateItem(index: Int): Offset? {
        return itemPositions.getOrNull(index)?.center
    }
}
```

---

## 12. KeyboardHost（集成组件）

### 12.1 实现

KeyboardHost 是顶层集成组件，根据 LayoutMode 组合 Zone A / Zone B 的面板组件，提供完整输入交互 UI，支持动态切换布局模式。KeyLayoutPanel 为单实例，同一时刻仅存在于 Zone A 或 Zone B 之一。KeyboardHost 提供「即插即用」的完整输入法 UI，第三方应用只需创建 KeyboardViewModel 并传入 KeyboardHost 即可获得完整的输入法界面。

| 属性 | 说明 |
|------|------|
| 角色 | 顶层集成组件 |
| 职责 | 根据 LayoutMode 组合 Zone A / Zone B 的面板组件，提供完整输入交互 UI，支持动态切换布局模式 |
| 约束 | KeyLayoutPanel 为单实例，同一时刻仅存在于 Zone A 或 Zone B 之一 |
| 关键属性 | layoutMode: State\<LayoutMode\>, viewModel: KeyboardViewModel |
| 所属包 | integration |

```kotlin
/**
 * 键盘宿主组件，顶层集成组件。
 *
 * 通过 LayoutMode 参数统一堆叠和分离两种布局模式的入口。
 * 支持运行时动态切换 LayoutMode，切换时按实例策略表重新部署组件。
 *
 * KeyboardHost 提供「即插即用」的完整输入法 UI，第三方应用只需：
 * ```kotlin
 * val viewModel: KeyboardViewModel = viewModel(factory = KeyboardViewModel.Factory(engine))
 * KeyboardHost(viewModel = viewModel)
 * ```
 */
@Composable
fun KeyboardHost(
    viewModel: KeyboardViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val layoutMode by viewModel.layoutMode.collectAsState()
    val feedbackState = viewModel.feedbackState
    var keyLayoutState by remember { mutableStateOf(KeyLayoutState()) }

    KeyboardTheme(themeType = state.config.ui.themeType) {
        when (layoutMode) {
            is LayoutMode.Stacked -> StackedLayout(viewModel, state, feedbackState, keyLayoutState)
            is LayoutMode.Separated -> SeparatedLayout(
                viewModel, state, feedbackState, keyLayoutState, layoutMode as LayoutMode.Separated
            )
        }
    }
}
```

### 12.2 StackedLayout

堆叠模式布局，所有组件集中在 Zone B 内，KeyLayoutPanel 部署在 Zone B Row 3。三层面板叠加共享同一空间，GestureFeedbackPanel 使用 StackedSet 配置绘制所有反馈元素。

```kotlin
/**
 * 堆叠模式布局。
 *
 * 所有组件集中在 Zone B 内，KeyLayoutPanel 部署在 Zone B Row 3。
 * Zone A 不使用（高度为 0），Zone B 占据全部 IME 屏幕空间。
 */
@Composable
private fun StackedLayout(
    viewModel: KeyboardViewModel,
    state: ImeState,
    feedbackState: GestureFeedbackState,
    keyLayoutState: KeyLayoutState,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Row 1: 候选栏 + 提示弹窗（叠加）
        Box(modifier = Modifier.weight(RowWeight.R1)) {
            CandidateListPanel(
                state = state.candidateList,
                onCandidateSelected = { viewModel.handleIntent(ImeIntent.SelectCandidate(it)) },
            )
            PopupTipPanel(state = state)
        }

        // Row 2: 工具栏 / 输入栏（互斥）
        Box(modifier = Modifier.weight(RowWeight.R2)) {
            if (state.isInputting) {
                InputListPanel(
                    state = state.inputList,
                    onGapTapped = { viewModel.handleIntent(ImeIntent.MoveCursorTo(it)) },
                    onItemSelected = { viewModel.handleIntent(ImeIntent.SelectInputItem(it)) },
                )
            } else {
                ToolListPanel(
                    state = state.toolList,
                    onToolSelected = { viewModel.handleIntent(it) },
                )
            }
        }

        // Row 3: 三层面板叠加
        Box(modifier = Modifier.weight(RowWeight.R3)) {
            KeyLayoutPanel(
                inputMode = state.inputMode,
                keyboardType = state.keyboardType,
                keyGrid = state.keyGrid,
                keyboardState = state.keyboardState,
                onLayoutStateChanged = { keyLayoutState = it },
            )
            GestureFeedbackPanel(
                elements = GestureFeedbackPanelSet.StackedSet.allElements,
                feedbackState = feedbackState,
                keyLayoutState = keyLayoutState,
            )
            GestureInputPanel(
                keyLayoutState = keyLayoutState,
                inputMode = state.inputMode,
                keyboardType = state.keyboardType,
                feedbackState = feedbackState,
                onGesture = { viewModel.handleGesture(it) },
            )
        }
    }
}
```

### 12.3 SeparatedLayout

分离模式布局，输入区域占据 Zone B，按键展示区域占据 Zone A。Zone A 部署 KeyLayoutPanel 和 GestureFeedbackPanel（按键侧），Zone B 包含三行结构，Row 3 为三列布局。

```kotlin
/**
 * 分离模式布局。
 *
 * Zone A: KeyLayoutPanel + GestureFeedbackPanel（按键侧反馈）
 * Zone B: 三行结构，Row 3 为三列布局
 */
@Composable
private fun SeparatedLayout(
    viewModel: KeyboardViewModel,
    state: ImeState,
    feedbackState: GestureFeedbackState,
    keyLayoutState: KeyLayoutState,
    mode: LayoutMode.Separated,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Zone A：按键布局 + 按键侧反馈
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(mode.zoneARatio)
                .align(CenterHorizontally),
        ) {
            KeyLayoutPanel(
                inputMode = state.inputMode,
                keyboardType = state.keyboardType,
                keyGrid = state.keyGrid,
                keyboardState = state.keyboardState,
                onLayoutStateChanged = { keyLayoutState = it },
            )
            GestureFeedbackPanel(
                elements = GestureFeedbackPanelSet.SeparatedSet.keySideElements,
                feedbackState = feedbackState,
                keyLayoutState = keyLayoutState,
            )
        }

        // Zone B：三行结构
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f - mode.zoneARatio),
        ) {
            // Row 1: 候选栏 + 提示弹窗（叠加）
            Box(modifier = Modifier.weight(RowWeight.R1)) {
                CandidateListPanel(
                    state = state.candidateList,
                    onCandidateSelected = { viewModel.handleIntent(ImeIntent.SelectCandidate(it)) },
                )
                PopupTipPanel(state = state)
            }

            // Row 2: 工具栏 / 输入栏（互斥）
            Box(modifier = Modifier.weight(RowWeight.R2)) {
                if (state.isInputting) {
                    InputListPanel(
                        state = state.inputList,
                        onGapTapped = { viewModel.handleIntent(ImeIntent.MoveCursorTo(it)) },
                        onItemSelected = { viewModel.handleIntent(ImeIntent.SelectInputItem(it)) },
                    )
                } else {
                    ToolListPanel(
                        state = state.toolList,
                        onToolSelected = { viewModel.handleIntent(it) },
                    )
                }
            }

            // Row 3: 三列布局
            Row(modifier = Modifier.weight(RowWeight.R3)) {
                // 左列：功能按钮
                LeftFunctionColumn(
                    keyboardType = state.keyboardType,
                    onAction = { viewModel.handleIntent(it) },
                    modifier = Modifier.weight(1f),
                )

                // 中列：手势交互（GestureFeedbackPanel + GestureInputPanel 叠加）
                Box(modifier = Modifier.weight(3f)) {
                    GestureFeedbackPanel(
                        elements = GestureFeedbackPanelSet.SeparatedSet.inputSideElements,
                        feedbackState = feedbackState,
                        keyLayoutState = keyLayoutState,
                    )
                    GestureInputPanel(
                        keyLayoutState = keyLayoutState,
                        inputMode = state.inputMode,
                        keyboardType = state.keyboardType,
                        feedbackState = feedbackState,
                        onGesture = { viewModel.handleGesture(it) },
                    )
                }

                // 右列：功能按钮
                RightFunctionColumn(
                    keyboardType = state.keyboardType,
                    onAction = { viewModel.handleIntent(it) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}
```

---

```plantuml
@file:../diagrams/ui-data-flow.puml
```
