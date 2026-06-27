package org.crazydan.studio.app.ime.kuaizi

import org.crazydan.studio.app.ime.kuaizi.engine.domain.InputWord
import org.crazydan.studio.app.ime.kuaizi.engine.dict.ImeDictProvider

class InMemoryDictProvider : ImeDictProvider {
    private val dict = mapOf(
        "shi" to listOf("是", "时", "事", "十"),
        "wo" to listOf("我", "窝", "卧"),
        "ni" to listOf("你", "尼"),
        "ta" to listOf("他", "她", "它"),
        "hao" to listOf("好", "号", "浩"),
        "zhong" to listOf("中", "钟", "种"),
        "guo" to listOf("国", "果", "过"),
        "ren" to listOf("人", "任", "忍"),
        "da" to listOf("大", "达", "打"),
        "xiao" to listOf("小", "晓", "肖"),
    )

    override suspend fun query(pinyin: String): List<InputWord> {
        return dict[pinyin]?.mapIndexed { i, text ->
            InputWord.Pinyin(text = text, spell = null, frequency = 100 - i * 10)
        } ?: emptyList()
    }

    override suspend fun queryPrefix(prefix: String): List<InputWord> {
        return dict.entries.filter { (key, _) ->
            key.startsWith(prefix)
        }.flatMap { (_, values) ->
            values.mapIndexed { i, text ->
                InputWord.Pinyin(text = text, spell = null, frequency = 100 - i * 10)
            }
        }
    }

    override suspend fun queryLatinCompletions(prefix: String): List<InputWord> = emptyList()

    override suspend fun queryPhraseCompletions(prefix: String): List<InputWord> = emptyList()

    override suspend fun recordInput(pinyin: String, word: String) {
        // no-op in memory implementation
    }
}
