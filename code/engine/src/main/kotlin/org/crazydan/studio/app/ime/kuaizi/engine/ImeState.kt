package org.crazydan.studio.app.ime.kuaizi.engine

import org.crazydan.studio.app.ime.kuaizi.engine.domain.*

data class ImeState(
    val keyboard: Keyboard = Keyboard(),
    val inputList: InputList = InputList(),
    val candidateList: CandidateList = CandidateList(),
    val clipboard: Clipboard = Clipboard(),
    val favoriteList: FavoriteList = FavoriteList(),
    val toolListState: ToolListState = ToolListState(),
    val config: ImeConfig = ImeConfig(),
)

data class ToolListState(
    val settings: Boolean = true,
    val switchIME: Boolean = true,
    val closeKeyboard: Boolean = true,
)
