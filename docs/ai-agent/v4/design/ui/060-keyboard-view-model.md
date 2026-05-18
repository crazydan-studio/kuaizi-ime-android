# KeyboardViewModel 设计

`KeyboardViewModel` 划归 `:ime-ui` 模块的 `viewmodel/` 包，作为 UI 层的协调中心，桥接 Compose UI 组件与 `:ime-engine` 引擎。ViewModel 将 UI 手势（`InputGesture`）转换为引擎意图（`ImeIntent`），暴露引擎状态（`StateFlow<ImeState>`）供 Compose 订阅，管理手势反馈状态（`GestureFeedbackState`），提供运行时布局模式切换（`LayoutMode`），以及集成输入动作播放器（`InputActionPlayer`）。ViewModel 仅依赖引擎核心模型和公开 API，平台级职责由 `:app` 模块承担，确保 `:ime-ui` 作为纯 UI 库可被第三方应用即插即用。

---

## 1. 设计定位

### 1.1 模块归属

`KeyboardViewModel` 属于 `:ime-ui` 模块的 `viewmodel/` 包，是 UI 层（UI Layer + ViewModel Layer）的核心组件。

**归属理由**：

| 理由 | 说明 |
|------|------|
| **职能本质是 UI 协调** | ViewModel 将 UI 手势（`InputGesture`）转换为引擎意图（`ImeIntent`），暴露引擎状态（`StateFlow<ImeState>`）供 Compose 订阅，管理 `LayoutMode` 切换和布局状态缓存——这些职能完全属于 UI 层 |
| **仅依赖引擎公开 API** | ViewModel 仅持有 `ImeEngine` 引用，使用其 `handleIntent()`、`state`、`updateConfig()` 等公开 API，不依赖引擎内部实现 |
| **第三方应用需要** | 任何引入 `:ime-engine` + `:ime-ui` 的第三方应用都需要 `KeyboardViewModel` 来驱动 UI。如果 ViewModel 在 `:app`，第三方应用必须自行实现等价组件，违背「即插即用」的设计目标 |
| **集成组件的直接搭档** | `KeyboardHost`、`KeyboardInputActionPlayerHost` 等集成组件均以 ViewModel 为交互入口，二者同属 UI 层、同生同灭 |

### 1.2 不属于 ViewModel 的职责

以下职责由 `:app` 模块承担，`KeyboardViewModel` 不参与：

| 职责 | 承担者 | 说明 |
|------|--------|------|
| 创建 `ImeEngine` | `:app`（`IMEService`） | `ImeEngine.create()` 需要 `ImeDictProvider`（Android `Context` 依赖），由应用层创建后传入 ViewModel |
| 管理 `InputConnectionBridge` | `:app`（`IMEService`） | 桥梁直接挂载到 `ImeEngine`，与 ViewModel 无关；`IMEService` 在 `onStartInput`/`onDestroy` 时管理桥梁生命周期 |
| 配置持久化 | `:app`（`ConfigDataStore`） | DataStore 读写是应用层职责；ViewModel 仅通过 `engine.updateConfig()` 修改运行时配置 |
| 系统输入法服务生命周期 | `:app`（`IMEService`） | `InputMethodService` 的 `onCreate`/`onDestroy`/`onStartInput` 回调属于平台层 |

### 1.3 与 `:app` 模块的职责边界

```plantuml
@file:../diagrams/app-viewmodel-boundary.puml
```

`KeyboardViewModel` 与 `:app` 模块的交互仅通过构造参数注入的 `ImeEngine` 完成。ViewModel 暴露的 API 分为三类：第一类是状态读取（`state`、`config`、`layoutMode`、`feedbackState`、`actionPlayer`），由集成组件通过 `collectAsState()` 订阅；第二类是意图分发（`handleGesture()`、`handleIntent()`），由集成组件的用户交互回调触发；第三类是运行时配置（`updateConfig()`、`setLayoutMode()`、`updateKeyLayoutState()` 等），由集成组件的布局回调触发。`IMEService` 在 `onCreateInputView()` 中通过 `KeyboardViewModel.Factory(engine)` 注入预创建的引擎，ViewModel 不感知桥梁、持久化和服务生命周期。

---

## 2. KeyboardViewModel 类设计

**KeyboardViewModel 规格表**：

| 属性 | 说明 |
|------|------|
| 角色 | `:ime-ui` 模块的 UI 协调中心，桥接 Compose UI 与 `:ime-engine` |
| 职责 | 手势/意图分发、状态暴露、布局模式管理、反馈状态持有、动作播放器集成、布局状态缓存、运行时配置修改 |
| 约束 | 仅依赖引擎公开 API；不持有 `InputConnectionBridge`；不执行配置持久化；不创建/销毁引擎 |
| 关键属性 | state: StateFlow\<ImeState\>, config: ImeConfig, layoutMode: StateFlow\<LayoutMode\>, feedbackState: GestureFeedbackState, actionPlayer: InputActionPlayer |
| 关键方法 | handleGesture(), handleIntent(), setLayoutMode(), updateConfig(), updateKeyLayoutState(), updateCandidateLayoutState(), updateInputListLayoutState() |
| 布局状态缓存 | _currentKeyLayoutState, _currentCandidateLayoutState, _currentInputListLayoutState |
| 所属包 | org.crazydan.studio.ime.ui.viewmodel |
| 所属模块 | :ime-ui |

