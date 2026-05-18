# UI 组件层次设计

本文档定义 kuaizi IME v4 版本的 UI 组件层次结构，是对 [010-ui-library-overview.md](010-ui-library-overview.md) 中组件层次的重新设计，也是对 [020-panel-separation.md](020-panel-separation.md) 面板三层分离模型的扩展与重命名。核心变更包括：引入 Zone A / Zone B 屏幕分区模型，将布局模式从 Overlay/FullScreen 重命名为 Stacked/Separated，引入 Zone B 三行结构，新增 KeyboardHost 作为统一集成组件，新增 KeyboardInputActionPlayerHost 支持输入动作播放演示，以及引入归一化坐标体系和 IndicatorOverlay 机制。所有变更均保持与现有 `:ime-engine` 公开 API 的依赖隔离原则，第三方应用仍可替换或定制任意组件。本次迭代进一步简化了 GestureFeedbackState 的职责范围，将弹出提示移至 ImeState 管理，将按键路径与 X-Pad 路径合并到输入轨迹中，移除了 Editor 键盘类型，并对播放相关模型统一添加 InputAction 前缀，同时将行指示器内建到面板组件中以消除独立覆盖层。

---

## 1. 概述

### 1.1 设计目标

本次 UI 组件层次重设计的核心目标是建立一套清晰、灵活、可扩展的屏幕布局模型与组件层次体系，使得输入法界面能够同时支持堆叠（Stacked）与分离（Separated）两种布局模式，并在两种模式间无缝切换。堆叠模式下所有交互组件集中在屏幕下半区，适合单手操作和紧凑屏幕；分离模式下输入区域和按键区域分占屏幕上下两区，提供更宽的按键视野、避免手指遮挡、缩短输入路径。两种模式共享同一套组件定义和状态管理逻辑，仅在空间分配和实例部署策略上有所差异。

此外，本次设计新增了归一化坐标体系，使得手势反馈面板在不同 Zone 和不同尺寸下均能正确绘制轨迹与高亮；新增了 IndicatorOverlay 机制，使得输入动作播放时能够在任意面板行上精准绘制指示器动画；新增了 KeyboardInputActionPlayerHost 作为独立演示组件，支持 Animation 和 DirectInput 两种使用模式。这些新增能力均遵循「坐标无关」原则，与 [040-input-action-player.md](040-input-action-player.md) 和 [engine/060-input-action.md](../engine/060-input-action.md) 中的 InputAction 体系完全对齐。本次迭代还简化了 GestureFeedbackState，将其职责收窄为纯粹的视觉反馈（触摸轨迹、按键高亮、手指指示器），将弹出提示归还给 ImeState 管理，将按键间路径和 X-Pad 路径统一为输入轨迹的组成部分，使状态职责更加清晰。

### 1.2 与现有 v4 设计的关系

本文档是对现有 v4 设计的演进而非推翻。[010-ui-library-overview.md](010-ui-library-overview.md) 中的三层组件体系（Atomic / Panel / Integration）和主题系统保持不变，但 Integration 层的组件名从 KeyboardPanel/KeyboardScreen 统一为 KeyboardHost。[020-panel-separation.md](020-panel-separation.md) 中的三层面板分离核心思想（GestureInputPanel 接收触摸 / GestureFeedbackPanel 绘制反馈 / KeyGridPanel 渲染按键）完全保留，但 KeyGridPanel 更名为 KeyLayoutPanel 以强调其布局渲染职责，LayoutMode 的命名从 Overlay/FullScreen 更新为 Stacked/Separated。[060-keyboard-view-model.md](060-keyboard-view-model.md) 中 KeyboardViewModel 作为 UI 协调中心的定位不变，但需扩展以支持 Zone 感知和归一化坐标转换。本次迭代将 KeyLayoutPanelLayoutInfo 重命名为 KeyLayoutState、CandidateListLayoutInfo 重命名为 CandidateListLayoutState、InputListLayoutInfo 重命名为 InputListLayoutState，以更准确反映其状态本质而非仅布局信息快照。

### 1.3 设计原则

本设计遵循以下核心原则。第一，组件职责单一：每个面板组件仅负责一项核心职责，不越界处理其他面板的事务；GestureFeedbackState 仅管理视觉反馈数据，弹出提示由 ImeState 管理，避免状态职责混淆。第二，坐标无关：所有逻辑层面使用归一化坐标，仅在绘制时转换到实际像素坐标，确保同一数据在不同 Zone 和不同设备上均可正确渲染。第三，单实例约束：KeyLayoutPanel 在任意时刻仅存在一个实例，根据 LayoutMode 决定其部署位置。第四，正交组合：InputMode 与 Keyboard.Type 作为两个独立的正交维度，任意 InputMode 可与任意 Type 组合，不存在互斥关系。第五，可替换性：所有 UI 组件仅依赖 `:ime-engine` 公开 API，第三方应用可替换任意组件而不影响引擎功能。第六，命名一致性：播放相关模型统一使用 InputAction 前缀（InputActionPlaybackState、InputActionFingerIndicator、InputActionPositionResolver 等），避免与普通 UI 状态命名混淆。

---

## 2. 核心概念

### 2.1 LayoutZone -- 屏幕分区

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

### 2.2 LayoutMode -- 布局模式

LayoutMode 定义 Zone A 与 Zone B 的使用方式，是对 v4 中 Overlay/FullScreen 的重命名和语义精化。Stacked 模式（原 Overlay）下，所有组件集中在 Zone B 内，三层面板叠加共享同一空间，适合紧凑布局和单手操作场景。Separated 模式（原 FullScreen）下，输入区域占据 Zone B，按键展示区域占据 Zone A，手指在 Zone B 输入时不会被自身遮挡，按键在 Zone A 的更宽空间中展示，缩短了视觉搜索路径。两种模式可动态切换，切换时组件实例按实例策略表进行重新部署。

