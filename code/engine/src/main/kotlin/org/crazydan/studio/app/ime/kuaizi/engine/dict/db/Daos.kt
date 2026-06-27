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

package org.crazydan.studio.app.ime.kuaizi.engine.dict.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface PinyinWordDao {
    @Query("SELECT * FROM pinyin_word WHERE spell = :spell ORDER BY freq DESC")
    suspend fun lookupBySpell(spell: String): List<PinyinWordEntity>

    @Query("SELECT * FROM pinyin_word WHERE spell LIKE :prefix || '%' ORDER BY freq DESC LIMIT :limit")
    suspend fun lookupByPrefix(prefix: String, limit: Int = 50): List<PinyinWordEntity>

    @Query("SELECT DISTINCT spell FROM pinyin_word WHERE spell LIKE :prefix || '%'")
    suspend fun lookupSpellsByPrefix(prefix: String): List<String>

    @Query("SELECT * FROM pinyin_word WHERE spell = :spell AND variant IS NOT NULL ORDER BY freq DESC")
    suspend fun lookupVariants(spell: String): List<PinyinWordEntity>
}

@Dao
interface PinyinPhraseDao {
    @Query("SELECT * FROM pinyin_phrase WHERE spells LIKE :spellPrefix || '%' ORDER BY freq DESC LIMIT :limit")
    suspend fun lookupBySpellPrefix(spellPrefix: String, limit: Int = 20): List<PinyinPhraseEntity>

    @Query("SELECT * FROM pinyin_phrase WHERE spells = :spells ORDER BY freq DESC")
    suspend fun lookupBySpells(spells: String): List<PinyinPhraseEntity>
}

@Dao
interface UserInputDao {
    @Query("SELECT * FROM user_input_data WHERE type = :type ORDER BY freq DESC LIMIT :limit")
    suspend fun getTopByType(type: String, limit: Int = 100): List<UserInputEntity>

    @Query("SELECT * FROM user_input_data WHERE text LIKE :prefix || '%' AND type = :type ORDER BY freq DESC LIMIT :limit")
    suspend fun getCompletions(prefix: String, type: String, limit: Int = 20): List<UserInputEntity>

    @Query("SELECT * FROM user_input_data WHERE text = :text AND type = :type")
    suspend fun getByTextAndType(text: String, type: String): UserInputEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: UserInputEntity)

    @Query("UPDATE user_input_data SET freq = freq + 1, lastUsed = :timestamp WHERE text = :text AND type = :type")
    suspend fun incrementFrequency(text: String, type: String, timestamp: Long = System.currentTimeMillis())
}

@Dao
interface HmmDao {
    @Query("SELECT toState FROM hmm_transition WHERE fromState = :spell ORDER BY probability DESC LIMIT 20")
    suspend fun predictNextStates(spell: String): List<String>
}
