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

`Tap` 模式编译策略：将文本拆分为拼音音节序列，每个音节的每个字母映射为一个 `KeyDown` + `KeyUp` 对，相邻按键之间插入 `Wait`（默认 80ms）模拟用户的手指移动时间，相邻音节之间插入更长的 `Wait`（默认 200ms）模拟音节间的思考间隔。每个音节结束后插入 `SelectCandidate`（`candidateIndex = 0`）选择首个候选词，模拟用户的默认选择行为。`Tap` 模式的动作序列结构为：`KeyDown(k1) → KeyUp(k1) → Wait(80) → KeyDown(k2) → KeyUp(k2) → Wait(80) → ... → SelectCandidate(0) → Wait(200) → ...`，清晰可读，便于调试和验证。

`Swipe` 模式编译策略：将文本拆分为拼音音节序列，每个音节的按键序列编译为连续的滑行动作。音节的首个字母为 `KeyDown`，后续字母通过 `SwipeTo` 连接到前一个字母所在按键，音节末尾字母执行 `KeyUp` 完成滑行。`SwipeTo` 的 `duration` 根据相邻按键的距离动态计算（基础时长 100ms + 距离系数），模拟真实滑行输入的速度感。每个音节结束后插入 `SelectCandidate(0)` 选择首个候选词，相邻音节之间插入 `Wait`（默认 300ms）模拟音节间的手指抬起和重新定位。`Swipe` 模式的动作序列结构为：`KeyDown(k1) → SwipeTo(k1, k2, d1) → SwipeTo(k2, k3, d2) → KeyUp(k3) → SelectCandidate(0) → Wait(300) → KeyDown(k4) → ...`，紧凑连贯，还原真实滑行输入的流畅感。

两种模式的选择影响回放时的视觉效果和手指指示器行为：`Tap` 模式下手指指示器在每个按键位置短暂出现后消失，呈现逐键点击的节奏感；`Swipe` 模式下手指指示器沿 Bézier 曲线平滑移动，呈现滑行输入的流畅感。用户通过 `ImeConfig.ui.practicePlaybackSpeed` 控制回放速度倍率，两种模式均受此配置影响——倍率作用于所有 `Wait` 的 `duration` 和 `SwipeTo` 的 `duration`，但不影响 `KeyDown`/`KeyUp` 的瞬时动作。

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

`compileTapActions()` 将拼音音节序列编译为逐键点击的动作列表。编译过程为：遍历每个音节，将音节的每个字母映射为 `InputKey`，为每个字母生成 `KeyDown` + `KeyUp` 对，相邻字母之间插入 `Wait(TAP_KEY_INTERVAL)`，每个音节结束后插入 `SelectCandidate(0)` + `Wait(TAP_SYLLABLE_INTERVAL)`。`TAP_KEY_INTERVAL` 为同音节内按键间隔（80ms），`TAP_SYLLABLE_INTERVAL` 为音节间间隔（200ms）。时间轴通过累加各动作的 `startTime` 精确计算，确保回放时的时间节拍与真实点击节奏一致。

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

`compileSwipeActions()` 将拼音音节序列编译为滑行输入的动作列表。编译过程为：遍历每个音节，首字母生成 `KeyDown`，后续字母通过 `SwipeTo` 从前一个字母的按键滑行到当前字母的按键，音节末字母生成 `KeyUp`，音节结束后插入 `SelectCandidate(0)` + `Wait(SWIPE_SYLLABLE_INTERVAL)`。`SwipeTo` 的 `duration` 根据相邻按键的归一化距离动态计算——距离越远滑行时长越长，模拟真实滑行输入中手指移动的物理规律。

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

`calculateSwipeDuration()` 根据 `fromKey` 和 `toKey` 的估算归一化距离计算滑行时长。`SWIPE_BASE_DURATION` 为基础滑行时长（100ms），`SWIPE_DISTANCE_FACTOR` 为距离系数（50ms/单位距离），`SWIPE_MIN_DURATION` 和 `SWIPE_MAX_DURATION` 为时长上下界（60ms ~ 300ms），确保滑行速度在合理范围内。距离估算通过 `estimateKeyDistance()` 实现——该方法根据两个 `InputKey` 在标准键盘布局中的相对位置计算归一化距离，不依赖实际屏幕尺寸。

### 4.3 textToPinyinSegments() 文本拆分

