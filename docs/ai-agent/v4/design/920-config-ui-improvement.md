# 920 — 配置界面改进设计

## 1. 概述

v4 版本对配置界面进行全面改进，使用户操作更加简洁、直观、快速，配置说明更加清晰、明确，配置分组更加准确，用户关注的主次更加分明。Java 版本的设置界面基于 `PreferenceFragmentCompat`，存在分组逻辑混乱、说明缺失、操作冗余、层级过深等问题，v4 版本以 Compose 重建，以用户心智模型为导向重新组织信息架构。

---

## 2. Java 版本配置界面分析

### 2.1 现有结构

Java 版本的设置分为两个页面，组织如下：

**主设置页**（`app_preferences.xml`）：

| 分组 | 配置项 |
|------|--------|
| 基本 | 主题设置（跳转子页面）、繁体优先 |
| 隐私 | 禁用输入数据记录、禁用按键弹出提示 |
| 性能（默认折叠） | 禁用按键音效、禁用按键动画、禁用翻页音效、禁用手势轨迹 |
| 关于 | 致谢、关于、捐赠、反馈、团队、更新日志、软件服务协议、版权、Alpha 用户协议 |

**主题子页面**（`app_preferences_theme.xml`）：

| 分组 | 配置项 |
|------|--------|
| 无分组 | 主题模式、手模式、启用 X-Pad、适配桌面滑动手势 |
| X-Pad | 拉丁键盘复用拼音 X-Pad 布局 |

### 2.2 问题分析

1. **分组逻辑混乱**：「基本」分组仅含主题和繁体优先两个不相关配置；「隐私」分组混入了「禁用按键弹出提示」——这本质是反馈控制而非隐私
2. **层级过深**：主题和手模式是最常用的配置，却被藏在子页面中，需要两次点击才能到达
3. **说明缺失**：大部分开关只有标题没有描述，用户无法理解功能含义。例如「禁用按键弹出提示」——什么是「弹出提示」？禁用后有什么影响？
4. **命名否定式**：所有开关用「禁用」前缀，用户需要心理反转才能理解。例如「禁用按键音效」= 关闭按键声音，双重否定更难理解
5. **主次不分**：繁体优先（少数用户需要）与主题设置（多数用户需要）在同一层级；「关于」信息占据了设置页一半的空间
6. **操作冗余**：切换主题需要跳转子页面→选择→返回，无法在主页面快速预览效果
7. **缺少搜索**：配置项增多后无搜索能力
8. **X-Pad 子分组过细**：仅一个配置项独占一个分组，造成视觉碎片化

---

## 3. v4 配置界面设计

### 3.1 设计原则

| 原则 | 说明 |
|------|------|
| **肯定式命名** | 用「启用」代替「禁用」，用正向描述让用户直接理解含义 |
| **扁平化** | 减少层级嵌套，常用配置一步可达 |
| **即时预览** | 主题、手模式等配置变更立即反映到键盘预览中 |
| **渐进式披露** | 高频配置直接展示，低频配置折叠或置于二级页面 |
| **场景化分组** | 按用户使用场景分组，而非按技术概念分组 |
| **描述完整** | 每个配置项都有标题和描述，描述说明功能含义和影响 |

### 3.2 信息架构

重新组织后的设置页面结构：

