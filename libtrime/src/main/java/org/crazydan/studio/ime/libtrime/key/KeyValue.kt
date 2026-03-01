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

package org.crazydan.studio.ime.libtrime.key

import android.view.KeyCharacterMap
import android.view.KeyEvent
import com.osfans.trime.core.RimeKeyMapping

@JvmInline
value class KeyValue(
    val value: Int,
) {
    val keyCode get() = RimeKeyMapping.valToKeyCode(value)

    override fun toString() = "0x" + value.toString(16).padStart(4, '0')

    companion object {
        fun fromKeyEvent(event: KeyEvent): KeyValue {
            val charCode = event.unicodeChar

            // try charCode first, allow upper and lower case characters generating different KeyValue
            if (charCode != 0 &&
                // skip \t, because it's charCode is different from KeyValue
                charCode != '\t'.code &&
                // skip \n, because rime wants \r for return
                charCode != '\n'.code &&
                // skip Android's private-use character
                charCode != KeyCharacterMap.HEX_INPUT.code &&
                charCode != KeyCharacterMap.PICKER_DIALOG_INPUT.code
            ) {
                return KeyValue(charCode)
            }
            return KeyValue(RimeKeyMapping.keyCodeToVal(event.keyCode))
        }
    }
}
