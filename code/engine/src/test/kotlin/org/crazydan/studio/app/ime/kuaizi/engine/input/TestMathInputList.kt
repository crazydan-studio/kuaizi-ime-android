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

import kotlinx.coroutines.test.runTest
import org.crazydan.studio.app.ime.kuaizi.engine.domain.MathSymbol
import org.crazydan.studio.app.ime.kuaizi.engine.input.math.MathInput
import org.crazydan.studio.app.ime.kuaizi.engine.input.math.MathInputList
import java.text.DecimalFormat
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2026-07-24
 */
class TestMathInputList {

    @Test
    fun `should throw exception when using invalid initial data`() = runTest {
        try {
            MathInputList(inputs = listOf())
            fail("Can not reach here when 'inputs' is empty")
        } catch (e: Exception) {
            assertTrue(e.message!!.contains("can not be empty"))
        }

        try {
            MathInputList(inputs = listOf(MathInput.Item.Dot))
            fail("Can not reach here when the first element of 'inputs' isn't a gap")
        } catch (e: Exception) {
            assertTrue(e.message!!.contains("must be a gap"))
        }

        try {
            MathInputList(cursor = 1)
            fail("Can not reach here when 'cursor' > 0")
        } catch (e: Exception) {
            assertTrue(e.message!!.contains("out of range"))
        }
    }

    @Test
    fun `should always contains at least one input in list`() = runTest {
        val inputList = MathInputList()

        assertTrue(inputList.isEmpty())
        assertEquals(1, inputList.inputs.size)
        assertEquals(0, inputList.cursor)
        assertNull(inputList.pending)
    }

