# 引擎库 `:ime-engine` 迁移对照

本文档整合所有 Java → v4 引擎层（`:ime-engine`）的迁移对比内容，涵盖架构、核心引擎、消息体系、状态机、输入列表、字典系统、X-Pad、剪贴板与收藏八个维度。

> 内容来源：000 §5、100 §2/§7、170 §7.1/§7.2、200 §2/§7、300 §2/§7、600 §2/§4、700 §2/§5

---

## 1. 架构对比

Java 版本采用自定义消息驱动的 MVP 架构，v4 采用 MVI + StateFlow。以下为引擎相关维度的全面对比：

| 维度 | Java v3 | Kotlin v4 | 变更说明 |
|------|---------|-----------|----------|
| 架构模式 | 自定义 MVP + 手动消息路由 | MVI + StateFlow | 单向数据流更清晰，状态可追踪 |
| 消息系统 | 3 套消息（UserKeyMsg, UserInputMsg, InputMsg） | 统一 Intent + StateFlow | 类型安全、响应式、自动生命周期管理 |
| 状态管理 | 可变对象 + 手动同步 | 不可变 data class + StateFlow | 线程安全、可追踪 |
| 异步 | CompletableFuture + Handler | Coroutine + Flow | 结构化并发、简化异步代码 |
| 键盘逻辑 | 深层继承（3 层） | 组合模式 + Sealed class | 逻辑集中、类型安全 |
| 配置 | SharedPreferences | DataStore + Flow | 类型安全、异步、响应式 |
| 数据库 | 手写 SQLiteOpenHelper | Room | 类型安全、编译期检查、官方推荐 |
| 依赖管理 | 手动构造 | 手动构造注入（同 Java，但更简洁） | 项目规模适中，手动注入足够 |
| 输出桥接 | 手动 when 分发（2 处重复） | ImeOutputBridge 语义化桥接（1 处分发） | 消除重复，桥梁实现者无需理解 ImeOutput 类型体系 |
| 日志 | Logger（仅 DEBUG 生效） | ImeLog（分级 + 持久化 + 崩溃拦截 + 查看导出） | release 也可用。核心基础设施在 :ime-engine，Android 特有实现在 :app |

**历史原因**：Java 版本的 MVP + 手动消息路由虽然实现了单向数据流，但三套消息体系（共 53+ 种消息类型）导致理解成本高、分发易出错。IMEService 职责过重，既是消息路由器又负责 InputConnection 操作和生命周期管理。v4 将消息路由职责内化到引擎的 `reduce()` 函数，通过 StateFlow 自动传播状态变更，消除了手动分发。

---

## 2. 核心引擎 class 迁移

> 来源：170 §7.1

