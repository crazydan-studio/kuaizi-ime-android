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

package org.crazydan.studio.ime.libtrime

import com.osfans.trime.core.CandidateItem

/** （单个）字信息 */
data class TrimeWord(
    /** 文本 */
    val text: String,
    /** 读音：非汉字没有读音信息 */
    val spell: String?,
) {

    companion object {

        fun from(item: CandidateItem): TrimeWord =
            TrimeWord(
                text = item.text,
                spell = item.comment.ifBlank { null },
            )

        fun from(items: Array<CandidateItem>): Array<TrimeWord> =
            items.map(::from).toTypedArray()
    }

    fun hasSpell(): Boolean =
        !spell.isNullOrEmpty() && spell != "∞"

    fun splitBySpell(): List<TrimeWord> =
        spell?.split("\\s+".toRegex())?.mapIndexed { index, s ->
            TrimeWord(
                text = text[index].toString(),
                spell = s,
            )
        } ?: listOf()
}