# 输入动作程序化

## 1. InputAction 动作体系

`InputAction` 是输入动作的类型基类，以 `sealed class` 表达六种坐标无关的逻辑动作：`KeyDown`（按下按键）、`SwipeTo`（滑行到目标按键）、`KeyUp`（抬起按键）、`Wait`（等待时长）、`SelectCandidate`（选择候选词）和 `SwitchKeyboard`（切换键盘类型）。`sealed class` 的穷举性保证引擎在模式匹配时编译期覆盖所有子类型，不会遗漏新增的动作类型。所有 `InputAction` 子类共享 `startTime` 字段，表达该动作在脚本中的起始时间偏移量（毫秒），为回放调度器提供精确的时间控制依据。

```kotlin
sealed class InputAction {
    abstract val startTime: Long

    data class KeyDown(
        override val startTime: Long,
        val key: InputKey,
    ) : InputAction()

    data class SwipeTo(
        override val startTime: Long,
        val fromKey: InputKey,
        val toKey: InputKey,
        val duration: Long,
    ) : InputAction()

    data class KeyUp(
        override val startTime: Long,
        val key: InputKey,
    ) : InputAction()

    data class Wait(
        override val startTime: Long,
        val duration: Long,
    ) : InputAction()

    data class SelectCandidate(
        override val startTime: Long,
        val candidateIndex: Int,
    ) : InputAction()

    data class SwitchKeyboard(
        override val startTime: Long,
        val targetType: KeyboardType,
    ) : InputAction()
}
```

`KeyDown` 表达手指按下某个按键的逻辑动作，`key` 字段标识被按下的按键。在点击模式下，`KeyDown` 与 `KeyUp` 成对出现，中间可能插入一个 `Wait` 表达按压保持时长；在滑行模式下，`KeyDown` 之后跟随一个或多个 `SwipeTo`，最终以 `KeyUp` 结束。`SwipeTo` 表达手指从 `fromKey` 滑行到 `toKey` 的逻辑动作，`duration` 字段指定滑行持续时间，用于路径插值器计算中间帧位置。`SwipeTo` 是滑行输入的核心动作——引擎的滑行识别逻辑需要连续的滑行轨迹，`SwipeTo` 提供了起点和终点信息，配合 `InputActionPathInterpolator` 生成平滑的中间路径点。

`KeyUp` 表达手指从按键抬起的逻辑动作，与 `KeyDown` 配对构成完整的按键生命周期。`Wait` 表达两个动作之间的等待间隔，`duration` 指定等待时长，用于模拟用户在连续操作之间的思考或反应时间。`SelectCandidate` 表达从候选列表中选择指定索引的候选词，`candidateIndex` 为候选词在当前候选列表中的位置索引，回放时通过 `InputActionPositionResolver.resolveCandidatePosition()` 将索引解析为屏幕坐标。`SwitchKeyboard` 表达切换到目标键盘类型的动作，`targetType` 为目标 `KeyboardType`，回放时触发 `ImeIntent.SwitchKeyboard(targetType)` 意图。

`InputAction` 的「坐标无关」设计是其核心特性：所有动作只引用逻辑标识（`InputKey`、`candidateIndex`、`KeyboardType`），不包含任何像素坐标或屏幕尺寸信息。坐标解析被推迟到回放时通过 `InputActionPositionResolver` 完成，使得同一份动作脚本可以在不同面板尺寸、不同设备密度的环境中正确回放。这种设计确保了动作脚本的可移植性——编译一次，处处回放，无需为不同设备重新编译。

---

## 2. InputActionScript 动作脚本

`InputActionScript` 将有序的 `InputAction` 序列组合为可命名、可描述、可回放的脚本。脚本是输入动作程序化的核心数据载体——编译器产出脚本，回放器消费脚本，UI 展示脚本列表供用户选择。每个脚本携带名称、描述、动作列表和总时长四个字段，其中 `totalDuration` 由编译器在构建动作列表时自动计算，等于最后一个动作的 `startTime` 加上其自身的持续时长（若为 `SwipeTo` 或 `Wait`），确保回放器可以精确控制时间轴。

