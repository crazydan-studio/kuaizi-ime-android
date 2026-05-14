# 500 — 配置与设置系统设计

## 1. 概述

配置系统管理筷字输入法的所有用户可配置项，包括主题、手模式、X-Pad 开关、音频控制等。Java 版本使用 `SharedPreferences` + 自定义 `Config` 层叠覆盖系统，v4 版本迁移到 DataStore + Flow，实现类型安全的配置管理和响应式更新。

---

## 2. Java 版本配置系统分析

### 2.1 架构

```
SharedPreferences
    ↕ 同步读写
IMEConfig (桥接层)
    ↕ 监听变更
Config.Mutable → Config.Immutable (层叠覆盖)
    ↕
ConfigKey (枚举，定义所有配置键和默认值)
```

### 2.2 配置项清单

| 键 | 类型 | 默认值 | 说明 |
|----|------|--------|------|
| `theme` | String | "follow_system" | 主题模式：light/night/follow_system |
| `hand_mode` | String | "right" | 手模式：left/right |
| `enable_x_input_pad` | Boolean | true | 是否启用 X-Pad |
| `enable_latin_use_pinyin_keys_in_x_input_pad` | Boolean | false | 拉丁键盘是否复用拼音 X-Pad 布局 |
| `adapt_desktop_swipe_up_gesture` | Boolean | false | 适配桌面滑动手势 |
| `enable_candidate_variant_first` | Boolean | false | 候选字繁体优先 |
| `disable_user_input_data` | Boolean | false | 禁用用户输入数据记录 |
| `disable_key_clicked_audio` | Boolean | false | 禁用按键音效 |
| `disable_key_animation` | Boolean | false | 禁用按键动画 |
| `disable_input_candidates_paging_audio` | Boolean | false | 禁用翻页音效 |
| `disable_input_key_popup_tips` | Boolean | false | 禁用按键弹出提示 |
| `disable_gesture_slipping_trail` | Boolean | false | 禁用手势轨迹 |
| `disable_input_clip_popup_tips` | Boolean | false | 禁用剪贴板弹出提示 |
| `input_clip_popup_tips_timeout` | Int | 15 | 剪贴板提示超时（秒） |

### 2.3 问题分析

1. **Config.Mutable 与 Config.Immutable 的层叠覆盖复杂**：`Config.Mutable` 包装 `Config.Immutable`，允许运行时覆盖部分值，但层叠逻辑增加了理解成本
2. **SharedPreferences 的同步阻塞**：`commit()` 阻塞主线程，`apply()` 虽然异步但可能在 `onStop()` 时丢失
3. **无类型安全**：所有值以基本类型存储，配置键以字符串引用
4. **变更通知机制原始**：`OnSharedPreferenceChangeListener` 需要手动注册/注销

---

## 3. v4 配置系统设计

### 3.1 数据模型

> **设计决策**：v4 配置系统采用统一的 `ImeConfig`（定义在 `:ime-engine` 库，详见文档 160 第 4.2 节），替代原设计中分离的 `ImeEngineConfig`（引擎配置）和 `Config`（应用配置）。`ImeConfig` 包含 `EngineConfig`（引擎配置）和 `UiConfig`（UI 配置）两个嵌套 data class，引擎与 UI 配置在数据结构上明确隔离。`:app` 模块的 `ConfigRepository` 直接持久化和恢复 `ImeConfig`，不再需要独立的 `Config` data class 和两套配置之间的运行时同步。

**运行时优先原则**：`ImeConfig` 在运行时的修改始终优先于应用侧配置，直到应用重启。重启时，`ImeConfig` 根据持久化配置进行初始化。`ImeConfig.runtimeOverrides` 记录被运行时临时修改的字段，持久化同步时跳过这些字段。

```kotlin
/**
 * 输入法配置（ImeConfig 的 :app 模块视角说明）。
 *
 * ImeConfig 定义在 :ime-engine 库中（详见文档 160 第 4.2 节），
 * 包含 EngineConfig（引擎配置）和 UiConfig（UI 配置）。
 * 此处仅列出 :app 模块关心的配置项与 Java 版本的对照关系。
 *
 * 运行时修改优先规则：
 * - 通过键盘 UI 进行的临时修改（如临时切换左右手模式）优先于持久化配置
 * - ImeConfig.runtimeOverrides 记录被运行时覆盖的字段
 * - 持久化配置同步到引擎时，跳过已被运行时覆盖的字段
 *
 * 完整的 ImeConfig 定义见文档 160 第 4.2 节。
 */
```

### 3.2 DataStore 实现

```kotlin
/**
 * 配置仓库，管理 ImeConfig 的持久化。
 *
 * 直接持久化 ImeConfig（含 EngineConfig 和 UiConfig），
 * 无需独立的 Config data class 或两套配置的同步逻辑。
 */
class ConfigRepository(private val context: Context) {
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

## 4. 主题系统

### 4.1 主题定义

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

### 4.2 跟随系统主题

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

## 5. Java 功能完整对照

| Java 配置功能 | v4 对应 | 改进说明 |
|-------------|---------|---------|
| `Config.Immutable` | `ImeConfig` data class | 移除层叠覆盖，统一不可变配置 |
| `Config.Mutable` | `ConfigRepository.updateConfig()` | DataStore 原子更新 |
| `ConfigChangeListener` | `Flow<ImeConfig>` | 响应式更新，自动生命周期管理 |
| `ConfigKey` 枚举 | `ImeConfig` 属性 | 类型安全，编译期检查 |
| `IMEConfig` 桥接 | `ConfigRepository` | DataStore 直接管理 |
| `SharedPreferences` | `DataStore<Preferences>` | 异步、类型安全、无 ANR |
| 主题资源 (`themes.xml`, `attrs.xml`) | Compose 主题 | 声明式主题系统 |
| 60+ 主题属性 | `KeyboardColors` data class | 类型安全，IDE 自动补全 |
