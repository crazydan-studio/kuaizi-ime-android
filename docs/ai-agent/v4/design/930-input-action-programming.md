# 930 — 输入动作程序化设计

## 1. 概述

v4 版本新增输入动作程序化能力，即通过指定待输入的字符序列，自动以动画模拟滑行或点击等方式完成输入过程。该功能是 **release 构建即提供的用户级能力**，主要用于输入练习演示（如教学引导、操作展示），后续版本将基于此能力提供完整的输入练习功能。核心思路是：将用户真实的手指操作抽象为可序列化的「动作脚本」，由播放引擎按脚本驱动键盘状态机和 UI 动画，产生与真人操作一致的视觉效果。

**坐标无关设计**：动作脚本只记录按键的语义标识（如 `InputKey`），不存储任何绝对坐标。回放时，播放器根据当前键盘状态动态查找按键的实时位置，从而消除按键布局变更、屏幕尺寸变化、手模式切换等因素导致的回放失效问题。这意味着同一份脚本可以在任意设备、任意布局下正确回放。

---

## 2. 需求分析

### 2.1 使用场景

| 场景 | 说明 | 输入方式 | 构建可用 |
|------|------|----------|----------|
| 输入练习演示 | 展示如何通过滑行/点击输入指定文字，用于教学引导 | 滑行/点击动画 | release ✅ |
| 新手引导 | 首次使用时演示滑行输入「你好」的完整过程 | 滑行动画 | release ✅ |
| 功能展示 | 在应用商店截图/录屏中展示 X-Pad 连续输入 | 滑行 + X-Pad 动画 | release ✅ |
| 键盘布局教学 | 展示特定拼音的滑行路径（如输入「zhuang」的完整路径） | 滑行动画 | release ✅ |
| 候选选择演示 | 展示如何选择候选字、翻页查看更多候选 | 点击动画 | release ✅ |
| 操作回放 | 记录并回放用户的真实操作路径 | 逻辑回放 | release ✅ |
| 自动化测试 | 按脚本自动操作键盘，验证状态机转换和输出正确性 | 即时执行（无动画） | test ✅ |
| 操作录制 | 录制用户真实操作生成脚本 | 开发者工具 | debug ✅ |

### 2.2 核心需求

| 需求 | 说明 |
|------|------|
| 字符序列转动作脚本 | 输入「你好」→ 自动生成滑行/点击的动作序列 |
| 动画模拟 | 在键盘 UI 上以动画形式展示手指移动、按键按下、滑行轨迹等 |
| 坐标无关 | 脚本不包含绝对坐标，回放时根据当前布局动态解析按键位置 |
| 播放控制 | 支持播放、暂停、步进、调速 |
| 多种输入方式 | 同一字符序列可选择滑行、点击、X-Pad 等不同输入方式演示 |
| 脚本序列化 | 动作脚本可导出/导入，便于预置演示脚本 |
| Release 可用 | 动画播放引擎在 release 构建中可用，用于输入练习演示 |
| 开发工具隔离 | 操作录制等开发工具仅在 debug 构建中可用 |

---

## 3. 架构设计

### 3.1 整体架构

```
┌────────────────────────────────────────────────────────────────┐
│                    调用层                                       │
│  引导页面 / 功能演示 / 自动化测试                                │
└──────────────────────┬─────────────────────────────────────────┘
                       │
                       ▼
┌────────────────────────────────────────────────────────────────┐
│              InputActionPlayer (播放器)                         │
│  - 脚本加载                                                     │
│  - 播放控制（播放/暂停/步进/调速）                                │
│  - 坐标解析：根据 ImeState 动态查找按键位置                       │
│  - 与 ViewModel 集成                                            │
└──────────┬───────────────────────────┬─────────────────────────┘
           │                           │
           ▼                           ▼
┌──────────────────────┐    ┌────────────────────────┐
│  ActionScriptCompiler│    │  ActionAnimator        │
│  (脚本编译器)         │    │  (动画引擎)             │
│  字符序列 → 逻辑动作   │    │  逻辑动作+坐标 → 动画   │
│  （坐标无关）          │    │                        │
└──────────┬───────────┘    └───────────┬────────────┘
           │                            │
           ▼                            ▼
┌──────────────────────┐    ┌────────────────────────┐
│  InputAction         │    │  FingerOverlay         │
│  (逻辑动作定义)       │    │  (手指指示器)           │
│  Sealed class        │    │  Canvas + Animatable   │
│  无绝对坐标           │    │                        │
└──────────────────────┘    └────────────────────────┘
```

### 3.2 数据模型

