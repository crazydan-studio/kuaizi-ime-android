# 输入动作播放设计

输入动作播放是基于坐标无关的动作脚本，自动以动画模拟滑行或点击等方式完成输入过程的 UI 层实现。核心思路是：将用户真实的手指操作抽象为可序列化的「动作脚本」（定义在 [engine/060-input-action.md](../engine/060-input-action.md)），由播放引擎按脚本驱动键盘状态机和 UI 动画，产生与真人操作一致的视觉效果。

**坐标无关设计**：动作脚本只记录按键的语义标识（如 `InputKey`），不存储任何绝对坐标。回放时，播放器通过 `InputActionPositionResolver` 根据当前键盘状态动态查找按键的归一化位置，从而消除按键布局变更、屏幕尺寸变化、手模式切换等因素导致的回放失效问题。同一份脚本可以在任意设备、任意布局下正确回放。

**两种使用模式**：Animation 模式用于演示/教学场景，访问真实字典数据但不提交到目标编辑器，显示完整的手指指示器动画；DirectInput 模式用于输入辅助场景，在完整输入流程上叠加动画效果，结果正常提交到编辑器，不显示 Row 1/2 的指示器。

**本文档范围**：播放状态模型（`InputActionPlayerState`）定义在本文档（`:ime-ui` 模块）。动画相关的其余数据模型（`InputActionFingerIndicator`、`InputActionPathInterpolator`、`InputActionPositionResolver` 接口）定义在 [engine/060-input-action.md](../engine/060-input-action.md)，此处不再重复定义。本文档覆盖 UI 层的实现：`KeyboardInputActionPlayerHost`、`ComposeInputActionPositionResolver`、`InputActionPlayer` 的使用方式和指示器内建机制。

```plantuml
@file:../diagrams/ui-input-action-data-flow.puml
```

```plantuml
@file:../diagrams/ui-input-action-class-relationship.puml
```

---

## 1. UseMode 使用模式

`KeyboardInputActionPlayerHost.UseMode` 定义输入动作播放的两种使用模式，决定了播放引擎与键盘交互的方式以及指示器的显示策略。

```kotlin
sealed class KeyboardInputActionPlayerHost {

    sealed class UseMode {
        /**
         * 动画播放模式。
         *
         * 不可中断的播放模式，访问真实字典数据但不提交到目标编辑器，
         * 不写入数据库。仅用于演示/教学场景。
         * 此模式下各面板的 showIndicator=true，显示手指指示器动画。
         */
        data object Animation : UseMode()

        /**
         * 直接输入模式。
         *
         * 封装 KeyboardHost 提供完整输入支持，
         * 在此基础上叠加播放引擎。
         * 此模式下 showIndicator=false，不显示 Row 1/2 的指示器，
         * 仅通过 GestureFeedbackPanel 的 FingerIndicator 显示手指位置。
         */
        data object DirectInput : UseMode()
    }
}
```

Animation 模式和 DirectInput 模式的核心差异在于：

- **数据提交**：Animation 模式下 ImeEngine 的状态机正常运转（访问真实字典），但 ImeOutput 不会被分发到目标编辑器；DirectInput 模式下所有输入结果正常提交。
- **指示器显示**：Animation 模式下 `showIndicator=true`，三行均显示 `InputActionFingerIndicator`——Row 1 的 CandidateListPanel 和 Row 2 的 InputListPanel/ToolListPanel 在内部绘制指示器，Row 3 通过 GestureFeedbackPanel 绘制；DirectInput 模式下 `showIndicator=false`，Row 1 和 Row 2 不显示指示器，仅 Row 3 通过 GestureFeedbackPanel 绘制。
- **中断性**：Animation 模式不可中断，播放过程中用户输入被忽略；DirectInput 模式下播放与用户输入共存。

| 属性 | Animation 模式 | DirectInput 模式 |
|------|---------------|-----------------|
| 用途 | 演示/教学 | 输入辅助 |
| 字典数据 | 访问真实字典 | 访问真实字典 |
| 编辑器提交 | 不提交 | 正常提交 |
| 数据库写入 | 不写入 | 正常写入 |
| Row 1 指示器 | showIndicator=true，内建绘制 | showIndicator=false |
| Row 2 指示器 | showIndicator=true，内建绘制 | showIndicator=false |
| Row 3 指示器 | 通过 GestureFeedbackPanel 绘制 | 通过 GestureFeedbackPanel 绘制 |
| 中断性 | 不可中断 | 与用户输入共存 |
| ImeOutput | 不分发 | 正常分发 |

