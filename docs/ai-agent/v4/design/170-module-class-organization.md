# 170 — 模块 Class 组织结构

本文档统一设计 v4 三层库架构中各模块的包结构与 class 组织，明确每个包的职责边界和 class 归属。模块划分与职责定义详见文档 160。

---

## 1. 设计原则

### 1.1 包结构设计原则

1. **按职责分包**：每个包围绕一个明确的职责领域组织，包内的 class 共同完成该领域的功能。不同包之间通过公开 API 交互，内部实现互不依赖
2. **公开与内部隔离**：每个包通过 `public` / `internal` 可见性控制暴露范围。`api` 包作为引擎库的唯一公开入口，其他包的对外 class 通过 `api` 包重导出（re-export）或直接声明为 `public`
3. **依赖方向单一**：包之间的依赖方向与模块依赖方向一致，禁止循环依赖。引擎库内部包只向下依赖，UI 库的包只依赖引擎公开 API
4. **包命名体现领域**：包名使用领域术语而非技术术语（如 `dict` 而非 `database`，`input` 而非 `model`），确保通过包名即可理解其业务含义

### 1.2 Class 归属原则

1. **引擎库（`:ime-engine`）**：所有对外 class 以 `Ime` 为前缀（命名规范详见文档 010、160 §3.4）。内部实现 class 不强制 `Ime` 前缀，因其不对外暴露
2. **UI 库（`:ime-ui`）**：class 命名贴近 UI 业务语义，不使用 `Ime` 前缀。面板类以 `Panel` 为后缀，视图类以 `View` 为后缀，桥梁类以 `Bridge` 为后缀
3. **应用模块（`:app`）**：class 命名贴近应用业务语义，不使用 `Ime` 前缀。页面以 `Screen` 为后缀，Android 系统服务类沿用平台命名惯例

---

## 2. 目录总览

```
kuaizi-ime-android/
├── code/
│   ├── app/                     ← 应用模块（系统 IME 服务 + 设置 + 日志诊断界面）
│   │   ├── build.gradle.kts     ← implementation(":ime-engine"), implementation(":ime-ui")
│   │   └── src/main/
│   │       └── org/crazydan/studio/app/ime/kuaizi/   ← 直接使用顶级包名，不加子模块名
│   │
│   ├── ime-ui/                  ← UI 库模块（Compose 缺省 UI）
│   │   ├── build.gradle.kts     ← android.library
│   │   └── src/main/
│   │       └── org/crazydan/studio/app/ime/kuaizi/ui/
│   │
│   └── ime-engine/              ← 引擎库模块（纯 Kotlin）
│       ├── build.gradle.kts     ← android.library
│       └── src/main/
│           └── org/crazydan/studio/app/ime/kuaizi/engine/
│
├── docs/
└── ...
```

---

## 3. 引擎库 `:ime-engine` 的 Class 组织

引擎库是纯 Kotlin 库，不依赖 Android 框架（字典 I/O 除外）。所有对外 class 以 `Ime` 为前缀，作为引擎库的命名空间标识。

### 3.1 api — 公开 API 入口

引擎库的唯一公开入口包。外部模块（`:ime-ui`、`:app`、第三方）只通过此包的 class 与引擎交互。其他包的公开 class 若需对外暴露，在此包中通过 `typealias` 或直接声明为 `public` 重导出。

