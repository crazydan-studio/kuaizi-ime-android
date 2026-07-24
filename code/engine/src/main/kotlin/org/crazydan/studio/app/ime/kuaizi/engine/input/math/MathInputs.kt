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

import org.crazydan.studio.app.ime.kuaizi.engine.domain.MathSymbol
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

/** 算术输入 */
sealed class MathInput {

    /** [Item] 之间的空隙，便于在相邻输入项之间添加其他输入项 */
    data object Gap : MathInput()

    /** 输入项：承载实际可见的输入内容 */
    sealed class Item : MathInput() {
        /** 输入项字符值 */
        abstract val value: String

        /** 常数 */
        sealed class Const : Item() {
            /** 正/负符号：添加逻辑见 [MathInput.Item.Const.addSign] */
            abstract val sign: Sign?

            /** 真实数值 */
            abstract val actual: Double

            /** 数字：含小数点 */
            data class Number(
                val chars: List<Char>,
                override val sign: Sign? = null,
            ) : Const() {
                override val value: String
                    get() = sign.withString(chars.joinToString(""))
                override val actual: Double
                    get() = value.toDouble()

                init {
                    require(chars.isNotEmpty())
                }
            }

            /** 自然数 `e` */
            data class E(
                override val sign: Sign? = null,
            ) : Const() {
                override val value: String
                    get() = sign.withString(MathSymbol.Const.E.value)
                override val actual: Double
                    get() = sign.withNumber(Math.E)
            }

            /** 圆周率 `π` */
            data class PI(
                override val sign: Sign? = null,
            ) : Const() {
                override val value: String
                    get() = sign.withString(MathSymbol.Const.PI.value)
                override val actual: Double
                    get() = sign.withNumber(Math.PI)
            }

            enum class Sign {
                /** `+` */
                Positive,

                /** `-` */
                Negative
            }
        }

        /** 运算符 */
        sealed class Op : Item() {
            /** 计算函数：可能发生除零异常 */
            abstract val compute: (Double, Double) -> Double

            data object Plus : Op() {
                override val value: String
                    get() = MathSymbol.Plus.value
                override val compute: (Double, Double) -> Double
                    get() = fun(v1: Double, v2: Double): Double { return v1 + v2 }
            }

            data object Minus : Op() {
                override val value: String
                    get() = MathSymbol.Minus.value
                override val compute: (Double, Double) -> Double
                    get() = fun(v1: Double, v2: Double): Double { return v1 - v2 }
            }

            data object Multiply : Op() {
                override val value: String
                    get() = MathSymbol.Multiply.value
                override val compute: (Double, Double) -> Double
                    get() = fun(v1: Double, v2: Double): Double { return v1 * v2 }
            }

            data object Divide : Op() {
                override val value: String
                    get() = MathSymbol.Divide.value
                override val compute: (Double, Double) -> Double
                    get() = fun(v1: Double, v2: Double): Double { return v1 / v2 }
            }

            /** `%` */
            data object Percent : Op() {
                override val value: String
                    get() = MathSymbol.Percent.value
                override val compute: (Double, Double) -> Double
                    get() = fun(v1: Double, _: Double): Double { return v1 * 0.01 }
            }

            /** `‰` */
            data object Permillage : Op() {
                override val value: String
                    get() = MathSymbol.Permillage.value
                override val compute: (Double, Double) -> Double
                    get() = fun(v1: Double, _: Double): Double { return v1 * 0.001 }
            }

            /** `‱` */
            data object Permyriad : Op() {
                override val value: String
                    get() = MathSymbol.Permyriad.value
                override val compute: (Double, Double) -> Double
                    get() = fun(v1: Double, _: Double): Double { return v1 * 0.0001 }
            }
        }

        /** 函数 */
        sealed class Func : Item() {
            /** 闭合符号 */
            abstract val close: Symbol

            /** 计算函数：可能发生计算异常 */
            abstract val compute: (Double) -> Double

            /** 三角函數 `sin(x)` */
            data class Sin(
                override val value: String = MathSymbol.Func.Sin.left,
                override val close: Symbol = Symbol(MathSymbol.Func.Sin.right),
                override val compute: (Double) -> Double = fun(v: Double): Double { return sin(v) },
            ) : Func()

