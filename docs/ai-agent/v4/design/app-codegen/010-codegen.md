# 代码生成设计（KSP）

`:app-codegen` 模块通过 KSP（Kotlin Symbol Processing）在编译期自动生成 `EngineConfig` 和 `UiConfig` 的 DataStore 持久化读写代码，消除手动维护所有 DataStore key 的样板代码。

---

## 1. 设计目标

`ConfigDataStore` 需要为 `EngineConfig` 和 `UiConfig` 的每个字段在 DataStore 中定义对应的 `Preferences.Key` 泛型参数，并编写 read/write 方法。这些字段包括 22+ 项配置：

| 配置类 | 字段数量 | 示例 |
|--------|----------|------|
| EngineConfig | 8 | `inputPredictionEnabled`, `logLevel`, `logStoragePath` ... |
| UiConfig | 10+ | `keyboardThemeType`, `audioFeedbackEnabled`, `hapticFeedbackEnabled` ... |

手动维护这些 key 定义和序列化/反序列化逻辑的工作量较大，且配置字段新增或变更时容易遗漏同步更新。通过 KSP 自动生成，将编译期的类型安全检查和代码生成结合在一起，消除维护成本。

---

## 2. 架构概览

```
┌────────────────────────────────────────────────────────────┐
│                    编译期（KSP）                             │
│                                                             │
│  @DataStoreConfig                                           │
│  data class EngineConfig(...)  ──→  KSP Processor           │
│  data class UiConfig(...)                                  │
│                                      │                      │
│                                      ▼                      │
│                              EngineConfigDataStoreKeys      │
│                              UiConfigDataStoreKeys          │
│                              (自动生成)                      │
└────────────────────────────────────────────────────────────┘
         │
         ▼  ：
┌────────────────────────────────────────────────────────────┐
│                    运行期（:app）                            │
│                                                             │
│  ConfigDataStore                                            │
│    ├── .config: Flow<ImeConfig>                             │
│    │     └── EngineConfigDataStoreKeys.readEngineConfig()   │
│    │     └── UiConfigDataStoreKeys.readUiConfig()           │
│    └── .updateConfig { }                                   │
│          └── EngineConfigDataStoreKeys.writeEngineConfig()  │
│          └── UiConfigDataStoreKeys.writeUiConfig()          │
└────────────────────────────────────────────────────────────┘
```

---

## 3. 注解设计

### 3.1 @DataStoreConfig

标记一个 data class 需要生成 DataStore 持久化代码。

```kotlin
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class DataStoreConfig(
    val prefix: String = "",
)
```

**参数说明**：
- `prefix`：DataStore key 的前缀，为空时使用类名自动推导

### 3.2 @DataStoreKey

可选注解，自定义字段的 DataStore key 名称。

```kotlin
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.SOURCE)
annotation class DataStoreKey(
    val name: String = "",
)
```

**参数说明**：
- `name`：自定义 key 名称。未指定时，使用字段名的 snake_case 形式

---

## 4. 使用示例

### 4.1 注解配置类

```kotlin
@DataStoreConfig(prefix = "engine")
data class EngineConfig(
    val inputPredictionEnabled: Boolean = true,
    val userDataPersistEnabled: Boolean = true,
    val favoriteInputEnabled: Boolean = true,
    val favoriteClipEnabled: Boolean = true,
    val favoriteSyncToUserDictEnabled: Boolean = true,
    val candidateVariantFirstEnabled: Boolean = false,
    val logLevel: String = "WARN",
    val logStoragePath: String? = null,
)

@DataStoreConfig(prefix = "ui")
data class UiConfig(
    val keyboardInputMode: String = "Pinyin",
    val keyboardHandMode: String = "Right",
    val keyboardThemeType: String = "FollowSystem",
    val audioFeedbackEnabled: Boolean = true,
    val hapticFeedbackEnabled: Boolean = true,
    val keyAnimationEnabled: Boolean = true,
    val candidatesPagingAudioEnabled: Boolean = true,
    val keyPopupTipsEnabled: Boolean = true,
    val gestureSlippingTrailEnabled: Boolean = true,
    val clipPopupTipsEnabled: Boolean = true,
)
```

