# 设计文档索引

本目录存放筷字输入法 v4 版本的设计文档，按模块组织在子目录中。

---

## 架构设计

跨模块的架构级设计文档。

| 文档 | 简述 |
|------|------|
| [010-架构总览](architecture/010-overview.md) | 三层库架构、MVI 数据流、键盘组合模式、数据流路径、风险与缓解 |
| [020-命名规范](architecture/020-naming-conventions.md) | 三层模块命名、KeyGridPanel 子类命名、引擎 API 命名、包命名、禁止使用的名称 |
| [030-三层模块划分](architecture/030-module-division.md) | 模块职责与依赖、设计原则、引擎库公开 API（ImeEngine/ImeConfig/ImeOutput/ImeIntent/ImeState）、UI 库设计与组件清单、输出桥接机制 |

---

## `:ime-engine` 引擎模块

纯 Kotlin 库，不依赖 Android 框架，提供核心输入引擎能力。

| 文档 | 简述 |
|------|------|
| [010-引擎库设计总览](engine/010-engine-overview.md) | 模块定位与设计目标、核心 class 关系图、公开 API 概览（ImeEngine/ImeConfig/ImeOutput/ImeIntent/ImeState） |
| [020-键盘状态机](engine/020-state-machine.md) | KeyboardState sealed class、状态转换规则、Keyboard 组合模式、InputKey 体系、StateHistory 有界历史栈 |
| [030-输入列表](engine/030-input-list.md) | InputListState 不可变数据模型、InputItem/InputWord/InputCompletion、线程安全、撤销机制、游标管理 |
| [040-字典系统](engine/040-dict-system.md) | DictRepository + Room 数据库、ImeDictProvider/ImeSqliteDictProvider、PinyinCharsTree、HmmModel + ViterbiDecoder |
| [050-X-Pad 核心](engine/050-xpad-core.md) | HexGrid 六边形网格计算、XPadZone/XPadLayout、X-Pad 状态集成 |
| [060-输入动作程序化](engine/060-input-action.md) | InputAction sealed class、ActionScript、ActionScriptCompiler、坐标无关设计 |
| [070-剪贴板与收藏](engine/070-clipboard-and-favorites.md) | ClipboardService、FavoriteService、InputClip/InputFavorite 数据模型 |
| [080-日志系统](engine/080-logging.md) | ImeLog 门面、ImeLogger、LogLevel、LogEntry、LogWriter 接口、LogStorage 文件存储、FileLogWriter 异步写入 |

---

## `:ime-ui` UI 模块

基于 Compose 的缺省 UI 实现 + KeyboardViewModel，对第三方应用开放。

| 文档 | 简述 |
|------|------|
| [010-UI 库设计总览](ui/010-ui-library-overview.md) | UI 库设计目标（缺省实现、可替换、可组合、可定制）、组件清单、组件层次关系、与引擎库的依赖关系 |
| [020-三层面板分离](ui/020-panel-separation.md) | GestureInputPanel/KeyGridPanel/GestureFeedbackPanel 三层分离、InputGesture/GestureFeedbackState 数据模型、KeyboardPanel/KeyboardScreen 集成组件 |
| [030-Compose 迁移](ui/030-compose-migration.md) | KeyboardPanel/KeyboardScreen Compose 实现、X-Pad Compose、滑行手势处理、性能验证 |
| [040-输入动作播放器](ui/040-input-action-player.md) | InputActionPlayer、KeyPositionResolver、动画引擎（FingerOverlay/SwipeTrailOverlay/KeyHighlightOverlay）、ExerciseScreen |
| [050-配置 UI 组件](ui/050-config-ui.md) | KeyboardPreview、ThemeSelector、HandModeToggle、QuickSettingsPopup |
| [060-KeyboardViewModel](ui/060-keyboard-view-model.md) | UI 层协调中心，持有 ImeEngine，InputGesture→ImeIntent 转换，GestureFeedbackState 管理，与 :app 集成方式 |

---

## `:app` 应用模块

系统 IME 服务壳（创建引擎、管理 InputConnectionBridge）、配置持久化、设置界面。

| 文档 | 简述 |
|------|------|
| [010-配置与设置](app/010-config.md) | ConfigRepository（DataStore）、ImeConfig 运行时/持久化配置管理、主题系统 |
| [020-日志系统](app/020-logging.md) | LogcatWriter、CrashInterceptor、ImeLog 初始化、LogViewerScreen/LogExportScreen、LogLevelSetting/LogStoragePathSetting |
| [030-UI 测试方案](app/030-ui-testing.md) | UITestOverlay 工具集、Release 自动移除、截图对比测试、Compose 编译器报告 |
| [040-用户数据导入导出](app/040-user-data.md) | UserDataService、JSON 备份格式、导入策略、权限与安全 |

---

## Java 迁移对照

独立的 Java → v4 迁移对照文档，与设计内容分离。

| 文档 | 简述 |
|------|------|
| [010-引擎模块迁移](migration/010-engine-mapping.md) | 核心引擎、消息体系、状态机、输入列表、字典系统、X-Pad、剪贴板与收藏的 Java→v4 对照 |
| [020-UI 模块迁移](migration/020-ui-mapping.md) | View 系统、输入面板、配置界面的 Java→v4 对照 |
| [030-应用模块迁移](migration/030-app-mapping.md) | 应用层、配置系统、库模式、命名变更的 Java→v4 对照 |