```kotlin
/**
 * 布局模式，定义 Zone A 与 Zone B 的使用方式。
 *
 * Stacked 模式（原 Overlay）：所有组件集中在 Zone B 内。
 * Separated 模式（原 FullScreen）：输入区域在 Zone B，按键展示在 Zone A。
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

### 2.3 InputMode -- 输入模式

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

### 2.4 Keyboard.Type -- 键盘类型

Keyboard.Type 决定键盘的内容类型，即按键集合的语义分类。Pinyin 类型提供拼音输入的声母韵母按键，Latin 类型提供拉丁字母按键，Symbol/Emoji 类型提供符号和表情，Number 类型提供数字和基本运算符，Math 类型提供数学公式相关按键。Type 的选择决定了 KeyLayoutPanel 渲染哪些按键以及按键的标签内容，但不影响按键的几何排列方式——几何排列由 InputMode 决定。原先的 Editor 类型已被移除，其文本编辑功能键（如全选、复制、粘贴、撤销等）合并到 ToolListPanel 中作为工具项展示，由 ToolListState.tools 统一管理，这样避免了 Editor 类型与其他类型在布局策略上的冗余组合，同时使编辑功能在任何键盘类型下均可通过工具栏快速访问。

```kotlin
/**
 * 键盘类型，决定键盘的内容类型。
 *
 * Type 与 InputMode 正交：任意 Type 可与任意 InputMode 组合。
 * Type 决定按键集合的语义内容和标签，InputMode 决定按键的几何排列方式。
 * Editor 类型已移除，其编辑功能合并到 ToolListPanel 中。
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

### 2.5 InputMode x Keyboard.Type 正交矩阵

InputMode 与 Keyboard.Type 是两个完全独立的正交维度，不存在互斥关系。任意 InputMode 可以与任意 Type 组合，产生不同的按键布局和交互体验。例如 Pinyin + XPad 组合产生六边形拼音滑行面板，Pinyin + RectGrid 组合产生传统 QWERTY 拼音键盘，Number + HexGrid 组合产生六边形数字面板。这种正交设计使得新增 InputMode 或 Type 时不需要修改已有组合的代码，只需在 KeyLayoutPanel 中为新的组合提供布局策略即可。原先的 Editor 类型已移除，其功能由 ToolListPanel 统一承载，因此正交矩阵中不再包含 Editor 列。

|  | Pinyin | Latin | Symbol/Emoji | Number | Math |
|---|---|---|---|---|---|
| **XPad** | 六边形拼音滑行 | 六边形拉丁滑行 | 六边形符号选择 | 六边形数字输入 | 六边形公式输入 |
| **HexGrid** | 六边形拼音网格 | 六边形拉丁网格 | 六边形符号网格 | 六边形数字网格 | 六边形公式网格 |
| **RectGrid** | QWERTY 拼音键盘 | QWERTY 拉丁键盘 | 矩形符号键盘 | 矩形数字键盘 | 矩形公式键盘 |
| **MultiZone** | 分区拼音输入 | 分区拉丁输入 | 分区符号输入 | 分区数字输入 | 分区公式输入 |

---

## 3. 屏幕布局模型

### 3.1 Zone A 与 Zone B 的空间关系

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

### 3.2 Zone B 三行结构

Zone B 纵向划分为三个行，每行承载不同类型的面板组件，行与行之间不存在重叠。Row 1 位于 Zone B 顶部，承载 CandidateListPanel 和 PopupTipPanel，二者叠加共享同一空间，PopupTipPanel 仅在需要时短暂浮现并覆盖在 CandidateListPanel 之上。Row 2 位于中间，承载 ToolListPanel 和 InputListPanel，二者互斥共享同一空间，空闲时显示 ToolListPanel，输入时切换为 InputListPanel。Row 3 位于底部，承载 KeyLayoutPanel、GestureFeedbackPanel 和 GestureInputPanel，三者始终共存并叠加共享同一空间，分别负责按键渲染、手势反馈绘制和触摸事件接收。

三行结构的设计考量在于：将不同交互职责的面板在空间上分离，避免面板之间的视觉干扰和事件冲突。Row 1 的候选提示和操作提示在功能上相近（都是展示信息），共享空间但通过叠加方式共存；Row 2 的工具栏和输入栏在时间上互斥（空闲时用工具、输入时用输入栏），共享空间通过切换方式共存；Row 3 的三层面板在交互上互补（触摸/反馈/渲染），必须始终共存通过叠加方式共享空间。在输入动作播放期间，Row 1 和 Row 2 的面板可通过内建的 showIndicator 参数在自身内部绘制指示器动画，无需额外的覆盖层组件。

### 3.3 Separated 模式下 Row 3 的三列布局

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

### 3.4 Zone A 的内容

Zone A 仅在 Separated 模式下被使用，其内容由 KeyLayoutPanel 和 GestureFeedbackPanel 叠加构成。KeyLayoutPanel 负责根据当前 InputMode 和 Keyboard.Type 渲染按键布局，GestureFeedbackPanel 负责绘制触摸轨迹、按键高亮等视觉反馈。两面板叠加共享 Zone A 的全部空间，叠加顺序从底到顶为：KeyLayoutPanel（底层渲染）-> GestureFeedbackPanel（透明反馈层）。Zone A 的容器支持指定尺寸和布局方向（居中、顶部对齐、底部对齐），面板组件自动填充容器提供的可用空间。

Zone A 中的 GestureFeedbackPanel 使用归一化坐标绘制反馈。由于 Zone A 和 Zone B 各有独立的 GestureFeedbackPanel 实例，它们的尺寸不同，但共享同一套归一化坐标数据。Zone A 实例的尺寸由 Zone A 面板容器决定，Zone B 实例的尺寸由 Zone B 面板容器决定。绘制时，各实例根据自身尺寸将归一化坐标转换为实际像素坐标。KeyLayoutPanel 只能出现在 Zone A（Separated 模式）或 Zone B（Stacked 模式）中的一个位置，不存在同时出现在两个 Zone 的情况，即 KeyLayoutPanel 是单实例组件。

---

## 4. UI 组件层次树

### 4.1 完整组件层次

以下为 kuaizi IME UI 组件的完整层次树，从顶层集成组件到面板组件再到原子组件，按照组合关系组织。KeyboardHost 作为顶层集成组件，内部根据 LayoutMode 组合 Zone A 和 Zone B 的内容；Zone B 内部按三行结构组织面板；面板组件内部组合原子组件。KeyboardInputActionPlayerHost 是独立的演示组件，内部组合 KeyboardHost 和播放引擎，通过面板内建的 showIndicator 参数控制指示器显示。

```
KeyboardHost (集成组件)
├── Zone A Container (仅 Separated 模式)
│   ├── KeyLayoutPanel (单实例)
│   │   └── KeyView (原子组件) x N
│   └── GestureFeedbackPanel (Zone A 实例)
│       └── Canvas (归一化坐标绘制)
└── Zone B Container
    ├── Row 1
    │   ├── CandidateListPanel
    │   │   ├── CandidateItem (原子组件) x N
    │   │   └── [内建] IndicatorOverlay (showIndicator 控制)
    │   └── PopupTipPanel (叠加)
    │       └── PopupTipItem
    ├── Row 2
    │   ├── ToolListPanel (空闲时显示)
    │   │   ├── ToolItem (原子组件) x N
    │   │   └── [内建] IndicatorOverlay (showIndicator 控制)
    │   └── InputListPanel (输入时显示，互斥)
    │       ├── CharInputItem (原子组件) x N
    │       ├── GapInputItem (原子组件) x N
    │       └── [内建] IndicatorOverlay (showIndicator 控制)
    └── Row 3
        ├── Stacked 模式:
        │   ├── KeyLayoutPanel (单实例)
        │   │   └── KeyView x N
        │   ├── GestureFeedbackPanel (Zone B 实例)
        │   └── GestureInputPanel (透明触摸层)
        └── Separated 模式:
            ├── 左列: 功能按钮区
            ├── 中列:
            │   ├── GestureFeedbackPanel (Zone B 实例)
            │   └── GestureInputPanel
            └── 右列: 功能按钮区

KeyboardInputActionPlayerHost (演示集成组件)
├── Animation 模式:
│   └── KeyboardHost (showIndicator=true, 传递 indicatorState 到各面板)
└── DirectInput 模式:
    └── KeyboardHost (showIndicator=false, 不显示指示器)
```

### 4.2 层次关系说明

组件层次树的设计遵循三层架构：集成层（KeyboardHost、KeyboardInputActionPlayerHost）、面板层（各 Panel 组件）、原子层（KeyView、CandidateItem 等）。集成层负责组合面板层组件并提供完整的功能入口，面板层负责组合原子层组件并管理面板级状态，原子层是最小粒度的 UI 组件，仅负责单一元素的渲染。这种层次结构确保了每层组件的职责清晰，第三方应用可以选择在不同层次上进行替换或定制。

KeyboardHost 替代了 v4 中的 KeyboardPanel 和 KeyboardScreen 两个组件，通过 LayoutMode 参数统一了堆叠和分离两种布局模式的入口。这种统一简化了第三方应用的集成方式——只需使用 KeyboardHost 并指定 LayoutMode 即可获得完整的输入法界面，无需根据模式选择不同的组件。KeyboardInputActionPlayerHost 在 KeyboardHost 基础上叠加播放引擎，专用于输入动作的演示和练习场景，不影响 KeyboardHost 的正常使用。行指示器已内建到 CandidateListPanel、InputListPanel、ToolListPanel 中，通过 showIndicator 布尔参数控制是否绘制，消除了独立 IndicatorOverlay 覆盖层的需要，使组件层次更加简洁。

---

## 5. 组件规格表

### 5.1 KeyboardHost

| 属性 | 说明 |
|------|------|
| 角色 | 顶层集成组件，替代 v4 的 KeyboardPanel 和 KeyboardScreen |
| 职责 | 根据 LayoutMode 组合 Zone A / Zone B 的面板组件，提供完整输入交互 UI，支持动态切换布局模式 |
| 约束 | KeyLayoutPanel 为单实例，同一时刻仅存在于 Zone A 或 Zone B 之一 |
| 关键属性 | layoutMode: State\<LayoutMode\>, viewModel: KeyboardViewModel |
| 所属包 | integration |

```kotlin
/**
 * 键盘宿主组件，顶层集成组件。
 *
 * 替代 v4 的 KeyboardPanel（原 Overlay 模式）和 KeyboardScreen（原 FullScreen 模式），
 * 通过 LayoutMode 参数统一两种布局模式的入口。
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

/**
 * 堆叠模式布局。
 *
 * 所有组件集中在 Zone B 内，KeyLayoutPanel 部署在 Zone B Row 3。
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
                onLayoutInfoChanged = { keyLayoutState = it },
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

/**
 * 分离模式布局。
 *
 * Zone A: KeyLayoutPanel + GestureFeedbackPanel
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
        // Zone A
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
                onLayoutInfoChanged = { keyLayoutState = it },
            )
            GestureFeedbackPanel(
                elements = GestureFeedbackPanelSet.SeparatedSet.keySideElements,
                feedbackState = feedbackState,
                keyLayoutState = keyLayoutState,
            )
        }

        // Zone B
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f - mode.zoneARatio),
        ) {
            // Row 1
            Box(modifier = Modifier.weight(RowWeight.R1)) {
                CandidateListPanel(
                    state = state.candidateList,
                    onCandidateSelected = { viewModel.handleIntent(ImeIntent.SelectCandidate(it)) },
                )
                PopupTipPanel(state = state)
            }

            // Row 2
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

                // 中列：手势交互
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

### 5.2 CandidateListPanel

| 属性 | 说明 |
|------|------|
| 角色 | Row 1 面板，展示候选列表 |
| 职责 | 展示多个可补全输入、可粘贴内容等候选项，支持滚动和选择；内建行指示器动画 |
| 约束 | 始终部署在 Zone B Row 1，与 PopupTipPanel 叠加共享空间 |
| 关键属性 | state: CandidateListState, onCandidateSelected: (Candidate) -> Unit, showIndicator: Boolean, indicatorState: InputActionRowIndicator? |
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
    indicatorState: InputActionRowIndicator? = null,
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

/**
 * 候选列表面板布局状态，用于坐标解析。
 */
