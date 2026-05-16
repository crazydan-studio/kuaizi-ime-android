# 配置管理设计

v4 版本使用 DataStore + Flow 实现类型安全的配置管理和响应式更新。`ImeConfig` 包含 `EngineConfig`（引擎配置）和 `UiConfig`（UI 配置）两个嵌套 data class，引擎与 UI 配置在数据结构上明确隔离。`:app` 模块的 `ConfigDataStore` 直接持久化和恢复 `ImeConfig`。

---

## 1. ImeConfig 运行时与持久化配置

**运行时优先原则**：`ImeConfig` 在运行时的修改始终优先于应用侧配置，直到应用重启。重启时，`ImeConfig` 根据持久化配置进行初始化。`ImeConfig.runtimeOverrides` 记录被运行时临时修改的字段，持久化同步时跳过这些字段。

> `ImeConfig` 定义在 `:ime-engine` 库中，包含 `EngineConfig` 和 `UiConfig`。完整的 ImeConfig 定义见[010-引擎库设计总览](../engine/010-engine-overview.md) §3.2。

---

## 2. ConfigDataStore（DataStore 实现）

```kotlin
/**
 * 配置仓库，管理 ImeConfig 的持久化。
 *
 * 直接持久化 ImeConfig（含 EngineConfig 和 UiConfig），
 * 无需独立的 Config data class 或两套配置的同步逻辑。
 */
class ConfigDataStore(private val context: Context) {
    private val Context.dataStore by preferencesDataStore("ime_config")

    val config: Flow<ImeConfig> = context.dataStore.data.map { prefs ->
        ImeConfig(
            engine = ImeConfig.EngineConfig(
                keyboardType = KeyboardType.entries[prefs[KEYBOARD_TYPE] ?: KeyboardType.Pinyin.ordinal],
                handMode = HandMode.entries[prefs[HAND_MODE] ?: HandMode.Right.ordinal],
                features = parseFeatures(prefs),
                candidatePredictionEnabled = prefs[CANDIDATE_PREDICTION] ?: true,
                singleLineInput = prefs[SINGLE_LINE_INPUT] ?: false,
            ),
            ui = ImeConfig.UiConfig(
                themeType = ThemeType.entries[prefs[THEME_TYPE] ?: ThemeType.FollowSystem.ordinal],
                xPadEnabled = prefs[ENABLE_X_PAD] ?: true,
                audioFeedbackEnabled = prefs[AUDIO_FEEDBACK] ?: true,
                hapticFeedbackEnabled = prefs[HAPTIC_FEEDBACK] ?: true,
                keyAnimationEnabled = prefs[KEY_ANIMATION] ?: true,
                keyPopupTipsEnabled = prefs[KEY_POPUP_TIPS] ?: true,
                gestureSlippingTrailEnabled = prefs[GESTURE_TRAIL] ?: true,
                clipPopupTipsEnabled = prefs[CLIP_POPUP_TIPS] ?: true,
                clipPopupTipsTimeout = prefs[CLIP_POPUP_TIMEOUT] ?: 15,
                adaptDesktopSwipeUpGesture = prefs[ADAPT_DESKTOP_SWIPE] ?: false,
                candidateVariantFirstEnabled = prefs[CANDIDATE_VARIANT_FIRST] ?: false,
                latinUsePinyinKeysInXPadEnabled = prefs[LATIN_USE_PINYIN_KEYS] ?: false,
                userInputDataEnabled = prefs[USER_INPUT_DATA] ?: true,
                candidatesPagingAudioEnabled = prefs[CANDIDATES_PAGING_AUDIO] ?: true,
                practicePlaybackSpeed = prefs[PRACTICE_PLAYBACK_SPEED] ?: 1.0f,
                practiceShowFingerOverlay = prefs[PRACTICE_SHOW_FINGER] ?: true,
                practiceShowSwipeTrail = prefs[PRACTICE_SHOW_TRAIL] ?: true,
                logLevel = LogLevel.entries[prefs[LOG_LEVEL] ?: LogLevel.WARN.ordinal],
                logStoragePath = prefs[LOG_STORAGE_PATH],
            ),
        )
    }

    suspend fun updateConfig(transform: (ImeConfig) -> ImeConfig) {
        context.dataStore.edit { prefs ->
            val current = config.first()
            val new = transform(current)
            // 持久化引擎配置
            prefs[KEYBOARD_TYPE] = new.engine.keyboardType.ordinal
            prefs[HAND_MODE] = new.engine.handMode.ordinal
            prefs[CANDIDATE_PREDICTION] = new.engine.candidatePredictionEnabled
            prefs[SINGLE_LINE_INPUT] = new.engine.singleLineInput
            serializeFeatures(prefs, new.engine.features)
            // 持久化 UI 配置
            prefs[THEME_TYPE] = new.ui.themeType.ordinal
            prefs[ENABLE_X_PAD] = new.ui.xPadEnabled
            prefs[AUDIO_FEEDBACK] = new.ui.audioFeedbackEnabled
            prefs[HAPTIC_FEEDBACK] = new.ui.hapticFeedbackEnabled
            prefs[KEY_ANIMATION] = new.ui.keyAnimationEnabled
            prefs[KEY_POPUP_TIPS] = new.ui.keyPopupTipsEnabled
            prefs[GESTURE_TRAIL] = new.ui.gestureSlippingTrailEnabled
            prefs[CLIP_POPUP_TIPS] = new.ui.clipPopupTipsEnabled
            prefs[CLIP_POPUP_TIMEOUT] = new.ui.clipPopupTipsTimeout
            prefs[ADAPT_DESKTOP_SWIPE] = new.ui.adaptDesktopSwipeUpGesture
            prefs[CANDIDATE_VARIANT_FIRST] = new.ui.candidateVariantFirstEnabled
            prefs[LATIN_USE_PINYIN_KEYS] = new.ui.latinUsePinyinKeysInXPadEnabled
            prefs[USER_INPUT_DATA] = new.ui.userInputDataEnabled
            prefs[CANDIDATES_PAGING_AUDIO] = new.ui.candidatesPagingAudioEnabled
            prefs[PRACTICE_PLAYBACK_SPEED] = new.ui.practicePlaybackSpeed
            prefs[PRACTICE_SHOW_FINGER] = new.ui.practiceShowFingerOverlay
            prefs[PRACTICE_SHOW_TRAIL] = new.ui.practiceShowSwipeTrail
            prefs[LOG_LEVEL] = new.ui.logLevel.ordinal
            if (new.ui.logStoragePath != null) prefs[LOG_STORAGE_PATH] = new.ui.logStoragePath
        }
    }

    companion object {
        private val KEYBOARD_TYPE = intPreferencesKey("keyboard_type")
        private val HAND_MODE = intPreferencesKey("hand_mode")
        private val CANDIDATE_PREDICTION = booleanPreferencesKey("candidate_prediction")
        private val SINGLE_LINE_INPUT = booleanPreferencesKey("single_line_input")
        private val THEME_TYPE = intPreferencesKey("theme_type")
        private val ENABLE_X_PAD = booleanPreferencesKey("enable_x_pad")
        private val AUDIO_FEEDBACK = booleanPreferencesKey("audio_feedback")
        private val HAPTIC_FEEDBACK = booleanPreferencesKey("haptic_feedback")
        private val KEY_ANIMATION = booleanPreferencesKey("key_animation")
        private val KEY_POPUP_TIPS = booleanPreferencesKey("key_popup_tips")
        private val GESTURE_TRAIL = booleanPreferencesKey("gesture_trail")
        private val CLIP_POPUP_TIPS = booleanPreferencesKey("clip_popup_tips")
        private val CLIP_POPUP_TIMEOUT = intPreferencesKey("clip_popup_timeout")
        private val ADAPT_DESKTOP_SWIPE = booleanPreferencesKey("adapt_desktop_swipe")
        private val CANDIDATE_VARIANT_FIRST = booleanPreferencesKey("candidate_variant_first")
        private val LATIN_USE_PINYIN_KEYS = booleanPreferencesKey("latin_use_pinyin_keys")
        private val USER_INPUT_DATA = booleanPreferencesKey("user_input_data")
        private val CANDIDATES_PAGING_AUDIO = booleanPreferencesKey("candidates_paging_audio")
        private val PRACTICE_PLAYBACK_SPEED = floatPreferencesKey("practice_playback_speed")
        private val PRACTICE_SHOW_FINGER = booleanPreferencesKey("practice_show_finger")
        private val PRACTICE_SHOW_TRAIL = booleanPreferencesKey("practice_show_trail")
        private val LOG_LEVEL = intPreferencesKey("log_level")
        private val LOG_STORAGE_PATH = stringPreferencesKey("log_storage_path")
    }
}
```