| Java Class | v4 对应 | 变更说明 |
|-----------|---------|----------|
| `IMEditor` | `ImeEngine` | 引擎核心入口点，MVI 模式替代消息中介。提供 `state`、`handleGesture()`、`handleIntent()`、`updateConfig()`、`attachOutputBridge()` |
| `IMEditorView` | （已废弃） | View 层职责由 `:ime-ui` 的 Compose 组件承担，引擎不依赖 View |
| `IMEditorDict` | `ImeDictProvider` / `ImeSqliteDictProvider` | 不再单例，由引擎使用者显式创建并注入。内置 SQLite 实现从 `assets/dict/` 加载预构建数据库 |
| `IMEConfig` | `ImeConfig` | 统一配置，合并原引擎配置（`ImeEngineConfig`）与应用配置（`Config`），含 `EngineConfig` 和 `UiConfig` 明确隔离 |
| `Key` | `InputKey` | sealed class 替代继承体系，子类：`Char`、`Ctrl`、`Candidate`、`MathOp`、`Symbol`、`XPad`、`Null` |
| `KeyFactory` | `InputKey` 的 companion factory | 按键工厂，不再独立类 |
| `Input` | `InputKey` / `InputWord` | 拆分为按键和字词两个维度，职责更清晰 |
| `InputFactory` | `InputWord` 的 companion factory | 输入工厂，不再独立类 |
| `Keyboard` | `Keyboard`（`:ime-engine` domain） | sealed class 组合模式替代继承树 |
| `BaseKeyboard` | `Keyboard` sealed class 基类 | 行为从多层继承提取为独立共享组件 |
| `PinyinKeyboard` | `PinyinKeyboard`（domain） | 组合模式：注入 `PinyinDict`、`ImeConfig`、`KeyboardStateMachine` |
| `LatinKeyboard` | `LatinKeyboard`（domain） | 复用拼音的滑行/X-Pad 模式 |
| `NumberKeyboard` | `NumberKeyboard`（domain） | 无子状态 |
| `SymbolKeyboard` | `SymbolKeyboard`（domain） | 直接迁移 |
| `EditorKeyboard` | `EditorKeyboard`（domain） | 直接迁移 |
| `EditorEditKeyboard` | `EditorEditKeyboard`（domain） | 直接迁移 |
| `MathKeyboard` | `MathKeyboard`（domain） | 嵌套 InputList |
| `EmojiKeyboard` | `EmojiKeyboard`（domain） | 直接迁移 |
| `PinyinCandidateKeyboard` | `PinyinCandidateKeyboard`（domain） | 直接迁移 |
| `InputCandidateKeyboard` | `InputCandidateKeyboard`（domain） | 直接迁移 |
| `InputListCommitOptionKeyboard` | `InputListCommitOptionKeyboard`（domain） | 直接迁移 |
| `InputList` | `InputList`（domain） | 不可变数据模型重构，1100+ 行可变类 → data class + `copy()` |
| `Inputboard` | `Inputboard`（domain） | 直接迁移 |
| `Favoriteboard` | `Favoriteboard`（domain） | 直接迁移（但功能拆分为 `ClipboardService` + `FavoriteService`） |
| `KeyboardContext` | `KeyboardContext`（domain） | 直接迁移 |
| `InputboardContext` | `InputboardContext`（domain） | 直接迁移 |
| `FavoriteboardContext` | `FavoriteboardContext`（domain） | 直接迁移 |
| `BaseInputContext` | `BaseInputContext`（domain） | 直接迁移 |
| `KeyboardStateMachine` | `KeyboardStateMachine`（domain） | 从 `BaseKeyboard` 中的状态管理逻辑提取为独立组件 |
| `InputListOperator` | `InputListOperator`（domain） | 从 `InputList` 中的操作逻辑提取为独立组件 |
| `FeatureRegistry` | `FeatureRegistry`（domain） | 新增：管理 `Feature` 的启用/禁用 |

### 共享组件提取

Java 版本中 `BaseKeyboard` 的共享行为被提取为 v4 的独立组件：

| Java BaseKeyboard 中的共享行为 | v4 独立组件 | 变更说明 |
|-------------------------------|-------------|----------|
| `playKeyAudio()` | `KeyAudioPlayer.play(keyType)` | 独立音频播放组件 |
| `updateInputList()` | `InputListOperator.apply(intent, list)` | 独立输入列表操作器 |
| `fireInputMsg()` | StateFlow 自动传播 | 不需要手动触发 |
| `evaluateInputWordKeys()` | `CandidateQuery.query(dict, spell)` | 独立候选查询 |
| `evaluatePinyinCandidates()` | `PinyinCandidateEvaluator.evaluate(dict, input)` | 独立拼音候选评估 |
| `evaluateLatinCompletion()` | `LatinCompletionEvaluator.evaluate(dict, input)` | 独立拉丁补全评估 |

---

## 3. 消息体系迁移

> 来源：170 §7.2

Java 版本有三套消息体系（共 53+ 种消息类型），v4 统一为 Intent + StateFlow 两层：

