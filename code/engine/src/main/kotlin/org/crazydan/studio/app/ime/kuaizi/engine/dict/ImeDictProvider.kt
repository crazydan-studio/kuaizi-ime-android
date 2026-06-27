package org.crazydan.studio.app.ime.kuaizi.engine.dict

import org.crazydan.studio.app.ime.kuaizi.engine.domain.InputWord

interface ImeDictProvider {
    suspend fun query(pinyin: String): List<InputWord>

    suspend fun queryPrefix(prefix: String): List<InputWord>

    suspend fun queryLatinCompletions(prefix: String): List<InputWord>

    suspend fun queryPhraseCompletions(prefix: String): List<InputWord>

    suspend fun recordInput(pinyin: String, word: String)
}
