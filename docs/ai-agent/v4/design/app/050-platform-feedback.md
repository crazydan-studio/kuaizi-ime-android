# 平台反馈实现

## 1. AndroidAudioPlayer

基于 Android `SoundPool` 的音效播放器实现，由 `:app` 模块提供。

```kotlin
package org.crazydan.studio.app.ime.kuaizi.device

import android.content.Context
import android.media.SoundPool
import org.crazydan.studio.app.ime.kuaizi.ui.AudioPlayer
import org.crazydan.studio.app.ime.kuaizi.ui.effect.AudioType

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

## 2. AndroidHapticPlayer

基于 Android `Vibrator` 服务的触觉播放器实现。

```kotlin
package org.crazydan.studio.app.ime.kuaizi.device

import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import org.crazydan.studio.app.ime.kuaizi.ui.HapticPlayer
import org.crazydan.studio.app.ime.kuaizi.ui.effect.HapticType

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

## 3. IMEService 装配

`IMEService` 创建 `AudioPlayer` 和 `HapticPlayer` 并注入 `KeyboardViewModel`：

```kotlin
class IMEService : InputMethodService() {
    private var audioPlayer: AndroidAudioPlayer? = null
    private var hapticPlayer: AndroidHapticPlayer? = null

    override fun onCreate() {
        super.onCreate()
        audioPlayer = AndroidAudioPlayer(this)
        hapticPlayer = AndroidHapticPlayer(this)
    }

    override fun onCreateInputView(): View {
        return ComposeView(this).apply {
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
        super.onDestroy()
    }
}
```

## 4. 可测试性

`FeedbackPlayer<T>` 的极简设计使得测试替身极为简单：

```kotlin
class RecordingFeedbackPlayer<T> : FeedbackPlayer<T> {
    val calls = mutableListOf<T>()
    override fun play(type: T) {
        calls.add(type)
    }
}
```
