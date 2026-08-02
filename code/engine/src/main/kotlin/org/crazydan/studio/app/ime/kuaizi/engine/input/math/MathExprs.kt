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

package org.crazydan.studio.app.ime.kuaizi.engine.input.math

import java.util.Stack
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

/** 算术表达式 */
class MathExpr(val items: List<Item>) {

    companion object {

        /** 根据输入项列表构造 [MathExpr] */
        fun create(items: List<MathInput.Item>): MathExpr =
            createMathExpr(items)
    }

    // --------------------------------------------------------

    /** 计算结果 */
    fun eval(): Double? =
        evalMathExpr(this)

    // --------------------------------------------------------

    /** [MathExpr] 元素 */
    sealed class Item {

        /** 数字 */
        data class Num(val value: Double) : Item()

        /** 运算符 */
        sealed class Op : Item() {
            /** 参数数量 */
            abstract val args: Int

            /** 符号优先级 */
            abstract val priority: Int

            /** 判断当前运算符是否优先于指定的运算符  */
            fun isPriorTo(other: Op): Boolean =
                this.priority >= other.priority

            /** 一元运算符 */
            data class Unary(
                override val priority: Int = 0,
                val fn: (Double) -> Double?,
            ) : Op() {
                override val args: Int
                    get() = 1
            }

            /** 二元运算符 */
            data class Binary(
                override val priority: Int = 0,
                val fn: (Double, Double) -> Double?,
            ) : Op() {
                override val args: Int
                    get() = 2
            }
        }

        /** 括号 */
        sealed class Bracket : Item() {

            /** 左括号 */
            data object Left : Bracket()

            /** 右括号 */
            data object Right : Bracket()
        }
    }
}

// ----------------------------------------------------------------------------

/** 计算结果 */
fun MathExpr.Item.Op.calc(numbers: List<MathExpr.Item.Num>): Double? =
    if (numbers.size != args)
        null
    else
        when (this) {
            is MathExpr.Item.Op.Unary ->
                fn(numbers[0].value)

            is MathExpr.Item.Op.Binary ->
                fn(numbers[0].value, numbers[1].value)
        }

// ----------------------------------------------------------------------------

private fun createMathExpr(items: List<MathInput.Item>): MathExpr {
    TODO()
}

/** 从输入项转换为算术表达式元素 */
private fun MathInput.Item.Op.toExprItem(): MathExpr.Item.Op =
    when (this) {
        is MathInput.Item.Op.Plus -> MathExpr.Item.Op.Binary(
            priority = 10,
            fn = fun(v1: Double, v2: Double): Double? = v1 + v2,
        )

        is MathInput.Item.Op.Minus -> MathExpr.Item.Op.Binary(
            priority = 10,
            fn = fun(v1: Double, v2: Double): Double? = v1 - v2,
        )

        is MathInput.Item.Op.Multiply -> MathExpr.Item.Op.Binary(
            priority = 20,
            fn = fun(v1: Double, v2: Double): Double? = v1 * v2,
        )

        is MathInput.Item.Op.Divide -> MathExpr.Item.Op.Binary(
            priority = 20,
            fn =
                fun(v1: Double, v2: Double): Double? =
                    if (v2 == 0.0) null
                    else v1 / v2,
        )

        is MathInput.Item.Op.Power -> MathExpr.Item.Op.Binary(
            priority = 30,
            fn =
                fun(v1: Double, v2: Double): Double? =
                    if (v1 == 0.0 && v2 < 0) null
                    else v1.pow(v2),
        )

        is MathInput.Item.Op.Percent -> MathExpr.Item.Op.Unary(
            priority = 1000,
            fn = fun(v1: Double): Double? = v1 * 0.01,
        )

        is MathInput.Item.Op.Permillage -> MathExpr.Item.Op.Unary(
            priority = 1000,
            fn = fun(v1: Double): Double? = v1 * 0.001,
        )

        is MathInput.Item.Op.Permyriad -> MathExpr.Item.Op.Unary(
            priority = 1000,
            fn = fun(v1: Double): Double? = v1 * 0.0001,
        )

        is MathInput.Item.Op.Degree -> MathExpr.Item.Op.Unary(
            priority = 1010,
            // 1° = π/180
            fn = fun(v1: Double): Double? = Math.toRadians(v1),
        )
    }

/** 从输入项转换为算术表达式元素 */
private fun MathInput.Item.Func.toExprItem(): MathExpr.Item.Op =
    when (this) {
        is MathInput.Item.Func.Sin -> MathExpr.Item.Op.Unary(
            priority = 500,
            fn = fun(v: Double): Double = sin(v),
        )

        is MathInput.Item.Func.Cos -> MathExpr.Item.Op.Unary(
            priority = 500,
            fn = fun(v: Double): Double = cos(v),
        )

        is MathInput.Item.Func.Tan -> MathExpr.Item.Op.Unary(
            priority = 500,
            fn = fun(v: Double): Double = tan(v),
        )

        is MathInput.Item.Func.Sqrt -> MathExpr.Item.Op.Unary(
            priority = 500,
            fn = fun(v: Double): Double = sqrt(v),
        )

        is MathInput.Item.Func.LogE -> MathExpr.Item.Op.Unary(
            priority = 500,
            fn = fun(v: Double): Double = ln(v),
        )
    }

// ----------------------------------------------------------------------------

private fun evalMathExpr(expr: MathExpr): Double? {
    val items = infixToPostfix(expr.items)

    // 后缀表达式的计算过程：
    // https://zh.wikipedia.org/wiki/%E9%80%86%E6%B3%A2%E5%85%B0%E8%A1%A8%E7%A4%BA%E6%B3%95
    val values = Stack<MathExpr.Item.Num>()
    for (expr in items) {
        if (expr is MathExpr.Item.Num) {
            values.push(expr)
        } else if (expr is MathExpr.Item.Op) {
            val op = expr
            val numbers = ArrayList<MathExpr.Item.Num>(op.args)

            var i = op.args
            while (!values.isEmpty() && i > 0) {
                val num = values.pop()
                numbers.add(num)
                i--
            }

            if (numbers.size != op.args) {
                return null
            } else {
                numbers.reverse()

                val result: Double? = op.calc(numbers)
                if (result == null) {
                    return null
                }

                values.add(MathExpr.Item.Num(result))
            }
        }
    }

    return if (values.size == 1) values.pop().value else null
}

/**
 * 中缀表达式转后缀表达式：
 * 采用调度场算法（https://zh.wikipedia.org/wiki/%E8%B0%83%E5%BA%A6%E5%9C%BA%E7%AE%97%E6%B3%95）
 */
private fun infixToPostfix(items: List<MathExpr.Item>): List<MathExpr.Item> {
    val output = ArrayList<MathExpr.Item>(items.size)
    val operators = Stack<MathExpr.Item.Op>()

    items.forEach { item ->
        when (item) {
            is MathExpr.Item.Num -> output.add(item)
            is MathExpr.Item.Op -> {
                val op1 = item
                while (!operators.isEmpty()) {
                    val op2 = operators.peek()
                    if (!op2.isPriorTo(op1)) {
                        break
                    }
                    operators.pop()

                    output.add(op2)
                }

                operators.push(op1)
            }
        }
    }

    while (!operators.isEmpty()) {
        val op = operators.pop()
        output.add(op)
    }

    return output
}
