# 配置界面

`:ime-ui` 模块提供一组与配置相关的 UI 组件，用于在键盘界面和设置页面中展示和修改配置项。这些组件遵循声明式设计原则，通过读取 `ImeConfig` 状态驱动渲染，配置变更通过 `KeyboardViewModel.updateConfig()` 写入运行时配置。持久化由 `:app` 模块的 `ConfigDataStore` 负责，UI 组件不直接处理持久化逻辑。

---

## 1. KeyboardPreview 键盘预览

`KeyboardPreview` 是一个非交互式的缩放键盘视图，用于设置页面中实时预览主题和手模式变更效果。该组件渲染一个按比例缩小的完整键盘布局，但不响应任何触摸事件。

```kotlin
@Composable
fun KeyboardPreview(
    config: ImeConfig,
    modifier: Modifier = Modifier,
)
```

### 缩放策略

`KeyboardPreview` 使用 `FitWidth` 缩放策略：以容器宽度为基准，按照标准键盘的宽高比（通常为 2.5:1）计算高度，确保预览视图在不同屏幕尺寸下保持一致的纵横比。缩放比例通过 `Density.density` 计算得出，按键尺寸、间距和圆角等视觉参数同步缩放，确保预览效果与实际键盘一致。

### 实时响应

`KeyboardPreview` 通过 `collectAsState()` 订阅 `ImeConfig` 的变更。当用户在 `ThemeSelector` 或 `KeyboardHandModeToggle` 中切换选项时，配置变更立即反映到预览视图中，无需重新创建组件。预览视图不加载字典数据，候选栏和输入栏显示占位内容，仅按键布局和主题色彩是真实的渲染结果。

### 键盘内容

预览视图根据 `config.engine.keyboardType` 渲染对应类型的按键布局。默认显示拼音键盘的 RectGrid 布局。手模式（`config.ui.keyboardHandMode`）变更时，按键布局会相应调整按键的排列偏移，模拟单手操作下的按键位置变化。主题变更时，所有按键的背景色、前景色和圆角等视觉属性立即更新。

---

## 2. ThemeSelector 主题选择器

`ThemeSelector` 提供横向滚动的主题卡片选择界面，支持浅色、深色和跟随系统三种主题模式。每个主题卡片包含一个缩略键盘预览和主题名称标签。

```kotlin
@Composable
fun ThemeSelector(
    currentTheme: KeyboardThemeType,
    onThemeSelected: (KeyboardThemeType) -> Unit,
    modifier: Modifier = Modifier,
)
```

### 主题卡片

三种主题模式分别渲染为独立的卡片：

| 模式 | 标签 | 预览内容 |
|------|------|---------|
| `KeyboardThemeType.Light` | 浅色 | 使用 `KeyboardThemes.Light` 色彩的缩略键盘 |
| `KeyboardThemeType.Night` | 深色 | 使用 `KeyboardThemes.Night` 色彩的缩略键盘 |
| `KeyboardThemeType.FollowSystem` | 跟随系统 | 根据当前系统模式动态切换的缩略键盘 |

每个卡片内嵌一个 `MiniKeyboardPreview`，该组件是 `KeyboardPreview` 的轻量版本，仅渲染 2-3 行按键的缩略图，高度不超过 80dp。卡片选中时显示 `KeyboardColors.themeSelectedBorder` 色彩的边框，未选中时无边框。

### 交互行为

用户点击卡片时，`ThemeSelector` 调用 `onThemeSelected` 回调，将选中的 `KeyboardThemeType` 传递给上层组件。上层组件（通常是设置页面）通过 `KeyboardViewModel.updateConfig()` 修改 `ImeConfig.ui.keyboardThemeType` 字段。配置变更后，`KeyboardTheme` 可组合函数自动切换 `LocalKeyboardColors` 的值，所有订阅主题色彩的组件立即重组。

### 横向滚动

当屏幕宽度不足以同时显示三张卡片时，`ThemeSelector` 使用 `LazyRow` 实现横向滚动。卡片宽度固定为 120dp，卡片间距 12dp，左右两端各保留 16dp 的内边距。滚动使用 `FlingBehavior` 控制减速曲线，确保滑动操作的流畅感。

---

## 3. KeyboardHandModeToggle 单手模式切换

`KeyboardHandModeToggle` 提供左右手模式的分段选择控件，以 `FilterChip` 组的形式呈现。该组件用于设置页面和快捷设置弹窗中切换手模式。

```kotlin
@Composable
fun KeyboardHandModeToggle(
    currentHandMode: KeyboardHandMode,
    onHandModeSelected: (KeyboardHandMode) -> Unit,
    modifier: Modifier = Modifier,
)
```

### 分段选择

`KeyboardHandModeToggle` 使用两个 `FilterChip` 并排组成分段选择器：

| 模式 | 标签 | 图标 |
|------|------|------|
| `KeyboardHandMode.Left` | 左手 | 左手图标 |
| `KeyboardHandMode.Right` | 右手 | 右手图标 |

选中状态的 `FilterChip` 使用 `KeyboardColors.handModeSelectedBackground` 和 `KeyboardColors.handModeSelectedForeground`，未选中状态使用默认的 `FilterChip` 样式。两个 `FilterChip` 之间无间距，通过 `Row` 组合形成连续的分段外观。

### 配置变更

用户选择手模式后，`KeyboardHandModeToggle` 调用 `onHandModeSelected` 回调。上层组件通过 `KeyboardViewModel.updateConfig()` 修改 `ImeConfig.ui.keyboardHandMode` 字段。手模式变更影响按键布局中功能键的排列位置：右手模式下功能键集中在右侧，左手模式下功能键集中在左侧。布局变更由 `KeyTableGenerator` 在下次生成按键表时应用。

---

## 4. QuickSettingsPopup 快捷设置弹窗

`QuickSettingsPopup` 是从键盘工具栏弹出的快捷设置面板，提供高频配置项的快速切换入口。弹窗以 `Popup` 形式展示，点击弹窗外部区域关闭。

```kotlin
@Composable
fun QuickSettingsPopup(
    config: ImeConfig,
    onConfigChanged: (ImeConfig) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
)
```

### 设置项列表

快捷设置弹窗按优先级排列以下配置项：

| 优先级 | 设置项 | 控件类型 | 说明 |
|--------|--------|---------|------|
| 1 | 主题模式 | 三选一分段 | 浅色 / 深色 / 跟随系统，高频切换需求 |
| 2 | 手模式 | 二选一分段 | 左手 / 右手，输入法特有高频操作 |
| 3 | 「更多设置」按钮 | 文本按钮 | 跳转至系统设置页面 |

### 交互行为

每项设置变更后，`QuickSettingsPopup` 立即调用 `onConfigChanged` 回调传递新的 `ImeConfig` 实例。由于弹窗通过 `KeyboardViewModel.updateConfig()` 修改运行时配置，变更立即生效且不触发持久化——持久化在 `:app` 层的 `ConfigDataStore` 中异步完成。「更多设置」按钮点击后，调用系统 `Settings.ACTION_INPUT_METHOD_SETTINGS` Intent 跳转至系统输入法设置页面，同时调用 `onDismiss` 关闭弹窗。

### 弹窗定位

`QuickSettingsPopup` 定位在触发工具按钮的上方，使用 `Popup` 的 `alignment` 参数控制偏移。弹窗宽度为 240dp，高度根据设置项数量自适应。弹窗背景使用 `KeyboardColors.popupBackground`，圆角为 12dp。弹窗出现和消失时带有缩放动画（从 0.9 倍放大到 1.0 倍），过渡时长 150ms。
