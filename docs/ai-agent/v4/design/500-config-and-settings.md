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

```kotlin
/**
 * 输入法配置，不可变。
 * 所有配置项通过 data class 的 copy() 方法更新。
 */
data class Config(
    // 外观
    val themeType: ThemeType = ThemeType.FollowSystem,
    val handMode: HandMode = HandMode.Right,

    // 输入
    val enableXPad: Boolean = true,
    val enableLatinUsePinyinKeysInXPad: Boolean = false,
    val adaptDesktopSwipeUpGesture: Boolean = false,
    val enableCandidateVariantFirst: Boolean = false,

    // 隐私
    val disableUserInputData: Boolean = false,

    // 反馈
    val disableKeyClickedAudio: Boolean = false,
    val disableKeyAnimation: Boolean = false,
    val disableCandidatesPagingAudio: Boolean = false,
    val disableKeyPopupTips: Boolean = false,
    val disableGestureSlippingTrail: Boolean = false,
    val disableClipPopupTips: Boolean = false,
    val clipPopupTipsTimeout: Int = 15,
)

enum class ThemeType { Light, Night, FollowSystem }
enum class HandMode { Left, Right }
```

### 3.2 DataStore 实现

```kotlin
class ConfigRepository(private val dataStore: DataStore<Config>) {
    val config: Flow<Config> = dataStore.data

    suspend fun update(transform: (Config) -> Config) {
        dataStore.updateData(transform)
    }

    companion object {
        fun create(context: Context): ConfigRepository {
            val dataStore = PreferenceDataStoreFactory.create(
                serializer = ConfigSerializer,
            ) {
                context.preferencesDataStoreFile("ime_config")
            }
            return ConfigRepository(dataStore)
        }
    }
}

object ConfigSerializer : Serializer<Config> {
    override val defaultValue: Config = Config()

    override suspend fun readFrom(input: InputStream): Config {
        // 从 Preferences 反序列化
        val prefs = Preferences.parseFrom(input)
        return Config(
            themeType = ThemeType.entries[prefs.getInt("theme_type", 2)],
            handMode = HandMode.entries[prefs.getInt("hand_mode", 1)],
            enableXPad = prefs.getBoolean("enable_x_pad", true),
            // ...
        )
    }

    override suspend fun writeTo(t: Config, output: OutputStream) {
        // 序列化为 Preferences
        val prefs = preferencesOf(
            "theme_type" to t.themeType.ordinal,
            "hand_mode" to t.handMode.ordinal,
            "enable_x_pad" to t.enableXPad,
            // ...
        )
        prefs.writeTo(output)
    }
}
```

### 3.3 简化方案：使用 Preferences DataStore

由于配置项都是基本类型，使用 Preferences DataStore 更简单：