            /** 三角函數 `cos(x)` */
            data class Cos(
                override val value: String = MathSymbol.Func.Cos.left,
                override val close: Symbol = Symbol(MathSymbol.Func.Cos.right),
                override val compute: (Double) -> Double = fun(v: Double): Double { return cos(v) },
            ) : Func()

            /** 三角函數 `tan(x)` */
            data class Tan(
                override val value: String = MathSymbol.Func.Tan.left,
                override val close: Symbol = Symbol(MathSymbol.Func.Tan.right),
                override val compute: (Double) -> Double = fun(v: Double): Double { return tan(v) },
            ) : Func()

            /** 开平方 */
            data class Sqrt(
                override val value: String = MathSymbol.Func.Sqrt.left,
                override val close: Symbol = Symbol(MathSymbol.Func.Sqrt.right),
                override val compute: (Double) -> Double = fun(v: Double): Double { return sqrt(v) },
            ) : Func()

            /** 以自然数 `e` 为底的对数 */
            data class LogE(
                override val value: String = MathSymbol.Func.LogE.left,
                override val close: Symbol = Symbol(MathSymbol.Func.LogE.right),
                override val compute: (Double) -> Double = fun(v: Double): Double { return ln(v) },
            ) : Func()
        }

        /** (圆)括号 */
        data class Bracket(
            /** 输入项字符值 */
            override val value: String = MathSymbol.Bracket.left,
            /** 闭合符号 */
            val close: Symbol = Symbol(MathSymbol.Bracket.right),
        ) : Item()

        /** 小数点 */
        data object Dot : Item() {
            override val value: String
                get() = MathSymbol.Dot.value
        }

        /** 等于号 */
        data object Equal : Item() {
            override val value: String
                get() = MathSymbol.Equal.value
        }

        /** 数学符号：仅内部使用，用于对括号、函数等符号的闭合符号的引用，确保二者始终成对出现和删除 */
        data class Symbol(
            override val value: String,
        ) : Item()
    }
}

// -----------------------------------------------------------------------

/**
 * 追加字符：
 * - 若 [char] 为数字，或者 [char] 是小数点且该输入不含小数点，则追加 [char]。
 *   注意，该输入必然已包含至少一个数字，故而，不会出现以小数点开头的数字；
 * - 否则，不做处理；
 */
fun MathInput.Item.Const.Number.appendChar(char: Char): MathInput.Item.Const.Number =
    if (
        (char == MathSymbol.Dot.value[0] && !chars.contains(char))
        || char.isDigit()
    )
        copy(chars = chars + char)
    else
        this

/** 丢弃最后一个字符 */
fun MathInput.Item.Const.Number.dropLastChar(): MathInput.Item.Const.Number =
    copy(chars = chars.dropLast(1))

/**
 * 添加正负号：
 * - 若当前正负号与 [sign] 相同，则去掉正负号；
 * - 否则，加上 [sign]；
 */
fun <T : MathInput.Item.Const> T.addSign(sign: MathInput.Item.Const.Sign?): T =
    (if (this.sign == sign) null else sign).let { s ->
        when (this) {
            is MathInput.Item.Const.Number ->
                copy(sign = s)

            is MathInput.Item.Const.E ->
                copy(sign = s)

            is MathInput.Item.Const.PI ->
                copy(sign = s)
        } as T
    }

/** 根据 [MathInput.Item.Op] 向常数补充正负号 */
fun <T : MathInput.Item.Const> T.addSignByOp(op: MathInput.Item.Op): T =
    addSign(
        when (op) {
            is MathInput.Item.Op.Plus -> MathInput.Item.Const.Sign.Positive
            is MathInput.Item.Op.Minus -> MathInput.Item.Const.Sign.Negative
            else -> null
        }
    )

private fun MathInput.Item.Const.Sign?.withString(s: String): String =
    when (this) {
        MathInput.Item.Const.Sign.Positive -> "+$s"
        MathInput.Item.Const.Sign.Negative -> "-$s"
        else -> s
    }

private fun MathInput.Item.Const.Sign?.withNumber(v: Double): Double =
    when (this) {
        MathInput.Item.Const.Sign.Negative -> -1 * v
        else -> v
    }
