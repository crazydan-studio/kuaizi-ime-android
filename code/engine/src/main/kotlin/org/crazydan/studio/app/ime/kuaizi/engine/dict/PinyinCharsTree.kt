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