    @Test
    fun `should create expression as expected`() = runTest {
        var inputList = MathInputList()

        listOf(
            MathInput.Item.Const.Number(chars = listOf('1')),
            MathInput.Item.Const.Number(chars = listOf('2')),
            MathInput.Item.Const.Number(chars = listOf('3')),
        ).forEach {
            inputList = inputList.addItem(it)
        }
        assertEquals(2 * 1 - 1, inputList.cursor)
        assertEquals("123", inputList.getText())
        assertEquals("123", inputList.pending!!.value)
        assertEquals("1", (inputList.selected as MathInput.Item).value)

        // ---------------------------------------
        // 向数字多次添加小数点，仅第一次有效
        listOf(
            MathInput.Item.Dot,
            MathInput.Item.Dot,
            MathInput.Item.Const.Number(chars = listOf('4')),
            MathInput.Item.Dot,
        ).forEach {
            inputList = inputList.addItem(it)
        }
        assertEquals(2 * 1 - 1, inputList.cursor)
        assertEquals("123.4", inputList.getText())
        assertEquals("123.4", inputList.pending.value)

        // ---------------------------------------
        // 继续添加新的输入项
        listOf(
            MathInput.Item.Op.Plus,
            MathInput.Item.Const.PI(),
        ).forEach {
            inputList = inputList.addItem(it)
        }
        assertEquals(3 * 2, inputList.cursor)
        assertNull(inputList.pending)
        assertEquals("123.4 + ${MathSymbol.Const.PI.value}", inputList.getText())

        // ---------------------------------------
        // 向数字添加正负号
        val target0 = 2 * 1 - 1
        inputList = inputList.select(target0)
        assertEquals(target0, inputList.cursor)
        assertNull(inputList.pending)

        inputList = inputList.addItem(MathInput.Item.Op.Plus)
        assertEquals(target0 + 1, inputList.cursor)
        assertNull(inputList.pending)
        assertEquals("+123.4", inputList.getText())

        inputList = inputList.select(target0).addItem(MathInput.Item.Op.Plus)
        assertEquals(target0 + 1, inputList.cursor)
        assertNull(inputList.pending)
        assertEquals("123.4", inputList.getText())

        inputList = inputList.select(target0).addItem(MathInput.Item.Op.Minus)
        assertEquals(target0 + 1, inputList.cursor)
        assertNull(inputList.pending)
        assertEquals("-123.4", inputList.getText())

        inputList = inputList.select(target0).addItem(MathInput.Item.Op.Plus)
        assertEquals(target0 + 1, inputList.cursor)
        assertNull(inputList.pending)
        assertEquals("+123.4", inputList.getText())

        inputList = inputList.select(target0).addItem(MathInput.Item.Op.Minus)
        assertEquals(target0 + 1, inputList.cursor)

        inputList = inputList.select(target0).addItem(MathInput.Item.Op.Minus)
        assertEquals(target0 + 1, inputList.cursor)
        assertNull(inputList.pending)
        assertEquals("123.4", inputList.getText())

        // ---------------------------------------
        // 向数字追加数
        inputList = inputList.select(target0).addItem(MathInput.Item.Const.Number(chars = listOf('5')))
        assertEquals(target0, inputList.cursor)
        assertEquals("123.45", inputList.pending!!.value)

        // ---------------------------------------
        // 向数字添加 % 号
        inputList = inputList.addItem(MathInput.Item.Op.Percent)
        assertEquals(target0 + 2 * 1 + 1, inputList.cursor)
        assertNull(inputList.pending)
        assertEquals("123.45% + ${MathSymbol.Const.PI.value}", inputList.getText())

        // ---------------------------------------
        // 向 pi 添加正负号
        val target1 = 2 * 4 - 1
        inputList = inputList.select(target1)
        assertEquals(target1, inputList.cursor)
        assertNull(inputList.pending)

        inputList = inputList.addItem(MathInput.Item.Op.Plus)
        assertEquals(target1 + 1, inputList.cursor)
        assertNull(inputList.pending)
        assertEquals("123.45% + +${MathSymbol.Const.PI.value}", inputList.getText())

        inputList = inputList.select(target1).addItem(MathInput.Item.Op.Minus)
        assertEquals(target1 + 1, inputList.cursor)
        assertNull(inputList.pending)
        assertEquals("123.45% + -${MathSymbol.Const.PI.value}", inputList.getText())

        inputList = inputList.select(target1).addItem(MathInput.Item.Op.Minus)
        assertEquals(target1 + 1, inputList.cursor)
        assertNull(inputList.pending)
        assertEquals("123.45% + ${MathSymbol.Const.PI.value}", inputList.getText())

        // ---------------------------------------
        // 向 pi 添加小数点将无影响
        inputList = inputList.select(target1).addItem(MathInput.Item.Dot)
        assertEquals(target1, inputList.cursor)
        assertNull(inputList.pending)
        assertEquals("123.45% + ${MathSymbol.Const.PI.value}", inputList.getText())

        // ---------------------------------------
        // 对 pi 加 % 号
        inputList = inputList.addItem(MathInput.Item.Op.Percent)
        assertEquals(target1 + 2 * 1 + 1, inputList.cursor)
        assertNull(inputList.pending)
        assertEquals("123.45% + ${MathSymbol.Const.PI.value}%", inputList.getText())

        // ---------------------------------------
        // 将 pi 替换为数字
        inputList = inputList.select(target1).addItem(MathInput.Item.Const.Number(chars = listOf('9')))
        assertEquals(target1, inputList.cursor)
        assertTrue(inputList.selected is MathInput.Item.Const.PI)
        assertEquals("9", inputList.pending!!.value)
        assertEquals("123.45% + 9%", inputList.getText())

        // ---------------------------------------
        // 小数点仅对数字有效
        val target2 = 3 * 2 - 1
        inputList = inputList.select(target2).addItem(MathInput.Item.Dot)
        assertEquals(target2, inputList.cursor)
        assertNull(inputList.pending)
        assertTrue(inputList.selected is MathInput.Item.Op.Plus)
        assertEquals("123.45% + 9%", inputList.getText())

        inputList = inputList.select(0).addItem(MathInput.Item.Dot)
        assertEquals(0, inputList.cursor)
        assertNull(inputList.pending)
        assertTrue(inputList.selected is MathInput.Gap)
        assertEquals("123.45% + 9%", inputList.getText())

        // ---------------------------------------
        // 数字被函数包裹
        val target3 = 2 * 1 - 1
        inputList = inputList.select(target3).addItem(MathInput.Item.Func.Sin())
        assertEquals(target3 + 1, inputList.cursor)
        assertNull(inputList.pending)
        assertTrue(inputList.selected is MathInput.Gap)
        assertEquals("sin( 123.45 )% + 9%", inputList.getText())

        // ---------------------------------------
        // 添加 ^
        val target4 = 2 * 6 - 1
        inputList = inputList.select(target4).addItem(MathInput.Item.Op.Power)
        assertEquals(target4 + 2, inputList.cursor)
        assertNull(inputList.pending)
        assertTrue(inputList.selected is MathInput.Gap)
        assertEquals("sin( 123.45 )% + 9^%", inputList.getText())

        inputList = inputList.addItem(MathInput.Item.Const.Number(chars = listOf('2')))
        assertEquals(target4 + 2 * 1 - 1, inputList.cursor)
        assertEquals("2", (inputList.selected as MathInput.Item).value)
        assertEquals("2", inputList.pending!!.value)
        assertEquals("sin( 123.45 )% + 9^2%", inputList.getText())

        // ---------------------------------------
        // 添加 °
        val target5 = 2 * 2 - 1
        inputList = inputList.select(target5).addItem(MathInput.Item.Op.Degree)
        assertEquals(target5 + 2, inputList.cursor)
        assertNull(inputList.pending)
        assertTrue(inputList.selected is MathInput.Gap)
        assertEquals("sin( 123.45° )% + 9^2%", inputList.getText())
    }

