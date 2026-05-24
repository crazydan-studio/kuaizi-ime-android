# 配置管理设计

v4 版本使用 DataStore + Flow 实现类型安全的配置管理和响应式更新。`ImeConfig` 包含 `EngineConfig`（引擎配置）、`UiConfig`（UI 配置）和 `RuntimeConfig`（运行时配置）三个嵌套 data class，引擎与 UI 配置在数据结构上明确隔离。`:app` 模块的 `ConfigDataStore` 仅持久化 `EngineConfig` 和 `UiConfig`，`RuntimeConfig` 不做持久化。

---

## 1. ImeConfig 运行时与持久化配置

`ImeConfig` 包含 `EngineConfig`、`UiConfig` 和 `RuntimeConfig` 三个嵌套配置。`EngineConfig` 和 `UiConfig` 由 `ConfigDataStore` 持久化，`RuntimeConfig` 不做持久化，应用重启时根据 `StartupConfig` 重新初始化。

> `ImeConfig` 定义在 `:ime-engine` 库中，包含 `EngineConfig`、`UiConfig` 和 `RuntimeConfig`。完整的 ImeConfig 定义见[010-引擎库设计总览](../engine/010-engine-overview.md) §3.2。

---

## 2. ConfigDataStore（DataStore 实现）

```kotlin
class ConfigDataStore(private val context: Context) {
    private val Context.dataStore by preferencesDataStore("ime_config")

    val config: Flow<ImeConfig> = context.dataStore.data.map { prefs ->
        ImeConfig(
            engine = EngineConfigDataStoreKeys.readEngineConfig(prefs),
            ui = UiConfigDataStoreKeys.readUiConfig(prefs),
            // runtime 不做持久化，始终使用默认值
        )
    }

    suspend fun updateConfig(transform: (ImeConfig) -> ImeConfig) {
        context.dataStore.edit { prefs ->
            val current = config.first()
            val new = transform(current)
            EngineConfigDataStoreKeys.writeEngineConfig(prefs, new.engine)
            UiConfigDataStoreKeys.writeUiConfig(prefs, new.ui)
            // runtime 不做持久化
        }
    }
}
```

> **注意**：ConfigDataStore 仅持久化 `EngineConfig` 和 `UiConfig`，`RuntimeConfig` 不做持久化。`EngineConfigDataStoreKeys` 和 `UiConfigDataStoreKeys` 由 `:ime-codegen` 模块通过 KSP 自动生成，详见 [架构/模块划分](../architecture/030-module-division.md)。

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
    themeType: KeyboardThemeType = KeyboardThemeType.FollowSystem,
    content: @Composable () -> Unit,
) {
    val isDark = when (themeType) {
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
| `keyboardInputMode` | 键盘输入模式 | XPad | 直接映射 |
| `keyboardHandMode` | 键盘手 mode | Right | 直接映射 |
| `keyboardThemeType` | 键盘主题 | 跟随系统 | 直接映射 |
| `audioFeedbackEnabled` | 按键音效 | 开 | 直接映射 |
| `hapticFeedbackEnabled` | 触觉反馈 | 开 | 直接映射 |
| `keyAnimationEnabled` | 按键动画 | 开 | 直接映射 |
| `candidatesPagingAudioEnabled` | 翻页提示音 | 开 | 直接映射 |
| `keyPopupTipsEnabled` | 按键放大提示 | 开 | 直接映射 |
| `gestureSlippingTrailEnabled` | 滑行轨迹显示 | 开 | 直接映射 |
| `clipPopupTipsEnabled` | 剪贴板粘贴提示 | 开 | 直接映射 |

映射规则：
- 所有字段统一使用肯定式命名（`*Enabled` 后缀）：UI 开关值 = `config.engine.*Enabled` 或 `config.ui.*Enabled`