```kotlin
data class InputActionScript(
    val name: String,
    val description: String,
    val actions: List<InputAction>,
    val totalDuration: Long,
)
```

`name` 字段为脚本的人类可读名称，用于 UI 层的脚本列表展示和用户识别。命名建议采用「内容 + 模式」的格式，如 `"你好-Tap"` 表达「你好」的点击输入脚本，`"中国-Swipe"` 表达「中国」的滑行输入脚本。`description` 字段为脚本的详细描述，可包含输入文本、模式类型、动作数量等元信息，用于 UI 层的脚本详情展示。`actions` 字段为有序的动作列表，按 `startTime` 升序排列。编译器保证动作列表的时间单调性——每个动作的 `startTime` 不小于前一个动作的 `startTime`，但允许多个动作共享相同的 `startTime`（如同时按下两个按键的并行操作）。

`totalDuration` 字段表达脚本的完整时长，单位为毫秒。编译器在构建动作列表时自动计算此值：遍历所有动作，取 `startTime + duration` 的最大值作为 `totalDuration`。对于 `KeyDown` 和 `KeyUp` 等瞬时动作，`duration` 视为 0；对于 `SwipeTo` 和 `Wait`，`duration` 为其声明的持续时长。回放器使用 `totalDuration` 初始化时间轴，控制回放进度条的总量和播放/暂停的状态切换。

`InputActionScript` 的不可变性确保了脚本的线程安全和可共享性——同一脚本实例可以被多个回放器同时消费，无需拷贝或同步。脚本的创建唯一路径是通过 `InputActionScriptCompiler.compile()` 编译产出，外部不应直接构造脚本实例。编译器负责保证动作列表的合法性：时间单调性、`KeyDown`/`KeyUp` 配对完整性、`SwipeTo` 的起止按键有效性等。若编译过程中检测到非法输入（如无法映射到按键的字符），编译器抛出 `IllegalArgumentException` 而非产出包含非法动作的脚本——这遵循引擎的 Fail Fast 原则。

---

## 3. InputActionMode 输入模式

`InputActionMode` 定义了动作脚本的输入交互模式，决定编译器如何将文本转换为动作序列。当前支持两种输入模式：`Tap`（逐键点击）和 `Swipe`（滑行输入）。两种模式的核心差异在于按键之间的连接方式——`Tap` 模式下每个按键独立完成「按下→抬起」的完整生命周期，相邻按键之间通过 `Wait` 间隔；`Swipe` 模式下同一拼音音节的按键通过 `SwipeTo` 连续连接，形成滑行轨迹，音节之间通过 `KeyUp` + `Wait` + `KeyDown` 间隔。

```kotlin
enum class InputActionMode {
    Tap,   // 逐键点击模式
    Swipe, // 滑行输入模式
}
```

`Tap` 模式编译策略：将文本拆分为拼音音节序列，每个音节的每个字母映射为一个 `KeyDown` + `KeyUp` 对，相邻按键之间插入 `Wait`（默认 80ms）模拟用户的手指移动时间，相邻音节之间插入更长的 `Wait`（默认 200ms）模拟音节间的思考间隔。每个音节结束后插入 `SelectCandidate`（`candidateIndex = 0`）选择首个候选词，模拟用户的默认选择行为。

`Swipe` 模式编译策略：将文本拆分为拼音音节序列，每个音节的按键序列编译为连续的滑行动作。音节的首个字母为 `KeyDown`，后续字母通过 `SwipeTo` 连接到前一个字母所在按键，音节末尾字母执行 `KeyUp` 完成滑行。`SwipeTo` 的 `duration` 根据相邻按键的距离动态计算（基础时长 100ms + 距离系数），模拟真实滑行输入的速度感。每个音节结束后插入 `SelectCandidate(0)` 选择首个候选词，相邻音节之间插入 `Wait`（默认 300ms）模拟音节间的手指抬起和重新定位。

---

## 4. InputActionScriptCompiler 脚本编译器