| Java Class | v4 对应 | 变更说明 |
|-----------|---------|----------|
| `UserKeyMsg` / `UserKeyMsgType` / `UserKeyMsgData`（7 种） | `InputGesture` | 统一为坐标无关的逻辑手势描述（Tap/LongPress/Swipe/Flip/XPadZonePath/CandidateTap） |
| `UserInputMsg` / `UserInputMsgType` / `UserInputMsgData`（11 种） | `ImeIntent` | 统一为用户意图（PressKey/SelectCandidate/PageCandidate/SwitchKeyboard/CommitInput/DeleteInput 等 16 种） |
| `InputMsg` / `InputMsgType` / `InputMsgData`（35+ 种） | `ImeOutput` | 统一为引擎编辑输出（CommitText/RevokeCommit/InsertPairedSymbols/MoveCursor/SelectRange/PerformEdit 6 种） |
| `InputMsgListener` | `ImeOutputBridge` | 桥接模式替代监听器。桥梁实现者只需实现语义方法，无需理解 ImeOutput 类型体系 |
| `UserMsgListener` | （已废弃） | 引擎内部统一 `dispatchToTarget()` 自动分发 |
| `BaseMsg` | （已废弃） | v4 不使用通用消息基类 |

**历史原因**：Java 版本的三套消息体系导致了 IMEService 职责过重——它既是消息路由器（UserMsg → IMEditor, InputMsg → IMEditorView），又负责 InputConnection 操作。`InputMsgType` 枚举膨胀至 35+ 值，不同关注点混在一个枚举中。v4 将上行消息（UserKeyMsg + UserInputMsg）统一为 `ImeIntent`，将下行消息（InputMsg）替换为 StateFlow 自动传播 + `ImeOutput` 语义化输出，彻底消除了手动消息分发。

### ImeOutput 与 InputMsg 的映射

| InputMsgType（Java） | ImeOutput（v4） | 说明 |
|---------------------|----------------|------|
| `InputList_Commit_Doing` | `ImeOutput.CommitText` | 提交文本，支持可替换字符列表 |
| `InputList_Committed_Revoke_Doing` | `ImeOutput.RevokeCommit` | 撤销提交，由桥梁根据选区快照恢复 |
| `InputList_PairSymbol_Commit_Doing` | `ImeOutput.InsertPairedSymbols` | 插入成对符号 |
| `Editor_Cursor_Move_Doing` | `ImeOutput.MoveCursor` | 移动光标 |
| `Editor_Range_Select_Doing` | `ImeOutput.SelectRange` | 扩展选区 |
| `Editor_Edit_Doing` | `ImeOutput.PerformEdit` | 编辑操作（退格、全选、复制、粘贴、剪切、撤销、重做） |

---

## 4. 状态机迁移

> 来源：100 §2 + §7

Java 版本通过 `State` 链表 + 10 个独立 `StateData` 类实现状态机，v4 重构为 Sealed class + 独立 `KeyboardStateMachine` 组件。

### Java 状态类型 → v4 对应

| Java 键盘类型 | Java 状态 | v4 对应 | 变更说明 |
|-------------|----------|---------|----------|
| PinyinKeyboard | `InputChars_Input_Wait` | `PinyinInput.Waiting` | 直接对应 |
| PinyinKeyboard | `InputChars_Slip_Doing` | `PinyinInput.Slipping` | 直接对应 |
| PinyinKeyboard | `InputChars_Flip_Doing` | `PinyinInput.Flipping` | 直接对应 |
| PinyinKeyboard | `InputChars_XPad_Doing` | `PinyinInput.XPadding` | 直接对应 |
| PinyinCandidateKeyboard | `Candidate_Choose` | `CandidateSelection.Choosing` | 直接对应 |
| PinyinCandidateKeyboard | `Candidate_Filter_Basic` | `CandidateSelection.Filtering` | 直接对应 |
| PinyinCandidateKeyboard | `Candidate_Filter_Advance` | `CandidateSelection.AdvanceFiltering` | 直接对应 |
| InputListCommitOptionKeyboard | `Commit_Option_Choose` | `CommitOptionChoosing` | 直接对应 |
| EditorEditKeyboard | `Editor_Edit_Cursor` | `EditorEditing.CursorMoving` | 直接对应 |
| EditorEditKeyboard | `Editor_Edit_Selection` | `EditorEditing.TextSelecting` | 直接对应 |
| SymbolKeyboard | `Symbol_Choose` | `SymbolChoosing` | 直接对应 |
| EmojiKeyboard | `Emoji_Choose` | `EmojiChoosing` | 直接对应 |
| LatinKeyboard | `InputChars_Slip/XPad` | `PinyinInput.Slipping/XPadding` | 拉丁键盘复用拼音的滑行/X-Pad 模式 |
| NumberKeyboard | 无子状态 | `Idle` | 直接对应 |
| MathKeyboard | 无子状态 | `Idle` | 直接对应 |
| EditorKeyboard | 无子状态 | `Idle` | 直接对应 |

