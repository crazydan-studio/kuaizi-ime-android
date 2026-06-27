package org.crazydan.studio.app.ime.kuaizi.ui.keyboard

import org.crazydan.studio.app.ime.kuaizi.engine.ImeConfig
import org.crazydan.studio.app.ime.kuaizi.engine.domain.*

interface KeyTableGenerator {
    fun generate(context: KeyTableContext): List<List<InputKey>>
}

data class KeyTableContext(
    val config: ImeConfig,
    val keyboard: Keyboard,
    val inputList: InputList,
    val candidateList: CandidateList,
)
