# Kotlin 最佳实践

本文档基于 Kotlin 2.3.20，说明 Kotlin 的最佳实践、推荐特性、避坑指南和不规范写法。

> **说明**：本文档前半部分为通用 Kotlin 最佳实践，第 5、8 节涉及本项目的 Java → Kotlin 迁移对比。通用示例不绑定本项目领域对象，项目迁移示例则直接使用本项目的类名和设计。

---

## 1. 语言特性优先级

### ✅ 优先使用的特性

| 特性 | 说明 | 适用场景 |
|------|------|----------|
| **Sealed class / interface** | 替代枚举和继承体系，表达有限类型集合 | 消息类型、按键类型、输入类型、键盘状态 |
| **Data class** | 自动生成 equals/hashCode/toString/copy | 所有不可变数据模型 |
| **Value class** | 零开销的类型包装，编译期类型安全 | 键码、配置键名、字典版本号等简单包装 |
| **Extension function** | 为已有类添加功能而不继承 | 工具方法、视图扩展 |
| **Scope function**（let/run/apply/also/with）| 作用域限定和链式调用 | 对象初始化、空安全操作 |
| **Coroutine + Flow** | 替代 CompletableFuture、Handler、RxJava | 异步操作、事件流、状态观察 |
| **DSL builder** | 类型安全的构建器模式 | 消息构建、键表定义、配置构建 |
| **Context parameters** | 替代隐式依赖传递和全局单例 | 字典访问、配置注入 |
| **Name-based destructuring** | 2.3.20 新特性，基于名称的解构声明 | 数据类解构，避免位置错乱 |
| **Inline function** | 消除 lambda 开销 | 高频调用的工具函数 |
| **Contract** | 协助编译器进行空值和类型推断 | require/check 后的智能类型转换 |

### ⚠️ 谨慎使用的特性

| 特性 | 原因 | 建议 |
|------|------|------|
| **Lateinit var** | 延迟初始化掩盖了初始化时序问题 | 优先使用构造参数注入或 `lazy {}`；仅在 Android 生命周期要求时使用 |
| **Companion object** | 容易沦为全局状态的容器 | 仅用于工厂方法和常量，不放可变状态 |
| **Operator overloading** | 过度使用降低可读性 | 仅在语义明确时使用（如 `plus` 用于集合合并），不用于业务逻辑 |
| **Delegated property** | `by lazy`、`by observable` 很有用，但自定义委托增加复杂度 | 优先使用标准委托 |
| **Actual/Expect** | 多平台代码会增加维护成本 | 本项目仅 Android，不需要 KMP |

### ❌ 禁止使用的特性

| 特性 | 原因 |
|------|------|
| **Global mutable state** | 违反「显式优于隐式」和「不可变优先」原则 |
| **Typealias 隐藏复杂类型** | typealias 应简化理解，而非掩盖复杂性 |
| **反射（Reflection）** | 运行时类型检查违反「显式优于隐式」，性能差 |
| **DslMarker 之外的标记注解滥用** | 增加理解成本，无明显收益 |
| **内联类的可变属性** | value class 不应有可变状态 |

---

## 2. Kotlin 2.3.20 新特性使用指南

### 2.1 基于名称的解构声明（Experimental）

启用方式：
```kotlin
// build.gradle.kts
kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xname-based-destructuring=name-mismatch")
    }
}
```

**推荐用法**：
- 在 `build.gradle.kts` 中启用 `name-mismatch` 模式，当位置解构的变量名与属性名不匹配时报警告
- 对新代码使用 `only-syntax` 模式的显式名称解构：`val (mail = email, name = username) = user`
- 数据类解构时始终核对变量名与属性名一致

**禁止用法**：
- 不要在解构声明中故意使用与属性名不同的变量名（位置解构）

### 2.2 Context Parameters

用于替代通过构造参数或单例传递依赖的模式：

```kotlin
// ✅ 推荐：context parameter 替代隐式依赖
context(DataSource, AppConfig)
fun queryItems(filter: String): List<Item> { ... }

// ❌ 避免：全局单例
object DataSourceHolder {
    val instance: DataSource get() = ...
}
```

**注意事项**：
- Kotlin 2.3.20 修改了带 context parameter 的重载决议规则——带 context parameter 的声明不再被认为比不带 context parameter 的更具体
- 避免仅通过 context parameter 有无来区分重载

### 2.3 `@Unmodifiable` 注解支持

从 Java 库返回的 `@Unmodifiable` 标注的集合，Kotlin 现在会警告将其赋值给 `MutableList`。这在 Kotlin 2.5.0 将变为错误。

**建议**：所有来自 Java 库的集合返回值一律按只读 `List` 处理。

---

## 3. 异步编程规范

### 协程 + Flow 替代方案

| Java 版本 | Kotlin 版本 | 说明 |
|-----------|-------------|------|
| `CompletableFuture` | `suspend` 函数 | 异步操作 |
| `Handler.postDelayed()` | `delay()` | 延迟执行 |
| `AsyncTask` | `CoroutineScope.launch` | 后台任务 |
| `BroadcastReceiver` | `Flow` / `callbackFlow` | 事件流 |
| `LiveData` | `StateFlow` / `SharedFlow` | 状态观察 |
| `SharedPreferences.OnChangeListener` | `callbackFlow` | 偏好变更流 |

### 协程使用规范