### 2.1 完整类定义

```kotlin
package org.crazydan.studio.ime.ui.viewmodel

/**
 * 键盘视图模型，`:ime-ui` 模块的 UI 协调中心。
 *
 * 桥接 Compose UI 组件与 `:ime-engine` 引擎：
 * - 将 UI 手势（InputGesture）转换为引擎意图（ImeIntent）
 * - 暴露引擎状态（StateFlow<ImeState>）供 Compose 订阅
 * - 管理手势反馈状态（GestureFeedbackState）
 * - 管理运行时布局模式（LayoutMode）
 * - 提供输入动作播放器（InputActionPlayer）
 * - 缓存面板布局状态供播放器坐标解析
 *
 * ViewModel 仅依赖引擎核心模型。
 * 平台级职责（ImeEngine 创建、InputConnectionBridge 管理、配置持久化）
 * 均由 `:app` 模块承担。
 *
 * 使用方式：
 * ```kotlin
 * // :app 模块的 IMEService 中创建
 * val engine = ImeEngine.create(
 *     config = ImeConfig(...),
 *     dictProvider = ImeSqliteDictProvider(context),
 * )
 * engine.attachOutputBridge(InputConnectionBridge { currentInputConnection })
 *
 * val viewModel: KeyboardViewModel = viewModel(
 *     factory = KeyboardViewModel.Factory(engine)
 * )
 * KeyboardHost(viewModel = viewModel)
 * ```
 */
class KeyboardViewModel(
    private val engine: ImeEngine,
) : ViewModel() {

    // ─── 状态暴露 ────────────────────────────────────────────────

    /** 引擎状态，供 Compose 订阅 */
    val state: StateFlow<ImeState> = engine.state

    /** 当前 ImeConfig 快照，便于 UI 组件快速访问 */
    val config: ImeConfig get() = state.value.config

    // ─── 布局模式 ────────────────────────────────────────────────

    /**
     * 运行时布局模式。
     *
     * 由 ViewModel 管理，KeyboardHost 订阅此状态决定组件部署方式。
     * 支持 Stacked（堆叠）和 Separated（分离）两种模式，
     * 可在运行时通过 setLayoutMode() 动态切换。
     * 切换时组件按实例策略表重新部署，所有状态通过
     * GestureFeedbackState 和 ImeState 保持连续。
     */
    private val _layoutMode = MutableStateFlow<LayoutMode>(LayoutMode.Stacked)
    val layoutMode: StateFlow<LayoutMode> = _layoutMode.asStateFlow()

    /**
     * 切换布局模式。
     *
     * 切换时 KeyboardHost 按实例策略表重新部署组件：
     * - Stacked -> Separated：KeyLayoutPanel 从 Zone B Row 3 迁移到 Zone A，
     *   GestureFeedbackPanel 从单实例拆分为双实例
     * - Separated -> Stacked：反向操作
     * 切换过程中状态通过 GestureFeedbackState 和 ImeState 保持连续。
     */
    fun setLayoutMode(mode: LayoutMode) {
        _layoutMode.value = mode
    }

    // ─── 手势反馈状态 ────────────────────────────────────────────

    /**
     * 手势反馈状态，独立于 ImeState，使用归一化坐标。
     *
     * 由 GestureInputPanel 的手势事件驱动更新，
     * 供 GestureFeedbackPanel 消费渲染。
     *
     * 反馈状态与 ImeState 分离：
     * - ImeState 描述键盘的逻辑状态（哪个键被选中、候选项列表、弹出提示等）
     * - GestureFeedbackState 描述手势的视觉反馈（滑行轨迹、按键高亮、手指指示器）
     *
     * 这种分离确保了反馈状态的高频更新不会触发 ImeState 的变更，
     * 避免不必要的按键面板重组。
     */
    val feedbackState = GestureFeedbackState()

    // ─── 输入动作播放器 ──────────────────────────────────────────

    /**
     * 输入动作播放器，坐标无关。
     *
     * 通过 viewModel.handleIntent() 驱动引擎状态转换，
     * 通过 feedbackState 驱动手势反馈动画。
     *
     * 使用 ComposeInputActionPositionResolver 解析归一化坐标，
     * 该解析器从 ViewModel 的布局状态缓存中读取面板布局信息，
     * 将按键语义标识映射为归一化坐标位置。
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

    // ─── 布局状态缓存 ────────────────────────────────────────────

    /**
     * 当前面板布局状态缓存，供 ComposeInputActionPositionResolver 使用。
     *
     * 布局状态由各面板组件的 onLayoutStateChanged 回调更新：
     * - KeyLayoutPanel -> updateKeyLayoutState()
     * - CandidateListPanel -> updateCandidateLayoutState()
     * - InputListPanel -> updateInputListLayoutState()
     *
     * 这些缓存使得 InputActionPlayer 可以在不依赖 Compose 上下文的情况下
     * 解析按键的归一化坐标位置。
     */
    private var _currentKeyLayoutState: KeyLayoutState? = null
    private var _currentCandidateLayoutState: CandidateListLayoutState? = null
    private var _currentInputListLayoutState: InputListLayoutState? = null

    /** 更新按键布局状态缓存 */
    fun updateKeyLayoutState(state: KeyLayoutState) {
        _currentKeyLayoutState = state
    }

    /** 更新候选列表布局状态缓存 */
    fun updateCandidateLayoutState(state: CandidateListLayoutState) {
        _currentCandidateLayoutState = state
    }

    /** 更新输入列表布局状态缓存 */
    fun updateInputListLayoutState(state: InputListLayoutState) {
        _currentInputListLayoutState = state
    }

    // ─── 手势与意图处理 ──────────────────────────────────────────

    /**
     * 处理输入手势。
     *
     * 将 InputGesture（UI 面板的输出）转换为 ImeIntent（引擎的输入），
     * 然后委托引擎执行 reduce。
     *
     * 两层转换的意义：
     * - InputGesture 表达「用户做了什么手势」，属于输入面板的领域
     * - ImeIntent 表达「系统应该做什么」，属于 ViewModel/引擎的领域
     * - 同一手势可以根据当前键盘状态产生不同的 Intent
     * - 不同手势也可以产生相同的 Intent
     */
    fun handleGesture(gesture: InputGesture) {
        val intent = gestureToIntent(gesture)
        engine.handleIntent(intent)
    }

    /**
     * 处理意图。
     *
     * 直接发送 ImeIntent 到引擎，适用于：
     * - UI 组件的直接操作（如候选选择、光标移动、键盘切换）
     * - InputActionPlayer 的程序化输入
     * - 配置变更（UpdateConfig）
     */
    fun handleIntent(intent: ImeIntent) {
        engine.handleIntent(intent)
    }

    // ─── 配置修改 ────────────────────────────────────────────────

    /**
     * 更新运行时配置。
     *
     * 委托引擎更新 ImeConfig，UI 通过 ImeState.config 自动同步。
     * 运行时修改始终优先于持久化配置（详见 ImeConfig 设计文档）。
     *
     * 注意：此方法仅修改运行时配置，不做持久化。
     * 持久化由 `:app` 模块的 ConfigDataStore 负责。
     */
    fun updateConfig(transform: (ImeConfig) -> ImeConfig) {
        engine.updateConfig(transform)
    }

    // ─── 生命周期 ────────────────────────────────────────────────

    override fun onCleared() {
        super.onCleared()
        // ViewModel 不负责销毁引擎——引擎的生命周期由 :app 管理
        // 此处仅清理 ViewModel 自身资源
        feedbackState.clear()
    }

    // ─── 内部实现 ────────────────────────────────────────────────

    /**
     * 将 InputGesture 转换为 ImeIntent。
     *
     * 转换逻辑可能根据当前键盘状态产生不同的 Intent，
     * 例如：同一 Tap 手势在 PinyinInput 状态下产生 PressKey，
     * 在 CandidateSelection 状态下可能产生 SelectCandidate。
     */
    private fun gestureToIntent(gesture: InputGesture): ImeIntent {
        return when (gesture) {
            is InputGesture.Tap -> ImeIntent.PressKey(gesture.key, KeyGesture.Tap)
            is InputGesture.LongPress -> ImeIntent.PressKey(gesture.key, KeyGesture.LongPress)
            is InputGesture.Swipe -> ImeIntent.PressKey(gesture.endKey, KeyGesture.Swipe)
            is InputGesture.Flip -> ImeIntent.PressKey(
                gesture.startKey,
                KeyGesture.Flip(gesture.direction),
            )
            is InputGesture.XPadZonePath -> ImeIntent.SelectXPadPath(
                gesture.startZone,
                gesture.path,
            )
            is InputGesture.CandidateTap -> ImeIntent.SelectCandidate(
                /* 根据 gesture.index 从当前候选列表中获取 */
            )
        }
    }

    // ─── 工厂 ────────────────────────────────────────────────────

    /**
     * ViewModel 工厂，由 `:app` 模块提供预创建的 ImeEngine。
     *
     * ```kotlin
     * // IMEService 中
     * val factory = KeyboardViewModel.Factory(engine)
     * val viewModel: KeyboardViewModel = viewModel(factory = factory)
     * ```
     */
    class Factory(
        private val engine: ImeEngine,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return KeyboardViewModel(engine) as T
        }
    }
}
```

