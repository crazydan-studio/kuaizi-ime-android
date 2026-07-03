# 交互反馈设计

## 1. 设计边界

交互反馈（音效、触觉、按键弹出提示）的共同特征是「fire-and-forget」——与引擎的逻辑状态无关，UI 层可以直接根据用户手势决定是否触发，不需要引擎协调或状态回滚。在新的架构中，反馈职责按以下边界划分：

- **Engine** 仅通过 `ImeEffect` 发射**领域事件型**的 `PopupTip`（如"已收藏"、"可粘贴内容"、"已切换到拉丁键盘"），这些是引擎内部业务逻辑产生的一次性通知，UI 层无法自行推导。
- **UI 层** 在 `gestureToIntent()` 中直接处理音效、触觉和按键弹出提示（key popup tips），因为这些反馈完全由用户手势本身决定，不需要引擎的业务判断。

这种边界划分基于以下考量：

**语义差异**：引擎的 `ImeEffect` 承载的是「引擎通知 UI 发生了某件事」——收藏确认、剪贴板检测、键盘切换提示。这些是引擎业务逻辑的副产品，UI 层无法预测。而按键音、触觉振动、按键弹出提示是用户手势的直接伴随反馈，UI 层在手势发生时即可独立决定，无需等待引擎的 reduce 结果。

**架构分层原则**：设计的状态频率分层明确指出「引擎只拥有逻辑状态，UI 拥有展示状态和交互反馈状态」。音效、触觉和按键弹出提示属于交互反馈，完全由 UI 层管理其播放策略和平台实现。引擎级别的反馈（`PopupTip`）是领域事件，属于引擎的业务逻辑输出。

**性能考量**：按键音和触觉在快速连续打字时需要低延迟触发。UI 层在 `gestureToIntent()` 中同步处理反馈避免了 engine → SharedFlow → collect 的异步路径延迟，确保反馈与手势同步。

---

## 2. AudioType 与 HapticType（:ui 模块）

### 2.1 AudioType 音效类型

`AudioType` 枚举定义在 `:ui` 模块中，覆盖所有音效场景。每种音效类型对应一段独立的音频资源文件，由 `AndroidAudioPlayer` 在初始化时加载到 `SoundPool` 中。

```kotlin
package org.crazydan.studio.ime.ui.feedback

enum class AudioType {
    /** 按键音：用户点击按键时播放 */
    KeyPress,

    /** 候选选择音：用户从候选列表选择候选词时播放 */
    CandidateSelect,

    /** 滑行输入音：用户滑行输入识别完成时播放 */
    Slip,

    /** 翻页音：候选列表翻页时播放 */
    PageFlip,
}
```

`KeyPress` 是最高频的音效类型，每次按键操作都会触发。`CandidateSelect` 在用户从候选列表选择候选词时触发。`Slip` 在滑行输入识别完成时触发。`PageFlip` 在候选列表翻页时触发。

### 2.2 HapticType 触觉类型

`HapticType` 枚举定义在 `:ui` 模块中，覆盖所有触觉反馈场景。每种触觉类型对应一种振动模式，由 `AndroidHapticPlayer` 通过 Android `Vibrator` 服务执行。

```kotlin
package org.crazydan.studio.ime.ui.feedback

enum class HapticType {
    /** 轻触反馈：按键点击、候选选择等轻量操作 */
    LightTap,

    /** 中等反馈：滑行识别完成、翻页等中等力度操作 */
    MediumTap,

    /** 重触反馈：长按触发上下文菜单等强调操作 */
    HeavyTap,
}
```

`LightTap` 对应短时轻振（约 20ms），用于最常见的按键点击和候选选择场景。`MediumTap` 对应中等时长的振动（约 50ms），用于滑行识别完成、翻页等需要稍强反馈的场景。`HeavyTap` 对应较长时长的强振（约 100ms），用于长按触发上下文菜单等需要明确感知的强调操作。

### 2.3 归属理由

`AudioType` 和 `HapticType` 定义在 `:ui` 而非 `:engine`，理由如下：

**架构分层**：音频播放和触觉振动属于 UI 层的交互反馈，不属于引擎的核心逻辑。引擎只通过 `ImeEffect` 发射领域事件型的 `PopupTip`，不持有任何反馈类型定义。UI 层在 `gestureToIntent()` 中根据手势类型自行决定播放何种音效和触觉。

**第三方友好性**：定义在 `:ui` 中，第三方引入 `:engine` + `:ui` 即可获得开箱即用的感官反馈能力。如果定义在 `:engine` 中，第三方仅引入引擎时被迫依赖反馈类型枚举，增加了不必要的耦合。

