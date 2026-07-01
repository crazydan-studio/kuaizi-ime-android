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

import kotlinx.coroutines.flow.Flow
import org.crazydan.studio.app.ime.kuaizi.engine.dict.db.*
import org.crazydan.studio.app.ime.kuaizi.engine.domain.FavoriteDao
import org.crazydan.studio.app.ime.kuaizi.engine.domain.FavoriteEntity

/**
 * 字典仓库，封装所有基于 Room 的数据库操作。
 *
 * 将数据库实体（Entity）映射为领域模型，提供协程化的异步接口。
 * 作为 [SqliteDictProvider] 的内部实现细节——引擎对外暴露的公共接口是 [ImeDictProvider]，
 * 第三方应用通过实现 [ImeDictProvider] 替换整个字典层时无需了解本类的存在。
 *
 * @param wordDao 拼音单字 DAO
 * @param phraseDao 拼音词组 DAO
 * @param userInputDao 用户输入记录 DAO
 * @param favoriteDao 用户收藏 DAO
 * @param hmmDao HMM 状态转移 DAO
 */
class DictRepository(
    private val wordDao: PinyinWordDao,
    private val phraseDao: PinyinPhraseDao,
    private val userInputDao: UserInputDao,
    private val favoriteDao: FavoriteDao,
    private val hmmDao: HmmDao,
) {

    /** 根据拼音精确查询单字候选列表。 */
    suspend fun lookupPinyinWords(spell: String): List<PinyinWordEntity> =
        wordDao.lookupBySpell(spell)

    /** 根据拼音序列精确查询词组候选列表。 */
    suspend fun lookupPinyinPhrases(spells: List<String>): List<PinyinPhraseEntity> =
        phraseDao.lookupBySpells(spells.joinToString(","))

    /** 根据拼音前缀模糊查询单字候选列表。 */
    suspend fun lookupByPrefix(prefix: String): List<PinyinWordEntity> =
        wordDao.lookupByPrefix(prefix)

    /**
     * 根据当前拼音和上下文预测下一个可能的词组。
     * 先通过 HMM 模型预测下一个拼音状态，再查询对应的词组候选。
     */
    suspend fun predictPhrase(currentSpell: String, context: List<String>): List<PinyinPhraseEntity> {
        val nextStates = hmmDao.predictNextStates(currentSpell)
        return nextStates.mapNotNull { state ->
            phraseDao.lookupBySpells(state).firstOrNull()
        }
    }

    /**
     * 记录用户输入，更新使用频率。
     * 不存在时插入新记录（频率为 1），已存在时原子递增频率。
     */
    suspend fun recordUserInput(text: String, type: String) {
        val existing = userInputDao.getByTextAndType(text, type)
        if (existing != null) {
            userInputDao.incrementFrequency(text, type)
        } else {
            userInputDao.upsert(UserInputEntity(text = text, type = type, freq = 1))
        }
    }

    /** 观察用户收藏列表的响应式 Flow。 */
    fun observeFavorites(): Flow<List<FavoriteEntity>> = favoriteDao.getAllFlow()
}
