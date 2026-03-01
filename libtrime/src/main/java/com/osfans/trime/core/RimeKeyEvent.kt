/*
 * SPDX-FileCopyrightText: 2015 - 2024 Rime community
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.osfans.trime.core

object RimeKeyEvent {

    @JvmStatic
    external fun getKeycodeByName(name: String): Int

    @JvmStatic
    external fun getModifierByName(name: String): Int
}
