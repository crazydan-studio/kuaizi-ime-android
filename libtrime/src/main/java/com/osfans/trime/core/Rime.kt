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
        /** Rime 共享数据目录绝对路径 */
        sharedDir: String,
        /** Rime 用户数据目录绝对路径 */
        userDir: String,
        /** Rime 发行方（在 JNI C 代码中已固定为 Trime）的版本号 */
        versionName: String = "3.3.9", // Trime version
        fullCheck: Boolean,
    )

    @JvmStatic
    external fun exitRime()

    // +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

    /**
     * 部署/更新 Rime 配置文件
     *
     * see `ConfigFileUpdate::Run` in `librime/src/rime/lever/deployment_tasks.cc`
     */
    @JvmStatic
    external fun deployRimeConfigFile(
        /**
         * 待部署/更新的配置文件名（如 `default.yaml`），其位于共享和用户目录，
         * Rime 将根据 [versionKey] 判断用户目录中的该配置文件是否需要更新，
         * 若共享目录中的配置文件版本更新，则将其更新到用户目录。
         *
         * Note: 用户自定义配置文件 `.custom.yaml` 不受影响
         */
        fileName: String = "default.yaml",
        /**
         * 配置文件中版本信息的键路径（如 `config/version`、`config_version`）。
         * Rime 将根据版本号决定是否更新配置文件
         */
        versionKey: String = "config_version",
    ): Boolean

    /**
     * 备份和同步用户词典及配置文件（存放在用户目录中的 `sync` 子目录）
     *
     * see `InstallationUpdate::Run`/`BackupConfigFiles::Run`/`UserDictSync::Run`
     * in `librime/src/rime/lever/deployment_tasks.cc`
     */
    @JvmStatic
    external fun syncRimeUserData(): Boolean

    // +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

    /** see [setRimeOption] */
    @JvmStatic
    external fun getRimeOption(option: String): Boolean

    /**
     * 更新输入选项，用于控制中英文切换（`ascii_mode`）、全半角切换（`full_shape`）、中英文标点切换（`ascii_punct`）等
     *
     * Note: 支持的选项在输入方案 `.schema.yaml` 文件中的 `switches` 定义，如：
     * ```yaml
     * switches:
     *   - name: ascii_mode
     *     states: [ 中文, 英文 ]
     * ```
     */
    @JvmStatic
    external fun setRimeOption(option: String, value: Boolean)

    // +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

    // input
    @JvmStatic
    external fun processRimeKey(
        /** Rime 按键值，其对应 [RimeKeyMapping].RimeKey_* */
        keyVal: Int,
        /** 控制按键组合标记，如 `Shift+Ctrl` 等，通过 [android.view.KeyEvent].META_* 做 `or` 运算得到 */
        modifiers: Int,
    ): Boolean

    /** 提交当前组合 */
    @JvmStatic
    external fun commitRimeComposition(): Boolean

    /** 清空组合 */
    @JvmStatic
    external fun clearRimeComposition()

    // +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

    // output
    @JvmStatic
    external fun getRimeCommit(): CommitProto

    /** 获取 Rime 当前输入的上下文信息，可以得到候选字列表 [MenuProto.candidates] */
    @JvmStatic
    external fun getRimeContext(): ContextProto

    /** 获取 Rime 当前状态，含 [getCurrentRimeSchema] 的 `id` 和 `name`，以及通用选项值 [getRimeOption] */
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
        /** 分页大小：设置一个较大的数，可以得到全部候选字 */
        limit: Int,
    ): Array<CandidateItem>

    /** 获取全部候选字数据：实际只有最多前 16 条数据（在 `rime_jni.cc` 中写死的） */
    @Deprecated("采用 Rime.getRimeCandidates() 循环获取全部分页数据")
    @JvmStatic
    private external fun getRimeBulkCandidates(): Array<Any>

    /** 选择候选字列表中指定序号的候选字：其将更新候选字的权重 */
    @JvmStatic
    external fun selectRimeCandidate(index: Int, global: Boolean): Boolean

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
     * 获取已启用的（在 `default.yaml` 中配置的 `schema_list` 项）输入方案列表：
     * ```yaml
     * schema_list:
     *   - schema: wanxiang
     *   - schema: wanxiang_english
     * ```
     *
     * Note: 其与 [getSelectedRimeSchemaList] 的数据来源相同，
     * 不同的是，后者返回结果只包含 [SchemaItem.id] 不含 [SchemaItem.name]，
     * 而本接口则同时包含方案的 `id` 和 `name`
     */
    @JvmStatic
    external fun getRimeSchemaList(): Array<SchemaItem>

    //

    /**
     * 获取可用的（在共享和用户目录中的 `.schema.yaml` 文件）输入方案列表
     *
     * Note: 返回结果同时包含方案 `id` 和 `name`
     */
    @JvmStatic
    external fun getAvailableRimeSchemaList(): Array<SchemaItem>

    /** 启用（在 `default.yaml` 中补充 `schema_list` 项）指定的（多个）输入方案 */
    @JvmStatic
    external fun selectRimeSchemas(schemaIds: Array<String>): Boolean

    /**
     * 获取已启用的（在 `default.yaml` 中配置的 `schema_list` 项）输入方案列表
     *
     * Note: 返回结果仅包含方案 `id`，不含方案 `name`
     */
    @Deprecated("改用 Rime.getRimeSchemaList() 获得结果")
    @JvmStatic
    private external fun getSelectedRimeSchemaList(): Array<SchemaItem>

    //

    /**
     * 部署输入方案 `.schema.yaml` 及其词典 `.dict.yaml`（其名字由 `.schema.yaml` 中的 `translator/dictionary` 确定），
     * 输入方案只有在被部署后才能启用并进而激活
     *
     * see `SchemaUpdate::Run` in `librime/src/rime/lever/deployment_tasks.cc`
     */
    @JvmStatic
    external fun deployRimeSchemaFile(
        /** 输入方案 `.schema.yaml` 文件的绝对路径 */
        schemaFile: String
    ): Boolean

    // +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

    // testing
    @JvmStatic
    external fun simulateRimeKeySequence(keySequence: String): Boolean
}
