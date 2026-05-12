# 设计文档索引

本目录存放筷字输入法 v4 版本的架构和功能设计文档。文档以三位数字开头并根据内容命名。文档内容始终实时根据情况整理以保持最新，无用和过时的文档需及时清理。

---

## 文档列表

| 编号 | 文档 | 简述 |
|------|------|------|
| 000 | [架构总览](000-architecture-overview.md) | v4 整体架构设计，包括分层、消息流、技术选型和与 Java 版本的架构对比 |
| 100 | [键盘状态机设计](100-keyboard-state-machine.md) | 键盘状态机重构，从继承链到组合模式，Sealed class 状态定义和转换规则 |
| 200 | [输入列表重构设计](200-input-list-redesign.md) | InputList 重构，不可变数据模型、游标管理、线程安全改进 |
| 300 | [字典系统重构设计](300-dict-system-redesign.md) | 字典系统重构，协程化、Room/SQLDelight 选型、升级策略 |
| 400 | [UI Compose 迁移设计](400-ui-compose-migration.md) | UI 层从 View 到 Compose 的迁移方案、性能验证、IME 桥接 |
| 500 | [配置与设置系统设计](500-config-and-settings.md) | 配置系统重构，DataStore 替代 SharedPreferences、主题系统 |
| 600 | [剪贴板与收藏系统设计](600-clipboard-and-favorites.md) | 剪贴板检测、收藏管理的重构，类型安全的数据模型 |
| 700 | [X-Pad 重构设计](700-xpad-redesign.md) | X-Pad 六边形键盘的重构，Compose Canvas 绘制、手势交互 |
| 800 | [用户数据导入导出设计](800-user-data-import-export.md) | v4 新增功能：用户数据的导入与导出，JSON 格式备份文件，替换/合并导入策略 |

---

## 待定事项

> 当前无待定事项。设计过程中发现的待定事项将在各文档中明确标注。
