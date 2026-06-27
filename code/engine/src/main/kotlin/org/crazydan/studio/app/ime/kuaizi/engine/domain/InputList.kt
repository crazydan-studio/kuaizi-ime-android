package org.crazydan.studio.app.ime.kuaizi.engine.domain

sealed class InputItem {
    abstract val id: String

    data class Char(
        override val id: String,
        val text: String,
        val keys: List<InputKey>,
        val replacements: List<String> = emptyList(),
        val word: InputWord? = null,
        val pairSymbol: PairSymbol? = null,
    ) : InputItem() {
        val hasPair: Boolean get() = pairSymbol != null
        val hasReplacements: Boolean get() = replacements.size > 1

        fun nextReplacement(text: String): String {
            if (replacements.size <= 1) return text
            val index = replacements.indexOf(text)
            return if (index >= 0) replacements[(index + 1) % replacements.size] else replacements[0]
        }

        fun canReplace(key: InputKey.Char): Boolean =
            replacements.size > 1 && key.text in replacements
    }

    data object Gap : InputItem() {
        override val id = "gap"
    }

    data class MathExpr(
        override val id: String,
        val nestedList: InputList,
    ) : InputItem()
}

data class InputList(
    val inputs: List<InputItem> = emptyList(),
    val gapIndex: Int = 0,
    val pending: PendingInput? = null,
    val mathExprNested: InputList? = null,
) {
    init {
        require(gapIndex >= 0)
        require(gapIndex <= inputs.lastIndex + 1)
    }

    val cursorGap: InputItem.Gap
        get() = inputs.getOrElse(gapIndex) { InputItem.Gap }

    val visibleInputs: List<InputItem.Char>
        get() = inputs.filterIsInstance<InputItem.Char>()

    val text: String
        get() = visibleInputs.joinToString("") { it.text }

    val isEmpty: Boolean
        get() = inputs.all { it is InputItem.Gap }

    fun appendChar(char: InputItem.Char): InputList {
        val newInputs = inputs.toMutableList().apply {
            add(gapIndex, char)
            add(gapIndex + 1, InputItem.Gap)
        }
        return copy(inputs = newInputs, gapIndex = gapIndex + 2)
    }

    fun deleteCharBeforeCursor(): InputList {
        if (gapIndex < 2) return this
        val newInputs = inputs.toMutableList().apply {
            removeAt(gapIndex - 2)
            removeAt(gapIndex - 2)
        }
        return copy(inputs = newInputs, gapIndex = gapIndex - 2)
    }

    fun moveCursorTo(newGapIndex: Int): InputList {
        val clampedIndex = newGapIndex.coerceIn(0, inputs.lastIndex)
        return copy(gapIndex = clampedIndex)
    }

    fun clean(): InputList = InputList()

    fun withPending(pending: PendingInput?): InputList =
        copy(pending = pending)
}

data class PendingInput(
    val chars: List<InputItem.Char>,
    val completions: List<InputCompletion> = emptyList(),
    val pinyinToggles: Set<PinyinToggleType> = emptySet(),
)

sealed class InputCompletion {
    abstract val text: String

    data class LatinWord(
        override val text: String,
        val remaining: String,
    ) : InputCompletion()

    data class PhraseWord(
        override val text: String,
        val remaining: String,
        val spells: List<String>,
    ) : InputCompletion()
}

data class PairSymbol(
    val open: String,
    val close: String,
    val content: String? = null,
)

enum class PinyinToggleType { FullPinyin, DoublePinyin, Bopomofo, ShowTone }

object InputGapSpacing {
    fun needsGap(left: InputItem.Char, right: InputItem.Char): Boolean {
        if (left.pairSymbol != null && left.pairSymbol.content == null) return false
        if (left.word is InputWord.PinyinPhrase && left.word == right.word) return false
        if (isLatinChar(left) && isLatinChar(right)) return false
        if (isDigitChar(left) && isDigitChar(right)) return false
        return true
    }

    private fun isLatinChar(char: InputItem.Char): Boolean =
        char.word is InputWord.Latin || char.text.all { it.isLetter() && it.code < 128 }

    private fun isDigitChar(char: InputItem.Char): Boolean =
        char.text.all { it.isDigit() }
}
