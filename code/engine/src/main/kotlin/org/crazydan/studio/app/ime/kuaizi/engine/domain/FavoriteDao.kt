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
