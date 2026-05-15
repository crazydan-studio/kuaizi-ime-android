# :app 模块设计文档

本目录包含 `:app` 模块的所有设计文档，涵盖配置管理、日志系统、UI 测试方案和用户数据导入导出等核心设计。

## 文档索引

| 文档 | 说明 |
|------|------|
| [010-配置管理](010-config.md) | ConfigRepository（DataStore 实现）、ImeConfig 运行时与持久化配置管理、ThemeType 处理 |
| [020-应用日志系统](020-logging.md) | ImeLog/ImeLogger、LogWriter/LogcatWriter/FileLogWriter、CrashInterceptor、LogStorage、LogViewerScreen/LogExportScreen |
| [030-UI 测试方案](030-ui-testing.md) | 构建配置（release 自动移除）、UITestOverlay、5 种测试工具、UITestToolbar、Compose 编译器报告、截图对比测试 |
| [040-用户数据导入导出](040-user-data.md) | UserDataService、UserBackup/BackupData 模型、ExportResult/ImportResult、ImportStrategy、UI 设计、权限与安全 |
