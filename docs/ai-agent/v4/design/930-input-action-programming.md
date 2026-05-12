# 930 — 输入动作程序化设计

## 1. 概述

v4 版本新增输入动作程序化能力，即通过指定待输入的字符序列，自动以动画模拟滑行或点击等方式完成输入过程。该功能是 **release 构建即提供的用户级能力**，主要用于输入练习演示（如教学引导、操作展示），后续版本将基于此能力提供完整的输入练习功能。核心思路是：将用户真实的手指操作抽象为可序列化的「动作脚本」，由播放引擎按脚本驱动键盘状态机和 UI 动画，产生与真人操作一致的视觉效果。

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
| 操作回放 | 记录并回放用户的真实操作路径 | 真实路径回放 | release ✅ |
| 自动化测试 | 按脚本自动操作键盘，验证状态机转换和输出正确性 | 即时执行（无动画） | test ✅ |
| 操作录制 | 录制用户真实操作生成脚本 | 开发者工具 | debug ✅ |

### 2.2 核心需求

| 需求 | 说明 |
|------|------|
| 字符序列转动作脚本 | 输入「你好」→ 自动生成滑行/点击的动作序列 |
| 动画模拟 | 在键盘 UI 上以动画形式展示手指移动、按键按下、滑行轨迹等 |
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
│  - 与 ViewModel 集成                                            │
└──────────┬───────────────────────────┬─────────────────────────┘
           │                           │
           ▼                           ▼
┌──────────────────────┐    ┌────────────────────────┐
│  ActionScriptCompiler│    │  ActionAnimator        │
│  (脚本编译器)         │    │  (动画引擎)             │
│  字符序列 → 动作序列   │    │  动作 → Compose 动画    │
└──────────┬───────────┘    └───────────┬────────────┘
           │                            │
           ▼                            ▼
┌──────────────────────┐    ┌────────────────────────┐
│  InputAction         │    │  FingerOverlay         │
│  (动作定义)           │    │  (手指指示器)           │
│  Sealed class        │    │  Canvas + Animatable   │
└──────────────────────┘    └────────────────────────┘
```

### 3.2 数据模型

```kotlin
/**
 * 输入动作，不可变。
 * 每个动作描述一次原子操作（按下、滑行到、抬起等）。
 */
sealed class InputAction {
    /** 动作开始时刻（毫秒），相对于脚本起始 */
    abstract val startTime: Long

    /** 按下按键 */
    data class KeyDown(
        override val startTime: Long,
        val key: InputKey,
        val position: Offset,
    ) : InputAction()

    /** 滑行到按键（手指从当前位置移动到目标按键） */
    data class SwipeTo(
        override val startTime: Long,
        val fromKey: InputKey,
        val toKey: InputKey,
        val fromPosition: Offset,
        val toPosition: Offset,
        val path: List<Offset>, // 滑行路径点（插值后的平滑曲线）
        val duration: Long, // 滑行持续时间（毫秒）
    ) : InputAction()

    /** 抬起手指 */
    data class KeyUp(
        override val startTime: Long,
        val key: InputKey,
        val position: Offset,
    ) : InputAction()

    /** 等待（用于添加间隔） */
    data class Wait(
        override val startTime: Long,
        val duration: Long,
    ) : InputAction()

    /** 选择候选项 */
    data class SelectCandidate(
        override val startTime: Long,
        val candidateIndex: Int,
        val position: Offset,
    ) : InputAction()

    /** 切换键盘 */
    data class SwitchKeyboard(
        override val startTime: Long,
        val targetType: KeyboardType,
    ) : InputAction()
}

/**
 * 输入动作脚本，不可变。
 * 由一组有序的动作组成，附带元信息。
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

### 3.3 动作脚本编译器

将待输入字符序列编译为具体的动作序列，需要结合键盘布局信息计算按键位置和滑行路径。

```kotlin
/**
 * 动作脚本编译器。
 *
 * 根据待输入文本、输入方式和键盘布局，生成具体的动作序列。
 * 编译过程需要查询键盘的按键表，计算按键位置和滑行路径。
 */