```
设置
├── 外观                          ← 高频：最直观的视觉变化
│   ├── 主题模式                  ← ListPreference → 即时预览
│   ├── 单手模式                  ← 双选项切换 → 即时预览
│   └── 键盘预览                  ← 内嵌小型键盘预览
│
├── 输入体验                      ← 高频：影响输入行为
│   ├── X-Pad 连续输入            ← Switch + 描述
│   ├── 拉丁键盘复用拼音布局      ← Switch（仅 X-Pad 启用时显示）
│   ├── 繁体异体字优先            ← Switch + 描述
│   └── 适配桌面滑动手势          ← Switch + 描述
│
├── 反馈控制                      ← 中频：个性化调节
│   ├── 按键音效                  ← Switch（肯定式：启用/关闭）
│   ├── 按键动画                  ← Switch
│   ├── 翻页提示音                ← Switch
│   ├── 按键放大提示              ← Switch（原"禁用按键弹出提示"）
│   ├── 滑行轨迹显示              ← Switch（原"禁用手势轨迹"）
│   ├── 剪贴板粘贴提示            ← Switch（原"禁用剪贴板弹出提示"）
│   └── 剪贴板提示自动关闭        ← Slider（5-60秒，仅提示启用时显示）
│
├── 数据与隐私                    ← 低频：一次性设置
│   ├── 记录输入习惯              ← Switch（原"禁用用户输入数据"，反转语义）
│   ├── 导出用户数据              ← Action → 系统文件选择器
│   └── 导入用户数据              ← Action → 系统文件选择器
│
├── 日志与诊断                    ← 低频：排障专用
│   ├── 日志等级                  ← 仅 release 可修改
│   ├── 查看日志                  ← Action → 日志浏览界面
│   ├── 导出日志                  ← Action → 系统文件选择器
│   └── 日志存储路径              ← Action → 目录选择器
│
└── 关于                          ← 低频：信息查看
    ├── 版本信息                  ← 只读
    ├── 更新日志                  ← Action → 更新日志页面
    ├── 用户反馈                  ← Action → 反馈页面
    ├── 友情赞助                  ← Action → 捐赠页面（微信/支付宝二维码）
    ├── 开源致谢                  ← Action → 致谢页面
    ├── 软件服务协议              ← Action → 协议页面
    └── 版权声明                  ← Action → 版权页面
```

### 3.3 与 Java 版本的分组对照

| Java 分组 | v4 分组 | 变更原因 |
|-----------|---------|----------|
| 基本 → 主题设置 | 外观 → 主题模式 | 扁平化，不再跳转子页面 |
| 基本 → 繁体优先 | 输入体验 → 繁体异体字优先 | 功能本质是输入行为，非基本外观 |
| 隐私 → 禁用输入数据 | 数据与隐私 → 记录输入习惯 | 语义反转，更直觉 |
| 隐私 → 禁用按键弹出提示 | 反馈控制 → 按键放大提示 | 归入反馈控制，语义反转 |
| 性能 → 禁用按键音效等 | 反馈控制 → 按键音效等 | 语义反转；「性能」分组名误导，关闭这些选项并非优化性能 |
| 主题子页面 → 手模式 | 外观 → 单手模式 | 扁平化，最常用配置直达 |
| 主题子页面 → X-Pad | 输入体验 → X-Pad 连续输入 | 功能本质是输入方式，非外观 |
| 关于 → 友情赞助 | 关于 → 友情赞助 | 保留捐赠入口，提供用户对本应用开发支持的便捷途径 |
| 关于 → Alpha 用户协议 | 移除 | v4 不再有 alpha 变体 |

### 3.4 语义反转映射

所有「禁用」前缀的配置改为「启用」语义，Config 数据模型保持 `disable*` 命名以保持内部一致性，UI 层做映射：

| Config 属性（内部） | UI 显示名称（正向） | UI 默认值 |
|---------------------|---------------------|-----------|
| `disableKeyClickedAudio` | 按键音效 | 关（Config=true → UI 关） |
| `disableKeyAnimation` | 按键动画 | 关 |
| `disableCandidatesPagingAudio` | 翻页提示音 | 关 |
| `disableKeyPopupTips` | 按键放大提示 | 关 |
| `disableGestureSlippingTrail` | 滑行轨迹显示 | 关 |
| `disableClipPopupTips` | 剪贴板粘贴提示 | 关 |
| `disableUserInputData` | 记录输入习惯 | 开（Config=false → UI 开） |

映射规则：UI 开关值 = `!config.disable*`

---

## 4. 核心组件设计

### 4.1 设置页主界面

