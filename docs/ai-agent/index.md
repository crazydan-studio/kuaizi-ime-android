# AI Agent — 文档和工程组织结构说明

本文档面向参与项目开发的 AI Agent，说明项目的文档组织结构和工程目录结构，以便 Agent 正确理解和操作项目。

---

## 工程目录结构

```
kotlin/                              ← Kotlin 工作树根目录
├── code/                            ← Kotlin 工程源码
│   ├── app/                         ← 主应用模块
│   │   ├── build.gradle             ← 模块级构建配置
│   │   ├── proguard-rules.pro       ← ProGuard 规则
│   │   └── src/                     ← 源码目录
│   │       ├── main/                ← 主源集
│   │       ├── debug/               ← Debug 构建源集
│   │       └── androidTest/         ← 仪器测试
│   ├── build.gradle                 ← 根构建配置
│   ├── settings.gradle              ← 模块设置
│   ├── gradle.properties            ← Gradle 属性
│   ├── gradle/                      ← Gradle Wrapper
│   ├── gradlew / gradlew.bat        ← Gradle 执行脚本
│   └── ...
├── docs/                            ← 项目文档
│   ├── index.md                     ← 文档组织说明和主要内容索引
│   └── ai-agent/                    ← AI Agent 相关文档
│       ├── index.md                 ← 本文档
│       ├── skills/                  ← AI 技能库
│       └── v4/                      ← v4 版本开发文档
├── metadata/                        ← F-Droid 构建必须目录（只读，勿修改）
├── tools/                           ← 构建相关 Shell 脚本
├── LICENSE                          ← 开源许可证（LGPL 3.0）
└── README.md                        ← 产品介绍、文档索引、许可证要点
```

### 关键目录说明

| 目录 | 读写权限 | 说明 |
|------|----------|------|
| `code/` | 读写 | Kotlin 工程源码，所有开发工作在此进行 |
| `docs/` | 读写 | 项目文档，开发和设计过程中持续更新 |
| `metadata/` | **只读** | F-Droid 构建所必须目录，非特别说明始终保持不变 |
| `tools/` | 读写 | 构建脚本，可按需调整 |
| `LICENSE` | **只读** | 与 java 工作树保持一致，不得修改 |
| `README.md` | 读写 | 按重构后的版本调整内容 |

---

## 文档组织结构

### 层级关系

```
docs/ai-agent/
├── index.md                        ← 本文档：工程组织结构说明
├── skills/                         ← AI 技能库（跨版本通用）
│   ├── index.md                    ← 技能库索引
│   ├── kotlin-best-practices.md    ← Kotlin 最佳实践
│   ├── compose-best-practices.md   ← Jetpack Compose 最佳实践
│   └── code-conventions.md         ← 代码规范
└── v4/                             ← v4 主线版本开发文档
    ├── index.md                    ← 版本文档索引
    ├── design/                     ← 架构/功能设计文档
    ├── plans/                      ← 开发计划文档
    ├── discussions/                ← 讨论记录文档
    ├── logs/                       ← 开发日志
    └── bugs/                       ← 缺陷修复文档
```

### 文档类型与规范

| 文档类型 | 目录 | 更新策略 | 说明 |
|----------|------|----------|------|
| AI 技能库 | `skills/` | 持续更新 | 跨版本通用的最佳实践和规范 |
| 设计文档 | `v4/design/` | 持续更新 | 保持最新，清理过时内容 |
| 计划文档 | `v4/plans/` | 持续更新 | 保持最新，清理已完成/过时内容 |
| 讨论记录 | `v4/discussions/` | **只追加** | 历史记录，不对已有内容做更新 |
| 开发日志 | `v4/logs/` | **只追加** | 历史记录，不对已有内容做更新 |
| 缺陷修复 | `v4/bugs/` | **只追加** | 历史记录，不对已有内容做更新 |

### 文件命名规范

- `design/`、`plans/`、`discussions/` 目录下的文档以 **三位数字** 开头并根据内容确定文件名
  - 示例：`000-architecture-overview.md`、`100-keyboard-state-machine.md`
- `logs/` 目录下的文档按 **年月** 组织子目录，以 **日期** 命名
  - 示例：`2026/05/12.md`
- `bugs/` 目录下的文档以 **三位数字** 开头和缺陷简要描述命名
  - 示例：`001-input-list-concurrent-modification.md`

### 图片资源规范

- 各级文档所引用的图片均放在各自所在目录的 `images/` 子目录中
- 跨文档共享的图片放在最近的公共父目录的 `images/` 中
- 文档中引用图片时使用相对路径

---

## 版本目录说明

版本目录以 `v + 主版本号` 命名（如 `v4`、`v5`），每个版本目录包含：

| 子目录 | 说明 | 更新策略 |
|--------|------|----------|
| `design/` | 架构和功能设计文档 | 持续更新，及时清理过时内容 |
| `plans/` | 开发计划文档 | 持续更新，及时清理已完成/过时内容 |
| `discussions/` | 与用户的讨论记录 | 只追加，不修改已有内容 |
| `logs/` | 开发日志 | 只追加，不修改已有内容 |
| `bugs/` | 缺陷修复记录 | 只追加，不修改已有内容 |

版本目录中的文档是开发过程中需要反复阅读、审查和核对的核心资料。

---

## 参考工作树

| 工作树 | 分支 | 用途 |
|--------|------|------|
| `kotlin/` | `refactor-kotlin` | **工作目录**：Kotlin 版本的开发在此进行 |
| `java/` | `java-frozen` | **只读参考**：Java 版本代码，仅在需要时查阅 |

> **重要**：`java/` 工作树为只读，仅用于在需要时查阅 Java 版本的代码。不要修改 java 工作树中的任何文件。
