/*
 * SPDX-FileCopyrightText: 2015 - 2025 Rime community
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.osfans.trime.core

object Rime {
    init {
        System.loadLibrary("rime_jni")
    }

    // ++++++++++++++++++++++++++++ Call by native ++++++++++++++++++++++++

    @JvmStatic
    fun handleRimeMessage(
        type: Int,
        params: Array<Any>,
    ) {
        // TODO 分发来自 Rime 的消息
    }

    // ++++++++++++++++++++++++++ Native Interface +++++++++++++++++++++++++++

    // init
    @JvmStatic
    external fun startupRime(
        sharedDir: String,
        userDir: String,
        versionName: String,
        fullCheck: Boolean,
    )

    @JvmStatic
    external fun exitRime()

    @JvmStatic
    external fun deployRimeSchemaFile(schemaFile: String): Boolean

    @JvmStatic
    external fun deployRimeConfigFile(
        fileName: String,
        versionKey: String,
    ): Boolean

    @JvmStatic
    external fun syncRimeUserData(): Boolean

    // input
    @JvmStatic
    external fun processRimeKey(
        keycode: Int,
        mask: Int,
    ): Boolean

    @JvmStatic
    external fun commitRimeComposition(): Boolean

    @JvmStatic
    external fun clearRimeComposition()

    // output
    @JvmStatic
    external fun getRimeCommit(): CommitProto

    @JvmStatic
    external fun getRimeContext(): ContextProto

    @JvmStatic
    external fun getRimeStatus(): StatusProto

    // runtime options
    @JvmStatic
    external fun setRimeOption(
        option: String,
        value: Boolean,
    )

    @JvmStatic
    external fun getRimeOption(option: String): Boolean

    @JvmStatic
    external fun getRimeSchemaList(): Array<SchemaItem>

    @JvmStatic
    external fun getCurrentRimeSchema(): String

    @JvmStatic
    external fun selectRimeSchema(schemaId: String): Boolean

    // testing
    @JvmStatic
    external fun simulateRimeKeySequence(keySequence: String): Boolean

    @JvmStatic
    external fun getRimeRawInput(): String

    @JvmStatic
    external fun getRimeCaretPos(): Int

    @JvmStatic
    external fun setRimeCaretPos(caretPos: Int)

    @JvmStatic
    external fun selectRimeCandidate(index: Int, global: Boolean): Boolean

    @JvmStatic
    external fun deleteRimeCandidate(index: Int, global: Boolean): Boolean

    @JvmStatic
    external fun changeRimeCandidatePage(backward: Boolean): Boolean

    @JvmStatic
    external fun getAvailableRimeSchemaList(): Array<SchemaItem>

    @JvmStatic
    external fun getSelectedRimeSchemaList(): Array<SchemaItem>

    @JvmStatic
    external fun selectRimeSchemas(schemaIds: Array<String>): Boolean

    @JvmStatic
    external fun getRimeCandidates(
        startIndex: Int,
        limit: Int,
    ): Array<CandidateItem>

    @JvmStatic
    external fun getRimeBulkCandidates(): Array<Any>
}
