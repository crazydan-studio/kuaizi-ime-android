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

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2026-07-07
 */
class TestPinyinTree {

    @Test
    fun `should create only one branch with same starting char`() = runTest {
        val pinyinList = listOf("a", "an", "ai", "ao")
        val pinyinTree = PinyinTree.Builder().addAll(pinyinList).build()

        assertTrue(pinyinTree is PinyinTree.Branch)
        assertEquals(1, pinyinTree.children.size)

        // ------------------------------------
        val nodeA = pinyinTree.children["a"]
        assertTrue(nodeA is PinyinTree.Branch)
        assertEquals(pinyinList.size, nodeA.children.size)

        assertTrue(nodeA.children[""] is PinyinTree.Leaf)
        assertTrue(nodeA.children["i"] is PinyinTree.Leaf)
        assertTrue(nodeA.children["o"] is PinyinTree.Leaf)
    }

    @Test
    fun `should create multiple branches with different starting chars`() = runTest {
        val pinyinList = listOf("hei", "he", "yan", "ya", "wu", "wo")
        val pinyinTree = PinyinTree.Builder().addAll(pinyinList).build()

        assertTrue(pinyinTree is PinyinTree.Branch)
        assertEquals(3, pinyinTree.children.size)

        // ------------------------------------
        val nodeH = pinyinTree.children["h"]
        assertTrue(nodeH is PinyinTree.Branch)
        assertEquals(1, nodeH.children.size)

        val nodeHE = nodeH.children["e"]
        assertTrue(nodeHE is PinyinTree.Branch)
        assertTrue(nodeHE.children[""] is PinyinTree.Leaf)
        assertTrue(nodeHE.children["i"] is PinyinTree.Leaf)

        // ------------------------------------
        val nodeY = pinyinTree.children["y"]
        assertTrue(nodeY is PinyinTree.Branch)
        assertEquals(1, nodeY.children.size)

        val nodeYA = nodeY.children["a"]
        assertTrue(nodeYA is PinyinTree.Branch)
        assertTrue(nodeYA.children[""] is PinyinTree.Leaf)
        assertTrue(nodeYA.children["n"] is PinyinTree.Leaf)

        // ------------------------------------
        val nodeW = pinyinTree.children["w"]
        assertTrue(nodeW is PinyinTree.Branch)
        assertEquals(2, nodeW.children.size)

        assertTrue(nodeW.children["o"] is PinyinTree.Leaf)
        assertTrue(nodeW.children["u"] is PinyinTree.Leaf)
    }

    @Test
    fun `should hasn't middle leaf with one pinyin`() = runTest {
        val pinyinList = listOf("han")
        val pinyinTree = PinyinTree.Builder().addAll(pinyinList).build()

        assertTrue(pinyinTree is PinyinTree.Branch)
        assertEquals(1, pinyinTree.children.size)

        // ------------------------------------
        val nodeH = pinyinTree.children["h"]
        assertTrue(nodeH is PinyinTree.Branch)
        assertEquals(1, nodeH.children.size)

        val nodeHA = nodeH.children["a"]
        assertTrue(nodeHA is PinyinTree.Branch)
        assertEquals(1, nodeHA.children.size)
        assertTrue(nodeHA.children["n"] is PinyinTree.Leaf)
    }

    @Test
    fun `should only three branches at most in long char pinyin`() = runTest {
        val pinyinList = listOf("huang", "huan")
        val pinyinTree = PinyinTree.Builder().addAll(pinyinList).build()

        assertTrue(pinyinTree is PinyinTree.Branch)
        assertEquals(1, pinyinTree.children.size)

        // ------------------------------------
        val nodeH = pinyinTree.children["h"]
        assertTrue(nodeH is PinyinTree.Branch)
        assertEquals(1, nodeH.children.size)

        val nodeHU = nodeH.children["u"]
        assertTrue(nodeHU is PinyinTree.Branch)
        assertEquals(2, nodeHU.children.size)
        assertTrue(nodeHU.children["an"] is PinyinTree.Leaf)
        assertTrue(nodeHU.children["ang"] is PinyinTree.Leaf)
    }

    @Test
    fun `should create branches as 'ch', 'zh', 'sh'`() = runTest {
        val pinyinList = listOf("ci", "chi", "zhe", "za", "sha", "se")
        val pinyinTree = PinyinTree.Builder().addAll(pinyinList).build()

        assertTrue(pinyinTree is PinyinTree.Branch)
        assertEquals(pinyinList.size, pinyinTree.children.size)

        // ------------------------------------
        val nodeC = pinyinTree.children["c"]
        assertTrue(nodeC is PinyinTree.Branch)
        assertEquals(1, nodeC.children.size)
        assertTrue(nodeC.children["i"] is PinyinTree.Leaf)

        val nodeCH = pinyinTree.children["ch"]
        assertTrue(nodeCH is PinyinTree.Branch)
        assertEquals(1, nodeCH.children.size)
        assertTrue(nodeCH.children["i"] is PinyinTree.Leaf)

        // ------------------------------------
        val nodeZ = pinyinTree.children["z"]
        assertTrue(nodeZ is PinyinTree.Branch)
        assertEquals(1, nodeZ.children.size)
        assertTrue(nodeZ.children["a"] is PinyinTree.Leaf)

        val nodeZH = pinyinTree.children["zh"]
        assertTrue(nodeZH is PinyinTree.Branch)
        assertEquals(1, nodeZH.children.size)
        assertTrue(nodeZH.children["e"] is PinyinTree.Leaf)

        // ------------------------------------
        val nodeS = pinyinTree.children["s"]
        assertTrue(nodeS is PinyinTree.Branch)
        assertEquals(1, nodeS.children.size)
        assertTrue(nodeS.children["e"] is PinyinTree.Leaf)

        val nodeSH = pinyinTree.children["sh"]
        assertTrue(nodeSH is PinyinTree.Branch)
        assertEquals(1, nodeSH.children.size)
        assertTrue(nodeSH.children["a"] is PinyinTree.Leaf)
    }
}
