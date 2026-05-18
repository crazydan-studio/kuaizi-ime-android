# 输入动作程序化

## 1. 概述

输入动作程序化是通过指定待输入的字符序列，自动以动画模拟滑行或点击等方式完成输入过程的能力。核心思路是：将用户真实的手指操作抽象为可序列化的「动作脚本」，由播放引擎按脚本驱动键盘状态机和 UI 动画，产生与真人操作一致的视觉效果。

**坐标无关设计**：动作脚本只记录按键的语义标识（如 `InputKey`），不存储任何绝对坐标。回放时，播放器根据当前键盘状态动态查找按键的实时位置，从而消除按键布局变更、屏幕尺寸变化、手模式切换等因素导致的回放失效问题。

> 本文档涵盖 `:ime-engine` 模块中的核心数据模型、编译器、播放状态模型（`InputActionPlaybackState`）、指示器模型（`InputActionFingerIndicator`，兼作行指示器）、路径插值算法（`InputActionPathInterpolator`）、位置解析器接口（`InputActionPositionResolver`）以及归一化坐标基础类型（`OffsetF`、`RectF`）。播放器（`InputActionPlayer`）的主体逻辑和 UI 覆盖层（`FingerOverlay`、`SwipeTrailOverlay`、`KeyHighlightOverlay`）属于 `:ime-ui` / `:app` 模块，不在本文档范围内。

---

## 2. InputAction 逻辑动作定义

```kotlin
/**
 * 输入动作，不可变，坐标无关。
 *
 * 每个动作描述一次原子操作（按下、滑行到、抬起等），
 * 仅包含按键的语义标识和时间信息，不包含绝对坐标。
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
     * 不存储路径点坐标，回放时动态计算贝塞尔曲线路径。
     */
    data class SwipeTo(
        override val startTime: Long,
        val fromKey: InputKey,
        val toKey: InputKey,
        val duration: Long,
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
     * 不存储坐标，回放时查询候选栏布局定位。
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
```

---

## 3. InputActionScript 动作脚本

```kotlin
/**
 * 输入动作脚本，不可变，坐标无关。
 * 由一组有序的逻辑动作组成，附带元信息。
 */
data class InputActionScript(
    val name: String,
    val description: String,
    val inputMethod: InputMethod,
    val actions: List<InputAction>,
    val totalDuration: Long,
)
```

---

## 4. InputMethod 输入方式

```kotlin
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

val InputMethod.displayName: String
    get() = when (this) {
        InputMethod.Tap -> "点击"
        InputMethod.Swipe -> "滑行"
        InputMethod.XPad -> "X-Pad"
    }
```

---

## 5. PinyinSegment 拼音段

```kotlin
data class PinyinSegment(
    val text: String,   // 目标汉字
    val spell: String,  // 拼音
)
```

---

## 6. InputActionScriptCompiler 脚本编译器

将待输入字符序列编译为逻辑动作序列。编译器仅需要知道键盘的按键表（哪些键存在），不需要知道按键的像素位置：

