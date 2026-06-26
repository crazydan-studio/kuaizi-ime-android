# `:ui` UI 库设计文档

本目录包含 `:ui` UI 库的完整设计文档，按照业务分层和功能模块组织。UI 库提供基于 Jetpack Compose 的缺省输入法界面实现，可被第三方应用直接使用或替换。

---

## 文档列表

| 编号 | 文档 | 说明 |
|------|------|------|
| 010 | [UI 库架构总览](010-ui-library-overview.md) | 设计目标（缺省实现、可替换、可组合、可定制）、组件清单、组件层次关系、引擎依赖、KeyTableGenerator 接口 |
| 020 | [面板三层分离与屏幕布局](020-panel-separation.md) | 三层分离架构、InputGesture 坐标无关逻辑手势、InputGesture→ImeIntent 转换、GestureFeedbackState 手势反馈状态、屏幕分区模型、Zone B 三行结构、归一化坐标体系、布局状态模型 |
| 030 | [键盘视图模型](030-keyboard-view-model.md) | KeyboardViewModel 定位与完整类定义、PopupTipState（Message/Action）弹出提示状态、ToolListState 工具列表、ImeEffect 订阅与处理、GestureFeedbackState 生产者-消费者、KeyboardHost 集成组件、IMEService 装配流程 |
| 040 | [Compose 组件](040-compose-components.md) | KeyboardHost 集成组件、KeyLayoutPanel 按键布局面板、KeyView 按键视图、GestureInputPanel 手势输入面板、GestureFeedbackPanel 手势反馈面板、CandidateListPanel 候选列表面板、InputListPanel 输入列表面板、PopupTipPanel 弹出提示面板、ToolListPanel 工具列表面板、主题系统 |
| 050 | [输入动作播放](050-input-action-player.md) | UseMode 使用模式、InputActionPlayerState 播放状态、InputActionPlayer 播放器、ComposeInputActionPositionResolver 位置解析器、InputActionScriptLoader 脚本加载器、指示器内建机制、归一化坐标流 |
| 060 | [配置界面](060-config-ui.md) | KeyboardPreview 键盘预览、ThemeSelector 主题选择器、HandModeToggle 单手模式切换、QuickSettingsPopup 快捷设置弹窗 |