`InputActionScriptCompiler` 是动作脚本的编译器，将待输入文本编译为 `InputActionScript`。编译器是纯函数式组件——无副作用、无外部依赖，接收文本和模式参数，产出脚本或抛出异常。编译器的核心职责是将文本转换为按键序列和动作序列，涉及文本分词、拼音拆分、按键映射和时间计算四个步骤。编译器的入口方法为 `compile()`，根据 `InputActionMode` 分别委托给 `compileTapActions()` 或 `compileSwipeActions()` 执行具体的编译逻辑。

```kotlin
class InputActionScriptCompiler {

    fun compile(
        text: String,
        mode: InputActionMode,
    ): InputActionScript {
        val pinyinSegments = textToPinyinSegments(text)
        val actions = when (mode) {
            InputActionMode.Tap -> compileTapActions(pinyinSegments)
            InputActionMode.Swipe -> compileSwipeActions(pinyinSegments)
        }
        val totalDuration = actions.maxOfOrNull { a ->
            a.startTime + when (a) {
                is InputAction.SwipeTo -> a.duration
                is InputAction.Wait -> a.duration
                else -> 0L
            }
        } ?: 0L

        return InputActionScript(
            name = "$text-${mode.name}",
            description = "输入「$text」的${mode.name}模式脚本，共 ${actions.size} 个动作",
            actions = actions,
            totalDuration = totalDuration,
        )
    }
}
```

### 4.1 compileTapActions() 点击编译

`compileTapActions()` 将拼音音节序列编译为逐键点击的动作列表。编译过程为：遍历每个音节，将音节的每个字母映射为 `InputKey`，为每个字母生成 `KeyDown` + `KeyUp` 对，相邻字母之间插入 `Wait(TAP_KEY_INTERVAL)`，每个音节结束后插入 `SelectCandidate(0)` + `Wait(TAP_SYLLABLE_INTERVAL)`。`TAP_KEY_INTERVAL` 为同音节内按键间隔（80ms），`TAP_SYLLABLE_INTERVAL` 为音节间间隔（200ms）。

```kotlin
private fun compileTapActions(segments: List<PinyinSegment>): List<InputAction> {
    val actions = mutableListOf<InputAction>()
    var currentTime = 0L

    for ((segmentIndex, segment) in segments.withIndex()) {
        for ((charIndex, char) in segment.letters.withIndex()) {
            val key = requireKeyForChar(char)
            actions.add(InputAction.KeyDown(currentTime, key))
            currentTime += KEY_DOWN_DURATION
            actions.add(InputAction.KeyUp(currentTime, key))
            currentTime += KEY_UP_DURATION

            val isLastChar = charIndex == segment.letters.lastIndex
            if (!isLastChar) {
                actions.add(InputAction.Wait(currentTime, TAP_KEY_INTERVAL))
                currentTime += TAP_KEY_INTERVAL
            }
        }

        actions.add(InputAction.SelectCandidate(currentTime, 0))
        currentTime += CANDIDATE_SELECT_DURATION

        val isLastSegment = segmentIndex == segments.lastIndex
        if (!isLastSegment) {
            actions.add(InputAction.Wait(currentTime, TAP_SYLLABLE_INTERVAL))
            currentTime += TAP_SYLLABLE_INTERVAL
        }
    }

    return actions
}
```

`requireKeyForChar()` 将单个字符映射为 `InputKey`，若字符无法映射到任何按键（如生僻符号），立即抛出 `IllegalArgumentException`。这种 Fail Fast 行为确保编译器产出的动作列表中不包含无效按键引用，避免回放时在位置解析阶段才发现错误。

### 4.2 compileSwipeActions() 滑行编译

`compileSwipeActions()` 将拼音音节序列编译为滑行输入的动作列表。编译过程为：遍历每个音节，首字母生成 `KeyDown`，后续字母通过 `SwipeTo` 从前一个字母的按键滑行到当前字母的按键，音节末字母生成 `KeyUp`，音节结束后插入 `SelectCandidate(0)` + `Wait(SWIPE_SYLLABLE_INTERVAL)`。`SwipeTo` 的 `duration` 根据相邻按键的归一化距离动态计算——距离越远滑行时长越长。

