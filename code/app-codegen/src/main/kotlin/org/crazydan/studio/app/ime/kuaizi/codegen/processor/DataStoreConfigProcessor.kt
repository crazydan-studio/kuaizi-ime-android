package org.crazydan.studio.app.ime.kuaizi.codegen.processor

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.Nullability
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ksp.writeTo
import com.squareup.kotlinpoet.memberName

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
                logger.error("Failed to generate DataStore keys: ${e.message}", declaration)
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
            ?.value as? String ?: className.replaceFirstChar { it.lowercase() }

        val properties = declaration.getAllProperties()
            .toList()

        val objectName = "${className}DataStoreKeys"
        val readMethodName = "read${className}"
        val writeMethodName = "write${className}"

        val preferencesKeyType = ClassName("androidx.datastore.preferences.core", "Preferences", "Key")
        val preferencesType = ClassName("androidx.datastore.preferences.core", "Preferences")
        val mutablePreferencesType = ClassName("androidx.datastore.preferences.core", "MutablePreferences")

        val fileSpec = FileSpec.builder(packageName, objectName)
            .addComment("自动生成，请勿手动修改")
            .addType(
                TypeSpec.objectBuilder(objectName)
                    .apply {
                        properties.forEach { prop ->
                            val propName = prop.simpleName.asString()
                            val keyName = prop.annotations
                                .firstOrNull { it.shortName.asString() == "DataStoreKey" }
                                ?.arguments?.firstOrNull { it.name?.asString() == "name" }
                                ?.value as? String ?: propName.toSnakeCase()
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
                                    KModifier.VAL,
                                )
                                    .initializer(
                                        "%M(%S)",
                                        memberName("androidx.datastore.preferences.core", keyFunction),
                                        "${prefix}_${keyName}",
                                    )
                                    .build()
                            )
                        }

                        val readArgs = properties.joinToString(",\n") { prop ->
                            val propName = prop.simpleName.asString()
                            "    ${propName} = prefs[${propName}Key] ${defaultValue(prop)}"
                        }
                        addFunction(
                            FunSpec.builder(readMethodName)
                                .addParameter("prefs", preferencesType)
                                .returns(ClassName(packageName, className))
                                .addStatement("return %T(\n%L\n)", ClassName(packageName, className), readArgs)
                                .build()
                        )

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

        fileSpec.writeTo(codeGenerator, aggregating = false)
        logger.info("Generated DataStore keys for $className with prefix $prefix")
    }

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

    private fun String.toSnakeCase(): String {
        return replace(Regex("([a-z])([A-Z])")) { "${it.groupValues[1]}_${it.groupValues[2]}" }
            .replace(Regex("([A-Z])([A-Z][a-z])")) { "${it.groupValues[1]}_${it.groupValues[2]}" }
            .lowercase()
    }
}
