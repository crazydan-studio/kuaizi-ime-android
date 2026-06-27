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
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.writeTo
import java.io.OutputStream

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
    }

    private fun buildGeneratedCode(
        packageName: String,
        className: String,
        prefix: String,
        properties: List<KSPropertyDeclaration>,
    ) {
        // Simplified code generation stub
        logger.info("Generated DataStore keys for $className with prefix $prefix")
    }

    private fun String.toSnakeCase(): String {
        return replace(Regex("([a-z])([A-Z])")) { "${it.groupValues[1]}_${it.groupValues[2]}" }
            .lowercase()
    }
}
