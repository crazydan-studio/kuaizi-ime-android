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

/**
 * 拼音字 DAO：提供单字拼音的精确查询、前缀匹配、拼写列表和变体查询。
 */
@Dao
interface PinyinWordDao {
    /** 根据拼音精确匹配查询单字，按频率降序排列。 */
    @Query("SELECT * FROM pinyin_word WHERE spell = :spell ORDER BY freq DESC")
    suspend fun lookupBySpell(spell: String): List<PinyinWordEntity>

    /** 根据拼音前缀模糊查询单字，最多返回 [limit] 条结果，按频率降序排列。 */
    @Query("SELECT * FROM pinyin_word WHERE spell LIKE :prefix || '%' ORDER BY freq DESC LIMIT :limit")
    suspend fun lookupByPrefix(prefix: String, limit: Int = 50): List<PinyinWordEntity>

    /** 查询匹配前缀的所有不重复拼音拼写，用于自动补全拼音输入。 */
    @Query("SELECT DISTINCT spell FROM pinyin_word WHERE spell LIKE :prefix || '%'")
    suspend fun lookupSpellsByPrefix(prefix: String): List<String>

    /** 查询指定拼音的繁体/异体变体，按频率降序排列。 */
    @Query("SELECT * FROM pinyin_word WHERE spell = :spell AND variant IS NOT NULL ORDER BY freq DESC")
    suspend fun lookupVariants(spell: String): List<PinyinWordEntity>
}

/**
 * 拼音词组 DAO：提供词组的拼音前缀匹配和精确匹配查询。
 */
@Dao
interface PinyinPhraseDao {
    /** 根据拼音前缀模糊查询词组，最多返回 [limit] 条结果。 */
    @Query("SELECT * FROM pinyin_phrase WHERE spells LIKE :spellPrefix || '%' ORDER BY freq DESC LIMIT :limit")
    suspend fun lookupBySpellPrefix(spellPrefix: String, limit: Int = 20): List<PinyinPhraseEntity>

    /** 根据拼音序列精确匹配查询词组。 */
    @Query("SELECT * FROM pinyin_phrase WHERE spells = :spells ORDER BY freq DESC")
    suspend fun lookupBySpells(spells: String): List<PinyinPhraseEntity>
}

/**
 * 用户输入 DAO：提供用户输入记录的查询、插入、频率更新和清空操作。
 */
@Dao
interface UserInputDao {
    /** 查询指定类型中使用频率最高的记录，最多返回 [limit] 条。 */
    @Query("SELECT * FROM user_input_data WHERE type = :type ORDER BY freq DESC LIMIT :limit")
    suspend fun getTopByType(type: String, limit: Int = 100): List<UserInputEntity>

    /** 根据前缀匹配查询指定类型的输入记录，用于输入补全。 */
    @Query("SELECT * FROM user_input_data WHERE text LIKE :prefix || '%' AND type = :type ORDER BY freq DESC LIMIT :limit")
    suspend fun getCompletions(prefix: String, type: String, limit: Int = 20): List<UserInputEntity>

    /** 根据文本和类型精确查询输入记录。 */
    @Query("SELECT * FROM user_input_data WHERE text = :text AND type = :type")
    suspend fun getByTextAndType(text: String, type: String): UserInputEntity?

    /** 插入或替换输入记录（冲突时替换）。 */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: UserInputEntity)

    /** 原子递增指定文本和类型的频率计数。 */
    @Query("UPDATE user_input_data SET freq = freq + 1, lastUsed = :timestamp WHERE text = :text AND type = :type")
    suspend fun incrementFrequency(text: String, type: String, timestamp: Long = System.currentTimeMillis())
}

/**
 * HMM 状态转移 DAO：提供基于拼音的下一状态预测查询。
 */
@Dao
interface HmmDao {
    /** 根据当前拼音预测最可能的下一个拼音状态，最多返回 20 条。 */
    @Query("SELECT toState FROM hmm_transition WHERE fromState = :spell ORDER BY probability DESC LIMIT 20")
    suspend fun predictNextStates(spell: String): List<String>
}
