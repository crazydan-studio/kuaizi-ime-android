/*
 * SPDX-FileCopyrightText: 2015 - 2025 Rime community
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.osfans.trime.core

import com.osfans.trime.core.Rime.getCurrentRimeSchema
import com.osfans.trime.core.Rime.getRimeOption
import com.osfans.trime.core.Rime.getRimeSchemaList
import com.osfans.trime.core.Rime.getSelectedRimeSchemaList
import com.osfans.trime.core.Rime.setRimeOption

typealias RimeMessageHandler = (Int, Array<Any>) -> Unit

/**
 * 只有 Rime 启动、同步和部署 schema（输入方案）/配置文件才是异步的，其与接口均为同步的。
 * 但为了统一上层调用，强制要求以消息形式监听输入等
 */
object Rime {
    private val rimeMessageHandlers = ArrayList<RimeMessageHandler>()

    init {
        System.loadLibrary("rime_jni")
    }

    fun registerRimeMessageHandler(handler: RimeMessageHandler) {
        if (!rimeMessageHandlers.contains(handler)) {
            rimeMessageHandlers.add(handler)
        }
    }

    fun unregisterRimeMessageHandler(handler: RimeMessageHandler) {
        rimeMessageHandlers.remove(handler)
    }

    // --------------------------------- Called by Native ---------------------------------

    /**
     *  处理来自 Rime native 层的消息，主要为 `1 - Schema`, `2 - Option`, `3 - Deploy`
     *
     * Note: 该函数由 c/c++ 代码调用，不能更改签名
     */
    @JvmStatic
    fun handleRimeMessage(type: Int, params: Array<Any>) {
        rimeMessageHandlers.forEach { it.invoke(type, params) }
    }

    // ---------------------------------- Native Interface --------------------------------

    // init
    /**
     * ## `fullCheck = true` 时的操作
     *
     * 执行完整的维护流程，无条件执行所有任务：
     *
     * 1. **clean_old_log_files** - 清理旧日志文件
     * 2. **installation_update** - 更新 `installation.yaml`
     * 3. **workspace_update** - 更新工作区，构建所有 Schema
     * 4. **user_dict_upgrade** - 升级用户词典
     * 5. **cleanup_trash** - 清理回收站目录
     *
     * ## `fullCheck = false` 时的操作
     *
     * 执行增量维护，先检测变更：
     *
     * 1. **clean_old_log_files** - 清理旧日志文件
     * 2. **installation_update** - 更新 `installation.yaml`
     * 3. **detect_modifications** - 检测文件变更
     *    - 检查用户数据目录和共享数据目录中的 `.yaml` 文件
     *    - 比较文件修改时间与 `user.yaml` 中的 `last_build_time`
     *    - 如果没有变更，提前返回
     * 4. **仅当检测到变更时才执行**：
     *    - `workspace_update`
     *    - `user_dict_upgrade`
     *    - `cleanup_trash`
     *
     * ## 对文件和数据的影响
     *
     * | 任务 | 影响的文件/数据 | 效果 |
     * |------|----------------|------|
     * | `clean_old_log_files` | 临时目录中的旧日志文件 | 删除非当日的 `.log` 文件 |
     * | `installation_update` | `installation.yaml` | 更新安装配置 |
     * | `detect_modifications` | 用户/共享目录的 `.yaml` 文件 | 检查是否需要重建 |
     * | `workspace_update` | `staging_dir` 中的编译文件 | 重新构建 Schema 和词典 |
     * | `user_dict_upgrade` | 用户词典数据库 | 升级词典格式 |
     * | `cleanup_trash` | `$user_data_dir/trash` 目录 | 清理过时文件 |
     *
     * ## Notes
     *
     * - `fullCheck=true` 强制执行完整维护，适合**首次部署或重大更新**
     * - `fullCheck=false` 执行智能维护，只在检测到变更时才重建，提高效率
     * - 所有任务在维护线程中**异步执行**
     * - 维护期间输入法会进入维护模式，暂时禁用输入功能
     */
    @JvmStatic
    external fun startupRime(
        /** Rime 共享数据目录绝对路径 */
        sharedDir: String,
        /** Rime 用户数据目录绝对路径 */
        userDir: String,
        /** Rime 发行方（在 JNI C 代码中已固定为 Trime）的版本号 */
        versionName: String = "3.3.9", // TODO Trime version from src/main/jni/CMakeLists.txt
        fullCheck: Boolean,
    )

    @JvmStatic
    external fun exitRime()

    // ------------------------------------------------------------------

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

    // ------------------------------------------------------------------

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

    // ------------------------------------------------------------------

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

    // ------------------------------------------------------------------

    // output
    @JvmStatic
    external fun getRimeCommit(): CommitProto

    /** 获取 Rime 当前输入的上下文信息，可以得到候选字列表 [MenuProto.candidates] */
    @JvmStatic
    external fun getRimeContext(): ContextProto

    /** 获取 Rime 当前状态，含 [getCurrentRimeSchema] 的 `id` 和 `name`，以及通用选项值 [getRimeOption] */
    @JvmStatic
    external fun getRimeStatus(): StatusProto

    // ------------------------------------------------------------------

    /** 获取当前正在输入的字符串 */
    @JvmStatic
    external fun getRimeRawInput(): String

    @JvmStatic
    external fun getRimeCaretPos(): Int

    @JvmStatic
    external fun setRimeCaretPos(caretPos: Int)

    // ------------------------------------------------------------------

    /** 获取候选字分页数据。循环分页可获取到全部候选字 */
    @JvmStatic
    external fun getRimeCandidates(
        /** 分页起始序号（从 0 开始） */
        startIndex: Int,
        /** 分页大小 */
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

    // ------------------------------------------------------------------
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

    // ------------------------------------------------------------------

    // testing
    @JvmStatic
    external fun simulateRimeKeySequence(keySequence: String): Boolean
}
