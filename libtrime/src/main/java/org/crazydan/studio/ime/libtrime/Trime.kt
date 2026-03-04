/*
 * SPDX-FileCopyrightText: 2015 - 2025 Rime community
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

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

package org.crazydan.studio.ime.libtrime

import kotlinx.coroutines.flow.SharedFlow
import java.io.File

/**
 * Trime 接口
 *
 * source from [trime/RimeApi.kt](https://github.com/osfans/trime/blob/develop/app/src/main/java/com/osfans/trime/core/RimeApi.kt)
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2026-03-04
 */
interface Trime {
    val messageFlow: SharedFlow<TrimeMessage<*>>

    val isReady: Boolean

    // -------------------------------------------------------------------

    /**
     * 获取已激活的输入方案（id 值）
     *
     * Note: 可通过 [TrimeConfig.openSchema] 获取输入方案的配置数据
     */
    suspend fun getActiveSchema(): String

    /** 激活指定的输入方案 */
    suspend fun activateSchema(schemaId: String): Boolean

    /** 获取已启用的输入方案 */
    suspend fun getEnabledSchemas(): Array<TrimeSchema>

    /** 获取可用的（已部署的，包括未启用的）输入方案 */
    suspend fun getAvailableSchemas(): Array<TrimeSchema>

    /** 启用指定的输入方案 */
    suspend fun enableSchemas(schemaIds: Array<String>): Boolean

    /** 部署 `.schema.yaml` 输入方案文件 */
    suspend fun deploySchema(schemaFile: File): Boolean

    // -------------------------------------------------------------------
}