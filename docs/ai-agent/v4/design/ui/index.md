# :ime-ui 模块设计文档

本目录包含 `:ime-ui` 模块的所有设计文档，涵盖键盘 UI 的三层面板分离、Compose 迁移、输入动作程序化及配置界面等核心设计。

---

## 文档索引

| 文档 | 说明 |
|------|------|
| [面板三层分离设计](panel-separation.md) | GestureInputPanel / GestureFeedbackPanel / KeyGridPanel 三层分离架构，InputGesture 与 GestureFeedbackState 数据模型，手势检测与反馈绘制 |
| [Compose UI 迁移](compose-migration.md) | Jetpack Compose 组件架构、KeyboardPanel/KeyboardScreen、候选栏/输入栏、X-Pad Compose 实现、滑行手势处理、性能验证计划 |
| [输入动作程序化](input-action-player.md) | KeyPositionResolver、InputActionPlayer、PlaybackState、FingerOverlay/SwipeTrailOverlay/KeyHighlightOverlay、ActionScriptLoader、ExerciseScreen |
| [配置界面](config-ui.md) | KeyboardPreview 组件、ThemeSelector、HandModeToggle、QuickSettingsPopup 等配置相关 UI 组件 |
