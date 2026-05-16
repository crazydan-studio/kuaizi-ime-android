# 应用模块 `:app` 迁移对照

本文档整合所有 Java → v4 应用层（`:app`）的迁移对比内容，涵盖应用层 class 迁移、配置系统迁移、库模式迁移和命名变更对照四个维度。

> 内容来源：170 §7.4、160 §2、500 §2/§5、010 §4

---

## 1. 应用层 class 迁移

> 来源：170 §7.4

| Java Class | v4 对应 | 变更说明 |
|-----------|---------|----------|
| `ImeIntegratedActivity` | `KeyboardPanel` + `EditTextBridge` | Compose 组件 + 桥梁替代特定 Activity。应用内嵌输入法不再需要继承特定 Activity，可在 Fragment、Dialog 或自定义 View 中使用 |
| `ImeSupportEditText` | `EditTextBridge` | 桥梁模式替代 `InputMsgListener` 实现。`EditTextBridge` 继承 `BaseImeOutputBridge`，构造时接受 `supplier: () -> EditText?` |
| `Preferences` | `SettingsScreen` | Compose 设置页面替代 `PreferenceFragmentCompat` |
| `PreferencesTheme` | `SettingsScreen` 中的主题设置 | 合并到设置页面，不再独立子页面 |
| `Guide` | `GuideScreen` | Compose 引导页面 |
| `IMEService` | `IMEService`（`:app`） | 不再充当消息中介，仅管理 `InputConnection` 生命周期。创建 `ImeEngine`，接入 `InputConnectionBridge`，使用 `KeyboardPanel` 作为输入视图 |
| — | `InputConnectionBridge`（`:app`） | 新增：面向系统 `InputConnection` 的桥梁实现，继承 `BaseImeOutputBridge`。替代原 `IMEService` 中嵌入的 InputConnection 操作 |
| — | `ConfigDataStore`（`:app`） | 新增：配置持久化仓库，基于 DataStore 存取 `ImeConfig`。处理运行时覆盖与持久化同步。替代原 `Config` + `SharedPreferences` |

### IMEService 职责变化

| 维度 | Java 版本 | v4 版本 |
|------|----------|---------|
| 消息路由 | UserMsg → IMEditor, InputMsg → IMEditorView | 不再路由，引擎内部通过 `reduce()` 处理 Intent |
| InputConnection 操作 | 在 IMEService 中手动处理 | 委托 `InputConnectionBridge` |
| 输出分发 | 手动 when 分发（2 处重复） | `ImeEngine.dispatchToTarget()` 自动分发到桥梁 |
| 输入视图 | `MainboardView`（自定义 View） | `KeyboardPanel`（Compose） |
| 配置管理 | `Config` + `SharedPreferences` | `ConfigDataStore` + DataStore |

---

## 2. 配置系统迁移

> 来源：500 §2 + §5

### 架构对比

| Java 配置功能 | v4 对应 | 变更说明 |
|-------------|---------|----------|
| `Config.Immutable` | `ImeConfig` data class | 移除层叠覆盖，统一不可变配置 |
| `Config.Mutable` | `ConfigDataStore.updateConfig()` | DataStore 原子更新 |
| `ConfigChangeListener` | `Flow<ImeConfig>` | 响应式更新，自动生命周期管理 |
| `ConfigKey` 枚举 | `ImeConfig` 属性 | 类型安全，编译期检查 |
| `IMEConfig` 桥接 | `ConfigDataStore` | DataStore 直接管理，无需桥接层 |
| `SharedPreferences` | `DataStore<Preferences>` | 异步、类型安全、无 ANR |
| 主题资源（`themes.xml`, `attrs.xml`） | Compose 主题 | 声明式主题系统 |
| 60+ 主题属性 | `KeyboardColors` data class | 类型安全，IDE 自动补全 |

### 配置模型对比

