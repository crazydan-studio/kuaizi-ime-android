package org.crazydan.studio.app.ime.kuaizi.codegen.processor

import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider

class DataStoreConfigProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return DataStoreConfigProcessor(
            codeGenerator = environment.codeGenerator,
            logger = environment.logger,
        )
    }
}