---

## 2. InputActionPlayerState 播放状态（:ime-ui 模块）

| 属性 | 说明 |
|------|------|
| 角色 | 输入动作播放生命周期状态模型 |
| 职责 | 描述播放器的状态转换：Idle → Ready → Playing ↔ Paused → Finished |
| 约束 | 纯数据模型，不持有 UI 引用，不依赖 Compose；定义在 `:ime-ui` 模块 |
| 所属模块 | :ime-ui（原属 `:ime-engine`，已移至 `:ime-ui`） |

```kotlin
/**
 * 输入动作播放状态。
 *
 * 描述 InputActionPlayer 的播放生命周期状态。
 * 纯逻辑状态模型，不依赖 UI 层，定义在 :ime-ui 模块。
 *
 * 原名 InputActionPlaybackState，已重命名为 InputActionPlayerState，
 * 并从 :ime-engine 移至 :ime-ui 模块。
 */
sealed class InputActionPlayerState {
    /** 空闲状态，未加载脚本 */
    data object Idle : InputActionPlayerState()
    /** 就绪状态，已加载脚本但未开始播放 */
    data class Ready(val script: InputActionScript) : InputActionPlayerState()
    /** 播放中状态 */
    data class Playing(val currentIndex: Int, val totalActions: Int) : InputActionPlayerState()
    /** 暂停状态 */
    data class Paused(val currentIndex: Int, val totalActions: Int) : InputActionPlayerState()
    /** 播放完成状态 */
    data object Finished : InputActionPlayerState()
}
```

---

## 3. KeyboardInputActionPlayerHost

| 属性 | 说明 |
|------|------|
| 角色 | 输入动作播放演示集成组件 |
| 职责 | 支持 Animation 和 DirectInput 两种 UseMode，组合 KeyboardHost 和播放引擎 |
| 约束 | 仅用于演示/练习场景；Animation 模式访问真实字典数据但不提交到目标编辑器；DirectInput 模式不显示 Row 1/2 的指示器 |
| 关键属性 | useMode: UseMode, viewModel: KeyboardViewModel |
| 指示器控制 | Animation 模式：showIndicator=true，传递 InputActionFingerIndicator；DirectInput 模式：showIndicator=false |
| 所属包 | integration（注：输入练习 UI 层 ExerciseScreen 属于 `:app` 模块，InputActionPlayerPanel 属于 `:ime-ui` 模块） |

