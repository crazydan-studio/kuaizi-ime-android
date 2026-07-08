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

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2026-07-08
 */
class TestInputKey {

    @Test
    fun `should get char self always by calling #getReplacement when replacements is null or empty`() = runTest {
        val char1 = InputKey.Char.Alphabet(value = "v", replacements = null)
        for (i in -2..4) {
            assertEquals(char1.value, char1.getReplacement(i))
        }

        val char2 = InputKey.Char.Alphabet(value = "v", replacements = emptyList())
        for (i in -2..4) {
            assertEquals(char2.value, char2.getReplacement(i))
        }
    }

    @Test
    fun `should get cycling replacement`() = runTest {
        val char1 = InputKey.Char.Alphabet(value = "v", replacements = listOf("v1"))
        for (i in -2..0) {
            assertEquals(char1.value, char1.getReplacement(i))
        }

        val expects1 = listOf("v1", "v", "v1", "v", "v1", "v")
        for (i in 1..expects1.size) {
            assertEquals(expects1[(i - 1) % expects1.size], char1.getReplacement(i))
        }

        // ------------------------------
        val char2 = InputKey.Char.Alphabet(value = "v", replacements = listOf("v1", "v2", "v3"))
        for (i in -2..0) {
            assertEquals(char2.value, char2.getReplacement(i))
        }

        val expects2 = listOf("v1", "v2", "v3", "v", "v1", "v2", "v3", "v", "v1", "v2", "v3", "v")
        for (i in 1..expects2.size) {
            assertEquals(expects2[(i - 1) % expects2.size], char2.getReplacement(i))
        }
    }
}