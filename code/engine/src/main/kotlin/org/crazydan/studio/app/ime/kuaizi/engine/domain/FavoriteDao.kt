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

/**
 * 收藏的数据库实体，映射 Room 数据库中的 user_input_favorite 表
 * @param id 主键
 * @param text 收藏文本
 * @param type 文本类型
 * @param usageCount 使用次数
 * @param createdAt 创建时间戳
 */
data class FavoriteEntity(
    val id: Long = 0,
    val text: String,
    val type: String? = null,
    val usageCount: Int,
    val createdAt: Long,
) {
    /** 转换为领域模型 [InputFavorite] */
    fun toDomain(): InputFavorite = InputFavorite(
        text = text,
        type = type?.let { InputTextType.valueOf(it) },
        usageCount = usageCount,
        createdAt = createdAt,
    )
}

/** 收藏数据访问接口，定义与 Room 数据库交互的契约 */
interface FavoriteDao {
    /** 获取所有收藏的响应式流 */
    fun getAllFlow(): Flow<List<FavoriteEntity>>
    /** 获取所有收藏列表（一次性查询） */
    suspend fun getAll(): List<FavoriteEntity>
    /** 根据文本查询收藏 */
    suspend fun getByText(text: String): FavoriteEntity?
    /** 插入或更新收藏（幂等操作） */
    suspend fun upsert(entity: FavoriteEntity)
    /** 删除指定文本的收藏 */
    suspend fun delete(text: String)
    /** 清空所有收藏 */
    suspend fun clearAll()
}
