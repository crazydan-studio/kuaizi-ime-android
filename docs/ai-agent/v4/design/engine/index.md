# `:ime-engine` 模块设计文档

`ime-engine` 是筷字输入法的纯 Kotlin 引擎库，提供核心 IME 引擎能力，不依赖 Android UI 框架。

## 文档索引

| 文档 | 说明 |
|------|------|
| [键盘状态机](state-machine.md) | KeyboardState sealed class 层次结构、状态转换规则、Keyboard 组合模式、InputKey 体系、StateHistory 有界历史栈 |
| [输入列表](input-list.md) | InputListState 不可变数据模型、InputItem/InputWord/InputCompletion 类型、线程安全设计、撤销机制、游标管理、InputListEditor |
| [字典系统](dict-system.md) | DictRepository + DAO 接口、Room 数据库与 Entity、ImeDictProvider/ImeSqliteDictProvider、PinyinCharsTree 前缀树、HmmModel + ViterbiDecoder |
| [X-Pad 核心](xpad-core.md) | HexGrid 六边形网格计算、XPadZone/XPadLayout 区域定义、KeyboardState.PinyinInput.XPadding 状态集成 |
| [输入动作程序化](input-action.md) | InputAction sealed class、ActionScript、InputMethod 枚举、PinyinSegment、ActionScriptCompiler 脚本编译器 |
| [剪贴板与收藏](clipboard-and-favorites.md) | ClipboardService 剪贴板监听与类型检测、FavoriteService 收藏管理、InputClip/InputFavorite 数据模型 |