```kotlin
@Composable
fun SettingsScreen(
    config: Config,
    onConfigChanged: (Config) -> Unit,
    onNavigate: (SettingsRoute) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp),
    ) {
        // 外观分组
        sectionHeader("外观")
        themeSelector(config, onConfigChanged)
        handModeToggle(config, onConfigChanged)
        keyboardPreview(config)

        // 输入体验分组
        sectionHeader("输入体验")
        inputSettings(config, onConfigChanged)

        // 反馈控制分组
        sectionHeader("反馈控制")
        feedbackSettings(config, onConfigChanged)

        // 数据与隐私分组（低频，折叠）
        expandableSection("数据与隐私", defaultExpanded = false) {
            dataPrivacySettings(config, onConfigChanged, onNavigate)
        }

        // 日志与诊断分组（低频，折叠）
        expandableSection("日志与诊断", defaultExpanded = false) {
            diagnosticSettings(config, onConfigChanged, onNavigate)
        }

        // 关于分组（低频，折叠）
        expandableSection("关于", defaultExpanded = false) {
            aboutSettings(onNavigate)
        }
    }
}
```

### 4.2 主题选择器（即时预览）

```kotlin
/**
 * 主题选择器，使用横向滑动的卡片组展示三种主题模式。
 * 选中主题后，下方的键盘预览立即切换。
 */
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

### 4.3 单手模式切换

```kotlin
/**
 * 单手模式切换，使用分段按钮（SegmentedButton）而非下拉列表。
 * 左手/右手两种模式直接展示，一键切换，无需打开选择器。
 */
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

### 4.4 键盘预览

```kotlin
/**
 * 内嵌的键盘预览。
 *
 * 在设置页面底部展示缩小版的键盘视图，主题和手模式变更立即反映。
 * 预览区域不可交互，仅用于视觉确认。
 */
@Composable
fun KeyboardPreview(config: Config) {
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
            KuaiziIMETheme(themeType = config.themeType) {
                // 使用真实键盘 Composable，但 scale 缩小
                Box(modifier = Modifier.scale(0.5f).fillMaxSize()) {
                    StandardKeyboard(
                        keyGrid = pinyinKeyGridPreview(config.handMode),
                        keyboardState = KeyboardState.Idle,
                        onKeyPress = { _, _ -> }, // 不可交互
                    )
                }
            }
        }
    }
}
```

### 4.5 增强型开关配置项

```kotlin
/**
 * 增强的开关配置项。
 *
 * 与 Java 版本 SwitchPreference 相比：
 * - 标题使用肯定式语义
 * - 描述完整说明功能和影响
 * - 条件显示（依赖其他配置项）
 * - 依赖项变更时自动提示
 */
@Composable
fun EnhancedSwitchPreference(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    visible: Boolean = true,
    enabled: Boolean = true,
    dependentHint: String? = null,
) {
    if (!visible) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (dependentHint != null) {
                    Text(
                        text = dependentHint,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = enabled,
            )
        }
    }
}
```

**配置项描述清单**：

| 配置项 | 标题 | 描述 |
|--------|------|------|
| `enableXPad` | X-Pad 连续输入 | 启用后可通过滑行在六边形面板上连续输入拼音 |
| `enableLatinUsePinyinKeysInXPad` | 拉丁键盘复用拼音布局 | 在拉丁键盘中使用与拼音相同的 X-Pad 布局 |
| `enableCandidateVariantFirst` | 繁体异体字优先 | 候选字中优先显示繁体和异体字形式 |
| `adaptDesktopSwipeUpGesture` | 适配桌面滑动手势 | 避免键盘上滑与桌面手势冲突 |
| `!disableKeyClickedAudio` | 按键音效 | 按下按键时播放提示音 |
| `!disableKeyAnimation` | 按键动画 | 按下按键时显示按压动画效果 |
| `!disableCandidatesPagingAudio` | 翻页提示音 | 候选字翻页时播放提示音 |
| `!disableKeyPopupTips` | 按键放大提示 | 按下按键时在上方显示放大的按键内容 |
| `!disableGestureSlippingTrail` | 滑行轨迹显示 | 滑行输入时在键盘上绘制手指移动轨迹 |
| `!disableClipPopupTips` | 剪贴板粘贴提示 | 检测到剪贴板内容时弹出粘贴提示 |
| `clipPopupTipsTimeout` | 剪贴板提示自动关闭 | 提示显示后自动关闭的等待时间 |
| `!disableUserInputData` | 记录输入习惯 | 根据输入频率优化候选字排序，关闭后不影响正常输入 |

### 4.6 条件显示逻辑

部分配置项根据其他配置项的状态条件显示，减少视觉噪音：