| Class | 类型 | 说明 |
|-------|------|------|
| `ImeEngine` | `class` | 引擎核心入口点，提供 `state`、`handleGesture()`、`handleIntent()`、`updateConfig()`、`attachOutputBridge()`、`detachOutputBridge()`。对应 Java `IMEditor` |
| `ImeConfig` | `data class` | 统一运行时配置，含 `EngineConfig`（引擎配置）和 `UiConfig`（UI 配置），二者明确隔离 |
| `ImeConfig.EngineConfig` | `data class` | 引擎配置：`keyboardType`、`handMode`、`features`、`candidatePredictionEnabled`、`singleLineInput` |
| `ImeConfig.UiConfig` | `data class` | UI 配置：`themeType`、`xPadEnabled`、`audioFeedbackEnabled`、`hapticFeedbackEnabled` 等界面和交互相关配置项 |
| `ImeIntent` | `sealed class` | 用户意图：`PressKey`、`LongPressKey`、`SelectCandidate`、`PageCandidate`、`SwitchKeyboard`、`CommitInput`、`DeleteInput`、`CleanInput`、`MoveCursorTo`、`PerformEdit`、`SelectXPadPath`、`PasteClip`、`SaveFavorite`、`UpdateConfig`、`ExportUserData`、`ImportUserData` |
| `ImeOutput` | `sealed class` | 引擎编辑输出：`CommitText`、`RevokeCommit`、`InsertPairedSymbols`、`MoveCursor`、`SelectRange`、`PerformEdit` |
| `ImeState` | `data class` | 引擎暴露给 UI 的完整状态：`keyboardType`、`keyboardState`、`keyGrid`、`inputList`、`candidates`、`clipboard`、`favorites`、`config` |
| `ImeOutputBridge` | `interface` | 输出桥梁接口：`commitText()`、`revokeCommit()`、`insertPairedSymbols()`、`moveCursor()`、`selectRange()`、`performAction()`、`getText()`、`getSelection()` |
| `BaseImeOutputBridge` | `abstract class` | 输出桥梁抽象基类，实现撤销快照机制（`resetRevertion()`、`recordRevertion()`、`onRevokeCommit()`） |
| `ImeDictProvider` | `interface` | 字典提供者接口：`pinyin`、`userInputData`、`userInputFavorite`、`initialize()`、`close()` |
| `ImeSqliteDictProvider` | `class` | 内置 SQLite 字典实现，对应 Java `IMEditorDict`，从 `assets/dict/` 加载预构建数据库 |
| `ImeInMemoryDictProvider` | `class` | 基于内存的字典实现，适用于测试环境或无需持久化场景 |
| `ConfigField` | `enum` | 运行时可覆盖的配置字段标识 |
| `ThemeType` | `enum` | 主题模式：`Light`、`Night`、`FollowSystem` |
| `HandMode` | `enum` | 手模式：`Left`、`Right` |
| `Feature` | `enum` | 可选功能标记：`Clipboard`、`Favorites`、`InputPractice`、`CandidatePrediction` |
| `KeyboardType` | `enum` | 键盘类型：`Pinyin`、`Latin`、`Number`、`Symbol`、`Editor`、`Math`、`Emoji`、`Candidate`、`CommitOption` |
| `CursorDirection` | `enum` | 光标方向：`Left`、`Right`、`Up`、`Down` |
| `EditorAction` | `enum` | 编辑动作：`SELECT_ALL`、`FAVORITE`、`COPY`、`BACKSPACE`、`PASTE`、`CUT`、`UNDO`、`REDO` |
| `KeyGesture` | `sealed class` | 按键手势类型：`Tap`、`LongPress`、`Swipe`、`Flip` |
| `PageDirection` | `enum` | 翻页方向：`Next`、`Previous` |
| `FlipDirection` | `enum` | 翻转方向：`Left`、`Right`、`Up`、`Down` |

### 3.2 bridge — 输出桥接

输出桥接机制的内部实现细节。`ImeOutputBridge` 接口和 `BaseImeOutputBridge` 抽象类虽然在 `api` 包中声明，但其辅助类型（如 `SelectionSnapshot`）在 `bridge` 包中管理。此包的 class 均为 `internal`，不对外暴露。

| Class | 类型 | 可见性 | 说明 |
|-------|------|--------|------|
| `SelectionSnapshot` | `data class` | internal | 撤销选区快照：`beforeStart`、`beforeEnd`、`beforeContent`、`afterStart`、`afterEnd` |
| `TextRange` | `data class` | public | 文本选区范围：`start`、`end`。供 `ImeOutputBridge.getSelection()` 返回 |

### 3.3 domain — 领域层

引擎的核心领域模型，包含键盘组合模式、输入列表和输入板等领域对象。这些 class 是引擎内部实现，不对外暴露，但承担了引擎最核心的业务逻辑。

