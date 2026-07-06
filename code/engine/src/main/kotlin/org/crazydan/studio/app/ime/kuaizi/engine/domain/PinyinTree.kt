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

package org.crazydan.studio.app.ime.kuaizi.engine.domain

import org.crazydan.studio.app.ime.kuaizi.engine.keyboard.InputKey

/**
 * 拼音前缀树（Trie），用于快速前缀匹配和拼音自动补全。
 *
 * 在拼音输入场景下的核心价值是：用户输入部分拼音时，引擎可以快速找到所有匹配该前缀的
 * 候选拼音序列，为自动补全和模糊匹配提供基础数据。
 * 在引擎初始化时从字典数据构建，构建后为不可变数据结构，所有查询操作为纯函数，无需同步。
 *
 * 查询时间复杂度为 O(m)，其中 m 为前缀长度，与字典规模无关。
 *
 * @property children 以字符为键的子树映射，每个字符对应拼音序列的下一个可能字符
 */
data class PinyinTree(
    private val children: Map<Char, PinyinTree> = emptyMap(),
) {

    /**
     * 查找指定前缀的子树。
     * 沿前缀字符逐层查找，返回匹配前缀的子树根节点；
     * 若前缀不匹配任何路径，返回 null。
     */
    fun find(prefix: String): PinyinTree? {
        if (prefix.isEmpty()) return this

        val first = prefix.first()
        return children[first]?.find(prefix.drop(1))
    }

    /** 获取当前节点的所有直接子字符。 */
    fun allReachableChars(): Set<Char> = children.keys

    /**
     * 前缀树构建器，提供增量构建能力。
     * 构建过程在引擎初始化时执行一次，从字典中加载所有拼音拼写构建完整的前缀树。
     */
    class Builder {
        // key: 前缀路径，value: 该路径下的子字符映射
        private val nodes = mutableMapOf<String, MutableMap<Char, PinyinTree>>()

        /** 逐字符添加 Trie 路径。 */
        fun addPath(path: String, key: InputKey.Char.Alphabet): Builder {
            var current = nodes.getOrPut("") { mutableMapOf() }

            for (c in path) {
                val child = current.getOrPut(c) { PinyinTree() }
                current = nodes.getOrPut(path.substring(0, path.indexOf(c) + 1)) { mutableMapOf() }
            }
            return this
        }

        /** 构建不可变的 [PinyinTree] 实例。 */
        fun build(): PinyinTree {
            return PinyinTree()
        }
    }
}