```kotlin
/**
 * 输入动作，不可变，坐标无关。
 *
 * 每个动作描述一次原子操作（按下、滑行到、抬起等），
 * 仅包含按键的语义标识和时间信息，不包含绝对坐标。
 * 坐标在回放时由 InputActionPlayer 根据当前键盘布局动态解析。
 */
sealed class InputAction {
    /** 动作开始时刻（毫秒），相对于脚本起始 */
    abstract val startTime: Long

    /** 按下按键 */
    data class KeyDown(
        override val startTime: Long,
        val key: InputKey,
    ) : InputAction()

    /**
     * 滑行到按键（手指从当前位置移动到目标按键）。
     *
     * 不存储路径点坐标。回放时，播放器根据 fromKey 和 toKey
     * 的实时坐标计算贝塞尔曲线路径，并驱动手指指示器沿路径移动。
     */
    data class SwipeTo(
        override val startTime: Long,
        val fromKey: InputKey,
        val toKey: InputKey,
        val duration: Long, // 滑行持续时间（毫秒）
    ) : InputAction()

    /** 抬起手指 */
    data class KeyUp(
        override val startTime: Long,
        val key: InputKey,
    ) : InputAction()

    /** 等待（用于添加间隔） */
    data class Wait(
        override val startTime: Long,
        val duration: Long,
    ) : InputAction()

    /**
     * 选择候选项。
     *
     * 不存储坐标。回放时，播放器根据当前候选栏布局
     * 查找第 candidateIndex 个候选项的位置。
     */
    data class SelectCandidate(
        override val startTime: Long,
        val candidateIndex: Int,
    ) : InputAction()

    /** 切换键盘 */
    data class SwitchKeyboard(
        override val startTime: Long,
        val targetType: KeyboardType,
    ) : InputAction()
}

/**
 * 输入动作脚本，不可变，坐标无关。
 * 由一组有序的逻辑动作组成，附带元信息。
 */
data class ActionScript(
    val name: String,
    val description: String,
    val inputMethod: InputMethod,
    val actions: List<InputAction>,
    val totalDuration: Long,
)

/**
 * 输入方式，决定脚本编译器如何将字符序列转为动作序列。
 */
enum class InputMethod {
    /** 逐键点击：每个字符单独点击 */
    Tap,
    /** 滑行输入：在按键间滑行，自动识别声母韵母 */
    Swipe,
    /** X-Pad 输入：通过六边形面板连续滑行 */
    XPad,
}
```

### 3.3 坐标解析器

回放时根据当前键盘状态动态解析按键位置，是坐标无关设计的核心组件：

```kotlin
/**
 * 按键位置解析器。
 *
 * 根据当前键盘状态（键盘类型、布局、手模式、屏幕尺寸等）
 * 动态查找按键的中心坐标。由于键盘布局可能因配置变更而改变，
 * 解析器总是在回放时实时查询，而非缓存编译期坐标。
 */
interface KeyPositionResolver {
    /**
     * 查找指定按键在当前键盘中的中心坐标。
     *
     * @param key 待查找的按键
     * @return 按键中心坐标，若按键在当前键盘中不存在则返回 null
     */
    fun resolve(key: InputKey): Offset?

    /**
     * 查找指定候选项在当前候选栏中的中心坐标。
     *
     * @param index 候选项索引
     * @return 候选项中心坐标，若索引越界则返回 null
     */
    fun resolveCandidatePosition(index: Int): Offset?
}

/**
 * 基于 Compose 布局的按键位置解析器实现。
 *
 * 通过 Compose 的布局系统（onGloballyPositioned / layoutInfo）
 * 获取各按键和候选项的实时位置，确保与屏幕上实际渲染位置一致。
 */
class ComposeKeyPositionResolver(
    private val keyboardLayoutProvider: () -> KeyboardLayoutInfo?,
    private val candidateLayoutProvider: () -> CandidateLayoutInfo?,
) : KeyPositionResolver {

    override fun resolve(key: InputKey): Offset? {
        val layout = keyboardLayoutProvider() ?: return null
        return layout.keyPositions[key]?.center
    }

    override fun resolveCandidatePosition(index: Int): Offset? {
        val layout = candidateLayoutProvider() ?: return null
        return layout.candidatePositions.getOrNull(index)?.center
    }
}

/**
 * 键盘布局信息，由 Compose 布局系统在每次重组后更新。
 * 包含当前键盘中所有按键的位置矩形。
 */
data class KeyboardLayoutInfo(
    val keyPositions: Map<InputKey, Rect>,
)

/**
 * 候选栏布局信息，由 Compose 布局系统在每次重组后更新。
 * 包含当前可见候选项的位置矩形。
 */
data class CandidateLayoutInfo(
    val candidatePositions: List<Rect>,
)
```

### 3.4 动作脚本编译器

将待输入字符序列编译为逻辑动作序列。编译器仅需要知道键盘的按键表（哪些键存在），不需要知道按键的像素位置：

