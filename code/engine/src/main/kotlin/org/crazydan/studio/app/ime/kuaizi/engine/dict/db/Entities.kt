package org.crazydan.studio.app.ime.kuaizi.engine.dict.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pinyin_word")
data class PinyinWordEntity(
    @PrimaryKey val id: Long,
    val spell: String,
    val text: String,
    val freq: Int,
    val variant: String? = null,
    val tone: Int? = null,
)

@Entity(tableName = "pinyin_phrase")
data class PinyinPhraseEntity(
    @PrimaryKey val id: Long,
    val spells: String,
    val text: String,
    val freq: Int,
)

@Entity(tableName = "user_input_data")
data class UserInputEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val text: String,
    val type: String,
    val freq: Int = 0,
    val lastUsed: Long = System.currentTimeMillis(),
)

@Entity(tableName = "user_input_favorite")
data class FavoriteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val text: String,
    val type: String? = null,
    val usageCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "hmm_transition")
data class HmmTransitionEntity(
    @PrimaryKey val id: Long,
    val fromState: String,
    val toState: String,
    val probability: Double,
)