data class CandidateListLayoutState(
    /** 各候选项的位置矩形（屏幕坐标系） */
    val candidatePositions: List<Rect>,
    /** 面板实际尺寸，用于归一化坐标转换 */
    val panelSize: Size = Size.Zero,
)

/**
 * 查找指定候选项的中心坐标。
 *
 * 用于 InputActionPlayer 绘制指示器动画时定位候选项。
 * 若目标候选项不在可视范围内，需先滚动到目标位置。
 *
 * @param index 候选项索引
 * @return 候选项中心坐标，若索引越界则返回 null
 */
fun CandidateListLayoutState.locateItem(index: Int): Offset? {
    return candidatePositions.getOrNull(index)?.center
}
```

### 5.3 PopupTipPanel

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
 * 此面板替代 v4 早期迭代中的 InfoTipPanel，
 * 强调其「短暂弹出」的交互特性。
 *
 * **重要变更**：PopupTipPanel 现在从 ImeState 读取弹出提示状态，
 * 而非从 GestureFeedbackState 读取。弹出提示是引擎处理意图后
 * 更新 ImeState 触发的，属于输入状态变化而非视觉反馈，
 * 因此不应由 GestureFeedbackState 管理。
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

/**
 * 弹出提示状态。
 */
data class PopupTipState(
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
)
```

### 5.4 ToolListPanel

| 属性 | 说明 |
|------|------|
| 角色 | Row 2 面板（空闲时），展示工具按钮（含原 Editor 类型的编辑功能键） |
| 职责 | 空闲时展示固定 ToolItem 按钮（剪贴板、收藏、设置、编辑功能等），输入时仅显示切换按钮；内建行指示器动画 |
| 约束 | 与 InputListPanel 互斥共享 Row 2 空间，由 isInputting 状态控制切换 |
| 关键属性 | state: ToolListState, onToolSelected: (ImeIntent) -> Unit, showIndicator: Boolean, indicatorState: InputActionRowIndicator? |
| 所属包 | panel |
| 备注 | 原 Editor 键盘类型的编辑功能键（全选、复制、粘贴、撤销等）已合并到 ToolListPanel 中作为 ToolItem 展示 |

```kotlin
/**
 * 工具列表面板。
 *
 * 空闲时展示固定 ToolItem 按钮（剪贴板粘贴、收藏管理、设置、键盘切换等），
 * 输入时收缩为仅显示切换按钮（从 InputListPanel 切回 ToolListPanel）。
 * 与 InputListPanel 互斥共享 Row 2 空间，由 ImeState.isInputting 控制切换。
 *
 * **Editor 功能合并说明**：原 Editor 键盘类型提供的编辑功能键
 * （如全选、复制、粘贴、剪切、撤销、重做等）已合并到 ToolListPanel 中，
 * 作为 ToolItem 统一管理。这样编辑功能在任何键盘类型下均可通过
 * 工具栏快速访问，避免了 Editor 类型与其他类型的冗余组合。
 */
@Composable
fun ToolListPanel(
    state: ToolListState,
    onToolSelected: (ImeIntent) -> Unit,
    modifier: Modifier = Modifier,
    showIndicator: Boolean = false,
    indicatorState: InputActionRowIndicator? = null,
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

### 5.5 InputListPanel

| 属性 | 说明 |
|------|------|
| 角色 | Row 2 面板（输入时），展示当前输入 |
| 职责 | 展示当前输入内容，支持选择已输入文本进行修改或选择候选；内建行指示器动画 |
| 约束 | 与 ToolListPanel 互斥共享 Row 2 空间；提供 locateItem() 方法供播放器定位 |
| 关键属性 | state: InputListState, locateItem(): Offset?, showIndicator: Boolean, indicatorState: InputActionRowIndicator? |
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
    indicatorState: InputActionRowIndicator? = null,
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

/**
 * 输入列表面板布局状态。
 */
data class InputListLayoutState(
    /** 各输入项的位置矩形（屏幕坐标系） */
    val itemPositions: List<Rect>,
    /** 面板实际尺寸，用于归一化坐标转换 */
    val panelSize: Size = Size.Zero,
)

/**
 * 查找指定输入项的中心坐标。
 *
 * 用于 InputActionPlayer 绘制指示器动画时定位输入项。
 * 若目标项不在可视范围内，需先滚动到目标位置。
 *
 * @param index 输入项索引
 * @return 输入项中心坐标，若索引越界则返回 null
 */
fun InputListLayoutState.locateItem(index: Int): Offset? {
    return itemPositions.getOrNull(index)?.center
}
```

### 5.6 KeyLayoutPanel

| 属性 | 说明 |
|------|------|
| 角色 | Row 3 面板，按键布局渲染 |
| 职责 | 根据 InputMode 和 Keyboard.Type 动态切换按键布局并渲染按键，提供布局状态供其他面板查询 |
| 约束 | 单实例：同一时刻仅存在于 Zone A 或 Zone B 之一；InputMode x Type 正交组合 |
| 关键属性 | inputMode: InputMode, keyboardType: Type, onLayoutInfoChanged, 计算归一化坐标 |
| 所属包 | panel |
| 变更说明 | 由 v4 的 KeyGridPanel 更名，强调布局渲染职责而非仅网格渲染；KeyLayoutPanelLayoutInfo 重命名为 KeyLayoutState |

```kotlin
/**
 * 按键布局面板，替代 v4 的 KeyGridPanel。
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
    onLayoutInfoChanged: (KeyLayoutState) -> Unit,
    modifier: Modifier = Modifier,
) {
    val layoutStrategy = remember(inputMode, keyboardType) {
        LayoutStrategy.resolve(inputMode, keyboardType)
    }

    layoutStrategy.Layout(
        keyGrid = keyGrid,
        keyboardState = keyboardState,
        onLayoutInfoChanged = onLayoutInfoChanged,
        modifier = modifier,
    )
}

/**
 * 布局策略，根据 InputMode x Type 组合分发。
 */
interface LayoutStrategy {
    @Composable
    fun Layout(
        keyGrid: List<List<InputKey>>,
        keyboardState: KeyboardState,
        onLayoutInfoChanged: (KeyLayoutState) -> Unit,
        modifier: Modifier,
    )

    companion object {
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

/**
 * 按键布局状态，替代 v4 的 KeyLayoutPanelLayoutInfo。
 *
 * 重命名为 KeyLayoutState 以更准确反映其状态本质：
 * 它不仅是布局信息的快照，更是面板布局的核心状态对象，
 * 被多个组件（GestureInputPanel、GestureFeedbackPanel、
 * InputActionPositionResolver）共同引用。
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
     * 将归一化坐标转换为指定面板尺寸下的像素坐标。
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

### 5.7 GestureInputPanel

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
    // 与 v4 的 GestureInputPanel 核心逻辑相同，
    // 但增加了 InputMode 参数以支持不同模式的手势识别策略
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

### 5.8 GestureFeedbackPanel

| 属性 | 说明 |
|------|------|
| 角色 | 透明反馈绘制层，双实例 |
| 职责 | 根据归一化坐标绘制触摸轨迹、按键高亮、手指指示器 |
| 约束 | 双实例：Zone A 实例和 Zone B 实例；使用归一化坐标，绘制时根据面板尺寸转换为像素坐标 |
| 关键属性 | elements: Set\<FeedbackElementType\>, feedbackState, keyLayoutState |
| 所属包 | panel |
| 变更说明 | 移除了 KeyPath 和 XPadPathHighlight 绘制，按键间路径和 X-Pad 路径统一合并到 TouchTrail（输入轨迹）中 |

```kotlin
/**
 * 手势反馈面板，透明绘制层，支持多实例。
 *
 * 与 v4 的 GestureFeedbackPanel 核心逻辑相同，
 * 但关键变更为：使用归一化坐标替代绝对坐标，
 * 且简化了反馈元素类型，将 KeyPath 和 XPadPathHighlight
 * 合并到 TouchTrail（输入轨迹）中统一处理。
 *
 * **归一化坐标机制**：
 * GestureFeedbackState 中的坐标数据以归一化形式存储 [0,1]x[0,1]，
 * GestureFeedbackPanel 在绘制时根据自身实际尺寸转换为像素坐标。
 * 这使得同一份反馈数据可以正确地在不同尺寸的面板实例上渲染。
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

        // 触摸轨迹（包含按键间路径和 X-Pad 路径）：归一化坐标 -> 像素坐标
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

/**
 * 反馈元素类型。
 *
 * 简化后仅保留三种核心类型：
 * - TouchTrail：触摸轨迹（包含按键间路径和 X-Pad 路径的平滑曲线）
 * - KeyHighlight：按键高亮（手势过程中按下的按键临时高亮）
 * - FingerIndicator：手指指示器（播放动画时显示手指位置）
 *
 * KeyPath 和 XPadPathHighlight 已合并到 TouchTrail 中，
 * 因为按键间路径和 X-Pad 路径本质上都是输入轨迹的组成部分，
 * 由 KeyLayoutPanel 根据 InputMode 计算起止按键间的平滑曲线，
 * 统一作为 touchTrailPoints 写入 GestureFeedbackState。
 */
