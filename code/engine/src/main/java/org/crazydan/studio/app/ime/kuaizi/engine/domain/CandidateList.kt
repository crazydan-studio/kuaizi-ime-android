package org.crazydan.studio.app.ime.kuaizi.engine.domain

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

    fun nextPage(): CandidateList =
        copy(pageIndex = (pageIndex + 1).coerceAtMost(totalPages - 1))

    fun previousPage(): CandidateList =
        copy(pageIndex = (pageIndex - 1).coerceAtLeast(0))

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

    data class Symbol(
        override val text: String,
        override val frequency: Int = 0,
        val group: String = "",
    ) : InputWord()

    data object Emoji : InputWord() {
        override val text: String get() = ""
        override val frequency: Int get() = 0
    }

    data class MathExpr(
        override val text: String,
        override val frequency: Int = 0,
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
