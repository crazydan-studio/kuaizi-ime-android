# 配置界面设计

v4 版本对配置界面中与 `:ime-ui` 模块相关的 UI 组件进行设计，包括键盘预览、主题选择器、单手模式切换和快捷设置弹窗等组件。完整的设置页面设计（含搜索、分组等）属于 `:app` 模块，详见 [app/config.md](../app/config.md)。

---

## 1. 键盘预览（KeyboardPreview）

在设置页面底部展示缩小版的键盘视图，主题和手模式变更立即反映。预览区域不可交互，仅用于视觉确认。

```kotlin
/**
 * 内嵌的键盘预览。
 *
 * 在设置页面底部展示缩小版的键盘视图，主题和手模式变更立即反映。
 * 预览区域不可交互，仅用于视觉确认。
 */
@Composable
fun KeyboardPreview(config: ImeConfig) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp) // 缩小的键盘预览高度
                .clip(RoundedCornerShape(8.dp)),
        ) {
            KeyboardTheme(themeType = config.ui.themeType) {
                // 使用真实键盘 Composable，但 scale 缩小
                Box(modifier = Modifier.scale(0.5f).fillMaxSize()) {
                    StandardKeyGridPanel(
                        keyGrid = pinyinKeyGridPreview(config.engine.handMode),
                        keyboardState = KeyboardState.Idle,
                    )
                }
            }
        }
    }
}
```

---

## 2. 主题相关 UI 组件

### 2.1 ThemeSelector

主题选择器，使用横向滑动的卡片组展示三种主题模式。选中主题后，下方的键盘预览立即切换。

```kotlin
@Composable
fun ThemeSelector(
    currentTheme: ThemeType,
    onThemeSelected: (ThemeType) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ThemeType.entries.forEach { theme ->
            val isSelected = theme == currentTheme
            val isDark = when (theme) {
                ThemeType.Light -> false
                ThemeType.Night -> true
                ThemeType.FollowSystem -> isSystemInDarkTheme()
            }

            Card(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onThemeSelected(theme) },
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    }
                ),
                border = if (isSelected) {
                    CardDefaults.outlinedCardBorder()
                } else null,
            ) {
                Column(
                    modifier = Modifier.padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    // 迷你键盘预览缩略图
                    MiniKeyboardPreview(isDark = isDark)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = theme.displayName,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
        }
    }
}

val ThemeType.displayName: String
    get() = when (this) {
        ThemeType.Light -> "浅色"
        ThemeType.Night -> "深色"
        ThemeType.FollowSystem -> "跟随系统"
    }
```

### 2.2 HandModeToggle

单手模式切换，使用分段按钮（SegmentedButton）而非下拉列表。左手/右手两种模式直接展示，一键切换，无需打开选择器。

```kotlin
@Composable
fun HandModeToggle(
    currentHandMode: HandMode,
    onHandModeSelected: (HandMode) -> Unit,
) {
    ListItem(
        headlineContent = { Text("单手模式") },
        supportingContent = { Text("调整按键布局适配左手或右手操作") },
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        HandMode.entries.forEach { mode ->
            val isSelected = mode == currentHandMode
            FilterChip(
                selected = isSelected,
                onClick = { onHandModeSelected(mode) },
                label = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = when (mode) {
                                HandMode.Left -> Icons.Outlined.PanTool // 左手图标
                                HandMode.Right -> Icons.Outlined.PanTool // 右手图标（镜像）
                            },
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(mode.displayName)
                    }
                },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

val HandMode.displayName: String
    get() = when (this) {
        HandMode.Left -> "左手"
        HandMode.Right -> "右手"
    }
```

---

## 3. 快捷设置弹窗（QuickSettingsPopup）

在键盘工具栏增加「快捷设置」入口，允许用户在输入过程中快速切换高频配置，无需离开当前应用打开设置：

```kotlin
/**
 * 键盘工具栏上的快捷设置弹窗。
 *
 * 展示最常用的 3-4 个配置项，点击工具栏齿轮图标弹出。
 */
@Composable
fun QuickSettingsPopup(
    config: ImeConfig,
    onConfigChanged: (ImeConfig) -> Unit,
    onDismiss: () -> Unit,
) {
    PopupWindow(
        onDismissRequest = onDismiss,
    ) {
        Card(
            modifier = Modifier
                .width(280.dp)
                .padding(8.dp),
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "快捷设置",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(bottom = 8.dp),
                )

                // 主题快速切换
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("主题", modifier = Modifier.weight(1f))
                    Row {
                        ThemeType.entries.forEach { theme ->
                            IconButton(
                                onClick = { onConfigChanged(config.copy(ui = config.ui.copy(themeType = theme))) },
                                modifier = Modifier.size(32.dp),
                            ) {
                                Icon(
                                    imageVector = when (theme) {
                                        ThemeType.Light -> Icons.Outlined.LightMode
                                        ThemeType.Night -> Icons.Outlined.DarkMode
                                        ThemeType.FollowSystem -> Icons.Outlined.BrightnessAuto
                                    },
                                    contentDescription = theme.displayName,
                                    tint = if (config.ui.themeType == theme) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                )
                            }
                        }
                    }
                }

                HorizontalDivider()

                // 单手模式
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("单手模式", modifier = Modifier.weight(1f))
                    SegmentedButtonGroup {
                        HandMode.entries.forEach { mode ->
                            SegmentedButton(
                                selected = config.engine.handMode == mode,
                                onClick = { onConfigChanged(config.copy(engine = config.engine.copy(handMode = mode)) },
                            ) {
                                Text(mode.displayName)
                            }
                        }
                    }
                }

                HorizontalDivider()

                // X-Pad
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("X-Pad", modifier = Modifier.weight(1f))
                    Switch(
                        checked = config.ui.xPadEnabled,
                        onCheckedChange = { onConfigChanged(config.copy(ui = config.ui.copy(xPadEnabled = it))) },
                    )
                }

                HorizontalDivider()

                // 更多设置入口
                TextButton(
                    onClick = { /* 打开完整设置页面 */ },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("更多设置")
                }
            }
        }
    }
}
```

**快捷设置项选择**：

| 优先级 | 配置项 | 理由 |
|--------|--------|------|
| 1 | 主题模式 | 用户频繁在深色/浅色间切换，尤其在夜间 |
| 2 | 单手模式 | 左右手切换是 IME 特有的高频操作 |
| 3 | X-Pad 开关 | 不同应用场景下可能需要开关 X-Pad |