---

## 3. GestureFeedbackState

**GestureFeedbackState 规格表**：

| 属性 | 说明 |
|------|------|
| 角色 | 手势视觉反馈状态管理，独立于 ImeState |
| 职责 | 管理触摸轨迹、按键高亮、手指指示器三类纯视觉反馈数据 |
| 约束 | 所有坐标使用归一化形式 [0,1]x[0,1]；不管理弹出提示（由 ImeState 管理）；不管理独立的按键间路径和 X-Pad 路径（统一合并到 touchTrailPoints） |
| 状态字段 | touchTrailPoints: StateFlow\<List\<OffsetF\>\>, pressedKeys: StateFlow\<Set\<InputKey\>\>, fingerIndicator: StateFlow\<InputActionFingerIndicator?\> |
| 更新方法 | addTouchTrailPoint(), setTouchTrailPoints(), clearTouchTrail(), setPressedKeys(), clearPressedKeys(), setFingerIndicator(), clearAll(), clear() |
| 所属包 | org.crazydan.studio.ime.ui.viewmodel |
| 所属模块 | :ime-ui |

### 3.1 设计说明

`GestureFeedbackState` 经过简化后，职责收窄为纯粹的视觉反馈。所有坐标数据以归一化形式 `[0,1]x[0,1]` 存储，绘制时由 `GestureFeedbackPanel` 根据面板实际尺寸转换为像素坐标。这使得同一份反馈数据可以正确地在不同尺寸的面板实例上渲染，包括 Zone A 和 Zone B 的双实例场景。