### Java 状态数据类 → v4 对应

| Java StateData | v4 对应 | 变更说明 |
|---------------|---------|----------|
| `InputCharsSlipStateData` | `PinyinInput.Slipping` 的 data class 字段 | 合并到 sealed class 子类 |
| `InputCharsFlipStateData` | `PinyinInput.Flipping` 的 data class 字段 | 合并到 sealed class 子类 |
| `PagingStateData` | `CandidateSelection.Choosing.pageIndex/pageSize` | 合并到候选选择状态 |
| `PinyinCandidateChooseStateData` | `CandidateSelection.Choosing` | 合并到 sealed class 子类 |
| `PinyinCandidateFilterStateData` | `CandidateSelection.Filtering` | 合并到 sealed class 子类 |
| `PinyinCandidateAdvanceFilterStateData` | `CandidateSelection.AdvanceFiltering` | 合并到 sealed class 子类 |
| `EmojiChooseStateData` | `EmojiChoosing` | 合并到 sealed class 子类 |
| `SymbolChooseStateData` | `SymbolChoosing` | 合并到 sealed class 子类 |
| `EditorEditStateData` | `EditorEditing` 子类 | 合并到 sealed class 子类 |
| `InputListCommitOptionChooseStateData` | `CommitOptionChoosing` | 合并到 sealed class 子类 |

### 状态管理机制对比

| 维度 | Java 版本 | v4 版本 | 变更说明 |
|------|----------|---------|----------|
| 状态定义 | 10 个独立 `StateData` 类 + `State` 链表 | `KeyboardState` sealed class 层级 | 编译期类型约束，非法状态转换不可构造 |
| 状态转换 | 分散在 `BaseKeyboard` 各方法中 | `KeyboardStateMachine.transition()` 集中处理 | 转换规则明确，易追踪 |
| 状态历史 | `State.previous` 链表（可能无限增长） | `ArrayDeque<KeyboardState>(maxSize = 10)` | 有界历史栈，防止内存泄漏 |
| 状态回退 | 通过 `previous` 链回退 | `stateHistory.pop()` | 简洁明确 |
| 键盘切换 | 无清空逻辑 | 切换时清空历史栈 | 不同键盘类型之间无回退关系 |

**历史原因**：Java 版本的状态类型分散在 10 个独立的 StateData 类中，缺乏统一的类型约束，任何状态都可以转换到任何其他状态，没有编译期检查。`State.previous` 链表可能无限增长导致内存泄漏。状态转换逻辑分散在 `BaseKeyboard` 的各个方法中，没有集中定义转换规则。v4 通过 Sealed class 提供编译期类型安全，通过 `KeyboardStateMachine` 集中管理转换规则，通过有界栈替代无限链表。

---

## 5. 输入列表迁移

> 来源：200 §2 + §7

### 数据结构对比

