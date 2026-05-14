# 010 — 命名规范

本文档定义 v4 版本三层库架构下的统一命名规范，确保通过类名即可识别其所在模块和职能。所有设计文档和代码实现必须遵循本规范。

---

## 1. 三层模块命名

| 模块 | 命名规则 | 示例 |
|------|----------|------|
| `:ime-engine` | 公开 class 以 `Ime` 为前缀 | `ImeEngine`, `ImeConfig`, `ImeOutput`, `ImeState`, `ImeIntent`, `ImeOutputBridge` |
| `:ime-ui` | 不使用 `Ime` 前缀，贴近 UI 业务命名 | `KeyboardPanel`, `EditTextBridge`, `GestureFeedbackPanel`, `CandidateListPanel` |
| `:app` | 不使用 `Ime` 前缀，贴近应用业务命名 | `IMEService`, `ConfigRepository`, `InputConnectionBridge` |

### 1.1 engine 模块

所有对外的 class（包括 sealed class、data class、enum、interface）均以 `Ime` 作为前缀，作为引擎库的命名空间标识。内部实现类（如 `KeyboardStateMachine`、`InputListOperator`、`FeatureRegistry`）不强制使用 `Ime` 前缀，因为它们不对外暴露。

### 1.2 ui 模块

组件命名贴近其 UI 职能，使用业务语义化的名称：

| 后缀 | 含义 | 示例 |
|------|------|------|
| `Panel` | 容器/组合器（组合多个子组件） | `KeyboardPanel`, `GestureInputPanel`, `CandidateListPanel`, `InputListPanel`, `KeyGridPanel`, `GestureFeedbackPanel` |
| `Screen` | 全屏界面（UI 库或应用层页面） | `KeyboardScreen`, `SettingsScreen`, `MainScreen`, `ExerciseScreen` |

主题系统使用 `Keyboard` 前缀（如 `KeyboardColors`、`KeyboardTheme`），与 Material3 的 `MaterialTheme` 等系统命名区分。

### 1.3 app 模块

Android 系统服务类沿用平台命名惯例（如 `IMEService`），配置类使用职能名称（如 `ConfigRepository`），桥接类使用目标对象 + Bridge 后缀命名（如 `InputConnectionBridge`），实现 ImeOutputBridge 接口。页面以 `Screen` 为后缀（如 `SettingsScreen`、`MainScreen`）。

---

## 2. KeyGridPanel 子类命名

`KeyGridPanel` 的子类以 `KeyGridPanel` 为**后缀**，前缀表达键盘变体类型：

| 类名 | 说明 |
|------|------|
| `StandardKeyGridPanel` | 标准键盘（拼音、拉丁、数字、符号、编辑器、数学） |
| `EmojiKeyGridPanel` | Emoji 面板 |
| `CandidateKeyGridPanel` | 候选键盘 |
| `CommitOptionKeyGridPanel` | 提交选项键盘 |

---

## 3. 引擎 API 命名

| 类型 | 命名 | 说明 |
|------|------|------|
| Intent | `ImeIntent.PerformEdit(EditorAction)` | 不是 `ImeIntent.EditorAction(EditorActionType)` |
| Output | `ImeOutput.PerformEdit(EditorAction)` | PerformEdit 与 Output 对称使用同一 `EditorAction` 枚举 |
| 手势输入 | `ImeEngine.handleGesture(InputGesture)` | 不是 `onKeyPress` / `handleKeyPress` |
| 意图处理 | `ImeEngine.handleIntent(ImeIntent)` | 直接发送意图 |

---

## 4. 常见已更名对照

