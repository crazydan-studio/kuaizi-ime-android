/*
 * SPDX-FileCopyrightText: 2015 - 2025 Rime community
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.osfans.trime.core

// source from https://github.com/osfans/trime/blob/develop/app/src/main/java/com/osfans/trime/core/RimeConfig.kt

object RimeConfig {
    init {
        System.loadLibrary("rime_jni")
    }

    @JvmStatic
    external fun openRimeConfig(configId: String): Long

    @JvmStatic
    external fun openRimeUserConfig(configId: String): Long

    @JvmStatic
    external fun openRimeSchema(schemaId: String): Long

    @JvmStatic
    external fun getRimeConfigInt(configRef: Long, key: String): Int?

    @JvmStatic
    external fun getRimeConfigString(configRef: Long, key: String): String?

    @JvmStatic
    external fun getRimeConfigListItemPath(configRef: Long, key: String): Array<String>

    @JvmStatic
    external fun setRimeConfigBool(configRef: Long, key: String, value: Boolean)

    @JvmStatic
    external fun closeRimeConfig(configRef: Long)
}