**配置检查归属**：`audioFeedbackEnabled` 和 `hapticFeedbackEnabled` 是 `ImeConfig.UiConfig` 的字段，属于 UI 配置。由 UI 层在执行 `gestureToIntent()` 时检查这些配置比引擎检查更合理——引擎不需要理解 UI 配置的语义。

---

## 3. Engine 的 ImeEffect 定义

Engine 的 `ImeEffect` 密封类现在仅承载领域事件型的 `PopupTip`，不再包含 `PlayAudio` 和 `PlayHaptic`：

```kotlin
sealed class ImeEffect {
    sealed class PopupTip : ImeEffect() {
        data class Message(
            val message: String,
            val timeoutMs: Long = 3000L,
        ) : PopupTip()

        data class Action(
            val message: String,
            val actionLabel: String,
            val action: ImeIntent,
            val persistent: Boolean = false,
            val timeoutMs: Long = 5000L,
        ) : PopupTip()
    }
}
```

`ImeEffect` 的设计体现了新的架构边界：引擎只发射 UI 层无法自行推导的领域事件，交互反馈完全由 UI 层自主决定。

---

## 4. UI 层反馈处理

### 4.1 gestureToIntent() 中的反馈处理

`KeyboardViewModel.gestureToIntent()` 在将手势转换为引擎意图的同时，直接同步处理音效和触觉反馈。这是因为反馈完全由手势类型决定，无需等待引擎的 reduce 结果。

```kotlin
private fun gestureToIntent(gesture: InputGesture): ImeIntent {
    // 根据手势类型决定交互反馈
    when (gesture) {
        is InputGesture.Tap -> {
            playAudio(AudioType.KeyPress)
            playHaptic(HapticType.LightTap)
        }
        is InputGesture.LongPress -> {
            playHaptic(HapticType.HeavyTap)
            // 长按不播放音效
        }
        is InputGesture.Swipe -> {
            playAudio(AudioType.Slip)
            playHaptic(HapticType.MediumTap)
        }
        is InputGesture.Flip -> {
            playAudio(AudioType.PageFlip)
            playHaptic(HapticType.MediumTap)
        }
        is InputGesture.CandidateTap -> {
            playAudio(AudioType.CandidateSelect)
            playHaptic(HapticType.LightTap)
        }
    }

    return when (gesture) {
        is InputGesture.Tap -> ImeIntent.PressKey(gesture.key, KeyGesture.Tap)
        is InputGesture.LongPress -> ImeIntent.PressKey(gesture.key, KeyGesture.LongPress)
        is InputGesture.Swipe -> ImeIntent.PressKey(gesture.endKey, KeyGesture.Swipe)
        is InputGesture.Flip -> ImeIntent.PressKey(
            gesture.startKey,
            KeyGesture.Flip(gesture.direction),
        )
        is InputGesture.CandidateTap -> ImeIntent.SelectCandidate(
            /* 根据 gesture.candidateIndex 从当前候选列表中获取 */
        )
    }
}

private fun playAudio(type: AudioType) {
    if (config.ui.audioFeedbackEnabled) {
        audioPlayer?.play(type)
    }
}

private fun playHaptic(type: HapticType) {
    if (config.ui.hapticFeedbackEnabled) {
        hapticPlayer?.play(type)
    }
}
```

反馈映射关系如下：

| `InputGesture` | `AudioType` | `HapticType` | 说明 |
|---|---|---|---|
| `Tap` | `KeyPress` | `LightTap` | 逐键点击，音效和触觉同时触发 |
| `Swipe` | `Slip` | `MediumTap` | 滑行输入识别完成时触发 |
| `CandidateTap` | `CandidateSelect` | `LightTap` | 选择候选词时触发 |
| `Flip` | `PageFlip` | `MediumTap` | 翻页时触发 |
| `LongPress` | 无 | `HeavyTap` | 长按仅触发触觉反馈 |
| `KeyboardSwitch` | 无 | `MediumTap` | 键盘切换仅触发触觉 |

映射原则：**高频操作同时触发音效和触觉**（按键、候选选择），**低频操作仅触发触觉**（键盘切换、长按），**状态变更操作不触发感官反馈**（提交输入、编辑器编辑操作）。

### 4.2 AudioPlayer / HapticPlayer 统一接口

音效播放器和触觉播放器共享相同的接口模式——接收类型枚举，执行平台特定的播放操作。

```kotlin
package org.crazydan.studio.ime.ui.feedback

interface FeedbackPlayer<T> {
    fun play(type: T)
}

typealias AudioPlayer = FeedbackPlayer<AudioType>

typealias HapticPlayer = FeedbackPlayer<HapticType>
```