`textToPinyinSegments()` 将输入文本拆分为拼音音节序列，是编译器的前置步骤。拆分逻辑区分中文文本和拉丁文本：中文文本逐字拆分为独立的音节（每字对应一个拼音），拉丁文本按空格和标点拆分为单词序列。拆分结果为 `List<PinyinSegment>`，每个 `PinyinSegment` 包含原始文本和对应的拼音字母列表。

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

`textToPinyinSegments()` 的拆分策略为逐字符处理：中文字符通过 `pinyinForChar()` 查询拼音映射表获取拼音拼写，将拼音拆分为字母序列（如 `"zhong"` → `["z", "h", "o", "n", "g"]`）；拉丁字母直接作为单字母音节。不支持的字符合并处理——数字、标点符号、空格等非输入字符导致编译器抛出 `IllegalArgumentException`，因为动作脚本的设计目标是演示文字输入过程，非文字字符不属于输入动作的范畴。`isCJKCharacter()` 通过 Unicode 范围判断字符是否为中日韩统一表意文字，覆盖常用汉字和扩展 A/B 区的生僻字。

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

`position` 字段为指示器在面板归一化坐标系中的当前位置，类型为 `OffsetF`，由回放器根据当前动作和 `InputActionPathInterpolator` 的插值结果实时更新。在 `KeyDown` 和 `KeyUp` 动作期间，`position` 定位在目标按键的中心；在 `SwipeTo` 动作期间，`position` 沿 Bézier 曲线从 `fromKey` 平滑移动到 `toKey`；在 `Wait` 动作期间，`position` 保持在最后一个按键位置不变。`pressed` 字段表达手指的按压状态——`KeyDown` 时设为 `true`，`KeyUp` 时设为 `false`，影响指示器的视觉大小和透明度：按压时指示器放大并降低透明度，抬起时恢复原始大小和完全透明度。

`visible` 字段控制指示器的可见性，由回放器的生命周期管理：回放开始时设为 `true`，回放结束或暂停时设为 `false`。`visible` 为 `false` 时指示器不参与渲染，避免在非回放状态下显示无意义的视觉元素。`visible` 的切换伴随淡入淡出动画（200ms），确保指示器的出现和消失不突兀。`clickAnimation` 字段驱动按键点击的微交互动画——`Pressing` 时指示器执行按下缩放动画（从 1.0x 缩放到 1.2x，同时透明度从 1.0 降到 0.7），`Releasing` 时执行抬起恢复动画（从 1.2x 恢复到 1.0x，透明度从 0.7 恢复到 1.0），动画时长为 100ms。

指示器的渲染由 UI 层的 Compose 组件实现，根据 `InputActionFingerIndicator` 的状态驱动重组。指示器的外观设计为半透明的圆形覆盖层，中心为高亮点，外围为扩散光晕，模拟手指触屏的视觉效果。指示器的 Z 轴层级高于按键面板但低于候选栏和工具栏，确保指示器不遮挡关键 UI 元素。`ImeConfig.ui.practiceShowFingerOverlay` 配置项控制指示器的整体显示开关——当该配置为 `false` 时，回放器不更新指示器状态，UI 层不渲染指示器组件，节省渲染开销。

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

二次 Bézier 曲线的参数方程为 `B(t) = (1-t)²·P0 + 2(1-t)t·P1 + t²·P2`，其中 `P0` 为起点、`P1` 为控制点、`P2` 为终点、`t` 为进度参数 `[0, 1]`。`interpolate()` 方法接收起点 `from`、终点 `to` 和进度 `t`，返回曲线在参数 `t` 处的插值位置。当 `t = 0` 时返回起点，`t = 1` 时返回终点，`t ∈ (0, 1)` 时返回曲线上的中间点。`computeControlPoint()` 方法计算中间控制点——取起点和终点连线的中点，沿法线方向偏移一定距离，偏移量与连线长度成正比（`CURVATURE_FACTOR = 0.15`），生成自然弯曲的弧线轨迹。法线方向取连线的逆时针旋转 90 度方向，确保弧线方向的一致性。

`CURVATURE_FACTOR` 控制弧线的弯曲程度——0.15 表示控制点偏移量为连线长度的 15%，产生轻微但可感知的弧形轨迹，模拟真实滑行输入中手指的非直线运动。较大的因子产生更弯曲的轨迹（更接近 U 形），较小的因子产生更接近直线的轨迹。0.15 的取值在视觉自然性和路径紧凑性之间取得平衡：弧线足够明显以至于看起来不像直线插值，但不会弯曲到偏离按键区域。