| 旧名称 | 新名称 | 备注 |
|--------|--------|------|
| `EditorActionType` | `EditorAction` | 统一为单一枚举 |
| `StandardKeyboard` | `StandardKeyGridPanel` | 去掉 `onKeyPress`，纯渲染 |
| `KeyPanel` | `KeyGridPanel` | 强调 Grid 布局特征 |
| `StandardKeyPanel` | `StandardKeyGridPanel` | 跟随 KeyGridPanel 更名 |
| `InputPanel` | `GestureInputPanel` | 强调手势输入职能 |
| `KeyboardView` | `KeyboardPanel` | 明确容器角色；三层面板直接叠加 |
| `CandidateBar` / `InputBar` | `CandidateListPanel` / `InputListPanel` | 统一 Panel 后缀 |
| `GuideScreen` | `MainScreen` | 准确反映主界面职能 |
| `InputPracticeScreen` | `ExerciseScreen` | 合并练习与演示 |
| `FingerOverlayState` | `GestureFeedbackState.fingerIndicator` | 合并到反馈状态 |
| `ImeEngineConfig` | `ImeConfig` | 合并引擎配置与应用配置 |
| `Config` | `ImeConfig.UiConfig` | 应用配置合并到 ImeConfig |
| `disable*` / `enable*` 前缀 | `*Enabled` 后缀 | 肯定式命名，如 `keyAnimationEnabled` |
| `ImeSupportEditText` / `ImeEditText` / `EditorField` | `EditTextBridge` | 桥梁模式替代独立编辑框组件 |
| `EditorHost` / `InputHostView` | （移除）| 替换为 ImeOutputBridge 接入示例 |
| `EditorState` | （移除）| 撤销状态由桥梁内部管理 |
| `AppLog` / `AppLogger` | `ImeLog` / `ImeLogger` | Ime 前缀，划归 engine 模块 |
| `LogExportActivity` | `LogExportScreen` | 页面以 Screen 为后缀，划归 app 模块 |
| `CandidateState` | `CandidateListState` | 体现列表语义 |
| `FavoritesState` | `FavoriteListState` | 体现列表语义；单数 Favorite + ListState |
| `CandidatePanel` | `CandidateListPanel` | 体现列表语义 |
| `FavoritesPanel` | `FavoriteListPanel` | 体现列表语义；单数 Favorite + ListPanel |
| `CandidatePager` | `CandidateListPager` | 体现列表语义 |
| `ImeOutput.EditAction` | `ImeOutput.PerformEdit` | 动作导向命名，与 ImeIntent.PerformEdit 对称 |

---

## 5. 包命名

- 格式：`org.crazydan.studio.app.ime.kuaizi.<module>.<submodule>`
- `:app` 模块无子模块名，直接使用顶级包名
- 全小写，不使用下划线
- 模块划分与功能对应，不按技术层划分

```
org.crazydan.studio.app.ime.kuaizi.engine.api
org.crazydan.studio.app.ime.kuaizi.engine.domain
org.crazydan.studio.app.ime.kuaizi.engine.dict
org.crazydan.studio.app.ime.kuaizi.ui.theme
org.crazydan.studio.app.ime.kuaizi.ui.keyboard
org.crazydan.studio.app.ime.kuaizi.ui.integration
org.crazydan.studio.app.ime.kuaizi       ← :app 模块（无子模块名）
```

---

## 6. 禁止使用的名称

以下名称已废弃，不得在设计文档和代码中使用：

- `KeyPanel`（使用 `KeyGridPanel`）
- `StandardKeyboard` / `StandardKeyPanel`（使用 `StandardKeyGridPanel`）
- `InputPanel`（使用 `GestureInputPanel`）
- `KeyboardView`（使用 `KeyboardPanel`）
- `CandidateBar` / `InputBar`（使用 `CandidateListPanel` / `InputListPanel`）
- `EditorActionType`（使用 `EditorAction`）
- `ImeEngineConfig`（使用 `ImeConfig`）
- `onKeyPress`（使用 `handleGesture` / `handleIntent`）
- `disable*` / `enable*` 前缀配置字段（使用 `*Enabled` 后缀）
- `EditorField`（使用 `EditTextBridge`）
- `EditorHost`（使用 Bridge 接入示例，见文档 160 §5.5）
- `InputHostView`（使用 Bridge 接入示例，见文档 160 §5.5）
- `ImeEditText` / `ImeSupportEditText`（使用 `EditTextBridge`）
- `EditorState`（撤销状态由 BaseImeOutputBridge 内部管理）
- `AppLog` / `AppLogger`（使用 `ImeLog` / `ImeLogger`）
- `LogExportActivity`（使用 `LogExportScreen`）
- `CandidateState`（使用 `CandidateListState`）
- `FavoritesState`（使用 `FavoriteListState`）
- `CandidatePanel`（使用 `CandidateListPanel`）
- `FavoritesPanel`（使用 `FavoriteListPanel`）
- `CandidatePager`（使用 `CandidateListPager`）
- `ImeOutput.EditAction`（使用 `ImeOutput.PerformEdit`）
- `InputScreen`（使用 `KeyboardPanel` 叠加模式 / `KeyboardScreen` 全屏模式）
- `ThreeLayerKeyboardArea`（使用 `KeyboardPanel`，三层面板直接叠加）
- `KeyboardArea`（使用 `KeyboardPanel`，三层面板直接叠加）