简化后的状态包含三类核心视觉反馈：触摸轨迹点（`touchTrailPoints`，含按键间路径和 X-Pad 路径的插值点）、按键高亮集合（`pressedKeys`）、手指指示器状态（`fingerIndicator`）。弹出提示由 `ImeState` 管理。按键间路径和 X-Pad 路径统一合并到 `touchTrailPoints` 中，由 `KeyLayoutPanel` 根据 `InputMode` 计算起止按键间的平滑曲线后，作为插值路径点统一写入。

`InputActionFingerIndicator` 的类型定义在 [engine/060-input-action.md](../engine/060-input-action.md) 中，此处直接引用。

### 3.2 完整类定义

```kotlin
package org.crazydan.studio.ime.ui.viewmodel

/**
 * 手势反馈状态，使用归一化坐标，职责简化为纯视觉反馈。
 *
 * 关键设计决策：
 * 1. 所有坐标数据以归一化形式 [0,1]x[0,1] 存储，
 *    绘制时由 GestureFeedbackPanel 根据面板实际尺寸转换为像素坐标。
 *    这使得同一份反馈数据可在不同 Zone、不同尺寸的面板实例上正确渲染。
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
     * 轨迹计算说明：touchTrailPoints 不仅包含手指的实际触摸点，
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
     *
     * 类型定义见 [engine/060-input-action.md](../engine/060-input-action.md) 中的 InputActionFingerIndicator。
     */
    private val _fingerIndicator = MutableStateFlow<InputActionFingerIndicator?>(null)
    val fingerIndicator: StateFlow<InputActionFingerIndicator?> = _fingerIndicator.asStateFlow()

    // ─── 更新方法 ──────────────────────────────────────────────

    /** 追加单个触摸轨迹点（归一化坐标） */
    fun addTouchTrailPoint(normalizedPoint: OffsetF) {
        _touchTrailPoints.update { it + normalizedPoint }
    }

    /**
     * 设置完整轨迹（含按键间路径和 X-Pad 路径的插值点）。
     *
     * 用于 InputActionPlayer 在播放滑行动作时，
     * 通过 InputActionPathInterpolator 生成插值路径后
     * 一次性写入完整轨迹。
     */
    fun setTouchTrailPoints(points: List<OffsetF>) {
        _touchTrailPoints.value = points
    }

    /** 清除触摸轨迹 */
    fun clearTouchTrail() {
        _touchTrailPoints.value = emptyList()
    }

    /** 设置当前按下的按键集合 */
    fun setPressedKeys(keys: Set<InputKey>) {
        _pressedKeys.value = keys
    }

    /** 清除按键高亮 */
    fun clearPressedKeys() {
        _pressedKeys.value = emptySet()
    }

    /**
     * 设置手指指示器状态。
     *
     * 由 InputActionPlayer 在播放过程中调用，
     * 更新虚拟手指的归一化坐标位置和按压状态。
     */
    fun setFingerIndicator(state: InputActionFingerIndicator?) {
        _fingerIndicator.value = state
    }

    /**
     * 手势结束，清除所有临时反馈（轨迹和高亮）。
     *
     * 手指指示器不被清除，因为它由播放器独立管理，
     * 其生命周期与播放状态绑定而非与手势绑定。
     */
    fun clearAll() {
        clearTouchTrail()
        clearPressedKeys()
    }

    /**
     * 清理所有状态，在 ViewModel.onCleared() 时调用。
     *
     * 包括手指指示器在内的所有状态都被清除。
     */
    fun clear() {
        clearAll()
        _fingerIndicator.value = null
    }
}
```

### 3.3 状态字段与消费者对照表

| 状态字段 | 生产者 | 消费者 | 坐标类型 |
|----------|--------|--------|----------|
| touchTrailPoints | GestureInputPanel（用户手势）, InputActionPlayer（程序化播放，含 InputActionPathInterpolator 插值） | GestureFeedbackPanel（反归一化后绘制轨迹） | 归一化 [0,1]x[0,1] |
| pressedKeys | GestureInputPanel（手势过程中更新）, InputActionPlayer（播放时更新） | GestureFeedbackPanel（通过 KeyLayoutState 映射按键位置后绘制高亮） | 语义标识，非坐标 |
| fingerIndicator | InputActionPlayer（播放时驱动） | GestureFeedbackPanel（反归一化后绘制手指指示器） | 归一化 [0,1]x[0,1] |

---

## 4. ImeState UI 层扩展

**ImeState 扩展字段规格表**：