`KeyboardInputActionPlayerHost` 是输入动作播放的集成组件，内部组合 `KeyboardHost` 和播放引擎，通过面板内建的 `showIndicator` 参数控制指示器在 Row 1 和 Row 2 的显示，Row 3 的指示器则通过 GestureFeedbackPanel 绘制。三行均使用统一的 `InputActionFingerIndicator` 模型。

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
 *   不显示 Row 1/2 的指示器，仅通过 GestureFeedbackPanel 绘制手指指示器。
 *
 * 输入数据包括键盘输入模式 + 动作序列，针对不同输入对象（按键、输入列表、候选列表），
 * 但 UI 坐标无关。输入轨迹由 KeyLayoutPanel 的 KeyboardInputMode 决定，
 * KeyLayoutPanel 动态计算按键位置和轨迹形状。
 *
 * 对于 InputListPanel 和 CandidateListPanel 的交互，仅需选择操作：
 * 在面板上通过 InputActionFingerIndicator 绘制手指图形及点击动画（Animation 模式），
 * 点击坐标由面板的 locateItem() 方法动态计算。
 * 若目标项不在可视范围内，需先滚动到目标位置再定位。
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
            && playerState is InputActionPlayerState.Playing

    // 指示器状态（仅 Animation 模式下有意义）
    val row1Indicator = viewModel.actionPlayer.row1IndicatorState
    val row2Indicator = viewModel.actionPlayer.row2IndicatorState

    Box(modifier = modifier.fillMaxSize()) {
        // 基础键盘组件，通过参数传递指示器控制
        when (useMode) {
            is KeyboardInputActionPlayerHost.UseMode.Animation -> {
                // Animation 模式：showIndicator=true，传递 indicatorState
                KeyboardHostWithIndicators(
                    viewModel = viewModel,
                    showIndicators = showIndicators,
                    row1Indicator = row1Indicator,
                    row2Indicator = row2Indicator,
                )
            }
            is KeyboardInputActionPlayerHost.UseMode.DirectInput -> {
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
 * 面板组件在内部绘制 InputActionFingerIndicator，无需外部覆盖层。
 */
@Composable
private fun KeyboardHostWithIndicators(
    viewModel: KeyboardViewModel,
    showIndicators: Boolean,
    row1Indicator: InputActionFingerIndicator?,
    row2Indicator: InputActionFingerIndicator?,
) {
    // 内部使用修改后的 KeyboardHost 逻辑，
    // 在各面板调用处传递 showIndicator 和 indicatorState
    // Row 1: CandidateListPanel(showIndicator = showIndicators, indicatorState = row1Indicator)
    // Row 2: InputListPanel/ToolListPanel(showIndicator = showIndicators, indicatorState = row2Indicator)
    // Row 3: 指示器通过 GestureFeedbackPanel 绘制
    KeyboardHost(viewModel = viewModel)
}
```

---

## 4. ComposeInputActionPositionResolver

| 属性 | 说明 |
|------|------|
| 角色 | InputActionPositionResolver 的 Compose 布局实现 |
| 职责 | 将按键、候选项、输入项的语义标识解析为归一化坐标，供 InputActionPlayer 使用 |
| 约束 | 返回归一化坐标 [0,1]x[0,1]；布局状态通过惰性提供者获取，确保回放时实时查询 |
| 按键解析 | KeyLayoutState.keyPositions[key]?.center（已是归一化坐标） |
| 候选项解析 | CandidateListLayoutState.locateItem(index) + CoordinateNormalizer.normalize() |
| 输入项解析 | InputListLayoutState.locateItem(index) + CoordinateNormalizer.normalize() |
| 所属包 | panel |

`ComposeInputActionPositionResolver` 是 `InputActionPositionResolver` 接口（定义在 [engine/060-input-action.md](../engine/060-input-action.md)）的 Compose 布局实现，将语义按键标识符和列表索引解析为归一化坐标。该实现返回归一化坐标 [0,1]x[0,1]，由 GestureFeedbackPanel 根据面板尺寸反归一化后绘制。

### 3.1 坐标解析策略

对于不同类型的 UI 元素，解析器采用不同的坐标获取和归一化策略：

- **按键定位**（`resolve(key)`）：直接查询 `KeyLayoutState.keyPositions`，该映射已存储归一化坐标（`RectF`），取其 `center`（`OffsetF`）即可，无需额外归一化。
- **候选项定位**（`resolveCandidatePosition(index)`）：调用 `CandidateListLayoutState.locateItem(index)` 获取像素坐标，再通过 `CoordinateNormalizer.normalize()` 归一化到 Row 1 的面板坐标系。
- **输入项定位**（`resolveInputItemPosition(index)`）：调用 `InputListLayoutState.locateItem(index)` 获取像素坐标，再通过 `CoordinateNormalizer.normalize()` 归一化到 Row 2 的面板坐标系。

若目标项不在可视范围内，需先滚动到目标位置再定位。

### 3.2 完整实现

```kotlin
/**
 * 基于 Compose 布局的输入动作位置解析器实现。
 *
 * 返回归一化坐标 [0,1]x[0,1]。
 *
 * 三类布局状态提供者均为惰性求值：
 * - keyboardLayoutStateProvider: 查询 KeyLayoutState（归一化坐标已就绪）
 * - candidateLayoutStateProvider: 查询 CandidateListLayoutState（需归一化）
 * - inputListLayoutStateProvider: 查询 InputListLayoutState（需归一化）
 */
class ComposeInputActionPositionResolver(
    private val keyboardLayoutStateProvider: () -> KeyLayoutState?,
    private val candidateLayoutStateProvider: () -> CandidateListLayoutState?,
    private val inputListLayoutStateProvider: () -> InputListLayoutState?,
) : InputActionPositionResolver {

    /**
     * 查找指定按键的归一化中心坐标。
     *
     * KeyLayoutState.keyPositions 已存储归一化坐标 (RectF)，
     * 直接取 center 即可，无需额外归一化。
     */
    override fun resolve(key: InputKey): OffsetF? {
        val layout = keyboardLayoutStateProvider() ?: return null
        return layout.keyPositions[key]?.center
    }

    /**
     * 查找指定候选项的归一化中心坐标。
     *
     * CandidateListLayoutState.locateItem() 返回像素坐标，
     * 需要归一化到 Row 1 的面板坐标系。
     */
    override fun resolveCandidatePosition(index: Int): OffsetF? {
        val layout = candidateLayoutStateProvider() ?: return null
        val pixelOffset = layout.locateItem(index) ?: return null
        // 候选项坐标需要归一化到 Row 1 的坐标系
        return CoordinateNormalizer.normalize(pixelOffset, layout.panelSize)
    }

    /**
     * 查找指定输入项的归一化中心坐标。
     *
     * InputListLayoutState.locateItem() 返回像素坐标，
     * 需要归一化到 Row 2 的面板坐标系。
     */
    override fun resolveInputItemPosition(index: Int): OffsetF? {
        val layout = inputListLayoutStateProvider() ?: return null
        val pixelOffset = layout.locateItem(index) ?: return null
        // 输入项坐标需要归一化到 Row 2 的坐标系
        return CoordinateNormalizer.normalize(pixelOffset, layout.panelSize)
    }
}
```

---

## 5. InputActionPlayer

| 属性 | 说明 |
|------|------|
| 角色 | 输入动作播放引擎，坐标无关 |
| 职责 | 按 InputActionScript 时间轴执行 InputAction，驱动 GestureFeedbackState 和 KeyboardViewModel |
| 约束 | 坐标无关，所有位置通过 InputActionPositionResolver 实时查询归一化坐标 |
| 构造参数 | viewModel: KeyboardViewModel, feedbackState: GestureFeedbackState, positionResolver: InputActionPositionResolver, scope: CoroutineScope |
| 播放状态 | playbackState: StateFlow\<InputActionPlayerState\>（定义在本文档，:ime-ui 模块） |
| Row 1/2 指示器 | row1IndicatorState: MutableStateFlow\<InputActionFingerIndicator?\>, row2IndicatorState: MutableStateFlow\<InputActionFingerIndicator?\> |
| 路径插值 | 使用 InputActionPathInterpolator.interpolate()（定义在 engine/060） |
| 动作分发 | KeyDown → 设置手指指示器 + 启动点击涟漪动画 + 按键高亮 + 发送 PressKey；SwipeTo → 生成插值路径 + 动画移动手指 + 发送 PressKey；KeyUp → 更新手指状态 + 清除按键高亮；SelectCandidate → 更新 Row 1 指示器 + 发送 SelectCandidate；SwitchKeyboard → 发送 SwitchKeyboard |
| 所属包 | player |

`InputActionPlayer` 是输入动作播放引擎，接收坐标无关的 InputActionScript，按时间轴依次执行动作。

### 4.1 完整实现

```kotlin
/**
 * 输入动作播放器，坐标无关。
 *
 * 接收坐标无关的 InputActionScript，按时间轴依次执行动作：
 * - 将 InputAction 转换为 ImeIntent 通过 KeyboardViewModel 发送到引擎
 * - 通过 InputActionPositionResolver 在回放时动态解析归一化坐标
 * - 同步驱动动画覆盖层（手指指示器、触摸轨迹）
 * - 提供播放控制接口
 *
 * 坐标无关意味着：
 * - 同一脚本在不同屏幕尺寸的设备上均可正确回放
 * - 切换左右手模式后脚本仍有效（按键位置会动态重新解析）
 * - 键盘布局变更后脚本不会失效
 */
class InputActionPlayer(
    private val viewModel: KeyboardViewModel,
    private val feedbackState: GestureFeedbackState,
    private val positionResolver: InputActionPositionResolver,
    private val scope: CoroutineScope,
) {
    private var job: Job? = null
    private var _playbackState = MutableStateFlow<InputActionPlayerState>(InputActionPlayerState.Idle)
    val playbackState: StateFlow<InputActionPlayerState> = _playbackState.asStateFlow()

    private var _speed = MutableStateFlow(1.0f)
    val speed: StateFlow<Float> = _speed.asStateFlow()

    private var currentScript: InputActionScript? = null
    private var actionIndex = 0

    // Row 1 指示器状态（归一化坐标，行相对）
    private val _row1IndicatorState = MutableStateFlow<InputActionFingerIndicator?>(null)
    val row1IndicatorState: StateFlow<InputActionFingerIndicator?> = _row1IndicatorState.asStateFlow()

    // Row 2 指示器状态（归一化坐标，行相对）
    private val _row2IndicatorState = MutableStateFlow<InputActionFingerIndicator?>(null)
    val row2IndicatorState: StateFlow<InputActionFingerIndicator?> = _row2IndicatorState.asStateFlow()

    fun load(script: InputActionScript) {
        stop()
        currentScript = script
        actionIndex = 0
        _playbackState.value = InputActionPlayerState.Ready(script)
    }

    fun play() {
        val script = currentScript ?: return
        if (_playbackState.value is InputActionPlayerState.Playing) return

        _playbackState.value = InputActionPlayerState.Playing(
            currentIndex = actionIndex,
            totalActions = script.actions.size,
        )
        feedbackState.setFingerIndicator(InputActionFingerIndicator(
            position = OffsetF(0.5f, 0.5f), pressed = false, visible = true
        ))

        job = scope.launch {
            val startTime = System.currentTimeMillis()
            val actions = script.actions

            while (actionIndex < actions.size) {
                val action = actions[actionIndex]

                // 等待到动作的执行时间
                val elapsed = System.currentTimeMillis() - startTime
                val delayMs = (action.startTime / _speed.value) - elapsed
                if (delayMs > 0) delay(delayMs)

                // 检查是否暂停
                if (_playbackState.value is InputActionPlayerState.Paused) break

                // 执行动作
                executeAction(action)
                actionIndex++

                // 更新播放进度
                _playbackState.value = InputActionPlayerState.Playing(
                    currentIndex = actionIndex,
                    totalActions = actions.size,
                )
            }

            feedbackState.setFingerIndicator(null)
            _row1IndicatorState.value = null
            _row2IndicatorState.value = null
            _playbackState.value = InputActionPlayerState.Finished
        }
    }

    fun pause() {
        job?.cancel()
        val script = currentScript ?: return
        _playbackState.value = InputActionPlayerState.Paused(
            currentIndex = actionIndex,
            totalActions = script.actions.size,
        )
    }

    fun resume() {
        play()
    }

    fun stop() {
        job?.cancel()
        feedbackState.setFingerIndicator(null)
        feedbackState.clearTouchTrail()
        feedbackState.clearPressedKeys()
        _row1IndicatorState.value = null
        _row2IndicatorState.value = null
        actionIndex = 0
        _playbackState.value = InputActionPlayerState.Idle
    }

    fun stepForward() {
        val script = currentScript ?: return
        if (actionIndex >= script.actions.size) return

        executeAction(script.actions[actionIndex])
        actionIndex++
    }

    fun setSpeed(speed: Float) {
        _speed.value = speed.coerceIn(0.25f, 4.0f)
    }

    /**
     * 执行单个动作。
     *
     * 坐标无关的核心：每个动作执行时，通过 positionResolver
     * 查询按键的当前归一化位置，而非使用预存的坐标。
     */
    private fun executeAction(action: InputAction) {
        when (action) {
            is InputAction.KeyDown -> {
                val position = positionResolver.resolve(action.key) ?: return
                feedbackState.setFingerIndicator(InputActionFingerIndicator(
                    position = position, pressed = true, visible = true,
                    clickAnimation = ClickAnimation(progress = 0f),
                ))
                feedbackState.setPressedKeys(setOf(action.key))
                // 启动按键点击涟漪动画
                scope.launch {
                    animateClickAnimation(feedbackState, position)
                }
                viewModel.handleIntent(ImeIntent.PressKey(action.key, KeyGesture.Tap))
            }
            is InputAction.SwipeTo -> {
                val fromPosition = positionResolver.resolve(action.fromKey) ?: return
                val toPosition = positionResolver.resolve(action.toKey) ?: return

                // 使用 InputActionPathInterpolator 生成归一化坐标插值路径
                val normalizedPath = InputActionPathInterpolator.interpolate(
                    from = fromPosition,
                    to = toPosition,
                )
                // 将插值路径写入触摸轨迹
                feedbackState.setTouchTrailPoints(normalizedPath)

                // 沿路径动画移动手指
                scope.launch {
                    animateFingerAlongPath(feedbackState, normalizedPath, action.duration)
                }
                viewModel.handleIntent(ImeIntent.PressKey(action.toKey, KeyGesture.Swipe))
            }
            is InputAction.KeyUp -> {
                val position = positionResolver.resolve(action.key)
                val currentIndicator = feedbackState.fingerIndicator.value
                feedbackState.setFingerIndicator(InputActionFingerIndicator(
                    position = position ?: currentIndicator?.position ?: OffsetF(0.5f, 0.5f),
                    pressed = false,
                    visible = true,
                ))
                feedbackState.clearPressedKeys()
            }
            is InputAction.Wait -> {
                // 等待已由时间轴控制
            }
            is InputAction.SelectCandidate -> {
                val position = positionResolver.resolveCandidatePosition(action.candidateIndex) ?: return
                // 更新 Row 1 指示器
                _row1IndicatorState.value = InputActionFingerIndicator(
                    position = position, pressed = true, visible = true,
                    clickAnimation = ClickAnimation(progress = 0f),
                )
                viewModel.handleIntent(ImeIntent.SelectCandidate(/* candidate */))
                // 启动点击涟漪动画
                scope.launch {
                    animateRowIndicatorClick(_row1IndicatorState, position)
                }
                // 短暂显示后清除指示器
                scope.launch {
                    delay(300)
                    _row1IndicatorState.value = null
                }
            }
            is InputAction.SwitchKeyboard -> {
                viewModel.handleIntent(ImeIntent.SwitchKeyboard(action.targetType))
            }
        }
    }

    /**
     * 按键点击涟漪动画。
     *
     * 在按键位置绘制涟漪扩散动画，
     * 动画半径从 0 扩展到 maxRadius，透明度从 1 衰减到 0。
     * 通过更新 InputActionFingerIndicator 的 clickAnimation 字段驱动。
     */
    private suspend fun animateClickAnimation(
        feedbackState: GestureFeedbackState,
        position: OffsetF,
    ) {
        val animatable = Animatable(0f)
        animatable.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 300),
        ) {
            val currentIndicator = feedbackState.fingerIndicator.value
            if (currentIndicator != null && currentIndicator.position == position) {
                feedbackState.setFingerIndicator(
                    currentIndicator.copy(clickAnimation = ClickAnimation(progress = value))
                )
            }
        }
        // 动画结束后清除 clickAnimation
        val finalIndicator = feedbackState.fingerIndicator.value
        if (finalIndicator != null && finalIndicator.position == position) {
            feedbackState.setFingerIndicator(finalIndicator.copy(clickAnimation = null))
        }
    }

    /**
     * 面板内建指示器的点击涟漪动画。
     *
     * 在 Row 1/2 面板内建指示器位置绘制涟漪扩散动画，
     * 通过更新 InputActionFingerIndicator 的 clickAnimation 字段驱动。
     */
    private suspend fun animateRowIndicatorClick(
        indicatorState: MutableStateFlow<InputActionFingerIndicator?>,
        position: OffsetF,
    ) {
        val animatable = Animatable(0f)
        animatable.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 300),
        ) {
            val current = indicatorState.value
            if (current != null && current.position == position) {
                indicatorState.value = current.copy(clickAnimation = ClickAnimation(progress = value))
            }
        }
        val final = indicatorState.value
        if (final != null && final.position == position) {
            indicatorState.value = final.copy(clickAnimation = null)
        }
    }

    /**
     * 沿路径动画移动手指指示器（通过 GestureFeedbackState.fingerIndicator）。
     *
     * 路径点均为归一化坐标，直接写入 InputActionFingerIndicator。
     */
    private suspend fun animateFingerAlongPath(
        feedbackState: GestureFeedbackState,
        path: List<OffsetF>,
        durationMs: Long,
    ) {
        if (path.size < 2) return
        val stepDuration = durationMs / (path.size - 1)
        val current = feedbackState.fingerIndicator.value
        for (i in 1 until path.size) {
            val animatable = Animatable(0f)
            animatable.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = stepDuration.toInt()),
            ) {
                val from = path[i - 1]
                val to = path[i]
                feedbackState.setFingerIndicator(InputActionFingerIndicator(
                    position = OffsetF(
                        x = from.x + (to.x - from.x) * value,
                        y = from.y + (to.y - from.y) * value,
                    ),
                    pressed = current?.pressed ?: false,
                    visible = true,
                ))
            }
        }
    }
}
```

---

## 6. 指示器内建机制

Zone B 三行结构中，每行在播放动画时需要展示指示器。本设计将指示器从独立覆盖层改为内建到面板组件中，通过 `showIndicator` 布尔参数和 `indicatorState` 状态参数控制。这种内建设计消除了独立的覆盖层组件，简化了组件层次，同时使指示器的坐标与面板内容使用同一坐标系，避免了跨组件坐标对齐问题。三行均使用统一的 `InputActionFingerIndicator` 模型，职能相同：绘制代表手指的图形并跟随滑行轨迹移动，以及手指的点击动画。

### 5.1 各行指示器绘制方式

| 行 | 面板 | 指示器方式 | 参数 |
|---|---|---|---|
| Row 1 | CandidateListPanel | 面板内建 Canvas 绘制 | showIndicator: Boolean, indicatorState: InputActionFingerIndicator? |
| Row 2 | InputListPanel / ToolListPanel | 面板内建 Canvas 绘制 | showIndicator: Boolean, indicatorState: InputActionFingerIndicator? |
| Row 3 | GestureFeedbackPanel | FingerIndicator 元素绘制 | 无需额外参数，通过 GestureFeedbackState.fingerIndicator 驱动 |

### 5.2 Animation 模式与 DirectInput 模式的指示器差异

在 Animation 模式下，`KeyboardInputActionPlayerHost` 将 `showIndicator=true` 传递给 Row 1 和 Row 2 的面板，并将 InputActionPlayer 计算的指示器状态通过 `indicatorState` 参数传入。面板在常规内容之上叠加绘制手指指示器图形。在 DirectInput 模式下，`showIndicator=false`，面板不绘制指示器，仅通过 GestureFeedbackPanel 显示手指位置。

这种模式差异使得 Animation 模式提供完整的可视化演示效果，而 DirectInput 模式仅保留必要的手指位置提示，避免在实际输入辅助场景中过度干扰用户操作。

### 5.3 面板内建指示器绘制逻辑

以 CandidateListPanel 为例，所有支持指示器的面板采用相同的内建绘制模式：

```kotlin
/**
 * 候选列表面板（指示器内建部分）。
 *
 * 当 showIndicator=true 且 indicatorState 非空时，
 * 面板在常规内容之上叠加绘制一个半透明圆形指示器。
 * 指示器的 position 为归一化坐标（行相对），
 * 绘制时根据面板实际尺寸反归一化为像素坐标。
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
        // ... 常规候选列表内容 ...

        // 内建指示器
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

InputListPanel 和 ToolListPanel 采用完全相同的内建指示器绘制模式，此处不再重复。

---

## 7. 播放执行流程

输入动作播放的执行流程如下。播放器加载 InputActionScript 后，按时间轴依次执行 InputAction。对于不同类型的动作，播放器通过 `InputActionPositionResolver` 解析归一化坐标，通过 `InputActionPathInterpolator` 生成插值轨迹，更新 `GestureFeedbackState` 的手指指示器和触摸轨迹，同时管理各行的指示器状态。

```plantuml
@file:../diagrams/ui-input-action-data-flow.puml
```

### 6.1 关键流程说明

**KeyDown 执行**：播放器通过 `resolve(key)` 获取按键的归一化中心坐标，设置 FingerIndicator 为按下状态并显示，同时启动按键点击涟漪动画（通过 `clickAnimation` 字段驱动 GestureFeedbackPanel 绘制涟漪扩散效果），并将按键加入 pressedKeys 集合触发按键高亮。GestureFeedbackPanel 读取归一化坐标后根据面板尺寸反归一化绘制。ViewModel 发送 PressKey 意图驱动引擎状态转换。

**SwipeTo 执行**：播放器解析起止按键的归一化坐标，通过 `InputActionPathInterpolator.interpolate()` 生成二次贝塞尔曲线插值路径（归一化坐标点列表），将完整路径写入 `touchTrailPoints`，同时沿路径动画移动 FingerIndicator。GestureFeedbackPanel 在 Zone A 和 Zone B 各自根据面板尺寸反归一化后绘制轨迹。

**SelectCandidate 执行**：播放器通过 `resolveCandidatePosition(index)` 获取候选项的归一化坐标（行相对），更新 `row1IndicatorState` 为 `InputActionFingerIndicator`。CandidateListPanel 在内部检测到 `showIndicator=true` 且 `indicatorState` 非空后，根据面板尺寸反归一化绘制圆形指示器。短暂延时后清除指示器。

**各行的指示器绘制**：三行均使用 `InputActionFingerIndicator` 模型，职能相同——绘制代表手指的图形并跟随滑行轨迹移动，以及手指的点击动画。Row 1 和 Row 2 的点击类操作（SelectCandidate、SelectInputItem）通过面板内建的 `indicatorState` 参数绘制；Row 3 的按键操作（KeyDown、SwipeTo、KeyUp）通过 GestureFeedbackPanel 绘制。两者均使用归一化坐标，但坐标系不同：Row 1/2 使用行相对坐标系，Row 3 使用 KeyLayoutPanel 的归一化坐标系。

---

## 8. InputActionScriptLoader

| 属性 | 说明 |
|------|------|
| 角色 | 动作脚本文件加载器 |
| 职责 | 从 assets 或外部 URI 加载 JSON 格式的 InputActionScript |
| 约束 | 仅负责加载和反序列化，不执行脚本 |
| 关键方法 | loadPreset(name), listPresets(), loadFromFile(uri) |
| 所属包 | player |

`InputActionScriptLoader` 负责从文件系统加载动作脚本。

```kotlin
/**
 * 动作脚本加载器。
 *
 * 从 assets 或外部文件加载 InputActionScript，
 * 使用 Kotlin Serialization 解析 JSON 格式的脚本。
 * 脚本格式定义在 [engine/060-input-action.md](../engine/060-input-action.md)。
 */