| Java InputList 功能 | v4 对应 | 变更说明 |
|--------------------|---------|----------|
| `inputs`（ArrayList，可变） | `inputs: List<InputItem>`（不可变 List） | 线程安全，无需同步 |
| `cursorIndex` | `gapIndex: Int` + `init` 断言 | 显式边界检查，编译期断言 |
| `pending`（CharInput，与 InputList 混合） | `PendingInput` 独立数据类 | 关注点分离：临时状态与持久化输入列表分开 |
| `completions`（与 InputList 耦合） | `PendingInput.completions` | 与 pending 绑定，补全由查询逻辑管理 |
| `gapSpacing`（是否显示间隔开关） | 始终显示 GapInput | 简化：移除 gapSpacing 开关 |
| `mathExprNested`（递归 InputList） | `InputList.mathExprNested` | 递归结构保留 |
| `Revertion`（撤销快照） | `InputListEditor`（undo/redo 栈） | 标准化的撤销/重做，支持多步 |
| `append()` | `appendChar()` | 返回新实例（不可变） |
| `delete()` | `deleteCharBeforeCursor()` | 返回新实例（不可变） |
| `clean()` | `clean()` | 返回新实例（不可变） |
| `commit()` | ViewModel 中处理 | 提交逻辑不属于 InputList |
| `evaluateInputViewData()` | `InputViewData` 计算 | 分离视图数据计算 |
| `evaluateGapIndex()` | `moveCursorTo()` | 显式游标操作 |
| 对偶符号处理 | `PairSymbol` data class | 类型安全 |
| 剪贴板输入 | `InputClip` + `InputTextType` | 类型安全的剪贴板检测 |
| 收藏输入 | `InputFavorite` | 独立数据类 |

### 输入项类型对比

| Java 输入项 | v4 对应 | 变更说明 |
|------------|---------|----------|
| `CharInput` | `InputItem.Char` | data class，不可变，支持 `replacements` 和 `nextReplacement()` |
| `GapInput` | `InputItem.Gap` | `data object`，单例 |
| `MathExprInput` | `InputItem.MathExpr` | 嵌套 `InputList` |

### 线程安全对比

| 问题 | Java 版本 | v4 版本 |
|------|----------|---------|
| 并发访问 | `ArrayList` 从主线程和异步字典回调线程同时访问，可能 `ConcurrentModificationException` | 不可变 `List<InputItem>` + `StateFlow` 原子性保证 |
| 状态变更 | 可变对象直接修改 | `copy()` 创建新实例，原始实例不受影响 |
| 异步操作 | `CompletableFuture` + 手动同步 | 协程 + `reduce` 串行执行 |

**历史原因**：Java 版本的 `InputList` 是一个 1100+ 行的可变类，存在线程安全风险（主线程 UI 更新 + 异步字典查询回调同时访问 `inputs`）。`pending` 临时状态与持久化的输入列表属于不同关注点却混在一起。`cursorIndex` 直接操作列表索引，插入/删除时需要手动维护，容易出错。v4 通过不可变 data class + `copy()` 模式彻底解决线程安全问题，通过 `PendingInput` 分离临时状态，通过 `gapIndex` 断言确保游标位置合法性。

---

## 6. 字典系统迁移

> 来源：300 §2 + §7

### 架构对比

| Java 字典功能 | v4 对应 | 变更说明 |
|-------------|---------|----------|
| `IMEditorDict` 单例 | `DictRepository` + Room | 移除单例，依赖注入；`ImeDictProvider` 为对外接口 |
| `IMEditorDict.openAsync()` | Room 自动管理 | Room 处理数据库连接生命周期 |
| `IMEditorDict.close()` | Room 自动管理 | 生命周期绑定 |
| `PinyinDict.findWords()` | `PinyinWordDao.lookupBySpell()` | 类型安全，编译期 SQL 检查 |
| `PinyinDict.findPhrases()` | `PinyinPhraseDao.lookupBySpells()` | 类型安全，编译期 SQL 检查 |
| `PinyinCharsTree`（可变 Trie） | 不可变 `PinyinCharsTree` data class | 线程安全，Builder 模式构建 |
| `Hmm` + `Viterbi`（纯 Java 实现） | `HmmModel` + `ViterbiDecoder` | value class 封装状态/观察值，协程化大规模计算，不可变 Map |
| `UserInputDataDict.record()` | `UserInputDao.incrementFrequency()` | Room DAO，类型安全 |
| `UserInputDataDict.findCompletions()` | `UserInputDao.getCompletions()` | Room DAO，类型安全 |
| `UserInputFavoriteDict` | `FavoriteDao` + Flow | 响应式查询，`observeFavorites()` 返回 `Flow<List<InputFavorite>>` |
| `From_v0/v2_v3/v3_v4` 升级类 | Room Migration | 标准化迁移框架 |
| `Emojis` 枚举 | `EmojiGroups` data class | 不依赖数据库 |
| `SymbolGroup` + `Symbol` | `SymbolGroup` data class | 不依赖数据库 |
| `DictDBType` 枚举 | Room Database 类 | 移除，Room 管理 DB 类型 |
| raw 字典资源复制 | `createFromAsset("dict/...")` | assets/dict 组织，Room 自动复制 |

