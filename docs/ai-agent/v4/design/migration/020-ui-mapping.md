# UI 库 `:ime-ui` 迁移对照

本文档整合所有 Java → v4 UI 层（`:ime-ui`）的迁移对比内容，涵盖 UI 层 class 迁移、View 系统迁移、输入面板迁移和配置界面迁移四个维度。

> 内容来源：170 §7.3、150 §2、400 §2/§8、920 §2/§7

---

## 1. UI 层 class 迁移

> 来源：170 §7.3

| Java Class | v4 对应 | 变更说明 |
|-----------|---------|----------|
| `MainboardView` | `KeyboardPanel` / `KeyboardScreen` | 两者均为完整输入法组件（含候选栏/输入栏/工具栏 + 键盘区域），叠加/全屏两种布局模式，合并原 `ThreeLayerKeyboardArea` 和 `InputScreen` 职责 |
| `KeyboardView` | `KeyGridPanel` + `GestureInputPanel` + `GestureFeedbackPanel` | 三层分离替代单 View：按键渲染层 + 透明手势拦截层 + 透明反馈绘制层 |
| `KeyboardViewGestureListener` | `GestureDetectorLayer` | Compose `Modifier.pointerInput` 手势检测替代 View 手势监听 |
| `KeyboardViewKeyAnimator` | `KeyView`（状态渲染）+ `GestureFeedbackPanel`（临时反馈） | 持续性状态与临时性反馈分离：KeyView 渲染按键常规状态（按下态、激活态、禁用态），GestureFeedbackPanel 绘制临时手势反馈（滑行轨迹、按键高亮） |
| `CandidatesView` | `CandidateListPanel` | Compose `LazyRow` 替代 FlexboxLayout + 自定义分页 |
| `InputListView` | `InputListPanel` | Compose `LazyRow` 替代自定义 RecyclerView |
| `InputListViewReadonly` | （已废弃） | 只读模式通过 `ImeConfig` 控制，无需单独 View |
| `FavoriteboardView` | `FavoriteListPanel` | Compose `LazyColumn` 替代自定义 RecyclerView |
| `InputQuickListView` | `CandidateListPanel`（补全候选） | 功能合并到候选栏 |
| `XPadView` | `XPadView`（`:ime-ui` keyboard） | Compose Canvas 重构，移除 Mixite 依赖 |
| `XPadState` | `GestureFeedbackState` | 反馈状态管理重构，集成到三层面板架构 |
| `BaseThemedView` | `KeyboardTheme` | Compose 主题系统替代自定义 View 主题 |
| `BaseMsgListenerView` | （已废弃） | MVI 模式下 View 不再监听消息，通过 StateFlow 自动更新 |

---

## 2. View 系统迁移

> 来源：400 §2 + §8

### Java View 体系概览

| 组件 | Java 实现 | 代码量 | 复杂度 | v4 Compose 对应 |
|------|----------|--------|--------|----------------|
| **KeyboardView** | 自定义 RecyclerView + LayoutManager | ~800 行 | 高 | `StandardKeyGridPanel` + `KeyView` |
| **InputListView** | 自定义 RecyclerView + LayoutManager | ~600 行 | 高 | `InputListPanel` + `LazyRow` |
| **CandidatesView** | FlexboxLayout + 自定义分页 | ~400 行 | 中 | `CandidateListPanel` + `LazyRow` |
| **FavoriteboardView** | 自定义 RecyclerView | ~300 行 | 中 | `FavoriteListPanel` + `LazyColumn` |
| **XPadView** | 自定义 Canvas 绘制 | ~500 行 | 高 | `XPadView` + Compose `Canvas` |
| **MainboardView** | 组合容器 | ~200 行 | 低 | `KeyboardPanel` / `KeyboardScreen` |
| **InputboardView** | 组合容器 | ~150 行 | 低 | `InputListPanel` |
| **ViewGestureDetector** | 自定义手势检测 | ~300 行 | 高 | `Modifier.pointerInput` |
| **ViewGestureTrailer** | 手势轨迹绘制 | ~200 行 | 中 | `GestureFeedbackPanel`（TouchTrail 元素） |
| **13 种 ViewHolder** | 各类按键和输入的视图 | ~1500 行 | 中 | `KeyContent()` / `InputItem()` 分支 |

### Java 功能完整对照