### 4.2 自定义 Key 名称（可选）

```kotlin
data class UiConfig(
    @DataStoreKey(name = "keyboard_hand_mode")
    val keyboardHandMode: String = "Right",
    // ...
)
```

---

## 5. 处理器设计

### 5.1 SymbolProcessorProvider

```kotlin
class DataStoreConfigProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return DataStoreConfigProcessor(
            codeGenerator = environment.codeGenerator,
            logger = environment.logger,
        )
    }
}
```

### 5.2 SymbolProcessor

```kotlin
class DataStoreConfigProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
) : SymbolProcessor {

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val symbols = resolver.getSymbolsWithAnnotation("DataStoreConfig")
        val deferred = mutableListOf<KSAnnotated>()

        symbols.filterIsInstance<KSClassDeclaration>().forEach { declaration ->
            try {
                generateDataStoreKeys(declaration)
            } catch (e: Exception) {
                logger.error("生成 DataStore keys 失败: ${e.message}", declaration)
                deferred.add(declaration)
            }
        }

        return deferred
    }

    private fun generateDataStoreKeys(declaration: KSClassDeclaration) {
        val packageName = declaration.packageName.asString()
        val className = declaration.simpleName.asString()
        val annotation = declaration.annotations
            .first { it.shortName.asString() == "DataStoreConfig" }
        val prefix = annotation.arguments
            .firstOrNull { it.name?.asString() == "prefix" }
            ?.value as? String ?: className.toSnakeCase()

        val properties = declaration.getAllProperties()
            .filter { !it.isAnnotationPresent("DataStoreKey") || true }
            .toList()

        val generatedCode = buildGeneratedCode(
            packageName = packageName,
            className = className,
            prefix = prefix,
            properties = properties,
        )

        val fileSpec = FileSpec.builder(packageName, "${className}DataStoreKeys")
            .addFileComment("自动生成，请勿手动修改")
            .addImport("androidx.datastore.preferences.core", "*")
            .addCode(generatedCode)
            .build()

        fileSpec.writeTo(codeGenerator, aggregating = false)
    }

    private fun buildGeneratedCode(
        packageName: String,
        className: String,
        prefix: String,
        properties: List<KSPropertyDeclaration>,
    ): CodeBlock {
        val objectName = "${className}DataStoreKeys"
        val readMethodName = "read${className}"
        val writeMethodName = "write${className}"

        return CodeBlock.builder()
            .add("object %L {\n", objectName)
            .apply {
                // Key 定义
                properties.forEach { prop ->
                    val propName = prop.simpleName.asString()
                    val keyName = prop.annotations
                        .firstOrNull { it.shortName.asString() == "DataStoreKey" }
                        ?.arguments?.firstOrNull { it.name?.asString() == "name" }
                        ?.value as? String ?: propName.toSnakeCase()
                    val typeName = prop.type.resolve().declaration.simpleName.asString()

                    add(
                        "    val %L = %L(\"%L_%L\")\n",
                        propName.toKeyFieldName(),
                        keyFunction(typeName),
                        prefix,
                        keyName,
                    )
                }
                add("\n")

                // read 方法
                add(
                    """
                    fun %L(prefs: Preferences): %L = %L(
                """.trimIndent(), readMethodName, className, className
                )
                apply {
                    properties.forEachIndexed { index, prop ->
                        val propName = prop.simpleName.asString()
                        val typeName = prop.type.resolve().declaration.simpleName.asString()
                        val comma = if (index < properties.lastIndex) "," else ""
                        defaultValue(prop)?.let { default ->
                            add(
                                "        %L = prefs[%L] %L,\n",
                                propName,
                                propName.toKeyFieldName(),
                                default,
                            )
                        }
                    }
                }
                add("    )\n\n")

                // write 方法
                add(
                    """
                    fun %L(prefs: MutablePreferences, config: %L) {
                """.trimIndent(), writeMethodName, className
                )
                apply {
                    properties.forEach { prop ->
                        val propName = prop.simpleName.asString()
                        add(
                            "        prefs[%L] = config.%L\n",
                            propName.toKeyFieldName(),
                            propName,
                        )
                    }
                }
                add("    }\n")
            }
            .add("}\n")
            .build()
    }

    private fun keyFunction(typeName: String): String = when (typeName) {
        "Boolean" -> "booleanPreferencesKey"
        "String" -> "stringPreferencesKey"
        "Int" -> "intPreferencesKey"
        "Long" -> "longPreferencesKey"
        "Float" -> "floatPreferencesKey"
        else -> "stringPreferencesKey"
    }

    private fun defaultValue(prop: KSPropertyDeclaration): String? {
        val typeName = prop.type.resolve().declaration.simpleName.asString()
        return when {
            prop.type.resolve().nullability == Nullability.NULLABLE -> "?: null"
            typeName == "Boolean" -> "?: false"
            typeName == "String" -> "?: \"\""
            typeName == "Int" -> "?: 0"
            typeName == "Long" -> "?: 0L"
            typeName == "Float" -> "?: 0f"
            else -> "?: null"
        }
    }

    private fun String.toSnakeCase(): String {
        return replace(Regex("([a-z])([A-Z])")) { "${it.groupValues[1]}_${it.groupValues[2]}" }
            .lowercase()
    }

    private fun String.toKeyFieldName(): String {
        return "${this}Key"
    }
}
```