```kotlin
private fun compileSwipeActions(segments: List<PinyinSegment>): List<InputAction> {
    val actions = mutableListOf<InputAction>()
    var currentTime = 0L

    for ((segmentIndex, segment) in segments.withIndex()) {
        val keys = segment.letters.map { requireKeyForChar(it) }

        actions.add(InputAction.KeyDown(currentTime, keys.first()))
        currentTime += KEY_DOWN_DURATION

        for (i in 1 until keys.size) {
            val fromKey = keys[i - 1]
            val toKey = keys[i]
            val duration = calculateSwipeDuration(fromKey, toKey)
            actions.add(InputAction.SwipeTo(currentTime, fromKey, toKey, duration))
            currentTime += duration
        }

        actions.add(InputAction.KeyUp(currentTime, keys.last()))
        currentTime += KEY_UP_DURATION

        actions.add(InputAction.SelectCandidate(currentTime, 0))
        currentTime += CANDIDATE_SELECT_DURATION

        val isLastSegment = segmentIndex == segments.lastIndex
        if (!isLastSegment) {
            actions.add(InputAction.Wait(currentTime, SWIPE_SYLLABLE_INTERVAL))
            currentTime += SWIPE_SYLLABLE_INTERVAL
        }
    }

    return actions
}

private fun calculateSwipeDuration(fromKey: InputKey, toKey: InputKey): Long {
    val distance = estimateKeyDistance(fromKey, toKey)
    return (SWIPE_BASE_DURATION + (distance * SWIPE_DISTANCE_FACTOR).toLong())
        .coerceIn(SWIPE_MIN_DURATION, SWIPE_MAX_DURATION)
}
```

`calculateSwipeDuration()` 根据 `fromKey` 和 `toKey` 的估算归一化距离计算滑行时长。`SWIPE_BASE_DURATION` 为基础滑行时长（100ms），`SWIPE_DISTANCE_FACTOR` 为距离系数（50ms/单位距离），`SWIPE_MIN_DURATION` 和 `SWIPE_MAX_DURATION` 为时长上下界（60ms ~ 300ms）。

### 4.3 textToPinyinSegments() 文本拆分

`textToPinyinSegments()` 将输入文本拆分为拼音音节序列，是编译器的前置步骤。拆分逻辑区分中文文本和拉丁文本：中文文本逐字拆分为独立的音节（每字对应一个拼音），拉丁文本按字母拆分为单字母音节。拆分结果为 `List<PinyinSegment>`，每个 `PinyinSegment` 包含原始文本和对应的拼音字母列表。

```kotlin
data class PinyinSegment(
    val originalText: String,
    val letters: List<String>,
)

private fun textToPinyinSegments(text: String): List<PinyinSegment> {
    val segments = mutableListOf<PinyinSegment>()

    for (char in text) {
        when {
            char.isCJKCharacter() -> {
                val pinyin = pinyinForChar(char)
                    ?: throw IllegalArgumentException(
                        "无法将字符「$char」转换为拼音"
                    )
                segments.add(PinyinSegment(
                    originalText = char.toString(),
                    letters = pinyin.toList().map { it.toString() },
                ))
            }
            char.isLetter() && char.code < 128 -> {
                segments.add(PinyinSegment(
                    originalText = char.toString(),
                    letters = listOf(char.lowercaseChar().toString()),
                ))
            }
            else -> {
                throw IllegalArgumentException(
                    "不支持的字符「$char」，仅支持中文和拉丁字母"
                )
            }
        }
    }

    return segments
}
```

---

## 5. InputActionFingerIndicator 手指指示器

`InputActionFingerIndicator` 是回放过程中的手指位置指示器，为用户提供直观的视觉反馈——显示一个虚拟手指在键盘面板上的移动轨迹，帮助用户理解动作脚本的执行过程和输入节奏。指示器的核心参数包括位置（`position`）、按压状态（`pressed`）、可见性（`visible`）和点击动画（`clickAnimation`），四个参数共同驱动指示器的渲染状态和视觉表现。