```kotlin
/**
 * 动作脚本编译器，坐标无关。
 *
 * 根据待输入文本和输入方式，生成逻辑动作序列。
 * 编译过程只查询键盘的按键表（哪些键存在），不涉及坐标计算。
 * 坐标在回放时由播放器通过 KeyPositionResolver 动态解析。
 */
class ActionScriptCompiler(
    private val keyTableProvider: (KeyboardType) -> List<List<InputKey>>,
) {

    /**
     * 将文本编译为动作脚本。
     *
     * @param text 待输入文本，如 "你好世界"
     * @param method 输入方式
     * @param speed 播放速度倍率，1.0 为正常速度
     */
    fun compile(
        text: String,
        method: InputMethod = InputMethod.Swipe,
        speed: Float = 1.0f,
    ): ActionScript {
        val actions = mutableListOf<InputAction>()
        var currentTime = 0L

        when (method) {
            InputMethod.Tap -> compileTapActions(text, actions) { currentTime = it }
            InputMethod.Swipe -> compileSwipeActions(text, actions) { currentTime = it }
            InputMethod.XPad -> compileXPadActions(text, actions) { currentTime = it }
        }

        // 按速度调整时间轴
        val adjustedActions = actions.map { action ->
            adjustActionTime(action, speed)
        }

        return ActionScript(
            name = "输入: $text",
            description = "以${method.displayName}方式输入「$text」",
            inputMethod = method,
            actions = adjustedActions,
            totalDuration = adjustedActions.maxOfOrNull { it.startTime } ?: 0L,
        )
    }

    /**
     * 编译点击方式输入的动作序列。
     *
     * 每个字符：KeyDown → 短暂等待 → KeyUp → 下一个字符
     */
    private fun compileTapActions(
        text: String,
        actions: MutableList<InputAction>,
        time: (Long) -> Unit,
    ) {
        val pinyinSegments = textToPinyinSegments(text)
        val pinyinKeyTable = keyTableProvider(KeyboardType.Pinyin)

        for (segment in pinyinSegments) {
            val chars = segment.spell.toCharArray()

            for (char in chars) {
                val key = findCharKey(pinyinKeyTable, char)
                    ?: continue

                val downTime = actions.lastOrNull()?.startTime?.plus(200) ?: 0L
                actions += InputAction.KeyDown(downTime, key)
                actions += InputAction.KeyUp(downTime + 80, key)

                // 输入最后一个字符后等待候选
                if (char == chars.last()) {
                    actions += InputAction.Wait(downTime + 80, 500)
                    // 选择第一个候选
                    actions += InputAction.SelectCandidate(downTime + 580, 0)
                }
            }
        }
    }

    /**
     * 编译滑行方式输入的动作序列。
     *
     * 每个拼音段：KeyDown（起始键）→ SwipeTo（中间键）→ ... → KeyUp → 选择候选
     * 滑行路径在回放时由播放器根据实时坐标动态计算。
     */
    private fun compileSwipeActions(
        text: String,
        actions: MutableList<InputAction>,
        time: (Long) -> Unit,
    ) {
        val pinyinSegments = textToPinyinSegments(text)
        val pinyinKeyTable = keyTableProvider(KeyboardType.Pinyin)

        for ((index, segment) in pinyinSegments.withIndex()) {
            val chars = segment.spell.toCharArray()
            var currentTime = actions.lastOrNull()?.startTime?.plus(300) ?: 0L

            // 第一个字符：按下
            val firstKey = findCharKey(pinyinKeyTable, chars.first()) ?: continue
            actions += InputAction.KeyDown(currentTime, firstKey)

            // 后续字符：滑行到
            for (i in 1 until chars.size) {
                val prevKey = findCharKey(pinyinKeyTable, chars[i - 1]) ?: continue
                val nextKey = findCharKey(pinyinKeyTable, chars[i]) ?: continue

                val swipeDuration = 150L * i // 逐步加速
                currentTime += swipeDuration
                actions += InputAction.SwipeTo(
                    startTime = currentTime,
                    fromKey = prevKey,
                    toKey = nextKey,
                    duration = swipeDuration,
                )
            }

            // 抬起
            val lastKey = findCharKey(pinyinKeyTable, chars.last()) ?: continue
            currentTime += 80
            actions += InputAction.KeyUp(currentTime, lastKey)

            // 等待候选
            currentTime += 400
            actions += InputAction.Wait(currentTime, 400)

            // 选择候选
            currentTime += 400
            actions += InputAction.SelectCandidate(currentTime, 0)

            // 段间间隔
            if (index < pinyinSegments.size - 1) {
                currentTime += 300
                actions += InputAction.Wait(currentTime, 300)
            }
        }
    }

    /**
     * 编译 X-Pad 方式输入的动作序列。
     *
     * X-Pad 模式下，手指在六边形区域间滑行，
     * 每经过一个区域即输入对应的拼音字符。
     */
    private fun compileXPadActions(
        text: String,
        actions: MutableList<InputAction>,
        time: (Long) -> Unit,
    ) {
        val pinyinSegments = textToPinyinSegments(text)

        for (segment in pinyinSegments) {
            // X-Pad 输入：从中心开始，滑行经过目标区域
            // 具体区域映射取决于 X-Pad 布局，编译为逻辑动作
            val zoneSequence = resolveXPadZones(segment.spell)
            // ... 编译为滑行动作（坐标无关，仅引用 InputKey）
        }
    }

    /** 文本转拼音段（简化实现，实际需要调用拼音引擎） */
    private fun textToPinyinSegments(text: String): List<PinyinSegment> {
        return PinyinTextConverter.convert(text)
    }
}

data class PinyinSegment(
    val text: String,   // 目标汉字
    val spell: String,  // 拼音
)

val InputMethod.displayName: String
    get() = when (this) {
        InputMethod.Tap -> "点击"
        InputMethod.Swipe -> "滑行"
        InputMethod.XPad -> "X-Pad"
    }
```

---

## 4. 动画引擎

### 4.1 贝塞尔曲线路径生成

回放时，播放器根据 `fromKey` 和 `toKey` 的实时坐标，动态计算贝塞尔曲线路径，用于手指指示器和滑行轨迹动画：

```kotlin
/**
 * 滑行路径生成器，坐标无关。
 *
 * 根据两个按键的实时坐标，生成贝塞尔曲线插值路径点。
 * 路径在每次滑行动作执行时重新计算，确保与当前布局一致。
 */
object SwipePathInterpolator {
    /**
     * 在两个坐标之间生成二次贝塞尔曲线路径。
     *
     * @param from 起始坐标
     * @param to 目标坐标
     * @param arcFactor 弧度系数，控制曲线弯曲程度。正值向上弯，负值向下弯
     * @param steps 插值步数
     */
    fun interpolate(
        from: Offset,
        to: Offset,
        arcFactor: Float = -20f,
        steps: Int = 20,
    ): List<Offset> {
        val points = mutableListOf<Offset>()
        // 控制点在 from 和 to 的中点上方
        val controlPoint = Offset(
            (from.x + to.x) / 2,
            (from.y + to.y) / 2 + arcFactor,
        )

        for (i in 0..steps) {
            val t = i.toFloat() / steps
            val x = (1 - t) * (1 - t) * from.x + 2 * (1 - t) * t * controlPoint.x + t * t * to.x
            val y = (1 - t) * (1 - t) * from.y + 2 * (1 - t) * t * controlPoint.y + t * t * to.y
            points += Offset(x, y)
        }
        return points
    }
}
```

