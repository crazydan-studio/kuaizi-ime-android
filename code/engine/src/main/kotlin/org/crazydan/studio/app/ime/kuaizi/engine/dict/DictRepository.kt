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

class DictRepository(
    private val wordDao: PinyinWordDao,
    private val phraseDao: PinyinPhraseDao,
    private val userInputDao: UserInputDao,
    private val favoriteDao: FavoriteDao,
    private val hmmDao: HmmDao,
) {
    suspend fun lookupPinyinWords(spell: String): List<PinyinWordEntity> =
        wordDao.lookupBySpell(spell)

    suspend fun lookupPinyinPhrases(spells: List<String>): List<PinyinPhraseEntity> =
        phraseDao.lookupBySpells(spells.joinToString(","))

    suspend fun lookupByPrefix(prefix: String): List<PinyinWordEntity> =
        wordDao.lookupByPrefix(prefix)

    suspend fun predictPhrase(currentSpell: String, context: List<String>): List<PinyinPhraseEntity> {
        val nextStates = hmmDao.predictNextStates(currentSpell)
        return nextStates.mapNotNull { state ->
            phraseDao.lookupBySpells(state).firstOrNull()
        }
    }

    suspend fun recordUserInput(text: String, type: String) {
        val existing = userInputDao.getByTextAndType(text, type)
        if (existing != null) {
            userInputDao.incrementFrequency(text, type)
        } else {
            userInputDao.upsert(UserInputEntity(text = text, type = type, freq = 1))
        }
    }

    fun observeFavorites(): Flow<List<FavoriteEntity>> = favoriteDao.getAllFlow()
}
