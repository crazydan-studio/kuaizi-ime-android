# 音效与触觉反馈

IME 的交互反馈分为两类：视觉反馈（按键动画、滑行轨迹、弹出提示等）和感官反馈（音效播放、触觉振动）。感官反馈的共同特征是「fire-and-forget」——引擎发出信号后不维护其状态，UI 层消费后即丢弃，不需要同步协调或状态回滚。本文档描述音效反馈和触觉反馈的设计，包括引擎信号定义、UI 层播放接口、平台实现、配置控制和注入装配。

---

## 1. 设计决策：为何不采用桥接模式

音效播放和触觉振动都属于交互反馈，不属于编辑器操作。虽然 `ImeEditorBridge` 提供了引擎与外部交互的桥接模式，但感官反馈不适合采用同一模式，原因如下：

**语义差异**：`ImeEditorBridge` 的语义是「引擎对目标编辑器的操作指令」——提交文本、撤销输入、移动光标，这些操作具有同步协调语义，引擎需要确保操作成功执行且状态一致。而感官反馈的语义是「引擎通知 UI 层发生了某件事」——按键音、振动，这些是 fire-and-forget 信号，不需要确认和回滚。

**架构分层原则**：v4 设计的状态频率分层明确指出「引擎只拥有逻辑状态，UI 拥有展示状态和交互反馈状态」。音效和触觉属于交互反馈，应由 UI 层管理其播放策略和平台实现。引擎仅通过 `ImeEffect` 通道发出信号，不持有播放器实例，不管理播放器生命周期——这与 `ImeEditorBridge` 直接持有桥梁实例、引擎内部调用桥梁方法的设计模式根本不同。

**扩展性考量**：如果为音效引入 `ImeAudioBridge`，为触觉引入 `ImeHapticBridge`，引擎需要管理越来越多的桥接接口（`attachAudioBridge` / `detachAudioBridge` / `attachHapticBridge` / `detachHapticBridge` ...），每次新增反馈类型都要修改引擎 API。而通过 `ImeEffect` 通道发送信号的方式，新增反馈类型只需在 `ImeEffect` 中添加子类型，引擎 API 无需任何变更。

**第三方友好性**：`ImeEffect` 作为通用副作用通道，第三方应用即使不使用 `:ime-ui`，也可以自行订阅 `SharedFlow<ImeEffect>` 实现自定义的音效和触觉处理逻辑。桥接模式则要求第三方必须实现引擎定义的接口，限制了灵活性。

---

## 2. 引擎信号定义

### 2.1 ImeEffect 扩展

`ImeEffect` 在引擎中新增 `PlayAudio` 和 `PlayHaptic` 两个子类型，与 `PopupTip` 并列。两种反馈信号各自携带类型枚举，表达触发反馈的具体场景。引擎在 `handleIntent()` 的 reduce 过程中根据业务逻辑决定发射时机，UI 层收到信号后根据配置和播放器可用性决定是否播放。

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

    /** 音效反馈信号：指示 UI 层播放指定类型的音效 */
    data class PlayAudio(val type: AudioType) : ImeEffect()

    /** 触觉反馈信号：指示 UI 层触发指定类型的振动 */
    data class PlayHaptic(val type: HapticType) : ImeEffect()
}
```

### 2.2 AudioType 音效类型

`AudioType` 枚举定义了引擎可能触发的所有音效场景。每种音效类型对应一段独立的音频资源文件，由 `AndroidAudioPlayer` 在初始化时加载到 `SoundPool` 中。新增音效类型只需在枚举中添加值并提供对应的音频资源文件，无需修改引擎和 UI 层的核心逻辑。

```kotlin
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

`KeyPress` 是最高频的音效类型，每次按键操作都会触发。引擎在 `PressKey` 意图的 reduce 过程中发射 `ImeEffect.PlayAudio(AudioType.KeyPress)`，无论当前键盘类型和输入模式——这与物理键盘的按键音行为一致，每个按键都有声音反馈。`CandidateSelect` 在用户从候选列表选择候选词时触发，频率仅次于 `KeyPress`。`Slip` 在滑行输入识别完成时触发，反馈滑行识别的成功。`PageFlip` 在候选列表翻页时触发，反馈翻页操作的完成。

### 2.3 HapticType 触觉类型