### 4.2 手指指示器

在键盘上方叠加一个虚拟手指指示器，跟随动作位置移动：

```kotlin
/**
 * 手指指示器覆盖层。
 *
 * 在键盘上绘制一个半透明的圆形手指指示器，
 * 跟随播放器解析出的实时坐标移动，模拟用户手指操作。
 * release 构建中可用，用于输入练习演示。
 */
@Composable
fun FingerOverlay(
    state: FingerOverlayState,
    modifier: Modifier = Modifier,
) {
    val fingerPosition by state.position.collectAsState()
    val fingerVisible by state.visible.collectAsState()
    val fingerPressed by state.pressed.collectAsState()

    if (!fingerVisible) return

    val scale by animateFloatAsState(
        targetValue = if (fingerPressed) 0.8f else 1.0f,
        animationSpec = tween(100),
        label = "fingerScale",
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val center = fingerPosition
        val radius = 24.dp.toPx()

        // 手指阴影
        drawCircle(
            color = Color.Black.copy(alpha = 0.2f),
            radius = radius * scale + 4.dp.toPx(),
            center = Offset(center.x, center.y + 2.dp.toPx()),
        )

        // 手指圆圈
        drawCircle(
            color = if (fingerPressed) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
            } else {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
            },
            radius = radius * scale,
            center = center,
        )

        // 中心点
        drawCircle(
            color = Color.White.copy(alpha = 0.8f),
            radius = 4.dp.toPx(),
            center = center,
        )
    }
}

class FingerOverlayState {
    private val _position = MutableStateFlow(Offset.Zero)
    val position: StateFlow<Offset> = _position.asStateFlow()

    private val _visible = MutableStateFlow(false)
    val visible: StateFlow<Boolean> = _visible.asStateFlow()

    private val _pressed = MutableStateFlow(false)
    val pressed: StateFlow<Boolean> = _pressed.asStateFlow()

    fun moveTo(offset: Offset) {
        _position.value = offset
    }

    /**
     * 沿路径动画移动手指指示器。
     *
     * @param path 贝塞尔曲线插值路径点（由 SwipePathInterpolator 生成）
     * @param durationMs 动画持续时间
     */
    suspend fun animateAlongPath(path: List<Offset>, durationMs: Long) {
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
                _position.value = Offset(
                    x = from.x + (to.x - from.x) * value,
                    y = from.y + (to.y - from.y) * value,
                )
            }
        }
    }

    fun show() { _visible.value = true }
    fun hide() { _visible.value = false }
    fun pressDown() { _pressed.value = true }
    fun pressUp() { _pressed.value = false }
}
```

### 4.3 滑行轨迹动画

在滑行动作播放时，实时绘制手指移动轨迹。轨迹点由播放器根据实时坐标动态生成：

```kotlin
@Composable
fun SwipeTrailOverlay(
    trailPoints: List<Offset>,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        if (trailPoints.size < 2) return@Canvas

        val path = Path().apply {
            moveTo(trailPoints.first())
            for (i in 1 until trailPoints.size) {
                quadraticBezierTo(
                    trailPoints[i - 1],
                    midpoint(trailPoints[i - 1], trailPoints[i]),
                )
            }
        }

        drawPath(
            path = path,
            color = Color(0xFF2196F3).copy(alpha = 0.6f),
            style = Stroke(
                width = 4.dp.toPx(),
                cap = StrokeCap.Round,
                pathEffect = null,
            ),
        )
    }
}
```

### 4.4 按键高亮动画

模拟按下按键时的视觉反馈：

```kotlin
@Composable
fun KeyHighlightOverlay(
    highlightedKeys: Set<InputKey>,
    keyPositionResolver: KeyPositionResolver,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        highlightedKeys.forEach { key ->
            val center = keyPositionResolver.resolve(key) ?: return@forEach
            val radius = 24.dp.toPx()
            drawCircle(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                radius = radius,
                center = center,
            )
        }
    }
}
```

---

## 5. 播放器

### 5.1 InputActionPlayer