```kotlin
@Composable
fun InputSettings(
    config: Config,
    onConfigChanged: (Config) -> Unit,
) {
    EnhancedSwitchPreference(
        title = "X-Pad 连续输入",
        description = "启用后可通过滑行在六边形面板上连续输入拼音",
        checked = config.enableXPad,
        onCheckedChange = { onConfigChanged(config.copy(enableXPad = it)) },
    )

    // 仅当 X-Pad 启用时显示
    EnhancedSwitchPreference(
        title = "拉丁键盘复用拼音布局",
        description = "在拉丁键盘中使用与拼音相同的 X-Pad 布局",
        checked = config.enableLatinUsePinyinKeysInXPad,
        onCheckedChange = { onConfigChanged(config.copy(enableLatinUsePinyinKeysInXPad = it)) },
        visible = config.enableXPad,
        dependentHint = if (!config.enableXPad) "需先启用 X-Pad 连续输入" else null,
    )

    EnhancedSwitchPreference(
        title = "繁体异体字优先",
        description = "候选字中优先显示繁体和异体字形式",
        checked = config.enableCandidateVariantFirst,
        onCheckedChange = { onConfigChanged(config.copy(enableCandidateVariantFirst = it)) },
    )

    EnhancedSwitchPreference(
        title = "适配桌面滑动手势",
        description = "避免键盘上滑与桌面手势冲突",
        checked = config.adaptDesktopSwipeUpGesture,
        onCheckedChange = { onConfigChanged(config.copy(adaptDesktopSwipeUpGesture = it)) },
    )
}
```

### 4.7 可折叠分组

```kotlin
/**
 * 可折叠的设置分组。
 *
 * 高频分组（外观、输入体验、反馈控制）默认展开，
 * 低频分组（数据与隐私、日志与诊断、关于）默认折叠。
 * 折叠状态通过 rememberSaveable 跨重组保持。
 */
@Composable
fun LazyListScope.expandableSection(
    title: String,
    defaultExpanded: Boolean = true,
    content: @Composable () -> Unit,
) {
    item {
        var expanded by rememberSaveable { mutableStateOf(defaultExpanded) }

        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "折叠" else "展开",
                    tint = MaterialTheme.colorScheme.primary,
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                content()
            }
        }
    }
}
```

### 4.8 分组标题

```kotlin
@Composable
fun LazyListScope.sectionHeader(title: String) {
    item {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        )
    }
}
```

---

## 5. 设置搜索

### 5.1 搜索入口

设置页面顶部提供搜索栏，用户输入关键词后实时过滤匹配的配置项：

```kotlin
@Composable
fun SettingsScreen(
    config: Config,
    onConfigChanged: (Config) -> Unit,
    onNavigate: (SettingsRoute) -> Unit,
) {
    var searchQuery by rememberSaveable { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        // 搜索栏
        SearchBar(
            query = searchQuery,
            onQueryChange = { searchQuery = it },
            onSearch = {},
            active = false,
            onActiveChange = {},
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            placeholder = { Text("搜索设置") },
        ) {}

        // 设置列表（搜索过滤后）
        SettingsList(
            config = config,
            onConfigChanged = onConfigChanged,
            onNavigate = onNavigate,
            filter = searchQuery,
        )
    }
}
```

### 5.2 搜索索引

每个配置项定义可搜索的关键词，包括标题、描述和别名：