| 维度 | Java 版本 | v4 版本 |
|------|----------|---------|
| 数据结构 | `Config.Mutable` 包装 `Config.Immutable`，运行时覆盖 | `ImeConfig` 不可变 data class + `runtimeOverrides` 标记 |
| 层叠覆盖 | `Config.Mutable` 动态覆盖 `Config.Immutable` 部分值 | `ImeConfig.runtimeOverrides` 记录被运行时覆盖的字段，持久化同步时跳过 |
| 持久化 | `SharedPreferences.commit()`（阻塞）/ `apply()`（可能丢失） | DataStore 协程化原子更新 |
| 变更通知 | `OnSharedPreferenceChangeListener` 手动注册/注销 | `Flow<ImeConfig>` 自动生命周期管理 |
| 类型安全 | 所有值以基本类型存储，配置键以字符串引用 | `ImeConfig` data class 属性，编译期检查 |
| 配置分离 | `IMEConfig`（引擎）+ `Config`（应用）两套配置 | `ImeConfig.EngineConfig` + `ImeConfig.UiConfig` 明确隔离在同一 data class 中 |

### 配置项完整对照

| Java 配置键 | 类型 | v4 `ImeConfig` 属性 | 变更说明 |
|------------|------|-------------------|----------|
| `theme` | String | `ImeConfig.UiConfig.themeType: ThemeType` | 字符串 → 枚举，类型安全 |
| `hand_mode` | String | `ImeConfig.EngineConfig.handMode: HandMode` | 字符串 → 枚举 |
| `enable_x_input_pad` | Boolean | `ImeConfig.UiConfig.xPadEnabled: Boolean` | 肯定式命名 |
| `enable_latin_use_pinyin_keys_in_x_input_pad` | Boolean | `ImeConfig.UiConfig.latinUsePinyinKeysInXPadEnabled: Boolean` | 肯定式命名 |
| `adapt_desktop_swipe_up_gesture` | Boolean | `ImeConfig.UiConfig.adaptDesktopSwipeUpGesture: Boolean` | 直接迁移 |
| `enable_candidate_variant_first` | Boolean | `ImeConfig.UiConfig.candidateVariantFirstEnabled: Boolean` | 肯定式命名 |
| `disable_user_input_data` | Boolean | `ImeConfig.UiConfig.userInputDataEnabled: Boolean` | 否定式 → 肯定式（语义反转） |
| `disable_key_clicked_audio` | Boolean | `ImeConfig.UiConfig.audioFeedbackEnabled: Boolean` | 否定式 → 肯定式（语义反转） |
| `disable_key_animation` | Boolean | `ImeConfig.UiConfig.keyAnimationEnabled: Boolean` | 否定式 → 肯定式（语义反转） |
| `disable_input_candidates_paging_audio` | Boolean | `ImeConfig.UiConfig.candidatesPagingAudioEnabled: Boolean` | 否定式 → 肯定式（语义反转） |
| `disable_input_key_popup_tips` | Boolean | `ImeConfig.UiConfig.keyPopupTipsEnabled: Boolean` | 否定式 → 肯定式（语义反转） |
| `disable_gesture_slipping_trail` | Boolean | `ImeConfig.UiConfig.gestureSlippingTrailEnabled: Boolean` | 否定式 → 肯定式（语义反转） |
| `disable_input_clip_popup_tips` | Boolean | `ImeConfig.UiConfig.clipPopupTipsEnabled: Boolean` | 否定式 → 肯定式（语义反转） |
| `input_clip_popup_tips_timeout` | Int | `ImeConfig.UiConfig.clipPopupTipsTimeout: Int` | 直接迁移 |
| — | — | `ImeConfig.UiConfig.hapticFeedbackEnabled: Boolean` | 新增：触觉反馈 |
| — | — | `ImeConfig.UiConfig.practicePlaybackSpeed: Float` | 新增：输入练习速度 |
| — | — | `ImeConfig.UiConfig.practiceShowFingerOverlay: Boolean` | 新增：手指指示器 |
| — | — | `ImeConfig.UiConfig.practiceShowSwipeTrail: Boolean` | 新增：滑行轨迹 |
| — | — | `ImeConfig.UiConfig.logLevel: LogLevel` | 新增：日志等级 |
| — | — | `ImeConfig.UiConfig.logStoragePath: String?` | 新增：日志路径 |
| — | — | `ImeConfig.EngineConfig.candidatePredictionEnabled: Boolean` | 新增：候选预测 |
| — | — | `ImeConfig.EngineConfig.singleLineInput: Boolean` | 新增：单行输入 |
| — | — | `ImeConfig.EngineConfig.features: Set<Feature>` | 新增：可选功能标记 |

