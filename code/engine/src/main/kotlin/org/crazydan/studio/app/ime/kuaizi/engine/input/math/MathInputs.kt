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

import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

/** 算术表达式输入项 */
sealed class MathInputItem {

    /** 输入项之间的空隙，便于在相邻输入项之间插入其他输入项 */
    data object Gap : MathInputItem()

    /** 常数 */
    sealed class Const : MathInputItem() {
        /** 正/负符号 */
        abstract val sign: Sign?

        /** 真实数值 */
        abstract val value: Double

        /** 数字 */
        data class Number() : Const()

        /** 自然数 e */
        data class E(
            override val sign: Sign? = null
        ) : Const() {
            override val value: Double
                get() = Math.E
        }

        /** 圆周率 π */
        data class PI(
            override val sign: Sign? = null
        ) : Const() {
            override val value: Double
                get() = Math.PI
        }

        enum class Sign {
            Positive, Negative
        }
    }

    /** 运算符 */
    sealed class Op : MathInputItem() {

        data object Plus : Op()
        data object Minus : Op()
        data object Multiply : Op()
        data object Divide : Op()
    }

    /** 函数 */
    sealed class Func : MathInputItem() {
        abstract val value: String
        abstract val close: Symbol

        /** 运算函数 */
        abstract val func: (Double) -> Double

        /** 三角函數 sin(x) */
        data class Sin(
            override val value: String = "sin(",
            override val close: Symbol = Symbol(")"),
            override val func: (Double) -> Double = fun(v: Double): Double { return sin(v) },
        ) : Func()

        /** 三角函數 cos(x) */
        data class Cos(
            override val value: String = "cos(",
            override val close: Symbol = Symbol(")"),
            override val func: (Double) -> Double = fun(v: Double): Double { return cos(v) },
        ) : Func()

        /** 三角函數 tan(x) */
        data class Tan(
            override val value: String = "tan(",
            override val close: Symbol = Symbol(")"),
            override val func: (Double) -> Double = fun(v: Double): Double { return tan(v) },
        ) : Func()

        /** 开平方 */
        data class Sqrt(
            override val value: String = "√(",
            override val close: Symbol = Symbol(")"),
            override val func: (Double) -> Double = fun(v: Double): Double { return sqrt(v) },
        ) : Func()

        /** 以自然数 e 为底的对数 */
        data class LogE(
            override val value: String = "ln(",
            override val close: Symbol = Symbol(")"),
            override val func: (Double) -> Double = fun(v: Double): Double { return ln(v) },
        ) : Func()

        /** 以 10 为底的对数 */
        data class Log10(
            override val value: String = "log10(",
            override val close: Symbol = Symbol(")"),
            override val func: (Double) -> Double = fun(v: Double): Double { return log10(v) },
        ) : Func()
    }

    /** (圆)括号 */
    data class Bracket(
        val value: String = "(",
        val close: Symbol = Symbol(")"),
    ) : MathInputItem()

    /** 数学符号：仅内部使用，用于对括号、函数等符号的闭合符号的引用，确保二者始终成对出现和删除 */
    data class Symbol(val value: String) : MathInputItem()
}
