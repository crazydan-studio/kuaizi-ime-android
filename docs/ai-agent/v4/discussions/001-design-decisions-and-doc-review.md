# 001 — v4 命名与架构方案探讨

## 基本信息

| 字段 | 值 |
|------|-----|
| 讨论日期 | 2026-05-12 |
| 参与者 | 用户、AI Designer |
| 主题 | 命名前缀的分层策略、配置统一方案、:app 模块定位、UiConfig 字段命名风格 |

---

## 讨论内容

### 1. 类名前缀该不该按模块区分？

**问题背景**：Java 版本中类名以 `IME` 为前缀（如 `IMEConfig`、`IMEService`），也有以 `Kuaizi` 为前缀的（如 `KuaiziIMETheme`），命名风格不统一，无法从类名识别其所在模块或职责。

**探讨过程**：

一种思路是全部统一为 `Ime` 前缀，让整个项目有一致的命名空间。但这带来一个问题：`:ime-ui` 和 `:app` 模块的类也加 `Ime` 前缀时，类名会变得冗余——`ImeKeyboardView`、`ImeSettingsScreen`——前缀反而成了噪音，因为这些模块的类本身就处于 IME 上下文中，不需要再通过前缀声明领域。

另一种思路是只有 `:ime-engine` 作为独立库使用 `Ime` 前缀。理由是引擎库会被第三方应用独立引入，类名需要命名空间标识来避免冲突并表明来源；而 `:ime-ui` 和 `:app` 的类本身就在 IME 领域内，命名应贴近其自身职能——UI 组件就叫 `KeyboardPanel`，设置页面就叫 `SettingsScreen`，类名即文档。

还有一个边界问题：`IMEService` 是 Android `InputMethodService` 的子类，`IME` 全大写是 Android 平台对 IME 服务的惯例命名（如系统 API 中的 `InputMethodService`、`EditorInfo.IME_ACTION_XXX`），不应改为 `ImeService`，否则与平台惯例不一致。这说明即使确立了前缀规则，平台惯例外也需留出空间。

**结论**：engine 公开 API 用 `Ime` 前缀作为命名空间，ui/app 模块不加 `Ime` 前缀而用职能命名，平台惯例命名除外。

### 2. 各模块包名要不要加子模块标识？

**问题背景**：三层库架构下，`:app`、`:ime-engine`、`:ime-ui` 三个模块的包名如何组织？是统一根包还是各自加子包？

**探讨过程**：

