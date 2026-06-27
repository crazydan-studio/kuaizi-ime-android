# 设计文档索引

本目录存放筷字输入法的设计文档，按模块组织在子目录中。

---

## 架构设计

跨模块的架构级设计文档。

| 文档 | 简述 |
|------|------|
| [010-架构总览](architecture/010-overview.md) | 三层库架构、MVI 数据流、键盘组合模式、数据流路径、风险与缓解 |
| [020-命名规范](architecture/020-naming-conventions.md) | 三层模块命名、KeyLayoutPanel 子类命名、引擎 API 命名、包命名、禁止使用的名称 |
| [030-三层模块划分](architecture/030-module-division.md) | 模块职责与依赖、设计原则、引擎库公开 API、UI 库设计与组件清单、编辑器桥接机制 |

---

## `:engine` 引擎模块

引擎库，逻辑层与 UI / 应用分离，提供核心输入引擎能力。

| 文档 | 简述 |
|------|------|
| [010-引擎架构总览](engine/010-engine-overview.md) | 模块定位与设计目标、核心 API 面、MVI 数据流、ImeEngine 完整类定义、ImeConfig 配置模型、Feature 门控机制、reduce 函数核心逻辑 |
| [020-全局状态模型](engine/020-ime-state.md) | ImeState 完整字段定义、Keyboard/CandidateList/Clipboard/FavoriteList 子状态、ImeEffect 副作用通道（PopupTip.Message/Action）、ToolListState、Feature 门控规则、状态不变式、状态频率分层 |
| [030-键盘状态机](engine/030-keyboard-state-machine.md) | KeyboardState 层次结构、KeyboardStateTransition 转换体系、KeyboardStateMachine 状态机、KeyboardIntentHandler 接口与实现、Keyboard 组合模式、完整状态转换规则、有界历史栈、三层映射模型 |
| [040-输入列表](engine/040-input-list.md) | InputList 不可变数据模型、InputItem 层次结构、PendingInput 待确认输入、InputCompletion 补全、PairSymbol 配对符号、InputListEditor 撤销/重做、InputListOperator 操作器、间距规则 |
| [050-候选与字典](engine/050-candidate-and-dict.md) | CandidateList 候选列表模型、InputWord 层次体系、PinyinWordFilter 过滤器、ImeDictProvider 字典接口、DictRepository 字典仓库、PinyinCharsTree 前缀树、HmmModel 隐马尔可夫模型、查询流程 |
| [060-意图、编辑器操作与桥接](engine/060-intent-editor-action-bridge.md) | ImeIntent 用户意图体系、EditorAction 编辑器操作体系、ImeEditorBridge 编辑器桥接接口、BaseImeEditorBridge 抽象类、InputConnectionBridge、EditTextBridge、数据流转全景 |
| [065-音效与触觉反馈](engine/065-audio-haptic-feedback.md) | 感官反馈信号、FeedbackPlayer 泛型接口、AndroidAudioPlayer/AndroidHapticPlayer 平台实现、配置控制、扩展模式 |
| [070-剪贴板与收藏](engine/070-clipboard-and-favorites.md) | InputClip 剪贴内容、InputTextType 文本类型检测、ClipboardService 剪贴板服务、InputFavorite 收藏项、FavoriteService 收藏服务、与 ImeEffect 的协作 |
| [080-输入动作程序化](engine/080-input-action.md) | InputAction 动作体系、InputActionScript 动作脚本、InputActionScriptCompiler 脚本编译器、InputActionFingerIndicator 指示器、InputActionPathInterpolator 路径插值、InputActionPositionResolver 位置解析接口、归一化坐标类型 |
| [090-日志系统](engine/090-logging.md) | 日志架构、LogLevel 日志等级、LogEntry 日志条目、LogWriter 写入接口、ImeLog 门面、ImeLogger 带标签记录器、LogStorage 文件存储、FileLogWriter 异步写入、LogcatWriter、CrashInterceptor 崩溃拦截 |

---

## `:ui` UI 模块

基于 Compose 的缺省 UI 实现 + KeyboardViewModel，对第三方应用开放。

| 文档 | 简述 |
|------|------|
| [010-UI 库架构总览](ui/010-ui-library-overview.md) | 设计目标（缺省实现、可替换、可组合、可定制）、组件清单、组件层次关系、引擎依赖、KeyTableGenerator 接口 |
| [020-面板三层分离与屏幕布局](ui/020-panel-separation.md) | 三层分离架构、InputGesture 逻辑手势、InputGesture→ImeIntent 转换、GestureFeedbackState 手势反馈状态、屏幕分区模型、归一化坐标体系、布局状态模型 |
| [030-键盘视图模型](ui/030-keyboard-view-model.md) | KeyboardViewModel 完整类定义、PopupTipState（Message/Action）弹出提示状态、ToolListState 工具列表、ImeEffect 订阅与处理、GestureFeedbackState 生产者-消费者、KeyboardHost 集成组件、IMEService 装配流程 |
| [040-Compose 组件](ui/040-compose-components.md) | KeyboardHost 集成组件、KeyLayoutPanel 按键布局面板、KeyView 按键视图、GestureInputPanel 手势输入面板、GestureFeedbackPanel 手势反馈面板、CandidateListPanel 候选列表面板、InputListPanel 输入列表面板、PopupTipPanel 弹出提示面板、ToolListPanel 工具列表面板、主题系统 |
| [050-输入动作播放](ui/050-input-action-player.md) | UseMode 使用模式、InputActionPlayerState 播放状态、InputActionPlayer 播放器、ComposeInputActionPositionResolver 位置解析器、InputActionScriptLoader 脚本加载器、指示器内建机制、归一化坐标流 |
| [060-配置界面](ui/060-config-ui.md) | KeyboardPreview 键盘预览、ThemeSelector 主题选择器、HandModeToggle 单手模式切换、QuickSettingsPopup 快捷设置弹窗 |

---

## `:app` 应用模块

系统 IME 服务壳（创建引擎、管理 InputConnectionBridge）、配置持久化、设置界面、输入练习 UI。

| 文档 | 简述 |
|------|------|
| [010-配置管理](app/010-config.md) | ConfigDataStore（DataStore）、ImeConfig 运行时 / 持久化配置管理、主题系统与 DataStore 集成 |
| [020-日志系统](app/020-logging.md) | LogcatWriter、CrashInterceptor、ImeLog 初始化、LogViewerScreen/LogExportScreen、LogLevelSetting/LogStoragePathSetting |
| [030-UI 测试方案](app/030-ui-testing.md) | UITestOverlay 工具集、Release 自动移除、截图对比测试、Compose 编译器报告 |
| [040-用户数据导入导出](app/040-user-data.md) | UserDataService、JSON 备份格式、导入策略、权限与安全 |

---

## `:app-codegen` 代码生成模块

通过 KSP（Kotlin Symbol Processing）自动生成 `EngineConfig` 和 `UiConfig` 的 DataStore 持久化读写代码，消除手动维护 DataStore key 的样板代码。

| 文档 | 简述 |
|------|------|
| [配置管理设计](app-codegen/010-codegen.md) | KSP 代码生成引擎，通过 KSP 注解处理自动生成 EngineConfig/UiConfig 的 DataStore 读写代码 |