    @Test
    fun `should delete backward as expected`() = runTest {
        //
    }

    @Test
    fun `should eval expression as expected`() = runTest {
        val option = InputTextOption()
        val df = DecimalFormat("#." + "#".repeat(option.mathResultPrecision))

        // ---------------------------------------
        var inputList = MathInputList()

        listOf(
            MathInput.Item.Equal,
            MathInput.Item.Const.Number(chars = listOf('1')),
            MathInput.Item.Op.Plus,
            MathInput.Item.Const.Number(chars = listOf('2')),
        ).forEach {
            inputList = inputList.addItem(it)
        }
        assertEquals(df.format(1.0 + 2.0), inputList.getText(option))

        // ---------------------------------------
        // 带小数点
        inputList = MathInputList()

        listOf(
            MathInput.Item.Equal,
            MathInput.Item.Const.Number(chars = listOf('1')),
            MathInput.Item.Dot,
            MathInput.Item.Const.Number(chars = listOf('2')),
            MathInput.Item.Op.Plus,
            MathInput.Item.Const.Number(chars = listOf('3')),
            MathInput.Item.Dot,
        ).forEach {
            inputList = inputList.addItem(it)
        }
        assertEquals(df.format(1.2 + 3.0), inputList.getText())

        // ---------------------------------------
        // 带百分号
        inputList = MathInputList()

        listOf(
            MathInput.Item.Equal,
            MathInput.Item.Const.Number(chars = listOf('1')),
            MathInput.Item.Const.Number(chars = listOf('0')),
            MathInput.Item.Op.Percent,
            MathInput.Item.Op.Multiply,
            MathInput.Item.Const.Number(chars = listOf('3')),
            MathInput.Item.Dot,
        ).forEach {
            inputList = inputList.addItem(it)
        }
        assertEquals(df.format(10 / 100.0 * 3.0), inputList.getText())

        // 带负号
        inputList = inputList.selectLastItem().addItem(MathInput.Item.Op.Minus)
        assertEquals(df.format(10 / 100.0 * -3.0), inputList.getText())

        // ---------------------------------------
        // 指数
        inputList = MathInputList()

        listOf(
            MathInput.Item.Equal,
            MathInput.Item.Const.Number(chars = listOf('1')),
            MathInput.Item.Const.Number(chars = listOf('0')),
            MathInput.Item.Op.Power,
            MathInput.Item.Const.Number(chars = listOf('2')),
        ).forEach {
            inputList = inputList.addItem(it)
        }
        assertEquals(df.format(10.0.pow(2.0)), inputList.getText())

        // ---------------------------------------
        // 常数
        inputList = MathInputList()

        listOf(
            MathInput.Item.Equal,
            MathInput.Item.Const.PI(),
            MathInput.Item.Op.Divide,
            MathInput.Item.Const.Number(chars = listOf('2')),
        ).forEach {
            inputList = inputList.addItem(it)
        }
        assertEquals(df.format(Math.PI / 2.0), inputList.getText())

        // ---------------------------------------
        // 角度度数：转换为弧度后再计算
        inputList = MathInputList()

        listOf(
            MathInput.Item.Equal,
            MathInput.Item.Const.Number(chars = listOf('9')),
            MathInput.Item.Const.Number(chars = listOf('0')),
            MathInput.Item.Op.Degree,
            MathInput.Item.Op.Multiply,
            MathInput.Item.Const.Number(chars = listOf('2')),
        ).forEach {
            inputList = inputList.addItem(it)
        }
        assertEquals(df.format(Math.toRadians(90.0) * 2.0), inputList.getText())

        // ---------------------------------------
        // 函数：角度
        inputList = MathInputList()

        listOf(
            MathInput.Item.Equal,
            MathInput.Item.Func.Sin(),
            MathInput.Item.Const.Number(chars = listOf('2')),
            MathInput.Item.Const.Number(chars = listOf('0')),
            MathInput.Item.Op.Degree,
            MathInput.Item.Op.Multiply,
            MathInput.Item.Const.Number(chars = listOf('3')),
        ).forEach {
            inputList = inputList.addItem(it)
        }
        assertEquals(df.format(sin(Math.toRadians(20.0) * 3)), inputList.getText())

        // ---------------------------------------
        // 函数：弧度
        inputList = MathInputList()

        listOf(
            MathInput.Item.Equal,
            MathInput.Item.Func.Cos(),
            MathInput.Item.Const.Number(chars = listOf('5')),
            MathInput.Item.Const.Number(chars = listOf('0')),
            MathInput.Item.Op.Plus,
            MathInput.Item.Const.Number(chars = listOf('5')),
        ).forEach {
            inputList = inputList.addItem(it)
        }
        assertEquals(df.format(cos((50.0 + 5.0) * 3.0)), inputList.getText())

        // ---------------------------------------
        // 括号：指数
        inputList = MathInputList()

        listOf(
            MathInput.Item.Equal,
            MathInput.Item.Bracket(),
            MathInput.Item.Const.Number(chars = listOf('1')),
            MathInput.Item.Op.Plus,
            MathInput.Item.Const.Number(chars = listOf('3')),
        ).forEach {
            inputList = inputList.addItem(it)
        }

        inputList = inputList.selectLast()
        listOf(
            MathInput.Item.Op.Power,
            MathInput.Item.Const.Number(chars = listOf('2')),
            MathInput.Item.Op.Power,
            MathInput.Item.Const.Number(chars = listOf('3')),
        ).forEach {
            inputList = inputList.addItem(it)
        }
        assertEquals(df.format((1.0 + 3.0).pow(2.0.pow(3.0))), inputList.getText())

        // ---------------------------------------
        // 括号：百分比
        inputList = MathInputList()

        listOf(
            MathInput.Item.Equal,
            MathInput.Item.Bracket(),
            MathInput.Item.Const.Number(chars = listOf('1')),
            MathInput.Item.Op.Minus,
            MathInput.Item.Const.Number(chars = listOf('3')),
        ).forEach {
            inputList = inputList.addItem(it)
        }

        inputList = inputList.selectLast()
        listOf(
            MathInput.Item.Op.Percent,
            MathInput.Item.Op.Multiply,
            MathInput.Item.Const.Number(chars = listOf('2')),
            MathInput.Item.Const.Number(chars = listOf('3')),
        ).forEach {
            inputList = inputList.addItem(it)
        }
        assertEquals(df.format((1.0 - 3.0) / 100 * 23), inputList.getText())

        // ---------------------------------------
        // 括号：角度度数
        inputList = MathInputList()

        listOf(
            MathInput.Item.Equal,
            MathInput.Item.Bracket(),
            MathInput.Item.Const.Number(chars = listOf('1')),
            MathInput.Item.Const.Number(chars = listOf('0')),
            MathInput.Item.Op.Multiply,
            MathInput.Item.Const.Number(chars = listOf('3')),
        ).forEach {
            inputList = inputList.addItem(it)
        }

        inputList = inputList.selectLast()
        listOf(
            MathInput.Item.Op.Degree,
            MathInput.Item.Op.Multiply,
            MathInput.Item.Const.Number(chars = listOf('2')),
        ).forEach {
            inputList = inputList.addItem(it)
        }
        assertEquals(df.format(Math.toRadians((10.0 * 3.0)) * 2), inputList.getText())
    }

