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
 * If not, see <see href="https://www.gnu.org/licenses/lgpl-3.0.en.html#license-text"/>.
 */

package org.crazydan.studio.app.ime.kuaizi.config

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.crazydan.studio.app.ime.kuaizi.engine.ImeConfig
import org.crazydan.studio.app.ime.kuaizi.engine.datastore.EngineConfigDataStore
import org.crazydan.studio.app.ime.kuaizi.engine.datastore.UiConfigDataStore

/** Context 扩展属性，提供 IME 配置的 DataStore 实例。 */
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore("ime_config")

/**
 * 基于 DataStore 的配置持久化实现。
 *
 * 将 [ImeConfig] 中的 [ImeConfig.Engine] 和 [ImeConfig.Ui] 持久化到 Preferences DataStore。
 * 支持增量更新：仅将变化的部分写入 DataStore，避免全量写入。
 *
 * [ImeConfig.Runtime] 不做持久化，应用重启时根据 [ImeConfig.Startup] 重新初始化。
 */
class ConfigDataStore(private val context: Context) {
    /**
     * 配置的响应式 Flow。
     *
     * 从 DataStore 读取原始 Preferences，映射为 [ImeConfig] 实例。
     * UI 层通过订阅此 Flow 实时响应配置变更。
     */
    val config: Flow<ImeConfig> = context.dataStore.data.map { prefs ->
        ImeConfig(
            engine = EngineConfigDataStore.readConfig(prefs),
            ui = UiConfigDataStore.readConfig(prefs),
        )
    }

    /**
     * 增量更新配置。
     *
     * 通过比较新旧配置的差异，仅将发生变化的字段写入 DataStore，
     * 避免每次 updateConfig 都全量写入所有 key。
     * 适用于高频配置切换场景（主题、音效开关的快速连续切换）。
     */
    suspend fun updateConfig(transform: (ImeConfig) -> ImeConfig) {
        context.dataStore.edit { prefs ->
            val current = config.first()
            val new = transform(current)

            EngineConfigDataStore.writeConfig(prefs, current.engine, new.engine)
            UiConfigDataStore.writeConfig(prefs, current.ui, new.ui)
        }
    }
}
