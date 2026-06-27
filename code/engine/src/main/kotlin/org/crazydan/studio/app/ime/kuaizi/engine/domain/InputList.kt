package org.crazydan.studio.app.ime.kuaizi.engine.domain

sealed class InputItem {
    abstract val id: String

    data class Char(
        override val id: String,
        val value: String,
    ) : InputItem()

    data class Gap(
        override val id: String,
    ) : InputItem()

    data class MathExpr(
        override val id: String,
        val expression: String,
        val result: String? = null,
    ) : InputItem()
}

data class InputList(
    val inputs: List<InputItem> = emptyList(),
    val gapIndex: Int = 0,
    val pendingInput: PendingInput? = null,
    val inputCompletion: InputCompletion? = null,
) {
    val hasPending: Boolean get() = pendingInput != null

    val chars: List<InputItem.Char>
        get() = inputs.filterIsInstance<InputItem.Char>()

    val charCount: Int get() = chars.size
}

data class PendingInput(
    val isSingleChar: Boolean = false,
    val pressedOnChar: Boolean = false,
    val swipedInThisLevel: Boolean = false,
    val chars: List<String> = emptyList(),
)

data class InputCompletion(
    val text: String = "",
    val candidates: List<String> = emptyList(),
)

data class PairSymbol(
    val left: String,
    val right: String,
)