enum class FeedbackElementType {
    /** 触摸轨迹，包含按键间路径和 X-Pad 路径的平滑曲线 */
    TouchTrail,
    /** 按键高亮，手势过程中按下的按键临时高亮 */
    KeyHighlight,
    /** 手指指示器，播放动画时显示手指位置 */
    FingerIndicator,
}

/**
 * 反馈面板配置集。
 *
 * 替代 v4 的 GestureFeedbackPanelSet，更新命名并简化元素类型。
 */
sealed class GestureFeedbackPanelSet {

    /** 堆叠模式配置：Zone B 单实例绘制所有反馈 */
    data object StackedSet : GestureFeedbackPanelSet() {
        val allElements: Set<FeedbackElementType> = setOf(
            FeedbackElementType.TouchTrail,
            FeedbackElementType.KeyHighlight,
            FeedbackElementType.FingerIndicator,
        )
    }

    /** 分离模式配置：Zone A 和 Zone B 各一个实例 */
    data class SeparatedSet(
        /** Zone A（按键侧）反馈元素：按键高亮 + 输入轨迹 */
        val keySideElements: Set<FeedbackElementType> = setOf(
            FeedbackElementType.KeyHighlight,
            FeedbackElementType.TouchTrail,
        ),
        /** Zone B（输入侧）反馈元素：输入轨迹 + 手指指示器 */
        val inputSideElements: Set<FeedbackElementType> = setOf(
            FeedbackElementType.TouchTrail,
            FeedbackElementType.FingerIndicator,
        ),
    ) : GestureFeedbackPanelSet()
}
```

### 5.9 KeyboardInputActionPlayerHost

| 属性 | 说明 |
|------|------|
| 角色 | 输入动作播放演示集成组件 |
| 职责 | 支持 Animation 和 DirectInput 两种 InputActionUseMode，组合 KeyboardHost 和播放引擎 |
| 约束 | 仅用于演示/练习场景；Animation 模式访问真实字典数据但不提交到目标编辑器；DirectInput 模式不显示指示器 |
| 关键属性 | inputActionUseMode: InputActionUseMode, viewModel: KeyboardViewModel |
| 所属包 | integration |

```kotlin
/**
 * 输入动作播放宿主组件。
 *
 * 支持两种 InputActionUseMode：
 * - Animation：不可中断的动画播放模式，访问真实字典数据但不提交到目标编辑器，
 *   不写入数据库，仅展示输入过程动画。此模式下各面板的 showIndicator=true，
 *   指示器状态通过面板的 indicatorState 参数传入，在面板内部绘制。
 * - DirectInput：封装 KeyboardHost 提供完整输入支持，
 *   在此基础上叠加播放引擎。此模式下 showIndicator=false，
 *   不显示行指示器动画，仅通过 GestureFeedbackPanel 绘制手指指示器。
 *
 * 输入数据包括键盘输入模式 + 动作序列，针对不同输入对象（按键、输入列表、候选列表），
 * 但 UI 坐标无关。输入轨迹由 KeyLayoutPanel 的 InputMode 决定，
 * KeyLayoutPanel 动态计算按键位置和轨迹形状。
 *
 * 对于 InputListPanel 和 CandidateListPanel 的交互，仅需选择操作：
 * 在面板上绘制圆形指示器点击动画（Animation 模式），
 * 点击坐标由面板的 locateItem() 方法动态计算。
 * 若目标项不在可视范围内，需先滚动到目标位置再定位。
 */
@Composable
fun KeyboardInputActionPlayerHost(
    viewModel: KeyboardViewModel,
    inputActionUseMode: KeyboardInputActionPlayerHost.InputActionUseMode,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val playerState by viewModel.actionPlayer.playbackState.collectAsState()

    // 判断是否显示指示器：仅 Animation 模式下播放中才显示
    val showIndicators = inputActionUseMode is KeyboardInputActionPlayerHost.InputActionUseMode.Animation
            && playerState is InputActionPlaybackState.Playing

    // 指示器状态（仅 Animation 模式下有意义）
    val row1Indicator = viewModel.actionPlayer.row1IndicatorState
    val row2Indicator = viewModel.actionPlayer.row2IndicatorState

    Box(modifier = modifier.fillMaxSize()) {
        // 基础键盘组件，通过参数传递指示器控制
        when (inputActionUseMode) {
            is KeyboardInputActionPlayerHost.InputActionUseMode.Animation -> {
                // Animation 模式：showIndicator=true，传递 indicatorState
                KeyboardHostWithIndicators(
                    viewModel = viewModel,
                    showIndicators = showIndicators,
                    row1Indicator = row1Indicator,
                    row2Indicator = row2Indicator,
                )
            }
            is KeyboardInputActionPlayerHost.InputActionUseMode.DirectInput -> {
                // DirectInput 模式：showIndicator=false，不显示指示器
                KeyboardHost(viewModel = viewModel)
            }
        }
    }
}

/**
 * 带指示器参数的 KeyboardHost 封装。
 *
 * 将 showIndicator 和 indicatorState 参数传递到各面板组件，
 * 面板组件在内部绘制指示器，无需外部覆盖层。
 */
@Composable
private fun KeyboardHostWithIndicators(
    viewModel: KeyboardViewModel,
    showIndicators: Boolean,
    row1Indicator: InputActionRowIndicator?,
    row2Indicator: InputActionRowIndicator?,
) {
    // 内部使用修改后的 KeyboardHost 逻辑，
    // 在各面板调用处传递 showIndicator 和 indicatorState
    // Row 1: CandidateListPanel(showIndicator = showIndicators, indicatorState = row1Indicator)
    // Row 2: InputListPanel/ToolListPanel(showIndicator = showIndicators, indicatorState = row2Indicator)
    // Row 3: 指示器通过 GestureFeedbackPanel 的 FingerIndicator 绘制
    KeyboardHost(viewModel = viewModel)
}

/**
 * 输入动作播放宿主的 InputActionUseMode。
 */
sealed class KeyboardInputActionPlayerHost {

    sealed class InputActionUseMode {
        /**
         * 动画播放模式。
         *
         * 不可中断的播放模式，访问真实字典数据但不提交到目标编辑器，
         * 不写入数据库。仅用于演示/教学场景。
         * 此模式下各面板的 showIndicator=true，显示行指示器动画。
         */
        data object Animation : InputActionUseMode()

