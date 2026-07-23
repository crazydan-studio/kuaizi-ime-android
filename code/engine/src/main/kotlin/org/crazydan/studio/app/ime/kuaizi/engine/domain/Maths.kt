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

/** 数学符号 */
sealed class MathSymbol {
    /** 符号字符值 */
    abstract val value: String

    // ------------------------------------------

    data object Equal : MathSymbol() {
        override val value: String
            get() = "="
    }

    data object Dot : MathSymbol() {
        override val value: String
            get() = "."
    }

    data object Plus : MathSymbol() {
        override val value: String
            get() = "+"
    }

    data object Minus : MathSymbol() {
        override val value: String
            get() = "-"
    }

    data object Multiply : MathSymbol() {
        override val value: String
            get() = "×"
    }

    data object Divide : MathSymbol() {
        override val value: String
            get() = "÷"
    }

    /** `%` */
    data object Percent : MathSymbol() {
        override val value: String
            get() = "%"
    }

    /** `‰` */
    data object Permillage : MathSymbol() {
        override val value: String
            get() = "‰"
    }

    /** `‱` */
    data object Permyriad : MathSymbol() {
        override val value: String
            get() = "‱"
    }

    // ------------------------------------------

    /** `( )` */
    data object Bracket : MathSymbol() {
        val left: String
            get() = "("
        val right: String
            get() = ")"

        override val value: String
            get() = "$left $right"
    }

    // ------------------------------------------

    sealed class Func : MathSymbol() {

        /** `sin(x)` */
        data object Sin : Func() {
            val left: String
                get() = "sin("
            val right: String
                get() = ")"

            override val value: String
                get() = left + "x" + right
        }

        /** `cos(x)` */
        data object Cos : Func() {
            val left: String
                get() = "cos("
            val right: String
                get() = ")"

            override val value: String
                get() = left + "x" + right
        }

        /** `tan(x)` */
        data object Tan : Func() {
            val left: String
                get() = "tan("
            val right: String
                get() = ")"

            override val value: String
                get() = left + "x" + right
        }

        /** `√(x)` */
        data object Sqrt : Func() {
            val left: String
                get() = "√("
            val right: String
                get() = ")"

            override val value: String
                get() = left + "x" + right
        }

        /** `ln(x)` */
        data object LogE : Func() {
            val left: String
                get() = "ln("
            val right: String
                get() = ")"

            override val value: String
                get() = left + "x" + right
        }
    }

    // ------------------------------------------

    sealed class Const : MathSymbol() {

        /** 自然数 `e` */
        data object E : Const() {
            override val value: String
                get() = "e"
        }

        /** 圆周率 `π` */
        data object PI : Const() {
            override val value: String
                get() = "π"
        }
    }
}