### 数据库方案对比

| 维度 | Java 版本 | v4 版本 |
|------|----------|---------|
| ORM | 手写 SQLiteOpenHelper | Room |
| SQL 安全 | 字符串拼接，运行时错误 | 编译期 SQL 检查 |
| 查询结果 | `Cursor` 手动读取列值 | Entity data class 自动映射 |
| 异步 | `CompletableFuture` | 协程 + Room 内建 `suspend` 函数 |
| 响应式 | 无 | `Flow<List<Entity>>` 响应式查询 |
| 数据库升级 | 分散的 `From_vX` 类 | Room Migration 标准框架 |

### 对外接口对比

| 维度 | Java 版本 | v4 版本 |
|------|----------|---------|
| 字典创建 | `IMEditorDict` 单例，固定路径 | `ImeDictProvider` 接口 + `ImeSqliteDictProvider` 内置实现 + `ImeInMemoryDictProvider` 测试实现 |
| 字典替换 | 不可替换 | 第三方可实现 `ImeDictProvider` 替换整个字典层 |

**历史原因**：Java 版本的手写 SQL 无编译期检查，拼写错误只能在运行时发现。`IMEditorDict` 的单例模式使用 `CompletableFuture` 管理异步开关，逻辑复杂。数据库升级分散在不同类中，难以维护。查询结果以 `Cursor` 形式返回，需要手动读取列值，无类型安全。v4 通过 Room 提供编译期 SQL 检查、类型安全的 Entity/DAO、协程化的 `suspend` 函数和响应式 `Flow` 查询，彻底解决了这些问题。

---

## 7. X-Pad 迁移

> 来源：700 §2 + §5

### 核心组件对比

| Java X-Pad 功能 | v4 对应 | 变更说明 |
|----------------|---------|----------|
| `XPadView`（自定义 View） | `XPadView`（Compose Composable） | 声明式 UI，Canvas 绘制 |
| `XPainter` 系列（5 个类：XTextPainter、XDrawablePainter、XPathPainter、XAlignPainter） | Compose `Canvas` 直接绘制 | 移除 Painter 体系，简化为直接绘制 |
| `XZone` | `XPadZone` data class | 不可变，含 `axial`、`label`、`chars` |
| `XPadState` | `KeyboardState.PinyinInput.XPadding` | 集成到键盘状态机 |
| `XPadKey` | `InputKey.XPad` sealed class 子类 | 类型安全 |
| Mixite 六边形网格 | 自实现 `HexGrid` | 移除 Mixite 依赖，使用轴向坐标（axial coordinates） |
| `HexagonDrawable` | Compose `drawPath` | Compose 原生绘制 |
| 触摸事件处理 | `Modifier.pointerInput` | Compose 手势 API |
| 手势轨迹绘制 | Compose `Canvas` | 简化 |
| `PinyinKeyTable`（使用 Mixite 生成） | `XPadLayout.fromPinyinKeys()` | 保留键表生成逻辑，移除 Mixite |
| 拉丁 X-Pad 模式 | `XPadLayout.fromLatinKeys()` | 保留复用逻辑 |

### 手势处理对比

| 维度 | Java 版本 | v4 版本 |
|------|----------|---------|
| 手势检测 | `XPadView.onTouchEvent()` + 区域查找 | `Modifier.pointerInput` + `HexGrid.pixelToAxial()` |
| 区域高亮 | `XPadView.onDraw()` 中重绘 | `GestureFeedbackState.xPadPath` → `GestureFeedbackPanel` |
| 状态更新 | `XPadState` → 触发重绘 | `KeyboardState.XPadding` → StateFlow → UI 自动更新 |