        /**
         * 直接输入模式。
         *
         * 封装 KeyboardHost 提供完整输入支持，
         * 在此基础上叠加播放引擎。
         * 此模式下 showIndicator=false，不显示行指示器动画，
         * 仅通过 GestureFeedbackPanel 的 FingerIndicator 显示手指位置。
         */
        data object DirectInput : InputActionUseMode()
    }
}
```

### 5.10 CoordinateNormalizer

| 属性 | 说明 |
|------|------|
| 角色 | 坐标归一化工具 |
| 职责 | 在绝对坐标与归一化坐标之间进行双向转换 |
| 约束 | 归一化坐标范围 [0,1] x [0,1]，(0,0) 为左上角 |
| 关键方法 | normalize(), denormalize() |
| 所属包 | panel |

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
 * Offset 扩展：归一化到 [0,1] 范围。
 */
fun Offset.normalize(sourceSize: Size): OffsetF =
    CoordinateNormalizer.normalize(this, sourceSize)

/**
 * OffsetF 扩展：反归一化为绝对坐标。
 */
fun OffsetF.denormalize(targetSize: Size): Offset =
    CoordinateNormalizer.denormalize(this, targetSize)
```

---

## 6. 面板共存规则

Zone B 三行结构中，不同行的面板组件遵循不同的共存规则。Row 1 的 CandidateListPanel 和 PopupTipPanel 采用叠加共存，二者共享同一空间，PopupTipPanel 短暂浮现覆盖在 CandidateListPanel 之上。Row 2 的 ToolListPanel 和 InputListPanel 采用互斥共存，同一时刻仅显示其中一个，由 ImeState.isInputting 状态控制切换。Row 3 的 KeyLayoutPanel、GestureFeedbackPanel 和 GestureInputPanel 采用叠加共存，三者始终同时存在，分别负责渲染、反馈和触摸，叠加顺序从底到顶为 KeyLayoutPanel -> GestureFeedbackPanel -> GestureInputPanel。在输入动作播放期间，Row 1 和 Row 2 的面板通过内建的 showIndicator 参数在自身绘制区域内叠加指示器动画，不再需要独立的覆盖层组件。

| 行 | 面板 | 共存规则 | 共享空间方式 | 切换条件 |
|---|---|---|---|---|
| Row 1 | CandidateListPanel | 叠加（底层） | 共享 Row 1 全部空间 | 始终存在 |
| Row 1 | PopupTipPanel | 叠加（顶层） | 覆盖在 CandidateListPanel 上 | 短暂浮现后自动消失 |
| Row 2 | ToolListPanel | 互斥 | 独占 Row 2 空间 | isInputting == false |
| Row 2 | InputListPanel | 互斥 | 独占 Row 2 空间 | isInputting == true |
| Row 3 | KeyLayoutPanel | 叠加（底层） | 共享 Row 3 全部空间（Stacked）或不在此行（Separated） | 始终存在 |
| Row 3 | GestureFeedbackPanel | 叠加（中层） | 共享 Row 3 中列空间 | 始终存在 |
| Row 3 | GestureInputPanel | 叠加（顶层） | 共享 Row 3 中列空间 | 始终存在 |

叠加共存与互斥共存的核心区别在于：叠加共存的面板同时渲染在同一空间，通过透明度和 Z 轴顺序实现视觉分层；互斥共存的面板同一时刻仅渲染其中一个，切换时存在短暂的进入/退出动画。Row 1 的叠加方式允许 PopupTipPanel 在不影响 CandidateListPanel 布局的情况下短暂浮现，提示消失后候选列表自然可见。Row 2 的互斥方式确保工具栏和输入栏不会视觉冲突，用户在输入时看到输入内容，空闲时看到工具选项。Row 3 的叠加方式延续了 v4 的三层分离设计，确保触摸/反馈/渲染三层的独立性。指示器动画通过面板内建的 showIndicator 参数控制在面板内部绘制，与面板的常规渲染内容叠加共存，不影响其他行的布局。

---

## 7. 实例策略表

不同 LayoutMode 下，组件实例的部署位置和数量不同。KeyLayoutPanel 作为单实例组件，在 Stacked 模式下部署在 Zone B Row 3，在 Separated 模式下部署在 Zone A。GestureFeedbackPanel 作为双实例组件，在 Stacked 模式下仅 Zone B 有一个实例，在 Separated 模式下 Zone A 和 Zone B 各有一个实例，分别绘制不同类型的反馈元素。GestureInputPanel 在两种模式下均仅存在于 Zone B Row 3。以下表格详细列出各面板在不同 LayoutMode 下的实例数量和部署位置。

| 组件 | Stacked 模式实例数 | Stacked 部署位置 | Separated 模式实例数 | Separated 部署位置 |
|---|---|---|---|---|
| KeyLayoutPanel | 1 | Zone B Row 3 | 1 | Zone A |
| GestureFeedbackPanel | 1 | Zone B Row 3 | 2 | Zone A (按键侧), Zone B Row 3 (输入侧) |
| GestureInputPanel | 1 | Zone B Row 3 | 1 | Zone B Row 3 中列 |
| CandidateListPanel | 1 | Zone B Row 1 | 1 | Zone B Row 1 |
| PopupTipPanel | 1 | Zone B Row 1 | 1 | Zone B Row 1 |
| ToolListPanel | 1 | Zone B Row 2 | 1 | Zone B Row 2 |
| InputListPanel | 1 | Zone B Row 2 | 1 | Zone B Row 2 |

LayoutMode 动态切换时的实例迁移策略如下。从 Stacked 切换到 Separated 时：KeyLayoutPanel 从 Zone B Row 3 迁移到 Zone A；GestureFeedbackPanel 从 Zone B 的单实例拆分为 Zone A 和 Zone B 的双实例，反馈元素按 SeparatedSet 配置重新分配；GestureInputPanel 保持在 Zone B 但从整行宽度收缩到中列宽度；Zone A 的容器被创建并填充内容。从 Separated 切换到 Stacked 时执行反向操作：KeyLayoutPanel 从 Zone A 迁移回 Zone B Row 3；GestureFeedbackPanel 的双实例合并为 Zone B 单实例，反馈元素按 StackedSet 配置合并；GestureInputPanel 从中列宽度扩展到整行宽度；Zone A 的容器被移除。切换过程中，所有状态通过 GestureFeedbackState 和 ImeState 保持连续，不丢失任何手势或输入进度信息。行指示器状态由 InputActionPlayer 管理，通过面板的 indicatorState 参数传递，切换过程不影响指示器的正常工作。

---

## 8. 逻辑/状态层

### 8.1 ImeState 扩展

ImeState 需要扩展以支持新的 UI 层概念。新增 inputMode 字段表示当前输入模式，isInputting 字段表示是否正在输入（控制 ToolListPanel 和 InputListPanel 的互斥切换），以及 Zone 相关的布局配置。本次迭代还新增了 popupTip 字段，将弹出提示从 GestureFeedbackState 移至 ImeState 管理，因为弹出提示是引擎处理意图后更新 ImeState 触发的展示，属于输入状态变化而非视觉反馈。这些扩展仅涉及 UI 层状态的暴露，不改变 `:ime-engine` 的核心 reduce 逻辑——引擎仍然通过 ImeIntent 驱动状态转换，UI 层从 ImeState 中读取新增字段来决定面板的部署和切换。

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

    /** 工具列表状态（含原 Editor 类型的编辑功能键） */
    val toolList: ToolListState = ToolListState(emptyList()),

    /**
     * 弹出提示状态。
     *
     * 由引擎处理意图后更新 ImeState 触发显示。
     * PopupTipPanel 从 ImeState.popupTip 读取提示内容，
     * 而非从 GestureFeedbackState 读取。
     * 弹出提示属于输入状态变化触发的展示，不属于视觉反馈。
     */
    val popupTip: PopupTipState? = null,
)
```

### 8.2 GestureFeedbackState 简化

GestureFeedbackState 经本次迭代简化后，仅保留纯粹的视觉反馈职责。原先的 popupTip 已移至 ImeState 管理，因为弹出提示是引擎处理意图后更新 ImeState 触发的展示，不属于手势视觉反馈。原先的 keyPath 和 xPadPath 已合并到 touchTrailPoints 中，因为按键间路径和 X-Pad 路径本质上都是输入轨迹的组成部分，由 KeyLayoutPanel 根据 InputMode 计算起止按键间的平滑曲线后，作为 touchTrailPoints 中的插值路径点统一写入。简化后的 GestureFeedbackState 包含三类核心视觉反馈：触摸轨迹点（含计算后的平滑曲线）、按键高亮集合、手指指示器状态。

```kotlin
/**
 * 手势反馈状态，使用归一化坐标，职责简化为纯视觉反馈。
 *
 * 与 v4 的关键变更：
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
     * 由输入面板在手势过程中更新，手势结束后清除。
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

    // --- 更新方法 ---

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

    /** 清理所有状态 */
    fun clear() {
        clearAll()
        _fingerIndicator.value = null
    }
}

