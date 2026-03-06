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
 * source from [trime/RimeApi.kt](https://github.com/osfans/trime/blob/develop/app/src/main/java/com/osfans/trime/core/RimeApi.kt)
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2026-03-04
 */
interface Trime {
    interface Config {
        /** Rime 用户数据目录 */
        var userDataDir: File?

        /** Rime 共享数据目录 */
        var sharedDataDir: File?
    }

    /** 通过 [TrimeSession.run] 启动对 [TrimeMessage] 的监听 */
    val messageFlow: SharedFlow<TrimeMessage<*>>

    val isReady: Boolean

    // -------------------------------------------------------------------

    /** 获取输入（全拼、双拼等）对应的候选字列表：结果按使用权重降序排序 */
    suspend fun getCandidates(
        input: String,
        /** 若 <= 0，则获取全部候选字，否则，仅获取前 [top] 个候选字 */
        top: Int = -1,
    ): Array<TrimeWord>

    /** 获取输入（全拼、双拼等）对应的最佳候选字 */
    suspend fun getBestCandidate(input: String): TrimeWord? =
        getCandidates(input, 1).firstOrNull()

    /** 根据输入（全拼、双拼等）预测短语 */
    suspend fun predictPhrase(inputs: Array<String>): Array<TrimeWord>

    /** 提交短语从而更新用户词库中的字词权重 */
    suspend fun commitPhrase(phrase: Array<TrimeWord>)

    // -------------------------------------------------------------------

    // 通过 /pinyin - 全拼，/zrm - 自然码，/flypy - 小鹤 命令切换 Schema 所支持的输入法

    // -------------------------------------------------------------------
    // Note: Rime Schema 需要依次经历 部署 -> 启用 -> 激活 三个步骤

    /**
     * 获取已激活的输入方案（id 值）
     *
     * Note: 可通过 [TrimeConfig.openSchemaConfig] 获取输入方案的配置数据
     */
    suspend fun getActivatedSchema(): String

    /** 激活指定的输入方案 */
    suspend fun activateSchema(schemaId: String): Boolean

    /** 获取已启用的输入方案 */
    suspend fun getEnabledSchemas(): Array<TrimeSchema>

    /** 启用指定的输入方案 */
    suspend fun enableSchemas(schemaIds: Array<String>): Boolean

    /** 获取已部署的输入方案 */
    suspend fun getDeployedSchemas(): Array<TrimeSchema>

    /** 部署 `.schema.yaml` 输入方案文件 */
    suspend fun deploySchema(schemaFile: File): Boolean

    // TODO 部署 zip 压缩包：支持对万象拼音的直接部署

    // -------------------------------------------------------------------

    /**
     * 重新（全量）部署共享/用户目录中的数据，**适合首次部署或重大更新之后**。
     * 详见 [com.osfans.trime.core.Rime.startupRime] (`fullCheck == true`)
     */
    suspend fun redeploy()
}