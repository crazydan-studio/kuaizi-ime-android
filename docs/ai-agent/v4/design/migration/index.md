# Java → v4 迁移对照文档

## 概述

本文档集将 kuaizi-ime-android 项目中散布在各设计文档里的 Java → v4 迁移/对比内容进行整合汇总，形成统一的迁移参考。每份对照表包含三列：**Java Class/Feature → v4 对应 → 变更说明**，确保迁移无遗漏。

## 文档结构

| 文档 | 范围 | 内容来源 |
|------|------|----------|
| [engine-mapping.md](engine-mapping.md) | `:ime-engine` 引擎库 | 000 §5、100 §2/§7、170 §7.1/§7.2、200 §2/§7、300 §2/§7、600 §2/§4、700 §2/§5 |
| [ui-mapping.md](ui-mapping.md) | `:ime-ui` UI 库 | 170 §7.3、150 §2、400 §2/§8、920 §2/§7 |
| [app-mapping.md](app-mapping.md) | `:app` 应用模块 | 170 §7.4、160 §2、500 §2/§5、010 §4 |

## 迁移背景

v4 版本对筷字输入法进行全面的 Kotlin 重构，核心改进方向：

- 从**命令式 MVP** 转向**声明式 MVI**（ImeIntent + StateFlow 替代三套消息体系）
- 从**深层继承链**转向**组合模式**（Keyboard sealed class 替代 BaseKeyboard 继承树）
- 从**手写异步**转向**协程**（Coroutine + Flow 替代 CompletableFuture + Handler）
- 从**View 系统**转向**Compose**（声明式 UI 替代自定义 View + RecyclerView）
- 从**SharedPreferences** 转向 **DataStore**（类型安全、异步、响应式）
- 从**手写 SQLite** 转向 **Room**（编译期检查、类型安全 DAO）

这些变更的历史原因详见各模块文档的「Java 版本分析」章节。