`HapticType` 枚举定义了引擎可能触发的所有触觉反馈场景。每种触觉类型对应一种振动模式，由 `AndroidHapticPlayer` 通过 Android `Vibrator` 服务执行。与 `AudioType` 不同，触觉类型的区分不基于场景语义，而基于振动强度和持续时间的组合——不同场景可能使用相同的振动模式。

```kotlin
enum class HapticType {
    /** 轻触反馈：按键点击、候选选择等轻量操作 */
    LightTap,

    /** 中等反馈：滑行识别完成、翻页等中等力度操作 */
    MediumTap,

    /** 重触反馈：长按触发上下文菜单等强调操作 */
    HeavyTap,
}
```

`LightTap` 对应短时轻振（约 20ms），用于最常见的按键点击和候选选择场景。`MediumTap` 对应中等时长的振动（约 50ms），用于滑行识别完成、翻页等需要稍强反馈的场景。`HeavyTap` 对应较长时长的强振（约 100ms），用于长按触发上下文菜单等需要明确感知的强调操作。三种振动模式通过 Android `Vibrator` 的 `VibrationEffect.createOneShot()` 实现，时长和强度参数由平台实现控制，引擎和 UI 层不感知具体振动参数。

### 2.4 发射时机与映射关系

`ImeEffect.PlayAudio` 和 `ImeEffect.PlayHaptic` 的发射时机与 `ImeIntent` 类型的映射关系如下。引擎在 reduce 过程中同时决定是否发射音效和触觉信号——同一用户操作可能同时触发音效和触觉反馈，也可能只触发其中一种。

| `ImeIntent` | `PlayAudio` 类型 | `PlayHaptic` 类型 | 说明 |
|---|---|---|---|
| `PressKey`（点击） | `KeyPress` | `LightTap` | 逐键点击输入，音效和触觉同时触发 |
| `PressKey`（滑行） | `Slip` | `MediumTap` | 滑行输入识别完成时触发 |
| `SelectCandidate` | `CandidateSelect` | `LightTap` | 选择候选词时触发 |
| `PageCandidate` | `PageFlip` | `MediumTap` | 翻页时触发 |
| `LongPressKey` | 无 | `HeavyTap` | 长按仅触发触觉反馈，不播放音效 |
| `SwitchKeyboard` | 无 | `MediumTap` | 键盘切换仅触发触觉反馈 |
| `CommitInput` | 无 | 无 | 提交输入不触发感官反馈 |
| `DeleteInput` | 无 | `LightTap` | 删除输入触发轻触反馈 |
| `PerformEdit` | 无 | 无 | 编辑器编辑操作不触发感官反馈 |

映射关系的设计原则是：**高频操作同时触发音效和触觉**（按键、候选选择），**低频操作仅触发触觉**（键盘切换、长按），**状态变更操作不触发感官反馈**（提交输入、编辑器编辑操作）。这种分层避免了音效过于频繁导致的听觉疲劳，同时确保触觉反馈覆盖了所有需要确认的操作。

---

## 3. UI 层播放接口

### 3.1 FeedbackPlayer 统一接口

音效播放器和触觉播放器共享相同的接口模式——接收类型枚举，执行平台特定的播放操作。为避免为每种反馈类型定义独立的接口，设计统一的 `FeedbackPlayer<T>` 泛型接口，音效和触觉各自提供类型特化的别名。

```kotlin
package org.crazydan.studio.ime.ui.feedback

/**
 * 感官反馈播放器的通用接口。
 *
 * 定义在 :ime-ui 中，由 :app 提供平台实现。
 * KeyboardViewModel 在收到 ImeEffect.PlayAudio/PlayHaptic 时调用此接口。
 *
 * @param T 反馈类型枚举，如 AudioType 或 HapticType
 */
interface FeedbackPlayer<T> {
    /** 播放指定类型的反馈 */
    fun play(type: T)
}

/** 音效播放器：FeedbackPlayer 的音效特化 */
typealias AudioPlayer = FeedbackPlayer<AudioType>

/** 触觉播放器：FeedbackPlayer 的触觉特化 */
typealias HapticPlayer = FeedbackPlayer<HapticType>
```

