# :ime-ui 模块设计文档

本目录包含 `:ime-ui` 模块的所有设计文档，涵盖 KeyboardViewModel、键盘 UI 的三层面板分离、Compose 迁移、输入动作程序化及配置界面等核心设计。

## 文档索引

| 文档 | 说明 |
|------|------|
| [010-UI 库设计总览](010-ui-library-overview.md) | UI 库设计目标（缺省实现、可替换、可组合、可定制）、组件清单（原子 / 面板 / 集成 / ViewModel / 主题）、组件层次关系、与引擎库的依赖关系 |
| [020-面板三层分离设计](020-panel-separation.md) | GestureInputPanel / GestureFeedbackPanel / KeyGridPanel 三层分离架构，InputGesture 与 GestureFeedbackState 数据模型，手势检测与反馈绘制 |
| [030-Compose UI 迁移](030-compose-migration.md) | Jetpack Compose 组件架构、KeyboardPanel/KeyboardScreen、候选栏 / 输入栏、X-Pad Compose 实现、滑行手势处理、性能验证计划 |
| [040-输入动作播放器](040-input-action-player.md) | KeyPositionResolver、InputActionPlayer、PlaybackState、FingerOverlay/SwipeTrailOverlay/KeyHighlightOverlay、ActionScriptLoader、ExerciseScreen |
| [050-配置界面](050-config-ui.md) | KeyboardPreview 组件、ThemeSelector、HandModeToggle、QuickSettingsPopup 等配置相关 UI 组件 |
| [060-KeyboardViewModel](060-keyboard-view-model.md) | UI 层协调中心，持有 ImeEngine，暴露 `StateFlow<ImeState>`，InputGesture→ImeIntent 转换，GestureFeedbackState 管理，与 :app 模块的集成方式 |