**历史原因**：Java 版本依赖 Mixite 库处理六边形网格计算，但仅使用了其极小部分功能，引入了过重的依赖。XPainter 体系有 5 个接口/类，职责分散且代码量大。`XPadView` 同时处理触摸事件和绘制逻辑，耦合严重。`XPadState` 与 `KeyboardState` 的交互不清晰。v4 自实现轻量的 `HexGrid` 移除 Mixite 依赖，将 X-Pad 状态集成到键盘状态机的 `XPadding` 子状态，手势检测和视觉反馈分离到三层面板架构中。

---

## 8. 剪贴板与收藏迁移

> 来源：600 §2 + §4

### 职责分离

Java 版本将剪贴板和收藏混合在 `Favoriteboard` 中，v4 拆分为两个独立服务：

| Java 功能 | v4 对应 | 变更说明 |
|----------|---------|----------|
| `Favoriteboard`（混合职责） | `ClipboardService` + `FavoriteService` | 职责分离：剪贴板检测和收藏管理各自独立 |

### 剪贴板功能对比

| Java 剪贴板功能 | v4 对应 | 变更说明 |
|----------------|---------|----------|
| `ClipboardManager.OnPrimaryClipChangedListener` | `callbackFlow` 封装 | 协程化，`awaitClose` 自动注销监听器 |
| `InputClip` + `InputTextType` 枚举 | `InputClip.from()` + `InputTextType.detect()` | 类型安全的自动检测，集中管理正则 |
| 正则提取（分散在多处） | `ClipboardService.extractType()` | 集中管理，按类型提取 |
| 剪贴板浮动提示（自定义 View） | `ClipTipPopup`（Compose） | 声明式 UI |
| 手动注册/注销监听器 | `callbackFlow` + `awaitClose` | 自动生命周期管理 |

### 收藏功能对比

| Java 收藏功能 | v4 对应 | 变更说明 |
|-------------|---------|----------|
| `UserInputFavoriteDict` | `FavoriteService` + `FavoriteDao` | Room DAO + Flow，响应式收藏列表 |
| 收藏面板（自定义 RecyclerView） | `FavoriteListPanel` + `LazyColumn` | Compose 简化 |
| 收藏删除（手动按钮） | `SwipeToDismiss` | 滑动删除，交互更自然 |
| 收藏使用计数 | `FavoriteEntity.usageCount` | 保留 |
| 收藏保存（从输入/剪贴板） | `FavoriteService.save()` | 统一入口 |
| 用户数据备份 | 不支持 | `UserDataService`（详见 [040-用户数据导入导出](../app/040-user-data.md)） |

### 剪贴板数据类型对比

| Java 类型 | v4 对应 | 说明 |
|----------|---------|------|
| URL | `InputTextType.Url` | 保留 |
| Email | `InputTextType.Email` | 保留 |
| Phone | `InputTextType.Phone` | 保留 |
| Captcha | `InputTextType.Captcha` | 保留 |
| IdCard | `InputTextType.IdCard` | 保留 |
| CreditCard | `InputTextType.CreditCard` | 保留 |
| Address | `InputTextType.Address` | 保留 |
| Html | `InputTextType.Html` | 保留 |
| — | `InputTextType.Text` | 新增：纯文本类型 |

**历史原因**：Java 版本的 `Favoriteboard` 同时处理剪贴板检测、剪贴板提示、收藏管理和智能提取四个职责，违反单一职责原则。剪贴板监听使用 `OnPrimaryClipChangedListener` 需要手动注册/注销，容易遗漏导致内存泄漏。收藏数据与字典耦合，`UserInputFavoriteDict` 直接操作数据库。类型检测正则分散在 `InputClip` 类和工具方法中。v4 将 `Favoriteboard` 拆分为 `ClipboardService`（协程化剪贴板监听）和 `FavoriteService`（Room DAO + Flow 收藏管理），各自拥有清晰的数据模型和业务逻辑。