`FeedbackPlayer<T>` 是最简化的接口——只有一个 `play(type: T)` 方法，不返回结果，不抛出异常，不提供停止或查询能力。这种极简设计反映了感官反馈的 fire-and-forget 语义：调用方发出播放指令后不关心播放是否成功、何时结束，播放器内部处理所有错误和边界情况（如音频资源加载失败时静默跳过、振动服务不可用时降级处理）。

使用 `typealias` 而非独立接口的设计理由是：音效播放和触觉播放的接口契约完全相同，只是类型参数不同。如果未来两者的接口需要分化（如音效播放需要 `stop()` 方法），可以在分化时将 `typealias` 替换为独立接口，不影响已有实现。

### 3.2 接口归属理由

`AudioPlayer` 和 `HapticPlayer` 定义在 `:ime-ui` 而非 `:ime-engine`，理由如下：

**架构分层**：音频播放和触觉振动属于 UI 层的交互反馈，不属于引擎的核心逻辑。引擎通过 `ImeEffect` 通道发出信号，UI 层决定是否和如何播放——这符合「引擎只拥有逻辑状态，UI 拥有展示状态和交互反馈状态」的频率分层原则。

**第三方友好性**：定义在 `:ime-ui` 中，第三方引入 `:ime-engine` + `:ime-ui` 即可获得开箱即用的感官反馈能力（只需提供平台实现）。如果定义在 `:ime-engine` 中，第三方仅引入引擎时被迫依赖播放器接口（即使不需要感官反馈），增加了不必要的耦合。

**配置检查归属**：`audioFeedbackEnabled` 和 `hapticFeedbackEnabled` 是 `ImeConfig.UiConfig` 的字段，属于 UI 配置。由 ViewModel 检查这些配置比引擎检查更合理——引擎不需要理解 UI 配置的语义，也不应该在 reduce 过程中根据 UI 配置决定是否发射副作用信号。

---

## 4. KeyboardViewModel 集成

### 4.1 构造参数扩展

`KeyboardViewModel` 的构造函数新增 `audioPlayer` 和 `hapticPlayer` 两个可选参数，由 `:app` 模块在创建 ViewModel 时注入。两个参数均为可空类型——默认值为 `null`，表示不提供感官反馈能力。这种设计确保 ViewModel 在没有播放器的情况下也能正常工作（如纯逻辑测试场景），感官反馈被自动跳过。

```kotlin
class KeyboardViewModel(
    private val engine: ImeEngine,
    private val audioPlayer: AudioPlayer? = null,
    private val hapticPlayer: HapticPlayer? = null,
) : ViewModel() {
    // ...
}
```

### 4.2 ImeEffect 订阅处理

ViewModel 在 `init` 块中订阅引擎的 `effect` 通道，对 `PlayAudio` 和 `PlayHaptic` 两种新增信号的处理逻辑遵循统一模式：检查配置开关 → 检查播放器可用性 → 调用播放器 `play()` 方法。

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
                is ImeEffect.PlayAudio -> {
                    if (state.value.config.ui.audioFeedbackEnabled && audioPlayer != null) {
                        audioPlayer.play(effect.type)
                    }
                }
                is ImeEffect.PlayHaptic -> {
                    if (state.value.config.ui.hapticFeedbackEnabled && hapticPlayer != null) {
                        hapticPlayer.play(effect.type)
                    }
                }
            }
        }
    }
}
```

配置检查使用 `state.value.config.ui.audioFeedbackEnabled` 和 `state.value.config.ui.hapticFeedbackEnabled`，而非引擎内部检查。这确保了引擎的 reduce 函数不感知 UI 配置——引擎始终发射副作用信号，UI 层根据配置决定是否消费。配置的运行时修改通过 `ImeEngine.updateConfig()` 生效，ViewModel 在下一次收到副作用信号时自动使用新配置。

### 4.3 Factory 扩展

`KeyboardViewModel.Factory` 同步扩展 `audioPlayer` 和 `hapticPlayer` 参数，由 `:app` 模块在创建 Factory 时传入：

```kotlin
class Factory(
    private val engine: ImeEngine,
    private val audioPlayer: AudioPlayer? = null,
    private val hapticPlayer: HapticPlayer? = null,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return KeyboardViewModel(engine, audioPlayer, hapticPlayer) as T
    }
}
```

---

## 5. 平台实现

### 5.1 AndroidAudioPlayer

`AndroidAudioPlayer` 是基于 Android `SoundPool` 的音效播放器实现，由 `:app` 模块提供。使用 `SoundPool` 而非 `MediaPlayer` 的原因是按键音需要低延迟、短时长、高并发——`SoundPool` 专门为这种场景设计，音频资源预加载到内存中，播放延迟可控制在 10ms 以内；`MediaPlayer` 需要每次播放时准备音频流，延迟可能达到数百毫秒。

```kotlin
package org.crazydan.studio.ime.app.feedback