```kotlin
class ConfigRepository(private val context: Context) {
    private val Context.dataStore by preferencesDataStore("ime_config")

    val config: Flow<Config> = context.dataStore.data.map { prefs ->
        Config(
            themeType = ThemeType.entries[prefs[THEME_TYPE] ?: ThemeType.FollowSystem.ordinal],
            handMode = HandMode.entries[prefs[HAND_MODE] ?: HandMode.Right.ordinal],
            enableXPad = prefs[ENABLE_X_PAD] ?: true,
            enableLatinUsePinyinKeysInXPad = prefs[ENABLE_LATIN_USE_PINYIN_KEYS] ?: false,
            adaptDesktopSwipeUpGesture = prefs[ADAPT_DESKTOP_SWIPE] ?: false,
            enableCandidateVariantFirst = prefs[ENABLE_VARIANT_FIRST] ?: false,
            disableUserInputData = prefs[DISABLE_USER_INPUT_DATA] ?: false,
            disableKeyClickedAudio = prefs[DISABLE_KEY_AUDIO] ?: false,
            disableKeyAnimation = prefs[DISABLE_KEY_ANIMATION] ?: false,
            disableCandidatesPagingAudio = prefs[DISABLE_PAGING_AUDIO] ?: false,
            disableKeyPopupTips = prefs[DISABLE_KEY_POPUP_TIPS] ?: false,
            disableGestureSlippingTrail = prefs[DISABLE_GESTURE_TRAIL] ?: false,
            disableClipPopupTips = prefs[DISABLE_CLIP_POPUP_TIPS] ?: false,
            clipPopupTipsTimeout = prefs[CLIP_POPUP_TIMEOUT] ?: 15,
        )
    }

    suspend fun updateConfig(transform: (Config) -> Config) {
        context.dataStore.edit { prefs ->
            val current = config.first()
            val new = transform(current)
            prefs[THEME_TYPE] = new.themeType.ordinal
            prefs[HAND_MODE] = new.handMode.ordinal
            prefs[ENABLE_X_PAD] = new.enableXPad
            // ...
        }
    }

    companion object {
        private val THEME_TYPE = intPreferencesKey("theme_type")
        private val HAND_MODE = intPreferencesKey("hand_mode")
        private val ENABLE_X_PAD = booleanPreferencesKey("enable_x_pad")
        private val ENABLE_LATIN_USE_PINYIN_KEYS = booleanPreferencesKey("enable_latin_use_pinyin_keys")
        private val ADAPT_DESKTOP_SWIPE = booleanPreferencesKey("adapt_desktop_swipe")
        private val ENABLE_VARIANT_FIRST = booleanPreferencesKey("enable_variant_first")
        private val DISABLE_USER_INPUT_DATA = booleanPreferencesKey("disable_user_input_data")
        private val DISABLE_KEY_AUDIO = booleanPreferencesKey("disable_key_audio")
        private val DISABLE_KEY_ANIMATION = booleanPreferencesKey("disable_key_animation")
        private val DISABLE_PAGING_AUDIO = booleanPreferencesKey("disable_paging_audio")
        private val DISABLE_KEY_POPUP_TIPS = booleanPreferencesKey("disable_key_popup_tips")
        private val DISABLE_GESTURE_TRAIL = booleanPreferencesKey("disable_gesture_trail")
        private val DISABLE_CLIP_POPUP_TIPS = booleanPreferencesKey("disable_clip_popup_tips")
        private val CLIP_POPUP_TIMEOUT = intPreferencesKey("clip_popup_timeout")
    }
}
```

---

## 4. 主题系统

### 4.1 主题定义

```kotlin
data class IMEColors(
    // 键盘
    val keyBackground: Color,
    val keyForeground: Color,
    val keyPressedBackground: Color,
    val keyActiveBackground: Color,
    val keyDisabledBackground: Color,
    val keyBorder: Color,

    // 候选栏
    val candidateBarBackground: Color,
    val candidateChipBackground: Color,
    val candidateChipForeground: Color,
    val candidateChipActiveBackground: Color,

    // 输入栏
    val inputBarBackground: Color,
    val inputBarForeground: Color,
    val inputBarCursorColor: Color,

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

object IMEThemes {
    val Light = IMEColors(
        keyBackground = Color(0xFFE8E8E8),
        keyForeground = Color(0xFF333333),
        // ...
    )

    val Night = IMEColors(
        keyBackground = Color(0xFF333333),
        keyForeground = Color(0xFFE8E8E8),
        // ...
    )
}

val LocalIMEColors = compositionLocalOf { IMEThemes.Light }
```

### 4.2 跟随系统主题

```kotlin
@Composable
fun KuaiziIMETheme(
    themeType: ThemeType = ThemeType.FollowSystem,
    content: @Composable () -> Unit,
) {
    val isDark = when (themeType) {
        ThemeType.Light -> false
        ThemeType.Night -> true
        ThemeType.FollowSystem -> isSystemInDarkTheme()
    }

    val colors = if (isDark) IMEThemes.Night else IMEThemes.Light

    CompositionLocalProvider(LocalIMEColors provides colors) {
        content()
    }
}
```

---

## 5. Java 功能完整对照

| Java 配置功能 | v4 对应 | 改进说明 |
|-------------|---------|---------|
| `Config.Immutable` | `Config` data class | 移除层叠覆盖，单一不可变配置 |
| `Config.Mutable` | `ConfigRepository.updateConfig()` | DataStore 原子更新 |
| `ConfigChangeListener` | `Flow<Config>` | 响应式更新，自动生命周期管理 |
| `ConfigKey` 枚举 | `Config` 属性 | 类型安全，编译期检查 |
| `IMEConfig` 桥接 | `ConfigRepository` | DataStore 直接管理 |
| `SharedPreferences` | `DataStore<Preferences>` | 异步、类型安全、无 ANR |
| 主题资源 (`themes.xml`, `attrs.xml`) | Compose 主题 | 声明式主题系统 |
| 60+ 主题属性 | `IMEColors` data class | 类型安全，IDE 自动补全 |
