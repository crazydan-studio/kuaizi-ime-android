# :app-codegen 模块设计文档

本目录包含 `:app-codegen` 模块的所有设计文档。`:app-codegen` 模块通过 KSP（Kotlin Symbol Processing）在编译期自动生成配置数据类的 DataStore 持久化读写代码，消除手动维护大量 DataStore key 的样板代码。

## 文档索引

| 文档 | 说明 |
|------|------|
| [010-代码生成](010-codegen.md) | KSP 注解处理器设计、`@DataStoreConfig`/`@DataStoreKey` 注解定义、处理器实现、生成的代码结构、与 ConfigDataStore 的集成 |
