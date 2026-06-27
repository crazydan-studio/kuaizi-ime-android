package org.crazydan.studio.app.ime.kuaizi.engine.dict

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.withContext
import org.crazydan.studio.app.ime.kuaizi.engine.dict.db.DictDatabase
import org.crazydan.studio.app.ime.kuaizi.engine.domain.FavoriteDao
import org.crazydan.studio.app.ime.kuaizi.engine.domain.FavoriteEntity
import org.crazydan.studio.app.ime.kuaizi.engine.domain.InputWord
import org.crazydan.studio.app.ime.kuaizi.engine.domain.Spell
import org.crazydan.studio.app.ime.kuaizi.engine.domain.Tone
import org.crazydan.studio.app.ime.kuaizi.engine.domain.Variant
import org.crazydan.studio.app.ime.kuaizi.engine.domain.VariantType

class ImeSqliteDictProvider(context: Context) : ImeDictProvider {
    private val db = DictDatabase.getInstance(context)
    private val repository = DictRepository(
        wordDao = db.pinyinWordDao(),
        phraseDao = db.pinyinPhraseDao(),
        userInputDao = db.userInputDao(),
        favoriteDao = object : FavoriteDao {
            override fun getAllFlow() = emptyFlow()
            override suspend fun getAll(): List<FavoriteEntity> = emptyList()
            override suspend fun getByText(text: String) = null
            override suspend fun upsert(entity: FavoriteEntity) {}
            override suspend fun delete(text: String) {}
            override suspend fun clearAll() {}
        },
        hmmDao = db.hmmDao(),
    )

    override suspend fun query(pinyin: String): List<InputWord> = withContext(Dispatchers.Default) {
        val words = repository.lookupPinyinWords(pinyin).map { entity ->
            InputWord.Pinyin(
                text = entity.text,
                spell = Spell(id = entity.spell, value = entity.spell),
                frequency = entity.freq,
                variant = entity.variant?.let { Variant(text = it, type = VariantType.Variant) },
                tone = entity.tone?.let { Tone.entries.getOrNull(it - 1) },
            )
        }
        val phrases = repository.lookupPinyinPhrases(listOf(pinyin)).map { entity ->
            InputWord.PinyinPhrase(
                text = entity.text,
                spells = entity.spells.split(","),
                frequency = entity.freq,
            )
        }
        (words + phrases).sortedByDescending { it.frequency }
    }

    override suspend fun queryPrefix(prefix: String): List<InputWord> = withContext(Dispatchers.Default) {
        repository.lookupByPrefix(prefix).map { entity ->
            InputWord.Pinyin(
                text = entity.text,
                spell = Spell(id = entity.spell, value = entity.spell),
                frequency = entity.freq,
            )
        }
    }

    override suspend fun queryLatinCompletions(prefix: String): List<InputWord> {
        return emptyList()
    }

    override suspend fun queryPhraseCompletions(prefix: String): List<InputWord> {
        val spells = db.pinyinWordDao().lookupSpellsByPrefix(prefix)
        val all = spells.flatMap { spell ->
            val words = repository.lookupPinyinWords(spell)
            words.map { entity ->
                InputWord.Pinyin(
                    text = entity.text,
                    spell = Spell(id = entity.spell, value = entity.spell),
                    frequency = entity.freq,
                )
            }
        }
        return all.take(50)
    }

    override suspend fun recordInput(pinyin: String, word: String) {
        repository.recordUserInput(word, "pinyin")
    }
}