| Class | 类型 | 可见性 | 说明 | 对应 Java |
|-------|------|--------|------|-----------|
| `Keyboard` | `sealed class` | internal | 键盘组合模式基类。子类对应各键盘类型的具体行为 | `BaseKeyboard` / `Keyboard` |
| `PinyinKeyboard` | `class` | internal | 拼音键盘 | `PinyinKeyboard` |
| `LatinKeyboard` | `class` | internal | 拉丁键盘 | `LatinKeyboard` |
| `NumberKeyboard` | `class` | internal | 数字键盘 | `NumberKeyboard` |
| `SymbolKeyboard` | `class` | internal | 符号键盘 | `SymbolKeyboard` |
| `EditorKeyboard` | `class` | internal | 编辑键盘 | `EditorKeyboard` |
| `EditorEditKeyboard` | `class` | internal | 编辑模式键盘 | `EditorEditKeyboard` |
| `MathKeyboard` | `class` | internal | 数学键盘 | `MathKeyboard` |
| `EmojiKeyboard` | `class` | internal | Emoji 键盘 | `EmojiKeyboard` |
| `PinyinCandidateKeyboard` | `class` | internal | 拼音候选键盘 | `PinyinCandidateKeyboard` |
| `InputCandidateKeyboard` | `class` | internal | 输入候选键盘 | `InputCandidateKeyboard` |
| `InputListCommitOptionKeyboard` | `class` | internal | 输入列表提交选项键盘 | `InputListCommitOptionKeyboard` |
| `InputList` | `class` | internal | 输入列表管理 | `InputList` |
| `Inputboard` | `class` | internal | 输入板 | `Inputboard` |
| `Favoriteboard` | `class` | internal | 收藏板 | `Favoriteboard` |
| `KeyboardContext` | `class` | internal | 键盘上下文 | `KeyboardContext` |
| `InputboardContext` | `class` | internal | 输入板上下文 | `InputboardContext` |
| `FavoriteboardContext` | `class` | internal | 收藏板上下文 | `FavoriteboardContext` |
| `BaseInputContext` | `class` | internal | 输入上下文基类 | `BaseInputContext` |
| `KeyboardStateMachine` | `class` | internal | 键盘状态机（详见文档 100） | 嵌入在 `BaseKeyboard` 中的状态管理逻辑 |
| `InputListOperator` | `class` | internal | 输入列表操作器 | 嵌入在 `InputList` 中的操作逻辑 |
| `FeatureRegistry` | `class` | internal | 功能注册表，管理 `Feature` 的启用/禁用 | 无直接对应 |

### 3.4 dict — 字典系统

字典接口定义与内置实现。接口在 `api` 包中声明（`ImeDictProvider`、`PinyinDict`、`UserInputDataDict`、`UserInputFavoriteDict`），具体实现在此包中。完整的字典系统重构设计详见文档 300。

| Class | 类型 | 可见性 | 说明 | 对应 Java |
|-------|------|--------|------|-----------|
| `PinyinDict` | `interface` | public | 拼音字典接口：`queryCandidates()`、`querySpellPath()` | `PinyinDict` |
| `UserInputDataDict` | `interface` | public | 用户输入数据字典接口：`recordSelection()`、`queryUserCandidates()` | `UserInputDataDict` |
| `UserInputFavoriteDict` | `interface` | public | 用户收藏字典接口：`queryFavorites()`、`addFavorite()`、`removeFavorite()`、`clearFavorites()` | `UserInputFavoriteDict` |
| `SqlitePinyinDict` | `class` | internal | SQLite 拼音字典实现 | `PinyinDict`（SQLite 实现） |
| `SqliteUserInputDataDict` | `class` | internal | SQLite 用户输入数据实现 | `UserInputDataDict`（SQLite 实现） |
| `SqliteUserInputFavoriteDict` | `class` | internal | SQLite 用户收藏实现 | `UserInputFavoriteDict`（SQLite 实现） |
| `InMemoryPinyinDict` | `class` | internal | 内存拼音字典实现 | 无 |
| `InMemoryUserInputDataDict` | `class` | internal | 内存用户输入数据实现 | 无 |
| `InMemoryUserInputFavoriteDict` | `class` | internal | 内存用户收藏实现 | 无 |
| `PinyinCharsTree` | `class` | internal | 拼音字符树 | `PinyinCharsTree` |
| `Hmm` | `class` | internal | HMM 模型 | `Hmm` |
| `Viterbi` | `class` | internal | Viterbi 算法 | `Viterbi` |
| `SymbolGroup` | `class` | internal | 符号分组 | `SymbolGroup` |
| `Symbol` | `class` | internal | 符号 | `Symbol` |
| `Emojis` | `class` | internal | Emoji 数据 | `Emojis` |
| `DictDBType` | `enum` | internal | 字典数据库类型 | `DictDBType` |
| `Upgrader` | `interface` | internal | 数据库升级器接口 | `Upgrader` |
| `From_v0` | `class` | internal | v0→最新 版本升级 | `From_v0` |
| `From_v2_to_v3` | `class` | internal | v2→v3 版本升级 | `From_v2_to_v3` |
| `From_v3_to_v4` | `class` | internal | v3→v4 版本升级 | `From_v3_to_v4` |

