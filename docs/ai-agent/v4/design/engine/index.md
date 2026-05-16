# `:ime-engine` 模块设计文档

`ime-engine` 是筷字输入法的引擎库，提供核心 IME 引擎能力。引擎库独立设计的目标是使输入法的逻辑层与 UI 和应用之间实现分离、解耦，从而方便第三方定制自己的 UI、修改交互逻辑等。

## 文档索引

| 文档 | 说明 |
|------|------|
| [010-引擎库设计总览](010-engine-overview.md) | 模块定位与设计目标、核心 class 关系图、核心模型概览（ImeEngine/ImeConfig/ImeOutput/ImeIntent/ImeState 子状态类型） |
| [020-键盘状态机](020-state-machine.md) | KeyboardState sealed class 层次结构、状态转换规则、Keyboard 组合模式、InputKey 体系、StateHistory 有界历史栈 |
| [030-输入列表](030-input-list.md) | InputList 不可变数据模型、InputItem/InputWord/InputCompletion 类型、线程安全设计、撤销机制、游标管理、InputListEditor |
| [040-字典系统](040-dict-system.md) | DictRepository + DAO 接口、Room 数据库与 Entity、ImeDictProvider/ImeSqliteDictProvider、PinyinCharsTree 前缀树、HmmModel + ViterbiDecoder |
| [050-X-Pad 核心](050-xpad-core.md) | HexGrid 六边形网格计算、XPadZone/XPadLayout 区域定义、KeyboardState.PinyinInput.XPadding 状态集成 |
| [060-输入动作程序化](060-input-action.md) | InputAction sealed class、ActionScript、InputMethod 枚举、PinyinSegment、ActionScriptCompiler 脚本编译器 |
| [070-剪贴板与收藏](070-clipboard-and-favorites.md) | ClipboardService 剪贴板监听与类型检测、FavoriteService 收藏管理、InputClip/InputFavorite 数据模型 |
| [080-日志系统](080-logging.md) | ImeLog 门面、ImeLogger 带标签记录器、LogLevel 枚举、LogEntry 不可变条目、LogWriter 接口、LogStorage 文件存储管理、FileLogWriter 异步文件写入、LogcatWriter Android Logcat 输出、CrashInterceptor 崩溃拦截 |
| [090-输出桥接机制](090-output-bridge.md) | ImeOutputBridge 桥接模式、BaseImeOutputBridge 抽象类、InputConnectionBridge 系统输入连接、EditTextBridge EditText 桥接 |
