# Java → v4 迁移对照文档

## 概述

本文档集将 kuaizi-ime-android 项目中散布在各设计文档里的 Java → v4 迁移 / 对比内容进行整合汇总，形成统一的迁移参考。每份对照表包含三列：**Java Class/Feature → v4 对应 → 变更说明**，确保迁移无遗漏。

## 文档索引

| 文档 | 范围 | 说明 |
|------|------|------|
| [010-引擎模块迁移](010-engine-mapping.md) | `:ime-engine` 引擎库 | 核心引擎、消息体系、状态机、输入列表、字典系统、X-Pad、剪贴板与收藏的 Java→v4 对照 |
| [020-UI 模块迁移](020-ui-mapping.md) | `:ime-ui` UI 库 | View 系统、输入面板、配置界面的 Java→v4 对照 |
| [030-应用模块迁移](030-app-mapping.md) | `:app` 应用模块 | 应用层、配置系统、库模式、命名变更的 Java→v4 对照 |

## 迁移背景

v4 版本对筷字输入法进行全面的 Kotlin 重构，核心改进方向：

- 从**命令式 MVP** 转向**声明式 MVI**（ImeIntent + StateFlow 替代三套消息体系）
- 从**深层继承链**转向**组合模式**（Keyboard sealed class 替代 BaseKeyboard 继承树）
- 从**手写异步**转向**协程**（Coroutine + Flow 替代 CompletableFuture + Handler）
- 从**View 系统**转向**Compose**（声明式 UI 替代自定义 View + RecyclerView）
- 从**SharedPreferences** 转向 **DataStore**（类型安全、异步、响应式）
- 从**手写 SQLite** 转向 **Room**（编译期检查、类型安全 DAO）