### 3.5 dict/db — 字典数据库辅助

SQLite 数据库操作辅助类，封装数据库的创建、升级和访问逻辑。

| Class | 类型 | 可见性 | 说明 | 对应 Java |
|-------|------|--------|------|-----------|
| `PinyinDBHelper` | `class` | internal | 拼音数据库辅助 | `PinyinDBHelper` |
| `UserInputDataDBHelper` | `class` | internal | 用户输入数据数据库辅助 | `UserInputDataDBHelper` |
| `UserInputFavoriteDBHelper` | `class` | internal | 用户收藏数据库辅助 | `UserInputFavoriteDBHelper` |
| `HmmDBHelper` | `class` | internal | HMM 数据库辅助 | `HmmDBHelper` |

### 3.6 input — 输入类型

引擎中的输入数据模型，定义按键、输入字词、输入手势等核心类型。这些类型是引擎与 UI 层之间的共享数据契约。

| Class | 类型 | 可见性 | 说明 | 对应 Java |
|-------|------|--------|------|-----------|
| `InputKey` | `sealed class` | public | 输入按键：`CharKey`、`CtrlKey`、`XPadKey`、`SymbolKey`、`MathOpKey`、`InputWordKey`、`TypedKey` | `Key` 及其子类 |
| `InputWord` | `sealed class` | public | 输入字词：`PinyinWord`、`EmojiWord` | `InputWord` 及其子类 |
| `InputGesture` | `sealed class` | public | 输入手势（坐标无关）：`Tap`、`LongPress`、`Swipe`、`Flip`、`XPadZonePath`、`CandidateTap` | 嵌入在 `UserKeyMsg` 中的手势信息 |
| `InputFavorite` | `data class` | public | 收藏项 | `InputFavorite` |
| `InputClip` | `data class` | public | 剪贴板项 | `InputClip` |
| `InputTextType` | `enum` | public | 输入文本类型 | `InputTextType` |
| `XPadZone` | `data class` | public | X-Pad 区域标识 | `XZone` |
| `PinyinSegment` | `data class` | public | 拼音片段 | 无直接对应，从 `PinyinWord` 字段提取 |
| `KeyFactory` | `class` | internal | 按键工厂 | `KeyFactory` |
| `InputFactory` | `class` | internal | 输入工厂 | `InputFactory` |

### 3.7 input/key — 按键子类型

`InputKey` 的各子类型定义，按键盘类型细分。

| Class | 类型 | 可见性 | 说明 | 对应 Java |
|-------|------|--------|------|-----------|
| `CharKey` | `data class` | public | 字符按键 | `CharKey` |
| `CtrlKey` | `sealed class` | public | 控制按键（含多种子类型，详见文档 020） | `CtrlKey` |
| `XPadKey` | `data class` | public | X-Pad 按键 | `XPadKey` |
| `SymbolKey` | `data class` | public | 符号按键 | `SymbolKey` |
| `MathOpKey` | `data class` | public | 数学运算按键 | `MathOpKey` |
| `InputWordKey` | `data class` | public | 输入字词按键 | `InputWordKey` |
| `TypedKey` | `data class` | public | 类型化按键 | `TypedKey` |

### 3.8 input/word — 输入字词子类型

`InputWord` 的各子类型定义。

| Class | 类型 | 可见性 | 说明 | 对应 Java |
|-------|------|--------|------|-----------|
| `PinyinWord` | `data class` | public | 拼音字词 | `PinyinWord` |
| `EmojiWord` | `data class` | public | Emoji 字词 | `EmojiWord` |

### 3.9 input/completion — 输入补全

输入补全相关类型。

| Class | 类型 | 可见性 | 说明 | 对应 Java |
|-------|------|--------|------|-----------|
| `InputCompletion` | `data class` | public | 输入补全项 | `InputCompletion` |
| `InputCompletions` | `class` | public | 输入补全集 | `InputCompletions` |

### 3.10 state — 状态定义

引擎的状态模型定义，包括键盘状态和各子状态。`ImeState` 在 `api` 包中声明，其子状态类型在此包中定义。

