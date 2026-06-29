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

import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider

/**
 * [DataStoreConfigProcessor] 的提供者。
 *
 * 实现 KSP 的 [SymbolProcessorProvider] 接口，
 * 用于在 KSP 编译过程中注册 [DataStoreConfigProcessor]。
 *
 * 在 `META-INF/services/com.google.devtools.ksp.processing.SymbolProcessorProvider`
 * 中注册此提供者，KSP 框架会自动发现并调用。
 */
class DataStoreConfigProcessorProvider : SymbolProcessorProvider {

    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return DataStoreConfigProcessor(
            codeGenerator = environment.codeGenerator,
            logger = environment.logger,
        )
    }
}
