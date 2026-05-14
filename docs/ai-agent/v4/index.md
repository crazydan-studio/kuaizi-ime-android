# v4 版本文档索引

本文档为筷字输入法 v4（Kotlin 重构版）的文档索引和核心工作说明。v4 版本基于 Kotlin 2.3.20 和 Jetpack Compose BOM 2026.04.01 对 Java 版本进行全面重构。

---

## 版本信息

| 属性 | 值 |
|------|-----|
| 版本号 | 4.0.0 |
| versionCode | 400 |
| 语言 | Kotlin 2.3.20 |
| UI 框架 | Jetpack Compose (BOM 2026.04.01) |
| 最低 SDK | 25 |
| 目标 SDK | 35 |
| 基于 | Java v3.1.1 (versionCode 311) |

---

## 核心工作说明

v4 版本的核心目标是将整个项目从 Java 迁移到 Kotlin，同时：

1. **保持功能完整性**：Java 版本已实现的每一项功能都必须在 Kotlin 版本中保留，可以改进但不能缺失或遗漏
2. **消除历史包袱**：利用 Kotlin 特性大胆移除或简化原设计和实现中的不规范、不合理、冗余之处（包括移除 alpha 变体等不再需要的构建类型）
3. **现代化架构**：采用 MVI 架构、协程/Flow、Jetpack Compose 等现代 Android 开发技术
4. **代码规范**：严格遵循「显式优于隐式」、「任其崩溃」等原则
5. **新增功能**：支持导入/导出用户数据到文件，便于换机迁移和数据备份

### 重构原则

| 原则 | 说明 |
|------|------|
| 显式优于隐式 | 所有公开 API 必须有显式声明，状态变更必须有迹可循 |
| 任其崩溃 | 遇到不可恢复的错误立即失败，不静默降级 |
| 不可变优先 | 数据默认不可变，状态变更通过创建新实例 |
| 单向数据流 | 状态自上而下流动，事件自下而上传递 |
| 组合优于继承 | 优先使用组合和委托，避免深层继承链 |

---

## 文档目录

### 设计文档 — `design/`

存放架构和功能设计文档。文档以三位数字开头并根据内容命名。内容始终保持最新，无用和过时的文档需及时清理。

| 文档 | 说明 |
|------|------|
| [design/index.md](design/index.md) | 设计文档索引 |
| [design/000-architecture-overview.md](design/000-architecture-overview.md) | 架构总览 |
| [design/010-naming-conventions.md](design/010-naming-conventions.md) | 命名规范 |
| [design/100-keyboard-state-machine.md](design/100-keyboard-state-machine.md) | 键盘状态机设计 |
| [design/150-input-key-panel-separation.md](design/150-input-key-panel-separation.md) | 输入面板、按键面板与反馈面板三层分离设计 |
| [design/160-ime-engine-library.md](design/160-ime-engine-library.md) | IME 引擎库与 UI 库设计 |
| [design/200-input-list-redesign.md](design/200-input-list-redesign.md) | 输入列表重构设计 |
| [design/300-dict-system-redesign.md](design/300-dict-system-redesign.md) | 字典系统重构设计 |
| [design/400-ui-compose-migration.md](design/400-ui-compose-migration.md) | UI Compose 迁移设计 |
| [design/500-config-and-settings.md](design/500-config-and-settings.md) | 配置与设置系统设计 |
| [design/600-clipboard-and-favorites.md](design/600-clipboard-and-favorites.md) | 剪贴板与收藏系统设计 |
| [design/700-xpad-redesign.md](design/700-xpad-redesign.md) | X-Pad 重构设计 |
| [design/800-user-data-import-export.md](design/800-user-data-import-export.md) | 用户数据导入导出设计（v4 新增） |
| [design/900-app-logging.md](design/900-app-logging.md) | 应用日志系统设计（v4 新增） |
| [design/910-ui-testing.md](design/910-ui-testing.md) | UI 测试方案设计（v4 新增） |
| [design/920-config-ui-improvement.md](design/920-config-ui-improvement.md) | 配置界面改进设计（v4 新增） |
| [design/930-input-action-programming.md](design/930-input-action-programming.md) | 输入动作程序化设计（v4 新增） |

