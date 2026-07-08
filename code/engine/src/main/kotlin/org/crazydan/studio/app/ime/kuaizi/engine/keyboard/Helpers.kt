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

package org.crazydan.studio.app.ime.kuaizi.engine.keyboard

import org.crazydan.studio.app.ime.kuaizi.engine.domain.PinyinTree
import org.crazydan.studio.app.ime.kuaizi.engine.input.InputItem

/** 根据 [startChar] 构造其韵母树 */
fun createVowelTree(pinyinTree: PinyinTree, startChar: String): Map<String, List<String>> =
    // children 为声母树列表
    (pinyinTree as PinyinTree.Branch).children[startChar]?.let { node ->
        when (node) {
            is PinyinTree.Leaf -> null
            is PinyinTree.Branch -> {
                // children 为声母节点的韵母树列表：需过滤掉 a、e、o 等单字母拼音
                node.children.filter { it.key != "" }.mapValues { entry ->
                    val child = entry.value // 第一级韵母树
                    when (child) {
                        is PinyinTree.Leaf -> emptyList() // 对应拼音 ou、ao、er 等
                        is PinyinTree.Branch ->
                            // children 为第二级韵母树列表
                            child.children.keys.toList()
                    }
                }
            }
        }
    }
        ?: emptyMap()

/** 创建拼音待输入 */
fun createPinyinInputPending(pinyinTree: PinyinTree, state: KeyboardState.Pinyin.Inputting): InputItem.Pinyin =
    state.getChars().let { chars ->
        InputItem.Pinyin(
            value = chars,
            valid = pinyinTree.isPinyin(chars),
        )
    }