```kotlin
data class InputActionFingerIndicator(
    val position: OffsetF = OffsetF.Zero,
    val pressed: Boolean = false,
    val visible: Boolean = false,
    val clickAnimation: ClickAnimation = ClickAnimation.None,
) {
    enum class ClickAnimation {
        None,       // 无动画
        Pressing,   // 按下动画（放大 + 透明度降低）
        Releasing,  // 抬起动画（缩小 + 透明度恢复）
    }
}
```

`position` 字段为指示器在面板归一化坐标系中的当前位置，类型为 `OffsetF`，由回放器根据当前动作和 `InputActionPathInterpolator` 的插值结果实时更新。`pressed` 字段表达手指的按压状态——`KeyDown` 时设为 `true`，`KeyUp` 时设为 `false`，影响指示器的视觉大小和透明度。`visible` 字段控制指示器的可见性，由回放器的生命周期管理。`clickAnimation` 字段驱动按键点击的微交互动画——`Pressing` 时执行按下缩放动画，`Releasing` 时执行抬起恢复动画。

---

## 6. InputActionPathInterpolator 路径插值

`InputActionPathInterpolator` 是路径插值的纯算法组件，为 `SwipeTo` 动作提供平滑的中间路径点计算。插值器采用二次 Bézier 曲线算法，以起点和终点为曲线的两端控制点，以起点和终点连线的中垂线偏移点为中间控制点，生成自然弯曲的滑行轨迹。纯算法设计意味着插值器不持有任何可变状态，不依赖任何外部服务——输入起点、终点和进度参数，输出插值位置，完全确定性的纯函数行为。

```kotlin
object InputActionPathInterpolator {

    /**
     * 二次 Bézier 曲线插值。
     *
     * @param from 起点坐标
     * @param to   终点坐标
     * @param t    插值进度 [0.0, 1.0]
     * @return 插值位置
     */
    fun interpolate(from: OffsetF, to: OffsetF, t: Float): OffsetF {
        val control = computeControlPoint(from, to)
        val oneMinusT = 1.0f - t
        val x = oneMinusT * oneMinusT * from.x + 2.0f * oneMinusT * t * control.x + t * t * to.x
        val y = oneMinusT * oneMinusT * from.y + 2.0f * oneMinusT * t * control.y + t * t * to.y
        return OffsetF(x, y)
    }

    /**
     * 计算二次 Bézier 曲线的中间控制点。
     * 控制点位于起点-终点连线的中垂线上，向连线法线方向偏移。
     * 偏移量与连线长度成正比，模拟自然滑行时的弧线轨迹。
     */
    private fun computeControlPoint(from: OffsetF, to: OffsetF): OffsetF {
        val midX = (from.x + to.x) / 2.0f
        val midY = (from.y + to.y) / 2.0f
        val dx = to.x - from.x
        val dy = to.y - from.y
        val distance = kotlin.math.sqrt(dx * dx + dy * dy)
        val curvature = distance * CURVATURE_FACTOR
        // 法线方向（逆时针旋转 90 度）
        val nx = -dy / distance
        val ny = dx / distance
        return OffsetF(midX + nx * curvature, midY + ny * curvature)
    }

    private const val CURVATURE_FACTOR = 0.15f
}
```

二次 Bézier 曲线的参数方程为 `B(t) = (1-t)²·P0 + 2(1-t)t·P1 + t²·P2`。`CURVATURE_FACTOR` 控制弧线的弯曲程度——0.15 表示控制点偏移量为连线长度的 15%，产生轻微但可感知的弧形轨迹。

---

## 7. InputActionPositionResolver 位置解析接口

`InputActionPositionResolver` 是位置解析的抽象接口，将逻辑标识（`InputKey`、候选索引、输入项索引）解析为归一化坐标 `OffsetF`。接口的抽象设计将坐标解析与动作逻辑彻底分离——编译器产出只包含逻辑标识的动作脚本，回放器通过注入的 `InputActionPositionResolver` 实现在运行时解析坐标。这种分离使得同一份动作脚本可以在不同面板布局、不同按键尺寸、不同屏幕密度的环境中正确回放，只需替换解析器实现即可适配新的布局参数。

