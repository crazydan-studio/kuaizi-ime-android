/*
 * 筷字输入法 - 高效编辑需要又好又快的输入法
 * Copyright (C) 2026 Crazydan Studio <https://studio.crazydan.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.
 * If not, see <https://www.gnu.org/licenses/lgpl-3.0.en.html#license-text>.
 */

package org.crazydan.studio.app.ime.kuaizi.engine.dict.provider

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.crazydan.studio.app.ime.kuaizi.engine.dict.DictRepository
import org.crazydan.studio.app.ime.kuaizi.engine.dict.ImeDictProvider
import org.crazydan.studio.app.ime.kuaizi.engine.dict.db.DictDatabase
import org.crazydan.studio.app.ime.kuaizi.engine.domain.FavoriteDao
import org.crazydan.studio.app.ime.kuaizi.engine.domain.FavoriteEntity
import org.crazydan.studio.app.ime.kuaizi.engine.domain.InputWord
import org.crazydan.studio.app.ime.kuaizi.engine.domain.Spell
import org.crazydan.studio.app.ime.kuaizi.engine.domain.Tone
import org.crazydan.studio.app.ime.kuaizi.engine.domain.Variant
import org.crazydan.studio.app.ime.kuaizi.engine.domain.VariantType

/**
 * 基于 SQLite（Room）的字典服务提供者，[ImeDictProvider] 的默认实现。
 *
 * 内部委托 [DictRepository] 完成数据库操作，三层委托职责清晰分离：
 * - [DictRepository]：负责字词查询与用户输入记录
 * - 数据库 DAO：负责 SQL 层面的数据访问
 * - Entity → Domain 映射：负责数据库实体到领域模型的转换
 *
 * @param context Android Context，用于初始化 Room 数据库
 * @param favoriteDao 可选的收藏 DAO，不提供时使用空实现
 */
class SqliteDictProvider(
    context: Context,
    private val favoriteDao: FavoriteDao? = null,
) : ImeDictProvider {
    private val db = DictDatabase.Companion.getInstance(context)
    private val repository = DictRepository(
        wordDao = db.pinyinWordDao(),
        phraseDao = db.pinyinPhraseDao(),
        userInputDao = db.userInputDao(),
        // 未提供收藏 DAO 时使用空实现，避免收藏功能必须依赖
        favoriteDao = favoriteDao ?: object : FavoriteDao {
            override fun getAllFlow(): Flow<List<FavoriteEntity>> {
                TODO("Not yet implemented")
            }

            override suspend fun getAll(): List<FavoriteEntity> {
                TODO("Not yet implemented")
            }

            override suspend fun getByText(text: String): FavoriteEntity? {
                TODO("Not yet implemented")
            }

            override suspend fun upsert(entity: FavoriteEntity) {
                TODO("Not yet implemented")
            }

            override suspend fun delete(text: String) {
                TODO("Not yet implemented")
            }

            override suspend fun clearAll() {
                TODO("Not yet implemented")
            }
        },
        hmmDao = db.hmmDao(),
    )

    /**
     * 根据完整拼音查询候选词，合并单字和词组结果按频率降序排列。
     * 在 [Dispatchers.Default] 上异步执行以避免阻塞主线程。
     */
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

    /** 根据拼音前缀模糊查询候选词，在 IO 线程异步执行。 */
    override suspend fun queryPrefix(prefix: String): List<InputWord> = withContext(Dispatchers.Default) {
        repository.lookupByPrefix(prefix).map { entity ->
            InputWord.Pinyin(
                text = entity.text,
                spell = Spell(id = entity.spell, value = entity.spell),
                frequency = entity.freq,
            )
        }
    }

    /** 查询拉丁词补全建议，将数据库实体映射为 [InputWord.Latin] 领域模型。 */
    override suspend fun queryLatinCompletions(prefix: String): List<InputWord> = withContext(Dispatchers.Default) {
        repository.lookupByPrefix(prefix).map { entity ->
            InputWord.Latin(
                text = entity.text,
                frequency = entity.freq,
            )
        }
    }

    /**
     * 查询拼音词组补全建议。
     * 先通过 DAO 获取匹配前缀的所有拼音拼写，再查询每个拼写对应的单字候选。
     * 结果限制为最多 50 条以避免内存开销过大。
     */
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

    /** 记录用户输入，委托仓库更新用户输入频率。 */
    override suspend fun recordInput(pinyin: String, word: String) {
        repository.recordUserInput(word, "pinyin")
    }
}