| Class | 类型 | 可见性 | 说明 | 对应 Java |
|-------|------|--------|------|-----------|
| `KeyboardState` | `sealed class` | public | 键盘状态：`Idle`、`Typing`、`Selecting` 等（详见文档 100） | `State` 及其子类 |
| `InputListState` | `data class` | public | 输入列表状态 | 嵌入在 `InputList` 中的状态 |
| `InputItem` | `sealed class` | public | 输入项：`Char`、`Gap`、`MathExpr` | `InputViewData` 及其子类 |
| `CandidateListState` | `data class` | public | 候选状态 | 嵌入在 `CandidatesView` 中的状态 |
| `ClipboardState` | `data class` | public | 剪贴板状态 | 无直接对应 |
| `FavoriteListState` | `data class` | public | 收藏状态 | 无直接对应 |
| `PinyinCandidateFilterStateData` | `data class` | internal | 拼音候选过滤状态 | `PinyinCandidateFilterStateData` |
| `PinyinCandidateAdvanceFilterStateData` | `data class` | internal | 拼音候选高级过滤状态 | `PinyinCandidateAdvanceFilterStateData` |
| `PinyinCandidateChooseStateData` | `data class` | internal | 拼音候选选择状态 | `PinyinCandidateChooseStateData` |
| `SymbolChooseStateData` | `data class` | internal | 符号选择状态 | `SymbolChooseStateData` |
| `InputListCommitOptionChooseStateData` | `data class` | internal | 输入列表提交选项选择状态 | `InputListCommitOptionChooseStateData` |
| `InputCharsFlipStateData` | `data class` | internal | 输入字符翻转状态 | `InputCharsFlipStateData` |
| `InputCharsSlipStateData` | `data class` | internal | 输入字符滑行状态 | `InputCharsSlipStateData` |
| `EmojiChooseStateData` | `data class` | internal | Emoji 选择状态 | `EmojiChooseStateData` |
| `EditorEditStateData` | `data class` | internal | 编辑器编辑状态 | `EditorEditStateData` |
| `PagingStateData` | `data class` | internal | 分页状态 | `PagingStateData` |

### 3.11 logging — 日志基础设施

引擎库的日志基础设施。提供结构化日志记录能力，供引擎内部和 UI 层使用。完整设计详见文档 900。

| Class | 类型 | 可见性 | 说明 | 对应 Java |
|-------|------|--------|------|-----------|
| `ImeLog` | `object` | public | 日志入口 | `Logger` |
| `ImeLogger` | `interface` | public | 日志记录器接口 | `Logger` 接口 |
| `LogLevel` | `enum` | public | 日志等级：`VERBOSE`、`DEBUG`、`INFO`、`WARN`、`ERROR` | 无直接对应 |
| `LogEntry` | `data class` | public | 日志条目 | 无直接对应 |
| `LogWriter` | `interface` | public | 日志写入器接口 | 无直接对应 |

---

## 4. UI 库 `:ime-ui` 的 Class 组织

UI 库基于 Compose 提供缺省 UI 实现，所有组件仅依赖 `:ime-engine` 的公开 API。组件命名贴近 UI 业务语义，不使用 `Ime` 前缀。

### 4.1 integration — 集成组件

顶层集成组件，提供一站式完整输入法 UI。第三方应用直接使用这些组件即可获得完整输入法界面，无需手动组合底层组件。

| Class | 类型 | 说明 |
|-------|------|------|
| `KeyboardPanel` | `@Composable` | 叠加模式完整输入法组件：`Column { CandidateListPanel + InputListPanel + Box(GestureInputPanel + GestureFeedbackPanel + KeyGridPanel) + Toolbar }` |
| `KeyboardScreen` | `@Composable` | 全屏模式完整输入法组件：分离布局的手势输入面板与按键面板 |
| `KeyboardViewModel` | `class` | IME ViewModel，桥接 `ImeEngine` 和 Compose UI。`:app` 和第三方应用均直接使用，不继承也不扩展 |

### 4.2 panel — 三层面板

输入面板、按键面板与反馈面板三层分离设计。完整设计详见文档 150。