**历史原因**：Java 版本的 `Config.Mutable` 包装 `Config.Immutable`，运行时覆盖部分值，层叠逻辑增加了理解成本。`SharedPreferences` 的 `commit()` 阻塞主线程，`apply()` 虽然异步但可能在 `onStop()` 时丢失。`IMEConfig`（引擎配置）和 `Config`（应用配置）两套配置之间存在字段重叠和同步问题。v4 将两套配置合并为统一的 `ImeConfig`，含 `EngineConfig` 和 `UiConfig` 明确隔离，使用 DataStore 实现异步、类型安全、原子更新。

---

## 3. 库模式迁移

> 来源：160 §2

Java 版本没有独立的库模块，整个 IME（引擎 + 视图 + 字典）在 `:app` 模块中。嵌入输入法有两种途径：

### Java 版本嵌入途径

| 途径 | 实现方式 | 问题 |
|------|---------|------|
| 系统 IME 模式 | `IMEService`（InputMethodService）充当消息中介 | IMEService 职责过重，既是消息路由器又负责 InputConnection 操作 |
| 应用内嵌模式 | `ImeIntegratedActivity`（Activity） | 必须继承特定 Activity，无法在 Fragment/Dialog/自定义 View 中使用 |

### ImeSupportEditText 能力迁移

Java 版本的 `ImeSupportEditText` 是"被动"接收者，实现 `InputMsgListener` 接口，支持 8 种 InputMsgType：

| ImeSupportEditText 能力 | InputMsgType | v4 对应 | 变更说明 |
|------------------------|-------------|---------|----------|
| 提交输入文本 | `InputList_Commit_Doing` | `ImeOutputBridge.commitText()` | 桥梁语义方法 |
| 撤销提交 | `InputList_Committed_Revoke_Doing` | `ImeOutputBridge.revokeCommit()` | 桥梁语义方法，快照机制内置 |
| 配对符号 | `InputList_PairSymbol_Commit_Doing` | `ImeOutputBridge.insertPairedSymbols()` | 桥梁语义方法 |
| 粘贴收藏 | `InputFavorite_Text_Commit_Doing` | `ImeOutputBridge.commitText()` | 统一到 commitText |
| 粘贴剪贴板 | `InputClip_Text_Commit_Doing` | `ImeOutputBridge.commitText()` | 统一到 commitText |
| 移动光标 | `Editor_Cursor_Move_Doing` | `ImeOutputBridge.moveCursor()` | 桥梁语义方法 |
| 选择文本 | `Editor_Range_Select_Doing` | `ImeOutputBridge.selectRange()` | 桥梁语义方法 |
| 编辑操作 | `Editor_Edit_Doing` | `ImeOutputBridge.performAction()` | 桥梁语义方法 |

### v4 三层库架构

| 维度 | Java 版本 | v4 版本 |
|------|----------|---------|
| 模块划分 | 单一 `:app` 模块 | `:ime-engine`（纯 Kotlin 引擎库）+ `:ime-ui`（Compose UI 库）+ `:app`（应用模块） |
| 库引入 | 不支持 | 第三方可引入 `:ime-engine` + `:ime-ui` 获得完整输入法能力与缺省 UI |
| 仅引擎引入 | 不支持 | 仅引入 `:ime-engine` 自行实现 UI |
| 引擎与 UI 分离 | `IMEditorView` 直接引用 `IMEditor` | `:ime-engine` 不包含任何 Compose/View 代码，UI 通过 StateFlow + Intent 与引擎交互 |
| 数据库替换 | `IMEditorDict` 单例，固定路径 | 第三方可实现 `ImeDictProvider` 替换整个字典层 |
| 功能裁剪 | 收藏和剪贴板与引擎深度绑定 | `Feature` 枚举按需启用/禁用可选功能 |
| 配置方式 | SharedPreferences 硬编码 | `ImeConfig` 代码设置，库不内置持久化 |

### 桥接机制对比

