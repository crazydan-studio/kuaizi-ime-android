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

package org.crazydan.studio.app.ime.kuaizi.engine.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class FavoriteEntity(
    val id: Long = 0,
    val text: String,
    val type: String? = null,
    val usageCount: Int,
    val createdAt: Long,
) {
    fun toDomain(): InputFavorite = InputFavorite(
        text = text,
        type = type?.let { InputTextType.valueOf(it) },
        usageCount = usageCount,
        createdAt = createdAt,
    )
}

interface FavoriteDao {
    fun getAllFlow(): Flow<List<FavoriteEntity>>
    suspend fun getAll(): List<FavoriteEntity>
    suspend fun getByText(text: String): FavoriteEntity?
    suspend fun upsert(entity: FavoriteEntity)
    suspend fun delete(text: String)
    suspend fun clearAll()
}
