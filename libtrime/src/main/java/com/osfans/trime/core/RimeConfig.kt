/*
 * SPDX-FileCopyrightText: 2015 - 2025 Rime community
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.osfans.trime.core

object RimeConfig {
    init {
        System.loadLibrary("rime_jni")
    }

    @JvmStatic
    private external fun openRimeConfig(configId: String): Long

    @JvmStatic
    private external fun openRimeUserConfig(configId: String): Long

    @JvmStatic
    private external fun openRimeSchema(schemaId: String): Long

    @JvmStatic
    private external fun getRimeConfigInt(peer: Long, key: String): Int?

    @JvmStatic
    private external fun getRimeConfigString(peer: Long, key: String): String?

    @JvmStatic
    private external fun getRimeConfigListItemPath(peer: Long, key: String): Array<String>

    @JvmStatic
    private external fun setRimeConfigBool(peer: Long, key: String, value: Boolean)

    @JvmStatic
    private external fun closeRimeConfig(peer: Long)
}
