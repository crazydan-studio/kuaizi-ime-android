package org.crazydan.studio.app.ime.kuaizi.engine.dict

import org.crazydan.studio.app.ime.kuaizi.engine.domain.CandidateList
import org.crazydan.studio.app.ime.kuaizi.engine.domain.InputWord

interface ImeDictProvider {
    fun query(pinyin: String): CandidateList
    fun queryPrefix(prefix: String): CandidateList
    fun recordInput(pinyin: String, word: String)
}
