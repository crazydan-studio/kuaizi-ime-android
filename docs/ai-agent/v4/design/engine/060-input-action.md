# 输入动作程序化

## 1. 概述

输入动作程序化是通过指定待输入的字符序列，自动以动画模拟滑行或点击等方式完成输入过程的能力。核心思路是：将用户真实的手指操作抽象为可序列化的「动作脚本」，由播放引擎按脚本驱动键盘状态机和 UI 动画，产生与真人操作一致的视觉效果。

**坐标无关设计**：动作脚本只记录按键的语义标识（如 `InputKey`），不存储任何绝对坐标。回放时，播放器根据当前键盘状态动态查找按键的实时位置，从而消除按键布局变更、屏幕尺寸变化、手模式切换等因素导致的回放失效问题。

> 本文档仅涵盖 `:ime-engine` 模块中的核心数据模型和编译器。播放器（`InputActionPlayer`）、动画引擎和 UI 覆盖层（`FingerOverlay`、`SwipeTrailOverlay`、`KeyHighlightOverlay`）属于 `:ime-ui` / `:app` 模块，不在本文档范围内。

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

## 3. ActionScript 动作脚本

```kotlin
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

## 6. ActionScriptCompiler 脚本编译器

将待输入字符序列编译为逻辑动作序列。编译器仅需要知道键盘的按键表（哪些键存在），不需要知道按键的像素位置：

```kotlin
/**
 * 动作脚本编译器，坐标无关。
 *
 * 根据待输入文本和输入方式，生成逻辑动作序列。
 * 编译过程只查询键盘的按键表（哪些键存在），不涉及坐标计算。
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
