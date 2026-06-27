package org.crazydan.studio.app.ime.kuaizi

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.crazydan.studio.app.ime.kuaizi.engine.domain.FavoriteDao
import org.crazydan.studio.app.ime.kuaizi.engine.domain.FavoriteEntity
import org.crazydan.studio.app.ime.kuaizi.engine.domain.InputFavorite
import org.crazydan.studio.app.ime.kuaizi.engine.domain.InputTextType

class FavoriteService(
    private val favoriteDao: FavoriteDao,
    private val scope: CoroutineScope,
) {
    val favorites: StateFlow<List<InputFavorite>> = favoriteDao.getAllFlow()
        .map { entities -> entities.map { it.toDomain() } }
        .stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())

    suspend fun save(text: String, type: InputTextType? = null) {
        val existing = favoriteDao.getByText(text)
        if (existing != null) {
            favoriteDao.upsert(
                existing.copy(usageCount = existing.usageCount + 1),
            )
        } else {
            favoriteDao.upsert(
                FavoriteEntity(
                    text = text,
                    type = type?.name,
                    usageCount = 1,
                    createdAt = System.currentTimeMillis(),
                ),
            )
        }
    }

    suspend fun delete(text: String) {
        favoriteDao.delete(text)
    }

    suspend fun clearAll() {
        favoriteDao.clearAll()
    }

    fun paste(favorite: InputFavorite): String = favorite.text
}