```kotlin
/**
 * 输入动作播放器，坐标无关。
 *
 * 接收坐标无关的 ActionScript，按时间轴依次执行动作：
 * - 将 InputAction 转换为 ImeIntent 发送到 ViewModel
 * - 通过 KeyPositionResolver 在回放时动态解析按键坐标
 * - 同步驱动动画覆盖层（手指指示器、轨迹、高亮）
 * - 提供播放控制接口
 * release 构建中可用，用于输入练习演示。
 *
 * 坐标无关意味着：
 * - 同一脚本在不同屏幕尺寸的设备上均可正确回放
 * - 切换左右手模式后脚本仍有效（按键位置会动态重新解析）
 * - 键盘布局变更后脚本不会失效
 */
class InputActionPlayer(
    private val viewModel: ImeViewModel,
    private val fingerOverlay: FingerOverlayState,
    private val positionResolver: KeyPositionResolver,
    private val scope: CoroutineScope,
) {
    private var job: Job? = null
    private var _playbackState = MutableStateFlow(PlaybackState.Idle)
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private var _speed = MutableStateFlow(1.0f)
    val speed: StateFlow<Float> = _speed.asStateFlow()

    private var currentScript: ActionScript? = null
    private var actionIndex = 0

    // 滑行轨迹点，由播放器在执行 SwipeTo 时根据实时坐标生成
    private val _trailPoints = MutableStateFlow<List<Offset>>(emptyList())
    val trailPoints: StateFlow<List<Offset>> = _trailPoints.asStateFlow()

    fun load(script: ActionScript) {
        stop()
        currentScript = script
        actionIndex = 0
        _playbackState.value = PlaybackState.Ready(script)
    }

    fun play() {
        val script = currentScript ?: return
        if (_playbackState.value is PlaybackState.Playing) return

        _playbackState.value = PlaybackState.Playing(script)
        fingerOverlay.show()

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
                if (_playbackState.value is PlaybackState.Paused) break

                // 执行动作
                executeAction(action)
                actionIndex++
            }

            fingerOverlay.hide()
            _playbackState.value = PlaybackState.Finished(script)
        }
    }

    fun pause() {
        job?.cancel()
        _playbackState.value = PlaybackState.Paused(currentScript!!)
    }

    fun resume() {
        play()
    }

    fun stop() {
        job?.cancel()
        fingerOverlay.hide()
        _trailPoints.value = emptyList()
        actionIndex = 0
        _playbackState.value = PlaybackState.Idle
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
     * 查询按键的当前实时位置，而非使用预存的坐标。
     */
    private fun executeAction(action: InputAction) {
        when (action) {
            is InputAction.KeyDown -> {
                val position = positionResolver.resolve(action.key) ?: return
                fingerOverlay.moveTo(position)
                fingerOverlay.pressDown()
                viewModel.handleIntent(ImeIntent.KeyPressed(action.key, KeyGesture.Tap))
            }
            is InputAction.SwipeTo -> {
                val fromPosition = positionResolver.resolve(action.fromKey) ?: return
                val toPosition = positionResolver.resolve(action.toKey) ?: return

                // 根据实时坐标动态计算滑行路径
                val path = SwipePathInterpolator.interpolate(fromPosition, toPosition)
                _trailPoints.value = path

                // 沿路径动画移动手指
                scope.launch {
                    fingerOverlay.animateAlongPath(path, action.duration)
                }
                viewModel.handleIntent(ImeIntent.KeyPressed(action.toKey, KeyGesture.Swipe))
            }
            is InputAction.KeyUp -> {
                val position = positionResolver.resolve(action.key)
                if (position != null) {
                    fingerOverlay.moveTo(position)
                }
                fingerOverlay.pressUp()
            }
            is InputAction.Wait -> {
                // 等待已由时间轴控制
            }
            is InputAction.SelectCandidate -> {
                val position = positionResolver.resolveCandidatePosition(action.candidateIndex) ?: return
                fingerOverlay.moveTo(position)
                fingerOverlay.pressDown()
                scope.launch {
                    delay(100)
                    fingerOverlay.pressUp()
                }
                viewModel.handleIntent(ImeIntent.CandidateSelected(/* candidate */))
            }
            is InputAction.SwitchKeyboard -> {
                viewModel.handleIntent(ImeIntent.SwitchKeyboard(action.targetType))
            }
        }
    }
}

sealed class PlaybackState {
    data object Idle : PlaybackState()
    data class Ready(val script: ActionScript) : PlaybackState()
    data class Playing(val script: ActionScript) : PlaybackState()
    data class Paused(val script: ActionScript) : PlaybackState()
    data class Finished(val script: ActionScript) : PlaybackState()
}
```

### 5.2 播放控制面板

```kotlin
@Composable
fun ActionPlayerPanel(
    player: InputActionPlayer,
    modifier: Modifier = Modifier,
) {
    val state by player.playbackState.collectAsState()
    val speed by player.speed.collectAsState()

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // 脚本信息
            val script = (state as? PlaybackState.Ready)?.script
                ?: (state as? PlaybackState.Playing)?.script
                ?: (state as? PlaybackState.Paused)?.script
            if (script != null) {
                Text(
                    text = script.name,
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    text = script.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 播放控制按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { player.stop() }) {
                    Icon(Icons.Default.Stop, "停止")
                }
                IconButton(onClick = {
                    when (state) {
                        is PlaybackState.Playing -> player.pause()
                        else -> player.play()
                    }
                }) {
                    Icon(
                        if (state is PlaybackState.Playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                        if (state is PlaybackState.Playing) "暂停" else "播放",
                    )
                }
                IconButton(onClick = { player.stepForward() }) {
                    Icon(Icons.Default.SkipNext, "步进")
                }
            }

            // 速度控制
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("速度", style = MaterialTheme.typography.labelSmall)
                Slider(
                    value = speed,
                    onValueChange = { player.setSpeed(it) },
                    valueRange = 0.25f..4.0f,
                    steps = 6,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    "${String.format("%.1f", speed)}x",
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
}
```

---

## 6. 脚本管理与序列化

### 6.1 脚本文件格式

动作脚本以 JSON 格式存储，便于人工编辑和版本管理。脚本为坐标无关格式，不包含任何绝对坐标信息：

```json
{
  "name": "输入「你好」",
  "description": "以滑行方式演示输入「你好」",
  "inputMethod": "Swipe",
  "actions": [
    { "type": "KeyDown", "startTime": 0, "key": "char_n" },
    { "type": "SwipeTo", "startTime": 150, "fromKey": "char_n", "toKey": "char_i", "duration": 150 },
    { "type": "KeyUp", "startTime": 230, "key": "char_i" },
    { "type": "Wait", "startTime": 230, "duration": 500 },
    { "type": "SelectCandidate", "startTime": 730, "candidateIndex": 0 },
    { "type": "Wait", "startTime": 730, "duration": 300 },
    { "type": "KeyDown", "startTime": 1030, "key": "char_h" },
    { "type": "SwipeTo", "startTime": 1180, "fromKey": "char_h", "toKey": "char_a", "duration": 150 },
    { "type": "SwipeTo", "startTime": 1330, "fromKey": "char_a", "toKey": "char_o", "duration": 150 },
    { "type": "KeyUp", "startTime": 1480, "key": "char_o" },
    { "type": "Wait", "startTime": 1480, "duration": 500 },
    { "type": "SelectCandidate", "startTime": 1980, "candidateIndex": 0 }
  ]
}
```