class ActionScriptCompiler(
    private val keyTableProvider: (KeyboardType) -> List<List<InputKey>>,
    private val keyPositionResolver: (InputKey) -> Offset,
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
                val pos = keyPositionResolver(key)

                actions += InputAction.KeyDown(time(actions.lastOrNull()?.startTime?.plus(200) ?: 0L), key, pos)
                val downTime = actions.last().startTime
                actions += InputAction.KeyUp(downTime + 80, key, pos)

                // 输入最后一个字符后等待候选
                if (char == chars.last()) {
                    actions += InputAction.Wait(downTime + 80, 500)
                    // 选择第一个候选
                    actions += InputAction.SelectCandidate(downTime + 580, 0, Offset(100f, 20f))
                }
            }
        }
    }

    /**
     * 编译滑行方式输入的动作序列。
     *
     * 每个拼音段：KeyDown（起始键）→ SwipeTo（中间键）→ ... → KeyUp → 选择候选
     * 滑行路径通过贝塞尔曲线插值生成平滑轨迹。
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
            val firstPos = keyPositionResolver(firstKey)
            actions += InputAction.KeyDown(currentTime, firstKey, firstPos)

            // 后续字符：滑行到
            for (i in 1 until chars.size) {
                val prevKey = findCharKey(pinyinKeyTable, chars[i - 1]) ?: continue
                val nextKey = findCharKey(pinyinKeyTable, chars[i]) ?: continue
                val prevPos = keyPositionResolver(prevKey)
                val nextPos = keyPositionResolver(nextKey)

                val swipeDuration = 150L * (i) // 逐步加速
                val path = interpolateSwipePath(prevPos, nextPos)

                currentTime += swipeDuration
                actions += InputAction.SwipeTo(
                    startTime = currentTime,
                    fromKey = prevKey,
                    toKey = nextKey,
                    fromPosition = prevPos,
                    toPosition = nextPos,
                    path = path,
                    duration = swipeDuration,
                )
            }

            // 抬起
            val lastKey = findCharKey(pinyinKeyTable, chars.last()) ?: continue
            val lastPos = keyPositionResolver(lastKey)
            currentTime += 80
            actions += InputAction.KeyUp(currentTime, lastKey, lastPos)

            // 等待候选
            currentTime += 400
            actions += InputAction.Wait(currentTime, 400)

            // 选择候选
            currentTime += 400
            actions += InputAction.SelectCandidate(currentTime, 0, Offset(100f, 20f))

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
        // X-Pad 编译需要查询 X-Pad 的区域布局
        val pinyinSegments = textToPinyinSegments(text)

        for (segment in pinyinSegments) {
            // X-Pad 输入：从中心开始，滑行经过目标区域
            // 具体路径取决于 X-Pad 布局，需要动态计算
            val zoneSequence = resolveXPadZones(segment.spell)
            // ... 编译为滑行动作
        }
    }

    /** 贝塞尔曲线插值生成平滑滑行路径 */
    private fun interpolateSwipePath(from: Offset, to: Offset): List<Offset> {
        val points = mutableListOf<Offset>()
        val steps = 20
        // 简单二次贝塞尔曲线：控制点在 from 和 to 的中点上方
        val controlPoint = Offset(
            (from.x + to.x) / 2,
            (from.y + to.y) / 2 - 20f, // 向上弧度
        )

        for (i in 0..steps) {
            val t = i.toFloat() / steps
            val x = (1 - t) * (1 - t) * from.x + 2 * (1 - t) * t * controlPoint.x + t * t * to.x
            val y = (1 - t) * (1 - t) * from.y + 2 * (1 - t) * t * controlPoint.y + t * t * to.y
            points += Offset(x, y)
        }
        return points
    }

    /** 文本转拼音段（简化实现，实际需要调用拼音引擎） */
    private fun textToPinyinSegments(text: String): List<PinyinSegment> {
        // 调用拼音字典将汉字转为拼音序列
        // 例如 "你好" → [PinyinSegment("ni"), PinyinSegment("hao")]
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

### 4.1 手指指示器

在键盘上方叠加一个虚拟手指指示器，跟随动作位置移动：

```kotlin
/**
 * 手指指示器覆盖层。
 *
 * 在键盘上绘制一个半透明的圆形手指指示器，
 * 跟随 InputAction 的位置信息移动，模拟用户手指操作。
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

    fun animateMoveTo(offset: Offset, durationMs: Long) {
        // 使用 Animatable 实现平滑移动
        // ... 动画逻辑
    }

    fun show() { _visible.value = true }
    fun hide() { _visible.value = false }
    fun pressDown() { _pressed.value = true }
    fun pressUp() { _pressed.value = false }
}
```

### 4.2 滑行轨迹动画

在滑行动作播放时，实时绘制手指移动轨迹：

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

### 4.3 按键高亮动画

模拟按下按键时的视觉反馈：

```kotlin
@Composable
fun KeyHighlightOverlay(
    highlightedKeys: Set<InputKey>,
    keyPositionResolver: (InputKey) -> Rect,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        highlightedKeys.forEach { key ->
            val rect = keyPositionResolver(key)
            drawRoundRect(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                topLeft = rect.topLeft,
                size = rect.size,
                cornerRadius = CornerRadius(4.dp.toPx()),
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
 * 输入动作播放器。
 *
 * 接收 ActionScript，按时间轴依次执行动作：
 * - 将 InputAction 转换为 IMEIntent 发送到 ViewModel
 * - 同步驱动动画覆盖层（手指指示器、轨迹、高亮）
 * - 提供播放控制接口
 * release 构建中可用，用于输入练习演示。
 */
class InputActionPlayer(
    private val viewModel: IMEViewModel,
    private val fingerOverlay: FingerOverlayState,
    private val scope: CoroutineScope,
) {
    private var job: Job? = null
    private var _playbackState = MutableStateFlow(PlaybackState.Idle)
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private var _speed = MutableStateFlow(1.0f)
    val speed: StateFlow<Float> = _speed.asStateFlow()

    private var currentScript: ActionScript? = null
    private var actionIndex = 0

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

    private fun executeAction(action: InputAction) {
        when (action) {
            is InputAction.KeyDown -> {
                fingerOverlay.moveTo(action.position)
                fingerOverlay.pressDown()
                viewModel.handleIntent(IMEIntent.KeyPressed(action.key, KeyGesture.Tap))
            }
            is InputAction.SwipeTo -> {
                fingerOverlay.animateMoveTo(action.toPosition, action.duration)
                viewModel.handleIntent(IMEIntent.KeyPressed(action.toKey, KeyGesture.Swipe))
            }
            is InputAction.KeyUp -> {
                fingerOverlay.pressUp()
            }
            is InputAction.Wait -> {
                // 等待已由时间轴控制
            }
            is InputAction.SelectCandidate -> {
                fingerOverlay.moveTo(action.position)
                fingerOverlay.pressDown()
                // 短暂延迟后抬起
                scope.launch {
                    delay(100)
                    fingerOverlay.pressUp()
                }
                viewModel.handleIntent(IMEIntent.CandidateSelected(/* candidate */))
            }
            is InputAction.SwitchKeyboard -> {
                viewModel.handleIntent(IMEIntent.SwitchKeyboard(action.targetType))
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

动作脚本以 JSON 格式存储，便于人工编辑和版本管理：

```json
{
  "name": "输入「你好」",
  "description": "以滑行方式演示输入「你好」",
  "inputMethod": "Swipe",
  "actions": [
    { "type": "KeyDown", "startTime": 0, "key": "char_n", "position": [120, 340] },
    { "type": "SwipeTo", "startTime": 150, "fromKey": "char_n", "toKey": "char_i", "duration": 150, "path": [...] },
    { "type": "KeyUp", "startTime": 230, "key": "char_i", "position": [200, 340] },
    { "type": "Wait", "startTime": 230, "duration": 500 },
    { "type": "SelectCandidate", "startTime": 730, "candidateIndex": 0, "position": [100, 20] },
    { "type": "Wait", "startTime": 730, "duration": 300 },
    { "type": "KeyDown", "startTime": 1030, "key": "char_h", "position": [280, 240] },
    { "type": "SwipeTo", "startTime": 1180, "fromKey": "char_h", "toKey": "char_a", "duration": 150, "path": [...] },
    { "type": "SwipeTo", "startTime": 1330, "fromKey": "char_a", "toKey": "char_o", "duration": 150, "path": [...] },
    { "type": "KeyUp", "startTime": 1480, "key": "char_o", "position": [360, 340] },
    { "type": "Wait", "startTime": 1480, "duration": 500 },
    { "type": "SelectCandidate", "startTime": 1980, "candidateIndex": 0, "position": [100, 20] }
  ]
}
```

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
                    trailOverlay = viewModel.trailPoints,
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
) : ViewModel() {
    private val _state = MutableStateFlow(InputPracticeState())
    val state: StateFlow<InputPracticeState> = _state.asStateFlow()

    val fingerOverlayState = FingerOverlayState()
    val actionPlayer = InputActionPlayer(
        viewModel = /* IMEViewModel */,
        fingerOverlay = fingerOverlayState,
        scope = viewModelScope,
    )
    private val _trailPoints = MutableStateFlow<List<Offset>>(emptyList())
    val trailPoints: StateFlow<List<Offset>> = _trailPoints.asStateFlow()

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

除了从字符序列编译脚本，还可以录制用户的真实操作路径生成脚本。录制功能是开发工具，仅在 debug 构建中可用：

```kotlin
// debug 源集 — 录制功能仅 debug 构建可用
/**
 * 操作录制器。
 *
 * 在键盘交互层拦截手势事件，记录每个动作的时间戳和位置，
 * 生成可回放的动作脚本。
 */
class ActionRecorder {
    private val recordedActions = mutableListOf<InputAction>()
    private var startTime = 0L

    fun start() {
        recordedActions.clear()
        startTime = System.currentTimeMillis()
    }

    fun recordKeyDown(key: InputKey, position: Offset) {
        recordedActions += InputAction.KeyDown(
            startTime = System.currentTimeMillis() - startTime,
            key = key,
            position = position,
        )
    }

    fun recordSwipePath(fromKey: InputKey, toKey: InputKey, path: List<Offset>, duration: Long) {
        recordedActions += InputAction.SwipeTo(
            startTime = System.currentTimeMillis() - startTime,
            fromKey = fromKey,
            toKey = toKey,
            fromPosition = path.first(),
            toPosition = path.last(),
            path = path,
            duration = duration,
        )
    }

    fun recordKeyUp(key: InputKey, position: Offset) {
        recordedActions += InputAction.KeyUp(
            startTime = System.currentTimeMillis() - startTime,
            key = key,
            position = position,
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

在自动化测试场景中，不需要动画播放，直接按顺序执行动作：

```kotlin
/**
 * 自动化测试驱动器。
 *
 * 与 InputActionPlayer 不同，不驱动动画和 UI 反馈，
 * 直接按顺序向 ViewModel 发送 Intent 并验证 State 变化。
 * 可在任何线程运行，不依赖 Compose UI。
 */
class ActionTestDriver(
    private val viewModel: IMEViewModel,
) {
    private val results = mutableListOf<TestStepResult>()

    fun execute(script: ActionScript): List<TestStepResult> {
        results.clear()

        for (action in script.actions) {
            when (action) {
                is InputAction.KeyDown -> {
                    val before = viewModel.state.value
                    viewModel.handleIntent(IMEIntent.KeyPressed(action.key, KeyGesture.Tap))
                    val after = viewModel.state.value
                    results += TestStepResult(action, before, after)
                }
                is InputAction.SelectCandidate -> {
                    // 选择候选并验证
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
    val stateBefore: IMEState,
    val stateAfter: IMEState,
)
```

### 9.2 测试用例示例

```kotlin
class PinyinInputActionTest {
    @Test
    fun `swipe input ni produces candidate 你`() {
        val viewModel = IMEViewModel(/* ... */)
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
| 模型层 | `main` | `InputAction`、`ActionScript`、`InputMethod` 定义 | ✅ |
| 编译层 | `main` | `ActionScriptCompiler`（字符→动作） | ✅ |
| 播放层 | `main` | `InputActionPlayer`、`FingerOverlayState` | ✅ 输入练习演示所需 |
| 动画覆盖层 | `main` | `FingerOverlay`、`SwipeTrailOverlay`、`KeyHighlightOverlay` | ✅ 输入练习演示所需 |
| 演示 UI | `main` | `InputPracticeScreen`、`ActionPlayerPanel`、`InputMethodSelector` | ✅ 输入练习演示入口 |
| 预置脚本 | `main` | `assets/scripts/` 预置演示脚本 | ✅ 练习素材 |
| 录制工具 | `debug` | `ActionRecorder`、`RecordButton` | ❌ 开发工具 |
| 测试驱动 | `test` | `ActionTestDriver` | ❌（test 依赖） |

### 10.2 Release 构建策略

Release 构建中：
- 动画播放引擎和手指指示器完整可用，用于输入练习演示
- 预置练习脚本随 APK 发布
- `InputPracticeScreen` 可从设置或引导页进入
- 操作录制功能不在 APK 中（`ActionRecorder` 和 `RecordButton` 在 debug 源集）
- 播放速度、手指指示器样式等参数可通过配置系统调整

### 10.3 包体积控制

动画系统以 Compose Canvas 绘制，不引入额外资源文件（除预置脚本 JSON 外）。预置脚本总大小控制在 50KB 以内。手指指示器和轨迹使用 Canvas 即时绘制，不使用图片资源。

---

## 11. 与其他系统的协作

| 协作系统 | 协作方式 |
|----------|----------|
| 键盘状态机（100） | 播放器通过 `IMEIntent` 驱动状态机，与真实用户操作路径一致 |
| UI Compose 迁移（400） | 动画覆盖层以 Compose Canvas 叠加在键盘 UI 之上 |
| 配置界面改进（920） | 输入练习演示可从设置页的「输入体验」分组或「关于」分组进入 |
| 配置系统（500） | 播放速度、手指指示器样式等参数作为配置项持久化 |
| UI 测试工具（910） | 操作录制的入口集成到 UI 测试浮动工具栏（仅 debug） |
| 日志系统（900） | 动作播放过程中记录 DEBUG 日志，便于排障 |
| 引导系统（Java Guide） | 替代 Java 版本的 `ExerciseGuide`，提供更自然的动画引导 |
| 用户数据导入导出（800） | 练习脚本可随用户数据一起导入导出 |
