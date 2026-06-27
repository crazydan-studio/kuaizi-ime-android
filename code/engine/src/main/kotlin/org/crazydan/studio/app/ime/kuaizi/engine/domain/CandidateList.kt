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
