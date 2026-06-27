package org.crazydan.studio.app.ime.kuaizi.engine.dict

import org.crazydan.studio.app.ime.kuaizi.engine.domain.InputKey

data class PinyinCharsTree(
    val children: Map<Char, PinyinCharsTree> = emptyMap(),
    val keys: List<InputKey.Char> = emptyList(),
) {
    fun find(prefix: String): PinyinCharsTree? {
        if (prefix.isEmpty()) return this
        val first = prefix.first()
        return children[first]?.find(prefix.drop(1))
    }

    fun allReachableChars(): Set<Char> = children.keys

    fun allReachableKeys(): List<InputKey.Char> {
        val result = mutableListOf<InputKey.Char>()
        result.addAll(keys)
        for (child in children.values) {
            result.addAll(child.allReachableKeys())
        }
        return result
    }

    class Builder {
        private val nodes = mutableMapOf<String, MutableMap<Char, PinyinCharsTree>>()

        fun addPath(path: String, key: InputKey.Char): Builder {
            var current = nodes.getOrPut("") { mutableMapOf() }
            for (c in path) {
                val child = current.getOrPut(c) { PinyinCharsTree() }
                current = nodes.getOrPut(path.substring(0, path.indexOf(c) + 1)) { mutableMapOf() }
            }
            return this
        }

        fun build(): PinyinCharsTree {
            return PinyinCharsTree()
        }
    }
}
