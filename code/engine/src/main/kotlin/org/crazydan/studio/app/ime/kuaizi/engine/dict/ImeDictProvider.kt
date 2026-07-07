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

package org.crazydan.studio.app.ime.kuaizi.engine.dict

import org.crazydan.studio.app.ime.kuaizi.engine.input.InputWord

/**
 * 字典服务提供者接口：引擎字典查询的公共契约。
 *
 * 接口与实现分离的设计允许第三方应用替换整个字典层——例如使用远程字典服务替代本地 SQLite，
 * 或使用自定义的候选排序算法——而无需修改引擎核心逻辑。
 * [ImeDictProvider] 是引擎与字典数据之间的唯一契约，引擎不直接依赖任何具体的字典实现。
 *
 * 查询结果按频率降序排列，高频候选排在前端。
 * 所有方法均为挂起函数，需在协程中调用。
 *
 * @see SqliteDictProvider 基于 Room SQLite 的默认实现
 */
interface ImeDictProvider {

    /** 加载全量拼音 */
    suspend fun loadAllPinyin(): List<String> = emptyList()

    /** 根据完整拼音查询候选词，返回按频率排序的 [InputWord] 列表。 */
    suspend fun query(pinyin: String): List<InputWord>

    /** 根据拼音前缀查询候选词，支持模糊匹配，用户输入部分拼音即可获得候选建议。 */
    suspend fun queryPrefix(prefix: String): List<InputWord>

    /** 查询拉丁词补全建议，根据已输入的拉丁字母前缀返回匹配的候选词。 */
    suspend fun queryLatinCompletions(prefix: String): List<InputWord>

    /** 查询拼音词组补全建议，根据已输入的拼音前缀返回匹配的词组候选。 */
    suspend fun queryPhraseCompletions(prefix: String): List<InputWord>

    /**
     * 记录用户的输入选择，更新用户词频。
     * 频率更新影响后续查询的排序结果，用户常用词会自然浮动到列表前端。
     */
    suspend fun recordInput(pinyin: String, word: String)
}
