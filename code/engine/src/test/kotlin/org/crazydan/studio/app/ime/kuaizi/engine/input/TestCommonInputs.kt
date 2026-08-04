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

package org.crazydan.studio.app.ime.kuaizi.engine.input

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2026-08-04
 */
class TestCommonInputs {

    @Test
    fun `should update chars of Latin correctly`() {
        try {
            CommonInput.Item.Char.Latin(chars = listOf())
            fail("Can not reach here when 'chars' is empty")
        } catch (e: Exception) {
            assertTrue(e.message!!.contains("can not be empty"))
        }

        var latin = CommonInput.Item.Char.Latin(chars = listOf("a"))
        assertEquals("a", latin.value)

        latin = latin.appendChar("b")
        assertEquals("ab", latin.value)

        latin = latin.appendChar("1")
        assertEquals("ab1", latin.value)

        // -------------------------------
        latin = latin.dropLastChar()
        assertEquals("ab", latin.value)

        latin = latin.dropLastChar()
        assertEquals("a", latin.value)
    }

    @Test
    fun `should do replace for Latin correctly`() {
        var latin = CommonInput.Item.Char.Latin(chars = listOf("a", "b", "C"))
        assertEquals("abC", latin.value)

        latin = latin.appendChar("c", listOf("c", "C"))
        assertEquals("abc", latin.value)

        latin = latin.appendChar("C", listOf("c", "C"))
        assertEquals("abC", latin.value)
    }

    @Test
    fun `should get text with option as expected`() {
        val option = InputTextOption()

        val latin1 = CommonInput.Item.Char.Latin(chars = listOf("a", "b", "c"))
        assertEquals("abc", latin1.getText(option).toString())

        val latin2 = CommonInput.Item.Char.Latin(
            chars = listOf("b", "e", "t", "t", "e", "r"),
            spell = Spell(id = "1", value = "ˈbetər"),
        )
        assertEquals("better", latin2.getText(option).toString())
        assertEquals(
            "better /ˈbetər/",
            latin2.getText(
                option.copy(
                    spellUseMode = InputWord.SpellUseMode.Follow
                )
            ).toString()
        )
        assertEquals(
            "/ˈbetər/",
            latin2.getText(
                option.copy(
                    spellUseMode = InputWord.SpellUseMode.Replace
                )
            ).toString()
        )
    }
}