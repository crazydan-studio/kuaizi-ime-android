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

import org.crazydan.studio.app.ime.kuaizi.engine.domain.MathSymbol
import org.crazydan.studio.app.ime.kuaizi.engine.input.math.MathInput
import org.crazydan.studio.app.ime.kuaizi.engine.input.math.addSignByOp
import org.crazydan.studio.app.ime.kuaizi.engine.input.math.appendChar
import org.crazydan.studio.app.ime.kuaizi.engine.input.math.dropLastChar
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2026-08-04
 */
class TestMathInputs {

    @Test
    fun `should update chars of Number correctly`() {
        try {
            MathInput.Item.Const.Number(chars = listOf())
            fail("Can not reach here when 'chars' is empty")
        } catch (e: Exception) {
            assertTrue(e.message!!.contains("can not be empty"))
        }

        var number = MathInput.Item.Const.Number(chars = listOf('1'))
        assertEquals("1", number.value)
        assertEquals(1.0, number.actual)

        number = number.appendChar('2')
        assertEquals("12", number.value)
        assertEquals(12.0, number.actual)

        // -------------------------------------
        // 添加非数字
        number = number.appendChar('a')
        assertEquals("12", number.value)
        assertEquals(12.0, number.actual)

        // -------------------------------------
        // 添加小数点
        number = number.appendChar(MathSymbol.Dot.value[0])
        assertEquals("12.", number.value)
        assertEquals(12.0, number.actual)

        number = number.appendChar(MathSymbol.Dot.value[0])
        assertEquals("12.", number.value)
        assertEquals(12.0, number.actual)

        // -------------------------------------
        // 追加数字
        number = number.appendChar('3')
        assertEquals("12.3", number.value)
        assertEquals(12.3, number.actual)

        // -------------------------------------
        // 丢弃尾部字符
        number = number.dropLastChar()
        assertEquals("12.", number.value)
        assertEquals(12.0, number.actual)

        number = number.dropLastChar()
        assertEquals("12", number.value)
        assertEquals(12.0, number.actual)
    }

    @Test
    fun `should add sign to Const correctly`() {
        listOf(
            MathInput.Item.Const.Number(chars = listOf('1')),
            MathInput.Item.Const.PI(),
            MathInput.Item.Const.E(),
        ).forEach { item ->
            val raw = item.value

            // ------------------------------------
            var const = item.addSignByOp(MathInput.Item.Op.Plus)
            assertEquals("+$raw", const.value)
            assertTrue(const.actual > 0)

            const = const.addSignByOp(MathInput.Item.Op.Multiply)
            assertEquals(raw, const.value)
            assertTrue(const.actual > 0)

            const = const.addSignByOp(MathInput.Item.Op.Plus)
            assertEquals("+$raw", const.value)
            assertTrue(const.actual > 0)

            const = const.addSignByOp(MathInput.Item.Op.Multiply)
            assertEquals(raw, const.value)
            assertTrue(const.actual > 0)

            // ------------------------------------
            const = const.addSignByOp(MathInput.Item.Op.Minus)
            assertEquals("-$raw", const.value)
            assertTrue(const.actual < 0)

            const = const.addSignByOp(MathInput.Item.Op.Multiply)
            assertEquals(raw, const.value)
            assertTrue(const.actual > 0)

            const = const.addSignByOp(MathInput.Item.Op.Minus)
            assertEquals("-$raw", const.value)
            assertTrue(const.actual < 0)

            const = const.addSignByOp(MathInput.Item.Op.Multiply)
            assertEquals(raw, const.value)
            assertTrue(const.actual > 0)

            // ------------------------------------
            const = const.addSignByOp(MathInput.Item.Op.Plus).addSignByOp(MathInput.Item.Op.Minus)
            assertEquals("-$raw", const.value)
            assertTrue(const.actual < 0)

            const = const.addSignByOp(MathInput.Item.Op.Plus)
            assertEquals("+$raw", const.value)
            assertTrue(const.actual > 0)
        }
    }

    @Test
    fun `should own different close item for different open item`() {
        val b1 = MathInput.Item.Bracket()
        val b2 = MathInput.Item.Bracket()
        assertTrue(b1 !== b2)
        assertTrue(b1.close !== b2.close)

        val f1 = MathInput.Item.Func.Cos()
        val f2 = MathInput.Item.Func.Cos()
        assertTrue(f1 !== f2)
        assertTrue(f1.close !== f2.close)

        val f3 = MathInput.Item.Func.Sin()
        val f4 = MathInput.Item.Func.Sin()
        assertTrue(f3 !== f4)
        assertTrue(f3.close !== f4.close)

        val f5 = MathInput.Item.Func.Sqrt()
        val f6 = MathInput.Item.Func.Sqrt()
        assertTrue(f5 !== f6)
        assertTrue(f5.close !== f6.close)

        val f7 = MathInput.Item.Func.Tan()
        val f8 = MathInput.Item.Func.Tan()
        assertTrue(f7 !== f8)
        assertTrue(f7.close !== f8.close)

        val f9 = MathInput.Item.Func.LogE()
        val f10 = MathInput.Item.Func.LogE()
        assertTrue(f9 !== f10)
        assertTrue(f9.close !== f10.close)
    }
}