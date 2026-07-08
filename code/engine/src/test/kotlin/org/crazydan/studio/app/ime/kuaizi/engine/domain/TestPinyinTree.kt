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
import org.crazydan.studio.app.ime.kuaizi.engine.keyboard.createVowelTree
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
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

    @Test
    fun `should verify whether string is a pinyin or not`() = runTest {
        val pinyinList = listOf("a", "bi", "ci", "cu", "shu", "suan", "shuang", "e", "o", "ou")
        val pinyinTree = PinyinTree.Builder().addAll(pinyinList).build()

        for (pinyin in pinyinList) {
            assertTrue(pinyinTree.isPinyin(pinyin))
        }

        assertFalse(pinyinTree.isPinyin("b"))
        assertFalse(pinyinTree.isPinyin("c"))
        assertFalse(pinyinTree.isPinyin("cui"))
        assertFalse(pinyinTree.isPinyin("ei"))
        assertFalse(pinyinTree.isPinyin("su"))
        assertFalse(pinyinTree.isPinyin("shua"))
        assertFalse(pinyinTree.isPinyin("shuan"))
    }

    @Test
    fun `should create correct vowel tree`() = runTest {
        val pinyinList =
            listOf("ao", "ai", "a", "o", "ou", "e", "hua", "hu", "huai", "huang", "chi", "chuan", "chong", "chang")
        val pinyinTree = PinyinTree.Builder().addAll(pinyinList).build()

        val vowelTree1 = createVowelTree(pinyinTree, "a")
        assertEquals(2, vowelTree1.size)
        assertTrue(vowelTree1["o"]?.isEmpty() == true)
        assertTrue(vowelTree1["i"]?.isEmpty() == true)

        val vowelTree2 = createVowelTree(pinyinTree, "o")
        assertEquals(1, vowelTree2.size)
        assertTrue(vowelTree2["u"]?.isEmpty() == true)

        val vowelTree3 = createVowelTree(pinyinTree, "e")
        assertTrue(vowelTree3.isEmpty())

        val vowelTree4 = createVowelTree(pinyinTree, "h")
        assertEquals(1, vowelTree4.size)
        assertEquals(4, vowelTree4["u"]?.size)
        assertEquals("a,,ai,ang", vowelTree4["u"]?.joinToString(","))

        val vowelTree5 = createVowelTree(pinyinTree, "ch")
        assertEquals(4, vowelTree5.size)
        assertTrue(vowelTree5["i"]?.isEmpty() == true)
        assertEquals(1, vowelTree5["u"]?.size)
        assertEquals("an", vowelTree5["u"]?.get(0))
        assertEquals(1, vowelTree5["o"]?.size)
        assertEquals("ng", vowelTree5["o"]?.get(0))
        assertEquals(1, vowelTree5["a"]?.size)
        assertEquals("ng", vowelTree5["a"]?.get(0))
    }
}