**与坐标绑定格式对比**：

| 维度 | 坐标绑定格式（旧） | 坐标无关格式（新） |
|------|-------------------|-------------------|
| 按键位置 | `"position": [120, 340]` | 无，回放时动态解析 |
| 滑行路径 | `"path": [[120,340], [140,335], ...]` | 无，回放时贝塞尔曲线计算 |
| 候选位置 | `"position": [100, 20]` | 无，回放时查询候选栏布局 |
| 设备适配 | 仅在录制设备+录制分辨率下有效 | 任意设备、任意分辨率、任意布局 |
| 布局变更 | 切换手模式后脚本失效 | 自动适配新布局 |
| 脚本大小 | 较大（含路径点数组） | 精简（仅语义标识+时间） |

### 6.2 预置脚本

在 `assets/scripts/` 目录下预置常用演示脚本：

```
code/app/src/main/assets/scripts/
├── demo_swipe_hello.json        ← 滑行输入「你好」
├── demo_swipe_world.json        ← 滑行输入「世界」
├── demo_xpad_continuous.json    ← X-Pad 连续输入演示
├── demo_candidate_selection.json← 候选选择操作演示
└── demo_full_workflow.json      ← 完整输入流程演示

// 录制脚本仅在 debug 构建中使用，放在 debug 源集中
code/app/src/debug/assets/scripts/
└── (用户录制的脚本，不随 release 发布)
```

### 6.3 脚本加载

```kotlin
class ActionScriptLoader(private val context: Context) {

    /** 从 assets 加载预置脚本 */
    fun loadPreset(name: String): ActionScript {
        val json = context.assets.open("scripts/$name.json")
            .bufferedReader().use { it.readText() }
        return parseScript(json)
    }

    /** 列出所有预置脚本 */
    fun listPresets(): List<String> {
        return context.assets.list("scripts")?.toList() ?: emptyList()
    }

    /** 从外部文件加载脚本 */
    fun loadFromFile(uri: Uri): ActionScript {
        val json = context.contentResolver.openInputStream(uri)
            ?.bufferedReader()?.use { it.readText() }
            ?: error("无法读取脚本文件")
        return parseScript(json)
    }

    private fun parseScript(json: String): ActionScript {
        // 使用 Kotlin Serialization 解析 JSON
        return Json.decodeFromString<ActionScript>(json)
    }
}
```

---

## 7. 输入练习演示模式

### 7.1 概述

输入练习演示是输入动作程序化的核心 release 用例。用户选择一段待练习文本后，系统自动编译并播放对应的输入动画，用户可以跟随动画学习如何使用滑行、点击、X-Pad 等方式输入文字。该模式在 v4 中提供基础能力，后续版本将扩展为完整的输入练习系统。

### 7.2 演示界面

```kotlin
/**
 * 输入练习演示页面。
 *
 * 展示待输入文本、当前输入进度和播放控制，
 * 用户可选择不同的输入方式（滑行/点击/X-Pad）观看演示。
 */
@Composable
fun InputPracticeScreen(
    viewModel: InputPracticeViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("输入练习") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "返回")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            // 目标文本展示
            TargetTextDisplay(
                text = state.targetText,
                inputProgress = state.inputProgress,
            )

            // 输入方式选择
            InputMethodSelector(
                methods = InputMethod.entries,
                selected = state.inputMethod,
                onSelected = { viewModel.selectMethod(it) },
            )

            // 键盘区域（含动画覆盖层）
            Box(modifier = Modifier.weight(1f)) {
                KeyboardWithOverlay(
                    keyboardState = state.keyboardState,
                    fingerOverlay = viewModel.fingerOverlayState,
                    trailOverlay = viewModel.actionPlayer.trailPoints,
                )
            }

            // 播放控制
            ActionPlayerPanel(
                player = viewModel.actionPlayer,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/**
 * 目标文本展示，高亮已输入部分。
 */
@Composable
fun TargetTextDisplay(
    text: String,
    inputProgress: Int, // 已完成的字符数
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "目标：",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.width(4.dp))
            // 已完成部分高亮，未完成部分灰色
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                onTextLayout = { textLayoutResult ->
                    // 根据 inputProgress 设置不同颜色范围
                },
            )
        }
    }
}

/**
 * 输入方式选择器。
 */
@Composable
fun InputMethodSelector(
    methods: List<InputMethod>,
    selected: InputMethod,
    onSelected: (InputMethod) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        methods.forEach { method ->
            FilterChip(
                selected = method == selected,
                onClick = { onSelected(method) },
                label = { Text(method.displayName) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}
```

### 7.3 练习 ViewModel

```kotlin
/**
 * 输入练习 ViewModel。
 *
 * 管理练习状态、编译脚本、控制播放。
 */
class InputPracticeViewModel(
    private val compiler: ActionScriptCompiler,
    private val imeViewModel: ImeViewModel,
    private val positionResolver: KeyPositionResolver,
) : ViewModel() {
    private val _state = MutableStateFlow(InputPracticeState())
    val state: StateFlow<InputPracticeState> = _state.asStateFlow()

    val fingerOverlayState = FingerOverlayState()
    val actionPlayer = InputActionPlayer(
        viewModel = imeViewModel,
        fingerOverlay = fingerOverlayState,
        positionResolver = positionResolver,
        scope = viewModelScope,
    )

    fun setTargetText(text: String) {
        _state.update { it.copy(targetText = text) }
        compileAndLoad()
    }

    fun selectMethod(method: InputMethod) {
        _state.update { it.copy(inputMethod = method) }
        compileAndLoad()
    }

    private fun compileAndLoad() {
        val current = _state.value
        if (current.targetText.isBlank()) return
        val script = compiler.compile(
            text = current.targetText,
            method = current.inputMethod,
        )
        actionPlayer.load(script)
    }
}

data class InputPracticeState(
    val targetText: String = "",
    val inputMethod: InputMethod = InputMethod.Swipe,
    val inputProgress: Int = 0,
    val keyboardState: KeyboardState = KeyboardState.Idle,
)
```