| 字段 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| inputMode | InputMode | RectGrid | 当前输入模式，决定布局几何和交互范式；与 Keyboard.Type 正交 |
| isInputting | Boolean | false | 是否正在输入，控制 Row 2 面板互斥切换 |
| toolList | ToolListState | emptyList() | 工具列表状态，含编辑功能键 |
| popupTip | PopupTipState? | null | 弹出提示状态，由引擎 reduce 写入，PopupTipPanel 消费 |

### 4.1 设计说明

`ImeState` 需要扩展以支持新的 UI 层概念。新增 `inputMode` 字段表示当前输入模式（决定按键布局几何和交互范式），`isInputting` 字段表示是否正在输入（控制 `ToolListPanel` 和 `InputListPanel` 的互斥切换），`toolList` 字段提供工具列表状态（含编辑功能键），以及 `popupTip` 字段提供弹出提示状态（弹出提示由引擎处理意图后更新 ImeState 触发显示）。这些扩展仅涉及 UI 层状态的暴露，不改变 `:ime-engine` 的核心 reduce 逻辑——引擎仍然通过 `ImeIntent` 驱动状态转换，UI 层从 `ImeState` 中读取新增字段来决定面板的部署和切换。

### 4.2 扩展字段定义

```kotlin
/**
 * ImeState 的 UI 层扩展字段。
 *
 * 这些字段由引擎的 reduce 逻辑计算并写入 ImeState，
 * UI 层通过 collectAsState() 订阅后驱动面板的部署和切换。
 */
data class ImeState(
    // ... 现有字段（keyboardType, keyGrid, keyboardState, candidateList, inputList, config 等） ...

    /**
     * 当前输入模式，决定按键布局几何和交互范式。
     *
     * InputMode 与 Keyboard.Type 正交组合：
     * 任意 InputMode 可与任意 Type 组合，产生不同的按键布局和交互体验。
     * KeyLayoutPanel 根据 inputMode 选择布局策略，
     * GestureInputPanel 根据 inputMode 选择手势识别策略。
     */
    val inputMode: InputMode = InputMode.RectGrid,

    /**
     * 是否正在输入，控制 ToolListPanel/InputListPanel 的互斥切换。
     *
     * isInputting == true 时显示 InputListPanel，
     * isInputting == false 时显示 ToolListPanel。
     */
    val isInputting: Boolean = false,

    /**
     * 工具列表状态（含编辑功能键）。
     *
     * 编辑功能键（如全选、复制、粘贴、
     * 剪切、撤销、重做等）统一由 ToolListPanel 中作为 ToolItem 管理，
     * 在任何键盘类型下均可通过工具栏快速访问。
     */
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

### 4.3 弹出提示状态

```kotlin
/**
 * 弹出提示状态。
 *
 * 短暂显示操作信息（如按键操作结果、已输入字符、功能切换提示等）。
 * PopupTipPanel 叠加在 CandidateListPanel 上方，短暂浮现后自动消失。
 *
 * 弹出提示由 ImeState 管理。
 */
data class PopupTipState(
    /** 提示消息内容 */
    val message: String,
    /** 创建时间戳，用于控制自动消失 */
    val timestamp: Long = System.currentTimeMillis(),
)
```

### 4.4 工具列表状态

```kotlin
/**
 * 工具列表状态。
 *
 * 空闲时展示固定工具按钮（剪贴板粘贴、收藏管理、设置、键盘切换、
 * 编辑功能等）。编辑功能键统一由 ToolListPanel 管理。
 */
data class ToolListState(
    /** 工具项列表 */
    val tools: List<ToolItem>,
)

/**
 * 工具项。
 *
 * 每个工具项对应一个可点击的功能按钮，
 * 点击后发送关联的 ImeIntent。
 */
data class ToolItem(
    /** 按钮标签 */
    val label: String,
    /** 按钮图标 */
    val icon: ImageVector?,
    /** 点击时发送的意图 */
    val intent: ImeIntent,
)
```

---

## 5. 与集成组件的协作

### 5.1 KeyboardHost

`KeyboardHost` 是顶层集成组件，通过 `viewModel.layoutMode` 统一两种布局模式的入口。`KeyboardHost` 订阅 `viewModel.state`、`viewModel.layoutMode` 和 `viewModel.feedbackState`，根据布局模式选择 `StackedLayout` 或 `SeparatedLayout`。

```kotlin
/**
 * 键盘宿主组件，顶层集成组件。
 *
 * 通过 LayoutMode 参数统一两种布局模式的入口。
 * 支持运行时动态切换 LayoutMode，切换时按实例策略表重新部署组件。
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
                viewModel, state, feedbackState, keyLayoutState,
                layoutMode as LayoutMode.Separated,
            )
        }
    }
}
```

`StackedLayout` 中，所有组件集中在 Zone B，三层面板叠加共享 Row 3 空间。`KeyLayoutPanel` 部署在 Zone B Row 3，`GestureFeedbackPanel` 和 `GestureInputPanel` 叠加在其上方。Row 1 承载 `CandidateListPanel` 和 `PopupTipPanel` 的叠加，Row 2 根据 `state.isInputting` 互斥切换 `ToolListPanel` 和 `InputListPanel`。

`SeparatedLayout` 中，Zone A 承载 `KeyLayoutPanel` 和 `GestureFeedbackPanel` 的叠加，Zone B 包含三行结构。Row 3 被进一步划分为三列：左列和右列放置功能按钮，中列承载 `GestureFeedbackPanel` 和 `GestureInputPanel` 的叠加。

各面板的用户交互均通过 `viewModel.handleGesture()` 或 `viewModel.handleIntent()` 分发意图。`KeyLayoutPanel` 的 `onLayoutStateChanged` 回调更新 `keyLayoutState` 局部变量，同时应调用 `viewModel.updateKeyLayoutState()` 更新 ViewModel 的布局状态缓存，供 `InputActionPlayer` 的坐标解析器使用。

### 5.2 KeyboardInputActionPlayerHost

`KeyboardInputActionPlayerHost` 是输入动作播放演示集成组件，内部组合 `KeyboardHost` 和播放引擎，支持 Animation 和 DirectInput 两种使用模式。

```kotlin
/**
 * 输入动作播放宿主组件。
 *
 * 支持两种 UseMode：
 * - Animation：不可中断的动画播放模式，访问真实字典数据但不提交到目标编辑器，
 *   不写入数据库，仅展示输入过程动画。此模式下各面板的 showIndicator=true，
 *   指示器状态通过面板的 indicatorState 参数传入，在面板内部绘制。
 * - DirectInput：封装 KeyboardHost 提供完整输入支持，
 *   在此基础上叠加播放引擎。此模式下 showIndicator=false，
 *   不显示行指示器动画，仅通过 GestureFeedbackPanel 绘制手指指示器。
 */
