# 配置管理设计

使用 DataStore + Flow 实现类型安全的配置管理和响应式更新。`ImeConfig` 包含 `EngineConfig`（引擎配置）、`UiConfig`（UI 配置）和 `RuntimeConfig`（运行时配置）三个嵌套 data class，引擎与 UI 配置在数据结构上明确隔离。`:app` 模块的 `ConfigDataStore` 仅持久化 `EngineConfig` 和 `UiConfig`，`RuntimeConfig` 不做持久化。

---

## 1. ImeConfig 运行时与持久化配置

`ImeConfig` 包含 `EngineConfig`、`UiConfig` 和 `RuntimeConfig` 三个嵌套配置。`EngineConfig` 和 `UiConfig` 由 `ConfigDataStore` 持久化，`RuntimeConfig` 不做持久化，应用重启时根据 `StartupConfig` 重新初始化。

> `ImeConfig` 定义在 `:engine` 库中，包含 `EngineConfig`、`UiConfig` 和 `RuntimeConfig`。完整的 ImeConfig 定义见 [010-引擎库设计总览](../engine/010-engine-overview.md) §3.2。

---

## 2. ConfigDataStore（DataStore 实现）

```kotlin
class ConfigDataStore(private val context: Context) {
    private val Context.dataStore by preferencesDataStore("ime_config")

    val config: Flow<ImeConfig> = context.dataStore.data.map { prefs ->
        ImeConfig(
            engine = EngineConfigDataStoreKeys.readEngineConfig(prefs),
            ui = UiConfigDataStoreKeys.readUiConfig(prefs),
        )
    }

    /**
     * 增量更新配置：仅将变化部分写入 DataStore。
     * 
     * 通过比较新旧配置的差异，仅序列化并写入发生变化的字段，
     * 避免每次 updateConfig 都全量写入 ~20 个 key。
     * 适用于高频配置切换场景（如主题、音效开关的快速连续切换）。
     */
    suspend fun updateConfig(transform: (ImeConfig) -> ImeConfig) {
        context.dataStore.edit { prefs ->
            val current = config.first()
            val new = transform(current)
            writeChangedEngineConfig(prefs, current.engine, new.engine)
            writeChangedUiConfig(prefs, current.ui, new.ui)
        }
    }

    private fun writeChangedEngineConfig(
        prefs: MutablePreferences,
        old: EngineConfig,
        new: EngineConfig,
    ) {
        if (old.inputPredictionEnabled != new.inputPredictionEnabled)
            prefs[inputPredictionEnabledKey] = new.inputPredictionEnabled
        if (old.userDataPersistEnabled != new.userDataPersistEnabled)
            prefs[userDataPersistEnabledKey] = new.userDataPersistEnabled
        if (old.favoriteInputEnabled != new.favoriteInputEnabled)
            prefs[favoriteInputEnabledKey] = new.favoriteInputEnabled
        if (old.favoriteClipEnabled != new.favoriteClipEnabled)
            prefs[favoriteClipEnabledKey] = new.favoriteClipEnabled
        if (old.favoriteSyncToUserDictEnabled != new.favoriteSyncToUserDictEnabled)
            prefs[favoriteSyncToUserDictEnabledKey] = new.favoriteSyncToUserDictEnabled
        if (old.candidateVariantFirstEnabled != new.candidateVariantFirstEnabled)
            prefs[candidateVariantFirstEnabledKey] = new.candidateVariantFirstEnabled
        if (old.logLevel != new.logLevel)
            prefs[logLevelKey] = new.logLevel
        if (old.logStoragePath != new.logStoragePath)
            prefs[logStoragePathKey] = new.logStoragePath
    }

    private fun writeChangedUiConfig(
        prefs: MutablePreferences,
        old: UiConfig,
        new: UiConfig,
    ) {
        if (old.keyboardInputMode != new.keyboardInputMode)
            prefs[keyboardInputModeKey] = new.keyboardInputMode.name
        if (old.keyboardHandMode != new.keyboardHandMode)
            prefs[keyboardHandModeKey] = new.keyboardHandMode.name
        if (old.keyboardThemeType != new.keyboardThemeType)
            prefs[keyboardThemeTypeKey] = new.keyboardThemeType.name
        if (old.keyPopupTipsEnabled != new.keyPopupTipsEnabled)
            prefs[keyPopupTipsEnabledKey] = new.keyPopupTipsEnabled
        if (old.audioFeedbackEnabled != new.audioFeedbackEnabled)
            prefs[audioFeedbackEnabledKey] = new.audioFeedbackEnabled
        if (old.hapticFeedbackEnabled != new.hapticFeedbackEnabled)
            prefs[hapticFeedbackEnabledKey] = new.hapticFeedbackEnabled
        if (old.keyAnimationEnabled != new.keyAnimationEnabled)
            prefs[keyAnimationEnabledKey] = new.keyAnimationEnabled
        if (old.gestureSlippingTrailEnabled != new.gestureSlippingTrailEnabled)
            prefs[gestureSlippingTrailEnabledKey] = new.gestureSlippingTrailEnabled
        if (old.clipPopupTipsEnabled != new.clipPopupTipsEnabled)
            prefs[clipPopupTipsEnabledKey] = new.clipPopupTipsEnabled
        if (old.clipPastePopupTipsEnabled != new.clipPastePopupTipsEnabled)
            prefs[clipPastePopupTipsEnabledKey] = new.clipPastePopupTipsEnabled
        if (old.clipPopupTipsTimeout != new.clipPopupTipsTimeout)
            prefs[clipPopupTipsTimeoutKey] = new.clipPopupTipsTimeout
        if (old.adaptDesktopSwipeUpGesture != new.adaptDesktopSwipeUpGesture)
            prefs[adaptDesktopSwipeUpGestureKey] = new.adaptDesktopSwipeUpGesture
        if (old.candidatesPagingAudioEnabled != new.candidatesPagingAudioEnabled)
            prefs[candidatesPagingAudioEnabledKey] = new.candidatesPagingAudioEnabled
        if (old.practicePlaybackSpeed != new.practicePlaybackSpeed)
            prefs[practicePlaybackSpeedKey] = new.practicePlaybackSpeed
        if (old.practiceShowFingerOverlay != new.practiceShowFingerOverlay)
            prefs[practiceShowFingerOverlayKey] = new.practiceShowFingerOverlay
        if (old.practiceShowSwipeTrail != new.practiceShowSwipeTrail)
            prefs[practiceShowSwipeTrailKey] = new.practiceShowSwipeTrail
    }
}
```