插值器的使用场景不仅限于手指指示器的位置更新，还包括滑行轨迹的渲染——UI 层在 `SwipeTo` 动作回放时，以固定间隔（如每 16ms）调用 `interpolate()` 采样轨迹点，将采样点连接为平滑的曲线绘制在面板上。轨迹的渐变色和宽度随 `t` 变化——起点处宽而透明，终点处窄而不透明，模拟手指滑行的速度感和方向感。`ImeConfig.ui.practiceShowSwipeTrail` 配置项控制轨迹的显示开关。

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

`resolve(key: InputKey)` 是最核心的解析方法，将 `InputKey` 解析为面板归一化坐标系中的中心点位置。解析过程由实现类根据当前键盘布局完成——实现类持有按键布局数据（按键行列索引、按键尺寸、面板边距等），通过几何计算将逻辑按键映射为归一化坐标。`InputKey` 是引擎中按键的统一标识，包含按键的语义类型（字符键、功能键、删除键等）和显示文本。实现类根据 `InputKey` 的类型和文本，在当前布局中查找对应的几何位置，返回按键中心点的归一化坐标。若 `InputKey` 在当前布局中不存在（如切换键盘后原按键不可见），返回 `null`，回放器据此跳过该动作或抛出异常。

`resolveCandidatePosition(index: Int)` 将候选列表中的索引解析为归一化坐标，用于 `SelectCandidate` 动作的手指指示器定位。候选词的布局取决于 UI 层的候选栏实现——横向滚动、纵向列表或网格排列，实现类需根据当前候选栏的布局参数计算指定索引候选词的中心位置。若索引超出当前候选列表范围（如翻页后候选数量变化），返回 `null`。`resolveInputItemPosition(index: Int)` 将输入列表中的索引解析为归一化坐标，用于辅助回放场景中输入项的高亮定位。

位置解析器的实现由 UI 层提供，注入到回放器中使用。UI 层拥有完整的布局信息（按键位置、候选栏参数、面板尺寸），是位置解析的天然实现者。引擎层不依赖具体的解析器实现——引擎的编译器只产出逻辑动作，引擎的回放器通过接口消费坐标。这种接口抽象确保了引擎与 UI 的解耦，符合引擎「逻辑与 UI 分离」的架构原则。

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

`OffsetF` 是二维归一化点的不可变数据类，包含 `x` 和 `y` 两个 `Float` 字段。`OffsetF.Zero` 为原点常量，用于初始化和默认值。`OffsetF` 的设计简洁而精确——不包含任何业务逻辑，仅作为坐标值的载体。在动作子系统中，`OffsetF` 广泛用于表达按键中心位置、手指指示器当前位置、路径插值器的输入输出坐标等。`OffsetF` 的值域理论上为 `[0, 1]`，但实现不强制 clamp——允许出现轻微越界的坐标（如面板边距为负时的负坐标），由消费方根据实际需求处理越界情况。

`RectF` 是二维归一化矩形的不可变数据类，以 `left`、`top`、`right`、`bottom` 四个 `Float` 字段表达矩形的边界。`RectF` 派生 `width`、`height`、`centerX`、`centerY` 和 `center` 五个计算属性，提供矩形的几何特征访问。`contains()` 方法判断一个 `OffsetF` 点是否落在矩形内部，用于命中测试——判断手指指示器的位置是否在某个按键的区域内。`RectF.Zero` 为零面积矩形常量，用于初始化和默认值。`RectF` 在动作子系统中的主要用途是表达按键的归一化边界框——`InputActionPositionResolver` 的实现类可以使用 `RectF` 描述按键的归一化区域，回放器据此判断手指指示器与按键的重叠关系。

归一化坐标到屏幕像素坐标的转换由 UI 层在渲染时完成：`screenX = offsetF.x * panelWidth`，`screenY = offsetF.y * panelHeight`，其中 `panelWidth` 和 `panelHeight` 为面板的实际像素尺寸。这种转换是单向的——引擎层只产出归一化坐标，UI 层负责转换为屏幕坐标。反向转换（屏幕坐标→归一化坐标）在触摸事件处理时使用：UI 层将触摸事件的屏幕坐标归一化后传递给引擎的手势识别器，确保手势识别逻辑与屏幕尺寸无关。
