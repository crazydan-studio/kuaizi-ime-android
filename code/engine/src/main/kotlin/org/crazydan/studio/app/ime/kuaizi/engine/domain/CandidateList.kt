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
    val currentPage: List<InputWord>
        get() {
            val start = pageIndex * pageSize
            val end = minOf(start + pageSize, candidates.size)
            return if (start < candidates.size) candidates.subList(start, end) else emptyList()
        }

    val totalPages: Int
        get() = if (candidates.isEmpty()) 0 else (candidates.size + pageSize - 1) / pageSize

    fun nextPage(): CandidateList {
        if (candidates.isEmpty()) return this
        return copy(pageIndex = (pageIndex + 1) % totalPages)
    }

    fun previousPage(): CandidateList {
        if (candidates.isEmpty()) return this
        return copy(pageIndex = if (pageIndex > 0) pageIndex - 1 else totalPages - 1)
    }

    fun withFilter(filter: PinyinWordFilter): CandidateList =
        copy(filter = filter)
}

sealed class InputWord {
    abstract val text: String
    abstract val frequency: Int

    data class Pinyin(
        override val text: String,
        override val frequency: Int = 0,
        val spell: String = "",
        val variant: String? = null,
        val tone: Int? = null,
    ) : InputWord()

    data class PinyinPhrase(
        override val text: String,
        override val frequency: Int = 0,
        val spells: List<String> = emptyList(),
    ) : InputWord()

    data class LatinWord(
        override val text: String,
        override val frequency: Int = 0,
    ) : InputWord()

    data class Emoji(
        override val text: String,
        override val frequency: Int = 0,
        val name: String = "",
        val group: String = "",
    ) : InputWord()

    data class CommitOption(
        override val text: String,
        override val frequency: Int = 0,
        val action: ImeIntent? = null,
    ) : InputWord()
}

class PinyinWordFilter(
    val radical: String? = null,
    val tone: Int? = null,
)