| Java UI 组件 | v4 Compose 对应 | 变更说明 |
|-------------|----------------|----------|
| `MainboardView` | `KeyboardPanel`（叠加模式）/ `KeyboardScreen`（全屏模式） | 两者均为完整输入法组件（含候选栏/输入栏/工具栏），叠加/全屏两种布局模式，合并原 `ThreeLayerKeyboardArea` 和 `InputScreen` 职责 |
| `KeyboardView` + `KeyboardViewAdapter` | `StandardKeyGridPanel` + `KeyView` | 移除 Adapter/ViewHolder 模式 |
| `KeyboardViewLayoutManager` | Compose `Row`/`Column` + `Modifier.weight` | 移除自定义 LayoutManager |
| `KeyboardViewGestureListener` | `Modifier.pointerInput` | Compose 手势 API |
| `KeyboardViewKeyAnimator` | Compose 动画 API | 声明式动画 |
| 12 种 `KeyViewHolder` | `KeyContent()` 分支 | 按类型分发 Composable，移除 ViewHolder 体系 |
| `InputListView` + `InputListViewAdapter` | `InputListPanel` + `LazyRow` | 简化 |
| 5 种 `InputViewHolder` | `InputItem()` 分支 | 按类型分发 Composable |
| `InputQuickListView` | `QuickListPopup` | 浮层 |
| `InputFavoriteListView` | `FavoritesList` + `LazyColumn` | 简化 |
| `CandidatesView` | `CandidateListPanel` + `LazyRow` | FlexboxLayout → Compose |
| `FavoriteboardView` | `FavoritesScreen` | Compose |
| `XPadView` + `XPainter` 系列 | `XPadView` + Compose `Canvas` | 统一绘制 API，移除 XPainter 体系 |
| `ViewGestureDetector` | `Modifier.pointerInput` | 标准手势 API |
| `ViewGestureTrailer` | `GestureFeedbackPanel`（TouchTrail 元素） | 三层分离设计（详见 [020-面板三层分离设计](../ui/020-panel-separation.md)） |
| `ShadowDrawable` / `HexagonDrawable` | Compose `drawBehind` | 声明式绘制 |
| `AudioPlayer` | Compose `LocalHapticFeedback` + 音频 | 扩展触觉反馈 |
| `DialogAlert` / `DialogConfirm` | Compose `AlertDialog` | 标准 Dialog |
| `Toast` | Compose `Snackbar` | 标准反馈 |
| `HtmlTextView` | `AnnotatedString.appendHtml()` | Compose 原生 HTML |
| `Preferences` | `SettingsScreen` | Compose 设置页 |
| `Guide` | `MainScreen` | Compose + Navigation |
| 12 个 About Activity | `AboutScreen` + Navigation | 单 Activity，移除 `AlphaUserAgreement` |

### View 与模型层交互对比

| 维度 | Java 版本 | v4 版本 |
|------|----------|---------|
| 数据流 | View → `UserKeyMsg` → `IMEditorView` → `IMEService` → `IMEditor` | `GestureInputPanel` → `InputGesture` → `ImeEngine.handleGesture()` |
| UI 更新 | `IMEditor` → `InputMsg` → `IMEService` → `IMEditorView` → View 手动更新 | `ImeEngine.state: StateFlow<ImeState>` → Compose 自动重组 |
| 操作方式 | 命令式：`setVisibility()`、`setText()`、`setAdapter()` | 声明式：状态驱动自动重组 |

**历史原因**：Java 版本使用传统 View 系统（自定义 View、RecyclerView、FlexboxLayout 等），每次 UI 更新都需要手动分发 InputMsg 并更新对应的 View。命令式操作大量 `setVisibility()`、`setText()`、`setAdapter()` 等，代码冗余且易出错。`KeyboardView` 作为自定义 RecyclerView 内嵌了 LayoutManager、Adapter、GestureListener 和 KeyAnimator，职责高度耦合。13 种 ViewHolder 代码量约 1500 行。v4 通过 Compose 声明式 UI 消除了手动更新逻辑，通过三层面板分离（[020-面板三层分离设计](../ui/020-panel-separation.md)）解耦了手势检测、按键渲染和视觉反馈。

---

## 3. 输入面板迁移

> 来源：150 §2

Java 版本中，按键的绘制、手势检测、手势反馈和输入处理高度耦合在 `KeyboardView` 及其相关组件中。v4 将其分离为三个独立层。

### Java 版本耦合结构

| 组件 | 耦合的职责 | v4 分离后 |
|------|----------|----------|
| `KeyboardView`（RecyclerView） | 六边形网格布局 + ViewHolder 管理 + 手势检测 + 按键动画 + 滑行轨迹绘制 | 拆分为三层：`KeyGridPanel`（渲染）+ `GestureInputPanel`（手势）+ `GestureFeedbackPanel`（反馈） |
| `KeyboardViewLayoutManager` | 六边形网格布局 | Compose `Row`/`Column` + `Modifier.weight` |
| `KeyboardViewAdapter` | ViewHolder 管理 | 移除，Compose 条件分支替代 |
| `KeyboardViewGestureListener` | 手势检测 + 按键查找 + 消息生成 | `GestureDetectorLayer`（`Modifier.pointerInput`） |
| `KeyboardViewKeyAnimator` | 按键动画（状态反馈） | `KeyView`（持续性状态）+ `GestureFeedbackPanel`（临时反馈） |
| `RecyclerViewGestureTrailer` | 滑行轨迹绘制（ItemDecoration） | `GestureFeedbackPanel`（TouchTrail 元素） |
| `XPadView`（自定义 View） | 六边形绘制 + 区域高亮 + 手势检测 + 区域查找 + 状态管理 | `XPadView`（Canvas 绘制）+ `GestureInputPanel`（手势）+ `GestureFeedbackPanel`（XPadPathHighlight） |

