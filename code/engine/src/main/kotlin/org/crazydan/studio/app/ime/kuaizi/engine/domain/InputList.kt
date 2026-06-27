package org.crazydan.studio.app.ime.kuaizi.engine.domain

sealed class InputItem {
    abstract val id: String

    data class Char(
        override val id: String,
        val text: String,
        val replacements: List<String>? = null,
        val pairSymbol: PairSymbol? = null,
    ) : InputItem()

    data class Gap : InputItem() {
        override val id: String = "gap"
    }

    data class MathExpr(
        override val id: String,
    ) : InputItem()
}

data class InputList(
    val inputs: List<InputItem> = emptyList(),
    val gapIndex: Int = 0,
    val pendingInput: PendingInput? = null,
    val inputCompletion: InputCompletion? = null,
) {
    val isEmpty: Boolean get() = inputs.isEmpty()
    val cursorGap: Gap? get() = inputs.getOrNull(gapIndex) as? Gap
    val visibleInputs: List<InputItem> get() = inputs.filter { it !is Gap }
    val text: String get() = chars.joinToString("") { it.text }
    val chars: List<Char> get() = inputs.filterIsInstance<Char>()

    val hasPending: Boolean get() = pendingInput != null
    val charCount: Int get() = chars.size
}

data class PendingInput(
    val chars: List<String> = emptyList(),
    val completions: List<InputCompletion> = emptyList(),
)

sealed class InputCompletion {
    data class LatinWord(
        val text: String,
        val frequency: Int = 0,
    ) : InputCompletion()

    data class PhraseWord(
        val text: String,
        val frequency: Int = 0,
    ) : InputCompletion()
}

data class PairSymbol(
    val left: String,
    val right: String,
    val content: String = "",
)
