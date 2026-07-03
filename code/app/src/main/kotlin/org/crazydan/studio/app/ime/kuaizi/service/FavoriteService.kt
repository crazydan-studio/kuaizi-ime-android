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

package org.crazydan.studio.app.ime.kuaizi.service

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.crazydan.studio.app.ime.kuaizi.engine.domain.FavoriteDao
import org.crazydan.studio.app.ime.kuaizi.engine.domain.FavoriteEntity
import org.crazydan.studio.app.ime.kuaizi.engine.input.InputFavorite
import org.crazydan.studio.app.ime.kuaizi.engine.input.InputTextType

/**
 * 收藏服务。
 *
 * 管理用户收藏的文本片段，通过 [FavoriteDao] 与 Room 数据库交互。
 * 提供响应式收藏列表 [favorites]，支持幂等保存、删除和清空操作。
 *
 * 保存操作为幂等设计：相同文本已存在时仅递增使用次数。
 */
class FavoriteService(
    /** 收藏数据访问对象 */
    private val favoriteDao: FavoriteDao,
    /** 协程作用域，用于驱动响应式列表 */
    private val scope: CoroutineScope,
) {
    /**
     * 所有收藏项的响应式列表。
     *
     * 从 Room 的 Flow 查询映射为领域模型，使用 [WhileSubscribed] 策略
     * 在最后一个订阅者消失后等待 5 秒再停止上游收集，避免配置变更时重启查询。
     */
    val favorites: StateFlow<List<InputFavorite>> = favoriteDao.getAllFlow()
        .map { entities -> entities.map { it.toDomain() } }
        .stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * 保存文本到收藏。
     *
     * 幂等操作：若文本已存在则递增使用次数，否则创建新条目。
     * 新条目记录创建时间戳，用于排序（使用次数降序优先，时间降序次之）。
     */
    suspend fun save(text: String, type: InputTextType? = null) {
        val existing = favoriteDao.getByText(text)
        if (existing != null) {
            // 已存在：递增使用次数
            favoriteDao.upsert(
                existing.copy(usageCount = existing.usageCount + 1),
            )
        } else {
            // 不存在：创建新条目
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

    /** 删除指定文本的收藏。 */
    suspend fun delete(text: String) {
        favoriteDao.delete(text)
    }

    /** 清空所有收藏。 */
    suspend fun clearAll() {
        favoriteDao.clearAll()
    }

    /** 获取收藏项的文本内容，用于粘贴。 */
    fun paste(favorite: InputFavorite): String = favorite.text
}