### 耦合问题与 v4 解决方案

| Java 耦合问题 | 说明 | v4 解决方案 |
|-------------|------|------------|
| 手势检测与按键坐标绑定 | `KeyboardViewGestureListener` 通过 `findVisibleKeyViewHolderUnderLoose()` 查找触摸点下的 ViewHolder，手势检测依赖按键的实际布局位置 | `GestureInputPanel` 通过 `keyPanelLayout.findKeyAt()` 查询按键布局定位目标按键，与渲染层解耦 |
| 手势反馈与按键面板绑定 | `RecyclerViewGestureTrailer` 作为 `ItemDecoration` 绑定在 `KeyboardView` 上，轨迹绘制依赖按键面板的 Canvas | `GestureFeedbackPanel` 是独立的透明绘制层，不依赖任何面板的 Canvas |
| 按键状态反馈与按键渲染混合 | `KeyboardViewKeyAnimator` 既负责按键常规渲染又负责按下/激活等状态动画 | `KeyView` 仅渲染持续性状态，`GestureFeedbackPanel` 绘制临时手势反馈 |
| X-Pad 手势、反馈与绘制不可分 | `XPadView.onTouchEvent()` 既检测手势区域又触发绘制更新 | 手势由 `GestureInputPanel` 处理，反馈由 `GestureFeedbackPanel` 绘制，XPadView 仅渲染 |
| 无法支持分离布局 | 输入手势、按键渲染和视觉反馈在同一组件中 | 三层独立，可灵活组合叠加或分离布局 |
| 无法独立控制反馈 | 反馈的显隐、样式、位置与按键面板生命周期绑定 | `GestureFeedbackPanel` 独立控制，支持多实例 |

### 三层面板架构

| 层级 | 组件 | 职责 | 是否处理触摸 | 是否绘制反馈 |
|------|------|------|------------|------------|
| 顶层 | `GestureInputPanel` | 透明手势拦截层，识别手势并输出 `InputGesture` | ✅ 唯一触摸接收者 | ❌ 完全透明 |
| 中层 | `GestureFeedbackPanel` | 透明反馈绘制层，绘制滑行轨迹/按键高亮/X-Pad 路径/手指指示器 | ❌ | ✅ 唯一反馈绘制者 |
| 底层 | `KeyGridPanel` | 按键渲染层，根据 `ImeState` 渲染按键布局和持续性状态 | ❌ | ❌ 仅渲染常规状态 |

**历史原因**：Java 版本的 `KeyboardView` 作为 RecyclerView 内嵌了 LayoutManager、Adapter、GestureListener 和 KeyAnimator，手势检测、按键渲染和视觉反馈高度耦合。`XPadView` 同样将绘制、手势和反馈合为一体。这种耦合导致无法将输入区域、按键区域和反馈区域放置在不同位置，无法独立控制反馈的显隐和样式，也无法支持分离布局模式。v4 通过三层面板分离（输入面板/反馈面板/按键面板）彻底解耦，每一层都可以独立地改变位置、大小和组合方式。

---

## 4. 配置界面迁移

> 来源：920 §2 + §7

### Java 版本配置界面结构

| 分组 | 配置项 | 问题 |
|------|--------|------|
| 基本 | 主题设置（跳转子页面）、繁体优先 | 分组混乱：主题与繁体优先不相关 |
| 隐私 | 禁用输入数据记录、禁用按键弹出提示 | 「禁用按键弹出提示」是反馈控制而非隐私 |
| 性能（默认折叠） | 禁用按键音效、禁用按键动画、禁用翻页音效、禁用手势轨迹 | 「性能」分组名误导，关闭这些选项并非优化性能 |
| 关于 | 致谢、关于、捐赠、反馈、团队、更新日志、软件服务协议、版权、Alpha 用户协议 | 占据设置页一半空间 |

### v4 配置界面分组对照