```kotlin
interface InputActionPositionResolver {
    /**
     * 将按键解析为归一化坐标。
     * 返回按键中心点在面板归一化坐标系中的位置，若按键不存在则返回 null。
     */
    fun resolve(key: InputKey): OffsetF?

    /**
     * 将候选索引解析为归一化坐标。
     * 返回候选词中心点在面板归一化坐标系中的位置，若索引越界则返回 null。
     */
    fun resolveCandidatePosition(index: Int): OffsetF?

    /**
     * 将输入项索引解析为归一化坐标。
     * 返回输入项中心点在面板归一化坐标系中的位置，若索引越界则返回 null。
     */
    fun resolveInputItemPosition(index: Int): OffsetF?
}
```

`resolve(key: InputKey)` 是最核心的解析方法，将 `InputKey` 解析为面板归一化坐标系中的中心点位置。`resolveCandidatePosition(index: Int)` 将候选列表中的索引解析为归一化坐标，用于 `SelectCandidate` 动作的手指指示器定位。`resolveInputItemPosition(index: Int)` 将输入列表中的索引解析为归一化坐标，用于辅助回放场景中输入项的高亮定位。

---

## 8. 归一化坐标类型

`OffsetF` 和 `RectF` 是输入动作子系统的归一化坐标基础类型，为动作脚本提供与设备无关的坐标表达。归一化坐标将面板的宽度和高度映射到 `[0, 1]` 区间，`x = 0` 为面板左边缘，`x = 1` 为面板右边缘，`y = 0` 为面板顶边缘，`y = 1` 为面板底边缘。归一化设计的核心优势在于坐标的设备无关性——同一组归一化坐标在不同面板尺寸和像素密度下通过简单的乘法即可转换为屏幕像素坐标，无需维护多套坐标数据。

```kotlin
data class OffsetF(
    val x: Float,
    val y: Float,
) {
    companion object {
        val Zero = OffsetF(0.0f, 0.0f)
    }
}

data class RectF(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top
    val centerX: Float get() = (left + right) / 2.0f
    val centerY: Float get() = (top + bottom) / 2.0f
    val center: OffsetF get() = OffsetF(centerX, centerY)

    /** 判断点是否在矩形内 */
    fun contains(offset: OffsetF): Boolean =
        offset.x in left..right && offset.y in top..bottom

    companion object {
        val Zero = RectF(0.0f, 0.0f, 0.0f, 0.0f)
    }
}
```

`OffsetF` 是二维归一化点的不可变数据类，包含 `x` 和 `y` 两个 `Float` 字段。`OffsetF.Zero` 为原点常量。`OffsetF` 的值域理论上为 `[0, 1]`，但实现不强制 clamp——允许出现轻微越界的坐标，由消费方根据实际需求处理越界情况。

`RectF` 是二维归一化矩形的不可变数据类，以 `left`、`top`、`right`、`bottom` 四个 `Float` 字段表达矩形的边界。`RectF` 派生 `width`、`height`、`centerX`、`centerY` 和 `center` 五个计算属性，提供矩形的几何特征访问。`contains()` 方法判断一个 `OffsetF` 点是否落在矩形内部，用于命中测试。`RectF.Zero` 为零面积矩形常量。

归一化坐标到屏幕像素坐标的转换由 UI 层在渲染时完成：`screenX = offsetF.x * panelWidth`，`screenY = offsetF.y * panelHeight`。这种转换是单向的——引擎层只产出归一化坐标，UI 层负责转换为屏幕坐标。

---

## 9. InputActionPlayer 动画帧驱动机制

`InputActionPlayer` 是输入动作播放的核心控制器，负责加载脚本、驱动回放、解析位置、写入视觉反馈状态并发射 `ImeIntent`。播放器的动画循环采用帧驱动机制——基于 `withFrameNanos` 与 Choreographer 同步，而非传统的 `delay()` 定时器，确保动画进度与屏幕刷新率精确对齐。

