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

import org.crazydan.studio.app.ime.kuaizi.engine.input.math.MathInput
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2026-07-21
 */
class TestInputList {

    @Test
    fun `should throw exception when using invalid initial data`() {
        try {
            InputList(inputs = listOf())
            fail("Can not reach here when 'inputs' is empty")
        } catch (e: Exception) {
            assertTrue(e.message!!.contains("can not be empty"))
        }

        try {
            InputList(inputs = listOf(CommonInput.Item.Char.Space))
            fail("Can not reach here when the first element of 'inputs' isn't a gap")
        } catch (e: Exception) {
            assertTrue(e.message!!.contains("must be a gap"))
        }

        try {
            InputList(cursor = 1)
            fail("Can not reach here when 'cursor' > 0")
        } catch (e: Exception) {
            assertTrue(e.message!!.contains("out of range"))
        }
    }

    @Test
    fun `should always contains at least one input in list`() {
        val inputList = InputList()

        assertTrue(inputList.isEmpty())
        assertEquals(1, inputList.inputs.size)
        assertEquals(0, inputList.cursor)
        assertNull(inputList.pending)
    }

    @Test
    fun `should add item as expected`() {
        var inputList = InputList()

        // ------------------------------------------
        // 添加算术表达式：输入列表无变更
        inputList = inputList.addItem(CommonInput.Item.MathExpr())
        assertEquals(1, inputList.inputs.size)
        assertNull(inputList.pending)
        assertEquals(0, inputList.cursor)

        // ------------------------------------------
        // 添加拼音：整体替换
        val pyIndex = 2 * 1 - 1
        val py01 = CommonInput.Item.Pinyin(value = "y", valid = false)
        inputList = inputList.addItem(py01)
        assertEquals(pyIndex, inputList.cursor)
        assertEquals(py01, inputList.pending)
        assertEquals(py01, inputList.selected)

        val py02 = CommonInput.Item.Pinyin(value = "yu", valid = true)
        inputList = inputList.addItem(py02)
        assertEquals(pyIndex, inputList.cursor)
        assertEquals(py02, inputList.pending)
        assertEquals(py01, inputList.selected)

        val py03 = CommonInput.Item.Pinyin(value = "yuan", valid = true)
        inputList = inputList.addItem(py03)
        assertEquals(pyIndex, inputList.cursor)
        assertEquals(py03, inputList.pending)
        assertEquals(py01, inputList.selected)

        inputList = inputList.confirmPending()
        assertEquals(pyIndex + 1, inputList.cursor)
        assertNull(inputList.pending)
        assertEquals(CommonInput.Gap, inputList.selected)
        assertEquals(py03, inputList.inputs[inputList.cursor - 1])

        // ------------------------------------------
        // 添加拉丁文：追加字符
        val latinIndex = 2 * 2 - 1
        val ch01 = CommonInput.Item.Char.Latin(chars = listOf("e"))
        inputList = inputList.addItem(ch01)
        assertEquals(latinIndex, inputList.cursor)
        assertEquals(ch01, inputList.pending)
        assertEquals(ch01, inputList.selected)

        val ch02 = CommonInput.Item.Char.Latin(chars = listOf("E"))
        inputList = inputList.addItem(ch02, listOf("e", "E"))
        assertEquals(latinIndex, inputList.cursor)
        assertEquals(ch02, inputList.pending)
        assertEquals(ch01, inputList.selected)

        val ch03 = CommonInput.Item.Char.Latin(chars = listOf("n"))
        inputList = inputList.addItem(ch03)
        assertEquals(latinIndex, inputList.cursor)
        assertEquals("En", inputList.pending!!.value)
        assertEquals(ch01, inputList.selected)

        val ch04 = CommonInput.Item.Char.Latin(chars = listOf("g"))
        inputList = inputList.addItem(ch04)
        assertEquals(latinIndex, inputList.cursor)
        assertEquals("Eng", inputList.pending!!.value)
        assertEquals(ch01, inputList.selected)

        val ch05 = CommonInput.Item.Char.Latin(chars = listOf("G"))
        inputList = inputList.addItem(ch05, listOf("g", "G"))
        assertEquals(latinIndex, inputList.cursor)
        assertEquals("EnG", inputList.pending!!.value)
        assertEquals(ch01, inputList.selected)

        // ------------------------------------------
        // 添加非拉丁文字符
        val ch11 = CommonInput.Item.Char.Symbol(value = "!")
        inputList = inputList.addItem(ch11)
        assertEquals(2 * 3, inputList.cursor)
        assertNull(inputList.pending)
        assertEquals(CommonInput.Gap, inputList.selected)
        assertEquals("EnG", (inputList.inputs[inputList.cursor - 2 * 1 - 1] as CommonInput.Item).value)
        assertEquals("!", (inputList.inputs[inputList.cursor - 1] as CommonInput.Item).value)

        val ch12 = CommonInput.Item.Char.Symbol(value = "！")
        inputList = inputList.addItem(ch12, listOf("!", "！"))
        assertEquals(2 * 3, inputList.cursor)
        assertNull(inputList.pending)
        assertEquals(CommonInput.Gap, inputList.selected)
        assertEquals("！", (inputList.inputs[inputList.cursor - 1] as CommonInput.Item).value)

        val ch13 = CommonInput.Item.Char.Symbol(value = ",")
        inputList = inputList.addItem(ch13)
        assertEquals(2 * 4, inputList.cursor)
        assertNull(inputList.pending)
        assertEquals(CommonInput.Gap, inputList.selected)
        assertEquals(",", (inputList.inputs[inputList.cursor - 1] as CommonInput.Item).value)

        // ------------------------------------------
        // 添加配对符号
        val openIndex = pyIndex
        val closeIndex = openIndex + 2 * 2
        val pair01 = CommonInput.Item.Char.Symbol(
            value = "(",
            close = CommonInput.Item.Char.Symbol(value = ")"),
        )
        inputList = inputList.selectAt(pyIndex).addItem(pair01)
        assertEquals(pair01.value, (inputList.inputs[openIndex] as CommonInput.Item).value)
        assertEquals(py03, inputList.inputs[openIndex + 2 * 1])
        assertEquals(pair01.close!!.value, (inputList.inputs[closeIndex] as CommonInput.Item).value)

        // 选中开符号，再替换
        val pair02 = CommonInput.Item.Char.Symbol(
            value = "[",
            close = CommonInput.Item.Char.Symbol(value = "]"),
        )
        inputList = inputList.selectAt(openIndex).addItem(pair02)
        assertEquals(pair02.value, (inputList.inputs[openIndex] as CommonInput.Item).value)
        assertEquals(py03, inputList.inputs[openIndex + 2 * 1])
        assertEquals(pair02.close!!.value, (inputList.inputs[closeIndex] as CommonInput.Item).value)

        // 选中闭合符号，再替换
        inputList = inputList.selectAt(closeIndex).addItem(pair01)
        assertEquals(pair01.value, (inputList.inputs[openIndex] as CommonInput.Item).value)
        assertEquals(py03, inputList.inputs[openIndex + 2 * 1])
        assertEquals(pair01.close.value, (inputList.inputs[closeIndex] as CommonInput.Item).value)

        // ------------------------------------------
        // 选中配对符号，再输入
        inputList = inputList.selectAt(closeIndex).addItem(CommonInput.Item.Char.Symbol(value = "?"))
        assertEquals("?", (inputList.inputs[closeIndex + 2 * 1] as CommonInput.Item).value)

        val py20 = CommonInput.Item.Pinyin(value = "q", valid = false)
        inputList = inputList.selectAt(closeIndex).addItem(py20)
        assertEquals(closeIndex + 2 * 1, inputList.cursor)
        assertEquals(py20, inputList.pending!!)
        assertEquals(py20, inputList.selected)

        val ch20 = CommonInput.Item.Char.Latin(chars = listOf("a"))
        inputList = inputList.selectAt(closeIndex).addItem(ch20)
        assertEquals(closeIndex + 2 * 1, inputList.cursor)
        assertEquals(ch20, inputList.pending!!)
        assertEquals(ch20, inputList.selected)

        // ------------------------------------------
        // 对 pending 加配对符号
        inputList = InputList()
        val zeroIndex = 0

        // 无效拼音不占位
        val py30 = CommonInput.Item.Pinyin(value = "q", valid = false)
        inputList = inputList.selectAt(zeroIndex).addItem(py30).addItem(pair01)
        assertEquals(zeroIndex + 2 * 1, inputList.cursor)
        assertNull(inputList.pending)
        assertEquals(pair01.value, (inputList.inputs[zeroIndex + 2 * 1 - 1] as CommonInput.Item).value)
        assertEquals(CommonInput.Gap, inputList.selected)
        assertEquals(pair01.close.value, (inputList.inputs[inputList.cursor + 2 * 1 - 1] as CommonInput.Item).value)

        // 有效拼音有占位
        inputList = InputList()
        val py31 = CommonInput.Item.Pinyin(value = "qu", valid = true)
        inputList = inputList.addItem(py31).addItem(pair01)
        assertEquals(2 * 2, inputList.cursor)
        assertNull(inputList.pending)
        assertEquals(pair01.value, (inputList.inputs[2 * 1 - 1] as CommonInput.Item).value)
        assertEquals(py31, inputList.inputs[inputList.cursor - 1])
        assertEquals(pair01.close.value, (inputList.inputs[inputList.cursor + 1] as CommonInput.Item).value)

        // 拉丁字符
        inputList = InputList()
        val ch30 = CommonInput.Item.Char.Latin(chars = listOf("e"))
        inputList = inputList.addItem(ch30).addItem(pair01)
        assertEquals(2 * 2, inputList.cursor)
        assertNull(inputList.pending)
        assertEquals(pair01.value, (inputList.inputs[2 * 1 - 1] as CommonInput.Item).value)
        assertEquals(ch30, inputList.inputs[inputList.cursor - 1])
        assertEquals(pair01.close.value, (inputList.inputs[inputList.cursor + 1] as CommonInput.Item).value)
    }