```kotlin
/**
 * 动作脚本编译器，坐标无关。
 *
 * 根据待输入文本和输入方式，生成逻辑动作序列。
 * 编译过程只查询键盘的按键表（哪些键存在），不涉及坐标计算。
 */
class InputActionScriptCompiler(
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
    ): InputActionScript {
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

        return InputActionScript(
            name = "输入: $text",
            description = "以${method.displayName}方式输入「$text」",
            inputMethod = method,
            actions = adjustedActions,
            totalDuration = adjustedActions.maxOfOrNull { it.startTime } ?: 0L,
        )
    }

    /**
     * 编译点击方式输入的动作序列。
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
                val key = findCharKey(pinyinKeyTable, char) ?: continue

                val downTime = actions.lastOrNull()?.startTime?.plus(200) ?: 0L
                actions += InputAction.KeyDown(downTime, key)
                actions += InputAction.KeyUp(downTime + 80, key)

                if (char == chars.last()) {
                    actions += InputAction.Wait(downTime + 80, 500)
                    actions += InputAction.SelectCandidate(downTime + 580, 0)
                }
            }
        }
    }

    /**
     * 编译滑行方式输入的动作序列。
     * 每个拼音段：KeyDown（起始键）→ SwipeTo（中间键）→ ... → KeyUp → 选择候选
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

            val firstKey = findCharKey(pinyinKeyTable, chars.first()) ?: continue
            actions += InputAction.KeyDown(currentTime, firstKey)

            for (i in 1 until chars.size) {
                val prevKey = findCharKey(pinyinKeyTable, chars[i - 1]) ?: continue
                val nextKey = findCharKey(pinyinKeyTable, chars[i]) ?: continue

                val swipeDuration = 150L * i
                currentTime += swipeDuration
                actions += InputAction.SwipeTo(
                    startTime = currentTime,
                    fromKey = prevKey,
                    toKey = nextKey,
                    duration = swipeDuration,
                )
            }

            val lastKey = findCharKey(pinyinKeyTable, chars.last()) ?: continue
            currentTime += 80
            actions += InputAction.KeyUp(currentTime, lastKey)

            currentTime += 400
            actions += InputAction.Wait(currentTime, 400)

            currentTime += 400
            actions += InputAction.SelectCandidate(currentTime, 0)

            if (index < pinyinSegments.size - 1) {
                currentTime += 300
                actions += InputAction.Wait(currentTime, 300)
            }
        }
    }

    /**
     * 编译 X-Pad 方式输入的动作序列。
     * X-Pad 模式下，手指在六边形区域间滑行。
     */
    private fun compileXPadActions(
        text: String,
        actions: MutableList<InputAction>,
        time: (Long) -> Unit,
    ) {
        val pinyinSegments = textToPinyinSegments(text)

        for (segment in pinyinSegments) {
            val zoneSequence = resolveXPadZones(segment.spell)
            // ... 编译为滑行动作（坐标无关，仅引用 InputKey）
        }
    }

    private fun textToPinyinSegments(text: String): List<PinyinSegment> {
        return PinyinTextConverter.convert(text)
    }
}
```

---

## 7. 脚本序列化格式

动作脚本以 JSON 格式存储，便于人工编辑和版本管理。脚本为坐标无关格式：

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

---

## 8. 输入动作播放状态 (InputActionPlaybackState)

| 属性 | 说明 |
|------|------|
| 角色 | 输入动作播放生命周期状态模型 |
| 职责 | 描述播放器的状态转换：Idle → Ready → Playing ↔ Paused → Finished |
| 约束 | 纯数据模型，不持有 UI 引用，不依赖 Compose |
| 所属模块 | 本文档（:ime-engine 模块）|

```kotlin
/**
 * 输入动作播放状态。
 *
 * 描述 InputActionPlayer 的播放生命周期状态。
 * 纯逻辑状态模型，不依赖 UI 层，定义见本文档。
 */
sealed class InputActionPlaybackState {
    /** 空闲状态，未加载脚本 */
    data object Idle : InputActionPlaybackState()
    /** 就绪状态，已加载脚本但未开始播放 */
    data class Ready(val script: InputActionScript) : InputActionPlaybackState()
    /** 播放中状态 */
    data class Playing(val currentIndex: Int, val totalActions: Int) : InputActionPlaybackState()
    /** 暂停状态 */
    data class Paused(val currentIndex: Int, val totalActions: Int) : InputActionPlaybackState()
    /** 播放完成状态 */
    data object Finished : InputActionPlaybackState()
}
```

---

## 9. 输入动作指示器 (InputActionFingerIndicator)

| 属性 | 说明 |
|------|------|
| 角色 | 播放动画中的虚拟手指指示器兼行指示器 |
| 职责 | **手指指示器**：存储归一化坐标位置、按下状态、可见性，供 UI 层反归一化后绘制；**行指示器**：存储归一化坐标和可见性，供面板内建绘制 |
| 约束 | 使用归一化坐标，不依赖具体面板尺寸；纯数据模型；行指示器模式下 `pressed` 在 `visible` 为 `true` 时恒为 `true` |
| 所属模块 | 本文档（:ime-engine 模块）|

```kotlin
/**
 * 输入动作指示器状态（归一化坐标）。
 *
 * 兼具两种角色：
 * - **手指指示器**：用于 Row 3 键盘区域的按键手势，描述虚拟手指的位置和按压状态；
 * - **行指示器**：用于 Row 1/2 候选栏/输入列表的交互，描述圆形点击指示器的位置和可见性。
 *   作为行指示器使用时，`pressed` 在 `visible` 为 `true` 时始终为 `true`。
 *
 * 使用归一化坐标 [0,1]x[0,1]，与面板尺寸无关。
 * 纯数据模型，定义见本文档。
 */
data class InputActionFingerIndicator(
    /** 归一化坐标位置 [0,1]x[0,1] */
    val position: OffsetF,
    /**
     * 手指是否按下。
     * 仅在手指指示器角色下有语义；作为行指示器时，当 `visible` 为 `true` 则 `pressed` 始终为 `true`。
     */
    val pressed: Boolean,
    /** 指示器是否可见 */
    val visible: Boolean = true,
)
```