@Composable
fun KeyboardInputActionPlayerHost(
    viewModel: KeyboardViewModel,
    useMode: KeyboardInputActionPlayerHost.UseMode,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val playerState by viewModel.actionPlayer.playbackState.collectAsState()

    // 判断是否显示指示器：仅 Animation 模式下播放中才显示
    val showIndicators = useMode is KeyboardInputActionPlayerHost.UseMode.Animation
            && playerState is InputActionPlaybackState.Playing

    // 指示器状态（仅 Animation 模式下有意义）
    val row1Indicator = viewModel.actionPlayer.row1IndicatorState
    val row2Indicator = viewModel.actionPlayer.row2IndicatorState

    Box(modifier = modifier.fillMaxSize()) {
        when (useMode) {
            is KeyboardInputActionPlayerHost.UseMode.Animation -> {
                // Animation 模式：传递 showIndicator 和 indicatorState 到各面板
                KeyboardHostWithIndicators(
                    viewModel = viewModel,
                    showIndicators = showIndicators,
                    row1Indicator = row1Indicator,
                    row2Indicator = row2Indicator,
                )
            }
            is KeyboardInputActionPlayerHost.UseMode.DirectInput -> {
                // DirectInput 模式：不显示行指示器
                KeyboardHost(viewModel = viewModel)
            }
        }
    }
}
```

`KeyboardInputActionPlayerHost` 从 `viewModel.actionPlayer` 读取播放状态和指示器状态。行指示器（`InputActionFingerIndicator`）通过面板的 `showIndicator` 和 `indicatorState` 参数内建绘制，无需外部覆盖层。手指指示器（`InputActionFingerIndicator`）通过 `GestureFeedbackPanel` 绘制，从 `feedbackState.fingerIndicator` 读取归一化坐标后反归一化渲染。`InputActionPlaybackState`、`InputActionFingerIndicator` 的类型定义见 [engine/060-input-action.md](../engine/060-input-action.md)。

### 5.3 InputActionPlayer 协作

`InputActionPlayer` 通过 `KeyboardViewModel` 的公开 API 完成两条协作路径：引擎状态转换和视觉反馈动画。

引擎状态转换路径：`InputActionPlayer` 执行 `InputAction` 时，调用 `viewModel.handleIntent(ImeIntent)` 将意图发送到引擎，引擎通过 reduce 更新 `ImeState`，UI 层通过 `collectAsState()` 订阅后驱动各面板更新。这条路径与用户手动操作的路径完全一致，确保程序化输入与真实输入行为等价。

视觉反馈动画路径：`InputActionPlayer` 执行 `InputAction` 时，通过 `ComposeInputActionPositionResolver` 将按键语义标识解析为归一化坐标，然后调用 `feedbackState.setFingerIndicator()` 更新手指指示器位置，调用 `feedbackState.setPressedKeys()` 更新按键高亮，调用 `feedbackState.setTouchTrailPoints()` 写入插值轨迹（由 `InputActionPathInterpolator` 计算生成）。`GestureFeedbackPanel` 订阅 `feedbackState` 的变化后，反归一化坐标并绘制动画。

```kotlin
// InputActionPlayer 与 KeyboardViewModel 的协作示意
class InputActionPlayer(
    private val viewModel: KeyboardViewModel,
    private val feedbackState: GestureFeedbackState,
    private val positionResolver: InputActionPositionResolver,
    private val scope: CoroutineScope,
) {
    // ... 播放控制逻辑 ...

    private fun executeAction(action: InputAction) {
        when (action) {
            is InputAction.KeyDown -> {
                // 解析按键归一化坐标
                val position = positionResolver.resolve(action.key) ?: return
                // 更新手指指示器
                feedbackState.setFingerIndicator(
                    InputActionFingerIndicator(position = position, pressed = true)
                )
                // 更新按键高亮
                feedbackState.setPressedKeys(setOf(action.key))
                // 驱动引擎状态转换
                viewModel.handleIntent(ImeIntent.PressKey(action.key, KeyGesture.Tap))
            }
            is InputAction.SwipeTo -> {
                val fromPos = positionResolver.resolve(action.fromKey) ?: return
                val toPos = positionResolver.resolve(action.toKey) ?: return
                // 通过插值器生成归一化坐标轨迹
                val trailPoints = InputActionPathInterpolator.interpolate(
                    from = fromPos, to = toPos
                )
                feedbackState.setTouchTrailPoints(trailPoints)
                feedbackState.setFingerIndicator(
                    InputActionFingerIndicator(position = toPos, pressed = true)
                )
                // 驱动引擎状态转换
                viewModel.handleIntent(ImeIntent.PressKey(action.toKey, KeyGesture.Swipe))
            }
            is InputAction.SelectCandidate -> {
                val position = positionResolver.resolveCandidatePosition(action.candidateIndex)
                    ?: return
                feedbackState.setFingerIndicator(
                    InputActionFingerIndicator(position = position, pressed = true)
                )
                viewModel.handleIntent(ImeIntent.SelectCandidate(/* ... */))
            }
            is InputAction.SwitchKeyboard -> {
                viewModel.handleIntent(ImeIntent.SwitchKeyboard(action.targetType))
            }
            // ... 其他动作 ...
        }
    }
}
```

`ComposeInputActionPositionResolver` 是 `InputActionPositionResolver` 接口的 Compose 实现，从 ViewModel 的布局状态缓存中读取面板布局信息，将按键语义标识映射为归一化坐标。接口定义见 [engine/060-input-action.md](../engine/060-input-action.md)。

```kotlin
/**
 * 基于 Compose 布局状态的坐标解析器。
 *
 * 从 ViewModel 的布局状态缓存中读取面板布局信息，
 * 将按键语义标识和列表索引映射为归一化坐标。
 */
