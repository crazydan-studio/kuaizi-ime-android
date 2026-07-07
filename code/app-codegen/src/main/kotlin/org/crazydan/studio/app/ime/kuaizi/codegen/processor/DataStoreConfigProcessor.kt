/*
 * 筷字输入法 - 高效编辑需要又好又快的输入法
 * Copyright (C) 2026 Crazydan Studio <https://studio.crazydan.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.
 * If not, see <https://www.gnu.org/licenses/lgpl-3.0.en.html#license-text>.
 */

package org.crazydan.studio.app.ime.kuaizi.codegen.processor

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.Nullability
import com.squareup.kotlinpoet.BOOLEAN
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FLOAT
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.INT
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.LONG
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.STRING
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.ksp.writeTo
import org.crazydan.studio.app.ime.kuaizi.engine.DataStoreConfig
import org.crazydan.studio.app.ime.kuaizi.engine.DataStoreConfigKey
import kotlin.reflect.KClass

/**
 * DataStore 配置的代码生成处理器。
 *
 * 解析被 [DataStoreConfig] 注解标注的配置类，为每个属性生成：
 * - Preferences.Key 常量（根据属性类型选择对应的 key 函数）
 * - read 方法：从 Preferences 中读取所有属性并构造配置类实例
 * - write 方法：将配置类的所有属性写入 MutablePreferences
 *
 * 通过 KSP（Kotlin Symbol Processing）在编译期执行代码生成，
 * 生成代码存放在同包名下的 `datastore` 子包，Object 名格式为 `{ClassName}DataStore`。
 */
class DataStoreConfigProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
) : SymbolProcessor {
    private var processed: Boolean = false

    @OptIn(KspExperimental::class)
    override fun process(resolver: Resolver): List<KSAnnotated> {
        // Note：防止多轮处理相同文件而导致报异常 kotlin.io.FileAlreadyExistsException
        // https://github.com/google/ksp/issues/755#issuecomment-988038218
        if (processed) return emptyList()
        processed = true

        // 查找所有标注了 @DataStoreConfig 的类
        // Note：ksp 不支持 resolver.getSymbolsWithAnnotation 查找依赖模块内标注的 class，
        // 只能按包做过滤
        val symbols =
            resolver.getDeclarationsFromPackage("org.crazydan.studio.app.ime.kuaizi.engine")
                .filterIsInstance<KSClassDeclaration>()
                .flatMap { it.withAllNestedClasses() }
                // Note：KSClassDeclaration#isAnnotationPresent 存在 bug，
                // 因为 KSAnnotation#annotationType.resolve().declaration.qualifiedName 始终为 null
                .filter {
                    it.annotations
                        .firstOrNull { a -> a.isType(DataStoreConfig::class) } != null
                }

        val deferred = mutableListOf<KSAnnotated>()
        symbols.forEach { declaration ->
            try {
                // 为每个配置类生成 DataStore Keys
                generateDataStoreKeys(declaration)
            } catch (e: Exception) {
                logger.error("Failed to generate config DataStore: $e", declaration)

                // 延迟处理，待下次轮次重试
                deferred.add(declaration)
            }
        }

        return deferred
    }

    /**
     * 为指定的配置类生成 DataStore Keys 代码。
     *
     * 生成的代码包含：
     * - Object {ClassName}DataStore
     * - 每个属性的 Preferences.Key 常量
     * - readConfig(prefs: Preferences): {ClassName} 方法
     * - writeConfig(prefs: MutablePreferences, old: {ClassName}, new: {ClassName}) 方法
     */
    private fun generateDataStoreKeys(declaration: KSClassDeclaration) {
        val packageName = declaration.packageName.asString()
        val className = declaration.simpleName.asString()

        // 解析 @DataStoreConfig 注解的 prefix 参数
        val annotation =
            declaration.annotations.first { it.isType(DataStoreConfig::class) }
        val prefix =
            annotation.arguments
                .firstOrNull { it.name?.asString() == "prefix" }
                ?.value as? String ?: className.replaceFirstChar { it.lowercase() }

        val objectName =
            if (className.endsWith("Config")) className
            else className + "Config"
        val objectTypeName = "${objectName}DataStore"

        // 构建文件规格
        val fileSpec =
            FileSpec.builder("$packageName.datastore", objectTypeName)
                .addFileComment("自动生成，请勿手动修改")
                .addImport("kotlin", "lazy")
                .addImport("kotlin", "run")
                .addType(
                    TypeSpec.objectBuilder(objectTypeName)
                        .also {
                            buildObject(it, declaration, prefix)
                        }
                        .build()
                )
                .build()

        // 写入生成的文件
        fileSpec.writeTo(codeGenerator, aggregating = true)
        logger.info("Generated config DataStore for $className with prefix $prefix")
    }

    private fun buildObject(
        builder: TypeSpec.Builder,
        declaration: KSClassDeclaration,
        confPrefix: String
    ) {
        val objectType = getClassName(declaration)
        val readMethodName = "readConfig"
        val writeMethodName = "writeConfig"

        // 类型引用
        val preferencePackageName = "androidx.datastore.preferences.core"
        val preferencesKeyType = ClassName(preferencePackageName, "Preferences", "Key")
        val preferencesType = ClassName(preferencePackageName, "Preferences")
        val mutablePreferencesType = ClassName(preferencePackageName, "MutablePreferences")

        // 利用 Kotlin data class 的构造默认值
        //  private val defaults: EngineConfig by lazy { EngineConfig() }
        builder.addProperty(
            PropertySpec.builder("defaults", objectType, KModifier.PRIVATE)
                .addKdoc("用于获取配置对象各属性的默认值")
                .delegate("lazy { %T() }", objectType)
                .build()
        )

        // 获取配置类的所有属性
        val properties = declaration.getAllProperties().toList()
        // 生成每个属性的 Key 常量
        properties.forEach { prop ->
            val propName = prop.simpleName.asString()
            // 解析 @DataStoreKey 注解的 name 参数
            val keyName =
                prop.annotations
                    .firstOrNull { it.isType(DataStoreConfigKey::class) }
                    ?.arguments?.firstOrNull { it.name?.asString() == "name" }
                    ?.value as? String ?: propName.toSnakeCase()

            // private val logStoragePathKey: Preferences.Key<String> = stringPreferencesKey("engine_log_storage_path")
            val typeName = prop.type.resolve().declaration.simpleName.asString()
            builder.addProperty(
                PropertySpec.builder(
                    "${propName}Key",
                    preferencesKeyType.parameterizedBy(getKeyType(typeName)),
                    KModifier.PRIVATE,
                )
                    .initializer(
                        "%M(%S)",
                        MemberName(preferencePackageName, getKeyFunction(typeName)),
                        "${confPrefix}_${keyName}",
                    )
                    .build()
            )
        }

        // 生成 read 方法：从 Preferences 读取所有属性
        builder.addFunction(
            FunSpec.builder(readMethodName)
                .addParameter("prefs", preferencesType)
                .returns(objectType)
                .apply {
                    val args = mutableListOf<Any>()

                    val assignments = mutableListOf<String>()
                    properties.forEach { prop ->
                        val propName = prop.simpleName.asString()
                        val patch = if (isEnum(prop)) "?.let { %T.valueOf(it) }" else ""

                        if (isEnum(prop)) {
                            val enumType = getClassName(prop.type.resolve().declaration)
                            args.add(enumType)
                        }
                        assignments.add("  $propName = prefs[${propName}Key]$patch ?: defaults.$propName")
                    }

                    addStatement(
                        "return %T(\n${assignments.joinToString(",\n")}\n)",
                        objectType,
                        *args.toTypedArray()
                    )
                }
                .build()
        )

        // 生成 write 方法：将配置类属性写入 MutablePreferences
        builder.addFunction(
            FunSpec.builder(writeMethodName)
                .addKdoc("增量写入配置：仅将变化的字段写入 Preferences。")
                .addParameter("prefs", mutablePreferencesType)
                .addParameter("old", objectType)
                .addParameter("new", objectType)
                .apply {
                    beginControlFlow("if (old == new)").apply {
                        addStatement("return")
                    }.endControlFlow()

                    properties.forEach { prop ->
                        val propName = prop.simpleName.asString()
                        val keyName = "${propName}Key"
                        val patch = if (isEnum(prop)) ".name" else ""

                        beginControlFlow("if (old.%L != new.%L)", propName, propName).apply {
                            if (isNullable(prop)) {
                                beginControlFlow("if (new.%L == null)", propName).apply {
                                    addStatement("prefs.remove(%L)", keyName)
                                }.endControlFlow()
                                beginControlFlow("else").apply {
                                    addStatement("prefs[%L] = new.%L!!$patch", keyName, propName)
                                }.endControlFlow()
                            } else {
                                addStatement("prefs[%L] = new.%L$patch", keyName, propName)
                            }
                        }.endControlFlow()
                    }
                }
                .build()
        )
    }

    /** 根据 Kotlin 类型选择对应的 Preferences key 类型 */
    private fun getKeyType(typeName: String): ClassName =
        when (typeName) {
            "Boolean" -> BOOLEAN
            "String" -> STRING
            "Int" -> INT
            "Long" -> LONG
            "Float" -> FLOAT
            else -> STRING
        }

    /** 根据 Kotlin 类型选择对应的 Preferences key 函数 */
    private fun getKeyFunction(typeName: String): String =
        when (typeName) {
            "Boolean" -> "booleanPreferencesKey"
            "String" -> "stringPreferencesKey"
            "Int" -> "intPreferencesKey"
            "Long" -> "longPreferencesKey"
            "Float" -> "floatPreferencesKey"
            else -> "stringPreferencesKey"
        }

    private fun getClassName(declaration: KSDeclaration): ClassName {
        val packageName = declaration.packageName.asString()
        val className = declaration.simpleName.asString()

        val parentClassName = declaration.parentDeclaration?.simpleName?.asString()

        return parentClassName?.let {
            ClassName(packageName, it, className)
        } ?: ClassName(packageName, className)
    }

    private fun isNullable(prop: KSPropertyDeclaration): Boolean =
        prop.type.resolve().nullability == Nullability.NULLABLE

    private fun isEnum(prop: KSPropertyDeclaration): Boolean {
        val declaration = prop.type.resolve().declaration

        return declaration is KSClassDeclaration && declaration.classKind == ClassKind.ENUM_CLASS
    }

    /**
     * 将驼峰命名转换为蛇形命名。
     *
     * 例如：`inputPredictionEnabled` -> `input_prediction_enabled`
     */
    private fun String.toSnakeCase(): String {
        return replace(Regex("([a-z])([A-Z])")) { "${it.groupValues[1]}_${it.groupValues[2]}" }
            .replace(Regex("([A-Z]{2,})([a-z])")) { "${it.groupValues[1]}_${it.groupValues[2]}" }
            .lowercase()
    }

    // 递归扩展函数：获取当前类及其所有嵌套/内部类
    private fun KSClassDeclaration.withAllNestedClasses(): List<KSClassDeclaration> {
        val result = mutableListOf(this)
        // getDeclarations() 可以获取当前类内部定义的函数、属性以及嵌套类
        val nestedClasses = this.declarations.filterIsInstance<KSClassDeclaration>()

        for (nested in nestedClasses) {
            result.addAll(nested.withAllNestedClasses())
        }
        return result
    }

    private fun KSAnnotation.isType(type: KClass<*>): Boolean =
        this.shortName.asString() == type.simpleName
}
