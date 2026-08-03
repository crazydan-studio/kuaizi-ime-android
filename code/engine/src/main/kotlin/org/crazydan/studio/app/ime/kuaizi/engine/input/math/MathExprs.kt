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

import org.crazydan.studio.app.ime.kuaizi.engine.domain.isZero
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

    /** 返回完整的中缀表达式 */
    override fun toString(): String =
        items.map {
            when (it) {
                is Item.Op -> it.name
                is Item.Num -> it.value
                is Item.Func -> it.name
                is Item.Bracket.Left -> "("
                is Item.Bracket.Right -> ")"
            }
        }.joinToString(" ")

    // --------------------------------------------------------

    /** [MathExpr] 元素 */
    sealed class Item {

        /** 数字 */
        data class Num(val value: Double) : Item()

        /** 运算符 */
        sealed class Op : Item() {
            /** 名字 */
            abstract val name: String

            /** 参数数量 */
            abstract val args: Int

            /** 符号优先级 */
            abstract val priority: Int

            /** 判断当前运算符是否优先于指定的运算符  */
            fun isPriorTo(other: Op): Boolean =
                when (other) {
                    is Binary if other.rightAssociative ->
                        other.priority < this.priority

                    else -> other.priority <= this.priority
                }

            /** 一元运算符 */
            data class Unary(
                override val name: String,
                override val priority: Int = 0,
                val fn: (Double) -> Double?,
            ) : Op() {
                override val args: Int
                    get() = 1
            }

            /** 二元运算符 */
            data class Binary(
                override val name: String,
                override val priority: Int = 0,
                /**
                 * 是否为右结合性。
                 *
                 * 结合性表示，相同运算符连续出现时，先计算左侧还是右侧，
                 * 如 `1 + 2 + 3` 等价于 `(1 + 2) + 3`，即，加号为左结合性，
                 * 而 `2^3^4` 则等价于 `2^(3^4)`，即，幂指数为右结合性。
                 */
                val rightAssociative: Boolean = false,
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

        /** 函数：暂时仅支持单参函数 */
        data class Func(
            val name: String,
            val args: Int = 1,
            val fn: (Double) -> Double?,
        ) : Item()
    }
}

// ----------------------------------------------------------------------------

/** 执行运算得到结果 */
fun MathExpr.Item.Op.exec(operands: DoubleArray?): Double? =
    if (operands == null || operands.size != args)
        null
    else
        when (this) {
            is MathExpr.Item.Op.Unary ->
                fn(operands[0])

            is MathExpr.Item.Op.Binary ->
                fn(operands[0], operands[1])
        }

/** 调用函数获得运算结果 */
fun MathExpr.Item.Func.call(operands: DoubleArray?): Double? =
    if (operands == null || operands.size != args)
        null
    else
        fn(operands[0])

// ----------------------------------------------------------------------------

private fun createMathExpr(inputs: List<MathInput.Item>): MathExpr {
    val items = ArrayList<MathExpr.Item>(inputs.size * 2)

    inputs.forEach { input ->
        when (input) {
            is MathInput.Item.Op -> items.add(input.toExprItem())
            is MathInput.Item.Const -> items.add(MathExpr.Item.Num(input.actual))

            is MathInput.Item.Bracket -> items.add(MathExpr.Item.Bracket.Left)
            is MathInput.Item.CloseSymbol -> items.add(MathExpr.Item.Bracket.Right)

            is MathInput.Item.Func -> {
                items.add(input.toExprItem())
                items.add(MathExpr.Item.Bracket.Left)
            }

            else -> {}
        }
    }

    return MathExpr(items)
}

/** 从输入项转换为算术表达式元素 */
private fun MathInput.Item.Op.toExprItem(): MathExpr.Item.Op =
    when (this) {
        is MathInput.Item.Op.Plus -> MathExpr.Item.Op.Binary(
            name = value,
            priority = 10,
            fn = fun(v1: Double, v2: Double): Double? = v1 + v2,
        )

        is MathInput.Item.Op.Minus -> MathExpr.Item.Op.Binary(
            name = value,
            priority = 10,
            fn = fun(v1: Double, v2: Double): Double? = v1 - v2,
        )

        is MathInput.Item.Op.Multiply -> MathExpr.Item.Op.Binary(
            name = value,
            priority = 20,
            fn = fun(v1: Double, v2: Double): Double? = v1 * v2,
        )

        is MathInput.Item.Op.Divide -> MathExpr.Item.Op.Binary(
            name = value,
            priority = 20,
            fn =
                fun(v1: Double, v2: Double): Double? =
                    if (isZero(v2)) null
                    else v1 / v2,
        )

        is MathInput.Item.Op.Power -> MathExpr.Item.Op.Binary(
            name = value,
            priority = 500,
            rightAssociative = true,
            fn =
                fun(v1: Double, v2: Double): Double? =
                    if (isZero(v1) && v2 < 0) null
                    else v1.pow(v2),
        )

        is MathInput.Item.Op.Percent -> MathExpr.Item.Op.Unary(
            name = value,
            priority = 100,
            fn = fun(v1: Double): Double? = v1 * 0.01,
        )

        is MathInput.Item.Op.Permillage -> MathExpr.Item.Op.Unary(
            name = value,
            priority = 100,
            fn = fun(v1: Double): Double? = v1 * 0.001,
        )

        is MathInput.Item.Op.Permyriad -> MathExpr.Item.Op.Unary(
            name = value,
            priority = 100,
            fn = fun(v1: Double): Double? = v1 * 0.0001,
        )

        is MathInput.Item.Op.Degree -> MathExpr.Item.Op.Unary(
            name = value,
            priority = 110,
            // 1° = π/180
            fn = fun(v1: Double): Double? = Math.toRadians(v1),
        )
    }

/** 从输入项转换为算术表达式元素 */
private fun MathInput.Item.Func.toExprItem(): MathExpr.Item.Func =
    when (this) {
        is MathInput.Item.Func.Sin -> MathExpr.Item.Func(
            name = value.removeSuffix("("),
            fn = fun(v: Double): Double? = sin(v),
        )

        is MathInput.Item.Func.Cos -> MathExpr.Item.Func(
            name = value.removeSuffix("("),
            fn = fun(v: Double): Double? = cos(v),
        )

        is MathInput.Item.Func.Tan -> MathExpr.Item.Func(
            name = value.removeSuffix("("),
            // cos(v) 不能等于 0
            fn =
                fun(v: Double): Double? =
                    if (isZero(cos(v))) null else tan(v),
        )

        is MathInput.Item.Func.Sqrt -> MathExpr.Item.Func(
            name = value.removeSuffix("("),
            fn = fun(v: Double): Double? = sqrt(v),
        )

        is MathInput.Item.Func.LogE -> MathExpr.Item.Func(
            name = value.removeSuffix("("),
            fn = fun(v: Double): Double? = if (v > 0) ln(v) else null,
        )
    }

// ----------------------------------------------------------------------------

private fun evalMathExpr(expr: MathExpr): Double? {
    val items = infixToPostfix(expr.items)
    val values = Stack<Double>()

    val popOperands = fun(stack: Stack<Double>, count: Int): DoubleArray? {
        // 缺少足够的操作数
        if (stack.size < count) {
            return null
        }

        val operands = DoubleArray(count)
        // Note：后缀表达式的操作数顺序是倒置的，需恢复其原始顺序
        for (i in count - 1 downTo 0) {
            operands[i] = stack.pop()
        }

        return operands
    }

    // 后缀表达式的计算过程：
    // https://zh.wikipedia.org/wiki/%E9%80%86%E6%B3%A2%E5%85%B0%E8%A1%A8%E7%A4%BA%E6%B3%95
    for (item in items) {
        when (item) {
            is MathExpr.Item.Num ->
                values.push(item.value)

            is MathExpr.Item.Op -> {
                val operands = popOperands(values, item.args)

                val value = item.exec(operands)
                if (value == null) {
                    return null
                }

                values.push(value)
            }

            is MathExpr.Item.Func -> {
                val operands = popOperands(values, item.args)

                val value = item.call(operands)
                if (value == null) {
                    return null
                }

                values.push(value)
            }

            else -> {}
        }
    }

    return if (values.size == 1) values.pop() else null
}

/**
 * 中缀表达式转后缀表达式：
 * 采用调度场算法（https://zh.wikipedia.org/wiki/%E8%B0%83%E5%BA%A6%E5%9C%BA%E7%AE%97%E6%B3%95）
 */
private fun infixToPostfix(items: List<MathExpr.Item>): List<MathExpr.Item> {
    val output = ArrayList<MathExpr.Item>(items.size)
    val operators = Stack<MathExpr.Item>()

    items.forEach { item ->
        // Note：暂时仅支持单参函数，因此，不处理参数分隔符
        when (item) {
            is MathExpr.Item.Num ->
                output.add(item)

            is MathExpr.Item.Func,
            is MathExpr.Item.Bracket.Left,
                ->
                operators.add(item)

            is MathExpr.Item.Op -> {
                val o1 = item
                while (!operators.isEmpty()) {
                    val o2 = operators.peek()
                    if (o2 is MathExpr.Item.Op && !o2.isPriorTo(o1)) {
                        break
                    }

                    operators.pop()
                    output.add(o2)
                }

                operators.push(o1)
            }

            is MathExpr.Item.Bracket.Right -> {
                // Note：括号由 MathInputList 保证其必然成对出现，因此，无需考虑左括号不存在的情况
                while (!operators.isEmpty()) {
                    val op = operators.pop()
                    if (op is MathExpr.Item.Bracket.Left) {
                        break
                    }

                    output.add(op)
                }

                if (!operators.isEmpty()) {
                    val op = operators.peek()
                    if (op is MathExpr.Item.Func) {
                        operators.pop()
                        output.add(op)
                    }
                }
            }
        }
    }

    while (!operators.isEmpty()) {
        val op = operators.pop()
        output.add(op)
    }

    return output
}