class ComposeInputActionPositionResolver(
    private val keyboardLayoutStateProvider: () -> KeyLayoutState?,
    private val candidateLayoutStateProvider: () -> CandidateListLayoutState?,
    private val inputListLayoutStateProvider: () -> InputListLayoutState?,
) : InputActionPositionResolver {

    override fun resolve(key: InputKey): OffsetF? {
        val layoutState = keyboardLayoutStateProvider() ?: return null
        return layoutState.keyPositions[key]?.center
    }

    override fun resolveCandidatePosition(index: Int): OffsetF? {
        val layoutState = candidateLayoutStateProvider() ?: return null
        val rect = layoutState.candidatePositions.getOrNull(index) ?: return null
        val panelSize = layoutState.panelSize
        if (panelSize == Size.Zero) return null
        // 将屏幕坐标转换为归一化坐标
        return OffsetF(
            x = rect.centerX() / panelSize.width,
            y = rect.centerY() / panelSize.height,
        )
    }

    override fun resolveInputItemPosition(index: Int): OffsetF? {
        val layoutState = inputListLayoutStateProvider() ?: return null
        val rect = layoutState.itemPositions.getOrNull(index) ?: return null
        val panelSize = layoutState.panelSize
        if (panelSize == Size.Zero) return null
        return OffsetF(
            x = rect.centerX() / panelSize.width,
            y = rect.centerY() / panelSize.height,
        )
    }
}
```

---

## 6. `:app` 模块集成方式

### 6.1 IMEService 组装

`IMEService` 是 `:app` 模块的平台入口，负责创建引擎、挂载桥梁、注入 ViewModel。集成代码使用 `KeyboardHost`，通过 `viewModel.layoutMode` 统一布局模式管理。

```kotlin
class IMEService : InputMethodService() {
    private var engine: ImeEngine? = null
    private var bridge: InputConnectionBridge? = null
    private var composeView: ComposeView? = null

    override fun onCreate() {
        super.onCreate()
        // 创建引擎
        engine = ImeEngine.create(
            config = ImeConfig(),
            dictProvider = ImeSqliteDictProvider(this),
        )
        // 创建并挂载输出桥梁
        bridge = InputConnectionBridge { currentInputConnection }
        engine?.attachOutputBridge(bridge!!)
    }

