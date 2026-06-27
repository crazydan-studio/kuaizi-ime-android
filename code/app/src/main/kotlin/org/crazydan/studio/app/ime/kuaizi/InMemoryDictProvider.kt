package org.crazydan.studio.app.ime.kuaizi

import org.crazydan.studio.app.ime.kuaizi.engine.domain.CandidateList
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

    private val phraseDict = mapOf(
        listOf("shi", "jie") to listOf("世界"),
        listOf("zhong", "guo") to listOf("中国"),
        listOf("ren", "min") to listOf("人民"),
        listOf("da", "jia") to listOf("大家"),
        listOf("hao", "hao") to listOf("好好"),
        listOf("wo", "men") to listOf("我们"),
        listOf("ni", "men") to listOf("你们"),
        listOf("ta", "men") to listOf("他们"),
    )

    override fun query(pinyin: String): CandidateList {
        val words = dict[pinyin]?.mapIndexed { i, text ->
            InputWord.Pinyin(text = text, spell = pinyin, frequency = 100 - i * 10)
        } ?: emptyList()

        return CandidateList(
            candidates = words,
            totalApprox = words.size,
        )
    }

    override fun queryPrefix(prefix: String): CandidateList {
        val words = dict.entries.filter { (key, _) ->
            key.startsWith(prefix)
        }.flatMap { (_, values) ->
            values.mapIndexed { i, text ->
                InputWord.Pinyin(text = text, spell = prefix, frequency = 100 - i * 10)
            }
        }

        return CandidateList(
            candidates = words,
            totalApprox = words.size,
        )
    }

    override fun recordInput(pinyin: String, word: String) {
        // no-op in memory implementation
    }
}
