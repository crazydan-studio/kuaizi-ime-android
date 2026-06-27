package org.crazydan.studio.app.ime.kuaizi.engine.dict

import org.crazydan.studio.app.ime.kuaizi.engine.domain.InputWord

interface ImeDictProvider {
    fun query(pinyin: String): List<InputWord>
    fun queryPrefix(prefix: String): List<InputWord>
    fun recordInput(pinyin: String, word: String)
}