```kotlin
data class SearchableSetting(
    val key: String,
    val title: String,
    val description: String,
    val aliases: List<String>, // 搜索别名
)

val searchableSettings = listOf(
    SearchableSetting("theme", "主题模式", "选择浅色、深色或跟随系统主题", listOf("深色模式", "夜间模式", "dark mode")),
    SearchableSetting("hand_mode", "单手模式", "调整按键布局适配左手或右手操作", listOf("左手", "右手", "单手")),
    SearchableSetting("enable_x_pad", "X-Pad 连续输入", "启用后可通过滑行在六边形面板上连续输入拼音", listOf("xpad", "六边形", "滑行输入", "连续输入")),
    SearchableSetting("variant_first", "繁体异体字优先", "候选字中优先显示繁体和异体字形式", listOf("繁体", "异体字", "简繁")),
    SearchableSetting("key_audio", "按键音效", "按下按键时播放提示音", listOf("声音", "音效", "按键声")),
    SearchableSetting("key_animation", "按键动画", "按下按键时显示按压动画效果", listOf("动画", "特效")),
    SearchableSetting("popup_tips", "按键放大提示", "按下按键时在上方显示放大的按键内容", listOf("放大", "弹出", "提示")),
    SearchableSetting("gesture_trail", "滑行轨迹显示", "滑行输入时在键盘上绘制手指移动轨迹", listOf("轨迹", "滑行线", "手势")),
    SearchableSetting("clip_tips", "剪贴板粘贴提示", "检测到剪贴板内容时弹出粘贴提示", listOf("剪贴板", "粘贴", "复制")),
    SearchableSetting("input_data", "记录输入习惯", "根据输入频率优化候选字排序", listOf("隐私", "数据", "频率", "学习")),
)
```

---

## 6. 快速切换

### 6.1 键盘上的快捷入口

在键盘工具栏增加「快捷设置」入口，允许用户在输入过程中快速切换高频配置，无需离开当前应用打开设置：

```kotlin
/**
 * 键盘工具栏上的快捷设置弹窗。
 *
 * 展示最常用的 3-4 个配置项，点击工具栏齿轮图标弹出。
 */
@Composable
fun QuickSettingsPopup(
    config: Config,
    onConfigChanged: (Config) -> Unit,
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
                                onClick = { onConfigChanged(config.copy(themeType = theme)) },
                                modifier = Modifier.size(32.dp),
                            ) {
                                Icon(
                                    imageVector = when (theme) {
                                        ThemeType.Light -> Icons.Outlined.LightMode
                                        ThemeType.Night -> Icons.Outlined.DarkMode
                                        ThemeType.FollowSystem -> Icons.Outlined.BrightnessAuto
                                    },
                                    contentDescription = theme.displayName,
                                    tint = if (config.themeType == theme) {
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
                                selected = config.handMode == mode,
                                onClick = { onConfigChanged(config.copy(handMode = mode)) },
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
                        checked = config.enableXPad,
                        onCheckedChange = { onConfigChanged(config.copy(enableXPad = it)) },
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

### 6.2 快捷设置项选择

快捷设置弹窗仅展示 3 项最高频配置：

| 优先级 | 配置项 | 理由 |
|--------|--------|------|
| 1 | 主题模式 | 用户频繁在深色/浅色间切换，尤其在夜间 |
| 2 | 单手模式 | 左右手切换是 IME 特有的高频操作 |
| 3 | X-Pad 开关 | 不同应用场景下可能需要开关 X-Pad |

---

## 7. Java 功能完整对照

| Java 配置 UI | v4 对应 | 改进说明 |
|-------------|---------|---------|
| `PreferenceFragmentCompat` | Compose `SettingsScreen` | 声明式，灵活布局 |
| `PreferenceScreen` 跳转 | 扁平化 `LazyColumn` | 减少页面跳转 |
| `SwitchPreferenceCompat` | `EnhancedSwitchPreference` | 增加描述、条件显示 |
| `ListPreference` 主题选择 | `ThemeSelector` 卡片+即时预览 | 可视化选择 |
| `ListPreference` 手模式 | `HandModeToggle` 分段按钮 | 一键切换 |
| `PreferenceCategory` 固定分组 | `expandableSection` 可折叠分组 | 低频配置折叠 |
| 否定式命名（禁用） | 肯定式命名（启用） | 直觉理解 |
| 无描述 | 每项完整描述 | 说明功能和影响 |
| 无搜索 | `SearchBar` + 搜索索引 | 快速定位配置 |
| 无键盘快捷入口 | `QuickSettingsPopup` | 输入中快速切换 |
| 无键盘预览 | `KeyboardPreview` 内嵌预览 | 配置变更即时可视化 |
| `AboutDonate`（微信/支付宝二维码） | `DonateScreen`（Compose） | 保留捐赠入口，简化交互 |
| Alpha 用户协议 | 移除 | v4 无 alpha 变体 |
| 12 个 About Activity | `AboutScreen` + Navigation | 单 Activity |
