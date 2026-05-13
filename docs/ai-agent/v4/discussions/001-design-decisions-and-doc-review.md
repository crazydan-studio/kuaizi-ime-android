# 001 — v4 设计决策完善与全文档审查

## 基本信息

| 字段 | 值 |
|------|-----|
| 讨论日期 | 2026-05-12 |
| 参与者 | 用户、AI Designer |
| 主题 | 确定多项设计决策、执行全文档审查、建立测试文档体系 |

---

## 讨论内容

### 1. 命名规范决策

用户提出统一类名前缀的规范要求，经讨论后确定以下规则：

- **不以 `Kuaizi` 作类名前缀**：去除所有 `KuaiziXxx` 形式的类名
- **公共 API 类统一 `Ime` 前缀**：如 `ImeEngine`、`ImeState`、`ImeIntent`、`ImeConfig`
- **`IME` 前缀规范**：`IMEState` → `ImeState`、`IMEIntent` → `ImeIntent`（:app 模块平台惯例 `IMEService` 例外）
- **StandardKeyboard 保持命名不变**：这是 Compose 标准键盘 Composable 组件的既定命名，不做改动
- **发布包名保持 `Kuaizi_IME-{version}.apk`**：发布包名是面向用户的外部标识，不在重命名范围内

### 2. 包名规范决策

讨论了各模块的包名结构，最终确定：

- **各模块顶级包名始终为 `org.crazydan.studio.app.ime.kuaizi`**
- **:app 模块不加子模块名**，直接使用顶级包名 `org.crazydan.studio.app.ime.kuaizi`
- :ime-engine 和 :ime-ui 子模块可以在顶级包名下有自己的子包（如 `.engine`、`.ui`）
- 此决策消除了原先 :app 模块可能使用 `.app.` 子包的歧义

### 3. ImeConfig 配置统一与运行时优先语义

这是讨论中最核心的架构决策，涉及多个方面：

- **ImeEngineConfig → ImeConfig**：重命名并合并原 `ImeEngineConfig`（引擎配置）与 :app 的 `Config`（应用配置）为统一的 `ImeConfig`
- **EngineConfig + UiConfig 嵌套隔离**：`ImeConfig` 同时包含引擎配置和 UI 配置，通过嵌套 data class 明确隔离，方便第三方按需使用
- **运行时修改始终优先于应用侧配置**：通过键盘 UI 进行的临时修改（如临时切换左右手模式）始终优先于应用侧持久化配置，直到应用重启
- **重启时从持久化配置重新初始化**：重启后运行时覆盖失效，ImeConfig 根据持久化配置重新初始化
- **runtimeOverrides 记录**：`ImeConfig.runtimeOverrides: Set<ConfigField>` 记录被运行时覆盖的字段，持久化同步时跳过这些字段

### 4. :app 模块定位

明确了 :app 模块在三层库架构中的定位：

- **视为对 :ime-engine、:ime-ui 库的使用特例**，地位与第三方应用相同
- **直接使用 UI 库的 KeyboardViewModel**，不继承也不扩展
- **配置通过 ImeConfig 与引擎交互**，不与 UI 库 ViewModel 耦合
- 配置持久化（DataStore）和 InputConnection 桥接等平台特定职责由 :app 中的独立组件承担

### 5. 全文档审查

用户要求检查全部 14 个设计文档，处理冲突内容、合并重复内容、移除冗余内容、确保内容准确清晰且无歧义。审查分三轮执行：

- **第1轮**：修正 `SwitchIME` → `SwitchIme`、:app 包名、日志配置命名、920 映射表字段不一致、920 配置项清单字段名（4 文档 5 问题）
- **第2轮**：ImeConfig.UiConfig 8 个否定式字段统一为 `*Enabled` 肯定式命名；920 添加触觉反馈设置项；FingerOverlayState 合并到 GestureFeedbackState.fingerIndicator；910 包名确认（7 文档 9 问题）
- **第3轮**：最终确认，修复 930 代码组织表中 FingerOverlayState 残留引用（1 文档 1 问题）

### 6. 字典数据库方案

确认现阶段采用 Room 框架作为字典数据库方案，后续视情况决定是否更换。

### 7. README.md 审查与修改

将 README.md 纳入审查范围，发现并修复：

- `IMEIntent` → `ImeIntent`（命名规范）
- `IMEService` 保持平台惯例命名（:app 模块 InputMethodService 子类）
- 分层架构图未体现三层库归属 → 更新为标注 :app/:ime-ui/:ime-engine 归属
- 缺少三层库架构描述 → 新增「三层库架构」小节，补充 ImeConfig 统一配置说明
- 标题去掉「Kotlin 重构版」标注 → `筷字输入法 Android 客户端`

### 8. 测试文档体系

用户要求补充测试文档目录，制定测试用例编写规范和模版，核心要求：

- **只能由软件验收员编写和更新单元测试**
- **验证测试用例是否真正通过**

基于此建立了完整的测试文档体系，包括索引、编写规范和模版。

---

## 结论与决策

| # | 决策 | 说明 |
|---|------|------|
| 1 | 命名规范 | 不以 `Kuaizi` 作类名前缀；:ime-engine 公开 API 统一 `Ime` 前缀；:ime-ui 业务语义命名（XxxPanel/XxxView）；:app 平台惯例命名（IMEService/XxxScreen）；发布包名 `Kuaizi_IME` 例外 |
| 2 | 包名规范 | 各模块顶级包名 `org.crazydan.studio.app.ime.kuaizi`；:app 不加子模块名 |
| 3 | ImeConfig 统一配置 | 合并 ImeEngineConfig 和 Config；EngineConfig+UiConfig 嵌套隔离；运行时修改始终优先于持久化，直到重启重新初始化 |
| 4 | :app 模块定位 | 库使用特例；直接用 KeyboardViewModel 不继承；配置通过 ImeConfig 交互 |
| 5 | 字典数据库 | 现阶段采用 Room 框架 |
| 6 | 全文档审查完成 | 3 轮审查共修改 8 个文档、修复 15 个问题，文档间一致性已确认 |
| 7 | 测试文档体系 | 建立测试目录和编写规范；验收员专属编写权限；测试必须真正通过 |

---

## 待定事项

| # | 事项 | 说明 |
|---|------|------|
| 1 | GestureInputPanel 手势检测性能 | 需原型阶段验证，全屏透明层在高频滑行输入时的性能表现 |
| 2 | Compose 在 IME 中的性能 | 需原型阶段验证，Compose 在 InputMethodService 中的内存占用和渲染帧率 |
| 3 | 测试用例编写 | 9 个测试文档（100/160/200/300/500/600/800/900/930）待验收员编写具体用例 |