| 维度 | Java 版本 | v4 版本 |
|------|----------|---------|
| 输出消费 | `EditorField` 和 `InputConnectionBridge` 各自独立实现完整的 `when(ImeOutput)` 分发，代码完全重复 | 引擎内部统一 `dispatchToTarget()` when 分发（仅一处），桥梁实现者只需实现语义方法 |
| 第三方接入 | 必须理解 `ImeOutput` sealed class | 只需实现 `ImeOutputBridge` 接口的 6 个语义方法 |
| 撤销机制 | 各消费方自行实现 | `BaseImeOutputBridge` 抽象类内置撤销快照机制 |

**历史原因**：Java 版本没有独立的库模块，整个 IME 在 `:app` 中，无法作为依赖被其他项目引入。`ImeIntegratedActivity` 要求嵌入输入法必须继承特定 Activity，不够灵活。引擎与视图不分离（`IMEditorView` 直接引用 `IMEditor`），无法仅使用引擎而不引入视图层。配置硬编码 `SharedPreferences`，库的使用者无法通过代码设置配置。数据库不可替换，`IMEditorDict` 是单例且使用固定路径。v4 的三层库架构解决了所有这些问题：引擎与 UI 完全分离、数据库层可替换、功能可裁剪、配置通过代码设置。

---

## 4. 命名变更对照

> 来源：010 §4

以下 22 项命名变更是 v4 设计中明确规定的，所有设计文档和代码实现必须遵循。

| 旧名称 | 新名称 | 变更说明 |
|--------|--------|----------|
| `EditorActionType` | `EditorAction` | 统一为单一枚举，与 ImeIntent/ImeOutput 对称使用 |
| `StandardKeyboard` | `StandardKeyGridPanel` | 去掉 `onKeyPress`，纯渲染；强调 Grid 布局特征 |
| `KeyPanel` | `KeyGridPanel` | 强调 Grid 布局特征 |
| `StandardKeyPanel` | `StandardKeyGridPanel` | 跟随 KeyGridPanel 更名 |
| `InputPanel` | `GestureInputPanel` | 强调手势输入职能 |
| `KeyboardView` | `KeyboardPanel` | 明确容器角色；完整输入法组件（含候选栏/输入栏/工具栏 + 三层面板叠加） |
| `CandidateBar` / `InputBar` | `CandidateListPanel` / `InputListPanel` | 统一 Panel 后缀，体现列表语义 |
| `GuideScreen` | `MainScreen` | 准确反映主界面职能 |
| `InputPracticeScreen` | `ExerciseScreen` | 合并练习与演示 |
| `FingerOverlayState` | `GestureFeedbackState.fingerIndicator` | 合并到反馈状态 |
| `ImeEngineConfig` | `ImeConfig` | 合并引擎配置与应用配置 |
| `Config` | `ImeConfig.UiConfig` | 应用配置合并到 ImeConfig |
| `disable*` / `enable*` 前缀 | `*Enabled` 后缀 | 肯定式命名，如 `keyAnimationEnabled` |
| `ImeSupportEditText` / `ImeEditText` / `EditorField` | `EditTextBridge` | 桥梁模式替代独立编辑框组件 |
| `EditorHost` / `InputHostView` | （移除） | 替换为 ImeOutputBridge 接入示例 |
| `EditorState` | （移除） | 撤销状态由 `BaseImeOutputBridge` 内部管理 |
| `AppLog` / `AppLogger` | `ImeLog` / `ImeLogger` | Ime 前缀，划归 engine 模块。核心基础设施详见 [080-日志系统](../engine/080-logging.md) |
| `LogExportActivity` | `LogExportScreen` | 页面以 Screen 为后缀，划归 app 模块。Android 日志实现与 UI 详见 [020-日志系统](../app/020-logging.md) |
| `CandidateState` | `CandidateList` | 体现列表语义 |
| `FavoritesState` | `FavoriteList` | 体现列表语义；单数 Favorite + ListState |
| `CandidatePanel` | `CandidateListPanel` | 体现列表语义 |
| `FavoritesPanel` | `FavoriteListPanel` | 体现列表语义；单数 Favorite + ListPanel |
| `CandidatePager` | `CandidateListPager` | 体现列表语义 |
| `ImeOutput.EditAction` | `ImeOutput.PerformEdit` | 动作导向命名，与 `ImeIntent.PerformEdit` 对称 |