import android.content.Context
import android.media.SoundPool
import org.crazydan.studio.ime.engine.model.AudioType
import org.crazydan.studio.ime.ui.feedback.AudioPlayer

/**
 * 基于 Android SoundPool 的音效播放器实现。
 *
 * 在 IMEService.onCreate() 中创建并注入 KeyboardViewModel。
 * 所有音效资源在构造时预加载到 SoundPool 中，确保播放时零延迟。
 */
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

    /** 释放 SoundPool 资源，在 IMEService.onDestroy() 中调用 */
    fun release() {
        soundPool.release()
    }
}
```

`setMaxStreams(4)` 限制最多同时播放 4 路音效——覆盖了极端场景下用户快速连续按键时多路音效重叠的需求，同时避免过多并发音效导致的资源消耗和听觉混乱。`soundPool.play()` 的参数依次为：音频 ID、左声道音量、右声道音量、优先级、循环次数（0 表示不循环）、播放速率（1.0 为正常速率）。所有音效使用统一音量和正常速率，未来可通过 `ImeConfig.UiConfig` 暴露音量和速率配置。

音频资源文件存放在 `:app` 模块的 `res/raw/` 目录下，每个 `AudioType` 枚举值对应一个文件：`key_press.ogg`、`slip.ogg`、`candidate_select.ogg`、`page_flip.ogg`。使用 OGG 格式是因为其压缩率优于 WAV 且 Android 原生支持，文件体积通常在 10KB 以内。

### 5.2 AndroidHapticPlayer

`AndroidHapticPlayer` 是基于 Android `Vibrator` 服务的触觉播放器实现，由 `:app` 模块提供。使用 `Vibrator` 的 `VibrationEffect.createOneShot()` API 创建单次振动效果，不同 `HapticType` 对应不同的振动时长。

```kotlin
package org.crazydan.studio.ime.app.feedback

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import org.crazydan.studio.ime.engine.model.HapticType
import org.crazydan.studio.ime.ui.feedback.HapticPlayer

/**
 * 基于 Android Vibrator 的触觉播放器实现。
 *
 * 在 IMEService.onCreate() 中创建并注入 KeyboardViewModel。
 * 使用 Vibrator 的 createOneShot() API 创建单次振动效果。
 */
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

`LightTap` 使用 20ms / 振幅 128（约 50% 强度）的轻振动，适用于按键点击和候选选择——短促而轻柔，不会在连续输入时产生累加疲劳感。`MediumTap` 使用 50ms / 振幅 180（约 70% 强度）的中等振动，适用于滑行识别和翻页——比轻触稍强，用户可以明确感知操作已被识别。`HeavyTap` 使用 100ms / 振幅 255（100% 强度）的强振动，适用于长按触发——需要明确的触觉确认，用户不会误以为没有响应。

Android 12（API 31）引入了 `VibratorManager` 替代直接获取 `Vibrator`，`AndroidHapticPlayer` 通过 `Build.VERSION.SDK_INT` 判断并使用兼容的 API 获取 `Vibrator` 实例。设备不支持振动或振动服务不可用时，`vibrator.vibrate()` 静默失败，不影响其他功能。

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

## 6. IMEService 装配

