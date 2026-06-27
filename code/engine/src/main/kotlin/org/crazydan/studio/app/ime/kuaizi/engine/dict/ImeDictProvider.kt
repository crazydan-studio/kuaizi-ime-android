package org.crazydan.studio.app.ime.kuaizi.engine.dict

import org.crazydan.studio.app.ime.kuaizi.engine.domain.InputCompletion
import org.crazydan.studio.app.ime.kuaizi.engine.domain.InputWord

interface ImeDictProvider {
    fun query(pinyin: String): List<InputWord>

    fun queryPrefix(prefix: String): List<InputWord>

    fun recordInput(pinyin: String, word: String)

    fun queryLatinCompletions(prefix: String): List<InputCompletion.LatinWord>

    fun queryPhraseCompletions(prefix: String): List<InputCompletion.PhraseWord>
}