    @Test
    fun `should add gap space as expected`() = runTest {
        var inputList = MathInputList()

        listOf(
            MathInput.Item.Const.Number(chars = listOf('1')),
            MathInput.Item.Op.Percent,
            MathInput.Item.Op.Plus,
            MathInput.Item.Const.Number(chars = listOf('2')),
            MathInput.Item.Op.Permillage,
            MathInput.Item.Op.Plus,
            MathInput.Item.Const.Number(chars = listOf('3')),
            MathInput.Item.Op.Permyriad,
            MathInput.Item.Op.Plus,
            MathInput.Item.Const.Number(chars = listOf('4')),
            MathInput.Item.Op.Percent,
            MathInput.Item.Op.Permillage,
            MathInput.Item.Op.Permyriad,
            MathInput.Item.Op.Plus,
            MathInput.Item.Const.PI(),
            MathInput.Item.Op.Plus,
            MathInput.Item.Const.PI(),
            MathInput.Item.Op.Percent,
            MathInput.Item.Op.Plus,
            MathInput.Item.Const.E(),
            MathInput.Item.Op.Plus,
            MathInput.Item.Const.E(),
            MathInput.Item.Op.Percent,
            MathInput.Item.Op.Plus,
            MathInput.Item.Const.Number(chars = listOf('5')),
            MathInput.Item.Op.Power,
            MathInput.Item.Const.Number(chars = listOf('2')),
            MathInput.Item.Op.Plus,
            MathInput.Item.Const.Number(chars = listOf('5')),
            MathInput.Item.Op.Power,
            MathInput.Item.Const.E(),
            MathInput.Item.Op.Plus,
            MathInput.Item.Const.Number(chars = listOf('5')),
            MathInput.Item.Op.Power,
            MathInput.Item.Const.PI(),
            MathInput.Item.Op.Plus,
            MathInput.Item.Const.Number(chars = listOf('5')),
            MathInput.Item.Op.Power,
            MathInput.Item.Func.Sin(),
            MathInput.Item.Const.Number(chars = listOf('9')),
            MathInput.Item.Const.Number(chars = listOf('0')),
            MathInput.Item.Op.Degree,
        ).forEach {
            inputList = inputList.addItem(it)
        }
        assertEquals("1% + 2‰ + 3‱ + 4%‰‱ + π + π% + e + e% + 5^2 + 5^e + 5^π + 5^sin( 90° )", inputList.getText())

        // -----------------------------------
        // ^ 与 左括号 之间无空格间隔
        inputList = MathInputList()

        listOf(
            MathInput.Item.Const.Number(chars = listOf('2')),
            MathInput.Item.Op.Power,
            MathInput.Item.Bracket(),
            MathInput.Item.Const.Number(chars = listOf('2')),
            MathInput.Item.Op.Plus,
            MathInput.Item.Const.Number(chars = listOf('1')),
        ).forEach {
            inputList = inputList.addItem(it)
        }
        assertEquals("2^( 2 + 1 )", inputList.getText())

        // -----------------------------------
        // 闭括号 和 ^/%/‰/‱/° 之间无空格间隔
        inputList = MathInputList()

        listOf(
            MathInput.Item.Bracket(),
            MathInput.Item.Const.Number(chars = listOf('2')),
            MathInput.Item.Op.Plus,
            MathInput.Item.Const.Number(chars = listOf('1')),
        ).forEach {
            inputList = inputList.addItem(it)
        }

        inputList = inputList.selectLast().addItem(MathInput.Item.Op.Percent)
        assertEquals("( 2 + 1 )%", inputList.getText())

        inputList = inputList.selectLastItem().addItem(MathInput.Item.Op.Permillage)
        assertEquals("( 2 + 1 )‰", inputList.getText())

        inputList = inputList.selectLastItem().addItem(MathInput.Item.Op.Permyriad)
        assertEquals("( 2 + 1 )‱", inputList.getText())

        inputList = inputList.selectLastItem().addItem(MathInput.Item.Op.Degree)
        assertEquals("( 2 + 1 )°", inputList.getText())

        inputList =
            inputList
                .selectLastItem()
                .addItem(MathInput.Item.Op.Power)
                .addItem(MathInput.Item.Const.Number(chars = listOf('2')))
        assertEquals("( 2 + 1 )^2", inputList.getText())

        // -----------------------------------
        // 函数的闭括号 和 ^/%/‰/‱/° 之间无空格间隔
        inputList = MathInputList()

        listOf(
            MathInput.Item.Func.Tan(),
            MathInput.Item.Const.PI(),
            MathInput.Item.Op.Divide,
            MathInput.Item.Const.Number(chars = listOf('2'))
        ).forEach {
            inputList = inputList.addItem(it)
        }

        inputList = inputList.selectLast().addItem(MathInput.Item.Op.Percent)
        assertEquals("tan( π ÷ 2 )%", inputList.getText())

        inputList = inputList.selectLastItem().addItem(MathInput.Item.Op.Permillage)
        assertEquals("tan( π ÷ 2 )‰", inputList.getText())

        inputList = inputList.selectLastItem().addItem(MathInput.Item.Op.Permyriad)
        assertEquals("tan( π ÷ 2 )‱", inputList.getText())

        inputList = inputList.selectLastItem().addItem(MathInput.Item.Op.Degree)
        assertEquals("tan( π ÷ 2 )°", inputList.getText())

        inputList =
            inputList
                .selectLastItem()
                .addItem(MathInput.Item.Op.Power)
                .addItem(MathInput.Item.Const.Number(chars = listOf('2')))
        assertEquals("tan( π ÷ 2 )^2", inputList.getText())
    }
}
