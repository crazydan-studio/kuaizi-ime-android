# 测试文档索引

本目录存放筷字输入法 v4 版本的单元测试文档。每个测试文档对应一个模块或功能领域的测试用例集合，由软件验收员负责编写、维护和验证。

---

## 核心原则

1. **只有软件验收员才能编写和更新单元测试用例**：软件开发员负责实现功能代码，软件验收员负责编写测试用例、验证实现是否符合设计、确保测试用例真正通过。这种职责分离确保测试从独立视角检验代码质量，避免开发员「自产自销」导致测试流于形式。

2. **测试用例必须真正通过**：每个测试用例的执行结果必须可验证、可复现。如果测试用例无法通过，说明功能实现存在缺陷或测试设计本身存在问题，必须修正后才能标记为通过。纸面上「写完」测试用例不等于完成，运行通过才算数。

3. **测试驱动验收**：计划文档（`plans/`）中的验收标准应关联到具体的测试用例。验收员通过执行测试用例来判定功能是否达到验收标准，而非凭主观判断。

---

## 文档列表

| 编号 | 文档 | 测试范围 | 状态 |
|------|------|----------|------|
| 100 | [键盘状态机测试](100-keyboard-state-machine-tests.md) | 键盘状态转换、Intent 处理、状态机边界条件（对应设计文档 100） | 📋待编写 |
| 160 | [引擎库 API 测试](160-ime-engine-api-tests.md) | ImeEngine 公开 API、ImeConfig、ImeOutput、DictProvider 接口（对应设计文档 160） | 📋待编写 |
| 200 | [输入列表测试](200-input-list-tests.md) | InputList 不可变操作、游标管理、撤销/重做、线程安全（对应设计文档 200） | 📋待编写 |
| 300 | [字典系统测试](300-dict-system-tests.md) | Room DAO、DictProvider 接口实现、字典查询、升级迁移（对应设计文档 300） | 📋待编写 |
| 500 | [配置系统测试](500-config-system-tests.md) | ConfigRepository、ImeConfig 运行时优先语义、DataStore 持久化（对应设计文档 500） | 📋待编写 |
| 600 | [剪贴板与收藏测试](600-clipboard-favorites-tests.md) | ClipboardProvider、FavoritesRepository、Feature 裁剪（对应设计文档 600） | 📋待编写 |
| 800 | [用户数据导入导出测试](800-user-data-import-export-tests.md) | UserDataService、JSON 序列化/反序列化、替换/合并策略、SAF 文件操作（对应设计文档 800） | 📋待编写 |
| 900 | [日志系统测试](900-app-logging-tests.md) | AppLog 分级、持久化、崩溃拦截、等级配置（对应设计文档 900） | 📋待编写 |
| 930 | [输入动作程序化测试](930-input-action-programming-tests.md) | InputActionPlayer、脚本编译、回放、InputKey 位置解析（对应设计文档 930） | 📋待编写 |

---

## 测试文档编写规范

详见 [000-test-writing-guide.md](000-test-writing-guide.md)。

---

## 测试状态说明

| 状态 | 说明 |
|------|------|
| 📋待编写 | 测试文档尚未创建，测试用例尚未编写 |
| 🔄编写中 | 验收员正在编写测试用例 |
| ✅已通过 | 所有测试用例已编写完成，且在当前代码上全部执行通过 |
| ❌未通过 | 部分测试用例执行失败，需开发员修正代码或验收员修正测试 |
| ⏸️暂停 | 测试编写或执行暂停，需说明原因 |

---

## 角色与权限

| 角色 | 可执行的操作 | 不可执行的操作 |
|------|-------------|---------------|
| **软件验收员** | 编写和更新测试用例文档、执行测试、记录测试结果、标记测试状态 | 修改功能实现代码 |
| **软件开发员** | 根据测试失败结果修正功能代码 | 修改测试用例文档（只能提交缺陷反馈） |
| **设计师** | 更新测试文档索引、调整测试范围 | 修改测试用例的具体内容和预期结果 |

---

## 测试文件组织

单元测试代码与测试文档紧密对应，测试代码按模块和功能组织在源码目录中：

```
code/
├── ime-engine/src/test/                    ← :ime-engine 库单元测试
│   └── org/crazydan/studio/app/ime/kuaizi/engine/
│       ├── api/                            ← 引擎公开 API 测试（对应文档 160）
│       │   ├── ImeEngineTest.kt
│       │   ├── ImeConfigTest.kt
│       │   └── ImeOutputTest.kt
│       ├── domain/                         ← 领域逻辑测试
│       │   ├── KeyboardStateMachineTest.kt ← 状态机测试（对应文档 100）
│       │   ├── InputListTest.kt            ← 输入列表测试（对应文档 200）
│       │   └── CandidatePagerTest.kt
│       └── dict/                           ← 字典接口测试（对应文档 300）
│           └── InMemoryDictProviderTest.kt
│
├── ime-ui/src/test/                        ← :ime-ui 库单元测试
│   └── org/crazydan/studio/app/ime/kuaizi/ui/
│       └── theme/                          ← 主题系统测试
│           └── ImeThemeTest.kt
│
└── app/src/test/                           ← :app 模块单元测试
    └── org/crazydan/studio/app/ime/kuaizi/
        ├── config/                         ← 配置系统测试（对应文档 500）
        │   └── ConfigRepositoryTest.kt
        ├── clipboard/                      ← 剪贴板测试（对应文档 600）
        ├── favorite/                       ← 收藏测试（对应文档 600）
        ├── userdata/                       ← 用户数据导入导出测试（对应文档 800）
        └── logging/                        ← 日志系统测试（对应文档 900）
```

> **注意**：截图对比测试（Paparazzi）和 UI 测试工具相关代码不属于单元测试范畴，其组织方式见设计文档 910。
