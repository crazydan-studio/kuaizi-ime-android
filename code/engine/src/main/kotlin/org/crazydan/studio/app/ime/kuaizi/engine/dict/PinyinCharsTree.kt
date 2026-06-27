package org.crazydan.studio.app.ime.kuaizi.engine.dict

class PinyinCharsTree {
    private val children: MutableMap<Char, PinyinCharsTree> = mutableMapOf()
    val keys: MutableList<String> = mutableListOf()

    fun insert(sequence: String) {
        var current = this
        for (ch in sequence) {
            current = current.children.getOrPut(ch) { PinyinCharsTree() }
        }
    }

    fun find(prefix: String): PinyinCharsTree? {
        var current: PinyinCharsTree? = this
        for (ch in prefix) {
            current = current?.children?.get(ch) ?: return null
        }
        return current
    }

    fun allReachableChars(): Set<Char> = children.keys

    fun allReachableKeys(): Set<String> {
        val result = mutableSetOf<String>()
        result.addAll(keys)
        children.values.forEach { result.addAll(it.allReachableKeys()) }
        return result
    }

    fun clear() {
        children.clear()
        keys.clear()
    }
}
