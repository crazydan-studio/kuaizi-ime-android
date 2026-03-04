/*
 * SPDX-FileCopyrightText: 2015 - 2025 Rime community
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.osfans.trime.core

import com.osfans.trime.core.Rime.getRimeSchemaList
import com.osfans.trime.core.Rime.getSelectedRimeSchemaList
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

    // +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

    @JvmStatic
    external fun deployRimeConfigFile(
        fileName: String,
        versionKey: String,
    ): Boolean

    @JvmStatic
    external fun syncRimeUserData(): Boolean

    // +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

    @JvmStatic
    external fun getRimeOption(option: String): Boolean

    @JvmStatic
    external fun setRimeOption(option: String, value: Boolean)

    // +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

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

    // +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

    // output
    @JvmStatic
    external fun getRimeCommit(): CommitProto

    @JvmStatic
    external fun getRimeContext(): ContextProto

    @JvmStatic
    external fun getRimeStatus(): StatusProto

    // +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

    /** 获取当前正在输入的字符串 */
    @JvmStatic
    external fun getRimeRawInput(): String

    @JvmStatic
    external fun getRimeCaretPos(): Int

    @JvmStatic
    external fun setRimeCaretPos(caretPos: Int)

    // +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

    /** 获取候选字分页数据 */
    @JvmStatic
    external fun getRimeCandidates(
        /** 分页起始序号（从 0 开始） */
        startIndex: Int,
        /** 分页大小 */
        limit: Int,
    ): Array<CandidateItem>

    /** 选择候选字列表中指定序号的候选字：其将更新候选字的权重 */
    @JvmStatic
    external fun selectRimeCandidate(index: Int, global: Boolean): Boolean

    @JvmStatic
    external fun getRimeBulkCandidates(): Array<Any>

    @JvmStatic
    external fun deleteRimeCandidate(index: Int, global: Boolean): Boolean

    @JvmStatic
    external fun changeRimeCandidatePage(backward: Boolean): Boolean

    // +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
    // Note: 以下接口名与业务层的概念存在混淆，为避免修改 jni 映射接口名，这里仅通过文档强调各自的区别
    // 输入方案（Schema）涉及的操作：部署 -> 启用 -> 激活

    /**
     * 获取当前已激活（正在使用）的输入方案
     *
     * @return 已激活方案的 id
     */
    @JvmStatic
    external fun getCurrentRimeSchema(): String

    /** 激活在 [getRimeSchemaList] 中指定的输入方案，使其称为正在使用的方案 */
    @JvmStatic
    external fun selectRimeSchema(schemaId: String): Boolean

    /**
     * 获取已启用的（在 default.yaml 中配置的 schema_list 项）输入方案列表
     *
     * Note: 其与 [getSelectedRimeSchemaList] 的数据来源相同，
     * 不同的是，后者返回结果只包含 [SchemaItem.id] 不含 [SchemaItem.name]，
     * 而本接口则同时包含方案的 id 和 name
     */
    @JvmStatic
    external fun getRimeSchemaList(): Array<SchemaItem>

    //

    /**
     * 获取可用的（在共享和用户目录中的 .schema.yaml 文件）输入方案列表
     *
     * Note: 返回结果同时包含方案 id 和 name
     */
    @JvmStatic
    external fun getAvailableRimeSchemaList(): Array<SchemaItem>

    /** 启用（在 default.yaml 中补充 schema_list 项）指定的（多个）输入方案 */
    @JvmStatic
    external fun selectRimeSchemas(schemaIds: Array<String>): Boolean

    /**
     * 获取已启用的（在 default.yaml 中配置的 schema_list 项）输入方案列表
     *
     * Note: 返回结果仅包含方案 id，不含方案 name
     */
    @Deprecated("改用 Rime.getRimeSchemaList() 获得结果")
    @JvmStatic
    private external fun getSelectedRimeSchemaList(): Array<SchemaItem>

    //

    /** 部署输入方案（如 Rime 万象拼音），方案只有在被部署后才能启用并进而激活 */
    @JvmStatic
    external fun deployRimeSchemaFile(
        /** 输入方案 yaml 文件绝对路径 */
        schemaFile: String
    ): Boolean

    // +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

    // testing
    @JvmStatic
    external fun simulateRimeKeySequence(keySequence: String): Boolean
}