| Class | 类型 | 说明 |
|-------|------|------|
| `GestureInputPanel` | `@Composable` | 透明手势拦截层，接收用户手势并识别为 `InputGesture` |
| `GestureDetectorLayer` | `@Composable` | 手势检测层，拦截触摸事件并根据按键布局识别目标按键和手势类型 |
| `KeyGridPanel` | `@Composable` | 按键渲染层入口，根据 `KeyboardType` 分发到对应子面板 |
| `GestureFeedbackPanel` | `@Composable` | 透明反馈绘制层，根据 `GestureFeedbackState` 绘制指定类型的手势视觉反馈 |
| `GestureFeedbackState` | `class` | 手势反馈状态：`touchTrailPoints`、`pressedKeys`、`keyPath`、`xPadPath`、`fingerIndicator` |
| `GestureFeedbackPanelSet` | `sealed class` | 反馈面板配置集：`OverlaySet`（叠加模式）和 `FullScreenSet`（全屏分离模式） |
| `FeedbackElementType` | `enum` | 反馈元素类型：`TouchTrail`、`KeyHighlight`、`KeyPath`、`XPadPathHighlight`、`FingerIndicator` |
| `KeyGridPanelLayoutInfo` | `data class` | 按键面板布局信息：`keyPositions`、`xPadLayoutInfo`、`candidateLayoutInfo` |
| `LayoutMode` | `sealed class` | 布局模式：`Overlay`（叠加模式）和 `FullScreen`（全屏分离模式） |
| `FingerIndicatorState` | `data class` | 手指指示器状态：`position`、`pressed`、`visible` |

### 4.3 keyboard — 键盘组件

各类型键盘的按键面板实现和按键渲染组件。

| Class | 类型 | 说明 |
|-------|------|------|
| `StandardKeyGridPanel` | `@Composable` | 标准按键面板（拼音/拉丁/数字/符号/编辑/数学键盘） |
| `EmojiKeyGridPanel` | `@Composable` | Emoji 键盘面板 |
| `CandidateKeyGridPanel` | `@Composable` | 候选键盘面板 |
| `CommitOptionKeyGridPanel` | `@Composable` | 提交选项键盘面板 |
| `KeyView` | `@Composable` | 单个按键渲染（纯展示，无触摸处理，无手势反馈） |
| `XPadView` | `@Composable` | X-Pad 六边形面板（详见文档 700） |

### 4.4 candidate — 候选栏

候选字词的展示与交互。

| Class | 类型 | 说明 |
|-------|------|------|
| `CandidateListPanel` | `@Composable` | 候选栏面板 |
| `CandidateItem` | `@Composable` | 单个候选项 |

### 4.5 input — 输入栏

输入列表的展示与交互。完整设计详见文档 200。

| Class | 类型 | 说明 |
|-------|------|------|
| `InputListPanel` | `@Composable` | 输入栏面板 |
| `CharInputItem` | `@Composable` | 字符输入项 |
| `GapInputItem` | `@Composable` | 间隙输入项（光标位置） |
| `MathExprInputItem` | `@Composable` | 数学表达式输入项 |

### 4.6 editor — 编辑器桥接

应用内嵌场景的编辑器桥接实现。

| Class | 类型 | 说明 |
|-------|------|------|
| `EditTextBridge` | `class` | 面向 `EditText` 的桥梁实现，继承 `BaseImeOutputBridge`。构造时接受 `supplier: () -> EditText?` |

### 4.7 clipboard — 剪贴板 UI

剪贴板相关 UI 组件。完整设计详见文档 600。

| Class | 类型 | 说明 |
|-------|------|------|
| `ClipTipPopup` | `@Composable` | 剪贴板提示弹窗 |

### 4.8 favorite — 收藏 UI

收藏管理相关 UI 组件。完整设计详见文档 600。

| Class | 类型 | 说明 |
|-------|------|------|
| `FavoriteListPanel` | `@Composable` | 收藏列表面板 |
| `FavoriteItem` | `@Composable` | 收藏项（含滑动删除） |

### 4.9 practice — 输入练习 UI

输入练习与程序化输入动画相关组件。完整设计详见文档 930。

| Class | 类型 | 说明 |
|-------|------|------|
| `FingerOverlay` | `@Composable` | 手指指示器 |
| `ActionPlayerPanel` | `@Composable` | 播放控制面板 |

### 4.10 theme — 主题系统

键盘主题的配色与主题切换。

| Class | 类型 | 说明 |
|-------|------|------|
| `KeyboardColors` | `data class` | 颜色定义（键盘/候选/输入栏/X-Pad） |
| `KeyboardThemes` | `object` | 预置主题（Light/Night） |
| `KeyboardTheme` | `@Composable` | 主题 Composable（支持跟随系统） |
| `LocalKeyboardColors` | `ProvidableCompositionLocal` | CompositionLocal 提供颜色 |

---

## 5. 应用模块 `:app` 的 Class 组织

应用模块是引擎库和 UI 库的官方消费者，提供系统 IME 服务、配置持久化、设置界面和引导页面。直接使用顶级包名 `org.crazydan.studio.app.ime.kuaizi`，不加子模块名。

