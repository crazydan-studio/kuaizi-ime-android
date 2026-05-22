# 命名规范

本文档定义 v4 版本三层库架构下的统一命名规范，确保通过类名即可识别其所在模块和职能。所有设计文档和代码实现必须遵循本规范。

---

## 1 三层模块命名

| 模块 | 命名规则 | 示例 |
|------|----------|------|
| `:ime-engine` | 公开 class 以 `Ime` 为前缀 | `ImeEngine`, `ImeConfig`, `EditorAction`, `ImeState`, `ImeIntent`, `ImeEditorBridge` |
| `:ime-ui` | 不使用 `Ime` 前缀，贴近 UI 业务命名 | `KeyboardHost`, `EditTextBridge`, `GestureFeedbackPanel`, `CandidateListPanel` |
| `:app` | 不使用 `Ime` 前缀，贴近应用业务命名 | `IMEService`, `ConfigDataStore`, `InputConnectionBridge` |

### 1.1 engine 模块

所有对外的 class（包括 sealed class、data class、enum、interface）均以 `Ime` 作为前缀，作为引擎库的命名空间标识。内部实现类（如 `KeyboardStateMachine`、`InputListOperator`、`FeatureRegistry`）不强制使用 `Ime` 前缀，因为它们不对外暴露。

### 1.2 ui 模块

组件命名贴近其 UI 职能，使用业务语义化的名称：

| 后缀 | 含义 | 示例 |
|------|------|------|
| `Panel` | 容器 / 组合器（组合多个子组件） | `KeyboardHost`, `GestureInputPanel`, `CandidateListPanel`, `InputListPanel`, `KeyLayoutPanel`, `GestureFeedbackPanel` |
| `Screen` | 全屏界面（应用层页面） | `SettingsScreen`, `MainScreen`, `ExerciseScreen`（:app 模块） |

> **注意**：`KeyboardHost` 统一了叠加模式和全屏模式两种交互形式，内部包含候选栏、输入栏、工具栏和键盘区域。

主题系统使用 `Keyboard` 前缀（如 `KeyboardColors`、`KeyboardTheme`），与 Material3 的 `MaterialTheme` 等系统命名区分。

### 1.3 app 模块

Android 系统服务类沿用平台命名惯例（如 `IMEService`），配置类使用职能名称（如 `ConfigDataStore`），桥接类使用目标对象 + Bridge 后缀命名（如 `InputConnectionBridge`），实现 ImeEditorBridge 接口。页面以 `Screen` 为后缀（如 `SettingsScreen`、`MainScreen`）。

---

## 2 KeyLayoutPanel 子类命名

`KeyLayoutPanel` 的子类以 `KeyLayoutPanel` 为**后缀**，前缀表达键盘变体类型：

| 类名 | 说明 |
|------|------|
| `StandardKeyLayoutPanel` | 标准键盘（拼音、拉丁、数字、符号、数学） |
| `EmojiKeyLayoutPanel` | Emoji 面板 |
| `CandidateKeyLayoutPanel` | 候选键盘 |
| `CommitOptionKeyLayoutPanel` | 提交选项键盘 |

---

## 3 引擎 API 命名

| 类型 | 命名 | 说明 |
|------|------|------|
| Intent | `ImeIntent.PerformEdit(EditorEditAction)` | 不是 `ImeIntent.EditorEditAction(EditorActionType)` |
| Output | `EditorAction.PerformEdit(EditorEditAction)` | PerformEdit 与 EditorAction 对称使用同一 `EditorEditAction` 枚举 |
| 手势输入 | `ImeEngine.handleGesture(InputGesture)` | 不是 `onKeyPress` / `handleKeyPress` |
| 意图处理 | `ImeEngine.handleIntent(ImeIntent)` | 直接发送意图 |

---

## 4 包命名

- 格式：`org.crazydan.studio.app.ime.kuaizi.<module>.<submodule>`
- `:app` 模块无子模块名，直接使用顶级包名
- 全小写，不使用下划线
- 模块划分与功能对应，不按技术层划分