    @Test
    fun `should update math expr as expected`() {
        var inputList = InputList()

        inputList = inputList.withMathExprUpdate {
            addItem(MathInput.Item.Const.Number(chars = listOf('1')))
                .addItem(MathInput.Item.Op.Plus)
                .addItem(MathInput.Item.Const.Number(chars = listOf('2')))
                .addItem(MathInput.Item.Op.Multiply)
                .addItem(MathInput.Item.Bracket())
                .addItem(MathInput.Item.Const.Number(chars = listOf('3')))
                .addItem(MathInput.Item.Op.Minus)
                .addItem(MathInput.Item.Const.Number(chars = listOf('5')))
        }.confirmPending()

        assertEquals("1 + 2 × ( 3 - 5 )", inputList.getText().toString())
    }

    @Test
    fun `should confirm pending as expected`() {
        var inputList = InputList()

        // -----------------------------------
        // 确认 gap：gap 处不会有 pending
        assertEquals(CommonInput.Gap, inputList.selected)
        inputList = inputList.confirmPending()
        assertEquals(1, inputList.inputs.size)
        assertNull(inputList.pending)
        assertEquals(CommonInput.Gap, inputList.selected)

        // -----------------------------------
        // 添加并确认无效拼音 pending：直接丢弃
        val py01 = CommonInput.Item.Pinyin(value = "y", valid = false)
        inputList = inputList.addItem(py01).confirmPending()
        assertNull(inputList.pending)
        assertEquals(CommonInput.Gap, inputList.selected)
        assertEquals(1, inputList.inputs.size)

        // 对选中的非拼音输入添加无效拼音：pending 丢弃，且选中输入不被修改
        val ch01 = CommonInput.Item.Char.Symbol(value = ",")
        inputList =
            inputList
                .addItem(ch01)
                .addItem(py01)
                .confirmPending()
        assertNull(inputList.pending)
        assertEquals(CommonInput.Gap, inputList.selected)
        assertEquals(ch01, inputList.inputs[inputList.cursor - 1])

        // 添加并确认有效拼音 pending
        val py11 = CommonInput.Item.Pinyin(value = "yu", valid = true)
        inputList = inputList.addItem(py11).confirmPending()
        assertNull(inputList.pending)
        assertEquals(CommonInput.Gap, inputList.selected)
        assertEquals(py11, inputList.inputs[inputList.cursor - 1])

        // 对选中的拼音输入项添加无效拼音：pending 丢弃
        inputList = inputList.selectAt(inputList.cursor - 1).addItem(py01).confirmPending()
        assertNull(inputList.pending)
        assertEquals(CommonInput.Gap, inputList.selected)
        assertEquals(py11, inputList.inputs[inputList.cursor - 1])

        // -----------------------------------
        // 确认 null pending
        inputList = inputList.selectAt(2 * 1 - 1)
        assertNull(inputList.pending)
        assertEquals(ch01, inputList.selected)

        inputList = inputList.confirmPending()
        assertNull(inputList.pending)
        assertEquals(CommonInput.Gap, inputList.selected)
        assertEquals(ch01, inputList.inputs[inputList.cursor - 1])

        // ------------------------------------
        // 确认 latin pending
        val ch11 = CommonInput.Item.Char.Latin(chars = listOf("a"))
        inputList = inputList.addItem(ch11).confirmPending()
        assertNull(inputList.pending)
        assertEquals(CommonInput.Gap, inputList.selected)
        assertEquals(ch11, inputList.inputs[inputList.cursor - 1])

        // 选中输入并替换为 latin pending
        val ch12 = CommonInput.Item.Char.Latin(chars = listOf("b"))
        inputList = inputList.selectAt(2 * 1 - 1).addItem(ch12).confirmPending()
        assertNull(inputList.pending)
        assertEquals(CommonInput.Gap, inputList.selected)
        assertEquals(ch12, inputList.inputs[inputList.cursor - 1])

        // -------------------------------------
        // 确认空算术输入 pending：直接删除空 pending
        inputList = inputList.selectAt(0).withMathExprUpdate { this }
        assertTrue(inputList.pending is CommonInput.Item.MathExpr)
        assertTrue(inputList.pending.inputList.isEmpty())
        assertEquals(inputList.pending, inputList.selected)

        inputList = inputList.confirmPending()
        assertEquals(0, inputList.cursor)
        assertNull(inputList.pending)
        assertEquals(CommonInput.Gap, inputList.selected)

        // 确认非空算术输入 pending
        inputList = inputList.withMathExprUpdate {
            addItem(MathInput.Item.Const.PI())
                .addItem(MathInput.Item.Op.Plus)
                .addItem(MathInput.Item.Const.Number(chars = listOf('1')))
        }
        assertTrue(inputList.pending is CommonInput.Item.MathExpr)
        assertFalse(inputList.pending.inputList.isEmpty())
        assertTrue(inputList.selected is CommonInput.Item.MathExpr)
        assertTrue((inputList.selected as CommonInput.Item.MathExpr).inputList.isEmpty())

        inputList = inputList.confirmPending()
        assertNull(inputList.pending)
        val expr01 = inputList.inputs[inputList.cursor - 1]
        assertTrue(expr01 is CommonInput.Item.MathExpr)
        assertEquals("π + 1", expr01.inputList.getText().toString())
    }

