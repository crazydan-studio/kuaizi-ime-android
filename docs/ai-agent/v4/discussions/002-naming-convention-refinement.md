# 002 — UI 组件 Panel 后缀统一与页面合并

## 基本信息

| 字段 | 值 |
|------|-----|
| 讨论日期 | 2026-05-13 |
| 参与者 | 用户、AI Designer |
| 主题 | Panel 与 Bar/View 后缀的取舍、键盘组件的命名定位、应用层页面的职能划分 |

---

## 讨论内容

### 1. 面板类该用 Panel 还是 Bar 作后缀？

**问题背景**：:ime-ui 模块中，面板和容器类的后缀不统一——`InputPanel`、`KeyPanel` 用 `Panel`，而 `CandidateBar`、`InputBar` 用 `Bar`。`Bar` 通常表示单行横向条状 UI（如 `TopAppBar`、`Snackbar`），但候选项和输入列表在布局上并非严格的单行条状组件。

**探讨过程**：

`Bar` 后缀的问题在于语义偏窄：候选项面板在候选字较多时会多行展示，输入列表面板包含字符项、间隔项和游标，布局复杂度超出"条"的概念。用 `Bar` 命名会误导使用者对组件布局形态的预期。

统一为 `Panel` 后缀的好处是：面板和容器类有一致的命名模式，从类名即能识别其"容器"本质；与 `GestureFeedbackPanel`、`GestureInputPanel` 等已有 Panel 命名形成统一。但代价是 `Panel` 的语义变宽——既包含三层面板这样的区域型组件，也包含候选栏这样的功能型组件，需要通过前缀来区分职能。

另一种方案是引入 `Strip` 或 `Row` 等后缀区分横向条状组件，但这会增加命名体系的复杂度，且这些后缀在 Compose 生态中不是主流惯例。

**结论**：面板和容器类统一使用 `Panel` 后缀，通过前缀区分职能。

### 2. InputPanel 为什么改名为 GestureInputPanel？

**问题背景**：`InputPanel` 作为三层面板中顶层的手势拦截层，其名称中的"Input"语义模糊——输入什么？按键输入？手势输入？文本输入？

**探讨过程**：

`InputPanel` 的核心职责是拦截触摸事件并识别手势，输出 `InputGesture`。叫 `InputPanel` 时，容易与"输入法"本身混淆，也容易与 `InputBar`（输入列表）混淆——两者都含"Input"但职能完全不同。

`GestureInputPanel` 明确表达了"手势输入"的职能：接收手势、识别手势、输出手势事件。前缀 `Gesture` 将其与 `GestureFeedbackPanel`（手势反馈）形成配对——前者是手势的输入端，后者是手势的输出端，命名上呼应清晰。

备选名 `GestureDetectorPanel` 也能表达职能，但 `Detector` 偏向实现细节（检测器），而 `Input` 表达的是面向使用者的功能（手势输入层），后者更符合 Compose 声明式 UI 的命名风格——组件名描述"做什么"而非"怎么做"。

**结论**：`InputPanel` 更名为 `GestureInputPanel`，强调手势输入职能，与 `GestureFeedbackPanel` 配对。

### 3. KeyPanel 为什么改名为 KeyGridPanel？

**问题背景**：`KeyPanel` 渲染键盘按键网格，其名称中的 `Key` 可以理解为"按键"也可以理解为"关键"，且 `KeyPanel` 无法区分不同键盘类型的按键面板变体。

**探讨过程**：

按键面板的布局本质是网格（Grid）——无论是拼音键盘的规则网格、Emoji 面板的网格，还是候选键盘的网格。`KeyGridPanel` 中的 `Grid` 描述了布局特征，也解释了为什么会有 `StandardKeyGridPanel`、`EmojiKeyGridPanel`、`CandidateKeyGridPanel` 等变体——它们都是按键网格，只是内容不同。

`KeyPanel` 的另一个问题是前缀 `Key` 太短太通用，与 Kotlin 的 `Map.Key`、Compose 的 `Modifier.key()` 等概念易混淆。`KeyGrid` 作为复合前缀，搜索友好且语义唯一。

派生类名也跟随变更（`StandardKeyPanel` → `StandardKeyGridPanel` 等），以及相关的 `KeyPanelLayoutInfo` → `KeyGridPanelLayoutInfo`、`KeyPanelPositionResolver` → `KeyGridPanelPositionResolver`，保持命名一致性。

**结论**：`KeyPanel` 更名为 `KeyGridPanel`，以 Grid 布局特征区分，派生类名同步变更。

### 4. KeyboardView 为什么改名为 KeyboardPanel？

**问题背景**：`KeyboardView` 是键盘的顶层组合面板，组合了三层面板、候选面板、输入列表面板等。它的角色是容器/组合器，而非独立的视图组件。

**探讨过程**：

