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

import org.crazydan.studio.app.ime.kuaizi.engine.PageDirection

/**
 * 候选列表分页器，按指定方向翻页候选词。
 * 封装 [CandidateList] 的翻页逻辑，提供方向驱动的分页操作。
 */
class CandidateListPager {
    /**
     * 按指定方向翻页
     * @param candidateList 当前候选列表
     * @param direction 翻页方向（下一页/上一页）
     * @return 翻页后的候选列表
     */
    fun page(candidateList: CandidateList, direction: PageDirection): CandidateList {
        return when (direction) {
            PageDirection.Next -> candidateList.nextPage()
            PageDirection.Previous -> candidateList.prevPage()
        }
    }
}
