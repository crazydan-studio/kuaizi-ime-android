# 设计文档索引

本目录存放筷字输入法 v4 版本的架构和功能设计文档。文档以三位数字开头并根据内容命名。文档内容始终实时根据情况整理以保持最新，无用和过时的文档需及时清理。

---

## 文档列表

| 编号 | 文档 | 简述 |
|------|------|------|
| 000 | [架构总览](000-architecture-overview.md) | v4 整体架构设计，包括分层、数据流、核心设计决策和与 Java 版本的架构对比 |
| 010 | [命名规范](010-naming-conventions.md) | 三层模块命名、UI 组件后缀、KeyGridPanel 子类命名、引擎 API 命名、已更名对照 |
| 100 | [键盘状态机设计](100-keyboard-state-machine.md) | 键盘状态机重构，从继承链到组合模式，Sealed class 状态定义和转换规则 |
| 150 | [输入面板、按键面板与反馈面板三层分离设计](150-input-key-panel-separation.md) | 输入手势、按键渲染与手势反馈三层分离：输入面板（透明手势层）接收手势识别为 InputGesture，按键面板纯展示（持续性状态渲染），反馈面板独立透明绘制（临时性手势反馈，支持多实例）；叠加布局模式（KeyboardPanel）和全屏输入模式（KeyboardScreen） |
| 160 | [三层模块划分：引擎库 `:ime-engine` + UI 库 `:ime-ui` + 应用模块 `:app`](160-three-layer-module-division.md) | 三层库架构：引擎库 `:ime-engine`（纯 Kotlin，可独立使用）+ UI 库 `:ime-ui`（Compose 缺省 UI，对第三方应用开放）+ 应用模块 `:app`（IME 服务 + 设置）。引擎与 UI 完全分离，统一配置 `ImeConfig`（含 `EngineConfig` 引擎配置和 `UiConfig` UI 配置的明确隔离），ImeIntent/ImeOutput/ImeState 完整定义，ImeDictProvider 接口支持替换内置 SQLite 实现，Feature 标记按需禁用收藏/剪贴板等功能；UI 库提供 KeyboardPanel（叠加模式）、KeyboardScreen（全屏模式）、EditTextBridge、ImeOutputBridge 等即插即用组件，第三方应用可直接使用缺省 UI 或自行替换；`:app` 直接使用 UI 库的 `KeyboardViewModel`，不继承也不扩展 |
| 200 | [输入列表重构设计](200-input-list-redesign.md) | InputList 重构，不可变数据模型、游标管理、线程安全改进 |
| 300 | [字典系统重构设计](300-dict-system-redesign.md) | 字典系统重构，协程化、Room 数据库、升级策略 |
| 400 | [UI Compose 迁移设计](400-ui-compose-migration.md) | UI 层从 View 到 Compose 的迁移方案、性能验证、IME 桥接 |
| 500 | [配置与设置系统设计](500-config-and-settings.md) | 配置系统重构，DataStore 替代 SharedPreferences、主题系统 |
| 600 | [剪贴板与收藏系统设计](600-clipboard-and-favorites.md) | 剪贴板检测、收藏管理的重构，类型安全的数据模型 |
| 700 | [X-Pad 重构设计](700-xpad-redesign.md) | X-Pad 六边形键盘的重构，Compose Canvas 绘制、手势交互 |
| 800 | [用户数据导入导出设计](800-user-data-import-export.md) | v4 新增功能：用户数据的导入与导出，JSON 格式备份文件，替换/合并导入策略 |
| 900 | [应用日志系统设计](900-app-logging.md) | 完整的应用内置日志系统：分级日志、崩溃拦截、文件持久化、日志查看与导出界面、可配置等级和存储路径 |
| 910 | [UI 测试方案设计](910-ui-testing.md) | 应用内置 UI 测试工具：布局边界可视化、组件信息查看、颜色拾取、栅格参考线、重组追踪，Release 构建自动移除 |
| 920 | [配置界面改进设计](920-config-ui-improvement.md) | 设置页面重构：场景化分组、肯定式命名、即时预览、条件显示、搜索、快捷切换弹窗、渐进式披露、保留「友情赞助」捐赠入口 |
| 930 | [输入动作程序化设计](930-input-action-programming.md) | 输入动作脚本化与动画模拟：坐标无关设计、Release 可用的输入练习演示、字符序列→逻辑动作编译、回放时动态解析按键位置、滑行/点击/X-Pad 动画、操作录制（仅 debug）、自动化测试驱动 |