    override fun onCreateInputView(): View {
        val engine = this.engine ?: error("Engine not initialized")
        return ComposeView(this).also { composeView = it }.apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
            )
            setContent {
                val viewModel: KeyboardViewModel = viewModel(
                    factory = KeyboardViewModel.Factory(engine)
                )
                KeyboardHost(viewModel = viewModel)
            }
        }
    }

    override fun onDestroy() {
        // 断开桥梁并销毁引擎
        engine?.detachOutputBridge()
        engine = null
        bridge = null
        composeView?.disposeComposition()
        composeView = null
        super.onDestroy()
    }
}
```

**关键设计**：

| 步骤 | 说明 |
|------|------|
| `onCreate` 创建引擎 | 引擎在 Service 创建时初始化，确保 `onCreateInputView` 时引擎已就绪 |
| `onCreate` 挂载桥梁 | `InputConnectionBridge` 在引擎创建后立即挂载，确保输入提交流程完整 |
| `onCreateInputView` 注入 ViewModel | 通过 `KeyboardViewModel.Factory(engine)` 将预创建的引擎注入 ViewModel，使用 `KeyboardHost` 作为集成组件 |
| `onDestroy` 断开桥梁 | 先断开桥梁再清空引用，确保无悬挂回调 |
| ViewModel 不持有桥梁 | 桥梁由 `IMEService` 管理，与 ViewModel 生命周期独立 |

### 6.2 配置持久化协作

`ConfigDataStore`（`:app`）与 `KeyboardViewModel`（`:ime-ui`）的协作：

```plantuml
@file:../diagrams/app-config-persistence.puml
```

`:app` 模块在配置变更时需同时执行两个操作：
1. `ConfigDataStore.updateConfig()` -- 持久化到 DataStore
2. `viewModel.updateConfig()` -- 修改运行时配置，触发 UI 更新

运行时优先原则保证：通过 `QuickSettingsPopup` 等键盘 UI 进行的临时修改只调用 `viewModel.updateConfig()`，不写入 DataStore；重启后由 `ConfigDataStore` 从 DataStore 恢复配置。

---

## 7. 完整数据流

### 7.1 用户手势输入数据流

用户手势输入遵循 MVI 模式，从手势事件到 UI 更新的完整数据流如下。关键特征在于归一化坐标的使用和弹出提示由 `ImeState` 驱动。

```
GestureInputPanel (Zone B)
  | 接收触摸事件
  | 归一化: eventX/panelWidth -> OffsetF
  | 写入 GestureFeedbackState (归一化坐标)
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
  | inputMode -> KeyLayoutPanel (布局策略选择)
  | isInputting -> ToolListPanel/InputListPanel (互斥切换)
  | popupTip -> PopupTipPanel (从 ImeState 读取)
  | toolList -> ToolListPanel (工具列表)
  | candidateList -> CandidateListPanel
  | inputList -> InputListPanel
  v
各面板组件重组
```

### 7.2 程序化输入数据流

程序化输入由 `InputActionPlayer` 驱动，坐标无关，通过归一化坐标解析和插值生成视觉反馈动画。

```
InputActionScript (坐标无关)
  |
  v
InputActionPlayer.executeAction(InputAction)
  | 查询 InputActionPositionResolver.resolve(key)
  |   -> ComposeInputActionPositionResolver
  |   -> 从 ViewModel 布局状态缓存读取 KeyLayoutState
  |   -> 返回归一化坐标 OffsetF
  |
  | KeyLayoutPanel 根据 InputMode 计算轨迹形状
  | 通过 InputActionPathInterpolator 生成归一化坐标插值路径
  | 写入 GestureFeedbackState (归一化坐标)
  v
  +---> GestureFeedbackState
  |     | fingerIndicator: InputActionFingerIndicator (归一化坐标)
  |     | pressedKeys: Set<InputKey>
  |     | touchTrailPoints: List<OffsetF> (含插值轨迹)
  |     v
  |     GestureFeedbackPanel (Zone A / Zone B)
  |       | 反归一化: OffsetF * panelSize -> Offset
  |       | 绘制轨迹、高亮、手指指示器
  |       v
  |
  +---> KeyboardViewModel.handleIntent(ImeIntent)
        | (Animation 模式不提交到编辑器)
        v
        ImeEngine.handleIntent() -> ImeState
        | popupTip -> PopupTipPanel
        | row1Indicator -> CandidateListPanel(showIndicator=true)
        | row2Indicator -> InputListPanel/ToolListPanel(showIndicator=true)
        v
        各面板组件重组
```

### 7.3 布局模式切换数据流

布局模式切换由 `viewModel.setLayoutMode()` 触发，`KeyboardHost` 订阅 `layoutMode` 状态后按实例策略表重新部署组件。切换过程中所有状态保持连续，不丢失手势或输入进度信息。

```
用户/应用调用
  | viewModel.setLayoutMode(LayoutMode.Separated)
  v
KeyboardViewModel._layoutMode
  | MutableStateFlow 更新
  v
KeyboardHost
  | collectAsState() 收到新 layoutMode
  | when (layoutMode) 选择 SeparatedLayout
  v
组件重新部署（Stacked -> Separated）：
  | KeyLayoutPanel: Zone B Row 3 -> Zone A
  | GestureFeedbackPanel: Zone B 单实例 -> Zone A + Zone B 双实例
  | GestureInputPanel: Zone B 整行 -> Zone B Row 3 中列
  | Zone A 容器创建并填充内容
  v
状态连续性保证：
  - ImeState 通过 engine.state 保持，不受布局切换影响
  - GestureFeedbackState 通过 feedbackState 保持，轨迹和高亮不丢失
  - InputActionPlayer 通过 actionPlayer 保持，播放进度不中断
  - 布局状态缓存通过 updateXxxLayoutState() 重新填充
```

反向切换（Separated -> Stacked）执行对称操作：KeyLayoutPanel 从 Zone A 迁移回 Zone B Row 3，GestureFeedbackPanel 双实例合并为单实例，GestureInputPanel 从中列宽度扩展到整行宽度，Zone A 容器被移除。

### 7.4 PlantUML 参考

```plantuml
@file:../diagrams/mvi-data-flow.puml
```

```plantuml
@file:../diagrams/app-config-persistence.puml
```