> **完整字段覆盖**：所有 16 个 UiConfig 字段均被差异检查覆盖。枚举类型（KeyboardInputMode、KeyboardHandMode、KeyboardThemeType）通过 `name` 序列化。

> `EngineConfigDataStoreKeys` 和 `UiConfigDataStoreKeys` 由 `:app-codegen` 模块通过 KSP 自动生成，详见 [010-代码生成](../app-codegen/010-codegen.md)。

> **性能说明**：增量写入策略：仅将变化字段的 Preferences.Key 写入 DataStore edit {} 块。单字段变更从 20 次 put 降至 1 次 put，减少 protobuf 序列化体积和磁盘 I/O。注意 DataStore 的 edit {} 仍会对完整的 MutablePreferences 做 protobuf 序列化，但减少 put 次数降低了 Preference 树的构建开销。

---

## 3. 主题系统

### 3.1 主题定义

`KeyboardColors`、`KeyboardThemes`、`KeyboardTheme` 和 `LocalKeyboardColors` 的完整定义见 `:ui` 模块的 [040-Compose 组件](../ui/040-compose-components.md#主题系统)。此处仅说明其在 `:app` 模块中的持久化和配置流程：

```kotlin
// :app 模块保存用户选择的主题类型到 DataStore
val themeTypeKey = stringPreferencesKey("ui_keyboard_theme_type")

suspend fun saveThemeType(type: KeyboardThemeType) {
    context.dataStore.edit { prefs ->
        prefs[themeTypeKey] = type.name
    }
}
```

主题通过 `ImeEngine.updateConfig()` 的 `UiConfig.keyboardThemeType` 字段同步到 `ImeState.config`，UI 层通过 `KeyboardTheme` Composable 消费。

### 3.2 跟随系统主题

```kotlin
@Composable
fun KeyboardTheme(
    type: KeyboardThemeType = KeyboardThemeType.FollowSystem,
    content: @Composable () -> Unit,
) {
    val isDark = when (type) {
        KeyboardThemeType.Light -> false
        KeyboardThemeType.Night -> true
        KeyboardThemeType.FollowSystem -> isSystemInDarkTheme()
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

| ImeConfig.EngineConfig 属性 | UI 显示名称（正向） | UI 默认值 | 映射关系 |
|-----------------------------|---------------------|-----------|----------|
| `inputPredictionEnabled` | 输入预测 | 开 | 直接映射 |
| `userDataPersistEnabled` | 记录输入习惯 | 开 | 直接映射 |
| `favoriteInputEnabled` | 收藏输入 | 开 | 直接映射 |
| `favoriteClipEnabled` | 收藏剪贴板 | 开 | 直接映射 |
| `favoriteSyncToUserDictEnabled` | 收藏同步到用户词典 | 开 | 直接映射 |
| `candidateVariantFirstEnabled` | 异体字优先 | 关 | 直接映射 |
| `logLevel` | 日志级别 | WARN | 直接映射 |
| `logStoragePath` | 日志存储路径 | null | 直接映射 |

| ImeConfig.UiConfig 属性 | UI 显示名称（正向） | UI 默认值 | 映射关系 |
|-------------------------|---------------------|-----------|----------|
| `keyboardInputMode` | 键盘输入模式 | Pinyin | 直接映射 |
| `keyboardHandMode` | 键盘手 mode | Right | 直接映射 |
| `keyboardThemeType` | 键盘主题 | 跟随系统 | 直接映射 |
| `audioFeedbackEnabled` | 按键音效 | 开 | 直接映射 |
| `hapticFeedbackEnabled` | 触觉反馈 | 开 | 直接映射 |
| `keyAnimationEnabled` | 按键动画 | 开 | 直接映射 |
| `candidatesPagingAudioEnabled` | 翻页提示音 | 开 | 直接映射 |
| `keyPopupTipsEnabled` | 按键放大提示 | 开 | 直接映射 |
| `gestureSlippingTrailEnabled` | 滑行轨迹显示 | 开 | 直接映射 |
| `clipPopupTipsEnabled` | 剪贴板收藏弹出提示 | 开 | 直接映射 |
| `clipPastePopupTipsEnabled` | 可粘贴内容弹出提示 | 开 | 直接映射 |
| `clipPopupTipsTimeout` | 剪贴板弹出提示超时 | 15 | 直接映射 |
| `adaptDesktopSwipeUpGesture` | 桌面下滑手势适配 | 关 | 直接映射 |
| `practicePlaybackSpeed` | 练习回放速度倍率 | 1.0f | 直接映射 |
| `practiceShowFingerOverlay` | 练习手指覆盖层显示 | 开 | 直接映射 |
| `practiceShowSwipeTrail` | 练习滑行轨迹显示 | 开 | 直接映射 |

映射规则：
- 所有字段统一使用肯定式命名（`*Enabled` 后缀）：UI 开关值 = `config.engine.*Enabled` 或 `config.ui.*Enabled`
