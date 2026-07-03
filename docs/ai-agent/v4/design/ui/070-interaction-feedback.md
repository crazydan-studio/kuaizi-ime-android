# 交互反馈设计

## 1. 设计边界

交互反馈（音效、触觉、按键弹出提示）的共同特征是「fire-and-forget」——与引擎的逻辑状态无关，UI 层可以直接根据用户手势决定是否触发，不需要引擎协调或状态回滚。

**归属理由**：

- **架构分层**：音频播放和触觉振动属于 UI 层的交互反馈，不属于引擎的核心逻辑。引擎只通过 `ImeEffect` 发射领域事件型的 `PopupTip`，不持有任何反馈类型定义。
- **性能考量**：按键音和触觉在快速连续打字时需要低延迟触发。UI 层在 `gestureToIntent()` 中同步处理反馈避免了 engine → SharedFlow → collect 的异步路径延迟。
- **第三方友好性**：`AudioType`/`HapticType` 定义在 `:ui` 中，第三方引入 `:engine` + `:ui` 即可获得开箱即用的反馈能力。

---

## 2. 反馈类型定义

### 2.1 AudioType

```kotlin
package org.crazydan.studio.app.ime.kuaizi.ui.effect

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

### 2.2 HapticType

```kotlin
package org.crazydan.studio.app.ime.kuaizi.ui.effect

enum class HapticType {
    /** 轻触反馈：按键点击、候选选择等 */
    LightTap,
    /** 中等反馈：滑行识别完成、翻页等 */
    MediumTap,
    /** 重触反馈：长按触发上下文菜单等 */
    HeavyTap,
}
```

---

## 3. FeedbackPlayer 接口

```kotlin
interface FeedbackPlayer<T> {
    fun play(type: T)
}

typealias AudioPlayer = FeedbackPlayer<AudioType>
typealias HapticPlayer = FeedbackPlayer<HapticType>
```

`FeedbackPlayer<T>` 是最简化的接口——只有一个 `play(type: T)` 方法，不返回结果，不抛出异常，不提供停止或查询能力。这种极简设计反映了交互反馈的 fire-and-forget 语义。

---

## 4. gestureToIntent() 中的反馈处理

`KeyboardViewModel.gestureToIntent()` 在将手势转换为引擎意图的同时，直接同步处理音效和触觉反馈：

```kotlin
fun handleGesture(gesture: InputGesture) {
    // 1. 处理按键弹出提示
    handleKeyPopupTip(gesture)

    // 2. 处理音效和触觉反馈
    playFeedback(gesture)

    // 3. 转换手势为意图并发送给引擎
    val intent = gestureToIntent(gesture)
    engine.handleIntent(intent)
}
```

### 4.1 反馈映射表

| `InputGesture` | `AudioType` | `HapticType` | 说明 |
|---|---|---|---|
| `Tap` | `KeyPress` | `LightTap` | 逐键点击 |
| `Swipe` | `Slip` | `MediumTap` | 滑行输入 |
| `CandidateTap` | `CandidateSelect` | `LightTap` | 选择候选词 |
| `Flip` | `PageFlip` | `MediumTap` | 翻页 |
| `LongPress` | 无 | `HeavyTap` | 长按仅触觉 |
| `KeyboardSwitch` | 无 | `MediumTap` | 键盘切换仅触觉 |

### 4.2 配置检查

```kotlin
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

`candidatesPagingAudioEnabled` 是候选翻页音效的独立开关，UI 层在处理 `AudioType.PageFlip` 时额外检查此配置。

---

## 5. 按键弹出提示

按键弹出提示（key popup tips）用于在用户点击按键时在按键上方显示放大提示。逻辑完全由 UI 层在 `handleGesture()` 中同步处理，与引擎无关。

### 5.1 控制逻辑

- 仅 `Tap` 和 `LongPress` 手势触发
- 受 `UiConfig.keyPopupTipsEnabled` 控制
- 弹出提示的坐标和尺寸由 `KeyLayoutState` 决定
- 按键释放或进入下一个手势时自动隐藏

```kotlin
private fun handleKeyPopupTip(gesture: InputGesture) {
    if (gesture !is InputGesture.Tap && gesture !is InputGesture.LongPress) return
    if (!config.ui.keyPopupTipsEnabled) return

    _keyPopupTipState.value = KeyPopupTipState(
        key = gesture.key,
        show = true,
    )
}
```

---

## 6. ViewModel 中的 ImeEffect 订阅

`KeyboardViewModel` 订阅引擎的 `ImeEffect` 通道时，只需处理 `PopupTip` 类型：

```kotlin
init {
    viewModelScope.launch {
        engine.effect.collect { effect ->
            when (effect) {
                is ImeEffect.Message -> {
                    _popupTipState.value = PopupTipState.Message(
                        message = effect.message,
                        timeoutMs = effect.timeoutMs,
                    )
                    dismissPopupTipAfter(effect.timeoutMs)
                }
                is ImeEffect.Action -> {
                    _popupTipState.value = PopupTipState.Action(...)
                    if (!effect.persistent) {
                        dismissPopupTipAfter(effect.timeoutMs)
                    }
                }
            }
        }
    }
}
```

---

## 7. 可测试性

`FeedbackPlayer<T>` 的极简设计使得测试替身极为简单：

```kotlin
class RecordingFeedbackPlayer<T> : FeedbackPlayer<T> {
    val calls = mutableListOf<T>()
    override fun play(type: T) {
        calls.add(type)
    }
}
```

---

## 8. 扩展模式

### 新增音效类型

1. 在 `AudioType` 中新增值
2. 在 `gestureToIntent()` 中添加对应分支
3. 在 `AndroidAudioPlayer` 的 `soundIds` 中新增资源

### 新增触觉类型

1. 在 `HapticType` 中新增值
2. 在 `gestureToIntent()` 中添加对应分支
3. 在 `AndroidHapticPlayer` 的 `effects` 中新增振动参数