    @Test
    fun `should delete backward as expected`() {
        var inputList = InputList()

        // ---------------------------------------
        // 对头部 gap 做删除无影响
        inputList = inputList.deleteBackward()
        assertEquals(0, inputList.cursor)
        assertNull(inputList.pending)
        assertEquals(CommonInput.Gap, inputList.selected)

        // ---------------------------------------
        // 逐字符删除 latin
        listOf(
            CommonInput.Item.Char.Latin(chars = listOf("a")),
            CommonInput.Item.Char.Latin(chars = listOf("b")),
            CommonInput.Item.Char.Latin(chars = listOf("c")),
        ).forEach {
            inputList = inputList.addItem(it)
        }
        inputList = inputList.confirmPending()

        assertEquals(2 * 1, inputList.cursor)
        assertNull(inputList.pending)
        assertEquals(CommonInput.Gap, inputList.selected)

        // 仅选中 latin
        inputList = inputList.deleteBackward()
        assertEquals(2 * 1 - 1, inputList.cursor)
        assertNull(inputList.pending)
        assertEquals("abc", (inputList.selected as CommonInput.Item.Char).value)

        // 删除尾部字符
        inputList = inputList.deleteBackward()
        assertEquals(2 * 1 - 1, inputList.cursor)
        assertEquals("ab", (inputList.pending as CommonInput.Item.Char).value)
        assertEquals("abc", (inputList.selected as CommonInput.Item.Char).value)

        // 连续删除整个 latin
        inputList = inputList.deleteBackward().deleteBackward()
        assertTrue(inputList.isEmpty())
        assertEquals(0, inputList.cursor)
        assertNull(inputList.pending)
        assertEquals(CommonInput.Gap, inputList.selected)

        // ---------------------------------------------
        // 对算术输入做整体直接删除
        inputList = inputList.withMathExprUpdate {
            addItem(MathInput.Item.Const.PI())
        }.confirmPending()

        assertNull(inputList.pending)
        assertEquals(CommonInput.Gap, inputList.selected)
        assertEquals("π", inputList.getText().toString())

        inputList = inputList.deleteBackward()
        assertTrue(inputList.isEmpty())
        assertEquals(0, inputList.cursor)
        assertNull(inputList.pending)
        assertEquals(CommonInput.Gap, inputList.selected)

        // ----------------------------------------
        // 删除配对符号
        val pair01 = CommonInput.Item.Char.Symbol(value = "(", close = CommonInput.Item.Char.Symbol(value = ")"))
        listOf(
            pair01,
            CommonInput.Item.Char.Symbol(value = "！"),
        ).forEach {
            inputList = inputList.addItem(it)
        }
        assertEquals("(！)", inputList.getText().toString())

        inputList = inputList.selectAt(2 * 1 - 1).deleteBackward()
        assertEquals(0, inputList.cursor)
        assertNull(inputList.pending)
        assertEquals(CommonInput.Gap, inputList.selected)
        assertEquals("！", inputList.getText().toString())

        inputList = inputList.selectAt(2 * 1 - 1).addItem(pair01).selectLast()
        inputList = inputList.deleteBackward()
        assertEquals(2 * 1, inputList.cursor)
        assertNull(inputList.pending)
        assertEquals(CommonInput.Gap, inputList.selected)
        assertEquals("！", inputList.getText().toString())

        inputList = inputList.selectAt(2 * 1 - 1).deleteBackward()
        assertTrue(inputList.isEmpty())
        assertEquals(0, inputList.cursor)
        assertNull(inputList.pending)
        assertEquals(CommonInput.Gap, inputList.selected)
    }

