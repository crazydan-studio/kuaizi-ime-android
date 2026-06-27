package org.crazydan.studio.app.ime.kuaizi.engine.dict

import kotlinx.coroutines.flow.Flow
import org.crazydan.studio.app.ime.kuaizi.engine.dict.db.*
import org.crazydan.studio.app.ime.kuaizi.engine.domain.FavoriteDao
import org.crazydan.studio.app.ime.kuaizi.engine.domain.FavoriteEntity

class DictRepository(
    private val wordDao: PinyinWordDao,
    private val phraseDao: PinyinPhraseDao,
    private val userInputDao: UserInputDao,
    private val favoriteDao: FavoriteDao,
    private val hmmDao: HmmDao,
) {
    suspend fun lookupPinyinWords(spell: String): List<PinyinWordEntity> =
        wordDao.lookupBySpell(spell)

    suspend fun lookupPinyinPhrases(spells: List<String>): List<PinyinPhraseEntity> =
        phraseDao.lookupBySpells(spells.joinToString(","))

    suspend fun lookupByPrefix(prefix: String): List<PinyinWordEntity> =
        wordDao.lookupByPrefix(prefix)

    suspend fun predictPhrase(currentSpell: String, context: List<String>): List<PinyinPhraseEntity> {
        val nextStates = hmmDao.predictNextStates(currentSpell)
        return nextStates.mapNotNull { state ->
            phraseDao.lookupBySpells(state).firstOrNull()
        }
    }

    suspend fun recordUserInput(text: String, type: String) {
        val existing = userInputDao.getByTextAndType(text, type)
        if (existing != null) {
            userInputDao.incrementFrequency(text, type)
        } else {
            userInputDao.upsert(UserInputEntity(text = text, type = type, freq = 1))
        }
    }

    fun observeFavorites(): Flow<List<FavoriteEntity>> = favoriteDao.getAllFlow()
}
