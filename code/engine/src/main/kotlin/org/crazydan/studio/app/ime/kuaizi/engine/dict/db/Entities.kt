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