    @Test
    fun `should add gap space as expected`() {
        var inputList = InputList()

        // -------------------------------------------------
        val pair01 = CommonInput.Item.Char.Symbol(value = "(", close = CommonInput.Item.Char.Symbol(value = ")"))
        listOf(
            CommonInput.Item.Char.Symbol(value = "，", fullWidth = true),
            CommonInput.Item.Char.Symbol(value = "<"),
            CommonInput.Item.Char.Latin(chars = listOf("1")),
            CommonInput.Item.Char.Latin(chars = listOf("2")),
            pair01,
            CommonInput.Item.Char.Latin(chars = listOf("a")),
            CommonInput.Item.Char.Latin(chars = listOf("b")),
            CommonInput.Item.Char.Symbol(value = ":"),
            CommonInput.Item.Char.Latin(chars = listOf("c")),
            CommonInput.Item.Char.Latin(chars = listOf("d")),
            CommonInput.Item.Char.Symbol(value = "、", fullWidth = true),
            CommonInput.Item.Char.Symbol(value = ","),
            CommonInput.Item.Char.Symbol(value = "：", fullWidth = true),
            CommonInput.Item.Char.Latin(chars = listOf("e")),
            CommonInput.Item.Char.Latin(chars = listOf("f")),
        ).forEach {
            inputList = inputList.addItem(it)
        }
        assertEquals("，<(12 ab: cd、,：ef)", inputList.getText().toString())

        inputList = inputList.selectLast()
        listOf(
            CommonInput.Item.Char.Latin(chars = listOf("g")),
            CommonInput.Item.Char.Latin(chars = listOf("h")),
            CommonInput.Item.Char.Symbol(
                value = "（",
                fullWidth = true,
                close = CommonInput.Item.Char.Symbol(value = "）", fullWidth = true),
            )
        ).forEach {
            inputList = inputList.addItem(it)
        }

        inputList = inputList.selectLast()
        listOf(
            CommonInput.Item.Char.Latin(chars = listOf("i")),
            CommonInput.Item.Char.Latin(chars = listOf("j")),
            CommonInput.Item.Char.Space,
            CommonInput.Item.Char.Space,
            CommonInput.Item.Char.Latin(chars = listOf("k")),
        ).forEach {
            inputList = inputList.addItem(it)
        }
        assertEquals("，<(12 ab: cd、,：ef)（gh）ij  k", inputList.getText().toString())

        // ---------------------------------------------
        inputList = InputList()
        listOf(
            CommonInput.Item.Char.Latin(chars = listOf("a")),
            CommonInput.Item.Char.Latin(chars = listOf("b")),
        ).forEach {
            inputList = inputList.addItem(it)
        }

        inputList = inputList.withMathExprUpdate {
            addItem(MathInput.Item.Const.Number(chars = listOf('1')))
                .addItem(MathInput.Item.Op.Plus)
                .addItem(MathInput.Item.Const.Number(chars = listOf('2')))
        }.confirmPending()

        listOf(
            CommonInput.Item.Char.Latin(chars = listOf("c")),
            CommonInput.Item.Char.Latin(chars = listOf("d")),
        ).forEach {
            inputList = inputList.addItem(it)
        }
        assertEquals("ab 1 + 2 cd", inputList.getText().toString())

        // ---------------------------------------------
        inputList = InputList()
        listOf(
            pair01,
        ).forEach {
            inputList = inputList.addItem(it)
        }

        inputList = inputList.withMathExprUpdate {
            addItem(MathInput.Item.Const.Number(chars = listOf('1')))
                .addItem(MathInput.Item.Op.Plus)
                .addItem(MathInput.Item.Const.Number(chars = listOf('2')))
        }.confirmPending()

        listOf(
            CommonInput.Item.Char.Latin(chars = listOf("a")),
            CommonInput.Item.Char.Latin(chars = listOf("b")),
        ).forEach {
            inputList = inputList.addItem(it)
        }

        inputList = inputList.selectLast()
        listOf(
            CommonInput.Item.Char.Symbol(value = ","),
        ).forEach {
            inputList = inputList.addItem(it)
        }
        assertEquals("(1 + 2 ab),", inputList.getText().toString())

        // ---------------------------------------------
        inputList = InputList()
        listOf(
            CommonInput.Item.Char.Symbol(value = ","),
            CommonInput.Item.Pinyin(value = "yuan", valid = true),
            CommonInput.Item.Char.Symbol(value = ":"),
            CommonInput.Item.Pinyin(value = "tian", valid = true),
            CommonInput.Item.Char.Symbol(value = "、", fullWidth = true),
            CommonInput.Item.Pinyin(value = "ren", valid = true),
        ).forEach {
            inputList = inputList.addItem(it)
        }
        assertEquals(", yuan: tian、ren", inputList.getText().toString())

        // ---------------------------------------------
        inputList = InputList()
        listOf(
            CommonInput.Item.Char.Symbol(value = ","),
            CommonInput.Item.Pinyin(
                value = "yuan",
                valid = true,
                word = InputWord.Hanzi(value = "元", spell = Spell(id = "1", value = "yuán")),
            ),
            CommonInput.Item.Char.Symbol(value = ":"),
            CommonInput.Item.Pinyin(
                value = "tian", valid = true,
                word = InputWord.Hanzi(value = "天", spell = Spell(id = "2", value = "tiān")),
            ),
            CommonInput.Item.Char.Symbol(value = "、", fullWidth = true),
            CommonInput.Item.Pinyin(
                value = "ren", valid = true,
                word = InputWord.Hanzi(value = "人", spell = Spell(id = "3", value = "rén")),
            ),
            CommonInput.Item.Char.Latin(chars = listOf("a")),
            CommonInput.Item.Char.Latin(chars = listOf("b")),
        ).forEach {
            inputList = inputList.addItem(it)
        }
        assertEquals(",元:天、人 ab", inputList.getText().toString())
        assertEquals(
            ", yuán: tiān、rén ab",
            inputList.getText(
                InputTextOption(spellUseMode = InputWord.SpellUseMode.Replace)
            ).toString()
        )
    }
}