### 7.4 预置练习脚本

在 `assets/scripts/` 中预置常用练习脚本，release 构建中包含：

| 脚本名 | 目标文本 | 输入方式 | 说明 |
|--------|----------|----------|------|
| `demo_swipe_hello` | 你好 | Swipe | 基础滑行入门 |
| `demo_swipe_world` | 世界 | Swipe | 短词滑行 |
| `demo_swipe_thanks` | 谢谢 | Swipe | 重复字滑行 |
| `demo_tap_basic` | 大 | Tap | 单字点击 |
| `demo_xpad_continuous` | 你好 | XPad | X-Pad 连续输入 |
| `demo_candidate_selection` | 输入法 | Swipe | 候选选择 |
| `demo_full_workflow` | 你好世界 | Swipe | 完整输入流程 |

---

## 8. 操作记录与回放

### 8.1 录制模式

除了从字符序列编译脚本，还可以录制用户的真实操作生成脚本。录制功能是开发工具，仅在 debug 构建中可用。录制的脚本同样是坐标无关的——只记录按键标识和时间，不记录坐标：

```kotlin
// debug 源集 — 录制功能仅 debug 构建可用
/**
 * 操作录制器，坐标无关。
 *
 * 在键盘交互层拦截手势事件，记录每个动作的时间戳和按键标识，
 * 生成坐标无关的可回放动作脚本。
 * 不记录绝对坐标，确保录制的脚本可在任意布局下回放。
 */
class ActionRecorder {
    private val recordedActions = mutableListOf<InputAction>()
    private var startTime = 0L

    fun start() {
        recordedActions.clear()
        startTime = System.currentTimeMillis()
    }

    fun recordKeyDown(key: InputKey) {
        recordedActions += InputAction.KeyDown(
            startTime = System.currentTimeMillis() - startTime,
            key = key,
        )
    }

    fun recordSwipeTo(fromKey: InputKey, toKey: InputKey, duration: Long) {
        recordedActions += InputAction.SwipeTo(
            startTime = System.currentTimeMillis() - startTime,
            fromKey = fromKey,
            toKey = toKey,
            duration = duration,
        )
    }

    fun recordKeyUp(key: InputKey) {
        recordedActions += InputAction.KeyUp(
            startTime = System.currentTimeMillis() - startTime,
            key = key,
        )
    }

    fun recordCandidateSelection(candidateIndex: Int) {
        recordedActions += InputAction.SelectCandidate(
            startTime = System.currentTimeMillis() - startTime,
            candidateIndex = candidateIndex,
        )
    }

    fun stop(): ActionScript? {
        if (recordedActions.isEmpty()) return null
        return ActionScript(
            name = "录制操作",
            description = "录制的用户操作",
            inputMethod = InputMethod.Swipe,
            actions = recordedActions.toList(),
            totalDuration = recordedActions.maxOf { it.startTime },
        )
    }
}
```

### 8.2 录制入口

录制功能通过 UI 测试工具栏（文档 910）的浮动按钮触发，仅 debug 构建可用：

```kotlin
// debug 源集 — 录制按钮仅 debug 构建可用
@Composable
fun RecordButton(
    isRecording: Boolean,
    onToggle: () -> Unit,
) {
    FloatingActionButton(
        onClick = onToggle,
        modifier = Modifier.size(40.dp),
    ) {
        Icon(
            imageVector = if (isRecording) Icons.Default.StopCircle else Icons.Default.FiberManualRecord,
            contentDescription = if (isRecording) "停止录制" else "开始录制",
            tint = if (isRecording) Color.Red else MaterialTheme.colorScheme.onSurface,
        )
    }
}
```

---

## 9. 自动化测试集成

### 9.1 无动画模式

在自动化测试场景中，不需要动画播放，直接按顺序执行动作。由于脚本是坐标无关的，测试驱动器不需要 `KeyPositionResolver`：

```kotlin
/**
 * 自动化测试驱动器。
 *
 * 与 InputActionPlayer 不同，不驱动动画和 UI 反馈，
 * 直接按顺序向 ViewModel 发送 Intent 并验证 State 变化。
 * 可在任何线程运行，不依赖 Compose UI 和坐标解析。
 */
class ActionTestDriver(
    private val viewModel: ImeViewModel,
) {
    private val results = mutableListOf<TestStepResult>()

    fun execute(script: ActionScript): List<TestStepResult> {
        results.clear()

        for (action in script.actions) {
            when (action) {
                is InputAction.KeyDown -> {
                    val before = viewModel.state.value
                    viewModel.handleIntent(ImeIntent.KeyPressed(action.key, KeyGesture.Tap))
                    val after = viewModel.state.value
                    results += TestStepResult(action, before, after)
                }
                is InputAction.SelectCandidate -> {
                    val before = viewModel.state.value
                    viewModel.handleIntent(ImeIntent.CandidateSelected(/* candidate */))
                    val after = viewModel.state.value
                    results += TestStepResult(action, before, after)
                }
                // Wait / KeyUp / SwipeTo 等在测试模式下跳过
                else -> continue
            }
        }

        return results.toList()
    }
}

data class TestStepResult(
    val action: InputAction,
    val stateBefore: ImeState,
    val stateAfter: ImeState,
)
```