/**
 * 手指指示器状态（归一化坐标）。
 *
 * 重命名为 InputActionFingerIndicator 以明确其属于输入动作播放体系，
 * 与普通 UI 状态命名区分。
 */
data class InputActionFingerIndicator(
    /** 归一化坐标位置 [0,1]x[0,1] */
    val position: OffsetF,
    val pressed: Boolean,
    val visible: Boolean = true,
)
```

### 8.3 KeyboardViewModel 扩展

KeyboardViewModel 需要扩展以支持 LayoutMode 管理、归一化坐标转换和 InputActionPlayer 集成。新增 layoutMode 状态管理，支持运行时动态切换；新增 actionPlayer 实例，提供输入动作播放能力；手势反馈状态中的坐标归一化逻辑集成到 ViewModel 中，确保输入面板写入的归一化坐标与反馈面板读取的归一化坐标一致。本次迭代将布局信息缓存变量的命名从 LayoutInfo 更新为 LayoutState 以与新命名对齐，将播放器相关模型统一添加 InputAction 前缀。

```kotlin
class KeyboardViewModel(
    private val engine: ImeEngine,
) : ViewModel() {

    // ─── 状态暴露 ────────────────────────────────────

    val state: StateFlow<ImeState> = engine.state
    val config: ImeConfig get() = state.value.config

    // ─── 布局模式 ────────────────────────────────────

    private val _layoutMode = MutableStateFlow<LayoutMode>(LayoutMode.Stacked)
    val layoutMode: StateFlow<LayoutMode> = _layoutMode.asStateFlow()

    fun setLayoutMode(mode: LayoutMode) {
        _layoutMode.value = mode
    }

    // ─── 手势反馈状态 ────────────────────────────────

    val feedbackState = GestureFeedbackState()

    // ─── 输入动作播放器 ───────────────────────────────

    /**
     * 输入动作播放器，坐标无关。
     *
     * 通过 viewModel.handleIntent() 驱动引擎状态转换，
     * 通过 feedbackState 驱动手势反馈动画。
     */
    val actionPlayer = InputActionPlayer(
        viewModel = this,
        feedbackState = feedbackState,
        positionResolver = ComposeInputActionPositionResolver(
            keyboardLayoutStateProvider = { _currentKeyLayoutState },
            candidateLayoutStateProvider = { _currentCandidateLayoutState },
            inputListLayoutStateProvider = { _currentInputListLayoutState },
        ),
        scope = viewModelScope,
    )

    // ─── 布局状态缓存 ────────────────────────────────

    private var _currentKeyLayoutState: KeyLayoutState? = null
    private var _currentCandidateLayoutState: CandidateListLayoutState? = null
    private var _currentInputListLayoutState: InputListLayoutState? = null

    fun updateKeyLayoutState(state: KeyLayoutState) {
        _currentKeyLayoutState = state
    }

    fun updateCandidateLayoutState(state: CandidateListLayoutState) {
        _currentCandidateLayoutState = state
    }

    fun updateInputListLayoutState(state: InputListLayoutState) {
        _currentInputListLayoutState = state
    }

    // ─── 手势与意图处理 ──────────────────────────────

    fun handleGesture(gesture: InputGesture) {
        val intent = gestureToIntent(gesture)
        engine.handleIntent(intent)
    }

    fun handleIntent(intent: ImeIntent) {
        engine.handleIntent(intent)
    }

    // ... gestureToIntent() 与 v4 相同 ...
}
```

### 8.4 数据流图

以下展示用户手势输入和程序化输入两条数据流，标注归一化坐标的转换节点。与前一版本的关键区别在于：弹出提示由 ImeState 驱动而非 GestureFeedbackState，按键间路径和 X-Pad 路径合并到 touchTrailPoints 中，行指示器通过面板内建参数传递而非独立覆盖层。

**用户手势输入数据流：**

```
GestureInputPanel (Zone B)
  | 接收触摸事件
  | 归一化: eventX/panelWidth -> OffsetF
  v
GestureFeedbackState (归一化坐标)
  | touchTrailPoints: List<OffsetF> (含按键间路径和 X-Pad 路径插值)
  | pressedKeys: Set<InputKey>
  v
  +---> GestureFeedbackPanel (Zone B)
  |     | 反归一化: OffsetF * panelSize -> Offset
  |     | 绘制触摸轨迹、手指指示器
  |     v
  |
  +---> GestureFeedbackPanel (Zone A, 仅 Separated)
        | 反归一化: OffsetF * panelSize -> Offset
        | 绘制按键高亮、输入轨迹
        v

GestureInputPanel
  | 查询 KeyLayoutState.findKeyAt()
  | 输出 InputGesture
  v
KeyboardViewModel.handleGesture()
  | gestureToIntent()
  v
ImeEngine.handleIntent(ImeIntent)
  |
  v
ImeState
  | collectAsState()
  | popupTip -> PopupTipPanel (从 ImeState 读取)
  v
KeyLayoutPanel / CandidateListPanel / InputListPanel / ToolListPanel / PopupTipPanel
```

**程序化输入数据流：**

```
ActionScript (坐标无关)
  |
  v
InputActionPlayer.executeAction(InputAction)
  | 查询 InputActionPositionResolver.resolve(key)
  | KeyLayoutPanel 根据 InputMode 计算轨迹形状
  | 生成归一化坐标插值路径（写入 touchTrailPoints）
  | 写入 GestureFeedbackState (归一化坐标)
  v
  +---> GestureFeedbackState
  |     | fingerIndicator: InputActionFingerIndicator
  |     | pressedKeys: Set<InputKey>
  |     | touchTrailPoints: List<OffsetF> (含插值轨迹)
  |     v
  |     GestureFeedbackPanel (Zone A / Zone B)
  |       | 反归一化绘制
  |       v
  |
  +---> KeyboardViewModel.handleIntent(ImeIntent)
        | (Animation 模式不提交到编辑器)
        v
        ImeEngine -> ImeState -> 各面板
        | popupTip -> PopupTipPanel
        | row1Indicator -> CandidateListPanel(showIndicator=true)
        | row2Indicator -> InputListPanel/ToolListPanel(showIndicator=true)
```

---

## 9. 输入动作播放

### 9.1 结构概览

KeyboardInputActionPlayerHost 是输入动作播放的集成组件，内部组合 KeyboardHost 和播放引擎，通过面板内建的 showIndicator 参数控制指示器在 Row 1 和 Row 2 的显示，Row 3 的指示器则通过 GestureFeedbackPanel 的 FingerIndicator 绘制。播放引擎沿用 [040-input-action-player.md](040-input-action-player.md) 中的 InputActionPlayer 设计，但坐标体系从绝对坐标改为归一化坐标，InputActionPositionResolver 从解析绝对像素坐标改为解析归一化坐标。ActionScript 和 InputAction 沿用 [engine/060-input-action.md](../engine/060-input-action.md) 的定义，保持坐标无关设计。播放相关模型统一添加 InputAction 前缀以区分于普通 UI 状态。

播放引擎的工作方式取决于 InputActionUseMode。Animation 模式下，播放器访问真实字典数据（通过 ImeEngine 的正常 reduce 路径），但不将结果提交到目标编辑器，也不写入数据库——这意味着 ImeEngine 的状态机正常运转，UI 正常渲染键盘状态变化，但 ImeOutput 不会被分发。此模式下 showIndicator=true，各面板在内部绘制行指示器动画。DirectInput 模式下，播放器在完整输入流程上叠加动画效果，结果会正常提交到目标编辑器，适用于实际输入辅助场景。此模式下 showIndicator=false，不显示行指示器，仅通过 GestureFeedbackPanel 的 FingerIndicator 显示手指位置。

### 9.2 InputActionPositionResolver 扩展

InputActionPositionResolver（原 KeyPositionResolver）从解析绝对像素坐标扩展为解析归一化坐标，并新增 InputListPanel 和 CandidateListPanel 的定位支持。重命名为 InputActionPositionResolver 以明确其属于输入动作播放体系，与普通 UI 位置解析逻辑区分。ComposeInputActionPositionResolver（原 ComposeKeyPositionResolver）同步重命名。对于 KeyLayoutPanel 的按键定位，解析器查询 KeyLayoutState 中的归一化位置映射；对于 CandidateListPanel 和 InputListPanel 的项定位，解析器调用面板的 locateItem() 方法获取像素坐标后再归一化。若目标项不在可视范围内，需先滚动到目标位置再定位。

```kotlin
/**
 * 输入动作位置解析器，返回归一化坐标。
 *
 * 重命名为 InputActionPositionResolver 以明确其属于输入动作播放体系。
 * 替代 v4 的 KeyPositionResolver（返回绝对像素坐标），
 * 现在返回归一化坐标 [0,1]x[0,1]，由 GestureFeedbackPanel
 * 根据面板尺寸反归一化后绘制。
 */