`FeedbackPlayer<T>` 是最简化的接口——只有一个 `play(type: T)` 方法，不返回结果，不抛出异常，不提供停止或查询能力。这种极简设计反映了交互反馈的 fire-and-forget 语义。

### 4.3 按键弹出提示处理

按键弹出提示（key popup tips）用于在用户点击按键时在按键上方显示放大提示，帮助用户确认按下的字符。这部分逻辑也完全由 UI 层在 `handleGesture()` 中同步处理，与引擎无关。

```kotlin
fun handleGesture(gesture: InputGesture) {
    // 1. 处理按键弹出提示
    handleKeyPopupTip(gesture)

    // 2. 处理音效和触觉反馈
    when (gesture) {
        is InputGesture.Tap -> {
            playAudio(AudioType.KeyPress)
            playHaptic(HapticType.LightTap)
        }
        // ... 其他手势类型
    }

    // 3. 转换手势为意图并发送给引擎
    val intent = gestureToIntent(gesture)
    engine.handleIntent(intent)
}

private fun handleKeyPopupTip(gesture: InputGesture) {
    if (gesture !is InputGesture.Tap && gesture !is InputGesture.LongPress) return
    if (!config.ui.keyPopupTipsEnabled) return
    if (config.runtime.keyPopupTipsEnabled == false) return

    _keyPopupTipState.value = KeyPopupTipState(
        key = gesture.key,
        show = true,
    )
}
```

按键弹出提示的控制逻辑如下：

- 仅 `Tap` 和 `LongPress` 手势触发按键弹出提示，`Swipe`、`Flip`、`CandidateTap` 等不触发
- 受 `UiConfig.keyPopupTipsEnabled` 和 `RuntimeConfig.keyPopupTipsEnabled` 双重控制
- 弹出提示的坐标和尺寸由 `KeyLayoutState` 中的按键布局信息决定
- 按键释放或进入下一个手势时自动隐藏

### 4.4 配置开关

音效播放和触觉反馈的配置开关由 UI 层在 `playAudio()` / `playHaptic()` 中检查：

- `audioFeedbackEnabled = true` 且 `audioPlayer != null`：调用 `audioPlayer.play(type)` 播放音效
- `audioFeedbackEnabled = false` 或 `audioPlayer == null`：静默跳过

`candidatesPagingAudioEnabled` 是候选翻页音效的独立开关，UI 层在处理 `AudioType.PageFlip` 时额外检查此配置。

触觉反馈的配置逻辑与音效对称，使用 `hapticFeedbackEnabled` 开关。

---

## 5. 平台实现

### 5.1 AndroidAudioPlayer

`AndroidAudioPlayer` 是基于 Android `SoundPool` 的音效播放器实现，由 `:app` 模块提供。

```kotlin
package org.crazydan.studio.ime.app.feedback

import android.content.Context
import android.media.SoundPool
import org.crazydan.studio.ime.ui.feedback.AudioPlayer
import org.crazydan.studio.ime.ui.feedback.AudioType

class AndroidAudioPlayer(context: Context) : AudioPlayer {

    private val soundPool = SoundPool.Builder()
        .setMaxStreams(4)
        .build()

    private val soundIds: Map<AudioType, Int> = mapOf(
        AudioType.KeyPress to soundPool.load(context, R.raw.key_press, 1),
        AudioType.Slip to soundPool.load(context, R.raw.slip, 1),
        AudioType.CandidateSelect to soundPool.load(context, R.raw.candidate_select, 1),
        AudioType.PageFlip to soundPool.load(context, R.raw.page_flip, 1),
    )

    override fun play(type: AudioType) {
        val soundId = soundIds[type] ?: return
        soundPool.play(soundId, 1.0f, 1.0f, 0, 0, 1.0f)
    }

    fun release() {
        soundPool.release()
    }
}
```

### 5.2 AndroidHapticPlayer

`AndroidHapticPlayer` 是基于 Android `Vibrator` 服务的触觉播放器实现，由 `:app` 模块提供。

```kotlin
package org.crazydan.studio.ime.app.feedback

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import org.crazydan.studio.ime.ui.feedback.HapticPlayer
import org.crazydan.studio.ime.ui.feedback.HapticType

class AndroidHapticPlayer(context: Context) : HapticPlayer {

    private val vibrator: Vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val manager = context.getSystemService(VibratorManager::class.java)
        manager.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Vibrator::class.java)
    }

    private val effects: Map<HapticType, VibrationEffect> = mapOf(
        HapticType.LightTap to VibrationEffect.createOneShot(20, 128),
        HapticType.MediumTap to VibrationEffect.createOneShot(50, 180),
        HapticType.HeavyTap to VibrationEffect.createOneShot(100, 255),
    )

    override fun play(type: HapticType) {
        val effect = effects[type] ?: return
        vibrator.vibrate(effect)
    }
}
```

