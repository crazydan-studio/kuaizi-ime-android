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

/**
 * 收藏列表状态的不可变数据模型。
 * 管理用户收藏的文本条目、加载状态和功能开关。
 *
 * @param favorites 收藏条目列表，按使用频次和时间排序
 * @param disabled 功能是否禁用，由配置门控决定
 * @param isLoading 是否正在从数据库加载收藏列表
 */
data class FavoriteList(
    val favorites: List<InputFavorite> = emptyList(),
    val disabled: Boolean = false,
    val isLoading: Boolean = false,
)

/**
 * 收藏条目的数据模型
 * @param text 收藏的文本内容
 * @param type 文本类型
 * @param usageCount 使用次数
 * @param createdAt 创建时间戳（毫秒）
 * @param usedAt 最后使用时间戳（毫秒）
 */
data class InputFavorite(
    val text: String,
    val type: InputTextType? = null,
    val usageCount: Int = 0,
    val createdAt: Long = 0L,
    val usedAt: Long = 0L,
)