### 计划文档 — `plans/`

存放开发计划文档。文档以三位数字开头并根据内容命名。内容始终保持最新，无用和过时的文档需及时清理。

| 文档 | 说明 | 状态 |
|------|------|------|
| [plans/index.md](plans/index.md) | 计划文档索引 | — |
| [plans/000-plan-authoring-and-execution-guide.md](plans/000-plan-authoring-and-execution-guide.md) | 计划编制与执行指南 | — |

### 测试文档 — `tests/`

存放单元测试用例文档，由软件验收员编写和维护。每个测试文档对应一个模块或功能领域，与设计文档编号一致。测试用例必须真正执行通过。

| 文档 | 说明 | 状态 |
|------|------|------|
| [tests/index.md](tests/index.md) | 测试文档索引、核心原则、角色权限、文件组织 | — |
| [tests/000-test-writing-guide.md](tests/000-test-writing-guide.md) | 测试用例编写规范和模版 | — |

### 讨论记录 — `discussions/`

存放与用户的讨论记录。文档为历史记录，不对已有内容做更新。

| 文档 | 说明 |
|------|------|
| [discussions/index.md](discussions/index.md) | 讨论记录索引 |

### 开发日志 — `logs/`

存放开发日志，按年/月-日组织，以 `{年}/{月}-{日}.md` 形式命名（月和日采用两位数字）。文档为历史记录，不对已有内容做更新。

| 文档 | 说明 |
|------|------|
| [logs/index.md](logs/index.md) | 开发日志索引 |

### 缺陷修复 — `bugs/`

存放缺陷修复记录。文档为历史记录，不对已有内容做更新。

| 文档 | 说明 |
|------|------|
| [bugs/index.md](bugs/index.md) | 缺陷修复索引 |

---

## 本目录文件组织规范

### 文件组织

- 每个子目录（design/、plans/、tests/、discussions/、logs/、bugs/）都有独立的 `index.md` 作为该类文档的索引
- 设计文档和计划文档使用三位数字前缀排序，编号规则：
  - `000-099`：基础/框架类文档
  - `100-199`：键盘核心类文档
  - `200-299`：输入系统类文档
  - `300-399`：数据层类文档
  - `400-499`：UI 层类文档
  - `500-599`：配置/设置类文档
  - `600-699`：辅助功能类文档
  - `700-799`：X-Pad 专项类文档
  - `800-899`：数据管理类文档

### 文档作用

| 文档类型 | 作用 | 阅读场景 |
|----------|------|----------|
| 设计文档 | 描述架构、功能设计、技术选型和实现方案 | 开发前理解设计意图、开发中核对实现是否符合设计 |
| 测试文档 | 定义单元测试用例、记录测试执行结果 | 验收员编写测试用例、执行测试验证功能是否满足设计 |
| 计划文档 | 定义开发任务、执行步骤和验收标准 | 开发中跟踪进度、验收时核对完成情况 |
| 讨论记录 | 记录与用户的讨论内容和结论 | 需要回顾决策背景时查阅 |
| 开发日志 | 记录开发过程中的关键事件和决策 | 需要了解开发历史时查阅 |
| 缺陷修复 | 记录缺陷的发现、分析和修复过程 | 需要了解缺陷历史时查阅 |

### 更新规范

- **设计文档、测试文档和计划文档**：始终实时根据情况整理以保持最新，无用和过时的文档需及时清理
- **测试文档**：只有软件验收员才能编写和更新测试用例，开发员不得修改测试用例（只能提交缺陷反馈）
- **对于待定事项**：需明确说明不确定的地方有哪些，不同的方案有什么不同和优劣，不同的选择会产生什么样的影响等
- **讨论记录、开发日志、缺陷修复**：为历史记录，不对已有内容做更新
