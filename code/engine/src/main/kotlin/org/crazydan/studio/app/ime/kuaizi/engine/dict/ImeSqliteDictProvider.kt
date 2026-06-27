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

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.withContext
import org.crazydan.studio.app.ime.kuaizi.engine.dict.db.DictDatabase
import org.crazydan.studio.app.ime.kuaizi.engine.domain.FavoriteDao
import org.crazydan.studio.app.ime.kuaizi.engine.domain.FavoriteEntity
import org.crazydan.studio.app.ime.kuaizi.engine.domain.InputWord
import org.crazydan.studio.app.ime.kuaizi.engine.domain.Spell
import org.crazydan.studio.app.ime.kuaizi.engine.domain.Tone
import org.crazydan.studio.app.ime.kuaizi.engine.domain.Variant
import org.crazydan.studio.app.ime.kuaizi.engine.domain.VariantType

class ImeSqliteDictProvider(
    context: Context,
    private val favoriteDao: FavoriteDao? = null,
) : ImeDictProvider {
    private val db = DictDatabase.getInstance(context)
    private val repository = DictRepository(
        wordDao = db.pinyinWordDao(),
        phraseDao = db.pinyinPhraseDao(),
        userInputDao = db.userInputDao(),
        favoriteDao = favoriteDao ?: object : FavoriteDao {
            override fun getAllFlow() = kotlinx.coroutines.flow.emptyFlow()
            override fun getAll(): List<FavoriteEntity> = emptyList()
            override fun getByText(text: String) = null
            override fun upsert(entity: FavoriteEntity) {
                db.userInputDao().upsert(
                    org.crazydan.studio.app.ime.kuaizi.engine.dict.db.UserInputEntity(
                        text = entity.text,
                        type = "favorite",
                        freq = entity.usageCount,
                    )
                )
            }
            override fun delete(text: String) {}
            override fun clearAll() {}
        },
        hmmDao = db.hmmDao(),
    )

    override suspend fun query(pinyin: String): List<InputWord> = withContext(Dispatchers.Default) {
        val words = repository.lookupPinyinWords(pinyin).map { entity ->
            InputWord.Pinyin(
                text = entity.text,
                spell = Spell(id = entity.spell, value = entity.spell),
                frequency = entity.freq,
                variant = entity.variant?.let { Variant(text = it, type = VariantType.Variant) },
                tone = entity.tone?.let { Tone.entries.getOrNull(it - 1) },
            )
        }
        val phrases = repository.lookupPinyinPhrases(listOf(pinyin)).map { entity ->
            InputWord.PinyinPhrase(
                text = entity.text,
                spells = entity.spells.split(","),
                frequency = entity.freq,
            )
        }
        (words + phrases).sortedByDescending { it.frequency }
    }

    override suspend fun queryPrefix(prefix: String): List<InputWord> = withContext(Dispatchers.Default) {
        repository.lookupByPrefix(prefix).map { entity ->
            InputWord.Pinyin(
                text = entity.text,
                spell = Spell(id = entity.spell, value = entity.spell),
                frequency = entity.freq,
            )
        }
    }

    override suspend fun queryLatinCompletions(prefix: String): List<InputWord> = withContext(Dispatchers.Default) {
        repository.lookupByPrefix(prefix).map { entity ->
            InputWord.Latin(
                text = entity.text,
                frequency = entity.freq,
            )
        }
    }

    override suspend fun queryPhraseCompletions(prefix: String): List<InputWord> {
        val spells = db.pinyinWordDao().lookupSpellsByPrefix(prefix)
        val all = spells.flatMap { spell ->
            val words = repository.lookupPinyinWords(spell)
            words.map { entity ->
                InputWord.Pinyin(
                    text = entity.text,
                    spell = Spell(id = entity.spell, value = entity.spell),
                    frequency = entity.freq,
                )
            }
        }
        return all.take(50)
    }

    override suspend fun recordInput(pinyin: String, word: String) {
        repository.recordUserInput(word, "pinyin")
    }
}
