/*
 * SPDX-FileCopyrightText: 2015 - 2025 Rime community
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.osfans.trime.core

import timber.log.Timber

/**
 * 只有 Rime 启动、同步和部署 schema（输入方案）/配置文件才是异步的，其与接口均为同步的。
 * 但为了统一上层调用，强制要求以消息形式监听输入等
 */
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
        Timber.i("Got rime message (type=${type})")
    }

    // ++++++++++++++++++++++++++ Native Interface +++++++++++++++++++++++++++

    // init
    /** Rime 启动是异步的，需要监听 1-Schema, 2-Option, 3-Deploy 消息 */
    @JvmStatic
    external fun startupRime(
        sharedDir: String,
        userDir: String,
        versionName: String = "3.3.9", // Trime version
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
        /** Rime 按键值，其对应 [RimeKeyMapping]#RimeKey_* */
        keyVal: Int,
        /** 控制按键组合标记，如 Shift+Ctrl 等，通过 [android.view.KeyEvent]#META_* 做 or 运算得到 */
        modifiers: Int,
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

    /** 选择候选字列表中指定序号的候选字：其将更新候选字的权重 */
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
