# 设计文档索引

本目录存放筷字输入法 v4 版本的设计文档，按模块组织在子目录中。

---

## 架构设计

跨模块的架构级设计文档。

| 文档 | 简述 |
|------|------|
| [架构总览](architecture/overview.md) | 三层库架构、MVI 数据流、键盘组合模式、数据流路径、风险与缓解 |
| [命名规范](architecture/naming-conventions.md) | 三层模块命名、KeyGridPanel 子类命名、引擎 API 命名、包命名、禁止使用的名称 |
| [三层模块划分](architecture/module-division.md) | 模块职责与依赖、设计原则、引擎库公开 API（ImeEngine/ImeConfig/ImeOutput/ImeIntent/ImeState）、UI 库设计与组件清单、输出桥接机制 |

---

## `:ime-engine` 引擎模块

纯 Kotlin 库，不依赖 Android 框架，提供核心输入引擎能力。

| 文档 | 简述 |
|------|------|
| [键盘状态机](engine/state-machine.md) | KeyboardState sealed class、状态转换规则、Keyboard 组合模式、InputKey 体系、StateHistory 有界历史栈 |
| [输入列表](engine/input-list.md) | InputListState 不可变数据模型、InputItem/InputWord/InputCompletion、线程安全、撤销机制、游标管理 |
| [字典系统](engine/dict-system.md) | DictRepository + Room 数据库、ImeDictProvider/ImeSqliteDictProvider、PinyinCharsTree、HmmModel + ViterbiDecoder |
| [X-Pad 核心](engine/xpad-core.md) | HexGrid 六边形网格计算、XPadZone/XPadLayout、X-Pad 状态集成 |
| [输入动作程序化](engine/input-action.md) | InputAction sealed class、ActionScript、ActionScriptCompiler、坐标无关设计 |
| [剪贴板与收藏](engine/clipboard-and-favorites.md) | ClipboardService、FavoriteService、InputClip/InputFavorite 数据模型 |

---

## `:ime-ui` UI 模块

基于 Compose 的缺省 UI 实现，对第三方应用开放。

| 文档 | 简述 |
|------|------|
| [三层面板分离](ui/panel-separation.md) | GestureInputPanel/KeyGridPanel/GestureFeedbackPanel 三层分离、InputGesture/GestureFeedbackState 数据模型、KeyboardPanel/KeyboardScreen 集成组件 |
| [Compose 迁移](ui/compose-migration.md) | KeyboardPanel/KeyboardScreen Compose 实现、X-Pad Compose、滑行手势处理、性能验证 |
| [输入动作播放器](ui/input-action-player.md) | InputActionPlayer、KeyPositionResolver、动画引擎（FingerOverlay/SwipeTrailOverlay/KeyHighlightOverlay）、ExerciseScreen |
| [配置 UI 组件](ui/config-ui.md) | KeyboardPreview、ThemeSelector、HandModeToggle、QuickSettingsPopup |

---

## `:app` 应用模块

系统 IME 服务壳、配置持久化、设置界面。

| 文档 | 简述 |
|------|------|
| [配置与设置](app/config.md) | ConfigRepository（DataStore）、ImeConfig 运行时/持久化配置管理、主题系统 |
| [日志系统](app/logging.md) | ImeLog/ImeLogger、LogWriter/LogcatWriter/FileLogWriter、CrashInterceptor、LogStorage、LogViewerScreen |
| [UI 测试方案](app/ui-testing.md) | UITestOverlay 工具集、Release 自动移除、截图对比测试、Compose 编译器报告 |
| [用户数据导入导出](app/user-data.md) | UserDataService、JSON 备份格式、导入策略、权限与安全 |

---

## Java 迁移对照

独立的 Java → v4 迁移对照文档，与设计内容分离。

| 文档 | 简述 |
|------|------|
| [引擎模块迁移](migration/engine-mapping.md) | 核心引擎、消息体系、状态机、输入列表、字典系统、X-Pad、剪贴板与收藏的 Java→v4 对照 |
| [UI 模块迁移](migration/ui-mapping.md) | View 系统、输入面板、配置界面的 Java→v4 对照 |
| [应用模块迁移](migration/app-mapping.md) | 应用层、配置系统、库模式、命名变更的 Java→v4 对照 |
