# `:ime-engine` 引擎库设计文档

本目录包含 `:ime-engine` 引擎库的完整设计文档，按照业务分层和功能模块组织。引擎库提供核心 IME 引擎能力，独立于 UI 框架和平台。

---

## 文档列表

| 编号 | 文档 | 说明 |
|------|------|------|
| 010 | [引擎架构总览](010-engine-overview.md) | 模块定位与设计目标、核心 API 面、MVI 数据流、ImeEngine 完整类定义、ImeConfig 配置模型、Feature 门控机制、reduce 函数核心逻辑 |
| 020 | [全局状态模型](020-ime-state.md) | ImeState 完整字段定义、Keyboard/CandidateList/Clipboard/FavoriteList 子状态、ImeEffect 副作用通道（PopupTip.Message/Action、PlayAudio、PlayHaptic）、ToolListState、Feature 门控规则、状态不变式、状态频率分层 |
| 030 | [键盘状态机](030-keyboard-state-machine.md) | KeyboardState sealed class 层次结构、KeyboardStateTransition 转换体系、KeyboardStateMachine 状态机、KeyboardIntentHandler 接口与实现、Keyboard 组合模式、完整状态转换规则、有界历史栈、三层映射模型 |
| 040 | [输入列表](040-input-list.md) | InputList 不可变数据模型、InputItem 层次结构、PendingInput 待确认输入、InputCompletion 补全、PairSymbol 配对符号、InputListEditor 撤销/重做、InputListOperator 操作器、间距规则 |
| 050 | [候选与字典](050-candidate-and-dict.md) | CandidateList 候选列表模型、InputWord 层次体系、PinyinWordFilter 过滤器、ImeDictProvider 字典接口、DictRepository 字典仓库、PinyinCharsTree 前缀树、HmmModel 隐马尔可夫模型、查询流程 |
| 060 | [意图、编辑器操作与桥接](060-intent-editor-action-bridge.md) | ImeIntent 用户意图体系、EditorAction 编辑器操作体系、ImeEditorBridge 编辑器桥接接口、BaseImeEditorBridge 抽象类、InputConnectionBridge 系统输入连接、EditTextBridge 编辑框桥接、数据流转全景 |
| 065 | [音效与触觉反馈](065-audio-haptic-feedback.md) | 感官反馈设计决策、ImeEffect.PlayAudio/PlayHaptic 信号定义、AudioType/HapticType 枚举、FeedbackPlayer 泛型接口、AndroidAudioPlayer/AndroidHapticPlayer 平台实现、ViewModel 集成、配置控制、扩展模式、三方集成指南 |
| 070 | [剪贴板与收藏](070-clipboard-and-favorites.md) | InputClip 剪贴内容、InputTextType 文本类型检测、ClipboardService 剪贴板服务、InputFavorite 收藏项、FavoriteService 收藏服务、与 ImeEffect 的协作 |
| 080 | [输入动作程序化](080-input-action.md) | InputAction 动作体系、InputActionScript 动作脚本、InputActionMode 输入模式、InputActionScriptCompiler 脚本编译器、InputActionFingerIndicator 指示器、InputActionPathInterpolator 路径插值、InputActionPositionResolver 位置解析接口、归一化坐标类型 |
| 090 | [日志系统](090-logging.md) | 日志架构概述、LogLevel 日志等级、LogEntry 日志条目、LogWriter 写入接口、ImeLog 门面、ImeLogger 带标签记录器、LogStorage 文件存储、FileLogWriter 异步文件写入、LogcatWriter、CrashInterceptor 崩溃拦截 |
