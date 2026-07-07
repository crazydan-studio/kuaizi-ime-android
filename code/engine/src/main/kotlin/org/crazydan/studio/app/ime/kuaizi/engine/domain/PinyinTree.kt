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

/**
 * 拼音树。
 *
 * 将拼音拆分为**声母+韵母首字母+韵母剩余字母**的三层结构，
 * 比如，将 `huang` 拆分为 `h -> u -> ang`，
 * 再按层级以相同字符为分支节点 [Branch] 构造子树，以最终形成拼音树。
 */
sealed class PinyinTree {

    /**
     * 拼音树构建器，提供增量构建能力。
     * 构建过程在引擎初始化时执行一次，从字典中加载所有拼音以构建完整的拼音树。
     */
    class Builder {
        private val branch = Branch(mutableMapOf())

        /** 将列表中的拼音全部添加到 [PinyinTree]。 */
        fun addAll(pinyinList: List<String>): Builder =
            this.apply {
                pinyinList.forEach { add(it) }
            }

        /** 将拼音添加到 [PinyinTree]。 */
        fun add(pinyin: String): Builder {
            val paths = splitPinyin(pinyin)

            // 沿路径构造拼音树分支
            var node = branch
            for (path in paths) {
                node = (node.children as MutableMap).getOrPut(path) {
                    Branch(mutableMapOf())
                } as Branch
            }
            // 标记当前拼音的终点，确保处于拼音树中间分支的有效拼音也能被识别出来
            (node.children as MutableMap).putIfAbsent("", Leaf)

            return this
        }

        /** 构建不可变的 [PinyinTree] 实例。 */
        fun build(): PinyinTree {
            return toImmutable(branch)
        }

        // --------------------------------------------------

        companion object {

            /** 递归构造不可变 [Branch]，并剪去多余的标记拼音终点的分支 */
            private fun toImmutable(branch: Branch): Branch =
                Branch(
                    children = branch.children.mapValues { entry ->
                        val node = entry.value

                        when (node) {
                            is Branch ->
                                // 若当前节点只包含一个拼音终点子节点，则该节点直接作为拼音树的叶子节点
                                if (node.children.size == 1 && node.children[""] != null) {
                                    Leaf
                                } else {
                                    toImmutable(node)
                                }

                            is Leaf -> node
                        }
                    }
                )

            /** 将拼音拆分为三层，再依次添加到 [PinyinTree]。 */
            private fun splitPinyin(pinyin: String): List<String> {
                val nextPathIndex =
                    when {
                        pinyin.length > 1
                                && pinyin[1] == 'h'
                                && pinyin[0].let { it == 'c' || it == 's' || it == 'z' }
                            -> 2

                        else -> 1
                    }

                val paths = mutableListOf<String>()
                paths.add(pinyin.substring(0, nextPathIndex))

                if (pinyin.length > nextPathIndex) {
                    val path = pinyin.substring(nextPathIndex, nextPathIndex + 1)
                    paths.add(path)
                }
                if (pinyin.length > nextPathIndex + 1) {
                    val path = pinyin.substring(nextPathIndex + 1)
                    paths.add(path)
                }

                return paths
            }
        }
    }

    // ------------------------------------------------------------------

    /**
     * 拼音树分支节点
     * @property children `key` 为拼音三层结构的分支字符，`value` 则为该字符对应的子树。
     * 若 `value` 为 [Leaf]，代表该分支为拼音组合的终点，沿该分支组合的字符是一个有效的拼音。
     * 在类似由拼音 `he`、`hei` 中的 `e` 构成的子树中，其 [children]
     * 将包含 `""` 和 `"i"` 两个 `key`，而空字符串 `""` 则专门用于表示其父节点也对应一个有效拼音
     */
    data class Branch(
        val children: Map<String, PinyinTree>,
    ) : PinyinTree() {
    }

    /** 拼音树叶子节点 */
    data object Leaf : PinyinTree() {
    }
}