---

## 3. 主题系统

### 3.1 主题定义

```kotlin
data class KeyboardColors(
    // 键盘
    val keyBackground: Color,
    val keyForeground: Color,
    val keyPressedBackground: Color,
    val keyActiveBackground: Color,
    val keyDisabledBackground: Color,
    val keyBorder: Color,

    // 候选栏
    val candidatePanelBackground: Color,
    val candidateChipBackground: Color,
    val candidateChipForeground: Color,
    val candidateChipActiveBackground: Color,

    // 输入栏
    val inputListPanelBackground: Color,
    val inputListPanelForeground: Color,
    val inputListPanelCursorColor: Color,

    // X-Pad
    val xPadBackground: Color,
    val xPadZoneBorder: Color,
    val xPadZoneForeground: Color,
    val xPadActiveZoneBackground: Color,

    // 通用
    val background: Color,
    val foreground: Color,
    val divider: Color,
)

object KeyboardThemes {
    val Light = KeyboardColors(
        keyBackground = Color(0xFFE8E8E8),
        keyForeground = Color(0xFF333333),
        // ...
    )

    val Night = KeyboardColors(
        keyBackground = Color(0xFF333333),
        keyForeground = Color(0xFFE8E8E8),
        // ...
    )
}

val LocalKeyboardColors = compositionLocalOf { KeyboardThemes.Light }
```

