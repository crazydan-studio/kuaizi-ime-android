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