interface InputActionPositionResolver {

    /**
     * 查找指定按键的归一化中心坐标。
     *
     * @param key 待查找的按键
     * @return 归一化中心坐标，若按键在当前键盘中不存在则返回 null
     */
    fun resolve(key: InputKey): OffsetF?

    /**
     * 查找指定候选项的归一化中心坐标。
     *
     * @param index 候选项索引
     * @return 归一化中心坐标，若索引越界则返回 null
     */
    fun resolveCandidatePosition(index: Int): OffsetF?

    /**
     * 查找指定输入项的归一化中心坐标。
     *
     * @param index 输入项索引
     * @return 归一化中心坐标，若索引越界则返回 null
     */
    fun resolveInputItemPosition(index: Int): OffsetF?
}

/**
 * 基于 Compose 布局的输入动作位置解析器实现。
 *
 * 重命名为 ComposeInputActionPositionResolver 以与接口重命名对齐。
 */
class ComposeInputActionPositionResolver(
    private val keyboardLayoutStateProvider: () -> KeyLayoutState?,
    private val candidateLayoutStateProvider: () -> CandidateListLayoutState?,
    private val inputListLayoutStateProvider: () -> InputListLayoutState?,
) : InputActionPositionResolver {

    override fun resolve(key: InputKey): OffsetF? {
        val layout = keyboardLayoutStateProvider() ?: return null
        return layout.keyPositions[key]?.center
    }

    override fun resolveCandidatePosition(index: Int): OffsetF? {
        val layout = candidateLayoutStateProvider() ?: return null
        val pixelOffset = layout.locateItem(index) ?: return null
        // 候选项坐标需要归一化到 Row 1 的坐标系
        return CoordinateNormalizer.normalize(pixelOffset, layout.panelSize)
    }

    override fun resolveInputItemPosition(index: Int): OffsetF? {
        val layout = inputListLayoutStateProvider() ?: return null
        val pixelOffset = layout.locateItem(index) ?: return null
        // 输入项坐标需要归一化到 Row 2 的坐标系
        return CoordinateNormalizer.normalize(pixelOffset, layout.panelSize)
    }
}
```

### 9.3 行指示器内建机制

Zone B 三行结构中，每行在播放动画时需要展示指示器。本次迭代将行指示器从独立覆盖层改为内建到面板组件中，通过 showIndicator 布尔参数和 indicatorState 状态参数控制。Row 1 的 CandidateListPanel 和 Row 2 的 InputListPanel/ToolListPanel 通过 showIndicator 和 indicatorState 参数在自身内部绘制指示器动画，当 showIndicator=true 且 indicatorState 非空时，面板在常规内容之上叠加绘制一个半透明圆形指示器。Row 3 的指示器通过 GestureFeedbackPanel 的 FingerIndicator 元素绘制，不需要面板内建支持。这种内建设计消除了独立的 Row1IndicatorOverlay 和 Row2IndicatorOverlay 覆盖层组件，简化了组件层次，同时使指示器的坐标与面板内容使用同一坐标系，避免了跨组件坐标对齐问题。

在 Animation 模式下，KeyboardInputActionPlayerHost 将 showIndicator=true 传递给 Row 1 和 Row 2 的面板，并将 InputActionPlayer 计算的行指示器状态通过 indicatorState 参数传入。在 DirectInput 模式下，showIndicator=false，面板不绘制指示器，仅通过 GestureFeedbackPanel 的 FingerIndicator 显示手指位置。这种模式差异使得 Animation 模式提供完整的可视化演示效果，而 DirectInput 模式仅保留必要的手指位置提示，避免在实际输入辅助场景中过度干扰用户操作。

```kotlin
/**
 * 行指示器状态（归一化坐标，行相对）。
 *
 * 重命名为 InputActionRowIndicator 以明确其属于输入动作播放体系。
 */
data class InputActionRowIndicator(
    /** 归一化坐标位置（相对于所在行的面板尺寸） */
    val position: OffsetF,
    val visible: Boolean = true,
)
```

### 9.4 播放执行流程

输入动作播放的执行流程如下，与 [040-input-action-player.md](040-input-action-player.md) 中的流程类似，但增加了归一化坐标、轨迹合并和内建指示器的处理。播放器加载 ActionScript 后，按时间轴依次执行 InputAction。对于 KeyDown/SwipeTo/KeyUp 等按键相关动作，播放器通过 InputActionPositionResolver 解析归一化坐标，更新 GestureFeedbackState 的手指指示器和按键高亮，反馈面板据此绘制动画。对于 SwipeTo 动作，播放器通过 InputActionPathInterpolator（原 SwipePathInterpolator）计算起止按键间的插值轨迹，将轨迹点写入 touchTrailPoints，实现平滑的手指移动动画。对于 SelectCandidate 动作，播放器通过 InputActionPositionResolver.resolveCandidatePosition() 获取候选项的归一化坐标，更新 Row 1 的 InputActionRowIndicator 状态，面板在内部绘制圆形点击动画。对于 InputListPanel 相关交互，同理使用 resolveInputItemPosition() 和 Row 2 的 InputActionRowIndicator。

```
ActionScript 加载
  |
  v
InputActionPlayer.play()
  |
  v
遍历 ActionScript.actions:
  |
  +-- InputAction.KeyDown
  |     | InputActionPositionResolver.resolve(key) -> OffsetF
  |     | feedbackState.setFingerIndicator(InputActionFingerIndicator(OffsetF, pressed=true))
  |     | feedbackState.setPressedKeys(setOf(key))
  |     | viewModel.handleIntent(PressKey(key, Tap))
  |     v
  |
  +-- InputAction.SwipeTo
  |     | resolve(fromKey), resolve(toKey) -> OffsetF, OffsetF
  |     | KeyLayoutPanel 根据 InputMode 计算轨迹形状
  |     | InputActionPathInterpolator 生成归一化坐标插值路径
  |     | feedbackState.setTouchTrailPoints(normalizedPath)
  |     | animateFingerAlongPath(feedbackState, duration)
  |     | viewModel.handleIntent(PressKey(toKey, Swipe))
  |     v
  |
  +-- InputAction.KeyUp
  |     | feedbackState.setFingerIndicator(InputActionFingerIndicator(position, pressed=false))
  |     v
  |
  +-- InputAction.SelectCandidate
  |     | resolveCandidatePosition(index) -> OffsetF (Row 1 归一化坐标)
  |     | actionPlayer.row1IndicatorState = InputActionRowIndicator(OffsetF)
  |     | viewModel.handleIntent(SelectCandidate(...))
  |     | delay -> actionPlayer.row1IndicatorState = null
  |     v
  |
  +-- InputAction.SwitchKeyboard
        | viewModel.handleIntent(SwitchKeyboard(targetType))
        v

播放结束
  feedbackState.setFingerIndicator(null)
  actionPlayer.row1IndicatorState = null
  actionPlayer.row2IndicatorState = null