```
org.crazydan.studio.app.ime.kuaizi.engine.core
org.crazydan.studio.app.ime.kuaizi.engine.domain
org.crazydan.studio.app.ime.kuaizi.engine.dict
org.crazydan.studio.app.ime.kuaizi.ui.theme
org.crazydan.studio.app.ime.kuaizi.ui.keyboard
org.crazydan.studio.app.ime.kuaizi.ui.integration
org.crazydan.studio.app.ime.kuaizi       ← :app 模块（无子模块名）
```

---

## 5 命名选择指引

以下列出 v4 设计中经过取舍后采用的命名及其替代方案，供后续设计决策参考。列出的替代方案并非错误，只是当前设计中选择了更合适的名称。

### 5.1 面板与布局

| 选用名称 | 替代方案 | 选择理由 |
|----------|---------|---------|
| `KeyLayoutPanel` | `KeyPanel`, `KeyGridPanel` | `Layout` 更准确表达按键布局计算的职责 |
| `StandardKeyLayoutPanel` | `StandardKeyboard`, `StandardKeyPanel`, `StandardKeyGridPanel` | 与 `KeyLayoutPanel` 子类命名规则一致 |
| `EmojiKeyLayoutPanel` | `EmojiKeyGridPanel` | 同上 |
| `CandidateKeyLayoutPanel` | `CandidateKeyGridPanel` | 同上 |
| `CommitOptionKeyLayoutPanel` | `CommitOptionKeyGridPanel` | 同上 |
| `GestureInputPanel` | `InputPanel` | `GestureInput` 更精确表达手势输入捕获职责 |
| `KeyboardHost` | `KeyboardView`, `KeyboardArea`, `KeyboardPanel`, `KeyboardScreen`, `InputScreen`, `ThreeLayerKeyboardArea` | Compose 宿主组件惯例用 `Host` 后缀；该组件内部已包含三层面板叠加区域 |
| `CandidateListPanel` | `CandidateBar`, `CandidatePanel` | `List` 表达列表滚动行为，`Panel` 表达容器组合 |
| `InputListPanel` | `InputBar` | 同上 |
| `FavoriteListPanel` | `FavoritesPanel` | `List` 与 `CandidateListPanel` 命名一致 |
| `CandidateListPager` | `CandidatePager` | `List` 明确所属面板 |
| `CandidateList` | `CandidateState` | 状态类以 `List` 后缀表达其数据集合本质 |

### 5.2 引擎 API

| 选用名称 | 替代方案 | 选择理由 |
|----------|---------|---------|
| `ImeConfig` | `ImeEngineConfig` | `Ime` 前缀已表达归属，无需冗余 `Engine` |
| `EditorEditAction` | `EditorActionType` | 枚举命名不带 `Type` 后缀，Kotlin 惯例 |
| `EditorAction.PerformEdit` | `EditorAction.EditAction` | `PerformEdit` 与 `ImeIntent.PerformEdit` 对称 |

### 5.3 桥接与输出

| 选用名称 | 替代方案 | 选择理由 |
|----------|---------|---------|
| `EditTextBridge` | `EditorField`, `ImeEditText`, `ImeSupportEditText` | `Bridge` 后缀与 `ImeEditorBridge` 一致；`EditText` 明确桥接目标 |
| `BaseImeEditorBridge` 内部管理撤销状态 | `EditorState` | 撤销状态为桥接实现细节，不独立暴露 |

### 5.4 输入动作程序化

| 选用名称 | 替代方案 | 选择理由 |
|----------|---------|---------|
| `InputActionScript` | `ActionScript` | `InputAction` 前缀统一归属播放相关模型 |
| `InputActionScriptLoader` | `ActionScriptLoader` | 同上 |
| `InputActionScriptCompiler` | `ActionScriptCompiler` | 同上 |

### 5.5 日志

| 选用名称 | 替代方案 | 选择理由 |
|----------|---------|---------|
| `ImeLog` | `AppLog` | `Ime` 前缀归属引擎模块，`App` 暗示应用层 |
| `ImeLogger` | `AppLogger` | 同上 |
| `LogExportScreen` | `LogExportActivity` | 应用页面统一用 `Screen` 后缀 |

### 5.6 配置字段

| 选用名称 | 替代方案 | 选择理由 |
|----------|---------|---------|
| `*Enabled` 后缀 | `disable*` / `enable*` 前缀 | 布尔配置字段统一使用 `Enabled` 后缀，语义更清晰 |
