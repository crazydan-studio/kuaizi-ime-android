# 设计文档索引

本目录存放筷字输入法 v4 版本的架构和功能设计文档。文档以三位数字开头并根据内容命名。文档内容始终实时根据情况整理以保持最新，无用和过时的文档需及时清理。

---

## 文档列表

| 编号 | 文档 | 简述 |
|------|------|------|
| 000 | [架构总览](000-architecture-overview.md) | v4 整体架构设计，包括分层、数据流、核心设计决策和与 Java 版本的架构对比 |
| 010 | [命名规范](010-naming-conventions.md) | 三层模块命名、UI 组件后缀、KeyGridPanel 子类命名、引擎 API 命名、已更名对照 |
| 100 | [键盘状态机设计](100-keyboard-state-machine.md) | 键盘状态机重构，从继承链到组合模式，Sealed class 状态定义和转换规则 |
| 150 | [输入面板、按键面板与反馈面板三层分离设计](150-input-key-panel-separation.md) | 输入手势、按键渲染与手势反馈三层分离：输入面板（透明手势层）接收手势识别为 InputGesture，按键面板纯展示（持续性状态渲染），反馈面板独立透明绘制（临时性手势反馈，支持多实例）；叠加布局模式；后续支持全屏输入模式 |
| 160 | [IME 引擎库与 UI 库设计](160-ime-engine-library.md) | 三层库架构：引擎库 `:ime-engine`（纯 Kotlin，可独立使用）+ UI 库 `:ime-ui`（Compose 缺省 UI，对第三方应用开放）+ 应用模块 `:app`（IME 服务 + 设置）。引擎与 UI 完全分离，统一配置 `ImeConfig`（含 `EngineConfig` 引擎配置和 `UiConfig` UI 配置的明确隔离），DictProvider 接口支持替换内置 SQLite 实现，Feature 标记按需禁用收藏/剪贴板等功能；UI 库提供 EditorField、KeyboardPanel、InputHostView 等即插即用组件，第三方应用可直接使用缺省 UI 或自行替换；`:app` 直接使用 UI 库的 `KeyboardViewModel`，不继承也不扩展 |
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

---

## 待定事项

1. ~~**ImeConfig 与 Config 的职责边界**~~：✅ 已解决。`ImeEngineConfig` 重命名为 `ImeConfig`，同时包含引擎配置（`EngineConfig`）和 UI 配置（`UiConfig`），二者明确隔离。原 `:app` 模块的 `Config` data class 已合并到 `ImeConfig.UiConfig`，不再需要独立的 `Config` 和两套配置的同步逻辑
2. ~~**ImeState.config 的实际类型**~~：✅ 已解决。`ImeState.config` 统一为 `ImeConfig`，包含完整的引擎配置和 UI 配置。UI 侧通过 `config.ui` 访问 UI 配置，引擎侧通过 `config.engine` 访问引擎配置
3. ~~**字典数据库方案**~~：✅ 已决定。现阶段直接采用 Room 框架，后续视情况决定是否更换方案（文档 300 第 3.2 节）
4. ~~**KeyboardViewModel 在 :app 模块中的继承方式**~~：✅ 已解决。`:app` 模块直接使用 UI 库的 `KeyboardViewModel`，不继承也不扩展。配置持久化和 InputConnection 桥接等平台特定职责由 `:app` 中的独立组件承担（文档 160 第 8.4 节）
5. **GestureInputPanel 手势检测的性能**：GestureInputPanel 作为全屏透明层拦截所有触摸事件，在高频滑行输入时的性能表现需要在原型阶段验证（文档 000 第 7 节）
6. **Compose 在 IME 环境中的性能**：Compose 在 InputMethodService 中的内存占用和渲染帧率需要在原型阶段验证，必要时降级为 View（文档 400 第 7 节）
7. ~~**ImeConfig 运行时覆盖的精确语义**~~：✅ 已解决。`ImeConfig` 在运行时的修改始终优先于应用侧配置，直到应用重启。重启时，`ImeConfig` 根据持久化配置进行初始化。`ImeConfig.runtimeOverrides` 记录被运行时覆盖的字段，持久化同步时跳过这些字段（文档 160 第 4.2 节、文档 500 第 3.1 节）
8. ~~**:app 模块的包结构**~~：✅ 已决定。:app 模块不加子模块名，直接使用顶级包名 `org.crazydan.studio.app.ime.kuaizi`（文档 160 第 3.1 节）
9. ~~**StandardKeyboard 命名**~~：✅ 已决定。StandardKeyboard 更名为 StandardKeyGridPanel，与三层分离设计中的 KeyGridPanel 命名体系保持一致，同时移除 onKeyPress 参数（手势由 GestureInputPanel 独立处理）
10. ~~**FingerOverlay 与 GestureFeedbackState 的手指指示器职责边界**~~：✅ 已解决。`FingerOverlayState`（文档 930）已合并到 `GestureFeedbackState.fingerIndicator`（文档 150），`InputActionPlayer` 直接通过 `feedbackState.setFingerIndicator()` 驱动手指指示器，`GestureFeedbackPanel` 在配置了 `FeedbackElementType.FingerIndicator` 时自动渲染。不再存在独立的 `FingerOverlayState`（文档 150 第 4.3 节、文档 930 第 4.2 节）
11. ~~**ConfigBackupEntry 与 ImeConfig 的映射**~~：✅ 已解决。文档 800 的 `ConfigBackupEntry` 已重构为嵌套结构，与 `ImeConfig` 的 `EngineConfig` 和 `UiConfig` 对齐（文档 800 第 3.2 节）
12. ~~**910 UI 测试工具的包名一致性**~~：✅ 已确认。文档 910 的 Lint 规则使用正确包名 `org.crazydan.studio.app.ime.kuaizi.uitest.`（无 `.app.` 子模块名），实现时只需遵循文档中的包名约定即可
13. ~~**ImeConfig.UiConfig 字段命名风格统一**~~：✅ 已解决。所有字段统一使用肯定式命名（`*Enabled` 后缀），移除了 `disable*` 和 `enable*` 前缀。具体变更：`disableKeyAnimation`→`keyAnimationEnabled`、`disableKeyPopupTips`→`keyPopupTipsEnabled`、`disableGestureSlippingTrail`→`gestureSlippingTrailEnabled`、`disableClipPopupTips`→`clipPopupTipsEnabled`、`disableUserInputData`→`userInputDataEnabled`、`disableCandidatesPagingAudio`→`candidatesPagingAudioEnabled`、`enableCandidateVariantFirst`→`candidateVariantFirstEnabled`、`enableLatinUsePinyinKeysInXPad`→`latinUsePinyinKeysInXPadEnabled`。默认值同步调整为肯定式语义（如 `keyAnimationEnabled = true` 替代 `disableKeyAnimation = false`）（文档 160、500、800、920）