---

## 10. 输入动作路径插值器 (InputActionPathInterpolator)

| 属性 | 说明 |
|------|------|
| 角色 | 输入动作路径插值算法 |
| 职责 | 在起止归一化坐标点之间生成二次贝塞尔曲线插值路径，供播放器写入 touchTrailPoints |
| 约束 | 纯算法，无副作用，不依赖 UI；输入输出均为归一化坐标 |
| 所属模块 | 本文档（:ime-engine 模块）|

```kotlin
/**
 * 输入动作路径插值器。
 *
 * 在两个归一化坐标点之间生成平滑的插值路径，
 * 用于 SwipeTo 动作的滑行轨迹动画。
 * 使用二次贝塞尔曲线，控制点偏移量由 arcFactor 决定。
 * 纯算法实现，不依赖 UI 层，定义见本文档。
 */
object InputActionPathInterpolator {

    /**
     * 在起止点之间生成二次贝塞尔曲线插值路径。
     *
     * @param from 起始归一化坐标
     * @param to 目标归一化坐标
     * @param arcFactor 弧线弯曲系数，0 为直线，越大弯曲越明显
     * @param steps 插值步数，越大曲线越平滑
     * @return 插值路径点列表（归一化坐标）
     */
    fun interpolate(
        from: OffsetF,
        to: OffsetF,
        arcFactor: Float = 0.3f,
        steps: Int = 20,
    ): List<OffsetF> {
        require(steps > 1) { "steps must be > 1: $steps" }
        // 计算控制点：起止点连线的垂直方向偏移
        val midX = (from.x + to.x) / 2
        val midY = (from.y + to.y) / 2
        val dx = to.x - from.x
        val dy = to.y - from.y
        // 垂直方向偏移
        val controlX = midX - dy * arcFactor
        val controlY = midY + dx * arcFactor

        return (0..steps).map { i ->
            val t = i.toFloat() / steps
            val oneMinusT = 1f - t
            OffsetF(
                x = oneMinusT * oneMinusT * from.x + 2 * oneMinusT * t * controlX + t * t * to.x,
                y = oneMinusT * oneMinusT * from.y + 2 * oneMinusT * t * controlY + t * t * to.y,
            )
        }
    }
}
```

---

## 11. 输入动作位置解析器 (InputActionPositionResolver)

| 属性 | 说明 |
|------|------|
| 角色 | 输入动作位置解析器接口，将语义标识映射为归一化坐标 |
| 职责 | 解析按键、候选项、输入项的归一化中心坐标，供播放器定位动画元素 |
| 约束 | 接口定义见本文档（:ime-engine 模块），实现在 :ime-ui 模块（ComposeInputActionPositionResolver）；返回归一化坐标 |
| 所属模块 | 接口定义见本文档（:ime-engine 模块），实现在 :ime-ui 模块 |

```kotlin
/**
 * 输入动作位置解析器接口，返回归一化坐标。
 *
 * 将语义按键标识符和列表索引解析为归一化坐标 [0,1]x[0,1]，
 * 供播放器更新 GestureFeedbackState 的手指指示器和轨迹。
 * 接口定义见本文档，实现在 :ime-ui 模块。
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
```

---

## 12. 归一化坐标基础类型

| 属性 | 说明 |
|------|------|
| 角色 | 归一化坐标基础类型，供跨面板、跨 Zone 传递坐标数据 |
| 职责 | OffsetF 表示归一化坐标点，RectF 表示归一化矩形区域 |
| 约束 | 坐标范围 [0,1] x [0,1]，不依赖具体面板尺寸 |
| 所属模块 | 本文档（:ime-engine 模块）|

```kotlin
/**
 * 归一化浮点坐标点。
 *
 * 坐标范围 [0,1] x [0,1]，
 * (0,0) 为左上角，(1,1) 为右下角。
 * 用于在逻辑层面传递坐标数据，
 * 绘制时由各面板根据自身尺寸反归一化为像素坐标。
 */
data class OffsetF(val x: Float, val y: Float)

/**
 * 归一化矩形。
 *
 * 坐标范围 [0,1] x [0,1]，
 * 用于表示按键、候选项等元素的归一化位置区域。
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
```