### 5.3 services 注册

```
# META-INF/services/com.google.devtools.ksp.processing.SymbolProcessorProvider
com.kuaizi.ime.codegen.processor.DataStoreConfigProcessorProvider
```

---

## 6. 生成的代码结构

对于如下配置类：

```kotlin
@DataStoreConfig(prefix = "engine")
data class EngineConfig(
    val inputPredictionEnabled: Boolean = true,
    val userDataPersistEnabled: Boolean = true,
    val logLevel: String = "WARN",
    val logStoragePath: String? = null,
    val favoriteInputEnabled: Boolean = true,
    val favoriteClipEnabled: Boolean = true,
    val favoriteSyncToUserDictEnabled: Boolean = true,
    val candidateVariantFirstEnabled: Boolean = false,
)
```

生成：

```kotlin
// 自动生成，请勿手动修改
package com.kuaizi.ime.config

import androidx.datastore.preferences.core.*

object EngineConfigDataStoreKeys {
    val inputPredictionEnabledKey = booleanPreferencesKey("engine_input_prediction_enabled")
    val userDataPersistEnabledKey = booleanPreferencesKey("engine_user_data_persist_enabled")
    val logLevelKey = stringPreferencesKey("engine_log_level")
    val logStoragePathKey = stringPreferencesKey("engine_log_storage_path")
    val favoriteInputEnabledKey = booleanPreferencesKey("engine_favorite_input_enabled")
    val favoriteClipEnabledKey = booleanPreferencesKey("engine_favorite_clip_enabled")
    val favoriteSyncToUserDictEnabledKey = booleanPreferencesKey("engine_favorite_sync_to_user_dict_enabled")
    val candidateVariantFirstEnabledKey = booleanPreferencesKey("engine_candidate_variant_first_enabled")

    fun readEngineConfig(prefs: Preferences): EngineConfig = EngineConfig(
        inputPredictionEnabled = prefs[inputPredictionEnabledKey] ?: false,
        userDataPersistEnabled = prefs[userDataPersistEnabledKey] ?: false,
        logLevel = prefs[logLevelKey] ?: "WARN",
        logStoragePath = prefs[logStoragePathKey] ?: null,
        favoriteInputEnabled = prefs[favoriteInputEnabledKey] ?: false,
        favoriteClipEnabled = prefs[favoriteClipEnabledKey] ?: false,
        favoriteSyncToUserDictEnabled = prefs[favoriteSyncToUserDictEnabledKey] ?: false,
        candidateVariantFirstEnabled = prefs[candidateVariantFirstEnabledKey] ?: false,
    )

    fun writeEngineConfig(prefs: MutablePreferences, config: EngineConfig) {
        prefs[inputPredictionEnabledKey] = config.inputPredictionEnabled
        prefs[userDataPersistEnabledKey] = config.userDataPersistEnabled
        prefs[logLevelKey] = config.logLevel
        prefs[logStoragePathKey] = config.logStoragePath
        prefs[favoriteInputEnabledKey] = config.favoriteInputEnabled
        prefs[favoriteClipEnabledKey] = config.favoriteClipEnabled
        prefs[favoriteSyncToUserDictEnabledKey] = config.favoriteSyncToUserDictEnabled
        prefs[candidateVariantFirstEnabledKey] = config.candidateVariantFirstEnabled
    }
}
```

