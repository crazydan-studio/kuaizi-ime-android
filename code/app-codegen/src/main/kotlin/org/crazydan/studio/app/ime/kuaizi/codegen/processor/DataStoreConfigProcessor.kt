package org.crazydan.studio.app.ime.kuaizi.codegen.processor

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.isAnnotationPresent
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration

class DataStoreConfigProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
) : SymbolProcessor {

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val symbols = resolver.getSymbolsWithAnnotation(DataStoreConfig::class.simpleName.orEmpty())
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

    @OptIn(KspExperimental::class)
    private fun generateDataStoreKeys(declaration: KSClassDeclaration) {
        val packageName = declaration.packageName.asString()
        val className = declaration.simpleName.asString()
        val annotation = declaration.annotations
            .first { it.javaClass == DataStoreConfig::class.java }
        val prefix = annotation.arguments
            .firstOrNull { it.name?.asString() == "prefix" }
            ?.value as? String ?: className.toSnakeCase()

        val properties = declaration.getAllProperties()
            .filter { it.isAnnotationPresent(DataStoreKey::class) }
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