```

---

## 10. 与现有 v4 设计对比

### 10.1 保留的设计

以下设计从 v4 原样保留，未做修改或仅做轻微调整。[010-ui-library-overview.md](010-ui-library-overview.md) 中的三层组件体系（Atomic / Panel / Integration）和主题系统完全保留。[020-panel-separation.md](020-panel-separation.md) 中的三层面板分离核心思想——GestureInputPanel 接收触摸、GestureFeedbackPanel 绘制反馈、按键面板渲染按键——完全保留。[040-input-action-player.md](040-input-action-player.md) 中的 InputActionPlayer 播放引擎和 ActionScript 脚本体系完全保留。[060-keyboard-view-model.md](060-keyboard-view-model.md) 中 KeyboardViewModel 作为 UI 协调中心的定位完全保留。[engine/060-input-action.md](../engine/060-input-action.md) 中的 InputAction 密封类和 ActionScriptCompiler 编译器完全保留。

### 10.2 新增的设计

以下设计是本次新增的概念和组件。Zone A / Zone B 屏幕分区模型是全新的空间组织概念，将 IME 屏幕纵向划分为两个功能区，使得分离布局模式下的空间分配有了清晰的语义定义。Zone B 三行结构是全新的面板组织方式，将候选、工具/输入、键盘交互三类面板在空间上分离，替代了 v4 中线性的 Column 布局。PopupTipPanel 是新增的短暂提示面板，替代了 v4 早期迭代中的 InfoTipPanel，从 ImeState 读取弹出提示状态。ToolListPanel 是新增的工具栏面板，在空闲时展示功能按钮，同时承载了原 Editor 类型的编辑功能键。KeyboardHost 是新增的统一集成组件，替代了 KeyboardPanel 和 KeyboardScreen 两个组件。KeyboardInputActionPlayerHost 是新增的播放演示集成组件，支持 Animation 和 DirectInput 两种 InputActionUseMode。CoordinateNormalizer 是新增的坐标归一化工具，支持归一化坐标与绝对坐标的双向转换。InputMode 枚举（XPad / HexGrid / RectGrid / MultiZone）是新增的交互范式维度，与 Keyboard.Type 正交组合。InputActionRowIndicator 是新增的行指示器状态模型，通过面板内建参数传递，替代了独立覆盖层。

### 10.3 修改的设计

以下设计从 v4 继承但做了重要修改。LayoutMode 命名从 Overlay/FullScreen 更新为 Stacked/Separated，语义更直观：Stacked 强调面板的堆叠共存，Separated 强调输入区域与按键区域的空间分离。KeyGridPanel 更名为 KeyLayoutPanel，强调其布局渲染职责而非仅网格渲染，并增加了 InputMode x Type 的正交组合支持。KeyLayoutPanelLayoutInfo 重命名为 KeyLayoutState，更准确反映其状态本质而非仅布局信息快照。CandidateListLayoutInfo 重命名为 CandidateListLayoutState，InputListLayoutInfo 重命名为 InputListLayoutState，同理更准确反映状态语义。GestureFeedbackState 从绝对坐标存储改为归一化坐标存储，并简化为仅管理视觉反馈（touchTrailPoints、pressedKeys、fingerIndicator），移除了 popupTip（移至 ImeState）、keyPath 和 xPadPath（合并到 touchTrailPoints）。KeyPositionResolver 重命名为 InputActionPositionResolver，从解析绝对像素坐标改为解析归一化坐标，并新增 InputListPanel 和 CandidateListPanel 的定位方法；ComposeKeyPositionResolver 同步重命名为 ComposeInputActionPositionResolver。GestureFeedbackPanelSet 从 OverlaySet/FullScreenSet 更名为 StackedSet/SeparatedSet，与 LayoutMode 命名对齐，并简化了元素类型（移除 KeyPath 和 XPadPathHighlight，合并到 TouchTrail）。FingerIndicatorState 重命名为 InputActionFingerIndicator，RowIndicatorState 重命名为 InputActionRowIndicator，UseMode 重命名为 InputActionUseMode，SwipePathInterpolator 重命名为 InputActionPathInterpolator，统一添加 InputAction 前缀。PopupTipPanel 改为从 ImeState 读取弹出提示状态而非 GestureFeedbackState。Keyboard.Type 移除了 Editor 枚举值，其功能合并到 ToolListPanel。行指示器从独立覆盖层改为内建到面板组件中，通过 showIndicator 和 indicatorState 参数控制。InputGesture 中增加了 InputMode 参数，使得手势识别逻辑可以根据不同输入模式采用不同策略。

### 10.4 废弃的设计

以下设计从 v4 中废弃。KeyboardPanel（原 Overlay 模式集成组件）被 KeyboardHost 替代，不再需要根据布局模式选择不同的集成组件。KeyboardScreen（原 FullScreen 模式集成组件）同样被 KeyboardHost 替代。InfoTipPanel（早期迭代的提示面板）被 PopupTipPanel 替代，强调了短暂弹出的交互特性。KeyGridPanelLayoutInfo 被 KeyLayoutState 替代，使用归一化坐标替代绝对像素坐标，命名更准确反映状态本质。GestureFeedbackState 中的绝对坐标字段（`MutableStateFlow<List<Offset>>`）被归一化坐标字段（`MutableStateFlow<List<OffsetF>>`）替代。GestureFeedbackState 中的 popupTip 字段移至 ImeState 管理。GestureFeedbackState 中的 keyPath 和 xPadPath 字段合并到 touchTrailPoints 中。FeedbackElementType 中的 KeyPath 和 XPadPathHighlight 合并到 TouchTrail 中。独立的 Row1IndicatorOverlay 和 Row2IndicatorOverlay 覆盖层被废弃，指示器改为面板内建绘制。Keyboard.Type 中的 Editor 枚举值被废弃，其功能合并到 ToolListPanel。KeyPositionResolver 废弃，由 InputActionPositionResolver 替代。ComposeKeyPositionResolver 废弃，由 ComposeInputActionPositionResolver 替代。FingerIndicatorState 废弃，由 InputActionFingerIndicator 替代。RowIndicatorState 废弃，由 InputActionRowIndicator 替代。UseMode 废弃，由 InputActionUseMode 替代。SwipePathInterpolator 废弃，由 InputActionPathInterpolator 替代。PlaybackState 废弃，由 InputActionPlaybackState 替代。

### 10.5 对比总表

| 概念 | v4 设计 | 本次设计 | 变更类型 |
|---|---|---|---|
| 屏幕分区 | 无 Zone 概念 | Zone A + Zone B | 新增 |
| 布局模式 | Overlay / FullScreen | Stacked / Separated | 修改（重命名） |
| Zone B 行结构 | 线性 Column | 三行（Row 1/2/3） | 新增 |
| 按键面板 | KeyGridPanel | KeyLayoutPanel | 修改（重命名+扩展） |
| 按键布局状态 | KeyLayoutPanelLayoutInfo | KeyLayoutState | 修改（重命名） |
| 候选布局状态 | CandidateListLayoutInfo | CandidateListLayoutState | 修改（重命名） |
| 输入列表布局状态 | InputListLayoutInfo | InputListLayoutState | 修改（重命名） |
| 提示面板 | InfoTipPanel | PopupTipPanel（从 ImeState 读取） | 修改（重命名+数据源变更） |
| 工具面板 | Toolbar（线性排列） | ToolListPanel（互斥切换，含原 Editor 功能） | 新增 |
| 集成组件 | KeyboardPanel + KeyboardScreen | KeyboardHost | 修改（合并重命名） |
| 播放组件 | ExerciseScreen + InputActionPlayer | KeyboardInputActionPlayerHost | 新增 |
| 反馈坐标 | 绝对像素坐标 | 归一化坐标 | 修改 |
| 位置解析 | KeyPositionResolver (绝对坐标) | InputActionPositionResolver (归一化坐标) | 修改（重命名+坐标变更） |
| InputMode | 无（隐含在 KeyboardType 中） | InputMode 枚举（正交于 Type） | 新增 |
| 键盘类型 | 含 Editor | 移除 Editor（合并到 ToolListPanel） | 修改（简化） |
| 指示器覆盖 | FingerOverlay / SwipeTrailOverlay / KeyHighlightOverlay | 面板内建 IndicatorOverlay | 修改（内建化） |
| 手指指示器 | FingerIndicatorState | InputActionFingerIndicator | 修改（重命名） |
| 行指示器 | RowIndicatorState + 独立覆盖层 | InputActionRowIndicator + 面板内建 | 修改（重命名+内建化） |
| 播放状态 | PlaybackState | InputActionPlaybackState | 修改（重命名） |
| 使用模式 | UseMode | InputActionUseMode | 修改（重命名） |
| 路径插值器 | SwipePathInterpolator | InputActionPathInterpolator | 修改（重命名） |
| 反馈状态 | 含 popupTip/keyPath/xPadPath | 仅 touchTrailPoints/pressedKeys/fingerIndicator | 修改（简化） |
| 反馈元素类型 | 含 KeyPath/XPadPathHighlight | 仅 TouchTrail/KeyHighlight/FingerIndicator | 修改（简化合并） |
| 坐标归一化 | 无 | CoordinateNormalizer | 新增 |
| 三列布局 | 无 | Separated 模式 Row 3 三列 | 新增 |
| 实例策略 | 隐含在组件代码中 | 实例策略表（显式定义） | 新增 |