| Java 分组 | v4 分组 | 变更原因 |
|-----------|---------|----------|
| 基本 → 主题设置 | 外观 → 主题模式 | 扁平化，不再跳转子页面，即时预览 |
| 基本 → 繁体优先 | 输入体验 → 繁体异体字优先 | 功能本质是输入行为，非基本外观 |
| 隐私 → 禁用输入数据 | 数据与隐私 → 记录输入习惯 | 语义反转，更直觉 |
| 隐私 → 禁用按键弹出提示 | 反馈控制 → 按键放大提示 | 归入反馈控制，语义反转 |
| 性能 → 禁用按键音效等 | 反馈控制 → 按键音效等 | 语义反转；「性能」分组名误导 |
| 主题子页面 → 手模式 | 外观 → 单手模式 | 扁平化，最常用配置直达 |
| 主题子页面 → X-Pad | 输入体验 → X-Pad 连续输入 | 功能本质是输入方式，非外观 |
| 关于 → Alpha 用户协议 | 移除 | v4 不再有 alpha 变体 |

### Java 功能完整对照

| Java 配置 UI | v4 对应 | 变更说明 |
|-------------|---------|----------|
| `PreferenceFragmentCompat` | Compose `SettingsScreen` | 声明式，灵活布局 |
| `PreferenceScreen` 跳转 | 扁平化 `LazyColumn` | 减少页面跳转 |
| `SwitchPreferenceCompat` | `EnhancedSwitchPreference` | 增加描述、条件显示、依赖提示 |
| `ListPreference` 主题选择 | `ThemeSelector` 卡片 + 即时预览 | 可视化选择，内嵌键盘预览 |
| `ListPreference` 手模式 | `HandModeToggle` 分段按钮 | 一键切换，无需打开选择器 |
| `PreferenceCategory` 固定分组 | `expandableSection` 可折叠分组 | 低频配置默认折叠 |
| 否定式命名（禁用 xxx） | 肯定式命名（xxx 启用） | 直觉理解，UI 开关值 = `config.ui.*Enabled` |
| 无描述 | 每项完整描述 | 说明功能和影响 |
| 无搜索 | `SearchBar` + 搜索索引 | 快速定位配置 |
| 无键盘快捷入口 | `QuickSettingsPopup` | 输入中快速切换高频配置 |
| 无键盘预览 | `KeyboardPreview` 内嵌预览 | 配置变更即时可视化 |
| `AboutDonate`（微信/支付宝二维码） | `DonateScreen`（Compose） | 保留捐赠入口，简化交互 |
| Alpha 用户协议 | 移除 | v4 无 alpha 变体 |
| 12 个 About Activity | `AboutScreen` + Navigation | 单 Activity 架构 |

### 配置字段命名对照

所有配置项从否定式（`disable*`）改为肯定式（`*Enabled`），UI 开关值直接映射：

| Java 配置键（否定式） | v4 `ImeConfig.UiConfig` 属性（肯定式） | UI 显示名称 | 映射关系 |
|---------------------|--------------------------------------|------------|----------|
| `disable_key_clicked_audio` | `audioFeedbackEnabled` | 按键音效 | 直接映射（反转语义） |
| `disable_key_animation` | `keyAnimationEnabled` | 按键动画 | 直接映射（反转语义） |
| `disable_input_candidates_paging_audio` | `candidatesPagingAudioEnabled` | 翻页提示音 | 直接映射（反转语义） |
| `disable_input_key_popup_tips` | `keyPopupTipsEnabled` | 按键放大提示 | 直接映射（反转语义） |
| `disable_gesture_slipping_trail` | `gestureSlippingTrailEnabled` | 滑行轨迹显示 | 直接映射（反转语义） |
| `disable_input_clip_popup_tips` | `clipPopupTipsEnabled` | 剪贴板粘贴提示 | 直接映射（反转语义） |
| `disable_user_input_data` | `userInputDataEnabled` | 记录输入习惯 | 直接映射（反转语义） |
| `input_clip_popup_tips_timeout` | `clipPopupTipsTimeout` | 剪贴板提示自动关闭 | 直接映射 |
| `enable_x_input_pad` | `xPadEnabled` | X-Pad 连续输入 | 直接映射 |
| `enable_latin_use_pinyin_keys_in_x_input_pad` | `latinUsePinyinKeysInXPadEnabled` | 拉丁键盘复用拼音布局 | 直接映射 |
| `adapt_desktop_swipe_up_gesture` | `adaptDesktopSwipeUpGesture` | 适配桌面滑动手势 | 直接映射 |
| `enable_candidate_variant_first` | `candidateVariantFirstEnabled` | 繁体异体字优先 | 直接映射 |

**历史原因**：Java 版本的设置界面基于 `PreferenceFragmentCompat`，存在分组逻辑混乱（「基本」仅含主题和繁体优先两个不相关配置、「隐私」混入反馈控制项）、层级过深（主题和手模式藏在子页面中）、说明缺失（大部分开关只有标题没有描述）、命名否定式（所有开关用「禁用」前缀，用户需心理反转）等问题。v4 以用户心智模型为导向重新组织信息架构，使用场景化分组、肯定式命名、即时预览和搜索功能，显著提升了配置操作效率。
