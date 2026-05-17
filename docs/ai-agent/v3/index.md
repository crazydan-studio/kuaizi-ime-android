# v3 版本文档索引

本文档为筷字输入法 v3（Java 版本）的功能清单。v3 版本基于 Java 8 开发，采用传统 Android View 系统 + RecyclerView 实现键盘界面，目标 SDK 35，最低 SDK 25，版本号 3.1.1。

v3 版本是当前线上运行版本，本文档通过全面分析其源码，系统梳理已实现的所有功能，为 v4 重构提供完整的功能对照基准。

---

## 核心架构特征

| 特征 | 说明 |
|------|------|
| **语言** | Java 8 |
| **UI 框架** | Android View + RecyclerView + 自定义 LayoutManager |
| **键盘布局** | 六边形蜂窝网格（mixite 库），POINTY_TOP 方向 |
| **X-Pad** | FLAT_TOP 方向六边形分区，三区域环形布局 |
| **消息驱动** | InputMsg / UserKeyMsg / UserInputMsg 双向消息流 |
| **状态机** | 枚举类型 State.Type 定义键盘状态，链表记录状态历史 |
| **字典系统** | SQLite + HMM + Viterbi 算法实现拼音短语预测 |
| **主题** | Context-based 主题切换，支持亮色 / 暗色 / 跟随系统 |
| **构建** | debug / release / alpha 三种构建类型，VasDolly 渠道打包 |

---

## 文档目录

| 目录 | 说明 |
|------|------|
| `engine/` | 核心引擎：输入列表、键盘状态机、消息体系、X-Pad |
| `keyboard/` | 键盘类型：拼音、拉丁、数学、符号、Emoji、编辑器等 |
| `ui/` | UI 视图：主面板、按键视图、输入视图、X-Pad 视图、练习引导 |
| `dict/` | 字典系统：拼音字典、用户数据、HMM 算法、数据库升级 |
| `config/` | 配置系统：运行时配置、按键表配置、应用偏好设置 |