`:app` 模块的 `IMEService` 负责创建播放器实例并注入 `KeyboardViewModel`。播放器的生命周期与 `IMEService` 相同——在 `onCreate()` 中创建，在 `onDestroy()` 中释放。播放器不随 `InputConnection` 变更而重建，因为 `SoundPool` 和 `Vibrator` 不依赖于当前输入目标。

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
        val audio = this.audioPlayer
        val haptic = this.hapticPlayer
        return ComposeView(this).also { composeView = it }.apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
            )
            setContent {
                val viewModel: KeyboardViewModel = viewModel(
                    factory = KeyboardViewModel.Factory(engine, audio, haptic)
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
        engine = null
        bridge = null
        composeView?.disposeComposition()
        composeView = null
        super.onDestroy()
    }
}
```

装配流程的关键步骤：

1. **播放器创建**：`IMEService.onCreate()` 中创建 `AndroidAudioPlayer` 和 `AndroidHapticPlayer`，传入 `Context` 用于加载音频资源和获取 `Vibrator` 服务。播放器的创建在引擎创建和桥梁挂载之后，确保所有依赖项就绪。

2. **播放器注入**：`IMEService.onCreateInputView()` 中通过 `KeyboardViewModel.Factory(engine, audio, haptic)` 将播放器注入 ViewModel。ViewModel 在收到 `ImeEffect.PlayAudio` 或 `ImeEffect.PlayHaptic` 时使用注入的播放器执行播放。

3. **播放器释放**：`IMEService.onDestroy()` 中调用 `audioPlayer.release()` 释放 `SoundPool` 资源。`AndroidHapticPlayer` 不需要显式释放（`Vibrator` 是系统服务），但引用置空以避免内存泄漏。播放器的释放先于引擎销毁，确保引擎在最后时刻仍可通过 `ImeEffect` 通道发送信号。

---

## 7. 配置控制

### 7.1 音效配置

音效播放受 `ImeConfig.UiConfig.audioFeedbackEnabled` 控制，默认值为 `true`。ViewModel 在收到 `ImeEffect.PlayAudio` 时检查此配置：

- `audioFeedbackEnabled = true` 且 `audioPlayer != null`：调用 `audioPlayer.play(type)` 播放音效
- `audioFeedbackEnabled = true` 且 `audioPlayer == null`：静默跳过（未注入播放器）
- `audioFeedbackEnabled = false`：静默跳过（用户关闭音效）

`candidatesPagingAudioEnabled` 是候选翻页音效的独立开关，默认值为 `true`。当此配置为 `false` 时，ViewModel 在处理 `ImeEffect.PlayAudio(AudioType.PageFlip)` 时跳过播放，其他音效类型不受影响。这种细粒度控制的原因是翻页音效在某些场景下可能过于频繁（如用户快速滑动候选列表），用户希望单独关闭翻页音而不影响其他音效。

### 7.2 触觉配置

触觉反馈受 `ImeConfig.UiConfig.hapticFeedbackEnabled` 控制，默认值为 `true`。ViewModel 在收到 `ImeEffect.PlayHaptic` 时检查此配置：

- `hapticFeedbackEnabled = true` 且 `hapticPlayer != null`：调用 `hapticPlayer.play(type)` 触发振动
- `hapticFeedbackEnabled = true` 且 `hapticPlayer == null`：静默跳过（未注入播放器）
- `hapticFeedbackEnabled = false`：静默跳过（用户关闭触觉反馈）

触觉反馈没有细粒度开关（如 `hapticFeedbackOnKeyPressEnabled`），因为振动频率远低于音效——按键振动虽然频繁，但触觉的感知阈值比听觉高，不容易产生疲劳。如果未来用户反馈需要细粒度控制，可以在 `UiConfig` 中新增对应字段。

### 7.3 配置变更流程

配置的运行时修改通过 `ImeEngine.updateConfig()` 触发，ViewModel 不需要额外监听配置变更。当用户在设置页面关闭音效开关时，`ConfigDataStore` 持久化新配置后调用 `engine.updateConfig()` 更新运行时配置，引擎通过 `ImeState.config` 暴露新配置。ViewModel 在下一次收到 `ImeEffect.PlayAudio` 时自动读取新配置值，无需配置变更回调或观察者。

这种「拉取式」配置检查的设计确保了 ViewModel 的简洁性——不维护配置的本地副本，不注册配置变更监听器，每次消费副作用信号时直接读取引擎状态中的最新配置。配置变更的生效延迟最多为一个 `ImeEffect` 发射周期（通常为一次按键操作的 reduce 时间，毫秒级），用户感知不到延迟。

---

## 8. 扩展模式

音效和触觉反馈的设计遵循统一的扩展模式——新增感官反馈类型只需四步，无需修改引擎 API 或 ViewModel 的核心逻辑。

### 8.1 新增音效类型

以新增「提交确认音」为例：

1. **引擎层**：在 `AudioType` 枚举中新增 `CommitConfirm` 值，在 `CommitInput` 的 reduce 逻辑中发射 `ImeEffect.PlayAudio(AudioType.CommitConfirm)`
2. **UI 层**：无需修改——`AudioPlayer.play(type)` 的参数类型已覆盖新枚举值
3. **平台层**：在 `AndroidAudioPlayer` 的 `soundIds` 映射中新增 `AudioType.CommitConfirm to soundPool.load(...)` 条目，提供对应的音频资源文件
4. **配置层**：可选——在 `UiConfig` 中新增 `commitConfirmAudioEnabled: Boolean` 细粒度开关

### 8.2 新增触觉类型

以新增「删除输入振动」为例：

1. **引擎层**：在 `HapticType` 枚举中新增 `DeleteFeedback` 值，在 `DeleteInput` 的 reduce 逻辑中发射 `ImeEffect.PlayHaptic(HapticType.DeleteFeedback)`
2. **UI 层**：无需修改——`HapticPlayer.play(type)` 的参数类型已覆盖新枚举值
3. **平台层**：在 `AndroidHapticPlayer` 的 `effects` 映射中新增 `HapticType.DeleteFeedback to VibrationEffect.createOneShot(...)` 条目
4. **配置层**：可选——在 `UiConfig` 中新增 `deleteHapticEnabled: Boolean` 细粒度开关

### 8.3 新增感官通道

以新增「LED 闪烁反馈」为例：

1. **引擎层**：在 `ImeEffect` 中新增 `data class PlayLed(val type: LedType) : ImeEffect()`，定义 `LedType` 枚举，在对应 reduce 逻辑中发射
2. **UI 层**：定义 `typealias LedPlayer = FeedbackPlayer<LedType>`，ViewModel 构造函数新增 `ledPlayer: LedPlayer? = null`，在 `ImeEffect` 订阅中新增 `is ImeEffect.PlayLed` 分支
3. **平台层**：实现 `AndroidLedPlayer`，通过设备 LED 控制接口执行闪烁
4. **配置层**：在 `UiConfig` 中新增 `ledFeedbackEnabled: Boolean` 开关

四步扩展模式的核心是：引擎只负责发信号（What），UI 层负责检查配置（Whether），平台层负责执行操作（How）。每层只需在自身职责范围内做最小变更，无需跨层协调。

---

## 9. 三方模块集成指南

第三方应用引入 `:ime-engine` 和 `:ime-ui` 时，感官反馈的集成分三种场景：

### 9.1 标准集成（使用 `:app` 的播放器实现）

最简单的集成方式——直接使用 `AndroidAudioPlayer` 和 `AndroidHapticPlayer`，在创建 `KeyboardViewModel` 时注入：

```kotlin
val engine = ImeEngine.create(config, dictProvider)
val audioPlayer = AndroidAudioPlayer(context)
val hapticPlayer = AndroidHapticPlayer(context)
val viewModel = KeyboardViewModel(engine, audioPlayer, hapticPlayer)
```

### 9.2 自定义播放器实现

第三方可以提供自定义的 `AudioPlayer` 或 `HapticPlayer` 实现，例如使用不同的音频引擎或振动模式：

```kotlin
class CustomAudioPlayer : AudioPlayer {
    override fun play(type: AudioType) {
        // 使用 Oboi 或 ExoPlayer 播放音效
    }
}

val viewModel = KeyboardViewModel(engine, CustomAudioPlayer(), hapticPlayer)
```

### 9.3 仅使用引擎（不使用 `:ime-ui`）

第三方仅引入 `:ime-engine` 时，自行订阅 `SharedFlow<ImeEffect>` 处理感官反馈：

```kotlin
val engine = ImeEngine.create(config, dictProvider)
engine.effect.collect { effect ->
    when (effect) {
        is ImeEffect.PlayAudio -> myAudioPlayer.play(effect.type)
        is ImeEffect.PlayHaptic -> myHapticPlayer.vibrate(effect.type)
        // 处理其他 ImeEffect 类型
    }
}
```

引擎的 `ImeEffect` 通道不依赖 `:ime-ui`——第三方完全自主决定如何消费副作用信号，包括完全忽略感官反馈。