在 Compose 生态中，`View` 后缀通常用于叶子组件（如 `KeyView`、`XPadView`），表示一个独立的可渲染单元。而 `KeyboardView` 是一个组合容器，将多个子面板编排在一起——这在语义上更接近 `Panel`（面板组合）而非 `View`（单一视图）。

统一为 `Panel` 后缀后，:ime-ui 模块的命名体系更清晰：
- `Panel` 后缀 = 容器/组合器（`KeyboardPanel`、`GestureInputPanel`、`CandidatePanel`、`InputListPanel`、`KeyGridPanel`、`GestureFeedbackPanel`）
- `View` 后缀 = 叶子视图（`XPadView`、`InputHostView`、`KeyView`）
- 直接职能名 = 非组合非叶子的功能组件（`EditorField`、`EditorHost`）

这种"容器-叶子-功能"的三分法，让类名本身就能传达组件在层级中的位置。

**结论**：`KeyboardView` 更名为 `KeyboardPanel`，明确其容器角色；`View` 后缀保留给叶子组件。

### 5. InputPracticeScreen 和 ExerciseScreen 为什么要合并？

**问题背景**：文档 930 中设计了 `InputPracticeScreen`（输入练习界面）用于动作程序化的动画演示，而文档 400 迁移对照表中有 `ExerciseScreen`（练习界面）对应 Java 版本的 `ExerciseGuide`。两者功能重叠。

**探讨过程**：

`InputPracticeScreen` 的核心是演示输入动作的动画回放——按键高亮、手指轨迹、自动输入——让用户观看标准输入动作的演示。`ExerciseScreen` 的核心是引导用户完成练习步骤——从 Java 版本的 `ExerciseGuide` 演化而来。

两者面向同一个用户场景：帮助用户熟悉输入法的手势和操作方式。将它们拆成两个页面，用户需要在两个入口之间切换，体验割裂。合并后，`ExerciseScreen` 可以同时提供"观看演示"和"跟随练习"两种模式，用户在同一个界面中先看示范再动手练习，流程更自然。

`ExerciseScreen` 这个名称也比 `InputPracticeScreen` 更简洁，且 `Exercise` 本身就包含"练习"和"演练"两层含义，无需再加 `Input` 前缀——在整个 IME 应用上下文中，练习自然是输入练习。

**结论**：合并为 `ExerciseScreen`，统一练习和演示两种交互模式。

### 6. GuideScreen 为什么更名为 MainScreen？

**问题背景**：`GuideScreen` 对应 Java 版本中 `Guide` Activity，是输入法的主界面。但 `Guide`（引导）一词无法准确表达这个页面的职能。

**探讨过程**：

Java 版本的 `Guide` Activity 名字源于最初设计时它包含引导功能（如首次使用引导、Alpha 协议确认等），但随着功能演进，这个页面已成为输入法的主工作界面——用户与键盘交互的主窗口。`Guide` 这个名字成了历史遗留，与实际职能脱节。

备选名 `HomeScreen` 也能表达"主页面"，但 `Home` 在移动应用中通常指代应用首页（如底部导航的 Home Tab），而 IME 的主界面是浮动在目标应用之上的输入法窗口，语义上不是"家"而是"主工作区"。`MainScreen` 更中性，既表达"主要"又不过度暗示特定的 UI 模式。

**结论**：`GuideScreen` 更名为 `MainScreen`，准确反映其作为应用主界面的职能。

### 7. ComposeKeyPositionResolver 应该放在哪个模块？

**问题背景**：文档 930 最初将 `ComposeKeyPositionResolver` 归在 `:app` 模块（main 源集），但它依赖 Compose 布局系统，而 `:app` 模块不应包含 Compose UI 实现细节。

**探讨过程**：

`KeyPositionResolver` 接口定义在 `:ime-engine`——它是一个纯 Kotlin 的坐标解析抽象契约，不依赖任何 UI 框架。`ComposeKeyPositionResolver` 是其 Compose 实现，通过 `onGloballyPositioned` 和 `layoutInfo` 获取按键的实时位置，这个实现天然依赖 Compose 运行时。

如果放在 `:app`，意味着 :app 需要理解 Compose 布局细节来构造解析器，但这部分逻辑应该在 UI 库中封装。放在 `:ime-ui` 更合理——UI 库既负责渲染按键，也负责提供按键位置的查询能力，这是同一职责的正反两面。

不过这引出一个架构问题：`:ime-ui` 提供了 `ComposeKeyPositionResolver`，但 `InputActionPlayer`（动作播放器）在 `:ime-ui` 中使用它时，需要 UI 库内部就完成坐标解析到动画驱动的闭环，而非让 `:app` 来拼装。

**结论**：`KeyPositionResolver` 接口在 `:ime-engine`，`ComposeKeyPositionResolver` 实现在 `:ime-ui`。

---