### 5.1 顶级 — 核心服务

| Class | 类型 | 说明 | 对应 Java |
|-------|------|------|-----------|
| `IMEService` | `class` | 系统 IME 服务，继承 `InputMethodService`。创建 `ImeEngine`，接入 `InputConnectionBridge`，使用 `KeyboardPanel` 作为输入视图 | `IMEService` |
| `InputConnectionBridge` | `class` | 面向系统 `InputConnection` 的桥梁实现，继承 `BaseImeOutputBridge`。构造时接受 `supplier: () -> InputConnection?` | 嵌入在 `IMEService` 中的 `InputConnection` 操作 |
| `ConfigRepository` | `class` | 配置持久化仓库，基于 DataStore 存取 `ImeConfig`。处理运行时覆盖与持久化同步 | `Config` + `SharedPreferences` |
| `IMESubtype` | `class` | IME 子类型定义 | `IMESubtype` |

### 5.2 settings — 设置页面

配置界面，使用 Compose 实现。完整设计详见文档 920。

| Class | 类型 | 说明 | 对应 Java |
|-------|------|------|-----------|
| `SettingsScreen` | `@Composable` | 设置主页面 | `Preferences` / `PreferencesTheme` |
| `LogLevelSetting` | `@Composable` | 日志等级设置项 | 无直接对应 |
| `LogStoragePathSetting` | `@Composable` | 日志存储路径设置项 | 无直接对应 |

### 5.3 logging — 日志界面

日志查看与导出界面。完整设计详见文档 900。

| Class | 类型 | 说明 | 对应 Java |
|-------|------|------|-----------|
| `LogViewerScreen` | `@Composable` | 日志查看页面 | 无直接对应 |
| `LogExportScreen` | `@Composable` | 日志导出页面 | 无直接对应 |
| `LogViewerViewModel` | `class` | 日志查看 ViewModel | 无直接对应 |

### 5.4 guide — 引导页面

输入法引导与练习页面。

| Class | 类型 | 说明 | 对应 Java |
|-------|------|------|-----------|
| `GuideScreen` | `@Composable` | 引导主页面 | `Guide` |
| `KeyboardSandboxView` | `@Composable` | 键盘沙盒视图 | `KeyboardSandboxView` |
| `KeyImageRender` | `class` | 按键图像渲染 | `KeyImageRender` |

---

## 6. 跨模块 Class 归属对照

某些 class 在概念上涉及多个模块，但实现只归属于一个模块。下表明确这些 class 的归属：

| Class | 归属模块 | 包路径 | 说明 |
|-------|---------|--------|------|
| `ImeOutputBridge` | `:ime-engine` | `api` | 接口定义在引擎库，实现在 `:app`（`InputConnectionBridge`）和 `:ime-ui`（`EditTextBridge`） |
| `BaseImeOutputBridge` | `:ime-engine` | `api` | 抽象类定义在引擎库，供各桥梁实现继承 |
| `InputConnectionBridge` | `:app` | 顶级 | 面向 Android `InputConnection`，需依赖 Android 框架 |
| `EditTextBridge` | `:ime-ui` | `editor` | 面向 `EditText`，需依赖 Android View |
| `KeyboardViewModel` | `:ime-ui` | `integration` | `:app` 和第三方均直接使用，不继承不扩展 |
| `ImeConfig` | `:ime-engine` | `api` | 统一配置，`:app` 负责持久化，UI 库通过 `ImeState.config` 读取 |
| `ImeSqliteDictProvider` | `:ime-engine` | `api` | 内置字典实现，需 Android `Context`（字典 I/O 是引擎中唯一依赖 Android 框架的部分） |
| `ImeState` | `:ime-engine` | `api` | 引擎输出状态，UI 层订阅 |
| `InputGesture` | `:ime-engine` | `input` | 输入面板的输出类型，UI 层构造后传给引擎 |
| `GestureFeedbackState` | `:ime-ui` | `panel` | 反馈状态由输入面板驱动，反馈面板消费 |

---

## 7. Java 版本 class 迁移对照

下表列出 Java 版本中的主要 class 与 v4 设计中的对应关系，用于确保迁移无遗漏。标记为「已废弃」的 class 表示其职责已被 v4 架构消解或合并。

### 7.1 核心引擎

