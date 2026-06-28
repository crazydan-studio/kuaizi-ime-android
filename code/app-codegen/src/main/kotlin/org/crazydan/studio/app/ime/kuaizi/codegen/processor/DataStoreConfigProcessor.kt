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

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
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

/**
 * DataStore 配置的代码生成处理器。
 *
 * 解析被 [DataStoreConfig] 注解标注的配置类，为每个属性生成：
 * - Preferences.Key 常量（根据属性类型选择对应的 key 函数）
 * - read 方法：从 Preferences 中读取所有属性并构造配置类实例
 * - write 方法：将配置类的所有属性写入 MutablePreferences
 *
 * 通过 KSP（Kotlin Symbol Processing）在编译期执行代码生成，
 * 生成代码存放在同包名下，Object 名格式为 `{ClassName}DataStoreKeys`。
 */
class DataStoreConfigProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
) : SymbolProcessor {

    override fun process(resolver: Resolver): List<KSAnnotated> {
        // 查找所有标注了 @DataStoreConfig 的类
        val symbols = resolver.getSymbolsWithAnnotation("DataStoreConfig")
        val deferred = mutableListOf<KSAnnotated>()

        symbols.filterIsInstance<KSClassDeclaration>().forEach { declaration ->
            try {
                // 为每个配置类生成 DataStore Keys
                generateDataStoreKeys(declaration)
            } catch (e: Exception) {
                logger.error("Failed to generate DataStore keys: ${e.message}", declaration)
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
     * - Object {ClassName}DataStoreKeys
     * - 每个属性的 Preferences.Key 常量
     * - read{className}(prefs: Preferences): ClassName 方法
     * - write{className}(prefs: MutablePreferences, config: ClassName) 方法
     */
    private fun generateDataStoreKeys(declaration: KSClassDeclaration) {
        val packageName = declaration.packageName.asString()
        val className = declaration.simpleName.asString()
        // 解析 @DataStoreConfig 注解的 prefix 参数
        val annotation = declaration.annotations
            .first { it.shortName.asString() == "DataStoreConfig" }
        val prefix = annotation.arguments
            .firstOrNull { it.name?.asString() == "prefix" }
            ?.value as? String ?: className.replaceFirstChar { it.lowercase() }

        // 获取配置类的所有属性
        val properties = declaration.getAllProperties()
            .toList()

        val objectName = "${className}DataStoreKeys"
        val readMethodName = "read${className}"
        val writeMethodName = "write${className}"

        // 类型引用
        val preferencesKeyType = ClassName("androidx.datastore.preferences.core", "Preferences", "Key")
        val preferencesType = ClassName("androidx.datastore.preferences.core", "Preferences")
        val mutablePreferencesType = ClassName("androidx.datastore.preferences.core", "MutablePreferences")

        // 构建文件规格
        val fileSpec = FileSpec.builder(packageName, objectName)
            .addFileComment("自动生成，请勿手动修改")
            .addType(
                TypeSpec.objectBuilder(objectName)
                    .apply {
                        // 生成每个属性的 Key 常量
                        properties.forEach { prop ->
                            val propName = prop.simpleName.asString()
                            // 解析 @DataStoreKey 注解的 name 参数
                            val keyName = prop.annotations
                                .firstOrNull { it.shortName.asString() == "DataStoreKey" }
                                ?.arguments?.firstOrNull { it.name?.asString() == "name" }
                                ?.value as? String ?: propName.toSnakeCase()
                            // 根据 Kotlin 类型选择对应的 Preferences key 函数
                            val typeName = prop.type.resolve().declaration.simpleName.asString()
                            val keyFunction = when (typeName) {
                                "Boolean" -> "booleanPreferencesKey"
                                "String" -> "stringPreferencesKey"
                                "Int" -> "intPreferencesKey"
                                "Long" -> "longPreferencesKey"
                                "Float" -> "floatPreferencesKey"
                                else -> "stringPreferencesKey"
                            }
                            addProperty(
                                PropertySpec.builder(
                                    "${propName}Key",
                                    preferencesKeyType.parameterizedBy(
                                        when (typeName) {
                                            "Boolean" -> BOOLEAN
                                            "String" -> STRING
                                            "Int" -> INT
                                            "Long" -> LONG
                                            "Float" -> FLOAT
                                            else -> STRING
                                        }
                                    ),
                                    KModifier.VALUE,
                                )
                                    .initializer(
                                        "%M(%S)",
                                        MemberName("androidx.datastore.preferences.core", keyFunction),
                                        "${prefix}_${keyName}",
                                    )
                                    .build()
                            )
                        }

                        // 生成 read 方法：从 Preferences 读取所有属性
                        val readArgs = properties.joinToString(",\n") { prop ->
                            val propName = prop.simpleName.asString()
                            "    $propName = prefs[${propName}Key] ${defaultValue(prop)}"
                        }
                        addFunction(
                            FunSpec.builder(readMethodName)
                                .addParameter("prefs", preferencesType)
                                .returns(ClassName(packageName, className))
                                .addStatement("return %T(\n%L\n)", ClassName(packageName, className), readArgs)
                                .build()
                        )

                        // 生成 write 方法：将配置类属性写入 MutablePreferences
                        addFunction(
                            FunSpec.builder(writeMethodName)
                                .addParameter("prefs", mutablePreferencesType)
                                .addParameter("config", ClassName(packageName, className))
                                .beginControlFlow("config.run")
                                .apply {
                                    properties.forEach { prop ->
                                        val propName = prop.simpleName.asString()
                                        addStatement("prefs[%L] = %L", "${propName}Key", propName)
                                    }
                                }
                                .endControlFlow()
                                .build()
                        )
                    }
                    .build()
            )
            .build()

        // 写入生成的文件
        fileSpec.writeTo(codeGenerator, aggregating = false)
        logger.info("Generated DataStore keys for $className with prefix $prefix")
    }

    /**
     * 计算属性的默认值表达式。
     *
     * 对于可空类型返回 `?: null`，
     * 对于基本类型返回各自的标准默认值。
     */
    private fun defaultValue(prop: KSPropertyDeclaration): String {
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

    /**
     * 将驼峰命名转换为蛇形命名。
     *
     * 例如：`inputPredictionEnabled` -> `input_prediction_enabled`
     */
    private fun String.toSnakeCase(): String {
        return replace(Regex("([a-z])([A-Z])")) { "${it.groupValues[1]}_${it.groupValues[2]}" }
            .replace(Regex("([A-Z])([A-Z][a-z])")) { "${it.groupValues[1]}_${it.groupValues[2]}" }
            .lowercase()
    }
}
