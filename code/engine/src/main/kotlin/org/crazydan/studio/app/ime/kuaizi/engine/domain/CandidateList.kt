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

import org.crazydan.studio.app.ime.kuaizi.engine.ImeIntent

data class CandidateList(
    val candidates: List<InputWord> = emptyList(),
    val pageIndex: Int = 0,
    val pageSize: Int = 20,
    val hasMore: Boolean = false,
    val totalApprox: Int = 0,
    val filter: PinyinWordFilter? = null,
) {
    val isEmpty: Boolean get() = candidates.isEmpty()

    val totalPages: Int
        get() = if (candidates.isEmpty()) 0 else (candidates.size + pageSize - 1) / pageSize

    val currentPage: List<InputWord>
        get() {
            val fromIndex = pageIndex * pageSize
            val toIndex = minOf(fromIndex + pageSize, candidates.size)
            return if (fromIndex >= candidates.size) emptyList()
            else candidates.subList(fromIndex, toIndex)
        }

    fun nextPage(): CandidateList {
        if (candidates.isEmpty()) return this
        val next = (pageIndex + 1) % totalPages
        return copy(pageIndex = next)
    }

    fun prevPage(): CandidateList {
        if (candidates.isEmpty()) return this
        val prev = if (pageIndex == 0) totalPages - 1 else pageIndex - 1
        return copy(pageIndex = prev)
    }

    fun withCandidates(candidates: List<InputWord>): CandidateList =
        copy(candidates = candidates, pageIndex = 0, hasMore = candidates.size >= pageSize)

    fun withFilter(filter: PinyinWordFilter): CandidateList {
        if (filter.isEmpty) return copy(filter = filter, pageIndex = 0)
        val filtered = candidates.filter { filter.matched(it) }
        return copy(filter = filter, pageIndex = 0, candidates = filtered)
    }
}

sealed class InputWord {
    abstract val text: String
    abstract val frequency: Int

    data class Pinyin(
        override val text: String,
        override val frequency: Int,
        val spell: Spell? = null,
        val variant: Variant? = null,
        val radical: Radical? = null,
        val tone: Tone? = null,
    ) : InputWord()

    data class PinyinPhrase(
        override val text: String,
        override val frequency: Int,
        val spells: List<String>,
    ) : InputWord()

    data class Emoji(
        override val text: String,
        override val frequency: Int = 0,
        val name: String,
        val group: String,
    ) : InputWord()

    data class Latin(
        override val text: String,
        override val frequency: Int,
    ) : InputWord()

    data class CommitOption(
        override val text: String,
        override val frequency: Int = 0,
        val action: ImeIntent? = null,
        val spell: Spell? = null,
        val variant: Variant? = null,
    ) : InputWord()
}

data class Spell(val id: String, val value: String)

data class Variant(val text: String, val type: VariantType)

data class Radical(val text: String, val strokeCount: Int)

enum class Tone { Tone1, Tone2, Tone3, Tone4, Neutral }

enum class SpellUsedMode { FullPinyin, DoublePinyin, Bopomofo }

enum class VariantType { Traditional, Variant }

data class PinyinWordFilter(
    val tones: Set<Tone> = emptySet(),
    val spells: Set<Spell> = emptySet(),
) {
    val isEmpty: Boolean get() = tones.isEmpty() && spells.isEmpty()

    fun matched(word: InputWord): Boolean {
        if (word !is InputWord.Pinyin) return false
        if (tones.isNotEmpty() && word.tone !in tones) return false
        if (spells.isNotEmpty() && word.spell !in spells) return false
        return true
    }

    fun withTone(tone: Tone): PinyinWordFilter =
        copy(tones = tones + tone)

    fun withoutTone(tone: Tone): PinyinWordFilter =
        copy(tones = tones - tone)

    fun withSpell(spell: Spell): PinyinWordFilter =
        copy(spells = spells + spell)

    fun withoutSpell(spell: Spell): PinyinWordFilter =
        copy(spells = spells - spell)

    fun clear(): PinyinWordFilter = PinyinWordFilter()
}