| Java Class | v4 对应 | 说明 |
|-----------|---------|------|
| `IMEditor` | `ImeEngine` | 引擎核心入口点，MVI 模式替代消息中介 |
| `IMEditorView` | 已废弃 | View 层职责由 `:ime-ui` 的 Compose 组件承担 |
| `IMEditorDict` | `ImeDictProvider` / `ImeSqliteDictProvider` | 不再单例，由引擎使用者显式创建 |
| `IMEConfig` | `ImeConfig` | 统一配置，合并原引擎配置与应用配置 |
| `IMEService` | `IMEService`（`:app`） | 不再充当消息中介，仅管理 `InputConnection` 生命周期 |
| `IMESubtype` | `IMESubtype`（`:app`） | 直接迁移 |
| `Key` | `InputKey` | sealed class 替代继承体系 |
| `KeyFactory` | `InputKey` 的 companion factory | 按键工厂 |
| `Input` | `InputKey` / `InputWord` | 拆分为按键和字词两个维度 |
| `InputFactory` | `InputWord` 的 companion factory | 输入工厂 |
| `Keyboard` | `Keyboard`（`:ime-engine` domain） | sealed class 组合模式替代继承 |
| `BaseKeyboard` | `Keyboard` | sealed class 基类 |
| `InputList` | `InputList`（`:ime-engine` domain） | 不可变数据模型重构 |
| `Inputboard` | `Inputboard`（`:ime-engine` domain） | 直接迁移 |
| `Favoriteboard` | `Favoriteboard`（`:ime-engine` domain） | 直接迁移 |
| `State` | `KeyboardState` | sealed class 状态定义 |
| `KeyboardContext` | `KeyboardContext`（`:ime-engine` domain） | 直接迁移 |

### 7.2 消息体系

| Java Class | v4 对应 | 说明 |
|-----------|---------|------|
| `UserKeyMsg` / `UserKeyMsgType` / `UserKeyMsgData` | `InputGesture` | 统一为坐标无关的逻辑手势 |
| `UserInputMsg` / `UserInputMsgType` / `UserInputMsgData` | `ImeIntent` | 统一为用户意图 |
| `InputMsg` / `InputMsgType` / `InputMsgData` | `ImeOutput` | 统一为引擎输出 |
| `InputMsgListener` | `ImeOutputBridge` | 桥接模式替代监听器 |
| `UserMsgListener` | 已废弃 | 引擎内部统一 `dispatchToTarget()` |
| `BaseMsg` | 已废弃 | v4 不使用通用消息基类 |

### 7.3 UI 层

| Java Class | v4 对应 | 说明 |
|-----------|---------|------|
| `MainboardView` | `KeyboardPanel` / `KeyboardScreen` | Compose 组件替代自定义 View |
| `KeyboardView` | `KeyGridPanel` + `GestureInputPanel` + `GestureFeedbackPanel` | 三层分离替代单 View |
| `KeyboardViewGestureListener` | `GestureDetectorLayer` | Compose 手势检测替代 View 手势监听 |
| `KeyboardViewKeyAnimator` | `KeyView`（状态渲染）+ `GestureFeedbackPanel`（临时反馈） | 持续性状态与临时性反馈分离 |
| `CandidatesView` | `CandidateListPanel` | Compose 组件替代自定义 View |
| `InputListView` | `InputListPanel` | Compose 组件替代自定义 View |
| `InputListViewReadonly` | 已废弃 | 只读模式通过 `ImeConfig` 控制 |
| `FavoriteboardView` | `FavoriteListPanel` | Compose 组件替代自定义 View |
| `InputQuickListView` | `CandidateListPanel`（补全候选） | 功能合并 |
| `XPadView` | `XPadView`（`:ime-ui` keyboard） | Compose Canvas 重构 |
| `XPadState` | `GestureFeedbackState` | 状态管理重构 |
| `BaseThemedView` | `KeyboardTheme` | Compose 主题系统替代 |
| `BaseMsgListenerView` | 已废弃 | MVI 模式下 View 不再监听消息 |

### 7.4 应用层

| Java Class | v4 对应 | 说明 |
|-----------|---------|------|
| `ImeIntegratedActivity` | `KeyboardPanel` + `EditTextBridge` | Compose 组件 + 桥梁替代特定 Activity |
| `ImeSupportEditText` | `EditTextBridge` | 桥梁模式替代 `InputMsgListener` 实现 |
| `Preferences` | `SettingsScreen` | Compose 设置页面替代 Activity |
| `PreferencesTheme` | `SettingsScreen` 中的主题设置 | 合并到设置页面 |
| `Guide` | `GuideScreen` | Compose 引导页面 |