对于 `UiConfig` 生成类似的 `UiConfigDataStoreKeys` 对象。

---

## 7. 与 ConfigDataStore 的集成

```kotlin
class ConfigDataStore(private val context: Context) {
    private val Context.dataStore by preferencesDataStore("ime_config")

    val config: Flow<ImeConfig> = context.dataStore.data.map { prefs ->
        ImeConfig(
            engine = EngineConfigDataStoreKeys.readEngineConfig(prefs),
            ui = UiConfigDataStoreKeys.readUiConfig(prefs),
        )
    }

    suspend fun updateConfig(transform: (ImeConfig) -> ImeConfig) {
        context.dataStore.edit { prefs ->
            val current = config.first()
            val new = transform(current)
            EngineConfigDataStoreKeys.writeEngineConfig(prefs, new.engine)
            UiConfigDataStoreKeys.writeUiConfig(prefs, new.ui)
        }
    }
}
```

---

## 8. Gradle 配置

```groovy
// :app-codegen/build.gradle
plugins {
    id("kotlin")
    id("com.google.devtools.ksp")
}

dependencies {
    implementation("com.google.devtools.ksp:symbol-processing-api:2.1.20-1.0.31")
    implementation("org.jetbrains.kotlin:kotlin-compiler-embeddable:2.1.20")
}

// 作为 KSP 插件发布
apply plugin: 'java-gradle-plugin'

gradlePlugin {
    plugins {
        register("appCodegen") {
            id = "com.kuaizi.ime.app-codegen"
            implementationClass = "com.kuaizi.ime.codegen.DataStoreConfigPlugin"
        }
    }
}
```

```groovy
// :app/build.gradle
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
    id("com.kuaizi.ime.app-codegen")
}

dependencies {
    implementation("androidx.datastore:datastore-preferences:1.1.3")
    ksp(project(":app-codegen"))
}
```

---

## 9. 类型支持矩阵

| Kotlin 类型 | DataStore Key 类型 | 序列化方式 |
|-------------|-------------------|-----------|
| Boolean | `booleanPreferencesKey` | 直接存储 |
| String | `stringPreferencesKey` | 直接存储 |
| Int | `intPreferencesKey` | 直接存储 |
| Long | `longPreferencesKey` | 直接存储 |
| Float | `floatPreferencesKey` | 直接存储 |
| String? | `stringPreferencesKey` | 可为 null |
| Double | — | 不支持，需转为 String |
| `Enum<T>` | `stringPreferencesKey` | 存储 name |

**Enum 支持**：枚举类型使用 `stringPreferencesKey`，存储 `enum.name`，读取时通过 `enumValueOf<T>()` 反序列化。处理器自动检测 `Enum` 超类型并生成对应的转换代码。

---

## 10. 增量编译支持

KSP 天然支持增量编译。处理器通过以下方式确保增量编译正确性：

- 注解 `@DataStoreConfig` 只在配置 data class 上声明，变更该文件时仅重新生成对应的 `DataStoreKeys` 文件
- 生成的代码文件之间无交叉依赖，一个配置类的变更不会影响其他配置类的生成代码
- `aggregating = false` 确保每个注解单独触发生成，而非聚合处理