### 9.2 测试用例示例

```kotlin
class PinyinInputActionTest {
    @Test
    fun `swipe input ni produces candidate 你`() {
        val viewModel = ImeViewModel(/* ... */)
        val compiler = ActionScriptCompiler(/* ... */)

        val script = compiler.compile("你", InputMethod.Swipe)
        val driver = ActionTestDriver(viewModel)
        val results = driver.execute(script)

        // 验证最终状态包含正确的候选
        val finalState = results.last().stateAfter
        assertTrue(finalState.candidates.candidates.any { it.text == "你" })
    }
}
```

---

## 10. 构建隔离

### 10.1 代码组织

输入动作程序化的代码按功能分层，动画播放和练习演示作为 release 可用能力放在 main 源集中，仅操作录制等开发工具放在 debug 源集中：

| 层 | 源集 | 内容 | Release 包含 |
|----|------|------|-------------|
| 模型层 | `main` | `InputAction`（坐标无关）、`ActionScript`、`InputMethod` | ✅ |
| 编译层 | `main` | `ActionScriptCompiler`（字符→逻辑动作） | ✅ |
| 坐标解析 | `main` | `KeyPositionResolver`、`ComposeKeyPositionResolver` | ✅ |
| 路径生成 | `main` | `SwipePathInterpolator`（贝塞尔曲线） | ✅ |
| 播放层 | `main` | `InputActionPlayer`、`FingerOverlayState` | ✅ |
| 动画覆盖层 | `main` | `FingerOverlay`、`SwipeTrailOverlay`、`KeyHighlightOverlay` | ✅ |
| 演示 UI | `main` | `InputPracticeScreen`、`ActionPlayerPanel`、`InputMethodSelector` | ✅ |
| 预置脚本 | `main` | `assets/scripts/` 预置演示脚本 | ✅ |
| 录制工具 | `debug` | `ActionRecorder`、`RecordButton` | ❌ 开发工具 |
| 测试驱动 | `test` | `ActionTestDriver` | ❌（test 依赖） |

### 10.2 Release 构建策略

Release 构建中：
- 动画播放引擎和手指指示器完整可用，用于输入练习演示
- 预置练习脚本随 APK 发布
- `InputPracticeScreen` 可从设置或引导页进入
- 操作录制功能不在 APK 中（`ActionRecorder` 和 `RecordButton` 在 debug 源集）
- 播放速度、手指指示器样式等参数可通过配置系统调整
- 坐标解析器在每次回放时实时查询键盘布局，确保适配当前设备

### 10.3 包体积控制

动画系统以 Compose Canvas 绘制，不引入额外资源文件（除预置脚本 JSON 外）。预置脚本总大小控制在 50KB 以内。手指指示器和轨迹使用 Canvas 即时绘制，不使用图片资源。坐标无关格式进一步减小脚本体积（无需存储路径点数组）。

---

## 11. 坐标无关设计总结

### 11.1 设计原则

| 原则 | 说明 |
|------|------|
| **脚本即意图** | 动作脚本只描述「做什么」（按哪个键、滑向哪个键），不描述「在哪里做」（坐标） |
| **延迟绑定** | 坐标在回放时绑定，而非编译/录制时绑定 |
| **布局自适应** | 同一脚本在不同布局下自动适配，无需多版本脚本 |
| **设备无关** | 脚本不依赖屏幕分辨率、密度、尺寸等设备参数 |

### 11.2 坐标解析时机

```
编译时（ActionScriptCompiler）  录制时（ActionRecorder）   回放时（InputActionPlayer）
─────────────────────────    ────────────────────────    ─────────────────────────
只生成逻辑动作                 只记录按键标识+时间           解析坐标 + 驱动动画
InputKey + startTime          InputKey + startTime         positionResolver.resolve(key)
无坐标字段                     无坐标字段                    → Offset（实时）
```

### 11.3 适应性场景

| 变更场景 | 坐标绑定脚本 | 坐标无关脚本 |
|----------|-------------|-------------|
| 切换左右手模式 | ❌ 坐标翻转，脚本失效 | ✅ 回放时查询新布局 |
| 屏幕旋转 | ❌ 坐标变化，脚本失效 | ✅ 回放时查询新布局 |
| 不同屏幕尺寸设备 | ❌ 坐标不匹配 | ✅ 回放时查询实际布局 |
| 键盘布局版本升级 | ❌ 按键位置可能变化 | ✅ 回放时查询新布局 |
| X-Pad 启用/禁用 | ❌ 键盘类型变化 | ✅ 回放时查询当前键盘 |

---

## 12. 与其他系统的协作

| 协作系统 | 协作方式 |
|----------|----------|
| 键盘状态机（100） | 播放器通过 `ImeIntent` 驱动状态机，与真实用户操作路径一致 |
| UI Compose 迁移（400） | 动画覆盖层以 Compose Canvas 叠加在键盘 UI 之上；`ComposeKeyPositionResolver` 通过布局系统获取实时坐标 |
| 配置界面改进（920） | 输入练习演示可从设置页的「输入体验」分组或「关于」分组进入 |
| 配置系统（500） | 播放速度、手指指示器样式等参数作为配置项持久化 |
| UI 测试工具（910） | 操作录制的入口集成到 UI 测试浮动工具栏（仅 debug） |
| 日志系统（900） | 动作播放过程中记录 DEBUG 日志，便于排障 |
| 引导系统（Java Guide） | 替代 Java 版本的 `ExerciseGuide`，提供更自然的动画引导 |
| 用户数据导入导出（800） | 练习脚本可随用户数据一起导入导出 |