### 9.1 帧定时器 FrameTimer

```kotlin
/**
 * 帧定时器：使用 withFrameNanos 驱动动画帧循环，
 * 确保与屏幕刷新率同步，避免 delay() 的定时误差。
 */
class FrameTimer(private val scope: CoroutineScope) {
    private var startNanos: Long = 0L
    private var lastFrameNanos: Long = 0L
    private var paused = false

    /**
     * 启动帧循环。
     * @param onFrame 每帧回调，接收当前进度 [0f..1f]
     * @param durationMs 动画总时长（毫秒）
     * @param onComplete 动画完成回调
     */
    fun start(
        durationMs: Long,
        onFrame: (progress: Float) -> Unit,
        onComplete: () -> Unit,
    ) {
        scope.launch {
            startNanos = System.nanoTime()
            lastFrameNanos = startNanos
            val durationNanos = durationMs * 1_000_000L

            while (true) {
                withFrameNanos { frameTimeNanos ->
                    if (paused) return@withFrameNanos
                    
                    val elapsed = frameTimeNanos - startNanos
                    val progress = (elapsed.toFloat() / durationNanos).coerceIn(0f, 1f)
                    
                    onFrame(progress)
                    lastFrameNanos = frameTimeNanos

                    if (progress >= 1f) {
                        onComplete()
                        return@withFrameNanos
                    }
                }
            }
        }
    }

    fun pause() { paused = true }
    fun resume() { paused = false }
}
```

`FrameTimer` 封装了基于 `withFrameNanos` 的帧循环逻辑。`start()` 启动一个协程，在无限循环中调用 `withFrameNanos` 挂起函数——每次挂起直到下一帧信号到达（由 Compose 的 Choreographer 驱动）。帧信号到达后，计算当前进度 `elapsed / durationNanos` 并回调 `onFrame`。`paused` 标志控制暂停/恢复：暂停时 `withFrameNanos` 仍会挂起等待下一帧，但回调内部直接返回，不更新进度，因此动画时钟暂停而不会出现跳帧。

### 9.2 播放器 play() 的帧驱动行为

播放器使用 `FrameTimer` 驱动帧循环，每帧通过 `withFrameNanos` 与 Choreographer 同步。帧间隔与屏幕刷新率一致（通常 16.6ms @60fps）。插值点实时计算而非预分配——每帧根据当前进度计算手指位置，避免大量对象的预分配。帧回调中直接写入 `GestureFeedbackState` 的 StateFlow，确保反馈绘制与渲染管线对齐。

`play()` 在开始播放时创建 `FrameTimer` 实例并调用 `start()`，传入脚本中当前动作的持续时长作为 `durationMs`。`onFrame` 回调读取当前进度，调用 `InputActionPathInterpolator.interpolate()` 实时计算插值位置，然后立即写入 `feedbackState` 的 `fingerIndicator` 和 `touchTrailPoints`。`onComplete` 回调推进到下一个动作——若动作列表未完成则递归调用 `play()`，否则切换状态为 `Finished`。

### 9.3 背压与帧跳过

当系统负载导致帧回调延迟时（如连续两帧间隔超过 32ms），`FrameTimer` 自动跳过中间状态——`onFrame` 仅以最新进度调用一次，不累积过期帧。这确保动画始终追赶上最新进度，不会因卡顿而累积延迟。

帧跳过的工作原理：`withFrameNanos` 返回的 `frameTimeNanos` 永远是最新的帧时间戳。即便前一帧因 UI 线程繁忙而被跳过，`frameTimeNanos` 仍然是当前帧的实际时间，`elapsed = frameTimeNanos - startNanos` 计算出的进度已经包含了被跳过的帧时间。因此动画进度总是基于真实时间而非帧计数，不会因丢帧而滞后于真实时间。

这种设计使得播放器在低端设备上也能够保持动画的同步性——可能帧率降低（跳过的帧不渲染），但动画的最终总时长始终精确等于 `durationMs`，与设备帧率无关。
