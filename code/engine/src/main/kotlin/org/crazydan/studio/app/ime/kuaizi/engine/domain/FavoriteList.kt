package org.crazydan.studio.app.ime.kuaizi.engine.domain

data class FavoriteList(
    val favorites: List<InputFavorite> = emptyList(),
    val disabled: Boolean = false,
    val isLoading: Boolean = false,
)

data class InputFavorite(
    val text: String,
    val type: InputTextType? = null,
    val usageCount: Int = 0,
    val createdAt: Long = 0L,
    val usedAt: Long = 0L,
)