### 3.2 跟随系统主题

```kotlin
@Composable
fun KeyboardTheme(
    themeType: ThemeType = ThemeType.FollowSystem,
    content: @Composable () -> Unit,
) {
    val isDark = when (themeType) {
        ThemeType.Light -> false
        ThemeType.Night -> true
        ThemeType.FollowSystem -> isSystemInDarkTheme()
    }

    val colors = if (isDark) KeyboardThemes.Night else KeyboardThemes.Light

    CompositionLocalProvider(LocalKeyboardColors provides colors) {
        content()
    }
}
```

---

## 4. 配置字段映射

所有配置项统一使用肯定式命名（`*Enabled` 后缀），与 UI 语义一致，无需反转映射：

| ImeConfig.UiConfig 属性 | UI 显示名称（正向） | UI 默认值 | 映射关系 |
|-------------------------|---------------------|-----------|----------|
| `audioFeedbackEnabled` | 按键音效 | 开 | 直接映射 |
| `hapticFeedbackEnabled` | 触觉反馈 | 开 | 直接映射 |
| `keyAnimationEnabled` | 按键动画 | 开 | 直接映射 |
| `candidatesPagingAudioEnabled` | 翻页提示音 | 开 | 直接映射 |
| `keyPopupTipsEnabled` | 按键放大提示 | 开 | 直接映射 |
| `gestureSlippingTrailEnabled` | 滑行轨迹显示 | 开 | 直接映射 |
| `clipPopupTipsEnabled` | 剪贴板粘贴提示 | 开 | 直接映射 |
| `userInputDataEnabled` | 记录输入习惯 | 开 | 直接映射 |

映射规则：
- 所有字段统一使用肯定式命名（`*Enabled` 后缀）：UI 开关值 = `config.ui.*Enabled`