```kotlin
// ✅ 正确：使用结构化并发
class DataProcessor(
    private val scope: CoroutineScope,
    private val repository: Repository,
) {
    fun loadData(query: String) = scope.launch {
        val result = repository.queryAsync(query)
        _state.update { it.copy(items = result) }
    }
}

// ❌ 错误：使用 GlobalScope
fun loadData(query: String) = GlobalScope.launch { ... }

// ❌ 错误：取消不处理的 Job
val job = CoroutineScope(Dispatchers.IO).launch { ... }
// job 没有被管理，无法正确取消
```

### Flow 使用规范

```kotlin
// ✅ 正确：冷流 + StateFlow 共享
private val _uiState = MutableStateFlow(UiState())
val uiState: StateFlow<UiState> = _uiState.asStateFlow()

// ✅ 正确：callbackFlow 封装回调
fun preferenceChanges(): Flow<AppConfig> = callbackFlow {
    val listener = OnConfigChangedListener { config -> trySend(config) }
    configStore.addListener(listener)
    awaitClose { configStore.removeListener(listener) }
}
```

---

## 4. 数据模型设计规范

### Sealed class 替代枚举和继承

```kotlin
// ✅ 推荐：Sealed class 表达有限类型集合
sealed class UiEvent {
    abstract val id: String

    data class Click(
        override val id: String,
        val target: String,
    ) : UiEvent()

    data class Swipe(
        override val id: String,
        val direction: Direction,
        val distance: Float,
    ) : UiEvent()

    data class LongPress(
        override val id: String,
        val target: String,
    ) : UiEvent()
}

// ❌ 避免：深层继承 + 抽象类
abstract class Event { ... }
class ClickEvent : Event() { ... }
class SwipeEvent : Event() { ... }
```

### Data class + copy 模式

```kotlin
// ✅ 推荐：不可变数据 + copy 更新
data class ListState(
    val items: List<Item> = emptyList(),
    val selectedIndex: Int = 0,
    val pendingItem: Item? = null,
) {
    fun addItem(item: Item): ListState =
        copy(items = items + item, selectedIndex = items.size)
}

// ❌ 避免：可变数据类
data class MutableListState(
    var items: MutableList<Item> = mutableListOf(),
    var selectedIndex: Int = 0,
    var pendingItem: Item? = null,
)
```

### Value class 替代类型别名

```kotlin
// ✅ 推荐：Value class 提供编译期类型安全
@JvmInline
value class EntityId(val value: String)
@JvmInline
value class PreferenceKey(val name: String)

// ❌ 避免：Typealias 无类型安全
typealias EntityId = String
typealias PreferenceKey = String
```

---

## 5. Builder 模式迁移

Java 版本大量使用自定义 Builder 模式，Kotlin 中应使用 DSL 风格或 data class：

```kotlin
// ✅ 推荐：DSL 风格构建
fun config(block: ConfigBuilder.() -> Unit): Config =
    ConfigBuilder().apply(block).build()

// 使用
val cfg = config {
    name = "default"
    options = Options(maxRetries = 3)
}

// ✅ 推荐：Data class + 命名参数（简单场景）
data class Config(
    val name: String,
    val options: Options,
)

val cfg = Config(
    name = "default",
    options = Options(maxRetries = 3),
)
```

---

## 6. 集合操作规范

```kotlin
// ✅ 推荐：函数式链式操作
val names = items
    .filterIsInstance<ActiveItem>()
    .map { it.name }
    .distinct()

// ✅ 推荐：只读集合
fun getItems(): List<Item> = itemList.toList() // 防御性拷贝

// ❌ 避免：可变集合作为返回类型
fun getItems(): MutableList<Item> = itemList // 暴露内部可变状态
```

---

## 7. 空安全规范

```kotlin
// ✅ 推荐：require/check 断言
fun findItem(id: String): Item {
    require(id.isNotBlank()) { "ID must not be blank" }
    return repository[id] ?: error("No item found for id: $id")
}

// ✅ 推荐：Elvis 运算符提供默认值
val name = item.name ?: ""

// ✅ 推荐：!! 用于逻辑上不可能为 null 的场景（任其崩溃）
val selected = state.selectedItem!! // 如果逻辑上不可能为 null，用 !! 让它崩溃

// ❌ 避免：不必要的 ?.
val size = list?.size ?: 0 // 如果 list 逻辑上不可能为 null

// ❌ 禁止：吞掉空值
val result = try { compute() } catch (e: Exception) { null }
result?.let { process(it) } // 吞掉了异常信息
```

---

## 8. 与 Java 版本的关键差异

| Java 版本 | Kotlin 版本改进 |
|-----------|-----------------|
| `Immutable` 基类 + Builder 缓存 | `data class` + `copy()`，编译器自动生成 equals/hashCode |
| 枚举 `InputMsgType`（35+ 值） | `sealed class InputMsg` + 子类，可携带类型安全的数据 |
| 枚举 `CtrlKey.Type`（25+ 值） | `sealed class CtrlKey` + 子类 |
| `CompletableFuture` 异步 | `suspend` 函数 + `Flow` |
| `Handler` 调度 | `CoroutineDispatcher` + `withContext` |
| 自定义 `RecyclerView` + `LayoutManager` | Jetpack Compose `LazyRow`/`LazyVerticalGrid` |
| 手动 `SharedPreferences` 读写 | `DataStore` + `Flow` |
| 自定义 Builder 模式 | DSL builder 或 data class 命名参数 |
| 接口 + 匿名实现 | Lambda / SAM 转换 |
| `instanceof` + 强转 | `is` + 智能类型转换 |
| 可变状态 + 手动同步 | `StateFlow` + 不可变数据类 |
