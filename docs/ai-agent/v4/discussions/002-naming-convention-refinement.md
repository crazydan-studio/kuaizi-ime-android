# 002 — 分层命名规范细化与 UI 组件重命名

## 基本信息

| 字段 | 值 |
|------|-----|
| 讨论日期 | 2026-05-13 |
| 参与者 | 用户、AI Designer |
| 主题 | 细化分层命名规范、统一 UI 组件 Panel 后缀、合并应用层页面 |

---

## 讨论内容

### 1. 分层命名规范的进一步细化

在 001 讨论中确定了基本的分层命名规范（engine 用 `Ime` 前缀、ui 和 app 不用 `Ime` 前缀），本次讨论进一步明确了以下细节：

- **:ime-engine 模块**：所有对外公开 API 类始终以 `Ime` 作为前缀，作为引擎库的命名空间标识，便于第三方应用识别其来源。内部领域类（如 `KeyboardStateMachine`、`InputListOperator`、`FeatureRegistry`）不强制加前缀，因为它们不是公开 API
- **:ime-ui 模块**：组件命名贴近其 UI 职能，不以 `Ime` 为前缀。进一步确定后缀规则：
  - **`Panel` 后缀**：面板类和容器类统一使用 `Panel` 后缀，不再使用 `Bar` 后缀（如候选栏从 `CandidateBar` 改为 `CandidatePanel`）
  - **`View` 后缀**：视图类使用 `View` 后缀（如 `XPadView`、`InputHostView`）
  - **直接职能名**：编辑器类直接使用职能名称（如 `EditorField`、`EditorHost`）
  - **`Keyboard` 前缀**：主题系统使用 `Keyboard` 前缀（如 `KeyboardColors`、`KeyboardTheme`），与 Material3 的 `MaterialTheme` 等系统命名区分
- **:app 模块**：不以 `Ime` 为前缀，贴近应用层业务命名。页面以 `Screen` 为后缀（如 `MainScreen`、`ExerciseScreen`、`SettingsScreen`），Android 系统服务类沿用平台命名惯例（如 `IMEService`）

### 2. UI 组件重命名为统一 Panel 后缀

用户指出 :ime-ui 模块中的面板和容器类应统一使用 `Panel` 后缀，便于从类名直观识别其组件类型。具体重命名如下：

| 旧名 | 新名 | 说明 |
|------|------|------|
| `InputPanel` | `GestureInputPanel` | 强调手势拦截职能，与其他 Panel 区分 |
| `CandidateBar` | `CandidatePanel` | 统一 Panel 后缀，候选字面板 |
| `InputBar` | `InputListPanel` | 统一 Panel 后缀，输入列表面板 |
| `KeyPanel` | `KeyGridPanel` | 明确为网格布局的按键面板，与其他 KeyGrid 变体区分 |
| `KeyboardView` | `KeyboardPanel` | 统一 Panel 后缀，键盘顶层组合面板 |

派生重命名（跟随父类更名）：

| 旧名 | 新名 |
|------|------|
| `StandardKeyPanel` | `StandardKeyGridPanel` |
| `EmojiKeyPanel` | `EmojiKeyGridPanel` |
| `CandidateKeyPanel` | `CandidateKeyGridPanel` |
| `CommitOptionKeyPanel` | `CommitOptionKeyGridPanel` |
| `KeyPanelLayoutInfo` | `KeyGridPanelLayoutInfo` |
| `KeyPanelPositionResolver` | `KeyGridPanelPositionResolver` |

### 3. 应用层页面合并与重命名

用户对 :app 模块的页面进行了整理：

- **`InputPracticeScreen` 与 `ExerciseScreen` 合并为 `ExerciseScreen`**：输入练习和操作演练在功能上高度重叠，合并为统一的练习界面，预备用于用户使用练习。合并后的 `ExerciseScreen` 同时包含输入动作程序化的动画演示和交互式练习功能
- **`GuideScreen` 更名为 `MainScreen`**：该页面作为应用的主界面（输入法主窗口），`Guide` 不能准确表达其职能，`Main` 更贴切地反映了其作为应用入口页面的定位

### 4. ComposeKeyPositionResolver 模块归属

确认 `ComposeKeyPositionResolver` 应属于 `:ime-ui` 模块而非 `:app` 模块。理由如下：

- `ComposeKeyPositionResolver` 依赖 Compose 布局系统（`onGloballyPositioned` / `layoutInfo`），属于 Compose UI 实现细节
- 其接口 `KeyPositionResolver` 定义在 `:ime-engine` 模块（坐标解析的抽象契约），而 Compose 实现自然归入 `:ime-ui`
- 文档 930 的代码组织表新增「模块」列，明确标注各行的模块归属

### 5. 架构图更新

根据命名变更更新了 3 个 PlantUML 架构图（01-layer-architecture、03-ui-layer-classes、04-mvi-dataflow），并重新生成了 PNG 和 SVG 格式的图片，打包为 `kuaizi-ime-v4-architecture-diagrams.zip`。02-engine-domain-classes 无需修改（纯引擎模块类名）。

---

## 结论与决策

| # | 决策 | 说明 |
|---|------|------|
| 1 | :ime-ui 统一 Panel 后缀 | 面板和容器类统一使用 `Panel` 后缀，不再使用 `Bar` 后缀；`CandidateBar` → `CandidatePanel`，`InputBar` → `InputListPanel` |
| 2 | 键盘组件重命名 | `KeyboardView` → `KeyboardPanel`（顶层组合面板），`InputPanel` → `GestureInputPanel`（强调手势职能），`KeyPanel` → `KeyGridPanel`（明确网格布局） |
| 3 | 练习页面合并 | `InputPracticeScreen` + `ExerciseScreen` → `ExerciseScreen`（统一练习界面） |
| 4 | 主界面重命名 | `GuideScreen` → `MainScreen`（应用主界面） |
| 5 | ComposeKeyPositionResolver 归属 | 接口 `KeyPositionResolver` 在 :ime-engine，Compose 实现在 :ime-ui |
| 6 | 架构图同步更新 | 3 个 PUML 图更新类名并重新生成图片（架构图不做 git 提交） |

---

## 待定事项

| # | 事项 | 说明 |
|---|------|------|
| 1 | GestureInputPanel 手势检测性能 | 同 001 #1，需原型阶段验证 |
| 2 | Compose 在 IME 中的性能 | 同 001 #2，需原型阶段验证 |
| 3 | 测试用例编写 | 同 001 #3，9 个测试文档待验收员编写 |
