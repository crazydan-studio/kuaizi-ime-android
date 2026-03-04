/*
 * SPDX-FileCopyrightText: 2015 - 2024 Rime community
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.osfans.trime.core

// source from https://github.com/osfans/trime/blob/develop/app/src/main/java/com/osfans/trime/core/RimeKeyEvent.kt

data class RimeKeyEvent(
    val value: Int,
    val modifiers: Int,
    val repr: String,
) {

    override fun toString() = repr

    companion object {
        val None = RimeKeyEvent(0, 0, "0x0000")

        @JvmStatic
        external fun parse(repr: String): RimeKeyEvent

        @JvmStatic
        external fun getKeycodeByName(name: String): Int

        @JvmStatic
        external fun getModifierByName(name: String): Int
    }
}