class InputActionScriptLoader(private val context: Context) {

    /** 从 assets 加载预置脚本 */
    fun loadPreset(name: String): InputActionScript {
        val json = context.assets.open("scripts/$name.json")
            .bufferedReader().use { it.readText() }
        return parseScript(json)
    }

    /** 列出所有预置脚本 */
    fun listPresets(): List<String> {
        return context.assets.list("scripts")?.toList() ?: emptyList()
    }

    /** 从外部文件加载脚本 */
    fun loadFromFile(uri: Uri): InputActionScript {
        val json = context.contentResolver.openInputStream(uri)
            ?.bufferedReader()?.use { it.readText() }
            ?: error("无法读取脚本文件")
        return parseScript(json)
    }

    private fun parseScript(json: String): InputActionScript {
        // 使用 Kotlin Serialization 解析 JSON
        return Json.decodeFromString<InputActionScript>(json)
    }
}
```

---

## 9. 数据流

以下展示程序化输入的完整数据流，标注归一化坐标的转换节点。

### 9.1 程序化输入数据流

```plantuml
@file:../diagrams/ui-programmatic-input-data-flow.puml
```

### 9.2 归一化坐标流详解

程序化输入中归一化坐标的流转路径如下：

1. **解析阶段**：`InputActionPositionResolver` 从布局状态中查询位置。按键位置直接取 `KeyLayoutState.keyPositions` 的归一化值；候选项和输入项通过 `locateItem()` 获取像素坐标后经 `CoordinateNormalizer.normalize()` 归一化。
2. **插值阶段**：`InputActionPathInterpolator.interpolate()` 接收归一化起止坐标，输出归一化坐标路径点列表。
3. **写入阶段**：归一化坐标写入 `GestureFeedbackState`（FingerIndicator、touchTrailPoints）或 Row 1/2 的 `InputActionFingerIndicator`（indicatorState）。
4. **绘制阶段**：GestureFeedbackPanel 读取归一化坐标后根据面板尺寸反归一化绘制；面板内建指示器读取行相对归一化坐标后根据面板尺寸反归一化绘制。

### 9.3 与用户手势输入数据流的关系

程序化输入与用户手势输入共享同一套 `GestureFeedbackState`，但写入来源不同：

- **用户手势输入**：`GestureInputPanel` 接收触摸事件，归一化后写入 `touchTrailPoints`，通过 `KeyLayoutState.findKeyAt()` 识别按键后输出 `InputGesture`。
- **程序化输入**：`InputActionPlayer` 通过 `InputActionPositionResolver` 解析归一化坐标，直接写入 `fingerIndicator` 和 `touchTrailPoints`，通过 `KeyboardViewModel.handleIntent()` 发送 `ImeIntent`。

两者最终都通过 `GestureFeedbackPanel` 绘制，归一化坐标机制确保同一份数据在不同 Zone 和不同尺寸的面板实例上均可正确渲染。