如果 `:app` 模块使用 `org.crazydan.studio.app.ime.kuaizi.app`，会显得冗余——包名中已经包含 `app`，再加 `.app` 子包没有增加信息量。更关键的是，`:`app` 模块的地位与第三方应用相同（只是库的一个消费者），不应该因为它是"官方应用"就在包名上有特殊标识。

`:ime-engine` 和 `:ime-ui` 作为库模块，其内部可以有自己的子包（如 `.engine.dict`、`.ui.panel`），因为它们的内部结构需要被第三方开发者理解和导航，子包有助于划清职责边界。

**结论**：所有模块共享顶级包名，`:app` 不加子模块名。

### 3. ImeEngineConfig 和应用 Config 该合并还是独立？

**问题背景**：引擎库有自己的配置 `ImeEngineConfig`，应用侧也有运行时配置 `Config`，两者存在重叠（如左右手模式既是引擎关心的也是 UI 关心的），且配置修改的优先级不清晰。

**探讨过程**：

方案 A 是保持分离——引擎配置归引擎，UI 配置归 UI，应用自己维护一份合并视图。问题在于"合并视图"谁来维护？如果 :app 模块来做，那每次新增配置项都要在 :app 里同步，而且运行时修改和持久化配置的优先级关系散落在各处，难以保证一致性。

方案 B 是合并为统一的 `ImeConfig`，通过嵌套 `EngineConfig` + `UiConfig` 明确隔离。这样做的好处是：配置只有一份，优先级规则集中在一处；第三方应用只需面对一个配置对象，按需读取 `engine` 或 `ui` 部分；运行时覆盖机制也可以统一管理。

关于运行时修改的优先级，核心矛盾是：用户在键盘 UI 上临时切换了左右手模式，这个改动应该立即生效，但不应该写入持久化配置（因为可能只是临时需要）。如果应用重启，临时修改应该失效，从持久化配置重新初始化。这引出了 `runtimeOverrides` 的设计——记录哪些字段被运行时覆盖了，持久化同步时跳过这些字段，重启后自然失效。

**结论**：合并为 `ImeConfig`，嵌套隔离引擎和 UI 配置，运行时修改优先于持久化配置直到重启。

### 4. :app 模块该不该继承 KeyboardViewModel？

**问题背景**：`:app` 模块需要配置持久化（DataStore）和 InputConnection 桥接，这些是平台特定职责。一种做法是让 :app 继承 `KeyboardViewModel`，在子类中加入平台逻辑。

**探讨过程**：

继承方案的问题在于：如果 :app 继承了 `KeyboardViewModel`，第三方应用也需要做同样的事才能获得完整功能，这违背了"库应该开箱即用"的原则。而且 ViewModel 的继承会引入配置耦合——:app 通过 ViewModel 访问配置，而不是通过 `ImeConfig`，两条配置通路会导致状态不一致。

更好的做法是把 :app 视为库的普通消费者：直接使用 `KeyboardViewModel`，不继承不扩展；配置持久化由独立的 `ConfigRepository` 处理，通过 `ImeConfig` 与引擎交互；InputConnection 桥接由 `InputConnectionBridge` 独立处理。这样 ViewModel 保持纯粹的状态管理职责，平台特定逻辑各自独立。

**结论**：:app 直接使用 KeyboardViewModel，不继承；平台职责由独立组件承担。

### 5. UiConfig 字段命名：否定式还是肯定式？

**问题背景**：UiConfig 中的开关字段最初使用否定式命名，如 `disableKeyAnimation`、`disableGestureSlippingTrail`，默认值为 `false`（即不禁用 = 启用）。

**探讨过程**：

否定式命名在阅读时需要双重否定思维：`disableKeyAnimation = false` 表示"不禁用按键动画" = "启用按键动画"。当多个否定式字段组合在一起时，理解成本很高。尤其在 DataStore 的 preferences key 中，`DISABLE_KEY_ANIMATION` 对应的 boolean 值为 `false` 才表示启用，容易写反逻辑。

肯定式命名如 `keyAnimationEnabled = true` 直接表达"启用按键动画"，语义直观。所有开关字段统一为 `*Enabled` 后缀，默认值全部为肯定方向（`true` 表示启用，`false` 表示禁用），消除了双重否定。

不过有一个边界情况：`enableCandidateVariantFirst` 原本就是肯定式前缀，但风格与其他字段不一致（其他是 `xxxEnabled` 后缀，这个是 `enableXxx` 前缀）。统一为 `candidateVariantFirstEnabled` 后缀，虽然稍长，但与整体命名模式一致。

**结论**：统一使用 `*Enabled` 肯定式后缀命名，默认值与语义方向一致。

### 6. FingerOverlayState 该不该独立存在？

**问题背景**：文档 930（输入动作程序化）中设计了 `FingerOverlayState` 管理手指指示器动画，而文档 150（面板分离）中 `GestureFeedbackState` 也包含手指指示器相关字段。两者职责有重叠。

**探讨过程**：

独立存在的优点是关注点分离——练习功能的手指动画状态与真实手势的反馈状态互不干扰。但问题在于：`FingerOverlay` Composable 需要同时响应真实手势和练习动画两种来源的手指指示器状态，如果状态源有两个，就需要在 UI 层做合并逻辑，增加了复杂度。

合并方案是让 `GestureFeedbackState.fingerIndicator` 成为手指指示器的唯一状态源，无论是真实手势还是练习动画，都通过 `setFingerIndicator()` 写入同一个状态。`InputActionPlayer` 驱动练习动画时直接操作 `GestureFeedbackState`，与真实手势共享反馈基础设施，`FingerOverlay` 只需读取一个状态源。

合并后 `FingerOverlayState` 作为空类保留，仅作文档标记说明已合并，避免后续开发者误以为它是遗漏。

**结论**：合并为 `GestureFeedbackState.fingerIndicator` 单一状态源。

---