### 5.3 可测试性设计

`FeedbackPlayer<T>` 接口的极简设计（单一 `play()` 方法，无返回值，无异常）使得测试替身的编写极为简单。单元测试中可以轻松创建记录调用参数的 mock 实现，验证 ViewModel 是否在正确的时机调用了正确的播放器方法。

```kotlin
class RecordingFeedbackPlayer<T> : FeedbackPlayer<T> {
    val calls = mutableListOf<T>()
    override fun play(type: T) {
        calls.add(type)
    }
}
```

---

## 6. ViewModel 中的 ImeEffect 订阅

`KeyboardViewModel` 订阅引擎的 `ImeEffect` 通道时，只需处理 `PopupTip` 类型，不再需要处理 `PlayAudio` 和 `PlayHaptic`：

```kotlin
init {
    viewModelScope.launch {
        engine.effect.collect { effect ->
            when (effect) {
                is ImeEffect.PopupTip.Message -> {
                    _popupTipState.value = PopupTipState.Message(
                        message = effect.message,
                        timeoutMs = effect.timeoutMs,
                    )
                    dismissPopupTipAfter(effect.timeoutMs)
                }
                is ImeEffect.PopupTip.Action -> {
                    _popupTipState.value = PopupTipState.Action(
                        message = effect.message,
                        actionLabel = effect.actionLabel,
                        action = effect.action,
                        persistent = effect.persistent,
                        timeoutMs = effect.timeoutMs,
                    )
                    if (!effect.persistent) {
                        dismissPopupTipAfter(effect.timeoutMs)
                    }
                }
            }
        }
    }
}
```

由于不再需要通过 `channelFlow` 对高频的 `PlayAudio`/`PlayHaptic` 进行 conflate 合并，`ImeEffect` 的订阅简化为直接的 `collect` 调用，代码更简洁且语义更清晰。

---

## 7. IMEService 装配

`IMEService` 创建 `AudioPlayer` 和 `HapticPlayer` 并注入 `KeyboardViewModel`，播放器生命周期与 `IMEService` 相同：

```kotlin
class IMEService : InputMethodService() {
    private var engine: ImeEngine? = null
    private var bridge: InputConnectionBridge? = null
    private var audioPlayer: AndroidAudioPlayer? = null
    private var hapticPlayer: AndroidHapticPlayer? = null

    override fun onCreate() {
        super.onCreate()
        engine = ImeEngine.create(
            config = ImeConfig(),
            dictProvider = ImeSqliteDictProvider(this),
        )
        bridge = InputConnectionBridge { currentInputConnection }
        engine?.attachEditorBridge(bridge!!)
        audioPlayer = AndroidAudioPlayer(this)
        hapticPlayer = AndroidHapticPlayer(this)
    }

    override fun onCreateInputView(): View {
        val engine = this.engine ?: error("Engine not initialized")
        return ComposeView(this).also { composeView = it }.apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
            )
            setContent {
                val viewModel: KeyboardViewModel = viewModel(
                    factory = KeyboardViewModel.Factory(engine, audioPlayer, hapticPlayer)
                )
                KeyboardHost(viewModel = viewModel)
            }
        }
    }

    override fun onDestroy() {
        audioPlayer?.release()
        audioPlayer = null
        hapticPlayer = null
        engine?.detachEditorBridge(bridge!!)
        engine?.destroy()
        engine = null
        bridge = null
        super.onDestroy()
    }
}
```

---

## 8. 扩展模式

新增交互反馈类型只需三步，无需修改引擎：

### 8.1 新增音效类型

1. **UI 层**：在 `AudioType` 枚举中新增值
2. **UI 层**：在 `gestureToIntent()` 的 `when(gesture)` 分支中为对应手势添加 `playAudio(AudioType.NewType)` 调用
3. **平台层**：在 `AndroidAudioPlayer` 的 `soundIds` 映射中新增条目，提供对应的音频资源文件

### 8.2 新增触觉类型

1. **UI 层**：在 `HapticType` 枚举中新增值
2. **UI 层**：在 `gestureToIntent()` 的 `when(gesture)` 分支中为对应手势添加 `playHaptic(HapticType.NewType)` 调用
3. **平台层**：在 `AndroidHapticPlayer` 的 `effects` 映射中新增条目

三步扩展模式的核心是：UI 层负责决定「何时和是否」触发反馈（What/Whether），平台层负责「如何」执行反馈（How）。引擎层完全不需要感知交互反馈的存在。
