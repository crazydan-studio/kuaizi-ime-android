/*
 * SPDX-FileCopyrightText: 2015 - 2025 Rime community
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.osfans.trime.core

import android.view.KeyCharacterMap
import android.view.KeyEvent

// source from https://github.com/osfans/trime/blob/develop/app/src/main/java/com/osfans/trime/core/KeyValue.kt

@JvmInline
value class KeyValue(
    val value: Int,
) {
    /** Rime 按键 [value] 所对应的 Android 系统按键 [KeyEvent]#KEYCODE_* */
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
