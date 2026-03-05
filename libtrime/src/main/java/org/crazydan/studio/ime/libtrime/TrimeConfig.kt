/*
 * 筷字输入法 - 高效编辑需要又好又快的输入法
 * Copyright (C) 2026 Crazydan Studio <https://studio.crazydan.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.
 * If not, see <https://www.gnu.org/licenses/lgpl-3.0.en.html#license-text>.
 */

package org.crazydan.studio.ime.libtrime

import com.osfans.trime.core.RimeConfig.closeRimeConfig
import com.osfans.trime.core.RimeConfig.getRimeConfigInt
import com.osfans.trime.core.RimeConfig.getRimeConfigListItemPath
import com.osfans.trime.core.RimeConfig.getRimeConfigString
import com.osfans.trime.core.RimeConfig.openRimeConfig
import com.osfans.trime.core.RimeConfig.openRimeSchema
import com.osfans.trime.core.RimeConfig.openRimeUserConfig
import com.osfans.trime.core.RimeConfig.setRimeConfigBool
import timber.log.Timber

/**
 * 支持如下配置：
 * - 系统配置文件：如 `default.yaml`、`symbols.yaml` 等
 * - 用户配置文件：如 `user.yaml`、`default.custom.yaml`、`installation.yaml` 等
 * - Schema 配置文件：如 `luna_pinyin.schema.yaml`
 *
 * source from [trime/TrimeConfig.kt](https://github.com/osfans/trime/blob/develop/app/src/main/java/com/osfans/trime/core/TrimeConfig.kt)
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2026-03-04
 */
class TrimeConfig private constructor(
    private var ref: Long,
) : AutoCloseable {

    override fun close() {
        closeRimeConfig(ref)
    }

    /** 获取指定键（如 `menu/page_size`）的整数值 */
    fun getInt(key: String) = getRimeConfigInt(ref, key)

    /** 获取指定键（如 `switcher/fold_options`）的布尔值 */
    fun setBool(key: String, value: Boolean) = setRimeConfigBool(ref, key, value)

    /** 获取指定键（如 `schema/schema_id`）的字符串值 */
    fun getString(key: String) = getRimeConfigString(ref, key)

    /**
     * 获取指定键（如 `schema/dependencies`）的列表值：
     * ```kotlin
     * config.getList("switches") { path ->
     *     Switch(
     *         // Note: 键名需附加父级 path
     *         name = getString("$path/name") ?: "",
     *         options = getList("$path/options", TrimeConfig::getString),
     *     )
     * }
     * ```
     */
    fun <E : Any> getList(key: String, getAction: TrimeConfig.(String) -> E?): List<E> {
        val paths = getRimeConfigListItemPath(ref, key)

        val values = ArrayList<E>(paths.size)
        for (path in paths) {
            val value = getAction(this, path)
            if (value == null) {
                Timber.w("Failed to get value '${getString(path)}' as expected on path '$path'")
                continue
            }

            values.add(value)
        }
        return values
    }

    companion object {

        /**
         * 可打开的配置：
         * - 系统配置：如 `default.yaml`、`symbols.yaml` 等
         * - 预构建配置：从 `prebuilt_data_dir` 加载的配置文件
         */
        fun openSharedConfig(configId: String): TrimeConfig =
            TrimeConfig(openRimeConfig(configId))

        /**
         * 可打开的配置：
         * - 用户配置文件：如 `user.yaml`、`default.custom.yaml`
         * - 用户自定义文件：存储在 `user_data_dir` 中的配置
         * - 安装配置：如 `installation.yaml`
         */
        fun openUserConfig(configId: String): TrimeConfig =
            TrimeConfig(openRimeUserConfig(configId))

        /** 打开 Schema 配置文件（如 `luna_pinyin.schema.yaml`） */
        fun openSchemaConfig(schemaId: String): TrimeConfig =
            TrimeConfig(openRimeSchema(schemaId))
    }
}